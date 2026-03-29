package com.invsmart.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.ItemStaffProductBinding

class StaffProductAdapter(
    private val onQuantityChanged: (Product, Int) -> Unit
) : ListAdapter<Product, StaffProductAdapter.ProductViewHolder>(DiffCallback) {

    private val selectedQuantities = mutableMapOf<String, Int>()

    fun setSelections(selections: Map<Product, Int>) {
        selectedQuantities.clear()
        selections.forEach { (p, qty) ->
            selectedQuantities[p.sku] = qty
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemStaffProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        val currentQty = selectedQuantities[product.sku] ?: 0
        holder.bind(product, currentQty)
    }

    inner class ProductViewHolder(
        private val binding: ItemStaffProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product, quantity: Int) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "${product.price} / ${product.unit}"
            binding.tvStock.text = "Tồn kho: ${product.stockQuantity}"
            binding.tvQuantity.text = quantity.toString()

            Glide.with(binding.ivProduct.context)
                .load(product.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivProduct)

            binding.btnPlus.setOnClickListener {
                val newQty = quantity + 1
                // Assuming we can't select more than stock if it's EXPORT, but here we just leave it to business logic
                if (newQty <= product.stockQuantity) {
                    onQuantityChanged(product, newQty)
                }
            }

            binding.btnMinus.setOnClickListener {
                if (quantity > 0) {
                    val newQty = quantity - 1
                    onQuantityChanged(product, newQty)
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.sku == newItem.sku
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
