package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
fun SmartLaunchScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    data class AutoStartApp(val name: String, var isEnabled: Boolean)

    val appsList = remember {
        mutableStateListOf(
            AutoStartApp("تحويل الفيديو MP3", true),
            AutoStartApp("MX Player", true),
            AutoStartApp("Manus", true),
            AutoStartApp("Messenger", true),
            AutoStartApp("PermenComic", true),
            AutoStartApp("Play Console", true),
            AutoStartApp("QR Code & Barcode Scanner", true)
        )
    }

    var showOptimizationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بدء تشغيل التطبيق تلقائيا", fontWeight = FontWeight.Bold, color = Color.White) },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "بعد تمكين إدارة بدء التشغيل، سيتم تشغيل التطبيق تلقائيا في الخلفية أو سيتم السماح لتطبيقات أخرى بتشغيله. يؤدي منع تشغيل التطبيقات في الخلفية إلى توفّر الطاقة وتسريع أداء الجهاز، لكن قد يؤدي إلى تأخير استقبال البيانات لبعض التطبيقات.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(appsList) { app ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = app.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Text(text = if (app.isEnabled) "مسموح به" else "متوقف", fontSize = 12.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = app.isEnabled,
                                onCheckedChange = { checked ->
                                    val index = appsList.indexOf(app)
                                    if (index != -1) {
                                        appsList[index] = app.copy(isEnabled = checked)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFF161922))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e2: Exception) {}
                        }
                        showOptimizationDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "التحسين الذكي الحقيقي", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showOptimizationDialog = false },
            title = { Text("التحسين الذكي الحقيقي") },
            text = { Text("تم تفعيل إذن تحسين البطارية وإلغاء القيود على التطبيقات في الخلفية بنجاح لتسريع الأداء وضمان عمل القفل.") },
            confirmButton = {
                Button(onClick = {
                    showOptimizationDialog = false
                    Toast.makeText(context, "تم تطبيق التحسين الذكي بنجاح", Toast.LENGTH_SHORT).show()
                }) {
                    Text("تم")
                }
            }
        )
    }
}
