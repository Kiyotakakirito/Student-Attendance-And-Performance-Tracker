package com.example.studenttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.studenttracker.data.local.UserPreferences
import com.example.studenttracker.data.network.AuthSession
import com.example.studenttracker.presentation.navigation.AppNavigation
import com.example.studenttracker.ui.theme.StudentTrackerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudentTrackerTheme(darkTheme = true, dynamicColor = false) {
                val user by AuthSession.user.collectAsState()
                val preferences = remember { UserPreferences(this@MainActivity) }
                LaunchedEffect(user?.expiresAt) {
                    user?.let {
                        delay(((it.expiresAt * 1000) - System.currentTimeMillis()).coerceAtLeast(0))
                        AuthSession.clear()
                        preferences.clearSession()
                    }
                }

                key(user?.email, user?.role) {
                    AppNavigation(initialRole = user?.role, userPreferences = preferences)
                }
            }
        }
    }
}
