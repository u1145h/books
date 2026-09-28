package com.u1145h.books.feature.reader

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import okhttp3.OkHttpClient
import okhttp3.Request

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubPageView(
    htmlContent: String,
    chapterId: Int,
    currentPage: Int,
    initialScrollY: Int,
    serverUrl: String,
    token: String?,
    apiKey: String?,
    okHttpClient: OkHttpClient,
    isLoading: Boolean,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onScrollChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onNextPage: () -> Unit,
    onPrevPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && htmlContent.isBlank()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = if (isDarkTheme) Color.White else Color.Black,
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = false
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = false
                        settings.defaultFontSize = 18

                        setBackgroundColor(if (isDarkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt())

                        // Monitor vertical scroll position
                        setOnScrollChangeListener { _, _, _, _, _ ->
                            onScrollChanged(scrollY)
                        }

                        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                val width = width.toFloat()
                                val x = e.x
                                when {
                                    x < width * 0.25f -> onPrevPage()
                                    x > width * 0.75f -> onNextPage()
                                    else -> onCenterTap()
                                }
                                return true
                            }

                            override fun onFling(
                                e1: MotionEvent?,
                                e2: MotionEvent,
                                velocityX: Float,
                                velocityY: Float,
                            ): Boolean {
                                if (e1 == null) return false
                                val diffX = e2.x - e1.x
                                val diffY = e2.y - e1.y
                                // Only trigger page flip if horizontal swipe is clearly dominant over vertical reading scroll
                                if (Math.abs(diffX) > Math.abs(diffY) * 1.5 && Math.abs(diffX) > 150 && Math.abs(velocityX) > 300) {
                                    if (diffX < 0) {
                                        onNextPage()
                                    } else {
                                        onPrevPage()
                                    }
                                    return true
                                }
                                return false
                            }
                        })

                        setOnTouchListener { _, event ->
                            gestureDetector.onTouchEvent(event)
                            false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (initialScrollY > 0) {
                                    view?.postDelayed({
                                        view.scrollTo(0, initialScrollY)
                                    }, 120)
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): WebResourceResponse? {
                                val url = request?.url?.toString() ?: return null
                                val baseTrimmed = serverUrl.trimEnd('/')
                                if (url.startsWith(baseTrimmed)) {
                                    return runCatching {
                                        val reqBuilder = Request.Builder().url(url)
                                        if (!token.isNullOrBlank()) reqBuilder.header("Authorization", "Bearer $token")
                                        if (!apiKey.isNullOrBlank()) reqBuilder.header("x-api-key", apiKey)
                                        val resp = okHttpClient.newCall(reqBuilder.build()).execute()
                                        val contentType = resp.header("Content-Type", "image/jpeg") ?: "image/jpeg"
                                        val mime = contentType.substringBefore(";")
                                        val encoding = resp.header("Content-Encoding", "UTF-8")
                                        WebResourceResponse(mime, encoding, resp.body?.byteStream())
                                    }.getOrNull()
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                    }
                },
                update = { webView ->
                    val contentKey = "$chapterId-$currentPage-$isDarkTheme"
                    if (webView.tag != contentKey && htmlContent.isNotBlank()) {
                        webView.tag = contentKey
                        webView.setBackgroundColor(if (isDarkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt())
                        val styledHtml = buildStyledHtml(htmlContent, isDarkTheme)
                        val baseUrl = "${serverUrl.trimEnd('/')}/api/Book/$chapterId/"
                        webView.loadDataWithBaseURL(baseUrl, styledHtml, "text/html", "UTF-8", null)
                    }
                },
            )
        }
    }
}

private fun buildStyledHtml(rawHtml: String, isDark: Boolean): String {
    val bgColor = if (isDark) "#121212" else "#FFFFFF"
    val textColor = if (isDark) "#E0E0E0" else "#202124"
    val headingColor = if (isDark) "#FFFFFF" else "#000000"
    val linkColor = if (isDark) "#8AB4F8" else "#1A73E8"
    val codeBg = if (isDark) "#242424" else "#F1F3F4"

    val customStyle = """
        <style>
            html, body {
                background-color: $bgColor !important;
                color: $textColor !important;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif !important;
                font-size: 18px !important;
                line-height: 1.75 !important;
                padding: 16px 20px 96px 20px !important;
                margin: 0 !important;
                word-wrap: break-word !important;
                overflow-wrap: break-word !important;
                box-sizing: border-box !important;
            }
            * {
                box-sizing: border-box !important;
            }
            a {
                color: $linkColor !important;
                text-decoration: underline !important;
            }
            img {
                max-width: 100% !important;
                height: auto !important;
                display: block !important;
                margin: 20px auto !important;
                border-radius: 8px !important;
            }
            p {
                margin-top: 0 !important;
                margin-bottom: 1.15em !important;
                text-align: justify !important;
            }
            h1, h2, h3, h4, h5, h6 {
                color: $headingColor !important;
                font-weight: 700 !important;
                line-height: 1.3 !important;
                margin-top: 1.5em !important;
                margin-bottom: 0.6em !important;
            }
            blockquote {
                border-left: 3px solid $linkColor !important;
                margin: 16px 0 !important;
                padding-left: 16px !important;
                color: $textColor !important;
                opacity: 0.85 !important;
            }
            pre, code {
                background-color: $codeBg !important;
                color: $textColor !important;
                font-family: monospace !important;
                padding: 2px 6px !important;
                border-radius: 4px !important;
                font-size: 0.9em !important;
            }
            pre {
                padding: 12px !important;
                overflow-x: auto !important;
            }
        </style>
    """.trimIndent()

    return if (rawHtml.contains("<head>", ignoreCase = true)) {
        rawHtml.replaceFirst("(?i)<head>".toRegex(), "<head>$customStyle")
    } else if (rawHtml.contains("<html>", ignoreCase = true)) {
        rawHtml.replaceFirst("(?i)<html>".toRegex(), "<html><head>$customStyle</head>")
    } else {
        "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">$customStyle</head><body>$rawHtml</body></html>"
    }
}
