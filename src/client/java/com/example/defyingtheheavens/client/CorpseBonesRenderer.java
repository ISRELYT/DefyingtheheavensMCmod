package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A player walking in a body of the dead gone to bones (CorpseWalk, ClientBodies#corpseRot): drawn as a skeleton in place of themselves
 * (PlayerRendererMixin), in the skeleton's own shape and bone, wearing their armour (the skeleton's armour models), holding what they hold,
 * a head on their head, moving as they move. Made with the player renderers (DefyingTheHeavensClient: each reload makes it anew).
 */
public class CorpseBonesRenderer extends LivingEntityRenderer<AbstractClientPlayer, HumanoidModel<AbstractClientPlayer>> {
	public static final ResourceLocation SKELETON = new ResourceLocation("textures/entity/skeleton/skeleton.png");
	/** Players are drawn this much smaller than their model (PlayerRenderer#scale): so are their bones. */
	private static final float SCALE = 0.9375f;
	private static CorpseBonesRenderer instance;

	public CorpseBonesRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5f);
		// A demon's bones keep its race's skull and tail (BeastHeadModels: the bones' look), its man's skull taken off (LivingEntityRendererMixin).
		addLayer(new BeastLayer<>(this));
		addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
				new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)), context.getModelManager()));
		addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
		addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
	}

	/** Made anew with the player renderers (their layers registered: DefyingTheHeavensClient). */
	public static void build(EntityRendererProvider.Context context) {
		instance = new CorpseBonesRenderer(context);
	}

	/** The bones' model (its arms for the hand in first person: ClientBodies#drawCorpseArm), or null before the renderers are made. */
	public static HumanoidModel<AbstractClientPlayer> bonesModel() {
		return instance == null ? null : instance.getModel();
	}

	/** {@code player} drawn as bones, in place of their own drawing: false (nothing drawn) before the renderers are made. */
	public static boolean draw(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		if (instance == null) return false;
		instance.render(player, yaw, partialTick, pose, buffers, light);
		return true;
	}

	@Override
	public void render(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		HumanoidModel<AbstractClientPlayer> model = getModel();
		model.crouching = player.isCrouching();
		HumanoidModel.ArmPose main = armPose(player, InteractionHand.MAIN_HAND), off = armPose(player, InteractionHand.OFF_HAND);
		if (main.isTwoHanded()) off = player.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
		boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
		model.rightArmPose = rightHanded ? main : off;
		model.leftArmPose = rightHanded ? off : main;
		super.render(player, yaw, partialTick, pose, buffers, light);
	}

	/** How an arm holds what is in {@code hand} (as PlayerRenderer reckons it, its main poses). */
	private static HumanoidModel.ArmPose armPose(AbstractClientPlayer player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
		if (player.getUsedItemHand() == hand && player.getUseItemRemainingTicks() > 0) {
			return switch (held.getUseAnimation()) {
				case BLOCK -> HumanoidModel.ArmPose.BLOCK;
				case BOW -> HumanoidModel.ArmPose.BOW_AND_ARROW;
				case SPEAR -> HumanoidModel.ArmPose.THROW_SPEAR;
				case CROSSBOW -> HumanoidModel.ArmPose.CROSSBOW_CHARGE;
				case SPYGLASS -> HumanoidModel.ArmPose.SPYGLASS;
				case TOOT_HORN -> HumanoidModel.ArmPose.TOOT_HORN;
				case BRUSH -> HumanoidModel.ArmPose.BRUSH;
				default -> HumanoidModel.ArmPose.ITEM;
			};
		}
		if (!player.swinging && held.is(Items.CROSSBOW) && CrossbowItem.isCharged(held)) return HumanoidModel.ArmPose.CROSSBOW_HOLD;
		return HumanoidModel.ArmPose.ITEM;
	}

	@Override
	protected void scale(AbstractClientPlayer player, PoseStack pose, float partialTick) {
		pose.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
		return SKELETON;
	}
}
