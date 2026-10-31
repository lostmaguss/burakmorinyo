package com.burakmorinyo.gelirgider.domain

import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AccountType
import com.burakmorinyo.gelirgider.data.Budget
import com.burakmorinyo.gelirgider.data.Category
import com.burakmorinyo.gelirgider.data.Recurring
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import java.time.LocalDate
import kotlin.math.roundToInt

data class CardStatus(
    val lastStatement: LocalDate,
    val nextStatement: LocalDate,
    val dueDate: LocalDate,
    /** Son hesap kesimindeki borç */
    val statementDebt: Long,
    /** Ekstreden kalan ödenmemiş tutar */
    val remainingDue: Long,
    /** Hesap kesiminden bu yana yapılan harcama (sonraki ekstreye yansır) */
    val currentPeriodSpend: Long,
    val available: Long
)

data class AccountBalance(
    val account: Account,
    /** Bugüne kadarki bakiye. Kredi kartında eksi değer borç demektir. */
    val balance: Long,
    /** Kredi kartında henüz vadesi gelmemiş taksitler */
    val futureInstallments: Long,
    val card: CardStatus?
)

data class BudgetStatus(val budget: Budget, val category: Category?, val spent: Long) {
    val limit get() = budget.amount
    val remaining get() = limit - spent
    val ratio: Float get() = if (limit <= 0) 0f else spent.toFloat() / limit
}

data class CategoryTotal(val category: Category?, val total: Long, val count: Int, val share: Float)

data class Upcoming(
    val title: String,
    val date: LocalDate,
    val amount: Long,
    val type: TxType,
    val icon: String,
    val color: Long,
    val isCard: Boolean = false,
    val recurring: Recurring? = null
)

enum class Tone { GOOD, WARN, BAD, INFO }

data class Insight(val icon: String, val title: String, val message: String, val tone: Tone)

object Finance {

    fun effect(tx: Transaction, accountId: Long): Long = when {
        tx.type == TxType.INCOME && tx.accountId == accountId -> tx.amount
        tx.type == TxType.EXPENSE && tx.accountId == accountId -> -tx.amount
        tx.type == TxType.TRANSFER && tx.accountId == accountId -> -tx.amount
        tx.type == TxType.TRANSFER && tx.toAccountId == accountId -> tx.toAmount ?: tx.amount
        else -> 0
    }

    /**
     * İstatistik ve bütçeler için: döviz/altın hesaplarındaki gelir-giderleri güncel kurla ana para
     * birimine çevirir. Transferler istatistiklere girmediği için olduğu gibi kalır.
     */
    fun toBase(txs: List<Transaction>, accounts: Map<Long, Account>, rates: Rates, baseCode: String): List<Transaction> {
        if (accounts.values.all { it.currency == baseCode }) return txs
        return txs.map { tx ->
            val code = accounts[tx.accountId]?.currency ?: baseCode
            if (code == baseCode || tx.type == TxType.TRANSFER) tx
            else tx.copy(amount = rates.convertOrSame(tx.amount, code, baseCode))
        }
    }

    /** Hesap bakiyelerinin ana para birimindeki toplamı (toplama dahil, arşivlenmemiş) */
    fun netWorth(balances: List<AccountBalance>, rates: Rates, baseCode: String): Long =
        balances.filter { it.account.includeInTotal && !it.account.archived }
            .sumOf { rates.convertOrSame(it.balance, it.account.currency, baseCode) }

    fun balanceUntil(account: Account, txs: List<Transaction>, endExclusiveMillis: Long): Long =
        account.initialBalance + txs.sumOf { if (it.date < endExclusiveMillis) effect(it, account.id) else 0L }

    fun balances(accounts: List<Account>, txs: List<Transaction>, today: LocalDate = today()): List<AccountBalance> {
        val end = today.plusDays(1).startMillis()
        return accounts.map { acc ->
            val related = txs.filter { it.accountId == acc.id || it.toAccountId == acc.id }
            val balance = balanceUntil(acc, related, end)
            val future = if (acc.type == AccountType.CREDIT_CARD)
                related.filter { it.date >= end && it.type == TxType.EXPENSE && it.accountId == acc.id }.sumOf { it.amount }
            else 0L
            val card = if (acc.type == AccountType.CREDIT_CARD && acc.statementDay in 1..31)
                cardStatus(acc, related, balance, future, today) else null
            AccountBalance(acc, balance, future, card)
        }
    }

