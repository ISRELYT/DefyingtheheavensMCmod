package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AstralPlanets;
import com.example.defyingtheheavens.PlanetTerrain;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec3;

import java.util.stream.IntStream;

/**
 * Paints what the Astral Realm shows from afar, on the CPU (PlanetTextures runs it on its own threads):
 * <ul>
 *   <li><b>A planet</b>: its land as PlanetTerrain lays it (the same continents, seas, ice, deserts, forests and mountains one lands
 *   on), seas by their depth, rock and snow on the heights, a mesa's bands, a crystal world's glints, lava's glow; shaded by its
 *   relief, under its clouds and their shadows, lit by its own star from where that star truly lies (nothing in the Astral Realm
 *   moves, so the light is painted in), the terminator soft and warm where the planet has air. Equirectangular: x round the
 *   equator, y pole to pole, as its land is laid out.</li>
 *   <li><b>A planet's rings</b>: a strip of bands and gaps from the inner edge to the outer.</li>
 *   <li><b>The sky behind it all</b>: nebulae of violet, teal and ember, a galactic band with its dust lanes.</li>
 *   <li><b>A star's face</b>: limb-darkened and granulated, and its corona.</li>
 * </ul>
 * Pixels are written as NativeImage wants them (ABGR).
 */
final class PlanetPainter {
	/** How far relief is exaggerated in the shading: the land's few hundred blocks of height would not show from space otherwise. */
	private static final double RELIEF = 18;

