package com.example.defyingtheheavens.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the Spatial Gap's ascent lift (a hidden Levitation effect) skip vanilla's levitation advancement tracking. */
@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
	@Accessor("levitationStartPos")
	void dth$setLevitationStartPos(Vec3 pos);
}
