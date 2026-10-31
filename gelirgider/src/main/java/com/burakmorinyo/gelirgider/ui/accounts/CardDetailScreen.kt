package com.burakmorinyo.gelirgider.ui.accounts

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.CardStatements
import com.burakmorinyo.gelirgider.domain.CardStatus
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.Statement
import com.burakmorinyo.gelirgider.domain.StatementStatus
import com.burakmorinyo.gelirgider.domain.monthName
import com.burakmorinyo.gelirgider.domain.relativeDays
import com.burakmorinyo.gelirgider.domain.short
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AmountInput
import com.burakmorinyo.gelirgider.ui.components.AppCard
import com.burakmorinyo.gelirgider.ui.components.EmptyState
import com.burakmorinyo.gelirgider.ui.components.Pill
import com.burakmorinyo.gelirgider.ui.components.ProgressBar
import com.burakmorinyo.gelirgider.ui.components.SectionHeader
import com.burakmorinyo.gelirgider.ui.components.asColor
import com.burakmorinyo.gelirgider.ui.components.budgetColor
import com.burakmorinyo.gelirgider.ui.theme.AmountStyle
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors
import com.burakmorinyo.gelirgider.ui.transactions.titleOf

@Composable
fun CardDetailScreen(data: AppData, vm: MainViewModel, nav: NavHostController, id: Long) {
    val card = data.accountMap[id] ?: return
    val status = data.balances.firstOrNull { it.account.id == id }?.card
    var showDebt by remember { mutableStateOf(false) }
    var showPay by remember { mutableStateOf(false) }
    val plans = remember(data, id) { CardStatements.installmentPlans(card, data.transactions) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
            Text(card.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { nav.navigate(Routes.account(id)) }) { Icon(Icons.Rounded.Edit, "Kartı düzenle") }
        }
        if (status == null) {
            EmptyState(Icons.Rounded.ViewAgenda, "Ekstre için kesim günü gerekli", "Kartı düzenleyip hesap kesim ve son ödeme gününü gir.") {
                FilledTonalButton(onClick = { nav.navigate(Routes.account(id)) }) { Text("Kartı düzenle") }
            }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CreditCardVisual(card, status) }
            item { CurrentCycleCard(card, status, onUpdateDebt = { showDebt = true }, nav = nav) }
            if (status.remainingDue > 0) item { DueStatementCard(status, onPay = { showPay = true }) }

            item { SectionHeader("Ekstreler") }
            val list = status.statements
            val upcoming = list.filter { it.status == StatementStatus.UPCOMING }
            val rest = list.filter { it.status != StatementStatus.UPCOMING }.reversed()
            rest.forEachIndexed { i, st -> item(key = "s${st.closing}") { StatementRow(st, data, initiallyOpen = i == 0) } }
            if (upcoming.isNotEmpty()) {
                item { SectionHeader("Gelecek aylara yansıyacak taksitler") }
                upcoming.forEach { st -> item(key = "u${st.closing}") { StatementRow(st, data, initiallyOpen = false) } }
            }

            item {
                SectionHeader("Taksitli alışverişler", action = "Ekle") {
                    nav.navigate(Routes.tx(account = id, installment = true))
                }
            }
            if (plans.isEmpty()) item {
                Text(
                    "Bu kartta taksit yok. “Ekle” ile taksitli alışverişini gir; geçmişte başladıysa (ör. Mart) alışveriş tarihini seçmen yeterli, geçmiş taksitler ödenmiş sayılır.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            plans.forEach { p -> item(key = "p${p.group}") { PlanRow(p, data) } }
        }
    }

    if (showDebt) DebtDialog(card, status!!, onDismiss = { showDebt = false }) { amount, includeUnpaid ->
        vm.setCardDebt(card, amount, includeUnpaid); showDebt = false
    }
    if (showPay) PayDialog(data, card, status!!, onDismiss = { showPay = false }) { from, amount ->
        vm.payCard(card, from, amount); showPay = false
    }
}

