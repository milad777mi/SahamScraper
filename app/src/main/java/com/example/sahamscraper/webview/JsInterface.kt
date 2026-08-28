package com.example.sahamscraper.webview

import android.webkit.JavascriptInterface
import com.example.sahamscraper.data.Prices
import kotlinx.coroutines.CompletableDeferred

class JsInterface(private val deferred: CompletableDeferred<Prices>) {

    @JavascriptInterface
    fun sendPrices(price490: String, price532: String, price1000: String) {
        if (!deferred.isCompleted) {
            val prices = Prices(
                price490 = price490.ifEmpty { "نامشخص" },
                price532 = price532.ifEmpty { "نامشخص" },
                price1000 = price1000.ifEmpty { "نامشخص" }
            )
            deferred.complete(prices)
        }
    }
}
