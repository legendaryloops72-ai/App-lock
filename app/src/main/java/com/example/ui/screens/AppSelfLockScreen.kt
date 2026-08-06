package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.SecuritySettingsEntity
import kotlinx.coroutines.delay

@Composable
fun AppSelfLockScreen(
    settings: SecuritySettingsEntity,
    onUnlock: () -> Unit
) {
    val currentContext = LocalContext.current
    val context = remember(currentContext) {
        var ctx = currentContext
        while (ctx is android.content.ContextWrapper) {
            if (ctx is FragmentActivity) {
                break
            }
            ctx = ctx.baseContext
        }
        ctx as FragmentActivity
    }
    var showPinFallback by remember { mutableStateOf(false) }
    var biometricAttempts by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }

    val biometricManager = BiometricManager.from(context)
    val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)

    LaunchedEffect(Unit) {
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS || !settings.isBiometricEnabled) {
            showPinFallback = true
        }
    }

    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = remember {
        BiometricPrompt(context, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                errorMessage = errString.toString()
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    showPinFallback = true
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onUnlock()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                biometricAttempts++
                errorMessage = "فشلت المصادقة بالبصمة"
                if (biometricAttempts >= 3) {
                    showPinFallback = true
                    errorMessage = "فشل 3 مرات متتالية. تم تفعيل رمز PIN."
                }
            }
        })
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("فتح قفل التطبيقات")
        .setSubtitle("استخدم بصمتك أو الوجه لفتح AppLock")
        .setNegativeButtonText("استخدام رمز PIN")
        .build()

    fun authenticate() {
        if (!showPinFallback) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    // Trigger initial auth
    LaunchedEffect(showPinFallback) {
        if (!showPinFallback) {
            authenticate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
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
                    contentDescription = "App Logo",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            if (showPinFallback) {
                Text(
                    text = "أدخل رمز PIN",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // PIN Dots indicator (Like in LockScreen)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    for (i in 0..3) {
                        val filled = i < pinInput.length
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
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("Bio", "0", "Del")
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
                                            if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                        }
                                        "Bio" -> {
                                            if (settings.isBiometricEnabled && canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                                                showPinFallback = false
                                            }
                                        }
                                        else -> {
                                            if (pinInput.length < 4) pinInput += key
                                            if (pinInput.length == 4) {
                                                if (pinInput == settings.pin) {
                                                    onUnlock()
                                                } else {
                                                    errorMessage = "رمز PIN غير صحيح"
                                                    pinInput = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                
            } else {
                IconButton(
                    onClick = { authenticate() },
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Authenticate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "اضغط للمصادقة بالبصمة",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "استخدم بصمتك أو الوجه لفتح AppLock",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                LaunchedEffect(errorMessage) {
                    delay(3000)
                    errorMessage = null
                }
            }
        }
    }
}
