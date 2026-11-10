package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BodyPath;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.FormationCoreBlockEntity;
import com.example.defyingtheheavens.Formations;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.NpcTitle;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.PlayerSects;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.Util;
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

	/** One of a player's sect's people on its Sect page: a cultivator (by seniority), or a player (title by strength; rank -1 offline). */
	public record SectMember(UUID id, String name, int title, int rank, boolean player) {}

	/**
	 * What the server last told of the core's Sect page (PlayerSects#sendInfo): the player's sect on it ({@code own}: its name, tribute waiting
	 * and a day's, places, mats inside, the founder's cap, gates, altar, ticks to the next aspirant or -1, its kind, its people), or whether one
	 * could be founded ({@code canFound}, else {@code refusal} why), the mats inside, their cap, and the fallen sect it would revive.
	 */
	public record SectInfo(BlockPos pos, boolean own, String name, long tribute, long daily, int capacity, int mats, int cap, int gates,
			boolean altar, long nextArrival, boolean clan, boolean holy, boolean immortal, boolean body, List<SectMember> members,
			boolean canFound, Component refusal, String fallen) {}

	private State state;
	private SectInfo sectInfo;
	private int radius, veilRadius;
	private boolean wanted, veilWanted;
	/** On the Sect page (PlayerSects), what is typed into its box (a name for the sect, or a player's to invite), and how far its list is scrolled. */
	private boolean sectPage;
	private EditBox sectBox;
	private String sectDraft = "";
	private int sectScroll, sectMaxScroll;
	/** Disband pressed once (pressed again within 3 s, it is done), and a cross pressed once beside whom (the same). */
	private long disbandArmedAt = -1, expelArmedAt = -1;
	private UUID expelArmed;
	private Button disbandButton;
	/** The expelling crosses drawn this frame: where, and whom. */
	private final List<Cross> crosses = new java.util.ArrayList<>();
	private record Cross(int x, int y, UUID id, String name) {
		boolean contains(double mx, double my) { return mx >= x && mx < x + 9 && my >= y && my < y + 9; }
	}
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

	/** The PLAYER_SECT packet: the Sect page's state, if the screen is open on that core (its widgets rebuilt when the page's kind changes). */
	public static void receiveSect(SectInfo info) {
		if (!(Minecraft.getInstance().screen instanceof FormationCoreScreen open) || !open.state.pos().equals(info.pos())) return;
		boolean rebuild = open.sectPage && (open.sectInfo == null || open.sectInfo.own() != info.own());
		open.sectInfo = info;
		if (rebuild) open.rebuildWidgets();
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		crosses.clear();
		disbandButton = null;
		sectBox = null;
		if (trustedPage) {
			initTrustedPage();
			return;
		}
		if (sectPage) {
			initSectPage();
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
		}).bounds(left + 8, top + HEIGHT - 28, 108, 20).build());
		// Its sect (PlayerSects): founding one on it, or running the one founded.
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_SECT_BUTTON), b -> {
			sectPage = true;
			sendSect(PlayerSects.Act.QUERY, "", null);
			rebuildWidgets();
		}).bounds(left + 119, top + HEIGHT - 28, 46, 20).build());
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_TRUSTED_BUTTON), b -> {
			trustedPage = true;
			rebuildWidgets();
		}).bounds(left + 168, top + HEIGHT - 28, 56, 20).build());
		updateToggle();
	}

	/** Where the Sect page's box well and buttons stand (above the bottom row). */
	private static final int SECT_WELL_Y = HEIGHT - 54;

	/**
	 * The Sect page: founding one (a name box and Found), or the player's own sect (a box for a new name or a player's name, Rename and
	 * Invite beside it; Collect by its tribute; Disband, twice pressed); the way back below.
	 */
	private void initSectPage() {
		nameBox = null;
		toggle = null;
		boolean own = sectInfo != null && sectInfo.own();
		int wellY = top + SECT_WELL_Y;
		sectBox = addRenderableWidget(new EditBox(font, left + 17, wellY + 6, WIDTH - (own ? 132 : 92), 10,
				Component.translatable(own ? ModLang.CORE_SECT_INVITE_HINT : ModLang.CORE_SECT_NAME_HINT)));
		sectBox.setBordered(false);
		sectBox.setMaxLength(PlayerSects.NAME_LENGTH);
		sectBox.setValue(sectDraft);
		sectBox.setResponder(text -> sectDraft = text);
		sectBox.setHint(Component.translatable(own ? ModLang.CORE_SECT_INVITE_HINT : ModLang.CORE_SECT_NAME_HINT).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
		if (!own) {
			addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_SECT_FOUND), b -> sendSect(PlayerSects.Act.FOUND, sectDraft, null))
					.bounds(left + WIDTH - 64, wellY, 52, 20).build());
		} else {
			addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_SECT_RENAME), b -> {
				sendSect(PlayerSects.Act.RENAME, sectDraft, null);
				clearSectBox();
			}).bounds(left + WIDTH - 112, wellY, 50, 20).build());
			addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_SECT_INVITE), b -> {
				sendSect(PlayerSects.Act.INVITE, sectDraft, null);
				clearSectBox();
			}).bounds(left + WIDTH - 60, wellY, 48, 20).build());
			addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_SECT_COLLECT), b -> sendSect(PlayerSects.Act.COLLECT, "", null))
					.bounds(left + 96, top + HEIGHT - 28, 56, 20).build());
			disbandButton = addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_SECT_DISBAND), b -> {
				long now = Util.getMillis();
				if (disbandArmedAt >= 0 && now - disbandArmedAt < 3000) {
					disbandArmedAt = -1;
					sendSect(PlayerSects.Act.DISBAND, "", null);
					b.setMessage(Component.translatable(ModLang.CORE_SECT_DISBAND));
				} else {
					disbandArmedAt = now;
					b.setMessage(Component.translatable(ModLang.CORE_SECT_SURE));
				}
			}).bounds(left + WIDTH - 76, top + HEIGHT - 28, 64, 20).build());
		}
		addRenderableWidget(HallButton.of(Component.translatable(ModLang.CORE_BACK), b -> {
			sectPage = false;
			rebuildWidgets();
		}).bounds(own ? left + 12 : left + WIDTH / 2 - 52, top + HEIGHT - 28, own ? 80 : 104, 20).build());
		setInitialFocus(sectBox);
	}

	private void clearSectBox() {
		sectDraft = "";
		if (sectBox != null) sectBox.setValue("");
	}

	private void sendSect(PlayerSects.Act act, String text, UUID target) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBlockPos(state.pos());
		buf.writeVarInt(act.ordinal());
		buf.writeUtf(text == null ? "" : text.length() > PlayerSects.NAME_LENGTH ? text.substring(0, PlayerSects.NAME_LENGTH) : text, PlayerSects.NAME_LENGTH * 2);
		buf.writeBoolean(target != null);
		if (target != null) buf.writeUUID(target);
		ClientPlayNetworking.send(ModPackets.PLAYER_SECT_ACT, buf);
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
		// Enter in the founding page's box founds it (the own sect's page has two buttons for its box: no guessing which).
		if (sectPage && sectBox != null && sectBox.isFocused() && (sectInfo == null || !sectInfo.own())
				&& (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
			sendSect(PlayerSects.Act.FOUND, sectDraft, null);
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	/** A cross on the Sect page's list pressed twice (within 3 s) casts that one out. */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (sectPage && button == 0) {
			for (Cross cross : crosses) {
				if (!cross.contains(mouseX, mouseY)) continue;
				long now = Util.getMillis();
				if (cross.id().equals(expelArmed) && now - expelArmedAt < 3000) {
					expelArmed = null;
					sendSect(PlayerSects.Act.EXPEL, "", cross.id());
				} else {
					expelArmed = cross.id();
					expelArmedAt = now;
				}
				Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
						net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (sectPage && sectMaxScroll > 0) {
			sectScroll = Math.max(0, Math.min(sectMaxScroll, sectScroll - (int) Math.round(delta * 11)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
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
		if (sectBox != null) sectBox.tick();
		if (disbandButton != null && disbandArmedAt >= 0 && Util.getMillis() - disbandArmedAt >= 3000) {
			disbandArmedAt = -1;
			disbandButton.setMessage(Component.translatable(ModLang.CORE_SECT_DISBAND));
		}
		if (sectPage && refresh % 20 == 0) sendSect(PlayerSects.Act.QUERY, "", null); // the Sect page once a second (it looks its grounds over)
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
		if (sectPage) {
			sectHover = null;
			renderSectPage(g, cx, mouseX, mouseY);
			super.render(g, mouseX, mouseY, partialTick);
			if (sectHover != null) g.renderTooltip(font, sectHover, mouseX, mouseY);
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

	/**
	 * The Sect page: founding one (who may, the mats inside and the cap, the fallen sect it would revive, and what stands in the way), or the
	 * player's own sect: its places, its tribute, the next aspirant, its gates and altar, and its people (a cross beside each to cast them out).
	 */
	private void renderSectPage(GuiGraphics g, int cx, int mouseX, int mouseY) {
		crosses.clear();
		SectInfo info = sectInfo;
		boolean own = info != null && info.own();
		int wellY = top + SECT_WELL_Y;
		HallHud.panel(g, left + 12, wellY + 1, left + WIDTH - (own ? 116 : 68), wellY + 19,
				sectBox != null && sectBox.isFocused() ? HallHud.FRAME_LIT : HallHud.FRAME, 0xFF0B0E12);
		if (!own) {
			HallHud.heading(g, font, Component.translatable(ModLang.CORE_SECT_FOUND_TITLE), left + 10, top + 8, WIDTH - 20);
			if (info == null) return;
			int y = lines(g, Component.translatable(ModLang.CORE_SECT_RULES, PlayerSects.MIN_MATS), top + 26, MUTED);
			y = lines(g, Component.translatable(ModLang.CORE_SECT_MATS, info.mats(), info.cap()), y + 2, PALE);
			if (!info.fallen().isEmpty()) y = lines(g, Component.translatable(ModLang.CORE_SECT_REVIVE, info.fallen()), y + 2, 0xB39DDB);
			lines(g, info.canFound() ? Component.translatable(ModLang.CORE_SECT_READY) : info.refusal(), y + 4, info.canFound() ? 0x69F0AE : 0xFF8A65);
			return;
		}
		HallHud.heading(g, font, Component.literal(info.name()), left + 10, top + 8, WIDTH - 20);
		int members = (int) info.members().stream().filter(m -> !m.player()).count();
		int y = lines(g, Component.translatable(ModLang.CORE_SECT_PLACES, members, info.capacity(), info.mats(), info.cap()), top + 24, PALE);
		y = lines(g, Component.translatable(ModLang.CORE_SECT_TRIBUTE, info.tribute(), info.daily()), y, 0xF2C94C);
		y = lines(g, info.nextArrival() < 0 ? Component.translatable(ModLang.CORE_SECT_FULL)
				: Component.translatable(ModLang.CORE_SECT_NEXT, Math.max(1, (info.nextArrival() + 1199) / 1200) + " min"), y, MUTED);
		y = lines(g, info.gates() == 0 ? Component.translatable(ModLang.CORE_SECT_NO_GATE)
				: Component.translatable(ModLang.CORE_SECT_GROUNDS, info.gates(), Component.translatable(info.altar() ? "gui.yes" : "gui.no")), y, FADED);
		g.fill(left + 12, y, left + WIDTH - 12, y + 1, HallHud.FRAME);
		// Its people: cultivators by seniority, then its players; scrolled with the wheel.
		int listTop = y + 3, listBottom = wellY - 4, rowH = 11;
		if (info.members().isEmpty()) {
			lines(g, Component.translatable(ModLang.CORE_SECT_NONE), listTop + 2, FADED);
			sectMaxScroll = 0;
			return;
		}
		sectMaxScroll = Math.max(0, info.members().size() * rowH - (listBottom - listTop));
		sectScroll = Math.max(0, Math.min(sectScroll, sectMaxScroll));
		g.enableScissor(left + 1, listTop, left + WIDTH - 1, listBottom);
		Component hover = null;
		int ry = listTop - sectScroll;
		for (SectMember m : info.members()) {
			if (ry + rowH > listTop && ry < listBottom) {
				NpcTitle title = NpcTitle.values()[Math.max(0, Math.min(NpcTitle.values().length - 1, m.title()))];
				Component realm = m.rank() < 0 ? Component.translatable(ModLang.SECT_TAB_OFFLINE)
						: info.body() ? BodyPath.rankName(m.rank()) : PlayerCultivation.rankName(CultivatorNpc.realmOf(m.rank()), CultivatorNpc.stageOf(m.rank()));
				int crossX = left + WIDTH - 23;
				int realmRight = crossX - 4;
				g.drawString(font, realm, realmRight - font.width(realm), ry + 1, 0x9E9E9E, false);
				Component who = Component.translatable(ModLang.NPC_TITLE_BRACKETS, title.getDisplayName(info.clan(), info.holy(), info.immortal()))
						.append(" ").append(m.player() ? Component.translatable(ModLang.CORE_SECT_PLAYER, m.name()) : Component.literal(m.name()));
				int room = realmRight - font.width(realm) - 6 - (left + 14);
				g.drawString(font, font.plainSubstrByWidth(who.getString(), Math.max(0, room)), left + 14, ry + 1, m.player() ? 0x8FEAE4 : title.getColor(), false);
				// The cross: pressed once it turns to a red question; again within 3 s, they are cast out.
				boolean shown = ry >= listTop && ry + 9 <= listBottom;
				boolean armed = m.id().equals(expelArmed) && Util.getMillis() - expelArmedAt < 3000;
				Cross cross = new Cross(crossX, ry, m.id(), m.name());
				boolean over = shown && cross.contains(mouseX, mouseY);
				HallHud.panel(g, crossX, ry, crossX + 9, ry + 9, over || armed ? HallHud.FRAME_LIT : HallHud.FRAME, 0xFF0B0E12);
				g.drawString(font, armed ? "?" : "x", crossX + 2, ry + 1, armed ? 0xFF6E6E : over ? 0xFFFFFF : MUTED, false);
				if (shown) crosses.add(cross);
				if (over) hover = Component.translatable(ModLang.CORE_SECT_EXPEL, m.name());
			}
			ry += rowH;
		}
		g.disableScissor();
		sectHover = hover;
	}

	/** The tooltip of the cross pointed at on the Sect page this frame (drawn over the widgets), or null. */
	private Component sectHover;

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
