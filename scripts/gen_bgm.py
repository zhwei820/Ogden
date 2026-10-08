#!/usr/bin/env python3
"""合成口算小游戏的背景音乐到 app/src/main/assets/bgm/*.mp3（自己生成，无版权问题）。

每首：大调、快节奏、旋律 + 贝斯 + 鼓，AABA 结构。固定随机种子，重跑结果一致。
依赖 numpy 与 ffmpeg。用法：python3 scripts/gen_bgm.py
"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/bgm"
SR = 22050

# (文件名, 主音 MIDI, 速度 BPM, 和弦进行(相对主音的级数), 种子)
TRACKS = [
    ("happy_1", 60, 132, [0, 5, 7, 0], 1),
    ("happy_2", 62, 140, [0, 9, 5, 7], 2),
    ("happy_3", 65, 124, [0, 5, 0, 7], 3),
    ("happy_4", 67, 136, [0, 7, 9, 5], 4),
]
MAJOR = [0, 2, 4, 5, 7, 9, 11]


def freq(midi):
    return 440.0 * 2 ** ((midi - 69) / 12)


def env(n, attack=0.005, release=0.06):
    e = np.ones(n)
    a, r = int(SR * attack), min(int(SR * release), n // 2)
    if a:
        e[:a] = np.linspace(0, 1, a)
    if r:
        e[-r:] = np.linspace(1, 0, r)
    return e


def square(f, n, duty=0.25):
    t = np.arange(n) / SR
    return np.where((t * f) % 1 < duty, 1.0, -1.0)


def triangle(f, n):
    t = np.arange(n) / SR
    return 2 * np.abs(2 * ((t * f) % 1) - 1) - 1


def chord_tones(root):
    # 大调里该级数上的三和弦（取自然音阶）
    i = MAJOR.index(root % 12) if root % 12 in MAJOR else 0
    return [MAJOR[(i + k) % 7] + 12 * ((i + k) // 7) for k in (0, 2, 4)]


def melody_bar(rng, chord, prev):
    """一小节 8 个八分音符位置，返回 [(音高, 长度八分音符数)]；强拍落和弦音，弱拍走音阶。"""
    notes, pos = [], 0
    scale = [d + 12 * o for o in (0, 1) for d in MAJOR] + [24]
    tones = [t for t in scale if t % 12 in {c % 12 for c in chord}]
    while pos < 8:
        length = rng.choice([1, 1, 2, 2, 3]) if pos % 2 == 0 else 1
        length = min(length, 8 - pos)
        pool = tones if pos % 4 == 0 else scale
        near = [p for p in pool if abs(p - prev) <= 4] or pool
        pitch = int(rng.choice(near))
        if rng.random() < 0.08 and pos != 0:
            pitch = None  # 偶尔空一拍，听起来有呼吸
        notes.append((pitch, length))
        prev = pitch if pitch is not None else prev
        pos += length
    return notes, prev


def render(tonic, bpm, progression, seed):
    rng = np.random.default_rng(seed)
    eighth = int(SR * 60 / bpm / 2)
    bar = eighth * 8
    # A、B 两段各 4 小节，AABA 共 16 小节，整首重复 2 遍
    sections = {}
    prev = 7
    for name in "AB":
        prog = progression if name == "A" else progression[2:] + progression[:2]
        bars = []
        for root in prog:
            notes, prev = melody_bar(rng, chord_tones(root), prev)
            bars.append((root, notes))
        sections[name] = bars
    form = [b for s in "AABA" * 2 for b in sections[s]]
    total = bar * len(form)
    lead = np.zeros(total)
    bass = np.zeros(total)
    drums = np.zeros(total)
    noise = rng.uniform(-1, 1, eighth)
    for i, (root, notes) in enumerate(form):
        start = i * bar
        pos = start
        for pitch, length in notes:
            n = length * eighth
            if pitch is not None:
                lead[pos:pos + n] += square(freq(tonic + 12 + pitch), n) * env(n)
            pos += n
        # 贝斯：根音、五度交替的八分音符
        for k in range(8):
            p = tonic - 12 + root + (7 if k % 4 == 2 else 0)
            s = start + k * eighth
            bass[s:s + eighth] += triangle(freq(p), eighth) * env(eighth, release=0.03)
        # 鼓：1、3 拍底鼓，2、4 拍军鼓，每个八分音符踩镲
        for k in range(8):
            s = start + k * eighth
            hat = noise[:eighth // 4] * np.exp(-np.linspace(0, 12, eighth // 4))
            drums[s:s + len(hat)] += 0.25 * hat
            if k in (0, 4):
                n = eighth
                t = np.arange(n) / SR
                kick = np.sin(2 * np.pi * (120 * np.exp(-t * 30)) * t) * np.exp(-t * 18)
                drums[s:s + n] += 0.9 * kick
            if k in (2, 6):
                n = eighth
                snare = noise[:n] * np.exp(-np.linspace(0, 8, n))
                drums[s:s + n] += 0.45 * snare
    mix = 0.30 * lead + 0.45 * bass + 0.5 * drums
    mix /= np.max(np.abs(mix)) * 1.1
    # 首尾淡入淡出，循环衔接不爆音
    fade = int(SR * 0.05)
    mix[:fade] *= np.linspace(0, 1, fade)
    mix[-fade:] *= np.linspace(1, 0, fade)
    return (mix * 32767).astype(np.int16)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, tonic, bpm, prog, seed in TRACKS:
        pcm = render(tonic, bpm, prog, seed)
        with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
            with wave.open(tmp.name, "wb") as w:
                w.setnchannels(1)
                w.setsampwidth(2)
                w.setframerate(SR)
                w.writeframes(pcm.tobytes())
            target = OUT / f"{name}.mp3"
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-b:a", "64k", str(target)], check=True)
        print(f"{target.relative_to(ROOT)} {len(pcm) / SR:.0f}s", flush=True)


if __name__ == "__main__":
    sys.exit(main())
