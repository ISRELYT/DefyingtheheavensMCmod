import java.awt.image.BufferedImage;

/**
 * The techniques' icons for the ability wheel and the Abilities tab (textures/gui/ability/<id>.png, 32 x 32, in the Law domains' gold
 * trim): Universe in a Sleeve's (Sleeves); the secret realms' war arts', Sword Intent's and Blood Battle's (WarArts: drawn 2026-11-01, they
 * had none). {@code java BeastArt.java <assets> techniques} writes these alone.
 */
public class Techniques {
	static void writeAll(java.nio.file.Path assets) throws java.io.IOException {
		BeastArt.write(assets.resolve("textures/gui/ability/sleeve.png"), sleeve());
		BeastArt.write(assets.resolve("textures/gui/ability/sword_intent.png"), swordIntent());
		BeastArt.write(assets.resolve("textures/gui/ability/blood_battle.png"), bloodBattle());
	}

	/** One blade of a sword along (x1, y1) to its point (x2, y2): steel lit along one edge, a guard of gold and a dark grip behind it. */
	private static void sword(java.awt.Graphics2D g, double x1, double y1, double x2, double y2) {
		double dx = x2 - x1, dy = y2 - y1, d = Math.hypot(dx, dy);
		dx /= d;
		dy /= d;
		double nx = -dy, ny = dx;
		java.awt.geom.GeneralPath blade = new java.awt.geom.GeneralPath();
		blade.moveTo(x1 + nx * 1.2, y1 + ny * 1.2);
		blade.lineTo(x2 - dx * 2.2 + nx * 1.2, y2 - dy * 2.2 + ny * 1.2);
		blade.lineTo(x2, y2);
		blade.lineTo(x2 - dx * 2.2 - nx * 1.2, y2 - dy * 2.2 - ny * 1.2);
		blade.lineTo(x1 - nx * 1.2, y1 - ny * 1.2);
		blade.closePath();
		BodyArts.fill(g, blade, x1 + nx * 2, y1 + ny * 2, 0xF6F9FF, x1 - nx * 2, y1 - ny * 2, 0x8A98AE, 0x1A2030, 0.7f);
		BodyArts.line(g, x1 + dx * 1.5, y1 + dy * 1.5, x2 - dx * 3.5, y2 - dy * 3.5, 0.5f, BodyArts.c(0x5A6A84));
		BodyArts.line(g, x1 - dx * 0.4 + nx * 3, y1 - dy * 0.4 + ny * 3, x1 - dx * 0.4 - nx * 3, y1 - dy * 0.4 - ny * 3, 1.8f, BodyArts.c(0xE8B840));
		BodyArts.line(g, x1 - dx * 1.2, y1 - dy * 1.2, x1 - dx * 4.4, y1 - dy * 4.4, 1.6f, BodyArts.c(0x3A2A1A));
		g.setColor(BodyArts.c(0xE8B840));
		g.fill(new java.awt.geom.Ellipse2D.Double(x1 - dx * 5.2 - 1.1, y1 - dy * 5.2 - 1.1, 2.2, 2.2));
	}

	/** Sword Intent: a sword point up to the right on a night of deep blue, three streaks of pale intent running beside its blade. */
	static BufferedImage swordIntent() {
		BufferedImage img = BodyArts.disc(0x0E1630, 0x1E2E5E);
		java.awt.Graphics2D g = BodyArts.inside(img);
		for (double off : new double[] {-3.4, 3.0, 5.6}) {
			BodyArts.line(g, 8.6 + off * 0.7, 21.0 + off * 0.7, 22.0 + off * 0.7, 7.6 + off * 0.7, off > 4 ? 0.6f : 0.9f,
					BodyArts.c(0xA8E8FF, off > 4 ? 0.35 : 0.6));
		}
		sword(g, 11.4, 20.6, 24.6, 7.4);
		BodyArts.glint(g, 24.6, 7.4, 2.4, BodyArts.c(0xE8FAFF));
		g.dispose();
		return img;
	}

	/** Blood Battle: two blades crossed on a field of dark red, blood dripping from where they meet. */
	static BufferedImage bloodBattle() {
		BufferedImage img = BodyArts.disc(0x1E0808, 0x3E1010);
		java.awt.Graphics2D g = BodyArts.inside(img);
		sword(g, 10.4, 22.0, 23.6, 8.4);
		sword(g, 21.6, 22.0, 8.4, 8.4);
		java.awt.geom.GeneralPath drop = new java.awt.geom.GeneralPath();
		drop.moveTo(16, 17.0);
		drop.curveTo(16, 17.0, 19.4, 21.2, 19.4, 23.2);
		drop.curveTo(19.4, 25.2, 17.9, 26.6, 16, 26.6);
		drop.curveTo(14.1, 26.6, 12.6, 25.2, 12.6, 23.2);
		drop.curveTo(12.6, 21.2, 16, 17.0, 16, 17.0);
		drop.closePath();
		BodyArts.fill(g, drop, 13, 19, 0xE8262E, 19, 26, 0x6A0408, 0x220204, 0.8f);
		g.setColor(BodyArts.c(0xFFFFFF, 0.6));
		g.fill(new java.awt.geom.Ellipse2D.Double(13.9, 21.4, 1.2, 2.4));
		g.setColor(BodyArts.c(0xC81820));
		g.fill(new java.awt.geom.Ellipse2D.Double(15.3, 13.6, 1.4, 2.0));
		g.dispose();
		return img;
	}

