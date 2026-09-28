package com.u1145h.books.feature.reader

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.u1145h.books.domain.model.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: Int,
    seriesId: Int,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val context = LocalContext.current

    val insetsController = remember(view, context) {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    LaunchedEffect(state.showControls, insetsController) {
        insetsController?.let { controller ->
            if (state.showControls) {
                controller.show(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(insetsController) {
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Immediately flush reading progress when leaving the reader
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProgressImmediately()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = Color.White,
            )
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = Color.White)
            }
            state.format == ReaderFormat.EPUB -> EpubPageView(
                htmlContent = state.currentPageHtml ?: "",
                chapterId = state.chapterId,
                currentPage = state.currentPage,
                initialScrollY = state.initialScrollY,
                serverUrl = viewModel.serverUrl,
                token = viewModel.sessionManager.token,
                apiKey = viewModel.sessionManager.apiKey,
                okHttpClient = viewModel.okHttpClient,
                isLoading = state.isPageLoading,
                onScrollChanged = { viewModel.onScrollPositionChanged(it) },
                onCenterTap = { viewModel.toggleControls() },
                onNextPage = { viewModel.nextPage() },
                onPrevPage = { viewModel.prevPage() },
            )
            state.isWebtoon -> WebtoonReader(
                state = state,
                viewModel = viewModel,
                onCenterTap = { viewModel.toggleControls() },
                onNextChapter = { viewModel.goToNextChapter() },
            )
            else -> PagerReader(
                state = state,
                viewModel = viewModel,
                onCenterTap = { viewModel.toggleControls() },
            )
        }

        // ── Controls overlay ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = state.showControls && !state.isLoading && state.error == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ReaderControls(
                state = state,
                onBack = onBack,
                onPageChange = { viewModel.goToPage(it) },
                onPrevChapter = { viewModel.goToPrevChapter() },
                onNextChapter = { viewModel.goToNextChapter() },
                onToggleChapterList = { viewModel.toggleChapterList() },
                onToggleRtl = { viewModel.toggleRtl() },
                onToggleWebtoon = { viewModel.toggleWebtoon() },
            )
        }

        // ── Chapter List Bottom Sheet ─────────────────────────────────────────
        if (state.showChapterList) {
            ChapterListBottomSheet(
                state = state,
                onDismiss = { viewModel.dismissChapterList() },
                onSelectChapter = { viewModel.selectChapter(it) },
            )
        }

        // ── Chapter Completion Confirmation Dialog ────────────────────────────
        if (state.showChapterCompletionDialog) {
            ChapterCompletionDialog(
                state = state,
                onDismiss = { viewModel.dismissCompletionDialog() },
                onConfirmCompleteAndNext = { viewModel.confirmCompleteAndNext() },
                onBackToSeries = onBack,
            )
        }
    }
}

@Composable
private fun PagerReader(
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    onCenterTap: () -> Unit,
) {
    val totalPages = state.totalPages.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = state.currentPage.coerceIn(0, totalPages - 1),
        pageCount = { totalPages },
    )

    // Sync ViewModel page -> pager
    LaunchedEffect(state.currentPage) {
        if (state.currentPage in 0 until totalPages && pagerState.currentPage != state.currentPage) {
            pagerState.scrollToPage(state.currentPage)
        }
    }

    // Sync pager swipe -> ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page in 0 until totalPages && page != state.currentPage) {
                viewModel.goToPage(page)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        reverseLayout = state.isRtl,
        modifier = Modifier.fillMaxSize(),
        key = { it },
    ) { page ->
        ZoomablePage(
            imageUrl = viewModel.pageImageUrl(page),
            onTap = { offset, width ->
                when {
                    offset.x < width * 0.25f -> if (state.isRtl) viewModel.nextPage() else viewModel.prevPage()
                    offset.x > width * 0.75f -> if (state.isRtl) viewModel.prevPage() else viewModel.nextPage()
                    else -> onCenterTap()
                }
            },
        )
    }
}

