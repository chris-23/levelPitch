#!/usr/bin/env python3
"""Generate the LevelPitch launcher icon as pixel-art vector drawables.

A motorhome in side view stands on sloping ground, its rear wheel on a stepped
wedge so the vehicle is level; a spirit-level vial with a centred bubble floats
above it, in front of a retro pixel sunset.

Writes the three adaptive-icon layers (background, foreground, monochrome) as
VectorDrawables on a 48 x 48 grid (108 dp, 2.25 dp per pixel) and, with
--preview DIR, PNG previews with circle and squircle masks.

Usage: python3 tools/icon/make_icon.py [--preview DIR]
"""
import argparse
import math
from pathlib import Path

N = 48  # grid size; 108 dp / 48 = 2.25 dp per pixel
RES = Path(__file__).resolve().parents[2] / "app/src/main/res/drawable"

# Palette (shared with the in-app sprites where it overlaps).
OUTLINE = "2B2D42"
BODY = "F4F1E8"
BODY_SHADE = "D9D4C5"
GLASS = "2E4A7D"
SHINE = "8EC5FF"
WINDOW = "3A4660"
STRIPE = "E07A2E"
STRIPE_DARK = "B35A1C"
SKIRT = "4A4E5A"
VENT = "B8BDC7"
SOLAR = "1F3B73"
TYRE = "1E1E1E"
RIM = "6B7078"
HUB = "C9CED6"
HEADLIGHT = "FFE08A"
TAILLIGHT = "D94A3A"
WEDGE = "F2B233"
WEDGE_TOP = "FFD86B"
WEDGE_DARK = "A66E14"
VIAL = "35D399"
VIAL_LIGHT = "A8F0CF"
BUBBLE = "F2FFF8"
GRASS_TOP = "7BCB8F"
GRASS = "3E8E5E"
GRASS_DARK = "2F6E48"
SKY = ["1B1F3B", "262A52", "3A3266", "5A3A70", "8A4772", "BF5A6C", "E57B5D", "F4A460"]
SUN_TOP = "FFE38A"
SUN_BOTTOM = "F7934C"
STAR = "F4E9C9"

# Colours that make up the monochrome (themed) icon; glass and ground fill become holes.
MONO_KEEP = {OUTLINE, BODY, BODY_SHADE, STRIPE, STRIPE_DARK, SKIRT, VENT, SOLAR, TYRE, RIM, HUB,
             HEADLIGHT, TAILLIGHT, WEDGE, WEDGE_TOP, WEDGE_DARK, VIAL, VIAL_LIGHT, BUBBLE, GRASS_TOP}


