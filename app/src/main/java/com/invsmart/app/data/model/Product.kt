package com.invsmart.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Product(
    val productId: String = "",
    val teamId: String = "",
    val sku: String = "",
    val name: String = "",
    val category: String = "",
    @get:PropertyName("stockQty") @set:PropertyName("stockQty")
    var stockQuantity: Int = 0,
    val price: Double = 0.0,
    val unit: String = "Cái",
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)