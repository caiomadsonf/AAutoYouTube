package br.zx9.krpqw.aabbcc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicia o MusicService em foreground imediatamente.
        // Isso é essencial: o Android Auto procura um MediaBrowserService ativo.
        // Se o serviço não estiver rodando quando o DHU conectar, o app não aparece.
        val serviceIntent = Intent(this, MusicService::class.java)
        startForegroundService(serviceIntent)

        val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
        val loggedIn = prefs.getBoolean("logged_in", false)

        if (loggedIn) {
            // Usuário já logou antes: vai direto para o WebView
            startActivity(Intent(this, WebViewActivity::class.java))
        } else {
            // Primeiro acesso: fluxo de login
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Fecha a MainActivity — ela não tem layout próprio,
        // só serve de ponto de entrada para iniciar o serviço e redirecionar.
        finish()
    }
}