package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellField;
import com.example.defyingtheheavens.SpellFx;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A spell's passing moments (SpellFx), drawn where the server said: bursts of flame, steam, ash, a bell's toll and its shattering,
 * splashes, bursts of earth and splinters, metal sparks, shockwaves, flashes, explosions of fire, streaks of light, motes of life,
 * dust, ice, ripples, a broken cage of thorns, a golden core's detonation, a tear in space, leaves, chips of rock. Each piece of one
 * flies on its own path (worked out from its seed and its age, so nothing need be kept per piece) and fades. Big blows shake the
 * view of those near ({@link #shake}), and so does an earthquake underfoot.
 */
public final class ClientSpellFx {
	private static final float[] WHITE = {1, 1, 1};
	private static final float[] FIRE_CORE = {1.0f, 0.93f, 0.7f}, FIRE_MID = {1.0f, 0.55f, 0.15f}, FIRE_OUTER = {0.95f, 0.22f, 0.06f};
	private static final float[] WATER = {0.7f, 0.88f, 1.0f}, ICE = {0.75f, 0.92f, 1.0f}, LIFE = {0.35f, 1.0f, 0.45f}, GOLD = {1.0f, 0.82f, 0.4f};

	/** One moment; {@code own}: at the viewer's own feet when it came (their own cast's seal). */
	private record Moment(SpellFx.Kind kind, Vec3 at, Vec3 way, float a, float b, long start, int life, long seed, boolean own) {}

	private static final List<Moment> MOMENTS = new ArrayList<>();
	/** The view's shaking now (0 none), set by blows near and quakes underfoot. */
	private static float shaking;

	/** The server's word: a moment to draw. */
	public static void add(SpellFx.Kind kind, Vec3 at, Vec3 way, float a, float b) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		long now = mc.level.getGameTime();
		boolean own = kind == SpellFx.Kind.CAST_SEAL && mc.player != null && mc.player.position().distanceToSqr(at) < 1.0;
		MOMENTS.add(new Moment(kind, at, way, a, b, now, lifeOf(kind, a), mc.level.random.nextLong(), own));
		// Big blows shake the view of those near.
		if (mc.player != null) {
			double d = mc.player.position().distanceTo(at);
			float push = switch (kind) {
				case FIRE_EXPLOSION -> a * 0.25f;
				case CORE_DETONATION -> a * 0.5f;
				case SHOCKWAVE -> a * 0.12f;
				case EARTH_BURST -> a * 0.15f;
				default -> 0;
			};
			if (push > 0) shaking = Math.max(shaking, (float) (push / (1 + d * 0.15)));
		}
	}

	private static int lifeOf(SpellFx.Kind kind, float a) {
		return switch (kind) {
			case FLAME_BURST, SPLINTERS, METAL_SPARKS, ROCK_CHIPS, ASH -> 14;
			case STEAM, DUST, LEAVES -> 30;
			case BELL_TOLL, FLASH, RIPPLE -> 12;
			case BELL_SHATTER, SPLASH, EARTH_BURST, ICE_BURST, THORNS_BREAK -> 24;
			case SHOCKWAVE -> 16;
			case FIRE_EXPLOSION -> 26;
			case STREAK, LIFE_MOTES -> 14;
			case CORE_DETONATION -> 60;
			case RIFT_TEAR -> 30;
			case CAST_SEAL -> 36;
			case WATER_COLUMN -> 26;
		};
	}

	public static void tick() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		long now = mc.level.getGameTime();
		MOMENTS.removeIf(m -> now - m.start() > m.life() + 2);
		shaking *= 0.82f;
		if (shaking < 0.01f) shaking = 0;
		// An earthquake underfoot shakes the view as long as it lasts.
		if (mc.player != null) {
			for (Entity e : mc.level.entitiesForRendering()) {
				if (e instanceof SpellField field && field.kind() == SpellField.Kind.EARTHQUAKE && e.distanceTo(mc.player) < field.radius() + 6) {
					shaking = Math.max(shaking, 0.6f * SpellRender.fade(field.tickCount, field.life(), 4, 10));
				}
			}
		}
	}

	/** The view shaken at least this hard now (a secret realm's quake: SecretRealmClient), dying away as any shaking does. */
	public static void shakeAtLeast(float amount) {
		shaking = Math.max(shaking, amount);
	}

	/** GameRenderer#bobHurt's end: the view shaken. */
	public static void shake(PoseStack pose, float partial) {
		if (shaking <= 0) return;
		Minecraft mc = Minecraft.getInstance();
		float t = (mc.level == null ? 0 : mc.level.getGameTime()) + partial;
		float s = Math.min(3, shaking);
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 2.3f) * s * 1.2f));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 1.9f + 1) * s * 0.8f));
	}

	public static void clear() {
		MOMENTS.clear();
		shaking = 0;
	}

	/** WorldRenderEvents.AFTER_TRANSLUCENT. */
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || MOMENTS.isEmpty()) return;
		Vec3 cam = context.camera().getPosition();
		float partial = context.tickDelta();
		long now = mc.level.getGameTime();
		Quaternionf facing = context.camera().rotation();
		PoseStack pose = context.matrixStack();
		MultiBufferSource buffers = SpellBuffers.begin(context.consumers() == null ? mc.renderBuffers().bufferSource() : context.consumers());
		try {
			for (Iterator<Moment> it = List.copyOf(MOMENTS).iterator(); it.hasNext(); ) {
				Moment m = it.next();
				float age = now - m.start() + partial;
				if (age < 0 || age > m.life()) continue;
				float t = age / m.life();
				pose.pushPose();
				pose.translate(m.at().x - cam.x, m.at().y - cam.y, m.at().z - cam.z);
				Vec3 eye = cam.subtract(m.at());
				draw(m, pose, buffers, facing, eye, age, t);
				pose.popPose();
			}
		} finally {
			SpellBuffers.end();
		}
	}

	private static float h(long seed) {
		return SpellRender.hash(seed);
	}

	/** A direction from a seed, spread over a sphere (or its upper half, {@code up}). */
	private static Vec3 dir(long seed, boolean up) {
		double a = h(seed) * Math.PI * 2, z = up ? h(seed + 1) : h(seed + 1) * 2 - 1;
		double r = Math.sqrt(1 - z * z);
		return new Vec3(Math.cos(a) * r, z, Math.sin(a) * r);
	}

	/** Where a piece flung at {@code v} (blocks a tick) is after {@code age} ticks, falling by {@code gravity} and slowed by the air. */
	private static Vec3 flung(Vec3 v, float age, float gravity) {
		double drag = (1 - Math.pow(0.9, age)) / 0.1;
		return new Vec3(v.x * drag, v.y * drag - 0.5 * gravity * age * age, v.z * drag);
	}

	/**
	 * The weight of the sea come down (WATER_COLUMN, {@code size} its round): a column of sea water plunging down onto the point (its first
	 * quarter), foaming where it strikes; then bursting out in a ring of spray and drops, the column falling away, a ripple running out.
	 */
	private static void waterColumn(Moment m, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 eye, float age, float t, float size) {
		float high = size * 4.5f, strike = 0.25f;
		float[] sea = {0.38f, 0.62f, 0.92f}, foam = {0.92f, 0.97f, 1.0f};
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		VertexConsumer water = buffers.getBuffer(SpellRender.clear(SpellRender.WATER));
		if (t < strike + 0.25f) {
			// The column: its foot plunging to the ground, then its body pouring after and thinning away.
			float fall = Math.min(1, t / strike), after = Math.max(0, (t - strike) / 0.25f);
			float foot = high * (1 - fall), top = high + size * 2 - after * (high + size * 2);
			if (top > foot) {
				pose.pushPose();
				pose.translate(0, foot, 0);
				SpellRender.cylinder(water, pose.last(), top - foot, h -> size * (0.5f + 0.15f * h) * (1 - after * 0.6f), 8, 18, sea, 0.85f * (1 - after), 1.5f, 1,
						0, fv[0] + age * 0.06f, 0.8f, SpellRender.FULL_BRIGHT, null);
				pose.popPose();
			}
		}
		if (t >= strike) {
			float out = (t - strike) / (1 - strike);
			VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
			for (int i = 0; i < 10; i++) {
				Vec3 p = dir(m.seed() + 90 + i, true).multiply(1, 0.3, 1).normalize().scale(size * (0.5 + 1.4 * out));
				SpellRender.billboard(smoke, pose, facing, (float) p.x, (float) p.y + 0.4f, (float) p.z, size * (0.8f + out), i, foam, 0.75f * (1 - out), 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
			VertexConsumer drops = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
			for (int i = 0; i < 28; i++) {
				Vec3 v = dir(m.seed() + i, true).scale(0.22 * Math.sqrt(size)).add(0, 0.25, 0);
				Vec3 p = flung(v, age - m.life() * strike, 0.05f);
				if (p.y < -0.5) continue;
				SpellRender.billboard(drops, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.2f + 0.06f * size, 0, foam, 1 - out, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
			VertexConsumer ring = buffers.getBuffer(SpellRender.clear(SpellRender.RIPPLE));
			SpellRender.disc(ring, pose.last(), size * (0.6f + 2.2f * SpellRender.easeOut(out)), 0.08f, 0, foam, 0.8f * (1 - out), SpellRender.FULL_BRIGHT);
		}
	}

	private static void glow(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, Vec3 at, float size, float[] rgb, float alpha) {
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose, facing, (float) at.x, (float) at.y, (float) at.z, size, 0, rgb, alpha,
				0, 0, 1, 1, SpellRender.FULL_BRIGHT);
	}

	private static void draw(Moment m, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 eye, float age, float t) {
		float size = m.a() <= 0 ? 1 : m.a();
		long seed = m.seed();
		switch (m.kind()) {
			case FLAME_BURST -> {
				glow(buffers, pose, facing, Vec3.ZERO, 2.4f * size * (1 + t), FIRE_MID, (1 - t) * 0.8f);
				glow(buffers, pose, facing, Vec3.ZERO, 1.2f * size, FIRE_CORE, (1 - t));
				VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
				for (int i = 0; i < 10; i++) {
					Vec3 p = flung(dir(seed + i, false).scale(0.25 * size), age, -0.01f);
					SpellRender.flameTongue(flames, pose, eye, (float) p.x, (float) p.y - 0.2f * size, (float) p.z, 0.6f * size * (1 - t), 0.9f * size * (1 - t * 0.5f),
							age + i * 3, FIRE_MID, 1 - t, SpellRender.FLAME_FRAMES);
				}
				embers(buffers, pose, facing, seed, age, t, size, 12, FIRE_MID);
			}
			case STEAM -> {
				VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
				for (int i = 0; i < 8; i++) {
					Vec3 p = new Vec3((h(seed + i) - 0.5) * size, age * 0.05 * (0.7 + h(seed + i + 50)), (h(seed + i + 9) - 0.5) * size);
					SpellRender.billboard(smoke, pose, facing, (float) p.x, (float) p.y, (float) p.z, (0.6f + t * 1.4f) * size, i + age * 0.02f,
							new float[]{0.95f, 0.97f, 1.0f}, 0.65f * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				}
			}
			case ASH -> {
				VertexConsumer dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
				for (int i = 0; i < 10; i++) {
					Vec3 p = flung(dir(seed + i, true).scale(0.12), age, 0.01f);
					SpellRender.billboard(dust, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.25f, i, new float[]{0.3f, 0.28f, 0.27f}, 1 - t, 0, 0, 1, 1,
							SpellRender.FULL_BRIGHT);
				}
				embers(buffers, pose, facing, seed, age, t, 0.6f, 6, FIRE_MID);
			}
			case BELL_TOLL -> {
				VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
				for (int k = 0; k < 2; k++) {
					pose.pushPose();
					pose.translate(0, 0.4f + k * 0.8f, 0);
					SpellRender.disc(ring, pose.last(), 1.0f + t * 2.5f, 0, 0, GOLD, (1 - t) * 0.8f, SpellRender.FULL_BRIGHT);
					pose.popPose();
				}
				// The toll rung out through the bell: its gold flaring and the metal's seal ringing out under it.
				glow(buffers, pose, facing, new Vec3(0, 1, 0), 3.2f * (1 - t * 0.5f), GOLD, 0.5f * (1 - t));
				SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.SEAL_METAL)), pose.last(), 1.3f + t * 1.8f, 0.05f, t * 2, GOLD, 0.6f * (1 - t),
						SpellRender.FULL_BRIGHT);
			}
			case BELL_SHATTER -> {
				VertexConsumer gold = buffers.getBuffer(SpellRender.solid(SpellRender.GOLD));
				for (int i = 0; i < 18; i++) {
					Vec3 p = flung(dir(seed + i, true).scale(0.3), age, 0.04f).add(0, 1, 0);
					pose.pushPose();
					pose.translate(p.x, p.y, p.z);
					pose.mulPose(Axis.XP.rotation(age * 0.4f + i));
					pose.mulPose(Axis.YP.rotation(age * 0.3f + i));
					float c = 0.12f * (1 - t * 0.5f);
					SpellRender.box(gold, pose.last(), -c, -0.02f, -c, c, 0.02f, c, WHITE, 1, 0, 0, 0.3f, 0.3f, SpellRender.FULL_BRIGHT);
					pose.popPose();
				}
				glow(buffers, pose, facing, new Vec3(0, 1, 0), 4 * (1 - t), GOLD, 1 - t);
			}
			case SPLASH -> {
				VertexConsumer drops = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
				int n = (int) (10 + 8 * size);
				for (int i = 0; i < n; i++) {
					Vec3 v = dir(seed + i, true).scale(0.18 * Math.sqrt(size)).add(0, 0.15, 0);
					Vec3 p = flung(v, age, 0.04f);
					SpellRender.billboard(drops, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.15f + 0.05f * size, 0, WHITE, 1 - t, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				}
				VertexConsumer foam = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
				for (int i = 0; i < 6; i++) {
					Vec3 p = dir(seed + 40 + i, true).scale(size * 0.6 * t);
					SpellRender.billboard(foam, pose, facing, (float) p.x, (float) p.y, (float) p.z, size * (0.8f + t), i, WATER, 0.6f * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				}
				VertexConsumer ring = buffers.getBuffer(SpellRender.clear(SpellRender.RIPPLE));
				SpellRender.disc(ring, pose.last(), size * (0.5f + t * 1.5f), 0.05f, 0, WHITE, 0.7f * (1 - t), SpellRender.FULL_BRIGHT);
			}
			case EARTH_BURST, ROCK_CHIPS -> {
				int stateId = (int) m.b();
				TextureAtlasSprite sprite = SpellRender.sprite(stateId == 0 ? Block.getId(Blocks.DIRT.defaultBlockState()) : stateId);
				VertexConsumer rock = buffers.getBuffer(SpellRender.blocks());
				int n = m.kind() == SpellFx.Kind.ROCK_CHIPS ? 8 : (int) Math.min(40, 10 + size * 6);
				for (int i = 0; i < n; i++) {
					Vec3 v = dir(seed + i, true).scale(0.2 * Math.sqrt(size)).add(0, 0.1, 0);
					Vec3 p = flung(v, age, 0.05f);
					float c = (0.08f + h(seed + i + 77) * 0.12f) * (m.kind() == SpellFx.Kind.ROCK_CHIPS ? 0.6f : (float) Math.sqrt(size));
					pose.pushPose();
					pose.translate(p.x, p.y, p.z);
					pose.mulPose(Axis.XP.rotation(age * 0.4f + i));
					pose.mulPose(Axis.ZP.rotation(age * 0.3f + i));
					SpellRender.box(rock, pose.last(), -c, -c, -c, c, c, c, WHITE, 1, sprite.getU0(), sprite.getV0(), sprite.getU(6), sprite.getV(6), SpellRender.FULL_BRIGHT);
					pose.popPose();
				}
				VertexConsumer dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
				for (int i = 0; i < 6; i++) {
					Vec3 p = dir(seed + 60 + i, true).scale(size * 0.5 * (0.5 + t));
					SpellRender.billboard(dust, pose, facing, (float) p.x, (float) p.y, (float) p.z, size * (1 + t), i, WHITE, 0.7f * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				}
				if (m.kind() == SpellFx.Kind.EARTH_BURST) {
					// A ring of dust and the earth's glow running out over the ground.
					SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), size * 1.6f * SpellRender.easeOut(t), 0.1f, 0,
							new float[]{0.85f, 0.6f, 0.3f}, 0.6f * (1 - t), SpellRender.FULL_BRIGHT);
					VertexConsumer ringDust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
					for (int i = 0; i < 12; i++) {
						double a = Math.PI * 2 * i / 12 + h(seed + i);
						float d = size * 1.5f * SpellRender.easeOut(t);
						SpellRender.billboard(ringDust, pose, facing, (float) Math.cos(a) * d, 0.35f, (float) Math.sin(a) * d, size * 0.6f + 0.6f, i, WHITE, 0.5f * (1 - t),
								0, 0, 1, 1, SpellRender.FULL_BRIGHT);
					}
				}
			}
			case SPLINTERS, THORNS_BREAK -> {
				VertexConsumer bark = buffers.getBuffer(SpellRender.solid(SpellRender.BARK));
				for (int i = 0; i < 12; i++) {
					Vec3 p = flung(dir(seed + i, false).scale(0.2 * size), age, 0.04f);
					pose.pushPose();
					pose.translate(p.x, p.y, p.z);
					pose.mulPose(Axis.XP.rotation(age * 0.5f + i));
					SpellRender.box(bark, pose.last(), -0.03f, -0.03f, -0.12f, 0.03f, 0.03f, 0.12f, WHITE, 1, 0, 0, 0.2f, 0.6f, SpellRender.FULL_BRIGHT);
					pose.popPose();
				}
				leaves(buffers, pose, facing, seed, age, t, size, 6);
			}
			case LEAVES -> leaves(buffers, pose, facing, seed, age, t, size, (int) (8 + 6 * size));
			case METAL_SPARKS -> {
				VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
				Vec3 back = m.way().lengthSqr() > 1.0e-4 ? m.way().normalize().scale(-0.3) : Vec3.ZERO;
				for (int i = 0; i < 14; i++) {
					Vec3 v = dir(seed + i, false).scale(0.35 * size).add(back);
					Vec3 p0 = flung(v, age, 0.03f), p1 = flung(v, Math.max(0, age - 1.2f), 0.03f);
					List<Vec3> line = List.of(p1, p0);
					SpellRender.ribbon(spark, pose.last(), line, new float[]{0.08f, 0.08f}, eye, new float[]{1.0f, 0.95f, 0.75f}, new float[]{1 - t, 1 - t}, 0, 0.5f,
							SpellRender.FULL_BRIGHT);
				}
				glow(buffers, pose, facing, Vec3.ZERO, 1.6f * size, new float[]{1.0f, 0.95f, 0.8f}, (1 - t) * (1 - t));
				if (t < 0.4f) SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.STAR)), pose, facing, 0, 0, 0, 1.4f * size, seed % 7, WHITE,
						1 - t / 0.4f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
			case SHOCKWAVE -> {
				VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
				float r = size * SpellRender.easeOut(t);
				float[] pale = {1.0f, 0.95f, 0.85f};
				SpellRender.disc(ring, pose.last(), r, 0.15f, 0, pale, 0.7f * (1 - t), SpellRender.FULL_BRIGHT);
				// A second, slower ring behind the first, a wall of force standing up off the front, a flash where it began.
				float t2 = Math.max(0, t - 0.2f) / 0.8f;
				if (t2 > 0) SpellRender.disc(ring, pose.last(), size * 0.7f * SpellRender.easeOut(t2), 0.12f, 0, pale, 0.45f * (1 - t2), SpellRender.FULL_BRIGHT);
				SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), 1.6f * (1 - t) + 0.2f, h0 -> r, 2, 32, pale, 0.3f * (1 - t), 1, 1,
						0, 0, 0, SpellRender.FULL_BRIGHT, h0 -> 1 - h0);
				if (t < 0.25f) glow(buffers, pose, facing, new Vec3(0, 0.5, 0), size * 0.8f, WHITE, 1 - t / 0.25f);
				VertexConsumer dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
				for (int i = 0; i < 20; i++) {
					double a = Math.PI * 2 * i / 20;
					SpellRender.billboard(dust, pose, facing, (float) Math.cos(a) * r, 0.4f, (float) Math.sin(a) * r, 1.2f + size * 0.1f, i, WHITE, 0.5f * (1 - t), 0, 0, 1, 1,
							SpellRender.FULL_BRIGHT);
				}
			}
			case FLASH -> {
				int c = (int) m.b();
				float[] rgb = c == 0 ? WHITE : SpellRender.rgb(c);
				glow(buffers, pose, facing, Vec3.ZERO, size * 4 * (0.6f + t), rgb, (1 - t) * (1 - t));
				SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.SPARK)), pose, facing, 0, 0, 0, size * 8, size * 8, 0, rgb, (1 - t) * 0.7f,
						0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.STAR)), pose, facing, 0, 0, 0, size * 5 * (0.5f + t), age * 0.04f, WHITE, (1 - t),
						0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
			case FIRE_EXPLOSION -> explosion(m, pose, buffers, facing, eye, age, t, size, seed, false);
			case CORE_DETONATION -> explosion(m, pose, buffers, facing, eye, age, t, size, seed, true);
			case STREAK -> {
				int c = (int) m.b();
				float[] rgb = c == 0 ? WHITE : SpellRender.rgb(c);
				Vec3 to = m.way().subtract(m.at());
				float head = SpellRender.easeOut(Math.min(1, t * 1.4f));
				float tail = Math.max(0, head - 0.35f);
				List<Vec3> line = new ArrayList<>();
				for (int i = 0; i <= 8; i++) {
					double k = tail + (head - tail) * i / 8.0;
					Vec3 p = to.scale(k).add(0, Math.sin(Math.PI * k) * to.length() * 0.08, 0);
					line.add(p);
				}
				float[] widths = new float[9], alphas = new float[9];
				for (int i = 0; i <= 8; i++) {
					widths[i] = 0.08f + 0.18f * i / 8f;
					alphas[i] = (i / 8f) * (1 - Math.max(0, t - 0.7f) / 0.3f);
				}
				SpellRender.ribbon(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), line, widths, eye, rgb, alphas, 0, 0, SpellRender.FULL_BRIGHT);
				glow(buffers, pose, facing, line.get(8), 0.6f, rgb, 1 - Math.max(0, t - 0.7f) / 0.3f);
			}
			case LIFE_MOTES -> {
				Vec3 to = m.way().subtract(m.at());
				VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
				for (int i = 0; i < 8; i++) {
					float k = Mth.clamp(t * 1.3f - i * 0.04f, 0, 1);
					Vec3 p = to.scale(k).add((h(seed + i) - 0.5) * Mth.sin((float) Math.PI * k), (h(seed + i + 8) - 0.5) * Mth.sin((float) Math.PI * k), 0);
					SpellRender.billboard(motes, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.22f, 0, LIFE, 1 - t, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				}
			}
			case DUST -> {
				int stateId = (int) m.b();
				float[] tint = stateId == 0 ? WHITE : tintOf(stateId);
				VertexConsumer dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
				for (int i = 0; i < 6; i++) {
					Vec3 p = new Vec3((h(seed + i) - 0.5) * size * 2, 0.2 + age * 0.03, (h(seed + i + 6) - 0.5) * size * 2).subtract(m.way().scale(age * 0.3));
					SpellRender.billboard(dust, pose, facing, (float) p.x, (float) p.y, (float) p.z, size * (0.8f + t * 1.5f), i, tint, 0.55f * (1 - t), 0, 0, 1, 1,
							SpellRender.FULL_BRIGHT);
				}
			}
			case ICE_BURST -> {
				VertexConsumer ice = buffers.getBuffer(SpellRender.clear(SpellRender.ICE));
				for (int i = 0; i < 14; i++) {
					Vec3 p = flung(dir(seed + i, false).scale(0.22 * size), age, 0.03f);
					pose.pushPose();
					pose.translate(p.x, p.y, p.z);
					pose.mulPose(Axis.XP.rotation(age * 0.4f + i));
					pose.mulPose(Axis.YP.rotation(i));
					List<Vec3> shard = List.of(new Vec3(0, -0.15, 0), new Vec3(0, 0, 0), new Vec3(0, 0.2, 0));
					SpellRender.tube(ice, pose.last(), shard, new float[]{0, 0.07f, 0}, 4, WHITE, 0.85f * (1 - t * 0.5f), 1, 0, SpellRender.FULL_BRIGHT, null);
					pose.popPose();
				}
				VertexConsumer mist = buffers.getBuffer(SpellRender.clear(SpellRender.MIST));
				SpellRender.billboard(mist, pose, facing, 0, 0, 0, size * 2 * (1 + t), age * 0.02f, ICE, 0.6f * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				VertexConsumer glints = buffers.getBuffer(SpellRender.glow(SpellRender.STAR));
				for (int i = 0; i < 6; i++) {
					Vec3 p = flung(dir(seed + 30 + i, false).scale(0.18 * size), age, 0.02f);
					float tw = Mth.sin(age * 0.6f + i * 2);
					if (tw > 0.3f) SpellRender.billboard(glints, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.45f, i, WHITE, (tw - 0.3f) * (1 - t), 0, 0, 1, 1,
							SpellRender.FULL_BRIGHT);
				}
			}
			case RIPPLE -> {
				VertexConsumer ring = buffers.getBuffer(SpellRender.clear(SpellRender.RIPPLE));
				SpellRender.disc(ring, pose.last(), size * (0.3f + t * 1.6f), 0.03f, 0, WHITE, 0.8f * (1 - t), SpellRender.FULL_BRIGHT);
			}
			case CAST_SEAL -> castSeal(m, pose, buffers, facing, age, t);
			case WATER_COLUMN -> waterColumn(m, pose, buffers, facing, eye, age, t, size);
			case RIFT_TEAR -> {
				float open = SpellRender.easeOut(Math.min(1, t * 2));
				pose.pushPose();
				pose.mulPose(Axis.YP.rotationDegrees((float) -m.way().x));
				VertexConsumer edge = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
				for (int i = 0; i < 12; i++) {
					float y = (i / 11f - 0.5f) * 3 * size * open;
					float jag = (h(seed + i) - 0.5f) * 0.6f * size;
					SpellRender.billboard(edge, pose, facing, jag, y + 1.5f, 0, 0.5f * size, 0, new float[]{0.85f, 0.7f, 1.0f}, 1 - t, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
				}
				pose.popPose();
				glow(buffers, pose, facing, new Vec3(0, 1.5, 0), 4 * size * open, new float[]{0.6f, 0.4f, 1.0f}, (1 - t) * 0.6f);
			}
		}
	}

	/**
	 * An element spell's casting seal (HeavenSpells#castSeal): the element's seal (its character within the trigrams and the star of
	 * the five) blooming out under the caster and turning, the plain rune turning the other way inside it, a ring of light at its rim
	 * and motes rising off it; from Core Formation a faint pillar of light over it as well, larger and brighter by the realm. A spell
	 * cast at something has a smaller seal upright before the caster's chest, facing the way, flaring as the spell goes.
	 */
	private static void castSeal(Moment m, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, float age, float t) {
		int code = (int) m.b();
		com.example.defyingtheheavens.QiElement element = com.example.defyingtheheavens.QiElement.values()[Math.floorMod(code % 16, 6)];
		int realm = code / 16;
		float radius = m.a() <= 0 ? 1.5f : m.a();
		float[] light = SpellRender.light(element);
		float bloom = SpellRender.easeOut(Math.min(1, age / 7f));
		float fade = Mth.clamp((m.life() - age) / 14f, 0, 1);
		float alpha = fade * (0.75f + 0.25f * Mth.sin(age * 0.6f));
		float spin = age * (0.045f + 0.01f * realm);
		pose.pushPose();
		pose.translate(0, 0.06f, 0);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.seal(element))), pose.last(), radius * bloom, 0, spin, light, alpha, SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RUNE)), pose.last(), radius * 0.55f * bloom, 0.01f, -spin * 1.6f, light, alpha * 0.7f,
				SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), radius * 1.12f * bloom, 0.02f, spin, light, alpha * 0.6f, SpellRender.FULL_BRIGHT);
		pose.popPose();
		// Motes rising off its rim, more of them for a deeper realm.
		VertexConsumer motes = buffers.getBuffer(SpellRender.glow(SpellRender.STAR));
		int count = 10 + realm * 4;
		for (int i = 0; i < count; i++) {
			float a = i * Mth.TWO_PI / count + spin * 0.6f;
			float rise = ((age * 0.045f + h(m.seed() + i)) % 1f);
			float r = radius * bloom * (0.92f + 0.12f * h(m.seed() + i + 99));
			float size = (0.16f + 0.05f * realm) * (1 - rise * 0.6f);
			SpellRender.billboard(motes, pose, facing, Mth.cos(a) * r, 0.1f + rise * radius * 0.9f, Mth.sin(a) * r, size, a, light, alpha * (1 - rise), 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
		// From Core Formation, a pillar of the element's light over the seal.
		if (realm >= 2) {
			float tall = radius * (0.6f + 0.25f * realm) * bloom;
			SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), tall, h0 -> radius * bloom * (1 - 0.15f * h0), 1, 32, light,
					alpha * (0.10f + 0.03f * realm), 1, 1, 0, 0, 0, SpellRender.FULL_BRIGHT, h0 -> 1 - h0);
		}
		// Cast at something: the hand's seal, upright before the chest, facing the way, flaring as the spell leaves it. One's own, seen
		// through one's own eyes, is smaller, fainter and lower and soon gone, so it frames the view rather than hiding what was cast at.
		Vec3 way = m.way();
		if (way.lengthSqr() > 1.0e-4) {
			boolean eyes = m.own() && Minecraft.getInstance().options.getCameraType().isFirstPerson();
			Vec3 dir = way.normalize();
			float yaw = (float) Math.atan2(dir.x, dir.z), pitch = (float) Math.asin(Mth.clamp(dir.y, -1, 1));
			float flare = 1 + 0.35f * Math.max(0, 1 - age / 6f);
			double reach = eyes ? 1.5 : 1.1, height = eyes ? 0.95 : 1.35;
			float shown = eyes ? alpha * 0.45f * Mth.clamp(1 - (age - 4) / 10f, 0, 1) : alpha;
			Vec3 centre = new Vec3(dir.x * reach, height + dir.y * reach, dir.z * reach);
			pose.pushPose();
			pose.translate(centre.x, centre.y, centre.z);
			pose.mulPose(Axis.YP.rotation(yaw));
			pose.mulPose(Axis.XP.rotation(-pitch + (float) Math.PI / 2));
			float handRadius = (0.45f + 0.18f * realm) * bloom * flare * (eyes ? 0.45f : 1);
			SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.seal(element))), pose.last(), handRadius, 0, -spin * 2, light, shown, SpellRender.FULL_BRIGHT);
			SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.RING)), pose.last(), handRadius * 1.15f, 0.01f, spin, light, shown * 0.5f, SpellRender.FULL_BRIGHT);
			pose.popPose();
			glow(buffers, pose, facing, centre, handRadius * 1.6f, light, shown * 0.35f * flare);
		}
	}

	/** Embers flung out and floating up, fading. */
	private static void embers(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, long seed, float age, float t, float size, int count, float[] rgb) {
		VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int i = 0; i < count; i++) {
			Vec3 p = flung(dir(seed + 100 + i, false).scale(0.3 * size), age, -0.006f);
			SpellRender.billboard(spark, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.2f * Math.max(0.6f, size), 0, rgb, 1 - t, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** Leaves flung out, fluttering down. */
	private static void leaves(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, long seed, float age, float t, float size, int count) {
		VertexConsumer leaf = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
		for (int i = 0; i < count; i++) {
			Vec3 v = dir(seed + 200 + i, true).scale(0.15 * size);
			Vec3 p = flung(v, age, 0.006f).add(Mth.sin(age * 0.3f + i) * 0.2, 0, Mth.cos(age * 0.25f + i) * 0.2);
			if (t > 0.85f) continue;
			SpellRender.billboard(leaf, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.22f, age * 0.2f + i, WHITE, 1, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * A great blast of fire (a bomb, a meteor, a phoenix) or of a golden core: the flash, a ball of fire swelling and boiling out,
	 * tongues of flame and embers flung far, a ring running over the ground, smoke rising after; a core's is gold and white, vast.
	 */
	private static void explosion(Moment m, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 eye, float age, float t, float size, long seed, boolean core) {
		float[] hot = core ? new float[]{1.0f, 0.97f, 0.85f} : FIRE_CORE, mid = core ? GOLD : FIRE_MID, outer = core ? new float[]{1.0f, 0.6f, 0.2f} : FIRE_OUTER;
		float grow = SpellRender.easeOut(Math.min(1, t * 2.5f));
		float fade = 1 - Math.max(0, (t - 0.3f) / 0.7f);
		if (t < 0.15f) glow(buffers, pose, facing, Vec3.ZERO, size * 8, WHITE, 1 - t / 0.15f);
		glow(buffers, pose, facing, Vec3.ZERO, size * 3.2f * grow, outer, 0.6f * fade);
		glow(buffers, pose, facing, Vec3.ZERO, size * 2.2f * grow, mid, 0.85f * fade);
		glow(buffers, pose, facing, Vec3.ZERO, size * 1.2f * grow, hot, fade);
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		int n = core ? 40 : 24;
		for (int i = 0; i < n; i++) {
			Vec3 p = dir(seed + i, false).scale(size * grow * (0.6 + 0.4 * h(seed + i + 300)));
			SpellRender.flameTongue(flames, pose, eye, (float) p.x, (float) p.y - size * 0.2f, (float) p.z, size * 0.7f * fade, size * 1.1f * fade, age + i * 3,
					mid, fade, SpellRender.FLAME_FRAMES);
		}
		embers(buffers, pose, facing, seed, age, t, size * 0.8f, core ? 40 : 20, mid);
		VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
		SpellRender.disc(ring, pose.last(), size * 2.2f * SpellRender.easeOut(t), -size * 0.3f + 0.2f, 0, mid, 0.7f * (1 - t), SpellRender.FULL_BRIGHT);
		float t2 = Math.max(0, t - 0.15f) / 0.85f;
		if (t2 > 0) SpellRender.disc(ring, pose.last(), size * 3.4f * SpellRender.easeOut(t2), -size * 0.3f + 0.25f, 0, outer, 0.45f * (1 - t2), SpellRender.FULL_BRIGHT);
		if (core) {
			// A core's: a column of gold light standing up out of it into the sky, and the fire's and metal's seals blazing under it.
			float column = SpellRender.easeOut(Math.min(1, t * 4)) * fade;
			SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), size * 9 * column, h0 -> Math.min(5, size * 0.18f) * (1 - 0.4f * h0), 4,
					16, hot, 0.45f * column, 1, 1, 0, 0, 0, SpellRender.FULL_BRIGHT, h0 -> 1 - h0);
			SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.SEAL_METAL)), pose.last(), size * 1.8f * grow, -size * 0.3f + 0.3f, age * 0.05f, GOLD,
					0.8f * fade, SpellRender.FULL_BRIGHT);
			SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.SEAL_FIRE)), pose.last(), size * 1.1f * grow, -size * 0.3f + 0.32f, -age * 0.07f, hot,
					0.7f * fade, SpellRender.FULL_BRIGHT);
		}
		VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		for (int i = 0; i < (core ? 18 : 10); i++) {
			Vec3 p = dir(seed + 500 + i, true).scale(size * (0.4 + t)).add(0, age * (core ? 0.12 : 0.05), 0);
			SpellRender.billboard(smoke, pose, facing, (float) p.x, (float) p.y, (float) p.z, size * (0.8f + t * 1.6f), i + age * 0.01f, new float[]{0.18f, 0.16f, 0.15f},
					0.6f * Math.min(1, t * 3) * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/** The colour a block's dust takes (its map colour), as {r, g, b}. */
	private static float[] tintOf(int stateId) {
		int c = Block.stateById(stateId).getBlock().defaultMapColor().col;
		float[] rgb = SpellRender.rgb(c);
		return new float[]{0.4f + rgb[0] * 0.8f, 0.4f + rgb[1] * 0.8f, 0.4f + rgb[2] * 0.8f};
	}

	private ClientSpellFx() {}
}
