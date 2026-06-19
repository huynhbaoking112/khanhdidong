package com.ptithcm.myapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.UserRole

@Composable
internal fun RoleAccessCard(role: UserRole) {
    val permissions = when (role) {
        UserRole.ADMIN -> listOf(
            "Manage users",
            "Manage all projects",
            "View all system data",
            "View reports and statistics"
        )

        UserRole.MANAGER -> listOf(
            "Manage assigned projects",
            "Assign tasks to members",
            "Track team progress",
            "View project reports"
        )

        UserRole.MEMBER -> listOf(
            "View assigned tasks",
            "Update task status",
            "Add work notes",
            "View joined projects"
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(
                icon = role.icon(),
                title = "Role permissions"
            )
            permissions.forEach { permission ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(text = permission)
                }
            }
        }
    }
}

@Composable
internal fun RoleWorkspaceCard(role: UserRole) {
    val title = when (role) {
        UserRole.ADMIN -> "Admin workspace"
        UserRole.MANAGER -> "Manager workspace"
        UserRole.MEMBER -> "Member workspace"
    }
    val description = when (role) {
        UserRole.ADMIN -> "You can access user, project, and full system management features."
        UserRole.MANAGER -> "You can manage projects, assign tasks, and monitor progress."
        UserRole.MEMBER -> "You can work on assigned tasks and update their status."
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(
                icon = role.icon(),
                title = title
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun UserRole.icon(): ImageVector = when (this) {
    UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
    UserRole.MANAGER -> Icons.Filled.Assignment
    UserRole.MEMBER -> Icons.Filled.Person
}
