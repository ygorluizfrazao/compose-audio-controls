package br.com.frazo.ui.compose.materialv3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import br.com.frazo.audio_services.recorder.AudioRecordingData
import br.com.frazo.ui.visualizer.MirrorWaveRecordingVisualizer
import br.com.frazo.ui.visualizer.RecordingVisualizer

@Composable
fun AudioRecorder(
    modifier: Modifier = Modifier,
    recordIcon: @Composable () -> Unit,
    stopIcon: @Composable () -> Unit,
    recordingWaveVisualizer: RecordingVisualizer = MirrorWaveRecordingVisualizer(),
    timeLabelStyle: TextStyle = LocalTextStyle.current,
    audioRecordingData: List<AudioRecordingData> = emptyList(),
    onRecordRequested: () -> Unit,
    onStopRequested: () -> Unit
) {
    var isRecording by rememberSaveable {
        mutableStateOf(false)
    }

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .animateContentSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.wrapContentSize(),
            onClick = {
                isRecording = if (isRecording) {
                    onStopRequested()
                    false
                } else {
                    onRecordRequested()
                    true
                }
            }) {
            if (isRecording)
                stopIcon()
            else
                recordIcon()
        }

        AnimatedVisibility(
            visible = audioRecordingData.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Row(
                modifier = modifier
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val amplitudes = audioRecordingData.map {
                    when (it) {
                        AudioRecordingData.NotStarted -> 0f
                        is AudioRecordingData.Paused -> it.maxAmplitudeInCycle.toFloat()
                        is AudioRecordingData.Recording -> it.maxAmplitudeInCycle.toFloat()
                    }
                }
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 32.dp)
                        .padding(end = 12.dp)
                        .drawWithContent {
                            drawIntoCanvas { canvas ->
                                recordingWaveVisualizer.drawGraphics(
                                    canvas = canvas.nativeCanvas,
                                    amplitudes = amplitudes,
                                    width = size.width,
                                    height = size.height
                                )
                            }
                        }
                )
                if (audioRecordingData.isNotEmpty()) {
                    val elapsedTime = when (val lastData = audioRecordingData.last()) {
                        AudioRecordingData.NotStarted -> 0
                        is AudioRecordingData.Paused -> lastData.elapsedTime
                        is AudioRecordingData.Recording -> lastData.elapsedTime
                    }
                    val minutes = elapsedTime / 1000 / 60
                    val seconds = elapsedTime / 1000 % 60
                    Text(
                        text = "${minutes.toString().padStart(2, '0')}:${
                            seconds.toString().padStart(2, '0')
                        }",
                        style = timeLabelStyle
                    )
                } else {
                    Text(
                        text = "00:00",
                        style = timeLabelStyle
                    )
                }
            }
        }
    }
}