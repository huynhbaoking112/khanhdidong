package com.ptithcm.myapplication.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.TaskItem
import com.ptithcm.myapplication.data.TaskStatus

@Composable
internal fun TaskDetailDialog(
    task: TaskItem,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onSave: (TaskStatus, Int, String) -> Boolean
) {
    var status by remember(task.id) { mutableStateOf(task.status) }
    var progressText by remember(task.id) { mutableStateOf(task.progress.toString()) }
    var notes by remember(task.id) { mutableStateOf(task.notes) }
    var errorMessage by remember(task.id) { mutableStateOf<String?>(null) }
    val progress = progressText.toIntOrNull()?.coerceIn(0, 100) ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${task.projectName} - ${task.assigneeName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(task.description.ifBlank { "No description" })
                Text("Priority: ${task.priority.value}")
                Text("Due date: ${task.dueDate}")

                StatusSelector(
                    status = status,
                    enabled = canManage && !task.isDeleted,
                    onStatusChange = {
                        status = it
                        if (it == TaskStatus.DONE) progressText = "100"
                        errorMessage = null
                    }
                )

                OutlinedTextField(
                    value = progressText,
                    onValueChange = {
                        progressText = it.filter(Char::isDigit).take(3)
                        errorMessage = null
                    },
                    enabled = canManage && !task.isDeleted,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Progress (%)") }
                )
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        notes = it
                        errorMessage = null
                    },
                    enabled = canManage && !task.isDeleted,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("Notes") }
                )

                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        confirmButton = {
            if (canManage && !task.isDeleted) {
                Button(
                    onClick = {
                        val parsedProgress = progressText.toIntOrNull()
                        if (parsedProgress == null || parsedProgress !in 0..100) {
                            errorMessage = "Progress must be from 0 to 100"
                            return@Button
                        }
                        if (onSave(status, parsedProgress, notes)) onDismiss()
                    }
                ) {
                    Text("Save")
                }
            }
        }
    )
}

@Composable
private fun StatusSelector(
    status: TaskStatus,
    enabled: Boolean,
    onStatusChange: (TaskStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Status", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            enabled = enabled,
            onClick = { expanded = true }
        ) {
            Text(status.value)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskStatus.values().forEach { option ->
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
