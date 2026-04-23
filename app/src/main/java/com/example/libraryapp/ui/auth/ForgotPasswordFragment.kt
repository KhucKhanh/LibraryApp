package com.example.libraryapp.ui.auth

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.libraryapp.databinding.FragmentForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {

    private lateinit var binding: FragmentForgotPasswordBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.btnSendReset.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    binding.txtStatus.visibility = View.VISIBLE
                    binding.txtStatus.setTextColor(
                        android.graphics.Color.parseColor("#388E3C")
                    )
                    binding.txtStatus.text = "Nếu email tồn tại, chúng tôi đã gửi link đặt lại đến $email\n(Kiểm tra cả thư mục Spam)"
                    binding.btnSendReset.isEnabled = false
                }
                .addOnFailureListener { e ->
                    binding.txtStatus.visibility = View.VISIBLE
                    binding.txtStatus.setTextColor(
                        android.graphics.Color.parseColor("#E53935")
                    )
                    binding.txtStatus.text = "Lỗi: ${e.message}"
                }
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}