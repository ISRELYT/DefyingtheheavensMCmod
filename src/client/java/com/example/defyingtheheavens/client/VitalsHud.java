package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.SoulProjection;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.TwoPaths;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The top-left corner as the mockups' "G" draws it (the user's pick, 2026-10-30, "EXACTLY the same as the artefact at all GUI scales":
 * drawn in the artifact's pixels, HallArt, whatever the GUI scale): a line with a gold diamond, the player's name, and at its right the
 * realm they stand at (the higher of their paths), then two flat bars 334 across, the **health** and the **qi**, each saying what it
 * holds against its maximum in short numbers ("Health 5.80M / 6.23M"), so the corner never grows with a realm's might and leaves
 * vanilla's boss bars in the middle. The health crimson, a pale trail draining after a blow, absorption a band of gold along its top;
 * the qi the mod's blue, a glint flowing along it while it fills. A soul out of its body (Soul Projection) shows its own: the line in
 * violet ("Tester's soul", "Soul out"), "Soul" on its bar. Under them the stack of thinner bars goes on (QiHud, NetherworldHud).
 * Nothing of hunger, air or armour up here (the user's word: those stay at the bottom, vanilla's, its hearts held to a row of ten:
 * mixin/GuiHeartsMixin). The health shows when vanilla's hearts do (not in creative or spectator); the qi once the meridians open.
 */
public final class VitalsHud {
	/** The stack's left edge and width, in the artifact's pixels. */
	static final int X = 12, W = 334;
	/** The line over the bars (its top), the first bar's top, a bar's height, border and band, and the gap under each. */
	private static final int HEAD_TOP = 10, TOP = 36, BAR_H = 30, BORDER = 3, BAND = 5, GAP = 4;
	/** The trail a blow leaves, draining after the health; the band of absorption; the leading edge of gathering qi. */
	private static final int TRAIL = 0xFFF2B5A8, ABSORPTION = 0xFFFFD54F, QI_EDGE = 0xFFCFF0FF;
	/** The line's words: a name, the realm; a soul's name, its "Soul out" and its diamond. */
	private static final int NAME = 0xE6D3A3, REALM = 0xFFD54F, SOUL_NAME = 0xD9B8FF, SOUL_MARK = 0xB57BFF;
	/** How quickly a fill chases its value, and the trail the health, per second; one glint along the qi this often (ms). */
	private static final double EASE = 14, TRAIL_EASE = 2.5;
	private static final long GLINT_PERIOD = 2400;

