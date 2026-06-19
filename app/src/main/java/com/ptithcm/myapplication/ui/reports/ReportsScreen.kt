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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.MemberPerformanceReport
import com.ptithcm.myapplication.data.ProjectReport
import com.ptithcm.myapplication.data.ReportData
import com.ptithcm.myapplication.data.ReportMetric

@Composable
internal fun ReportsScreen(
    reportData: ReportData,
    onBack: () -> Unit
) {
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

        ReportsHeaderCard(reportData)
        ReportMetricSection(
            title = "Tasks by status",
            metrics = reportData.statusMetrics,
            color = MaterialTheme.colorScheme.primary
        )
        ReportMetricSection(
            title = "Tasks by priority",
            metrics = reportData.priorityMetrics,
            color = MaterialTheme.colorScheme.tertiary
        )
        ProjectReportSection(reportData.projectReports)
        MemberPerformanceSection(reportData.memberPerformance)
    }
}

@Composable
private fun ReportsHeaderCard(reportData: ReportData) {
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
                        text = "Project, task and member performance overview",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    title = "Projects",
                    value = reportData.totalProjects.toString(),
                    icon = Icons.Filled.Folder
                )
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    title = "Tasks",
                    value = reportData.totalTasks.toString(),
                    icon = Icons.Filled.Assessment
                )
                SummaryTile(
                    modifier = Modifier.weight(1f),
                    title = "Done",
                    value = "${reportData.completionPercent}%",
                    icon = Icons.Filled.CheckCircle
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportMetricSection(
    title: String,
    metrics: List<ReportMetric>,
    color: Color
) {
    ReportCard(title = title) {
        val maxCount = metrics.maxOfOrNull { it.count } ?: 0
        metrics.forEach { metric ->
            BarRow(
                label = metric.label,
                value = metric.count,
                maxValue = maxCount,
                color = color
            )
        }
    }
}

@Composable
private fun ProjectReportSection(projectReports: List<ProjectReport>) {
    ReportCard(title = "Tasks by project") {
        if (projectReports.isEmpty()) {
            Text("No project data.")
        } else {
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
}

@Composable
private fun MemberPerformanceSection(memberPerformance: List<MemberPerformanceReport>) {
    ReportCard(title = "Member performance") {
        if (memberPerformance.isEmpty()) {
            Text("No member task data.")
        } else {
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
}

@Composable
private fun ReportCard(
    title: String,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
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
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        helperText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
