package com.example.ogdenkids

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/** [start] / [duration] 单位为秒；[click] 为节拍器的短促敲击声 */
data class Tone(val start: Double, val duration: Double, val midi: Int, val volume: Float = 0.8f, val click: Boolean = false)

fun midiFrequency(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

/**
 * 乐理课的发声都在本地实时合成，不依赖音频素材，离线可用。
 *
 * 一条流式 AudioTrack 常开，由混音线程把各个声部叠加写入。不能每个音单独建 AudioTrack、
 * 来新音就 stop 旧的：波形在非零处被硬切断会爆音。停止一律走短淡出。
 */
class ToneSynth {
    private val lock = Object()
    private val voices = ArrayList<Voice>()
    // 已交给混音的帧数，新声部的起点以它为基准
    private var frame = 0L
    @Volatile private var running = true

    init {
        Thread(::loop, "ToneSynth").apply { priority = Thread.MAX_PRIORITY }.start()
    }

    /** 播放一段音序，先淡出正在响的 */
    fun play(tones: List<Tone>) = synchronized(lock) {
        fadeAll()
        add(tones)
    }

    /** 单个音（按琴键），与正在响的音自然叠加 */
    fun note(midi: Int, duration: Double = 0.9, volume: Float = 0.8f) = synchronized(lock) {
        add(listOf(Tone(0.0, duration, midi, volume)))
    }

    fun stop() = synchronized(lock) { fadeAll() }

    fun release() = synchronized(lock) {
        running = false
        lock.notifyAll()
    }

    private fun add(tones: List<Tone>) {
        tones.forEach { voices += Voice(it, frame + (it.start * SampleRate).toLong()) }
        lock.notifyAll()
    }

    private fun fadeAll() = voices.forEach { it.fade() }

    private fun loop() {
        val minBuf = AudioTrack.getMinBufferSize(SampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuf, Chunk * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val mix = FloatArray(Chunk)
        val out = ShortArray(Chunk)
        var idle = 0
        track.play()
        try {
            while (running) {
                synchronized(lock) {
                    // 静默一秒就暂停，有新音再继续，免得一直空转
                    if (voices.isEmpty() && idle > SampleRate) {
                        track.pause()
                        while (voices.isEmpty() && running) lock.wait()
                        track.play()
                        idle = 0
                    }
                    mix.fill(0f)
                    voices.forEach { it.render(mix, frame) }
                    voices.removeAll { it.done }
                    frame += Chunk
                    idle = if (voices.isEmpty()) idle + Chunk else 0
                }
                if (!running) break
                // 软限幅：和弦叠加时不削波
                for (i in 0 until Chunk) out[i] = (tanh(mix[i].toDouble()) * Short.MAX_VALUE).toInt().toShort()
                track.write(out, 0, Chunk)
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private class Voice(val tone: Tone, val startFrame: Long) {
        private val w = 2 * PI * midiFrequency(tone.midi) / SampleRate
        private val durFrames = (tone.duration * SampleRate).toLong()
        private val tailFrames = (Tail * SampleRate).toLong()
        private val attackFrames = 0.005 * SampleRate
        private val k1 = exp(-1.5 / SampleRate)
        private val k2 = exp(-6.0 / SampleRate)
        private val kClick = exp(-60.0 / SampleRate)
        private var e1 = 1.0
        private var e2 = 1.0
        private var t = 0L
        private var gain = 1.0
        private var fading = false
        var done = false
            private set

        fun fade() {
            fading = true
        }

        fun render(mix: FloatArray, frame0: Long) {
            for (i in mix.indices) {
                if (frame0 + i < startFrame) {
                    // 还没轮到就被停掉的，直接作废
                    if (fading) { done = true; return }
                    continue
                }
                if (t > durFrames + tailFrames || gain <= 0) { done = true; return }
                if (fading) gain -= 1.0 / (FadeSeconds * SampleRate)
                val v = if (tone.click) {
                    sin(w * t) * e1
                } else {
                    val attack = min(1.0, t / attackFrames)
                    val release = if (t > durFrames) 1 - (t - durFrames).toDouble() / tailFrames else 1.0
                    // 叠几个泛音，听起来像简化的钢琴，而不是纯正弦波的「嘀」
                    attack * release * (0.7 * e1 + 0.3 * e2) *
                        (sin(w * t) + 0.45 * sin(2 * w * t) + 0.2 * sin(3 * w * t) + 0.1 * sin(4 * w * t)) / 1.75
                }
                mix[i] += (v * tone.volume * 0.4 * max(0.0, gain)).toFloat()
                e1 *= if (tone.click) kClick else k1
                e2 *= k2
                t++
            }
        }
    }

    private companion object {
        const val SampleRate = 22050
        const val Chunk = 256
        const val Tail = 0.3
        const val FadeSeconds = 0.02
    }
}

fun melody(notes: List<Pair<Int, Double>>, bpm: Int = 96, volume: Float = 0.8f): List<Tone> {
    val beat = 60.0 / bpm
    var at = 0.0
    return notes.map { (midi, beats) ->
        Tone(at, beats * beat * 0.95, midi, volume).also { at += beats * beat }
    }
}

/** [meter] 拍一小节：第一拍强，四拍子第三拍次强，其余弱 */
fun metronome(beats: Int, bpm: Int, meter: Int, start: Double = 0.0): List<Tone> = (0 until beats).map { i ->
    val pos = i % meter
    val volume = when {
        pos == 0 -> 1f
        meter == 4 && pos == 2 -> 0.55f
        else -> 0.3f
    }
    Tone(start + i * 60.0 / bpm, 0.08, if (pos == 0) 96 else 89, volume, click = true)
}

fun chord(midis: List<Int>, start: Double = 0.0, duration: Double = 1.4): List<Tone> =
    midis.map { Tone(start, duration, it, 0.6f) }

/** 先分解再齐奏 */
fun brokenChord(midis: List<Int>): List<Tone> =
    midis.mapIndexed { i, m -> Tone(i * 0.35, 0.5, m, 0.7f) } + chord(midis, start = midis.size * 0.35 + 0.15)
