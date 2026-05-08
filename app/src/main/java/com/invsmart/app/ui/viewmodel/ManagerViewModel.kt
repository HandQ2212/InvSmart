package com.invsmart.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invsmart.app.data.model.Product
import com.invsmart.app.data.model.User
import com.invsmart.app.data.repository.OrderRepository
import com.invsmart.app.data.repository.ProductRepository
import com.invsmart.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagerViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _operationStatus = MutableStateFlow<Result<Unit>?>(null)
    val operationStatus: StateFlow<Result<Unit>?> = _operationStatus.asStateFlow()

    private val _managedUsers = MutableStateFlow<List<User>>(emptyList())
    val managedUsers: StateFlow<List<User>> = _managedUsers.asStateFlow()

    private val _revenue = MutableStateFlow(0.0)
    val revenue: StateFlow<Double> = _revenue.asStateFlow()

    private val _managerMessage = MutableStateFlow<String?>(null)
    val managerMessage: StateFlow<String?> = _managerMessage.asStateFlow()

    fun resetMessage() {
        _managerMessage.value = null
    }

    fun loadDashboard(actor: User) {
        viewModelScope.launch {
            orderRepository.getTotalRevenue(actor.storeId)
                .onSuccess { _revenue.value = it }
                .onFailure { _managerMessage.value = it.localizedMessage ?: "Không tải được doanh thu" }

            userRepository.getManageableUsers(actor)
                .onSuccess { _managedUsers.value = it }
                .onFailure { _managerMessage.value = it.localizedMessage ?: "Không tải được danh sách người dùng" }
        }
    }

    fun toggleManagerStaffRole(actor: User, target: User) {
        if (actor.roleGlobal != "master" && !actor.isMaster) {
            _managerMessage.value = "Chỉ Master mới được đổi quyền Manager/Staff"
            return
        }

        if (target.roleGlobal == "master" || target.isMaster) {
            _managerMessage.value = "Không thể đổi quyền tài khoản Master"
            return
        }

        val newRole = if (target.roleGlobal == "manager") "staff" else "manager"

        viewModelScope.launch {
            userRepository.updateUserRole(target.uid, newRole)
                .onSuccess {
                    _managerMessage.value = "Đã đổi ${target.email} thành ${newRole.uppercase()}"
                    loadDashboard(actor)
                }
                .onFailure {
                    _managerMessage.value = it.localizedMessage ?: "Không thể đổi quyền"
                }
        }
    }

    fun resetOperationStatus() {
        _operationStatus.value = null
    }

    fun addProduct(product: Product, actor: User) {
        viewModelScope.launch {
            val now = com.google.firebase.Timestamp.now()
            val payload = product.copy(
                productId = product.productId.ifEmpty { product.sku },
                storeId = actor.storeId,
                teamId = actor.storeId,
                createdBy = actor.uid,
                createdAt = now,
                updatedAt = now
            )
            _operationStatus.value = productRepository.addProduct(payload)
        }
    }

    fun updateProduct(product: Product, actor: User) = viewModelScope.launch {
        val payload = product.copy(
            storeId = actor.storeId,
            teamId = actor.storeId,
            updatedAt = com.google.firebase.Timestamp.now()
        )
        _operationStatus.value = productRepository.updateProduct(payload)
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _operationStatus.value = productRepository.deleteProduct(productId)
        }
    }
}
