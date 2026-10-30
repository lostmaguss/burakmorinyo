package com.burakmorinyo.gelirgider.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.ui.theme.LocalFinanceColors

data class Slice(val value: Float, val color: Color)

@Composable
private fun rememberProgress(key: Any?): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(key) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    return anim.value
}

/** Halka grafik; ortasına içerik yerleştirilebilir */
@Composable
fun DonutChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    thickness: Dp = 22.dp,
    center: @Composable () -> Unit = {}
) {
    val progress = rememberProgress(slices.map { it.value })
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) return@Canvas
            val gap = if (slices.size > 1) 2.5f else 0f
            var start = -90f
            slices.forEach { s ->
                val sweep = s.value / total * 360f * progress
                if (sweep > gap) drawArc(s.color, start + gap / 2, sweep - gap, false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        center()
    }
}

/** Halka şeklinde ilerleme göstergesi (hedefler için) */
@Composable
fun RingProgress(ratio: Float, color: Color, modifier: Modifier = Modifier, size: Dp = 56.dp, thickness: Dp = 6.dp, content: @Composable () -> Unit = {}) {
    val progress = rememberProgress(ratio)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = thickness.toPx()
            val inset = stroke / 2
            val s = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), s, style = Stroke(stroke))
            drawArc(color, -90f, 360f * ratio.coerceIn(0f, 1f) * progress, false, Offset(inset, inset), s,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

data class BarGroup(val label: String, val values: List<Long>)

/**
 * Gruplanmış sütun grafik (ör. aylık gelir/gider). Sütuna dokununca değeri gösterir.
 */
@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    highlightLast: Boolean = true
) {
    val progress = rememberProgress(groups.map { it.values })
    var selected by remember(groups) { mutableIntStateOf(if (highlightLast) groups.lastIndex else -1) }
    val max = (groups.flatMap { it.values }.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val label = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        if (selected in groups.indices) {
            val g = groups[selected]
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(g.label, style = MaterialTheme.typography.labelLarge)
                g.values.forEachIndexed { i, v ->
                    Text(Money.format(v), style = MaterialTheme.typography.labelLarge, color = colors[i % colors.size], fontWeight = FontWeight.Bold)
                }
            }
        }
        Canvas(
            Modifier.fillMaxWidth().height(height).pointerInput(groups) {
                detectTapGestures { o ->
                    val w = size.width / groups.size.coerceAtLeast(1)
                    selected = (o.x / w).toInt().coerceIn(0, groups.lastIndex)
                }
            }
        ) {
            val n = groups.size.coerceAtLeast(1)
            val groupW = size.width / n
            val chartH = size.height
            for (k in 1..3) {
                val y = chartH * k / 4
                drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            }
            groups.forEachIndexed { gi, g ->
                val count = g.values.size.coerceAtLeast(1)
                val barW = (groupW * 0.62f / count).coerceAtMost(28.dp.toPx())
                val totalW = barW * count + (count - 1) * 4f
                var x = gi * groupW + (groupW - totalW) / 2
                val alpha = if (selected == -1 || selected == gi) 1f else 0.45f
                g.values.forEachIndexed { vi, v ->
                    val h = (v.toFloat() / max) * chartH * progress
                    if (h > 0f) drawRoundRect(
                        colors[vi % colors.size].copy(alpha = alpha),
                        topLeft = Offset(x, chartH - h), size = Size(barW, h),
                        cornerRadius = CornerRadius(barW / 2.6f, barW / 2.6f)
                    )
                    x += barW + 4f
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            groups.forEachIndexed { i, g ->
                Text(
                    g.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected) MaterialTheme.colorScheme.onSurface else label,
                    fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1
                )
            }
        }
    }
}

/**
 * Kümülatif harcama çizgisi + bütçe temposu (kesikli çizgi). [values] günlük harcamalardır.
 * [elapsed] günden sonrası çizilmez.
 */
