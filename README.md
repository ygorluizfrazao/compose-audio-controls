# compose-audio-controls

<div id="header" align="center">
  <a href="https://jitpack.io/#ygorluizfrazao/compose-audio-controls"><img src="https://jitpack.io/v/ygorluizfrazao/compose-audio-controls.svg" alt="Version Name"/></a>
  <img src="https://komarev.com/ghpvc/?username=ygorluizfrazao&style=flat-square&color=blue" alt=""/>
</div>
<div id="badges" align="center">
  <a href="https://www.linkedin.com/in/ygorluizfrazao/">
    <img src="https://img.shields.io/badge/LinkedIn-blue?style=flat&logo=linkedin&logoColor=white" alt="LinkedIn Badge"/>
  </a>
  <a href="https://ko-fi.com/ygorfrazao">
    <img src="https://img.shields.io/badge/Kofi-blue?style=flat&logo=kofi&logoColor=white" alt="Youtube Badge"/>
  </a>
</div>

## How can i use it?

Just add this to your *settings.gradle*:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Then, in your *build.gradle*:

```groovy
	dependencies {
	        implementation 'com.github.ygorluizfrazao.compose-audio-controls:audio-services:v1.0.0-alpha03'
          	implementation 'com.github.ygorluizfrazao.compose-audio-controls:ui:v1.0.0-alpha03'
	}
```

