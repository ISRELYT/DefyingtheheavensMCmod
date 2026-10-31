package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.AlchemyRecipes;
import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.CelestialCourt;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.PillItem;
import com.example.defyingtheheavens.CultivationBoost;
import com.example.defyingtheheavens.CultivationStats;
import com.example.defyingtheheavens.DemonPath;
import com.example.defyingtheheavens.GhostMethod;
import com.example.defyingtheheavens.GhostMethods;
import com.example.defyingtheheavens.HeavenMethod;
import com.example.defyingtheheavens.HeavenMethods;
import com.example.defyingtheheavens.HeavenPassives;
import com.example.defyingtheheavens.HeavenState;
import com.example.defyingtheheavens.MethodLayers;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.GhostQi;
import com.example.defyingtheheavens.NetherworldState;
import com.example.defyingtheheavens.Spell;
import com.example.defyingtheheavens.Spells;
import com.example.defyingtheheavens.Journal;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.MortalStage;
import com.example.defyingtheheavens.BodyRefining;
import com.example.defyingtheheavens.Physique;
import com.example.defyingtheheavens.Physiques;
import com.example.defyingtheheavens.FourAxis;
import com.example.defyingtheheavens.CraftMastery;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.RealmSuppressSystem;
import com.example.defyingtheheavens.RingOfPowerItem;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.StatPoints;
import com.example.defyingtheheavens.TwoPaths;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.HeavenRitual;
import com.example.defyingtheheavens.NpcTitle;
import com.example.defyingtheheavens.Regard;
import com.example.defyingtheheavens.SectMembership;
import com.example.defyingtheheavens.SectMissions;
import com.example.defyingtheheavens.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToDoubleBiFunction;

/**
 * The cultivation menu: one panel with six tabs, left to right Cultivation (progress), Stats, Abilities, Methods, Spells and
 * Journal (the rumours cultivators have told you, see Journal). The previous single-page layout is kept in
 * backups/cultivation-menu-2026-10-09.
 */
public class CultivationScreen extends Screen implements HallFrame.Framed {
	private static final int WIDTH = 310; // room for seven tabs (the smallest usual GUI is 427 wide)
	private static final int HEIGHT = 230; // fits the smallest usual GUI height (240)
	private static final int TAB_Y = 6;
	private static final int TAB_HEIGHT = 19;
	private static final int CONTENT_Y = 34;
	private static final int SWITCH_W = 30;
	private static final int SWITCH_H = 14;
	/** Pixels the Abilities and Journal tabs move per mouse-wheel notch. */
	private static final int SCROLL_STEP = 14;
	/** The scrollbar of those two tabs: its width and where it starts from the panel's left; their lists end short of it. */
	private static final int SCROLLBAR_W = 6;
	private static final int SCROLLBAR_X = WIDTH - 11;
	private static final int LIST_RIGHT = SCROLLBAR_X - 6;
	/** The Abilities tab's content stops above its button (Arrange Wheel / Ability List). */
	private static final int ABILITIES_BOTTOM = HEIGHT - 38;
	/** The wheel editor: slot ring, slot radius, and the ability palette's cells. */
	private static final int EDIT_RING = 52;
	private static final int EDIT_SLOT = 12;
	private static final int EDIT_BACKDROP = EDIT_RING + EDIT_SLOT + 5;
	private static final int CELL = 22;
	private static final int CELL_GAP = 4;
	private static final int PALETTE_X = 162;
	/** Right edges of the Base and Realm columns in the Stats tab (Current ends at the panel's inner margin). */
	private static final int BASE_RIGHT = 138;
	private static final int BONUS_RIGHT = 190;

	private static final int BORDER = 0xFFB8860B;
	private static final int PANEL = HallHud.PANEL;
	private static final int TAB_IDLE = HallHud.CARD;
	private static final int TAB_HOVER = HallHud.ROW_LIT;
	/** Qi gathering boosted by the Upper Realm, in the Qi bar's blue. */
	private static final int QI_UPPER_COLOR = 0x64B5F6;

	private enum Tab {
		CULTIVATION(ModLang.TAB_CULTIVATION),
		STATS(ModLang.TAB_STATS),
		ABILITIES(ModLang.TAB_ABILITIES),
		METHODS(ModLang.TAB_METHODS),
		SPELLS(ModLang.TAB_SPELLS),
		SECT(ModLang.TAB_SECT),
		JOURNAL(ModLang.TAB_JOURNAL);

		final String key;

		Tab(String key) {
			this.key = key;
		}
	}

	/** Reopening the menu returns to the tab it was closed on, and the Abilities tab to its list or its wheel editor. */
	private static Tab tab = Tab.CULTIVATION;
	private static boolean arranging;

	private int left;
	private int top;
	/**
	 * The Hall (the user's picks, 2026-10-30, "the fullscreen version ... of the cultivation menu"): the whole screen in ink, the tabs
	 * down a column on the left under the cultivator's name and realm, and the Cultivation tab a panel of its own beside the Ladder of
	 * Realms; the other tabs' panels beside the column. Since 2026-10-31 the only layout (the frame always has room for it, HallFrame).
	 * The Cultivation tab's panel in the frame's pixels: its left, top, width and height.
	 */
	private int cultX, cultY, cultW, cultH;
	/** How far the Cultivation tab's cards and lines are scrolled in the Hall, and how far they can go (0: they all fit): the artifact's pixels. */
	private int cultScroll, cultMaxScroll;
	/** The Hall's column of tabs, the mockup's 264 of the artifact's pixels (88 of the frame's: HallFrame). */
	private static final int NAV_W = 88;
	/**
	 * The menu's mockup in the artifact's pixels (HallArt): its column's rows (from 124, 54 apart), the Cultivation tab's panel (580, 20,
	 * 680 across); and how far right of the mockup's places the ladder and the panel stand in a frame wider than its 1280.
	 */
	private static final int A_ROW_TOP = 124, A_ROW = 54, A_PANEL_X = 580, A_PANEL_Y = 20, A_PANEL_W = 680;
	private float ox;
	private static final int NAV_INK = HallHud.NAV_INK, HALL_FRAME = HallHud.FRAME, CARD = 0xFF0D1116;
	/**
	 * The Cultivation tab's field for the title one takes (PlayerTitles, from Void Shattering on: the user's ask, 2026-10-27), over its
	 * columns, Enter to set it; and the title it was last filled with from the server.
	 */
	private net.minecraft.client.gui.components.EditBox titleBox;
	private String titleShown = "";
	private Button meditateButton;
	private Button breakthroughButton;
	/** The Cultivation tab's switch between the two qi (TwoPaths#switchPath): which path a meditation cultivates. */
	private Button pathButton;
	/**
	 * The Cultivation tab's pace for meditating (x1 to x3, beside Meditate; Lifespan#cycleMeditationSpeed), its price in years told by
	 * its tooltip; and the pace the tooltip was last made for.
	 */
	private Button paceButton;
	private int paceShown;
	private Button wheelButton;
	/** The wheel editor's palette as drawn this frame, for {@link #mouseClicked}. Empty unless arranging. */
	private final List<PaletteCell> palette = new ArrayList<>();
	/** The ability being dragged in the wheel editor (null: none), and the slot it was lifted from (-1: the palette). */
	private Ability dragged;
	private int dragFrom = -1;
	/** Where each ability's switch was drawn this frame, for {@link #mouseClicked}. Empty on other tabs. */
	private final List<AbilitySwitch> switches = new ArrayList<>();
	/** How far the Abilities tab is scrolled, in pixels, and how far it can go (0 when everything fits). */
	private int abilityScroll;
	private int abilityMaxScroll;
	/** The same for the Journal tab. */
	private int journalScroll;
	private int journalMaxScroll;
	/** Where the scrollbar was drawn this frame (null: none on this tab), and where its thumb is held (-1: not held). */
	private ScrollBar bar;
	private int barGrab = -1;
	/** The Methods and Spells tabs' scrolling, as the Abilities tab's. */
	private int methodScroll;
	private int methodMaxScroll;
	private int spellScroll;
	private int spellMaxScroll;
	/** The Spells tab shows its wheel editor rather than its list (kept on reopening, as the Abilities tab's). */
	private static boolean arrangingSpells;
	private Button spellWheelButton;
	/**
	 * The Spells tab's switch for whether spells change the land for good (ModGameRules#SPELL_GRIEFING, the gamerule spellGriefing; the
	 * user's word, 2026-10-25: "I need that grief toggle idk where it is I couldn't find it"), at its foot to the left: shown to all,
	 * turned only by an operator or the world's host.
	 */
	private Button griefButton;
	private static final int GRIEF_W = 84;
	/** Which tooltip the switch was last given (0 none yet, 1 the switch's, 2 the locked one's). */
	private int griefTipFor;
	/** The Methods tab's Practise buttons and the Spells tab's Ready buttons, as drawn this frame, for {@link #mouseClicked}. */
	private final List<RowButton<GhostMethod>> practiseButtons = new ArrayList<>();
	private final List<RowButton<Spell>> readyButtons = new ArrayList<>();
	/** Life Force Drain's and Gain's mode switches (what they move: health, qi or years; WoodSpells.LifeMode), as drawn this frame. */
	private final List<RowButton<Spell>> lifeModeButtons = new ArrayList<>();
	/** The Methods tab's heaven page's Practise buttons, and the pages' own buttons (their {@link #methodPage}). */
	private final List<RowButton<HeavenMethod>> heavenPractiseButtons = new ArrayList<>();
	private final List<RowButton<Integer>> methodPageButtons = new ArrayList<>();
	/** Which page the Methods tab shows (kept on reopening): the ghost path's methods, the heaven path's, or the Laws (phase 3). */
	private static final int PAGE_GHOST = 0, PAGE_HEAVEN = 1, PAGE_LAWS = 2;
	private static int methodPage = PAGE_GHOST;
	/** The Laws page's Focus and +1 buttons, as drawn this frame (the Law they act on). */
	private final List<RowButton<com.example.defyingtheheavens.Law>> lawFocusButtons = new ArrayList<>();
	private final List<RowButton<com.example.defyingtheheavens.Law>> lawChooseButtons = new ArrayList<>();
	/** The spell wheel editor's palette as drawn this frame, the spell being dragged in it, and the slot it came from (-1: the palette). */
	private final List<SpellCell> spellPalette = new ArrayList<>();
	private Spell draggedSpell;
	private int spellDragFrom = -1;
	/** How far the spell wheel editor's palette is scrolled, and how far it can go (0 when every spell fits). */
	private int spellPaletteScroll;
	private int spellPaletteMaxScroll;
	/**
	 * The Stats tab's page shown (kept on reopening): 0 its stats, 1 its Stat Points, 2 the crafts' mastery (CraftMastery); its page
	 * buttons and + buttons this frame.
	 */
	private static int statPage;
	private final List<RowButton<Integer>> statPageButtons = new ArrayList<>();
	private final List<RowButton<StatPoints.Stat>> statPlusButtons = new ArrayList<>();
	/** The Sect tab's member list's scrolling. */
	private int sectScroll;
	private int sectMaxScroll;
	/**
	 * The Sect tab's buttons for a soul in a member's body (SectMembership#leaveAsBody, #banish): Leave ({@link #LEAVE_SECT}) and each
	 * Banish (the member's id); the first click on one asks "Sure?" for {@link #CONFIRM_MS}, the second does it.
	 */
	private final List<RowButton<UUID>> sectButtons = new ArrayList<>();
	private static final UUID LEAVE_SECT = new UUID(0, 0);
	private static final long CONFIRM_MS = 3000;
	private UUID sectConfirm;
	private long sectConfirmAt;

