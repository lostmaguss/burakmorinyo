package com.burakmorinyo.gelirgider.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.Category
import com.burakmorinyo.gelirgider.data.Frequency
import com.burakmorinyo.gelirgider.data.Recurring
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Calculator
import com.burakmorinyo.gelirgider.domain.Currencies
import com.burakmorinyo.gelirgider.domain.formatIn
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.dayHeader
import com.burakmorinyo.gelirgider.domain.nextOccurrence
import com.burakmorinyo.gelirgider.domain.splitInstallments
import com.burakmorinyo.gelirgider.domain.startMillis
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.domain.trLocale
import com.burakmorinyo.gelirgider.domain.withNowTime
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AmountInput
import com.burakmorinyo.gelirgider.ui.components.AppIcons
import com.burakmorinyo.gelirgider.ui.components.asColor
import com.burakmorinyo.gelirgider.ui.components.typeColor
import com.burakmorinyo.gelirgider.ui.theme.AmountStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(data: AppData, vm: MainViewModel, nav: NavHostController, id: Long, initialType: TxType, startScan: Boolean = false) {
    val existing = remember(id, data.loaded) { if (id != 0L) data.transactions.firstOrNull { it.id == id } else null }
    if (id != 0L && existing == null) return

    val defaultAccount = data.activeAccounts.firstOrNull { it.id == data.settings.lastAccountId }?.id
        ?: data.activeAccounts.firstOrNull()?.id ?: 0L

    var type by rememberSaveable { mutableStateOf(existing?.type ?: initialType) }
    var expr by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.amount) } ?: "") }
    var accountId by rememberSaveable { mutableLongStateOf(existing?.accountId ?: defaultAccount) }
    var toAccountId by rememberSaveable {
        mutableLongStateOf(existing?.toAccountId ?: data.activeAccounts.firstOrNull { it.id != accountId }?.id ?: 0L)
    }
    var categoryId by rememberSaveable { mutableLongStateOf(existing?.categoryId ?: 0L) }
    var dateEpochDay by rememberSaveable { mutableLongStateOf((existing?.date?.toLocalDate() ?: today()).toEpochDay()) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var installments by rememberSaveable { mutableIntStateOf(1) }
    var repeat by rememberSaveable { mutableStateOf(false) }
    var repeatFreq by rememberSaveable { mutableStateOf(Frequency.MONTHLY) }
    var noteFocused by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Farklı para birimli hesaplar arası transferde hedefe giren miktar (boşsa kurla hesaplanır)
    var toAmountText by rememberSaveable { mutableStateOf(existing?.toAmount?.let { Money.toInput(it) } ?: "") }
    var scanMenu by remember { mutableStateOf(startScan) }
    var scanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    val date = LocalDate.ofEpochDay(dateEpochDay)
    val color by animateColorAsState(typeColor(type), label = "type")
    val amount = Calculator.evaluate(expr) ?: 0L
    val account = data.accountMap[accountId]
    val isCard = account?.type == AccountType.CREDIT_CARD
    val isNew = existing == null
    val fromCode = data.currencyOf(accountId)
    val toCode = data.currencyOf(toAccountId)
    val crossCurrency = type == TxType.TRANSFER && toAccountId != 0L && fromCode != toCode
    val suggestedToAmount = if (crossCurrency) data.rates.convert(amount, fromCode, toCode) else null
    val toAmount = if (crossCurrency) Money.parse(toAmountText) ?: suggestedToAmount else null
    val focus = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val categories = remember(type, data) { data.categoriesByUsage(type) }
    // Klavye geri tuşuyla kapatılınca not alanı odağını bırakır, hesap makinesi geri gelir
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeWasVisible && !imeVisible) focus.clearFocus()
        imeWasVisible = imeVisible
    }

    // Aynı nota daha önce hangi kategori verildiyse onu öner
    val suggestions = remember(note, type, data) {
        if (note.length < 2) emptyList() else {
            val q = note.lowercase(trLocale)
            data.transactions.asSequence()
                .filter { it.type == type && it.note.isNotBlank() && it.note.lowercase(trLocale).contains(q) && it.note != note }
                .groupBy { it.note }
                .entries.sortedByDescending { it.value.size }
                .take(4)
                .map { (n, list) -> n to list.groupingBy { it.categoryId }.eachCount().maxByOrNull { it.value }?.key }
                .toList()
        }
    }

    fun save() {
        error = when {
            amount <= 0 -> "Tutar gir"
            accountId == 0L -> "Hesap seç"
            type == TxType.TRANSFER && (toAccountId == 0L || toAccountId == accountId) -> "Farklı bir hedef hesap seç"
            crossCurrency && (toAmount ?: 0) <= 0 -> "Hedefe giren miktarı yaz"
            type != TxType.TRANSFER && categoryId == 0L -> "Kategori seç"
            else -> null
        }
        if (error != null) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); return }
        val millis = if (existing != null && existing.date.toLocalDate() == date) existing.date else date.withNowTime()
        val tx = (existing ?: Transaction(type = type, amount = amount, accountId = accountId, date = millis)).copy(
            type = type,
            amount = amount,
            accountId = accountId,
            toAccountId = if (type == TxType.TRANSFER) toAccountId else null,
            categoryId = if (type == TxType.TRANSFER) null else categoryId,
            date = millis,
            note = note.trim(),
            toAmount = toAmount
        )
        val recurring = if (isNew && repeat && type != TxType.TRANSFER) Recurring(
            title = note.trim().ifBlank { data.categoryMap[categoryId]?.name ?: "Tekrarlayan" },
            type = type,
            amount = amount,
            accountId = accountId,
            categoryId = categoryId,
            frequency = repeatFreq,
            nextDate = nextOccurrence(date, repeatFreq, date.dayOfMonth).startMillis(),
            anchorDay = date.dayOfMonth,
            autoCreate = true,
            isSubscription = data.categoryMap[categoryId]?.icon == "subscriptions"
        ) else null
        vm.saveTransaction(tx, if (isNew && isCard && type == TxType.EXPENSE) installments else 1, recurring)
        vm.message(
            when {
                installments > 1 && isNew && isCard && type == TxType.EXPENSE -> "$installments taksit olarak kaydedildi"
                recurring != null -> "Kaydedildi, ${repeatFreq.label.lowercase(trLocale)} tekrarlanacak"
                else -> if (isNew) "Kaydedildi" else "Güncellendi"
            }
        )
        nav.popBackStack()
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().imePadding()) {
        // Üst çubuk + tür seçici
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Rounded.Close, "Kapat") }
            TypeSelector(type, enabled = isNew || existing?.installmentGroup == null, modifier = Modifier.weight(1f)) {
                type = it; categoryId = 0L; error = null
            }
            if (!isNew) IconButton(onClick = { showDelete = true }) { Icon(Icons.Rounded.DeleteOutline, "Sil") }
            else Box {
                IconButton(onClick = { scanMenu = true }) { Icon(Icons.Rounded.DocumentScanner, "Fiş tara") }
                ReceiptScanMenu(
                    expanded = scanMenu,
                    onDismiss = { scanMenu = false },
                    onStart = { scanning = true; scanResult = null },
                    onResult = { info ->
                        scanning = false
                        if (info == null) { scanResult = "Fiş okunamadı. Daha aydınlık bir yerde, fişi düz tutarak tekrar dene."; return@ReceiptScanMenu }
                        type = TxType.EXPENSE
                        info.total?.let { expr = Money.toInput(it) }
                        info.date?.let { dateEpochDay = it.toEpochDay() }
                        info.merchant?.let { note = it }
                        // Kategori: önce bu mağazaya daha önce verdiğin kategori, sonra fişten tahmin
                        val learned = info.merchant?.let { m ->
                            data.transactions.filter { it.note.equals(m, ignoreCase = true) && it.categoryId != null }
                                .groupingBy { it.categoryId!! }.eachCount().maxByOrNull { it.value }?.key
                        }
                        val guessed = info.categoryName?.let { n -> data.categoriesOf(TxType.EXPENSE).firstOrNull { it.name == n }?.id }
                        (learned ?: guessed)?.let { categoryId = it }
                        error = null
                        scanResult = listOfNotNull(
                            info.total?.let { "Toplam ${Money.format(it, reveal = true)}" } ?: "Toplam bulunamadı",
                            info.merchant, info.date?.dayHeader()
                        ).joinToString(" · ") + " — kontrol edip kaydet."
                    }
                )
            }
        }

        // Tutar
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val shown = expr.ifEmpty { "0" }
            Text(
                "$shown ${Currencies.get(fromCode).symbol}",
                style = MaterialTheme.typography.displaySmall.merge(AmountStyle).copy(fontSize = if (shown.length > 12) 30.sp else 42.sp),
                color = color, maxLines = 1, textAlign = TextAlign.Center
            )
            AnimatedVisibility(Calculator.hasOperator(expr)) {
                Text("= ${Money.format(amount, reveal = true, symbol = Currencies.get(fromCode).symbol)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isNew && isCard && type == TxType.EXPENSE && installments > 1 && amount > 0) {
                Text(
                    "$installments × ${Money.format(splitInstallments(amount, installments).last(), reveal = true)}",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge) }
            if (scanning) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Fiş okunuyor…", style = MaterialTheme.typography.labelLarge)
            }
            scanResult?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Hesap, hedef hesap, tarih
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountPicker(data, accountId, if (type == TxType.TRANSFER) "Kaynak" else null) { accountId = it; error = null }
                if (type == TxType.TRANSFER) AccountPicker(data, toAccountId, "Hedef") { toAccountId = it; error = null }
                ChipButton(Icons.Rounded.CalendarMonth, date.dayHeader()) { showDatePicker = true }
                if (date != today()) ChipButton(null, "Bugün") { dateEpochDay = today().toEpochDay() }
                else ChipButton(null, "Dün") { dateEpochDay = today().minusDays(1).toEpochDay() }
            }
            if (crossCurrency) {
                Spacer(Modifier.height(10.dp))
                AmountInput(
                    toAmountText, { toAmountText = it },
                    "${data.accountMap[toAccountId]?.name} hesabına giren",
                    suffix = Currencies.get(toCode).symbol,
                    supporting = suggestedToAmount?.let { "Güncel kurla ≈ ${formatIn(it, toCode)}. Gerçek alış miktarını yazabilirsin." }
                        ?: "Kur bilgisi yok; hedef hesaba giren miktarı yaz."
                )
            }
            Spacer(Modifier.height(10.dp))

            TextField(
                value = note, onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth().onFocusChanged { noteFocused = it.isFocused },
                placeholder = { Text(if (type == TxType.TRANSFER) "Açıklama (isteğe bağlı)" else "Not ekle (ör. Migros, kira, Netflix)") },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
            if (suggestions.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.forEach { (n, cat) ->
                        FilterChip(selected = false, onClick = {
                            note = n
                            if (cat != null && type != TxType.TRANSFER) categoryId = cat
                            focus.clearFocus()
                        }, label = { Text(n, maxLines = 1) })
                    }
                }
            }

            if (type != TxType.TRANSFER) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Kategori", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { nav.navigate(Routes.CATEGORIES) }) { Text("Düzenle") }
                }
                CategoryGrid(categories, categoryId) { categoryId = it; error = null; focus.clearFocus() }
            }

            if (isNew && isCard && type == TxType.EXPENSE) {
                Spacer(Modifier.height(14.dp))
                Text("Taksit", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 3, 4, 6, 9, 12, 18, 24).forEach { n ->
                        FilterChip(selected = installments == n, onClick = { installments = n },
                            label = { Text(if (n == 1) "Tek çekim" else "$n ay") })
                    }
                }
                if (installments > 1) Text(
                    "Her ay ${account?.name} hesabına bir taksit düşer; bütçe ve grafiklerde ilgili aya yazılır.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (existing?.installmentCount?.let { it > 1 } == true) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Bu işlem ${existing.installmentCount} taksitli bir alışverişin ${existing.installmentIndex}. taksiti. Değişiklik sadece bu taksite uygulanır.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isNew && type != TxType.TRANSFER) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { repeat = !repeat }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Repeat, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tekrarla", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Kira, maaş, fatura gibi düzenli işlemler kendiliğinden eklensin",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = repeat, onCheckedChange = { repeat = it })
                }
                AnimatedVisibility(repeat) {
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Frequency.entries.forEach { f ->
                            FilterChip(selected = repeatFreq == f, onClick = { repeatFreq = f }, label = { Text(f.label) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!noteFocused) {
            Keypad(
                onKey = { k -> expr = applyKey(expr, k); error = null; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                onClear = { expr = "" }
            )
        }
        Button(
            onClick = ::save,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
        ) {
            Text(if (isNew) "Kaydet" else "Güncelle", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Vazgeç") } }
        ) { DatePicker(state) }
    }

    if (showDelete && existing != null) {
        val grouped = existing.installmentGroup != null
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("İşlem silinsin mi?") },
            text = { Text(if (grouped) "Bu bir taksit. Sadece bu taksiti mi, alışverişin tüm taksitlerini mi silmek istersin?" else "Bu işlem kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteTransaction(existing, grouped); showDelete = false; nav.popBackStack() }) {
                    Text(if (grouped) "Tüm taksitler" else "Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                if (grouped) TextButton(onClick = { vm.deleteTransaction(existing, false); showDelete = false; nav.popBackStack() }) { Text("Sadece bu") }
                else TextButton(onClick = { showDelete = false }) { Text("Vazgeç") }
            }
        )
    }
}

/** Hesap makinesi tuşlarını ifadeye uygular */
internal fun applyKey(expr: String, key: Char): String {
    val ops = Calculator.operators
    val last = expr.lastOrNull()
    val currentNumber = expr.takeLastWhile { it !in ops }
    return when {
        key == '⌫' -> expr.dropLast(1)
        key in ops -> when {
            expr.isEmpty() -> expr
            last in ops -> expr.dropLast(1) + key
            last == ',' -> expr.dropLast(1) + key
            else -> expr + key
        }
        key == ',' -> when {
            currentNumber.contains(',') -> expr
            currentNumber.isEmpty() -> "${expr}0,"
            else -> expr + key
        }
        else -> when {
            currentNumber.contains(',') && currentNumber.substringAfter(',').length >= 2 -> expr
            currentNumber == "0" -> expr.dropLast(1) + key
            currentNumber.length >= 10 -> expr
            else -> expr + key
        }
    }
}

@Composable
private fun TypeSelector(type: TxType, enabled: Boolean, modifier: Modifier, onChange: (TxType) -> Unit) {
    Row(
        modifier.padding(horizontal = 8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp)
    ) {
        listOf(TxType.EXPENSE to "Gider", TxType.INCOME to "Gelir", TxType.TRANSFER to "Transfer").forEach { (t, label) ->
            val sel = t == type
            val bg by animateColorAsState(if (sel) typeColor(t) else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).clip(CircleShape).background(bg)
                    .clickable(enabled = enabled || sel) { onChange(t) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ChipButton(icon: ImageVector?, text: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun AccountPicker(data: AppData, selected: Long, label: String?, onSelect: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val acc = data.accountMap[selected]
    Box {
        Row(
            Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { open = true }
                .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background((acc?.color ?: 0xFF94A3B8).asColor().copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.get(acc?.icon), null, tint = (acc?.color ?: 0xFF94A3B8).asColor(), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text((label?.let { "$it: " } ?: "") + (acc?.name ?: "Hesap seç"), style = MaterialTheme.typography.labelLarge, maxLines = 1)
            Icon(Icons.Rounded.ExpandMore, null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(open, onDismissRequest = { open = false }) {
            data.activeAccounts.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.name) },
                    leadingIcon = { Icon(AppIcons.get(a.icon), null, tint = a.color.asColor()) },
                    onClick = { onSelect(a.id); open = false }
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<Category>, selected: Long, onSelect: (Long) -> Unit) {
    val columns = 4
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c ->
                    val sel = c.id == selected
                    val col = c.color.asColor()
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) col.copy(alpha = 0.16f) else Color.Transparent)
                            .border(if (sel) 1.5.dp else 0.dp, if (sel) col else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { onSelect(c.id) }.padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (sel) col else col.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(AppIcons.get(c.icon), null, tint = if (sel) Color.White else col, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.height(4.dp))
                        Text(c.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Keypad(onKey: (Char) -> Unit, onClear: () -> Unit) {
    val rows = listOf("789÷", "456×", "123−", ",0⌫+")
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                r.forEach { k ->
                    val isOp = k in Calculator.operators
                    val bg = if (isOp) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    Box(
                        Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                            .then(
                                if (k == '⌫') Modifier.combinedClickableCompat(onClick = { onKey(k) }, onLongClick = onClear)
                                else Modifier.clickable { onKey(k) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (k == '⌫') Icon(Icons.AutoMirrored.Rounded.Backspace, "Sil", modifier = Modifier.size(22.dp))
                        else Text(
                            k.toString(), fontSize = 22.sp, fontWeight = if (isOp) FontWeight.Bold else FontWeight.Medium,
                            color = if (isOp) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit, onLongClick: () -> Unit) =
    this.combinedClickable(onClick = onClick, onLongClick = onLongClick)
