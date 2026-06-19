package com.ptithcm.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            MyApplicationTheme {
                TaskManagerApp(
                    database = database,
                    sessionManager = sessionManager
                )
            }
        }
    }

    override fun onDestroy() {
        database.close()
        super.onDestroy()
    }
}
