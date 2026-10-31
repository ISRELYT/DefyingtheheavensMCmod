package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.MirrorReflection;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * A reflection out of the Jade Lake Mirage's mirror (MirrorReflection): its owner, in their own skin (InnerBodyRenderer#skinOf), drawn
 * glassy: pale and silvered, a little see-through, as a figure in the lake's still water. Its plate (name and bar) as any cultivator's.
 */
public class MirrorReflectionRenderer extends CultivatorNpcRenderer {
	/** The glass's tint (red, green, blue, alpha) on its owner's colours. */
	static final float[] GLASS = {0.78f, 0.92f, 0.96f, 0.72f};

	public MirrorReflectionRenderer(EntityRendererProvider.Context context) {
		super(context, new GlassModel(context.bakeLayer(ModelLayers.PLAYER)));
	}

	@Override
	public ResourceLocation getTextureLocation(CultivatorNpc npc) {
		return npc instanceof MirrorReflection reflection ? InnerBodyRenderer.skinOf(reflection.getOwnerId()) : super.getTextureLocation(npc);
	}

	@Override
	protected RenderType getRenderType(CultivatorNpc npc, boolean bodyVisible, boolean translucent, boolean glowing) {
		return RenderType.entityTranslucent(getTextureLocation(npc));
	}

	/** The player model, drawn pale and silvered, seen through. */
	private static final class GlassModel extends PlayerModel<CultivatorNpc> {
		GlassModel(ModelPart root) {
			super(root, false);
		}

		@Override
		public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int light, int overlay, float r, float g, float b, float a) {
			super.renderToBuffer(pose, consumer, light, overlay, r * GLASS[0], g * GLASS[1], b * GLASS[2], a * GLASS[3]);
		}
	}
}
