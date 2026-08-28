package com.example.sahamscraper.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sahamscraper.repository.SahamRepository
import com.example.sahamscraper.utils.NetworkUtils

class SahamWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = SahamRepository(applicationContext)

            if (NetworkUtils.isNetworkAvailable(applicationContext)) {
                val result = repo.fetchAndSendPrices()
                println("✅ $result")
                Result.success()
            } else {
                val prices = repo.extractPricesWithWebView()
                // داده در PreferencesManager ذخیره میشه
                Result.retry()
            }
        } catch (e: Exception) {
            println("❌ خطا: ${e.message}")
            Result.retry()
        }
    }
}
