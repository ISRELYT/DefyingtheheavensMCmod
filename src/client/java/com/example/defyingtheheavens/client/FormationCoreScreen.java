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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * A Formation Core's screen (its owner's only): the barrier's radius, its switch, and the qi that carries it. The battery is
 * drawn like the Qi bar (lacquer track, gold trim and quarter notches, azure qi); below it what the connected veins bring in
 * against what the barrier costs at the chosen radius, how strong a cultivator must be to break through, and the core's state.
 * A second page lists the cultivators the owner trusts to pass the barrier as they do: type a name to add one, press the
 * cross beside a name to strike it off. The screen asks the server for a fresh state twice a second while open.
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

	private static final int ROWS = 8;
	private static final int ROW_HEIGHT = 13;

	/** A trusted cultivator, with the name they had when trusted. */
	public record Trusted(UUID id, String name) {}

	/** What the server last told us about the core. */
	public record State(BlockPos pos, int radius, boolean wanted, boolean raised, double battery, double max, double supply, int veins,
			int rank, int cooldown, String owner, List<Trusted> trusted) {}

	private State state;
	private int radius;
	private boolean wanted;
	private int left, top;
	private Button toggle;
	private int refresh;
	/** On the trusted cultivators' page, and what is typed into its name box. */
	private boolean trustedPage;
	private EditBox nameBox;
	private String draft = "";

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
			boolean listChanged = !open.state.trusted().equals(state.trusted());
			open.state = state;
			if (listChanged && open.trustedPage) open.rebuildWidgets(); // a name came or went: its row and cross with it
			return;
		}
		if (mc.screen == null) mc.setScreen(new FormationCoreScreen(state));
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		if (trustedPage) {
			initTrustedPage();
			return;
		}
		nameBox = null;
		toggle = null;
		int y = top + 44;
		addRenderableWidget(Button.builder(Component.literal("-8"), b -> setRadius(radius - 8)).bounds(left + 12, y, 26, 18).build());
		addRenderableWidget(Button.builder(Component.literal("-1"), b -> setRadius(radius - 1)).bounds(left + 40, y, 22, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+1"), b -> setRadius(radius + 1)).bounds(left + WIDTH - 62, y, 22, 18).build());
		addRenderableWidget(Button.builder(Component.literal("+8"), b -> setRadius(radius + 8)).bounds(left + WIDTH - 38, y, 26, 18).build());
		toggle = addRenderableWidget(Button.builder(Component.empty(), b -> {
			wanted = !wanted;
			send();
		}).bounds(left + 10, top + HEIGHT - 28, 104, 20).build());
		addRenderableWidget(Button.builder(Component.translatable(ModLang.CORE_TRUSTED_BUTTON), b -> {
			trustedPage = true;
			rebuildWidgets();
		}).bounds(left + WIDTH - 114, top + HEIGHT - 28, 104, 20).build());
		updateToggle();
	}

	/** The trusted page: a name box with its button, a cross beside each name in two columns, and the way back. */
	private void initTrustedPage() {
		nameBox = addRenderableWidget(new EditBox(font, left + 12, top + 46, WIDTH - 82, 18, Component.translatable(ModLang.CORE_TRUSTED_TITLE)));
		nameBox.setMaxLength(16);
		nameBox.setValue(draft);
		nameBox.setResponder(text -> draft = text);
		setInitialFocus(nameBox);
		addRenderableWidget(Button.builder(Component.translatable(ModLang.CORE_TRUST_ADD), b -> addTrusted()).bounds(left + WIDTH - 64, top + 45, 52, 20).build());
		List<Trusted> trusted = state.trusted();
		int columnWidth = (WIDTH - 24) / 2;
		for (int i = 0; i < trusted.size() && i < ROWS * 2; i++) {
			Trusted player = trusted.get(i);
			int x = left + 12 + (i / ROWS) * columnWidth + columnWidth - 16;
			int y = top + 84 + (i % ROWS) * ROW_HEIGHT;
			Button cross = Button.builder(Component.literal("x"), b -> sendTrust(false, null, player.id())).bounds(x, y - 1, 12, 12).build();
			cross.setTooltip(Tooltip.create(Component.translatable(ModLang.CORE_TRUST_REMOVE, player.name())));
			addRenderableWidget(cross);
		}
		addRenderableWidget(Button.builder(Component.translatable(ModLang.CORE_BACK), b -> {
			trustedPage = false;
			rebuildWidgets();
		}).bounds(left + WIDTH / 2 - 52, top + HEIGHT - 28, 104, 20).build());
	}

	private void addTrusted() {
		String name = draft.trim();
		if (name.isEmpty()) return;
		sendTrust(true, name, null);
		draft = "";
		if (nameBox != null) nameBox.setValue("");
	}

	private void sendTrust(boolean add, String name, UUID id) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(state.pos());
		buf.writeBoolean(add);
		if (add) buf.writeUtf(name, 16);
		else buf.writeUUID(id);
		ClientPlayNetworking.send(ModPackets.FORMATION_CORE_TRUST, buf);
	}

	/** Enter in the name box trusts the name typed. */
	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (trustedPage && nameBox != null && nameBox.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
			addTrusted();
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
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
		if (nameBox != null) nameBox.tick(); // the cursor's blink
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
		if (trustedPage) {
			renderTrustedPage(g, cx);
			super.render(g, mouseX, mouseY, partialTick);
			return;
		}
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
		for (FormattedCharSequence line : font.split(strength, WIDTH - 28)) {
			g.drawString(font, line, left + 14, y, 0xB39DDB);
			y += 10;
		}
		y += 3;
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

	private void renderTrustedPage(GuiGraphics g, int cx) {
		g.drawCenteredString(font, Component.translatable(ModLang.CORE_TRUSTED_TITLE), cx, top + 8, GOLD_TEXT);
		g.drawCenteredString(font, Component.translatable(ModLang.CORE_TRUSTED_HINT), cx, top + 20, 0x9E9E9E);
		g.fill(left + 10, top + 32, left + WIDTH - 10, top + 33, BORDER_DARK);
		List<Trusted> trusted = state.trusted();
		g.drawString(font, Component.translatable(ModLang.CORE_TRUSTED_COUNT, trusted.size(), FormationCoreBlockEntity.MAX_TRUSTED),
				left + 12, top + 70, 0x8A8A9A);
		if (trusted.isEmpty()) {
			g.drawCenteredString(font, Component.translatable(ModLang.CORE_TRUSTED_NONE), cx, top + 100, 0x8A8A9A);
			return;
		}
		int columnWidth = (WIDTH - 24) / 2;
		for (int i = 0; i < trusted.size() && i < ROWS * 2; i++) {
			int x = left + 12 + (i / ROWS) * columnWidth;
			int y = top + 84 + (i % ROWS) * ROW_HEIGHT;
			g.drawString(font, font.plainSubstrByWidth(trusted.get(i).name(), columnWidth - 22), x, y + 1, PALE);
		}
	}

	private static String fmt(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