    fun cardStatus(acc: Account, txs: List<Transaction>, balance: Long, future: Long, today: LocalDate): CardStatus {
        val sd = acc.statementDay
        var last = today.withDayClamped(sd)
        if (last.isAfter(today)) last = today.minusMonths(1).withDayClamped(sd)
        val next = last.plusMonths(1).withDayClamped(sd)
        val dueDay = if (acc.dueDay in 1..31) acc.dueDay else sd
        var due = last.withDayClamped(dueDay)
        if (!due.isAfter(last)) due = last.plusMonths(1).withDayClamped(dueDay)

        val afterLast = last.plusDays(1).startMillis()
        val todayEnd = today.plusDays(1).startMillis()
        val statementDebt = (-balanceUntil(acc, txs, afterLast)).coerceAtLeast(0)
        val since = txs.filter { it.date in afterLast until todayEnd }
        val paidSince = since.sumOf { tx -> effect(tx, acc.id).let { if (it > 0) it else 0L } }
        val spentSince = since.sumOf { tx -> effect(tx, acc.id).let { if (it < 0) -it else 0L } }
        return CardStatus(
            lastStatement = last,
            nextStatement = next,
            dueDate = due,
            statementDebt = statementDebt,
            remainingDue = (statementDebt - paidSince).coerceAtLeast(0),
            currentPeriodSpend = spentSince,
            available = acc.creditLimit + balance - future
        )
    }

    fun inPeriod(txs: List<Transaction>, period: Period) = txs.filter { it.date in period }

    fun sum(txs: List<Transaction>, type: TxType) = txs.filter { it.type == type }.sumOf { it.amount }

    fun categoryTotals(txs: List<Transaction>, type: TxType, categories: Map<Long, Category>): List<CategoryTotal> {
        val filtered = txs.filter { it.type == type }
        val total = filtered.sumOf { it.amount }.coerceAtLeast(1)
        return filtered.groupBy { it.categoryId }
            .map { (cid, list) ->
                val t = list.sumOf { it.amount }
                CategoryTotal(cid?.let { categories[it] }, t, list.size, t.toFloat() / total)
            }
            .sortedByDescending { it.total }
    }

    fun budgetStatuses(budgets: List<Budget>, periodTxs: List<Transaction>, categories: Map<Long, Category>): List<BudgetStatus> {
        val expenses = periodTxs.filter { it.type == TxType.EXPENSE }
        return budgets.map { b ->
            val spent = if (b.categoryId == null) expenses.sumOf { it.amount }
            else expenses.filter { it.categoryId == b.categoryId }.sumOf { it.amount }
            BudgetStatus(b, b.categoryId?.let { categories[it] }, spent)
        }.sortedWith(compareBy<BudgetStatus> { it.budget.categoryId != null }.thenByDescending { it.ratio })
    }

    /** Dönemin her günü için harcama toplamı */
    fun dailyExpenses(periodTxs: List<Transaction>, period: Period): List<Long> {
        val arr = LongArray(period.days)
        periodTxs.filter { it.type == TxType.EXPENSE }.forEach {
            val idx = java.time.temporal.ChronoUnit.DAYS.between(period.start, it.date.toLocalDate()).toInt()
            if (idx in arr.indices) arr[idx] += it.amount
        }
        return arr.toList()
    }

    fun upcoming(
        recurring: List<Recurring>,
        balances: List<AccountBalance>,
        categories: Map<Long, Category>,
        today: LocalDate = today(),
        horizonDays: Long = 30
    ): List<Upcoming> {
        val limit = today.plusDays(horizonDays)
        val items = mutableListOf<Upcoming>()
        recurring.filter { it.active }.forEach { r ->
            var d = r.nextDate.toLocalDate()
            var guard = 0
            while (!d.isAfter(limit) && guard++ < 40) {
                if (r.endDate != null && d.isAfter(r.endDate.toLocalDate())) break
                val c = r.categoryId?.let { categories[it] }
                items += Upcoming(r.title, d, r.amount, r.type, c?.icon ?: "repeat", c?.color ?: 0xFF6366F1, recurring = r)
                d = nextOccurrence(d, r.frequency, r.anchorDay)
            }
        }
        balances.forEach { b ->
            val c = b.card ?: return@forEach
            if (c.remainingDue > 0 && !c.dueDate.isAfter(limit)) {
                items += Upcoming("${b.account.name} ekstre", c.dueDate, c.remainingDue, TxType.EXPENSE, "credit_card", b.account.color, isCard = true)
            }
        }
        return items.sortedBy { it.date }
    }

