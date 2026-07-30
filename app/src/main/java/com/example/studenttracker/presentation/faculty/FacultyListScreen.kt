package com.example.studenttracker.presentation.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studenttracker.data.network.Faculty
import com.example.studenttracker.data.network.FacultyCache
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.presentation.components.SkeletonLoadingCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddFaculty: () -> Unit,
    onFacultyClick: () -> Unit
) {
    var facultyList by remember { mutableStateOf<List<Faculty>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val response = NetworkModule.api.getFaculty()
            if (response.status == "success" && response.faculty != null) {
                facultyList = response.faculty
            } else {
                error = response.message ?: "Unknown error occurred"
            }
        } catch (e: Exception) {
            error = e.message ?: "Failed to connect"
        } finally {
            isLoading = false
        }
    }

    val filteredFaculty = facultyList.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.employeeId.contains(searchQuery, ignoreCase = true) ||
        it.department.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Faculty Directory", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddFaculty,
                containerColor = Color(0xFFBB86FC)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Faculty", tint = Color.Black)
            }
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by Name, ID, or Dept") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFBB86FC),
                    unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) {
                            SkeletonLoadingCard(height = 80.dp)
                        }
                    }
                } else if (error != null) {
                    Text(
                        text = "Failed to load faculty: $error",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredFaculty.isEmpty()) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matches found." else "No faculty found. Add one!",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFaculty) { faculty ->
                            FacultyCard(faculty = faculty) {
                                FacultyCache.selectedFaculty = faculty
                                onFacultyClick()
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) } // padding for FAB
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyCard(faculty: Faculty, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF03DAC5).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF03DAC5))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = faculty.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = "${faculty.designation} • ${faculty.department}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
