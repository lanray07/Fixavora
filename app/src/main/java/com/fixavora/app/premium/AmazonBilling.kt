package com.fixavora.app.premium

import android.content.Context
import com.amazon.device.iap.*
import com.amazon.device.iap.model.*
import com.fixavora.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.HttpURLConnection
import java.net.URL

data class BillingState(val premium: Boolean = false, val price: String? = null, val message: String = "billing_unavailable", val busy: Boolean = false)

/** No debug unlock path. Grant only after the configured HTTPS service validates an Amazon receipt. */
class AmazonBilling(private val context: Context) : PurchasingListener {
    companion object { const val SKU = "fixavora.premium.lifetime" }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = context.getSharedPreferences("verified_entitlement", Context.MODE_PRIVATE)
    val state = MutableStateFlow(BillingState(premium = cache.getBoolean("active", false)))
    private var userId: String? = null
    private var registered = false
    fun connect() {
        if (registered) return
        // Missing public key or service is a configuration blocker, never a simulated success.
        if (BuildConfig.RECEIPT_ENDPOINT.isBlank() || !context.assets.list("").orEmpty().contains("AppstoreAuthenticationKey.pem")) return
        runCatching {
            PurchasingService.registerListener(context.applicationContext, this)
            registered = true
            PurchasingService.getUserData()
            PurchasingService.getProductData(setOf(SKU))
        }.onFailure { state.value = state.value.copy(message = "billing_unavailable", busy = false) }
    }
    fun buy() {
        if (!registered || state.value.price == null) return
        runCatching { state.value = state.value.copy(busy = true); PurchasingService.purchase(SKU) }.onFailure { fail() }
    }
    fun restore() {
        if (!registered) { state.value = state.value.copy(message = "billing_unavailable"); return }
        runCatching { state.value = state.value.copy(busy = true); PurchasingService.getUserData(); PurchasingService.getPurchaseUpdates(true) }.onFailure { fail() }
    }
    private fun fail() { state.value = state.value.copy(message = "purchase_failed", busy = false) }
    override fun onUserDataResponse(response: UserDataResponse) {
        if (response.requestStatus == UserDataResponse.RequestStatus.SUCCESSFUL) {
            userId = response.userData.userId
            if (cache.getString("user", null) != userId) { cache.edit().clear().apply(); state.value = state.value.copy(premium = false) }
        }
    }
    override fun onProductDataResponse(response: ProductDataResponse) {
        val product = response.productData?.get(SKU)
        state.value = state.value.copy(price = product?.price, message = if (product != null) "purchase_ready" else "billing_unavailable")
    }
    override fun onPurchaseResponse(response: PurchaseResponse) {
        if (response.requestStatus == PurchaseResponse.RequestStatus.SUCCESSFUL) verify(response.receipt, response.userData.userId)
        else if (response.requestStatus == PurchaseResponse.RequestStatus.ALREADY_PURCHASED) restore() else fail()
    }
    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        if (response.requestStatus != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) { fail(); return }
        response.receipts.filter { it.sku == SKU }.forEach { verify(it, response.userData.userId) }
        if (response.hasMore()) PurchasingService.getPurchaseUpdates(false)
        else state.value = state.value.copy(busy = false, message = "restore_checked")
    }
    private fun verify(receipt: Receipt, user: String) {
        if (receipt.sku != SKU || receipt.productType != ProductType.ENTITLED) { fail(); return }
        scope.launch {
            try {
                val url = URL(BuildConfig.RECEIPT_ENDPOINT)
                require(url.protocol == "https")
                val conn = (url.openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 15000; readTimeout = 15000; doOutput = true; instanceFollowRedirects = false; setRequestProperty("Content-Type", "application/json") }
                val active = try {
                    conn.outputStream.use { it.write(Gson().toJson(mapOf("receiptId" to receipt.receiptId, "userId" to user)).toByteArray()) }
                    require(conn.responseCode == 200)
                    val result = conn.inputStream.bufferedReader().use { Gson().fromJson(it, Map::class.java) }
                    require(result["sku"] == SKU && result["receiptId"] == receipt.receiptId && result["userId"] == user)
                    result["active"] == true && !receipt.isCanceled
                } finally { conn.disconnect() }
                cache.edit().putBoolean("active", active).putString("user", user).apply()
                state.value = state.value.copy(premium = active, busy = false, message = if (active) "premium_active" else "purchase_inactive")
                if (active) PurchasingService.notifyFulfillment(receipt.receiptId, FulfillmentResult.FULFILLED)
            } catch (_: Exception) { state.value = state.value.copy(busy = false, message = "verification_pending") }
        }
    }
    fun close() { scope.cancel() }
}
