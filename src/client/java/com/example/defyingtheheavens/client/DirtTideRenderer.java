package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DirtTide;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The Dirt Tide (DirtTide): a rolling wave of the ground's own earth under its rider, its crest curling forward under their feet, its
 * body churning (its clods turning over and over), dust and clods flung off its front and sides, a wake of loose earth behind.
 */
public class DirtTideRenderer extends EntityRenderer<DirtTide> {
	private static final float[] WHITE = {1, 1, 1};

	public DirtTideRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 1.0f;
	}

	@Override
	public ResourceLocation getTextureLocation(DirtTide tide) {
		return SpellRender.DUST;
	}

	@Override
	public void render(DirtTide tide, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		float age = tide.tickCount + partial;
		float life = Math.max(1, tide.life());
		float rise = SpellRender.easeOut(Math.min(1, age / 6)) * Mth.clamp((life - age) / 8, 0, 1);
		TextureAtlasSprite sprite = SpellRender.sprite(tide.earth());
		VertexConsumer earth = buffers.getBuffer(SpellRender.blocks());
		Quaternionf facing = entityRenderDispatcher.cameraOrientation();
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, tide.yRotO, tide.getYRot())));
		// The wave's body: clods heaped into a crest, each turning over as the wave rolls on.
		for (int i = 0; i < 26; i++) {
			long s = tide.getId() * 41L + i;
			float across = (SpellRender.hash(s) - 0.5f) * 2.0f;
			float along = (SpellRender.hash(s + 1) - 0.5f) * 1.8f;
			// Higher toward the front and middle: the crest.
			float height = (0.25f + 0.6f * (1 - Math.abs(across)) * (0.5f + 0.5f * (along + 0.9f) / 1.8f)) * rise;
			float roll = age * 0.35f + i;
			float c = (0.22f + SpellRender.hash(s + 2) * 0.18f) * rise;
			pose.pushPose();
			pose.translate(across, height + Mth.sin(roll) * 0.08f, along + Mth.cos(roll) * 0.1f);
			pose.mulPose(Axis.XP.rotation(roll));
			pose.mulPose(Axis.YP.rotation(i));
			SpellRender.box(earth, pose.last(), -c, -c, -c, c, c, c, WHITE, 1, sprite.getU0(), sprite.getV0(), sprite.getU(8), sprite.getV(8), light);
			pose.popPose();
		}
		// The curl of the crest under the rider's feet: a band of earth folding forward.
		for (int k = 0; k < 9; k++) {
			float t = k / 8f;
			float a = (float) Math.PI * (0.15f + 0.85f * t) + age * 0.25f;
			float y = 0.55f + Mth.sin(a) * 0.35f;
			float z = 0.5f + Mth.cos(a) * 0.35f;
			pose.pushPose();
			pose.translate(0, y * rise, z);
			pose.mulPose(Axis.XP.rotation(a));
			SpellRender.box(earth, pose.last(), -0.9f * rise, -0.07f, -0.12f, 0.9f * rise, 0.07f, 0.12f, WHITE, 1, sprite.getU0(), sprite.getV0(), sprite.getU1(),
					sprite.getV(4), light);
			pose.popPose();
		}
		pose.popPose();
		// Dust flung off its front and sides.
		VertexConsumer dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
		Vec3 ahead = Vec3.directionFromRotation(0, tide.getYRot());
		for (int i = 0; i < 10; i++) {
			long s = tide.getId() * 13L + i + (long) (age / 3);
			float t = (age % 3 + i * 0.3f) / 3f;
			Vec3 p = ahead.scale(0.8 - t * 2.5).add((SpellRender.hash(s) - 0.5) * 2.4, 0.2 + t * 0.8, (SpellRender.hash(s + 1) - 0.5) * 2.4);
			SpellRender.billboard(dust, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.9f + t, i, WHITE, 0.6f * (1 - t) * rise, 0, 0, 1, 1, light);
		}
	}
}
