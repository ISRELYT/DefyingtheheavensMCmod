package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Legend;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.SwordSpirit;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * A sword spirit (SwordSpirit): its sword itself, flying point first along its way and glowing (drawn full bright): one of the tomb's old
 * iron blades, or a spirit of Ganjiang or Moye in its own living colours. Its trail is its own (SwordSpirit#tick).
 */
public class SwordSpiritRenderer extends EntityRenderer<SwordSpirit> {
	/** Its sword drawn this many times a held sword's size. */
	static final float SCALE = 1.4f;

	private final ItemRenderer items;
	private final ItemStack tomb = new ItemStack(Items.IRON_SWORD);
	private final ItemStack ganjiang = new ItemStack(ModItems.legend(Legend.GANJIANG));
	private final ItemStack moye = new ItemStack(ModItems.legend(Legend.MOYE));

	public SwordSpiritRenderer(EntityRendererProvider.Context context) {
		super(context);
		items = context.getItemRenderer();
		shadowRadius = 0;
	}

	@Override
	public void render(SwordSpirit spirit, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		ItemStack sword = switch (spirit.kind()) {
			case SwordSpirit.GANJIANG -> ganjiang;
			case SwordSpirit.MOYE -> moye;
			default -> tomb;
		};
		Vec3 motion = spirit.getDeltaMovement();
		float heading = Mth.rotLerp(partialTick, spirit.yRotO, spirit.getYRot());
		float climb = (float) Math.toDegrees(Math.atan2(motion.y, Math.max(1.0e-4, motion.horizontalDistance())));
		poseStack.pushPose();
		poseStack.translate(0, spirit.getBbHeight() * 0.5, 0);
		poseStack.mulPose(Axis.YP.rotationDegrees(-heading)); // its +z to where it faces
		poseStack.mulPose(Axis.XP.rotationDegrees(-climb)); // its point up or down along its flight
		poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin((spirit.tickCount + partialTick) * 0.25f) * 12)); // a sway as it hangs in the air
		poseStack.mulPose(Axis.YP.rotationDegrees(-90)); // the sword's length (now along +x) to +z
		poseStack.mulPose(Axis.ZP.rotationDegrees(-45)); // its sprite's diagonal, from the hand at the lower left to the point, along +x
		poseStack.scale(SCALE, SCALE, SCALE);
		items.renderStatic(sword, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffers, spirit.level(),
				spirit.getId());
		poseStack.popPose();
		super.render(spirit, yaw, partialTick, poseStack, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(SwordSpirit spirit) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
