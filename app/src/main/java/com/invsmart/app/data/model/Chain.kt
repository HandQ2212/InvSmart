package com.invsmart.app.data.model

import com.google.firebase.Timestamp

data class Chain(
    val chainId: String = "",
    val name: String = "",
    val masterUid: String = "",
    val createdAt: Timestamp? = null,
    val description: String = ""
)
