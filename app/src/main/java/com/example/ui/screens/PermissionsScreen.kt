package com.example.ui.screens

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AppLockViewModel

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = android.content.ComponentName(context, com.example.service.AppLockAccessibilityService::class.java)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Check permissions dynamically
    val overlayGranted = remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val usageGranted = remember {
        mutableStateOf(
            try {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
                mode == AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                false
            }
        )
    }
    val accessibilityGranted = remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    val cameraGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraGranted.value = isGranted
    }

    // Refresh permissions on resume when returning from settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                overlayGranted.value = Settings.canDrawOverlays(context)
                usageGranted.value = try {
                    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                    val mode = appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(),
                        context.packageName
                    )
                    mode == AppOpsManager.MODE_ALLOWED
                } catch (e: Exception) {
                    false
                }
                accessibilityGranted.value = isAccessibilityServiceEnabled(context)
                cameraGranted.value = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مركز الأذونات والصلاحيات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "أذونات القفل والتشغيل الحقيقي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "يتطلب تطبيق قفل التطبيقات هذه الصلاحيات لكي يعمل بشكل مثالي في الخلفية واعتراض فتح التطبيقات المقفلة فوراً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                PermissionCardItem(
                    title = "خدمة إمكانية الوصول (Accessibility Service)",
                    description = "ضرورية للغاية لحماية خصوصيتك ومنع الحذف غير المصرح به؛ تتيح للتطبيق رصد محاولات الدخول لصفحة إعدادات (AppLock) لمنع المتطفلين من إلغاء تثبيت التطبيق أو إيقافه إجبارياً، بالإضافة لرصد تشغيل التطبيقات المقفلة بالوقت الفعلي وعرض شاشة القفل فوراً.",
                    isGranted = accessibilityGranted.value,
                    grantButtonText = if (accessibilityGranted.value) "تم منح الإذن بنجاح" else "منح إذن الوصول",
                    onGrantClick = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                        }
                    }
                )
            }

            item {
                PermissionCardItem(
                    title = "إذن الوصول إلى الاستخدام (Usage Access)",
                    description = "ضروري لمعرفة التطبيق الذي يعمل حالياً في المقدمة لضمان القفل الدقيق.",
                    isGranted = usageGranted.value,
                    grantButtonText = if (usageGranted.value) "إعدادات الإذن" else "منح إذن الاستخدام",
                    onGrantClick = {
                        try {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                        }
                    }
                )
            }

            item {
                PermissionCardItem(
                    title = "العرض فوق التطبيقات الأخرى (Display Over Other Apps)",
                    description = "مطلوب لإظهار شاشة رمز المرور أو النمط فوق التطبيقات الأخرى عند فتحها.",
                    isGranted = overlayGranted.value,
                    grantButtonText = if (overlayGranted.value) "إعدادات الإذن" else "منح إذن العرض",
                    onGrantClick = {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
                            viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                        }
                    }
                )
            }

            item {
                PermissionCardItem(
                    title = "صلاحية الكاميرا (Camera Permission)",
                    description = "ضرورية لالتقاط صورة سيلفي للمتطفل (Intruder Selfie) تلقائياً عند كتابة رمز فتح خاطئ لثلاث مرات متتالية.",
                    isGranted = cameraGranted.value,
                    grantButtonText = if (cameraGranted.value) "تم منح الإذن بنجاح" else "منح إذن الكاميرا",
                    onGrantClick = {
                        if (!cameraGranted.value) {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionCardItem(
    title: String,
    description: String,
    isGranted: Boolean,
    grantButtonText: String,
    onGrantClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (isGranted) "مفعل" else "مطلوب",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onGrantClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(grantButtonText)
            }
        }
    }
}

