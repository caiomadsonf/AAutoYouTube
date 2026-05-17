package br.zx9.krpqw.aabbcc

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class NowPlayingScreen(carContext: CarContext) : Screen(carContext) {

    private var isPlaying = true

    override fun onGetTemplate(): Template {

        val openAction = Action.Builder()
            .setTitle("Abrir YouTube")
            .setOnClickListener {
                val intent = Intent(carContext, WebViewActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                carContext.startActivity(intent)
            }
            .build()

        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pausar" else "Play")
            .setOnClickListener {
                isPlaying = !isPlaying
                val intent = Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND")
                intent.putExtra("command", if (isPlaying) "play" else "pause")
                carContext.sendBroadcast(intent)
                invalidate()
            }
            .build()

        return MessageTemplate.Builder("Bem vindo ao AAuto YouTube!")
            .setTitle("AAuto YouTube")
            .setHeaderAction(Action.APP_ICON)
            .addAction(openAction)
            .addAction(playPauseAction)
            .build()
    }
}