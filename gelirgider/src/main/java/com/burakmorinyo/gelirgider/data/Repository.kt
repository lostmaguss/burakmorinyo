package com.burakmorinyo.gelirgider.data

import androidx.room.withTransaction
import com.burakmorinyo.gelirgider.domain.CardStatements
import com.burakmorinyo.gelirgider.domain.Currencies
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.Rates
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
        // Döviz/altın hesaplarındaki giderler güncel kurla ana para birimine çevrilir
        val accounts = db.accounts().all().associateBy { it.id }
        val baseCode = Currencies.codeForSymbol(s.currency)
        val periodExpenses = Finance.toBase(
            db.transactions().all().filter { it.type == TxType.EXPENSE && it.date in period }, accounts, s.rates, baseCode
        )
        for (b in db.budgets().all()) {
            if (b.categoryId != null && b.categoryId != tx.categoryId) continue
            if (b.amount <= 0) continue
            val spent = periodExpenses.filter { b.categoryId == null || it.categoryId == b.categoryId }.sumOf { it.amount }
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

    // ---------- Kurlar ----------

    /** Kurlar [maxAgeMs]'den eskiyse internetten günceller. Başarısızsa eski kurlar kalır. */
    suspend fun refreshRates(maxAgeMs: Long = 60 * 60 * 1000L): Result<Rates> {
        val store = settings ?: return Result.failure(IllegalStateException("ayar yok"))
        val current = store.current().rates
        if (!current.isEmpty && System.currentTimeMillis() - current.updatedAt < maxAgeMs) return Result.success(current)
        return runCatching { RatesService.fetch() }.onSuccess { r ->
            store.update { it.copy(rates = r) }
            val day = today().toEpochDay()
            db.rates().insertAll(r.values.map { (code, rate) -> RateRecord(day, code, rate) })
        }
    }

    /** Geçmiş bir günün dolar kuru: önce yerel geçmişe, yoksa TCMB arşivine bakar */
    suspend fun usdRateOn(date: LocalDate): Double? {
        val day = date.toEpochDay()
        db.rates().onOrBefore("USD", day)?.takeIf { day - it.day <= 7 }?.let { return it.rate }
        val (d, rate) = RatesService.historicalUsd(date) ?: return null
        db.rates().insertAll(listOf(RateRecord(d.toEpochDay(), "USD", rate)))
        return rate
    }

    // ---------- Kredi kartı ----------

    /**
     * "Kartımdaki borç şu kadar oldu": açık dönemin (ya da toplam borcun) gerçek tutarını eşitlemek için
     * bugüne bir düzeltme kaydı ekler. [includeUnpaid] true ise girilen tutar kesilmiş ödenmemiş ekstreleri de kapsar.
     */
    suspend fun setCardDebt(card: Account, amount: Long, includeUnpaid: Boolean = false): Long {
        val txs = db.transactions().all().filter { it.accountId == card.id || it.toAccountId == card.id }
        val st = CardStatements.status(card, txs)
        val current = if (includeUnpaid) st.debt else st.currentPeriodSpend - (st.openStatement?.paid ?: 0)
        val diff = amount - current
        if (diff == 0L) return 0
        db.transactions().upsert(
            Transaction(
                type = if (diff > 0) TxType.EXPENSE else TxType.INCOME,
                amount = kotlin.math.abs(diff),
                accountId = card.id,
                date = System.currentTimeMillis(),
                note = if (diff > 0) "Kart harcamaları (toplu)" else "Kart borcu düzeltmesi"
            )
        )
        return diff
    }

    /** Ekstre ödemesi: banka/nakit hesabından karta transfer */
    suspend fun payCard(card: Account, fromAccountId: Long, amount: Long) {
        db.transactions().upsert(
            Transaction(
                type = TxType.TRANSFER, amount = amount, accountId = fromAccountId, toAccountId = card.id,
                date = System.currentTimeMillis(), note = "${card.name} ödemesi"
            )
        )
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

    suspend fun saveRecurring(r: Recurring): Long {
        // Hatırlatmalı yeni kayıtta geçmiş tarihler atlanır: Mart'ta başlamış bir ödeme için geçmiş ayları
        // tek tek "ödendi" diye işaretlemek gerekmesin
        var item = r
        if (r.id == 0L && !r.autoCreate) {
            var guard = 0
            while (item.active && item.nextDate.toLocalDate().isBefore(today()) && guard++ < 400) item = item.advanced()
        }
        return db.recurring().upsert(item)
    }
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

    /**
     * Yedekteki verileri mevcut verilerin üzerine yazmadan ekler. Hesap ve kategoriler adına göre eşleştirilir,
     * olmayanlar oluşturulur. Eklenen işlem sayısını döner.
     */
    suspend fun mergeSnapshot(s: Backup.Snapshot): Int = db.withTransaction {
        val accounts = db.accounts().all()
        val accMap = s.accounts.associate { a ->
            val match = accounts.firstOrNull { it.name.equals(a.name, ignoreCase = true) && it.type == a.type }
            a.id to (match?.id ?: db.accounts().upsert(a.copy(id = 0, sortOrder = accounts.size + a.sortOrder)))
        }
        val categories = db.categories().all()
        val catMap = s.categories.associate { c ->
            val match = categories.firstOrNull { it.name.equals(c.name, ignoreCase = true) && it.type == c.type }
            c.id to (match?.id ?: db.categories().upsert(c.copy(id = 0)))
        }
        val groupBase = System.currentTimeMillis()
        val groupMap = s.transactions.mapNotNull { it.installmentGroup }.distinct().withIndex().associate { (i, g) -> g to groupBase + i }
        val txs = s.transactions.mapNotNull { t ->
            val acc = accMap[t.accountId] ?: return@mapNotNull null
            t.copy(
                id = 0, accountId = acc, toAccountId = t.toAccountId?.let { accMap[it] },
                categoryId = t.categoryId?.let { catMap[it] }, installmentGroup = t.installmentGroup?.let { groupMap[it] },
                recurringId = null
            )
        }
        db.transactions().insertAll(txs)
        s.recurring.forEach { r ->
            val acc = accMap[r.accountId] ?: return@forEach
            db.recurring().upsert(r.copy(id = 0, accountId = acc, categoryId = r.categoryId?.let { catMap[it] }))
        }
        s.budgets.forEach { b ->
            val cat = b.categoryId?.let { catMap[it] ?: return@forEach }
            val existing = db.budgets().all().firstOrNull { it.categoryId == cat }
            db.budgets().upsert((existing ?: Budget(categoryId = cat, amount = b.amount)).copy(amount = b.amount))
        }
        val goalMap = s.goals.associate { g -> g.id to db.goals().upsert(g.copy(id = 0)) }
        db.goals().insertAllDeposits(s.deposits.mapNotNull { d -> goalMap[d.goalId]?.let { d.copy(id = 0, goalId = it) } })
        s.debts.forEach { db.debts().upsert(it.copy(id = 0)) }
        txs.size
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
