package com.example.libraryapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.libraryapp.ui.chat.ChatBottomSheet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val sharedPref = getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode", false)
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            )
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    com.example.libraryapp.ai.FirestoreRAGRepository.getAllChunks()
                    android.util.Log.d("CHAT_DEBUG", "Cache preloaded: ${com.example.libraryapp.ai.FirestoreRAGRepository.chunksCache.size} chunks")
                } catch (e: Exception) {
                    android.util.Log.e("CHAT_DEBUG", "Preload failed: ${e.message}")
                }
            }
        }


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val graph = navController.navInflater.inflate(R.navigation.nav_graph)

        graph.setStartDestination(
            if (user != null) R.id.homeFragment else R.id.loginFragment
        )
        navController.graph = graph

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val btnChat = findViewById<FloatingActionButton>(R.id.btnChat)

        setupBottomNavigation(bottomNav, navController)

        btnChat.setOnClickListener {
            ChatBottomSheet().show(supportFragmentManager, "chat")
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.menu.findItem(destination.id)?.isChecked = true

            val shouldHideBottom =
                destination.id == R.id.loginFragment ||
                        destination.id == R.id.registerFragment ||
                        destination.id == R.id.chapterReaderFragment ||
                        destination.id == R.id.forgotPasswordFragment

            val shouldHideChat =
                destination.id == R.id.loginFragment ||
                        destination.id == R.id.registerFragment ||
                        destination.id == R.id.forgotPasswordFragment

            bottomNav.visibility = if (shouldHideBottom) View.GONE else View.VISIBLE
            btnChat.visibility = if (shouldHideChat) View.GONE else View.VISIBLE
        }

        com.example.libraryapp.notification.NotificationHelper
            .createNotificationChannel(this)
    }

    private fun setupBottomNavigation(
        bottomNav: BottomNavigationView,
        navController: NavController
    ) {
        bottomNav.setOnItemSelectedListener { item ->
            val targetId = item.itemId

            if (navController.currentDestination?.id == targetId) return@setOnItemSelectedListener true

            val popped = navController.popBackStack(targetId, false)

            if (!popped) {
                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build()
                try {
                    navController.navigate(targetId, null, navOptions)
                } catch (e: Exception) {
                    return@setOnItemSelectedListener false
                }
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            findNavController(R.id.nav_host_fragment).navigate(R.id.loginFragment)
        }
    }
}