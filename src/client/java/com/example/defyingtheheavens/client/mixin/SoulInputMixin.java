package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.SoulProjection;
import com.example.defyingtheheavens.client.ClientSoul;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the soul gathers itself to leave, the cultivator sits still: the movement keys do nothing (moving would break it off). */
@Mixin(KeyboardInput.class)
public abstract class SoulInputMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void dth$sitStill(boolean slow, float sneakFactor, CallbackInfo ci) {
		if (ClientSoul.state() != SoulProjection.State.PROJECTING) return;
		Input input = (Input) (Object) this;
		input.up = input.down = input.left = input.right = false;
		input.forwardImpulse = 0;
		input.leftImpulse = 0;
		input.jumping = false;
		input.shiftKeyDown = false;
	}
}
