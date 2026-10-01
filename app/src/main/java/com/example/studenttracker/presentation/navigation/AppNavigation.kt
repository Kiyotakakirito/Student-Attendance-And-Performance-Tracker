package com.example.studenttracker.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.studenttracker.data.local.UserPreferences
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.presentation.academic.*
import com.example.studenttracker.presentation.admin.*
import com.example.studenttracker.presentation.auth.LoginScreen
import com.example.studenttracker.presentation.components.RemoteContent
import com.example.studenttracker.presentation.faculty.*
import com.example.studenttracker.presentation.student.*
import com.example.studenttracker.presentation.subject.*
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController(), initialRole: String?, userPreferences: UserPreferences) {
    val scope = rememberCoroutineScope()
    val logout: () -> Unit = { scope.launch { userPreferences.clearSession() }; Unit }
    val back: () -> Unit = { navController.popBackStack(); Unit }
    val start = when (initialRole) { "admin" -> "admin_dashboard"; "faculty" -> "faculty_dashboard"; "student" -> "student_dashboard"; else -> "login" }
    val openAcademic: (String, String) -> Unit = { page, code -> navController.navigate("$page/${Uri.encode(code)}") }
    NavHost(navController, startDestination = start) {
        if (initialRole == null) {
            composable("login") { LoginScreen(userPreferences, onLoginSuccess = {}) }
        }
        if (initialRole == "student") {
            composable("student_dashboard") { StudentDashboardScreen(onLogout = logout) }
        }
        if (initialRole == "faculty") {
            composable("faculty_dashboard") { FacultyDashboardScreen(onOpen = openAcademic, onLogout = logout) }
        }
        if (initialRole == "faculty" || initialRole == "admin") {
            composable("academic") { AcademicHubScreen(isAdmin = initialRole == "admin", onOpen = openAcademic, onBack = back) }
            composable("report/{subjectCode}") { entry -> AttendanceReportScreen(back, entry.arguments?.getString("subjectCode")) }
            composable("attendance/{subjectCode}") { entry -> AttendanceScreen(entry.arguments?.getString("subjectCode").orEmpty(), back) }
            composable("assessments/{subjectCode}") { entry -> AssessmentsScreen(entry.arguments?.getString("subjectCode").orEmpty(), back) }
        }
        if (initialRole == "admin") {
            composable("admin_dashboard") { AdminDashboardScreen(navController, userPreferences) }
            composable("attendance_report") { AttendanceReportScreen(back) }
            composable("enrollment/{subjectCode}") { entry -> EnrollmentScreen(entry.arguments?.getString("subjectCode").orEmpty(), back) }
            composable("student_list") {
                StudentListScreen(onNavigateBack = back,
                    onStudentClick = { id -> navController.navigate("student_details/${Uri.encode(id)}") },
                    onNavigateToAddStudent = { navController.navigate("add_student") })
            }
            composable("add_student") { AddStudentScreen(back) }
            composable("student_details/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RemoteContent(key = id, load = { NetworkModule.api.getStudent(id).student }, onBack = back) { record ->
                    StudentDetailsScreen(student = record, onNavigateBack = back,
                        onNavigateToEdit = { navController.navigate("edit_student/${Uri.encode(id)}") })
                }
            }
            composable("edit_student/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RemoteContent(key = id, load = { NetworkModule.api.getStudent(id).student }, onBack = back) { record ->
                    EditStudentScreen(student = record, onNavigateBack = back)
                }
            }
            composable("faculty_list") {
                FacultyListScreen(onNavigateBack = back,
                    onFacultyClick = { id -> navController.navigate("faculty_details/${Uri.encode(id)}") },
                    onNavigateToAddFaculty = { navController.navigate("add_faculty") })
            }
            composable("add_faculty") { AddFacultyScreen(back) }
            composable("faculty_details/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RemoteContent(key = id, load = { NetworkModule.api.getFacultyMember(id).facultyMember }, onBack = back) { record ->
                    FacultyDetailsScreen(faculty = record, onNavigateBack = back,
                        onNavigateToEdit = { navController.navigate("edit_faculty/${Uri.encode(id)}") })
                }
            }
            composable("edit_faculty/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RemoteContent(key = id, load = { NetworkModule.api.getFacultyMember(id).facultyMember }, onBack = back) { record ->
                    EditFacultyScreen(faculty = record, onNavigateBack = back)
                }
            }
            composable("subject_list") {
                SubjectListScreen(onNavigateBack = back,
                    onSubjectClick = { id -> navController.navigate("subject_details/${Uri.encode(id)}") },
                    onNavigateToAddSubject = { navController.navigate("add_subject") })
            }
            composable("add_subject") { AddSubjectScreen(back) }
            composable("subject_details/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RemoteContent(key = id, load = { NetworkModule.api.getSubject(id).subject }, onBack = back) { record ->
                    SubjectDetailsScreen(subject = record, onNavigateBack = back,
                        onNavigateToEdit = { navController.navigate("edit_subject/${Uri.encode(id)}") })
                }
            }
            composable("edit_subject/{id}") { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                RemoteContent(key = id, load = { NetworkModule.api.getSubject(id).subject }, onBack = back) { record ->
                    EditSubjectScreen(subject = record, onNavigateBack = back)
                }
            }
        }
    }
}
