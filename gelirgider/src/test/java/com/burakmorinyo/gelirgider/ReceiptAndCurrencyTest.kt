package com.burakmorinyo.gelirgider

import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.RatesService
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Inflation
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.Rates
import com.burakmorinyo.gelirgider.domain.ReceiptParser
import com.burakmorinyo.gelirgider.domain.formatIn
import com.burakmorinyo.gelirgider.domain.startMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.math.abs

class ReceiptParserTest {
    private val today = LocalDate.of(2026, 10, 31)

    @Test fun migrosReceipt() {
        val text = """
            MİGROS TİCARET A.Ş.
            ATATÜRK MAH. CUMHURİYET CAD. NO:12
            KADIKÖY V.D. 6220529513
            TARİH : 28.10.2026      SAAT : 18:42
            FİŞ NO : 0042
            SÜT 1 L            %1   *34,50
            EKMEK              %1   *15,00
            DOMATES 1,25 KG X 48,00
                               %1   *60,00
            DETERJAN           %20  *189,90
            TOPKDV                  *37,57
            TOPLAM                  *299,40
            KREDİ KARTI             *299,40
        """.trimIndent()
        val r = ReceiptParser.parse(text, today)
        assertEquals(29940L, r.total)
        assertEquals(LocalDate.of(2026, 10, 28), r.date)
        assertEquals("Migros", r.merchant)
        assertEquals("Market", r.categoryName)
    }

    @Test fun labelAndAmountOnSeparateLines() {
        // ML Kit sütunları ayrı bloklara bölebilir
        val text = """
            OPET YILDIZ PETROL LTD. ŞTİ.
            26/10/2026 09:15
            MOTORİN
            42,15 LT X 52,40
            KDV
            TOPLAM
            *2.208,66
            *368,11
        """.trimIndent()
        val r = ReceiptParser.parse(text, today)
        assertEquals(220866L, r.total)
        assertEquals(LocalDate.of(2026, 10, 26), r.date)
        assertEquals("Opet", r.merchant)
        assertEquals("Akaryakıt", r.categoryName)
    }

    @Test fun unknownShopUsesFirstLineAndKeywords() {
        val text = """
            ÖZKAN KEBAP SALONU LTD. ŞTİ.
            Tarih 30.10.26
            ADANA DÜRÜM 1 X 250,00   *250,00
            AYRAN 2 X 35,00          *70,00
            GENEL TOPLAM             *320,00
        """.trimIndent()
        val r = ReceiptParser.parse(text, today)
        assertEquals(32000L, r.total)
        assertEquals(LocalDate.of(2026, 10, 30), r.date)
        assertEquals("Özkan Kebap Salonu", r.merchant)
        assertEquals("Yemek & Kafe", r.categoryName)
    }

    @Test fun fallsBackToLargestStarredAmount() {
        val text = "ECZANE DEVA\n*45,90\n*120,00\n*165,90"
        val r = ReceiptParser.parse(text, today)
        assertEquals(16590L, r.total)
        assertEquals("Sağlık", r.categoryName)
        assertNull(r.date)
    }

    @Test fun ignoresSubtotalAndChange() {
        val text = "ARA TOPLAM 500,00\nİNDİRİM 50,00\nTOPLAM 450,00\nNAKİT 500,00\nPARA ÜSTÜ 50,00"
        assertEquals(45000L, ReceiptParser.parse(text, today).total)
    }

    @Test fun toleratesOcrSpacingAndListsAllAmounts() {
        // OCR virgülün etrafına boşluk koyabilir; toplam bulunamazsa bile tutarlar seçilebilsin
        val r = ReceiptParser.parse("KAFE\nÇAY 25 ,00\nTOST 120, 00\nTOPLAM 145 , 00", today)
        assertEquals(14500L, r.total)
        assertEquals(listOf(14500L, 12000L, 2500L), r.amounts)
        val noTotal = ReceiptParser.parse("Bir şeyler\n12,50\n7,25", today)
        assertEquals(listOf(1250L, 725L), noTotal.amounts)
        assertEquals("Bir şeyler\n12,50\n7,25", noTotal.rawText)
    }

