package com.burakmorinyo.gelirgider.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.burakmorinyo.gelirgider.GelirGiderApp
import com.burakmorinyo.gelirgider.MainActivity
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.relativeDays
import com.burakmorinyo.gelirgider.domain.short
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.widget.SummaryWidget
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Saatte bir çalışır: tekrarlayan işlemlerin günü geldiyse ekler, yaklaşan ödemeleri ve
 * kredi kartı son ödeme günlerini hatırlatır, akşam harcama girilmediyse hatırlatır.
 */
class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GelirGiderApp
        val repo = app.repository
        val settings = app.settings.current()
        val notifier = app.notifier
        val today = today()

        repo.processRecurring(today)
        repo.refreshRates()

        if (settings.paymentReminders) {
            for (r in repo.allRecurring()) {
                if (!r.active || r.type != TxType.EXPENSE || r.remindDaysBefore < 0) continue
                val due = r.nextDate.toLocalDate()
                val days = ChronoUnit.DAYS.between(today, due)
                // Otomatik kaydedilenler geçmişte kalmaz; hatırlatmalı olanlar ödenene kadar bir kez gecikme bildirimi alır
                if (days < 0 && r.autoCreate) continue
                if (days > r.remindDaysBefore) continue
                if (!app.settings.markNotified("pay:${r.id}:${r.nextDate}")) continue
                val text = if (days < 0) "${Money.format(r.amount)} tutarındaki ödemenin günü geçti (${due.short()})."
                else "${Money.format(r.amount)} — ${relativeDays(due, today)} (${due.short()})" +
                    if (r.autoCreate) ", otomatik kaydedilecek." else ". Ödediğinde uygulamadan işaretle."
                notifier.show(Notifier.CH_PAYMENT, ("pay" + r.id).hashCode(), r.title, text, MainActivity.ACTION_PLAN)
            }
            val balances = Finance.balances(repo.allAccounts(), repo.allTransactions(), today)
            for (b in balances) {
                val card = b.card ?: continue
                if (card.remainingDue <= 0) continue
                val days = ChronoUnit.DAYS.between(today, card.dueDate)
                if (days !in 0..3) continue
                if (!app.settings.markNotified("card:${b.account.id}:${card.dueDate}:$days")) continue
                notifier.show(
                    Notifier.CH_PAYMENT, ("card" + b.account.id).hashCode(),
                    "${b.account.name} son ödeme ${relativeDays(card.dueDate, today)}",
                    "Ekstre borcu: ${Money.format(card.remainingDue)}. Son ödeme tarihi ${card.dueDate.short()}."
                )
            }
        }

        if (settings.dailyReminder && LocalTime.now().hour >= settings.reminderHour && !repo.hasTransactionToday()) {
            if (app.settings.markNotified("daily:$today")) {
                notifier.show(
                    Notifier.CH_REMINDER, 7001, "Bugünkü harcamalarını girdin mi?",
                    "Birkaç saniye ayır, bütçen hep güncel kalsın.", MainActivity.ACTION_ADD_EXPENSE
                )
            }
        }

        runCatching { SummaryWidget().updateAll(applicationContext) }
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<DailyWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("daily", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