@Composable
private fun WebtoonReader(
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    onCenterTap: () -> Unit,
    onNextChapter: () -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.currentPage.coerceIn(0, (state.totalPages - 1).coerceAtLeast(0)),
    )

    // Sync user scroll -> ViewModel
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index in 0 until state.totalPages && index != state.currentPage) {
                    viewModel.updatePageSilently(index)
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { onCenterTap() }
            },
    ) {
        items(state.totalPages) { page ->
            AsyncImage(
                model = viewModel.pageImageUrl(page),
                contentDescription = "Page $page",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.nextChapter != null) {
                    Button(
                        onClick = onNextChapter,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Next Chapter", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                } else {
                    Text(
                        text = "End of Series",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterCompletionDialog(
    state: ReaderUiState,
    onDismiss: () -> Unit,
    onConfirmCompleteAndNext: () -> Unit,
    onBackToSeries: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chapter Finished", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                if (state.nextChapter != null) {
                    val nextLabel = state.nextChapter.title?.ifBlank { null }
                        ?: "Chapter ${state.nextChapter.number}"
                    Text(
                        text = "Up next: $nextLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "You've caught up with the latest chapter!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (state.nextChapter != null) {
                Button(onClick = onConfirmCompleteAndNext) {
                    Text("Next Chapter")
                }
            } else {
                Button(onClick = {
                    onConfirmCompleteAndNext()
                    onBackToSeries()
                }) {
                    Text("Finish & Exit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Reading")
            }
        },
    )
}

@Composable
private fun ZoomablePage(
    imageUrl: String,
    onTap: (offset: Offset, width: Float) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }
    var layoutWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(transformState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset -> onTap(tapOffset, layoutWidth) },
                    onDoubleTap = {
                        scale = if (scale > 1.5f) 1f else 2.5f
                        offset = Offset.Zero
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}

@Composable
private fun ReaderControls(
    state: ReaderUiState,
    onBack: () -> Unit,
    onPageChange: (Int) -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onToggleChapterList: () -> Unit,
    onToggleRtl: () -> Unit,
    onToggleWebtoon: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Text(
                    text = state.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleChapterList) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "Chapter list",
                        tint = Color.White,
                    )
                }
                if (state.format != ReaderFormat.EPUB) {
                    IconButton(onClick = onToggleRtl) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Toggle RTL",
                            tint = if (state.isRtl) MaterialTheme.colorScheme.primary else Color.White,
                        )
                    }
                    IconButton(onClick = onToggleWebtoon) {
                        Icon(
                            Icons.Default.ViewDay,
                            contentDescription = "Webtoon mode",
                            tint = if (state.isWebtoon) MaterialTheme.colorScheme.primary else Color.White,
                        )
                    }
                }
            }
        }

        // ── Bottom slider + Chapter controls ──────────────────────────────────
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = state.prevChapter != null,
                        onClick = onPrevChapter,
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous Chapter",
                            tint = if (state.prevChapter != null) Color.White else Color.DarkGray,
                        )
                    }

                    Text(
                        text = if (state.totalPages > 0) "${state.currentPage + 1} / ${state.totalPages}" else "",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )

                    IconButton(
                        enabled = state.nextChapter != null,
                        onClick = onNextChapter,
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next Chapter",
                            tint = if (state.nextChapter != null) Color.White else Color.DarkGray,
                        )
                    }
                }

                if (state.totalPages > 1) {
                    Slider(
                        value = state.currentPage.toFloat(),
                        onValueChange = { onPageChange(it.toInt()) },
                        valueRange = 0f..(state.totalPages - 1).toFloat(),
                        steps = (state.totalPages - 2).coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterListBottomSheet(
    state: ReaderUiState,
    onDismiss: () -> Unit,
    onSelectChapter: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Chapters (${state.chapters.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider()

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(state.chapters, key = { it.id }) { chapter ->
                    val isCurrent = chapter.id == state.chapterId
                    ChapterListItem(
                        chapter = chapter,
                        isCurrent = isCurrent,
                        onClick = { onSelectChapter(chapter.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterListItem(
    chapter: Chapter,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val title = chapter.title?.ifBlank { null } ?: "Chapter ${chapter.number}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (chapter.pages > 0) {
                val progressText = if (chapter.pagesRead >= chapter.pages) "Completed"
                else "${chapter.pagesRead}/${chapter.pages} pages"
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (isCurrent) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Current chapter",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
