package com.burakmorinyo.gelirgider.data

import com.burakmorinyo.gelirgider.domain.Rates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Güncel döviz ve altın kurları (anahtar gerektirmeyen kaynaklar).
 * 1) finans.truncgil.com — döviz + gram/çeyrek altın, TL cinsinden
 * 2) Yedek: TCMB günlük kurlar + gold-api.com ons fiyatından gram altın
 * Değerleme için alış fiyatı kullanılır (elindekini bozdursan alacağın tutar).
 */
object RatesService {
    private const val GRAMS_PER_OUNCE = 31.1034768
    /** Çeyrek altın 1,75 gr 22 ayar ≈ 1,60 gr saf altın; yedek kaynakta gram fiyatından türetilir */
    private const val QUARTER_FACTOR = 1.6035

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 10_000
        c.setRequestProperty("User-Agent", "GelirGider-Android")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** "6.222,08" / "49,9256" -> Double */
    internal fun parseTr(v: String): Double? = v.trim().replace(".", "").replace(',', '.').toDoubleOrNull()

    internal fun parseTruncgil(json: String): Map<String, Double> {
        val o = JSONObject(json)
        fun buy(key: String) = o.optJSONObject(key)?.optString("Alış")?.let(::parseTr)
        val out = mutableMapOf<String, Double>()
        listOf("USD", "EUR", "GBP", "CHF").forEach { k -> buy(k)?.let { out[k] = it } }
        buy("gram-altin")?.let { out["GOLD_GRAM"] = it }
        buy("ceyrek-altin")?.let { out["GOLD_QUARTER"] = it }
        return out
    }

    internal fun parseTcmb(xml: String): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        Regex("""<Currency[^>]*CurrencyCode="(\w+)"[^>]*>(.*?)</Currency>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { m ->
            val code = m.groupValues[1]
            val body = m.groupValues[2]
            val unit = Regex("<Unit>(\\d+)</Unit>").find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0
            val buying = Regex("<ForexBuying>([\\d.]+)</ForexBuying>").find(body)?.groupValues?.get(1)?.toDoubleOrNull()
            if (buying != null && code in setOf("USD", "EUR", "GBP", "CHF")) out[code] = buying / unit
        }
        return out
    }

    suspend fun fetch(): Rates = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        runCatching { parseTruncgil(get("https://finans.truncgil.com/today.json")) }
            .getOrNull()?.takeIf { it.size >= 4 }
            ?.let { return@withContext Rates(it, now, "Truncgil Finans") }

        val tcmb = parseTcmb(get("https://www.tcmb.gov.tr/kurlar/today.xml")).toMutableMap()
        require(tcmb.isNotEmpty()) { "Kur alınamadı" }
        runCatching {
            val ounceUsd = JSONObject(get("https://api.gold-api.com/price/XAU")).getDouble("price")
            val gram = ounceUsd / GRAMS_PER_OUNCE * tcmb.getValue("USD")
            tcmb["GOLD_GRAM"] = gram
            tcmb["GOLD_QUARTER"] = gram * QUARTER_FACTOR
        }
        Rates(tcmb, now, "TCMB")
    }

    /**
     * TCMB arşivinden geçmiş bir günün dolar kuru. Hafta sonu ve tatillerde bülten olmadığı için
     * en fazla 7 gün geriye bakılır.
     */
    suspend fun historicalUsd(date: LocalDate): Pair<LocalDate, Double>? = withContext(Dispatchers.IO) {
        var d = date
        repeat(7) {
            val url = "https://www.tcmb.gov.tr/kurlar/${d.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${d.format(DateTimeFormatter.ofPattern("ddMMyyyy"))}.xml"
            runCatching { parseTcmb(get(url))["USD"] }.getOrNull()?.let { return@withContext d to it }
            d = d.minusDays(1)
        }
        null
    }
}
