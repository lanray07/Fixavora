"""Original geometric vector artwork and raster icon exports. No stock or generated AI imagery."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "deliverables/brand-assets"
OUT.mkdir(parents=True, exist_ok=True)
navy, teal, ivory, amber = "#172A3A", "#246B73", "#F8F7F3", "#D9A45B"
house = '<path d="M180 470 500 200 820 470 M270 400V800H730V400"/><path d="M410 540H510V650H410Z M580 800V580H670V800"/>'
check = '<path d="m400 610 100 100 200-220"/>'
def svg(name, body, background=ivory):
    data = f'<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1000 1000"><rect width="1000" height="1000" fill="{background}"/><g fill="none" stroke="{teal}" stroke-width="28" stroke-linejoin="round" stroke-linecap="round">{body}</g></svg>'
    (OUT / f"{name}.svg").write_text(data, encoding="utf-8")
svg("property", house)
svg("onboarding", house + '<circle cx="780" cy="730" r="120" fill="#D9A45B" stroke="none"/><path d="m720 730 45 45 85-100" stroke="#172A3A"/>')
svg("maintenance", '<rect x="220" y="240" width="560" height="550" rx="35"/><path d="M220 390H780 M350 170V300 M650 170V300 m-280 220 80 80 190-210"/>')
svg("repair", '<path d="M650 220a180 180 0 0 0-200 230L220 680a70 70 0 0 0 100 100l230-230a180 180 0 0 0 230-200L660 470 530 340Z"/>')
svg("appliance", '<rect x="240" y="160" width="520" height="690" rx="40"/><circle cx="500" cy="570" r="170"/><path d="M240 330H760 M330 245H410 M610 245H660"/>')
svg("warranty", '<path d="M500 160 770 260V490q0 210-270 350Q230 700 230 490V260Z m-140 310 90 90 180-200"/>')
svg("contractor", '<circle cx="500" cy="360" r="130"/><path d="M240 820V730q0-200 260-200t260 200v90 M340 300q10-200 160-200t160 200 M300 310H700"/>')
svg("expenses", '<path d="M210 200V800H820 M340 700V530 M500 700V400 M660 700V270"/>')
svg("voice", '<rect x="410" y="190" width="180" height="390" rx="90"/><path d="M300 450a200 200 0 0 0 400 0 M500 650V810 M380 810H620"/>')
svg("assistant", '<path d="m500 160 90 250 250 90-250 90-90 250-90-250-250-90 250-90Z"/><path d="m800 150 20 60 60 20-60 20-20 60-20-60-60-20 60-20Z"/>')
svg("premium", house + '<circle cx="780" cy="720" r="130" fill="#D9A45B" stroke="none"/><path d="m710 720 50 50 90-110" stroke="#172A3A"/>')
svg("empty-state", '<rect x="230" y="240" width="540" height="570" rx="40"/><path d="M340 390H660 M340 510H570 M340 630H490"/><circle cx="750" cy="750" r="120" fill="#F8F7F3"/><path d="M690 750H810 M750 690V810"/>')
svg("splash", house)
svg("logo-mark", '<path d="M260 480 500 270 740 480 690 530V770H310V530Z" fill="#F8F7F3" stroke="none"/><path d="m390 560 100 100 160-190" stroke="#246B73" stroke-width="50"/>', teal)
for size in [1024, 512, 114]:
    scale = 4
    im = Image.new("RGBA", (size*scale, size*scale), teal)
    d = ImageDraw.Draw(im)
    def pts(seq): return [(int(x*size*scale/1000), int(y*size*scale/1000)) for x,y in seq]
    d.polygon(pts([(230,470),(500,240),(770,470),(710,540),(680,515),(680,780),(320,780),(320,515),(290,540)]), fill=ivory)
    d.line(pts([(390,560),(490,660),(650,470)]), fill=teal, width=int(size*scale*.055), joint="curve")
    im.resize((size,size), Image.Resampling.LANCZOS).save(OUT / f"app-icon-{size}.png")
font_path = Path("C:/Windows/Fonts/segoeui.ttf")
for name, dark in [("logo-light", False), ("logo-dark", True)]:
    im = Image.new("RGB", (1400, 400), navy if dark else ivory)
    d = ImageDraw.Draw(im)
    d.text((110, 90), "Fixavora", font=ImageFont.truetype(str(font_path), 150), fill=ivory if dark else navy)
    im.save(OUT / f"{name}.png")
im = Image.new("RGB", (1024,500), navy); d = ImageDraw.Draw(im)
d.text((80,150), "Fixavora", font=ImageFont.truetype(str(font_path), 110), fill=ivory)
d.line([(720,290),(770,340),(880,210)], fill=amber, width=22)
im.save(OUT / "amazon-promotional-1024x500.png")
(OUT / "RIGHTS.txt").write_text("Original geometric artwork authored for Fixavora. No third-party stock imagery. Font previews use the local system UI font; font files are not redistributed. App uses Android system typography. SVG source files are editable.\n", encoding="utf-8")
print(f"Generated {len(list(OUT.iterdir()))} original asset files")
