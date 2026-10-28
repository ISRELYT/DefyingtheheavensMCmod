package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.LandLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A chunk's land ledger (LandLedger) is saved in the chunk's own file entry, and read back with it. */
@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
	@Inject(method = "write", at = @At("RETURN"))
	private static void dth$writeLedger(ServerLevel level, ChunkAccess chunk, CallbackInfoReturnable<CompoundTag> cir) {
		LandLedger.write(chunk, cir.getReturnValue());
	}

	@Inject(method = "read", at = @At("RETURN"))
	private static void dth$readLedger(ServerLevel level, PoiManager poi, ChunkPos pos, CompoundTag tag, CallbackInfoReturnable<ProtoChunk> cir) {
		LandLedger.read(cir.getReturnValue(), tag);
	}
}
