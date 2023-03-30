package br.com.frazo.audio_services.recorder

sealed class AudioRecordingData {
    object NotStarted: AudioRecordingData()
    data class Recording(val elapsedTime: Long, val maxAmplitudeInCycle: Int): AudioRecordingData()
    data class Paused(val elapsedTime: Long, val maxAmplitudeInCycle: Int): AudioRecordingData()
}