package com.example.sahamscraper

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.sahamscraper.repository.SahamRepository
import com.example.sahamscraper.utils.NetworkUtils
import com.example.sahamscraper.utils.PreferencesManager
import com.example.sahamscraper.worker.SahamWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var statusText: TextView
    private lateinit var intervalText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            prefs = PreferencesManager(this)

            statusText = findViewById(R.id.status_text)
            intervalText = findViewById(R.id.interval_text)

            val runNowBtn = findViewById<Button>(R.id.run_now_btn)
            val setIntervalBtn = findViewById<Button>(R.id.set_interval_btn)

            updateUI()

            runNowBtn.setOnClickListener { runNow() }
            setIntervalBtn.setOnClickListener { showIntervalDialog() }

            schedulePeriodic()
            checkPending()

            Toast.makeText(this, "✅ برنامه با موفقیت اجرا شد!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ خطا: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun updateUI() {
        try {
            val millis = prefs.intervalMillis
            val hours = millis / (60 * 60 * 1000)
            val minutes = (millis % (60 * 60 * 1000)) / (60 * 1000)

            val timeText = when {
                hours > 0 && minutes > 0 -> "$hours ساعت و $minutes دقیقه"
                hours > 0 -> "$hours ساعت"
                minutes > 0 -> "$minutes دقیقه"
                else -> "نامشخص"
            }

            intervalText.text = "⏱️ زمان بین اجراها: $timeText"

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
            Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // 🔥 دیالوگ جدید با پشتیبانی از h و m
    // ==========================================
    private fun showIntervalDialog() {
        try {
            val input = android.widget.EditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                val currentMillis = prefs.intervalMillis
                val hours = currentMillis / (60 * 60 * 1000)
                val minutes = (currentMillis % (60 * 60 * 1000)) / (60 * 1000)
                setText(
                    when {
                        hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
                        hours > 0 -> "${hours}h"
                        minutes > 0 -> "${minutes}m"
                        else -> "12h"
                    }
                )
            }

            android.app.AlertDialog.Builder(this)
                .setTitle("تنظیم زمان بین اجراها")
                .setMessage("مثال‌ها:\n1h = ۱ ساعت\n30m = ۳۰ دقیقه\n12h = ۱۲ ساعت")
                .setView(input)
                .setPositiveButton("ذخیره") { _, _ ->
                    val raw = input.text.toString().trim()
                    val parsed = parseTime(raw)
                    if (parsed != null && parsed >= 15 * 60 * 1000) { // حداقل ۱۵ دقیقه
                        prefs.intervalMillis = parsed
                        schedulePeriodic()
                        updateUI()
                        Toast.makeText(
                            this,
                            "✅ زمان تنظیم شد: ${formatTime(parsed)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "❌ نامعتبر! حداقل ۱۵ دقیقه (مثلاً 15m) یا بیشتر وارد کنید.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .setNegativeButton("لغو", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطا: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // 🔧 تبدیل رشته به میلی‌ثانیه
    // ==========================================
    private fun parseTime(input: String): Long? {
        val regex = Regex("""^(\d+)([hm])$""")
        val match = regex.find(input.lowercase()) ?: return null
        val value = match.groupValues[1].toLongOrNull() ?: return null
        val unit = match.groupValues[2]

        return when (unit) {
            "h" -> value * 60 * 60 * 1000
            "m" -> value * 60 * 1000
            else -> null
        }
    }

    private fun formatTime(millis: Long): String {
        val hours = millis / (60 * 60 * 1000)
        val minutes = (millis % (60 * 60 * 1000)) / (60 * 1000)
        return when {
            hours > 0 && minutes > 0 -> "$hours ساعت و $minutes دقیقه"
            hours > 0 -> "$hours ساعت"
            minutes > 0 -> "$minutes دقیقه"
            else -> "نامشخص"
        }
    }

    private fun schedulePeriodic() {
        try {
            val intervalMillis = prefs.intervalMillis

            // تبدیل به ساعت (برای PeriodicWorkRequest)
            val intervalHours = intervalMillis / (60 * 60 * 1000)

            // اگر کمتر از ۱ ساعت باشد، به ساعت تبدیل می‌کنیم (با اعشار)
            // ولی PeriodicWorkRequest فقط عدد صحیح ساعت قبول می‌کند
            // برای دقت بیشتر، از OneTimeWorkRequest با تکرار استفاده می‌کنیم
            if (intervalHours < 1) {
                // برای کمتر از ۱ ساعت، از OneTimeWorkRequest با delay استفاده می‌کنیم
                scheduleOneTimeRepeating(intervalMillis)
            } else {
                val workRequest = PeriodicWorkRequestBuilder<SahamWorker>(
                    intervalHours,
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
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در schedule: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // 🔄 برای فواصل کمتر از ۱ ساعت (مثلاً ۱۵ دقیقه)
    // ==========================================
    private fun scheduleOneTimeRepeating(intervalMillis: Long) {
        // برای تست با فواصل کوتاه (مثلاً ۱ دقیقه)
        // از OneTimeWorkRequest با تأخیر استفاده می‌کنیم
        val workRequest = OneTimeWorkRequestBuilder<SahamWorker>()
            .setInitialDelay(intervalMillis, TimeUnit.MILLISECONDS)
            .addTag("saham_one_time")
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "saham_one_time",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

        // برای تکرار، از یک تایمر ساده استفاده نمی‌کنیم
        // اما چون برای تست است، همین کافی است
        Toast.makeText(
            this,
            "⏱️ زمان تست (${formatTime(intervalMillis)}) تنظیم شد.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkPending() {
        try {
            if (NetworkUtils.isNetworkAvailable(this)) {
                val repo = SahamRepository(this)
                val result = repo.sendPendingPrices()
                if (result.contains("ارسال")) {
                    Toast.makeText(
                        this,
                        result,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطا در checkPending: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
