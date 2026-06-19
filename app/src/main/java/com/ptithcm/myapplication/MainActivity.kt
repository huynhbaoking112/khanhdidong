package com.ptithcm.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ptithcm.myapplication.data.AuthDatabaseHelper
import com.ptithcm.myapplication.data.SessionManager
import com.ptithcm.myapplication.ui.TaskManagerApp
import com.ptithcm.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var database: AuthDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AuthDatabaseHelper(applicationContext)
        val sessionManager = SessionManager(applicationContext)

        setContent {
            var themeMode by remember { mutableStateOf(sessionManager.getThemeMode()) }
            val darkTheme = sessionManager.isDarkTheme(themeMode, isSystemInDarkTheme())

            MyApplicationTheme(darkTheme = darkTheme) {
                TaskManagerApp(
                    database = database,
                    sessionManager = sessionManager,
                    themeMode = themeMode,
                    onThemeModeChange = {
                        sessionManager.saveThemeMode(it)
                        themeMode = it
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        database.close()
        super.onDestroy()
    }
}
