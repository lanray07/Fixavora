# Architecture

Fixavora is a native Kotlin / Compose Android app (minimum API 26, target 35). No mandatory Google Play Services or Google billing. The UI adapts between tablet navigation and compact navigation and supports both orientations.

The application owns a Room database, repository and DataStore settings. `HomeViewModel` exposes Flow-backed records and handles mutations. Domain calculations use `LocalDate` calendar arithmetic and exact currency minor units, never floating-point money. Recurrence stores its original day anchor so month-end and leap-day schedules recover their original day. Completing a late task advances one occurrence; it never jumps silently to today. Snooze is separate from the due date. Recurrence pause prevents generation of the next occurrence at completion.

`HomeRecord` stores explicit relational links and indexed scheduling columns. Kind-specific user fields are a validated JSON object of strings, avoiding many mostly-null columns. SQLite foreign keys cascade property deletions and remove room/contractor links safely. Repository validation rejects links across properties. History snapshots support immediate completion undo; deletion removes related history. Room schema version 2 adds a scheduling index through a non-destructive migration.

Attachments use read-only persisted SAF grants and stay in the selected document provider. Android's provider controls whether those documents are available offline. The app database works offline. External documents are not copied or silently uploaded. Backups explicitly exclude external file contents and attachment links; retain separate copies.

WorkManager checks reminders every six hours, respecting user opt-in and quiet hours. It does not promise exact delivery. Notification content is private on lock screens. Schedule dates are local calendar dates, not UTC timestamps. Appointments currently store a local time without a timezone identifier; cross-timezone appointment conversion remains a limitation.

Speech uses runtime detection of Android SpeechRecognizer and TTS. It is push-to-talk and the controller is disposed with the screen. Text input remains available. The installed engine determines network processing and language availability. Local assistant output is a proposal; only the normal validated editor can save it.

Amazon Appstore SDK 3.0.9 provides product details, purchase callbacks and restore pagination. Receipt verification is delegated to an HTTPS service; only verified entitlements unlock Premium. A cached verified entitlement preserves offline access. No real public key, shared secret or hosted receipt endpoint is supplied. The Python service is a deployable API reference and requires TLS/rate-limiting infrastructure before public use. Core app storage is local and requires no server.

Portable backups use PBKDF2-HMAC-SHA256 (210,000 iterations), random salt, AES-256-GCM and a random nonce. Authentication failures prevent replacement of current records; restoration validates and inserts transactionally. Keystore is not used for these portable passphrase backups, because device-bound keys would prevent recovery on a new device.

Strings are compiled from JSON to Android resources, with BCP47 locale folders, fallback, placeholder checks, review tracking and a development-only translation gateway. No translation credentials are packaged. There are no production analytics or advertising SDKs. The repository includes CI and test scripts; remote CI completion is reported separately from local results.
