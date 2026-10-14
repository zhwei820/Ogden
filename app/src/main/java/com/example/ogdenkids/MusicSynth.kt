package com.example.ogdenkids

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** [start] / [duration] 单位为秒；[click] 为节拍器的短促敲击声 */
data class Tone(val start: Double, val duration: Double, val midi: Int, val volume: Float = 0.8f, val click: Boolean = false)

fun midiFrequency(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

/** 乐理课的发声都在本地实时合成，不依赖音频素材，离线可用 */
class ToneSynth {
    private val executor = Executors.newSingleThreadExecutor()
    // 每次 play / stop 递增：渲染完发现已被更新的请求取代就丢弃
    private val generation = AtomicInteger()
    private var track: AudioTrack? = null

    fun play(tones: List<Tone>) {
        if (tones.isEmpty()) return
        val gen = generation.incrementAndGet()
        executor.execute {
            if (gen != generation.get()) return@execute
            val pcm = render(tones)
            if (gen != generation.get()) return@execute
            releaseTrack()
            val t = AudioTrack.Builder()
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            t.write(pcm, 0, pcm.size)
            t.play()
            track = t
        }
    }

    fun note(midi: Int, duration: Double = 0.9, volume: Float = 0.8f) = play(listOf(Tone(0.0, duration, midi, volume)))

    fun stop() {
        generation.incrementAndGet()
        executor.execute { releaseTrack() }
    }

    fun release() {
        stop()
        executor.shutdown()
    }

    private fun releaseTrack() {
        track?.let {
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }

    private fun render(tones: List<Tone>): ShortArray {
        val end = tones.maxOf { it.start + it.duration } + Tail
        val buf = FloatArray((end * SampleRate).toInt())
        for (tone in tones) {
            val from = (tone.start * SampleRate).toInt()
            val len = min(((tone.duration + Tail) * SampleRate).toInt(), buf.size - from)
            val w = 2 * PI * midiFrequency(tone.midi) / SampleRate
            for (i in 0 until len) {
                val t = i.toDouble() / SampleRate
                val v = if (tone.click) {
                    sin(w * i) * exp(-t * 60)
                } else {
                    val attack = min(1.0, t / 0.005)
                    val release = if (t > tone.duration) max(0.0, 1 - (t - tone.duration) / Tail) else 1.0
                    val env = attack * release * (0.7 * exp(-t * 1.5) + 0.3 * exp(-t * 6))
                    // 叠几个泛音，听起来像简化的钢琴，而不是纯正弦波的「嘀」
                    env * (sin(w * i) + 0.45 * sin(2 * w * i) + 0.2 * sin(3 * w * i) + 0.1 * sin(4 * w * i)) / 1.75
                }
                buf[from + i] += (v * tone.volume * 0.4).toFloat()
            }
        }
        // 只在会削波时整体压低；不做归一化，否则力度演示的强弱差别会被抹平
        val peak = buf.maxOfOrNull { kotlin.math.abs(it) } ?: 0f
        val scale = if (peak > 0.95f) 0.95f / peak else 1f
        return ShortArray(buf.size) { (buf[it] * scale * Short.MAX_VALUE).toInt().toShort() }
    }

    private companion object {
        const val SampleRate = 22050
        const val Tail = 0.3
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
