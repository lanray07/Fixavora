# Verified platform facts and product hypotheses

Research date: 10 November 2026. Sources below were opened during implementation. This is preliminary research, not trademark clearance, legal assessment or marketplace approval.

## Distribution

Amazon discontinued its Appstore on third-party Android devices on 20 August 2025. Fire tablets remain supported. Fixavora is therefore designed for Fire OS and compatible Android runtimes; the Amazon distribution plan must not promise a general Android Amazon marketplace launch.

Source: https://developer.amazon.com/apps-and-games/blogs/2025/02/upcoming-changes-to-amazon-appstore-for-android-devices-and-coins-program

## Amazon listing assets and metadata

Tablet screenshots: 3–10 images. The documented sizes include 800×480, 1024×600, 1280×720, 1280×800, 1920×1080, 1920×1200 and 2560×1600, with landscape or portrait support. Icons: 114×114 and 512×512 transparent PNG. Optional promotional image: 1024×500. Screenshot content should reflect the actual tablet experience and use dummy information. This project targets 1920×1200 tablet and 800×1280 compact candidates. The compact captures simulate a viewport on a Fire tablet and do not establish phone hardware compatibility.

The current console shows 200 characters for display title, 1,200 characters for short description, 4,000 for long description and up to 10 feature bullets. Documentation also specifies a 2,000-byte short-description limit; the exported metadata meets both short-description limits and uses five bullets. Keywords are optional. These are Amazon-specific checks.

Source: https://developer.amazon.com/docs/app-submission/appstore-details.html

## Localisation matrices

The live account's description dialog offers 15 locales: en-US, en-GB, en-AU, en-IN, de-DE, es-ES, es-MX, fr-FR, it-IT, ja-JP, nl-NL, pl-PL, pt-BR, pt-PT and sv-SE. Its image dialog offers 11: en-US, en-GB, en-AU, en-IN, de-DE, es-ES, es-MX, fr-FR, it-IT, ja-JP and pt-BR. Console observations take precedence over an older 13-locale documentation list. The delivery also includes assets for the additional interface languages. Storefront, interface, distribution country, recognition and installed TTS voice are separate matrices.

The draft targets the available countries and supported Fire tablet selections under the user's worldwide-release instructions. TV/automotive targets are excluded. The publisher explicitly authorized the export-compliance certification. This is not an independent assessment of rights or every jurisdiction's laws.

Source: https://developer.amazon.com/docs/app-submission/distribute-your-app-internationally.html

## Purchasing

The app pins Appstore SDK 3.0.9 and packages the downloaded app-specific public key. The server-held shared secret is encrypted in Cloudflare. The product is configured as a one-time GBP 4.99 entitlement with marketplace conversions; the app displays Amazon's localized price. Real App Tester sandbox receipts are verified through the deployed sandbox Worker, including reconnect/restore. The release uses production RVS. Amazon-signed/LAT distribution and production charging have not been tested; they are distinct from the completed sandbox test.

Sources:
- https://developer.amazon.com/docs/appstore-sdk/integrate-appstore-sdk.html
- https://developer.amazon.com/docs/in-app-purchasing/iap-rvs-for-android-apps.html
- https://developer.amazon.com/docs/in-app-purchasing/iap-get-started.html

## Name and positioning

A preliminary exact-name web search did not establish trademark clearance for Fixavora. A similarly named product, Fixora, describes home problem records, repairs, costs and an offline experience. Treat similarity and confusion risk as unresolved; formal name/mark and marketplace searches remain required.

Primary source: https://play.google.com/store/apps/details?id=com.fixora.home

Hypotheses, not verified demand or revenue evidence: a calm Fire-tablet interface, private offline records, portable history and a one-time upgrade may appeal to households avoiding accounts/subscriptions. GBP 4.99 is now configured, but willingness to pay is not validated. No search volumes, ranking guarantees, reviews or revenue projections were invented.

## Compatibility and currency verification

Amazon's current compatibility table requires target API 28 for Fire OS 7 and API 30 for Fire OS 8. The chosen target 35 satisfies those requirements; minimum API 26 excludes older runtimes. Fire OS 16's API-36 requirement concerns other selected platforms such as newer TV targets, which are outside this release.

Source: https://developer.amazon.com/docs/app-submission/device-filtering-and-compatibility.html

Java NumberFormat does not change fraction-digit limits when changing currency. The release therefore sets precision from the selected currency explicitly, avoiding GBP rounding in Japanese/Korean locales and preserving zero-/three-decimal currencies. Regression tests and actual Japanese/Korean report captures check this boundary.

Source: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/NumberFormat.html#setCurrency(java.util.Currency)

## Privacy label interpretation (10 November 2026)

Amazon asks for user-associated data handled by the app and its providers, and excludes provider processing and deliberate user sharing from its third-party transfer category. The draft conservatively discloses local record inputs plus Amazon customer/receipt identifiers and Cloudflare IP metadata for functionality/security; it does not declare advertising or analytics. No Google Play off-device-only definition was substituted for Amazon’s questionnaire. See [Amazon privacy label definitions](https://developer.amazon.com/docs/app-submission/appstore-privacy-labels.html). This is an implementation assessment, not legal certification.
