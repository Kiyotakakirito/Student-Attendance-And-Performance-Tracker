package com.example.studenttracker.presentation.student

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Form State
    var rollNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var parentEmail by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var semester by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }

    var isError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    fun validateAndSave() {
        val cleanRoll = rollNumber.trim()
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanDept = department.trim()

        if (cleanRoll.isBlank() || cleanName.isBlank() || cleanEmail.isBlank() || cleanDept.isBlank()) {
            isError = true
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            Toast.makeText(context, "Invalid Email Format (e.g., student@college.edu)", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSaving = true
        coroutineScope.launch {
            try {
                val api = com.example.studenttracker.data.network.NetworkModule.api
                val request = com.example.studenttracker.data.network.AddStudentRequest(
                    rollNumber = cleanRoll,
                    name = cleanName,
                    email = cleanEmail,
                    phone = phone.trim(),
                    department = cleanDept,
                    batch = batch.trim(),
                    year = year.trim(),
                    semester = semester.trim(),
                    section = section.trim(),
                    parentName = parentName.trim(),
                    parentPhone = parentPhone.trim(),
                    parentEmail = parentEmail.trim()
                )
                
                val response = api.addStudent(request)
                if (response.status == "success") {
                    Toast.makeText(context, "Student saved successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                } else {
                    Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSaving = false
            }
        }
    }

    fun resetForm() {
        rollNumber = ""
        name = ""
        email = ""
        phone = ""
        parentName = ""
        parentPhone = ""
        parentEmail = ""
        department = ""
        year = ""
        semester = ""
        section = ""
        batch = ""
        isError = false
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = rollNumber.isNotBlank() || name.isNotBlank() || email.isNotBlank()
    
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
                title = { Text("Add New Student", color = Color.White) },
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
            Text("Student Details", color = Color(0xFFBB86FC), style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = rollNumber, onValueChange = { rollNumber = it },
                label = { Text("Roll Number *") },
                modifier = Modifier.fillMaxWidth(),
                isError = isError && rollNumber.isBlank()
            )
            
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
                isError = isError && name.isBlank()
            )
            
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                isError = isError && email.isBlank()
            )
            
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
            Text("Academic Info", color = Color(0xFFBB86FC), style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = department, onValueChange = { department = it },
                    label = { Text("Department *") },
                    modifier = Modifier.weight(1f),
                    isError = isError && department.isBlank()
                )
                OutlinedTextField(
                    value = batch, onValueChange = { batch = it },
                    label = { Text("Batch") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = year, onValueChange = { year = it },
                    label = { Text("Year") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = semester, onValueChange = { semester = it },
                    label = { Text("Semester") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = section, onValueChange = { section = it },
                    label = { Text("Section") },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.DarkGray)
            Text("Parent/Guardian Info", color = Color(0xFFBB86FC), style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = parentName, onValueChange = { parentName = it },
                label = { Text("Parent Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = parentPhone, onValueChange = { parentPhone = it },
                label = { Text("Parent Phone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = parentEmail, onValueChange = { parentEmail = it },
                label = { Text("Parent Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { resetForm() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Reset")
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = { validateAndSave() },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC), disabledContainerColor = Color.DarkGray)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", color = Color.White)
                        } else {
                            Text("Save Student", color = Color.Black)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
