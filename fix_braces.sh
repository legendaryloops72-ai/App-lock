#!/bin/bash
# Remove the extra brace before if (showLockTimeoutDialog)
sed -i 's/    }//g' app/src/main/java/com/example/ui/screens/SettingsScreen.kt
# Wait, that's dangerous. Let me just replace the exact text.
