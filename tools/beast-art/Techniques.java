import java.awt.image.BufferedImage;

/**
 * The techniques' icons for the ability wheel and the Abilities tab (textures/gui/ability/<id>.png, 32 x 32, in the Law domains' gold
 * trim): Universe in a Sleeve's (Sleeves). {@code java BeastArt.java <assets> techniques} writes these alone.
 */
public class Techniques {
	static void writeAll(java.nio.file.Path assets) throws java.io.IOException {
		BeastArt.write(assets.resolve("textures/gui/ability/sleeve.png"), sleeve());
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
