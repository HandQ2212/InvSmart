package com.invsmart.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val uid: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "", // Legacy field
    val roleGlobal: String = "staff", // "master", "manager" hoặc "staff"
    val isMaster: Boolean = false, // Deprecated, use roleGlobal == "master"
    val storeId: String = "", // Current store the user belongs to
    val status: String = "active",
    val defaultTeamId: String? = null, // Kept for backward compatibility
    val accessStatus: String = "pending",
    val phoneNumber: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
)