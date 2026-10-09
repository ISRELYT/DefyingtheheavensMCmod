package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** The strikes still to come while a tribulation is running; the clouds overhead are {@link TribulationClouds}. */
public final class TribulationHud {
	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			if (!ClientTribulationData.isActive()) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.options.hideGui) return;

			int w = mc.getWindow().getGuiScaledWidth();
			graphics.drawCenteredString(mc.font,
					Component.translatable(ModLang.TRIB_HUD, ClientTribulationData.getTargetName(), ClientTribulationData.getStrikesLeft()),
					w / 2, 24, 0xFF5555);
		});
	}

	private TribulationHud() {}
}
