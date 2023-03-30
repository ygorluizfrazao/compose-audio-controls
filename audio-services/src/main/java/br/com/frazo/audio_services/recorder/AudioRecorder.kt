package br.com.frazo.audio_services.recorder

import kotlinx.coroutines.flow.Flow
import java.io.File

interface AudioRecorder {

    fun startRecording(outputFile: File): Flow<AudioRecordingData>

    fun stopRecording()

    fun pause()

    fun resume()

}