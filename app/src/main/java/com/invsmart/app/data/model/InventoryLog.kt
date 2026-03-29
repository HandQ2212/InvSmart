package com.invsmart.app.data.model

import com.google.firebase.Timestamp

data class InventoryLog(
    val id: String = "",
    val product_id: String = "",
    val user_id: String = "",
    val type: String = "IN",
    val quantity_change: Int = 0,
    val reason: String = "",
    val timestamp: Timestamp? = null
)
