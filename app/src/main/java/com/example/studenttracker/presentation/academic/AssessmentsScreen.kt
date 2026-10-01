package com.example.studenttracker.presentation.academic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.*
import com.example.studenttracker.presentation.components.*
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AssessmentsScreen(subjectCode: String, onBack: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    var selectedId by rememberSaveable(subjectCode) { mutableStateOf<String?>(null) }
    RemoteContent(subjectCode to refresh, { NetworkModule.api.getAssessments(subjectCode).assessments to NetworkModule.api.getRoster(subjectCode).students }, onBack = onBack) { (assessments, students) ->
        if (selectedId != null) {
            val existing = assessments.find { it.assessmentId == selectedId }
            key(selectedId) {
                AssessmentEditor(subjectCode, selectedId!!, existing, students) { selectedId = null; refresh++ }
            }
        } else WorkspacePage("Assessments · $subjectCode", onBack, actions = { TextButton(onClick = { refresh++ }) { Text("Refresh") } }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Button(onClick = { selectedId = UUID.randomUUID().toString() }, enabled = students.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Create assessment") } }
                if (students.isEmpty()) item { Text("Enroll students before creating an assessment.") }
                if (assessments.isEmpty()) item { InfoCard("No assessments yet", "Create an assessment, enter marks, then publish results to students.") }
                items(assessments, key = { it.assessmentId }) { assessment ->
                    InfoCard(assessment.title, "Out of ${assessment.maxMarks} · ${if (assessment.published) "Published" else "Draft"}") {
                        OutlinedButton(onClick = { selectedId = assessment.assessmentId }) { Text(if (assessment.published) "View results" else "Continue entering marks") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssessmentEditor(subjectCode: String, assessmentId: String, existing: Assessment?, currentRoster: List<RosterStudent>, onBack: () -> Unit) {
    val roster = if (existing?.published == true) existing.marks.map { RosterStudent(it.studentId, it.name) } else currentRoster
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var maxMarks by rememberSaveable { mutableStateOf(existing?.maxMarks?.toString() ?: "100") }
    val initial = roster.associate { student ->
        val mark = existing?.marks?.find { it.studentId == student.studentId }
        student.studentId to when (mark?.status) { "absent" -> "A"; "scored" -> mark.score.toString(); else -> "" }
    }
    var valuesJson by rememberSaveable { mutableStateOf(Gson().toJson(initial)) }
    val values = remember(valuesJson) { Gson().fromJson(valuesJson, Map::class.java).map { it.key.toString() to it.value.toString() }.toMap() }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val locked = existing?.published == true
    val dirty = title != existing?.title.orEmpty() || maxMarks != (existing?.maxMarks?.toString() ?: "100") || values != initial
    val back = guardedBack(dirty && !locked, saving, onBack)
    val scope = rememberCoroutineScope()
    fun save(publish: Boolean) {
        error = null; confirm = false
        val maximum = maxMarks.toDoubleOrNull()
        if (title.isBlank() || maximum == null || !maximum.isFinite() || maximum < 1 || maximum > 10000) {
            error = "Enter a title and maximum marks between 1 and 10,000."; return
        }
        val marks = roster.map { student ->
            val value = values[student.studentId].orEmpty().trim()
            MarkEntry(student.studentId, when { value.equals("A", true) -> "absent"; value.isBlank() -> "pending"; else -> "scored" }, value.toDoubleOrNull())
        }
        if (marks.any { it.status == "scored" && (it.score == null || !it.score.isFinite() || it.score < 0 || it.score > maximum) }) {
            error = "Each mark must be between 0 and $maximum. Use A for absent or leave blank for pending."; return
        }
        if (publish && marks.any { it.status == "pending" }) { error = "Complete every student's marks before publishing."; return }
        val fingerprint = Gson().toJson(listOf(assessmentId, title.trim(), maximum, marks, publish, existing?.revision ?: 0))
        val requestId = UUID.nameUUIDFromBytes(fingerprint.toByteArray(Charsets.UTF_8)).toString()
        saving = true
        scope.launch {
            try {
                NetworkModule.api.saveAssessment(AssessmentRequest(assessmentId, subjectCode, title.trim(), maximum, marks, publish, existing?.revision ?: 0, requestId))
                onBack()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message }
            finally { saving = false }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Publish results?") },
        text = { Text("Students will be able to see their own marks. Published results are locked in this version.") },
        confirmButton = { TextButton(onClick = { save(true) }) { Text("Publish") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Review") } })
    WorkspacePage(if (locked) "Published results" else "Enter marks", back) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, enabled = !locked && !saving, label = { Text("Assessment title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(maxMarks, { maxMarks = it }, enabled = !locked && !saving, label = { Text("Maximum marks") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Text("Enter 0 for zero marks, A for absent, or leave blank for pending.", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(roster, key = { it.studentId }) { student ->
                    OutlinedTextField(values[student.studentId].orEmpty(), { valuesJson = Gson().toJson(values + (student.studentId to it)) },
                        label = { Text(student.name) }, supportingText = { Text(student.studentId) }, modifier = Modifier.fillMaxWidth(), enabled = !locked && !saving, singleLine = true)
                }
            }
            if (!locked) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(enabled = !saving, onClick = { save(false) }) { Text(if (saving) "Saving…" else "Save draft") }
                Button(enabled = !saving && roster.isNotEmpty(), onClick = { confirm = true }) { Text("Review & publish") }
            }
        }
    }
}
