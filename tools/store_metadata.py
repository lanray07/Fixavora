"""Validate and export reviewed store catalogs. Never publishes a listing."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'localization/store';OUT=ROOT/'deliverables/store-assets/metadata'
OUT.mkdir(parents=True,exist_ok=True)
for file in sorted(SOURCE.glob('*.json')):
    data=json.loads(file.read_text(encoding='utf-8'))
    assert data['locale']==file.stem
    for field,limit in [('display_title',200),('short_description',1200),('long_description',4000)]:
        assert isinstance(data[field],str) and 0<len(data[field])<=limit,(file.name,field)
    assert 1<=len(data['product_feature_bullets'])<=10
    assert len(data['short_description'].encode('utf-8'))<=2000
    assert len(data['screenshot_headlines'])==8
    assert data['privacy_policy'].startswith('https://github.com/lanray07/Fixavora/')
    assert data['support_email']=='banksmi@mail.com'
    (OUT/file.name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Validated and exported {len(list(SOURCE.glob("*.json")))} localized store catalogs; publishing is separate')