## What is it?
A library that provides an Android [Jetpack Compose](https://developer.android.com/jetpack/compose) audio recorder and an audio player, with its UI and services. Also, provides a visualizer abstraction which you can extend yourself and implement.

## Why it exists?
I've searched online for audio composable libs to use with [materialv3]([https://m3.material.io]), but, had no success, then, developed one.

## When should i use it?
Whenever you want:

- An audio recorder component.
- An audio player component.
- A service to record audio.
- A service to play audio.

## How to use it?

The controls are:

### Record:
<img src="https://user-images.githubusercontent.com/17025709/228987300-7c569fc8-0d21-4e47-aa0c-fd7daae33407.gif" alt="Record Component" style="width:20%; height:20%">

When you want to record, use the following function:

```kotlin
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
)
```

As you can see, you can provide your own icons and modifier object.

As important params we have:

*`recordingWaveVisualizer: RecordingVisualizer`: The strategy to draw the audio visualizer waves, currently, there is one implemented, being `MirrorWaveRecordingVisualizer`. You can implement your own vizualizer by extending `RecordingVisualizer`.

```kotlin
interface RecordingVisualizer {

    fun drawGraphics(canvas: Canvas,
                     width: Float = canvas.width.toFloat(),
                     height: Float = canvas.height.toFloat(),
                     offsetX: Float = 0f,
                     offsetY: Float = 0f,
                     amplitudes: List<Float>)
}
```

*`audioRecordingData: List<AudioRecordingData>`: The data list of the running audio record service, its used by the visualizer and the time count.
```kotlin
sealed class AudioRecordingData {
    object NotStarted: AudioRecordingData()
    data class Recording(val elapsedTime: Long, val maxAmplitudeInCycle: Int): AudioRecordingData()
    data class Paused(val elapsedTime: Long, val maxAmplitudeInCycle: Int): AudioRecordingData()
}
```

To acquire the `AudioRecordingData` flow, you will need to make use of `AndroidAudioRecorder` class, by calling its `override fun startRecording(outputFile: File): Flow<AudioRecordingData>` which will return a `Flow<AudioRecordingData>`that you can observe in your viewmodel. for example:

```kotlin
class AudioRecordViewModel: ViewModel() {

    ...
    
    enum class AudioNoteStatus {
        HAVE_TO_RECORD, CAN_PLAY
    }
    
    ...
    
    //initialize states and variables
    private var _audioRecordFlow = MutableStateFlow<List<AudioRecordingData>>(emptyList())
    val audioRecordFlow = _audioRecordFlow.asStateFlow()
    private var currentAudioFile: File? = null
    private var _audioNoteStatus = MutableStateFlow(AudioNoteStatus.HAVE_TO_RECORD)
    val audioStatus = _audioNoteStatus.asStateFlow()
    
    ...
    
    //After you got the permission and the user clicks the record button, onRecordRequested is called...
    fun startRecordingAudioNote(audioDirectory: File) {
        viewModelScope.launch {
            _audioRecordFlow.value = emptyList()
            currentAudioFile?.delete()
            currentAudioFile = File(audioDirectory, UUID.randomUUID().toString())
            currentAudioFile?.let { fileOutput ->
                val flow =
                    audioRecorder.startRecording(fileOutput)
                flow.catch {
                    audioRecorder.stopRecording()
                    fileOutput.delete()
                    currentAudioFile = null
                    //Do something with the error
                }
                    .collectLatest {
                        if (_audioRecordFlow.value.size >= 1000)
                            _audioRecordFlow.value =
                                _audioRecordFlow.value - _audioRecordFlow.value.first()
                        _audioRecordFlow.value = _audioRecordFlow.value + it
                    }
            }
        }
    }
    
    ...
    
    fun stopRecordingAudio() {
        audioRecorder.stopRecording()
        currentAudioFile?.let {
            _audioNoteStatus.value = AudioNoteStatus.CAN_PLAY
        }
    }
    
    ...
}
```

If you need a way to get the needed permission, consider using [composed-permissions]([https://github.com/ygorluizfrazao/composed-permissions])

for futher clarification, the ui for this example looks like:

```kotlin
if (audioNoteStatus == viewmodel.AudioNoteStatus.HAVE_TO_RECORD) {
    AudioRecorder(
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
                    recordIcon = {
                        //Compose your icon
                    },
                    stopIcon = {
                        //Compose your icon
                    },
                    onRecordRequested = onAudioRecordStartRequested, // that will call the viewmodel `startRecordingAudioNote` method.
                    onStopRequested = onAudioRecordStopRequested, // that will call the viewmodel `stopRecordingAudio` method.
                    audioRecordingData = audioRecordingData,
                    recordingWaveVisualizer = MirrorWaveRecordingVisualizer(
                        wavePaint = Paint().apply {
                            color = LocalContentColor.current.toArgb()
                            strokeWidth = 2f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            flags = Paint.ANTI_ALIAS_FLAG
                            strokeJoin = Paint.Join.BEVEL
                        },
                        middleLinePaint = Paint().apply {
                            color =
                                LocalTextSelectionColors.current.handleColor.toArgb()
                            style = Paint.Style.FILL_AND_STROKE
                            strokeWidth = 2f
                            pathEffect =
                                DashPathEffect(arrayOf(4f, 4f).toFloatArray(), 0f)
                        }
                    )
                ) 
}
```

### Playing:
<img src="https://user-images.githubusercontent.com/17025709/229643756-b2619a08-3b9e-44dd-b1aa-9107543e7c5d.gif" alt="PLaying Component" style="width:20%; height:20%">

When you want to play an audio file, use:

```kotlin
@Composable
@Composable
fun AudioPlayer(
    modifier: Modifier = Modifier,
    audioPlayingData: AudioPlayingData,
    audioPlayerParams: AudioPlayerParams = buildAudioPlayerParams(),
    audioPlayerCallbacks: AudioPlayerCallbacks
)
```

*`audioPlayingData: AudioPlayingData`: An object with useful information about the audio being played.
```kotlin
data class AudioPlayingData(val status: AudioPlayerStatus, val duration: Long, val elapsed: Long)

enum class AudioPlayerStatus{
    NOT_INITIALIZED, PLAYING, PAUSED
}
```
*`audioPlayerParams` -> Configures the player.

```kotlin
data class AudioPlayerParams(
    val timeLabelStyle: TextStyle,
    val timeContainerModifier: Modifier,
    val timeLabelContent: (@Composable (elapsedTime: Long, totalDuration: Long) -> Unit),
    val playIcon: @Composable () -> Unit,
    val pauseIcon: @Composable () -> Unit,
    val endIcon: (@Composable () -> Unit)?,
)
```
Has a convenience function with default values where you can fine tune what you need:

```kotlin
@Composable
fun buildAudioPlayerParams
```

*`audioPlayerCallbacks` -> Callback for user inputs.

```kotlin
data class AudioPlayerCallbacks(
    val onPlay: () -> Unit,
    val onPause: () -> Unit,
    val onSeekPosition: (Float) -> Unit,
    val onEndIconClicked: (() -> Unit)? = null
)
```

To acquire the `AudioPlayingData` object, you will need to make use of `AndroidAudioPlayer` class, by calling its `override fun start(file: File): Flow<AudioPlayingData>` which will return a `Flow<AudioPlayingData>` that you can observe in your viewmodel. for example:


```kotlin

    enum class AudioNoteStatus {
        HAVE_TO_RECORD, CAN_PLAY
    }

    //Initialize your variables
    private var _audioNotePlayingData =
        MutableStateFlow(AudioPlayingData(AudioPlayerStatus.NOT_INITIALIZED, 0, 0))
    val audioNotePlayingData = _audioNotePlayingData.asStateFlow()
    private var _audioNoteStatus = MutableStateFlow(AudioNoteStatus.HAVE_TO_RECORD)
    val audioStatus = _audioNoteStatus.asStateFlow()
    
    
    fun playAudioNote() {
      if(_audioNotePlayingData.value.status == AudioPlayerStatus.NOT_INITIALIZED) {
        currentAudioFile?.let { file ->
            viewModelScope.launch {
                val flow = audioPlayer.start(file)
                flow.catch {
                    _uiState.value = UIState.Error(it)
                    mediator.broadcast(
                        uiParticipantRepresentative,
                        UIEvent.Error(
                            TextResource.RuntimeString(
                                it.localizedMessage ?: it.message ?: "An error has occurred."
                            )
                        )
                    )
                    audioPlayer.stop()
                }.collectLatest {
                    _audioNotePlayingData.value = it
                }
            }
        }
    }else{
        resumeAudioNote()
    }
    
    fun pauseAudioNote() {
        audioPlayer.pause()
    }

    private fun resumeAudioNote(){
        audioPlayer.resume()
    }

    fun deleteAudioNote(){
        audioPlayer.stop()
        currentAudioFile?.delete()
        _audioNoteStatus.value = AudioNoteStatus.HAVE_TO_RECORD
    }
}
```

If you need a way to get the needed permission, consider using [composed-permissions]([https://github.com/ygorluizfrazao/composed-permissions])

for futher clarification, the ui for this example looks like:

```kotlin
AudioPlayer(
    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
    audioPlayingData = audioPlayingData,
    audioPlayerParams = buildAudioPlayerParams(
	playIcon = {
	    IconResource.fromImageVector(Icons.Default.PlayArrow)
		.ComposeIcon()
	},
	pauseIcon = {
	    IconResource.fromImageVector(Icons.Default.Pause)
		.ComposeIcon()
	},
	endIcon =
	if (onAudioNoteDeleteRequest != null) {
	    {
		IconResource.fromImageVector(Icons.Default.Delete)
		    .ComposeIcon()
	    }
	} else null,
    ),
    audioPlayerCallbacks = AudioPlayerCallbacks(
	onPlay = onAudioNotePlayRequest,
	onPause = onAudioNotePauseRequest,
	onEndIconClicked = onAudioNoteDeleteRequest,
	onSeekPosition = onAudionNoteSeekPosition
    )
)
```

Hope it helps you.
