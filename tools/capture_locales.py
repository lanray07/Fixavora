"""Capture localized real app journeys and native text compositions on a QA device.
Seeding replaces only Fixavora's debug data. Display and Wi-Fi are restored.
"""
import argparse, hashlib, json, os, subprocess
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
ADB=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))/'platform-tools/adb.exe'
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--locales',nargs='+',default=['en-GB']);p.add_argument('--compact',action='store_true');a=p.parse_args()
def adb(*cmd,check=True):
    r=subprocess.run([str(ADB),'-s',a.serial,*cmd],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=300)
    if check and r.returncode: raise RuntimeError(r.stderr)
    return r.stdout.strip()
def setting(key): return adb('shell','settings','get','system',key)
before={k:setting(k) for k in ['user_rotation','accelerometer_rotation']}
size=adb('shell','wm','size');override=next((s.split(':',1)[1].strip() for s in size.splitlines() if s.startswith('Override')),None)
wifi=adb('shell','settings','get','global','wifi_on')
variant='compact' if a.compact else 'tablet'
dest=ROOT/'deliverables/store-assets/screenshots';logs=ROOT/'deliverables/android/build-logs/locales';logs.mkdir(parents=True,exist_ok=True)
try:
    adb('shell','input','keyevent','224')
    adb('shell','settings','put','system','accelerometer_rotation','0')
    adb('shell','settings','put','system','user_rotation','0' if a.compact else '1')
    if a.compact: adb('shell','wm','size','800x1280')
    adb('shell','svc','wifi','disable')
    for tag in a.locales:
        result=adb('shell','am','instrument','-w','-e','class','com.fixavora.app.'+('CompactWorkspaceTest' if a.compact else 'WorkspaceTest'),'-e','locale',tag,'com.fixavora.app.test/androidx.test.runner.AndroidJUnitRunner')
        (logs/f'{tag}-{variant}.log').write_text(result,encoding='utf-8');print(tag,variant,result[-180:],flush=True)
        if 'OK (' not in result or 'FAILURES!!!' in result: raise RuntimeError('Journey failed: '+tag)
        raw=dest/'raw'/tag;composed=dest/tag;raw.mkdir(parents=True,exist_ok=True);composed.mkdir(parents=True,exist_ok=True)
        remote='/sdcard/Android/data/com.fixavora.app/files/screenshots/'+tag
        adb('pull',remote+'/'+variant,str(raw));adb('pull',remote+'/composed/'+variant,str(composed))
        files=list((composed/variant).glob('*.png'));assert len(files)==8
        manifest=[]
        for file in sorted(files):
            with Image.open(file) as img: dimensions=img.size
            assert dimensions==((800,1280) if a.compact else (1920,1200))
            manifest.append({'file':str(file.relative_to(ROOT)).replace('\\','/'),'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'dimensions':dimensions})
        (composed/f'{variant}-capture.json').write_text(json.dumps({'locale':tag,'variant':variant,'journey':'PASS','device':a.serial,'network':'Wi-Fi disabled','visual_review':'PENDING','files':manifest},indent=2),encoding='utf-8')
finally:
    if a.compact: adb('shell','wm','size',override or 'reset',check=False)
    for key,value in before.items():
        adb('shell','settings','delete','system',key,check=False) if value=='null' else adb('shell','settings','put','system',key,value,check=False)
    if wifi=='1': adb('shell','svc','wifi','enable',check=False)
