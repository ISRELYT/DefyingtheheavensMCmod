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
 * A Formation Core's screen (its owner's only), in the Hall's ink and gold (HallHud, HallButton; 2026-10-30): its two functions on two
 * tabs, the barrier and the concealment (since 2026-10-28, Concealments), the one shown in jade, each with its radius and its switch, and
 * the qi that carries them. The battery is drawn like the HUD's qi bar (a thin frame, the mod's blue, what it holds written across it);
 * below it what the connected veins bring in against what the chosen function costs at its
 * radius (and both together, when both are wanted), how strong a cultivator must be to break through the barrier or to see through the
 * concealment, and the function's state. A second page lists the cultivators the owner trusts to pass the barrier and see through the
 * concealment as they do: type a name to add one, press the cross beside a name to strike it off. The screen asks the server for a fresh
 * state twice a second while open.
 */
public class FormationCoreScreen extends Screen implements HallFrame.Framed {
	private static final int WIDTH = 232;
	private static final int HEIGHT = 232;
	private static final int GOLD_TEXT = 0xFFD54F;
	private static final int PALE = 0xE6D3A3;
	private static final int MUTED = 0xB9B2A0;
	private static final int FADED = 0x8C8676;

	private static final int ROWS = 8;
	private static final int ROW_HEIGHT = 13;
	/** Where the function's radius row and the battery stand (below the tabs). */
	private static final int RADIUS_Y = 58, BATTERY_Y = 88, LINES_Y = 114;

	/** A trusted cultivator, with the name they had when trusted. */
	public record Trusted(UUID id, String name) {}

	/** What the server last told us about the core. */
	public record State(BlockPos pos, int radius, boolean wanted, boolean raised, double battery, double max, double supply, int veins,
			int rank, int cooldown, String owner, List<Trusted> trusted, int veilRadius, boolean veilWanted, boolean veilRaised, double share) {}

