package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AstralPlanets;
import com.example.defyingtheheavens.AstralRealm;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModLang;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The cultivator's strike counter, under it the lifespan tribulation's power (LifespanTribulation), and in space what flying there
 * has cost in years (AstralRealm#travel) and the way home to the Overworld; on a planet just come down onto, how to leave it again.
 * Storm visuals are shared world entities, with no full-screen tint.
 */
public final class TribulationHud {
	/** A line's size and height, in the artifact's pixels (HallArt: drawn as the mockups' "G" draws its words). */
	private static final float SIZE = 17, LINE = 24;

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.options.hideGui) return;
			HallArt.beginHud(graphics);
			try {
				float cx = HallArt.width() / 2;
				// Below the bars down the left (QiHud, NetherworldHud, a soul's SoulHud), which a long centred line would run into on a
				// narrow screen.
				float y = Math.max(Math.max(72, QiHud.stackBottom() + SuppressionClient.lineHeight() + 12), SoulHud.bottom() + 6);
				if (ClientTribulationData.isActive()) {
					boolean ghost = ClientTribulationData.isGhost(); // the Netherworld's: the ghost path's
					line(graphics, Component.translatable(ghost ? ModLang.TRIB_HUD_GHOST : ModLang.TRIB_HUD, ClientTribulationData.getTargetName(),
							ClientTribulationData.getStrikesLeft()), cx, y, ghost ? 0x6FE8D8 : 0x8ECFFF);
					y += LINE;
				}
				if (ClientTribulationData.isLifespanActive()) {
					int percent = Math.round(ClientTribulationData.lifespanPower() * 100);
					boolean waiting = ClientTribulationData.isLifespanPaused();
					String key = waiting ? ModLang.LIFE_TRIB_HUD_WAITING : ClientTribulationData.isLifespanGhost() ? ModLang.LIFE_TRIB_HUD_DEAD : ModLang.LIFE_TRIB_HUD;
					line(graphics, Component.translatable(key, percent), cx, y, waiting ? 0xA0A0A0 : 0xFF5050);
					y += LINE;
				}
				// In space (AstralRealm#travel): what the trip has cost, the years left, and what flying straight home would cost; red once
				// those left can't pay for it.
				if (ClientTribulationData.isInSpace()) {
					boolean stranded = ClientTribulationData.spaceLeft() < ClientTribulationData.spaceHome();
					line(graphics, Component.translatable(ModLang.HUD_SPACE_YEARS, years(ClientTribulationData.spaceTrip()),
							years(ClientTribulationData.spaceLeft()), years(ClientTribulationData.spaceHome())), cx, y, stranded ? 0xFF6060 : 0xC8B4FF);
					y += LINE;
				}
				if (mc.player != null && ModDimensions.isSpace(mc.level.dimension())) drawWayHome(graphics, mc, cx, y);
				// Just come down onto a planet: how to leave it again (out into space, where the way home shows).
				if (ModDimensions.isPlanetSurface(mc.level.dimension())) {
					if (mc.level != landedOn) {
						landedOn = mc.level;
						landedAt = net.minecraft.Util.getMillis();
					}
					if (net.minecraft.Util.getMillis() - landedAt < PLANET_HINT_MS) {
						line(graphics, Component.translatable(ModLang.HUD_PLANET_WAY_OUT, AstralRealm.EXIT_Y), cx, y, 0xB8B8D8);
					}
				} else {
					landedOn = null;
				}
			} finally {
				HallArt.end(graphics);
			}
		});
	}

	/** A centred line at {@code y}, outlined as the mockups' words are. */
	private static void line(net.minecraft.client.gui.GuiGraphics g, Component text, float cx, float y, int color) {
		HallArt.textCentred(g, text.getString(), cx, y, SIZE, LINE, color, true);
	}

	/** Years as said on the HUD: to a tenth under ten, else whole, with thousands marked (billions too: whole doubles, ModPackets). */
	private static String years(double years) {
		return years < 10 ? String.format("%.1f", Math.max(0, years)) : String.format("%,d", Math.round(years));
	}

	/** A planet's level the player was last set down in, and when (to show {@link ModLang#HUD_PLANET_WAY_OUT} a while). */
	private static net.minecraft.client.multiplayer.ClientLevel landedOn;
	private static long landedAt;
	private static final long PLANET_HINT_MS = 15_000;

	/**
	 * The way home from space (the user's ask, 2026-10-22: a way to find the Overworld again): an arrow turned toward it from where one
	 * looks (straight up the screen: ahead), whether it lies well above or below, and how far its air is.
	 */
	private static void drawWayHome(net.minecraft.client.gui.GuiGraphics g, Minecraft mc, float cx, float y) {
		AstralPlanets planets = AstralPlanets.get();
		if (planets == null) return;
		AstralPlanets.Planet home = planets.overworld();
		Vec3 to = home.pos().subtract(mc.player.getEyePosition());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		float turn = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-to.x, to.z)) - mc.player.getYRot()); // + to the right
		float rise = Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(to.y, flat)) - mc.player.getXRot()); // - above the gaze
		String key = rise < -HOME_STEEP ? ModLang.HUD_SPACE_HOME_ABOVE : rise > HOME_STEEP ? ModLang.HUD_SPACE_HOME_BELOW : ModLang.HUD_SPACE_HOME;
		String text = Component.translatable(key, String.format("%,d", Math.round(Math.max(0, to.length() - home.atmosphere())))).getString();
		float arrowX = cx - HallArt.width(text, SIZE) / 2 - 24;
		g.pose().pushPose();
		g.pose().translate(arrowX, y + LINE / 2, 0);
		g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(turn));
		HallArt.textCentred(g, HOME_ARROW, 0, -LINE / 2, SIZE, LINE, HOME_COLOUR, true);
		g.pose().popPose();
		HallArt.textCentred(g, text, cx + 18, y, SIZE, LINE, HOME_COLOUR, true);
	}

	private static final String HOME_ARROW = "▲";
	private static final int HOME_COLOUR = 0xFFD54F;
	/** Further than this (degrees) above or below one's gaze, the Overworld is said to lie above or below. */
	private static final float HOME_STEEP = 25;

	private TribulationHud() {}
}
