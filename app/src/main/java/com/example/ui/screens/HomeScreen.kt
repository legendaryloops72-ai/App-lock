package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProtectedAppEntity
import com.example.ui.viewmodel.AppLockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppLockViewModel,
    onNavigateToIntruders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCloudBackup: () -> Unit,
    onNavigateToJunkCleaner: () -> Unit,
    onNavigateToDisguise: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val apps by viewModel.apps.collectAsState()
    val intruderLogs by viewModel.intruderLogs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Open (إفتح), 1: Locked (مقفل)

    val filteredApps = apps.filter { app ->
        val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        val matchesTab = if (selectedTab == 0) !app.isLocked else app.isLocked
        matchesSearch && matchesTab
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "App Lock",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToIntruders) {
                        Icon(Icons.Default.Security, contentDescription = "Intruders", tint = Color(0xFFFACC15))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B1E2B))
            )
        },
        bottomBar = {
            com.example.ui.components.AdBanner()
        },
        containerColor = Color(0xFF161922)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Top Quick Action Row (التنكر, النسخ الاحتياطي, موضوع, إزالة الملفات, الخزانة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionItem(icon = Icons.Default.PhoneAndroid, label = "التنكر", onClick = onNavigateToDisguise)
                QuickActionItem(icon = Icons.Default.Cloud, label = "السحاب", onClick = onNavigateToCloudBackup)
                QuickActionItem(icon = Icons.Default.Palette, label = "موضوع", onClick = onNavigateToTheme)
                QuickActionItem(icon = Icons.Default.CleaningServices, label = "المنظف", onClick = onNavigateToJunkCleaner)
                QuickActionItem(icon = Icons.Default.Lock, label = "الخزانة", onClick = onNavigateToVault)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Protection Banner
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
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "الحماية الذكية مفعلة", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "جدولة ذكية لحماية التطبيقات تلقائياً", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permissions Center Banner Card
            Card(
                onClick = onNavigateToPermissions,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "مركز الأذونات والصلاحيات المطلوبة", fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "اضغط هنا لمنح أذونات القفل والوصول", fontSize = 12.sp, color = Color(0xFF93C5FD))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("بحث عن تطبيق...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF2D3242),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: إفتح (Open/Unlocked) vs مقفل (Locked)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF232733), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                TabButton(
                    text = "إفتح",
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    text = "مقفل",
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Warning banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "قم بتمكين الحماية ضد إزالة التثبيت للوقاية من فشل القفل", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Apps List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val scale by animateFloatAsState(
                        targetValue = if (app.isLocked) 1.02f else 1.0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "scale"
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (app.isLocked) 1.0f else 0.8f,
                        animationSpec = tween(durationMillis = 200),
                        label = "alpha"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (app.isLocked) Color(0xFF2C3242) else Color(0xFF232733),
                        animationSpec = tween(durationMillis = 200),
                        label = "bgColor"
                    )
                    
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .alpha(alpha)
                            .clickable { viewModel.triggerAppLaunch(app) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                val context = LocalContext.current
                                
                                // حالة الاحتفاظ بالأيقونة بعد جلبها لتفادي إعادة التحميل (Caching)
                                var iconDrawable by remember(app.packageName) { mutableStateOf<Drawable?>(null) }
                                // حالة تحميل الأيقونة لعرض Skeleton Loading
                                var isIconLoading by remember(app.packageName) { mutableStateOf(true) }

                                // جلب الأيقونة بشكل غير متزامن (Asynchronous) في خلفية التطبيق لمنع التجميد
                                LaunchedEffect(app.packageName) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            iconDrawable = context.packageManager.getApplicationIcon(app.packageName)
                                        } catch (e: Exception) {
                                            // في حال عدم العثور على التطبيق، نعتمد على الأيقونة الاحتياطية
                                            iconDrawable = null
                                        } finally {
                                            isIconLoading = false
                                        }
                                    }
                                }

                                // خريطة أيقونات احتياطية (Fallback Map) لأشهر التطبيقات بألوانها المميزة
                                val (fallbackIcon, iconBgColor, iconTint) = when (app.packageName) {
                                    "com.whatsapp" -> Triple(Icons.Default.Chat, Color(0xFF25D366).copy(alpha = 0.2f), Color(0xFF25D366))
                                    "com.instagram.android" -> Triple(Icons.Default.CameraAlt, Color(0xFFE1306C).copy(alpha = 0.2f), Color(0xFFE1306C))
                                    "com.google.android.youtube" -> Triple(Icons.Default.PlayArrow, Color(0xFFFF0000).copy(alpha = 0.2f), Color(0xFFFF0000))
                                    "com.android.settings" -> Triple(Icons.Default.Settings, Color(0xFF6B7280).copy(alpha = 0.2f), Color(0xFF9CA3AF))
                                    "com.google.android.gm" -> Triple(Icons.Default.Email, Color(0xFFEA4335).copy(alpha = 0.2f), Color(0xFFEA4335))
                                    "com.google.android.apps.photos" -> Triple(Icons.Default.Image, Color(0xFF4285F4).copy(alpha = 0.2f), Color(0xFF4285F4))
                                    "com.sec.android.gallery3d" -> Triple(Icons.Default.PhotoLibrary, Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF10B981))
                                    "com.bankofamerica.android" -> Triple(Icons.Default.AccountBalance, Color(0xFF0066B2).copy(alpha = 0.2f), Color(0xFF38BDF8))
                                    "com.facebook.katana" -> Triple(Icons.Default.Public, Color(0xFF1877F2).copy(alpha = 0.2f), Color(0xFF1877F2))
                                    "com.twitter.android" -> Triple(Icons.Default.Share, Color(0xFF1DA1F2).copy(alpha = 0.2f), Color(0xFF1DA1F2))
                                    "com.google.android.apps.messaging" -> Triple(Icons.Default.Message, Color(0xFF34A853).copy(alpha = 0.2f), Color(0xFF34A853))
                                    "com.spotify.music" -> Triple(Icons.Default.MusicNote, Color(0xFF1DB954).copy(alpha = 0.2f), Color(0xFF1DB954))
                                    else -> Triple(Icons.Default.Apps, Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF3B82F6))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isIconLoading) Color.DarkGray else if (iconDrawable != null) Color.Transparent else iconBgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!isIconLoading) {
                                        if (iconDrawable != null) {
                                            androidx.compose.foundation.Image(
                                                painter = coil.compose.rememberAsyncImagePainter(model = iconDrawable),
                                                contentDescription = app.appName,
                                                modifier = Modifier.size(48.dp).clip(CircleShape)
                                            )
                                        } else {
                                            Icon(fallbackIcon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = app.appName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    Text(text = app.packageName, fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f), maxLines = 1)
                                }
                            }

                            Switch(
                                checked = app.isLocked,
                                onCheckedChange = { viewModel.toggleAppLock(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF232733)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
fun TabButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF3B82F6) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}
