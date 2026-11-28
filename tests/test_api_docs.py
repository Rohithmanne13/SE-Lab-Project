"""Checks of the API documentation deliverables (SRS UI-2, SE-7, CON-9).

The Postman collection docs/api/financial-ledger.postman_collection.json must cover every endpoint of the OpenAPI
contract docs/api/openapi.yaml, use the right key for each role, send an Idempotency-Key where one is required and
contain no API key. Run: python3 -m unittest tests.test_api_docs -v
"""
import json
import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
COLLECTION = REPO / "docs" / "api" / "financial-ledger.postman_collection.json"
OPENAPI = REPO / "docs" / "api" / "openapi.yaml"
ADMIN_PATHS = {"/api/v1/wallets/{walletId}/deposits", "/api/v1/transactions/{transactionId}/reversal",
               "/api/v1/admin/reconciliation"}
IDEMPOTENT = {("POST", "/api/v1/wallets/{walletId}/deposits"), ("POST", "/api/v1/transfers"),
              ("POST", "/api/v1/transactions/{transactionId}/reversal")}


def openapi_operations():
    """(METHOD, path) of every operation, read from the paths section of the YAML without a YAML library."""
    ops, path, in_paths = set(), None, False
    for line in OPENAPI.read_text(encoding="utf-8").splitlines():
        if line.startswith("paths:"):
            in_paths = True
        elif in_paths and re.match(r"^\S", line):
            break
        elif in_paths and re.match(r"^  /\S*:$", line):
            path = line.strip()[:-1]
        elif in_paths and path and re.match(r"^    (get|post|put|patch|delete):$", line):
            ops.add((line.strip()[:-1].upper(), path))
    return ops


def requests():
    data = json.loads(COLLECTION.read_text(encoding="utf-8"))
    for folder in data["item"]:
        for item in folder["item"]:
            yield item


def template(raw_url):
    """{{baseUrl}}/api/v1/users/{{aliceUserId}}/wallets?x=1 -> /api/v1/users/{}/wallets"""
    path = raw_url.replace("{{baseUrl}}", "").split("?")[0]
    return re.sub(r"\{\{\w+\}\}", "{}", path)


class PostmanCollectionTest(unittest.TestCase):
    def test_is_a_v21_collection_without_keys(self):
        data = json.loads(COLLECTION.read_text(encoding="utf-8"))
        self.assertIn("v2.1.0", data["info"]["schema"])
        variables = {v["key"]: v["value"] for v in data["variable"]}
        self.assertEqual(variables["clientKey"], "")
        self.assertEqual(variables["adminKey"], "")
        self.assertEqual(variables["baseUrl"], "http://127.0.0.1:8000")

    def test_covers_every_endpoint_of_the_contract(self):
        ops = openapi_operations()
        self.assertEqual(len(ops), 12)
        covered = {(r["request"]["method"], template(r["request"]["url"]["raw"])) for r in requests()}
        for method, path in ops:
            self.assertIn((method, re.sub(r"\{\w+\}", "{}", path)), covered, f"{method} {path} not in the collection")

    def test_roles_and_idempotency_keys(self):
        for item in requests():
            r = item["request"]
            headers = {h["key"]: h["value"] for h in r["header"]}
            path = template(r["url"]["raw"])
            admin = any(re.sub(r"\{\w+\}", "{}", p) == path for p in ADMIN_PATHS)
            if path == "/health" or "No API key" in item["name"]:
                self.assertNotIn("X-API-Key", headers, item["name"])
            elif admin and "CLIENT key" not in item["name"]:
                self.assertEqual(headers["X-API-Key"], "{{adminKey}}", item["name"])
            else:
                self.assertEqual(headers["X-API-Key"], "{{clientKey}}", item["name"])
            needs_key = any(m == r["method"] and re.sub(r"\{\w+\}", "{}", p) == path for m, p in IDEMPOTENT)
            if needs_key and "Missing Idempotency-Key" not in item["name"]:
                self.assertIn("Idempotency-Key", headers, item["name"])
            if r["method"] == "POST":
                self.assertEqual(headers.get("Content-Type"), "application/json", item["name"])
                json.loads(re.sub(r"\{\{\w+\}\}", "x", r["body"]["raw"]))

    def test_every_request_has_a_test_script(self):
        for item in requests():
            scripts = [e for e in item.get("event", []) if e["listen"] == "test"]
            self.assertTrue(scripts, item["name"])
            self.assertIn("to.have.status(", "\n".join(scripts[0]["script"]["exec"]), item["name"])


if __name__ == "__main__":
    unittest.main()
