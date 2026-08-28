# سهام‌یاب (SahamScraper)

برنامه اندروید برای استخراج قیمت سهام عدالت از سایت isignal.ir و ارسال به تلگرام از طریق Cloudflare Worker.

## ویژگی‌ها

- 🔍 استخراج قیمت‌ها با WebView و JavaScript
- ⏱️ اجرای دوره‌ای با WorkManager (قابل تنظیم ۱ تا ۷۲ ساعت)
- 🚀 اجرای فوری با یک دکمه
- 📶 ذخیره آفلاین و ارسال مجدد پس از اتصال به اینترنت
- 📤 ارسال به Worker و سپس تلگرام

## نحوه Build

```bash
./gradlew assembleDebug
