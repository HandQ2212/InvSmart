package com.invsmart.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.Chain
import com.invsmart.app.databinding.ItemChainBinding

class ChainAdapter(
    private val onChainClick: (Chain) -> Unit,
    private val onDeleteClick: (Chain) -> Unit
) : ListAdapter<Chain, ChainAdapter.ChainViewHolder>(ChainDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChainViewHolder {
        val binding = ItemChainBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChainViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChainViewHolder(private val binding: ItemChainBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chain: Chain) {
            binding.tvChainName.text = chain.name
            binding.tvMasterUid.text = if (chain.masterUid.isEmpty()) "Master: (Chưa gán)" else "Master: ${chain.masterUid}"
            
            binding.root.setOnClickListener { onChainClick(chain) }
            binding.btnDeleteChain.setOnClickListener { onDeleteClick(chain) }
        }
    }

    class ChainDiffCallback : DiffUtil.ItemCallback<Chain>() {
        override fun areItemsTheSame(oldItem: Chain, newItem: Chain): Boolean = oldItem.chainId == newItem.chainId
        override fun areContentsTheSame(oldItem: Chain, newItem: Chain): Boolean = oldItem == newItem
    }
}
