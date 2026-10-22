package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The cultivator's strike counter, under it the lifespan tribulation's power (LifespanTribulation), and in space what flying there
 * has cost in years (AstralRealm#travel); storm visuals are shared
 * world entities, with no full-screen tint.
 */
public final class TribulationHud {
	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.options.hideGui) return;
			int w = mc.getWindow().getGuiScaledWidth();
			// Below the bars down the left (QiHud, NetherworldHud, a soul's SoulHud), which a long centred line would run into on a
			// narrow screen.
			int y = Math.max(Math.max(24, QiHud.stackBottom() + 4), SoulHud.bottom() + 2);
			if (ClientTribulationData.isActive()) {
				boolean ghost = ClientTribulationData.isGhost(); // the Netherworld's: the ghost path's
				graphics.drawCenteredString(mc.font,
						Component.translatable(ghost ? ModLang.TRIB_HUD_GHOST : ModLang.TRIB_HUD, ClientTribulationData.getTargetName(), ClientTribulationData.getStrikesLeft()),
						w / 2, y, ghost ? 0x6FE8D8 : 0x8ECFFF);
				y += 12;
			}
			if (ClientTribulationData.isLifespanActive()) {
				int percent = Math.round(ClientTribulationData.lifespanPower() * 100);
				boolean waiting = ClientTribulationData.isLifespanPaused();
				String key = waiting ? ModLang.LIFE_TRIB_HUD_WAITING : ClientTribulationData.isLifespanGhost() ? ModLang.LIFE_TRIB_HUD_DEAD : ModLang.LIFE_TRIB_HUD;
				graphics.drawCenteredString(mc.font, Component.translatable(key, percent), w / 2, y, waiting ? 0xA0A0A0 : 0xFF5050);
				y += 12;
			}
			// In space (AstralRealm#travel): what the trip has cost, the years left, and what flying straight home would cost; red once
			// those left can't pay for it.
			if (ClientTribulationData.isInSpace()) {
				boolean stranded = ClientTribulationData.spaceLeft() < ClientTribulationData.spaceHome();
				graphics.drawCenteredString(mc.font, Component.translatable(ModLang.HUD_SPACE_YEARS, years(ClientTribulationData.spaceTrip()),
						years(ClientTribulationData.spaceLeft()), years(ClientTribulationData.spaceHome())), w / 2, y, stranded ? 0xFF6060 : 0xC8B4FF);
			}
		});
	}

	/** Years as said on the HUD: to a tenth under ten, else whole, with thousands marked. */
	private static String years(float years) {
		return years < 10 ? String.format("%.1f", Math.max(0, years)) : String.format("%,d", Math.round(years));
	}

	private TribulationHud() {}
}
