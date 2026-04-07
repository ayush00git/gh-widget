package com.example.githubwidget

import android.content.Context

class PreferencesManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "github_widget_prefs"
        private const val KEY_USERNAME = "github_username"
        private const val KEY_CONTRIBUTIONS = "contributions"
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUsername(): String = sharedPreferences.getString(KEY_USERNAME, "") ?: ""

    fun setUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username.trim()).apply()
    }

    fun getContributions(): String = sharedPreferences.getString(KEY_CONTRIBUTIONS, "") ?: ""

    fun setContributions(contributions: String) {
        sharedPreferences.edit().putString(KEY_CONTRIBUTIONS, contributions).apply()
    }

    fun isUsernameSet(): Boolean = getUsername().isNotBlank()
}
