"""leveled-words.md → app/src/main/assets/cefr_words.tsv（level<TAB>word<TAB>pos<TAB>zh），改了词表重跑。"""
import re
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
src = root / "leveled-words.md"
dst = root / "app/src/main/assets/cefr_words.tsv"

entry = re.compile(r"^\d+\. (.+?) ((?:(?:n|v|adj|adv|prep|conj|pron|num|det|int|phr\.v)\./?)+) (.+)$")
level = None
rows, seen, bad = [], set(), []
for line in src.read_text(encoding="utf-8").splitlines():
    if line.startswith("## "):
        level = line[3:].split()[0]
        continue
    m = entry.match(line)
    if m and level:
        word, pos, zh = m.groups()
        if word.lower() in seen:
            bad.append(f"dup {word}")
            continue
        seen.add(word.lower())
        rows.append(f"{level}\t{word}\t{pos}\t{zh.strip()}")
    elif re.match(r"^\d+\. ", line):
        bad.append(line)
if bad:
    sys.exit("unparsed:\n" + "\n".join(bad[:20]))
dst.write_text("\n".join(rows) + "\n", encoding="utf-8")
print(len(rows), "words →", dst.relative_to(root))
