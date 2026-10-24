package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.GhostGate;
import com.example.defyingtheheavens.SectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * A blast doesn't break into a sect's treasury while its elders keep it (SectManager#treasurySealed): the seal that holds its
 * chests shut holds its walls and chests against fire and blast too, so nothing can be blown open and spilled.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow @Final private Level level;

	@Shadow public abstract List<BlockPos> getToBlow();

	@Inject(method = "explode", at = @At("TAIL"))
	private void dth$spareSealedTreasuries(CallbackInfo ci) {
		if (level instanceof ServerLevel server) SectManager.spareSealedTreasuries(server, getToBlow());
		// Nor a Ghost King's palace's treasury chests while their seal holds (PalaceTreasury#sealed: a bed set off beside them in the
		// Netherworld spilled them before the sweep of 2026-10-23).
		if (level instanceof ServerLevel server) {
			getToBlow().removeIf(pos -> com.example.defyingtheheavens.PalaceTreasury.isTreasury(server, pos) && com.example.defyingtheheavens.PalaceTreasury.sealed(server, pos));
		}
		// Nor anything on the Ghost Gate's ground (GhostGate#protects).
		GhostGate.spareFromBlast(level, getToBlow());
		// A spell's own blocks are taken up, not blown into loot (HeavenSpells#spareTemporaryFromBlast).
		if (level instanceof ServerLevel server) com.example.defyingtheheavens.HeavenSpells.spareTemporaryFromBlast(server, getToBlow());
	}
}
