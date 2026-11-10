package de.uhrwerksarchiv.watchaccurancy.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import de.uhrwerksarchiv.watchaccurancy.MainActivity
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(ReminderManager.PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(ReminderManager.KEY_REMINDER_ENABLED, false)

        if (!isEnabled) {
            return
        }

        // Show Notification
        showReminderNotification(context)

        // Reschedule for the next day at the configured hour and minute
        val hour = prefs.getInt(ReminderManager.KEY_REMINDER_HOUR, ReminderManager.DEFAULT_HOUR)
        val minute = prefs.getInt(ReminderManager.KEY_REMINDER_MINUTE, ReminderManager.DEFAULT_MINUTE)
        ReminderManager.scheduleDailyAlarm(context, hour, minute)
    }

    private fun showReminderNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = "watch_accuracy_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.reminder_notification_title)
        val message = context.getString(R.string.reminder_notification_message)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}

object ReminderManager {
    const val PREFS_NAME = "watchaccurancy_reminder_prefs"
    const val KEY_REMINDER_ENABLED = "reminder_enabled"
    const val KEY_REMINDER_HOUR = "reminder_hour"
    const val KEY_REMINDER_MINUTE = "reminder_minute"

    const val DEFAULT_HOUR = 20 // 20:00 Uhr
    const val DEFAULT_MINUTE = 0

    fun isReminderEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMINDER_ENABLED, false)
    }

    fun getReminderTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_HOUR)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_MINUTE)
        return Pair(hour, minute)
    }

    fun setReminderEnabled(context: Context, enabled: Boolean, hour: Int = DEFAULT_HOUR, minute: Int = DEFAULT_MINUTE) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()

        if (enabled) {
            scheduleDailyAlarm(context, hour, minute)
        } else {
            cancelAlarm(context)
        }
    }

    fun scheduleDailyAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback for strict background alarm policies
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
