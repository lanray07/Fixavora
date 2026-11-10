"""Development-only localisation compiler/validator. No runtime translation or private data upload.

Usage: python tools/localize.py generate|validate|translate [--locale TAG]
TRANSLATION_ENDPOINT: HTTPS POST accepting {locale, strings, glossary}, returning {strings}.
TRANSLATION_API_KEY: bearer credential held only in development environment.
"""
import argparse, csv, hashlib, json, os, re, sys, urllib.request
from pathlib import Path
from xml.sax.saxutils import escape
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
LOC = ROOT / "localization"
TARGETS = "en-GB en-US en-AU es-ES es-419 fr-FR fr-CA de-DE it-IT pt-BR pt-PT nl-NL pl-PL sv-SE ja-JP hi-IN ar tr-TR id-ID".split()

def load(path):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}

def placeholders(s):
    return sorted(re.findall(r"%(?:\d+\$)?[dsf]|\{[\w]+\}", s))

def generate():
    base = load(LOC / "en-GB.json")
    res = ROOT / "app/src/main/res"
    for tag in ["base"] + TARGETS:
        data = (base | load(LOC / f"{tag}.json")) if tag in ("base", "en-GB", "en-US", "en-AU") else load(LOC / f"{tag}.json")
        folder = "values" if tag == "base" else "values-b+" + tag.replace("-", "+")
        path = res / folder / "strings.xml"
        path.parent.mkdir(parents=True, exist_ok=True)
        def android(s):
            return escape(s).replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
        lines = [f'    <string name="{k}">{android(v)}</string>' for k, v in sorted(data.items()) if k in base]
        if tag in ("base", "en-GB", "en-US", "en-AU"):
            lines.append('    <plurals name="record_count"><item quantity="one">%d record</item><item quantity="other">%d records</item></plurals>')
        # Draft locales deliberately inherit the base plural until reviewed native forms are supplied.
        path.write_text('<resources>\n' + '\n'.join(lines) + '\n</resources>\n', encoding="utf-8")

def validate():
    base = load(LOC / "en-GB.json")
    literals = set()
    for source in (ROOT / "app/src/main/java").rglob("*.kt"):
        literals.update(re.findall(r'\.t\("([a-z_]+)"\)', source.read_text(encoding="utf-8")))
    absent = sorted(literals - set(base))
    if absent: raise SystemExit("FAIL: source keys missing from base catalog: " + ", ".join(absent))
    output = ROOT / "deliverables/localization"
    output.mkdir(parents=True, exist_ok=True)
    rows = []
    failures = []
    review = load(LOC / "review.json")
    for tag in TARGETS:
        data = base if tag.startswith("en-") else load(LOC / f"{tag}.json")
        missing = sorted(set(base) - set(data))
        bad = [k for k in data if k in base and placeholders(data[k]) != placeholders(base[k])]
        failures.extend((tag, k) for k in bad)
        rows.append(dict(locale=tag, translated=len(base) - len(missing), total=len(base), coverage=round((len(base)-len(missing))*100/len(base), 2), missing=missing, placeholder_errors=bad, human_review=review.get(tag, "NOT TESTED"), release_status="BLOCKED" if missing or review.get(tag) != "PASS" else "PASS"))
    for path in (ROOT / "app/src/main/res").glob("values*/strings.xml"):
        root = ET.parse(path).getroot()
        names = [e.attrib["name"] for e in root]
        assert len(names) == len(set(names)), path
        for plural in root.findall("plurals"):
            assert "other" in {e.attrib["quantity"] for e in plural}, path
    (output / "coverage.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"locales": len(rows), "base_strings": len(base), "placeholder_errors": failures, "fully_translated": [r["locale"] for r in rows if not r["missing"]], "release_approved": [r["locale"] for r in rows if r["release_status"] == "PASS"]}, indent=2))
    if failures: sys.exit(1)

def translate(tag):
    endpoint = os.environ.get("TRANSLATION_ENDPOINT", "")
    key = os.environ.get("TRANSLATION_API_KEY", "")
    if not endpoint.startswith("https://") or not key:
        raise SystemExit("BLOCKED: set TRANSLATION_ENDPOINT (HTTPS) and TRANSLATION_API_KEY. No credentials were printed.")
    base = load(LOC / "en-GB.json")
    existing = load(LOC / f"{tag}.json")
    missing = {k: v for k, v in base.items() if k not in existing}
    payload = dict(locale=tag, strings=missing, glossary=load(LOC / "glossary.json"))
    digest = hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest()
    cache = LOC / "cache" / (digest + ".json")
    if cache.exists(): result = load(cache)
    else:
        request = urllib.request.Request(endpoint, data=json.dumps(payload).encode(), headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=120) as response: result = json.load(response)["strings"]
        assert set(result) == set(missing), "Translation response missing keys"
        assert all(placeholders(result[k]) == placeholders(missing[k]) for k in missing), "Placeholder corruption"
        cache.parent.mkdir(exist_ok=True); cache.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    existing.update(result)
    (LOC / f"{tag}.json").write_text(json.dumps(existing, ensure_ascii=False, indent=2), encoding="utf-8")
    generate(); validate()

if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("command", choices=["generate", "validate", "translate"]); parser.add_argument("--locale", choices=TARGETS)
    args = parser.parse_args()
    if args.command == "generate": generate()
    elif args.command == "validate": validate()
    else:
        if not args.locale: parser.error("--locale required")
        translate(args.locale)
