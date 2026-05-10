package com.invsmart.app.ui.view

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.invsmart.app.BuildConfig
import com.invsmart.app.R
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.FragmentInventoryManagementBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.ManagerProductAdapter
import com.invsmart.app.ui.viewmodel.ManagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@AndroidEntryPoint
class InventoryManagementFragment : Fragment() {

    private var _binding: FragmentInventoryManagementBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()

    private lateinit var productAdapter: ManagerProductAdapter
    private var onImagePickedCallback: ((Uri) -> Unit)? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onImagePickedCallback?.invoke(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        productAdapter = ManagerProductAdapter(
            onEditClick = { product -> showProductDialog(product) },
            onDeleteClick = { product ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Xóa sản phẩm")
                    .setMessage("Bạn có chắc muốn xóa ${product.name}?")
                    .setPositiveButton("Xóa") { _, _ -> managerViewModel.deleteProduct(product.productId.ifEmpty { product.sku }) }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
        }

        binding.fabAddProduct.setOnClickListener {
            showProductDialog(null)
        }

        binding.btnImportCatalog.setOnClickListener {
            showCatalogImportDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.uiState.collect { state ->
                        binding.progressBar.visibility = if (state.isLoadingProducts) View.VISIBLE else View.GONE
                        productAdapter.submitList(state.products)
                    }
                }
                launch {
                    managerViewModel.operationStatus.collect { result ->
                        if (result != null) {
                            if (result.isSuccess) {
                                Toast.makeText(requireContext(), "Thành công", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                            managerViewModel.resetOperationStatus()
                        }
                    }
                }
            }
        }
    }

    private fun showCatalogImportDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_catalog_select, null, false)
        val rvCatalog = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvCatalogSelect)
        
        val adapter = com.invsmart.app.ui.adapter.CatalogProductAdapter(
            onImportClick = { product -> showImportQuantityDialog(product) },
            onItemClick = { product -> showImportQuantityDialog(product) }
        )
        rvCatalog.adapter = adapter
        rvCatalog.layoutManager = LinearLayoutManager(requireContext())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Chọn sản phẩm nhập kho")
            .setView(dialogView)
            .setNegativeButton("Đóng", null)
            .create()

        // Observe catalog productsF
        viewLifecycleOwner.lifecycleScope.launch {
            managerViewModel.catalogProducts.collect { products ->
                android.util.Log.d("INVENTORY_FRAGMENT", "Dialog observing ${products.size} catalog products")
                if (products.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Tìm thấy ${products.size} sản phẩm mẫu", Toast.LENGTH_SHORT).show()
                }
                adapter.submitList(products)
            }
        }

        dialog.show()
    }

    private fun showImportQuantityDialog(product: Product) {
        val input = EditText(requireContext()).apply {
            hint = "Số lượng nhập"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Nhập ${product.name}")
            .setMessage("Nhập số lượng hàng vào kho:")
            .setView(input)
            .setPositiveButton("Nhập") { _, _ ->
                val qty = input.text.toString().toIntOrNull() ?: 0
                if (qty > 0) {
                    mainViewModel.uiState.value.currentUser?.let { actor ->
                        managerViewModel.importFromCatalog(product, qty, actor)
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showProductDialog(productToEdit: Product?) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val edtSku = EditText(requireContext()).apply { 
            hint = "SKU"
            setText(productToEdit?.sku ?: "")
            isEnabled = productToEdit == null // Can't edit SKU of existing product
        }
        val edtName = EditText(requireContext()).apply { 
            hint = "Tên sản phẩm"
            setText(productToEdit?.name ?: "")
            isEnabled = productToEdit == null // Manager can't change name of existing (imported) product
        }
        val edtPrice = EditText(requireContext()).apply { 
            hint = "Giá (đ)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(productToEdit?.price?.toString() ?: "")
            isEnabled = productToEdit == null // Manager can't change price of existing (imported) product
        }
        val edtStock = EditText(requireContext()).apply { 
            hint = "Tồn kho"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(productToEdit?.stockQuantity?.toString() ?: "0")
        }
        var selectedImageUri: Uri? = null
        val ivImagePreview = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            maxHeight = 500
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
        }
        val btnPickImage = Button(requireContext()).apply {
            text = "Chọn ảnh"
        }
        val tvImageStatus = TextView(requireContext()).apply {
            val hasExistingImage = !productToEdit?.imageUrl.isNullOrBlank()
            text = if (hasExistingImage) {
                "Ảnh hiện tại: Đã có ảnh trên Cloudinary"
            } else {
                "Chưa chọn ảnh"
            }
        }

        layout.addView(edtSku)
        layout.addView(edtName)
        layout.addView(edtPrice)
        layout.addView(edtStock)
        layout.addView(ivImagePreview)
        layout.addView(btnPickImage)
        layout.addView(tvImageStatus)

        val existingImageUrl = productToEdit?.imageUrl
        if (!existingImageUrl.isNullOrBlank()) {
            ivImagePreview.visibility = View.VISIBLE
            Glide.with(this)
                .load(existingImageUrl)
                .into(ivImagePreview)
        }

        btnPickImage.setOnClickListener {
            onImagePickedCallback = { uri ->
                selectedImageUri = uri
                tvImageStatus.text = "Đã chọn ảnh mới"
                ivImagePreview.visibility = View.VISIBLE
                Glide.with(this)
                    .load(uri)
                    .into(ivImagePreview)
            }
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (productToEdit == null) "Thêm sản phẩm" else "Sửa sản phẩm")
            .setView(layout)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val inputSku = edtSku.text.toString().trim()
                val name = edtName.text.toString().trim()
                val price = edtPrice.text.toString().toDoubleOrNull() ?: 0.0
                val stock = edtStock.text.toString().toIntOrNull() ?: 0

                val sku = if (productToEdit == null) {
                    inputSku
                } else {
                    inputSku.ifBlank {
                        productToEdit.sku.ifBlank { productToEdit.productId }
                    }
                }

                val hasEnoughInfo = if (productToEdit == null) {
                    sku.isNotEmpty() && name.isNotEmpty()
                } else {
                    name.isNotEmpty()
                }

                if (!hasEnoughInfo) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                fun submitProduct(finalImageUrl: String?) {
                    val p = Product(
                        productId = productToEdit?.productId ?: sku,
                        teamId = "",
                        sku = sku,
                        name = name,
                        category = productToEdit?.category ?: "",
                        stockQuantity = stock,
                        price = price,
                        unit = productToEdit?.unit ?: "Cái",
                        imageUrl = finalImageUrl
                    )
                    val currentUser = mainViewModel.uiState.value.currentUser
                    if (productToEdit == null) {
                        if (currentUser == null) {
                            Toast.makeText(requireContext(), "Không tìm thấy tài khoản quản lý", Toast.LENGTH_SHORT).show()
                            return
                        }
                        managerViewModel.addProduct(p, currentUser)
                    } else {
                        if (currentUser == null) {
                            Toast.makeText(requireContext(), "Không tìm thấy tài khoản quản lý", Toast.LENGTH_SHORT).show()
                            return
                        }
                        managerViewModel.updateProduct(p, currentUser)
                    }
                    dialog.dismiss()
                }

                if (selectedImageUri == null) {
                    submitProduct(existingImageUrl)
                    return@setOnClickListener
                }

                if (BuildConfig.CLOUDINARY_UPLOAD_PRESET.isBlank()) {
                    Toast.makeText(requireContext(), "Thiếu cấu hình CLOUDINARY_UPLOAD_PRESET trong local.properties", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                saveButton.isEnabled = false
                saveButton.text = "Đang tải ảnh..."
                viewLifecycleOwner.lifecycleScope.launch {
                    val uploadResult = uploadImageToCloudinary(selectedImageUri!!)
                    saveButton.isEnabled = true
                    saveButton.text = "Lưu"

                    uploadResult.onSuccess { cloudinaryUrl ->
                        submitProduct(cloudinaryUrl)
                    }.onFailure {
                        Toast.makeText(
                            requireContext(),
                            "Upload ảnh thất bại: ${it.localizedMessage ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            var requestId: String? = null

            try {
                requestId = MediaManager.get()
                    .upload(imageUri)
                    .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                    .option("folder", "invsmart/products")
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String?) = Unit

                        override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) = Unit

                        override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                            val secureUrl = resultData?.get("secure_url")?.toString()
                            if (!secureUrl.isNullOrBlank()) {
                                continuation.resume(Result.success(secureUrl))
                            } else {
                                continuation.resume(Result.failure(IllegalStateException("Cloudinary không trả về secure_url")))
                            }
                        }

                        override fun onError(requestId: String?, error: ErrorInfo?) {
                            continuation.resume(Result.failure(IllegalStateException(error?.description ?: "Cloudinary upload error")))
                        }

                        override fun onReschedule(requestId: String?, error: ErrorInfo?) = Unit
                    })
                    .dispatch()
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }

            continuation.invokeOnCancellation {
                requestId?.let { id ->
                    runCatching { MediaManager.get().cancelRequest(id) }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onImagePickedCallback = null
        _binding = null
    }
}