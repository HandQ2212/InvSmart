package com.invsmart.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.Invitation
import com.invsmart.app.databinding.ItemInvitationBinding

class InvitationAdapter(
    private val onAccept: (Invitation) -> Unit,
    private val onReject: (Invitation) -> Unit
) : ListAdapter<Invitation, InvitationAdapter.InvitationViewHolder>(InvitationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        val binding = ItemInvitationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvitationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InvitationViewHolder(private val binding: ItemInvitationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(invitation: Invitation) {
            binding.tvInvitationMessage.text = "Lời mời vào: ${invitation.storeName}"
            binding.tvRoleInfo.text = "Vai trò: ${invitation.targetRole.uppercase()}"
            binding.tvSenderInfo.text = "Từ: ${invitation.senderName}"
            
            binding.btnAccept.setOnClickListener { onAccept(invitation) }
            binding.btnReject.setOnClickListener { onReject(invitation) }
        }
    }

    class InvitationDiffCallback : DiffUtil.ItemCallback<Invitation>() {
        override fun areItemsTheSame(oldItem: Invitation, newItem: Invitation): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Invitation, newItem: Invitation): Boolean = oldItem == newItem
    }
}
