package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AstralPlanets;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * How each planet looks from space beyond its land (PlanetPainter paints the land itself from PlanetTerrain): its clouds (how much
 * of it they cover, their colour), its atmosphere's glow at the rim, and its rings. By kind, then varied by the planet's own seed
 * so no two of a kind wear quite the same sky.
 */
final class PlanetLooks {
	/** What one planet's sky looks like: cloud cover (0 none), cloud colour, atmosphere colour and thickness (0: none), rings. */
	record Look(double cloudCover, int cloudColour, double cloudSwirl, int atmosphere, float atmosphereThickness, float atmosphereStrength,
				int ringColour, int ringColourB, float ringInner, float ringOuter, float ringTiltX, float ringTiltZ) {}

	static Look of(AstralPlanets.Planet planet) {
		RandomSource r = RandomSource.create(planet.seed() ^ 0x10C5L);
		double cover;
		int cloud = 0xFFFFFF, atmosphere;
		float thickness = 0.06f, strength = 1.0f;
		switch (planet.kind()) {
			case OCEAN -> { cover = 0.55 + r.nextDouble() * 0.15; atmosphere = 0x6AA6FF; }
			case JUNGLE, SWAMP -> { cover = 0.55 + r.nextDouble() * 0.15; atmosphere = 0x7AC8D8; }
			case VERDANT, TEMPERATE, TAIGA -> { cover = 0.4 + r.nextDouble() * 0.15; atmosphere = 0x6FA8FF; }
			case BLOSSOM -> { cover = 0.35 + r.nextDouble() * 0.15; atmosphere = 0xB8A8FF; }
			case MUSHROOM -> { cover = 0.45 + r.nextDouble() * 0.15; atmosphere = 0xD69CE8; cloud = 0xF4E8F8; }
			case DESERT, SAVANNA -> { cover = 0.1 + r.nextDouble() * 0.12; atmosphere = 0xFFC88A; cloud = 0xFFF4E4; }
			case MESA -> { cover = 0.08 + r.nextDouble() * 0.1; atmosphere = 0xFFA878; cloud = 0xFFEEDC; }
			case FROZEN -> { cover = 0.3 + r.nextDouble() * 0.15; atmosphere = 0xA8E4FF; }
			case VOLCANIC -> { cover = 0.25 + r.nextDouble() * 0.15; atmosphere = 0xFF7038; cloud = 0x5E544E; strength = 0.8f; }
			case ASHEN -> { cover = 0.15 + r.nextDouble() * 0.12; atmosphere = 0xC8BCA8; cloud = 0xB8AC9C; thickness = 0.045f; strength = 0.6f; }
			case CRYSTAL -> { cover = 0.18 + r.nextDouble() * 0.12; atmosphere = 0xB48CFF; cloud = 0xEADCFF; }
			default -> { cover = 0; atmosphere = 0; thickness = 0; strength = 0; } // BARREN: no air at all
		}
		if (planet.parent() >= 0 && planet.kind() != AstralPlanets.Kind.BARREN) {
			// A moon holds only a thin air, if any.
			cover *= 0.4;
			thickness *= 0.6f;
			strength *= 0.6f;
		}
		// The planet's own shade of its kind's sky.
		atmosphere = shift(atmosphere, (r.nextFloat() - 0.5f) * 0.12f);
		int ring = 0, ringB = 0;
		float inner = 0, outer = 0;
		if (planet.rings()) {
			int[][] palettes = {{0xE8DCC0, 0xA89474}, {0xD8E4EC, 0x8898A8}, {0xC8A880, 0x7A5E44}, {0xE8D0E8, 0x9A80A8}, {0xF0E8D8, 0xB8A890}};
			int[] palette = palettes[r.nextInt(palettes.length)];
			ring = palette[0];
			ringB = palette[1];
			inner = 1.35f + r.nextFloat() * 0.3f;
			outer = inner + 0.6f + r.nextFloat() * 0.8f;
		}
		return new Look(cover, cloud, 0.6 + r.nextDouble() * 1.2, atmosphere, thickness, strength, ring, ringB, inner, outer,
				(r.nextFloat() - 0.5f) * 1.1f, (r.nextFloat() - 0.5f) * 1.1f);
	}

	/** {@code rgb} turned round the colour wheel a little (by {@code by} of a turn), keeping its brightness. */
	static int shift(int rgb, float by) {
		float[] hsb = java.awt.Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
		return java.awt.Color.HSBtoRGB(hsb[0] + by, hsb[1], hsb[2]) & 0xFFFFFF;
	}

	static float r(int rgb) { return (rgb >> 16 & 255) / 255f; }

	static float g(int rgb) { return (rgb >> 8 & 255) / 255f; }

	static float b(int rgb) { return (rgb & 255) / 255f; }

	static int lerp(int a, int b, double t) {
		t = Mth.clamp(t, 0, 1);
		int r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
		int g = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
		int bl = (int) Mth.lerp(t, a & 255, b & 255);
		return r << 16 | g << 8 | bl;
	}

	private PlanetLooks() {}
}
