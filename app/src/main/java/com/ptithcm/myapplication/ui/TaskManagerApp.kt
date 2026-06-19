package com.ptithcm.myapplication.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ptithcm.myapplication.data.AuthDatabaseHelper
import com.ptithcm.myapplication.data.ProjectSaveResult
import com.ptithcm.myapplication.data.SessionManager
import com.ptithcm.myapplication.data.UserRole
import com.ptithcm.myapplication.data.UserSaveResult
import com.ptithcm.myapplication.ui.admin.UserManagementScreen
import com.ptithcm.myapplication.ui.auth.ChangePasswordScreen
import com.ptithcm.myapplication.ui.auth.LoginScreen
import com.ptithcm.myapplication.ui.home.HomeScreen
import com.ptithcm.myapplication.ui.projects.ProjectManagementScreen

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
    var projectsVersion by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val user = currentUser
            if (user != null && screen != AppScreen.ChangePassword) {
                AppBottomMenu(
                    role = user.role,
                    selectedScreen = screen,
                    onSelect = { screen = it }
                )
            }
        }
    ) { innerPadding ->
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

                screen == AppScreen.ProjectManagement -> ProjectManagementScreen(
                    currentUser = user,
                    projects = remember(projectsVersion, user.id, user.role) {
                        database.listProjectsForUser(user)
                    },
                    users = remember(usersVersion) { database.listUsers() },
                    onBack = { screen = AppScreen.Home },
                    onCreateProject = { name, description, status, memberIds ->
                        val error = database.createProject(
                            name = name,
                            description = description,
                            status = status,
                            createdBy = user.id,
                            memberIds = memberIds
                        ).toErrorMessage()
                        if (error == null) projectsVersion++
                        error
                    },
                    onUpdateProject = { projectId, name, description, status, memberIds ->
                        val error = database.updateProject(
                            projectId = projectId,
                            name = name,
                            description = description,
                            status = status,
                            memberIds = memberIds
                        ).toErrorMessage()
                        if (error == null) projectsVersion++
                        error
                    },
                    onDeleteProject = { projectId ->
                        if (database.deleteProject(projectId)) {
                            projectsVersion++
                            null
                        } else {
                            "Project not found"
                        }
                    }
                )

                else -> HomeScreen(
                    user = user,
                    onManageUsers = { screen = AppScreen.UserManagement },
                    onManageProjects = { screen = AppScreen.ProjectManagement },
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

@Composable
private fun AppBottomMenu(
    role: UserRole,
    selectedScreen: AppScreen,
    onSelect: (AppScreen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedScreen == AppScreen.Home,
            onClick = { onSelect(AppScreen.Home) },
            icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedScreen == AppScreen.ProjectManagement,
            onClick = { onSelect(AppScreen.ProjectManagement) },
            icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
            label = { Text("Projects") }
        )
        if (role == UserRole.ADMIN) {
            NavigationBarItem(
                selected = selectedScreen == AppScreen.UserManagement,
                onClick = { onSelect(AppScreen.UserManagement) },
                icon = { Icon(Icons.Filled.ManageAccounts, contentDescription = null) },
                label = { Text("Users") }
            )
        }
    }
}

private enum class AppScreen {
    Login,
    Home,
    ChangePassword,
    UserManagement,
    ProjectManagement
}

private fun UserSaveResult.toErrorMessage(): String? = when (this) {
    UserSaveResult.SUCCESS -> null
    UserSaveResult.USERNAME_EXISTS -> "Username already exists"
    UserSaveResult.NOT_FOUND -> "User not found"
}

private fun ProjectSaveResult.toErrorMessage(): String? = when (this) {
    ProjectSaveResult.SUCCESS -> null
    ProjectSaveResult.DUPLICATE_NAME -> "Project name already exists"
    ProjectSaveResult.NOT_FOUND -> "Project not found"
}
