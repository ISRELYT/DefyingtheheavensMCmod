package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.SwordFlight;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
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
 * Sword Flight on the client ({@link SwordFlight}): who is riding (players from the server's packets, NPCs from their synced
 * flags), the sword drawn flat under the rider's feet with its point the way they face, and the trail of light it leaves:
 * a glowing azure ribbon along the path of the last {@link #TRAIL_TICKS} ticks, with a few lingering glints. The rider's
 * sideways stance is in {@link SwordStance}; the sword is hidden from their hand while they ride.
 */
public final class SwordFlightClient {
	/** A player's ride is forgotten if the server's once-a-second word stops (out of range, gone) for this long. */
	private static final long EXPIRY_MS = 2500;
	private static final double DRAW_RANGE = 128;
	/** The sword item is a block wide; scaled up so its blade runs about two blocks under the rider. */
	private static final float SWORD_SCALE = 1.6f;
	/** The blade's middle this far below the soles, so they stand on its flat. */
	private static final double SWORD_DROP = 0.07;
	/** How long (in ticks) the trail behind a rider lasts. */
	private static final int TRAIL_TICKS = 14;
	/** The trail's widest, just behind the sword. */
	private static final double TRAIL_WIDTH = 0.5;
	private static final float[] TRAIL_RGB = {0.45f, 0.82f, 1.0f};

	/** Riding players by entity id, with when the server last said so. */
	private static final Map<Integer, Long> RIDERS = new ConcurrentHashMap<>();
	/** Recent trail points (the sword's middle, once a tick), newest first, by entity id. */
	private static final Map<Integer, Deque<Point>> TRAILS = new HashMap<>();

	private record Point(Vec3 at, long tick) {}

	/** The server's word about a player (entity id). */
	public static void set(int entityId, boolean riding) {
		if (riding) {
			RIDERS.put(entityId, System.currentTimeMillis());
		} else {
			RIDERS.remove(entityId);
		}
	}

	/** Whether this entity is riding its sword right now. */
	public static boolean isRiding(Entity entity) {
		if (entity instanceof CultivatorNpc npc) return npc.isSwordFlying() && npc.isAlive();
		if (!(entity instanceof Player player) || !player.isAlive()) return false;
		Long last = RIDERS.get(player.getId());
		if (last == null) return false;
		if (System.currentTimeMillis() - last > EXPIRY_MS) {
			RIDERS.remove(player.getId());
			return false;
		}
		return true;
	}

	/**
	 * The way a rider and its sword point: a player where they look (they fly that way, while vanilla lets their body lag
	 * up to 75 degrees behind), an NPC where its body faces (it turns its body along its course and looks about freely).
	 */
	public static float riderYaw(LivingEntity rider, float partialTick) {
		return rider instanceof Player ? Mth.rotLerp(partialTick, rider.yHeadRotO, rider.yHeadRot)
				: Mth.rotLerp(partialTick, rider.yBodyRotO, rider.yBodyRot);
	}

	/** How far the rider's body (as the renderer turns it) is from {@link #riderYaw}, in degrees. */
	public static float bodyOffset(LivingEntity rider, float partialTick) {
		return Mth.wrapDegrees(Mth.rotLerp(partialTick, rider.yBodyRotO, rider.yBodyRot) - riderYaw(rider, partialTick));
	}

	/** Where the sword's middle is: under the entity's feet. */
	private static Vec3 swordAt(Entity entity, float partialTick) {
		return entity.getPosition(partialTick).add(0, -SWORD_DROP, 0);
	}

	/** Each client tick: lays down the next trail point for every rider, lets old points fade, and sheds a glint now and then. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.isPaused()) return;
		long now = mc.level.getGameTime();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!isRiding(entity)) continue;
			Vec3 at = swordAt(entity, 1.0f);
			Deque<Point> trail = TRAILS.computeIfAbsent(entity.getId(), id -> new ArrayDeque<>());
			Point newest = trail.peekFirst();
			if (newest == null || newest.at().distanceToSqr(at) > 1.0e-4) trail.addFirst(new Point(at, now));
			// Glints of sword light, left hanging in the air where it passed.
			if (newest != null && newest.at().distanceToSqr(at) > 0.04 && mc.level.random.nextFloat() < 0.4f) {
				Vec3 glint = newest.at().lerp(at, mc.level.random.nextDouble());
				mc.level.addParticle(ParticleTypes.END_ROD, glint.x + (mc.level.random.nextDouble() - 0.5) * 0.3, glint.y,
						glint.z + (mc.level.random.nextDouble() - 0.5) * 0.3, 0, -0.01, 0);
			}
		}
		Iterator<Map.Entry<Integer, Deque<Point>>> it = TRAILS.entrySet().iterator();
		while (it.hasNext()) {
			Deque<Point> trail = it.next().getValue();
			while (!trail.isEmpty() && now - trail.peekLast().tick() > TRAIL_TICKS) trail.removeLast();
			if (trail.isEmpty()) it.remove();
		}
	}

	/** WorldRenderEvents.AFTER_ENTITIES: each rider's own sword, lying flat under their feet, point first. */
	public static void renderSwords(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Vec3 cam = context.camera().getPosition();
		float partial = context.tickDelta();
		PoseStack pose = context.matrixStack();
		MultiBufferSource.BufferSource own = context.consumers() == null ? mc.renderBuffers().bufferSource() : null;
		MultiBufferSource buffers = own != null ? own : context.consumers();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity rider) || rider.isInvisible() || !isRiding(rider)) continue;
			ItemStack sword = rider.getMainHandItem();
			if (!SwordFlight.isSword(sword)) continue;
			Vec3 at = swordAt(rider, partial);
			if (at.distanceToSqr(cam) > DRAW_RANGE * DRAW_RANGE) continue;
			float yaw = riderYaw(rider, partial);
			pose.pushPose();
			pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
			// +Z turned to face the rider's way; the sprite laid flat; its diagonal blade (point at the top right) along +Z.
			pose.mulPose(Axis.YP.rotationDegrees(-yaw));
			pose.mulPose(Axis.XP.rotationDegrees(90.0f));
			pose.mulPose(Axis.ZP.rotationDegrees(45.0f));
			pose.scale(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE);
			int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at.x, at.y + 0.5, at.z));
			mc.getItemRenderer().renderStatic(sword, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers,
					mc.level, rider.getId());
			pose.popPose();
		}
		if (own != null) own.endBatch();
	}

	/**
	 * WorldRenderEvents.AFTER_TRANSLUCENT: the trails, as ribbons turned to face the camera, glowing (added light). Each
	 * runs from the sword (thin) out to its widest just behind it, then narrows and fades with age; a bright core runs
	 * down the middle.
	 */
	public static void renderTrails(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || TRAILS.isEmpty()) return;
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
			List<Vec3> points = new ArrayList<>();
			List<Float> ages = new ArrayList<>();
			// The head follows the sword between ticks, so the ribbon stays joined to it.
			Entity entity = mc.level.getEntity(entry.getKey());
			if (entity != null && isRiding(entity)) {
				points.add(swordAt(entity, partial));
				ages.add(0f);
			}
			for (Point point : entry.getValue()) {
				points.add(point.at());
				ages.add(Math.max(0, now - point.tick()));
			}
			if (points.size() < 2 || points.get(0).distanceToSqr(cam) > DRAW_RANGE * DRAW_RANGE) continue;
			ribbon(buffer, matrix, cam, points, ages, 1.0f, 0.55f);
			ribbon(buffer, matrix, cam, points, ages, 0.3f, 0.9f); // the bright core
		}
		BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
		if (rendered != null) BufferUploader.drawWithShader(rendered);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** One camera-facing ribbon through the points, {@code widthShare} of the trail's width, at {@code brightness}. */
	private static void ribbon(BufferBuilder buffer, Matrix4f matrix, Vec3 cam, List<Vec3> points, List<Float> ages,
							   float widthShare, float brightness) {
		int n = points.size();
		Vec3[] left = new Vec3[n];
		Vec3[] right = new Vec3[n];
		float[] alpha = new float[n];
		for (int i = 0; i < n; i++) {
			Vec3 p = points.get(i);
			Vec3 along = points.get(Math.max(0, i - 1)).subtract(points.get(Math.min(n - 1, i + 1)));
			Vec3 side = along.cross(p.subtract(cam));
			if (side.lengthSqr() < 1.0e-8) side = new Vec3(0, 1, 0);
			float life = 1.0f - Math.min(1.0f, ages.get(i) / TRAIL_TICKS);
			// Thin where it leaves the sword, full a couple of points back, then narrowing as it fades.
			double width = TRAIL_WIDTH * widthShare * life * Math.min(1.0, (i + 0.35) / 2.0);
			side = side.normalize().scale(width / 2);
			left[i] = p.add(side).subtract(cam);
			right[i] = p.subtract(side).subtract(cam);
			alpha[i] = life * life * brightness;
		}
		for (int i = 0; i < n - 1; i++) {
			vertex(buffer, matrix, left[i], alpha[i]);
			vertex(buffer, matrix, right[i], alpha[i]);
			vertex(buffer, matrix, right[i + 1], alpha[i + 1]);
			vertex(buffer, matrix, left[i], alpha[i]);
			vertex(buffer, matrix, right[i + 1], alpha[i + 1]);
			vertex(buffer, matrix, left[i + 1], alpha[i + 1]);
		}
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 at, float alpha) {
		buffer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z).color(TRAIL_RGB[0], TRAIL_RGB[1], TRAIL_RGB[2], alpha).endVertex();
	}

	public static void clear() {
		RIDERS.clear();
		TRAILS.clear();
	}

	private SwordFlightClient() {}
}
