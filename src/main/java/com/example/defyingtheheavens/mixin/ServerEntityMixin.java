package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.MeteorFlight;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One flying fast in Meteor Flight (and a little after) is told to those watching where they are outright, not how far they went
 * since the last word (vanilla's teleport packet, which it sends itself once a move is past eight blocks, or every twenty seconds).
 * Vanilla tells one who starts watching a moving player where it is now, but goes on counting its moves from where it last told
 * the rest: at Meteor Flight's speeds the newcomer saw the flier blocks ahead of where it was, until the twenty seconds ran out
 * (2026-10-23, run 3: a Nascent Soul stopped at a wall, seen six blocks past it).
 */
@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
	@Shadow @Final private Entity entity;
	@Shadow private int teleportDelay;

	@Inject(method = "sendChanges", at = @At("HEAD"))
	private void dth$meteorFlierToldOutright(CallbackInfo ci) {
		if (entity instanceof ServerPlayer player && MeteorFlight.toldOutright(player)) teleportDelay = 401;
	}
}
