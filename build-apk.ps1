$ErrorActionPreference = 'Stop'
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Node.js نصب نیست. ابتدا Node.js LTS را نصب کن.' }
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { throw 'npm در دسترس نیست.' }
Write-Host 'مرحله ۱ از ۵: نصب وابستگی‌ها' -ForegroundColor Cyan
npm install
Write-Host 'مرحله ۲ از ۵: ساخت پل ارتباطی افزونه‌های اندروید' -ForegroundColor Cyan
npm run build
if (-not (Test-Path '.\android')) {
  Write-Host 'مرحله ۳ از ۵: ساخت پروژه Android' -ForegroundColor Cyan
  npx cap add android
}
Write-Host 'مرحله ۴ از ۵: آماده‌سازی انتخاب مسیر پشتیبان و همگام‌سازی' -ForegroundColor Cyan
node .\scripts\setup-android.mjs
npx capacitor-assets generate --android
npx cap sync android
Write-Host 'مرحله ۵ از ۵: ساخت APK دیباگ' -ForegroundColor Cyan
Push-Location '.\android'
try { .\gradlew.bat assembleDebug } finally { Pop-Location }
$apk = Join-Path (Get-Location) 'android\app\build\outputs\apk\debug\app-debug.apk'
if (Test-Path $apk) { Write-Host "APK ساخته شد: $apk" -ForegroundColor Green } else { throw 'ساخت تمام شد اما فایل APK پیدا نشد؛ خروجی Gradle را بررسی کن.' }
