package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.example.service.AdManager

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // Remember the AdView instance to prevent re-creation and redundant loadAd calls on recomposition
    val adView = remember {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = AdManager.BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView },
        update = {}
    )
}

