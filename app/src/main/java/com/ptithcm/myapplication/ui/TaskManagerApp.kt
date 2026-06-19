package com.ptithcm.myapplication.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ptithcm.myapplication.data.AuthDatabaseHelper
import com.ptithcm.myapplication.data.SessionManager
import com.ptithcm.myapplication.ui.auth.ChangePasswordScreen
import com.ptithcm.myapplication.ui.auth.LoginScreen
import com.ptithcm.myapplication.ui.home.HomeScreen

@Composable
internal fun TaskManagerApp(
    database: AuthDatabaseHelper,
    sessionManager: SessionManager
) {
    var currentUser by remember {
        mutableStateOf(sessionManager.getUserId()?.let(database::getUserById))
    }
    var screen by remember {
        mutableStateOf(if (currentUser == null) AppScreen.Login else AppScreen.Home)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            val user = currentUser
            when {
                user == null -> LoginScreen(
                    onLogin = { username, password ->
                        val loggedInUser = database.login(username, password)
                        if (loggedInUser == null) {
                            "Invalid username or password"
                        } else {
                            sessionManager.saveSession(loggedInUser)
                            currentUser = loggedInUser
                            screen = AppScreen.Home
                            null
                        }
                    }
                )

                screen == AppScreen.ChangePassword -> ChangePasswordScreen(
                    user = user,
                    onBack = { screen = AppScreen.Home },
                    onChangePassword = { currentPassword, newPassword ->
                        if (database.changePassword(user.id, currentPassword, newPassword)) {
                            null
                        } else {
                            "Current password is incorrect"
                        }
                    }
                )

                else -> HomeScreen(
                    user = user,
                    onChangePassword = { screen = AppScreen.ChangePassword },
                    onLogout = {
                        sessionManager.clearSession()
                        currentUser = null
                        screen = AppScreen.Login
                    }
                )
            }
        }
    }
}

private enum class AppScreen {
    Login,
    Home,
    ChangePassword
}
