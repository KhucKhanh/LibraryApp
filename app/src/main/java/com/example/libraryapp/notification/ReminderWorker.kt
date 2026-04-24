package com.example.libraryapp.notification

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val sharedPref = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val enabled = sharedPref.getBoolean("reminder_enabled", false)
        if (!enabled) return Result.success()

        val savedHour = sharedPref.getInt("reminder_hour", -1)
        val savedMinute = sharedPref.getInt("reminder_minute", -1)
        if (savedHour == -1) return Result.success()

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val nowInMinutes = currentHour * 60 + currentMinute
        val targetInMinutes = savedHour * 60 + savedMinute

        // Gửi notification nếu trong khoảng ±7 phút so với giờ đặt
        val diff = Math.abs(nowInMinutes - targetInMinutes)
        if (diff <= 7) {
            NotificationHelper.sendReminderNotification(context)
        }

        return Result.success()
    }
}