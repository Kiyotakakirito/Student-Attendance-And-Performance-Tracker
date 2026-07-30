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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.data.network.StudentCache
import com.example.studenttracker.data.network.UpdateStudentRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val student = StudentCache.selectedStudent
    
    if (student == null) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    var name by remember { mutableStateOf(student.name) }
    var email by remember { mutableStateOf(student.email) }
    var phone by remember { mutableStateOf(student.phone) }
    var department by remember { mutableStateOf(student.department) }
    var batch by remember { mutableStateOf(student.batch) }
    var year by remember { mutableStateOf(student.year) }
    var semester by remember { mutableStateOf(student.semester) }
    var section by remember { mutableStateOf(student.section) }
    var parentName by remember { mutableStateOf(student.parentName) }
    var parentPhone by remember { mutableStateOf(student.parentPhone) }
    var parentEmail by remember { mutableStateOf(student.parentEmail) }

    var isLoading by remember { mutableStateOf(false) }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = name != student.name || email != student.email || phone != student.phone || department != student.department || semester != student.semester
    
    androidx.activity.compose.BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedDialog = true
    }
    
    val handleBackPress = {
        if (hasUnsavedChanges) {
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
            CustomTextField(value = name, onValueChange = { name = it }, label = "Full Name")
            CustomTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
            CustomTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardType = KeyboardType.Phone)

            Text("Academic Info", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
            CustomTextField(value = department, onValueChange = { department = it }, label = "Department (e.g., B.Tech CSE)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomTextField(value = batch, onValueChange = { batch = it }, label = "Batch (e.g., 2023)", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                CustomTextField(value = year, onValueChange = { year = it }, label = "Year", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomTextField(value = semester, onValueChange = { semester = it }, label = "Semester", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                CustomTextField(value = section, onValueChange = { section = it }, label = "Section", modifier = Modifier.weight(1f))
            }

            Text("Parent/Guardian Info", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
            CustomTextField(value = parentName, onValueChange = { parentName = it }, label = "Parent Name")
            CustomTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = "Parent Phone", keyboardType = KeyboardType.Phone)
            CustomTextField(value = parentEmail, onValueChange = { parentEmail = it }, label = "Parent Email", keyboardType = KeyboardType.Email)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || department.isBlank() || year.isBlank()) {
                        Toast.makeText(context, "Name, Department, and Year are required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val request = UpdateStudentRequest(
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
                                // Update Cache
                                StudentCache.selectedStudent = com.example.studenttracker.data.network.Student(
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
                                Toast.makeText(context, "Student updated successfully", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } else {
                                Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_LONG).show()
                            }
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}
