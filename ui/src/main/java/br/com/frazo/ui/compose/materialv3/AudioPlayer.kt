package br.com.frazo.ui.compose.materialv3

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.frazo.audio_services.player.AudioPlayerStatus
import br.com.frazo.audio_services.player.AudioPlayingData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayer(
    modifier: Modifier = Modifier,
    audioPlayingData: AudioPlayingData,
    audioPlayerParams: AudioPlayerParams = rememberAudioPlayerParams(),
    audioPlayerCallbacks: AudioPlayerCallbacks
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
                    AudioPlayerStatus.NOT_INITIALIZED, AudioPlayerStatus.PAUSED -> audioPlayerCallbacks.onPlay()
                    AudioPlayerStatus.PLAYING -> audioPlayerCallbacks.onPause()
                }
            }) {
            when (audioPlayingData.status) {
                AudioPlayerStatus.NOT_INITIALIZED, AudioPlayerStatus.PAUSED -> audioPlayerParams.playIcon()
                AudioPlayerStatus.PLAYING -> audioPlayerParams.pauseIcon()
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Slider(
                value = progress,
                onValueChange = audioPlayerCallbacks.onSeekPosition,
                valueRange = (0f..1f),
                thumb = {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = audioPlayerParams.timeContainerModifier
                        ) {
                            audioPlayerParams.timeLabelContent(
                                audioPlayingData.elapsed,
                                audioPlayingData.duration
                            )
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                },
                track = {
                    SliderDefaults.Track(
                        sliderPositions = it, colors = SliderDefaults.colors(
                            inactiveTrackColor = LocalContentColor.current.copy(alpha = 0.5f)
                        )
                    )
                })
        }

        if (audioPlayerParams.endIcon != null && audioPlayerCallbacks.onEndIconClicked != null) {
            IconButton(
                modifier = Modifier
                    .wrapContentSize(),
                onClick = {
                    audioPlayerCallbacks.onEndIconClicked.invoke()
                }) {
                audioPlayerParams.endIcon.invoke()
            }
        }
    }
}


@Composable
fun rememberAudioPlayerParams(
    timeLabelStyle: TextStyle = LocalTextStyle.current.copy(
        color = contentColorFor(backgroundColor = LocalTextSelectionColors.current.handleColor),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    ),
    timeContainerModifier: Modifier = Modifier
        .background(
            LocalTextSelectionColors.current.handleColor,
            shape = RoundedCornerShape(6.dp)
        )
        .shadow(
            elevation = 10.dp
        )
        .padding(2.dp),
    timeLabelContent: (@Composable (elapsedTime: Long, totalDuration: Long) -> Unit) = { elapsedTime, _ ->
        val minutes = elapsedTime / 1000 / 60
        val seconds = elapsedTime / 1000 % 60
        Text(
            text = "${minutes.toString().padStart(2, '0')}:${
                seconds.toString().padStart(2, '0')
            }",
            style = timeLabelStyle
        )
    },
    playIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play"
        )
    },
    pauseIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Pause,
            contentDescription = "Pause"
        )
    },
    endIcon: (@Composable () -> Unit)? = null,
): AudioPlayerParams {
    val params by remember {
        mutableStateOf(
            AudioPlayerParams(
                timeLabelStyle,
                timeContainerModifier,
                timeLabelContent,
                playIcon,
                pauseIcon,
                endIcon
            )
        )
    }
    return params
}

data class AudioPlayerParams(
    val timeLabelStyle: TextStyle,
    val timeContainerModifier: Modifier,
    val timeLabelContent: (@Composable (elapsedTime: Long, totalDuration: Long) -> Unit),
    val playIcon: @Composable () -> Unit,
    val pauseIcon: @Composable () -> Unit,
    val endIcon: (@Composable () -> Unit)?,
)

data class AudioPlayerCallbacks(
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onSeekPosition: (Float) -> Unit,
    val onEndIconClicked: (() -> Unit)? = null
)