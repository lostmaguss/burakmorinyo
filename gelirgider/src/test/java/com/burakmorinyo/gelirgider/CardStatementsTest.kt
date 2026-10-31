package com.burakmorinyo.gelirgider

import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.CardStatements
import com.burakmorinyo.gelirgider.domain.StatementStatus
import com.burakmorinyo.gelirgider.domain.splitInstallments
import com.burakmorinyo.gelirgider.domain.startMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CardStatementsTest {
    private val today = LocalDate.of(2026, 10, 31)
    // Hesap kesim ayın 1'i, son ödeme 10'u (maaşla kapatılan alışveriş kartı)
    private val red = Account(1, "Kırmızı Kart", AccountType.CREDIT_CARD, 0xFFDC2626, "credit_card",
        creditLimit = 10_000_000, statementDay = 1, dueDay = 10, cycleBudget = 1_500_000)
    private fun at(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).startMillis() + 3_600_000

    /** Mart'ta 10 taksitle alınmış 12.000 ₺'lik telefon */
    private fun phone(card: Long = 1): List<Transaction> = splitInstallments(1_200_000, 10).mapIndexed { i, a ->
        Transaction(100L + i, TxType.EXPENSE, a, card, date = at(2026, 3 + i, 15), note = "Telefon",
            installmentGroup = 7, installmentIndex = i + 1, installmentCount = 10)
    }

    @Test fun pastInstallmentsAreAssumedPaid() {
        val st = CardStatements.status(red, phone(), today)
        // Mart–Eylül taksitleri 1 Nis … 1 Eki ekstrelerine; hepsinin son ödeme günü geçti → ödenmiş sayılır
        val past = st.statements.filter { it.closing.isBefore(LocalDate.of(2026, 10, 1)) }
        assertTrue(past.all { it.status == StatementStatus.ASSUMED_PAID })
        // 1 Eki ekstresi (15 Eyl taksiti): son ödeme 10 Eki geçti → ödenmiş
        assertEquals(0L, st.remainingDue)
        assertFalse(st.overdue)
        // Açık dönem (kesim 1 Kas): 15 Eki taksiti 1.200 ₺
        assertEquals(120_000L, st.currentPeriodSpend)
        assertEquals(LocalDate.of(2026, 11, 1), st.nextStatement)
        // Kasım ve Aralık taksitleri gelecek ekstrelerde
        assertEquals(240_000L, st.futureInstallments)
        assertEquals(120_000L, st.debt)
        assertEquals(10_000_000L - 120_000 - 240_000, st.available)
        // Hedef 15.000 ₺ − 1.200 = 13.800 kaldı; kesime 2 gün (31 Eki, 1 Kas)
        assertEquals(1_380_000L, st.cycleRemaining)
        assertEquals(2, st.daysToClose)

        val plan = CardStatements.installmentPlans(red, phone(), today).single()
        // Bu ayki (Ekim) taksit dahil 8/10; ondan sonra 2 taksit kaldı
        assertEquals(8, plan.billed)
        assertEquals(10, plan.count)
        assertEquals(240_000L, plan.remainingAmount)
        assertFalse(plan.finished)
    }

    @Test fun withoutAssumePaidOldStatementsAreOverdueUntilPaid() {
        val strict = red.copy(assumePaid = false)
        val st = CardStatements.status(strict, phone(), today)
        assertTrue(st.overdue)
        assertEquals(7 * 120_000L, st.remainingDue)
        // Bankadan 7 taksitlik ödeme girilince hepsi kapanır (en eski ekstreden başlayarak)
        val paid = phone() + Transaction(999, TxType.TRANSFER, 840_000, 2, toAccountId = 1, date = at(2026, 10, 9))
        val st2 = CardStatements.status(strict, paid, today)
        assertFalse(st2.overdue)
        assertEquals(0L, st2.remainingDue)
        assertTrue(st2.statements.filter { it.closing.isBefore(LocalDate.of(2026, 11, 1)) }.all { it.status == StatementStatus.PAID })
    }

    @Test fun closedStatementWaitsForPaymentUntilDueDate() {
        val day = LocalDate.of(2026, 11, 5) // 1 Kas kesildi, son ödeme 10 Kas
        val txs = listOf(Transaction(1, TxType.EXPENSE, 500_000, 1, date = at(2026, 10, 20), note = "Market"))
        val st = CardStatements.status(red, txs, day)
        assertEquals(500_000L, st.remainingDue)
        assertEquals(LocalDate.of(2026, 11, 10), st.dueDate)
        assertEquals(StatementStatus.DUE, st.statements.first { it.closing == LocalDate.of(2026, 11, 1) }.status)
        // Maaş günü 3.000 ödendi → 2.000 kaldı
        val partial = txs + Transaction(2, TxType.TRANSFER, 300_000, 2, toAccountId = 1, date = at(2026, 11, 6))
        assertEquals(200_000L, CardStatements.status(red, partial, day).remainingDue)
        // 10 Kas geçince kalan da ödenmiş sayılır
        assertEquals(0L, CardStatements.status(red, partial, LocalDate.of(2026, 11, 11)).remainingDue)
    }

    @Test fun purchaseOnStatementDayGoesToThatStatement() {
        assertEquals(LocalDate.of(2026, 11, 1), CardStatements.closingFor(LocalDate.of(2026, 11, 1), 1))
        assertEquals(LocalDate.of(2026, 12, 1), CardStatements.closingFor(LocalDate.of(2026, 11, 2), 1))
        assertEquals(LocalDate.of(2026, 2, 28), CardStatements.closingFor(LocalDate.of(2026, 2, 10), 31))
        assertEquals(LocalDate.of(2026, 11, 10), CardStatements.dueFor(LocalDate.of(2026, 11, 1), 10))
        assertEquals(LocalDate.of(2026, 12, 3), CardStatements.dueFor(LocalDate.of(2026, 11, 20), 3))
    }

    @Test fun refundsReduceStatement() {
        val txs = listOf(
            Transaction(1, TxType.EXPENSE, 100_000, 1, date = at(2026, 10, 20)),
            Transaction(2, TxType.INCOME, 30_000, 1, date = at(2026, 10, 25), note = "İade")
        )
        assertEquals(70_000L, CardStatements.status(red, txs, today).currentPeriodSpend)
    }
}
