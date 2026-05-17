package br.zx9.krpqw.aabbcc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout

class LoginActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Container principal
        val container = FrameLayout(this)
        setContentView(container)

        // WebView de login
        webView = WebView(this)
        container.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // ── Botão de bypass (só em debug) ────────────────────────────────────
        // Em produção (release), BuildConfig.DEBUG é false e o botão não aparece.
        if (BuildConfig.DEBUG) {
            val skipButton = Button(this).apply {
                text = "Pular login (debug)"
                setBackgroundColor(0xFF333333.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                alpha = 0.85f
                setOnClickListener { completeLogin() }
            }
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 48
            }
            container.addView(skipButton, params)
        }

        // ── WebView de login Google ───────────────────────────────────────────
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Linux; Android 15; Moto G05) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                // Detecta quando o login foi concluído e o YouTube foi aberto
                if (url.contains("youtube.com") && !url.contains("accounts.google.com")) {
                    cookieManager.flush()
                    completeLogin()
                    return true
                }
                return false
            }
        }

        webView.loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https://m.youtube.com/signin?action_handle_signin=true")
    }

    private fun completeLogin() {
        getSharedPreferences("aauto", MODE_PRIVATE)
            .edit()
            .putBoolean("logged_in", true)
            .apply()
        startActivity(Intent(this, WebViewActivity::class.java))
        finish()
    }
}