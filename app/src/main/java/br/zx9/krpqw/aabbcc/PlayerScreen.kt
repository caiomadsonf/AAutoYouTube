package br.zx9.krpqw.aabbcc

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class PlayerScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("Toque em Abrir para assistir YouTube no seu carro!")
            .setTitle("AAuto YouTube")
            .addAction(
                Action.Builder()
                    .setTitle("Abrir YouTube")
                    .setOnClickListener {
                        val intent = Intent(carContext, WebViewActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        carContext.startActivity(intent)
                    }
                    .build()
            )
            .build()
    }
}