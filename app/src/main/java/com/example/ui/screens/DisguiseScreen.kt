package com.example.ui.screens

import android.content.ComponentName
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppLockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisguiseScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hidePatternPath by remember { mutableStateOf(false) }
    var statusBarNotification by remember { mutableStateOf(true) }
    var crashScreenDisguise by remember { mutableStateOf(true) }
    var selectedIcon by remember { mutableStateOf("افتراضي") }

    var showIconPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التنكر", fontWeight = FontWeight.Bold, color = Color.White) },
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
            // Change Icon Card
            item {
                Card(
                    onClick = { showIconPicker = true },
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
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "تغيير أيقونة التطبيق", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = selectedIcon, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }

            // Crash Screen Disguise Card
            item {
                Card(
                    onClick = { crashScreenDisguise = !crashScreenDisguise },
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
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "تنكر شاشة التعطل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "اجعل التطبيقات المقفلة تبدو وكأنها تعطلت", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = crashScreenDisguise, onCheckedChange = { crashScreenDisguise = it })
                    }
                }
            }

            // Hide pattern path
            item {
                Card(
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
                            Text(text = "إخفاء مسار رسم النمط", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Switch(checked = hidePatternPath, onCheckedChange = { hidePatternPath = it })
                    }
                }
            }

            // Status bar notifications
            item {
                Card(
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
                            Text(text = "حالة شريط الإشعارات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Switch(checked = statusBarNotification, onCheckedChange = { statusBarNotification = it })
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            title = { Text("اختر أيقونة التمويه") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "افتراضي" to ".MainActivityAliasDefault",
                        "الحاسبة" to ".MainActivityAliasCalculator",
                        "المتصفح" to ".MainActivityAliasBrowser",
                        "الطقس" to ".MainActivityAliasWeather"
                    ).forEach { (iconName, aliasSuffix) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val pm = context.packageManager
                                        val pkg = context.packageName
                                        val aliases = listOf(
                                            ".MainActivityAliasDefault",
                                            ".MainActivityAliasCalculator",
                                            ".MainActivityAliasBrowser",
                                            ".MainActivityAliasWeather"
                                        )
                                        aliases.forEach { alias ->
                                            val comp = ComponentName(pkg, "$pkg$alias")
                                            val state = if (alias == aliasSuffix) {
                                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                            } else {
                                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                            }
                                            pm.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
                                        }
                                        selectedIcon = iconName
                                        Toast.makeText(context, "تم تغيير الأيقونة إلى: $iconName بنجاح", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "فشل تغيير الأيقونة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                    showIconPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (selectedIcon == iconName), onClick = {
                                try {
                                    val pm = context.packageManager
                                    val pkg = context.packageName
                                    val aliases = listOf(
                                        ".MainActivityAliasDefault",
                                        ".MainActivityAliasCalculator",
                                        ".MainActivityAliasBrowser",
                                        ".MainActivityAliasWeather"
                                    )
                                    aliases.forEach { alias ->
                                        val comp = ComponentName(pkg, "$pkg$alias")
                                        val state = if (alias == aliasSuffix) {
                                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                        } else {
                                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                                        }
                                        pm.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
                                    }
                                    selectedIcon = iconName
                                    Toast.makeText(context, "تم تغيير الأيقونة إلى: $iconName بنجاح", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                showIconPicker = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = iconName, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
