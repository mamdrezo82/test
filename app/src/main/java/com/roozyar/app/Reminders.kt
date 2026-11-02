package com.roozyar.app

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.ZoneId

const val CHANNEL = "reminders"

class RoozApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val ch = NotificationChannel(CHANNEL, "یادآورها", NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
}

object Reminders {
    private fun pi(c: Context, id: Int, title: String) = PendingIntent.getBroadcast(
        c, id, Intent(c, ReminderReceiver::class.java).putExtra("id", id).putExtra("title", title),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun cancel(c: Context, id: Int) {
        c.getSystemService(AlarmManager::class.java).cancel(pi(c, id, ""))
    }

    fun sync(c: Context, t: Task) {
        cancel(c, t.id.toInt())
        if (t.done || t.timeMin < 0) return
        val at = LocalDate.ofEpochDay(t.dateDay).atTime(t.timeMin / 60, t.timeMin % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (at <= System.currentTimeMillis()) return
        val am = c.getSystemService(AlarmManager::class.java)
        val p = pi(c, t.id.toInt(), t.title)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms())
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("روزیار")
            .setContentText(i.getStringExtra("title") ?: "")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open).setAutoCancel(true).build()
        c.getSystemService(NotificationManager::class.java).notify(i.getIntExtra("id", 0), n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val r = goAsync()
        Thread {
            try { AppDb.get(c).tasks().pending().forEach { Reminders.sync(c, it) } } finally { r.finish() }
        }.start()
    }
}
