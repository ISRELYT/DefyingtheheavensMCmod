package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.ImmortalCapital;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Immortal Realm's capital is laid by a feature, not a structure (ImmortalCapital: too wide for one), so vanilla's Beardifier, which
 * shapes the land round a structure's pieces as it is generated, knows nothing of its buildings: this hands it their boxes too, and the land
 * is drawn up under each and cut away over it as round a village's houses (the user's ask, 2026-10-25: the cities "dont need to be on
 * perfectly flat ground", as Minecraft's and Terralith's structures don't).
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
	@WrapOperation(method = "createNoiseChunk", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/Beardifier;forStructuresInChunk(Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/world/level/levelgen/Beardifier;"))
	private Beardifier dth$capitalBeard(StructureManager structures, ChunkPos pos, Operation<Beardifier> original,
			@Local(argsOnly = true) ChunkAccess chunk, @Local(argsOnly = true) RandomState random) {
		return ImmortalCapital.beard((NoiseBasedChunkGenerator) (Object) this, chunk, random, original.call(structures, pos));
	}
}
