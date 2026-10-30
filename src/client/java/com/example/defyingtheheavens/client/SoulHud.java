package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.SoulProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The projected soul's own readout as the mockups' "G" draws it (in the artifact's pixels, HallArt), under the stack of bars (and the
 * pressure line): while it gathers to leave or comes home, a violet bar of how long is left; while it is out, hung from the stack on a
 * dashed violet cord, a line with a rose diamond and "Body", the tether at its right ("Tether: 21 blocks, no end"; red as it strains),
 * then the body's health as a bar, and the body it holds, if any; in a person's body, whose body it is in (its own pool is a bar of its
 * own: QiHud). In one's clone (Clones), that one is, and how the main body fares where it sits.
 */
public final class SoulHud {
	/** The violet of the soul's words and its cord; the rose of the body's line; the tether's words, and while it strains. */
	private static final int TEXT = 0xD9B8FF, CORD = 0xFFB57BFF, BODY_LINE = 0xFFB4AA, TETHER = 0xC9A8FF, STRAINED = 0xFF8A8A;
	/** The body's bar: its height, border and band. */
	private static final int BODY_H = 26, BODY_BORDER = 3, BODY_BAND = 4;
	private static final QiHud.Pool CHANNEL = new QiHud.Pool(HallHud.SOUL, HallHud.SOUL_LIGHT);

	/** Where it starts, in the artifact's pixels: under the stack of bars, and the pressure line under them while pressed. */
	private static float start() {
		return QiHud.stackBottom() + SuppressionClient.lineHeight();
	}

