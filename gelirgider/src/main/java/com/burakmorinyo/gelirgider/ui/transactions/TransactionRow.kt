package com.burakmorinyo.gelirgider.ui.transactions

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.short
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.components.AmountText
import com.burakmorinyo.gelirgider.ui.components.CategoryIcon
import com.burakmorinyo.gelirgider.ui.components.IconBadge
import com.burakmorinyo.gelirgider.ui.components.ListRow
import com.burakmorinyo.gelirgider.ui.components.Pill
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors

fun AppData.titleOf(tx: Transaction): String = when {
    tx.note.isNotBlank() -> tx.note
    tx.type == TxType.TRANSFER -> "Transfer"
    else -> tx.categoryId?.let { categoryMap[it]?.name } ?: "Kategorisiz"
}

fun AppData.subtitleOf(tx: Transaction, showDate: Boolean = false): String {
    val parts = mutableListOf<String>()
    if (tx.type == TxType.TRANSFER) {
        parts += "${accountMap[tx.accountId]?.name ?: "?"} → ${tx.toAccountId?.let { accountMap[it]?.name } ?: "?"}"
    } else {
        if (tx.note.isNotBlank()) tx.categoryId?.let { categoryMap[it]?.name }?.let { parts += it }
        accountMap[tx.accountId]?.name?.let { parts += it }
    }
    if (showDate) parts += tx.date.toLocalDate().short()
    return parts.joinToString(" · ")
}

@Composable
fun TransactionRow(tx: Transaction, data: AppData, onClick: () -> Unit, showDate: Boolean = false, modifier: Modifier = Modifier) {
    val cat = tx.categoryId?.let { data.categoryMap[it] }
    ListRow(
        leading = {
            if (tx.type == TxType.TRANSFER) IconBadge(Icons.AutoMirrored.Rounded.CompareArrows, LocalFinanceColors.current.transfer, 42.dp)
            else CategoryIcon(cat?.icon ?: "more", cat?.color ?: 0xFF94A3B8)
        },
        title = data.titleOf(tx),
        subtitle = data.subtitleOf(tx, showDate),
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (tx.installmentCount > 1) {
                    Pill("${tx.installmentIndex}/${tx.installmentCount}", MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                }
                AmountText(tx.amount, tx.type, MaterialTheme.typography.titleSmall, currency = data.currencyOf(tx.accountId).takeIf { it != data.baseCode })
            }
        }
    )
}
