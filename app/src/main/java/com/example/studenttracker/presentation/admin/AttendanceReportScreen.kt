package com.example.studenttracker.presentation.admin

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.studenttracker.data.network.AttendanceReportItem
import com.example.studenttracker.data.network.NetworkModule
import com.example.studenttracker.data.network.Subject
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var subjectList by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var selectedSubject by remember { mutableStateOf<Subject?>(null) }
    var reportData by remember { mutableStateOf<List<AttendanceReportItem>?>(null) }
    var isLoadingSubjects by remember { mutableStateOf(true) }
    var isLoadingReport by remember { mutableStateOf(false) }
    
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val res = NetworkModule.api.getSubjects()
            if (res.status == "success" && res.subjects != null) {
                subjectList = res.subjects
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load subjects", Toast.LENGTH_SHORT).show()
        } finally {
            isLoadingSubjects = false
        }
    }

    LaunchedEffect(selectedSubject) {
        selectedSubject?.let { subject ->
            isLoadingReport = true
            try {
                val res = NetworkModule.api.getAttendanceReport(subjectCode = subject.subjectCode)
                if (res.status == "success") {
                    reportData = res.report ?: emptyList()
                } else {
                    Toast.makeText(context, res.message ?: "Error loading report", Toast.LENGTH_SHORT).show()
                    reportData = emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load report", Toast.LENGTH_SHORT).show()
                reportData = emptyList()
            } finally {
                isLoadingReport = false
            }
        }
    }

    fun exportToCsv() {
        val data = reportData
        if (data.isNullOrEmpty()) {
            Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = File(context.cacheDir, "Attendance_${selectedSubject?.subjectCode}.csv")
            val fos = FileOutputStream(file)
            fos.write("Student_ID,Name,Total_Present,Total_Classes,Percentage\n".toByteArray())
            data.forEach { item ->
                fos.write("${item.studentId},${item.name},${item.totalPresent},${item.totalClasses},${item.percentage}%\n".toByteArray())
            }
            fos.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Reports", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                actions = {
                    if (!reportData.isNullOrEmpty()) {
                        IconButton(onClick = { exportToCsv() }) {
                            Icon(Icons.Default.Download, contentDescription = "Export CSV", tint = Color(0xFF03DAC5))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        containerColor = Color(0xFF121212)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (isLoadingSubjects) {
                CircularProgressIndicator(color = Color(0xFFBB86FC), modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedSubject?.subjectName ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3700B3),
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        subjectList.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text("${subject.subjectCode} - ${subject.subjectName}") },
                                onClick = {
                                    selectedSubject = subject
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoadingReport) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFBB86FC))
                }
            } else if (reportData != null) {
                if (reportData!!.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No attendance records found for this subject.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(reportData!!) { item ->
                            AttendanceItemCard(item)
                        }
                    }
                }
            } else if (selectedSubject == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a subject to view attendance", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AttendanceItemCard(item: AttendanceReportItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(item.studentId, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                val percent = item.percentage.toDoubleOrNull() ?: 0.0
                val percentColor = if (percent >= 75.0) Color(0xFF03DAC5) else Color.Red
                Text("${item.percentage}%", color = percentColor, fontWeight = FontWeight.Bold)
                Text("${item.totalPresent}/${item.totalClasses}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
