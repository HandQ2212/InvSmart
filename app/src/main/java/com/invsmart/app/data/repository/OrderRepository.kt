package com.invsmart.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.invsmart.app.data.model.OrderItem
import com.invsmart.app.data.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createOrder(order: Order): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val batch = firestore.batch()

            val orderRef = if (order.orderId.isEmpty()) {
                firestore.collection("orders").document()
            } else {
                firestore.collection("orders").document(order.orderId)
            }

            val normalizedItems = order.items.map { item ->
                item.copy(productId = item.productId.ifBlank { item.productName })
            }
            val totalQty = normalizedItems.sumOf(OrderItem::quantity)
            val totalAmount = normalizedItems.sumOf { it.quantity * it.priceAtTime }

            batch.set(orderRef, order.copy(
                orderId = orderRef.id,
                items = normalizedItems,
                totalQuantity = totalQty,
                totalAmount = totalAmount,
                status = "pending_payment"
            ))

            for (item in normalizedItems) {
                val productRef = firestore.collection("products").document(item.productId)
                val diff = if (order.orderType == "import") item.quantity else -item.quantity
                if (diff != 0) {
                    batch.update(productRef, "stockQty", FieldValue.increment(diff.toLong()))
                }
            }

            batch.commit().await()
            orderRef.id
        }
    }

    suspend fun getOrdersByStaff(staffUid: String): Result<List<Order>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("staffUid", staffUid)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.toObject(Order::class.java) }
        }
    }

    suspend fun markOrderPaid(orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("orders").document(orderId).update(
                mapOf(
                    "status" to "paid",
                    "paidAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            Unit
        }
    }

    suspend fun getTotalRevenue(): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("status", "paid")
                .get()
                .await()

            snapshot.documents.sumOf { it.getDouble("totalAmount") ?: 0.0 }
        }
    }
}
