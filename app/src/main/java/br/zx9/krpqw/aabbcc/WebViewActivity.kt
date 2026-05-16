package br.zx9.krpqw.aabbcc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class WebViewActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("logged_in", false)

        if (!loggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        webView = WebView(this)
        setContentView(webView)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/3.1 TV Safari/538.1"
            cacheMode = WebSettings.LOAD_DEFAULT
            allowContentAccess = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                injectCSS()
                injectAllJS()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                return true
            }
        }

        webView.loadUrl("https://m.youtube.com")
    }

    private fun injectCSS() {
        val css = readRawFile(R.raw.hide_ui)
        val js = """
            (function() {
                var style = document.createElement('style');
                style.innerHTML = `$css`;
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectAllJS() {
        listOf(
            R.raw.remove_ads,
            R.raw.touchless_click,
            R.raw.intercept_clicks,
            R.raw.autoplay,
            R.raw.fullscreen
        ).forEach { resId ->
            webView.evaluateJavascript(readRawFile(resId), null)
        }
    }

    private fun readRawFile(resId: Int): String {
        return resources.openRawResource(resId)
            .bufferedReader()
            .use { it.readText() }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}