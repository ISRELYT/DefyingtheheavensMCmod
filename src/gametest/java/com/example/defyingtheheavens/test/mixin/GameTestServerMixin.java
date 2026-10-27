package com.example.defyingtheheavens.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The game tests' world has the mod's dimensions (the user's rule, 2026-10-27: "in future run game tests with the mods' dimensions"). The
 * vanilla test server bakes its world's dimensions from the flat preset with an empty registry of level stems, so no datapack dimension was
 * ever there (the Immortal Realm, the Great Void, the small worlds, the passages...): whatever ran only in them went untested (the small
 * worlds' tick, which broke the server's tick as it sent someone out, bug sweep 4). Baked here, as a world made on the title screen is, with
 * the datapack's own. Only in the tests' source set: the mod as shipped never loads this.
 */
@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
	/** In the lambda GameTestServer#create loads its world with: the flat preset's dimensions baked with the datapack's, not with none. */
	@WrapOperation(method = "method_40377", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
	private static WorldDimensions.Complete dth$withTheModsDimensions(WorldDimensions dimensions, Registry<LevelStem> none,
			Operation<WorldDimensions.Complete> bake, LevelSettings settings, WorldLoader.DataLoadContext context) {
		return bake.call(dimensions, context.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));
	}
}
