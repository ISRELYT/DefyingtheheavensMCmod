package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.NpcTitle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Players' plates as the server tells them (PlayerTitles, 2026-10-27): over each player's name, the title they have taken, in gold, and
 * above it their sect's title for them, as the sects' titles are written over cultivators ([Elder], in its colour), each on a line of its
 * own so neither stands over the other nor over the name.
 */
public final class ClientPlates {
	/** A player's plate: their title ("" none), their sect's title for them (an NpcTitle's ordinal, -1 none) and how their sect names it. */
	public record Plate(String title, int sectTitle, boolean clan, boolean holy, boolean immortal) {}

	/** A taken title's colour: the gold of the Immortal Realm. */
	public static final int TITLE_COLOR = 0xFFD54F;
	private static final Map<UUID, Plate> PLATES = new HashMap<>();

	static void put(Map<UUID, Plate> plates) {
		plates.forEach((id, plate) -> {
			if (plate.title().isEmpty() && plate.sectTitle() < 0) PLATES.remove(id);
			else PLATES.put(id, plate);
		});
	}

	/** Disconnect. */
	public static void clear() {
		PLATES.clear();
	}

	/** {@code id}'s plate as last told, or null. */
	public static Plate of(UUID id) {
		return PLATES.get(id);
	}

	/** A plate's line, in blocks over the name (a name tag's line: 10 at its scale of 0.025). */
	public static final float LINE = 0.25f;

	/** How many lines {@code id}'s plate stands over their name (0 to 2): what else is shown over a player's head goes above them. */
	public static int lineCount(UUID id) {
		Plate plate = PLATES.get(id);
		return plate == null ? 0 : lines(plate).size();
	}

	/** The lines over {@code plate}'s owner's name, nearest the name first: their title, then their sect's title. */
	static List<Component> lines(Plate plate) {
		List<Component> lines = new ArrayList<>(2);
		if (!plate.title().isEmpty()) {
			lines.add(Component.translatable(ModLang.NPC_TITLE_BRACKETS, plate.title()).withStyle(s -> s.withColor(TextColor.fromRgb(TITLE_COLOR))));
		}
		if (plate.sectTitle() >= 0 && plate.sectTitle() < NpcTitle.values().length) {
			NpcTitle title = NpcTitle.values()[plate.sectTitle()];
			lines.add(Component.translatable(ModLang.NPC_TITLE_BRACKETS, title.getDisplayName(plate.clan(), plate.holy(), plate.immortal()))
					.withStyle(s -> s.withColor(TextColor.fromRgb(title.getColor()))));
		}
		return lines;
	}

	/**
	 * Over {@code player}'s name tag, once vanilla has drawn it (PlayerRendererMixin): the plate's lines, each a line higher, drawn as a name
	 * is (faint through walls, whole in sight; only faint while they sneak), as far off as a name is drawn.
	 */
	public static void drawAbove(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light) {
		Plate plate = PLATES.get(player.getUUID());
		if (plate == null) return;
		Minecraft mc = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
		double distance = dispatcher.distanceToSqr(player);
		if (distance > 4096) return;
		List<Component> lines = lines(plate);
		if (lines.isEmpty()) return;
		pose.pushPose();
		float lift = player.getNameTagOffsetY();
		// With a score shown under the name its name stands a line higher (PlayerRenderer#renderNameTag): these over it too.
		if (distance < 100 && player.getScoreboard().getDisplayObjective(2) != null) lift += 9.0f * 1.15f * 0.025f;
		pose.translate(0.0f, lift, 0.0f);
		pose.mulPose(dispatcher.cameraOrientation());
		pose.scale(-0.025f, -0.025f, 0.025f);
		Matrix4f matrix = pose.last().pose();
		Font font = mc.font;
		boolean inSight = !player.isDiscrete();
		int back = (int) (mc.options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
		float y = -10;
		for (Component line : lines) {
			float x = -font.width(line) / 2f;
			font.drawInBatch(line, x, y, 0x20FFFFFF, false, matrix, buffers, inSight ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, back, light);
			if (inSight) font.drawInBatch(line, x, y, 0xFFFFFFFF, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
			y -= 10;
		}
		pose.popPose();
	}

	private ClientPlates() {}
}
