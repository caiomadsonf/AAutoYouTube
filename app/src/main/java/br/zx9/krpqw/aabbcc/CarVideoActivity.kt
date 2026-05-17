package br.zx9.krpqw.aabbcc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class CarVideoActivity : Activity() {

    private lateinit var webView: WebView
    private var mediaCommandReceiver: BroadcastReceiver? = null

    inner class AndroidVideoBridge {

        @JavascriptInterface
        fun reportState(state: String) {
            val intent = Intent("br.zx9.krpqw.aabbcc.PLAYER_STATE").apply {
                setPackage(packageName)
                putExtra("state", state)
            }
            sendBroadcast(intent)
        }

        @JavascriptInterface
        fun reportProgress(position: Long, duration: Long, title: String) {
            val safeTitle = title
                .replace(" - YouTube", "")
                .replace("YouTube", "")
                .trim()
                .ifEmpty { "AAuto YouTube" }

            val intent = Intent("br.zx9.krpqw.aabbcc.PLAYER_PROGRESS").apply {
                setPackage(packageName)
                putExtra("position", position)
                putExtra("duration", duration)
                putExtra("title", safeTitle)
            }
            sendBroadcast(intent)
        }

        @JavascriptInterface
        fun reportEnded(videoId: String) {
            val intent = Intent("br.zx9.krpqw.aabbcc.VIDEO_ENDED").apply {
                setPackage(packageName)
                putExtra("videoId", videoId)
            }
            sendBroadcast(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        webView = WebView(this)
        setContentView(webView)

        configureWebView()
        setupMediaCommandReceiver()

        val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
        val url =
            intent.getStringExtra("url")
                ?: prefs.getString("selected_url", null)
                ?: prefs.getString("shared_video_url", null)
                ?: "https://www.youtube.com/tv"

        webView.loadUrl(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(AndroidVideoBridge(), "AndroidVideoBridge")
        webView.webChromeClient = FullscreenWebChromeClient(this, webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString =
                "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 " +
                    "(KHTML, like Gecko) Version/5.0 TV Safari/538.1"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return !(url.startsWith("file://") ||
                    url.contains("youtube.com") ||
                    url.contains("youtu.be") ||
                    url.contains("ytimg.com") ||
                    url.contains("google.com") ||
                    url.contains("googlevideo.com") ||
                    url.contains("gstatic.com"))
            }

            override fun onPageFinished(view: WebView, url: String) {
                injectFullscreenAndSyncBridge()
                autoStartPlayback()
            }
        }
    }

    private fun setupMediaCommandReceiver() {
        mediaCommandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getStringExtra("command")) {
                    "play" -> runVideoCommand("play")
                    "pause" -> runVideoCommand("pause")
                    "stop" -> runVideoCommand("pause")
                    "next" -> runVideoCommand("next")
                    "previous" -> runVideoCommand("previous")
                    "seekTo" -> runVideoCommand("seekTo", intent.getLongExtra("position", 0L))
                    "loadVideo" -> {
                        val videoId = intent.getStringExtra("videoId")
                        val playlistId = intent.getStringExtra("playlistId")
                        when {
                            videoId != null -> webView.loadUrl("https://www.youtube.com/watch?v=$videoId")
                            playlistId != null -> webView.loadUrl("https://www.youtube.com/playlist?list=$playlistId")
                            else -> webView.loadUrl("https://www.youtube.com/tv")
                        }
                    }
                }
            }
        }

        registerReceiver(
            mediaCommandReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.MEDIA_COMMAND"),
            RECEIVER_NOT_EXPORTED
        )
    }

    private fun runVideoCommand(command: String, value: Long = 0L) {
        val js = when (command) {
            "play" -> """
                (function() {
                    var v = document.querySelector('video');
                    if (v) v.play().catch(function(){});
                })();
            """.trimIndent()
            "pause" -> """
                (function() {
                    var v = document.querySelector('video');
                    if (v) v.pause();
                })();
            """.trimIndent()
            "seekTo" -> """
                (function() {
                    var v = document.querySelector('video');
                    if (v) v.currentTime = ${value / 1000.0};
                })();
            """.trimIndent()
            "next" -> """
                (function() {
                    var selectors = ['.ytp-next-button', 'button[aria-label*=Next]', 'button[aria-label*=Próximo]'];
                    for (var i = 0; i < selectors.length; i++) {
                        var btn = document.querySelector(selectors[i]);
                        if (btn) { btn.click(); return; }
                    }
                })();
            """.trimIndent()
            "previous" -> """
                (function() { history.back(); })();
            """.trimIndent()
            else -> ""
        }

        if (js.isNotEmpty()) {
            webView.post { webView.evaluateJavascript(js, null) }
        }
    }

    private fun autoStartPlayback() {
        val js = """
            (function() {
                function tryPlay() {
                    var v = document.querySelector('video');
                    if (v) v.play().catch(function(){});
                }
                setTimeout(tryPlay, 500);
                setTimeout(tryPlay, 1500);
                setTimeout(tryPlay, 3000);
                setTimeout(tryPlay, 5000);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectFullscreenAndSyncBridge() {
        val js = """
            (function() {
                if (window.__AAUTO_SYNC_INSTALLED__) return;
                window.__AAUTO_SYNC_INSTALLED__ = true;

                document.documentElement.style.background = 'black';
                document.body.style.background = 'black';
                document.body.style.margin = '0';
                document.body.style.overflow = 'hidden';

                var style = document.createElement('style');
                style.innerHTML = `
                    html, body { background: #000 !important; margin: 0 !important; padding: 0 !important; overflow: hidden !important; }
                    video { width: 100vw !important; height: 100vh !important; object-fit: contain !important; background: #000 !important; }
                    .ytp-chrome-top, .ytp-chrome-bottom, .ytp-gradient-top, .ytp-gradient-bottom { opacity: 0.15 !important; }
                `;
                document.head.appendChild(style);

                function getTitle() {
                    var title = document.title || 'AAuto YouTube';
                    var selectors = ['h1.title', 'h1', '.title', '.ytp-title-link', '[class*=title]'];
                    for (var i = 0; i < selectors.length; i++) {
                        var el = document.querySelector(selectors[i]);
                        if (el && el.textContent && el.textContent.trim().length > 0) {
                            title = el.textContent.trim();
                            break;
                        }
                    }
                    return title || 'AAuto YouTube';
                }

                function getVideoId() {
                    try {
                        var url = new URL(window.location.href);
                        return url.searchParams.get('v') || '';
                    } catch(e) { return ''; }
                }

                function bindVideo(video) {
                    if (!video || video.__AAUTO_BOUND__) return;
                    video.__AAUTO_BOUND__ = true;

                    video.addEventListener('play', function() { AndroidVideoBridge.reportState('playing'); });
                    video.addEventListener('playing', function() { AndroidVideoBridge.reportState('playing'); });
                    video.addEventListener('pause', function() { AndroidVideoBridge.reportState('paused'); });
                    video.addEventListener('waiting', function() { AndroidVideoBridge.reportState('buffering'); });
                    video.addEventListener('ended', function() {
                        AndroidVideoBridge.reportState('ended');
                        AndroidVideoBridge.reportEnded(getVideoId());
                    });

                    setInterval(function() {
                        try {
                            var position = isFinite(video.currentTime) ? Math.floor(video.currentTime * 1000) : 0;
                            var duration = isFinite(video.duration) ? Math.floor(video.duration * 1000) : 0;
                            AndroidVideoBridge.reportProgress(position, duration, getTitle());
                            if (!video.paused && !video.ended) AndroidVideoBridge.reportState('playing');
                        } catch(e) {}
                    }, 1000);
                }

                function findAndBindVideo() {
                    var video = document.querySelector('video');
                    if (video) bindVideo(video);
                }

                findAndBindVideo();
                var observer = new MutationObserver(function() { findAndBindVideo(); });
                observer.observe(document.documentElement, { childList: true, subtree: true });
                setInterval(findAndBindVideo, 1000);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        mediaCommandReceiver?.let { runCatching { unregisterReceiver(it) } }
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
