"""A look at the spells' masses of blocks without the game (a scratch tool): reads the block layouts LayoutDump wrote (lines of x y z block part)
and draws each from two corners, every face of a block that shows in its block's own colours (the average of its texture in the game's jar,
grass and leaves tinted as a plain's), shaded by its side, a little noise in it.

python masses_preview.py <dump dir> <minecraft client jar> <out dir>"""
import io
import math
import os
import sys
import zipfile

from PIL import Image, ImageDraw

TINTED = {'grass_block': (0x79, 0xC0, 0x5A), 'grass': (0x79, 0xC0, 0x5A), 'fern': (0x79, 0xC0, 0x5A), 'oak_leaves': (0x59, 0xAE, 0x30),
          'spruce_leaves': (0x61, 0x99, 0x61), 'jungle_leaves': (0x59, 0xAE, 0x30)}
ALIAS = {'water': 'water_still', 'snow_block': 'snow', 'smooth_sandstone': 'sandstone_top', 'smooth_quartz': 'quartz_block_bottom',
         'magma_block': 'magma', 'smooth_stone': 'smooth_stone', 'snow': 'snow', 'soul_campfire': 'soul_campfire_log_lit', 'soul_lantern': 'soul_lantern', 'chain': 'chain',
         'iron_bars': 'iron_bars', 'polished_blackstone_wall': 'polished_blackstone', 'hanging_roots': 'hanging_roots', 'powder_snow': 'powder_snow'}


