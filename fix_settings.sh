#!/bin/bash
# Remove the stuff appended outside the class
# It starts with "    if (showLockTimeoutDialog)"
sed -i '/    if (showLockTimeoutDialog) {/,$d' app/src/main/java/com/example/ui/screens/SettingsScreen.kt

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
}
INNER_EOF

sed -i '$d' app/src/main/java/com/example/ui/screens/SettingsScreen.kt
cat /tmp/timeout_dialog.txt >> app/src/main/java/com/example/ui/screens/SettingsScreen.kt

