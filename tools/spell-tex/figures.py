"""The two Impermanences' skins (textures/entity/spell/<id>.png), painted onto their models' boxes (models/figure/<id>.json, box UV as vanilla
lays it out), and a look at them without the game: `python figures.py paint` writes the skins; `python figures.py preview <out dir>` draws
each figure turning round, and striking, as GIFs (orthographic, two lights, as the entity shader lights a model). Every run the same.

Run from the repository's root (PIL and numpy wanted)."""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image

ASSETS = 'src/main/resources/assets/defying-the-heavens'
IDS = ['white_impermanence_figure', 'black_impermanence_figure']


def load(fig):
    with open(os.path.join(ASSETS, 'models/figure', fig + '.json'), encoding='utf-8') as f:
        return json.load(f)


# --- Box UV (vanilla's ModelPart.Cube): where each face of a box lies on its texture ---

def faces(box):
    """The texture's rect (x, y, w, h) of each face of a box: top and bottom as the world sees them, then its right, front, left and back."""
    u, v = box['uv']
    w, h, d = [int(round(s)) for s in box['size']]
    return {
        'top': (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h),
        'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h),
        'back': (u + 2 * d + w, v + d, w, h),
    }


# --- Paint ---

def hexrgb(c, a=255):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255, a)


def shade(c, f):
    r, g, b = (c >> 16) & 255, (c >> 8) & 255, c & 255
    return (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)))


class Canvas:
    def __init__(self, w, h, seed):
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)
        # What glows of itself (drawn again over the figure full bright, as a glow mask is): its eyes, its marks.
        self.glow = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.gpx = self.glow.load()

    def shine(self, x, y):
        self.gpx[x, y] = self.px[x, y]

    def fill(self, rect, colour, folds=0.0, fold_every=2, alpha=255, jitter=0.04):
        x0, y0, w, h = rect
        for x in range(w):
            fold = 1.0 - folds * (0.5 + 0.5 * math.cos(x * math.pi * 2 / max(1, fold_every * 2))) if folds else 1.0
            for y in range(h):
                f = fold * (1 + self.rnd.uniform(-jitter, jitter))
                r, g, b = shade(colour, f)
                self.px[x0 + x, y0 + y] = (r, g, b, alpha)

    def put(self, x, y, colour, alpha=255):
        r, g, b = (colour >> 16) & 255, (colour >> 8) & 255, colour & 255
        self.px[x, y] = (r, g, b, alpha)

    def pattern(self, rect, rows, palette):
        x0, y0, w, h = rect
        for y, row in enumerate(rows[:h]):
            for x, ch in enumerate(row[:w]):
                if ch in palette and palette[ch] is not None:
                    c = palette[ch]
                    if isinstance(c, tuple):
                        self.px[x0 + x, y0 + y] = c
                    else:
                        self.put(x0 + x, y0 + y, c)

    def fade_bottom(self, rect, rows):
        """The last {rows} rows of a face fading away (a hem dissolving into the mist)."""
        x0, y0, w, h = rect
        for k in range(rows):
            y = y0 + h - 1 - k
            a = int(255 * (k + 1) / (rows + 1))
            for x in range(w):
                r, g, b, aa = self.px[x0 + x, y]
                self.px[x0 + x, y] = (r, g, b, min(aa, a))

    def clear(self, rect):
        x0, y0, w, h = rect
        for x in range(w):
            for y in range(h):
                self.px[x0 + x, y0 + y] = (0, 0, 0, 0)


def part(model, name):
    for p in model['parts']:
        if p['name'] == name:
            return p
    return None


