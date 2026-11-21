package com.example.defyingtheheavens.client.cinematic;

import com.example.defyingtheheavens.client.SoulRenderer;
import com.example.defyingtheheavens.client.fx.Fx;
import com.example.defyingtheheavens.client.fx.FxPost;
import com.example.defyingtheheavens.client.fx.FxTextures;
import com.example.defyingtheheavens.client.fx.HeightField;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The hero's elements drawn for real (the VFX plan's step B2; the first half, Acts 1 to 3): everything a sequence names of the kinds in
 * {@link #KINDS} is drawn here into the effects' layer (light, smoke and cloud: client/fx), and the torn-up ground's blocks into the
 * world; the kinds not built yet are still the grey box's ({@link GreyBox}). Everything is a function of the elapsed tick, the seed, the
 * start and the ground's heights read as it began, so it looks the same to every viewer and at any frame rate.
 * <ul>
 * <li>{@code storm_deck}: the storm's dark cloud deck overhead, carried from the tribulation (two layers drifting apart); the cloud
 * base lit orange round the pillar once it has struck.</li>
 * <li>{@code wisps}: warm embers and faint red wisps rising off the ground.</li>
 * <li>{@code array}: the runic array: a pool of light under it, flame swirling between its rings, its rings and runes turning one way and
 * its small circles the other, an eight-pointed star, and a red heart; it grows from nothing in a second.</li>
 * <li>{@code ring_race}: a band of gold flame racing out over the ground, flames standing up at its leading edge.</li>
 * <li>{@code ground_glow}: the light the gathering casts on the ground round the player, swelling with it and flaring as the pillar strikes.</li>
 * <li>{@code threads}: qi threads raining from high and far, wavy, white-hot cores with orange edges, and hair-thin streaks falling with them.</li>
 * <li>{@code ribbons}: the golden fan: ribbons sweeping in low from both sides to meet at a white-hot point on the player.</li>
 * <li>{@code cracks}: glowing cracks running out across the ground.</li>
 * <li>{@code debris}: copies of the real blocks round the player torn loose, rising and turning (drawn in the world, lit as the world is).</li>
 * <li>{@code streaks}: the absorption: streaks and sparks rushing into the player from every side.</li>
 * <li>{@code slashes}: two diagonal slashes of light across the frame.</li>
 * <li>{@code soul_glow}: the nascent soul's violet light showing through the body, three pulses on the heartbeat.</li>
 * <li>{@code pillar}: the pillar of light: a cream-white core, a gold body of flame streaming upward, an orange haze, tongues of flame
 * climbing it and licking out past its edge, specks of debris carried up in it; it shoots up in 6 ticks and thickens.</li>
 * <li>{@code pillar_rays}: gold rays bursting out round the pillar's foot as it strikes, and embers flung up along it.</li>
 * <li>{@code dust_ring}: dust thrown out over the ground round its foot.</li>
 * <li>{@code mushroom}: where it meets the cloud base (or the ceiling): a great mushroom of fire billowing out under it, pale gold at its
 * middle to deep orange, dark smoke at its rim; the cloud base round it lit orange (with the storm deck).</li>
 * <li>{@code veins}: gold veins crawling out across the cloud base (or the ceiling), each at its own pace, branching, brightening, then
 * fading.</li>
 * </ul>
 */
public final class HeroFx {
	public static final Set<String> KINDS = Set.of("storm_deck", "wisps", "array", "ring_race", "ground_glow", "threads", "ribbons", "cracks",
			"debris", "streaks", "slashes", "soul_glow", "pillar", "pillar_rays", "dust_ring", "mushroom", "veins");

	// The palette (the brief §3.4; the soul's violet the mod's own).
	private static final float[] EMBER = {0.92f, 0.36f, 0.10f}, WISP = {0.70f, 0.15f, 0.06f}, ORANGE = {0.902f, 0.506f, 0.055f},
			GOLD = {0.953f, 0.635f, 0.196f}, GOLD_LIGHT = {0.992f, 0.82f, 0.408f}, DEEP = {0.58f, 0.28f, 0.03f}, CREAM = {0.992f, 0.984f, 0.831f},
			DUST = {0.34f, 0.29f, 0.24f}, STORM = {0.125f, 0.118f, 0.14f}, STORM_LOW = {0.2f, 0.19f, 0.23f},
			VIOLET = {0.48f, 0.37f, 0.71f},
			LAVENDER = {0.66f, 0.58f, 0.91f},
			// Fire's body (covering) and its flame (light kept under white, so it stays orange).
			FIRE = {0.55f, 0.17f, 0.03f}, FLAME = {0.98f, 0.49f, 0.08f};

	/** What each cinematic needs worked out once: the ground's heights, the torn-up blocks, the veins' branches, the cracks' paths. */
	static final class State {
		final HeightField ground;
		final List<Piece> debris = new ArrayList<>();
		/**
		 * Vein segments, {@link #VEIN} numbers each: x0, z0, x1, z1 (from the pillar), distance from the root at each end, width, flicker
		 * seed, and its vein's pace and delay (each vein crawls out at its own speed, some setting off late: never a ring growing evenly).
		 */
		final float[] veins;
		final int veinSegments;
		/** Crack points, per crack: a run of (x, z, distance) from the player; {@code crackStart[i]} where crack i begins, its count in {@code crackLength}. */
		final float[] crackPoints;
		final int[] crackStart, crackLength;

		State(Level level, CinematicClient.Playing p) {
			ground = HeightField.of(level, p.at.x, p.at.y, p.at.z, 32); // read once, about 60 thousand blocks: a few milliseconds
			float s = CinematicCamera.heightScale(p);
			// The blocks torn loose: 3 to 10 blocks out, the real top block there (never moved: these are copies).
			for (int i = 0; i < 44; i++) {
				double a = GreyBox.h(p.seed, i, 31) * Math.PI * 2, r = 3 + GreyBox.h(p.seed, i, 32) * 7;
				double x = p.at.x + Math.cos(a) * r, z = p.at.z + Math.sin(a) * r;
				BlockPos top = BlockPos.containing(x, ground.at(x, z) - 0.5, z);
				BlockState state = level.getBlockState(top);
				if (state.isAir() || state.getRenderShape() != RenderShape.MODEL || !state.getFluidState().isEmpty()) continue;
				debris.add(new Piece(state, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, i, a));
			}
			// The veins: 18 from the root at uneven angles, each a wandering line of segments, branching, each with its own pace and delay.
			List<float[]> segs = new ArrayList<>();
			int roots = 18;
			for (int i = 0; i < roots; i++) {
				double a = (i + (GreyBox.h(p.seed, i, 40) - 0.5) * 0.8) * Math.PI * 2 / roots;
				int from = segs.size();
				// From a little way out from the strike (not all from one point, where 18 roots would pile into a white star).
				float root = (4 + GreyBox.h(p.seed, i, 45) * 3) * s;
				vein(segs, p.seed, i * 977, root * (float) Math.cos(a), root * (float) Math.sin(a), a, 0, 0, 22 + (int) (GreyBox.h(p.seed, i, 41) * 14), s);
				float pace = 0.45f + GreyBox.h(p.seed, i, 42) * 0.7f, delay = GreyBox.h(p.seed, i, 43) * 14;
				for (int k = from; k < segs.size(); k++) {
					segs.get(k)[8] = pace * (0.85f + 0.3f * GreyBox.h(p.seed, i * 31 + k, 44)); // a branch a little faster or slower than its vein
					segs.get(k)[9] = delay;
				}
			}
			veinSegments = segs.size();
			veins = new float[veinSegments * VEIN];
			for (int i = 0; i < veinSegments; i++) System.arraycopy(segs.get(i), 0, veins, i * VEIN, VEIN);
			// The cracks: 14 jagged lines out from about a block round the player, some forked.
			List<float[]> pts = new ArrayList<>();
			int cracks = 14;
			crackStart = new int[cracks * 2];
			crackLength = new int[cracks * 2];
			for (int c = 0; c < cracks; c++) {
				double a = c * Math.PI * 2 / cracks + (GreyBox.h(p.seed, c, 50) - 0.5) * 0.4;
				double x = Math.cos(a) * 1.1, z = Math.sin(a) * 1.1, d = 0, heading = a;
				int steps = 7 + (int) (GreyBox.h(p.seed, c, 51) * 7);
				crackStart[c * 2] = pts.size();
				pts.add(new float[] {(float) x, (float) z, 0});
				double forkX = 0, forkZ = 0, forkD = 0, forkHeading = 0;
				int forkAt = 2 + (int) (GreyBox.h(p.seed, c, 52) * 4);
				for (int k = 0; k < steps; k++) {
					heading += (GreyBox.h(p.seed, c * 31 + k, 53) - 0.5) * 0.9;
					heading = a + Mth.clamp(heading - a, -0.5, 0.5); // wander, but keep going outward
					double len = 0.45 + GreyBox.h(p.seed, c * 31 + k, 54) * 0.5;
					x += Math.cos(heading) * len;
					z += Math.sin(heading) * len;
					d += len;
					pts.add(new float[] {(float) x, (float) z, (float) d});
					if (k == forkAt) {
						forkX = x;
						forkZ = z;
						forkD = d;
						forkHeading = heading + (GreyBox.h(p.seed, c, 55) < 0.5 ? -1 : 1) * (0.5 + GreyBox.h(p.seed, c, 56) * 0.4);
					}
				}
				crackLength[c * 2] = pts.size() - crackStart[c * 2];
				// Its fork, from partway along it.
				crackStart[c * 2 + 1] = pts.size();
				pts.add(new float[] {(float) forkX, (float) forkZ, (float) forkD});
				for (int k = 0; k < 4; k++) {
					forkHeading += (GreyBox.h(p.seed, c * 37 + k, 57) - 0.5) * 0.8;
					double len = 0.4 + GreyBox.h(p.seed, c * 37 + k, 58) * 0.4;
					forkX += Math.cos(forkHeading) * len;
					forkZ += Math.sin(forkHeading) * len;
					forkD += len;
					pts.add(new float[] {(float) forkX, (float) forkZ, (float) forkD});
				}
				crackLength[c * 2 + 1] = pts.size() - crackStart[c * 2 + 1];
			}
			crackPoints = new float[pts.size() * 3];
			for (int i = 0; i < pts.size(); i++) System.arraycopy(pts.get(i), 0, crackPoints, i * 3, 3);
		}

		/** One vein from (x, z) heading {@code a}, {@code steps} segments long, its root {@code d} along; branching while {@code depth} allows. */
		private static void vein(List<float[]> out, long seed, int id, float x, float z, double a, float d, int depth, int steps, float s) {
			double heading = a;
			for (int k = 0; k < steps; k++) {
				// A jagged, lightning-like line that keeps running outward: each step turns at random, but never far off its course.
				heading += (GreyBox.h(seed, id + k, 60) - 0.5) * 0.7;
				heading = a + Mth.clamp(heading - a, -0.32, 0.32);
				float len = (1.5f + GreyBox.h(seed, id + k, 61) * 1.3f) * s * (depth == 0 ? 1 : 0.75f);
				float nx = x + (float) Math.cos(heading) * len, nz = z + (float) Math.sin(heading) * len;
				float width = (depth == 0 ? 0.8f : depth == 1 ? 0.42f : 0.24f) * s * (1 - 0.55f * k / steps);
				out.add(new float[] {x, z, nx, nz, d, d + len, width, GreyBox.h(seed, id, 62) * 100, 1, 0});
				x = nx;
				z = nz;
				d += len;
				if (depth < 2 && k > 1 && GreyBox.h(seed, id + k, 63) < (depth == 0 ? 0.22f : 0.12f)) { // a few bold branches, not a fuzz
					double side = GreyBox.h(seed, id + k, 64) < 0.5 ? -1 : 1;
					vein(out, seed, id * 7 + k * 131 + 17, x, z, heading + side * (0.45 + GreyBox.h(seed, id + k, 65) * 0.5), d, depth + 1,
							Math.max(3, (steps - k) / 2), s);
				}
			}
		}
	}

	/** How many numbers each vein segment takes (State#veins). */
	private static final int VEIN = 10;

	/** A block torn loose: its state, its place, the seed's index, the direction it lies from the player. */
	record Piece(BlockState state, double x, double y, double z, int index, double angle) {}

	private static final Map<CinematicClient.Playing, State> STATES = new WeakHashMap<>();

	static State state(CinematicClient.Playing p) {
		State s = STATES.get(p);
		if (s == null) {
			Minecraft mc = Minecraft.getInstance();
			s = new State(mc.level, p);
			STATES.put(p, s);
		}
		return s;
	}

	/** How many of a swarm to draw, by the player's Particles setting. */
	private static int count(int full) {
		ParticleStatus status = Minecraft.getInstance().options.particles().get();
		return status == ParticleStatus.ALL ? full : status == ParticleStatus.DECREASED ? full / 2 : full / 4;
	}

	// --- The layer (FxLayer's painter) ---

	/** Whether a cinematic's storm deck is over this world's sky now (vanilla's white clouds are then left out: CinematicCloudsMixin). */
	public static boolean coversTheSky() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		for (CinematicClient.Playing p : CinematicClient.playing()) {
			if (!p.world.equals(mc.level.dimension()) || !p.probe.openSky()) continue;
			float t = p.elapsed(0);
			for (Sequence.Element e : p.seq.elements) if (e.kind().equals("storm_deck") && t >= e.from() && t < e.to()) return true;
		}
		return false;
	}

	/** Whether any cinematic is playing in this world (the layer is drawn then). */
	public static boolean wants() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		for (CinematicClient.Playing p : CinematicClient.playing()) if (p.world.equals(mc.level.dimension())) return true;
		return false;
	}

	/** Into the effects' layer: every cinematic's built elements, smoke and cloud first, then light; then the grey box's; then the grade. */
	public static void paint(WorldRenderContext context) {
		CinematicClient.safely(() -> {
			Minecraft mc = Minecraft.getInstance();
			float partial = context.tickDelta();
			for (CinematicClient.Playing p : CinematicClient.playing()) {
				if (!p.world.equals(mc.level.dimension())) continue;
				State st = state(p);
				float t = p.elapsed(partial);
				for (int pass = 0; pass < 2; pass++) {
					for (Sequence.Element e : p.seq.elements) {
						if (t < e.from() || t >= e.to() || !KINDS.contains(e.kind())) continue;
						element(context, p, st, e, t, pass == 0);
					}
				}
				if (Fx.devThrowNow("painter")) {
					// The failure-safety test (development only): a body drawn into the layer let go part-way, the shared builder left building.
					Fx.end();
					com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder().begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
							com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);
					throw new IllegalStateException("[DTH-FX] dev: a painter throws, the shared builder left building (the failure-safety test)");
				}
			}
			Fx.end();
			GreyBox.render(context);
			CinematicClient.Playing own = CinematicClient.own();
			if (own != null) look(context, own, own.elapsed(partial));
		});
	}

	/** The world pass (AFTER_ENTITIES): the torn-up blocks, lit as the world is. */
	public static void renderWorld(WorldRenderContext context) {
		CinematicClient.safely(() -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) return;
			for (CinematicClient.Playing p : CinematicClient.playing()) {
				if (!p.world.equals(mc.level.dimension())) continue;
				float t = p.elapsed(context.tickDelta());
				for (Sequence.Element e : p.seq.elements) {
					if (e.kind().equals("debris") && t >= e.from() && t < e.to()) debris(context, p, state(p), e, t);
				}
			}
		});
	}

	private static void element(WorldRenderContext ctx, CinematicClient.Playing p, State st, Sequence.Element e, float t, boolean cover) {
		float lt = t - e.from(), life = e.to() - e.from();
		switch (e.kind()) {
			// The cloud base's glow goes with the deck, before everything that hangs under it (the mushroom's billows cover it).
			case "storm_deck" -> {
				if (cover) {
					stormDeck(p, e, t, lt, life);
					deckLight(p, t);
				}
			}
			case "dust_ring" -> dustRing(p, st, lt, life, cover);
			case "mushroom" -> mushroom(p, e, lt, life, cover);
			case "pillar" -> { if (!cover) pillar(p, st, e, t, lt, life); else pillarSpecks(p, lt, life); }
			default -> {
				if (cover) return;
				switch (e.kind()) {
					case "wisps" -> wisps(p, st, lt, life);
					case "array" -> array(p, st, e, lt, life);
					case "ring_race" -> ringRace(p, st, e, lt, life);
					case "ground_glow" -> groundGlow(p, st, e, t, lt, life);
					case "threads" -> threads(p, e, lt, life);
					case "ribbons" -> fan(p, lt, life);
					case "cracks" -> cracks(p, st, lt, life);
					case "debris" -> debrisGlow(p, st, e, t);
					case "streaks" -> absorption(p, lt, life);
					case "slashes" -> slashes(p, e, lt);
					case "soul_glow" -> soulGlow(ctx, p, lt);
					case "pillar_rays" -> pillarRays(p, st, lt, life);
					case "veins" -> veins(p, st, lt, life);
					default -> { }
				}
			}
		}
	}

	/** Eased in over {@code in} ticks and out over {@code out}, nothing popping. */
	private static float fade(float lt, float life, float in, float out) {
		return Mth.clamp(Math.min(in <= 0 ? 1 : lt / in, out <= 0 ? 1 : (life - lt) / out), 0, 1);
	}

	private static float smooth(float e0, float e1, float x) {
		float k = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
		return k * k * (3 - 2 * k);
	}

	/** The pillar's foot and top. */
	private static double groundY(CinematicClient.Playing p) {
		return p.probe.groundY();
	}

	private static double topY(CinematicClient.Playing p) {
		return p.probe.topY();
	}

	// --- Act 1 ---

	private static void stormDeck(CinematicClient.Playing p, Sequence.Element e, float t, float lt, float life) {
		if (!p.probe.openSky()) return; // underground, the rock is the sky
		float alpha = fade(lt, life, 4, 25); // closing over the sky in the first four ticks, as the grade darkens the world
		// Wide enough that its edge sits low on the horizon from anywhere near (about 17 degrees up under clouds 128 blocks high), but
		// never past what the camera draws (four times the render distance): at a short render distance it shrinks to fit.
		double up = topY(p) - groundY(p);
		double far = Minecraft.getInstance().gameRenderer.getDepthFar() * 0.95;
		float radius = (float) Math.min(Math.max(420, up * 3.3), Math.sqrt(Math.max(far * far - up * up, 64 * 64)));
		Fx.begin(FxTextures.CLOUD, Fx.Mode.COVER, 1);
		deckDisc(p.at.x, topY(p) - 0.5, p.at.z, radius, 1 / 115f, t * 0.0004f, STORM, alpha);
		deckDisc(p.at.x, topY(p) - 4.0, p.at.z, radius * 0.9f, 1 / 60f, -t * 0.0006f + 0.37f, STORM_LOW, 0.45f * alpha);
	}

	/** A cloud layer at height y: a disc of rings, its texture laid flat across it (repeating every 1/scale blocks), its edge fading. */
	private static void deckDisc(double x, double y, double z, float radius, float scale, float drift, float[] c, float alpha) {
		int rings = 8, sides = 48;
		for (int k = 0; k < rings; k++) {
			float r0 = radius * k / rings, r1 = radius * (k + 1) / rings;
			float a0 = alpha * (1 - smooth(radius * 0.72f, radius, r0)), a1 = alpha * (1 - smooth(radius * 0.72f, radius, r1));
			for (int i = 0; i < sides; i++) {
				double t0 = i * Math.PI * 2 / sides, t1 = (i + 1) * Math.PI * 2 / sides;
				deckVertex(x, y, z, Math.cos(t0) * r0, Math.sin(t0) * r0, scale, drift, c, a0);
				deckVertex(x, y, z, Math.cos(t0) * r1, Math.sin(t0) * r1, scale, drift, c, a1);
				deckVertex(x, y, z, Math.cos(t1) * r1, Math.sin(t1) * r1, scale, drift, c, a1);
				deckVertex(x, y, z, Math.cos(t1) * r0, Math.sin(t1) * r0, scale, drift, c, a0);
			}
		}
	}

	private static void deckVertex(double x, double y, double z, double dx, double dz, float scale, float drift, float[] c, float a) {
		Fx.v(x + dx, y, z + dz, (float) (dx * scale) + drift, (float) (dz * scale) + drift * 0.6f, c[0], c[1], c[2], a);
	}

	/**
	 * The cloud base lit orange round the pillar, from the moment it strikes (the pillar element's start), flickering: the deck's own
	 * billows lit from below (its cloud texture laid the same way, as light, so the pale billows catch the most), spreading out as the fire
	 * spreads, and a broad soft glow over the strike.
	 */
	private static void deckLight(CinematicClient.Playing p, float t) {
		Sequence.Element pillar = find(p, "pillar");
		if (pillar == null || t < pillar.from() + 3) return;
		float lt = t - pillar.from() - 3;
		float k = CinematicCamera.heightScale(p);
		float on = Mth.clamp(lt / 10f, 0, 1) * (1 - smooth(pillar.to() - pillar.from() - 20, pillar.to() - pillar.from(), lt + 3));
		float flicker = 0.88f + 0.12f * Mth.sin(t * 0.7f) * Mth.sin(t * 0.31f + 1.3f);
		if (p.probe.openSky()) {
			float reach = 140 * k * (0.55f + 0.45f * Ease.CUBIC_OUT.between(lt, 0, 45));
			Fx.begin(FxTextures.CLOUD, Fx.Mode.LIGHT, 1.7f);
			litDisc(p.at.x, topY(p) - 4.3, p.at.z, reach, 1 / 115f, t * 0.0004f, ORANGE, on * flicker);
		}
		double y = topY(p) - (p.probe.openSky() ? 4.4 : 0.08);
		Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
		Fx.flat(p.at.x, y, p.at.z, 60 * k, 0, ORANGE[0], ORANGE[1], ORANGE[2], 0.5f * on * flicker);
		Fx.flat(p.at.x, y - 0.1, p.at.z, 26 * k, 0, GOLD[0], GOLD[1], GOLD[2], 0.4f * on * flicker);
	}

	/** Light laid on a cloud layer from below: a disc of rings, its texture as the deck's (the same scale and drift), brightest at the middle. */
	private static void litDisc(double x, double y, double z, float radius, float scale, float drift, float[] c, float alpha) {
		int rings = 10, sides = 40;
		for (int k = 0; k < rings; k++) {
			float r0 = radius * k / rings, r1 = radius * (k + 1) / rings;
			float a0 = alpha * lit(r0 / radius), a1 = alpha * lit(r1 / radius);
			for (int i = 0; i < sides; i++) {
				double t0 = i * Math.PI * 2 / sides, t1 = (i + 1) * Math.PI * 2 / sides;
				deckVertex(x, y, z, Math.cos(t0) * r0, Math.sin(t0) * r0, scale, drift, c, a0);
				deckVertex(x, y, z, Math.cos(t0) * r1, Math.sin(t0) * r1, scale, drift, c, a1);
				deckVertex(x, y, z, Math.cos(t1) * r1, Math.sin(t1) * r1, scale, drift, c, a1);
				deckVertex(x, y, z, Math.cos(t1) * r0, Math.sin(t1) * r0, scale, drift, c, a0);
			}
		}
	}

	/** How strongly the cloud is lit at {@code f} of the light's reach: full over the strike, falling away softly to nothing. */
	private static float lit(float f) {
		float k = 1 - smooth(0.05f, 1, f);
		return k * k;
	}

	private static Sequence.Element find(CinematicClient.Playing p, String kind) {
		for (Sequence.Element e : p.seq.elements) if (e.kind().equals(kind)) return e;
		return null;
	}

	private static void wisps(CinematicClient.Playing p, State st, float lt, float life) {
		float f = fade(lt, life, 6, 14);
		// Read from where anyone stands: as built up close, wider to a watcher further off (24 blocks: two and a half times).
		float far = (float) Math.max(1, Fx.distanceTo(p.at.x, p.at.y + 1, p.at.z) / 10);
		// The embers kept under white (their hottest stays ember-red, never spilling toward a pale salmon), the wisps' threads too.
		Fx.begin(FxTextures.GLOW, Fx.Mode.LIGHT, 1.15f);
		int n = count(56);
		for (int i = 0; i < n; i++) {
			double a = GreyBox.h(p.seed, i, 1) * Math.PI * 2, r = 0.7 + GreyBox.h(p.seed, i, 2) * 3.4;
			float period = 36 + GreyBox.h(p.seed, i, 3) * 34, ph = (lt / period + GreyBox.h(p.seed, i, 4)) % 1;
			double x = p.at.x + Math.cos(a) * r + Mth.sin(lt * 0.06f + i) * 0.18, z = p.at.z + Math.sin(a) * r + Mth.cos(lt * 0.05f + i * 1.7f) * 0.18;
			double y = st.ground.at(x, z) + 0.08 + ph * (1.4 + GreyBox.h(p.seed, i, 5) * 1.6);
			float flick = 0.7f + 0.3f * Mth.sin(lt * 0.9f + i * 2.1f);
			Fx.sprite(x, y, z, (0.05f + 0.06f * GreyBox.h(p.seed, i, 6)) * far, 0, EMBER[0], EMBER[1], EMBER[2], Mth.sin(Mth.PI * ph) * flick * f);
		}
		// Red wisps standing up off the ground (the reference's cave: "red wisps on both sides"), drifting upward, a soft glow and a
		// brighter thread in each.
		for (int pass = 0; pass < 2; pass++) {
			Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, pass == 0 ? 1.0f : 1.3f);
			for (int i = 0; i < 14; i++) {
				double a = GreyBox.h(p.seed, i, 7) * Math.PI * 2, r = 1.4 + GreyBox.h(p.seed, i, 8) * 3.0;
				double bx = p.at.x + Math.cos(a) * r, bz = p.at.z + Math.sin(a) * r, by = st.ground.at(bx, bz);
				float tall = 1.6f + GreyBox.h(p.seed, i, 9) * 2.2f, scroll = lt * 0.03f + i;
				Fx.path();
				for (int k = 0; k <= 12; k++) {
					float u = k / 12f;
					double sway = Mth.sin(u * 4 + lt * 0.08f + i) * 0.16 * u;
					float a2 = Mth.sin(Mth.PI * u) * (pass == 0 ? 0.35f : 0.6f) * f * (0.7f + 0.3f * Mth.sin(lt * 0.2f + i));
					Fx.point(bx + sway, by + u * tall, bz + sway * 0.6, (pass == 0 ? 0.7f : 0.18f) * far, u * 0.6f - scroll, WISP[0], WISP[1], WISP[2], a2);
				}
				Fx.ribbon();
			}
		}
	}

	private static void array(CinematicClient.Playing p, State st, Sequence.Element e, float lt, float life) {
		float size = e.params().length > 0 ? e.params()[0] : 3;
		float grow = Ease.CUBIC_OUT.between(lt, 0, 20);
		float out = 1 - smooth(life - 30, life, lt);
		float r = size * grow;
		float breathe = 0.9f + 0.1f * Mth.sin(lt * 0.25f);
		double x = p.at.x, z = p.at.z;
		HeightField g = st.ground;
		// Its birth: gold light welling up under the player before the first ring is drawn, fading as the array grows out of it.
		float birth = 1 - smooth(4, 16, lt);
		if (birth > 0) {
			Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
			Fx.groundSquare(g, x, z, size * 0.9f, 6, 0, 0.04f, GOLD[0], GOLD[1], GOLD[2], 0.6f * birth * out);
		}
		if (r <= 0.01f) return;
		// Its light on the ground about it: a broad warm pool under it and round it, out past its rim (the grass lit, not left dark round it;
		// kept low at its middle, where the array's own light already stacks).
		Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
		Fx.groundSquare(g, x, z, r * 2.6f, 12, 0, 0.03f, ORANGE[0], ORANGE[1], ORANGE[2], 0.14f * out * grow);
		// Flame swirling between the rings, drawn as orange light in its own shapes (not through the heat ramp, which ran its thick parts to
		// a smooth cream fill), on a dark ground: the swirls stay visible, a fainter deep layer turning the other way under them.
		Fx.begin(FxTextures.ARRAY_FLAME, Fx.Mode.LIGHT, 1.8f);
		Fx.groundSquare(g, x, z, r, 8, lt * 0.03f, 0.05f, ORANGE[0], ORANGE[1], ORANGE[2], 0.85f * out * breathe);
		Fx.groundSquare(g, x, z, r * 0.97f, 8, -lt * 0.018f + 1.1f, 0.055f, DEEP[0] * 1.4f, DEEP[1] * 1.4f, DEEP[2] * 1.4f, 0.4f * out);
		// The rings and runes one way, the small circles the other (they come a few ticks after), the star slowest.
		float circles = Ease.CUBIC_OUT.between(lt, 4, 16), star = Ease.CUBIC_OUT.between(lt, 8, 22);
		Fx.begin(FxTextures.ARRAY_RINGS, Fx.Mode.HEAT, 1.75f);
		Fx.groundSquare(g, x, z, r, 8, lt * 0.012f, 0.06f, GOLD[0], GOLD[1], GOLD[2], out * breathe);
		Fx.begin(FxTextures.ARRAY_CIRCLES, Fx.Mode.HEAT, 1.6f);
		Fx.groundSquare(g, x, z, r, 8, -lt * 0.022f, 0.065f, GOLD[0], GOLD[1], GOLD[2], out * circles);
		Fx.begin(FxTextures.ARRAY_STAR, Fx.Mode.HEAT, 1.3f);
		Fx.groundSquare(g, x, z, r, 8, lt * 0.006f + 0.39f, 0.07f, GOLD[0], GOLD[1], GOLD[2], 0.42f * out * star);
		// The red heart, under the player and wider than them (seen round the seated body from above), a red glow over it.
		Fx.begin(FxTextures.ARRAY_CORE, Fx.Mode.LIGHT, 1.4f);
		Fx.groundSquare(g, x, z, r * 0.45f, 4, -lt * 0.01f, 0.075f, 1, 1, 1, out * grow);
		Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
		Fx.groundSquare(g, x, z, r * 0.55f, 4, 0, 0.08f, 0.8f, 0.12f, 0.05f, 0.5f * out * grow * breathe);
		// Sparks rising off its rings.
		Fx.begin(FxTextures.GLOW, Fx.Mode.LIGHT, 1.8f);
		int n = count(40);
		for (int i = 0; i < n; i++) {
			double a = GreyBox.h(p.seed, i, 70) * Math.PI * 2 + lt * 0.012;
			double rr = r * (GreyBox.h(p.seed, i, 71) < 0.5 ? 0.97 : 0.52);
			float period = 22 + GreyBox.h(p.seed, i, 72) * 20, ph = (lt / period + GreyBox.h(p.seed, i, 73)) % 1;
			double sx = x + Math.cos(a) * rr, sz = z + Math.sin(a) * rr;
			Fx.sprite(sx, g.at(sx, sz) + 0.1 + ph * 1.3, sz, 0.035f, 0, GOLD_LIGHT[0], GOLD_LIGHT[1], GOLD_LIGHT[2], Mth.sin(Mth.PI * ph) * out * grow);
		}
	}

	private static void ringRace(CinematicClient.Playing p, State st, Sequence.Element e, float lt, float life) {
		float u = lt / life;
		float from = e.params().length > 0 ? e.params()[0] : 3, to = e.params().length > 1 ? e.params()[1] : 22;
		float r = Mth.lerp(Ease.CUBIC_OUT.apply(u), from, to);
		float a = (1 - u * u) * fade(lt, life, 2, 6);
		float band = 1.6f + 2.4f * u;
		int sides = 128;
		float around = (float) (r * Math.PI * 2 / 3.2);
		double x = p.at.x, z = p.at.z;
		// The ground it has passed over still warm, from the array out to the band (faint: by day, orange light on green grass turns lemon).
		Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
		Fx.groundSquare(st.ground, x, z, r * 1.08f, 14, 0, 0.028f, ORANGE[0], ORANGE[1], ORANGE[2], 0.15f * a);
		// The band's body of flame, deep orange, covering the grass under it (so it reads orange even on noon grass, never lemon on green);
		// over it the flame itself, orange, kept under the heat ramp's white but along its leading line (v 0, at the outer radius).
		Fx.begin(FxTextures.FLAME_BAND, Fx.Mode.COVER, 1);
		Fx.groundRing(st.ground, x, z, r, Math.max(0.05f, r - band), sides, around, lt * 0.01f, 0.05f, FIRE[0], FIRE[1], FIRE[2], a * 0.75f, a * 0.55f);
		Fx.begin(FxTextures.FLAME_BAND, Fx.Mode.HEAT, 1.6f);
		Fx.groundRing(st.ground, x, z, r, Math.max(0.05f, r - band), sides, around, lt * 0.01f, 0.06f, ORANGE[0], ORANGE[1], ORANGE[2], a * 0.6f, a * 0.5f);
		// A warm light under the band, filling it between its tongues (the same band, stretched round the ring a few times only).
		Fx.begin(FxTextures.FLAME_BAND, Fx.Mode.LIGHT, 1.0f);
		Fx.groundRing(st.ground, x, z, r, Math.max(0.05f, r - band * 1.2f), sides, 6, -lt * 0.004f, 0.055f, ORANGE[0], ORANGE[1], ORANGE[2],
				a * 0.55f, a * 0.4f);
		// Flames standing up out of it: a wall of flame at its leading edge about a block and a half high (lower as it spreads thin), and a
		// lower one inside it turning the other way, so the flame swirls, from eye level and from far off. Each a covering body of fire with
		// its light over it, the light kept under white where the near and far sides of the ring are seen one over the other.
		float tall = 1.6f * (1 - 0.4f * u);
		Fx.begin(FxTextures.FLAME_BAND, Fx.Mode.COVER, 1);
		Fx.groundWall(st.ground, x, z, r - 0.1f, tall, sides, around * 1.3f, -lt * 0.025f, FIRE[0], FIRE[1], FIRE[2], a * 0.7f, 0);
		Fx.begin(FxTextures.FLAME_BAND, Fx.Mode.LIGHT, 1.0f);
		Fx.groundWall(st.ground, x, z, r - 0.1f, tall, sides, around * 1.3f, -lt * 0.025f, FLAME[0], FLAME[1], FLAME[2], a * 0.65f, 0);
		Fx.groundWall(st.ground, x, z, Math.max(0.3f, r - band * 0.45f), tall * 0.7f, sides, around * 1.05f, lt * 0.03f + 0.5f,
				ORANGE[0], ORANGE[1], ORANGE[2], a * 0.5f, 0);
	}

	private static void groundGlow(CinematicClient.Playing p, State st, Sequence.Element e, float t, float lt, float life) {
		float radius = e.params().length > 0 ? e.params()[0] : 6;
		float level = e.params().length > 1 ? e.params()[1] : 1;
		// Swelling with the gathering (the fan, the absorption), flaring as the pillar strikes, then held: warm light on the ground, never
		// white (only the flare at the strike runs hot).
		// The golden fan's light spills gold over the grass round the player (the shot is low: the pool is seen flat, so it is strong).
		float fanSpill = smooth(108, 128, t) * (1 - smooth(148, 166, t));
		float k = 0.25f + 0.45f * fanSpill + 0.2f * smooth(150, 200, t);
		k += (CinematicSettings.reducedFlashes ? 0.12f : 0.2f) * Math.max(0, 1 - Math.abs(t - 246) / 10f);
		k *= 0.9f + 0.1f * Mth.sin(t * 0.6f);
		float a = level * k * fade(lt, life, 10, 20);
		float r = radius * (1 + 0.5f * fanSpill + 0.6f * smooth(240, 252, t));
		Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
		Fx.groundSquare(st.ground, p.at.x, p.at.z, r, 14, 0, 0.035f, ORANGE[0], ORANGE[1], ORANGE[2], a);
		Fx.groundSquare(st.ground, p.at.x, p.at.z, r * 0.5f, 8, 0, 0.04f, GOLD[0], GOLD[1], GOLD[2], a * 0.6f);
	}

	// --- Act 2 ---

	private static void threads(CinematicClient.Playing p, Sequence.Element e, float lt, float life) {
		int n = (int) (e.params().length > 0 ? e.params()[0] : 32);
		double height = Math.max(40, topY(p) - groundY(p));
		double cx = p.at.x, cy = p.at.y + 0.7, cz = p.at.z;
		float f = fade(lt, life, 3, 12);
		for (int pass = 0; pass < 2; pass++) {
			// First a wide faint glow about each, then the thread itself: a white-hot core with orange edges.
			if (pass == 0) Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, 1.0f);
			else Fx.begin(FxTextures.RIBBON, Fx.Mode.HEAT, 2.2f);
			for (int i = 0; i < n; i++) {
				// They rain from just under the storm, some near the player and some far out, falling nearly straight down at first and
				// curving into the chest only in the last stretch.
				double a = GreyBox.h(p.seed, i, 4) * Math.PI * 2, far = 4 + GreyBox.h(p.seed, i, 5) * 28;
				double high = height * (0.7 + GreyBox.h(p.seed, i, 6) * 0.25);
				double sx = cx + Math.cos(a) * far, sy = cy + high, sz = cz + Math.sin(a) * far;
				double bx = cx + Math.cos(a + 0.25) * far * 0.9, by = cy + high * 0.12, bz = cz + Math.sin(a + 0.25) * far * 0.9;
				// Each starts falling at its own moment (some already on their way as the shot opens), its head racing down the curve,
				// its tail behind it.
				float start = (GreyBox.h(p.seed, i, 7) * 0.55f - 0.15f) * life, dur = life * (0.3f + GreyBox.h(p.seed, i, 81) * 0.2f);
				float head = Mth.clamp((lt - start) / dur, 0, 1.6f), tail = head - 0.6f;
				if (head <= 0 || tail >= 1) continue;
				float u0 = Math.max(0, tail), u1 = Math.min(1, head);
				// Gently wavy (a few long swells along its length), not a jagged bolt.
				double amp = 0.35 + GreyBox.h(p.seed, i, 82) * 0.5, wave = 22 + GreyBox.h(p.seed, i, 83) * 16, ph = GreyBox.h(p.seed, i, 84) * 6;
				double len = far + high;
				// The wave's side: level and across the fall.
				double ax = -Math.sin(a), az = Math.cos(a);
				// Wide enough to read from a hundred blocks below (a few pixels of white-hot core, a broad orange glow).
				float width = pass == 0 ? 6f : 1.6f + 0.6f * GreyBox.h(p.seed, i, 85);
				float alphaBase = pass == 0 ? 0.2f : 0.95f;
				float[] col = i % 4 == 0 ? GOLD : ORANGE;
				Fx.path();
				int steps = 40;
				for (int k = 0; k <= steps; k++) {
					float u = u0 + (u1 - u0) * k / steps;
					double kk = 1 - u;
					double x = sx * kk * kk + bx * 2 * kk * u + cx * u * u, y = sy * kk * kk + by * 2 * kk * u + cy * u * u, z = sz * kk * kk + bz * 2 * kk * u + cz * u * u;
					double sway = Math.sin(u * len / wave * Math.PI * 2 - lt * 0.35 + ph) * amp * (1 - u);
					float along = (u - u0) / Math.max(1.0E-3f, u1 - u0); // 0 at the tail, 1 at the head
					float alpha = alphaBase * f * along * (along > 0.92f ? (1 - along) / 0.08f * 0.6f + 0.4f : 1);
					// Thinner as it nears the player (it arrives a thread, not a beam).
					float w = width * (0.55f + 0.45f * along) * (0.3f + 0.7f * (1 - u));
					Fx.point(x + ax * sway, y, z + az * sway, w, (float) (u * len / 6) - lt * 0.12f, col[0], col[1], col[2], alpha);
				}
				Fx.ribbon();
			}
		}
		// Hair-thin white streaks falling with them, many.
		Fx.begin(FxTextures.STREAK, Fx.Mode.LIGHT, 1.6f);
		int hair = count(80);
		for (int i = 0; i < hair; i++) {
			double a = GreyBox.h(p.seed, i, 86) * Math.PI * 2, r = 3 + GreyBox.h(p.seed, i, 87) * 18;
			float period = 14 + GreyBox.h(p.seed, i, 88) * 10, ph = (lt / period + GreyBox.h(p.seed, i, 89)) % 1;
			double x = cx + Math.cos(a) * r, z = cz + Math.sin(a) * r;
			double y = cy + (1 - ph) * height * 0.6;
			Fx.streak(x, y + 6, z, x, y, z, 0.07f, 1, 0.95f, 0.85f, 0.5f * f * Mth.sin(Mth.PI * ph));
		}
	}

	/**
	 * The golden fan: eleven ribbons a side arcing in to the chest, each rising off the ground out to the side (some behind the player,
	 * some toward the camera), bowing up three to five blocks and sweeping down into it; a white-hot point there as they arrive.
	 */
	private static void fan(CinematicClient.Playing p, float lt, float life) {
		double yaw = Math.toRadians(p.facing);
		double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		double cx = p.at.x, cy = p.at.y + 0.65, cz = p.at.z;
		float f = fade(lt, life, 2, 8);
		float far = (float) Math.max(1, Fx.distanceTo(cx, cy, cz) / 12); // thicker to someone far off
		int perSide = 11;
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 0) Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, 1.0f);
			else Fx.begin(FxTextures.RIBBON, Fx.Mode.HEAT, 2.2f);
			for (int side = -1; side <= 1; side += 2) {
				for (int i = 0; i < perSide; i++) {
					int id = (side + 1) * perSide + i;
					double out = 6 + GreyBox.h(p.seed, id, 90) * 10, fw = -6 + GreyBox.h(p.seed, id, 91) * 12;
					double rise = 3 + GreyBox.h(p.seed, id, 93) * 2.5;
					double ax = cx + rx * side * out + fx * fw, ay = p.at.y + 0.2 + GreyBox.h(p.seed, id, 94) * 0.8, az = cz + rz * side * out + fz * fw;
					// The middle point pulled up so the curve peaks about {@code rise} over the ground halfway in.
					double bx = cx + rx * side * out * 0.6 + fx * fw * 0.5, by = p.at.y + 2 * rise - 0.6, bz = cz + rz * side * out * 0.6 + fz * fw * 0.5;
					float start = i * 1.4f + GreyBox.h(p.seed, id, 92) * 4;
					float head = Mth.clamp((lt - start) / 20f, 0, 1.7f), tail = head - 0.7f;
					if (head <= 0 || tail >= 1) continue;
					float u0 = Math.max(0, tail), u1 = Math.min(1, head);
					float width = (pass == 0 ? 1.3f : 0.38f + 0.08f * (i % 3)) * far;
					float base = pass == 0 ? 0.25f : 1.0f;
					float[] col = i % 2 == 0 ? GOLD : ORANGE;
					Fx.path();
					for (int k = 0; k <= 32; k++) {
						float u = u0 + (u1 - u0) * k / 32;
						double kk = 1 - u;
						double x = ax * kk * kk + bx * 2 * kk * u + cx * u * u, y = ay * kk * kk + by * 2 * kk * u + cy * u * u, z = az * kk * kk + bz * 2 * kk * u + cz * u * u;
						float along = (u - u0) / Math.max(1.0E-3f, u1 - u0);
						Fx.point(x, y + Mth.sin(u * 9 + lt * 0.4f + id) * 0.1 * (1 - u), z, width * (0.4f + 0.6f * along) * (1 - 0.5f * u),
								u * 6 - lt * 0.2f, col[0], col[1], col[2], base * f * along);
					}
					Fx.ribbon();
				}
			}
		}
		// Where they meet: a white-hot point, growing as they arrive, rays about it.
		float meet = Ease.CUBIC_OUT.between(lt, 14, 30) * f;
		if (meet > 0) {
			float pulse = 0.85f + 0.15f * Mth.sin(lt * 1.3f);
			Fx.begin(FxTextures.GLOW, Fx.Mode.HEAT, 3.0f);
			Fx.sprite(cx + fx * 0.35, cy, cz + fz * 0.35, 1.3f * meet * pulse, 0, GOLD[0], GOLD[1], GOLD[2], meet);
			Fx.begin(FxTextures.RAYS, Fx.Mode.HEAT, 2.4f);
			Fx.sprite(cx + fx * 0.35, cy, cz + fz * 0.35, 2.4f * meet, lt * 0.02f, GOLD_LIGHT[0], GOLD_LIGHT[1], GOLD_LIGHT[2], meet * 0.9f);
		}
	}

	private static void cracks(CinematicClient.Playing p, State st, float lt, float life) {
		float cool = 1 - smooth(life - 30, life, lt);
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 0) Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, 1.0f);
			else Fx.begin(FxTextures.RIBBON, Fx.Mode.HEAT, 2.0f);
			for (int c = 0; c < st.crackStart.length; c++) {
				int crack = c / 2;
				float start = GreyBox.h(p.seed, crack, 59) * 50 + (c % 2) * 14;
				float front = Ease.CUBIC_OUT.between(lt, start, start + 45) * 9;
				if (front <= 0) continue;
				int s0 = st.crackStart[c], n = st.crackLength[c];
				Fx.path();
				for (int k = 0; k < n; k++) {
					int i = (s0 + k) * 3;
					float d = st.crackPoints[i + 2];
					if (d > front && k > 0) {
						// The last piece, cut where the crack has got to.
						int j = (s0 + k - 1) * 3;
						float pd = st.crackPoints[j + 2], f = (front - pd) / Math.max(1.0E-3f, d - pd);
						double x = p.at.x + Mth.lerp(f, st.crackPoints[j], st.crackPoints[i]), z = p.at.z + Mth.lerp(f, st.crackPoints[j + 1], st.crackPoints[i + 1]);
						crackPoint(st, x, z, front, front, pass, cool, lt, c);
						break;
					}
					double x = p.at.x + st.crackPoints[i], z = p.at.z + st.crackPoints[i + 1];
					crackPoint(st, x, z, d, front, pass, cool, lt, c);
				}
				Fx.ribbonOnPlane(0, 1, 0);
			}
		}
	}

	private static void crackPoint(State st, double x, double z, float d, float front, int pass, float cool, float lt, int c) {
		float fresh = 1 - Mth.clamp((front - d) / 3f, 0, 1); // white-hot just behind the front
		float flick = 0.8f + 0.2f * Mth.sin(lt * 0.8f + c * 1.9f + d);
		float width = pass == 0 ? 0.75f : (0.17f - 0.012f * d) * (1 + 0.4f * fresh);
		float alpha = pass == 0 ? 0.28f * cool : Mth.clamp(0.55f + 0.45f * fresh, 0, 1) * cool * flick;
		float[] col = cool < 0.6f ? DEEP : ORANGE;
		Fx.point(x, st.ground.at(x, z) + 0.035, z, Math.max(0.05f, width), d * 0.5f, col[0], col[1], col[2], alpha);
	}

	private static void debris(WorldRenderContext ctx, CinematicClient.Playing p, State st, Sequence.Element e, float t) {
		Minecraft mc = Minecraft.getInstance();
		MultiBufferSource buffers = ctx.consumers();
		if (buffers == null) return;
		PoseStack pose = ctx.matrixStack();
		Vec3 cam = ctx.camera().getPosition();
		// Lit by the fire while it burns (from the fan to the pillar's end): full block light, so they show warm against the gold, never black.
		float fireLit = smooth(100, 116, t) * (1 - smooth(440, 460, t));
		org.joml.Vector3f look = ctx.camera().getLookVector();
		boolean own = CinematicClient.own() == p;
		for (Piece piece : st.debris) {
			if (!debrisAt(p, piece, e, t, cam.x, cam.y, cam.z, look.x(), look.y(), look.z(), own)) continue;
			int i = piece.index();
			double x = DEBRIS_AT[0], y = DEBRIS_AT[1], z = DEBRIS_AT[2];
			float spin = (float) DEBRIS_AT[3], scale = (float) DEBRIS_AT[4];
			pose.pushPose();
			try { // the level's pose stack must come back level whatever happens (vanilla checks it after the entities)
				pose.translate(x - cam.x, y - cam.y, z - cam.z);
				SPIN.rotationY(Mth.DEG_TO_RAD * spin * (GreyBox.h(p.seed, i, 38) < 0.5 ? 1 : -1))
						.rotateX(Mth.DEG_TO_RAD * spin * 0.7f).rotateZ(Mth.DEG_TO_RAD * (spin * 0.4f + i * 17));
				pose.mulPose(SPIN);
				pose.scale(scale, scale, scale);
				pose.translate(-0.5, -0.5, -0.5);
				int light = LevelRenderer.getLightColor(mc.level, AT.set(x, y + 0.6, z));
				int block = Math.max(net.minecraft.client.renderer.LightTexture.block(light), Math.round(15 * fireLit));
				light = net.minecraft.client.renderer.LightTexture.pack(block, net.minecraft.client.renderer.LightTexture.sky(light));
				mc.getBlockRenderer().renderSingleBlock(piece.state(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
			} finally {
				pose.popPose();
			}
		}
	}

	/**
	 * Where torn-up block {@code piece} is at {@code t} seen from (camX, camY, camZ), looking along (lookX, lookY, lookZ), into
	 * {@link #DEBRIS_AT} (x, y, z, its spin, its scale); false if it hasn't torn loose yet or is shrunk away. The same for the block in the
	 * world and for its glow in the layer. {@code own}: the camera is the film's own (its shots keep the middle of the frame clear).
	 */
	private static boolean debrisAt(CinematicClient.Playing p, Piece piece, Sequence.Element e, float t, double camX, double camY, double camZ,
									float lookX, float lookY, float lookZ, boolean own) {
		int i = piece.index();
		float start = e.from() + GreyBox.h(p.seed, i, 33) * 60;
		if (t < start) return false;
		float lt = t - start;
		float rise = Ease.CUBIC_OUT.between(lt, 0, 60 + GreyBox.h(p.seed, i, 34) * 40);
		float lift = 2 + GreyBox.h(p.seed, i, 35) * 4;
		// Pulled in a little by the absorption, flung back as the pillar strikes, then hanging; thrown out by the blast (step B2's
		// second half keeps the rest).
		float pull = 0.15f * Ease.SMOOTH.between(t, 150, 200);
		float thrown = Ease.EXPO_OUT.between(t, 240, 262) * 5f + Ease.EXPO_OUT.between(t, 528, 545) * 2.5f;
		double r = Math.hypot(piece.x() - p.at.x, piece.z() - p.at.z) * (1 - pull) + thrown;
		double x = p.at.x + Math.cos(piece.angle()) * r, z = p.at.z + Math.sin(piece.angle()) * r;
		double y = piece.y() - 1 + rise * (1 + lift) + Math.max(0, t - 545) * 0.008;
		float scale = 0.55f + GreyBox.h(p.seed, i, 37) * 0.4f;
		// Nothing parks in front of the lens: a block coming within five blocks of whoever's camera shrinks away, gone at two and a half
		// (shot 10 looks up the pillar from among them; one hid it whole).
		double ex = x - camX, ey = y - camY, ez = z - camZ; // the block is drawn centred on (x, y, z)
		float near = Mth.clamp((float) (Math.sqrt(ex * ex + ey * ey + ez * ez) - 2.5) / 2.5f, 0, 1);
		// Nor in front of the player: one crossing the line from the camera to them shrinks softly as it nears the line, and grows back past it.
		double tx = p.at.x - camX, ty = p.at.y + 0.7 - camY, tz = p.at.z - camZ, tl = tx * tx + ty * ty + tz * tz;
		if (tl > 1.0E-6) {
			double s = (ex * tx + ey * ty + ez * tz) / tl;
			if (s > 0.05 && s < 0.85) {
				double dx = ex - tx * s, dy = ey - ty * s, dz = ez - tz * s;
				near = Math.min(near, Mth.clamp((float) (Math.sqrt(dx * dx + dy * dy + dz * dz) - 0.5) / 1.5f, 0, 1));
			}
		}
		// In the film's own shots, nor across the middle of the frame (the pillar, the player: whatever the shot is on): one drifting into a
		// cone about the view's centre (wider further off) shrinks softly away there, and grows back as it leaves it.
		if (own) {
			double depth = ex * lookX + ey * lookY + ez * lookZ;
			if (depth > 0) {
				double ax = ex - lookX * depth, ay = ey - lookY * depth, az = ez - lookZ * depth;
				double off = Math.sqrt(ax * ax + ay * ay + az * az);
				near = Math.min(near, Mth.clamp((float) ((off - (0.8 + 0.07 * depth)) / 1.2), 0, 1));
			}
		}
		scale *= near;
		if (scale <= 0.02f) return false;
		DEBRIS_AT[0] = x;
		DEBRIS_AT[1] = y;
		DEBRIS_AT[2] = z;
		DEBRIS_AT[3] = lt * (1.2f + GreyBox.h(p.seed, i, 36) * 2.4f);
		DEBRIS_AT[4] = scale;
		return true;
	}

	/** Into the layer: an orange glow behind each torn-up block (the block itself hides its middle: a rim of firelight round it). */
	private static void debrisGlow(CinematicClient.Playing p, State st, Sequence.Element e, float t) {
		float fireLit = smooth(100, 116, t) * (1 - smooth(440, 460, t));
		if (fireLit <= 0) return;
		Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.5f);
		boolean own = CinematicClient.own() == p;
		for (Piece piece : st.debris) {
			if (!debrisAt(p, piece, e, t, Fx.camX, Fx.camY, Fx.camZ, Fx.lookX, Fx.lookY, Fx.lookZ, own)) continue;
			float flick = 0.85f + 0.15f * Mth.sin(t * 0.9f + piece.index() * 2.3f);
			Fx.sprite(DEBRIS_AT[0], DEBRIS_AT[1], DEBRIS_AT[2], 1.1f * (float) DEBRIS_AT[4], 0, ORANGE[0], ORANGE[1], ORANGE[2], 0.85f * fireLit * flick);
		}
	}

	/** Reused by the torn-up blocks each frame (nothing allocated per block). */
	private static final org.joml.Quaternionf SPIN = new org.joml.Quaternionf();
	private static final BlockPos.MutableBlockPos AT = new BlockPos.MutableBlockPos();
	private static final double[] DEBRIS_AT = new double[5];

	private static void absorption(CinematicClient.Playing p, float lt, float life) {
		double cx = p.at.x, cy = p.at.y + 0.65, cz = p.at.z;
		float f = fade(lt, life, 4, 4);
		float build = 0.45f + 0.55f * smooth(0, life * 0.7f, lt); // denser as it goes
		// Read from where anyone stands: up close (the film's own push-in) as built, from further off thicker, so the storm still
		// shows round the player to someone 24 blocks away.
		float far = (float) Math.max(1, Fx.distanceTo(cx, cy, cz) / 8);
		// As the camera whips through them in the last ten ticks, the streaks dim a little: the rush stays gold, never a pale wash.
		float rush = 1 - 0.35f * smooth(life - 10, life - 5, lt);
		Fx.begin(FxTextures.STREAK, Fx.Mode.HEAT, 2.0f * rush);
		int n = (int) (count(450) * build);
		for (int i = 0; i < n; i++) {
			double a = GreyBox.h(p.seed, i, 15) * Math.PI * 2, el = (GreyBox.h(p.seed, i, 16) - 0.4) * 1.5;
			double dx = Math.cos(a) * Math.cos(el), dy = Math.sin(el), dz = Math.sin(a) * Math.cos(el);
			float period = 8 + GreyBox.h(p.seed, i, 17) * 9, k = (lt / period + GreyBox.h(p.seed, i, 18)) % 1;
			double r = 0.45 + Math.pow(1 - k, 1.6) * (3 + GreyBox.h(p.seed, i, 19) * 7);
			double len = 0.5 + r * 0.22;
			double hx = cx + dx * r, hy = cy + dy * r, hz = cz + dz * r;
			float[] col = i % 5 == 0 ? GOLD_LIGHT : GOLD;
			Fx.streak(hx + dx * len, hy + dy * len, hz + dz * len, hx, hy, hz, (0.05f + 0.07f * GreyBox.h(p.seed, i, 20)) * far, col[0], col[1], col[2],
					0.9f * Mth.sin(Mth.PI * k) * f);
		}
		// Sparks with them, faster; and broad soft streaks filling the space between.
		Fx.begin(FxTextures.SPARK, Fx.Mode.HEAT, 2.2f);
		int sparks = (int) (count(160) * build);
		for (int i = 0; i < sparks; i++) {
			double a = GreyBox.h(p.seed, i, 21) * Math.PI * 2, el = (GreyBox.h(p.seed, i, 22) - 0.45) * 1.6;
			double dx = Math.cos(a) * Math.cos(el), dy = Math.sin(el), dz = Math.sin(a) * Math.cos(el);
			float period = 6 + GreyBox.h(p.seed, i, 23) * 6, k = (lt / period + GreyBox.h(p.seed, i, 24)) % 1;
			double r = 0.4 + (1 - k) * (2 + GreyBox.h(p.seed, i, 25) * 6);
			double hx = cx + dx * r, hy = cy + dy * r, hz = cz + dz * r;
			Fx.streak(hx + dx * 0.35 * far, hy + dy * 0.35 * far, hz + dz * 0.35 * far, hx, hy, hz, 0.07f * far, GOLD_LIGHT[0], GOLD_LIGHT[1], GOLD_LIGHT[2],
					Mth.sin(Mth.PI * k) * f);
		}
		// Broad soft streaks of orange flame filling the space between (the reference's wall of fire, thinned so the player still shows).
		Fx.begin(FxTextures.STREAK, Fx.Mode.LIGHT, 1.0f);
		for (int i = 0; i < count(90); i++) {
			double a = GreyBox.h(p.seed, i, 26) * Math.PI * 2, el = (GreyBox.h(p.seed, i, 27) - 0.4) * 1.2;
			double dx = Math.cos(a) * Math.cos(el), dy = Math.sin(el), dz = Math.sin(a) * Math.cos(el);
			float k = (lt / 14f + GreyBox.h(p.seed, i, 28)) % 1;
			double r = 0.8 + (1 - k) * 7;
			double hx = cx + dx * r, hy = cy + dy * r, hz = cz + dz * r;
			Fx.streak(hx + dx * 2.6, hy + dy * 2.6, hz + dz * 2.6, hx, hy, hz, 0.6f * far, ORANGE[0], ORANGE[1], ORANGE[2], 0.3f * Mth.sin(Mth.PI * k) * f * build);
		}
		// The light at the heart of it (from far off, a glow about the player as wide as the storm round them). In its last ten ticks (the
		// whip past the player) it flares: a wide gold glow the camera rushes through, as the reference's does, kept saturated gold (never
		// a white frame) and pulled in about the player so their head still shows; with Flashes: Reduced, a warm glow only.
		float whip = smooth(life - 10, life - 5, lt) * (1 - smooth(life - 3, life, lt));
		boolean reduced = CinematicSettings.reducedFlashes;
		Fx.begin(FxTextures.GLOW, Fx.Mode.HEAT, 2.6f);
		Fx.sprite(cx, cy, cz, (1.6f + 0.6f * build) * (float) Math.sqrt(far) * (1 - 0.6f * whip), 0, GOLD[0], GOLD[1], GOLD[2],
				0.55f * f * build * (1 - 0.4f * whip));
		if (whip > 0) {
			Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
			Fx.sprite(cx, cy, cz, 2.4f * (1 + 1.2f * whip), 0, ORANGE[0], ORANGE[1], ORANGE[2], (reduced ? 0.2f : 0.4f) * whip);
		}
		if (far > 1.5f) {
			Fx.begin(FxTextures.GLOW, Fx.Mode.LIGHT, 1.0f);
			Fx.sprite(cx, cy, cz, 7f * build, 0, ORANGE[0], ORANGE[1], ORANGE[2], 0.45f * f * Math.min(1, far - 1.5f));
		}
	}

	private static void slashes(CinematicClient.Playing p, Sequence.Element e, float lt) {
		double yaw = Math.toRadians(p.facing);
		double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		for (int k = 0; k < 2; k++) {
			float at = e.params().length > k ? e.params()[k] : (k == 0 ? 15 : 35);
			// Its head crosses the whole frame in 4 ticks; then it hangs there, the whole length of it, for 3 more; then its tail catches up
			// over 5 more as it fades.
			float u = (lt - at) / 4f;
			if (u <= 0 || u >= 3f) continue;
			float head = Math.min(1, u), tail = u <= 1.75f ? 0 : (u - 1.75f) / 1.25f;
			double side0 = k == 0 ? -4.6 : 4.6, side1 = -side0, fw = 0.9 + k * 0.2;
			double ax = p.at.x + rx * side0 + fx * fw, ay = p.at.y + 3.6, az = p.at.z + rz * side0 + fz * fw;
			double bx = p.at.x + rx * side1 + fx * fw, by = p.at.y - 1.0, bz = p.at.z + rz * side1 + fz * fw;
			float fadeOut = u > 1.75f ? 1 - (u - 1.75f) / 1.25f : 1;
			for (int pass = 0; pass < 2; pass++) {
				if (pass == 0) Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, 1.2f);
				else Fx.begin(FxTextures.RIBBON, Fx.Mode.HEAT, 3.0f);
				Fx.path();
				for (int s = 0; s <= 12; s++) {
					float v = tail + (head - tail) * s / 12f;
					float along = s / 12f;
					float w = (pass == 0 ? 2.4f : 0.5f) * Mth.sin(Mth.PI * Mth.clamp(along * 0.85f + 0.1f, 0, 1));
					Fx.point(Mth.lerp(v, ax, bx), Mth.lerp(v, ay, by), Mth.lerp(v, az, bz), w, v * 2, GOLD[0], GOLD[1], GOLD[2],
							(pass == 0 ? 0.3f : 1f) * fadeOut);
				}
				Fx.ribbon();
			}
		}
	}

	// --- Act 3 ---

	/** The nascent soul's violet light through the body: three pulses on the heartbeat (ticks 5, 15, 25), each brighter. */
	private static void soulGlow(WorldRenderContext ctx, CinematicClient.Playing p, float lt) {
		float pulse = 0;
		for (int k = 0; k < 3; k++) {
			float at = 5 + k * 10, d = lt - at;
			float shape = d < 0 ? Math.max(0, 1 + d / 2f) : Math.max(0, 1 - d / 7f);
			pulse = Math.max(pulse, (0.5f + k * 0.18f) * shape); // each brighter, the last still violet (never a white bloom)
		}
		// To a watcher further off the light at its heart reads larger (24 blocks off: about three times).
		float far = (float) Math.max(1, Fx.distanceTo(p.at.x, p.at.y + 0.6, p.at.z) / 8);
		float glow = 0.12f + 0.5f * pulse; // the edges' light: a rim, not a shell
		Minecraft mc = Minecraft.getInstance();
		Entity entity = mc.level.getEntity(p.entityId);
		if (entity instanceof Player player) {
			Fx.end();
			SoulRenderer.drawInnerLight(ctx, player, glow, ctx.tickDelta());
		}
		// Its heart: the light at the chest, just before the body's front (the layer keeps the world's depth, the body's too), with
		// rays reaching out from it on each beat.
		double yaw = Math.toRadians(p.facing);
		double cx = p.at.x - Math.sin(yaw) * 0.32, cy = p.at.y + 0.62, cz = p.at.z + Math.cos(yaw) * 0.32;
		Fx.begin(FxTextures.GLOW, Fx.Mode.LIGHT, 1.6f);
		Fx.sprite(cx, cy, cz, (0.45f + 0.8f * pulse) * far, 0, VIOLET[0], VIOLET[1], VIOLET[2], 0.3f + 0.6f * pulse);
		Fx.begin(FxTextures.GLOW, Fx.Mode.HEAT, 2.0f);
		Fx.sprite(cx, cy, cz, (0.18f + 0.3f * pulse) * (float) Math.sqrt(far), 0, LAVENDER[0], LAVENDER[1], LAVENDER[2], 0.3f + 0.32f * pulse);
		Fx.begin(FxTextures.RAYS, Fx.Mode.LIGHT, 1.5f);
		Fx.sprite(cx, cy, cz, (0.5f + 0.8f * pulse) * far, lt * 0.03f, LAVENDER[0], LAVENDER[1], LAVENDER[2], 0.55f * pulse);
	}

	private static void pillar(CinematicClient.Playing p, State st, Sequence.Element e, float t, float lt, float life) {
		float radius = e.params().length > 0 ? e.params()[0] : 1.5f;
		double x = p.at.x, z = p.at.z, foot = st.ground.at(x, z) - 0.2, top = topY(p);
		double climb = Ease.EXPO_OUT.between(lt, 0, 6);
		double head = foot + (top - foot) * climb;
		float thick = 0.3f + 0.7f * Ease.CUBIC_OUT.between(lt, 3, 14);
		// After Act 3 it narrows to a slender beam (the soul climbs into it in Act 4, its close shots orbiting a few blocks off).
		thick *= 1 - 0.55f * smooth(330, 350, t);
		float out = 1 - smooth(life - 20, life, lt);
		// Seen from far off (the far shot, 120 blocks away) drawn wider, as wide to the eye as from 45 blocks: the column, not the burst at
		// its top, stays the subject.
		float farW = (float) Math.max(1, Math.hypot(Fx.camX - x, Fx.camZ - z) / 45);
		float r = radius * thick * farW;
		// Flaring a little as it rises (to 1.5 times its foot's width at the clouds).
		float topR = r * (float) (1 + 0.5 * climb);
		float flicker = 0.92f + 0.08f * Mth.sin(t * 1.7f) * Mth.sin(t * 0.53f);
		float height = (float) (head - foot);
		float scroll = -t * 0.09f;
		// Looked up along (its length folded toward the vanishing point, every layer piling on the last), its heat is held lower, so the
		// shaft stays gold flame round a narrow cream core instead of one white wedge; the orange tongues licking up it take over.
		float endOn = smooth(0.6f, 0.95f, Fx.lookY);
		float dim = 1 - 0.5f * endOn;
		// The wide haze about it, facing the camera whichever way it's seen.
		Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, 1.0f);
		Fx.path();
		Fx.point(x, foot, z, r * 9, 0, ORANGE[0], ORANGE[1], ORANGE[2], 0.25f * out * dim);
		Fx.point(x, head, z, topR * 9, height / 20, ORANGE[0], ORANGE[1], ORANGE[2], 0.18f * out * dim);
		Fx.ribbon();
		// The gold body of streaming flame (both halves of each tube add, so the middle is brightest: kept low enough that the body stays
		// gold and orange, white only in the core), the cream-white core. (No wide outer tube: seen from near the foot its straight sides
		// stood out as a flat wedge; the haze above and the tongues below give the glow round it.)
		Fx.begin(FxTextures.FLOW, Fx.Mode.HEAT, 1.2f);
		Fx.tube(x, z, foot, head, r, topR, 20, 2, scroll, scroll + height / 6, GOLD[0], GOLD[1], GOLD[2], 0.44f * out * flicker * dim, 1.0f, 0.45f);
		Fx.tube(x, z, foot, head, r * 0.72f, topR * 0.72f, 16, 3, scroll * 1.4f + 0.3f, scroll * 1.4f + 0.3f + height / 5,
				ORANGE[0], ORANGE[1], ORANGE[2], 0.36f * out * dim, 0.8f, 0.4f);
		Fx.begin(FxTextures.RIBBON, Fx.Mode.HEAT, 2.4f);
		Fx.path();
		Fx.point(x, foot, z, r * 0.27f * (1 - 0.3f * endOn), scroll * 2, CREAM[0], CREAM[1], CREAM[2], out * 0.9f * (1 - 0.35f * endOn));
		Fx.point(x, head, z, topR * 0.27f * (1 - 0.3f * endOn), scroll * 2 + height / 8, CREAM[0], CREAM[1], CREAM[2], out * 0.55f * (1 - 0.35f * endOn));
		Fx.ribbon();
		// Under clouds, its head flares out into the fire it feeds (the top seventh of it widening to two and a half times), so the column
		// runs into the vortex's middle instead of meeting it as a thin rod.
		if (p.probe.openSky()) {
			float neck = smooth(6, 16, lt) * out;
			double neckFoot = head - height * 0.14;
			Fx.begin(FxTextures.FLOW, Fx.Mode.LIGHT, 1.2f);
			Fx.tube(x, z, neckFoot, head, topR, topR * 2.6f, 20, 2, scroll * 0.8f, scroll * 0.8f + height * 0.14f / 6,
					ORANGE[0], ORANGE[1], ORANGE[2], 0.4f * neck * dim, 1.2f, 0.3f);
		}
		// Tongues of flame streaming up it, spiralling: gold ones hugging the body, and wider orange ones licking out past its edge.
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 0) Fx.begin(FxTextures.RIBBON, Fx.Mode.HEAT, 1.5f);
			else Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, 1.2f);
			int tongues = pass == 0 ? 26 : 18;
			for (int i = 0; i < tongues; i++) {
				int id = i + pass * 40;
				double a0 = GreyBox.h(p.seed, id, 100) * Math.PI * 2;
				float speed = 1.8f + GreyBox.h(p.seed, id, 101) * 1.8f, len = 10 + GreyBox.h(p.seed, id, 102) * 16;
				float rk = pass == 0 ? 0.75f + GreyBox.h(p.seed, id, 103) * 0.5f : 1.05f + GreyBox.h(p.seed, id, 103) * 0.35f;
				double span = top - foot + len;
				double base = ((lt * speed + GreyBox.h(p.seed, id, 104) * span) % span) - len + foot;
				float[] col = pass == 0 ? GOLD : ORANGE;
				Fx.path();
				for (int k = 0; k <= 16; k++) {
					float u = k / 16f;
					double y = base + u * len;
					if (y < foot || y > head) continue;
					float rr = Mth.lerp((float) ((y - foot) / Math.max(1, height)), r, topR) * rk;
					double a = a0 + y * 0.09 + t * 0.03;
					float w = (pass == 0 ? 0.9f : 1.5f) * rr * Mth.sin(Mth.PI * u);
					Fx.point(x + Math.cos(a) * rr, y, z + Math.sin(a) * rr, w, u * 2 - t * 0.05f, col[0], col[1], col[2],
							(pass == 0 ? 0.62f * dim : 0.6f * (1 + 0.25f * endOn)) * Mth.sin(Mth.PI * u) * out);
				}
				Fx.ribbon();
			}
		}
		// Its head as it climbs, and its foot: gold, a small hot heart (never a white slab on the ground; both held lower still with
		// Flashes: Reduced).
		float flash = CinematicSettings.reducedFlashes ? 0.45f : 1;
		float headGlow = (1 - smooth(5, 12, lt)) * flash;
		Fx.begin(FxTextures.GLOW, Fx.Mode.HEAT, 3.0f * flash);
		if (headGlow > 0) Fx.sprite(x, head, z, r * 3.2f, 0, GOLD[0], GOLD[1], GOLD[2], headGlow);
		Fx.begin(FxTextures.GLOW, Fx.Mode.HEAT, 2.2f * flash);
		float footFlare = 0.6f + 0.6f * (1 - smooth(0, 18, lt)) * flash;
		Fx.sprite(x, foot + 0.9, z, r * 1.6f * footFlare, 0, GOLD[0], GOLD[1], GOLD[2], 0.45f * out);
	}

	/** Dark specks of debris carried up inside the pillar. */
	private static void pillarSpecks(CinematicClient.Playing p, float lt, float life) {
		if (lt < 4) return;
		double x = p.at.x, z = p.at.z, foot = p.at.y, top = topY(p);
		float out = 1 - smooth(life - 20, life, lt);
		Fx.begin(FxTextures.PUFF, Fx.Mode.COVER, 1);
		int n = count(36);
		for (int i = 0; i < n; i++) {
			double a = GreyBox.h(p.seed, i, 110) * Math.PI * 2, rr = 0.3 + GreyBox.h(p.seed, i, 111) * 1.4;
			float speed = 1.2f + GreyBox.h(p.seed, i, 112) * 1.5f;
			double span = top - foot;
			double y = foot + (lt * speed + GreyBox.h(p.seed, i, 113) * span) % span;
			Fx.sprite(x + Math.cos(a + lt * 0.05) * rr, y, z + Math.sin(a + lt * 0.05) * rr, 0.09f + 0.08f * GreyBox.h(p.seed, i, 114), lt * 0.1f + i,
					0.08f, 0.05f, 0.03f, 0.85f * out);
		}
	}

	private static void pillarRays(CinematicClient.Playing p, State st, float lt, float life) {
		double x = p.at.x, z = p.at.z, foot = st.ground.at(x, z);
		float burst = Ease.EXPO_OUT.between(lt, 0, 10), out = 1 - smooth(4, life, lt);
		Fx.begin(FxTextures.STREAK, Fx.Mode.HEAT, 2.4f);
		int n = count(48);
		for (int i = 0; i < n; i++) {
			double a = GreyBox.h(p.seed, i, 120) * Math.PI * 2, el = 0.15 + GreyBox.h(p.seed, i, 121) * 0.9;
			double dx = Math.cos(a) * Math.cos(el), dy = Math.sin(el), dz = Math.sin(a) * Math.cos(el);
			double reach = (2.5 + GreyBox.h(p.seed, i, 122) * 6.5) * burst;
			double len = 1.2 + reach * 0.45;
			double hx = x + dx * reach, hy = foot + 0.3 + dy * reach, hz = z + dz * reach;
			Fx.streak(hx - dx * len, hy - dy * len, hz - dz * len, hx, hy, hz, 0.12f, GOLD[0], GOLD[1], GOLD[2], 0.9f * out);
		}
		// Embers flung up along the pillar, slowing as they rise.
		Fx.begin(FxTextures.GLOW, Fx.Mode.LIGHT, 2.0f);
		int embers = count(70);
		for (int i = 0; i < embers; i++) {
			double a = GreyBox.h(p.seed, i, 123) * Math.PI * 2, rr = 1.5 + GreyBox.h(p.seed, i, 124) * 4;
			float delay = GreyBox.h(p.seed, i, 125) * 20, el = Math.max(0, lt - delay);
			double y = foot + 0.5 + Math.sqrt(el) * (2.2 + GreyBox.h(p.seed, i, 126) * 3);
			float alpha = Mth.clamp(el / 3f, 0, 1) * (1 - smooth(life * 0.5f, life, el));
			Fx.sprite(x + Math.cos(a) * rr, y, z + Math.sin(a) * rr, 0.06f + 0.05f * GreyBox.h(p.seed, i, 127), 0, EMBER[0], EMBER[1], EMBER[2], alpha);
		}
	}

	private static void dustRing(CinematicClient.Playing p, State st, float lt, float life, boolean cover) {
		float u = lt / life;
		float spread = Ease.CUBIC_OUT.apply(u);
		if (cover) Fx.begin(FxTextures.PUFF, Fx.Mode.COVER, 1);
		else Fx.begin(FxTextures.PUFF, Fx.Mode.LIGHT, 1.0f);
		int n = count(44);
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n + GreyBox.h(p.seed, i, 130) * 0.3;
			double r = 1.2 + spread * (7 + GreyBox.h(p.seed, i, 131) * 4);
			double x = p.at.x + Math.cos(a) * r, z = p.at.z + Math.sin(a) * r;
			double y = st.ground.at(x, z) + 0.35 + spread * (0.6 + GreyBox.h(p.seed, i, 132) * 0.8);
			float size = 0.7f + spread * (1.6f + GreyBox.h(p.seed, i, 133) * 1.2f);
			float alpha = (float) Math.pow(1 - u, 1.3) * Mth.clamp(lt / 2f, 0, 1);
			if (cover) Fx.sprite(x, y, z, size, i * 1.7f + lt * 0.02f, DUST[0] * 1.3f, DUST[1] * 1.2f, DUST[2], 0.6f * alpha);
			else Fx.sprite(x, y, z, size * 0.9f, i * 1.7f + lt * 0.02f, ORANGE[0], ORANGE[1], ORANGE[2], 0.05f * alpha); // lit faintly by the pillar (dust, not a bar of light)
		}
	}

	/** How much larger what spreads across the cloud base is drawn than its numbers say: as large to the eye 128 blocks up as 48 up. */
	private static float farScale(CinematicClient.Playing p) {
		return (float) Math.max(1, (topY(p) - groundY(p)) / 48);
	}

	/**
	 * Where the pillar meets the cloud base (or the ceiling): a great mushroom of fire billowing out under it, about fifty blocks across
	 * under clouds 128 blocks up. Its billows are drawn with the smoke and cloud, before every light, so the pillar runs into its very
	 * middle in plain sight from anywhere (seen from below or from far off, the column touches the vortex's centre): dark smoke at its rim
	 * first, a body of fire, then rings of billows from the rim in, each covering the last (every billow shaded by its own texture, its
	 * colour well under white, so the fire reads as billows and never adds up into a white sun), deep orange outside to pale gold at the
	 * middle, cooling as it spreads. With the light: the heat glowing through its middle, and a glow where the pillar meets it.
	 */
	private static void mushroom(CinematicClient.Playing p, Sequence.Element e, float lt, float life, boolean cover) {
		float s = farScale(p);
		float size = (e.params().length > 0 ? e.params()[0] : 20) * s;
		float spread = Ease.EXPO_OUT.between(lt, 0, 18) * (0.7f + 0.3f * smooth(18, 140, lt));
		float out = 1 - smooth(life - 25, life, lt);
		float cool = smooth(30, 220, lt);
		double x = p.at.x, z = p.at.z, y = topY(p) - (p.probe.openSky() ? 4.5 : 0.1);
		if (!cover) {
			mushroomLight(p, x, y, z, size, spread, lt, out);
			return;
		}
		Fx.begin(FxTextures.PUFF, Fx.Mode.COVER, 1);
		int smoke = MUSHROOM_RINGS[3];
		for (int i = 0; i < smoke; i++) mushroomPuff(p, 3, i, smoke, x, y, z, size, spread, lt, out, cool);
		// A body of deep orange fire behind the billows, inside the smoke (a disc of the cloud's own billows, opaque but at its rim): where two
		// billows part, fire shows, never a dark hole.
		Fx.begin(FxTextures.CLOUD, Fx.Mode.COVER, 1);
		FIRE_FILL[1] = 0.36f * (1 - 0.2f * cool);
		deckDisc(x, y - size * 0.13f * spread, z, size * 0.9f * spread, 1 / 40f, lt * 0.002f, FIRE_FILL, out * Mth.clamp(spread * 1.5f, 0, 1));
		Fx.begin(FxTextures.PUFF, Fx.Mode.COVER, 1);
		for (int ring = 2; ring >= 0; ring--) {
			int n = MUSHROOM_RINGS[ring];
			for (int i = 0; i < n; i++) mushroomPuff(p, ring, i, n, x, y, z, size, spread, lt, out, cool);
		}
	}

	/** The mushroom's light: its hottest billows glowing, the heat through its middle, and the join with the pillar lit. */
	private static void mushroomLight(CinematicClient.Playing p, double x, double y, double z, float size, float spread, float lt, float out) {
		// The hottest billows glowing (light over the inner two rings' own shading), and the heat through the middle: gold, never white.
		Fx.begin(FxTextures.PUFF, Fx.Mode.LIGHT, 1.0f);
		for (int ring = 1; ring >= 0; ring--) {
			int n = MUSHROOM_RINGS[ring];
			for (int i = 0; i < n; i++) mushroomGlow(p, ring, i, n, x, y, z, size, spread, lt, out);
		}
		Fx.begin(FxTextures.SOFT, Fx.Mode.LIGHT, 1.0f);
		Fx.flat(x, y - size * 0.2f * spread, z, size * 0.55f * spread, 0, GOLD[0], GOLD[1], GOLD[2], 0.2f * out);
		// Where the column meets the fire, facing whoever looks: the fire round the pillar's head lit, so the two read as one from any side
		// (small and warm: the billows and veins show through it, never a white bulb on a stem).
		Fx.sprite(x, y - size * 0.26f * spread, z, size * 0.2f * (0.6f + 0.4f * spread), 0, ORANGE[0], ORANGE[1], ORANGE[2], 0.32f * out);
		Fx.begin(FxTextures.GLOW, Fx.Mode.HEAT, 1.5f);
		Fx.sprite(x, y - size * 0.22f * spread, z, size * 0.07f * (0.6f + 0.4f * spread), 0, GOLD[0], GOLD[1], GOLD[2], 0.42f * out);
	}

	/** The mushroom's rings of billows, from the middle out (the last its rim of cooler fire), and how far out each lies. */
	private static final int[] MUSHROOM_RINGS = {8, 12, 16, 14};
	private static final float[] MUSHROOM_FRAC = {0.2f, 0.46f, 0.72f, 0.94f};

	/** Where billow {@code i} of {@code ring} is, into PUFF_AT (x, y, z, its half-size, its turn). */
	private static void mushroomPlace(CinematicClient.Playing p, int ring, int i, int n, double x, double y, double z, float size, float spread,
									  float lt) {
		int id = ring * 40 + i;
		// Spaced round its ring with a jitter (the rim's widely: no even scallops), drifting slowly round (rings turning opposite ways),
		// each breathing as it billows.
		double jitter = ring == 3 ? 1.0 : 0.6;
		double a = (i + jitter * GreyBox.h(p.seed, id, 140)) * Math.PI * 2 / n + lt * 0.0025 * (ring % 2 == 0 ? 1 : -1);
		float frac = MUSHROOM_FRAC[ring] * (ring == 3 ? 0.8f + 0.4f * GreyBox.h(p.seed, id, 141) : 0.88f + 0.24f * GreyBox.h(p.seed, id, 141));
		double r = size * spread * frac;
		// Hanging under the base: lowest at the middle, where it feeds, the rim curling down again.
		float drop = (ring == 0 ? 0.2f : ring == 1 ? 0.15f : ring == 2 ? 0.11f : 0.13f) * (0.8f + 0.4f * GreyBox.h(p.seed, id, 143));
		float breathe = 1 + 0.06f * Mth.sin(lt * 0.12f + id * 1.7f);
		float vary = ring == 3 ? 0.6f + 0.8f * GreyBox.h(p.seed, id, 142) : ring == 2 ? 0.7f + 0.6f * GreyBox.h(p.seed, id, 142)
				: 0.85f + 0.3f * GreyBox.h(p.seed, id, 142);
		float half = size * (ring == 3 ? 0.27f : ring == 0 ? 0.25f : 0.22f) * vary * (0.45f + 0.55f * spread) * breathe;
		PUFF_AT[0] = x + Math.cos(a) * r;
		PUFF_AT[1] = y - size * drop * spread;
		PUFF_AT[2] = z + Math.sin(a) * r;
		PUFF_AT[3] = half;
		PUFF_AT[4] = lt * 0.008f * ((id % 3) - 1) + id;
	}

	private static final double[] PUFF_AT = new double[5];
	/** The mushroom's body of fire behind its billows (its green cools with time: set each frame, nothing allocated). */
	private static final float[] FIRE_FILL = {0.85f, 0.36f, 0.07f};

	private static void mushroomPuff(CinematicClient.Playing p, int ring, int i, int n, double x, double y, double z, float size, float spread,
									 float lt, float out, float cool) {
		mushroomPlace(p, ring, i, n, x, y, z, size, spread, lt);
		int id = ring * 40 + i;
		float vary = GreyBox.h(p.seed, id, 144) - 0.5f;
		if (ring == 3) { // the rim: cooler fire, deep red-orange darkening toward smoke, each billow its own shade (never an even brown ring)
			Fx.sprite(PUFF_AT[0], PUFF_AT[1], PUFF_AT[2], (float) PUFF_AT[3] * 1.15f, (float) PUFF_AT[4], 0.5f * (1 + vary * 0.5f),
					0.17f * (1 + vary * 0.6f), 0.045f, 0.85f * out * Mth.clamp(spread * 1.3f, 0, 1));
			return;
		}
		// Fire: pale gold at the middle, orange, deep orange out at the rim; each billow its own shade; cooling as it spreads.
		float red, green, blue;
		if (ring == 0) {
			red = 1.0f;
			green = 0.74f + 0.08f * vary;
			blue = 0.3f;
		} else if (ring == 1) {
			red = 0.97f;
			green = 0.56f + 0.1f * vary;
			blue = 0.14f;
		} else {
			red = 0.86f + 0.08f * vary;
			green = 0.36f + 0.08f * vary;
			blue = 0.07f;
		}
		green *= 1 - 0.22f * cool;
		blue *= 1 - 0.5f * cool;
		Fx.sprite(PUFF_AT[0], PUFF_AT[1], PUFF_AT[2], (float) PUFF_AT[3], (float) PUFF_AT[4], red, green, blue, out * Mth.clamp(spread * 1.5f, 0, 1));
	}

	private static void mushroomGlow(CinematicClient.Playing p, int ring, int i, int n, double x, double y, double z, float size, float spread,
									 float lt, float out) {
		mushroomPlace(p, ring, i, n, x, y, z, size, spread, lt);
		float flick = 0.8f + 0.2f * Mth.sin(lt * 0.5f + (ring * 40 + i) * 2.1f);
		float[] col = ring == 0 ? GOLD : ORANGE;
		Fx.sprite(PUFF_AT[0], PUFF_AT[1], PUFF_AT[2], (float) PUFF_AT[3] * 0.8f, (float) PUFF_AT[4], col[0], col[1], col[2],
				(ring == 0 ? 0.3f : 0.22f) * out * flick);
	}

	private static void veins(CinematicClient.Playing p, State st, float lt, float life) {
		float s = CinematicCamera.heightScale(p);
		// As bold to the eye under high clouds as under a low ceiling: the ribbon's bright core is a third of its width, so a vein 128
		// blocks up is drawn several blocks wide.
		float wide = (float) Math.max(1, (topY(p) - groundY(p)) / 40) * 2.6f;
		// Out well past the fire's own circle, across the cloud base (the fire is about fifty blocks across under clouds 128 blocks up).
		float maxLen = 160 * s;
		float brighten = 0.6f + 0.4f * smooth(0, 40, lt);
		float out = 1 - smooth(life - 14, life, lt);
		double x = p.at.x, z = p.at.z, y = topY(p) - (p.probe.openSky() ? 4.6 : 0.06);
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 0) Fx.begin(FxTextures.RIBBON, Fx.Mode.LIGHT, 1.0f);
			else Fx.begin(FxTextures.RIBBON, Fx.Mode.HEAT, 1.5f);
			for (int i = 0; i < st.veinSegments; i++) {
				int o = i * VEIN;
				float d0 = st.veins[o + 4], d1 = st.veins[o + 5];
				// Each vein's front, crawling out at its own pace from its own moment.
				float front = Ease.CUBIC_OUT.between(lt - st.veins[o + 9], 0, 50) * maxLen * st.veins[o + 8];
				if (d0 >= front) continue;
				float cut = d1 <= front ? 1 : (front - d0) / Math.max(1.0E-3f, d1 - d0);
				double x0 = x + st.veins[o], z0 = z + st.veins[o + 1];
				double x1 = x + Mth.lerp(cut, st.veins[o], st.veins[o + 2]), z1 = z + Mth.lerp(cut, st.veins[o + 1], st.veins[o + 3]);
				float fresh = 1 - Mth.clamp((front - d1) / (5 * s), 0, 1);
				float flick = 0.75f + 0.25f * Mth.sin(lt * 0.9f + st.veins[o + 7]);
				float w = st.veins[o + 6] * wide * (pass == 0 ? 2.8f : 1 + 0.5f * fresh);
				// Faint over the fire's hot middle (where they would pile into one white glare), bold out over its rim and the cloud beyond.
				float reach = 0.35f + 0.65f * smooth(8 * s, 30 * s, d0);
				float a = (pass == 0 ? 0.28f : Mth.clamp(0.6f + 0.35f * fresh, 0, 1)) * brighten * flick * out * reach;
				float[] col = pass == 0 ? ORANGE : GOLD;
				// A flat strip under the cloud base: its side level and across it.
				double dx = x1 - x0, dz = z1 - z0, len = Math.sqrt(dx * dx + dz * dz);
				if (len < 1.0E-4) continue;
				double sx = -dz / len * w * 0.5, sz = dx / len * w * 0.5;
				Fx.v(x0 - sx, y, z0 - sz, 0, d0 * 0.2f, col[0], col[1], col[2], a);
				Fx.v(x0 + sx, y, z0 + sz, 1, d0 * 0.2f, col[0], col[1], col[2], a);
				Fx.v(x1 + sx, y, z1 + sz, 1, d1 * 0.2f, col[0], col[1], col[2], a);
				Fx.v(x1 - sx, y, z1 - sz, 0, d1 * 0.2f, col[0], col[1], col[2], a);
			}
		}
	}

	// --- The breakthrough player's own grade and blur ---

	private static final Vector4f SCRATCH = new Vector4f();

	/** The sequence's grade at {@code t} (its keys eased between), the radial blur aimed at the player's chest. */
	private static void look(WorldRenderContext context, CinematicClient.Playing p, float t) {
		float[] g = p.seq.gradeAt(t);
		if (g != null) FxPost.grade(g[0], g[1], g[2], g[3], g[4], g[5], g[6], g[7]);
		float radial = p.seq.radialAt(t);
		if (CinematicSettings.reduceMotion) radial *= 0.4f;
		if (radial > 0) {
			Matrix4f view = context.matrixStack().last().pose();
			Vec3 cam = context.camera().getPosition();
			SCRATCH.set((float) (p.at.x - cam.x), (float) (p.at.y + 0.7 - cam.y), (float) (p.at.z - cam.z), 1);
			view.transform(SCRATCH);
			context.projectionMatrix().transform(SCRATCH);
			float sx = 0.5f, sy = 0.5f;
			if (SCRATCH.w > 0.01f) {
				sx = Mth.clamp(SCRATCH.x / SCRATCH.w * 0.5f + 0.5f, 0.05f, 0.95f);
				sy = Mth.clamp(SCRATCH.y / SCRATCH.w * 0.5f + 0.5f, 0.05f, 0.95f);
			}
			FxPost.radial(radial, sx, sy);
		}
	}

	/** A cinematic ended or the world was left: its worked-out state let go (the map is weak, this only hastens it). */
	public static void forget(CinematicClient.Playing p) {
		STATES.remove(p);
	}

	private HeroFx() {}
}
