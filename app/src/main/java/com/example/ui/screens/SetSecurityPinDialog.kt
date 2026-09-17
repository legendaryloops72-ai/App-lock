package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.SecuritySettingsEntity
import com.example.ui.viewmodel.AppLockViewModel

@Composable
fun SetSecurityPinDialog(
    settings: SecuritySettingsEntity,
    viewModel: AppLockViewModel,
    onSaved: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("إنشاء رمز قفل التطبيقات") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("أنشئ رمزًا جديدًا لحماية التطبيقات التي تختار قفلها. هذا الرمز لا يُطلب عند منح الصلاحيات.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.all(Char::isDigit) && it.length <= 6) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("الرمز الجديد (4-6 أرقام)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = {
                        if (it.all(Char::isDigit) && it.length <= 6) {
                            confirmation = it
                            error = null
                        }
                    },
                    label = { Text("إعادة كتابة الرمز") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    pin.length !in 4..6 -> error = "الرمز يجب أن يكون من 4 إلى 6 أرقام."
                    pin != confirmation -> error = "الرمزان غير متطابقين."
                    else -> {
                        viewModel.updateSettings(
                            settings.copy(
                                pin = pin,
                                lockType = "PIN",
                                isOnboardingCompleted = true
                            )
                        )
                        onSaved()
                    }
                }
            }) {
                Text("حفظ الرمز")
            }
        },
        dismissButton = null
    )
}
