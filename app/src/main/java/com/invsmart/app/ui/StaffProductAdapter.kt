package com.invsmart.app.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.ItemStaffProductBinding
import com.invsmart.app.util.VndFormatter

data class ProductSelection(
    val product: Product,
    val quantity: Int
)

class StaffProductAdapter(
    private val onQuantityChanged: (Product, Int) -> Unit
) : ListAdapter<ProductSelection, StaffProductAdapter.ProductViewHolder>(DiffCallback) {

    private val selectedQuantities = mutableMapOf<String, Int>()

    fun setSelections(selections: Map<Product, Int>) {
        selectedQuantities.clear()
        selections.forEach { (p, qty) ->
            selectedQuantities[p.sku] = qty
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemStaffProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val selection = getItem(position)
        holder.bind(selection.product, selection.quantity)
    }

    inner class ProductViewHolder(
        private val binding: ItemStaffProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: android.text.TextWatcher? = null

        fun bind(product: Product, quantity: Int) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "${VndFormatter.format(product.price)} / ${product.unit}"
            binding.tvStock.text = "Tồn kho: ${product.stockQuantity}"
            
            // Remove old watcher before setting text
            textWatcher?.let { binding.etQuantity.removeTextChangedListener(it) }
            
            val qtyStr = quantity.toString()
            if (binding.etQuantity.text.toString() != qtyStr) {
                binding.etQuantity.setText(qtyStr)
            }

            Glide.with(binding.ivProduct.context)
                .load(product.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivProduct)

            binding.btnPlus.setOnClickListener {
                val current = binding.etQuantity.text.toString().toIntOrNull() ?: 0
                val newQty = current + 1
                if (newQty <= product.stockQuantity) {
                    onQuantityChanged(product, newQty)
                } else {
                    android.widget.Toast.makeText(binding.root.context, "Vượt quá tồn kho!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnMinus.setOnClickListener {
                val current = binding.etQuantity.text.toString().toIntOrNull() ?: 0
                if (current > 0) {
                    val newQty = current - 1
                    onQuantityChanged(product, newQty)
                }
            }

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val inputString = s.toString()
                    val input = inputString.toIntOrNull() ?: 0
                    if (input < 0) {
                        binding.etQuantity.setText("0")
                        onQuantityChanged(product, 0)
                    } else if (input > product.stockQuantity) {
                        Toast.makeText(binding.root.context, "Chỉ còn ${product.stockQuantity} sản phẩm!", Toast.LENGTH_SHORT).show()
                        binding.etQuantity.setText(product.stockQuantity.toString())
                        binding.etQuantity.setSelection(binding.etQuantity.text.length)
                        onQuantityChanged(product, product.stockQuantity)
                    } else {
                        if (selectedQuantities[product.sku] != input) {
                            onQuantityChanged(product, input)
                        }
                    }
                }
            }
            binding.etQuantity.addTextChangedListener(textWatcher)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductSelection>() {
        override fun areItemsTheSame(oldItem: ProductSelection, newItem: ProductSelection): Boolean {
            return oldItem.product.productId == newItem.product.productId || 
                   (oldItem.product.sku.isNotEmpty() && oldItem.product.sku == newItem.product.sku)
        }

        override fun areContentsTheSame(oldItem: ProductSelection, newItem: ProductSelection): Boolean {
            return oldItem == newItem
        }
    }
}
