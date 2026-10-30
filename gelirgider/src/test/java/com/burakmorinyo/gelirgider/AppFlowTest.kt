package com.burakmorinyo.gelirgider

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.onRoot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Gerçek uygulama sınıfı ve MainActivity ile uçtan uca akış */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class AppFlowTest {

    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String) = compose.waitUntil(10_000) {
        compose.onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test fun onboardingThenAddExpense() {
        waitText("Başlayalım")
        compose.onNodeWithText("Başlayalım").performScrollTo().performClick()
        waitText("Toplam varlık")

        compose.onNodeWithContentDescription("İşlem ekle").performClick()
        waitText("Kategori")
        listOf("1", "2", "5").forEach { compose.onNodeWithText(it).performClick() }
        compose.onNodeWithText("Market").performClick()
        compose.onRoot().captureRoboImage("build/screenshots/20_flow_add.png")
        compose.onNodeWithText("Kaydet").performClick()

        waitText("Toplam varlık")
        val app = compose.activity.application as GelirGiderApp
        compose.waitUntil(10_000) { runBlocking { app.repository.allTransactions().isNotEmpty() } }
        val tx = runBlocking { app.repository.allTransactions() }.single()
        assertEquals(12_500L, tx.amount)
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("build/screenshots/21_flow_home.png")
    }
}
