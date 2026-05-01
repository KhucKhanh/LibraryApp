package com.example.libraryapp.ui.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.libraryapp.R
import com.example.libraryapp.databinding.FragmentFriendBinding
import kotlinx.coroutines.launch

class FriendFragment : Fragment() {

    private var _binding: FragmentFriendBinding? = null
    private val binding get() = _binding!!

    private val vm: FriendViewModel by viewModels()

    private lateinit var friendAdapter: FriendAdapter
    private lateinit var requestAdapter: FriendRequestAdapter

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupSearch()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupAdapters() {
        friendAdapter = FriendAdapter(
            onRemove = { user ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Hủy kết bạn")
                    .setMessage("Bạn có chắc muốn hủy kết bạn với ${user.displayName}?")
                    .setPositiveButton("Hủy kết bạn") { _, _ -> vm.removeFriend(user.uid) }
                    .setNegativeButton("Không", null)
                    .show()
            }
        )
        binding.rvFriends.adapter = friendAdapter

        requestAdapter = FriendRequestAdapter(
            onAccept = { vm.acceptRequest(it.uid) },
            onDecline = { vm.declineRequest(it.uid) }
        )
        binding.rvRequests.adapter = requestAdapter
    }

    private fun setupSearch() {
        // Nút tìm kiếm mở/đóng panel
        binding.btnSearch.setOnClickListener {
            val isVisible = binding.searchPanel.isVisible
            binding.searchPanel.isVisible = !isVisible
            if (isVisible) {
                binding.etEmail.text?.clear()
                vm.clearSearch()
            }
        }

        // Tìm kiếm khi nhấn Done trên bàn phím
        binding.etEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                vm.searchByEmail(binding.etEmail.text.toString())
                true
            } else false
        }

        binding.btnDoSearch.setOnClickListener {
            vm.searchByEmail(binding.etEmail.text.toString())
        }
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Danh sách bạn bè
                launch {
                    vm.friends.collect { list ->
                        friendAdapter.submitList(list)
                        binding.tvFriendCount.text = "Bạn bè (${list.size})"
                        binding.tvEmptyFriends.isVisible = list.isEmpty()
                        binding.rvFriends.isVisible = list.isNotEmpty()
                    }
                }

                // Lời mời kết bạn
                launch {
                    vm.friendRequests.collect { list ->
                        requestAdapter.submitList(list)
                        val hasRequests = list.isNotEmpty()
                        binding.sectionRequests.isVisible = hasRequests
                        binding.tvRequestBadge.text = list.size.toString()
                        binding.tvRequestBadge.isVisible = hasRequests
                    }
                }

                // Kết quả tìm kiếm
                launch {
                    vm.searchState.collect { state ->
                        renderSearchState(state)
                    }
                }

                // Toast thông báo action
                launch {
                    vm.actionResult.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            vm.consumeActionResult()
                        }
                    }
                }
            }
        }
    }

    // ── Render search result ──────────────────────────────────────────────────

    private fun renderSearchState(state: FriendViewModel.SearchState) = with(binding) {
        searchResultCard.isVisible = false
        progressSearch.isVisible = false
        tvSearchEmpty.isVisible = false

        when (state) {
            is FriendViewModel.SearchState.Idle -> Unit

            is FriendViewModel.SearchState.Loading -> {
                progressSearch.isVisible = true
            }

            is FriendViewModel.SearchState.NotFound -> {
                tvSearchEmpty.isVisible = true
                tvSearchEmpty.text = "Không tìm thấy người dùng với email này"
            }

            is FriendViewModel.SearchState.Error -> {
                tvSearchEmpty.isVisible = true
                tvSearchEmpty.text = state.message
            }

            is FriendViewModel.SearchState.Found -> {
                searchResultCard.isVisible = true
                val user = state.user

                tvResultName.text = user.displayName
                tvResultEmail.text = user.email
                Glide.with(ivResultAvatar)
                    .load(user.avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(ivResultAvatar)

                // Hiện đúng button theo trạng thái quan hệ
                btnAddFriend.isVisible = false
                btnCancelRequest.isVisible = false
                btnAlreadyFriend.isVisible = false
                btnAcceptFromSearch.isVisible = false

                when (state.status) {
                    RelationStatus.NONE -> {
                        btnAddFriend.isVisible = true
                        btnAddFriend.setOnClickListener { vm.sendRequest(user.uid) }
                    }
                    RelationStatus.REQUEST_SENT -> {
                        btnCancelRequest.isVisible = true
                        btnCancelRequest.setOnClickListener { vm.cancelRequest(user.uid) }
                    }
                    RelationStatus.REQUEST_RECEIVED -> {
                        btnAcceptFromSearch.isVisible = true
                        btnAcceptFromSearch.setOnClickListener { vm.acceptRequest(user.uid) }
                    }
                    RelationStatus.FRIEND -> {
                        btnAlreadyFriend.isVisible = true
                    }
                }
            }
        }
    }
}