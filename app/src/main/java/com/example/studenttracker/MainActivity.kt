package com.example.studenttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.studenttracker.ui.theme.StudentTrackerTheme

import com.example.studenttracker.presentation.auth.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudentTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val userPreferences = com.example.studenttracker.data.local.UserPreferences(this@MainActivity)
                        val initialRole by userPreferences.userRoleFlow.collectAsState(initial = "LOADING")

                        if (initialRole != "LOADING") {
                            com.example.studenttracker.presentation.navigation.AppNavigation(
                                initialRole = initialRole,
                                userPreferences = userPreferences
                            )
                        }
                    }
                }
            }
        }
    }
}