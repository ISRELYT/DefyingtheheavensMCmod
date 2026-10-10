package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.MeditationManager;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.QiSurges;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * The screen games of a qi surge (see QiSurges), drawn round the crosshair only while a surge lasts. Each is played with the
 * Circulate key (R) and reports its result to the server, which keeps the real reward (MeditationManager#onCirculation):
 * <ul>
 *   <li><b>Small Heavenly Circuit:</b> a bead of qi travels a ring of acupoints; tap as it meets each lit point, all the
 *   way round.</li>
 *   <li><b>Five Elements:</b> elements flash in the centre; tap when the one the current element generates appears (wood,
 *   fire, earth, metal, water), five times round the cycle.</li>
 *   <li><b>Breath rhythm:</b> a ring swells and shrinks; hold the key as it swells and let go as it shrinks, for three
 *   breaths.</li>
 * </ul>
 * Three wrong taps in the Circuit or the Elements force the qi astray: a qi deviation. Ignoring a surge costs nothing.
 */
public final class QiSurgeHud {
	private static final int RADIUS = 28;
	/** Once started, a game must be finished within this long (ms). */
	private static final long PLAY_LIMIT_MS = 20_000;
	/** How long the game lingers, fading, after it ends (ms). */
	private static final long FADE_MS = 700;
	private static final int WRONG_FOR_DEVIATION = 3;

	// Five Elements, in generating order: wood feeds fire, fire makes earth, earth bears metal, metal carries water...
	private static final String[] ELEMENT = {"木", "火", "土", "金", "水"};
	private static final int[] ELEMENT_COLOUR = {0x66BB6A, 0xEF5350, 0xD4A055, 0xF5E6A8, 0x4FC3F7};

	private static QiSurges.Game game;
	private static long startMs, windowMs, engagedMs, endedMs = -1;
	private static boolean engaged, won;
	private static int wrong;
	private static final Random RANDOM = new Random();

	// Circuit
	private static int target, hits;
	// Elements
	private static int current, done, shown, sinceTarget;
	private static long shownAt;
	// Breath
	private static double inRhythm, judged;
	private static long lastFrameMs;

	/** The server's word: a surge starts ({@code index} a Game), or ends (-1). */
	public static void start(int index, int windowTicks) {
		QiSurges.Game g = QiSurges.Game.byIndex(index);
		if (g == null || !g.screen) {
			if (game != null && endedMs < 0) endedMs = Util.getMillis();
			return;
		}
		game = g;
		startMs = Util.getMillis();
		windowMs = windowTicks * 50L;
		engaged = won = false;
		endedMs = -1;
		wrong = hits = target = done = 0;
		current = RANDOM.nextInt(5);
		shown = (current + 2) % 5;
		shownAt = startMs;
		sinceTarget = 1;
		inRhythm = judged = 0;
		lastFrameMs = startMs;
		Minecraft mc = Minecraft.getInstance();
		sound(mc, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 0.6f);
	}

	public static void register() {
		HudRenderCallback.EVENT.register((g, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			PlayerCultivation c = ClientCultivationData.get();
			boolean meditating = mc.player != null && ClientCultivationData.isMeditating() && !c.isMortal();
			if (!meditating || game == null) {
				game = null;
				while (ModKeybinds.CIRCULATE.consumeClick()) { } // presses outside a surge do nothing
				return;
			}
			long now = Util.getMillis();
			float fade = 1;
			if (endedMs >= 0) {
				while (ModKeybinds.CIRCULATE.consumeClick()) { }
				fade = 1 - (now - endedMs) / (float) FADE_MS;
				if (fade <= 0) {
					game = null;
					return;
				}
			} else if (mc.screen == null) {
				if (!engaged && now - startMs > windowMs) {
					finish(MeditationManager.CIRCUIT_IGNORED, false);
				} else if (engaged && now - engagedMs > PLAY_LIMIT_MS) {
					finish(MeditationManager.CIRCUIT_BROKEN, false);
				} else {
					switch (game) {
						case CIRCUIT -> updateCircuit(mc, c, now);
						case ELEMENTS -> updateElements(mc, c, now);
						case BREATH -> updateBreath(mc, now);
						default -> { }
					}
				}
			}
			if (mc.options.hideGui || mc.screen != null) return;
			// Faint until played, then clear; fading as it goes.
			float alpha = (engaged || endedMs >= 0 ? 1.0f : 0.55f) * Mth.clamp((now - startMs) / 300f, 0, 1) * fade;
			int cx = mc.getWindow().getGuiScaledWidth() / 2;
			int cy = mc.getWindow().getGuiScaledHeight() / 2;
			switch (game) {
				case CIRCUIT -> renderCircuit(g, c, now, cx, cy, alpha);
				case ELEMENTS -> renderElements(g, mc, cx, cy, alpha);
				case BREATH -> renderBreath(g, mc, now, cx, cy, alpha);
				default -> { }
			}
			renderFooter(g, mc, now, cx, cy, alpha);
		});
	}

	private static void engage(long now) {
		if (engaged) return;
		engaged = true;
		engagedMs = now;
	}

	private static void finish(int outcome, boolean success) {
		if (endedMs >= 0) return;
		endedMs = Util.getMillis();
		won = success;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(outcome);
		ClientPlayNetworking.send(ModPackets.CIRCULATION, buf);
		if (success) sound(Minecraft.getInstance(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 0.8f);
	}

	/** A wrong tap: three in a row force the qi astray. */
	private static void mistake(Minecraft mc) {
		sound(mc, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 0.5f);
		if (++wrong >= WRONG_FOR_DEVIATION) finish(MeditationManager.CIRCUIT_DEVIATION, false);
	}

	// --- Small Heavenly Circuit ---

	/** More acupoints at higher realms: 6 at Qi Refining, up to 10. */
	private static int points(PlayerCultivation c) { return 6 + Math.min(4, c.getRealm().ordinal()); }

	/** A faster bead at higher realms: 4 s a lap at Qi Refining, down to 2 s. */
	private static long periodMs(PlayerCultivation c) { return Math.max(2000, 4000 - 400L * c.getRealm().ordinal()); }

	private static double circuitPhase(PlayerCultivation c, long now) {
		return ((now - startMs) % periodMs(c)) / (double) periodMs(c);
	}

	private static void updateCircuit(Minecraft mc, PlayerCultivation c, long now) {
		int n = points(c);
		double phase = circuitPhase(c, now);
		double window = 0.3 / n;
		if (engaged && offset(phase, target, n) > window) { // the bead slipped past the lit point: start the round again
			hits = 0;
			target = (target + 1) % n;
		}
		while (ModKeybinds.CIRCULATE.consumeClick() && endedMs < 0) {
			if (!engaged) {
				engage(now);
				target = nextAhead(phase, n, window);
			}
			if (Math.abs(offset(phase, target, n)) <= window) {
				hits++;
				wrong = 0;
				target = (target + 1) % n;
				if (hits >= n) finish(MeditationManager.CIRCUIT_CLEAN, true);
				else sound(mc, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f + 0.8f * hits / n, 0.5f);
			} else {
				hits = 0;
				mistake(mc);
			}
		}
	}

	private static void renderCircuit(GuiGraphics g, PlayerCultivation c, long now, int cx, int cy, float alpha) {
		int n = points(c);
		double phase = circuitPhase(c, now);
		int a = Math.round(alpha * 255);
		for (int i = 0; i < 48; i++) {
			double ang = angle(i / 48.0);
			dot(g, cx + (int) Math.round(Math.cos(ang) * RADIUS), cy + (int) Math.round(Math.sin(ang) * RADIUS), 0, (a / 3) << 24 | 0x6FA8C8);
		}
		for (int i = 0; i < n; i++) {
			double ang = angle(i / (double) n);
			int x = cx + (int) Math.round(Math.cos(ang) * RADIUS);
			int y = cy + (int) Math.round(Math.sin(ang) * RADIUS);
			int back = (target - i + n) % n;
			int colour;
			if (won) colour = 0x69F0AE;
			else if (engaged && i == target) colour = (now / 200) % 2 == 0 ? 0xFFD54F : 0xFFB300;
			else if (engaged && back >= 1 && back <= hits) colour = 0x69F0AE;
			else colour = 0x9E9E9E;
			dot(g, x, y, 1, a << 24 | colour);
		}
		for (int t = 3; t >= 0; t--) {
			double ang = angle(phase - t * 0.012);
			int x = cx + (int) Math.round(Math.cos(ang) * RADIUS);
			int y = cy + (int) Math.round(Math.sin(ang) * RADIUS);
			int fade = t == 0 ? a : a * (4 - t) / 6;
			dot(g, x, y, t == 0 ? 1 : 0, fade << 24 | (t == 0 ? 0xE0F7FF : 0x80DEEA));
		}
	}

	/** Where the bead is relative to point {@code index}, as a share of a lap: negative before it, positive past it. */
	private static double offset(double phase, int index, int n) {
		double d = phase - index / (double) n;
		return d - Math.floor(d + 0.5);
	}

	/** The nearest point the bead hasn't yet reached the window of. */
	private static int nextAhead(double phase, int n, double window) {
		int best = -1;
		for (int j = 0; j < n; j++) {
			double o = offset(phase, j, n);
			if (o < -window && (best < 0 || o > offset(phase, best, n))) best = j;
		}
		return Math.max(best, 0);
	}

	// --- Five Elements ---

	/** Faster flashes at higher realms. */
	private static long flashMs(PlayerCultivation c) { return Math.max(550, 950 - 70L * c.getRealm().ordinal()); }

	private static void updateElements(Minecraft mc, PlayerCultivation c, long now) {
		int wanted = (current + 1) % 5;
		if (now - shownAt >= flashMs(c)) nextElement(now, wanted);
		while (ModKeybinds.CIRCULATE.consumeClick() && endedMs < 0) {
			engage(now);
			if (shown == wanted) {
				current = wanted;
				done++;
				wrong = 0;
				if (done >= 5) finish(MeditationManager.CIRCUIT_CLEAN, true);
				else sound(mc, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f + 0.2f * done, 0.5f);
				wanted = (current + 1) % 5;
				nextElement(now, wanted);
			} else {
				mistake(mc);
			}
		}
	}

	/** Shows another element: often the wanted one, never the same twice, and the wanted one at least every third flash. */
	private static void nextElement(long now, int wanted) {
		int next;
		if (sinceTarget >= 2 || RANDOM.nextFloat() < 0.35f) next = wanted;
		else do next = RANDOM.nextInt(5); while (next == shown || next == wanted);
		if (next == shown) next = (next + 2) % 5; // a fresh flash, so a held rhythm can't just repeat
		sinceTarget = next == wanted ? 0 : sinceTarget + 1;
		shown = next;
		shownAt = now;
	}

	private static void renderElements(GuiGraphics g, Minecraft mc, int cx, int cy, float alpha) {
		int a = Math.round(alpha * 255);
		// The cycle round the crosshair: the current element bright, those already passed in colour, the rest grey.
		for (int i = 0; i < 5; i++) {
			double ang = angle(i / 5.0);
			int x = cx + (int) Math.round(Math.cos(ang) * RADIUS);
			int y = cy + (int) Math.round(Math.sin(ang) * RADIUS);
			boolean lit = (current - i + 5) % 5 <= done; // the current element and those passed through this time
			int colour = won || lit ? ELEMENT_COLOUR[i] : 0x808080;
			int glyphAlpha = i == current ? a : a * 3 / 4;
			g.drawString(mc.font, ELEMENT[i], x - mc.font.width(ELEMENT[i]) / 2, y - 4, glyphAlpha << 24 | colour, false);
		}
		if (won) return;
		// The flash in the centre, large.
		g.pose().pushPose();
		g.pose().translate(cx, cy, 0);
		g.pose().scale(2.0f, 2.0f, 1.0f);
		g.drawString(mc.font, ELEMENT[shown], -mc.font.width(ELEMENT[shown]) / 2, -4, Math.max(a, 4) << 24 | ELEMENT_COLOUR[shown], false);
		g.pose().popPose();
	}

	// --- Breath rhythm ---

	private static final long BREATH_MS = 5000;
	private static final int BREATHS = 3;
	/** Near the turn of each breath, either is fine (ms). */
	private static final long GRACE_MS = 300;

	private static void updateBreath(Minecraft mc, long now) {
		while (ModKeybinds.CIRCULATE.consumeClick()) { }
		boolean down = ModKeybinds.CIRCULATE.isDown();
		if (down) engage(now);
		long dt = Math.min(100, now - lastFrameMs);
		lastFrameMs = now;
		if (!engaged) return;
		long t = (now - startMs) % BREATH_MS;
		boolean inhaling = t < BREATH_MS / 2;
		long fromTurn = Math.min(t % (BREATH_MS / 2), BREATH_MS / 2 - t % (BREATH_MS / 2));
		if (fromTurn > GRACE_MS) {
			judged += dt;
			if (down == inhaling) inRhythm += dt;
		}
		if (now - engagedMs >= BREATH_MS * BREATHS) {
			boolean good = judged > 0 && inRhythm / judged >= 0.7;
			finish(good ? MeditationManager.CIRCUIT_CLEAN : MeditationManager.CIRCUIT_BROKEN, good);
		}
	}

	private static void renderBreath(GuiGraphics g, Minecraft mc, long now, int cx, int cy, float alpha) {
		long t = (now - startMs) % BREATH_MS;
		float half = BREATH_MS / 2f;
		float f = t < half ? t / half : 1 - (t - half) / half;
		f = f * f * (3 - 2 * f); // ease in and out
		float r = 10 + 20 * f;
		int a = Math.round(alpha * 255);
		boolean inhaling = t < half;
		boolean inTime = !engaged || ModKeybinds.CIRCULATE.isDown() == inhaling;
		int colour = won ? 0x69F0AE : engaged ? (inTime ? 0x80DEEA : 0x9E9E9E) : 0x9E9E9E;
		for (int i = 0; i < 40; i++) {
			double ang = angle(i / 40.0);
			dot(g, cx + (int) Math.round(Math.cos(ang) * r), cy + (int) Math.round(Math.sin(ang) * r), i % 5 == 0 ? 1 : 0, a << 24 | colour);
		}
		if (engaged && judged > 0 && endedMs < 0) {
			int percent = (int) Math.round(100 * inRhythm / judged);
			Component line = Component.translatable(ModLang.SURGE_SYNC, percent);
			g.drawCenteredString(mc.font, line, cx, cy + RADIUS + 20, a << 24 | (percent >= 70 ? 0x69F0AE : 0xBDBDBD));
		}
	}

	// --- Shared ---

	/** The hint (until played) and a thin bar for the time left. */
	private static void renderFooter(GuiGraphics g, Minecraft mc, long now, int cx, int cy, float alpha) {
		if (endedMs >= 0) return;
		int a = Math.round(alpha * 255);
		if (a < 8) return;
		if (!engaged) {
			String key = switch (game) {
				case ELEMENTS -> ModLang.SURGE_ELEMENTS;
				case BREATH -> ModLang.SURGE_BREATH;
				default -> ModLang.SURGE_CIRCUIT;
			};
			g.drawCenteredString(mc.font, Component.translatable(key, ModKeybinds.CIRCULATE.getTranslatedKeyMessage()), cx, cy + RADIUS + 8,
					a << 24 | 0xBDBDBD);
		}
		float left = engaged ? 1 - (now - engagedMs) / (float) (game == QiSurges.Game.BREATH ? BREATH_MS * BREATHS : PLAY_LIMIT_MS)
				: 1 - (now - startMs) / (float) windowMs;
		int w = Math.round(40 * Mth.clamp(left, 0, 1));
		g.fill(cx - w / 2, cy + RADIUS + 4, cx - w / 2 + w, cy + RADIUS + 5, (a / 2) << 24 | 0x80DEEA);
	}

	private static void sound(Minecraft mc, SoundEvent event, float pitch, float volume) {
		mc.getSoundManager().play(SimpleSoundInstance.forUI(event, pitch, volume));
	}

	/** Lap share to screen angle, starting at the top and running clockwise. */
	private static double angle(double share) { return share * Math.PI * 2 - Math.PI / 2; }

	private static void dot(GuiGraphics g, int x, int y, int r, int argb) {
		g.fill(x - r, y - r, x + r + 1, y + r + 1, argb);
	}

	public static void clear() {
		game = null;
	}

	private QiSurgeHud() {}
}
