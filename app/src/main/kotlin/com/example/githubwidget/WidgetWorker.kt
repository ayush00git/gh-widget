package com.example.githubwidget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.concurrent.TimeUnit

class WidgetWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @Serializable
    data class GraphQLRequest(val query: String)

    @Serializable
    data class GithubResponse(val data: DataBlock? = null)

    @Serializable
    data class DataBlock(val user: UserBlock? = null)

    @Serializable
    data class UserBlock(val contributionsCollection: ContributionsCollection? = null)

    @Serializable
    data class ContributionsCollection(val contributionCalendar: ContributionCalendar? = null)

    @Serializable
    data class ContributionCalendar(val weeks: List<Week> = emptyList())

    @Serializable
    data class Week(val contributionDays: List<ContributionDay> = emptyList())

    @Serializable
    data class ContributionDay(val contributionCount: Int, val date: String)

    override suspend fun doWork(): Result {
        val token = BuildConfig.GITHUB_TOKEN
        val username = BuildConfig.GITHUB_USERNAME

        if (token.isBlank() || username.isBlank()) {
            return Result.failure()
        }

        val client = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }

        val query = """
            query {
              user(login: "$username") {
                contributionsCollection {
                  contributionCalendar {
                    weeks {
                      contributionDays {
                        contributionCount
                        date
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        return try {
            val response: GithubResponse = client.post("https://api.github.com/graphql") {
                header("Authorization", "bearer $token")
                contentType(ContentType.Application.Json)
                setBody(GraphQLRequest(query))
            }.body()

            val weeks = response.data?.user?.contributionsCollection?.contributionCalendar?.weeks ?: emptyList()
            val counts = weeks.flatMapIndexed { index, week ->
                val days = week.contributionDays.map { it.contributionCount }
                if (days.size < 7) {
                    if (index == 0) {
                        List(7 - days.size) { 0 } + days
                    } else {
                        days + List(7 - days.size) { 0 }
                    }
                } else {
                    days
                }
            }

            val sharedPrefs = applicationContext.getSharedPreferences("github_widget_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("contributions", Json.encodeToString(counts)).apply()

            // Update widget
            GithubWidget().updateAll(applicationContext)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        } finally {
            client.close()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WidgetWorker>(3, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "GithubWidgetUpdate",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun fetchNow(context: Context) {
            val oneTimeRequest = androidx.work.OneTimeWorkRequestBuilder<WidgetWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "GithubWidgetUpdateNow",
                androidx.work.ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }
}
