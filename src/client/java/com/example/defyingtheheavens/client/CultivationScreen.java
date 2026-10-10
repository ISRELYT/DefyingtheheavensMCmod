package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.AlchemyRecipes;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.PillItem;
import com.example.defyingtheheavens.CultivationBoost;
import com.example.defyingtheheavens.CultivationStats;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.MortalStage;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.RealmSuppressSystem;
import com.example.defyingtheheavens.RingOfPowerItem;
import com.example.defyingtheheavens.Stage;
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
import java.util.function.ToDoubleBiFunction;

/**
 * The cultivation menu: one panel with five tabs, left to right Cultivation (progress), Stats, Abilities, Methods and
 * Spells. The previous single-page layout is kept in backups/cultivation-menu-2026-10-09.
 */
public class CultivationScreen extends Screen {
	private static final int WIDTH = 260;
	private static final int HEIGHT = 230; // fits the smallest usual GUI height (240)
	private static final int TAB_Y = 6;
	private static final int TAB_HEIGHT = 19;
	private static final int TAB_GAP = 2;
	/** Least space between a tab's label and its edges; tabs are as wide as their labels plus a share of what's left. */
	private static final int TAB_PADDING = 8;
	private static final int CONTENT_Y = 34;
	private static final int SWITCH_W = 30;
	private static final int SWITCH_H = 14;
	/** Pixels the Abilities tab moves per mouse-wheel notch. */
	private static final int SCROLL_STEP = 14;
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
		SPELLS(ModLang.TAB_SPELLS);

		final String key;

