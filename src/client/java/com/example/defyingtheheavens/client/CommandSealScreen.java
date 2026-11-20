package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ArtifactGrade;
import com.example.defyingtheheavens.ArtifactKind;
import com.example.defyingtheheavens.Artifacts;
import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.PuppetBlueprint;
import com.example.defyingtheheavens.PuppetCommands;
import com.example.defyingtheheavens.PuppetEntity;
import com.example.defyingtheheavens.PuppetSoul;
import com.example.defyingtheheavens.Puppetry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Command Seal's list (Puppetry; the user's pick, "Seal + list screen"; S2C puppet_list, PuppetCommands): every puppet of its holder's
 * down the left, each with its order and health and where it is (or that it is away on an errand, inert, out of reach); the one chosen on
 * the right in full (its realm, health, place, order, circuits and cuts) with its orders: follow, guard here, hold; a bee's work between the
 * two chests the seal marks, and its message to a player; and taking it apart (pressed twice), beside it. What is done is sent (C2S
 * puppet_command) and the list comes back with what came of it at its foot.
 */
public class CommandSealScreen extends Screen implements HallFrame.Framed {
	private static final int W = 412, H = 236, LIST_X = 8, LIST_W = 176, BODY_TOP = 24, BODY_BOTTOM = H - 20, PANE_X = 192, PANE_W = W - PANE_X - 8,
			ROW_H = 22, BUTTON_H = 14, ROLLER_W = 4;
	/**
	 * The pane's rows of buttons, stacked up from its foot (taking it apart, Sub-Seals) as the chosen puppet has them ({@link #rowKinds}):
	 * its orders; a bee's work and its message; a General's ranks; its soul's (phase 2d: a player held in it, or a hollow one entered or
	 * offered).
	 */
	private static final int FOOT_Y = 206, ROW_STEP = 18;

	private enum RowKind { ORDERS, WORK, MESSAGE, GENERAL, SOUL, FOOT }

	/**
	 * One puppet as the list shows it; its circuits' grade and scores (null if it is not where they can be read). Its ranks of command
	 * (phase 2c): the General whose ranks it is in (empty: none), the grade of the seal it is bound to, whose Sub-Seal it answers (empty:
	 * none), a General's ranks (-1 no General, -2 one can be made of it), its grown mind's bent (-1 none); -1s where it can't be read.
	 */
	record Row(UUID id, String name, PuppetBlueprint blueprint, int grade, int rank, int order, float health, float max, ResourceLocation dimension,
			BlockPos pos, boolean inert, boolean away, String inRanksOf, boolean inReach, @Nullable Component circuits, int[] scores, int cuts,
			int seal, String lentTo, int ranks, int mind, int person, int copied, boolean hollow, int fellows, boolean heldDown, boolean spring) {
		boolean loaded() { return circuits != null; }

		/** A player's soul held in it (phase 2d): -1 none, 0 under control, 1 released. */
		boolean holdsPlayer() { return person >= 0; }
	}

	/** The rows of buttons {@code row}'s puppet has, top down (the foot's last). */
	private static List<RowKind> rowKinds(Row row) {
		List<RowKind> kinds = new ArrayList<>();
		kinds.add(RowKind.ORDERS);
		boolean bee = row.blueprint() == PuppetBlueprint.BEE;
		if (bee) kinds.add(RowKind.WORK);
		if (bee && !row.holdsPlayer()) kinds.add(RowKind.MESSAGE);
		if (!bee && row.ranks() != -1) kinds.add(RowKind.GENERAL);
		if (row.holdsPlayer() || row.loaded() && row.hollow()) kinds.add(RowKind.SOUL);
		kinds.add(RowKind.FOOT);
		return kinds;
	}

	/** The top of {@code row}'s row of {@code kind} on the screen, or -1 if it has none. */
	private int rowY(Row row, RowKind kind) {
		List<RowKind> kinds = rowKinds(row);
		int i = kinds.indexOf(kind);
		return i < 0 ? -1 : top + FOOT_Y - (kinds.size() - 1 - i) * ROW_STEP;
	}

