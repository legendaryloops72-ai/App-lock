package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SecuritySettingsEntity
import com.example.ui.viewmodel.AppLockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToCloudBackup: () -> Unit,
    onNavigateToJunkCleaner: () -> Unit,
    onNavigateToDisguise: () -> Unit,
    onNavigateToSmartLaunch: () -> Unit,
    onNavigateToTutorials: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToTroubleshooting: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentSettings = settings ?: SecuritySettingsEntity()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showSmartLockDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings_title), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B1E2B))
            )
        },
        containerColor = Color(0xFF161922)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permissions Center Card
            item {
                Card(
                    onClick = onNavigateToPermissions,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_permissions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_permissions_desc), style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Troubleshooting Card
            item {
                Card(
                    onClick = onNavigateToTroubleshooting,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFF97316))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_troubleshooting_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_troubleshooting_desc), style = MaterialTheme.typography.bodySmall, color = Color(0xFF3B82F6))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Tutorials Card
            item {
                Card(
                    onClick = onNavigateToTutorials,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFF97316))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_tutorials_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Password & Security Group Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(id = R.string.settings_security_title), fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Fingerprint toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFFFACC15))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = stringResource(id = R.string.settings_biometric_title), fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFEF4444),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(text = stringResource(id = R.string.settings_badge_hot), color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Switch(
                                checked = currentSettings.isBiometricEnabled,
                                onCheckedChange = {
                                    viewModel.updateSettings(currentSettings.copy(isBiometricEnabled = it))
                                }
                            )
                        }

                        Divider(color = Color(0xFF2D3242))

                        // Uninstall Protection toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = stringResource(id = R.string.settings_uninstall_protection_title), fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFF10B981),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(text = stringResource(id = R.string.settings_badge_secure), color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = stringResource(id = R.string.settings_uninstall_protection_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = currentSettings.uninstallProtectionEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateSettings(currentSettings.copy(uninstallProtectionEnabled = isChecked))
                                    if (isChecked) {
                                        onNavigateToPermissions()
                                    }
                                }
                            )
                        }

                        Divider(color = Color(0xFF2D3242))

                        // Change PIN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLockTimeoutDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFACC15))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(id = R.string.settings_timeout_title), fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = when (currentSettings.lockTimeout) {
                                    "IMMEDIATELY" -> stringResource(id = R.string.settings_timeout_immediately)
                                    "1_MIN" -> stringResource(id = R.string.settings_timeout_1_minute)
                                    "5_MIN" -> stringResource(id = R.string.settings_timeout_5_minutes)
                                    else -> stringResource(id = R.string.settings_timeout_immediately)
                                }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showChangePinDialog = true }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFFFACC15))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(id = R.string.settings_change_pin_title), fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        }

                        Divider(color = Color(0xFF2D3242))

                        // Lock Type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFACC15))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(id = R.string.settings_lock_type_title), fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                val newType = if (currentSettings.lockType == "PIN") "PATTERN" else "PIN"
                                viewModel.updateSettings(currentSettings.copy(lockType = newType))
                            }) {
                                Text(if (currentSettings.lockType == "PIN") stringResource(id = R.string.settings_lock_type_pin) else stringResource(id = R.string.settings_lock_type_pattern), color = Color(0xFF3B82F6))
                            }
                        }

                        Divider(color = Color(0xFF2D3242))

                        // Recovery method
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFACC15))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(id = R.string.settings_recovery_method_title), fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Smart Lock Card
            item {
                Card(
                    onClick = { showSmartLockDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_smart_lock_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_smart_lock_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Language Card
            item {
                Card(
                    onClick = { showLanguageDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.language_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.language_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Advanced Card
            item {
                Card(
                    onClick = onNavigateToSmartLaunch,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_advanced_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_advanced_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Notes Card
            item {
                Card(
                    onClick = onNavigateToVault,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_notes_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_notes_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Other Card
            item {
                Card(
                    onClick = onNavigateToTheme,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_other_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_other_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Privacy Policy Card
            item {
                Card(
                    onClick = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://kidspolicy-bcd2mive.manus.space/")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                android.widget.Toast.makeText(context, "لا يمكن فتح رابط سياسة الخصوصية", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (ex: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_privacy_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_privacy_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Ad Inspector Card
            item {
                Card(
                    onClick = {
                        com.example.service.AdManager.openAdInspector(context) { error ->
                            if (error != null) {
                                android.widget.Toast.makeText(
                                    context,
                                    "فشل فتح Ad Inspector: $error",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_ad_inspector_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = stringResource(id = R.string.settings_ad_inspector_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Version info footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Version 1.7.1", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text(stringResource(id = R.string.settings_change_pin_title)) },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text(stringResource(id = R.string.settings_new_pin_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length == 4) {
                        viewModel.updateSettings(currentSettings.copy(pin = newPin))
                        showChangePinDialog = false
                    }
                }) {
                    Text(stringResource(id = R.string.settings_btn_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text(stringResource(id = R.string.settings_btn_cancel))
                }
            }
        )
    }

    if (showSmartLockDialog) {
        AlertDialog(
            onDismissRequest = { showSmartLockDialog = false },
            title = { Text(stringResource(id = R.string.settings_smart_lock_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(id = R.string.settings_smart_lock_dialog_desc))
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSmartLockDialog = false
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e2: Exception) {}
                    }
                }) {
                    Text(stringResource(id = R.string.settings_system_security))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartLockDialog = false }) {
                    Text(stringResource(id = R.string.dialog_close))
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(id = R.string.language_choose)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showLanguageDialog = false
                            try {
                                val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags("ar")
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                                (context as? android.app.Activity)?.recreate()
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.lang_ar))
                    }
                    Button(
                        onClick = {
                            showLanguageDialog = false
                            try {
                                val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags("en")
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                                (context as? android.app.Activity)?.recreate()
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.lang_en))
                    }
                    Button(
                        onClick = {
                            showLanguageDialog = false
                            try {
                                val appLocale = androidx.core.os.LocaleListCompat.forLanguageTags("es")
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                                (context as? android.app.Activity)?.recreate()
                            } catch (e: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.lang_es))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(id = R.string.dialog_close))
                }
            }
        )
    }
    
    if (showLockTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showLockTimeoutDialog = false },
            title = { Text(stringResource(id = R.string.settings_timeout_title)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.updateSettings(currentSettings.copy(lockTimeout = "IMMEDIATELY"))
                            showLockTimeoutDialog = false
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSettings.lockTimeout == "IMMEDIATELY", onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.settings_timeout_immediately))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.updateSettings(currentSettings.copy(lockTimeout = "1_MIN"))
                            showLockTimeoutDialog = false
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSettings.lockTimeout == "1_MIN", onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.settings_timeout_1_minute))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.updateSettings(currentSettings.copy(lockTimeout = "5_MIN"))
                            showLockTimeoutDialog = false
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSettings.lockTimeout == "5_MIN", onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.settings_timeout_5_minutes))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLockTimeoutDialog = false }) { Text(stringResource(id = R.string.dialog_close)) }
            }
        )
    }
}
