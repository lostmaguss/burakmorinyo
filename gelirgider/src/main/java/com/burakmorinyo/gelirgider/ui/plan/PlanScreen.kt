package com.burakmorinyo.gelirgider.ui.plan

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.Budget
import com.burakmorinyo.gelirgider.data.Debt
import com.burakmorinyo.gelirgider.data.DebtDirection
import com.burakmorinyo.gelirgider.data.Frequency
import com.burakmorinyo.gelirgider.data.Goal
import com.burakmorinyo.gelirgider.data.Recurring
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.BudgetStatus
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.monthlyEquivalent
import com.burakmorinyo.gelirgider.domain.relativeDays
import com.burakmorinyo.gelirgider.domain.short
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AmountInput
import com.burakmorinyo.gelirgider.ui.components.AmountText
import com.burakmorinyo.gelirgider.ui.components.AppCard
import com.burakmorinyo.gelirgider.ui.components.CategoryIcon
import com.burakmorinyo.gelirgider.ui.components.EmptyState
import com.burakmorinyo.gelirgider.ui.components.ListRow
import com.burakmorinyo.gelirgider.ui.components.Pill
import com.burakmorinyo.gelirgider.ui.components.ProgressBar
import com.burakmorinyo.gelirgider.ui.components.RingProgress
import com.burakmorinyo.gelirgider.ui.components.SectionHeader
import com.burakmorinyo.gelirgider.ui.components.asColor
import com.burakmorinyo.gelirgider.ui.components.budgetColor
import com.burakmorinyo.gelirgider.ui.theme.AmountStyle
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private enum class PlanTab(val label: String) { BUDGET("Bütçe"), PAYMENTS("Ödemeler"), GOALS("Hedefler"), DEBTS("Borç/Alacak") }

