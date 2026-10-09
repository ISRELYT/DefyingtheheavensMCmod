package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FruitAura;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.SpiritPedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the fruit on a Spirit Pedestal (the same peach as on the tree, floating, turning slowly and bobbing), its age
 * aura (shared with the tree, {@link CultivationFruitAuraRenderer#drawAura}), and, while someone meditates within reach,
 * the fruit's qi: a glowing arc from the fruit into its own orbit round the meditator, and that orbit, with beads of
 * light running along both. The shapes and the particles that go with them are {@link MeditationFormation}'s.
 */
public class SpiritPedestalRenderer implements BlockEntityRenderer<SpiritPedestalBlockEntity> {
	private static final int STREAM_SEGMENTS = 24;
	private static final int ORBIT_SEGMENTS = 48;
	private static final float STREAM_HALF_WIDTH = 0.06f;
	private static final float ORBIT_HALF_WIDTH = 0.05f;

	/** The pedestal's top surface (20 px). */
	private static final float PEDESTAL_TOP = 20 / 16f;
	/** Ginseng on a pedestal is drawn at this size. */
	private static final float HERB_SCALE = 0.7f;

	private final BlockRenderDispatcher blocks;

	public SpiritPedestalRenderer(BlockEntityRendererProvider.Context context) {
		this.blocks = context.getBlockRenderDispatcher();
	}

	@Override
	public void render(SpiritPedestalBlockEntity pedestal, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		Level level = pedestal.getLevel();
		if (level == null || !pedestal.hasFruit()) return;
		BlockPos pos = pedestal.getBlockPos();
		float clock = FruitAura.clock(level.getGameTime(), pos) + partialTick;
		int lightAbove = LevelRenderer.getLightColor(level, pos.above());
		float y;
		if (pedestal.isHerb()) {
			// Ginseng sits on the pedestal's top like a little potted plant, at a smaller size.
			y = FruitAura.PEDESTAL_HERB_Y;
			poseStack.pushPose();
			poseStack.translate(0.5, PEDESTAL_TOP, 0.5);
			poseStack.scale(HERB_SCALE, HERB_SCALE, HERB_SCALE);
			poseStack.translate(-0.5, 0, -0.5);
			Block plant = pedestal.getFruit().is(ModItems.SPIRIT_GINSENG) ? ModBlocks.SPIRIT_GINSENG : ModBlocks.GINSENG;
			blocks.renderSingleBlock(plant.defaultBlockState(), poseStack, buffers, lightAbove, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		} else {
			// The fruit floats, turning slowly about its own centre and bobbing.
			y = FruitAura.PEDESTAL_FRUIT_Y + 0.04f * Mth.sin(clock * 0.08f);
			poseStack.pushPose();
			poseStack.translate(0.5, y, 0.5);
			poseStack.mulPose(Axis.YP.rotationDegrees(clock * 1.2f));
			poseStack.translate(-0.5, -FruitAura.CENTER_Y, -0.5);
			blocks.renderSingleBlock(ModBlocks.CULTIVATION_FRUIT.defaultBlockState(), poseStack, buffers, lightAbove, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		}

		int years = pedestal.fruitAge();
		Vec3 fruit = Vec3.atLowerCornerOf(pos).add(0.5, y, 0.5);
		poseStack.pushPose();
		poseStack.translate(0.5, y, 0.5);
		CultivationFruitAuraRenderer.drawAura(years, pos, fruit, clock, poseStack, buffers);
		poseStack.popPose();

		var feeding = MeditationFormation.fedBy(pos);
		if (feeding.isEmpty()) return;
		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		VertexConsumer glow = buffers.getBuffer(FruitAuraRenderType.GLOW);
		FruitAura.Tier tier = FruitAura.of(years);
		float[] rgb = FruitAura.colour(tier);
		// Young fruit give a faint stream; the oldest a bright one.
		float strength = 0.35f + 0.1f * tier.ordinal();
		double time = level.getGameTime() + partialTick;
		Vec3 origin = Vec3.atLowerCornerOf(pos);
		Matrix4f pose = poseStack.last().pose();
		for (MeditationFormation.Link link : feeding) {
			if (link.player().isRemoved()) continue;
			Vec3 feet = link.player().getPosition(partialTick);
			int lane = link.lane();
			// Your own orbits, seen from inside in first person, are kept softer.
			boolean ownView = link.player() == camera.getEntity() && !camera.isDetached();
			float s = ownView ? strength * 0.6f : strength;

			// The arc from the fruit to its orbit, beads of light running along it.
			List<Vec3> arc = new ArrayList<>(STREAM_SEGMENTS + 1);
			for (int i = 0; i <= STREAM_SEGMENTS; i++) arc.add(MeditationFormation.streamPoint(fruit, feet, lane, i / (double) STREAM_SEGMENTS));
			float flow = (float) (time / MeditationFormation.FLOW_TICKS);
			ribbon(glow, pose, camera, origin, arc, STREAM_HALF_WIDTH, false, rgb, t -> s * fade(t) * (0.3f + 0.7f * bead(t * 2 - flow)));

			// The orbit itself: a soft ring, with beads circling in the lane's direction.
			List<Vec3> ring = new ArrayList<>(ORBIT_SEGMENTS + 1);
			double entry = MeditationFormation.entryAngle(fruit, feet, lane);
			int dir = MeditationFormation.laneDirection(lane);
			for (int i = 0; i <= ORBIT_SEGMENTS; i++) ring.add(MeditationFormation.orbitPoint(feet, lane, entry + dir * Math.PI * 2 * i / ORBIT_SEGMENTS));
			float spin = (float) (time * MeditationFormation.ORBIT_SPEED / (Math.PI * 2));
			ribbon(glow, pose, camera, origin, ring, ORBIT_HALF_WIDTH, true, rgb, t -> s * 0.7f * (0.25f + 0.75f * bead(t * 3 - spin * 3)));
		}
	}

	/** Brightness of the stream along its length: fades in off the fruit, full where it joins the orbit. */
	private static float fade(double t) {
		return (float) Math.min(1.0, t * 4.0);
	}

	/** Beads of light: sharp bright peaks once per unit of {@code x}. */
	private static float bead(double x) {
		return (float) Math.pow(0.5 + 0.5 * Math.cos(Math.PI * 2 * x), 6);
	}

	/**
	 * A soft ribbon through {@code points} that always faces the camera: bright down its middle, clear at its edges.
	 * {@code alpha} gives the brightness at each point from its position 0..1 along the ribbon.
	 */
	private static void ribbon(VertexConsumer consumer, Matrix4f pose, Camera camera, Vec3 origin, List<Vec3> points, float halfWidth,
			boolean loop, float[] rgb, DoubleToFloatFunction alpha) {
		Vec3 eye = camera.getPosition();
		int n = points.size() - 1;
		Vec3 previous = null, previousSide = null;
		float previousAlpha = 0;
		for (int i = 0; i <= n; i++) {
			Vec3 at = points.get(i);
			Vec3 ahead = i < n ? points.get(i + 1).subtract(at) : loop ? points.get(1).subtract(points.get(0)) : at.subtract(points.get(i - 1));
			Vec3 side = ahead.cross(at.subtract(eye));
			side = side.lengthSqr() < 1.0e-8 ? Vec3.ZERO : side.normalize().scale(halfWidth);
			float a = alpha.apply(i / (double) n);
			if (previous != null) {
				Vec3 p0 = previous.subtract(origin), p1 = at.subtract(origin);
				// Edge to middle, then middle to the other edge, so the ribbon is soft at both sides.
				quad(consumer, pose, p0.add(previousSide), 0, p0, previousAlpha, p1, a, p1.add(side), 0, rgb);
				quad(consumer, pose, p0, previousAlpha, p0.subtract(previousSide), 0, p1.subtract(side), 0, p1, a, rgb);
			}
			previous = at;
			previousSide = side;
			previousAlpha = a;
		}
	}

	@FunctionalInterface
	private interface DoubleToFloatFunction {
		float apply(double value);
	}

	private static void quad(VertexConsumer consumer, Matrix4f pose, Vec3 p0, float a0, Vec3 p1, float a1, Vec3 p2, float a2, Vec3 p3,
			float a3, float[] rgb) {
		vertex(consumer, pose, p0, rgb, a0);
		vertex(consumer, pose, p1, rgb, a1);
		vertex(consumer, pose, p2, rgb, a2);
		vertex(consumer, pose, p3, rgb, a3);
	}

	private static void vertex(VertexConsumer consumer, Matrix4f pose, Vec3 p, float[] rgb, float a) {
		consumer.vertex(pose, (float) p.x, (float) p.y, (float) p.z).color(rgb[0], rgb[1], rgb[2], a).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(SpiritPedestalBlockEntity pedestal) {
		return true; // the aura and the streams reach beyond the block
	}

	@Override
	public int getViewDistance() {
		return 256; // like the fruit on the tree: a 10,000-year pillar is seen from afar
	}
}
