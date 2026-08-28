package com.example.sahamscraper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.sahamscraper.repository.SahamRepository
import com.example.sahamscraper.utils.NetworkUtils
import com.example.sahamscraper.utils.PreferencesManager
import com.example.sahamscraper.worker.SahamWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var statusText: MaterialTextView
    private lateinit var intervalText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "خطا در setContentView: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        try {
            prefs = PreferencesManager(this)

            statusText = findViewById(R.id.status_text)
            intervalText = findViewById(R.id.interval_text)

            val runNowBtn = findViewById<MaterialButton>(R.id.run_now_btn)
            val setIntervalBtn = findViewById<MaterialButton>(R.id.set_interval_btn)

            updateUI()

            runNowBtn.setOnClickListener { runNow() }
            setIntervalBtn.setOnClickListener { showIntervalDialog() }

            schedulePeriodic()
            checkPending()
            
            android.widget.Toast.makeText(this, "✅ برنامه با موفقیت اجرا شد!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "❌ خطا: ${e.message}\n${e.stackTraceToString().take(200)}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        try {
            val hours = prefs.intervalHours
            intervalText.text = "⏱️ زمان بین اجراها: $hours ساعت"
            val lastRun = prefs.lastRunTime
            if (lastRun > 0) {
                val date = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale("fa"))
                statusText.text = "🕒 آخرین اجرا: ${date.format(java.util.Date(lastRun))}"
            } else {
                statusText.text = "⏳ هنوز اجرا نشده"
            }
        } catch (e: Exception) {
            statusText.text = "⚠️ خطا: ${e.message}"
        }
    }

    private fun runNow() {
        try {
            val workRequest = OneTimeWorkRequestBuilder<SahamWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)
            statusText.text = "⏳ در حال اجرا..."

            WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id)
                .observe(this) { info ->
                    if (info != null && info.state.isFinished) {
                        statusText.text = if (info.state == WorkInfo.State.SUCCEEDED) {
                            "✅ اجرا موفق"
                        } else {
                            "❌ اجرا ناموفق"
                        }
                        updateUI()
                    }
                }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "خطا: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showIntervalDialog() {
        try {
            val input = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(prefs.intervalHours.toString())
            }

            android.app.AlertDialog.Builder(this)
                .setTitle("تنظیم زمان بین اجراها (ساعت)")
                .setView(input)
                .setPositiveButton("ذخیره") { _, _ ->
                    val value = input.text.toString().toIntOrNull() ?: 12
                    if (value in 1..72) {
                        prefs.intervalHours = value
                        schedulePeriodic()
                        updateUI()
                    } else {
                        android.widget.Toast.makeText(
                            this,
                            "لطفاً عددی بین ۱ تا ۷۲ وارد کن",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("لغو", null)
                .show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "خطا: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun schedulePeriodic() {
        try {
            val hours = prefs.intervalHours
            val workRequest = PeriodicWorkRequestBuilder<SahamWorker>(
                hours.toLong(),
                TimeUnit.HOURS
            )
                .addTag("saham_periodic")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    "saham_periodic",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "خطا در schedule: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPending() {
        try {
            if (NetworkUtils.isNetworkAvailable(this)) {
                val repo = SahamRepository(this)
                val result = repo.sendPendingPrices()
                if (result.contains("ارسال")) {
                    android.widget.Toast.makeText(
                        this,
                        result,
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "خطا در checkPending: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
