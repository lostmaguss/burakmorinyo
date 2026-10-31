package com.burakmorinyo.gelirgider.ui.stats

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.monthName
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.domain.startMillis
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AppCard
import com.burakmorinyo.gelirgider.ui.components.BarGroup
import com.burakmorinyo.gelirgider.ui.components.CategoryIcon
import com.burakmorinyo.gelirgider.ui.components.DonutChart
import com.burakmorinyo.gelirgider.ui.components.EmptyState
import com.burakmorinyo.gelirgider.ui.components.GroupedBarChart
import com.burakmorinyo.gelirgider.ui.components.LineChart
import com.burakmorinyo.gelirgider.ui.components.ProgressBar
import com.burakmorinyo.gelirgider.ui.components.SectionHeader
import com.burakmorinyo.gelirgider.ui.components.SimpleBars
import com.burakmorinyo.gelirgider.ui.components.Slice
import com.burakmorinyo.gelirgider.ui.components.asColor
import com.burakmorinyo.gelirgider.ui.components.typeColor
import com.burakmorinyo.gelirgider.ui.theme.AmountStyle
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors
import com.burakmorinyo.gelirgider.ui.transactions.shiftedPeriod
import kotlin.math.roundToInt

private val weekdayShort = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
private val weekdayLong = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

