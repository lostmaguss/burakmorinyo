package com.burakmorinyo.gelirgider

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.burakmorinyo.gelirgider.ui.AppNav
import com.burakmorinyo.gelirgider.ui.LockScreen
import com.burakmorinyo.gelirgider.ui.MainViewModel
import com.burakmorinyo.gelirgider.ui.OnboardingScreen
import com.burakmorinyo.gelirgider.ui.theme.GelirGiderTheme

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_ACTION = "action"
        const val ACTION_ADD_EXPENSE = "com.burakmorinyo.gelirgider.ADD_EXPENSE"
        const val ACTION_ADD_INCOME = "com.burakmorinyo.gelirgider.ADD_INCOME"
        const val ACTION_PLAN = "com.burakmorinyo.gelirgider.PLAN"
        /** Arka planda bu kadar kalınca kilit yeniden istenir (dosya seçici vb. için tolerans) */
        private const val RELOCK_AFTER_MS = 60_000L
    }

    private val pendingAction = mutableStateOf<String?>(null)
    private var unlocked by mutableStateOf(false)
    private var backgroundedAt = 0L

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as GelirGiderApp
        val vm = ViewModelProvider(this, viewModelFactory { initializer { MainViewModel(app.repository) } })[MainViewModel::class.java]
        unlocked = savedInstanceState?.getBoolean("unlocked") ?: false
        handleIntent(intent)

        setContent {
            val data by vm.data.collectAsStateWithLifecycle()
            val s = data.settings
            GelirGiderTheme(themeMode = s.themeMode, dynamicColor = s.dynamicColor) {
                when {
                    !data.loaded -> LockScreen(showButton = false, onUnlock = {})
                    !s.onboarded -> OnboardingScreen { startDay ->
                        vm.updateSettings { it.copy(onboarded = true, periodStartDay = startDay) }
                        askNotificationPermission()
                    }
                    s.appLock && !unlocked -> {
                        androidx.compose.runtime.LaunchedEffect(Unit) { authenticate() }
                        LockScreen(showButton = true, onUnlock = { authenticate() })
                    }
                    else -> AppNav(vm, pendingAction)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.getStringExtra(EXTRA_ACTION) ?: intent?.action?.takeIf { it.startsWith("com.burakmorinyo") }
        if (action != null) pendingAction.value = action
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("unlocked", unlocked)
    }

    override fun onStop() {
        super.onStop()
        backgroundedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt != 0L && SystemClock.elapsedRealtime() - backgroundedAt > RELOCK_AFTER_MS) unlocked = false
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun authenticate() {
        val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            // Cihazda ekran kilidi yoksa uygulamayı kilitli bırakmanın anlamı yok
            unlocked = true
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlocked = true }
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Gelir Gider kilidi")
                .setSubtitle("Devam etmek için kimliğini doğrula")
                .setAllowedAuthenticators(authenticators)
                .build()
        )
    }
}
