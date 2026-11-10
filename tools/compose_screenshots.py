"""Compose only captured Fixavora screens. Never renders simulated app UI.

Input: deliverables/store-assets/screenshots/raw/en-GB/{tablet,compact}/*.png
Output: Amazon dimension candidate PNGs plus editable SVG composition templates.
Run after WorkspaceTest and adb pull; inspect every result before store approval.
"""
import csv, hashlib, json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from xml.sax.saxutils import escape
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "deliverables/store-assets/screenshots"
headlines = [
    ("Your home, thoughtfully organised", "Maintenance, repairs and useful details in one place."),
    ("Give every task a place", "Plan recurring maintenance around your home."),
    ("Keep repair details together", "Record progress, dates and contractor details."),
    ("Know your appliance records", "Keep model details and warranty dates close."),
    ("Speak or type a task draft", "Review the details before saving. Voice needs a supported engine."),
    ("See the month ahead", "A clear calendar of saved maintenance tasks."),
    ("Keep your records on your device", "Your core home records work offline."),
    ("A clear view of your home history", "Preview saved records; Premium adds PDF and CSV exports.")
]
rows = []
for variant, size in [("tablet", (1920,1200)), ("compact", (800,1280))]:
    files = sorted((OUT / "raw/en-GB" / variant).glob("*.png"))
    if not files: continue
    assert len(files) == 8, f"Expected eight real captures for {variant}; got {len(files)}"
    folder = OUT / "en-GB" / variant; folder.mkdir(parents=True, exist_ok=True)
    for source, (title, caption) in zip(files, headlines):
        capture = Image.open(source).convert("RGB")
        if variant == "tablet": margin, top, available, font_size = 80, 210, (1760, 920), 54
        else: margin, top, available, font_size = 38, 200, (724,1020), 35
        canvas = Image.new("RGB", size, "#F8F7F3"); draw = ImageDraw.Draw(canvas)
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", font_size)
        small = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 26 if variant == "tablet" else 21)
        def wrap(s, f, max_width):
            lines=[]; line=""
            for word in s.split():
                candidate=(line+" "+word).strip()
                if draw.textlength(candidate, font=f) > max_width and line: lines.append(line); line=word
                else: line=candidate
            lines.append(line); return lines
        y=30
        text_svg=[]
        for line in wrap(title,font,size[0]-2*margin):
            draw.text((margin,y),line,font=font,fill="#172A3A")
            text_svg.append(f'<text x="{margin}" y="{y+font_size}" font-family="sans-serif" font-size="{font_size}" fill="#172A3A">{escape(line)}</text>')
            y += font_size+6
        for line in wrap(caption,small,size[0]-2*margin):
            draw.text((margin,y+6),line,font=small,fill="#246B73")
            text_svg.append(f'<text x="{margin}" y="{y+6+small.size}" font-family="sans-serif" font-size="{small.size}" fill="#246B73">{escape(line)}</text>')
            y+=small.size+5
        top=max(top,y+30)
        available=(available[0],size[1]-top-40)
        capture.thumbnail(available,Image.Resampling.LANCZOS)
        left=(size[0]-capture.width)//2
        draw.rectangle((left-2,top-2,left+capture.width+2,top+capture.height+2),fill="#DCE6E8")
        canvas.paste(capture,(left,top))
        target=folder/source.name; canvas.save(target)
        svg = f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{size[0]}" height="{size[1]}"><rect width="100%" height="100%" fill="#F8F7F3"/>' + ''.join(text_svg) + f'<image x="{left}" y="{top}" width="{capture.width}" height="{capture.height}" xlink:href="../../raw/en-GB/{variant}/{source.name}"/></svg>'
        target.with_suffix(".svg").write_text(svg,encoding="utf-8")
        rows.append(dict(locale="en-GB",variant=variant,raw=str(source.relative_to(ROOT)),file=str(target.relative_to(ROOT)),width=size[0],height=size[1],sha256=hashlib.sha256(target.read_bytes()).hexdigest(),status="NOT TESTED",notes="Real device capture; composition requires final visual and listing review. Compact is a Fire tablet viewport simulation, not physical phone validation."))
OUT.mkdir(parents=True,exist_ok=True)
(OUT/"manifest.json").write_text(json.dumps(rows,indent=2),encoding="utf-8")
print(f"Composed {len(rows)} files from real captures. No other locale screenshots are claimed.")
