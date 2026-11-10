"""Run real compact journeys with larger text and dark/high-contrast preferences.
Uses disposable Fixavora debug fixtures and restores device settings in finally.
"""
import argparse,json,os,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ADB=Path(os.environ.get('ANDROID_HOME',str(Path.home()/'AppData/Local/Android/Sdk')))/'platform-tools/adb.exe'
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--locales',nargs='+',default=['de-DE','ar','ja-JP']);a=p.parse_args()
def adb(*cmd,check=True):
    r=subprocess.run([str(ADB),'-s',a.serial,*cmd],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=600)
    if check and r.returncode:raise RuntimeError(r.stderr)
    return r.stdout.strip()
before={key:adb('shell','settings','get','system',key) for key in ['font_scale','user_rotation','accelerometer_rotation']}
size=adb('shell','wm','size');override=next((s.split(':',1)[1].strip() for s in size.splitlines() if s.startswith('Override')),None)
wifi=adb('shell','settings','get','global','wifi_on')
out=ROOT/'deliverables/qa/layout-stress';out.mkdir(parents=True,exist_ok=True)
try:
    adb('shell','settings','put','system','font_scale','1.3')
    adb('shell','wm','size','800x1280')
    adb('shell','settings','put','system','accelerometer_rotation','0')
    adb('shell','settings','put','system','user_rotation','0')
    adb('shell','svc','wifi','disable')
    results=[]
    for tag in a.locales:
        result=adb('shell','am','instrument','-w','-e','class','com.fixavora.app.CompactWorkspaceTest','-e','locale',tag,'-e','style','dark-contrast-large','com.fixavora.app.test/androidx.test.runner.AndroidJUnitRunner')
        (out/f'{tag}.log').write_text(result,encoding='utf-8');print(tag,result[-180:],flush=True)
        if 'OK (' not in result or 'FAILURES!!!' in result:raise RuntimeError('Large-text journey failed: '+tag)
        dest=out/tag;dest.mkdir(exist_ok=True)
        adb('pull',f'/sdcard/Android/data/com.fixavora.app/files/screenshots/{tag}/compact-dark-contrast-large',str(dest))
        assert len(list((dest/'compact-dark-contrast-large').glob('*.png')))==8
        results.append({'locale':tag,'journey':'PASS','font_scale':1.3,'dark':True,'high_contrast':True,'visual_review':'PENDING'})
    (out/'results.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
finally:
    adb('shell','wm','size',override or 'reset',check=False)
    for key,value in before.items():
        adb('shell','settings','delete','system',key,check=False) if value=='null' else adb('shell','settings','put','system',key,value,check=False)
    if wifi=='1':adb('shell','svc','wifi','enable',check=False)
