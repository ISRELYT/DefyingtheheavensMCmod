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
