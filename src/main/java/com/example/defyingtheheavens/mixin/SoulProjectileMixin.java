package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.GhostQuellingTalismanEntity;
import com.example.defyingtheheavens.Incorporeal;
import com.example.defyingtheheavens.Lingering;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Arrows, tridents and fireballs pass through a projected soul (see SoulProjection) and a ghost: only a cultivator's own hand
 * can reach them. All but a Ghost-Quelling Talisman, made to strike the dead (GhostQuellingTalismanEntity, which turns souls
 * away itself); and a nascent soul lingering after its death (Lingering), which anyone's blow finds, as it found its body.
 */
@Mixin(Projectile.class)
public abstract class SoulProjectileMixin {
	@Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
	private void dth$passThroughSouls(Entity target, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof GhostQuellingTalismanEntity) return;
		if (target instanceof Player player && Incorporeal.untouchable(player) && !Lingering.isSoul(player)) cir.setReturnValue(false);
	}
}