@Composable
fun CreditCardVisual(card: Account, status: CardStatus, modifier: Modifier = Modifier) {
    val base = card.color.asColor()
    val brush = Brush.linearGradient(listOf(lerp(base, Color.White, 0.08f), base, lerp(base, Color.Black, 0.45f)))
    Box(modifier.fillMaxWidth().height(196.dp).clip(RoundedCornerShape(24.dp)).background(brush)) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-60).dp).size(200.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-50).dp, y = 70.dp).size(160.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.07f)))
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.name, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
                // Çip
                Box(Modifier.size(38.dp, 28.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5D27A).copy(alpha = 0.9f)))
            }
            Spacer(Modifier.weight(1f))
            Text("Güncel borç", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
            Text(Money.format(status.debt), color = Color.White, style = MaterialTheme.typography.headlineMedium.merge(AmountStyle))
            Spacer(Modifier.height(8.dp))
            Row {
                CardStat("Kesim", status.nextStatement.short(), Modifier.weight(1f))
                CardStat("Son ödeme", status.dueDate.short(), Modifier.weight(1f))
                if (card.creditLimit > 0) CardStat("Kullanılabilir", Money.compact(status.available), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CardStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CurrentCycleCard(card: Account, st: CardStatus, onUpdateDebt: () -> Unit, nav: NavHostController) {
    val open = st.openStatement ?: return
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bu dönem (${open.start.short()} – ${open.closing.short()})", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(open.total), style = MaterialTheme.typography.headlineSmall.merge(AmountStyle))
            }
            Pill("kesime ${st.daysToClose} gün", MaterialTheme.colorScheme.primary)
        }
        val remaining = st.cycleRemaining
        if (remaining != null && card.cycleBudget > 0) {
            val ratio = open.total.toFloat() / card.cycleBudget
            Spacer(Modifier.height(10.dp))
            ProgressBar(ratio, budgetColor(ratio), height = 10.dp)
            Spacer(Modifier.height(8.dp))
            if (remaining > 0) {
                Text("Hedef ${Money.format(card.cycleBudget)} · kalan ${Money.format(remaining)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Kesime kadar günde ${Money.format(remaining / st.daysToClose.coerceAtLeast(1))} harcayabilirsin.",
                    style = MaterialTheme.typography.titleSmall, color = LocalFinanceColors.current.income
                )
            } else Text(
                "Dönem hedefini ${Money.format(-remaining)} aştın.", style = MaterialTheme.typography.titleSmall,
                color = LocalFinanceColors.current.expense
            )
        } else if (card.creditLimit > 0) {
            Spacer(Modifier.height(6.dp))
            Text("Kullanılabilir limit: ${Money.format(st.available)}", style = MaterialTheme.typography.bodyMedium)
            Text("Kartı düzenleyip “dönem harcama hedefi” koyarsan günlük harcama hakkını hesaplarım.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val installmentsInCycle = open.items.filter { it.installmentGroup != null }.sumOf { it.amount }
        if (installmentsInCycle > 0) Text(
            "Bunun ${Money.format(installmentsInCycle)} kadarı taksit.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onUpdateDebt, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Icon(Icons.Rounded.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Borcu güncelle")
            }
            OutlinedButton(onClick = { nav.navigate(Routes.tx(account = card.id)) }, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Harcama")
            }
        }
    }
}

@Composable
private fun DueStatementCard(st: CardStatus, onPay: () -> Unit) {
    val fc = LocalFinanceColors.current
    val color = if (st.overdue) fc.expense else fc.warning
    AppCard(color = color.copy(alpha = 0.10f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (st.overdue) "Gecikmiş ekstre" else "Ödenecek ekstre", style = MaterialTheme.typography.labelLarge, color = color)
                Text(Money.format(st.remainingDue), style = MaterialTheme.typography.headlineSmall.merge(AmountStyle))
                Text("Son ödeme ${st.dueDate.short()} (${relativeDays(st.dueDate)})", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onPay) { Text("Öde") }
        }
    }
}

private fun statementTitle(st: Statement): String {
    val m = monthName(st.closing.monthValue)
    return if (st.closing.year == today().year) "$m ekstresi" else "$m ${st.closing.year} ekstresi"
}

