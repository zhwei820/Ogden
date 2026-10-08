#!/usr/bin/env python3
"""合成口算小游戏的背景音乐到 app/src/main/assets/bgm/*.mp3。

旋律与伴奏都是原创（写在下面的曲谱里），用 GeneralUser GS 音色库（真实乐器采样，许可允许商用、无需署名）
经 fluidsynth 渲染。音色库约 30MB，不进仓库，用时下载：
  https://github.com/mrbumpy409/GeneralUser-GS
依赖 fluidsynth（brew install fluid-synth）与 ffmpeg。
用法：python3 scripts/gen_bgm.py <GeneralUser-GS.sf2>
"""
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/bgm"
TPB = 480  # ticks per beat

NOTE = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def pitch(name):
    """'C5' / 'F#4' / 'Bb3' → MIDI 编号"""
    base = NOTE[name[0]]
    rest = name[1:]
    if rest[0] == "#":
        base, rest = base + 1, rest[1:]
    elif rest[0] == "b":
        base, rest = base - 1, rest[1:]
    return 12 * (int(rest) + 1) + base


def chord_pitches(symbol):
    """'C' 'Am' 'D7' 'Bb' 'F#m' → 和弦音的音级（0..11），根音在前"""
    root = NOTE[symbol[0]]
    q = symbol[1:]
    if q[:1] in ("#", "b"):
        root += 1 if q[0] == "#" else -1
        q = q[1:]
    intervals = {"": [0, 4, 7], "m": [0, 3, 7], "7": [0, 4, 7, 10]}[q]
    return [(root + i) % 12 for i in intervals]


def voice(pcs, low, high):
    """把音级排进 [low, high] 区间，得到一个紧凑的和弦"""
    out = []
    for pc in pcs:
        p = low + (pc - low) % 12
        if p > high:
            p -= 12
        out.append(p)
    return sorted(out)


