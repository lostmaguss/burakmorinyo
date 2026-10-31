package com.burakmorinyo.gelirgider.domain

import kotlin.math.pow
import kotlin.math.roundToLong

data class CurrencyDef(val code: String, val name: String, val symbol: String, val icon: String)

/** Desteklenen para birimleri. Tutarlar her birimde 1/100 hassasiyetle saklanır (kuruş, cent, 0,01 gr…). */
object Currencies {
    const val BASE = "TRY"

    val all = listOf(
        CurrencyDef("TRY", "Türk lirası", "₺", "wallet"),
        CurrencyDef("USD", "ABD doları", "$", "exchange"),
        CurrencyDef("EUR", "Euro", "€", "exchange"),
        CurrencyDef("GBP", "İngiliz sterlini", "£", "exchange"),
        CurrencyDef("CHF", "İsviçre frangı", "CHF", "exchange"),
        CurrencyDef("GOLD_GRAM", "Gram altın", "gr", "diamond"),
        CurrencyDef("GOLD_QUARTER", "Çeyrek altın", "çeyrek", "diamond")
    )

    private val byCode = all.associateBy { it.code }

    fun get(code: String): CurrencyDef = byCode[code] ?: all.first()

    /** Ayarlardaki ana para birimi sembolünden kod */
    fun codeForSymbol(symbol: String): String = when (symbol) {
        "$" -> "USD"; "€" -> "EUR"; "£" -> "GBP"; "CHF" -> "CHF"
        else -> BASE
    }

    fun isGold(code: String) = code.startsWith("GOLD")
}

/**
 * Kurlar: 1 birim = kaç TL. TRY her zaman 1'dir.
 * [updatedAt] 0 ise hiç güncellenmemiştir.
 */
data class Rates(val values: Map<String, Double> = emptyMap(), val updatedAt: Long = 0, val source: String = "") {

    fun tryPer(code: String): Double? = if (code == Currencies.BASE) 1.0 else values[code]?.takeIf { it > 0 }

    val isEmpty get() = values.isEmpty()

    /**
     * [amount] (kaynak birimin 1/100'ü) tutarını hedef birime çevirir. Kur yoksa null.
     */
    fun convert(amount: Long, from: String, to: String): Long? {
        if (from == to) return amount
        val f = tryPer(from) ?: return null
        val t = tryPer(to) ?: return null
        return (amount * f / t).roundToLong()
    }

    /** Kur yoksa tutarı olduğu gibi döndürür (çevrimdışı ilk açılış için güvenli varsayım) */
    fun convertOrSame(amount: Long, from: String, to: String): Long = convert(amount, from, to) ?: amount
}

/** Bir tutarı hesabın para birimine göre biçimlendirir: "1.250 $", "12,5 gr", "3 çeyrek" */
fun formatIn(amount: Long, code: String, signed: Boolean = false): String =
    Money.format(amount, signed = signed, symbol = Currencies.get(code).symbol)

object Inflation {
    /** Yıllık oranı (ör. 0.33) aylığa çevirir: (1+y)^(1/12) − 1 */
    fun monthlyFromAnnual(annual: Double): Double = (1 + annual).pow(1.0 / 12) - 1

    /**
     * Reel değişim: önceki tutar enflasyonla bugünün parasına çevrilip karşılaştırılır.
     * [inflation] dönemler arası toplam enflasyon (ör. yıllık 0.33). Sonuç oran (0.05 = %5 reel artış).
     */
    fun realChange(current: Long, previous: Long, inflation: Double): Double? {
        if (previous <= 0) return null
        return current / (previous * (1 + inflation)) - 1
    }

    fun nominalChange(current: Long, previous: Long): Double? =
        if (previous <= 0) null else current.toDouble() / previous - 1

    /** Döviz bazlı değişim: iki tutar o günkü kurla dövize çevrilip karşılaştırılır */
    fun fxChange(current: Long, currentRate: Double, previous: Long, previousRate: Double): Double? {
        if (previous <= 0 || currentRate <= 0 || previousRate <= 0) return null
        return (current / currentRate) / (previous / previousRate) - 1
    }
}
