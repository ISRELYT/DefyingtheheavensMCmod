package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.GhostCultivatorNpc;
import com.example.defyingtheheavens.HungryGhost;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The entity renderers of the Netherworld's dead (ghost cultivators, Hungry Ghosts). Their bodies are drawn as cyan light on
 * the sense layer (SoulRenderer#drawDead), like the players' ghosts, in the skins named here; these draw only what goes over
 * them: a ghost cultivator's nameplate (CultivatorNpcRenderer's), a Hungry Ghost's name tag if it has one. No shadow: the
 * dead cast none.
 */
final class GhostlyRenderers {
	static final ResourceLocation HUNGRY_GHOST_SKIN = DefyingTheHeavens.id("textures/entity/ghost/hungry_ghost.png");
	private static final ResourceLocation OX_HEAD = DefyingTheHeavens.id("textures/entity/ghost/ox_head.png");
	private static final ResourceLocation HORSE_FACE = DefyingTheHeavens.id("textures/entity/ghost/horse_face.png");
	private static final ResourceLocation MENG_PO = DefyingTheHeavens.id("textures/entity/ghost/meng_po.png");

	/** A ghost cultivator's skin: its own face from life, or the Gate's keepers' own. */
	static ResourceLocation skin(GhostCultivatorNpc ghost) {
		return switch (ghost.getRole()) {
			case OX_HEAD -> OX_HEAD;
			case HORSE_FACE -> HORSE_FACE;
			case MENG_PO -> MENG_PO;
			case NONE -> CultivatorNpcRenderer.skinOf(ghost.getSkin());
		};
	}

	/** A ghost cultivator: its plate alone. */
	static final class GhostCultivatorRenderer extends CultivatorNpcRenderer {
		GhostCultivatorRenderer(EntityRendererProvider.Context context) {
			super(context);
			shadowRadius = 0;
		}

		@Override
		protected boolean drawsBody(CultivatorNpc npc) {
			return false;
		}
	}

	/** A Hungry Ghost: nothing but a name tag, if one was put on it. */
	static final class HungryGhostRenderer extends EntityRenderer<HungryGhost> {
		HungryGhostRenderer(EntityRendererProvider.Context context) {
			super(context);
			shadowRadius = 0;
		}

		@Override
		public void render(HungryGhost ghost, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
			super.render(ghost, yaw, partialTick, poseStack, buffers, light);
		}

		@Override
		public ResourceLocation getTextureLocation(HungryGhost ghost) {
			return HUNGRY_GHOST_SKIN;
		}
	}

	private GhostlyRenderers() {}
}
