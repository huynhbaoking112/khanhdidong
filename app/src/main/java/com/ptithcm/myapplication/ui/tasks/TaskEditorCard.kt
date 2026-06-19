package com.ptithcm.myapplication.ui.tasks

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.TaskItem
import com.ptithcm.myapplication.data.TaskPriority
import com.ptithcm.myapplication.data.TaskStatus
import com.ptithcm.myapplication.data.UserAccount
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun TaskEditorDialog(
    editingTask: TaskItem?,
    projectId: Long,
    projectName: String,
    users: List<UserAccount>,
    onDismiss: () -> Unit,
    onCancelEdit: () -> Unit,
    onCreateTask: (String, String, Long, TaskStatus, TaskPriority, String) -> Boolean,
    onUpdateTask: (Long, String, String, Long, TaskStatus, TaskPriority, String) -> Boolean
) {
    val isEditMode = editingTask != null
    var title by remember(editingTask?.id) { mutableStateOf(editingTask?.title.orEmpty()) }
    var description by remember(editingTask?.id) { mutableStateOf(editingTask?.description.orEmpty()) }
    var assigneeId by remember(editingTask?.id, users) {
        mutableStateOf(editingTask?.assigneeId ?: users.firstOrNull()?.id ?: 0L)
    }
    var status by remember(editingTask?.id) { mutableStateOf(editingTask?.status ?: TaskStatus.TODO) }
    var priority by remember(editingTask?.id) { mutableStateOf(editingTask?.priority ?: TaskPriority.MEDIUM) }
    var dueDate by remember(editingTask?.id) { mutableStateOf(editingTask?.dueDate.orEmpty()) }
    var errorMessage by remember(editingTask?.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isEditMode) "Edit task" else "Create task",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = projectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
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
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Title") }
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
                DropdownSelector(
                    label = "Assignee",
                    value = users.firstOrNull { it.id == assigneeId }?.fullName ?: "No assignee",
                    options = users.map { it.id to "${it.fullName} - ${it.role.value}" },
                    onSelect = { assigneeId = it }
                )
                EnumSelector(
                    label = "Status",
                    value = status.value,
                    options = TaskStatus.values().map { it.value to it },
                    onSelect = { status = it }
                )
                EnumSelector(
                    label = "Priority",
                    value = priority.value,
                    options = TaskPriority.values().map { it.value to it },
                    onSelect = { priority = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {
                            dueDate = it
                            errorMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Due date") },
                        placeholder = { Text("yyyy-MM-dd") }
                    )
                    OutlinedButton(
                        onClick = {
                            val calendar = parseDateOrToday(dueDate)
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    dueDate = formatDate(calendar)
                                    errorMessage = null
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Text("Pick")
                    }
                }

                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
                    val validationError = validateTask(title, projectId, assigneeId, dueDate)
                    if (validationError != null) {
                        errorMessage = validationError
                        return@Button
                    }
                    val saved = if (editingTask == null) {
                        onCreateTask(title, description, assigneeId, status, priority, dueDate)
                    } else {
                        onUpdateTask(editingTask.id, title, description, assigneeId, status, priority, dueDate)
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
private fun DropdownSelector(
    label: String,
    value: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }) { Text(value) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        onSelect(option.first)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun <T> EnumSelector(
    label: String,
    value: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }) { Text(value) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.first) },
                    onClick = {
                        onSelect(option.second)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun validateTask(title: String, projectId: Long, assigneeId: Long, dueDate: String): String? = when {
    title.isBlank() -> "Title is required"
    projectId == 0L -> "Select a project"
    assigneeId == 0L -> "Select an assignee"
    dueDate.isBlank() -> "Due date is required"
    !isValidDate(dueDate.trim()) -> "Due date must be yyyy-MM-dd"
    else -> null
}

private fun parseDateOrToday(value: String): Calendar {
    val calendar = Calendar.getInstance()
    runCatching {
        SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }.parse(value.trim())
    }.getOrNull()?.let { calendar.time = it }
    return calendar
}

private fun formatDate(calendar: Calendar): String =
    SimpleDateFormat(DATE_PATTERN, Locale.US).format(calendar.time)

private fun isValidDate(value: String): Boolean =
    DATE_REGEX.matches(value) && runCatching {
        val format = SimpleDateFormat(DATE_PATTERN, Locale.US).apply { isLenient = false }
        val parsed = format.parse(value)
        parsed != null && format.format(parsed) == value
    }.getOrDefault(false)

private const val DATE_PATTERN = "yyyy-MM-dd"
private val DATE_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
