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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EnrollmentScreen(subjectCode: String, onBack: () -> Unit) {
    RemoteContent(subjectCode, { NetworkModule.api.getStudents().students.orEmpty() to NetworkModule.api.getRoster(subjectCode) }, onBack = onBack) { (students, roster) ->
        var selected by rememberSaveable(subjectCode) { mutableStateOf<List<String>>(roster.students.map { it.studentId }) }
        var query by rememberSaveable { mutableStateOf("") }
        var saving by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val back = guardedBack(selected.toSet() != roster.students.map { it.studentId }.toSet(), saving, onBack)
        WorkspacePage("Enrollment · $subjectCode", back) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${selected.size} students selected · maximum 250")
                Text("Only enrolled students appear in attendance and marks. Earlier submitted records are retained when enrollment changes.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(query, { query = it }, label = { Text("Search name, roll number or department") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.weight(1f)) {
                    items(students.filter { "${it.name} ${it.rollNumber} ${it.department}".contains(query, true) }, key = { it.rollNumber }) { student ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(student.rollNumber in selected, enabled = !saving, onCheckedChange = { checked ->
                                selected = if (checked) selected + student.rollNumber else selected - student.rollNumber
                            })
                            Column(Modifier.padding(vertical = 8.dp)) {
                                Text(student.name)
                                Text("${student.rollNumber} · ${student.department} · ${student.section}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Button(enabled = !saving && selected.size <= 250, modifier = Modifier.fillMaxWidth(), onClick = {
                    saving = true; error = null
                    scope.launch {
                        try { NetworkModule.api.saveEnrollment(EnrollmentRequest(subjectCode, selected, roster.revision)); onBack() }
                        catch (e: CancellationException) { throw e }
                        catch (e: Exception) { error = e.message }
                        finally { saving = false }
                    }
                }) { Text(if (saving) "Saving…" else "Save enrollment") }
            }
        }
    }
}
