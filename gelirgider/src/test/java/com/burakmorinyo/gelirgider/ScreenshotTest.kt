package com.burakmorinyo.gelirgider

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.burakmorinyo.gelirgider.data.AppDatabase
import com.burakmorinyo.gelirgider.data.AppSettings
import com.burakmorinyo.gelirgider.data.Repository
import com.burakmorinyo.gelirgider.data.SettingsStore
import com.burakmorinyo.gelirgider.data.ThemeMode
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.domain.Money
import com.burakmorinyo.gelirgider.ui.AppData
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.OnboardingScreen
import com.burakmorinyo.gelirgider.ui.accounts.AccountsScreen
import com.burakmorinyo.gelirgider.ui.home.HomeScreen
import com.burakmorinyo.gelirgider.ui.plan.PlanScreen
import com.burakmorinyo.gelirgider.ui.stats.StatsScreen
import com.burakmorinyo.gelirgider.ui.theme.GelirGiderTheme
import com.burakmorinyo.gelirgider.ui.transactions.EditTransactionScreen
import com.burakmorinyo.gelirgider.ui.transactions.TransactionsScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Ekranları örnek veriyle çizip PNG olarak kaydeder (build/screenshots) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi", application = Application::class)
class ScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val out = "build/screenshots"

    private fun vm(): MainViewModel {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        return MainViewModel(Repository(AppDatabase.build(ctx, inMemory = true), SettingsStore(ctx)))
    }

    private fun shoot(name: String, dark: Boolean = false, data: AppData = SampleData.build(), content: @Composable (AppData, MainViewModel) -> Unit) {
        Money.symbol = "₺"; Money.hidden = false
        val vm = vm()
        compose.setContent {
            GelirGiderTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content(data, vm) }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("$out/$name.png")
    }

    @Test fun home() = shoot("01_home") { d, vm -> HomeScreen(d, vm, rememberNavController()) }

    @Config(qualifiers = "w400dp-h2300dp-xxhdpi")
    @Test fun homeTall() = shoot("01b_home_tall") { d, vm -> HomeScreen(d, vm, rememberNavController()) }

    @Config(qualifiers = "w400dp-h2600dp-xxhdpi")
    @Test fun statsTall() = shoot("04b_stats_tall") { d, vm -> StatsScreen(d, vm, rememberNavController()) }

    @Config(qualifiers = "w400dp-h1900dp-xxhdpi")
    @Test fun accountsTall() = shoot("10b_accounts_tall") { d, vm -> AccountsScreen(d, vm, rememberNavController()) }

    @Config(qualifiers = "w400dp-h2400dp-xxhdpi")
    @Test fun redCard() = shoot("14_card_red") { d, vm -> com.burakmorinyo.gelirgider.ui.accounts.CardDetailScreen(d, vm, rememberNavController(), 3) }

    @Config(qualifiers = "w400dp-h2400dp-xxhdpi")
    @Test fun blueCard() = shoot("15_card_blue") { d, vm -> com.burakmorinyo.gelirgider.ui.accounts.CardDetailScreen(d, vm, rememberNavController(), 6) }

    @Test fun addInstallment() = shoot("16_add_installment") { d, vm ->
        EditTransactionScreen(d, vm, rememberNavController(), 0, TxType.EXPENSE, presetAccount = 6, installmentMode = true)
    }

    @Test fun homeDark() = shoot("02_home_dark", dark = true) { d, vm -> HomeScreen(d, vm, rememberNavController()) }

    @Test fun transactions() = shoot("03_transactions") { d, vm -> TransactionsScreen(d, vm, rememberNavController(), 0, 0, true) }

    @Test fun stats() = shoot("04_stats") { d, vm -> StatsScreen(d, vm, rememberNavController()) }

    @Test fun planBudget() = shoot("05_plan_budget") { d, vm -> PlanScreen(d, vm, rememberNavController()) }

    @Test fun planPayments() {
        shoot("06_plan_payments_pre") { d, vm -> PlanScreen(d, vm, rememberNavController()) }
        compose.onNodeWithText("Ödemeler").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("$out/06_plan_payments.png")
        compose.onNodeWithText("Hedefler").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("$out/07_plan_goals.png")
        compose.onNodeWithText("Borç/Alacak").performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("$out/08_plan_debts.png")
    }

    @Test fun addExpense() = shoot("09_add_expense") { d, vm ->
        EditTransactionScreen(d.copy(settings = d.settings.copy(lastAccountId = 3)), vm, rememberNavController(), 0, TxType.EXPENSE)
    }

    @Test fun accounts() = shoot("10_accounts") { d, vm -> AccountsScreen(d, vm, rememberNavController()) }

    @Test fun statsDark() = shoot("11_stats_dark", dark = true) { d, vm -> StatsScreen(d, vm, rememberNavController()) }

    @Test fun onboarding() = shoot("12_onboarding") { _, _ -> OnboardingScreen {} }

    @Test fun emptyHome() = shoot("13_home_empty", data = AppData(settings = AppSettings(onboarded = true), loaded = true)) { d, vm ->
        HomeScreen(d, vm, rememberNavController())
    }
}
