import os, unittest, threading, json, urllib.request, urllib.error
from unittest.mock import patch
from receipt_service import evaluate, SKU, Handler, ThreadingHTTPServer

class ReceiptTest(unittest.TestCase):
    def receipt(self, **kwargs): return {"productId": SKU, "productType": "ENTITLED", "receiptId": "receipt-example", **kwargs}
    def test_valid_entitlement(self): self.assertTrue(evaluate(self.receipt(), "user", "receipt-example")["active"])
    def test_cancelled_or_other_product(self):
        self.assertFalse(evaluate(self.receipt(cancelDate=123), "user", "receipt-example")["active"])
        self.assertFalse(evaluate(self.receipt(productId="other"), "user", "receipt-example")["active"])
    def test_receipt_must_match(self): self.assertFalse(evaluate(self.receipt(), "user", "wrong")["active"])
    def test_test_receipt_rejected_in_production(self):
        with patch.dict(os.environ, {"AMAZON_RVS_ENV":"production"}): self.assertFalse(evaluate(self.receipt(testTransaction=True), "user", "receipt-example")["active"])
    def test_rate_limit_is_shared_between_requests(self):
        Handler.attempts = {}
        server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        thread = threading.Thread(target=server.serve_forever, daemon=True); thread.start()
        try:
            with patch.dict(os.environ, {"AMAZON_SHARED_SECRET":""}):
                codes = []
                for _ in range(31):
                    request = urllib.request.Request(f"http://127.0.0.1:{server.server_port}/verify", data=json.dumps({"userId":"example", "receiptId":"example"}).encode(), headers={"Content-Type":"application/json"})
                    try:
                        with urllib.request.urlopen(request, timeout=5) as response: codes.append(response.status)
                    except urllib.error.HTTPError as error: codes.append(error.code)
                self.assertEqual([503]*30+[429], codes)
        finally: server.shutdown(); server.server_close(); thread.join(timeout=5)

if __name__ == "__main__": unittest.main()
