"""Amazon RVS adapter; API only, no website. Run behind a TLS reverse proxy.

No receipt/user IDs or shared secrets are written to access logs. The only permitted
upstream is Amazon RVS. Production infrastructure must enforce global rate limits.
"""
import json, os, threading, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import quote
from urllib.request import urlopen
from urllib.error import HTTPError

SKU = "fixavora.premium.lifetime"

def evaluate(receipt, user, receipt_id):
    valid = receipt.get("productId") == SKU and receipt.get("productType") == "ENTITLED" and receipt.get("receiptId") == receipt_id
    # Production RVS validates genuine LAT/reviewer receipts too. Never accept
    # client-supplied verification data or route production traffic to sandbox.
    return {"sku": SKU, "userId": user, "receiptId": receipt_id, "active": valid and not receipt.get("cancelDate"), "testTransaction": receipt.get("testTransaction") is True}

class Handler(BaseHTTPRequestHandler):
    attempts = {}
    lock = threading.Lock()
    def log_message(self, *_): pass
    def respond(self, status, data):
        payload = json.dumps(data).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers(); self.wfile.write(payload)
    def do_POST(self):
        self.connection.settimeout(10)
        if self.path != "/verify": return self.respond(404, {"error": "not_found"})
        with self.lock:
            now = time.monotonic(); ip = self.client_address[0]
            type(self).attempts = {k: v for k, v in type(self).attempts.items() if v[0] > now - 60}
            start, count = type(self).attempts.get(ip, (now, 0))
            if count >= 30: return self.respond(429, {"error": "rate_limit"})
            type(self).attempts[ip] = (start, count + 1)
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 8192: return self.respond(400, {"error": "invalid_request"})
            data = json.loads(self.rfile.read(size))
            user, receipt_id = data["userId"], data["receiptId"]
            if not all(isinstance(x, str) and 1 <= len(x) <= 2048 for x in (user, receipt_id)): raise ValueError()
            secret = os.environ.get("AMAZON_SHARED_SECRET")
            if not secret: return self.respond(503, {"error": "not_configured"})
            mode = os.environ.get("AMAZON_RVS_ENV", "production")
            if mode not in ("production", "sandbox"): raise ValueError()
            prefix = "sandbox/" if mode == "sandbox" else ""
            url = "https://appstore-sdk.amazon.com/" + prefix + "version/1.0/verifyReceiptId/developer/" + quote(secret, safe="") + "/user/" + quote(user, safe="") + "/receiptId/" + quote(receipt_id, safe="")
            with urlopen(url, timeout=15) as response: verified = json.load(response)
            return self.respond(200, evaluate(verified, user, receipt_id))
        except HTTPError as error:
            if error.code in (400, 410): return self.respond(200, {"sku": SKU, "userId": user, "receiptId": receipt_id, "active": False})
            return self.respond(502, {"error": "verification_unavailable"})
        except (ValueError, KeyError, TypeError): return self.respond(400, {"error": "invalid_request"})
        except Exception: return self.respond(502, {"error": "verification_unavailable"})

if __name__ == "__main__":
    if not os.environ.get("AMAZON_SHARED_SECRET"): raise SystemExit("BLOCKED: AMAZON_SHARED_SECRET is not configured")
    # Bind only loopback; expose through a managed HTTPS proxy with request limits.
    ThreadingHTTPServer(("127.0.0.1", int(os.environ.get("PORT", "8081"))), Handler).serve_forever()
