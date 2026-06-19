package com.ptithcm.myapplication.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.UserAccount
import com.ptithcm.myapplication.data.UserRole

@Composable
internal fun UserEditorDialog(
    editingUser: UserAccount?,
    currentAdminId: Long,
    onDismiss: () -> Unit,
    onCancelEdit: () -> Unit,
    onCreateUser: (String, String, String, UserRole) -> Boolean,
    onUpdateUser: (Long, String, UserRole) -> Boolean
) {
    val isEditMode = editingUser != null
    val isEditingSelf = editingUser?.id == currentAdminId
    var username by remember(editingUser?.id) { mutableStateOf(editingUser?.username.orEmpty()) }
    var password by remember(editingUser?.id) { mutableStateOf("") }
    var fullName by remember(editingUser?.id) { mutableStateOf(editingUser?.fullName.orEmpty()) }
    var role by remember(editingUser?.id) { mutableStateOf(editingUser?.role ?: UserRole.MEMBER) }
    var errorMessage by remember(editingUser?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isEditMode) "Edit user" else "Create user",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isEditMode) "Update profile and role" else "Create an account for your team",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isEditMode,
                singleLine = true,
                label = { Text("Username") }
            )

            if (!isEditMode) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Full name") }
            )

            RoleSelector(
                role = role,
                enabled = !isEditingSelf,
                onRoleChange = {
                    role = it
                    errorMessage = null
                }
            )

            if (isEditingSelf) {
                Text(
                    text = "You cannot change your own role.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelEdit) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validationError = validateUserForm(
                        username = username,
                        password = password,
                        fullName = fullName,
                        isEditMode = isEditMode
                    )
                    if (validationError != null) {
                        errorMessage = validationError
                        return@Button
                    }

                    val saved = if (editingUser == null) {
                        onCreateUser(username, password, fullName, role)
                    } else {
                        onUpdateUser(editingUser.id, fullName, role)
                    }
                    if (saved) onDismiss()
                }
            ) {
                Text(if (isEditMode) "Save" else "Create")
            }
        }
    )
}

@Composable
private fun RoleSelector(
    role: UserRole,
    enabled: Boolean,
    onRoleChange: (UserRole) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Role",
            style = MaterialTheme.typography.labelLarge
        )
        OutlinedButton(
            enabled = enabled,
            onClick = { expanded = true }
        ) {
            Text(role.value)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            UserRole.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.value) },
                    onClick = {
                        onRoleChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun validateUserForm(
    username: String,
    password: String,
    fullName: String,
    isEditMode: Boolean
): String? = when {
    username.isBlank() -> "Username is required"
    fullName.isBlank() -> "Full name is required"
    !isEditMode && password.isBlank() -> "Password is required"
    !isEditMode && password.length < 6 -> "Password must be at least 6 characters"
    else -> null
}
