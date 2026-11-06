"""Generates the Quintana mark, logos and app icon artwork from one definition.

    pip install fonttools
    python brand/tools/build_brand.py Cinzel-Regular.ttf

Cinzel Regular is at https://github.com/google/fonts/tree/main/ofl/cinzel (a variable font;
instance it at wght=400 with `fonttools varLib.instancer`) or from Google Fonts. The tagline uses
Barlow SemiBold from the app's resources. Text is converted to outlines, so the SVGs do not need
the fonts.

Writes the SVGs in brand/ and the vector drawables of the app (logo, launcher foreground, themed
icon, notification icon). The PNGs next to the SVGs are renderings of them, e.g. with
`rsvg-convert -w 1500 brand/quintana-readme-banner.svg -o brand/quintana-readme-banner.png`.
"""
import math
import os
import sys

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
BRAND = os.path.join(ROOT, "brand")
DRAWABLE = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
BARLOW = os.path.join(ROOT, "app", "src", "main", "res", "font", "barlow_semibold.ttf")

# Palette
CHARCOAL = "#1B1B1B"
GOLD = "#D4AF37"
IVORY = "#F5EAD7"
RED = "#B22222"
INK = "#29231A"  # the stopwatch on light grounds
WHITE = "#FFFFFF"

# The mark on a 512 grid: a heraldic shield holding a stopwatch. A quarter of the dial is
# red, the part of the round already run.
SHIELD = "M256 44C214 82 156 100 86 104V262C86 364 166 432 256 474C346 432 426 364 426 262V104C356 100 298 82 256 44Z"
CX, CY, R = 256, 292, 96


def circle(cx, cy, r):
    return f"M{cx - r} {cy}A{r} {r} 0 1 0 {cx + r} {cy}A{r} {r} 0 1 0 {cx - r} {cy}Z"


def radial(angle, r1, r2):
    """A line along the radius at `angle` degrees from 12 o'clock."""
    a = math.radians(angle)
    x1, y1 = CX + math.sin(a) * r1, CY - math.cos(a) * r1
    x2, y2 = CX + math.sin(a) * r2, CY - math.cos(a) * r2
    return f"M{x1:.1f} {y1:.1f}L{x2:.1f} {y2:.1f}"


def wedge(r, start, end):
    """A pie slice, clockwise from `start` to `end` degrees from 12 o'clock."""
    def point(d):
        a = math.radians(d)
        return CX + math.sin(a) * r, CY - math.cos(a) * r

    (x1, y1), (x2, y2) = point(start), point(end)
    large = 1 if (end - start) % 360 > 180 else 0
    return f"M{CX} {CY}L{x1:.1f} {y1:.1f}A{r} {r} 0 {large} 1 {x2:.1f} {y2:.1f}Z"


def mark(shield=GOLD, watch=IVORY, quarter=RED, heavy=False):
    """The mark as shapes: dicts of path data and paint. Without `quarter` that quarter of the
    dial stays open, for one-color use. `heavy` thickens the lines for tiny sizes."""
    extra = 8 if heavy else 0
    shapes = [
        dict(d=SHIELD, stroke=shield, width=30 + extra, join="miter"),
        dict(d=f"M{CX} {CY - R - 6}V{CY - R - 30}", stroke=watch, width=26),
        dict(d=circle(CX, CY - R - 50, 20), stroke=watch, width=13 + extra / 2),
        dict(d=radial(-42, R + 14, R + 34) + radial(42, R + 14, R + 34), stroke=watch, width=26, cap="round"),
        dict(d=circle(CX, CY, R), stroke=watch, width=22 + extra),
        dict(d=wedge(68, 90, 360), fill=watch),
    ]
    if quarter:
        shapes.append(dict(d=wedge(68, 0, 90), fill=quarter))
    return shapes


