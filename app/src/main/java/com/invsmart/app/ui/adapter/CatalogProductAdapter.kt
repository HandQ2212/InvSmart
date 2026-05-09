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
import com.invsmart.app.databinding.ItemStaffProductBinding
import com.invsmart.app.util.VndFormatter

class CatalogProductAdapter(
    private val onItemClick: (Product) -> Unit = {}
) : ListAdapter<Product, CatalogProductAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStaffProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    class ViewHolder(private val binding: ItemStaffProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvName.text = product.name
            binding.tvPrice.text = "${VndFormatter.format(product.price)} / ${product.unit}"
            binding.tvStock.text = "Sản phẩm danh mục"
            binding.tvQuantity.text = "0"

            // Hide selection controls for catalog view
            binding.btnPlus.visibility = View.GONE
            binding.btnMinus.visibility = View.GONE
            binding.tvQuantity.visibility = View.GONE

            Glide.with(binding.ivProduct.context)
                .load(product.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.ic_menu_gallery)
                .into(binding.ivProduct)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.sku == newItem.sku
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }
}