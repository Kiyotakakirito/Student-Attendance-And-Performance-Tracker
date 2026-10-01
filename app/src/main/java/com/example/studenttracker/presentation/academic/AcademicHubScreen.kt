package com.example.studenttracker.presentation.academic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.*
import com.example.studenttracker.presentation.components.*

@Composable
fun AcademicHubScreen(isAdmin: Boolean, onOpen: (String, String) -> Unit, onBack: (() -> Unit)? = null, onLogout: () -> Unit = {}) {
    val user by AuthSession.user.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    WorkspacePage(if (isAdmin) "Teaching & enrollment" else "My teaching", onBack, actions = {
        TextButton(onClick = { refresh++ }) { Text("Refresh") }
        if (!isAdmin) TextButton(onClick = onLogout) { Text("Logout") }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            RemoteContent(refresh, { NetworkModule.api.getSubjects().subjects.orEmpty() }) { subjects ->
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text(if (isAdmin) "Manage enrollment and academic records by subject." else "Welcome, ${user?.name.orEmpty()}. Choose an assigned subject.") }
                    if (subjects.isEmpty()) item { InfoCard("No subjects yet", if (isAdmin) "Create subjects and assign faculty to get started." else "Your administrator needs to assign you to a subject.") }
                    items(subjects, key = { it.subjectCode }) { subject ->
                        InfoCard(subject.subjectName, "${subject.subjectCode} · Semester ${subject.semester}") {
                            if (isAdmin) OutlinedButton(onClick = { onOpen("enrollment", subject.subjectCode) }, modifier = Modifier.fillMaxWidth()) { Text("Manage enrolled students") }
                            Button(onClick = { onOpen("attendance", subject.subjectCode) }, modifier = Modifier.fillMaxWidth()) { Text("Mark attendance") }
                            OutlinedButton(onClick = { onOpen("report", subject.subjectCode) }, modifier = Modifier.fillMaxWidth()) { Text("Attendance report") }
                            OutlinedButton(onClick = { onOpen("assessments", subject.subjectCode) }, modifier = Modifier.fillMaxWidth()) { Text("Assessments & marks") }
                        }
                    }
                }
            }
        }
    }
}