	private static double health = -1, trail = -1, qi = -1;
	private static long lastFrame;

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> render(graphics));
	}

	/** Whether the health bar is drawn now (as vanilla's hearts are). */
	static boolean healthShowing() {
		Minecraft mc = Minecraft.getInstance();
		return visible() && mc.gameMode != null && mc.gameMode.canHurtPlayer();
	}

	/** Whether the qi bar is drawn now: once the meridians open (the pool there is). */
	static boolean qiShowing() {
		return visible() && !ClientCultivationData.get().isMortal();
	}

	/** Whether anything of it is drawn: a player in the world, the HUD up, no debug screen, not a spectator. */
	private static boolean visible() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.player != null && !mc.options.hideGui && !mc.options.renderDebug && !mc.player.isSpectator()
				&& mc.getCameraEntity() instanceof Player;
	}

	/** Where the stack under it goes on (QiHud#top), in the artifact's pixels: under the bars while they show, else the line's top. */
	static float bottom() {
		boolean showHealth = healthShowing(), showQi = qiShowing();
		if (!showHealth && !showQi) return HEAD_TOP;
		return TOP + (showHealth && showQi ? 2 : 1) * (BAR_H + GAP);
	}

	/** What one heart of vanilla's ten stands for in {@code player}'s health (GuiHeartsMixin): a tenth of a body past twenty, else 2. */
	public static float heartScale(Player player) {
		float max = player.getMaxHealth();
		return max > 20 ? 20 / max : 1;
	}

	/** Whether the player's soul is out of its own body (Soul Projection): the line and the health bar are the soul's. */
	static boolean soulOut() {
		SoulProjection.State state = ClientSoul.state();
		return state == SoulProjection.State.PROJECTED || state == SoulProjection.State.RECALLING || state == SoulProjection.State.INHABITING;
	}

	private static void render(GuiGraphics g) {
		boolean showHealth = healthShowing(), showQi = qiShowing();
		if (!showHealth) health = trail = -1;
		if (!showQi) qi = -1;
		if (!showHealth && !showQi) return;
		HallArt.beginHud(g);
		try {
			draw(g, showHealth, showQi);
		} finally {
			HallArt.end(g);
		}
	}

	private static void draw(GuiGraphics g, boolean showHealth, boolean showQi) {
		Minecraft mc = Minecraft.getInstance();
		Player player = (Player) mc.getCameraEntity();
		PlayerCultivation c = ClientCultivationData.get();
		long time = Util.getMillis();
		double seconds = lastFrame == 0 ? 0 : Math.min(0.25, (time - lastFrame) / 1000.0);
		lastFrame = time;
		boolean soul = soulOut();

		// The line over the bars: a diamond, the name, and at the right the realm stood at (or that the soul is out).
		HallArt.diamond(g, 17, 17, 10, 0xFF000000 | (soul ? SOUL_MARK : REALM));
		String right = soul ? Component.translatable(ModLang.HUD_SOUL_OUT).getString() : realmName(c);
		float rightW = right.isEmpty() ? 0 : HallArt.width(right, 16);
		String name = soul ? Component.translatable(ModLang.HUD_SOUL_HEADER, player.getName()).getString() : player.getName().getString();
		name = HallArt.fit(name, X + W - 34 - (rightW > 0 ? rightW + 10 : 0), 19);
		HallArt.text(g, name, 34, HEAD_TOP, 19, 22, soul ? SOUL_NAME : NAME, true);
		if (!right.isEmpty()) HallArt.textRight(g, right, X + W, HEAD_TOP + 2, 16, 22, soul ? SOUL_MARK : REALM, true);
		HallArt.corner(HEAD_TOP, HEAD_TOP + 22, right.isEmpty() ? 34 + HallArt.width(name, 19) + 2 : X + W + 2);

		float y = TOP;
		float inner = W - 2 * BORDER, ix = X + BORDER;
		if (showHealth) {
			float max = Math.max(1, player.getMaxHealth());
			float now = Mth.clamp(player.getHealth(), 0, max);
			float absorption = player.getAbsorptionAmount();
			double ratio = now / max;
			if (health < 0) {
				health = trail = ratio;
			} else {
				health += (ratio - health) * Math.min(1.0, seconds * EASE);
				trail = trail < health ? health : trail + (health - trail) * Math.min(1.0, seconds * TRAIL_EASE);
			}
			float iy = y + BORDER, bottom = y + BAR_H - BORDER;
			HallArt.box(g, X, y, W, BAR_H, BORDER, HallArt.FRAME, HallArt.TRACK);
			float fill = HallArt.filled(inner, health), trailTo = HallArt.filled(inner, trail);
			if (trailTo > fill) HallArt.rect(g, ix + fill, iy, ix + trailTo, bottom, TRAIL);
			if (fill > 0) {
				HallArt.rect(g, ix, iy, ix + fill, bottom, soul ? HallHud.SOUL : HallHud.HEALTH);
				HallArt.rect(g, ix, iy, ix + fill, iy + BAND, soul ? HallHud.SOUL_LIGHT : HallHud.HEALTH_LIGHT);
			}
			if (absorption > 0) HallArt.rect(g, ix, iy, ix + Math.max(3, HallArt.filled(inner, absorption / max)), iy + BAND, ABSORPTION);
			// Both up, so a whole body never reads over its max; what absorption adds after it, in gold.
			String text = Component.translatable(soul ? ModLang.HUD_BAR_SOUL : ModLang.HUD_BAR_HEALTH).getString() + " "
					+ HallHud.compact(Math.ceil(now)) + " / " + HallHud.compact(Math.ceil(max));
			String extra = absorption > 0 ? " +" + HallHud.compact(Math.ceil(absorption)) : "";
			float textW = HallArt.width(text, 17), from = X + W / 2f - (textW + HallArt.width(extra, 17)) / 2;
			HallArt.text(g, text, from, iy + 2, 17, 20, HallArt.WHITE, true);
			if (!extra.isEmpty()) HallArt.text(g, extra, from + textW, iy + 2, 17, 20, ABSORPTION & 0xFFFFFF, true);
			HallArt.corner(y, y + BAR_H, X + W);
			y += BAR_H + GAP;
		}
		if (showQi) {
			double ratio = c.maxQi() <= 0 ? 0 : Math.min(1.0, c.getQi() / c.maxQi());
			qi = qi < 0 ? ratio : qi + (ratio - qi) * Math.min(1.0, seconds * EASE);
			// The gathering glint shows while the pool is rising: not in a Qi Flight that costs more than gathering brings in (a
			// ghostly form's flight costs nothing: TwoPaths#fliesFreely).
			boolean draining = player.getAbilities().flying && !player.isCreative() && c.getEffectiveRealm().canFlyOnQi()
					&& !c.isQiFlightSustained() && !TwoPaths.fliesFreely(c);
			float iy = y + BORDER, bottom = y + BAR_H - BORDER;
			HallArt.box(g, X, y, W, BAR_H, BORDER, HallArt.FRAME, HallArt.TRACK);
			float fill = HallArt.filled(inner, qi);
			if (fill > 0) {
				HallArt.rect(g, ix, iy, ix + fill, bottom, HallHud.QI);
				HallArt.rect(g, ix, iy, ix + fill, iy + BAND, HallHud.QI_LIGHT);
				if (c.getQi() < c.maxQi() && !draining) {
					// The leading edge of the gathering qi, and a glint flowing toward it.
					if (fill < inner) HallArt.rect(g, ix + fill - 3, iy, ix + fill, bottom, QI_EDGE);
					float centre = ix - 9 + (fill + 18) * (time % GLINT_PERIOD) / (float) GLINT_PERIOD;
					glint(g, iy, bottom, centre - 10, centre + 11, 0x28FFFFFF, ix, ix + fill);
					glint(g, iy, bottom, centre - 4, centre + 5, 0x40FFFFFF, ix, ix + fill);
				}
			}
			String text = Component.translatable(ModLang.HUD_BAR_QI).getString() + " " + HallHud.compact(c.getQi()) + " / " + HallHud.compact(c.maxQi());
			HallArt.textCentred(g, text, X + W / 2f, iy + 2, 17, 20, HallArt.WHITE, true);
			HallArt.corner(y, y + BAR_H, X + W);
		}
	}

	/** The realm {@code c} stands at, by the higher of its paths ("" for a mortal). */
	private static String realmName(PlayerCultivation c) {
		if (c.isMortal()) return "";
		int rank = TwoPaths.higherRank(c, false);
		return rank < 0 ? "" : Realm.byIndex(rank / Stage.values().length).getDisplayName().getString();
	}

	/** Lightens the fill between x1 and x2, clipped to the filled part [clipLeft, clipRight). */
	private static void glint(GuiGraphics g, float top, float bottom, float x1, float x2, int color, float clipLeft, float clipRight) {
		float from = Math.max(x1, clipLeft), to = Math.min(x2, clipRight);
		if (to > from) HallArt.rect(g, from, top, to, bottom, color);
	}

	private VitalsHud() {}
}