@Composable
private fun StatementRow(st: Statement, data: AppData, initiallyOpen: Boolean) {
    var open by remember { mutableStateOf(initiallyOpen) }
    val fc = LocalFinanceColors.current
    val (pillText, pillColor) = when (st.status) {
        StatementStatus.PAID, StatementStatus.ASSUMED_PAID -> "Ödendi" to fc.income
        StatementStatus.DUE -> "Ödenecek · ${st.dueDate.short()}" to fc.warning
        StatementStatus.OVERDUE -> "Gecikti" to fc.expense
        StatementStatus.OPEN -> "Açık dönem" to MaterialTheme.colorScheme.primary
        StatementStatus.UPCOMING -> "Son ödeme ${st.dueDate.short()}" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(statementTitle(st), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(pillText, pillColor)
                    Spacer(Modifier.width(6.dp))
                    Text("${st.items.size} hareket", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(Money.format(st.total), style = MaterialTheme.typography.titleMedium.merge(AmountStyle))
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(top = 8.dp)) {
                Text("Kesim ${st.closing.short()} · son ödeme ${st.dueDate.short()}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (st.status == StatementStatus.ASSUMED_PAID) Text(
                    "Son ödeme günü geçtiği için ödenmiş sayıldı.", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                st.items.forEach { tx ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tx.date.toLocalDate().short(), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
                        Text(data.titleOf(tx), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (tx.installmentCount > 1) { Pill("${tx.installmentIndex}/${tx.installmentCount}", MaterialTheme.colorScheme.primary); Spacer(Modifier.width(6.dp)) }
                        Text(
                            (if (tx.type == TxType.INCOME) "−" else "") + Money.format(tx.amount),
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (st.paid > 0) Text("Ödenen: ${Money.format(st.paid)}", style = MaterialTheme.typography.labelMedium, color = fc.income)
            }
        }
    }
}

@Composable
private fun PlanRow(p: CardStatements.InstallmentPlan, data: AppData) {
    val cat = p.categoryId?.let { data.categoryMap[it] }
    AppCard(padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    listOfNotNull(cat?.name, "aylık ${Money.format(p.perInstallment)}", "toplam ${Money.format(p.total)}").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${p.billed}/${p.count}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (p.finished) "bitti" else "kalan ${Money.format(p.remainingAmount)}", style = MaterialTheme.typography.labelSmall,
                    color = if (p.finished) LocalFinanceColors.current.income else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(p.billed.toFloat() / p.count, MaterialTheme.colorScheme.primary, height = 6.dp)
        if (!p.finished) Text("Son taksit ${monthName(p.lastClosing.monthValue)} ${p.lastClosing.year} ekstresinde",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DebtDialog(card: Account, st: CardStatus, onDismiss: () -> Unit, onSave: (Long, Boolean) -> Unit) {
    var includeUnpaid by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    val current = if (includeUnpaid) st.debt else st.currentPeriodSpend - (st.openStatement?.paid ?: 0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${card.name} borcu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bankanın uygulamasında gördüğün tutarı yaz; aradaki farkı tek kayıtla eşitlerim.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { includeUnpaid = false }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(!includeUnpaid, { includeUnpaid = false })
                    Text("Dönem içi harcama (son kesimden bu yana)", style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { includeUnpaid = true }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(includeUnpaid, { includeUnpaid = true })
                    Text("Toplam güncel borç (ödenmemiş ekstre dahil)", style = MaterialTheme.typography.bodyMedium)
                }
                AmountInput(text, { text = it }, "Güncel tutar", supporting = "Uygulamadaki: ${Money.format(current)}")
            }
        },
        confirmButton = { TextButton(onClick = { Money.parse(text)?.let { onSave(it, includeUnpaid) } }) { Text("Güncelle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun PayDialog(data: AppData, card: Account, st: CardStatus, onDismiss: () -> Unit, onPay: (Long, Long) -> Unit) {
    val sources = data.activeAccounts.filter { it.type != AccountType.CREDIT_CARD && it.currency == card.currency }
    var from by remember { mutableLongStateOf(sources.firstOrNull { it.type == AccountType.BANK }?.id ?: sources.firstOrNull()?.id ?: 0L) }
    var text by remember { mutableStateOf(Money.toInput(st.remainingDue)) }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${card.name} ekstre ödemesi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountInput(text, { text = it }, "Ödenen tutar", supporting = "Ekstre: ${Money.format(st.remainingDue)}")
                Box {
                    OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text("Hangi hesaptan: ${data.accountMap[from]?.name ?: "seç"}") }
                    DropdownMenu(menu, { menu = false }) {
                        sources.forEach { a -> DropdownMenuItem(text = { Text(a.name) }, onClick = { from = a.id; menu = false }) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { val a = Money.parse(text) ?: 0; if (a > 0 && from != 0L) onPay(from, a) }) { Text("Ödendi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}
