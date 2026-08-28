package com.example.sahamscraper.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.sahamscraper.data.Prices
import com.example.sahamscraper.utils.PreferencesManager
import com.example.sahamscraper.webview.JsInterface
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface WorkerApi {
    @POST("/")
    suspend fun sendPrices(@Body prices: Prices): retrofit2.Response<String>
}

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
        val jsInterface = JsInterface(deferred)

        try {
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36"
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.domStorageEnabled = true

                addJavascriptInterface(jsInterface, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // ✅ تاخیر افزایش یافته: 3s → 5s
                        Handler(Looper.getMainLooper()).postDelayed({
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    try {
                                        var text = document.body.innerText;
                                        var regex = /(\d{1,3}(?:,\d{3})*|\d+)\s*ریال/g;
                                        var matches = [];
                                        var match;
                                        while ((match = regex.exec(text)) !== null) {
                                            matches.push(match[1]);
                                            if (matches.length === 3) break;
                                        }
                                        if (matches.length < 3) {
                                            var allText = document.documentElement.innerText;
                                            var regex2 = /(\d{1,3}(?:,\d{3})*|\d+)\s*ریال/g;
                                            var m;
                                            while ((m = regex2.exec(allText)) !== null) {
                                                if (!matches.includes(m[1])) {
                                                    matches.push(m[1]);
                                                }
                                                if (matches.length === 3) break;
                                            }
                                        }
                                        Android.sendPrices(
                                            matches[0] || '',
                                            matches[1] || '',
                                            matches[2] || ''
                                        );
                                    } catch(e) {
                                        Android.sendPrices('', '', '');
                                    }
                                })();
                                """.trimIndent()
                            ) { result ->
                                // اسکریپت اجرا شد
                            }
                        }, 5000) // 🔥 5 ثانیه (قبلاً 3 ثانیه)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        if (newProgress == 100 && !deferred.isCompleted) {
                            // ✅ تاخیر افزایش یافته: 2s → 3s
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (!deferred.isCompleted) {
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            try {
                                                var text = document.body.innerText;
                                                var regex = /(\d{1,3}(?:,\d{3})*|\d+)\s*ریال/g;
                                                var matches = [];
                                                var match;
                                                while ((match = regex.exec(text)) !== null) {
                                                    matches.push(match[1]);
                                                    if (matches.length === 3) break;
                                                }
                                                if (matches.length < 3) {
                                                    var allText = document.documentElement.innerText;
                                                    var regex2 = /(\d{1,3}(?:,\d{3})*|\d+)\s*ریال/g;
                                                    var m;
                                                    while ((m = regex2.exec(allText)) !== null) {
                                                        if (!matches.includes(m[1])) {
                                                            matches.push(m[1]);
                                                        }
                                                        if (matches.length === 3) break;
                                                    }
                                                }
                                                Android.sendPrices(
                                                    matches[0] || '',
                                                    matches[1] || '',
                                                    matches[2] || ''
                                                );
                                            } catch(e) {
                                                Android.sendPrices('', '', '');
                                            }
                                        })();
                                        """.trimIndent()
                                    ) { result -> }
                                }
                            }, 3000) // 🔥 3 ثانیه (قبلاً 2 ثانیه)
                        }
                    }
                }

                loadUrl("https://isignal.ir/saham-edalat/")
            }

            // ✅ تایم‌اوت نهایی افزایش یافته: 25s → 40s
            Handler(Looper.getMainLooper()).postDelayed({
                if (!deferred.isCompleted) {
                    deferred.complete(Prices("نامشخص", "نامشخص", "نامشخص"))
                }
            }, 40000) // 🔥 40 ثانیه (قبلاً 25 ثانیه)

            deferred.await()
        } catch (e: Exception) {
            deferred.complete(Prices("نامشخص", "نامشخص", "نامشخص"))
            deferred.await()
        }
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
