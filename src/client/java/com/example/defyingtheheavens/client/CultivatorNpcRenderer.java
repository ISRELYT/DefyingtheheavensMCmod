package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModLang;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * NPC cultivators: the player model in one of {@link CultivatorNpc#SKINS} skins, wearing whatever clothing or armor they
 * have, and always carrying a nameplate over their heads: their title ("[Sect Master]", "[Rogue Cultivator]") in its
 * colour, a red health bar with the numbers on it, and their path with its alignment ("Righteous (+120)"). A secluded
 * cultivator's Concealment Barrier hides the plate unless you're close.
 */
public class CultivatorNpcRenderer extends HumanoidMobRenderer<CultivatorNpc, PlayerModel<CultivatorNpc>> {
	private static final ResourceLocation[] SKINS = new ResourceLocation[CultivatorNpc.SKINS];
	private static final double PLATE_RANGE = 48;
	private static final double CONCEALED_RANGE = 10;
	private static final int BAR_WIDTH = 50;
	private static final int BAR_HEIGHT = 9;
	/** Depth between the health bar's layers, in plate units (the plate is drawn 1/40 of a block per unit). */
	private static final float LAYER = 0.6f;

	static {
		for (int i = 0; i < SKINS.length; i++) SKINS[i] = DefyingTheHeavens.id("textures/entity/cultivator/cultivator_" + i + ".png");
	}

	public CultivatorNpcRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
		addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(CultivatorNpc npc) {
		return SKINS[Mth.clamp(npc.getSkin(), 0, SKINS.length - 1)];
	}

	/** The plate replaces the vanilla name tag. */
	@Override
	protected boolean shouldShowName(CultivatorNpc npc) {
		return false;
	}

	@Override
	public void render(CultivatorNpc npc, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		super.render(npc, yaw, partialTick, poseStack, buffers, light);
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || npc.isInvisible()) return;
		double distanceSqr = entityRenderDispatcher.distanceToSqr(npc);
		double range = npc.isConcealed() ? CONCEALED_RANGE : PLATE_RANGE;
		if (distanceSqr > range * range) return;
		plate(npc, poseStack, buffers, light);
	}

	private void plate(CultivatorNpc npc, PoseStack poseStack, MultiBufferSource buffers, int light) {
		Font font = getFont();
		poseStack.pushPose();
		poseStack.translate(0, npc.getBbHeight() + 0.5, 0);
		poseStack.mulPose(entityRenderDispatcher.cameraOrientation());
		poseStack.scale(-0.025f, -0.025f, 0.025f);
		Matrix4f matrix = poseStack.last().pose();
		int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25f) * 255.0f) << 24;

		// The title, in its colour.
		Component title = Component.translatable(ModLang.NPC_TITLE_BRACKETS, npc.getTitle().getDisplayName());
		int titleColor = 0xFF000000 | npc.getTitle().getColor();
		float tx = -font.width(title) / 2f;
		font.drawInBatch(title, tx, -24, 0x20FFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, light);
		font.drawInBatch(title, tx, -24, titleColor, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);

		// The health bar: a gold frame, a dark track, the red of what's left, the numbers over it. Each layer sits a whole
		// LAYER in front of the one behind (in plate units, 1/40 block): any closer and the depth buffer can't tell them
		// apart at a distance, so the numbers flicker through the red and the font's own shadow shows as a second copy.
		float health = Math.max(0, npc.getHealth());
		float max = Math.max(1, npc.getMaxHealth());
		float x0 = -BAR_WIDTH / 2f, y0 = -13.5f;
		float fill = BAR_WIDTH * Math.min(1, health / max);
		VertexConsumer quads = buffers.getBuffer(RenderType.textBackground());
		quad(quads, matrix, x0 - 1, y0 - 1, x0 + BAR_WIDTH + 1, y0 + BAR_HEIGHT + 1, 0, 0xC0B8860B, light);
		quad(quads, matrix, x0, y0, x0 + BAR_WIDTH, y0 + BAR_HEIGHT, -LAYER, 0xE0200A0A, light);
		quad(quads, matrix, x0, y0, x0 + fill, y0 + BAR_HEIGHT, -2 * LAYER, 0xF0C0262B, light);
		quad(quads, matrix, x0, y0, x0 + fill, y0 + 2, -3 * LAYER, 0xF0E8585C, light);
		Component numbers = Component.literal(Mth.ceil(health) + " / " + Mth.ceil(max));
		float nx = -font.width(numbers) / 2f;
		poseStack.pushPose();
		// A dark shadow one pixel down and right, then the white numbers in front of it.
		poseStack.translate(0, 0, -4 * LAYER);
		font.drawInBatch(numbers, nx + 1, y0 + 2, 0xFF3A0A0C, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
		poseStack.translate(0, 0, -LAYER);
		font.drawInBatch(numbers, nx, y0 + 1, 0xFFFFFFFF, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
		poseStack.popPose();

		// The path.
		Component path = Alignment.describe(npc.getAlignment());
		float px = -font.width(path) / 2f;
		font.drawInBatch(path, px, -2, 0xFFFFFFFF, false, matrix, buffers, Font.DisplayMode.NORMAL, background, light);
		poseStack.popPose();
	}

	/** A flat rectangle, both windings, so it shows whichever way the culling of the render type runs. */
	private static void quad(VertexConsumer buffer, Matrix4f matrix, float x0, float y0, float x1, float y1, float z, int argb, int light) {
		int a = argb >>> 24, r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
		buffer.vertex(matrix, x0, y0, z).color(r, g, b, a).uv2(light).endVertex();
		buffer.vertex(matrix, x0, y1, z).color(r, g, b, a).uv2(light).endVertex();
		buffer.vertex(matrix, x1, y1, z).color(r, g, b, a).uv2(light).endVertex();
		buffer.vertex(matrix, x1, y0, z).color(r, g, b, a).uv2(light).endVertex();
		buffer.vertex(matrix, x1, y0, z).color(r, g, b, a).uv2(light).endVertex();
		buffer.vertex(matrix, x1, y1, z).color(r, g, b, a).uv2(light).endVertex();
		buffer.vertex(matrix, x0, y1, z).color(r, g, b, a).uv2(light).endVertex();
		buffer.vertex(matrix, x0, y0, z).color(r, g, b, a).uv2(light).endVertex();
	}
}
