package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.WheelOfReincarnationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The Wheel of Reincarnation (轮回): an upright six-spoked wheel 9 blocks across, of polished blackstone with gold studs and a
 * gold hub, Yin Jade inlaid along its spokes, turning slowly (once a minute) about its hub block. It faces the bridge (north).
 */
public class WheelOfReincarnationRenderer implements BlockEntityRenderer<WheelOfReincarnationBlockEntity> {
	/** Once round a minute: 360 degrees over 1,200 ticks. */
	private static final float DEGREES_PER_TICK = 0.3f;
	private static final float RIM_INNER = 4.0f, RIM_OUTER = 4.5f;
	private static final int RIM_SEGMENTS = 30;
	private static final ResourceLocation BLACKSTONE = new ResourceLocation("block/polished_blackstone_bricks");
	private static final ResourceLocation POLISHED = new ResourceLocation("block/polished_blackstone");
	private static final ResourceLocation GOLD = new ResourceLocation("block/gold_block");
	private static final ResourceLocation JADE = DefyingTheHeavens.id("block/yin_jade_block");

	public WheelOfReincarnationRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(WheelOfReincarnationBlockEntity wheel, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		if (wheel.getLevel() == null) return;
		float time = wheel.getLevel().getGameTime() + partialTick;
		float angle = time * DEGREES_PER_TICK + wheel.turnAngle(time); // its slow drift, and the sweep of a ghost's turn
		VertexConsumer solid = buffers.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
		TextureAtlasSprite blackstone = sprite(BLACKSTONE), polished = sprite(POLISHED), gold = sprite(GOLD), jade = sprite(JADE);
		int bright = LightTexture.FULL_BRIGHT;

		poseStack.pushPose();
		poseStack.translate(0.5, 0.5, 0.5);
		poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
		// The hub: gold, with a Yin Jade boss on either face.
		box(poseStack, solid, gold, light, -0.6f, -0.6f, -0.35f, 0.6f, 0.6f, 0.35f);
		box(poseStack, solid, jade, bright, -0.3f, -0.3f, -0.45f, 0.3f, 0.3f, 0.45f);
		for (int spoke = 0; spoke < 6; spoke++) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.ZP.rotationDegrees(spoke * 60.0f));
			box(poseStack, solid, blackstone, light, -0.16f, 0.55f, -0.18f, 0.16f, RIM_INNER + 0.05f, 0.18f);
			// Yin Jade inlaid down the spoke's faces, and a gold stud where it meets the rim.
			box(poseStack, solid, jade, bright, -0.07f, 1.0f, -0.21f, 0.07f, RIM_INNER - 0.4f, 0.21f);
			box(poseStack, solid, gold, light, -0.24f, RIM_OUTER - 0.05f, -0.24f, 0.24f, RIM_OUTER + 0.35f, 0.24f);
			poseStack.popPose();
		}
		// The rim, in short straight pieces.
		float step = 360.0f / RIM_SEGMENTS;
		float half = (float) (RIM_OUTER * Math.tan(Math.toRadians(step / 2))) + 0.02f;
		for (int i = 0; i < RIM_SEGMENTS; i++) {
			poseStack.pushPose();
			poseStack.mulPose(Axis.ZP.rotationDegrees(i * step));
			box(poseStack, solid, i % 5 == 0 ? polished : blackstone, light, -half, RIM_INNER, -0.22f, half, RIM_OUTER, 0.22f);
			poseStack.popPose();
		}
		poseStack.popPose();
	}

	private static TextureAtlasSprite sprite(ResourceLocation location) {
		return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
	}

	/** A box from (x0, y0, z0) to (x1, y1, z1) in the current pose, every face showing the whole sprite. */
	private static void box(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite, int light,
			float x0, float y0, float z0, float x1, float y1, float z1) {
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normal = poseStack.last().normal();
		float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
		// -z (north) and +z
		quad(consumer, pose, normal, sprite, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v1, u1, v0);
		quad(consumer, pose, normal, sprite, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v1, u1, v0);
		// -x and +x
		quad(consumer, pose, normal, sprite, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v1, u1, v0);
		quad(consumer, pose, normal, sprite, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v1, u1, v0);
		// -y and +y
		quad(consumer, pose, normal, sprite, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v1, u1, v0);
		quad(consumer, pose, normal, sprite, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v1, u1, v0);
	}

	private static void quad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, TextureAtlasSprite sprite, int light,
			float nx, float ny, float nz,
			float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
			float u0, float v0, float u1, float v1) {
		vertex(consumer, pose, normal, ax, ay, az, u0, v0, light, nx, ny, nz);
		vertex(consumer, pose, normal, bx, by, bz, u1, v0, light, nx, ny, nz);
		vertex(consumer, pose, normal, cx, cy, cz, u1, v1, light, nx, ny, nz);
		vertex(consumer, pose, normal, dx, dy, dz, u0, v1, light, nx, ny, nz);
	}

	private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, float x, float y, float z, float u, float v,
			int light, float nx, float ny, float nz) {
		consumer.vertex(pose, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
				.normal(normal, nx, ny, nz).endVertex();
	}

	@Override
	public boolean shouldRenderOffScreen(WheelOfReincarnationBlockEntity wheel) {
		return true; // the wheel reaches four and a half blocks out of its hub
	}

	@Override
	public int getViewDistance() {
		return 128;
	}
}
