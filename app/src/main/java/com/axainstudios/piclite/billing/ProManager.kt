package com.axainstudios.piclite.billing

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/** Manages the one-time "PicLite Pro" purchase (Google Play Billing) and the entitlement. */
object ProManager {

    /** One-time (in-app) product ID — must match the Play Console product. */
    const val PRODUCT_ID = "piclite_pro"
    const val FREE_PHOTO_LIMIT = 5
    const val PRO_PHOTO_LIMIT = 50

    var isPro by mutableStateOf(false)
        private set
    var productDetails: ProductDetails? by mutableStateOf(null)
        private set

    val priceText: String
        get() = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$4.99"

    fun photoLimit(): Int = if (isPro) PRO_PHOTO_LIMIT else FREE_PHOTO_LIMIT

    private var billing: BillingClient? = null
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (billing != null) return
        prefs = context.getSharedPreferences("piclite_pro", Context.MODE_PRIVATE)
        isPro = prefs?.getBoolean("isPro", false) ?: false
        billing = BillingClient.newBuilder(context.applicationContext)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .setListener(listener)
            .build()
        connect()
    }

    private val listener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun connect() {
        billing?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct(); queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(ProductType.INAPP)
                    .build()
            )
        ).build()
        billing?.queryProductDetailsAsync(params) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = list.firstOrNull()
            }
        }
    }

    /** Restore / refresh entitlement from Play. */
    fun queryPurchases() {
        billing?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = purchases.any {
                    it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                updatePro(owned)
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    fun restore() = queryPurchases()

    fun purchase(activity: Activity) {
        val pd = productDetails ?: return
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(pd)
                    .build()
            )
        ).build()
        billing?.launchBillingFlow(activity, params)
    }

    private fun handlePurchase(p: Purchase) {
        if (p.purchaseState == Purchase.PurchaseState.PURCHASED) {
            updatePro(true)
            if (!p.isAcknowledged) {
                billing?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
                ) {}
            }
        }
    }

    private fun updatePro(value: Boolean) {
        isPro = value
        prefs?.edit()?.putBoolean("isPro", value)?.apply()
    }
}
