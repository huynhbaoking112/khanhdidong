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
import com.ptithcm.myapplication.data.UserRole
import com.ptithcm.myapplication.data.UserSaveResult
import com.ptithcm.myapplication.ui.admin.UserManagementScreen
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
    var usersVersion by remember { mutableStateOf(0) }

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

                screen == AppScreen.UserManagement && user.role == UserRole.ADMIN ->
                    UserManagementScreen(
                        users = remember(usersVersion) { database.listUsers() },
                        currentAdminId = user.id,
                        onBack = { screen = AppScreen.Home },
                        onCreateUser = { username, password, fullName, role ->
                            val error = database.createUser(username, password, fullName, role).toErrorMessage()
                            if (error == null) usersVersion++
                            error
                        },
                        onUpdateUser = { userId, fullName, role ->
                            if (userId == user.id && role != user.role) {
                                "Cannot change your own role"
                            } else {
                                val error = database.updateUser(userId, fullName, role).toErrorMessage()
                                if (error == null) {
                                    usersVersion++
                                    if (userId == user.id) currentUser = database.getUserById(user.id)
                                }
                                error
                            }
                        },
                        onToggleUserActive = { userId, isActive ->
                            if (userId == user.id) {
                                "Cannot lock your current account"
                            } else if (database.setUserActive(userId, isActive)) {
                                usersVersion++
                                null
                            } else {
                                "User not found"
                            }
                        },
                        onDeleteUser = { userId ->
                            if (userId == user.id) {
                                "Cannot delete your current account"
                            } else if (database.deleteUser(userId)) {
                                usersVersion++
                                null
                            } else {
                                "User not found"
                            }
                        }
                    )

                else -> HomeScreen(
                    user = user,
                    onManageUsers = { screen = AppScreen.UserManagement },
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
    ChangePassword,
    UserManagement
}

private fun UserSaveResult.toErrorMessage(): String? = when (this) {
    UserSaveResult.SUCCESS -> null
    UserSaveResult.USERNAME_EXISTS -> "Username already exists"
    UserSaveResult.NOT_FOUND -> "User not found"
}
