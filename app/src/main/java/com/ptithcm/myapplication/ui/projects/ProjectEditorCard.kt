package com.ptithcm.myapplication.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.ProjectStatus
import com.ptithcm.myapplication.data.ProjectSummary
import com.ptithcm.myapplication.data.UserAccount

@Composable
internal fun ProjectEditorDialog(
    editingProject: ProjectSummary?,
    currentUserId: Long,
    users: List<UserAccount>,
    onDismiss: () -> Unit,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isEditMode) "Edit project" else "Create project",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Assign members and project status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .padding(top = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    if (saved) onDismiss()
                }
            ) {
                Text(if (isEditMode) "Save" else "Create")
            }
        }
    )
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
