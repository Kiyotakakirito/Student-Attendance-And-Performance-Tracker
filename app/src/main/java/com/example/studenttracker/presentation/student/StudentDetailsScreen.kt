package com.example.studenttracker.presentation.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsScreen(
    student: Student,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Deactivate Student") },
            text = { Text("Are you sure you want to delete ${student.name} (${student.rollNumber})? The profile will be deactivated. Historical records will be retained.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val api = com.example.studenttracker.data.network.NetworkModule.api
                                val response = api.deleteStudent(com.example.studenttracker.data.network.DeleteStudentRequest(revision = student.revision, rollNumber = student.rollNumber))
                                if (response.status == "success") {
                                    Toast.makeText(context, "Student deactivated successfully", Toast.LENGTH_SHORT).show()
                                    showDeleteDialog = false
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to delete: ${e.toString()}", Toast.LENGTH_LONG).show()
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Deactivate", color = Color.Red)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit, enabled = !isDeleting) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }, enabled = !isDeleting) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCF6679))
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFBB86FC).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(60.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(student.name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(student.rollNumber, color = Color.Gray, style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(24.dp))

            SectionCard(title = "Academic Information") {
                DetailRow("Department", student.department)
                DetailRow("Batch", student.batch)
                DetailRow("Year", student.year)
                DetailRow("Semester", student.semester)
                DetailRow("Section", student.section)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "Contact Information") {
                DetailIconRow(Icons.Default.Email, "Email", student.email)
                DetailIconRow(Icons.Default.Phone, "Phone", student.phone)
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "Parent Information") {
                DetailRow("Name", student.parentName)
                DetailIconRow(Icons.Default.Phone, "Phone", student.parentPhone)
                DetailIconRow(Icons.Default.Email, "Email", student.parentEmail)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFF03DAC5), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(if (value.isBlank()) "N/A" else value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DetailIconRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(if (value.isBlank()) "N/A" else value, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}
