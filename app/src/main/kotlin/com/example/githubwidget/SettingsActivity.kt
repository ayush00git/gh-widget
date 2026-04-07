package com.example.githubwidget

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class SettingsActivity : Activity() {
    private lateinit var usernameInput: EditText
    private lateinit var saveButton: Button
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        preferencesManager = PreferencesManager(this)

        usernameInput = findViewById(R.id.username_input)
        saveButton = findViewById(R.id.save_button)

        // Load existing username
        usernameInput.setText(preferencesManager.getUsername())

        saveButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isBlank()) {
                Toast.makeText(this, "Please enter a GitHub username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save username
            preferencesManager.setUsername(username)
            
            // Show loading toast
            Toast.makeText(this, "Fetching contributions...", Toast.LENGTH_SHORT).show()
            
            // Disable button during fetch
            saveButton.isEnabled = false

            // Fetch data immediately in background
            CoroutineScope(Dispatchers.IO).launch {
                val data = GitHubFetcher.fetchContributions(username)
                if (data != null) {
                    preferencesManager.setContributions(Json.encodeToString(data))
                    runOnUiThread {
                        // Trigger widget update via broadcast
                        val intent = android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        intent.setClass(this@SettingsActivity, WidgetReceiver::class.java)
                        sendBroadcast(intent)
                        
                        Toast.makeText(this@SettingsActivity, "Widget updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    runOnUiThread {
                        saveButton.isEnabled = true
                        Toast.makeText(this@SettingsActivity, "Failed to fetch contributions. Check username.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

