package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.AlchemyRecipes;
import com.example.defyingtheheavens.Alignment;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.PillItem;
import com.example.defyingtheheavens.CultivationBoost;
import com.example.defyingtheheavens.CultivationStats;
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
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.RealmSuppressSystem;
import com.example.defyingtheheavens.RingOfPowerItem;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.TwoPaths;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleBiFunction;

/**
 * The cultivation menu: one panel with six tabs, left to right Cultivation (progress), Stats, Abilities, Methods, Spells and
 * Journal (the rumours cultivators have told you, see Journal). The previous single-page layout is kept in
 * backups/cultivation-menu-2026-10-09.
 */
public class CultivationScreen extends Screen {
	private static final int WIDTH = 290;
	private static final int HEIGHT = 230; // fits the smallest usual GUI height (240)
	private static final int TAB_Y = 6;
	private static final int TAB_HEIGHT = 19;
	private static final int TAB_GAP = 2;
	/** Least space between a tab's label and its edges; tabs are as wide as their labels plus a share of what's left. */
	private static final int TAB_PADDING = 8;
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
	private static final int PANEL = 0xF0101018;
	private static final int TAB_IDLE = 0xFF1A1A26;
	private static final int TAB_HOVER = 0xFF262638;
	/** Qi gathering boosted by the Upper Realm, in the Qi bar's blue. */
	private static final int QI_UPPER_COLOR = 0x64B5F6;

	private enum Tab {
		CULTIVATION(ModLang.TAB_CULTIVATION),
		STATS(ModLang.TAB_STATS),
		ABILITIES(ModLang.TAB_ABILITIES),
		METHODS(ModLang.TAB_METHODS),
		SPELLS(ModLang.TAB_SPELLS),
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
	private Button meditateButton;
	private Button breakthroughButton;
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
	/** The Methods tab's Practise buttons and the Spells tab's Ready buttons, as drawn this frame, for {@link #mouseClicked}. */
	private final List<RowButton<GhostMethod>> practiseButtons = new ArrayList<>();
	private final List<RowButton<Spell>> readyButtons = new ArrayList<>();
	/** The Methods tab's heaven page's Practise buttons, and the two pages' own buttons (true: the heaven page). */
	private final List<RowButton<HeavenMethod>> heavenPractiseButtons = new ArrayList<>();
	private final List<RowButton<Boolean>> methodPageButtons = new ArrayList<>();
	/** The Methods tab shows the heaven path's methods rather than the ghost path's (kept on reopening). */
	private static boolean heavenPage;
	/** The spell wheel editor's palette as drawn this frame, the spell being dragged in it, and the slot it came from (-1: the palette). */
	private final List<SpellCell> spellPalette = new ArrayList<>();
	private Spell draggedSpell;
	private int spellDragFrom = -1;

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
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;

		// Five labels of very different lengths don't fit equal widths, so each tab gets its label's width plus an even share
		// of the strip that's left.
		Tab[] tabs = Tab.values();
		int[] widths = new int[tabs.length];
		int spare = WIDTH - 12 - TAB_GAP * (tabs.length - 1);
		for (int i = 0; i < tabs.length; i++) {
			widths[i] = font.width(Component.translatable(tabs[i].key)) + TAB_PADDING;
			spare -= widths[i];
		}
		int tabX = left + 6;
		for (int i = 0; i < tabs.length; i++) {
			// floorDiv/floorMod so a long translation (negative spare) still shares out exactly.
			int w = widths[i] + Math.floorDiv(spare, tabs.length) + (i < Math.floorMod(spare, tabs.length) ? 1 : 0);
			addRenderableWidget(new TabButton(tabX, top + TAB_Y, w, TAB_HEIGHT, tabs[i]));
			tabX += w + TAB_GAP;
		}

		meditateButton = addRenderableWidget(Button.builder(Component.empty(),
						b -> ClientPlayNetworking.send(ModPackets.TOGGLE_MEDITATION, PacketByteBufs.create()))
				.bounds(left + 12, top + HEIGHT - 32, 112, 20).build());

		breakthroughButton = addRenderableWidget(Button.builder(Component.translatable(ModLang.BTN_BREAKTHROUGH),
						b -> ClientPlayNetworking.send(ModPackets.BREAKTHROUGH, PacketByteBufs.create()))
				.bounds(left + WIDTH - 124, top + HEIGHT - 32, 112, 20).build());

		wheelButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
					arranging = !arranging;
					dragged = null;
					updateButtons();
				})
				.bounds(left + (WIDTH - 112) / 2, top + HEIGHT - 32, 112, 20).build());

		spellWheelButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
					arrangingSpells = !arrangingSpells;
					draggedSpell = null;
					updateButtons();
				})
				.bounds(left + (WIDTH - 112) / 2, top + HEIGHT - 32, 112, 20).build());

		updateButtons();
		if (tab == Tab.JOURNAL) ClientJournal.request();
	}

	@Override
	public void tick() {
		updateButtons();
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
		meditateButton.setMessage(Component.translatable(
				ClientCultivationData.isMeditating() ? ModLang.BTN_STOP : ModLang.BTN_MEDITATE));
		breakthroughButton.active = ClientCultivationData.get().canBreakthrough() && !ClientTribulationData.isActive();
		meditateButton.active = !ClientCultivationData.get().isMortal(); // a mortal has no qi to gather
	}

	@Override
	public boolean isPauseScreen() {
		return false; // meditation keeps running while the menu is open
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ModKeybinds.OPEN_MENU.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);

		g.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, BORDER);
		g.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
		// The line under the tab strip; the selected tab is drawn over it so it opens into the panel.
		g.fill(left, top + TAB_Y + TAB_HEIGHT - 1, left + WIDTH, top + TAB_Y + TAB_HEIGHT, BORDER);

		Player player = minecraft == null ? null : minecraft.player;
		switches.clear();
		palette.clear();
		practiseButtons.clear();
		readyButtons.clear();
		heavenPractiseButtons.clear();
		methodPageButtons.clear();
		spellPalette.clear();
		bar = null;
		switch (tab) {
			case CULTIVATION -> renderCultivation(g, player);
			case STATS -> {
				// A mortal on neither path has only a tempered body; a mortal's ghost on the ghost path, that path's strength.
				if (ClientCultivationData.get().isMortal() && !TwoPaths.body(ClientCultivationData.get(), false).ghostPath()) {
					renderPlaceholder(g, ModLang.MORTAL, ModLang.MORTAL_STATS, ModLang.MORTAL_DETAIL);
				} else {
					renderStats(g, player);
				}
			}
			case ABILITIES -> renderAbilities(g, mouseX, mouseY);
			case METHODS -> renderMethods(g, mouseX, mouseY);
			case SPELLS -> renderSpells(g, mouseX, mouseY);
			case JOURNAL -> renderJournal(g, mouseX, mouseY);
		}

		super.render(g, mouseX, mouseY, partialTick);
		// The ability (or spell) being dragged in a wheel editor rides on the pointer, over everything.
		if (dragged != null) AbilityWheel.drawIcon(g, dragged, mouseX - 8, mouseY - 8, 16, true);
		if (draggedSpell != null) SpellWheelScreen.drawIcon(g, draggedSpell, mouseX - 8, mouseY - 8, 16, true);
	}

	// --- Cultivation: realm, stage, cultivation progress and what comes next ---

	private void renderCultivation(GuiGraphics g, Player player) {
		PlayerCultivation c = ClientCultivationData.get();
		int cx = left + WIDTH / 2;
		int x = left + 14;
		int y = top + CONTENT_Y;

		if (c.isMortal()) {
			renderMortal(g, cx, y);
			return;
		}

		g.drawCenteredString(font, Component.translatable(ModLang.REALM, c.getRealm().getDisplayName()), cx, y, 0x7FDBFF);
		y += 12;
		g.drawCenteredString(font, Component.translatable(ModLang.STAGE, c.getStage().getDisplayName()), cx, y, 0xB39DDB);
		y += 18;

		// Cultivation bar
		int barW = WIDTH - 28;
		double required = c.cultivationRequired();
		double ratio = required <= 0 ? 0 : Math.min(1.0, c.getCultivation() / required);
		boolean bottleneck = c.isStalled() || c.isHeldByMethod();
		g.fill(x - 1, y - 1, x + barW + 1, y + 11, 0xFF000000);
		g.fill(x, y, x + barW, y + 10, 0xFF2A2A3A);
		g.fill(x, y, x + (int) (barW * ratio), y + 10, bottleneck ? 0xFFFFC107 : 0xFF4DD0E1);
		// Unrefined qi from pills and fruit, waiting to be refined: a pale stretch beyond the bar's fill.
		if (c.getMedicinalQi() > 0 && required > 0 && !bottleneck) {
			double pending = Math.min(1.0, (c.getCultivation() + c.getMedicinalQi()) / required);
			g.fill(x + (int) (barW * ratio), y + 3, x + (int) (barW * pending), y + 7, 0x90B2EBF2);
		}
		y += 14;

		Component progressLine = c.isMaxed()
				? Component.translatable(ModLang.CULTIVATION_MAX, num(c.getCultivation()))
				: Component.translatable(ModLang.CULTIVATION, num(c.getCultivation()), num(required));
		g.drawCenteredString(font, progressLine, cx, y, 0xFFFFFF);
		y += 11;
		// What meditating here gathers, surroundings included (mats, fruit on pedestals, height, tranquillity); jade when boosted.
		double boost = player == null ? 1.0 : CultivationBoost.of(player).multiplier();
		double rate = c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(player)) * boost; // 0 while suppressed
		g.drawCenteredString(font, c.isInUpperRealm()
				? Component.translatable(ModLang.CULTIVATION_RATE_UPPER, one(rate), (int) PlayerCultivation.UPPER_REALM_QI_MULTIPLIER)
				: Component.translatable(ModLang.CULTIVATION_RATE, one(rate)), cx, y, boost > 1.0 && rate > 0 ? 0x7FE0A0 : 0xA0A0A0);
		y += 11;
		y = renderNetherworldLines(g, cx, y);
		// Pills at work: a Qi Gathering Pill's boost, and how much less the next Cultivation Pill will do.
		if (c.getQiBoost() > 0) {
			g.drawCenteredString(font, Component.translatable(ModLang.QI_BOOST_LINE, Math.round(c.getQiBoost() * 100)), cx, y, 0x64B5F6);
			y += 11;
		}
		if (c.getMedicinalQi() >= 0.5) {
			g.drawCenteredString(font, Component.translatable(ModLang.UNREFINED_LINE, num(c.getMedicinalQi())), cx, y, 0xB2EBF2);
			y += 11;
		}
		if (c.getPillResistance() > 0.005) {
			g.drawCenteredString(font, Component.translatable(ModLang.PILL_RESISTANCE_LINE, Math.round(c.getPillResistance() * 100)),
					cx, y, 0x9E9E9E);
			y += 11;
		}
		// The cultivator's path (see Alignment), in its colour.
		g.drawCenteredString(font, Component.translatable(ModLang.ALIGNMENT_LINE, Alignment.describe(c.getAlignment())), cx, y, 0xA0A0A0);
		y += 11;
		// The ghost path beside this one (TwoPaths), and how much the two in harmony add to the body.
		if (TwoPaths.walksBoth(c)) {
			g.drawCenteredString(font, Component.translatable(ModLang.TWO_PATHS_LINE, GhostMethods.rankName(TwoPaths.ghostRank(c, false)),
					Math.round(TwoPaths.harmony(c) * 100)), cx, y, 0x8FEAE4);
			y += 11;
		}
		// Heaven's Favour from a generous offering at the Ritual to the Heavens.
		if (c.getFavour() > 0) {
			g.drawCenteredString(font, Component.translatable(ModLang.FAVOUR_LINE, Math.round((c.favourFactor() - 1) * 100)), cx, y, 0xFFE082);
			y += 11;
		}
		y += 7;

		if (c.isUnderPressure()) {
			// Another cultivator's Realm Suppress: what it holds the player to (if a realm), and what it takes.
			g.drawCenteredString(font, Component.translatable(ModLang.PRESSURE), cx, y, 0xFF6E6E);
			if (c.getPressureStages() > 0) {
				y += 11;
				g.drawCenteredString(font, PlayerCultivation.rankName(c.getEffectiveRealm(), c.getEffectiveStage()), cx, y, 0xFF6E6E);
			}
			g.drawCenteredString(font, Component.translatable(ModLang.PRESSURE_DETAIL, Math.round(c.getPressurePenalty() * 100)),
					cx, y + 11, 0xFF6E6E);
		} else if (c.isSuppressed()) {
			g.drawCenteredString(font, Component.translatable(ModLang.SUPPRESSED), cx, y, 0xFF6E6E);
			g.drawCenteredString(font, Component.translatable(ModLang.SUPPRESSED_TO,
					PlayerCultivation.rankName(c.getEffectiveRealm(), c.getEffectiveStage())), cx, y + 11, 0xFF6E6E);
		} else if (c.isMaxed()) {
			g.drawCenteredString(font, Component.translatable(ModLang.PINNACLE), cx, y, 0xFFD700);
		} else if (c.isAtBottleneck() && c.isBreakthroughLocked()) {
			g.drawCenteredString(font, Component.translatable(ModLang.BREAKTHROUGH_SEALED), cx, y, 0xFF6E6E);
		} else if (c.isAtBottleneck() && c.isMissingBreakthroughPill()) {
			// The realm ahead needs a pill: name it and how to brew it.
			var pill = PillItem.breakthroughPillFor(c.breakthroughRealm());
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.NEED_PILL, pill.getDescription(),
					c.breakthroughRealm().getDisplayName()), WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, 0xFFC107);
				y += 11;
			}
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.PILL_RECIPE, AlchemyRecipes.describe(pill)),
					WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, 0xA0A0A0);
				y += 11;
			}
		} else if (c.needsHeavensLeave()) {
			// Qi Refining Mid: Late takes the heavens' leave (the Ritual to the Heavens at a Heaven Altar).
			boolean full = c.isAtRitualGate();
			for (FormattedCharSequence line : font.split(Component.translatable(full ? ModLang.RITUAL_GATE : ModLang.RITUAL_AHEAD), WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, full ? 0xFFC107 : 0xA0A0A0);
				y += 11;
			}
			if (full) {
				for (FormattedCharSequence line : font.split(Component.translatable(ModLang.RITUAL_HOW), WIDTH - 28)) {
					g.drawCenteredString(font, line, cx, y, 0xA0A0A0);
					y += 11;
				}
			}
		} else if (!c.netherworld().ghost() && !HeavenMethods.carries(c, PlayerCultivation.rank(c.getRealm(), c.getStage()) + 1)) {
			// The next stage waits on a heaven method (HeavenMethods#reach): said ahead in grey, in amber once the bar is full.
			boolean full = c.isHeldByMethod();
			for (FormattedCharSequence line : font.split(HeavenMethods.gateLine(c), WIDTH - 28)) {
				g.drawCenteredString(font, line, cx, y, full ? 0xFFC107 : 0xA0A0A0);
				y += 11;
			}
		} else if (c.canBreakthrough()) {
			g.drawCenteredString(font, Component.translatable(ModLang.BOTTLENECK), cx, y, 0xFFC107);
			g.drawCenteredString(font, Component.translatable(ModLang.BOTTLENECK_NEXT,
					PlayerCultivation.rankName(c.breakthroughRealm(), c.breakthroughStage())), cx, y + 11, 0xFFC107);
		} else if (c.getPreparedRealm() != null) {
			// A breakthrough pill already taken, waiting for the bar to fill.
			g.drawCenteredString(font, Component.translatable(ModLang.PILL_PREPARED, c.getPreparedRealm().getDisplayName()), cx, y, 0x7FE0A0);
		}

		boolean meditating = ClientCultivationData.isMeditating();
		g.drawCenteredString(font, Component.translatable(meditating ? ModLang.STATUS_MEDITATING : ModLang.STATUS_IDLE),
				cx, top + HEIGHT - 46, meditating ? 0x69F0AE : 0x9E9E9E);
	}

	/** A mortal's Cultivation tab: the body's tempering stage and progress, then how to open the meridians. */
	private void renderMortal(GuiGraphics g, int cx, int y) {
		PlayerCultivation c = ClientCultivationData.get();
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
		renderNetherworldLines(g, cx, y + 4);
	}

	/**
	 * The ghostly qi where the player stands (only where there is any: the Nether and the Netherworld), and their yin corruption
	 * while they carry any, with whether it is ebbing or held still. Returns the y below what it drew.
	 */
	private int renderNetherworldLines(GuiGraphics g, int cx, int y) {
		double density = ClientNetherworldData.density();
		if (density > 0) {
			String word = GhostQi.word(density);
			Component amount = Component.translatable(switch (word) {
				case "dense" -> density > GhostQi.NETHERWORLD ? ModLang.GHOST_QI_PEAKS : ModLang.GHOST_QI_DENSE;
				case "thin" -> ModLang.GHOST_QI_THIN;
				default -> ModLang.GHOST_QI_FAINT;
			}, density == Math.rint(density) ? String.valueOf((long) density) : String.valueOf(density));
			g.drawCenteredString(font, Component.translatable(ModLang.GHOST_QI_HERE, amount), cx, y, 0x8FEAE4);
			y += 11;
		}
		float corruption = ClientNetherworldData.corruption();
		if (corruption > 0) {
			long percent = Math.round(corruption * 100);
			Component line = ClientNetherworldData.heldSeconds() > 0
					? Component.translatable(ModLang.YIN_HELD, percent, ClientNetherworldData.heldSeconds())
					: ClientNetherworldData.trend() < 0 ? Component.translatable(ModLang.YIN_EBBING, percent)
					: Component.translatable(ModLang.YIN_LINE, percent);
			g.drawCenteredString(font, line, cx, y, corruption >= 0.75f ? 0x4FC3D9 : 0x7FB8C8);
			y += 11;
		}
		return y;
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
		int y = top + CONTENT_Y;
		// The ghost path's strength is named so; and the two paths' harmony, if they are both walked near enough.
		double harmony = TwoPaths.harmony(c);
		java.util.function.Function<Component, Component> named = rank -> {
			Component line = body.ghostPath() ? Component.translatable(ModLang.STATS_GHOST_BODY, rank) : rank;
			return harmony > 0 ? line.copy().append(Component.translatable(ModLang.STATS_HARMONY, Math.round(harmony * 100))) : line;
		};

		if (suppressed) {
			// True cultivation, then what the lower realm or the pressure holds it to.
			g.drawCenteredString(font, named.apply(PlayerCultivation.rankName(truth.realm(), truth.stage())), cx, y, 0xB39DDB);
			y += 11;
			g.drawCenteredString(font, Component.translatable(c.isUnderPressure() ? ModLang.STATS_PRESSED : ModLang.STATS_SUPPRESSED),
					cx, y, 0xFF6E6E);
			y += 11;
			Component held = PlayerCultivation.rankName(r, s);
			if (c.isUnderPressure()) held = held.copy().append(" (-" + Math.round(c.getPressurePenalty() * 100) + "%)");
			g.drawCenteredString(font, held, cx, y, 0xFF6E6E);
		} else {
			g.drawCenteredString(font, named.apply(PlayerCultivation.rankName(r, s)), cx, y, 0xB39DDB);
		}
		y += 16;

		int currentX = left + WIDTH - 14;
		g.drawString(font, Component.translatable(ModLang.STATS_COL_STAT), x, y, 0xFFD700);
		right(g, Component.translatable(ModLang.STATS_COL_BASE), left + BASE_RIGHT, y, 0xFFD700);
		right(g, Component.translatable(suppressed ? ModLang.STATS_COL_TRUE : ModLang.STATS_COL_BONUS), left + BONUS_RIGHT, y, 0xFFD700);
		right(g, Component.translatable(suppressed ? ModLang.STATS_COL_NOW : ModLang.STATS_COL_CURRENT), currentX, y, 0xFFD700);
		y += 11;
		g.fill(x, y, currentX, y + 1, 0xFF3A3A4A);
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
		}
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
				g.fill(x, y, rightEdge, y + 1, 0xFF3A3A4A);
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
		int x = left + SCROLLBAR_X;
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
			case JOURNAL -> journalScroll = scroll;
			case METHODS -> methodScroll = scroll;
			case SPELLS -> spellScroll = scroll;
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
				g.fill(x, y, rightEdge, y + 1, 0xFF3A3A4A);
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
		if (tab == Tab.SPELLS && !arrangingSpells && spellMaxScroll > 0) {
			spellScroll = Mth.clamp(spellScroll - (int) Math.round(delta * SCROLL_STEP), 0, spellMaxScroll);
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

	/** An On/Off switch in the panel's colours: green "On", grey "Off". */
	private void drawSwitch(GuiGraphics g, AbilitySwitch toggle, boolean on, boolean hovered) {
		int x = toggle.x();
		int y = toggle.y();
		g.fill(x, y, x + SWITCH_W, y + SWITCH_H, BORDER);
		g.fill(x + 1, y + 1, x + SWITCH_W - 1, y + SWITCH_H - 1, hovered ? TAB_HOVER : TAB_IDLE);
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
			for (RowButton<Boolean> page : methodPageButtons) {
				if (!page.contains(mouseX, mouseY)) continue;
				if (heavenPage != page.value()) {
					heavenPage = page.value();
					methodScroll = 0;
				}
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
		double bonus = line.bonus().applyAsDouble(body.realm(), body.stage()) * body.scale();
		double now = valueWith(player, attribute, bonus, true);
		g.drawString(font, Component.translatable(line.nameKey()), left + 14, y, 0xE0E0E0);
		right(g, Component.literal(one(base * line.scale()) + line.unit()), left + BASE_RIGHT, y, 0xA0A0A0);
		if (suppressed) {
			double truth = valueWith(player, attribute, line.bonus().applyAsDouble(trueBody.realm(), trueBody.stage()) * trueBody.scale(), false);
			right(g, Component.literal(one(truth * line.scale()) + line.unit()), left + BONUS_RIGHT, y, 0xB39DDB);
			right(g, Component.literal(one(now * line.scale()) + line.unit()), left + WIDTH - 14, y, now < truth ? 0xFF8A80 : 0xFFFFFF);
		} else {
			double bonusShown = line.unit().isEmpty() ? bonus : bonus * 100;
			right(g, Component.literal("+" + one(bonusShown) + line.unit()), left + BONUS_RIGHT, y, bonus > 0 ? 0x69F0AE : 0x9E9E9E);
			right(g, Component.literal(one(now * line.scale()) + line.unit()), left + WIDTH - 14, y, 0xFFFFFF);
		}
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

	/** Thousands separated, like the HUD: "8,000" and "2,000.0/s". */
	private static String qiText(double v, boolean rate) {
		return rate ? String.format(Locale.ROOT, "%,.1f/s", v) : num(v);
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
		// The two pages, side by side at the top: the ghost path's methods and the heaven path's.
		int y = top + CONTENT_Y;
		int x = left + 14;
		for (boolean heaven : new boolean[]{false, true}) {
			Component label = Component.translatable(heaven ? ModLang.METHODS_PAGE_HEAVEN : ModLang.METHODS_PAGE_GHOST);
			int w = font.width(label) + 12;
			RowButton<Boolean> button = new RowButton<>(heaven, x, y, w);
			methodPageButtons.add(button);
			boolean chosen = heavenPage == heaven;
			drawRowButton(g, button, label, button.contains(mouseX, mouseY), chosen ? (heaven ? 0xFFD700 : 0x8FEAE4) : 0x9E9E9E);
			if (chosen) g.fill(button.x() + 2, button.y() + SWITCH_H, button.x() + w - 2, button.y() + SWITCH_H + 1, heaven ? 0xFFFFD700 : 0xFF8FEAE4);
			x += w + 4;
		}
		int viewTop = top + CONTENT_Y + METHOD_PAGE_ROW;
		if (heavenPage) renderHeavenMethods(g, mouseX, mouseY, viewTop);
		else renderGhostMethods(g, mouseX, mouseY, viewTop);
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
		// A mortal's methods lie dormant (GhostMethods#refusal): no pace, no next floor, only the sealed path below.
		boolean sealed = c.isMortal();
		if (active != null && !sealed) {
			Component rate;
			if (density <= 0) {
				rate = Component.translatable(ModLang.METHODS_NO_QI);
				rateColor = 0x9E9E9E;
			} else if (GhostMethods.heldByRealm(c, active)) {
				int cap = GhostMethods.layerCap(c, active);
				rate = Component.translatable(ModLang.METHODS_REALM_CAPPED, cap, GhostMethods.realmToPass(active, cap).getDisplayName());
				rateColor = 0xE0A040;
			} else if (s.layer(active) >= active.layers()) {
				rate = Component.translatable(ModLang.METHODS_PERFECTED);
			} else {
				double perSecond = GhostMethods.ratePerSecond(c, density);
				double minutes = (GhostMethods.layerCost(active, s.layer(active)) - s.progress(active)) / Math.max(1.0e-6, perSecond) / 60.0;
				rate = Component.translatable(ModLang.METHODS_RATE, active.getDisplayName(), minutes < 1 ? "<1" : String.valueOf(Math.round(minutes)));
			}
			rateLines = font.split(rate, width);
		}
		// The deepest foundation's next layer that carries the ghost path on (GhostMethods#floor); or, perfected, the foundation that
		// carries it further, to be sought out.
		List<FormattedCharSequence> floorLines = new ArrayList<>();
		GhostMethod foundation = GhostMethods.deepestFoundation(s);
		if (foundation != null && !sealed) {
			int layer = s.layer(foundation);
			int reached = GhostMethods.floor(foundation, layer);
			Component floorLine = null;
			for (int next = layer + 1; next <= foundation.layers() && floorLine == null; next++) {
				int floor = GhostMethods.floor(foundation, next);
				if (floor > reached) floorLine = Component.translatable(ModLang.METHODS_FLOOR_NEXT, next, foundation.getDisplayName(), GhostMethods.rankName(floor));
			}
			if (floorLine == null && layer >= foundation.layers() && foundation.next() != null) {
				floorLine = Component.translatable(ModLang.METHODS_SEEK_NEXT, foundation.getDisplayName(), foundation.next().getDisplayName());
			}
			if (floorLine != null) floorLines = font.split(floorLine, width);
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
					drawRealmPips(g, x, y, layer, method.layers());
					y += 11;
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER_REALM, layer, method.layers(), MethodLayers.realmOf(layer).getDisplayName(),
							MethodLayers.within(layer)), x, y, 0xB0B0B0);
					y += 10;
				} else {
					drawPips(g, x, y, layer);
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER, layer, method.layers()), x + 9 * 9 + 6, y, 0xB0B0B0);
					y += 11;
				}
				boolean perfected = layer >= method.layers();
				double fraction = perfected ? 1 : Mth.clamp(s.progress(method) / GhostMethods.layerCost(method, layer), 0, 1);
				g.fill(x, y, rightEdge, y + 5, 0xFF6E5214);
				g.fill(x + 1, y + 1, rightEdge - 1, y + 4, 0xFF0C0C14);
				g.fill(x + 1, y + 1, x + 1 + (int) Math.round((rightEdge - x - 2) * fraction), y + 4, perfected ? 0xFFFFD700 : 0xFF4FC3D9);
				y += 9;
				Component progress = perfected ? Component.translatable(ModLang.METHODS_PERFECTED)
						: Component.translatable(ModLang.METHODS_PROGRESS, (int) Math.floor(fraction * 100));
				g.drawString(font, progress, x, y, perfected ? 0xFFD700 : 0x8FEAE4);
				y += 11;
				for (FormattedCharSequence line : gains.get(i)) {
					g.drawString(font, line, x, y, 0x8A8A8A);
					y += 10;
				}
			}
			if (i < methods.length - 1) {
				y += 4;
				g.fill(x, y, rightEdge, y + 1, 0xFF3A3A4A);
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
		HeavenMethod active = h.activeMethod();
		if (dead) {
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_DEAD), 0x9E9E9E);
		} else if (c.isMortal()) {
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_SEALED), 0x9E9E9E);
		} else {
			// How far the methods carry the realm (HeavenMethods#reach): cultivation waits there for them.
			addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_REACH, GhostMethods.rankName(HeavenMethods.reach(c))), 0xC8B88A);
		}
		if (!dead && !c.isMortal() && active != null) {
			int layer = h.layer(active);
			if (layer >= active.layers()) {
				addTop.accept(Component.translatable(ModLang.METHODS_PERFECTED), 0xFFD700);
			} else {
				// What meditating here gathers (as the Cultivation tab reckons it) times what the method takes in of it.
				double share = shareHere(active.element());
				Player player = minecraft == null ? null : minecraft.player;
				double boost = player == null ? 1.0 : CultivationBoost.of(player).multiplier();
				double perSecond = c.meditationCultivationPerSecond(RingOfPowerItem.cultivationBonus(player)) * boost
						* HeavenMethods.methodFactor(c, share, false);
				if (perSecond > 0) {
					double minutes = (HeavenMethods.layerCost(layer) - h.progress(active)) / perSecond / 60.0;
					addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_RATE, active.getDisplayName(), minutes < 1 ? "<1" : String.valueOf(Math.round(minutes))), 0xF0D78C);
				}
				addTop.accept(Component.translatable(ModLang.METHODS_HEAVEN_SHARE, active.element().getDisplayName(), Math.round(share * 100),
						String.format(Locale.ROOT, "%.2f", HeavenMethods.elementFactor(share))), 0xB0A070);
			}
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
					drawRealmPips(g, x, y, layer, method.layers());
					y += 11;
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER_REALM, layer, method.layers(), MethodLayers.realmOf(layer).getDisplayName(),
							MethodLayers.within(layer)), x, y, 0xB0B0B0);
					y += 10;
				} else {
					drawPips(g, x, y, layer);
					g.drawString(font, Component.translatable(ModLang.METHODS_LAYER, layer, method.layers()), x + 9 * 9 + 6, y, 0xB0B0B0);
					y += 11;
				}
				boolean perfected = layer >= method.layers();
				double fraction = perfected ? 1 : Mth.clamp(h.progress(method) / HeavenMethods.layerCost(layer), 0, 1);
				g.fill(x, y, rightEdge, y + 5, 0xFF6E5214);
				g.fill(x + 1, y + 1, rightEdge - 1, y + 4, 0xFF0C0C14);
				g.fill(x + 1, y + 1, x + 1 + (int) Math.round((rightEdge - x - 2) * fraction), y + 4, perfected ? 0xFFFFD700 : 0xFF000000 | element.getGlowColor());
				y += 9;
				Component progress = perfected ? Component.translatable(ModLang.METHODS_PERFECTED)
						: Component.translatable(ModLang.METHODS_PROGRESS, (int) Math.floor(fraction * 100));
				g.drawString(font, progress, x, y, perfected ? 0xFFD700 : 0xF0D78C);
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

	/** "4 of the 6 Wood methods are unknown...": how many of {@code element}'s are still to be found. */
	private static Component unknownLine(QiElement element, int unknown) {
		return Component.translatable(ModLang.METHODS_HEAVEN_UNKNOWN, unknown, Realm.values().length, element.getDisplayName());
	}

	/** A passive's amount as the Methods tab shows it: whole where it is whole, else to two places. */
	private static String passiveAmount(double amount) {
		return Math.abs(amount - Math.rint(amount)) < 1.0e-6 ? String.valueOf((long) Math.rint(amount)) : String.format(Locale.ROOT, "%.2f", amount);
	}

	/** What share of the qi where the player stands is {@code element} (QiElement#share), as the server reckons it. */
	private static double shareHere(QiElement element) {
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
	private static void drawRealmPips(GuiGraphics g, int x, int y, int layer, int total) {
		int realmNow = (Math.max(1, layer) - 1) / MethodLayers.PER_REALM;
		for (int i = 0; i < total; i++) {
			int realm = i / MethodLayers.PER_REALM;
			int px = x + i * 4 + realm * 4;
			boolean reached = i < layer;
			g.fill(px, y, px + 3, y + 7, reached ? BORDER : realm == realmNow ? 0xFF4A4A5E : 0xFF2E2E3A);
			g.fill(px + 1, y + 1, px + 2, y + 6, reached ? 0xFFFFD700 : 0xFF14141E);
		}
	}

	/** Nine pips, one a layer: gold to the layer reached, dark beyond it. */
	private static void drawPips(GuiGraphics g, int x, int y, int layer) {
		for (int i = 0; i < GhostMethods.LAYERS; i++) {
			int px = x + i * 9;
			boolean reached = i < layer;
			g.fill(px, y, px + 7, y + 7, reached ? BORDER : 0xFF3A3A4A);
			g.fill(px + 1, y + 1, px + 6, y + 6, reached ? 0xFFFFD700 : 0xFF14141E);
			if (reached) g.fill(px + 1, y + 1, px + 3, y + 3, 0xFFFFF4C0); // a glint
		}
	}

	/** A small framed button on a list row, in the switches' colours. */
	private void drawRowButton(GuiGraphics g, RowButton<?> button, Component label, boolean hovered, int color) {
		g.fill(button.x(), button.y(), button.x() + button.w(), button.y() + SWITCH_H, BORDER);
		g.fill(button.x() + 1, button.y() + 1, button.x() + button.w() - 1, button.y() + SWITCH_H - 1, hovered ? TAB_HOVER : TAB_IDLE);
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
		// A method's spells once the method is known; the spells of both paths (TwoPaths) once both are walked.
		boolean both = TwoPaths.walksBoth(ClientCultivationData.get());
		// A heaven method's spells once it is known, to the living (the dead cast none of them).
		PlayerCultivation self = ClientCultivationData.get();
		List<Spell> shown = Arrays.stream(Spell.values()).filter(spell -> spell.isDual() ? both
				: spell.isHeaven() ? !s.ghost() && self.heaven().knows(spell.heavenMethod()) : s.knows(spell.method())).toList();
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
			}
			y += 15;
			Component cost = spell.isDual() ? Component.translatable(ModLang.SPELLS_COST_BOTH, Math.round(spell.qiCost()), Math.round(spell.cost()))
					: spell.isHeaven() ? Component.translatable(ModLang.SPELLS_QI_COST, Math.round(spell.qiCost()))
					: Component.translatable(ModLang.SPELLS_COST, Math.round(spell.cost()));
			Component teacher = spell.isHeaven() ? spell.heavenMethod().getDisplayName() : spell.isDual() ? Component.empty() : spell.method().getDisplayName();
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
				g.fill(x, y, rightEdge, y + 1, 0xFF3A3A4A);
				y += 8;
			}
		}
		g.disableScissor();
		scrollbar(g, viewTop, viewBottom, contentHeight, spellScroll, spellMaxScroll, mouseX, mouseY);
	}

	/** Ticks as seconds, a decimal only where it isn't whole: "2", "1.5". */
	private static String seconds(int ticks) {
		return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format(Locale.ROOT, "%.1f", ticks / 20.0);
	}

	/**
	 * The spell wheel's editor, as the ability wheel's: the wheel on the left, the spells that can be cast on the right; dragging
	 * one onto a slot puts it there (trading places with the slot's), a slot dragged off the wheel or right-clicked empties.
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
		int perRow = (WIDTH - 12 - PALETTE_X + CELL_GAP) / (CELL + CELL_GAP);
		List<Spell> castable = Arrays.stream(Spell.values()).filter(ClientMethods::known).toList();
		for (int i = 0; i < castable.size(); i++) {
			Spell spell = castable.get(i);
			SpellCell cell = new SpellCell(spell, px + (i % perRow) * (CELL + CELL_GAP), y + (i / perRow) * (CELL + CELL_GAP));
			spellPalette.add(cell);
			boolean hovered = draggedSpell == null && cell.contains(mouseX, mouseY);
			if (hovered) pointed = spell;
			boolean placed = false;
			for (int slot = 0; slot < Spells.WHEEL_SLOTS; slot++) placed |= s.spellSlot(slot) == spell;
			g.fill(cell.x(), cell.y(), cell.x() + CELL, cell.y() + CELL, placed ? AbilityWheel.GOLD : AbilityWheel.SLOT_OFF_RIM);
			g.fill(cell.x() + 1, cell.y() + 1, cell.x() + CELL - 1, cell.y() + CELL - 1, hovered ? AbilityWheel.SLOT_HOVER : AbilityWheel.SLOT);
			SpellWheelScreen.drawIcon(g, spell, cell.x() + (CELL - 16) / 2, cell.y() + (CELL - 16) / 2, 16, true);
		}
		y += ((castable.size() + perRow - 1) / perRow) * (CELL + CELL_GAP) + 4;
		int textWidth = WIDTH - 12 - PALETTE_X;
		Component hint = Component.translatable(ModLang.SPELLS_ARRANGE_HINT,
				ModKeybinds.SPELL_WHEEL.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(0xFFD700)),
				ModKeybinds.CAST_SPELL.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(0xFFD700)));
		for (FormattedCharSequence line : font.split(hint, textWidth)) {
			g.drawString(font, line, px, y, 0x8A8A8A);
			y += 10;
		}

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

	private void renderPlaceholder(GuiGraphics g, String titleKey, String emptyKey, String hintKey) {
		int cx = left + WIDTH / 2;
		int y = top + CONTENT_Y + 30;
		g.drawCenteredString(font, Component.translatable(titleKey), cx, y, 0xFFD700);
		y += 20;
		for (FormattedCharSequence line : font.split(Component.translatable(emptyKey), WIDTH - 28)) {
			g.drawCenteredString(font, line, cx, y, 0xB0B0B0);
			y += 11;
		}
		for (FormattedCharSequence line : font.split(Component.translatable(hintKey), WIDTH - 28)) {
			g.drawCenteredString(font, line, cx, y, 0x808080);
			y += 11;
		}
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

	private static String one(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}

	private static String num(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	/** A tab in the strip at the top of the panel, drawn in the panel's own colours. */
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
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			boolean selected = tab == target;
			int x = getX();
			int y = getY();
			g.fill(x, y, x + width, y + height, BORDER);
			// The selected tab has no bottom edge, so it reads as part of the panel below it.
			g.fill(x + 1, y + 1, x + width - 1, y + height - (selected ? 0 : 1), selected ? PANEL : isHoveredOrFocused() ? TAB_HOVER : TAB_IDLE);
			int color = selected ? 0xFFD700 : isHoveredOrFocused() ? 0xE0E0E0 : 0x9E9E9E;
			g.drawCenteredString(font, getMessage(), x + width / 2, y + (height - 8) / 2, color);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
