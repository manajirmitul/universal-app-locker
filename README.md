# 🔒 Universal App Locker

Set a **daily time limit for any app** on your Android phone. When the limit is reached, the app is locked behind a PIN screen.

অ্যাপ বেছে দৈনিক সময়সীমা ঠিক করুন। সীমা শেষ হলে অ্যাপ PIN দিয়ে লক হয়ে যাবে।

- ✅ Free and open source (MIT)
- ✅ **No internet permission**: nothing leaves your phone, no ads, no tracking
- ✅ Android 8.0+ (API 26)

## Download / ডাউনলোড

Go to the **[Releases](../../releases/latest)** page and download the latest `Universal-App-Locker-*.apk`.

## Install / ইনস্টল

1. Download the APK on your phone and tap it.
2. If asked, allow **Install unknown apps** for your browser or file manager.
3. If Play Protect warns you, tap **More details → Install anyway** (the app is not on Play Store).
4. Updates install over the old version. No need to uninstall.

## First-time setup / প্রথমবার সেটআপ

1. Open the app and tap **Accessibility → open**, then turn on **Universal App Locker**.
   - If Android says **"Restricted setting"**: open **Settings → Apps → Universal App Locker → ⋮ (top right) → Allow restricted settings**, then try again.
2. Set a **PIN** (4 to 8 digits).
3. Set **Battery → Unrestricted** (important on Oppo, Xiaomi, Vivo, Realme, Samsung, otherwise the phone may stop the service).
4. Tap any app in the list and choose a daily limit.
5. Open Settings (⚙, top right) to change the language or switch between dark and light theme.

## Features

- Per-app daily limit (minutes), per-second accurate counting, nothing counted while the screen is off
- Lock screen with PIN pad; after unlocking choose **+5 / +15 / +30 minutes** or **unlock for today**
- The app itself is PIN-protected (limits can't be changed without the PIN)
- PIN is stored as a salted SHA-256 hash; 5 wrong tries = 30 s cooldown
- Optional **Usage Access** makes counting more accurate by including usage before the service started
- Dark and light theme, app search, progress bars
- **Languages:** English and বাংলা, choose it on first launch, change it anytime in Settings (⚙), or follow the phone language. To add a language, copy `app/src/main/res/values/strings.xml` to `values-xx/strings.xml`, translate it, and add the language to the list in `SettingsActivity` (and `MainActivity.buildWelcome()`)

## Troubleshooting (works on all brands, but some need extra settings)

Some phone makers (Xiaomi/Redmi/POCO, Oppo/Realme/OnePlus, Vivo/iQOO, Huawei/Honor, Samsung, Tecno/Infinix/itel) stop background apps aggressively. If locking does not work:

1. Turn **Accessibility** off and on again. Turn on **Allow restricted settings** first if Android blocks it.
2. Set battery to **Unrestricted** and turn **Autostart / Auto launch** ON for this app.
3. Lock the app card in the **recent apps** screen so it is not cleared.
4. In the app tap **Tips for your phone brand** for exact menu names. The home screen also shows a warning when the service is not running.

Requires Android 8.0 or newer.

## Limitations

This is a self-control tool, not a tamper-proof parental control. Someone who knows the phone's settings can turn off the Accessibility service or uninstall the app.

## Build from source

Requires JDK 17 and Gradle 8.11+.

```bash
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or fork the repo and use GitHub Actions (`.github/workflows`).

### Publishing a signed release (maintainer)

1. Add these repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
2. Create a new **Release** with a tag like `v1.0.0` and publish it.
3. The *Release APK* workflow builds the signed APK and attaches it to the release.

## Contributing

Issues and pull requests are welcome.

## License

[MIT](LICENSE)