class Song:
    def __init__(self, bpm, beats_per_bar):
        self.bpm, self.bpb = bpm, beats_per_bar
        self.events = []  # (tick, order, bytes)；order 让同一时刻先关音再开音
        self.programs = {}

    def program(self, ch, prog):
        self.programs[ch] = prog

    def note(self, ch, p, start_beat, beats, vel):
        on = int(start_beat * TPB)
        off = int((start_beat + beats) * TPB) - 10
        self.events.append((on, 1, bytes([0x90 | ch, p, vel])))
        self.events.append((off, 0, bytes([0x80 | ch, p, 0])))

    def write(self, path):
        def vlq(n):
            out = [n & 0x7F]
            n >>= 7
            while n:
                out.append(0x80 | (n & 0x7F))
                n >>= 7
            return bytes(reversed(out))

        head = [(0, -1, b"\xff\x51\x03" + struct.pack(">I", 60_000_000 // self.bpm)[1:])]
        head += [(0, -1, bytes([0xC0 | ch, prog])) for ch, prog in self.programs.items()]
        # 混响适中、和声效果关闭，听感更干净
        head += [(0, -1, bytes([0xB0 | ch, 91, 40])) for ch in range(16)]
        data, last = b"", 0
        for tick, _, msg in sorted(head + self.events, key=lambda e: (e[0], e[1])):
            data += vlq(tick - last) + msg
            last = tick
        end = max(e[0] for e in self.events) + TPB * 2
        data += vlq(end - last) + b"\xff\x2f\x00"
        path.write_bytes(b"MThd" + struct.pack(">IHHH", 6, 0, 1, TPB) + b"MTrk" + struct.pack(">I", len(data)) + data)


# GM 音色（0 起）
PIANO, CELESTA, GLOCK, MARIMBA, XYLO, NYLON, STEEL, AC_BASS, PIZZ, HARP, CLARINET, FLUTE, RECORDER = \
    0, 8, 9, 12, 13, 24, 25, 32, 45, 46, 71, 73, 74
# GM 打击乐（通道 10）；不用沙锤，听着吵
KICK, TAMB = 35, 54


def melody(bars):
    """曲谱一行一小节：'E5 1, G5 .5, r 1'（音名 时值拍数，r 为休止）"""
    return [[(None if n == "r" else pitch(n), float(d)) for n, d in (tok.split() for tok in bar.split(","))] for bar in bars]


def arrange(song, form, lead, doubles, accomp, bass_prog, percussion, arpeggio=False):
    """form：[(和弦列表, 旋律小节)]；后半程用 doubles 的音色叠一层高八度旋律。
    arpeggio：伴奏弹八分音符分解和弦（竖琴式），否则在反拍弹柱式和弦"""
    ch_lead, ch_double, ch_acc, ch_bass, ch_drum = 0, 1, 2, 3, 9
    song.program(ch_lead, lead)
    song.program(ch_double, doubles)
    song.program(ch_acc, accomp)
    song.program(ch_bass, bass_prog)
    bpb = song.bpb
    half = len(form) // 2
    for i, (chords, notes) in enumerate(form):
        assert sum(d for _, d in notes) == bpb, f"第 {i + 1} 小节时值 {sum(d for _, d in notes)} ≠ {bpb}"
        start = i * bpb
        pos = start
        for p, d in notes:
            if p is not None:
                song.note(ch_lead, p, pos, d, 92)
                if i >= half:
                    song.note(ch_double, p + 12, pos, d, 58)
            pos += d
        span = bpb / len(chords)
        for k, sym in enumerate(chords):
            c0 = start + k * span
            pcs = chord_pitches(sym)
            root = 36 + (pcs[0] - 36) % 12  # C2..B2
            # 贝斯：根音落在强拍，整小节一个和弦时第三拍弹五度
            song.note(ch_bass, root, c0, min(span, 1), 80)
            if span >= 4:
                song.note(ch_bass, root + 7 if root + 7 <= 50 else root - 5, c0 + 2, 1, 72)
            tones = voice(pcs, 55, 67)
            if arpeggio:
                order = [0, 1, 2, 1] if len(tones) == 3 else [0, 1, 2, 3]
                for e in range(int(span * 2)):
                    song.note(ch_acc, tones[order[e % len(order)]], c0 + e / 2, 1, 48)
                continue
            # 和弦：四拍子弹在 2、4 拍（反拍），三拍子弹在 2、3 拍
            offbeats = [b for b in range(int(span)) if b % 2 == 1] if bpb == 4 else [b for b in range(int(span)) if b > 0]
            for b in offbeats:
                for j, t in enumerate(tones):
                    # 吉他式轻微扫弦
                    song.note(ch_acc, t, c0 + b + j * 0.02, 0.8, 52)
        if percussion:
            song.note(ch_drum, KICK, start, 0.5, 58)
            for b in range(bpb):
                if (bpb == 4 and b in (1, 3)) or (bpb == 3 and b > 0):
                    song.note(ch_drum, TAMB, start + b, 0.5, 42)
    # 收尾：主和弦延长一小节
    end = len(form) * bpb
    tonic = chord_pitches(form[-1][0][-1])
    for t in voice(tonic, 55, 67):
        song.note(ch_acc, t, end, bpb, 60)
    song.note(ch_bass, 36 + (tonic[0] - 36) % 12, end, bpb, 75)


def track_picnic():
    """A 大调，竖笛 + 竖琴分解和弦 + 拨弦，后半段钢片琴，野餐郊游"""
    a = (["A", "D", "E", "A", "F#m", "D", "E", "A"], melody([
        "C#5 .5, E5 .5, A5 1, E5 1, C#5 1", "D5 1, F#5 1, A5 1, F#5 1", "E5 1.5, D5 .5, B4 1, G#4 1", "A4 2, E4 1, r 1",
        "F#4 .5, A4 .5, C#5 1, F#5 1, E5 .5, C#5 .5", "D5 1, B4 .5, A4 .5, F#4 1, A4 1", "B4 1, C#5 .5, D5 .5, E5 1, G#4 1",
        "A4 3, r 1"]))
    b = (["D", "A", "Bm", "E", "D", "A", "E7", "A"], melody([
        "F#5 1, F#5 .5, E5 .5, D5 1, A4 1", "C#5 1, E5 1, A4 2", "D5 .5, C#5 .5, B4 1, F#4 1, B4 1", "G#4 1, B4 1, E5 2",
        "A5 1, F#5 .5, D5 .5, A4 1, D5 1", "C#5 1, E5 .5, C#5 .5, A4 2", "B4 .5, C#5 .5, D5 .5, E5 .5, G#5 1, E5 1",
        "A5 3, r 1"]))
    song = Song(120, 4)
    arrange(song, expand([a, a, b, a] * 2), RECORDER, CELESTA, HARP, PIZZ, True, arpeggio=True)
    return song


def track_marimba():
    """G 大调，马林巴 + 尼龙吉他 + 原声贝斯，蹦蹦跳跳"""
    a = (["G", "Em", "C", "D", "G", "Em", "C D", "G"], melody([
        "B4 .5, D5 .5, G5 1, D5 1, B4 1", "E5 1, D5 .5, B4 .5, G4 2", "C5 .5, D5 .5, E5 1, G5 1, E5 1",
        "D5 1.5, C5 .5, B4 1, A4 1", "B4 .5, D5 .5, G5 1, D5 1, B4 1", "E5 1, G5 1, E5 .5, D5 .5, B4 1",
        "C5 1, E5 1, D5 1, A4 1", "G4 3, r 1"]))
    b = (["C", "G", "C", "D", "Em", "C", "D", "G"], melody([
        "E5 1, E5 .5, F#5 .5, G5 1, E5 1", "D5 1, B4 1, G4 2", "E5 .5, D5 .5, C5 1, E5 1, G5 1",
        "F#5 1.5, E5 .5, D5 2", "G5 1, F#5 .5, E5 .5, B4 1, E5 1", "E5 1, D5 .5, C5 .5, G4 2",
        "A4 .5, B4 .5, C5 .5, D5 .5, E5 1, F#5 1", "G5 3, r 1"]))
    song = Song(126, 4)
    arrange(song, expand([a, a, b, a] * 2), MARIMBA, XYLO, NYLON, AC_BASS, True)
    return song


def track_waltz():
    """F 大调三拍子，长笛 + 钢弦吉他 + 拨弦，转圈圈"""
    a = (["F", "Bb", "C", "F", "F", "Bb", "C7", "F"], melody([
        "C5 1, A4 1, C5 1", "D5 1, F5 1, D5 1", "C5 1, Bb4 1, G4 1", "A4 2, F4 1",
        "A4 1, C5 1, F5 1", "F5 1, D5 1, Bb4 1", "G4 1, Bb4 1, E5 1", "F5 2, r 1"]))
    b = (["Dm", "Gm", "C", "F", "Dm", "Gm", "C7", "F"], melody([
        "F5 1, E5 1, D5 1", "D5 1, Bb4 1, G4 1", "E5 1, D5 1, C5 1", "C5 2, A4 1",
        "D5 1, F5 1, A5 1", "G5 1, D5 1, Bb4 1", "C5 1, E5 1, G5 1", "F5 2, r 1"]))
    song = Song(156, 3)
    arrange(song, expand([a, a, b, a] * 3), FLUTE, GLOCK, STEEL, PIZZ, True)
    return song


def track_duck():
    """D 大调，单簧管 + 木琴 + 钢琴，憨憨的小鸭子"""
    a = (["D", "D", "A7", "D", "G", "D", "A7", "D"], melody([
        "F#4 .5, A4 .5, D5 1, A4 .5, F#4 .5, A4 1", "B4 .5, A4 .5, F#4 1, D4 2", "E4 .5, G4 .5, C#5 1, A4 .5, G4 .5, E4 1",
        "F#4 1, A4 1, D5 2", "B4 .5, D5 .5, B4 .5, G4 .5, D4 1, G4 1", "A4 .5, F#4 .5, D4 1, F#4 1, A4 1",
        "G4 .5, F#4 .5, E4 .5, D4 .5, C#4 1, E4 1", "D4 3, r 1"]))
    b = (["G", "D", "E7", "A7", "G", "D", "A7", "D"], melody([
        "B4 1, B4 .5, C#5 .5, D5 1, B4 1", "A4 1, F#4 1, D4 2", "G#4 .5, B4 .5, E5 1, D5 1, B4 1", "C#5 1, A4 1, E4 1, G4 1",
        "B4 .5, A4 .5, G4 .5, F#4 .5, G4 1, B4 1", "A4 1, D5 1, F#4 2", "E4 1, G4 1, C#5 1, E5 1", "D5 3, r 1"]))
    song = Song(120, 4)
    arrange(song, expand([a, a, b, a] * 2), CLARINET, XYLO, PIANO, AC_BASS, True)
    return song


def expand(sections):
    """[(和弦8个, 旋律8小节)] → 每小节一项 (和弦列表, 旋律)"""
    return [(chords[k].split(), notes[k]) for chords, notes in sections for k in range(len(notes))]


TRACKS = {"picnic": track_picnic, "marimba": track_marimba, "waltz": track_waltz, "duck": track_duck}


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    sf2 = sys.argv[1]
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.mp3"):
        old.unlink()
    with tempfile.TemporaryDirectory() as tmp:
        for name, fn in TRACKS.items():
            mid, wav = Path(tmp) / f"{name}.mid", Path(tmp) / f"{name}.wav"
            fn().write(mid)
            subprocess.run(["fluidsynth", "-ni", "-q", "-g", "0.6", "-r", "44100", "-F", str(wav), sf2, str(mid)], check=True)
            target = OUT / f"{name}.mp3"
            subprocess.run(["ffmpeg", "-nostdin", "-y", "-loglevel", "error", "-i", str(wav),
                            "-af", "loudnorm=I=-18:TP=-2", "-ac", "1", "-ar", "44100", "-b:a", "64k", str(target)], check=True)
            print(f"{target.relative_to(ROOT)}", flush=True)


if __name__ == "__main__":
    main()
