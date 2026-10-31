package com.burakmorinyo.gelirgider

import android.app.Application
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.burakmorinyo.gelirgider.data.AppDatabase
import com.burakmorinyo.gelirgider.data.MIGRATION_1_2
import com.burakmorinyo.gelirgider.data.MIGRATION_2_3
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Eski sürümü (v1) kullananların verisi güncellemede kaybolmamalı */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun v2ToV3MovesCardDebtIntoCurrentCycle() {
        helper.createDatabase("mig3.db", 2).apply {
            execSQL("INSERT INTO accounts (id, name, type, color, icon, initialBalance, creditLimit, statementDay, dueDay, includeInTotal, archived, sortOrder, currency) VALUES (1, 'Kart', 'CREDIT_CARD', 0, 'credit_card', -320000, 6000000, 1, 10, 1, 0, 0, 'TRY')")
            execSQL("INSERT INTO accounts (id, name, type, color, icon, initialBalance, creditLimit, statementDay, dueDay, includeInTotal, archived, sortOrder, currency) VALUES (2, 'Banka', 'BANK', 0, 'bank', 500000, 0, 0, 0, 1, 0, 1, 'TRY')")
            close()
        }
        helper.runMigrationsAndValidate("mig3.db", 3, true, MIGRATION_2_3).close()
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "mig3.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).allowMainThreadQueries().build()
        runBlocking {
            val card = db.accounts().all().first { it.id == 1L }
            assertEquals(0L, card.initialBalance)
            assert(card.assumePaid)
            assertEquals(500000L, db.accounts().all().first { it.id == 2L }.initialBalance)
            val tx = db.transactions().all().single()
            assertEquals(320000L, tx.amount)
            assertEquals("Devreden kart borcu", tx.note)
            val st = com.burakmorinyo.gelirgider.domain.CardStatements.status(card, db.transactions().all())
            assertEquals(320000L, st.debt)
        }
        db.close()
    }

    @Test fun v1ToV2KeepsData() {
        helper.createDatabase("mig.db", 1).apply {
            execSQL("INSERT INTO accounts (id, name, type, color, icon, initialBalance, creditLimit, statementDay, dueDay, includeInTotal, archived, sortOrder) VALUES (1, 'Banka', 'BANK', 0, 'bank', 500000, 0, 0, 0, 1, 0, 0)")
            execSQL("INSERT INTO transactions (id, type, amount, accountId, toAccountId, categoryId, date, note, installmentGroup, installmentIndex, installmentCount, recurringId, createdAt) VALUES (1, 'EXPENSE', 1250, 1, NULL, NULL, 1700000000000, 'Kahve', NULL, 0, 0, NULL, 0)")
            close()
        }
        helper.runMigrationsAndValidate("mig.db", 3, true, MIGRATION_1_2, MIGRATION_2_3).close()

        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "mig.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).allowMainThreadQueries().build()
        runBlocking {
            val acc = db.accounts().all().single()
            assertEquals("TRY", acc.currency)
            assertEquals(500000L, acc.initialBalance)
            val tx = db.transactions().all().single()
            assertEquals("Kahve", tx.note)
            assertNull(tx.toAmount)
        }
        db.close()
    }
}
