# Fixavora Privacy Policy

Effective date: 10 November 2026

Publisher: O. Bankole  
Privacy and support contact: banksmi@mail.com  
Support: https://github.com/lanray07/Fixavora/issues

This policy describes the current Fixavora Android application. Fixavora is a home maintenance and repair organiser. An account is not required.

## Your home records

Fixavora stores the records you enter on your device in the application's private storage. These may include property nicknames and optional addresses, rooms, maintenance tasks, repairs, appliance and warranty information, contractor contact details, appointments, expenses and notes.

Fixavora does not automatically upload these records to the publisher. The current app has no cloud synchronisation or remote AI processing. It does not request precise location access and does not include an advertising or product analytics service.

## Documents and photographs

When you attach a document or photograph, Android's file picker lets you choose the file and grant read access. Fixavora keeps a link to the selected file rather than automatically uploading it. Your chosen document or photo provider may offer cloud storage and apply its own privacy policy. Availability of those files offline depends on that provider.

Removing a link or deleting a Fixavora record does not delete the original file from its storage provider.

## Microphone and spoken feedback

Microphone permission is requested only when needed for a speech feature. Speech input is push-to-talk, not continuous listening. Fixavora does not store microphone recordings. The installed speech-recognition service may process audio online under its own policies. Speech services and languages vary by device, and text input remains available.

Spoken feedback uses the device's installed text-to-speech service. Automatic spoken feedback can be disabled in Fixavora's settings. The selected engine determines whether speech processing requires a connection.

## Notifications

If you enable reminders and grant any required notification permission, Fixavora uses saved dates to schedule local maintenance notifications. You can disable reminders in the app or revoke notification permission in device settings. Quiet hours and device power settings can affect delivery.

## Exports, sharing and backups

Reports and backups leave the application's private storage only when you choose to export, save or share them. The selected destination or recipient then controls its copy. Reports may contain private home, contact or expense information, so choose recipients and storage destinations carefully.

Portable encrypted backups contain home records and history. They do not bundle the contents of externally attached files. Backups are encrypted using a passphrase you choose. Keep the passphrase safe: the publisher cannot recover it. Protect exported reports, backup files and separate copies of your documents.

## Amazon Appstore and purchases

Premium is an optional one-time Amazon purchase. Amazon handles its own Appstore account, checkout, installation and store interactions under its policies. Fixavora checks the current Amazon account's purchase status when connecting to the store, so it can restore access and respond to account changes. The app does not receive your Amazon password or payment-card details.

To verify a Premium receipt, Fixavora sends the Amazon customer identifier and receipt identifier over HTTPS to the publisher's receipt-verification service, hosted on Cloudflare Workers. That service asks Amazon's Receipt Verification Service whether the receipt is valid for Fixavora Premium and whether it has been cancelled. It returns the entitlement result to the app. Your property records, addresses, notes, documents and microphone audio are not included in these verification requests.

The verifier does not keep a purchase database or intentionally log customer or receipt identifiers. Cloudflare processes network metadata, including the requesting IP address, to deliver and protect the service. A rate limiter uses the IP address for short-lived request limits. Cloudflare and Amazon may retain information under their own privacy and security policies; the absence of application logs is not a promise that infrastructure providers retain no metadata.

The app caches the last verified entitlement, associated Amazon identifier and verification time in its private storage. Cached entitlement checks have a seven-day maximum grace period and may require connectivity or confirmation of the current Amazon account. Existing home records remain accessible when Premium verification is unavailable. Clearing home data does not cancel or remove an Amazon purchase.

## Retention and deletion

Home records remain on your device until you delete them or remove the app. Settings includes a Delete all home data action for local records, history, attachment links and preferences. Android automatic application backup is disabled for Fixavora.

Copies you exported or shared, files held by external document providers and Amazon's own store records are not deleted by clearing Fixavora's home data. The separately stored entitlement cache supports restoration of an existing purchase and is removed when the app's storage is cleared or the app is uninstalled. Manage external copies with the relevant recipient or provider.

Because the publisher does not receive your local home records automatically, the publisher generally cannot access, recover or remotely delete them. Contact banksmi@mail.com with privacy questions. Do not post private addresses, receipts, contractor details or other sensitive records in public GitHub issues.

## Security

Fixavora uses Android's application-private storage and access controls. Portable record backups use authenticated encryption. The local database is not separately encrypted by Fixavora, and the app does not claim that every record or exported file is end-to-end encrypted. Secure your device and keep its operating system updated. No storage or transmission method can be guaranteed completely secure.

## Intended audience

Fixavora is a general-purpose household organisation tool and is not designed specifically for children. It does not require users to provide a date of birth.

## Policy changes and contact

This policy will be updated when the app's data practices change. The effective date above identifies this version. For support or privacy questions, contact O. Bankole at banksmi@mail.com.
