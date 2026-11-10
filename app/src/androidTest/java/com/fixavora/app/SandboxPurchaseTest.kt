package com.fixavora.app

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.amazon.device.iap.PurchasingService
import com.fixavora.app.premium.AmazonBilling
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.regex.Pattern

/** Real Amazon App Tester -> HTTPS sandbox verifier -> real app billing state.
 * Refuses to touch a purchase dialog unless both SDK and endpoint are sandbox. */
class SandboxPurchaseTest {
    @get:Rule val compose = createAndroidComposeRule<EditorTestActivity>()
    @Test fun purchaseIsVerifiedAndRestoresWithoutClientUnlock() {
        assertTrue("Only a debug APK can run this test",BuildConfig.DEBUG)
        assertEquals("https://fixavora-receipts-sandbox.lanraybanks.workers.dev/verify",BuildConfig.RECEIPT_ENDPOINT)
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val device=UiDevice.getInstance(instrumentation)
        lateinit var billing: AmazonBilling
        var purchasedThisRun=false
        compose.setContent { Text("Fixavora sandbox purchase QA") }
        compose.runOnIdle {
            context.getSharedPreferences("verified_entitlement_debug",0).edit().clear().commit()
            billing=AmazonBilling(context); billing.connect()
        }
        try {
            val descriptor = instrumentation.uiAutomation.executeShellCommand("getprop debug.amazon.sandboxmode")
            val mode = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText().trim() }
            assertEquals("App Tester sandbox mode is required", "debug", mode)
            compose.waitUntil(60000) { billing.state.value.accountReady && billing.state.value.price != null && !billing.state.value.busy }
            if (!billing.state.value.premium) {
                compose.runOnIdle { billing.buy() }
                val confirm=By.res("com.amazon.sdktestclient", "buy_button").text("Get Item")
                val button=device.wait(Until.findObject(confirm),15000)
                if (button == null) {
                    device.dumpWindowHierarchy(File(context.getExternalFilesDir(null),"sandbox-purchase-dialog.xml"))
                    assertTrue("No recognized Amazon sandbox confirmation; no external button was clicked",billing.state.value.premium)
                } else { button.click(); purchasedThisRun=true }
                compose.waitUntil(60000) { billing.state.value.premium && !billing.state.value.busy }
            }
            assertTrue("Only server-verified access can pass",billing.state.value.premium)
            compose.runOnIdle { billing.close(); billing=AmazonBilling(context); billing.connect() }
            compose.waitUntil(60000) { billing.state.value.premium && !billing.state.value.busy }
            compose.runOnIdle { billing.restore() }
            compose.waitUntil(60000) { !billing.state.value.busy && billing.state.value.message=="premium_active" }
            val purchaseResult=if(purchasedThisRun) "CHECKOUT_AND_VERIFICATION_PASS" else "EXISTING_TEST_RECEIPT_VERIFIED"
            File(context.getExternalFilesDir(null),"sandbox-purchase-result.json").writeText("""{"purchase":"$purchaseResult","reconnect":"PASS","restore":"PASS","source":"Amazon App Tester and live RVS Cloud Sandbox","production_purchase":"NOT TESTED"}""")
        } finally {
            device.findObject(By.res("com.amazon.sdktestclient", "cancel_button"))?.click()
            compose.runOnIdle { billing.close() }
        }
    }
}
