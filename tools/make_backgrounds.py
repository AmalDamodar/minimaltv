#!/usr/bin/env python3
"""Turns your own photos into the launcher's backgrounds.

    python3 -m pip install pillow
    python3 tools/make_backgrounds.py photo1.jpg photo2.jpg photo3.jpg ...

For every photo it writes two files into launcher/app/src/main/assets/bg/:
  bgN.jpg       the sharp background, 1920x1080
  bgN_blur.jpg  a pre-blurred, darkened copy that the frosted glass panels show (no live blur on a 1 GB TV)
It removes the old bg*.jpg files and updates Background.COUNT, so rebuild the launcher afterwards.
Use photos you have the right to use (your own, or a licence such as Unsplash/Pexels that allows it).
"""
import glob, os, re, sys

try:
    from PIL import Image, ImageFilter
except ImportError:
    sys.exit("This needs Pillow:  python3 -m pip install pillow")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "launcher/app/src/main/assets/bg")
BACKGROUND = os.path.join(ROOT, "launcher/app/src/main/java/com/minimaltv/home/Background.java")


def cover(im, w, h):
    s = max(w / im.width, h / im.height)
    im = im.resize((round(im.width * s), round(im.height * s)), Image.LANCZOS)
    x, y = (im.width - w) // 2, (im.height - h) // 2
    return im.crop((x, y, x + w, y + h))


def main(paths):
    if not paths:
        sys.exit(__doc__)
    for old in glob.glob(os.path.join(OUT, "bg*.jpg")):
        os.remove(old)
    os.makedirs(OUT, exist_ok=True)
    for i, p in enumerate(paths, 1):
        im = Image.open(p).convert("RGB")
        cover(im, 1920, 1080).save(os.path.join(OUT, f"bg{i}.jpg"), quality=84, optimize=True)
        blur = cover(im, 960, 540).filter(ImageFilter.GaussianBlur(26))
        blur = Image.blend(blur, Image.new("RGB", blur.size, (12, 12, 18)), 0.34)   # darker, so white text stays readable
        blur.save(os.path.join(OUT, f"bg{i}_blur.jpg"), quality=80, optimize=True)
        print(f"{i}: {p}")
    src = open(BACKGROUND).read()
    src = re.sub(r"static final int COUNT = \d+;", f"static final int COUNT = {len(paths)};", src)
    open(BACKGROUND, "w").write(src)
    print(f"{len(paths)} backgrounds written, Background.COUNT updated. Rebuild the launcher.")


main(sys.argv[1:])
