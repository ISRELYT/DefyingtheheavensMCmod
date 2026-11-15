package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.CorpseWalk;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A body of the dead walked in (CorpseWalk, 2026-11-13) is undead to the world, as its zombies and skeletons are: Smite bites it, a healing
 * potion harms it and a harming one heals it, Regeneration and poison don't take. Server side, where all of that is reckoned.
 */
@Mixin(LivingEntity.class)
public abstract class CorpseUndeadMixin {
	@Inject(method = "getMobType", at = @At("HEAD"), cancellable = true)
	private void dth$theWalkingDead(CallbackInfoReturnable<MobType> cir) {
		if ((Object) this instanceof ServerPlayer walker && CorpseWalk.walking(walker)) cir.setReturnValue(MobType.UNDEAD);
	}
}
