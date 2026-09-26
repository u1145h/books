package com.u1145h.kavitaandroid.feature.home

import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * The embedded Kavita web interface, configured to never look like a browser:
 * JS/DOM storage enabled, zoom/overscroll/scrollbars disabled, single window.
 */
@Composable
fun KavitaWebView(
    url: String,
    bridge: KavitaBridge,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
    onOfflineChanged: (Boolean) -> Unit = {},
    onShowFileChooser: (WebChromeClient.FileChooserParams, ValueCallback<Array<Uri>>) -> Boolean =
        { _, _ -> false },
) {
    val webViewRef = remember { arrayOfNulls<WebView>(1) }

    val client = remember {
        object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onOfflineChanged(false)
                onLoadingChanged(true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onLoadingChanged(false)
                view?.evaluateJavascript(KavitaJs.SESSION_SYNC, null)
                view?.evaluateJavascript(KavitaJs.BODY_COLOR, null)
                view?.evaluateJavascript(KavitaJs.OFFLINE_INJECT, null)
                view?.evaluateJavascript(KavitaJs.SIDEBAR_INJECT, null)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val reqUrl = request?.url
                if (reqUrl != null) {
                    val offlineResp = bridge.getOfflineResource(reqUrl)
                    if (offlineResp != null) return offlineResp
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && error != null) {
                    val code = error.errorCode
                    val unreachable = code == WebViewClient.ERROR_CONNECT ||
                        code == WebViewClient.ERROR_HOST_LOOKUP ||
                        code == WebViewClient.ERROR_TIMEOUT ||
                        code == WebViewClient.ERROR_IO
                    if (unreachable) onOfflineChanged(true)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true && errorResponse?.statusCode != null) {
                    onOfflineChanged(true)
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.setSupportMultipleWindows(false)
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                webViewClient = client
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.grant(request.resources)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?,
                    ): Boolean {
                        if (filePathCallback != null && fileChooserParams != null) {
                            return onShowFileChooser(fileChooserParams, filePathCallback)
                        }
                        return false
                    }
                }
                addJavascriptInterface(bridge, KavitaJs.OBJECT_NAME)
                bridge.attachWebView(this)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewRef[0] = this
                onWebViewReady(this)
                loadUrl(url)
            }
        },
        update = { view ->
            webViewRef[0] = view
            onWebViewReady(view)
        },
        modifier = modifier,
    )

    LaunchedEffect(url) {
        if (url.isNotEmpty()) webViewRef[0]?.loadUrl(url)
    }
}
