package br.com.frazo.audio_services.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AndroidAudioRecorder(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher
) :
    AudioRecorder {

    private val UPDATE_DATA_INTERVAL_MILLIS = 50L

    private var recorder: MediaRecorder? = null
    private var audioRecordingDataFlowID: String? = null
    private var _audioRecordingData =
        MutableStateFlow<AudioRecordingData>(AudioRecordingData.NotStarted)

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    override fun startRecording(outputFile: File): Flow<AudioRecordingData> {
        stopRecording()
        recorder = createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            val fos = FileOutputStream(outputFile)
            setOutputFile(fos.fd)
            prepare()
            start()
            fos.close()
        }

        UUID.randomUUID().toString().also {uuid->
            audioRecordingDataFlowID = uuid
            _audioRecordingData.value = AudioRecordingData.Recording(0, 0)
            CoroutineScope(dispatcher).launch {
                startFlowingAudioRecordingData(uuid)
                    .collectLatest {
                        _audioRecordingData.value = it
                    }
            }
        }

        return _audioRecordingData.asStateFlow()

    }

    override fun stopRecording() {
        audioRecordingDataFlowID = null
        recorder?.stop()
        recorder?.reset()
        recorder?.release()
        recorder = null
        _audioRecordingData.value = AudioRecordingData.NotStarted
    }

    override fun pause() {
        val currentData = _audioRecordingData.value
        if(currentData is AudioRecordingData.Recording) {
            recorder?.pause()
            _audioRecordingData.value = AudioRecordingData.Paused(
                currentData.elapsedTime,
                recorder?.maxAmplitude ?: 0
            )
        }
    }

    override fun resume() {
        val currentData = _audioRecordingData.value
        if(currentData is AudioRecordingData.Paused) {
            recorder?.resume()
            _audioRecordingData.value = AudioRecordingData.Recording(
                currentData.elapsedTime,
                recorder?.maxAmplitude ?: 0
            )
        }
    }

    private fun startFlowingAudioRecordingData(flowId: String): Flow<AudioRecordingData> {

        return flow {
            while (true) {
                //finishes the previous flow
                if (flowId != audioRecordingDataFlowID)
                    break

                val currentData = _audioRecordingData.value

                if(currentData is AudioRecordingData.NotStarted)
                    break

                if(currentData is AudioRecordingData.Recording) {
                    emit(
                        AudioRecordingData.Recording(
                            currentData.elapsedTime + UPDATE_DATA_INTERVAL_MILLIS,
                            recorder?.maxAmplitude ?: 0
                        )
                    )
                }
                delay(UPDATE_DATA_INTERVAL_MILLIS)
            }
        }
    }
}