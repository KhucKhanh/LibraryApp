package com.example.libraryapp

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings

class LibraryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable Firestore offline cache
        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}