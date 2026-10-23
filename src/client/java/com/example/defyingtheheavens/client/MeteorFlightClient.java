package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.MeteorFlight;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Meteor Flight on the client ({@link MeteorFlight}): the local flier steered where they look while forward is held (from
 * MeteorFlightMixin, in place of Player#travel), at cruise or, sprinting, up to their realm's top speed, held back where the land
 * ahead hasn't come yet; the word to the server whether forward is held; who flies fast and how hot they are, from the server's
 * word (oneself too); the body laid out along the way it goes, a fist out ahead ({@link #layOut}, {@link #pose}); and the sights:
 * a trail of vapour behind one flying fast, and a ring of air where the sound barrier broke. The heat is on the body itself, glowing
 * gold ablaze and red as it cools (SpellAuraLayer#meteorHeat): no flames or glow thrown about it (the user's word, 2026-10-23).
 */
public final class MeteorFlightClient {
	/** The top speed is reached from a standstill in this many ticks; a lower aim is come down to twice as fast. */
	private static final double ACCEL_TICKS = 30, BRAKE_TICKS = 15;
	/** The way flown turns this share of the way to the look each tick: a little weight at speed. */
	private static final double TURN = 0.45;
	/**
	 * The land must have come this many ticks' flight ahead (looked for every {@link #CHUNK_STEP} blocks)... Few: a server sends land
	 * only so far (its view distance, some 128 blocks), so a long reach would hold back even a flight over land long made (run 1 of
	 * 2026-10-23: 30 ticks kept a True Immortal to a third of its speed).
	 */
	private static final int AHEAD_TICKS = 8;
	private static final double CHUNK_STEP = 8;
	/** ...or the flier is held back to reach its edge no sooner, and creeps at least this fast (blocks a tick). */
	private static final double EDGE_SPEED = 0.25;
	/** Shown laid out only faster than this (blocks a tick): not while just setting off. */
	private static final double LAID_OUT_FROM = 0.3;
	/** The server's word about another player is forgotten after this long without another. */
	private static final long EXPIRY_MS = 1500;
	/** How far off the sights are drawn. */
	private static final double DRAW_RANGE = 192;
	private static final int TRAIL_TICKS = 16, RING_TICKS = 16;
	private static final float[] VAPOUR = {0.85f, 0.92f, 1.0f}, RING = {0.92f, 0.96f, 1.0f};

	// --- The local flier ---

	/** Blocks a tick along the way flown, and that way. */
	private static double speed;
	private static Vec3 heading;
	/** Forward held in Meteor Flight this tick; and what the server was last told. */
	private static boolean steering, told;

	/** Whether the local player flies by Meteor Flight now (as MeteorFlight#mayStreak on the server). */
	public static boolean active(LocalPlayer p) {
		PlayerCultivation c = ClientCultivationData.get();
		return c.isAbilityActive(Ability.METEOR_FLIGHT) && p.getAbilities().flying && !p.isCreative() && !p.isSpectator() && !p.isPassenger()
				&& !SwordFlightClient.isRiding(p) && !ClientSoul.isOut() && MeteorFlight.flownIn(p.level().dimension());
	}

	/**
	 * The local player's flight this tick (MeteorFlightMixin, at the head of Player#travel): with forward held in Meteor Flight,
	 * along the line of sight, free of gravity, sped up toward cruise or (sprinting) the top speed and held back where the land
	 * ahead hasn't come; let go, vanilla's flight takes it from there, the momentum and all. @return whether it flew here
	 */
	public static boolean travel(LocalPlayer player) {
		steering = active(player) && player.input.forwardImpulse > 0;
		if (!steering) {
			speed = player.getDeltaMovement().length();
			heading = null;
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		PlayerCultivation c = ClientCultivationData.get();
		double top = MeteorFlight.topPerTick(c);
		double aim = mc.options.keySprint.isDown() ? top : MeteorFlight.cruisePerTick(c);
		Vec3 look = player.getViewVector(1.0f);
		Vec3 turned = heading == null ? look : heading.lerp(look, TURN);
		heading = turned.lengthSqr() < 1.0e-6 ? look : turned.normalize();
		double land = landAhead(player, heading, aim);
		aim = Math.min(aim, land);
		speed = speed < aim ? Math.min(aim, speed + top / ACCEL_TICKS) : Math.max(aim, speed - top / BRAKE_TICKS);
		speed = Math.min(speed, land); // never on into land that hasn't come, braking or not
		Vec3 v = heading.scale(speed);
		// Never into the void below the world.
		if (player.getY() + v.y < player.level().getMinBuildHeight() - 2 && v.y < 0) v = new Vec3(v.x, 0, v.z);
		Vec3 before = player.position();
		player.setDeltaMovement(v);
		player.move(MoverType.SELF, v);
		// Stopped short (what it can't go through, or what isn't torn away yet): the speed is what it went.
		double went = player.position().distanceTo(before);
		if (went < speed * 0.9) speed = went;
		player.setDeltaMovement(heading.scale(speed));
		player.fallDistance = 0;
		player.calculateEntityAnimation(false);
		return true;
	}

	/**
	 * The fastest {@code p} may go along {@code dir} (blocks a tick) to reach no sooner than {@link #AHEAD_TICKS} ticks on the
	 * edge of the land this client has, or {@code aim} if it reaches that far.
	 */
	private static double landAhead(LocalPlayer p, Vec3 dir, double aim) {
		double reach = aim * AHEAD_TICKS;
		for (double d = CHUNK_STEP; d <= reach; d += CHUNK_STEP) {
			Vec3 at = p.position().add(dir.scale(d));
			if (!p.level().getChunkSource().hasChunk(SectionPos.blockToSectionCoord(at.x), SectionPos.blockToSectionCoord(at.z))) {
				return Math.max(EDGE_SPEED, (d - CHUNK_STEP) / AHEAD_TICKS);
			}
		}
		return aim;
	}

	// --- Who flies fast, and how hot ---

	private record Seen(boolean streaking, float heat, long at) {}

	private static final Map<Integer, Seen> SEEN = new ConcurrentHashMap<>();
	/** Heat as drawn, by entity id, eased toward the server's word. */
	private static final Map<Integer, Float> SHOWN = new HashMap<>();

	/** The server's word (S2C meteor_state) about a player: flying fast or not, and how hot. */
	public static void set(int entityId, boolean streaking, float heat) {
		SEEN.put(entityId, new Seen(streaking, heat, System.currentTimeMillis()));
	}

	/** Whether {@code entity} flies fast now: the local player by its own steering, others by the server's word. */
	public static boolean isStreaking(Entity entity) {
		if (!(entity instanceof Player player) || !player.isAlive()) return false;
		if (player == Minecraft.getInstance().player) return steering && speed > LAID_OUT_FROM;
		Seen seen = SEEN.get(player.getId());
		return seen != null && seen.streaking() && System.currentTimeMillis() - seen.at() <= EXPIRY_MS;
	}

	/** How hot {@code entity} looks, 0 to 1. */
	public static float heat(Entity entity) {
		Float shown = SHOWN.get(entity.getId());
		return shown == null ? 0 : shown;
	}

	// --- The body laid out ---

	/**
	 * (LivingEntityRendererMixin, at the tail of setupRotations.) The body turned to the way the flier looks, then laid along it,
	 * head first, about its own middle (so it stays where its box is): flat flying level, head down diving, upright climbing.
	 */
	public static void layOut(LivingEntity entity, PoseStack pose, float partial) {
		float pitch = Mth.lerp(partial, entity.xRotO, entity.getXRot());
		float half = entity.getBbHeight() * 0.5f;
		// By the look's yaw (sent with every move), not the head's: vanilla tells one who starts watching a player its head only once
		// it turns, so a flier holding its course was seen lying the wrong way (2026-10-23, run 3).
		float yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
		float body = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
		pose.mulPose(Axis.YP.rotationDegrees(Mth.wrapDegrees(body - yaw)));
		pose.translate(0, half, 0);
		pose.mulPose(Axis.XP.rotationDegrees(-90.0f - pitch));
		pose.translate(0, -half, 0);
	}

	/** (HumanoidModelMixin.) Laid out: the head raised to look ahead, the right fist out in front, the left arm and the legs trailing. */
	public static void pose(HumanoidModel<?> m, float ageInTicks) {
		m.head.xRot = -1.05f;
		m.head.yRot = 0;
		m.head.zRot = 0;
		m.hat.copyFrom(m.head);
		m.body.xRot = 0;
		m.rightArm.xRot = -(float) Math.PI + 0.12f;
		m.rightArm.yRot = 0;
		m.rightArm.zRot = 0.1f;
		m.leftArm.xRot = 0.15f;
		m.leftArm.yRot = 0;
		m.leftArm.zRot = -0.1f;
		float flutter = Mth.sin(ageInTicks * 0.6f) * 0.06f;
		m.rightLeg.xRot = 0.05f + flutter;
		m.leftLeg.xRot = 0.05f - flutter;
		m.rightLeg.yRot = 0;
		m.leftLeg.yRot = 0;
		m.rightLeg.zRot = 0.03f;
		m.leftLeg.zRot = -0.03f;
	}

	// --- Sights ---

	/** A point of a trail: where the body's middle was, and when. */
	private record Point(Vec3 at, long tick) {}

	private static final Map<Integer, Deque<Point>> TRAILS = new HashMap<>();
	/** Where each player's middle was last tick (for how fast others go). */
	private static final Map<Integer, Vec3> LAST = new HashMap<>();

	private record Boom(Vec3 at, Vec3 dir, long born) {}

	private static final List<Boom> BOOMS = new ArrayList<>();

	/** The server's word (S2C meteor_boom): the sound barrier broke at {@code at}, going {@code dir}. */
	public static void boom(Vec3 at, Vec3 dir) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		BOOMS.add(new Boom(at, dir, mc.level.getGameTime()));
		// A burst of air round the way it went.
		Vec3[] basis = basis(dir);
		for (int i = 0; i < 20; i++) {
			double a = i * Math.PI * 2 / 20;
			Vec3 out = basis[0].scale(Math.cos(a)).add(basis[1].scale(Math.sin(a)));
			mc.level.addParticle(ParticleTypes.CLOUD, at.x + out.x * 0.8, at.y + out.y * 0.8, at.z + out.z * 0.8, out.x * 0.45, out.y * 0.45, out.z * 0.45);
		}
	}

	/** Each client tick: the word to the server, the heat eased, the trails laid and let fade, and the rings aged. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		boolean want = steering && active(mc.player);
		if (want != told) {
			told = want;
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeBoolean(want);
			ClientPlayNetworking.send(ModPackets.METEOR_STREAK, buf);
		}
		if (mc.isPaused()) return;
		long now = mc.level.getGameTime();
		long ms = System.currentTimeMillis();
		for (Player player : mc.level.players()) {
			int id = player.getId();
			Seen seen = SEEN.get(id);
			float target = seen != null && ms - seen.at() <= EXPIRY_MS ? seen.heat() : 0;
			float shown = SHOWN.getOrDefault(id, 0f);
			shown += (target - shown) * 0.2f;
			if (target == 0 && shown < 0.005f) SHOWN.remove(id);
			else SHOWN.put(id, shown);
			Vec3 middle = player.position().add(0, player.getBbHeight() * 0.5, 0);
			Vec3 before = LAST.put(id, middle);
			double going = before == null ? 0 : middle.distanceTo(before);
			// A trail of vapour behind one flying faster than Qi Flight ever goes.
			if (isStreaking(player) && going > MeteorFlight.NORMAL_SPRINT && going < 64) {
				TRAILS.computeIfAbsent(id, k -> new ArrayDeque<>()).addFirst(new Point(middle, now));
			}
		}
		for (Iterator<Map.Entry<Integer, Deque<Point>>> it = TRAILS.entrySet().iterator(); it.hasNext(); ) {
			Deque<Point> trail = it.next().getValue();
			while (!trail.isEmpty() && now - trail.peekLast().tick() > TRAIL_TICKS) trail.removeLast();
			if (trail.isEmpty()) it.remove();
		}
		BOOMS.removeIf(b -> now - b.born() > RING_TICKS);
		LAST.keySet().removeIf(id -> mc.level.getEntity(id) == null);
	}

	/** WorldRenderEvents.AFTER_TRANSLUCENT: the trails and the rings, in added light. */
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || TRAILS.isEmpty() && BOOMS.isEmpty()) return;
		Vec3 cam = context.camera().getPosition();
		float partial = context.tickDelta();
		float now = mc.level.getGameTime() + partial;
		Matrix4f matrix = context.matrixStack().last().pose();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Map.Entry<Integer, Deque<Point>> entry : TRAILS.entrySet()) {
			Entity entity = mc.level.getEntity(entry.getKey());
			List<Vec3> points = new ArrayList<>();
			List<Float> ages = new ArrayList<>();
			if (entity != null) {
				points.add(entity.getPosition(partial).add(0, entity.getBbHeight() * 0.5, 0));
				ages.add(0f);
			}
			for (Point point : entry.getValue()) {
				points.add(point.at());
				ages.add(Math.max(0, now - point.tick()));
			}
			if (points.size() < 2 || points.get(0).distanceToSqr(cam) > DRAW_RANGE * DRAW_RANGE) continue;
			ribbon(buffer, matrix, cam, points, ages, 0.9, VAPOUR, 0.3f);
		}
		for (Boom boom : BOOMS) {
			float age = now - boom.born();
			if (age < 0 || age > RING_TICKS || boom.at().distanceToSqr(cam) > DRAW_RANGE * DRAW_RANGE) continue;
			float life = 1 - age / RING_TICKS;
			double radius = 0.8 + age * 0.55;
			ring(buffer, matrix, boom.at().subtract(cam), boom.dir(), radius, 0.5 + age * 0.08, RING, (float) Math.pow(life, 1.5) * 0.7f);
		}
		BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
		if (rendered != null) BufferUploader.drawWithShader(rendered);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** The heat's colour: yellow-white ablaze, orange as it falls, red as the body cools (SpellAuraLayer#meteorHeat). */
	public static float[] heatColour(float heat) {
		if (heat >= 0.75f) return lerp((heat - 0.75f) / 0.25f, new float[] {1.0f, 0.75f, 0.3f}, new float[] {1.0f, 0.95f, 0.65f});
		if (heat >= MeteorFlight.ABLAZE) return lerp((heat - 0.5f) / 0.25f, new float[] {1.0f, 0.45f, 0.12f}, new float[] {1.0f, 0.75f, 0.3f});
		return lerp(heat / 0.5f, new float[] {0.85f, 0.12f, 0.05f}, new float[] {1.0f, 0.45f, 0.12f});
	}

	private static float[] lerp(float t, float[] a, float[] b) {
		return new float[] {Mth.lerp(t, a[0], b[0]), Mth.lerp(t, a[1], b[1]), Mth.lerp(t, a[2], b[2])};
	}

	/** Two directions square to {@code dir} and to each other. */
	private static Vec3[] basis(Vec3 dir) {
		Vec3 u = dir.cross(Math.abs(dir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0));
		u = u.lengthSqr() < 1.0e-8 ? new Vec3(1, 0, 0) : u.normalize();
		return new Vec3[] {u, dir.cross(u).normalize()};
	}

	/** A ring of air square to {@code dir} about {@code at} (camera-relative), {@code width} across, brightest at {@code radius}. */
	private static void ring(BufferBuilder buffer, Matrix4f matrix, Vec3 at, Vec3 dir, double radius, double width, float[] rgb, float alpha) {
		Vec3[] b = basis(dir);
		int sides = 40;
		double[] radii = {radius - width / 2, radius, radius + width / 2};
		float[] alphas = {0, alpha, 0};
		for (int i = 0; i < sides; i++) {
			double a0 = i * Math.PI * 2 / sides, a1 = (i + 1) * Math.PI * 2 / sides;
			Vec3 d0 = b[0].scale(Math.cos(a0)).add(b[1].scale(Math.sin(a0))), d1 = b[0].scale(Math.cos(a1)).add(b[1].scale(Math.sin(a1)));
			for (int band = 0; band < 2; band++) {
				Vec3 in0 = at.add(d0.scale(radii[band])), in1 = at.add(d1.scale(radii[band]));
				Vec3 out0 = at.add(d0.scale(radii[band + 1])), out1 = at.add(d1.scale(radii[band + 1]));
				vertex(buffer, matrix, in0, rgb, alphas[band]);
				vertex(buffer, matrix, out0, rgb, alphas[band + 1]);
				vertex(buffer, matrix, out1, rgb, alphas[band + 1]);
				vertex(buffer, matrix, in0, rgb, alphas[band]);
				vertex(buffer, matrix, out1, rgb, alphas[band + 1]);
				vertex(buffer, matrix, in1, rgb, alphas[band]);
			}
		}
	}

	/** A ribbon through the points turned to the camera, {@code width} at its head, narrowing and fading with age. */
	private static void ribbon(BufferBuilder buffer, Matrix4f matrix, Vec3 cam, List<Vec3> points, List<Float> ages, double width, float[] rgb,
							   float brightness) {
		int n = points.size();
		Vec3[] left = new Vec3[n], right = new Vec3[n];
		float[] alpha = new float[n];
		for (int i = 0; i < n; i++) {
			Vec3 p = points.get(i);
			Vec3 along = points.get(Math.max(0, i - 1)).subtract(points.get(Math.min(n - 1, i + 1)));
			Vec3 side = along.cross(p.subtract(cam));
			if (side.lengthSqr() < 1.0e-8) side = new Vec3(0, 1, 0);
			float life = 1.0f - Math.min(1.0f, ages.get(i) / TRAIL_TICKS);
			side = side.normalize().scale(width * life / 2);
			left[i] = p.add(side).subtract(cam);
			right[i] = p.subtract(side).subtract(cam);
			alpha[i] = life * life * brightness;
		}
		for (int i = 0; i < n - 1; i++) {
			vertex(buffer, matrix, left[i], rgb, alpha[i]);
			vertex(buffer, matrix, right[i], rgb, alpha[i]);
			vertex(buffer, matrix, right[i + 1], rgb, alpha[i + 1]);
			vertex(buffer, matrix, left[i], rgb, alpha[i]);
			vertex(buffer, matrix, right[i + 1], rgb, alpha[i + 1]);
			vertex(buffer, matrix, left[i + 1], rgb, alpha[i + 1]);
		}
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 at, float[] rgb, float alpha) {
		buffer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
	}

	/** Disconnect. */
	public static void clear() {
		SEEN.clear();
		SHOWN.clear();
		TRAILS.clear();
		LAST.clear();
		BOOMS.clear();
		speed = 0;
		heading = null;
		steering = told = false;
	}

	private MeteorFlightClient() {}
}
