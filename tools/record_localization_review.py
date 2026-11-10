"""Record the explicitly performed AI review with immutable catalog/capture hashes.
This records an AI review, never a human or native-speaker certification.
"""
import argparse,hashlib,json
from pathlib import Path
from PIL import Image
from localize import TARGETS,load,placeholders
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--reviewer',required=True);a=p.parse_args()
def digest(value):return hashlib.sha256(json.dumps(value,ensure_ascii=False,sort_keys=True).encode('utf-8')).hexdigest()
base=load(ROOT/'localization/en-GB.json');reviews={};inventory=[]
for tag in TARGETS+['en-IN','es-MX']:
    data=base|load(ROOT/f'localization/{tag}.json') if tag.startswith('en-') else load(ROOT/f'localization/{"es-419" if tag=="es-MX" else tag}.json')
    assert set(data)==set(base) and all(placeholders(data[k])==placeholders(base[k]) for k in base)
    captures=[]
    for variant in ['tablet','compact']:
        path=ROOT/f'deliverables/store-assets/screenshots/{tag}/{variant}-capture.json'
        capture=load(path);assert capture.get('journey')=='PASS',(tag,variant)
        files=sorted((path.parent/variant).glob('*.png'));assert len(files)==8,(tag,variant)
        rows=[]
        for file in files:
            raw=ROOT/f'deliverables/store-assets/screenshots/raw/{tag}/{variant}/{file.name}'
            assert raw.is_file() and file.with_suffix('.svg').is_file()
            with Image.open(file) as im:dimensions=im.size
            assert dimensions==((1920,1200) if variant=='tablet' else (800,1280))
            row={'file':file.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'dimensions':dimensions,'raw':raw.relative_to(ROOT).as_posix(),'raw_sha256':hashlib.sha256(raw.read_bytes()).hexdigest()}
            rows.append(row);inventory.append({'locale':tag,'variant':variant,**row})
        capture.update(files=rows,visual_review='AI sample review plus real journey and native composition bounds',human_review='NOT PERFORMED')
        path.write_text(json.dumps(capture,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        captures.append({'variant':variant,'manifest_sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
    if tag in TARGETS:
        reviews[tag]={'status':'PASS','reviewer':a.reviewer,'method':'AI terminology/privacy review, real localized journeys, native composition bounds and script/layout samples','human_review':'NOT PERFORMED','source_sha256':digest(base),'catalog_sha256':digest(data),'captures':captures}
(ROOT/'localization/review.json').write_text(json.dumps(reviews,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(ROOT/'deliverables/store-assets/screenshots/manifest.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Recorded {len(reviews)} AI-reviewed catalogs and {len(inventory)} real screenshot compositions; human review not claimed')
