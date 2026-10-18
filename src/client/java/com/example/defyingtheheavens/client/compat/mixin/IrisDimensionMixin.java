package com.example.defyingtheheavens.client.compat.mixin;

import com.example.defyingtheheavens.client.compat.ShaderPacks;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Iris names the dimension a shader pack draws by the world's own name, and a pack gives one it doesn't know its Overworld
 * programs (Complementary Reimagined: {@code dimension.world0=*}), so the Netherworld had the Overworld's night: its stars,
 * clouds and moonlight. Named the Nether instead (ShaderPacks#dimensionFor), it has the Nether's: a dark sky that is only the
 * fog, lit by the fog's own colour, the Netherworld's dark teal. Only with Iris (IrisMixinPlugin); if a later Iris moves this,
 * it is left alone.
 */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.Iris", remap = false)
public abstract class IrisDimensionMixin {
	@Inject(method = "getCurrentDimension", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
	private static void dth$netherworldIsTheNether(CallbackInfoReturnable<Object> cir) {
		Object dimension = ShaderPacks.dimensionFor(Minecraft.getInstance().level);
		if (dimension != null) cir.setReturnValue(dimension);
	}
}
