package com.example.studenttracker.domain.model

import com.google.gson.annotations.SerializedName

data class UserRoleResponse(
    @SerializedName("status") val status: String,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String?,
    @SerializedName("role") val role: String,
    val expiresAt: Long = 0
)
