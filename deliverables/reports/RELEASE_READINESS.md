# Fixavora submission readiness

10 November 2026. Candidate: **1.0.0, version code 2**. The Amazon application draft is complete and its **Submit App** button is enabled. No app or IAP has been submitted or published. Final source and CI evidence are recorded in `deliverables/qa/source-verification.json`.

## Release artifacts

- Source: https://github.com/lanray07/Fixavora, branch `main`.
- Amazon upload: `deliverables/android/release/Fixavora-1.0.0-release-unsigned.apk`.
- Installable debug test build: `deliverables/android/debug/Fixavora-1.0.0-debug.apk`.
- Release SHA-256: `c9582dcc96a4a42168eb6c85dcd866282f384a7b774e17490377210c4c90fbc3`.
- APK inspection, version, SDK and endpoint checks: `android/build-logs/release-artifact-verification.json`.
- Exact artifact hashes and file inventory: `android/artifact-hashes.json`, `DELIVERABLES_MANIFEST.csv`. APK binaries remain local/CI artifacts and are not committed to Git.

## Completed checks

| Gate | Result | Evidence |
| --- | --- | --- |
| Build | PASS | Debug, unsigned Amazon release and instrumentation APKs built with JDK 17, minimum API 26, target 35. |
| JVM tests | PASS: 21 | Recurrence/DST, draft routing, exact localized money, currency precision, backup cryptography/passphrase session, quiet hours and entitlement cache boundaries. XML results under `android/build-logs/unit-test-results/`. |
| Android lint | PASS: 0 errors | 337 maintenance warnings reviewed in `qa/lint-review.json`; dynamic localized resource references account for most unused-resource warnings. No missing-translation suppression/baseline. |
| Receipt service | PASS: 13 automated tests | Eight deployed-worker implementation tests plus five Python reference tests; SKU/receipt matching, cancellation, errors, redirects and malformed input. Production and sandbox health both report configured. |
| Device workflows | PASS: 14 tests | Real App Tester receipt verification/reconnect/restore plus 13 tests covering all nine module forms, deletion, reminders, voice capability, database integrity/migration, actual document pickers, backup restore and PDF/CSV. Final log: `android/build-logs/final-device-workflows.log`. |
| Localized journeys | PASS | 24 tablet and 24 compact locale journeys, including two extra regional store captures. Compact is a resized Fire tablet viewport, not a physical phone. |
| Localization | PASS: AI review | 22 full catalogs, 283 keys each, placeholder/resource consistency and native plurals. Immutable catalog/capture hashes in `localization/review.json` and screenshot manifests. Human linguistic review NOT PERFORMED, as authorized by the publisher. |
| Screenshots | PASS | 384 actual localized compositions, eight screens × 24 locales × two sizes; matching raw captures and SVG sources. Native shaping for Arabic/Indic/CJK. Japanese/Korean money displays preserve GBP decimals. |
| Large text/theme | PASS: scoped | German, Arabic and Japanese compact journeys at font scale 1.3 in dark/high contrast; AI visual samples reviewed. See `qa/layout-stress/`. |
| Privacy/security | Scoped assessment complete | Encrypted portable backups, private storage, deletion/grants, fixed HTTPS verifier endpoints, no production secrets in client/Git, provider metadata disclosed. See `SECURITY_AND_PRIVACY.md`. |

## Amazon draft configuration

App ID `amzn1.devportal.mobileapp.11ee49e6c8e94500b8800d17b794aa02`; release ID `amzn1.devportal.apprelease.5c4b51dc4fd749ddac050c9949d1be55`.

The final version-code-2 APK is the only upload. Twelve compatible Fire tablet models are selected; Fire TV and automotive are excluded. Availability is all countries/regions Amazon offers for those devices. The audience is 18+. Amazon's API warning concerns excluded/incompatible devices; it does not prevent this selected-device submission. The shared To-Do Lists & Reminders subcategory maps to Business, Productivity and Lifestyle in the console review.

Fifteen localized descriptions and all eleven offered image locales are populated. Each image locale has eight actual localized UI captures plus original brand icons/promotion. Reviewer instructions explain the free limits, purchase/restore, voice fallback, file picker, backup scope and local deletion. Public support is banksmi@mail.com; policy/support links use the public GitHub repository. Privacy labels conservatively cover local record inputs, purchase identifiers and verification-network identifiers, with functionality/security purposes. No advertising/analytics use is declared. Export certification was checked under the publisher's explicit instruction.

Premium SKU `fixavora.premium.lifetime` is a one-time entitlement with GBP 4.99 base pricing and Amazon marketplace conversions. Its draft descriptions/icons are localized in the offered IAP languages. The shared secret is held in encrypted Cloudflare secrets; production and sandbox use distinct Workers on the Free plan. No paid service was enabled. The IAP remains a separate draft; submitting the application does not itself publish the product.

## Verified environment and limits

One physical Amazon KFTRWI (Fire HD 10, 2021), Fire OS 7 / API 28, 1200×1920, density 240. System display size, font scale and stay-awake setting were restored after testing. No emulator, physical Android phone, second Amazon account or other Fire hardware is certified by these tests.

The Fire has no installed compatible speech recognizer. Text input works as the fallback. en-GB, en-US and en-AU TTS synthesized real audio; other 19 voices were unavailable. Acoustic recognition, screen-reader speech and voice accuracy across languages are NOT TESTED. These capabilities are disclosed conditionally in the app and store copy.

App Tester and a live sandbox RVS request have passed; the repeat final run restored an existing genuine test receipt. Production charging, Amazon-signed distribution, refund with a second account and LAT are NOT TESTED. LAT/Amazon-signed testing is recommended additional assurance; it is not being misrepresented as a completed test or as a requirement of the completed App Tester sandbox gate.

The local database uses Android private storage and is not separately encrypted. There is no app lock, cloud sync, remote-model adapter, product analytics or advertising. Reports keep currencies separate; appointments use local times; reminders are periodic and subject to power settings. Backups contain records/history and exclude external files/photos/attachment links. The local assistant is limited and always needs user review. Whole-device security, a penetration test, human linguistic certification and legal/name clearance are not claimed.

## Remaining release action

The publisher must explicitly authorize final app and IAP submission. Until then both drafts stay unpublished. After Amazon signs and distributes a candidate, run the documented signed-build purchase/restore smoke check before claiming production payment verification. Setup, architecture, environment templates, voice, privacy and release instructions are included in README, `docs/` and the delivery reports.
