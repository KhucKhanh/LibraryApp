package com.example.libraryapp.ui.chat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.R
import com.example.libraryapp.adapter.ChatAdapter
import com.example.libraryapp.model.Message

class ChatActivity : AppCompatActivity() {

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        
        setContentView(R.layout.activity_chat)

        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        adapter = ChatAdapter(messages)

        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString()

            if (text.isNotEmpty()) {

                // user message
                messages.add(Message(text, true))
                adapter.notifyItemInserted(messages.size - 1)

                // fake AI reply
                val reply = "AI: Mình đã nhận được '$text'"

                messages.add(Message(reply, false))
                adapter.notifyItemInserted(messages.size - 1)

                edtMessage.text.clear()
                rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }
}