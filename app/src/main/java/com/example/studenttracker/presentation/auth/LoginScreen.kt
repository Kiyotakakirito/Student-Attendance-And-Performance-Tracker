package com.example.studenttracker.presentation.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

import com.example.studenttracker.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun LoginScreen(
    userPreferences: com.example.studenttracker.data.local.UserPreferences,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)
    
    var isLoading by remember { mutableStateOf(false) }

    // Using the Web Client ID provided by the user
    val webClientId = "172023820889-761d0e5evulp4nbiqoqia3tqsqt2kuij.apps.googleusercontent.com"

    fun initiateGoogleSignIn() {
        if (webClientId == "YOUR_WEB_CLIENT_ID") {
            Toast.makeText(context, "Please set your WEB_CLIENT_ID in LoginScreen.kt", Toast.LENGTH_LONG).show()
            return
        }

        coroutineScope.launch {
            try {
                isLoading = true
                val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    
                    Log.d("Auth", "Successfully signed in! Email: $email")
                    Toast.makeText(context, "Verifying role for $email...", Toast.LENGTH_SHORT).show()
                    
                    // Call our Google Sheets Apps Script API!
                    try {
                        val response = withContext(Dispatchers.IO) {
                            NetworkModule.api.getUserRole(email)
                        }
                        
                        Log.d("Auth", "API Response: $response")
                        Toast.makeText(context, "Welcome! Role: ${response.role}", Toast.LENGTH_LONG).show()
                        
                        // Save session
                        userPreferences.saveSession(email = response.email, role = response.role)
                        
                        onLoginSuccess(response.role)
                        
                    } catch (e: Exception) {
                        Log.e("Auth", "Network Error checking role", e)
                        Toast.makeText(context, "Network Error checking role", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Google Sign-In failed: ${e.type}", e)
                Toast.makeText(context, "Auth Error: ${e.type.substringAfterLast('.')}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("Auth", "Unexpected error", e)
                Toast.makeText(context, "Unexpected Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎓", fontSize = 64.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Welcome to\nStudent Tracker",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Log in with your institutional or personal Google account to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { initiateGoogleSignIn() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Signing in...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
