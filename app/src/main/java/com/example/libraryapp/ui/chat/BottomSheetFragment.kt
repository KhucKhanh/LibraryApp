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

        viewModel = ViewModelProvider(requireActivity())[ChatViewModel::class.java]

        chatAdapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(context)
        binding.rvChat.adapter = chatAdapter

        chatListAdapter = ChatListAdapter(chatList) { chat ->
            openChat(chat.id)
        }
        binding.rvChatList.layoutManager = LinearLayoutManager(context)
        binding.rvChatList.adapter = chatListAdapter

        chatId = getSavedChatId()
        loadHistory()

        binding.btnMinimize.setOnClickListener { dismiss() }

        binding.btnNewChat.setOnClickListener {
            startNewChat()
        }

        binding.btnHistory.setOnClickListener {
            showHistoryPanel()
        }

        binding.btnBackToChat.setOnClickListener {
            showChatPanel()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.edtMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val isFirstMessage = messages.isEmpty()
            val contextSnapshot = AIContextManager.snapshot()
            val history = messages.toList()

            messages.add(Message(text, true))
            chatAdapter.notifyItemInserted(messages.size - 1)
            binding.rvChat.scrollToPosition(messages.size - 1)
            binding.edtMessage.text.clear()

            viewModel.buildAndSend(
                contextSnapshot = contextSnapshot,
                text = text,
                userId = userId,
                chatId = chatId,
                isFirstMessage = isFirstMessage,
                history = history
            ) { reply ->
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    messages.add(Message(reply, false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            }

            android.util.Log.d("CHAT_DEBUG", "Send clicked | text=$text")
            android.util.Log.d("CHAT_DEBUG", "Screen=${contextSnapshot.screen} | book=${contextSnapshot.book?.title}")

        }
    }

    private fun openChat(id: String) {
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

        viewModel.loadChatList(userId) { chats ->
            if (!isAdded || _binding == null) return@loadChatList
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
            if (!isAdded || _binding == null) return@loadChatHistory
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