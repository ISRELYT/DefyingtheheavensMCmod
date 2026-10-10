package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FormationCoreBlockEntity;
import com.example.defyingtheheavens.Formations;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Locale;

/**
 * A Formation Core's screen (its owner's only): the barrier's radius, its switch, and the qi that carries it. The battery is
 * drawn like the Qi bar (lacquer track, gold trim and quarter notches, azure qi); below it what the connected veins bring in
 * against what the barrier costs at the chosen radius, how strong a cultivator must be to break through, and the core's state.
 * The screen asks the server for a fresh state twice a second while open.
 */
public class FormationCoreScreen extends Screen {
	private static final int WIDTH = 232;
	private static final int HEIGHT = 212;
	private static final int BORDER = 0xFFB8860B;
	private static final int BORDER_DARK = 0xFF6E5214;
	private static final int PANEL = 0xF0101018;
	private static final int TRACK = 0xFF1B1E2A;
	private static final int QI = 0xFF4FA8E8;
	private static final int QI_LIGHT = 0xFF8FD0FF;
	private static final int GOLD_TEXT = 0xE8C77A;
	private static final int PALE = 0xC8D6E8;

	/** What the server last told us about the core. */
	public record State(BlockPos pos, int radius, boolean wanted, boolean raised, double battery, double max, double supply, int veins,
			int rank, int cooldown, String owner) {}

	private State state;
	private int radius;
	private boolean wanted;
	private int left, top;
	private Button toggle;
	private int refresh;

	private FormationCoreScreen(State state) {
		super(Component.translatable(ModLang.CORE_TITLE));
		this.state = state;
		this.radius = state.radius();
		this.wanted = state.wanted();
	}

	/** The FORMATION_CORE packet: opens the screen, or refreshes it if it is open on that core. */
	public static void receive(State state) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof FormationCoreScreen open && open.state.pos().equals(state.pos())) {
			open.state = state;
			return;
		}
		if (mc.screen == null) mc.setScreen(new FormationCoreScreen(state));
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		int y = top + 44;
		addRenderableWidget(Button.builder(Component.literal("-8"), b -> setRadius(radius - 8)).bounds(left + 12, y, 26, 18).build());
		addRenderableWidget(Button.builder(Component.literal("-1"), b -> setRadius(radius - 1)).bounds(left + 40, y, 22, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+1"), b -> setRadius(radius + 1)).bounds(left + WIDTH - 62, y, 22, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+8"), b -> setRadius(radius + 8)).bounds(left + WIDTH - 38, y, 26, 18).build());
		toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
			wanted = !wanted;
			send();
		}).bounds(left + WIDTH / 2 - 60, top + HEIGHT - 28, 120, 20).build());
		updateToggle();
	}

	private void setRadius(int value) {
		int clamped = Math.max(Formations.MIN_RADIUS, Math.min(Formations.MAX_RADIUS, value));
		if (clamped == radius) return;
		radius = clamped;
		send();
	}

	private void send() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(state.pos());
		buf.writeVarInt(radius);
		buf.writeBoolean(wanted);
		ClientPlayNetworking.send(ModPackets.FORMATION_CORE_CONFIGURE, buf);
		updateToggle();
	}

	private void updateToggle() {
		if (toggle != null) toggle.setMessage(Component.translatable(wanted ? ModLang.CORE_SWITCH_OFF : ModLang.CORE_SWITCH_ON));
	}

	@Override
	public void tick() {
		if (++refresh % 10 == 0) {
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeBlockPos(state.pos());
			ClientPlayNetworking.send(ModPackets.FORMATION_CORE_QUERY, buf);
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.player.distanceToSqr(state.pos().getCenter()) > 64) onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, BORDER);
		g.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, BORDER_DARK);
		g.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
		int cx = left + WIDTH / 2;
		g.drawCenteredString(font, title, cx, top + 8, GOLD_TEXT);
		if (!state.owner().isEmpty()) {
			g.drawCenteredString(font, Component.translatable(ModLang.CORE_OWNER, state.owner()), cx, top + 20, 0x9E9E9E);
		}
		g.fill(left + 10, top + 32, left + WIDTH - 10, top + 33, BORDER_DARK);

		// Radius between its buttons.
		g.drawCenteredString(font, Component.translatable(ModLang.CORE_RADIUS, radius), cx, top + 49, PALE);

		// The battery.
		int barX = left + 16, barY = top + 72, barW = WIDTH - 32, barH = 6;
		double fill = state.max() <= 0 ? 0 : Math.min(1, state.battery() / state.max());
		g.fill(barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, BORDER);
		g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, BORDER_DARK);
		g.fill(barX, barY, barX + barW, barY + barH, TRACK);
		int filled = (int) Math.round(barW * fill);
		g.fill(barX, barY, barX + filled, barY + barH, QI);
		g.fill(barX, barY, barX + filled, barY + 2, QI_LIGHT);
		for (int i = 1; i < 4; i++) {
			int x = barX + barW * i / 4;
			g.fill(x, barY - 2, x + 1, barY + barH + 2, i == 2 ? BORDER : BORDER_DARK);
		}
		g.drawCenteredString(font, Component.translatable(ModLang.CORE_BATTERY, (int) state.battery(), (int) state.max()), cx, barY + 11, PALE);

		// Supply against upkeep at the chosen radius.
		double upkeep = FormationCoreBlockEntity.upkeep(radius);
		int y = top + 100;
		g.drawString(font, Component.translatable(ModLang.CORE_VEINS, state.veins(), fmt(state.supply())), left + 14, y, 0x7FE0A0);
		y += 11;
		g.drawString(font, Component.translatable(ModLang.CORE_UPKEEP, fmt(upkeep)), left + 14, y, state.supply() >= upkeep ? 0xB0B0B0 : 0xFF8A65);
		y += 11;
		if (state.supply() < upkeep) {
			double seconds = (upkeep - state.supply()) <= 0 ? 0 : state.battery() / (upkeep - state.supply());
			g.drawString(font, Component.translatable(ModLang.CORE_SHORTFALL, (int) seconds), left + 14, y, 0xFF8A65);
			y += 11;
		}
		Component strength = state.rank() < 0 ? Component.translatable(ModLang.CORE_STRENGTH_MORTAL)
				: Component.translatable(ModLang.CORE_STRENGTH, PlayerCultivation.rankName(Realm.byIndex(state.rank() / Stage.values().length),
						Stage.byIndex(state.rank() % Stage.values().length)));
		g.drawString(font, strength, left + 14, y, 0xB39DDB);
		y += 13;
		Component status = state.cooldown() > 0 ? Component.translatable(ModLang.CORE_SHATTERED, state.cooldown())
				: Component.translatable(state.raised() ? ModLang.CORE_RAISED : ModLang.CORE_LOWERED);
		g.drawCenteredString(font, status, cx, y, state.raised() ? 0x69F0AE : 0xFFC107);
		if (state.veins() == 0) {
			int hy = y + 12;
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.CORE_HINT), WIDTH - 24)) {
				g.drawCenteredString(font, line, cx, hy, 0x8A8A9A);
				hy += 10;
			}
		}
		super.render(g, mouseX, mouseY, partialTick);
	}

	private static String fmt(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
