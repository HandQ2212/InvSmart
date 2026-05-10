package com.invsmart.app.ui.adapter

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.ItemManagerProductBinding
import com.invsmart.app.util.VndFormatter

class CatalogProductAdapter(
    private val onEditClick: ((Product) -> Unit)? = null,
    private val onDeleteClick: ((Product) -> Unit)? = null,
    private val onImportClick: ((Product) -> Unit)? = null,
    private val onItemClick: ((Product) -> Unit)? = null
) : ListAdapter<Product, CatalogProductAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManagerProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onEditClick, onDeleteClick, onImportClick, onItemClick)
    }

    class ViewHolder(private val binding: ItemManagerProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            product: Product,
            onEdit: ((Product) -> Unit)?,
            onDelete: ((Product) -> Unit)?,
            onImport: ((Product) -> Unit)?,
            onItem: ((Product) -> Unit)?
        ) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "${VndFormatter.format(product.price)} / ${product.unit}"
            binding.tvStock.text = "Sản phẩm danh mục"
            binding.tvSku.text = "SKU: ${product.sku}"

            binding.btnEdit.visibility = if (onEdit != null) View.VISIBLE else View.GONE
            binding.btnDelete.visibility = if (onDelete != null) View.VISIBLE else View.GONE
            binding.btnImport.visibility = if (onImport != null) View.VISIBLE else View.GONE

            binding.btnEdit.setOnClickListener { onEdit?.invoke(product) }
            binding.btnDelete.setOnClickListener { onDelete?.invoke(product) }
            binding.btnImport.setOnClickListener { onImport?.invoke(product) }
            binding.root.setOnClickListener { onItem?.invoke(product) }

            Glide.with(binding.ivProduct.context)
                .load(product.imageUrl)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivProduct)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = 
            oldItem.productId == newItem.productId || (oldItem.sku.isNotEmpty() && oldItem.sku == newItem.sku)
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}