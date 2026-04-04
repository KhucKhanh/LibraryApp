package com.example.libraryapp.ui.auth

import androidx.lifecycle.ViewModel
import com.example.libraryapp.data.AuthRepository

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repo.login(email, password, onResult)
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        repo.register(email, password, onResult)
    }

    fun isLoggedIn(): Boolean {
        return repo.getCurrentUser() != null
    }

    fun logout() {
        repo.logout()
    }
}