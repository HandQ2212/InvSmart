package com.invsmart.app.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = "", // Legacy field
    val roleGlobal: String = "unassigned", // "admin", "master", "manager", "staff" hoặc "unassigned"
    val isMaster: Boolean = false, // Deprecated, use roleGlobal == "master"
    val chainId: String = "", // The chain the user belongs to
    val storeId: String = "", // The specific branch/store the user belongs to
    val status: String = "active",
    val defaultTeamId: String? = null, // Kept for backward compatibility
    val accessStatus: String = "pending",
    val phoneNumber: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
)