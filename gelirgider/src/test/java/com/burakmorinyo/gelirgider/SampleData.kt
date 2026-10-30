package com.burakmorinyo.gelirgider

import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.AppSettings
import com.burakmorinyo.gelirgider.data.Budget
import com.burakmorinyo.gelirgider.data.Debt
import com.burakmorinyo.gelirgider.data.DebtDirection
import com.burakmorinyo.gelirgider.data.DefaultData
import com.burakmorinyo.gelirgider.data.Frequency
import com.burakmorinyo.gelirgider.data.Goal
import com.burakmorinyo.gelirgider.data.Recurring
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.startMillis
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.ui.AppData
import java.time.LocalDate
import kotlin.random.Random

/** Ekran görüntüsü testleri için gerçekçi örnek veri (bugüne göre) */
object SampleData {
    fun build(settings: AppSettings = AppSettings(onboarded = true)): AppData {
        val categories = DefaultData.categories().mapIndexed { i, c -> c.copy(id = i + 1L) }
        fun cat(name: String, type: TxType = TxType.EXPENSE) = categories.first { it.name == name && it.type == type }.id
        val accounts = listOf(
            Account(1, "Nakit", AccountType.CASH, 0xFF22C55E, "wallet", initialBalance = 150_000, sortOrder = 0),
            Account(2, "Ziraat Maaş", AccountType.BANK, 0xFF3B82F6, "bank", initialBalance = 2_000_000, sortOrder = 1),
            Account(3, "Bonus Kart", AccountType.CREDIT_CARD, 0xFF10B981, "credit_card", initialBalance = -320_000,
                creditLimit = 6_000_000, statementDay = 5, dueDay = 15, sortOrder = 2),
            Account(4, "Altın Hesabı", AccountType.INVESTMENT, 0xFFF59E0B, "diamond", initialBalance = 4_500_000, sortOrder = 3)
        )
        val rnd = Random(42)
        val txs = mutableListOf<Transaction>()
        var id = 1L
        fun add(date: LocalDate, type: TxType, amount: Long, account: Long, category: Long?, note: String = "", to: Long? = null) {
            if (date.isAfter(today())) return
            txs += Transaction(id++, type, amount, account, to, category, date.startMillis() + rnd.nextLong(8, 21) * 3_600_000, note)
        }
        val start = today().withDayOfMonth(1).minusMonths(5)
        var d = start
        while (!d.isAfter(today())) {
            if (d.dayOfMonth == 1) {
                add(d, TxType.INCOME, 6_500_000, 2, cat("Maaş", TxType.INCOME), "Maaş")
                add(d.plusDays(1), TxType.EXPENSE, 1_650_000, 2, cat("Kira"), "Kira")
                add(d.plusDays(3), TxType.EXPENSE, 89_000 + rnd.nextLong(0, 40_000), 2, cat("Faturalar"), "Elektrik")
                add(d.plusDays(4), TxType.EXPENSE, 45_000 + rnd.nextLong(0, 30_000), 2, cat("Faturalar"), "Doğalgaz")
                add(d.plusDays(6), TxType.EXPENSE, 49_900, 3, cat("Telefon & İnternet"), "Superonline")
                add(d.plusDays(8), TxType.EXPENSE, 22_999, 3, cat("Abonelikler"), "Netflix")
                add(d.plusDays(12), TxType.EXPENSE, 5_999, 3, cat("Abonelikler"), "Spotify")
                add(d.plusDays(14), TxType.TRANSFER, 1_900_000, 2, null, "Kart ödemesi", to = 3)
                add(d.plusDays(2), TxType.TRANSFER, 300_000, 2, null, "ATM", to = 1)
                if (d.monthValue % 2 == 0) add(d.plusDays(18), TxType.INCOME, 750_000, 2, cat("Ek Gelir", TxType.INCOME), "Freelance proje")
            }
            if (d.dayOfWeek.value == 6) add(d, TxType.EXPENSE, 120_000 + rnd.nextLong(0, 140_000), 3, cat("Market"), listOf("Migros", "A101", "CarrefourSA").random(rnd))
            if (rnd.nextInt(100) < 45) add(d, TxType.EXPENSE, 9_000 + rnd.nextLong(0, 45_000), if (rnd.nextBoolean()) 1 else 3, cat("Yemek & Kafe"), listOf("Starbucks", "Öğle yemeği", "Kahve", "Yemeksepeti").random(rnd))
            if (rnd.nextInt(100) < 12) add(d, TxType.EXPENSE, 120_000 + rnd.nextLong(0, 80_000), 3, cat("Akaryakıt"), "Opet")
            if (rnd.nextInt(100) < 25) add(d, TxType.EXPENSE, 3_500 + rnd.nextLong(0, 4_000), 1, cat("Ulaşım"), "İstanbulkart")
            if (rnd.nextInt(100) < 6) add(d, TxType.EXPENSE, 60_000 + rnd.nextLong(0, 150_000), 3, cat("Giyim"), listOf("Zara", "LC Waikiki", "Koton").random(rnd))
            if (rnd.nextInt(100) < 7) add(d, TxType.EXPENSE, 25_000 + rnd.nextLong(0, 90_000), 3, cat("Eğlence"), listOf("Sinema", "Konser", "Bowling").random(rnd))
            if (rnd.nextInt(100) < 4) add(d, TxType.EXPENSE, 35_000 + rnd.nextLong(0, 60_000), 1, cat("Sağlık"), "Eczane")
            d = d.plusDays(1)
        }
        // Taksitli alışveriş: 3 ay önce 6 taksit telefon
        val buy = today().minusMonths(2).withDayOfMonth(10)
        val group = 777L
        for (i in 0 until 6) {
            val date = buy.plusMonths(i.toLong())
            txs += Transaction(id++, TxType.EXPENSE, 750_000, 3, null, cat("Elektronik"), date.startMillis() + 50_000_000, "iPhone",
                installmentGroup = group, installmentIndex = i + 1, installmentCount = 6)
        }

        val t = today()
        val recurring = listOf(
            Recurring(1, "Kira", TxType.EXPENSE, 1_650_000, 2, cat("Kira"), Frequency.MONTHLY, t.withDayOfMonth(1).plusMonths(1).plusDays(1).startMillis(), 2, true),
            Recurring(2, "Netflix", TxType.EXPENSE, 22_999, 3, cat("Abonelikler"), Frequency.MONTHLY, t.plusDays(3).startMillis(), t.plusDays(3).dayOfMonth, true, isSubscription = true),
            Recurring(3, "Spotify", TxType.EXPENSE, 5_999, 3, cat("Abonelikler"), Frequency.MONTHLY, t.plusDays(9).startMillis(), t.plusDays(9).dayOfMonth, true, isSubscription = true),
            Recurring(4, "Elektrik", TxType.EXPENSE, 110_000, 2, cat("Faturalar"), Frequency.MONTHLY, t.plusDays(1).startMillis(), t.plusDays(1).dayOfMonth, autoCreate = false),
            Recurring(5, "Maaş", TxType.INCOME, 5_200_000, 2, cat("Maaş", TxType.INCOME), Frequency.MONTHLY, t.withDayOfMonth(1).plusMonths(1).startMillis(), 1, true)
        )
        val budgets = listOf(
            Budget(1, null, 4_000_000),
            Budget(2, cat("Market"), 700_000),
            Budget(3, cat("Yemek & Kafe"), 400_000),
            Budget(4, cat("Eğlence"), 150_000)
        )
        val goals = listOf(
            Goal(1, "Yaz tatili", 4_000_000, 1_850_000, t.plusMonths(7).startMillis(), 0xFF14B8A6, "beach"),
            Goal(2, "Acil durum fonu", 10_000_000, 6_200_000, null, 0xFF6366F1, "savings")
        )
        val debts = listOf(
            Debt(1, "Ahmet", DebtDirection.OWED_TO_ME, 250_000, 50_000, t.plusDays(5).startMillis(), "Konser bileti"),
            Debt(2, "Ayşe", DebtDirection.I_OWE, 120_000, 0, t.plusDays(12).startMillis())
        )
        return AppData(settings, accounts, categories, txs.sortedByDescending { it.date }, recurring, budgets, goals, emptyList(), debts, loaded = true)
    }
}
