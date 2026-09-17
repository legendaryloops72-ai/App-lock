package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppLockViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : androidx.fragment.app.FragmentActivity() {
  private val viewModel: AppLockViewModel by viewModels()
  private var backgroundTime: Long = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    // In Release builds, prevent screenshots, screen recording, and task switcher previews.
    if (!BuildConfig.DEBUG) {
      window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    enableEdgeToEdge()

    try {
      com.example.service.AdManager.initialize(this) { status ->
        android.util.Log.d("MainActivity", "MobileAds initialized with status: $status")
        com.example.service.AdManager.loadInterstitialAd(this)
      }
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "AdManager initialization error", e)
    }

    try {
      startService(android.content.Intent(this, com.example.service.AppLockUsageService::class.java))
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Failed to start AppLockUsageService", e)
    }

    handleIntent(intent)

    setContent {
      val settings by viewModel.settings.collectAsState()
      val darkTheme = settings?.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme()

      MyApplicationTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val interceptedApp by viewModel.interceptedAppName.collectAsState()

        val sharedPrefs = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        // App Lock itself opens directly. First-run setup is handled by the PIN dialog below.
        val startDest = "home"
        var showFirstRunPinSetup by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        LaunchedEffect(settings) {
          if (settings != null && settings!!.pin.isBlank() && settings!!.patternSequence.isBlank()) {
            showFirstRunPinSetup = true
          }
        }

        val pendingNavigation by viewModel.pendingNavigation.collectAsState()
        LaunchedEffect(pendingNavigation) {
          if (pendingNavigation != null) {
            try { navController.navigate(pendingNavigation!!) } catch (_: Exception) { }
            viewModel.clearPendingNavigation()
          }
        }

        if (settings == null) {
          Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.height(24.dp))
              Text("قفل التطبيقات", style = MaterialTheme.typography.headlineMedium)
              Spacer(modifier = Modifier.height(8.dp))
              Text("جاري تحميل الإعدادات...", style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.height(48.dp))
              CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            }
          }
        } else {
          Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = startDest) {
              composable("onboarding") { OnboardingScreen(viewModel) { navController.navigate("home") } }
              composable("home") {
                HomeScreen(
                  viewModel = viewModel,
                  onNavigateToIntruders = { navController.navigate("intruders") },
                  onNavigateToSettings = { navController.navigate("settings") },
                  onNavigateToCloudBackup = { navController.navigate("cloud_backup") },
                  onNavigateToJunkCleaner = { navController.navigate("junk_cleaner") },
                  onNavigateToDisguise = { navController.navigate("disguise") },
                  onNavigateToTheme = { navController.navigate("theme") },
                  onNavigateToVault = { navController.navigate("vault") },
                  onNavigateToPermissions = { navController.navigate("permissions") }
                )
              }
              composable("intruders") { IntruderLogsScreen(viewModel) { navController.popBackStack() } }
              composable("settings") {
                SettingsScreen(viewModel, { navController.popBackStack() }, { navController.navigate("permissions") }, { navController.navigate("theme") }, { navController.navigate("cloud_backup") }, { navController.navigate("junk_cleaner") }, { navController.navigate("disguise") }, { navController.navigate("smart_launch") }, { navController.navigate("tutorials") }, { navController.navigate("vault") }, { navController.navigate("troubleshooting") })
              }
              composable("permissions") { PermissionsScreen(viewModel) { navController.popBackStack() } }
              composable("theme") { ThemeCustomizationScreen(viewModel) { navController.popBackStack() } }
              composable("cloud_backup") { CloudBackupScreen(viewModel) { navController.popBackStack() } }
              composable("junk_cleaner") { JunkCleanerScreen(viewModel) { navController.popBackStack() } }
              composable("disguise") { DisguiseScreen(viewModel) { navController.popBackStack() } }
              composable("smart_launch") { SmartLaunchScreen(viewModel) { navController.popBackStack() } }
              composable("tutorials") { TutorialsScreen(viewModel) { navController.popBackStack() } }
              composable("vault") { VaultScreen(viewModel) { navController.popBackStack() } }
              composable("troubleshooting") { TroubleshootingScreen(viewModel) { navController.popBackStack() } }
            }

            AnimatedVisibility(
              visible = interceptedApp != null,
              enter = fadeIn(tween(350)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(350)),
              exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(250))
            ) {
              if (interceptedApp != null) LockScreen(appName = interceptedApp!!, viewModel = viewModel)
            }

            // Self Lock remains permanently disabled.
            val isSelfLocked by viewModel.isSelfLocked.collectAsState()
            if (isSelfLocked) {
              AppSelfLockScreen(settings = settings!!, onUnlock = { viewModel.unlockSelf() })
            }

            if (showFirstRunPinSetup && settings!!.pin.isBlank() && settings!!.patternSequence.isBlank()) {
              SetSecurityPinDialog(
                settings = settings!!,
                viewModel = viewModel,
                onSaved = { showFirstRunPinSetup = false }
              )
            }
          }
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    backgroundTime = System.currentTimeMillis()
  }

  override fun onResume() {
    super.onResume()
    // App Lock itself is never self-locked.
    viewModel.checkAndNotifyUnseenIntruders(this)
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    val navTo = intent?.getStringExtra("navigate_to")
    if (navTo != null) viewModel.setPendingNavigation(navTo)
    val pkg = intent?.getStringExtra("INTERCEPT_PACKAGE")
    val name = intent?.getStringExtra("INTERCEPT_NAME")
    if (pkg != null && name != null) viewModel.triggerIntercept(pkg, name)
  }
}
