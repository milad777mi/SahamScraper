package com.example.sahamscraper.repository

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.sahamscraper.api.WorkerApi
import com.example.sahamscraper.data.Prices
import com.example.sahamscraper.utils.PreferencesManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class SahamRepository(private val context: Context) {

    private val prefs = PreferencesManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val api: WorkerApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dawn-shape-6e4d.mhmdkwarkw.workers.dev/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WorkerApi::class.java)
    }

    suspend fun extractPricesWithWebView(): Prices = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<Prices>()

        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36"
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript(
                        """
                        (function() {
                            var text = document.body.innerText;
                            var regex = /(\d{1,3}(?:,\d{3})*|\d+)\s*ریال/g;
                            var matches = [];
                            var match;
                            while ((match = regex.exec(text)) !== null) {
                                matches.push(match[1]);
                                if (matches.length === 3) break;
                            }
                            return JSON.stringify({
                                price490: matches[0] || 'نامشخص',
                                price532: matches[1] || 'نامشخص',
                                price1000: matches[2] || 'نامشخص'
                            });
                        })();
                        """.trimIndent()
                    ) { result ->
                        try {
                            val json = JSONObject(result.trim())
                            val prices = Prices(
                                price490 = json.getString("price490"),
                                price532 = json.getString("price532"),
                                price1000 = json.getString("price1000")
                            )
                            deferred.complete(prices)
                        } catch (e: Exception) {
                            deferred.complete(Prices("نامشخص", "نامشخص", "نامشخص"))
                        }
                    }
                }
            }
            loadUrl("https://isignal.ir/saham-edalat/")
        }

        deferred.await()
    }

    suspend fun fetchAndSendPrices(): String {
        return withContext(Dispatchers.IO) {
            try {
                val prices = extractPricesWithWebView()
                val response = api.sendPrices(prices)
                if (response.isSuccessful) {
                    prefs.clearPending()
                    "✅ موفق: ${response.body() ?: "ارسال شد"}"
                } else {
                    "❌ خطای سرور: ${response.code()}"
                }
            } catch (e: Exception) {
                val prices = extractPricesWithWebView()
                prefs.savePendingPrices(
                    prices.price490,
                    prices.price532,
                    prices.price1000
                )
                "⚠️ آفلاین: داده ذخیره شد - ${e.message}"
            }
        }
    }

    fun sendPendingPrices(): String {
        val pending = prefs.getPendingPrices()
        return if (pending != null) {
            "📤 ارسال داده‌های آفلاین: ${pending.first}, ${pending.second}, ${pending.third}"
        } else {
            "✅ داده‌ای برای ارسال نیست"
        }
    }
}
