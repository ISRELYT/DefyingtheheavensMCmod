package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.SoulRiver;
import com.example.defyingtheheavens.SoulRiverCourse;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * The local player on the Soul River (see SoulRiver): the current carries them along at {@link SoulRiverCourse#FLOW} while
 * they steer across it ({@link #carry}, from SoulRiverMixin in place of walking or falling), and the HUD shows the time left
 * and how near the edge of the flow they are, with a warning once they're past it.
 */
public final class ClientSoulRiver {
	/** How much of the last tick's sideways motion carries into this one: steering eases in and out. */
	private static final double DAMPING = 0.6;
	/** The edge warning starts this far out across the river (a share of its reach). */
	private static final double WARN_FROM = 0.7;
	private static final int GOLD = 0xFFB8860B;
	private static final int GOLD_DARK = 0xFF6E5214;
	private static final int LACQUER = 0xD0101420;
	private static final int TEXT = 0xE6D3A3;

	private static SoulRiver.Route route = SoulRiver.Route.NONE;
	/** Ticks into the journey, counted here from the server's start. */
	private static int tick;
	private static Vec3 velocity = Vec3.ZERO;
	/** How far across the river the player is, as a share of its reach (1 is the edge), as of the last tick. */
	private static double across;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!isOn()) return;
			tick++;
			LocalPlayer player = client.player;
			if (player == null) return;
			double distance = SoulRiverCourse.distanceFromCentre(player.position().add(0, player.getBbHeight() / 2, 0));
			across = distance / SoulRiverCourse.radius(SoulRiverCourse.progress(player.getX(), ascending()));
		});
		HudRenderCallback.EVENT.register(ClientSoulRiver::renderHud);
	}

	static void set(SoulRiver.Route newRoute, int atTick) {
		route = newRoute;
		tick = atTick;
		velocity = Vec3.ZERO;
		across = 0;
	}

	/** On the river, and still in its dimension (the packet ending the journey may trail the move off it). */
	public static boolean isOn() {
		Minecraft mc = Minecraft.getInstance();
		return route != SoulRiver.Route.NONE && mc.level != null && ModDimensions.isSoulRiver(mc.level.dimension());
	}

	static boolean ascending() {
		return route != SoulRiver.Route.DESCEND;
	}

	/** +1 when the river flows along +X for this player (going up, or watching it from anywhere else), -1 going down. */
	static int flow() {
		return isOn() ? SoulRiverCourse.direction(ascending()) : 1;
	}

	static int tick() {
		return tick;
	}

	/**
	 * One tick of travel on the river: carried along X at the current's pace whatever they do, pushed by the crosscurrent, and
	 * steering across it toward where they look (forward), sideways (strafe) and up or down (jump, sneak); steering never
	 * speeds or slows them along the river.
	 */
	public static void carry(LocalPlayer player) {
		float forward = player.input.forwardImpulse, strafe = player.input.leftImpulse;
		float up = (player.input.jumping ? 1 : 0) - (player.input.shiftKeyDown ? 1 : 0);
		Vec3 look = player.getViewVector(1.0f);
		float yaw = player.getYRot() * Mth.DEG_TO_RAD;
		Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		Vec3 wish = look.scale(forward).add(left.scale(strafe)).add(0, up, 0);
		wish = new Vec3(0, wish.y, wish.z); // across the river only
		if (wish.lengthSqr() > 1) wish = wish.normalize();
		Vec3 across = wish.scale(SoulRiverCourse.STEER).add(SoulRiverCourse.crosscurrent(tick));
		velocity = new Vec3(SoulRiverCourse.direction(ascending()) * SoulRiverCourse.FLOW,
				velocity.y * DAMPING + across.y * (1 - DAMPING), velocity.z * DAMPING + across.z * (1 - DAMPING));
		player.setDeltaMovement(velocity);
		player.move(MoverType.SELF, velocity);
		player.fallDistance = 0;
		player.setOnGround(false);
		player.calculateEntityAnimation(false);
	}

	/**
	 * Top centre: the river's name and the seconds left on a small lacquer plate. Nearing the edge of the flow, a soft glow
	 * gathers at the screen's sides, cold at first, red once past it; past the edge, a warning flashes in the middle of the
	 * screen.
	 */
	private static void renderHud(GuiGraphics g, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();
		if (!isOn() || mc.player == null || mc.options.hideGui) return;
		int w = g.guiWidth();
		boolean outside = across > 1;
		if (across > WARN_FROM) edgeGlow(g, (float) Mth.clamp((across - WARN_FROM) / (1.3 - WARN_FROM), 0, 1), outside);

		int seconds = Math.max(0, (SoulRiverCourse.TICKS - tick + 19) / 20);
		Component title = Component.translatable(ModLang.HUD_RIVER, seconds);
		int textW = mc.font.width(title);
		int plateW = textW + 16;
		int x0 = (w - plateW) / 2, y0 = 6;
		g.fill(x0, y0, x0 + plateW, y0 + 16, LACQUER);
		g.fill(x0, y0, x0 + plateW, y0 + 1, GOLD);
		g.fill(x0, y0 + 15, x0 + plateW, y0 + 16, GOLD_DARK);
		g.fill(x0, y0, x0 + 1, y0 + 16, GOLD);
		g.fill(x0 + plateW - 1, y0, x0 + plateW, y0 + 16, GOLD);
		g.drawString(mc.font, title, (w - textW) / 2, y0 + 4, TEXT);

		if (outside && (Util.getMillis() / 250) % 2 == 0) {
			Component warning = Component.translatable(ModLang.HUD_RIVER_STRAY);
			g.pose().pushPose();
			g.pose().translate(w / 2.0, g.guiHeight() / 2.0 - 30, 0);
			g.pose().scale(1.5f, 1.5f, 1);
			g.drawString(mc.font, warning, -mc.font.width(warning) / 2, 0, 0xFFFF6A50);
			g.pose().popPose();
		}
	}

	/** A soft glow in from the screen's edges, {@code strength} 0 to 1: cyan nearing the edge of the flow, red past it. */
	private static void edgeGlow(GuiGraphics g, float strength, boolean outside) {
		int w = g.guiWidth(), h = g.guiHeight();
		int rgb = outside ? 0xD03020 : 0x60D8F0;
		float peak = (outside ? 0.45f : 0.28f) * strength;
		int reach = (int) (Math.min(w, h) * (0.08f + 0.12f * strength));
		int strips = 20;
		for (int i = 0; i < strips; i++) {
			float fall = 1 - i / (float) strips;
			int color = ((int) (peak * fall * fall * 255) << 24) | rgb;
			int a = reach * i / strips, z = reach * (i + 1) / strips;
			g.fill(a, 0, z, h, color);
			g.fill(w - z, 0, w - a, h, color);
			g.fill(z, a, w - z, z, color);
			g.fill(z, h - z, w - z, h - a, color);
		}
	}

	private ClientSoulRiver() {}
}
