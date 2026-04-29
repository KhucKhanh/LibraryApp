package com.example.libraryapp.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.libraryapp.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                Glide.with(this).load(selectedImageUri).circleCrop().into(binding.imgAvatar)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = auth.currentUser ?: return
        val userId = user.uid

        // Load dữ liệu hiện tại
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("displayName") ?: ""
                val avatarUrl = doc.getString("avatarUrl") ?: ""
                binding.edtDisplayName.setText(name)
                if (avatarUrl.isNotEmpty()) {
                    Glide.with(this).load(avatarUrl).circleCrop().into(binding.imgAvatar)
                }
            }

        binding.btnChangeAvatar.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            val newName = binding.edtDisplayName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Tên không được để trống", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSave.isEnabled = false
            showStatus("Đang lưu...", "#888888")

            if (selectedImageUri != null) {
                uploadToCloudinary(selectedImageUri!!) { imageUrl ->
                    if (imageUrl != null) {
                        saveToFirestore(userId, newName, imageUrl)
                    } else {
                        requireActivity().runOnUiThread {
                            binding.btnSave.isEnabled = true
                            showStatus("Lỗi upload ảnh", "#E53935")
                        }
                    }
                }
            } else {
                saveToFirestore(userId, newName, null)
            }
        }
    }

    private fun uploadToCloudinary(uri: Uri, callback: (String?) -> Unit) {
        try {
            // Copy uri sang file tạm
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File(requireContext().cacheDir, "upload_temp.jpg")
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", tempFile.name,
                    tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .addFormDataPart("upload_preset", "library_app")
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/dzkes4tqu/image/upload")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    callback(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    if (response.isSuccessful && body != null) {
                        val json = JSONObject(body)
                        val url = json.getString("secure_url")
                        callback(url)
                    } else {
                        callback(null)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            callback(null)
        }
    }

    private fun saveToFirestore(userId: String, name: String, avatarUrl: String?) {
        val updates = mutableMapOf<String, Any>("displayName" to name)
        if (avatarUrl != null) updates["avatarUrl"] = avatarUrl

        db.collection("users").document(userId)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                requireActivity().runOnUiThread {
                    showStatus("Đã lưu thành công!", "#388E3C")
                    binding.root.postDelayed({
                        findNavController().popBackStack()
                    }, 1000)
                }
            }
            .addOnFailureListener { e ->
                requireActivity().runOnUiThread {
                    binding.btnSave.isEnabled = true
                    showStatus("Lỗi: ${e.message}", "#E53935")
                }
            }
    }

    private fun showStatus(message: String, colorHex: String) {
        binding.txtStatus.visibility = View.VISIBLE
        binding.txtStatus.text = message
        binding.txtStatus.setTextColor(android.graphics.Color.parseColor(colorHex))
    }
}