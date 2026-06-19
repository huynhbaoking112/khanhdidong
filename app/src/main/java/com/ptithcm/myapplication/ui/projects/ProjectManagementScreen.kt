package com.ptithcm.myapplication.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.ProjectStatus
import com.ptithcm.myapplication.data.ProjectSummary
import com.ptithcm.myapplication.data.UserAccount
import com.ptithcm.myapplication.data.UserRole
import com.ptithcm.myapplication.data.UserSession

@Composable
internal fun ProjectManagementScreen(
    currentUser: UserSession,
    projects: List<ProjectSummary>,
    users: List<UserAccount>,
    onBack: () -> Unit,
    onCreateProject: (String, String, ProjectStatus, Set<Long>) -> String?,
    onUpdateProject: (Long, String, String, ProjectStatus, Set<Long>) -> String?,
    onDeleteProject: (Long) -> String?
) {
    val canManage = currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.MANAGER
    var editingProject by remember { mutableStateOf<ProjectSummary?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProjectHeader(onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            message?.let {
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            if (canManage) {
                ProjectEditorCard(
                    editingProject = editingProject,
                    currentUserId = currentUser.id,
                    users = users.filter { it.isActive },
                    onCancelEdit = {
                        editingProject = null
                        message = null
                    },
                    onCreateProject = { name, description, status, memberIds ->
                        val error = onCreateProject(name, description, status, memberIds)
                        isError = error != null
                        message = error ?: "Project created successfully"
                        error == null
                    },
                    onUpdateProject = { projectId, name, description, status, memberIds ->
                        val error = onUpdateProject(projectId, name, description, status, memberIds)
                        isError = error != null
                        message = error ?: "Project updated successfully"
                        if (error == null) editingProject = null
                        error == null
                    }
                )
            }

            ProjectListCard(
                projects = projects,
                canManage = canManage,
                onEditProject = {
                    editingProject = it
                    message = null
                },
                onDeleteProject = { project ->
                    val error = onDeleteProject(project.id)
                    isError = error != null
                    message = error ?: "Project deleted"
                    if (error == null && editingProject?.id == project.id) editingProject = null
                }
            )
        }
    }
}

@Composable
private fun ProjectHeader(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) {
        Icon(Icons.Filled.ArrowBack, contentDescription = null)
        Text("Back to home")
    }
}
