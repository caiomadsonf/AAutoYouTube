package br.zx9.krpqw.aabbcc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForegroundService(
            Intent(this, MusicService::class.java)
        )

        val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("logged_in", false)

        if (loggedIn) {
            startActivity(
                Intent(this, CarVideoActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            )
        } else {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
        }

        finish()
    }
}
