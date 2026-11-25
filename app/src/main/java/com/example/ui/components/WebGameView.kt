package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class WebGameController {
    internal var webView: WebView? = null

    fun reload(fallbackUrl: String) {
        val view = webView ?: return
        if (view.url.isNullOrBlank()) {
            view.loadUrl(fallbackUrl)
        } else {
            view.reload()
        }
    }

    fun loadHome(homeUrl: String) {
        webView?.loadUrl(homeUrl)
    }

    fun goBack() {
        webView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun goForward() {
        webView?.let { if (it.canGoForward()) it.goForward() }
    }
}

private val IN_APP_ALLOWED_HOST_SUFFIXES = listOf(
    "ai.studio",
    "google.com",
    "googleusercontent.com",
    "gstatic.com",
    "googleapis.com",
    "firebaseapp.com",
    "web.app",
)

private fun isInAppHost(host: String?): Boolean {
    if (host.isNullOrBlank()) return false
    val lower = host.lowercase()
    return IN_APP_ALLOWED_HOST_SUFFIXES.any { suffix ->
        lower == suffix || lower.endsWith(".$suffix")
    }
}

private fun openExternally(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ignore if no handler exists on the device
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebGameView(
    initialUrl: String,
    controller: WebGameController,
    onPageStarted: (String?) -> Unit,
    onPageFinished: (url: String?, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTitleReceived: (String?) -> Unit,
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onMainFrameError: (code: Int, description: String, failingUrl: String) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setBackgroundColor(AndroidColor.parseColor("#070709"))
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            val currentWebView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(currentWebView, true)
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                setSupportMultipleWindows(false)
                allowFileAccess = false
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                javaScriptCanOpenWindowsAutomatically = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val uri = request?.url ?: return false
                    val scheme = uri.scheme?.lowercase()
                    if (scheme != "https" && scheme != "http") {
                        openExternally(context, uri)
                        return true
                    }
                    if (isInAppHost(uri.host)) {
                        return false
                    }
                    openExternally(context, uri)
                    return true
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onPageStarted(url)
                    onNavigationStateChanged(
                        view?.canGoBack() == true,
                        view?.canGoForward() == true,
                    )
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onPageFinished(
                        url,
                        view?.canGoBack() == true,
                        view?.canGoForward() == true,
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        val code = error?.errorCode ?: -1
                        val desc = error?.description?.toString().orEmpty()
                        val failingUrl = request.url?.toString().orEmpty()
                        onMainFrameError(code, desc, failingUrl)
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?,
                ): Boolean {
                    view?.loadUrl(initialUrl)
                    return true
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onProgressChanged(newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    onTitleReceived(title)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view != null && callback != null) {
                        onShowCustomView(view, callback)
                    } else {
                        super.onShowCustomView(view, callback)
                    }
                }

                override fun onHideCustomView() {
                    onHideCustomView()
                    super.onHideCustomView()
                }
            }

            controller.webView = this
            loadUrl(initialUrl)
        }
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.webView = null
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .testTag("web_game_container"),
    )
}
