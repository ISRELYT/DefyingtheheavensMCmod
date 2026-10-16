package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A block a spell moves before your eyes (SpellBlock), drawn as the block itself: sliding along its way (a wall's rising, a trunk's
 * growing), flying in an arc and tumbling (clods gathering into a boulder), crumbling where it is (shaking, then shrinking and
 * falling apart), growing out of nothing, or rising and sinking back.
 */
public class SpellBlockRenderer extends EntityRenderer<SpellBlock> {
	private final BlockRenderDispatcher blocks;

	public SpellBlockRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.blocks = context.getBlockRenderDispatcher();
		shadowRadius = 0;
	}

	@Override
	public ResourceLocation getTextureLocation(SpellBlock block) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

	@Override
	public boolean shouldRender(SpellBlock block, Frustum frustum, double x, double y, double z) {
		return true;
	}

	@Override
	public void render(SpellBlock block, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		BlockState state = block.state();
		if (state.getRenderShape() != RenderShape.MODEL) return;
		float t = block.along(partial);
		if (block.tickCount + partial < block.delay() && block.motion() != SpellBlock.Motion.CRUMBLE) return; // not set off yet
		Vec3 travel = block.travel();
		float scale = block.scale();
		pose.pushPose();
		switch (block.motion()) {
			case SLIDE -> {
				float e = SpellRender.ease(t);
				pose.translate(travel.x * e, travel.y * e, travel.z * e);
				// A little shake as the ground gives it up.
				if (t < 1) pose.translate(Mth.sin((block.tickCount + partial) * 3.1f) * 0.02f, 0, Mth.cos((block.tickCount + partial) * 2.7f) * 0.02f);
			}
			case FLY -> {
				float e = SpellRender.ease(t);
				double arc = Math.sin(Math.PI * t) * Math.max(1.0, travel.length() * 0.25);
				pose.translate(travel.x * e, travel.y * e + arc, travel.z * e);
				scale *= 1 - 0.35f * t;
			}
			case CRUMBLE -> {
				float age = block.tickCount + partial - block.delay();
				if (age < 0) {
					t = 0;
				}
				// It trembles, then sinks and shrinks to nothing, turning as it goes.
				float shake = t < 0.3f ? 0.04f : 0.02f;
				pose.translate(Mth.sin(age * 4.3f) * shake, -t * t * 0.6f, Mth.cos(age * 3.7f) * shake);
				scale *= 1 - SpellRender.ease(Math.max(0, (t - 0.25f) / 0.75f));
			}
			case GROW -> scale *= SpellRender.easeOut(t) * (1 + 0.15f * Mth.sin(t * (float) Math.PI));
			case RISE_AND_SINK -> {
				float up = t < 0.5f ? SpellRender.ease(t * 2) : SpellRender.ease((1 - t) * 2);
				pose.translate(travel.x * up, travel.y * up, travel.z * up);
			}
		}
		if (scale <= 0.01f) {
			pose.popPose();
			return;
		}
		// About its middle: turned (a flying clod tumbles), scaled, then drawn from its corner.
		pose.translate(0, 0.5, 0);
		if (block.motion() == SpellBlock.Motion.FLY) {
			float spin = (block.tickCount + partial) * 0.4f + block.getId();
			pose.mulPose(Axis.XP.rotation(spin));
			pose.mulPose(Axis.YP.rotation(spin * 0.7f));
		} else if (block.motion() == SpellBlock.Motion.CRUMBLE) {
			pose.mulPose(Axis.YP.rotation(t * 1.5f));
		}
		pose.scale(scale, scale, scale);
		pose.translate(-0.5, -0.5, -0.5);
		blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
		pose.popPose();
	}
}