class Grid:
    def __init__(self):
        self.px = {}

    def set(self, x, y, c):
        if 0 <= x < N and 0 <= y < N:
            self.px[(x, y)] = c

    def fill(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.set(xx, yy, c)

    def disc(self, cx, cy, r, c):
        for y in range(int(cy - r) - 1, int(cy + r) + 2):
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                    self.set(x, y, c)


def mix(a, b, t):
    ca = [int(a[i:i + 2], 16) for i in (0, 2, 4)]
    cb = [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return "".join(f"{round(x + (y - x) * t):02X}" for x, y in zip(ca, cb))


def background():
    g = Grid()
    # Sky in bands, dithered at each boundary so it reads as a pixel gradient.
    band = N / len(SKY)
    for y in range(N):
        i = min(int(y / band), len(SKY) - 1)
        for x in range(N):
            c = SKY[i]
            if i + 1 < len(SKY) and y == int((i + 1) * band) - 1 and (x + y) % 2 == 0:
                c = SKY[i + 1]
            g.set(x, y, c)
    # Retro sun with cut lines in its lower half, setting behind the cab.
    cx, cy, r = 33.0, 16.0, 7.0
    for y in range(N):
        for x in range(N):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                t = (y - (cy - r)) / (2 * r)
                cut = y > cy and (y - int(cy)) % 2 == 0
                if not cut:
                    g.set(x, y, mix(SUN_TOP, SUN_BOTTOM, t))
    for x, y in [(12, 11), (16, 8), (10, 15), (24, 7), (19, 5), (40, 7), (6, 9), (29, 4)]:
        g.set(x, y, STAR)
    return g


def ground_top(x):
    """Ground falls towards the rear: 2 px lower under the rear wheel than under the front."""
    if x <= 20:
        return 34
    if x <= 24:
        return 33
    return 32


REAR_X, FRONT_X, AXLE_Y, TYRE_R = 16.0, 31.0, 29.0, 2.95  # r 2.95 gives a round 6 x 6 tyre


def foreground(with_ground_fill=True):
    """Everything must stay inside the 66 dp safe circle: radius ~14.7 px around (24, 24)."""
    g = Grid()
    # Ground: full width (the launcher mask trims it), with a lighter top edge.
    for x in range(N):
        top = ground_top(x)
        g.set(x, top, GRASS_TOP)
        if with_ground_fill:
            for y in range(top + 1, N):
                g.set(x, y, GRASS_DARK if (x * 7 + y * 3) % 11 == 0 else GRASS)

    # Stepped wedge under the rear wheel: low step at the back, the wheel on the top step.
    for x0, x1, h in [(11, 12, 1), (13, 19, 2)]:
        top = 34 - h
        g.fill(x0, top, x1 - x0 + 1, h, WEDGE)
        g.fill(x0, top, x1 - x0 + 1, 1, WEDGE_TOP)
        g.fill(x0, top, 1, h, WEDGE_DARK)

    # Body, level: rear wall at x=12, roof at y=16, windscreen sloping down to the bonnet.
    def right_edge(y):
        return min(31 + (y - 16), 35)

    for y in range(16, 29):
        for x in range(12, right_edge(y) + 1):
            edge = y in (16, 28) or x in (12, right_edge(y))
            g.set(x, y, OUTLINE if edge else BODY)
    for y in range(16, 21):  # keep the slope a clean diagonal
        g.set(right_edge(y), y, OUTLINE)
    for y in range(17, 21):  # windscreen with a glint
        for x in range(30, right_edge(y)):
            g.set(x, y, GLASS)
    g.set(31, 18, SHINE)
    g.set(32, 19, SHINE)
    # Side windows, door, stripe, skirt, lights, mirror.
    g.fill(14, 18, 3, 2, WINDOW)
    g.set(14, 18, SHINE)
    g.fill(19, 18, 5, 2, WINDOW)
    g.set(19, 18, SHINE)
    g.fill(13, 23, 22, 1, STRIPE)
    g.fill(13, 24, 22, 1, STRIPE_DARK)
    for y in range(18, 28):
        g.set(26, y, OUTLINE)
        g.set(28, y, OUTLINE)
    g.set(27, 18, OUTLINE)
    g.fill(13, 26, 22, 2, SKIRT)
    g.set(34, 22, HEADLIGHT)
    g.fill(13, 20, 1, 2, TAILLIGHT)
    g.fill(36, 19, 1, 2, OUTLINE)
    # Roof: AC unit and a solar panel.
    g.fill(15, 14, 4, 1, OUTLINE)
    g.fill(15, 15, 4, 1, VENT)
    g.fill(21, 15, 7, 1, SOLAR)

    # Wheels over the skirt: tyre, rim, hub; bottoms rest on the wedge and the ground.
    for cx in (REAR_X, FRONT_X):
        g.disc(cx, AXLE_Y, TYRE_R, TYRE)
        g.disc(cx, AXLE_Y, 1.6, RIM)
        g.disc(cx, AXLE_Y, 0.75, HUB)

    # Spirit-level vial above the roof, bubble dead centre between the marks: level.
    g.fill(20, 10, 9, 1, OUTLINE)
    g.fill(20, 12, 9, 1, OUTLINE)
    g.set(19, 11, OUTLINE)
    g.set(29, 11, OUTLINE)
    g.fill(20, 11, 9, 1, VIAL)
    g.fill(23, 11, 3, 1, BUBBLE)
    g.set(22, 11, OUTLINE)
    g.set(26, 11, OUTLINE)
    return g


def check_rests_on_ground(g):
    """Both tyres touch what is below them: the wedge's top step or the ground."""
    for cx in (int(REAR_X), int(FRONT_X)):
        bottom = max(y for (x, y), c in g.px.items() if c == TYRE and x == cx)
        below = g.px.get((cx, bottom + 1))
        assert below in (WEDGE_TOP, GRASS_TOP), f"wheel at {cx} floats: {below}"


def to_vector(g, only=None, colour=None):
    """VectorDrawable with one path per colour, pixels merged into horizontal runs."""
    by_colour = {}
    for (x, y), c in g.px.items():
        if c is None or (only is not None and c not in only):
            continue
        by_colour.setdefault(colour or c, set()).add((x, y))
    paths = []
    for c, cells in sorted(by_colour.items()):
        d = []
        for y in range(N):
            x = 0
            while x < N:
                if (x, y) in cells:
                    start = x
                    while (x, y) in cells:
                        x += 1
                    d.append(f"M{start},{y}h{x - start}v1h{start - x}z")
                else:
                    x += 1
        paths.append(f'    <path android:fillColor="#{c}" android:pathData="{"".join(d)}" />')
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/icon/make_icon.py; edit the script, not this file. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        f'    android:viewportWidth="{N}"\n    android:viewportHeight="{N}">\n'
        + "\n".join(paths) + "\n</vector>\n"
    )


def preview(bg, fg, out_dir):
    from PIL import Image, ImageDraw

    scale = 10
    img = Image.new("RGBA", (N, N))
    for layer in (bg, fg):
        for (x, y), c in layer.px.items():
            if c is not None:
                img.putpixel((x, y), tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    big = img.resize((N * scale, N * scale), Image.NEAREST)
    # Launchers show the central 72 of 108 dp.
    inset = round(N * scale * 18 / 108)
    visible = big.crop((inset, inset, N * scale - inset, N * scale - inset))
    size = visible.size[0]
    sheet = Image.new("RGBA", (size * 2 + 60, size + 40), (240, 240, 240, 255))
    for i, shape in enumerate(("circle", "squircle")):
        mask = Image.new("L", (size, size), 0)
        d = ImageDraw.Draw(mask)
        if shape == "circle":
            d.ellipse((0, 0, size - 1, size - 1), fill=255)
        else:
            d.rounded_rectangle((0, 0, size - 1, size - 1), radius=size // 4, fill=255)
        sheet.paste(visible, (20 + i * (size + 20), 20), mask)
    out_dir.mkdir(parents=True, exist_ok=True)
    sheet.save(out_dir / "icon-preview.png")
    big.save(out_dir / "icon-full-layer.png")
    # Store listing size, for later.
    visible.resize((512, 512), Image.NEAREST).save(out_dir / "icon-512.png")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", type=Path, help="write PNG previews to this directory")
    args = ap.parse_args()
    bg, fg = background(), foreground()
    check_rests_on_ground(fg)
    (RES / "ic_launcher_background.xml").write_text(to_vector(bg))
    (RES / "ic_launcher_foreground.xml").write_text(to_vector(fg))
    (RES / "ic_launcher_monochrome.xml").write_text(to_vector(foreground(with_ground_fill=False), only=MONO_KEEP, colour="FFFFFF"))
    if args.preview:
        preview(bg, fg, args.preview)


if __name__ == "__main__":
    main()
