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
import com.google.android.libraries.ads.mobile.sdk.banner.AdView

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isInitialized by AdManager.isInitialized.collectAsState()
    var bannerView by remember { mutableStateOf<View?>(null) }

    DisposableEffect(isInitialized) {
        var disposed = false
        var loadedView: View? = null
        if (isInitialized) {
            val activity = context as? Activity
            if (activity != null) {
                AdManager.loadBannerAd(activity) { adView ->
                    if (disposed) {
                        (adView as? AdView)?.destroy()
                    } else {
                        loadedView = adView
                        bannerView = adView
                    }
                }
            }
        }
        onDispose {
            disposed = true
            loadedView?.let { view ->
                (view.parent as? android.view.ViewGroup)?.removeView(view)
                (view as? AdView)?.destroy()
            }
            loadedView = null
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