	/**
	 * Universe in a Sleeve (袖里乾坤): in the domains' gold-trimmed disc, on a dusk of deep indigo, a wide robe's sleeve of white silk shaded
	 * azure sweeping down from the upper left, its cuff banded with gold, its open mouth at the lower right a whole small sky of stars
	 * turning in a swirl of violet light.
	 */
	static BufferedImage sleeve() {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int duskTop = BeastArt.rgb(0x1A1440), duskLow = BeastArt.rgb(0x3A2C6E), white = BeastArt.rgb(0xF4F2FF);
		int gold = BeastArt.rgb(0xD9A22E), goldLight = BeastArt.rgb(0xF5CF55), goldDeep = BeastArt.rgb(0x6E5214);
		int silk = BeastArt.rgb(0xEEF2FA), silkShade = BeastArt.rgb(0xB8C6E6), silkDeep = BeastArt.rgb(0x7E8FC2), fold = BeastArt.rgb(0x5E6EA6);
		int voidDeep = BeastArt.rgb(0x07061A), swirl = BeastArt.rgb(0x8E6CF0), swirlLight = BeastArt.rgb(0xCDB8FF), star = BeastArt.rgb(0xFFF8DC);
		// The disc: dusk within, the rim of light, the gold trim.
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				double dx = x - 15.5, dy = y - 15.5, r = Math.sqrt(dx * dx + dy * dy);
				int c = 0;
				if (r < 12.6) c = BeastArt.mix(duskTop, duskLow, (y - 3) / 24.0);
				else if (r < 13.5) c = BeastArt.mix(duskLow, white, 0.22);
				else if (r < 14.3) c = dy < 0 ? goldLight : gold;
				else if (r < 15.2) c = dy < 0 ? gold : goldDeep;
				else if (r < 15.8) c = 0xA0 << 24 | goldDeep & 0xFFFFFF;
				img.setRGB(x, y, c);
			}
		}
		// The sleeve: a band sweeping from the upper left down to its mouth at the lower right, widening as it goes, its upper edge lit.
		double mx = 19.5, my = 19.5; // the mouth's middle
		for (int y = 3; y <= 29; y++) {
			for (int x = 3; x <= 29; x++) {
				double dx = x - 15.5, dy = y - 15.5;
				if (dx * dx + dy * dy >= 12.6 * 12.6) continue;
				// Along the sleeve (t: 0 at its top, 1 at the mouth) and across it (s), about a gently bowed line.
				double t = ((x - 5.5) + (y - 5.5)) / ((mx - 5.5) + (my - 5.5));
				if (t < 0 || t > 1.02) continue;
				double lineY = 5.5 + t * (my - 5.5) - 2.4 * Math.sin(Math.PI * t); // bowed up a little in its middle
				double lineX = 5.5 + t * (mx - 5.5) + 2.4 * Math.sin(Math.PI * t);
				double s = ((x - lineX) - (y - lineY)) / Math.sqrt(2);
				double half = 2.6 + 5.4 * t;
				if (Math.abs(s) > half) continue;
				double across = s / half; // -1 (its lit upper edge) .. 1 (its shaded lower edge)
				int c = across < -0.55 ? silk : across < 0.1 ? BeastArt.mix(silk, silkShade, (across + 0.55) / 0.65) : BeastArt.mix(silkShade, silkDeep, (across - 0.1) / 0.9);
				// The folds of the silk, curving down its length.
				if (Math.abs(Math.sin((t * 9 + across * 1.4) * Math.PI)) < 0.16 && t > 0.15 && t < 0.86) c = BeastArt.mix(c, fold, 0.55);
				// The cuff: a band of gold before the mouth.
				if (t > 0.86 && t < 0.95) c = across < -0.2 ? goldLight : across < 0.5 ? gold : goldDeep;
				img.setRGB(x, y, c);
			}
		}
		// The mouth: an ellipse of the void, stars in it, a swirl of violet light turning about its heart.
		for (int y = 11; y <= 28; y++) {
			for (int x = 11; x <= 28; x++) {
				double dx = x - 15.5, dy = y - 15.5;
				if (dx * dx + dy * dy >= 12.6 * 12.6) continue;
				double ux = (x - mx - 0.5), uy = (y - my - 0.5);
				double a = (ux - uy) / Math.sqrt(2), b = (ux + uy) / Math.sqrt(2); // across the cuff, and along the sleeve
				double e = (a / 7.2) * (a / 7.2) + (b / 4.2) * (b / 4.2);
				if (e > 1) continue;
				double angle = Math.atan2(b * 1.7, a), rr = Math.sqrt(e);
				double arm = Math.sin(angle * 2 + rr * 6.5);
				int c = BeastArt.mix(voidDeep, swirl, Math.max(0.3, arm) * (1 - rr * 0.55));
				if (arm > 0.6 && rr < 0.85) c = BeastArt.mix(c, swirlLight, 0.75);
				if (rr < 0.28) c = BeastArt.mix(swirlLight, star, 1 - rr / 0.28);
				if (BeastArt.hash(x, y, 91) < 0.16 && rr > 0.3) c = BeastArt.mix(c, star, 0.7 + 0.3 * BeastArt.hash(x, y, 92));
				if (e > 0.78) c = e > 0.9 ? goldLight : gold; // the cuff's rim of gold
				img.setRGB(x, y, c);
			}
		}
		// Its heart: a bright point of starlight and its four rays.
		img.setRGB(20, 20, star);
		for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) img.setRGB(20 + d[0], 20 + d[1], BeastArt.mix(swirlLight, star, 0.6));
		return img;
	}
}
