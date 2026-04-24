package com.example.libraryapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.libraryapp.notification.ReminderScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val enabled = sharedPref.getBoolean("reminder_enabled", false)
            if (enabled) {
                ReminderScheduler.schedule(context)
            }
        }
    }
}