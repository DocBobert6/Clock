package com.example.clock.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import kotlin.random.Random

data class SoundItem(
    val id: String, // URI string or procedural identifier
    val name: String,
    val isProcedural: Boolean = true
)

object ProceduralSoundGenerator {

    val PROCEDURAL_ALARM_TONES = listOf(
        SoundItem("procedural:morning_bell", "Morning Bell"),
        SoundItem("procedural:cosmic_pulse", "Cosmic Pulse"),
        SoundItem("procedural:beacon", "Beacon"),
        SoundItem("procedural:radiance", "Radiance"),
        SoundItem("procedural:chime_stream", "Chime Stream"),
        SoundItem("procedural:zen_breeze", "Zen Breeze"),
        SoundItem("procedural:starlight", "Starlight"),
        SoundItem("procedural:marimba_rise", "Marimba Rise")
    )

    val SLEEP_SOUNDS = listOf(
        "Gentle Rain",
        "Ocean Waves",
        "White Noise",
        "Pink Noise",
        "Brown Noise",
        "Ambient Fan"
    )

    private const val SAMPLE_RATE = 44100

    /**
     * Ensures all 8 procedural WAV files are created in context.filesDir/sounds/
     */
    fun ensureSoundsGenerated(context: Context) {
        val soundDir = File(context.filesDir, "sounds")
        if (!soundDir.exists()) soundDir.mkdirs()

        PROCEDURAL_ALARM_TONES.forEach { sound ->
            val toneKey = sound.id.removePrefix("procedural:")
            val targetFile = File(soundDir, "$toneKey.wav")
            if (!targetFile.exists() || targetFile.length() < 1000) {
                generateToneWav(toneKey, targetFile)
            }
        }
    }

    fun getSoundFileUri(context: Context, soundUriString: String): Uri? {
        if (soundUriString.startsWith("procedural:")) {
            val key = soundUriString.removePrefix("procedural:")
            val file = File(File(context.filesDir, "sounds"), "$key.wav")
            if (file.exists()) return Uri.fromFile(file)
            // Generate on demand if missing
            ensureSoundsGenerated(context)
            if (file.exists()) return Uri.fromFile(file)
        } else if (soundUriString.startsWith("content://") || soundUriString.startsWith("file://")) {
            return Uri.parse(soundUriString)
        }
        return null
    }

    private fun generateToneWav(type: String, targetFile: File) {
        val durationSeconds = 3.5
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val buffer = ShortArray(numSamples)

        when (type) {
            "morning_bell" -> {
                // Bell frequencies: fundamental + harmonics with exponential decay
                val freqs = doubleArrayOf(440.0, 880.0, 1320.0, 2200.0)
                val decays = doubleArrayOf(1.2, 2.5, 4.0, 6.0)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    var sample = 0.0
                    for (f in freqs.indices) {
                        sample += sin(2.0 * PI * freqs[f] * t) * exp(-decays[f] * t) * (1.0 / (f + 1))
                    }
                    // Repeated strike at 1.5s
                    if (t > 1.5) {
                        val t2 = t - 1.5
                        for (f in freqs.indices) {
                            sample += sin(2.0 * PI * (freqs[f] * 1.25) * t2) * exp(-decays[f] * t2) * (0.8 / (f + 1))
                        }
                    }
                    buffer[i] = (sample.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
                }
            }
            "cosmic_pulse" -> {
                // Warm pulsating harmonic drone
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val lfo = 0.5 + 0.5 * sin(2.0 * PI * 2.0 * t) // 2 Hz pulse
                    val s1 = sin(2.0 * PI * 220.0 * t)
                    val s2 = sin(2.0 * PI * 330.0 * t) * 0.5
                    val s3 = sin(2.0 * PI * 440.0 * t) * 0.3
                    val sample = (s1 + s2 + s3) * lfo * exp(-0.4 * t)
                    buffer[i] = (sample.coerceIn(-1.0, 1.0) * 26000).toInt().toShort()
                }
            }
            "beacon" -> {
                // Crisp dual-tone marimba chime: 523Hz (C5) and 659Hz (E5)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    var s = 0.0
                    if (t < 1.0) {
                        s += sin(2.0 * PI * 523.25 * t) * exp(-5.0 * t)
                    }
                    if (t in 0.25..1.5) {
                        val t2 = t - 0.25
                        s += sin(2.0 * PI * 659.25 * t2) * exp(-5.0 * t2)
                    }
                    if (t in 0.5..2.0) {
                        val t3 = t - 0.5
                        s += sin(2.0 * PI * 783.99 * t3) * exp(-5.0 * t3)
                    }
                    buffer[i] = (s.coerceIn(-1.0, 1.0) * 29000).toInt().toShort()
                }
            }
            "radiance" -> {
                // Uplifting triad arpeggio (C - G - C - E)
                val notes = doubleArrayOf(261.63, 392.00, 523.25, 659.25)
                val starts = doubleArrayOf(0.0, 0.3, 0.6, 0.9)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    var s = 0.0
                    for (n in notes.indices) {
                        if (t >= starts[n]) {
                            val noteT = t - starts[n]
                            s += sin(2.0 * PI * notes[n] * noteT) * exp(-3.0 * noteT)
                        }
                    }
                    buffer[i] = (s.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
                }
            }
            "chime_stream" -> {
                // Cascading pentatonic sparkle (5 notes)
                val notes = doubleArrayOf(587.33, 659.25, 783.99, 880.00, 1046.50)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    var s = 0.0
                    for (idx in notes.indices) {
                        val start = idx * 0.18
                        if (t >= start) {
                            val dt = t - start
                            s += sin(2.0 * PI * notes[idx] * dt) * exp(-4.5 * dt) * 0.6
                        }
                    }
                    buffer[i] = (s.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
                }
            }
            "zen_breeze" -> {
                // Tibetan singing bowl frequency with soft overtone
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val s = sin(2.0 * PI * 216.0 * t) * exp(-0.6 * t) +
                            sin(2.0 * PI * 648.0 * t) * 0.35 * exp(-1.2 * t)
                    buffer[i] = (s.coerceIn(-1.0, 1.0) * 27000).toInt().toShort()
                }
            }
            "starlight" -> {
                // High frequency celestial chime
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val s1 = sin(2.0 * PI * 1318.51 * t) * exp(-3.5 * t)
                    val s2 = if (t > 0.4) sin(2.0 * PI * 1760.00 * (t - 0.4)) * exp(-3.5 * (t - 0.4)) else 0.0
                    buffer[i] = ((s1 + s2).coerceIn(-1.0, 1.0) * 25000).toInt().toShort()
                }
            }
            else -> { // marimba_rise
                val notes = doubleArrayOf(349.23, 440.00, 523.25, 698.46)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    var s = 0.0
                    for (n in notes.indices) {
                        val st = n * 0.2
                        if (t >= st) {
                            val dt = t - st
                            s += sin(2.0 * PI * notes[n] * dt) * exp(-6.0 * dt)
                        }
                    }
                    buffer[i] = (s.coerceIn(-1.0, 1.0) * 28000).toInt().toShort()
                }
            }
        }

        writeWavFile(targetFile, buffer, SAMPLE_RATE)
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val totalAudioLen = samples.size * 2
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size
            header.putShort(1) // PCM format
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort((channels * 2).toShort()) // BlockAlign
            header.putShort(16) // Bits per sample
            header.put("data".toByteArray())
            header.putInt(totalAudioLen)

            out.write(header.array())

            val audioBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                audioBuffer.putShort(s)
            }
            out.write(audioBuffer.array())
        }
    }
}