def colours(jar, names):
    out = {}
    with zipfile.ZipFile(jar) as z:
        files = set(z.namelist())

        def avg(path):
            im = Image.open(io.BytesIO(z.read(path))).convert('RGBA')
            w, h = im.size
            im = im.crop((0, 0, w, min(h, w)))
            px = [p for p in list(im.getdata()) if p[3] > 40]
            if not px:
                return None
            return tuple(sum(p[i] for p in px) // len(px) for i in range(3))

        for n in names:
            base = ALIAS.get(n, n)
            got = {}
            for face, tries in (('top', [base + '_top', base, base.replace('_wood', '_log'), base + '_side']),
                                ('side', [base + '_side', base, base.replace('_wood', '_log'), base + '_top'])):
                for t in tries:
                    path = 'assets/minecraft/textures/block/%s.png' % t
                    if path in files:
                        c = avg(path)
                        if c:
                            got[face] = c
                            break
            if 'top' not in got:
                got = {'top': (200, 0, 200), 'side': (200, 0, 200)}
            tint = TINTED.get(n)
            if tint:
                for face in ('top',) if n == 'grass_block' else ('top', 'side'):
                    c = got[face]
                    got[face] = tuple(int(c[i] * tint[i] / 255 * 1.6) for i in range(3))
            out[n] = got
    return out


def load(path):
    blocks = {}
    with open(path) as f:
        for line in f:
            x, y, z, name, part = line.split()
            blocks[(int(x), int(y), int(z))] = (name, int(part))
    return blocks


SEETHROUGH = {'glass', 'ice', 'iron_bars', 'chain', 'soul_lantern', 'hanging_roots', 'grass', 'fern', 'snow', 'poppy', 'dandelion', 'soul_campfire',
              'polished_blackstone_wall', 'oak_leaves', 'spruce_leaves', 'jungle_leaves', 'azalea_leaves', 'flowering_azalea_leaves'}


def render(blocks, cols, yaw, scale, title):
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    # Isometric: x turned by yaw about up, then tipped 30 degrees.
    tilt = math.radians(32)
    ct, st = math.cos(tilt), math.sin(tilt)

    def project(x, y, z):
        rx, rz = x * cy - z * sy, x * sy + z * cy
        sx = rx
        sy_ = -(y * ct - rz * st)
        depth = y * st + rz * ct
        return sx, sy_, depth

    faces = []
    dirs = {'top': (0, 1, 0), 'bottom': (0, -1, 0), 'east': (1, 0, 0), 'west': (-1, 0, 0), 'south': (0, 0, 1), 'north': (0, 0, -1)}
    view = (sy * ct, st, cy * ct)  # toward the viewer, roughly
    view = (math.sin(math.radians(yaw)) * 0 + (-sy) * 0, 0, 0)
    for (x, y, z), (name, part) in blocks.items():
        for face, (dx, dy, dz) in dirs.items():
            n = blocks.get((x + dx, y + dy, z + dz))
            if n is not None and n[0] not in SEETHROUGH:
                continue
            # Only faces turned toward the camera.
            fx, fz = dx * cy - dz * sy, dx * sy + dz * cy
            toward = dy * st + fz * ct
            if toward <= 0.01:
                continue
            if face == 'top':
                q = [(x, y + 1, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)]
            elif face == 'bottom':
                q = [(x, y, z), (x + 1, y, z), (x + 1, y, z + 1), (x, y, z + 1)]
            elif face == 'east':
                q = [(x + 1, y, z), (x + 1, y + 1, z), (x + 1, y + 1, z + 1), (x + 1, y, z + 1)]
            elif face == 'west':
                q = [(x, y, z), (x, y + 1, z), (x, y + 1, z + 1), (x, y, z + 1)]
            elif face == 'south':
                q = [(x, y, z + 1), (x + 1, y, z + 1), (x + 1, y + 1, z + 1), (x, y + 1, z + 1)]
            else:
                q = [(x, y, z), (x + 1, y, z), (x + 1, y + 1, z), (x, y + 1, z)]
            pts = [project(*p) for p in q]
            depth = sum(p[2] for p in pts) / 4
            c = cols.get(name, {'top': (200, 0, 200), 'side': (200, 0, 200)})
            base = c['top'] if face in ('top', 'bottom') else c['side']
            light = {'top': 1.0, 'bottom': 0.5}.get(face, 0.62 + 0.25 * abs(fx))
            noise = 0.94 + 0.12 * ((x * 73856093 ^ y * 19349663 ^ z * 83492791) % 100) / 100
            col = tuple(max(0, min(255, int(v * light * noise))) for v in base)
            faces.append((depth, pts, col))
    faces.sort(key=lambda f: f[0])
    if not faces:
        return Image.new('RGB', (400, 300), (30, 34, 48))
    xs = [p[0] for f in faces for p in f[1]]
    ys = [p[1] for f in faces for p in f[1]]
    w = int((max(xs) - min(xs)) * scale) + 40
    h = int((max(ys) - min(ys)) * scale) + 60
    img = Image.new('RGB', (w, h), (30, 34, 48))
    for y in range(h):
        g = int(30 + 30 * y / h)
        ImageDraw.Draw(img).line([(0, y), (w, y)], fill=(g - 8, g - 4, g + 12))
    d = ImageDraw.Draw(img)
    ox, oy = -min(xs) * scale + 20, -min(ys) * scale + 40
    for depth, pts, col in faces:
        d.polygon([(p[0] * scale + ox, p[1] * scale + oy) for p in pts], fill=col)
    d.text((10, 10), title, fill=(230, 230, 230))
    return img


def main():
    dump, jar, out = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(out, exist_ok=True)
    files = sorted(f for f in os.listdir(dump) if f.endswith('.txt'))
    all_blocks = {f: load(os.path.join(dump, f)) for f in files}
    names = {n for b in all_blocks.values() for n, _ in b.values()}
    cols = colours(jar, names)
    for f, blocks in all_blocks.items():
        size = max(max(abs(c) for c in p) for p in blocks) if blocks else 1
        scale = max(2.0, min(12.0, 420.0 / (size * 2.2)))
        a = render(blocks, cols, 35, scale, f[:-4] + ' (front left)')
        b = render(blocks, cols, 215, scale, f[:-4] + ' (back right)')
        img = Image.new('RGB', (a.width + b.width, max(a.height, b.height)), (30, 34, 48))
        img.paste(a, (0, 0))
        img.paste(b, (a.width, 0))
        img.save(os.path.join(out, f[:-4] + '.png'))
        print('drew', f[:-4], len(blocks))


if __name__ == '__main__':
    main()
