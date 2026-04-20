package com.example.libraryapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            navController.navigate(R.id.loginFragment)
        } else {
            navController.navigate(R.id.homeFragment)
        }


        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            bottomNav.visibility = if (
                destination.id == R.id.chapterReaderFragment ||
                destination.id == R.id.loginFragment ||
                destination.id == R.id.registerFragment
            ) View.GONE else View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.loginFragment)
        }
    }
}