    @Test fun rejectsFutureDates() {
        assertNull(ReceiptParser.parse("TARİH 15.12.2027\nTOPLAM 10,00", today).date)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CurrencyTest {
    private val rates = Rates(mapOf("USD" to 50.0, "EUR" to 55.0, "GOLD_GRAM" to 6000.0), updatedAt = 1)

    @Test fun converts() {
        assertEquals(500_000L, rates.convert(10_000, "USD", "TRY"))          // 100 $ = 5.000 ₺
        assertEquals(100L, rates.convert(600_000, "TRY", "GOLD_GRAM"))        // 6.000 ₺ = 1 gr
        assertEquals(11_000L, rates.convert(10_000, "EUR", "USD"))            // 100 € = 110 $
        assertNull(rates.convert(100, "GBP", "TRY"))
        assertEquals(100L, rates.convertOrSame(100, "GBP", "TRY"))
    }

    @Test fun formatsUnits() {
        Money.hidden = false
        assertEquals("12,50 gr", formatIn(1250, "GOLD_GRAM"))
        assertEquals("3 çeyrek", formatIn(300, "GOLD_QUARTER"))
        assertEquals("1.250 $", formatIn(125_000, "USD"))
    }

    @Test fun crossCurrencyTransferAndNetWorth() {
        val bank = Account(1, "Banka", AccountType.BANK, 0, "bank", initialBalance = 10_000_000)
        val gold = Account(2, "Altın", AccountType.INVESTMENT, 0, "diamond", currency = "GOLD_GRAM")
        val usd = Account(3, "Dolar", AccountType.SAVINGS, 0, "exchange", initialBalance = 100_000, currency = "USD")
        val d = LocalDate.of(2026, 10, 1).startMillis()
        val txs = listOf(
            // 60.000 ₺ ile 10 gr altın alındı
            Transaction(1, TxType.TRANSFER, 6_000_000, 1, 2, null, d, toAmount = 1_000),
            // dolar hesabından 20 $ harcama
            Transaction(2, TxType.EXPENSE, 2_000, 3, null, 5, d)
        )
        val b = Finance.balances(listOf(bank, gold, usd), txs, LocalDate.of(2026, 10, 31))
        assertEquals(4_000_000L, b[0].balance)
        assertEquals(1_000L, b[1].balance)
        assertEquals(98_000L, b[2].balance)
        // 40.000 + 10 gr × 6.000 + 980 $ × 50 = 149.000 ₺
        assertEquals(14_900_000L, Finance.netWorth(b, rates, "TRY"))
        val base = Finance.toBase(txs, listOf(bank, gold, usd).associateBy { it.id }, rates, "TRY")
        assertEquals(100_000L, base[1].amount)      // 20 $ = 1.000 ₺ istatistiklerde
        assertEquals(6_000_000L, base[0].amount)    // transfer değişmez
    }

    @Test fun inflationMath() {
        assertEquals(0.0241, Inflation.monthlyFromAnnual(0.33), 0.0001)
        // Geçen yıl 10.000, bu yıl 12.000; enflasyon %33 → reel %-9,8
        assertEquals(-0.0977, Inflation.realChange(1_200_000, 1_000_000, 0.33)!!, 0.0005)
        assertEquals(0.2, Inflation.nominalChange(1_200_000, 1_000_000)!!, 1e-9)
        // dolar 35 → 50: 10.000 ₺ = 285,7 $, 12.000 ₺ = 240 $ → %-16
        assertEquals(-0.16, Inflation.fxChange(1_200_000, 50.0, 1_000_000, 35.0)!!, 1e-9)
        assertNull(Inflation.realChange(100, 0, 0.3))
    }

    @Test fun parsesRateSources() {
        val truncgil = """{"Update_Date":"2026-10-31","USD":{"Alış":"49,9256","Satış":"49,9589"},"EUR":{"Alış":"55,7150"},
            "GBP":{"Alış":"66,9473"},"CHF":{"Alış":"60,5159"},"gram-altin":{"Alış":"6.222,08"},"ceyrek-altin":{"Alış":"10.042,08"}}"""
        val r = RatesService.parseTruncgil(truncgil)
        assertEquals(49.9256, r["USD"]!!, 1e-9)
        assertEquals(6222.08, r["GOLD_GRAM"]!!, 1e-9)
        assertEquals(10042.08, r["GOLD_QUARTER"]!!, 1e-9)
        val tcmb = """<Tarih_Date><Currency CrossOrder="0" Kod="USD" CurrencyCode="USD"><Unit>1</Unit><ForexBuying>49.7747</ForexBuying></Currency>
            <Currency CrossOrder="9" Kod="JPY" CurrencyCode="JPY"><Unit>100</Unit><ForexBuying>32.5</ForexBuying></Currency>
            <Currency CrossOrder="2" Kod="EUR" CurrencyCode="EUR"><Unit>1</Unit><ForexBuying>55.6</ForexBuying></Currency></Tarih_Date>"""
        val t = RatesService.parseTcmb(tcmb)
        assertEquals(49.7747, t["USD"]!!, 1e-9)
        assertEquals(55.6, t["EUR"]!!, 1e-9)
        assertEquals(true, abs((t["JPY"] ?: 0.0)) < 1e-9)
    }
}

class OcrMergeTest {
    @Test fun mergesColumnsIntoRows() {
        val P = com.burakmorinyo.gelirgider.ocr.ReceiptScanner::TextPart
        val parts = listOf(
            P("*245,90", 401, 20, 300), P("MİGROS", 50, 30, 100),
            P("TOPLAM", 400, 20, 20), P("KDV", 360, 20, 20), P("*37,50", 362, 20, 300)
        )
        assertEquals("MİGROS\nKDV *37,50\nTOPLAM *245,90", com.burakmorinyo.gelirgider.ocr.ReceiptScanner.mergeRows(parts))
    }
}
