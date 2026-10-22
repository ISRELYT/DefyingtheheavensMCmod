package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.VitalsHud;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla's hearts at the bottom held to a row of ten (the user's ask, 2026-10-22: a body past Four Axis has thousands of hearts),
 * the whole health shown in them, each a tenth of it (VitalsHud#heartScale; the bar in the top-left corner says the numbers): the health,
 * its maximum and absorption the hearts are drawn from are read scaled, so they and the armour row above them stand as for a body of
 * twenty. A body of twenty or less is drawn as ever. Not required: another mod's own hearts are left as they are drawn.
 */
@Mixin(Gui.class)
public abstract class GuiHeartsMixin {
	@ModifyExpressionValue(method = "renderPlayerHealth", require = 0,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getHealth()F"))
	private float dth$tenHeartsOfHealth(float health) {
		Player player = dth$player();
		return player == null ? health : health * VitalsHud.heartScale(player);
	}

	@ModifyExpressionValue(method = "renderPlayerHealth", require = 0,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D"))
	private double dth$tenHeartsAtMost(double max) {
		return Math.min(max, 20.0);
	}

	@ModifyExpressionValue(method = "renderPlayerHealth", require = 0,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAbsorptionAmount()F"))
	private float dth$tenHeartsOfAbsorption(float absorption) {
		Player player = dth$player();
		return player == null ? absorption : absorption * VitalsHud.heartScale(player);
	}

	/** The player whose hearts are drawn (vanilla's camera player). */
	private static Player dth$player() {
		return net.minecraft.client.Minecraft.getInstance().getCameraEntity() instanceof Player player ? player : null;
	}
}
