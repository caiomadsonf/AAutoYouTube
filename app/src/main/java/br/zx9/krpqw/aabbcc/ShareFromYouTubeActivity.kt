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
            prefs.edit().putString("shared_video_url", videoUrl).apply()
        }

        val intent = Intent(this, WebViewActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun extractYouTubeUrl(text: String): String {
        val regex = Regex("(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/(watch\\?v=|shorts/)?([\\w-]+)")
        val match = regex.find(text)
        return if (match != null) {
            "https://m.youtube.com/watch?v=${match.groupValues[5]}"
        } else {
            ""
        }
    }
}