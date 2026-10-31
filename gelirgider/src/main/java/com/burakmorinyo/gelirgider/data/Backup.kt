package com.burakmorinyo.gelirgider.data

import com.burakmorinyo.gelirgider.domain.Currencies
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.domain.toLocalDate
import org.json.JSONArray
import org.json.JSONObject

/** Tüm verinin JSON yedeği ve işlemlerin CSV dışa aktarımı */
object Backup {
    const val VERSION = 1

    data class Snapshot(
        val accounts: List<Account>,
        val categories: List<Category>,
        val transactions: List<Transaction>,
        val recurring: List<Recurring>,
        val budgets: List<Budget>,
        val goals: List<Goal>,
        val deposits: List<GoalDeposit>,
        val debts: List<Debt>
    )

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else getLong(key)

    private inline fun <T> List<T>.toJson(block: JSONObject.(T) -> Unit) =
        JSONArray().also { arr -> forEach { arr.put(JSONObject().apply { block(it) }) } }

    private inline fun <T> JSONObject.list(key: String, block: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return List(arr.length()) { arr.getJSONObject(it).block() }
    }

    fun toJson(s: Snapshot): String = JSONObject().apply {
        put("app", "gelirgider")
        put("version", VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("accounts", s.accounts.toJson {
            put("id", it.id); put("name", it.name); put("type", it.type.name); put("color", it.color)
            put("icon", it.icon); put("initialBalance", it.initialBalance); put("creditLimit", it.creditLimit)
            put("statementDay", it.statementDay); put("dueDay", it.dueDay); put("includeInTotal", it.includeInTotal)
            put("archived", it.archived); put("sortOrder", it.sortOrder); put("currency", it.currency)
        })
        put("categories", s.categories.toJson {
            put("id", it.id); put("name", it.name); put("type", it.type.name); put("icon", it.icon)
            put("color", it.color); put("archived", it.archived); put("sortOrder", it.sortOrder)
        })
        put("transactions", s.transactions.toJson {
            put("id", it.id); put("type", it.type.name); put("amount", it.amount); put("accountId", it.accountId)
            put("toAccountId", it.toAccountId ?: JSONObject.NULL); put("categoryId", it.categoryId ?: JSONObject.NULL)
            put("date", it.date); put("note", it.note); put("installmentGroup", it.installmentGroup ?: JSONObject.NULL)
            put("installmentIndex", it.installmentIndex); put("installmentCount", it.installmentCount)
            put("recurringId", it.recurringId ?: JSONObject.NULL); put("createdAt", it.createdAt)
            put("toAmount", it.toAmount ?: JSONObject.NULL)
        })
        put("recurring", s.recurring.toJson {
            put("id", it.id); put("title", it.title); put("type", it.type.name); put("amount", it.amount)
            put("accountId", it.accountId); put("categoryId", it.categoryId ?: JSONObject.NULL)
            put("frequency", it.frequency.name); put("nextDate", it.nextDate); put("anchorDay", it.anchorDay)
            put("autoCreate", it.autoCreate); put("remindDaysBefore", it.remindDaysBefore)
            put("isSubscription", it.isSubscription); put("active", it.active); put("endDate", it.endDate ?: JSONObject.NULL)
        })
        put("budgets", s.budgets.toJson {
            put("id", it.id); put("categoryId", it.categoryId ?: JSONObject.NULL); put("amount", it.amount)
        })
        put("goals", s.goals.toJson {
            put("id", it.id); put("name", it.name); put("target", it.target); put("saved", it.saved)
            put("deadline", it.deadline ?: JSONObject.NULL); put("color", it.color); put("icon", it.icon)
            put("createdAt", it.createdAt); put("completed", it.completed)
        })
        put("deposits", s.deposits.toJson {
            put("id", it.id); put("goalId", it.goalId); put("amount", it.amount); put("date", it.date)
        })
        put("debts", s.debts.toJson {
            put("id", it.id); put("person", it.person); put("direction", it.direction.name); put("amount", it.amount)
            put("paid", it.paid); put("dueDate", it.dueDate ?: JSONObject.NULL); put("note", it.note); put("createdAt", it.createdAt)
        })
    }.toString()

    fun fromJson(text: String): Snapshot {
        val o = JSONObject(text)
        require(o.optString("app") == "gelirgider") { "Bu dosya bir Gelir Gider yedeği değil." }
        return Snapshot(
            accounts = o.list("accounts") {
                Account(getLong("id"), getString("name"), AccountType.valueOf(getString("type")), getLong("color"),
                    getString("icon"), getLong("initialBalance"), optLong("creditLimit"), optInt("statementDay"),
                    optInt("dueDay"), optBoolean("includeInTotal", true), optBoolean("archived"), optInt("sortOrder"),
                    optString("currency", "TRY"))
            },
            categories = o.list("categories") {
                Category(getLong("id"), getString("name"), TxType.valueOf(getString("type")), getString("icon"),
                    getLong("color"), optBoolean("archived"), optInt("sortOrder"))
            },
            transactions = o.list("transactions") {
                Transaction(getLong("id"), TxType.valueOf(getString("type")), getLong("amount"), getLong("accountId"),
                    optLongOrNull("toAccountId"), optLongOrNull("categoryId"), getLong("date"), optString("note"),
                    optLongOrNull("installmentGroup"), optInt("installmentIndex"), optInt("installmentCount"),
                    optLongOrNull("recurringId"), optLong("createdAt"), optLongOrNull("toAmount"))
            },
            recurring = o.list("recurring") {
                Recurring(getLong("id"), getString("title"), TxType.valueOf(getString("type")), getLong("amount"),
                    getLong("accountId"), optLongOrNull("categoryId"), Frequency.valueOf(getString("frequency")),
                    getLong("nextDate"), getInt("anchorDay"), optBoolean("autoCreate", true), optInt("remindDaysBefore", 1),
                    optBoolean("isSubscription"), optBoolean("active", true), optLongOrNull("endDate"))
            },
            budgets = o.list("budgets") { Budget(getLong("id"), optLongOrNull("categoryId"), getLong("amount")) },
            goals = o.list("goals") {
                Goal(getLong("id"), getString("name"), getLong("target"), getLong("saved"), optLongOrNull("deadline"),
                    getLong("color"), getString("icon"), optLong("createdAt"), optBoolean("completed"))
            },
            deposits = o.list("deposits") { GoalDeposit(getLong("id"), getLong("goalId"), getLong("amount"), getLong("date")) },
            debts = o.list("debts") {
                Debt(getLong("id"), getString("person"), DebtDirection.valueOf(getString("direction")), getLong("amount"),
                    optLong("paid"), optLongOrNull("dueDate"), optString("note"), optLong("createdAt"))
            }
        )
    }

    /** Excel'de Türkçe karakterler doğru görünsün diye UTF-8 BOM ve ; ayırıcı kullanılır */
    fun toCsv(s: Snapshot): String {
        val acc = s.accounts.associateBy { it.id }
        val cat = s.categories.associateBy { it.id }
        fun esc(v: String) = if (v.any { it == ';' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
        val sb = StringBuilder("﻿Tarih;Tür;Tutar;Birim;Hesap;Hedef Hesap;Kategori;Not;Taksit\n")
        s.transactions.sortedBy { it.date }.forEach { t ->
            val type = when (t.type) { TxType.EXPENSE -> "Gider"; TxType.INCOME -> "Gelir"; TxType.TRANSFER -> "Transfer" }
            val amount = Money.format(if (t.type == TxType.EXPENSE) -t.amount else t.amount, alwaysDecimals = true, symbol = "", reveal = true)
            sb.append(t.date.toLocalDate()).append(';').append(type).append(';').append(amount).append(';')
                .append(Currencies.get(acc[t.accountId]?.currency ?: Currencies.BASE).symbol).append(';')
                .append(esc(acc[t.accountId]?.name ?: "")).append(';')
                .append(esc(t.toAccountId?.let { acc[it]?.name } ?: "")).append(';')
                .append(esc(t.categoryId?.let { cat[it]?.name } ?: "")).append(';')
                .append(esc(t.note)).append(';')
                .append(if (t.installmentCount > 1) "${t.installmentIndex}/${t.installmentCount}" else "")
                .append('\n')
        }
        return sb.toString()
    }
}
