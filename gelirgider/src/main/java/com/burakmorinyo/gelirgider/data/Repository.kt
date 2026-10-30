package com.burakmorinyo.gelirgider.data

import androidx.room.withTransaction
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.nextOccurrence
import com.burakmorinyo.gelirgider.domain.periodFor
import com.burakmorinyo.gelirgider.domain.splitInstallments
import com.burakmorinyo.gelirgider.domain.startMillis
import com.burakmorinyo.gelirgider.domain.toLocalDate
import com.burakmorinyo.gelirgider.domain.today
import com.burakmorinyo.gelirgider.domain.withDayClamped
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Bütçe uyarısı gibi bildirimleri veri katmanından ayırmak için */
fun interface AlertSink {
    fun budgetAlert(key: String, title: String, message: String)
}

class Repository(
    private val db: AppDatabase,
    val settings: SettingsStore?,
    private val alerts: AlertSink? = null
) {
    val accounts = db.accounts().observeAll()
    val categories = db.categories().observeAll()
    val transactions = db.transactions().observeAll()
    val recurring = db.recurring().observeAll()
    val budgets = db.budgets().observeAll()
    val goals = db.goals().observeAll()
    val deposits = db.goals().observeDeposits()
    val debts = db.debts().observeAll()

    suspend fun ensureSeed() {
        if (db.accounts().count() == 0 && db.categories().all().isEmpty()) {
            db.withTransaction {
                db.accounts().insertAll(DefaultData.accounts())
                db.categories().insertAll(DefaultData.categories())
            }
        }
    }

    // ---------- İşlemler ----------

    /**
     * İşlemi kaydeder. Yeni bir gider [installments] > 1 ile kaydedilirse her ay için ayrı
     * taksit işlemleri oluşturulur (kredi kartı taksitleri gibi).
     */
    suspend fun saveTransaction(tx: Transaction, installments: Int = 1) {
        if (tx.id == 0L && installments > 1 && tx.type == TxType.EXPENSE) {
            val group = System.currentTimeMillis()
            val first = tx.date.toLocalDate()
            val time = java.time.Instant.ofEpochMilli(tx.date).atZone(ZoneId.systemDefault()).toLocalTime()
            val parts = splitInstallments(tx.amount, installments)
            val items = parts.mapIndexed { i, part ->
                val d = first.plusMonths(i.toLong()).withDayClamped(first.dayOfMonth)
                tx.copy(
                    amount = part,
                    date = d.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    installmentGroup = group,
                    installmentIndex = i + 1,
                    installmentCount = installments
                )
            }
            db.transactions().insertAll(items)
        } else {
            db.transactions().upsert(tx)
        }
        settings?.update { it.copy(lastAccountId = tx.accountId) }
        if (tx.type == TxType.EXPENSE) checkBudgets(tx)
    }

    suspend fun transaction(id: Long) = db.transactions().byId(id)

    suspend fun installmentsOf(group: Long) = db.transactions().byInstallmentGroup(group)

    suspend fun deleteTransaction(tx: Transaction, wholeGroup: Boolean = false) {
        if (wholeGroup && tx.installmentGroup != null) db.transactions().deleteInstallmentGroup(tx.installmentGroup)
        else db.transactions().delete(tx)
    }

    /** Silinen işlemi geri almak için */
    suspend fun restore(items: List<Transaction>) = db.transactions().insertAll(items)

    private suspend fun checkBudgets(tx: Transaction) {
        val s = settings?.current() ?: return
        if (!s.budgetAlerts || alerts == null) return
        val period = periodFor(tx.date.toLocalDate(), s.periodStartDay)
        if (today() !in period) return
        val cats = db.categories().all().associateBy { it.id }
        for (b in db.budgets().all()) {
            if (b.categoryId != null && b.categoryId != tx.categoryId) continue
            if (b.amount <= 0) continue
            val spent = db.transactions().expenseBetween(period.startMillis, period.endMillis, b.categoryId)
            val ratio = spent.toDouble() / b.amount
            val level = when { ratio >= 1.0 -> 100; ratio >= 0.8 -> 80; else -> continue }
            val name = b.categoryId?.let { cats[it]?.name } ?: "Aylık toplam"
            val key = "budget:${period.start}:${b.categoryId ?: 0}:$level"
            if (!settings.markNotified(key)) continue
            if (level == 100) alerts.budgetAlert(key, "$name bütçesi aşıldı",
                "Limit ${Money.format(b.amount)}, harcanan ${Money.format(spent)}.")
            else alerts.budgetAlert(key, "$name bütçesinin %80'i doldu",
                "Kalan: ${Money.format(b.amount - spent)}. Dönem sonuna kadar dikkatli ol.")
        }
    }

    // ---------- Hesaplar ----------

    suspend fun saveAccount(a: Account): Long = db.accounts().upsert(a)

    suspend fun deleteAccount(a: Account) = db.withTransaction {
        db.transactions().deleteForAccount(a.id)
        db.recurring().all().filter { it.accountId == a.id }.forEach { db.recurring().delete(it) }
        db.accounts().delete(a)
    }

    // ---------- Kategoriler ----------

    suspend fun saveCategory(c: Category): Long = db.categories().upsert(c)

    suspend fun deleteCategory(c: Category) = db.withTransaction {
        db.transactions().detachCategory(c.id)
        db.budgets().deleteForCategory(c.id)
        db.categories().delete(c)
    }

    // ---------- Tekrarlayan işlemler ----------

    suspend fun saveRecurring(r: Recurring): Long = db.recurring().upsert(r)
    suspend fun deleteRecurring(r: Recurring) = db.recurring().delete(r)

    private fun Recurring.toTransaction(date: LocalDate) = Transaction(
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = categoryId,
        date = date.atTime(LocalTime.of(9, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        note = title,
        recurringId = id
    )

    private fun Recurring.advanced(): Recurring {
        val next = nextOccurrence(nextDate.toLocalDate(), frequency, anchorDay)
        val ended = endDate != null && next.isAfter(endDate.toLocalDate())
        return copy(nextDate = next.startMillis(), active = active && !ended)
    }

    /** Ödendi olarak işaretle: işlemi ekler ve sonraki tarihe geçer */
    suspend fun markRecurringPaid(r: Recurring) {
        db.transactions().upsert(r.toTransaction(r.nextDate.toLocalDate()))
        db.recurring().upsert(r.advanced())
    }

    suspend fun skipRecurring(r: Recurring) = db.recurring().upsert(r.advanced())

    /** Otomatik eklenen tekrarlayan işlemlerin günü geldiyse ekler. Eklenen işlem sayısını döner. */
    suspend fun processRecurring(today: LocalDate = today()): Int {
        var created = 0
        db.withTransaction {
            for (r0 in db.recurring().all()) {
                var r = r0
                var guard = 0
                while (r.active && r.autoCreate && !r.nextDate.toLocalDate().isAfter(today) && guard++ < 400) {
                    db.transactions().upsert(r.toTransaction(r.nextDate.toLocalDate()))
                    created++
                    r = r.advanced()
                }
                if (r != r0) db.recurring().upsert(r)
            }
        }
        return created
    }

    // ---------- Bütçeler ----------

    suspend fun setBudget(categoryId: Long?, amount: Long) {
        val existing = db.budgets().all().firstOrNull { it.categoryId == categoryId }
        if (amount <= 0) { existing?.let { db.budgets().delete(it) }; return }
        db.budgets().upsert((existing ?: Budget(categoryId = categoryId, amount = amount)).copy(amount = amount))
    }

    suspend fun deleteBudget(b: Budget) = db.budgets().delete(b)

    // ---------- Hedefler ----------

    suspend fun saveGoal(g: Goal): Long = db.goals().upsert(g)

    suspend fun deleteGoal(g: Goal) = db.withTransaction {
        db.goals().deleteDeposits(g.id)
        db.goals().delete(g)
    }

    suspend fun addDeposit(g: Goal, amount: Long) = db.withTransaction {
        db.goals().insertDeposit(GoalDeposit(goalId = g.id, amount = amount, date = System.currentTimeMillis()))
        val saved = (g.saved + amount).coerceAtLeast(0)
        db.goals().upsert(g.copy(saved = saved, completed = saved >= g.target))
    }

    // ---------- Borç / Alacak ----------

    suspend fun saveDebt(d: Debt): Long = db.debts().upsert(d)
    suspend fun deleteDebt(d: Debt) = db.debts().delete(d)

    /** Ödeme/tahsilat kaydeder; [accountId] verilirse hesaba da gider/gelir olarak işler */
    suspend fun payDebt(d: Debt, amount: Long, accountId: Long?) = db.withTransaction {
        db.debts().upsert(d.copy(paid = (d.paid + amount).coerceAtMost(d.amount)))
        if (accountId != null) {
            val isExpense = d.direction == DebtDirection.I_OWE
            db.transactions().upsert(
                Transaction(
                    type = if (isExpense) TxType.EXPENSE else TxType.INCOME,
                    amount = amount,
                    accountId = accountId,
                    date = System.currentTimeMillis(),
                    note = if (isExpense) "Borç ödemesi: ${d.person}" else "Alacak tahsilatı: ${d.person}"
                )
            )
        }
    }

    // ---------- Yedek ----------

    suspend fun snapshot() = Backup.Snapshot(
        accounts = db.accounts().all(),
        categories = db.categories().all(),
        transactions = db.transactions().all(),
        recurring = db.recurring().all(),
        budgets = db.budgets().all(),
        goals = db.goals().all(),
        deposits = db.goals().allDeposits(),
        debts = db.debts().all()
    )

    suspend fun restoreSnapshot(s: Backup.Snapshot) = db.withTransaction {
        db.transactions().clear(); db.accounts().clear(); db.categories().clear(); db.recurring().clear()
        db.budgets().clear(); db.goals().clearDeposits(); db.goals().clear(); db.debts().clear()
        db.accounts().insertAll(s.accounts)
        db.categories().insertAll(s.categories)
        db.transactions().insertAll(s.transactions)
        db.recurring().insertAll(s.recurring)
        db.budgets().insertAll(s.budgets)
        db.goals().insertAll(s.goals)
        db.goals().insertAllDeposits(s.deposits)
        db.debts().insertAll(s.debts)
    }

    suspend fun hasTransactionToday(): Boolean {
        val t = today()
        return db.transactions().countBetween(t.startMillis(), t.plusDays(1).startMillis()) > 0
    }

    suspend fun allRecurring() = db.recurring().all()
    suspend fun allAccounts() = db.accounts().all()
    suspend fun allTransactions() = db.transactions().all()
    suspend fun allCategories() = db.categories().all()
}
