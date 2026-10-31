package com.burakmorinyo.gelirgider

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.burakmorinyo.gelirgider.data.AppDatabase
import com.burakmorinyo.gelirgider.data.Backup
import com.burakmorinyo.gelirgider.data.Frequency
import com.burakmorinyo.gelirgider.data.Recurring
import com.burakmorinyo.gelirgider.data.Repository
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.startMillis
import com.burakmorinyo.gelirgider.domain.toLocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RepositoryTest {

    private fun repo(): Repository {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        return Repository(AppDatabase.build(ctx, inMemory = true), settings = null)
    }

    @Test fun seedsDefaults() = runBlocking {
        val r = repo()
        r.ensureSeed()
        r.ensureSeed() // ikinci kez çoğaltmamalı
        assertEquals(2, r.allAccounts().size)
        assertTrue(r.allCategories().any { it.name == "Market" })
        assertTrue(r.allCategories().any { it.name == "Maaş" && it.type == TxType.INCOME })
    }

    @Test fun installmentsAreSpreadMonthly() = runBlocking {
        val r = repo()
        val first = LocalDate.of(2026, 1, 31)
        r.saveTransaction(Transaction(type = TxType.EXPENSE, amount = 100_000, accountId = 1, date = first.startMillis() + 1000), installments = 3)
        val all = r.allTransactions().sortedBy { it.date }
        assertEquals(3, all.size)
        assertEquals(100_000L, all.sumOf { it.amount })
        assertEquals(listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31)), all.map { it.date.toLocalDate() })
        assertEquals(listOf(1, 2, 3), all.map { it.installmentIndex })
        r.deleteTransaction(all[1], wholeGroup = true)
        assertTrue(r.allTransactions().isEmpty())
    }

    @Test fun recurringAutoCreatesUntilToday() = runBlocking {
        val r = repo()
        val today = LocalDate.of(2026, 10, 30)
        r.saveRecurring(Recurring(title = "Kira", type = TxType.EXPENSE, amount = 1_500_000, accountId = 1, categoryId = null,
            frequency = Frequency.MONTHLY, nextDate = LocalDate.of(2026, 8, 31).startMillis(), anchorDay = 31))
        // Hatırlatmalı ödeme gelecekte: kendiliğinden eklenmez, "ödendi" deyince bir ay ileri gider
        val billDay = LocalDate.now().plusDays(5)
        r.saveRecurring(Recurring(title = "Fatura", type = TxType.EXPENSE, amount = 50_000, accountId = 1, categoryId = null,
            frequency = Frequency.MONTHLY, nextDate = billDay.startMillis(), anchorDay = billDay.dayOfMonth, autoCreate = false))
        val created = r.processRecurring(today)
        assertEquals(2, created) // 31 Ağustos, 30 Eylül; hatırlatmalı olan eklenmez
        val next = r.allRecurring().first { it.title == "Kira" }.nextDate.toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 31), next)
        assertEquals(0, r.processRecurring(today))

        val bill = r.allRecurring().first { it.title == "Fatura" }
        r.markRecurringPaid(bill)
        assertEquals(billDay.plusMonths(1), r.allRecurring().first { it.title == "Fatura" }.nextDate.toLocalDate())
        assertEquals(3, r.allTransactions().size)
    }

    @Test fun recurringStopsAtEndDate() = runBlocking {
        val r = repo()
        r.saveRecurring(Recurring(title = "Kredi", type = TxType.EXPENSE, amount = 100, accountId = 1, categoryId = null,
            frequency = Frequency.MONTHLY, nextDate = LocalDate.of(2026, 8, 15).startMillis(), anchorDay = 15,
            endDate = LocalDate.of(2026, 9, 15).startMillis()))
        assertEquals(2, r.processRecurring(LocalDate.of(2026, 12, 1)))
        assertFalse(r.allRecurring().first().active)
    }

    @Test fun cardDebtUpdateAdjustsOpenCycle() = runBlocking {
        val r = repo()
        val card = com.burakmorinyo.gelirgider.data.Account(name = "Mavi Kart", type = com.burakmorinyo.gelirgider.data.AccountType.CREDIT_CARD,
            color = 0xFF2563EB, icon = "credit_card", statementDay = 1, dueDay = 10)
        val id = r.saveAccount(card)
        val saved = card.copy(id = id)
        r.saveTransaction(Transaction(type = TxType.EXPENSE, amount = 200_000, accountId = id, date = System.currentTimeMillis(), note = "Market"))
        // Bankada dönem içi harcama 7.450 ₺ görünüyor
        assertEquals(545_000L, r.setCardDebt(saved, 745_000))
        val st = com.burakmorinyo.gelirgider.domain.CardStatements.status(saved, r.allTransactions())
        assertEquals(745_000L, st.currentPeriodSpend)
        // Fazla girilmişse düzeltme iade olarak düşer
        assertEquals(-45_000L, r.setCardDebt(saved, 700_000))
        assertEquals(700_000L, com.burakmorinyo.gelirgider.domain.CardStatements.status(saved, r.allTransactions()).currentPeriodSpend)
        assertEquals(0L, r.setCardDebt(saved, 700_000))
    }

    @Test fun remindOnlyRecurringSkipsPastDates() = runBlocking {
        val r = repo()
        val start = java.time.LocalDate.now().minusMonths(7)
        r.saveRecurring(Recurring(title = "Kredi taksiti", type = TxType.EXPENSE, amount = 100_000, accountId = 1, categoryId = null,
            frequency = Frequency.MONTHLY, nextDate = start.startMillis(), anchorDay = start.dayOfMonth, autoCreate = false))
        val next = r.allRecurring().single().nextDate.toLocalDate()
        assertFalse(next.isBefore(java.time.LocalDate.now()))
        assertTrue(r.allTransactions().isEmpty())
    }

    @Test fun mergeImportKeepsExistingData() = runBlocking {
        val r = repo()
        r.ensureSeed()
        r.saveTransaction(Transaction(type = TxType.EXPENSE, amount = 1_000, accountId = 1, date = 1_700_000_000_000, note = "Mevcut"))
        val incoming = Backup.Snapshot(
            accounts = listOf(
                com.burakmorinyo.gelirgider.data.Account(50, "Nakit", com.burakmorinyo.gelirgider.data.AccountType.CASH, 0, "wallet"),
                com.burakmorinyo.gelirgider.data.Account(51, "Kırmızı Kart", com.burakmorinyo.gelirgider.data.AccountType.CREDIT_CARD, 0xFFDC2626, "credit_card", statementDay = 1, dueDay = 10)
            ),
            categories = listOf(com.burakmorinyo.gelirgider.data.Category(70, "Elektronik", TxType.EXPENSE, "devices", 0)),
            transactions = listOf(
                Transaction(1, TxType.EXPENSE, 50_000, 51, categoryId = 70, date = 1_700_000_000_000, note = "Telefon", installmentGroup = 1, installmentIndex = 1, installmentCount = 2),
                Transaction(2, TxType.EXPENSE, 50_000, 51, categoryId = 70, date = 1_702_000_000_000, note = "Telefon", installmentGroup = 1, installmentIndex = 2, installmentCount = 2),
                Transaction(3, TxType.EXPENSE, 2_000, 50, date = 1_700_000_000_000, note = "Simit")
            ),
            recurring = emptyList(), budgets = emptyList(), goals = emptyList(), deposits = emptyList(), debts = emptyList()
        )
        assertEquals(3, r.mergeSnapshot(incoming))
        val accounts = r.allAccounts()
        assertEquals(3, accounts.size) // Nakit eşleşti, kart eklendi
        val card = accounts.first { it.name == "Kırmızı Kart" }
        val txs = r.allTransactions()
        assertEquals(4, txs.size)
        assertTrue(txs.any { it.note == "Mevcut" })
        assertEquals(2, txs.count { it.accountId == card.id })
        val elektronik = r.allCategories().first { it.name == "Elektronik" }
        assertEquals(1, r.allCategories().count { it.name == "Elektronik" }) // var olan kategori kullanıldı
        assertTrue(txs.filter { it.accountId == card.id }.all { it.categoryId == elektronik.id && it.installmentGroup != null })
        assertEquals("Simit", txs.first { it.accountId == accounts.first { a -> a.name == "Nakit" }.id && it.note == "Simit" }.note)
    }

    @Test fun backupRoundTrip() = runBlocking {
        val r = repo()
        r.ensureSeed()
        r.saveTransaction(Transaction(type = TxType.INCOME, amount = 5_000_000, accountId = 2, categoryId = 22, date = 1_700_000_000_000, note = "Maaş; \"Ekim\""))
        r.setBudget(null, 3_000_000)
        val json = Backup.toJson(r.snapshot())
        val other = repo()
        other.restoreSnapshot(Backup.fromJson(json))
        assertEquals(r.snapshot(), other.snapshot())
        val csv = Backup.toCsv(other.snapshot())
        assertTrue(csv.contains("\"Maaş; \"\"Ekim\"\"\""))
        assertTrue(csv.contains("50.000,00"))
    }
}