/**
 * Procedural Sleep Sound Player using AudioTrack.
 * Generates continuous gapless real-time noise (White, Pink, Brown, Rain, Ocean, Fan).
 */
class SleepSoundEngine {
    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    var isPlaying: Boolean = false
        private set

    @Volatile
    var currentSound: String = "Gentle Rain"

    @Volatile
    var volume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    fun start(soundName: String, initialVolume: Float = 0.7f) {
        stop()
        currentSound = soundName
        volume = initialVolume
        isPlaying = true

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

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
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(volume)
        track.play()
        audioTrack = track

        job = scope.launch {
            val chunk = ShortArray(1024)
            var b0 = 0.0
            var b1 = 0.0
            var b2 = 0.0
            var b3 = 0.0
            var b4 = 0.0
            var b5 = 0.0
            var b6 = 0.0
            var lastBrown = 0.0
            var wavePhase = 0.0
            var rainEnvelope = 1.0

            while (isActive && isPlaying) {
                for (i in chunk.indices) {
                    val white = (Random.nextDouble() * 2.0 - 1.0)
                    val sample: Double = when (currentSound) {
                        "White Noise" -> white * 0.4
                        "Pink Noise" -> {
                            // Paul Kellet's filter method
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.08
                            b6 = white * 0.115926
                            pink
                        }
                        "Brown Noise" -> {
                            // Integrated Brownian random walk with high-pass leak
                            lastBrown = (lastBrown + (0.02 * white)) / 1.02
                            lastBrown * 2.5
                        }
                        "Gentle Rain" -> {
                            // Pink noise background + random droplet transient pops
                            b0 = 0.99 * b0 + white * 0.1
                            val hiss = b0 * 0.5
                            val isDrop = Random.nextInt(1000) > 994
                            val drop = if (isDrop) (Random.nextDouble() * 2.0 - 1.0) * 0.7 else 0.0
                            (hiss + drop) * 0.5
                        }
                        "Ocean Waves" -> {
                            // Brown noise modulated with slow 0.12 Hz swell cycle
                            wavePhase += 2.0 * PI * 0.12 / sampleRate
                            if (wavePhase > 2.0 * PI) wavePhase -= 2.0 * PI
                            val swell = (sin(wavePhase) * 0.5 + 0.5).pow(2.0)
                            lastBrown = (lastBrown + (0.03 * white)) / 1.015
                            lastBrown * (0.2 + 0.8 * swell) * 2.2
                        }
                        "Ambient Fan" -> {
                            // Low-frequency rumble + 120Hz hum + smooth air flow
                            wavePhase += 2.0 * PI * 60.0 / sampleRate
                            if (wavePhase > 2.0 * PI) wavePhase -= 2.0 * PI
                            val hum = sin(wavePhase) * 0.15
                            lastBrown = (lastBrown + (0.02 * white)) / 1.02
                            (lastBrown * 1.5 + hum + white * 0.1) * 0.5
                        }
                        else -> white * 0.3
                    }
                    chunk[i] = (sample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                }
                track.write(chunk, 0, chunk.size)
            }
        }
    }

    fun stop() {
        isPlaying = false
        job?.cancel()
        job = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
