package com.pingu.tfg_glitch.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.pingu.tfg_glitch.R

/**
 * Un objeto singleton para gestionar la música de fondo.
 */
object SoundManager {

    private var backgroundMusicPlayer: MediaPlayer? = null
    private var isInitialized = false

    /**
     * Inicializa el gestor.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
    }

    fun playMenuMusic(context: Context) {
        playMusic(context, R.raw.music_menu)
    }

    fun playGameMusic(context: Context) {
        playMusic(context, R.raw.music_game)
    }

    private fun playMusic(context: Context, musicResId: Int) {
        // Si ya hay música, la detenemos primero
        backgroundMusicPlayer?.stop()
        backgroundMusicPlayer?.release()

        backgroundMusicPlayer = MediaPlayer.create(context, musicResId).apply {
            isLooping = true // Para que la música se repita en bucle
            setVolume(0.5f, 0.5f) // Volumen a la mitad (ajusta a tu gusto)
            start()
        }
    }

    fun stopMusic() {
        backgroundMusicPlayer?.stop()
        backgroundMusicPlayer?.release()
        backgroundMusicPlayer = null
    }

    fun pauseMusic() {
        backgroundMusicPlayer?.pause()
    }

    fun resumeMusic() {
        backgroundMusicPlayer?.start()
    }

    fun release() {
        stopMusic()
        isInitialized = false
    }
}

