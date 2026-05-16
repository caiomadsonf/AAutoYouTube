package com.aauto.youtube

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class PlayerScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("Abrindo YouTube...")
            .setTitle("AAuto YouTube")
            .addAction(
                Action.Builder()
                    .setTitle("Abrir YouTube")
                    .setOnClickListener {
                        carContext.startCarApp(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://m.youtube.com")
                            )
                        )
                    }
                    .build()
            )
            .build()
    }
}