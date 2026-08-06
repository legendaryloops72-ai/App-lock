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
 * Real AdMob App ID must be placed in /app/src/main/AndroidManifest.xml inside the meta-data element:
 * <meta-data
 *     android:name="com.google.android.gms.ads.APPLICATION_ID"
 *     android:value="ca-app-pub-3940256099942544~3347511713"/> <-- Replace with your real App ID (e.g., ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx)
 */
object AdManager {
    // Real / Test Ad Unit IDs (Defaults to Google test IDs for safety and local testing)
    // Replace with your real ones from the AdMob dashboard once ready for production.
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // <-- Real Banner Ad ID goes here
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // <-- Real Interstitial Ad ID goes here
    private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921" // <-- Real App Open Ad ID goes here

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
                    Log.e("AdManager", "App Open Ad Failed to load: ${error.message}")
                }
            }
        )
    }

    // عرض إعلان فتح التطبيق
    fun showAppOpenAd(activity: Activity) {
        if (isEmergencyMode) return
        
        if (appOpenAd != null) {
            try {
                appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AdManager", "App Open Ad dismissed.")
                        appOpenAd = null
                        loadAppOpenAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e("AdManager", "App Open Ad failed to show: ${adError.message}")
                        appOpenAd = null
                        loadAppOpenAd(activity)
                    }
                }

                // Safety Check: Avoid WindowManager$BadTokenException if Activity is finishing or destroyed
                if (!activity.isFinishing && !activity.isDestroyed) {
                    appOpenAd?.show(activity)
                } else {
                    Log.w("AdManager", "Skipped App Open Ad show: Activity is finishing/destroyed.")
                    appOpenAd = null
                }
            } catch (e: Exception) {
                Log.e("AdManager", "Exception while trying to show App Open Ad", e)
                appOpenAd = null
            }
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
                    Log.e("AdManager", "Failed to load Interstitial Ad: ${adError.message}")
                    interstitialAd = null
                    isAdLoading = false
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
            try {
                // إضافة回调 لمعرفة متى يتم إغلاق الإعلان لتحميل إعلان جديد تلقائياً
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AdManager", "Interstitial Ad was dismissed.")
                        interstitialAd = null
                        // إعادة تحميل الإعلان التلقائي (Ad Reload)
                        loadInterstitialAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e("AdManager", "Interstitial Ad failed to show: ${adError.message}")
                        interstitialAd = null
                        // Reload ad automatically to recover
                        loadInterstitialAd(activity)
                    }
                }

                if (isAppJustOpened) {
                    // تأخير عرض الإعلان ٣ ثوانٍ كما هو مطلوب لكي لا يؤثر على تجربة المستخدم
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000)
                        // Safety Check: Verify that the activity is still fully active after the delay
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            try {
                                interstitialAd?.show(activity)
                            } catch (e: Exception) {
                                Log.e("AdManager", "Exception during delayed Interstitial show", e)
                                interstitialAd = null
                            }
                        } else {
                            Log.w("AdManager", "Skipped delayed Interstitial Ad show: Activity is finishing/destroyed.")
                            interstitialAd = null
                        }
                        isAppJustOpened = false
                    }
                } else {
                    // Safety Check: Verify activity before direct show
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        interstitialAd?.show(activity)
                    } else {
                        Log.w("AdManager", "Skipped direct Interstitial Ad show: Activity is finishing/destroyed.")
                        interstitialAd = null
                    }
                }
            } catch (e: Exception) {
                Log.e("AdManager", "Exception while setting up fullScreenContentCallback or showing Interstitial Ad", e)
                interstitialAd = null
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

