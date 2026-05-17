package br.zx9.krpqw.aabbcc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

class MusicService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private var playerStateReceiver: BroadcastReceiver? = null
    private var playerProgressReceiver: BroadcastReceiver? = null
    private var videoEndedReceiver: BroadcastReceiver? = null

    // Estado atual rastreado — evita notificações desnecessárias
    private var currentState = PlaybackStateCompat.STATE_PAUSED
    private var currentPosition = 0L
    private var currentDuration = 0L
    private var currentTitle = "AAuto YouTube"

    companion object {
        private const val ROOT_ID = "root"
        private const val MEDIA_ID_YOUTUBE = "youtube_player"
        private const val CHANNEL_ID = "aauto_youtube_media"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        setupMediaSession()
        setupStateReceivers()

        setPlaybackState(PlaybackStateCompat.STATE_PAUSED, 0L)
        setSessionToken(mediaSession.sessionToken)
        startForegroundWithNotification()
    }

    // ── MediaSession ──────────────────────────────────────────────────────────

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "AAutoYouTube").apply {
            setCallback(object : MediaSessionCompat.Callback() {

                override fun onPlay() {
                    sendCommandToWebView("play")
                    // Estado será atualizado via PLAYER_STATE broadcast — não aqui.
                    // Isso garante que a MediaSession reflita o estado REAL do player.
                }

                override fun onPause() {
                    sendCommandToWebView("pause")
                }

                override fun onSkipToNext() {
                    sendCommandToWebView("next")
                }

                override fun onSkipToPrevious() {
                    sendCommandToWebView("previous")
                }

                override fun onStop() {
                    sendCommandToWebView("stop")
                    setPlaybackState(PlaybackStateCompat.STATE_STOPPED, 0L)
                    stopForeground(true)
                    stopSelf()
                }

                override fun onSeekTo(pos: Long) {
                    val intent = Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND").apply {
                        setPackage(packageName)
                        putExtra("command", "seekTo")
                        putExtra("position", pos)
                    }
                    sendBroadcast(intent)
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    if (mediaId == MEDIA_ID_YOUTUBE) {
                        launchWebView()
                    }
                }
            })

            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            isActive = true
        }
    }

    // ── Receivers — estado real vindo do WebViewActivity ─────────────────────
    // Esta é a grande diferença: em vez de o serviço adivinhar o estado,
    // ele escuta o que o player HTML reportou via AndroidInterface.

    private fun setupStateReceivers() {
        // Estado de reprodução: playing, paused, ended, buffering...
        playerStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getStringExtra("state") ?: return
                val playbackState = when (state) {
                    "playing"   -> PlaybackStateCompat.STATE_PLAYING
                    "paused"    -> PlaybackStateCompat.STATE_PAUSED
                    "buffering" -> PlaybackStateCompat.STATE_BUFFERING
                    "ended"     -> PlaybackStateCompat.STATE_PAUSED
                    "stopped"   -> PlaybackStateCompat.STATE_STOPPED
                    else        -> return
                }
                currentState = playbackState
                setPlaybackState(playbackState, currentPosition)

                if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                } else {
                    stopForeground(false)
                    updateNotification()
                }
            }
        }
        registerReceiver(
            playerStateReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.PLAYER_STATE"),
            RECEIVER_NOT_EXPORTED
        )

        // Progresso: posição + duração em ms + título do vídeo
        playerProgressReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                currentPosition = intent.getLongExtra("position", 0L)
                currentDuration = intent.getLongExtra("duration", 0L)
                val title = intent.getStringExtra("title") ?: ""

                // Atualiza metadados se o título mudou (novo vídeo)
                if (title.isNotEmpty() && title != currentTitle) {
                    currentTitle = title
                    updateMetadata(title)
                }

                // Atualiza posição na MediaSession (barra de progresso no Android Auto)
                setPlaybackState(currentState, currentPosition)
            }
        }
        registerReceiver(
            playerProgressReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.PLAYER_PROGRESS"),
            RECEIVER_NOT_EXPORTED
        )

        // Vídeo terminou — poderia iniciar autoplay aqui no futuro
        videoEndedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                currentPosition = 0L
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED, 0L)
            }
        }
        registerReceiver(
            videoEndedReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.VIDEO_ENDED"),
            RECEIVER_NOT_EXPORTED
        )
    }

    // ── MediaBrowser ──────────────────────────────────────────────────────────

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot = BrowserRoot(ROOT_ID, null)

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId != ROOT_ID) { result.sendResult(emptyList()); return }

        val description = MediaDescriptionCompat.Builder()
            .setMediaId(MEDIA_ID_YOUTUBE)
            .setTitle("AAuto YouTube")
            .setSubtitle("Reproduzir YouTube")
            .build()

        result.sendResult(listOf(
            MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
        ))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun setPlaybackState(state: Int, position: Long) {
        val pb = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
            )
            .setState(state, position, if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f)
            .build()
        mediaSession.setPlaybackState(pb)
    }

    private fun updateMetadata(title: String) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "YouTube")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration)
            .build()
        mediaSession.setMetadata(metadata)
        updateNotification()
    }

    private fun sendCommandToWebView(command: String) {
        val intent = Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND").apply {
            setPackage(packageName)
            putExtra("command", command)
        }
        sendBroadcast(intent)
    }

    private fun launchWebView() {
        startActivity(Intent(this, WebViewActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ── Notificação ───────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "AAuto YouTube — mídia",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Controles de reprodução no Android Auto" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, WebViewActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val isPlaying = currentState == PlaybackStateCompat.STATE_PLAYING

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(if (isPlaying) "Reproduzindo" else "Pausado")
            .setContentIntent(contentIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_previous, "Anterior",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausar" else "Reproduzir",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY)
            )
            .addAction(android.R.drawable.ic_media_next, "Próximo",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
            .build()
    }

    private fun startForegroundWithNotification() =
        startForeground(NOTIFICATION_ID, buildNotification())

    private fun updateNotification() =
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onDestroy() {
        playerStateReceiver?.let { unregisterReceiver(it) }
        playerProgressReceiver?.let { unregisterReceiver(it) }
        videoEndedReceiver?.let { unregisterReceiver(it) }
        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }
        super.onDestroy()
    }
}