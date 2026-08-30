package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.ui.screens.CloudBackupScreen
import com.example.ui.screens.DisguiseScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntruderLogsScreen
import com.example.ui.screens.JunkCleanerScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmartLaunchScreen
import com.example.ui.screens.ThemeCustomizationScreen
import com.example.ui.screens.TroubleshootingScreen
import com.example.ui.screens.TutorialsScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.screens.AppSelfLockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppLockViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : androidx.fragment.app.FragmentActivity() {
  private val viewModel: AppLockViewModel by viewModels()
  private var backgroundTime: Long = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    try {
      com.example.service.AdManager.initialize(this) { status ->
        android.util.Log.d("MainActivity", "MobileAds initialized with status: $status")
        com.example.service.AdManager.loadInterstitialAd(this)
      }
    } catch (e: Exception) {}
    
    try {
      startService(android.content.Intent(this, com.example.service.AppLockUsageService::class.java))
    } catch (e: Exception) {}

    handleIntent(intent)

    setContent {
      val settings by viewModel.settings.collectAsState()
      val darkTheme = settings?.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme()
      val isSelfLocked by viewModel.isSelfLocked.collectAsState()

      MyApplicationTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val interceptedApp by viewModel.interceptedAppName.collectAsState()
        
        val sharedPrefs = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val isOnboardingCompleted = sharedPrefs.getBoolean("onboarding_done", false)
        val startDest = if (isOnboardingCompleted) "home" else "onboarding"

        val pendingNavigation by viewModel.pendingNavigation.collectAsState()
        LaunchedEffect(pendingNavigation) {
            if (pendingNavigation != null) {
                try {
                    navController.navigate(pendingNavigation!!)
                } catch (e: Exception) {}
                viewModel.clearPendingNavigation()
            }
        }

        LaunchedEffect(settings) {
            if (settings?.isOnboardingCompleted == true) {
                sharedPrefs.edit().putBoolean("onboarding_done", true).apply()
                if (navController.currentBackStackEntry?.destination?.route == "onboarding") {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            }
        }

        if (settings == null) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
          ) {
            androidx.compose.foundation.layout.Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
              androidx.compose.material3.Text(
                text = "قفل التطبيقات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
              )
              androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
              androidx.compose.material3.Text(
                text = "جاري تحميل الإعدادات...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
              )
              androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(48.dp))
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
              )
            }
          }
        } else {
          Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = startDest) {
              composable("onboarding", enterTransition = { androidx.compose.animation.EnterTransition.None }, exitTransition = { androidx.compose.animation.ExitTransition.None }) {
                OnboardingScreen(
                  viewModel = viewModel,
                  onFinished = {
                    sharedPrefs.edit().putBoolean("onboarding_done", true).apply()
                    navController.navigate("home") {
                      popUpTo("onboarding") { inclusive = true }
                    }
                  }
                )
              }
              composable("home", enterTransition = { androidx.compose.animation.EnterTransition.None }, exitTransition = { androidx.compose.animation.ExitTransition.None }) {
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
              composable("intruders") {
                IntruderLogsScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("settings") {
                SettingsScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() },
                  onNavigateToPermissions = { navController.navigate("permissions") },
                  onNavigateToTheme = { navController.navigate("theme") },
                  onNavigateToCloudBackup = { navController.navigate("cloud_backup") },
                  onNavigateToJunkCleaner = { navController.navigate("junk_cleaner") },
                  onNavigateToDisguise = { navController.navigate("disguise") },
                  onNavigateToSmartLaunch = { navController.navigate("smart_launch") },
                  onNavigateToTutorials = { navController.navigate("tutorials") },
                  onNavigateToVault = { navController.navigate("vault") },
                  onNavigateToTroubleshooting = { navController.navigate("troubleshooting") }
                )
              }
              composable("permissions") {
                PermissionsScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("theme") {
                ThemeCustomizationScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("cloud_backup") {
                CloudBackupScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("junk_cleaner") {
                JunkCleanerScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("disguise") {
                DisguiseScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("smart_launch") {
                SmartLaunchScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("tutorials") {
                TutorialsScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("vault") {
                VaultScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
              composable("troubleshooting") {
                TroubleshootingScreen(
                  viewModel = viewModel,
                  onBack = { navController.popBackStack() }
                )
              }
            }

            // If an app is intercepted (simulated launch), show Lock Screen overlay with smooth animation
            AnimatedVisibility(
                visible = interceptedApp != null,
                enter = fadeIn(animationSpec = tween(350)) + slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(350)),
                exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(targetOffsetY = { it / 3 }, animationSpec = tween(250))
            ) {
                if (interceptedApp != null) {
                    LockScreen(
                        appName = interceptedApp!!,
                        viewModel = viewModel
                    )
                }
            }
            
            // Self Auth overlay for AppLock itself
            if (isSelfLocked && isOnboardingCompleted) {
              AppSelfLockScreen(
                settings = settings!!,
                onUnlock = { viewModel.unlockSelf() }
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
    val settings = viewModel.settings.value
    val sharedPrefs = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
    val isOnboardingCompleted = sharedPrefs.getBoolean("onboarding_done", false)
    
    if (settings != null && isOnboardingCompleted) {
        val timeoutMs = when (settings.lockTimeout) {
            "IMMEDIATELY" -> 0L
            "1_MIN" -> 60_000L
            "5_MIN" -> 300_000L
            else -> 0L // Default to immediately if unknown
        }
        viewModel.checkAndRequireSelfAuth(timeoutMs, backgroundTime)
    }
    viewModel.checkAndNotifyUnseenIntruders(this)
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    val navTo = intent?.getStringExtra("navigate_to")
    if (navTo != null) {
      viewModel.setPendingNavigation(navTo)
    }
    val pkg = intent?.getStringExtra("INTERCEPT_PACKAGE")
    val name = intent?.getStringExtra("INTERCEPT_NAME")
    if (pkg != null && name != null) {
      viewModel.triggerIntercept(pkg, name)
    }
  }
}
