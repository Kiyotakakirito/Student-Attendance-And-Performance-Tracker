package com.example.studenttracker.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.studenttracker.data.local.UserPreferences
import com.example.studenttracker.data.network.*
import com.example.studenttracker.presentation.components.*
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(navController: NavHostController, userPreferences: UserPreferences) {
    val scope = rememberCoroutineScope()
    val user by AuthSession.user.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    WorkspacePage("Administration", actions = {
        TextButton(onClick = { refresh++ }) { Text("Refresh") }
        TextButton(onClick = { scope.launch { userPreferences.clearSession() } }) { Text("Logout") }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Welcome, ${user?.name.orEmpty()}", style = MaterialTheme.typography.headlineSmall) }
            item { Text(user?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                RemoteContent(refresh, { NetworkModule.api.getDashboard().dashboard }) { counts ->
                    InfoCard("Institution overview", "Current active records") {
                        Text("${counts.students} students · ${counts.faculty} faculty")
                        Text("${counts.subjects} subjects · ${counts.departments} departments")
                    }
                }
            }
            item { InfoCard("Academic workspace", "Enroll students, mark class attendance and manage assessments.") {
                Button(onClick = { navController.navigate("academic") }, modifier = Modifier.fillMaxWidth()) { Text("Open academic workspace") }
            } }
            item { InfoCard("Students", "Manage profiles, academic details and guardian contacts.") {
                Button(onClick = { navController.navigate("student_list") }) { Text("Manage students") }
                OutlinedButton(onClick = { navController.navigate("add_student") }) { Text("Add student") }
            } }
            item { InfoCard("Faculty", "Manage faculty profiles and subject assignments.") {
                Button(onClick = { navController.navigate("faculty_list") }) { Text("Manage faculty") }
                OutlinedButton(onClick = { navController.navigate("add_faculty") }) { Text("Add faculty") }
            } }
            item { InfoCard("Subjects & reports") {
                Button(onClick = { navController.navigate("subject_list") }) { Text("Manage subjects") }
                OutlinedButton(onClick = { navController.navigate("attendance_report") }) { Text("Attendance reports & export") }
            } }
        }
    }
}
