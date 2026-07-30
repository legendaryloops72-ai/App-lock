package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SecuritySettingsEntity
import com.example.ui.viewmodel.AppLockViewModel
import java.io.File
import java.io.FileOutputStream

data class GradientPreset(
    val name: String,
    val colors: List<Color>,
    val serializedValue: String
)

val gradientPresets = listOf(
    GradientPreset("شفق بنفسجي", listOf(Color(0xFF6750A4), Color(0xFF4F378B)), "0xFF6750A4,0xFF4F378B"),
    GradientPreset("أعماق المحيط", listOf(Color(0xFF005B96), Color(0xFF011F4B)), "0xFF005B96,0xFF011F4B"),
    GradientPreset("غروب الشمس", listOf(Color(0xFFFF6F61), Color(0xFFDE5246)), "0xFFFF6F61,0xFFDE5246"),
    GradientPreset("غابة زمردية", listOf(Color(0xFF11998E), Color(0xFF38EF7D)), "0xFF11998E,0xFF38EF7D"),
    GradientPreset("منتصف الليل", listOf(Color(0xFF232526), Color(0xFF414345)), "0xFF232526,0xFF414345"),
    GradientPreset("ذهب فخم", listOf(Color(0xFFF2994A), Color(0xFFF2C94C)), "0xFFF2994A,0xFFF2C94C"),
    GradientPreset("ياقوت أحمر", listOf(Color(0xFFCB356B), Color(0xFFBD3F32)), "0xFFCB356B,0xFFBD3F32"),
    GradientPreset("أرجواني ملكي", listOf(Color(0xFF9D50BB), Color(0xFF6E48AA)), "0xFF9D50BB,0xFF6E48AA")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentSettings = settings ?: SecuritySettingsEntity()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.filesDir, "custom_wallpaper_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                viewModel.updateSettings(
                    currentSettings.copy(
                        wallpaperType = "CUSTOM",
                        wallpaperValue = file.absolutePath
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تخصيص الثيم والخلفيات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark Mode Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "الوضع الداكن (Dark Mode)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "تفعيل المظهر الليلي للتطبيق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = currentSettings.isDarkMode,
                        onCheckedChange = { checked ->
                            viewModel.updateSettings(currentSettings.copy(isDarkMode = checked))
                        }
                    )
                }
            }

            // Custom Gallery Wallpaper Button
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "اختيار خلفية من الاستوديو", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Default System Wallpaper Reset Button
            if (currentSettings.wallpaperType != "DEFAULT") {
                OutlinedButton(
                    onClick = {
                        viewModel.updateSettings(currentSettings.copy(wallpaperType = "DEFAULT", wallpaperValue = ""))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "إعادة تعيين الخلفية الافتراضية")
                }
            }

            Text(
                text = "اختر خلفيتك المفضلة (تدرجات لونية جاهزة):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Gradient Presets Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(gradientPresets) { preset ->
                    val isSelected = currentSettings.wallpaperType == "GRADIENT" && currentSettings.wallpaperValue == preset.serializedValue
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateSettings(
                                    currentSettings.copy(
                                        wallpaperType = "GRADIENT",
                                        wallpaperValue = preset.serializedValue
                                    )
                                )
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(preset.colors))
                                .padding(12.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "محدد", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
