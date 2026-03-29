package com.invsmart.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
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

    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            auth.sendPasswordResetEmail(email).await()
            Unit
        }
    }

    fun logout() {
        auth.signOut()
    }
}
