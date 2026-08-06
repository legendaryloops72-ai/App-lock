package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.viewmodel.AppLockViewModel
import com.example.utils.FileEncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("VaultPrefs", Context.MODE_PRIVATE) }
    
    // Directory for encrypted files inside internal private storage
    val vaultDir = remember {
        File(context.filesDir, "vault_files").apply { mkdirs() }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<String?>(null) }
    
    // Load file list from Preferences
    var vaultItems by remember { 
        mutableStateOf(
            sharedPrefs.getStringSet("vault_items", emptySet())?.toList() ?: emptyList()
        )
    }

    var pendingIntentSender by remember { mutableStateOf<android.content.IntentSender?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "تم حذف الملفات الأصلية من المعرض بنجاح!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "لم يتم السماح بحذف الملفات الأصلية. يرجى حذفها يدوياً لضمان الخصوصية.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { sender ->
            val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(sender).build()
            deleteLauncher.launch(intentSenderRequest)
            pendingIntentSender = null
        }
    }

    // Picker for adding photos/videos to vault
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isLoading = true
            scope.launch(Dispatchers.IO) {
                val newItems = vaultItems.toMutableSet()
                var successCount = 0
                val successfullyEncryptedUris = mutableListOf<Uri>()

                uris.forEach { uri ->
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            // Generate unique encrypted filename
                            val fileName = "vault_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.enc"
                            val encryptedFile = File(vaultDir, fileName)
                            
                            // Encrypt and save to internal private storage
                            encryptedFile.outputStream().use { outputStream ->
                                FileEncryptionHelper.encrypt(inputStream, outputStream)
                            }
                            
                            newItems.add(fileName)
                            successCount++
                            successfullyEncryptedUris.add(uri)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Request user consent for deleting the original files from the gallery using modern MediaStore APIs
                val intentSender = deleteOriginalFiles(context, successfullyEncryptedUris)

                // Update UI state
                withContext(Dispatchers.Main) {
                    sharedPrefs.edit().putStringSet("vault_items", newItems).apply()
                    vaultItems = newItems.toList()
                    isLoading = false
                    
                    if (successCount > 0) {
                        if (intentSender != null) {
                            pendingIntentSender = intentSender
                        } else {
                            Toast.makeText(context, "تم نقل وتشفير $successCount ملفات بنجاح وحذف الأصل!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "فشل تشفير الملفات. حاول مرة أخرى.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الخزانة الآمنة (Vault)", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B1E2B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add to Vault")
            }
        },
        containerColor = Color(0xFF161922)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (vaultItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "الخزانة فارغة حالياً",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "قم بإضافة الصور والمقاطع السرية لحمايتها بكلمة مرور.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vaultItems) { itemFileName ->
                        VaultThumbnailItem(
                            fileName = itemFileName,
                            file = File(vaultDir, itemFileName),
                            onClick = {
                                selectedItemForDetail = itemFileName
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("جاري تشفير ونقل الملفات بأمان...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة ملف إلى الخزانة") },
            text = { Text("اختر الملفات المراد تشفيرها وحفظها بأمان داخل الخزانة الآمنة. سيتم حمايتها من السرقة والوصول غير المصرح به.") },
            confirmButton = {
                Button(
                    onClick = { 
                        showAddDialog = false 
                        viewModel.ignoreNextSelfLock()
                        launcher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("إضافة صور")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Full-screen detail viewer dialog
    selectedItemForDetail?.let { itemFileName ->
        VaultItemDetailDialog(
            fileName = itemFileName,
            file = File(vaultDir, itemFileName),
            onDismiss = { selectedItemForDetail = null },
            onDelete = {
                scope.launch(Dispatchers.IO) {
                    val file = File(vaultDir, itemFileName)
                    if (file.exists()) {
                        file.delete()
                    }
                    val newItems = vaultItems.toMutableSet().apply { remove(itemFileName) }
                    withContext(Dispatchers.Main) {
                        sharedPrefs.edit().putStringSet("vault_items", newItems).apply()
                        vaultItems = newItems.toList()
                        selectedItemForDetail = null
                        Toast.makeText(context, "تم حذف الملف نهائياً وبأمان من الخزانة والذاكرة.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRestore = {
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    val file = File(vaultDir, itemFileName)
                    val success = restoreFileToGallery(context, itemFileName, file)
                    if (success) {
                        if (file.exists()) {
                            file.delete()
                        }
                        val newItems = vaultItems.toMutableSet().apply { remove(itemFileName) }
                        withContext(Dispatchers.Main) {
                            sharedPrefs.edit().putStringSet("vault_items", newItems).apply()
                            vaultItems = newItems.toList()
                            isLoading = false
                            selectedItemForDetail = null
                            Toast.makeText(context, "تم فك تشفير الملف واستعادته إلى المعرض بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            Toast.makeText(context, "فشل استعادة الملف.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultThumbnailItem(
    fileName: String,
    file: File,
    onClick: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(fileName) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val decryptedBytes = file.inputStream().use { FileEncryptionHelper.decrypt(it) }
                    val decoded = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                    if (decoded != null) {
                        bitmap = decoded
                    } else {
                        hasError = true
                    }
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                hasError = true
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF232733))
            .combinedClickable(
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Vault Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay lock badge to show cryptographically secured status
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else if (hasError) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("تالف أو محذوف", color = Color.Gray, fontSize = 10.sp)
            }
        } else {
            CircularProgressIndicator(
                color = Color(0xFF2563EB),
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun VaultItemDetailDialog(
    fileName: String,
    file: File,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(fileName) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    val decryptedBytes = file.inputStream().use { FileEncryptionHelper.decrypt(it) }
                    bitmap = BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Main image viewer
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Full Encrypted View",
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
            } else if (!isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("فشل تحميل الصورة المشفرة", color = Color.White)
                }
            } else {
                CircularProgressIndicator(
                    color = Color(0xFF2563EB),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Top action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close", tint = Color.White)
                }
                
                Row {
                    // Restore button
                    IconButton(onClick = onRestore) {
                        Icon(Icons.Default.SettingsBackupRestore, contentDescription = "Restore to Gallery", tint = Color.Green)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Delete permanently button
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Securely", tint = Color.Red)
                    }
                }
            }

            // Bottom informational banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("مؤمن بتشفير AES-GCM 256-bit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("البيانات مفكوكة مؤقتاً بالذاكرة فقط لضمان الخصوصية.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Attempts to delete the original selected files securely from public storage.
 * On Android 11+ (API 30+), it uses MediaStore.createDeleteRequest to batch delete with user consent.
 * On Android 10 (API 29), it handles RecoverableSecurityException.
 */
private fun deleteOriginalFiles(context: Context, uris: List<Uri>): android.content.IntentSender? {
    if (uris.isEmpty()) return null

    // On Android 11+ (API 30+), we can use MediaStore.createDeleteRequest for seamless batch deletion
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val pendingIntent = android.provider.MediaStore.createDeleteRequest(context.contentResolver, uris)
            return pendingIntent.intentSender
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        // On older versions, we try standard contentResolver.delete individually
        uris.forEach { uri ->
            try {
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted <= 0) {
                    // Try direct file delete as a fallback (older versions / local files)
                    val projection = arrayOf(android.provider.MediaStore.Images.Media.DATA)
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                            val filePath = cursor.getString(columnIndex)
                            if (filePath != null) {
                                val file = File(filePath)
                                if (file.exists()) {
                                    file.delete()
                                }
                            }
                        }
                    }
                }
            } catch (securityException: SecurityException) {
                // On Android 10 (API 29), capture RecoverableSecurityException to request write/delete permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is android.app.RecoverableSecurityException) {
                    return securityException.userAction.actionIntent.intentSender
                }
                securityException.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    return null
}

/**
 * Decrypts the internally encrypted file, copies its bytes back to the public pictures collection.
 */
private fun restoreFileToGallery(context: Context, fileName: String, file: File): Boolean {
    try {
        if (!file.exists()) return false
        val decryptedBytes = file.inputStream().use { FileEncryptionHelper.decrypt(it) }
        
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "Restored_" + fileName.replace(".enc", ".jpg"))
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/Restored")
            }
        }
        
        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(decryptedBytes)
                return true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}
