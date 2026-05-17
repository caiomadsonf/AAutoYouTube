package br.zx9.krpqw.aabbcc

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

// PlayerScreen agora serve como tela de boas-vindas / fallback.
// O fluxo principal é: PlaylistScreen → NowPlayingScreen.
class PlayerScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("Escolha uma playlist para começar.")
            .setTitle("AAuto YouTube")
            .setHeaderAction(Action.APP_ICON)
            .addAction(
                Action.Builder()
                    .setTitle("Ver playlists")
                    .setOnClickListener {
                        screenManager.push(PlaylistScreen(carContext))
                    }
                    .build()
            )
            .build()
    }
}