package com.invsmart.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Store(
    @DocumentId
    val storeId: String = "",
    val name: String = "",
    val address: String = "",
    val masterUid: String = "",
    val status: String = "active", // "active", "suspended"
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp? = null
)
