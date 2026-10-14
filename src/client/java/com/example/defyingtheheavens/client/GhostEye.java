package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.GhostMethod;
import com.example.defyingtheheavens.MethodSteleBlock;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModDimensions;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghost Eye (Netherworld Breathing Art, third layer): within {@link #RANGE} blocks the dead and the Method Steles glow through
 * walls, drawn into the sense layer (SenseOverlay) only where something stands in front of them, so what is in plain sight
 * isn't drawn twice. Outside the Netherworld it also shows projected nascent souls, as a Nascent Soul cultivator with Qi
 * Sense open sees them, clearer with each layer of the art ({@link #soulClarity}; the server sends them, SoulProjection#perceives).
 */
public final class GhostEye {
	public static final double RANGE = 32;
	/** The steles are looked for this far above and below, every so many ticks. */
	private static final int SCAN_HEIGHT = 16, SCAN_TICKS = 40;
	/** The steles' glow: cyan, this strong at its faces. */
	private static final float[] CYAN = {0.35f, 0.95f, 1.0f};
	private static final List<BlockPos> STELES = new ArrayList<>();
	private static int scanIn;

	public static boolean isActive() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && !mc.player.isSpectator() && ClientCultivationData.get().isAbilityActive(Ability.GHOST_EYE);
	}

	/**
	 * How clearly the Ghost Eye shows projected souls outside the Netherworld: a haze at its third layer (as a Nascent Soul
	 * Early's Qi Sense), whole at the ninth; 0 when it's off, or in the Netherworld.
	 */
	public static float soulClarity() {
		Minecraft mc = Minecraft.getInstance();
		if (!isActive() || mc.level == null || ModDimensions.isNetherworld(mc.level.dimension())) return 0;
		int layer = ClientCultivationData.get().netherworld().layer(GhostMethod.NETHERWORLD_BREATHING);
		return Mth.clamp(0.25f + 0.75f * (layer - 3) / 6.0f, 0.25f, 1.0f);
	}

	static void tick(Minecraft mc) {
		if (!isActive() || mc.level == null) {
			STELES.clear();
			scanIn = 0;
			ghostlyNear = false;
			return;
		}
		ghostlyNear = !mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, mc.player.getBoundingBox().inflate(RANGE),
				e -> e instanceof com.example.defyingtheheavens.Ghostly).isEmpty();
		if (--scanIn > 0) return;
		scanIn = SCAN_TICKS;
		STELES.clear();
		BlockPos at = mc.player.blockPosition();
		int r = (int) RANGE;
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -SCAN_HEIGHT, -r), at.offset(r, SCAN_HEIGHT, r))) {
			BlockState state = mc.level.getBlockState(pos);
			if (state.is(ModBlocks.METHOD_STELE) && state.getValue(MethodSteleBlock.PART) == MethodSteleBlock.Part.MIDDLE
					&& pos.distToCenterSqr(mc.player.position()) <= RANGE * RANGE) {
				STELES.add(pos.immutable());
			}
		}
	}

	/** Anything for the Ghost Eye to show this frame. */
	static boolean hasWork() {
		return isActive() && (!STELES.isEmpty() || ClientGhosts.any() || ghostlyNear);
	}

	/** Whether any of the Netherworld's own dead (ghost cultivators, Hungry Ghosts) were within reach at the last scan. */
	private static boolean ghostlyNear;

	/** Into the sense layer (its blending set, its depth the world's, see SenseOverlay): what walls hide, and only that. */
	static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		float partial = context.tickDelta();
		RenderSystem.depthFunc(GL11.GL_GREATER); // only where the world is nearer than what's drawn: behind walls
		for (Player ghost : mc.level.players()) {
			// The dead, and the living in a Ghost King's form: whatever is drawn as the dead's light.
			if (ghost == mc.player || !ClientGhosts.drawnAsLight(ghost) || ghost.isInvisible() || ghost.distanceToSqr(mc.player) > RANGE * RANGE) continue;
			SoulRenderer.drawGhostSeen(context, ghost, partial);
		}
		if (ghostlyNear) {
			for (net.minecraft.world.entity.LivingEntity dead : mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					mc.player.getBoundingBox().inflate(RANGE), e -> e instanceof com.example.defyingtheheavens.Ghostly && !e.isInvisible())) {
				SoulRenderer.drawDead(context, dead, partial);
			}
		}
		if (!STELES.isEmpty()) {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			Vec3 cam = context.camera().getPosition();
			Matrix4f m = context.matrixStack().last().pose();
			float pulse = 0.8f + 0.2f * Mth.sin((mc.player.tickCount + partial) * 0.08f);
			BufferBuilder b = Tesselator.getInstance().getBuilder();
			b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			for (BlockPos pos : STELES) {
				// The whole stone, the middle block and the one above and below: a pillar of cold light. Its faces clear of the stone's
				// own (2/16 in, 14/16 tall at the top) and no floor to it: a face the depth test meets at the stone's or the ground's
				// own depth flickers (the light is drawn only where the world stands nearer, GL_GREATER).
				float x0 = (float) (pos.getX() + 0.08 - cam.x), x1 = (float) (pos.getX() + 0.92 - cam.x);
				float y0 = (float) (pos.getY() - 1 - cam.y), y1 = (float) (pos.getY() + 1.95 - cam.y);
				float z0 = (float) (pos.getZ() + 0.08 - cam.z), z1 = (float) (pos.getZ() + 0.92 - cam.z);
				box(b, m, x0, y0, z0, x1, y1, z1, 0.42f * pulse);
				// A fainter haze around it, so a far stele still catches the eye.
				float h = 0.3f;
				box(b, m, x0 - h, y0, z0 - h, x1 + h, y1 + h, z1 + h, 0.14f * pulse); // not below its foot, into the ground
			}
			BufferUploader.drawWithShader(b.end());
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		}
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
	}

	/**
	 * A box's four sides and top, brighter at the bottom and fading toward the top. No bottom: it lies on the ground, where it
	 * would flicker against the ground's own surface.
	 */
	private static void box(BufferBuilder b, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float alpha) {
		float top = alpha * 0.35f;
		quad(b, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, alpha, top);
		quad(b, m, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, alpha, top);
		quad(b, m, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, alpha, top);
		quad(b, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, alpha, top);
		quad(b, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, top, top);
	}

	/** A quad whose first two corners are {@code low} and last two {@code high} in alpha. */
	private static void quad(BufferBuilder b, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
							 float dx, float dy, float dz, float low, float high) {
		b.vertex(m, ax, ay, az).color(CYAN[0], CYAN[1], CYAN[2], low).endVertex();
		b.vertex(m, bx, by, bz).color(CYAN[0], CYAN[1], CYAN[2], low).endVertex();
		b.vertex(m, cx, cy, cz).color(CYAN[0], CYAN[1], CYAN[2], high).endVertex();
		b.vertex(m, dx, dy, dz).color(CYAN[0], CYAN[1], CYAN[2], high).endVertex();
	}

	static void clear() {
		STELES.clear();
		scanIn = 0;
		ghostlyNear = false;
	}

	private GhostEye() {}
}
