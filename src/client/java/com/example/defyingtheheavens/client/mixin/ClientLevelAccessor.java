package com.example.defyingtheheavens.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The client's block prediction (as when it breaks a block one mines), for Meteor Flight's bulldozer to clear the way on screen at
 * once and have it undone if the server says no (MeteorFlightClient#clearAhead).
 */
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
	@Accessor("blockStatePredictionHandler")
	BlockStatePredictionHandler dth$getPredictions();
}
