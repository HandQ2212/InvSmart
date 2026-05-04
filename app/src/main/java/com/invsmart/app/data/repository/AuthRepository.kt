package com.invsmart.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
            Unit
        }
    }

    suspend fun register(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
            Unit
        }
    }

    suspend fun checkAndResetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userQuery = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            if (userQuery.isEmpty) {
                throw Exception("Email không tồn tại trong hệ thống.")
            }

            auth.sendPasswordResetEmail(email).await()
            Unit
        }
    }

    fun logout() {
        auth.signOut()
    }
}
