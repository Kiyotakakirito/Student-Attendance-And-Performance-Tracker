package com.example.studenttracker.presentation.faculty

import androidx.compose.runtime.Composable
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.presentation.components.DirectoryRecord
import com.example.studenttracker.presentation.components.DirectoryScreen

@Composable
fun FacultyListScreen(onNavigateBack: () -> Unit, onFacultyClick: (String) -> Unit, onNavigateToAddFaculty: () -> Unit) {
    DirectoryScreen("Faculty", onNavigateBack, onNavigateToAddFaculty, onFacultyClick) {
        NetworkModule.api.getFaculty().faculty.orEmpty().map { DirectoryRecord(it.employeeId, it.name, "${it.department} · ${it.designation}") }
    }
}
