import os, unittest
from unittest.mock import patch
from receipt_service import evaluate, SKU

class ReceiptTest(unittest.TestCase):
    def receipt(self, **kwargs): return {"productId": SKU, "productType": "ENTITLED", "receiptId": "receipt-example", **kwargs}
    def test_valid_entitlement(self): self.assertTrue(evaluate(self.receipt(), "user", "receipt-example")["active"])
    def test_cancelled_or_other_product(self):
        self.assertFalse(evaluate(self.receipt(cancelDate=123), "user", "receipt-example")["active"])
        self.assertFalse(evaluate(self.receipt(productId="other"), "user", "receipt-example")["active"])
    def test_receipt_must_match(self): self.assertFalse(evaluate(self.receipt(), "user", "wrong")["active"])
    def test_test_receipt_rejected_in_production(self):
        with patch.dict(os.environ, {"AMAZON_RVS_ENV":"production"}): self.assertFalse(evaluate(self.receipt(testTransaction=True), "user", "receipt-example")["active"])

if __name__ == "__main__": unittest.main()
