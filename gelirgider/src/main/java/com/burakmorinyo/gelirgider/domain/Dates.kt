package com.burakmorinyo.gelirgider.domain

import com.burakmorinyo.gelirgider.data.Frequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

val trLocale: Locale = Locale.forLanguageTag("tr-TR")

fun zone(): ZoneId = ZoneId.systemDefault()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(zone()).toLocalDate()
fun LocalDate.startMillis(): Long = atStartOfDay(zone()).toInstant().toEpochMilli()
/** İşlem tarihi: seçilen gün + şu anki saat (aynı gündeki sıralama korunur) */
fun LocalDate.withNowTime(): Long =
    if (this == LocalDate.now()) System.currentTimeMillis()
    else atTime(java.time.LocalTime.now()).atZone(zone()).toInstant().toEpochMilli()

fun today(): LocalDate = LocalDate.now()

/** Ayın gün sayısını aşan günleri ayın son gününe çeker (31 Şubat -> 28/29 Şubat) */
fun LocalDate.withDayClamped(day: Int): LocalDate = withDayOfMonth(day.coerceIn(1, lengthOfMonth()))

/**
 * Bütçe dönemi. Maaş günü 1 ise takvim ayı, değilse (örn. 15) 15 Ekim – 14 Kasım gibi.
 */
data class Period(val start: LocalDate, val endExclusive: LocalDate) {
    val startMillis get() = start.startMillis()
    val endMillis get() = endExclusive.startMillis()
    val days: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()
    operator fun contains(millis: Long) = millis in startMillis until endMillis
    operator fun contains(date: LocalDate) = !date.isBefore(start) && date.isBefore(endExclusive)

    fun previous(startDay: Int) = periodFor(start.minusDays(1), startDay)
    fun next(startDay: Int) = periodFor(endExclusive, startDay)

    /** Bugüne kadar geçen gün (bugün dahil), dönem dışında ise tamamı/0 */
    fun elapsedDays(today: LocalDate = today()): Int = when {
        today.isBefore(start) -> 0
        !today.isBefore(endExclusive) -> days
        else -> ChronoUnit.DAYS.between(start, today).toInt() + 1
    }

    fun label(startDay: Int): String {
        return if (startDay == 1) {
            val m = start.month.getDisplayName(TextStyle.FULL, trLocale).replaceFirstChar { it.titlecase(trLocale) }
            if (start.year == today().year) m else "$m ${start.year}"
        } else {
            val f = DateTimeFormatter.ofPattern("d MMM", trLocale)
            "${start.format(f)} – ${endExclusive.minusDays(1).format(f)}"
        }
    }
}

fun periodFor(date: LocalDate, startDay: Int): Period {
    val day = startDay.coerceIn(1, 28)
    val thisMonthStart = date.withDayClamped(day)
    val start = if (date.isBefore(thisMonthStart)) date.minusMonths(1).withDayClamped(day) else thisMonthStart
    return Period(start, start.plusMonths(1).withDayClamped(day))
}

fun nextOccurrence(from: LocalDate, frequency: Frequency, anchorDay: Int): LocalDate = when (frequency) {
    Frequency.DAILY -> from.plusDays(1)
    Frequency.WEEKLY -> from.plusWeeks(1)
    Frequency.MONTHLY -> from.plusMonths(1).withDayClamped(anchorDay)
    Frequency.YEARLY -> from.plusYears(1).withDayClamped(anchorDay)
}

/** Aylık eşdeğer tutar (abonelik toplamı için) */
fun monthlyEquivalent(amount: Long, frequency: Frequency): Long = when (frequency) {
    Frequency.DAILY -> amount * 30
    Frequency.WEEKLY -> amount * 52 / 12
    Frequency.MONTHLY -> amount
    Frequency.YEARLY -> amount / 12
}

/** Taksitleri böler: kalan kuruş ilk taksite eklenir (bankalar gibi). */
fun splitInstallments(total: Long, count: Int): List<Long> {
    require(count >= 1)
    val base = total / count
    val rest = total - base * count
    return List(count) { i -> if (i == 0) base + rest else base }
}

private val dayFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", trLocale)
private val shortFormatter = DateTimeFormatter.ofPattern("d MMM", trLocale)
private val fullFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", trLocale)

fun LocalDate.dayHeader(today: LocalDate = today()): String = when (this) {
    today -> "Bugün"
    today.minusDays(1) -> "Dün"
    today.plusDays(1) -> "Yarın"
    else -> if (year == today.year) format(dayFormatter) else format(fullFormatter)
}

fun LocalDate.short(): String = format(shortFormatter)
fun LocalDate.full(): String = format(fullFormatter)

/** "3 gün sonra", "yarın", "bugün", "2 gün geçti" */
fun relativeDays(target: LocalDate, today: LocalDate = today()): String {
    val d = ChronoUnit.DAYS.between(today, target)
    return when {
        d == 0L -> "bugün"
        d == 1L -> "yarın"
        d > 1 -> "$d gün sonra"
        d == -1L -> "dün"
        else -> "${-d} gün geçti"
    }
}

fun monthName(month: Int, short: Boolean = false): String =
    java.time.Month.of(month).getDisplayName(if (short) TextStyle.SHORT else TextStyle.FULL, trLocale)
        .replaceFirstChar { it.titlecase(trLocale) }
