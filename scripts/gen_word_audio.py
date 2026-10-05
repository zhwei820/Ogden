#!/usr/bin/env python3
"""为词表中缺离线发音的词调用 Azure TTS 生成 assets/audio/{us,uk}/<word>.mp3。

key / region 读 local.properties 的 azure.speech.key / azure.speech.region。已存在的文件跳过，可重复执行。
用法：python3 scripts/gen_word_audio.py
"""
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"
# 与 AzureSpeaker / buildSsml 保持一致：同一嗓音、同一输出格式，和原有 850 词音频同为 24kHz 48kbps mono
VOICES = {"us": ("en-US-JennyNeural", "en-US"), "uk": ("en-GB-SoniaNeural", "en-GB")}
OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"


def local_props():
    props = {}
    for line in (ROOT / "local.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    return props


def file_name(word):
    # 与 MainActivity.localAudioPath 的命名规则一致
    return re.sub(r"[^a-z0-9]+", "_", word.lower()).strip("_") + ".mp3"


def synthesize(word, voice, lang, key, region):
    ssml = (f"<speak version='1.0' xml:lang='{lang}'><voice xml:lang='{lang}' name='{voice}'>"
            f"{escape(word)}</voice></speak>")
    req = urllib.request.Request(
        f"https://{region}.tts.speech.microsoft.com/cognitiveservices/v1",
        data=ssml.encode("utf-8"),
        headers={
            "Ocp-Apim-Subscription-Key": key,
            "Content-Type": "application/ssml+xml",
            "X-Microsoft-OutputFormat": OUTPUT_FORMAT,
            "User-Agent": "OgdenBasic",
        },
    )
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                return resp.read()
        except urllib.error.HTTPError as e:
            # 429 为免费档限流，退避后重试
            if e.code == 429 and attempt < 3:
                time.sleep(2 ** attempt * 2)
                continue
            raise


def main():
    props = local_props()
    key, region = props.get("azure.speech.key", ""), props.get("azure.speech.region", "")
    if not key or not region:
        sys.exit("local.properties 缺少 azure.speech.key / azure.speech.region")
    words = [item["w"] for item in json.loads((ASSETS / "ogden_words.json").read_text(encoding="utf-8-sig"))]
    made = 0
    for accent, (voice, lang) in VOICES.items():
        out_dir = ASSETS / "audio" / accent
        for word in words:
            target = out_dir / file_name(word)
            if target.exists() and target.stat().st_size > 0:
                continue
            target.write_bytes(synthesize(word, voice, lang, key, region))
            made += 1
            print(f"{accent} {word}")
    print(f"生成 {made} 个文件")


if __name__ == "__main__":
    main()
