# Fixavora receipt verification

Production: https://fixavora-receipts.lanraybanks.workers.dev/verify

App Tester sandbox: https://fixavora-receipts-sandbox.lanraybanks.workers.dev/verify

The deployed account uses Workers Free. No paid database, analytics or log service is enabled. The platform's free daily allowance is a hard availability limit; do not enable a paid plan without the publisher's approval. The approximate distributed rate limiter permits 30 requests per minute per connecting IP and location. Home records, addresses and documents are never sent to this service.

`GET /health` reports configuration and environment without exposing credentials. `POST /verify` accepts JSON with `userId` and `receiptId`. The server validates size and types, calls only Amazon's fixed HTTPS receipt endpoint, rejects redirects, checks the exact Fixavora SKU, receipt ID, product type and cancellation state, and returns a non-cacheable entitlement result. An invalid/revoked receipt differs from temporary verification failure. Amazon-verified LAT receipts retain their test marker.

## Configure and deploy

Use Node.js 22 or later and Wrangler. Authenticate to the intended Cloudflare account, confirm Workers Free, then run from this directory:

```sh
npx wrangler secret put AMAZON_SHARED_SECRET
npx wrangler deploy
npx wrangler deploy --env sandbox
node --test worker.test.mjs
```

Enter the Amazon RVS shared secret only at the encrypted secret prompt. Never place it in the APK, repository, command arguments or logs. The public `AppstoreAuthenticationKey.pem` packaged with the app is a different, non-secret authentication key. The sandbox's configured dummy string is intentionally non-secret; Amazon's App Tester sandbox accepts any non-empty developer-secret value.

Application logs and traces are disabled. Cloudflare and Amazon may process operational request metadata under their own policies. The IP-based limiter has transient counters. The app limits offline entitlement caching to seven days and confirms the Amazon account before using it.

The Android release build is fixed to production; the debug build is fixed to sandbox. A successful sandbox test does not certify an Amazon-signed/LAT distribution. Deployments retain encrypted server secrets; use the dashboard to confirm the secret exists after changing account or Worker names.
