package com.example.libraryapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // 🔥 connect bottom nav
        bottomNav.setupWithNavController(navController)

        // 🔥 THÊM ĐOẠN NÀY
        navController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {

                // ❌ Ẩn ở login / register
                R.id.loginFragment,
                R.id.registerFragment -> {
                    bottomNav.visibility = View.GONE
                }

                // ✅ Còn lại hiện hết
                else -> {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }
}