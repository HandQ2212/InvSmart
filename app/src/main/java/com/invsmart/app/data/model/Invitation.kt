package com.invsmart.app.data.model

import com.google.firebase.Timestamp

data class Invitation(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val receiverUid: String = "",
    val targetChainId: String = "",
    val targetStoreId: String = "",
    val targetRole: String = "", // "master", "manager", "staff"
    val storeName: String = "", // To show to the user
    val status: String = "pending", // "pending", "accepted", "rejected"
    val createdAt: Timestamp = Timestamp.now()
)
