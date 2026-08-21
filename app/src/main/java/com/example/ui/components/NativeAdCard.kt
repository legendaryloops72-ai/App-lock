package com.example.ui.components

import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.AdManager
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

/**
 * مكون لعرض الإعلان المدمج مع المحتوى (Native Ad)
 * بتصميم عصري ومتناسق مع هوية التطبيق الداكنة متوافق مع معايير AdMob Validator و Next-Gen SDK
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInitialized by AdManager.isInitialized.collectAsState()
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    DisposableEffect(isInitialized) {
        if (isInitialized) {
            AdManager.loadNativeAd(
                context = context,
                onAdLoaded = { ad ->
                    nativeAdState = ad
                    isLoaded = true
                },
                onAdFailed = {
                    isLoaded = false
                }
            )
        }

        onDispose {
            nativeAdState = null
        }
    }

    AnimatedVisibility(
        visible = isLoaded && nativeAdState != null,
        enter = fadeIn()
    ) {
        nativeAdState?.let { nativeAd ->
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                factory = { ctx ->
                    val density = ctx.resources.displayMetrics.density

                    // NativeAdView هو الحاوي الرئيسي المطلق لكافة عناصر الإعلان
                    val nativeAdView = NativeAdView(ctx).apply {
                        val pad = (14 * density).toInt()
                        setPadding(pad, pad, pad, pad)
                        val shape = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#232733"))
                            cornerRadius = 16 * density
                            setStroke((1 * density).toInt(), android.graphics.Color.parseColor("#2D3242"))
                        }
                        background = shape
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        clipChildren = true
                        clipToPadding = true
                    }

                    // Root LinearLayout داخل NativeAdView
                    val rootLayout = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // 1. Top Row: Badge ("إعلان") + Advertiser/Store Info
                    val headerRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 0, 0, (8 * density).toInt())
                        }
                    }

                    val adBadge = TextView(ctx).apply {
                        text = "إعلان"
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        setTextColor(android.graphics.Color.parseColor("#3B82F6"))
                        setTypeface(null, Typeface.BOLD)
                        val badgePadH = (8 * density).toInt()
                        val badgePadV = (3 * density).toInt()
                        setPadding(badgePadH, badgePadV, badgePadH, badgePadV)
                        val badgeBg = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#1E3A8A"))
                            cornerRadius = 6 * density
                        }
                        background = badgeBg
                    }
                    headerRow.addView(adBadge)

                    val advertiserView = TextView(ctx).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins((8 * density).toInt(), 0, 0, 0)
                        }
                    }
                    headerRow.addView(advertiserView)

                    // 2. Middle Section: Icon + Headline + Body
                    val middleRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, (4 * density).toInt(), 0, (10 * density).toInt())
                        }
                    }

                    val iconView = ImageView(ctx).apply {
                        val iconSize = (48 * density).toInt()
                        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                            setMargins(0, 0, (12 * density).toInt(), 0)
                        }
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    middleRow.addView(iconView)

                    val textLayout = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val headlineView = TextView(ctx).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                        setTextColor(android.graphics.Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    textLayout.addView(headlineView)

                    val bodyView = TextView(ctx).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, (2 * density).toInt(), 0, 0)
                        }
                    }
                    textLayout.addView(bodyView)

                    middleRow.addView(textLayout)

                    // 3. MediaView: بحد أدنى 120dp عرض و 120dp طول متوافق مع AdMob Video Validator
                    val minDim = (120 * density).toInt()
                    val mediaView = MediaView(ctx).apply {
                        minimumWidth = minDim
                        minimumHeight = minDim
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (150 * density).toInt()
                        ).apply {
                            setMargins(0, (4 * density).toInt(), 0, (10 * density).toInt())
                        }
                        val mediaBg = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#1A1D24"))
                            cornerRadius = 10 * density
                        }
                        background = mediaBg
                        clipToOutline = true
                    }

                    // 4. Bottom: Call to Action (CTA) Button
                    val ctaButton = Button(ctx).apply {
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        setTextColor(android.graphics.Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        val btnBg = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#3B82F6"))
                            cornerRadius = 12 * density
                        }
                        background = btnBg
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (44 * density).toInt()
                        ).apply {
                            setMargins(0, (4 * density).toInt(), 0, 0)
                        }
                    }

                    // تجميع الهيكل داخل rootLayout المقيد بالكامل بداخل NativeAdView
                    rootLayout.addView(headerRow)
                    rootLayout.addView(middleRow)
                    rootLayout.addView(mediaView)
                    rootLayout.addView(ctaButton)

                    nativeAdView.addView(rootLayout)

                    // تعيين روابط العناصر الرسمية لـ NativeAdView
                    nativeAdView.iconView = iconView
                    nativeAdView.headlineView = headlineView
                    nativeAdView.bodyView = bodyView
                    nativeAdView.advertiserView = advertiserView
                    nativeAdView.callToActionView = ctaButton

                    // تسجيل الإعلان مع MediaView
                    nativeAdView.registerNativeAd(nativeAd, mediaView)

                    nativeAdView
                },
                update = { nativeAdView ->
                    val headlineView = nativeAdView.headlineView as? TextView
                    val bodyView = nativeAdView.bodyView as? TextView
                    val iconView = nativeAdView.iconView as? ImageView
                    val advertiserView = nativeAdView.advertiserView as? TextView
                    val ctaButton = nativeAdView.callToActionView as? Button
                    val mediaView = nativeAdView.mediaView

                    headlineView?.text = nativeAd.headline ?: ""

                    if (nativeAd.body != null) {
                        bodyView?.visibility = View.VISIBLE
                        bodyView?.text = nativeAd.body
                    } else {
                        bodyView?.visibility = View.GONE
                    }

                    if (nativeAd.icon != null) {
                        iconView?.visibility = View.VISIBLE
                        iconView?.setImageDrawable(nativeAd.icon?.drawable)
                    } else {
                        iconView?.visibility = View.GONE
                    }

                    val advertiserText = nativeAd.advertiser ?: nativeAd.store
                    if (!advertiserText.isNullOrBlank()) {
                        advertiserView?.visibility = View.VISIBLE
                        advertiserView?.text = advertiserText
                    } else {
                        advertiserView?.visibility = View.GONE
                    }

                    if (nativeAd.callToAction != null) {
                        ctaButton?.visibility = View.VISIBLE
                        ctaButton?.text = nativeAd.callToAction
                    } else {
                        ctaButton?.visibility = View.GONE
                    }

                    if (mediaView != null) {
                        nativeAdView.registerNativeAd(nativeAd, mediaView)
                    }
                }
            )
        }
    }
}
