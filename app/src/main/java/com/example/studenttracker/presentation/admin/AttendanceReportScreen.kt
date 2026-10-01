package com.example.studenttracker.presentation.admin

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.studenttracker.data.network.*
import com.example.studenttracker.domain.model.CsvExporter
import com.example.studenttracker.presentation.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportScreen(onNavigateBack: () -> Unit, subjectCode: String? = null) {
    var refresh by remember { mutableIntStateOf(0) }
    var selectedCode by rememberSaveable { mutableStateOf(subjectCode.orEmpty()) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportError by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    WorkspacePage("Attendance reports", onNavigateBack, actions = {
        TextButton(onClick = { refresh++ }, enabled = !exporting) { Text("Refresh") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            RemoteContent(refresh, { NetworkModule.api.getSubjects().subjects.orEmpty() }) { subjects ->
                if (subjects.isEmpty()) Text("No subjects are available yet.")
                else ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                    OutlinedTextField(subjects.find { it.subjectCode == selectedCode }?.let { "${it.subjectCode} · ${it.subjectName}" } ?: "Select subject",
                        onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        label = { Text("Subject") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        subjects.forEach { subject -> DropdownMenuItem(text = { Text("${subject.subjectCode} · ${subject.subjectName}") }, onClick = { selectedCode = subject.subjectCode; expanded = false; exportError = null }) }
                    }
                }
            }
            if (selectedCode.isNotBlank()) {
                val reportCode = selectedCode
                RemoteContent(reportCode to refresh, { NetworkModule.api.getAttendanceReport(subjectCode = reportCode).report.orEmpty() }) { report ->
                    if (report.isEmpty()) Text("No attendance records or enrolled students for this subject.")
                    else {
                        Text("Submitted sessions only · Late counts as present; excused is excluded.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(enabled = !exporting, onClick = {
                            exporting = true; exportError = null
                            scope.launch {
                                try {
                                    val file = withContext(Dispatchers.IO) { attendanceCsv(context, reportCode, report) }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        clipData = ClipData.newRawUri("Attendance report", uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share attendance report"))
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) { exportError = "Unable to export the report. Please try again." }
                                finally { exporting = false }
                            }
                        }) { Text(if (exporting) "Preparing export…" else "Export CSV") }
                        exportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(report, key = { it.studentId }) { item ->
                                InfoCard(item.name, item.studentId) {
                                    val percentage = item.percentage.toFloatOrNull()
                                    Text(if (percentage == null) "No counted sessions" else "${item.percentage}% · ${item.totalPresent}/${item.totalClasses} classes",
                                        color = if (percentage != null && percentage < 75) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            } else Text("Select a subject to view attendance.")
        }
    }
}

private fun attendanceCsv(context: Context, code: String, report: List<AttendanceReportItem>): File {
    val directory = File(context.cacheDir, "exports").apply { check(isDirectory || mkdirs()) }

    directory.listFiles()?.filter { it.isFile && it.lastModified() < System.currentTimeMillis() - 86_400_000 }?.forEach { it.delete() }
    val safeCode = code.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80)
    val file = File.createTempFile("Attendance_${safeCode}_", ".csv", directory)
    val rows = listOf(listOf("Student_ID", "Name", "Total_Present", "Total_Classes", "Percentage")) + report.map {
        listOf(it.studentId, it.name, it.totalPresent, it.totalClasses, it.percentage.ifBlank { "N/A" })
    }
    file.writeText(CsvExporter.encode(rows), Charsets.UTF_8)
    return file
}
