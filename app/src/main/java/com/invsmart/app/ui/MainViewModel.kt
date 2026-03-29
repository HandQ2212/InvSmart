package com.invsmart.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invsmart.app.data.model.AuthState
import com.invsmart.app.data.model.UiState
import com.invsmart.app.data.repository.AuthRepository
import com.invsmart.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.invsmart.app.data.repository.UserRepository
import com.invsmart.app.data.model.User

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private var observeProductsJob: Job? = null

    init {
        if (authRepository.isLoggedIn()) {
            _uiState.update { it.copy(authState = AuthState.Loading, message = "Đang tải thông tin...") }
            loadUserAndStartProducts()
        }
    }

    private fun loadUserAndStartProducts() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            if (uid != null) {
                userRepository.getUser(uid).onSuccess { user ->
                    if (user == null) {
                        authRepository.logout()
                        _uiState.update {
                            it.copy(
                                authState = AuthState.Error("Tài khoản chưa được cấu hình trong hệ thống. Vui lòng liên hệ quản lý."),
                                currentUser = null,
                                activeTeamId = null,
                                products = emptyList(),
                                isLoadingProducts = false
                            )
                        }
                        return@onSuccess
                    }

                    if (user.accessStatus.equals("blocked", ignoreCase = true)
                        || user.accessStatus.equals("disabled", ignoreCase = true)
                    ) {
                        authRepository.logout()
                        _uiState.update {
                            it.copy(
                                authState = AuthState.Error("Tài khoản đã bị khóa. Vui lòng liên hệ quản lý."),
                                currentUser = null,
                                activeTeamId = null,
                                products = emptyList(),
                                isLoadingProducts = false
                            )
                        }
                        return@onSuccess
                    }

                    val normalizedRole = when {
                        user.isMaster || user.roleGlobal.equals("master", ignoreCase = true) -> "master"
                        user.roleGlobal.equals("manager", ignoreCase = true)
                            || user.role.equals("manager", ignoreCase = true) -> "manager"
                        else -> "staff"
                    }
                    val normalizedUser = user.copy(
                        roleGlobal = normalizedRole,
                        isMaster = user.isMaster || normalizedRole == "master"
                    )

                    _uiState.update {
                        it.copy(
                            authState = AuthState.Authenticated,
                            message = "Đăng nhập thành công",
                            currentUser = normalizedUser,
                            activeTeamId = null
                        ) 
                    }

                    startObserveProducts()
                }.onFailure {
                    val reason = it.localizedMessage ?: it.message ?: "unknown"
                    _uiState.update {
                        it.copy(authState = AuthState.Error("Không thể tải thông tin user: $reason"))
                    }
                }
            } else {
                _uiState.update { it.copy(authState = AuthState.Unauthenticated) }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading, message = "Đang đăng nhập...") }
            authRepository.login(email, password)
                .onSuccess {
                    loadUserAndStartProducts()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(authState = AuthState.Error(error.localizedMessage ?: "Đăng nhập thất bại"))
                    }
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Loading, message = "Đang tạo tài khoản...") }
            authRepository.register(email, password)
                .onSuccess {
                    val uid = authRepository.getCurrentUserId()
                    if (uid != null) {
                        val newUser = User(
                            uid = uid,
                            email = email.trim().lowercase(),
                            roleGlobal = "staff",
                            isMaster = false,
                            accessStatus = "active"
                        )
                        userRepository.createUser(newUser).onSuccess {
                            loadUserAndStartProducts()
                        }.onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    authState = AuthState.Error(
                                        error.localizedMessage ?: "Tạo hồ sơ người dùng thất bại"
                                    )
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(authState = AuthState.Error("Không lấy được thông tin tài khoản mới"))
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(authState = AuthState.Error(error.localizedMessage ?: "Tạo tài khoản thất bại"))
                    }
                }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authRepository.resetPassword(email)
                .onSuccess {
                    onResult(true, "Email khôi phục đã được gửi")
                }
                .onFailure { error ->
                    onResult(false, error.localizedMessage ?: "Lỗi gửi email khôi phục")
                }
        }
    }

    fun logout() {
        observeProductsJob?.cancel()
        authRepository.logout()
        _uiState.update {
            it.copy(
                authState = AuthState.Unauthenticated,
                products = emptyList(),
                isLoadingProducts = false,
                message = "Đã đăng xuất",
                currentUser = null,
                activeTeamId = null
            )
        }
    }

    fun updateProfile(fullName: String, phoneNumber: String) {
        val uid = _uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.updateProfile(uid, fullName.trim(), phoneNumber.trim())
                .onSuccess {
                    val current = _uiState.value.currentUser
                    if (current != null) {
                        _uiState.update {
                            it.copy(
                                currentUser = current.copy(
                                    fullName = fullName.trim(),
                                    phoneNumber = phoneNumber.trim()
                                ),
                                message = "Cập nhật thông tin thành công"
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(message = it.localizedMessage ?: "Không thể cập nhật thông tin")
                    }
                }
        }
    }

    private fun startObserveProducts() {
        observeProductsJob?.cancel()
        observeProductsJob = viewModelScope.launch {
            productRepository.getProductsRealtime().collect { result ->
                _uiState.update { it.copy(isLoadingProducts = true) }
                result.onSuccess { products ->
                    _uiState.update {
                        it.copy(
                            products = products,
                            isLoadingProducts = false,
                            message = "Tải ${products.size} sản phẩm"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingProducts = false,
                            message = error.localizedMessage ?: "Không thể tải sản phẩm"
                        )
                    }
                }
            }
        }
    }
}
