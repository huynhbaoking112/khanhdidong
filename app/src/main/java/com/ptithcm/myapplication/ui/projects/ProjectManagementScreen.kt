package com.ptithcm.myapplication.ui.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    var projectToDelete by remember { mutableStateOf<ProjectSummary?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
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
                .padding(innerPadding)
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
                if (canManage) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            editingProject = null
                            showEditor = true
                            message = null
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create project")
                    }
                }

                if (showEditor) {
                    ProjectEditorDialog(
                        editingProject = editingProject,
                        currentUserId = currentUser.id,
                        users = users.filter { it.isActive },
                        onDismiss = {
                            showEditor = false
                            editingProject = null
                        },
                        onCancelEdit = {
                            showEditor = false
                            editingProject = null
                            message = null
                        },
                        onCreateProject = { name, description, status, memberIds ->
                            val error = onCreateProject(name, description, status, memberIds)
                            message = error ?: "Project created successfully"
                            error == null
                        },
                        onUpdateProject = { projectId, name, description, status, memberIds ->
                            val error = onUpdateProject(projectId, name, description, status, memberIds)
                            message = error ?: "Project updated successfully"
                            error == null
                        }
                    )
                }

                ProjectListCard(
                    projects = projects,
                    canManage = canManage,
                    onEditProject = {
                        editingProject = it
                        showEditor = true
                        message = null
                    },
                    onDeleteProject = { project ->
                        projectToDelete = project
                        message = null
                    }
                )
            }
        }
    }

    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete project") },
            text = { Text("Are you sure you want to delete ${project.name}? Projects with active tasks cannot be deleted.") },
            dismissButton = {
                OutlinedButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val error = onDeleteProject(project.id)
                        message = error ?: "Project deleted"
                        if (error == null && editingProject?.id == project.id) {
                            editingProject = null
                            showEditor = false
                        }
                        projectToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            }
        )
    }
}

@Composable
private fun ProjectHeader(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack) {
        Icon(Icons.Filled.ArrowBack, contentDescription = null)
        Text("Back to home")
    }
}
