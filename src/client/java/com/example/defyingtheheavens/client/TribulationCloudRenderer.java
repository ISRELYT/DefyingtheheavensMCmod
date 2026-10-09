package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.TribulationCloud;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** A small bank of slowly churning, block-shaped thunderclouds, with blue-white flashes on each strike. */
public class TribulationCloudRenderer extends EntityRenderer<TribulationCloud> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("defying-the-heavens", "textures/entity/tribulation_cloud.png");

	public TribulationCloudRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(TribulationCloud cloud, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
		// Shared world time keeps shape animation consistent for observers joining later.
		double time = cloud.level().getGameTime() + (double) partialTick;
		float age = (float) (time % 1000000);
		float growth = Mth.clamp((cloud.tickCount + partialTick) / 40.0f, 0.05f, 1.0f);
		float flash = cloud.flashStrength();
		Vec3 color = ((ClientLevel) cloud.level()).getCloudColor(partialTick).scale(0.95);
		poses.pushPose();
		poses.scale(growth * 2.0f, growth * 1.5f, growth * 2.0f);
		poses.mulPose(Axis.YP.rotationDegrees(Mth.sin(age * 0.0015f) * 8.0f));
		VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

		// A rounded, irregular silhouette about 17 blocks across. Individual lobes rise and sink slightly.
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				if (x * x + z * z > 5) continue;
				int shape = Math.floorMod(x * 31 + z * 17, 7);
				float bob = Mth.sin(age * 0.028f + shape) * 0.28f;
				float cx = x * 2.6f + Mth.sin(age * 0.018f + shape) * 0.45f;
				float cz = z * 2.6f + Mth.cos(age * 0.021f + shape) * 0.45f;
				float halfWidth = 1.6f + shape * 0.07f + Mth.sin(age * 0.009f + shape) * 0.22f;
				float bottom = 0.4f + bob + (shape % 3) * 0.13f;
				float top = bottom + 1.3f + shape * 0.18f;
				box(vertices, poses.last(), color, cx - halfWidth, bottom, cz - halfWidth,
						cx + halfWidth, top, cz + halfWidth, 1.0f, flash);
				if (x * x + z * z <= 2) {
					box(vertices, poses.last(), color, cx - 1.25f, top - 0.2f, cz - 1.25f,
							cx + 1.25f, top + 1.2f, cz + 1.25f, 1.0f, flash);
				}
			}
		}
		// Uneven detached clusters: offset overlapping slabs like the reference, not a uniform ring.
		for (int i = 0; i < 8; i++) {
			float angle = i * Mth.TWO_PI / 8.0f + Mth.sin(i * 2.3f) * 0.18f;
			float radius = 10.0f + (i % 3) * 1.1f;
			float cx = Mth.cos(angle) * radius + Mth.sin(age * 0.006f + i) * 1.4f;
			float cz = Mth.sin(angle) * radius + Mth.cos(age * 0.007f + i) * 1.3f;
			float bottom = 1.0f + (i % 3) * 0.7f + Mth.sin(age * 0.009f + i) * 0.5f;
			float width = 1.3f + (i % 3) * 0.45f;
			for (int lobe = 0; lobe < 2 + i % 3; lobe++) {
				float phase = i * 1.7f + lobe * 2.1f;
				float morph = 1.0f + Mth.sin(age * 0.008f + phase) * 0.2f;
				float dx = (lobe - 1) * width * 0.75f + Mth.sin(age * 0.005f + phase) * 0.35f;
				float dz = ((lobe % 2) - 0.5f) * width * 0.9f;
				float halfX = width * morph * (0.75f + (i % 2) * 0.25f);
				float halfZ = width * (1.8f - morph) * (0.7f + (lobe % 2) * 0.3f);
				float y = bottom + lobe * 0.13f;
				box(vertices, poses.last(), color, cx + dx - halfX, y, cz + dz - halfZ,
						cx + dx + halfX, y + 0.8f + morph * 0.3f, cz + dz + halfZ,
						1.0f, flash);
			}
		}
		poses.popPose();
	}

	private static void box(VertexConsumer v, PoseStack.Pose pose, Vec3 color, float x0, float y0, float z0,
			float x1, float y1, float z1, float shade, float flash) {
		face(v, pose, color, shade * 0.7f, flash, 0, -1, 0, x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1);
		face(v, pose, color, shade, flash, 0, 1, 0, x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0);
		face(v, pose, color, shade * 0.8f, flash, 0, 0, -1, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0);
		face(v, pose, color, shade * 0.8f, flash, 0, 0, 1, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
		face(v, pose, color, shade * 0.9f, flash, -1, 0, 0, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
		face(v, pose, color, shade * 0.9f, flash, 1, 0, 0, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1);
	}

	private static void face(VertexConsumer v, PoseStack.Pose pose, Vec3 color, float shade, float flash, float nx, float ny, float nz,
			float x0,float y0,float z0, float x1,float y1,float z1, float x2,float y2,float z2, float x3,float y3,float z3) {
		float red = Math.min(1, (float) color.x * shade + flash * 0.16f);
		float green = Math.min(1, (float) color.y * shade + flash * 0.20f);
		float blue = Math.min(1, (float) color.z * shade + flash * 0.28f);
		vertex(v, pose, x0,y0,z0, 0,0, nx,ny,nz, red,green,blue);
		vertex(v, pose, x1,y1,z1, 1,0, nx,ny,nz, red,green,blue);
		vertex(v, pose, x2,y2,z2, 1,1, nx,ny,nz, red,green,blue);
		vertex(v, pose, x3,y3,z3, 0,1, nx,ny,nz, red,green,blue);
	}

	private static void vertex(VertexConsumer v, PoseStack.Pose pose, float x, float y, float z, float u, float uv,
			float nx, float ny, float nz, float r, float g, float b) {
		v.vertex(pose.pose(), x, y, z).color(r, g, b, 0.88f).uv(u, uv)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
				.normal(pose.normal(), nx, ny, nz).endVertex();
	}

	@Override
	public ResourceLocation getTextureLocation(TribulationCloud cloud) {
		return TEXTURE;
	}
}
