package com.example.studenttracker.presentation.subject

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.AddSubjectRequest
import com.example.studenttracker.data.network.NetworkModule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var subjectCode by rememberSaveable { mutableStateOf("") }
    var subjectName by rememberSaveable { mutableStateOf("") }
    var facultyId by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var totalClasses by rememberSaveable { mutableStateOf("") }

    var isError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun validateAndSave() {
        if (subjectCode.isBlank() || subjectName.isBlank() || facultyId.isBlank() || semester.isBlank() || totalClasses.isBlank()) {
            isError = true
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        coroutineScope.launch {
            try {
                val request = AddSubjectRequest(
                    subjectCode = subjectCode.trim(),
                    subjectName = subjectName.trim(),
                    facultyId = facultyId.trim(),
                    semester = semester.trim(),
                    totalClasses = totalClasses.trim()
                )
                val response = NetworkModule.api.addSubject(request)
                if (response.status == "success") {
                    Toast.makeText(context, "Subject added successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = subjectCode.isNotBlank() || subjectName.isNotBlank() || facultyId.isNotBlank() || semester.isNotBlank() || totalClasses.isNotBlank()

    androidx.activity.compose.BackHandler(enabled = hasUnsavedChanges || isSaving) {
        if (!isSaving) showUnsavedDialog = true
    }

    val handleBackPress = {
        if (isSaving) {

        } else if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Discard Changes?", color = Color.White) },
            text = { Text("You have unsaved changes. Are you sure you want to go back?", color = Color.LightGray) },
            containerColor = Color(0xFF1E1E1E),
            confirmButton = {
                TextButton(onClick = { onNavigateBack() }) { Text("Discard", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Subject", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Subject Details", color = Color(0xFF3700B3), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                enabled = !isSaving,
                value = subjectCode, onValueChange = { subjectCode = it },
                label = { Text("Subject Code (e.g., CS101) *") },
                modifier = Modifier.fillMaxWidth(),
                isError = isError && subjectCode.isBlank()
            )

            OutlinedTextField(
                enabled = !isSaving,
                value = subjectName, onValueChange = { subjectName = it },
                label = { Text("Subject Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = isError && subjectName.isBlank()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
            Text("Assignment & Curriculum", color = Color(0xFF3700B3), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                enabled = !isSaving,
                value = facultyId, onValueChange = { facultyId = it },
                label = { Text("Assigned Faculty ID *") },
                modifier = Modifier.fillMaxWidth(),
                isError = isError && facultyId.isBlank()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                enabled = !isSaving,
                    value = semester, onValueChange = { semester = it },
                    label = { Text("Semester *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = isError && semester.isBlank()
                )
                OutlinedTextField(
                enabled = !isSaving,
                    value = totalClasses, onValueChange = { totalClasses = it },
                    label = { Text("Total Classes *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    isError = isError && totalClasses.isBlank()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { validateAndSave() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3700B3), disabledContainerColor = Color.DarkGray)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...", color = Color.White)
                } else {
                    Text("Save Subject", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
