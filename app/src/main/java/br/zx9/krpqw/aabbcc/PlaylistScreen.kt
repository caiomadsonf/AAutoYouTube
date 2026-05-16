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

    private val playlists = listOf(
        Pair("YouTube Principal", "https://m.youtube.com"),
        Pair("Em Alta", "https://m.youtube.com/feed/trending"),
        Pair("Músicas", "https://m.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ"),
        Pair("Podcasts", "https://m.youtube.com/podcasts"),
        Pair("Ao Vivo", "https://m.youtube.com/channel/UCVyTG4sCw-rOosB7gvTrPbw")
    )

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        playlists.forEach { (title, url) ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .setOnClickListener {
                        val prefs = carContext.getSharedPreferences("aauto", 0)
                        prefs.edit().putString("selected_url", url).apply()
                        val intent = Intent(carContext, WebViewActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        carContext.startActivity(intent)
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
}