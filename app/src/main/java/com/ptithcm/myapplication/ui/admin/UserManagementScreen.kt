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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    var showEditor by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            message?.let {
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

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
                        isError = error != null
                        message = error ?: "User created successfully"
                        error == null
                    },
                    onUpdateUser = { userId, fullName, role ->
                        val error = onUpdateUser(userId, fullName, role)
                        isError = error != null
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
                    isError = error != null
                    message = error ?: if (nextActive) "User unlocked" else "User locked"
                },
                onDeleteUser = { user ->
                    val error = onDeleteUser(user.id)
                    isError = error != null
                    message = error ?: "User deleted"
                    if (error == null && editingUser?.id == user.id) {
                        editingUser = null
                        showEditor = false
                    }
                }
            )
        }
    }
}

@Composable
private fun UserManagementHeader(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) {
        Icon(Icons.Filled.ArrowBack, contentDescription = null)
        Text("Back to home")
    }
}
