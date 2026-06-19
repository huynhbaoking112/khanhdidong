package com.ptithcm.myapplication.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.TaskItem
import com.ptithcm.myapplication.data.TaskPriority
import com.ptithcm.myapplication.data.TaskStatus

@Composable
internal fun TaskListCard(
    projectName: String,
    tasks: List<TaskItem>,
    canManage: Boolean,
    includeDeleted: Boolean,
    onIncludeDeletedChange: (Boolean) -> Unit,
    onViewTask: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onRestoreTask: (TaskItem) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$projectName (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trash")
                    Switch(checked = includeDeleted, onCheckedChange = onIncludeDeletedChange)
                }
            }

            if (tasks.isEmpty()) {
                EmptyTaskState(includeDeleted)
            } else {
                tasks.forEach { task ->
                    TaskRow(
                        task = task,
                        canManage = canManage,
                        onViewTask = onViewTask,
                        onEditTask = onEditTask,
                        onDeleteTask = onDeleteTask,
                        onRestoreTask = onRestoreTask
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskItem,
    canManage: Boolean,
    onViewTask: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onRestoreTask: (TaskItem) -> Unit
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = task.status.value,
                    color = task.status.color()
                )
            }
            Text(task.description.ifBlank { "No description" })
            Text("Assignee: ${task.assigneeName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Priority: ${task.priority.value} - Due: ${task.dueDate}" + if (task.isDeleted) " - Deleted" else "",
                color = task.priority.color()
            )
            Text("Progress: ${task.progress}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onViewTask(task) }
                ) {
                    Text("Details")
                }
                if (canManage) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        enabled = !task.isDeleted,
                        onClick = { onEditTask(task) }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("Edit")
                    }
                }
            }
            if (canManage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (task.isDeleted) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        },
                        onClick = { if (task.isDeleted) onRestoreTask(task) else onDeleteTask(task) }
                    ) {
                        Icon(
                            imageVector = if (task.isDeleted) Icons.Filled.Restore else Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(if (task.isDeleted) "Restore" else "Del")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTaskState(includeDeleted: Boolean) {
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
                imageVector = Icons.Filled.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (includeDeleted) "Trash is empty" else "No tasks found",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (includeDeleted) {
                        "Deleted tasks will appear here when trash is enabled."
                    } else {
                        "Create a task or clear filters to see more work items."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskStatus.color() = when (this) {
    TaskStatus.TODO -> MaterialTheme.colorScheme.primary
    TaskStatus.DOING -> MaterialTheme.colorScheme.tertiary
    TaskStatus.DONE -> MaterialTheme.colorScheme.secondary
    TaskStatus.OVERDUE -> MaterialTheme.colorScheme.error
}

@Composable
private fun TaskPriority.color() = when (this) {
    TaskPriority.LOW -> MaterialTheme.colorScheme.secondary
    TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
    TaskPriority.HIGH -> MaterialTheme.colorScheme.error
}
