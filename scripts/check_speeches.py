#!/usr/bin/env python3
"""校验 app/src/main/assets/speeches.json：每级句数 / 句长 / 句型数是否在梯度范围内，并列出每单元的词表外用词。

用法：python3 scripts/check_speeches.py [speeches.json ...]   有错误时退出码为 1。
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"

# level -> (Track1 句数, 每句词数, Track2 句型数)
RULES = {
    1: ((6, 8), (2, 7), (4, 4)),
    2: ((8, 10), (3, 10), (4, 6)),
    3: ((10, 14), (3, 16), (4, 6)),
}
THEMES = ["me", "school", "food", "nature", "seasons", "hobbies", "places", "city", "jobs", "festivals"]
UNITS_PER_THEME = 5
UNITS_PER_LEVEL = len(THEMES) * UNITS_PER_THEME

# 词表只收原形；这些代词变格 / be·do·have 变位 / 情态词按 Ogden 规则视为表内
INFLECTED = set("""
an me my mine your yours him his she her hers it its we us our ours they them their theirs
is am are was were been being does did doing done has had having can could would shall should might must
these those what which isn't don't doesn't didn't can't i'm it's you're we're they're he's she's
there's that's let's i've i'll you'll we'll won't
""".split())


def ogden_words():
    data = json.loads((ASSETS / "ogden_words.json").read_text(encoding="utf-8-sig"))
    return {item["w"].lower() for item in data}


def in_list(token, words):
    if token in words or token in INFLECTED:
        return True
    # Ogden 允许 -s/-es/-ed/-ing/-er/-ly 派生；按常见拼写规则粗还原
    for suffix, repl in (("ies", "y"), ("ied", "y"), ("es", ""), ("s", ""), ("ed", ""), ("ed", "e"),
                         ("ing", ""), ("ing", "e"), ("er", ""), ("er", "e"), ("ly", "")):
        if token.endswith(suffix):
            base = token[: -len(suffix)] + repl
            if base in words or (len(base) > 2 and base[-1] == base[-2] and base[:-1] in words):
                return True
    return False


def tokens(text):
    return [t.lower() for t in re.findall(r"[A-Za-z]+(?:['-][A-Za-z]+)*", text)]


def check(path, words):
    units = json.loads(Path(path).read_text(encoding="utf-8"))
    errors = []
    seen = {}
    for u in units:
        uid = u.get("id", "?")
        level = u.get("level")
        if level not in RULES:
            errors.append(f"{uid}: level={level} 不合法")
            continue
        seen.setdefault(level, []).append((u.get("unit"), u.get("theme")))
        if u.get("theme") not in THEMES:
            errors.append(f"{uid}: theme={u.get('theme')} 不合法")
        (lmin, lmax), (wmin, wmax), (pmin, pmax) = RULES[level]
        lines, patterns = u.get("lines", []), u.get("patterns", [])
        if not lmin <= len(lines) <= lmax:
            errors.append(f"{uid}: Track1 {len(lines)} 句，应为 {lmin}-{lmax}")
        if not pmin <= len(patterns) <= pmax:
            errors.append(f"{uid}: Track2 {len(patterns)} 条，应为 {pmin}-{pmax}")
        for item in lines + patterns:
            if not item.get("en", "").strip() or not item.get("zh", "").strip():
                errors.append(f"{uid}: 存在空的 en/zh")
        for line in lines:
            n = len(tokens(line["en"]))
            if not wmin <= n <= wmax:
                errors.append(f"{uid}: 「{line['en']}」{n} 词，应为 {wmin}-{wmax}")
        extra = sorted({t for item in lines + patterns for t in tokens(item["en"]) if not in_list(t, words)})
        print(f"{uid:6} L{level} {u.get('title', '')[:28]:28} 表外词({len(extra)}): {' '.join(extra)}")
    for level, units_seen in sorted(seen.items()):
        units_seen.sort(key=lambda x: x[0] or 0)
        if [n for n, _ in units_seen] != list(range(1, UNITS_PER_LEVEL + 1)):
            errors.append(f"L{level}: 单元号应为 1-{UNITS_PER_LEVEL}，实际 {[n for n, _ in units_seen]}")
        # 单元按主题顺序连续编号：第 1-5 单元属于 THEMES[0]，依此类推
        expected = [t for t in THEMES for _ in range(UNITS_PER_THEME)]
        if [t for _, t in units_seen] != expected:
            errors.append(f"L{level}: 主题应按 {THEMES} 顺序每个 {UNITS_PER_THEME} 单元连续排列")
    return errors


def main():
    words = ogden_words()
    paths = sys.argv[1:] or [ASSETS / "speeches.json"]
    errors = [e for p in paths for e in check(p, words)]
    for e in errors:
        print("ERROR", e)
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
