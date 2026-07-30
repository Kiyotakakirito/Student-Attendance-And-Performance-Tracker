package com.example.studenttracker.presentation.faculty

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
import com.example.studenttracker.data.network.AddFacultyRequest
import com.example.studenttracker.data.network.NetworkModule
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFacultyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    var employeeId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf("") }

    var isError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun validateAndSave() {
        if (employeeId.isBlank() || name.isBlank() || email.isBlank() || department.isBlank() || designation.isBlank()) {
            isError = true
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        isSaving = true
        coroutineScope.launch {
            try {
                val request = AddFacultyRequest(
                    employeeId = employeeId.trim(),
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    department = department.trim(),
                    designation = designation.trim(),
                    joiningDate = joiningDate.trim()
                )
                val response = NetworkModule.api.addFaculty(request)
                if (response.status == "success") {
                    Toast.makeText(context, "Faculty added successfully!", Toast.LENGTH_SHORT).show()
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

    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = employeeId.isNotBlank() || name.isNotBlank() || email.isNotBlank()
    
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
                title = { Text("Add New Faculty", color = Color.White) },
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
            Text("Faculty Details", color = Color(0xFF03DAC5), style = MaterialTheme.typography.titleMedium)
            
            OutlinedTextField(
                value = employeeId, onValueChange = { employeeId = it },
                label = { Text("Employee ID *") },
                modifier = Modifier.fillMaxWidth(),
                isError = isError && employeeId.isBlank()
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
            Text("Professional Info", color = Color(0xFF03DAC5), style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = department, onValueChange = { department = it },
                    label = { Text("Department *") },
                    modifier = Modifier.weight(1f),
                    isError = isError && department.isBlank()
                )
                OutlinedTextField(
                    value = designation, onValueChange = { designation = it },
                    label = { Text("Designation *") },
                    modifier = Modifier.weight(1f),
                    isError = isError && designation.isBlank()
                )
            }

            OutlinedTextField(
                value = joiningDate, onValueChange = { joiningDate = it },
                label = { Text("Joining Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { validateAndSave() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5), disabledContainerColor = Color.DarkGray)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...", color = Color.Black)
                } else {
                    Text("Save Faculty", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
