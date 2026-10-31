package com.burakmorinyo.gelirgider.ui.home

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.AccountBalance
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Insight
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.Tone
import com.burakmorinyo.gelirgider.domain.Upcoming
import com.burakmorinyo.gelirgider.domain.relativeDays
import com.burakmorinyo.gelirgider.domain.short
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.accounts.netWorthEquivalents
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AmountText
import com.burakmorinyo.gelirgider.ui.components.AppCard
import com.burakmorinyo.gelirgider.ui.components.AppIcons
import com.burakmorinyo.gelirgider.ui.components.CategoryIcon
import com.burakmorinyo.gelirgider.ui.components.DonutChart
import com.burakmorinyo.gelirgider.ui.components.EmptyState
import com.burakmorinyo.gelirgider.ui.components.ListRow
import com.burakmorinyo.gelirgider.ui.components.Pill
import com.burakmorinyo.gelirgider.ui.components.ProgressBar
import com.burakmorinyo.gelirgider.ui.components.SectionHeader
import com.burakmorinyo.gelirgider.ui.components.Slice
import com.burakmorinyo.gelirgider.ui.components.SpendingPaceChart
import com.burakmorinyo.gelirgider.ui.components.asColor
import com.burakmorinyo.gelirgider.ui.components.budgetColor
import com.burakmorinyo.gelirgider.ui.navigateTab
import com.burakmorinyo.gelirgider.ui.theme.AmountStyle
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors
import com.burakmorinyo.gelirgider.ui.transactions.TransactionRow
import java.time.LocalTime
import kotlin.math.roundToInt

private data class HomeModel(
    val income: Long,
    val expense: Long,
    val daily: List<Long>,
    val elapsed: Int,
    val daysLeft: Int,
    val overallBudget: Long?,
    val insights: List<Insight>,
    val upcoming: List<Upcoming>,
    val topCategories: List<com.burakmorinyo.gelirgider.domain.CategoryTotal>
)

