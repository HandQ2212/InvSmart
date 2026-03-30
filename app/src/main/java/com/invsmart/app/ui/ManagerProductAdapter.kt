package com.invsmart.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.ItemManagerProductBinding
import com.invsmart.app.util.VndFormatter

class ManagerProductAdapter(
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : ListAdapter<Product, ManagerProductAdapter.ProductViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemManagerProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemManagerProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "${VndFormatter.format(product.price)} / ${product.unit}"
            binding.tvStock.text = "Tồn kho: ${product.stockQuantity}"
            binding.tvSku.text = "SKU: ${product.sku}"

            Glide.with(binding.ivProduct.context)
                .load(product.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivProduct)

            binding.btnEdit.setOnClickListener { onEditClick(product) }
            binding.btnDelete.setOnClickListener { onDeleteClick(product) }
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
