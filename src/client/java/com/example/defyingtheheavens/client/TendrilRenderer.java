package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A tendril (a SpellConstruct of Form TENDRIL: Void Withering's grasping roots, the user's ask, 2026-10-24: "having branches actually reach
 * out and grab things would be cool"), drawn by hand, as RiftRenderer draws a tear: a root of the void tree's black wood lashing from where
 * it stands (the rift its tree hangs from) down to the being it holds (SpellConstruct#target), wherever that is hauled to, and coiled round
 * it. Its bark is ridged and gnarled and thorned; threads of the life it drinks glow up it in swells, and the life itself wells up it from
 * its catch to the rift. It lashes out ({@link #LASH}), winds round its catch ({@link #COIL}), writhes as it holds, and draws back up as it
 * goes (SpellConstruct#FADE). Its size is its girth (its realm's greatness).
 */
final class TendrilRenderer {
	/** Ticks it takes to lash out to its catch, and then to wind round it. */
	static final float LASH = 7, COIL = 6;
	/** The sides of its round. */
	private static final int SIDES = 7;
	/** How many times it winds round its catch. */
	private static final float TURNS = 2.2f;
	/** The void tree's wood and the life it drinks (tools/spell-art Void: VOID_BLACK, VOID_BARK, VOID_VIOLET, VOID_SHEEN, LIFE, LIFE_WHITE). */
	private static final float[] BLACK = SpellRender.rgb(0x07040C), BARK = SpellRender.rgb(0x1E1229), VIOLET = SpellRender.rgb(0x46285E),
			SHEEN = SpellRender.rgb(0x8662B4), LIFE = SpellRender.rgb(0x86FFAE), LIFE_WHITE = SpellRender.rgb(0xEAFFF2), VOID = SpellRender.rgb(SpellConstruct.VOID);
	/** Where its sheen comes from (it is lit by nothing but its own dim light: a little more on top). */
	private static final Vec3 LIGHT = new Vec3(0.35, 1, 0.25).normalize();

	/**
	 * How a tendril of its kind looks: its body's colours from its darkest to its sheen (four stops), the light up it (its threads, their
	 * brightest, its welling drops), whether it is thorned, how gnarled its round is, how fast its threads wind round it, the light drawn out
	 * of its catch, whether it is a chain of links (no threads: its light only wells along it), and how it first goes from where it stands (a
	 * share of its reach, down from a rift above, or up out of the ground). The void tree's root ({@link #ROOT}); Immortal-Binding Golden
	 * Ropes' rope of braided gold ({@link #ROPE}, Sacred Vessel, 2026-10-24: smooth, unthorned, its two strands' light winding fast round it as
	 * a braid's); a chain of hell's black iron, ghost fire running along it ({@link #CHAIN}, Chains of Hell, 2026-10-25: up out of the way to
	 * hell at its caster's feet).
	 */
	private record Style(float[][] ramp, float[] light, float[] lightWhite, float[] catchGlow, boolean thorns, float knot, float wind, boolean links, float rise) {}

	private static final Style ROOT = new Style(new float[][] {BLACK, BARK, VIOLET, SHEEN}, LIFE, LIFE_WHITE, VOID, true, 0.16f, 0.75f, false, -0.42f);
	private static final Style ROPE = new Style(new float[][] {SpellRender.rgb(0x4A2E08), SpellRender.rgb(0xA87414), SpellRender.rgb(0xE8B83A), SpellRender.rgb(0xFFF0B0)},
			SpellRender.rgb(0xFFD866), SpellRender.rgb(0xFFFBE6), SpellRender.rgb(0xFFC84A), false, 0.04f, 2.4f, false, -0.42f);
	private static final Style CHAIN = new Style(new float[][] {SpellRender.rgb(0x111217), SpellRender.rgb(0x2A2D36), SpellRender.rgb(0x505664), SpellRender.rgb(0x9098A8)},
			SpellRender.rgb(0x56EEE2), SpellRender.rgb(0xC4FFF6), SpellRender.rgb(0x18A6A8), false, 0, 0, true, 0.32f);

	/**
	 * A living root of the woods' (LIVING_ROOT, the user's ask, 2026-10-25: the wood spells' trees drained their foes with nothing to show why):
	 * brown bark with a green sheen, thorned, reaching up out of the ground in a low arc, the life it drinks running green down it to its root.
	 */
	private static final Style LIVING = new Style(new float[][] {SpellRender.rgb(0x24160B), SpellRender.rgb(0x553820), SpellRender.rgb(0x84653B), SpellRender.rgb(0xA6C46A)},
			SpellRender.rgb(0x7CFF6E), SpellRender.rgb(0xE6FFD8), SpellRender.rgb(0x46E07A), true, 0.14f, 0.75f, false, 0.22f);

	private static Style style(SpellConstruct tendril) {
		return switch (tendril.kind()) {
			case GOLDEN_ROPE -> ROPE;
			case HELL_CHAIN -> CHAIN;
			case LIVING_ROOT -> LIVING;
			default -> ROOT;
		};
	}

	private TendrilRenderer() {}

	/** A point of its path: where, how thick there, how far along it from its root (blocks). */
	private record Point(Vec3 at, float radius, float along) {}

	/** Draws {@code root} (the pose at its place). */
	static void render(SpellConstruct root, float partialTick, PoseStack pose, MultiBufferSource parent) {
		Entity held = root.target();
		if (held == null) return;
		float time = root.tickCount + partialTick;
		// Let go, it unwinds from its catch over the first half of its fade, then draws back up over the second.
		float going = Mth.clamp((root.life() - time) / SpellConstruct.FADE, 0, 1);
		float lash = SpellRender.easeOut(Mth.clamp(time / LASH, 0, 1)) * Mth.clamp(going * 2, 0, 1);
		float coil = SpellRender.ease(Mth.clamp((time - LASH) / COIL, 0, 1)) * Mth.clamp(going * 2 - 1, 0, 1);
		if (lash <= 0.01f) return;
		long seed = root.getId() * 0x9E3779B97F4A7C15L;
		Style style = style(root);
		float girth = root.size();
		float thick = 0.42f * girth, thin = 0.13f * girth, coiled = thin * 0.9f;
		Vec3 feet = held.getPosition(partialTick).subtract(root.getPosition(partialTick));
		float width = held.getBbWidth(), height = held.getBbHeight();

		// Its coil: a helix round its catch's body from its chest down toward its knees, begun on the side toward the rift.
		double toward = Math.atan2(-feet.z, -feet.x), round = width * 0.5 + coiled + 0.05;
		double top = height * 0.66, bottom = height * 0.16;
		Vec3 grip = feet.add(Math.cos(toward) * round, top, Math.sin(toward) * round);
		Vec3 helixWay = new Vec3(-Math.sin(toward) * round * TURNS * Mth.TWO_PI, bottom - top, Math.cos(toward) * round * TURNS * Mth.TWO_PI).normalize();

		// Its reach: down out of the rift first (or up out of the ground), then round to come at its catch along its coil's way, writhing as it holds.
		double reach = grip.length();
		Vec3 side = new Vec3(-grip.z, 0, grip.x);
		side = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
		float writhe = 1 - 0.6f * coil;
		Vec3 c1 = new Vec3(grip.x * 0.18, reach * style.rise(), grip.z * 0.18)
				.add(side.scale(Mth.sin(time * 0.13f + hash(seed, 1) * 6) * reach * 0.07 * writhe)).add(0, Mth.sin(time * 0.09f + hash(seed, 2) * 6) * reach * 0.03, 0);
		Vec3 c2 = grip.subtract(helixWay.scale(reach * 0.3)).add(0, reach * 0.12, 0)
				.add(side.scale(Mth.sin(time * 0.17f + hash(seed, 3) * 6) * reach * 0.05 * writhe));

		// Its path: its reach (as far as it has lashed out), then its coil (as far as it has wound).
		List<Point> path = new ArrayList<>();
		int pieces = Mth.clamp((int) (reach / 1.1), 14, 60);
		float along = 0;
		Vec3 last = null;
		for (int i = 0; i <= pieces; i++) {
			float u = (float) i / pieces;
			if (u > lash) {
				u = lash;
				i = pieces;
			}
			Vec3 p = bezier(Vec3.ZERO, c1, c2, grip, u);
			if (last != null) along += (float) p.distanceTo(last);
			path.add(new Point(p, Mth.lerp((float) Math.pow(u, 0.8), thick, thin), along));
			last = p;
		}
		int reachEnds = path.size();
		if (lash >= 0.999f && coil > 0.01f) {
			int turns = Mth.clamp((int) (TURNS * 14), 12, 40);
			for (int i = 1; i <= turns; i++) {
				float v = (float) i / turns;
				if (v > coil) break;
				double angle = toward + v * TURNS * Mth.TWO_PI;
				Vec3 p = feet.add(Math.cos(angle) * round, Mth.lerp(v, (float) top, (float) bottom), Math.sin(angle) * round);
				along += (float) p.distanceTo(last);
				path.add(new Point(p, coiled * (1 - 0.35f * v), along));
				last = p;
			}
		}
		if (path.size() < 2) return;

		MultiBufferSource buffers = SpellBuffers.begin(parent);
		try {
			Matrix4f m = pose.last().pose();
			Frames frames = frames(path);
			if (style.links()) links(buffers.getBuffer(Types.BODY), m, seed, style, path, girth);
			else bark(buffers.getBuffer(Types.BODY), m, seed, style, path, frames, reachEnds);
			life(buffers.getBuffer(SpellRender.flatGlow()), m, seed, style, path, frames, time, coil, held, feet, width, height);
		} finally {
			SpellBuffers.end();
		}
	}

	/** A cubic Bezier's point {@code u} of the way from {@code a} to {@code d}. */
	private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double u) {
		double v = 1 - u;
		return a.scale(v * v * v).add(b.scale(3 * v * v * u)).add(c.scale(3 * v * u * u)).add(d.scale(u * u * u));
	}

	/** A number from 0 to 1, the same every frame for the same seed and index. */
	private static float hash(long seed, int i) {
		return SpellRender.hash(seed + i * 0x632BE59BD9B4E019L);
	}

	/** Its round's frame at each point (carried along it without twisting): the way it runs, and two ways across it. */
	private record Frames(Vec3[] way, Vec3[] across, Vec3[] over) {}

	private static Frames frames(List<Point> path) {
		int n = path.size();
		Vec3[] way = new Vec3[n], across = new Vec3[n], over = new Vec3[n];
		for (int i = 0; i < n; i++) {
			Vec3 a = path.get(Math.max(0, i - 1)).at(), b = path.get(Math.min(n - 1, i + 1)).at();
			Vec3 t = b.subtract(a);
			way[i] = t.lengthSqr() < 1.0e-10 ? (i > 0 ? way[i - 1] : new Vec3(0, -1, 0)) : t.normalize();
		}
		Vec3 start = Math.abs(way[0].y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		across[0] = way[0].cross(start).normalize();
		for (int i = 0; i < n; i++) {
			Vec3 prev = i == 0 ? across[0] : across[i - 1];
			Vec3 a = prev.subtract(way[i].scale(prev.dot(way[i])));
			across[i] = a.lengthSqr() < 1.0e-10 ? prev : a.normalize();
			over[i] = way[i].cross(across[i]);
		}
		return new Frames(way, across, over);
	}

	/** Its round at point {@code i}, side {@code j}: gnarled, swelling and shrinking a little along it. */
	private static Vec3 ring(Style style, List<Point> path, Frames f, long seed, int i, int j) {
		Point p = path.get(i);
		double angle = Mth.TWO_PI * j / SIDES + p.along() * 0.08;
		float knot = 1 + style.knot() * (hash(seed, (int) (p.along() / 1.4f) * 31 + j) - 0.5f) + style.knot() * 0.6f * Mth.sin(p.along() * 0.9f + j);
		return p.at().add(f.across()[i].scale(Math.cos(angle) * p.radius() * knot)).add(f.over()[i].scale(Math.sin(angle) * p.radius() * knot));
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float[] rgb, float alpha) {
		vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(rgb[0], rgb[1], rgb[2], Mth.clamp(alpha, 0, 1)).endVertex();
	}

	/** Its wood: the round, ridged and gnarled, lit a little on top; thorns off its reach; its growing end drawn to a point. */
	private static void bark(VertexConsumer vc, Matrix4f m, long seed, Style style, List<Point> path, Frames f, int reachEnds) {
		int n = path.size();
		for (int i = 0; i < n - 1; i++) {
			for (int j = 0; j < SIDES; j++) {
				Vec3 a = ring(style, path, f, seed, i, j), b = ring(style, path, f, seed, i, j + 1), c = ring(style, path, f, seed, i + 1, j + 1), d = ring(style, path, f, seed, i + 1, j);
				Vec3 out = a.add(b).add(c).add(d).scale(0.25).subtract(path.get(i).at().add(path.get(i + 1).at()).scale(0.5));
				float lit = out.lengthSqr() < 1.0e-8 ? 0.5f : (float) Math.max(0, out.normalize().dot(LIGHT));
				float ridge = hash(seed, i * 13 + j * 7 + 5);
				float[] colour = shade(style, 0.16f + 0.3f * ridge + 0.42f * lit);
				vertex(vc, m, a, colour, 1);
				vertex(vc, m, b, colour, 1);
				vertex(vc, m, c, colour, 1);
				vertex(vc, m, d, colour, 1);
			}
			// A thorn now and then off its reach, hooked back toward the rift.
			if (style.thorns() && i < reachEnds - 1 && i > 1 && hash(seed, i * 5 + 77) < 0.28f) {
				Point p = path.get(i);
				int j = (int) (hash(seed, i * 5 + 78) * SIDES);
				Vec3 base0 = ring(style, path, f, seed, i, j), base1 = ring(style, path, f, seed, i + 1, j);
				Vec3 outward = base0.subtract(p.at()).normalize();
				Vec3 tip = base0.add(outward.scale(p.radius() * 1.4 + 0.15)).subtract(f.way()[i].scale(p.radius() * 0.9));
				float[] colour = shade(style, 0.22f);
				vertex(vc, m, base0, colour, 1);
				vertex(vc, m, base1, colour, 1);
				vertex(vc, m, tip, shade(style, 0.5f), 1);
				vertex(vc, m, tip, shade(style, 0.5f), 1);
			}
		}
		// Its end drawn to a point (while it lashes out, and its coil's last).
		int last = n - 1;
		Vec3 tip = path.get(last).at().add(f.way()[last].scale(path.get(last).radius() * 2.2));
		for (int j = 0; j < SIDES; j++) {
			Vec3 a = ring(style, path, f, seed, last, j), b = ring(style, path, f, seed, last, j + 1);
			vertex(vc, m, a, shade(style, 0.3f), 1);
			vertex(vc, m, b, shade(style, 0.3f), 1);
			vertex(vc, m, tip, shade(style, 0.55f), 1);
			vertex(vc, m, tip, shade(style, 0.55f), 1);
		}
	}

	/** How far apart a chain's links are along it, how long and how wide each is, and how thick its iron, at a girth of 1 (blocks). */
	private static final float LINK_STEP = 0.3f, LINK_LONG = 0.21f, LINK_WIDE = 0.12f, LINK_IRON = 0.035f;

	/**
	 * A chain's links along its path: each an oval of iron round it, every other one turned a quarter about the way it runs (as links hang),
	 * each overlapping the next; lit a little on top.
	 */
	private static void links(VertexConsumer vc, Matrix4f m, long seed, Style style, List<Point> path, float girth) {
		float total = path.get(path.size() - 1).along();
		float step = LINK_STEP * girth, along = LINK_LONG * girth, wide = LINK_WIDE * girth, iron = LINK_IRON * girth;
		int segments = 10;
		int k = 0;
		for (float at = step * 0.5f; at <= total; at += step, k++) {
			Vec3 middle = pointAlong(path, at).at();
			Vec3 way = pointAlong(path, Math.min(total, at + 0.05f)).at().subtract(pointAlong(path, Math.max(0, at - 0.05f)).at());
			way = way.lengthSqr() < 1.0e-10 ? new Vec3(0, 1, 0) : way.normalize();
			Vec3 a = way.cross(Math.abs(way.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize(), b = way.cross(a);
			Vec3 across = k % 2 == 0 ? a : b, normal = way.cross(across);
			float worn = 0.12f * hash(seed, k * 7 + 3);
			for (int s = 0; s < segments; s++) {
				double t0 = Mth.TWO_PI * s / segments, t1 = Mth.TWO_PI * (s + 1) / segments;
				Vec3 p0 = middle.add(way.scale(Math.cos(t0) * along)).add(across.scale(Math.sin(t0) * wide));
				Vec3 p1 = middle.add(way.scale(Math.cos(t1) * along)).add(across.scale(Math.sin(t1) * wide));
				// Its iron's round there: out from the link's middle, and across its face; each side lit as it faces the light.
				Vec3 out0 = p0.subtract(middle).normalize(), out1 = p1.subtract(middle).normalize();
				for (int q = 0; q < 4; q++) {
					double f0 = Mth.HALF_PI * q, f1 = Mth.HALF_PI * (q + 1), fm = (f0 + f1) / 2;
					Vec3 a0 = p0.add(out0.scale(Math.cos(f0) * iron)).add(normal.scale(Math.sin(f0) * iron)), b0 = p0.add(out0.scale(Math.cos(f1) * iron)).add(normal.scale(Math.sin(f1) * iron));
					Vec3 a1 = p1.add(out1.scale(Math.cos(f0) * iron)).add(normal.scale(Math.sin(f0) * iron)), b1 = p1.add(out1.scale(Math.cos(f1) * iron)).add(normal.scale(Math.sin(f1) * iron));
					float lit = (float) Math.max(0, out0.scale(Math.cos(fm)).add(normal.scale(Math.sin(fm))).dot(LIGHT));
					float[] c = shade(style, 0.14f + 0.6f * lit + worn);
					vertex(vc, m, a0, c, 1);
					vertex(vc, m, b0, c, 1);
					vertex(vc, m, b1, c, 1);
					vertex(vc, m, a1, c, 1);
				}
			}
		}
	}

	/** Its body from its darkest to its sheen ({@link Style#ramp}: the void tree's wood, the rope's gold), {@code t} of the way. */
	private static float[] shade(Style style, float t) {
		t = Mth.clamp(t, 0, 1);
		float[][] ramp = style.ramp();
		float[] lo, hi;
		float f;
		if (t < 0.4f) {
			lo = ramp[0];
			hi = ramp[1];
			f = t / 0.4f;
		} else if (t < 0.75f) {
			lo = ramp[1];
			hi = ramp[2];
			f = (t - 0.4f) / 0.35f;
		} else {
			lo = ramp[2];
			hi = ramp[3];
			f = (t - 0.75f) / 0.25f;
		}
		return new float[] {Mth.lerp(f, lo[0], hi[0]), Mth.lerp(f, lo[1], hi[1]), Mth.lerp(f, lo[2], hi[2])};
	}

	/**
	 * The life it drinks: two threads winding up it, brighter in swells that run up toward the rift; the life itself welling up it in glowing
	 * drops; and, once it has wound round its catch, the catch's own light drawn out of it, pulsing.
	 */
	private static void life(VertexConsumer vc, Matrix4f m, long seed, Style style, List<Point> path, Frames f, float time, float coil, Entity held, Vec3 feet, float width,
			float height) {
		float[] LIFE = style.light(), LIFE_WHITE = style.lightWhite(), VOID = style.catchGlow(); // its style's light (the void tree's: its life)
		int n = path.size();
		for (int thread = 0; thread < (style.links() ? 0 : 2); thread++) {
			for (int i = 0; i < n - 1; i++) {
				Point p = path.get(i), q = path.get(i + 1);
				Vec3 a = surface(style, p, f, i, thread), b = surface(style, q, f, i + 1, thread);
				Vec3 wa = thread(style, f, i, p, thread), wb = thread(style, f, i + 1, q, thread);
				float swellA = 0.35f + 0.65f * Math.max(0, Mth.sin(p.along() * 0.55f + time * 0.45f + thread * 2)), swellB = 0.35f + 0.65f * Math.max(0, Mth.sin(q.along() * 0.55f + time * 0.45f + thread * 2));
				float[] colourA = mix(LIFE, LIFE_WHITE, swellA * 0.6f), colourB = mix(LIFE, LIFE_WHITE, swellB * 0.6f);
				float wideA = 0.05f + p.radius() * 0.12f, wideB = 0.05f + q.radius() * 0.12f;
				vertex(vc, m, a.add(wa.scale(wideA)), colourA, 0.85f * swellA);
				vertex(vc, m, a.subtract(wa.scale(wideA)), colourA, 0.85f * swellA);
				vertex(vc, m, b.subtract(wb.scale(wideB)), colourB, 0.85f * swellB);
				vertex(vc, m, b.add(wb.scale(wideB)), colourB, 0.85f * swellB);
				// Its glow round it.
				vertex(vc, m, a.add(wa.scale(wideA * 4)), LIFE, 0.18f * swellA);
				vertex(vc, m, a.subtract(wa.scale(wideA * 4)), LIFE, 0.18f * swellA);
				vertex(vc, m, b.subtract(wb.scale(wideB * 4)), LIFE, 0.18f * swellB);
				vertex(vc, m, b.add(wb.scale(wideB * 4)), LIFE, 0.18f * swellB);
			}
		}
		// The life welling up it in drops, from its catch to the rift (along its whole length, coil and all).
		Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
		Vector3f right = facing.transform(new Vector3f(1, 0, 0)), up = facing.transform(new Vector3f(0, 1, 0));
		float total = path.get(n - 1).along();
		if (coil > 0.3f && total > 1) {
			for (int k = 0; k < 4; k++) {
				float at = total * (1 - ((time * 0.035f + k / 4f + hash(seed, 200 + k) * 0.1f) % 1f));
				Point p = pointAlong(path, at);
				float size = 0.25f + p.radius() * 0.9f;
				blot(vc, m, p.at(), right, up, size, LIFE_WHITE, 0.75f * coil);
				blot(vc, m, p.at(), right, up, size * 2.4f, LIFE, 0.22f * coil);
			}
			// Its catch's light, drawn out of it where it is held.
			Vec3 heart = feet.add(0, height * 0.5, 0);
			float pulse = 0.6f + 0.4f * Mth.sin(time * 0.5f);
			blot(vc, m, heart, right, up, Math.max(width, height * 0.5f) * 1.1f, VOID, 0.3f * coil * pulse);
			blot(vc, m, heart, right, up, Math.max(width, height * 0.5f) * 0.55f, LIFE, 0.35f * coil * pulse);
		}
	}

	/** A thread's point on its surface at point {@code i} (two threads winding round it, half a turn apart). */
	private static Vec3 surface(Style style, Point p, Frames f, int i, int thread) {
		double angle = p.along() * style.wind() + thread * Math.PI;
		return p.at().add(f.across()[i].scale(Math.cos(angle) * p.radius() * 1.04)).add(f.over()[i].scale(Math.sin(angle) * p.radius() * 1.04));
	}

	/** Across a thread there (round its surface), the way its band spreads. */
	private static Vec3 thread(Style style, Frames f, int i, Point p, int thread) {
		double angle = p.along() * style.wind() + thread * Math.PI;
		return f.across()[i].scale(-Math.sin(angle)).add(f.over()[i].scale(Math.cos(angle)));
	}

	/** Its point {@code at} blocks along it. */
	private static Point pointAlong(List<Point> path, float at) {
		for (int i = 0; i < path.size() - 1; i++) {
			Point p = path.get(i), q = path.get(i + 1);
			if (at <= q.along()) {
				float f = q.along() - p.along() < 1.0e-4f ? 0 : (at - p.along()) / (q.along() - p.along());
				return new Point(p.at().lerp(q.at(), f), Mth.lerp(f, p.radius(), q.radius()), at);
			}
		}
		return path.get(path.size() - 1);
	}

	/** A soft round glow {@code size} across at {@code at}, turned to the eye: brightest at its heart, fading to nothing at its edge. */
	private static void blot(VertexConsumer vc, Matrix4f m, Vec3 at, Vector3f right, Vector3f up, float size, float[] rgb, float alpha) {
		int sides = 8;
		for (int i = 0; i < sides; i++) {
			float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
			Vec3 p0 = at.add(right.x() * Mth.cos(a0) * size + up.x() * Mth.sin(a0) * size, right.y() * Mth.cos(a0) * size + up.y() * Mth.sin(a0) * size,
					right.z() * Mth.cos(a0) * size + up.z() * Mth.sin(a0) * size);
			Vec3 p1 = at.add(right.x() * Mth.cos(a1) * size + up.x() * Mth.sin(a1) * size, right.y() * Mth.cos(a1) * size + up.y() * Mth.sin(a1) * size,
					right.z() * Mth.cos(a1) * size + up.z() * Mth.sin(a1) * size);
			vertex(vc, m, at, rgb, alpha);
			vertex(vc, m, p0, rgb, 0);
			vertex(vc, m, p1, rgb, 0);
			vertex(vc, m, p1, rgb, 0);
		}
	}

	private static float[] mix(float[] a, float[] b, float t) {
		return new float[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
	}

	/** Its wood's render type: its colour per point, lit by nothing, both faces, hiding what is behind it (as RiftRenderer's void). */
	private static final class Types extends RenderType {
		static final RenderType BODY = create("defying_the_heavens_tendril", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 8192, false, true,
				CompositeState.builder()
						.setShaderState(RENDERTYPE_LIGHTNING_SHADER)
						.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
						.setCullState(NO_CULL)
						.setWriteMaskState(COLOR_DEPTH_WRITE)
						.createCompositeState(false));

		private Types(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
			super(name, format, mode, size, crumbling, sort, setup, clear);
		}
	}
}
