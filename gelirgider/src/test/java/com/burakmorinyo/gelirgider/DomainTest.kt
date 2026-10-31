package com.burakmorinyo.gelirgider

import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.Frequency
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Calculator
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.nextOccurrence
import com.burakmorinyo.gelirgider.domain.periodFor
import com.burakmorinyo.gelirgider.domain.splitInstallments
import com.burakmorinyo.gelirgider.domain.startMillis
import com.burakmorinyo.gelirgider.ui.transactions.applyKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DomainTest {

    @Test fun formatsTurkishMoney() {
        Money.symbol = "₺"; Money.hidden = false
        assertEquals("1.234,56 ₺", Money.format(123456))
        assertEquals("1.234 ₺", Money.format(123400))
        assertEquals("−50 ₺", Money.format(-5000))
        assertEquals("+12,50 ₺", Money.format(1250, signed = true))
        assertEquals("1,2M", Money.compact(120_000_000))
        assertEquals("12,5B", Money.compact(1_250_000))
    }

    @Test fun hiddenModeMasksAmounts() {
        Money.hidden = true
        assertEquals("•••• ₺", Money.format(123456))
        assertEquals("1.234,56", Money.format(123456, symbol = "", reveal = true))
        Money.hidden = false
    }

    @Test fun parsesAmounts() {
        assertEquals(123450L, Money.parse("1.234,5"))
        assertEquals(123450L, Money.parse("1234,50"))
        assertEquals(1250L, Money.parse("12.5"))
        assertEquals(100000000L, Money.parse("1.000.000"))
        assertNull(Money.parse("abc"))
        assertEquals("1234,5", Money.toInput(123450))
        assertEquals("12", Money.toInput(1200))
    }

    @Test fun calculatorRespectsPrecedence() {
        assertEquals(21100L, Calculator.evaluate("120+45,5×2"))
        assertEquals(5000L, Calculator.evaluate("100÷2"))
        assertEquals(7500L, Calculator.evaluate("100−25"))
        assertEquals(10000L, Calculator.evaluate("100+"))
        assertNull(Calculator.evaluate("10÷0"))
        assertNull(Calculator.evaluate(""))
    }

    @Test fun keypadInputRules() {
        assertEquals("0,", applyKey("", ','))
        assertEquals("12,5", applyKey("12,", '5'))
        assertEquals("12,55", applyKey("12,55", '9'))   // en fazla 2 kuruş hanesi
        assertEquals("12+", applyKey("12−", '+'))      // operatör değiştirme
        assertEquals("", applyKey("", '+'))
        assertEquals("7", applyKey("0", '7'))
        assertEquals("1", applyKey("12", '⌫'))
    }

    @Test fun salaryDayPeriods() {
        val p = periodFor(LocalDate.of(2026, 10, 30), 15)
        assertEquals(LocalDate.of(2026, 10, 15), p.start)
        assertEquals(LocalDate.of(2026, 11, 15), p.endExclusive)
        val q = periodFor(LocalDate.of(2026, 10, 3), 15)
        assertEquals(LocalDate.of(2026, 9, 15), q.start)
        val cal = periodFor(LocalDate.of(2026, 2, 10), 1)
        assertEquals(28, cal.days)
        assertEquals(LocalDate.of(2026, 1, 1), cal.previous(1).start)
    }

    @Test fun recurrenceKeepsAnchorDay() {
        val jan31 = LocalDate.of(2026, 1, 31)
        val feb = nextOccurrence(jan31, Frequency.MONTHLY, 31)
        assertEquals(LocalDate.of(2026, 2, 28), feb)
        assertEquals(LocalDate.of(2026, 3, 31), nextOccurrence(feb, Frequency.MONTHLY, 31))
        assertEquals(LocalDate.of(2026, 2, 7), nextOccurrence(jan31, Frequency.WEEKLY, 31))
    }

    @Test fun installmentsSumToTotal() {
        val parts = splitInstallments(100000, 3)
        assertEquals(listOf(33334L, 33333L, 33333L), parts)
        assertEquals(100000L, parts.sum())
    }

    @Test fun creditCardStatement() {
        val card = Account(id = 1, name = "Kart", type = AccountType.CREDIT_CARD, color = 0, icon = "credit_card",
            creditLimit = 5_000_000, statementDay = 10, dueDay = 20)
        val bank = Account(id = 2, name = "Banka", type = AccountType.BANK, color = 0, icon = "bank", initialBalance = 10_000_000)
        fun at(d: LocalDate) = d.startMillis() + 3_600_000
        val today = LocalDate.of(2026, 10, 15)
        val txs = listOf(
            Transaction(id = 1, type = TxType.EXPENSE, amount = 100_000, accountId = 1, date = at(LocalDate.of(2026, 10, 5))),
            Transaction(id = 2, type = TxType.EXPENSE, amount = 50_000, accountId = 1, date = at(LocalDate.of(2026, 10, 12))),
            Transaction(id = 3, type = TxType.TRANSFER, amount = 30_000, accountId = 2, toAccountId = 1, date = at(LocalDate.of(2026, 10, 14))),
            // gelecek taksit
            Transaction(id = 4, type = TxType.EXPENSE, amount = 20_000, accountId = 1, date = at(LocalDate.of(2026, 11, 5)), installmentGroup = 9, installmentIndex = 2, installmentCount = 2)
        )
        val b = Finance.balances(listOf(card, bank), txs, today)
        val cb = b.first { it.account.id == 1L }
        val st = cb.card!!
        // 10 Eki ekstresi 1.000 ₺, 300 ₺ ödendi → 700 kaldı (son ödeme 20 Eki)
        assertEquals(LocalDate.of(2026, 10, 10), st.lastStatement)
        assertEquals(LocalDate.of(2026, 10, 20), st.dueDate)
        assertEquals(100_000L, st.statementDebt)
        assertEquals(70_000L, st.remainingDue)
        // Açık dönem (kesim 10 Kas): 500 ₺ harcama + 5 Kas'taki 200 ₺ taksit
        assertEquals(70_000L, st.currentPeriodSpend)
        assertEquals(-140_000L, cb.balance)
        assertEquals(5_000_000L - 140_000, st.available)
        assertEquals(10_000_000L - 30_000, b.first { it.account.id == 2L }.balance)
    }
}

class InsightTest {
    @Test fun projectionUsesTrackedDaysOnly() {
        val today = LocalDate.of(2026, 10, 20)
        val period = periodFor(today, 1)
        // Kullanıcı 18 Ekim'de başlamış, 3 günde 300 ₺ harcamış
        val txs = (18..20).map { d ->
            Transaction(id = d.toLong(), type = TxType.EXPENSE, amount = 10_000, accountId = 1, date = LocalDate.of(2026, 10, d).startMillis() + 1000)
        }
        val insights = Finance.insights(period, 1, txs, emptyMap(), emptyList(), emptyList(), emptyList(), today)
        val projection = insights.first { it.title == "Dönem sonu tahmini" }
        // 300 + 100/gün × 11 kalan gün = 1.400 ₺
        assertEquals(true, projection.message.contains("1.400 ₺"))
    }
}
