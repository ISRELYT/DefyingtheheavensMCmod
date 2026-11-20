package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ArtifactGrade;
import com.example.defyingtheheavens.CircuitBoard;
import com.example.defyingtheheavens.CraftMastery;
import com.example.defyingtheheavens.DemonCoreItem;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PuppetBlueprint;
import com.example.defyingtheheavens.PuppetBoards;
import com.example.defyingtheheavens.PuppetSoul;
import com.example.defyingtheheavens.Puppetry;
import com.example.defyingtheheavens.Spell;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A puppet's circuit board in its puppeteer's hands (Puppetry; S2C puppet_board, PuppetBoards): the board on the left, square by square, its
 * core's socket in the middle, its nodes, joints and flaws, and the lines of qi painted on it glowing azure (dim where nothing feeds them);
 * on the right the strokes to paint with (those above one's Puppetry rank locked), the board's scores as far as one's rank reads them, the
 * qi its veins draw of what its core gives (the deeper circuits, phase A), its nodes fed or dead (each one's veins, power and toughness
 * when pointed at), the core, the brush, and the buttons: the rank's Circuit Path for a build laid over the board as a ghost, every stroke
 * in its own colour (Path, again for the next build), the heat left in the lines (Heat, Grandmaster), the cells a single cut would sever
 * a node at (Weak spots, Saint), wiping the board, and at a bench sealing the puppet.
 * <p>
 * Before a player's soul held in another's puppet (phase 2d, its menu's first tab; the user, 2026-11-18: "a non-free puppet should see
 * circuits too so they can takeover/struggle free") the board is only to look at: its captor's command line lit violet from the core, the
 * next cell a struggle cuts filling as it is cut, Struggle and Let go under its Soul line; sent again each second.
 * <p>
 * Left-click or drag paints the chosen stroke, right-click or drag wipes; 1 to 9 choose a stroke. Each stroke is painted on the screen's
 * own copy at once and sent (C2S puppet_paint); the server's board comes back with the answer to the last one sent and is taken as it is.
 */
public class PuppetBoardScreen extends Screen implements HallFrame.Framed {
	private static final int W = 412, H = 236, BOARD_X = 8, BOARD_Y = 22, BOARD_PX = 200, PANE_X = 216, PANE_W = W - PANE_X - 8, BUTTON_H = 14;
	private static final int LINE = 0xFF5FD8FF, LINE_THICK = 0xFFA8F0FF, LINE_DEAD = 0xFF35556A, RING = 0xFFE8D27A, GHOST = 0x80FFFFFF, GHOST_ABOVE = 0x60707070,
			CUT = 0xFFE05050, WEAK = 0xFFFF9A3A, GROUND_A = 0xFF1C1610, GROUND_B = 0xFF19140E, BRASS = 0xFF8A7038, BRASS_LIGHT = 0xFFC8A858;
	/**
	 * The strokes to paint with, in the palette's order (1 to 9); the tenth slot (0) is the phase seal, its element chosen by clicking again;
	 * the eleventh (L) the link knot (phase 3).
	 */
	private static final CircuitBoard.Stroke[] PALETTE = {CircuitBoard.Stroke.LINE, CircuitBoard.Stroke.THICK, CircuitBoard.Stroke.DIAGONAL,
			CircuitBoard.Stroke.BRIDGE, CircuitBoard.Stroke.RELAY, CircuitBoard.Stroke.VENT, CircuitBoard.Stroke.GATHER, CircuitBoard.Stroke.WARD,
			CircuitBoard.Stroke.RING};
	private static final int PALETTE_SLOTS = 11, PALETTE_STEP = 17, PALETTE_BOX = 16, SEAL_SLOT = 9, LINK_SLOT = 10;
	/** The scores, and the Puppetry rank that reads each (Apprentice reads none: the nodes alone); the Command Seal's list reads them so too. */
	private static final String[] SCORES = {"flow", "stability", "harmony", "clarity", "resilience"};
	static final int[] SCORE_RANK = {1, 2, 2, 3, 3};

	/** The page as the server sent it (PuppetBoards#send). */
	/**
	 * The page: the board and all round it. A puppet's soul (phase 2a): its kind (-1 hollow, or a frame on a bench), its name, how far its copy
	 * has come and whether it is whole, its arts (spells' names); whether the puppet stands by a bench, and whether a ring waits there.
	 */
	public record Page(int seq, boolean open, Component title, PuppetBlueprint blueprint, int grade, int flaws, long seed, byte[] strokes, int[] cuts,
			int rank, boolean sealed, ItemStack core, Component coreLine, int brushLeft, boolean creative, byte[] diagram, int[] wearCuts, int[] wearHeat,
			ItemStack stones, boolean haveStones, int used, int slots, int cost, int soulKind, String soulName, int soulCopied, boolean soulWhole,
			List<String> soulArts, boolean atBench, boolean canMove, byte[] meridians, int refining, boolean ownBody, @Nullable Captive captive,
			@Nullable Component said) {}

	/**
	 * Another's puppet, its board before the soul held in it (PuppetPlayers#writeCaptive; the user, 2026-11-18: "a non-free puppet should see
	 * circuits too so they can takeover/struggle free"): its captor, how far the copy of it has come, whether it may struggle (else the
	 * puppet's realm, for why not) and whether it does, the command line (cells from the core) with each cell's seconds from within, how far
	 * the next is cut, and whether it plays the puppet.
	 */
	public record Captive(String captor, int copied, boolean mayStruggle, boolean struggling, Component realm, int[] line, int[] seconds, int progress,
			boolean playing) {
		static Captive read(FriendlyByteBuf buf) {
			return new Captive(buf.readUtf(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readComponent(), buf.readVarIntArray(),
					buf.readVarIntArray(), buf.readVarInt(), buf.readBoolean());
		}

		/** Where {@code cell} stands on the command line (0: the next a struggle cuts), or -1 off it. */
		int indexOf(int cell) {
			for (int k = 0; k < line.length; k++) if (line[k] == cell) return k;
			return -1;
		}
	}

	public static Page read(FriendlyByteBuf buf) {
		int seq = buf.readVarInt();
		if (!buf.readBoolean()) return new Page(seq, false, Component.empty(), PuppetBlueprint.HUMANOID, 0, 0, 0, new byte[0], new int[0], 0, false,
				ItemStack.EMPTY, Component.empty(), -1, false, new byte[0], new int[0], new int[0], ItemStack.EMPTY, false, 0, 0, 0, -1, "", 0, false,
				List.of(), false, false, new byte[0], -1, false, null, null);
		Component title = buf.readComponent();
		PuppetBlueprint blueprint = PuppetBlueprint.byId(buf.readUtf());
		int grade = buf.readVarInt(), flaws = buf.readVarInt();
		long seed = buf.readLong();
		byte[] strokes = buf.readByteArray();
		int[] cuts = buf.readVarIntArray();
		int rank = buf.readVarInt();
		boolean sealed = buf.readBoolean();
		ItemStack core = buf.readItem();
		Component coreLine = buf.readComponent();
		int brush = buf.readVarInt();
		boolean creative = buf.readBoolean();
		byte[] diagram = buf.readByteArray();
		int[] wearCuts = buf.readVarIntArray(), wearHeat = buf.readVarIntArray();
		ItemStack stones = buf.readItem();
		boolean haveStones = buf.readBoolean();
		int used = buf.readVarInt(), slots = buf.readVarInt(), cost = buf.readVarInt();
		int soulKind = buf.readVarInt(), soulCopied = 0;
		String soulName = "";
		boolean soulWhole = false;
		List<String> soulArts = new ArrayList<>();
		if (soulKind >= 0) {
			soulName = buf.readUtf();
			soulCopied = buf.readVarInt();
			soulWhole = buf.readBoolean();
			for (int k = buf.readVarInt(); k > 0; k--) soulArts.add(buf.readUtf());
		}
		boolean atBench = buf.readBoolean(), canMove = buf.readBoolean();
		byte[] meridians = buf.readByteArray();
		int refining = buf.readVarInt();
		boolean ownBody = buf.readBoolean();
		Captive captive = buf.readBoolean() ? Captive.read(buf) : null;
		Component said = buf.readBoolean() ? buf.readComponent() : null;
		return new Page(seq, true, title, blueprint == null ? PuppetBlueprint.HUMANOID : blueprint, grade, flaws, seed, strokes, cuts, rank, sealed,
				core, coreLine, brush, creative, diagram, wearCuts, wearHeat, stones, haveStones, used, slots, cost, soulKind, soulName, soulCopied,
				soulWhole, soulArts, atBench, canMove, meridians, refining, ownBody, captive, said);
	}

	/**
	 * The page come: onto the open screen, a new one opened, or (the board gone or sealed) the screen closed. Opened from the cultivation menu
	 * (a free soul's first tab there is its own body's board: ClientPuppetPilot), closing it goes back to the menu.
	 */
	public static void receive(Page page) {
		Minecraft mc = Minecraft.getInstance();
		if (!page.open()) {
			if (mc.screen instanceof PuppetBoardScreen open) {
				open.closedByServer = true;
				mc.setScreen(open.parent);
			}
			return;
		}
		if (mc.screen instanceof PuppetBoardScreen open) open.update(page);
		else {
			PuppetBoardScreen opened = new PuppetBoardScreen(page);
			if (mc.screen instanceof CultivationScreen menu) opened.parent = menu;
			mc.setScreen(opened);
		}
	}

	/** The screen to go back to as it closes (the cultivation menu it was opened from), or none. */
	private @Nullable Screen parent;

	private Page page;
	private CircuitBoard board;
	private CircuitBoard.Scores scores;
	private boolean[] fed;
	private int[] heat;
	private boolean[] weak;
	private CircuitBoard.Stroke[] path;
	/** The qi its core gives (Puppetry#budget), and the build whose Circuit Path is shown (an index into {@link #builds}). */
	private int budget, build;
	/** Its core's phase (null: none of the five), each cell's phase and clash as the board stands (phase B), the phase seal to paint. */
	private CircuitBoard.@Nullable Phase corePhase;
	private CircuitBoard.Phase[] phaseAt;
	private boolean[] clashAt;
	private CircuitBoard.Phase sealPhase = CircuitBoard.Phase.METAL;
	private CircuitBoard.Stroke chosen = CircuitBoard.Stroke.LINE;
	/** What lies over the board as a ghost: nothing, the rank's Circuit Path for a build, or the Circuit Diagram in hand. */
	private int ghostMode;
	private static final int GHOST_NONE = 0, GHOST_PATH = 1, GHOST_DIAGRAM = 2;
	private boolean showHeat, showWeak, closedByServer;
	private int seq, brushLeft, sureWipe, sureSoul, dragButton = -1, lastCell = -1;
	/** Where the soul's buttons stand (under its Soul line, as the pane last laid it out); -1: none shown. */
	private int soulRowY = -1;
	private @Nullable Component said;
	private int left, top;

	private PuppetBoardScreen(Page page) {
		super(page.title());
		update(page);
	}

	private void update(Page page) {
		Page before = this.page;
		this.page = page;
		// The board as the server has it, once it has answered the last stroke sent (or on its own: opened, wiped).
		if (before == null || page.seq() == 0 || page.seq() == seq) {
			board = new CircuitBoard(page.blueprint(), page.grade(), page.flaws(), page.seed());
			// A body's own meridians (phase 2b), before its strokes.
			board.setMeridians(page.meridians().length > 0 ? com.example.defyingtheheavens.PuppetBodies.mask(page.meridians()) : null);
			Puppetry.lay(board, page.strokes());
			int qi = Puppetry.budget(ArtifactGrade.byIndex(page.grade()), page.core());
			CircuitBoard.Phase phase = Puppetry.phaseOf(page.core());
			if (before == null || before.rank() != page.rank() || qi != budget || phase != corePhase) path = null;
			budget = qi;
			corePhase = phase;
			rescore();
			brushLeft = page.brushLeft();
		}
		if (page.said() != null) said = page.said();
	}

	private void rescore() {
		scores = board.score(budget, corePhase);
		fed = board.fedCells();
		phaseAt = board.phases();
		clashAt = board.clashes();
		heat = null;
		weak = null;
	}

	/** The builds the rank's Circuit Path shows for this frame (an Apprentice knows one way only). */
	private List<CircuitBoard.Build> builds() {
		return page.rank() == 0 ? List.of(CircuitBoard.Build.BALANCED) : CircuitBoard.Build.of(page.blueprint(), board.hasArt());
	}

	private CircuitBoard.Build shownBuild() {
		List<CircuitBoard.Build> builds = builds();
		return builds.get(Math.floorMod(build, builds.size()));
	}

	private CircuitBoard.Stroke[] path() {
		if (path == null) path = board.circuitPath(page.rank(), shownBuild(), budget, corePhase);
		return path;
	}

	/** The Circuit Diagram in hand, as strokes in board order (null with none of this make). */
	private CircuitBoard.Stroke @Nullable [] diagram() {
		byte[] d = page.diagram();
		if (d.length < board.size * board.size) return null;
		CircuitBoard.Stroke[] all = CircuitBoard.Stroke.values(), out = new CircuitBoard.Stroke[board.size * board.size];
		for (int i = 0; i < out.length; i++) out[i] = d[i] > 0 && d[i] < all.length ? all[d[i]] : CircuitBoard.Stroke.NONE;
		return out;
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		if (!closedByServer) send(PuppetBoards.CLOSE);
		super.removed();
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(parent);
	}

	@Override
	public void tick() {
		if (sureWipe > 0) sureWipe--;
		if (sureSoul > 0) sureSoul--;
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private void send(int act) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(act);
		ClientPlayNetworking.send(ModPackets.PUPPET_ACT, buf);
	}

	private void click() {
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	// --- The board's place on the screen ---

	private int cell() {
		return Math.min(22, BOARD_PX / board.size);
	}

	private int boardLeft() {
		return left + BOARD_X + (BOARD_PX - cell() * board.size) / 2;
	}

	private int boardTop() {
		return top + BOARD_Y + (BOARD_PX - cell() * board.size) / 2;
	}

	/** The cell under the mouse, or -1. */
	private int cellAt(double mx, double my) {
		int c = cell(), bx = boardLeft(), by = boardTop();
		if (mx < bx || my < by) return -1;
		int x = (int) ((mx - bx) / c), y = (int) ((my - by) / c);
		return board.inside(x, y) ? board.at(x, y) : -1;
	}

	// --- Painting ---

	/** Paints {@code stroke} (NONE wipes) on {@code i} here at once, and sends it. */
	private void paint(int i, CircuitBoard.Stroke stroke) {
		int x = i % board.size, y = i / board.size;
		if (stroke.rank > page.rank()) {
			said = Component.translatable(ModLang.PAINT_RANK, CraftMastery.Rank.values()[stroke.rank].getDisplayName()).withStyle(ChatFormatting.RED);
			return;
		}
		String refusal = board.refusal(x, y, stroke);
		if (refusal != null) {
			if (!refusal.equals("same") && !refusal.equals("empty")) said = Component.translatable(ModLang.boardRefusal(refusal)).withStyle(ChatFormatting.RED);
			return;
		}
		// (One's own body's lines, a free soul's, are painted with its own qi: no brush, phase 2d.)
		if (stroke != CircuitBoard.Stroke.NONE && !page.creative() && !page.ownBody()) {
			int wear = stroke == CircuitBoard.Stroke.THICK && board.stroke(x, y) == CircuitBoard.Stroke.LINE ? 1 : stroke.wear;
			if (brushLeft < wear) {
				said = Component.translatable(ModLang.PAINT_NO_BRUSH).withStyle(ChatFormatting.RED);
				return;
			}
			brushLeft -= wear;
		}
		board.paint(x, y, stroke);
		rescore();
		said = null;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(++seq);
		buf.writeVarInt(x);
		buf.writeVarInt(y);
		buf.writeVarInt(stroke.ordinal());
		ClientPlayNetworking.send(ModPackets.PUPPET_PAINT, buf);
	}

	// --- Its buttons ---

	private record Button(int id, Component label, boolean enabled, boolean on, int x, int y, int w) {}

	private static final int B_PATH = 0, B_HEAT = 1, B_WEAK = 2, B_WIPE = 3, B_SEAL = 4, B_COPY = 5, B_SOUL = 6, B_MOVE = 7, B_LEAVE = 8, B_PUPPETS = 9,
			B_STRUGGLE = 10, B_LET_GO = 11;

	private List<Button> buttons() {
		List<Button> buttons = new ArrayList<>();
		int y = top + H - 34, x = left + PANE_X, w = 29, step = 31;
		Component pathLabel = Component.translatable(ghostMode == GHOST_DIAGRAM ? ModLang.BOARD_DIAGRAM : ModLang.BOARD_PATH);
		buttons.add(new Button(B_PATH, pathLabel, true, ghostMode != GHOST_NONE, x, y, w));
		x += step;
		buttons.add(new Button(B_HEAT, Component.translatable(ModLang.BOARD_HEAT), page.rank() >= 3, showHeat, x, y, w));
		x += step;
		buttons.add(new Button(B_WEAK, Component.translatable(ModLang.BOARD_WEAK), page.rank() >= 4, showWeak, x, y, w));
		x += step;
		// (A captor's board, before the soul it holds: looked at, never wiped nor copied.)
		if (page.captive() == null) {
			buttons.add(new Button(B_WIPE, Component.translatable(sureWipe > 0 ? ModLang.BOARD_SURE : ModLang.BOARD_WIPE), true, sureWipe > 0, x, y, w));
			x += step;
			buttons.add(new Button(B_COPY, Component.translatable(ModLang.BOARD_COPY), true, false, x, y, w));
			x += step;
		}
		if (!page.sealed()) buttons.add(new Button(B_SEAL, Component.translatable(ModLang.BOARD_SEAL), true, false, x, y, left + W - 8 - x));
		else if (page.captive() != null && soulRowY > 0) {
			// Held in another's (phase 2d): struggling against its command line (if one may), letting go (asked twice).
			Captive captive = page.captive();
			int sx = left + PANE_X;
			Component struggle = Component.translatable(captive.struggling() ? ModLang.BOARD_STRUGGLE_ON : ModLang.PUPPET_SOUL_STRUGGLE_BUTTON);
			int stw = Math.max(font.width(Component.translatable(ModLang.BOARD_STRUGGLE_ON)), font.width(struggle)) + 14;
			buttons.add(new Button(B_STRUGGLE, struggle, captive.mayStruggle() && !captive.struggling(), captive.struggling(), sx, soulRowY, stw));
			Component letGo = Component.translatable(sureSoul > 0 ? ModLang.BOARD_SURE : ModLang.PUPPET_SOUL_LET_GO_BUTTON);
			int lw = Math.max(font.width(Component.translatable(ModLang.PUPPET_SOUL_LET_GO_BUTTON)), font.width(letGo)) + 14;
			buttons.add(new Button(B_LET_GO, letGo, true, sureSoul > 0, sx + stw + 3, soulRowY, lw));
		} else if (page.ownBody() && soulRowY > 0) {
			// One's own body, a free soul playing it (phase 2d: the menu's first tab): leaving it (asked twice), one's other puppets.
			int sx = left + PANE_X;
			Component leave = Component.translatable(sureSoul > 0 ? ModLang.BOARD_SURE : ModLang.PUPPET_SOUL_LEAVE_BUTTON);
			int lw = Math.max(font.width(Component.translatable(ModLang.PUPPET_SOUL_LEAVE_BUTTON)), font.width(leave)) + 14;
			buttons.add(new Button(B_LEAVE, leave, true, sureSoul > 0, sx, soulRowY, lw));
			Component puppets = Component.translatable(ModLang.SOUL_SCREEN_PUPPETS);
			buttons.add(new Button(B_PUPPETS, puppets, true, false, sx + lw + 3, soulRowY, font.width(puppets) + 14));
		} else if (page.soulKind() >= 0 && soulRowY > 0) {
			// A puppet's soul (phase 2a), by a bench, in a row of their own under its Soul line: a real one let go or moved, a whole copy
			// wiped (letting go and wiping asked twice).
			int sx = left + PANE_X;
			Component soul = Component.translatable(sureSoul > 0 ? ModLang.BOARD_SURE : page.soulWhole() ? ModLang.BOARD_SOUL_WIPE : ModLang.BOARD_SOUL_RELEASE);
			int sw = Math.max(font.width(Component.translatable(ModLang.BOARD_SOUL_RELEASE)), font.width(soul)) + 14;
			buttons.add(new Button(B_SOUL, soul, page.atBench(), sureSoul > 0, sx, soulRowY, sw));
			if (!page.soulWhole()) {
				Component move = Component.translatable(ModLang.BOARD_SOUL_MOVE);
				buttons.add(new Button(B_MOVE, move, page.canMove(), false, sx + sw + 3, soulRowY, font.width(move) + 14));
			}
		}
		return buttons;
	}

	private void press(Button b) {
		click();
		switch (b.id()) {
			// Nothing, the rank's path for each build in turn, the diagram in hand (if there is one), nothing again.
			case B_PATH -> {
				if (ghostMode == GHOST_NONE) {
					ghostMode = GHOST_PATH;
					build = 0;
				} else if (ghostMode == GHOST_PATH && build + 1 < builds().size()) {
					build++;
				} else {
					ghostMode = ghostMode == GHOST_PATH && diagram() != null ? GHOST_DIAGRAM : GHOST_NONE;
					build = 0;
				}
				path = null;
			}
			case B_COPY -> send(PuppetBoards.COPY);
			case B_HEAT -> showHeat = !showHeat;
			case B_WEAK -> showWeak = !showWeak;
			case B_WIPE -> {
				if (sureWipe > 0) {
					sureWipe = 0;
					send(PuppetBoards.WIPE_ALL);
				} else {
					sureWipe = 60;
				}
			}
			case B_SEAL -> send(PuppetBoards.SEAL);
			case B_SOUL -> {
				if (sureSoul > 0) {
					sureSoul = 0;
					send(page.soulWhole() ? PuppetBoards.SOUL_WIPE : PuppetBoards.SOUL_RELEASE);
				} else {
					sureSoul = 60;
				}
			}
			case B_MOVE -> send(PuppetBoards.SOUL_MOVE);
			// One's own body's (phase 2d): out of it for good (asked twice), or one's list of puppets.
			case B_LEAVE -> {
				if (sureSoul > 0) {
					sureSoul = 0;
					PuppetSoulScreen.send(com.example.defyingtheheavens.PuppetPlayers.LEAVE);
				} else {
					sureSoul = 60;
				}
			}
			case B_PUPPETS -> PuppetSoulScreen.send(com.example.defyingtheheavens.PuppetPlayers.PUPPETS);
			// Held in another's (phase 2d): the struggle begun, or out of it for good (asked twice).
			case B_STRUGGLE -> PuppetSoulScreen.send(com.example.defyingtheheavens.PuppetPlayers.STRUGGLE);
			case B_LET_GO -> {
				if (sureSoul > 0) {
					sureSoul = 0;
					PuppetSoulScreen.send(com.example.defyingtheheavens.PuppetPlayers.LET_GO);
				} else {
					sureSoul = 60;
				}
			}
			default -> {
			}
		}
	}

	// --- Input ---

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		for (Button b : buttons()) {
			if (button == 0 && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H)) {
				if (b.enabled()) press(b);
				return true;
			}
		}
		for (int p = 0; p < PALETTE_SLOTS; p++) {
			if (button == 0 && in(mx, my, left + PANE_X + p * PALETTE_STEP, top + BOARD_Y, PALETTE_BOX, PALETTE_BOX)) {
				if (page.captive() == null) choose(p);
				return true;
			}
		}
		int i = cellAt(mx, my);
		// A captor's board, before the soul it holds: nothing painted, nothing wiped (its lines go only by the struggle).
		if (i >= 0 && page.captive() != null) {
			said = Component.translatable(ModLang.BOARD_CAPTIVE_PAINT).withStyle(ChatFormatting.RED);
			return true;
		}
		if (i >= 0 && (button == 0 || button == 1)) {
			dragButton = button;
			lastCell = i;
			paint(i, button == 0 ? chosen : CircuitBoard.Stroke.NONE);
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}

	/** The palette's slot {@code p} chosen (its rank allowing): the phase seal, again for its next element. */
	private void choose(int p) {
		CircuitBoard.Stroke stroke = paletteStroke(p);
		if (stroke.rank > page.rank()) return;
		if (p == SEAL_SLOT && chosen.phase() != null) {
			sealPhase = CircuitBoard.Phase.values()[(sealPhase.ordinal() + 1) % CircuitBoard.Phase.values().length];
			stroke = paletteStroke(p);
		}
		chosen = stroke;
		click();
	}

	/** The palette's stroke in slot {@code p}: the nine, the phase seal of the element chosen for it, the link knot. */
	private CircuitBoard.Stroke paletteStroke(int p) {
		return p < PALETTE.length ? PALETTE[p] : p == LINK_SLOT ? CircuitBoard.Stroke.LINK : CircuitBoard.Stroke.seal(sealPhase);
	}

	/** A dull red: the lines of a channel whose seals clash. */
	private static final int CLASH = 0xFFB04848;
	/** A body's own meridians (phase 2b): flesh, and dull where nothing feeds them. */
	private static final int MERIDIAN = 0xFFE8A98C, MERIDIAN_DEAD = 0xFF7A5446;

	/** {@code colour} paler: halfway to white. */
	private static int paler(int colour) {
		int r = (colour >> 16) & 0xFF, gr = (colour >> 8) & 0xFF, b = colour & 0xFF;
		return 0xFF000000 | ((r + 255) / 2) << 16 | ((gr + 255) / 2) << 8 | (b + 255) / 2;
	}

	/** A phase's colour on the board (its element's glow). */
	private static int phaseColour(CircuitBoard.Phase phase) {
		return switch (phase) {
			case METAL -> 0xFFFFD050;
			case WOOD -> 0xFF3CE65A;
			case WATER -> 0xFF3C8CFF;
			case FIRE -> 0xFFFF5A20;
			case EARTH -> 0xFFD99A2B;
		};
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (button == dragButton) {
			int i = cellAt(mx, my);
			if (i >= 0 && i != lastCell) {
				lastCell = i;
				paint(i, button == 0 ? chosen : CircuitBoard.Stroke.NONE);
			}
			return true;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (button == dragButton) {
			dragButton = -1;
			lastCell = -1;
			return true;
		}
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
			choose(key - GLFW.GLFW_KEY_1);
			return true;
		}
		if (key == GLFW.GLFW_KEY_0) {
			choose(SEAL_SLOT);
			return true;
		}
		if (key == GLFW.GLFW_KEY_L) {
			choose(LINK_SLOT);
			return true;
		}
		// P: the path (and the diagram in hand), H: the heat, K: the weak spots, as their buttons.
		int pressed = key == GLFW.GLFW_KEY_P ? B_PATH : key == GLFW.GLFW_KEY_H ? B_HEAT : key == GLFW.GLFW_KEY_K ? B_WEAK : -1;
		if (pressed >= 0) {
			for (Button b : buttons()) if (b.id() == pressed && b.enabled()) press(b);
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	// --- Drawing ---

	@Override
	public void render(GuiGraphics g, int mx, int my, float partialTick) {
		renderBackground(g);
		TalkUi.panel(g, left, top, left + W, top + H, TalkUi.GOLD);
		HallHud.heading(g, font, title, left + 8, top + 6, W - 16);
		List<Component> tip = renderBoard(g, mx, my);
		List<Component> paneTip = renderPane(g, mx, my);
		// What came of the last stroke, or how to paint (a captor's board, before the soul it holds: what it may do there).
		Component foot = said != null ? said : Component.translatable(page.captive() != null ? ModLang.BOARD_CAPTIVE_HINT : ModLang.BOARD_HINT)
				.withStyle(s -> s.withColor(TalkUi.MUTED));
		String text = foot.getString();
		if (font.width(foot) > W - 16) text = font.plainSubstrByWidth(text, W - 16 - font.width("...")) + "...";
		g.drawString(font, Component.literal(text).withStyle(foot.getStyle()), left + 8, top + H - 14, TalkUi.CREAM, false);
		super.render(g, mx, my, partialTick);
		List<Component> shown = tip != null ? tip : paneTip;
		if (shown != null && !shown.isEmpty()) g.renderComponentTooltip(font, shown, mx, my);
	}

	/** The board: its cells, its lines, its nodes, and what is laid over them. @return the tooltip of the cell pointed at */
	private @Nullable List<Component> renderBoard(GuiGraphics g, int mx, int my) {
		int c = cell(), bx = boardLeft(), by = boardTop(), n = board.size, span = c * n;
		HallHud.panel(g, bx - 2, by - 2, bx + span + 2, by + span + 2, HallHud.FRAME, 0xFF100C08);
		if (showHeat && heat == null) heat = board.heat(budget, corePhase);
		if (showWeak && weak == null) weak = board.critical();
		int hovered = cellAt(mx, my);
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				int x0 = bx + x * c, y0 = by + y * c;
				ground(g, x, y, x0, y0, c);
			}
		}
		CircuitBoard.Stroke[] ghost = ghostMode == GHOST_PATH ? path() : ghostMode == GHOST_DIAGRAM ? diagram() : null;
		for (int y = 0; y < n; y++) {
			for (int x = 0; x < n; x++) {
				int i = board.at(x, y), x0 = bx + x * c, y0 = by + y * c;
				CircuitBoard.Stroke s = board.stroke(x, y);
				if (s != CircuitBoard.Stroke.NONE) {
					int colour = s == CircuitBoard.Stroke.RING ? RING : !fed[i] && s.conducts() ? LINE_DEAD : s == CircuitBoard.Stroke.THICK ? LINE_THICK : LINE;
					// A line of a tuned channel in its phase's colour (a thick one paler); a clashing channel's in dull red.
					if (fed[i] && s.conducts() && clashAt[i]) colour = CLASH;
					else if (fed[i] && s.conducts() && phaseAt[i] != null && s != CircuitBoard.Stroke.RING) {
						colour = s == CircuitBoard.Stroke.THICK ? paler(phaseColour(phaseAt[i])) : phaseColour(phaseAt[i]);
					} else if (board.isMeridian(x, y)) {
						colour = fed[i] ? MERIDIAN : MERIDIAN_DEAD; // a body's own meridian (phase 2b), flesh-coloured
					}
					drawStroke(g, s, joins(x, y, null), x0, y0, c, colour);
				} else if (ghost != null && ghost[i] != CircuitBoard.Stroke.NONE && board.isFree(x, y)) {
					// Each ghost stroke in its own colour, seen through (the ring's gold, a thick line's pale blue...); a diagram's strokes above
					// one's rank greyed: they can't be painted yet.
					CircuitBoard.Stroke gs = ghost[i];
					int own = gs == CircuitBoard.Stroke.RING ? RING : gs == CircuitBoard.Stroke.THICK ? LINE_THICK : gs.phase() != null ? phaseColour(gs.phase())
							: gs == CircuitBoard.Stroke.LINE || gs == CircuitBoard.Stroke.DIAGONAL || gs == CircuitBoard.Stroke.BRIDGE ? GHOST : 0xFFFFD54F;
					drawStroke(g, gs, joins(x, y, ghost), x0, y0, c, gs.rank > page.rank() ? GHOST_ABOVE : 0x90000000 | (own & 0xFFFFFF));
				}
				if (ghost != null && ghost[i] != CircuitBoard.Stroke.NONE && board.isFlaw(x, y)) g.fill(x0 + 1, y0 + 1, x0 + c - 1, y0 + c - 1, 0x40FF4040);
				// A broken meridian of a body's (phase 2b): its place marked, to be mended with a plain line.
				if (s == CircuitBoard.Stroke.NONE && board.isMeridian(x, y)) {
					outline(g, x0 + 2, y0 + 2, c - 4, 0x90E8A98C);
					g.fill(x0 + c / 2 - 1, y0 + c / 2 - 1, x0 + c / 2 + 1, y0 + c / 2 + 1, 0xC0E8A98C);
				}
				if (showHeat && heat[i] > 0) g.fill(x0, y0, x0 + c, y0 + c, (Math.min(200, 70 + 45 * heat[i]) << 24) | 0xFF3020);
				if (showWeak && weak[i]) outline(g, x0, y0, c, WEAK);
			}
		}
		for (int cut : page.cuts()) {
			if (cut < 0 || cut >= n * n || board.stroke(cut % n, cut / n) != CircuitBoard.Stroke.NONE) continue;
			int x0 = bx + (cut % n) * c, y0 = by + (cut / n) * c;
			for (int k = 2; k < c - 2; k++) {
				g.fill(x0 + k, y0 + k, x0 + k + 1, y0 + k + 1, CUT);
				g.fill(x0 + c - 1 - k, y0 + k, x0 + c - k, y0 + k + 1, CUT);
			}
		}
		// A captor's board, before the soul it holds (phase 2d): its command line lit violet, cell by cell from the core; the next a struggle
		// cuts filling from the foot as it is cut (pulsing while none is begun).
		Captive captive = page.captive();
		if (captive != null) {
			for (int k = 0; k < captive.line().length; k++) {
				int cell = captive.line()[k];
				if (cell < 0 || cell >= n * n) continue;
				int x0 = bx + (cell % n) * c, y0 = by + (cell / n) * c;
				g.fill(x0 + 1, y0 + 1, x0 + c - 1, y0 + c - 1, 0x40B060FF);
				outline(g, x0, y0, c, 0xC0C080FF);
				if (k > 0) continue;
				if (captive.struggling()) {
					int h = (c - 2) * captive.progress() / 100;
					g.fill(x0 + 1, y0 + c - 1 - h, x0 + c - 1, y0 + c - 1, 0xA0E070FF);
				} else {
					int pulse = (int) (0x50 + 0x60 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 250.0)));
					outline(g, x0 + 1, y0 + 1, c - 2, (pulse << 24) | 0xF0B0FF);
				}
			}
		}
		// The diagram's wear, laid over with it (phase 3: an Overseer Core's record): its cells cut in fights in red, the more the deeper; the
		// cells that ran hot as its puppets overheated ringed in orange.
		if (ghostMode == GHOST_DIAGRAM) wearMap(g, page.wearCuts(), page.wearHeat(), bx, by, c, n);
		// The arrays drawn (phase C), each outlined in its own colour (grey while it doesn't work).
		for (CircuitBoard.Formed f : scores.arrays()) arrayOutline(g, f, bx, by, c, f.fed() ? arrayColour(f.array()) : 0xFF707070);
		for (int k = 0; k < board.nodes().size(); k++) node(g, k, bx, by, c);
		if (hovered < 0) return null;
		outline(g, bx + (hovered % n) * c, by + (hovered / n) * c, c, 0xFFFFE6A0);
		return cellTip(hovered);
	}

	/**
	 * A diagram's wear map (phase 3: Puppetry.Wear, the Overseer Core's) over a board of {@code n} cells a side at ({@code bx}, {@code by}),
	 * {@code c} pixels a cell: each cell cut in fights a red wash, deeper the more it was cut (against the most cut); each that ran hot as its
	 * puppets overheated ringed in orange. Also the Overseer Core's own screen's.
	 */
	static void wearMap(GuiGraphics g, int[] cuts, int[] heat, int bx, int by, int c, int n) {
		int most = 1;
		for (int k : cuts) most = Math.max(most, k);
		for (int i = 0; i < Math.min(n * n, cuts.length); i++) {
			if (cuts[i] <= 0) continue;
			int x0 = bx + (i % n) * c, y0 = by + (i / n) * c, alpha = 70 + 150 * cuts[i] / most;
			g.fill(x0 + 1, y0 + 1, x0 + c - 1, y0 + c - 1, (alpha << 24) | 0xE02828);
		}
		for (int i = 0; i < Math.min(n * n, heat.length); i++) {
			if (heat[i] <= 0) continue;
			outline(g, bx + (i % n) * c + 1, by + (i / n) * c + 1, c - 2, 0xFFFF9A3A);
		}
	}

	/** An array's outline: a line along every edge of its cells that doesn't border another of its cells. */
	private void arrayOutline(GuiGraphics g, CircuitBoard.Formed f, int bx, int by, int c, int colour) {
		java.util.Set<Integer> mine = new java.util.HashSet<>();
		for (int i : f.cells()) mine.add(i);
		int n = board.size;
		for (int i : f.cells()) {
			int x = i % n, y = i / n, x0 = bx + x * c, y0 = by + y * c;
			if (y == 0 || !mine.contains(i - n)) g.fill(x0, y0, x0 + c, y0 + 1, colour);
			if (y == n - 1 || !mine.contains(i + n)) g.fill(x0, y0 + c - 1, x0 + c, y0 + c, colour);
			if (x == 0 || !mine.contains(i - 1)) g.fill(x0, y0, x0 + 1, y0 + c, colour);
			if (x == n - 1 || !mine.contains(i + 1)) g.fill(x0 + c - 1, y0, x0 + c, y0 + c, colour);
		}
	}

	/** Each array's colour on the board (none a line's blue). */
	private static int arrayColour(CircuitBoard.Array array) {
		return switch (array) {
			case EYE -> 0xFFFF7FC8;
			case GUARD_COIL -> 0xFFC8D2DC;
			case GALE_STAIR -> 0xFF8CE68C;
			case GATHERING_SPIRAL -> 0xFFFFD54F;
			case IRON_LOCK -> 0xFFE89A62;
		};
	}

	/** The qi array {@code f} draws: its kind's, and on a Twin Seal's right half the seal's own. */
	private int arrayQi(CircuitBoard.Formed f) {
		return f.array().qi + (f.twin() && board.nodes().get(f.node()).x() > board.size / 2 ? CircuitBoard.TWIN_QI : 0);
	}

	/** The lines telling of array {@code f}: its name and qi, what it does, a Twin Seal, and why it doesn't work if it doesn't. */
	private void arrayLines(List<Component> lines, CircuitBoard.Formed f) {
		lines.add(Component.translatable(ModLang.BOARD_NODE_ARRAY, Component.translatable(ModLang.boardArray(f.array().getId())), arrayQi(f))
				.withStyle(ChatFormatting.YELLOW));
		// What it does, wrapped (a tooltip doesn't wrap by itself).
		String about = Component.translatable(ModLang.boardArrayAbout(f.array().getId())).getString();
		for (net.minecraft.network.chat.FormattedText part : font.getSplitter().splitLines(about, 220, net.minecraft.network.chat.Style.EMPTY)) {
			lines.add(Component.literal("  " + part.getString()).withStyle(ChatFormatting.GRAY));
		}
		if (f.twin()) lines.add(Component.translatable(ModLang.BOARD_NODE_ARRAY_TWIN).withStyle(ChatFormatting.YELLOW));
		if (!f.fed()) {
			boolean reached = !scores.dead()[f.node()];
			if (reached) {
				reached = false;
				for (int i : f.cells()) reached |= fed[i];
			}
			lines.add(Component.translatable(reached ? ModLang.BOARD_NODE_ARRAY_UNFED : ModLang.BOARD_NODE_ARRAY_UNREACHED).withStyle(ChatFormatting.RED));
		}
	}

	/** A cell's ground: lacquer for a free one, brass for a joint, a crack for a flaw, the socket in the middle. */
	private void ground(GuiGraphics g, int x, int y, int x0, int y0, int c) {
		if (board.isJoint(x, y)) {
			g.fill(x0 + 1, y0 + 1, x0 + c - 1, y0 + c - 1, BRASS);
			g.fill(x0 + 1, y0 + 1, x0 + c - 1, y0 + 2, BRASS_LIGHT);
			g.fill(x0 + c / 2 - 1, y0 + c / 2 - 1, x0 + c / 2 + 1, y0 + c / 2 + 1, 0xFF4A3A18);
			return;
		}
		if (board.isFlaw(x, y)) {
			g.fill(x0, y0, x0 + c, y0 + c, 0xFF0A0705);
			for (int k = 1; k < c - 1; k++) g.fill(x0 + k, y0 + (k * 7 / 5) % (c - 2) + 1, x0 + k + 1, y0 + (k * 7 / 5) % (c - 2) + 2, 0xFF5A3A22);
			return;
		}
		if (board.isSocket(x, y)) {
			int glow = page.core().isEmpty() ? 0xFF3A3026 : 0xFF000000 | DemonCoreItem.glow(page.core());
			g.fill(x0, y0, x0 + c, y0 + c, 0xFF241A12);
			g.fill(x0 + 2, y0 + 1, x0 + c - 2, y0 + c - 1, glow);
			g.fill(x0 + 1, y0 + 2, x0 + c - 1, y0 + c - 2, glow);
			outline(g, x0, y0, c, 0xFFD4A63A);
			return;
		}
		g.fill(x0, y0, x0 + c, y0 + c, ((x + y) & 1) == 0 ? GROUND_A : GROUND_B);
		if (board.isOutlet(x, y)) g.fill(x0 + c / 2 - 1, y0 + c / 2 - 1, x0 + c / 2 + 1, y0 + c / 2 + 1, 0xFF2E4A56);
	}

	/** Which ways the stroke on (x, y) joins: a bit each way (1 up, 2 right, 4 down, 8 left; 16.. the diagonals), from {@code over} or the board. */
	private int joins(int x, int y, CircuitBoard.Stroke @Nullable [] over) {
		int[] dx = {0, 1, 0, -1, 1, 1, -1, -1}, dy = {-1, 0, 1, 0, -1, 1, 1, -1};
		CircuitBoard.Stroke here = over != null ? over[board.at(x, y)] : board.stroke(x, y);
		int mask = 0;
		for (int d = 0; d < 8; d++) {
			if (d >= 4 && here != CircuitBoard.Stroke.DIAGONAL) break;
			int nx = x + dx[d], ny = y + dy[d];
			if (!board.inside(nx, ny)) continue;
			if (board.isSocket(nx, ny)) {
				if (d < 4 && board.isOutlet(x, y)) mask |= 1 << d;
				continue;
			}
			if (board.nodeAt(nx, ny) >= 0) {
				if (d < 4) mask |= 1 << d;
				continue;
			}
			CircuitBoard.Stroke there = over != null ? over[board.at(nx, ny)] : board.stroke(nx, ny);
			if (d < 4 ? there.conducts() : there == CircuitBoard.Stroke.DIAGONAL) mask |= 1 << d;
		}
		return mask;
	}

	/** A stroke drawn in its cell: its line to each way it joins, and its own mark (a knot, a vent, a ward...). */
	static void drawStroke(GuiGraphics g, CircuitBoard.Stroke s, int joins, int x0, int y0, int c, int colour) {
		int cx = x0 + c / 2, cy = y0 + c / 2;
		int w = s == CircuitBoard.Stroke.THICK ? Math.max(3, c / 4) : Math.max(2, c / 7);
		int lo = -(w / 2), hi = lo + w;
		// A ghost (a path or a diagram laid over, seen through) has its marks in its own colour too.
		boolean ghost = (colour >>> 24) < 0xFF;
		int gold = ghost ? colour : 0xFFFFD54F;
		switch (s) {
			case VENT -> {
				for (int k = 0; k < 3; k++) {
					int yy = y0 + c / 4 + k * c / 4;
					g.fill(x0 + 2, yy, x0 + c - 2, yy + Math.max(1, c / 9), ghost ? colour : 0xFF9AA7B0);
				}
				return;
			}
			case WARD -> {
				outline(g, x0 + 2, y0 + 2, c - 4, ghost ? colour : 0xFFE0D0FF);
				g.fill(cx - 1, cy - 1, cx + 1, cy + 1, ghost ? colour : 0xFFE0D0FF);
				return;
			}
			case BRIDGE -> {
				// The line up and down whole; the one across broken round it, arching over.
				if ((joins & 5) != 0) g.fill(cx + lo, y0, cx + hi, y0 + c, colour);
				if ((joins & 10) != 0) {
					int gap = Math.max(2, c / 5);
					g.fill(x0, cy + lo, cx - gap, cy + hi, colour);
					g.fill(cx + gap, cy + lo, x0 + c, cy + hi, colour);
					g.fill(cx - gap, cy + lo - gap, cx + gap, cy + hi - gap, colour);
				}
				return;
			}
			default -> {
			}
		}
		g.fill(cx + lo, cy + lo, cx + hi, cy + hi, colour);
		if ((joins & 1) != 0) g.fill(cx + lo, y0, cx + hi, cy, colour);
		if ((joins & 4) != 0) g.fill(cx + lo, cy, cx + hi, y0 + c, colour);
		if ((joins & 8) != 0) g.fill(x0, cy + lo, cx, cy + hi, colour);
		if ((joins & 2) != 0) g.fill(cx, cy + lo, x0 + c, cy + hi, colour);
		// A diagonal stroke's lines to its diagonal fellows, pixel by pixel to the corner.
		int[] ddx = {1, 1, -1, -1}, ddy = {-1, 1, 1, -1};
		for (int d = 0; d < 4; d++) {
			if ((joins & (16 << d)) == 0) continue;
			for (int k = 0; k <= c / 2; k++) g.fill(cx + ddx[d] * k + lo, cy + ddy[d] * k + lo, cx + ddx[d] * k + hi, cy + ddy[d] * k + hi, colour);
		}
		if (s == CircuitBoard.Stroke.DIAGONAL && (joins & 0xF0) == 0) {
			for (int k = -c / 4; k <= c / 4; k++) g.fill(cx + k, cy - k, cx + k + 1, cy - k + 1, colour);
		}
		if (s == CircuitBoard.Stroke.RELAY) {
			int r = Math.max(3, c / 3);
			outline(g, cx - r, cy - r, 2 * r, gold);
		}
		if (s == CircuitBoard.Stroke.GATHER) {
			int r = Math.max(2, c / 4);
			for (int k = -r; k <= r; k++) g.fill(cx - (r - Math.abs(k)), cy + k, cx + (r - Math.abs(k)) + 1, cy + k + 1, gold);
		}
		if (s == CircuitBoard.Stroke.LINK) {
			// A link knot (phase 3): two rings in gold, linked across the line.
			int r = Math.max(2, c / 5), off = Math.max(1, r - 1);
			outline(g, cx - off - r, cy - r, 2 * r, gold);
			outline(g, cx + off - r, cy - r + 1, 2 * r, ghost ? colour : 0xFFE8D27A);
		}
		if (s.phase() != null) {
			// A phase seal: a hollow diamond in white (its line in its element's colour), a ghost's in the ghost's.
			int r = Math.max(2, c / 3), mark = ghost ? colour : 0xFFFFFFFF;
			for (int k = -r; k <= r; k++) {
				int w2 = r - Math.abs(k);
				g.fill(cx - w2, cy + k, cx - w2 + 1, cy + k + 1, mark);
				g.fill(cx + w2, cy + k, cx + w2 + 1, cy + k + 1, mark);
			}
		}
	}

	private static void outline(GuiGraphics g, int x0, int y0, int c, int colour) {
		g.fill(x0, y0, x0 + c, y0 + 1, colour);
		g.fill(x0, y0 + c - 1, x0 + c, y0 + c, colour);
		g.fill(x0, y0, x0 + 1, y0 + c, colour);
		g.fill(x0 + c - 1, y0, x0 + c, y0 + c, colour);
	}

	/** Node {@code k}: a token in its kind's colour, lit while fed, with its letter. */
	private void node(GuiGraphics g, int k, int bx, int by, int c) {
		PuppetBlueprint.Node node = board.nodes().get(k);
		int x0 = bx + node.x() * c, y0 = by + node.y() * c;
		boolean dead = scores.dead()[k];
		int colour = switch (node.kind()) {
			case LIMB -> 0xE0A060;
			case SENSE -> 0xB48CFF;
			case ART -> page.core().isEmpty() ? 0xFF7A50 : DemonCoreItem.glow(page.core());
			case SEAL -> 0xFFD54F;
		};
		g.fill(x0 + 1, y0 + 1, x0 + c - 1, y0 + c - 1, dead ? 0xFF2A2420 : 0xFF000000 | colour);
		outline(g, x0, y0, c, dead ? 0xFF6A5A4A : 0xFFFFFFFF);
		if (c >= 10) {
			String letter = letter(node.id());
			g.drawString(font, letter, x0 + (c - font.width(letter)) / 2 + 1, y0 + (c - 8) / 2 + 1, dead ? 0x8A7A6A : 0x101010, false);
		}
	}

	private static String letter(String id) {
		return switch (id) {
			case "head" -> "H";
			case "mouth" -> "M";
			case "stinger" -> "S";
			case "heart" -> "C";
			case "left_wing", "right_wing" -> "W";
			case "left_arm", "right_arm" -> "A";
			default -> "L";
		};
	}

	/** What the cell {@code i} is, for its tooltip. */
	private List<Component> cellTip(int i) {
		int x = i % board.size, y = i / board.size;
		List<Component> lines = new ArrayList<>();
		int k = board.nodeAt(x, y);
		if (k >= 0) return nodeTip(k);
		if (board.isSocket(x, y)) lines.add(Component.translatable(ModLang.BOARD_SOCKET));
		else if (board.isJoint(x, y)) lines.add(Component.translatable(ModLang.BOARD_JOINT));
		else if (board.isFlaw(x, y)) lines.add(Component.translatable(ModLang.BOARD_FLAW));
		else {
			CircuitBoard.Stroke s = board.stroke(x, y);
			if (s != CircuitBoard.Stroke.NONE) {
				lines.add(Component.translatable(ModLang.boardStroke(id(s))).withStyle(ChatFormatting.AQUA));
				if (s.conducts() && !fed[i]) lines.add(Component.translatable(ModLang.BOARD_UNFED_LINE).withStyle(ChatFormatting.GRAY));
			} else if (contains(page.cuts(), i)) {
				lines.add(Component.translatable(ModLang.BOARD_CUT).withStyle(ChatFormatting.RED));
			}
			if (board.isOutlet(x, y)) lines.add(Component.translatable(ModLang.BOARD_OUTLET).withStyle(ChatFormatting.GRAY));
			if (board.isMeridian(x, y)) lines.add(Component.translatable(ModLang.BOARD_MERIDIAN).withStyle(ChatFormatting.GOLD));
			// A cell of the captor's command line, before the soul it holds (phase 2d): how long it takes to cut from within; the next one, how
			// far it is cut.
			Captive captive = page.captive();
			int on = captive == null ? -1 : captive.indexOf(i);
			if (on >= 0) {
				lines.add(Component.translatable(ModLang.BOARD_COMMAND_LINE).withStyle(ChatFormatting.LIGHT_PURPLE));
				lines.add(Component.translatable(ModLang.BOARD_COMMAND_CELL, captive.seconds()[on]).withStyle(ChatFormatting.GRAY));
				if (board.wardedCells()[i] || s == CircuitBoard.Stroke.WARD) lines.add(Component.translatable(ModLang.BOARD_COMMAND_WARDED).withStyle(ChatFormatting.GOLD));
				if (on == 0) lines.add(captive.struggling() ? Component.translatable(ModLang.BOARD_COMMAND_NEXT, captive.progress()).withStyle(ChatFormatting.LIGHT_PURPLE)
						: Component.translatable(ModLang.BOARD_COMMAND_FIRST).withStyle(ChatFormatting.DARK_PURPLE));
			}
			for (CircuitBoard.Formed f : scores.arrays()) if (contains(f.cells(), i)) arrayLines(lines, f);
			if (showHeat && heat != null && heat[i] > 0) lines.add(Component.translatable(ModLang.BOARD_HEAT_AT, heat[i]).withStyle(ChatFormatting.RED));
			if (showWeak && weak != null && weak[i]) lines.add(Component.translatable(ModLang.BOARD_WEAK_SPOT).withStyle(ChatFormatting.GOLD));
		}
		return lines;
	}

	/** A node's tooltip: its name, fed or dead, its veins and what they make of it, what it draws, whether one cut can cut it off. */
	private List<Component> nodeTip(int k) {
		PuppetBlueprint.Node node = board.nodes().get(k);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable(ModLang.puppetNode(node.id())).withStyle(ChatFormatting.GOLD));
		boolean dead = scores.dead()[k];
		if (node.kind() == PuppetBlueprint.NodeKind.ART && !board.hasArt()) {
			lines.add(Component.translatable(ModLang.BOARD_NODE_NO_ART).withStyle(ChatFormatting.GRAY));
			return lines;
		}
		lines.add(Component.translatable(dead ? ModLang.BOARD_NODE_DEAD : ModLang.BOARD_NODE_FED).withStyle(dead ? ChatFormatting.RED : ChatFormatting.AQUA));
		if (dead) return lines;
		int veins = scores.veins()[k];
		lines.add(Component.translatable(ModLang.BOARD_NODE_VEINS, veins).withStyle(ChatFormatting.AQUA));
		if (scores.drawn()[k] > veins) lines.add(Component.translatable(ModLang.BOARD_NODE_UNFED_VEINS, scores.drawn()[k] - veins).withStyle(ChatFormatting.RED));
		Component power = power(node, veins);
		if (power != null) lines.add(power.copy().withStyle(ChatFormatting.GREEN));
		lines.add(Component.translatable(ModLang.BOARD_NODE_DRAWS, veins * board.load(k)).withStyle(ChatFormatting.GRAY));
		// Its phases (phase B), each with what it does for a part of its kind.
		List<CircuitBoard.Phase> phases = CircuitBoard.Phase.of(scores.phases()[k]);
		if (!phases.isEmpty()) {
			Component names = Component.empty();
			for (int p = 0; p < phases.size(); p++) {
				if (p > 0) names = names.copy().append(", ");
				names = names.copy().append(Puppetry.elementOf(phases.get(p)).getDisplayName());
			}
			lines.add(Component.translatable(ModLang.BOARD_NODE_PHASES, names).withStyle(ChatFormatting.LIGHT_PURPLE));
			String role = role(node);
			for (CircuitBoard.Phase phase : phases) {
				lines.add(Component.literal("  ").append(Component.translatable(ModLang.boardPhaseEffect(role, phase.name().toLowerCase(Locale.ROOT))))
						.withStyle(ChatFormatting.DARK_PURPLE));
			}
		}
		String toughness = !scores.sure()[k] ? ModLang.BOARD_NODE_FRAIL : veins >= 2 ? ModLang.BOARD_NODE_SURE : ModLang.BOARD_NODE_WARDED;
		lines.add(Component.translatable(toughness).withStyle(scores.sure()[k] ? ChatFormatting.DARK_AQUA : ChatFormatting.GOLD));
		// Its array (phase C), if one is drawn by it.
		CircuitBoard.Formed f = scores.arrayOf(k);
		if (f != null) arrayLines(lines, f);
		return lines;
	}

	/** What a phase does for {@code node}: its blows (the arms, a bee's stinger), its art (the mouth), or its body (the rest). */
	private String role(PuppetBlueprint.Node node) {
		boolean bee = page.blueprint() == PuppetBlueprint.BEE;
		if (node.kind() == PuppetBlueprint.NodeKind.ART) return bee ? "sting" : "art";
		if (!bee && node.kind() == PuppetBlueprint.NodeKind.LIMB && node.pair() == 0) return "blows";
		return "body";
	}

	/** What {@code veins} veins make of {@code node} (Puppetry's steps), or null. */
	private @Nullable Component power(PuppetBlueprint.Node node, int veins) {
		boolean bee = page.blueprint() == PuppetBlueprint.BEE;
		return switch (node.kind()) {
			case LIMB -> {
				boolean arms = node.pair() == 0;
				if (bee) yield Component.translatable(ModLang.boardNodePower(arms ? "wing" : "cargo"), times(Puppetry.step(arms ? Puppetry.WING_STEP : Puppetry.CARGO_STEP, veins)));
				yield Component.translatable(ModLang.boardNodePower(arms ? "arm" : "leg"), times(Puppetry.step(arms ? Puppetry.ARM_STEP : Puppetry.LEG_STEP, veins)));
			}
			// (An Eye of Discernment working: farther sight, and a foe taken up at once.)
			case SENSE -> scores.has(CircuitBoard.Array.EYE)
					? Component.translatable(ModLang.boardNodePower("head"), Puppetry.sight(veins) + Puppetry.EYE_SIGHT, "0")
					: Component.translatable(ModLang.boardNodePower("head"), Puppetry.sight(veins), times(Puppetry.step(Puppetry.REACTION_STEP, veins)));
			case ART -> Component.translatable(ModLang.boardNodePower("art"), times(Puppetry.step(Puppetry.ART_STEP, veins)));
			// (An Iron Lock working: no seal seizes it, whatever its veins.)
			case SEAL -> scores.has(CircuitBoard.Array.IRON_LOCK)
					? Component.translatable(ModLang.boardNodePower("heart_locked"), times(Puppetry.step(Puppetry.REACH_STEP, veins)))
					: Component.translatable(ModLang.boardNodePower("heart"), times(Puppetry.step(Puppetry.REACH_STEP, veins)), Math.max(0, veins - 1));
		};
	}

	/** A factor as the tooltips show it: "1.25". */
	private static String times(double factor) {
		return String.format(Locale.ROOT, factor == Math.rint(factor) ? "%.0f" : factor * 10 == Math.rint(factor * 10) ? "%.1f" : "%.2f", factor);
	}

	private static boolean contains(int[] all, int i) {
		for (int a : all) if (a == i) return true;
		return false;
	}

	private static String id(CircuitBoard.Stroke s) {
		return s.name().toLowerCase(Locale.ROOT);
	}

	/** The right side: the strokes, the scores, the nodes, the core and brush, the buttons. @return the tooltip of what is pointed at */
	private @Nullable List<Component> renderPane(GuiGraphics g, int mx, int my) {
		int x = left + PANE_X, y = top + BOARD_Y;
		List<Component> tip = null;
		// The palette: each stroke as it is drawn, the chosen one framed in gold, those above one's rank faint; then the phase seal and the
		// link knot.
		for (int p = 0; p < PALETTE_SLOTS; p++) {
			CircuitBoard.Stroke s = p == SEAL_SLOT && chosen.phase() != null ? chosen : paletteStroke(p);
			int px = x + p * PALETTE_STEP, box = PALETTE_BOX;
			boolean open = s.rank <= page.rank(), on = s == chosen;
			HallHud.panel(g, px, y, px + box, y + box, on ? HallHud.FRAME_LIT : HallHud.FRAME, on ? HallHud.ROW_OPEN : 0xFF100C08);
			int colour = !open ? 0xFF3A3A3A : s == CircuitBoard.Stroke.RING ? RING : s.phase() != null ? phaseColour(s.phase()) : LINE;
			drawStroke(g, s, s == CircuitBoard.Stroke.DIAGONAL ? 16 | 64 : s == CircuitBoard.Stroke.BRIDGE ? 15 : 10, px + 1, y + 1, box - 2, colour);
			if (!open) g.fill(px + 1, y + 1, px + box - 1, y + box - 1, 0x90000000);
			if (in(mx, my, px, y, box, box)) {
				tip = new ArrayList<>();
				tip.add(Component.translatable(ModLang.boardStroke(id(s))).withStyle(ChatFormatting.GOLD));
				tip.add(Component.translatable(ModLang.boardStrokeAbout(id(s))).withStyle(ChatFormatting.GRAY));
				tip.add(Component.translatable(ModLang.BOARD_WEAR, s.wear).withStyle(ChatFormatting.DARK_AQUA));
				if (s.phase() != null) {
					tip.add(Component.translatable(ModLang.PAINT_NEEDS, new ItemStack(Puppetry.tokenItem(s.phase())).getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
					tip.add(Component.translatable(ModLang.BOARD_SEAL_CYCLE).withStyle(ChatFormatting.GRAY));
				}
				if (!open) tip.add(Component.translatable(ModLang.BOARD_NEEDS_RANK, CraftMastery.Rank.values()[s.rank].getDisplayName()).withStyle(ChatFormatting.RED));
			}
		}
		// (A captor's board, before the soul it holds: no stroke of its to paint, the palette dimmed.)
		if (page.captive() != null) g.fill(x, y, x + PALETTE_SLOTS * PALETTE_STEP, y + PALETTE_BOX, 0xA0000000);
		y += 22;
		// The stroke chosen, its wear beside it (what it does is in the palette's tooltip).
		Component chosenName = Component.translatable(ModLang.boardStroke(id(chosen)));
		if (page.captive() == null) {
			g.drawString(font, chosenName, x, y, TalkUi.GOLD_TEXT, false);
			g.drawString(font, Component.translatable(ModLang.BOARD_WEAR, chosen.wear), x + font.width(chosenName) + 8, y, TalkUi.MUTED, false);
		}
		y = top + BOARD_Y + 34;
		// The scores, as far as one's rank reads them.
		for (int k = 0; k < SCORES.length; k++) {
			int value = switch (k) {
				case 0 -> scores.flow();
				case 1 -> scores.stability();
				case 2 -> scores.harmony();
				case 3 -> scores.clarity();
				default -> scores.resilience();
			};
			boolean read = page.rank() >= SCORE_RANK[k];
			Component name = Component.translatable(ModLang.boardScore(SCORES[k]));
			g.drawString(font, name, x, y, read ? TalkUi.CREAM : TalkUi.DISABLED, false);
			int barX = x + 62, barW = PANE_W - 62 - 22;
			g.fill(barX, y + 1, barX + barW, y + 7, 0xFF2A1F14);
			if (read) {
				int colour = value >= 85 ? 0xFFFFD54F : value >= 65 ? 0xFF5FD8FF : value >= 40 ? 0xFF8FD88F : 0xFFE0806A;
				g.fill(barX, y + 1, barX + barW * value / 100, y + 7, colour);
				g.drawString(font, String.valueOf(value), barX + barW + 4, y, TalkUi.CREAM, false);
			} else {
				g.drawString(font, "-", barX + barW + 4, y, TalkUi.DISABLED, false);
			}
			if (in(mx, my, x, y - 1, PANE_W, 10)) {
				tip = new ArrayList<>();
				tip.add(name.copy().withStyle(ChatFormatting.GOLD));
				tip.add(Component.translatable(ModLang.boardScoreAbout(SCORES[k])).withStyle(ChatFormatting.GRAY));
				if (!read) tip.add(Component.translatable(ModLang.BOARD_SCORE_LOCKED, CraftMastery.Rank.values()[SCORE_RANK[k]].getDisplayName())
						.withStyle(ChatFormatting.RED));
			}
			y += 10;
		}
		// Its grade: read as one paints from Grandmaster on; told on sealing before that (a sealed puppet's is known).
		if (page.rank() >= 3 || page.sealed()) {
			Component grade = Component.translatable(ModLang.circuitGrade(scores.grade().name().toLowerCase(Locale.ROOT)));
			g.drawString(font, Component.translatable(ModLang.BOARD_GRADE, grade), x, y + 1, TalkUi.GOLD_TEXT, false);
			y += 11;
		} else {
			for (FormattedCharSequence line : wrap(Component.translatable(ModLang.BOARD_GRADE_HIDDEN), 2)) {
				g.drawString(font, line, x, y + 1, TalkUi.MUTED, false);
				y += 9;
			}
			y += 2;
		}
		// The qi its veins want of what its core gives: over it, it won't seal.
		Component qiLine = Component.translatable(ModLang.BOARD_QI, scores.wanted(), scores.budget());
		boolean over = scores.over();
		g.drawString(font, qiLine, x, y + 1, over ? TalkUi.WARNING : TalkUi.CREAM, false);
		int qx = x + font.width(qiLine) + 6, qw = Math.max(10, PANE_W - (qx - x) - 2);
		g.fill(qx, y + 2, qx + qw, y + 8, 0xFF2A1F14);
		g.fill(qx, y + 2, qx + qw * Math.min(scores.wanted(), scores.budget()) / Math.max(1, scores.budget()), y + 8, over ? 0xFFE0806A : 0xFF5FD8FF);
		if (in(mx, my, x, y, PANE_W, 10)) {
			tip = new ArrayList<>();
			tip.add(qiLine.copy().withStyle(ChatFormatting.GOLD));
			tip.add(Component.translatable(ModLang.BOARD_QI_ABOUT).withStyle(ChatFormatting.GRAY));
			if (scores.phaseQi() != 0) {
				tip.add(Component.translatable(ModLang.BOARD_QI_PHASES, (scores.phaseQi() > 0 ? "+" : "") + scores.phaseQi())
						.withStyle(scores.phaseQi() > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
			}
			int byArrays = 0;
			for (CircuitBoard.Formed f : scores.arrays()) if (f.fed()) byArrays += arrayQi(f);
			if (byArrays > 0) tip.add(Component.translatable(ModLang.BOARD_QI_ARRAYS, byArrays).withStyle(ChatFormatting.YELLOW));
			if (over) tip.add(Component.translatable(ModLang.BOARD_QI_OVER, scores.wanted()).withStyle(ChatFormatting.RED));
		}
		y += 11;
		// The nodes, fed or dead: how many, then each a lettered chip (its name, veins and power pointed at).
		int fedCount = 0;
		for (boolean d : scores.dead()) if (!d) fedCount++;
		Component nodesLine = Component.translatable(ModLang.BOARD_NODES, fedCount, scores.dead().length);
		g.drawString(font, nodesLine, x, y + 1, fedCount == scores.dead().length ? TalkUi.CREAM : TalkUi.WARNING, false);
		List<PuppetBlueprint.Node> nodes = board.nodes();
		int chipX = x + font.width(nodesLine) + 6;
		for (int k = 0; k < nodes.size(); k++) {
			int nx = chipX + k * 13;
			boolean dead = scores.dead()[k];
			g.fill(nx, y, nx + 11, y + 10, dead ? 0xFF4A2420 : 0xFF1E4A5A);
			outline(g, nx, y, 11, dead ? 0xFFC4473A : 0xFF5FD8FF);
			String letter = letter(nodes.get(k).id());
			g.drawString(font, letter, nx + (11 - font.width(letter)) / 2 + 1, y + 2, dead ? 0xFFE08070 : 0xFFE0F8FF, false);
			if (in(mx, my, nx, y, 11, 10)) tip = nodeTip(k);
		}
		y += 14;
		// The core, the brush, and at a bench what sealing takes.
		for (FormattedCharSequence line : wrap(page.coreLine(), 2)) {
			g.drawString(font, line, x, y, page.core().isEmpty() ? TalkUi.WARNING : TalkUi.SECT_TEXT, false);
			y += 9;
		}
		// (One's own body, a free soul's: painted with its own qi, no brush wanted. A captor's: not to be painted at all.)
		Component brush = page.captive() != null ? Component.translatable(ModLang.BOARD_CAPTIVE_BRUSH)
				: page.creative() ? Component.translatable(ModLang.BOARD_BRUSH_CREATIVE) : page.ownBody() ? Component.translatable(ModLang.BOARD_OWN_QI)
				: brushLeft < 0 ? Component.translatable(ModLang.BOARD_NO_BRUSH) : Component.translatable(ModLang.BOARD_BRUSH, brushLeft);
		g.drawString(font, brush, x, y, page.captive() != null ? 0xFFD6A8FF : brushLeft < 0 && !page.creative() && !page.ownBody() ? TalkUi.WARNING
				: TalkUi.MUTED, false);
		y += 9;
		if (!page.sealed()) {
			// A body still refining on the bench (phase 2b): how far, in sealing's place (it won't seal yet).
			if (page.refining() >= 0 && page.refining() < 100) {
				g.drawString(font, Component.translatable(ModLang.BOARD_REFINING, page.refining()), x, y, TalkUi.WARNING, false);
				y += 9;
			} else {
				Component cost = Component.translatable(ModLang.BOARD_SEAL_COST, page.stones().getCount(), page.stones().getHoverName(), page.used(),
						page.slots(), page.cost());
				for (FormattedCharSequence line : wrap(cost, 2)) {
					g.drawString(font, line, x, y, page.haveStones() && page.used() + page.cost() <= page.slots() ? TalkUi.MUTED : TalkUi.WARNING, false);
					y += 9;
				}
			}
		} else {
			// Its soul (phase 2a): hollow (under the sealed note), or a real soul and how far its copy has come, or a whole copy, in the note's
			// place with what can be done with it by a bench in a row of its own (buttons()); pointed at, what it gives.
			if (page.soulKind() < 0) {
				g.drawString(font, Component.translatable(ModLang.BOARD_SEALED_NOTE), x, y, TalkUi.MUTED, false);
				y += 9;
			}
			Component soulLine = soulLine();
			g.drawString(font, soulLine, x, y, page.soulKind() < 0 ? TalkUi.MUTED : page.soulWhole() ? TalkUi.SECT_TEXT : 0xFFD6A8FF, false);
			if (in(mx, my, x, y - 1, PANE_W, 10)) tip = soulTip();
			soulRowY = page.soulKind() < 0 ? -1 : y + 10;
		}
		// The buttons.
		for (Button b : buttons()) {
			boolean hover = b.enabled() && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H);
			if (b.on()) {
				HallHud.panel(g, b.x(), b.y(), b.x() + b.w(), b.y() + BUTTON_H, HallHud.FRAME_LIT, HallHud.ROW_OPEN);
				g.drawString(font, b.label(), b.x() + (b.w() - font.width(b.label())) / 2, b.y() + 3, TalkUi.GOLD_TEXT, false);
			} else {
				HallHud.button(g, font, b.x(), b.y(), b.w(), BUTTON_H, b.label(), b.enabled(), hover);
			}
			if (b.id() == B_PATH && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H)) {
				// The build shown (or the one the next press shows), and what it is for.
				CircuitBoard.Build shown = ghostMode == GHOST_PATH ? shownBuild() : builds().get(0);
				tip = ghostMode == GHOST_DIAGRAM ? page.wearCuts().length > 0
						? List.of(Component.translatable(ModLang.BOARD_DIAGRAM_ABOVE).withStyle(ChatFormatting.GOLD),
								Component.translatable(ModLang.BOARD_WEAR_KEY).withStyle(ChatFormatting.RED))
						: List.of(Component.translatable(ModLang.BOARD_DIAGRAM_ABOVE).withStyle(ChatFormatting.GOLD))
						: List.of(Component.translatable(ModLang.BOARD_PATH_BUILD, Component.translatable(ModLang.pathName(page.rank())),
								Component.translatable(ModLang.pathBuild(shown.getId()))).withStyle(ChatFormatting.GOLD),
								Component.translatable(ModLang.pathBuildAbout(shown.getId())).withStyle(ChatFormatting.GRAY));
			}
			if (b.id() == B_COPY && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H)) {
				tip = List.of(Component.translatable(ModLang.BOARD_COPY_NEEDS).withStyle(ChatFormatting.GRAY));
			}
			// A soul button that can't act yet: why.
			if ((b.id() == B_SOUL || b.id() == B_MOVE) && !b.enabled() && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H)) {
				tip = List.of(Component.translatable(page.atBench() ? ModLang.BOARD_SOUL_NO_RING : ModLang.BOARD_SOUL_AT_BENCH).withStyle(ChatFormatting.GRAY));
			}
			// A held soul's (phase 2d): what struggling does (or why it can't), what letting go does.
			if (b.id() == B_STRUGGLE && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H) && page.captive() != null) {
				Captive captive = page.captive();
				tip = tipLines(captive.mayStruggle() ? Component.translatable(ModLang.PUPPET_SOUL_STRUGGLE_TIP)
						: Component.translatable(ModLang.PUPPET_SOUL_CANT_STRUGGLE, captive.realm()), captive.mayStruggle() ? ChatFormatting.GRAY : ChatFormatting.RED);
			}
			if (b.id() == B_LET_GO && in(mx, my, b.x(), b.y(), b.w(), BUTTON_H)) {
				tip = tipLines(Component.translatable(ModLang.PUPPET_SOUL_LET_GO_TIP), ChatFormatting.GRAY);
			}
		}
		return tip;
	}

	/** "Soul: hollow", "Soul: Lin Feng (a cultivator), copied 37%", "Soul: a copy of a Wolf (a beast)". */
	private Component soulLine() {
		if (page.captive() != null) return Component.translatable(ModLang.BOARD_SOUL_CAPTIVE, page.captive().captor());
		if (page.ownBody()) return Component.translatable(ModLang.BOARD_SOUL_OWN);
		if (page.soulKind() < 0) return Component.translatable(ModLang.BOARD_SOUL_HOLLOW);
		Component kind = Component.translatable(ModLang.soulKind(soulKindId()));
		return page.soulWhole() ? Component.translatable(ModLang.BOARD_SOUL_COPY, page.soulName(), kind)
				: Component.translatable(ModLang.BOARD_SOUL_REAL, page.soulName(), kind, page.soulCopied());
	}

	private String soulKindId() {
		PuppetSoul.Kind[] kinds = PuppetSoul.Kind.values();
		return kinds[Math.max(0, Math.min(kinds.length - 1, page.soulKind()))].getId();
	}

	/** The soul line's tooltip: its ring's ways, what this soul gives, a cultivator's arts. */
	private List<Component> soulTip() {
		List<Component> lines = new ArrayList<>();
		lines.add(soulLine().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
		// Held in another's (phase 2d): how far the copy of it has come, its struggle (or why it can't), and the command line it struggles against.
		Captive captive = page.captive();
		if (captive != null) {
			lines.add(Component.translatable(ModLang.SOUL_SCREEN_COPIED, captive.copied()).withStyle(ChatFormatting.WHITE));
			if (captive.struggling()) {
				lines.add(Component.translatable(ModLang.PUPPET_SOUL_STRUGGLING, captive.seconds().length > 0 ? captive.seconds()[0] : 0)
						.withStyle(ChatFormatting.GOLD));
			} else if (!captive.mayStruggle()) {
				lines.addAll(tipLines(Component.translatable(ModLang.PUPPET_SOUL_CANT_STRUGGLE, captive.realm()), ChatFormatting.RED));
			}
			lines.addAll(tipLines(Component.translatable(ModLang.BOARD_CAPTIVE_ABOUT), ChatFormatting.GRAY));
			return lines;
		}
		// One's own body (phase 2d): painted with one's own qi; Leave it.
		if (page.ownBody()) {
			for (String key : new String[] {ModLang.PUPPET_SOUL_BOARD_TIP, ModLang.PUPPET_SOUL_LEAVE_TIP}) {
				for (net.minecraft.network.chat.FormattedText line : font.getSplitter().splitLines(Component.translatable(key).getString(), 220,
						net.minecraft.network.chat.Style.EMPTY)) {
					lines.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
				}
			}
			return lines;
		}
		List<String> parts = new ArrayList<>();
		if (page.soulKind() >= 0) parts.add(Component.translatable(ModLang.soulGives(soulKindId())).getString());
		parts.add(Component.translatable(ModLang.BOARD_SOUL_ABOUT).getString());
		for (String part : parts) {
			for (net.minecraft.network.chat.FormattedText line : font.getSplitter().splitLines(part, 220, net.minecraft.network.chat.Style.EMPTY)) {
				lines.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
			}
		}
		if (!page.soulArts().isEmpty()) {
			Component arts = Component.empty();
			for (int k = 0; k < page.soulArts().size(); k++) {
				if (k > 0) arts = arts.copy().append(", ");
				try {
					arts = arts.copy().append(Spell.valueOf(page.soulArts().get(k)).getDisplayName());
				} catch (IllegalArgumentException e) {
					arts = arts.copy().append(page.soulArts().get(k));
				}
			}
			lines.add(Component.translatable(ModLang.BOARD_SOUL_ARTS, arts).withStyle(ChatFormatting.YELLOW));
		}
		return lines;
	}

	/** {@code text} wrapped for a tooltip, each line in {@code style}. */
	private List<Component> tipLines(Component text, ChatFormatting style) {
		List<Component> lines = new ArrayList<>();
		for (net.minecraft.network.chat.FormattedText line : font.getSplitter().splitLines(text.getString(), 220, net.minecraft.network.chat.Style.EMPTY)) {
			lines.add(Component.literal(line.getString()).withStyle(style));
		}
		return lines;
	}

	/** {@code text} wrapped to the pane, at most {@code most} lines. */
	private List<FormattedCharSequence> wrap(Component text, int most) {
		List<FormattedCharSequence> lines = font.split(text, PANE_W);
		return lines.size() > most ? lines.subList(0, most) : lines;
	}
}
