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
    private val productIds: List<String> = emptyList(),
    private val onUserPurchasedRemoveAds: () -> Unit, // Callback khi mua thành công
    private val onBillingSetupFailed: (() -> Unit)? = null,
    private val onPurchaseFailed: ((billingResult: BillingResult) -> Unit)? = null
) {
    private lateinit var billingClient: BillingClient

    private val _isUserPremium = MutableStateFlow(false)
    val isUserPremium = _isUserPremium.asStateFlow()

    private val _purchasedProducts = MutableStateFlow<Set<String>>(emptySet())
    val purchasedProducts = _purchasedProducts.asStateFlow()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()
    
    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
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

        val allProductIds = (productIds + PRODUCT_ID_REMOVE_ADS).distinct()
        val productList = allProductIds.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                }
                i(TAG, "Product details fetched successfully: ${productDetailsMap.keys}")
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
            val purchasedSet = mutableSetOf<String>()
            var isPremium = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        purchasedSet.addAll(purchase.products)
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase.purchaseToken, purchase.products)
                        }
                    }
                }
            } else {
                e(TAG, "Error querying existing purchases: ${billingResult.debugMessage}, Response Code: ${billingResult.responseCode}")
            }
            if (purchasedSet.contains(PRODUCT_ID_REMOVE_ADS)) {
                isPremium = true
            }
            if (isPremium && !_isUserPremium.value) {
                onUserPurchasedRemoveAds()
            }
            _isUserPremium.value = isPremium
            _purchasedProducts.value = purchasedSet
            i(TAG, "Existing purchases checked. User is premium: ${_isUserPremium.value}, Products: $purchasedSet")
        }
    }

    fun queryPurchasesAsync() {
        queryExistingPurchases()
    }

    fun launchPurchaseFlow(activity: Activity, productId: String = PRODUCT_ID_REMOVE_ADS) {
        if (!billingClient.isReady) {
            e(TAG, "launchPurchaseFlow: BillingClient is not ready.")
            onPurchaseFailed?.invoke(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE).build())
            return
        }

        val productDetails = productDetailsMap[productId]
        if (productDetails == null) {
            e(TAG, "launchPurchaseFlow: Product details for $productId not available. Querying again...")
            queryProductDetails()
            onPurchaseFailed?.invoke(BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE).build())
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
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
            i(TAG, "Billing flow launched successfully for $productId.")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase.purchaseToken, purchase.products)
            } else {
                updatePurchasedState(purchase.products)
                i(TAG, "Purchase for ${purchase.products} already acknowledged.")
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            i(TAG, "Purchase is PENDING for: ${purchase.products.joinToString()}. Inform user.")
        } else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
            e(TAG, "Purchase state is UNSPECIFIED for: ${purchase.products.joinToString()}")
        }
    }

    private fun acknowledgePurchase(purchaseToken: String, products: List<String>) {
        if (!billingClient.isReady) {
            e(TAG, "acknowledgePurchase: BillingClient is not ready.")
            return
        }
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                updatePurchasedState(products)
                i(TAG, "Purchase acknowledged successfully for $products.")
            } else {
                e(TAG, "Failed to acknowledge purchase. Error: ${billingResult.debugMessage}, Response Code: ${billingResult.responseCode}")
            }
        }
    }

    private fun updatePurchasedState(products: List<String>) {
        val newSet = _purchasedProducts.value + products
        _purchasedProducts.value = newSet
        
        if (products.contains(PRODUCT_ID_REMOVE_ADS) && !_isUserPremium.value) {
            _isUserPremium.value = true
            onUserPurchasedRemoveAds()
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
