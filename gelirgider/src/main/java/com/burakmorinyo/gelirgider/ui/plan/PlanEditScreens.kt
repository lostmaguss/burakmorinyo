package com.burakmorinyo.gelirgider.ui.plan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.Debt
import com.burakmorinyo.gelirgider.data.DebtDirection
import com.burakmorinyo.gelirgider.data.DefaultData
import com.burakmorinyo.gelirgider.data.Frequency
import com.burakmorinyo.gelirgider.data.Goal
import com.burakmorinyo.gelirgider.data.Recurring
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.startMillis
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.components.AmountInput
import com.burakmorinyo.gelirgider.ui.components.CategoryIcon
import com.burakmorinyo.gelirgider.ui.components.ColorPicker
import com.burakmorinyo.gelirgider.ui.components.DateField
import com.burakmorinyo.gelirgider.ui.components.FormLabel
import com.burakmorinyo.gelirgider.ui.components.FormScreen
import com.burakmorinyo.gelirgider.ui.components.IconPicker
import com.burakmorinyo.gelirgider.ui.components.SwitchRow
import com.burakmorinyo.gelirgider.ui.components.TextInput
import java.time.LocalDate

/** Popüler abonelikler: tek dokunuşla doldurmak için */
private val popularSubscriptions = listOf(
    "Netflix", "Spotify", "YouTube Premium", "Disney+", "Amazon Prime", "BluTV", "Exxen", "iCloud", "Google One", "ChatGPT", "Xbox Game Pass", "PlayStation Plus"
)
private val popularBills = listOf("Kira", "Elektrik", "Doğalgaz", "Su", "İnternet", "Telefon", "Aidat", "Kredi taksiti", "Sigorta", "Spor salonu")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringEditScreen(data: AppData, vm: MainViewModel, nav: NavHostController, id: Long, subscription: Boolean) {
    val existing = remember(id) { data.recurring.firstOrNull { it.id == id } }
    if (id != 0L && existing == null) return
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var type by rememberSaveable { mutableStateOf(existing?.type ?: TxType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.amount) } ?: "") }
    var freq by rememberSaveable { mutableStateOf(existing?.frequency ?: Frequency.MONTHLY) }
    var next by rememberSaveable { mutableLongStateOf((existing?.nextDate?.toLocalDate() ?: today()).toEpochDay()) }
    var end by rememberSaveable { mutableStateOf(existing?.endDate?.toLocalDate()?.toEpochDay()) }
    var auto by rememberSaveable { mutableStateOf(existing?.autoCreate ?: subscription) }
    var remind by rememberSaveable { mutableIntStateOf(existing?.remindDaysBefore ?: 1) }
    var isSub by rememberSaveable { mutableStateOf(existing?.isSubscription ?: subscription) }
    var active by rememberSaveable { mutableStateOf(existing?.active ?: true) }
    val defaultCat = data.categoriesOf(TxType.EXPENSE).firstOrNull { it.icon == if (subscription) "subscriptions" else "bolt" }?.id ?: 0L
    var categoryId by rememberSaveable { mutableLongStateOf(existing?.categoryId ?: defaultCat) }
    var accountId by rememberSaveable { mutableLongStateOf(existing?.accountId ?: data.activeAccounts.firstOrNull()?.id ?: 0L) }
    var error by remember { mutableStateOf(false) }

    FormScreen(
        title = when { existing != null -> "Düzenle"; subscription -> "Yeni abonelik"; else -> "Düzenli işlem" },
        onBack = { nav.popBackStack() },
        onDelete = existing?.let { { vm.deleteRecurring(it); nav.popBackStack() } },
        deleteMessage = "Bu düzenli işlem silinecek. Daha önce kaydedilmiş işlemler kalır.",
        onSave = {
            val a = Money.parse(amount) ?: 0
            if (title.isBlank() || a <= 0 || accountId == 0L) { error = true; return@FormScreen }
            val nextDate = LocalDate.ofEpochDay(next)
            vm.saveRecurring(
                (existing ?: Recurring(title = "", type = type, amount = 0, accountId = 0, categoryId = null, frequency = freq,
                    nextDate = 0, anchorDay = 1)).copy(
                    title = title.trim(), type = type, amount = a, accountId = accountId,
                    categoryId = categoryId.takeIf { it != 0L }, frequency = freq,
                    nextDate = nextDate.startMillis(), anchorDay = nextDate.dayOfMonth,
                    autoCreate = auto, remindDaysBefore = remind, isSubscription = isSub && type == TxType.EXPENSE,
                    active = active, endDate = end?.let { LocalDate.ofEpochDay(it).startMillis() }
                )
            )
            vm.message("Kaydedildi")
            nav.popBackStack()
        }
    ) {
        if (existing == null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                (if (subscription) popularSubscriptions else popularBills).forEach { s ->
                    FilterChip(selected = title == s, onClick = { title = s }, label = { Text(s) })
                }
            }
        }
        if (!subscription || existing != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == TxType.EXPENSE, { type = TxType.EXPENSE; categoryId = 0L }, label = { Text("Gider") })
            FilterChip(type == TxType.INCOME, { type = TxType.INCOME; categoryId = 0L; isSub = false }, label = { Text("Gelir (maaş, kira geliri…)") })
        }
        TextInput(title, { title = it }, "Başlık")
        AmountInput(amount, { amount = it }, "Tutar", isError = error && (Money.parse(amount) ?: 0) <= 0,
            supporting = "Fatura gibi değişken tutarlarda ortalama yaz; ödediğinde düzeltebilirsin.")
        FormLabel("Sıklık")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Frequency.entries.forEach { f -> FilterChip(freq == f, { freq = f }, label = { Text(f.label) }) }
        }
        DateField(if (existing == null) "İlk ödeme günü" else "Sıradaki ödeme günü", LocalDate.ofEpochDay(next), { it?.let { d -> next = d.toEpochDay() } })
        FormLabel("Kategori")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            data.categoriesByUsage(type).forEach { c ->
                FilterChip(categoryId == c.id, { categoryId = c.id }, label = { Text(c.name) }, leadingIcon = { CategoryIcon(c.icon, c.color, 22.dp) })
            }
        }
        FormLabel("Hesap")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            data.activeAccounts.forEach { a -> FilterChip(accountId == a.id, { accountId = a.id }, label = { Text(a.name) }) }
        }
        SwitchRow("Otomatik kaydet", "Günü gelince işlem kendiliğinden eklenir. Kapalıysa sadece hatırlatırım, sen “Ödendi” dersin.", auto) { auto = it }
        FormLabel("Hatırlatma")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(-1 to "Yok", 0 to "O gün", 1 to "1 gün önce", 2 to "2 gün önce", 3 to "3 gün önce", 7 to "1 hafta önce").forEach { (v, l) ->
                FilterChip(remind == v, { remind = v }, label = { Text(l) })
            }
        }
        if (type == TxType.EXPENSE) SwitchRow("Abonelik", "Abonelik toplamında ve yıllık maliyet hesabında gösterilir", isSub) { isSub = it }
        DateField("Bitiş tarihi (isteğe bağlı)", end?.let { LocalDate.ofEpochDay(it) }, { end = it?.toEpochDay() }, clearable = true)
        if (existing != null) SwitchRow("Aktif", "Kapatırsan durdurulur, silinmez", active) { active = it }
        if (error) Text("Başlık, tutar ve hesap gerekli.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun GoalEditScreen(data: AppData, vm: MainViewModel, nav: NavHostController, id: Long) {
    val existing = remember(id) { data.goals.firstOrNull { it.id == id } }
    if (id != 0L && existing == null) return
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var target by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.target) } ?: "") }
    var saved by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.saved) } ?: "") }
    var deadline by rememberSaveable { mutableStateOf(existing?.deadline?.toLocalDate()?.toEpochDay()) }
    var color by rememberSaveable { mutableLongStateOf(existing?.color ?: 0xFF6366F1) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: "savings") }
    var error by remember { mutableStateOf(false) }

    FormScreen(
        title = if (existing == null) "Yeni hedef" else "Hedefi düzenle",
        onBack = { nav.popBackStack() },
        onDelete = existing?.let { { vm.deleteGoal(it); nav.popBackStack() } },
        onSave = {
            val t = Money.parse(target) ?: 0
            val s = Money.parse(saved) ?: 0
            if (name.isBlank() || t <= 0) { error = true; return@FormScreen }
            vm.saveGoal(
                (existing ?: Goal(name = "", target = 0, color = color, icon = icon)).copy(
                    name = name.trim(), target = t, saved = s, deadline = deadline?.let { LocalDate.ofEpochDay(it).startMillis() },
                    color = color, icon = icon, completed = s >= t
                )
            )
            nav.popBackStack()
        }
    ) {
        if (existing == null) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Acil durum fonu" to "savings", "Tatil" to "beach", "Araba" to "car", "Ev peşinatı" to "home", "Telefon" to "phone", "Düğün" to "diamond", "Eğitim" to "school")
                    .forEach { (n, i) -> FilterChip(name == n, { name = n; icon = i }, label = { Text(n) }) }
            }
        }
        TextInput(name, { name = it }, "Hedef adı")
        AmountInput(target, { target = it }, "Hedef tutar", isError = error && (Money.parse(target) ?: 0) <= 0)
        AmountInput(saved, { saved = it }, "Şu ana kadar biriken")
        DateField("Hedef tarih (isteğe bağlı)", deadline?.let { LocalDate.ofEpochDay(it) }, { deadline = it?.toEpochDay() }, clearable = true)
        FormLabel("Renk")
        ColorPicker(color, DefaultData.palette) { color = it }
        FormLabel("İkon")
        IconPicker(icon, color) { icon = it }
        if (error) Text("Ad ve hedef tutar gerekli.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun DebtEditScreen(data: AppData, vm: MainViewModel, nav: NavHostController, id: Long) {
    val existing = remember(id) { data.debts.firstOrNull { it.id == id } }
    if (id != 0L && existing == null) return
    var person by rememberSaveable { mutableStateOf(existing?.person ?: "") }
    var direction by rememberSaveable { mutableStateOf(existing?.direction ?: DebtDirection.OWED_TO_ME) }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.amount) } ?: "") }
    var paid by rememberSaveable { mutableStateOf(existing?.let { Money.toInput(it.paid) } ?: "") }
    var due by rememberSaveable { mutableStateOf(existing?.dueDate?.toLocalDate()?.toEpochDay()) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf(false) }

    FormScreen(
        title = if (existing == null) "Borç / alacak" else "Düzenle",
        onBack = { nav.popBackStack() },
        onDelete = existing?.let { { vm.deleteDebt(it); nav.popBackStack() } },
        onSave = {
            val a = Money.parse(amount) ?: 0
            if (person.isBlank() || a <= 0) { error = true; return@FormScreen }
            vm.saveDebt(
                (existing ?: Debt(person = "", direction = direction, amount = 0)).copy(
                    person = person.trim(), direction = direction, amount = a,
                    paid = (Money.parse(paid) ?: 0).coerceIn(0, a),
                    dueDate = due?.let { LocalDate.ofEpochDay(it).startMillis() }, note = note.trim()
                )
            )
            nav.popBackStack()
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(direction == DebtDirection.OWED_TO_ME, { direction = DebtDirection.OWED_TO_ME }, label = { Text("Ben verdim (alacak)") })
            FilterChip(direction == DebtDirection.I_OWE, { direction = DebtDirection.I_OWE }, label = { Text("Ben aldım (borç)") })
        }
        TextInput(person, { person = it }, "Kişi")
        AmountInput(amount, { amount = it }, "Tutar", isError = error && (Money.parse(amount) ?: 0) <= 0)
        if (existing != null) AmountInput(paid, { paid = it }, "Ödenen kısım")
        DateField("Vade (isteğe bağlı)", due?.let { LocalDate.ofEpochDay(it) }, { due = it?.toEpochDay() }, clearable = true)
        TextInput(note, { note = it }, "Not", singleLine = false)
        if (error) Text("Kişi ve tutar gerekli.", color = MaterialTheme.colorScheme.error)
    }
}