	public static void render(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui || mc.options.renderDebug) return;
		HallArt.beginHud(g);
		try {
			draw(g, mc);
		} finally {
			HallArt.end(g);
		}
	}

	private static void draw(GuiGraphics g, Minecraft mc) {
		SoulProjection.State state = ClientSoul.state();
		if (state == SoulProjection.State.NONE) {
			// In one's clone (Clones): that one is, and how the main body fares where it sits.
			if (ClientCultivationData.inClone()) {
				float y = start();
				line(g, y, Component.translatable(ModLang.HUD_CLONE_IN).getString(), CORD, TEXT);
				float max = ClientClone.mainMax();
				bodyBar(g, y + 26, max <= 0 ? 0 : ClientClone.mainHealth() / max, ModLang.HUD_CLONE_MAIN, ClientClone.mainHealth(), max);
			}
			return;
		}
		float y = start();
		if (state == SoulProjection.State.PROJECTING || state == SoulProjection.State.RECALLING) {
			float seconds = ClientSoul.ticksLeft() / 20f;
			String key = state == SoulProjection.State.PROJECTING ? ModLang.HUD_SOUL_GATHERING : ModLang.HUD_SOUL_RECALLING;
			CHANNEL.draw(g, y, 1 - ClientSoul.ticksLeft() / (float) SoulProjection.CHANNEL_TICKS,
					Component.translatable(key, String.format("%.1f", seconds)).getString(), false);
			y += QiHud.ROW;
		}
		if (state == SoulProjection.State.PROJECTING) return;

		// The cord from the stack down to the body's line: three wide, four on and three off.
		for (float dy = y - 2; dy < y + 18; dy += 7) HallArt.rect(g, 20.5f, dy, 23.5f, Math.min(dy + 4, y + 18), CORD);
		// The body's line: a rose diamond and "Body", the tether at its right.
		float head = y + 21;
		HallArt.diamond(g, 17, head + 6, 10, 0xFF000000 | BODY_LINE);
		HallArt.text(g, Component.translatable(ModLang.HUD_SOUL_BODY_HEADER).getString(), 34, head, 17, 22, BODY_LINE, true);
		String tether;
		boolean strained = false;
		int radius = ClientSoul.radius();
		if (ClientSoul.bodyHere() && radius != SoulProjection.ENDLESS_TETHER) {
			double distance = from(mc).distanceTo(ClientSoul.body());
			strained = radius > 0 && distance / radius > 0.85;
			tether = Component.translatable(ModLang.HUD_SOUL_TETHER, Math.round(distance), radius).getString();
		} else {
			// From Void Shattering the tether has no end: only how far off the body is; carried into another realm in a person's body,
			// it reaches across the realms (its cost is what it is beside the body, SoulProjection#CROSS_REALM_REACH).
			tether = ClientSoul.bodyHere()
					? Component.translatable(ModLang.HUD_SOUL_TETHER_ENDLESS, Math.round(from(mc).distanceTo(ClientSoul.body()))).getString()
					: Component.translatable(ModLang.HUD_SOUL_TETHER_AWAY).getString();
		}
		HallArt.textRight(g, tether, VitalsHud.X + VitalsHud.W, head + 2, 14, 22, strained ? STRAINED : TETHER, true);
		HallArt.corner(head, head + 22, VitalsHud.X + VitalsHud.W);
		float max = ClientSoul.bodyMax();
		y = head + 25;
		bodyBar(g, y, max <= 0 ? 0 : ClientSoul.bodyHealth() / max, ModLang.HUD_SOUL_BODY, ClientSoul.bodyHealth(), max);
		y += BODY_H + 4;

		Entity host = ClientSoul.host() < 0 || mc.level == null ? null : mc.level.getEntity(ClientSoul.host());
		if (host != null) {
			line(g, y, Component.translatable(ModLang.HUD_SOUL_POSSESSING, host.getDisplayName()).getString(), CORD, TEXT);
		} else if (ClientSoul.isInside()) {
			// In a person's body (Possession): the soul's own pool has its own bar in the stack above, under the body's (QiHud).
			Component name = ClientBodies.name(mc.player);
			line(g, y, Component.translatable(ModLang.HUD_SOUL_INSIDE, name == null ? Component.literal("?") : name).getString(), CORD, TEXT);
		}
	}

	/** The y under its last row, in the artifact's pixels, so TribulationHud's lines go below it (0 with the soul at home). */
	static float bottom() {
		SoulProjection.State state = ClientSoul.state();
		if (state == SoulProjection.State.NONE) return ClientCultivationData.inClone() ? start() + 26 + BODY_H : 0;
		float y = start() + (state == SoulProjection.State.PROJECTING || state == SoulProjection.State.RECALLING ? QiHud.ROW : 0);
		if (state == SoulProjection.State.PROJECTING) return y;
		y += 21 + 25 + BODY_H;
		Minecraft mc = Minecraft.getInstance();
		boolean host = ClientSoul.host() >= 0 && mc.level != null && mc.level.getEntity(ClientSoul.host()) != null;
		return y + (host || ClientSoul.isInside() ? 4 + 22 : 0);
	}

	/** Where the soul is: the vehicle it rides, else itself. */
	private static Vec3 from(Minecraft mc) {
		return mc.player.isPassenger() && mc.player.getVehicle() != null ? mc.player.getVehicle().position() : mc.player.position();
	}

	/** A body's bar (the mockup's, 334 by 26): its health in crimson, "Body 6.23M / 6.23M" by {@code key} across it. */
	private static void bodyBar(GuiGraphics g, float y, double ratio, String key, float health, float max) {
		HallArt.bar(g, VitalsHud.X, y, VitalsHud.W, BODY_H, BODY_BORDER, ratio, HallHud.HEALTH, HallHud.HEALTH_LIGHT, BODY_BAND);
		String text = Component.translatable(key, HallHud.compact(Math.ceil(health)), HallHud.compact(Math.ceil(max))).getString();
		HallArt.textCentred(g, text, VitalsHud.X + VitalsHud.W / 2f, y + BODY_BORDER + 1, 16, 18, HallArt.WHITE, true);
		HallArt.corner(y, y + BODY_H, VitalsHud.X + VitalsHud.W);
	}

	/** A line of the stack: a diamond in {@code mark} and {@code text} at 16, outlined. */
	private static void line(GuiGraphics g, float y, String text, int mark, int color) {
		HallArt.diamond(g, 17, y + 7, 10, mark);
		HallArt.text(g, text, 34, y, 16, 22, color, true);
		HallArt.corner(y, y + 22, 34 + HallArt.width(text, 16) + 2);
	}

	private SoulHud() {}
}
