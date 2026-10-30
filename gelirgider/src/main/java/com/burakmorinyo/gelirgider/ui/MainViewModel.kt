package com.burakmorinyo.gelirgider.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burakmorinyo.gelirgider.data.Account
import com.burakmorinyo.gelirgider.data.AppSettings
import com.burakmorinyo.gelirgider.data.Backup
import com.burakmorinyo.gelirgider.data.Budget
import com.burakmorinyo.gelirgider.data.Category
import com.burakmorinyo.gelirgider.data.Debt
import com.burakmorinyo.gelirgider.data.Goal
import com.burakmorinyo.gelirgider.data.GoalDeposit
import com.burakmorinyo.gelirgider.data.Recurring
import com.burakmorinyo.gelirgider.data.Repository
import com.burakmorinyo.gelirgider.data.Transaction
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.AccountBalance
import com.burakmorinyo.gelirgider.domain.Finance
import com.burakmorinyo.gelirgider.domain.Period
import com.burakmorinyo.gelirgider.domain.periodFor
import com.burakmorinyo.gelirgider.domain.today
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ekranların ortak kullandığı tüm veri tek bir anlık görüntüde */
data class AppData(
    val settings: AppSettings = AppSettings(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val recurring: List<Recurring> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val deposits: List<GoalDeposit> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val loaded: Boolean = false
) {
    val categoryMap: Map<Long, Category> by lazy { categories.associateBy { it.id } }
    val accountMap: Map<Long, Account> by lazy { accounts.associateBy { it.id } }
    val activeAccounts: List<Account> by lazy { accounts.filter { !it.archived } }
    val balances: List<AccountBalance> by lazy { Finance.balances(accounts, transactions) }
    val netWorth: Long by lazy { balances.filter { it.account.includeInTotal && !it.account.archived }.sumOf { it.balance } }
    val currentPeriod: Period by lazy { periodFor(today(), settings.periodStartDay) }

    fun categoriesOf(type: TxType) = categories.filter { it.type == type && !it.archived }

    /** Son 90 günde en çok kullanılan kategoriler önce gelir */
    fun categoriesByUsage(type: TxType): List<Category> {
        val since = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
        val usage = transactions.asSequence().filter { it.type == type && it.date >= since && it.categoryId != null }
            .groupingBy { it.categoryId!! }.eachCount()
        return categoriesOf(type).sortedWith(compareByDescending<Category> { usage[it.id] ?: 0 }.thenBy { it.sortOrder })
    }
}

sealed interface UiEvent {
    data class Message(val text: String, val undo: (() -> Unit)? = null) : UiEvent
}

class MainViewModel(val repo: Repository) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    val data: StateFlow<AppData> = combine(
        listOf<Flow<Any>>(
            repo.settings!!.flow, repo.accounts, repo.categories, repo.transactions, repo.recurring,
            repo.budgets, repo.goals, repo.deposits, repo.debts
        )
    ) { a ->
        AppData(
            settings = a[0] as AppSettings,
            accounts = a[1] as List<Account>,
            categories = a[2] as List<Category>,
            transactions = a[3] as List<Transaction>,
            recurring = a[4] as List<Recurring>,
            budgets = a[5] as List<Budget>,
            goals = a[6] as List<Goal>,
            deposits = a[7] as List<GoalDeposit>,
            debts = a[8] as List<Debt>,
            loaded = true
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, AppData())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    private fun launch(block: suspend () -> Unit) = viewModelScope.launch { block() }
    fun message(text: String, undo: (() -> Unit)? = null) { _events.tryEmit(UiEvent.Message(text, undo)) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = launch { repo.settings!!.update(transform) }

    fun saveTransaction(tx: Transaction, installments: Int, makeRecurring: Recurring?) = launch {
        repo.saveTransaction(tx, installments)
        makeRecurring?.let { repo.saveRecurring(it) }
    }

    fun deleteTransaction(tx: Transaction, wholeGroup: Boolean) = launch {
        val removed = if (wholeGroup && tx.installmentGroup != null) repo.installmentsOf(tx.installmentGroup) else listOf(tx)
        repo.deleteTransaction(tx, wholeGroup)
        message(if (removed.size > 1) "${removed.size} taksit silindi" else "İşlem silindi") {
            launch { repo.restore(removed) }
        }
    }

    fun saveAccount(a: Account) = launch { repo.saveAccount(a) }
    fun deleteAccount(a: Account) = launch { repo.deleteAccount(a); message("${a.name} silindi") }
    fun saveCategory(c: Category) = launch { repo.saveCategory(c) }
    fun deleteCategory(c: Category) = launch { repo.deleteCategory(c); message("${c.name} silindi") }
    fun saveRecurring(r: Recurring) = launch { repo.saveRecurring(r) }
    fun deleteRecurring(r: Recurring) = launch { repo.deleteRecurring(r) }
    fun markPaid(r: Recurring) = launch { repo.markRecurringPaid(r); message("${r.title} ödendi olarak kaydedildi") }
    fun skip(r: Recurring) = launch { repo.skipRecurring(r); message("${r.title} bu sefer atlandı") }
    fun setBudget(categoryId: Long?, amount: Long) = launch { repo.setBudget(categoryId, amount) }
    fun deleteBudget(b: Budget) = launch { repo.deleteBudget(b) }
    fun saveGoal(g: Goal) = launch { repo.saveGoal(g) }
    fun deleteGoal(g: Goal) = launch { repo.deleteGoal(g) }
    fun addDeposit(g: Goal, amount: Long) = launch {
        repo.addDeposit(g, amount)
        if (amount > 0 && g.saved + amount >= g.target && !g.completed) message("Tebrikler! “${g.name}” hedefine ulaştın 🎉")
    }
    fun saveDebt(d: Debt) = launch { repo.saveDebt(d) }
    fun deleteDebt(d: Debt) = launch { repo.deleteDebt(d) }
    fun payDebt(d: Debt, amount: Long, accountId: Long?) = launch { repo.payDebt(d, amount, accountId) }

    suspend fun exportJson(): String = Backup.toJson(repo.snapshot())
    suspend fun exportCsv(): String = Backup.toCsv(repo.snapshot())
    suspend fun importJson(text: String) = repo.restoreSnapshot(Backup.fromJson(text))
}
