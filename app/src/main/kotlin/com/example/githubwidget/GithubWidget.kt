package com.example.githubwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class GithubWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sharedPrefs = context.getSharedPreferences("github_widget_prefs", Context.MODE_PRIVATE)
        val dataJson = sharedPrefs.getString("contributions", "[]") ?: "[]"
        val contributions: List<Int> = try {
            Json.decodeFromString(dataJson)
        } catch (e: Exception) {
            emptyList()
        }

        // Draw graph manually out of Glance's layout engine to avoid RemoteViews's strict rendering limits
        val weeksCount = 53
        val daysInWeek = 7
        
        val dotRadius = 9f
        val dotSize = dotRadius * 2
        val gap = 4f
        val w = (weeksCount * (dotSize + gap) - gap).toInt()
        val h = (daysInWeek * (dotSize + gap) - gap).toInt()
        
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paintLevel0 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.TRANSPARENT } 
        val paintLevel1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(102, 255, 255, 255) } // ~40%
        val paintLevel2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(153, 255, 255, 255) } // ~60%
        val paintLevel3 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(204, 255, 255, 255) } // ~80%
        val paintLevel4 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }

        val paddedContributions = contributions.toMutableList()
        val totalRequired = weeksCount * daysInWeek
        if (paddedContributions.size < totalRequired) {
            val missing = totalRequired - paddedContributions.size
            paddedContributions.addAll(0, List(missing) { 0 })
        }
        val finalData = paddedContributions.takeLast(totalRequired)

        for (weekIndex in 0 until weeksCount) {
            for (dayIndex in 0 until daysInWeek) {
                val dataIndex = weekIndex * daysInWeek + dayIndex
                val count = finalData.getOrNull(dataIndex) ?: 0
                val px = weekIndex * (dotSize + gap)
                val py = dayIndex * (dotSize + gap)
                val paint = when {
                    count == 0 -> paintLevel0
                    count in 1..2 -> paintLevel1
                    count in 3..5 -> paintLevel2
                    count in 6..9 -> paintLevel3
                    else -> paintLevel4
                }
                canvas.drawRoundRect(
                    RectF(px, py, px + dotSize, py + dotSize),
                    dotRadius, dotRadius, paint
                )
            }
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = "GitHub Contributions",
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