def paint(fig):
    model = load(fig)
    tw, th = model['texture']
    white = fig.startswith('white')
    c = Canvas(tw, th, 7 if white else 11)
    robe = 0xF1F1EA if white else 0x1F1E25
    robe_fold = 0.13 if white else 0.3
    trim = 0xB9C6CF if white else 0x5B1820
    skin = 0xEEF1F3 if white else 0x3B3643
    hair = 0x131216
    hat = 0xF5F5F0 if white else 0x18181D
    strip = 0xB3242C if white else 0xEDEBE4
    mark = 0xE7C24A if white else 0x2A1C1C
    sash = 0xC9B57A if white else 0x6B1A22

    # The head: skin, the hair black over its top and back and down its sides; the face its own.
    head = faces(part(model, 'head')['boxes'][0])
    for k in ('right', 'left', 'back', 'bottom'):
        c.fill(head[k], skin, jitter=0.02)
    c.fill(head['top'], hair, jitter=0.06)
    c.fill(head['back'], hair, jitter=0.06)
    for k in ('right', 'left'):
        x0, y0, w, h = head[k]
        c.fill((x0, y0, w, 3), hair, jitter=0.06)
        c.fill((x0 + (w - 3 if k == 'right' else 0), y0 + 3, 3, h - 3), hair, jitter=0.06)
    if white:
        rows = ['KKKKKKKK',
                'KSSSSSSK',
                'SbbSSbbS',
                'SKKSSKKS',
                'SSSSSSSS',
                'SPSSSSPS',
                'SSMRRMSS',
                'SSSRRSSS']
        pal = {'K': hair, 'S': skin, 'b': 0x8C8F96, 'P': 0xF0B4BC, 'M': 0x7A1018, 'R': 0xC8202C}
    else:
        rows = ['KKKKKKKK',
                'KDDDDDDK',
                'BBDDDDBB',
                'DWrDDrWD',
                'DDDDDDDD',
                'DDDddDDD',
                'DDMMMMDD',
                'DMDDDDMD']
        pal = {'K': hair, 'D': skin, 'B': 0x0B0A0E, 'W': 0xE8E4DC, 'r': 0xC0141C, 'd': 0x2C2833, 'M': 0x140F16}
    c.pattern(head['front'], rows, pal)
    x0, y0, w, h = head['front']
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in ('W', 'r'):
                c.shine(x0 + x, y0 + y)

    # The hat: tall, its brim banded, a strip down its front with the characters on it (as marks: too small for strokes).
    hatp = part(model, 'hat')
    for i, box in enumerate(hatp['boxes']):
        f = faces(box)
        for k, r in f.items():
            c.fill(r, hat, folds=0.06 if white else 0.12, fold_every=3, jitter=0.03)
        if i == 0:
            for k in ('right', 'front', 'left', 'back'):
                x0, y0, w, h = f[k]
                c.fill((x0, y0 + 1, w, 1), 0x15151A if white else 0x6E6A64, jitter=0.02)
        else:
            x0, y0, w, h = f['front']
            sx = x0 + w // 2 - 1
            c.fill((sx, y0, 2, h), strip, jitter=0.03)
            for y in range(1, h - 1, 2):
                c.put(sx + (y // 2) % 2, y0 + y, mark)
                c.shine(sx + (y // 2) % 2, y0 + y)
    # The hair hanging down its back, the tongue (the White's) hanging from its mouth.
    hp = part(model, 'hair')
    for k, r in faces(hp['boxes'][0]).items():
        c.fill(r, hair, folds=0.25, fold_every=1, jitter=0.08)
    hf = faces(hp['boxes'][0])
    c.fade_bottom(hf['back'], 3)
    c.fade_bottom(hf['front'], 3)
    tp = part(model, 'tongue')
    if tp:
        for k, r in faces(tp['boxes'][0]).items():
            c.fill(r, 0xC8202C, jitter=0.05)
        x0, y0, w, h = faces(tp['boxes'][0])['front']
        for y in range(h):
            c.put(x0 + w // 2 - (y % 2), y0 + y, 0x8E1018)
        c.fill((x0, y0 + h - 2, w, 2), 0xE0505A, jitter=0.03)

    # The robe: torso with its crossed collar, sash, skirt and hem, its folds running down it, the hem dissolving into mist.
    body = part(model, 'body')
    torso = faces(body['boxes'][0])
    for k, r in torso.items():
        c.fill(r, robe, folds=robe_fold * 0.6, fold_every=2)
    x0, y0, w, h = torso['front']
    for y in range(min(h, 6)):
        for x in (w // 2 - 1 - y, w // 2 + y):
            if 0 <= x < w:
                c.put(x0 + x, y0 + y, trim)
    c.fill((x0, y0, w, 1), trim, jitter=0.02)
    for k, r in faces(body['boxes'][1]).items():
        c.fill(r, sash, folds=0.1, fold_every=1, jitter=0.06)
    skirt = part(model, 'skirt')
    for i, box in enumerate(skirt['boxes']):
        f = faces(box)
        for k, r in f.items():
            c.fill(r, robe, folds=robe_fold, fold_every=2)
        if i == 1:
            for k in ('right', 'front', 'left', 'back'):
                x0, y0, w, h = f[k]
                c.fill((x0, y0 + h - 4, w, 1), trim, jitter=0.02)
                c.fade_bottom(f[k], 3)
            c.clear(f['bottom'])
        else:
            x0, y0, w, h = f['front']
            c.fill((x0 + w // 2, y0, 1, h), shade(robe, 0.8)[0] << 16 | shade(robe, 0.8)[1] << 8 | shade(robe, 0.8)[2], jitter=0.02)

    # The sleeves, wide, dark within; the hands.
    for arm in ('right_arm', 'left_arm'):
        ap = part(model, arm)
        upper, sleeve, hand = [faces(b) for b in ap['boxes']]
        for k, r in upper.items():
            c.fill(r, robe, folds=robe_fold * 0.7, fold_every=2)
        for k, r in sleeve.items():
            c.fill(r, robe, folds=robe_fold, fold_every=2)
        for k in ('right', 'front', 'left', 'back'):
            x0, y0, w, h = sleeve[k]
            c.fill((x0, y0 + h - 1, w, 1), trim, jitter=0.02)
        c.fill(sleeve['bottom'], shade_int(robe, 0.35))
        for k, r in hand.items():
            c.fill(r, skin, jitter=0.03)

    # The White's mourning staff, a stick wrapped in white paper, hung with streamers of it; the Black's plaque of summons.
    sp = part(model, 'staff')
    if sp:
        for k, r in faces(sp['boxes'][0]).items():
            x0, y0, w, h = r
            for y in range(h):
                for x in range(w):
                    wrapped = (y + x) % 3 != 0
                    c.put(x0 + x, y0 + y, 0xEFEFE8 if wrapped else 0x6B5236)
        for box in part(model, 'streamers')['boxes']:
            for k, r in faces(box).items():
                if r[2] <= 0 or r[3] <= 0:
                    continue
                c.fill(r, 0xF4F4EE, folds=0.1, fold_every=1, jitter=0.03)
                x0, y0, w, h = r
                for x in range(w):
                    if (x + h) % 2:
                        c.px[x0 + x, y0 + h - 1] = (0, 0, 0, 0)
    pq = part(model, 'plaque')
    if pq:
        f = faces(pq['boxes'][0])
        for k, r in f.items():
            c.fill(r, 0x3A2416, jitter=0.05)
        for k in ('right', 'left'):
            x0, y0, w, h = f[k]
            c.fill((x0, y0, w, h), 0xC9A23E, jitter=0.03)
            c.fill((x0 + 1, y0 + 1, w - 2, h - 2), 0x8C1A1E, jitter=0.04)
            for y in range(2, h - 2, 2):
                c.put(x0 + w // 2, y0 + y, 0xE8C860)
                c.put(x0 + w // 2 - 1 + (y // 2) % 2, y0 + y + 1, 0xE8C860)
                c.shine(x0 + w // 2, y0 + y)
                c.shine(x0 + w // 2 - 1 + (y // 2) % 2, y0 + y + 1)
    out = os.path.join(ASSETS, 'textures/entity/spell', fig + '.png')
    c.img.save(out)
    c.glow.save(os.path.join(ASSETS, 'textures/entity/spell', fig + '_glow.png'))
    print('wrote', out)
    return c.img


def shade_int(c, f):
    r, g, b = shade(c, f)
    return r << 16 | g << 8 | b


# --- A look without the game: the figure drawn as vanilla draws a model ---

def rot(x, y, z):
    """ModelPart's turn: z, then y, then x (its own xRot, yRot, zRot, radians), as a matrix on model space."""
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def quads(model, pose):
    """Every face of every box, posed: (four corners in model pixels, its texture rect, its outward normal)."""
    parts = {p['name']: p for p in model['parts']}
    world = {}

    def frame(name):
        if name in world:
            return world[name]
        p = parts[name]
        r = pose.get(name, (0, 0, 0))
        local = rot(*r)
        pivot = np.array(p['pivot'], dtype=float) + np.array(pose.get(name + '.offset', (0, 0, 0)))
        if p['parent']:
            pm, pt = frame(p['parent'])
            m, t = pm @ local, pm @ pivot + pt
        else:
            m, t = local, pivot
        world[name] = (m, t)
        return m, t

    out = []
    for name, p in parts.items():
        m, t = frame(name)
        for box in p['boxes']:
            x0, y0, z0 = box['from']
            w, h, d = box['size']
            g = box.get('grow', 0)
            x0, y0, z0, x1, y1, z1 = x0 - g, y0 - g, z0 - g, x0 + w + g, y0 + h + g, z0 + d + g
            f = faces(box)
            mirror = box.get('mirror', False)
            # In model space y is down and a face looks toward -z: the front (vanilla's NORTH) at z0.
            corners = {
                'front': ([(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)], (0, 0, -1)),
                'back': ([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], (0, 0, 1)),
                'right': ([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)], (-1, 0, 0)),
                'left': ([(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)], (1, 0, 0)),
                'top': ([(x1, y0, z1), (x0, y0, z1), (x0, y0, z0), (x1, y0, z0)], (0, -1, 0)),
                'bottom': ([(x1, y1, z0), (x0, y1, z0), (x0, y1, z1), (x1, y1, z1)], (0, 1, 0)),
            }
            for k, (cs, n) in corners.items():
                rect = f[k]
                if mirror and k in ('right', 'left'):
                    rect = f['left' if k == 'right' else 'right']
                pts = [m @ np.array(cc, dtype=float) + t for cc in cs]
                out.append((pts, rect, m @ np.array(n, dtype=float), mirror))
    return out


def render(model, tex, pose, yaw, size=(360, 520), scale=7.0, bg=(26, 30, 44)):
    """The figure posed, turned {yaw} (degrees) about its up, drawn orthographic (a z-buffer, nearest texel), lit by two lights."""
    w, h = size
    img = np.zeros((h, w, 3), dtype=float)
    img[:] = bg
    # A floor line and a little gradient sky.
    for y in range(h):
        img[y, :] = np.array(bg) * (0.8 + 0.4 * y / h)
    zbuf = np.full((h, w), -1e9)
    texa = np.asarray(tex.convert('RGBA'), dtype=float)
    th, tw = texa.shape[:2]
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    turn = np.array([[cy, 0, -sy], [0, 1, 0], [sy, 0, cy]])
    light0 = np.array([0.2, 1.0, -0.7]) / np.linalg.norm([0.2, 1.0, -0.7])
    light1 = np.array([-0.2, 1.0, 0.7]) / np.linalg.norm([-0.2, 1.0, 0.7])
    ox, oy = w / 2, h - 30
    for pts, rect, n, mirror in quads(model, pose):
        if rect[2] <= 0 or rect[3] <= 0:
            continue
        # Model space (y down, front -z) into the view: x right, y up, toward the viewer +z; the figure faces the viewer.
        sp = [turn @ np.array([p[0], -p[1], -p[2]]) for p in pts]
        nn = turn @ np.array([n[0], -n[1], -n[2]])
        lit = 0.4 + 0.6 * min(1.0, max(0.0, nn @ light0) * 0.6 + max(0.0, nn @ light1) * 0.6 + 0.4)
        a, b, cpt, d = [np.array([ox + q[0] * scale, oy - (q[1] + 24) * scale, q[2]]) for q in sp]
        x0, y0, rw, rh = rect
        uv = [(x0 + rw, y0), (x0, y0), (x0, y0 + rh), (x0 + rw, y0 + rh)]
        if mirror:
            uv = [(x0, y0), (x0 + rw, y0), (x0 + rw, y0 + rh), (x0, y0 + rh)]
        for tri in ((a, b, cpt, uv[0], uv[1], uv[2]), (a, cpt, d, uv[0], uv[2], uv[3])):
            raster(img, zbuf, texa, tri, lit, tw, th)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))


def raster(img, zbuf, tex, tri, lit, tw, th):
    p0, p1, p2, t0, t1, t2 = tri
    h, w = zbuf.shape
    minx, maxx = int(max(0, math.floor(min(p0[0], p1[0], p2[0])))), int(min(w - 1, math.ceil(max(p0[0], p1[0], p2[0]))))
    miny, maxy = int(max(0, math.floor(min(p0[1], p1[1], p2[1])))), int(min(h - 1, math.ceil(max(p0[1], p1[1], p2[1]))))
    if minx > maxx or miny > maxy:
        return
    area = (p1[0] - p0[0]) * (p2[1] - p0[1]) - (p2[0] - p0[0]) * (p1[1] - p0[1])
    if abs(area) < 1e-9:
        return
    xs, ys = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
    w0 = ((p1[0] - xs) * (p2[1] - ys) - (p2[0] - xs) * (p1[1] - ys)) / area
    w1 = ((p2[0] - xs) * (p0[1] - ys) - (p0[0] - xs) * (p2[1] - ys)) / area
    w2 = 1 - w0 - w1
    inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
    if not inside.any():
        return
    z = w0 * p0[2] + w1 * p1[2] + w2 * p2[2]
    u = w0 * t0[0] + w1 * t1[0] + w2 * t2[0]
    v = w0 * t0[1] + w1 * t1[1] + w2 * t2[1]
    ui = np.clip(np.floor(u).astype(int), 0, tw - 1)
    vi = np.clip(np.floor(v).astype(int), 0, th - 1)
    texel = tex[vi, ui]
    alpha = texel[..., 3] / 255.0
    region = zbuf[miny:maxy + 1, minx:maxx + 1]
    draw = inside & (alpha > 0.02) & (z > region)
    if not draw.any():
        return
    col = texel[..., :3] * lit
    target = img[miny:maxy + 1, minx:maxx + 1]
    a = alpha[..., None]
    blended = target * (1 - a) + col * a
    target[draw] = blended[draw]
    region[draw] = np.where(alpha[draw] > 0.5, z[draw], region[draw])


def stance(fig, t, move=None, phase=0.0):
    """The figure's pose at {t} ticks (as ImpermanenceFigures poses it: its float, its sway, and its moves), in radians."""
    import importlib
    white = fig.startswith('white')
    breathe = math.sin(t * 0.08)
    pose = {
        'body': (math.radians(6), 0, 0),
        'body.offset': (0, -7 - 1.5 * breathe, 0),
        'skirt': (math.radians(-4 + 3 * math.sin(t * 0.11)), 0, math.radians(2 * math.sin(t * 0.07))),
        'head': (math.radians(4 + 2 * math.sin(t * 0.05)), 0, math.radians(3 if white else -3)),
        'hair': (math.radians(8 + 4 * math.sin(t * 0.09)), 0, 0),
        'tongue': (math.radians(-6 + 5 * math.sin(t * 0.13)), 0, math.radians(4 * math.sin(t * 0.1))),
        'right_arm': (math.radians(-18 + 4 * math.sin(t * 0.07)), 0, math.radians(8)),
        'left_arm': (math.radians(-10 - 4 * math.sin(t * 0.07)), 0, math.radians(-10)),
        'staff': (math.radians(18), 0, 0),
        'plaque': (math.radians(14), 0, 0),
        'streamers': (math.radians(10 * math.sin(t * 0.2)), 0, math.radians(8 * math.sin(t * 0.17))),
    }
    if move == 'strike':
        f = phase
        if white:
            raise_up = min(1, f / 0.45)
            down = max(0, (f - 0.45) / 0.2)
            down = min(1, down)
            x = -18 - 150 * raise_up + 130 * down
            pose['right_arm'] = (math.radians(x), 0, math.radians(8 - 6 * raise_up))
            pose['body'] = (math.radians(6 - 8 * raise_up + 22 * down), 0, 0)
        else:
            thrust = min(1, f / 0.3)
            pose['right_arm'] = (math.radians(-18 - 72 * thrust), math.radians(-10 * thrust), math.radians(8))
            pose['body'] = (math.radians(6 + 8 * thrust), math.radians(-12 * thrust), 0)
    if move == 'take':
        reach = min(1, phase / 0.4)
        pose['right_arm'] = (math.radians(-18 - 62 * reach), math.radians(12 * reach), math.radians(8))
        pose['left_arm'] = (math.radians(-10 - 70 * reach), math.radians(-12 * reach), math.radians(-10))
        pose['body'] = (math.radians(6 + 14 * reach), 0, 0)
        pose['head'] = (math.radians(4 + 14 * reach), 0, 0)
    return pose


def preview(outdir):
    os.makedirs(outdir, exist_ok=True)
    for fig in IDS:
        model = load(fig)
        tex = Image.open(os.path.join(ASSETS, 'textures/entity/spell', fig + '.png'))
        frames = [render(model, tex, stance(fig, i * 2), i * 360 / 36) for i in range(36)]
        frames[0].save(os.path.join(outdir, fig + '_turn.gif'), save_all=True, append_images=frames[1:], duration=70, loop=0)
        moves = [render(model, tex, stance(fig, i, 'strike', i / 20.0), 25) for i in range(24)]
        moves += [render(model, tex, stance(fig, 24 + i, 'take', i / 16.0), 25) for i in range(20)]
        moves[0].save(os.path.join(outdir, fig + '_moves.gif'), save_all=True, append_images=moves[1:], duration=70, loop=0)
        still = Image.new('RGB', (720, 520))
        still.paste(render(model, tex, stance(fig, 0), 20), (0, 0))
        still.paste(render(model, tex, stance(fig, 0), 160), (360, 0))
        still.save(os.path.join(outdir, fig + '.png'))
        print('previewed', fig)


if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == 'preview':
        preview(sys.argv[2] if len(sys.argv) > 2 else 'run/previews/figures')
    else:
        for fig in IDS:
            paint(fig)
