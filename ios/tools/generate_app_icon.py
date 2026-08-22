"""Regenera el icono iOS de 1024 px a partir de la identidad visual de Android."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "LaCestaDeAuri" / "Assets.xcassets" / "AppIcon.appiconset" / "AppIcon.png"

image = Image.new("RGB", (1024, 1024), "#683499")
draw = ImageDraw.Draw(image)

# Destellos celestes muy suaves para mantener la paleta de la aplicación.
draw.ellipse((-170, -190, 470, 450), fill="#7541A3")
draw.ellipse((710, 700, 1190, 1180), fill="#5D2B8A")

# Asa y cuerpo de la cesta, inspirados en el icono vectorial de Android.
draw.arc((336, 188, 688, 548), start=180, end=360, fill="#DABEED", width=72)
draw.polygon([(252, 408), (772, 408), (720, 790), (304, 790)], fill="#FCF9FF")
draw.rounded_rectangle((280, 380, 744, 470), radius=42, fill="#E8F5FB")

# Trama morada de la cesta.
for x in (372, 500, 628):
    draw.rounded_rectangle((x, 492, x + 62, 716), radius=22, fill="#A96FD0")
draw.rounded_rectangle((300, 560, 724, 620), radius=24, fill="#DABEED")
draw.rounded_rectangle((310, 700, 714, 760), radius=24, fill="#DABEED")

OUTPUT.parent.mkdir(parents=True, exist_ok=True)
image.save(OUTPUT, format="PNG", optimize=True)
print(OUTPUT)
