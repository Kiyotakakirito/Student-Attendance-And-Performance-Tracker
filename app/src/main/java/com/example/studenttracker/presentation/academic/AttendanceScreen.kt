package com.example.studenttracker.presentation.academic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.*
import com.example.studenttracker.presentation.components.*
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(subjectCode: String, onBack: () -> Unit) {
    RemoteContent(subjectCode, { NetworkModule.api.getRoster(subjectCode) }, onBack = onBack) { roster ->
        var date by rememberSaveable { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
        var slot by rememberSaveable { mutableStateOf("") }
        var statusesJson by rememberSaveable { mutableStateOf("{}") }
        val statuses = remember(statusesJson) { Gson().fromJson(statusesJson, Map::class.java).map { it.key.toString() to it.value.toString() }.toMap() }
        var saving by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf<String?>(null) }
        var submitted by rememberSaveable { mutableStateOf(false) }
        var confirm by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val back = guardedBack(!submitted && (statuses.isNotEmpty() || slot.isNotBlank()), saving, onBack)
        val complete = roster.students.isNotEmpty() && roster.students.all { statuses[it.studentId] != null }
        fun submit() {
            saving = true; message = null; confirm = false
            val entries = roster.students.map { AttendanceEntry(it.studentId, statuses.getValue(it.studentId)) }
            val fingerprint = Gson().toJson(listOf(subjectCode, date, slot.trim(), entries))
            val requestId = UUID.nameUUIDFromBytes(fingerprint.toByteArray(Charsets.UTF_8)).toString()
            scope.launch {
                try { message = NetworkModule.api.saveAttendance(AttendanceRequest(subjectCode, date, slot.trim(), requestId, entries)).message; submitted = true }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { message = e.message }
                finally { saving = false }
            }
        }
        if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Submit attendance?") },
            text = { Text("$subjectCode · $date · $slot\n${roster.students.size} students. Submitted sessions are locked against duplicate entry.") },
            confirmButton = { TextButton(onClick = { submit() }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Review") } })
        WorkspacePage("Attendance · $subjectCode", back) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(date, { date = it }, enabled = !saving && !submitted, label = { Text("Date · YYYY-MM-DD") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(slot, { slot = it }, enabled = !saving && !submitted, label = { Text("Session / period") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                if (roster.students.isEmpty()) Text("No students are enrolled. Ask your administrator to set up enrollment.")
                else Text("${roster.students.count { statuses[it.studentId] != null }}/${roster.students.size} marked · Late counts as present; excused is excluded.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(enabled = !saving && !submitted && roster.students.isNotEmpty(), onClick = { statusesJson = Gson().toJson(roster.students.associate { it.studentId to "present" }) }) { Text("Mark all present") }
                message?.let { Text(it, color = if (submitted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(roster.students, key = { it.studentId }) { student ->
                        InfoCard(student.name, student.studentId) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("present", "absent", "late", "excused").forEach { status ->
                                    FilterChip(selected = statuses[student.studentId] == status, enabled = !saving && !submitted,
                                        onClick = { statusesJson = Gson().toJson(statuses + (student.studentId to status)) }, label = { Text(status.replaceFirstChar { it.uppercase() }) })
                                }
                            }
                        }
                    }
                }
                if (submitted) Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                else Button(onClick = { confirm = true }, enabled = complete && date.isNotBlank() && slot.isNotBlank() && !saving, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Submitting…" else "Review & submit") }
            }
        }
    }
}