@Composable
fun PlanScreen(data: AppData, vm: MainViewModel, nav: NavHostController) {
    var tab by rememberSaveable { mutableStateOf(PlanTab.BUDGET) }
    Column(Modifier.fillMaxSize()) {
        Text("Planla", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.statusBarsPadding().padding(start = 20.dp, top = 12.dp, bottom = 8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlanTab.entries.forEach { t ->
                val sel = t == tab
                val bg by animateColorAsState(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, label = "tab")
                Text(
                    t.label,
                    Modifier.clip(CircleShape).background(bg).clickable { tab = t }.padding(horizontal = 18.dp, vertical = 10.dp),
                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                PlanTab.BUDGET -> budgetTab(data, vm)
                PlanTab.PAYMENTS -> paymentsTab(data, vm, nav)
                PlanTab.GOALS -> goalsTab(data, vm, nav)
                PlanTab.DEBTS -> debtsTab(data, vm, nav)
            }
        }
    }
}

// ---------------- Bütçe ----------------

/** Son 3 dönemin ortalama harcaması: bütçe önerisi için */
private fun AppData.averageSpend(categoryId: Long?): Long {
    var p = currentPeriod.previous(settings.periodStartDay)
    val values = mutableListOf<Long>()
    repeat(3) {
        val t = Finance.inPeriod(transactions, p).filter { it.type == TxType.EXPENSE && (categoryId == null || it.categoryId == categoryId) }
        values += t.sumOf { it.amount }
        p = p.previous(settings.periodStartDay)
    }
    val nonZero = values.filter { it > 0 }
    return if (nonZero.isEmpty()) 0 else nonZero.average().toLong()
}

private fun LazyListScope.budgetTab(data: AppData, vm: MainViewModel) {
    val period = data.currentPeriod
    val txs = Finance.inPeriod(data.transactions, period)
    val statuses = Finance.budgetStatuses(data.budgets, txs, data.categoryMap)
    val overall = statuses.firstOrNull { it.budget.categoryId == null }
    val daysLeft = (period.days - period.elapsedDays() + 1).coerceAtLeast(1)

    item {
        var editing by remember { mutableStateOf(false) }
        if (overall == null) {
            val avg = remember(data) { data.averageSpend(null) }
            AppCard {
                Text("Aylık toplam bütçe", style = MaterialTheme.typography.titleMedium)
                Text("Bir üst sınır belirle; ilerlemeni, günlük harcama hakkını ve aşım uyarılarını göstereyim.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(onClick = { editing = true }) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Bütçe belirle") }
                if (avg > 0) Text("Son dönemlerde ortalama ${Money.format(avg)} harcamışsın.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            BudgetCard(overall, "Aylık toplam", daysLeft, big = true) { editing = true }
        }
        if (editing) BudgetDialog(data, overall?.budget, fixedCategory = true, categoryId = null, onDismiss = { editing = false }, vm = vm)
    }

    item {
        var adding by remember { mutableStateOf(false) }
        SectionHeader("Kategori bütçeleri", action = "Ekle") { adding = true }
        if (adding) BudgetDialog(data, null, fixedCategory = false, categoryId = null, onDismiss = { adding = false }, vm = vm)
    }
    val catStatuses = statuses.filter { it.budget.categoryId != null }
    if (catStatuses.isEmpty()) item {
        EmptyState(Icons.Rounded.Savings, "Kategori bütçesi yok", "Market, yemek, eğlence gibi kategorilere ayrı limit koyarak kontrolü elinde tut.")
    }
    catStatuses.forEach { st ->
        item(key = "b${st.budget.id}") {
            var editing by remember { mutableStateOf(false) }
            BudgetCard(st, st.category?.name ?: "?", daysLeft) { editing = true }
            if (editing) BudgetDialog(data, st.budget, fixedCategory = true, categoryId = st.budget.categoryId, onDismiss = { editing = false }, vm = vm)
        }
    }
}

@Composable
private fun BudgetCard(st: BudgetStatus, title: String, daysLeft: Int, big: Boolean = false, onClick: () -> Unit) {
    val color = budgetColor(st.ratio)
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (st.category != null) { CategoryIcon(st.category.icon, st.category.color, 40.dp); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = if (big) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("${Money.format(st.spent)} / ${Money.format(st.limit)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("%${(st.ratio * 100).roundToInt()}", style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(st.ratio, color, height = if (big) 12.dp else 8.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (st.remaining >= 0) "${Money.format(st.remaining)} kaldı · günde ${Money.format(st.remaining / daysLeft)}"
            else "${Money.format(-st.remaining)} aşıldı",
            style = MaterialTheme.typography.bodySmall, color = if (st.remaining >= 0) MaterialTheme.colorScheme.onSurfaceVariant else color
        )
    }
}

@Composable
private fun BudgetDialog(data: AppData, budget: Budget?, fixedCategory: Boolean, categoryId: Long?, onDismiss: () -> Unit, vm: MainViewModel) {
    val used = data.budgets.mapNotNull { it.categoryId }.toSet()
    val options = data.categoriesOf(TxType.EXPENSE).filter { it.id !in used }
    var cat by remember { mutableLongStateOf(categoryId ?: options.firstOrNull()?.id ?: 0L) }
    var text by remember { mutableStateOf(budget?.let { Money.toInput(it.amount) } ?: "") }
    var menu by remember { mutableStateOf(false) }
    val targetCategory = if (fixedCategory) categoryId else cat
    val avg = remember(targetCategory) { data.averageSpend(targetCategory) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (fixedCategory && categoryId == null) "Aylık toplam bütçe" else (data.categoryMap[targetCategory ?: 0]?.name ?: "Kategori bütçesi")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!fixedCategory) {
                    Box {
                        OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(data.categoryMap[cat]?.name ?: "Kategori seç")
                        }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            options.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, leadingIcon = { CategoryIcon(c.icon, c.color, 28.dp) },
                                    onClick = { cat = c.id; menu = false })
                            }
                        }
                    }
                }
                AmountInput(text, { text = it }, "Aylık limit")
                if (avg > 0) TextButton(onClick = { text = Money.toInput((avg + 9_999) / 10_000 * 10_000) }) {
                    Text("Öneri: son dönemlerin ortalaması ${Money.format(avg)}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = Money.parse(text) ?: 0
                if (amount > 0 && (fixedCategory || cat != 0L)) { vm.setBudget(targetCategory, amount); onDismiss() }
            }) { Text("Kaydet") }
        },
        dismissButton = {
            if (budget != null) TextButton(onClick = { vm.deleteBudget(budget); onDismiss() }) { Text("Kaldır", color = MaterialTheme.colorScheme.error) }
            else TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

// ---------------- Ödemeler ----------------

private fun LazyListScope.paymentsTab(data: AppData, vm: MainViewModel, nav: NavHostController) {
    val active = data.recurring.filter { it.active }
    val fixed = active.filter { it.type == TxType.EXPENSE }.sumOf { monthlyEquivalent(it.amount, it.frequency) }
    val subs = active.filter { it.type == TxType.EXPENSE && it.isSubscription }.sumOf { monthlyEquivalent(it.amount, it.frequency) }
    val regularIncome = active.filter { it.type == TxType.INCOME }.sumOf { monthlyEquivalent(it.amount, it.frequency) }

    item {
        AppCard {
            Text("Aylık sabit giderler", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AmountText(fixed, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Abonelikler", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${Money.format(subs)} / ay", style = MaterialTheme.typography.titleSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("Düzenli gelir", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${Money.format(regularIncome)} / ay", style = MaterialTheme.typography.titleSmall, color = LocalFinanceColors.current.income)
                }
            }
            if (regularIncome > 0) {
                Spacer(Modifier.height(10.dp))
                val ratio = fixed.toFloat() / regularIncome
                ProgressBar(ratio, budgetColor(ratio / 0.7f))
                Text("Sabit giderler düzenli gelirinin %${(ratio * 100).roundToInt()}'i", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { nav.navigate(Routes.recurring()) }, Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text("Düzenli işlem") }
            FilledTonalButton(onClick = { nav.navigate(Routes.recurring(subscription = true)) }, Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text("Abonelik") }
        }
    }
    if (data.recurring.isEmpty()) item {
        EmptyState(Icons.Rounded.Repeat, "Düzenli ödeme yok", "Kira, faturalar, maaş, Netflix/Spotify gibi abonelikleri ekle; günü gelince hatırlatayım veya kendiliğinden kaydedeyim.")
    }
    val groups = listOf(
        "Abonelikler" to active.filter { it.type == TxType.EXPENSE && it.isSubscription },
        "Faturalar ve düzenli ödemeler" to active.filter { it.type == TxType.EXPENSE && !it.isSubscription },
        "Düzenli gelirler" to active.filter { it.type == TxType.INCOME },
        "Durdurulanlar" to data.recurring.filter { !it.active }
    )
    groups.filter { it.second.isNotEmpty() }.forEach { (title, list) ->
        item { SectionHeader(title) }
        item {
            AppCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                list.sortedBy { it.nextDate }.forEach { r -> RecurringRow(r, data, vm) { nav.navigate(Routes.recurring(r.id)) } }
            }
        }
    }
}

@Composable
private fun RecurringRow(r: Recurring, data: AppData, vm: MainViewModel, onClick: () -> Unit) {
    val c = r.categoryId?.let { data.categoryMap[it] }
    val due = r.nextDate.toLocalDate()
    val today = today()
    ListRow(
        leading = { CategoryIcon(c?.icon ?: "repeat", c?.color ?: 0xFF6366F1, 40.dp) },
        title = r.title,
        subtitle = if (r.active) listOfNotNull(
            r.frequency.label.takeIf { r.frequency != Frequency.MONTHLY },
            "${due.short()}, ${relativeDays(due, today)}",
            if (r.autoCreate) "otomatik" else "hatırlatma"
        ).joinToString(" · ") else "Durduruldu",
        onClick = onClick,
        trailing = {
            Column(horizontalAlignment = Alignment.End) {
                AmountText(r.amount, r.type, MaterialTheme.typography.titleSmall)
                if (r.active && !r.autoCreate && !due.isAfter(today.plusDays(3))) {
                    Row {
                        TextButton(onClick = { vm.skip(r) }, contentPadding = PaddingValues(horizontal = 6.dp), modifier = Modifier.height(32.dp)) { Text("Atla") }
                        FilledTonalButton(onClick = { vm.markPaid(r) }, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(32.dp)) {
                            Text(if (r.type == TxType.INCOME) "Alındı" else "Ödendi")
                        }
                    }
                }
            }
        }
    )
}

// ---------------- Hedefler ----------------

private fun LazyListScope.goalsTab(data: AppData, vm: MainViewModel, nav: NavHostController) {
    val totalSaved = data.goals.sumOf { it.saved }
    val totalTarget = data.goals.sumOf { it.target }
    if (data.goals.isNotEmpty()) item {
        AppCard {
            Text("Hedefler için biriken", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(Money.format(totalSaved), style = MaterialTheme.typography.headlineMedium.merge(AmountStyle))
            Text("Toplam hedef ${Money.format(totalTarget)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            ProgressBar(if (totalTarget > 0) totalSaved.toFloat() / totalTarget else 0f, MaterialTheme.colorScheme.primary, height = 10.dp)
        }
    }
    item {
        FilledTonalButton(onClick = { nav.navigate(Routes.goal()) }, Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Yeni birikim hedefi")
        }
    }
    if (data.goals.isEmpty()) item {
        EmptyState(Icons.Rounded.Flag, "Henüz hedef yok", "Tatil, araba, acil durum fonu… Hedef koy, her ay ne kadar ayırman gerektiğini hesaplayayım.")
    }
    data.goals.forEach { g -> item(key = "g${g.id}") { GoalCard(g, vm) { nav.navigate(Routes.goal(g.id)) } } }
}

@Composable
private fun GoalCard(g: Goal, vm: MainViewModel, onClick: () -> Unit) {
    var deposit by remember { mutableStateOf<Int?>(null) } // 1 = ekle, -1 = çek
    val ratio = if (g.target > 0) g.saved.toFloat() / g.target else 0f
    val color = g.color.asColor()
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(ratio, color, size = 64.dp, thickness = 7.dp) {
                Text("%${(ratio * 100).roundToInt().coerceAtMost(999)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (g.completed) { Spacer(Modifier.width(6.dp)); Pill("Tamamlandı", LocalFinanceColors.current.income) }
                }
                Text("${Money.format(g.saved)} / ${Money.format(g.target)}", style = MaterialTheme.typography.bodyMedium)
                val remaining = g.target - g.saved
                if (g.deadline != null && remaining > 0) {
                    val d = g.deadline.toLocalDate()
                    val months = ChronoUnit.MONTHS.between(today().withDayOfMonth(1), d.withDayOfMonth(1)).coerceAtLeast(1)
                    Text(
                        if (d.isBefore(today())) "Süre doldu · ${Money.format(remaining)} eksik"
                        else "${d.short()} · ayda ${Money.format(remaining / months)} ayırmalısın",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (remaining > 0) Text("${Money.format(remaining)} kaldı", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { deposit = 1 }, Modifier.weight(1f)) { Text("Para ekle") }
            OutlinedButton(onClick = { deposit = -1 }, Modifier.weight(1f), enabled = g.saved > 0) { Text("Çek") }
        }
    }
    deposit?.let { sign ->
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { deposit = null },
            title = { Text(if (sign > 0) "${g.name} için para ekle" else "${g.name} hedefinden çek") },
            text = { AmountInput(text, { text = it }, "Tutar") },
            confirmButton = {
                TextButton(onClick = {
                    val a = Money.parse(text) ?: 0
                    if (a > 0) { vm.addDeposit(g, a * sign); deposit = null }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { deposit = null }) { Text("Vazgeç") } }
        )
    }
}

// ---------------- Borç / Alacak ----------------

private fun LazyListScope.debtsTab(data: AppData, vm: MainViewModel, nav: NavHostController) {
    val open = data.debts.filter { !it.settled }
    val iOwe = open.filter { it.direction == DebtDirection.I_OWE }.sumOf { it.remaining }
    val owed = open.filter { it.direction == DebtDirection.OWED_TO_ME }.sumOf { it.remaining }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppCard(Modifier.weight(1f), padding = PaddingValues(14.dp)) {
                Text("Borçlarım", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(iOwe), style = MaterialTheme.typography.titleLarge.merge(AmountStyle), color = LocalFinanceColors.current.expense, maxLines = 1)
            }
            AppCard(Modifier.weight(1f), padding = PaddingValues(14.dp)) {
                Text("Alacaklarım", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(owed), style = MaterialTheme.typography.titleLarge.merge(AmountStyle), color = LocalFinanceColors.current.income, maxLines = 1)
            }
        }
    }
    item {
        FilledTonalButton(onClick = { nav.navigate(Routes.debt()) }, Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Borç / alacak ekle")
        }
    }
    if (data.debts.isEmpty()) item {
        EmptyState(Icons.Rounded.Handshake, "Kayıt yok", "Arkadaşına verdiğin ya da aldığın borçları unutma; ödendikçe işaretle.")
    }
    val settled = data.debts.filter { it.settled }
    open.forEach { d -> item(key = "d${d.id}") { DebtCard(d, data, vm) { nav.navigate(Routes.debt(d.id)) } } }
    if (settled.isNotEmpty()) {
        item { SectionHeader("Kapananlar") }
        settled.forEach { d -> item(key = "d${d.id}") { DebtCard(d, data, vm) { nav.navigate(Routes.debt(d.id)) } } }
    }
}

@Composable
private fun DebtCard(d: Debt, data: AppData, vm: MainViewModel, onClick: () -> Unit) {
    var paying by remember { mutableStateOf(false) }
    val fc = LocalFinanceColors.current
    val color = if (d.direction == DebtDirection.I_OWE) fc.expense else fc.income
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(d.person.take(1).uppercase(), color = color, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.person, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                val sub = listOfNotNull(
                    d.direction.label,
                    d.dueDate?.toLocalDate()?.let { "vade ${it.short()} (${relativeDays(it)})" },
                    d.note.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(d.remaining), style = MaterialTheme.typography.titleMedium.merge(AmountStyle), color = if (d.settled) MaterialTheme.colorScheme.onSurfaceVariant else color)
                if (d.paid > 0) Text("/ ${Money.format(d.amount)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!d.settled) {
            if (d.paid > 0) { Spacer(Modifier.height(8.dp)); ProgressBar(d.paid.toFloat() / d.amount, color) }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { paying = true }, Modifier.fillMaxWidth()) {
                Text(if (d.direction == DebtDirection.I_OWE) "Ödeme yap" else "Tahsilat ekle")
            }
        }
    }
    if (paying) {
        var text by remember { mutableStateOf(Money.toInput(d.remaining)) }
        var record by remember { mutableStateOf(true) }
        var account by remember { mutableLongStateOf(data.activeAccounts.firstOrNull()?.id ?: 0L) }
        var menu by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { paying = false },
            title = { Text(if (d.direction == DebtDirection.I_OWE) "${d.person} kişisine ödeme" else "${d.person} kişisinden tahsilat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AmountInput(text, { text = it }, "Tutar", supporting = "Kalan: ${Money.format(d.remaining)}")
                    Row(Modifier.clickable { record = !record }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(record, { record = it })
                        Text("Hesaba ${if (d.direction == DebtDirection.I_OWE) "gider" else "gelir"} olarak işle")
                    }
                    if (record) Box {
                        OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text(data.accountMap[account]?.name ?: "Hesap seç") }
                        DropdownMenu(menu, { menu = false }) {
                            data.activeAccounts.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { account = a.id; menu = false }) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val a = Money.parse(text) ?: 0
                    if (a > 0) { vm.payDebt(d, a.coerceAtMost(d.remaining), if (record && account != 0L) account else null); paying = false }
                }) { Text("Kaydet") }
            },
            dismissButton = { TextButton(onClick = { paying = false }) { Text("Vazgeç") } }
        )
    }
}
