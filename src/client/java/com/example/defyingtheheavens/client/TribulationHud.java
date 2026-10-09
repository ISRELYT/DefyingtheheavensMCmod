package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Pulsing blood-red tint and the strikes still to come while a tribulation is running. */
public final class TribulationHud {
	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			if (!ClientTribulationData.isActive()) return;
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.options.hideGui) return;

			int w = mc.getWindow().getGuiScaledWidth();
			int h = mc.getWindow().getGuiScaledHeight();

			double pulse = 0.5 + 0.5 * Math.sin((mc.level.getGameTime() + tickDelta) * 0.2);
			int alpha = (int) (35 + 35 * pulse);
			graphics.fill(0, 0, w, h, (alpha << 24) | 0x400000);

			graphics.drawCenteredString(mc.font,
					Component.translatable(ModLang.TRIB_HUD, ClientTribulationData.getTargetName(), ClientTribulationData.getStrikesLeft()),
					w / 2, 24, 0xFF5555);
		});
	}

	private TribulationHud() {}
}
