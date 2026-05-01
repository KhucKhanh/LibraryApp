package com.example.libraryapp.ui.profile

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.libraryapp.notification.NotificationHelper
import com.example.libraryapp.notification.ReminderScheduler
import java.util.Calendar
import android.os.Bundle
import android.view.*
import android.widget.Toast
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
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        loadStats(userId)

        val sharedPref = requireActivity().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)

        val docRef = db.collection("users").document(userId)

        docRef.get(com.google.firebase.firestore.Source.CACHE)
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener // ✅ thêm dòng này
                val displayName = doc.getString("displayName") ?: user.email ?: ""
                val avatarUrl = doc.getString("avatarUrl") ?: ""
                binding.txtEmail.text = displayName
                if (avatarUrl.isNotEmpty()) {
                    Glide.with(this).load(avatarUrl).circleCrop().into(binding.imgAvatar)
                }
            }

        docRef.get(com.google.firebase.firestore.Source.SERVER)
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener // ✅ thêm dòng này
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

        binding.switchDarkMode.isChecked = sharedPref.getBoolean("dark_mode", false)

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("dark_mode", isChecked).apply()
            val message = if (isChecked) "Chủ đề tối sẽ được áp dụng khi khởi động lại app"
            else "Chủ đề sáng sẽ được áp dụng khi khởi động lại app"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                showTimePicker()
            } else {
                binding.switchNotification.isChecked = false
                Toast.makeText(requireContext(), "Cần cấp quyền thông báo để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            }
        }
        val savedHour = sharedPref.getInt("reminder_hour", -1)
        val savedMinute = sharedPref.getInt("reminder_minute", -1)
        binding.switchNotification.isChecked = sharedPref.getBoolean("reminder_enabled", false)

        if (savedHour != -1) {
            val timeText = String.format("Nhắc lúc %02d:%02d", savedHour, savedMinute)
            binding.txtReminderTime.text = timeText
            binding.txtReminderTime.visibility = android.view.View.VISIBLE
        } else {
            binding.txtReminderTime.visibility = android.view.View.GONE
        }

        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Xin quyền POST_NOTIFICATIONS (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                showTimePicker()
            } else {
                sharedPref.edit()
                    .putBoolean("reminder_enabled", false)
                    .remove("reminder_hour")
                    .remove("reminder_minute")
                    .apply()
                ReminderScheduler.cancel(requireContext())
                binding.txtReminderTime.visibility = android.view.View.GONE
                Toast.makeText(requireContext(), "Đã tắt nhắc nhở đọc sách", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFriendInbox.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_friendFragment)
        }

        binding.btnBookShareInbox.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_bookShareInboxFragment)
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

    private fun showTimePicker() {
        val sharedPref = requireActivity().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val currentHour = sharedPref.getInt("reminder_hour", Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        val currentMinute = sharedPref.getInt("reminder_minute", 0)

        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                // Lưu giờ
                sharedPref.edit()
                    .putBoolean("reminder_enabled", true)
                    .putInt("reminder_hour", hour)
                    .putInt("reminder_minute", minute)
                    .apply()

                // Schedule WorkManager
                NotificationHelper.createNotificationChannel(requireContext())
                ReminderScheduler.schedule(requireContext())

                // Cập nhật UI
                val timeText = String.format("Nhắc lúc %02d:%02d", hour, minute)
                binding.txtReminderTime.text = timeText
                binding.txtReminderTime.visibility = android.view.View.VISIBLE
                binding.switchNotification.isChecked = true

                Toast.makeText(requireContext(), "Đã đặt nhắc nhở lúc $timeText", Toast.LENGTH_SHORT).show()
            },
            currentHour,
            currentMinute,
            true // 24h format
        ).apply {
            setOnCancelListener {
                // User bấm Cancel → tắt switch lại nếu chưa có giờ nào được lưu
                val hasReminder = sharedPref.getInt("reminder_hour", -1) != -1
                if (!hasReminder) {
                    binding.switchNotification.isChecked = false
                }
            }
        }.show()
    }

}