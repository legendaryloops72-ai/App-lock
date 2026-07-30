#!/bin/bash
sed -i 's/composable("home")/composable("home", enterTransition = { androidx.compose.animation.EnterTransition.None }, exitTransition = { androidx.compose.animation.ExitTransition.None })/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/composable("onboarding")/composable("onboarding", enterTransition = { androidx.compose.animation.EnterTransition.None }, exitTransition = { androidx.compose.animation.ExitTransition.None })/g' app/src/main/java/com/example/MainActivity.kt
