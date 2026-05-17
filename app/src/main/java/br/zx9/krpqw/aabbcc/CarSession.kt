package br.zx9.krpqw.aabbcc

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class CarSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        // Inicia na playlist para o usuário escolher o que tocar.
        // PlaylistScreen navega para NowPlayingScreen ao selecionar um item.
        return PlaylistScreen(carContext)
    }
}