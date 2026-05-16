package br.zx9.krpqw.aabbcc

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.media.CarAudioRecord
import androidx.car.app.model.MessageTemplate

class NowPlayingScreen(carContext: CarContext) : Screen(carContext) {

    private var isPlaying = true
    private var currentTitle = "YouTube"

    override fun onGetTemplate(): Template {

        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "⏸ Pausar" else "▶ Play")
            .setOnClickListener {
                isPlaying = !isPlaying
                val intent = android.content.Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND")
                intent.putExtra("command", if (isPlaying) "play" else "pause")
                carContext.sendBroadcast(intent)
                invalidate()
            }
            .build()

        val nextAction = Action.Builder()
            .setTitle("⏭ Próximo")
            .setOnClickListener {
                val intent = android.content.Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND")
                intent.putExtra("command", "next")
                carContext.sendBroadcast(intent)
            }
            .build()

        val prevAction = Action.Builder()
            .setTitle("⏮ Anterior")
            .setOnClickListener {
                val intent = android.content.Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND")
                intent.putExtra("command", "previous")
                carContext.sendBroadcast(intent)
            }
            .build()

        val openAction = Action.Builder()
            .setTitle("🎬 Abrir")
            .setOnClickListener {
                val intent = android.content.Intent(carContext, WebViewActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                carContext.startActivity(intent)
            }
            .build()

        return MessageTemplate.Builder(
            if (isPlaying) "▶ Reproduzindo YouTube" else "⏸ Pausado"
        )
            .setTitle("AAuto YouTube")
            .setHeaderAction(Action.APP_ICON)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(openAction)
            .build()
    }
}