package com.example.studenttracker.presentation.student

import androidx.compose.runtime.Composable
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.presentation.components.DirectoryRecord
import com.example.studenttracker.presentation.components.DirectoryScreen

@Composable
fun StudentListScreen(onNavigateBack: () -> Unit, onStudentClick: (String) -> Unit, onNavigateToAddStudent: () -> Unit) {
    DirectoryScreen("Students", onNavigateBack, onNavigateToAddStudent, onStudentClick) {
        NetworkModule.api.getStudents().students.orEmpty().map { DirectoryRecord(it.rollNumber, it.name, "${it.department} · Semester ${it.semester} · ${it.section}") }
    }
}
