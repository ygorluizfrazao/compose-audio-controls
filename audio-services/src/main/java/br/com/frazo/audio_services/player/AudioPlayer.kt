package br.com.frazo.audio_services.player

import kotlinx.coroutines.flow.Flow
import java.io.File

interface AudioPlayer {

    fun start(file: File): Flow<AudioPlayingData>

    fun pause()

    fun resume()

    fun stop()

    fun seek(position: Long)

}