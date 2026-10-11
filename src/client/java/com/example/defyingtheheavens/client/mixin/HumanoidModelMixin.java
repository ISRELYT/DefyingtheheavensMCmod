package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.Gesture;
import com.example.defyingtheheavens.InnerBodyEntity;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.SoulBodyEntity;
import com.example.defyingtheheavens.client.ClientGestures;
import com.example.defyingtheheavens.client.ClientMeditationTracker;
import com.example.defyingtheheavens.client.GesturePose;
import com.example.defyingtheheavens.client.LotusPose;
import com.example.defyingtheheavens.client.SwordFlightClient;
import com.example.defyingtheheavens.client.SwordStance;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses meditating players, meditating NPC cultivators and bodies left behind by souls in the Inner Realm cross-legged on
 * the ground, sword riders (players and NPCs) in their sideways stance on the blade, kneeling cultivators on one knee, and
 * greetings (a bow, a cupped fist; see GesturePose). Injected at the TAIL of
 * HumanoidModel#setupAnim, which PlayerModel calls before copying limbs onto the sleeve/pants layers, so the outer layers
 * follow automatically.
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dth$lotusPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
							   float netHeadYaw, float headPitch, CallbackInfo ci) {
		if (SwordFlightClient.isRiding(entity)) {
			float partialTick = ageInTicks - entity.tickCount; // the renderer's ageInTicks is tickCount + partial tick
			SwordStance.apply((HumanoidModel<?>) (Object) this, netHeadYaw, SwordFlightClient.bodyOffset(entity, partialTick));
			return;
		}
		// A soul walking its Inner Realm is meditating too, but stands and walks.
		boolean meditating = entity instanceof Player ? ClientMeditationTracker.isMeditating(entity.getUUID())
				&& !ModDimensions.isInnerRealm(entity.level().dimension())
				: entity instanceof CultivatorNpc npc && npc.isMeditating();
		// A body left behind while its soul is in the Inner Realm, or out of it (Soul Projection), sits the same way.
		if (meditating || entity instanceof InnerBodyEntity || entity instanceof SoulBodyEntity) {
			LotusPose.apply((HumanoidModel<?>) (Object) this);
			return;
		}
		// A cultivator on one knee: begging for its life, or under the pressure of someone two realms above (TalkSystem).
		if (entity instanceof CultivatorNpc npc && npc.isKneeling()) {
			GesturePose.kneel((HumanoidModel<?>) (Object) this);
			return;
		}
		// A greeting: a bow, or fist cupped in palm (TalkSystem's Greet).
		Gesture gesture = ClientGestures.of(entity);
		if (gesture != null) GesturePose.apply((HumanoidModel<?>) (Object) this, gesture, ClientGestures.weight(entity, ageInTicks - entity.tickCount));
	}
}
