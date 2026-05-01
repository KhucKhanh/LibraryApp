package com.example.libraryapp.ui.friend

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.libraryapp.R
import com.example.libraryapp.databinding.ItemFriendRequestBinding

class FriendRequestAdapter(
    private val onAccept: (User) -> Unit,
    private val onDecline: (User) -> Unit
) : ListAdapter<User, FriendRequestAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemFriendRequestBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(user: User) {
            b.tvName.text = user.displayName
            b.tvEmail.text = user.email
            Glide.with(b.ivAvatar)
                .load(user.avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(b.ivAvatar)
            b.btnAccept.setOnClickListener { onAccept(user) }
            b.btnDecline.setOnClickListener { onDecline(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemFriendRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(a: User, b: User) = a.uid == b.uid
            override fun areContentsTheSame(a: User, b: User) = a == b
        }
    }
}