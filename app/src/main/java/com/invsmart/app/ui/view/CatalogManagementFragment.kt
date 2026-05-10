package com.invsmart.app.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.invsmart.app.R
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.FragmentCatalogManagementBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.adapter.CatalogProductAdapter
import com.invsmart.app.ui.viewmodel.ManagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CatalogManagementFragment : Fragment() {

    private var _binding: FragmentCatalogManagementBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()

    private var selectedImageUri: android.net.Uri? = null
    private val pickedImageFlow = kotlinx.coroutines.flow.MutableSharedFlow<android.net.Uri>()
    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            viewLifecycleOwner.lifecycleScope.launch {
                pickedImageFlow.emit(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val actor = mainViewModel.uiState.value.currentUser
        val isMaster = actor?.roleGlobal == "master"

        val adapter = CatalogProductAdapter(
            onEditClick = if (isMaster) { product -> showAddCatalogDialog(product) } else null,
            onDeleteClick = if (isMaster) { product ->
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xóa sản phẩm danh mục")
                    .setMessage("Bạn có chắc muốn xóa ${product.name}? Hành động này không thể hoàn tác.")
                    .setPositiveButton("Xóa") { _, _ -> managerViewModel.deleteProduct(product.productId) }
                    .setNegativeButton("Hủy", null)
                    .show()
            } else null,
            onImportClick = if (!isMaster) { product ->
                showImportStockDialog(product)
            } else null
        )
        binding.rvCatalog.adapter = adapter
        binding.rvCatalog.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    managerViewModel.catalogProducts.collect { products ->
                        adapter.submitList(products)
                    }
                }
                launch {
                    managerViewModel.managerMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
                            managerViewModel.resetMessage()
                        }
                    }
                }
            }
        }

        binding.btnAddCatalogItem.visibility = if (isMaster) View.VISIBLE else View.GONE
        binding.btnAddCatalogItem.setOnClickListener {
            showAddCatalogDialog(null)
        }
    }

    private fun showImportStockDialog(product: Product) {
        val input = TextInputEditText(requireContext())
        input.hint = "Số lượng nhập"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nhập kho: ${product.name}")
            .setMessage("Nhập số lượng sản phẩm muốn nhập vào cửa hàng của bạn.")
            .setView(input)
            .setPositiveButton("Nhập") { _, _ ->
                val qty = input.text.toString().toIntOrNull() ?: 0
                if (qty > 0) {
                    mainViewModel.uiState.value.currentUser?.let { actor ->
                        managerViewModel.importFromCatalog(product, qty, actor)
                    }
                } else {
                    android.widget.Toast.makeText(requireContext(), "Số lượng phải lớn hơn 0", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAddCatalogDialog(productToEdit: Product?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etProductName)
        val etSku = dialogView.findViewById<TextInputEditText>(R.id.etSku)
        val etPrice = dialogView.findViewById<TextInputEditText>(R.id.etPrice)
        val etUnit = dialogView.findViewById<TextInputEditText>(R.id.etUnit)
        val etCategory = dialogView.findViewById<TextInputEditText>(R.id.etCategory)
        val ivProductImage = dialogView.findViewById<android.widget.ImageView>(R.id.ivProductImage)
        val btnPickImage = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickImage)

        selectedImageUri = null // Reset for new dialog

        productToEdit?.let {
            etName.setText(it.name)
            etSku.setText(it.sku)
            etSku.isEnabled = false // Cannot edit SKU
            etPrice.setText(it.price.toString())
            etUnit.setText(it.unit)
            etCategory.setText(it.category)
            if (!it.imageUrl.isNullOrBlank()) {
                Glide.with(requireContext()).load(it.imageUrl).into(ivProductImage)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (productToEdit == null) "Thêm sản phẩm danh mục" else "Sửa sản phẩm danh mục")
            .setView(dialogView)
            .setPositiveButton(if (productToEdit == null) "Thêm" else "Lưu") { _, _ ->
                val name = etName.text.toString()
                val sku = etSku.text.toString()
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val unit = etUnit.text.toString().ifEmpty { "Cái" }
                val category = etCategory.text.toString()

                if (name.isNotEmpty() && sku.isNotEmpty()) {
                    val product = (productToEdit ?: Product()).copy(
                        name = name,
                        sku = sku,
                        price = price,
                        unit = unit,
                        category = category,
                        stockQuantity = 0
                    )
                    val pbUpload = dialogView.findViewById<android.widget.ProgressBar>(R.id.pbUpload)
                    pbUpload.visibility = View.VISIBLE
                    
                    mainViewModel.uiState.value.currentUser?.let { actor ->
                        if (productToEdit == null) {
                            managerViewModel.addCatalogProduct(product, actor, selectedImageUri)
                        } else {
                            managerViewModel.updateCatalogProduct(product, actor, selectedImageUri)
                        }
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .create()

        // Observe picked images to update preview
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pickedImageFlow.collect { uri ->
                    if (dialog.isShowing) {
                        Glide.with(requireContext()).load(uri).into(ivProductImage)
                    }
                }
            }
        }

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
