package br.zx9.krpqw.aabbcc

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class PlaylistScreen(carContext: CarContext) : Screen(carContext) {

    // Cada entrada tem um título e um playlistId do YouTube.
    // O player HTML usa loadPlaylist({ list: playlistId }) — funciona sem URL.
    // Para adicionar mais playlists, basta incluir novas entradas aqui.
    private val playlists = listOf(
        PlaylistItem("Em Alta",       playlistId = null, videoId = null,          feedUrl = "https://www.youtube.com/feed/trending"),
        PlaylistItem("Músicas",       playlistId = "PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI", videoId = null, feedUrl = null),
        PlaylistItem("Podcasts",      playlistId = "PLbpi6ZahtOH6Ar_3GPy3workfCgiG2QDb", videoId = null, feedUrl = null),
        PlaylistItem("Ao Vivo",       playlistId = null, videoId = null,          feedUrl = "https://www.youtube.com/channel/UCVyTG4sCw-rOosB7gvTrPbw"),
        PlaylistItem("Recomendados",  playlistId = null, videoId = null,          feedUrl = "https://www.youtube.com")
    )

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        playlists.forEach { item ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(item.title)
                    .setOnClickListener {
                        // Salva a seleção para o WebViewActivity carregar
                        val prefs = carContext.getSharedPreferences("aauto", 0)
                        prefs.edit().apply {
                            putString("selected_playlist_id", item.playlistId)
                            putString("selected_video_id", item.videoId)
                            putString("selected_url", item.feedUrl ?: "https://www.youtube.com")
                            apply()
                        }

                        // Envia comando direto ao WebView se já estiver rodando
                        if (item.playlistId != null || item.videoId != null) {
                            val cmdIntent = Intent("br.zx9.krpqw.aabbcc.MEDIA_COMMAND").apply {
                                setPackage(carContext.packageName)
                                putExtra("command", "loadVideo")
                                putExtra("playlistId", item.playlistId)
                                putExtra("videoId", item.videoId)
                            }
                            carContext.sendBroadcast(cmdIntent)
                        } else {
                            // Feed sem playlistId: abre o WebView direto
                            carContext.startActivity(
                                Intent(carContext, WebViewActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }

                        // Navega para a tela Now Playing
                        screenManager.push(NowPlayingScreen(carContext))
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle("AAuto YouTube")
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(listBuilder.build())
            .build()
    }

    data class PlaylistItem(
        val title: String,
        val playlistId: String?,
        val videoId: String?,
        val feedUrl: String?
    )
}