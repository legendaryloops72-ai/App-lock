package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProtectedAppEntity
import com.example.ui.components.AdBanner
import com.example.ui.components.NativeAdCard
import com.example.ui.viewmodel.AppLockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * شاشة الواجهة الرئيسية المعاد تصميمها بالكامل
 * مع دعم كامل للغة العربية (RTL) وتصميم داكن أنيق يطابق أحدث معايير Material Design 3
 */
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
    val searchQuery by viewModel.searchQuery.collectAsState()

    var currentNavIndex by remember { mutableStateOf(0) } // 0: الرئيسية, 1: التطبيقات, 2: المميزات, 3: الإعدادات
    var selectedAppFilterTab by remember { mutableStateOf(0) } // 0: الكل, 1: مقفل, 2: مفتوح

    val filteredApps = remember(apps, searchQuery, selectedAppFilterTab) {
        apps.filter { app ->
            val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesTab = when (selectedAppFilterTab) {
                1 -> app.isLocked
                2 -> !app.isLocked
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    val lockedCount = remember(apps) { apps.count { it.isLocked } }
    val totalCount = remember(apps) { apps.size }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D111A))) {
                    AdBanner()
                    ModernBottomNavigationBar(
                        selectedIndex = currentNavIndex,
                        onItemSelected = { index ->
                            if (index == 3) {
                                onNavigateToSettings()
                            } else {
                                currentNavIndex = index
                            }
                        }
                    )
                }
            },
            containerColor = Color(0xFF0A0D14)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0E131F),
                                Color(0xFF0A0D14),
                                Color(0xFF07090E)
                            )
                        )
                    )
            ) {
                when (currentNavIndex) {
                    0 -> HomeDashboardView(
                        apps = filteredApps,
                        lockedCount = lockedCount,
                        totalCount = totalCount,
                        onOpenAppsSection = { currentNavIndex = 1 },
                        onNavigateToVault = onNavigateToVault,
                        onNavigateToJunkCleaner = onNavigateToJunkCleaner,
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToDisguise = onNavigateToDisguise,
                        onNavigateToTheme = onNavigateToTheme,
                        onNavigateToCloudBackup = onNavigateToCloudBackup,
                        onNavigateToIntruders = onNavigateToIntruders,
                        viewModel = viewModel
                    )
                    1 -> AppsLockListView(
                        apps = filteredApps,
                        searchQuery = searchQuery,
                        selectedFilter = selectedAppFilterTab,
                        onFilterSelected = { selectedAppFilterTab = it },
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onToggleLock = { viewModel.toggleAppLock(it) },
                        onLaunchApp = { viewModel.triggerAppLaunch(it) },
                        onNavigateToPermissions = onNavigateToPermissions
                    )
                    2 -> FeaturesListView(
                        onNavigateToVault = onNavigateToVault,
                        onNavigateToJunkCleaner = onNavigateToJunkCleaner,
                        onNavigateToPermissions = onNavigateToPermissions,
                        onNavigateToDisguise = onNavigateToDisguise,
                        onNavigateToTheme = onNavigateToTheme,
                        onNavigateToCloudBackup = onNavigateToCloudBackup,
                        onNavigateToIntruders = onNavigateToIntruders
                    )
                }
            }
        }
    }
}

/**
 * الواجهة الرئيسية المطابقة للتصميم المطلوب (Dashboard)
 */
