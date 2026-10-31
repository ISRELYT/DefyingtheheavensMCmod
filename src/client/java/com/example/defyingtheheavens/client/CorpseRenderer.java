package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Bloodline;
import com.example.defyingtheheavens.CorpseBlock;
import com.example.defyingtheheavens.CorpseBlockEntity;
import com.example.defyingtheheavens.QiElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A body of the dead (CorpseBlockEntity, Corpses) drawn on its back where it lies, the way it faced, as the player model lies still: a player
 * in their own skin (their profile's, asked of the skin manager once as ClientBodies does; slim arms or broad as it says), a cultivator in
 * its own (CultivatorNpcRenderer); a demon in its race's skin, its beast's head and tail on it (BeastHeadModels). Fresh, a little pale;
 * rotting, grey-green and darker; bones, a skeleton in the same pose. Looked at, whose it was floats over it. Under Qi Sense its yin rises
 * off it (QiSenseClientHandler), as the ghostly qi it gives off (Corpses#yinAt).
 */
public class CorpseRenderer implements BlockEntityRenderer<CorpseBlockEntity> {
	private static final ResourceLocation SKELETON = new ResourceLocation("textures/entity/skeleton/skeleton.png");
	/** The player model's size (PlayerRenderer's), and how high its middle lies: its back on the ground, its head a little into it. */
	private static final float SCALE = 0.9375f, LIFT = 0.13f;
	/** How long the body is (the model's 32 pixels, at its size): it is laid with its middle on the block's. */
	private static final float LENGTH = 2.0f * SCALE;
	private static final double NAME_RANGE = 8;
	private static final RandomSource RANDOM = RandomSource.create();

	private final PlayerModel<LivingEntity> wide, slim;
	private final SkeletonModel<AbstractSkeleton> skeleton;

	/** Each dead player's skin and arms as last known (their profile asked of the skin manager once), as ClientBodies keeps them. */
	private static final Map<UUID, ResourceLocation> SKINS = new HashMap<>();
	private static final Map<UUID, Boolean> SLIM = new HashMap<>();
	private static final Set<UUID> ASKED = new HashSet<>();

	public CorpseRenderer(BlockEntityRendererProvider.Context context) {
		wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
		slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
		skeleton = new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON));
	}

	@Override
	public void render(CorpseBlockEntity body, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		BlockState state = body.getBlockState();
		CorpseBlock.Rot rot = state.hasProperty(CorpseBlock.STAGE) ? state.getValue(CorpseBlock.STAGE) : CorpseBlock.Rot.FRESH;
		pose.pushPose();
		// Its middle on the block's, turned the way it faced, fallen backwards: its feet toward where it faced, its face to the sky.
		pose.translate(0.5, LIFT, 0.5);
		pose.mulPose(Axis.YP.rotationDegrees(180 - body.yaw()));
		pose.translate(0, 0, LENGTH / 2);
		pose.mulPose(Axis.XP.rotationDegrees(-90));
		pose.scale(-SCALE, -SCALE, SCALE);
		pose.translate(0, -1.501, 0);
		if (rot == CorpseBlock.Rot.BONES) drawBones(pose, buffers, light);
		else drawBody(body, rot, pose, buffers, light);
		pose.popPose();
		yin(body, rot);
		name(body, pose, buffers, light);
	}

	/** The body in its own skin (or its race's), lying still; paler fresh, grey-green and dark rotting; a demon's beast's head on it. */
	private void drawBody(CorpseBlockEntity body, CorpseBlock.Rot rot, PoseStack pose, MultiBufferSource buffers, int light) {
		Bloodline race = body.race();
		ClientBloodlines.Look look = race == null ? null : new ClientBloodlines.Look(race, body.raceVariant(), body.tails());
		GameProfile profile = body.profile();
		ResourceLocation skin = look != null ? ClientBloodlines.skinOf(look) : profile != null ? playerSkin(profile) : CultivatorNpcRenderer.skinOf(body.skin());
		PlayerModel<LivingEntity> model = profile != null && look == null && slim(profile) ? slim : wide;
		still(model);
		BeastHeadModels.Head beast = look == null ? null : BeastHeadModels.get(race.head());
		if (beast != null) {
			model.head.visible = false;
			model.hat.visible = false;
		}
		float r = rot == CorpseBlock.Rot.FRESH ? 0.86f : 0.52f, g = rot == CorpseBlock.Rot.FRESH ? 0.86f : 0.58f, b = rot == CorpseBlock.Rot.FRESH ? 0.88f : 0.45f;
		model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY, r, g, b, 1.0f);
		if (beast != null) {
			beast.on(null, look);
			beast.follow(model);
			beast.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(beast.texture())), light, OverlayTexture.NO_OVERLAY, r, g, b, 1.0f);
		}
	}

	/** A skeleton in the body's place, lying as it lay. */
	private void drawBones(PoseStack pose, MultiBufferSource buffers, int light) {
		skeleton.setAllVisible(true);
		skeleton.young = false; // a model is drawn as a child's until told otherwise
		skeleton.crouching = false;
		skeleton.riding = false;
		skeleton.head.resetPose();
		skeleton.hat.resetPose();
		skeleton.body.resetPose();
		skeleton.rightArm.resetPose();
		skeleton.leftArm.resetPose();
		skeleton.rightLeg.resetPose();
		skeleton.leftLeg.resetPose();
		skeleton.rightArm.zRot = 0.08f;
		skeleton.leftArm.zRot = -0.08f;
		skeleton.head.zRot = 0.25f; // the skull rolled to one side
		skeleton.hat.copyFrom(skeleton.head);
		skeleton.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(SKELETON)), light, OverlayTexture.NO_OVERLAY, 0.92f, 0.9f, 0.84f, 1.0f);
	}

	/** {@code model} lying still: arms by its sides a little out, legs a little apart, looking straight up; its outer layers on its limbs. */
	private static void still(PlayerModel<LivingEntity> model) {
		model.setAllVisible(true);
		model.young = false;
		model.crouching = false;
		model.riding = false;
		model.attackTime = 0;
		model.head.resetPose();
		model.body.resetPose();
		model.rightArm.resetPose();
		model.leftArm.resetPose();
		model.rightLeg.resetPose();
		model.leftLeg.resetPose();
		model.rightArm.zRot = 0.08f;
		model.leftArm.zRot = -0.08f;
		model.rightLeg.zRot = 0.04f;
		model.leftLeg.zRot = -0.04f;
		model.hat.copyFrom(model.head);
		model.jacket.copyFrom(model.body);
		model.rightSleeve.copyFrom(model.rightArm);
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightPants.copyFrom(model.rightLeg);
		model.leftPants.copyFrom(model.leftLeg);
	}

	/**
	 * Under Qi Sense, yin rising off it now and then, as small as the land's own motes: a few a second whatever the frame rate (bones' half
	 * as often, as they give off half).
	 */
	private static void yin(CorpseBlockEntity body, CorpseBlock.Rot rot) {
		float perTick = rot == CorpseBlock.Rot.BONES ? 0.06f : 0.12f;
		if (!QiSenseClientHandler.isActive() || RANDOM.nextFloat() > perTick * Minecraft.getInstance().getDeltaFrameTime()) return;
		double x = body.getBlockPos().getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 1.4, y = body.getBlockPos().getY() + 0.3,
				z = body.getBlockPos().getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 1.4;
		QiSenseClientHandler.emit(QiElement.YIN, x, y, z, 0, 0.01 + RANDOM.nextDouble() * 0.01, 0, 0.06f + RANDOM.nextFloat() * 0.04f,
				80 + RANDOM.nextInt(40), null);
	}

	/** Looked at from near, whose body it was, over it. */
	private static void name(CorpseBlockEntity body, PoseStack pose, MultiBufferSource buffers, int light) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || body.name().isEmpty() || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
				|| !hit.getBlockPos().equals(body.getBlockPos()) || mc.player == null
				|| mc.player.distanceToSqr(hit.getLocation()) > NAME_RANGE * NAME_RANGE) return;
		Font font = mc.font;
		Component name = Component.literal(body.name());
		pose.pushPose();
		pose.translate(0.5, 0.75, 0.5);
		pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
		pose.scale(-0.025f, -0.025f, 0.025f);
		Matrix4f matrix = pose.last().pose();
		float x = -font.width(name) / 2f;
		int background = (int) (mc.options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
		font.drawInBatch(name, x, 0, 0x20FFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, light);
		font.drawInBatch(name, x, 0, 0xFFFFFFFF, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
		pose.popPose();
	}

	/** A dead player's skin: from the player list while they're in it, else from their profile, asked of the skin manager once. */
	private static ResourceLocation playerSkin(GameProfile profile) {
		UUID id = profile.getId();
		if (id == null) return DefaultPlayerSkin.getDefaultSkin();
		PlayerInfo info = Minecraft.getInstance().getConnection() == null ? null : Minecraft.getInstance().getConnection().getPlayerInfo(id);
		if (info != null) {
			SLIM.put(id, "slim".equals(info.getModelName()));
			return info.getSkinLocation();
		}
		if (!SKINS.containsKey(id) && ASKED.add(id)) {
			Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, location, texture) -> {
				if (type == MinecraftProfileTexture.Type.SKIN) {
					SKINS.put(id, location);
					SLIM.put(id, "slim".equals(texture.getMetadata("model")));
				}
			}, false);
		}
		return SKINS.getOrDefault(id, DefaultPlayerSkin.getDefaultSkin(id));
	}

	/** Whether a dead player's skin has slim arms (as last known, else their default skin's). */
	private static boolean slim(GameProfile profile) {
		UUID id = profile.getId();
		if (id == null) return false;
		Boolean known = SLIM.get(id);
		return known != null ? known : "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
	}

	/** Bodies are long: drawn while their block is near the edge of view. */
	@Override
	public boolean shouldRenderOffScreen(CorpseBlockEntity body) {
		return true;
	}
}
