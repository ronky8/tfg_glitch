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

    /**
     * Reproduce la música del menú. Si ya está sonando, no hace nada.
     */
    fun playMenuMusic(context: Context) {
        playMusic(context, R.raw.music_menu)
    }

    /**
     * Reproduce la música de la partida.
     */
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

    /**
     * Detiene la música de fondo.
     */
    fun stopMusic() {
        backgroundMusicPlayer?.stop()
        backgroundMusicPlayer?.release()
        backgroundMusicPlayer = null
    }

    /**
     * Pausa la música (cuando la app va a segundo plano).
     */
    fun pauseMusic() {
        backgroundMusicPlayer?.pause()
    }

    /**
     * Reanuda la música (cuando la app vuelve al primer plano).
     */
    fun resumeMusic() {
        backgroundMusicPlayer?.start()
    }

    /**
     * Libera los recursos cuando la app se cierra.
     */
    fun release() {
        stopMusic()
        isInitialized = false
    }
}

