package br.zx9.krpqw.aabbcc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class NowPlayingScreen(carContext: CarContext) : Screen(carContext) {

    private var isPlaying = false
    private var currentTitle = "Aguardando reproducao..."
    private var stateReceiver: BroadcastReceiver? = null
    private var progressReceiver: BroadcastReceiver? = null

    init {
        // Screen implementa LifecycleOwner — usamos DefaultLifecycleObserver
        // para registrar/desregistrar receivers junto com o ciclo de vida da tela.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = registerReceivers()
            override fun onStop(owner: LifecycleOwner) = unregisterReceivers()
        })
    }

    override fun onGetTemplate(): Template {
        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pausar" else "Play")
            .setOnClickListener {
                val cmd = if (isPlaying) "pause" else "play"
                carContext.sendBroadcast(
                    Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND").apply {
                        setPackage(carContext.packageName)
                        putExtra("command", cmd)
                    }
                )
            }
            .build()

        val nextAction = Action.Builder()
            .setTitle("Proximo")
            .setOnClickListener {
                carContext.sendBroadcast(
                    Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND").apply {
                        setPackage(carContext.packageName)
                        putExtra("command", "next")
                    }
                )
            }
            .build()

        return MessageTemplate.Builder(currentTitle)
            .setTitle("AAuto YouTube")
            .setHeaderAction(Action.BACK)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .build()
    }

    private fun registerReceivers() {
        stateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = intent.getStringExtra("state") ?: return
                val wasPlaying = isPlaying
                isPlaying = state == "playing" || state == "buffering"
                if (isPlaying != wasPlaying) invalidate()
            }
        }
        carContext.registerReceiver(
            stateReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.PLAYER_STATE"),
            Context.RECEIVER_NOT_EXPORTED
        )

        progressReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val title = intent.getStringExtra("title") ?: return
                if (title.isNotEmpty() && title != currentTitle) {
                    currentTitle = title
                    invalidate()
                }
            }
        }
        carContext.registerReceiver(
            progressReceiver,
            IntentFilter("br.zx9.krpqw.aabbcc.PLAYER_PROGRESS"),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterReceivers() {
        stateReceiver?.let { runCatching { carContext.unregisterReceiver(it) } }
        progressReceiver?.let { runCatching { carContext.unregisterReceiver(it) } }
        stateReceiver = null
        progressReceiver = null
    }
}