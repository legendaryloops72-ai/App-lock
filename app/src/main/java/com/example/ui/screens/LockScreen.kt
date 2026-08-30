package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SecuritySettingsEntity
import com.example.ui.viewmodel.AppLockViewModel
import com.example.utils.BiometricAuthHelper
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun LockScreen(
    appName: String,
    viewModel: AppLockViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val authError by viewModel.authError.collectAsState()
    val unlockSuccess by viewModel.unlockSuccess.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var showSuccessCelebration by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val lockType = settings?.lockType ?: "PIN"

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val context = LocalContext.current
    val fragmentActivity = context as? androidx.fragment.app.FragmentActivity
    var biometricFailCount by remember { mutableStateOf(0) }

    LaunchedEffect(unlockSuccess) {
        if (unlockSuccess) {
            showSuccessCelebration = true
        }
    }

    val launchBiometric = {
        if (fragmentActivity != null && settings?.isBiometricEnabled == true && BiometricAuthHelper.canAuthenticate(context)) {
            BiometricAuthHelper.showBiometricPrompt(
                activity = fragmentActivity,
                title = "مصادقة بصمة التطبيق",
                subtitle = "حط بصمتك لفتح التطبيق ($appName)",
                negativeButtonText = "الرمز السري (PIN)",
                onSuccess = {
                    biometricFailCount = 0
                    viewModel.onBiometricSuccess()
                },
                onError = { err ->
                    viewModel.setAuthError(err)
                },
                onFailed = {
                    biometricFailCount++
                    val details = "محاولة بصمة غير متطابقة - محاولة $biometricFailCount"
                    com.example.service.IntruderDetectionService.recordFailedAttempt(
                        context = context,
                        appName = appName,
                        details = details,
                        capturePhoto = true
                    )
                    if (biometricFailCount >= 3) {
                        viewModel.setAuthError("تجاوزت عدد المحاولات، استخدم الرمز السري")
                    } else {
                        viewModel.setAuthError("البصمة ما تطابقت، جرب مرة ثانية")
                    }
                }
            )
        } else {
            viewModel.setAuthError("البصمة غير متوفرة أو غير مفعلة في الجهاز")
        }
    }

    LaunchedEffect(Unit) {
        if (settings?.isBiometricEnabled == true) {
            launchBiometric()
        }
    }

    val wallpaperType = settings?.wallpaperType ?: "DEFAULT"
    val wallpaperValue = settings?.wallpaperValue ?: ""

    if (showSuccessCelebration) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1600)
            viewModel.unlockSuccessful()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFF0F172A).copy(alpha = 0.98f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(model = com.example.R.drawable.img_app_shield_logo_1785331232602),
                    contentDescription = "Success Shield Logo",
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "تم فك القفل بنجاح والانتصار! 🛡️✨",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "تم التحقق من هويتك بنجاح وفتح تطبيق $appName.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.ui.graphics.Color(0xFF93C5FD),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                when (wallpaperType) {
                    "GRADIENT" -> {
                        val colorStrs = wallpaperValue.split(",")
                        val colors = if (colorStrs.size >= 2) {
                            try {
                                listOf(androidx.compose.ui.graphics.Color(colorStrs[0].trim().toLong()), androidx.compose.ui.graphics.Color(colorStrs[1].trim().toLong()))
                            } catch (e: Exception) {
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            }
                        } else {
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        }
                        Modifier.background(androidx.compose.ui.graphics.Brush.linearGradient(colors))
                    }
                    else -> Modifier.background(MaterialTheme.colorScheme.background)
                }
            )
    ) {
        if (wallpaperType == "CUSTOM" && wallpaperValue.isNotBlank()) {
            coil.compose.AsyncImage(
                model = java.io.File(wallpaperValue),
                contentDescription = "Custom Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400)) { it / 4 },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "App is locked. Enter PIN or Pattern to open.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authError!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Authentication Input (PIN or Pattern)
            if (lockType == "PIN") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    // PIN Dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        for (i in 0..3) {
                            val filled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }

                    // Numeric Keypad
                    val showBiometricButton = settings?.isBiometricEnabled == true && BiometricAuthHelper.canAuthenticate(context)
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf(if (showBiometricButton) "Bio" else "", "0", "Del")
                    )

                    rows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            row.forEach { key ->
                                KeypadButton(
                                    key = key,
                                    onClick = {
                                        when (key) {
                                            "Del" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                            "Bio" -> {
                                                launchBiometric()
                                            }
                                            "" -> {}
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += key
                                                    if (enteredPin.length == 4) {
                                                        settings?.let { s ->
                                                            viewModel.verifyPin(enteredPin, s, appName)
                                                        }
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Pattern Lock View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    PatternLockView(
                        onPatternComplete = { pattern ->
                            settings?.let { s ->
                                viewModel.verifyPattern(pattern, s, appName)
                            }
                        }
                    )
                }
            }

            // Cancel / Dismiss button
            Button(
                onClick = { viewModel.dismissLockScreen() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(48.dp)
            ) {
                Text("Cancel / Go Back", fontWeight = FontWeight.SemiBold)
            }
        }
      }
    }
}

@Composable
fun KeypadButton(
    key: String,
    onClick: () -> Unit
) {
    if (key.isEmpty()) {
        Spacer(modifier = Modifier.size(68.dp))
        return
    }
    val isSpecial = key == "Del" || key == "Bio"
    val bgColor = if (isSpecial) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val contentColor = if (isSpecial) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.size(68.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (key) {
                "Del" -> Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = contentColor)
                "Bio" -> Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = contentColor)
                else -> Text(
                    text = key,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}
