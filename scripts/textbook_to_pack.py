#!/usr/bin/env python3
"""把整理好的教材纯文本转成教材资源包（scripts/mock_packs/<id>/），供 mock_pack_server.py 提供下载。

文本格式（# 书名 / ## 单元 / ### 小节，小节即 App 里的一课）：
    # 外研版（一年级起点）二年级上册
    ## Unit 1 Face | 第一单元 脸
    ### Let's chant! | 一起说唱
    words: face, eye, mouth          ← 可选；本课单词，不写则为空
    Eyes on the face. | 脸上有眼睛。   ← 中文可省略，省略的用 Azure 翻译补上并列为「待审」

同时用 Azure 生成美音、英音朗读（tts/<lang>/<sha1>.mp3，与 App 的 ttsKey 一致），已存在的跳过。
key 读 local.properties 的 azure.speech.* 与 azure.translator.*。
用法：python3 scripts/textbook_to_pack.py scripts/textbook_src/fltrp-g2a.txt --id textbook-fltrp-g2a [--version 1]
"""
import argparse
import json
import sys
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_tts_assets import VOICES, key, synthesize, write  # noqa: E402
from gen_word_audio import SSL_CONTEXT, local_props  # noqa: E402

ROOT = Path(__file__).resolve().parent
TEXTBOOK_LEVEL = 5  # 与 SpeechText.kt 的 TextbookLevel 一致
GRADES = "一二三四五六"


def book_label(title):
    """书名里括号之后的部分作 App 选书芯片上的简称：「外研版（一年级起点）一年级上册」→「一年级上册」。"""
    return title.rsplit("）", 1)[-1].strip() or title


def book_order(label):
    """选书芯片的排序：一年级上册 → 11、一年级下册 → 12 …；认不出年级的排最后。"""
    if len(label) >= 4 and label[0] in GRADES and label[1:3] == "年级" and label[3] in "上下":
        return (GRADES.index(label[0]) + 1) * 10 + (1 if label[3] == "上" else 2)
    return 99


def split_bilingual(text):
    en, _, zh = text.partition("|")
    return en.strip(), zh.strip()


def parse_textbook(text):
    """解析教材文本为 {"title", "units": [{"title", "titleZh", "sections": [{"title", "titleZh", "words", "lines"}]}]}。

    lines 为 [(en, zh)]，zh 可能为空串。格式错误（句子出现在小节之前等）抛 ValueError 并带行号。
    """
    book = {"title": "", "units": []}
    section = None
    for number, raw in enumerate(text.splitlines(), 1):
        line = raw.strip()
        if not line:
            continue
        if line.startswith("### "):
            if not book["units"]:
                raise ValueError(f"第 {number} 行：小节前缺少 ## 单元")
            title, title_zh = split_bilingual(line[4:])
            section = {"title": title, "titleZh": title_zh, "words": [], "lines": []}
            book["units"][-1]["sections"].append(section)
        elif line.startswith("## "):
            title, title_zh = split_bilingual(line[3:])
            book["units"].append({"title": title, "titleZh": title_zh, "sections": []})
            section = None
        elif line.startswith("# "):
            book["title"] = line[2:].strip()
        elif section is None:
            raise ValueError(f"第 {number} 行：句子前缺少 ### 小节：{line}")
        elif line.lower().startswith("words:"):
            section["words"] = [w.strip() for w in line[6:].split(",") if w.strip()]
        else:
            section["lines"].append(split_bilingual(line))
    empty = [s["title"] for u in book["units"] for s in u["sections"] if not s["lines"]]
    if not book["title"] or not book["units"] or empty:
        raise ValueError(f"缺少书名 / 单元，或小节没有句子：{empty}")
    return book


def build_speeches(pack_id, book):
    """每个小节一课；unit 在整册内连续编号，决定排序；group 为单元名，App 按它分组，book 为选书芯片上的书名。"""
    speeches, unit = [], 0
    label = book_label(book["title"])
    for u_index, u in enumerate(book["units"], 1):
        for s_index, s in enumerate(u["sections"], 1):
            unit += 1
            speeches.append({
                "id": f"{pack_id}-u{u_index}-{s_index}",
                "level": TEXTBOOK_LEVEL,
                "unit": unit,
                "book": label,
                "bookOrder": book_order(label),
                "group": u["title"],
                "groupZh": u["titleZh"],
                "title": s["title"],
                "titleZh": s["titleZh"] or s["title"],
                "lines": [{"en": en, "zh": zh} for en, zh in s["lines"]],
                "patterns": [],
                "words": s["words"],
            })
    return speeches


def translate_missing(speeches, props):
    """补空的中文翻译，返回补过的英文句子（需人工审）。"""
    missing = [line for s in speeches for line in s["lines"] if not line["zh"]]
    if not missing:
        return []
    req = urllib.request.Request(
        "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=en&to=zh-Hans",
        data=json.dumps([{"Text": line["en"]} for line in missing]).encode("utf-8"),
        headers={
            "Ocp-Apim-Subscription-Key": props["azure.translator.key"],
            "Ocp-Apim-Subscription-Region": props.get("azure.translator.region", ""),
            "Content-Type": "application/json; charset=UTF-8",
        },
    )
    with urllib.request.urlopen(req, timeout=30, context=SSL_CONTEXT) as resp:
        results = json.loads(resp.read())
    for line, result in zip(missing, results):
        line["zh"] = result["translations"][0]["text"]
    return [line["en"] for line in missing]


def synthesize_lines(speeches, out_dir, props):
    count = 0
    for voice, lang in VOICES["en"]:
        for text in {line["en"] for s in speeches for line in s["lines"]}:
            target = out_dir / "tts" / lang / f"{key(voice, text)}.mp3"
            if target.exists():
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            write(target, synthesize(text, voice, lang, props["azure.speech.key"], props["azure.speech.region"]))
            count += 1
    return count


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("--id", required=True, help="资源包 id，也是 mock_packs 下的目录名")
    parser.add_argument("--version", type=int, default=1)
    args = parser.parse_args()

    book = parse_textbook(Path(args.source).read_text(encoding="utf-8"))
    speeches = build_speeches(args.id, book)
    props = local_props()
    to_review = translate_missing(speeches, props)

    out_dir = ROOT / "mock_packs" / args.id
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "speeches.json").write_text(json.dumps(speeches, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    units = ", ".join(u["title"] for u in book["units"])
    meta = {"type": "textbook", "title": book["title"], "description": f"{units} · 共 {len(speeches)} 课", "version": args.version}
    (out_dir / "pack.json").write_text(json.dumps(meta, ensure_ascii=False) + "\n", encoding="utf-8")
    generated = synthesize_lines(speeches, out_dir, props)

    print(f"{out_dir}: {len(speeches)} 课，新生成朗读 {generated} 条")
    for en in to_review:
        print(f"  待审翻译：{en}")


if __name__ == "__main__":
    main()
