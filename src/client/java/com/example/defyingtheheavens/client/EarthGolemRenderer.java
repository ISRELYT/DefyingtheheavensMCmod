package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.EarthGolem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * An earth golem (EarthGolem): its model in packed earth with its molten seams glowing over it, floating with a slow bob, three
 * times as large for the Earthen Titan. It rises out of the ground over its first second, and its seams flicker.
 */
public class EarthGolemRenderer extends MobRenderer<EarthGolem, EarthGolemModel> {
	public EarthGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new EarthGolemModel(context.bakeLayer(EarthGolemModel.LAYER)), 0.8f);
		addLayer(new Seams(this));
	}

	@Override
	public ResourceLocation getTextureLocation(EarthGolem golem) {
		return SpellRender.GOLEM;
	}

	@Override
	protected void scale(EarthGolem golem, PoseStack pose, float partial) {
		float s = golem.size();
		pose.scale(s, s, s);
		shadowRadius = 0.8f * s;
	}

	@Override
	public void render(EarthGolem golem, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		float age = golem.tickCount + partial;
		pose.pushPose();
		// Out of the ground over its first second, then a slow bob in the air.
		float rise = SpellRender.easeOut(Math.min(1, age / 20));
		pose.translate(0, -golem.getBbHeight() * (1 - rise) + Mth.sin(age * 0.1f) * 0.08f * golem.size() + 0.3f * golem.size(), 0);
		super.render(golem, yaw, partial, pose, buffers, light);
		pose.popPose();
		if (golem.isInvisible()) return;
		MultiBufferSource own = SpellBuffers.begin(buffers);
		try {
			earthLight(golem, pose, own, age, rise, light);
		} finally {
			SpellBuffers.end();
		}
	}

	private static final float[] WHITE = {1, 1, 1}, EARTH = {0.85f, 0.6f, 0.3f};

	/**
	 * What the earth's qi shows round a golem: the earth's seal turning on the ground under it and its glow, stones circling its waist
	 * slowly, and dust thrown up as it heaves itself out of the ground.
	 */
	private void earthLight(EarthGolem golem, PoseStack pose, MultiBufferSource buffers, float age, float rise, int light) {
		float s = golem.size();
		float[] seal = SpellRender.light(com.example.defyingtheheavens.QiElement.EARTH);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.SEAL_EARTH)), pose.last(), 1.7f * s * rise, 0.06f, age * 0.02f, seal, 0.6f, SpellRender.FULL_BRIGHT);
		SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), 3.2f * s, 0.07f, 0, EARTH, 0.3f, SpellRender.FULL_BRIGHT);
		var sprite = SpellRender.sprite(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
		var rock = buffers.getBuffer(SpellRender.blocks());
		float lift = -golem.getBbHeight() * (1 - rise) + 0.3f * s;
		for (int i = 0; i < 6; i++) {
			double a = age * 0.04 + i * Math.PI / 3;
			float r = 1.1f * s, c = (0.12f + 0.08f * SpellRender.hash(golem.getId() * 5L + i)) * s;
			pose.pushPose();
			pose.translate(Math.cos(a) * r, lift + golem.getBbHeight() * 0.45f + 0.25f * s * Mth.sin(age * 0.07f + i), Math.sin(a) * r);
			pose.mulPose(com.mojang.math.Axis.XP.rotation(age * 0.05f + i));
			pose.mulPose(com.mojang.math.Axis.YP.rotation(age * 0.03f + i));
			SpellRender.box(rock, pose.last(), -c, -c, -c, c, c, c, WHITE, 1, sprite.getU0(), sprite.getV0(), sprite.getU(6), sprite.getV(6), light);
			pose.popPose();
		}
		if (age < 30) {
			var facing = entityRenderDispatcher.cameraOrientation();
			var dust = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
			float t = age / 30;
			for (int i = 0; i < 12; i++) {
				double a = Math.PI * 2 * i / 12 + SpellRender.hash(golem.getId() + i);
				float d = s * (0.8f + 1.6f * t);
				SpellRender.billboard(dust, pose, facing, (float) Math.cos(a) * d, 0.4f + t * s, (float) Math.sin(a) * d, s * (1.2f + t * 1.5f), i, WHITE, 0.6f * (1 - t),
						0, 0, 1, 1, light);
			}
		}
	}

	/** The golem's molten seams, glowing whatever the light. */
	private static final class Seams extends RenderLayer<EarthGolem, EarthGolemModel> {
		Seams(RenderLayerParent<EarthGolem, EarthGolemModel> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource buffers, int light, EarthGolem golem, float walk, float walkSpeed, float partial, float age,
						   float headYaw, float headPitch) {
			VertexConsumer vc = buffers.getBuffer(RenderType.eyes(SpellRender.GOLEM_GLOW));
			float flicker = 0.75f + 0.25f * Mth.sin(age * 0.3f + golem.getId());
			getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, flicker, flicker * 0.9f, flicker * 0.8f, 1);
		}
	}
}
