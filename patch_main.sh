#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/MainActivity.kt
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppLockViewModel

class MainActivity : androidx.fragment.app.FragmentActivity() {
  private val viewModel: AppLockViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    com.google.android.gms.ads.MobileAds.initialize(this) {}
    com.example.service.AdManager.loadInterstitialAd(this)
    com.example.service.AdManager.showInterstitialAd(this)
    
    try {
      startService(android.content.Intent(this, com.example.service.AppLockUsageService::class.java))
    } catch (e: Exception) {}

    setContent {
      val settings by viewModel.settings.collectAsState()
      val darkTheme = settings?.isDarkMode ?: androidx.compose.foundation.isSystemInDarkTheme()

      MyApplicationTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val interceptedApp by viewModel.interceptedAppName.collectAsState()
        
        val sharedPrefs = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
        val isOnboardingCompleted = sharedPrefs.getBoolean("onboarding_done", false)
        val startDest = if (isOnboardingCompleted) "home" else "onboarding"

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

        Box(modifier = Modifier.fillMaxSize()) {
          NavHost(navController = navController, startDestination = startDest) {
            composable("onboarding") {
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

          // If an app is intercepted (simulated launch), show Lock Screen overlay
          if (interceptedApp != null) {
            LockScreen(
              appName = interceptedApp!!,
              viewModel = viewModel
            )
          }
        }
      }
    }
  }
}
INNER_EOF
