package com.ptithcm.myapplication.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.UserAccount
import com.ptithcm.myapplication.data.UserRole

@Composable
internal fun UserManagementScreen(
    users: List<UserAccount>,
    currentAdminId: Long,
    onBack: () -> Unit,
    onCreateUser: (String, String, String, UserRole) -> String?,
    onUpdateUser: (Long, String, UserRole) -> String?,
    onToggleUserActive: (Long, Boolean) -> String?,
    onDeleteUser: (Long) -> String?
) {
    var editingUser by remember { mutableStateOf<UserAccount?>(null) }
    var userToDelete by remember { mutableStateOf<UserAccount?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            message = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserManagementHeader(onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingUser = null
                        showEditor = true
                        message = null
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create user")
                }

                if (showEditor) {
                    UserEditorDialog(
                        editingUser = editingUser,
                        currentAdminId = currentAdminId,
                        onDismiss = {
                            showEditor = false
                            editingUser = null
                        },
                        onCancelEdit = {
                            showEditor = false
                            editingUser = null
                            message = null
                        },
                        onCreateUser = { username, password, fullName, role ->
                            val error = onCreateUser(username, password, fullName, role)
                            message = error ?: "User created successfully"
                            error == null
                        },
                        onUpdateUser = { userId, fullName, role ->
                            val error = onUpdateUser(userId, fullName, role)
                            message = error ?: "User updated successfully"
                            error == null
                        }
                    )
                }

                UserListCard(
                    users = users,
                    currentAdminId = currentAdminId,
                    onEditUser = {
                        editingUser = it
                        showEditor = true
                        message = null
                    },
                    onToggleUserActive = { user, nextActive ->
                        val error = onToggleUserActive(user.id, nextActive)
                        message = error ?: if (nextActive) "User unlocked" else "User locked"
                    },
                    onDeleteUser = { userToDelete = it }
                )
            }
        }
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete user") },
            text = { Text("Delete ${user.fullName}? This account will be removed from active use.") },
            dismissButton = {
                OutlinedButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val error = onDeleteUser(user.id)
                        message = error ?: "User deleted"
                        if (error == null && editingUser?.id == user.id) {
                            editingUser = null
                            showEditor = false
                        }
                        userToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }
}

@Composable
private fun UserManagementHeader(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) {
        Icon(Icons.Filled.ArrowBack, contentDescription = null)
        Text("Back to home")
    }
}
