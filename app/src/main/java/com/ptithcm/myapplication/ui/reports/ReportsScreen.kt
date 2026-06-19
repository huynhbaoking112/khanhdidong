package com.ptithcm.myapplication.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.MemberPerformanceReport
import com.ptithcm.myapplication.data.ProjectReport
import com.ptithcm.myapplication.data.ProjectSummary
import com.ptithcm.myapplication.data.ReportData
import com.ptithcm.myapplication.data.ReportMetric
import com.ptithcm.myapplication.data.TaskItem
import com.ptithcm.myapplication.data.TaskPriority
import com.ptithcm.myapplication.data.TaskStatus

@Composable
internal fun ReportsScreen(
    reportData: ReportData,
    projects: List<ProjectSummary>,
    tasks: List<TaskItem>,
    onBack: () -> Unit
) {
    var projectIdFilter by remember { mutableStateOf<Long?>(null) }
    var statusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var priorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }

    val filteredTasks = tasks
        .filter { task -> projectIdFilter == null || task.projectId == projectIdFilter }
        .filter { task -> statusFilter == null || task.status == statusFilter }
        .filter { task -> priorityFilter == null || task.priority == priorityFilter }
        .filter { task -> fromDate.isBlank() || task.dueDate >= fromDate.trim() }
        .filter { task -> toDate.isBlank() || task.dueDate <= toDate.trim() }
    val filteredReport = buildFilteredReport(projects, filteredTasks)
    val filtersActive = projectIdFilter != null || statusFilter != null || priorityFilter != null || fromDate.isNotBlank() || toDate.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back to home")
        }

        ReportsHeaderCard(filteredReport, reportData.totalTasks)
        ReportFiltersCard(
            projects = projects,
            projectIdFilter = projectIdFilter,
            statusFilter = statusFilter,
            priorityFilter = priorityFilter,
            fromDate = fromDate,
            toDate = toDate,
            onProjectFilterChange = { projectIdFilter = it },
            onStatusFilterChange = { statusFilter = it },
            onPriorityFilterChange = { priorityFilter = it },
            onFromDateChange = { fromDate = it },
            onToDateChange = { toDate = it },
            onClearFilters = {
                projectIdFilter = null
                statusFilter = null
                priorityFilter = null
                fromDate = ""
                toDate = ""
            }
        )

        if (filteredTasks.isEmpty()) {
            EmptyReportState(filtersActive)
        } else {
            ReportMetricSection(
                title = "Tasks by status",
                metrics = filteredReport.statusMetrics,
                color = MaterialTheme.colorScheme.primary
            )
            ReportMetricSection(
                title = "Tasks by priority",
                metrics = filteredReport.priorityMetrics,
                color = MaterialTheme.colorScheme.tertiary
            )
            ProjectReportSection(filteredReport.projectReports)
            MemberPerformanceSection(filteredReport.memberPerformance)
        }
    }
}

@Composable
private fun ReportsHeaderCard(reportData: ReportData, originalTaskCount: Int) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Assessment,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Reports",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Showing ${reportData.totalTasks} of $originalTaskCount tasks",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile(Modifier.weight(1f), "Projects", reportData.totalProjects.toString(), Icons.Filled.Folder)
                SummaryTile(Modifier.weight(1f), "Tasks", reportData.totalTasks.toString(), Icons.Filled.Assessment)
                SummaryTile(Modifier.weight(1f), "Done", "${reportData.completionPercent}%", Icons.Filled.CheckCircle)
            }
        }
    }
}

@Composable
private fun ReportFiltersCard(
    projects: List<ProjectSummary>,
    projectIdFilter: Long?,
    statusFilter: TaskStatus?,
    priorityFilter: TaskPriority?,
    fromDate: String,
    toDate: String,
    onProjectFilterChange: (Long?) -> Unit,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onPriorityFilterChange: (TaskPriority?) -> Unit,
    onFromDateChange: (String) -> Unit,
    onToDateChange: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    ReportCard(title = "Filters", icon = Icons.Filled.FilterAlt) {
        DropdownFilter(
            label = "Project",
            value = projects.firstOrNull { it.id == projectIdFilter }?.name ?: "All projects",
            options = listOf("All projects" to null) + projects.map { it.name to it.id },
            onSelect = onProjectFilterChange
        )
        DropdownFilter(
            label = "Status",
            value = statusFilter?.value ?: "All statuses",
            options = listOf("All statuses" to null) + TaskStatus.values().map { it.value to it },
            onSelect = onStatusFilterChange
        )
        DropdownFilter(
            label = "Priority",
            value = priorityFilter?.value ?: "All priorities",
            options = listOf("All priorities" to null) + TaskPriority.values().map { it.value to it },
            onSelect = onPriorityFilterChange
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = fromDate,
                onValueChange = onFromDateChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("From") },
                placeholder = { Text("yyyy-MM-dd") }
            )
            OutlinedTextField(
                value = toDate,
                onValueChange = onToDateChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("To") },
                placeholder = { Text("yyyy-MM-dd") }
            )
        }
        TextButton(onClick = onClearFilters) {
            Text("Clear filters")
        }
    }
}

