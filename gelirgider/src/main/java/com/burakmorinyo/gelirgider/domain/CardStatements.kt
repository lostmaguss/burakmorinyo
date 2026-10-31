package com.burakmorinyo.gelirgider.domain

import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class StatementStatus(val label: String) {
    /** Kayıtlı ödemelerle kapandı */
    PAID("Ödendi"),
    /** Son ödeme günü geçti, ödemesi girilmedi ama ödenmiş sayılıyor */
    ASSUMED_PAID("Ödendi"),
    /** Kesildi, son ödeme günü gelmedi */
    DUE("Ödenecek"),
    /** Son ödeme günü geçti, ödenmedi (sadece "ödenmiş say" kapalıysa) */
    OVERDUE("Gecikti"),
    /** Şu an içinde bulunulan dönem */
    OPEN("Açık dönem"),
    /** Gelecek aylar: sadece taksitlerden oluşur */
    UPCOMING("Gelecek")
}

data class Statement(
    /** Bu ekstreye giren ilk gün (önceki kesimin ertesi günü) */
    val start: LocalDate,
    /** Hesap kesim tarihi */
    val closing: LocalDate,
    val dueDate: LocalDate,
    /** Ekstreye yansıyan harcamalar, taksit dilimleri ve iadeler */
    val items: List<Transaction>,
    /** Harcamalar − iadeler */
    val total: Long,
    /** Kayıtlı ödemelerden bu ekstreye düşen kısım */
    val paid: Long,
    val status: StatementStatus
) {
    val remaining: Long get() = when (status) {
        StatementStatus.PAID, StatementStatus.ASSUMED_PAID -> 0
        else -> (total - paid).coerceAtLeast(0)
    }
}

/**
 * Kredi kartı ekstreleri. Her harcama, tarihinden sonraki ilk hesap kesimine yazılır (kesim günü dahil).
 * Karta yapılan ödemeler (transferler) en eski ödenmemiş ekstreden başlayarak kapatılır.
 * [Account.assumePaid] açıksa son ödeme günü geçmiş ekstreler ödenmiş sayılır: Mart'tan beri süren bir
 * taksiti girdiğinde geçmiş ayları tek tek "ödendi" diye işaretlemen gerekmez.
 */
object CardStatements {

    fun closingFor(date: LocalDate, statementDay: Int): LocalDate {
        val c = date.withDayClamped(statementDay)
        return if (date.isAfter(c)) date.plusMonths(1).withDayClamped(statementDay) else c
    }

    fun dueFor(closing: LocalDate, dueDay: Int): LocalDate {
        val d = closing.withDayClamped(dueDay)
        return if (d.isAfter(closing)) d else closing.plusMonths(1).withDayClamped(dueDay)
    }

    /** Karta borç yazan hareketler: harcama ve karttan çekilen nakit/transfer; iadeler eksi */
    private fun charge(tx: Transaction, cardId: Long): Long = when {
        tx.accountId == cardId && tx.type == TxType.EXPENSE -> tx.amount
        tx.accountId == cardId && tx.type == TxType.TRANSFER -> tx.amount
        tx.accountId == cardId && tx.type == TxType.INCOME -> -tx.amount
        else -> 0
    }

    private fun payment(tx: Transaction, cardId: Long): Long =
        if (tx.type == TxType.TRANSFER && tx.toAccountId == cardId) tx.toAmount ?: tx.amount else 0

    /**
     * Kartın tüm ekstreleri: eskiden yeniye (geçmiş, ödenecek, açık dönem, gelecek).
     * Ödemeler tarih sırasıyla işlenir: her ödeme, tarihinden önce kesilmiş en eski ödenmemiş ekstreye gider;
     * artanı bir sonraki ekstreden düşülür. Son ödeme günü geçmiş ve ödenmemiş ekstre ([Account.assumePaid])
     * ödenmiş sayılır ve sonraki ödemeleri yutmaz.
     */
    fun build(card: Account, txs: List<Transaction>, today: LocalDate = today()): List<Statement> {
        val sd = card.statementDay.coerceIn(1, 31)
        val dd = if (card.dueDay in 1..31) card.dueDay else sd
        val openClosing = closingFor(today, sd)

        val byClosing = txs.filter { charge(it, card.id) != 0L }
            .groupBy { closingFor(it.date.toLocalDate(), sd) }.toMutableMap()
        byClosing.putIfAbsent(openClosing, emptyList()) // açık dönem her zaman listede olsun
        val closings = byClosing.keys.sorted()

        val payments = txs.filter { payment(it, card.id) > 0 }
            .map { it.date.toLocalDate() to payment(it, card.id) }.sortedBy { it.first }.toMutableList()

        class Acc(val closing: LocalDate, val due: LocalDate, val total: Long) {
            var paid = 0L
            var assumed = false
            val remaining get() = total - paid
        }
        val accs = closings.map { c -> Acc(c, dueFor(c, dd), byClosing.getValue(c).sumOf { charge(it, card.id) }) }
        val unpaid = ArrayDeque<Acc>()
        var credit = card.initialBalance.coerceAtLeast(0)

        fun allocate(amount: Long) {
            var left = amount
            while (left > 0 && unpaid.isNotEmpty()) {
                val a = unpaid.first()
                val pay = minOf(left, a.remaining)
                a.paid += pay; left -= pay
                if (a.remaining <= 0) unpaid.removeFirst()
            }
            credit += left
        }

        fun markAssumed(at: LocalDate) {
            if (!card.assumePaid) return
            unpaid.removeAll { a -> a.due.isBefore(at).also { if (it) a.assumed = true } }
        }

        for (a in accs) {
            if (a.closing.isAfter(openClosing)) break // gelecek ekstreler: sadece taksit
            val isOpen = a.closing == openClosing
            // Bu döneme kadarki ödemeler (açık dönemde bugüne kadar olanların hepsi)
            while (payments.isNotEmpty() && (isOpen || !payments.first().first.isAfter(a.closing))) allocate(payments.removeAt(0).second)
            markAssumed(if (isOpen) today else a.closing)
            val fromCredit = minOf(credit, a.total.coerceAtLeast(0))
            a.paid += fromCredit; credit -= fromCredit
            if (!isOpen && a.remaining > 0) unpaid.addLast(a)
        }

        return accs.map { a ->
            val items = byClosing.getValue(a.closing).sortedBy { it.date }
            val status = when {
                a.closing.isAfter(openClosing) -> StatementStatus.UPCOMING
                a.closing == openClosing -> StatementStatus.OPEN
                a.remaining <= 0 -> StatementStatus.PAID
                a.assumed -> StatementStatus.ASSUMED_PAID
                !a.due.isBefore(today) -> StatementStatus.DUE
                card.assumePaid -> StatementStatus.ASSUMED_PAID
                else -> StatementStatus.OVERDUE
            }
            Statement(a.closing.minusMonths(1).withDayClamped(sd).plusDays(1), a.closing, a.due, items, a.total, a.paid, status)
        }
    }

