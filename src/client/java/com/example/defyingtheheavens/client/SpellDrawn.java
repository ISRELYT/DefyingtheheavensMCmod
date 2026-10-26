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
				case VOID_SUN -> voidSun(construct, partial, pose, buffers, camera, cam);
				case GLACIER_SURGE -> glacier(construct, partial, pose, buffers, camera, cam);
				case PRIMORDIAL_STORM -> primordialStorm(construct, partial, pose, buffers, camera, cam);
				case XIRANG_BLOOM -> xirang(construct, partial, pose, buffers, camera, light);
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
		face(buffers, pose, sunR * 1.05f, age, 0.25f, shown, false); // see-through: the moon passes before it, nearer than its face
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
	 * looks, its texture, square itself, drew a square, found checking toast's push in game, 2026-10-26). {@code solid}, it hides what is behind
	 * and in it (SpellRender#core), a skin of fire over it turning the other way; else see-through (the eclipse's, that its moon passes before).
	 */
	private static void face(MultiBufferSource buffers, PoseStack pose, float radius, float age, float rate, float shown, boolean solid) {
		float[] fv = SpellRender.frame(age, rate, SpellRender.SUN_FRAMES);
		SpellRender.Wobble boil = (polar, azimuth) -> 1 + 0.02f * Mth.sin((float) (azimuth * 6 + age * 0.2));
		if (!solid) {
			SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.SUN)), pose.last(), radius, 12, 18, SUN_WHITE, shown, 1, fv[1] - fv[0], age * 0.004f, fv[0],
					SpellRender.FULL_BRIGHT, boil);
			return;
		}
		// Solid, it can't fade: it shrinks away instead.
		float r = radius * Mth.clamp(shown * 1.25f, 0, 1);
		if (r < 0.05f) return;
		SpellRender.sphere(buffers.getBuffer(SpellRender.core(SpellRender.SUN)), pose.last(), r, 14, 22, SUN_WHITE, 1, 1, fv[1] - fv[0], age * 0.004f, fv[0],
				SpellRender.FULL_BRIGHT, boil);
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.SUN)), pose.last(), r * 1.045f, 12, 18, SUN_GOLD, 0.5f * shown, 1.3f, fv[1] - fv[0],
				-age * 0.005f, fv[0], SpellRender.FULL_BRIGHT, null);
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
		face(buffers, pose, r * 1.05f, age, 0.3f, shown, true);
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		rimFlames(flames, pose, camera, right, toward, Vec3.ZERO, r, r * 0.7f, 12, age, GOLD, shown, 0);
		// Its fire streaming out behind it, back the way it came, from its face out (not from within it).
		Vec3 way = c.getPosition(partial).subtract(c.getPosition(Math.max(0, partial - 1)));
		Vec3 back = way.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : way.normalize().scale(-1);
		for (int i = 0; i < 16; i++) {
			float t = (age * 0.1f + i / 16f) % 1;
			Vec3 p = back.scale(r * (1.15 + t * 6)).add((SpellRender.hash(i * 7L + c.getId()) - 0.5) * r * t, (SpellRender.hash(i * 9L) - 0.5) * r * t,
					(SpellRender.hash(i * 11L) - 0.5) * r * t);
			float[] ff = SpellRender.frame(age + i * 3, 0.5f, SpellRender.FLAME_FRAMES);
			SpellRender.billboard(flames, pose, camera, (float) p.x, (float) p.y, (float) p.z, r * (1.8f - t), i, i % 2 == 0 ? GOLD : EMBER, 0.85f * (1 - t) * shown,
					0, ff[0], 1, ff[1], SpellRender.FULL_BRIGHT);
		}
	}

	// --- Sun Pulled Through the Void ---

	/**
	 * The sun pulled through the void (VOID_SUN: its size its round, its value how far below it the land is), drawn as Solar Field's sun is but
	 * as a sun of the sky (the user's ask, 2026-10-26: "I think solar field looks so much better and then u have this move for so much qi which
	 * looks very bland and boring"; it was a model of spikes and shards). Its body solid and boiling, a skin of fire over it, its limb burning
	 * white; prominences of fire looping off its face and falling back; flames standing round its rim; its corona and long rays wheeling slowly
	 * round it, its light vast about it; a shaft of heat poured down onto the land under it, the fire's seal burning there as wide as it burns,
	 * embers raining down. Dragged out of the void it swells as it comes; collapsing ({@code collapse}) it shrinks white-hot, its light drawn in.
	 */
	private static void voidSun(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = c.tickCount + partial;
		float shown = SpellRender.fade(age, c.life(), 3, SpellConstruct.FADE);
		if (shown <= 0) return;
		float full = Math.max(2, c.size()), drop = Math.max(4, c.value());
		float swell = SpellRender.easeOut(Math.min(1, age / com.example.defyingtheheavens.FireSpells.SUN_PULL));
		boolean collapsing = "collapse".equals(c.phase());
		float since = collapsing ? c.phaseAge(partial) : 0;
		float collapse = collapsing ? Mth.clamp(since / com.example.defyingtheheavens.FireSpells.SUN_COLLAPSE, 0, 1) : 0;
		// Burst: gone in a few ticks after its collapse (the blast is the explosion's).
		float burst = collapsing ? Mth.clamp(1 - (since - com.example.defyingtheheavens.FireSpells.SUN_COLLAPSE) / 3, 0, 1) : 1;
		float r = full * (0.15f + 0.85f * swell) * (1 - 0.65f * collapse * collapse) * burst * Mth.clamp(shown * 1.5f, 0, 1);
		if (r < 0.05f) return;
		float pulse = 1 + 0.03f * Mth.sin(age * 0.21f) + 0.25f * collapse * Mth.sin(since * 2.6f);
		float[] hot = mix(SUN_GOLD, SUN_WHITE, 0.35f + 0.65f * collapse), body = mix(SUN_WHITE, WHITE, collapse);
		float[] fv = SpellRender.frame(age, 0.25f, SpellRender.SUN_FRAMES);
		Vector3f right = camera.transform(new Vector3f(1, 0, 0)), toward = camera.transform(new Vector3f(0, 0, 1)).negate();

		// Its light, vast about it (Solar Field's three glows, as great as it is), whiter as it collapses.
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 3.8f * pulse, 0, hot, 0.85f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 8.5f, 0, SUN_GOLD, 0.45f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 18 * (1 - 0.4f * collapse), 0, EMBER, 0.2f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);

		// Its body: the sun's own boiling face, solid; a skin of fire over it turning the other way; its limb burning white.
		SpellRender.Wobble boil = (polar, azimuth) -> 1 + 0.018f * Mth.sin((float) (azimuth * 7 + age * 0.23)) * Mth.sin((float) (polar * 5 - age * 0.17));
		SpellRender.sphere(buffers.getBuffer(SpellRender.core(SpellRender.SUN)), pose.last(), r, 14, 22, body, 1, 2, (fv[1] - fv[0]) * 2, age * 0.003f, fv[0],
				SpellRender.FULL_BRIGHT, boil);
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.SUN)), pose.last(), r * 1.03f, 10, 16, SUN_GOLD, 0.5f * shown, 1.5f, fv[1] - fv[0],
				-age * 0.004f, fv[0], SpellRender.FULL_BRIGHT, boil);
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose, camera, 0, 0, 0, r * 2.35f, 0, hot, 0.75f * shown, 0, 0, 1, 1,
				SpellRender.FULL_BRIGHT);

		// The corona, streaming out past its limb in long rays, two of it turning against each other; and long rays wheeling slowly.
		VertexConsumer corona = buffers.getBuffer(SpellRender.glow(CORONA));
		SpellRender.billboard(corona, pose, camera, 0, 0, 0, r * 4.9f * pulse, age * 0.003f, SUN_WHITE, 0.85f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.billboard(corona, pose, camera, 0, 0, 0, r * 6.2f, -age * 0.0022f + 0.7f, SUN_GOLD, 0.45f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		VertexConsumer rays = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 3; i++) {
			float span = r * 12 * (1 - 0.5f * collapse);
			SpellRender.billboard(rays, pose, camera, 0, 0, 0, span, span, age * 0.003f * (i + 1) + i * 0.6f, hot, 0.45f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}

		// Flames standing round its rim; prominences of fire looping off its face, rising and falling back (behind it, hidden by it).
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		rimFlames(flames, pose, camera, right, toward, Vec3.ZERO, r * 0.98f, r * (0.42f + 0.2f * collapse), 22, age, GOLD, shown, 0);
		for (int k = 0; k < 8; k++) {
			long seed = c.getId() * 37L + k * 11L;
			float cycle = (age * (0.006f + 0.004f * SpellRender.hash(seed)) + SpellRender.hash(seed + 1)) % 1;
			float lift = Mth.sin((float) Math.PI * cycle) * (1 - collapse);
			if (lift <= 0.02f) continue;
			Vector3f foot = unit(Math.acos(1 - 2 * SpellRender.hash(seed + 2)), Math.PI * 2 * SpellRender.hash(seed + 3) + age * 0.002f);
			Vector3f across = new Vector3f(foot).cross(Math.abs(foot.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0)).normalize();
			float span = 0.35f + 0.3f * SpellRender.hash(seed + 4), high = r * (0.3f + 0.45f * SpellRender.hash(seed + 5)) * lift;
			List<Vec3> arc = new ArrayList<>();
			float[] radii = new float[15];
			for (int i = 0; i <= 14; i++) {
				float f = i / 14f, a = (f - 0.5f) * span;
				Vector3f p = new Vector3f(foot).rotateAxis(a, across.x, across.y, across.z).mul(r * 1.01f + high * Mth.sin((float) Math.PI * f));
				arc.add(new Vec3(p.x, p.y, p.z));
				radii[i] = r * 0.06f * (0.55f + 0.45f * Mth.sin((float) Math.PI * f)) * (0.5f + 0.5f * lift);
			}
			SpellRender.tube(flames, pose.last(), arc, radii, 6, k % 2 == 0 ? GOLD : EMBER, 0.9f * shown, 0.12f / Math.max(1, r * 0.1f), -age * 0.04f + k,
					SpellRender.FULL_BRIGHT, null);
		}

		// Its collapse: light drawn in to it from all round, faster as it ends.
		if (collapse > 0) {
			VertexConsumer star = buffers.getBuffer(SpellRender.glow(SpellRender.STAR));
			for (int i = 0; i < 28; i++) {
				long seed = c.getId() * 53L + i;
				float t = (since * 0.09f + SpellRender.hash(seed)) % 1;
				Vector3f d = unit(Math.acos(1 - 2 * SpellRender.hash(seed + 1)), Math.PI * 2 * SpellRender.hash(seed + 2));
				float out = r * (1.1f + 3.5f * (1 - t));
				SpellRender.billboard(star, pose, camera, d.x * out, d.y * out, d.z * out, r * 0.5f * t, i, SUN_WHITE, collapse * t * shown, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
			SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * (2.4f + 3 * collapse), 0, WHITE, 0.6f * collapse * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}

		// The land under it: a shaft of its heat poured down onto it, widening; the fire's seal burning there as wide as it burns; embers
		// raining down out of it.
		float under = drop - r * 0.85f, burns = full * 4.4f;
		if (under > 1) {
			float settle = Mth.clamp((age - com.example.defyingtheheavens.FireSpells.SUN_PULL) / 15f, 0, 1) * (1 - collapse);
			pose.pushPose();
			pose.translate(0, -drop, 0);
			if (settle > 0) {
				SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.BEAM)), pose.last(), under, h -> r * (1.7f - 0.9f * h), 6, 28, SUN_GOLD,
						0.22f * settle * shown, 3, Math.max(1, under / 10), 0, age * 0.03f, 0.3f, SpellRender.FULL_BRIGHT, h -> 0.3f + 0.7f * h);
				SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), under, h -> r * (2.4f - 1.2f * h), 4, 24, EMBER,
						0.12f * settle * shown, 1, 1, 0, 0, 0, SpellRender.FULL_BRIGHT, h -> 0.4f + 0.6f * h);
				SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.seal(com.example.defyingtheheavens.QiElement.FIRE))), pose.last(), burns * 0.7f, 0.3f,
						age * 0.004f, SpellRender.light(com.example.defyingtheheavens.QiElement.FIRE), 0.5f * settle * shown, SpellRender.FULL_BRIGHT);
				SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RUNE)), pose.last(), burns * 0.95f, 0.32f, -age * 0.003f, EMBER, 0.3f * settle * shown,
						SpellRender.FULL_BRIGHT);
				SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), burns * 1.05f, 0.34f, 0, SUN_GOLD, 0.45f * settle * shown,
						SpellRender.FULL_BRIGHT);
				SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), burns * 1.3f, 0.36f, 0, EMBER, 0.3f * settle * shown,
						SpellRender.FULL_BRIGHT);
			}
			VertexConsumer embers = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
			for (int i = 0; i < 48; i++) {
				long seed = c.getId() * 71L + i;
				float t = (age * (0.012f + 0.01f * SpellRender.hash(seed)) + SpellRender.hash(seed + 1)) % 1;
				double a = SpellRender.hash(seed + 2) * Math.PI * 2;
				float out = r * 0.9f * (float) Math.sqrt(SpellRender.hash(seed + 3)) + t * r * 0.8f;
				SpellRender.billboard(embers, pose, camera, (float) Math.cos(a) * out, under * (1 - t), (float) Math.sin(a) * out, 0.35f + r * 0.05f, 0,
						i % 3 == 0 ? SUN_WHITE : EMBER, 0.9f * Math.min(1, (1 - t) * 3) * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
			pose.popPose();
		}
	}

	/** {@code a} toward {@code b} by {@code t}. */
	private static float[] mix(float[] a, float[] b, float t) {
		return new float[] {Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
	}

	// --- Glacier of the First Age ---

	/** Each glacier's crystals' feet (the land under each, as found on this client the first time it is drawn), by its entity id. */
	private static final java.util.Map<Integer, float[]> GLACIER_FEET = new java.util.HashMap<>();
	private static final float[] ICE_PALE = {0.82f, 0.95f, 1.0f}, ICE_DEEP = {0.42f, 0.7f, 1.0f}, ICE_GLOW = {0.55f, 0.85f, 1.0f};
	/** How far apart its rows of crystals stand along its way, and how wide each crystal is apart across it (blocks). */
	private static final float GLACIER_ROW = 2.6f, GLACIER_ACROSS = 4.2f;

	/**
	 * Glacier of the First Age's surge (GLACIER_SURGE: its size the half of its width, its value how far it surges; WaterSpells#Glacier): the first
	 * age's ice bursting up out of the land as its front comes, row on row of colossal crystals, faceted, clear and blue, leaning on the way it
	 * goes, tallest along its middle, their light cold within them; each heaving up past its full height as the front reaches it and settling,
	 * those far behind sinking down into frosted mounds; cold mist rolling off its front. At its end ({@code calve}) its crystals shatter, the
	 * shards of its front flung on ahead tumbling, the rest sinking away.
	 */
	private static void glacier(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = (float) c.flown(partial);
		float shown = SpellRender.fade(c.tickCount + partial, c.life(), 2, SpellConstruct.FADE);
		if (shown <= 0) return;
		float half = Math.max(4, c.size()), reach = Math.max(8, c.value()), high = half * 0.57f;
		float rises = com.example.defyingtheheavens.WaterSpells.GLACIER_RISES, ticks = com.example.defyingtheheavens.WaterSpells.GLACIER_TICKS;
		float front = age < rises ? 0 : Math.min(reach, reach * (age - rises + 1) / ticks);
		boolean calving = "calve".equals(c.phase());
		float since = calving ? c.phaseAge(partial) : 0;
		long seed = c.tint();
		Vec3 forward = Vec3.directionFromRotation(0, c.drawnYaw(partial)), side = new Vec3(-forward.z, 0, forward.x);
		int rows = Math.max(1, (int) (reach / GLACIER_ROW) + 1), across = Math.max(2, Math.round(half * 2 / GLACIER_ACROSS));
		float[] feet = GLACIER_FEET.computeIfAbsent(c.getId(), id -> {
			float[] none = new float[rows * across];
			java.util.Arrays.fill(none, Float.NaN);
			return none;
		});
		if (feet.length != rows * across) return;
		VertexConsumer ice = buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(SpellRender.ICE));
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		VertexConsumer star = buffers.getBuffer(SpellRender.glow(SpellRender.STAR));
		Vec3 here = c.getPosition(partial);
		for (int row = 0; row < rows; row++) {
			for (int k = 0; k < across; k++) {
				long s = seed * 131 + row * 977L + k * 31L;
				float along = row * GLACIER_ROW + (SpellRender.hash(s) - 0.5f) * 1.8f + 1.2f;
				if (along > reach) continue;
				float u = -1 + (k + 0.5f + (SpellRender.hash(s + 1) - 0.5f) * 0.7f) * 2f / across;
				float out = u * half;
				// When the front reached it: its rise (heaving up past its height and settling), and, far behind the front, its sinking to a mound.
				float reached = rises + Math.max(0, along) / reach * ticks - (along < 6 ? rises * (1 - along / 6) : 0);
				float since0 = age - reached;
				if (since0 <= 0) continue;
				float g = since0 >= 7 ? 1 : 1 + 2.70158f * (float) Math.pow(since0 / 7 - 1, 3) + 1.70158f * (float) Math.pow(since0 / 7 - 1, 2);
				float behind = front - along;
				// Far behind its front it has sunk away into the frost (and is not drawn: four hundred crystals at once were heavy to draw).
				if (behind > 58) continue;
				float settle = behind > 30 ? Math.max(0, 1 - (behind - 30) / 28f) : 1;
				float scale = g * settle;
				// Shattering: its front's crystals burst into shards flung on ahead; those behind sink away.
				boolean shatters = calving && behind < 26;
				if (calving) scale *= shatters ? Math.max(0, 1 - since / 4) : Math.max(0, 1 - since / 16);
				// Its foot, on the land under it (found once).
				int slot = row * across + k;
				if (Float.isNaN(feet[slot])) {
					Vec3 at = here.add(forward.scale(along)).add(side.scale(out));
					Vec3 ground = BlockMasses.groundUnder(at.add(0, high, 0), high * 2.4);
					feet[slot] = ground == null ? 0 : (float) (ground.y - here.y);
				}
				float footY = feet[slot];
				float edge = 1 - 0.55f * u * u * u * u;
				float tall = high * (0.45f + 0.7f * SpellRender.hash(s + 2)) * edge * (0.85f + 0.3f * (1 - along / reach));
				float radius = (0.9f + 0.9f * SpellRender.hash(s + 3)) * (0.7f + 0.3f * edge);
				Vec3 foot = forward.scale(along).add(side.scale(out)).add(0, footY - radius * 0.3f, 0);
				float lean = 0.12f + 0.2f * SpellRender.hash(s + 4), sway = (SpellRender.hash(s + 5) - 0.5f) * 0.25f;
				Vec3 tilt = forward.scale(lean).add(side.scale(sway + u * 0.12f));
				float[] tint = mix(ICE_PALE, ICE_DEEP, SpellRender.hash(s + 6) * 0.7f);
				if (scale > 0.02f) {
					boolean far = cam.distanceToSqr(foot) > 80 * 80;
					crystal(ice, pose.last(), foot, radius * Math.min(1, 0.4f + 0.6f * scale), tall * scale, (float) tilt.x, (float) tilt.z, far ? 4 : 6, s, tint, 0.82f * shown);
					if (far) continue;
					// The cold light within it, and now and then a glint at its point.
					float mid = tall * scale * 0.45f;
					SpellRender.billboard(glow, pose, camera, (float) (foot.x + tilt.x * mid), (float) foot.y + mid, (float) (foot.z + tilt.z * mid), radius * 3.2f, 0, ICE_GLOW,
							0.14f * shown * scale, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
					float twinkle = Mth.sin(age * 0.13f + SpellRender.hash(s + 7) * 30);
					if (twinkle > 0.85f && behind < 40) {
						float tipH = tall * scale;
						SpellRender.billboard(star, pose, camera, (float) (foot.x + tilt.x * tipH), (float) foot.y + tipH, (float) (foot.z + tilt.z * tipH), radius * 2.2f,
								age * 0.05f, WHITE, (twinkle - 0.85f) / 0.15f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
					}
				}
				if (shatters && since < 22) shards(ice, pose, foot, tall, radius, forward, since, s, tint, shown);
			}
		}
		// Cold mist rolling off its front along the land, brighter at its heart.
		if (!calving || since < 20) {
			VertexConsumer mist = buffers.getBuffer(SpellRender.clear(SpellRender.MIST));
			VertexConsumer haze = buffers.getBuffer(SpellRender.glow(SpellRender.MIST));
			float strength = shown * (calving ? 1 - since / 20 : 1) * Mth.clamp(age / rises, 0, 1);
			for (int i = 0; i < 44; i++) {
				long s = seed * 17 + i;
				float drift = (age * (0.014f + 0.01f * SpellRender.hash(s + 1)) + SpellRender.hash(s)) % 1;
				float out = (SpellRender.hash(s + 2) * 2 - 1) * (half + 2);
				float along = front + 0.5f + drift * 7 - (i % 3 == 0 ? SpellRender.hash(s + 3) * 14 : 0);
				int row = Mth.clamp((int) (Math.max(0, along) / GLACIER_ROW), 0, rows - 1), k = Mth.clamp((int) ((out / half + 1) / 2 * across), 0, across - 1);
				float footY = Float.isNaN(feet[row * across + k]) ? 0 : feet[row * across + k];
				Vec3 at = forward.scale(along).add(side.scale(out)).add(0, footY + 0.8f + drift * 2.4f, 0);
				float size = 6 + 5 * SpellRender.hash(s + 4) + drift * 5;
				float a = strength * (1 - drift) * 0.45f;
				SpellRender.billboard(mist, pose, camera, (float) at.x, (float) at.y, (float) at.z, size, age * 0.004f * (i % 2 == 0 ? 1 : -1) + i, ICE_PALE, a, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
				if (i % 3 == 0) SpellRender.billboard(haze, pose, camera, (float) at.x, (float) at.y, (float) at.z, size * 0.7f, i, ICE_GLOW, a * 0.4f, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
		}
		GLACIER_FEET.keySet().removeIf(id -> net.minecraft.client.Minecraft.getInstance().level == null
				|| !(net.minecraft.client.Minecraft.getInstance().level.getEntity(id) instanceof SpellConstruct));
	}

	/** A shattered crystal's shards, {@code since} ticks after it broke: flung up and on ahead, tumbling, falling, fading. */
	private static void shards(VertexConsumer vc, PoseStack pose, Vec3 foot, float tall, float radius, Vec3 forward, float since, long seed, float[] tint, float shown) {
		float fade = Math.max(0, 1 - since / 22) * shown;
		for (int i = 0; i < 4; i++) {
			long s = seed * 7 + i;
			float h0 = tall * (0.2f + 0.2f * i);
			Vec3 v = forward.scale(0.35 + 0.35 * SpellRender.hash(s)).add((SpellRender.hash(s + 1) - 0.5) * 0.5, 0.25 + 0.3 * SpellRender.hash(s + 2), (SpellRender.hash(s + 3) - 0.5) * 0.5);
			Vec3 at = foot.add(0, h0, 0).add(v.scale(since)).add(0, -0.045 * since * since, 0);
			pose.pushPose();
			pose.translate(at.x, at.y, at.z);
			pose.mulPose(com.mojang.math.Axis.XP.rotation(since * (0.15f + 0.2f * SpellRender.hash(s + 4))));
			pose.mulPose(com.mojang.math.Axis.ZP.rotation(since * (0.1f + 0.2f * SpellRender.hash(s + 5)) + i));
			crystal(vc, pose.last(), Vec3.ZERO, radius * 0.45f, tall * 0.28f, 0, 0, 5, s, tint, 0.8f * fade);
			pose.popPose();
		}
	}

	/**
	 * A crystal of ice standing on {@code foot}, {@code radius} round and {@code height} tall, leaning by {@code leanX, leanZ} (along its height): a
	 * prism of {@code sides} faces swelling a little to its shoulder, then drawn to a point; each face its own facet, the ice's picture up it.
	 */
	private static void crystal(VertexConsumer vc, PoseStack.Pose pose, Vec3 foot, float radius, float height, float leanX, float leanZ, int sides, long seed, float[] tint,
								float alpha) {
		if (height <= 0.05f) return;
		float[] at = {0, 0.62f, 1}, wide = {1, 1.08f, 0};
		Vector3f[][] ring = new Vector3f[3][sides];
		float twist = SpellRender.hash(seed + 11) * 6.28f;
		for (int k = 0; k < 3; k++) {
			float y = height * at[k];
			for (int i = 0; i < sides; i++) {
				double t = Math.PI * 2 * i / sides + twist;
				float r = radius * wide[k] * (k == 1 ? 0.9f + 0.2f * SpellRender.hash(seed + i * 3) : 1);
				ring[k][i] = new Vector3f((float) foot.x + leanX * y + (float) Math.cos(t) * r, (float) foot.y + y, (float) foot.z + leanZ * y + (float) Math.sin(t) * r);
			}
		}
		for (int k = 0; k < 2; k++) {
			for (int i = 0; i < sides; i++) {
				int n = (i + 1) % sides;
				Vector3f a = ring[k][i], b = ring[k + 1][i], cc = ring[k + 1][n], d = ring[k][n];
				float shade = 0.8f + 0.2f * SpellRender.hash(seed + i * 7 + k);
				float[] col = {tint[0] * shade, tint[1] * shade, tint[2] * shade};
				float v0 = 0, v1 = (b.y - a.y) * 0.25f;
				if (k == 1) SpellRender.quad(vc, pose, a, b, b, d, col, alpha, 0, v0, 1, v1, SpellRender.FULL_BRIGHT);
				else SpellRender.quad(vc, pose, a, b, cc, d, col, alpha, 0, v0, 1, v1, SpellRender.FULL_BRIGHT);
			}
		}
	}

	// --- The Primordial Sea ---

	private static final float[] DEEP_SEA = {0.18f, 0.42f, 0.72f}, SHALLOW_SEA = {0.55f, 0.85f, 1.0f}, STORM = {0.2f, 0.22f, 0.27f}, BOLT = {0.85f, 0.9f, 1.0f};

	/**
	 * The Primordial Sea (PRIMORDIAL_STORM: its size its round, WaterSpells#PrimalSea), the waters before heaven and earth, grander than its ring of
	 * waves was (the user's ask, 2026-10-26: "make it more grand and imposing, idk add some rain on the inside if ur in that middle area"): a ring
	 * of colossal waves welling up out of the land all round, their faces running with water, curling in over it, foam and spray along their lips,
	 * mist at their feet, the whole ring wheeling slowly; within them the sea churning round a whirlpool, its arms of foam drawn in to a dark eye;
	 * over all a storm, black clouds turning, lightning striking down into the sea, rain falling everywhere within, heavy all round one who stands
	 * in it, ringing the water where it falls. Closing ({@code close}), its waves crash in over its heart.
	 */
	private static void primordialStorm(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = c.tickCount + partial;
		float shown = SpellRender.fade(age, c.life(), 6, SpellConstruct.FADE);
		if (shown <= 0) return;
		float r = Math.max(8, c.size());
		float rise = SpellRender.easeOut(Math.min(1, age / com.example.defyingtheheavens.WaterSpells.PRIMAL_SEA_RISES));
		boolean closing = "close".equals(c.phase());
		float since = closing ? c.phaseAge(partial) : 0;
		float close = closing ? SpellRender.ease(since / com.example.defyingtheheavens.WaterSpells.PRIMAL_SEA_CLOSES) : 0;
		float tall = r * 0.95f * rise * (1 - 0.55f * close), ring = r * (1 - 0.8f * close), sky = r * 1.35f + 6;
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		long seed = c.getId() * 7919L;

		// The sea within, churning round its whirlpool.
		VertexConsumer water = buffers.getBuffer(SpellRender.clear(SpellRender.WATER));
		for (int k = 0; k < 3; k++) {
			round(water, pose.last(), ring * (1.02f - 0.3f * k) * rise, 0.5f + 0.25f * k, age * (0.012f + 0.02f * k), k == 0 ? DEEP_SEA : SHALLOW_SEA, (0.8f - 0.15f * k) * shown, fv);
		}
		VertexConsumer foam = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		for (int arm = 0; arm < 6; arm++) {
			List<Vec3> points = new ArrayList<>();
			int steps = 28;
			float[] widths = new float[steps + 1], alphas = new float[steps + 1];
			for (int i = 0; i <= steps; i++) {
				float f = i / (float) steps;
				double a = Math.PI * 2 * arm / 6 + age * 0.03 + f * Math.PI * 2.2;
				float d = ring * 0.95f * (1 - f) * rise;
				points.add(new Vec3(Math.cos(a) * d, 1.4f + 0.3f * Mth.sin(age * 0.2f + i), Math.sin(a) * d));
				widths[i] = (2.2f + r * 0.05f) * (1 - 0.7f * f);
				alphas[i] = 0.4f * shown * Mth.sin((float) Math.PI * f);
			}
			SpellRender.ribbon(foam, pose.last(), points, widths, new Vec3(0, 400, 0), FOAM, alphas, 0, 0, SpellRender.FULL_BRIGHT);
		}
		pose.pushPose();
		pose.translate(0, 1.6f, 0);
		SpellRender.disc(buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE)), pose.last(), r * 0.16f * rise, 0, age * 0.1f, new float[] {0.03f, 0.08f, 0.15f}, 0.85f * shown,
				SpellRender.FULL_BRIGHT);
		pose.popPose();

		// The ring of waves: their faces running with water, curling in at their lips, wheeling slowly round.
		wavesRound(water, pose.last(), ring, tall, age, fv, shown);
		// Foam along their lips, spray flung in off them, mist at their feet.
		VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		VertexConsumer spray = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
		for (int i = 0; i < 64; i++) {
			long s = seed + i * 13L;
			double a = Math.PI * 2 * i / 64 + age * 0.008 + SpellRender.hash(s) * 0.1;
			float lip = tall * 0.98f, in = ring - tall * 0.1f;
			float bob = Mth.sin(age * 0.2f + i * 1.7f);
			SpellRender.billboard(smoke, pose, camera, (float) Math.cos(a) * in, lip + bob, (float) Math.sin(a) * in, 4 + 3 * SpellRender.hash(s + 1), age * 0.01f + i, FOAM,
					0.7f * shown * rise, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			float t = (age * 0.03f + SpellRender.hash(s + 2)) % 1;
			float d = in - t * tall * 0.5f;
			SpellRender.billboard(spray, pose, camera, (float) Math.cos(a) * d, lip - t * t * tall * 0.9f, (float) Math.sin(a) * d, 0.5f, 0, FOAM, 0.85f * (1 - t) * shown * rise,
					0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			if (i % 2 == 0) {
				float m = (age * 0.012f + SpellRender.hash(s + 3)) % 1;
				float out = ring + tall * 0.25f + m * 6;
				SpellRender.billboard(smoke, pose, camera, (float) Math.cos(a) * out, 1 + m * 4, (float) Math.sin(a) * out, 8 + m * 8, i, FOAM, 0.4f * (1 - m) * shown * rise,
						0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}

		// The storm over it: black clouds piled and turning.
		VertexConsumer clouds = buffers.getBuffer(SpellRender.clear(SpellRender.CLOUD));
		float gather = Mth.clamp(age / 30, 0, 1) * shown;
		for (int i = 0; i < 70; i++) {
			long s = seed + 500 + i * 7L;
			double a = SpellRender.hash(s) * Math.PI * 2 + age * 0.0025 * (i % 2 == 0 ? 1 : 0.6);
			float d = (float) Math.sqrt(SpellRender.hash(s + 1)) * r * 1.35f;
			float y = sky + (SpellRender.hash(s + 2) - 0.5f) * 8 + d * 0.08f;
			float shade = 0.75f + 0.5f * SpellRender.hash(s + 3);
			SpellRender.billboard(clouds, pose, camera, (float) Math.cos(a) * d, y, (float) Math.sin(a) * d, (16 + 14 * SpellRender.hash(s + 4)) * (0.4f + 0.6f * gather), age * 0.002f + i,
					new float[] {STORM[0] * shade, STORM[1] * shade, STORM[2] * shade}, 0.92f * gather, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		// Lightning striking down into the sea, now and again: the clouds lit from within, a bolt, a flash where it strikes.
		long beat = (long) (age / 23);
		float flash = age % 23;
		if (gather > 0.6f && SpellRender.hash(seed * 3 + beat) < 0.8f && flash < 7) {
			float f = (1 - flash / 7) * ((int) flash % 2 == 0 ? 1 : 0.45f) * shown;
			double a = SpellRender.hash(seed + beat * 7) * Math.PI * 2, d = Math.sqrt(SpellRender.hash(seed + beat * 7 + 1)) * ring * 0.8;
			float bx = (float) (Math.cos(a) * d), bz = (float) (Math.sin(a) * d);
			glowAt(buffers, pose, camera, bx, sky, bz, r * 2.2f, BOLT, 0.6f * f);
			glowAt(buffers, pose, camera, bx, 2, bz, r * 0.8f, BOLT, 0.7f * f);
			VertexConsumer bolt = buffers.getBuffer(SpellRender.glow(SpellRender.LIGHTNING));
			float yaw = (float) Math.atan2(cam.x - bx, cam.z - bz);
			pose.pushPose();
			pose.translate(bx, sky, bz);
			pose.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
			float w = 2.2f;
			boolean flip = SpellRender.hash(seed + beat * 5) < 0.5f;
			SpellRender.quad(bolt, pose.last(), SpellRender.v(-w, 0, 0), SpellRender.v(-w, -sky, 0), SpellRender.v(w, -sky, 0), SpellRender.v(w, 0, 0), BOLT, f,
					flip ? 1 : 0, 0, flip ? 0 : 1, 1, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}

		// The rain: everywhere within it, falling from the storm to the sea; and heavy all round one who stands within it.
		rain(buffers, pose, cam, ring, sky, age, seed, gather);
	}

	/** A light of {@code rgb} at (x, y, z), {@code size} across. */
	private static void glowAt(MultiBufferSource buffers, PoseStack pose, Quaternionf camera, float x, float y, float z, float size, float[] rgb, float alpha) {
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose, camera, x, y, z, size, 0, rgb, alpha, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
	}

	/**
	 * The ring of waves {@code ring} round and {@code tall} high, facing in: each wave's section from its foot outside up its face to its lip curling
	 * in, crests running round it and the whole wheeling slowly; deep and dark at its foot, pale toward its lip, its lip white with foam.
	 */
	private static void wavesRound(VertexConsumer vc, PoseStack.Pose pose, float ring, float tall, float age, float[] fv, float alpha) {
		if (tall < 0.5f) return;
		// Its section: out from the ring (blocks, as shares of its height; out is positive) and up (shares of its height).
		float[][] section = {{0.26f, 0}, {0.17f, 0.2f}, {0.09f, 0.46f}, {0.03f, 0.72f}, {-0.02f, 0.9f}, {-0.08f, 1.0f}, {-0.14f, 0.97f}, {-0.18f, 0.86f}, {-0.17f, 0.74f}};
		int round = 64;
		float fh = fv[1] - fv[0];
		for (int j = 0; j < round; j++) {
			for (int k = 0; k < section.length - 1; k++) {
				Vector3f p00 = wavePoint(ring, tall, section[k], j / (float) round, age), p01 = wavePoint(ring, tall, section[k], (j + 1) / (float) round, age);
				Vector3f p10 = wavePoint(ring, tall, section[k + 1], j / (float) round, age), p11 = wavePoint(ring, tall, section[k + 1], (j + 1) / (float) round, age);
				float t0 = k / (float) (section.length - 1), t1 = (k + 1) / (float) (section.length - 1);
				float[] c0 = k >= section.length - 3 ? FOAM : mix(DEEP_SEA, SHALLOW_SEA, t0), c1 = k + 1 >= section.length - 3 ? FOAM : mix(DEEP_SEA, SHALLOW_SEA, t1);
				float u0 = j / (float) round * 14 - age * 0.01f, u1 = (j + 1) / (float) round * 14 - age * 0.01f;
				float v0 = fv[0] + fh * (0.02f + 0.96f * Mth.frac(t0 * 1.6f - age * 0.04f)), v1 = v0 + fh * 0.2f;
				SpellRender.vertex(vc, pose, p00.x, p00.y, p00.z, c0[0], c0[1], c0[2], 0.82f * alpha, u0, v0, SpellRender.FULL_BRIGHT, 0, 1, 0);
				SpellRender.vertex(vc, pose, p10.x, p10.y, p10.z, c1[0], c1[1], c1[2], 0.82f * alpha, u0, v1, SpellRender.FULL_BRIGHT, 0, 1, 0);
				SpellRender.vertex(vc, pose, p11.x, p11.y, p11.z, c1[0], c1[1], c1[2], 0.82f * alpha, u1, v1, SpellRender.FULL_BRIGHT, 0, 1, 0);
				SpellRender.vertex(vc, pose, p01.x, p01.y, p01.z, c0[0], c0[1], c0[2], 0.82f * alpha, u1, v0, SpellRender.FULL_BRIGHT, 0, 1, 0);
			}
		}
	}

	/** A point of the ring of waves: {@code at} of its section, {@code round} of the way round it (0 to 1), its crests running round as time goes. */
	private static Vector3f wavePoint(float ring, float tall, float[] at, float round, float age) {
		double a = Math.PI * 2 * round + age * 0.006;
		float crest = 0.88f + 0.12f * Mth.sin((float) (a * 9 - age * 0.09)) * Mth.sin((float) (a * 4 + age * 0.05));
		float r = ring + at[0] * tall * (1 + 0.25f * Mth.sin((float) (a * 6 + age * 0.07)));
		return new Vector3f((float) Math.cos(a) * r, at[1] * tall * crest, (float) Math.sin(a) * r);
	}

	/**
	 * The storm's rain within the ring {@code ring} round, from {@code sky} down to the sea: streaks spread all over it, seen from without; and, for
	 * one standing within it, heavy all round them (a streak in every cell of the land about them, set to the land, so it stays as they move),
	 * rings spreading on the water where it falls.
	 */
	private static void rain(MultiBufferSource buffers, PoseStack pose, Vec3 cam, float ring, float sky, float age, long seed, float alpha) {
		if (alpha <= 0.02f) return;
		VertexConsumer rain = buffers.getBuffer(SpellRender.clear(SpellRender.RAIN));
		float[] tint = {0.72f, 0.82f, 0.95f};
		int drops = (int) Math.min(240, ring * ring * 0.16f);
		for (int i = 0; i < drops; i++) {
			long s = seed * 31 + i;
			double a = SpellRender.hash(s) * Math.PI * 2, d = Math.sqrt(SpellRender.hash(s + 1)) * ring * 0.97;
			float x = (float) (Math.cos(a) * d), z = (float) (Math.sin(a) * d);
			float fall = (age * 1.1f + SpellRender.hash(s + 2) * sky) % sky;
			streak(rain, pose, cam, x, sky - fall, z, 0.07f, 3.2f, tint, 0.55f * alpha);
		}
		// Within it: rain heavy all round, in a cell of every block and a half of the land about the eye.
		if (cam.x * cam.x + cam.z * cam.z > ring * ring || cam.y > sky || cam.y < -8) return;
		VertexConsumer rings = buffers.getBuffer(SpellRender.clear(SpellRender.RIPPLE));
		float cell = 1.8f, reach = 16;
		int n = (int) (reach / cell);
		int cx = Mth.floor(cam.x / cell), cz = Mth.floor(cam.z / cell);
		for (int gx = cx - n; gx <= cx + n; gx++) {
			for (int gz = cz - n; gz <= cz + n; gz++) {
				float x = (gx + SpellRender.hash(gx * 73856093L ^ gz * 19349663L)) * cell, z = (gz + SpellRender.hash(gx * 83492791L ^ gz * 2654435761L)) * cell;
				double dx = x - cam.x, dz = z - cam.z;
				if (dx * dx + dz * dz > reach * reach || x * x + z * z > ring * ring) continue;
				long s = gx * 341873128712L + gz * 132897987541L + seed;
				float top = (float) cam.y + 14, bottom = Math.max(0.6f, (float) cam.y - 12), span = top - bottom;
				float fall = (age * 1.4f + SpellRender.hash(s) * span) % span;
				float near = 1 - (float) Math.sqrt(dx * dx + dz * dz) / reach;
				streak(rain, pose, cam, x, top - fall, z, 0.05f, 1.6f, tint, 0.75f * alpha * Math.min(1, near * 3));
				if (SpellRender.hash(s + 5) < 0.18f) {
					float t = (age * 0.09f + SpellRender.hash(s + 6)) % 1;
					pose.pushPose();
					pose.translate(x, 1.7f, z);
					SpellRender.disc(rings, pose.last(), 0.15f + t * 0.7f, 0, 0, WHITE, 0.6f * (1 - t) * alpha, SpellRender.FULL_BRIGHT);
					pose.popPose();
				}
			}
		}
	}

	/** One streak of rain at (x, y, z) falling {@code length}, {@code width} across, turned about the up axis to the eye. */
	private static void streak(VertexConsumer vc, PoseStack pose, Vec3 cam, float x, float y, float z, float width, float length, float[] rgb, float alpha) {
		float yaw = (float) Math.atan2(cam.x - x, cam.z - z);
		pose.pushPose();
		pose.translate(x, y, z);
		pose.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
		SpellRender.quad(vc, pose.last(), SpellRender.v(-width, 0, 0), SpellRender.v(-width, -length, 0), SpellRender.v(width, -length, 0), SpellRender.v(width, 0, 0), rgb,
				alpha, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		pose.popPose();
	}

	// --- Xirang, the Self-Growing Soil ---

	/** Each bloom's roots' points on the land (found on this client every so often: the land under them heaves), and when they were last found. */
	private static final java.util.Map<Integer, float[][]> BLOOM_LAND = new java.util.HashMap<>();
	private static final java.util.Map<Integer, Integer> BLOOM_FOUND = new java.util.HashMap<>();
	private static final float[] ROOT_GOLD = {1.0f, 0.78f, 0.28f}, SOIL_LIFE = {0.55f, 0.9f, 0.35f};
	private static final int BLOOM_ROOTS = 10, BLOOM_STEPS = 14;

	/**
	 * Xirang's soil growing (XIRANG_BLOOM: its size how far round it grows, its value how high the land heaves; EarthSpells#Xirang), from where its
	 * clod sank into the land: roots of gold running out over the land to its edge, forking as they go, glowing; a ring of the earth's light
	 * running out at their ends, the land within it alight with life; shoots springing up all over it and motes of gold rising off it. Its work
	 * done ({@code settle}), its roots dim and sink back into the land.
	 */
	private static void xirang(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, int light) {
		float age = c.tickCount + partial;
		float shown = SpellRender.fade(age, c.life(), 3, SpellConstruct.FADE);
		if ("settle".equals(c.phase())) shown *= Math.max(0, 1 - c.phaseAge(partial) / 16);
		if (shown <= 0) return;
		float r = Math.max(3, c.size());
		float spread = SpellRender.easeOut(Math.min(1, age / (com.example.defyingtheheavens.EarthSpells.XIRANG_SWELLS * 0.6f)));
		long seed = c.getId() * 4099L;
		// The land under its roots, found again every half second as it heaves.
		float[][] land = BLOOM_LAND.computeIfAbsent(c.getId(), id -> new float[BLOOM_ROOTS * 2][BLOOM_STEPS + 1]);
		int found = BLOOM_FOUND.getOrDefault(c.getId(), -100);
		boolean refind = c.tickCount - found >= 10;
		if (refind) BLOOM_FOUND.put(c.getId(), c.tickCount);
		Vec3 here = c.getPosition(partial);
		VertexConsumer gold = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		VertexConsumer bark = buffers.getBuffer(SpellRender.solid(SpellRender.BARK));
		for (int k = 0; k < BLOOM_ROOTS * 2; k++) {
			// Ten roots, each with a fork off it.
			boolean fork = k >= BLOOM_ROOTS;
			int main = k % BLOOM_ROOTS;
			long s = seed + main * 101L + (fork ? 7 : 0);
			double a0 = Math.PI * 2 * main / BLOOM_ROOTS + SpellRender.hash(s) * 0.5;
			float from = fork ? 0.35f + 0.25f * SpellRender.hash(s + 1) : 0, length = fork ? 0.45f : 1;
			double bend = (SpellRender.hash(s + 2) - 0.5) * 0.9 + (fork ? (SpellRender.hash(s + 3) < 0.5 ? 0.7 : -0.7) : 0);
			List<Vec3> path = new ArrayList<>();
			float[] radii = new float[BLOOM_STEPS + 1];
			for (int i = 0; i <= BLOOM_STEPS; i++) {
				float f = i / (float) BLOOM_STEPS;
				float d = r * (from + length * f * (fork ? 1 - from : 1)) * spread;
				double a = a0 + bend * f * (fork ? 1 : 0.4) + Math.sin(f * 7 + main) * 0.08;
				float x = (float) Math.cos(a) * d, z = (float) Math.sin(a) * d;
				if (refind) {
					Vec3 ground = BlockMasses.groundUnder(here.add(x, r * 0.6, z), r * 1.6);
					land[k][i] = ground == null ? 0 : (float) (ground.y - here.y);
				}
				path.add(new Vec3(x, land[k][i] + 0.12, z));
				radii[i] = (fork ? 0.14f : 0.24f) * (1 - 0.7f * f) * (0.6f + 0.4f * spread);
			}
			SpellRender.tube(bark, pose.last(), path, radii, 5, new float[] {0.75f, 0.62f, 0.45f}, 1, 0.5f, k, light, null);
			float[] glow = new float[radii.length];
			for (int i = 0; i < glow.length; i++) glow[i] = radii[i] * 2.6f;
			SpellRender.tube(gold, pose.last(), path, glow, 5, ROOT_GOLD, 0.55f * shown * (0.75f + 0.25f * Mth.sin(age * 0.25f + k)), 0.2f, 0, SpellRender.FULL_BRIGHT, null);
		}
		// The ring of the earth's light running out at their ends, and the land within it alight.
		float front = r * spread;
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), front * 1.05f + 0.5f, 0.2f, age * 0.01f, ROOT_GOLD, 0.6f * shown * (1.2f - spread),
				SpellRender.FULL_BRIGHT);
		SpellRender.disc(gold, pose.last(), front * 1.1f + 1, 0.15f, 0, SOIL_LIFE, 0.22f * shown, SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.seal(com.example.defyingtheheavens.QiElement.EARTH))), pose.last(), Math.min(front, r * 0.55f), 0.25f,
				age * 0.006f, ROOT_GOLD, 0.5f * shown, SpellRender.FULL_BRIGHT);
		// Shoots springing up all over it, on the land under the nearest root's point; motes of gold rising off it.
		VertexConsumer leaves = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
		VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < 70; i++) {
			long s = seed + 9000 + i * 13L;
			float d = (float) Math.sqrt(SpellRender.hash(s)) * r;
			if (d > front) continue;
			int k = (int) (SpellRender.hash(s + 1) * BLOOM_ROOTS) % BLOOM_ROOTS;
			double a = Math.PI * 2 * k / BLOOM_ROOTS + (SpellRender.hash(s + 2) - 0.5) * 0.6;
			int step = Mth.clamp(Math.round(d / r * BLOOM_STEPS), 0, BLOOM_STEPS);
			float y = land[k][step];
			float x = (float) Math.cos(a) * d, z = (float) Math.sin(a) * d;
			float sprout = SpellRender.easeOut(Mth.clamp((age - d / r * 18) / 8, 0, 1));
			if (sprout > 0.02f) {
				SpellRender.billboard(leaves, pose, camera, x, y + 0.25f * sprout, z, 0.5f * sprout, (float) (SpellRender.hash(s + 3) * 6.3), WHITE, 1, 0, 0, 1, 1, light);
			}
			float rise = (age * 0.04f + SpellRender.hash(s + 4)) % 1;
			SpellRender.billboard(motes, pose, camera, x, y + rise * 4, z, 0.3f, 0, i % 3 == 0 ? SOIL_LIFE : ROOT_GOLD, 0.8f * (1 - rise) * shown, 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
		BLOOM_LAND.keySet().removeIf(id -> net.minecraft.client.Minecraft.getInstance().level == null
				|| !(net.minecraft.client.Minecraft.getInstance().level.getEntity(id) instanceof SpellConstruct));
		BLOOM_FOUND.keySet().retainAll(BLOOM_LAND.keySet());
	}

	// --- Twenty-Four Sea-Fixing Pearls ---

	/** Each pearl's way behind it (world places, newest first), for its stream of water. */
	private static final java.util.Map<Integer, java.util.ArrayDeque<Vec3>> WAKES = new java.util.HashMap<>();
	private static final int WAKE = 14;
	private static final float[] PEARL = {0.97f, 0.97f, 1.0f}, SEA = {0.4f, 0.66f, 0.95f}, FOAM = {0.9f, 0.96f, 1.0f};

	/**
	 * A Sea-Fixing Pearl (PEARL, its size its round): a lustrous pearl, round from every side, white at its heart and its rim shifting through the
	 * colours of nacre as it turns to whoever looks, a glint of light on it; held in a skin of sea water turning about it, two currents wheeling
	 * round it on tilted rings; flying, its water streams out behind it the way it came. Nothing of it is a flat square: the sea about it was
	 * two squares of the water's picture turning on it (the user's report, 2026-10-26, of the pearls: "the squares around the circles just look
	 * very very odd").
	 */
	private static void pearl(SpellConstruct c, float partial, PoseStack pose, MultiBufferSource buffers, Quaternionf camera, Vec3 cam) {
		float age = c.tickCount + partial;
		float shown = SpellRender.fade(age, c.life(), 6, 4);
		if (shown <= 0) return;
		float r = Math.max(0.2f, c.size());
		float hue = (age * 0.012f + c.getId() * 0.13f) % 1;
		// Its light about it (round: the glow's own picture fades to nothing at its edge).
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 5.5f, 0, SEA, 0.32f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		SpellRender.billboard(glow, pose, camera, 0, 0, 0, r * 2.6f, 0, hsv(hue, 0.25f, 1.0f), 0.35f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		// The pearl itself.
		pose.pushPose();
		pose.mulPose(com.mojang.math.Axis.YP.rotation(age * 0.03f));
		// The way to the eye, in the pearl's own turned space.
		Vector3f toward = new Vector3f((float) cam.x, (float) cam.y, (float) cam.z);
		if (toward.lengthSquared() > 1.0e-6f) toward.normalize();
		new Quaternionf().rotationY(-age * 0.03f).transform(toward);
		pearlBall(buffers.getBuffer(SpellRender.clear(SpellRender.GLOW)), pose.last(), r, toward, hue, shown);
		pose.popPose();
		// The sea held about it: a skin of moving water, rippling, see-through.
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		SpellRender.Wobble ripple = (polar, azimuth) -> 1 + 0.05f * Mth.sin((float) (azimuth * 4 + age * 0.3)) * Mth.sin((float) (polar * 3 + age * 0.22));
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), r * 1.45f, 10, 16, SEA, 0.42f * shown, 2, (fv[1] - fv[0]) * 2,
				age * 0.008f, fv[0], SpellRender.FULL_BRIGHT, ripple);
		// Two currents wheeling round it on tilted rings, thickest at their heads.
		for (int k = 0; k < 2; k++) {
			pose.pushPose();
			pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(62 + k * 56));
			pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(k * 40));
			pose.mulPose(com.mojang.math.Axis.YP.rotation(age * (k == 0 ? 0.16f : -0.12f)));
			List<Vec3> ring = new ArrayList<>();
			float[] radii = new float[21];
			for (int i = 0; i <= 20; i++) {
				double a = Math.PI * 1.5 * i / 20;
				ring.add(new Vec3(Math.cos(a) * r * 1.85, Math.sin(a * 3 + age * 0.2) * r * 0.06, Math.sin(a) * r * 1.85));
				radii[i] = r * 0.13f * Mth.sin((float) Math.PI * i / 20) * (0.4f + 0.6f * (1 - i / 20f));
			}
			SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), ring, radii, 6, FOAM, 0.65f * shown, 0.4f, fv[0] - age * 0.05f,
					SpellRender.FULL_BRIGHT, null);
			pose.popPose();
		}
		// The glint of light on it, up on the side the light comes from.
		Vector3f glint = camera.transform(new Vector3f(-r * 0.38f, r * 0.4f, 0));
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.STAR)), pose, camera, glint.x, glint.y, glint.z, r * 1.2f, age * 0.03f, PEARL,
				0.85f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
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

	/**
	 * A pearl's body: a sphere of {@code radius} lit by how each part of it faces {@code toward} (the eye, in the pose's space): white where it
	 * faces the eye, the colours of nacre (from {@code hue} on round) creeping in toward its rim. Only its near side is drawn (it is drawn see-through,
	 * writing no depth: its far side would be laid over its near).
	 */
	private static void pearlBall(VertexConsumer vc, PoseStack.Pose pose, float radius, Vector3f toward, float hue, float alpha) {
		int rings = 14, segments = 22;
		for (int i = 0; i < rings; i++) {
			double p0 = Math.PI * i / rings, p1 = Math.PI * (i + 1) / rings;
			for (int j = 0; j < segments; j++) {
				double t0 = Math.PI * 2 * j / segments, t1 = Math.PI * 2 * (j + 1) / segments;
				Vector3f mid = unit((p0 + p1) / 2, (t0 + t1) / 2);
				if (mid.dot(toward) < -0.05f) continue;
				Vector3f[] corners = {unit(p0, t0), unit(p1, t0), unit(p1, t1), unit(p0, t1)};
				for (Vector3f n : corners) {
					float facing = Mth.clamp(n.dot(toward), 0, 1);
					float rim = (float) Math.pow(1 - facing, 1.6);
					float[] nacre = hsv((hue + rim * 0.45f + n.y * 0.1f) % 1, 0.42f * rim, 1.0f);
					float lit = 0.82f + 0.18f * facing;
					float[] col = {Mth.lerp(rim, 0.97f, nacre[0]) * lit, Mth.lerp(rim, 0.96f, nacre[1]) * lit, Mth.lerp(rim, 1.0f, nacre[2]) * lit};
					SpellRender.vertex(vc, pose, n.x * radius, n.y * radius, n.z * radius, col[0], col[1], col[2], alpha, 0.5f, 0.5f, SpellRender.FULL_BRIGHT, n.x, n.y, n.z);
				}
			}
		}
	}

	/** The point of a unit sphere at {@code polar} from its top and {@code azimuth} round. */
	private static Vector3f unit(double polar, double azimuth) {
		return new Vector3f((float) (Math.sin(polar) * Math.cos(azimuth)), (float) Math.cos(polar), (float) (Math.sin(polar) * Math.sin(azimuth)));
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