	/** {@code planet} lit from {@code sun} (a unit vector from the planet toward its star), {@code width} by {@code width / 2}. */
	static NativeImage paint(AstralPlanets.Planet planet, Vec3 sun, int width) {
		int w = width, h = width / 2;
		PlanetTerrain terrain = PlanetTerrain.of(planet);
		PlanetLooks.Look look = PlanetLooks.of(planet);
		boolean air = look.atmosphereStrength() > 0;
		SimplexNoise clouds = new SimplexNoise(RandomSource.create(planet.seed() ^ 0xC10DL));
		SimplexNoise grain = new SimplexNoise(RandomSource.create(planet.seed() ^ 0x6A1L));
		SimplexNoise regions = new SimplexNoise(RandomSource.create(planet.seed() ^ 0x2E61L));
		boolean arid = switch (planet.kind()) {
			case DESERT, MESA, SAVANNA, ASHEN, BARREN, VOLCANIC -> true;
			default -> false;
		};
		Vec3 wind = new Vec3(Math.cos(planet.seed() % 7), 0.3, Math.sin(planet.seed() % 11)).normalize();
		RandomSource bands = RandomSource.create(planet.seed() ^ 0xBA4DL);
		int[] mesa = new int[16];
		int[] mesaPalette = {0xD07A3A, 0xB8502E, 0xE8D4B0, 0x9A5A3A, 0xE0B050, 0xC8A090, 0xA8442A};
		for (int i = 0; i < mesa.length; i++) mesa[i] = mesaPalette[bands.nextInt(mesaPalette.length)];

		float[] heights = new float[w * h];
		int[] albedo = new int[w * h];
		float[] glow = new float[w * h];
		boolean[] wet = new boolean[w * h];
		double halfX = AstralPlanets.LAND_RADIUS, halfZ = AstralPlanets.LAND_POLE;
		// Row by row across the machine's cores (each pixel its own; the noises only read).
		IntStream.range(0, h).parallel().forEach(y -> {
			double dz = ((y + 0.5) / h * 2 - 1) * halfZ;
			for (int x = 0; x < w; x++) {
				double dx = ((x + 0.5) / w * 2 - 1) * halfX;
				int i = y * w + x;
				PlanetTerrain.Column c = terrain.column(dx, dz, true);
				Vec3 p = PlanetTerrain.directionOver(dx, dz);
				double g = grain.getValue(p.x * 180, p.y * 180, p.z * 180) * 0.6 + grain.getValue(p.x * 520, p.y * 520, p.z * 520) * 0.4;
				int colour;
				float shine = 0;
				if (c.underFluid() && c.fluid() == PlanetTerrain.Fluid.LAVA) {
					colour = PlanetLooks.lerp(0xFF5A14, 0xFFB040, g * 0.5 + 0.5);
					shine = 0.9f;
					wet[i] = true;
					heights[i] = c.fluidTop();
				} else if (c.underFluid()) {
					double depth = c.fluidTop() - c.ground();
					int base = c.zone().colour;
					boolean ice = c.temperature() < -0.55;
					if (ice) {
						colour = PlanetLooks.lerp(0xC8DCEC, 0xEEF6FA, g * 0.5 + 0.5);
					} else if (depth < 4) {
						// Only the narrow shelves are bright; the open sea is deep blue.
						colour = PlanetLooks.lerp(PlanetLooks.lerp(base, 0x3AC0C0, 0.35), base, depth / 4.0);
					} else {
						colour = PlanetLooks.lerp(base, PlanetLooks.lerp(base, 0x061428, 0.65), (depth - 4) / 24.0);
					}
					wet[i] = !ice;
					heights[i] = ice ? c.fluidTop() + 1 : c.fluidTop();
				} else {
					colour = c.zone().colour;
					int alt = c.ground() - PlanetTerrain.SEA_LEVEL;
					if (c.mountain() > 25) colour = PlanetLooks.lerp(colour, 0x7E7A74, Math.min(0.8, (c.mountain() - 25) / 90.0));
					if (c.zone() == PlanetTerrain.Zone.BADLANDS || c.zone() == PlanetTerrain.Zone.ERODED_BADLANDS || c.zone() == PlanetTerrain.Zone.WOODED_BADLANDS) {
						colour = PlanetLooks.lerp(colour, mesa[Math.floorMod(c.ground() / 5, mesa.length)], 0.6);
					}
					boolean snowy = c.temperature() < -0.5 || alt > 125 && c.temperature() < 0.35;
					if (snowy && terrain.fluid() != PlanetTerrain.Fluid.LAVA) colour = PlanetLooks.lerp(colour, 0xF2F6FA, 0.88);
					if (planet.kind() == AstralPlanets.Kind.CRYSTAL && g > 0.55) {
						colour = PlanetLooks.lerp(colour, 0xF4ECFF, (g - 0.55) * 1.6);
						shine = (float) ((g - 0.55) * 0.5);
					}
					if (planet.kind() == AstralPlanets.Kind.VOLCANIC) {
						// Glowing fissures across the volcanic lands.
						double crack = 1 - Math.abs(grain.getValue(p.x * 40 + 9, p.y * 40, p.z * 40 - 3));
						if (crack > 0.93) shine = (float) ((crack - 0.93) * 12);
					}
					// Broad regions of the land, darker and lighter (and on the dry worlds, redder and paler), as a world's face is
					// never one colour: rock showing through, old lava fields, salt flats.
					double region = fbm(regions, p.x, p.y, p.z, 2.4, 4);
					colour = scale(colour, 1 + (arid ? 0.22 : 0.12) * region);
					if (arid) colour = region > 0 ? PlanetLooks.lerp(colour, 0x7A4A30, region * 0.45) : PlanetLooks.lerp(colour, 0xF2E6C8, -region * 0.35);
					// A sand sea's dunes, in streaks across the wind.
					if (c.zone() == PlanetTerrain.Zone.DESERT || c.zone() == PlanetTerrain.Zone.ASH_DUNES) {
						double across = p.x * wind.x + p.y * wind.y + p.z * wind.z;
						double streak = Math.sin(across * 900 + 3 * grain.getValue(p.x * 25, p.y * 25, p.z * 25));
						colour = scale(colour, 1 + 0.06 * streak);
					}
					heights[i] = c.ground();
				}
				colour = scale(colour, 0.93 + 0.14 * g);
				albedo[i] = colour;
				glow[i] = shine;
			}
		});

		// Coasts and the edges between lands softened (a texel's hard step would show as a stair from near by).
		soften(albedo, w, h);

		NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
		double step = 2.0 * halfX / w;
		int atmosphere = look.atmosphere();
		IntStream.range(0, h).parallel().forEach(y -> {
			double lat = (0.5 - (y + 0.5) / h) * Math.PI;
			double cl = Math.cos(lat), sl = Math.sin(lat);
			double stepEast = Math.max(step * cl, step * 0.05), stepNorth = step;
			for (int x = 0; x < w; x++) {
				int i = y * w + x;
				double lon = ((x + 0.5) / w * 2 - 1) * Math.PI;
				double co = Math.cos(lon), so = Math.sin(lon);
				Vec3 up = new Vec3(cl * co, sl, cl * so);
				Vec3 east = new Vec3(-so, 0, co);
				Vec3 north = new Vec3(-sl * co, cl, -sl * so);
				float hE = heights[y * w + (x + 1) % w], hW = heights[y * w + (x + w - 1) % w];
				float hN = heights[Math.max(0, y - 1) * w + x], hS = heights[Math.min(h - 1, y + 1) * w + x];
				double sx = wet[i] ? 0 : (hE - hW) / (2 * stepEast) * RELIEF, sy = wet[i] ? 0 : (hN - hS) / (2 * stepNorth) * RELIEF;
				Vec3 n = up.scale(1).add(east.scale(-sx)).add(north.scale(-sy)).normalize();

				double d0 = up.dot(sun), d = n.dot(sun);
				double day = smooth(-0.14, 0.16, d0);
				double lit = Math.max(0, d) * day;
				// Clouds: swirled bands, thicker at the equator and the poles' edge, thin over the dry belts.
				double cloud = 0, cloudLit = 0;
				if (look.cloudCover() > 0) {
					double swirl = look.cloudSwirl();
					double wx = up.x + swirl * 0.18 * clouds.getValue(up.x * 3 + 11, up.y * 3, up.z * 3);
					double wy = up.y + swirl * 0.18 * clouds.getValue(up.x * 3, up.y * 3 + 23, up.z * 3);
					double wz = up.z + swirl * 0.18 * clouds.getValue(up.x * 3, up.y * 3, up.z * 3 + 37);
					double density = fbm(clouds, wx, wy, wz, 4.5, 5) * 0.5 + 0.5;
					density *= 0.82 + 0.28 * Math.cos(lat * 6);
					double cover = look.cloudCover();
					cloud = smooth(1.02 - cover - 0.12, 1.02 - cover + 0.2, density);
					// Its shadow on the land, a little toward the sun's far side.
					Vec3 off = up.subtract(sun.scale(0.012)).normalize();
					double shadowDensity = (fbm(clouds, off.x + (wx - up.x), off.y + (wy - up.y), off.z + (wz - up.z), 4.5, 3) * 0.5 + 0.5)
							* (0.82 + 0.28 * Math.cos(lat * 6));
					double shadow = smooth(1.02 - cover - 0.12, 1.02 - cover + 0.2, shadowDensity);
					lit *= 1 - 0.45 * shadow;
					cloudLit = smooth(-0.1, 0.25, d0) * (0.75 + 0.25 * Math.max(0, d0)) * (0.82 + 0.18 * density);
				}

				int a = albedo[i];
				float r = PlanetLooks.r(a), gr = PlanetLooks.g(a), b = PlanetLooks.b(a);
				// Sunlight, reddened where it comes in low across the air at the terminator.
				double warm = air ? smooth(0.35, 0.0, d0) * day : 0;
				double lr = lit * (1 + 0.15 * warm), lg = lit * (1 - 0.15 * warm), lb = lit * (1 - 0.35 * warm);
				double ambient = 0.02;
				double fr = r * (ambient + lr * 1.05), fg = gr * (ambient + lg * 1.05), fb = b * (ambient + lb * 1.05);
				if (air) {
					// A little of the air's own colour over the lit land.
					double haze = 0.1 * look.atmosphereStrength() * day;
					fr = Mth.lerp(haze, fr, PlanetLooks.r(atmosphere) * lit);
					fg = Mth.lerp(haze, fg, PlanetLooks.g(atmosphere) * lit);
					fb = Mth.lerp(haze, fb, PlanetLooks.b(atmosphere) * lit);
				}
				// Glow: lava and crystal, seen most on the night side, through the clouds dimly.
				double shine = glow[i] * (1 - 0.7 * cloud) * (0.45 + 0.55 * (1 - day));
				if (shine > 0) {
					boolean crystal = planet.kind() == AstralPlanets.Kind.CRYSTAL;
					fr += shine * (crystal ? 0.75 : 1.0);
					fg += shine * (crystal ? 0.6 : 0.42);
					fb += shine * (crystal ? 1.0 : 0.12);
				}
				if (cloud > 0) {
					int cc = look.cloudColour();
					fr = Mth.lerp(cloud, fr, PlanetLooks.r(cc) * cloudLit);
					fg = Mth.lerp(cloud, fg, PlanetLooks.g(cc) * cloudLit);
					fb = Mth.lerp(cloud, fb, PlanetLooks.b(cc) * cloudLit);
				}
				image.setPixelRGBA(x, y, abgr(fr, fg, fb, 1));
			}
		});
		return image;
	}

