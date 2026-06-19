package com.ptithcm.myapplication.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.UserAccount

@Composable
internal fun UserListCard(
    users: List<UserAccount>,
    currentAdminId: Long,
    onEditUser: (UserAccount) -> Unit,
    onToggleUserActive: (UserAccount, Boolean) -> Unit,
    onDeleteUser: (UserAccount) -> Unit
) {
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
            Text(
                text = "Users (${users.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (users.isEmpty()) {
                EmptyUserState()
            } else {
                users.forEach { user ->
                    UserRow(
                        user = user,
                        isCurrentAdmin = user.id == currentAdminId,
                        onEditUser = onEditUser,
                        onToggleUserActive = onToggleUserActive,
                        onDeleteUser = onDeleteUser
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserAccount,
    isCurrentAdmin: Boolean,
    onEditUser: (UserAccount) -> Unit,
    onToggleUserActive: (UserAccount, Boolean) -> Unit,
    onDeleteUser: (UserAccount) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = user.fullName,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${user.username} - ${user.role.value}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = when {
                        isCurrentAdmin -> "Current"
                        user.isActive -> "Active"
                        else -> "Locked"
                    },
                    isActive = user.isActive
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onEditUser(user) }
                ) {
                    Text("Edit")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = !isCurrentAdmin,
                    onClick = { onToggleUserActive(user, !user.isActive) }
                ) {
                    Text(if (user.isActive) "Lock" else "Unlock")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !isCurrentAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { onDeleteUser(user) }
                ) {
                    Text("Del")
                }
            }
        }
    }
}

@Composable
private fun EmptyUserState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No users found", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Create accounts for managers and members.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    isActive: Boolean
) {
    val color = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        shape = CircleShape,
        color = color,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.Block,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
