package com.example.studenttracker.presentation.subject

import androidx.compose.runtime.Composable
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.presentation.components.DirectoryRecord
import com.example.studenttracker.presentation.components.DirectoryScreen

@Composable
fun SubjectListScreen(onNavigateBack: () -> Unit, onSubjectClick: (String) -> Unit, onNavigateToAddSubject: () -> Unit) {
    DirectoryScreen("Subjects", onNavigateBack, onNavigateToAddSubject, onSubjectClick) {
        NetworkModule.api.getSubjects().subjects.orEmpty().map { DirectoryRecord(it.subjectCode, it.subjectName, "Semester ${it.semester} · Faculty ${it.facultyId}") }
    }
}