	/** A ring strip, 512 by 4, inner edge at x 0: bands of the look's two colours, with gaps; alpha is how dense the ring is. */
	static NativeImage rings(AstralPlanets.Planet planet, PlanetLooks.Look look) {
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, 512, 4, false);
		SimplexNoise noise = new SimplexNoise(RandomSource.create(planet.seed() ^ 0x41A65L));
		RandomSource r = RandomSource.create(planet.seed() ^ 0x6A9L);
		double gapA = 0.3 + r.nextDouble() * 0.4, gapB = r.nextDouble();
		for (int x = 0; x < 512; x++) {
			double t = x / 511.0;
			double density = 0.55 + 0.25 * noise.getValue(t * 9, 0.5) + 0.2 * noise.getValue(t * 40, 3.5) + 0.12 * noise.getValue(t * 130, 7.5);
			density *= smooth(0, 0.06, t) * smooth(1, 0.9, t);
			if (Math.abs(t - gapA) < 0.025) density *= 0.05; // the great gap
			if (Math.abs(t - gapB) < 0.008) density *= 0.3;
			density = Mth.clamp(density, 0, 1);
			int colour = PlanetLooks.lerp(look.ringColour(), look.ringColourB(), noise.getValue(t * 18, 11.5) * 0.5 + 0.5);
			for (int y = 0; y < 4; y++) image.setPixelRGBA(x, y, abgr(PlanetLooks.r(colour), PlanetLooks.g(colour), PlanetLooks.b(colour), density * 0.9));
		}
		return image;
	}

	/**
	 * The sky behind everything, {@code width} by {@code width / 2} (equirectangular, as the sphere it's laid on): faint nebulae in
	 * three colours, a galactic band whitening along one great circle with dark dust lanes through it, and a haze of stars too
	 * faint to be told apart.
	 */
	static NativeImage nebula(long seed, int width) {
		int w = width, h = width / 2;
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, false);
		RandomSource r = RandomSource.create(seed ^ 0x4EB1L);
		SimplexNoise a = new SimplexNoise(r), b = new SimplexNoise(r), c = new SimplexNoise(r), dust = new SimplexNoise(r), speck = new SimplexNoise(r);
		Vec3 pole = galacticPole(seed);
		int[] hues = {0x8A3CFF, 0x22C8D8, 0xFF5A3A, 0x3A6CFF, 0xFF3CB8};
		int colourA = hues[r.nextInt(hues.length)], colourB = hues[r.nextInt(hues.length)], colourC = 0xFF8A3A;
		if (colourA == colourB) colourB = 0x22C8D8;
		for (int y = 0; y < h; y++) {
			double lat = (0.5 - (y + 0.5) / h) * Math.PI;
			for (int x = 0; x < w; x++) {
				double lon = ((x + 0.5) / w * 2 - 1) * Math.PI;
				double px = Math.cos(lat) * Math.cos(lon), py = Math.sin(lat), pz = Math.cos(lat) * Math.sin(lon);
				double band = Math.exp(-Mth.square((px * pole.x + py * pole.y + pz * pole.z) / 0.2));
				double na = smooth(0.1, 0.75, fbm(a, px, py, pz, 2.2, 6) * 0.5 + 0.5);
				double nb = smooth(0.15, 0.8, fbm(b, px + 3, py, pz, 3.1, 6) * 0.5 + 0.5);
				double nc = smooth(0.35, 0.85, fbm(c, px, py - 5, pz, 4.0, 5) * 0.5 + 0.5);
				double lanes = smooth(0.45, 0.8, fbm(dust, px, py, pz, 6.5, 5) * 0.5 + 0.5) * band;
				double fr = 0, fg = 0, fb = 0;
				double ka = na * na * (0.16 + 0.22 * band), kb = nb * nb * (0.12 + 0.2 * band), kc = nc * nc * nc * 0.22;
				fr += PlanetLooks.r(colourA) * ka + PlanetLooks.r(colourB) * kb + PlanetLooks.r(colourC) * kc;
				fg += PlanetLooks.g(colourA) * ka + PlanetLooks.g(colourB) * kb + PlanetLooks.g(colourC) * kc;
				fb += PlanetLooks.b(colourA) * ka + PlanetLooks.b(colourB) * kb + PlanetLooks.b(colourC) * kc;
				// The galactic band: a pale glow of countless stars, crossed by dark lanes of dust.
				double milk = band * (0.13 + 0.1 * (fbm(a, px * 2, py * 2, pz * 2, 5, 4) * 0.5 + 0.5));
				fr += milk * 0.95;
				fg += milk * 0.88;
				fb += milk * 0.8;
				double dim = 1 - 0.85 * lanes;
				fr *= dim;
				fg *= dim;
				fb *= dim;
				// Faint unresolved stars, thicker in the band.
				double s = speck.getValue(px * 400, py * 400, pz * 400);
				if (s > 0.62 - band * 0.12) {
					double k = (s - 0.62 + band * 0.12) * 1.6;
					fr += k;
					fg += k;
					fb += k * 1.05;
				}
				image.setPixelRGBA(x, y, abgr(fr, fg, fb, 1));
			}
		}
		return image;
	}

	/** The axis the galaxy's band lies square to (the nebula's band and the star field's crowding share it). */
	static Vec3 galacticPole(long seed) {
		RandomSource r = RandomSource.create(seed ^ 0x9A1AC7L);
		return new Vec3(r.nextDouble() - 0.5, 0.5 + r.nextDouble(), r.nextDouble() - 0.5).normalize();
	}

	/** A star's face, 256 square: white-hot, darkening and reddening toward its limb, mottled with granulation; transparent outside. */
	static NativeImage starFace() {
		int size = 256;
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
		SimplexNoise noise = new SimplexNoise(RandomSource.create(0x57A2L));
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double u = (x + 0.5) / size * 2 - 1, v = (y + 0.5) / size * 2 - 1;
				double r = Math.sqrt(u * u + v * v);
				if (r > 1) {
					image.setPixelRGBA(x, y, 0);
					continue;
				}
				double mu = Math.sqrt(1 - r * r);
				double limb = 0.4 + 0.6 * Math.pow(mu, 0.6);
				double gran = 0.9 + 0.1 * noise.getValue(u * 22, v * 22) + 0.05 * noise.getValue(u * 60, v * 60);
				double k = limb * gran;
				image.setPixelRGBA(x, y, abgr(k, k * (0.9 + 0.1 * mu), k * (0.8 + 0.2 * mu), smooth(1, 0.97, r)));
			}
		}
		return image;
	}

	/** A soft glow, 128 square: bright at its heart, falling away to nothing at its edge (for coronas, glare and far stars). */
	static NativeImage glow() {
		int size = 128;
		NativeImage image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double u = (x + 0.5) / size * 2 - 1, v = (y + 0.5) / size * 2 - 1;
				double r = Math.sqrt(u * u + v * v);
				double k = r >= 1 ? 0 : Math.pow(1 - r, 2.2) * 0.85 + Math.exp(-r * r * 60) * 0.15;
				image.setPixelRGBA(x, y, abgr(1, 1, 1, k));
			}
		}
		return image;
	}

	// --- Helpers ---

	/** A soft blur over a map of colours, 1-2-1 each way (wrapping round the equator, held at the poles). */
	private static void soften(int[] rgb, int w, int h) {
		int[] pass = new int[rgb.length];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				pass[y * w + x] = mix3(rgb[y * w + (x + w - 1) % w], rgb[y * w + x], rgb[y * w + (x + 1) % w]);
			}
		}
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				rgb[y * w + x] = mix3(pass[Math.max(0, y - 1) * w + x], pass[y * w + x], pass[Math.min(h - 1, y + 1) * w + x]);
			}
		}
	}

	private static int mix3(int a, int b, int c) {
		int r = ((a >> 16 & 255) + 2 * (b >> 16 & 255) + (c >> 16 & 255)) >> 2;
		int g = ((a >> 8 & 255) + 2 * (b >> 8 & 255) + (c >> 8 & 255)) >> 2;
		int bl = ((a & 255) + 2 * (b & 255) + (c & 255)) >> 2;
		return r << 16 | g << 8 | bl;
	}

	private static double fbm(SimplexNoise noise, double x, double y, double z, double freq, int octaves) {
		double sum = 0, amp = 1, norm = 0;
		for (int i = 0; i < octaves; i++) {
			sum += amp * noise.getValue(x * freq + i * 5.3, y * freq - i * 3.1, z * freq + i * 7.7);
			norm += amp;
			amp *= 0.5;
			freq *= 2.05;
		}
		return sum / norm;
	}

	private static double smooth(double from, double to, double x) {
		double t = Mth.clamp((x - from) / (to - from), 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static int scale(int rgb, double k) {
		int r = Mth.clamp((int) ((rgb >> 16 & 255) * k), 0, 255), g = Mth.clamp((int) ((rgb >> 8 & 255) * k), 0, 255), b = Mth.clamp((int) ((rgb & 255) * k), 0, 255);
		return r << 16 | g << 8 | b;
	}

	/** As NativeImage stores it: alpha, blue, green, red (each 0-1, clamped). */
	static int abgr(double r, double g, double b, double a) {
		int ir = Mth.clamp((int) Math.round(r * 255), 0, 255), ig = Mth.clamp((int) Math.round(g * 255), 0, 255);
		int ib = Mth.clamp((int) Math.round(b * 255), 0, 255), ia = Mth.clamp((int) Math.round(a * 255), 0, 255);
		return ia << 24 | ib << 16 | ig << 8 | ir;
	}

	private PlanetPainter() {}
}
