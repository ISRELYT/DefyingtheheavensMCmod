/**
 * Sacred Vessel's spell icons (Icons#ICONS lists them): each treasure as the spell calls it, in Icons' way: a few bold shapes, shaded,
 * outlined dark.
 */
public class VesselIcons {
	static Icons.Canvas canvas() { return new Icons.Canvas(); }

	static int c(int rgb) { return Icons.c(rgb); }

	/** Heaven-Swallowing Gourd: a jade gourd tipped mouth down, its whirl drawing in a stream of clods and leaves. */
	static Icons.Canvas gourd() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 6; i++) k.fill(Icons.disc(8 - i * 0.6 + (i % 2), 22 + i * 1.6, 1.0 + 0.15 * i), c(i % 2 == 0 ? Vessel.SOIL_LIGHT : Wood.LEAF));
		k.fill(Icons.ellipse(19, 10, 8.5, 7), Icons.shaded(Vessel.GOURD, 11, 3, 27, 17));
		k.fill(Icons.ellipse(14, 19.5, 5, 4.5), Icons.shaded(Vessel.GOURD, 9, 15, 19, 24));
		k.fill(Icons.line(16.2, 14.3, 13.2, 17.4, 1.3), c(Earth.GOLD_SCALE));
		k.fill(Icons.line(11.5, 22, 9.5, 24.5, 2.3), c(Earth.GOLD_SCALE));
		k.fill(Icons.disc(9.2, 25, 1.7), c(Vessel.GOURD_GLOW)).fill(Icons.disc(9.2, 25, 0.8), c(0x041810));
		k.fill(Icons.line(23, 4, 27, 1, 0.7), c(Wood.LEAF_DARK)).fill(Icons.ellipse(28, 3.5, 2.6, 1.7), c(Wood.LEAF));
		k.fill(Icons.disc(16, 7, 1.4), c(Vessel.GOURD_PALE));
		return k.outline();
	}

	/** Seven-Treasure Bodhi Branch: a branch swept across, heart-shaped leaves on it, the seven treasures hanging from it. */
	static Icons.Canvas bodhiBranch() {
		Icons.Canvas k = canvas();
		k.fill(Icons.line(3, 26, 29, 7, 1.6), c(Vessel.BODHI_BARK)).fill(Icons.line(3, 25.4, 29, 6.4, 0.5), c(Vessel.BODHI_BARK_LIGHT));
		for (int i = 0; i < 6; i++) {
			double x = 6 + i * 4.2, y = 23.8 - i * 3.1;
			k.fill(Icons.ellipse(x - 1, y - 3, 2.2, 1.6), c(i % 2 == 0 ? Vessel.BODHI_LEAF : Vessel.BODHI_LEAF_GOLD));
		}
		for (int i = 0; i < 7; i++) {
			double x = 5 + i * 3.6, y = 24 - i * 2.7;
			k.fill(Icons.line(x, y, x, y + 4, 0.25), c(Vessel.BODHI_GOLD));
			k.fill(Icons.disc(x, y + 5, 1.3), c(Vessel.TREASURES[i]));
		}
		return k.outline();
	}

	/** Wind-Fire Wheels: two wheels of fire, one behind the other, flames streaming back off their rims, wind round them. */
	static Icons.Canvas windFireWheels() {
		Icons.Canvas k = canvas();
		for (int w = 0; w < 2; w++) {
			double cx = w == 0 ? 11 : 20, cy = w == 0 ? 12 : 20, r = w == 0 ? 6.5 : 8.5;
			for (int i = 0; i < 10; i++) {
				double a = Math.PI * 2 * i / 10;
				k.fill(Icons.tri(cx + Math.cos(a) * r, cy + Math.sin(a) * r, cx + Math.cos(a + 0.3) * r, cy + Math.sin(a + 0.3) * r, cx + Math.cos(a) * (r + 3.2) + 1.5, cy + Math.sin(a) * (r + 3.2) - 1),
						c(i % 2 == 0 ? Fire.FLAME : Fire.AMBER));
			}
			double rr = r;
			k.fill((x, y) -> Math.abs(Math.hypot(x - cx, y - cy) - rr) < 1.4, c(Fire.VERMILION));
			for (int s = 0; s < 4; s++) {
				double a = Math.PI * s / 4;
				k.fill(Icons.line(cx - Math.cos(a) * r, cy - Math.sin(a) * r, cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0.45), c(Fire.AMBER));
			}
			k.fill(Icons.disc(cx, cy, 1.6), c(0xFFF4D0));
		}
		k.fill(Icons.line(2, 28, 9, 26, 0.5), c(Vessel.WIND)).fill(Icons.line(1, 24, 6, 23, 0.4), c(Vessel.WIND));
		return k.outline();
	}

	/** Eight Trigrams Furnace: the round bronze furnace on its three legs, Samadhi fire in its window, the eight trigrams round it, flames leaping from under its lid. */
	static Icons.Canvas furnace() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 5; i++) {
			double x = 9 + i * 3.5;
			k.fill(Icons.tri(x - 1.6, 9, x + 1.6, 9, x + (i % 2 == 0 ? -0.6 : 0.6), 1.5 + (i % 2) * 2), c(i % 2 == 0 ? Vessel.SAMADHI[3] : Vessel.SAMADHI[4]));
		}
		k.fill(Icons.rect(6, 22, 8.5, 29), c(Vessel.FURNACE)).fill(Icons.rect(23.5, 22, 26, 29), c(Vessel.FURNACE)).fill(Icons.rect(14.8, 24, 17.2, 30), c(Vessel.FURNACE_DARK));
		k.fill(Icons.ellipse(16, 18, 10.5, 7.5), Icons.shaded(Vessel.FURNACE, 5, 10, 27, 26));
		k.fill(Icons.rect(6.5, 10.5, 25.5, 12), c(Vessel.FURNACE_GILT)).fill(Icons.ellipse(16, 10, 8, 2.4), c(Vessel.FURNACE_LIGHT)).fill(Icons.disc(16, 7.4, 1.4), c(Vessel.FURNACE_GILT));
		k.fill(Icons.rect(3.5, 12, 6, 16), c(Vessel.FURNACE_GILT)).fill(Icons.rect(26, 12, 28.5, 16), c(Vessel.FURNACE_GILT));
		k.fill(Icons.disc(16, 18.5, 3.6), (x, y) -> c(SpellArt.mix(Vessel.SAMADHI[4], Vessel.SAMADHI[1], Math.min(1, Math.hypot(x - 16, y - 18.5) / 3.6))));
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8, x = 16 + Math.cos(a) * 6, y = 18.5 + Math.sin(a) * 5, px = -Math.sin(a), py = Math.cos(a);
			k.fill(Icons.line(x - px * 0.9, y - py * 0.9, x + px * 0.9, y + py * 0.9, 0.42), c(Vessel.FURNACE_GILT));
		}
		return k.outline();
	}

	/** Xirang, the Self-Growing Soil: a mound of dark earth swelling up, gold root-veins in it, a shoot on its crown, clods heaving off it. */
	static Icons.Canvas xirang() {
		Icons.Canvas k = canvas();
		k.fill((x, y) -> y > 13 + Math.pow((x - 16) / 13, 2) * 13 && y < 29 && x > 2 && x < 30, Icons.shaded(Vessel.SOIL, 3, 13, 29, 29));
		k.fill(Icons.line(16, 15, 12, 24, 0.5), c(Vessel.SOIL_GOLD)).fill(Icons.line(16, 15, 21, 25, 0.5), c(Vessel.SOIL_GOLD)).fill(Icons.line(12, 24, 7, 27, 0.4), c(Vessel.SOIL_GOLD))
				.fill(Icons.line(18.5, 20, 25, 23, 0.4), c(Vessel.SOIL_GOLD));
		k.fill(Icons.line(16, 13.5, 16, 8, 0.6), c(Wood.LEAF_DARK)).fill(Icons.ellipse(13.5, 7.5, 2.8, 1.6), c(Wood.LEAF)).fill(Icons.ellipse(18.6, 6.5, 2.8, 1.6), c(Wood.LEAF_LIGHT));
		for (double[] cl : new double[][] {{5, 10}, {27, 11}, {8, 6}, {25, 6}}) k.fill(Icons.disc(cl[0], cl[1], 1.3), c(Vessel.SOIL_LIGHT));
		return k.outline();
	}

	/** Nine Cauldrons: a great square cauldron of bronze in front, its glyph burning, two more behind it. */
	static Icons.Canvas nineCauldrons() {
		Icons.Canvas k = canvas();
		for (int s = -1; s <= 1; s += 2) ding(k, 16 + s * 9.5, 3, 0.55, SpellArt.shade(Vessel.CAULDRON, -0.22), false);
		ding(k, 16, 8, 1, Vessel.CAULDRON, true);
		return k.outline();
	}

	/** A square ding: its loop handles at {@code top}, lipped body, pillar legs, {@code scale} of the great one; {@code glyph} burns the land's glyph on its belly. */
	static void ding(Icons.Canvas k, double cx, double top, double scale, int bronze, boolean glyph) {
		double w = 9 * scale, lip = top + 4 * scale, body = lip + 2 * scale, foot = body + 11 * scale;
		int light = SpellArt.shade(bronze, 0.22), gilt = glyph ? Vessel.CAULDRON_GILT : SpellArt.shade(Vessel.CAULDRON_GILT, -0.25);
		for (int s = -1; s <= 1; s += 2) {
			double hx = cx + s * w * 0.62, hw = 1.6 * scale;
			k.fill(Icons.rect(hx - hw, top, hx + hw, top + 0.9 * scale + 0.4), c(gilt)).fill(Icons.rect(hx - hw, top, hx - hw + 0.9, lip), c(gilt)).fill(Icons.rect(hx + hw - 0.9, top, hx + hw, lip), c(gilt));
			k.fill(Icons.rect(cx + s * w * 0.6 - 1.1 * scale, foot - 0.5, cx + s * w * 0.6 + 1.1 * scale, foot + 5 * scale), c(SpellArt.shade(bronze, -0.12)));
		}
		k.fill(Icons.rect(cx - w * 0.9, body, cx + w * 0.9, foot), Icons.shaded(bronze, cx - w, body, cx + w, foot));
		k.fill(Icons.rect(cx - w, lip, cx + w, body), c(light));
		k.fill(Icons.rect(cx - w * 0.9, body + 0.6, cx + w * 0.9, body + 1.2 + 0.6 * scale), c(SpellArt.shade(bronze, -0.25)));
		if (!glyph) return;
		double gy = body + 3.5, gw = 3.6;
		k.fill(Icons.rect(cx - gw, gy, cx + gw, gy + 1), c(Vessel.CAULDRON_GLOW)).fill(Icons.rect(cx - 0.5, gy, cx + 0.5, gy + 5.5), c(Vessel.CAULDRON_GLOW))
				.fill(Icons.rect(cx - gw - 0.6, gy + 4.5, cx + gw + 0.6, gy + 5.5), c(Vessel.CAULDRON_GLOW));
	}

	/** Twenty-Four Sea-Fixing Pearls: a ring of pearls round a swirl of the sea, one of them flying off. */
	static Icons.Canvas seaPearls() {
		Icons.Canvas k = canvas();
		k.fill((x, y) -> {
			double r = Math.hypot(x - 15, y - 17), a = Math.atan2(y - 17, x - 15);
			return r < 6.5 && Math.sin(a * 2 + r * 0.9) > 0.1;
		}, c(Vessel.PEARL_SEA));
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			k.fill(Icons.disc(15 + Math.cos(a) * 10, 17 + Math.sin(a) * 10, 1.9), (x, y) -> c(SpellArt.mix(Vessel.PEARL, Vessel.PEARL_SHADE, (y - 7) / 22)));
		}
		k.fill(Icons.line(25, 7, 29, 3, 0.6), c(Vessel.PEARL_SEA)).fill(Icons.disc(29, 3, 2), c(Vessel.PEARL));
		return k.outline();
	}

	/** Ocean-Sealing Vase: the white vase painted with waves, tipped mouth down, streams of the sea spiralling up into it. */
	static Icons.Canvas oceanVase() {
		Icons.Canvas k = canvas();
		Icons.Paint porcelain = (x, y) -> c(SpellArt.mix(Vessel.VASE_WHITE, Vessel.VASE_SHADE, Math.max(0, (x - 13) / 12)));
		k.fill(Icons.rect(12.5, 1.5, 19.5, 3.5), c(Vessel.VASE_SHADE));
		k.fill(Icons.ellipse(16, 10, 8.2, 7.2), porcelain).fill(Icons.rect(14.2, 15, 17.8, 21), porcelain);
		k.fill((x, y) -> Math.abs(y - (10 + 1.6 * Math.sin(x * 0.95))) < 0.95 && Math.hypot((x - 16) / 8.2, (y - 10) / 7.2) < 0.97, c(Vessel.VASE_BLUE));
		k.fill((x, y) -> Math.abs(y - 5.4) < 0.5 && Math.hypot((x - 16) / 8.2, (y - 10) / 7.2) < 0.97, c(Vessel.VASE_BLUE)).fill(Icons.rect(14.2, 17.5, 17.8, 18.3), c(Vessel.VASE_BLUE));
		k.fill(Icons.tri(12.4, 23, 19.6, 23, 16, 19.5), c(Vessel.VASE_WHITE)).fill(Icons.rect(12.4, 22, 19.6, 23.2), c(Earth.GOLD_SCALE));
		for (int i = 0; i < 3; i++) {
			int ii = i;
			k.fill((x, y) -> {
				if (y < 23 || y > 31) return false;
				double f = (y - 23) / 8, cx = 16 + Math.sin(f * 4 + ii * 2.1) * (1 + f * 9);
				return Math.abs(x - cx) < 0.8;
			}, c(Vessel.PEARL_SEA));
		}
		return k.outline();
	}

	/** Immortal-Binding Golden Ropes: a coil of golden rope, a talisman hanging from it, two ropes lashing out of it. */
	static Icons.Canvas bindingRopes() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 3; i++) {
			int ii = i;
			k.fill((x, y) -> Math.abs(Math.hypot((x - 16) / 1.4, y - 10 - ii * 1.8) - 5.2) < 0.9, c(i % 2 == 0 ? Earth.GOLD_SCALE : Earth.GOLD_PALE));
		}
		k.fill(Icons.line(10, 14, 4, 29, 0.8), c(Earth.GOLD_SCALE)).fill(Icons.line(22, 14, 28, 29, 0.8), c(Earth.GOLD_SCALE));
		k.fill(Icons.line(10.3, 14, 4.3, 29, 0.3), c(Earth.GOLD_PALE)).fill(Icons.line(22.3, 14, 28.3, 29, 0.3), c(Earth.GOLD_PALE));
		k.fill(Icons.rect(14.5, 17, 17.5, 26), c(0xF2D884)).fill(Icons.rect(15.5, 18.5, 16.5, 24.5), c(0xD02A1C));
		return k.outline();
	}

	/** Golden Dragon Scissors: a pair of golden scissors flung open, their blades two dragons' bodies, their rings coiled tails. */
	static Icons.Canvas scissors() {
		Icons.Canvas k = canvas();
		for (int s = -1; s <= 1; s += 2) {
			double tx = 16 + s * 13.5, ty = 2.5, dx = tx - 16, dy = ty - 18, len = Math.hypot(dx, dy), px = -dy / len * 2.6, py = dx / len * 2.6;
			k.fill(Icons.tri(16 + px, 18 + py, 16 - px, 18 - py, tx, ty), Icons.shaded(Vessel.SCISSOR_GOLD, Math.min(16, tx), ty, Math.max(16, tx), 18));
			double ex = s < 0 ? 16 + px : 16 - px, ey = s < 0 ? 18 + py : 18 - py;
			k.fill(Icons.line(ex, ey, tx, ty, 0.42), c(Vessel.SCISSOR_EDGE));
			k.fill(Icons.line(16, 18, 16 + s * 5.4, 22.6, 1.15), c(Vessel.SCISSOR_GOLD_DEEP));
		}
		k.fill((x, y) -> Math.abs(Math.hypot(x - 10, y - 25.5) - 3.4) < 1.1 || Math.abs(Math.hypot(x - 22, y - 25.5) - 3.4) < 1.1, Icons.shaded(Vessel.SCISSOR_GOLD, 6, 22, 26, 29));
		k.fill(Icons.disc(16, 18, 1.9), c(Wood.JADE)).fill(Icons.disc(15.5, 17.4, 0.6), c(0xC8FFE6));
		return k.outline();
	}

	/** Mirror of Retribution: the round bronze mirror on its stand of bone, its face the dead's teal, flames of hell and ghost fire round it. */
	static Icons.Canvas karmaMirror() {
		Icons.Canvas k = canvas();
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10;
			k.fill(Icons.tri(16 + Math.cos(a - 0.2) * 9, 13 + Math.sin(a - 0.2) * 9, 16 + Math.cos(a + 0.2) * 9, 13 + Math.sin(a + 0.2) * 9, 16 + Math.cos(a) * 13, 13 + Math.sin(a) * 13),
					c(i % 2 == 0 ? Vessel.HELL_FIRE[3] : Ghost.FIRE_CYAN));
		}
		k.fill(Icons.disc(16, 13, 9.2), c(Vessel.MIRROR_BRONZE));
		k.fill(Icons.disc(16, 13, 7.4), (x, y) -> c(SpellArt.mix(Ghost.FIRE_DEEP, Ghost.FIRE_CYAN, Math.max(0, Math.sin(Math.atan2(y - 13, x - 16) * 2 + Math.hypot(x - 16, y - 13))))));
		k.fill(Icons.rect(14.5, 22, 17.5, 28), c(Ghost.BONE)).fill(Icons.rect(11, 28, 21, 30), c(Ghost.BONE_SHADE));
		return k.outline();
	}

	/** Soul-Locking Pagoda: a pagoda of black iron, seven tiers, soul lanterns burning teal at its eaves, a pearl of ghost fire on its spire. */
	static Icons.Canvas soulPagoda() {
		Icons.Canvas k = canvas();
		for (int t = 0; t < 6; t++) {
			double y = 27 - t * 3.8, w = 12 - t * 1.4;
			k.fill(Icons.rect(16 - w * 0.6, y - 2.6, 16 + w * 0.6, y), c(Vessel.IRON));
			k.fill(Icons.tri(16 - w - 1.4, y - 2.2, 16 + w + 1.4, y - 2.2, 16, y - 4.2), c(Vessel.IRON_LIGHT));
			k.fill(Icons.disc(16 - w - 1, y - 1.2, 0.8), c(Ghost.FIRE_CYAN)).fill(Icons.disc(16 + w + 1, y - 1.2, 0.8), c(Ghost.FIRE_CYAN));
		}
		k.fill(Icons.rect(15.3, 2, 16.7, 6.5), c(Vessel.IRON_LIGHT)).fill(Icons.disc(16, 2.5, 1.6), c(Ghost.FIRE_CYAN));
		return k.outline();
	}

	/** Heaven-Demon Bell: the great bell, heaven's gold on one side and the demon's crimson on the other, a ring of its toll round it. */
	static Icons.Canvas heavenDemonBell() {
		Icons.Canvas k = canvas();
		k.fill((x, y) -> Math.abs(Math.hypot(x - 16, (y - 18) * 1.4) - 14.5) < 0.6 && y > 17, c(HeavenDemon.GOLD_PALE));
		k.fill((x, y) -> {
			double t = (y - 6) / 20.0, half = 5.5 + 5 * Math.pow(Math.max(0, t), 1.3);
			return y > 6 && y < 26 && Math.abs(x - 16) < half;
		}, (x, y) -> c(x < 16 ? SpellArt.mix(HeavenDemon.GOLD_PALE, HeavenDemon.GOLD, (y - 6) / 20) : SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.BLOOD, (y - 6) / 20)));
		k.fill(Icons.rect(15.6, 6, 16.4, 26), c(HeavenDemon.WHITE_LIGHT));
		k.fill(Icons.rect(5, 24.8, 27, 26.4), (x, y) -> c(x < 16 ? HeavenDemon.GOLD_BRIGHT : HeavenDemon.CRIMSON));
		k.fill((x, y) -> Math.abs(Math.hypot(x - 16, y - 5) - 2.6) < 0.8 && y < 6, (x, y) -> c(x < 16 ? HeavenDemon.GOLD_BRIGHT : HeavenDemon.SCARLET));
		for (int r = 0; r < 2; r++) for (int col = 0; col < 3; col++) {
			double x = 10.5 + col * 2.3, y = 12 + r * 3;
			k.fill(Icons.disc(x, y, 0.7), c(HeavenDemon.GOLD_DEEP)).fill(Icons.disc(32 - x, y, 0.7), c(HeavenDemon.BLOOD_DARK));
		}
		return k.outline();
	}
}
