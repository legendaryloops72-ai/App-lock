package com.example.service

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * AdManager يوازي ad_service.dart المطلوب
 * 
 * NOTE: Replace the test Ad Unit IDs with your real AdMob Ad Unit IDs before releasing to production.
 */
object AdManager {
    // Test Ad Unit IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921" // Included as requested

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var isAppOpenAdLoading = false
    private var isEmergencyMode = false
    private var isAppJustOpened = true

    // تحميل إعلان فتح التطبيق (App Open Ad)
    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null || isAppOpenAdLoading || isEmergencyMode) return

        isAppOpenAdLoading = true
        val adRequest = AdRequest.Builder().build()
        
        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT_ID,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isAppOpenAdLoading = false
                    Log.d("AdManager", "App Open Ad Loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAppOpenAdLoading = false
                    Log.d("AdManager", "App Open Ad Failed: \${error.message}")
                }
            }
        )
    }

    // عرض إعلان فتح التطبيق
    fun showAppOpenAd(activity: Activity) {
        if (isEmergencyMode) return
        
        if (appOpenAd != null) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    loadAppOpenAd(activity)
                }
            }
            appOpenAd?.show(activity)
        } else {
            loadAppOpenAd(activity)
        }
    }

    // الدالة المسؤولة عن تحميل إعلان بيني (Interstitial)
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isAdLoading || isEmergencyMode) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("AdManager", "Failed to load Interstitial Ad: \${adError.message}")
                    interstitialAd = null
                    isAdLoading = false
                    // إعادة المحاولة التلقائية بعد فشل التحميل يمكن إضافتها هنا إن لزم الأمر
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("AdManager", "Interstitial Ad Loaded successfully")
                    interstitialAd = ad
                    isAdLoading = false
                }
            }
        )
    }

    // عرض الإعلان البيني (Interstitial) - مع تأخير ٣ ثوانٍ عند بداية التطبيق
    fun showInterstitialAd(activity: Activity) {
        if (isEmergencyMode) return
        
        if (interstitialAd != null) {
            // إضافة回调 لمعرفة متى يتم إغلاق الإعلان لتحميل إعلان جديد تلقائياً
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdManager", "Ad was dismissed.")
                    interstitialAd = null
                    // إعادة تحميل الإعلان التلقائي (Ad Reload)
                    loadInterstitialAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d("AdManager", "Ad failed to show.")
                    interstitialAd = null
                }
            }

            if (isAppJustOpened) {
                // تأخير عرض الإعلان ٣ ثوانٍ كما هو مطلوب لكي لا يؤثر على تجربة المستخدم
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3000)
                    interstitialAd?.show(activity)
                    isAppJustOpened = false
                }
            } else {
                interstitialAd?.show(activity)
            }
        } else {
            Log.d("AdManager", "The interstitial ad wasn't ready yet.")
            // إذا لم يكن جاهزاً حاول تحميله
            loadInterstitialAd(activity)
        }
    }

    // لتحديث حالة الطوارئ (Emergency Mode) لمنع الإعلانات
    fun setEmergencyMode(enabled: Boolean) {
        isEmergencyMode = enabled
    }
}
