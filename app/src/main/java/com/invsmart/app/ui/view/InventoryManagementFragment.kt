package com.invsmart.app.ui.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.invsmart.app.R
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.FragmentInventoryManagementBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.ManagerProductAdapter
import com.invsmart.app.ui.viewmodel.ManagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InventoryManagementFragment : Fragment() {

    private var _binding: FragmentInventoryManagementBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val managerViewModel: ManagerViewModel by activityViewModels()
    
    private lateinit var productAdapter: ManagerProductAdapter

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
        }
        val edtPrice = EditText(requireContext()).apply { 
            hint = "Giá"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(productToEdit?.price?.toString() ?: "")
        }
        val edtStock = EditText(requireContext()).apply { 
            hint = "Tồn kho"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(productToEdit?.stockQuantity?.toString() ?: "0")
        }
        val edtImageUrl = EditText(requireContext()).apply {
            hint = "Link ảnh (Cloudinary URL)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(productToEdit?.imageUrl ?: "")
        }

        layout.addView(edtSku)
        layout.addView(edtName)
        layout.addView(edtPrice)
        layout.addView(edtStock)
        layout.addView(edtImageUrl)

        AlertDialog.Builder(requireContext())
            .setTitle(if (productToEdit == null) "Thêm sản phẩm" else "Sửa sản phẩm")
            .setView(layout)
            .setPositiveButton("Lưu") { _, _ ->
                val inputSku = edtSku.text.toString().trim()
                val name = edtName.text.toString().trim()
                val price = edtPrice.text.toString().toDoubleOrNull() ?: 0.0
                val stock = edtStock.text.toString().toIntOrNull() ?: 0
                val imageUrl = edtImageUrl.text.toString().trim().ifBlank { null }

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

                if (hasEnoughInfo) {
                    val p = Product(
                        productId = productToEdit?.productId ?: sku,
                        teamId = "",
                        sku = sku,
                        name = name,
                        category = productToEdit?.category ?: "",
                        stockQuantity = stock,
                        price = price,
                        unit = productToEdit?.unit ?: "Cái",
                        imageUrl = imageUrl
                    )
                    val currentUser = mainViewModel.uiState.value.currentUser
                    if (productToEdit == null) {
                        if (currentUser == null) {
                            Toast.makeText(requireContext(), "Không tìm thấy tài khoản quản lý", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        managerViewModel.addProduct(p, currentUser.uid)
                    } else {
                        managerViewModel.updateProduct(p)
                    }
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}