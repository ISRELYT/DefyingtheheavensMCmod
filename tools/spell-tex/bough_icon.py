"""Return to the Bough's icon (textures/gui/spell/return_to_the_bough.png, 32 x 32), drawn as the spell-art icons are (tools/spell-art Icons): a
few bold shapes, shaded, outlined dark. A bough of leaves across the top, a peach hanging from it (its blush, its crease, its stem), and a
swirl of green-gold light rising up to it from below, the way the fruit was brought back. Run from the repository's root."""
import math
import os

from PIL import Image

OUT = 'src/main/resources/assets/defying-the-heavens/textures/gui/spell/return_to_the_bough.png'

px = [[0] * 32 for _ in range(32)]


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def fill(test, paint):
    for y in range(32):
        for x in range(32):
            cx, cy = x + 0.5, y + 0.5
            if test(cx, cy):
                c = paint(cx, cy)
                if c is not None:
                    px[y][x] = c


def seg_dist(x, y, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(x - (ax + dx * t), y - (ay + dy * t))


BARK, BARK_DARK = (124, 84, 52), (84, 54, 34)
LEAF, LEAF_LIGHT, LEAF_DARK = (86, 170, 64), (140, 214, 96), (52, 116, 44)
PEACH, PEACH_LIGHT, BLUSH = (246, 176, 150), (255, 214, 190), (226, 90, 92)
GLOW, GLOW_GOLD = (150, 255, 150), (255, 236, 140)

# The swirl of light rising to it, behind all else.
for k in range(90):
    t = k / 89.0
    a = t * math.pi * 3.2
    r = 7.5 * (1 - t) + 1.5
    x = 16 + math.cos(a) * r
    y = 30 - t * 13
    c = mix(GLOW, GLOW_GOLD, t)
    xi, yi = int(x), int(y)
    if 0 <= xi < 32 and 0 <= yi < 32 and px[yi][xi] == 0:
        px[yi][xi] = c
# The bough, across the top, leaning.
fill(lambda x, y: seg_dist(x, y, 2, 8, 29, 5) < 1.9, lambda x, y: mix(BARK, BARK_DARK, (y - 4) / 5))
# Its leaves: almond shapes off the bough.
for (lx, ly, ang) in ((6, 5.2, -0.9), (11, 9.5, 0.7), (21, 3.6, -0.6), (26, 8.2, 0.9), (16, 3.2, -1.3)):
    ca, sa = math.cos(ang), math.sin(ang)

    def leaf(x, y, lx=lx, ly=ly, ca=ca, sa=sa):
        u, v = (x - lx) * ca + (y - ly) * sa, -(x - lx) * sa + (y - ly) * ca
        return (u / 4.2) ** 2 + (v / 1.9) ** 2 < 1

    fill(leaf, lambda x, y, lx=lx, ly=ly: mix(LEAF_LIGHT, LEAF_DARK, (y - ly + 2) / 4))
# The peach's stem, down from the bough.
fill(lambda x, y: seg_dist(x, y, 16, 7, 16.5, 12) < 0.9, lambda x, y: BARK_DARK)
# The peach: round, a point at its foot, its blush, its crease.
def peach(x, y):
    dx, dy = x - 16.5, y - 18.5
    return dx * dx / 36 + dy * dy / 38 < 1 or (abs(dx) < 2.5 - (y - 23) * 0.9 and 23 <= y < 26)


fill(peach, lambda x, y: mix(PEACH_LIGHT, PEACH, (x - 12) / 9 + (y - 14) / 14))
fill(lambda x, y: peach(x, y) and math.hypot(x - 19.5, y - 20) < 3.6, lambda x, y: mix(BLUSH, PEACH, math.hypot(x - 19.5, y - 20) / 3.6))
fill(lambda x, y: peach(x, y) and abs(x - (16.2 + (y - 13) * 0.08)) < 0.5 and 13 < y < 24, lambda x, y: mix(BLUSH, (170, 60, 70), 0.4))
# A glint of light on it.
fill(lambda x, y: math.hypot(x - 14, y - 16) < 1.1, lambda x, y: (255, 240, 225))
# The outline: each clear pixel touching a drawn one takes that one's colour, darkened.
out = [row[:] for row in px]
for y in range(32):
    for x in range(32):
        if px[y][x]:
            continue
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 32 and 0 <= ny < 32 and px[ny][nx]:
                out[y][x] = mix(px[ny][nx], (10, 6, 8), 0.72)
                break
img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
for y in range(32):
    for x in range(32):
        if out[y][x]:
            img.putpixel((x, y), out[y][x] + (255,))
img.save(OUT)
print('wrote', OUT)
