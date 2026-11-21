package com.example.defyingtheheavens.client.cinematic;

import com.example.defyingtheheavens.BodyPath;
import com.example.defyingtheheavens.DemonPath;
import com.example.defyingtheheavens.GhostMethods;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The breakthrough player's screen while their cinematic plays (the brief §6.2, §6.10): the HUD gone (Gui#render cancelled at its head,
 * GuiMixin), and in its place the overlays the sequence names (the storm's dark, the blink, the white-out, the colour drain: stand-ins
 * for step B2's post effects), the 2.35:1 letterbox sliding in and out, the realm's title near the end, and the skip ring (hold Sneak).
 */
public final class CinematicHud {
	private static final int WARM_GLOW = 0xFFFFE6B8;

	public static void render(GuiGraphics g, float partial) {
		CinematicClient.Playing p = CinematicClient.own();
		if (p == null) return;
		Minecraft mc = Minecraft.getInstance();
		Sequence seq = p.seq;
		int w = g.guiWidth(), h = g.guiHeight();
		float t = p.elapsed(partial);
		for (Sequence.Overlay o : seq.overlays) {
			if (t < o.from() || t >= o.to()) continue;
			float lt = t - o.from();
			switch (o.kind()) {
				case "storm" -> {
					float a = o.alpha() * Math.min(Ease.SMOOTH.between(lt, 0, 15), 1 - Ease.SMOOTH.between(t, o.to() - 20, o.to()));
					g.fill(0, 0, w, h, argb(o.color(), a));
				}
				case "blink" -> { // down, held, up
					int[] k = o.times();
					float a = lt < k[0] ? lt / k[0] : lt < k[0] + k[1] ? 1 : 1 - (lt - k[0] - k[1]) / k[2];
					g.fill(0, 0, w, h, argb(0xFF000000, Mth.clamp(a, 0, 1)));
				}
				case "whiteout" -> { // up, held, then cleared from the bottom up; reduced flashes: a warm glow at 60% instead
					int[] k = o.times();
					boolean reduced = CinematicSettings.reducedFlashes;
					int colour = reduced ? WARM_GLOW : o.color();
					float peak = reduced ? 0.6f : 1;
					if (lt < k[0] + k[1]) {
						g.fill(0, 0, w, h, argb(colour, peak * Mth.clamp(lt / k[0], 0, 1)));
					} else {
						float c = Mth.clamp((lt - k[0] - k[1]) / k[2], 0, 1);
						int edge = Math.round(h * (1 - Ease.CUBIC_IN.apply(c)));
						g.fill(0, 0, w, edge, argb(colour, peak * (1 - c * 0.3f)));
					}
				}
				case "drain" -> {
					float a = o.alpha() * Math.min(Ease.SMOOTH.between(lt, 0, 24), 1 - Ease.SMOOTH.between(t, o.to() - 20, o.to()));
					g.fill(0, 0, w, h, argb(o.color(), a));
				}
				default -> { }
			}
		}
		// The letterbox: 2.35:1, sliding in at the start and out at the end.
		float full = Math.max(0, (h - w / 2.35f) / 2f);
		float in = Ease.CUBIC_OUT.between(t, 0, seq.letterbox), out = 1 - Ease.CUBIC_IN.between(t, p.length - seq.letterbox, p.length);
		int bar = Math.round(full * Math.min(in, out));
		g.fill(0, 0, w, bar, 0xFF000000);
		g.fill(0, h - bar, w, h, 0xFF000000);
		// The title: the realm reached, by the path's own names, and the stage under it.
		if (t >= seq.titleFrom && t < seq.titleTo) {
			float a = Math.min(Ease.SMOOTH.between(t, seq.titleFrom, seq.titleFrom + 12), 1 - Ease.SMOOTH.between(t, seq.titleTo - 12, seq.titleTo));
			if (a > 0.02f) {
				int rank = PlayerCultivation.rank(p.realm, p.stage);
				Component title = switch (p.path) {
					case GHOST -> GhostMethods.rankName(rank);
					case DEMON -> DemonPath.rankName(rank);
					case BODY -> BodyPath.rankName(rank);
					default -> p.realm.getDisplayName();
				};
				int alpha = Math.max(4, Math.round(a * 255)) << 24;
				// In the picture's lower part, just above the lower bar: clear of the hero shot's body, which fills its middle.
				float y = h - full - 46;
				g.pose().pushPose();
				g.pose().translate(w / 2f, y, 0);
				g.pose().scale(2.5f, 2.5f, 1);
				g.drawCenteredString(mc.font, title, 0, 0, alpha | 0xE8D6FC);
				g.pose().popPose();
				if (p.path == com.example.defyingtheheavens.TwoPaths.Path.HEAVEN) {
					g.drawCenteredString(mc.font, p.stage.getDisplayName(), w / 2, Math.round(y) + 26, alpha | 0xE6D3A3);
				}
			}
		}
		// The skip ring in the lower bar's right end: hold Sneak a second.
		Component hint = Component.translatable(ModLang.HUD_CINEMATIC_SKIP, mc.options.keyShift.getTranslatedKeyMessage());
		int rx = w - 18, ry = h - Math.max(14, bar / 2);
		g.drawString(mc.font, hint, rx - 14 - mc.font.width(hint), ry - 4, 0xA0E6D3A3);
		float progress = CinematicClient.skipProgress();
		for (int i = 0; i < 16; i++) {
			double a = -Math.PI / 2 + i * Math.PI * 2 / 16;
			int x = rx + (int) Math.round(Math.cos(a) * 7), y = ry + (int) Math.round(Math.sin(a) * 7);
			g.fill(x - 1, y - 1, x + 1, y + 1, i < progress * 16 ? 0xFFFFD54F : 0x806E5214);
		}
	}

	private static int argb(int colour, float alpha) {
		return Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24 | colour & 0xFFFFFF;
	}

	private CinematicHud() {}
}
