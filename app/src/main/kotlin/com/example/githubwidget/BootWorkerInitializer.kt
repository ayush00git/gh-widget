package com.example.githubwidget

import android.content.Context
import androidx.startup.Initializer

class BootWorkerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        WidgetWorker.schedule(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
