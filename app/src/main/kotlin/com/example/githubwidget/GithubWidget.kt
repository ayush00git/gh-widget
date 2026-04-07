package com.example.githubwidget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // The widget needs 53 weeks x 7 days
                Row {
                    val weeksCount = 53
                    val daysInWeek = 7
                    // if contributions is empty or less we pad with 0
                    val paddedContributions = contributions.toMutableList()
                    val totalRequired = weeksCount * daysInWeek
                    if (paddedContributions.size < totalRequired) {
                        val missing = totalRequired - paddedContributions.size
                        paddedContributions.addAll(0, List(missing) { 0 })
                    }
                    // take last totalRequired
                    val finalData = paddedContributions.takeLast(totalRequired)

                    for (weekIndex in 0 until weeksCount) {
                        Column {
                            for (dayIndex in 0 until daysInWeek) {
                                val dataIndex = weekIndex * daysInWeek + dayIndex
                                val count = finalData.getOrNull(dataIndex) ?: 0
                                Box(
                                    modifier = GlanceModifier
                                        .size(8.dp)
                                        .background(
                                            if (count > 0) Color.White else Color(0x33FFFFFF)
                                        )
                                        .cornerRadius(4.dp)
                                ) {}
                                if (dayIndex < daysInWeek - 1) {
                                    Spacer(GlanceModifier.height(2.dp))
                                }
                            }
                        }
                        if (weekIndex < weeksCount - 1) {
                            Spacer(GlanceModifier.width(2.dp))
                        }
                    }
                }
            }
        }
    }
}
