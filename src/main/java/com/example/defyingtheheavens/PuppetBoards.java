package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A puppet's circuit board open in its puppeteer's hands (Puppetry; the client's PuppetBoardScreen): a frame's on a Puppet Workbench, or a
 * sealed puppet's own (its owner right-clicking it with the Inscription Brush: mending it in the field). The server holds the board; the
 * screen paints on its own copy at once and sends each stroke (C2S puppet_paint), and the page (S2C puppet_board) comes back with what the
 * board now is.
 * <p>
 * A stroke wears the brush by its wear (a thick line over a plain one only one more); a cooling vent takes an iron ingot besides, a
 * gathering knot a spirit stone of the frame's grade. Each stroke is the puppeteer's rank's or below (CircuitBoard.Stroke#rank). Wiping costs
 * nothing. A cut line of a puppet's (PuppetEntity#cut) painted again teaches a little Puppetry. Sealing (at the bench) wants the core in,
 * the heart of command fed, no more veins than the core's qi feeds (Puppetry#budget), a slot free and {@link Puppetry#SEAL_STONES} stones of
 * the frame's grade: the puppet stands up from the bench.
 */
public final class PuppetBoards {
	/**
	 * The acts of C2S puppet_act: seal the bench's puppet, wipe the board, the screen closed, copy the board onto paper (a Circuit Diagram); a
	 * puppet's soul let go, moved into another of its puppeteer's puppets, or its whole copy wiped (phase 2a: PuppetSouls, by a bench).
	 */
	public static final int SEAL = 0, WIPE_ALL = 1, CLOSE = 2, COPY = 3, SOUL_RELEASE = 4, SOUL_MOVE = 5, SOUL_WIPE = 6;
	/** The brush's wear a diagram's copying takes (and a sheet of paper). */
	public static final int COPY_WEAR = 4;
	/** How near the bench or the puppet its puppeteer must stand. */
	private static final double NEAR = 6;

	/** What a puppeteer has open: a bench's frame (where), or a puppet's own board (which). */
	private record Open(ResourceKey<Level> dimension, @Nullable BlockPos bench, @Nullable UUID puppet) {}

	private static final Map<UUID, Open> OPEN = new HashMap<>();

	/** A board open: where it is kept, and what goes with it. */
	private interface Target {
		CircuitBoard board();
		/** Keeps {@code board}'s drawing where it belongs. */
		void store(CircuitBoard board);
		Component title();
		PuppetBlueprint blueprint();
		ArtifactGrade grade();
		int flaws();
		long seed();
		ItemStack core();
		/** A sealed puppet's board (no sealing, its cuts shown). */
		boolean sealed();
		int[] cuts();
		/** Whether {@code cell} was cut and is now painted again (taken off the cuts). */
		boolean mend(int cell);
		Vec3 where();
		/** A body's meridians (phase 2b), or null for a frame of matter. */
		boolean @Nullable [] meridians();
	}

	private record BenchTarget(PuppetWorkbenchBlockEntity bench) implements Target {
		PuppetFrameItem frame() { return (PuppetFrameItem) bench.frame().getItem(); }
		public CircuitBoard board() { return PuppetFrameItem.board(bench.frame()); }
		public void store(CircuitBoard board) {
			PuppetFrameItem.keep(bench.frame(), board);
			bench.boardChanged();
		}
		public Component title() { return bench.frame().getHoverName(); }
		public PuppetBlueprint blueprint() { return frame().blueprint(); }
		public ArtifactGrade grade() { return frame().grade(); }
		public int flaws() { return PuppetFrameItem.flaws(bench.frame()); }
		public long seed() { return PuppetFrameItem.seed(bench.frame()); }
		public ItemStack core() { return bench.core(); }
		public boolean sealed() { return false; }
		public int[] cuts() { return new int[0]; }
		public boolean mend(int cell) { return false; }
		public Vec3 where() { return Vec3.atCenterOf(bench.getBlockPos()); }
		public boolean @Nullable [] meridians() { return PuppetBodies.meridians(bench.frame()); }
	}

	private record PuppetTarget(PuppetEntity puppet) implements Target {
		public CircuitBoard board() { return puppet.board(); }
		public void store(CircuitBoard board) { puppet.boardChanged(board); }
		public Component title() { return puppet.getDisplayName(); }
		public PuppetBlueprint blueprint() { return puppet.blueprint(); }
		public ArtifactGrade grade() { return puppet.frameGrade(); }
		public int flaws() { return CircuitBoard.flawsFor(puppet.frameQuality().ordinal()); }
		public long seed() { return puppet.seed(); }
		public ItemStack core() { return puppet.core(); }
		public boolean sealed() { return true; }
		public int[] cuts() { return puppet.cuts(); }
		public boolean mend(int cell) { return puppet.mend(cell); }
		public Vec3 where() { return puppet.position(); }
		public boolean @Nullable [] meridians() { return puppet.meridians(); }
	}

	/** {@code player} opens the frame on {@code bench}. */
	public static void openBench(ServerPlayer player, PuppetWorkbenchBlockEntity bench) {
		OPEN.put(player.getUUID(), new Open(player.level().dimension(), bench.getBlockPos().immutable(), null));
		send(player, 0, null);
	}

	/** {@code player} opens their puppet's own board (with the brush in hand: mending it where it stands). */
	public static void openPuppet(ServerPlayer player, PuppetEntity puppet) {
		OPEN.put(player.getUUID(), new Open(player.level().dimension(), null, puppet.getUUID()));
		send(player, 0, null);
	}

	/** What {@code player} has open, if it is still there and near enough. */
	private static @Nullable Target target(ServerPlayer player) {
		Open open = OPEN.get(player.getUUID());
		if (open == null || player.level().dimension() != open.dimension() || !player.isAlive()) return null;
		ServerLevel level = player.serverLevel();
		Target target = null;
		if (open.bench() != null) {
			if (level.isLoaded(open.bench()) && level.getBlockEntity(open.bench()) instanceof PuppetWorkbenchBlockEntity bench
					&& bench.frame().getItem() instanceof PuppetFrameItem) {
				target = new BenchTarget(bench);
			}
		} else if (open.puppet() != null && level.getEntity(open.puppet()) instanceof PuppetEntity puppet && puppet.isAlive()
				&& (puppet.isOwnedBy(player) || PuppetPlayers.heldWithin(player, puppet))) {
			target = new PuppetTarget(puppet);
		}
		if (target == null || player.position().distanceTo(target.where()) > NEAR + 2) return null;
		return target;
	}

	/**
	 * Whether {@code t} is open before a soul held in it, another's puppet (phase 2d; the user, 2026-11-18: "a non-free puppet should see
	 * circuits too so they can takeover/struggle free"): theirs to look at, never to paint, wipe or copy; its lines theirs to struggle against.
	 */
	private static boolean captive(ServerPlayer player, Target t) {
		return t instanceof PuppetTarget pt && !pt.puppet().isOwnedBy(player);
	}

	/** {@code player}'s board sent again as it stands, if they have one open (a held soul's, each second: its struggle). */
	public static void refresh(ServerPlayer player) {
		if (OPEN.containsKey(player.getUUID())) send(player, 0, null);
	}

	/** {@code player}'s brushes' strokes left in all; -1 with none. */
	private static int brushLeft(ServerPlayer player) {
		int left = -1;
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(ModItems.INSCRIPTION_BRUSH)) left = Math.max(0, left) + stack.getMaxDamage() - stack.getDamageValue();
		}
		return left;
	}

	/** The brush a stroke of {@code wear} is painted with: the one in hand, else the first in the pack with as much left. */
	private static ItemStack brushFor(ServerPlayer player, int wear) {
		for (ItemStack held : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
			if (held.is(ModItems.INSCRIPTION_BRUSH) && held.getMaxDamage() - held.getDamageValue() >= wear) return held;
		}
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.is(ModItems.INSCRIPTION_BRUSH) && stack.getMaxDamage() - stack.getDamageValue() >= wear) return stack;
		}
		return ItemStack.EMPTY;
	}

	/** Takes {@code count} of {@code item} from {@code player}'s pack; whether they had it (nothing taken if not). */
	static boolean take(ServerPlayer player, Item item, int count) {
		Inventory inv = player.getInventory();
		if (inv.countItem(item) < count) return false;
		for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
			ItemStack stack = inv.getItem(i);
			if (!stack.is(item)) continue;
			int taken = Math.min(count, stack.getCount());
			stack.shrink(taken);
			count -= taken;
		}
		return true;
	}

	/** Takes one of whatever in {@code player}'s pack {@code test} accepts; whether there was one. */
	static boolean takeOne(ServerPlayer player, java.util.function.Predicate<ItemStack> test) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.isEmpty() || !test.test(stack)) continue;
			stack.shrink(1);
			return true;
		}
		return false;
	}

	/** The page of what {@code player} has open (or, with nothing open any more, the word to close it), after stroke {@code seq}. */
	static void send(ServerPlayer player, int seq, @Nullable Component said) {
		Target t = target(player);
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(seq);
		if (t == null) {
			OPEN.remove(player.getUUID());
			buf.writeBoolean(false);
			ServerPlayNetworking.send(player, ModPackets.PUPPET_BOARD, buf);
			return;
		}
		buf.writeBoolean(true);
		buf.writeComponent(t.title());
		buf.writeUtf(t.blueprint().getId());
		buf.writeVarInt(t.grade().ordinal());
		buf.writeVarInt(t.flaws());
		buf.writeLong(t.seed());
		CircuitBoard board = t.board();
		buf.writeByteArray(Puppetry.strokes(board));
		buf.writeVarIntArray(t.cuts());
		PlayerCultivation own = CultivationManager.own(player);
		buf.writeVarInt(Puppetry.rank(own).ordinal());
		buf.writeBoolean(t.sealed());
		ItemStack core = t.core();
		buf.writeItem(core);
		buf.writeComponent(coreLine(t));
		buf.writeVarInt(brushLeft(player));
		buf.writeBoolean(player.getAbilities().instabuild);
		// A Circuit Diagram of this make and grade in hand: laid over the board on the screen (empty: none).
		ItemStack diagram = CircuitDiagramItem.fits(player.getMainHandItem(), t.blueprint(), t.grade()) ? player.getMainHandItem()
				: CircuitDiagramItem.fits(player.getOffhandItem(), t.blueprint(), t.grade()) ? player.getOffhandItem() : ItemStack.EMPTY;
		buf.writeByteArray(diagram.isEmpty() ? new byte[0] : CircuitDiagramItem.strokes(diagram));
		// Its wear, as an Overseer Core wrote it on the diagram (phase 3): where its puppets' lines were cut, and ran hot, in fights.
		Puppetry.Wear wear = diagram.isEmpty() ? null : CircuitDiagramItem.wear(diagram);
		buf.writeVarIntArray(wear == null ? new int[0] : wear.cuts);
		buf.writeVarIntArray(wear == null ? new int[0] : wear.heat);
		Item stone = t.grade().stone();
		buf.writeItem(new ItemStack(stone, Puppetry.SEAL_STONES));
		buf.writeBoolean(player.getAbilities().instabuild || player.getInventory().countItem(stone) >= Puppetry.SEAL_STONES);
		buf.writeVarInt(Puppetry.Ledger.get(player.server).used(player.getUUID()));
		buf.writeVarInt(Puppetry.slots(own));
		buf.writeVarInt(Puppetry.cost(t.grade()));
		// A puppet's soul (phase 2a): its kind (-1: hollow, or a frame on a bench), its name, how far copied, whether whole, its arts; whether
		// the puppet stands by a bench, and whether another of its puppeteer's puppets there has an empty ring to move the soul into.
		PuppetEntity puppet = t instanceof PuppetTarget pt ? pt.puppet() : null;
		PuppetSoul soul = puppet == null ? null : puppet.soul();
		buf.writeVarInt(soul == null ? -1 : soul.kind.ordinal());
		if (soul != null) {
			buf.writeUtf(soul.goesBy()); // a mind grown of its own goes by the name it took (phase 2c)
			buf.writeVarInt(soul.copiedPercent());
			buf.writeBoolean(soul.whole());
			buf.writeVarInt(soul.arts.size());
			for (Spell spell : soul.arts) buf.writeUtf(spell.name());
		}
		BlockPos bench = puppet == null ? null : PuppetSouls.benchNear(puppet);
		buf.writeBoolean(bench != null);
		buf.writeBoolean(bench != null && soul != null && soul.real() && PuppetSouls.moveTarget(player, puppet, bench) != null);
		// A body's meridians (phase 2b: empty for a frame of matter), and on a bench how far it has refined (-1: a frame, or refined).
		boolean[] meridians = t.meridians();
		buf.writeByteArray(meridians == null ? new byte[0] : PuppetBodies.bytes(meridians));
		buf.writeVarInt(t instanceof BenchTarget bt && PuppetBodies.isBody(bt.bench().frame())
				? 100 * PuppetBodies.refined(bt.bench().frame(), player.serverLevel().getGameTime()) / PuppetBodies.REFINE_TICKS : -1);
		// Its own body's, a free soul playing it (phase 2d: the menu's first tab): its soul's row holds Leave it and its puppets.
		buf.writeBoolean(puppet != null && PuppetPlayers.paintsOwnBody(player, puppet));
		// Another's, a soul held in it (the menu's first tab too): its captor, its copy, its struggle and the command line it cuts.
		boolean captive = captive(player, t);
		buf.writeBoolean(captive);
		if (captive) PuppetPlayers.writeCaptive(buf, player, puppet);
		buf.writeBoolean(said != null);
		if (said != null) buf.writeComponent(said);
		ServerPlayNetworking.send(player, ModPackets.PUPPET_BOARD, buf);
	}

	/**
	 * {@link #coreLine(ArtifactGrade, ItemStack, boolean)}, or for a body of flesh (phase 2b): its puppet stands at the dead's own rank, in the dead's
	 * element (the core's if they had none), with a core of at least the dead's realm.
	 */
	private static Component coreLine(Target t) {
		PuppetBodies.Dead dead = t instanceof BenchTarget bt ? PuppetBodies.deadOf(bt.bench().frame()) : t instanceof PuppetTarget pt ? pt.puppet().dead() : null;
		ItemStack core = t.core();
		boolean live = t instanceof PuppetTarget;
		if (dead == null) return coreLine(t.grade(), core, live);
		int rank = Math.max(0, dead.rank());
		Component realm = CultivatorNpc.realmOf(rank).getDisplayName();
		if (core.isEmpty()) return Component.translatable(ModLang.BODY_WANTS_CORE, realm);
		if (!PuppetBodies.coreFits(dead, core)) return Component.translatable(ModLang.BODY_CORE_TOO_WEAK, realm);
		Component stands = PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
		return stands(live, stands, dead.element() != null ? dead.element() : DemonCoreItem.element(core));
	}

	/** "Its puppet will stand at Core Formation Mid; fire" for the core set in it, or what is wanted. */
	private static Component coreLine(ArtifactGrade grade, ItemStack core, boolean live) {
		if (core.isEmpty()) return Component.translatable(ModLang.BOARD_NO_CORE, Puppetry.lowest(grade).getDisplayName());
		int rank = Puppetry.puppetRank(grade, DemonCoreItem.rank(core));
		if (rank < 0) return Component.translatable(ModLang.BENCH_CORE_TOO_WEAK, Puppetry.lowest(grade).getDisplayName());
		Component stands = PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
		return stands(live, stands, DemonCoreItem.element(core));
	}

	/** Where its puppet will stand (a frame on the bench), or where it stands (a live puppet's board: 2026-11-18), and its art's element. */
	private static Component stands(boolean live, Component stands, @Nullable QiElement element) {
		if (element == null) return Component.translatable(live ? ModLang.BOARD_CORE_LIVE : ModLang.BOARD_CORE, stands);
		return Component.translatable(live ? ModLang.BOARD_CORE_ELEMENT_LIVE : ModLang.BOARD_CORE_ELEMENT, stands, element.getDisplayName());
	}

	/** C2S puppet_paint: stroke {@code seq}, {@code stroke} (its order; NONE wipes) on ({@code x}, {@code y}). */
	public static void paint(ServerPlayer player, int seq, int x, int y, int strokeIndex) {
		Target t = target(player);
		if (t == null) {
			send(player, seq, null);
			return;
		}
		if (captive(player, t)) {
			send(player, seq, Component.translatable(ModLang.BOARD_CAPTIVE_PAINT).withStyle(ChatFormatting.RED));
			return;
		}
		CircuitBoard.Stroke[] strokes = CircuitBoard.Stroke.values();
		if (strokeIndex < 0 || strokeIndex >= strokes.length) return;
		CircuitBoard.Stroke stroke = strokes[strokeIndex];
		CircuitBoard board = t.board();
		if (!board.inside(x, y)) return;
		CraftMastery.Rank rank = Puppetry.rank(CultivationManager.own(player));
		if (stroke.rank > rank.ordinal()) {
			send(player, seq, Component.translatable(ModLang.PAINT_RANK, CraftMastery.Rank.values()[stroke.rank].getDisplayName()).withStyle(ChatFormatting.RED));
			return;
		}
		String refusal = board.refusal(x, y, stroke);
		if (refusal != null) {
			// Painting over the same stroke (a drag over what is drawn) is nothing to tell of.
			send(player, seq, refusal.equals("same") || refusal.equals("empty") ? null
					: Component.translatable(ModLang.boardRefusal(refusal)).withStyle(ChatFormatting.RED));
			return;
		}
		boolean creative = player.getAbilities().instabuild;
		// A soul playing its own puppet paints its body's lines with its own qi: no brush (phase 2d: PuppetPlayers).
		boolean ownQi = t instanceof PuppetTarget pt && PuppetPlayers.paintsOwnBody(player, pt.puppet());
		if (stroke != CircuitBoard.Stroke.NONE && !creative) {
			int wear = stroke == CircuitBoard.Stroke.THICK && board.stroke(x, y) == CircuitBoard.Stroke.LINE ? 1 : stroke.wear;
			ItemStack brush = ownQi ? ItemStack.EMPTY : brushFor(player, wear);
			if (brush.isEmpty() && !ownQi) {
				send(player, seq, Component.translatable(ModLang.PAINT_NO_BRUSH).withStyle(ChatFormatting.RED));
				return;
			}
			Item matter = stroke == CircuitBoard.Stroke.VENT ? Items.IRON_INGOT : stroke == CircuitBoard.Stroke.GATHER ? t.grade().stone() : null;
			if (matter != null && !take(player, matter, 1)) {
				send(player, seq, Component.translatable(ModLang.PAINT_NEEDS, new ItemStack(matter).getHoverName()).withStyle(ChatFormatting.RED));
				return;
			}
			// A phase seal takes its element's token, as the forge's banners do (any sapling for Wood).
			CircuitBoard.Phase phase = stroke.phase();
			if (phase != null && !takeOne(player, s -> Puppetry.sealToken(s, phase))) {
				send(player, seq, Component.translatable(ModLang.PAINT_NEEDS, new ItemStack(Puppetry.tokenItem(phase)).getHoverName()).withStyle(ChatFormatting.RED));
				return;
			}
			if (!ownQi) brush.hurtAndBreak(wear, player, p -> {});
		}
		board.paint(x, y, stroke);
		t.store(board);
		if (stroke.conducts() && t.mend(board.at(x, y))) CraftMastery.practise(player, CraftMastery.Craft.PUPPETRY, 1);
		player.level().playSound(null, BlockPos.containing(t.where()), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.5f,
				stroke == CircuitBoard.Stroke.NONE ? 0.8f : 1.1f);
		send(player, seq, null);
	}

	/** C2S puppet_act: sealing, wiping the whole board, or the screen closed. */
	public static void act(ServerPlayer player, int act) {
		if (act == CLOSE) {
			OPEN.remove(player.getUUID());
			return;
		}
		Target t = target(player);
		if (t == null) {
			send(player, 0, null);
			return;
		}
		if (captive(player, t)) {
			send(player, 0, Component.translatable(ModLang.BOARD_CAPTIVE_PAINT).withStyle(ChatFormatting.RED));
			return;
		}
		if (act == WIPE_ALL) {
			CircuitBoard board = t.board();
			// (A body's meridians stay: they are its own, never wiped.)
			for (int y = 0; y < board.size; y++) for (int x = 0; x < board.size; x++) if (!board.isMeridian(x, y)) board.set(x, y, CircuitBoard.Stroke.NONE);
			t.store(board);
			player.level().playSound(null, BlockPos.containing(t.where()), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.7f, 0.7f);
			send(player, 0, Component.translatable(ModLang.BOARD_WIPED));
		} else if (act == SEAL && t instanceof BenchTarget bench) {
			Component refusal = seal(player, bench.bench());
			if (refusal != null) send(player, 0, refusal.copy().withStyle(ChatFormatting.RED));
		} else if (act == COPY) {
			send(player, 0, copy(player, t));
		} else if ((act == SOUL_RELEASE || act == SOUL_MOVE || act == SOUL_WIPE) && t instanceof PuppetTarget target) {
			// Its soul let go, moved or its copy wiped (PuppetSouls: by a Puppet Workbench).
			Component said = switch (act) {
				case SOUL_RELEASE -> PuppetSouls.release(player, target.puppet());
				case SOUL_MOVE -> PuppetSouls.move(player, target.puppet());
				default -> PuppetSouls.wipe(player, target.puppet());
			};
			send(player, 0, said);
		}
	}

	/** The board copied onto a sheet of paper with the brush: a Circuit Diagram for its puppeteer. What to tell them. */
	private static Component copy(ServerPlayer player, Target t) {
		if (!player.getAbilities().instabuild) {
			ItemStack brush = brushFor(player, COPY_WEAR);
			if (brush.isEmpty()) return Component.translatable(ModLang.PAINT_NO_BRUSH).withStyle(ChatFormatting.RED);
			if (!take(player, Items.PAPER, 1)) return Component.translatable(ModLang.BOARD_COPY_NEEDS).withStyle(ChatFormatting.RED);
			brush.hurtAndBreak(COPY_WEAR, player, p -> {});
		}
		ItemStack diagram = CircuitDiagramItem.of(t.blueprint(), t.grade(), Puppetry.strokes(t.board()), player.getGameProfile().getName(), false);
		if (!player.getInventory().add(diagram)) player.drop(diagram, false);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
		return Component.translatable(ModLang.BOARD_COPIED);
	}

	/** Seals the puppet on {@code bench} for {@code player}: null if it stood up, else why not. */
	private static @Nullable Component seal(ServerPlayer player, PuppetWorkbenchBlockEntity bench) {
		ItemStack frame = bench.frame(), core = bench.core();
		if (!(frame.getItem() instanceof PuppetFrameItem item)) return Component.translatable(ModLang.BENCH_EMPTY);
		if (core.isEmpty()) return Component.translatable(ModLang.SEAL_NO_CORE);
		// A body of flesh (phase 2b): refined a day on the bench, its heart a core of at least the dead's realm.
		PuppetBodies.Dead dead = PuppetBodies.deadOf(frame);
		if (dead != null) {
			long now = player.serverLevel().getGameTime();
			if (!PuppetBodies.ready(frame, now)) {
				return Component.translatable(ModLang.BODY_REFINING, 100 * PuppetBodies.refined(frame, now) / PuppetBodies.REFINE_TICKS);
			}
			if (!PuppetBodies.coreFits(dead, core)) {
				return Component.translatable(ModLang.BODY_CORE_TOO_WEAK, CultivatorNpc.realmOf(Math.max(0, dead.rank())).getDisplayName());
			}
		} else if (Puppetry.puppetRank(item.grade(), DemonCoreItem.rank(core)) < 0) {
			return Component.translatable(ModLang.BENCH_CORE_TOO_WEAK, Puppetry.lowest(item.grade()).getDisplayName());
		}
		CircuitBoard board = PuppetFrameItem.board(frame);
		CircuitBoard.Scores scores = board.score(Puppetry.budget(item.grade(), core), Puppetry.phaseOf(core));
		if (!scores.sealFed()) return Component.translatable(ModLang.SEAL_HEART_UNFED);
		// The user's pick of 2026-11-17: a drawing whose veins want more qi than its core gives won't be sealed.
		if (scores.over()) return Component.translatable(ModLang.SEAL_OVER_BUDGET, scores.wanted(), scores.budget());
		PlayerCultivation own = CultivationManager.own(player);
		Puppetry.Ledger ledger = Puppetry.Ledger.get(player.server);
		int used = ledger.used(player.getUUID()), most = Puppetry.slots(own), cost = Puppetry.cost(item.grade());
		if (used + cost > most) return Component.translatable(ModLang.SEAL_NO_SLOTS, cost, used, most);
		boolean creative = player.getAbilities().instabuild;
		if (!creative && !take(player, item.grade().stone(), Puppetry.SEAL_STONES)) {
			return Component.translatable(ModLang.SEAL_NO_STONES, Puppetry.SEAL_STONES, new ItemStack(item.grade().stone()).getHoverName());
		}
		ServerLevel level = player.serverLevel();
		Vec3 at = Vec3.atBottomCenterOf(bench.getBlockPos().above());
		PuppetEntity puppet = PuppetEntity.seal(level, frame, core, player, at);
		bench.clear();
		OPEN.remove(player.getUUID());
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(0);
		buf.writeBoolean(false);
		ServerPlayNetworking.send(player, ModPackets.PUPPET_BOARD, buf);
		CraftMastery.practise(player, CraftMastery.Craft.PUPPETRY, Puppetry.sealPractice(item.grade(), scores.grade()));
		level.playSound(null, bench.getBlockPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
		level.playSound(null, bench.getBlockPos(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0f, 0.7f);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x, at.y + 0.8, at.z, 30, 0.3, 0.6, 0.3, 0.04);
		player.sendSystemMessage(Component.translatable(ModLang.SEAL_DONE, puppet.getDisplayName(),
				Component.translatable(ModLang.circuitGrade(scores.grade().name().toLowerCase(java.util.Locale.ROOT))), scores.flow(),
				scores.stability(), scores.harmony(), scores.clarity(), scores.resilience()).withStyle(ChatFormatting.AQUA));
		return null;
	}

	/** {@code player} has left: their board closed. */
	public static void forget(UUID player) {
		OPEN.remove(player);
	}

	/** The server stopped: no board stays open. */
	public static void clear() {
		OPEN.clear();
	}

	private PuppetBoards() {}
}
