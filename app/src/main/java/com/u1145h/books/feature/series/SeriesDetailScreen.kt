package com.u1145h.books.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.u1145h.books.domain.model.Chapter
import com.u1145h.books.domain.model.SeriesDetail
import com.u1145h.books.domain.model.Volume
import com.u1145h.books.feature.audio.AudioMiniPlayer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SeriesDetailScreen(
    seriesId: Int,
    onChapterClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.series?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            AudioMiniPlayer(playerManager = viewModel.audioPlayerManager)
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.series != null -> SeriesContent(
                series = state.series!!,
                coverUrl = viewModel.coverUrlBuilder.series(state.series!!.id),
                downloadedChapterIds = state.downloadedChapterIds,
                downloadingChapterIds = state.downloadingChapterIds,
                hasCompanionAudiobook = state.companionAudiobookId != null,
                onChapterClick = onChapterClick,
                onDownloadChapter = { viewModel.downloadChapter(it) },
                onRemoveDownload = { viewModel.removeChapterDownload(it) },
                onContinueClick = {
                    viewModel.continueChapter()?.let { onChapterClick(it.id) }
                },
                onPlayAudiobook = {
                    viewModel.playCompanionAudiobook()
                },
                modifier = Modifier.padding(padding),
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Something went wrong")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeriesContent(
    series: SeriesDetail,
    coverUrl: String,
    downloadedChapterIds: Set<Int>,
    downloadingChapterIds: Set<Int>,
    hasCompanionAudiobook: Boolean,
    onChapterClick: (Int) -> Unit,
    onDownloadChapter: (Chapter) -> Unit,
    onRemoveDownload: (Int) -> Unit,
    onContinueClick: () -> Unit,
    onPlayAudiobook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var summaryExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize()) {

        // Hero cover
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Scrim overlay bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                            )
                        )
                )
            }
        }

        // Series info & Dual Read/Listen actions
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))

                // Format Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (hasCompanionAudiobook) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Audiobook Available",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                    }

                    val formatName = when (series.format) {
                        1 -> "EPUB"
                        2 -> "PDF"
                        else -> "Image / Manga"
                    }
                    val meta = listOfNotNull(
                        formatName,
                        "${series.pages} pages".takeIf { series.pages > 0 },
                    ).joinToString(" • ")

                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Progress bar
                if (series.pages > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { series.progressPercent },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(series.progressPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Action Buttons: Read eBook & Listen Audiobook
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onContinueClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (series.pagesRead == 0) "Read eBook" else "Continue Reading")
                    }

                    if (hasCompanionAudiobook) {
                        FilledTonalButton(
                            onClick = onPlayAudiobook,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Listen Audio")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Summary
                if (!series.summary.isNullOrBlank()) {
                    Text(
                        text = series.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (summaryExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { summaryExpanded = !summaryExpanded },
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Genres
                if (series.genres.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        series.genres.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp),
                            ) {
                                Text(
                                    genre,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    "Volumes & Chapters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Volumes / Chapters
        items(series.volumes) { volume ->
            VolumeRow(
                volume = volume,
                downloadedChapterIds = downloadedChapterIds,
                downloadingChapterIds = downloadingChapterIds,
                onChapterClick = onChapterClick,
                onDownloadChapter = onDownloadChapter,
                onRemoveDownload = onRemoveDownload,
            )
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun VolumeRow(
    volume: Volume,
    downloadedChapterIds: Set<Int>,
    downloadingChapterIds: Set<Int>,
    onChapterClick: (Int) -> Unit,
    onDownloadChapter: (Chapter) -> Unit,
    onRemoveDownload: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (volume.number > 0) {
            Text(
                text = "Volume ${volume.number}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        volume.chapters.forEach { chapter ->
            ChapterRow(
                chapter = chapter,
                isDownloaded = downloadedChapterIds.contains(chapter.id),
                isDownloading = downloadingChapterIds.contains(chapter.id),
                onDownloadClick = { onDownloadChapter(chapter) },
                onRemoveDownloadClick = { onRemoveDownload(chapter.id) },
                onClick = { onChapterClick(chapter.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            val displayTitle = when {
                !chapter.title.isNullOrBlank() -> chapter.title
                chapter.isSpecial -> "Special: ${chapter.range}"
                else -> "Chapter ${chapter.number}"
            }
            Text(displayTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "${chapter.pages} pages • ${chapter.pagesRead} read",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (chapter.pages > 0) {
            LinearProgressIndicator(
                progress = { chapter.pagesRead.toFloat() / chapter.pages },
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
        }

        when {
            isDownloading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
            isDownloaded -> {
                IconButton(onClick = onRemoveDownloadClick) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded offline. Tap to remove.",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            else -> {
                IconButton(onClick = onDownloadClick) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download for offline reading",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
