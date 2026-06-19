package com.ptithcm.myapplication.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.ProjectSummary
import com.ptithcm.myapplication.data.TaskItem
import com.ptithcm.myapplication.data.TaskStatus
import com.ptithcm.myapplication.data.UserSession

@Composable
internal fun ProfileScreen(
    user: UserSession,
    projects: List<ProjectSummary>,
    assignedTasks: List<TaskItem>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateProfile: (String) -> String?
) {
    val completedTasks = assignedTasks.filter { it.status == TaskStatus.DONE }
    var message by remember(user.id) { mutableStateOf<String?>(null) }
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Back to home")
            }

            ProfileHeaderCard(user)
            ProfileEditorCard(
                user = user,
                onUpdateProfile = onUpdateProfile,
                onMessage = { message = it }
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSettings
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null)
                Text("Settings")
            }
            ProfileStatsCard(
                projectCount = projects.size,
                assignedCount = assignedTasks.size,
                completedCount = completedTasks.size
            )
            ProjectOverviewCard(projects)
            TaskOverviewCard(
                title = "Assigned tasks",
                tasks = assignedTasks,
                emptyText = "No assigned tasks yet."
            )
            TaskOverviewCard(
                title = "Completed tasks",
                tasks = completedTasks,
                emptyText = "No completed tasks yet."
            )
        }
    }
}

@Composable
private fun ProfileHeaderCard(user: UserSession) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${user.username} - ${user.role.value}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileEditorCard(
    user: UserSession,
    onUpdateProfile: (String) -> String?,
    onMessage: (String) -> Unit
) {
    var fullName by remember(user.id) { mutableStateOf(user.fullName) }
    val hasChanged = fullName.trim() != user.fullName

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Profile information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Full name") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = hasChanged,
                    onClick = {
                        fullName = user.fullName
                    }
                ) {
                    Text("Reset")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = hasChanged,
                    onClick = {
                        if (fullName.isBlank()) {
                            onMessage("Full name is required")
                        } else {
                            val cleanFullName = fullName.trim()
                            val error = onUpdateProfile(cleanFullName)
                            if (error == null) {
                                fullName = cleanFullName
                                onMessage("Profile updated")
                            } else {
                                onMessage(error)
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ProfileStatsCard(
    projectCount: Int,
    assignedCount: Int,
    completedCount: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Projects",
            value = projectCount.toString(),
            icon = Icons.Filled.Folder
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Assigned",
            value = assignedCount.toString(),
            icon = Icons.Filled.Assignment
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            title = "Done",
            value = completedCount.toString(),
            icon = Icons.Filled.CheckCircle
        )
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProjectOverviewCard(projects: List<ProjectSummary>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Projects joined",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (projects.isEmpty()) {
                EmptyProfileState("No projects joined", "Projects you manage or participate in will appear here.")
            } else {
                projects.forEach { project ->
                    InfoRow(
                        title = project.name,
                        subtitle = "${project.status.value} - Owner: ${project.createdByName}"
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskOverviewCard(
    title: String,
    tasks: List<TaskItem>,
    emptyText: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$title (${tasks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (tasks.isEmpty()) {
                EmptyProfileState(emptyText, "Task activity will appear here once work is assigned.")
            } else {
                tasks.forEach { task ->
                    InfoRow(
                        title = task.title,
                        subtitle = "${task.projectName} - ${task.status.value} - Due: ${task.dueDate}"
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProfileState(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
