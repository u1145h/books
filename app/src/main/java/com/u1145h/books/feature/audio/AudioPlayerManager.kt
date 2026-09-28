package com.u1145h.books.feature.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.u1145h.books.data.repository.AudiobookshelfRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AudioPlayerState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val itemId: String = "",
    val title: String = "",
    val author: String = "",
    val coverUrl: String = "",
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val companionSeriesId: Int? = null,
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val absRepository: AudiobookshelfRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressTickerJob: Job? = null

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.update { it.copy(isPlaying = isPlaying) }
                    if (isPlaying) startProgressTicker() else stopProgressTicker()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _state.update { it.copy(durationMs = duration.coerceAtLeast(0L)) }
                    }
                }
            })
        }
    }

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    fun playAudiobook(
        itemId: String,
        title: String,
        author: String,
        coverUrl: String,
        companionSeriesId: Int? = null,
    ) {
        scope.launch {
            val sessionResult = absRepository.startPlaySession(itemId)
            val session = sessionResult.getOrNull()
            val streamUrl = session?.audioTracks?.firstOrNull()?.let { track ->
                absRepository.getStreamUrl(itemId, track.contentUrl.substringAfterLast('/'))
            } ?: absRepository.getStreamUrl(itemId, "")

            _state.update {
                it.copy(
                    isActive = true,
                    itemId = itemId,
                    title = title,
                    author = author,
                    coverUrl = coverUrl,
                    companionSeriesId = companionSeriesId,
                )
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(author)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(streamUrl))
                .setMediaMetadata(metadata)
                .build()

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L)))
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    fun skipForward(seconds: Int = 30) {
        val target = exoPlayer.currentPosition + (seconds * 1000L)
        seekTo(target)
    }

    fun skipBackward(seconds: Int = 15) {
        val target = exoPlayer.currentPosition - (seconds * 1000L)
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _state.update { it.copy(playbackSpeed = speed) }
    }

    fun closePlayer() {
        exoPlayer.stop()
        stopProgressTicker()
        _state.update { AudioPlayerState() }
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = scope.launch {
            while (isActive) {
                val current = exoPlayer.currentPosition
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                _state.update { it.copy(currentPositionMs = current, durationMs = dur) }
                delay(1000)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }
}
