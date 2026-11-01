import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * The Body Path's techniques' icons for the ability wheel and the Abilities tab (textures/gui/ability/<id>.png, 32 x 32, in the Law domains'
 * gold-trimmed disc, drawn smooth): Iron Skin (a chest of iron plates, glinting), Mountain Stomp (a mountain on the ground, shockwaves running
 * from its foot, the ground cracked), the Ground-Splitting Leap (a leap's arc over ground cracked where it lands), the Thousand-Pound Fist (a
 * fist before the burst of its blow), Blood Frenzy (a drop of blood in red flame) and the Golden Body (a figure of gold seated in its own
 * light); and the realms above: Diamond Will (a cut diamond), Comet Charge (a falling star), Rebounding Bones (crossed bones, an arrow
 * turning back), Flesh Reborn (a heart, a cross of mending light over it) and the Heaven-Shattering Fist (a fist of white gold, the sky breaking
 * before it); and the body's way up, Cloud-Treading Steps (a cloth shoe striding onto clouds); and the ladder's, Earthshaker (rings of heaving
 * earth), the Sky-Pillar Strike (a fist of gold down a pillar of light), Imperishable Gold (a figure of gold in a shell of light), Primordial
 * Roar (a maw and its roar's rings), Myriad Fists (a fan of fists) and the Dharma Body of Heaven and Earth (a giant of gold behind a man).
 * {@code java BeastArt.java <assets> body} writes these alone.
 */
public class BodyArts {
	static void writeAll(java.nio.file.Path assets) throws java.io.IOException {
		java.nio.file.Path dir = assets.resolve("textures/gui/ability");
		BeastArt.write(dir.resolve("iron_skin.png"), ironSkin());
		BeastArt.write(dir.resolve("mountain_stomp.png"), stomp());
		BeastArt.write(dir.resolve("ground_splitting_leap.png"), leap());
		BeastArt.write(dir.resolve("thousand_pound_fist.png"), fist());
		BeastArt.write(dir.resolve("blood_frenzy.png"), frenzy());
		BeastArt.write(dir.resolve("golden_body.png"), golden());
		BeastArt.write(dir.resolve("diamond_will.png"), diamondWill());
		BeastArt.write(dir.resolve("comet_charge.png"), comet());
		BeastArt.write(dir.resolve("rebounding_bones.png"), rebound());
		BeastArt.write(dir.resolve("flesh_reborn.png"), reborn());
		BeastArt.write(dir.resolve("heaven_shattering_fist.png"), shatter());
		BeastArt.write(dir.resolve("cloud_treading.png"), clouds());
		BeastArt.write(dir.resolve("earthshaker.png"), earthshaker());
		BeastArt.write(dir.resolve("sky_pillar_strike.png"), skyPillar());
		BeastArt.write(dir.resolve("imperishable_gold.png"), imperishable());
		BeastArt.write(dir.resolve("primordial_roar.png"), roar());
		BeastArt.write(dir.resolve("myriad_fists.png"), myriad());
		BeastArt.write(dir.resolve("dharma_body.png"), dharma());
	}

	/** Earthshaker: three rings of heaving earth running out across cracked ground from where it was struck, clods thrown up into the dusk. */
	static BufferedImage earthshaker() {
		BufferedImage img = disc(0x2A1A10, 0x6A4422);
		Graphics2D g = inside(img);
		fill(g, new java.awt.geom.Rectangle2D.Double(0, 17.5, 32, 15), 16, 17.5, 0x8A5E2C, 16, 30, 0x3A2412, 0, 0);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 21), 6f, new float[] {0f, 1f}, new Color[] {c(0xFFD890, 0.85), c(0xFFB040, 0)}));
		g.fill(new Ellipse2D.Double(10, 15, 12, 12));
		// The cracks run out from the blow under the rings.
		Color crack = c(0x1A0C04, 0.9);
		line(g, 16, 21.5, 8.5, 26.5, 0.7f, crack);
		line(g, 16, 21.5, 24.5, 27.0, 0.7f, crack);
		line(g, 16, 21.5, 22.5, 18.6, 0.6f, crack);
		line(g, 16, 21.5, 9.6, 18.8, 0.6f, crack);
		// Three rings heaving out, flattened by the eye's slant, the nearest brightest.
		for (int i = 0; i < 3; i++) {
			double rx = 3.6 + i * 3.7, ry = 1.5 + i * 1.35;
			g.setColor(c(0xFFD27A, 0.95 - i * 0.22));
			g.setStroke(new BasicStroke(1.4f - i * 0.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new Ellipse2D.Double(16 - rx, 21.5 - ry, rx * 2, ry * 2));
		}
		// Clods of the ground thrown up into the air.
		for (double[] clod : new double[][] {{7.4, 12.4, 1.8}, {22.8, 11.6, 1.6}, {11.6, 8.4, 1.3}, {19.6, 7.6, 1.4}, {15.4, 12.0, 1.1}}) {
			fill(g, new RoundRectangle2D.Double(clod[0], clod[1], clod[2], clod[2], 0.6, 0.6), clod[0], clod[1], 0xA87440, clod[0] + clod[2],
					clod[1] + clod[2], 0x4A2C12, 0x1E1008, 0.4f);
		}
		g.dispose();
		return img;
	}

	/** Sky-Pillar Strike: a pillar of gold light down out of the night sky, a fist of gold slamming down it, the ground bursting where it lands. */
	static BufferedImage skyPillar() {
		BufferedImage img = disc(0x0E1C3C, 0x34507E);
		Graphics2D g = inside(img);
		g.setColor(c(0xFFFFFF, 0.7));
		for (double[] star : new double[][] {{8.4, 8.0, 0.8}, {23.6, 10.2, 0.7}, {6.6, 15.4, 0.6}, {25.0, 17.0, 0.6}}) {
			g.fill(new Ellipse2D.Double(star[0] - star[2] / 2, star[1] - star[2] / 2, star[2], star[2]));
		}
		// The ground, and the pillar of light standing on it.
		fill(g, new java.awt.geom.Rectangle2D.Double(0, 24.5, 32, 8), 16, 24.5, 0x6A4A2A, 16, 30, 0x2A1A0C, 0, 0);
		g.setPaint(new GradientPaint(16, 2, c(0xFFF2C0, 0.1), 16, 25, c(0xFFD86A, 0.85)));
		g.fill(new java.awt.geom.Rectangle2D.Double(12.6, 2, 6.8, 23));
		g.setPaint(new GradientPaint(16, 2, c(0xFFFFFF, 0.2), 16, 25, c(0xFFFFF0, 0.95)));
		g.fill(new java.awt.geom.Rectangle2D.Double(14.8, 2, 2.4, 23));
		// The burst where it lands, and its shockwave out along the ground.
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 25), 7f, new float[] {0f, 1f}, new Color[] {c(0xFFF4C8, 0.95), c(0xFFB040, 0)}));
		g.fill(new Ellipse2D.Double(9, 18, 14, 14));
		g.setColor(c(0xFFE7A0, 0.85));
		g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new java.awt.geom.Arc2D.Double(4, 22.5, 24, 5, 0, 180, java.awt.geom.Arc2D.OPEN));
		// The fist coming down it, knuckles down: its wrist above, the folds of its fingers.
		fill(g, new RoundRectangle2D.Double(14.0, 6.2, 4.0, 6.0, 1.2, 1.2), 14, 6, 0xF4DCA0, 18, 12, 0xB88A3A, 0x4A320A, 0.7f);
		fill(g, new RoundRectangle2D.Double(12.6, 11.0, 6.8, 7.4, 2.6, 2.6), 12.6, 11, 0xFFF6D8, 19.4, 18.4, 0xD8A84A, 0x5A3A0A, 0.8f);
		Color fold = c(0xA8782A);
		for (double x : new double[] {14.4, 16.0, 17.6}) line(g, x, 15.6, x, 18.0, 0.55f, fold);
		glint(g, 21.6, 9.0, 1.8, c(0xFFFFFF, 0.85));
		g.dispose();
		return img;
	}

	/** Imperishable Gold: a figure of gold standing in a shell of its own light, blows flashing off the shell, its rays bursting out round it. */
	static BufferedImage imperishable() {
		BufferedImage img = disc(0x2A1C04, 0x4E3610);
		Graphics2D g = inside(img);
		for (int i = 0; i < 16; i++) {
			double a = Math.PI * 2 * i / 16, half = 0.1, r = i % 2 == 0 ? 13 : 10;
			GeneralPath ray = new GeneralPath();
			ray.moveTo(16, 16);
			ray.lineTo(16 + Math.cos(a - half) * r, 16 + Math.sin(a - half) * r);
			ray.lineTo(16 + Math.cos(a + half) * r, 16 + Math.sin(a + half) * r);
			ray.closePath();
			g.setColor(c(0xFFE9A0, i % 2 == 0 ? 0.4 : 0.25));
			g.fill(ray);
		}
		// The shell of light about it.
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 16), 9.5f, new float[] {0f, 0.8f, 1f},
				new Color[] {c(0xFFE08A, 0.15), c(0xFFE08A, 0.3), c(0xFFF4C8, 0.7)}));
		g.fill(new Ellipse2D.Double(6.5, 6.5, 19, 19));
		g.setColor(c(0xFFF6D0, 0.95));
		g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Ellipse2D.Double(6.5, 6.5, 19, 19));
		// The figure standing in it: head, shoulders, the body tapering to its feet.
		java.awt.geom.Area figure = new java.awt.geom.Area(new Ellipse2D.Double(14.0, 8.0, 4.0, 4.0));
		GeneralPath body = new GeneralPath();
		body.moveTo(12.2, 13.4);
		body.quadTo(16, 11.6, 19.8, 13.4);
		body.lineTo(19.0, 18.6);
		body.lineTo(18.2, 24.0);
		body.lineTo(13.8, 24.0);
		body.lineTo(13.0, 18.6);
		body.closePath();
		figure.add(new java.awt.geom.Area(body));
		fill(g, figure, 13, 8, 0xFFF6C8, 19, 24, 0xD8921E, 0x6A420A, 0.8f);
		// Blows flashing off the shell.
		glint(g, 7.6, 12.0, 2.0, c(0xFFFFFF, 0.95));
		glint(g, 24.6, 19.6, 1.8, c(0xFFFFFF, 0.9));
		glint(g, 20.4, 7.4, 1.4, c(0xFFFFFF, 0.85));
		g.dispose();
		return img;
	}

	/** Primordial Roar: a great maw roaring to the right, its teeth bared, rings of its roar running out before it. */
	static BufferedImage roar() {
		BufferedImage img = disc(0x1C0E2A, 0x3E1E4A);
		Graphics2D g = inside(img);
		// Its roar: rings opening out to the right, widening, fading.
		for (int i = 0; i < 3; i++) {
			double r = 6.0 + i * 3.6;
			g.setColor(c(0xE8D2FF, 0.9 - i * 0.24));
			g.setStroke(new BasicStroke(1.4f - i * 0.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new java.awt.geom.Arc2D.Double(12.5 - r, 16 - r, r * 2, r * 2, -50, 100, java.awt.geom.Arc2D.OPEN));
		}
		// The maw, open: its throat dark, its upper and lower jaws.
		GeneralPath throat = new GeneralPath();
		throat.moveTo(4.6, 11.4);
		throat.lineTo(14.6, 13.2);
		throat.lineTo(14.6, 18.8);
		throat.lineTo(4.6, 20.6);
		throat.closePath();
		g.setColor(c(0x5A0A14));
		g.fill(throat);
		GeneralPath upper = new GeneralPath();
		upper.moveTo(3.8, 9.0);
		upper.lineTo(15.4, 11.0);
		upper.lineTo(15.0, 13.6);
		upper.lineTo(4.4, 12.6);
		upper.closePath();
		GeneralPath lower = new GeneralPath();
		lower.moveTo(4.4, 19.4);
		lower.lineTo(15.0, 18.4);
		lower.lineTo(15.4, 21.0);
		lower.lineTo(3.8, 23.0);
		lower.closePath();
		fill(g, upper, 4, 9, 0xD8B070, 15, 13, 0x8A5A20, 0x2A1404, 0.7f);
		fill(g, lower, 4, 19, 0xC8A060, 15, 23, 0x7A4A18, 0x2A1404, 0.7f);
		// Its teeth, down from the upper jaw and up from the lower.
		g.setColor(c(0xFFF8E8));
		for (double x : new double[] {7.0, 10.0, 13.0}) {
			GeneralPath fang = new GeneralPath();
			fang.moveTo(x - 0.9, 12.8 + (x - 7) * 0.08);
			fang.lineTo(x + 0.9, 12.9 + (x - 7) * 0.08);
			fang.lineTo(x, 15.0);
			fang.closePath();
			g.fill(fang);
			GeneralPath tusk = new GeneralPath();
			tusk.moveTo(x - 0.9, 19.2 - (x - 7) * 0.08);
			tusk.lineTo(x + 0.9, 19.1 - (x - 7) * 0.08);
			tusk.lineTo(x, 17.2);
			tusk.closePath();
			g.fill(tusk);
		}
		glint(g, 24.8, 8.6, 1.8, c(0xFFFFFF, 0.9));
		g.dispose();
		return img;
	}

	/** Myriad Fists: a fan of fists of gold striking out up and to the right all at once, the ones behind fading, the lines of their swiftness. */
	static BufferedImage myriad() {
		BufferedImage img = disc(0x2A0C0C, 0x4E1A16);
		Graphics2D g = inside(img);
		// A fan of fists out from the lower left, each along its own way, the lines of their swiftness behind them.
		double ox = 5.0, oy = 27.0;
		double[][] fists = {{-78, 15.0, 0.55}, {-56, 17.5, 0.75}, {-34, 17.5, 0.9}, {-12, 15.0, 1.0}};
		for (double[] f : fists) {
			double a = Math.toRadians(f[0]), x = ox + Math.cos(a) * f[1], y = oy + Math.sin(a) * f[1];
			line(g, ox + Math.cos(a) * 4, oy + Math.sin(a) * 4, ox + Math.cos(a) * (f[1] - 5), oy + Math.sin(a) * (f[1] - 5), 0.6f, c(0xFFC8A0, 0.5 * f[2]));
			myriadFist(g, x, y, a, f[2]);
		}
		glint(g, 25.6, 22.6, 1.6, c(0xFFFFFF, 0.85));
		g.dispose();
		return img;
	}

	/** One of Myriad Fists' fists at ({@code x}, {@code y}), striking along {@code angle} (radians), as clear as {@code alpha}: wrist, knuckles, folds. */
	private static void myriadFist(Graphics2D g, double x, double y, double angle, double alpha) {
		java.awt.geom.AffineTransform before = g.getTransform();
		java.awt.Composite composite = g.getComposite();
		g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, (float) alpha));
		g.translate(x, y);
		g.rotate(angle);
		g.scale(1.2, 1.2);
		fill(g, new RoundRectangle2D.Double(-4.2, -1.6, 3.2, 3.2, 1, 1), -4, -1.6, 0xF4DCA0, -1, 1.6, 0xB88A3A, 0x4A320A, 0.5f);
		fill(g, new RoundRectangle2D.Double(-1.6, -2.4, 4.6, 4.8, 1.8, 1.8), -1.6, -2.4, 0xFFF4D4, 3, 2.4, 0xD8A84A, 0x5A3A0A, 0.6f);
		Color fold = c(0xA8782A);
		for (double fy : new double[] {-1.0, 0.4, 1.6}) line(g, 0.6, fy, 2.6, fy, 0.45f, fold);
		g.setTransform(before);
		g.setComposite(composite);
	}

	/** Dharma Body of Heaven and Earth: a giant of gold towering behind a small dark figure, a halo of gold behind its head, the land at its feet. */
	static BufferedImage dharma() {
		BufferedImage img = disc(0x241634, 0x7A4628);
		Graphics2D g = inside(img);
		// The halo behind its head.
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 8.4), 6.5f, new float[] {0f, 0.7f, 1f},
				new Color[] {c(0xFFF2C0, 0.9), c(0xFFD060, 0.5), c(0xFFD060, 0)}));
		g.fill(new Ellipse2D.Double(9.5, 1.9, 13, 13));
		g.setColor(c(0xFFE7A0, 0.9));
		g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Ellipse2D.Double(11.2, 3.6, 9.6, 9.6));
		// The giant: its head, broad shoulders and arms a little out, its legs planted apart.
		java.awt.geom.Area giant = new java.awt.geom.Area(new Ellipse2D.Double(13.6, 5.6, 4.8, 5.2));
		GeneralPath body = new GeneralPath();
		body.moveTo(10.4, 12.2);
		body.quadTo(16, 10.6, 21.6, 12.2);
		body.lineTo(24.2, 19.4);
		body.lineTo(22.4, 19.8);
		body.lineTo(20.4, 15.6);
		body.lineTo(20.2, 20.4);
		body.lineTo(21.4, 28.0);
		body.lineTo(17.6, 28.0);
		body.lineTo(16.0, 21.6);
		body.lineTo(14.4, 28.0);
		body.lineTo(10.6, 28.0);
		body.lineTo(11.8, 20.4);
		body.lineTo(11.6, 15.6);
		body.lineTo(9.6, 19.8);
		body.lineTo(7.8, 19.4);
		body.closePath();
		giant.add(new java.awt.geom.Area(body));
		fill(g, giant, 11, 6, 0xFFF4C0, 21, 28, 0xD48A1C, 0x6A420A, 0.8f);
		// The land at its feet, and the small dark figure standing before it.
		fill(g, new java.awt.geom.Rectangle2D.Double(0, 27.2, 32, 6), 16, 27.2, 0x5A3A1C, 16, 30, 0x2A1A0C, 0, 0);
		g.setColor(c(0x140A10));
		g.fill(new Ellipse2D.Double(15.0, 21.0, 2.0, 2.0));
		GeneralPath man = new GeneralPath();
		man.moveTo(14.8, 23.2);
		man.lineTo(17.2, 23.2);
		man.lineTo(17.6, 27.6);
		man.lineTo(14.4, 27.6);
		man.closePath();
		g.fill(man);
		glint(g, 25.0, 7.0, 1.6, c(0xFFFFFF, 0.85));
		g.dispose();
		return img;
	}

	/** Cloud-Treading Steps: two clouds rising like steps up a night-blue sky, a cloth shoe striding from the lower onto the higher, wind behind it. */
	static BufferedImage clouds() {
		BufferedImage img = disc(0x12284E, 0x2E5C96);
		Graphics2D g = inside(img);
		g.setColor(c(0xFFFFFF, 0.75));
		for (double[] star : new double[][] {{9.0, 8.4, 0.8}, {24.6, 22.8, 0.7}, {7.2, 15.0, 0.6}}) {
			g.fill(new Ellipse2D.Double(star[0] - star[2] / 2, star[1] - star[2] / 2, star[2], star[2]));
		}
		cloud(g, 10.2, 23.8, 9.6);
		cloud(g, 21.6, 19.6, 10.4);
		// The wind of the stride: three strokes trailing off behind the shoe.
		for (int i = 0; i < 3; i++) line(g, 6.8 + i * 0.8, 9.2 + i * 2.0, 11.4 + i * 0.4, 9.2 + i * 2.0, 0.6f, c(0xCFE6FF, 0.75 - i * 0.18));
		// The shoe, side on, its toe curling up to the right: a shaft of slate cloth, a gold band at its top, a thick white sole.
		GeneralPath shoe = new GeneralPath();
		shoe.moveTo(13.0, 14.0);
		shoe.lineTo(21.0, 14.0);
		shoe.quadTo(23.6, 13.8, 23.4, 10.8);
		shoe.quadTo(22.0, 11.6, 20.2, 11.2);
		shoe.quadTo(18.2, 10.6, 17.6, 8.8);
		shoe.lineTo(17.6, 5.8);
		shoe.lineTo(12.8, 5.8);
		shoe.lineTo(12.8, 13.2);
		shoe.closePath();
		fill(g, shoe, 13, 6, 0x7A7A9C, 22, 14, 0x2A2A44, 0x0A0A16, 0.8f);
		line(g, 13.8, 8.0, 13.8, 12.6, 0.6f, c(0xFFFFFF, 0.35));
		line(g, 13.2, 14.4, 21.2, 14.4, 1.6f, c(0xF4F0E4));
		line(g, 13.0, 6.7, 17.4, 6.7, 1.4f, c(0xF0B840));
		glint(g, 25.4, 8.6, 1.8, c(0xFFFFFF, 0.85));
		g.dispose();
		return img;
	}

	/** A cloud {@code w} wide resting on (x, y): a low bank and two puffs over it, white above, sky-blue in its shade, edged deep blue. */
	private static void cloud(Graphics2D g, double x, double y, double w) {
		java.awt.geom.Area shape = new java.awt.geom.Area(new Ellipse2D.Double(x - w / 2, y - w * 0.2, w, w * 0.4));
		shape.add(new java.awt.geom.Area(new Ellipse2D.Double(x - w * 0.36, y - w * 0.4, w * 0.4, w * 0.4)));
		shape.add(new java.awt.geom.Area(new Ellipse2D.Double(x - w * 0.06, y - w * 0.5, w * 0.46, w * 0.46)));
		fill(g, shape, x, y - w * 0.5, 0xFFFFFF, x, y + w * 0.2, 0x9CC2E8, 0x24426E, 0.7f);
	}

	/** Diamond Will: a cut diamond, its facets lit pale blue and white from the upper left, glinting, on a night of deep violet. */
	static BufferedImage diamondWill() {
		BufferedImage img = disc(0x160E30, 0x2C2060);
		Graphics2D g = inside(img);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 16), 11f, new float[] {0f, 1f},
				new Color[] {c(0x9FE8FF, 0.45), c(0x9FE8FF, 0)}));
		g.fill(new Ellipse2D.Double(5, 5, 22, 22));
		// The stone: a crown above its girdle (y 12.5), a pavilion tapering to its point below.
		double[][] crown = {{9.0, 12.5}, {12.0, 8.4}, {20.0, 8.4}, {23.0, 12.5}};
		GeneralPath stone = new GeneralPath();
		stone.moveTo(crown[0][0], crown[0][1]);
		for (int i = 1; i < crown.length; i++) stone.lineTo(crown[i][0], crown[i][1]);
		stone.lineTo(16, 25.6);
		stone.closePath();
		fill(g, stone, 10, 8, 0xE8FBFF, 22, 25, 0x3A8FC8, 0x0E2A48, 1.0f);
		// Its facets: the table, the crown's stars, the pavilion's mains, each a shade of its own.
		facet(g, 0xFFFFFF, 0.55, new double[] {12.0, 8.4, 20.0, 8.4, 18.0, 12.5, 14.0, 12.5});
		facet(g, 0xCFF4FF, 0.45, new double[] {9.0, 12.5, 12.0, 8.4, 14.0, 12.5});
		facet(g, 0x7FC8EE, 0.45, new double[] {23.0, 12.5, 20.0, 8.4, 18.0, 12.5});
		facet(g, 0xBFEFFF, 0.5, new double[] {9.0, 12.5, 14.0, 12.5, 16, 25.6});
		facet(g, 0x4AA0D8, 0.5, new double[] {23.0, 12.5, 18.0, 12.5, 16, 25.6});
		Color edge = c(0x1C4A70, 0.7);
		line(g, 9.0, 12.5, 23.0, 12.5, 0.6f, edge);
		line(g, 14.0, 12.5, 16, 25.6, 0.5f, edge);
		line(g, 18.0, 12.5, 16, 25.6, 0.5f, edge);
		glint(g, 12.6, 9.8, 2.6, c(0xFFFFFF));
		glint(g, 24.4, 19.6, 1.8, c(0xFFFFFF, 0.85));
		g.dispose();
		return img;
	}

	/** A facet of a stone: the polygon of {@code points} (x, y pairs) washed {@code rgb} at {@code alpha}. */
	private static void facet(Graphics2D g, int rgb, double alpha, double[] points) {
		GeneralPath path = new GeneralPath();
		path.moveTo(points[0], points[1]);
		for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
		path.closePath();
		g.setColor(c(rgb, alpha));
		g.fill(path);
	}

	/** Comet Charge: a falling star streaking down from the upper right, its head white-hot, its tail of flame trailing up and away. */
	static BufferedImage comet() {
		BufferedImage img = disc(0x0C1430, 0x2A2050);
		Graphics2D g = inside(img);
		g.setColor(c(0xFFFFFF, 0.8));
		for (double[] star : new double[][] {{8.2, 9.0, 0.9}, {12.4, 22.6, 0.7}, {23.4, 24.2, 0.8}, {6.6, 16.4, 0.6}}) {
			g.fill(new Ellipse2D.Double(star[0] - star[2] / 2, star[1] - star[2] / 2, star[2], star[2]));
		}
		// The tail: a tapering wedge from the head up to the upper right, flame to ember, fading.
		double hx = 11.6, hy = 20.4;
		GeneralPath tail = new GeneralPath();
		tail.moveTo(hx - 2.4, hy - 2.0);
		tail.lineTo(26.0, 5.4);
		tail.lineTo(hx + 2.0, hy + 2.4);
		tail.closePath();
		g.setPaint(new GradientPaint((float) hx, (float) hy, c(0xFFC24A, 0.95), 25f, 6f, c(0xE0401A, 0.0f)));
		g.fill(tail);
		GeneralPath core = new GeneralPath();
		core.moveTo(hx - 1.0, hy - 0.8);
		core.lineTo(22.5, 8.6);
		core.lineTo(hx + 0.8, hy + 1.0);
		core.closePath();
		g.setPaint(new GradientPaint((float) hx, (float) hy, c(0xFFF6D0, 0.95), 22f, 9f, c(0xFFD070, 0.0f)));
		g.fill(core);
		// The head, white-hot in a glow.
		g.setPaint(new RadialGradientPaint(new Point2D.Double(hx, hy), 5.5f, new float[] {0f, 1f},
				new Color[] {c(0xFFE9A0, 0.9), c(0xFFB040, 0)}));
		g.fill(new Ellipse2D.Double(hx - 5.5, hy - 5.5, 11, 11));
		g.setColor(c(0xFFFFFF));
		g.fill(new Ellipse2D.Double(hx - 2.2, hy - 2.2, 4.4, 4.4));
		g.dispose();
		return img;
	}

	/** A bone {@code w} thick from (x1, y1) to (x2, y2), its two knobbed ends, ivory lit from above. */
	private static void bone(Graphics2D g, double x1, double y1, double x2, double y2, double w) {
		Color ivory = c(0xF2EBD8), shade = c(0x8C8270);
		g.setColor(shade);
		g.setStroke(new BasicStroke((float) (w + 1.2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(x1, y1, x2, y2));
		double dx = x2 - x1, dy = y2 - y1, d = Math.hypot(dx, dy), nx = -dy / d, ny = dx / d;
		for (double[] end : new double[][] {{x1, y1}, {x2, y2}}) {
			for (int s = -1; s <= 1; s += 2) {
				double kx = end[0] + nx * s * w * 0.55, ky = end[1] + ny * s * w * 0.55, r = w * 0.75;
				g.setColor(shade);
				g.fill(new Ellipse2D.Double(kx - r - 0.6, ky - r - 0.6, (r + 0.6) * 2, (r + 0.6) * 2));
			}
		}
		g.setColor(ivory);
		g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(x1, y1, x2, y2));
		for (double[] end : new double[][] {{x1, y1}, {x2, y2}}) {
			for (int s = -1; s <= 1; s += 2) {
				double kx = end[0] + nx * s * w * 0.55, ky = end[1] + ny * s * w * 0.55, r = w * 0.75;
				g.fill(new Ellipse2D.Double(kx - r, ky - r, r * 2, r * 2));
			}
		}
	}

	/** Rebounding Bones: two bones crossed, and a curving arrow of gold turning back on itself about them. */
	static BufferedImage rebound() {
		BufferedImage img = disc(0x1E1A16, 0x3C342A);
		Graphics2D g = inside(img);
		bone(g, 10.2, 10.2, 21.8, 21.8, 2.2);
		bone(g, 21.8, 10.2, 10.2, 21.8, 2.2);
		// The arrow: an arc over the top, turning back down the right, its head pointing back the way it came.
		g.setColor(c(0xF0B840));
		g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new java.awt.geom.Arc2D.Double(6.2, 6.2, 19.6, 19.6, 200, -150, java.awt.geom.Arc2D.OPEN));
		double a = Math.toRadians(50), ex = 16 + Math.cos(a) * 9.8, ey = 16 - Math.sin(a) * 9.8;
		GeneralPath head = new GeneralPath();
		head.moveTo(ex + 2.6, ey + 2.4);
		head.lineTo(ex - 1.2, ey + 1.4);
		head.lineTo(ex + 1.6, ey - 1.8);
		head.closePath();
		g.fill(head);
		glint(g, 8.0, 22.4, 1.8, c(0xFFFFFF, 0.8));
		g.dispose();
		return img;
	}

	/** Flesh Reborn: a heart of living red, a cross of green mending light over it, the glow of it about. */
	static BufferedImage reborn() {
		BufferedImage img = disc(0x14200E, 0x2C3E1E);
		Graphics2D g = inside(img);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 17), 11f, new float[] {0f, 1f},
				new Color[] {c(0xB8FF9A, 0.45), c(0xB8FF9A, 0)}));
		g.fill(new Ellipse2D.Double(5, 6, 22, 22));
		GeneralPath heart = new GeneralPath();
		heart.moveTo(16, 25.2);
		heart.curveTo(9.0, 20.0, 7.6, 15.6, 9.6, 12.8);
		heart.curveTo(11.6, 10.2, 15.0, 10.8, 16, 13.6);
		heart.curveTo(17.0, 10.8, 20.4, 10.2, 22.4, 12.8);
		heart.curveTo(24.4, 15.6, 23.0, 20.0, 16, 25.2);
		heart.closePath();
		fill(g, heart, 11, 11, 0xF2525C, 21, 25, 0x8A1420, 0x3A0408, 1.0f);
		g.setColor(c(0xFFFFFF, 0.6));
		g.fill(new Ellipse2D.Double(10.6, 13.0, 2.4, 3.0));
		// The mending: a cross of green light over its upper right, edged dark, and motes of it about.
		line(g, 21.4, 7.0, 21.4, 14.6, 3.6f, c(0x1E4A12));
		line(g, 17.6, 10.8, 25.2, 10.8, 3.6f, c(0x1E4A12));
		line(g, 21.4, 7.0, 21.4, 14.6, 2.2f, c(0x9CF07A));
		line(g, 17.6, 10.8, 25.2, 10.8, 2.2f, c(0x9CF07A));
		g.setColor(c(0xE8FFD8, 0.9));
		for (double[] mote : new double[][] {{9.0, 8.8, 1.2}, {7.6, 20.4, 1.0}, {24.4, 21.6, 1.1}}) {
			g.fill(new Ellipse2D.Double(mote[0] - mote[2] / 2, mote[1] - mote[2] / 2, mote[2], mote[2]));
		}
		g.dispose();
		return img;
	}

	/** Heaven-Shattering Fist: a fist of white gold driving to the right, the sky before it breaking in rings of force and cracks of light. */
	static BufferedImage shatter() {
		BufferedImage img = disc(0x1A0E2E, 0x3A1C4C);
		Graphics2D g = inside(img);
		// The cracks of light across the sky before it.
		Color crack = c(0xFFF0B0, 0.85);
		line(g, 19.0, 15.6, 23.4, 12.0, 0.7f, crack);
		line(g, 23.4, 12.0, 26.6, 13.2, 0.6f, crack);
		line(g, 19.4, 17.2, 24.0, 21.8, 0.7f, crack);
		line(g, 24.0, 21.8, 23.4, 26.0, 0.6f, crack);
		line(g, 20.0, 16.4, 27.4, 17.0, 0.7f, crack);
		// The rings of force, three widening arcs running out to the right.
		for (int i = 0; i < 3; i++) {
			double r = 5.0 + i * 3.2;
			g.setColor(c(0xBFE6FF, 0.85 - i * 0.22));
			g.setStroke(new BasicStroke(1.3f - i * 0.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(new java.awt.geom.Arc2D.Double(15.2 - r, 16.4 - r, r * 2, r * 2, -55, 110, java.awt.geom.Arc2D.OPEN));
		}
		// The fist, side on: its knuckles to the right, the thumb over the fingers, the wrist off the left edge.
		fill(g, new RoundRectangle2D.Double(3.0, 13.4, 8.0, 6.4, 2, 2), 3, 13, 0xF4DCA0, 11, 20, 0xB88A3A, 0x4A320A, 0.8f);
		fill(g, new RoundRectangle2D.Double(9.0, 11.2, 9.0, 10.4, 4, 4), 9, 11, 0xFFF6D8, 18, 22, 0xD8A84A, 0x5A3A0A, 0.9f);
		Color fold = c(0xA8782A);
		for (double y : new double[] {13.8, 16.4, 19.0}) line(g, 13.4, y, 17.4, y, 0.6f, fold);
		fill(g, new RoundRectangle2D.Double(9.4, 11.6, 6.6, 3.0, 2.6, 2.6), 9, 12, 0xFFF0C8, 16, 15, 0xE0B458, 0x5A3A0A, 0.7f);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(18.2, 16.4), 4.5f, new float[] {0f, 1f},
				new Color[] {c(0xFFFFFF, 0.8), c(0xFFFFFF, 0)}));
		g.fill(new Ellipse2D.Double(13.7, 11.9, 9, 9));
		g.dispose();
		return img;
	}

	/** The disc every one is drawn in (as Techniques#sleeve's): {@code top} to {@code low} down its face, its rim of light, the gold trim lit from above. */
	static BufferedImage disc(int top, int low) {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int faceTop = BeastArt.rgb(top), faceLow = BeastArt.rgb(low), white = BeastArt.rgb(0xFFFFFF);
		int gold = BeastArt.rgb(0xD9A22E), goldLight = BeastArt.rgb(0xF5CF55), goldDeep = BeastArt.rgb(0x6E5214);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				double dx = x - 15.5, dy = y - 15.5, r = Math.sqrt(dx * dx + dy * dy);
				int c = 0;
				if (r < 12.6) c = BeastArt.mix(faceTop, faceLow, (y - 3) / 24.0);
				else if (r < 13.5) c = BeastArt.mix(faceLow, white, 0.22);
				else if (r < 14.3) c = dy < 0 ? goldLight : gold;
				else if (r < 15.2) c = dy < 0 ? gold : goldDeep;
				else if (r < 15.8) c = 0xA0 << 24 | goldDeep & 0xFFFFFF;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** {@code img}'s graphics, drawing smooth, held inside the disc's face (pixel x's middle is x + 0.5 here, so its middle is at 16). */
	static Graphics2D inside(BufferedImage img) {
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setClip(new Ellipse2D.Double(16 - 12.6, 16 - 12.6, 25.2, 25.2));
		return g;
	}

	static Color c(int rgb) {
		return new Color(rgb);
	}

	static Color c(int rgb, double alpha) {
		return new Color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, (int) Math.round(alpha * 255));
	}

	/** {@code shape} filled down a gradient from {@code from} at (x1, y1) to {@code to} at (x2, y2), edged {@code edge} wide in {@code dark}. */
	static void fill(Graphics2D g, Shape shape, double x1, double y1, int from, double x2, double y2, int to, int dark, float edge) {
		g.setPaint(new GradientPaint((float) x1, (float) y1, c(from), (float) x2, (float) y2, c(to)));
		g.fill(shape);
		if (edge > 0) {
			g.setColor(c(dark));
			g.setStroke(new BasicStroke(edge, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(shape);
		}
	}

	static void line(Graphics2D g, double x1, double y1, double x2, double y2, float width, Color colour) {
		g.setColor(colour);
		g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new Line2D.Double(x1, y1, x2, y2));
	}

	/** A four-pointed glint {@code r} across at (x, y). */
	static void glint(Graphics2D g, double x, double y, double r, Color colour) {
		GeneralPath star = new GeneralPath();
		star.moveTo(x, y - r);
		star.quadTo(x, y, x + r, y);
		star.quadTo(x, y, x, y + r);
		star.quadTo(x, y, x - r, y);
		star.quadTo(x, y, x, y - r);
		g.setColor(colour);
		g.fill(star);
	}

	/** Iron Skin: a chest of iron, its plates parted by dark seams, lit from the upper left, a gleam along its shoulder, glints about it. */
	static BufferedImage ironSkin() {
		BufferedImage img = disc(0x1C2026, 0x3A424C);
		Graphics2D g = inside(img);
		GeneralPath chest = new GeneralPath();
		chest.moveTo(7.5, 12.5);
		chest.quadTo(16, 8.2, 24.5, 12.5);
		chest.lineTo(22.6, 19.5);
		chest.quadTo(21.4, 25.6, 16, 27.2);
		chest.quadTo(10.6, 25.6, 9.4, 19.5);
		chest.closePath();
		fill(g, chest, 9, 9, 0xE2E8EF, 23, 27, 0x4A525C, 0x15191E, 1.1f);
		Color seam = c(0x262C34);
		line(g, 16, 11.2, 16, 26.4, 0.9f, seam);
		GeneralPath pecs = new GeneralPath();
		pecs.moveTo(9.6, 16.6);
		pecs.quadTo(13.2, 19.8, 16, 18.2);
		pecs.quadTo(18.8, 19.8, 22.4, 16.6);
		g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(pecs);
		line(g, 11.8, 21.6, 20.2, 21.6, 0.8f, seam);
		line(g, 12.8, 24.2, 19.2, 24.2, 0.8f, seam);
		// Rivets at the shoulders, and the gleam along the left one.
		g.setColor(c(0xF4F7FA));
		g.fill(new Ellipse2D.Double(9.4, 12.2, 1.4, 1.4));
		g.fill(new Ellipse2D.Double(21.2, 12.2, 1.4, 1.4));
		line(g, 10.6, 14.6, 13.6, 13.2, 1.1f, c(0xFFFFFF, 0.75));
		glint(g, 24.8, 8.6, 2.6, c(0xFFFFFF));
		glint(g, 7.4, 22.4, 1.8, c(0xFFFFFF, 0.85));
		g.dispose();
		return img;
	}

	/** Mountain Stomp: a mountain set on the ground, two rings of force running out from its foot, the ground cracked under it, stones flung up. */
	static BufferedImage stomp() {
		BufferedImage img = disc(0x24180F, 0x4A3221);
		Graphics2D g = inside(img);
		g.setColor(c(0x5A4630));
		g.fill(new java.awt.geom.Rectangle2D.Double(0, 22.5, 32, 10));
		line(g, 0, 22.6, 32, 22.6, 0.9f, c(0x8C704A));
		g.setStroke(new BasicStroke(1.0f));
		g.setColor(c(0xFFE7A0, 0.55));
		g.draw(new Ellipse2D.Double(16 - 11.2, 22.6 - 3.4, 22.4, 6.8));
		g.setColor(c(0xFFE7A0, 0.9));
		g.draw(new Ellipse2D.Double(16 - 7.2, 22.6 - 2.2, 14.4, 4.4));
		GeneralPath mountain = new GeneralPath();
		mountain.moveTo(8.6, 22.8);
		mountain.lineTo(16, 7.4);
		mountain.lineTo(23.4, 22.8);
		mountain.closePath();
		fill(g, mountain, 16, 8, 0xC2B39A, 16, 23, 0x6A5A48, 0x2A2016, 0.9f);
		GeneralPath shade = new GeneralPath();
		shade.moveTo(16, 7.4);
		shade.lineTo(23.4, 22.8);
		shade.lineTo(17.2, 22.8);
		shade.closePath();
		g.setColor(c(0x000000, 0.25));
		g.fill(shade);
		GeneralPath snow = new GeneralPath();
		snow.moveTo(13.5, 12.6);
		snow.lineTo(16, 7.4);
		snow.lineTo(18.5, 12.6);
		snow.lineTo(17.3, 11.6);
		snow.lineTo(16, 13.2);
		snow.lineTo(14.7, 11.6);
		snow.closePath();
		g.setColor(c(0xF6F2EA));
		g.fill(snow);
		Color crack = c(0x1A100A);
		line(g, 16, 23.2, 12.6, 26.4, 0.9f, crack);
		line(g, 12.6, 26.4, 11.4, 29.4, 0.8f, crack);
		line(g, 16, 23.2, 19.8, 27.0, 0.9f, crack);
		line(g, 19.8, 27.0, 19.4, 29.8, 0.8f, crack);
		line(g, 16, 23.2, 16.4, 29.6, 0.8f, crack);
		g.setColor(c(0x9A8466));
		for (double[] stone : new double[][] {{6.6, 18.2, 1.6}, {25.2, 17.6, 1.8}, {8.6, 14.8, 1.2}, {23.6, 14.2, 1.1}}) {
			g.fill(new Ellipse2D.Double(stone[0] - stone[2] / 2, stone[1] - stone[2] / 2, stone[2], stone[2]));
		}
		g.dispose();
		return img;
	}

	/** Ground-Splitting Leap: a leap's arc, dashed in gold, from a puff of dust up over the disc and down onto ground split where it lands. */
	static BufferedImage leap() {
		BufferedImage img = disc(0x142238, 0x2E4466);
		Graphics2D g = inside(img);
		g.setColor(c(0x4A3A2A));
		g.fill(new java.awt.geom.Rectangle2D.Double(0, 23, 32, 10));
		line(g, 0, 23.1, 32, 23.1, 0.9f, c(0x7A6248));
		Color crack = c(0x140C06);
		line(g, 22.2, 23.2, 20.8, 25.4, 1.2f, crack);
		line(g, 20.8, 25.4, 23.0, 27.0, 1.1f, crack);
		line(g, 23.0, 27.0, 21.6, 29.8, 1.0f, crack);
		line(g, 22.2, 23.2, 25.6, 25.6, 0.9f, crack);
		g.setColor(c(0xD8CCB8, 0.6));
		for (double[] puff : new double[][] {{5.6, 21.4, 2.6}, {8.2, 21.8, 2.0}, {18.6, 21.6, 2.2}, {25.8, 21.6, 2.4}}) {
			g.fill(new Ellipse2D.Double(puff[0] - puff[2] / 2, puff[1] - puff[2] / 2, puff[2], puff[2]));
		}
		g.setColor(c(0xFFD86A));
		g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[] {2.4f, 1.8f}, 0));
		g.draw(new QuadCurve2D.Double(6.4, 21.6, 14.4, -5.6, 21.8, 20.6));
		// Its head, pointing down along the arc's end.
		double ex = 21.8, ey = 20.6, dx = 21.8 - 14.4, dy = 20.6 + 5.6, d = Math.hypot(dx, dy);
		dx /= d;
		dy /= d;
		GeneralPath head = new GeneralPath();
		head.moveTo(ex + dx * 2.0, ey + dy * 2.0);
		head.lineTo(ex - dx * 1.4 - dy * 2.0, ey - dy * 1.4 + dx * 2.0);
		head.lineTo(ex - dx * 1.4 + dy * 2.0, ey - dy * 1.4 - dx * 2.0);
		head.closePath();
		g.fill(head);
		g.dispose();
		return img;
	}

	/** Thousand-Pound Fist: a fist of bronze-brown skin seen from before, its knuckles lit, the burst of its blow flaring behind it. */
	static BufferedImage fist() {
		BufferedImage img = disc(0x2A1210, 0x4C2418);
		Graphics2D g = inside(img);
		// The burst: an eight-pointed star of pale gold, softer toward its points.
		GeneralPath burst = new GeneralPath();
		for (int i = 0; i < 16; i++) {
			double a = Math.PI * 2 * i / 16 - Math.PI / 2, r = i % 2 == 0 ? 11.5 : 4.6;
			double x = 17.5 + Math.cos(a) * r, y = 13.5 + Math.sin(a) * r;
			if (i == 0) burst.moveTo(x, y);
			else burst.lineTo(x, y);
		}
		burst.closePath();
		g.setPaint(new RadialGradientPaint(new Point2D.Double(17.5, 13.5), 11.5f, new float[] {0f, 1f},
				new Color[] {c(0xFFF2C0, 0.95), c(0xFFB040, 0.15)}));
		g.fill(burst);
		// The forearm and its wrap, then the fist on it.
		fill(g, new RoundRectangle2D.Double(12.2, 19.6, 7.6, 9.4, 2, 2), 12, 20, 0xB87040, 20, 29, 0x7A4220, 0x3A1A08, 0.9f);
		fill(g, new RoundRectangle2D.Double(11.8, 20.6, 8.4, 2.4, 1, 1), 12, 20, 0xE6D8B0, 20, 23, 0xA89870, 0x4A3A20, 0.7f);
		fill(g, new RoundRectangle2D.Double(9.2, 9.2, 13.6, 11.2, 4.2, 4.2), 9, 9, 0xEDB27A, 22, 20, 0x9A5A30, 0x3A1A08, 1.0f);
		Color fold = c(0x6A3818);
		for (double x : new double[] {12.6, 16.0, 19.4}) line(g, x, 9.8, x, 14.8, 0.75f, fold);
		g.setColor(c(0xFFD8AC));
		for (double x : new double[] {10.4, 13.8, 17.2, 20.6}) g.fill(new Ellipse2D.Double(x - 0.2, 10.0, 1.8, 1.2));
		fill(g, new RoundRectangle2D.Double(9.0, 14.8, 10.8, 4.2, 3.6, 3.6), 9, 15, 0xE0A06A, 20, 19, 0xA8683A, 0x3A1A08, 0.9f);
		g.dispose();
		return img;
	}

	/** Blood Frenzy: a drop of blood, deep and glossy, in a ring of red flame licking up about it. */
	static BufferedImage frenzy() {
		BufferedImage img = disc(0x1E0606, 0x3C0A0A);
		Graphics2D g = inside(img);
		GeneralPath flame = new GeneralPath();
		for (int i = 0; i <= 72; i++) {
			double a = Math.PI * 2 * i / 72;
			double r = 9.0 + 2.0 * Math.sin(a * 5) + 1.1 * Math.sin(a * 9 + 1);
			double up = Math.max(0, -Math.sin(a)); // the tongues reach higher above than below
			double x = 16 + Math.cos(a) * r, y = 17.5 + Math.sin(a) * r * (1 + 0.35 * up);
			if (i == 0) flame.moveTo(x, y);
			else flame.lineTo(x, y);
		}
		flame.closePath();
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 17.5), 12f, new float[] {0f, 0.55f, 1f},
				new Color[] {c(0xFFB040, 0.95), c(0xFF4A1A, 0.85), c(0xA01008, 0.25)}));
		g.fill(flame);
		GeneralPath drop = new GeneralPath();
		drop.moveTo(16, 7.2);
		drop.curveTo(16, 7.2, 22.2, 14.6, 22.2, 18.6);
		drop.curveTo(22.2, 22.2, 19.4, 25.0, 16, 25.0);
		drop.curveTo(12.6, 25.0, 9.8, 22.2, 9.8, 18.6);
		drop.curveTo(9.8, 14.6, 16, 7.2, 16, 7.2);
		drop.closePath();
		fill(g, drop, 11, 12, 0xE8262E, 21, 25, 0x5E0408, 0x220204, 1.0f);
		g.setColor(c(0xFFFFFF, 0.55));
		g.fill(new Ellipse2D.Double(12.2, 15.2, 2.2, 4.4));
		g.setColor(c(0xFFFFFF, 0.8));
		g.fill(new Ellipse2D.Double(12.8, 15.8, 1.0, 1.4));
		g.dispose();
		return img;
	}

	/** Golden Body: a figure of gold seated in meditation, its light raying out behind it and a glow about it. */
	static BufferedImage golden() {
		BufferedImage img = disc(0x261A06, 0x4A3410);
		Graphics2D g = inside(img);
		for (int i = 0; i < 12; i++) {
			double a = Math.PI * 2 * i / 12, half = 0.13, r = i % 2 == 0 ? 13 : 9;
			GeneralPath ray = new GeneralPath();
			ray.moveTo(16, 15);
			ray.lineTo(16 + Math.cos(a - half) * r, 15 + Math.sin(a - half) * r);
			ray.lineTo(16 + Math.cos(a + half) * r, 15 + Math.sin(a + half) * r);
			ray.closePath();
			g.setColor(c(0xFFE9A0, i % 2 == 0 ? 0.42 : 0.28));
			g.fill(ray);
		}
		g.setPaint(new RadialGradientPaint(new Point2D.Double(16, 16), 11f, new float[] {0f, 1f},
				new Color[] {c(0xFFE08A, 0.6), c(0xFFE08A, 0)}));
		g.fill(new Ellipse2D.Double(5, 5, 22, 22));
		GeneralPath body = new GeneralPath();
		body.append(new Ellipse2D.Double(13.5, 6.2, 5.0, 5.0), false);
		GeneralPath torso = new GeneralPath();
		torso.moveTo(12.0, 13.0);
		torso.quadTo(16, 10.8, 20.0, 13.0);
		torso.lineTo(20.9, 20.0);
		torso.lineTo(11.1, 20.0);
		torso.closePath();
		body.append(torso, false);
		body.append(new Ellipse2D.Double(8.4, 18.6, 15.2, 6.6), false);
		java.awt.geom.Area figure = new java.awt.geom.Area(body);
		fill(g, figure, 12, 6, 0xFFF4B8, 20, 25, 0xD08A1A, 0x6A420A, 0.9f);
		// Its hands at rest in its lap, and the line of its arms.
		g.setColor(c(0x8A5A10, 0.8));
		g.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(new QuadCurve2D.Double(12.4, 14.4, 12.6, 19.4, 16, 20.2));
		g.draw(new QuadCurve2D.Double(19.6, 14.4, 19.4, 19.4, 16, 20.2));
		glint(g, 23.6, 8.0, 2.2, c(0xFFFFFF, 0.9));
		g.dispose();
		return img;
	}
}
