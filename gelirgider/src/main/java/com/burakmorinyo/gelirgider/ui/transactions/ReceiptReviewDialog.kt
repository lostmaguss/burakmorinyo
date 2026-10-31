package com.burakmorinyo.gelirgider.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.ReceiptInfo
import com.burakmorinyo.gelirgider.domain.full

/**
 * Fişten okunanları gösterir: toplam yanlış seçildiyse fişteki diğer tutarlardan biri seçilebilir,
 * okunan ham metin de görülebilir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptReviewDialog(info: ReceiptInfo, onDismiss: () -> Unit, onApply: (Long?) -> Unit) {
    var selected by remember { mutableStateOf(info.total ?: info.amounts.firstOrNull()) }
    var showRaw by remember { mutableStateOf(info.isEmpty) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fişten okunanlar") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row2("Mağaza", info.merchant ?: "bulunamadı")
                Row2("Tarih", info.date?.full() ?: "bulunamadı")
                Text("Toplam tutar", style = MaterialTheme.typography.labelLarge)
                if (info.amounts.isEmpty()) Text("Fişte tutar bulunamadı; tutarı kendin yazabilirsin.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else {
                    if (info.total == null) Text("Toplam satırı bulunamadı; doğru tutara dokun.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        info.amounts.forEach { a ->
                            FilterChip(selected == a, { selected = a }, label = {
                                Text(Money.format(a, reveal = true), fontWeight = if (a == info.total) FontWeight.Bold else FontWeight.Normal)
                            })
                        }
                    }
                }
                TextButton(onClick = { showRaw = !showRaw }) { Text(if (showRaw) "Okunan metni gizle" else "Okunan metni göster") }
                if (showRaw) Text(
                    info.rawText.ifBlank { "(boş)" },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).horizontalScroll(rememberScrollState()).padding(8.dp),
                    style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = { TextButton(onClick = { onApply(selected) }) { Text("Uygula") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun Row2(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
