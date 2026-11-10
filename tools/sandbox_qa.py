"""Run real App Tester verification, preserving the device's existing SKU file/mode.
No live purchase is clicked. SandboxPurchaseTest checks SDK mode before purchasing.
"""
import argparse,json,os,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ADB=Path(os.environ.get("ANDROID_HOME",str(Path.home()/"AppData/Local/Android/Sdk")))/"platform-tools/adb.exe"
parser=argparse.ArgumentParser(); parser.add_argument("--serial",required=True); parser.add_argument("--documents",action="store_true"); parser.add_argument("--workflows",action="store_true"); parser.add_argument("--classes"); parser.add_argument("--tests-only-install",action="store_true"); args=parser.parse_args()
def adb(*command,check=True,timeout=180):
    result=subprocess.run([str(ADB),"-s",args.serial,*command],capture_output=True,text=True,timeout=timeout,encoding="utf-8",errors="replace")
    if check and result.returncode: raise RuntimeError(result.stderr)
    return result
runtime=ROOT/".runtime"; runtime.mkdir(exist_ok=True)
before=runtime/"amazon-sdktester-before.json"
existed=adb("pull","/sdcard/amazon.sdktester.json",str(before),check=False).returncode==0
original_mode=adb("shell","getprop","debug.amazon.sandboxmode").stdout.strip()
items=json.loads(before.read_text(encoding="utf-8-sig")) if existed else {}
items["fixavora.premium.lifetime"]={"itemType":"ENTITLED","price":4.99,"title":"Fixavora Premium","description":"One-time Premium upgrade for sandbox testing only","smallIconUrl":"https://raw.githubusercontent.com/lanray07/Fixavora/main/deliverables/brand-assets/app-icon-114.png"}
config=runtime/"amazon.sdktester.json"; config.write_text(json.dumps(items,indent=2),encoding="utf-8")
try:
    adb("push",str(config),"/sdcard/amazon.sdktester.json")
    adb("shell","setprop","debug.amazon.sandboxmode","debug")
    for apk in ["app/build/outputs/apk/debug/app-debug.apk","app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]:
        if args.tests_only_install and '/androidTest/' not in apk: continue
        print(adb("install","-r",str(ROOT/apk)).stdout,flush=True)
    result=adb("shell","am","instrument","-w","-e","class","com.fixavora.app.SandboxPurchaseTest","com.fixavora.app.test/androidx.test.runner.AndroidJUnitRunner",timeout=240)
    log=ROOT/"deliverables/android/build-logs/amazon-sandbox-purchase.log"; log.parent.mkdir(parents=True,exist_ok=True)
    log.write_text(result.stdout+result.stderr,encoding="utf-8"); print(result.stdout,flush=True)
    adb("pull","/sdcard/Android/data/com.fixavora.app/files/sandbox-purchase-result.json",str(log.with_suffix(".json")),check=False)
    adb("pull","/sdcard/Android/data/com.fixavora.app/files/sandbox-purchase-dialog.xml",str(runtime/"sandbox-purchase-dialog.xml"),check=False)
    if "OK (" not in result.stdout or "FAILURES!!!" in result.stdout: raise RuntimeError("Amazon sandbox device test failed; see saved evidence")
    if args.documents or args.workflows or args.classes:
        classes=args.classes.split(',') if args.classes else (["DocumentBackupTest"] if args.documents else []) + (["AttachmentPickerTest","SettingsDeletionTest","ReminderDeliveryTest","VoiceCompatibilityTest","PersistenceTest","RecomposeStoreTest"] if args.workflows else [])
        result=adb("shell","am","instrument","-w","-e","class",','.join('com.fixavora.app.'+x for x in classes),"-e","locales","ja-JP,zh-Hans,zh-Hant","com.fixavora.app.test/androidx.test.runner.AndroidJUnitRunner",timeout=600)
        (log.parent/"document-backup.log").write_text(result.stdout+result.stderr,encoding="utf-8");print(result.stdout,flush=True)
        for name in ["document-backup-result.json","voice-compatibility.json","downloads-root.xml","picker-root.xml","create-document.xml","create-document-initial.xml","create-document-initial.png","save-document.xml","open-document.xml","after-create.xml"]:
            adb("pull","/sdcard/Android/data/com.fixavora.app/files/"+name,str(runtime/name),check=False)
        if "OK (" not in result.stdout or "FAILURES!!!" in result.stdout: raise RuntimeError("Document backup device test failed; see saved evidence")
finally:
    adb("shell","setprop","debug.amazon.sandboxmode",original_mode or "''",check=False)
    if existed: adb("push",str(before),"/sdcard/amazon.sdktester.json",check=False)
    else: adb("shell","rm","-f","/sdcard/amazon.sdktester.json",check=False)