@Composable
private fun HomeDashboardView(
    apps: List<ProtectedAppEntity>,
    lockedCount: Int,
    totalCount: Int,
    onOpenAppsSection: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToJunkCleaner: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToDisguise: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToCloudBackup: () -> Unit,
    onNavigateToIntruders: () -> Unit,
    viewModel: AppLockViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: العنوان "تطبيقاتي" + درع الأمان المضيء
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تطبيقاتي",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "$lockedCount من إجمالي $totalCount تطبيق محمي",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }

                // زر الدرع المضيء العلوي
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E3A8A).copy(alpha = 0.8f),
                                    Color(0xFF0F172A).copy(alpha = 0.9f)
                                )
                            )
                        )
                        .border(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.6f), CircleShape)
                        .clickable(onClick = onNavigateToPermissions),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "الحماية",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. Apps Section (قسم التطبيقات والأدوات الرئيسية - شبكة أنيقة)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // الصف الأول: 3 بطاقات
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryFeatureGridCard(
                        title = "قفل التطبيقات",
                        subtitle = "احم تطبيقاتك وخصوصيتك",
                        icon = Icons.Default.Lock,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenAppsSection
                    )
                    PrimaryFeatureGridCard(
                        title = "مدير الملفات",
                        subtitle = "إدارة ملفاتك بسهولة",
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToVault
                    )
                    PrimaryFeatureGridCard(
                        title = "تنظيف الهاتف",
                        subtitle = "تخلص من الملفات الزائدة",
                        icon = Icons.Default.CleaningServices,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToJunkCleaner
                    )
                }

                // الصف الثاني: بطاقتان عريضتان
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PrimaryFeatureGridCard(
                        title = "الحماية",
                        subtitle = "حماية شاملة لهاتفك",
                        icon = Icons.Default.Security,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPermissions
                    )
                    PrimaryFeatureGridCard(
                        title = "الأدوات",
                        subtitle = "أدوات ذكية مفيدة",
                        icon = Icons.Default.BusinessCenter,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDisguise
                    )
                }
            }
        }

        // 3. Features Section (قسم المميزات)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المميزات",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "✦",
                        fontSize = 18.sp,
                        color = Color(0xFF818CF8),
                        fontWeight = FontWeight.Bold
                    )
                }

                // ميزة: الحماية الذكية
                FeatureListItemCard(
                    title = "الحماية الذكية",
                    subtitle = "حماية متقدمة في الوقت الحقيقي",
                    icon = Icons.Default.Shield,
                    onClick = onNavigateToPermissions
                )

                // ميزة: الخصوصية
                FeatureListItemCard(
                    title = "الخصوصية",
                    subtitle = "حافظ على بياناتك وخصوصيتك",
                    icon = Icons.Default.Lock,
                    onClick = onNavigateToVault
                )

                // ميزة: الأداء السريع
                FeatureListItemCard(
                    title = "الأداء السريع",
                    subtitle = "تحسين الأداء وتسريع هاتفك",
                    icon = Icons.Default.Speed,
                    onClick = onNavigateToJunkCleaner
                )

                // ميزة: سجل المتطفلين
                FeatureListItemCard(
                    title = "سجل الدخلاء والمتطفلين",
                    subtitle = "التقاط صور لمن يحاول فتح القفل",
                    icon = Icons.Default.CameraAlt,
                    onClick = onNavigateToIntruders
                )
            }
        }

        // 4. Native Ad Section (قسم الإعلان المدمج الأنيق في الأسفل)
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                NativeAdCard()
            }
        }
    }
}

/**
 * بطاقة الميزة المربعة في الشبكة العلوية بتأثير التوهج النيوني الأزرق
 */
@Composable
private fun PrimaryFeatureGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131722)
        ),
        modifier = modifier
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF263044),
                        Color(0xFF161B26)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0xFF1D4ED8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // دائرة الأيقونة المتوهجة
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8).copy(alpha = 0.5f),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF60A5FA),
                                Color(0xFF2563EB).copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * عنصر قائمة المميزات بتصميم مستطيل مع سهم للتنقل (Chevron)
 */
@Composable
private fun FeatureListItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131722)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF263044),
                        Color(0xFF161B26)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // سهم الدخول (RTL)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // النصوص
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // أيقونة الميزة المتوهجة
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E3A8A).copy(alpha = 0.6f),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF3B82F6).copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * واجهة إدارة وقفل التطبيقات مع البحث والفلترة
 */
@Composable
private fun AppsLockListView(
    apps: List<ProtectedAppEntity>,
    searchQuery: String,
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit,
    onSearchChanged: (String) -> Unit,
    onToggleLock: (ProtectedAppEntity) -> Unit,
    onLaunchApp: (ProtectedAppEntity) -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // شريط البحث
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("بحث عن تطبيق...", color = Color(0xFF64748B), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFF263044),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF131722),
                unfocusedContainerColor = Color(0xFF131722)
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // أزرار الفلترة: الكل / مقفل / مفتوح
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131722), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF263044), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            FilterTabItem(
                text = "الكل",
                selected = selectedFilter == 0,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(0) }
            )
            FilterTabItem(
                text = "المقفل",
                selected = selectedFilter == 1,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(1) }
            )
            FilterTabItem(
                text = "المفتوح",
                selected = selectedFilter == 2,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(2) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // قائمة التطبيقات
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            item {
                NativeAdCard()
            }

            items(apps, key = { it.packageName }) { app ->
                AppLockItemRow(
                    app = app,
                    onToggleLock = { onToggleLock(app) },
                    onClick = { onLaunchApp(app) }
                )
            }
        }
    }
}

