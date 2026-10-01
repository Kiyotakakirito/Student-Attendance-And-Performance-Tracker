package com.example.studenttracker.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SignedInUser(val email: String, val name: String, val role: String, val expiresAt: Long)

object AuthSession {
    private val mutableUser = MutableStateFlow<SignedInUser?>(null)
    val user = mutableUser.asStateFlow()
    @Volatile var idToken: String? = null
        private set
    fun begin(token: String) { idToken = token }
    fun complete(user: SignedInUser) { mutableUser.value = user }
    fun clear() {
        idToken = null
        mutableUser.value = null
    }
}
