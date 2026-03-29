package com.invsmart.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.invsmart.app.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getProductsRealtime(): Flow<Result<List<Product>>> = callbackFlow {
        val registration = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Product::class.java)?.let { product ->
                        if (product.productId.isBlank()) product.copy(productId = document.id) else product
                    }
                }.orEmpty()

                trySend(Result.success(products))
            }

        awaitClose { registration.remove() }
    }
        .flowOn(Dispatchers.IO)

    suspend fun addProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = product.productId.ifEmpty { product.sku }
            firestore.collection("products").document(docId).set(product).await()
            Unit
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = product.productId.ifEmpty { product.sku }
            firestore.collection("products").document(docId).set(product).await()
            Unit
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("products").document(productId).delete().await()
            Unit
        }
    }
}