package com.invsmart.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invsmart.app.data.model.Product
import com.invsmart.app.data.model.Store
import com.invsmart.app.data.model.User
import com.invsmart.app.data.repository.OrderRepository
import com.invsmart.app.data.repository.ProductRepository
import com.invsmart.app.data.repository.StoreRepository
import com.invsmart.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagerViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val cloudinaryRepository: com.invsmart.app.data.repository.CloudinaryRepository
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

    fun resetMessage() {
        _managerMessage.value = null
    }

    private val _currentChainId = MutableStateFlow("")
    private val _currentStoreId = MutableStateFlow("")

    init {
        // Realtime Stores for Master
        viewModelScope.launch {
            _currentChainId
                .flatMapLatest { chainId ->
                    if (chainId.isNotEmpty()) {
                        storeRepository.getStoresRealtime(chainId)
                    } else {
                        kotlinx.coroutines.flow.flowOf(Result.success(emptyList()))
                    }
                }
                .collect { result ->
                    if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return@collect
                    result.onSuccess { 
                        _stores.value = it 
                        loadRevenuePerStore(it)
                    }
                }
        }

        // Realtime Catalog Products for Master/Manager
        viewModelScope.launch {
            _currentChainId
                .flatMapLatest { chainId ->
                    android.util.Log.d("MANAGER_VM", "Fetching Catalog for ChainId: '$chainId'")
                    if (chainId.isNotEmpty()) {
                        productRepository.getProductsRealtime(chainId = chainId, storeId = "", onlyCatalog = true)
                    } else {
                        flowOf(Result.success(emptyList()))
                    }
                }
                .collect { result ->
                    if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return@collect
                    result.onSuccess { 
                        android.util.Log.d("MANAGER_VM", "Catalog Fetch SUCCESS: ${it.size} items")
                        _catalogProducts.value = it 
                    }
                    result.onFailure { 
                        android.util.Log.e("MANAGER_VM", "Catalog Fetch FAILED", it)
                        _managerMessage.value = "Lỗi tải danh mục: ${it.localizedMessage}"
                    }
                }
        }
    }

    fun loadDashboard(actor: User) {
        _currentChainId.value = actor.chainId
        _currentStoreId.value = if (actor.roleGlobal != "master") actor.storeId else ""

        viewModelScope.launch {
            // 1. One-time fetch for Manageable Users
            userRepository.getManageableUsers(actor)
                .onSuccess { _managedUsers.value = it }
                .onFailure { _managerMessage.value = it.localizedMessage ?: "Không tải được danh sách người dùng" }

            // 2. Refresh Revenue
            refreshRevenue(actor)

            // 2. Refresh Revenue
            refreshRevenue(actor)
        }
    }

    private fun refreshRevenue(actor: User) {
        viewModelScope.launch {
            val chainId = actor.chainId
            val storeId = if (actor.roleGlobal != "master") actor.storeId else ""
            orderRepository.getTotalRevenue(chainId = if (actor.roleGlobal == "master") chainId else "", storeId = storeId)
                .onSuccess { _revenue.value = it }
        }
    }

    private fun loadRevenuePerStore(stores: List<Store>) {
        val chainId = _currentChainId.value
        viewModelScope.launch {
            coroutineScope {
                val revenueDeferred = stores.map { store ->
                    async {
                        store.storeId to (orderRepository.getTotalRevenue(chainId = chainId, storeId = store.storeId).getOrDefault(0.0))
                    }
                }
                val results = revenueDeferred.awaitAll()
                _branchRevenueMap.value = results.toMap()
            }
        }
    }

    fun createBranch(name: String, address: String, actor: User, manager: User? = null) {
        android.util.Log.d("MANAGER_VM", "createBranch: $name, manager: ${manager?.uid}")
        
        if (actor.chainId.isBlank()) {
            _managerMessage.value = "Lỗi: Tài khoản Master chưa được gán mã chuỗi (chainId)"
            android.util.Log.e("MANAGER_VM", "createBranch aborted: Actor chainId is blank")
            return
        }

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
                    android.util.Log.d("MANAGER_VM", "createBranch SUCCESS: $name")
                    _managerMessage.value = "Đã tạo chi nhánh $name"
                    loadDashboard(actor)
                }
                .onFailure {
                    android.util.Log.e("MANAGER_VM", "createBranch FAILED: ${it.message}")
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

    fun addCatalogProduct(product: Product, actor: User, imageUri: android.net.Uri? = null) {
        if (actor.roleGlobal.lowercase() != "master") {
            _managerMessage.value = "Chỉ Master mới có quyền tạo sản phẩm danh mục"
            return
        }
        if (actor.chainId.isBlank()) {
            _managerMessage.value = "Lỗi: Tài khoản Master chưa được gán mã chuỗi (chainId)"
            return
        }
        viewModelScope.launch {
            var finalImageUrl = product.imageUrl
            if (imageUri != null) {
                cloudinaryRepository.uploadImage(imageUri)
                    .onSuccess { finalImageUrl = it }
                    .onFailure { 
                        _managerMessage.value = "Lỗi tải ảnh lên Cloudinary: ${it.localizedMessage}"
                        return@launch
                    }
            }

            val now = com.google.firebase.Timestamp.now()
            val finalProductId = if (product.productId.isNotEmpty()) product.productId else "catalog_${actor.chainId}_${product.sku}"
            
            val payload = product.copy(
                productId = finalProductId,
                chainId = actor.chainId,
                storeId = "", // Catalog product
                imageUrl = finalImageUrl,
                createdBy = actor.uid,
                createdAt = now,
                updatedAt = now
            )
            val result = productRepository.addProduct(payload)
            _operationStatus.value = result

            if (result.isSuccess) {
                _managerMessage.value = "Đã thêm sản phẩm vào danh mục"
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Lỗi không xác định"
                _managerMessage.value = "Lỗi thêm sản phẩm: $error"
            }
        }
    }

    fun updateCatalogProduct(product: Product, actor: User, imageUri: android.net.Uri? = null) {
        if (actor.roleGlobal.lowercase() != "master") {
            _managerMessage.value = "Chỉ Master mới có quyền chỉnh sửa sản phẩm danh mục"
            return
        }
        viewModelScope.launch {
            var finalImageUrl = product.imageUrl
            if (imageUri != null) {
                cloudinaryRepository.uploadImage(imageUri)
                    .onSuccess { finalImageUrl = it }
                    .onFailure { 
                        _managerMessage.value = "Lỗi cập nhật ảnh lên Cloudinary: ${it.localizedMessage}"
                        return@launch
                    }
            }

            val payload = product.copy(
                chainId = actor.chainId,
                storeId = "",
                imageUrl = finalImageUrl,
                updatedAt = com.google.firebase.Timestamp.now()
            )
            val result = productRepository.updateProduct(payload)
            _operationStatus.value = result
            
            if (result.isSuccess) {
                _managerMessage.value = "Đã cập nhật sản phẩm danh mục"
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Lỗi không xác định"
                _managerMessage.value = "Lỗi cập nhật: $error"
            }
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

    fun importFromCatalog(catalogProduct: Product, quantity: Int, actor: User) {
        viewModelScope.launch {
            val now = com.google.firebase.Timestamp.now()
            val branchProductId = "${actor.storeId}_${catalogProduct.sku}"
            
            // 1. Check if product already exists in this branch
            val existingResult = productRepository.getProduct(branchProductId)
            val existingProduct = existingResult.getOrNull()

            val finalProduct = if (existingProduct != null) {
                // Update existing stock
                existingProduct.copy(
                    stockQuantity = existingProduct.stockQuantity + quantity,
                    updatedAt = now
                )
            } else {
                // Create new branch product based on catalog
                catalogProduct.copy(
                    productId = branchProductId,
                    storeId = actor.storeId,
                    chainId = actor.chainId,
                    stockQuantity = quantity,
                    createdBy = actor.uid,
                    createdAt = now,
                    updatedAt = now
                )
            }
            
            val result = productRepository.addProduct(finalProduct)
            _operationStatus.value = result
            if (result.isSuccess) {
                val action = if (existingProduct != null) "Cập nhật tồn kho cho" else "Đã nhập"
                _managerMessage.value = "$action ${catalogProduct.name} vào kho"
            } else {
                _managerMessage.value = "Lỗi nhập kho: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }
}
