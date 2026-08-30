package com.example.service

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.Ad
import com.google.android.libraries.ads.mobile.sdk.common.AdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.OnAdInspectorClosedListener
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationStatus
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AdManager المحدث بالكامل لحزمة الجيل الجديد (Next-Gen GMA SDK)
 * يدعم جاهزية الوساطة والمزادات اللحظية (Mediation & Bidding Readiness)
 */
object AdManager {
    private const val TAG = "AdManager_Mediation"

    const val APP_ID = "ca-app-pub-4760027279848820~7533922348"
    const val BANNER_AD_UNIT_ID = "ca-app-pub-4760027279848820/6077370442"
    const val NATIVE_AD_UNIT_ID = "ca-app-pub-4760027279848820/7039098324"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAdLoading = false
    private var isAppOpenAdLoading = false
    private var isEmergencyMode = false
    private var isAppJustOpened = true
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // تهيئة Next-Gen SDK في الخلفية (Background Thread) لعدم تجميد خيط الواجهة والشاشة الافتتاحية
    fun initialize(context: Context, onComplete: ((InitializationStatus) -> Unit)? = null) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = InitializationConfig.Builder(APP_ID).build()
                MobileAds.initialize(appContext, config) { status ->
                    _isInitialized.value = true
                    Log.d(TAG, "Next-Gen MobileAds SDK Initialized in background successfully. Total Latency: ${status.totalLatency}ms")

                    // تسجيل حالة كل محول وساطة ومزاد إعلاني (Mediation / Bidding Adapters)
                    try {
                        val adapterStatusMap = status.adapterStatusMap
                        if (adapterStatusMap.isNotEmpty()) {
                            for ((adapterClass, adapterStatus) in adapterStatusMap) {
                                val state = when (adapterStatus.initializationState) {
                                    AdapterStatus.InitializationState.COMPLETE -> "READY (COMPLETE)"
                                    AdapterStatus.InitializationState.INITIALIZING -> "INITIALIZING"
                                    AdapterStatus.InitializationState.NOT_STARTED -> "NOT_STARTED"
                                    AdapterStatus.InitializationState.TIMED_OUT -> "TIMED_OUT"
                                    AdapterStatus.InitializationState.FAILED -> "FAILED"
                                    else -> "UNKNOWN"
                                }
                                Log.d(TAG, "Mediation Adapter: $adapterClass -> State: $state, Latency: ${adapterStatus.latency}ms, Desc: ${adapterStatus.description}")
                            }
                        } else {
                            Log.d(TAG, "No third-party mediation adapters registered.")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error checking adapter status map: ${e.message}")
                    }

                    onComplete?.invoke(status)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Next-Gen SDK on background thread", e)
            }
        }
    }

    // تسجيل تفاصيل استجابة الإعلان عند تحميل أي إعلان بنجاح
    private fun logAdResponseInfo(responseInfo: ResponseInfo?, adType: String) {
        if (responseInfo == null) return
        try {
            Log.d(TAG, "[$adType] Ad Loaded. ResponseInfo: $responseInfo")
        } catch (e: Exception) {
            Log.w(TAG, "Could not log response info: ${e.message}")
        }
    }

    // تحميل إعلان بنر Next-Gen
    fun loadBannerAd(activity: Activity, onAdLoaded: (View) -> Unit) {
        if (isEmergencyMode || !_isInitialized.value) {
            Log.w(TAG, "[Banner] Skipping load: isEmergencyMode=$isEmergencyMode, isInitialized=${_isInitialized.value}")
            return
        }

        try {
            val request = BannerAdRequest.Builder(BANNER_AD_UNIT_ID, AdSize.BANNER).build()
            BannerAd.load(request, object : AdLoadCallback<BannerAd> {
                override fun onAdLoaded(ad: BannerAd) {
                    logAdResponseInfo(ad.getResponseInfo(), "Banner")
                    val adView = ad.getView(activity)
                    onAdLoaded(adView)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "[Banner] Failed to load. Code: ${error.code}, Message: ${error.message}, ResponseID: ${error.responseInfo?.responseId}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading Next-Gen Banner Ad", e)
        }
    }

    // تحميل إعلان فتح التطبيق (App Open Ad)
    fun loadAppOpenAd(context: Context) {
        if (appOpenAd != null || isAppOpenAdLoading || isEmergencyMode || !_isInitialized.value) return

        isAppOpenAdLoading = true
        val request = AdRequest.Builder(APP_OPEN_AD_UNIT_ID).build()

        AppOpenAd.load(request, object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenAd = ad
                isAppOpenAdLoading = false
                logAdResponseInfo(ad.getResponseInfo(), "AppOpen")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                isAppOpenAdLoading = false
                Log.e(TAG, "[AppOpen] Failed to load. Code: ${error.code}, Message: ${error.message}")
            }
        })
    }

    // عرض إعلان فتح التطبيق
    fun showAppOpenAd(activity: Activity) {
        if (isEmergencyMode) return

        val ad = appOpenAd
        if (ad != null) {
            try {
                ad.adEventCallback = object : AppOpenAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "[AppOpen] Ad dismissed.")
                        appOpenAd = null
                        loadAppOpenAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                        Log.e(TAG, "[AppOpen] Failed to show: ${error.message}")
                        appOpenAd = null
                        loadAppOpenAd(activity)
                    }
                }

                if (!activity.isFinishing && !activity.isDestroyed) {
                    ad.show(activity)
                } else {
                    appOpenAd = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception showing App Open Ad", e)
                appOpenAd = null
            }
        } else {
            loadAppOpenAd(activity)
        }
    }

    // تحميل إعلان بيني (Interstitial Ad)
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isAdLoading || isEmergencyMode || !_isInitialized.value) return

        isAdLoading = true
        val request = AdRequest.Builder(INTERSTITIAL_AD_UNIT_ID).build()

        InterstitialAd.load(request, object : AdLoadCallback<InterstitialAd> {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                isAdLoading = false
                logAdResponseInfo(ad.getResponseInfo(), "Interstitial")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "[Interstitial] Failed to load. Code: ${error.code}, Message: ${error.message}")
                interstitialAd = null
                isAdLoading = false
            }
        })
    }

    // عرض الإعلان البيني (Interstitial)
    fun showInterstitialAd(activity: Activity) {
        if (isEmergencyMode) return

        val ad = interstitialAd
        if (ad != null) {
            try {
                ad.adEventCallback = object : InterstitialAdEventCallback {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "[Interstitial] Ad dismissed.")
                        interstitialAd = null
                        loadInterstitialAd(activity)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                        Log.e(TAG, "[Interstitial] Failed to show: ${error.message}")
                        interstitialAd = null
                        loadInterstitialAd(activity)
                    }
                }

                if (isAppJustOpened) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(3000)
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            try {
                                interstitialAd?.show(activity)
                            } catch (e: Exception) {
                                Log.e(TAG, "Exception during delayed Interstitial show", e)
                                interstitialAd = null
                            }
                        }
                        isAppJustOpened = false
                    }
                } else {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        ad.show(activity)
                    } else {
                        interstitialAd = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception showing Next-Gen Interstitial Ad", e)
                interstitialAd = null
            }
        } else {
            loadInterstitialAd(activity)
        }
    }

    // تحميل وإدارة الإعلان المدمج مع المحتوى (Native Ad)
    fun loadNativeAd(
        context: Context,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (isEmergencyMode || !_isInitialized.value) {
            Log.w(TAG, "[Native] Skipping load: isEmergencyMode=$isEmergencyMode, isInitialized=${_isInitialized.value}")
            return
        }

        try {
            val request = NativeAdRequest.Builder(
                NATIVE_AD_UNIT_ID,
                listOf(NativeAd.NativeAdType.NATIVE)
            ).build()

            NativeAdLoader.load(request, object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(ad: NativeAd) {
                    logAdResponseInfo(ad.getResponseInfo(), "Native")
                    onAdLoaded(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "[Native] Failed to load. Code: ${error.code}, Message: ${error.message}, ResponseID: ${error.responseInfo?.responseId}")
                    onAdFailed?.invoke(error)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception during loadNativeAd", e)
        }
    }

    // تحديث حالة الطوارئ
    fun setEmergencyMode(enabled: Boolean) {
        isEmergencyMode = enabled
    }

    // فتح أداة فحص واختبار الإعلانات (Ad Inspector)
    fun openAdInspector(context: Context, onClosed: ((errorMessage: String?) -> Unit)? = null) {
        try {
            MobileAds.openAdInspector(OnAdInspectorClosedListener { error ->
                if (error != null) {
                    Log.e(TAG, "Ad Inspector closed with error: ${error.message}")
                    onClosed?.invoke(error.message)
                } else {
                    Log.d(TAG, "Ad Inspector closed successfully.")
                    onClosed?.invoke(null)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception opening Ad Inspector", e)
            onClosed?.invoke(e.message)
        }
    }
}
