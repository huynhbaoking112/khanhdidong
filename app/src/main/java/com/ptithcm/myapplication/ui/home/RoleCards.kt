package com.ptithcm.myapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Role permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            permissions.forEach { permission ->
                Text(text = "- $permission")
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

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
