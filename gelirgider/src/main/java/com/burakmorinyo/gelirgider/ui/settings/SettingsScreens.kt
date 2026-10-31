package com.burakmorinyo.gelirgider.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.burakmorinyo.gelirgider.data.Category
import com.burakmorinyo.gelirgider.data.DefaultData
import com.burakmorinyo.gelirgider.data.ThemeMode
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.Routes
import com.burakmorinyo.gelirgider.ui.components.AppCard
import com.burakmorinyo.gelirgider.ui.components.CategoryIcon
import com.burakmorinyo.gelirgider.ui.components.ColorPicker
import com.burakmorinyo.gelirgider.ui.components.FormLabel
import com.burakmorinyo.gelirgider.ui.components.IconPicker
import com.burakmorinyo.gelirgider.ui.components.ListRow
import com.burakmorinyo.gelirgider.ui.components.SectionHeader
import com.burakmorinyo.gelirgider.ui.components.SwitchRow
import com.burakmorinyo.gelirgider.ui.components.TextInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun SettingsScreen(data: AppData, vm: MainViewModel, nav: NavHostController) {
    val s = data.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<String?>(null) }
    val mergeJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                vm.mergeJson(text)
            }.onSuccess { vm.message("$it işlem eklendi") }
                .onFailure { vm.message(it.message ?: "Dosya içe aktarılamadı") }
        }
    }
    var day by remember(s.periodStartDay) { mutableFloatStateOf(s.periodStartDay.toFloat()) }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = vm.exportJson()
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
            }.onSuccess { vm.message("Yedek kaydedildi") }.onFailure { vm.message("Yedek alınamadı: ${it.message}") }
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = vm.exportCsv()
                withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
            }.onSuccess { vm.message("CSV dosyası kaydedildi") }.onFailure { vm.message("Dışa aktarılamadı: ${it.message}") }
        }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
            }.onSuccess { pendingRestore = it }.onFailure { vm.message("Dosya okunamadı") }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Ayarlar", { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionHeader("Görünüm") }
            item {
                AppCard {
                    FormLabel("Tema")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { m ->
                            FilterChip(s.themeMode == m, { vm.updateSettings { it.copy(themeMode = m) } }, label = { Text(m.label) })
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(Modifier.height(6.dp))
                        SwitchRow("Duvar kağıdı renkleri", "Material You: renkler duvar kağıdına uyum sağlar", s.dynamicColor) { v ->
                            vm.updateSettings { it.copy(dynamicColor = v) }
                        }
                    }
                }
            }
            item { SectionHeader("Bütçe dönemi ve para birimi") }
            item {
                AppCard {
                    FormLabel("Maaş günü: ayın ${day.toInt()}'${suffix(day.toInt())}")
                    Text(
                        if (day.toInt() == 1) "Dönemler takvim ayına göre hesaplanır."
                        else "Dönemler her ayın ${day.toInt()}. gününden bir sonraki ayın ${day.toInt() - 1}. gününe kadar sürer.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(day, { day = it }, valueRange = 1f..28f, steps = 26,
                        onValueChangeFinished = { vm.updateSettings { it.copy(periodStartDay = day.toInt()) } })
                    FormLabel("Para birimi")
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("₺", "$", "€", "£", "CHF").forEach { c ->
                            FilterChip(s.currency == c, { vm.updateSettings { it.copy(currency = c) } }, label = { Text(c) })
                        }
                    }
                }
            }
            item { SectionHeader("Enflasyon ve kurlar") }
            item {
                var text by remember(s.annualInflation) {
                    mutableStateOf(s.annualInflation?.let { java.math.BigDecimal(it * 100).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',') } ?: "")
                }
                AppCard {
                    FormLabel("Yıllık enflasyon (TÜFE)")
                    Text(
                        "TÜİK her ayın 3'ünde açıklar. Girersen Analiz ekranında harcama ve gelirlerin enflasyondan arındırılmış (reel) değişimini gösteririm.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = text,
                        onValueChange = { t ->
                            text = t.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
                            vm.updateSettings { it.copy(annualInflation = text.replace(',', '.').toDoubleOrNull()?.takeIf { v -> v in 0.0..1000.0 }?.div(100)) }
                        },
                        label = { Text("Yıllık oran") }, prefix = { Text("%") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (s.rates.updatedAt == 0L) "Döviz ve altın kurları henüz alınmadı"
                            else "Kurlar: ${s.rates.source}, ${java.time.Instant.ofEpochMilli(s.rates.updatedAt).atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm", com.burakmorinyo.gelirgider.domain.trLocale))}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { vm.refreshRates() }) { Text("Yenile") }
                    }
                }
            }
            item { SectionHeader("Bildirimler") }
            item {
                AppCard {
                    SwitchRow("Günlük hatırlatma", "O gün hiç işlem girmediysen akşam hatırlatır", s.dailyReminder) { v -> vm.updateSettings { it.copy(dailyReminder = v) } }
                    if (s.dailyReminder) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(18, 19, 20, 21, 22, 23).forEach { h ->
                            FilterChip(s.reminderHour == h, { vm.updateSettings { it.copy(reminderHour = h) } }, label = { Text("$h:00") })
                        }
                    }
                    SwitchRow("Bütçe uyarıları", "Bütçenin %80'i dolunca ve aşılınca", s.budgetAlerts) { v -> vm.updateSettings { it.copy(budgetAlerts = v) } }
                    SwitchRow("Ödeme hatırlatmaları", "Faturalar, abonelikler ve kredi kartı son ödeme günleri", s.paymentReminders) { v ->
                        vm.updateSettings { it.copy(paymentReminders = v) }
                    }
                }
            }
            item { SectionHeader("Gizlilik ve güvenlik") }
            item {
                AppCard {
                    SwitchRow("Uygulama kilidi", "Açılışta parmak izi, yüz tanıma veya ekran kilidi ister", s.appLock) { v -> vm.updateSettings { it.copy(appLock = v) } }
                    SwitchRow("Tutarları gizle", "Kalabalık ortamlar için: tüm tutarlar yıldızlı görünür", s.hideAmounts) { v -> vm.updateSettings { it.copy(hideAmounts = v) } }
                }
            }
            item { SectionHeader("Veriler") }
            item {
                AppCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    NavRow(Icons.Rounded.Category, "Kategoriler", "Ekle, düzenle, renk ve ikon seç") { nav.navigate(Routes.CATEGORIES) }
                    NavRow(Icons.Rounded.CloudUpload, "Yedek al", "Tüm verileri tek bir dosyaya kaydet (Drive'a da kaydedebilirsin)") {
                        exportJson.launch("gelirgider-yedek-${LocalDate.now()}.json")
                    }
                    NavRow(Icons.Rounded.CloudDownload, "İçe aktar (silmeden ekle)", "Taksit listesi veya başka bir yedekteki kayıtları mevcut verilerine ekle") {
                        mergeJson.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                    }
                    NavRow(Icons.Rounded.CloudDownload, "Yedekten geri yükle", "Mevcut verilerin yerine yedektekiler gelir") {
                        importJson.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                    }
                    NavRow(Icons.Rounded.TableChart, "Excel'e aktar (CSV)", "Tüm işlemleri tablo olarak dışa aktar") {
                        exportCsv.launch("gelirgider-islemler-${LocalDate.now()}.csv")
                    }
                }
            }
            item {
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
                }
                Text(
                    "Gelir Gider $version · Veriler sadece bu telefonda saklanır.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    pendingRestore?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Yedek geri yüklensin mi?") },
            text = { Text("Telefondaki mevcut tüm kayıtlar silinip yedekteki veriler yüklenecek. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        runCatching { vm.importJson(text) }
                            .onSuccess { vm.message("Yedek geri yüklendi") }
                            .onFailure { vm.message(it.message ?: "Geçersiz yedek dosyası") }
                    }
                }) { Text("Geri yükle", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Vazgeç") } }
        )
    }
}

