package com.burakmorinyo.gelirgider.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tutarlar her yerde kuruş cinsinden Long olarak saklanır (kayan nokta hatası olmasın).

enum class TxType { EXPENSE, INCOME, TRANSFER }

enum class AccountType(val label: String) {
    CASH("Nakit"),
    BANK("Banka hesabı"),
    CREDIT_CARD("Kredi kartı"),
    SAVINGS("Birikim / Vadeli"),
    INVESTMENT("Yatırım (altın, döviz, fon)")
}

enum class Frequency(val label: String) {
    DAILY("Her gün"), WEEKLY("Her hafta"), MONTHLY("Her ay"), YEARLY("Her yıl")
}

enum class DebtDirection(val label: String) {
    I_OWE("Borcum"), OWED_TO_ME("Alacağım")
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val color: Long,
    val icon: String,
    val initialBalance: Long = 0,
    /** Kredi kartı limiti (kuruş) */
    val creditLimit: Long = 0,
    /** Kredi kartı hesap kesim günü (1-31), 0 = yok */
    val statementDay: Int = 0,
    /** Kredi kartı son ödeme günü (1-31), 0 = yok */
    val dueDay: Int = 0,
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    /** Hesabın para birimi kodu (bkz. [com.burakmorinyo.gelirgider.domain.Currencies]); tutarlar bu birimin 1/100'ü */
    val currency: String = "TRY",
    /** Kredi kartı: her hesap dönemi için harcama hedefi (kuruş), 0 = yok */
    val cycleBudget: Long = 0,
    /** Kredi kartı: son ödeme tarihi geçen ekstreler ödenmiş sayılır (her ödemeyi girmek gerekmez) */
    val assumePaid: Boolean = true
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val icon: String,
    val color: Long,
    val archived: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "transactions",
    indices = [Index("date"), Index("accountId"), Index("categoryId"), Index("installmentGroup")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amount: Long,
    val accountId: Long,
    /** Sadece transferlerde hedef hesap */
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val date: Long,
    val note: String = "",
    /** Taksitli alışverişlerde aynı alışverişin tüm taksitleri aynı grup numarasını taşır */
    val installmentGroup: Long? = null,
    val installmentIndex: Int = 0,
    val installmentCount: Int = 0,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Farklı para birimli hesaplar arası transferde hedefe giren miktar (ör. 30.000 ₺ → 4,82 gr altın) */
    val toAmount: Long? = null
)

@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: TxType,
    val amount: Long,
    val accountId: Long,
    val categoryId: Long?,
    val frequency: Frequency,
    /** Sıradaki ödeme/gelir günü (gün başlangıcı, epoch ms) */
    val nextDate: Long,
    /** Ayın 31'i gibi günler kısa aylarda kaymasın diye ilk gün saklanır */
    val anchorDay: Int,
    /** Günü gelince işlemi kendiliğinden ekle; kapalıysa sadece hatırlat */
    val autoCreate: Boolean = true,
    val remindDaysBefore: Int = 1,
    val isSubscription: Boolean = false,
    val active: Boolean = true,
    val endDate: Long? = null
)

/** Aylık bütçe. categoryId null ise toplam aylık harcama bütçesi. */
@Entity(tableName = "budgets", indices = [Index(value = ["categoryId"], unique = true)])
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long?,
    val amount: Long
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val saved: Long = 0,
    val deadline: Long? = null,
    val color: Long,
    val icon: String,
    val createdAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)

@Entity(tableName = "goal_deposits", indices = [Index("goalId")])
data class GoalDeposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long,
    val date: Long
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: DebtDirection,
    val amount: Long,
    val paid: Long = 0,
    val dueDate: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val remaining: Long get() = (amount - paid).coerceAtLeast(0)
    val settled: Boolean get() = paid >= amount
}

/** Günlük kur geçmişi: 1 birim = [rate] TL. Enflasyon/dolar karşılaştırmaları için. */
@Entity(tableName = "rate_history", primaryKeys = ["day", "code"])
data class RateRecord(
    /** LocalDate.toEpochDay() */
    val day: Long,
    val code: String,
    val rate: Double
)
