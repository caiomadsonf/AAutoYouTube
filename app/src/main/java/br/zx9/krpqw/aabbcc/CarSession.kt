package br.zx9.krpqw.aabbcc

import androidx.car.app.Screen
import androidx.car.app.Session

class CarSession : Session() {

    override fun onCreateScreen(intent: android.content.Intent): Screen {
        return NowPlayingScreen(carContext)
    }
}