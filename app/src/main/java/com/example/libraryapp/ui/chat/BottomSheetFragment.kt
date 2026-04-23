package com.example.libraryapp.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.libraryapp.adapter.ChatAdapter
import com.example.libraryapp.adapter.ChatListAdapter
import com.example.libraryapp.databinding.ChatBottomSheetBinding
import com.example.libraryapp.model.Chat
import com.example.libraryapp.model.Message
import com.example.libraryapp.model.MessageRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.libraryapp.ai.AIContextManager

class ChatBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ChatBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatViewModel
    private val messages = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatAdapter

    private val chatList = mutableListOf<Chat>()
    private lateinit var chatListAdapter: ChatListAdapter

    private val userId by lazy {
        com.google.firebase.auth.FirebaseAuth.getInstance()
            .currentUser?.uid ?: throw Exception("User not logged in")
    }

    private var chatId: String = ""

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

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // Setup chat RecyclerView
        chatAdapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(context)
        binding.rvChat.adapter = chatAdapter

        // Setup chat list RecyclerView
        chatListAdapter = ChatListAdapter(chatList) { chat ->
            // Bấm vào chat cũ -> mở chat đó
            openChat(chat.id)
        }
        binding.rvChatList.layoutManager = LinearLayoutManager(context)
        binding.rvChatList.adapter = chatListAdapter

        // Load chat hiện tại
        chatId = getSavedChatId()
        loadHistory()

        // Nút thu nhỏ
        binding.btnMinimize.setOnClickListener { dismiss() }

        // Nút new chat
        binding.btnNewChat.setOnClickListener {
            startNewChat()
        }

        // Nút lịch sử
        binding.btnHistory.setOnClickListener {
            showHistoryPanel()
        }

        // Nút back về chat
        binding.btnBackToChat.setOnClickListener {
            showChatPanel()
        }

        // Nút gửi
        binding.btnSend.setOnClickListener {
            val text = binding.edtMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val isFirstMessage = messages.isEmpty()

            messages.add(Message(text, true))
            chatAdapter.notifyItemInserted(messages.size - 1)
            binding.rvChat.scrollToPosition(messages.size - 1)
            binding.edtMessage.text.clear()

            val requestMessages = mutableListOf<MessageRequest>()
            requestMessages.add(
                MessageRequest(role = "system", content = AIContextManager.buildPrompt(""))
            )
            requestMessages.addAll(
                messages.map {
                    MessageRequest(
                        role = if (it.isUser) "user" else "assistant",
                        content = it.text
                    )
                }
            )

            viewModel.sendMessage(
                userId = userId,
                chatId = chatId,
                userText = text,
                messages = requestMessages,
                isFirstMessage = isFirstMessage
            ) { reply ->
                requireActivity().runOnUiThread {
                    messages.add(Message(reply, false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun openChat(id: String) {
        // Lưu chatId mới, load lịch sử, về panel chat
        requireContext()
            .getSharedPreferences("chat_prefs", 0)
            .edit().putString("chat_id", id).apply()

        chatId = id
        messages.clear()
        chatAdapter.notifyDataSetChanged()
        loadHistory()
        showChatPanel()
    }

    private fun startNewChat() {
        val newId = java.util.UUID.randomUUID().toString()
        requireContext()
            .getSharedPreferences("chat_prefs", 0)
            .edit().putString("chat_id", newId).apply()

        chatId = newId
        messages.clear()
        chatAdapter.notifyDataSetChanged()
    }

    private fun showHistoryPanel() {
        binding.panelChat.visibility = View.GONE
        binding.panelHistory.visibility = View.VISIBLE

        // Load danh sách chat
        viewModel.loadChatList(userId) { chats ->
            chatList.clear()
            chatList.addAll(chats)
            chatListAdapter.notifyDataSetChanged()
        }
    }

    private fun showChatPanel() {
        binding.panelHistory.visibility = View.GONE
        binding.panelChat.visibility = View.VISIBLE
    }

    private fun getSavedChatId(): String {
        val prefs = requireContext().getSharedPreferences("chat_prefs", 0)
        val saved = prefs.getString("chat_id", null)
        if (saved != null) return saved
        val newId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("chat_id", newId).apply()
        return newId
    }

    private fun loadHistory() {
        viewModel.loadChatHistory(userId, chatId) { history ->
            messages.addAll(history)
            chatAdapter.notifyDataSetChanged()
            if (messages.isNotEmpty())
                binding.rvChat.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}