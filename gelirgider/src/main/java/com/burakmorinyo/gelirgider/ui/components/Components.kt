package com.burakmorinyo.gelirgider.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.ui.theme.AmountStyle
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors

fun Long.asColor() = Color(this.toInt())

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val border = if (!LocalFinanceColors.current.isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier
    Surface(
        modifier = modifier.fillMaxWidth().then(border).clip(shape).let { if (onClick != null) it.clickable(onClick = onClick) else it },
        color = color,
        shape = shape
    ) {
        Column(Modifier.padding(padding).animateContentSize(), content = content)
    }
}

@Composable
fun CategoryIcon(icon: String?, color: Long, size: Dp = 42.dp, iconSize: Dp = size * 0.5f) {
    val c = color.asColor()
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.34f)).background(c.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(AppIcons.get(icon), contentDescription = null, tint = c, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun typeColor(type: TxType): Color = when (type) {
    TxType.EXPENSE -> LocalFinanceColors.current.expense
    TxType.INCOME -> LocalFinanceColors.current.income
    TxType.TRANSFER -> LocalFinanceColors.current.transfer
}

@Composable
fun AmountText(
    amount: Long,
    type: TxType? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    val text = when (type) {
        TxType.EXPENSE -> "−" + Money.format(amount)
        TxType.INCOME -> "+" + Money.format(amount)
        else -> Money.format(amount)
    }
    Text(
        text, modifier = modifier, maxLines = 1,
        style = style.merge(AmountStyle),
        color = color ?: if (type != null) typeColor(type) else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(action) }
        }
    }
}

/** Yuvarlatılmış, animasyonlu ilerleme çubuğu. ratio > 1 ise aşım rengiyle dolar. */
@Composable
fun ProgressBar(ratio: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 8.dp, track: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(700), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(
            Modifier.fillMaxWidth(animated).height(height).clip(CircleShape)
                .background(if (ratio > 1f) LocalFinanceColors.current.expense else color)
        )
    }
}

@Composable
fun budgetColor(ratio: Float): Color = when {
    ratio >= 1f -> LocalFinanceColors.current.expense
    ratio >= 0.8f -> LocalFinanceColors.current.warning
    else -> LocalFinanceColors.current.income
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(16.dp)); action() }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp),
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
    )
}

/** İki satırlı liste öğesi: ikon, başlık, alt başlık, sağda içerik */
@Composable
fun ListRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrEmpty()) Text(
                subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: Long, colors: List<Long>, onSelect: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c.asColor()).clickable { onSelect(c) },
                contentAlignment = Alignment.Center
            ) {
                if (c == selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(selected: String, color: Long, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppIcons.pickable.forEach { key ->
            val sel = key == selected
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (sel) color.asColor() else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.get(key), null, tint = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}
