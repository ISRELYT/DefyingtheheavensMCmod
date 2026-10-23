import java.awt.image.BufferedImage;

/**
 * A demon race's whole player skin (a broad-armed 64 x 64 one, vanilla's layout), worn in place of the player's own (the user's rule,
 * 2026-10-23: "Your own skin MUST change to match that of your race"): fur or scales all over, a lighter belly, clawed hands and
 * feet, loose trousers and a sash. Its head is drawn over by the beast's own head but painted all the same (the face in the player
 * list, and wherever the head is drawn plain). The outer layers are left bare.
 */
public class Skins {
	static BufferedImage skin(String race, Beasts.Palette p) {
		BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		boolean scaled = race.equals("lizard") || race.equals("dragon");
		BeastArt.paintBox(img, 0, 0, 8, 8, 8, (f, x, y, w, h, d) -> head(race, p, scaled, f, x, y));
		BeastArt.paintBox(img, 16, 16, 8, 12, 4, (f, x, y, w, h, d) -> body(race, p, scaled, f, x, y, w));
		BeastArt.paintBox(img, 40, 16, 4, 12, 4, (f, x, y, w, h, d) -> arm(race, p, scaled, f, x, y, 1));
		BeastArt.paintBox(img, 32, 48, 4, 12, 4, (f, x, y, w, h, d) -> arm(race, p, scaled, f, x, y, 2));
		BeastArt.paintBox(img, 0, 16, 4, 12, 4, (f, x, y, w, h, d) -> leg(race, p, scaled, f, x, y, 3));
		BeastArt.paintBox(img, 16, 48, 4, 12, 4, (f, x, y, w, h, d) -> leg(race, p, scaled, f, x, y, 4));
		return img;
	}

	/** The coat on a limb or the body: a tiger's stripes running round it, scales, or plain fur. */
	static int coat(String race, Beasts.Palette p, boolean scaled, BeastArt.Face f, int x, int y, int seed) {
		int px = x + f.ordinal() * 19;
		if (scaled) {
			boolean edge = y % 2 == 0 && Math.floorMod(x + (y / 2) % 2, 2) == 0;
			int c = edge ? BeastArt.mix(p.base(), p.dark(), 0.5) : p.base();
			return BeastArt.hash(px, y, seed) < 0.1 ? BeastArt.mix(c, p.light(), 0.45) : c;
		}
		if (race.equals("tiger") && f != BeastArt.Face.UP && f != BeastArt.Face.DOWN) {
			double wobble = Math.sin(px * 1.1 + seed) * 0.7;
			if (Math.floorMod((int) Math.floor(y + wobble + seed), 3) == 0 && BeastArt.hash(px, y, seed) < 0.8) return p.dark();
		}
		return BeastArt.fur(p.base(), px, y, seed);
	}

	static int head(String race, Beasts.Palette p, boolean scaled, BeastArt.Face f, int x, int y) {
		if (f == BeastArt.Face.FRONT) {
			if (y == 3 && (x == 1 || x == 2 || x == 5 || x == 6)) return (x == 2 || x == 5) ? 0xFF0A0A0A : p.eye();
			if (y == 5 && (x == 3 || x == 4)) return p.nose();
			if (y >= 5 && !scaled) return BeastArt.fur(p.light(), x, y, 201);
			if (y >= 6 && scaled) return BeastArt.fur(p.belly(), x, y, 202);
		}
		if (f == BeastArt.Face.UP && race.equals("tiger") && x % 3 == 1) return p.dark();
		return coat(race, p, scaled, f, x, y, 200);
	}

	/** The body: the belly lighter down the front (plated for a lizard or a dragon), a sash round the waist. */
	static int body(String race, Beasts.Palette p, boolean scaled, BeastArt.Face f, int x, int y, int w) {
		if (f != BeastArt.Face.UP && f != BeastArt.Face.DOWN && y >= 10) {
			if (y == 10 && f == BeastArt.Face.FRONT && x == 3) return BeastArt.shade(p.sash(), 0.35); // the sash's knot
			return BeastArt.fur(p.sash(), x, y, 210);
		}
		if (f == BeastArt.Face.DOWN) return p.cloth();
		if (f == BeastArt.Face.FRONT && x >= 2 && x <= 5 && y >= 1) {
			if (scaled) return y % 2 == 0 ? BeastArt.mix(p.belly(), p.dark(), 0.25) : p.belly();
			return BeastArt.fur(p.belly(), x, y, 211);
		}
		return coat(race, p, scaled, f, x, y, 212);
	}

	/** An arm: the coat, a fox's dark below the elbow, the hand clawed at its end. */
	static int arm(String race, Beasts.Palette p, boolean scaled, BeastArt.Face f, int x, int y, int seed) {
		if (f == BeastArt.Face.DOWN) return BeastArt.mix(p.dark(), 0xFF000000, 0.3);
		if (f == BeastArt.Face.UP) return coat(race, p, scaled, f, x, y, 220 + seed);
		if (y == 11 && f == BeastArt.Face.FRONT && x % 2 == 0) return p.horn(); // claws
		if (race.equals("fox") && y >= 7) return BeastArt.fur(p.dark(), x, y, 221);
		if (y >= 10) return BeastArt.fur(BeastArt.mix(p.base(), p.dark(), scaled ? 0.3 : 0.15), x, y, 222);
		return coat(race, p, scaled, f, x, y, 223 + seed);
	}

	/** A leg: loose trousers to the shin, the coat below, the foot clawed. */
	static int leg(String race, Beasts.Palette p, boolean scaled, BeastArt.Face f, int x, int y, int seed) {
		if (f == BeastArt.Face.DOWN) return BeastArt.mix(p.dark(), 0xFF000000, 0.3);
		if (f == BeastArt.Face.UP) return p.cloth();
		if (y < 8) {
			int cloth = BeastArt.fur(p.cloth(), x, y, 230 + seed);
			return y == 7 ? BeastArt.shade(cloth, -0.25) : cloth; // the hem
		}
		if (y == 11 && f == BeastArt.Face.FRONT && x % 2 == 1) return p.horn();
		if (race.equals("fox")) return BeastArt.fur(p.dark(), x, y, 231);
		return coat(race, p, scaled, f, x, y, 232 + seed);
	}
}
