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
import com.example.studenttracker.data.network.FacultyCache
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.data.network.UpdateFacultyRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFacultyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val faculty = FacultyCache.selectedFaculty
    
    if (faculty == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    var name by remember { mutableStateOf(faculty.name) }
    var email by remember { mutableStateOf(faculty.email) }
    var phone by remember { mutableStateOf(faculty.phone) }
    var department by remember { mutableStateOf(faculty.department) }
    var designation by remember { mutableStateOf(faculty.designation) }
    var joiningDate by remember { mutableStateOf(faculty.joiningDate) }

    var isLoading by remember { mutableStateOf(false) }

    var showUnsavedDialog by remember { mutableStateOf(false) }
    val hasUnsavedChanges = name != faculty.name || email != faculty.email || phone != faculty.phone || department != faculty.department || designation != faculty.designation
    
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
                title = { Text("Edit Faculty", color = Color.White) },
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
            Text("Employee ID: ${faculty.employeeId} (Cannot be changed)", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)

            Text("Professional Info", color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold)
            CustomTextField(value = name, onValueChange = { name = it }, label = "Full Name")
            CustomTextField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
            CustomTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number", keyboardType = KeyboardType.Phone)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomTextField(value = department, onValueChange = { department = it }, label = "Department", modifier = Modifier.weight(1f))
                CustomTextField(value = designation, onValueChange = { designation = it }, label = "Designation", modifier = Modifier.weight(1f))
            }
            
            CustomTextField(value = joiningDate, onValueChange = { joiningDate = it }, label = "Joining Date")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || department.isBlank() || designation.isBlank()) {
                        Toast.makeText(context, "All required fields must be filled", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val request = UpdateFacultyRequest(
                                employeeId = faculty.employeeId,
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                department = department.trim(),
                                designation = designation.trim(),
                                joiningDate = joiningDate.trim()
                            )
                            val response = NetworkModule.api.updateFaculty(request)
                            if (response.status == "success") {
                                FacultyCache.selectedFaculty = com.example.studenttracker.data.network.Faculty(
                                    employeeId = faculty.employeeId,
                                    name = name.trim(),
                                    email = email.trim(),
                                    phone = phone.trim(),
                                    department = department.trim(),
                                    designation = designation.trim(),
                                    joiningDate = joiningDate.trim()
                                )
                                Toast.makeText(context, "Faculty updated successfully", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Update Faculty", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