	/** A small button on a row of a list (Practise, Ready): what it acts on, and where it was drawn. */
	private record RowButton<T>(T value, int x, int y, int w) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + SWITCH_H;
		}
	}

	private record SpellCell(Spell spell, int x, int y) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
		}
	}

	private record AbilitySwitch(Ability ability, int x, int y) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + SWITCH_W && mouseY >= y && mouseY < y + SWITCH_H;
		}
	}

	/** The scrollbar's thumb runs from laneTop to laneBottom; a pixel either side of the trough still counts as on it. */
	private record ScrollBar(int x, int laneTop, int laneBottom, int thumbY, int thumbHeight, int maxScroll) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x - 1 && mouseX < x + SCROLLBAR_W + 1 && mouseY >= laneTop && mouseY < laneBottom;
		}

		boolean onThumb(double mouseY) {
			return mouseY >= thumbY && mouseY < thumbY + thumbHeight;
		}

		/** The scroll that puts the thumb's top at {@code thumbTop}. */
		int scrollFor(double thumbTop) {
			int travel = laneBottom - laneTop - thumbHeight;
			return travel <= 0 ? 0 : Mth.clamp((int) Math.round((thumbTop - laneTop) * maxScroll / travel), 0, maxScroll);
		}
	}

	private record PaletteCell(Ability ability, int x, int y) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
		}
	}

	public CultivationScreen() {
		super(Component.translatable(ModLang.TITLE));
	}

	@Override
	protected void init() {
		// The frame is never narrower than 427 of its pixels (HallFrame), so the Hall always has room: the other tabs' panel beside the
		// column; the menu's mockup in the artifact's pixels (three to one of the frame's), its ladder and the Cultivation tab's panel kept
		// together in the middle of a frame wider than its 1280.
		left = NAV_W + (width - NAV_W - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		ox = Math.max(0, (width * 3f - HallArt.W) / 2);
		cultX = Math.round((A_PANEL_X + ox) / 3);
		cultY = A_PANEL_Y / 3;
		cultW = A_PANEL_W / 3;
		cultH = Math.round((height * 3f - 2 * A_PANEL_Y) / 3);

		// The tabs down the column, one under another as the mockup has them, the Dao Codex at their foot (CodexScreen comes back here).
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) addRenderableWidget(new TabButton(0, navTop(i), 87, 16, tabs[i]));
		addRenderableWidget(new CodexLink(0, navTop(tabs.length), 87, 16));

		meditateButton = addRenderableWidget(HallButton.of(Component.empty(),
						b -> ClientPlayNetworking.send(ModPackets.TOGGLE_MEDITATION, PacketByteBufs.create()))
				.bounds(cultX + 12, cultY + cultH - 32, 112, 20).build());

		breakthroughButton = addRenderableWidget(HallButton.of(Component.translatable(ModLang.BTN_BREAKTHROUGH),
						b -> ClientPlayNetworking.send(ModPackets.BREAKTHROUGH, PacketByteBufs.create()))
				.bounds(cultX + cultW - 124, cultY + cultH - 32, 112, 20).build());

		pathButton = addRenderableWidget(HallButton.of(Component.empty(), b -> ClientMethods.switchPath())
				.bounds(cultX + (cultW - 112) / 2, cultY + cultH - 32, 112, 20).build());
		pathButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(ModLang.HALL_PATH_TIP)));

		paceButton = addRenderableWidget(HallButton.of(Component.empty(),
						b -> ClientPlayNetworking.send(ModPackets.MEDITATION_SPEED, PacketByteBufs.create()))
				.bounds(cultX + 12 + 112 + BUTTON_GAP, cultY + cultH - 32, PACE_W, 20).build());
		paceShown = 0;
		griefTipFor = 0;

		wheelButton = addRenderableWidget(HallButton.of(Component.empty(), b -> {
					arranging = !arranging;
					dragged = null;
					updateButtons();
				})
				.bounds(left + (WIDTH - 112) / 2, top + HEIGHT - 32, 112, 20).build());

		spellWheelButton = addRenderableWidget(HallButton.of(Component.empty(), b -> {
					arrangingSpells = !arrangingSpells;
					draggedSpell = null;
					updateButtons();
				})
				.bounds(left + (WIDTH - 112) / 2, top + HEIGHT - 32, 112, 20).build());

		griefButton = addRenderableWidget(HallButton.of(Component.empty(), b -> {
					FriendlyByteBuf buf = PacketByteBufs.create();
					buf.writeBoolean(!ClientMethods.griefing);
					ClientPlayNetworking.send(ModPackets.SPELL_GRIEFING, buf);
				})
				.bounds(left + 12, top + HEIGHT - 32, GRIEF_W, 20).build());

		int labelW = font.width(Component.translatable(ModLang.TITLE_LABEL)) + 8;
		titleBox = addRenderableWidget(new net.minecraft.client.gui.components.EditBox(font, cultX + 16 + labelW, cultY + 10,
				cultW - 32 - labelW, 10, Component.translatable(ModLang.TITLE_LABEL)));
		titleBox.setBordered(false); // framed in the panel's own gold (renderCultivation)
		titleBox.setMaxLength(com.example.defyingtheheavens.PlayerTitles.MAX_LENGTH);
		titleShown = ClientCultivationData.get().epithet();
		titleBox.setValue(titleShown);
		titleBox.setTextColor(ClientPlates.TITLE_COLOR);
		titleBox.setHint(Component.translatable(ModLang.TITLE_HINT).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
		titleBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(ModLang.TITLE_TIP)));
		// The mockup's row of buttons under the panel, and its title field (its words in the field's ink, past its border and margin).
		float buttonsY = height * 3f - 108, buttonsX = A_PANEL_X + 24 + ox;
		((HallButton) meditateButton).art(buttonsX, buttonsY, 196, 50, 22).primary(() -> !ClientCultivationData.isMeditating());
		((HallButton) paceButton).art(buttonsX + 208, buttonsY, 60, 50, 22);
		((HallButton) pathButton).art(buttonsX + 280, buttonsY, 176, 50, 21);
		((HallButton) breakthroughButton).art(buttonsX + 468, buttonsY, 164, 50, 20).primary(() -> breakthroughButton.active);
		titleBox.setX(Math.round((A_PANEL_X + 92 + 13 + ox) / 3));
		titleBox.setY(Math.round(57 / 3f) - 5);
		titleBox.setWidth(Math.round((564 - 26) / 3f));

		updateButtons();
		if (tab == Tab.JOURNAL) ClientJournal.request();
		if (tab == Tab.SECT) ClientSect.request();
	}

	/** A row of the Hall's column of tabs: its top in the frame's pixels (the mockup's rows 54 apart from 124, 48 tall). */
	private static int navTop(int row) {
		return Math.round((A_ROW_TOP + A_ROW * row) / 3f);
	}

	@Override
	public void tick() {
		updateButtons();
		// The title as the server last told it, while one isn't typing in its field.
		String told = ClientCultivationData.get().epithet();
		if (titleBox != null && !titleBox.isFocused() && !told.equals(titleShown)) {
			titleShown = told;
			titleBox.setValue(told);
		}
	}

	/** The title typed into the Cultivation tab's field, sent to be taken (C2S set_title; emptied, cleared). */
	private void sendTitle(String title) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(title, com.example.defyingtheheavens.PlayerTitles.MAX_LENGTH * 4);
		ClientPlayNetworking.send(ModPackets.SET_TITLE, buf);
	}

	/** Meditate and Breakthrough belong to the Cultivation tab; Arrange Wheel to the Abilities tab, once one is awakened. */
	private void updateButtons() {
		if (meditateButton == null) return;
		boolean progressTab = tab == Tab.CULTIVATION;
		meditateButton.visible = progressTab;
		breakthroughButton.visible = progressTab;
		wheelButton.visible = tab == Tab.ABILITIES && !awakened().isEmpty();
		wheelButton.setMessage(Component.translatable(arranging ? ModLang.BTN_ABILITY_LIST : ModLang.BTN_ARRANGE_WHEEL));
		spellWheelButton.visible = tab == Tab.SPELLS && ClientMethods.anyKnown();
		spellWheelButton.setMessage(Component.translatable(arrangingSpells ? ModLang.BTN_SPELL_LIST : ModLang.BTN_ARRANGE_WHEEL));
		griefButton.visible = tab == Tab.SPELLS;
		griefButton.active = ClientMethods.mayChangeGriefing;
		griefButton.setMessage(Component.translatable(ClientMethods.griefing ? ModLang.BTN_GRIEFING_ON : ModLang.BTN_GRIEFING_OFF));
		boolean mayGrief = ClientMethods.mayChangeGriefing;
		if (griefTipFor != (mayGrief ? 1 : 2)) {
			griefTipFor = mayGrief ? 1 : 2;
			griefButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(mayGrief ? ModLang.TIP_GRIEFING : ModLang.TIP_GRIEFING_LOCKED)));
		}
		meditateButton.setMessage(Component.translatable(
				ClientCultivationData.isMeditating() ? ModLang.HALL_STOP : ModLang.BTN_MEDITATE));
		PlayerCultivation c = ClientCultivationData.get();
		// The title's field, from Void Shattering on, on the Cultivation tab (PlayerTitles).
		if (titleBox != null) {
			titleBox.visible = progressTab && com.example.defyingtheheavens.PlayerTitles.mayTake(c);
			if (!titleBox.visible && titleBox.isFocused()) titleBox.setFocused(false);
		}
		// The path whose qi is drawn in breaks through: the heaven path's tribulation, the Netherworld's (the ghost path's), or the
		// heavens' harder one (the Demon Path's).
		TwoPaths.Path drawn = TwoPaths.drawnIn(c);
		breakthroughButton.active = (drawn == TwoPaths.Path.GHOST ? GhostMethods.canBreakthrough(c) : drawn == TwoPaths.Path.DEMON ? DemonPath.canBreakthrough(c)
				: c.canBreakthrough()) && !ClientTribulationData.isActive();
		meditateButton.active = !c.isMortal() || DemonPath.rank(c) >= 0; // a mortal has no qi to gather (a demon's core has)
		// The pace, for whoever can meditate to some end (a body refined, a shattered one too), right of Meditate.
		boolean pace = progressTab && (!c.isMortal() || c.hasShatteredMeridians());
		paceButton.visible = pace;
		paceButton.setMessage(Component.translatable(ModLang.BTN_PACE, c.meditationSpeed()));
		if (paceShown != c.meditationSpeed()) {
			paceShown = c.meditationSpeed();
			paceButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(ModLang.TIP_PACE[paceShown - 1])));
		}
		// The switch, for the living on both paths (the dead draw in only ghostly qi), between the two: then three share the row.
		pathButton.visible = progressTab && TwoPaths.drawable(c).size() >= 2 && !c.netherworld().ghost();
		// The mockup's row (its places fixed in init: HallButton#art): "Path: Heaven" as it has it.
		pathButton.setMessage(Component.translatable(drawn == TwoPaths.Path.GHOST ? ModLang.BTN_PATH_GHOST
				: drawn == TwoPaths.Path.DEMON ? ModLang.BTN_PATH_DEMON : ModLang.BTN_PATH_HEAVEN));
	}

	/** The pace button's width before its place in the mockup's row is set (HallButton#art), and the gap before it. */
	private static final int BUTTON_GAP = 5;
	private static final int PACE_W = 30;

	@Override
	public boolean isPauseScreen() {
		return false; // meditation keeps running while the menu is open
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Typing a title: Enter takes it; every other key but Escape is the field's (the menu's own key types a letter there).
		if (titleBox != null && titleBox.visible && titleBox.isFocused()) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
				sendTitle(titleBox.getValue());
				titleBox.setFocused(false);
				setFocused(null);
				return true;
			}
			if (keyCode != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
				titleBox.keyPressed(keyCode, scanCode, modifiers);
				return true;
			}
		}
		if (ModKeybinds.OPEN_MENU.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		if (ModKeybinds.CODEX.matches(keyCode, scanCode) && minecraft != null) {
			minecraft.setScreen(new CodexScreen(this));
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		renderHallFrame(g);

		// The Cultivation tab is a panel of its own (renderCultivationHall); every other tab, the Hall's panel in the artifact's pixels,
		// headed by the tab's name as the mockup heads a path's card.
		if (tab != Tab.CULTIVATION) {
			HallArt.beginFramed(g);
			try {
				float px = left * 3f, py = top * 3f, rule = (top + TAB_Y + TAB_HEIGHT - 1) * 3f;
				HallArt.box(g, px - 3, py - 3, WIDTH * 3f + 6, HEIGHT * 3f + 6, 3, HALL_FRAME, 0xFF11161C);
				HallArt.diamond(g, px + 21, py + 28, 12, 0xFFFFD54F);
				HallArt.text(g, Component.translatable(tab.key).getString(), px + 45, py + 22, 26, 26, 0xFFD54F, false);
				HallArt.rect(g, px + 21, rule, px + WIDTH * 3f - 21, rule + 3, HALL_FRAME);
			} finally {
				HallArt.end(g);
			}
		}

		Player player = minecraft == null ? null : minecraft.player;
		switches.clear();
		palette.clear();
		practiseButtons.clear();
		readyButtons.clear();
		lifeModeButtons.clear();
		heavenPractiseButtons.clear();
		methodPageButtons.clear();
		lawFocusButtons.clear();
		lawChooseButtons.clear();
		spellPalette.clear();
		statPageButtons.clear();
		statPlusButtons.clear();
		sectButtons.clear();
		bar = null;
		switch (tab) {
			case CULTIVATION -> renderCultivationHall(g, player, mouseX, mouseY);
			case STATS -> {
				// A mortal on neither path has only a tempered body; a mortal's ghost on the ghost path, that path's strength.
				if (ClientCultivationData.get().isMortal() && !TwoPaths.body(ClientCultivationData.get(), false).ghostPath()) {
					renderMortalStats(g);
				} else {
					renderStatPages(g, mouseX, mouseY);
					if (statPage == 1) renderStatPoints(g, mouseX, mouseY);
					else if (statPage == 2) renderCrafts(g);
					else renderStats(g, player);
				}
			}
			case ABILITIES -> renderAbilities(g, mouseX, mouseY);
			case METHODS -> renderMethods(g, mouseX, mouseY);
			case SPELLS -> renderSpells(g, mouseX, mouseY);
			case SECT -> renderSect(g, mouseX, mouseY);
			case JOURNAL -> renderJournal(g, mouseX, mouseY);
		}

		super.render(g, mouseX, mouseY, partialTick);
		// The ability (or spell) being dragged in a wheel editor rides on the pointer, over everything.
		if (dragged != null) AbilityWheel.drawIcon(g, dragged, mouseX - 8, mouseY - 8, 16, true);
		if (draggedSpell != null) SpellWheelScreen.drawIcon(g, draggedSpell, mouseX - 8, mouseY - 8, 16, true);
	}

	// --- Cultivation: realm, stage, cultivation progress and what comes next (the Hall's panel: renderCultivationHall) ---

	/** The paths the Cultivation tab shows, in their order: the heaven path, a demon's Demon Path, the ghost path once begun. */
	private static List<TwoPaths.Path> columns(PlayerCultivation c) {
		List<TwoPaths.Path> columns = new ArrayList<>();
		columns.add(TwoPaths.Path.HEAVEN);
		if (DemonPath.rank(c) >= 0) columns.add(TwoPaths.Path.DEMON);
		if (TwoPaths.ghostRank(c, false) >= 0) columns.add(TwoPaths.Path.GHOST);
		return columns;
	}

	/**
	 * The Cultivation tab's details: the ghostly qi here and one's yin, pills at work (a Qi Gathering Pill's boost, qi not yet refined,
	 * how much less the next Cultivation Pill will do), one's path (Alignment) in its colour, the two paths' harmony, Heaven's Favour from
	 * a generous offering at the Ritual to the Heavens.
	 */
	private List<Detail> cultivationDetails(PlayerCultivation c, Player player, HeavenMethod method, double boost) {
		List<Detail> details = netherworldDetails();
		// What the rest of the realm's road costs in years by meditation (Lifespan#seclude: the talent race), against the years left.
		Detail seclusion = seclusionDetail(c, player, method, boost);
		if (seclusion != null) details.add(seclusion);
		// The practised method's next layer settling in behind the realm (HeavenMethods#settle), and how far.
		if (method != null && HeavenMethods.settling(c, method)) {
			int next = c.heaven().layer(method) + 1;
			int percent = (int) Math.floor(Mth.clamp(c.heaven().progress(method) / HeavenMethods.settleCost(next), 0, 1) * 100);
			details.add(new Detail(Component.translatable(ModLang.SETTLING_LINE, next, percent), 0xF0D78C));
		}
		if (c.isInUpperRealm()) {
			details.add(new Detail(Component.translatable(ModLang.UPPER_REALM_LINE, (int) PlayerCultivation.UPPER_REALM_QI_MULTIPLIER), 0x7FDBFF));
		} else if (c.getPlaceQi() == PlayerCultivation.GREAT_VOID_QI_MULTIPLIER) {
			details.add(new Detail(Component.translatable(ModLang.GREAT_VOID_LINE, (int) PlayerCultivation.GREAT_VOID_QI_MULTIPLIER), 0xC8A8FF));
		} else if (c.getPlaceQi() == PlayerCultivation.IMMORTAL_QI_MULTIPLIER) {
			details.add(new Detail(Component.translatable(ModLang.IMMORTAL_REALM_LINE, (int) PlayerCultivation.IMMORTAL_QI_MULTIPLIER), 0xF5D47A));
		} else if (Math.abs(c.getPlaceQi() - 1) > 1e-6) {
			// The Astral Realm's places: space's thin qi, a planet's own.
			details.add(new Detail(Component.translatable(ModLang.ASTRAL_QI_LINE, String.format(Locale.ROOT, "%.2f", c.getPlaceQi())),
					c.getPlaceQi() > 1 ? 0x7FDBFF : 0xB0B0C8));
		}
		if (c.getQiBoost() > 0) details.add(new Detail(Component.translatable(ModLang.QI_BOOST_LINE, Math.round(c.getQiBoost() * 100)), 0x64B5F6));
		if (c.getMedicinalQi() >= 0.5) details.add(new Detail(Component.translatable(ModLang.UNREFINED_LINE, num(c.getMedicinalQi())), 0xB2EBF2));
		if (c.getPillResistance() > 0.005) {
			details.add(new Detail(Component.translatable(ModLang.PILL_RESISTANCE_LINE, Math.round(c.getPillResistance() * 100)), 0x9E9E9E));
		}
		details.add(new Detail(Component.translatable(ModLang.ALIGNMENT_LINE, Alignment.describe(c.getAlignment())), 0xA0A0A0));
		if (TwoPaths.stageGap(c) >= 0 && TwoPaths.harmony(c) > 0) {
			details.add(new Detail(Component.translatable(ModLang.TWO_PATHS_LINE, Math.round(TwoPaths.harmony(c) * 100)), 0x8FEAE4));
		}
		if (c.getFavour() > 0) {
			details.add(new Detail(Component.translatable(ModLang.FAVOUR_LINE, Math.round((c.favourFactor() - 1) * 100)), 0xFFE082));
		}
		return details;
	}

	/**
	 * Under the details, what holds the cultivator (another's pressure, their own Realm Suppress); else what stands next on the heaven
	 * path, and on both paths the ghost path's next after it, across the whole width (a column's half would wrap a pill's recipe and a
	 * method's gate into a dozen lines).
	 */
	private List<Line> cultivationStatus(PlayerCultivation c, int width) {
		List<Line> status = heldStatus(c, width);
		if (status.isEmpty()) {
			status = heavenNext(c, width);
			status.addAll(demonNext(c, width));
			if (TwoPaths.ghostRank(c, false) >= 0 && TwoPaths.heavenRank(c, false) >= 0) {
				// What the heaven path's has said already (an axis of virtue both paths stand at: FourAxis) is said once; its bar framed
				// in amber if either path's breakthrough waits on it.
				Set<String> said = new HashSet<>();
				for (Line line : status) if (line.said() != null) said.add(line.said());
				for (Line line : ghostNext(c, width)) {
					if (line.said() == null || !said.contains(line.said())) status.add(line);
					else if (line.meter() != null && line.color() != 0) status.replaceAll(l -> line.said().equals(l.said()) ? line : l);
				}
			}
		}
		return status;
	}

	// --- The Hall: the whole screen, the tabs down a column, the Cultivation tab beside the Ladder of Realms ---

	/** The mockup's two ranges of far hills (its SVG polygons: x along its 1280, y down from 220 above the foot), the nearer the darker. */
	static final float[][] FAR_HILLS = {{0, 150}, {120, 90}, {210, 140}, {330, 60}, {470, 150}, {560, 110}, {700, 170}, {820, 80}, {960, 150},
			{1080, 100}, {1280, 160}};
	static final float[][] NEAR_HILLS = {{0, 190}, {160, 140}, {300, 190}, {420, 130}, {600, 200}, {760, 150}, {900, 200}, {1060, 150},
			{1280, 200}};

	/**
	 * The Hall's ground as the menu's mockup draws it (in the artifact's pixels): the screen in ink, the far hills along its foot, and the
	 * column of tabs 264 across under the cultivator's name and realm and a rule.
	 */
	private void renderHallFrame(GuiGraphics g) {
		float w = width * 3f, h = height * 3f;
		HallArt.beginFramed(g);
		try {
			HallArt.rect(g, 0, 0, w, h, 0xFF0C0F13);
			HallArt.ridge(g, FAR_HILLS, w / HallArt.W, h - 220, h, 0xFF11161C);
			HallArt.ridge(g, NEAR_HILLS, w / HallArt.W, h - 220, h, 0xFF0E1318);
			HallArt.rect(g, 0, 0, 261, h, NAV_INK);
			HallArt.rect(g, 261, 0, 264, h, HALL_FRAME);
			Player player = minecraft == null ? null : minecraft.player;
			if (player != null) HallArt.text(g, HallArt.fit(player.getName().getString(), 216, 26), 30, 28, 26, 26, 0xE6D3A3, false);
			String realm = standingName(ClientCultivationData.get());
			if (!realm.isEmpty()) HallArt.text(g, HallArt.fit(realm, 216, 20), 30, 62, 20, 22, 0xFFD54F, false);
			HallArt.rect(g, 24, 100, 240, 103, HALL_FRAME);
		} finally {
			HallArt.end(g);
		}
	}

	/**
	 * The Ladder of Realms beside the Cultivation tab as the menu's mockup draws it (in the artifact's pixels, within HallArt#beginFramed):
	 * a gold rail with a tick for each realm, Qi Refining at its foot and the last at its head, each named at its left; a bordered diamond
	 * on the rung of each path walked (the heaven path gold, the ghost path cyan, the Demon Path red), side by side where they share one,
	 * and that tick and name in the first's colour.
	 */
	private void renderLadder(GuiGraphics g, PlayerCultivation c) {
		int realms = Realm.values().length, stages = Stage.values().length;
		float h = height * 3f, first = 79, last = h - 31, step = (last - first) / (realms - 1);
		HallArt.text(g, Component.translatable(ModLang.HALL_LADDER).getString(), 288 + ox, 26, 20, 22, 0x8C7A55, false);
		HallArt.rect(g, 493 + ox, 80, 497 + ox, h - 28, 0xFFB8860B);
		int[] ranks = c.isMortal() ? new int[] {-1, -1, -1} : new int[] {TwoPaths.heavenRank(c, false), TwoPaths.ghostRank(c, false), TwoPaths.demonRank(c, false)};
		int[] marks = {0xFFFFD54F, 0xFF5FE6DC, 0xFFFF6A6A}, rims = {0xFF8C6A1E, 0xFF1A7A78, 0xFF7A2020}, names = {0xFFD54F, 0x8FEAE4, 0xFF8A8A};
		int[] litBy = new int[realms];
		java.util.Arrays.fill(litBy, -1);
		for (int i = 0; i < ranks.length; i++) {
			if (ranks[i] < 0) continue;
			int r = Math.min(realms - 1, ranks[i] / stages);
			if (litBy[r] < 0) litBy[r] = i;
		}
		for (int r = 0; r < realms; r++) {
			float t = last - r * step;
			int path = litBy[r];
			HallArt.rect(g, 482 + ox, t, 508 + ox, t + 3, path < 0 ? HALL_FRAME : marks[path]);
			HallArt.textRight(g, Realm.byIndex(r).getDisplayName().getString(), 476 + ox, t - 8, 17, 18, path < 0 ? 0x7D776A : names[path], false);
		}
		// Side by side 26 apart, or 20 where all three share a rung (so the third keeps clear of the panel at 580).
		int[] onRung = new int[realms], sharing = new int[realms];
		for (int rank : ranks) if (rank >= 0) sharing[Math.min(realms - 1, rank / stages)]++;
		for (int i = 0; i < ranks.length; i++) {
			if (ranks[i] < 0) continue;
			int r = Math.min(realms - 1, ranks[i] / stages);
			HallArt.diamond(g, 516 + (sharing[r] > 2 ? 20 : 26) * onRung[r]++ + ox, last - r * step - 7, 17, 3, rims[i], marks[i]);
		}
	}

	/**
	 * The Cultivation tab in the Hall (the user's picks, 2026-10-30): the title's field, the realm in large gold and its stage (or what the
	 * body comes to: TwoPaths#effective); then, scrolling under them, a card for each path walked ({@link #pathCard}), the details (a
	 * diamond in its colour and its line each), what holds the cultivator (in a crimson frame) or what stands next, and whether one
	 * meditates; the buttons fixed under them, and a scrollbar while they don't all fit ("incase all 3 paths + effects dont fit").
	 */
	private void renderCultivationHall(GuiGraphics g, Player player, int mouseX, int mouseY) {
		PlayerCultivation c = ClientCultivationData.get();
		float h = height * 3f, px = A_PANEL_X + ox;
		HallArt.beginFramed(g);
		try {
			renderLadder(g, c);
			HallArt.box(g, px, A_PANEL_Y, A_PANEL_W, h - 2 * A_PANEL_Y, 3, HALL_FRAME, NAV_INK);
			if (!c.isMortal()) cultivationPanel(g, player, c, px, h);
		} finally {
			HallArt.end(g);
		}
		if (c.isMortal()) renderMortal(g, cultX + cultW / 2, cultY + 12);
	}

	/**
	 * The panel's insides as the mockup lays them out (in the artifact's pixels): the title's field (Void Shattering on), the realm in 36
	 * and under it its stage or what the body comes to; then, scrolling between them and the buttons (clipped, a thin gold scrollbar while
	 * it overflows), a card for each path ({@link #pathCard}), the facts two to a row (a square in each one's colour), and what holds the
	 * cultivator (in a crimson box) or what stands next.
	 */
	private void cultivationPanel(GuiGraphics g, Player player, PlayerCultivation c, float px, float h) {
		boolean titled = titleBox != null && titleBox.visible;
		float dy = titled ? 0 : -58;
		if (titled) {
			HallArt.text(g, Component.translatable(ModLang.TITLE_LABEL).getString(), px + 24, 46, 20, 22, 0xE6D3A3, false);
			HallArt.box(g, px + 92, 36, 564, 42, 3, titleBox.isFocused() ? 0xFFB8860B : HALL_FRAME, 0xFF0B0E12);
		}
		int rank = Math.max(0, TwoPaths.higherRank(c, false)), stages = Stage.values().length;
		HallArt.text(g, HallArt.fit(Realm.byIndex(rank / stages).getDisplayName().getString(), 632, 36), px + 24, 94 + dy, 36, 38, 0xFFD54F, false);
		boolean stronger = TwoPaths.strongerThanRank(c);
		String under = stronger ? Component.translatable(ModLang.EFFECTIVE_LINE, TwoPaths.effectiveLabel(TwoPaths.effective(c, true))).getString()
				: Stage.byIndex(rank % stages).getDisplayName().getString();
		HallArt.text(g, HallArt.fit(under, 632, 20), px + 24, 136 + dy, 20, 22, 0xB39DDB, false);

		// The scrolling part, between the header and the buttons, clipped (in the frame's pixels: HallFrame scales the scissor).
		float ax = px + 20, top = 168 + dy, bottom = h - 122;
		g.enableScissor(Math.round(ax / 3), Math.round(top / 3), Math.round((ax + 636) / 3), Math.round(bottom / 3));
		float y = top + 6 - cultScroll, x = ax + 4;
		HeavenMethod method = HeavenMethods.practised(c.heaven());
		double boost = meditationBoost(player);
		// The mockup's order: the heaven path, the ghost path, the Demon Path.
		List<TwoPaths.Path> walked = columns(c);
		for (TwoPaths.Path path : List.of(TwoPaths.Path.HEAVEN, TwoPaths.Path.GHOST, TwoPaths.Path.DEMON)) {
			if (walked.contains(path)) y = pathCard(g, player, c, path, method, boost, x, y) + 12;
		}
		y += 4;
		List<Detail> facts = new ArrayList<>();
		Detail methodFact = methodFact(player, c, method, boost);
		if (methodFact != null) facts.add(methodFact);
		facts.addAll(cultivationDetails(c, player, method, boost));
		boolean meditating = ClientCultivationData.isMeditating();
		facts.add(new Detail(Component.translatable(meditating ? ModLang.STATUS_MEDITATING : ModLang.STATUS_IDLE), meditating ? 0x69F0AE : 0x7D776A));
		y = facts(g, facts, x, y) + 4;
		// What holds the cultivator, or what stands next: its lines wrapped as the frame's font would to 612 at the size drawn (20 of 24).
		int wrap = Math.round(612 * 24f / 20 / 3);
		boolean held = !heldStatus(c, wrap).isEmpty();
		List<Line> status = cultivationStatus(c, held ? wrap - 16 : wrap);
		if (held) {
			float boxH = 28 + status.size() * 22;
			HallArt.box(g, x, y, 612, boxH, 3, 0xFF8E2A2A, 0x993C0A0A);
			HallArt.diamond(g, x + 20, y + 19, 12, HallHud.PRESSURE);
			for (int i = 0; i < status.size(); i++) HallArt.text(g, plain(status.get(i).text()), x + 44, y + 14 + i * 22, 20, 22, 0xFF8A8A, false);
			y += boxH + 6;
		} else {
			for (Line line : status) {
				if (line.meter() != null) {
					virtueBar(g, c, line.meter(), line.color(), x, y);
					y += 32;
				} else {
					HallArt.text(g, plain(line.text()), x, y, 20, 22, line.color(), false);
					y += 22;
				}
			}
		}
		g.disableScissor();

		int content = Math.round(y + cultScroll - top + 10);
		cultMaxScroll = Math.max(0, Math.round(content - (bottom - top)));
		cultScroll = Mth.clamp(cultScroll, 0, cultMaxScroll);
		bar = null;
		if (cultMaxScroll > 0) {
			// The mockup's scrollbar: a dark trough 8 across at the panel's right, a gold thumb (dragged as the others are, in the frame's pixels).
			float lane = bottom - top, thumb = Math.max(42, lane * lane / content), thumbY = top + (lane - thumb) * cultScroll / cultMaxScroll;
			HallArt.rect(g, px + 662, top, px + 670, bottom, 0xFF0B0E12);
			HallArt.rect(g, px + 662, thumbY, px + 670, thumbY + thumb, 0xFF8C6A1E);
			bar = new ScrollBar(Math.round((px + 662) / 3), Math.round(top / 3), Math.round(bottom / 3), Math.round(thumbY / 3), Math.round(thumb / 3), cultMaxScroll);
		}
	}

	/** {@code text}'s characters, its styles left behind (drawn by HallArt, in the line's own colour). */
	private static String plain(FormattedCharSequence text) {
		StringBuilder out = new StringBuilder();
		text.accept((index, style, codePoint) -> {
			out.appendCodePoint(codePoint);
			return true;
		});
		return out.toString();
	}

	/**
	 * The facts as the mockup sets them (in the artifact's pixels): two to a row 32 apart, the second from 320 along, a square in each
	 * one's colour before its words; one too long for half the row has the row to itself. @return the y under them
	 */
	private float facts(GuiGraphics g, List<Detail> facts, float x, float y) {
		boolean open = false;
		for (Detail fact : facts) {
			String text = fact.text().getString();
			if (HallArt.width(text, 20) > 270) {
				if (open) y += 32;
				open = false;
				fact(g, x, y, 612, text, fact.color());
				y += 32;
			} else if (!open) {
				fact(g, x, y, 296, text, fact.color());
				open = true;
			} else {
				fact(g, x + 320, y, 292, text, fact.color());
				y += 32;
				open = false;
			}
		}
		return open ? y + 32 : y;
	}

	private void fact(GuiGraphics g, float x, float y, float w, String text, int color) {
		HallArt.rect(g, x, y + 5, x + 12, y + 17, 0xFF000000 | color);
		HallArt.text(g, HallArt.fit(text, w - 22, 20), x + 22, y, 20, 22, color == 0x7D776A ? 0x9E9886 : 0xE6D3A3, false);
	}

	/** The heaven method practised and its layers, or that there is none, and what meditating on the heavens' qi adds a second. */
	private Detail methodFact(Player player, PlayerCultivation c, HeavenMethod method, double boost) {
		if (TwoPaths.heavenRank(c, false) < 0) return null;
		double rate = heavenRate(player, c, method, boost);
		Component line = method == null ? Component.translatable(ModLang.METHOD_LINE_NONE, one(rate))
				: c.isMaxed() ? Component.translatable(ModLang.METHOD_BAR, method.getDisplayName(), c.heaven().layer(method), method.layers())
				: Component.translatable(ModLang.METHOD_LINE, method.getDisplayName(), c.heaven().layer(method), method.layers(), one(rate));
		return new Detail(line, boost > 1.0 && rate > 0 ? 0x7FE0A0 : 0xF0D78C);
	}

	/** What meditating on the heavens' qi adds a second here, at the pace set (0 while held down). */
	private double heavenRate(Player player, PlayerCultivation c, HeavenMethod method, double boost) {
		return c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(player)) * boost
				* HeavenMethods.cultivationFactor(c, method, method == null ? 0 : shareHere(method.element()), false) * c.meditationSpeed();
	}

	/**
	 * A path's card on the Hall's Cultivation tab: framed in gold when a meditation draws its qi in; a diamond and its name in its colour,
	 * its realm and stage, "Drawn in" on the one drawn in; its cultivation bar in its colour (the heaven path gold, the ghost path cyan,
	 * the Demon Path red; the user's ask: "a coloured bar depending on the path"), framed in amber when held and full at the very last, the stage's
	 * cultivation written on it; its pool (the qi, the ghostly qi; the Demon Path draws on the qi, TwoPaths#pool, so has none of its own);
	 * and its pace, or that it rests. @return the y under it
	 */
	private float pathCard(GuiGraphics g, Player player, PlayerCultivation c, TwoPaths.Path path, HeavenMethod method, double boost, float x, float y) {
		int stages = Stage.values().length;
		boolean drawing, maxed, held;
		int color, fill, light, rim;
		Component name, realm, stage;
		double ratio, done, required, rate;
		switch (path) {
			case GHOST -> {
				NetherworldState s = c.netherworld();
				int rank = Math.max(0, TwoPaths.ghostRank(c, false));
				drawing = TwoPaths.cultivatesGhost(c);
				color = 0x5FE6DC;
				fill = 0xFF2FB5B0;
				light = 0xFF5FE6DC;
				rim = 0xFF1A7A78;
				name = Component.translatable(ModLang.METHODS_PAGE_GHOST);
				realm = Realm.byIndex(rank / stages).getDisplayName();
				stage = Stage.byIndex(rank % stages).getDisplayName();
				done = s.pathCultivation();
				required = GhostMethods.required(rank);
				ratio = GhostMethods.stageProgress(s);
				maxed = GhostMethods.isMaxed(s);
				held = GhostMethods.atBottleneck(c);
				double density = ClientNetherworldData.density();
				rate = density <= 0 ? 0 : GhostMethods.ratePerSecond(c, density) * c.meditationSpeed();
			}
			case DEMON -> {
				int rank = Math.max(0, DemonPath.rank(c));
				drawing = TwoPaths.cultivatesDemon(c);
				color = 0xFF6A6A;
				fill = 0xFFC03131;
				light = 0xFFE05A5A;
				rim = 0xFF7A2020;
				name = Component.translatable(ModLang.METHODS_PAGE_DEMON);
				realm = DemonPath.realm(rank).getDisplayName();
				stage = DemonPath.stage(rank).getDisplayName();
				done = c.demon().cultivation();
				required = DemonPath.required(rank);
				ratio = DemonPath.stageProgress(c);
				maxed = DemonPath.isMaxed(c);
				held = DemonPath.atBottleneck(c);
				rate = DemonPath.perSecond(c, 0) * c.meditationSpeed();
			}
			default -> {
				drawing = TwoPaths.drawnIn(c) == TwoPaths.Path.HEAVEN;
				color = 0xFFD54F;
				fill = 0xFFD9A62B;
				light = 0xFFF0C75A;
				rim = 0xFF8C6A1E;
				name = Component.translatable(ModLang.METHODS_PAGE_HEAVEN);
				realm = c.getRealm().getDisplayName();
				stage = c.getStage().getDisplayName();
				done = c.getCultivation();
				required = c.cultivationRequired();
				ratio = required <= 0 ? 0 : Math.min(1.0, done / required);
				maxed = c.isMaxed();
				held = c.isStalled();
				rate = heavenRate(player, c, method, boost);
			}
		}
		boolean pool = path != TwoPaths.Path.DEMON;
		float h = pool ? 124 : 92;
		HallArt.box(g, x, y, 612, h, 3, rim, CARD);
		HallArt.diamond(g, x + 20, y + 16, 12, 0xFF000000 | color);
		String title = name.getString();
		HallArt.text(g, title, x + 44, y + 10, 26, 26, color, false);
		float standX = x + 44 + HallArt.width(title, 26) + 14, right;
		if (drawing) {
			HallArt.box(g, x + 494, y + 10, 102, 26, 2, 0xFF3E8C72, 0xFF1E5A49);
			HallArt.textCentred(g, Component.translatable(ModLang.HALL_DRAWN_IN).getString(), x + 545, y + 12, 16, 22, 0xDFF5EA, false);
			right = x + 484;
		} else {
			String resting = Component.translatable(ModLang.HALL_RESTING).getString();
			HallArt.textRight(g, resting, x + 596, y + 13, 20, 22, 0x9E9886, false);
			right = x + 584 - HallArt.width(resting, 20);
		}
		// Its realm and stage, a little smaller (down to 16) where a long one would be cut short.
		String standing = Component.translatable(ModLang.HALL_REALM_STAGE, realm, stage).getString();
		float size = HallArt.fitSize(standing, right - standX, 20, 16);
		HallArt.text(g, HallArt.fit(standing, right - standX, size), standX, y + 13, size, 22, 0xE6D3A3, false);

		// The cultivation bar: the stage's, how far along (amber framed while held; full at the very last), what meditating adds.
		if (maxed) ratio = 1;
		float bx = x + 16, by = y + 46;
		HallArt.box(g, bx, by, 580, 36, 3, held && !maxed ? 0xFFC9A227 : HALL_FRAME, 0xFF0B0E12);
		float f = HallArt.filled(574, ratio);
		if (f > 0) {
			HallArt.rect(g, bx + 3, by + 3, bx + 3 + f, by + 33, fill);
			HallArt.rect(g, bx + 3, by + 3, bx + 3 + f, by + 9, light);
		}
		if (path == TwoPaths.Path.HEAVEN && c.getMedicinalQi() > 0 && required > 0 && !c.isStalled()) {
			float pending = HallArt.filled(574, (done + c.getMedicinalQi()) / required);
			HallArt.rect(g, bx + 3 + f, by + 12, bx + 3 + pending, by + 24, 0x90B2EBF2);
		}
		String text = maxed ? Component.translatable(ModLang.HALL_CULT_MAX).getString()
				: Component.translatable(ModLang.HALL_CULT, big(done), big(required), (int) Math.floor(ratio * 100)).getString();
		if (drawing && rate > 0) text += "   " + Component.translatable(ModLang.HALL_PER_SECOND, one(rate)).getString();
		HallArt.textCentred(g, HallArt.fit(text, 560, 21), bx + 290, by + 7, 21, 22, HallArt.WHITE, true);
		if (pool) {
			// Its pool: the qi, the ghostly qi (the Demon Path draws on the qi: TwoPaths#pool).
			boolean ghost = path == TwoPaths.Path.GHOST;
			double now = ghost ? ClientNetherworldData.ghostQi() : c.getQi(), max = ghost ? ClientNetherworldData.ghostQiMax() : c.maxQi();
			float py = y + 88;
			HallArt.box(g, bx, py, 580, 26, 3, HALL_FRAME, 0xFF0B0E12);
			HallArt.rect(g, bx + 3, py + 3, bx + 3 + HallArt.filled(574, max <= 0 ? 0 : now / max), py + 23, ghost ? HallHud.GHOST : HallHud.QI);
			String amount = Component.translatable(ghost ? ModLang.HUD_GHOST_QI_LABEL : ModLang.HUD_BAR_QI).getString() + " "
					+ HallHud.compact(now) + " / " + HallHud.compact(max);
			HallArt.textCentred(g, amount, bx + 290, py + 3, 18, 20, HallArt.WHITE, true);
		}
		return y + h;
	}

	/** The realm {@code c} stands at by the higher of its paths ("" for a mortal). */
	private static String standingName(PlayerCultivation c) {
		if (c.isMortal()) return "";
		int rank = TwoPaths.higherRank(c, false);
		return rank < 0 ? "" : Realm.byIndex(rank / Stage.values().length).getDisplayName().getString();
	}

	/** {@code text} at half size from ({@code x}, {@code y}), or ending at {@code x} if {@code right}. */
	private void half(GuiGraphics g, String text, int x, int y, int color, boolean right) {
		g.pose().pushPose();
		g.pose().translate(right ? x - font.width(text) / 2f : x, y, 0);
		g.pose().scale(0.5f, 0.5f, 1);
		g.drawString(font, text, 0, 0, color, false);
		g.pose().popPose();
	}

	/**
	 * The years meditation would cost from here to the realm's Grand Perfection on the path drawn in (Lifespan#seclusionYears: by
	 * talent, what saves years here (Lifespan#saving: the meditation's boost {@code boost}, the method practised by its element's share
	 * of the qi here, a spirit stone in hand, dense qi) and the pace), against the years left; red when they won't cover it. A stele's
	 * part isn't known here, so it is left out (the server counts it). Null with no path to cultivate.
	 */
	private Detail seclusionDetail(PlayerCultivation c, Player player, HeavenMethod method, double boost) {
		if (Double.isInfinite(ClientCultivationData.lifeSpan())) return null; // a True Immortal's cultivation costs no years
		int stages = Stage.values().length;
		boolean ghost = TwoPaths.cultivatesGhost(c), demon = TwoPaths.cultivatesDemon(c);
		int rank = ghost ? GhostMethods.ghostRank(c.netherworld()) : demon ? DemonPath.rank(c) : PlayerCultivation.rank(c.getRealm(), c.getStage());
		if (rank < 0) return null;
		double stone = player == null ? 1.0 : com.example.defyingtheheavens.SpiritStoneItem.meditationMultiplier(player);
		double saving = ghost ? com.example.defyingtheheavens.Lifespan.saving(boost, 1, com.example.defyingtheheavens.Lifespan.ghostPlaceSaving(ClientNetherworldData.density()), stone)
				: demon ? com.example.defyingtheheavens.Lifespan.saving(boost, 1, com.example.defyingtheheavens.Lifespan.placeSaving(c, DemonPath.realm(rank)), stone)
				: com.example.defyingtheheavens.Lifespan.saving(boost, method == null ? 1 : HeavenMethods.elementFactor(shareHere(method.element())),
						com.example.defyingtheheavens.Lifespan.placeSaving(c), stone);
		double cultivated = ghost ? c.netherworld().pathCultivation() : demon ? c.demon().cultivation() : c.getCultivation();
		double years = 0;
		for (int r = rank; r < (rank / stages + 1) * stages; r++) {
			Realm realm = Realm.byIndex(r / stages);
			Stage stage = Stage.byIndex(r % stages);
			double need = ghost ? GhostMethods.required(r) : demon ? DemonPath.required(r) : PlayerCultivation.required(realm, stage);
			if (need <= 0) continue;
			years += com.example.defyingtheheavens.Lifespan.seclusionYears(c, realm, stage, r == rank ? Math.max(0, 1 - cultivated / need) : 1, saving);
		}
		double left = Math.max(0, ClientCultivationData.lifeSpan() - ClientCultivationData.lifeUsed());
		return new Detail(Component.translatable(ModLang.SECLUSION_LINE, String.format(Locale.ROOT, "%,d", Math.round(years)),
				String.format(Locale.ROOT, "%,d", (long) Math.floor(left))), years > left ? 0xFF6E6E : 0xC8B88A);
	}

	/** What stands next on the Demon Path, wrapped to {@code width}: its pinnacle, the lower realms' cap, an axis of virtue, or its tribulation. */
	private List<Line> demonNext(PlayerCultivation c, int width) {
		List<Line> lines = new ArrayList<>();
		int rank = DemonPath.rank(c);
		if (rank < 0) return lines;
		// A half-blood's blood, and how pure it has grown (DemonBlood: at the whole, it turns).
		if (DemonPath.isHalfBlood(c)) {
			addStatus(lines, Component.translatable(ModLang.HALF_BLOOD_LINE, DemonPath.race(c).getDisplayName(), Math.round(c.demon().purity() * 100)), 0xD06060, width);
		}
		if (DemonPath.heldByBlood(c)) {
			// The realm ahead shut to it for want of its race's blood (DemonPath#heldByBlood), as a method wanted holds the heaven path.
			addStatus(lines, Component.translatable(ModLang.DEMON_HELD_BY_BLOOD, DemonPath.realm(rank).getDisplayName(), DemonPath.race(c).getDisplayName(),
					DemonPath.realm(rank + 1).getDisplayName()), 0xFFC107, width);
			return lines;
		}
		if (DemonPath.heldByLaws(c)) { // the ladder past True Immortal shut for want of the Laws (DemonPath#heldByLaws): blood alone climbs no further
			addStatus(lines, DemonPath.lawGateLine(c), 0xFFC107, width);
			return lines;
		}
		if (DemonPath.isMaxed(c)) {
			addStatus(lines, Component.translatable(ModLang.DEMON_PINNACLE), 0xFFD700, width);
		} else if (DemonPath.atBottleneck(c) && DemonPath.locked(c)) {
			addStatus(lines, c.lockLine(rank + 1), 0xFFC107, width); // the lower realms' cap, or the Immortal Realm wanted
		} else if (FourAxis.holds(c, rank)) {
			addAxis(lines, FourAxis.wanted(rank), DemonPath.heldByAxis(c), width);
		} else if (DemonPath.canBreakthrough(c)) {
			addStatus(lines, Component.translatable(TwoPaths.cultivatesDemon(c) ? ModLang.DEMON_BOTTLENECK : ModLang.DEMON_BOTTLENECK_SWITCH,
					DemonPath.rankName(rank + 1)), 0xF08070, width);
		}
		return lines;
	}


	/**
	 * What stands next on the ghost path, wrapped to {@code width}: its pinnacle; a foundation the breakthrough waits on (said ahead
	 * in grey at Grand Perfection, amber once the bar is full); or the Netherworld's tribulation waiting at its bottleneck.
	 */
	private List<Line> ghostNext(PlayerCultivation c, int width) {
		List<Line> lines = new ArrayList<>();
		NetherworldState s = c.netherworld();
		int rank = GhostMethods.ghostRank(s);
		if (rank < 0 || c.isMortal()) return lines;
		int stages = Stage.values().length;
		if (GhostMethods.isMaxed(s)) {
			addStatus(lines, Component.translatable(ModLang.GHOST_PINNACLE), 0xFFD700, width);
		} else if (GhostMethods.heldByPlace(c)) { // into True Immortal and past it: the Immortal Realm wanted
			addStatus(lines, c.lockLine(rank + 1), 0xFF6E6E, width);
		} else if (Stage.byIndex(rank % stages).isLast() && !GhostMethods.opensWay(c, Realm.byIndex(rank / stages + 1))) {
			addStatus(lines, GhostMethods.gateLine(c), GhostMethods.heldByMethod(c) ? 0xFFC107 : 0xA0A0A0, width);
		} else if (FourAxis.holds(c, rank)) {
			// A Four Axis stage: its axis of virtue (FourAxis, the cultivator's), and how it is filled once the bar is full.
			addAxis(lines, FourAxis.wanted(rank), GhostMethods.heldByAxis(c), width);
		} else if (GhostMethods.canBreakthrough(c)) {
			// The Breakthrough button breaks through on the path drawn in: with the heavens' qi drawn in, the switch first.
			addStatus(lines, Component.translatable(TwoPaths.cultivatesGhost(c) ? ModLang.GHOST_BOTTLENECK : ModLang.GHOST_BOTTLENECK_SWITCH,
					GhostMethods.rankName(rank + 1)), 0x6FE8D8, width);
		}
		return lines;
	}

	/** What holds the cultivator now, whichever path: another cultivator's Realm Suppress (what it holds them to, what it takes), or their own. */
	private List<Line> heldStatus(PlayerCultivation c, int width) {
		List<Line> status = new ArrayList<>();
		if (c.isUnderPressure()) {
			addStatus(status, Component.translatable(ModLang.PRESSURE), 0xFF6E6E, width);
			if (c.getPressureStages() > 0) addStatus(status, PlayerCultivation.rankName(c.getEffectiveRealm(), c.getEffectiveStage()), 0xFF6E6E, width);
			addStatus(status, Component.translatable(ModLang.PRESSURE_DETAIL, Math.round(c.getPressurePenalty() * 100)), 0xFF6E6E, width);
		} else if (c.isSuppressed()) {
			addStatus(status, Component.translatable(ModLang.SUPPRESSED), 0xFF6E6E, width);
			addStatus(status, Component.translatable(ModLang.SUPPRESSED_TO, PlayerCultivation.rankName(c.getEffectiveRealm(), c.getEffectiveStage())),
					0xFF6E6E, width);
		}
		return status;
	}

	/** What stands next on the heaven path, wrapped to {@code width}: the pinnacle, or what the breakthrough ahead waits on. */
	private List<Line> heavenNext(PlayerCultivation c, int width) {
		List<Line> status = new ArrayList<>();
		if (c.isMaxed()) {
			// Heavenly Lord Grand Perfection since phase 3 (True Immortal's before; Four Axis's, with its last axis of virtue, before the
			// realms after it: 2026-10-22).
			addStatus(status, Component.translatable(ModLang.PINNACLE), 0xFFD700, width);
		} else if (c.isAtBottleneck() && c.isBreakthroughLocked()) {
			// Into True Immortal and past it, the Immortal Realm wanted; else the lower realms' cap.
			addStatus(status, c.isImmortalRealmLocked() ? c.lockLine() : Component.translatable(ModLang.BREAKTHROUGH_SEALED), 0xFF6E6E, width);
		} else if (c.isAtBottleneck() && c.isMissingBreakthroughPill()) {
			// The realm ahead needs a pill: name it and how to brew it.
			var pill = PillItem.breakthroughPillFor(c.breakthroughRealm());
			addStatus(status, Component.translatable(ModLang.NEED_PILL, pill.getDescription(), c.breakthroughRealm().getDisplayName()), 0xFFC107, width);
			addStatus(status, Component.translatable(ModLang.PILL_RECIPE, AlchemyRecipes.describe(pill)), 0xA0A0A0, width);
			// ...and a heaven method of the realm, if none is known.
			if (c.isHeldByMethod()) addStatus(status, HeavenMethods.gateLine(c), 0xFFC107, width);
		} else if (c.needsHeavensLeave()) {
			// Qi Refining Mid: Late takes the heavens' leave (the Ritual to the Heavens at a Heaven Altar).
			boolean full = c.isAtRitualGate();
			addStatus(status, Component.translatable(full ? ModLang.RITUAL_GATE : ModLang.RITUAL_AHEAD), full ? 0xFFC107 : 0xA0A0A0, width);
			if (full) addStatus(status, Component.translatable(ModLang.RITUAL_HOW), 0xA0A0A0, width);
		} else if (!c.netherworld().ghost() && c.getStage().isLast() && !c.isMaxed() && !HeavenMethods.opensWay(c, c.getRealm().next())) {
			// Grand Perfection with no heaven method known of the realm ahead: one wanted, said ahead in grey, in amber once the
			// stage is full (the breakthrough waits on it).
			addStatus(status, HeavenMethods.gateLine(c), c.isHeldByMethod() ? 0xFFC107 : 0xA0A0A0, width);
		} else if (c.getRealm() == Realm.FOUR_AXIS && FourAxis.wanted(PlayerCultivation.rank(c.getRealm(), c.getStage())) != null
				&& (!c.isMaxed() || !FourAxis.allComplete(c)) && !FourAxis.complete(c, FourAxis.wanted(PlayerCultivation.rank(c.getRealm(), c.getStage())))) {
			// A Four Axis stage: its axis of virtue (FourAxis), and how it is filled once the stage is full and the breakthrough waits on it.
			addAxis(status, FourAxis.wanted(PlayerCultivation.rank(c.getRealm(), c.getStage())), c.isHeldByAxis(), width);
		} else if (c.canBreakthrough()) {
			// With ghostly qi drawn in, the Breakthrough button breaks through on the ghost path: the switch first.
			boolean switchFirst = TwoPaths.drawnIn(c) != TwoPaths.Path.HEAVEN && !c.netherworld().ghost();
			addStatus(status, Component.translatable(switchFirst ? ModLang.BOTTLENECK_SWITCH : ModLang.BOTTLENECK), 0xFFC107, width);
			addStatus(status, Component.translatable(ModLang.BOTTLENECK_NEXT, PlayerCultivation.rankName(c.breakthroughRealm(), c.breakthroughStage())),
					0xFFC107, width);
		} else if (c.getPreparedRealm() != null) {
			// A breakthrough pill already taken, waiting for the bar to fill.
			addStatus(status, Component.translatable(ModLang.PILL_PREPARED, c.getPreparedRealm().getDisplayName()), 0x7FE0A0, width);
		}
		return status;
	}

	/** One of the Cultivation tab's details: a short line in its colour. */
	private record Detail(Component text, int color) {}

	/**
	 * A line as it is drawn (a detail, or a wrapped piece of one or of a status line) and its colour; with {@code meter}, that axis of
	 * virtue's bar in its place (its colour the bar's frame); {@code said}, what it says, so what both paths would say is said once (the
	 * axis both stand at).
	 */
	private record Line(FormattedCharSequence text, int color, FourAxis.Axis meter, String said) {
		Line(FormattedCharSequence text, int color) {
			this(text, color, null, null);
		}
	}

	/** {@code text} wrapped to {@code width}, each piece a status line in {@code color}. */
	private void addStatus(List<Line> status, Component text, int color, int width) {
		for (FormattedCharSequence line : font.split(text, width)) status.add(new Line(line, color));
	}

	/**
	 * An axis of virtue a Four Axis stage's breakthrough waits on (FourAxis): its bar, like the cultivation bar (the user's ask,
	 * 2026-10-20), framed in amber once the stage is full and the breakthrough waits on it ({@code held}), and then how it is filled.
	 * The bar's line carries its frame in its colour (0: none).
	 */
	private void addAxis(List<Line> status, FourAxis.Axis axis, boolean held, int width) {
		status.add(new Line(FormattedCharSequence.EMPTY, held ? 0xFFFFC107 : 0, axis, "meter." + axis.getId()));
		if (!held) return;
		for (FormattedCharSequence line : font.split(FourAxis.howLine(ClientCultivationData.get(), axis), width)) {
			status.add(new Line(line, 0xFFC107, null, "how." + axis.getId()));
		}
	}

	/** The axes' bars' fills: Longevity jade, Fortune gold, Merit vermilion, Enlightenment pearl. */
	private static final Map<FourAxis.Axis, Integer> AXIS_FILLS = new EnumMap<>(Map.of(FourAxis.Axis.LONGEVITY, 0xFF43A865,
			FourAxis.Axis.FORTUNE, 0xFFD9A62A, FourAxis.Axis.MERIT, 0xFFCC4535, FourAxis.Axis.ENLIGHTENMENT, 0xFFA889EE));

	/**
	 * {@code axis}'s bar in the Hall (in the artifact's pixels), as the mockup draws a card's pool bar: 612 by 26, filled in the axis's
	 * colour, framed in {@code frame} (0: the panel's dark gold), its measure written on it.
	 */
	private void virtueBar(GuiGraphics g, PlayerCultivation c, FourAxis.Axis axis, int frame, float x, float y) {
		int progress = FourAxis.progress(c, axis), need = FourAxis.need(c, axis);
		HallArt.box(g, x, y, 612, 26, 3, frame != 0 ? frame : HALL_FRAME, 0xFF0B0E12);
		HallArt.rect(g, x + 3, y + 3, x + 3 + HallArt.filled(606, progress / (double) need), y + 23, AXIS_FILLS.get(axis));
		HallArt.textCentred(g, Component.translatable(ModLang.AXIS_BAR, axis.getDisplayName(), progress, need, axis.unit()).getString(),
				x + 306, y + 3, 18, 20, HallArt.WHITE, true);
	}

	/**
	 * The effective cultivation line (TwoPaths#effectiveLabel) as it fits {@code width}: one line; else "Effective cultivation:" and
	 * the name under it (wrapped only if the name alone won't fit).
	 */
	private List<FormattedCharSequence> effectiveLines(PlayerCultivation c, int width) {
		Component label = TwoPaths.effectiveLabel(TwoPaths.effective(c, true));
		Component line = Component.translatable(ModLang.EFFECTIVE_LINE, label);
		if (font.width(line) <= width) return List.of(line.getVisualOrderText());
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.add(Component.literal(Component.translatable(ModLang.EFFECTIVE_LINE, "").getString().trim()).getVisualOrderText());
		lines.addAll(font.split(label, width));
		return lines;
	}

	/** The first of {@code texts} no wider than {@code width}; the last if none is. */
	private Component fit(int width, Component... texts) {
		for (Component text : texts) if (font.width(text) <= width) return text;
		return texts[texts.length - 1];
	}

	/** A mortal's Cultivation tab: the body's tempering stage and progress, then how to open the meridians. */
	private void renderMortal(GuiGraphics g, int cx, int y) {
		PlayerCultivation c = ClientCultivationData.get();
		if (c.hasShatteredMeridians()) {
			renderBodyRefining(g, c, cx, y);
			return;
		}
		MortalStage stage = c.getMortalStage();
		g.drawCenteredString(font, Component.translatable(ModLang.REALM, stage.getDisplayName()), cx, y, 0x7FDBFF);
		y += 18;
		int x = left + 14;
		int barW = WIDTH - 28;
		double ratio = stage.isLast() ? 1 : Math.min(1, c.getTempering() / c.temperingRequired());
		g.fill(x - 1, y - 1, x + barW + 1, y + 11, 0xFF000000);
		g.fill(x, y, x + barW, y + 10, 0xFF2A2A3A);
		g.fill(x, y, x + (int) (barW * ratio), y + 10, stage.isLast() ? 0xFFFFC107 : 0xFFE57373);
		y += 14;
		if (!stage.isLast()) {
			g.drawCenteredString(font, Component.translatable(ModLang.TEMPERING, Math.round(c.getTempering()),
					Math.round(c.temperingRequired())), cx, y, 0xE0E0E0);
			y += 16;
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.MORTAL_HOW), WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, 0xB0B0B0);
				y += 11;
			}
		} else {
			y += 4;
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.MORTAL_DETAIL), WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, 0xB0B0B0);
				y += 11;
			}
		}
		y += 8;
		for (FormattedCharSequence line : font.split(Component.translatable(ModLang.MORTAL_RECIPE,
				ModItems.MARROW_CLEANSING_ELIXIR.getDescription(), AlchemyRecipes.describe(ModItems.MARROW_CLEANSING_ELIXIR)), WIDTH - 28)) {
			g.drawCenteredString(font, line, cx, y, 0xFFC107);
			y += 11;
		}
		y += 4;
		for (Detail detail : netherworldDetails()) {
			g.drawCenteredString(font, detail.text(), cx, y, detail.color());
			y += 11;
		}
	}

	/**
	 * Shattered Meridians' Cultivation tab (BodyRefining): the mortal stage below Peak, then the body refining stage past it, its
	 * tempering bar, and what it is about.
	 */
	private void renderBodyRefining(GuiGraphics g, PlayerCultivation c, int cx, int y) {
		boolean refining = c.getMortalStage().isLast();
		Component name = refining ? BodyRefining.name(c.getBodyRefining()) : c.getMortalStage().getDisplayName();
		g.drawCenteredString(font, Component.translatable(ModLang.REALM, name), cx, y, 0xFFB74D);
		y += 18;
		int x = left + 14;
		int barW = WIDTH - 28;
		boolean done = !c.canTemper();
		double ratio = done ? 1 : Math.min(1, c.getTempering() / Math.max(1, c.temperingRequired()));
		g.fill(x - 1, y - 1, x + barW + 1, y + 11, 0xFF000000);
		g.fill(x, y, x + barW, y + 10, 0xFF2A2A3A);
		g.fill(x, y, x + (int) (barW * ratio), y + 10, done ? 0xFFFFC107 : 0xFFFF8A65);
		y += 14;
		if (!done) {
			g.drawCenteredString(font, Component.translatable(ModLang.TEMPERING, Math.round(c.getTempering()),
					Math.round(c.temperingRequired())), cx, y, 0xE0E0E0);
			y += 16;
		}
		for (FormattedCharSequence line : font.split(c.physique().getDescription(), WIDTH - 28)) {
			g.drawCenteredString(font, line, cx, y, 0xB0B0B0);
			y += 11;
		}
		y += 4;
		for (Detail detail : netherworldDetails()) {
			g.drawCenteredString(font, detail.text(), cx, y, detail.color());
			y += 11;
		}
	}

	/**
	 * The ghostly qi where the player stands (only where there is any: the Overworld, the Nether, the Netherworld and the Great Void's
	 * battlefields), and their yin corruption
	 * while they carry any, with whether it is ebbing or held still.
	 */
	private List<Detail> netherworldDetails() {
		List<Detail> details = new ArrayList<>();
		double density = ClientNetherworldData.density();
		if (density > 0) {
			String word = GhostQi.word(density);
			Component amount = Component.translatable(switch (word) {
				case "teeming" -> ModLang.GHOST_QI_TEEMING;
				case "dense" -> density > GhostQi.NETHERWORLD ? ModLang.GHOST_QI_PEAKS : ModLang.GHOST_QI_DENSE;
				case "thin" -> ModLang.GHOST_QI_THIN;
				default -> ModLang.GHOST_QI_FAINT;
			}, density == Math.rint(density) ? String.valueOf((long) density) : String.valueOf(density));
			details.add(new Detail(Component.translatable(ModLang.GHOST_QI_HERE, amount), 0x8FEAE4));
		}
		float corruption = ClientNetherworldData.corruption();
		if (corruption > 0) {
			long percent = Math.round(corruption * 100);
			Component line = ClientNetherworldData.heldSeconds() > 0
					? Component.translatable(ModLang.YIN_HELD, percent, ClientNetherworldData.heldSeconds())
					: ClientNetherworldData.trend() < 0 ? Component.translatable(ModLang.YIN_EBBING, percent)
					: Component.translatable(ModLang.YIN_LINE, percent);
			details.add(new Detail(line, corruption >= 0.75f ? 0x4FC3D9 : 0x7FB8C8));
		}
		return details;
	}

	// --- Stats: base values, what the realm adds, and the current totals (true vs. suppressed when suppressed) ---

	/** One row of the Stats tab. Values are multiplied by {@code scale} for display. */
	private record StatLine(String nameKey, Attribute attribute, ToDoubleBiFunction<Realm, Stage> bonus, double scale, String unit) {}

	private static final List<StatLine> STAT_LINES = List.of(
			new StatLine(ModLang.STAT_NAME_HEALTH, Attributes.MAX_HEALTH, CultivationStats::maxHealth, 1, ""),
			new StatLine(ModLang.STAT_NAME_DAMAGE, Attributes.ATTACK_DAMAGE, CultivationStats::attackDamage, 1, ""),
			// Speed as a percentage of the default walking speed (0.1 = 100%); its bonus multiplies that base.
			new StatLine(ModLang.STAT_NAME_SPEED, Attributes.MOVEMENT_SPEED, CultivationStats::moveSpeed, 1000, "%"),
			new StatLine(ModLang.STAT_NAME_ARMOR, Attributes.ARMOR, CultivationStats::armor, 1, ""),
			new StatLine(ModLang.STAT_NAME_TOUGHNESS, Attributes.ARMOR_TOUGHNESS, CultivationStats::toughness, 1, ""),
			new StatLine(ModLang.STAT_NAME_KNOCKBACK, Attributes.KNOCKBACK_RESISTANCE, CultivationStats::knockbackResistance, 100, "%"));

	private void renderStats(GuiGraphics g, Player player) {
		PlayerCultivation c = ClientCultivationData.get();
		// The body follows the higher of the two paths (TwoPaths), as wielded now and as it truly stands.
		TwoPaths.Body body = TwoPaths.body(c, true);
		TwoPaths.Body truth = TwoPaths.body(c, false);
		// The lower realm's cap, or another cultivator's Realm Suppress: either way the stats now fall short of the true ones.
		boolean suppressed = body.rank() != truth.rank() || c.isUnderPressure();
		boolean qiSuppressed = c.isSuppressed() || c.isUnderPressure();
		Realm r = body.realm(); // bonuses follow the (possibly suppressed) effective stage
		Stage s = body.stage();
		int cx = left + WIDTH / 2;
		int x = left + 14;
		int y = top + CONTENT_Y + METHOD_PAGE_ROW; // under the page buttons (Stats, Stat Points)
		// The ghost path's strength is named so; and the two paths' harmony, if they are both walked near enough: on the same line
		// if it fits, else on a line of its own under it ("Ghost path: Foundation Building - Grand Perfection, harmony +20%" ran
		// past both edges of the panel). Returns the y of the last line drawn.
		double harmony = TwoPaths.harmony(c);
		int textWidth = WIDTH - 28;
		java.util.function.BiFunction<Component, Integer, Integer> drawNamed = (rank, at) -> {
			Component line = body.ghostPath() ? Component.translatable(ModLang.STATS_GHOST_BODY, rank) : rank;
			if (harmony <= 0) {
				g.drawCenteredString(font, line, cx, at, 0xB39DDB);
				return at;
			}
			Component joined = line.copy().append(Component.translatable(ModLang.STATS_HARMONY, Math.round(harmony * 100)));
			if (font.width(joined) <= textWidth) {
				g.drawCenteredString(font, joined, cx, at, 0xB39DDB);
				return at;
			}
			g.drawCenteredString(font, line, cx, at, 0xB39DDB);
			g.drawCenteredString(font, Component.translatable(ModLang.TWO_PATHS_LINE, Math.round(harmony * 100)), cx, at + 10, 0x8FEAE4);
			return at + 10;
		};

		if (suppressed) {
			// True cultivation, then what the lower realm or the pressure holds it to.
			y = drawNamed.apply(PlayerCultivation.rankName(truth.realm(), truth.stage()), y);
			y += 11;
			g.drawCenteredString(font, Component.translatable(c.isUnderPressure() ? ModLang.STATS_PRESSED : ModLang.STATS_SUPPRESSED),
					cx, y, 0xFF6E6E);
			y += 11;
			Component held = PlayerCultivation.rankName(r, s);
			if (c.isUnderPressure()) held = held.copy().append(" (-" + Math.round(c.getPressurePenalty() * 100) + "%)");
			g.drawCenteredString(font, held, cx, y, 0xFF6E6E);
		} else {
			y = drawNamed.apply(PlayerCultivation.rankName(r, s), y);
		}
		// What the body comes to on one path (TwoPaths#effective), when the harmony or Heaven's Favour carry it past its own rank.
		if (TwoPaths.strongerThanRank(c)) {
			y += 1;
			for (FormattedCharSequence part : effectiveLines(c, WIDTH - 28)) {
				y += 10;
				g.drawCenteredString(font, part, cx, y, 0xFFD54F);
			}
		}
		// The body they were born with (Physique), its roots' elements, shattered meridians' body refining; a Myriad Form Body's forms
		// on a line of their own (wrapped), and the roots with them, or under it, when they don't fit beside it.
		if (c.getPhysique() != null) {
			y += 11;
			Physique physique = c.getPhysique();
			// With its talent (Physique#talent: its grade's, an Eternal Reincarnation Body's a Low one's): what the years meditation costs
			// are divided by (Lifespan#seclude).
			double talent = c.physiqueTalent();
			Component bare = Component.translatable(ModLang.STATS_PHYSIQUE, physique.getDisplayName(), c.physiqueGrade().getDisplayName(),
					talent == Math.floor(talent) ? String.valueOf((long) talent) : String.valueOf(talent));
			Component roots = c.getRootElements().isEmpty() ? null : Component.translatable(ModLang.STATS_ROOTS, Physiques.rootsLine(c.getRootElements()));
			Component line = bare;
			if (roots != null) line = line.copy().append("  ").append(roots);
			if (c.getBodyRefining() > 0) line = line.copy().append("  ").append(Component.translatable(ModLang.STATS_BODY_REFINING, BodyRefining.name(c.getBodyRefining())));
			Component first = fit(WIDTH - 20, line, bare);
			g.drawCenteredString(font, first, cx, y, 0xE0E0E0);
			Component more = first == line || roots == null ? null : roots;
			if (!c.getForms().isEmpty()) {
				Component forms = Component.translatable(ModLang.STATS_FORMS, Physiques.formsLine(c.getForms()));
				more = more == null ? forms : forms.copy().append("  ").append(more);
			}
			if (more != null) {
				for (FormattedCharSequence part : font.split(more, WIDTH - 28)) {
					y += 10;
					g.drawCenteredString(font, part, cx, y, 0xE0E0E0);
				}
			}
		}
		// Their years (Lifespan): their age of their span, alive or dead (one clock); red in the last ten. Beside them the obsession
		// they named (Obsession) and what it gives, as much as there is room for.
		y += 11;
		double used = ClientCultivationData.lifeUsed(), span = ClientCultivationData.lifeSpan();
		boolean late = span - used <= com.example.defyingtheheavens.Lifespan.WARN_YEARS;
		Component years = Double.isInfinite(span) ? Component.translatable(ModLang.STATS_LIFESPAN_ENDLESS, (long) Math.floor(used)) // a True Immortal's
				: Component.translatable(ModLang.STATS_LIFESPAN, (long) Math.floor(used), (long) Math.floor(span));
		com.example.defyingtheheavens.FourAxis.Axis obsession = c.obsession();
		Component lifeLine = years;
		if (obsession != null) {
			Component named = com.example.defyingtheheavens.Obsession.displayName(obsession);
			lifeLine = fit(WIDTH - 20,
					years.copy().append("   ").append(Component.translatable(ModLang.STATS_OBSESSION_BOON, named, com.example.defyingtheheavens.Obsession.boon(obsession))),
					years.copy().append("   ").append(Component.translatable(ModLang.STATS_OBSESSION, named)), years);
		}
		g.drawCenteredString(font, lifeLine, cx, y, late ? 0xFF6E6E : 0xC8B88A);
		// (The sect they belong to, and their title in it, are on the Sect tab.)
		y += 13;

		int currentX = left + WIDTH - 14;
		g.drawString(font, Component.translatable(ModLang.STATS_COL_STAT), x, y, 0xFFD700);
		right(g, Component.translatable(ModLang.STATS_COL_BASE), left + BASE_RIGHT, y, 0xFFD700);
		right(g, Component.translatable(suppressed ? ModLang.STATS_COL_TRUE : ModLang.STATS_COL_BONUS), left + BONUS_RIGHT, y, 0xFFD700);
		right(g, Component.translatable(suppressed ? ModLang.STATS_COL_NOW : ModLang.STATS_COL_CURRENT), currentX, y, 0xFFD700);
		y += 11;
		g.fill(x, y, currentX, y + 1, HallHud.RULE);
		y += 4;

		for (StatLine line : STAT_LINES) {
			statRow(g, player, line, body, truth, suppressed, y);
			y += 11;
		}
		if (c.isMortal()) return; // a mortal's ghost: no qi of the heaven path's
		qiRow(g, ModLang.STAT_NAME_MAX_QI, CultivationStats::maxQi, c.maxQi(), false, c, qiSuppressed, y);
		y += 11;
		qiRow(g, ModLang.STAT_NAME_QI_GATHER, CultivationStats::qiGather, c.qiGatherPerSecond(), true, c, qiSuppressed, y);
		y += 11;
		if (c.isInUpperRealm()) {
			g.drawString(font, Component.translatable(ModLang.STATS_QI_GATHER_UPPER, (int) PlayerCultivation.UPPER_REALM_QI_MULTIPLIER),
					x, y, QI_UPPER_COLOR);
		} else if (c.getPlaceQi() == PlayerCultivation.GREAT_VOID_QI_MULTIPLIER) {
			g.drawString(font, Component.translatable(ModLang.STATS_QI_GATHER_GREAT_VOID, (int) PlayerCultivation.GREAT_VOID_QI_MULTIPLIER), x, y, 0xC8A8FF);
		} else if (c.getPlaceQi() == PlayerCultivation.IMMORTAL_QI_MULTIPLIER) {
			g.drawString(font, Component.translatable(ModLang.STATS_QI_GATHER_IMMORTAL, (int) PlayerCultivation.IMMORTAL_QI_MULTIPLIER), x, y, 0xF5D47A);
		} else if (Math.abs(c.getPlaceQi() - 1) > 1e-6) {
			g.drawString(font, Component.translatable(ModLang.STATS_QI_GATHER_PLACE, String.format(Locale.ROOT, "%.2f", c.getPlaceQi())), x, y,
					c.getPlaceQi() > 1 ? QI_UPPER_COLOR : 0xB0B0C8);
		}
	}

	// --- Stats: the Stat Points page ---

	/** Each stat's colour on the Points page and its chart. */
	private static int statColour(StatPoints.Stat stat) {
		return switch (stat) {
			case STRENGTH -> 0xFF8A65;
			case CONSTITUTION -> 0x81C784;
			case AGILITY -> 0x4FC3F7;
			case ENDURANCE -> 0xFFB74D;
			case SPIRIT -> 0xB39DDB;
			case INSIGHT -> 0xFFE082;
		};
	}

	/**
	 * The Stats tab's three pages, as buttons over it: Stats, Stat Points (gold, with the count, while there are points to spend), and
	 * Crafts.
	 */
	private void renderStatPages(GuiGraphics g, int mouseX, int mouseY) {
		int unspent = StatPoints.unspent(ClientCultivationData.get());
		int x = left + 14;
		int y = top + CONTENT_Y;
		for (int page = 0; page < 3; page++) {
			boolean points = page == 1;
			Component label = Component.translatable(page == 1 ? ModLang.BTN_STAT_POINTS : page == 2 ? ModLang.BTN_STAT_CRAFTS : ModLang.BTN_STAT_LIST);
			if (points && unspent > 0) label = label.copy().append(" (" + unspent + ")");
			int w = font.width(label) + 12;
			RowButton<Integer> button = new RowButton<>(page, x, y, w);
			statPageButtons.add(button);
			boolean chosen = statPage == page;
			int colour = points && unspent > 0 ? 0xFFD700 : chosen ? 0xFFD54F : 0x9E9E9E;
			drawRowButton(g, button, label, button.contains(mouseX, mouseY), colour, chosen);
			x += w + 4;
		}
	}

	/**
	 * The Stat Points page: the six stats' spread drawn as a hexagon on the left (each corner a stat, the gold shape out toward it as
	 * far as its points go against the most in any one), and on the right the points to spend and each stat with its points, what
	 * they give, and a + to put one more in (shift-click: five).
	 */
	private void renderStatPoints(GuiGraphics g, int mouseX, int mouseY) {
		PlayerCultivation c = ClientCultivationData.get();
		StatPoints.Stat[] stats = StatPoints.Stat.values();
		int[] values = new int[stats.length];
		int most = 0;
		for (StatPoints.Stat stat : stats) {
			values[stat.ordinal()] = c.getStatPoints(stat);
			most = Math.max(most, values[stat.ordinal()]);
		}
		int cx = left + 58, cy = top + 136, radius = 42;
		radarChart(g, cx, cy, radius, values, Math.max(5, most), stats);

		int x = left + 116, right = left + WIDTH - 12;
		int y = top + CONTENT_Y + METHOD_PAGE_ROW + 4;
		int unspent = StatPoints.unspent(c), earned = StatPoints.earned(c);
		g.drawString(font, Component.translatable(ModLang.STAT_POINTS_UNSPENT, unspent), x, y, unspent > 0 ? 0xFFD700 : 0xE0E0E0);
		y += 11;
		Component earnedLine = earned <= 0 ? Component.translatable(ModLang.STAT_POINTS_MORTAL)
				: Component.translatable(ModLang.STAT_POINTS_EARNED, earned, StatPoints.STAGE_POINTS, StatPoints.REALM_POINTS);
		for (FormattedCharSequence line : font.split(earnedLine, right - x)) {
			g.drawString(font, line, x, y, 0x9E9E9E);
			y += 10;
		}
		y += 3;
		for (StatPoints.Stat stat : stats) {
			int points = c.getStatPoints(stat);
			g.drawString(font, stat.getDisplayName(), x, y, statColour(stat));
			RowButton<StatPoints.Stat> plus = new RowButton<>(stat, right - 16, y - 3, 16);
			right(g, Component.literal(String.valueOf(points)), plus.x() - 4, y, 0xFFFFFF);
			if (unspent > 0) {
				statPlusButtons.add(plus);
				drawRowButton(g, plus, Component.literal("+"), plus.contains(mouseX, mouseY), 0x69F0AE);
			}
			y += 10;
			g.drawString(font, stat.describe(points), x, y, 0xA8A8A8);
			y += 11;
		}
		g.drawCenteredString(font, Component.translatable(ModLang.STAT_POINTS_HINT), left + WIDTH / 2, top + HEIGHT - 12, 0x7A7A7A);
	}

	/**
	 * The Crafts page (CraftMastery, 2026-10-28): each craft with its rank, a gold bar of the practice toward the next rank, and what the
	 * rank does: in alchemy the chance of a better grade, how often the hardest brews fail, and a second pill's chance.
	 */
	private void renderCrafts(GuiGraphics g) {
		PlayerCultivation c = ClientCultivationData.get();
		int x = left + 14, right = left + WIDTH - 14;
		int y = top + CONTENT_Y + METHOD_PAGE_ROW + 6;
		for (CraftMastery.Craft craft : CraftMastery.Craft.values()) {
			int practice = c.getMastery(craft);
			CraftMastery.Rank rank = CraftMastery.Rank.of(practice);
			CraftMastery.Rank next = rank.next();
			g.drawString(font, Component.translatable(ModLang.CRAFTS_TITLE, craft.getDisplayName(), rank.getDisplayName()), x, y, 0xFFFFFF);
			y += 11;
			// The bar: the practice past this rank's start, against the way to the next (full at Saint).
			double share = next == null ? 1 : (practice - rank.from) / (double) (next.from - rank.from);
			g.fill(x, y, right, y + 5, 0xFF2A1F14);
			g.fill(x, y, x + (int) Math.round((right - x) * Math.max(0, Math.min(1, share))), y + 5, 0xFFD4A63A);
			y += 8;
			Component progress = next == null ? Component.translatable(ModLang.CRAFTS_PRACTICE_TOP, practice)
					: Component.translatable(ModLang.CRAFTS_PRACTICE, practice, next.from, next.getDisplayName());
			g.drawString(font, progress, x, y, 0xA8A8A8);
			y += 11;
			List<Component> lines = new ArrayList<>();
			switch (craft) {
				case ALCHEMY -> {
					lines.add(Component.translatable(ModLang.CRAFTS_ALCHEMY_GRADE, Math.round(CraftMastery.betterGrade(rank) * 100)));
					double fails = CraftMastery.failChance(rank, 3);
					lines.add(fails > 0 ? Component.translatable(ModLang.CRAFTS_ALCHEMY_FAIL, Math.round(fails * 100))
							: Component.translatable(ModLang.CRAFTS_ALCHEMY_NEVER_FAILS));
					if (CraftMastery.secondPill(rank) > 0) {
						lines.add(Component.translatable(ModLang.CRAFTS_ALCHEMY_SECOND, Math.round(CraftMastery.secondPill(rank) * 100)));
					}
				}
				case FORGING -> lines.add(Component.translatable(ModLang.CRAFTS_FORGING));
				case FORMATIONS -> lines.add(Component.translatable(ModLang.CRAFTS_FORMATIONS));
			}
			for (Component line : lines) {
				for (FormattedCharSequence part : font.split(line, right - x)) {
					g.drawString(font, part, x + 4, y, 0x8FA8C8);
					y += 10;
				}
			}
			y += 6;
		}
		g.drawCenteredString(font, Component.translatable(ModLang.CRAFTS_HINT), left + WIDTH / 2, top + HEIGHT - 12, 0x7A7A7A);
	}

	/**
	 * A radar chart: a hexagon (or as many corners as there are stats) {@code radius} round ({@code cx}, {@code cy}), rings at a
	 * third and two thirds, a spoke to each corner and the stat's short name past it; over it the shape of {@code values}, each as
	 * far toward its corner as it goes against {@code scale}, filled in gold, its edge bright and a dot at each point.
	 */
	private void radarChart(GuiGraphics g, int cx, int cy, int radius, int[] values, int scale, StatPoints.Stat[] stats) {
		int n = values.length;
		float[][] corner = new float[n][2], point = new float[n][2];
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			corner[i][0] = (float) Math.sin(a);
			corner[i][1] = (float) -Math.cos(a);
			float t = Math.max(0.05f, Math.min(1, values[i] / (float) scale));
			point[i][0] = cx + corner[i][0] * radius * t;
			point[i][1] = cy + corner[i][1] * radius * t;
		}
		org.joml.Matrix4f m = g.pose().last().pose();
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
		com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
		com.mojang.blaze3d.vertex.BufferBuilder b = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
		b.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			// The field, dark; its rings and spokes, faint gold.
			tri(b, m, cx, cy, cx + corner[i][0] * radius, cy + corner[i][1] * radius, cx + corner[j][0] * radius, cy + corner[j][1] * radius, 0xC0141420);
			for (int ring = 1; ring <= 3; ring++) {
				float r = radius * ring / 3f;
				line(b, m, cx + corner[i][0] * r, cy + corner[i][1] * r, cx + corner[j][0] * r, cy + corner[j][1] * r, ring == 3 ? 1.2f : 0.7f,
						ring == 3 ? 0xFFB8860B : 0x806E5214);
			}
			line(b, m, cx, cy, cx + corner[i][0] * radius, cy + corner[i][1] * radius, 0.7f, 0x806E5214);
		}
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			tri(b, m, cx, cy, point[i][0], point[i][1], point[j][0], point[j][1], 0x88E0B040);
		}
		for (int i = 0; i < n; i++) {
			int j = (i + 1) % n;
			line(b, m, point[i][0], point[i][1], point[j][0], point[j][1], 1.4f, 0xFFFFD54F);
			float d = 2.2f;
			int colour = 0xFF000000 | statColour(stats[i]);
			tri(b, m, point[i][0] - d, point[i][1], point[i][0], point[i][1] - d, point[i][0] + d, point[i][1], colour);
			tri(b, m, point[i][0] - d, point[i][1], point[i][0] + d, point[i][1], point[i][0], point[i][1] + d, colour);
		}
		com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(b.end());
		com.mojang.blaze3d.systems.RenderSystem.disableBlend();
		for (int i = 0; i < n; i++) {
			String name = stats[i].getDisplayName().getString();
			String shortName = name.length() > 3 ? name.substring(0, 3).toUpperCase(Locale.ROOT) : name;
			int lx = Math.round(cx + corner[i][0] * (radius + 10)), ly = Math.round(cy + corner[i][1] * (radius + 9)) - 4;
			g.drawCenteredString(font, shortName, lx, ly, statColour(stats[i]));
		}
	}

	private static void tri(com.mojang.blaze3d.vertex.BufferBuilder b, org.joml.Matrix4f m, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
		int a = argb >>> 24, r = argb >> 16 & 0xFF, gr = argb >> 8 & 0xFF, bl = argb & 0xFF;
		b.vertex(m, x1, y1, 0).color(r, gr, bl, a).endVertex();
		b.vertex(m, x2, y2, 0).color(r, gr, bl, a).endVertex();
		b.vertex(m, x3, y3, 0).color(r, gr, bl, a).endVertex();
	}

	/** A line {@code width} across, as two triangles. */
	private static void line(com.mojang.blaze3d.vertex.BufferBuilder b, org.joml.Matrix4f m, float x1, float y1, float x2, float y2, float width, int argb) {
		float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 1.0e-3f) return;
		float nx = -dy / len * width / 2, ny = dx / len * width / 2;
		tri(b, m, x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, argb);
		tri(b, m, x1 + nx, y1 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, argb);
	}

	// --- Sect: the player's sect, or how to join one ---

	/**
	 * A disciple's sect: its name and path, their title and its regard, where its grounds lie, its barrier, the treasury's rule for
	 * them, its members (counted, then listed by seniority, players after, scrolling); for one of no sect, how to join one and the
	 * nearest sect.
	 */
	private void renderSect(GuiGraphics g, int mouseX, int mouseY) {
		ClientSect.Info info = ClientSect.info();
		int cx = left + WIDTH / 2;
		int x = left + 14;
		int y = top + CONTENT_Y + 4;
		if (info == null) {
			sectMaxScroll = 0;
			g.drawCenteredString(font, Component.translatable(ModLang.SECT_TAB_LOADING), cx, y + 40, 0x9E9E9E);
			return;
		}
		if (!info.member()) {
			sectMaxScroll = 0;
			g.drawCenteredString(font, Component.translatable(ModLang.SECT_TAB_NONE), cx, y + 6, 0xFFD700);
			y += 24;
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.SECT_TAB_HOW), WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, 0xB0B0B0);
				y += 11;
			}
			y += 8;
			Component nearest = info.sectName().isEmpty() ? Component.translatable(ModLang.SECT_TAB_NEAREST_NONE)
					: Component.translatable(ModLang.SECT_TAB_NEAREST, info.sectName(), Alignment.describe(info.alignment()), info.nearestDistance(),
					Component.translatable(ModLang.talkDirection(info.direction())));
			for (FormattedCharSequence line : font.split(nearest, WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, 0xE0E0E0);
				y += 11;
			}
			if (info.barredDays() > 0) {
				g.drawCenteredString(font, Component.translatable(ModLang.SECT_TAB_BARRED, info.barredDays()), cx, y + 2, 0xFF6E6E);
				y += 11;
			}
			if (info.court() != null) g.drawCenteredString(font, courtLine(info), cx, y + 10, NpcTitle.IMMORTAL_OFFICER.getColor());
			return;
		}
		g.drawCenteredString(font, fit(WIDTH - 20, Component.literal(info.sectName()), Component.literal(info.sectName())), cx, y, 0xFFD700);
		y += 11;
		g.drawCenteredString(font, Alignment.describe(info.alignment()), cx, y, 0xB0B0B0);
		y += 15;
		NpcTitle title = info.title();
		g.drawString(font, Component.translatable(ModLang.SECT_TAB_TITLE, title.getDisplayName(info.clan(), info.holy(), info.immortal())), x, y, title.getColor());
		y += 11;
		g.drawString(font, Component.translatable(ModLang.SECT_TAB_STANDING, Component.translatable(ModLang.talkRegardLevel(Regard.levelId(info.standing()))),
				(info.standing() >= 0 ? "+" : "") + info.standing()), x, y, 0xE0E0E0);
		y += 11;
		Component grounds = info.onGrounds() ? Component.translatable(ModLang.SECT_TAB_GROUNDS_HERE, Math.round((SectMembership.GROUNDS_BONUS - 1) * 100))
				: info.distance() < 0 ? Component.translatable(ModLang.SECT_TAB_GROUNDS_ELSEWHERE)
				: Component.translatable(ModLang.SECT_TAB_GROUNDS_AWAY, info.distance(), Component.translatable(ModLang.talkDirection(info.direction())));
		g.drawString(font, fit(WIDTH - 28, grounds, grounds), x, y, info.onGrounds() ? 0x69F0AE : 0xE0E0E0);
		y += 11;
		g.drawString(font, Component.translatable(info.barrierUp() ? ModLang.SECT_TAB_BARRIER_UP : ModLang.SECT_TAB_BARRIER_DOWN), x, y,
				info.barrierUp() ? 0x8FEAE4 : 0xFF6E6E);
		y += 11;
		String treasury = info.elder() ? ModLang.SECT_TAB_TREASURY_OPEN
				: info.treasuryTries() >= SectMembership.TREASURY_WARNINGS ? ModLang.SECT_TAB_TREASURY_LAST : ModLang.SECT_TAB_TREASURY_CLOSED;
		Component treasuryLine = Component.translatable(treasury);
		g.drawString(font, fit(WIDTH - 28, treasuryLine, treasuryLine), x, y, info.elder() ? 0xFFD700 : info.treasuryTries() > 0 ? 0xFF6E6E : 0xE0E0E0);
		y += 11;
		long cultivators = info.members().stream().filter(member -> !member.player()).count();
		g.drawString(font, Component.translatable(ModLang.SECT_TAB_MEMBERS, cultivators, info.members().size() - cultivators), x, y, 0xE0E0E0);
		y += 11;
		if (info.inBody()) {
			// In one of its people's bodies (Possession): the body may leave it from here (the first click asks again).
			g.drawString(font, Component.translatable(ModLang.SECT_TAB_BODY), x, y + 3, 0x9E9E9E);
			Component label = Component.translatable(confirming(LEAVE_SECT) ? ModLang.SECT_TAB_SURE : ModLang.SECT_TAB_LEAVE_BUTTON);
			int w = font.width(label) + 8;
			RowButton<UUID> leave = new RowButton<>(LEAVE_SECT, left + LIST_RIGHT - w, y, w);
			sectButtons.add(leave);
			drawRowButton(g, leave, label, leave.contains(mouseX, mouseY), 0xFF6E6E);
			y += SWITCH_H + 2;
		} else {
			g.drawString(font, Component.translatable(ModLang.SECT_TAB_DAYS, Math.max(1, info.ticksMember() / HeavenRitual.DAY + 1)), x, y, 0x9E9E9E);
			y += 11;
			// Their Contribution there (its mission board: SectMissions), all they have earned of it, and the missions they hold.
			Component contribution = Component.translatable(ModLang.SECT_TAB_CONTRIBUTION, info.contribution(), info.earned(), info.missions(), SectMissions.HELD);
			g.drawString(font, fit(WIDTH - 28, contribution, contribution), x, y, 0xF2C94C);
			y += 13;
		}
		if (info.court() != null) {
			Component court = courtLine(info);
			g.drawString(font, fit(WIDTH - 28, court, court), x, y - 2, NpcTitle.IMMORTAL_OFFICER.getColor());
			y += 11;
		}
		g.fill(x, y, left + LIST_RIGHT, y + 1, HallHud.RULE);
		y += 3;

		// The members, by seniority (the strongest first), players after them; in a senior's body, a Banish button by those below it.
		int viewTop = y, viewBottom = top + HEIGHT - 8;
		boolean banishing = info.members().stream().anyMatch(ClientSect.Member::banishable);
		int rowHeight = banishing ? SWITCH_H + 1 : 10;
		int textOffset = banishing ? (SWITCH_H - 8) / 2 : 0;
		int contentHeight = info.members().size() * rowHeight + 4;
		sectMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		sectScroll = Mth.clamp(sectScroll, 0, sectMaxScroll);
		g.enableScissor(left + 1, viewTop, left + WIDTH - 1, viewBottom);
		int ry = viewTop + 2 - sectScroll;
		for (ClientSect.Member member : info.members()) {
			if (ry > viewTop - rowHeight && ry < viewBottom) {
				MutableComponent row = member.title() == null ? Component.literal("")
						: Component.translatable(ModLang.NPC_TITLE_BRACKETS, member.title().getDisplayName(info.clan(), info.holy(), info.immortal())).append(" ");
				row.append(member.player() ? Component.translatable(ModLang.SECT_TAB_PLAYER, member.name()) : Component.literal(member.name()));
				int colour = member.title() == null ? 0x8A8A8A : member.title().getColor();
				Component realm = member.rank() < 0 ? Component.translatable(ModLang.SECT_TAB_OFFLINE)
						: PlayerCultivation.rankName(CultivatorNpc.realmOf(member.rank()), CultivatorNpc.stageOf(member.rank()));
				right(g, realm, left + LIST_RIGHT, ry + textOffset, 0x9E9E9E);
				// The name gets what the realm (and a Banish button) leaves, cut short with an ellipsis if it must be.
				int room = left + LIST_RIGHT - font.width(realm) - 6 - x;
				if (member.banishable()) {
					Component label = Component.translatable(confirming(member.id()) ? ModLang.SECT_TAB_SURE : ModLang.SECT_TAB_BANISH_BUTTON);
					int w = font.width(label) + 6;
					RowButton<UUID> banish = new RowButton<>(member.id(), left + LIST_RIGHT - font.width(realm) - 6 - w, ry, w);
					// Pressable only while the whole of it shows (not half hidden at the list's edge).
					boolean shown = ry >= viewTop && ry + SWITCH_H <= viewBottom;
					if (shown) sectButtons.add(banish);
					drawRowButton(g, banish, label, shown && banish.contains(mouseX, mouseY), 0xFF6E6E);
					room -= w + 4;
				}
				String name = row.getString();
				if (font.width(name) > room) name = font.plainSubstrByWidth(name, Math.max(0, room - font.width("..."))) + "...";
				g.drawString(font, name, x, ry + textOffset, member.player() ? 0x8FEAE4 : colour);
			}
			ry += rowHeight;
		}
		g.disableScissor();
		scrollbar(g, viewTop, viewBottom, contentHeight, sectScroll, sectMaxScroll, mouseX, mouseY);
	}

	/** The Sect tab's line of the Celestial Court: their rank in it and their merit, and the merit the next rank wants. */
	private static Component courtLine(ClientSect.Info info) {
		MutableComponent line = Component.translatable(ModLang.SECT_TAB_COURT, info.court().displayName(), info.merit());
		CelestialCourt.Rank next = info.court().next();
		if (next != null) line.append(Component.translatable(ModLang.SECT_TAB_COURT_NEXT, next.displayName(), next.merit()));
		return line;
	}

	/** Whether the Sect tab's button for {@code id} has been clicked once and is asking "Sure?" still. */
	private boolean confirming(UUID id) {
		return id.equals(sectConfirm) && System.currentTimeMillis() - sectConfirmAt < CONFIRM_MS;
	}

	// --- Abilities: what the realm has awakened, each switchable on and off ---

	/**
	 * One entry per awakened ability, top to bottom: its name with an On/Off switch, then what it does (Qi Flight always
	 * shows its cost, even where gathering covers it: players find out flight is endless there by flying). Scrolls with the
	 * mouse wheel or the {@link #scrollbar} at the right edge once the entries outgrow the panel.
	 */
	private void renderAbilities(GuiGraphics g, int mouseX, int mouseY) {
		PlayerCultivation c = ClientCultivationData.get();
		List<Ability> awakened = awakened();
		if (awakened.isEmpty()) {
			abilityMaxScroll = 0;
			renderPlaceholder(g, ModLang.ABILITIES_TITLE, ModLang.ABILITIES_EMPTY, ModLang.ABILITIES_HINT);
			return;
		}
		if (arranging) {
			renderWheelEditor(g, c, awakened, mouseX, mouseY);
			return;
		}

		int x = left + 14;
		int rightEdge = left + LIST_RIGHT;
		int viewTop = top + CONTENT_Y;
		int viewBottom = top + ABILITIES_BOTTOM;
		List<List<FormattedCharSequence>> descriptions = new ArrayList<>();
		int contentHeight = 6;
		for (int i = 0; i < awakened.size(); i++) {
			List<FormattedCharSequence> lines = font.split(awakened.get(i).getDescription(c), rightEdge - x);
			descriptions.add(lines);
			contentHeight += 15 + lines.size() * 10 + (i < awakened.size() - 1 ? 12 : 0);
		}
		abilityMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		abilityScroll = Mth.clamp(abilityScroll, 0, abilityMaxScroll);

		g.enableScissor(left + 1, viewTop, left + WIDTH - 1, viewBottom);
		int y = viewTop + 3 - abilityScroll;
		for (int i = 0; i < awakened.size(); i++) {
			Ability ability = awakened.get(i);
			boolean on = c.isAbilityEnabled(ability);
			AbilitySwitch toggle = new AbilitySwitch(ability, rightEdge - SWITCH_W, y - 3);
			// Only a switch wholly in view can be clicked.
			boolean visible = toggle.y() >= viewTop && toggle.y() + SWITCH_H <= viewBottom;
			if (visible) switches.add(toggle);
			AbilityWheel.drawIcon(g, ability, x, y - 4, 16, on);
			g.drawString(font, ability.getDisplayName(), x + 20, y, on ? 0xFFD700 : 0x9E9E9E);
			drawSwitch(g, toggle, on, visible && toggle.contains(mouseX, mouseY));
			y += 15;
			for (FormattedCharSequence line : descriptions.get(i)) {
				g.drawString(font, line, x, y, on ? 0xB0B0B0 : 0x707070);
				y += 10;
			}
			if (i < awakened.size() - 1) {
				y += 4;
				g.fill(x, y, rightEdge, y + 1, HallHud.RULE);
				y += 8;
			}
		}
		g.disableScissor();

		scrollbar(g, viewTop, viewBottom, contentHeight, abilityScroll, abilityMaxScroll, mouseX, mouseY);
	}

	/**
	 * The scrollbar at the panel's right edge, there whenever the tab lists anything: a lacquer trough between two gold
	 * roller caps, and a gold thumb, gripped in the middle, for where the view is. The thumb can be dragged, and a click on
	 * the trough brings it there. When everything fits, the thumb fills the trough, dimmed.
	 */
	private void scrollbar(GuiGraphics g, int viewTop, int viewBottom, int contentHeight, int scroll, int maxScroll,
						   int mouseX, int mouseY) {
		scrollbar(g, left + SCROLLBAR_X, viewTop, viewBottom, contentHeight, scroll, maxScroll, mouseX, mouseY);
	}

	/** The scrollbar with its trough's left at {@code x} (the Hall's Cultivation tab's, at its own panel's edge). */
	private void scrollbar(GuiGraphics g, int x, int viewTop, int viewBottom, int contentHeight, int scroll, int maxScroll,
						   int mouseX, int mouseY) {
		int laneTop = viewTop + 2;
		int laneBottom = viewBottom - 2;
		int lane = laneBottom - laneTop;
		boolean scrolls = maxScroll > 0;
		int thumbHeight = scrolls ? Mth.clamp(lane * (viewBottom - viewTop) / contentHeight, 14, lane) : lane;
		int thumbY = scrolls ? laneTop + (lane - thumbHeight) * scroll / maxScroll : laneTop;
		bar = new ScrollBar(x, laneTop, laneBottom, thumbY, thumbHeight, maxScroll);

		// The trough, rimmed in dark gold, and a roller cap at each end a pixel wider than it.
		g.fill(x, laneTop, x + SCROLLBAR_W, laneBottom, 0xFF6E5214);
		g.fill(x + 1, laneTop, x + SCROLLBAR_W - 1, laneBottom, 0xFF0C0C14);
		for (int capY : new int[] {viewTop, laneBottom}) {
			g.fill(x - 1, capY, x + SCROLLBAR_W + 1, capY + 1, BORDER);
			g.fill(x - 1, capY + 1, x + SCROLLBAR_W + 1, capY + 2, 0xFF6E5214);
		}

		// The thumb: lit on its left and top, shaded on its right and bottom, brighter while pointed at or held.
		boolean hot = scrolls && (barGrab >= 0 || bar.contains(mouseX, mouseY) && bar.onThumb(mouseY));
		int tx = x + 1;
		int tw = SCROLLBAR_W - 2;
		int face = !scrolls ? 0xFF4A3A12 : hot ? 0xFFDAA520 : BORDER;
		int lit = !scrolls ? 0xFF5A4818 : 0xFFF0C850;
		int shade = !scrolls ? 0xFF3A2E0E : 0xFF6E5214;
		g.fill(tx, thumbY, tx + tw, thumbY + thumbHeight, face);
		g.fill(tx, thumbY, tx + tw, thumbY + 1, lit);
		g.fill(tx, thumbY + 1, tx + 1, thumbY + thumbHeight, lit);
		g.fill(tx + tw - 1, thumbY + 1, tx + tw, thumbY + thumbHeight, shade);
		g.fill(tx + 1, thumbY + thumbHeight - 1, tx + tw, thumbY + thumbHeight, shade);
		if (scrolls) {
			int mid = thumbY + thumbHeight / 2;
			for (int gy = mid - 2; gy <= mid + 2; gy += 2) g.fill(tx + 1, gy, tx + tw - 1, gy + 1, shade);
		}
	}

	/** Scrolls whichever tab has the scrollbar. */
	private void setScroll(int scroll) {
		switch (tab) {
			case CULTIVATION -> cultScroll = scroll;
			case JOURNAL -> journalScroll = scroll;
			case METHODS -> methodScroll = scroll;
			case SECT -> sectScroll = scroll;
			case SPELLS -> {
				if (arrangingSpells) spellPaletteScroll = scroll;
				else spellScroll = scroll;
			}
			default -> abilityScroll = scroll;
		}
	}

	// --- Journal: the rumours cultivators have told you ---

	/** Each rumour heard (newest first): the day and who told it, in gold, then their words; scrolls like the Abilities tab. */
	private void renderJournal(GuiGraphics g, int mouseX, int mouseY) {
		List<Journal.Entry> entries = ClientJournal.entries();
		int cx = left + WIDTH / 2;
		if (entries.isEmpty()) {
			journalMaxScroll = 0;
			int y = top + CONTENT_Y + 30;
			g.drawCenteredString(font, Component.translatable(ModLang.TAB_JOURNAL), cx, y, 0xFFD700);
			y += 20;
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.JOURNAL_EMPTY), WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, 0xB0B0B0);
				y += 11;
			}
			return;
		}
		int x = left + 14;
		int rightEdge = left + LIST_RIGHT;
		int viewTop = top + CONTENT_Y;
		int viewBottom = top + HEIGHT - 8;
		List<List<FormattedCharSequence>> texts = new ArrayList<>();
		int contentHeight = 6;
		for (int i = 0; i < entries.size(); i++) {
			List<FormattedCharSequence> lines = font.split(entries.get(i).text(), rightEdge - x);
			texts.add(lines);
			contentHeight += 12 + lines.size() * 10 + (i < entries.size() - 1 ? 10 : 0);
		}
		journalMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		journalScroll = Mth.clamp(journalScroll, 0, journalMaxScroll);
		g.enableScissor(left + 1, viewTop, left + WIDTH - 1, viewBottom);
		int y = viewTop + 3 - journalScroll;
		for (int i = 0; i < entries.size(); i++) {
			Journal.Entry entry = entries.get(i);
			// Who told it; "#" marks a source of the player's own (a method learned, the Terrace's view), a translation key.
			Object teller = entry.teller().startsWith("#") ? Component.translatable(entry.teller().substring(1)) : entry.teller();
			g.drawString(font, Component.translatable(ModLang.JOURNAL_ENTRY, entry.day(), teller), x, y, 0xFFD700);
			y += 12;
			for (FormattedCharSequence line : texts.get(i)) {
				g.drawString(font, line, x, y, 0xC8C8C8);
				y += 10;
			}
			if (i < entries.size() - 1) {
				y += 3;
				g.fill(x, y, rightEdge, y + 1, HallHud.RULE);
				y += 7;
			}
		}
		g.disableScissor();
		scrollbar(g, viewTop, viewBottom, contentHeight, journalScroll, journalMaxScroll, mouseX, mouseY);
	}

	/** The abilities the cultivator has awakened, in menu order. */
	private static List<Ability> awakened() {
		PlayerCultivation c = ClientCultivationData.get();
		return Arrays.stream(Ability.values()).filter(ability -> ability.isUnlocked(c)).toList();
	}

	/**
	 * The wheel editor: the wheel on the left (the slots of {@link AbilityWheelScreen}, smaller), the awakened abilities on
	 * the right. Dragging an ability onto a slot puts it there; one already on the wheel trades places with the slot's
	 * occupant. A slot dragged off the wheel, or right-clicked, empties. The wheel's centre names what the pointer is on.
	 */
	private void renderWheelEditor(GuiGraphics g, PlayerCultivation c, List<Ability> awakened, int mouseX, int mouseY) {
		int cx = wheelX();
		int cy = wheelY();
		int hoveredSlot = AbilityWheel.slotUnder(mouseX, mouseY, cx, cy, EDIT_RING, EDIT_SLOT);
		Ability pointed = hoveredSlot < 0 ? null : AbilityWheel.shown(c, hoveredSlot);

		AbilityWheel.drawBackdrop(g, cx, cy, EDIT_BACKDROP);
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			Ability ability = AbilityWheel.shown(c, i);
			// The slot an ability was lifted from looks empty while it's carried.
			if (dragged != null && i == dragFrom) ability = null;
			AbilityWheel.drawSlot(g, AbilityWheel.slotX(cx, i, EDIT_RING), AbilityWheel.slotY(cy, i, EDIT_RING), EDIT_SLOT, ability,
					ability != null && c.isAbilityEnabled(ability), i == hoveredSlot, 16);
		}

		// The palette: every awakened ability, gold-framed if it's already on the wheel.
		int px = left + PALETTE_X;
		int y = top + CONTENT_Y;
		g.drawString(font, Component.translatable(ModLang.ABILITIES_TITLE), px, y, 0xFFD700);
		y += 12;
		int perRow = (WIDTH - 12 - PALETTE_X + CELL_GAP) / (CELL + CELL_GAP);
		for (int i = 0; i < awakened.size(); i++) {
			Ability ability = awakened.get(i);
			PaletteCell cell = new PaletteCell(ability, px + (i % perRow) * (CELL + CELL_GAP), y + (i / perRow) * (CELL + CELL_GAP));
			palette.add(cell);
			boolean hovered = dragged == null && cell.contains(mouseX, mouseY);
			if (hovered) pointed = ability;
			boolean placed = false;
			for (int s = 0; s < PlayerCultivation.WHEEL_SLOTS; s++) placed |= c.getWheelSlot(s) == ability;
			g.fill(cell.x(), cell.y(), cell.x() + CELL, cell.y() + CELL, placed ? AbilityWheel.GOLD : AbilityWheel.SLOT_OFF_RIM);
			g.fill(cell.x() + 1, cell.y() + 1, cell.x() + CELL - 1, cell.y() + CELL - 1, hovered ? AbilityWheel.SLOT_HOVER : AbilityWheel.SLOT);
			AbilityWheel.drawIcon(g, ability, cell.x() + (CELL - 16) / 2, cell.y() + (CELL - 16) / 2, 16, c.isAbilityEnabled(ability));
		}
		y += ((awakened.size() + perRow - 1) / perRow) * (CELL + CELL_GAP) + 4;
		int textWidth = WIDTH - 12 - PALETTE_X;
		for (FormattedCharSequence line : font.split(Component.translatable(ModLang.WHEEL_EDITOR_HINT), textWidth)) {
			g.drawString(font, line, px, y, 0x8A8A8A);
			y += 10;
		}
		y += 4;
		for (FormattedCharSequence line : font.split(Component.translatable(ModLang.WHEEL_EDITOR_KEY,
				ModKeybinds.ABILITY_WHEEL.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(0xFFD700))), textWidth)) {
			g.drawString(font, line, px, y, 0x8A8A8A);
			y += 10;
		}

		// The centre: what's carried, or what the pointer is on, or the wheel's name.
		int centreWidth = 2 * (EDIT_RING - EDIT_SLOT) - 8;
		Ability named = dragged != null ? dragged : pointed;
		List<FormattedCharSequence> lines = named == null
				? font.split(Component.translatable(ModLang.WHEEL_TITLE), centreWidth)
				: font.split(named.getDisplayName(), centreWidth);
		List<FormattedCharSequence> hint = dragged != null && dragFrom < 0
				? font.split(Component.translatable(ModLang.WHEEL_EDITOR_PICK), centreWidth) : List.of();
		int ty = cy - (lines.size() + hint.size()) * 10 / 2;
		for (FormattedCharSequence line : lines) {
			g.drawCenteredString(font, line, cx, ty, named == null || c.isAbilityEnabled(named) ? 0xFFD700 : 0x9E9E9E);
			ty += 10;
		}
		for (FormattedCharSequence line : hint) {
			g.drawCenteredString(font, line, cx, ty, 0x8A8A8A);
			ty += 10;
		}
	}

	private int wheelX() {
		return left + 14 + EDIT_BACKDROP;
	}

	private int wheelY() {
		return top + CONTENT_Y + (ABILITIES_BOTTOM - CONTENT_Y) / 2;
	}

	/** Puts an ability in a wheel slot (null empties it): at once on the client's copy, and on the server, which syncs back. */
	private void placeOnWheel(int slot, Ability ability) {
		ClientCultivationData.get().setWheelSlot(slot, ability);
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(slot);
		buf.writeVarInt(ability == null ? -1 : ability.ordinal());
		ClientPlayNetworking.send(ModPackets.SET_WHEEL_SLOT, buf);
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (tab == Tab.CULTIVATION && cultMaxScroll > 0) {
			// Its scroll is in the artifact's pixels, three to one of the frame's.
			cultScroll = Mth.clamp(cultScroll - (int) Math.round(delta * SCROLL_STEP * 3), 0, cultMaxScroll);
			return true;
		}
		if (tab == Tab.ABILITIES && !arranging && abilityMaxScroll > 0) {
			abilityScroll = Mth.clamp(abilityScroll - (int) Math.round(delta * SCROLL_STEP), 0, abilityMaxScroll);
			return true;
		}
		if (tab == Tab.JOURNAL && journalMaxScroll > 0) {
			journalScroll = Mth.clamp(journalScroll - (int) Math.round(delta * SCROLL_STEP), 0, journalMaxScroll);
			return true;
		}
		if (tab == Tab.METHODS && methodMaxScroll > 0) {
			methodScroll = Mth.clamp(methodScroll - (int) Math.round(delta * SCROLL_STEP), 0, methodMaxScroll);
			return true;
		}
		if (tab == Tab.SECT && sectMaxScroll > 0) {
			sectScroll = Mth.clamp(sectScroll - (int) Math.round(delta * SCROLL_STEP), 0, sectMaxScroll);
			return true;
		}
		if (tab == Tab.SPELLS && !arrangingSpells && spellMaxScroll > 0) {
			spellScroll = Mth.clamp(spellScroll - (int) Math.round(delta * SCROLL_STEP), 0, spellMaxScroll);
			return true;
		}
		if (tab == Tab.SPELLS && arrangingSpells && spellPaletteMaxScroll > 0) {
			spellPaletteScroll = Mth.clamp(spellPaletteScroll - (int) Math.round(delta * SCROLL_STEP), 0, spellPaletteMaxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	/** A held scrollbar thumb follows the pointer. */
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && barGrab >= 0 && bar != null) {
			setScroll(bar.scrollFor(mouseY - barGrab));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	/** An On/Off switch in the Hall's look: framed in jade and green "On", or dark gold and grey "Off" (as the HUD's ability slot). */
	private void drawSwitch(GuiGraphics g, AbilitySwitch toggle, boolean on, boolean hovered) {
		int x = toggle.x();
		int y = toggle.y();
		HallHud.panel(g, x, y, x + SWITCH_W, y + SWITCH_H, hovered ? HallHud.FRAME_LIT : on ? HallHud.FRAME_ON : HALL_FRAME,
				hovered ? TAB_HOVER : TAB_IDLE);
		g.drawCenteredString(font, Component.translatable(on ? ModLang.ABILITY_ON : ModLang.ABILITY_OFF),
				x + SWITCH_W / 2, y + (SWITCH_H - 8) / 2, on ? 0x69F0AE : 0x9E9E9E);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (tab == Tab.ABILITIES && arranging && (button == 0 || button == 1)) {
			PlayerCultivation c = ClientCultivationData.get();
			int slot = AbilityWheel.slotUnder(mouseX, mouseY, wheelX(), wheelY(), EDIT_RING, EDIT_SLOT);
			Ability inSlot = slot < 0 ? null : AbilityWheel.shown(c, slot);
			if (inSlot != null) {
				if (button == 1) {
					placeOnWheel(slot, null);
				} else {
					dragged = inSlot;
					dragFrom = slot;
				}
				return true;
			}
			for (PaletteCell cell : palette) {
				if (button == 0 && cell.contains(mouseX, mouseY)) {
					dragged = cell.ability();
					dragFrom = -1;
					return true;
				}
			}
		}
		if (tab == Tab.SPELLS && arrangingSpells && (button == 0 || button == 1)) {
			int slot = AbilityWheel.slotUnder(mouseX, mouseY, wheelX(), wheelY(), EDIT_RING, EDIT_SLOT);
			Spell inSlot = slot < 0 ? null : ClientMethods.shown(slot);
			if (inSlot != null) {
				if (button == 1) {
					placeSpell(slot, null);
				} else {
					draggedSpell = inSlot;
					spellDragFrom = slot;
				}
				return true;
			}
			for (SpellCell cell : spellPalette) {
				if (button == 0 && cell.contains(mouseX, mouseY)) {
					draggedSpell = cell.spell();
					spellDragFrom = -1;
					return true;
				}
			}
		}
		if (button == 0) {
			for (RowButton<UUID> act : sectButtons) {
				if (!act.contains(mouseX, mouseY)) continue;
				// The first click asks "Sure?"; the second, while it asks, does it.
				if (confirming(act.value())) {
					sectConfirm = null;
					if (act.value().equals(LEAVE_SECT)) ClientSect.leaveAsBody();
					else ClientSect.banish(act.value());
				} else {
					sectConfirm = act.value();
					sectConfirmAt = System.currentTimeMillis();
				}
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<Integer> page : statPageButtons) {
				if (!page.contains(mouseX, mouseY)) continue;
				statPage = page.value();
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<StatPoints.Stat> plus : statPlusButtons) {
				if (!plus.contains(mouseX, mouseY)) continue;
				// One point; shift: five (as many as are left, the server keeps to it).
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeVarInt(plus.value().ordinal());
				buf.writeVarInt(Screen.hasShiftDown() ? 5 : 1);
				ClientPlayNetworking.send(ModPackets.SPEND_STAT_POINTS, buf);
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<Integer> page : methodPageButtons) {
				if (!page.contains(mouseX, mouseY)) continue;
				if (methodPage != page.value()) {
					methodPage = page.value();
					methodScroll = 0;
				}
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			// The Laws page: a Law put in (or, clicked again, out of) meditation's focus; a level owed for a Decay weathered spent on one.
			for (RowButton<com.example.defyingtheheavens.Law> focus : lawFocusButtons) {
				if (!focus.contains(mouseX, mouseY)) continue;
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeVarInt(focus.value() == ClientCultivationData.get().laws().focus() ? -1 : focus.value().ordinal());
				ClientPlayNetworking.send(ModPackets.LAW_FOCUS, buf);
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<com.example.defyingtheheavens.Law> choose : lawChooseButtons) {
				if (!choose.contains(mouseX, mouseY)) continue;
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeVarInt(choose.value().ordinal());
				ClientPlayNetworking.send(ModPackets.LAW_CHOOSE, buf);
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<GhostMethod> practise : practiseButtons) {
				if (!practise.contains(mouseX, mouseY)) continue;
				ClientMethods.practise(practise.value());
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<HeavenMethod> practise : heavenPractiseButtons) {
				if (!practise.contains(mouseX, mouseY)) continue;
				ClientMethods.practise(practise.value());
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<Spell> mode : lifeModeButtons) {
				if (!mode.contains(mouseX, mouseY)) continue;
				ClientMethods.nextLifeMode();
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
			for (RowButton<Spell> ready : readyButtons) {
				if (!ready.contains(mouseX, mouseY)) continue;
				// Ready it; the readied one's button clears it.
				ClientMethods.ready(ready.value() == ClientCultivationData.get().netherworld().readiedSpell() ? null : ready.value());
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
		}
		if (button == 0 && bar != null && bar.maxScroll() > 0 && bar.contains(mouseX, mouseY)) {
			// The thumb keeps the point it was taken by; a click on the trough takes it by the middle and brings it there.
			barGrab = bar.onThumb(mouseY) ? (int) (mouseY - bar.thumbY()) : bar.thumbHeight() / 2;
			setScroll(bar.scrollFor(mouseY - barGrab));
			return true;
		}
		if (button == 0) {
			for (AbilitySwitch toggle : switches) {
				if (!toggle.contains(mouseX, mouseY)) continue;
				// The server flips it and syncs back; the switch shows the synced state.
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeVarInt(toggle.ability().ordinal());
				ClientPlayNetworking.send(ModPackets.TOGGLE_ABILITY, buf);
				if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/**
	 * Dropping a dragged ability: onto a slot puts it there (swapping with whatever was in it, if it came from another
	 * slot); a slot's ability dropped off the wheel empties that slot; anywhere else it goes back where it was.
	 */
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && barGrab >= 0) {
			barGrab = -1;
			return true;
		}
		if (button == 0 && dragged != null) {
			int cx = wheelX();
			int cy = wheelY();
			int target = AbilityWheel.slotUnder(mouseX, mouseY, cx, cy, EDIT_RING, EDIT_SLOT + 2);
			double dx = mouseX - cx, dy = mouseY - cy;
			if (target >= 0 && target != dragFrom) {
				placeOnWheel(target, dragged);
			} else if (target < 0 && dragFrom >= 0 && dx * dx + dy * dy > EDIT_BACKDROP * EDIT_BACKDROP) {
				placeOnWheel(dragFrom, null);
			}
			dragged = null;
			dragFrom = -1;
			super.mouseReleased(mouseX, mouseY, button);
			return true;
		}
		if (button == 0 && draggedSpell != null) {
			int cx = wheelX();
			int cy = wheelY();
			int target = AbilityWheel.slotUnder(mouseX, mouseY, cx, cy, EDIT_RING, EDIT_SLOT + 2);
			double dx = mouseX - cx, dy = mouseY - cy;
			if (target >= 0 && target != spellDragFrom) {
				placeSpell(target, draggedSpell);
			} else if (target < 0 && spellDragFrom >= 0 && dx * dx + dy * dy > EDIT_BACKDROP * EDIT_BACKDROP) {
				placeSpell(spellDragFrom, null);
			}
			draggedSpell = null;
			spellDragFrom = -1;
			super.mouseReleased(mouseX, mouseY, button);
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	/**
	 * One stat: the attribute's base value, then either the body's bonus (TwoPaths#body: the higher path's, Heaven's Favour and
	 * the harmony on it) and the current value, or (while suppressed or pressed) the value at the true cultivation without the
	 * pressure and the value now. The bonus of a percentage stat is a fraction, shown x100.
	 */
	private void statRow(GuiGraphics g, Player player, StatLine line, TwoPaths.Body body, TwoPaths.Body trueBody, boolean suppressed, int y) {
		Attribute attribute = line.attribute();
		double base = player == null ? 0 : player.getAttributeBaseValue(attribute);
		double bonus = bodyBonus(line, body);
		double now = valueWith(player, attribute, bonus, true);
		g.drawString(font, Component.translatable(line.nameKey()), left + 14, y, 0xE0E0E0);
		right(g, Component.literal(one(base * line.scale()) + line.unit()), left + BASE_RIGHT, y, 0xA0A0A0);
		if (suppressed) {
			double truth = valueWith(player, attribute, bodyBonus(line, trueBody), false);
			right(g, Component.literal(one(truth * line.scale()) + line.unit()), left + BONUS_RIGHT, y, 0xB39DDB);
			right(g, Component.literal(one(now * line.scale()) + line.unit()), left + WIDTH - 14, y, now < truth ? 0xFF8A80 : 0xFFFFFF);
		} else {
			// Below nothing (half a path's health, of the whole: TwoPaths#pathShare), the "bonus" is a loss, in red.
			double bonusShown = line.unit().isEmpty() ? bonus : bonus * 100;
			right(g, Component.literal((bonus < 0 ? "" : "+") + one(bonusShown) + line.unit()), left + BONUS_RIGHT, y,
					bonus > 0 ? 0x69F0AE : bonus < 0 ? 0xFF8A80 : 0x9E9E9E);
			right(g, Component.literal(one(now * line.scale()) + line.unit()), left + WIDTH - 14, y, 0xFFFFFF);
		}
	}

	/**
	 * The body's bonus to {@code line}'s stat, as CultivationStats#apply gives it (its path's share, on health of the whole; an Ancient
	 * Holy Body's health and strength, CultivationStats#holyFactor).
	 */
	private static double bodyBonus(StatLine line, TwoPaths.Body body) {
		double bonus = line.bonus().applyAsDouble(body.realm(), body.stage()) * body.scale();
		double holy = com.example.defyingtheheavens.CultivationStats.holyFactor(ClientCultivationData.get());
		if (line.attribute() == net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) {
			return (com.example.defyingtheheavens.CultivationStats.BASE_HEALTH + bonus) * body.share() * holy
					* com.example.defyingtheheavens.RaceGifts.healthFactor(ClientCultivationData.get()) - com.example.defyingtheheavens.CultivationStats.BASE_HEALTH;
		}
		if (line.attribute() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) return bonus * body.share() * holy;
		return bonus * body.share();
	}

	/**
	 * A qi row, laid out like {@link #statRow}. Qi has no vanilla base value, and the gather rate's current value includes
	 * the Upper Realm's multiplier ({@code now} above the realm's own value is shown in blue).
	 */
	private void qiRow(GuiGraphics g, String nameKey, ToDoubleBiFunction<Realm, Stage> value, double now, boolean rate,
					   PlayerCultivation c, boolean suppressed, int y) {
		double bonus = value.applyAsDouble(c.getEffectiveRealm(), c.getEffectiveStage()) * c.favourFactor();
		g.drawString(font, Component.translatable(nameKey), left + 14, y, 0xE0E0E0);
		right(g, Component.literal(qiText(0, rate)), left + BASE_RIGHT, y, 0xA0A0A0);
		if (suppressed) {
			double truth = value.applyAsDouble(c.getRealm(), c.getStage()) * c.favourFactor();
			right(g, Component.literal(qiText(truth, rate)), left + BONUS_RIGHT, y, 0xB39DDB);
			right(g, Component.literal(qiText(now, rate)), left + WIDTH - 14, y, now < truth ? 0xFF8A80 : 0xFFFFFF);
		} else {
			right(g, Component.literal("+" + qiText(bonus, rate)), left + BONUS_RIGHT, y, bonus > 0 ? 0x69F0AE : 0x9E9E9E);
			right(g, Component.literal(qiText(now, rate)), left + WIDTH - 14, y, now > bonus ? QI_UPPER_COLOR : 0xFFFFFF);
		}
	}

	/**
	 * Thousands separated, like the HUD: "8,000" and "2,000.0/s"; the realms after Four Axis's pools and rates in short ("3.25M",
	 * "81.3k/s"), so they keep to their columns.
	 */
	private static String qiText(double v, boolean rate) {
		if (Math.abs(v) >= (rate ? 10_000 : 1_000_000)) return compact(v) + (rate ? "/s" : "");
		return rate ? String.format(Locale.ROOT, "%,.1f/s", v) : num(v);
	}

	/** A great number in short: "81.3k", "3.25M", "1.60B". */
	private static String compact(double v) {
		double a = Math.abs(v);
		if (a >= 1e9) return String.format(Locale.ROOT, "%.2fB", v / 1e9);
		if (a >= 1e6) return String.format(Locale.ROOT, "%.2fM", v / 1e6);
		return String.format(Locale.ROOT, "%.1fk", v / 1e3);
	}

	// --- Methods: the ghost methods learned, their layers, and which one meditation cultivates ---

	/**
	 * How many of the methods are known, where the ghost path stands, how soon meditating here brings the practised one's next
	 * layer (or why it can't: the density, or the ghost path's gate), which layer of the deepest foundation carries the ghost path
	 * on (or, perfected, which foundation to seek), then
	 * each method: its stele's emblem and name, Practise (or "Practising"), nine pips for its layers, the bar through the current
	 * one, and what its layers bring. Methods not yet learned show only that they exist. Scrolls like the Abilities tab.
	 */
	private void renderMethods(GuiGraphics g, int mouseX, int mouseY) {
		// The pages, side by side at the top: the ghost path's methods and the heaven path's, and an immortal's Laws (phase 3).
		PlayerCultivation c = ClientCultivationData.get();
		boolean laws = com.example.defyingtheheavens.Laws.isImmortal(c) || c.laws().total() > 0 || c.laws().choices() > 0;
		if (methodPage == PAGE_LAWS && !laws) methodPage = PAGE_GHOST;
		int y = top + CONTENT_Y;
		int x = left + 14;
		for (int page = PAGE_GHOST; page <= (laws ? PAGE_LAWS : PAGE_HEAVEN); page++) {
			Component label = Component.translatable(page == PAGE_LAWS ? ModLang.METHODS_PAGE_LAWS : page == PAGE_HEAVEN ? ModLang.METHODS_PAGE_HEAVEN : ModLang.METHODS_PAGE_GHOST);
			int w = font.width(label) + 12;
			RowButton<Integer> button = new RowButton<>(page, x, y, w);
			methodPageButtons.add(button);
			boolean chosen = methodPage == page;
			int color = page == PAGE_LAWS ? LAWS_COLOR : page == PAGE_HEAVEN ? 0xFFD700 : 0x8FEAE4;
			drawRowButton(g, button, label, button.contains(mouseX, mouseY), chosen ? color : 0x9E9E9E, chosen);
			x += w + 4;
		}
		int viewTop = top + CONTENT_Y + METHOD_PAGE_ROW;
		if (methodPage == PAGE_LAWS) renderLaws(g, mouseX, mouseY, viewTop);
		else if (methodPage == PAGE_HEAVEN) renderHeavenMethods(g, mouseX, mouseY, viewTop);
		else renderGhostMethods(g, mouseX, mouseY, viewTop);
	}

	/** The Laws page's own colour: a pale violet, between the heavens' gold and the void's dark. */
	private static final int LAWS_COLOR = 0xE6C3FF;

	/**
	 * The Laws page (Laws, phase 3): how many levels of the Laws are held and what the way into the realm ahead wants, the Law in
	 * meditation's focus (comprehended only in the Immortal Realm), the levels owed for Decays weathered, when the Five Decays come (or
	 * which of their signs is on one); then each Law: its colour and name, Focus (or "In focus") and +1 while levels are owed, nine pips,
	 * the insight toward its next level, what its levels give and give now. The greater Laws greyed short of Golden Immortal. Scrolls as
	 * the other pages do.
	 */
	private void renderLaws(GuiGraphics g, int mouseX, int mouseY, int viewTop) {
		PlayerCultivation c = ClientCultivationData.get();
		com.example.defyingtheheavens.LawState s = c.laws();
		int x = left + 14;
		int rightEdge = left + LIST_RIGHT;
		int width = rightEdge - x;
		int viewBottom = top + HEIGHT - 8;
		boolean immortal = com.example.defyingtheheavens.Laws.isImmortal(c);
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

		// The top lines, measured before drawing so the scroll knows the height.
		List<FormattedCharSequence> topLines = new ArrayList<>();
		List<Integer> topColors = new ArrayList<>();
		java.util.function.BiConsumer<Component, Integer> addTop = (line, color) -> {
			for (FormattedCharSequence part : font.split(line, width)) {
				topLines.add(part);
				topColors.add(color);
			}
		};
		addTop.accept(Component.translatable(ModLang.LAWS_TOTAL, s.total()), 0xFFD700);
		if (!immortal) {
			addTop.accept(Component.translatable(ModLang.LAWS_SEALED), 0x9E9E9E);
		} else {
			int stages = Stage.values().length;
			int next = TwoPaths.higherRank(c, false) / stages + 1;
			if (next < Realm.values().length && Realm.byIndex(next).isPastTrueImmortal()) {
				Realm ahead = Realm.byIndex(next);
				int needed = com.example.defyingtheheavens.Laws.needed(ahead);
				boolean open = s.total() >= needed;
				addTop.accept(Component.translatable(open ? ModLang.LAWS_NEXT_OPEN : ModLang.LAWS_NEXT, ahead.getDisplayName(), needed), open ? 0x69F0AE : 0xF0D78C);
			}
			com.example.defyingtheheavens.Law focus = s.focus();
			boolean here = mc.level != null && com.example.defyingtheheavens.ModDimensions.hasImmortalQi(mc.level.dimension());
			addTop.accept(focus == null ? Component.translatable(ModLang.LAWS_NO_FOCUS)
					: Component.translatable(here ? ModLang.LAWS_FOCUS : ModLang.LAWS_FOCUS_AWAY, focus.getDisplayName()), focus == null ? 0x9E9E9E : here ? LAWS_COLOR : 0xB0A0C0);
			if (s.choices() > 0) addTop.accept(s.choices() == 1 ? Component.translatable(ModLang.LAWS_CHOICE_ONE) : Component.translatable(ModLang.LAWS_CHOICES, s.choices()), 0x69F0AE);
			int sign = s.decaySign();
			if (sign >= com.example.defyingtheheavens.FiveDecays.JUDGEMENT) {
				addTop.accept(Component.translatable(ModLang.LAWS_DECAY_JUDGEMENT), 0xFF8A80);
			} else if (sign > 0) {
				addTop.accept(Component.translatable(ModLang.LAWS_DECAY_SIGN, sign), 0xCE93D8);
			} else if (s.decayDue() > 0 && mc.level != null) {
				long due = s.decayDue() - mc.level.getGameTime();
				addTop.accept(due < 24000 ? Component.translatable(ModLang.LAWS_DECAY_SOON)
						: Component.translatable(ModLang.LAWS_DECAY_IN, (due + 23999) / 24000), 0xB39DDB);
			}
		}
		// Each Law's gains, wrapped, and what they give now.
		com.example.defyingtheheavens.Law[] laws = com.example.defyingtheheavens.Law.values();
		List<List<FormattedCharSequence>> gains = new ArrayList<>();
		int contentHeight = 6 + topLines.size() * 10 + 6;
		for (com.example.defyingtheheavens.Law law : laws) {
			List<FormattedCharSequence> lines = new ArrayList<>(font.split(law.getGains(), width));
			if (s.level(law) > 0) lines.addAll(font.split(lawNow(law, s.level(law)), width));
			gains.add(lines);
			contentHeight += 15 + 11 + 9 + 11 + lines.size() * 10 + 12;
		}
		methodMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		methodScroll = Mth.clamp(methodScroll, 0, methodMaxScroll);

		g.enableScissor(left + 1, viewTop, left + WIDTH - 1, viewBottom);
		int y = viewTop + 3 - methodScroll;
		for (int i = 0; i < topLines.size(); i++) {
			g.drawString(font, topLines.get(i), x, y, topColors.get(i));
			y += 10;
		}
		y += 6;
		for (int i = 0; i < laws.length; i++) {
			com.example.defyingtheheavens.Law law = laws[i];
			int level = s.level(law);
			boolean shut = law.isGreater() && !com.example.defyingtheheavens.Laws.greaterOpen(c);
			boolean comprehends = com.example.defyingtheheavens.Laws.mayComprehend(c, law);
			int color = shut ? 0x707070 : law.color();
			// Its mark: a small square of its colour in a gold frame.
			g.fill(x, y - 2, x + 11, y + 9, shut ? HallHud.RULE : BORDER);
			g.fill(x + 1, y - 1, x + 10, y + 8, 0xFF000000 | color);
			g.drawString(font, law.getDisplayName(), x + 16, y, shut ? 0x707070 : law == s.focus() ? LAWS_COLOR : 0xE0E0E0);
			int buttonRight = rightEdge;
			if (law == s.focus()) {
				Component focused = Component.translatable(ModLang.LAWS_FOCUSED);
				right(g, focused, buttonRight, y, 0x69F0AE);
				buttonRight -= font.width(focused) + 6;
			} else if (comprehends) {
				Component label = Component.translatable(ModLang.BTN_LAW_FOCUS);
				int w = font.width(label) + 10;
				RowButton<com.example.defyingtheheavens.Law> button = new RowButton<>(law, buttonRight - w, y - 3, w);
				boolean visible = button.y() >= viewTop && button.y() + SWITCH_H <= viewBottom;
				if (visible) lawFocusButtons.add(button);
				drawRowButton(g, button, label, visible && button.contains(mouseX, mouseY), 0xE0E0E0);
				buttonRight -= w + 4;
			}
			if (s.choices() > 0 && comprehends) {
				Component label = Component.translatable(ModLang.BTN_LAW_CHOOSE);
				int w = font.width(label) + 10;
				RowButton<com.example.defyingtheheavens.Law> button = new RowButton<>(law, buttonRight - w, y - 3, w);
				boolean visible = button.y() >= viewTop && button.y() + SWITCH_H <= viewBottom;
				if (visible) lawChooseButtons.add(button);
				drawRowButton(g, button, label, visible && button.contains(mouseX, mouseY), 0x69F0AE);
			}
			y += 15;
			drawPips(g, x, y, level);
			g.drawString(font, Component.translatable(ModLang.LAWS_LEVEL, level, com.example.defyingtheheavens.Laws.MAX_LEVEL), x + 9 * 9 + 6, y, 0xB0B0B0);
			y += 11;
			boolean complete = level >= com.example.defyingtheheavens.Laws.MAX_LEVEL;
			double fraction = complete ? 1 : Mth.clamp(s.insight(law) / com.example.defyingtheheavens.Laws.insightFor(level + 1), 0, 1);
			g.fill(x, y, rightEdge, y + 5, 0xFF6E5214);
			g.fill(x + 1, y + 1, rightEdge - 1, y + 4, 0xFF0C0C14);
			g.fill(x + 1, y + 1, x + 1 + (int) Math.round((rightEdge - x - 2) * fraction), y + 4, complete ? 0xFFFFD700 : 0xFF000000 | color);
			y += 9;
			Component progress = complete ? Component.translatable(ModLang.LAWS_COMPLETE) : shut ? Component.translatable(ModLang.LAWS_GREATER_SHUT)
					: Component.translatable(ModLang.LAWS_INSIGHT, level + 1, (int) Math.floor(fraction * 100));
			g.drawString(font, progress, x, y, complete ? 0xFFD700 : shut ? 0x707070 : 0xB0A0C0);
			y += 11;
			for (FormattedCharSequence line : gains.get(i)) {
				g.drawString(font, line, x, y, 0x8A8A8A);
				y += 10;
			}
			if (i < laws.length - 1) {
				y += 4;
				g.fill(x, y, rightEdge, y + 1, HallHud.RULE);
				y += 8;
			}
		}
		g.disableScissor();
		scrollbar(g, viewTop, viewBottom, contentHeight, methodScroll, methodMaxScroll, mouseX, mouseY);
	}

	/** What {@code law}'s {@code level} levels give now ("Now: your Fire spells +30% harm, +15% reach."), as Laws reckons it. */
	private static Component lawNow(com.example.defyingtheheavens.Law law, int level) {
		long harm = Math.round(level * com.example.defyingtheheavens.Laws.HARM_PER_LEVEL * 100);
		long reach = Math.round(level * com.example.defyingtheheavens.Laws.REACH_PER_LEVEL * 100);
		long sooner = Math.round(level * com.example.defyingtheheavens.Laws.COOLDOWN_PER_LEVEL * 100);
		long life = Math.round(level * com.example.defyingtheheavens.Laws.LIFE_PER_LEVEL * 100);
		String key = ModLang.lawKey(law.getId()) + ".now";
		return switch (law) {
			case SPACE -> Component.translatable(key, harm, reach, sooner);
			case TIME -> Component.translatable(key, sooner);
			case LIFE_AND_DEATH -> Component.translatable(key, life, life);
			default -> Component.translatable(key, harm, reach);
		};
	}

	/** The pages' buttons' row above either page's list. */
	private static final int METHOD_PAGE_ROW = SWITCH_H + 5;

	private void renderGhostMethods(GuiGraphics g, int mouseX, int mouseY, int viewTop) {
		PlayerCultivation c = ClientCultivationData.get();
		NetherworldState s = c.netherworld();
		if (!s.knowsAnyMethod()) {
			methodMaxScroll = 0;
			// A mortal can't take one up at all yet (its meridians closed): said so rather than sent to a stele.
			renderPlaceholder(g, ModLang.METHODS_TITLE, ModLang.METHODS_EMPTY, c.isMortal() ? ModLang.METHODS_GHOST_PATH_SEALED : ModLang.METHODS_HINT);
			return;
		}
		int x = left + 14;
		int rightEdge = left + LIST_RIGHT;
		int width = rightEdge - x;
		int viewBottom = top + HEIGHT - 8;

		// What the top says, wrapped, measured before drawing so the scroll knows the height.
		GhostMethod active = s.activeMethod();
		double density = ClientNetherworldData.density();
		List<FormattedCharSequence> rateLines = new ArrayList<>();
		int rateColor = 0x8FEAE4;
		// A mortal's methods lie dormant (GhostMethods#refusal): no pace, no reach, only the sealed path below.
		boolean sealed = c.isMortal();
		if (active != null && !sealed) {
			// The method practised's layers, as the heaven methods' (they are understood as far as the ghost path has come): its next
			// one settling in, how soon here; or waiting on the path; or all of it understood. Drawing in the heavens' qi, it rests.
			Component rate;
			int layer = s.layer(active);
			if (!TwoPaths.cultivatesGhost(c)) {
				rate = Component.translatable(ModLang.METHODS_GHOST_RESTING);
				rateColor = 0x9E9E9E;
			} else if (layer >= active.layers()) {
				rate = Component.translatable(ModLang.METHODS_PERFECTED);
				rateColor = 0xFFD700;
			} else if (GhostMethods.settling(s, active)) {
				if (density <= 0) {
					rate = Component.translatable(ModLang.METHODS_NO_QI);
					rateColor = 0x9E9E9E;
				} else {
					double perSecond = GhostMethods.ratePerSecond(c, density) * c.meditationSpeed(); // at the pace it is set to
					double minutes = (GhostMethods.settleCost(active, layer + 1) - s.progress(active)) / Math.max(1.0e-6, perSecond) / 60.0;
					rate = Component.translatable(ModLang.METHODS_HEAVEN_RATE, layer + 1, active.getDisplayName(), minutes < 1 ? "<1" : String.valueOf(Math.round(minutes)));
				}
			} else {
				rate = Component.translatable(ModLang.METHODS_GHOST_NEXT, active.getDisplayName(), GhostMethods.rankName(GhostMethods.opensAt(active, layer + 1)));
			}
			rateLines = font.split(rate, width);
		}
		// How far the foundations known open the ghost path's way (GhostMethods#opensWay): the deepest realm a breakthrough may lead into.
		List<FormattedCharSequence> floorLines = new ArrayList<>();
		GhostMethod foundation = GhostMethods.deepestFoundation(s);
		if (foundation != null && !sealed) {
			Realm reach = Realm.QI_REFINING;
			for (GhostMethod method : s.knownMethods()) if (method.isFoundation() && method.realm().ordinal() > reach.ordinal()) reach = method.realm();
			floorLines = font.split(Component.translatable(ModLang.METHODS_GHOST_REACH, reach.getDisplayName()), width);
		}
		List<List<FormattedCharSequence>> gains = new ArrayList<>();
		int ghostPath = GhostMethods.ghostRank(s);
		// A mortal's methods lie dormant, its meridians closed (TwoPaths#ghostRank): it has no ghost path yet, whatever it knows.
		Component pathLine = sealed ? Component.translatable(ModLang.METHODS_GHOST_PATH_SEALED)
				: ghostPath < 0 ? Component.translatable(ModLang.METHODS_GHOST_PATH_NONE)
				: Component.translatable(ModLang.METHODS_GHOST_PATH, GhostMethods.rankName(ghostPath));
		int contentHeight = 6 + 12 + 11 + rateLines.size() * 10 + floorLines.size() * 10 + 6;
		for (GhostMethod method : GhostMethod.values()) {
			List<FormattedCharSequence> lines = s.knows(method) ? font.split(method.getGains(), width) : font.split(Component.translatable(method.tier() >= 3 ? ModLang.METHODS_UNKNOWN_DEEP : ModLang.METHODS_UNKNOWN), width - 20);
			gains.add(lines);
			contentHeight += s.knows(method) ? 15 + 11 + (spansRealms(method) ? 10 : 0) + 9 + 11 + lines.size() * 10 : lines.size() * 10 + 4;
			contentHeight += 12;
		}
		methodMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		methodScroll = Mth.clamp(methodScroll, 0, methodMaxScroll);

		g.enableScissor(left + 1, viewTop, left + WIDTH - 1, viewBottom);
		int y = viewTop + 3 - methodScroll;
		int known = 0;
		for (GhostMethod method : GhostMethod.values()) if (s.knows(method)) known++;
		g.drawString(font, Component.translatable(ModLang.METHODS_KNOWN, known, GhostMethod.values().length), x, y, 0xFFD700);
		y += 12;
		g.drawString(font, pathLine, x, y, 0x8FEAE4);
		y += 11;
		for (FormattedCharSequence line : rateLines) {
			g.drawString(font, line, x, y, rateColor);
			y += 10;
		}
		for (FormattedCharSequence line : floorLines) {
			g.drawString(font, line, x, y, 0xB388FF);
			y += 10;
		}
		y += 6;
		GhostMethod[] methods = GhostMethod.values();
		for (int i = 0; i < methods.length; i++) {
			GhostMethod method = methods[i];
			if (!s.knows(method)) {
				drawEmblem(g, method, x, y - 3, false);
				for (FormattedCharSequence line : gains.get(i)) {
					g.drawString(font, line, x + 20, y, 0x707070);
					y += 10;
				}
				y += 4;
			} else {
				boolean isActive = method == active;
				drawEmblem(g, method, x, y - 4, true);
				g.drawString(font, method.getDisplayName(), x + 20, y, isActive ? 0xFFD700 : 0xE0E0E0);
				if (isActive) {
					right(g, Component.translatable(ModLang.METHODS_ACTIVE), rightEdge, y, 0x69F0AE);
				} else {
					int w = font.width(Component.translatable(ModLang.BTN_PRACTISE)) + 10;
					RowButton<GhostMethod> button = new RowButton<>(method, rightEdge - w, y - 3, w);
					boolean visible = button.y() >= viewTop && button.y() + SWITCH_H <= viewBottom;
					if (visible) practiseButtons.add(button);
					drawRowButton(g, button, Component.translatable(ModLang.BTN_PRACTISE), visible && button.contains(mouseX, mouseY), 0xE0E0E0);
				}
				y += 15;
				int layer = s.layer(method);
				if (spansRealms(method)) {
					// Nine small pips for each realm it spans, the realms apart; under them, the layer and where it stands in its realm.
					drawRealmPips(g, x, y, layer, method.layers(), rightEdge - x);
					y += 11;
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER_REALM, layer, method.layers(), MethodLayers.realmOf(layer).getDisplayName(),
							MethodLayers.within(layer)), x, y, 0xB0B0B0);
					y += 10;
				} else {
					drawPips(g, x, y, layer);
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER, layer, method.layers()), x + 9 * 9 + 6, y, 0xB0B0B0);
					y += 11;
				}
				// Its next layer: settling in behind the ghost path (how far), or waiting on the path to reach it; or all of it understood.
				boolean perfected = layer >= method.layers();
				boolean settling = !sealed && GhostMethods.settling(s, method);
				double fraction = perfected ? 1 : settling ? Mth.clamp(s.progress(method) / GhostMethods.settleCost(method, layer + 1), 0, 1) : 0;
				g.fill(x, y, rightEdge, y + 5, 0xFF6E5214);
				g.fill(x + 1, y + 1, rightEdge - 1, y + 4, 0xFF0C0C14);
				g.fill(x + 1, y + 1, x + 1 + (int) Math.round((rightEdge - x - 2) * fraction), y + 4, perfected ? 0xFFFFD700 : 0xFF4FC3D9);
				y += 9;
				Component opensAt = GhostMethods.rankName(GhostMethods.opensAt(method, layer + 1));
				Component progress = perfected ? Component.translatable(ModLang.METHODS_PERFECTED)
						: settling ? Component.translatable(ModLang.SETTLING_LINE, layer + 1, (int) Math.floor(fraction * 100))
						: fit(rightEdge - x, Component.translatable(ModLang.METHODS_HEAVEN_OPENS, layer + 1, opensAt),
						Component.translatable(ModLang.METHODS_HEAVEN_OPENS_SHORT, layer + 1, opensAt));
				g.drawString(font, progress, x, y, perfected ? 0xFFD700 : settling ? 0x8FEAE4 : 0x9A9A9A);
				y += 11;
				for (FormattedCharSequence line : gains.get(i)) {
					g.drawString(font, line, x, y, 0x8A8A8A);
					y += 10;
				}
			}
			if (i < methods.length - 1) {
				y += 4;
				g.fill(x, y, rightEdge, y + 1, HallHud.RULE);
				y += 8;
			}
		}
		g.disableScissor();
		scrollbar(g, viewTop, viewBottom, contentHeight, methodScroll, methodMaxScroll, mouseX, mouseY);
	}

	/**
	 * The heaven path's page: how many of the thirty are known, each element's passive the layers have strengthened, how soon
	 * meditating here brings the practised method's next layer and what share of the qi here is its element's (or why it can't
	 * go on), then the five elements in turn: each method of it known (its element's emblem and name, Practise, its layers realm
	 * by realm, the bar, what its layers bring), and how many of the element's six are not.
	 */
	private void renderHeavenMethods(GuiGraphics g, int mouseX, int mouseY, int viewTop) {
		PlayerCultivation c = ClientCultivationData.get();
		HeavenState h = c.heaven();
		boolean dead = c.netherworld().ghost();
		if (!h.knowsAny()) {
			methodMaxScroll = 0;
			renderPlaceholder(g, ModLang.METHODS_TITLE, ModLang.METHODS_HEAVEN_EMPTY,
					dead ? ModLang.METHODS_HEAVEN_DEAD : c.isMortal() ? ModLang.METHODS_HEAVEN_SEALED : ModLang.METHODS_HEAVEN_HINT);
			return;
		}
		int x = left + 14;
		int rightEdge = left + LIST_RIGHT;
		int width = rightEdge - x;
		int viewBottom = top + HEIGHT - 8;

		// The top lines, measured before drawing so the scroll knows the height.
		List<FormattedCharSequence> topLines = new ArrayList<>();
		List<Integer> topColors = new ArrayList<>();
		java.util.function.BiConsumer<Component, Integer> addTop = (line, color) -> {
			for (FormattedCharSequence part : font.split(line, width)) {
				topLines.add(part);
				topColors.add(color);
			}
		};
		addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_KNOWN, h.known().size(), HeavenMethod.values().length), 0xFFD700);
		for (QiElement element : QiElement.five()) {
			double amount = HeavenPassives.amount(c, element);
			if (amount <= 0) continue;
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_PASSIVE, Component.translatable(ModLang.passiveKey(element)),
					Component.translatable(ModLang.passiveKey(element) + ".amount", passiveAmount(amount))), element.getGlowColor() | 0x404040);
		}
		HeavenMethod active = HeavenMethods.practised(h);
		if (dead) {
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_DEAD), 0x9E9E9E);
		} else if (c.isMortal()) {
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_SEALED), 0x9E9E9E);
		} else {
			// The deepest realm a breakthrough may lead into (HeavenMethods#opensWay).
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_REACH, HeavenMethods.deepestRealm(h).getDisplayName()), 0xC8B88A);
		}
		if (!dead && !c.isMortal() && active != null) {
			int layer = h.layer(active);
			double share = shareHere(active.element());
			if (layer >= active.layers()) {
				addTop.accept(Component.translatable(ModLang.METHODS_PERFECTED), 0xFFD700);
			} else if (HeavenMethods.settling(c, active)) {
				// What meditating here gathers (as the Cultivation tab reckons it) by the method's element, against what is left of the
				// next layer's settling.
				Player player = minecraft == null ? null : minecraft.player;
				double boost = meditationBoost(player);
				double perSecond = c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(player)) * boost
						* HeavenMethods.methodFactor(c, share, false) * c.meditationSpeed(); // at the pace it is set to
				if (perSecond > 0) {
					double minutes = (HeavenMethods.settleCost(layer + 1) - h.progress(active)) / perSecond / 60.0;
					addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_RATE, layer + 1, active.getDisplayName(),
							minutes < 1 ? "<1" : String.valueOf(Math.round(minutes))), 0xF0D78C);
				}
			} else {
				addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_NEXT, active.getDisplayName(),
						GhostMethods.rankName(MethodLayers.floor(layer + 1))), 0xF0D78C);
			}
			// The element's share here, and what it makes of meditation's cultivation (perfected or not).
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_SHARE, active.element().getDisplayName(), Math.round(share * 100),
					String.format(Locale.ROOT, "%.2f", HeavenMethods.elementFactor(share))), 0xB0A070);
		}
		// Each element: its header, the methods of it known, then how many are not.
		List<HeavenMethod> known = new ArrayList<>();
		Map<HeavenMethod, List<FormattedCharSequence>> gains = new java.util.EnumMap<>(HeavenMethod.class);
		int contentHeight = 6 + topLines.size() * 10 + 4;
		for (QiElement element : QiElement.five()) {
			contentHeight += 14;
			int unknown = 0;
			for (Realm realm : Realm.values()) {
				HeavenMethod method = HeavenMethod.of(element, realm);
				if (method == null) continue; // the ladder past True Immortal: no method teaches it (Realm#LAST_TAUGHT)
				if (!h.knows(method)) {
					unknown++;
					continue;
				}
				known.add(method);
				List<FormattedCharSequence> lines = font.split(method.getGains(), width);
				gains.put(method, lines);
				contentHeight += 15 + 11 + (method.layers() > MethodLayers.PER_REALM ? 10 : 0) + 9 + 11 + lines.size() * 10 + 6;
			}
			if (unknown > 0) contentHeight += font.split(unknownLine(element, unknown), width - 20).size() * 10 + 4;
			contentHeight += 8;
		}
		methodMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		methodScroll = Mth.clamp(methodScroll, 0, methodMaxScroll);

		g.enableScissor(left + 1, viewTop, left + WIDTH - 1, viewBottom);
		int y = viewTop + 3 - methodScroll;
		for (int i = 0; i < topLines.size(); i++) {
			g.drawString(font, topLines.get(i), x, y, topColors.get(i));
			y += 10;
		}
		y += 4;
		for (QiElement element : QiElement.five()) {
			// The element's header: its name in its colour, a rule after it.
			Component name = element.getDisplayName();
			int color = element == QiElement.METAL ? 0xFFE8C060 : 0xFF000000 | element.getCoreColor();
			g.drawString(font, name, x, y, color);
			g.fill(x + font.width(name) + 4, y + 4, rightEdge, y + 5, (color & 0x00FFFFFF) | 0x60000000);
			y += 14;
			int unknown = 0;
			for (Realm realm : Realm.values()) {
				HeavenMethod method = HeavenMethod.of(element, realm);
				if (method == null) continue;
				if (!h.knows(method)) {
					unknown++;
					continue;
				}
				boolean isActive = method == active;
				drawElementEmblem(g, element, x, y - 4, true);
				g.drawString(font, method.getDisplayName(), x + 20, y, isActive ? 0xFFD700 : 0xE0E0E0);
				if (isActive) {
					right(g, Component.translatable(ModLang.METHODS_ACTIVE), rightEdge, y, 0x69F0AE);
				} else if (!dead) {
					int w = font.width(Component.translatable(ModLang.BTN_PRACTISE)) + 10;
					RowButton<HeavenMethod> button = new RowButton<>(method, rightEdge - w, y - 3, w);
					boolean visible = button.y() >= viewTop && button.y() + SWITCH_H <= viewBottom;
					if (visible) heavenPractiseButtons.add(button);
					drawRowButton(g, button, Component.translatable(ModLang.BTN_PRACTISE), visible && button.contains(mouseX, mouseY), 0xE0E0E0);
				}
				y += 15;
				int layer = h.layer(method);
				if (method.layers() > MethodLayers.PER_REALM) {
					drawRealmPips(g, x, y, layer, method.layers(), rightEdge - x);
					y += 11;
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER_REALM, layer, method.layers(), MethodLayers.realmOf(layer).getDisplayName(),
							MethodLayers.within(layer)), x, y, 0xB0B0B0);
					y += 10;
				} else {
					drawPips(g, x, y, layer);
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER, layer, method.layers()), x + 9 * 9 + 6, y, 0xB0B0B0);
					y += 11;
				}
				// Its next layer: settling in behind the realm (how far), or waiting on the realm to reach it; or all of it understood.
				boolean perfected = layer >= method.layers();
				boolean settling = !dead && !c.isMortal() && HeavenMethods.settling(c, method);
				double fraction = perfected ? 1 : settling ? Mth.clamp(h.progress(method) / HeavenMethods.settleCost(layer + 1), 0, 1) : 0;
				g.fill(x, y, rightEdge, y + 5, 0xFF6E5214);
				g.fill(x + 1, y + 1, rightEdge - 1, y + 4, 0xFF0C0C14);
				g.fill(x + 1, y + 1, x + 1 + (int) Math.round((rightEdge - x - 2) * fraction), y + 4, perfected ? 0xFFFFD700 : 0xFF000000 | element.getGlowColor());
				y += 9;
				Component opensAt = GhostMethods.rankName(MethodLayers.floor(layer + 1));
				Component progress = perfected ? Component.translatable(ModLang.METHODS_PERFECTED)
						: settling ? Component.translatable(ModLang.SETTLING_LINE, layer + 1, (int) Math.floor(fraction * 100))
						: fit(rightEdge - x, Component.translatable(ModLang.METHODS_HEAVEN_OPENS, layer + 1, opensAt),
						Component.translatable(ModLang.METHODS_HEAVEN_OPENS_SHORT, layer + 1, opensAt));
				g.drawString(font, progress, x, y, perfected ? 0xFFD700 : settling ? 0xF0D78C : 0x9A9A9A);
				y += 11;
				for (FormattedCharSequence line : gains.get(method)) {
					g.drawString(font, line, x, y, 0x8A8A8A);
					y += 10;
				}
				y += 6;
			}
			if (unknown > 0) {
				drawElementEmblem(g, element, x, y - 3, false);
				for (FormattedCharSequence line : font.split(unknownLine(element, unknown), width - 20)) {
					g.drawString(font, line, x + 20, y, 0x707070);
					y += 10;
				}
				y += 4;
			}
			y += 8;
		}
		g.disableScissor();
		scrollbar(g, viewTop, viewBottom, contentHeight, methodScroll, methodMaxScroll, mouseX, mouseY);
	}

	/** "4 of the 6 Wood methods are unknown...": how many of {@code element}'s are still to be found (one a realm, to True Immortal's). */
	private static Component unknownLine(QiElement element, int unknown) {
		return Component.translatable(ModLang.METHODS_HEAVEN_UNKNOWN, unknown, Realm.LAST_TAUGHT.ordinal() + 1, element.getDisplayName());
	}

	/** A passive's amount as the Methods tab shows it: whole where it is whole, else to two places. */
	private static String passiveAmount(double amount) {
		return Math.abs(amount - Math.rint(amount)) < 1.0e-6 ? String.valueOf((long) Math.rint(amount)) : String.format(Locale.ROOT, "%.2f", amount);
	}

	/**
	 * Meditation's boost as the server reckons it: the surroundings' where the player stands (CultivationBoost), or, the soul in the
	 * Inner Realm, the body's (told by the server: ClientInnerPlace) with the Inner Realm's own x1.25.
	 */
	private static double meditationBoost(Player player) {
		if (player == null) return 1.0;
		return ClientInnerPlace.active() ? ClientInnerPlace.boost() : CultivationBoost.of(player).multiplier();
	}

	/** What share of the qi where the player stands is {@code element} (QiElement#share), as the server reckons it (in the Inner Realm, the body's). */
	private static double shareHere(QiElement element) {
		if (ClientInnerPlace.active()) return ClientInnerPlace.share(element);
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return 0;
		net.minecraft.core.BlockPos pos = mc.player.blockPosition();
		boolean underground = mc.level.dimensionType().hasSkyLight() && !mc.level.dimensionType().hasCeiling() && pos.getY() < mc.level.getSeaLevel()
				&& mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos) < mc.level.getMaxLightLevel();
		return QiElement.share(mc.level.getBiome(pos), underground, element);
	}

	/** An element's emblem (the one carved on its Dao Steles), 16 pixels square; dark for one of its methods not yet learned. */
	private static void drawElementEmblem(GuiGraphics g, QiElement element, int x, int y, boolean lit) {
		if (!lit) g.setColor(0.3F, 0.3F, 0.36F, 1.0F);
		g.blit(new net.minecraft.resources.ResourceLocation(com.example.defyingtheheavens.DefyingTheHeavens.MOD_ID,
				"textures/block/dao_stele_" + element.name().toLowerCase(Locale.ROOT) + ".png"), x, y, 16, 16, 0, 0, 16, 16, 16, 16);
		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** A method's emblem (the one carved on its stele), 16 pixels square; one not yet learned, dark. */
	private static void drawEmblem(GuiGraphics g, GhostMethod method, int x, int y, boolean lit) {
		if (!lit) g.setColor(0.3F, 0.3F, 0.36F, 1.0F);
		g.blit(new net.minecraft.resources.ResourceLocation(com.example.defyingtheheavens.DefyingTheHeavens.MOD_ID,
				"textures/block/method_stele_" + method.getId() + ".png"), x, y, 16, 16, 0, 0, 16, 16, 16, 16);
		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** Whether {@code method} spans more than one realm (a foundation past the Breathing Art: MethodLayers), drawn realm by realm. */
	private static boolean spansRealms(GhostMethod method) {
		return method.layers() > MethodLayers.PER_REALM;
	}

	/**
	 * A narrow pip for each of {@code total} layers, nine to a realm, a gap between realms: gold to the layer reached, the realm
	 * it is in lit a little, dark beyond.
	 */
	private static void drawRealmPips(GuiGraphics g, int x, int y, int layer, int total, int width) {
		int realmNow = (Math.max(1, layer) - 1) / MethodLayers.PER_REALM;
		int realms = (total + MethodLayers.PER_REALM - 1) / MethodLayers.PER_REALM;
		// Narrower pips for the deepest methods (a True Immortal one's 99 layers), so they keep to the page.
		int pitch = 4;
		while (pitch > 2 && pitch * (total + realms - 1) > width) pitch--;
		for (int i = 0; i < total; i++) {
			int realm = i / MethodLayers.PER_REALM;
			int px = x + (i + realm) * pitch;
			boolean reached = i < layer;
			if (pitch >= 4) {
				g.fill(px, y, px + 3, y + 7, reached ? BORDER : realm == realmNow ? HallHud.FRAME : HallHud.RULE);
				g.fill(px + 1, y + 1, px + 2, y + 6, reached ? 0xFFFFD700 : 0xFF0B0E12);
			} else {
				g.fill(px, y, px + pitch - 1, y + 7, reached ? 0xFFFFD700 : realm == realmNow ? HallHud.FRAME : HallHud.RULE);
			}
		}
	}

	/** Nine pips, one a layer: gold to the layer reached, dark beyond it. */
	private static void drawPips(GuiGraphics g, int x, int y, int layer) {
		for (int i = 0; i < GhostMethods.LAYERS; i++) {
			int px = x + i * 9;
			boolean reached = i < layer;
			g.fill(px, y, px + 7, y + 7, reached ? BORDER : HallHud.RULE);
			g.fill(px + 1, y + 1, px + 6, y + 6, reached ? 0xFFFFD700 : 0xFF0B0E12);
			if (reached) g.fill(px + 1, y + 1, px + 3, y + 3, 0xFFFFF4C0); // a glint
		}
	}

	/** A small framed button on a list row, in the switches' colours. */
	private void drawRowButton(GuiGraphics g, RowButton<?> button, Component label, boolean hovered, int color) {
		drawRowButton(g, button, label, hovered, color, false);
	}

	/** One of a row of page buttons (the Stats and Methods tabs'): the page open in jade, as the Hall's open tab. */
	private void drawRowButton(GuiGraphics g, RowButton<?> button, Component label, boolean hovered, int color, boolean open) {
		HallHud.panel(g, button.x(), button.y(), button.x() + button.w(), button.y() + SWITCH_H, open || hovered ? HallHud.FRAME_LIT : HALL_FRAME,
				open ? HallHud.ROW_OPEN : hovered ? TAB_HOVER : TAB_IDLE);
		g.drawCenteredString(font, label, button.x() + button.w() / 2, button.y() + (SWITCH_H - 8) / 2, color);
	}

	// --- Spells: what the methods have taught, which is readied, and the spell wheel ---

	/**
	 * Each spell a method known teaches (cast or not yet): its icon and name, Ready (or "Readied"), its cost and cooldown, and
	 * what it does; one not yet reached says which layer brings it. Or, arranging, the spell wheel's editor. Scrolls like the
	 * Abilities tab.
	 */
	private void renderSpells(GuiGraphics g, int mouseX, int mouseY) {
		if (!ClientMethods.anyKnown()) {
			spellMaxScroll = 0;
			renderPlaceholder(g, ModLang.SPELLS_TITLE, ModLang.SPELLS_EMPTY, ModLang.SPELLS_HINT);
			return;
		}
		if (arrangingSpells) {
			renderSpellWheelEditor(g, mouseX, mouseY);
			return;
		}
		NetherworldState s = ClientCultivationData.get().netherworld();
		int x = left + 14;
		int rightEdge = left + LIST_RIGHT;
		int viewTop = top + CONTENT_Y;
		int viewBottom = top + ABILITIES_BOTTOM;
		// A method's spells once the method is known; the spells of both paths (TwoPaths) once the heaven path and the Demon Path are both
		// walked, to the living.
		boolean both = !s.ghost() && TwoPaths.walksHeavenAndDemon(ClientCultivationData.get());
		// A heaven method's spells once it is known, to the living (the dead cast none of them).
		PlayerCultivation self = ClientCultivationData.get();
		// The elements' in the order of their realms, each element's together, each in its place in its realm (a spell added since, at the
		// enum's end, among its own: Return to the Bough among the Four Axis wood spells); the ghost methods' and both paths' as they stand.
		List<Spell> shown = Arrays.stream(Spell.values()).filter(spell -> spell.isDual() ? both
				: spell.isHeaven() ? !s.ghost() && spell.taughtBy(self.heaven()) : s.knows(spell.method()))
				.sorted(java.util.Comparator.comparingInt(CultivationScreen::spellOrder)).toList();
		List<List<FormattedCharSequence>> descriptions = new ArrayList<>();
		int contentHeight = 6;
		for (int i = 0; i < shown.size(); i++) {
			Spell spell = shown.get(i);
			// Known, what it does; a heaven spell not yet, the stage one's cultivation must reach for it too (Spell#realmNeeded).
			List<FormattedCharSequence> lines = ClientMethods.known(spell) ? font.split(spell.getDescription(), rightEdge - x)
					: spell.isHeaven() ? font.split(Component.translatable(ModLang.SPELLS_LOCKED_STAGE, GhostMethods.rankName(spell.realmNeeded())), rightEdge - x)
					: List.of();
			descriptions.add(lines);
			contentHeight += 15 + 10 + lines.size() * 10 + (i < shown.size() - 1 ? 12 : 0);
		}
		spellMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		spellScroll = Mth.clamp(spellScroll, 0, spellMaxScroll);

		g.enableScissor(left + 1, viewTop, left + WIDTH - 1, viewBottom);
		int y = viewTop + 3 - spellScroll;
		for (int i = 0; i < shown.size(); i++) {
			Spell spell = shown.get(i);
			boolean known = ClientMethods.known(spell);
			boolean readied = known && spell == s.readiedSpell();
			SpellWheelScreen.drawIcon(g, spell, x, y - 4, 16, known);
			g.drawString(font, spell.getDisplayName(), x + 20, y, readied ? 0xFFD700 : known ? 0xE0E0E0 : 0x707070);
			if (known) {
				Component label = Component.translatable(readied ? ModLang.SPELLS_READIED : ModLang.SPELLS_READY);
				int w = Math.max(SWITCH_W + 16, font.width(label) + 10);
				RowButton<Spell> button = new RowButton<>(spell, rightEdge - w, y - 3, w);
				boolean visible = button.y() >= viewTop && button.y() + SWITCH_H <= viewBottom;
				if (visible) readyButtons.add(button);
				drawRowButton(g, button, label, visible && button.contains(mouseX, mouseY), readied ? 0x69F0AE : 0xB0B0B0);
				if (spell == Spell.LIFE_FORCE_DRAIN || spell == Spell.LIFE_FORCE_GAIN) {
					// What it moves (the user's ask, 2026-10-25: "the wood life force give and take ability modes, qi, hp and years of life").
					Component mode = Component.translatable(s.lifeMode().key());
					int mw = font.width(mode) + 10;
					RowButton<Spell> modeButton = new RowButton<>(spell, rightEdge - w - 4 - mw, y - 3, mw);
					if (visible) lifeModeButtons.add(modeButton);
					drawRowButton(g, modeButton, mode, visible && modeButton.contains(mouseX, mouseY), s.lifeMode().colour());
				}
			}
			y += 15;
			Component cost = spell.isHeaven() || spell.isDual() ? Component.translatable(ModLang.SPELLS_QI_COST, Math.round(spell.qiCost()))
					: Component.translatable(ModLang.SPELLS_COST, Math.round(spell.cost()));
			Component teacher = spell.isHeaven() ? spell.teacher(self.heaven()).getDisplayName() : spell.isDual() ? Component.empty() : spell.method().getDisplayName();
			Component detail = known
					? cost.copy().append(", ").append(Component.translatable(ModLang.SPELLS_COOLDOWN, seconds(spell.cooldownTicks())))
					: spell.isDual() ? Component.translatable(ModLang.SPELLS_LOCKED_BOTH, spell.dualRealm().getDisplayName())
					: spell.layer() > MethodLayers.PER_REALM // a method's spell in its own realm past the first: where in it, too
					? Component.translatable(ModLang.SPELLS_LOCKED_REALM, teacher, spell.layer(),
							MethodLayers.realmOf(spell.layer()).getDisplayName(), MethodLayers.within(spell.layer()))
					: Component.translatable(ModLang.SPELLS_LOCKED, teacher, spell.layer());
			g.drawString(font, detail, x + 20, y, known ? 0x7FDBE8 : 0x707070);
			y += 10;
			for (FormattedCharSequence line : descriptions.get(i)) {
				g.drawString(font, line, x, y, 0xB0B0B0);
				y += 10;
			}
			if (i < shown.size() - 1) {
				y += 4;
				g.fill(x, y, rightEdge, y + 1, HallHud.RULE);
				y += 8;
			}
		}
		g.disableScissor();
		scrollbar(g, viewTop, viewBottom, contentHeight, spellScroll, spellMaxScroll, mouseX, mouseY);
	}

	/** Where {@code spell} stands in the Spells tab: the ghost methods' first, then both paths', then the elements' by realm, element and place. */
	private static int spellOrder(Spell spell) {
		if (!spell.isHeaven()) return (spell.isDual() ? 10000 : 0) + spell.ordinal();
		// Its element's place in its realm: where that element's first spell of the realm stands among the spells.
		int realm = spell.layer() / MethodLayers.PER_REALM, first = spell.ordinal();
		for (Spell other : Spell.values()) {
			if (other.isHeaven() && other.element() == spell.element() && other.layer() / MethodLayers.PER_REALM == realm) first = Math.min(first, other.ordinal());
		}
		return 100000 + realm * 10000 + first * 20 + spell.layer() % MethodLayers.PER_REALM;
	}

	/** Ticks as seconds, a decimal only where it isn't whole: "2", "1.5". */
	private static String seconds(int ticks) {
		return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
	}

	/**
	 * The spell wheel's editor, as the ability wheel's: the wheel on the left, the spells that can be cast on the right; dragging
	 * one onto a slot puts it there (trading places with the slot's), a slot dragged off the wheel or right-clicked empties. The
	 * spells, with the hint under them, scroll in the room above the button: one who knows many methods can cast more spells
	 * than the panel holds, and drawn whole they ran under the button and off the panel's foot.
	 */
	private void renderSpellWheelEditor(GuiGraphics g, int mouseX, int mouseY) {
		NetherworldState s = ClientCultivationData.get().netherworld();
		int cx = wheelX();
		int cy = wheelY();
		int hoveredSlot = AbilityWheel.slotUnder(mouseX, mouseY, cx, cy, EDIT_RING, EDIT_SLOT);
		Spell pointed = hoveredSlot < 0 ? null : ClientMethods.shown(hoveredSlot);

		AbilityWheel.drawBackdrop(g, cx, cy, EDIT_BACKDROP);
		for (int i = 0; i < Spells.WHEEL_SLOTS; i++) {
			Spell spell = ClientMethods.shown(i);
			if (draggedSpell != null && i == spellDragFrom) spell = null;
			int sx = AbilityWheel.slotX(cx, i, EDIT_RING), sy = AbilityWheel.slotY(cy, i, EDIT_RING);
			if (i == hoveredSlot) AbilityWheel.ring(g, sx, sy, EDIT_SLOT + 3, EDIT_SLOT + 1, 0x80FFD700);
			AbilityWheel.disc(g, sx, sy, EDIT_SLOT + 1, spell == null ? AbilityWheel.SLOT_EMPTY_RIM : AbilityWheel.GOLD);
			AbilityWheel.disc(g, sx, sy, EDIT_SLOT, i == hoveredSlot ? AbilityWheel.SLOT_HOVER : AbilityWheel.SLOT);
			if (spell != null) SpellWheelScreen.drawIcon(g, spell, sx - 8, sy - 8, 16, true);
		}

		int px = left + PALETTE_X;
		int y = top + CONTENT_Y;
		g.drawString(font, Component.translatable(ModLang.SPELLS_TITLE), px, y, 0xFFD700);
		y += 12;
		int viewTop = y;
		int viewBottom = top + ABILITIES_BOTTOM;
		int perRow = (WIDTH - 12 - PALETTE_X + CELL_GAP) / (CELL + CELL_GAP);
		List<Spell> castable = Arrays.stream(Spell.values()).filter(ClientMethods::known).toList();
		int rows = (castable.size() + perRow - 1) / perRow;
		// Wrapped short of the scrollbar, which stands at the panel's edge when the spells overflow.
		int textWidth = SCROLLBAR_X - 4 - PALETTE_X;
		Component hint = Component.translatable(ModLang.SPELLS_ARRANGE_HINT,
				ModKeybinds.SPELL_WHEEL.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(0xFFD700)),
				ModKeybinds.CAST_SPELL.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(0xFFD700)));
		List<FormattedCharSequence> hintLines = font.split(hint, textWidth);
		int contentHeight = rows * (CELL + CELL_GAP) + 4 + hintLines.size() * 10;
		spellPaletteMaxScroll = Math.max(0, contentHeight - (viewBottom - viewTop));
		spellPaletteScroll = Mth.clamp(spellPaletteScroll, 0, spellPaletteMaxScroll);

		g.enableScissor(px, viewTop, left + SCROLLBAR_X - 1, viewBottom);
		y = viewTop - spellPaletteScroll;
		for (int i = 0; i < castable.size(); i++) {
			Spell spell = castable.get(i);
			SpellCell cell = new SpellCell(spell, px + (i % perRow) * (CELL + CELL_GAP), y + (i / perRow) * (CELL + CELL_GAP));
			// Only a cell wholly in view can be taken (as the list's Ready buttons).
			boolean visible = cell.y() >= viewTop && cell.y() + CELL <= viewBottom;
			if (visible) spellPalette.add(cell);
			boolean hovered = visible && draggedSpell == null && cell.contains(mouseX, mouseY);
			if (hovered) pointed = spell;
			boolean placed = false;
			for (int slot = 0; slot < Spells.WHEEL_SLOTS; slot++) placed |= s.spellSlot(slot) == spell;
			g.fill(cell.x(), cell.y(), cell.x() + CELL, cell.y() + CELL, placed ? AbilityWheel.GOLD : AbilityWheel.SLOT_OFF_RIM);
			g.fill(cell.x() + 1, cell.y() + 1, cell.x() + CELL - 1, cell.y() + CELL - 1, hovered ? AbilityWheel.SLOT_HOVER : AbilityWheel.SLOT);
			SpellWheelScreen.drawIcon(g, spell, cell.x() + (CELL - 16) / 2, cell.y() + (CELL - 16) / 2, 16, true);
		}
		y += rows * (CELL + CELL_GAP) + 4;
		for (FormattedCharSequence line : hintLines) {
			g.drawString(font, line, px, y, 0x8A8A8A);
			y += 10;
		}
		g.disableScissor();
		if (spellPaletteMaxScroll > 0) scrollbar(g, viewTop, viewBottom, contentHeight, spellPaletteScroll, spellPaletteMaxScroll, mouseX, mouseY);

		int centreWidth = 2 * (EDIT_RING - EDIT_SLOT) - 8;
		Spell named = draggedSpell != null ? draggedSpell : pointed;
		List<FormattedCharSequence> lines = font.split(named == null ? Component.translatable(ModLang.SPELL_WHEEL_TITLE) : named.getDisplayName(), centreWidth);
		int ty = cy - lines.size() * 10 / 2;
		for (FormattedCharSequence line : lines) {
			g.drawCenteredString(font, line, cx, ty, 0xFFD700);
			ty += 10;
		}
	}

	/** Puts a spell in a slot of the spell wheel (null empties it): at once here, and on the server, which syncs back. */
	private void placeSpell(int slot, Spell spell) {
		ClientCultivationData.get().netherworld().setSpellSlot(slot, spell);
		ClientMethods.setSlot(slot, spell);
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}

	// --- Placeholders: a title, what's missing, and a hint ---

	/** A tab with nothing of its own yet: its title, what it would hold, and a hint ({@code hintKey} null: none). @return the y under it */
	private int renderPlaceholder(GuiGraphics g, String titleKey, String emptyKey, String hintKey) {
		int cx = left + WIDTH / 2;
		int y = top + CONTENT_Y + 30;
		g.drawCenteredString(font, Component.translatable(titleKey), cx, y, 0xFFD700);
		y += 20;
		for (FormattedCharSequence line : font.split(Component.translatable(emptyKey), WIDTH - 28)) {
			g.drawCenteredString(font, line, cx, y, 0xB0B0B0);
			y += 11;
		}
		if (hintKey == null) return y;
		for (FormattedCharSequence line : font.split(Component.translatable(hintKey), WIDTH - 28)) {
			g.drawCenteredString(font, line, cx, y, 0x808080);
			y += 11;
		}
		return y;
	}

	/**
	 * A mortal's Stats tab: only a tempered body to speak of, and what comes next (tempering on to Peak, then the meridians to open;
	 * a shattered body refines itself instead, on the Cultivation tab; a mortal's ghost tempers nothing), and their years (Lifespan)
	 * as a cultivator's page says them.
	 */
	private void renderMortalStats(GuiGraphics g) {
		PlayerCultivation c = ClientCultivationData.get();
		String hint = c.hasShatteredMeridians() || ClientCultivationData.lifeDead() ? null
				: c.getMortalStage().isLast() ? ModLang.MORTAL_DETAIL : ModLang.MORTAL_HOW;
		int y = renderPlaceholder(g, ModLang.MORTAL, ModLang.MORTAL_STATS, hint) + 8;
		double used = ClientCultivationData.lifeUsed(), span = ClientCultivationData.lifeSpan();
		Component years = Double.isInfinite(span) ? Component.translatable(ModLang.STATS_LIFESPAN_ENDLESS, (long) Math.floor(used))
				: Component.translatable(ModLang.STATS_LIFESPAN, (long) Math.floor(used), (long) Math.floor(span));
		g.drawCenteredString(font, years, left + WIDTH / 2, y, span - used <= com.example.defyingtheheavens.Lifespan.WARN_YEARS ? 0xFF6E6E : 0xC8B88A);
	}

	private void right(GuiGraphics g, Component text, int rightX, int y, int color) {
		g.drawString(font, text, rightX - font.width(text), y, color);
	}

	/**
	 * What an attribute comes to with the cultivation bonus set to {@code cultivationBonus} and everything else (gear,
	 * effects) as it is now, computed the way vanilla does. With the player's own bonus this is their current value; with
	 * another stage's bonus it's what they would have there. The server only sends some attributes to clients (max
	 * health, speed, armor, toughness); for the others (attack damage, knockback resistance) the client sees no
	 * modifiers, so those come out as the base value plus the bonus (bare-handed, without gear or effects).
	 * {@code withPressure} false leaves Realm Suppress's modifiers out, for the value the true cultivation would have.
	 */
	private static double valueWith(Player player, Attribute attribute, double cultivationBonus, boolean withPressure) {
		AttributeInstance instance = player == null ? null : player.getAttribute(attribute);
		if (instance == null) return 0;
		AttributeModifier.Operation cultivationOp = CultivationStats.operation(attribute);
		double added = instance.getBaseValue() + (cultivationOp == AttributeModifier.Operation.ADDITION ? cultivationBonus : 0);
		if (!attribute.isClientSyncable()) {
			// Realm Suppress's share is known even so, from the synced pressure.
			boolean pressed = withPressure && RealmSuppressSystem.lowers(attribute);
			return attribute.sanitizeValue(added) * (pressed ? 1 - ClientCultivationData.get().getPressurePenalty() : 1);
		}
		List<AttributeModifier> others = instance.getModifiers().stream()
				.filter(modifier -> !CultivationStats.isCultivationModifier(modifier.getId()))
				.filter(modifier -> withPressure || !RealmSuppressSystem.isPressureModifier(modifier.getId()))
				.toList();
		for (AttributeModifier modifier : others) {
			if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) added += modifier.getAmount();
		}
		double value = added + (cultivationOp == AttributeModifier.Operation.MULTIPLY_BASE ? added * cultivationBonus : 0);
		for (AttributeModifier modifier : others) {
			if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) value += added * modifier.getAmount();
		}
		for (AttributeModifier modifier : others) {
			if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) value *= 1.0 + modifier.getAmount();
		}
		return attribute.sanitizeValue(value);
	}

	/**
	 * {@code v} to one decimal; from 100,000 by thousands, millions, billions or trillions (104.8B), so a high realm's numbers (a True
	 * Immortal's cultivation a second runs to eleven figures) fit their columns.
	 */
	private static String one(double v) {
		double a = Math.abs(v);
		if (a < 100_000) return String.format(Locale.ROOT, "%.1f", v);
		String[] units = {"K", "M", "B", "T"};
		int u = Math.min(units.length - 1, (int) (Math.log10(a) / 3) - 1);
		return String.format(Locale.ROOT, "%.1f%s", v / Math.pow(1000, u + 1), units[u]);
	}

	private static String num(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	/**
	 * A bar's number when its whole figures won't fit the bar (a Heavenly Lord's cultivation runs to seventeen): in short from a million,
	 * "3.25M", "56.29Q" (to quadrillions), else whole.
	 */
	private static String big(double v) {
		double a = Math.abs(v);
		if (a < 1_000_000) return num(v);
		String[] units = {"M", "B", "T", "Q"};
		int u = Math.min(units.length - 1, (int) (Math.log10(a) / 3) - 2);
		return String.format(Locale.ROOT, "%.2f%s", v / Math.pow(1000, u + 2), units[u]);
	}

	/** A tab in the Hall's column of tabs ({@link #navRow}). */
	private final class TabButton extends AbstractButton {
		private final Tab target;

		TabButton(int x, int y, int width, int height, Tab target) {
			super(x, y, width, height, Component.translatable(target.key));
			this.target = target;
		}

		@Override
		public void onPress() {
			tab = target;
			dragged = null;
			updateButtons();
			if (target == Tab.JOURNAL) ClientJournal.request(); // fresh from the server
			if (target == Tab.SECT) ClientSect.request();
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			boolean selected = tab == target;
			navRow(g, target.ordinal(), getMessage(), selected, isHoveredOrFocused(), false);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}

	/**
	 * Row {@code row} of the Hall's column of tabs as the menu's mockup draws it (in the artifact's pixels): 261 by 48, jade under the one
	 * open with a gold diamond and its name in gold, the rest lit as they're pointed at; the gold square of what is unread at its right.
	 */
	private void navRow(GuiGraphics g, int row, Component label, boolean selected, boolean hovered, boolean unread) {
		float top = A_ROW_TOP + A_ROW * row;
		HallArt.beginFramed(g);
		try {
			if (selected) HallArt.rect(g, 0, top, 261, top + 48, 0xFF1E3A32);
			else if (hovered) HallArt.rect(g, 0, top, 261, top + 48, 0xFF19202A);
			if (selected) HallArt.diamond(g, 22, top + 18, 12, 0xFFFFD54F);
			HallArt.text(g, label.getString(), 48, top + 11, 26, 26, selected ? 0xFFD54F : hovered ? 0xE6D3A3 : 0xB9B2A0, false);
			if (unread) HallArt.rect(g, 236, top + 15, 245, top + 24, 0xFFFFD54F);
		} finally {
			HallArt.end(g);
		}
	}

	/** The Dao Codex at the foot of the Hall's column of tabs (CodexScreen comes back here), wearing its gold dot while a page is unread. */
	private final class CodexLink extends AbstractButton {
		CodexLink(int x, int y, int width, int height) {
			super(x, y, width, height, Component.translatable(ModLang.HALL_CODEX));
		}

		@Override
		public void onPress() {
			if (minecraft != null) minecraft.setScreen(new CodexScreen(CultivationScreen.this));
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			navRow(g, Tab.values().length, getMessage(), false, isHoveredOrFocused(), CodexState.anyUnread());
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
