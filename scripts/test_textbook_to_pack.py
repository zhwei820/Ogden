"""python3 scripts/test_textbook_to_pack.py"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from textbook_to_pack import build_speeches, parse_textbook  # noqa: E402

SAMPLE = """# 书
## Unit 1 Face | 第一单元 脸
### Let's chant! | 一起说唱
words: face, eye
Eyes on the face. | 脸上有眼睛。
### Story
I have big eyes.
## Unit 2 Colours
### Story
Red and blue. | 红和蓝。
"""


class ParseTextbookTest(unittest.TestCase):
    def test_sections_become_lessons_numbered_across_units(self):
        speeches = build_speeches("t", parse_textbook(SAMPLE))
        self.assertEqual([(s["id"], s["unit"], s["group"]) for s in speeches],
                         [("t-u1-1", 1, "Unit 1 Face"), ("t-u1-2", 2, "Unit 1 Face"), ("t-u2-1", 3, "Unit 2 Colours")])
        self.assertEqual(speeches[0]["words"], ["face", "eye"])
        self.assertEqual(speeches[0]["lines"], [{"en": "Eyes on the face.", "zh": "脸上有眼睛。"}])
        self.assertEqual(speeches[1]["lines"][0]["zh"], "")
        self.assertEqual(speeches[1]["titleZh"], "Story")

    def test_sentence_outside_section_reports_line_number(self):
        with self.assertRaisesRegex(ValueError, "第 3 行"):
            parse_textbook("# 书\n## Unit 1\nHello.\n")

    def test_section_without_sentences_is_rejected(self):
        with self.assertRaises(ValueError):
            parse_textbook("# 书\n## Unit 1\n### Empty\n")


if __name__ == "__main__":
    unittest.main()
