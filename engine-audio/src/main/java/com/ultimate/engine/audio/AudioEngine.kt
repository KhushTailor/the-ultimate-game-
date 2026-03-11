package com.ultimate.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class AudioEngine(private val context: Context, maxStreams: Int = 16) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(maxStreams)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val effectIds = mutableMapOf<String, Int>()
    private var background: MediaPlayer? = null

    fun loadEffect(name: String, resId: Int) {
        effectIds[name] = soundPool.load(context, resId, 1)
    }

    fun playEffect(name: String, volume: Float = 1f, x: Float = 0f, z: Float = 0f) {
        val pan = (x / 10f).coerceIn(-1f, 1f)
        val left = (1f - pan).coerceIn(0f, 1f) * volume
        val right = (1f + pan).coerceIn(0f, 1f) * volume
        effectIds[name]?.let { soundPool.play(it, left, right, 1, 0, 1f - (z / 30f)) }
    }

    fun playMusic(resId: Int, looping: Boolean = true) {
        background?.release()
        background = MediaPlayer.create(context, resId).apply {
            isLooping = looping
            start()
        }
    }

    fun stopMusic() = background?.pause()

    fun release() {
        background?.release()
        soundPool.release()
    }
}
