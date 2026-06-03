package com.invsmart.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
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
    private fun mapFirestoreError(throwable: Throwable): String {
        return if (throwable is FirebaseFirestoreException) {
            when (throwable.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Firestore từ chối ghi dữ liệu. Kiểm tra rules và chainId của tài khoản master."
                FirebaseFirestoreException.Code.UNAVAILABLE -> "Không kết nối được tới Firestore. Kiểm tra mạng hoặc Firestore emulator."
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Chưa đăng nhập Firebase hoặc phiên đăng nhập đã hết hạn."
                else -> throwable.localizedMessage ?: "Lỗi Firestore không xác định"
            }
        } else {
            throwable.localizedMessage ?: "Lỗi không xác định"
        }
    }

    fun getProductsRealtime(chainId: String = "", storeId: String = "", onlyCatalog: Boolean = false): Flow<Result<List<Product>>> = callbackFlow {
        val baseQuery = firestore.collection("products")
        var query = if (storeId.isNotEmpty()) {
            baseQuery.whereEqualTo("storeId", storeId)
        } else if (chainId.isNotEmpty()) {
            baseQuery.whereEqualTo("chainId", chainId)
        } else {
            baseQuery
        }

        if (onlyCatalog) {
            query = query.whereEqualTo("storeId", "")
        }

        val registration = query.addSnapshotListener { snapshot, error ->
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
            Log.d("PRODUCT_REPO", "addProduct docId=$docId chainId=${product.chainId} storeId=${product.storeId} sku=${product.sku}")
            firestore.collection("products").document(docId).set(product).await()
            Unit
        }.onFailure { throwable ->
            Log.e("PRODUCT_REPO", "addProduct failed: ${throwable.message}", throwable)
        }.recoverCatching { throwable ->
            throw IllegalStateException(mapFirestoreError(throwable), throwable)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val docId = product.productId.ifEmpty { product.sku }
            Log.d("PRODUCT_REPO", "updateProduct docId=$docId chainId=${product.chainId} storeId=${product.storeId} sku=${product.sku}")
            firestore.collection("products").document(docId).set(product).await()
            Unit
        }.onFailure { throwable ->
            Log.e("PRODUCT_REPO", "updateProduct failed: ${throwable.message}", throwable)
        }.recoverCatching { throwable ->
            throw IllegalStateException(mapFirestoreError(throwable), throwable)
        }
    }

    suspend fun getProduct(productId: String): Result<Product?> = withContext(Dispatchers.IO) {
        runCatching {
            val doc = firestore.collection("products").document(productId).get().await()
            doc.toObject(Product::class.java)?.copy(productId = doc.id)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            firestore.collection("products").document(productId).delete().await()
            Unit
        }
    }
}