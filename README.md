# Fixavora

Native, offline-first home maintenance and repair organiser for Amazon Fire tablets, written in Kotlin and Jetpack Compose. Source project: `app/`. This is a development delivery, **not an Appstore-ready release**. See `deliverables/reports/RELEASE_READINESS.md` for evidence and outstanding work.

## Build and install

Requirements: JDK 17, Android SDK platform 35 and build tools, an Android API 26+ device. The Gradle wrapper is included. No Google Play Services account is required.

1. Clone this repository.
2. Point `local.properties` at your SDK (`sdk.dir=C:/Users/YourName/AppData/Local/Android/Sdk` on Windows), or set `ANDROID_HOME`.
3. Run `./gradlew assembleDebug assembleRelease testDebugUnitTest assembleDebugAndroidTest` (Windows: `.\gradlew.bat`).
4. Install `app/build/outputs/apk/debug/app-debug.apk` using `adb install -r`.

The release build produces `app/build/outputs/apk/release/app-release-unsigned.apk`. It is deliberately unsigned; do not distribute it as an installable publisher-signed release. Amazon documents applying its own signature to signed or unsigned uploads; verify the resulting Amazon-signed candidate through LAT. For separate publisher-signed distribution, create and securely retain your own key, align/sign using Android tools and verify the signature. No signing key or production secret is committed.

Local APK copies and SHA-256 hashes are collected under `deliverables/android/` after validation. APKs are ignored by Git to avoid committing binaries; the workflow uploads build artifacts. Check its result, because localisation errors currently prevent full lint from passing.

## App workflow

Choose language, household role, currency and measurement preference during optional onboarding. Create a property. Open the property to add rooms, edit/archive it, attach photos, or open linked records. Use the module navigation to add maintenance tasks, repairs and appliances. Tasks support recurrence, completion, immediate undo, pause, reminders and snooze. Open a record to view history and attach read-only documents using the Android picker.

Premium-gated additions include warranties, contractor directory, expense tracking, appointments and additional properties. Existing records can still be opened and edited if entitlement becomes unavailable. The assistant prepares local task drafts for review; it never silently saves. Voice input depends on the installed recogniser; text input remains available.

Settings includes notification opt-in, quiet hours, appearance, spoken feedback, currency, language, data deletion and portable encrypted record backups. Reports preview saved data for the current property with dates and record-type filters. PDF, CSV and text exports require an explicit confirmation and Premium. Backup restore is available for recovery without a new purchase; restoring replaces current records transactionally. External attachments are not bundled in backups.

## Configuration

`.env.example` documents optional development/server configuration. The app does not automatically read a `.env` file or package it into the APK.

Amazon IAP uses SKU `fixavora.premium.lifetime` (entitlement, one-time purchase). Before enabling purchases:

- Register the actual package/SKU in your Amazon developer account and set marketplace prices; £4.99 is only the requested price hypothesis.
- Supply your app's `AppstoreAuthenticationKey.pem` in `app/src/main/assets/` (ignored by Git).
- Host `server/receipt_service.py` behind HTTPS with global rate limits and server-side `AMAZON_SHARED_SECRET`.
- Set `FIXAVORA_RECEIPT_ENDPOINT` in the Gradle build environment to the service's HTTPS `/verify` URL, then rebuild.
- Test purchases, cancellations, restore, account changes, revocation, verification failures and offline entitlement behaviour with Amazon App Tester/LAT. No production purchase has been simulated.

Remote AI is not configured or connected. The assistant uses deterministic local organisation and record summaries; no private records are sent to an AI provider. This build does not include a remote-model adapter or paid inference allowance.

## Localisation and assets

`localization/en-GB.json` is the base catalog. All 19 requested locale tags are selectable, with English fallback. Three English variants have complete base-string coverage; the other 16 have draft navigation translations, not complete UI translations or review. Do not advertise them as fully supported. `deliverables/localization/coverage.json` enumerates every missing key.

Run `python tools/localize.py generate` and `python tools/localize.py validate`. To fill missing strings through your own translation gateway, configure `TRANSLATION_ENDPOINT` and `TRANSLATION_API_KEY`, then run `python tools/localize.py translate --locale fr-FR` for each locale. The endpoint receives public UI strings only. Review terminology, placeholders, plural forms, RTL, text expansion, privacy copy and screenshot copy before setting review status to PASS. Validation currently reports zero approved locales.

`python tools/brand_assets.py` generates original editable SVG artwork and store icon exports. Python Pillow is needed (`python -m pip install -r tools/requirements.txt`). `python tools/store_metadata.py` generates 13 draft listing localisations, separate from UI locale support. No app or store listing is published.

Device tests capture the actual interface with fictional fixtures. Debug-only fixed-orientation hosts inherit MainActivity and its real screens/data flows; they are not packaged in release. `python tools/fire_qa.py --serial SERIAL` installs only Fixavora packages, runs database/voice/UI tests, temporarily disables Wi-Fi for the core journey, and restores Wi-Fi. `--compact` additionally simulates a compact viewport on the connected tablet and restores display size. It is not a physical phone test. `python tools/compose_screenshots.py` consumes those real captures to generate image compositions and editable templates. Screenshots in other locales are blocked until UI translations and marketing copy are complete.

## Tests and operational limits

- JVM: recurrence anchors, leap years, DST, currencies, local proposals, command routing, duplicate detection, encrypted backups and CSV safety.
- Instrumentation: relational integrity, free limits, transactional restore, migration, database reopen, PDF/CSV generation, device voice capabilities and an actual create-task journey.
- `python -m unittest discover -s server -v` checks receipt response evaluation without contacting Amazon.
- `./gradlew lintDebug` currently fails on incomplete translations. This release check has not been weakened or baselined away.

There are no analytics, advertising, cloud sync or background AI uploads. Core storage uses Android's private application sandbox; it is not SQLCipher-encrypted. Portable record backups are encrypted and tested separately. Monitoring is limited to Android's platform tools and CI. See `docs/ARCHITECTURE.md`, `docs/PRIVACY_POLICY_DRAFT.md`, `docs/RESEARCH.md` and the deliverable reports for exact scope, privacy dependencies and known limitations.
