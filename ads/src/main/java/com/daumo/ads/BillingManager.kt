package com.daumo.ads

import android.app.Activity
import android.content.Context
import android.util.Log.*
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
private var PRODUCT_ID_REMOVE_ADS = "remove_ads_sku" // Will be updated with BuildConfig value
class BillingManager(
    private val context: Context,
    private val onUserPurchasedRemoveAds: () -> Unit, // Callback khi mua thành công
    private val onBillingSetupFailed: (() -> Unit)? = null,
    private val onPurchaseFailed: ((billingResult: BillingResult) -> Unit)? = null
) {
    private lateinit var billingClient: BillingClient

    private val _isUserPremium = MutableStateFlow(false)
    val isUserPremium = _isUserPremium.asStateFlow()

    private var removeAdsProductDetails: ProductDetails? = null
    
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (purchase.products.contains(PRODUCT_ID_REMOVE_ADS)) {
                        handlePurchase(purchase)
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                i(TAG, "User cancelled the purchase flow.")
            } else {
                e(TAG, "Purchase error. Response Code: ${billingResult.responseCode}, Debug message: ${billingResult.debugMessage}")
                billingResult.responseCode.let { subCode ->
                    e(TAG, "Purchase error Sub Response Code: $subCode")
                }
                onPurchaseFailed?.invoke(billingResult)
            }
        }

    init {
        // Update PRODUCT_ID_REMOVE_ADS with the value from BuildConfig
        PRODUCT_ID_REMOVE_ADS = "remove_ads_sku"
        setupBillingClient()
    }

    private fun setupBillingClient() {
        val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(pendingPurchasesParams)
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    i(TAG, "Billing Client Setup Finished successfully.")
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    e(TAG, "Billing Client Setup Failed. Error: ${billingResult.debugMessage}, Response Code: ${billingResult.responseCode}")
                    onBillingSetupFailed?.invoke()
                }
            }

            override fun onBillingServiceDisconnected() {
                w(TAG, "Billing Service Disconnected. Auto-reconnection is enabled.")
            }
        })
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) {
            e(TAG, "queryProductDetails: BillingClient is not ready.")
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                removeAdsProductDetails = productDetailsList.find { it.productId == PRODUCT_ID_REMOVE_ADS }
                if (removeAdsProductDetails == null) {
                    e(TAG, "Product $PRODUCT_ID_REMOVE_ADS not found in Play Console or not configured correctly.")
                } else {
                    i(TAG, "Product details for $PRODUCT_ID_REMOVE_ADS fetched successfully.")
                }
            } else {
                e(TAG, "Error querying product details. Response Code: ${billingResult.responseCode}, Debug Message: ${billingResult.debugMessage}")
            }
        }
    }

    fun queryExistingPurchases() {
        if (!billingClient.isReady) {
            e(TAG, "queryExistingPurchases: BillingClient is not ready.")
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            var isPremium = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    if (purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        isPremium = true
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase.purchaseToken, isRestoring = true)
                        }
                    }
                }
            } else {
                e(TAG, "Error querying existing purchases: ${billingResult.debugMessage}, Response Code: ${billingResult.responseCode}")
            }
            if (isPremium && !_isUserPremium.value) {
            } else if (!isPremium) {
                _isUserPremium.value = false
            }
            i(TAG, "Existing purchases checked. User is premium: ${_isUserPremium.value}")
        }
    }

    fun queryPurchasesAsync() {
        if (!billingClient.isReady) {
            e(TAG, "queryPurchasesAsync: BillingClient is not ready.")
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var foundRemoveAdsPurchase = false
                for (purchase in purchasesList) {
                    if (purchase.products.contains(PRODUCT_ID_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        foundRemoveAdsPurchase = true
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase.purchaseToken)
                        }
                    }
                }
                _isUserPremium.value = foundRemoveAdsPurchase
                if (foundRemoveAdsPurchase) {
                    i(TAG, "User has already purchased remove_ads.")
                } else {
                    i(TAG, "User has NOT purchased remove_ads.")
                }
            } else {
                e(TAG, "Error querying purchases: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        if (!billingClient.isReady) {
            e(TAG, "launchPurchaseFlow: BillingClient is not ready.")
            onPurchaseFailed?.invoke(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE).build())
            return
        }

        if (removeAdsProductDetails == null) {
            e(TAG, "launchPurchaseFlow: Product details for $PRODUCT_ID_REMOVE_ADS not available. Querying again...")
            queryProductDetails()
            onPurchaseFailed?.invoke(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE).build())
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(removeAdsProductDetails!!)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val launchResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            e(TAG, "Failed to launch billing flow. Error: ${launchResult.debugMessage}, Response Code: ${launchResult.responseCode}")
            launchResult.responseCode.let { subCode ->
                e(TAG, "Launch billing flow Sub Response Code: $subCode")
            }
            onPurchaseFailed?.invoke(launchResult)
        } else {
            i(TAG, "Billing flow launched successfully for $PRODUCT_ID_REMOVE_ADS.")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase.purchaseToken)
            } else {
                if (!_isUserPremium.value) {
                    _isUserPremium.value = true
                    onUserPurchasedRemoveAds()
                }
                i(TAG, "Purchase for $PRODUCT_ID_REMOVE_ADS already acknowledged.")
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            i(TAG, "Purchase is PENDING for: ${purchase.products.joinToString()}. Inform user.")
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            e(TAG, "Purchase state is UNSPECIFIED for: ${purchase.products.joinToString()}")
        }
    }

    private fun acknowledgePurchase(purchaseToken: String, isRestoring: Boolean = false) {
        if (!billingClient.isReady) {
            e(TAG, "acknowledgePurchase: BillingClient is not ready.")
            return
        }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (!_isUserPremium.value) {
                    _isUserPremium.value = true
                    onUserPurchasedRemoveAds()
                }
                i(TAG, "Purchase acknowledged successfully for $PRODUCT_ID_REMOVE_ADS. Is restoring: $isRestoring")
            } else {
                e(TAG, "Failed to acknowledge purchase. Error: ${billingResult.debugMessage}, Response Code: ${billingResult.responseCode}")
            }
        }
    }

    private fun acknowledgePurchase(purchaseToken: String) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _isUserPremium.value = true
                onUserPurchasedRemoveAds()
                i(TAG, "Purchase acknowledged successfully for $PRODUCT_ID_REMOVE_ADS.")
            } else {
                e(TAG, "Failed to acknowledge purchase. Error: ${billingResult.debugMessage}")
            }
        }
    }

    fun destroy() {
        if (::billingClient.isInitialized && billingClient.isReady) {
            billingClient.endConnection()
        }
        d(TAG, "BillingManager destroyed.")
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}
