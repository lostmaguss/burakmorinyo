package com.burakmorinyo.gelirgider.domain

import java.time.LocalDate

/** Fişten okunan bilgiler; bulunamayanlar null */
data class ReceiptInfo(
    val total: Long?,
    val date: LocalDate?,
    val merchant: String?,
    /** Varsayılan kategori adlarından biri (ör. "Market"); eşleşme yoksa null */
    val categoryName: String?
)

/**
 * Türkiye'deki yazar kasa fişlerinin metninden (OCR çıktısı) toplam tutar, tarih, mağaza ve
 * tahmini kategoriyi çıkarır. OCR hatalarına toleranslıdır: etiket ile tutar farklı satırlara
 * düşebilir, "TOPLAM" yerine "TOP" yazabilir, tutarın başında "*" olabilir.
 */
object ReceiptParser {

    private data class Brand(val pattern: Regex, val name: String, val category: String)

    private fun brand(p: String, name: String, cat: String) = Brand(Regex("(?U)$p", RegexOption.IGNORE_CASE), name, cat)

    private val brands = listOf(
        brand("M[İI]GROS|MACROCENTER", "Migros", "Market"),
        brand("\\bA ?101\\b", "A101", "Market"),
        brand("\\bB[İI]M\\b|B[İI]RLE[ŞS][İI]K MA[ĞG]AZALAR", "BİM", "Market"),
        brand("\\b[ŞS]OK MARKET|\\b[ŞS]OK MA[ĞG]AZA", "ŞOK", "Market"),
        brand("CARREFOUR", "CarrefourSA", "Market"),
        brand("METRO GROSMARKET|METRO TOPTAN", "Metro", "Market"),
        brand("HAKMAR", "Hakmar", "Market"),
        brand("F[İI]LE MARKET", "File", "Market"),
        brand("OPET", "Opet", "Akaryakıt"),
        brand("SHELL", "Shell", "Akaryakıt"),
        brand("\\bBP\\b|BP PETROL", "BP", "Akaryakıt"),
        brand("PETROL OF[İI]S[İI]|\\bPO\\b AKARYAKIT", "Petrol Ofisi", "Akaryakıt"),
        brand("TOTAL ?ENERGIES|TOTAL OIL", "TotalEnergies", "Akaryakıt"),
        brand("AYTEMIZ|AYTEM[İI]Z", "Aytemiz", "Akaryakıt"),
        brand("STARBUCKS", "Starbucks", "Yemek & Kafe"),
        brand("BURGER K[İI]NG", "Burger King", "Yemek & Kafe"),
        brand("MC ?DONALD", "McDonald's", "Yemek & Kafe"),
        brand("KAHVE D[ÜU]NYASI", "Kahve Dünyası", "Yemek & Kafe"),
        brand("SIMIT SARAYI|S[İI]M[İI]T SARAYI", "Simit Sarayı", "Yemek & Kafe"),
        brand("DOM[İI]NO'?S", "Domino's", "Yemek & Kafe"),
        brand("TEKNOSA", "Teknosa", "Elektronik"),
        brand("MEDIA ?MARKT", "MediaMarkt", "Elektronik"),
        brand("VATAN B[İI]LG[İI]SAYAR", "Vatan Bilgisayar", "Elektronik"),
        brand("LC ?WA[İI]K[İI]K[İI]", "LC Waikiki", "Giyim"),
        brand("DEFACTO", "DeFacto", "Giyim"),
        brand("KOTON", "Koton", "Giyim"),
        brand("\\bZARA\\b", "Zara", "Giyim"),
        brand("\\bH ?& ?M\\b", "H&M", "Giyim"),
        brand("\\bIKEA\\b|[İI]KEA", "IKEA", "Ev & Yaşam"),
        brand("KO[ÇC]TA[ŞS]", "Koçtaş", "Ev & Yaşam"),
        brand("ENGLISH HOME", "English Home", "Ev & Yaşam"),
        brand("WATSONS", "Watsons", "Kişisel Bakım"),
        brand("GRATIS|GRAT[İI]S", "Gratis", "Kişisel Bakım"),
        brand("ROSSMANN", "Rossmann", "Kişisel Bakım"),
        brand("D ?& ?R\\b|KITAPYURDU", "D&R", "Eğitim")
    )

