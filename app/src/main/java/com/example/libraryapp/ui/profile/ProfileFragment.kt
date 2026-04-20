package com.example.libraryapp.ui.profile

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import com.example.libraryapp.R
import com.example.libraryapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val user = FirebaseAuth.getInstance().currentUser

        // ✅ Hiển thị email
        binding.txtEmail.text = user?.email ?: "No email"

        // 🔥 Logout
        binding.btnLogout.setOnClickListener {

            FirebaseAuth.getInstance().signOut()

            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // xoá toàn bộ stack
                .build()

            findNavController().navigate(
                R.id.loginFragment,
                null,
                navOptions
            )
        }
    }
}