package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.HeavenSpells;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A spell's tame fire (the spells' griefing off: Devastation#ignite, the user's ask, 2026-10-25): set down for a moment as one of the
 * spells' own blocks (HeavenSpells#placeTemporary), it catches nothing round it, burns nothing away and doesn't die down of itself; it goes
 * out when its moment is up and its block is taken up again.
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void dth$tameFire(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (HeavenSpells.isTemporary(level, pos)) ci.cancel();
	}
}
