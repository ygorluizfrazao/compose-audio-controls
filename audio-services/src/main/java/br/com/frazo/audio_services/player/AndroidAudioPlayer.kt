package br.com.frazo.audio_services.player

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

class AndroidAudioPlayer(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AudioPlayer {

    private val UPDATE_DATA_INTERVAL_MILLIS = 200L

    private var player: MediaPlayer? = null
    private val _audioPlayingData =
        MutableStateFlow(AudioPlayingData(AudioPlayerStatus.NOT_INITIALIZED, 0, 0))
    private val audioPlayingData = _audioPlayingData.asStateFlow()
    private var flowJob: Job? = null
    private var currentUniqueId: String? = null

    override fun start(file: File): Flow<AudioPlayingData> {

        stop()

        player = MediaPlayer.create(context, file.toUri()).apply {
            currentUniqueId = UUID.randomUUID().toString()
            start()
            setOnCompletionListener {
                this@AndroidAudioPlayer.stop()
            }
        }

        _audioPlayingData.value = _audioPlayingData.value.copy(status = AudioPlayerStatus.PLAYING)
        flowJob = CoroutineScope(dispatcher).launch {
            currentUniqueId?.let {
                startFlowing(it)
                    .collectLatest {
                        _audioPlayingData.value = it
                    }
            }
        }
        return audioPlayingData
    }

    override fun pause() {
        player?.let {
            it.pause()
            _audioPlayingData.value =
                _audioPlayingData.value.copy(status = AudioPlayerStatus.PAUSED)
        }
    }

    override fun resume() {
        player?.let {
            if (!it.isPlaying)
                it.start()
            _audioPlayingData.value =
                _audioPlayingData.value.copy(status = AudioPlayerStatus.PLAYING)
        }
    }

    override fun stop() {

        flowJob?.cancel()
        player?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset()
            it.release()
        }
        player = null
        _audioPlayingData.value = AudioPlayingData(AudioPlayerStatus.NOT_INITIALIZED, 0, 0)
    }

    override fun seek(position: Long) {
        player?.let {
            if(_audioPlayingData.value.status!=AudioPlayerStatus.NOT_INITIALIZED){
                it.seekTo(position.toInt())
            }
        }
    }


    private fun startFlowing(uniqueId: String): Flow<AudioPlayingData> {
        return flow {
            while (true) {
                //finishes the flow
                if (player == null) {
                    Log.d("Audio Player: ","Exit")
                    return@flow
                }

                player?.let {
                    if (uniqueId != currentUniqueId) {
                        Log.d("Audio Player: ","Exit")
                        return@flow
                    }
                    val newData = _audioPlayingData.value.copy(
                        duration = it.duration.toLong(),
                        elapsed = it.currentPosition.toLong()
                    )
                    emit(newData)
                }
                delay(UPDATE_DATA_INTERVAL_MILLIS)
            }
        }
    }
}