    private val categoryKeywords = listOf(
        Regex("(?U)ECZANE|ECZ\\.", RegexOption.IGNORE_CASE) to "Sağlık",
        Regex("(?U)AKARYAKIT|PETROL|BENZ[İI]N|MOTOR[İI]N|D[İI]ZEL|\\bLPG\\b", RegexOption.IGNORE_CASE) to "Akaryakıt",
        Regex("(?U)RESTORAN|RESTAURANT|CAFE|KAFE|KAHVE|LOKANTA|D[ÖO]NER|P[İI]DE|BURGER|PIZZA|P[İI]ZZA|KEBAP|BÖREK|PASTANE|F[İI]NCAN", RegexOption.IGNORE_CASE) to "Yemek & Kafe",
        Regex("(?U)MARKET|GIDA|MANAV|KASAP|F[İI]R[İI]N", RegexOption.IGNORE_CASE) to "Market",
        Regex("(?U)K[İI]TAP|KIRTAS[İI]YE", RegexOption.IGNORE_CASE) to "Eğitim",
        Regex("(?U)OTOPARK|TAKS[İI]|OTOB[ÜU]S", RegexOption.IGNORE_CASE) to "Ulaşım"
    )

    /** "1.234,56", "1234,56", "1 234,56", "1234.56" (OCR bazen nokta okur) */
    private val amountRegex = Regex("""(?<![\d.,])(\*?)\s?(\d{1,3}(?:[. ]\d{3})+|\d+)[,.](\d{2})(?![\d])""")
    private val dateRegex = Regex("""(?<!\d)(\d{1,2})[./-](\d{1,2})[./-](\d{4}|\d{2})(?!\d)""")

    private val totalKeys = listOf(
        Regex("(?U)GENEL\\s*TOPLAM", RegexOption.IGNORE_CASE) to 100,
        Regex("(?U)[ÖO]DENECEK|[ÖO]DENEN", RegexOption.IGNORE_CASE) to 90,
        Regex("(?U)(?<!ARA\\s)(?<!ARA)\\bTOPLAM\\b|\\bTOPLAH\\b|\\bTOPLAN\\b", RegexOption.IGNORE_CASE) to 80,
        Regex("(?U)\\bTOP\\b|\\bTOT\\b|TOTAL", RegexOption.IGNORE_CASE) to 70,
        Regex("(?U)KRED[İI]\\s*KART|K\\.\\s*KART|NAK[İI]T|BANKA\\s*KART", RegexOption.IGNORE_CASE) to 60,
        Regex("(?U)TUTAR", RegexOption.IGNORE_CASE) to 40
    )
    private val excludeKeys = Regex("(?U)KDV|TOPKDV|VERG[İI]|ARA\\s*TOPLAM|PARA\\s*[ÜU]ST[ÜU]|[İI]ND[İI]R[İI]M|[İI]SKONTO|B[İI]R[İI]M|ADET|KG\\b", RegexOption.IGNORE_CASE)

    private val skipMerchant = Regex(
        "\\bF[İI][ŞS]\\b|TAR[İI]H|\\bSAAT\\b|HO[ŞS]\\s*GELD|T[ÜU]RK[İI]YE|\\bT\\.?C\\.?\\b|VERG[İI]|\\bV\\.?D\\b|MERS[İI]S|\\bTEL\\b|ADRES|\\bMAH\\b|\\bCAD\\b|\\bSOK\\b|\\bNO\\s*:|\\d{5,}",
        RegexOption.IGNORE_CASE
    )

