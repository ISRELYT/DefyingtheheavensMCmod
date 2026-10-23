import java.awt.image.BufferedImage;

/**
 * The races' acts' icons for the ability wheel and the Abilities tab (textures/gui/ability/<id>.png, 32 x 32, soft-edged like the
 * others): Tiger's Roar (a striped tiger's head, its jaws wide, the roar going out from it), Illusion (a fox's mask, pale, fading).
 */
public class Icons {
	static BufferedImage roar() {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int orange = BeastArt.rgb(0xE88A2A), dark = BeastArt.rgb(0x1C1410), white = BeastArt.rgb(0xF6EAD2), mouth = BeastArt.rgb(0x8A1A14);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				double dx = x - 11.5, dy = y - 16.5;
				double r = Math.sqrt(dx * dx / 1.0 + dy * dy / 1.2);
				int c = 0;
				if (r < 9.5) {
					c = BeastArt.fur(orange, x, y, 1);
					if ((x + y / 2) % 4 == 0 && Math.abs(dx) > 2.5 && dy < 3) c = dark; // stripes
					if (dy > 1 && Math.abs(dx) < 5) c = BeastArt.fur(white, x, y, 2); // muzzle
					if (dy > 3.5 && dy < 7.5 && Math.abs(dx) < 3.5) c = mouth; // the jaws wide
					if (dy > 3.5 && dy < 4.5 && (x == 9 || x == 14)) c = white; // fangs
					if (dy > -3.5 && dy < -2 && (x == 8 || x == 15)) c = BeastArt.rgb(0xF2C030); // eyes
				}
				// Ears.
				if ((Math.abs(x - 5) <= 1 || Math.abs(x - 18) <= 1) && y >= 5 && y <= 8) c = y == 5 ? dark : orange;
				// The roar going out: three arcs to the right.
				double ax = x - 14, ay = y - 20;
				double ar = Math.sqrt(ax * ax + ay * ay);
				if (ax > 4 && Math.abs(ay) < ax * 0.9) {
					for (double ring : new double[] {9, 13, 17}) {
						if (Math.abs(ar - ring) < 0.8) c = BeastArt.mix(BeastArt.rgb(0xFFFFFF), 0x00FFFFFF, (ring - 9) / 14.0);
					}
				}
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	static BufferedImage illusion() {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int pale = BeastArt.rgb(0xE8E0FF), violet = BeastArt.rgb(0x9A6AE8), dark = BeastArt.rgb(0x2A1E3A);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				int c = 0;
				double dx = x - 15.5, dy = y - 17;
				// A fox's mask: a broad top narrowing to the snout, two tall ears.
				double half = dy < -2 ? 10 : 10 - (dy + 2) * 1.0;
				if (dy > -6 && dy < 11 && Math.abs(dx) < half) c = BeastArt.mix(pale, violet, Math.min(1, (dy + 6) / 20.0));
				for (int side : new int[] {-1, 1}) {
					double ex = dx - side * 6;
					if (dy <= -6 && dy > -14 && Math.abs(ex) < (dy + 14) * 0.45) c = BeastArt.mix(pale, violet, 0.3);
				}
				if (c != 0 && Math.abs(dy + 1) < 1 && Math.abs(Math.abs(dx) - 4.5) < 1.6) c = dark; // the eye holes
				if (c != 0 && dy > 9) c = dark; // the nose
				// Fading to nothing toward the right, as an illusion goes.
				if (c != 0) {
					double fade = Math.max(0, Math.min(1, (x - 14) / 18.0));
					int alpha = (int) Math.round(255 * (1 - fade * 0.8));
					c = alpha << 24 | (c & 0xFFFFFF);
				}
				if (c == 0 && BeastArt.hash(x, y, 9) < 0.03 && Math.abs(dx) < 15) c = BeastArt.rgb(0xFFFFFF); // sparkles
				img.setRGB(x, y, c);
			}
		}
		return img;
	}
}