@Composable
fun StatsScreen(data: AppData, vm: MainViewModel, nav: NavHostController) {
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var type by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    val startDay = data.settings.periodStartDay
    val period = data.shiftedPeriod(offset)
    val fc = LocalFinanceColors.current

    val m = remember(data, offset, type) {
        val txs = Finance.inPeriod(data.statTransactions, period)
        val totals = Finance.categoryTotals(txs, type, data.categoryMap)
        val history = (5 downTo 0).map { k ->
            var p = period
            repeat(k) { p = p.previous(startDay) }
            p
        }
        val bars = history.map { p ->
            val t = Finance.inPeriod(data.statTransactions, p)
            BarGroup(monthName(p.start.plusDays(14).monthValue, short = true), listOf(Finance.sum(t, TxType.INCOME), Finance.sum(t, TxType.EXPENSE)))
        }
        val netWorth = history.map { p ->
            data.accounts.filter { it.includeInTotal }.sumOf { a ->
                data.toBase(Finance.balanceUntil(a, data.transactions, p.endMillis.coerceAtMost(today().plusDays(1).startMillis())), a.currency)
            }
        }
        // Son 12 haftanın hafta günlerine göre ortalama harcaması
        val since = today().minusWeeks(12)
        val weekday = LongArray(7)
        data.statTransactions.filter { it.type == TxType.EXPENSE }.forEach {
            val d = it.date.toLocalDate()
            if (!d.isBefore(since) && !d.isAfter(today())) weekday[d.dayOfWeek.value - 1] += it.amount
        }
        val total = Finance.sum(txs, type)
        val biggest = txs.filter { it.type == type }.maxByOrNull { it.amount }
        Model(totals, total, bars, netWorth, Finance.dailyExpenses(txs, period), weekday.map { it / 12 }, biggest,
            txs.count { it.type == type }, Finance.sum(txs, TxType.INCOME), Finance.sum(txs, TxType.EXPENSE))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Analiz", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.statusBarsPadding().padding(top = 12.dp, start = 4.dp))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.Rounded.ChevronLeft, "Önceki") }
                Text(period.label(startDay), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.Rounded.ChevronRight, "Sonraki") }
            }
            Segmented(type) { type = it }
        }
        item {
            AppCard {
                if (m.totals.isEmpty()) {
                    EmptyState(Icons.Rounded.PieChart, "Veri yok", "Bu dönemde ${if (type == TxType.EXPENSE) "gider" else "gelir"} kaydı bulunmuyor.")
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(m.totals.map { Slice(it.total.toFloat(), (it.category?.color ?: 0xFF94A3B8).asColor()) }, size = 200.dp, thickness = 26.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (type == TxType.EXPENSE) "Toplam gider" else "Toplam gelir", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Money.format(m.total), style = MaterialTheme.typography.titleLarge.merge(AmountStyle), color = typeColor(type))
                                Text("${m.count} işlem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    m.totals.forEach { ct ->
                        val c = ct.category
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                                .clickable(enabled = c != null) { nav.navigate(Routes.history(category = c!!.id)) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(c?.icon ?: "more", c?.color ?: 0xFF94A3B8, 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row {
                                    Text(c?.name ?: "Kategorisiz", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(Money.format(ct.total), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProgressBar(ct.share, (c?.color ?: 0xFF94A3B8).asColor(), Modifier.weight(1f), height = 6.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("%${(ct.share * 100).roundToInt()} · ${ct.count} işlem", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val days = if (offset == 0) period.elapsedDays().coerceAtLeast(1) else period.days
                Metric("Günlük ortalama", Money.format(m.total / days), Modifier.weight(1f))
                Metric(if (type == TxType.EXPENSE) "En büyük harcama" else "En büyük gelir", m.biggest?.let { Money.format(it.amount) } ?: "—", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val rate = if (m.income > 0) ((m.income - m.expense) * 100.0 / m.income).roundToInt() else null
                Metric("Tasarruf oranı", rate?.let { "%$it" } ?: "—", Modifier.weight(1f),
                    color = when { rate == null -> null; rate >= 20 -> fc.income; rate < 0 -> fc.expense; else -> fc.warning })
                Metric("Net", Money.format(m.income - m.expense, signed = true), Modifier.weight(1f))
            }
        }
        item { InflationCard(data, vm, nav, period, offset == 0, type) }
        item { SectionHeader("Son 6 dönem") }
        item {
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Legend("Gelir", fc.income); Legend("Gider", fc.expense)
                }
                Spacer(Modifier.height(10.dp))
                GroupedBarChart(m.bars, listOf(fc.income, fc.expense))
                Spacer(Modifier.height(10.dp))
                val avgExp = m.bars.dropLast(1).map { it.values[1] }.filter { it > 0 }.let { if (it.isEmpty()) 0 else it.average().toLong() }
                if (avgExp > 0) Text(
                    "Önceki dönemlerde ortalama ${Money.format(avgExp)} harcamışsın.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (type == TxType.EXPENSE && m.daily.any { it > 0 }) {
            item { SectionHeader("Günlük harcama") }
            item {
                AppCard {
                    val maxIdx = m.daily.indices.maxByOrNull { m.daily[it] } ?: -1
                    SimpleBars(m.daily, fc.expense, height = 100.dp, highlight = maxIdx)
                    Spacer(Modifier.height(8.dp))
                    if (maxIdx >= 0) Text(
                        "En çok ${period.start.plusDays(maxIdx.toLong()).let { "${it.dayOfMonth} ${monthName(it.monthValue)}" }} günü harcadın: ${Money.format(m.daily[maxIdx])}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (m.weekday.any { it > 0 }) {
            item { SectionHeader("Hangi gün daha çok harcıyorum?") }
            item {
                AppCard {
                    val maxIdx = m.weekday.indices.maxByOrNull { m.weekday[it] } ?: 0
                    SimpleBars(m.weekday, MaterialTheme.colorScheme.primary, height = 90.dp, highlight = maxIdx, labels = weekdayShort)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Son 12 haftada en çok ${weekdayLong[maxIdx]} günleri harcıyorsun (ortalama ${Money.format(m.weekday[maxIdx])}).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { SectionHeader("Net varlık seyri") }
        item {
            AppCard {
                Text(Money.format(m.netWorth.last()), style = MaterialTheme.typography.titleLarge.merge(AmountStyle))
                val change = m.netWorth.last() - m.netWorth.first()
                Text(
                    "6 dönemde ${Money.format(change, signed = true)}",
                    style = MaterialTheme.typography.bodySmall, color = if (change >= 0) fc.income else fc.expense
                )
                Spacer(Modifier.height(10.dp))
                LineChart(m.netWorth, MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    m.bars.forEach { Text(it.label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

private data class Model(
    val totals: List<com.burakmorinyo.gelirgider.domain.CategoryTotal>,
    val total: Long,
    val bars: List<BarGroup>,
    val netWorth: List<Long>,
    val daily: List<Long>,
    val weekday: List<Long>,
    val biggest: com.burakmorinyo.gelirgider.data.Transaction?,
    val count: Int,
    val income: Long,
    val expense: Long
)

@Composable
private fun Segmented(type: TxType, onChange: (TxType) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp)) {
        listOf(TxType.EXPENSE to "Giderler", TxType.INCOME to "Gelirler").forEach { (t, l) ->
            val sel = t == type
            val bg by animateColorAsState(if (sel) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).clip(CircleShape).background(bg).clickable { onChange(t) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(l, style = MaterialTheme.typography.labelLarge, color = if (sel) typeColor(t) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier, color: Color? = null) {
    AppCard(modifier, padding = PaddingValues(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.merge(AmountStyle), color = color ?: MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private data class Comparison(val label: String, val current: Long, val previous: Long, val inflation: Double?, val previousDate: java.time.LocalDate)

/**
 * Enflasyon karşısında: geçen yılın aynı dönemiyle (yoksa önceki dönemle) nominal, reel (TÜFE'ye göre)
 * ve dolar bazında karşılaştırma.
 */
@Composable
private fun InflationCard(data: AppData, vm: MainViewModel, nav: NavHostController, period: com.burakmorinyo.gelirgider.domain.Period, isCurrent: Boolean, type: TxType) {
    val startDay = data.settings.periodStartDay
    val annual = data.settings.annualInflation
    val fc = LocalFinanceColors.current
    val c = remember(data, period, type) {
        val elapsed = if (isCurrent) period.elapsedDays() else period.days
        fun total(p: com.burakmorinyo.gelirgider.domain.Period): Long {
            val end = p.start.plusDays(elapsed.toLong().coerceAtMost(p.days.toLong())).startMillis()
            return data.statTransactions.filter { it.type == type && it.date >= p.startMillis && it.date < end }.sumOf { it.amount }
        }
        val current = total(period)
        val lastYear = com.burakmorinyo.gelirgider.domain.periodFor(period.start.minusYears(1), startDay)
        val ly = total(lastYear)
        if (ly > 0) Comparison("Geçen yılın aynı dönemine göre", current, ly, annual, lastYear.start.plusDays(14))
        else {
            val prev = period.previous(startDay)
            Comparison("Geçen döneme göre", current, total(prev),
                annual?.let { com.burakmorinyo.gelirgider.domain.Inflation.monthlyFromAnnual(it) }, prev.start.plusDays(14))
        }
    }
    // Dolar bazlı karşılaştırma için o günün kuru (TCMB arşivi)
    var usdThen by remember(c.previousDate) { mutableStateOf<Double?>(null) }
    androidx.compose.runtime.LaunchedEffect(c.previousDate) { usdThen = vm.usdRateOn(c.previousDate) }
    val usdNow = data.rates.tryPer("USD")?.takeIf { data.baseCode == "TRY" }
    val isExpense = type == TxType.EXPENSE
    val word = if (isExpense) "harcama" else "gelir"

    AppCard {
        Text("Enflasyon karşısında", style = MaterialTheme.typography.titleMedium)
        Text(c.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        if (c.previous <= 0 || c.current <= 0) {
            Text("Karşılaştırmak için önceki dönemlerde de $word kaydı gerekiyor.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@AppCard
        }
        val nominal = com.burakmorinyo.gelirgider.domain.Inflation.nominalChange(c.current, c.previous)!!
        val real = c.inflation?.let { com.burakmorinyo.gelirgider.domain.Inflation.realChange(c.current, c.previous, it) }
        val fx = if (usdNow != null && usdThen != null) com.burakmorinyo.gelirgider.domain.Inflation.fxChange(c.current, usdNow, c.previous, usdThen!!) else null
        // Giderde artış kötü, gelirde artış iyi
        fun tone(v: Double) = if ((v > 0) == isExpense) fc.expense else fc.income
        fun pct(v: Double) = (if (v > 0) "+" else "−") + "%" + kotlin.math.abs(v * 100).roundToInt()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChangeCell("Nominal", pct(nominal), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            ChangeCell("Reel (TÜFE)", real?.let(::pct) ?: "—", real?.let(::tone) ?: MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            ChangeCell("Dolar bazında", fx?.let(::pct) ?: "—", fx?.let(::tone) ?: MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Önceki: ${Money.format(c.previous)} → şimdi: ${Money.format(c.current)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (real != null) {
            val msg = when {
                isExpense && real < -0.02 -> "Enflasyondan arındırınca gerçekte %${(-real * 100).roundToInt()} daha az harcamışsın. Tasarruf ediyorsun 👏"
                isExpense && real > 0.02 -> "Fiyat artışlarının ötesinde, gerçekten %${(real * 100).roundToInt()} daha fazla harcıyorsun."
                !isExpense && real < -0.02 -> "Gelirin enflasyonun gerisinde kaldı: alım gücün %${(-real * 100).roundToInt()} azaldı."
                !isExpense && real > 0.02 -> "Gelirin enflasyonu %${(real * 100).roundToInt()} geçti, alım gücün arttı."
                else -> "Enflasyondan arındırınca $word düzeyin hemen hemen aynı."
            }
            Spacer(Modifier.height(4.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium)
        } else {
            Spacer(Modifier.height(4.dp))
            androidx.compose.material3.TextButton(onClick = { nav.navigate(com.burakmorinyo.gelirgider.ui.Routes.SETTINGS) },
                contentPadding = PaddingValues(0.dp)) {
                Text("Reel değişimi görmek için Ayarlar'dan yıllık enflasyon oranını gir →")
            }
        }
    }
}

@Composable
private fun ChangeCell(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium.merge(AmountStyle), color = color, maxLines = 1)
    }
}
