package com.burakmorinyo.gelirgider.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.burakmorinyo.gelirgider.MainActivity
import com.burakmorinyo.gelirgider.R
import com.burakmorinyo.gelirgider.data.AlertSink

class Notifier(private val context: Context) : AlertSink {

    companion object {
        const val CH_BUDGET = "budget"
        const val CH_PAYMENT = "payment"
        const val CH_REMINDER = "reminder"
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_BUDGET, "Bütçe uyarıları", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Bütçenin %80'i dolduğunda veya aşıldığında" },
                NotificationChannel(CH_PAYMENT, "Yaklaşan ödemeler", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Fatura, abonelik ve kredi kartı son ödeme hatırlatmaları" },
                NotificationChannel(CH_REMINDER, "Günlük hatırlatma", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Gün içinde harcama girmediysen akşam hatırlatır" }
            )
        )
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(channel: String, id: Int, title: String, text: String, action: String? = null) {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (action != null) putExtra(MainActivity.EXTRA_ACTION, action)
        }
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF6366F1.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }

    override fun budgetAlert(key: String, title: String, message: String) =
        show(CH_BUDGET, key.hashCode(), title, message, MainActivity.ACTION_PLAN)
}
