/**
 * Entering Nirvana's spell icons (Icons#ICONS lists them): death and rebirth, each as the spell calls it, in Icons' way: a few bold shapes,
 * shaded, outlined dark.
 */
public class NirvanaIcons {
	static Icons.Canvas canvas() { return new Icons.Canvas(); }

	static int c(int rgb) { return Icons.c(rgb); }

	/** Seed of Withering Rebirth: a seed half withered and half green, a flower of rebirth opening out of its top, its vines reaching down. */
	static Icons.Canvas seed() {
		Icons.Canvas k = canvas();
		for (int s = -1; s <= 1; s += 2) {
			k.fill(Icons.line(16, 22, 16 + s * 6, 29, 0.6), c(Wood.LEAF_DARK)).fill(Icons.line(16 + s * 6, 29, 16 + s * 9, 30, 0.5), c(Wood.LEAF_DARK));
			k.fill(Icons.line(16, 23, 16 + s * 2.5, 30, 0.5), c(Wood.LEAF));
		}
		k.fill(Icons.ellipse(16, 19, 4.2, 5.5), (x, y) -> c(x < 16 ? (y % 3 < 1 ? Nirvana.HUSK_DARK : Nirvana.HUSK) : (Math.abs(x - 18) < 0.6 ? Wood.VEIN : Wood.JADE)));
		k.fill(Icons.rect(15.6, 13.5, 16.4, 24.5), c(Earth.GOLD_PALE));
		for (int i = 0; i < 5; i++) {
			double a = Math.toRadians(-150 + i * 30), tx = 16 + Math.cos(a) * 10, ty = 12 + Math.sin(a) * 9;
			k.fill(Icons.tri(16 + Math.cos(a - 0.5) * 2, 13 + Math.sin(a - 0.5) * 1.5, 16 + Math.cos(a + 0.5) * 2, 13 + Math.sin(a + 0.5) * 1.5, tx, ty),
					(x, y) -> c(SpellArt.mix(Nirvana.PETAL_ROOT, Nirvana.PETAL_BLUSH, Math.min(1, Math.hypot(x - 16, y - 13) / 10))));
		}
		k.fill(Icons.disc(16, 11.5, 1.8), c(Nirvana.PETAL_GOLD));
		return k.outline();
	}

	/** Withering and Flourishing: the wheel of the seasons, a taiji of spring's green and autumn's dry earth, a blossom and a dead leaf on it. */
	static Icons.Canvas seasons() {
		Icons.Canvas k = canvas();
		k.fill(Icons.disc(16, 16, 14), c(Earth.GOLD_SCALE));
		k.fill(Icons.disc(16, 16, 12.6), (x, y) -> c(Nirvana.alive((x - 16) / 12.6, (y - 16) / 12.6) ? SpellArt.mix(Wood.LEAF_DARK, Wood.LEAF, 0.5 + 0.3 * Math.sin(x * 2 + y)) : SpellArt.mix(Nirvana.DRY, Nirvana.DRY_LIGHT, 0.5 + 0.3 * Math.sin(x * 3 - y * 2))));
		k.fill((x, y) -> {
			double X = (x - 16) / 12.6, Y = (y - 16) / 12.6, e = 0.07;
			if (Math.hypot(X, Y) > 1) return false;
			boolean live = Nirvana.alive(X, Y);
			return Nirvana.alive(X + e, Y) != live || Nirvana.alive(X, Y + e) != live || Nirvana.alive(X - e, Y) != live || Nirvana.alive(X, Y - e) != live;
		}, c(Earth.GOLD_PALE));
		for (double[] p : new double[][] {{12, 9}, {9, 14}, {13, 13}}) k.fill(Icons.disc(p[0], p[1], 1), c(Nirvana.BLOSSOM));
		for (double[] p : new double[][] {{20, 21}, {23, 18}, {19, 25}}) k.fill(Icons.disc(p[0], p[1], 1), c(Fire.FLAME));
		return k.outline();
	}