		Tab(String key) {
			this.key = key;
		}
	}

	/** Reopening the menu returns to the tab it was closed on. */
	private static Tab tab = Tab.CULTIVATION;

	private int left;
	private int top;
	private Button meditateButton;
	private Button breakthroughButton;
	/** Where each ability's switch was drawn this frame, for {@link #mouseClicked}. Empty on other tabs. */
	private final List<AbilitySwitch> switches = new ArrayList<>();
	/** How far the Abilities tab is scrolled, in pixels, and how far it can go (0 when everything fits). */
	private int abilityScroll;
	private int abilityMaxScroll;

	private record AbilitySwitch(Ability ability, int x, int y) {
		boolean contains(double mouseX, double mouseY) {
			return mouseX >= x && mouseX < x + SWITCH_W && mouseY >= y && mouseY < y + SWITCH_H;
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

		updateButtons();
	}

	@Override
	public void tick() {
		updateButtons();
	}

	/** Meditate and Breakthrough belong to the Cultivation tab. */
	private void updateButtons() {
		if (meditateButton == null) return;
		boolean progressTab = tab == Tab.CULTIVATION;
		meditateButton.visible = progressTab;
		breakthroughButton.visible = progressTab;
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
		switch (tab) {
			case CULTIVATION -> renderCultivation(g, player);
			case STATS -> {
				if (ClientCultivationData.get().isMortal()) renderPlaceholder(g, ModLang.MORTAL, ModLang.MORTAL_STATS, ModLang.MORTAL_DETAIL);
				else renderStats(g, player);
			}
			case ABILITIES -> renderAbilities(g, mouseX, mouseY);
			case METHODS -> renderPlaceholder(g, ModLang.METHODS_TITLE, ModLang.METHODS_EMPTY, ModLang.METHODS_HINT);
			case SPELLS -> renderPlaceholder(g, ModLang.SPELLS_TITLE, ModLang.SPELLS_EMPTY, ModLang.SPELLS_HINT);
		}

		super.render(g, mouseX, mouseY, partialTick);
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
		boolean bottleneck = c.isAtBottleneck();
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
		// The lower realm's cap, or another cultivator's Realm Suppress: either way the stats now fall short of the true ones.
		boolean suppressed = c.isSuppressed() || c.isUnderPressure();
		Realm r = c.getEffectiveRealm(); // bonuses follow the (possibly suppressed) effective stage
		Stage s = c.getEffectiveStage();
		int cx = left + WIDTH / 2;
		int x = left + 14;
		int y = top + CONTENT_Y;

		if (suppressed) {
			// True cultivation, then what the lower realm or the pressure holds it to.
			g.drawCenteredString(font, PlayerCultivation.rankName(c.getRealm(), c.getStage()), cx, y, 0xB39DDB);
			y += 11;
			g.drawCenteredString(font, Component.translatable(c.isUnderPressure() ? ModLang.STATS_PRESSED : ModLang.STATS_SUPPRESSED),
					cx, y, 0xFF6E6E);
			y += 11;
			Component held = PlayerCultivation.rankName(r, s);
			if (c.isUnderPressure()) held = held.copy().append(" (-" + Math.round(c.getPressurePenalty() * 100) + "%)");
			g.drawCenteredString(font, held, cx, y, 0xFF6E6E);
		} else {
			g.drawCenteredString(font, PlayerCultivation.rankName(r, s), cx, y, 0xB39DDB);
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
			statRow(g, player, line, c, suppressed, y);
			y += 11;
		}
		qiRow(g, ModLang.STAT_NAME_MAX_QI, CultivationStats::maxQi, c.maxQi(), false, c, suppressed, y);
		y += 11;
		qiRow(g, ModLang.STAT_NAME_QI_GATHER, CultivationStats::qiGather, c.qiGatherPerSecond(), true, c, suppressed, y);
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
	 * mouse wheel once the entries outgrow the panel, with a thin gold bar at the right edge showing where the view is.
	 */
	private void renderAbilities(GuiGraphics g, int mouseX, int mouseY) {
		PlayerCultivation c = ClientCultivationData.get();
		List<Ability> awakened = Arrays.stream(Ability.values()).filter(ability -> ability.isUnlocked(c)).toList();
		if (awakened.isEmpty()) {
			abilityMaxScroll = 0;
			renderPlaceholder(g, ModLang.ABILITIES_TITLE, ModLang.ABILITIES_EMPTY, ModLang.ABILITIES_HINT);
			return;
		}

		int x = left + 14;
		int rightEdge = left + WIDTH - 14;
		int viewTop = top + CONTENT_Y;
		int viewBottom = top + HEIGHT - 6;
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
			g.drawString(font, ability.getDisplayName(), x, y, on ? 0xFFD700 : 0x9E9E9E);
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

		if (abilityMaxScroll > 0) {
			int trackX = left + WIDTH - 7;
			int trackHeight = viewBottom - viewTop - 4;
			int thumbHeight = Math.max(12, trackHeight * (viewBottom - viewTop) / contentHeight);
			int thumbY = viewTop + 2 + (trackHeight - thumbHeight) * abilityScroll / abilityMaxScroll;
			g.fill(trackX, viewTop + 2, trackX + 2, viewTop + 2 + trackHeight, 0xFF2A2A3A);
			g.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, BORDER);
			g.fill(trackX + 1, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF6E5214); // shaded side, like the Qi bar's trim
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (tab == Tab.ABILITIES && abilityMaxScroll > 0) {
			abilityScroll = Mth.clamp(abilityScroll - (int) Math.round(delta * SCROLL_STEP), 0, abilityMaxScroll);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
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
	 * One stat: the attribute's base value, then either the realm's bonus and the current value, or (while suppressed or
	 * pressed) the value at the true cultivation without the pressure and the value now. The bonus of a percentage stat is
	 * a fraction, shown x100.
	 */
	private void statRow(GuiGraphics g, Player player, StatLine line, PlayerCultivation c, boolean suppressed, int y) {
		Attribute attribute = line.attribute();
		double base = player == null ? 0 : player.getAttributeBaseValue(attribute);
		double bonus = line.bonus().applyAsDouble(c.getEffectiveRealm(), c.getEffectiveStage());
		double now = valueWith(player, attribute, bonus, true);
		g.drawString(font, Component.translatable(line.nameKey()), left + 14, y, 0xE0E0E0);
		right(g, Component.literal(one(base * line.scale()) + line.unit()), left + BASE_RIGHT, y, 0xA0A0A0);
		if (suppressed) {
			double truth = valueWith(player, attribute, line.bonus().applyAsDouble(c.getRealm(), c.getStage()), false);
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
		double bonus = value.applyAsDouble(c.getEffectiveRealm(), c.getEffectiveStage());
		g.drawString(font, Component.translatable(nameKey), left + 14, y, 0xE0E0E0);
		right(g, Component.literal(qiText(0, rate)), left + BASE_RIGHT, y, 0xA0A0A0);
		if (suppressed) {
			double truth = value.applyAsDouble(c.getRealm(), c.getStage());
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

	// --- Methods and Spells: nothing to learn yet ---

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
			updateButtons();
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