@Composable
fun HomeScreen(data: AppData, vm: MainViewModel, nav: NavHostController) {
    val s = data.settings
    val model = remember(data) {
        val period = data.currentPeriod
        val txs = Finance.inPeriod(data.statTransactions, period)
        val budgets = Finance.budgetStatuses(data.budgets, txs, data.categoryMap)
        val upcoming = Finance.upcoming(data.recurring, data.balances, data.categoryMap)
        HomeModel(
            income = Finance.sum(txs, TxType.INCOME),
            expense = Finance.sum(txs, TxType.EXPENSE),
            daily = Finance.dailyExpenses(txs, period),
            elapsed = period.elapsedDays(),
            daysLeft = period.days - period.elapsedDays() + 1,
            overallBudget = budgets.firstOrNull { it.budget.categoryId == null }?.limit,
            insights = Finance.insights(period, s.periodStartDay, data.statTransactions, data.categoryMap, budgets, upcoming, data.recurring),
            upcoming = upcoming.filter { !it.date.isAfter(today().plusDays(14)) },
            topCategories = Finance.categoryTotals(txs, TxType.EXPENSE, data.categoryMap)
        )
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${data.currentPeriod.label(s.periodStartDay)} · ${model.daysLeft} gün kaldı",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.updateSettings { it.copy(hideAmounts = !it.hideAmounts) } }) {
                    Icon(if (s.hideAmounts) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Tutarları gizle")
                }
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Rounded.Settings, "Ayarlar") }
            }
        }
        item { HeroCard(data.netWorth, netWorthEquivalents(data), model.income, model.expense) { nav.navigate(Routes.ACCOUNTS) } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                QuickAction(Icons.Rounded.Remove, "Gider", LocalFinanceColors.current.expense) { nav.navigate(Routes.tx(type = TxType.EXPENSE)) }
                QuickAction(Icons.Rounded.Add, "Gelir", LocalFinanceColors.current.income) { nav.navigate(Routes.tx(type = TxType.INCOME)) }
                QuickAction(Icons.AutoMirrored.Rounded.CompareArrows, "Transfer", LocalFinanceColors.current.transfer) { nav.navigate(Routes.tx(type = TxType.TRANSFER)) }
                QuickAction(Icons.Rounded.DocumentScanner, "Fiş tara", MaterialTheme.colorScheme.primary) { nav.navigate(Routes.tx(scan = true)) }
            }
        }
        item { SpendingCard(model, onClick = { nav.navigateTab(Routes.PLAN) }) }

        if (model.insights.isNotEmpty()) {
            item { SectionHeader("Akıllı öneriler") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    items(model.insights) { InsightCard(it) }
                }
            }
        }

        if (model.upcoming.isNotEmpty()) {
            item { SectionHeader("Yaklaşan ödemeler", action = "Tümü") { nav.navigateTab(Routes.PLAN) } }
            item {
                AppCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    model.upcoming.take(5).forEach { u -> UpcomingRow(u, vm) }
                }
            }
        }

        item { SectionHeader("Hesaplarım", action = "Yönet") { nav.navigate(Routes.ACCOUNTS) } }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(data.balances.filter { !it.account.archived }) { b ->
                    AccountChip(b, data) { nav.navigate(Routes.history(account = b.account.id)) }
                }
                item {
                    AppCard(Modifier.width(120.dp).height(118.dp), onClick = { nav.navigate(Routes.account()) }) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Hesap ekle", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        if (model.topCategories.isNotEmpty()) {
            item { SectionHeader("Nereye harcadım?", action = "Analiz") { nav.navigateTab(Routes.STATS) } }
            item { CategoryDonutCard(model) { nav.navigateTab(Routes.STATS) } }
        }

        item { SectionHeader("Son işlemler", action = "Tümü") { nav.navigateTab(Routes.TRANSACTIONS) } }
        item {
            val recent = data.transactions.filter { it.date <= System.currentTimeMillis() }.take(6)
            AppCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                if (recent.isEmpty()) EmptyState(
                    Icons.AutoMirrored.Rounded.ReceiptLong, "Henüz işlem yok",
                    "Ortadaki + düğmesiyle ilk gelirini veya giderini ekle."
                )
                recent.forEach { tx -> TransactionRow(tx, data, onClick = { nav.navigate(Routes.tx(tx.id)) }, showDate = true) }
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Günaydın 👋"
    in 12..17 -> "İyi günler 👋"
    in 18..22 -> "İyi akşamlar 👋"
    else -> "İyi geceler 🌙"
}

