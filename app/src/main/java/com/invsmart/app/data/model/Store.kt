package com.invsmart.app.data.model

import com.google.firebase.Timestamp

data class Store(
    val storeId: String = "",
    val chainId: String = "",
    val name: String = "",
    val address: String = "",
    val masterUid: String = "",
    val managerUid: String = "",
    val managerName: String = "",
    val status: String = "active", // "active", "suspended"
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
)
