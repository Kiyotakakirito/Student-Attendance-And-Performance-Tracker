package com.example.studenttracker.presentation.student

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
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.data.network.Student
import com.example.studenttracker.data.network.UpdateStudentRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    student: Student,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf(student.name) }
    var email by rememberSaveable { mutableStateOf(student.email) }
    var phone by rememberSaveable { mutableStateOf(student.phone) }
    var department by rememberSaveable { mutableStateOf(student.department) }
    var batch by rememberSaveable { mutableStateOf(student.batch) }
    var year by rememberSaveable { mutableStateOf(student.year) }
    var semester by rememberSaveable { mutableStateOf(student.semester) }
    var section by rememberSaveable { mutableStateOf(student.section) }
    var parentName by rememberSaveable { mutableStateOf(student.parentName) }
    var parentPhone by rememberSaveable { mutableStateOf(student.parentPhone) }
    var parentEmail by rememberSaveable { mutableStateOf(student.parentEmail) }

    var isLoading by remember { mutableStateOf(false) }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = name != student.name || email != student.email || phone != student.phone || department != student.department || batch != student.batch || year != student.year || semester != student.semester || section != student.section || parentName != student.parentName || parentPhone != student.parentPhone || parentEmail != student.parentEmail

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
                title = { Text("Edit Student", color = Color.White) },
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
            Text("Roll Number: ${student.rollNumber} (Cannot be changed)", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

            Text("Personal Info", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
            CustomTextField(value = name, onValueChange = { name = it }, label = "Full Name", enabled = !isLoading)
            CustomTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email, enabled = !isLoading)
            CustomTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardType = KeyboardType.Phone, enabled = !isLoading)

            Text("Academic Info", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
            CustomTextField(value = department, onValueChange = { department = it }, label = "Department (e.g., B.Tech CSE)", enabled = !isLoading)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomTextField(value = batch, onValueChange = { batch = it }, label = "Batch (e.g., 2023)", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, enabled = !isLoading)
                CustomTextField(value = year, onValueChange = { year = it }, label = "Year", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, enabled = !isLoading)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomTextField(value = semester, onValueChange = { semester = it }, label = "Semester", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number, enabled = !isLoading)
                CustomTextField(value = section, onValueChange = { section = it }, label = "Section", modifier = Modifier.weight(1f), enabled = !isLoading)
            }

            Text("Parent/Guardian Info", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
            CustomTextField(value = parentName, onValueChange = { parentName = it }, label = "Parent Name", enabled = !isLoading)
            CustomTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = "Parent Phone", keyboardType = KeyboardType.Phone, enabled = !isLoading)
            CustomTextField(value = parentEmail, onValueChange = { parentEmail = it }, label = "Parent Email", keyboardType = KeyboardType.Email, enabled = !isLoading)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || department.isBlank()) {
                        Toast.makeText(context, "Name, Email, and Department are required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val request = UpdateStudentRequest(
                                revision = student.revision,
                                rollNumber = student.rollNumber,
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                department = department.trim(),
                                batch = batch.trim(),
                                year = year.trim(),
                                semester = semester.trim(),
                                section = section.trim(),
                                parentName = parentName.trim(),
                                parentPhone = parentPhone.trim(),
                                parentEmail = parentEmail.trim()
                            )
                            val response = NetworkModule.api.updateStudent(request)
                            if (response.status == "success") {
                                Toast.makeText(context, "Student updated successfully", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Student", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