	/** Undying Ember: a coal glowing through its cracks, feathers of flame flaring round it. */
	static Icons.Canvas ember() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 9; i++) {
			double a = Math.PI * 2 * i / 9 - Math.PI / 2, r = i % 2 == 0 ? 15 : 11;
			k.fill(Icons.tri(16 + Math.cos(a - 0.35) * 5, 16 + Math.sin(a - 0.35) * 5, 16 + Math.cos(a + 0.35) * 5, 16 + Math.sin(a + 0.35) * 5, 16 + Math.cos(a) * r, 16 + Math.sin(a) * r),
					(x, y) -> c(SpellArt.mix(Fire.GOLD, Fire.VERMILION, Math.min(1, Math.hypot(x - 16, y - 16) / 14))));
		}
		k.fill(Icons.disc(16, 16, 6), (x, y) -> c((Math.sin(x * 2.3) * Math.cos(y * 2.1) > 0.55) ? Fire.GOLD_LIGHT : SpellArt.mix(Fire.CRIMSON, Fire.EMBER, 0.3)));
		return k.outline();
	}

	/** Pyre of the Heavens: a ring of burning cribs of logs, seen from above and aside, flames standing up off it. */
	static Icons.Canvas pyre() {
		Icons.Canvas k = canvas();
		k.fill((x, y) -> {
			double e = Math.hypot((x - 16) / 13, (y - 20) / 7);
			return e > 0.75 && e < 1.05;
		}, (x, y) -> c(SpellArt.mix(Fire.FLAME, Fire.CRIMSON, 0.4)));
		for (int i = 0; i < 7; i++) {
			double a = Math.PI * 2 * i / 7 + 0.3, x = 16 + Math.cos(a) * 12, y = 20 + Math.sin(a) * 6.3;
			k.fill(Icons.tri(x - 2.4, y, x + 2.4, y, x + (i % 2 == 0 ? 0.6 : -0.6), y - 9 - (i % 3)), (px, py) -> c(SpellArt.mix(Fire.GOLD_LIGHT, Fire.VERMILION, (y - py) / 11)));
			k.fill(Icons.rect(x - 2, y - 1, x + 2, y + 1.6), c(0x2A1A10)).fill(Icons.rect(x - 2, y - 0.2, x + 2, y + 0.4), c(Fire.AMBER));
		}
		return k.outline();
	}

	/** Gravestone Falls: a stele written with a burning glyph, borne on a stone tortoise, falling. */
	static Icons.Canvas gravestone() {
		Icons.Canvas k = canvas();
		k.fill(Icons.ellipse(16, 26, 10, 3.5), c(Nirvana.STELE)).fill(Icons.rect(8.5, 24, 10.5, 27), c(Nirvana.STELE_DARK));
		k.fill(Icons.rect(10, 9, 22, 25), Icons.shaded(Nirvana.STELE, 10, 4, 22, 25)).fill(Icons.disc(16, 9, 6), Icons.shaded(Nirvana.STELE, 10, 3, 22, 15));
		k.fill(Icons.rect(14.6, 7, 17.4, 22), (x, y) -> c((int) Math.floor(y) % 4 == 2 ? Nirvana.STELE : Nirvana.STELE_GLYPH));
		for (int i = 0; i < 3; i++) k.fill(Icons.line(6 + i * 9, 1, 6 + i * 9, 5, 0.4), c(0xD8DCE0));
		return k.outline();
	}

	/** Buried World: three standing stones round a pit sinking into darkness. */
	static Icons.Canvas buried() {
		Icons.Canvas k = canvas();
		k.fill(Icons.ellipse(16, 23, 13, 6), c(Earth.STONE_DARK)).fill(Icons.ellipse(16, 23.5, 9, 3.6), c(0x14100C));
		double[][] stones = {{6, 21, 3.2, 12}, {16, 16, 3.6, 13}, {26, 21, 3.2, 12}};
		for (double[] s : stones) {
			k.fill(Icons.rect(s[0] - s[2] / 2, s[1] - s[3], s[0] + s[2] / 2, s[1]), Icons.shaded(Earth.STONE, s[0] - 2, s[1] - s[3], s[0] + 2, s[1]));
			k.fill(Icons.disc(s[0], s[1] - s[3] * 0.6, 0.8), c(Earth.GOLD_PALE));
		}
		return k.outline();
	}

	/** Frozen Moon in the Well: a crescent moon of ice, and under it its reflection in a well of black water. */
	static Icons.Canvas moonWell() {
		Icons.Canvas k = canvas();
		k.fill((x, y) -> Math.hypot(x - 15, y - 10) < 8 && Math.hypot(x - 19, y - 9) > 6.5, (x, y) -> c(SpellArt.mix(0xFFFFFF, Nirvana.MOON_ICE_SHADE, (y - 2) / 16)));
		k.fill(Icons.ellipse(16, 25, 12, 4.5), c(Water.NAVY));
		k.fill((x, y) -> Math.hypot((x - 15) / 1.0, (y - 25) / 0.45) < 5 && Math.hypot((x - 17) / 1.0, (y - 25.4) / 0.45) > 4, c(Nirvana.MOON_GLOW));
		k.fill((x, y) -> Math.abs(Math.hypot((x - 16) / 12, (y - 25) / 4.5) - 0.97) < 0.05, c(Nirvana.MOON_ICE));
		return k.outline();
	}

	/** The Frozen Moon Falls: a moon of ice, cratered, cracked with light, its frost streaming up behind it. */
	static Icons.Canvas moonFalls() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 4; i++) k.fill(Icons.line(9 + i * 4.5, 2, 11 + i * 4, 13, 1.2), c(SpellArt.mix(0xFFFFFF, Nirvana.MOON_GLOW, i % 2)));
		k.fill(Icons.disc(16, 19, 10.5), (x, y) -> c(SpellArt.mix(Nirvana.MOON_ICE, Nirvana.MOON_ICE_SHADE, Math.max(0, (x - 10 + y - 13) / 24))));
		for (double[] p : new double[][] {{12, 16, 2.2}, {19, 22, 2.8}, {20, 14, 1.4}, {13, 24, 1.3}}) k.fill(Icons.disc(p[0], p[1], p[2]), c(Nirvana.MOON_ICE_SHADE));
		k.fill(Icons.line(14, 19, 22, 25, 0.35), c(Water.CYAN));
		return k.outline();
	}

	/** Sword Tomb Awakens: a ring of grave earth, swords standing in it hilts up, one plunging point-down into its heart. */
	static Icons.Canvas tomb() {
		Icons.Canvas k = canvas();
		k.fill(Icons.ellipse(16, 25, 14, 5), Icons.shaded(0x3A2C20, 2, 20, 30, 30)).fill(Icons.ellipse(16, 25, 9, 2.8), c(0x1A120C));
		// The ring's swords, the far ones first, so the near ones stand over them.
		java.util.List<double[]> ring = new java.util.ArrayList<>();
		for (int i = 0; i < 8; i++) {
			double a = Math.toRadians(22.5 + 45 * i);
			ring.add(new double[] {16 + 11.5 * Math.cos(a), 25 + 4 * Math.sin(a)});
		}
		ring.sort((p, q) -> Double.compare(p[1], q[1]));
		for (double[] s : ring) {
			double x = s[0], y = s[1];
			k.fill(Icons.line(x, y, x, y - 8, 0.8), c(Metal.STEEL)).fill(Icons.line(x + 0.3, y, x + 0.3, y - 8, 0.25), c(0xFFFFFF));
			k.fill(Icons.line(x - 2, y - 8, x + 2, y - 8, 0.55), c(Metal.GOLD_DARK)).fill(Icons.line(x, y - 8, x, y - 11, 0.55), c(0x2A2420));
		}
		// The last of them, coming down into its heart.
		k.fill(Icons.disc(16, 24, 3), c(SpellArt.mix(0xFFFFFF, Nirvana.TOMB_GLOW, 0.4)));
		k.fill(Icons.line(16, 22, 16, 6, 1.1), c(0xE6EEFA)).fill(Icons.line(16.3, 22, 16.3, 6, 0.4), c(0xFFFFFF));
		k.fill(Icons.line(12.5, 6, 19.5, 6, 0.8), c(Metal.GOLD)).fill(Icons.line(16, 6, 16, 2, 0.7), c(0x2A2420));
		return k.outline();
	}

	/** Nine Rebirths of the Sword: a sword in white flame sweeping round from its hilt, the arc of its sweep burning behind it. */
	static Icons.Canvas rebirths() {
		Icons.Canvas k = canvas();
		// The sweep's arc, fading back from the blade.
		for (int i = 0; i < 8; i++) {
			double a0 = Math.toRadians(45 - i * 7), a1 = Math.toRadians(45 - (i + 1) * 7), r = 21;
			k.fill(Icons.line(6.5 + Math.cos(a0) * r, 25.5 - Math.sin(a0) * r, 6.5 + Math.cos(a1) * r, 25.5 - Math.sin(a1) * r, 2.2 - i * 0.2),
					c(SpellArt.mix(0xFFFFFF, 0x4A5A8A, i / 8.0)));
		}
		for (int i = 0; i < 6; i++) {
			double t = 0.2 + i * 0.13, x = 6 + 20 * t, y = 26 - 20 * t;
			k.fill(Icons.tri(x - 1.6, y + 1.6, x + 1.6, y - 1.6, x + 3.5, y + 3.5), c(SpellArt.mix(0xFFFFFF, 0x8CA4E8, 0.4)));
		}
		k.fill(Icons.line(7, 25, 26, 6, 1.4), c(0xE6EEFA)).fill(Icons.line(7, 25, 26, 6, 0.5), c(0xFFFFFF));
		k.fill(Icons.line(4, 25, 9, 30, 1), c(Metal.GOLD)).fill(Icons.line(3, 29, 6, 26, 1.2), c(0x2A3442));
		return k.outline();
	}

	/** Black and White Impermanence: the two soul-catchers side by side, the White's tall hat and the Black's, their eyes of ghost fire. */
	static Icons.Canvas impermanence() {
		Icons.Canvas k = canvas();
		for (int s = 0; s < 2; s++) {
			boolean white = s == 0;
			double cx = white ? 10 : 22;
			int robe = white ? 0xE8F4F4 : 0x1E2630, hat = white ? 0xF6F4EE : 0x0E1014;
			k.fill(Icons.tri(cx - 6, 31, cx + 6, 31, cx, 15), c(robe));
			k.fill(Icons.rect(cx - 3, 12, cx + 3, 18), c(white ? 0xF2F0EA : 0x2A2E34));
			k.fill(Icons.rect(cx - 2.4, 2, cx + 2.4, 12), c(hat)).fill(Icons.rect(cx - 0.5, 4, cx + 0.5, 10), c(white ? 0x141418 : 0xF0EEE6));
			k.fill(Icons.disc(cx - 1.3, 14.5, 0.8), c(Ghost.FIRE_CYAN)).fill(Icons.disc(cx + 1.3, 14.5, 0.8), c(Ghost.FIRE_CYAN));
			if (white) k.fill(Icons.rect(cx - 0.5, 16.5, cx + 0.5, 20), c(Nirvana.TONGUE));
		}
		return k.outline();
	}

	/** Book of Life and Death: the open book, its names in black, one struck through in cinnabar, the judge's brush over it. */
	static Icons.Canvas book() {
		Icons.Canvas k = canvas();
		k.fill(Icons.tri(2, 24, 16, 28, 16, 12), c(Nirvana.PAPER)).fill(Icons.tri(2, 24, 2, 10, 16, 12), c(Nirvana.PAPER));
		k.fill(Icons.tri(30, 24, 16, 28, 16, 12), c(Nirvana.PAPER_SHADE)).fill(Icons.tri(30, 24, 30, 10, 16, 12), c(Nirvana.PAPER_SHADE));
		for (int i = 0; i < 4; i++) {
			k.fill(Icons.line(5 + i * 2.6, 12 + i * 0.4, 5 + i * 2.6, 22 + i * 1, 0.35), c(Nirvana.INK));
			k.fill(Icons.line(19 + i * 2.6, 13 - i * 0.3, 19 + i * 2.6, 24 - i * 0.6, 0.35), c(Nirvana.INK));
		}
		k.fill(Icons.line(18, 16, 28, 20, 0.6), c(Nirvana.CINNABAR_INK));
		k.fill(Icons.line(20, 2, 27, 11, 1.1), c(Ghost.LACQUER)).fill(Icons.tri(26, 10, 28.5, 12.5, 29, 15), c(Nirvana.CINNABAR_INK));
		return k.outline();
	}

	/** Heaven-Demon Eclipse: heaven's sun, the demon's moon passing before it, the corona blazing round them. */
	static Icons.Canvas eclipse() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			k.fill(Icons.tri(16 + Math.cos(a - 0.15) * 9, 16 + Math.sin(a - 0.15) * 9, 16 + Math.cos(a + 0.15) * 9, 16 + Math.sin(a + 0.15) * 9, 16 + Math.cos(a) * 15, 16 + Math.sin(a) * 15),
					c(i % 2 == 0 ? HeavenDemon.GOLD_PALE : HeavenDemon.GOLD_BRIGHT));
		}
		k.fill(Icons.disc(16, 16, 10), c(HeavenDemon.GOLD_BRIGHT));
		k.fill(Icons.disc(18.5, 16, 9), c(0x0A0204)).fill((x, y) -> Math.abs(Math.hypot(x - 18.5, y - 16) - 8.6) < 0.6, c(HeavenDemon.SCARLET));
		k.fill(Icons.disc(10.5, 11.5, 1.2), c(0xFFFFFF));
		return k.outline();
	}
}
