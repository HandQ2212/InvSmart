package com.invsmart.app.ui

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.invsmart.app.data.model.User
import com.invsmart.app.databinding.ItemUserBinding

class UserAdapter(
    private val canToggleRole: (User) -> Boolean,
    private val onChangeRoleClick: (User) -> Unit
) : ListAdapter<User, UserAdapter.UserViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            val role = user.roleGlobal.ifBlank { user.role.ifBlank { "staff" } }
            val roleDisplay = when(role) {
                "admin" -> "Admin"
                "master" -> "Chủ chuỗi"
                "manager" -> "Quản lý"
                "unassigned" -> "Chưa gán"
                else -> "Nhân viên"
            }
            binding.tvUserEmail.text = user.email
            binding.tvRole.text = "Quyền: $roleDisplay"

            val canChange = canToggleRole(user) && (role == "manager" || role == "staff")
            binding.btnChangeRole.visibility = if (canChange) View.VISIBLE else View.GONE

            if (canChange) {
                binding.btnChangeRole.text = if (role == "manager") "Hạ xuống Staff" else "Nâng lên Manager"
                binding.btnChangeRole.setOnClickListener {
                    onChangeRoleClick(user)
                }
            } else {
                binding.btnChangeRole.setOnClickListener(null)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