/**
 * زر التبويب داخل الفلترة
 */
@Composable
private fun FilterTabItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF2563EB) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF94A3B8),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

/**
 * عنصر صف التطبيق في القائمة
 */
@Composable
private fun AppLockItemRow(
    app: ProtectedAppEntity,
    onToggleLock: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var iconDrawable by remember(app.packageName) { mutableStateOf<Drawable?>(null) }
    var isIconLoading by remember(app.packageName) { mutableStateOf(true) }

    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            try {
                iconDrawable = context.packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                iconDrawable = null
            } finally {
                isIconLoading = false
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isLocked) Color(0xFF171E2E) else Color(0xFF131722)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (app.isLocked) Color(0xFF2563EB).copy(alpha = 0.5f) else Color(0xFF263044),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isIconLoading && iconDrawable != null) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(model = iconDrawable),
                            contentDescription = app.appName,
                            modifier = Modifier.size(46.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.packageName,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = app.isLocked,
                onCheckedChange = { onToggleLock() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2563EB),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

/**
 * شاشة استعراض كافة المميزات
 */
@Composable
private fun FeaturesListView(
    onNavigateToVault: () -> Unit,
    onNavigateToJunkCleaner: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToDisguise: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToCloudBackup: () -> Unit,
    onNavigateToIntruders: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "جميع المميزات والأدوات",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            FeatureListItemCard(
                title = "خزانة الصور والملفات",
                subtitle = "إخفاء الصور ومقاطع الفيديو برقم سري",
                icon = Icons.Default.Lock,
                onClick = onNavigateToVault
            )
        }

        item {
            FeatureListItemCard(
                title = "منظف الملفات المؤقتة",
                subtitle = "تفريغ مساحة الذاكرة وتسريع الهاتف",
                icon = Icons.Default.CleaningServices,
                onClick = onNavigateToJunkCleaner
            )
        }

        item {
            FeatureListItemCard(
                title = "أيقونة التنكر والتمويه",
                subtitle = "تغيير شكل أيقونة التطبيق لآلة حاسبة أو مفكرة",
                icon = Icons.Default.PhoneAndroid,
                onClick = onNavigateToDisguise
            )
        }

        item {
            FeatureListItemCard(
                title = "الثيمات والمظهر",
                subtitle = "تخصيص ألوان وتصميم شاشة القفل",
                icon = Icons.Default.Palette,
                onClick = onNavigateToTheme
            )
        }

        item {
            FeatureListItemCard(
                title = "النسخ الاحتياطي السحابي",
                subtitle = "مزامنة إعدادات القفل مع حساب Google Drive",
                icon = Icons.Default.Cloud,
                onClick = onNavigateToCloudBackup
            )
        }

        item {
            FeatureListItemCard(
                title = "سجل صور المتطفلين",
                subtitle = "عرض صور الأشخاص الذين أدخلوا كلمة سر خاطئة",
                icon = Icons.Default.Security,
                onClick = onNavigateToIntruders
            )
        }

        item {
            FeatureListItemCard(
                title = "مركز الصلاحيات والأذونات",
                subtitle = "التحقق من حالة إذن الوصول والحماية الخلفية",
                icon = Icons.Default.Shield,
                onClick = onNavigateToPermissions
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            NativeAdCard()
        }
    }
}

/**
 * شريط التنقل السفلي الحديث المتناسق مع التصميم
 */
@Composable
private fun ModernBottomNavigationBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    Surface(
        color = Color(0xFF0F131D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2536)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = Icons.Default.Home,
                label = "الرئيسية",
                selected = selectedIndex == 0,
                onClick = { onItemSelected(0) }
            )
            BottomNavItem(
                icon = Icons.Default.Apps,
                label = "التطبيقات",
                selected = selectedIndex == 1,
                onClick = { onItemSelected(1) }
            )
            BottomNavItem(
                icon = Icons.Default.Star,
                label = "المميزات",
                selected = selectedIndex == 2,
                onClick = { onItemSelected(2) }
            )
            BottomNavItem(
                icon = Icons.Default.Settings,
                label = "الإعدادات",
                selected = selectedIndex == 3,
                onClick = { onItemSelected(3) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF3B82F6) else Color(0xFF64748B),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF3B82F6) else Color(0xFF64748B)
        )
    }
}
