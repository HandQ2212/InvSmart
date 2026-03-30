package com.invsmart.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.Product
import com.invsmart.app.databinding.ItemOrderDetailBinding
import com.invsmart.app.util.VndFormatter

class OrderDetailAdapter : ListAdapter<Pair<Product, Int>, OrderDetailAdapter.DetailViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ItemOrderDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DetailViewHolder(
        private val binding: ItemOrderDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pair: Pair<Product, Int>) {
            val (product, quantity) = pair
            val unitPrice = product.price
            val total = unitPrice * quantity

            binding.tvProductName.text = product.name
            binding.tvUnitPrice.text = "Đơn giá: ${VndFormatter.format(unitPrice)}"
            binding.tvQuantity.text = "Số lượng: $quantity"
            binding.tvProvisionalPrice.text = VndFormatter.format(total)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Pair<Product, Int>>() {
        override fun areItemsTheSame(oldItem: Pair<Product, Int>, newItem: Pair<Product, Int>): Boolean {
            return oldItem.first.sku == newItem.first.sku
        }

        override fun areContentsTheSame(oldItem: Pair<Product, Int>, newItem: Pair<Product, Int>): Boolean {
            return oldItem == newItem
        }
    }
}
