package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.MeditationManager;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * The Small Heavenly Circuit: while meditating, a ring of acupoints circles the crosshair and a bead of qi travels round it.
 * Press the Circulate key as the bead reaches the lit point; light every point in turn for a clean circuit, which speeds
 * meditation (the server keeps the real bonus, see MeditationManager#onCirculation). Letting the bead pass a point breaks
 * the circuit; three wrong presses in a row are a qi deviation. Ignoring the ring costs nothing.
 */
public final class QiCirculationHud {
	private static final int RADIUS = 28;
	/** Half the hit window, as a share of the gap between two points. */
	private static final double WINDOW = 0.3;
	/** Two points passed without a press: the player has stopped playing, so the game goes quiet again. */
	private static final int IDLE_AFTER_PASSES = 2;
	private static final int WRONG_FOR_DEVIATION = 3;

	private static boolean active;
	private static boolean engaged;
	private static long startMs;
	private static int target;
	private static int hits;
	private static int passes;
	private static int wrong;
	private static int streak;
	private static long lastCircuitMs;

	/** More acupoints at higher realms: 6 at Qi Refining, up to 10. */
	private static int points(PlayerCultivation c) { return 6 + Math.min(4, c.getRealm().ordinal()); }

	/** A faster bead at higher realms: 4 s a lap at Qi Refining, down to 2 s. */
	private static long periodMs(PlayerCultivation c) { return Math.max(2000, 4000 - 400L * c.getRealm().ordinal()); }

	public static void register() {
		HudRenderCallback.EVENT.register((g, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			PlayerCultivation c = ClientCultivationData.get();
			boolean meditating = mc.player != null && ClientCultivationData.isMeditating() && !c.isMortal() && mc.screen == null;
			if (!meditating) {
				if (active) reset();
				while (ModKeybinds.CIRCULATE.consumeClick()) { } // presses outside meditation do nothing
				return;
			}
			long now = Util.getMillis();
			if (!active) {
				active = true;
				startMs = now;
			}
			if (now - lastCircuitMs > MeditationManager.CIRCULATION_LASTS * 50L) streak = 0;
			int n = points(c);
			double phase = ((now - startMs) % periodMs(c)) / (double) periodMs(c);
			double window = WINDOW / n;

			if (engaged && offset(phase, target, n) > window) { // the bead slipped past the lit point
				if (hits > 0 || streak > 0) send(MeditationManager.CIRCUIT_BROKEN);
				hits = 0;
				streak = 0;
				target = (target + 1) % n;
				if (++passes >= IDLE_AFTER_PASSES) engaged = false;
			}
			while (ModKeybinds.CIRCULATE.consumeClick()) press(mc, phase, n, window, now);

			if (!mc.options.hideGui) render(g, mc, phase, n);
		});
	}

	private static void press(Minecraft mc, double phase, int n, double window, long now) {
		if (!engaged) {
			engaged = true;
			hits = 0;
			wrong = 0;
			target = nextAhead(phase, n, window);
		}
		passes = 0;
		if (Math.abs(offset(phase, target, n)) <= window) {
			hits++;
			wrong = 0;
			target = (target + 1) % n;
			if (hits >= n) {
				hits = 0;
				streak = Math.min(streak + 1, MeditationManager.MAX_STREAK);
				lastCircuitMs = now;
				send(MeditationManager.CIRCUIT_CLEAN);
				sound(mc, 1.6f, true);
			} else {
				sound(mc, 0.8f + 0.8f * hits / n, false);
			}
			return;
		}
		if (hits > 0 || streak > 0) send(MeditationManager.CIRCUIT_BROKEN);
		hits = 0;
		streak = 0;
		sound(mc, 0.5f, false);
		if (++wrong >= WRONG_FOR_DEVIATION) {
			send(MeditationManager.CIRCUIT_DEVIATION);
			wrong = 0;
			engaged = false;
		}
	}

	/** Where the bead is relative to point {@code index}, as a share of a lap: negative before it, positive past it. */
	private static double offset(double phase, int index, int n) {
		double d = phase - index / (double) n;
		return d - Math.floor(d + 0.5);
	}

	/** The first point the bead hasn't yet reached the window of. */
	private static int nextAhead(double phase, int n, double window) {
		for (int i = 0; i < n; i++) {
			if (offset(phase, i, n) < -window) {
				int best = i;
				for (int j = 0; j < n; j++) {
					double o = offset(phase, j, n);
					if (o < -window && o > offset(phase, best, n)) best = j;
				}
				return best;
			}
		}
		return 0;
	}

	private static void send(int outcome) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(outcome);
		ClientPlayNetworking.send(ModPackets.CIRCULATION, buf);
	}

	private static void sound(Minecraft mc, float pitch, boolean circuit) {
		mc.getSoundManager().play(SimpleSoundInstance.forUI(
				(circuit ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.NOTE_BLOCK_CHIME.value()), pitch, circuit ? 0.7f : 0.5f));
	}

	private static void render(GuiGraphics g, Minecraft mc, double phase, int n) {
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int cy = mc.getWindow().getGuiScaledHeight() / 2;
		int alpha = engaged ? 0xFF : 0x70; // faint until played
		// The meridian: a dotted ring.
		for (int i = 0; i < 48; i++) {
			double a = angle(i / 48.0);
			dot(g, cx + (int) Math.round(Math.cos(a) * RADIUS), cy + (int) Math.round(Math.sin(a) * RADIUS), 0, (alpha / 3) << 24 | 0x6FA8C8);
		}
		// Acupoints: lit (gold, pulsing) is next, jade ones are done this circuit, the rest grey.
		long now = Util.getMillis();
		for (int i = 0; i < n; i++) {
			double a = angle(i / (double) n);
			int x = cx + (int) Math.round(Math.cos(a) * RADIUS);
			int y = cy + (int) Math.round(Math.sin(a) * RADIUS);
			int back = (target - i + n) % n;
			int colour;
			if (engaged && i == target) colour = (now / 200) % 2 == 0 ? 0xFFD54F : 0xFFB300;
			else if (engaged && back >= 1 && back <= hits) colour = 0x69F0AE;
			else colour = 0x9E9E9E;
			dot(g, x, y, 1, alpha << 24 | colour);
		}
		// The bead and a short trail.
		for (int t = 3; t >= 0; t--) {
			double a = angle(phase - t * 0.012);
			int x = cx + (int) Math.round(Math.cos(a) * RADIUS);
			int y = cy + (int) Math.round(Math.sin(a) * RADIUS);
			int fade = t == 0 ? alpha : alpha * (4 - t) / 6;
			dot(g, x, y, t == 0 ? 1 : 0, fade << 24 | (t == 0 ? 0xE0F7FF : 0x80DEEA));
		}
		Component line = streak > 0
				? Component.translatable(ModLang.CIRCULATION_STREAK, streak == 1 ? "1.5" : "2")
				: engaged ? null : Component.translatable(ModLang.CIRCULATION_HINT, ModKeybinds.CIRCULATE.getTranslatedKeyMessage());
		if (line != null) g.drawCenteredString(mc.font, line, cx, cy + RADIUS + 8, streak > 0 ? 0x69F0AE : 0x9E9E9E);
	}

	/** Lap share to screen angle, starting at the top and running clockwise. */
	private static double angle(double share) { return share * Math.PI * 2 - Math.PI / 2; }

	private static void dot(GuiGraphics g, int x, int y, int r, int argb) {
		g.fill(x - r, y - r, x + r + 1, y + r + 1, argb);
	}

	private static void reset() {
		active = engaged = false;
		hits = passes = wrong = 0;
	}

	private QiCirculationHud() {}
}
