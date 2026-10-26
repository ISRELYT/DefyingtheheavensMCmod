package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.HeavenDemonSpells;
import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The great spells' shapes drawn by hand whole (SpellConstruct Form DRAWN), each its own way, where a model of GeckoLib's looked wrong (the
 * user's asks, 2026-10-25: of the Heaven-Demon Eclipse, "still looks kinda retarded" and "the beam coming out of the moons as its larger and
 * doesn't line up and is just poor"; of Ten Thousand Souls Ascend and the two Impermanences, "try a diff style").
 */
final class SpellDrawn {
	static final ResourceLocation DEMON_MOON = DefyingTheHeavens.id("textures/spell/demon_moon.png"), CORONA = DefyingTheHeavens.id("textures/spell/corona.png");

	static void render(SpellConstruct construct, float partial, PoseStack pose, MultiBufferSource source, int light, Quaternionf camera, Vec3 cam) {
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			switch (construct.kind()) {
				case ECLIPSE -> eclipse(construct, partial, pose, buffers, camera, cam);
				case SOUL_VORTEX -> SoulVortex.render(construct, partial, pose, buffers, camera, cam);
				case WHITE_IMPERMANENCE_FIGURE, BLACK_IMPERMANENCE_FIGURE -> ImpermanenceFigures.render(construct, partial, pose, buffers, light, cam);
				case PEARL -> pearl(construct, partial, pose, buffers, camera, cam);
				case VASE_VORTEX -> vortex(construct, partial, pose, buffers, camera, cam);
				case SUN -> sun(construct, partial, pose, buffers, camera, cam);
				default -> {}
			}
		} finally {
			SpellBuffers.end();
		}
	}

	// --- Heaven-Demon Eclipse ---

	private static final float[] SUN_WHITE = {1.0f, 0.97f, 0.85f}, SUN_GOLD = {1.0f, 0.78f, 0.3f}, CRIMSON = {0.86f, 0.08f, 0.15f},
			EMBER = {1.0f, 0.36f, 0.16f}, GOLD = {1.0f, 0.8f, 0.25f}, DARK = {0.05f, 0.0f, 0.02f}, WHITE = {1, 1, 1};

	/**
	 * Heaven-Demon Eclipse (HeavenDemonSpells#Eclipse): heaven's sun blazing in the sky and the demon's moon sliding across it from the side
	 * ({@link HeavenDemonSpells#ECLIPSE_MEETS} ticks), until it covers it: a flash where the last of the sun shows at its rim, then the corona
	 * blazing out round the black moon, heaven's gold flames and the demon's crimson licking round its rim; and from the moon's face a beam of
	 * darkness pours straight down to the ground, as wide as its harm reaches (its size) and as long as the drop below it (its value),
	 * narrower than the moon whose middle it comes out of, a swirl of darkness edged with crimson and gold, flaring as it pulses (every
	 * {@link HeavenDemonSpells#ECLIPSE_EVERY} ticks), a ring of crimson light burning on the ground where it falls. Sun and moon are drawn
	 * facing whoever looks, so from below as from the side they are discs.
	 */
	private static void eclipse(SpellConstruct e, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = e.tickCount + partial;
		float shown = SpellRender.fade(age, e.life(), 6, SpellConstruct.FADE);
		if (shown <= 0) return;
		float beamR = Math.max(1, e.size()), drop = Math.max(4, e.value());
		float moonR = beamR * 1.35f, sunR = moonR * 0.96f;
		float meets = HeavenDemonSpells.ECLIPSE_MEETS;
		float closing = SpellRender.ease(age / meets);
		float met = Mth.clamp((age - meets + 4) / 8, 0, 1);
		// Since its last pulse (0 to 1 over a few ticks; 1 when none is under way): the beam's flare.
		float sincePulse = age < meets ? 99 : (age - meets) % HeavenDemonSpells.ECLIPSE_EVERY;
		float flare = age >= meets && age < meets + HeavenDemonSpells.ECLIPSE_EVERY * HeavenDemonSpells.ECLIPSE_PULSES.length ? Math.max(0, 1 - sincePulse / 7) : 0;
		// Across the face of the sky as whoever looks sees it (the camera's own left, as Camera keeps it), and the way toward them.
		Vector3f right = camera.transform(new Vector3f(1, 0, 0)), toward = camera.transform(new Vector3f(0, 0, 1)).negate();

		// The sun: its glow wide about it, dimming as it is covered; its blazing face.
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, sunR * 7 * (1 - 0.45f * met), 0, SUN_GOLD, 0.55f * shown * (1 - 0.5f * met), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, sunR * 3.2f, 0, SUN_WHITE, 0.8f * shown * (1 - 0.6f * met), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		face(buffers, pose, sunR * 1.05f, age, 0.25f, shown);
		// Heaven's flames round the sun's rim (and, once they meet, round the moon's).
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		rimFlames(flames, pose, camera, right, toward, Vec3.ZERO, sunR * 1.02f, sunR * 0.55f, 14, age, GOLD, shown * (1 - 0.3f * met), 0);

		// The corona, blazing out as they meet, turning slowly; the flash of the sun's last light at the moon's rim.
		if (met > 0) {
			float swell = 0.85f + 0.15f * SpellRender.easeOut(met) + 0.08f * flare;
			VertexConsumer corona = buffers.getBuffer(SpellRender.glow(CORONA));
			SpellRender.billboard(corona, pose, camera, 0, 0, 0, moonR * 4.6f * swell, age * 0.004f, SUN_WHITE, 0.95f * met * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			SpellRender.billboard(corona, pose, camera, 0, 0, 0, moonR * 5.6f * swell, -age * 0.003f + 0.4f, SUN_GOLD, 0.45f * met * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			float diamond = Math.max(0, 1 - Math.abs(age - meets) / 6);
			if (diamond > 0) {
				Vector3f at = new Vector3f(right).mul(moonR * 0.72f).add(camera.transform(new Vector3f(0, moonR * 0.68f, 0)));
				SpellRender.billboard(glow, pose, camera, at.x, at.y, at.z, moonR * 2.6f * diamond, 0, WHITE, diamond * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				VertexConsumer star = buffers.getBuffer(SpellRender.glow(SpellRender.STAR));
				SpellRender.billboard(star, pose, camera, at.x, at.y, at.z, moonR * 3.4f * diamond, age * 0.05f, WHITE, diamond * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}

		// The beam of darkness, poured down from the moon's face once they have met, reaching the ground in a few ticks: drawn before the moon,
		// so it is seen coming out from behind it, from its middle.
		float pour = Mth.clamp((age - meets + 2) / 7, 0, 1);
		if (pour > 0) beam(pose, buffers, camera, age, beamR * (1 + 0.28f * flare), drop, pour, flare, shown);

		// The demon's moon, sliding in across the sun from the side until it covers it, a little before it (toward whoever looks).
		float off = (1 - closing) * moonR * 2.6f;
		Vector3f moonAt = new Vector3f(right).mul(off).add(new Vector3f(toward).mul(moonR * 0.08f));
		VertexConsumer moon = buffers.getBuffer(SpellRender.clear(DEMON_MOON));
		SpellRender.billboard(moon, pose, camera, moonAt.x, moonAt.y, moonAt.z, moonR * 2, 0.3f, WHITE, shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		// The demon's crimson fire round its rim, and its glow.
		rimFlames(flames, pose, camera, right, toward, new Vec3(moonAt.x, moonAt.y, moonAt.z), moonR * 1.0f, moonR * (0.4f + 0.25f * met + 0.2f * flare), 18, age + 7,
				CRIMSON, shown * (0.5f + 0.5f * met), 1);
		SpellRender.billboard(glow, pose, camera, moonAt.x, moonAt.y, moonAt.z, moonR * 2.9f, 0, CRIMSON, 0.35f * shown * (0.4f + 0.6f * met), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
	}

	/**
	 * A sun's blazing face, {@code radius} round, its surface boiling ({@code rate} of its frames a tick): a ball of it, as Solar Field's sun is
	 * (SpellFieldRenderer), round from every side and blended rather than added, so it is seen by day (laid flat on a square facing whoever
	 * looks, its texture, square itself, drew a square, found checking toast's push in game, 2026-10-26).
	 */
	private static void face(MultiBufferSource buffers, PoseStack pose, float radius, float age, float rate, float shown) {
		float[] fv = SpellRender.frame(age, rate, SpellRender.SUN_FRAMES);
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.SUN)), pose.last(), radius, 12, 18, SUN_WHITE, shown, 1, fv[1] - fv[0], age * 0.004f, fv[0],
				SpellRender.FULL_BRIGHT, (polar, azimuth) -> 1 + 0.02f * Mth.sin((float) (azimuth * 6 + age * 0.2)));
	}

	/** Tongues of flame standing out round a disc's rim in the sky ({@code radius} out from {@code at}), {@code length} long, {@code count} of them. */
	private static void rimFlames(VertexConsumer vc, PoseStack pose, Quaternionf camera, Vector3f right, Vector3f toward, Vec3 at, float radius, float length,
								  int count, float age, float[] rgb, float alpha, int salt) {
		if (alpha <= 0) return;
		Vector3f up = camera.transform(new Vector3f(0, 1, 0));
		for (int i = 0; i < count; i++) {
			double a = Math.PI * 2 * i / count + Math.sin(age * 0.02 + i) * 0.08 + salt * 0.3;
			float flick = 0.7f + 0.3f * Mth.sin(age * 0.35f + i * 2.3f + salt);
			float tall = length * flick * (0.75f + 0.5f * SpellRender.hash(i * 13L + salt));
			Vector3f out = new Vector3f(right).mul((float) Math.cos(a)).add(new Vector3f(up).mul((float) Math.sin(a)));
			Vector3f base = new Vector3f(out).mul(radius + tall * 0.42f).add((float) at.x, (float) at.y, (float) at.z);
			float roll = (float) a - (float) Math.PI / 2;
			float[] fv = SpellRender.frame(age + i * 3, 0.5f, SpellRender.FLAME_FRAMES);
			SpellRender.billboard(vc, pose, camera, base.x, base.y, base.z, tall * 0.55f, tall, roll, i % 3 == 0 ? (salt == 0 ? SUN_WHITE : EMBER) : rgb, alpha * 0.9f,
					0, fv[0], 1, fv[1], SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * The beam: from the moon's middle straight down {@code drop} (its first {@code pour} of the way), {@code radius} round: a core of swirling
	 * darkness pouring down, crimson light streaming down within its edge and gold at its very rim, a ring of crimson fire on the ground where
	 * it lands and darkness welling up about its foot; flaring with each pulse ({@code flare}).
	 */
	private static void beam(PoseStack pose, MultiBufferSource buffers, Quaternionf camera, float age, float radius, float drop, float pour, float flare, float shown) {
		float length = drop * SpellRender.easeOut(pour);
		pose.pushPose();
		pose.translate(0, -length, 0);
		PoseStack.Pose p = pose.last();
		float wrap = Math.max(1, length / (radius * 6));
		// Its core: darkness, pouring down (seen through only a little).
		SpellRender.cylinder(buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE)), p, length, h -> radius * 0.82f, 12, 24, DARK, 0.82f * shown, 2, wrap,
				0, age * 0.06f, 0.6f, SpellRender.FULL_BRIGHT, null);
		SpellRender.cylinder(buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE)), p, length, h -> radius * 0.62f, 12, 20, DARK, 0.9f * shown, 1.5f, wrap * 1.3f,
				0.3f, age * 0.09f, -0.9f, SpellRender.FULL_BRIGHT, null);
		// Crimson light streaming down within its edge, gold at its rim.
		VertexConsumer streaks = buffers.getBuffer(SpellRender.glow(SpellRender.BEAM));
		SpellRender.cylinder(streaks, p, length, h -> radius * 0.93f, 10, 28, CRIMSON, (0.55f + 0.45f * flare) * shown, 3, wrap * 2, 0, age * 0.12f, 0.4f,
				SpellRender.FULL_BRIGHT, null);
		SpellRender.cylinder(streaks, p, length, h -> radius * 1.0f, 10, 28, GOLD, (0.25f + 0.5f * flare) * shown, 2, wrap * 1.5f, 0.5f, age * 0.16f, -0.3f,
				SpellRender.FULL_BRIGHT, null);
		SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), p, length, h -> radius * (1.25f + 0.3f * flare), 8, 24, CRIMSON,
				(0.12f + 0.25f * flare) * shown, 1, 1, 0, 0, 0, SpellRender.FULL_BRIGHT, null);
		// Where it lands: a ring of crimson fire, darkness welling up about its foot.
		if (pour >= 1) {
			VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
			float swell = 1 + 0.4f * flare + 0.05f * Mth.sin(age * 0.4f);
			SpellRender.disc(ring, p, radius * 1.35f * swell, 0.15f, age * 0.03f, CRIMSON, 0.9f * shown, SpellRender.FULL_BRIGHT);
			SpellRender.disc(ring, p, radius * 1.05f * swell, 0.2f, -age * 0.05f, EMBER, 0.7f * shown, SpellRender.FULL_BRIGHT);
			if (flare > 0) SpellRender.disc(ring, p, radius * (1.2f + 3.2f * (1 - flare)), 0.25f, 0, GOLD, flare * shown, SpellRender.FULL_BRIGHT);
			VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
			for (int i = 0; i < 10; i++) {
				float t = (age * 0.02f + i / 10f) % 1;
				double a = i * 2.4 + age * 0.01;
				float r = radius * (0.6f + 0.7f * t);
				SpellRender.billboard(smoke, pose, camera, (float) Math.cos(a) * r, 0.5f + t * radius * 1.6f, (float) Math.sin(a) * r, radius * (0.6f + t), i, DARK,
						0.55f * (1 - t) * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}
		pose.popPose();
	}

	// --- Nine Suns Descend ---

	/**
	 * A sun come down out of the sky (SUN, its size its round): its blazing face (the sun's own surface, turning), its light wide about it,
	 * flames leaping round its rim, its fire streaming out behind it the way it came; landed ({@code impact}), it flares and is gone.
	 */
	private static void sun(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = c.tickCount + partial;
		float shown = SpellRender.fade(age, c.life(), 4, 4);
		float r = Math.max(0.5f, c.size());
		float flare = 0;
		if ("impact".equals(c.phase())) {
			float t = c.phaseAge(partial);
			flare = Math.max(0, 1 - t / 8);
			shown *= flare;
		}
		if (shown <= 0) return;
		Vector3f right = camera.transform(new Vector3f(1, 0, 0)), toward = camera.transform(new Vector3f(0, 0, 1)).negate();
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * (6 + 6 * flare), 0, SUN_GOLD, 0.55f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 3.2f, 0, SUN_WHITE, 0.85f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		face(buffers, pose, r * 1.05f, age, 0.3f, shown);
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		rimFlames(flames, pose, camera, right, toward, Vec3.ZERO, r, r * 0.7f, 12, age, GOLD, shown, 0);
		// Its fire streaming out behind it, back the way it came.
		Vec3 way = c.getPosition(partial).subtract(c.getPosition(Math.max(0, partial - 1)));
		Vec3 back = way.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : way.normalize().scale(-1);
		for (int i = 0; i < 16; i++) {
			float t = (age * 0.1f + i / 16f) % 1;
			Vec3 p = back.scale(r * (0.8 + t * 6)).add((SpellRender.hash(i * 7L + c.getId()) - 0.5) * r * t, (SpellRender.hash(i * 9L) - 0.5) * r * t,
					(SpellRender.hash(i * 11L) - 0.5) * r * t);
			float[] ff = SpellRender.frame(age + i * 3, 0.5f, SpellRender.FLAME_FRAMES);
			SpellRender.billboard(flames, pose, camera, (float) p.x, (float) p.y, (float) p.z, r * (1.8f - t), i, i % 2 == 0 ? GOLD : EMBER, 0.85f * (1 - t) * shown,
					0, ff[0], 1, ff[1], SpellRender.FULL_BRIGHT);
		}
	}

	// --- Twenty-Four Sea-Fixing Pearls ---

	/** Each pearl's way behind it (world places, newest first), for its stream of water. */
	private static final java.util.Map<Integer, java.util.ArrayDeque<Vec3>> WAKES = new java.util.HashMap<>();
	private static final int WAKE = 14;
	private static final float[] PEARL = {0.97f, 0.97f, 1.0f}, SEA = {0.4f, 0.66f, 0.95f}, FOAM = {0.9f, 0.96f, 1.0f};

	/**
	 * A Sea-Fixing Pearl (PEARL, its size its round): a lustrous white pearl, its sheen shifting through the colours of nacre round its rim, the
	 * sea turning in a halo about it; flying, its water streams out behind it the way it came, and drips from it.
	 */
	private static void pearl(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = c.tickCount + partial;
		float shown = SpellRender.fade(age, c.life(), 6, 4);
		if (shown <= 0) return;
		float r = Math.max(0.2f, c.size());
		// Its sheen: the colours of nacre, turning.
		float hue = (age * 0.012f + c.getId() * 0.13f) % 1;
		float[] nacre = hsv(hue, 0.35f, 1.0f);
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 6, 0, SEA, 0.35f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		VertexConsumer sea = buffers.getBuffer(SpellRender.clear(SpellRender.WATER));
		SpellRender.billboard(sea, pose, camera, 0, 0, 0, r * 3.4f, age * 0.08f, SEA, 0.7f * shown, 0, fv[0], 1, fv[1], SpellRender.FULL_BRIGHT);
		SpellRender.billboard(sea, pose, camera, 0, 0, 0, r * 2.8f, -age * 0.11f + 1, FOAM, 0.4f * shown, 0, fv[0], 1, fv[1], SpellRender.FULL_BRIGHT);
		VertexConsumer body = buffers.getBuffer(SpellRender.clear(SpellRender.GLOW));
		SpellRender.billboard(body, pose, camera, 0, 0, 0, r * 2.2f, 0, PEARL, shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 1.4f, 0, nacre, 0.9f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
		SpellRender.billboard(ring, pose, camera, 0, 0, 0, r * 2.3f, age * 0.03f, nacre, 0.7f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		Vector3f glint = camera.transform(new Vector3f(-r * 0.35f, r * 0.35f, 0));
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.STAR)), pose, camera, glint.x, glint.y, glint.z, r * 1.1f, age * 0.05f, PEARL, 0.9f * shown,
				0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		// Its wake: the way it came, as a stream of water behind it.
		Vec3 here = c.getPosition(partial);
		java.util.ArrayDeque<Vec3> wake = WAKES.computeIfAbsent(c.getId(), id -> new java.util.ArrayDeque<>());
		if (wake.isEmpty() || wake.peekFirst().distanceToSqr(here) > 0.09) {
			wake.addFirst(here);
			while (wake.size() > WAKE) wake.removeLast();
		}
		if (wake.size() >= 3) {
			List<Vec3> points = new ArrayList<>();
			for (Vec3 p : wake) points.add(p.subtract(here));
			int n = points.size();
			float[] widths = new float[n], alphas = new float[n];
			for (int i = 0; i < n; i++) {
				float f = i / (float) (n - 1);
				widths[i] = r * 1.6f * (1 - f * 0.8f);
				alphas[i] = 0.75f * (1 - f) * shown;
			}
			SpellRender.ribbon(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), points, widths, cam, SEA, alphas, 0.3f, -age * 0.08f,
					SpellRender.FULL_BRIGHT);
			for (int i = 0; i < n; i++) widths[i] *= 0.45f;
			SpellRender.ribbon(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), points, widths, cam, FOAM, alphas, 0, 0, SpellRender.FULL_BRIGHT);
		}
		WAKES.keySet().removeIf(id -> net.minecraft.client.Minecraft.getInstance().level == null
				|| !(net.minecraft.client.Minecraft.getInstance().level.getEntity(id) instanceof SpellConstruct));
	}

	/** A colour of {@code h} (0 to 1 round), {@code s}, {@code v}. */
	private static float[] hsv(float h, float s, float v) {
		int rgb = java.awt.Color.HSBtoRGB(h, s, v);
		return new float[] {(rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f};
	}

	// --- Ocean-Sealing Vase ---

	/**
	 * The Ocean-Sealing Vase's drinking (VASE_VORTEX: its size the whirlpool's round, its value how high the vase's mouth stands over it): a
	 * whirlpool turning on the land, foam round its rim; and out of it the water drawn up in eight twisting streams that wind in toward one
	 * another as they climb and pour into the vase's mouth, drops flung off them, a swirl of it turning in the mouth itself.
	 */
	private static void vortex(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = c.tickCount + partial;
		float shown = SpellRender.fade(age, c.life(), 12, SpellConstruct.FADE);
		if (shown <= 0) return;
		float radius = Math.max(3, c.size()), high = Math.max(4, c.value());
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		VertexConsumer water = buffers.getBuffer(SpellRender.clear(SpellRender.WATER));
		// The whirlpool, turning ever faster in toward its heart.
		for (int k = 0; k < 3; k++) {
			round(water, pose.last(), radius * (0.85f - 0.25f * k) * shown, 0.12f + 0.02f * k, age * (0.04f + 0.05f * k), SEA, 0.7f * shown, fv);
		}
		VertexConsumer ripple = buffers.getBuffer(SpellRender.clear(SpellRender.RIPPLE));
		for (int k = 0; k < 3; k++) {
			float t = (age * 0.03f + k / 3f) % 1;
			SpellRender.disc(ripple, pose.last(), radius * (1 - 0.8f * t), 0.2f, age * 0.05f, FOAM, 0.6f * t * shown, SpellRender.FULL_BRIGHT);
		}
		// The streams, each winding up from the whirlpool's rim into the mouth.
		int streams = 8, steps = 24;
		for (int s = 0; s < streams; s++) {
			List<Vec3> points = new ArrayList<>();
			float[] widths = new float[steps + 1], alphas = new float[steps + 1];
			double a0 = Math.PI * 2 * s / streams + age * 0.045;
			for (int i = 0; i <= steps; i++) {
				float f = i / (float) steps;
				double a = a0 + f * Math.PI * 1.6;
				float r = radius * 0.8f * (1 - f) * (1 - f) + radius * 0.06f;
				points.add(new Vec3(Math.cos(a) * r, f * high, Math.sin(a) * r));
				widths[i] = radius * 0.16f * (1 - 0.65f * f) + 0.4f;
				alphas[i] = 0.8f * shown * Math.min(1, f * 6 + 0.2f);
			}
			SpellRender.ribbon(water, pose.last(), points, widths, cam, SEA, alphas, 0.15f, -age * 0.12f + s * 0.3f, SpellRender.FULL_BRIGHT);
			for (int i = 0; i <= steps; i++) widths[i] *= 0.35f;
			SpellRender.ribbon(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), points, widths, cam, FOAM, alphas, 0, 0, SpellRender.FULL_BRIGHT);
		}
		// Drops flung off the streams as they climb.
		VertexConsumer drops = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
		for (int i = 0; i < 60; i++) {
			float f = (age * 0.02f + SpellRender.hash(i * 7L + c.getId())) % 1;
			double a = SpellRender.hash(i * 3L) * Math.PI * 2 + age * 0.045 + f * Math.PI * 1.6;
			float r = (radius * 0.8f * (1 - f) * (1 - f) + radius * 0.06f) * (1.1f + 0.3f * SpellRender.hash(i * 5L));
			SpellRender.billboard(drops, pose, camera, (float) Math.cos(a) * r, f * high, (float) Math.sin(a) * r, 0.35f, 0, FOAM, 0.9f * (1 - f) * shown, 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
		// The swirl in the mouth.
		pose.pushPose();
		pose.translate(0, high, 0);
		round(water, pose.last(), radius * 0.2f, 0, age * 0.25f, SEA, 0.85f * shown, fv);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), radius * 0.26f, 0.05f, -age * 0.2f, FOAM, 0.6f * shown, SpellRender.FULL_BRIGHT);
		pose.popPose();
	}

	/**
	 * A round disc of {@code radius} lying on the pose's ground, frame {@code fv} of a square texture across it, turned {@code spin} radians, cut
	 * round and fading a little toward its rim (SpellRender#disc lays the whole texture on a square: the Vase's whirlpool was a square sheet of
	 * water, found checking toast's push in game, 2026-10-26).
	 */
	private static void round(VertexConsumer vc, PoseStack.Pose pose, float radius, float y, float spin, float[] rgb, float alpha, float[] fv) {
		int segments = 36;
		float cos = Mth.cos(spin), sin = Mth.sin(spin), middle = Mth.lerp(0.5f, fv[0], fv[1]);
		for (int i = 0; i < segments; i++) {
			double a0 = Math.PI * 2 * i / segments, a1 = Math.PI * 2 * (i + 1) / segments;
			float x0 = (float) Math.cos(a0) * radius, z0 = (float) Math.sin(a0) * radius, x1 = (float) Math.cos(a1) * radius, z1 = (float) Math.sin(a1) * radius;
			SpellRender.vertex(vc, pose, 0, y, 0, rgb[0], rgb[1], rgb[2], alpha, 0.5f, middle, SpellRender.FULL_BRIGHT, 0, 1, 0);
			rim(vc, pose, x1, y, z1, radius, cos, sin, rgb, alpha * 0.55f, fv);
			rim(vc, pose, x1, y, z1, radius, cos, sin, rgb, alpha * 0.55f, fv);
			rim(vc, pose, x0, y, z0, radius, cos, sin, rgb, alpha * 0.55f, fv);
		}
	}

	/** One of {@link #round}'s rim corners, the texture under it turned with the disc. */
	private static void rim(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float radius, float cos, float sin, float[] rgb, float alpha, float[] fv) {
		float u = 0.5f + (x * cos - z * sin) / (2 * radius), v = 0.5f + (x * sin + z * cos) / (2 * radius);
		SpellRender.vertex(vc, pose, x, y, z, rgb[0], rgb[1], rgb[2], alpha, u, Mth.lerp(v, fv[0], fv[1]), SpellRender.FULL_BRIGHT, 0, 1, 0);
	}

	private SpellDrawn() {}
}
