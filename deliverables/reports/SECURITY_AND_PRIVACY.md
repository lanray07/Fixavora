# Security and privacy assessment

10 November 2026. Scope: changed source, build configuration, verifier tests, APK inspection and the physical KFTRWI debug workflows. This is a scoped engineering assessment, not a penetration test or legal certification.

| Area | Evidence and limits |
| --- | --- |
| Portable backup encryption | PASS: authenticated AES-256-GCM, random salt/nonce, PBKDF2-HMAC-SHA256, wrong-password/tampering tests and actual encrypted file-picker restore. |
| Passphrase handling | Single-use RAM CharArray retained across activity recreation, wiped after use; UI text reset, password keyboard configured, no Bundle/preference/server storage. This does not claim forensic erasure of every OS/IME memory copy. |
| Database integrity | PASS: cross-property validation, cascades, transactional restore rollback, non-destructive migration and disk reopen tests. |
| File access | PASS: actual picker links and persisted read grants; a removed test document produces the app fallback. Detaching the last link releases its persisted grant in source. External originals are not deleted. |
| Local deletion | PASS: confirmation/cancel/delete UI, records/history/preferences, notification history and persisted grants. Exported files, external originals and Amazon store records remain under their respective owners. The separate purchase cache is cleared by Android app-storage clearing/uninstall, disclosed in the policy. |
| Report handling | PASS: reviewed snapshots survive recreation, real PDF renders and CSV contains the reviewed record. CSV formula prefixes are neutralized. PDF uses native text shaping and pagination. |
| Receipt verification | PASS: real App Tester receipt through deployed sandbox RVS verifier, reconnect and restore; 13 worker/Python tests cover matching SKU/receipt, revocation, errors and malformed input. No client-only unlock is present. Amazon-verified LAT test receipts retain their marker. Production charging/LAT distribution are not tested. |
| Account/cache boundaries | Confirmed matching Amazon account required; stale requests invalidated, operation timeout, complete paginated restore, empty restore revocation, seven-day cache maximum and future-time rejection. Cache-policy tests pass; a second real Amazon account was not used. |
| Secrets | Shared secret remains in encrypted Cloudflare secrets, absent from client/source. The packaged authentication key is public and verified against the release APK. Environment, private key and Wrangler state files are excluded from Git. |
| Transport/service | HTTPS only, redirects rejected, fixed Amazon upstream, 8 KB request limit, typed input, approximate distributed per-IP rate limiting, non-cacheable replies and disabled application logs/traces. Workers Free imposes a hard availability cap; no paid resource was enabled. Provider operational metadata may still be retained. |
| Notifications | PASS: actual OS notification delivery, duplicate suppression and quiet-hour suppression. Source uses private lock-screen visibility. This is not a long-duration test across every OEM power setting or lock-screen preference. |
| Core storage | Android private app storage; automatic cloud backup and device transfer excluded. Whole-database encryption, app lock and rooted-device resistance are not implemented or claimed. |
| External services | Amazon store SDK and optional OS speech/document providers have independent policies. Home records are not sent to the receipt verifier. No advertising, product analytics or remote AI service is connected. No packet-capture audit was performed. |

Published policy: https://github.com/lanray07/Fixavora/blob/main/docs/PRIVACY_POLICY.md. Publisher O. Bankole; contact banksmi@mail.com. The policy explains local records, optional providers, receipt identifiers, provider metadata, retention and deletion. Do not describe the app as encrypting its entire database or make blanket legal-compliance claims.

Additional prelaunch assurance can include Amazon-signed/LAT testing, a second account/refund exercise, VoiceView/TalkBack and more physical Fire models. These must be reported separately from the completed sandbox and device checks.
