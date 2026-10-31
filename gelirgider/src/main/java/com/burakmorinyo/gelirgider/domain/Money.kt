package com.burakmorinyo.gelirgider.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private val trSymbols = DecimalFormatSymbols(Locale.forLanguageTag("tr-TR")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

object Money {
    var symbol: String = "₺"
    /** Gizlilik modu: tutarlar ekranda yıldızlı gösterilir */
    var hidden: Boolean = false

    /** 123456 kuruş -> "1.234,56 ₺"; küsuratı olmayanlar "1.234 ₺" */
    fun format(kurus: Long, signed: Boolean = false, alwaysDecimals: Boolean = false, symbol: String = this.symbol, reveal: Boolean = false): String {
        if (hidden && !reveal) return "•••• $symbol".trim()
        val sign = when {
            kurus < 0 -> "−"
            signed && kurus > 0 -> "+"
            else -> ""
        }
        val absVal = abs(kurus)
        val pattern = if (alwaysDecimals || absVal % 100 != 0L) "#,##0.00" else "#,##0"
        val body = DecimalFormat(pattern, trSymbols).format(absVal / 100.0)
        return if (symbol.isEmpty()) "$sign$body" else "$sign$body $symbol"
    }

    /** Grafik eksenleri ve dar alanlar için: 1,2B / 3,4M */
    fun compact(kurus: Long): String {
        if (hidden) return "••"
        val v = abs(kurus) / 100.0
        val sign = if (kurus < 0) "−" else ""
        val f = DecimalFormat("#,##0.#", trSymbols)
        return sign + when {
            v >= 1_000_000 -> f.format(v / 1_000_000) + "M"
            v >= 1_000 -> f.format(v / 1_000) + "B"
            else -> DecimalFormat("#,##0", trSymbols).format(v)
        }
    }

    /** "1.234,5" / "1234.5" / "12" -> kuruş. Geçersizse null. */
    fun parse(text: String): Long? {
        val t = text.trim().replace(" ", "").replace(symbol, "")
        if (t.isEmpty()) return null
        val normalized = when {
            t.contains(',') -> t.replace(".", "").replace(',', '.')
            t.count { it == '.' } > 1 -> t.replace(".", "")
            else -> t
        }
        return normalized.toBigDecimalOrNull()
            ?.movePointRight(2)
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.toLong()
    }

    /** Kuruşu düzenleme alanı için yazıya çevirir: 123450 -> "1234,5" */
    fun toInput(kurus: Long): String {
        val bd = BigDecimal.valueOf(kurus).movePointLeft(2).stripTrailingZeros()
        return bd.toPlainString().replace('.', ',')
    }
}

/**
 * Tutar girerken kullanılan basit hesap makinesi: "120+45,5×2" gibi ifadeleri hesaplar.
 * Çarpma/bölme önceliklidir. Sonuç kuruş cinsindendir.
 */
object Calculator {
    val operators = setOf('+', '−', '×', '÷')

    fun evaluate(expr: String): Long? {
        if (expr.isBlank()) return null
        val tokens = mutableListOf<Any>()
        val num = StringBuilder()
        for (c in expr) {
            if (c in operators) {
                if (num.isEmpty()) {
                    if (c == '−' && tokens.isEmpty()) { num.append('-'); continue }
                    return null
                }
                tokens += num.toString(); num.clear(); tokens += c
            } else num.append(c)
        }
        if (num.isEmpty() || num.toString() == "-") {
            if (tokens.isNotEmpty() && tokens.last() is Char) tokens.removeAt(tokens.size - 1) else return null
        } else tokens += num.toString()

        val values = mutableListOf<BigDecimal>()
        val ops = mutableListOf<Char>()
        fun parseNum(s: String) = s.replace(',', '.').toBigDecimalOrNull()

        var current = parseNum(tokens[0] as String) ?: return null
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i] as Char
            val next = parseNum(tokens[i + 1] as String) ?: return null
            when (op) {
                '×' -> current = current.multiply(next)
                '÷' -> {
                    if (next.signum() == 0) return null
                    current = current.divide(next, MathContext.DECIMAL64)
                }
                else -> { values += current; ops += op; current = next }
            }
            i += 2
        }
        values += current
        var result = values[0]
        for (k in ops.indices) {
            result = if (ops[k] == '+') result.add(values[k + 1]) else result.subtract(values[k + 1])
        }
        return result.movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
    }

    fun hasOperator(expr: String) = expr.drop(1).any { it in operators }
}
