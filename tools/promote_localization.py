"""Validate and stage AI-translated catalogs with explicit terminology corrections.
This does not approve layouts or mark any locale human-reviewed.
"""
import hashlib,json,re
from pathlib import Path
from localize import TARGETS,load,placeholders
ROOT=Path(__file__).resolve().parents[1]
base=load(ROOT/"localization/en-GB.json")
shots=load(ROOT/"localization/screenshots/en-GB.json")
source={**base,**{"shot:"+k:v for k,v in shots.items()}}
ordinals={key:str(i+1).zfill(3) for i,key in enumerate(source)}
overrides=load(ROOT/"localization/release-overrides.json")
aliases={"es-419":"es-ES","fr-CA":"fr-FR"}
summary=[]
for tag in TARGETS:
    if tag.startswith("en-"):continue
    parent=aliases.get(tag,tag)
    data=load(ROOT/f".runtime/google-candidates/{parent}.json")
    if set(data)!=set(source):raise SystemExit(f"BLOCKED: candidate keys differ for {tag}")
    for key,value in data.items():
        value=re.sub(r"[\u200b\u202a-\u202e\u2066-\u2069\ufeff]","",value)
        value=re.sub(r"\s*\n\s*"," ",value).strip()
        # Explicitly numbered translation rows are transport markers, never app copy.
        marker=ordinals[key]
        if value.startswith(marker) and not source[key].startswith(marker):value=value[len(marker):].lstrip(" |｜")
        if tag=="ja-JP":value=value.replace("プロパティ","物件").replace("アプライアンス","家電").replace("レコード","記録")
        if tag=="hi-IN":value=value.replace("फिक्सवोरा","Fixavora").replace("फिक्सावोरा","Fixavora")
        data[key]=value
    data.update(overrides.get(parent,{}));data.update(overrides.get(tag,{}))
    data["shot:warranty"]=data["warranty"]+": "+data["shot:appliance"]
    problems=[key for key,value in data.items() if not value or placeholders(value)!=placeholders(source[key]) or re.search(r"\b\d{3}\s*[|｜]",value) or re.search(r"(.{5,30})\1{3}",value)]
    if problems:raise SystemExit(f"BLOCKED: invalid candidate strings for {tag}: {problems}")
    ui={key:data[key] for key in base}; artwork={key:data["shot:"+key] for key in shots}
    for path,content in [(ROOT/f"localization/{tag}.json",ui),(ROOT/f"localization/screenshots/{tag}.json",artwork)]:
        path.write_text(json.dumps(content,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    summary.append({"locale":tag,"ui_strings":len(ui),"screenshot_strings":len(artwork),"method":"Google Translate web UI with Codex terminology and privacy corrections","human_review":"NOT PERFORMED","status":"PENDING_LAYOUT_QA","source_sha256":hashlib.sha256(json.dumps(base,sort_keys=True).encode()).hexdigest()})
out=ROOT/"deliverables/localization/ai-review.json";out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(f"Staged {len(summary)} complete AI candidate locales; layout and release checks remain required")
