package com.example.defyingtheheavens.client.cinematic;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

/**
 * The hero's stand-ins for step B1, the grey box: every element of the cinematic as a plain flat-coloured shape at its final time,
 * place and size (a disc for the array, lines for the qi threads, cubes for the debris, a cylinder for the pillar, discs for the
 * vortex, a player-sized box for the soul, a round card for the orb and the star, rings for the shockwave and the renewal), so the
 * timing and the camera can be judged before any of the real art (step B2) exists. Drawn in the world for everyone who sees the
 * player, after the translucent pass, see-through and not writing depth. Everything is a function of the elapsed tick and the seed.
 */
public final class GreyBox {
	/** The element kinds a sequence may name. */
	public static final Set<String> KINDS = Set.of("wisps", "array", "ring_race", "ground_glow", "threads", "debris", "cracks", "ribbons", "streaks",
			"slashes", "soul_glow", "pillar", "dust_ring", "mushroom", "veins", "vortex", "tendrils", "soul", "orb", "prism", "star", "flare",
			"suck_in", "shockwave", "fire_wall", "vortex_ring", "eye", "sign", "merge_glow", "aura_rings", "pillar_violet", "nebula", "renewal", "aura");

	// --- Where the soul and the star are (the camera's anchors too) ---

	/** Where the soul's feet are at {@code t}: small on the head, up into the pillar, standing above the body, sunk back into the head. */
	public static Vec3 soulFeet(CinematicClient.Playing p, float t) {
		double y;
		if (t < 400) y = 1.3 + 0.25 * Ease.SMOOTH.between(t, 365, 400);
		else if (t < 425) y = Mth.lerp(Ease.CUBIC_IN_OUT.between(t, 400, 425), 1.55, 2.4);
		else if (t < 650) y = 2.4;
		else y = Mth.lerp(Ease.CUBIC_IN.between(t, 650, 672), 2.4, 0.9);
		return p.at.add(0, y, 0);
	}

	/** The soul's size: a third at first, life size from tick 425, collapsed into the orb by 458, back from 590, a third as it sinks. */
	static float soulScale(float t) {
		if (t < 400) return 0.3f;
		if (t < 425) return Mth.lerp(Ease.SMOOTH.between(t, 400, 425), 0.3f, 1f);
		if (t < 450) return 1;
		if (t < 590) return Math.max(0, 1 - Ease.CUBIC_IN.between(t, 450, 458));
		if (t < 650) return 1;
		return Mth.lerp(Ease.CUBIC_IN.between(t, 650, 672), 1f, 0.3f);
	}

	/** The star: born where the soul's orb was, rising into the vortex's heart (its core, 7 blocks under the top at 40 blocks' height) until the blast. */
	public static Vec3 starPos(CinematicClient.Playing p, float t) {
		Vec3 from = soulFeet(p, 450).add(0, 0.9, 0);
		double top = p.probe.topY() - 7 * Math.max(1, (p.probe.topY() - p.probe.groundY()) / 40);
		return new Vec3(from.x, Mth.lerp(Ease.CUBIC_IN_OUT.between(t, 470, 520), from.y, Math.max(from.y + 8, top)), from.z);
	}

	// --- Drawing ---

	private static final int GOLD = 0xFFF3A232, CREAM = 0xFFFDFBD4, ORANGE = 0xFFE6810E, VIOLET = 0xFF7B5EB4, LAVENDER = 0xFFA894E9;

	/**
	 * The stand-ins still left (every kind {@link HeroFx} doesn't draw yet), drawn into the effects' layer after the real elements (from
	 * HeroFx#paint), so the storm's cloud deck doesn't hide them; blended "over", which is the layer's premultiplied blending.
	 */
	public static void render(WorldRenderContext context) {
		if (CinematicClient.playing().isEmpty()) return;
		Camera cam = context.camera();
		Draw d = new Draw(context.matrixStack().last().pose(), cam);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		d.buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		boolean drawn = false;
		try {
			for (CinematicClient.Playing p : CinematicClient.playing()) {
				float t = p.elapsed(context.tickDelta());
				for (Sequence.Element e : p.seq.elements) {
					if (t < e.from() || t >= e.to() || HeroFx.KINDS.contains(e.kind())) continue; // built for real: HeroFx draws it
					element(d, p, e, t);
				}
			}
			if (d.count > 0) {
				Tesselator.getInstance().end();
				drawn = true;
			}
		} finally {
			// Nothing in sight this frame, or a shape that failed (CinematicClient#safely ends the film): the shared builder let go unfinished,
			// so the next user of it finds it free, and the state put back either way.
			if (!drawn && d.buf.building()) d.buf.end().release();
			RenderSystem.depthMask(true);
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
		}
	}

