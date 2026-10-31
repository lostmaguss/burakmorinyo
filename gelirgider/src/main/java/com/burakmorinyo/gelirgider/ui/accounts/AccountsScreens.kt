package com.burakmorinyo.gelirgider.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.DefaultData
import com.burakmorinyo.gelirgider.domain.AccountBalance
import com.burakmorinyo.gelirgider.domain.Currencies
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.formatIn
import com.burakmorinyo.gelirgider.domain.relativeDays
import com.burakmorinyo.gelirgider.domain.short
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AmountInput
import com.burakmorinyo.gelirgider.ui.components.AmountText
import com.burakmorinyo.gelirgider.ui.components.AppCard
import com.burakmorinyo.gelirgider.ui.components.CategoryIcon
import com.burakmorinyo.gelirgider.ui.components.ColorPicker
import com.burakmorinyo.gelirgider.ui.components.FormLabel
import com.burakmorinyo.gelirgider.ui.components.FormScreen
import com.burakmorinyo.gelirgider.ui.components.IconPicker
import com.burakmorinyo.gelirgider.ui.components.Pill
import com.burakmorinyo.gelirgider.ui.components.ProgressBar
import com.burakmorinyo.gelirgider.ui.components.SectionHeader
import com.burakmorinyo.gelirgider.ui.components.SwitchRow
import com.burakmorinyo.gelirgider.ui.components.TextInput
import com.burakmorinyo.gelirgider.ui.components.budgetColor
import com.burakmorinyo.gelirgider.ui.theme.AmountStyle
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors

@Composable
fun AccountsScreen(data: AppData, vm: MainViewModel, nav: NavHostController) {
    val fc = LocalFinanceColors.current
    val active = data.balances.filter { !it.account.archived }
    val inBase = { b: AccountBalance -> data.toBase(b.balance, b.account.currency) }
    val assets = active.filter { it.account.includeInTotal && it.balance > 0 }.sumOf(inBase)
    val debts = active.filter { it.account.includeInTotal && it.balance < 0 }.sumOf { -inBase(it) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
            Text("Hesaplar", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { nav.navigate(Routes.account()) }) { Icon(Icons.Rounded.Add, "Hesap ekle") }
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AppCard {
                    Text("Net varlık", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Money.format(data.netWorth), style = MaterialTheme.typography.headlineMedium.merge(AmountStyle))
                    netWorthEquivalents(data)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Varlıklar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(assets), color = fc.income, style = MaterialTheme.typography.titleSmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Borçlar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Money.format(debts), color = fc.expense, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            item { RatesCard(data, vm) }
            AccountType.entries.forEach { type ->
                val list = active.filter { it.account.type == type }
                if (list.isNotEmpty()) {
                    item { SectionHeader(type.label) }
                    list.forEach { b -> item(key = b.account.id) { AccountCard(b, data, onOpen = { nav.navigate(Routes.history(account = b.account.id)) }, onEdit = { nav.navigate(Routes.account(b.account.id)) }) } }
                }
            }
            val archived = data.balances.filter { it.account.archived }
            if (archived.isNotEmpty()) {
                item { SectionHeader("Arşiv") }
                archived.forEach { b -> item(key = b.account.id) { AccountCard(b, data, onOpen = { nav.navigate(Routes.history(account = b.account.id)) }, onEdit = { nav.navigate(Routes.account(b.account.id)) }) } }
            }
            item {
                FilledTonalButton(onClick = { nav.navigate(Routes.account()) }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Hesap / kart ekle")
                }
            }
        }
    }
}

