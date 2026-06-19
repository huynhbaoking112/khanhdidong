package com.ptithcm.myapplication.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.UserSession

@Composable
internal fun ChangePasswordScreen(
    user: UserSession,
    onBack: () -> Unit,
    onChangePassword: (String, String) -> String?
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Change password",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Account: ${user.username} (${user.role.value})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PasswordField(
                    value = currentPassword,
                    label = "Current password",
                    onValueChange = {
                        currentPassword = it
                        errorMessage = null
                        successMessage = null
                    }
                )
                PasswordField(
                    value = newPassword,
                    label = "New password",
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                        successMessage = null
                    }
                )
                PasswordField(
                    value = confirmPassword,
                    label = "Confirm new password",
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                        successMessage = null
                    }
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                successMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val validationError = validatePasswordForm(
                                currentPassword,
                                newPassword,
                                confirmPassword
                            )
                            errorMessage = validationError ?: onChangePassword(currentPassword, newPassword)
                            if (errorMessage == null) {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                successMessage = "Password changed successfully"
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation()
    )
}

private fun validatePasswordForm(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String
): String? = when {
    currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() ->
        "Please fill all password fields"

    newPassword.length < 6 -> "New password must be at least 6 characters"
    newPassword != confirmPassword -> "New password confirmation does not match"
    currentPassword == newPassword -> "New password must be different"
    else -> null
}
