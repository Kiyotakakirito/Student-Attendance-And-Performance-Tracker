package com.example.studenttracker.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: androidx.navigation.NavHostController,
    userPreferences: com.example.studenttracker.data.local.UserPreferences
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf("Dashboard") }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.activity.compose.BackHandler(enabled = drawerState.isClosed) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App", color = Color.White) },
            text = { Text("Are you sure you want to exit the application?", color = Color.LightGray) },
            containerColor = Color(0xFF1E1E1E),
            confirmButton = {
                TextButton(onClick = { (context as? android.app.Activity)?.finish() }) {
                    Text("Exit", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E1E1E),
                modifier = Modifier.width(300.dp)
            ) {
                DrawerHeader()
                HorizontalDivider(color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    val items = listOf(
                        "Dashboard" to Icons.Default.Dashboard,
                        "Students" to Icons.Default.School,
                        "Faculty" to Icons.Default.Group,
                        "Subjects" to Icons.Default.Book,
                        "Attendance Reports" to Icons.Default.Assessment,
                        "Marks Reports" to Icons.Default.Grade,
                        "Notifications" to Icons.Default.Notifications,
                        "Messages" to Icons.Default.Message,
                        "Settings" to Icons.Default.Settings,
                        "Help" to Icons.Default.Help,
                        "About" to Icons.Default.Info,
                        "Logout" to Icons.Default.ExitToApp
                    )

                    items.forEach { (title, icon) ->
                        NavigationDrawerItem(
                            icon = { Icon(icon, contentDescription = title, tint = if (selectedItem == title) Color(0xFFBB86FC) else Color.LightGray) },
                            label = { 
                                Text(
                                    title, 
                                    color = if (selectedItem == title) Color(0xFFBB86FC) else Color.White,
                                    fontWeight = if (selectedItem == title) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            selected = title == selectedItem,
                            onClick = {
                                selectedItem = title
                                scope.launch { drawerState.close() }
                                
                                if (title == "Students") {
                                    scope.launch {
                                        navController.navigate("student_list")
                                    }
                                }
                                if (title == "Faculty") {
                                    scope.launch {
                                        navController.navigate("faculty_list")
                                    }
                                }
                                if (title == "Subjects") {
                                    scope.launch {
                                        navController.navigate("subject_list")
                                    }
                                }
                                if (title == "Attendance Reports") {
                                    scope.launch {
                                        navController.navigate("attendance_report")
                                    }
                                }
                                if (title == "Logout") {
                                    scope.launch {
                                        userPreferences.clearSession()
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true } // Clear entire back stack
                                        }
                                    }
                                }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFFBB86FC).copy(alpha = 0.1f),
                                unselectedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                )
            },
            containerColor = Color(0xFF121212)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardHeader()
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Overview", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(200.dp) // Fixed height since it's inside a scrollable column
                ) {
                    item { StatCard("Total Students", "1,245", Color(0xFFBB86FC)) { navController.navigate("student_list") } }
                    item { StatCard("Total Faculty", "84", Color(0xFF03DAC5)) { navController.navigate("faculty_list") } }
                    item { StatCard("Subjects", "42", Color(0xFF3700B3)) { navController.navigate("subject_list") } }
                    item { StatCard("Departments", "6", Color(0xFFCF6679)) {} }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Action Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    item { DashboardActionCard("Add Student", Icons.Default.PersonAdd, Color(0xFF03DAC5)) { navController.navigate("add_student") } }
                    item { DashboardActionCard("Manage Students", Icons.Default.School, Color(0xFFBB86FC)) { navController.navigate("student_list") } }
                    item { DashboardActionCard("Add Faculty", Icons.Default.GroupAdd, Color(0xFFCF6679)) { navController.navigate("add_faculty") } }
                    item { DashboardActionCard("Manage Faculty", Icons.Default.Group, Color(0xFF3700B3)) { navController.navigate("faculty_list") } }
                }
            }
        }
    }
}

@Composable
fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFBB86FC)),
            contentAlignment = Alignment.Center
        ) {
            Text("👑", fontSize = 40.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Admin Name", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("admin@college.edu", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Badge(containerColor = Color(0xFF03DAC5)) {
            Text("Administrator", color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}

@Composable
fun DashboardHeader() {
    val currentDate = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFBB86FC)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎓", fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Welcome back, Admin!", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(currentDate, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF03DAC5))
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, accentColor: Color, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().height(90.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = accentColor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardActionCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
