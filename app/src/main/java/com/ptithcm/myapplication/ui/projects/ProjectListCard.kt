package com.ptithcm.myapplication.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
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
import com.ptithcm.myapplication.data.ProjectStatus
import com.ptithcm.myapplication.data.ProjectSummary

@Composable
internal fun ProjectListCard(
    projects: List<ProjectSummary>,
    canManage: Boolean,
    onEditProject: (ProjectSummary) -> Unit,
    onDeleteProject: (ProjectSummary) -> Unit
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
                text = "Projects (${projects.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (projects.isEmpty()) {
                Text("No projects found.")
            } else {
                projects.forEach { project ->
                    ProjectRow(
                        project = project,
                        canManage = canManage,
                        onEditProject = onEditProject,
                        onDeleteProject = onDeleteProject
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: ProjectSummary,
    canManage: Boolean,
    onEditProject: (ProjectSummary) -> Unit,
    onDeleteProject: (ProjectSummary) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = project.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusPill(project.status)
            }
            Text(
                text = project.description.ifBlank { "No description" },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Owner: ${project.createdByName}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = project.memberNames.joinToString().ifBlank { "No members" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canManage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onEditProject(project) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Text("Edit")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onDeleteProject(project) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Text("Del")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: ProjectStatus) {
    val color = when (status) {
        ProjectStatus.PLANNING -> MaterialTheme.colorScheme.tertiary
        ProjectStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        ProjectStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