	record Page(int used, int slots, int reach, int rank, boolean marked, int heldSeal, List<Row> rows, @Nullable UUID chosen, @Nullable Component said) {}

	public static Page read(FriendlyByteBuf buf) {
		int used = buf.readVarInt(), slots = buf.readVarInt(), reach = buf.readVarInt(), rank = buf.readVarInt();
		boolean marked = buf.readBoolean();
		int heldSeal = buf.readVarInt();
		List<Row> rows = new ArrayList<>();
		for (int n = buf.readVarInt(); n > 0; n--) {
			UUID id = buf.readUUID();
			String name = buf.readUtf();
			PuppetBlueprint blueprint = PuppetBlueprint.byId(buf.readUtf());
			int grade = buf.readVarInt(), rank2 = buf.readVarInt(), order = buf.readVarInt();
			float health = buf.readFloat(), max = buf.readFloat();
			ResourceLocation dimension = buf.readResourceLocation();
			BlockPos pos = buf.readBlockPos();
			boolean inert = buf.readBoolean(), away = buf.readBoolean();
			String inRanksOf = buf.readUtf();
			boolean inReach = buf.readBoolean();
			Component circuits = null;
			int[] scores = new int[0];
			int cuts = 0, seal = -1, ranks = -1, mind = -1, person = -1, copied = 0, fellows = -1;
			boolean hollow = false, heldDown = false, spring = false;
			String lentTo = "";
			if (buf.readBoolean()) {
				circuits = buf.readComponent();
				scores = new int[] {buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()};
				cuts = buf.readVarInt();
				seal = buf.readVarInt();
				lentTo = buf.readUtf();
				ranks = buf.readVarInt();
				mind = buf.readVarInt();
				person = buf.readVarInt();
				copied = buf.readVarInt();
				hollow = buf.readBoolean();
				fellows = buf.readVarInt();
				heldDown = buf.readBoolean();
				spring = buf.readBoolean();
			}
			rows.add(new Row(id, name, blueprint == null ? PuppetBlueprint.HUMANOID : blueprint, grade, rank2, order, health, max, dimension, pos,
					inert, away, inRanksOf, inReach, circuits, scores, cuts, seal, lentTo, ranks, mind, person, copied, hollow, fellows, heldDown, spring));
		}
		UUID chosen = buf.readBoolean() ? buf.readUUID() : null;
		Component said = buf.readBoolean() ? buf.readComponent() : null;
		return new Page(used, slots, reach, rank, marked, heldSeal, rows, chosen, said);
	}

