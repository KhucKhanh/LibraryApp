package com.example.libraryapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.adapter.ChatAdapter
import com.example.libraryapp.databinding.ChatBottomSheetBinding
import com.example.libraryapp.model.Message
import com.example.libraryapp.model.MessageRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChatBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ChatBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChatBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ⚠️ init ViewModel (tạm đơn giản)
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(context)
        binding.rvChat.adapter = adapter

        binding.btnSend.setOnClickListener {

            val text = binding.edtMessage.text.toString()
            if (text.isEmpty()) return@setOnClickListener

            // 1. add user message
            messages.add(Message(text, true))
            adapter.notifyItemInserted(messages.size - 1)

            binding.edtMessage.text.clear()

            // 2. convert history → API format
            val requestMessages = messages.map {
                MessageRequest(
                    role = if (it.isUser) "user" else "assistant",
                    content = it.text
                )
            }

            // 3. CALL AI
            viewModel.sendMessage(requestMessages) { reply ->

                messages.add(Message(reply, false))
                adapter.notifyItemInserted(messages.size - 1)

                binding.rvChat.scrollToPosition(messages.size - 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}