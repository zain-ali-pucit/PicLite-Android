package com.axain.photocompressor.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Loads and shows a single interstitial ad, keeping one preloaded so it can be shown
 * instantly at a natural break (when the user finishes a tool). A frequency cap keeps
 * the app from showing ads too often, which also helps stay within AdMob policy.
 */
object InterstitialAdManager {
    private const val TAG = "InterstitialAds"

    // Production interstitial ad unit ID for PicLite.
    // While developing, swap this for Google's test unit "ca-app-pub-3940256099942544/1033173712"
    // (or register your device as a test device) so you never click live ads.
    private const val AD_UNIT_ID = "ca-app-pub-6901103725908073/7191055844"

    // Show at most one interstitial per minute regardless of how often it's triggered.
    private const val MIN_INTERVAL_MS = 60_000L

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAt = 0L

    /** Requests an interstitial if none is loaded or loading. Safe to call repeatedly. */
    fun preload(context: Context) {
        if (com.axain.photocompressor.billing.ProManager.isPro) return
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Shows the preloaded interstitial (if the frequency cap allows) and runs [onDone] once
     * the ad is dismissed. If no ad is ready or it's too soon, [onDone] runs immediately.
     * Always safe to call — navigation never stalls waiting on an ad.
     */
    fun showThenContinue(activity: Activity, onDone: () -> Unit) {
        if (com.axain.photocompressor.billing.ProManager.isPro) { onDone(); return }
        val ad = interstitialAd
        val now = System.currentTimeMillis()
        if (ad == null || now - lastShownAt < MIN_INTERVAL_MS) {
            preload(activity)
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                lastShownAt = System.currentTimeMillis()
                preload(activity)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                preload(activity)
                onDone()
            }
        }
        ad.show(activity)
    }
}