	public static void show(Page page) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof CommandSealScreen open) open.page(page);
		else mc.setScreen(new CommandSealScreen(page));
	}

	private Page page;
	private @Nullable UUID chosen;
	/** The act pressed once that wants a second press to be done (taking apart, making a General), and how long the first press holds. */
	private int left, top, listScroll, listMax, sureAct = -1, sureTicks;
	private String toDraft = "", messageDraft = "";
	private EditBox toBox, messageBox;

	private CommandSealScreen(Page page) {
		super(Component.translatable(ModLang.LIST_TITLE));
		page(page);
	}

	private void page(Page page) {
		this.page = page;
		if (page.chosen() != null) chosen = page.chosen();
		if (chosen == null || page.rows().stream().noneMatch(r -> r.id().equals(chosen))) chosen = page.rows().isEmpty() ? null : page.rows().get(0).id();
		layoutBoxes();
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		toBox = addRenderableWidget(new EditBox(font, 0, 0, 60, 10, Component.translatable(ModLang.LIST_MESSAGE_TO)));
		toBox.setBordered(false);
		toBox.setMaxLength(16);
		toBox.setValue(toDraft);
		toBox.setResponder(text -> toDraft = text);
		toBox.setHint(Component.translatable(ModLang.LIST_MESSAGE_TO).withStyle(ChatFormatting.DARK_GRAY));
		messageBox = addRenderableWidget(new EditBox(font, 0, 0, 100, 10, Component.translatable(ModLang.LIST_MESSAGE)));
		messageBox.setBordered(false);
		messageBox.setMaxLength(PuppetCommands.MESSAGE_LENGTH);
		messageBox.setValue(messageDraft);
		messageBox.setResponder(text -> messageDraft = text);
		messageBox.setHint(Component.translatable(ModLang.LIST_MESSAGE).withStyle(ChatFormatting.DARK_GRAY));
		layoutBoxes();
	}

	/** The message's boxes: shown for a bee, on its message's row. */
	private void layoutBoxes() {
		if (toBox == null) return;
		Row row = chosenRow();
		// (A bee holding a player's soul runs no errands: it has no message row, phase 2d.)
		int rowTop = row == null ? -1 : rowY(row, RowKind.MESSAGE);
		boolean bee = rowTop >= 0;
		int y = rowTop + 3, x = left + PANE_X;
		toBox.setVisible(bee);
		messageBox.setVisible(bee);
		toBox.setX(x + 3);
		toBox.setY(y);
		toBox.setWidth(54);
		messageBox.setX(x + 66);
		messageBox.setY(y);
		messageBox.setWidth(PANE_W - 66 - 44);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean sure(int act) {
		return sureAct == act && sureTicks > 0;
	}

	@Override
	public void tick() {
		if (sureTicks > 0 && --sureTicks == 0) sureAct = -1;
		if (toBox != null) {
			toBox.tick();
			messageBox.tick();
		}
	}

	private @Nullable Row chosenRow() {
		if (chosen == null) return null;
		for (Row row : page.rows()) if (row.id().equals(chosen)) return row;
		return null;
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void send(UUID id, int act, String text, String to) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUUID(id);
		buf.writeVarInt(act);
		buf.writeUtf(text, PuppetCommands.MESSAGE_LENGTH * 4);
		buf.writeUtf(to, 64);
		ClientPlayNetworking.send(ModPackets.PUPPET_COMMAND, buf);
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	// --- Its buttons ---

	/**
	 * A button of the pane: its act, label, whether it can be pressed, where, what it does when pointed at (null: nothing to tell), and the
	 * player it is for (an offer's: empty for none).
	 */
	private record Button(int act, Component label, boolean enabled, int x, int y, int w, @Nullable Component tip, String to) {
		Button(int act, Component label, boolean enabled, int x, int y, int w, @Nullable Component tip) {
			this(act, label, enabled, x, y, w, tip, "");
		}
	}

	/** The acts pressed twice: taking it apart, making a General, severing a soul, entering it (one's body dies). */
	private static boolean twice(int act) {
		return act == PuppetCommands.DISMANTLE || act == PuppetCommands.MAKE_GENERAL || act == PuppetCommands.SEVER_SOUL || act == PuppetCommands.ENTER_SOUL
				|| act == PuppetCommands.LEAVE_SOUL || act == PuppetCommands.SELF_DESTRUCT;
	}

	/** The Self-destruct button's width, at the head of a puppet's page. */
	private int burstWidth() {
		return font.width(Component.translatable(ModLang.LIST_SELF_DESTRUCT)) + 12;
	}

	/** The other player nearest this one, within reach of an offer (PuppetPlayers#NEAR), or null. */
	private static @Nullable String besideMe() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return null;
		net.minecraft.world.entity.player.Player best = null;
		double bestDistance = com.example.defyingtheheavens.PuppetPlayers.NEAR;
		for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
			if (other == mc.player || other.isSpectator()) continue;
			double d = other.distanceTo(mc.player);
			if (d <= bestDistance) {
				bestDistance = d;
				best = other;
			}
		}
		return best == null ? null : best.getGameProfile().getName();
	}

	private List<Button> buttons() {
		List<Button> buttons = new ArrayList<>();
		Row row = chosenRow();
		if (row == null) return buttons;
		int x = left + PANE_X, y = rowY(row, RowKind.ORDERS);
		boolean able = row.inReach() && !row.inert() && !row.away();
		// Self-destruct (phase 3, pressed twice): at the head of its page, apart from its orders.
		int bw = burstWidth();
		buttons.add(new Button(PuppetCommands.SELF_DESTRUCT, Component.translatable(sure(PuppetCommands.SELF_DESTRUCT) ? ModLang.BOARD_SURE : ModLang.LIST_SELF_DESTRUCT),
				able && row.loaded(), x + PANE_W - bw, top + BODY_TOP + 2, bw, Component.translatable(ModLang.LIST_SELF_DESTRUCT_TIP)));
		int w = (PANE_W - 8) / 3, half = (PANE_W - 4) / 2;
		buttons.add(new Button(PuppetCommands.FOLLOW, Component.translatable(ModLang.LIST_FOLLOW), able, x, y, w, null));
		buttons.add(new Button(PuppetCommands.GUARD, Component.translatable(ModLang.LIST_GUARD), able, x + w + 4, y, w, null));
		buttons.add(new Button(PuppetCommands.HOLD, Component.translatable(ModLang.LIST_HOLD), able, x + 2 * (w + 4), y, w, null));
		int workY = rowY(row, RowKind.WORK), messageY = rowY(row, RowKind.MESSAGE), generalY = rowY(row, RowKind.GENERAL), soulY = rowY(row, RowKind.SOUL);
		if (workY >= 0) {
			buttons.add(new Button(PuppetCommands.WORK, Component.translatable(page.marked() ? ModLang.LIST_WORK : ModLang.LIST_WORK_UNMARKED),
					able && page.marked(), x, workY, PANE_W, null));
		}
		if (messageY >= 0) {
			buttons.add(new Button(PuppetCommands.MESSAGE, Component.translatable(ModLang.LIST_SEND), able && !messageDraft.isBlank() && !toDraft.isBlank(),
					x + PANE_W - 40, messageY, 40, null));
		}
		if (generalY >= 0 && row.ranks() >= 0) {
			// A General's ranks (phase 2c): mustered from round it, deployed round it.
			buttons.add(new Button(PuppetCommands.MUSTER, Component.translatable(ModLang.LIST_MUSTER, row.ranks(), Puppetry.GENERAL_RANKS),
					able && row.ranks() < Puppetry.GENERAL_RANKS, x, generalY, half,
					Component.translatable(ModLang.LIST_MUSTER_TIP, Puppetry.GENERAL_RANKS, (int) Puppetry.MUSTER_REACH)));
			buttons.add(new Button(PuppetCommands.DEPLOY, Component.translatable(ModLang.LIST_DEPLOY), able && row.ranks() > 0, x + half + 4, generalY, half,
					Component.translatable(ModLang.LIST_DEPLOY_TIP)));
		} else if (generalY >= 0) {
			Component stone = new ItemStack(ArtifactGrade.byIndex(row.grade()).stone()).getHoverName();
			buttons.add(new Button(PuppetCommands.MAKE_GENERAL, sure(PuppetCommands.MAKE_GENERAL) ? Component.translatable(ModLang.BOARD_SURE)
					: Component.translatable(ModLang.LIST_MAKE_GENERAL, Puppetry.GENERAL_STONES, stone), able, x, generalY, PANE_W,
					Component.translatable(ModLang.LIST_MAKE_GENERAL_TIP, Puppetry.GENERAL_RANKS, Puppetry.GENERAL_STONES, stone)));
		}
		if (soulY >= 0 && row.person() == 2) {
			// One's own soul in it, one's body (phase 2d): left (pressed twice), one's soul going on.
			buttons.add(new Button(PuppetCommands.LEAVE_SOUL, Component.translatable(sure(PuppetCommands.LEAVE_SOUL) ? ModLang.BOARD_SURE
					: ModLang.PUPPET_SOUL_LEAVE_BUTTON), row.loaded(), x, soulY, PANE_W, Component.translatable(ModLang.PUPPET_SOUL_LEAVE_TIP)));
		} else if (soulY >= 0 && row.holdsPlayer()) {
			// A player's soul held in it (phase 2d): released to play it or controlled again, and severed for good (pressed twice).
			boolean released = row.person() == 1;
			buttons.add(new Button(released ? PuppetCommands.CONTROL_SOUL : PuppetCommands.RELEASE_SOUL,
					Component.translatable(released ? ModLang.LIST_CONTROL_SOUL : ModLang.LIST_RELEASE_SOUL), row.loaded(), x, soulY, half,
					Component.translatable(released ? ModLang.LIST_CONTROL_SOUL_TIP : ModLang.LIST_RELEASE_SOUL_TIP)));
			buttons.add(new Button(PuppetCommands.SEVER_SOUL, Component.translatable(sure(PuppetCommands.SEVER_SOUL) ? ModLang.BOARD_SURE : ModLang.LIST_SEVER_SOUL),
					row.loaded(), x + half + 4, soulY, half, Component.translatable(ModLang.LIST_SEVER_SOUL_TIP)));
		} else if (soulY >= 0) {
			// A hollow one (phase 2d): entered oneself (pressed twice: one's body dies), or offered to the player beside one.
			buttons.add(new Button(PuppetCommands.ENTER_SOUL, Component.translatable(sure(PuppetCommands.ENTER_SOUL) ? ModLang.BOARD_SURE
					: ModLang.PUPPET_SOUL_ENTER_BUTTON), !row.away(), x, soulY, half, Component.translatable(ModLang.LIST_ENTER_SOUL_TIP)));
			String beside = besideMe();
			buttons.add(new Button(PuppetCommands.OFFER_SOUL, beside == null ? Component.translatable(ModLang.LIST_OFFER_NOBODY)
					: Component.translatable(ModLang.LIST_OFFER_SOUL, beside), beside != null && !row.away(), x + half + 4, soulY, half,
					Component.translatable(ModLang.LIST_OFFER_SOUL_TIP), beside == null ? "" : beside));
		}
		int footY = rowY(row, RowKind.FOOT);
		Component dismantle = Component.translatable(sure(PuppetCommands.DISMANTLE) ? ModLang.BOARD_SURE : ModLang.LIST_DISMANTLE);
		buttons.add(new Button(PuppetCommands.DISMANTLE, dismantle, !row.away(), x, footY, 70, null));
		// A Sub-Seal written for it, and every one revoked (phase 2c).
		int sw = (PANE_W - 70 - 8) / 2;
		buttons.add(new Button(PuppetCommands.SUB_SEAL, Component.translatable(ModLang.LIST_SUB_SEAL), row.loaded() && !row.away(), x + 74, footY, sw,
				Component.translatable(ModLang.LIST_SUB_SEAL_TIP)));
		buttons.add(new Button(PuppetCommands.REVOKE, Component.translatable(ModLang.LIST_REVOKE), row.loaded() && !row.away(), x + 78 + sw, footY,
				PANE_W - 78 - sw, Component.translatable(ModLang.LIST_REVOKE_TIP)));
		return buttons;
	}

	// --- Input ---

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			Row row = chosenRow();
			for (Button b : buttons()) {
				if (!in(mx, my, b.x(), b.y(), b.w(), BUTTON_H)) continue;
				if (b.enabled() && row != null) {
					if (twice(b.act()) && !sure(b.act())) {
						sureAct = b.act();
						sureTicks = 60;
					} else {
						sureAct = -1;
						sureTicks = 0;
						send(row.id(), b.act(), b.act() == PuppetCommands.MESSAGE ? messageDraft : "", b.act() == PuppetCommands.MESSAGE ? toDraft : b.to());
						if (b.act() == PuppetCommands.MESSAGE) {
							messageDraft = "";
							messageBox.setValue("");
						}
					}
				}
				return true;
			}
			int listTop = top + BODY_TOP, listBottom = top + BODY_BOTTOM;
			if (in(mx, my, left + LIST_X, listTop, LIST_W - ROLLER_W - 2, listBottom - listTop)) {
				int index = (int) ((my - listTop + listScroll) / ROW_H);
				if (index >= 0 && index < page.rows().size()) {
					chosen = page.rows().get(index).id();
					sureAct = -1;
					sureTicks = 0;
					layoutBoxes();
					send(chosen, PuppetCommands.CHOOSE, "", "");
				}
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		listScroll = Mth.clamp(listScroll - (int) Math.signum(delta) * ROW_H, 0, listMax);
		return true;
	}

	// --- Drawing ---

	private ItemStack icon(Row row) {
		return Artifacts.create(ArtifactKind.frameOf(row.blueprint()), ArtifactGrade.byIndex(row.grade()), Artifacts.Quality.NORMAL, null);
	}

	/** Where {@code row} stands against the player: far, in another world, away, inert, out of reach. */
	private Component whereAbout(Row row) {
		if (!row.inRanksOf().isEmpty()) return Component.translatable(ModLang.LIST_IN_RANKS, row.inRanksOf());
		if (row.away()) return Component.translatable(ModLang.LIST_AWAY);
		if (row.inert()) return Component.translatable(ModLang.LIST_INERT);
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !mc.player.level().dimension().location().equals(row.dimension())) {
			return Component.translatable(ModLang.LIST_OTHER_WORLD, row.dimension().getPath().replace('_', ' '));
		}
		int far = (int) Math.round(Math.sqrt(mc.player.blockPosition().distSqr(row.pos())));
		return Component.translatable(row.inReach() ? ModLang.LIST_FAR : ModLang.LIST_OUT_OF_REACH, far);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float partialTick) {
		renderBackground(g);
		TalkUi.panel(g, left, top, left + W, top + H, TalkUi.GOLD);
		HallHud.heading(g, font, title, left + 8, top + 6, W - 16);
		Component slots = Component.translatable(ModLang.LIST_SLOTS, page.used(), page.slots());
		Component reach = Component.translatable(ModLang.LIST_REACH, page.reach());
		// The seal in hand's grade (phase 2c).
		Component seal = page.heldSeal() < 0 ? Component.translatable(ModLang.LIST_NO_SEAL)
				: Component.translatable(ModLang.LIST_SEAL, ArtifactGrade.byIndex(page.heldSeal()).getDisplayName());
		int rx = left + W - 10 - font.width(reach);
		g.drawString(font, reach, rx, top + 6, TalkUi.MUTED, false);
		int sx = rx - 10 - font.width(slots);
		g.drawString(font, slots, sx, top + 6, page.used() >= page.slots() ? TalkUi.GOLD_TEXT : TalkUi.MUTED, false);
		g.drawString(font, seal, sx - 10 - font.width(seal), top + 6, TalkUi.MUTED, false);
		g.fill(left + PANE_X - 5, top + BODY_TOP, left + PANE_X - 4, top + BODY_BOTTOM, TalkUi.GOLD_DIM);
		ItemStack hover = renderList(g, mx, my);
		renderPane(g, mx, my);
		if (page.said() != null) {
			Component said = page.said();
			String text = said.getString();
			if (font.width(said) > W - 20) text = font.plainSubstrByWidth(text, W - 20 - font.width("...")) + "...";
			g.drawString(font, Component.literal(text).withStyle(said.getStyle()), left + 8, top + H - 13, TalkUi.CREAM, false);
		}
		super.render(g, mx, my, partialTick);
		if (hover != null) g.renderTooltip(font, hover, mx, my);
		// What a button of phase 2c's does, pointed at.
		for (Button b : buttons()) {
			if (b.tip() != null && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H)) g.renderTooltip(font, font.split(b.tip(), 200), mx, my);
		}
	}

	private @Nullable ItemStack renderList(GuiGraphics g, int mx, int my) {
		int x = left + LIST_X, viewTop = top + BODY_TOP, viewBottom = top + BODY_BOTTOM, rowW = LIST_W - ROLLER_W - 3;
		List<Row> rows = page.rows();
		if (rows.isEmpty()) {
			int y = viewTop + 4;
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.LIST_NONE), rowW - 4)) {
				g.drawString(font, line, x + 2, y, TalkUi.MUTED, false);
				y += 10;
			}
			return null;
		}
		int content = rows.size() * ROW_H;
		listMax = Math.max(0, content - (viewBottom - viewTop));
		listScroll = Mth.clamp(listScroll, 0, listMax);
		g.enableScissor(x, viewTop, x + rowW, viewBottom);
		ItemStack hovered = null;
		for (int k = 0; k < rows.size(); k++) {
			Row row = rows.get(k);
			int y = viewTop + k * ROW_H - listScroll;
			if (y + ROW_H <= viewTop || y >= viewBottom) continue;
			boolean on = row.id().equals(chosen), lit = in(mx, my, x, y, rowW, ROW_H) && my < viewBottom;
			if (on) {
				g.fill(x, y, x + rowW, y + ROW_H - 1, HallHud.ROW_OPEN);
				g.fill(x, y, x + 1, y + ROW_H - 1, HallHud.GOLD);
			} else if (lit) {
				g.fill(x, y, x + rowW, y + ROW_H - 1, HallHud.ROW_LIT);
			}
			ItemStack icon = icon(row);
			g.renderItem(icon, x + 2, y + 2);
			String name = row.name();
			if (font.width(name) > rowW - 26) name = font.plainSubstrByWidth(name, rowW - 26 - font.width("...")) + "...";
			g.drawString(font, name, x + 21, y + 2, on || lit ? TalkUi.CREAM_BRIGHT : TalkUi.CREAM, false);
			// Its order, and its health as a thin bar.
			Component order = PuppetEntity.Order.byIndex(row.order()).getDisplayName();
			g.drawString(font, order, x + 21, y + 12, row.inert() || row.away() ? TalkUi.DISABLED : TalkUi.GOLD_TEXT, false);
			int barX = x + 25 + font.width(order), barW = rowW - (barX - x) - 4;
			if (barW > 8) {
				g.fill(barX, y + 14, barX + barW, y + 17, 0xFF2A1F14);
				g.fill(barX, y + 14, barX + Math.round(barW * Mth.clamp(row.health() / Math.max(1, row.max()), 0, 1)), y + 17, HallHud.HEALTH);
			}
			if (in(mx, my, x + 2, y + 2, 16, 16) && my < viewBottom) hovered = icon;
		}
		g.disableScissor();
		CodexScreen.roller(g, x + LIST_W - ROLLER_W, viewTop, viewBottom, content, listScroll, listMax);
		return hovered;
	}

	private void renderPane(GuiGraphics g, int mx, int my) {
		Row row = chosenRow();
		int x = left + PANE_X, y = top + BODY_TOP;
		if (row == null) return;
		g.renderItem(icon(row), x, y);
		// Its name, short of the Self-destruct button beside it (phase 3).
		String name = row.name();
		int room = PANE_W - 24 - burstWidth();
		if (font.width(name) > room) name = font.plainSubstrByWidth(name, room - font.width("...")) + "...";
		g.drawString(font, name, x + 20, y + 4, TalkUi.GOLD_TEXT, true);
		g.fill(x, y + 19, x + PANE_W, y + 20, TalkUi.GOLD_DIM);
		y += 23;
		List<Component> lines = new ArrayList<>();
		// Its realm, and the grade of the seal it is bound to (phase 2c).
		net.minecraft.network.chat.MutableComponent realm = PlayerCultivation.rankName(CultivatorNpc.realmOf(row.rank()), CultivatorNpc.stageOf(row.rank())).copy();
		if (row.seal() >= 0) realm.append(" · ").append(Component.translatable(ModLang.LIST_BOUND_SEAL, ArtifactGrade.byIndex(row.seal()).getDisplayName()));
		lines.add(realm.withStyle(s -> s.withColor(TalkUi.SECT_TEXT)));
		// Whole numbers as the health bar shows them: a scratch rounds up, never past the whole.
		int max = (int) Math.ceil(row.max()), health = Math.min(max, (int) Math.ceil(row.health()));
		lines.add(Component.translatable(ModLang.LIST_HEALTH, health, max).withStyle(s -> s.withColor(TalkUi.CREAM)));
		lines.add(Component.translatable(ModLang.LIST_PLACE, row.pos().getX(), row.pos().getY(), row.pos().getZ()).append(" · ").append(whereAbout(row))
				.withStyle(s -> s.withColor(row.inReach() && !row.away() ? TalkUi.MUTED : TalkUi.WARNING)));
		net.minecraft.network.chat.MutableComponent order = Component.translatable(ModLang.LIST_ORDER, PuppetEntity.Order.byIndex(row.order()).getDisplayName());
		if (!row.lentTo().isEmpty()) order.append(" · ").append(Component.translatable(ModLang.LIST_LENT, row.lentTo()));
		lines.add(order.withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
		// A General's ranks, and a mind grown of its own (phase 2c).
		if (row.ranks() >= 0) lines.add(Component.translatable(ModLang.LIST_GENERAL, row.ranks(), Puppetry.GENERAL_RANKS).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
		// A player's soul held in it (phase 2d): how far its copy has come, and whether it plays the puppet now.
		if (row.person() == 2) {
			lines.add(Component.translatable(ModLang.LIST_PERSON_OWN).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
		} else if (row.holdsPlayer()) {
			lines.add(Component.translatable(row.person() == 1 ? ModLang.LIST_PERSON_RELEASED : ModLang.LIST_PERSON_HELD, row.copied())
					.withStyle(s -> s.withColor(0xE0A0FF)));
		}
		PuppetSoul.Trait mind = PuppetSoul.Trait.byIndex(row.mind());
		if (mind != null) {
			lines.add(Component.translatable(ModLang.LIST_MIND, mind.getDisplayName(), Component.translatable(ModLang.puppetTraitAbout(mind.getId())))
					.withStyle(s -> s.withColor(0xD8A8FF)));
		}
		if (row.circuits() != null && row.scores().length == 5) {
			// As far as one's Puppetry reads them, as its board shows them (the list read all five out to anyone: 2026-11-18).
			int[] s5 = row.scores();
			Object[] read = new Object[5];
			for (int k = 0; k < 5; k++) read[k] = page.rank() >= PuppetBoardScreen.SCORE_RANK[k] ? (Object) s5[k] : "-";
			lines.add(Component.translatable(ModLang.LIST_SCORES, row.circuits(), read[0], read[1], read[2], read[3], read[4]).withStyle(s -> s.withColor(TalkUi.MUTED)));
			if (row.cuts() > 0) lines.add(Component.translatable(ModLang.LIST_CUTS, row.cuts()).withStyle(s -> s.withColor(TalkUi.WARNING)));
			// The linked army (phase 3): its linked fellows near and what they lend it; held down where it stands; a spring in it.
			if (row.fellows() >= 0) {
				lines.add(Component.translatable(ModLang.LIST_LINKED, row.fellows(), Math.round(100 * Puppetry.linkBonus(row.fellows())))
						.withStyle(s -> s.withColor(0xFFE8D27A)));
			}
			if (row.heldDown()) lines.add(Component.translatable(ModLang.LIST_HELD_DOWN).withStyle(s -> s.withColor(TalkUi.WARNING)));
			if (row.spring()) lines.add(Component.translatable(ModLang.LIST_SPRING).withStyle(s -> s.withColor(0xFF7FE0F0)));
		} else {
			lines.add(Component.translatable(ModLang.LIST_UNLOADED).withStyle(s -> s.withColor(TalkUi.DISABLED)));
		}
		if (row.blueprint() == PuppetBlueprint.BEE && !row.holdsPlayer()) lines.add(Component.translatable(ModLang.LIST_MESSENGER).withStyle(s -> s.withColor(TalkUi.MUTED)));
		int bottom = rowY(row, RowKind.ORDERS) - 3;
		for (Component line : lines) {
			for (FormattedCharSequence part : font.split(line, PANE_W)) {
				if (y + 9 > bottom) break;
				g.drawString(font, part, x, y, TalkUi.CREAM, false);
				y += 10;
			}
		}
		// The message's ink wells, for a bee (not one holding a player's soul).
		int wy = rowY(row, RowKind.MESSAGE);
		if (wy >= 0) {
			HallHud.panel(g, x, wy, x + 60, wy + 14, HallHud.FRAME, TalkUi.INNER);
			HallHud.panel(g, x + 63, wy, x + PANE_W - 43, wy + 14, HallHud.FRAME, TalkUi.INNER);
		}
		for (Button b : buttons()) {
			boolean hover = b.enabled() && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H);
			HallHud.button(g, font, b.x(), b.y(), b.w(), BUTTON_H, b.label(), b.enabled(), hover);
		}
	}
}
