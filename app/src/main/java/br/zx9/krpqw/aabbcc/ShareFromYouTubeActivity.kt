package br.zx9.krpqw.aabbcc

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class ShareFromYouTubeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val videoUrl = extractYouTubeUrl(sharedText)

        if (videoUrl.isNotEmpty()) {
            val prefs = getSharedPreferences("aauto", MODE_PRIVATE)
            prefs.edit()
                .putString("shared_video_url", videoUrl)
                .putString("selected_url", videoUrl)
                .apply()
        }

        startActivity(
            Intent(this, CarVideoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (videoUrl.isNotEmpty()) putExtra("url", videoUrl)
            }
        )

        finish()
    }

    private fun extractYouTubeUrl(text: String): String {
        val patterns = listOf(
            Regex("https?://(www\\.)?youtube\\.com/watch\\?[^\\s]*v=([a-zA-Z0-9_-]{11})"),
            Regex("https?://youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("https?://(www\\.)?youtube\\.com/shorts/([a-zA-Z0-9_-]{11})")
        )

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val videoId = match.groupValues.lastOrNull { it.length == 11 } ?: continue
            return "https://www.youtube.com/watch?v=$videoId"
        }

        return ""
    }
}
