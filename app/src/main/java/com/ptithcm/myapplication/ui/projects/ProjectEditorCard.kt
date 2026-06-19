package com.ptithcm.myapplication.ui.projects

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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.ProjectStatus
import com.ptithcm.myapplication.data.ProjectSummary
import com.ptithcm.myapplication.data.UserAccount

@Composable
internal fun ProjectEditorCard(
    editingProject: ProjectSummary?,
    currentUserId: Long,
    users: List<UserAccount>,
    onCancelEdit: () -> Unit,
    onCreateProject: (String, String, ProjectStatus, Set<Long>) -> Boolean,
    onUpdateProject: (Long, String, String, ProjectStatus, Set<Long>) -> Boolean
) {
    val isEditMode = editingProject != null
    var name by remember(editingProject?.id) { mutableStateOf(editingProject?.name.orEmpty()) }
    var description by remember(editingProject?.id) { mutableStateOf(editingProject?.description.orEmpty()) }
    var status by remember(editingProject?.id) { mutableStateOf(editingProject?.status ?: ProjectStatus.PLANNING) }
    var memberIds by remember(editingProject?.id) {
        mutableStateOf(editingProject?.memberIds ?: setOf(currentUserId))
    }
    var errorMessage by remember(editingProject?.id) { mutableStateOf<String?>(null) }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Filled.Edit else Icons.Filled.AddBusiness,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = if (isEditMode) "Edit project" else "Create project",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Assign members and project status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Project name") }
            )
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Description") }
            )
            StatusSelector(
                status = status,
                onStatusChange = {
                    status = it
                    errorMessage = null
                }
            )
            MemberSelector(
                users = users,
                selectedIds = memberIds,
                onToggle = { userId ->
                    memberIds = if (memberIds.contains(userId)) memberIds - userId else memberIds + userId
                    errorMessage = null
                }
            )

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
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
                        val validationError = validateProject(name, memberIds)
                        if (validationError != null) {
                            errorMessage = validationError
                            return@Button
                        }

                        val saved = if (editingProject == null) {
                            onCreateProject(name, description, status, memberIds)
                        } else {
                            onUpdateProject(editingProject.id, name, description, status, memberIds)
                        }
                        if (saved && !isEditMode) {
                            name = ""
                            description = ""
                            status = ProjectStatus.PLANNING
                            memberIds = setOf(currentUserId)
                        }
                    }
                ) {
                    Text(if (isEditMode) "Save changes" else "Create project")
                }
            }
        }
    }
}

@Composable
private fun StatusSelector(status: ProjectStatus, onStatusChange: (ProjectStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Status", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }) {
            Text(status.value)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProjectStatus.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.value) },
                    onClick = {
                        onStatusChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MemberSelector(
    users: List<UserAccount>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Project members", style = MaterialTheme.typography.labelLarge)
        users.forEach { user ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedIds.contains(user.id),
                    onCheckedChange = { onToggle(user.id) }
                )
                Text("${user.fullName} - ${user.role.value}")
            }
        }
    }
}

private fun validateProject(name: String, memberIds: Set<Long>): String? = when {
    name.isBlank() -> "Project name is required"
    memberIds.isEmpty() -> "Select at least one member"
    else -> null
}
