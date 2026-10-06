#!/usr/bin/env python3
"""按 app/build/tts-manifest.json 预生成英文句子朗读音频到 app/src/main/assets/tts/<lang>/<sha1>.mp3（中文不预生成）。

清单由单元测试生成：./gradlew :app:testDebugUnitTest --tests '*TtsCoverageTest.writeManifest'
文件名规则与 TtsTexts.ttsKey / AzureSpeaker 缓存一致；已存在的跳过，可中断后重跑。
用法：python3 scripts/gen_tts_assets.py
装了 lameenc（pip install lameenc）时走合并模式：一次请求合成多条、按静音切开再编码成 mp3，
请求数约为逐条的 1/25（Azure 按请求次数限流）；没装时逐条请求。
"""
import array
import hashlib
import http.client
import json
import sys
import threading
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_word_audio import SSL_CONTEXT, local_props  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets/tts"
MANIFEST = ROOT / "app/build/tts-manifest.json"
# 与 AzureVoice 一致；英文按设置里的 US / UK 口音各生成一份
VOICES = {"en": [("en-US-JennyNeural", "en-US"), ("en-GB-SoniaNeural", "en-GB")]}
# 打包进 APK，比运行时缓存用的 24kHz/48kbps 更省体积，语音清晰度够用
OUTPUT_FORMAT = "audio-16khz-32kbitrate-mono-mp3"


def ssml(text, voice, lang):
    # 必须与 SpeechText.buildSsml 一致（同样的转义与 -6% 语速），否则预生成音频和在线合成听起来不同
    escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;").replace("'", "&apos;")
    return (f"<speak version='1.0' xml:lang='{lang}'><voice xml:lang='{lang}' name='{voice}'>"
            f"<prosody rate='-6%'>{escaped}</prosody></voice></speak>")


BATCH = 25
BREAK_MS = 3000          # 条与条之间插入的静音；句内停顿实测可达约 1.1s，要拉开足够距离
GAP_MS = 2500            # 连续这么长的静音才算分界
PAD_MS = 120             # 切出的每段首尾保留的静音
SAMPLE_RATE = 16000
SILENT = 400             # 16-bit 振幅低于此视为静音

try:
    import lameenc
except ImportError:
    lameenc = None


def batch_ssml(texts, voice, lang):
    def esc(t):
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;").replace("'", "&apos;")
    body = f"<break time='{BREAK_MS}ms'/>".join(f"<prosody rate='-6%'>{esc(t)}</prosody>" for t in texts)
    return f"<speak version='1.0' xml:lang='{lang}'><voice xml:lang='{lang}' name='{voice}'>{body}</voice></speak>"


def split_pcm(pcm, expected):
    """按长静音把 16-bit PCM 切成 expected 段；段数对不上返回 None。"""
    samples = array.array("h", pcm)
    win = SAMPLE_RATE // 100  # 10ms
    loud = [max(map(abs, samples[i:i + win]), default=0) >= SILENT for i in range(0, len(samples), win)]
    voiced = [i for i, v in enumerate(loud) if v]
    if not voiced:
        return None
    segments, start, prev = [], voiced[0], voiced[0]
    for i in voiced[1:]:
        if (i - prev) * 10 >= GAP_MS:
            segments.append((start, prev))
            start = i
        prev = i
    segments.append((start, prev))
    if len(segments) != expected:
        return None
    pad = PAD_MS // 10
    return [samples[max(0, a - pad) * win:min(len(loud), b + 1 + pad) * win].tobytes() for a, b in segments]


def encode_mp3(pcm):
    enc = lameenc.Encoder()
    enc.set_bit_rate(32)
    enc.set_in_sample_rate(SAMPLE_RATE)
    enc.set_channels(1)
    enc.set_quality(2)
    return enc.encode(pcm) + enc.flush()


def request(ssml_text, sub_key, region, output_format):
    req = urllib.request.Request(
        f"https://{region}.tts.speech.microsoft.com/cognitiveservices/v1",
        data=ssml_text.encode("utf-8"),
        headers={
            "Ocp-Apim-Subscription-Key": sub_key,
            "Content-Type": "application/ssml+xml",
            "X-Microsoft-OutputFormat": output_format,
            "User-Agent": "OgdenBasic",
        },
    )
    for attempt in range(12):
        try:
            with urllib.request.urlopen(req, timeout=120, context=SSL_CONTEXT) as resp:
                return resp.read()
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 502, 503) and attempt < 11:
                time.sleep(max(int(e.headers.get("Retry-After") or 0), min(2 ** attempt, 60)))
                continue
            raise
        except (urllib.error.URLError, TimeoutError, http.client.IncompleteRead, ConnectionError):
            # 大批量请求偶尔会断流，重试即可
            if attempt < 11:
                time.sleep(min(2 ** attempt, 60))
                continue
            raise


def key(voice, text):
    return hashlib.sha1(f"{voice}|{text.strip()}".encode("utf-8")).hexdigest()


def synthesize(text, voice, lang, sub_key, region):
    return request(ssml(text, voice, lang), sub_key, region, OUTPUT_FORMAT)


def write(target, data):
    partial = target.with_suffix(".part")
    partial.write_bytes(data)
    partial.replace(target)


def main():
    props = local_props()
    sub_key, region = props.get("azure.speech.key", ""), props.get("azure.speech.region", "")
    if not sub_key or not region:
        sys.exit("local.properties 缺少 azure.speech.key / azure.speech.region")
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    jobs = []
    for lang_group, texts in (("en", manifest["en"]),):
        for voice, lang in VOICES[lang_group]:
            out_dir = ASSETS / lang
            out_dir.mkdir(parents=True, exist_ok=True)
            for text in texts:
                target = out_dir / f"{key(voice, text)}.mp3"
                if not (target.exists() and target.stat().st_size > 0):
                    jobs.append((text, voice, lang, target))
    total = len(jobs)
    print(f"待生成 {total} 个，{'合并' if lameenc else '逐条'}模式", flush=True)
    done = [0]
    fallback = [0]
    lock = threading.Lock()

    def progress(n):
        with lock:
            before = done[0]
            done[0] += n
            if done[0] // 500 != before // 500 or done[0] == total:
                print(f"{done[0]}/{total}（退回逐条 {fallback[0]}）", flush=True)

    def run_single(job):
        text, voice, lang, target = job
        write(target, synthesize(text, voice, lang, sub_key, region))
        progress(1)

    def run_batch(batch):
        voice, lang = batch[0][1], batch[0][2]
        pcm = request(batch_ssml([j[0] for j in batch], voice, lang), sub_key, region, "raw-16khz-16bit-mono-pcm")
        parts = split_pcm(pcm, len(batch))
        if parts is None:
            with lock:
                fallback[0] += len(batch)
            for job in batch:
                run_single(job)
            return
        for (text, _, _, target), part in zip(batch, parts):
            write(target, encode_mp3(part))
        progress(len(batch))

    with ThreadPoolExecutor(max_workers=4) as pool:
        if lameenc:
            groups = {}
            for job in jobs:
                groups.setdefault(job[1], []).append(job)
            batches = [g[i:i + BATCH] for g in groups.values() for i in range(0, len(g), BATCH)]
            list(pool.map(run_batch, batches))
        else:
            list(pool.map(run_single, jobs))
    print("完成", flush=True)


if __name__ == "__main__":
    main()
