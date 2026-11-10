# Fixavora

Native Kotlin and Jetpack Compose home maintenance app for Amazon Fire tablets and compatible Android devices. Version 1.0.0, minimum API 26, target API 35. Current release evidence and remaining gates are in [RELEASE_READINESS.md](deliverables/reports/RELEASE_READINESS.md).

## Build

Install JDK 17 and Android SDK platform 35/build tools. Set `ANDROID_HOME` or `sdk.dir` in an untracked `local.properties`, then run:

```sh
./gradlew testDebugUnitTest assembleDebug assembleRelease assembleDebugAndroidTest lintDebug
python tools/localize.py validate
python tools/store_metadata.py
python -m unittest discover -s server -v
node --test server/cloudflare/worker.test.mjs
```

On Windows use `gradlew.bat`. Install `app/build/outputs/apk/debug/app-debug.apk` with ADB. The release artifact is `app/build/outputs/apk/release/app-release-unsigned.apk`, intended for Amazon's signing/upload route. It is not a directly installable publisher-signed APK. Keep a publisher-controlled signing key outside Git for any separate distribution.

## Application

Create a property and rooms, schedule recurring maintenance, complete/undo/snooze tasks, track repairs and appliances, keep warranties and contractor contacts, record expenses and appointments, and attach read-only external documents. Reports preview real records; Premium adds PDF/CSV exports and encrypted record backups. Restore validates the entire backup before replacing records transactionally. External files and their attachment links are excluded from backups and must be retained separately and relinked.

Room and DataStore keep home data on the device. WorkManager provides periodic opt-in reminders with quiet hours. The local assistant prepares drafts for review and summaries of saved records. It never silently saves. Speech input and output depend on the installed engines; typing remains available. No remote AI, analytics, advertising or cloud sync is connected.

## Premium and infrastructure

`fixavora.premium.lifetime` is a one-time Amazon entitlement, configured at a GBP 4.99 base price with marketplace conversions. The store supplies the displayed local price. App and IAP drafts remain separate from publishing approval.

The app-specific **public** authentication key is intentionally versioned in `app/src/main/assets/`. The RVS shared secret is held only in encrypted Cloudflare Worker secrets. Production uses `https://fixavora-receipts.lanraybanks.workers.dev/verify`; debug uses the separate sandbox Worker. These URLs are fixed by build type to prevent an environment override from shipping a sandbox endpoint.

The verifier runs on Workers Free with rate limiting and application logs disabled. See [server/cloudflare/README.md](server/cloudflare/README.md). Core records require no server. Offline entitlement caching is limited to seven days and requires a confirmed matching Amazon account. Existing records remain accessible when Premium verification is unavailable.

## Languages and screenshots

There are 22 complete interface catalogs: en-GB, en-US, en-AU, es-ES, es-419, fr-FR, fr-CA, de-DE, it-IT, pt-BR, pt-PT, nl-NL, pl-PL, sv-SE, ja-JP, ko-KR, zh-Hans, zh-Hant, hi-IN, ar, tr-TR and id-ID. System detection, manual selection, English fallback, native plural resources and Arabic RTL are implemented. All language resources stay in a bundle; on-demand language splitting is disabled.

`localization/` is the canonical UI source. Run `python tools/localize.py generate` after editing it; validation checks completeness, placeholders, source keys and generated Android resource consistency. AI translation and terminology corrections are recorded separately from human review. No human linguistic review is claimed.

`localization/store/` contains 15 canonical Amazon descriptions. `python tools/store_metadata.py` validates and exports them. The console currently offers 11 image-localization slots; additional localized assets remain available in the delivery folders.

Screenshot tests use real screens and clearly fictional records in the debug installation. Run `python tools/prepare_capture_assets.py`, build/install the test APK, then `python tools/capture_locales.py --serial SERIAL --locales en-GB fr-FR` (add `--compact --batch` for a compact batch). This replaces Fixavora's disposable debug data. Wi-Fi, rotation and display size are restored. Compact captures on a resized Fire tablet are not physical-phone test evidence. Native Android composition preserves Arabic, Indic and CJK shaping and exports PNG/SVG assets.

## Verification and limits

Unit tests cover recurrence/DST, currencies and localized digits, draft routing, backup cryptography, cache boundaries and quiet hours. Device tests cover database integrity/migration, module forms, actual file pickers and recreation, deletion, reminder delivery, installed voice capabilities and localized journeys. `tools/sandbox_qa.py` preserves the existing App Tester SKU configuration and SDK mode while exercising real Amazon test receipts through the live sandbox verifier.

The tested Fire has no compatible SpeechRecognizer service. Three installed English TTS variants synthesize successfully; other installed voices are unavailable. Natural-language parsing is limited and every draft needs review. Reports keep currencies separate; appointments use local dates/times. Android's private storage is used; the whole database is not separately encrypted. There is no app lock, remote AI adapter or exact-time alarm guarantee. [Privacy policy](docs/PRIVACY_POLICY.md), [architecture](docs/ARCHITECTURE.md), and delivery reports describe the boundaries and evidence.