def svg_shapes(shapes, indent="  "):
    out = []
    for s in shapes:
        attrs = [f'd="{s["d"]}"', f'fill="{s.get("fill", "none")}"']
        if "stroke" in s:
            attrs.append(f'stroke="{s["stroke"]}" stroke-width="{s["width"]}"')
            if s.get("cap"):
                attrs.append(f'stroke-linecap="{s["cap"]}"')
            if s.get("join"):
                attrs.append(f'stroke-linejoin="{s["join"]}" stroke-miterlimit="8"')
        out.append(f"{indent}<path {' '.join(attrs)}/>")
    return "\n".join(out)


def text_path(font_path, text, size, spacing, x, baseline, anchor="start"):
    """`text` as one SVG path, with `spacing` in px between letters. Returns (path, width)."""
    font = TTFont(font_path)
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    scale = size / font["head"].unitsPerEm
    advances = []
    for ch in text:
        advances.append(glyphs[cmap[ord(ch)]].width * scale)
    width = sum(advances) + spacing * (len(text) - 1)
    left = x - width / 2 if anchor == "middle" else x
    pen = SVGPathPen(glyphs)
    cursor = left
    for ch, advance in zip(text, advances):
        glyphs[cmap[ord(ch)]].draw(TransformPen(pen, (scale, 0, 0, -scale, cursor, baseline)))
        cursor += advance + spacing
    return pen.getCommands(), width, left


def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(content)


def svg(width, height, body):
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}">\n{body}\n</svg>\n'


def placed(shapes, x, y, scale):
    return f'<g transform="translate({x} {y}) scale({scale})">\n{svg_shapes(shapes, "    ")}\n</g>'


def rule(x1, x2, y, width):
    """The gold line under the wordmark with a lozenge in its middle."""
    mid = (x1 + x2) / 2
    d = width * 2.4
    return (f'<path d="M{x1:.0f} {y}H{x2:.0f}" stroke="{GOLD}" stroke-width="{width}"/>\n'
            f'<path d="M{mid:.0f} {y - d:.1f}l{d:.1f} {d:.1f}-{d:.1f} {d:.1f}-{d:.1f}-{d:.1f}z" fill="{GOLD}"/>')


def lockup(font, ground, watch, word):
    word_path, width, left = text_path(font, "QUINTANA", 112, 5, 455, 238)
    body = [f'<rect width="1200" height="420" fill="{ground}"/>',
            placed(mark(watch=watch), 52, 14, 0.76),
            f'<path d="{word_path}" fill="{word}"/>',
            rule(left + 6, left + width, 286, 5)]
    return svg(1200, 420, "\n".join(body))


def android_shapes(shapes, indent):
    out = []
    for s in shapes:
        lines = [f"{indent}<path"]
        if "fill" in s:
            lines.append(f'{indent}    android:fillColor="#FF{s["fill"][1:]}"')
        lines.append(f'{indent}    android:pathData="{s["d"]}"')
        if "stroke" in s:
            lines.append(f'{indent}    android:strokeColor="#FF{s["stroke"][1:]}"')
            if s.get("cap"):
                lines.append(f'{indent}    android:strokeLineCap="{s["cap"]}"')
            if s.get("join"):
                lines.append(f'{indent}    android:strokeLineJoin="{s["join"]}"')
                lines.append(f'{indent}    android:strokeMiterLimit="8"')
            lines.append(f'{indent}    android:strokeWidth="{s["width"]}"')
        lines[-1] += " />"
        out.append("\n".join(lines))
    return "\n".join(out)


def vector(comment, size, viewport, body):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            f"<!-- {comment} Generated by brand/tools/build_brand.py. -->\n"
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
            f'    android:viewportWidth="{viewport}"\n    android:viewportHeight="{viewport}">\n'
            f"{body}\n</vector>\n")


def adaptive(shapes):
    """The mark (drawn on 512) scaled into the 66 dp safe zone of an adaptive icon."""
    return ("    <group\n        android:scaleX=\"0.135\"\n        android:scaleY=\"0.135\"\n"
            "        android:translateX=\"19.44\"\n        android:translateY=\"19.3\">\n"
            f"{android_shapes(shapes, '        ')}\n    </group>")


