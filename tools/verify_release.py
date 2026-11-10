"""Verify the built Amazon upload artifact without exposing any key material."""
import hashlib,json,os,re,subprocess,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
apk=ROOT/'app/build/outputs/apk/release/app-release-unsigned.apk'
with zipfile.ZipFile(apk) as archive:
    names=archive.namelist()
    dex=b''.join(archive.read(n) for n in names if re.fullmatch(r'classes\d*\.dex',n))
    manifest=archive.read('AndroidManifest.xml')
    assert b'https://fixavora-receipts.lanraybanks.workers.dev/verify' in dex
    assert b'fixavora-receipts-sandbox.lanraybanks.workers.dev' not in dex
    assert not any(n.startswith('assets/screenshots/') for n in names)
    for host in ['EditorTestActivity','TabletCaptureActivity','CompactCaptureActivity']:
        assert host.encode() not in dex and host.encode('utf-16le') not in manifest,host
    key=archive.read('assets/AppstoreAuthenticationKey.pem')
    assert key==(ROOT/'app/src/main/assets/AppstoreAuthenticationKey.pem').read_bytes()
    assert b'PRIVATE KEY' not in key
sdk=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))
aapt=sdk/'build-tools/35.0.0'/('aapt.exe' if os.name=='nt' else 'aapt')
badging=subprocess.check_output([str(aapt),'dump','badging',str(apk)],text=True,encoding='utf-8')
manifest_tree=subprocess.check_output([str(aapt),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True,encoding='utf-8')
resources=subprocess.check_output([str(aapt),'dump','resources',str(apk)],text=True,encoding='utf-8')
assert 'android:dataExtractionRules' in manifest_tree and 'xml/data_extraction_rules' in resources
assert 'android:allowBackup(0x01010280)=(type 0x12)0x0' in manifest_tree
assert "package: name='com.fixavora.app' versionCode='2' versionName='1.0.0'" in badging
assert "sdkVersion:'26'" in badging and "targetSdkVersion:'35'" in badging
report={'artifact':str(apk.relative_to(ROOT)).replace('\\','/'),'version':'1.0.0','version_code':2,'min_sdk':26,'target_sdk':35,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'production_endpoint':'PASS','sandbox_endpoint_excluded':'PASS','test_hosts_and_fixtures_excluded':'PASS','public_authentication_key_matches':'PASS','public_key_sha256':hashlib.sha256(key).hexdigest(),'shared_secret':'SERVER ONLY','artifact_signing':'Unsigned Amazon upload; Amazon-signed distribution not tested'}
out=ROOT/'deliverables/android/build-logs/release-artifact-verification.json'
out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
