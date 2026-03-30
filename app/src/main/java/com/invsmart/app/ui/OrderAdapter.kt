package com.invsmart.app.ui

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.Order
import com.invsmart.app.databinding.ItemOrderBinding
import com.invsmart.app.util.VndFormatter
import java.util.Date

class OrderAdapter(
    private val onOrderClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position), onOrderClick)
    }

    class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order, onOrderClick: (Order) -> Unit) {
            binding.tvOrderCode.text = "Mã đơn: ${order.orderId}"
            binding.tvOrderType.text = "Loại: ${if (order.orderType == "import") "Nhập kho" else "Xuất kho"}"
            binding.tvOrderQuantity.text = "Tổng số lượng: ${order.totalQuantity}"
            binding.tvOrderAmount.text = "Tổng tiền: ${VndFormatter.format(order.totalAmount)}"
            binding.tvOrderDate.text = "Ngày: ${DateFormat.format("dd/MM/yyyy HH:mm", order.createdAt.toDate())}"
            binding.root.setOnClickListener { onOrderClick(order) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
            return oldItem == newItem
        }
    }
}
