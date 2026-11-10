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

data class BillingState(val premium: Boolean = false, val price: String? = null, val message: String = "billing_unavailable", val busy: Boolean = false, val accountReady: Boolean = false)

/** No debug unlock path. Grant only after the configured HTTPS service validates an Amazon receipt. */
class AmazonBilling(private val context: Context) : PurchasingListener {
    companion object { const val SKU = "fixavora.premium.lifetime" }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cache = context.getSharedPreferences(if (BuildConfig.DEBUG) "verified_entitlement_debug" else "verified_entitlement", Context.MODE_PRIVATE)
    val state = MutableStateFlow(BillingState())
    private var userId: String? = null
    private var registered = false
    private var generation = 0L
    private var updateRequest: RequestId? = null
    private var purchaseRequest: RequestId? = null
    private val restoredReceipts = mutableListOf<Receipt>()
    private var timeout: Job? = null
    fun connect() {
        if (registered) {
            runCatching { PurchasingService.getUserData() }.onFailure { fail() }
            return
        }
        // Missing public key or service is a configuration blocker, never a simulated success.
        if (BuildConfig.RECEIPT_ENDPOINT.isBlank() || !context.assets.list("").orEmpty().contains("AppstoreAuthenticationKey.pem")) return
        runCatching {
            PurchasingService.registerListener(context.applicationContext, this)
            registered = true
            beginOperation()
            PurchasingService.getUserData()
            PurchasingService.getProductData(setOf(SKU))
        }.onFailure { state.value = state.value.copy(message = "billing_unavailable", busy = false) }
    }
    fun buy() {
        if (!registered || userId == null || state.value.price == null || state.value.busy || state.value.premium) return
        runCatching { beginOperation(); purchaseRequest = PurchasingService.purchase(SKU) }.onFailure { fail() }
    }
    fun restore() {
        if (!registered) { state.value = state.value.copy(message = "billing_unavailable"); return }
        if (state.value.busy) return
        runCatching { PurchasingService.getUserData() }.onFailure { fail() }
    }
    private fun beginOperation() {
        generation++
        val token = generation
        state.value = state.value.copy(busy = true)
        timeout?.cancel()
        timeout = scope.launch {
            delay(60_000)
            if (generation == token) {
                generation++; updateRequest = null; purchaseRequest = null; restoredReceipts.clear()
                state.value = state.value.copy(message = "verification_pending", busy = false)
            }
        }
    }
    private fun fail() {
        timeout?.cancel(); generation++; updateRequest = null; purchaseRequest = null; restoredReceipts.clear()
        state.value = state.value.copy(message = "purchase_failed", busy = false)
    }
    override fun onUserDataResponse(response: UserDataResponse) {
        if (response.requestStatus == UserDataResponse.RequestStatus.SUCCESSFUL) {
            val user = response.userData.userId
            if (userId != user) {
                generation++; timeout?.cancel(); updateRequest = null; purchaseRequest = null; restoredReceipts.clear()
                userId = user
                if (cache.getString("user", null) != user) cache.edit().clear().apply()
                state.value = state.value.copy(premium = EntitlementPolicy.cached(cache.getBoolean("active", false), cache.getString("user", null), user, cache.getLong("verifiedAt", 0), System.currentTimeMillis()), busy = false, accountReady = true)
            }
            if (!state.value.busy) runCatching {
                beginOperation(); restoredReceipts.clear(); updateRequest = PurchasingService.getPurchaseUpdates(true)
            }.onFailure { fail() }
        } else {
            userId = null; state.value = state.value.copy(premium = false, accountReady = false); fail()
        }
    }
    override fun onProductDataResponse(response: ProductDataResponse) {
        val product = response.productData?.get(SKU)?.takeIf { it.productType == ProductType.ENTITLED }
        state.value = state.value.copy(price = product?.price, message = if (product != null) "purchase_ready" else "billing_unavailable")
    }
    override fun onPurchaseResponse(response: PurchaseResponse) {
        if (response.requestId != purchaseRequest) return
        purchaseRequest = null
        if (response.requestStatus == PurchaseResponse.RequestStatus.SUCCESSFUL && response.userData.userId == userId) {
            verifyAll(listOf(response.receipt), response.userData.userId)
        } else if (response.requestStatus == PurchaseResponse.RequestStatus.ALREADY_PURCHASED) {
            timeout?.cancel(); state.value = state.value.copy(busy = false); restore()
        } else fail()
    }
    override fun onPurchaseUpdatesResponse(response: PurchaseUpdatesResponse) {
        if (response.requestId != updateRequest) return
        if (response.requestStatus != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) { fail(); return }
        if (response.userData.userId != userId) { fail(); return }
        restoredReceipts.addAll(response.receipts.filter { it.sku == SKU })
        if (response.hasMore()) runCatching { updateRequest = PurchasingService.getPurchaseUpdates(false) }.onFailure { fail() }
        else {
            updateRequest = null
            val receipts = restoredReceipts.toList(); restoredReceipts.clear()
            verifyAll(receipts, response.userData.userId)
        }
    }
    private fun verifyAll(receipts: List<Receipt>, user: String) {
        val token = generation
        scope.launch {
            try {
                // All pages form one authoritative snapshot, so an empty restore revokes the cache.
                // Verify every active receipt before changing access; a network failure is not a revocation.
                var active = false
                for (receipt in receipts) {
                    require(receipt.sku == SKU && receipt.productType == ProductType.ENTITLED)
                    if (!receipt.isCanceled && verifyReceipt(receipt, user)) active = true
                }
                if (token != generation || userId != user) return@launch
                timeout?.cancel()
                cache.edit().putBoolean("active", active).putString("user", user).putLong("verifiedAt", System.currentTimeMillis()).apply()
                state.value = state.value.copy(premium = active, busy = false, message = if (active) "premium_active" else "purchase_inactive")
                if (active) receipts.filter { !it.isCanceled }.forEach { PurchasingService.notifyFulfillment(it.receiptId, FulfillmentResult.FULFILLED) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (token == generation && userId == user) {
                    timeout?.cancel()
                    state.value = state.value.copy(premium = EntitlementPolicy.cached(cache.getBoolean("active", false), cache.getString("user", null), user, cache.getLong("verifiedAt", 0), System.currentTimeMillis()), busy = false, message = "verification_pending")
                }
            }
        }
    }
    private suspend fun verifyReceipt(receipt: Receipt, user: String): Boolean = withContext(Dispatchers.IO) {
                val url = URL(BuildConfig.RECEIPT_ENDPOINT)
                require(url.protocol == "https")
                val conn = (url.openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 15000; readTimeout = 15000; doOutput = true; instanceFollowRedirects = false; setRequestProperty("Content-Type", "application/json"); setRequestProperty("User-Agent", "Fixavora/1.0 (Android; com.fixavora.app)") }
                try {
                    conn.outputStream.use { it.write(Gson().toJson(mapOf("receiptId" to receipt.receiptId, "userId" to user)).toByteArray()) }
                    require(conn.responseCode == 200)
                    val result = conn.inputStream.bufferedReader().use { Gson().fromJson(it, Map::class.java) }
                    require(result["sku"] == SKU && result["receiptId"] == receipt.receiptId && result["userId"] == user)
                    result["active"] == true && !receipt.isCanceled
                } finally { conn.disconnect() }
    }
    fun close() { scope.cancel() }
}
