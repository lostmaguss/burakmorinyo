package com.burakmorinyo.gelirgider.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.burakmorinyo.gelirgider.GelirGiderApp
import com.burakmorinyo.gelirgider.MainActivity
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Currencies
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.periodFor
import com.burakmorinyo.gelirgider.domain.today

/** Ana ekran widget'ı: bu dönemin harcaması, geliri ve tek dokunuşla gider/gelir ekleme */
class SummaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as GelirGiderApp
        val settings = app.settings.current()
        val txs = app.repository.allTransactions()
        val accounts = app.repository.allAccounts()
        val period = periodFor(today(), settings.periodStartDay)
        val base = Currencies.codeForSymbol(settings.currency)
        val inPeriod = Finance.toBase(Finance.inPeriod(txs, period), accounts.associateBy { it.id }, settings.rates, base)
        val expense = Finance.sum(inPeriod, TxType.EXPENSE)
        val income = Finance.sum(inPeriod, TxType.INCOME)
        val total = Finance.netWorth(Finance.balances(accounts, txs), settings.rates, base)
        val label = period.label(settings.periodStartDay)
        provideContent { Content(context, label, expense, income, total) }
    }

    @Composable
    private fun Content(context: Context, label: String, expense: Long, income: Long, total: Long) {
        val white = ColorProvider(Color.White)
        val faded = ColorProvider(Color(0xCCFFFFFF))
        Column(
            modifier = GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(Color(0xFF312E81)).padding(14.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(label, style = TextStyle(color = faded, fontSize = 12.sp))
            Text(Money.format(expense), style = TextStyle(color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold))
            Text("harcandı · gelir ${Money.format(income)}", style = TextStyle(color = faded, fontSize = 11.sp))
            Text("Toplam varlık ${Money.format(total)}", style = TextStyle(color = faded, fontSize = 11.sp))
            Spacer(GlanceModifier.height(8.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Pill(context, "− Gider", Color(0xFFF43F5E), MainActivity.ACTION_ADD_EXPENSE, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(8.dp))
                Pill(context, "+ Gelir", Color(0xFF10B981), MainActivity.ACTION_ADD_INCOME, GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun Pill(context: Context, text: String, color: Color, action: String, modifier: GlanceModifier) {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .putExtra(MainActivity.EXTRA_ACTION, action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        Box(
            modifier = modifier.height(36.dp).cornerRadius(18.dp).background(color).clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 13.sp))
        }
    }
}

class SummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummaryWidget()
}
