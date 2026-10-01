package com.example.studenttracker.presentation.faculty

import androidx.compose.runtime.Composable
import com.example.studenttracker.presentation.academic.AcademicHubScreen

@Composable
fun FacultyDashboardScreen(onOpen: (String, String) -> Unit, onLogout: () -> Unit) {
    AcademicHubScreen(isAdmin = false, onOpen = onOpen, onLogout = onLogout)
}