@Composable
private fun AccountCard(b: AccountBalance, data: AppData, onOpen: () -> Unit, onEdit: () -> Unit) {
    val a = b.account
    val fc = LocalFinanceColors.current
    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(a.icon, a.color, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (!a.includeInTotal) { Spacer(Modifier.width(6.dp)); Pill("toplama dahil değil", MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                val foreign = a.currency != data.baseCode
                Text(if (foreign) "${a.type.label} · ${Currencies.get(a.currency).name}" else a.type.label,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                AmountText(b.balance, style = MaterialTheme.typography.titleMedium, color = if (b.balance < 0) fc.expense else null,
                    currency = a.currency)
                if (a.currency != data.baseCode && data.rates.tryPer(a.currency) != null) Text(
                    "≈ ${Money.format(data.toBase(b.balance, a.currency))}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.material3.TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(28.dp)) { Text("Düzenle") }
            }
        }
        val card = b.card
        if (a.type == AccountType.CREDIT_CARD) {
            Spacer(Modifier.height(10.dp))
            if (a.creditLimit > 0) {
                val used = (a.creditLimit - (card?.available ?: (a.creditLimit + b.balance))).coerceAtLeast(0)
                val ratio = used.toFloat() / a.creditLimit
                ProgressBar(ratio, budgetColor(ratio))
                Spacer(Modifier.height(6.dp))
                Row {
                    Text("Kullanılan ${Money.format(used)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("Limit ${Money.format(a.creditLimit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (b.futureInstallments > 0) Text(
                "Gelecek taksitler: ${Money.format(b.futureInstallments)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (card != null) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Ekstre borcu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(card.remainingDue), style = MaterialTheme.typography.titleSmall,
                            color = if (card.remainingDue > 0) fc.expense else fc.income)
                        Text("Son ödeme ${card.dueDate.short()} (${relativeDays(card.dueDate)})", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Dönem içi harcama", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(card.currentPeriodSpend), style = MaterialTheme.typography.titleSmall)
                        Text("Kesim ${card.nextStatement.short()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private val typeIcons = mapOf(
    AccountType.CASH to "wallet", AccountType.BANK to "bank", AccountType.CREDIT_CARD to "credit_card",
    AccountType.SAVINGS to "savings", AccountType.INVESTMENT to "trending_up"
)

@Composable
fun AccountEditScreen(data: AppData, vm: MainViewModel, nav: NavHostController, id: Long) {
    val existing = remember(id) { data.accounts.firstOrNull { it.id == id } }
    if (id != 0L && existing == null) return
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: AccountType.BANK) }
    var initial by rememberSaveable {
        mutableStateOf(existing?.let { Money.toInput(if (it.type == AccountType.CREDIT_CARD) -it.initialBalance else it.initialBalance) } ?: "")
    }
    var limit by rememberSaveable { mutableStateOf(existing?.let { if (it.creditLimit > 0) Money.toInput(it.creditLimit) else "" } ?: "") }
    var statement by rememberSaveable { mutableStateOf(existing?.statementDay?.takeIf { it > 0 }?.toString() ?: "") }
    var due by rememberSaveable { mutableStateOf(existing?.dueDay?.takeIf { it > 0 }?.toString() ?: "") }
    var color by rememberSaveable { mutableLongStateOf(existing?.color ?: 0xFF3B82F6) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "bank") }
    var include by rememberSaveable { mutableStateOf(existing?.includeInTotal ?: true) }
    var archived by rememberSaveable { mutableStateOf(existing?.archived ?: false) }
    var currency by rememberSaveable { mutableStateOf(existing?.currency ?: data.baseCode) }
    // İşlemi olan hesabın para birimi değiştirilemez (geçmiş tutarlar anlamsızlaşır)
    val currencyLocked = existing != null && data.transactions.any { it.accountId == existing.id || it.toAccountId == existing.id }
    var error by remember { mutableStateOf<String?>(null) }

    FormScreen(
        title = if (existing == null) "Yeni hesap" else "Hesabı düzenle",
        onBack = { nav.popBackStack() },
        onDelete = existing?.let { { vm.deleteAccount(it); nav.popBackStack() } },
        deleteMessage = "Hesap ve bu hesaba ait TÜM işlemler silinecek. Sadece gizlemek istersen “Arşivle” seçeneğini kullan.",
        onSave = {
            val sd = statement.toIntOrNull() ?: 0
            val dd = due.toIntOrNull() ?: 0
            error = when {
                name.isBlank() -> "Hesap adı gerekli"
                type == AccountType.CREDIT_CARD && (sd !in 1..31 || dd !in 1..31) -> "Hesap kesim ve son ödeme günü 1-31 arası olmalı"
                else -> null
            }
            if (error != null) return@FormScreen
            val entered = Money.parse(initial) ?: 0
            vm.saveAccount(
                (existing ?: Account(name = "", type = type, color = color, icon = icon)).copy(
                    name = name.trim(), type = type, color = color, icon = icon,
                    initialBalance = if (type == AccountType.CREDIT_CARD) -entered else entered,
                    creditLimit = if (type == AccountType.CREDIT_CARD) Money.parse(limit) ?: 0 else 0,
                    statementDay = if (type == AccountType.CREDIT_CARD) sd else 0,
                    dueDay = if (type == AccountType.CREDIT_CARD) dd else 0,
                    includeInTotal = include, archived = archived,
                    currency = if (type == AccountType.CREDIT_CARD) data.baseCode else currency,
                    sortOrder = existing?.sortOrder ?: data.accounts.size
                )
            )
            nav.popBackStack()
        }
    ) {
        FormLabel("Hesap türü")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AccountType.entries.forEach { t ->
                FilterChip(type == t, { type = t; if (existing == null) icon = typeIcons[t] ?: icon }, label = { Text(t.label) })
            }
        }
        TextInput(name, { name = it }, "Hesap adı", supporting = if (type == AccountType.CREDIT_CARD) "Ör. Bonus Kart, Maximum, World" else null)
        if (type != AccountType.CREDIT_CARD) {
            FormLabel("Para birimi")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Currencies.all.forEach { c ->
                    FilterChip(
                        currency == c.code, enabled = !currencyLocked || currency == c.code,
                        onClick = {
                            currency = c.code
                            if (existing == null && Currencies.isGold(c.code)) { icon = "diamond"; color = 0xFFF59E0B }
                            else if (existing == null && c.code != data.baseCode) icon = "exchange"
                        },
                        label = { Text(c.name) }
                    )
                }
            }
            if (currencyLocked) Text("İşlemi olan hesabın para birimi değiştirilemez.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (type == AccountType.CREDIT_CARD) {
            AmountInput(initial, { initial = it }, "Mevcut kart borcu",
                supporting = "Kartın bugünkü toplam borcunu yaz (taksitler dahil); eksi bakiye olarak kaydedilir")
            AmountInput(limit, { limit = it }, "Kart limiti")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    statement, { statement = it.filter(Char::isDigit).take(2) }, label = { Text("Hesap kesim günü") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                androidx.compose.material3.OutlinedTextField(
                    due, { due = it.filter(Char::isDigit).take(2) }, label = { Text("Son ödeme günü") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
            Text("Kredi kartı ödemesini, banka hesabından bu karta “Transfer” olarak gir.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            AmountInput(
                initial, { initial = it },
                if (Currencies.isGold(currency)) "Elindeki miktar" else "Başlangıç bakiyesi",
                suffix = Currencies.get(currency).symbol,
                supporting = when {
                    currency == "GOLD_GRAM" -> "Kaç gram altının var? (ör. 12,5)"
                    currency == "GOLD_QUARTER" -> "Kaç çeyrek altının var?"
                    currency != data.baseCode -> "Hesaptaki ${Currencies.get(currency).name} miktarı"
                    else -> "Hesabın bugünkü bakiyesi"
                }
            )
        }
        FormLabel("Renk")
        ColorPicker(color, DefaultData.palette) { color = it }
        FormLabel("İkon")
        IconPicker(icon, color) { icon = it }
        SwitchRow("Toplam varlığa dahil et", "Kapatırsan bu hesap net varlık hesabına katılmaz", include) { include = it }
        if (existing != null) SwitchRow("Arşivle", "Hesap gizlenir ama işlemleri korunur", archived) { archived = it }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

/** "≈ 2.250 $ · 18,2 gr altın" — kur yoksa null */
fun netWorthEquivalents(data: AppData): String? {
    val parts = listOfNotNull(
        data.rates.convert(data.netWorth, data.baseCode, "USD")?.takeIf { data.baseCode != "USD" }?.let { formatIn(it, "USD") },
        data.rates.convert(data.netWorth, data.baseCode, "GOLD_GRAM")?.let { "${formatIn(it, "GOLD_GRAM")} altın" }
    )
    return if (parts.isEmpty()) null else "≈ " + parts.joinToString(" · ")
}

@Composable
private fun RatesCard(data: AppData, vm: MainViewModel) {
    val r = data.rates
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Güncel kurlar", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (r.updatedAt == 0L) "Henüz alınmadı — internet gerekli"
                    else "${r.source} · ${java.time.Instant.ofEpochMilli(r.updatedAt).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm", com.burakmorinyo.gelirgider.domain.trLocale))}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.IconButton(onClick = { vm.refreshRates() }) {
                Icon(Icons.Rounded.Refresh, "Kurları yenile", tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (!r.isEmpty) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Currencies.all.filter { it.code != data.baseCode }.forEach { c ->
                    val v = r.convert(100, c.code, data.baseCode) ?: return@forEach
                    Column(
                        Modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(if (c.code.startsWith("GOLD")) c.name else c.code, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(v, alwaysDecimals = v < 100_00, reveal = true), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
