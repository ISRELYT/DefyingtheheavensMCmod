package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModDimensions;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.util.Mth;

/**
 * Closing the eyes: before the soul turns inward the screen fades to black (the server says when, see
 * InnerRealm#FADE_TICKS), stays black through the dimension change (ReceivingLevelScreenMixin keeps the loading screen
 * black too), then opens onto the island. Coming back, the eyes open again from black.
 */
public final class InnerRealmFade {
	private enum Phase { IDLE, CLOSING, DARK, OPENING }

	private static final int OPEN_TICKS = 30;
	private static final int RETURN_OPEN_TICKS = 16;
	/** On arrival, the island gets this long to load before the eyes open. */
	private static final int SETTLE_TICKS = 8;
	/** Never stays black longer than this (ticks), whatever happens. */
	private static final int MAX_DARK_TICKS = 120;

	private static Phase phase = Phase.IDLE;
	private static float alpha, previous;
	private static int closeTicks = 30, openTicks = OPEN_TICKS, timer, settle;
	private static boolean wasInner;

	/** The server's cue: the soul turns inward in {@code ticks}. */
	public static void begin(int ticks) {
		phase = Phase.CLOSING;
		closeTicks = Math.max(1, ticks);
		timer = 0;
	}

	public static void register() {
		HudRenderCallback.EVENT.register((g, tickDelta) -> {
			float a = Mth.lerp(tickDelta, previous, alpha);
			if (a <= 0.003f) return;
			Minecraft mc = Minecraft.getInstance();
			int shade = Math.round(Mth.clamp(a, 0, 1) * 255);
			g.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), shade << 24);
		});
	}

	public static void tick(Minecraft mc) {
		previous = alpha;
		boolean inner = mc.level != null && ModDimensions.isInnerRealm(mc.level.dimension());
		if (wasInner && !inner && mc.level != null) { // back in the body: the eyes open
			phase = Phase.OPENING;
			openTicks = RETURN_OPEN_TICKS;
			alpha = previous = 1;
		}
		wasInner = inner;
		boolean loading = mc.screen instanceof ReceivingLevelScreen;
		switch (phase) {
			case CLOSING -> {
				alpha = Math.min(1, alpha + 1.0f / closeTicks);
				timer++;
				if (!ClientCultivationData.isMeditating() && !inner) { // got up before the soul went
					phase = Phase.OPENING;
					openTicks = 10;
				} else if (alpha >= 1) {
					phase = Phase.DARK;
					timer = settle = 0;
				}
			}
			case DARK -> {
				alpha = 1;
				timer++;
				settle = inner && !loading ? settle + 1 : 0;
				boolean gaveUp = !inner && !loading && !ClientCultivationData.isMeditating();
				if (settle >= SETTLE_TICKS || gaveUp || timer > MAX_DARK_TICKS) {
					phase = Phase.OPENING;
					openTicks = OPEN_TICKS;
				}
			}
			case OPENING -> {
				if (loading) break; // the new world is still coming in: stay closed
				alpha = Math.max(0, alpha - 1.0f / openTicks);
				if (alpha <= 0) phase = Phase.IDLE;
			}
			default -> alpha = 0;
		}
	}

	/** True while the screen should be fully black (the loading screen included). */
	public static boolean isDark() {
		return phase == Phase.DARK || (phase == Phase.CLOSING && alpha >= 0.99f) || (phase == Phase.OPENING && alpha >= 0.99f);
	}

	public static void clear() {
		phase = Phase.IDLE;
		alpha = previous = 0;
		wasInner = false;
	}

	private InnerRealmFade() {}
}
