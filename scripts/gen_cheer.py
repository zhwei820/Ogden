#!/usr/bin/env python3
"""合成口算小游戏全对时的欢呼音效到 app/src/main/assets/sfx/cheer.mp3：铜管号角 + 镲 + 掌声欢呼。

不放进 assets/bgm —— BackgroundMusic 会把那里的每个 mp3 都当背景音乐随机播放。
用法：python3 scripts/gen_cheer.py <GeneralUser-GS.sf2>（音色库获取与依赖见 gen_bgm.py）
"""
import subprocess
import sys
import tempfile
from pathlib import Path

from gen_bgm import ROOT, Song, pitch

OUT = ROOT / "app/src/main/assets/sfx/cheer.mp3"
TRUMPET, BRASS, APPLAUSE = 56, 61, 126
CRASH, SPLASH, TIMPANI_ROLL = 49, 55, 38


def cheer():
    song = Song(bpm=150, beats_per_bar=4)
    song.program(0, TRUMPET)
    song.program(1, BRASS)
    song.program(2, APPLAUSE)
    # 号角：G-C-E-G 上行，落在长音 C 大三和弦
    for i, p in enumerate(["G4", "C5", "E5"]):
        song.note(0, pitch(p), i * 0.5, 0.45, 110)
    song.note(0, pitch("G5"), 1.5, 3, 120)
    for p in ["C4", "E4", "G4", "C5"]:
        song.note(1, pitch(p), 1.5, 3, 100)
    for b in [0, 0.5, 1.0]:
        song.note(9, TIMPANI_ROLL, b, 0.4, 90)
    song.note(9, CRASH, 1.5, 2, 120)
    song.note(9, SPLASH, 1.5, 2, 90)
    # 掌声欢呼：几个音高叠在一起更像一群人
    for p, start in [(60, 1.4), (64, 1.6), (67, 1.8), (72, 2.0)]:
        song.note(2, p, start, 6, 120)
    return song


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        mid, wav = Path(tmp) / "cheer.mid", Path(tmp) / "cheer.wav"
        cheer().write(mid)
        subprocess.run(["fluidsynth", "-ni", "-q", "-g", "0.6", "-r", "44100", "-F", str(wav), sys.argv[1], str(mid)], check=True)
        # 掌声在末尾 1 秒淡出，免得被 MIDI 关音切断
        subprocess.run(["ffmpeg", "-nostdin", "-y", "-loglevel", "error", "-i", str(wav),
                        "-af", "atrim=0:4.5,afade=t=out:st=3.5:d=1,loudnorm=I=-16:TP=-1.5",
                        "-ac", "1", "-ar", "44100", "-b:a", "96k", str(OUT)], check=True)
    print(OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
