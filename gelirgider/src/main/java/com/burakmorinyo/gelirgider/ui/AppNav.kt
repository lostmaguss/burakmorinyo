package com.burakmorinyo.gelirgider.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burakmorinyo.gelirgider.data.TxType
import com.burakmorinyo.gelirgider.ui.accounts.AccountEditScreen
import com.burakmorinyo.gelirgider.ui.accounts.AccountsScreen
import com.burakmorinyo.gelirgider.ui.home.HomeScreen
import com.burakmorinyo.gelirgider.ui.plan.DebtEditScreen
import com.burakmorinyo.gelirgider.ui.plan.GoalEditScreen
import com.burakmorinyo.gelirgider.ui.plan.PlanScreen
import com.burakmorinyo.gelirgider.ui.plan.RecurringEditScreen
import com.burakmorinyo.gelirgider.ui.settings.CategoriesScreen
import com.burakmorinyo.gelirgider.ui.settings.SettingsScreen
import com.burakmorinyo.gelirgider.ui.stats.StatsScreen
import com.burakmorinyo.gelirgider.ui.transactions.EditTransactionScreen
import com.burakmorinyo.gelirgider.ui.transactions.TransactionsScreen

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val STATS = "stats"
    const val PLAN = "plan"
    const val HISTORY = "history?account={account}&category={category}"
    const val TX = "tx?id={id}&type={type}"
    const val ACCOUNTS = "accounts"
    const val ACCOUNT = "account?id={id}"
    const val CATEGORIES = "categories"
    const val SETTINGS = "settings"
    const val RECURRING = "recurring?id={id}&sub={sub}"
    const val GOAL = "goal?id={id}"
    const val DEBT = "debt?id={id}"

    fun tx(id: Long = 0, type: TxType = TxType.EXPENSE) = "tx?id=$id&type=${type.name}"
    fun history(account: Long = 0, category: Long = 0) = "history?account=$account&category=$category"
    fun account(id: Long = 0) = "account?id=$id"
    fun recurring(id: Long = 0, subscription: Boolean = false) = "recurring?id=$id&sub=$subscription"
    fun goal(id: Long = 0) = "goal?id=$id"
    fun debt(id: Long = 0) = "debt?id=$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Özet", Icons.Rounded.Home),
    Tab(Routes.TRANSACTIONS, "İşlemler", Icons.AutoMirrored.Rounded.ReceiptLong),
    Tab(Routes.STATS, "Analiz", Icons.Rounded.PieChart),
    Tab(Routes.PLAN, "Planla", Icons.Rounded.TaskAlt)
)

@Composable
fun AppNav(vm: MainViewModel, pendingAction: MutableState<String?>) {
    val nav = rememberNavController()
    val data by vm.data.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in tabs.map { it.route }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            if (e is UiEvent.Message) {
                val r = snackbar.showSnackbar(e.text, actionLabel = if (e.undo != null) "Geri al" else null,
                    duration = SnackbarDuration.Short, withDismissAction = e.undo == null)
                if (r == SnackbarResult.ActionPerformed) e.undo?.invoke()
            }
        }
    }

    LaunchedEffect(pendingAction.value) {
        when (pendingAction.value) {
            com.burakmorinyo.gelirgider.MainActivity.ACTION_ADD_EXPENSE -> nav.navigate(Routes.tx(type = TxType.EXPENSE))
            com.burakmorinyo.gelirgider.MainActivity.ACTION_ADD_INCOME -> nav.navigate(Routes.tx(type = TxType.INCOME))
            com.burakmorinyo.gelirgider.MainActivity.ACTION_PLAN -> nav.navigateTab(Routes.PLAN)
        }
        pendingAction.value = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(showBar, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                BottomBar(route, onTab = { nav.navigateTab(it) }, onAdd = { nav.navigate(Routes.tx()) })
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(
            nav, startDestination = Routes.HOME,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } }
        ) {
            composable(Routes.HOME, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { HomeScreen(data, vm, nav) }
            composable(Routes.TRANSACTIONS, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                TransactionsScreen(data, vm, nav, accountFilter = 0, categoryFilter = 0, isTab = true)
            }
            composable(
                Routes.HISTORY,
                arguments = listOf(
                    navArgument("account") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("category") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { e ->
                TransactionsScreen(data, vm, nav, e.arguments!!.getLong("account"), e.arguments!!.getLong("category"), isTab = false)
            }
            composable(Routes.STATS, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { StatsScreen(data, nav) }
            composable(Routes.PLAN, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) { PlanScreen(data, vm, nav) }
            composable(
                Routes.TX,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("type") { type = NavType.StringType; defaultValue = TxType.EXPENSE.name }
                ),
                enterTransition = { slideInVertically { it / 3 } + fadeIn() },
                popExitTransition = { slideOutVertically { it / 3 } + fadeOut() }
            ) { e ->
                EditTransactionScreen(
                    data, vm, nav, e.arguments!!.getLong("id"),
                    TxType.valueOf(e.arguments!!.getString("type") ?: TxType.EXPENSE.name)
                )
            }
            composable(Routes.ACCOUNTS) { AccountsScreen(data, nav) }
            composable(Routes.ACCOUNT, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })) { e ->
                AccountEditScreen(data, vm, nav, e.arguments!!.getLong("id"))
            }
            composable(Routes.CATEGORIES) { CategoriesScreen(data, vm, nav) }
            composable(Routes.SETTINGS) { SettingsScreen(data, vm, nav) }
            composable(
                Routes.RECURRING,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("sub") { type = NavType.BoolType; defaultValue = false }
                )
            ) { e -> RecurringEditScreen(data, vm, nav, e.arguments!!.getLong("id"), e.arguments!!.getBoolean("sub")) }
            composable(Routes.GOAL, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })) { e ->
                GoalEditScreen(data, vm, nav, e.arguments!!.getLong("id"))
            }
            composable(Routes.DEBT, arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })) { e ->
                DebtEditScreen(data, vm, nav, e.arguments!!.getLong("id"))
            }
        }
    }
}

fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String?, onTab: (String) -> Unit, onAdd: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp, shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(70.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            tabs.take(2).forEach { TabItem(it, current == it.route) { onTab(it.route) } }
            Box(
                Modifier.size(56.dp).shadow(10.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899))))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Add, "İşlem ekle", tint = Color.White, modifier = Modifier.size(30.dp)) }
            tabs.drop(2).forEach { TabItem(it, current == it.route) { onTab(it.route) } }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.clip(RoundedCornerShape(16.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) { Icon(tab.icon, tab.label, tint = color, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(2.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}
