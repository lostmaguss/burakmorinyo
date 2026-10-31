package com.burakmorinyo.gelirgider

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.work.Configuration
import com.burakmorinyo.gelirgider.data.AppDatabase
import com.burakmorinyo.gelirgider.data.Repository
import com.burakmorinyo.gelirgider.data.SettingsStore
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.widget.SummaryWidget
import com.burakmorinyo.gelirgider.work.DailyWorker
import com.burakmorinyo.gelirgider.work.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class GelirGiderApp : Application(), Configuration.Provider {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database by lazy { AppDatabase.build(this) }
    val settings by lazy { SettingsStore(this) }
    val notifier by lazy { Notifier(this) }
    val repository by lazy { Repository(database, settings, notifier) }

    // WorkManager ilk kullanımda bu ayarla kendini başlatır
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        notifier.createChannels()
        DailyWorker.schedule(this)
        appScope.launch {
            settings.flow.collect {
                Money.symbol = it.currency
                Money.hidden = it.hideAmounts
            }
        }
        appScope.launch {
            repository.ensureSeed()
            repository.processRecurring()
            repository.refreshRates()
        }
        // İşlemler değiştikçe ana ekran widget'ı tazelenir
        appScope.launch {
            repository.transactions.drop(1).debounce(800).collect {
                runCatching { SummaryWidget().updateAll(this@GelirGiderApp) }
            }
        }
    }
}
