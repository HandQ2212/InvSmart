package com.invsmart.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invsmart.app.data.local.SessionManager
import com.invsmart.app.data.model.AuthState
import com.invsmart.app.data.model.UiState
import com.invsmart.app.data.model.User
import com.invsmart.app.data.repository.AuthRepository
import com.invsmart.app.data.repository.ProductRepository
import com.invsmart.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        loadUserAndStartProducts()
    }

    fun loadUserAndStartProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProducts = true) }
            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                userRepository.getUser(uid).onSuccess { user ->
                    if (user == null) {
                        authRepository.logout()
                        _uiState.update {
                            it.copy(
                                authState = AuthState.Error("Tài khoản chưa được cấu hình."),
                                currentUser = null,
                                isLoadingProducts = false
                            )
                        }
                        return@onSuccess
                    }

                    if (user.accessStatus == "blocked") {
                        authRepository.logout()
                        _uiState.update {
                            it.copy(
                                authState = AuthState.Error("Tài khoản đã bị khóa."),
                                currentUser = null,
                                isLoadingProducts = false
                            )
                        }
                        return@onSuccess
                    }

                    val normalizedRole = normalizeRole(user)
                    sessionManager.saveRole(normalizedRole)
                    sessionManager.saveStoreId(user.storeId)

                    _uiState.update {
                        it.copy(
                            authState = AuthState.Authenticated,
                            currentUser = user.copy(roleGlobal = normalizedRole),
                            isLoadingProducts = false
                        )
                    }
                    android.util.Log.d("AUTH", "LOGIN SUCCESS: Role is $normalizedRole")
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Error(e.message ?: "Lỗi tải dữ liệu"),
                            isLoadingProducts = false
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        authState = AuthState.Unauthenticated,
                        currentUser = null,
                        isLoadingProducts = false
                    )
                }
            }
        }
    }

    private fun normalizeRole(user: User): String {
        val rGlobal = user.roleGlobal.trim().lowercase()
        val rLegacy = user.role.trim().lowercase()
        
        android.util.Log.d("DEBUG_ROLE", "RAW DATA from Firestore -> roleGlobal: '${user.roleGlobal}', role: '${user.role}', isMaster: ${user.isMaster}")
        
        val role = when {
            rGlobal == "admin" || rLegacy == "admin" -> "admin"
            user.isMaster || rGlobal == "master" || rLegacy == "master" -> "master"
            rGlobal == "manager" || rLegacy == "manager" -> "manager"
            rGlobal == "unassigned" -> "unassigned"
            else -> "staff"
        }
        android.util.Log.d("DEBUG_ROLE", "FINAL Result for ${user.email} -> $role")
        return role
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading) }
            authRepository.login(email, password)
                .onSuccess {
                    checkAuthStatus()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(authState = AuthState.Error(e.message ?: "Đăng nhập thất bại")) }
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading) }
            authRepository.register(email, password)
                .onSuccess {
                    val uid = authRepository.getCurrentUserId() ?: return@onSuccess
                    val newUser = User(
                        uid = uid,
                        email = email,
                        roleGlobal = "unassigned",
                        accessStatus = "pending"
                    )
                    userRepository.createUser(newUser).onSuccess {
                        _uiState.update {
                            it.copy(
                                authState = AuthState.Authenticated,
                                currentUser = newUser
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(authState = AuthState.Error(e.message ?: "Đăng ký thất bại")) }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            sessionManager.clear()
            _uiState.update {
                it.copy(
                    authState = AuthState.Unauthenticated,
                    currentUser = null,
                    products = emptyList()
                )
            }
        }
    }

    fun resetPassword(email: String, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.checkAndResetPassword(email)
                .onSuccess { callback(true, "Yêu cầu đã được gửi.") }
                .onFailure { callback(false, it.message ?: "Lỗi.") }
        }
    }

    fun updateProfile(fullName: String, phoneNumber: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            userRepository.updateProfile(uid, fullName, phoneNumber).onSuccess {
                checkAuthStatus()
            }
        }
    }

    fun runMigration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProducts = true) }
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val defaultChainId = "default_chain"
                val defaultStoreId = "default_store"

                // 1. Tạo Chuỗi mặc định (nếu chưa có)
                android.util.Log.d("MIGRATION", "Creating default chain...")
                db.collection("chains").document(defaultChainId).set(mapOf(
                    "chainId" to defaultChainId,
                    "name" to "Chuỗi mặc định",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )).await()

                // 2. Tạo Cửa hàng mặc định (nếu chưa có)
                android.util.Log.d("MIGRATION", "Creating default store...")
                db.collection("stores").document(defaultStoreId).set(mapOf(
                    "storeId" to defaultStoreId,
                    "chainId" to defaultChainId,
                    "name" to "Cửa hàng mặc định",
                    "address" to "Địa chỉ mặc định",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )).await()

                // 3. Cập nhật toàn bộ User
                val users = userRepository.getAllUsers().getOrNull() ?: emptyList()
                users.forEach { user ->
                    if (user.roleGlobal != "admin") {
                        db.collection("users").document(user.uid).update(mapOf(
                            "chainId" to defaultChainId,
                            "storeId" to (user.storeId.ifEmpty { defaultStoreId }),
                            "roleGlobal" to (if (user.isMaster) "master" else if (user.role == "manager") "manager" else "staff")
                        )).await()
                    }
                }

                // 4. Cập nhật Sản phẩm và Đơn hàng
                val products = db.collection("products").get().await()
                products.forEach { doc ->
                    doc.reference.update(mapOf("chainId" to defaultChainId, "storeId" to defaultStoreId)).await()
                }

                val orders = db.collection("orders").get().await()
                orders.forEach { doc ->
                    doc.reference.update(mapOf("chainId" to defaultChainId, "storeId" to defaultStoreId)).await()
                }

                android.util.Log.d("MIGRATION", "DATABASE INITIALIZED SUCCESSFULLY!")
            } catch (e: Exception) {
                android.util.Log.e("MIGRATION", "Migration Failed: ${e.message}")
            }
            _uiState.update { it.copy(isLoadingProducts = false) }
        }
    }
}
