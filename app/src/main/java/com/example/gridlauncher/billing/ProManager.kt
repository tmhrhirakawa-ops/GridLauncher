package com.example.gridlauncher.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * GridLauncher PRO（有料機能の買い切り解放）の購入状態を管理する。
 *
 * Google Play の課金（Play Billing）で、買い切りの商品[PRO_PRODUCT_ID]を購入済みかどうかを確認する。
 * 起動直後や通信できないときでも有料機能が一瞬消えないよう、最後に確認できた状態を保存しておき、
 * それを初期値にする（Google Play で確認できたら、その結果で上書きする）。
 *
 * デバッグビルドでは、Google Play Console に商品を登録していなくても動作を確かめられるよう、
 * 開発用にPROの状態を切り替えられる（[setDebugOverride]）。
 */
object ProManager {
    /** Google Play Console に登録する、PRO解放の買い切り商品のID。 */
    const val PRO_PRODUCT_ID = "gridlauncher_pro"

    private const val KEY_CACHED_PRO = "pro_unlocked_cached"
    private const val KEY_DEBUG_OVERRIDE = "pro_debug_override"

    private val _isPro = MutableStateFlow(false)

    /** PROの機能を使えるかどうか。 */
    val isPro: StateFlow<Boolean> = _isPro

    private val _price = MutableStateFlow<String?>(null)

    /** PRO解放の価格の表示用文字列（例: ￥480）。Google Play から取得できるまではnull。 */
    val price: StateFlow<String?> = _price

    private var prefs: SharedPreferences? = null
    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var isDebuggable = false

    // Google Play で確認できた購入状態（未確認ならnull）と、開発用の上書き（未設定ならnull）
    private var purchasedOnPlay: Boolean? = null
    private var debugOverride: Boolean? = null

    /** アプリ起動時に一度呼ぶ。Google Play に接続し、購入状態と価格を取得する。 */
    fun init(context: Context) {
        if (billingClient != null) return
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE)
        prefs = preferences
        isDebuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        debugOverride = if (isDebuggable && preferences.contains(KEY_DEBUG_OVERRIDE)) {
            preferences.getBoolean(KEY_DEBUG_OVERRIDE, false)
        } else null
        updateIsPro(cached = preferences.getBoolean(KEY_CACHED_PRO, false))

        billingClient = BillingClient.newBuilder(appContext)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases, isFullList = false)
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() により、次の呼び出し時に自動で再接続される
            }
        })
    }

    /** Google Play の購入状態を取得し直す（「購入を復元」やホーム画面に戻ったとき）。 */
    fun refreshPurchases() {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases, isFullList = true)
            }
        }
    }

    /**
     * PRO解放の購入画面（Google Play）を開く。
     *
     * @return 購入画面を開けたかどうか。商品の情報をまだ取得できていない（Google Play に接続できない、
     *   商品が未登録など）場合はfalse。
     */
    fun launchPurchase(activity: Activity): Boolean {
        val client = billingClient ?: return false
        val details = productDetails ?: run {
            queryProductDetails()
            return false
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
            )
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    /** デバッグビルドかどうか（開発用のPRO切り替えを表示するかどうか）。 */
    fun isDebugBuild(): Boolean = isDebuggable

    /** 開発用に、PROの状態を強制的に切り替える（デバッグビルドのみ。nullで上書きを解除）。 */
    fun setDebugOverride(enabled: Boolean?) {
        if (!isDebuggable) return
        debugOverride = enabled
        prefs?.edit {
            if (enabled == null) remove(KEY_DEBUG_OVERRIDE) else putBoolean(KEY_DEBUG_OVERRIDE, enabled)
        }
        updateIsPro()
    }

    private fun queryProductDetails() {
        val client = billingClient ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = detailsResult.productDetailsList.firstOrNull { it.productId == PRO_PRODUCT_ID }
                productDetails = details
                _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    /**
     * 購入の一覧からPROの購入状態を反映する。購入が完了していて未承認のものは承認する
     * （3日以内に承認しないと自動で払い戻されるため）。
     *
     * @param isFullList 手元の全購入の一覧かどうか（trueなら、PROが含まれないことを「未購入」と判断する）。
     */
    private fun handlePurchases(purchases: List<Purchase>, isFullList: Boolean) {
        val proPurchases = purchases.filter { PRO_PRODUCT_ID in it.products }
        val completed = proPurchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        completed.filter { !it.isAcknowledged }.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            billingClient?.acknowledgePurchase(params) { }
        }
        if (completed.isNotEmpty()) {
            purchasedOnPlay = true
        } else if (isFullList) {
            purchasedOnPlay = false
        }
        purchasedOnPlay?.let { purchased -> prefs?.edit { putBoolean(KEY_CACHED_PRO, purchased) } }
        updateIsPro()
    }

    private fun updateIsPro(cached: Boolean = prefs?.getBoolean(KEY_CACHED_PRO, false) ?: false) {
        _isPro.value = debugOverride ?: purchasedOnPlay ?: cached
    }
}
