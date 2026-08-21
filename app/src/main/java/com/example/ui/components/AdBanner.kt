package com.example.ui.components

import android.app.Activity
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.AdManager

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isInitialized by AdManager.isInitialized.collectAsState()
    var bannerView by remember { mutableStateOf<View?>(null) }

    DisposableEffect(isInitialized) {
        if (isInitialized) {
            val activity = context as? Activity
            if (activity != null) {
                AdManager.loadBannerAd(activity) { adView ->
                    bannerView = adView
                }
            }
        }
        onDispose {
            bannerView = null
        }
    }

    bannerView?.let { view ->
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = {
                val parent = view.parent as? android.view.ViewGroup
                parent?.removeView(view)
                view
            },
            update = {}
        )
    }
}