    /**
     * Kartın özet durumu. Borç = ödenmemiş kesilmiş ekstreler + açık dönem; gelecek taksitler ayrıca.
     */
    fun status(card: Account, txs: List<Transaction>, today: LocalDate = today()): CardStatus {
        val list = build(card, txs, today)
        val open = list.first { it.status == StatementStatus.OPEN }
        val closed = list.filter { it.closing.isBefore(open.closing) }
        val last = closed.lastOrNull()
        val unpaid = closed.sumOf { it.remaining }
        val upcoming = list.filter { it.status == StatementStatus.UPCOMING }.sumOf { it.total }
        val openRemaining = open.total - open.paid
        val debt = unpaid + openRemaining
        val daysToClose = ChronoUnit.DAYS.between(today, open.closing).toInt() + 1
        return CardStatus(
            lastStatement = last?.closing ?: open.closing.minusMonths(1).withDayClamped(card.statementDay.coerceIn(1, 31)),
            nextStatement = open.closing,
            // Ödenmemiş ekstre varsa onun, yoksa açık dönemin son ödeme günü
            dueDate = closed.firstOrNull { it.remaining > 0 }?.dueDate ?: open.dueDate,
            statementDebt = last?.total ?: 0,
            remainingDue = unpaid,
            currentPeriodSpend = open.total,
            available = card.creditLimit - debt - upcoming,
            debt = debt,
            futureInstallments = upcoming,
            openStatement = open,
            statements = list,
            daysToClose = daysToClose,
            cycleRemaining = if (card.cycleBudget > 0) card.cycleBudget - open.total else null,
            overdue = closed.any { it.status == StatementStatus.OVERDUE }
        )
    }

    /** Aynı alışverişin taksitleri: kaçıncı taksitte olunduğu ve sonrasında kalan tutar */
    data class InstallmentPlan(
        val group: Long,
        val title: String,
        val perInstallment: Long,
        val count: Int,
        val total: Long,
        /** Bu ayın (açık dönemin) taksiti dahil, ekstreye yansımış taksit sayısı: "8/9" */
        val billed: Int,
        val remainingAmount: Long,
        val lastClosing: LocalDate,
        val categoryId: Long?,
        /** Son taksit de kesilmiş bir ekstreye yansıdıysa plan bitmiştir */
        val finished: Boolean
    )

    fun installmentPlans(card: Account, txs: List<Transaction>, today: LocalDate = today()): List<InstallmentPlan> {
        val sd = card.statementDay.coerceIn(1, 31)
        val openClosing = closingFor(today, sd)
        return txs.filter { it.accountId == card.id && it.installmentGroup != null }
            .groupBy { it.installmentGroup!! }
            .map { (g, parts) ->
                val sorted = parts.sortedBy { it.installmentIndex }
                val billedParts = sorted.filter { !closingFor(it.date.toLocalDate(), sd).isAfter(openClosing) }
                val lastClosing = closingFor(sorted.last().date.toLocalDate(), sd)
                InstallmentPlan(
                    group = g,
                    title = sorted.first().note.ifBlank { "Taksitli alışveriş" },
                    perInstallment = sorted.last().amount,
                    count = sorted.first().installmentCount.coerceAtLeast(sorted.size),
                    total = sorted.sumOf { it.amount },
                    billed = billedParts.size,
                    remainingAmount = sorted.sumOf { it.amount } - billedParts.sumOf { it.amount },
                    lastClosing = lastClosing,
                    categoryId = sorted.first().categoryId,
                    finished = lastClosing.isBefore(openClosing)
                )
            }
            .sortedWith(compareBy<InstallmentPlan> { it.finished }.thenBy { it.lastClosing })
    }
}