/** Türkçe sayı eki: 1'i, 2'si, 3'ü, 4'ü, 5'i, 6'sı, 9'u, 10'u ... */
private fun suffix(n: Int): String {
    val last = n % 10
    val tens = n % 100 / 10
    return when {
        last == 0 && tens in setOf(1, 3) -> "u"
        last == 0 && tens == 2 -> "si"
        last in setOf(1, 5, 8) -> "i"
        last in setOf(3, 4) -> "ü"
        last == 2 || last == 7 -> "si"
        last == 6 -> "sı"
        last == 9 -> "u"
        else -> "i"
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListRow(
        leading = {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        },
        title = title, subtitle = subtitle, onClick = onClick,
        trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

@Composable
fun CategoriesScreen(data: AppData, vm: MainViewModel, nav: NavHostController) {
    var type by remember { mutableStateOf(TxType.EXPENSE) }
    var editing by remember { mutableStateOf<Category?>(null) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Kategoriler", { nav.popBackStack() }) {
            IconButton(onClick = {
                editing = Category(name = "", type = type, icon = "more", color = DefaultData.palette.random(), sortOrder = data.categories.size)
            }) { Icon(Icons.Rounded.Add, "Kategori ekle") }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == TxType.EXPENSE, { type = TxType.EXPENSE }, label = { Text("Gider") })
            FilterChip(type == TxType.INCOME, { type = TxType.INCOME }, label = { Text("Gelir") })
        }
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                AppCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    data.categories.filter { it.type == type }.forEach { c ->
                        val count = data.transactions.count { it.categoryId == c.id }
                        ListRow(
                            leading = { CategoryIcon(c.icon, c.color) },
                            title = c.name,
                            subtitle = "$count işlem",
                            onClick = { editing = c },
                            trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }
                }
            }
        }
    }
    editing?.let { c ->
        CategoryDialog(c, onDismiss = { editing = null }, onSave = { vm.saveCategory(it); editing = null },
            onDelete = if (c.id != 0L) ({ vm.deleteCategory(c); editing = null }) else null)
    }
}

@Composable
private fun CategoryDialog(initial: Category, onDismiss: () -> Unit, onSave: (Category) -> Unit, onDelete: (() -> Unit)?) {
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var color by remember { mutableStateOf(initial.color) }
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(icon, color, 36.dp)
                Spacer(Modifier.width(10.dp))
                Text(if (initial.id == 0L) "Yeni kategori" else "Kategoriyi düzenle")
            }
        },
        text = {
            Column(Modifier.height(420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(name, { name = it }, "Ad")
                FormLabel("Renk")
                ColorPicker(color, DefaultData.palette) { color = it }
                FormLabel("İkon")
                IconPicker(icon, color) { icon = it }
                if (confirmDelete) Text(
                    "Silinirse bu kategorideki işlemler “Kategorisiz” olur ve kategori bütçesi kaldırılır. Silmek için tekrar dokun.",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(initial.copy(name = name.trim(), icon = icon, color = color)) }) { Text("Kaydet") }
        },
        dismissButton = {
            if (onDelete != null) TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                Text("Sil", color = MaterialTheme.colorScheme.error)
            } else TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
