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
import com.example.studenttracker.data.network.Subject
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.data.network.UpdateSubjectRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubjectScreen(
    subject: Subject,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var subjectName by rememberSaveable { mutableStateOf(subject.subjectName) }
    var facultyId by rememberSaveable { mutableStateOf(subject.facultyId) }
    var semester by rememberSaveable { mutableStateOf(subject.semester) }
    var totalClasses by rememberSaveable { mutableStateOf(subject.totalClasses) }

    var isLoading by remember { mutableStateOf(false) }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = subjectName != subject.subjectName || facultyId != subject.facultyId || semester != subject.semester || totalClasses != subject.totalClasses

    androidx.activity.compose.BackHandler(enabled = hasUnsavedChanges || isLoading) {
        if (!isLoading) showUnsavedDialog = true
    }

    val handleBackPress = {
        if (isLoading) {

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
                title = { Text("Edit Subject", color = Color.White) },
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
            Text("Subject Code: ${subject.subjectCode} (Cannot be changed)", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

            CustomTextField(value = subjectName, onValueChange = { subjectName = it }, label = "Subject Name", enabled = !isLoading)
            CustomTextField(value = facultyId, onValueChange = { facultyId = it }, label = "Assigned Faculty ID", enabled = !isLoading)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomTextField(value = semester, onValueChange = { semester = it }, label = "Semester", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, enabled = !isLoading)
                CustomTextField(value = totalClasses, onValueChange = { totalClasses = it }, label = "Total Classes", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, enabled = !isLoading)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (subjectName.isBlank() || facultyId.isBlank() || semester.isBlank() || totalClasses.isBlank()) {
                        Toast.makeText(context, "All required fields must be filled", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val request = UpdateSubjectRequest(
                                revision = subject.revision,
                                subjectCode = subject.subjectCode,
                                subjectName = subjectName.trim(),
                                facultyId = facultyId.trim(),
                                semester = semester.trim(),
                                totalClasses = totalClasses.trim()
                            )
                            val response = NetworkModule.api.updateSubject(request)
                            if (response.status == "success") {
                                Toast.makeText(context, "Subject updated successfully", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3700B3)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Subject", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}
