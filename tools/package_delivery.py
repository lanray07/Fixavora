"""Copy built artifacts, preserve test evidence and inventory actual files with SHA-256 hashes."""
import csv, hashlib, json, shutil, re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"deliverables"
version=re.search(r'versionName\s*=\s*"([^"]+)"',(ROOT/"app/build.gradle.kts").read_text()).group(1)
for source,target in [
    ("app/build/outputs/apk/debug/app-debug.apk",f"android/debug/Fixavora-{version}-debug.apk"),
    ("app/build/outputs/apk/release/app-release-unsigned.apk",f"android/release/Fixavora-{version}-release-unsigned.apk")
]:
    src=ROOT/source
    if not src.exists(): raise SystemExit("BLOCKED: missing build artifact "+source)
    dst=OUT/target;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(src,dst)
tests=OUT/"android/build-logs/unit-test-results";tests.mkdir(parents=True,exist_ok=True)
for src in (ROOT/"app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"): shutil.copy2(src,tests/src.name)
lint=ROOT/"app/build/reports/lint-results-debug.txt"
if lint.exists(): shutil.copy2(lint,OUT/"android/build-logs/lint-results-debug.txt")
rows=[]
for file in sorted(OUT.rglob("*")):
    if file.is_file() and file.name not in ("DELIVERABLES_MANIFEST.csv","artifact-hashes.json"):
        rows.append(dict(path=file.relative_to(ROOT).as_posix(),bytes=file.stat().st_size,sha256=hashlib.sha256(file.read_bytes()).hexdigest()))
with (OUT/"DELIVERABLES_MANIFEST.csv").open("w",newline="",encoding="utf-8") as f:
    writer=csv.DictWriter(f,fieldnames=["path","bytes","sha256"]);writer.writeheader();writer.writerows(rows)
(OUT/"android/artifact-hashes.json").write_text(json.dumps([r for r in rows if r["path"].endswith(".apk")],indent=2),encoding="utf-8")
print(f"Inventoried {len(rows)} actual files; APKs copied and hashed. This does not certify release readiness.")
