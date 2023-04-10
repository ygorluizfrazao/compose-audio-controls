package br.com.frazo.audio_services.player

data class AudioPlayingData(val status: AudioPlayerStatus, val duration: Long, val elapsed: Long)

enum class AudioPlayerStatus{
    NOT_INITIALIZED, PLAYING, PAUSED
}