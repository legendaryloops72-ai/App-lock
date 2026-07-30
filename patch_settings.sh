#!/bin/bash
sed -i 's/var showLanguageDialog by remember { mutableStateOf(false) }/var showLanguageDialog by remember { mutableStateOf(false) }\n    var showLockTimeoutDialog by remember { mutableStateOf(false) }/g' app/src/main/java/com/example/ui/screens/SettingsScreen.kt

cat << 'INNER_EOF' > /tmp/timeout_dialog.txt

    if (showLockTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showLockTimeoutDialog = false },
            title = { Text("قفل تلقائي بعد مغادرة التطبيق") },
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
                        Text("فوري")
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
                        Text("بعد دقيقة")
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
                        Text("بعد 5 دقائق")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLockTimeoutDialog = false }) { Text("إغلاق") }
            }
        )
    }
INNER_EOF

# Append the dialog at the end before the last closing brace
sed -i -e '/^}$/i \    ' -e '/^}$/r /tmp/timeout_dialog.txt' app/src/main/java/com/example/ui/screens/SettingsScreen.kt

cat << 'INNER_EOF' > /tmp/timeout_row.txt
                        Divider(color = Color(0xFF2D3242))
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
                                Text(text = "قفل تلقائي بعد مغادرة التطبيق", fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = when (currentSettings.lockTimeout) {
                                    "IMMEDIATELY" -> "فوري"
                                    "1_MIN" -> "بعد دقيقة"
                                    "5_MIN" -> "بعد 5 دقائق"
                                    else -> "فوري"
                                }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
INNER_EOF

sed -i '/\/\/ Change PIN/r /tmp/timeout_row.txt' app/src/main/java/com/example/ui/screens/SettingsScreen.kt

