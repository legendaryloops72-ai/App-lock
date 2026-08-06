package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppLockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunkCleanerScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var vidmateChecked by remember { mutableStateOf(true) }
    var snaptubeChecked by remember { mutableStateOf(true) }
    var historyChecked by remember { mutableStateOf(true) }
    var tempChecked by remember { mutableStateOf(true) }
    var systemChecked by remember { mutableStateOf(true) }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var cleanedSuccessfully by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إزالة الملفات غير الهامة", fontWeight = FontWeight.Bold, color = Color.White) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "GB ٢,٤٠",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "تم فحص ملفات مهملة! لا تقلق، تظل بياناتك الخاصة بأمان.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Category 1: Remaining junk
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("مهملات متبقية", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("GB ١,٩٧", color = Color(0xFF9CA3AF))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("VidMate (ملفات مؤقتة للفيديو)", color = Color.White)
                                Checkbox(checked = vidmateChecked, onCheckedChange = { vidmateChecked = it })
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Snap tube (ذاكرة التخزين المؤقت)", color = Color.White)
                                Checkbox(checked = snaptubeChecked, onCheckedChange = { snaptubeChecked = it })
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("بقايا السجل والعمليات المعلقة", color = Color.White)
                                Checkbox(checked = historyChecked, onCheckedChange = { historyChecked = it })
                            }
                        }
                    }
                }

                // Category 2: Temp files
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الملفات المؤقتة", fontWeight = FontWeight.Bold, color = Color.White)
                                Checkbox(checked = tempChecked, onCheckedChange = { tempChecked = it })
                            }
                        }
                    }
                }

                // Category 3: System files
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ملفات النظام والذاكرة العشوائية", fontWeight = FontWeight.Bold, color = Color.White)
                                Checkbox(checked = systemChecked, onCheckedChange = { systemChecked = it })
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("/storage/emulated/0/Android/data/nardo.Ai", fontSize = 12.sp, color = Color.Gray)
                            Text("/storage/emulated/0/Documents/كتب رسمية.pdf", fontSize = 12.sp, color = Color.Gray)
                            Text("/storage/emulated/0/Movies/Subtitles", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Bottom action button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF161922))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { showPermissionDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "إزالة (GB ٢,٠٠)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B)) },
            title = { Text("مطلوب إذن الوصول إلى كل الملفات", fontWeight = FontWeight.Bold) },
            text = { Text("قفز التطبيقات يتطلب إذن الوصول إلى جميع الملفات للعثور على جميع الملفات المهملة. يرجى منح الإذن في الإعدادات لتمكين التنظيف الحقيقي.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                        } catch (e2: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                viewModel.ignoreNextSelfLock(); context.startActivity(intent)
                            } catch (e3: Exception) {}
                        }
                    }
                }) {
                    Text("منح")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
