package com.burakmorinyo.gelirgider.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.Period
import com.burakmorinyo.gelirgider.domain.dayHeader
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.trLocale
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AmountText
import com.burakmorinyo.gelirgider.ui.components.AppCard
import com.burakmorinyo.gelirgider.ui.components.EmptyState
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors

fun AppData.shiftedPeriod(offset: Int): Period {
    var p = currentPeriod
    val d = settings.periodStartDay
    repeat(kotlin.math.abs(offset)) { p = if (offset < 0) p.previous(d) else p.next(d) }
    return p
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(data: AppData, vm: MainViewModel, nav: NavHostController, accountFilter: Long, categoryFilter: Long, isTab: Boolean) {
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var typeFilter by rememberSaveable { mutableStateOf<TxType?>(null) }
    var account by rememberSaveable { mutableLongStateOf(accountFilter) }
    var category by rememberSaveable { mutableLongStateOf(categoryFilter) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val period = data.shiftedPeriod(offset)

    val filtered = remember(data, offset, typeFilter, account, category, query) {
        val q = query.trim().lowercase(trLocale)
        val base = if (q.isNotEmpty()) data.transactions else Finance.inPeriod(data.transactions, period)
        base.filter { tx ->
            (typeFilter == null || tx.type == typeFilter) &&
                (account == 0L || tx.accountId == account || tx.toAccountId == account) &&
                (category == 0L || tx.categoryId == category) &&
                (q.isEmpty() || matches(tx, q, data))
        }
    }
    val income = Finance.sum(filtered, TxType.INCOME)
    val expense = Finance.sum(filtered, TxType.EXPENSE)
    val grouped = remember(filtered) { filtered.groupBy { it.date.toLocalDate() }.toList() }

    Column(Modifier.fillMaxSize()) {
        // Başlık
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!isTab) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
            if (searching) {
                TextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f).padding(start = 8.dp),
                    placeholder = { Text("Not, kategori, hesap veya tutar ara") }, singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                IconButton(onClick = { searching = false; query = "" }) { Icon(Icons.Rounded.Close, "Kapat") }
            } else {
                val title = when {
                    accountFilter != 0L -> data.accountMap[accountFilter]?.name ?: "İşlemler"
                    categoryFilter != 0L -> data.categoryMap[categoryFilter]?.name ?: "İşlemler"
                    else -> "İşlemler"
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
                IconButton(onClick = { searching = true }) { Icon(Icons.Rounded.Search, "Ara") }
            }
        }

        // Dönem seçici
        AnimatedVisibility(query.isBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.Rounded.ChevronLeft, "Önceki dönem") }
                Text(period.label(data.settings.periodStartDay), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { offset++ }, enabled = offset < 12) { Icon(Icons.Rounded.ChevronRight, "Sonraki dönem") }
            }
        }

        // Filtreler
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(null to "Tümü", TxType.EXPENSE to "Gider", TxType.INCOME to "Gelir", TxType.TRANSFER to "Transfer").forEach { (t, l) ->
                FilterChip(selected = typeFilter == t, onClick = { typeFilter = t }, label = { Text(l) })
            }
            FilterMenu(
                label = data.accountMap[account]?.name ?: "Hesap",
                selected = account != 0L,
                options = listOf(0L to "Tüm hesaplar") + data.accounts.map { it.id to it.name },
                onSelect = { account = it }
            )
            FilterMenu(
                label = data.categoryMap[category]?.name ?: "Kategori",
                selected = category != 0L,
                options = listOf(0L to "Tüm kategoriler") + data.categories.map { it.id to (it.name + if (it.type == TxType.INCOME) " (gelir)" else "") },
                onSelect = { category = it }
            )
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                AppCard(padding = PaddingValues(16.dp)) {
                    Row {
                        SummaryCell("Gelir", income, LocalFinanceColors.current.income, Modifier.weight(1f))
                        SummaryCell("Gider", expense, LocalFinanceColors.current.expense, Modifier.weight(1f))
                        SummaryCell("Net", income - expense, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), signed = true)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            if (grouped.isEmpty()) item {
                EmptyState(
                    Icons.AutoMirrored.Rounded.ReceiptLong,
                    if (query.isNotBlank()) "Sonuç bulunamadı" else "Bu dönemde işlem yok",
                    if (query.isNotBlank()) "Farklı bir kelime veya tutar dene." else "Yeni bir işlem eklemek için + düğmesine dokun."
                )
            }
            grouped.forEach { (day, list) ->
                item(key = "h$day") {
                    val dayExpense = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                    val dayIncome = list.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(day.dayHeader(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (dayIncome > 0) Text("+${Money.format(dayIncome)}  ", style = MaterialTheme.typography.labelMedium, color = LocalFinanceColors.current.income)
                        if (dayExpense > 0) Text("−${Money.format(dayExpense)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(list, key = { it.id }) { tx ->
                    SwipeToDelete(onDelete = { vm.deleteTransaction(tx, false) }) {
                        Box(Modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.background)) {
                            TransactionRow(tx, data, onClick = { nav.navigate(Routes.tx(tx.id)) }, showDate = query.isNotBlank())
                        }
                    }
                }
            }
        }
    }
}

private fun matches(tx: Transaction, q: String, data: AppData): Boolean {
    if (tx.note.lowercase(trLocale).contains(q)) return true
    if (tx.categoryId?.let { data.categoryMap[it]?.name?.lowercase(trLocale)?.contains(q) } == true) return true
    if (data.accountMap[tx.accountId]?.name?.lowercase(trLocale)?.contains(q) == true) return true
    val amount = Money.parse(q)
    return amount != null && (tx.amount == amount || Money.toInput(tx.amount).startsWith(q.replace('.', ',')))
}

@Composable
private fun SummaryCell(label: String, value: Long, color: Color, modifier: Modifier, signed: Boolean = false) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Money.format(value, signed = signed), style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FilterMenu(label: String, selected: Boolean, options: List<Pair<Long, String>>, onSelect: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(selected = selected, onClick = { open = true }, label = { Text(label, maxLines = 1) })
        DropdownMenu(open, onDismissRequest = { open = false }) {
            options.forEach { (id, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); open = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(confirmValueChange = {
        if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
    })
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium).background(LocalFinanceColors.current.expense.copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White)
                }
            }
        }
    ) { content() }
}
