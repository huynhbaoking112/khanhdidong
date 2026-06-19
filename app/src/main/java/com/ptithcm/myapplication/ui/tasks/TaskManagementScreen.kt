package com.ptithcm.myapplication.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.ProjectSummary
import com.ptithcm.myapplication.data.TaskItem
import com.ptithcm.myapplication.data.TaskPriority
import com.ptithcm.myapplication.data.TaskStatus
import com.ptithcm.myapplication.data.UserAccount
import com.ptithcm.myapplication.data.UserRole
import com.ptithcm.myapplication.data.UserSession

@Composable
internal fun TaskManagementScreen(
    currentUser: UserSession,
    tasks: List<TaskItem>,
    projects: List<ProjectSummary>,
    users: List<UserAccount>,
    includeDeleted: Boolean,
    onIncludeDeletedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onCreateTask: (String, String, Long, Long, TaskStatus, TaskPriority, String) -> String?,
    onUpdateTask: (Long, String, String, Long, Long, TaskStatus, TaskPriority, String) -> String?,
    onUpdateTaskDetails: (Long, TaskStatus, Int, String) -> String?,
    onDeleteTask: (Long) -> String?,
    onRestoreTask: (Long) -> String?
) {
    val canManage = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.MANAGER
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var detailTask by remember { mutableStateOf<TaskItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var selectedProjectId by remember(projects) { mutableStateOf(projects.firstOrNull()?.id ?: 0L) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var priorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }
    val projectTasks = tasks
        .filter { it.projectId == selectedProjectId }
        .filter { task ->
            val query = searchQuery.trim()
            query.isEmpty() ||
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true)
        }
        .filter { task -> statusFilter == null || task.status == statusFilter }
        .filter { task -> priorityFilter == null || task.priority == priorityFilter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Text("Back to home")
        }

        ProjectPickerCard(
            projects = projects,
            selectedProjectId = selectedProjectId,
            onSelectProject = {
                selectedProjectId = it
                editingTask = null
                message = null
                searchQuery = ""
                statusFilter = null
                priorityFilter = null
            }
        )

        TaskFilterCard(
            searchQuery = searchQuery,
            statusFilter = statusFilter,
            priorityFilter = priorityFilter,
            onSearchQueryChange = { searchQuery = it },
            onStatusFilterChange = { statusFilter = it },
            onPriorityFilterChange = { priorityFilter = it },
            onClearFilters = {
                searchQuery = ""
                statusFilter = null
                priorityFilter = null
            }
        )

        message?.let {
            Text(
                text = it,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        if (selectedProject == null) {
            Text("No project available. Create a project before adding tasks.")
        } else if (canManage) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    editingTask = null
                    showEditor = true
                    message = null
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create task")
            }
        }

        if (showEditor && selectedProject != null) {
            TaskEditorDialog(
                editingTask = editingTask,
                projectId = selectedProject.id,
                projectName = selectedProject.name,
                users = users.filter { it.isActive && selectedProject.memberIds.contains(it.id) },
                onDismiss = {
                    showEditor = false
                    editingTask = null
                },
                onCancelEdit = {
                    showEditor = false
                    editingTask = null
                    message = null
                },
                onCreateTask = { title, description, assigneeId, status, priority, dueDate ->
                    val error = onCreateTask(title, description, selectedProject.id, assigneeId, status, priority, dueDate)
                    isError = error != null
                    message = error ?: "Task created successfully"
                    error == null
                },
                onUpdateTask = { taskId, title, description, assigneeId, status, priority, dueDate ->
                    val error = onUpdateTask(taskId, title, description, selectedProject.id, assigneeId, status, priority, dueDate)
                    isError = error != null
                    message = error ?: "Task updated successfully"
                    error == null
                }
            )
        }

        detailTask?.let { task ->
            TaskDetailDialog(
                task = task,
                canManage = canManage,
                onDismiss = { detailTask = null },
                onSave = { status, progress, notes ->
                    val error = onUpdateTaskDetails(task.id, status, progress, notes)
                    isError = error != null
                    message = error ?: "Task detail updated"
                    error == null
                }
            )
        }

        TaskListCard(
            projectName = selectedProject?.name ?: "No project",
            tasks = projectTasks,
            canManage = canManage,
            includeDeleted = includeDeleted,
            onIncludeDeletedChange = onIncludeDeletedChange,
            onViewTask = {
                detailTask = it
                message = null
            },
            onEditTask = {
                editingTask = it
                showEditor = true
                message = null
            },
            onDeleteTask = { task ->
                val error = onDeleteTask(task.id)
                isError = error != null
                message = error ?: "Task moved to trash"
                if (error == null && editingTask?.id == task.id) editingTask = null
                if (error == null && detailTask?.id == task.id) detailTask = null
            },
            onRestoreTask = { task ->
                val error = onRestoreTask(task.id)
                isError = error != null
                message = error ?: "Task restored"
            }
        )
    }
}

@Composable
private fun TaskFilterCard(
    searchQuery: String,
    statusFilter: TaskStatus?,
    priorityFilter: TaskPriority?,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onPriorityFilterChange: (TaskPriority?) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                label = { Text("Search task") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EnumFilterButton(
                    modifier = Modifier.weight(1f),
                    label = "Status",
                    value = statusFilter?.value ?: "All status",
                    selected = statusFilter != null,
                    options = TaskStatus.values().map { it.value to it },
                    onSelect = onStatusFilterChange
                )
                EnumFilterButton(
                    modifier = Modifier.weight(1f),
                    label = "Priority",
                    value = priorityFilter?.value ?: "All priority",
                    selected = priorityFilter != null,
                    options = TaskPriority.values().map { it.value to it },
                    onSelect = onPriorityFilterChange
                )
            }

            if (searchQuery.isNotBlank() || statusFilter != null || priorityFilter != null) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear filters")
                }
            }
        }
    }
}

@Composable
private fun <T> EnumFilterButton(
    modifier: Modifier,
    label: String,
    value: String,
    selected: Boolean,
    options: List<Pair<String, T>>,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(value) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All $label") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
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

@Composable
private fun ProjectPickerCard(
    projects: List<ProjectSummary>,
    selectedProjectId: Long,
    onSelectProject: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedProject = projects.firstOrNull { it.id == selectedProjectId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Choose project",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Tasks below belong to the selected project only.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = projects.isNotEmpty(),
                onClick = { expanded = true }
            ) {
                Text(selectedProject?.name ?: "No project")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                projects.forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.name) },
                        onClick = {
                            onSelectProject(project.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
