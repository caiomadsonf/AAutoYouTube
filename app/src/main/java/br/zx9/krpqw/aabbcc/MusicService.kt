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

    private var currentState = PlaybackStateCompat.STATE_PAUSED
    private var currentPosition = 0L
    private var currentDuration = 0L
    private var currentTitle = "AAuto YouTube"
    private var currentArtist = "YouTube"

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

        setInitialMetadata()
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED, 0L)
        setSessionToken(mediaSession.sessionToken)
        startForegroundWithNotification()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "AAutoYouTube").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        launchWebViewIfNeeded()
                        sendCommandToWebView("play")
                        currentState = PlaybackStateCompat.STATE_PLAYING
                        setPlaybackState(PlaybackStateCompat.STATE_PLAYING, currentPosition)
                        updateNotification()
                    }

                    override fun onPause() {
                        sendCommandToWebView("pause")
                        currentState = PlaybackStateCompat.STATE_PAUSED
                        setPlaybackState(PlaybackStateCompat.STATE_PAUSED, currentPosition)
                        updateNotification()
                    }

                    override fun onSkipToNext() {
                        launchWebViewIfNeeded()
                        sendCommandToWebView("next")
                    }

                    override fun onSkipToPrevious() {
                        launchWebViewIfNeeded()
                        sendCommandToWebView("previous")
                    }

                    override fun onStop() {
                        sendCommandToWebView("stop")
                        currentState = PlaybackStateCompat.STATE_STOPPED
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
                        currentPosition = pos
                        setPlaybackState(currentState, currentPosition)
                    }

                    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                        if (mediaId == MEDIA_ID_YOUTUBE || mediaId == null) {
                            launchWebViewIfNeeded()
                            sendCommandToWebView("play")
                            currentState = PlaybackStateCompat.STATE_PLAYING
                            setPlaybackState(PlaybackStateCompat.STATE_PLAYING, currentPosition)
                            updateNotification()
                        }
                    }

                    override fun onPrepare() {
                        setInitialMetadata()
                        setPlaybackState(PlaybackStateCompat.STATE_PAUSED, currentPosition)
                    }

                    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
                        setInitialMetadata()
                        setPlaybackState(PlaybackStateCompat.STATE_PAUSED, currentPosition)
                    }
                }
            )

            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            isActive = true
        }
    }

    private fun setupStateReceivers() {
        playerStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getStringExtra("state") ?: return
                val playbackState = when (state) {
                    "playing" -> PlaybackStateCompat.STATE_PLAYING
                    "paused" -> PlaybackStateCompat.STATE_PAUSED
                    "buffering" -> PlaybackStateCompat.STATE_BUFFERING
                    "ended" -> PlaybackStateCompat.STATE_PAUSED
                    "stopped" -> PlaybackStateCompat.STATE_STOPPED
                    else -> return
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

        playerProgressReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                currentPosition = intent.getLongExtra("position", 0L)
                currentDuration = intent.getLongExtra("duration", 0L)

                val title = intent.getStringExtra("title") ?: ""
                if (title.isNotEmpty() && title != currentTitle) {
                    currentTitle = title
                    currentArtist = "YouTube"
                }

                updateMetadata(currentTitle, currentArtist, currentDuration)
                setPlaybackState(currentState, currentPosition)
            }
        }

        registerReceiver(
            playerProgressReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.PLAYER_PROGRESS"),
            RECEIVER_NOT_EXPORTED
        )

        videoEndedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                currentPosition = 0L
                currentState = PlaybackStateCompat.STATE_PAUSED
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED, 0L)
                updateNotification()
            }
        }

        registerReceiver(
            videoEndedReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.VIDEO_ENDED"),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        if (parentId != ROOT_ID) {
            result.sendResult(emptyList())
            return
        }

        val description = MediaDescriptionCompat.Builder()
            .setMediaId(MEDIA_ID_YOUTUBE)
            .setTitle("AAuto YouTube")
            .setSubtitle("Reproduzir YouTube")
            .setDescription("Abrir player de video")
            .build()

        result.sendResult(
            listOf(MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        )
    }

    private fun setInitialMetadata() {
        currentTitle = "AAuto YouTube"
        currentArtist = "YouTube"
        currentDuration = 0L
        updateMetadata(currentTitle, currentArtist, currentDuration)
    }

    private fun updateMetadata(title: String, artist: String, duration: Long) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, MEDIA_ID_YOUTUBE)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "AAuto YouTube")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()

        mediaSession.setMetadata(metadata)
        updateNotification()
    }

    private fun setPlaybackState(state: Int, position: Long) {
        val actions =
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE or
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setActiveQueueItemId(0L)
            .setState(state, position, if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f)
            .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun sendCommandToWebView(command: String) {
        val intent = Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND").apply {
            setPackage(packageName)
            putExtra("command", command)
        }
        sendBroadcast(intent)
    }

    private fun launchWebViewIfNeeded() {
        startActivity(Intent(this, CarVideoActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AAuto YouTube - midia",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Controles de reproducao no Android Auto" }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, CarVideoActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isPlaying = currentState == PlaybackStateCompat.STATE_PLAYING

        val previousIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        val playPauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this,
            if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        )
        val nextIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(if (isPlaying) "Reproduzindo" else "Pausado")
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Anterior", previousIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausar" else "Reproduzir",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Proximo", nextIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun startForegroundWithNotification() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        playerStateReceiver?.let { runCatching { unregisterReceiver(it) } }
        playerProgressReceiver?.let { runCatching { unregisterReceiver(it) } }
        videoEndedReceiver?.let { runCatching { unregisterReceiver(it) } }

        if (::mediaSession.isInitialized) {
            mediaSession.isActive = false
            mediaSession.release()
        }

        super.onDestroy()
    }
}
