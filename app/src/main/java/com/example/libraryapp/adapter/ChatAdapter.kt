package com.example.libraryapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.R
import com.example.libraryapp.model.Message

class ChatAdapter(
    private val messages: MutableList<Message>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]

        holder.txtMessage.text = msg.text

        if (msg.isUser) {
            holder.txtMessage.setBackgroundColor(Color.parseColor("#1976D2"))
            holder.txtMessage.setTextColor(Color.WHITE)
        } else {
            holder.txtMessage.setBackgroundColor(Color.LTGRAY)
            holder.txtMessage.setTextColor(Color.BLACK)
        }
    }

    override fun getItemCount(): Int = messages.size
}