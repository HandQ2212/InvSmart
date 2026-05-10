package com.invsmart.app.ui.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.invsmart.app.databinding.FragmentProductListBinding
import com.invsmart.app.ui.MainViewModel
import com.invsmart.app.ui.StaffProductAdapter
import com.invsmart.app.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val staffViewModel: StaffViewModel by activityViewModels() // Shared with OrderDetail
    
    private lateinit var productAdapter: StaffProductAdapter
    private var allProducts: List<Product> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var selectionsMap: Map<Product, Int> = emptyMap()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        productAdapter = StaffProductAdapter { product, qty ->
            staffViewModel.setProductQuantity(product, qty)
        }

        binding.rvProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            // Disable animations to further stabilize focus if needed
            itemAnimator = null
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                refreshProducts()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.fabContinue.setOnClickListener {
            if (staffViewModel.selectedProducts.value.isEmpty()) {
                Toast.makeText(requireContext(), "Chưa chọn sản phẩm nào", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(R.id.action_productListFragment_to_orderDetailFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.uiState.collect { state ->
                        binding.progressBar.visibility = if (state.isLoadingProducts) View.VISIBLE else View.GONE
                        allProducts = state.products
                        refreshProducts()
                    }
                }
                launch {
                    staffViewModel.selectedProducts.collect { selections ->
                        selectionsMap = selections
                        productAdapter.setSelections(selections)
                        val totalSelected = selections.values.sum()
                        binding.fabContinue.text = "Tiếp tục ($totalSelected)"
                        refreshProducts()
                    }
                }
            }
        }
    }

    private fun refreshProducts() {
        val query = binding.edtSearch.text.toString().lowercase()
        val filtered = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter {
                it.name.lowercase().contains(query) || it.sku.lowercase().contains(query)
            }
        }
        
        val selectionList = filtered.map { product ->
            com.invsmart.app.ui.ProductSelection(
                product = product,
                quantity = selectionsMap.entries.find { it.key.productId == product.productId }?.value ?: 0
            )
        }
        productAdapter.submitList(selectionList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}