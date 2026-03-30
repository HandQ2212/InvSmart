package com.invsmart.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.OrderItem
import com.invsmart.app.databinding.ItemOrderDetailBinding
import com.invsmart.app.util.VndFormatter

class OrderItemHistoryAdapter : ListAdapter<OrderItem, OrderItemHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemOrderDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ItemOrderDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderItem) {
            val lineTotal = item.priceAtTime * item.quantity
            binding.tvProductName.text = item.productName
            binding.tvUnitPrice.text = "Đơn giá: ${VndFormatter.format(item.priceAtTime)}"
            binding.tvQuantity.text = "Số lượng: ${item.quantity}"
            binding.tvProvisionalPrice.text = VndFormatter.format(lineTotal)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
            return oldItem == newItem
        }
    }
}
