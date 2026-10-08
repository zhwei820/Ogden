#!/usr/bin/env python3
"""生成口算小游戏读题用的离线录音到 app/src/main/assets/math/<lang>/{0..100,add,sub}.mp3。

与 App 其他朗读（tts/，统一 -6% 语速）分开：读题要快、段间不能有空档，所以语速调快并裁掉首尾静音。
数字读法须与 MathDrill.kt 的 chineseNumber / numberWord 一致。
用法：python3 scripts/gen_math_audio.py [--force]
"""
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_tts_assets import request  # noqa: E402
from gen_word_audio import local_props  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/math"
VOICES = [("zh-CN", "zh-CN-XiaoxiaoNeural", "+12%"), ("en-US", "en-US-JennyNeural", "+5%"), ("en-GB", "en-GB-SoniaNeural", "+5%")]

DIGITS = "零一二三四五六七八九"
ONES = ["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve",
        "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"]
TENS = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"]


def zh(n):
    if n == 100:
        return "一百"
    if n < 10:
        return DIGITS[n]
    return ("" if n < 20 else DIGITS[n // 10]) + "十" + ("" if n % 10 == 0 else DIGITS[n % 10])


def en(n):
    if n < 20:
        return ONES[n]
    if n == 100:
        return "one hundred"
    return TENS[n // 10] + ("" if n % 10 == 0 else "-" + ONES[n % 10])


def texts(lang):
    words = zh if lang == "zh-CN" else en
    ops = {"add": "加", "sub": "减"} if lang == "zh-CN" else {"add": "plus", "sub": "minus"}
    return {str(n): words(n) for n in range(101)} | ops


def ssml(text, voice, lang, rate):
    return (f"<speak version='1.0' xml:lang='{lang}'><voice xml:lang='{lang}' name='{voice}'>"
            f"<prosody rate='{rate}'>{text}</prosody></voice></speak>")


# 首尾低于 -45dB 的部分裁掉，各留 30ms 免得吞字头
TRIM = ("silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.03,"
        "areverse,silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.03,areverse")


def main():
    force = "--force" in sys.argv
    props = local_props()
    key, region = props.get("azure.speech.key", ""), props.get("azure.speech.region", "")
    if not key or not region:
        sys.exit("local.properties 缺少 azure.speech.key / azure.speech.region")
    for lang, voice, rate in VOICES:
        out_dir = OUT / lang
        out_dir.mkdir(parents=True, exist_ok=True)
        made = 0
        for name, text in texts(lang).items():
            target = out_dir / f"{name}.mp3"
            if target.exists() and not force:
                continue
            raw = request(ssml(text, voice, lang, rate), key, region, "audio-24khz-48kbitrate-mono-mp3")
            with tempfile.NamedTemporaryFile(suffix=".mp3") as tmp:
                Path(tmp.name).write_bytes(raw)
                subprocess.run(["ffmpeg", "-nostdin", "-y", "-loglevel", "error", "-i", tmp.name, "-af", TRIM,
                                "-ac", "1", "-b:a", "32k", str(target)], check=True)
            made += 1
        print(f"{lang}: 生成 {made} 个", flush=True)


if __name__ == "__main__":
    main()
