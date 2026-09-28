package com.u1145h.books.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.u1145h.books.domain.model.ServerSource
import com.u1145h.books.domain.model.UnifiedLibrary
import com.u1145h.books.domain.model.UnifiedMediaItem
import com.u1145h.books.domain.model.UnifiedMediaType
import com.u1145h.books.feature.audio.AudioMiniPlayer
import com.u1145h.books.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeriesClick: (Int) -> Unit,
    onLibraryClick: (Int, String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Books",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val connectedServers = buildList {
                            if (state.isKavitaConnected) add("Kavita")
                            if (state.isAbsConnected) add("Audiobookshelf")
                        }.joinToString(" • ")

                        if (connectedServers.isNotBlank()) {
                            Text(
                                connectedServers,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            AudioMiniPlayer(
                playerManager = viewModel.playerManager,
                onClick = {},
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val isEmpty = state.libraries.isEmpty() &&
            state.onDeck.isEmpty() &&
            state.inProgress.isEmpty() &&
            state.recentlyAdded.isEmpty()

        if (isEmpty) {
            EmptyState(
                icon = Icons.Default.GridView,
                title = "No media content found",
                subtitle = "Connected to server, but no books or audiobooks were found.",
                modifier = Modifier.padding(padding),
                action = {
                    Button(onClick = { viewModel.load() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Refresh")
                    }
                },
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // — Libraries (Kavita & Audiobookshelf) —
            if (state.libraries.isNotEmpty()) {
                item {
                    SectionHeader("Libraries")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.libraries, key = { it.id }) { lib ->
                            UnifiedLibraryCard(
                                library = lib,
                                onClick = {
                                    if (lib.source == ServerSource.Kavita) {
                                        val seriesIdInt = lib.id.substringAfter("kavita:").toIntOrNull() ?: 0
                                        onLibraryClick(seriesIdInt, lib.name)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // — Continue Reading & Listening (On Deck) —
            if (state.onDeck.isNotEmpty()) {
                item {
                    SectionHeader("Continue Reading & Listening")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.onDeck, key = { it.id }) { item ->
                            UnifiedMediaCard(
                                item = item,
                                onClick = {
                                    if (item.kavitaSeriesId != null) {
                                        onSeriesClick(item.kavitaSeriesId)
                                    } else if (item.absItemId != null) {
                                        viewModel.playAudiobook(item)
                                    }
                                },
                                showProgress = true,
                            )
                        }
                    }
                }
            }

            // — Recently Added —
            if (state.recentlyAdded.isNotEmpty()) {
                item {
                    SectionHeader("Recently Added")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.recentlyAdded, key = { it.id }) { item ->
                            UnifiedMediaCard(
                                item = item,
                                onClick = {
                                    if (item.kavitaSeriesId != null) {
                                        onSeriesClick(item.kavitaSeriesId)
                                    } else if (item.absItemId != null) {
                                        viewModel.playAudiobook(item)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // — In Progress Manga & Comics —
            if (state.inProgress.isNotEmpty()) {
                item {
                    SectionHeader("In Progress")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.inProgress, key = { it.id }) { item ->
                            UnifiedMediaCard(
                                item = item,
                                onClick = {
                                    if (item.kavitaSeriesId != null) {
                                        onSeriesClick(item.kavitaSeriesId)
                                    }
                                },
                                showProgress = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp),
    )
}

@Composable
fun UnifiedLibraryCard(
    library: UnifiedLibrary,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val icon = when (library.mediaType) {
                UnifiedMediaType.Book -> Icons.AutoMirrored.Filled.MenuBook
                UnifiedMediaType.Comic -> Icons.Default.AutoStories
                UnifiedMediaType.Manga -> Icons.Default.CollectionsBookmark
                UnifiedMediaType.Audiobook -> Icons.Default.Headphones
                UnifiedMediaType.Podcast -> Icons.Default.Headphones
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (library.source == ServerSource.Audiobookshelf) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )

            if (!library.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(library.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = library.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = library.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Surface(
            color = if (library.source == ServerSource.Audiobookshelf) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = if (library.source == ServerSource.Audiobookshelf) "Audiobook" else "Kavita",
                style = MaterialTheme.typography.labelSmall,
                color = if (library.source == ServerSource.Audiobookshelf) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
fun UnifiedMediaCard(
    item: UnifiedMediaItem,
    onClick: () -> Unit,
    showProgress: Boolean = false,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (item.hasAudio && !item.hasText) Icons.Default.Headphones else Icons.Default.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp),
            )

            if (item.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Format Badge top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Surface(
                    color = when {
                        item.isDualFormat -> MaterialTheme.colorScheme.secondary
                        item.hasAudio -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    shape = RoundedCornerShape(6.dp),
                    tonalElevation = 4.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        if (item.isDualFormat) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Text & Audio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
                        } else if (item.hasAudio) {
                            Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Audio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiary)
                        } else {
                            Text("eBook", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
        }

        if (showProgress && (item.readProgressPercent != null && item.readProgressPercent > 0f)) {
            LinearProgressIndicator(
                progress = { item.readProgressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )

        if (!item.author.isNullOrBlank()) {
            Text(
                text = item.author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SeriesCard(
    series: com.u1145h.books.domain.model.Series,
    coverUrl: String,
    onClick: () -> Unit,
    showProgress: Boolean = false,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.67f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp),
            )

            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = series.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = series.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}
