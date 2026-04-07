package com.example.githubwidget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class GithubWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sharedPrefs = context.getSharedPreferences("github_widget_prefs", Context.MODE_PRIVATE)
        val dataJson = sharedPrefs.getString("contributions", "[]") ?: "[]"
        val username = sharedPrefs.getString("github_username", "") ?: ""
        
        val contributions: List<Int> = try {
            Json.decodeFromString(dataJson)
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (username.isBlank()) {
                    Text("Add GitHub username")
                } else if (contributions.isEmpty()) {
                    Text("Loading contributions...")
                } else {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            username,
                            style = TextStyle(fontSize = 14.sp)
                        )
                        Text(
                            "${contributions.size} days tracked",
                            style = TextStyle(fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}