@Composable
private fun <T> DropdownFilter(
    label: String,
    value: String,
    options: List<Pair<String, T?>>,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        ) {
            Text(value)
        }
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

@Composable
private fun EmptyReportState(filtersActive: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("No report data", fontWeight = FontWeight.SemiBold)
            Text(
                text = if (filtersActive) "No tasks match the selected filters." else "Create tasks to generate reports.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryTile(modifier: Modifier, title: String, value: String, icon: ImageVector) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportMetricSection(title: String, metrics: List<ReportMetric>, color: Color) {
    ReportCard(title = title, icon = Icons.Filled.Groups) {
        val maxCount = metrics.maxOfOrNull { it.count } ?: 0
        metrics.forEach { metric ->
            BarRow(metric.label, metric.count, maxCount, color)
        }
    }
}

@Composable
private fun ProjectReportSection(projectReports: List<ProjectReport>) {
    ReportCard(title = "Tasks by project", icon = Icons.Filled.Folder) {
        val maxTasks = projectReports.maxOfOrNull { it.totalTasks } ?: 0
        projectReports.forEach { project ->
            val donePercent = if (project.totalTasks == 0) 0 else project.doneTasks * 100 / project.totalTasks
            BarRow(
                label = project.projectName,
                value = project.totalTasks,
                maxValue = maxTasks,
                color = MaterialTheme.colorScheme.secondary,
                helperText = "${project.doneTasks}/${project.totalTasks} done - $donePercent%"
            )
        }
    }
}

@Composable
private fun MemberPerformanceSection(memberPerformance: List<MemberPerformanceReport>) {
    ReportCard(title = "Member performance", icon = Icons.Filled.Groups) {
        val maxDone = memberPerformance.maxOfOrNull { it.doneTasks } ?: 0
        memberPerformance.forEach { member ->
            val donePercent = if (member.totalTasks == 0) 0 else member.doneTasks * 100 / member.totalTasks
            BarRow(
                label = member.memberName,
                value = member.doneTasks,
                maxValue = maxDone,
                color = MaterialTheme.colorScheme.primary,
                helperText = "${member.doneTasks}/${member.totalTasks} completed - $donePercent%"
            )
        }
    }
}

@Composable
private fun ReportCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun BarRow(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color,
    helperText: String? = null
) {
    val fraction = if (maxValue == 0) 0f else value.toFloat() / maxValue.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(12.dp)
                    .background(color, RoundedCornerShape(50))
            )
        }
        helperText?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun buildFilteredReport(projects: List<ProjectSummary>, tasks: List<TaskItem>): ReportData {
    val filteredProjectIds = tasks.map { it.projectId }.toSet()
    val filteredProjects = projects.filter { filteredProjectIds.contains(it.id) }
    val completionPercent = if (tasks.isEmpty()) 0 else tasks.count { it.status == TaskStatus.DONE } * 100 / tasks.size

    return ReportData(
        totalProjects = filteredProjects.size,
        totalTasks = tasks.size,
        completionPercent = completionPercent,
        statusMetrics = TaskStatus.values().map { status -> ReportMetric(status.value, tasks.count { it.status == status }) },
        priorityMetrics = TaskPriority.values().map { priority -> ReportMetric(priority.value, tasks.count { it.priority == priority }) },
        projectReports = filteredProjects.map { project ->
            val projectTasks = tasks.filter { it.projectId == project.id }
            ProjectReport(project.name, projectTasks.size, projectTasks.count { it.status == TaskStatus.DONE })
        }.sortedByDescending { it.totalTasks },
        memberPerformance = tasks.groupBy { it.assigneeName }
            .map { (memberName, memberTasks) ->
                MemberPerformanceReport(memberName, memberTasks.size, memberTasks.count { it.status == TaskStatus.DONE })
            }
            .sortedByDescending { it.doneTasks }
    )
}