@Composable
private fun HeroCard(netWorth: Long, equivalents: String?, income: Long, expense: Long, onClick: () -> Unit) {
    val fc = LocalFinanceColors.current
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(fc.heroGradient).clickable(onClick = onClick)
    ) {
        // Dekoratif daireler
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 60.dp, y = (-70).dp).size(190.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-40).dp, y = 50.dp).size(120.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Column(Modifier.padding(22.dp)) {
            Text("Toplam varlık", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(Money.format(netWorth), color = Color.White, style = MaterialTheme.typography.displaySmall.merge(AmountStyle), maxLines = 1)
            if (equivalents != null && !Money.hidden) Text(equivalents, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroStat(Icons.Rounded.ArrowDownward, "Gelir", income, Modifier.weight(1f))
                HeroStat(Icons.Rounded.ArrowUpward, "Gider", expense, Modifier.weight(1f))
                Column(Modifier.weight(1f)) {
                    Text("Net", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                    Text(Money.format(income - expense, signed = true), color = Color.White, style = MaterialTheme.typography.titleMedium.merge(AmountStyle), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun HeroStat(icon: ImageVector, label: String, value: Long, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
            Text(Money.compact(value), color = Color.White, style = MaterialTheme.typography.titleMedium.merge(AmountStyle), maxLines = 1)
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, label, tint = color, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SpendingCard(m: HomeModel, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Bu dönem harcanan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AmountText(m.expense, style = MaterialTheme.typography.headlineMedium)
            }
            if (m.overallBudget != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Bütçe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.format(m.overallBudget), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        if (m.overallBudget != null && m.overallBudget > 0) {
            val ratio = m.expense.toFloat() / m.overallBudget
            Spacer(Modifier.height(10.dp))
            ProgressBar(ratio, budgetColor(ratio), height = 10.dp)
            Spacer(Modifier.height(6.dp))
            val left = m.overallBudget - m.expense
            Text(
                if (left >= 0) "%${(ratio * 100).roundToInt()} kullanıldı · ${Money.format(left)} kaldı"
                else "Bütçe ${Money.format(-left)} aşıldı",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                "Aylık bütçe belirle, sana günlük harcama hakkını hesaplayayım →",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        SpendingPaceChart(m.daily, m.elapsed, m.overallBudget, height = 90.dp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("Dönem başı", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            if (m.overallBudget != null) Text("- - - bütçe temposu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InsightCard(i: Insight) {
    val fc = LocalFinanceColors.current
    val color = when (i.tone) {
        Tone.GOOD -> fc.income
        Tone.WARN -> fc.warning
        Tone.BAD -> fc.expense
        Tone.INFO -> MaterialTheme.colorScheme.primary
    }
    AppCard(Modifier.width(280.dp).height(150.dp), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.get(i.icon), null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(i.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        Text(i.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun UpcomingRow(u: Upcoming, vm: MainViewModel) {
    val today = today()
    val overdue = u.date.isBefore(today)
    val r = u.recurring
    ListRow(
        leading = { CategoryIcon(u.icon, u.color, 40.dp) },
        title = u.title,
        subtitle = "${u.date.short()} · ${relativeDays(u.date, today)}" + if (r != null && r.autoCreate) " · otomatik" else "",
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                AmountText(u.amount, u.type, MaterialTheme.typography.titleSmall)
                if (overdue) Pill("Gecikti", LocalFinanceColors.current.expense)
                else if (r != null && !r.autoCreate && u.date == r.nextDate.toLocalDate()) {
                    FilledTonalButton(onClick = { vm.markPaid(r) }, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(30.dp)) {
                        Text(if (u.type == TxType.INCOME) "Alındı" else "Ödendi", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    )
}

@Composable
private fun AccountChip(b: AccountBalance, data: AppData, onClick: () -> Unit) {
    val a = b.account
    AppCard(Modifier.width(170.dp).height(118.dp), onClick = onClick, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(a.icon, a.color, 34.dp)
            Spacer(Modifier.width(8.dp))
            Text(a.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.weight(1f))
        AmountText(
            b.balance, style = MaterialTheme.typography.titleMedium,
            color = if (b.balance < 0) LocalFinanceColors.current.expense else MaterialTheme.colorScheme.onSurface,
            currency = a.currency
        )
        val card = b.card
        if (a.type == AccountType.CREDIT_CARD && card != null) {
            Text(
                if (card.remainingDue > 0) "Son ödeme ${card.dueDate.short()}" else "Kullanılabilir ${Money.compact(card.available)}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
            )
        } else if (a.currency != data.baseCode && data.rates.tryPer(a.currency) != null) {
            Text("≈ ${Money.format(data.toBase(b.balance, a.currency))}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        } else Text(a.type.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun CategoryDonutCard(m: HomeModel, onClick: () -> Unit) {
    val top = m.topCategories.take(5)
    val rest = m.topCategories.drop(5).sumOf { it.total }
    val slices = top.map { Slice(it.total.toFloat(), (it.category?.color ?: 0xFF94A3B8).asColor()) } +
        if (rest > 0) listOf(Slice(rest.toFloat(), MaterialTheme.colorScheme.outline)) else emptyList()
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(slices, size = 130.dp, thickness = 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Toplam", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.compact(m.expense), style = MaterialTheme.typography.titleMedium.merge(AmountStyle))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                top.forEach { ct ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background((ct.category?.color ?: 0xFF94A3B8).asColor()))
                        Spacer(Modifier.width(8.dp))
                        Text(ct.category?.name ?: "Kategorisiz", style = MaterialTheme.typography.bodySmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("%${(ct.share * 100).roundToInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
