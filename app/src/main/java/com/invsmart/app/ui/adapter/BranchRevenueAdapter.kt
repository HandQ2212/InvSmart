package com.invsmart.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.Store
import com.invsmart.app.databinding.ItemBranchRevenueBinding
import com.invsmart.app.util.VndFormatter

class BranchRevenueAdapter : RecyclerView.Adapter<BranchRevenueAdapter.ViewHolder>() {

    private var stores: List<Store> = emptyList()
    private var revenueMap: Map<String, Double> = emptyMap()

    fun submitData(stores: List<Store>, revenueMap: Map<String, Double>) {
        this.stores = stores
        this.revenueMap = revenueMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBranchRevenueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        holder.bind(store, revenueMap[store.storeId] ?: 0.0)
    }

    override fun getItemCount(): Int = stores.size

    class ViewHolder(private val binding: ItemBranchRevenueBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(store: Store, revenue: Double) {
            binding.tvBranchName.text = store.name
            binding.tvBranchAddress.text = store.address
            binding.tvBranchRevenue.text = VndFormatter.format(revenue)
        }
    }
}