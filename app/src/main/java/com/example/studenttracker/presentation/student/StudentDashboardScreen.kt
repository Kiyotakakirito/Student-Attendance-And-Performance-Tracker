package com.example.studenttracker.presentation.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.*
import com.example.studenttracker.presentation.components.*

@Composable
fun StudentDashboardScreen(onLogout: () -> Unit) {
    val user by AuthSession.user.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    WorkspacePage("My progress", actions = {
        TextButton(onClick = { refresh++ }) { Text("Refresh") }
        TextButton(onClick = onLogout) { Text("Logout") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            RemoteContent(refresh, { NetworkModule.api.getMyProgress() }) { progress ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text("Welcome, ${user?.name.orEmpty()}", style = MaterialTheme.typography.headlineSmall) }
                    item { Text("Attendance", style = MaterialTheme.typography.titleLarge) }
                    if (progress.attendance.isEmpty()) item { InfoCard("No attendance yet", "Your enrolled subjects and submitted sessions will appear here.") }
                    progress.attendance.forEach { record -> item(key = "attendance-${record.subjectCode}") {
                        InfoCard(record.subjectName, record.subjectCode) {
                            val percentage = record.percentage.toFloatOrNull()
                            Text(if (percentage == null) "No counted classes yet" else "${record.percentage}% · ${record.totalPresent}/${record.totalClasses} classes")
                            if (percentage != null) LinearProgressIndicator(progress = { (percentage / 100).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        }
                    } }
                    item { Text("Published results", style = MaterialTheme.typography.titleLarge) }
                    if (progress.results.isEmpty()) item { InfoCard("No published results", "Results appear after your faculty publishes them.") }
                    progress.results.forEach { result -> item(key = "result-${result.assessmentId}") {
                        InfoCard(result.title, result.subjectCode) {
                            Text(if (result.status == "absent") "Absent" else "${result.score} / ${result.maxMarks}", style = MaterialTheme.typography.headlineSmall)
                        }
                    } }
                }
            }
        }
    }
}