	/** A number of the element's (its params), or {@code fallback}. */
	private static float param(Sequence.Element e, int i, float fallback) {
		return i < e.params().length ? e.params()[i] : fallback;
	}

	private static void element(Draw d, CinematicClient.Playing p, Sequence.Element e, float t) {
		float lt = t - e.from(), life = e.to() - e.from(), u = lt / life;
		float fade = Math.min(1, Math.min(lt / 3f, (life - lt) / 5f)); // eased in over 3 ticks, out over 5: nothing pops
		Vec3 o = p.at;
		double ground = p.probe.groundY() + 0.03, top = p.probe.topY();
		// What hangs at the pillar's top (the mushroom, the vortex, the star, the nebula) grows with its height: as big to the eye under high
		// clouds as under a low ceiling (the shapes are sized for a pillar 40 blocks tall).
		float s = Math.max(1, (float) ((top - ground) / 40));
		int c = e.color();
		switch (e.kind()) {
			case "wisps" -> {
				for (int i = 0; i < 14; i++) {
					double a = h(p.seed, i, 1) * Math.PI * 2, r = 0.8 + h(p.seed, i, 2) * 1.8;
					double rise = ((lt / 40f + h(p.seed, i, 3)) % 1) * 1.6;
					Vec3 base = o.add(Math.cos(a) * r, rise, Math.sin(a) * r);
					d.line(base, base.add(0, 0.5, 0), 0.05f, c, 0.6f * fade);
				}
			}
			case "array" -> {
				float grow = Ease.CUBIC_OUT.between(lt, 0, 20), r = 3 * grow, spin = lt * 0.06f;
				d.disc(o.x, ground, o.z, r, c, 0.28f * fade);
				d.ring(o.x, ground + 0.01, o.z, r * 0.78f, r * 0.84f, c, 0.7f * fade);
				d.ring(o.x, ground + 0.01, o.z, r * 0.94f, r, c, 0.8f * fade);
				for (int i = 0; i < 12; i++) { // the glyphs on its rim, the inner band turning one way and the outer the other
					double a1 = spin + i * Math.PI / 6, a2 = -spin + (i + 0.5) * Math.PI / 6;
					d.square(o.x + Math.cos(a1) * r * 0.66, ground + 0.02, o.z + Math.sin(a1) * r * 0.66, 0.16f * grow, c, 0.8f * fade);
					d.square(o.x + Math.cos(a2) * r * 0.89, ground + 0.02, o.z + Math.sin(a2) * r * 0.89, 0.13f * grow, CREAM, 0.7f * fade);
				}
			}
			case "ring_race" -> {
				float r = Mth.lerp(Ease.CUBIC_OUT.apply(u), param(e, 0, 3), param(e, 1, 14));
				d.ring(o.x, ground + 0.02, o.z, r - 0.25f, r + 0.25f, c, 0.8f * (1 - u) * fade);
			}
			case "ground_glow" -> d.disc(o.x, ground + 0.015, o.z, param(e, 0, 6), c, param(e, 1, 0.22f) * fade);
			case "threads" -> {
				int n = (int) param(e, 0, 32);
				Vec3 chest = o.add(0, 0.6, 0);
				for (int i = 0; i < n; i++) {
					double a = h(p.seed, i, 4) * Math.PI * 2, far = 20 + h(p.seed, i, 5) * 20, high = 40 + h(p.seed, i, 6) * 30;
					Vec3 start = o.add(Math.cos(a) * far, high, Math.sin(a) * far);
					Vec3 bend = o.add(Math.cos(a + 0.6) * far * 0.5, high * 0.35, Math.sin(a + 0.6) * far * 0.5);
					// Each falls from its own moment in the first quarter, its streak half a curve long and half a block wide: seen against the sky
					// from far below (shot 4 looks up at them before tilting down).
					float head = Mth.clamp((lt - h(p.seed, i, 7) * life * 0.25f) / (life * 0.5f), 0, 1);
					d.curve(start, bend, chest, Math.max(0, head - 0.45f), head, 0.5f, i % 3 == 0 ? CREAM : c, 0.9f * fade);
				}
			}
			case "debris" -> {
				for (int i = 0; i < 24; i++) {
					double a = h(p.seed, i, 8) * Math.PI * 2, r = 4 + h(p.seed, i, 9) * 6, size = 0.4 + h(p.seed, i, 10) * 0.5;
					float lift = Ease.CUBIC_OUT.between(t, e.from() + h(p.seed, i, 11) * 60, 528) * (2 + h(p.seed, i, 12) * 4);
					float pushed = Ease.EXPO_OUT.between(t, 528, 545) * 2.5f; // the blast throws them out, then they hang
					double rr = r + pushed;
					double drift = Math.max(0, t - 545) * 0.01;
					d.box(o.x + Math.cos(a + drift * 0.2) * rr, ground + lift + size / 2 + drift, o.z + Math.sin(a + drift * 0.2) * rr, (float) size / 2, c, 0.85f * fade);
				}
			}
			case "cracks" -> {
				for (int i = 0; i < 10; i++) {
					double a = h(p.seed, i, 13) * Math.PI * 2;
					float len = 1.5f + Ease.CUBIC_OUT.apply(u) * (6 + h(p.seed, i, 14) * 4);
					d.line(new Vec3(o.x + Math.cos(a) * 1.2, ground + 0.02, o.z + Math.sin(a) * 1.2), new Vec3(o.x + Math.cos(a) * len, ground + 0.02, o.z + Math.sin(a) * len), 0.12f, c, 0.85f * fade);
				}
			}
			case "ribbons" -> {
				Vec3 chest = o.add(0, 0.6, 0);
				for (int side = -1; side <= 1; side += 2) {
					for (int i = 0; i < 6; i++) {
						Vec3 start = CinematicCamera.local(p, o, new float[] {side * 14, 0.5f + i * 0.5f, -4 + i * 1.6f});
						Vec3 bend = CinematicCamera.local(p, o, new float[] {side * 6, 0.4f, 2 + i * 0.6f});
						float head = Ease.CUBIC_IN_OUT.apply(Mth.clamp(u * 1.3f - i * 0.05f, 0, 1));
						d.curve(start, bend, chest, Math.max(0, head - 0.5f), head, 0.18f, i % 2 == 0 ? c : CREAM, 0.85f * fade);
					}
				}
			}
			case "streaks" -> {
				Vec3 chest = o.add(0, 0.6, 0);
				for (int i = 0; i < 40; i++) {
					double a = h(p.seed, i, 15) * Math.PI * 2, el = (h(p.seed, i, 16) - 0.3) * 1.2;
					float k = (lt * 0.08f + h(p.seed, i, 17)) % 1; // each runs in from 6 blocks and starts again
					double r = 6 * (1 - k);
					Vec3 at = chest.add(Math.cos(a) * Math.cos(el) * r, Math.sin(el) * r, Math.sin(a) * Math.cos(el) * r);
					d.line(at, at.add(chest.subtract(at).normalize().scale(-0.9)), 0.06f, i % 4 == 0 ? CREAM : c, 0.9f * fade);
				}
			}
			case "slashes" -> {
				for (int k = 0; k < 2; k++) {
					float at = param(e, k, k == 0 ? 5 : 25);
					float f = 1 - Math.abs(lt - at - 3) / 3f;
					if (f <= 0) continue;
					Vec3 a = CinematicCamera.local(p, o, new float[] {k == 0 ? -3 : 3, 2.6f, 0.8f});
					Vec3 b = CinematicCamera.local(p, o, new float[] {k == 0 ? 3 : -3, -0.4f, 0.8f});
					d.line(a, b, 0.22f, CREAM, f * fade);
				}
			}
			case "soul_glow" -> {
				float pulse = 0;
				for (int k = 0; k < 3; k++) pulse = Math.max(pulse, (0.5f + k * 0.25f) * Math.max(0, 1 - Math.abs(lt - 5 - k * 10) / 5f));
				d.box(o.x, o.y + 0.6, o.z, 0.3f, c, (0.25f + 0.6f * pulse) * fade);
			}
			case "pillar" -> {
				float r = param(e, 0, 2f) * Ease.EXPO_OUT.between(lt, 0, 6);
				d.cylinder(o.x, ground, o.z, top, r, c, 0.7f * fade);
				d.cylinder(o.x, ground, o.z, top, r * 0.4f, CREAM, 0.9f * fade);
			}
			case "dust_ring" -> {
				float r = 1 + Ease.CUBIC_OUT.apply(u) * 8;
				d.band(o.x, ground, o.z, r, 0.8f * (1 - u), c, 0.6f * (1 - u) * fade);
			}
			case "mushroom" -> {
				float r = param(e, 0, 9) * s * Ease.EXPO_OUT.between(lt, 0, 15);
				d.disc(o.x, top - 0.3, o.z, r, c, 0.55f * fade);
				d.ring(o.x, top - 0.6, o.z, r * 0.8f, r, CREAM, 0.6f * fade);
			}
			case "veins" -> {
				for (int i = 0; i < 12; i++) {
					double a = i * Math.PI / 6 + h(p.seed, i, 18) * 0.4;
					float grow = Mth.clamp(u * 1.4f - h(p.seed, i, 19) * 0.3f, 0, 1);
					Vec3 a0 = new Vec3(o.x, top - 0.2, o.z), a1 = a0.add(Math.cos(a) * 9 * s * grow, 0, Math.sin(a) * 9 * s * grow);
					d.line(a0, a1, 0.18f * s, c, 0.85f * fade);
					for (int b = -1; b <= 1; b += 2) { // a branch off each, a generation thinner
						Vec3 m = a0.add(Math.cos(a) * 5 * s * grow, 0, Math.sin(a) * 5 * s * grow);
						double ab = a + b * 0.6;
						d.line(m, m.add(Math.cos(ab) * 4 * s * grow, 0, Math.sin(ab) * 4 * s * grow), 0.1f * s, c, 0.75f * fade);
					}
				}
			}
			case "vortex" -> {
				float open = Ease.CUBIC_OUT.between(lt, 0, 25);
				int[] colours = {0xFF662B2B, 0xFF9F3C39, 0xFF653B47};
				for (int k = 0; k < 3; k++) {
					float r = (14 - k * 4) * s * open, spin = lt * (0.02f + k * 0.015f) * (k % 2 == 0 ? 1 : -1);
					d.spiral(o.x, top - (2 + k * 2) * s, o.z, r, spin, colours[k], 0.45f * fade);
				}
				d.card(new Vec3(o.x, top - 7 * s, o.z), 2.5f * s * open, 0xFFF8AFB2, 0.9f * fade);
			}
			case "tendrils" -> { // from the vortex's rim down to a ring about 12 blocks round the player
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4 + lt * 0.01;
					Vec3 from = new Vec3(o.x + Math.cos(a) * 8 * s, top - 4 * s, o.z + Math.sin(a) * 8 * s);
					Vec3 to = new Vec3(o.x + Math.cos(a + 0.3) * 12, ground, o.z + Math.sin(a + 0.3) * 12);
					double out = Math.max(8 * s, 12) + 4;
					Vec3 bend = new Vec3(o.x + Math.cos(a + 0.15) * out, (from.y + to.y) / 2, o.z + Math.sin(a + 0.15) * out);
					d.curve(from, bend, to, 0, Ease.CUBIC_OUT.apply(Math.min(1, u * 2)), 0.5f, c, 0.8f * fade);
				}
			}
			case "soul" -> {
				float size = soulScale(t);
				if (size <= 0.01f) return;
				int col = t < 590 ? lerpColour(VIOLET, GOLD, Ease.SMOOTH.between(t, 405, 425)) : lerpColour(GOLD, LAVENDER, Ease.SMOOTH.between(t, 630, 650));
				Vec3 feet = soulFeet(p, t);
				float alpha = t >= 650 ? 1 - Ease.CUBIC_IN.between(t, 660, 675) : 1;
				d.boxSized(feet.x, feet.y + 0.9 * size, feet.z, 0.3f * size, 0.9f * size, 0.15f * size, p.facing, col, 0.65f * alpha * fade);
				// Two eyes on its face (closed, thin lines, until the eye has opened in shot 20; open squares after), so a close shot reads as a face.
				for (int side = -1; side <= 1; side += 2) {
					Vec3 eye = CinematicCamera.local(p, feet, new float[] {side * 0.1f * size, 1.56f * size, 0.16f * size});
					d.flatQuad(eye, p.facing, 0.05f * size, (t < 595 ? 0.01f : 0.04f) * size, 0xFF3A2A55, 0.9f * alpha * fade);
				}
			}
			case "orb" -> {
				Vec3 at = soulFeet(p, 450).add(0, 0.9, 0);
				d.card(at, Mth.lerp(u, 1.0f, 0.3f), CREAM, 0.95f * fade);
			}
			case "prism" -> {
				Vec3 at = soulFeet(p, 450).add(0, 0.9, 0);
				int[] rainbow = {0xFFFF5050, 0xFFFFB040, 0xFFFFFF60, 0xFF60FF80, 0xFF60A0FF, 0xFFB060FF};
				for (int k = 0; k < 6; k++) {
					double a = k * Math.PI / 3;
					d.card(at.add(Math.cos(a) * 1.6 * u, Math.sin(a) * 1.6 * u, 0), 0.5f, rainbow[k], 0.85f * (1 - u) * fade);
				}
			}
			case "star" -> d.card(starPos(p, t), Mth.lerp(Ease.CUBIC_IN.apply(u), 0.6f, 1.8f) * s, CREAM, 0.95f * fade);
			case "flare" -> {
				Vec3 at = starPos(p, t);
				float len = Mth.lerp(Ease.CUBIC_IN.apply(u), 12, 40) * s;
				d.flare(at, len, 0.18f * s, CREAM, 0.85f * fade);
			}
			case "suck_in" -> {
				Vec3 star = starPos(p, t);
				for (int i = 0; i < 30; i++) {
					double a = h(p.seed, i, 20) * Math.PI * 2, el = (h(p.seed, i, 21) - 0.5) * 2;
					double r = (18 * (1 - u) + 1) * s;
					Vec3 at = star.add(Math.cos(a) * Math.cos(el) * r, Math.sin(el) * r, Math.sin(a) * Math.cos(el) * r);
					d.line(at, at.add(star.subtract(at).normalize().scale(2.5 * s)), 0.08f * s, CREAM, 0.8f * fade);
				}
			}
			case "shockwave" -> {
				float r = 80 * Ease.CUBIC_OUT.apply(u);
				d.ring(o.x, ground + 0.05, o.z, Math.max(0, r - 1.5f), r, lerpColour(CREAM, ORANGE, u), 0.9f * (1 - u * 0.8f) * fade);
			}
			case "fire_wall" -> {
				float r = 70 * Ease.CUBIC_OUT.apply(u);
				d.band(o.x, ground, o.z, r, 6 * (1 - u * 0.5f), c, 0.55f * (1 - u) * fade);
			}
			case "vortex_ring" -> {
				float r = Mth.lerp(Ease.CUBIC_OUT.apply(u), 10, 60) * s;
				d.ring(o.x, top - 3 * s, o.z, r * 0.8f, r, c, 0.6f * (1 - u) * fade);
			}
			case "eye" -> {
				Vec3 face = soulFeet(p, t).add(0, 1.55, 0);
				Vec3 eye = CinematicCamera.local(p, face, new float[] {0.12f, 0, 0.17f});
				float open = Ease.CUBIC_OUT.between(lt, 0, 5);
				d.flatQuad(eye, p.facing, 0.12f, 0.01f + 0.06f * open, c, fade);
				if (lt > 6) d.flare(eye.add(CinematicCamera.local(p, Vec3.ZERO, new float[] {0, 0, 0.05f})), Mth.lerp(Ease.CUBIC_OUT.between(lt, 6, 20), 0.2f, 3), 0.04f, LAVENDER, 0.8f * fade);
			}
			case "sign" -> {
				Vec3 chest = CinematicCamera.local(p, soulFeet(p, t).add(0, 1.2, 0), new float[] {0, 0, 0.2f});
				d.flatQuad(chest, p.facing, 0.35f * Ease.BACK_OUT.apply(Math.min(1, u * 2)), 0.35f * Ease.BACK_OUT.apply(Math.min(1, u * 2)), c, 0.9f * fade);
			}
			case "merge_glow" -> {
				float swell = (float) Math.sin(Math.PI * Mth.clamp(lt / 20f, 0, 1));
				d.card(o.add(0, 1.0, 0), 0.4f + 1.4f * swell, CREAM, 0.8f * swell * fade);
			}
			case "aura_rings" -> {
				for (int k = 0; k < 3; k++) {
					float f = Mth.clamp((lt - k * 5) / 20f, 0, 1);
					if (f <= 0 || f >= 1) continue;
					float r = 0.5f + f * 6;
					d.ring(o.x, ground + 0.06, o.z, r - 0.2f, r, c, 0.8f * (1 - f) * fade);
				}
			}
			case "pillar_violet" -> {
				float r = 1.6f * Ease.EXPO_OUT.between(lt, 0, 5);
				d.cylinder(o.x, ground, o.z, top, r, c, 0.7f * fade);
				d.cylinder(o.x, ground, o.z, top, r * 0.4f, 0xFFF0E8FF, 0.9f * fade);
			}
			case "nebula" -> {
				int[] colours = {0xFF564B91, 0xFFAA5C67, 0xFF5F8695};
				for (int k = 0; k < 3; k++) d.spiral(o.x, top - (1 + k) * s, o.z, (30 - k * 8) * s * 0.5f, lt * 0.01f * (k + 1), colours[k], 0.35f * fade);
			}
			case "renewal" -> {
				float r = 200 * Ease.CUBIC_OUT.apply(u);
				d.ring(o.x, ground + 0.07, o.z, Math.max(0, r - 3), r, c, 0.85f * fade);
			}
			case "aura" -> {
				float pulse = 0.5f + 0.5f * (float) Math.sin(lt * 0.25);
				d.boxSized(o.x, o.y + 0.65, o.z, 0.55f, 0.7f, 0.55f, p.facing, c, (0.15f + 0.15f * pulse) * fade);
			}
			default -> { }
		}
	}

	/** A number in 0..1 from the seed, an index and a salt: the same seed always gives the same. */
	static float h(long seed, int i, int salt) {
		long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
		x ^= x >>> 31;
		x *= 0x7FB5D329728EA185L;
		x ^= x >>> 27;
		return (x >>> 40) / (float) (1L << 24);
	}

	static int lerpColour(int a, int b, float f) {
		int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
		return 0xFF000000 | Math.round(Mth.lerp(f, ar, br)) << 16 | Math.round(Mth.lerp(f, ag, bg)) << 8 | Math.round(Mth.lerp(f, ab, bb));
	}

	/** Flat-coloured shapes into one buffer, in camera-relative space. */
	private static final class Draw {
		final BufferBuilder buf = Tesselator.getInstance().getBuilder();
		final Matrix4f mat;
		final Vec3 cam;
		final Vector3f left, up, look;
		int count;

		Draw(Matrix4f mat, Camera camera) {
			this.mat = mat;
			this.cam = camera.getPosition();
			this.left = camera.getLeftVector();
			this.up = camera.getUpVector();
			this.look = camera.getLookVector();
		}

		void v(double x, double y, double z, int c, float a) {
			count++;
			buf.vertex(mat, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(c >> 16 & 255, c >> 8 & 255, c & 255, (int) (Mth.clamp(a, 0, 1) * 255)).endVertex();
		}

		void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int col, float al) {
			v(a.x, a.y, a.z, col, al);
			v(b.x, b.y, b.z, col, al);
			v(c.x, c.y, c.z, col, al);
			v(d.x, d.y, d.z, col, al);
		}

		/** A flat disc on the level of {@code y}. */
		void disc(double x, double y, double z, float r, int c, float a) {
			ring(x, y, z, 0, r, c, a);
		}

		/** A flat ring from {@code r0} to {@code r1}. */
		void ring(double x, double y, double z, float r0, float r1, int c, float a) {
			if (r1 <= 0 || a <= 0) return;
			int n = Mth.clamp((int) (r1 * 3), 24, 96);
			for (int i = 0; i < n; i++) {
				double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
				v(x + Math.cos(a0) * r0, y, z + Math.sin(a0) * r0, c, a);
				v(x + Math.cos(a0) * r1, y, z + Math.sin(a0) * r1, c, a);
				v(x + Math.cos(a1) * r1, y, z + Math.sin(a1) * r1, c, a);
				v(x + Math.cos(a1) * r0, y, z + Math.sin(a1) * r0, c, a);
			}
		}

		/** A spiral-armed disc (the vortex, the nebula): four arms as wedges, turned by {@code spin}. */
		void spiral(double x, double y, double z, float r, float spin, int c, float a) {
			if (r <= 0 || a <= 0) return;
			for (int arm = 0; arm < 4; arm++) {
				for (int s = 0; s < 10; s++) {
					float r0 = r * s / 10f, r1 = r * (s + 1) / 10f;
					double a0 = spin + arm * Math.PI / 2 + s * 0.18, a1 = a0 + 0.5;
					v(x + Math.cos(a0) * r0, y, z + Math.sin(a0) * r0, c, a);
					v(x + Math.cos(a0 + 0.18) * r1, y, z + Math.sin(a0 + 0.18) * r1, c, a);
					v(x + Math.cos(a1 + 0.18) * r1, y, z + Math.sin(a1 + 0.18) * r1, c, a);
					v(x + Math.cos(a1) * r0, y, z + Math.sin(a1) * r0, c, a);
				}
			}
			disc(x, y - 0.01, z, r, c, a * 0.4f);
		}

		/** A standing ring wall of radius {@code r}, {@code h} high (a dust ring, a wall of fire). */
		void band(double x, double y, double z, float r, float h, int c, float a) {
			if (r <= 0 || h <= 0 || a <= 0) return;
			int n = Mth.clamp((int) (r * 3), 24, 128);
			for (int i = 0; i < n; i++) {
				double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
				v(x + Math.cos(a0) * r, y, z + Math.sin(a0) * r, c, a);
				v(x + Math.cos(a1) * r, y, z + Math.sin(a1) * r, c, a);
				v(x + Math.cos(a1) * r, y + h, z + Math.sin(a1) * r, c, a * 0.2f);
				v(x + Math.cos(a0) * r, y + h, z + Math.sin(a0) * r, c, a * 0.2f);
			}
		}

		/** An upright cylinder from {@code y0} to {@code y1}. */
		void cylinder(double x, double y0, double z, double y1, float r, int c, float a) {
			if (r <= 0 || a <= 0) return;
			for (int i = 0; i < 20; i++) {
				double a0 = i * Math.PI * 2 / 20, a1 = (i + 1) * Math.PI * 2 / 20;
				v(x + Math.cos(a0) * r, y0, z + Math.sin(a0) * r, c, a);
				v(x + Math.cos(a1) * r, y0, z + Math.sin(a1) * r, c, a);
				v(x + Math.cos(a1) * r, y1, z + Math.sin(a1) * r, c, a);
				v(x + Math.cos(a0) * r, y1, z + Math.sin(a0) * r, c, a);
			}
		}

		/** A cube of half-size {@code s} about its centre. */
		void box(double x, double y, double z, float s, int c, float a) {
			boxSized(x, y, z, s, s, s, 0, c, a);
		}

		/** A box of half-sizes (side, height, depth) about its centre, turned to {@code yaw}. */
		void boxSized(double x, double y, double z, float hx, float hy, float hz, float yaw, int c, float a) {
			if (a <= 0) return;
			double r = Math.toRadians(yaw), cs = Math.cos(r), sn = Math.sin(r);
			Vec3[] p = new Vec3[8];
			for (int i = 0; i < 8; i++) {
				double lx = (i & 1) == 0 ? -hx : hx, ly = (i & 2) == 0 ? -hy : hy, lz = (i & 4) == 0 ? -hz : hz;
				p[i] = new Vec3(x + lx * cs - lz * sn, y + ly, z + lx * sn + lz * cs);
			}
			int shade = c;
			quad(p[0], p[1], p[3], p[2], shade, a);
			quad(p[4], p[5], p[7], p[6], shade, a);
			quad(p[0], p[1], p[5], p[4], shade, a * 0.8f);
			quad(p[2], p[3], p[7], p[6], shade, a);
			quad(p[0], p[2], p[6], p[4], shade, a * 0.9f);
			quad(p[1], p[3], p[7], p[5], shade, a * 0.9f);
		}

		/** A small flat square lying on the ground (a glyph). */
		void square(double x, double y, double z, float s, int c, float a) {
			quad(new Vec3(x - s, y, z - s), new Vec3(x + s, y, z - s), new Vec3(x + s, y, z + s), new Vec3(x - s, y, z + s), c, a);
		}

		/** A flat upright quad facing where the player faced (an eye's slit, a sign on the chest). */
		void flatQuad(Vec3 at, float yaw, float hw, float hh, int c, float a) {
			double r = Math.toRadians(yaw), rx = -Math.cos(r), rz = -Math.sin(r);
			Vec3 side = new Vec3(rx * hw, 0, rz * hw), upv = new Vec3(0, hh, 0);
			quad(at.subtract(side).subtract(upv), at.add(side).subtract(upv), at.add(side).add(upv), at.subtract(side).add(upv), c, a);
		}

		/** A round card facing the camera (an orb, a star, a glow): a 16-sided disc. */
		void card(Vec3 at, float r, int c, float a) {
			if (r <= 0 || a <= 0) return;
			for (int i = 0; i < 16; i++) {
				double a0 = i * Math.PI * 2 / 16, a1 = (i + 1) * Math.PI * 2 / 16;
				Vec3 p0 = at.add(axis(left, Math.cos(a0) * r)).add(axis(up, Math.sin(a0) * r));
				Vec3 p1 = at.add(axis(left, Math.cos(a1) * r)).add(axis(up, Math.sin(a1) * r));
				quad(at, p0, p1, at, c, a);
			}
		}

		/** A long thin streak across the screen's width through {@code at} (the star's flare). */
		void flare(Vec3 at, float len, float w, int c, float a) {
			Vec3 l = axis(left, len / 2), u = axis(up, w / 2);
			quad(at.subtract(l).subtract(u), at.add(l).subtract(u), at.add(l).add(u), at.subtract(l).add(u), c, a);
		}

		/** A line from {@code a} to {@code b} as a strip {@code w} wide, turned to face the camera. */
		void line(Vec3 a, Vec3 b, float w, int c, float al) {
			Vec3 d = b.subtract(a);
			Vec3 toCam = cam.subtract(a);
			Vec3 side = d.cross(toCam);
			if (side.lengthSqr() < 1.0E-8) return;
			side = side.normalize().scale(w / 2);
			quad(a.subtract(side), a.add(side), b.add(side), b.subtract(side), c, al);
		}

		/** The part from {@code u0} to {@code u1} of a curve from {@code a} bending toward {@code b} to {@code c}. */
		void curve(Vec3 a, Vec3 b, Vec3 c, float u0, float u1, float w, int col, float al) {
			if (u1 <= u0 || al <= 0) return;
			int n = 12;
			Vec3 prev = bezier(a, b, c, u0);
			for (int i = 1; i <= n; i++) {
				float u = u0 + (u1 - u0) * i / n;
				Vec3 next = bezier(a, b, c, u);
				line(prev, next, w, col, al * (0.4f + 0.6f * i / n)); // brightest at its head
				prev = next;
			}
		}

		private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, float u) {
			double k = 1 - u;
			return a.scale(k * k).add(b.scale(2 * k * u)).add(c.scale(u * u));
		}

		private static Vec3 axis(Vector3f v, double s) {
			return new Vec3(v.x() * s, v.y() * s, v.z() * s);
		}
	}

	private GreyBox() {}
}