	private State state;
	private int radius, veilRadius;
	private boolean wanted, veilWanted;
	/** On the concealment's tab (kept between openings), else the barrier's. */
	private static boolean veilTab;
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
		this.veilRadius = state.veilRadius();
		this.veilWanted = state.veilWanted();
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
		// The two functions' tabs, the one shown in jade.
		int tabW = (WIDTH - 24) / 2;
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_TAB_BARRIER), b -> showTab(false)).open(() -> !veilTab)
				.bounds(left + 10, top + 37, tabW, 16).build());
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_TAB_VEIL), b -> showTab(true)).open(() -> veilTab)
				.bounds(left + 14 + tabW, top + 37, tabW, 16).build());
		int y = top + RADIUS_Y;
		addRenderableWidget(HallButton.of(Component.literal("-8"), b -> setRadius(shownRadius() - 8)).bounds(left + 12, y, 26, 18).build());
		addRenderableWidget(HallButton.of(Component.literal("-1"), b -> setRadius(shownRadius() - 1)).bounds(left + 40, y, 22, 18).build());
		addRenderableWidget(HallButton.of(Component.literal("+1"), b -> setRadius(shownRadius() + 1)).bounds(left + WIDTH - 62, y, 22, 18).build());
		addRenderableWidget(HallButton.of(Component.literal("+8"), b -> setRadius(shownRadius() + 8)).bounds(left + WIDTH - 38, y, 26, 18).build());
		toggle = addRenderableWidget(HallButton.of(Component.empty(), b -> {
			if (veilTab) veilWanted = !veilWanted;
			else wanted = !wanted;
			send();
		}).bounds(left + 10, top + HEIGHT - 28, 124, 20).build());
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_TRUSTED_BUTTON), b -> {
			trustedPage = true;
			rebuildWidgets();
		}).bounds(left + WIDTH - 94, top + HEIGHT - 28, 84, 20).build());
		updateToggle();
	}

	private void showTab(boolean veil) {
		if (veilTab == veil) return;
		veilTab = veil;
		rebuildWidgets();
	}

	private int shownRadius() {
		return veilTab ? veilRadius : radius;
	}

	/** The trusted page: a name box with its button, a cross beside each name in two columns, and the way back. */
	private void initTrustedPage() {
		// The box without vanilla's frame: it stands in an ink well of the Hall's (renderTrustedPage).
		nameBox = addRenderableWidget(new EditBox(font, left + 17, top + 51, WIDTH - 92, 10, Component.translatable(ModLang.CORE_TRUSTED_TITLE)));
		nameBox.setBordered(false);
		nameBox.setMaxLength(16);
		nameBox.setValue(draft);
		nameBox.setResponder(text -> draft = text);
		setInitialFocus(nameBox);
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_TRUST_ADD), b -> addTrusted()).bounds(left + WIDTH - 64, top + 45, 52, 20).build());
		List<Trusted> trusted = state.trusted();
		int columnWidth = (WIDTH - 24) / 2;
		for (int i = 0; i < trusted.size() && i < ROWS * 2; i++) {
			Trusted player = trusted.get(i);
			int x = left + 12 + (i / ROWS) * columnWidth + columnWidth - 16;
			int y = top + 84 + (i % ROWS) * ROW_HEIGHT;
			Button cross = HallButton.of(Component.literal("x"), b -> sendTrust(false, null, player.id())).bounds(x, y - 1, 12, 12).build();
			cross.setTooltip(Tooltip.create(Component.translatable(ModLang.CORE_TRUST_REMOVE, player.name())));
			addRenderableWidget(cross);
		}
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_BACK), b -> {
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

	/** The shown function's radius. */
	private void setRadius(int value) {
		int clamped = Math.max(Formations.MIN_RADIUS, Math.min(Formations.maxRadius(net.minecraft.client.Minecraft.getInstance().level), value));
		if (clamped == shownRadius()) return;
		if (veilTab) veilRadius = clamped;
		else radius = clamped;
		send();
	}

	private void send() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(state.pos());
		buf.writeVarInt(radius);
		buf.writeBoolean(wanted);
		buf.writeVarInt(veilRadius);
		buf.writeBoolean(veilWanted);
		ClientPlayNetworking.send(ModPackets.FORMATION_CORE_CONFIGURE, buf);
		updateToggle();
	}

	private void updateToggle() {
		if (toggle == null) return;
		boolean on = veilTab ? veilWanted : wanted;
		toggle.setMessage(Component.translatable(veilTab ? on ? ModLang.CORE_VEIL_OFF : ModLang.CORE_VEIL_ON : on ? ModLang.CORE_SWITCH_OFF : ModLang.CORE_SWITCH_ON));
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
		HallHud.panel(g, left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, HallHud.FRAME, HallHud.PANEL);
		int cx = left + WIDTH / 2;
		if (trustedPage) {
			renderTrustedPage(g, cx);
			super.render(g, mouseX, mouseY, partialTick);
			return;
		}
		HallHud.heading(g, font, title, left + 10, top + 8, WIDTH - 20);
		if (!state.owner().isEmpty()) {
			g.drawString(font, Component.translatable(ModLang.CORE_OWNER, state.owner()), left + 10, top + 24, MUTED, false);
		}

		// The shown function's radius between its buttons.
		g.drawCenteredString(font, Component.translatable(veilTab ? ModLang.CORE_VEIL_RADIUS : ModLang.CORE_RADIUS, shownRadius()), cx, top + RADIUS_Y + 5, PALE);

		// The battery, as the HUD's qi bar: what it holds written across it.
		int barX = left + 16, barY = top + BATTERY_Y, barW = WIDTH - 32;
		double fill = state.max() <= 0 ? 0 : Math.min(1, state.battery() / state.max());
		HallHud.bar(g, font, barX, barY, barW, 11, fill, HallHud.QI, HallHud.QI_LIGHT,
				Component.translatable(ModLang.CORE_BATTERY, (int) state.battery(), (int) state.max()).getString());

		// Supply against upkeep: the shown function's at its radius, and both together when both are wanted.
		double share = state.share();
		double barrierCost = FormationCoreBlockEntity.upkeep(radius) * share, veilCost = FormationCoreBlockEntity.veilUpkeep(veilRadius) * share;
		double shownCost = veilTab ? veilCost : barrierCost;
		double total = (wanted ? barrierCost : 0) + (veilWanted ? veilCost : 0);
		double needed = Math.max(shownCost, total);
		int y = top + LINES_Y;
		y = lines(g, Component.translatable(ModLang.CORE_VEINS, state.veins(), fmt(state.supply())), y, 0x7FE0A0);
		y = lines(g, Component.translatable(veilTab ? ModLang.CORE_VEIL_UPKEEP : ModLang.CORE_UPKEEP, fmt(shownCost)), y,
				state.supply() >= shownCost ? MUTED : 0xFF8A65);
		if (wanted && veilWanted) {
			y = lines(g, Component.translatable(ModLang.CORE_UPKEEP_BOTH, fmt(total)), y, state.supply() >= total ? MUTED : 0xFF8A65);
		}
		if (share < 1) {
			y = lines(g, Component.translatable(ModLang.CORE_MASTERY, Math.round((1 - share) * 100)), y, 0x8FBC8F);
		}
		if (state.supply() < needed) {
			double seconds = state.battery() / (needed - state.supply());
			// Wrapped to the panel ("the store runs dry in 152 s" ran past its side: 2026-10-30).
			y = lines(g, Component.translatable(ModLang.CORE_SHORTFALL, (int) seconds), y, 0xFF8A65);
		}
		Component rankName = state.rank() < 0 ? null : PlayerCultivation.rankName(Realm.byIndex(state.rank() / Stage.values().length),
				Stage.byIndex(state.rank() % Stage.values().length));
		Component strength = veilTab
				? rankName == null ? Component.translatable(ModLang.CORE_VEIL_STRENGTH_MORTAL) : Component.translatable(ModLang.CORE_VEIL_STRENGTH, rankName)
				: rankName == null ? Component.translatable(ModLang.CORE_STRENGTH_MORTAL) : Component.translatable(ModLang.CORE_STRENGTH, rankName);
		for (FormattedCharSequence line : font.split(strength, WIDTH - 28)) {
			g.drawString(font, line, left + 14, y, 0xB39DDB);
			y += 10;
		}
		y += 3;
		boolean up = veilTab ? state.veilRaised() : state.raised();
		Component status = veilTab ? Component.translatable(up ? ModLang.CORE_VEIL_RAISED : ModLang.CORE_VEIL_LOWERED)
				: state.cooldown() > 0 ? Component.translatable(ModLang.CORE_SHATTERED, state.cooldown())
				: Component.translatable(up ? ModLang.CORE_RAISED : ModLang.CORE_LOWERED);
		// Wrapped to the panel, as the lines above it are (the shattered barrier's countdown ran out past both its sides: 2026-10-24).
		for (FormattedCharSequence line : font.split(status, WIDTH - 24)) {
			g.drawCenteredString(font, line, cx, y, up ? 0x69F0AE : 0xFFC107);
			y += 10;
		}
		if (state.veins() == 0) {
			int hy = y + 2;
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.CORE_HINT), WIDTH - 24)) {
				if (hy > top + HEIGHT - 40) break; // never under the buttons
				g.drawCenteredString(font, line, cx, hy, FADED);
				hy += 10;
			}
		}
		super.render(g, mouseX, mouseY, partialTick);
	}

	private void renderTrustedPage(GuiGraphics g, int cx) {
		HallHud.heading(g, font, Component.translatable(ModLang.CORE_TRUSTED_TITLE), left + 10, top + 8, WIDTH - 20);
		g.drawString(font, Component.translatable(ModLang.CORE_TRUSTED_HINT), left + 10, top + 24, MUTED, false);
		// The name box's well (the box itself has no frame of its own).
		HallHud.panel(g, left + 12, top + 46, left + WIDTH - 70, top + 64, nameBox != null && nameBox.isFocused() ? HallHud.FRAME_LIT : HallHud.FRAME,
				0xFF0B0E12);
		List<Trusted> trusted = state.trusted();
		g.drawString(font, Component.translatable(ModLang.CORE_TRUSTED_COUNT, trusted.size(), FormationCoreBlockEntity.MAX_TRUSTED),
				left + 12, top + 70, FADED);
		if (trusted.isEmpty()) {
			g.drawCenteredString(font, Component.translatable(ModLang.CORE_TRUSTED_NONE), cx, top + 100, FADED);
			return;
		}
		int columnWidth = (WIDTH - 24) / 2;
		for (int i = 0; i < trusted.size() && i < ROWS * 2; i++) {
			int x = left + 12 + (i / ROWS) * columnWidth;
			int y = top + 84 + (i % ROWS) * ROW_HEIGHT;
			g.drawString(font, font.plainSubstrByWidth(trusted.get(i).name(), columnWidth - 22), x, y + 1, PALE);
		}
	}

	/** {@code text} wrapped to the panel from its left margin at {@code y}; returns where the next line goes. */
	private int lines(GuiGraphics g, Component text, int y, int color) {
		for (FormattedCharSequence line : font.split(text, WIDTH - 28)) {
			g.drawString(font, line, left + 14, y, color);
			y += 10;
		}
		return y + 1;
	}

	private static String fmt(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
