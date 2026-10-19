#!/usr/bin/env python3
"""把视频课素材目录打成视频课资源包（scripts/mock_packs/<id>/），供 mock_pack_server.py 提供下载。

素材目录下每集一个子目录，含 video.mp4、thumb.jpg、transcript.md（Whisper 文稿）、lesson.json（课文 / 单词 / 句型 / 题目）；
页面模板是 scripts/video_course/index.html。包内布局：course/index.html + course/data.js，各集 <集>/video.mp4、<集>/thumb.jpg，
页面用 ../<集>/ 相对路径引用视频，所以包目录在电脑上直接用浏览器打开 course/index.html 也能看。
视频用硬链接放进包目录，不额外占空间。

用法：python3 scripts/video_course_to_pack.py videos --id course-singsing --title "英文儿歌视频课" [--version 1]
"""
import argparse
import json
import os
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent
LINE_RE = re.compile(r"^\*\*\[(?:(\d+):)?(\d+):(\d+)\]\*\* (.*?)\s*$")


def read_lines(transcript):
    lines = []
    for raw in transcript.read_text(encoding="utf-8").splitlines():
        m = LINE_RE.match(raw)
        if m:
            h, mi, s, text = m.groups()
            lines.append({"t": int(h or 0) * 3600 + int(mi) * 60 + int(s), "text": text})
    return lines


def link(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists():
        dst.unlink()
    try:
        os.link(src, dst)
    except OSError:  # 跨文件系统时退回复制
        shutil.copy2(src, dst)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("src", type=Path, help="素材目录，每集一个子目录")
    parser.add_argument("--id", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--description", default="")
    parser.add_argument("--version", type=int, default=1)
    args = parser.parse_args()

    out = ROOT / "mock_packs" / args.id
    shutil.rmtree(out, ignore_errors=True)
    lessons = []
    for d in sorted(p for p in args.src.iterdir() if p.is_dir()):
        files = [d / n for n in ("lesson.json", "transcript.md", "video.mp4", "thumb.jpg")]
        if not all(f.is_file() for f in files):
            print("skip", d.name)
            continue
        lesson = json.loads(files[0].read_text(encoding="utf-8"))
        lesson.update(id=d.name, video=f"../{d.name}/video.mp4", cover=f"../{d.name}/thumb.jpg", lines=read_lines(files[1]))
        lessons.append(lesson)
        link(files[2], out / d.name / "video.mp4")
        link(files[3], out / d.name / "thumb.jpg")
    if not lessons:
        raise SystemExit(f"{args.src} 下没有完整的课")

    lessons.sort(key=lambda l: (l.get("level", 1), l["topic_en"]))
    (out / "course").mkdir(parents=True, exist_ok=True)
    shutil.copy2(ROOT / "video_course" / "index.html", out / "course" / "index.html")
    (out / "course" / "data.js").write_text("window.LESSONS = " + json.dumps(lessons, ensure_ascii=False) + ";\n", encoding="utf-8")
    # course.json 是 App 认包的数据文件（PackType.Course.dataFile），列表上显示它的标题和课数
    (out / "course.json").write_text(json.dumps({"title": args.title, "count": len(lessons)}, ensure_ascii=False), encoding="utf-8")
    description = args.description or f"{len(lessons)} 课，含视频、课文、重点单词与复习题"
    pack = {"type": "course", "title": args.title, "description": description, "version": args.version}
    (out / "pack.json").write_text(json.dumps(pack, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(lessons)} lessons → {out}")


if __name__ == "__main__":
    main()
