package com.ismaelcr.custodiaapp

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillingManager(
    private val activity: Activity,
    private val preferencesManager: PreferencesManager,
    private val onPurchaseSuccess: () -> Unit,
    private val onPurchaseError: (String) -> Unit
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private val productId = "premium_version"  // ID del producto en Google Play Console

    init {
        setupBillingClient()
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "✅ Conexión establecida con Google Play")
                    // Verificar compras existentes al iniciar
                    queryPurchases()
                } else {
                    Log.e("Billing", "❌ Error conexión: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("Billing", "⚠️ Desconectado. Reintentando...")
                // Reintentar conexión
                startConnection()
            }
        })
    }

    // Verificar si el usuario ya compró Premium
    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchasesList.forEach { purchase ->
                    if (purchase.products.contains(productId) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {

                        // Verificar compra en el servidor de Google
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            preferencesManager.setPremium(true)
                            onPurchaseSuccess()
                        }
                    }
                }
            }
        }
    }

    // Iniciar flujo de compra (2,49€)
    fun launchPurchaseFlow() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()) {

                val productDetails = productDetailsList[0]

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient?.launchBillingFlow(activity, flowParams)
            } else {
                onPurchaseError("No se pudo cargar el producto: ${billingResult.debugMessage}")
            }
        }
    }

    // Callback cuando se completa una compra
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    acknowledgePurchase(purchase)
                    preferencesManager.setPremium(true)
                    onPurchaseSuccess()
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d("Billing", "Usuario canceló la compra")
        } else {
            onPurchaseError("Error en la compra: ${billingResult.debugMessage}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            billingClient?.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d("Billing", "✅ Compra reconocida correctamente")
                }
            }
        }
    }

    fun endConnection() {
        billingClient?.endConnection()
    }
}
