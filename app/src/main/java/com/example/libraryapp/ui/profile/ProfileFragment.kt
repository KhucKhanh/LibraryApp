package com.example.libraryapp.ui.profile

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import com.example.libraryapp.R
import com.example.libraryapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        loadStats(userId)

        val docRef = db.collection("users").document(userId)

        docRef.get(com.google.firebase.firestore.Source.CACHE)
            .addOnSuccessListener { doc ->
                val displayName = doc.getString("displayName") ?: user.email ?: ""
                val avatarUrl = doc.getString("avatarUrl") ?: ""
                binding.txtEmail.text = displayName
                if (avatarUrl.isNotEmpty()) {
                    Glide.with(this).load(avatarUrl).circleCrop().into(binding.imgAvatar)
                }
            }

        docRef.get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                val displayName = doc.getString("displayName") ?: user.email ?: ""
                val avatarUrl = doc.getString("avatarUrl") ?: ""
                binding.txtEmail.text = displayName
                if (avatarUrl.isNotEmpty()) {
                    Glide.with(this).load(avatarUrl).circleCrop().into(binding.imgAvatar)
                }
            }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }

        binding.btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_forgotPasswordFragment)
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

    }

    private fun loadStats(userId: String) {
        db.collection("users")
            .document(userId)
            .collection("readingProgress")
            .get()
            .addOnSuccessListener { progressDocs ->

                var countReading = 0
                var countRead = 0
                val total = progressDocs.size()

                if (total == 0) {
                    updateStatsUI(0, 0)
                    return@addOnSuccessListener
                }

                var processed = 0

                for (doc in progressDocs) {
                    val bookId = doc.id

                    db.collection("books")
                        .document(bookId)
                        .collection("chapters")
                        .orderBy("order", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { chapterDocs ->
                            val lastOrder = chapterDocs.documents.firstOrNull()?.getLong("order")

                            val scrollKey = "chapter_${lastOrder}_scrollY"
                            val scrollY = doc.getLong(scrollKey) ?: 0L

                            if (scrollY > 0) countRead++ else countReading++

                            processed++
                            if (processed == total) updateStatsUI(countReading, countRead)
                        }
                        .addOnFailureListener {
                            countReading++
                            processed++
                            if (processed == total) updateStatsUI(countReading, countRead)
                        }
                }
            }
    }

    private fun updateStatsUI(reading: Int, read: Int) {
        binding.txtCountReading.text = reading.toString()
        binding.txtCountRead.text = read.toString()
    }
}