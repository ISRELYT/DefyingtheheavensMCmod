/**
 * True Immortal's spell icons (Icons#ICONS lists them): the elements as they were before heaven and earth were parted, each as the spell
 * calls it, in Icons' way: a few bold shapes, shaded, outlined dark.
 */
public class ImmortalIcons {
	static Icons.Canvas canvas() { return new Icons.Canvas(); }

	static int c(int rgb) { return Icons.c(rgb); }

	/** The band of an ellipse between {@code inner} (a share of its radii) and its edge. */
	static Icons.Test band(double cx, double cy, double rx, double ry, double inner) {
		return (x, y) -> {
			double e = Math.pow((x - cx) / rx, 2) + Math.pow((y - cy) / ry, 2);
			return e <= 1 && e >= inner * inner;
		};
	}

	/** Primal Wind of Creation: a funnel of green-gold wind, narrow at its foot, opening wide at its top, leaves and a seed of light in it. */
	static Icons.Canvas wind() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 7; i++) {
			double y = 27.5 - i * 3.7, rx = 2.4 + i * 2.0, ry = 0.9 + i * 0.28, cx = 16 + Math.sin(i * 0.9) * 1.2;
			int ii = i;
			k.fill(band(cx, y, rx, ry + 0.9, 0.55), (x, yy) -> c(SpellArt.mix(SpellArt.mix(Immortal.PRIMAL_DEEP, Immortal.PRIMAL_GREEN, 0.35 + ii * 0.08),
					Immortal.PRIMAL_GOLD, yy > y ? 0.35 : 0)));
		}
		for (double[] l : new double[][] {{9, 9, 0.6}, {23, 13, -0.5}, {12, 19, 0.4}, {21, 5, -0.7}}) {
			double a = l[2];
			k.fill((x, y) -> {
				double u = (x - l[0]) * Math.cos(a) + (y - l[1]) * Math.sin(a), v = -(x - l[0]) * Math.sin(a) + (y - l[1]) * Math.cos(a);
				return u * u / 4.5 + v * v / 1.0 <= 1;
			}, (x, y) -> c(SpellArt.mix(Wood.LEAF, Wood.LEAF_LIGHT, (16 - y) / 20 + 0.4)));
		}
		k.fill(Icons.disc(16, 14, 2), c(Immortal.PRIMAL_GOLD)).fill(Icons.disc(15.4, 13.4, 0.8), c(0xFFFFFF));
		return k.outline();
	}

	/** First Seed of Creation: the seed of gold and jade under the dome of life, vines arching over it to a knot of light, a ring at their feet. */
	static Icons.Canvas seed() {
		Icons.Canvas k = canvas();
		k.fill(band(16, 27, 14.5, 3.4, 0.7), c(Immortal.PRIMAL_GOLD));
		for (double fx : new double[] {-1, -0.5, 0.5, 1}) {
			for (int i = 0; i <= 40; i++) {
				double t = i / 40.0, x = 16 + 13.5 * fx * Math.cos(t * Math.PI / 2), y = 27 - 22 * Math.sin(t * Math.PI / 2);
				k.fill(Icons.disc(x, y, 1.15 - 0.4 * t), (px, py) -> c(SpellArt.mix(0x24683E, 0x4CAE68, 0.5 + (16 - px) / 30)));
				if (i % 8 == 4) k.fill(Icons.ellipse(x + fx * 1.6, y + 0.8, 1.4, 0.8), c(Wood.LEAF_LIGHT));
			}
		}
		k.fill(Icons.disc(16, 5, 1.8), c(Immortal.PRIMAL_GOLD)).fill(Icons.disc(15.6, 4.6, 0.7), c(0xFFFFFF));
		k.fill(Icons.ellipse(16, 19, 3.4, 4.4), (x, y) -> c(SpellArt.mix(Wood.JADE, Immortal.PRIMAL_GOLD, (y - 14.6) / 8.8)));
		k.fill(Icons.line(15, 16, 17, 22, 0.35), c(0xFFFFFF));
		k.fill(Icons.line(16, 14.8, 17.5, 12.5, 0.5), c(Wood.LEAF_LIGHT));
		return k.outline();
	}

	/** Samadhi True Fire: three pillars of flame braided about one another, the heart's crimson, the spirit's gold and the essence's white. */
	static Icons.Canvas samadhi() {
		Icons.Canvas k = canvas();
		k.fill(Icons.ellipse(16, 28.5, 9, 2.4), c(0xFF7A3C));
		int[] strand = {0xD8361E, 0xFFB838, 0xF0F4FF}, dim = {0x8A1408, 0xE07A1E, 0x8CA4E8};
		// Every strand's far turns first, then every near one over them, so the braid reads as three winding about each other.
		for (int pass = 0; pass < 2; pass++) {
			for (int s = 0; s < 3; s++) {
				for (int i = 0; i <= 60; i++) {
					double t = i / 60.0, ph = t * Math.PI * 2 * 1.1 + s * Math.PI * 2 / 3, x = 16 + 5 * Math.sin(ph), y = 27 - 22 * t;
					boolean near = Math.cos(ph) > 0;
					if (near != (pass == 1)) continue;
					k.fill(Icons.disc(x, y, 1.6), c(near ? strand[s] : dim[s]));
				}
			}
		}
		for (int s = 0; s < 3; s++) {
			double tx = 16 + 5 * Math.sin(Math.PI * 2 * 1.1 + s * Math.PI * 2 / 3);
			k.fill(Icons.tri(tx - 1.6, 5.5, tx + 1.6, 5.5, tx, 1), c(strand[s]));
		}
		return k.outline();
	}

	/** First Flame Before Heaven: the flower of chaos-fire lying open, its petals of white flame and black by turns, its heart a white star. */
	static Icons.Canvas firstFlame() {
		Icons.Canvas k = canvas();
		for (int p = 0; p < 2; p++) {
			for (int i = 0; i < 8; i++) {
				// The far petals first, so the near ones lie over them.
				double a = Math.toRadians(45 * i + 22.5);
				if ((Math.sin(a) > 0) != (p == 1)) continue;
				boolean white = i % 2 == 0;
				double tx = 16 + Math.cos(a) * 14, ty = 17 + Math.sin(a) * 9, nx = -Math.sin(a) * 3, ny = Math.cos(a) * 2;
				k.fill(Icons.tri(16 + nx, 17 + ny, 16 - nx, 17 - ny, tx, ty), (x, y) -> {
					double f = Math.hypot(x - 16, (y - 17) * 1.5) / 14;
					return c(white ? SpellArt.mix(0xFFFFFF, 0x8CA4E8, f) : SpellArt.mix(Immortal.BLACK_FLAME[1], Immortal.BLACK_FLAME[3], f));
				});
			}
		}
		k.fill(Icons.disc(16, 16.5, 3.6), c(0x7A5AA8)).fill(Icons.disc(16, 16.5, 2.6), c(0xFFFFFF));
		return k.outline();
	}

	/** Hand of Houtu: a hand of the earth's yellow jade rising open out of the land, a bracelet of gold at its wrist. */
	static Icons.Canvas houtu() {
		Icons.Canvas k = canvas();
		k.fill(Icons.ellipse(16, 29, 13, 3), Icons.shaded(0x5A4026, 3, 26, 29, 32));
		k.fill(Icons.rect(12.5, 19, 19.5, 29), Icons.shaded(Earth.JADE_Y, 12, 19, 20, 29));
		k.fill(Icons.rect(11.5, 19.5, 20.5, 21), c(Metal.GOLD));
		k.fill(Icons.rect(10.5, 11, 21.5, 19.5), Icons.shaded(Earth.JADE_Y, 10, 11, 22, 20));
		double[][] fingers = {{11.8, 5.5}, {14.8, 3.5}, {17.8, 4}, {20.6, 6.5}};
		for (double[] f : fingers) k.fill(Icons.rect(f[0] - 1.1, f[1], f[0] + 1.1, 12), Icons.shaded(Earth.JADE_Y_LIGHT, f[0] - 1, f[1], f[0] + 1, 12));
		k.fill(Icons.line(21, 16, 26, 11, 1.3), Icons.shaded(Earth.JADE_Y, 20, 10, 27, 17));
		k.fill(Icons.line(13, 15, 19, 15, 0.3), c(Metal.GOLD));
		return k.outline();
	}

	/** Falling Continent: a land torn up whole falling, meadow and snow-capped mountains on it, its jagged bedrock hanging under it. */
	static Icons.Canvas continent() {
		Icons.Canvas k = canvas();
		for (double[] s : new double[][] {{8, 1, 8, 5}, {15, 0, 15, 4}, {22, 1, 22, 5}}) k.fill(Icons.line(s[0], s[1], s[2], s[3], 0.4), c(0xC8D8E8));
		k.fill(Icons.tri(5, 16, 27, 16, 15, 30), Icons.shaded(0x6E685A, 5, 16, 27, 30));
		k.fill(Icons.tri(9, 16, 14, 16, 11, 24), c(0x4A443C)).fill(Icons.tri(18, 16, 25, 16, 21, 23), c(0x4A443C));
		k.fill(Icons.rect(4, 13.5, 28, 17), c(0x5A3E26));
		k.fill(Icons.ellipse(16, 12.5, 12.5, 3.6), Icons.shaded(0x5A9A40, 4, 9, 28, 16));
		k.fill(Icons.tri(8, 13, 13, 13, 10.5, 6), c(0x948E7C)).fill(Icons.tri(14, 12, 22, 12, 18, 3), c(0x948E7C));
		k.fill(Icons.tri(9.3, 8.5, 11.7, 8.5, 10.5, 6), c(0xFFFFFF)).fill(Icons.tri(16.4, 6, 19.6, 6, 18, 3), c(0xFFFFFF));
		k.fill(Icons.ellipse(23, 13, 3.4, 1.5), c(0x2E7AB0));
		return k.outline();
	}

	/** Glacier of the First Age: a wall of ancient ice, broken towers of it, snow on their tops, a blue crevasse down it, frost at its foot. */
	static Icons.Canvas glacier() {
		Icons.Canvas k = canvas();
		k.fill(Icons.ellipse(16, 28.5, 15, 2.6), c(0xDCEEF8));
		double[][] seracs = {{3, 13}, {7.5, 8}, {12, 11}, {16.5, 5}, {21, 9}, {25.5, 12}};
		for (double[] s : seracs) {
			k.fill(Icons.rect(s[0], s[1], s[0] + 4.6, 28), Icons.shaded(0xA8DCF4, s[0], s[1], s[0] + 5, 28));
			k.fill(Icons.rect(s[0], s[1], s[0] + 4.6, s[1] + 1.4), c(0xFFFFFF));
		}
		k.fill(Icons.line(18.5, 8, 17.5, 22, 0.5), c(0x2A6AB0)).fill(Icons.line(9.5, 12, 10.5, 20, 0.4), c(0x2A6AB0));
		return k.outline();
	}

	/** The Primordial Sea: a ring of towering waves round a whirlpool of the dark first sea, their crests curling in white. */
	static Icons.Canvas sea() {
		Icons.Canvas k = canvas();
		k.fill(Icons.ellipse(16, 20, 13.5, 7.5), (x, y) -> {
			double a = Math.atan2((y - 20) * 1.8, x - 16), r = Math.hypot(x - 16, (y - 20) * 1.8) / 13.5;
			double arm = Math.sin(a * 3 + r * 9);
			return c(arm > 0.6 ? SpellArt.mix(Water.DEEP, Water.TEAL, r) : SpellArt.mix(0x02060C, Water.DEEP, r));
		});
		for (int p = 0; p < 2; p++) {
			for (int i = 0; i < 12; i++) {
				double a = Math.toRadians(30 * i + 15);
				if ((Math.sin(a) > 0) != (p == 1)) continue;
				double bx = 16 + Math.cos(a) * 13, by = 20 + Math.sin(a) * 7, h = 6 + (p == 1 ? 2 : 0);
				double tx = bx - Math.cos(a) * 3, ty = by - h;
				k.fill(Icons.tri(bx - 2.6, by + 0.5, bx + 2.6, by + 0.5, tx, ty), (x, y) -> c(SpellArt.mix(Water.TEAL, Water.CYAN, (by - y) / h)));
				k.fill(Icons.disc(tx, ty + 0.6, 1.1), c(Water.FOAM));
			}
		}
		return k.outline();
	}

	/** Heaven-Slaying Sword Rises: a great sword of sword-light rising point-up out of the cracked land, its guard swept up like wings, a halo at its point. */
	static Icons.Canvas slayer() {
		Icons.Canvas k = canvas();
		// Rays of sword-light out from its point.
		for (int i = 0; i < 8; i++) {
			double a = Math.toRadians(45 * i + 22.5);
			k.fill(Icons.line(16 + Math.cos(a) * 3.5, 6 + Math.sin(a) * 3.5, 16 + Math.cos(a) * 6.5, 6 + Math.sin(a) * 6.5, 0.4), c(0xFFE08A));
		}
		// The land, broken where it bursts up, chunks of it flung aside.
		k.fill(Icons.rect(1, 25.5, 31, 31), Icons.shaded(0x5A4A38, 1, 25, 31, 31));
		k.fill(Icons.tri(11, 25.5, 21, 25.5, 16, 31), c(0x1A1410));
		for (double[] r : new double[][] {{6, 20, 1.6}, {25, 21.5, 1.3}, {8.5, 15.5, 1}, {24, 16, 0.9}}) k.fill(Icons.disc(r[0], r[1], r[2]), c(0x76685A));
		// The blade, broad, its edges steel, its middle white, a fuller of gold glyphs down it.
		k.fill(Icons.rect(13.2, 8, 18.8, 27), (x, y) -> c(Math.abs(x - 16) > 2 ? Metal.SILVER : SpellArt.mix(0xFFFFFF, Metal.GLEAM, Math.abs(x - 16) / 2.2)));
		k.fill(Icons.tri(13.2, 8.2, 18.8, 8.2, 16, 1.5), (x, y) -> c(Math.abs(x - 16) > 1.6 ? Metal.SILVER : 0xFFFFFF));
		for (double y = 10; y < 25; y += 3) k.fill(Icons.rect(15.5, y, 16.5, y + 1.4), c(0xFFD54A));
		return k.outline();
	}

	/** Primal Axe Splits Chaos: Pangu's crescent axe coming down, its edge white with light, the land split under it in a line of gold. */
	static Icons.Canvas axe() {
		Icons.Canvas k = canvas();
		k.fill(Icons.line(3, 28.5, 29, 26.5, 0.9), c(0xFFE89A)).fill(Icons.line(3, 28.5, 29, 26.5, 0.35), c(0xFFFFFF));
		k.fill(Icons.line(26, 25, 15, 3, 1.2), (x, y) -> c(SpellArt.mix(Wood.WOOD_DARK, Wood.WOOD, 0.4)));
		for (double t : new double[] {0.3, 0.55, 0.8}) k.fill(Icons.disc(26 - 11 * t, 25 - 22 * t, 1.4), c(Metal.GOLD_DARK));
		// The crescent: a fan of bronze out from the haft, widening to its curved edge.
		Icons.Test head = (x, y) -> x < 16 && Math.hypot(x - 16.5, y - 9) <= 12.5 && Math.abs(y - 9) <= 2.2 + (16 - x) * 0.62;
		k.fill(head, (x, y) -> {
			double edge = 12.5 - Math.hypot(x - 16.5, y - 9);
			return c(edge < 1.5 ? 0xFFFFFF : edge < 2.3 ? Metal.GOLD_DARK : SpellArt.mix(0x3A3028, 0x6A5A44, Math.sin((x + y) * 0.9) * 0.5 + 0.5));
		});
		k.fill(Icons.line(9, 6, 11, 12, 0.45), c(Metal.GOLD));
		return k.outline();
	}

	/** Fengdu's Walls Rise: the walls of the city of the dead, black stone, crenellated, its gate-tower roofed in black tile, soul lanterns burning teal. */
	static Icons.Canvas fengdu() {
		Icons.Canvas k = canvas();
		k.fill(Icons.rect(2, 16, 30, 28), Icons.shaded(Ghost.SLATE, 2, 16, 30, 28));
		for (int i = 0; i < 7; i++) k.fill(Icons.rect(2.5 + i * 4, 13.5, 4.5 + i * 4, 16), c(Ghost.SLATE));
		k.fill(Icons.rect(11, 9, 21, 16), Icons.shaded(Ghost.SLATE, 11, 9, 21, 16));
		k.fill(Icons.tri(8, 10, 24, 10, 16, 4.5), c(0x1E2228)).fill(Icons.rect(8, 9.5, 24, 10.5), c(0x323C48));
		k.fill(Icons.ellipse(16, 24, 3, 4.5), c(0x050608)).fill(Icons.rect(13, 24, 19, 28), c(0x050608));
		for (double[] l : new double[][] {{5, 19}, {27, 19}}) k.fill(Icons.disc(l[0], l[1], 1.6), c(Ghost.FIRE_CYAN));
		for (double[] g : new double[][] {{8, 23}, {24, 22}, {10, 19.5}}) k.fill(Icons.rect(g[0], g[1], g[0] + 1, g[1] + 1.6), c(Ghost.FIRE_TEAL));
		for (int i = 0; i < 6; i++) k.fill(Icons.line(2, 20 + i * 1.9 - 1, 30, 20 + i * 1.9 - 1, 0.12), c(Ghost.SLATE_LIGHT));
		return k.outline();
	}

	/** Ten Thousand Souls Ascend: two strands of the dead winding up about one another, pale faces among the wisps, ghost fire at their foot. */
	static Icons.Canvas souls() {
		Icons.Canvas k = canvas();
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i <= 50; i++) {
				double t = i / 50.0, ph = t * Math.PI * 2 * 1.5 + s * Math.PI, x = 16 + 6 * Math.sin(ph), y = 27 - 24 * t;
				k.fill(Icons.disc(x, y, 1.5), c(Math.cos(ph) > 0 ? 0xC8DCDA : 0x7E9C9C));
			}
		}
		for (double[] f : new double[][] {{12.2, 20}, {19.8, 12}, {13, 6}}) {
			k.fill(Icons.disc(f[0], f[1], 2), c(0xF0FAF8)).fill(Icons.disc(f[0] - 0.7, f[1] - 0.3, 0.45), c(Ghost.FIRE_CYAN)).fill(Icons.disc(f[0] + 0.7, f[1] - 0.3, 0.45), c(Ghost.FIRE_CYAN));
		}
		for (double x : new double[] {9, 13, 19, 23}) k.fill(Icons.tri(x - 2, 30, x + 2, 30, x, 24), (px, py) -> c(SpellArt.mix(Ghost.FIRE_TEAL, Ghost.FIRE_CYAN, (30 - py) / 6)));
		return k.outline();
	}

	/** Primordial Chaos: a sphere of heaven's gold and the demon's crimson swirled together, two orbits of their light round it. */
	static Icons.Canvas chaos() {
		Icons.Canvas k = canvas();
		k.fill(band(16, 16, 15, 5.5, 0.84), c(HeavenDemon.GOLD_BRIGHT));
		k.fill(Icons.disc(16, 16, 10), (x, y) -> {
			double a = Math.atan2(y - 16, x - 16), r = Math.hypot(x - 16, y - 16), swirl = Math.sin(a * 2 + r * 0.45);
			if (Math.abs(swirl) < 0.12) return c(0xFFFFFF);
			return c(swirl > 0 ? SpellArt.mix(HeavenDemon.GOLD, HeavenDemon.GOLD_PALE, swirl) : SpellArt.mix(HeavenDemon.CRIMSON, HeavenDemon.BLOOD_DARK, -swirl));
		});
		// The near half of heaven's orbit over the sphere (the far half lies behind it).
		k.fill((x, y) -> band(16, 16, 15, 5.5, 0.84).in(x, y) && y > 16, c(HeavenDemon.GOLD_BRIGHT));
		k.fill((x, y) -> {
			double u = (x - 16) * 0.8 + (y - 16) * 0.6, v = -(x - 16) * 0.6 + (y - 16) * 0.8, e = u * u / 196 + v * v / 16;
			return e <= 1 && e >= 0.72 && (v > 0 || Math.hypot(x - 16, y - 16) > 10);
		}, c(HeavenDemon.SCARLET));
		return k.outline();
	}
}
