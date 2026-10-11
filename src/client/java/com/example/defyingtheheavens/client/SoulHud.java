package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.SoulProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The projected soul's own readout, top left under the Qi bar (and the pressure mark): while it gathers to leave or comes home,
 * how long is left; while it is out, how far along its tether it is (blue, then red as it strains), its body's health (red when
 * low), and the body it holds, if any.
 */
public final class SoulHud {
	private static final int X = 4;
	private static final int Y = 30;
	private static final int BAR_W = 74;
	private static final int GOLD = 0xFFB8860B;
	private static final int LACQUER = 0xD0101420;
	private static final int TEXT = 0xE6D3A3;

	public static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.options.renderDebug) return;
		SoulProjection.State state = ClientSoul.state();
		if (state == SoulProjection.State.NONE) return;
		int y = Y;
		if (state == SoulProjection.State.PROJECTING || state == SoulProjection.State.RECALLING) {
			float seconds = ClientSoul.ticksLeft() / 20f;
			String key = state == SoulProjection.State.PROJECTING ? ModLang.HUD_SOUL_GATHERING : ModLang.HUD_SOUL_RECALLING;
			bar(g, y, 1 - ClientSoul.ticksLeft() / (float) SoulProjection.CHANNEL_TICKS, 0xFFB57BFF);
			g.drawString(mc.font, Component.translatable(key, String.format("%.1f", seconds)), X + BAR_W + 6, y - 1, TEXT);
			y += 12;
		}
		if (state == SoulProjection.State.PROJECTING) return;

		int radius = ClientSoul.radius();
		Vec3 at = mc.player.isPassenger() && mc.player.getVehicle() != null ? mc.player.getVehicle().position() : mc.player.position();
		double distance = at.distanceTo(ClientSoul.body());
		float reach = radius <= 0 ? 0 : (float) Math.min(1, distance / radius);
		bar(g, y, reach, reach > 0.85f ? 0xFFE0524A : 0xFF59C3FF);
		g.drawString(mc.font, Component.translatable(ModLang.HUD_SOUL_TETHER, Math.round(distance), radius), X + BAR_W + 6, y - 1, TEXT);
		y += 12;

		float max = ClientSoul.bodyMax();
		float health = max <= 0 ? 0 : ClientSoul.bodyHealth() / max;
		bar(g, y, health, health < 0.35f ? 0xFFE0524A : 0xFF7BD37B);
		g.drawString(mc.font, Component.translatable(ModLang.HUD_SOUL_BODY, Math.round(ClientSoul.bodyHealth()), Math.round(max)), X + BAR_W + 6, y - 1, TEXT);
		y += 12;

		Entity host = ClientSoul.host() < 0 || mc.level == null ? null : mc.level.getEntity(ClientSoul.host());
		if (host != null) g.drawString(mc.font, Component.translatable(ModLang.HUD_SOUL_POSSESSING, host.getDisplayName()), X, y, 0xD9B8FF);
	}

	/** A thin gold-framed bar, filled to {@code fill}. */
	private static void bar(GuiGraphics g, int y, float fill, int color) {
		g.fill(X, y, X + BAR_W, y + 6, GOLD);
		g.fill(X + 1, y + 1, X + BAR_W - 1, y + 5, LACQUER);
		int width = Math.round((BAR_W - 2) * Math.max(0, Math.min(1, fill)));
		if (width > 0) g.fill(X + 1, y + 1, X + 1 + width, y + 5, color);
	}

	private SoulHud() {}
}
