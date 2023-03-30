package br.com.frazo.ui.compose.materialv3

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import br.com.frazo.audio_services.player.AudioPlayerStatus
import br.com.frazo.audio_services.player.AudioPlayingData

@Composable
fun AudioPlayer(
    modifier: Modifier = Modifier,
    audioPlayingData: AudioPlayingData,
    playIcon: @Composable () -> Unit,
    pauseIcon: @Composable () -> Unit,
    deleteIcon: (@Composable () -> Unit)? = null,
    timeLabelStyle: TextStyle = LocalTextStyle.current,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onDelete: (() -> Unit)? = null
) {

    val progress = remember(audioPlayingData) {
        if (audioPlayingData.duration == 0L || audioPlayingData.status == AudioPlayerStatus.NOT_INITIALIZED)
            return@remember 0f
        with(audioPlayingData) {
            return@remember elapsed / duration.toFloat()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .animateContentSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.wrapContentSize(),
            onClick = {
                when (audioPlayingData.status) {
                    AudioPlayerStatus.NOT_INITIALIZED, AudioPlayerStatus.PAUSED -> onPlay()
                    AudioPlayerStatus.PLAYING -> onPause()
                }
            }) {
            when (audioPlayingData.status) {
                AudioPlayerStatus.NOT_INITIALIZED, AudioPlayerStatus.PAUSED -> playIcon()
                AudioPlayerStatus.PLAYING -> pauseIcon()
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(progress = progress)
            val minutes = audioPlayingData.elapsed / 1000 / 60
            val seconds = audioPlayingData.elapsed / 1000 % 60
            Text(
                text = "${minutes.toString().padStart(2, '0')}:${
                    seconds.toString().padStart(2, '0')
                }",
                style = timeLabelStyle
            )
        }
        deleteIcon?.let {
            IconButton(
                modifier = Modifier.wrapContentSize(),
                onClick = {
                    onDelete?.invoke()
                }) {
                it()
            }
        }
    }
}