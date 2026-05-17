package br.zx9.krpqw.aabbcc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject

class WebViewActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var mediaCommandReceiver: BroadcastReceiver

    // MediaPlayer silencioso — keepalive para o Android não matar o processo.
    // Mesma técnica do Cartube (silent.mp3 em loop).
    private var silentPlayer: MediaPlayer? = null

    // ── Interface JavaScript → Android ───────────────────────────────────────
    // O player HTML chama AndroidInterface.onPlayerEvent(json) para reportar
    // o estado real do YouTube — sem polling, sem clicar em botões frágeis.
    inner class AndroidInterface {

        @JavascriptInterface
        fun onPlayerEvent(json: String) {
            try {
                val obj = JSONObject(json)
                val event = obj.getString("event")
                val data = obj.getJSONObject("data")

                when (event) {
                    "ready" -> {
                        // Player pronto: carrega URL pendente se houver
                        val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
                        val url = prefs.getString("selected_url", null)
                        val videoId = url?.let { extractVideoId(it) }
                        if (videoId != null) {
                            sendPlayerCommand("loadVideo", mapOf("videoId" to videoId))
                        }
                    }
                    "stateChange" -> {
                        // Repassa estado real para o MusicService atualizar MediaSession
                        val intent = Intent("br.zx9.krpqw.aabbcc.PLAYER_STATE").apply {
                            setPackage(packageName)
                            putExtra("state", data.getString("state"))
                        }
                        sendBroadcast(intent)
                    }
                    "progress" -> {
                        // Atualiza barra de progresso no Android Auto
                        val intent = Intent("br.zx9.krpqw.aabbcc.PLAYER_PROGRESS").apply {
                            setPackage(packageName)
                            putExtra("position", data.getLong("position"))
                            putExtra("duration", data.getLong("duration"))
                            putExtra("title", data.optString("title", ""))
                        }
                        sendBroadcast(intent)
                    }
                    "videoEnded" -> {
                        // Vídeo terminou: MusicService decide o próximo
                        val intent = Intent("br.zx9.krpqw.aabbcc.VIDEO_ENDED").apply {
                            setPackage(packageName)
                            putExtra("videoId", data.optString("videoId", ""))
                        }
                        sendBroadcast(intent)
                    }
                }
            } catch (e: Exception) {
                // JSON malformado não deve travar a UI
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
        if (!prefs.getBoolean("logged_in", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        webView = WebView(this)
        setContentView(webView)

        configureWebView()
        setupMediaCommandReceiver()
        startSilentPlayer()

        // Carrega o player HTML local de res/raw/youtube_player.html
        val prefs2 = getSharedPreferences("aauto", MODE_PRIVATE)
        val startUrl = prefs2.getString("selected_url", "https://www.youtube.com/tv") ?: "https://www.youtube.com/tv"
        webView.loadUrl(startUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        // Interface registrada ANTES de loadUrl — garante que o JS já encontra o objeto
        webView.addJavascriptInterface(AndroidInterface(), "AndroidInterface")

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            // User-agent de Smart TV — mesmo que o Cartube usa para youtube.com/tv
            // Faz o YouTube servir a interface de TV em vez de mobile/desktop
            userAgentString = "Mozilla/5.0 (SMART-TV; Linux; Tizen 5.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/5.0 TV Safari/538.1"
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                injectCSS()
                skipWelcomeScreen()
                checkSharedVideo()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                // Permite apenas o player local e domínios do YouTube/Google
                return !(url.startsWith("file://") ||
                        url.contains("youtube.com") ||
                        url.contains("youtu.be") ||
                        url.contains("ytimg.com") ||
                        url.contains("google.com"))
            }
        }
    }

    // ── Controle via broadcast (enviado pelo MusicService) ───────────────────

    private fun setupMediaCommandReceiver() {
        mediaCommandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.getStringExtra("command")) {
                    "play"     -> sendPlayerCommand("play")
                    "pause"    -> sendPlayerCommand("pause")
                    "stop"     -> sendPlayerCommand("stop")
                    "next"     -> sendPlayerCommand("next")
                    "previous" -> sendPlayerCommand("previous")
                    "loadVideo" -> {
                        val videoId = intent.getStringExtra("videoId") ?: return
                        val playlistId = intent.getStringExtra("playlistId")
                        val params = buildMap<String, Any> {
                            put("videoId", videoId)
                            if (playlistId != null) put("playlistId", playlistId)
                        }
                        sendPlayerCommand("loadVideo", params)
                    }
                }
            }
        }
        val filter = IntentFilter("br.zx9.krpqw.aabbcc.MEDIA_COMMAND")
        registerReceiver(mediaCommandReceiver, filter, RECEIVER_NOT_EXPORTED)
    }

    // ── Envia comando para o player HTML ─────────────────────────────────────
    // handleCommand() está definido no youtube_player.html e usa a IFrame API —
    // player.playVideo(), pauseVideo(), nextVideo() etc. — sem toggle.

    private fun sendPlayerCommand(action: String, params: Map<String, Any> = emptyMap()) {
        val json = JSONObject().apply {
            put("action", action)
            params.forEach { (k, v) -> put(k, v) }
        }.toString()
        webView.post {
            webView.evaluateJavascript("handleCommand('${json.replace("'", "\\'")}');", null)
        }
    }

    // ── Vídeo compartilhado via Share do YouTube ─────────────────────────────

    private fun checkSharedVideo() {
        val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
        val sharedUrl = prefs.getString("shared_video_url", "") ?: ""
        if (sharedUrl.isNotEmpty()) {
            extractVideoId(sharedUrl)?.let { id ->
                sendPlayerCommand("loadVideo", mapOf("videoId" to id))
            }
            prefs.edit().remove("shared_video_url").apply()
        }
    }

    // ── Extrai videoId de qualquer formato de URL do YouTube ─────────────────

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("[?&]v=([a-zA-Z0-9_-]{11})"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("shorts/([a-zA-Z0-9_-]{11})")
        )
        for (pattern in patterns) {
            pattern.find(url)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    // ── CSS de ocultação (hide_ui.css) ───────────────────────────────────────

    // ── Pula tela de boas-vindas do YouTube TV ───────────────────────────────
    // Injeta JS que clica em 'Get started' e depois vai direto para os vídeos.
    // Roda com delay para dar tempo ao React do YouTube TV renderizar.
    private fun skipWelcomeScreen() {
        val js = """
            (function() {
                function trySkip() {
                    // Tenta clicar em 'Get started' ou 'Use without account'
                    var btns = document.querySelectorAll('button, [role=button], a');
                    for (var i = 0; i < btns.length; i++) {
                        var txt = (btns[i].textContent || '').trim().toLowerCase();
                        if (txt === 'get started' || txt === 'use without an account' ||
                            txt === 'use youtube without an account' || txt === 'skip') {
                            btns[i].click();
                            return true;
                        }
                    }
                    return false;
                }
                // Tenta imediatamente e com delays progressivos
                setTimeout(function() { trySkip(); }, 500);
                setTimeout(function() { trySkip(); }, 1500);
                setTimeout(function() { trySkip(); }, 3000);
            })();
        """.trimIndent()
        webView.post { webView.evaluateJavascript(js, null) }
    }

    private fun injectCSS() {
        val css = try {
            resources.openRawResource(R.raw.hide_ui).bufferedReader().use { it.readText() }
        } catch (e: Exception) { return }

        val js = """
            (function() {
                var style = document.createElement('style');
                style.innerHTML = ${JSONObject.quote(css)};
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    // ── silent.mp3 keepalive ──────────────────────────────────────────────────
    // Toca áudio silencioso em loop — o Android trata o processo como player
    // ativo e não o mata em background. Mesmo mecanismo do Cartube.

    @Suppress("DEPRECATION")
    private fun startSilentPlayer() {
        try {
            silentPlayer = MediaPlayer().apply {
                val afd = resources.openRawResourceFd(R.raw.silent)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Se o silent.mp3 não existir, o app continua funcionando
        }
    }

    private fun stopSilentPlayer() {
        silentPlayer?.apply { if (isPlaying) stop(); release() }
        silentPlayer = null
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onDestroy() {
        unregisterReceiver(mediaCommandReceiver)
        stopSilentPlayer()
        webView.destroy()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}