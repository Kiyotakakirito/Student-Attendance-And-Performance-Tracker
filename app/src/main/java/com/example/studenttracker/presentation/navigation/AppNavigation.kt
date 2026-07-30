package com.example.studenttracker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studenttracker.presentation.admin.AdminDashboardScreen
import com.example.studenttracker.presentation.auth.LoginScreen
import com.example.studenttracker.presentation.faculty.FacultyDashboardScreen
import com.example.studenttracker.presentation.student.StudentDashboardScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object AdminDashboard : Screen("admin_dashboard")
    object FacultyDashboard : Screen("faculty_dashboard")
    object StudentDashboard : Screen("student_dashboard")
    object AddStudent : Screen("add_student")
    object StudentList : Screen("student_list")
    object StudentDetails : Screen("student_details")
    object EditStudent : Screen("edit_student")
    
    // Faculty Management
    object FacultyList : Screen("faculty_list")
    object AddFaculty : Screen("add_faculty")
    object FacultyDetails : Screen("faculty_details")
    object EditFaculty : Screen("edit_faculty")
    
    // Subject Management
    object SubjectList : Screen("subject_list")
    object AddSubject : Screen("add_subject")
    object SubjectDetails : Screen("subject_details")
    object EditSubject : Screen("edit_subject")
    
    // Reports
    object AttendanceReport : Screen("attendance_report")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    initialRole: String?,
    userPreferences: com.example.studenttracker.data.local.UserPreferences
) {
    val startDest = when (initialRole?.lowercase()) {
        "admin" -> Screen.AdminDashboard.route
        "faculty" -> Screen.FacultyDashboard.route
        "student" -> Screen.StudentDashboard.route
        else -> Screen.Login.route
    }

    NavHost(navController = navController, startDestination = startDest) {
        composable(Screen.Login.route) {
            LoginScreen(
                userPreferences = userPreferences,
                onLoginSuccess = { role ->
                    val destination = when (role.lowercase()) {
                        "admin" -> Screen.AdminDashboard.route
                        "faculty" -> Screen.FacultyDashboard.route
                        "student" -> Screen.StudentDashboard.route
                        else -> null
                    }
                    
                    if (destination != null) {
                        navController.navigate(destination) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                navController = navController,
                userPreferences = userPreferences
            )
        }
        
        composable(Screen.FacultyDashboard.route) {
            FacultyDashboardScreen()
        }
        
        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen()
        }
        
        composable(Screen.AddStudent.route) {
            com.example.studenttracker.presentation.student.AddStudentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.StudentList.route) {
            com.example.studenttracker.presentation.student.StudentListScreen(
                onNavigateBack = { navController.popBackStack() },
                onStudentClick = { navController.navigate("student_details") }
            )
        }
        
        composable(Screen.StudentDetails.route) {
            com.example.studenttracker.presentation.student.StudentDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate("edit_student") }
            )
        }
        
        composable(Screen.EditStudent.route) {
            com.example.studenttracker.presentation.student.EditStudentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.FacultyList.route) {
            com.example.studenttracker.presentation.faculty.FacultyListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddFaculty = { navController.navigate("add_faculty") },
                onFacultyClick = { navController.navigate("faculty_details") }
            )
        }
        
        composable(Screen.AddFaculty.route) {
            com.example.studenttracker.presentation.faculty.AddFacultyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.FacultyDetails.route) {
            com.example.studenttracker.presentation.faculty.FacultyDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate("edit_faculty") }
            )
        }
        
        composable(Screen.EditFaculty.route) {
            com.example.studenttracker.presentation.faculty.EditFacultyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SubjectList.route) {
            com.example.studenttracker.presentation.subject.SubjectListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddSubject = { navController.navigate("add_subject") },
                onSubjectClick = { navController.navigate("subject_details") }
            )
        }
        
        composable(Screen.AddSubject.route) {
            com.example.studenttracker.presentation.subject.AddSubjectScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.SubjectDetails.route) {
            com.example.studenttracker.presentation.subject.SubjectDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate("edit_subject") }
            )
        }
        
        composable(Screen.EditSubject.route) {
            com.example.studenttracker.presentation.subject.EditSubjectScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AttendanceReport.route) {
            com.example.studenttracker.presentation.admin.AttendanceReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
