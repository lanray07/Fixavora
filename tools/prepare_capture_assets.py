"""Copy validated localized public fixture copy into the instrumentation APK."""
import json
from pathlib import Path
from localize import TARGETS,load
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/"localization/screenshots"
base=load(source/"en-GB.json")
output=ROOT/"app/src/androidTest/assets/screenshots";output.mkdir(parents=True,exist_ok=True)
for locale in TARGETS + ["en-IN", "es-MX"]:
    data=base.copy() if locale.startswith("en-") else load(source/f"{'es-419' if locale=='es-MX' else locale}.json")
    if locale=="en-US":data={key:value.replace("organised","organized").replace("tap","faucet") for key,value in data.items()}
    if set(data)!=set(base) or not all(isinstance(v,str) and v.strip() for v in data.values()):
        raise SystemExit(f"BLOCKED: incomplete screenshot/fixture copy for {locale}")
    (output/f"{locale}.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print(f"Prepared {len(TARGETS)+2} complete localized capture fixtures")
