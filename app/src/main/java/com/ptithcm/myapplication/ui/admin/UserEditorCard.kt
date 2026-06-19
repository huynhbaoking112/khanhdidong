package com.ptithcm.myapplication.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
internal fun UserEditorCard(
    editingUser: UserAccount?,
    currentAdminId: Long,
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

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Filled.Edit else Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = if (isEditMode) "Edit user" else "Create user",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isEditMode) "Update profile and role" else "Create an account for your team",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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

            Row(modifier = Modifier.fillMaxWidth()) {
                if (isEditMode) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onCancelEdit
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Button(
                    modifier = Modifier.weight(1f),
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
                        if (saved && !isEditMode) {
                            username = ""
                            password = ""
                            fullName = ""
                            role = UserRole.MEMBER
                        }
                    }
                ) {
                    Text(if (isEditMode) "Save changes" else "Create account")
                }
            }
        }
    }
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
