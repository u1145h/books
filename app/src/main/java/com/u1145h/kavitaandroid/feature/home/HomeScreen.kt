package com.u1145h.kavitaandroid.feature.home

import android.app.Activity
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.u1145h.kavitaandroid.ui.components.OfflineScreen
import com.u1145h.kavitaandroid.ui.components.ServerSetupScreen

/**
 * The embedded Kavita web interface. Never looks like a browser — no URL bar,
 * no scrollbars, zoom disabled. The system back button walks the web view
 * history and only exits when there is no history. On first run (no saved
 * server address) shows [ServerSetupScreen] instead.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val serverUrlState by viewModel.serverUrlState.collectAsStateWithLifecycle()
    val bodyColor by viewModel.bridge.bodyColor.collectAsStateWithLifecycle()
    val setupState by viewModel.setupState.collectAsStateWithLifecycle()
    val downloadedBooks by viewModel.downloadedBooks.collectAsStateWithLifecycle(initialValue = emptyList())

    val isManualOffline by viewModel.bridge.isManualOffline.collectAsStateWithLifecycle()
    val showAppSettings by viewModel.bridge.showAppSettings.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity

    var isLoading by rememberSaveable { mutableStateOf(true) }
    var isOffline by rememberSaveable { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        pendingFileCallback = null
    }

    val onShowFileChooser: (WebChromeClient.FileChooserParams, ValueCallback<Array<Uri>>) -> Boolean =
        remember {
            { params, callback ->
                pendingFileCallback = callback
                fileLauncher.launch(params.acceptTypes.ifEmpty { arrayOf("*/*") })
                true
            }
        }

    val currentUrl = (serverUrlState as? ServerUrlState.Configured)?.url ?: ""

    BackHandler {
        if (showAppSettings) {
            viewModel.bridge.setShowAppSettings(false)
            return@BackHandler
        }
        if (isManualOffline) {
            viewModel.bridge.setManualOffline(false)
            return@BackHandler
        }
        if (currentUrl.isBlank()) {
            activity?.finish()
            return@BackHandler
        }
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            activity?.finish()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(bodyColor))) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black)
                .statusBarsPadding(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black)
                .navigationBarsPadding(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            when (val state = serverUrlState) {
                is ServerUrlState.Loading -> {
                    // Waiting for DataStore; render neutral background without flashing setup screen
                }
                is ServerUrlState.Unconfigured -> {
                    ServerSetupScreen(
                        state = setupState,
                        onConnect = viewModel::submitServerUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is ServerUrlState.Configured -> {
                    KavitaWebView(
                        url = state.url,
                        bridge = viewModel.bridge,
                        modifier = Modifier.fillMaxSize(),
                        onWebViewReady = { webView = it },
                        onLoadingChanged = { isLoading = it },
                        onOfflineChanged = { isOffline = it },
                        onShowFileChooser = onShowFileChooser,
                    )

                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (isOffline || isManualOffline) {
                        OfflineScreen(
                            url = state.url,
                            downloadedBooks = downloadedBooks,
                            onRetry = {
                                isOffline = false
                                viewModel.bridge.setManualOffline(false)
                                webView?.reload()
                            },
                            onOpenBook = { book ->
                                val chapterId = book.chapterId
                                if (chapterId != null && chapterId > 0) {
                                    isOffline = false
                                    viewModel.bridge.setManualOffline(false)
                                    webView?.loadUrl("${state.url.trimEnd('/')}/reader/chapter/$chapterId")
                                }
                            },
                        )
                    }

                    if (showAppSettings) {
                        com.u1145h.kavitaandroid.ui.components.AppSettingsScreen(
                            currentServerUrl = state.url,
                            username = viewModel.bridge.getUsername(),
                            setupState = setupState,
                            downloadedBooks = downloadedBooks,
                            onSaveServerUrl = viewModel::submitServerUrl,
                            onDeleteBook = viewModel::deleteBook,
                            onClearAllDownloads = viewModel::clearAllDownloads,
                            onClose = { viewModel.bridge.setShowAppSettings(false) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
