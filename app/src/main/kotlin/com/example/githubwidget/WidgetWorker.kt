package com.example.githubwidget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.util.concurrent.TimeUnit

class WidgetWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val preferencesManager = PreferencesManager(applicationContext)
        val username = preferencesManager.getUsername()

        if (username.isBlank()) {
            // No username set, skip update
            return Result.success()
        }

        return try {
            val contributions = GitHubFetcher.fetchContributions(username)
            
            if (contributions != null) {
                preferencesManager.setContributions(Json.encodeToString(contributions))
                // Update widget
                GithubWidget().updateAll(applicationContext)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
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

