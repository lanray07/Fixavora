# Security and privacy assessment

Assessment date: 10 November 2026. Scope: source inspection, local tests, APK configuration and the connected Fire tablet. This is not a penetration test or a legal compliance certification.

| Area | Status | Evidence / limit |
| --- | --- | --- |
| Backup confidentiality and integrity | PASS | AES-GCM round-trip, random ciphertext, wrong-passphrase and tampering tests pass. PBKDF2-HMAC-SHA256 derives a portable key. |
| Relational integrity and restore rollback | PASS | Device tests reject cross-property links and orphan restores, exercise cascades and preserve current data on validation failure. |
| CSV formula handling | PASS | Tests neutralise formula prefixes and escape quotes/newlines. |
| Core local persistence | PASS | Device tests reopen a disk database and read the saved record. |
| No embedded production secrets | PASS | Source/configuration review found no provider keys, Amazon shared secret or signing key. `.env.example` contains empty values. Receipt and translation secrets are server/development-only. This is a scoped review, not a guarantee against all possible leaks. |
| Cleartext network policy | PASS | Application manifest disables cleartext traffic. Client receipt URL must be HTTPS and redirects are disabled. |
| Purchases fail closed | PASS | Source gates purchase setup on a public key and verification endpoint; there is no production test unlock. Server evaluation tests reject wrong/cancelled/test receipts. Real Amazon purchase verification remains BLOCKED. |
| File picker / external viewer end to end | NOT TESTED | Implemented with read-only persisted SAF grants, explicit user picker and compatible ACTION_VIEW intents; physical provider permission/revocation scenarios still need testing. |
| Complete local deletion journey | NOT TESTED | Database cascade behaviour is tested. Settings deletion also clears preferences, notifications and persisted grants; its full UI journey still needs execution. External originals, exported backups and Amazon purchases are intentionally not deleted. |
| Platform storage confidentiality | NOT TESTED | Uses Android app-private storage and disables automatic backup. The Room database is not independently encrypted. No rooted-device resistance or device-encryption assessment is claimed. |
| Notifications / lock screen | NOT TESTED | Notification visibility is private and scheduling respects opt-in/quiet hours in source. Delivery, revocation and all Fire OS battery cases are not established by a long-duration test. |
| Network / SDK audit | NOT TESTED | No advertising/analytics dependency is intentionally included. Amazon SDK and OS speech/document providers have their own policies. No packet-capture audit was performed. |
| Public receipt-service deployment | BLOCKED | Requires publisher-held Amazon secret, TLS endpoint, global edge rate limits, operational logging/retention policy and Amazon verification tests. No service was deployed. |
| Legal and privacy publication | BLOCKED | Publisher identity/contact, policy URL, processor disclosures and jurisdiction review are required. Draft policy is in `docs/PRIVACY_POLICY_DRAFT.md`. |

The assistant's local organisation does not upload records and requires review before saving. Remote AI is not connected. Speech may be processed by the installed engine, which is disclosed before the user invokes it. No continuous listening or recording retention is implemented.

Remaining hardening: assess app lock if required by the intended threat model; conduct accessibility, dependency and SDK privacy audits; test attachment loss/revocation, sensitive report sharing, notification leakage, entitlement account changes/refunds and signed-release behaviour. Do not advertise encryption of all local data or blanket legal compliance.
