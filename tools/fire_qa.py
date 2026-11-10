"""Run authorised device QA and collect actual evidence. Restores Wi-Fi/display size afterward.

python tools/fire_qa.py --serial SERIAL [--compact]
Never clears unrelated packages or device data. Fixture data belongs only to Fixavora.
"""
import argparse, json, os, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ADB=Path(os.environ.get("ANDROID_HOME",str(Path.home()/"AppData/Local/Android/Sdk")))/"platform-tools/adb.exe"
LOG=ROOT/"deliverables/android/build-logs"; LOG.mkdir(parents=True,exist_ok=True)
parser=argparse.ArgumentParser();parser.add_argument("--serial",required=True);parser.add_argument("--compact",action="store_true");parser.add_argument("--workspace-only",action="store_true");args=parser.parse_args()
def adb(*command, timeout=180):
    return subprocess.run([str(ADB),"-s",args.serial,*command],capture_output=True,text=True,encoding="utf-8",errors="replace",timeout=timeout)
def require(result):
    if result.returncode: raise RuntimeError(result.stderr)
    return result.stdout
for apk in ["app/build/outputs/apk/debug/app-debug.apk","app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]:
    print(require(adb("install","-r",str(ROOT/apk))),flush=True)
require(adb("shell","input","keyevent","224"))
adb("shell","wm","dismiss-keyguard")
device={}
for key in ["ro.product.manufacturer","ro.product.model","ro.build.version.sdk","ro.build.version.release","ro.build.display.id"]:
    device[key]=require(adb("shell","getprop",key)).strip()
device["display_size"]=require(adb("shell","wm","size")).strip()
device["display_density"]=require(adb("shell","wm","density")).strip()
(LOG/"device.json").write_text(json.dumps(device,indent=2),encoding="utf-8")
def instrument(test, suffix, *extras):
    result=adb("shell","am","instrument","-w","-e","class","com.fixavora.app."+test,*extras,"com.fixavora.app.test/androidx.test.runner.AndroidJUnitRunner",timeout=300)
    (LOG/(suffix+".log")).write_text(result.stdout+result.stderr,encoding="utf-8")
    print(result.stdout,flush=True)
    if result.returncode or "FAILURES!!!" in result.stdout or "OK (" not in result.stdout: raise RuntimeError("Device test failed: "+test)
if not args.compact and not args.workspace_only:
    instrument("PersistenceTest","fire-persistence-final")
    instrument("VoiceCompatibilityTest","fire-voice")
    require(adb("pull","/sdcard/Android/data/com.fixavora.app/files/voice-compatibility.json",str(LOG/"voice-compatibility.json")))
wifi=require(adb("shell","settings","get","global","wifi_on")).strip()
size=require(adb("shell","wm","size")).strip()
override=next((line.split(":",1)[1].strip() for line in size.splitlines() if line.startswith("Override")),None)
rotation=require(adb("shell","settings","get","system","user_rotation")).strip()
auto_rotation=require(adb("shell","settings","get","system","accelerometer_rotation")).strip()
try:
    if args.compact: require(adb("shell","wm","size","800x1280"))
    require(adb("shell","settings","put","system","accelerometer_rotation","0"))
    require(adb("shell","settings","put","system","user_rotation","0" if args.compact else "1"))
    require(adb("shell","svc","wifi","disable"))
    disabled=require(adb("shell","settings","get","global","wifi_on")).strip()
    instrument("CompactWorkspaceTest" if args.compact else "WorkspaceTest","fire-compact" if args.compact else "fire-workspace")
    raw=ROOT/"deliverables/store-assets/screenshots/raw/en-GB";raw.mkdir(parents=True,exist_ok=True)
    variant="compact" if args.compact else "tablet"
    require(adb("pull","/sdcard/Android/data/com.fixavora.app/files/screenshots/en-GB/"+variant,str(raw)))
    (LOG/("offline-"+variant+".json")).write_text(json.dumps({"wifi_before":wifi,"wifi_during":disabled,"status":"PASS" if disabled=="0" else "NOT TESTED","scope":"Core workspace journey with Wi-Fi disabled, not external document or speech-service connectivity"},indent=2),encoding="utf-8")
finally:
    if args.compact: adb("shell","wm","size",override or "reset")
    for key,value in [("user_rotation",rotation),("accelerometer_rotation",auto_rotation)]:
        if value=="null": adb("shell","settings","delete","system",key)
        else: adb("shell","settings","put","system",key,value)
    if wifi=="1": adb("shell","svc","wifi","enable")
