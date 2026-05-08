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

    private val _stores = MutableStateFlow<List<Store>>(emptyList())
    val stores: StateFlow<List<Store>> = _stores.asStateFlow()

    private val _catalogProducts = MutableStateFlow<List<Product>>(emptyList())
    val catalogProducts: StateFlow<List<Product>> = _catalogProducts.asStateFlow()

    private val _branchRevenueMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    val branchRevenueMap: StateFlow<Map<String, Double>> = _branchRevenueMap.asStateFlow()

    private val _managerMessage = MutableStateFlow<String?>(null)
    val managerMessage: StateFlow<String?> = _managerMessage.asStateFlow()

    @Inject
    lateinit var storeRepository: StoreRepository

    fun resetMessage() {
        _managerMessage.value = null
    }

    fun loadDashboard(actor: User) {
        viewModelScope.launch {
            val chainId = actor.chainId
            val storeId = if (actor.roleGlobal != "master") actor.storeId else ""

            // Total Revenue
            orderRepository.getTotalRevenue(chainId = if (actor.roleGlobal == "master") chainId else "", storeId = storeId)
                .onSuccess { _revenue.value = it }
                .onFailure { _managerMessage.value = it.localizedMessage ?: "Không tải được doanh thu" }

            // Users
            userRepository.getManageableUsers(actor)
                .onSuccess { _managedUsers.value = it }
                .onFailure { _managerMessage.value = it.localizedMessage ?: "Không tải được danh sách người dùng" }

            if (actor.roleGlobal == "master") {
                // Stores
                storeRepository.getStoresByChain(chainId)
                    .onSuccess { 
                        _stores.value = it 
                        // Load revenue for each store
                        loadRevenuePerStore(it)
                    }
                
                // Catalog Products
                productRepository.getProductsRealtime(chainId = chainId, storeId = "")
                    .collect { result ->
                        result.onSuccess { _catalogProducts.value = it }
                    }
            }
        }
    }

    private fun loadRevenuePerStore(stores: List<Store>) {
        viewModelScope.launch {
            val revenueMap = mutableMapOf<String, Double>()
            stores.forEach { store ->
                orderRepository.getTotalRevenue(storeId = store.storeId)
                    .onSuccess { revenueMap[store.storeId] = it }
            }
            _branchRevenueMap.value = revenueMap
        }
    }

    fun createBranch(name: String, address: String, actor: User, manager: User? = null) {
        viewModelScope.launch {
            val newStore = Store(
                name = name,
                address = address,
                chainId = actor.chainId,
                masterUid = actor.uid,
                managerUid = manager?.uid ?: "",
                managerName = manager?.fullName ?: ""
            )
            storeRepository.createStore(newStore, manager)
                .onSuccess {
                    _managerMessage.value = "Đã tạo chi nhánh $name"
                    loadDashboard(actor)
                }
                .onFailure {
                    _managerMessage.value = it.localizedMessage ?: "Lỗi tạo chi nhánh"
                }
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

    fun addCatalogProduct(product: Product, actor: User) {
        if (actor.roleGlobal != "master") {
            _managerMessage.value = "Chỉ Master mới có quyền tạo sản phẩm danh mục"
            return
        }
        viewModelScope.launch {
            val now = com.google.firebase.Timestamp.now()
            val payload = product.copy(
                productId = product.productId.ifEmpty { product.sku },
                chainId = actor.chainId,
                storeId = "", // Catalog product
                createdBy = actor.uid,
                createdAt = now,
                updatedAt = now
            )
            _operationStatus.value = productRepository.addProduct(payload)
        }
    }

    fun addProduct(product: Product, actor: User) {
        viewModelScope.launch {
            val now = com.google.firebase.Timestamp.now()
            val payload = product.copy(
                productId = product.productId.ifEmpty { product.sku },
                chainId = actor.chainId,
                storeId = actor.storeId,
                createdBy = actor.uid,
                createdAt = now,
                updatedAt = now
            )
            _operationStatus.value = productRepository.addProduct(payload)
        }
    }

    fun updateProduct(product: Product, actor: User) = viewModelScope.launch {
        val payload = product.copy(
            chainId = actor.chainId,
            storeId = actor.storeId,
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
