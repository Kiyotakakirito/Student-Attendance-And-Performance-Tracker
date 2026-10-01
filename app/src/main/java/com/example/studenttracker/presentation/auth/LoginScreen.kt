package com.example.studenttracker.presentation.auth

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
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

import com.example.studenttracker.BuildConfig
import com.example.studenttracker.data.network.AuthSession
import com.example.studenttracker.data.network.SignedInUser
import com.example.studenttracker.data.network.NetworkModule
import kotlinx.coroutines.CancellationException
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

    var loginError by remember { mutableStateOf<String?>(null) }
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

    fun initiateGoogleSignIn() {
        if (!NetworkModule.isConfigured) {
            loginError = "This app has not been connected to your institution yet. Contact the administrator."
            return
        }
        loginError = null
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
                    AuthSession.begin(googleIdTokenCredential.idToken)
                    val response = withContext(Dispatchers.IO) { NetworkModule.api.getUserRole() }
                    check(response.status == "success" && response.role in setOf("admin", "faculty", "student") && response.expiresAt > System.currentTimeMillis() / 1000) {
                        "Your account does not have an active role. Contact the administrator."
                    }
                    userPreferences.saveSession(response.email, response.role)
                    AuthSession.complete(SignedInUser(response.email, response.name.orEmpty(), response.role, response.expiresAt))
                    onLoginSuccess(response.role)
                } else {
                    error("Google returned an unsupported credential. Try signing in again.")
                }
            } catch (e: CancellationException) {
                AuthSession.clear()
                throw e
            } catch (e: NoCredentialException) {
                AuthSession.clear()
                loginError = "No Google account is available. Add an account to your device and try again."
            } catch (e: GetCredentialException) {
                AuthSession.clear()
                loginError = "Sign-in was cancelled or unavailable. Please try again."
            } catch (e: Exception) {
                AuthSession.clear()
                loginError = e.message ?: "Unable to sign in. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
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
            Spacer(modifier = Modifier.height(24.dp))
            loginError?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
            Spacer(modifier = Modifier.height(24.dp))
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
