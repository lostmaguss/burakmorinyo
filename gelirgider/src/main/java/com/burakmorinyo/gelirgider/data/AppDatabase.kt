package com.burakmorinyo.gelirgider.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun txType(v: TxType): String = v.name
    @TypeConverter fun toTxType(v: String): TxType = TxType.valueOf(v)
    @TypeConverter fun accountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)
    @TypeConverter fun frequency(v: Frequency): String = v.name
    @TypeConverter fun toFrequency(v: String): Frequency = Frequency.valueOf(v)
    @TypeConverter fun debtDirection(v: DebtDirection): String = v.name
    @TypeConverter fun toDebtDirection(v: String): DebtDirection = DebtDirection.valueOf(v)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY archived, sortOrder, id")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM accounts")
    suspend fun all(): List<Account>

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun count(): Int

    @Upsert suspend fun upsert(account: Account): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Account>)
    @Delete suspend fun delete(account: Account)
    @Query("DELETE FROM accounts") suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun all(): List<Category>

    @Upsert suspend fun upsert(category: Category): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Category>)
    @Delete suspend fun delete(category: Category)
    @Query("DELETE FROM categories") suspend fun clear()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun all(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun byId(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE installmentGroup = :group ORDER BY installmentIndex")
    suspend fun byInstallmentGroup(group: Long): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions WHERE date >= :from AND date < :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Upsert suspend fun upsert(tx: Transaction): Long
    @Insert suspend fun insertAll(items: List<Transaction>): List<Long>
    @Update suspend fun update(tx: Transaction)
    @Delete suspend fun delete(tx: Transaction)
    @Query("DELETE FROM transactions WHERE installmentGroup = :group") suspend fun deleteInstallmentGroup(group: Long)
    @Query("DELETE FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId") suspend fun deleteForAccount(accountId: Long)
    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId") suspend fun detachCategory(categoryId: Long)
    @Query("DELETE FROM transactions") suspend fun clear()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY active DESC, nextDate")
    fun observeAll(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurring")
    suspend fun all(): List<Recurring>

    @Upsert suspend fun upsert(item: Recurring): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Recurring>)
    @Delete suspend fun delete(item: Recurring)
    @Query("DELETE FROM recurring") suspend fun clear()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun all(): List<Budget>

    @Upsert suspend fun upsert(item: Budget): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Budget>)
    @Delete suspend fun delete(item: Budget)
    @Query("DELETE FROM budgets WHERE categoryId = :categoryId") suspend fun deleteForCategory(categoryId: Long)
    @Query("DELETE FROM budgets") suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY completed, createdAt")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals")
    suspend fun all(): List<Goal>

    @Query("SELECT * FROM goal_deposits ORDER BY date DESC")
    fun observeDeposits(): Flow<List<GoalDeposit>>

    @Query("SELECT * FROM goal_deposits")
    suspend fun allDeposits(): List<GoalDeposit>

    @Upsert suspend fun upsert(item: Goal): Long
    @Insert suspend fun insertDeposit(item: GoalDeposit): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAllDeposits(items: List<GoalDeposit>)
    @Delete suspend fun delete(item: Goal)
    @Query("DELETE FROM goal_deposits WHERE goalId = :goalId") suspend fun deleteDeposits(goalId: Long)
    @Query("DELETE FROM goals") suspend fun clear()
    @Query("DELETE FROM goal_deposits") suspend fun clearDeposits()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY (paid >= amount), dueDate IS NULL, dueDate, createdAt")
    fun observeAll(): Flow<List<Debt>>

    @Query("SELECT * FROM debts")
    suspend fun all(): List<Debt>

    @Upsert suspend fun upsert(item: Debt): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Debt>)
    @Delete suspend fun delete(item: Debt)
    @Query("DELETE FROM debts") suspend fun clear()
}

@Dao
interface RateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<RateRecord>)

    /** [day] gününe en yakın (öncesindeki) kayıt */
    @Query("SELECT * FROM rate_history WHERE code = :code AND day <= :day ORDER BY day DESC LIMIT 1")
    suspend fun onOrBefore(code: String, day: Long): RateRecord?
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN currency TEXT NOT NULL DEFAULT 'TRY'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN toAmount INTEGER")
        db.execSQL("CREATE TABLE IF NOT EXISTS rate_history (day INTEGER NOT NULL, code TEXT NOT NULL, rate REAL NOT NULL, PRIMARY KEY(day, code))")
    }
}

@Database(
    entities = [
        Account::class, Category::class, Transaction::class, Recurring::class,
        Budget::class, Goal::class, GoalDeposit::class, Debt::class, RateRecord::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun recurring(): RecurringDao
    abstract fun budgets(): BudgetDao
    abstract fun goals(): GoalDao
    abstract fun debts(): DebtDao
    abstract fun rates(): RateDao

    companion object {
        fun build(context: Context, inMemory: Boolean = false): AppDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries()
            } else {
                Room.databaseBuilder(context, AppDatabase::class.java, "gelirgider.db")
            }
            return builder.addMigrations(MIGRATION_1_2).build()
        }
    }
}
