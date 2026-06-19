package com.ptithcm.myapplication.ui.tasks

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.TaskAttachment
import com.ptithcm.myapplication.data.TaskComment
import com.ptithcm.myapplication.data.TaskHistoryEntry
import com.ptithcm.myapplication.data.TaskItem
import com.ptithcm.myapplication.data.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun TaskDetailDialog(
    task: TaskItem,
    attachments: List<TaskAttachment>,
    comments: List<TaskComment>,
    history: List<TaskHistoryEntry>,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onSave: (TaskStatus, Int, String) -> Boolean,
    onAddAttachment: (String, String) -> Boolean,
    onDeleteAttachment: (Long) -> Boolean,
    onAddComment: (String) -> Boolean
) {
    var status by remember(task.id) { mutableStateOf(task.status) }
    var progressText by remember(task.id) { mutableStateOf(task.progress.toString()) }
    var notes by remember(task.id) { mutableStateOf(task.notes) }
    var commentText by remember(task.id) { mutableStateOf("") }
    var errorMessage by remember(task.id) { mutableStateOf<String?>(null) }
    val progress = progressText.toIntOrNull()?.coerceIn(0, 100) ?: 0
    val context = LocalContext.current
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val added = onAddAttachment(resolveDisplayName(context, uri), uri.toString())
            if (!added) errorMessage = "Could not attach file"
        }
    }

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
                    .padding(top = 4.dp)
                    .verticalScroll(rememberScrollState()),
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

                AttachmentSection(
                    attachments = attachments,
                    canManage = canManage && !task.isDeleted,
                    onAddAttachment = {
                        errorMessage = null
                        attachmentLauncher.launch(arrayOf("*/*"))
                    },
                    onOpenAttachment = { attachment ->
                        val opened = openAttachment(context, attachment.uri)
                        if (!opened) errorMessage = "No app can open this attachment"
                    },
                    onDeleteAttachment = { attachment ->
                        val deleted = onDeleteAttachment(attachment.id)
                        if (!deleted) errorMessage = "Could not delete attachment"
                    }
                )

                CommentSection(
                    comments = comments,
                    commentText = commentText,
                    canComment = !task.isDeleted,
                    onCommentTextChange = {
                        commentText = it
                        errorMessage = null
                    },
                    onAddComment = {
                        if (commentText.isBlank()) {
                            errorMessage = "Comment is required"
                        } else if (onAddComment(commentText)) {
                            commentText = ""
                        }
                    }
                )

                HistorySection(history)

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
private fun CommentSection(
    comments: List<TaskComment>,
    commentText: String,
    canComment: Boolean,
    onCommentTextChange: (String) -> Unit,
    onAddComment: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Comments", style = MaterialTheme.typography.labelLarge)
        if (canComment) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Add comment") }
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddComment
            ) {
                Icon(Icons.Filled.Send, contentDescription = null)
                Text("Post comment")
            }
        }

        if (comments.isEmpty()) {
            Text(
                text = "No comments yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            comments.forEach { comment ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(comment.content)
                    Text(
                        text = "${comment.authorName} - ${formatTimestamp(comment.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySection(history: List<TaskHistoryEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("History", style = MaterialTheme.typography.labelLarge)
        if (history.isEmpty()) {
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            history.forEach { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(entry.description)
                    Text(
                        text = "${entry.actorName} - ${formatTimestamp(entry.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentSection(
    attachments: List<TaskAttachment>,
    canManage: Boolean,
    onAddAttachment: () -> Unit,
    onOpenAttachment: (TaskAttachment) -> Unit,
    onDeleteAttachment: (TaskAttachment) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Attachments", style = MaterialTheme.typography.labelLarge)
            if (canManage) {
                OutlinedButton(onClick = onAddAttachment) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                    Text("Add")
                }
            }
        }

        if (attachments.isEmpty()) {
            Text(
                text = "No attachments yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            attachments.forEach { attachment ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = attachment.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onOpenAttachment(attachment) }) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Open attachment")
                    }
                    if (canManage) {
                        IconButton(onClick = { onDeleteAttachment(attachment) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete attachment",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
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

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    return cursor.use {
        if (it != null && it.moveToFirst()) {
            it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        } else {
            uri.lastPathSegment ?: "Attachment"
        }
    }
}

private fun openAttachment(context: android.content.Context, uri: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setData(Uri.parse(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

private fun formatTimestamp(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(value))