@Composable
fun SpendingPaceChart(
    daily: List<Long>,
    elapsed: Int,
    budget: Long?,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val progress = rememberProgress(daily)
    val line = MaterialTheme.colorScheme.primary
    val over = LocalFinanceColors.current.expense
    val paceColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (daily.isEmpty()) return@Canvas
        val cumulative = daily.runningReduce { a, b -> a + b }
        val shown = cumulative.take(elapsed.coerceIn(1, daily.size))
        val max = maxOf(shown.lastOrNull() ?: 0L, budget ?: 0L, 1L).toFloat() * 1.08f
        val stepX = size.width / (daily.size - 1).coerceAtLeast(1)
        fun y(v: Long) = size.height - v / max * size.height

        if (budget != null && budget > 0) {
            drawLine(paceColor, Offset(0f, size.height), Offset(size.width, y(budget)), 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        }
        val path = Path()
        val fill = Path()
        val points = (shown.size * progress).toInt().coerceAtLeast(1)
        for (i in 0 until points) {
            val p = Offset(i * stepX, y(shown[i]))
            if (i == 0) { path.moveTo(p.x, p.y); fill.moveTo(p.x, size.height); fill.lineTo(p.x, p.y) }
            else { path.lineTo(p.x, p.y); fill.lineTo(p.x, p.y) }
        }
        val lastX = (points - 1) * stepX
        fill.lineTo(lastX, size.height); fill.close()
        val isOver = budget != null && budget > 0 && (shown.lastOrNull() ?: 0L) > budget * shown.size / daily.size
        val c = if (isOver) over else line
        drawPath(fill, Brush.verticalGradient(listOf(c.copy(alpha = 0.28f), c.copy(alpha = 0f))))
        drawPath(path, c, style = Stroke(4.dp.toPx() * 0.75f, cap = StrokeCap.Round))
        drawCircle(c, 5.dp.toPx(), Offset(lastX, y(shown[points - 1])))
        drawCircle(Color.White, 2.dp.toPx(), Offset(lastX, y(shown[points - 1])))
    }
}

/** Basit sütunlar (günlük harcama, hafta günleri) */
@Composable
fun SimpleBars(values: List<Long>, color: Color, modifier: Modifier = Modifier, height: Dp = 90.dp, highlight: Int = -1, labels: List<String>? = null) {
    val progress = rememberProgress(values)
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = values.size.coerceAtLeast(1)
            val slot = size.width / n
            val w = (slot * 0.6f).coerceAtMost(26.dp.toPx())
            values.forEachIndexed { i, v ->
                val x = i * slot + (slot - w) / 2
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(w / 2, w / 2))
                val h = v.toFloat() / max * size.height * progress
                if (h > 0) drawRoundRect(
                    if (highlight == -1 || highlight == i) color else color.copy(alpha = 0.55f),
                    Offset(x, size.height - h), Size(w, h), CornerRadius(w / 2, w / 2)
                )
            }
        }
        if (labels != null) Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            labels.forEachIndexed { i, l ->
                Text(l, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                    color = if (i == highlight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i == highlight) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** Çizgi grafik (net varlık seyri) */
@Composable
fun LineChart(values: List<Long>, color: Color, modifier: Modifier = Modifier, height: Dp = 110.dp) {
    val progress = rememberProgress(values)
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (values.size < 2) return@Canvas
        val min = values.min().toFloat()
        val max = values.max().toFloat()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val pad = 8.dp.toPx()
        fun y(v: Long) = pad + (1f - (v - min) / range) * (size.height - 2 * pad)
        val path = Path()
        val fill = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val yy = size.height - (size.height - y(v)) * progress
            if (i == 0) { path.moveTo(x, yy); fill.moveTo(x, size.height); fill.lineTo(x, yy) }
            else {
                val px = (i - 1) * stepX
                val py = size.height - (size.height - y(values[i - 1])) * progress
                path.cubicTo((px + x) / 2, py, (px + x) / 2, yy, x, yy)
                fill.cubicTo((px + x) / 2, py, (px + x) / 2, yy, x, yy)
            }
        }
        fill.lineTo(size.width, size.height); fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f))))
        drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        values.forEachIndexed { i, v ->
            drawCircle(color, 3.5.dp.toPx(), Offset(i * stepX, size.height - (size.height - y(v)) * progress))
        }
    }
}
