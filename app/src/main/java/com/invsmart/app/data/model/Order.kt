package com.invsmart.app.data.model

import com.google.firebase.Timestamp

data class Order(
    val orderId: String = "",
    val orderCode: String = "",
    val chainId: String = "",
    val storeId: String = "",
    val staffUid: String = "",
    val staffName: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val orderType: String = "sale",
    val status: String = "pending_payment",
    val items: List<OrderItem> = emptyList(),
    val totalQuantity: Int = 0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "qr",
    val paidAt: Timestamp? = null
)

data class OrderItem(
    val productId: String = "", 
    val productName: String = "",
    val quantity: Int = 0,
    val priceAtTime: Double = 0.0
)