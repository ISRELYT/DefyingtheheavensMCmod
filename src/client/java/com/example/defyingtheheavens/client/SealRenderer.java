package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FormationCoreBlock;
import com.example.defyingtheheavens.ModTags;
import com.example.defyingtheheavens.SealBlock;
import com.example.defyingtheheavens.SealBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Seals of qi ink, seen only with Qi Sense: bright azure lines painted along the floor and up the walls, like redstone dust,
 * from each seal toward every seal, Qi Vein and Formation Core it touches, with a soft pulse running along them. Drawn into
 * the Qi Sense layer's glow ({@link QiSenseTreasures#glow}), so they stay vivid in the grey plane of Qi; without Qi Sense
 * nothing is drawn at all.
 */
public class SealRenderer implements BlockEntityRenderer<SealBlockEntity> {
	private static final float HALF_WIDTH = 0.07f;
	private static final float DOT = 0.13f;
	private static final float LIFT = 0.03f;
	private static final float[] INK = {0.30f, 0.72f, 1.0f};

	public SealRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(SealBlockEntity seal, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		Level level = seal.getLevel();
		if (level == null || !QiSenseClientHandler.isActive()) return;
		BlockState state = seal.getBlockState();
		if (!(state.getBlock() instanceof SealBlock)) return;
		BlockPos pos = seal.getBlockPos();
		Direction face = state.getValue(SealBlock.FACE);
		Vec3 centre = centre(face);
		Vec3 normal = Vec3.atLowerCornerOf(face.getOpposite().getNormal());
		float time = level.getGameTime() + partialTick;
		float glow = 0.55f + 0.45f * Mth.sin(time * 0.12f - (pos.getX() + pos.getY() + pos.getZ()) * 0.7f);
		VertexConsumer buffer = QiSenseTreasures.glow(buffers);
		Matrix4f matrix = poseStack.last().pose();

		dot(buffer, matrix, centre, face, glow);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) continue;
					BlockPos other = pos.offset(dx, dy, dz);
					BlockState there = level.getBlockState(other);
					Vec3 target;
					if (there.getBlock() instanceof SealBlock) {
						if (bridged(level, pos, dx, dy, dz)) continue; // an orthogonal neighbour already joins the two
						target = new Vec3(dx, dy, dz).add(centre(there.getValue(SealBlock.FACE)));
					} else if (there.is(ModTags.QI_VEIN_BLOCKS) || there.getBlock() instanceof FormationCoreBlock) {
						if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) continue; // only what it lies against
						target = new Vec3(dx + 0.5, dy + 0.5, dz + 0.5);
					} else {
						continue;
					}
					Vec3 mid = centre.add(target).scale(0.5);
					ribbon(buffer, matrix, centre, mid, normal, glow);
				}
			}
		}
	}

	/** Where the ink lies in the block: just off the face of whatever it is painted on. */
	private static Vec3 centre(Direction face) {
		return switch (face) {
			case NORTH -> new Vec3(0.5, 0.5, LIFT);
			case SOUTH -> new Vec3(0.5, 0.5, 1 - LIFT);
			case WEST -> new Vec3(LIFT, 0.5, 0.5);
			case EAST -> new Vec3(1 - LIFT, 0.5, 0.5);
			default -> new Vec3(0.5, LIFT, 0.5);
		};
	}

	/** A diagonal neighbour also reachable through a seal beside both: the line goes round the corner, not across it. */
	private static boolean bridged(Level level, BlockPos pos, int dx, int dy, int dz) {
		if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) < 2) return false;
		int[][] steps = {{dx, 0, 0}, {0, dy, 0}, {0, 0, dz}};
		for (int[] step : steps) {
			if (step[0] == 0 && step[1] == 0 && step[2] == 0) continue;
			if (level.getBlockState(pos.offset(step[0], step[1], step[2])).getBlock() instanceof SealBlock) return true;
		}
		return false;
	}

	private static void ribbon(VertexConsumer buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 normal, float glow) {
		Vec3 along = b.subtract(a);
		Vec3 side = along.cross(normal);
		if (side.lengthSqr() < 1.0e-6) side = along.cross(new Vec3(0, 1, 0));
		if (side.lengthSqr() < 1.0e-6) side = new Vec3(1, 0, 0);
		Vec3 halo = side.normalize().scale(HALF_WIDTH * 2.6);
		side = side.normalize().scale(HALF_WIDTH);
		float edge = 0.15f * glow;
		float core = 0.85f * glow;
		// A faint halo of qi either side, then the ink itself, bright down the middle and fading to its edges.
		quad(buffer, matrix, a.add(halo), 0, a.add(side), edge * 0.8f, b.add(side), edge * 0.8f, b.add(halo), 0);
		quad(buffer, matrix, a.subtract(side), edge * 0.8f, a.subtract(halo), 0, b.subtract(halo), 0, b.subtract(side), edge * 0.8f);
		quad(buffer, matrix, a.add(side), edge, a, core, b, core, b.add(side), edge);
		quad(buffer, matrix, a, core, a.subtract(side), edge, b.subtract(side), edge, b, core);
	}

	/** A little diamond of ink where the seal itself is. */
	private static void dot(VertexConsumer buffer, Matrix4f matrix, Vec3 c, Direction face, float glow) {
		Vec3 u, v;
		if (face.getAxis() == Direction.Axis.Y) {
			u = new Vec3(DOT, 0, 0);
			v = new Vec3(0, 0, DOT);
		} else if (face.getAxis() == Direction.Axis.Z) {
			u = new Vec3(DOT, 0, 0);
			v = new Vec3(0, DOT, 0);
		} else {
			u = new Vec3(0, 0, DOT);
			v = new Vec3(0, DOT, 0);
		}
		float a = 0.9f * glow;
		quad(buffer, matrix, c.add(u), 0.1f * glow, c.add(v), 0.1f * glow, c.subtract(u), 0.1f * glow, c.subtract(v), 0.1f * glow);
		quad(buffer, matrix, c.add(u.scale(0.5)), a, c.add(v.scale(0.5)), a, c.subtract(u.scale(0.5)), a, c.subtract(v.scale(0.5)), a);
	}

	private static void quad(VertexConsumer buffer, Matrix4f matrix, Vec3 p0, float a0, Vec3 p1, float a1, Vec3 p2, float a2, Vec3 p3, float a3) {
		vertex(buffer, matrix, p0, a0);
		vertex(buffer, matrix, p1, a1);
		vertex(buffer, matrix, p2, a2);
		vertex(buffer, matrix, p3, a3);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 p, float alpha) {
		buffer.vertex(matrix, (float) p.x, (float) p.y, (float) p.z).color(INK[0], INK[1], INK[2], alpha).endVertex();
	}

	@Override
	public int getViewDistance() {
		return 64;
	}
}
