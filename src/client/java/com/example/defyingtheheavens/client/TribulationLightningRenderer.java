package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.TribulationCloud;
import com.example.defyingtheheavens.TribulationLightning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import org.joml.Matrix4f;

/**
 * Heavenly Tribulation lightning: vanilla's bolt shape, recoloured bright blue (the Netherworld's tribulation's cyan). Vanilla
 * draws its layered quads in (0.45, 0.45, 0.5) with additive blending; the same layers in this blue build up to a glowing blue
 * core. Drawn here rather than by vanilla's renderer so the greater tribulations' bolts can be thicker
 * (TribulationManager#boltScale) while their zig-zag stays vanilla's: scaled whole, the bigger bolts sprawled far across the
 * sky (the user's ask, 2026-10-19: "only thicker and keep the old spread").
 */
public class TribulationLightningRenderer extends LightningBoltRenderer {
	private static final int RED = 38;
	private static final int GREEN = 115;
	private static final int BLUE = 255;
	/** The Netherworld's tribulation's bolt: a soul fire cyan. */
	private static final int GHOST_RED = 70, GHOST_GREEN = 235, GHOST_BLUE = 210;
	/** Vanilla's alpha (0.3) for every quad. */
	private static final int ALPHA = (int) (0.3f * 255.0f);

	public TribulationLightningRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(LightningBolt bolt, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		poseStack.pushPose();
		// Use the owning storm's server-synced height, including bolts striking lower terrain (vanilla's bolt is 128 tall).
		float height = bolt instanceof TribulationLightning stormBolt
				? stormBolt.renderHeight() : TribulationCloud.BASE_HEIGHT_ABOVE_PLAYER;
		// Thicker the greater the tribulation (TribulationManager#boltScale); the zig-zag as vanilla's whatever the tribulation.
		float thickness = bolt instanceof TribulationLightning stormBolt ? stormBolt.renderScale() : 1.0f;
		boolean ghost = bolt instanceof TribulationLightning stormBolt && stormBolt.isGhost();
		poseStack.scale(1.0f, height / 128.0f, 1.0f);
		drawBolt(bolt.seed, poseStack.last().pose(), buffers.getBuffer(RenderType.lightning()), thickness,
				ghost ? GHOST_RED : RED, ghost ? GHOST_GREEN : GREEN, ghost ? GHOST_BLUE : BLUE);
		poseStack.popPose();
	}

	/**
	 * Vanilla's bolt (LightningBoltRenderer#render): the same branches from the same seed, four layers each wider and fainter,
	 * every layer {@code thickness} times vanilla's width, in the given colour.
	 */
	private static void drawBolt(long seed, Matrix4f pose, VertexConsumer vertices, float thickness, int red, int green, int blue) {
		float[] mainX = new float[8];
		float[] mainZ = new float[8];
		float endX = 0.0f;
		float endZ = 0.0f;
		RandomSource random = RandomSource.create(seed);
		for (int segment = 7; segment >= 0; segment--) {
			mainX[segment] = endX;
			mainZ[segment] = endZ;
			endX += random.nextInt(11) - 5;
			endZ += random.nextInt(11) - 5;
		}
		for (int layer = 0; layer < 4; layer++) {
			RandomSource branches = RandomSource.create(seed);
			for (int branch = 0; branch < 3; branch++) {
				int top = branch > 0 ? 7 - branch : 7;
				int bottom = branch > 0 ? top - 2 : 0;
				float x = mainX[top] - endX;
				float z = mainZ[top] - endZ;
				for (int segment = top; segment >= bottom; segment--) {
					float upperX = x;
					float upperZ = z;
					if (branch == 0) {
						x += branches.nextInt(11) - 5;
						z += branches.nextInt(11) - 5;
					} else {
						x += branches.nextInt(31) - 15;
						z += branches.nextInt(31) - 15;
					}
					float upperWidth = 0.1f + layer * 0.2f;
					if (branch == 0) upperWidth *= segment * 0.1f + 1.0f;
					float lowerWidth = 0.1f + layer * 0.2f;
					if (branch == 0) lowerWidth *= (segment - 1.0f) * 0.1f + 1.0f;
					upperWidth *= thickness;
					lowerWidth *= thickness;
					quad(pose, vertices, x, z, segment, upperX, upperZ, red, green, blue, upperWidth, lowerWidth, false, false, true, false);
					quad(pose, vertices, x, z, segment, upperX, upperZ, red, green, blue, upperWidth, lowerWidth, true, false, true, true);
					quad(pose, vertices, x, z, segment, upperX, upperZ, red, green, blue, upperWidth, lowerWidth, true, true, false, true);
					quad(pose, vertices, x, z, segment, upperX, upperZ, red, green, blue, upperWidth, lowerWidth, false, true, false, false);
				}
			}
		}
	}

	/** One side of a segment, as vanilla's LightningBoltRenderer#quad draws it. */
	private static void quad(Matrix4f pose, VertexConsumer vertices, float lowerX, float lowerZ, int segment, float upperX, float upperZ,
			int red, int green, int blue, float upperWidth, float lowerWidth, boolean x1, boolean z1, boolean x2, boolean z2) {
		vertices.vertex(pose, lowerX + (x1 ? lowerWidth : -lowerWidth), segment * 16, lowerZ + (z1 ? lowerWidth : -lowerWidth)).color(red, green, blue, ALPHA).endVertex();
		vertices.vertex(pose, upperX + (x1 ? upperWidth : -upperWidth), (segment + 1) * 16, upperZ + (z1 ? upperWidth : -upperWidth)).color(red, green, blue, ALPHA).endVertex();
		vertices.vertex(pose, upperX + (x2 ? upperWidth : -upperWidth), (segment + 1) * 16, upperZ + (z2 ? upperWidth : -upperWidth)).color(red, green, blue, ALPHA).endVertex();
		vertices.vertex(pose, lowerX + (x2 ? lowerWidth : -lowerWidth), segment * 16, lowerZ + (z2 ? lowerWidth : -lowerWidth)).color(red, green, blue, ALPHA).endVertex();
	}
}