def main(font):
    # Mark
    write(os.path.join(BRAND, "logo", "quintana-mark.svg"), svg(512, 512, svg_shapes(mark())))
    write(os.path.join(BRAND, "logo", "quintana-mark-mono.svg"),
          svg(512, 512, svg_shapes(mark(shield=CHARCOAL, watch=CHARCOAL, quarter=None))))

    # Logos
    write(os.path.join(BRAND, "logo", "quintana-logo-dark.svg"), lockup(font, CHARCOAL, IVORY, IVORY))
    write(os.path.join(BRAND, "logo", "quintana-logo-light.svg"), lockup(font, WHITE, INK, INK))

    # App icon: the store applies its own mask to the square one.
    icon = placed(mark(), 36, 34, 0.86)
    write(os.path.join(BRAND, "icon", "quintana-app-icon.svg"),
          svg(512, 512, f'<rect width="512" height="512" rx="92" fill="{CHARCOAL}"/>\n{icon}'))
    write(os.path.join(BRAND, "icon", "quintana-app-icon-square.svg"),
          svg(512, 512, f'<rect width="512" height="512" fill="{CHARCOAL}"/>\n{icon}'))

    # Splash artwork for store graphics
    word_path, width, left = text_path(font, "QUINTANA", 94, 9, 540, 1190, anchor="middle")
    write(os.path.join(BRAND, "splash", "quintana-splash.svg"), svg(1080, 1920, "\n".join([
        f'<rect width="1080" height="1920" fill="{CHARCOAL}"/>',
        '<path d="M0 1280Q540 1060 1080 1280V1920H0Z" fill="#211F1B"/>',
        placed(mark(), 284, 500, 1),
        f'<path d="{word_path}" fill="{IVORY}"/>',
        rule(left, left + width, 1245, 4),
    ])))

    # README banner
    word_path, width, left = text_path(font, "QUINTANA", 116, 6, 690, 265)
    tag_path, _, _ = text_path(BARLOW, "TRAIN WITH PRECISION", 27, 7, 710, 375)
    write(os.path.join(BRAND, "quintana-readme-banner.svg"), svg(1500, 500, "\n".join([
        '<defs><linearGradient id="g" x2="1" y2="1"><stop stop-color="#24221E"/>'
        f'<stop offset="1" stop-color="{CHARCOAL}"/></linearGradient></defs>',
        '<rect width="1500" height="500" fill="url(#g)"/>',
        '<path d="M0 410Q750 330 1500 410" stroke="#3A3326" stroke-width="2" fill="none"/>',
        placed(mark(), 178, 22, 0.88),
        f'<path d="{word_path}" fill="{IVORY}"/>',
        rule(left + 10, left + width, 315, 5),
        f'<path d="{tag_path}" fill="{GOLD}"/>',
    ])))

    # App drawables
    write(os.path.join(DRAWABLE, "logo.xml"),
          vector("The mark: a heraldic shield holding a stopwatch, see brand/.", 64, 512, android_shapes(mark(), "    ")))
    write(os.path.join(DRAWABLE, "ic_launcher_foreground.xml"),
          vector("Launcher icon and splash screen: the mark on charcoal.", 108, 108, adaptive(mark())))
    write(os.path.join(DRAWABLE, "ic_launcher_monochrome.xml"),
          vector("Themed icon of Android 13+, tinted by the system.", 108, 108,
                 adaptive(mark(shield=WHITE, watch=WHITE, quarter=None))))
    write(os.path.join(DRAWABLE, "ic_notification.xml"),
          vector("Status bar icon: the mark in one color with heavier lines.", 24, 512,
                 android_shapes(mark(shield=WHITE, watch=WHITE, quarter=None, heavy=True), "    ")))


if __name__ == "__main__":
    main(sys.argv[1])
