package com.example.libraryapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // 🔥 LẤY NavController
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 🔥 LẤY BottomNav
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // 🔥 CONNECT (QUAN TRỌNG NHẤT)
        bottomNav.setupWithNavController(navController)
    }
}