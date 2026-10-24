import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Dao Integration and Void Shattering spells' icons, and those of the spells of both paths, for the spell wheel and the Spells tab
 * (textures/gui/spell/<id>.png, 32 x 32), drawn as the others
 * are: a few bold shapes, shaded, outlined dark. Each shape is a test on a pixel's middle and a paint for it, laid in order; the outline
 * is laid last round whatever was drawn.
 * <p>
 * Run: {@code java Icons.java <the mod's src/main/resources/assets/defying-the-heavens>}.
 */
public class Icons {
	interface Test {
		boolean in(double x, double y);
	}

	interface Paint {
		int at(double x, double y);
	}

	static final class Canvas {
		final int[] px = new int[32 * 32];

		/** Paints {@code paint} wherever {@code test} holds (a pixel's middle at x + 0.5, y + 0.5). */
		Canvas fill(Test test, Paint paint) {
			for (int y = 0; y < 32; y++) {
				for (int x = 0; x < 32; x++) {
					double cx = x + 0.5, cy = y + 0.5;
					if (!test.in(cx, cy)) continue;
					int c = paint.at(cx, cy);
					if (c >>> 24 != 0) px[y * 32 + x] = c;
				}
			}
			return this;
		}

		Canvas fill(Test test, int argb) { return fill(test, (x, y) -> argb); }

		/** A dark rim round what is drawn: each clear pixel touching a drawn one takes that one's colour, darkened. */
		Canvas outline() {
			int[] out = px.clone();
			for (int y = 0; y < 32; y++) {
				for (int x = 0; x < 32; x++) {
					if (px[y * 32 + x] >>> 24 != 0) continue;
					int[][] near = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
					for (int[] d : near) {
						int nx = x + d[0], ny = y + d[1];
						if (nx < 0 || ny < 0 || nx >= 32 || ny >= 32) continue;
						int c = px[ny * 32 + nx];
						if (c >>> 24 == 0) continue;
						out[y * 32 + x] = 0xFF000000 | SpellArt.mix(c & 0xFFFFFF, 0x0A0608, 0.72);
						break;
					}
				}
			}
			System.arraycopy(out, 0, px, 0, px.length);
			return this;
		}

		BufferedImage image() {
			BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
			img.setRGB(0, 0, 32, 32, px, 0, 32);
			return img;
		}
	}

	static int c(int rgb) { return 0xFF000000 | rgb; }

	static Test disc(double cx, double cy, double r) { return (x, y) -> (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r; }

	static Test ellipse(double cx, double cy, double rx, double ry) { return (x, y) -> (x - cx) * (x - cx) / (rx * rx) + (y - cy) * (y - cy) / (ry * ry) <= 1; }

	static Test rect(double x0, double y0, double x1, double y1) { return (x, y) -> x >= x0 && x <= x1 && y >= y0 && y <= y1; }

	/** Within {@code w} of the segment from (ax, ay) to (bx, by). */
	static Test line(double ax, double ay, double bx, double by, double w) { return (x, y) -> Fire.segment(x, y, ax, ay, bx, by) <= w; }

	/** Inside the triangle. */
	static Test tri(double ax, double ay, double bx, double by, double cx, double cy) {
		return (x, y) -> {
			double d1 = (x - bx) * (ay - by) - (ax - bx) * (y - by), d2 = (x - cx) * (by - cy) - (bx - cx) * (y - cy), d3 = (x - ax) * (cy - ay) - (cx - ax) * (y - ay);
			boolean neg = d1 < 0 || d2 < 0 || d3 < 0, pos = d1 > 0 || d2 > 0 || d3 > 0;
			return !(neg && pos);
		};
	}

	/** {@code base} lit toward the top left and shadowed toward the bottom right of the box (x0, y0)..(x1, y1). */
	static Paint shaded(int base, double x0, double y0, double x1, double y1) {
		return (x, y) -> {
			double t = ((x - x0) / Math.max(1, x1 - x0) + (y - y0) / Math.max(1, y1 - y0)) / 2;
			return c(SpellArt.shade(base, 0.28 - 0.56 * t));
		};
	}

	// --- The icons ---

	/** Azure Dragon: an azure serpent's body coiling up, leaf-fins down its back, its antlered head at the top. */
	static Canvas azureDragon() {
		Canvas k = new Canvas();
		for (int i = 0; i <= 60; i++) {
			double t = i / 60.0, x = 7 + 15 * t + 5 * Math.sin(t * Math.PI * 2.1), y = 28 - 19 * t, r = 1.3 + 2.1 * t;
			k.fill(disc(x + 1.4, y - 1.6, r * 0.55), c(Wood.LEAF));
			k.fill(disc(x, y, r), (px, py) -> c(SpellArt.mix(Wood.AZURE, Wood.JADE, (py - y + r) / (2 * r))));
		}
		for (int i = 0; i <= 60; i++) {
			double t = i / 60.0, x = 7 + 15 * t + 5 * Math.sin(t * Math.PI * 2.1), y = 28 - 19 * t, r = 1.3 + 2.1 * t;
			k.fill(disc(x - r * 0.35, y + r * 0.35, r * 0.3), c(Wood.BELLY));
		}
		k.fill(line(23, 6.5, 22, 1.5, 0.7), c(Wood.BARK_LIGHT)).fill(line(25.5, 7, 27.5, 2, 0.7), c(Wood.BARK_LIGHT));
		k.fill(ellipse(25.5, 9.5, 4.5, 3.2), shaded(Wood.AZURE, 21, 6, 30, 13)).fill(rect(27, 9.5, 31, 12), c(Wood.JADE));
		k.fill(disc(25, 8.6, 0.8), c(0xFFF2A0)).fill(rect(28, 11.5, 31, 12.5), c(0xE8FFF4));
		return k.outline();
	}

	/** Myriad Woods Return to the Dao: a tree of jade light, its crown haloed, fruits of light in it. */
	static Canvas daoTree() {
		Canvas k = new Canvas();
		k.fill((x, y) -> {
			double r = Math.hypot(x - 16, y - 11);
			return r > 12.2 && r < 13.4;
		}, c(0xB0FFE2));
		k.fill(rect(13.5, 16, 18.5, 29), shaded(Wood.JADE_WOOD, 13, 16, 19, 29));
		k.fill(tri(9, 30, 16, 22, 23, 30), shaded(Wood.JADE_WOOD, 9, 22, 23, 30));
		k.fill(line(16, 21, 9, 15, 1.2), c(Wood.JADE_WOOD_LIGHT)).fill(line(16, 20, 23, 14, 1.2), c(Wood.JADE_WOOD_LIGHT));
		k.fill(disc(10, 13, 5.5), shaded(Wood.LEAF, 4, 7, 16, 19)).fill(disc(22, 13, 5.5), shaded(Wood.LEAF, 16, 7, 28, 19)).fill(disc(16, 9, 7), shaded(Wood.LEAF, 9, 2, 23, 16));
		k.fill(disc(13, 6.5, 2), c(Wood.LEAF_LIGHT));
		for (double[] f : new double[][] {{11, 14}, {20, 10}, {16, 13}, {23, 15}}) k.fill(disc(f[0], f[1], 1), c(0xFFF6C0));
		k.fill(line(16, 22, 16, 28, 0.5), c(Wood.GROOVE));
		return k.outline();
	}

	/** Vermilion Bird's Descent: the bird stooping, its wings flung up in a V, its streamers trailing above it, flame below. */
	static Canvas vermilionBird() {
		Canvas k = new Canvas();
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			k.fill(tri(16, 14, 16 + s * 15, 2, 16 + s * 11, 13), (x, y) -> {
				double f = Math.abs(x - 16) / 15;
				int col = SpellArt.ramp(f, Fire.CRIMSON, Fire.VERMILION, Fire.FLAME, Fire.GOLD);
				return c((int) Math.floor(Math.abs(x - 16) + y * 0.6 * ss) % 3 == 0 ? SpellArt.shade(col, -0.25) : col);
			});
		}
		for (double sx : new double[] {11, 16, 21}) {
			k.fill(line(16, 12, sx, 3, 0.6), c(Fire.VERMILION));
			k.fill(disc(sx, 3, 1.6), c(Fire.GOLD)).fill(disc(sx, 3, 0.7), c(Fire.WHITE_FIRE));
		}
		k.fill(ellipse(16, 16, 3.2, 5), shaded(Fire.VERMILION, 12, 11, 20, 21));
		k.fill(disc(16, 21.5, 2.6), c(Fire.RUDDY)).fill(tri(14.8, 23, 17.2, 23, 16, 27), c(Fire.HORN)).fill(disc(15, 21, 0.6), c(Fire.WHITE_FIRE));
		k.fill((x, y) -> y > 26 && Math.abs(x - 16) < 6 - (y - 26) * 1.2 && Math.sin(x * 1.7) > -0.3, (x, y) -> c(SpellArt.mix(Fire.FLAME, Fire.GOLD_LIGHT, (y - 26) / 5)));
		return k.outline();
	}

	/** One Flame Burns the Heavens: a pillar of fire, white at its heart, over its molten pool. */
	static Canvas oneFlame() {
		Canvas k = new Canvas();
		k.fill(ellipse(16, 28, 13, 3.2), (x, y) -> c(Math.abs(Math.sin(x * 1.3 + y * 2)) < 0.25 ? Fire.FLAME : 0x3A1206));
		k.fill((x, y) -> {
			double half = 6.5 + 1.2 * Math.sin(y * 0.9) + (y > 24 ? (y - 24) * 0.6 : 0);
			return y > 1 && y < 29 && Math.abs(x - 16) < half;
		}, (x, y) -> {
			double half = 6.5 + 1.2 * Math.sin(y * 0.9) + (y > 24 ? (y - 24) * 0.6 : 0), f = Math.abs(x - 16) / half;
			return c(SpellArt.ramp(1 - f, Fire.CRIMSON, Fire.VERMILION, Fire.FLAME, Fire.GOLD_LIGHT, Fire.WHITE_FIRE));
		});
		k.fill((x, y) -> y < 6 && Math.abs(x - 16 - 3 * Math.sin(y)) < 5 - y * 0.4, c(Fire.FLAME));
		return k.outline();
	}

	/** Qilin Tramples the Land: a golden qilin's head in profile, its amber horn swept back, its mane of earth-fire behind it. */
	static Canvas qilin() {
		Canvas k = new Canvas();
		k.fill((x, y) -> x > 18 && x < 31 && y > 4 && y < 26 && Math.abs(Math.sin(y * 0.9 + x * 0.3)) < 0.75 && x < 31 - Math.abs(y - 15) * 0.4,
				(x, y) -> c(SpellArt.mix(Earth.MOLTEN, Earth.GOLD_PALE, (x - 18) / 13)));
		k.fill(disc(19, 13.5, 6.6), shaded(Earth.GOLD_SCALE, 12, 7, 26, 20));
		k.fill(ellipse(10, 15.2, 7, 3.8), shaded(Earth.OCHRE, 3, 11, 17, 19)).fill(ellipse(12, 19.2, 5.4, 1.9), c(Earth.CREAM));
		k.fill(line(6, 13.2, 14, 11.6, 0.6), c(Earth.GOLD_PALE));
		k.fill(line(19, 7.5, 21.5, 2.5, 1.2), c(Earth.EMBER)).fill(line(21.5, 2.5, 26, 1.2, 0.9), c(Earth.MOLTEN)).fill(disc(26, 1.2, 0.8), c(Earth.MOLTEN_CORE));
		k.fill(disc(16, 12.5, 1.3), c(0xFFF2A0)).fill(rect(15.6, 11.5, 16.4, 13.5), c(0x120804));
		k.fill(disc(3.5, 13.5, 0.9), c(0x1A1008));
		k.fill((x, y) -> y > 21 && y < 29 && x > 7 && x < 15 && Math.abs(Math.sin(x * 1.4)) > 0.2 && y < 29 - Math.abs(x - 11) * 0.8, (x, y) -> c(SpellArt.mix(Earth.MOLTEN, Earth.GOLD_PALE, (y - 21) / 8)));
		return k.outline();
	}

	/** Seal of Ten Thousand Mountains: a seal of yellow jade, gold-banded, its face red with paste, mountains rising from it. */
	static Canvas mountainSeal() {
		Canvas k = new Canvas();
		k.fill(tri(16, 2, 9, 18, 23, 18), shaded(Earth.STONE_LIGHT, 9, 2, 23, 18)).fill(tri(8, 7, 3, 18, 13, 18), shaded(Earth.STONE, 3, 7, 13, 18))
				.fill(tri(24, 6, 19, 18, 29, 18), shaded(Earth.STONE, 19, 6, 29, 18));
		k.fill(tri(16, 2, 14.5, 5, 17.5, 5), c(Earth.MOSS)).fill(tri(8, 7, 7, 9, 9, 9), c(Earth.MOSS)).fill(tri(24, 6, 23, 8, 25, 8), c(Earth.MOSS));
		k.fill((x, y) -> Math.abs(y - 13 - Math.sin(x * 0.6) * 0.8) < 0.8 && x > 6 && x < 26, c(Earth.MIST));
		k.fill(rect(4, 18, 28, 28), shaded(Earth.JADE_Y, 4, 18, 28, 28));
		k.fill(rect(4, 18, 28, 18.9), c(Earth.MOLTEN)).fill(rect(4, 26.2, 28, 27), c(Earth.MOLTEN)).fill(rect(4, 27, 28, 29), c(Earth.PASTE));
		k.fill((x, y) -> y > 20 && y < 25 && x > 6 && x < 26 && Math.abs(Math.sin(x * 1.1) * 1.5 + 22.5 - y) < 0.5, c(Earth.JADE_Y_DARK));
		return k.outline();
	}

	/** Black Tortoise Bastion: the dark shell, its scutes lit cyan, the tortoise's head out of it, its serpent rearing over it. */
	static Canvas blackTortoise() {
		Canvas k = new Canvas();
		k.fill(ellipse(16, 22, 12, 9), (x, y) -> y > 22.5 ? 0 : c(SpellArt.mix(Water.NAVY, Water.BLACK_SHELL, (y - 13) / 10)));
		k.fill((x, y) -> {
			if ((x - 16) * (x - 16) / 144 + (y - 22) * (y - 22) / 81 > 1 || y > 22.5) return false;
			double hx = (x - 16) / 4.6, hy = (y - 19) / 4;
			return Math.abs(hx - Math.rint(hx)) < 0.12 || Math.abs(hy - Math.rint(hy)) < 0.12 && Math.abs(Math.sin(hx * Math.PI)) > 0.1;
		}, c(Water.CYAN));
		k.fill(rect(4, 22, 28, 23.5), c(Water.CYAN));
		k.fill(ellipse(3.5, 21, 3, 2.4), c(Water.NAVY)).fill(disc(2.5, 20.4, 0.7), c(Water.ICE));
		k.fill(ellipse(8.5, 25, 2.4, 2.6), c(Water.NAVY)).fill(ellipse(23.5, 25, 2.4, 2.6), c(Water.NAVY));
		k.fill(rect(6, 26.8, 11, 27.6), c(Water.CYAN)).fill(rect(21, 26.8, 26, 27.6), c(Water.CYAN));
		for (int i = 0; i <= 40; i++) {
			double t = i / 40.0, x = 26 - 10 * t + 3 * Math.sin(t * Math.PI * 2), y = 21 - 17 * t;
			k.fill(disc(x, y, 1.6), (px, py) -> c(SpellArt.mix(Water.SERPENT, Water.SERPENT_LIGHT, (px - x + 1.6) / 3.2)));
		}
		k.fill(ellipse(15, 3.6, 2.8, 1.8), c(Water.SERPENT)).fill(disc(14.2, 3, 0.6), c(Water.ICE)).fill(line(12.5, 4.4, 10.5, 5.4, 0.35), c(Water.CYAN));
		return k.outline();
	}

	/** Hundred Rivers Converge: a whirlpool of blue water, foam along its arms, dark at its eye. */
	static Canvas hundredRivers() {
		Canvas k = new Canvas();
		k.fill(disc(16, 16, 14.5), (x, y) -> {
			double r = Math.hypot(x - 16, y - 16), ang = Math.atan2(y - 16, x - 16) / (2 * Math.PI);
			double s = ang * 4 - Math.log(Math.max(0.5, r)) * 1.6, f = s - Math.floor(s);
			if (r < 2.5) return c(Water.ABYSS);
			if (f < 0.16) return c(SpellArt.mix(Water.ICE, Water.FOAM, r / 14));
			return c(SpellArt.ramp(f * 0.7 + r / 28, Water.NAVY, Water.DEEP, Water.TEAL, Water.CYAN));
		});
		return k.outline();
	}

	/** White Tiger's Pounce: a white tiger's head, striped in dark steel, its eyes ice-blue, the mark of king on its brow, claw-marks behind it. */
	static Canvas whiteTiger() {
		Canvas k = new Canvas();
		for (int i = 0; i < 3; i++) k.fill(line(22 + i * 3, 2, 30 + i * 0.5, 12 + i * 2, 0.7), c(Metal.GLEAM));
		k.fill(disc(8, 6.5, 3), c(Metal.FUR)).fill(disc(22, 6.5, 3), c(Metal.FUR)).fill(disc(8, 6.5, 1.5), c(Metal.STRIPE)).fill(disc(22, 6.5, 1.5), c(Metal.STRIPE));
		k.fill(ellipse(15, 16, 11, 10), (x, y) -> {
			double side = Math.abs(x - 15);
			if (side > 4 && Math.abs(Math.sin((y + side * 0.6) * 0.9)) < 0.3) return c(Metal.STRIPE);
			return c(SpellArt.mix(Metal.FUR, Metal.FUR_SHADE, (y - 6) / 20 * 0.6));
		});
		k.fill(ellipse(15, 21, 5, 3.6), c(0xFFFFFF)).fill(tri(13, 18, 17, 18, 15, 20.5), c(Metal.NOSE)).fill(line(15, 20.5, 15, 22.5, 0.4), c(Metal.STRIPE));
		k.fill(ellipse(10.5, 14, 2, 1.2), c(Metal.ICE_EYE)).fill(ellipse(19.5, 14, 2, 1.2), c(Metal.ICE_EYE)).fill(disc(10.5, 14, 0.6), c(0x05080C)).fill(disc(19.5, 14, 0.6), c(0x05080C));
		k.fill((x, y) -> Math.abs(x - 15) < 2.2 && (Math.abs(y - 8) < 0.45 || Math.abs(y - 9.6) < 0.45 || Math.abs(y - 11.2) < 0.45) || Math.abs(x - 15) < 0.45 && y > 8 && y < 11.2, c(Metal.GOLD));
		return k.outline();
	}

	/** Myriad Swords Return to One: a great sword point down, a ring of small swords of light about it. */
	static Canvas myriadSwords() {
		Canvas k = new Canvas();
		for (int i = 0; i < 10; i++) {
			double a = Math.PI * 2 * i / 10, x = 16 + Math.cos(a) * 12, y = 15 + Math.sin(a) * 12, tx = Math.cos(a + 1.2) * 3.2, ty = Math.sin(a + 1.2) * 3.2;
			k.fill(line(x - tx, y - ty, x + tx, y + ty, 0.6), c(Metal.GLEAM)).fill(disc(x - tx, y - ty, 0.8), c(Metal.GOLD));
		}
		k.fill(tri(13, 9, 19, 9, 16, 30), (x, y) -> c(x < 16 ? Metal.SILVER : Metal.STEEL)).fill(line(16, 9, 16, 27, 0.4), c(Metal.GLEAM));
		k.fill(rect(9.5, 7, 22.5, 9), c(Metal.GOLD)).fill(rect(14.8, 1.5, 17.2, 7), c(0x2A3442)).fill(disc(16, 1.6, 1.4), c(Metal.GOLD));
		return k.outline();
	}

	// --- The ghost foundation's (Ghost) and both paths' (HeavenDemon) ---

	/** Procession of Ten Thousand Ghosts: hooded dead in a column coming on, the nearest great, their eyes ghost fire, a lantern held up before them. */
	static Canvas procession() {
		Canvas k = new Canvas();
		double[][] dead = {{24, 9, 3.2}, {18, 12, 4}, {11, 17, 5.2}};
		for (double[] d : dead) {
			double x = d[0], y = d[1], r = d[2];
			k.fill(tri(x - r * 1.1, y + r * 0.6, x + r * 1.1, y + r * 0.6, x + r * 0.2, y + r * 3.4), (px, py) -> c(SpellArt.mix(Ghost.MIST, Ghost.MIST_SHADE, (py - y) / (r * 3))));
			k.fill(disc(x, y, r), shaded(Ghost.MIST, x - r, y - r, x + r, y + r));
			k.fill(ellipse(x - r * 0.15, y + r * 0.15, r * 0.62, r * 0.7), c(Ghost.VOID));
			k.fill(disc(x - r * 0.42, y, r * 0.2), c(Ghost.FIRE_PALE)).fill(disc(x + r * 0.15, y, r * 0.2), c(Ghost.FIRE_PALE));
		}
		k.fill(line(5, 27, 4, 14, 0.5), c(Ghost.BONE_SHADE)).fill(rect(2, 15, 6, 20), c(Ghost.FIRE_CYAN)).fill(rect(2.5, 16, 5.5, 19), c(Ghost.FIRE_WHITE));
		k.fill(disc(27, 22, 1.4), c(Ghost.FIRE_CYAN)).fill(disc(29, 15, 1), c(Ghost.FIRE_CYAN));
		return k.outline();
	}

	/** Gate of Ten Thousand Ghosts: a gate of bone under a dark roof, the way beyond it a whirl of ghost fire. */
	static Canvas ghostGate() {
		Canvas k = new Canvas();
		k.fill(rect(9, 11, 23, 29), (x, y) -> {
			double r = Math.hypot(x - 16, y - 20), ang = Math.atan2(y - 20, x - 16) / (2 * Math.PI), s = ang * 3 + Math.log(Math.max(0.5, r)) * 1.4, f = s - Math.floor(s);
			return c(r < 1.5 ? Ghost.VOID : f < 0.35 ? SpellArt.mix(Ghost.FIRE_PALE, Ghost.FIRE_TEAL, r / 9) : Ghost.VOID);
		});
		k.fill(rect(5, 9, 9, 29), shaded(Ghost.BONE, 5, 9, 9, 29)).fill(rect(23, 9, 27, 29), shaded(Ghost.BONE, 23, 9, 27, 29));
		k.fill(rect(4, 9, 28, 11.5), c(Ghost.BONE_SHADE));
		k.fill(rect(12.5, 8.8, 19.5, 11.8), c(Ghost.LACQUER)).fill(rect(14, 9.6, 18, 11), c(Ghost.FIRE_CYAN));
		k.fill(tri(1, 8, 16, 2, 31, 8), shaded(Ghost.SLATE_LIGHT, 1, 2, 31, 8)).fill(rect(1, 7, 31, 8.6), c(Ghost.SLATE));
		k.fill(line(1.5, 7.5, 0.5, 5, 0.6), c(Ghost.SLATE_LIGHT)).fill(line(30.5, 7.5, 31.5, 5, 0.6), c(Ghost.SLATE_LIGHT));
		k.fill(rect(6.2, 13, 7.8, 15), c(Ghost.FIRE_CYAN)).fill(rect(24.2, 13, 25.8, 15), c(Ghost.FIRE_CYAN));
		k.fill(rect(6.2, 18, 7.8, 20), c(Ghost.FIRE_CYAN)).fill(rect(24.2, 18, 25.8, 20), c(Ghost.FIRE_CYAN));
		return k.outline();
	}

	/** Heaven-Demon Palm: an open palm of gold, a sun in its hollow, three crimson claw-marks raked across it. */
	static Canvas heavenDemonPalm() {
		Canvas k = new Canvas();
		k.fill(rect(9, 14, 23, 27), shaded(HeavenDemon.GOLD_BRIGHT, 9, 14, 23, 27));
		for (int i = 0; i < 4; i++) {
			double x = 10.6 + i * 3.6, top = 4 + Math.abs(i - 1.5) * 1.6;
			k.fill(rect(x - 1.4, top, x + 1.4, 15), shaded(HeavenDemon.GOLD_BRIGHT, x - 1.4, top, x + 1.4, 15)).fill(disc(x, top, 1.4), c(HeavenDemon.GOLD_PALE));
		}
		k.fill(line(22.5, 22, 28, 16, 1.6), c(HeavenDemon.GOLD));
		k.fill(disc(16, 20.5, 3.2), c(HeavenDemon.WHITE_LIGHT));
		for (int i = 0; i < 3; i++) k.fill(line(6 + i * 4.5, 8, 14 + i * 4.5, 30, 0.85), c(HeavenDemon.SCARLET));
		return k.outline();
	}

	/** Heaven-Demon Mantle: a round mantle, gold light above and crimson scales below, a white seam between, an arrow turned back off it. */
	static Canvas heavenDemonMantle() {
		Canvas k = new Canvas();
		k.fill(disc(15, 17, 12.5), (x, y) -> {
			if (Math.abs(x - 15 - (y - 17) * 0.15) < 0.8) return c(HeavenDemon.WHITE_LIGHT);
			if (x < 15 + (y - 17) * 0.15) return c(SpellArt.mix(HeavenDemon.GOLD_PALE, HeavenDemon.GOLD, Math.hypot(x - 15, y - 17) / 12.5));
			double sx = (x - 15) / 3.4, sy = (y - 17) / 2.6 + (Math.floor(sx) % 2 == 0 ? 0.5 : 0), fy = sy - Math.floor(sy);
			return c(fy < 0.2 ? HeavenDemon.BLOOD_DARK : SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.CRIMSON, fy));
		});
		k.fill(line(21, 9, 30, 2, 0.6), c(0xF2F2F2)).fill(tri(30.5, 1, 26.5, 2, 29.5, 5), c(0xF2F2F2));
		for (int i = 0; i < 5; i++) {
			double a = Math.PI * (0.65 + i * 0.17);
			k.fill(line(15 + Math.cos(a) * 13.5, 17 - Math.sin(a) * 13.5, 15 + Math.cos(a) * 16, 17 - Math.sin(a) * 16, 0.5), c(HeavenDemon.GOLD_PALE));
		}
		return k.outline();
	}

	/** Devouring Seal: heaven's ring of gold about it, and in it a demon's crimson maw closing on a soul of light. */
	static Canvas devouringSeal() {
		Canvas k = new Canvas();
		k.fill((x, y) -> {
			double r = Math.hypot(x - 16, y - 16);
			return r > 12.3 && r < 14.6;
		}, c(HeavenDemon.GOLD_BRIGHT));
		for (int i = 0; i < 4; i++) {
			double a = Math.PI / 2 * i + Math.PI / 4;
			k.fill(disc(16 + Math.cos(a) * 13.4, 16 + Math.sin(a) * 13.4, 2.1), c(HeavenDemon.GOLD_PALE));
		}
		k.fill(ellipse(16, 11.5, 9.5, 5.5), (x, y) -> y < 12.5 ? c(SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.CRIMSON, (y - 6) / 6)) : 0);
		k.fill(ellipse(16, 20.5, 9.5, 5.5), (x, y) -> y > 19.5 ? c(SpellArt.mix(HeavenDemon.CRIMSON, HeavenDemon.BLOOD, (y - 19.5) / 6)) : 0);
		for (int i = 0; i < 4; i++) {
			double x = 10 + i * 4;
			k.fill(tri(x - 1.2, 12.4, x + 1.2, 12.4, x, 15.4), c(0xFFF4EC)).fill(tri(x + 0.8, 19.6, x + 3.2, 19.6, x + 2, 16.8), c(0xFFF4EC));
		}
		k.fill(disc(16, 16, 2.4), c(HeavenDemon.WHITE_LIGHT)).fill(disc(16, 16, 1.2), c(HeavenDemon.GOLD_PALE));
		return k.outline();
	}

	/** Heaven-Demon Claw: four great rakes slanting down across it, crimson, edged with gold. */
	static Canvas heavenDemonClaw() {
		Canvas k = new Canvas();
		for (int i = 0; i < 4; i++) {
			double x0 = 13 + i * 5, y0 = 2 + i * 0.6, x1 = 3 + i * 5, y1 = 29 - i * 0.6;
			k.fill((x, y) -> {
				double f = SpellArt.clamp01(((x - x0) * (x1 - x0) + (y - y0) * (y1 - y0)) / ((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)));
				return Fire.segment(x, y, x0, y0, x1, y1) < 0.4 + 1.5 * Math.sin(Math.PI * f);
			}, (x, y) -> c(SpellArt.mix(HeavenDemon.EMBER, HeavenDemon.CRIMSON, Fire.segment(x, y, x0, y0, x1, y1) / 1.9)));
			k.fill(line(x0 + 1.4, y0 + 0.4, x1 + 1.4, y1 + 0.4, 0.35), c(HeavenDemon.GOLD_BRIGHT));
		}
		return k.outline();
	}

	/** Heaven and Demon as One: two arms, gold and crimson, wound into one heart of white light. */
	static Canvas heavenAndDemonAsOne() {
		Canvas k = new Canvas();
		k.fill(disc(16, 16, 14.5), (x, y) -> {
			double r = Math.hypot(x - 16, y - 16), ang = Math.atan2(y - 16, x - 16) / (2 * Math.PI);
			double s = ang * 2 - Math.log(Math.max(0.5, r)) * 1.3, f = s - Math.floor(s);
			if (r < 3) return c(SpellArt.mix(HeavenDemon.WHITE_LIGHT, HeavenDemon.GOLD_PALE, r / 3));
			if (Math.abs(f - 0.25) < 0.16) return c(SpellArt.mix(HeavenDemon.GOLD_PALE, HeavenDemon.GOLD, r / 14));
			if (Math.abs(f - 0.75) < 0.16) return c(SpellArt.mix(HeavenDemon.EMBER, HeavenDemon.CRIMSON, r / 14));
			return 0;
		});
		return k.outline();
	}

	/** Heaven-Demon Avatar: the colossus's head and shoulders, gold to the left, crimson and horned to the right, a third eye, its halo behind. */
	static Canvas heavenDemonAvatar() {
		Canvas k = new Canvas();
		k.fill((x, y) -> {
			double r = Math.hypot(x - 16, y - 12);
			return r > 10.5 && r < 12.3;
		}, (x, y) -> c(x < 16 ? HeavenDemon.GOLD_PALE : HeavenDemon.EMBER));
		k.fill(rect(4, 23, 28, 31), (x, y) -> c(x < 16 ? SpellArt.shade(HeavenDemon.GOLD, 0.1) : HeavenDemon.CRIMSON));
		k.fill(rect(10, 4, 22, 22), (x, y) -> c(x < 16 ? SpellArt.mix(HeavenDemon.GOLD_BRIGHT, HeavenDemon.GOLD, (y - 4) / 18) : SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.BLOOD, (y - 4) / 18)));
		k.fill(rect(15.5, 4, 16.5, 31), c(HeavenDemon.WHITE_LIGHT));
		k.fill(rect(11.5, 11.5, 14.5, 12.6), c(HeavenDemon.WHITE_LIGHT)).fill(tri(17.5, 11, 21, 11.4, 17.5, 13), c(HeavenDemon.GOLD_BRIGHT));
		k.fill(ellipse(16, 8.4, 0.9, 1.8), c(HeavenDemon.WHITE_LIGHT));
		k.fill(rect(12, 17.5, 15.5, 18.3), c(HeavenDemon.GOLD_PALE)).fill(tri(17, 17, 20.5, 17.5, 18.5, 20), c(HeavenDemon.BLOOD_DARK));
		k.fill(line(21, 5, 25, 0.5, 1.1), c(HeavenDemon.HORN)).fill(disc(25, 0.8, 0.8), c(HeavenDemon.EMBER));
		return k.outline();
	}

	// --- Void Shattering's (Void) ---

	/** A tear in space between (x0, y0) and (x1, y1), {@code wide} across: black, its rim of {@code rim}'s light; ragged. */
	static void tear(Canvas k, double x0, double y0, double x1, double y1, double wide, int rim) {
		double dx = x1 - x0, dy = y1 - y0, len = Math.hypot(dx, dy), ux = dx / len, uy = dy / len;
		Test inside = (x, y) -> {
			double along = ((x - x0) * ux + (y - y0) * uy) / len, off = (x - x0) * -uy + (y - y0) * ux;
			if (along < 0 || along > 1) return false;
			double half = wide / 2 * Math.pow(Math.sin(Math.PI * along), 0.7) * (1 + 0.3 * Math.sin(along * 23));
			return Math.abs(off) < half;
		};
		k.fill((x, y) -> !inside.in(x, y) && (inside.in(x + 0.9, y) || inside.in(x - 0.9, y) || inside.in(x, y + 0.9) || inside.in(x, y - 0.9)), c(rim));
		k.fill(inside, (x, y) -> c(Math.abs(Math.sin(x * 3.1 + y * 1.7)) > 0.985 ? 0xFFFFFF : 0x07040C));
	}

	/** Spring Thunder of the Azure Emperor: a dark storm cloud, the thunder trigram burning on it, two bolts of jade lightning striking down from it. */
	static Canvas springThunder() {
		Canvas k = new Canvas();
		for (double[] b : new double[][] {{9, 13}, {21, 12}}) {
			double x = b[0], y = b[1];
			k.fill(line(x, y, x - 3, y + 6, 0.8), c(Void.JADE_PALE)).fill(line(x - 3, y + 6, x + 1, y + 9, 0.8), c(Void.JADE_PALE)).fill(line(x + 1, y + 9, x - 2, y + 17, 0.8), c(Void.JADE_PALE));
		}
		k.fill(ellipse(16, 8, 14, 6), shaded(Void.STORM_LIGHT, 2, 2, 30, 14)).fill(disc(9, 6, 5), shaded(Void.STORM_PALE, 4, 1, 14, 11)).fill(disc(22, 5.5, 5.5), shaded(Void.STORM_PALE, 16, 0, 28, 11));
		k.fill(ellipse(16, 11, 13, 2.6), c(Void.STORM_DARK));
		k.fill(rect(11, 5, 21, 6.2), c(Void.JADE_WHITE));
		k.fill(rect(11, 7.6, 15, 8.8), c(Void.JADE)).fill(rect(17, 7.6, 21, 8.8), c(Void.JADE));
		k.fill(rect(11, 10.2, 15, 11.4), c(Void.JADE)).fill(rect(17, 10.2, 21, 11.4), c(Void.JADE));
		return k.outline();
	}

	/** Void Withering: a tear of the void, black roots hanging out of it, motes of life drawn up into it. */
	static Canvas voidWithering() {
		Canvas k = new Canvas();
		for (int i = 0; i < 5; i++) {
			double x0 = 10 + i * 3, x1 = 3 + i * 6.5;
			k.fill((x, y) -> {
				double t = (y - 8) / 22, xc = x0 + (x1 - x0) * Math.sin(t * Math.PI / 2) + Math.sin(t * 6 + x0) * 0.8;
				return y > 8 && y < 30 && Math.abs(x - xc) < 1.4 - t;
			}, (x, y) -> c(Math.abs(x - (x0 + (x1 - x0) * Math.sin((y - 8) / 22 * Math.PI / 2))) < 0.3 ? Void.LIFE : Void.VOID_BARK));
		}
		for (double[] m : new double[][] {{6, 22}, {24, 24}, {14, 26}, {27, 16}}) k.fill(disc(m[0], m[1], 0.9), c(Void.LIFE_WHITE));
		tear(k, 4, 5, 28, 7, 7, Void.VOID_SHEEN);
		return k.outline();
	}

	/** Golden Crow Descends: the red sun, and in it the Golden Crow, its wings spread, its three legs beneath it. */
	static Canvas goldenCrow() {
		Canvas k = new Canvas();
		k.fill(disc(16, 16, 14), (x, y) -> c(SpellArt.mix(Void.SUN_ORANGE, 0xB01C08, Math.hypot(x - 16, y - 16) / 14)));
		k.fill((x, y) -> {
			double r = Math.hypot(x - 16, y - 16);
			return r > 13 && r < 14.5;
		}, c(Void.SUN_PALE));
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			k.fill(tri(16, 14, 16 + s * 12, 8, 16 + s * 8, 16), (x, y) -> c(SpellArt.mix(Void.SUN_PALE, Void.SUN_GOLD, Math.abs(x - 16) / 12)));
			for (int f = 0; f < 3; f++) k.fill(line(16 + s * (9 + f * 1.2), 9 + f * 1.6, 16 + s * (14 - f * 0.5), 6 + f * 2.6, 0.55), c(Void.SUN_GOLD));
		}
		k.fill(ellipse(16, 15.5, 3, 5), shaded(Void.SUN_GOLD, 13, 10, 19, 21)).fill(disc(16, 10, 2.3), c(Void.SUN_PALE)).fill(tri(15, 8.6, 17, 8.6, 16, 5.4), c(Void.SUN_WHITE));
		k.fill(disc(15.2, 9.6, 0.5), c(0x3A0A00));
		for (double x : new double[] {14, 16, 18}) k.fill(line(x, 20, x + (x - 16) * 0.3, 24.5, 0.45), c(Void.SUN_PALE));
		k.fill(tri(14, 20, 18, 20, 16, 26), c(Void.SUN_GOLD));
		return k.outline();
	}

	/** Sun Pulled Through the Void: a tear in space, and a blazing sun half through it, its flames licking out. */
	static Canvas sunThroughTheVoid() {
		Canvas k = new Canvas();
		tear(k, 16, 1, 16, 31, 13, 0xFFC860);
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12;
			k.fill(line(16 + Math.cos(a) * 8, 17 + Math.sin(a) * 8, 16 + Math.cos(a) * 12, 17 + Math.sin(a) * 12, 0.9), c(Void.SUN_ORANGE));
		}
		k.fill(disc(16, 17, 8.4), (x, y) -> c(SpellArt.ramp(1 - Math.hypot(x - 16, y - 17) / 8.4, Void.SUN_DEEP, Void.SUN_ORANGE, Void.SUN_GOLD, Void.SUN_PALE, Void.SUN_WHITE)));
		return k.outline();
	}

	/** Buzhou Falls: the broken pillar of heaven falling out of a crack in the sky, bronze-banded, its top broken. */
	static Canvas buzhouFalls() {
		Canvas k = new Canvas();
		tear(k, 3, 4, 29, 3, 6, 0xFFB050);
		k.fill(rect(11, 6, 21, 27), shaded(Void.PILLAR_LIGHT, 11, 6, 21, 27));
		for (double y : new double[] {10, 17, 24}) k.fill(rect(10, y, 22, y + 1.6), c(Void.VERDIGRIS));
		k.fill(tri(11, 27, 21, 27, 14, 31), c(Void.PILLAR)).fill(tri(15, 27, 21, 27, 19, 30), c(Void.PILLAR_LIGHT));
		k.fill(line(14, 12, 16, 22, 0.35), c(Earth.MOLTEN)).fill(line(18, 19, 17, 26, 0.35), c(Earth.MOLTEN));
		k.fill(rect(9, 27.5, 23, 29), c(0xFFD890));
		return k.outline();
	}

	/** Earth Veins Lock the Void: a dome of glowing veins over the land, the earth's trigram at its crown. */
	static Canvas earthVeinsLock() {
		Canvas k = new Canvas();
		k.fill(ellipse(16, 27, 15, 3), (x, y) -> c(Math.abs(Math.hypot((x - 16) / 15, (y - 27) / 3) - 0.85) < 0.12 ? Earth.MOLTEN : Earth.UMBER));
		for (int i = 0; i < 5; i++) {
			double fx = -1 + i * 0.5;
			k.fill((x, y) -> {
				double u = (x - 16) / 14, v = (27 - y) / 21;
				if (v < 0 || v > 1) return false;
				double archX = fx * Math.sqrt(Math.max(0, 1 - v * v));
				return Math.abs(u - archX) < 0.06;
			}, c(Earth.MOLTEN));
		}
		k.fill((x, y) -> Math.abs(Math.hypot((x - 16) / 14, (27 - y) / 21) - 1) < 0.05 && y < 27, c(Earth.MOLTEN_CORE));
		k.fill((x, y) -> Math.abs((27 - y) / 21 - 0.5) < 0.04 && Math.abs((x - 16) / 14) < 0.86, c(Earth.MOLTEN));
		k.fill(rect(12, 3, 20, 9), c(Earth.JADE_Y_DARK));
		for (int i = 0; i < 3; i++) k.fill(rect(13, 4 + i * 1.8, 15.4, 4.9 + i * 1.8), c(Earth.MOLTEN_CORE)).fill(rect(16.6, 4 + i * 1.8, 19, 4.9 + i * 1.8), c(Earth.MOLTEN_CORE));
		return k.outline();
	}

	/** Weak Water from the Void: pale water pouring out of a tear in the sky into a pool, a feather sinking in it. */
	static Canvas weakWater() {
		Canvas k = new Canvas();
		k.fill(ellipse(16, 27, 14, 3.5), (x, y) -> c(Math.abs(Math.sin(Math.hypot((x - 16) / 14, (y - 27) / 3.5) * 9)) < 0.3 ? Void.WEAK_WHITE : Void.WEAK));
		k.fill(rect(11, 6, 21, 27), (x, y) -> c(Math.abs(Math.sin(x * 2.2 + y * 0.4)) < 0.25 ? Void.WEAK_WHITE : SpellArt.mix(Void.WEAK_PALE, Void.WEAK, (x - 11) / 10)));
		k.fill(ellipse(15, 17, 1.3, 3.6), c(0xFFFFFF)).fill(line(15, 13.4, 15, 21, 0.3), c(Void.WEAK));
		tear(k, 3, 5, 29, 4, 6, 0xB0DCFF);
		return k.outline();
	}

	/** Kun Rises from the Northern Abyss: the great dark fish arching up out of the black abyss, light along its flank. */
	static Canvas kunRises() {
		Canvas k = new Canvas();
		k.fill(ellipse(16, 27.5, 15, 3.5), (x, y) -> c(Math.abs(Math.hypot((x - 16) / 15, (y - 27.5) / 3.5) - 0.88) < 0.1 ? 0x5C8CFF : 0x050810));
		// Its body arching out of the abyss, its blunt head up and over to the left, tapering to its tail at the right.
		for (int i = 0; i <= 60; i++) {
			double t = i / 60.0, a = Math.PI * (0.95 - 0.8 * t), x = 16 + Math.cos(a) * 11, y = 26 - Math.sin(a) * 19, r = 1.2 + 3.8 * Math.pow(1 - t, 0.7) * Math.min(1, t * 6 + 0.35);
			k.fill(disc(x, y, r), (px, py) -> c(SpellArt.mix(Void.KUN_FLANK, Void.KUN_BACK, (py - y + r) / (2 * r))));
		}
		for (int i = 4; i <= 46; i++) {
			double t = i / 50.0, a = Math.PI * (0.95 - 0.8 * t);
			k.fill(disc(16 + Math.cos(a) * 11 - Math.cos(a) * 1.2, 26 - Math.sin(a) * 19 + Math.sin(a) * 1.2, 0.45), c(Void.KUN_GLOW));
		}
		double tx = 16 + Math.cos(Math.PI * 0.15) * 11, ty = 26 - Math.sin(Math.PI * 0.15) * 19;
		k.fill(tri(tx, ty, tx + 5, ty - 5, tx + 2, ty), c(Void.KUN_FLANK)).fill(tri(tx, ty, tx + 5.5, ty + 3, tx + 2, ty), c(Void.KUN_FLANK));
		k.fill(disc(5.4, 19.2, 1), c(Void.KUN_WHITE)).fill(disc(5.4, 19.2, 0.4), c(Void.KUN_BACK));
		return k.outline();
	}

	/** Void-Severing Sword Light: a sword of light slashing down across it, space cut open behind it in a black line. */
	static Canvas voidSevering() {
		Canvas k = new Canvas();
		tear(k, 2, 29, 30, 3, 5, Void.LIGHT_VIOLET);
		k.fill(line(6, 26, 25, 7, 1.4), (x, y) -> c(SpellArt.mix(Void.LIGHT_WHITE, Void.LIGHT_BLUE, Fire.segment(x, y, 6, 26, 25, 7) / 1.4)));
		k.fill(line(25, 7, 29, 3, 0.6), c(Void.LIGHT_WHITE)).fill(line(4.5, 24.5, 7.5, 27.5, 0.7), c(Void.LIGHT_PALE)).fill(line(6, 26, 3, 29, 0.6), c(Void.LIGHT_PALE));
		return k.outline();
	}

	/** Ten-Direction Sword Prison: a ring of great swords standing point down, and the one above coming down into their midst. */
	static Canvas swordPrison() {
		Canvas k = new Canvas();
		k.fill(ellipse(16, 25, 14, 4), (x, y) -> Math.abs(Math.hypot((x - 16) / 14, (y - 25) / 4) - 0.9) < 0.12 ? c(Metal.GLEAM) : 0);
		double[] xs = {4, 9, 16, 23, 28};
		double[] ys = {23, 26, 27, 26, 23};
		for (int i = 0; i < xs.length; i++) {
			double x = xs[i], y = ys[i];
			k.fill(tri(x - 1.1, y - 11, x + 1.1, y - 11, x, y), (px, py) -> c(px < x ? Metal.SILVER : Metal.STEEL)).fill(rect(x - 2, y - 12.2, x + 2, y - 11), c(Metal.GOLD)).fill(rect(x - 0.5, y - 15, x + 0.5, y - 12.2), c(0x2A3442));
		}
		k.fill(tri(14.4, 2, 17.6, 2, 16, 14), (x, y) -> c(x < 16 ? Metal.WHITE : Metal.SILVER)).fill(rect(13, 0.5, 19, 2), c(Metal.GOLD));
		return k.outline();
	}

	/** Rift to the Nine Nethers: rings of ghost fire whirling down into a black rift in the land. */
	static Canvas nineNethers() {
		Canvas k = new Canvas();
		for (int i = 0; i < 6; i++) {
			double rx = 14 - i * 2.1, ry = 6 - i * 0.85, cy = 13 + i * 2.2;
			k.fill((x, y) -> Math.abs(Math.hypot((x - 16) / rx, (y - cy) / ry) - 1) < 0.13, c(SpellArt.mix(Ghost.FIRE_PALE, Ghost.FIRE_TEAL, i / 5.0)));
		}
		k.fill(ellipse(16, 25.5, 3.6, 1.6), c(Ghost.VOID));
		for (double[] f : new double[][] {{4, 10}, {28, 10}, {9, 6}, {23, 6}}) k.fill(tri(f[0] - 1, f[1] + 2, f[0] + 1, f[1] + 2, f[0], f[1] - 1.5), c(Ghost.FIRE_CYAN));
		return k.outline();
	}

	/** Bridge of No Return: the arched bridge of bone-white stone over the dark river, lanterns of ghost fire on it, red lilies by its foot. */
	static Canvas bridgeOfNoReturn() {
		Canvas k = new Canvas();
		k.fill(rect(0, 24, 32, 30), (x, y) -> c(Math.abs(Math.sin(x * 0.8 + y)) < 0.15 ? Ghost.FIRE_TEAL : Ghost.LACQUER));
		k.fill((x, y) -> {
			double f = (x - 16) / 15, top = 22 - 10 * (1 - f * f);
			return Math.abs(f) < 1 && y > top && y < top + 3;
		}, (x, y) -> c(Ghost.BONE));
		k.fill((x, y) -> {
			double f = (x - 16) / 15, top = 22 - 10 * (1 - f * f);
			return Math.abs(f) < 1 && Math.abs(y - (top - 2.4)) < 0.5;
		}, c(Ghost.BONE_SHADE));
		for (double x : new double[] {4, 10, 16, 22, 28}) {
			double f = (x - 16) / 15, top = 22 - 10 * (1 - f * f);
			k.fill(rect(x - 0.5, top - 3, x + 0.5, top), c(Ghost.BONE_SHADE));
		}
		for (double x : new double[] {4, 16, 28}) {
			double f = (x - 16) / 15, top = 22 - 10 * (1 - f * f);
			k.fill(rect(x - 1.3, top - 7, x + 1.3, top - 4), c(Ghost.FIRE_CYAN)).fill(rect(x - 0.6, top - 6.3, x + 0.6, top - 4.7), c(Ghost.FIRE_WHITE));
		}
		for (double x : new double[] {2, 5, 27, 30}) k.fill(line(x, 25, x, 22, 0.4), c(0xE0202E)).fill(disc(x, 22, 1.1), c(0xE0303A));
		return k.outline();
	}

	/** Heaven-Demon Void Shatter: a fist, gold and crimson, struck into space, cracks running out from where it struck. */
	static Canvas heavenDemonVoidShatter() {
		Canvas k = new Canvas();
		for (int i = 0; i < 9; i++) {
			double a = Math.PI * 2 * i / 9 + 0.3, len = 13 + (i % 3) * 2;
			k.fill(line(19 + Math.cos(a) * 3, 13 + Math.sin(a) * 3, 19 + Math.cos(a) * len, 13 + Math.sin(a) * len, 0.45), c(0xF4ECFF));
		}
		k.fill((x, y) -> Math.abs(Math.hypot(x - 19, y - 13) - 7.5) < 0.4, c(0xD8C8FF));
		k.fill(disc(19, 13, 3.2), c(0x07040C));
		k.fill(rect(4, 15, 13, 27), (x, y) -> c(x < 8.5 ? HeavenDemon.GOLD : HeavenDemon.CRIMSON));
		k.fill(rect(9, 8, 20, 20), (x, y) -> c(x < 14.5 ? SpellArt.mix(HeavenDemon.GOLD_PALE, HeavenDemon.GOLD_BRIGHT, (y - 8) / 12) : SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.CRIMSON, (y - 8) / 12)));
		for (int i = 0; i < 4; i++) k.fill(rect(17.6, 8.4 + i * 2.9, 21, 10.6 + i * 2.9), c(i < 2 ? HeavenDemon.GOLD_BRIGHT : HeavenDemon.SCARLET));
		k.fill(rect(14.2, 8, 14.8, 20), c(HeavenDemon.WHITE_LIGHT));
		return k.outline();
	}

	static final Object[][] ICONS = {{"azure_dragon", (java.util.function.Supplier<Canvas>) Icons::azureDragon}, {"myriad_woods_return", (java.util.function.Supplier<Canvas>) Icons::daoTree},
			{"vermilion_bird_descent", (java.util.function.Supplier<Canvas>) Icons::vermilionBird}, {"one_flame_burns_the_heavens", (java.util.function.Supplier<Canvas>) Icons::oneFlame},
			{"qilin_tramples_the_land", (java.util.function.Supplier<Canvas>) Icons::qilin}, {"seal_of_ten_thousand_mountains", (java.util.function.Supplier<Canvas>) Icons::mountainSeal},
			{"black_tortoise_bastion", (java.util.function.Supplier<Canvas>) Icons::blackTortoise}, {"hundred_rivers_converge", (java.util.function.Supplier<Canvas>) Icons::hundredRivers},
			{"white_tiger_pounce", (java.util.function.Supplier<Canvas>) Icons::whiteTiger}, {"myriad_swords_return", (java.util.function.Supplier<Canvas>) Icons::myriadSwords},
			{"procession_of_ten_thousand_ghosts", (java.util.function.Supplier<Canvas>) Icons::procession}, {"gate_of_ten_thousand_ghosts", (java.util.function.Supplier<Canvas>) Icons::ghostGate},
			{"heaven_demon_palm", (java.util.function.Supplier<Canvas>) Icons::heavenDemonPalm}, {"heaven_demon_mantle", (java.util.function.Supplier<Canvas>) Icons::heavenDemonMantle},
			{"devouring_seal", (java.util.function.Supplier<Canvas>) Icons::devouringSeal}, {"heaven_demon_claw", (java.util.function.Supplier<Canvas>) Icons::heavenDemonClaw},
			{"heaven_and_demon_as_one", (java.util.function.Supplier<Canvas>) Icons::heavenAndDemonAsOne}, {"heaven_demon_avatar", (java.util.function.Supplier<Canvas>) Icons::heavenDemonAvatar},
			{"spring_thunder_of_the_azure_emperor", (java.util.function.Supplier<Canvas>) Icons::springThunder}, {"void_withering", (java.util.function.Supplier<Canvas>) Icons::voidWithering},
			{"golden_crow_descends", (java.util.function.Supplier<Canvas>) Icons::goldenCrow}, {"sun_through_the_void", (java.util.function.Supplier<Canvas>) Icons::sunThroughTheVoid},
			{"buzhou_falls", (java.util.function.Supplier<Canvas>) Icons::buzhouFalls}, {"earth_veins_lock_the_void", (java.util.function.Supplier<Canvas>) Icons::earthVeinsLock},
			{"weak_water_from_the_void", (java.util.function.Supplier<Canvas>) Icons::weakWater}, {"kun_rises_from_the_abyss", (java.util.function.Supplier<Canvas>) Icons::kunRises},
			{"void_severing_sword_light", (java.util.function.Supplier<Canvas>) Icons::voidSevering}, {"ten_direction_sword_prison", (java.util.function.Supplier<Canvas>) Icons::swordPrison},
			{"rift_to_the_nine_nethers", (java.util.function.Supplier<Canvas>) Icons::nineNethers}, {"bridge_of_no_return", (java.util.function.Supplier<Canvas>) Icons::bridgeOfNoReturn},
			{"heaven_demon_void_shatter", (java.util.function.Supplier<Canvas>) Icons::heavenDemonVoidShatter}};

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		Path dir = Path.of(args[0]).resolve("textures/gui/spell");
		Files.createDirectories(dir);
		for (Object[] icon : ICONS) {
			ImageIO.write(((java.util.function.Supplier<Canvas>) icon[1]).get().image(), "png", dir.resolve(icon[0] + ".png").toFile());
			System.out.println(icon[0]);
		}
	}
}