    fun insights(
        period: Period,
        startDay: Int,
        allTxs: List<Transaction>,
        categories: Map<Long, Category>,
        budgets: List<BudgetStatus>,
        upcoming: List<Upcoming>,
        recurring: List<Recurring>,
        today: LocalDate = today()
    ): List<Insight> {
        val out = mutableListOf<Insight>()
        val cur = inPeriod(allTxs, period)
        val spent = sum(cur, TxType.EXPENSE)
        val income = sum(cur, TxType.INCOME)
        val isCurrent = today in period
        val elapsed = period.elapsedDays(today).coerceAtLeast(1)
        val daysLeft = (period.days - elapsed + 1).coerceAtLeast(1)

        // 1) Günlük harcama hakkı
        val overall = budgets.firstOrNull { it.budget.categoryId == null }
        if (isCurrent) {
            val pendingExpenses = upcoming.filter { it.type == TxType.EXPENSE && it.date in period && it.date.isAfter(today) && !it.isCard }
                .sumOf { it.amount }
            val pool = when {
                overall != null -> overall.remaining - pendingExpenses
                income > 0 -> income - spent - pendingExpenses
                else -> null
            }
            if (pool != null) {
                if (pool > 0) {
                    out += Insight(
                        "savings", "Günlük harcama hakkın",
                        "Dönem sonuna $daysLeft gün var. Planına sadık kalmak için günde en fazla ${Money.format(pool / daysLeft)} harcayabilirsin." +
                            if (pendingExpenses > 0) " (Yaklaşan ${Money.format(pendingExpenses)} ödeme düşüldü.)" else "",
                        Tone.GOOD
                    )
                } else {
                    out += Insight(
                        "warning", "Bütçe aşıldı",
                        "Yaklaşan ödemelerle birlikte bu dönem ${Money.format(-pool)} açık veriyorsun. Harcamaları kısmayı düşün.",
                        Tone.BAD
                    )
                }
            }
        }

        // 2) Ay sonu tahmini: uygulamayı dönem ortasında kullanmaya başlayanlar için ilk kayıttan itibaren sayılır
        val firstDay = allTxs.minOfOrNull { it.date }?.toLocalDate()
        val trackedStart = if (firstDay != null && firstDay.isAfter(period.start)) firstDay else period.start
        val trackedDays = (java.time.temporal.ChronoUnit.DAYS.between(trackedStart, today).toInt() + 1).coerceAtLeast(1)
        val remainingDays = period.days - elapsed
        if (isCurrent && spent > 0 && trackedDays >= 3) {
            val projected = spent + spent / trackedDays * remainingDays
            val ref = overall?.limit
            val msg = if (ref != null && ref > 0) {
                val pct = (projected * 100 / ref).toInt()
                "Bu hızla dönem sonunda ${Money.format(projected)} harcamış olacaksın (bütçenin %$pct'i)."
            } else "Bu hızla dönem sonunda yaklaşık ${Money.format(projected)} harcamış olacaksın."
            out += Insight("trending_up", "Dönem sonu tahmini", msg,
                if (ref != null && projected > ref) Tone.WARN else Tone.INFO)
        }

        // 3) Geçen döneme göre (aynı gün sayısı)
        val prev = period.previous(startDay)
        val prevSameDays = allTxs.filter {
            it.type == TxType.EXPENSE && it.date >= prev.startMillis && it.date < prev.start.plusDays(elapsed.toLong()).startMillis()
        }.sumOf { it.amount }
        if (prevSameDays > 0 && spent > 0) {
            val change = ((spent - prevSameDays) * 100.0 / prevSameDays).roundToInt()
            if (change >= 10) out += Insight("arrow_upward", "Harcamaların arttı",
                "Geçen dönemin aynı günlerine göre %$change daha fazla harcadın.", Tone.WARN)
            else if (change <= -10) out += Insight("arrow_downward", "Harika gidiyorsun",
                "Geçen dönemin aynı günlerine göre %${-change} daha az harcadın.", Tone.GOOD)
        }

        // 4) Olağandışı kategori: son 3 dönem ortalamasının 1,5 katı
        val totals = categoryTotals(cur, TxType.EXPENSE, categories)
        var p = prev
        val history = mutableListOf<List<Transaction>>()
        repeat(3) { history += inPeriod(allTxs, p); p = p.previous(startDay) }
        if (history.any { it.isNotEmpty() }) {
            val ratio = if (isCurrent) period.days.toDouble() / elapsed else 1.0
            totals.take(5).firstNotNullOfOrNull { ct ->
                val cat = ct.category ?: return@firstNotNullOfOrNull null
                val avg = history.map { h -> h.filter { it.type == TxType.EXPENSE && it.categoryId == cat.id }.sumOf { it.amount } }
                    .filter { it > 0 }.let { if (it.size >= 2) it.average() else null } ?: return@firstNotNullOfOrNull null
                if (ct.total * ratio > avg * 1.5 && ct.total > avg * 0.5) {
                    Insight(cat.icon, "${cat.name} harcaman yüksek",
                        "Bu dönem ${Money.format(ct.total)}; son dönemlerin ortalaması ${Money.format(avg.toLong())}.", Tone.WARN)
                } else null
            }?.let { out += it }
        }

        // 5) En büyük kategori
        totals.firstOrNull()?.let { top ->
            if (top.category != null && spent > 0) out += Insight(top.category.icon, "En çok ${top.category.name}",
                "Harcamalarının %${(top.share * 100).roundToInt()}'i bu kategoride (${Money.format(top.total)}).", Tone.INFO)
        }

        // 6) Tasarruf oranı
        if (income > 0) {
            val rate = ((income - spent) * 100.0 / income).roundToInt()
            out += when {
                rate >= 20 -> Insight("savings", "Tasarruf oranın %$rate", "Gelirinin %$rate'ini biriktiriyorsun. Önerilen en az %20 — tebrikler!", Tone.GOOD)
                rate >= 0 -> Insight("savings", "Tasarruf oranın %$rate", "Gelirinin en az %20'sini biriktirmeyi hedefle (50/30/20 kuralı).", Tone.INFO)
                else -> Insight("warning", "Gelirinden fazla harcadın", "Bu dönem harcamaların gelirini ${Money.format(spent - income)} aştı.", Tone.BAD)
            }
        }

        // 7) Aşılan kategori bütçeleri
        budgets.filter { it.category != null && it.ratio >= 0.8f }.take(2).forEach { b ->
            out += if (b.ratio >= 1f) Insight(b.category!!.icon, "${b.category.name} bütçesi aşıldı",
                "Limit ${Money.format(b.limit)}, harcanan ${Money.format(b.spent)}.", Tone.BAD)
            else Insight(b.category!!.icon, "${b.category.name} bütçesi dolmak üzere",
                "%${(b.ratio * 100).roundToInt()} kullanıldı, ${Money.format(b.remaining)} kaldı.", Tone.WARN)
        }

        // 8) Abonelikler
        val subs = recurring.filter { it.active && it.isSubscription && it.type == TxType.EXPENSE }
        if (subs.isNotEmpty()) {
            val monthly = subs.sumOf { monthlyEquivalent(it.amount, it.frequency) }
            out += Insight("subscriptions", "${subs.size} aboneliğin var",
                "Aylık ${Money.format(monthly)}, yıllık ${Money.format(monthly * 12)}. Kullanmadıklarını iptal etmeyi düşün.", Tone.INFO)
        }

        // 9) Harcamasız günler
        if (isCurrent && elapsed >= 5) {
            val daily = dailyExpenses(cur, period).take(elapsed)
            val noSpend = daily.count { it == 0L }
            if (noSpend > 0) out += Insight("celebration", "$noSpend harcamasız gün",
                "Bu dönem $noSpend gün hiç harcama yapmadın.", Tone.GOOD)
        }
        return out
    }
}
