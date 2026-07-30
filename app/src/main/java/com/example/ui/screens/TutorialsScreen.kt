package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppLockViewModel

data class FaqItem(val question: String, val answer: String, val isExpanded: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialsScreen(
    viewModel: AppLockViewModel,
    onBack: () -> Unit
) {
    val faqList = remember {
        mutableStateListOf(
            FaqItem("كيف يمكن تشغيل الفتح بصمة الإصبع؟", "يمكنك تفعيل الفتح ببصمة الإصبع من خلال قائمة الإعدادات الرئيسية بتشغيل مفتاح بصمة الإصبع."),
            FaqItem("كيف يمكن إيقاف الفتح بصمة الإصبع؟", "من نفس شاشة الإعدادات، قم بإلغاء تفعيل مفتاح بصمة الإصبع وسيحتاج التطبيق إلى رمز المرور فقط."),
            FaqItem("كيف يمكن تعيين رمز PIN؟", "اذهب إلى الإعدادات > تغيير رمز المرور، واكتب الرمز الجديد المكون من 4 أرقام."),
            FaqItem("كيف يمكن تغيير كلمة المرور؟", "يمكنك تغيير كلمة المرور في أي وقت من خلال إعدادات الأمان الرئيسية."),
            FaqItem("أحتاج إلى فتح القفل بشكل متكرر، وهذا يزعجني.", "يمكنك ضبط وقت القفل التلقائي لتجنب مطالبة كلمة المرور المتكررة في الإعدادات المتقدمة."),
            FaqItem("كيف يمكن إخفاء التطبيقات؟", "استخدم ميزة التنكر وتمويه الأيقونات لإخفاء التطبيقات خلف حاسبة أو متصفح وهمي."),
            FaqItem("كيف يمكن تشغيل الحماية ضد إزالة التثبيت؟", "قم بتفعيل حماية إزالة التثبيت من قسم الحماية الذكية لمنع إلغاء تثبيت التطبيق دون إذن.")
        )
    }

    var showSecurityQuestionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التعليمات", fontWeight = FontWeight.Bold, color = Color.White) },
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
                // Top Security Question Button
                item {
                    Button(
                        onClick = { showSecurityQuestionDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text(text = "تعيين سؤال الأمان", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                itemsIndexed(faqList) { index, faq ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF232733)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                faqList[index] = faq.copy(isExpanded = !faq.isExpanded)
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(text = faq.question, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }
                                Icon(
                                    imageVector = if (faq.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                            if (faq.isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = faq.answer, color = Color.Gray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { showSecurityQuestionDialog = true }) {
                                        Text("تعيين سؤال الأمان")
                                    }
                                }
                            }
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
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(text = "الملاحظات", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showSecurityQuestionDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityQuestionDialog = false },
            title = { Text("تعيين سؤال الأمان") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = "ما هو اسم أخصائي الطفولة المفضل لديك؟", onValueChange = {}, label = { Text("السؤال") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = "", onValueChange = {}, label = { Text("الإجابة") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { showSecurityQuestionDialog = false }) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityQuestionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
