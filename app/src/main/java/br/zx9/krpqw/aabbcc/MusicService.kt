package br.zx9.krpqw.aabbcc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat

class MusicService : MediaBrowserServiceCompat() {

    private lateinit var mediaSession: MediaSessionCompat
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val CHANNEL_ID = "aauto_media_channel"
        private const val NOTIFICATION_ID = 1
        private const val ROOT_ID = "root"
        private const val MEDIA_ID_YOUTUBE = "youtube_player"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        mediaSession = MediaSessionCompat(this, "AAutoYouTube")

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                launchWebView()
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }
            override fun onPause() {
                sendCommandToWebView("pause")
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
            override fun onSkipToNext() {
                sendCommandToWebView("next")
            }
            override fun onSkipToPrevious() {
                sendCommandToWebView("previous")
            }
            override fun onStop() {
                sendCommandToWebView("pause")
                setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
            }
            override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                if (mediaId == MEDIA_ID_YOUTUBE) {
                    launchWebView()
                    setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
                }
            }
        })

        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        setPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        mediaSession.isActive = true
        sessionToken = mediaSession.sessionToken

        mediaPlayer = MediaPlayer.create(this, R.raw.silent)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    private fun launchWebView() {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AAuto YouTube",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AAuto YouTube")
            .setContentText("Pronto para reproduzir")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setSilent(true)
            .build()
    }

    private fun setPlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
            )
            .setState(state, 0, 1f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun sendCommandToWebView(command: String) {
        val intent = Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND")
        intent.putExtra("command", command)
        sendBroadcast(intent)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId != ROOT_ID) {
            result.sendResult(emptyList())
            return
        }

        val description = MediaDescriptionCompat.Builder()
            .setMediaId(MEDIA_ID_YOUTUBE)
            .setTitle("AAuto YouTube")
            .setSubtitle("Reproduzir YouTube")
            .build()

        val item = MediaBrowserCompat.MediaItem(
            description,
            MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        )

        result.sendResult(listOf(item))
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaSession.release()
        super.onDestroy()
    }
}