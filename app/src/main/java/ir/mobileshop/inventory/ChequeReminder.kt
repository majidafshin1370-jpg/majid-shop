package ir.mobileshop.inventory

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "cheques"
private const val NOTIFY_WITHIN_DAYS = 3

fun hasNotificationPermission(c: Context) =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Once a day (between 8:00 and 21:00) tells the user how many pending cheques are due within three days
 * or already overdue. Only counts are shown, never names or amounts, so a lock-screen glance reveals nothing.
 */
class ChequeReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!hasNotificationPermission(ctx)) return Result.success()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 8 || hour >= 21) return Result.success()

        val prefs = ctx.getSharedPreferences("cheque_reminder", Context.MODE_PRIVATE)
        val today = JalaliDates.getShamsiToday()
        if (prefs.getString("last", "") == today) return Result.success()

        val pending = AppDatabase.get(ctx).productDao().getPendingCheques()
        var received = 0
        var paid = 0
        for (c in pending) {
            val left = JalaliDates.daysUntil(c.dueDate) ?: continue
            if (left > NOTIFY_WITHIN_DAYS) continue
            if (c.direction == "received") received++ else paid++
        }
        if (received + paid == 0) return Result.success()

        val text = buildList {
            if (received > 0) add("${received.faNum()} چک دریافتی")
            if (paid > 0) add("${paid.faNum()} چک پرداختی")
        }.joinToString(" و ") + " تا ${NOTIFY_WITHIN_DAYS.faNum()} روز دیگر سررسید می‌شود یا گذشته است."

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "یادآوری چک", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = open?.let {
            android.app.PendingIntent.getActivity(
                ctx, 0, it, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("سررسید چک")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(4101, notification)
            prefs.edit().putString("last", today).apply()
        } catch (_: SecurityException) {
        }
        return Result.success()
    }
}

fun scheduleChequeReminders(context: Context) {
    val request = PeriodicWorkRequestBuilder<ChequeReminderWorker>(3, TimeUnit.HOURS).build()
    WorkManager.getInstance(context)
        .enqueueUniquePeriodicWork("cheque-reminders", ExistingPeriodicWorkPolicy.KEEP, request)
}
