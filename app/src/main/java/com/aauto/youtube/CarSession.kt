package com.aauto.youtube

import androidx.car.app.Screen
import androidx.car.app.Session

class CarSession : Session() {

    override fun onCreateScreen(intent: android.content.Intent): Screen {
        return PlayerScreen(carContext)
    }
}