    private fun toKurus(m: MatchResult): Long? {
        val whole = m.groupValues[2].replace(".", "").replace(" ", "")
        return "$whole${m.groupValues[3]}".toLongOrNull()
    }

    fun parse(text: String, today: LocalDate = LocalDate.now()): ReceiptInfo {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ReceiptInfo(findTotal(lines), findDate(lines, today), findMerchant(lines, text), findCategory(text))
    }

    private fun findTotal(lines: List<String>): Long? {
        data class Candidate(val amount: Long, val score: Int)
        val candidates = mutableListOf<Candidate>()
        lines.forEachIndexed { i, line ->
            val key = totalKeys.firstOrNull { it.first.containsMatchIn(line) } ?: return@forEachIndexed
            if (excludeKeys.containsMatchIn(line)) return@forEachIndexed
            // Tutar aynı satırda ya da OCR sütunları ayırdıysa sonraki satırlarda olabilir
            val sameLine = amountRegex.findAll(line).mapNotNull { toKurus(it) }.lastOrNull()
            if (sameLine != null) candidates += Candidate(sameLine, key.second + 5)
            else lines.drop(i + 1).take(2).firstNotNullOfOrNull { next ->
                if (excludeKeys.containsMatchIn(next)) null else amountRegex.find(next)?.let(::toKurus)
            }?.let { candidates += Candidate(it, key.second) }
        }
        candidates.maxWithOrNull(compareBy<Candidate> { it.score }.thenBy { it.amount })?.let { return it.amount }

        // Etiket bulunamadı: yıldızlı tutarlar ("*245,90") fişlerde toplamı ve kalemleri gösterir; en büyüğü toplamdır
        val starred = lines.flatMap { l -> amountRegex.findAll(l).filter { it.groupValues[1] == "*" }.mapNotNull { toKurus(it) }.toList() }
        if (starred.isNotEmpty()) return starred.max()
        return lines.filter { !excludeKeys.containsMatchIn(it) && dateRegex.find(it) == null }
            .flatMap { l -> amountRegex.findAll(l).mapNotNull { toKurus(it) }.toList() }
            .maxOrNull()
    }

    private fun findDate(lines: List<String>, today: LocalDate): LocalDate? {
        for (line in lines) {
            for (m in dateRegex.findAll(line)) {
                val (d, mo, y) = m.destructured
                val year = y.toInt().let { if (it < 100) 2000 + it else it }
                val date = runCatching { LocalDate.of(year, mo.toInt(), d.toInt()) }.getOrNull() ?: continue
                // Gelecekteki ya da çok eski tarihler büyük ihtimalle OCR hatasıdır
                if (!date.isAfter(today.plusDays(1)) && date.isAfter(today.minusYears(2))) return date
            }
        }
        return null
    }

    private fun findMerchant(lines: List<String>, text: String): String? {
        brands.firstOrNull { it.pattern.containsMatchIn(text) }?.let { return it.name }
        val candidate = lines.take(5).firstOrNull { l ->
            l.count { it.isLetter() } >= 3 && !skipMerchant.containsMatchIn(l) && amountRegex.find(l) == null
        } ?: return null
        // "ÖZKAN GIDA LTD. ŞTİ." -> "Özkan Gıda"
        val cleaned = candidate.replace(Regex("(?U)\\b(LTD|ŞT[İI]|STI|A\\.?Ş|SAN|T[İI]C|VE)\\b\\.?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?U)[^\\p{L}\\d&' ]"), " ").replace(Regex("(?U)\\s+"), " ").trim()
        if (cleaned.length < 3) return null
        return cleaned.split(' ').joinToString(" ") { w ->
            w.lowercase(trLocale).replaceFirstChar { it.titlecase(trLocale) }
        }.take(40)
    }

    private fun findCategory(text: String): String? =
        brands.firstOrNull { it.pattern.containsMatchIn(text) }?.category
            ?: categoryKeywords.firstOrNull { it.first.containsMatchIn(text) }?.second
}
