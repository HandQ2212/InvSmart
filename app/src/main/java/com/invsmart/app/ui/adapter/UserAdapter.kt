package com.invsmart.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.User
import com.invsmart.app.databinding.ItemUserBinding

class UserAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvUserEmail.text = user.email
            binding.tvUserName.text = user.fullName.ifEmpty { "Người dùng mới" }
            
            val roleDisplay = when(user.roleGlobal) {
                "admin" -> "Admin"
                "master" -> "Chủ chuỗi"
                "manager" -> "Quản lý"
                "unassigned" -> "Chưa gán"
                else -> "Nhân viên"
            }
            binding.tvRole.text = "Quyền: $roleDisplay"
            
            binding.root.setOnClickListener { onUserClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
