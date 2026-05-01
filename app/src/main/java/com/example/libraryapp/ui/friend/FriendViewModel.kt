package com.example.libraryapp.ui.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendViewModel : ViewModel() {

    private val repo = FriendRepository()

    // ── Danh sách bạn bè ─────────────────────────────────────────────────────
    val friends: StateFlow<List<User>> = repo.getFriendsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Lời mời đến ──────────────────────────────────────────────────────────
    val friendRequests: StateFlow<List<User>> = repo.getFriendRequestsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Tìm kiếm ─────────────────────────────────────────────────────────────
    sealed class SearchState {
        object Idle : SearchState()
        object Loading : SearchState()
        data class Found(val user: User, val status: RelationStatus) : SearchState()
        object NotFound : SearchState()
        data class Error(val message: String) : SearchState()
    }

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    fun searchByEmail(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val user = repo.searchUserByEmail(email)
                if (user == null) {
                    _searchState.value = SearchState.NotFound
                } else {
                    val status = repo.getRelationStatus(user.uid)
                    _searchState.value = SearchState.Found(user, status)
                }
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun clearSearch() { _searchState.value = SearchState.Idle }

    // ── Actions ───────────────────────────────────────────────────────────────
    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult

    fun sendRequest(toId: String) = launchAction("Đã gửi lời mời!") {
        repo.sendFriendRequest(toId)
        // Refresh trạng thái kết quả tìm kiếm
        val current = _searchState.value
        if (current is SearchState.Found) {
            _searchState.value = current.copy(status = RelationStatus.REQUEST_SENT)
        }
    }

    fun acceptRequest(fromId: String) = launchAction("Đã chấp nhận lời mời!") {
        repo.acceptFriendRequest(fromId)
    }

    fun declineRequest(fromId: String) = launchAction("Đã từ chối lời mời.") {
        repo.declineFriendRequest(fromId)
    }

    fun cancelRequest(toId: String) = launchAction("Đã hủy lời mời.") {
        repo.cancelFriendRequest(toId)
        val current = _searchState.value
        if (current is SearchState.Found) {
            _searchState.value = current.copy(status = RelationStatus.NONE)
        }
    }

    fun removeFriend(friendId: String) = launchAction("Đã hủy kết bạn.") {
        repo.removeFriend(friendId)
    }

    fun consumeActionResult() { _actionResult.value = null }

    private fun launchAction(successMsg: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                _actionResult.value = successMsg
            } catch (e: Exception) {
                _actionResult.value = "Lỗi: ${e.message}"
            }
        }
    }
}