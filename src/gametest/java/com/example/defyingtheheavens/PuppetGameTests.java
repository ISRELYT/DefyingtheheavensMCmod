package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Puppetry, phase 1 (2026-11-17): the circuit board's ranks and builds (each build's Circuit Path within its qi, cool from Master, the
 * Saint's Balanced Perfect), its flaws (by forging, fixed by the seed, never cutting a node off), the frames at the forge, the core's realm
 * within the frame's, a sealed puppet's strength and the ledger's slots, its lines cut by harm and by Metal qi, its heart cut off (inert)
 * and mended, painting with the brush at the bench (its wear, its rank, its matter), a bee carrying between two chests, seizing by rank, and
 * taking a puppet apart. The deeper circuits, phase A: veins, the core's qi and the ring's, an overdrawn board refused, more veins harder
 * blows, and blows heating a puppet on every frame. Phase B: phase seals, their tokens and what they do. Phase C: arrays known by their
 * shape (each kind, a Twin Seal, none unreached nor in the core's square) and what they do on a puppet.
 */
public class PuppetGameTests implements FabricGameTest {
	private static final BlockPos BENCH = new BlockPos(2, 2, 2);

	/** A player in earnest (a mock player says it is creative), at the bench. */
	private static ServerPlayer player(GameTestHelper helper, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(BENCH);
		p.teleportTo(helper.getLevel(), at.getX() + 1.5, at.getY(), at.getZ() + 0.5, 0, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setMortal(false);
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		return p;
	}

	private static void gone(ServerPlayer... players) {
		for (ServerPlayer p : players) if (p != null && p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	/** A frame of {@code blueprint} at {@code grade}, its board drawn as a Saint's Circuit Path (on its own flaws). */
	private static ItemStack drawnFrame(PuppetBlueprint blueprint, ArtifactGrade grade, int rank, long seed) {
		ItemStack frame = PuppetFrameItem.of(blueprint, grade, Artifacts.Quality.PERFECT, seed, null);
		CircuitBoard board = PuppetFrameItem.board(frame);
		board.lay(board.circuitPath(rank));
		PuppetFrameItem.keep(frame, board);
		return frame;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void circuitPathsByRankAndBuild(GameTestHelper helper) {
		// The deeper circuits, phase A: every build's path at every rank feeds every part within the frame's qi; from Master its lines carry
		// cool (Stability 100); a Balanced path is Fine from Master and Perfect at Saint; each build feeds its own parts at least as hard as
		// the Balanced path does, or draws its array by them (phase C: an Eye, a Spiral). From 13 x 13 every array the build's rank paints is
		// drawn and works, a pair's a Twin Seal at Saint.
		for (PuppetBlueprint blueprint : PuppetBlueprint.values()) {
			for (int grade = 0; grade < 5; grade++) {
				CircuitBoard blank = new CircuitBoard(blueprint, grade, 0, 7L);
				int qi = CircuitBoard.BUDGET[grade];
				for (int rank = 0; rank < 5; rank++) {
					CircuitBoard balanced = blank.copy();
					balanced.lay(blank.circuitPath(rank, CircuitBoard.Build.BALANCED, qi));
					CircuitBoard.Scores even = balanced.score(qi);
					for (CircuitBoard.Build build : CircuitBoard.Build.of(blueprint, blank.hasArt())) {
						CircuitBoard board = blank.copy();
						board.lay(blank.circuitPath(rank, build, qi));
						CircuitBoard.Scores s = board.score(qi);
						String what = blueprint.getId() + " grade " + grade + " rank " + rank + " " + build.getId();
						helper.assertTrue(s.sealFed() && !s.over(), what + ": the heart fed, within its qi (" + s.wanted() + " of " + s.budget() + ")");
						for (boolean dead : s.dead()) helper.assertTrue(!dead, what + ": every node fed");
						if (rank >= 2) helper.assertTrue(s.stability() == 100, what + ": a Master's lines carry cool, Stability " + s.stability());
						if (build == CircuitBoard.Build.BALANCED) {
							if (rank >= 2) helper.assertTrue(s.grade().ordinal() >= CircuitBoard.Grade.FINE.ordinal(), what + ": Fine from Master, was " + s.grade());
							if (rank == 4) helper.assertTrue(s.grade() == CircuitBoard.Grade.PERFECT, what + ": Perfect at Saint, was " + s.grade());
						}
						if (rank >= 2) {
							for (int n = 0; n < board.nodes().size(); n++) {
								if (!feeds(build, blueprint, board.nodes().get(n))) continue;
								CircuitBoard.Formed f = s.arrayOf(n);
								boolean array = f != null && f.fed() && List.of(build.arrays()).contains(f.array());
								helper.assertTrue(s.veins()[n] >= even.veins()[n] || array, what + ": " + board.nodes().get(n).id()
										+ " fed at least as hard as Balanced feeds it, or its array drawn (" + s.veins()[n] + " vs " + even.veins()[n] + ")");
							}
						}
						if (grade >= 2) {
							for (CircuitBoard.Array a : build.arrays()) {
								if (rank < a.rank) continue;
								helper.assertTrue(s.has(a), what + ": its " + a.getId() + " drawn and working");
								if (rank == 4 && a.twinnable()) helper.assertTrue(s.twin(a), what + ": its " + a.getId() + " a Twin Seal at Saint");
							}
						}
					}
				}
			}
		}
		helper.succeed();
	}

	/** Whether {@code build} feeds {@code node} hardest (its first raises). */
	private static boolean feeds(CircuitBoard.Build build, PuppetBlueprint blueprint, PuppetBlueprint.Node node) {
		return switch (build) {
			case BRAWLER -> node.kind() == PuppetBlueprint.NodeKind.LIMB && node.pair() == 0;
			case SCOUT -> node.kind() == PuppetBlueprint.NodeKind.LIMB && node.pair() == 1;
			case COURIER -> node.kind() == PuppetBlueprint.NodeKind.LIMB && node.pair() == 0;
			case SENTINEL -> node.kind() == PuppetBlueprint.NodeKind.SENSE;
			case CASTER -> node.kind() == PuppetBlueprint.NodeKind.ART;
			default -> false;
		};
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void veinsAreTheSidesALineReaches(GameTestHelper helper) {
		// A Spirit humanoid's board (11 x 11): its left arm at (1, 4), the left outlet at (4, 5), the up outlet at (5, 4) under the mouth.
		CircuitBoard board = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT.ordinal(), 0, 1L);
		int arm = node(board, "left_arm"), mouth = node(board, "mouth");
		for (int x = 1; x <= 4; x++) board.paint(x, 5, CircuitBoard.Stroke.LINE);
		CircuitBoard.Scores s = board.score(18);
		helper.assertTrue(s.veins()[arm] == 1 && !s.dead()[arm], "A line to the arm's underside: one vein, was " + s.veins()[arm]);
		helper.assertTrue(!s.sure()[arm], "One way in: one cut kills it");
		int before = s.wanted();
		// A second way in, by its other side, from the up outlet.
		board.paint(5, 4, CircuitBoard.Stroke.LINE);
		for (int x = 2; x <= 4; x++) board.paint(x, 4, CircuitBoard.Stroke.LINE);
		s = board.score(18);
		helper.assertTrue(s.veins()[arm] == 2, "Its other side reached: two veins, was " + s.veins()[arm]);
		helper.assertTrue(s.sure()[arm], "Two separate ways in: a cut can't kill it");
		helper.assertTrue(s.veins()[mouth] == 1 && s.wanted() == before + 1 + 2, "The arm's second vein draws 1, the mouth's first 2 (an art), was "
				+ before + " -> " + s.wanted());
		// A Mortal frame has no art: its mouth draws nothing, and passes the qi on to the head.
		CircuitBoard mortal = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL.ordinal(), 0, 1L);
		int c = mortal.size / 2;
		mortal.paint(c, c - 1, CircuitBoard.Stroke.LINE);
		CircuitBoard.Scores m = mortal.score(14);
		helper.assertTrue(mortal.load(node(mortal, "mouth")) == 0 && m.wanted() == 1, "A Mortal mouth draws nothing; the head through it 1, was " + m.wanted());
		helper.assertTrue(m.budget() == 14 && CircuitBoard.BUDGET[4] == 26, "A Mortal frame's core gives 14, an Immortal one's 26");
		helper.succeed();
	}

	private static int node(CircuitBoard board, String id) {
		for (int n = 0; n < board.nodes().size(); n++) if (board.nodes().get(n).id().equals(id)) return n;
		return -1;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void phaseSealsTuneTheirLines(GameTestHelper helper) {
		// A Spirit humanoid's board (11 x 11): a Fire seal on the left outlet (4, 5) and a line to the left arm's underside (1, 5).
		CircuitBoard board = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT.ordinal(), 0, 1L);
		int arm = node(board, "left_arm");
		helper.assertTrue("seal_outlet".equals(board.refusal(3, 5, CircuitBoard.Stroke.SEAL_FIRE)), "A seal goes on an outlet only");
		board.paint(4, 5, CircuitBoard.Stroke.SEAL_FIRE);
		for (int x = 1; x <= 3; x++) board.paint(x, 5, CircuitBoard.Stroke.LINE);
		CircuitBoard.Scores s = board.score(18, CircuitBoard.Phase.FIRE);
		helper.assertTrue(s.phases()[arm] == CircuitBoard.Phase.FIRE.bit(), "Its line tuned to Fire, the arm fed Fire");
		helper.assertTrue(s.phaseQi() == CircuitBoard.RESONANCE_QI && s.budget() == 18 + CircuitBoard.RESONANCE_QI, "A Fire core resonates with a Fire seal: +2");
		helper.assertTrue(board.score(18, CircuitBoard.Phase.METAL).phaseQi() == -CircuitBoard.RESONANCE_QI, "Fire melts Metal: a Metal core loses 2 to it");
		// A Metal seal on the down outlet joined to the Fire lines: the one set of lines holds two elements and clashes.
		board.paint(5, 6, CircuitBoard.Stroke.SEAL_METAL);
		board.paint(4, 6, CircuitBoard.Stroke.LINE);
		s = board.score(18, null);
		helper.assertTrue(s.phases()[arm] == 0 && board.clashes()[board.at(3, 5)], "Two elements in one set of lines clash: none");
		// Two sets of lines close by on a 13 x 13 board (centre (6, 6), heart (6, 8)): Wood from the left outlet down to (3, 7), Fire from the
		// down outlet round under the heart up to (4, 8), a corner and a gap from the Wood (outside the core's square). Wood feeds Fire;
		// a Metal seal instead overcomes the Wood.
		CircuitBoard feeding = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.EARTH.ordinal(), 0, 1L);
		feeding.paint(5, 6, CircuitBoard.Stroke.SEAL_WOOD);
		for (int[] at : new int[][] {{4, 6}, {3, 6}, {3, 7}}) feeding.paint(at[0], at[1], CircuitBoard.Stroke.LINE);
		feeding.paint(6, 7, CircuitBoard.Stroke.SEAL_FIRE);
		for (int[] at : new int[][] {{7, 7}, {7, 8}, {7, 9}, {6, 9}, {5, 9}, {4, 9}, {4, 8}}) feeding.paint(at[0], at[1], CircuitBoard.Stroke.LINE);
		CircuitBoard.Scores fed = feeding.score(22, null);
		helper.assertTrue(fed.phaseQi() == CircuitBoard.FEEDING_QI, "Wood feeds Fire close by: +1 qi, was " + fed.phaseQi());
		CircuitBoard overcoming = feeding.copy();
		overcoming.paint(6, 7, CircuitBoard.Stroke.SEAL_METAL);
		CircuitBoard.Scores over = overcoming.score(22, null);
		helper.assertTrue(over.phaseQi() == 0 && over.clarity() < fed.clarity() && over.stability() < fed.stability(),
				"Metal overcomes Wood close by: it leaks twice over and heats (Clarity " + fed.clarity() + " -> " + over.clarity() + ", Stability "
						+ fed.stability() + " -> " + over.stability() + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void elementsWorkOnThePuppet(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-elements");
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
		PuppetEntity fire = sealedWith(level, p, at, DemonCoreItem.of(rank, QiElement.FIRE, null), 31L);
		PuppetEntity earth = sealedWith(level, p, at, DemonCoreItem.of(rank, QiElement.EARTH, null), 32L);
		PuppetEntity plain = sealedWith(level, p, at, DemonCoreItem.of(rank, null, null), 33L);
		Husk foe = EntityType.HUSK.create(level);
		try {
			// A Master's path seals its outlets in the core's own element: every part takes it, and the core resonates (+2).
			helper.assertTrue(fire.scores().phaseQi() == CircuitBoard.RESONANCE_QI && fire.blowsCarry(CircuitBoard.Phase.FIRE), "A Fire core's path: Fire blows");
			foe.moveTo(at.x + 1, at.y, at.z, 0, 0);
			level.addFreshEntity(foe);
			fire.doHurtTarget(foe);
			helper.assertTrue(foe.isOnFire(), "Fire blows set the foe alight");
			helper.assertTrue(earth.bodyVeins(CircuitBoard.Phase.EARTH) > 0 && earth.getMaxHealth() > plain.getMaxHealth() * 1.05,
					"Earth in its body: more health (" + earth.getMaxHealth() + " vs " + plain.getMaxHealth() + ")");
		} finally {
			foe.discard();
			for (PuppetEntity puppet : new PuppetEntity[] {fire, earth, plain}) {
				Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
				puppet.discard();
			}
			gone(p);
		}
		helper.succeed();
	}

	/** A Spirit humanoid sealed for {@code p} with {@code core}, drawn as a Master's Balanced path for that core. */
	private static PuppetEntity sealedWith(ServerLevel level, ServerPlayer p, Vec3 at, ItemStack core, long seed) {
		ItemStack frame = PuppetFrameItem.of(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT, Artifacts.Quality.PERFECT, seed, null);
		CircuitBoard board = PuppetFrameItem.board(frame);
		board.lay(board.circuitPath(CraftMastery.Rank.MASTER.ordinal(), CircuitBoard.Build.BALANCED, Puppetry.budget(ArtifactGrade.SPIRIT, core),
				Puppetry.phaseOf(core)));
		PuppetFrameItem.keep(frame, board);
		return PuppetEntity.seal(level, frame, core, p, at);
	}

	// A Heaven humanoid's board (15 x 15, centre (7, 7)): head (7, 1), mouth (7, 5), heart (7, 9), left arm (1, 6), right arm (13, 6), left
	// leg (5, 13); shoulders (5, 5) (9, 5) and (4, 4) (10, 4), hips (6, 12) (8, 12) and (4, 11) (10, 11).
	private static final int HEAVEN = ArtifactGrade.HEAVEN.ordinal();

	private static void lines(CircuitBoard board, CircuitBoard.Stroke stroke, int[]... cells) {
		for (int[] at : cells) board.paint(at[0], at[1], stroke);
	}

	/** A line from the left outlet (6, 7) out and up the board's left side to (5, 1). */
	private static void lineToTheHeadsLeft(CircuitBoard board) {
		lines(board, CircuitBoard.Stroke.LINE, new int[] {6, 7}, new int[] {5, 7}, new int[] {4, 7}, new int[] {3, 7}, new int[] {3, 6},
				new int[] {3, 5}, new int[] {3, 4}, new int[] {3, 3}, new int[] {3, 2}, new int[] {3, 1}, new int[] {4, 1}, new int[] {5, 1});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void arraysAreKnownByTheirShape(GameTestHelper helper) {
		// The deeper circuits, phase C. An Eye of Discernment: a relay touching the head's left (6, 1), lines on its other sides, fed from the
		// left outlet: the head fed through it, the Eye working, 2 qi drawn besides the head's 1.
		CircuitBoard eye = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		lineToTheHeadsLeft(eye);
		eye.paint(6, 1, CircuitBoard.Stroke.RELAY);
		lines(eye, CircuitBoard.Stroke.LINE, new int[] {6, 0}, new int[] {6, 2});
		CircuitBoard.Scores s = eye.score(24);
		int head = node(eye, "head");
		CircuitBoard.Formed f = s.arrayOf(head);
		helper.assertTrue(f != null && f.array() == CircuitBoard.Array.EYE && f.fed() && f.cells().length == 4 && s.has(CircuitBoard.Array.EYE),
				"A relay by the head, lines round it: an Eye of Discernment, working");
		helper.assertTrue(s.wanted() == 1 + CircuitBoard.Array.EYE.qi, "The head's vein and the Eye: 3 qi, was " + s.wanted());
		// Cut off from the core, it does nothing and draws nothing.
		eye.paint(4, 1, CircuitBoard.Stroke.NONE);
		s = eye.score(24);
		helper.assertTrue(!s.has(CircuitBoard.Array.EYE) && s.arrayOf(head) != null && s.wanted() == 0, "Unreached: drawn, but nothing and no qi");
		// One line short (its top side empty): no Eye at all.
		eye.paint(6, 0, CircuitBoard.Stroke.NONE);
		eye.paint(4, 1, CircuitBoard.Stroke.LINE);
		helper.assertTrue(eye.score(24).arrayOf(head) == null, "A free side of the knot left empty: no array");

		// Guard Coils: two-by-two thick blocks by each arm's inner side, each the other's mirror, fed from the side outlets: a Twin Seal (both
		// arms' veins, 2 qi each coil and 1 for the seal).
		CircuitBoard coil = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		lines(coil, CircuitBoard.Stroke.THICK, new int[] {2, 6}, new int[] {3, 6}, new int[] {2, 7}, new int[] {3, 7}, new int[] {12, 6},
				new int[] {11, 6}, new int[] {12, 7}, new int[] {11, 7});
		lines(coil, CircuitBoard.Stroke.LINE, new int[] {6, 7}, new int[] {5, 7}, new int[] {4, 7}, new int[] {8, 7}, new int[] {9, 7}, new int[] {10, 7});
		s = coil.score(24);
		helper.assertTrue(s.has(CircuitBoard.Array.GUARD_COIL) && s.twin(CircuitBoard.Array.GUARD_COIL), "Mirrored coils on both arms: a Twin Seal");
		helper.assertTrue(s.wanted() == 2 + 2 * CircuitBoard.Array.GUARD_COIL.qi + CircuitBoard.TWIN_QI, "Two arms, two coils, the seal: 7 qi, was " + s.wanted());
		coil.paint(10, 7, CircuitBoard.Stroke.NONE);
		s = coil.score(24);
		helper.assertTrue(s.has(CircuitBoard.Array.GUARD_COIL) && !s.twin(CircuitBoard.Array.GUARD_COIL), "One side cut off: one coil works, no Twin Seal");

		// A Gale Stair: three diagonals in a slant from the left leg's left side (4, 13), fed from the left outlet down the side.
		CircuitBoard stair = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		lines(stair, CircuitBoard.Stroke.DIAGONAL, new int[] {4, 13}, new int[] {3, 12}, new int[] {2, 11});
		lines(stair, CircuitBoard.Stroke.LINE, new int[] {6, 7}, new int[] {5, 7}, new int[] {4, 7}, new int[] {3, 7}, new int[] {2, 7}, new int[] {2, 8},
				new int[] {2, 9}, new int[] {2, 10});
		s = stair.score(24);
		CircuitBoard.Formed slant = s.arrayOf(node(stair, "left_leg"));
		helper.assertTrue(slant != null && slant.array() == CircuitBoard.Array.GALE_STAIR && slant.fed() && !s.dead()[node(stair, "left_leg")],
				"A slant of diagonals by a leg: a Gale Stair, the leg fed through it");

		// A Gathering Spiral: a gathering knot on the mouth's top side (7, 4), lines round it, fed from the left outlet: the art's second
		// charge and the spiral. The same knot on the up outlet (the core's square) is no spiral.
		CircuitBoard spiral = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		spiral.paint(7, 4, CircuitBoard.Stroke.GATHER);
		lines(spiral, CircuitBoard.Stroke.LINE, new int[] {6, 4}, new int[] {8, 4}, new int[] {7, 3}, new int[] {6, 7}, new int[] {5, 7}, new int[] {4, 7},
				new int[] {3, 7}, new int[] {3, 6}, new int[] {3, 5}, new int[] {3, 4}, new int[] {3, 3}, new int[] {4, 3}, new int[] {5, 3}, new int[] {6, 3});
		s = spiral.score(24);
		helper.assertTrue(s.has(CircuitBoard.Array.GATHERING_SPIRAL) && s.artCharges() == 2, "A spiral by the mouth: working, two charges");
		CircuitBoard core = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		core.paint(7, 6, CircuitBoard.Stroke.GATHER);
		lines(core, CircuitBoard.Stroke.LINE, new int[] {6, 6}, new int[] {8, 6});
		helper.assertTrue(core.arrays().isEmpty(), "No array in the core's square");

		// An Iron Lock: a ward under the heart (7, 10), bridges on its three other sides, one fed from the left outlet round to (5, 10); the
		// heart fed by the down outlet. 3 qi.
		CircuitBoard lock = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		lock.paint(7, 10, CircuitBoard.Stroke.WARD);
		lines(lock, CircuitBoard.Stroke.BRIDGE, new int[] {6, 10}, new int[] {8, 10}, new int[] {7, 11});
		lines(lock, CircuitBoard.Stroke.LINE, new int[] {7, 8}, new int[] {6, 7}, new int[] {6, 8}, new int[] {5, 8}, new int[] {5, 9}, new int[] {5, 10});
		s = lock.score(24);
		helper.assertTrue(s.has(CircuitBoard.Array.IRON_LOCK) && s.wanted() == 1 + CircuitBoard.Array.IRON_LOCK.qi,
				"A ward by the heart, bridges round it: an Iron Lock (heart 1 + lock 3 qi, was " + s.wanted() + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void arraysWorkOnThePuppet(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-arrays"), thief = player(helper, "test-lockpick");
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))), there = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 2, 8)));
		// An Eye, mirrored Guard Coils, a Gale Stair and an Iron Lock on one Heaven humanoid (the boards above, together).
		CircuitBoard all = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		lineToTheHeadsLeft(all);
		all.paint(6, 1, CircuitBoard.Stroke.RELAY);
		lines(all, CircuitBoard.Stroke.LINE, new int[] {6, 0}, new int[] {6, 2});
		lines(all, CircuitBoard.Stroke.THICK, new int[] {2, 6}, new int[] {3, 6}, new int[] {2, 7}, new int[] {3, 7}, new int[] {12, 6},
				new int[] {11, 6}, new int[] {12, 7}, new int[] {11, 7});
		lines(all, CircuitBoard.Stroke.LINE, new int[] {8, 7}, new int[] {9, 7}, new int[] {10, 7}, new int[] {2, 8}, new int[] {2, 9}, new int[] {2, 10});
		lines(all, CircuitBoard.Stroke.DIAGONAL, new int[] {4, 13}, new int[] {3, 12}, new int[] {2, 11});
		all.paint(7, 10, CircuitBoard.Stroke.WARD);
		lines(all, CircuitBoard.Stroke.BRIDGE, new int[] {6, 10}, new int[] {8, 10}, new int[] {7, 11});
		lines(all, CircuitBoard.Stroke.LINE, new int[] {7, 8}, new int[] {6, 8}, new int[] {5, 8}, new int[] {5, 9}, new int[] {5, 10});
		// And a plain one: its heart fed by the down outlet, nothing else.
		CircuitBoard bare = new CircuitBoard(PuppetBlueprint.HUMANOID, HEAVEN, 0, 1L);
		bare.paint(7, 8, CircuitBoard.Stroke.LINE);
		PuppetEntity armed = sealedBoard(level, p, at, all), plain = sealedBoard(level, p, there, bare);
		Husk foe = EntityType.HUSK.create(level);
		try {
			helper.assertTrue(armed.hasArray(CircuitBoard.Array.EYE, false) && armed.hasArray(CircuitBoard.Array.GUARD_COIL, true)
					&& armed.hasArray(CircuitBoard.Array.GALE_STAIR, false) && armed.ironLocked(), "Its arrays all work");
			helper.assertTrue(armed.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE)
					== Puppetry.sight(armed.scores().veins()[node(all, "head")]) + Puppetry.EYE_SIGHT, "The Eye: 16 blocks more sight");
			// The coil turns the first blow aside, not the next.
			foe.moveTo(at.x + 1, at.y, at.z, 0, 0);
			foe.setNoAi(true);
			level.addFreshEntity(foe);
			float health = armed.getHealth();
			helper.assertTrue(!armed.hurt(level.damageSources().mobAttack(foe), 4) && armed.getHealth() == health, "The Guard Coil turns the blow aside");
			helper.assertTrue(armed.hurt(level.damageSources().mobAttack(foe), 4) && armed.getHealth() < health, "The next blow lands");
			// The lock: no seal takes it, whatever the seizer's rank.
			CultivationManager.own(thief).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.SAINT.from);
			thief.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, CommandSealItem.of(ArtifactGrade.IMMORTAL));
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, armed) != null && PuppetCommands.seizeRefusal(thief, plain) == null,
					"An Iron Lock no Saint can seize; the plain puppet a Saint can");
			// The stair: a dash at a foe out of its reach, along a stone floor.
			for (int x = 1; x <= 10; x++) helper.setBlock(new BlockPos(x, 1, 6), Blocks.STONE);
			Vec3 from = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 6)));
			Husk far = EntityType.HUSK.create(level);
			far.moveTo(from.x + 7, from.y, from.z, 0, 0);
			far.setNoAi(true);
			level.addFreshEntity(far);
			armed.setNoAi(true);
			armed.moveTo(from.x, from.y, from.z, 0, 0);
			double before = Math.hypot(far.getX() - armed.getX(), far.getZ() - armed.getZ());
			armed.startDash(far, 1.5);
			helper.runAfterDelay(8, () -> {
				double after = Math.hypot(far.getX() - armed.getX(), far.getZ() - armed.getZ());
				helper.assertTrue(after < before - 3 && armed.dashWait() > 0, "The Gale Stair dashed it at the foe (" + before + " -> " + after + ")");
				far.discard();
				// A freezing holds the locked one half as long.
				armed.freeze(40);
				plain.freeze(40);
				helper.runAfterDelay(25, () -> {
					helper.assertTrue(!armed.isFrozen() && plain.isFrozen(), "The lock halved its freezing: free at 25 ticks, the plain one still frozen");
					foe.discard();
					for (PuppetEntity puppet : new PuppetEntity[] {armed, plain}) {
						Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
						puppet.discard();
					}
					gone(p, thief);
					helper.succeed();
				});
			});
		} catch (RuntimeException e) {
			foe.discard();
			for (PuppetEntity puppet : new PuppetEntity[] {armed, plain}) {
				Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
				puppet.discard();
			}
			gone(p, thief);
			throw e;
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSlainBeastsSoulFillsAnEmptyRing(GameTestHelper helper) {
		// Puppetry phase 2a: a cow slain by its puppeteer beside a hollow puppet: its soul is drawn into the ring, the puppet sharper and
		// swifter for it; a second cow finds the ring full.
		ServerPlayer p = player(helper, "test-soulcatcher");
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		PuppetEntity puppet = sealedWith(level, p, at, DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 41L);
		net.minecraft.world.entity.animal.Cow cow = EntityType.COW.create(level), second = EntityType.COW.create(level);
		try {
			double sight = puppet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
			double pace = puppet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
			helper.assertTrue(puppet.soul() == null && puppet.soulKind() == null, "A new puppet is hollow");
			cow.moveTo(at.x + 3, at.y, at.z, 0, 0);
			level.addFreshEntity(cow);
			cow.hurt(level.damageSources().playerAttack(p), 1000);
			PuppetSoul soul = puppet.soul();
			helper.assertTrue(!cow.isAlive() && soul != null && soul.kind == PuppetSoul.Kind.BEAST && soul.real(), "The slain cow's soul is in the ring");
			helper.assertTrue(puppet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE) == sight + PuppetSouls.BEAST_SIGHT
					&& puppet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) > pace * 1.05,
					"A beast's soul: farther sight, a swifter pace");
			second.moveTo(at.x - 3, at.y, at.z, 0, 0);
			level.addFreshEntity(second);
			second.hurt(level.damageSources().playerAttack(p), 1000);
			helper.assertTrue(puppet.soul() == soul, "The ring already full: the second cow's soul goes its way");
			// Far off, nothing is caught: a ring draws a soul from within 16 blocks.
			puppet.bindSoul(null);
			net.minecraft.world.entity.animal.Cow far = EntityType.COW.create(level);
			far.moveTo(at.x + 40, at.y, at.z, 0, 0);
			PuppetSouls.onDeath(far, level.damageSources().playerAttack(p));
			helper.assertTrue(puppet.soul() == null, "A beast slain 40 blocks off: nothing drawn");
		} finally {
			cow.discard();
			second.discard();
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
			puppet.discard();
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aCultivatorsSoulBringsItsNameAndArts(GameTestHelper helper) {
		// A cultivator slain by its puppeteer beside a hollow Spirit puppet: its name on the puppet, a charge more of the core's art, its own
		// striking arts (one or two), and its slayer drawn toward the devilish path.
		ServerPlayer p = player(helper, "test-soulbinder");
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		PuppetEntity puppet = sealedWith(level, p, at, DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 42L);
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		try {
			npc.moveTo(at.x + 2, at.y, at.z, 0, 0);
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE));
			npc.setNoAi(true);
			level.addFreshEntity(npc);
			int charges = puppet.maxCharges(), alignment = CultivationManager.own(p).getAlignment();
			// (Its death called for straight: no body left on the test's ground.)
			PuppetSouls.onDeath(npc, level.damageSources().playerAttack(p));
			PuppetSoul soul = puppet.soul();
			helper.assertTrue(soul != null && soul.kind == PuppetSoul.Kind.CULTIVATOR && soul.name.equals(npc.getPersonalName()),
					"The cultivator's soul in the ring, its own name kept (" + (soul == null ? "none" : soul.name) + ")");
			helper.assertTrue(puppet.soulName().equals(soul.name) && puppet.soulKind() == PuppetSoul.Kind.CULTIVATOR, "The puppet goes by its name");
			helper.assertTrue(puppet.maxCharges() == charges + 1, "A charge more of its core's art (" + charges + " -> " + puppet.maxCharges() + ")");
			helper.assertTrue(CultivationManager.own(p).getAlignment() == alignment - PuppetSouls.BIND_SHIFT, "Binding a cultivator's soul is devilish");
			helper.assertTrue(soul.arts.size() <= PuppetSouls.MOST_ARTS, "One or two of its arts at most");
			boolean strikes = false;
			for (Spell spell : CultivatorSpells.repertoire(npc)) {
				CultivatorSpells.Use use = CultivatorSpells.use(spell);
				strikes |= spell.isHeaven() && (use == CultivatorSpells.Use.AT_FOE || use == CultivatorSpells.Use.ON_FOE);
			}
			helper.assertTrue(!strikes || !soul.arts.isEmpty(), "A cultivator who knew a striking art lends the puppet one");
			// Kept when the puppet is saved and loaded.
			net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
			puppet.addAdditionalSaveData(saved);
			PuppetEntity copy = new PuppetEntity(ModEntities.PUPPET_HUMANOID, level);
			copy.readAdditionalSaveData(saved);
			helper.assertTrue(copy.soul() != null && copy.soul().name.equals(soul.name) && copy.soul().arts.equals(soul.arts)
					&& copy.soulKind() == PuppetSoul.Kind.CULTIVATOR, "Its soul kept through saving and loading");
		} finally {
			npc.discard();
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
			puppet.discard();
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void aSoulIsCopiedAndHandledAtTheBench(GameTestHelper helper) {
		// The copy grows while its puppeteer is in the world with it; whole, the real soul gone, it can only be wiped. At a bench: a real
		// soul released (a cultivator's: back toward the righteous) or moved into another puppet's empty ring there.
		ServerPlayer p = player(helper, "test-soulkeeper");
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		PuppetEntity puppet = sealedWith(level, p, at, core, 43L), other = sealedWith(level, p, at.add(2, 0, 0), core, 44L);
		puppet.bindSoul(new PuppetSoul(PuppetSoul.Kind.BEAST, "Wolf", "minecraft:wolf", null, 0, List.of()));
		helper.runAfterDelay(45, () -> {
			try {
				helper.assertTrue(puppet.underControl() && puppet.soul().copied() >= 20, "Under its puppeteer's control its soul is copied ("
						+ puppet.soul().copied() + " ticks)");
				helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
				// Moved into the other puppet's empty ring by the bench: its copying begun anew.
				PuppetSouls.move(p, puppet);
				helper.assertTrue(puppet.soul() == null && other.soul() != null && other.soul().copied() == 0, "The soul moved into the other ring, copied anew");
				// A cultivator's soul let go: the ring hollow, its keeper drawn back a little toward the righteous.
				puppet.bindSoul(new PuppetSoul(PuppetSoul.Kind.CULTIVATOR, "Lin Test", "defying-the-heavens:cultivator", QiElement.FIRE, 0, List.of()));
				int alignment = CultivationManager.own(p).getAlignment();
				PuppetSouls.release(p, puppet);
				helper.assertTrue(puppet.soul() == null && CultivationManager.own(p).getAlignment() == alignment + PuppetSouls.RELEASE_SHIFT,
						"Released: hollow again, and a little righteousness back");
				// Copied whole: the real soul gone, the copy kept, only to be wiped.
				PuppetSoul wolf = other.soul();
				helper.assertTrue(wolf.copy(PuppetSouls.COPY_TICKS) && wolf.whole() && !wolf.real(), "Copied for an hour: whole");
				PuppetSouls.release(p, other);
				helper.assertTrue(other.soul() == wolf, "A whole copy can't be released");
				PuppetSouls.wipe(p, other);
				helper.assertTrue(other.soul() == null, "Wiped: the ring empty again");
			} finally {
				for (PuppetEntity e : new PuppetEntity[] {puppet, other}) {
					Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), e.getUUID());
					e.discard();
				}
				gone(p);
			}
			helper.succeed();
		});
	}

	/** A fresh body of {@code dead} laid at {@code rel} in the test's ground (its block entity), as Corpses#lay lays one. */
	private static CorpseBlockEntity bodyOf(GameTestHelper helper, net.minecraft.world.entity.LivingEntity dead, BlockPos rel) {
		helper.setBlock(rel, ModBlocks.CORPSE);
		CorpseBlockEntity body = (CorpseBlockEntity) helper.getBlockEntity(rel);
		body.of(dead, helper.getLevel().getGameTime());
		return body;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aCultivatorsBodyIsTakenUpWithItsMeridians(GameTestHelper helper) {
		// Puppetry phase 2b: a fresh cultivator's body taken up with the brush: a Body of it (a frame of its realm's grade), its board the
		// body's meridians, two broken; the meridians never wiped nor painted over, a break mended with a plain line. A player's body refused.
		ServerPlayer p = player(helper, "test-bodytaker");
		ServerLevel level = helper.getLevel();
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		try {
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID));
			BlockPos rel = new BlockPos(4, 2, 4);
			CorpseBlockEntity corpse = bodyOf(helper, npc, rel);
			CultivationManager.own(p).setAlignment(0);
			PuppetBodies.take(p, corpse, helper.getBlockState(rel), helper.absolutePos(rel));
			helper.assertTrue(!helper.getBlockState(rel).is(ModBlocks.CORPSE), "The body taken up: gone from the ground");
			ItemStack body = ItemStack.EMPTY;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (PuppetBodies.isBody(p.getInventory().getItem(i))) body = p.getInventory().getItem(i);
			helper.assertTrue(!body.isEmpty() && body.getItem() instanceof PuppetFrameItem frame && frame.grade() == ArtifactGrade.SPIRIT
					&& PuppetBodies.deadOf(body).rank() == npc.getRank(), "A Body of a Core Formation cultivator: a Spirit frame of flesh");
			helper.assertTrue(CultivationManager.own(p).getAlignment() == -PuppetBodies.TAKE_SHIFT, "A devilish deed");
			CircuitBoard board = PuppetFrameItem.board(body);
			int lines = 0, breaks = 0;
			int[] line = null, broken = null;
			for (int y = 0; y < board.size; y++) {
				for (int x = 0; x < board.size; x++) {
					if (!board.isMeridian(x, y)) continue;
					if (board.stroke(x, y) == CircuitBoard.Stroke.NONE) {
						breaks++;
						broken = new int[] {x, y};
					} else {
						lines++;
						line = new int[] {x, y};
					}
				}
			}
			helper.assertTrue(lines > 10 && breaks == PuppetBodies.FRESH_BREAKS, "Its meridians drawn, two broken fresh (" + lines + " lines, " + breaks + " breaks)");
			helper.assertTrue("meridian".equals(board.refusal(line[0], line[1], CircuitBoard.Stroke.NONE))
					&& "meridian".equals(board.refusal(line[0], line[1], CircuitBoard.Stroke.THICK)), "A meridian is never wiped nor painted over");
			helper.assertTrue("meridian_break".equals(board.refusal(broken[0], broken[1], CircuitBoard.Stroke.RELAY))
					&& board.refusal(broken[0], broken[1], CircuitBoard.Stroke.LINE) == null, "A break is mended with a plain line only");
			// A player's body is their own.
			BlockPos theirs = new BlockPos(6, 2, 4);
			CorpseBlockEntity players = bodyOf(helper, p, theirs);
			PuppetBodies.take(p, players, helper.getBlockState(theirs), helper.absolutePos(theirs));
			helper.assertTrue(helper.getBlockState(theirs).is(ModBlocks.CORPSE), "A player's body can't be taken");
		} finally {
			npc.discard();
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aBodyRefinesAndStandsAtTheDeadsRank(GameTestHelper helper) {
		// Laid on the bench, a body refines a day before it seals; its heart a core of at least the dead's realm; sealed, its puppet stands at
		// the dead's own rank at a cultivator's whole might, the board its meridians; taken apart, its body back; righteous cultivators
		// turn on its puppeteer.
		ServerPlayer p = player(helper, "test-bodymaker");
		ServerLevel level = helper.getLevel();
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		PuppetWorkbenchBlockEntity bench = (PuppetWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
		int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE);
		PuppetBodies.Dead dead = new PuppetBodies.Dead("Test Body", 3, "", 0, 1, rank, QiElement.WATER);
		ItemStack body = PuppetBodies.bodyItem(dead, 5L, 0, level.random);
		PuppetEntity puppet = null, frame = null;
		CultivatorNpc watcher = CultivatorNpc.createFor(level);
		try {
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.MASTER.from);
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, body.copy());
			bench.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND);
			helper.assertTrue(PuppetBodies.isBody(bench.frame()) && !PuppetBodies.ready(bench.frame(), level.getGameTime()), "Laid on the bench: refining");
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), null, null));
			bench.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND);
			helper.assertTrue(bench.core().isEmpty(), "A Qi Refining core is too weak for a Core Formation body");
			p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null));
			bench.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND);
			helper.assertTrue(!bench.core().isEmpty(), "A Core Formation core fits");
			p.getInventory().add(new ItemStack(ArtifactGrade.SPIRIT.stone(), Puppetry.SEAL_STONES));
			PuppetBoards.openBench(p, bench);
			PuppetBoards.act(p, PuppetBoards.SEAL);
			helper.assertTrue(!bench.frame().isEmpty(), "Not refined yet: it won't seal");
			// A day on: refined.
			bench.frame().getOrCreateTag().putLong(PuppetBodies.LAID_AT, level.getGameTime() - PuppetBodies.REFINE_TICKS);
			PuppetBoards.act(p, PuppetBoards.SEAL);
			List<PuppetEntity> made = level.getEntitiesOfClass(PuppetEntity.class, new AABB(helper.absolutePos(BENCH)).inflate(4), e -> e.isOwnedBy(p));
			helper.assertTrue(made.size() == 1 && bench.frame().isEmpty(), "Refined: sealed");
			puppet = made.get(0);
			helper.assertTrue(puppet.isBody() && puppet.rank() == rank && puppet.element() == QiElement.WATER, "At the dead's own rank (" + puppet.rank()
					+ " vs " + rank + "), its art the dead's element");
			helper.assertTrue(puppet.board().isMeridian(puppet.board().size / 2, puppet.board().size / 2 - 1) || puppet.meridians() != null,
					"Its board the body's meridians");
			// A frame puppet of the same rank (a Core Formation Late core): seven tenths of the body's might.
			frame = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT, 2, 6L), DemonCoreItem.of(rank, null, null), p,
					Vec3.atBottomCenterOf(helper.absolutePos(BENCH.offset(2, 1, 0))));
			helper.assertTrue(puppet.getMaxHealth() > frame.getMaxHealth() * 1.2, "A cultivator's whole might: " + puppet.getMaxHealth() + " vs a frame's "
					+ frame.getMaxHealth());
			// A righteous cultivator nearby takes its puppeteer for an enemy; without the body puppet, not.
			watcher.moveTo(p.getX() + 2, p.getY(), p.getZ(), 0, 0);
			watcher.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			watcher.setAlignment(150);
			watcher.setNoAi(true);
			level.addFreshEntity(watcher);
			CultivationManager.own(p).setAlignment(0);
			PuppetBodies.forget(p.getUUID());
			helper.assertTrue(watcher.isEnemy(p), "A righteous cultivator sees the body puppet: an enemy");
			// Taken apart: its body back, refined.
			puppet.dismantle();
			List<net.minecraft.world.entity.item.ItemEntity> dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new AABB(helper.absolutePos(BENCH)).inflate(4), e -> PuppetBodies.isBody(e.getItem()));
			helper.assertTrue(!dropped.isEmpty() && PuppetBodies.ready(dropped.get(0).getItem(), level.getGameTime()), "Taken apart: its body back, refined");
			for (net.minecraft.world.entity.item.ItemEntity e : dropped) e.discard();
			PuppetBodies.forget(p.getUUID());
			helper.assertTrue(!watcher.isEnemy(p), "No body puppet with it: no enemy");
		} finally {
			watcher.discard();
			for (PuppetEntity e : new PuppetEntity[] {puppet, frame}) {
				if (e == null) continue;
				Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), e.getUUID());
				e.discard();
			}
			PuppetBoards.forget(p.getUUID());
			gone(p);
		}
		helper.succeed();
	}

	/** A Heaven humanoid sealed for {@code p} with a Nascent Soul core, its board drawn as {@code drawn} (a flawless frame). */
	private static PuppetEntity sealedBoard(ServerLevel level, ServerPlayer p, Vec3 at, CircuitBoard drawn) {
		ItemStack frame = PuppetFrameItem.of(PuppetBlueprint.HUMANOID, ArtifactGrade.HEAVEN, Artifacts.Quality.PERFECT, 1L, null);
		PuppetFrameItem.keep(frame, drawn);
		return PuppetEntity.seal(level, frame, DemonCoreItem.of(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), null, null), p, at);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSealTakesItsToken(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-sealer");
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		PuppetWorkbenchBlockEntity bench = (PuppetWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
		bench.setFrame(PuppetFrameItem.of(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, Artifacts.Quality.PERFECT, 3L, null));
		try {
			p.getInventory().add(new ItemStack(ModItems.INSCRIPTION_BRUSH));
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.MASTER.from);
			PuppetBoards.openBench(p, bench);
			int c = CircuitBoard.sizeFor(0) / 2;
			PuppetBoards.paint(p, 1, c - 1, c, CircuitBoard.Stroke.SEAL_FIRE.ordinal());
			helper.assertTrue(PuppetFrameItem.board(bench.frame()).stroke(c - 1, c) == CircuitBoard.Stroke.NONE, "No blaze powder, no Fire seal");
			p.getInventory().add(new ItemStack(Items.BLAZE_POWDER, 2));
			PuppetBoards.paint(p, 2, c - 1, c, CircuitBoard.Stroke.SEAL_FIRE.ordinal());
			helper.assertTrue(PuppetFrameItem.board(bench.frame()).stroke(c - 1, c) == CircuitBoard.Stroke.SEAL_FIRE, "A Fire seal on the outlet");
			helper.assertTrue(p.getInventory().countItem(Items.BLAZE_POWDER) == 1, "Its blaze powder taken");
			p.getInventory().add(new ItemStack(Items.OAK_SAPLING));
			PuppetBoards.paint(p, 3, c - 1, c, CircuitBoard.Stroke.SEAL_WOOD.ordinal());
			helper.assertTrue(PuppetFrameItem.board(bench.frame()).stroke(c - 1, c) == CircuitBoard.Stroke.SEAL_WOOD, "A Wood seal over it, for a sapling");
		} finally {
			PuppetBoards.forget(p.getUUID());
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCoresStageAndTheRingGiveQi(GameTestHelper helper) {
		ItemStack early = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		ItemStack perfection = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION), null, null);
		helper.assertTrue(Puppetry.budget(ArtifactGrade.SPIRIT, early) == 18 && Puppetry.budget(ArtifactGrade.SPIRIT, perfection) == 21,
				"A Spirit core's qi: 18 at Early, 21 at Grand Perfection");
		helper.assertTrue(Puppetry.budget(ArtifactGrade.SPIRIT, ItemStack.EMPTY) == 18, "No core yet: the frame's own");
		// A Saint's Balanced path on a Spirit humanoid closes the resonance ring: three more.
		CircuitBoard board = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT.ordinal(), 0, 7L);
		board.lay(board.circuitPath(CraftMastery.Rank.SAINT.ordinal(), CircuitBoard.Build.BALANCED, 18));
		helper.assertTrue(board.ringClosed() && board.score(18).budget() == 18 + CircuitBoard.RING_QI, "The closed ring gives 3 more qi");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void anOverdrawnBoardWontSeal(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-overdrawn");
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		PuppetWorkbenchBlockEntity bench = (PuppetWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
		// Every free cell of a Mortal humanoid's board a line: every part fed on every side it has, far past a Mortal core's 14 qi.
		ItemStack frame = PuppetFrameItem.of(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, Artifacts.Quality.PERFECT, 5L, null);
		CircuitBoard board = PuppetFrameItem.board(frame);
		for (int y = 0; y < board.size; y++) for (int x = 0; x < board.size; x++) board.set(x, y, CircuitBoard.Stroke.LINE);
		PuppetFrameItem.keep(frame, board);
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), null, null);
		bench.setFrame(frame.copy());
		bench.setCore(core.copy());
		PuppetEntity puppet = null;
		try {
			helper.assertTrue(board.score(Puppetry.budget(ArtifactGrade.MORTAL, core)).over(), "Its veins want more than the core gives");
			PuppetBoards.openBench(p, bench);
			PuppetBoards.act(p, PuppetBoards.SEAL);
			helper.assertTrue(!bench.frame().isEmpty(), "Over its qi: the board won't seal, the frame stays on the bench");
			// A puppet made before the veins drew qi keeps its drawing: its nearest veins fed, every part alive, within its core's qi.
			puppet = PuppetEntity.seal(helper.getLevel(), frame, core, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())));
			CircuitBoard.Scores s = puppet.scores();
			helper.assertTrue(s.over() && s.draw() <= s.budget(), "Its farthest veins unfed: it draws " + s.draw() + " of " + s.budget());
			for (int n = 0; n < s.dead().length; n++) helper.assertTrue(!s.dead()[n] && s.veins()[n] >= 1, "Every part still fed");
		} finally {
			PuppetBoards.forget(p.getUUID());
			if (puppet != null) {
				Puppetry.Ledger.get(helper.getLevel().getServer()).remove(p.getUUID(), puppet.getUUID());
				puppet.discard();
			}
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void moreVeinsHitHarderAndBlowsHeat(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-brawler");
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		int rank = PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY);
		ItemStack brawlerFrame = PuppetFrameItem.of(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, Artifacts.Quality.PERFECT, 3L, null);
		CircuitBoard brawlerBoard = PuppetFrameItem.board(brawlerFrame);
		// (A Grandmaster's: a Saint's spends 5 qi on twin Guard Coils, phase C, and its arms get fewer veins on a Mortal frame.)
		brawlerBoard.lay(brawlerBoard.circuitPath(CraftMastery.Rank.GRANDMASTER.ordinal(), CircuitBoard.Build.BRAWLER, 14));
		PuppetFrameItem.keep(brawlerFrame, brawlerBoard);
		PuppetEntity brawler = PuppetEntity.seal(level, brawlerFrame, DemonCoreItem.of(rank, null, null), p, at);
		PuppetEntity crude = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, 0, 3L), DemonCoreItem.of(rank, null, null), p, at);
		Husk foe = EntityType.HUSK.create(level);
		try {
			int arm = node(brawler.board(), "left_arm");
			helper.assertTrue(brawler.scores().veins()[arm] == 4, "A Grandmaster's Brawler: four veins to each arm, was " + brawler.scores().veins()[arm]);
			double hard = brawler.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			double soft = hard / (Puppetry.flowFactor(brawler.scores().flow()) * Puppetry.ARM_STEP[3]);
			helper.assertTrue(hard > soft * 1.45, "Its blows x1.5 for four veins: " + hard + " vs " + soft);
			// Every blow heats a puppet, on a Mortal frame too: the Apprentice's crude lines stall it after a few.
			foe.moveTo(at.x + 1, at.y, at.z, 0, 0);
			level.addFreshEntity(foe);
			int bound = Puppetry.overheatAfter(crude.scores().stability());
			helper.assertTrue(bound < Integer.MAX_VALUE, "The trunk path runs hot: Stability " + crude.scores().stability());
			for (int k = 0; k <= bound && !crude.isOverheated(); k++) crude.doHurtTarget(foe);
			helper.assertTrue(crude.isOverheated(), "Past " + bound + " heat its blows stall it");
			// A Grandmaster's lines carry cool: thirty blows and it never stalls.
			helper.assertTrue(brawler.scores().stability() >= 85, "The Grandmaster's Brawler carries cool");
			for (int k = 0; k < 30; k++) brawler.doHurtTarget(foe);
			helper.assertTrue(!brawler.isOverheated(), "From Stability 85 it never overheats");
		} finally {
			foe.discard();
			for (PuppetEntity puppet : new PuppetEntity[] {brawler, crude}) {
				Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
				puppet.discard();
			}
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void flawsByForgingFixedBySeed(GameTestHelper helper) {
		for (PuppetBlueprint blueprint : PuppetBlueprint.values()) {
			for (int grade = 0; grade < 5; grade++) {
				for (int quality = 0; quality < 3; quality++) {
					for (long seed = 1; seed <= 6; seed++) {
						CircuitBoard a = new CircuitBoard(blueprint, grade, CircuitBoard.flawsFor(quality), seed);
						CircuitBoard b = new CircuitBoard(blueprint, grade, CircuitBoard.flawsFor(quality), seed);
						int flaws = 0, c = a.size / 2;
						for (int y = 0; y < a.size; y++) {
							for (int x = 0; x < a.size; x++) {
								helper.assertTrue(a.isFlaw(x, y) == b.isFlaw(x, y), "The same seed, the same flaws");
								if (!a.isFlaw(x, y)) continue;
								flaws++;
								helper.assertTrue(Math.max(Math.abs(x - c), Math.abs(y - c)) > 2, "No flaw by the core");
							}
						}
						helper.assertTrue(flaws == CircuitBoard.flawsFor(quality), blueprint.getId() + ": " + CircuitBoard.flawsFor(quality) + " flaws, was " + flaws);
						// Every node can still be fed: the Saint's path laid on it (round the flaws) feeds them all, or a full board does.
						CircuitBoard full = a.copy();
						for (int y = 0; y < a.size; y++) for (int x = 0; x < a.size; x++) full.set(x, y, CircuitBoard.Stroke.LINE);
						for (boolean dead : full.score().dead()) helper.assertTrue(!dead, "No node cut off by a flaw");
					}
				}
			}
		}
		helper.assertTrue(CircuitBoard.flawsFor(0) == 3 && CircuitBoard.flawsFor(1) == 1 && CircuitBoard.flawsFor(2) == 0, "3 flaws Normal, 1 Fine, none Perfect");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void framesAtTheForgeWithoutACore(GameTestHelper helper) {
		for (ArtifactGrade grade : ArtifactGrade.values()) {
			for (ArtifactKind kind : new ArtifactKind[] {ArtifactKind.HUMANOID_FRAME, ArtifactKind.BEE_FRAME}) {
				ArtifactRecipes.Recipe recipe = ArtifactRecipes.forOutput(ModItems.artifact(kind, grade));
				helper.assertTrue(recipe != null && !recipe.core() && !recipe.token(), kind.getId() + " at " + grade.getId() + ": forged, no core");
				helper.assertTrue(recipe.materials().containsKey(ArtifactRecipes.frameBody(grade)), "Of its grade's matter");
				ItemStack frame = Artifacts.create(kind, grade, Artifacts.Quality.FINE, null);
				helper.assertTrue(frame.getItem() instanceof PuppetFrameItem item && item.blueprint() == kind.blueprint() && item.grade() == grade, "A frame");
				helper.assertTrue(PuppetFrameItem.flaws(frame) == 1, "A Fine frame has one flaw");
				helper.assertTrue(Artifacts.gradeOf(frame) == grade && Artifacts.priceOf(frame) > 0, "Priced as an artifact");
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCoreSetsTheRealmWithinTheFrame(GameTestHelper helper) {
		int qr = PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), fbTop = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		int cf = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE), ns = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
		helper.assertTrue(Puppetry.puppetRank(ArtifactGrade.MORTAL, qr) == qr, "A Mortal frame stands at its core's Qi Refining");
		helper.assertTrue(Puppetry.puppetRank(ArtifactGrade.MORTAL, cf) == fbTop, "A core too strong runs at the frame's top");
		helper.assertTrue(Puppetry.puppetRank(ArtifactGrade.SPIRIT, qr) < 0, "A core too weak won't go in");
		helper.assertTrue(Puppetry.puppetRank(ArtifactGrade.SPIRIT, cf) == cf, "A Spirit frame at Core Formation");
		helper.assertTrue(Puppetry.puppetRank(ArtifactGrade.EARTH, ns) == ns, "An Earth frame from Nascent Soul");
		int sv = PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.EARLY), ei = PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.MID);
		helper.assertTrue(Puppetry.puppetRank(ArtifactGrade.IMMORTAL, sv) == sv, "An Immortal frame from Sacred Vessel");
		helper.assertTrue(Puppetry.puppetRank(ArtifactGrade.IMMORTAL, ei) == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION),
				"No puppet past True Immortal");
		helper.assertTrue(Puppetry.cost(ArtifactGrade.MORTAL) == 1 && Puppetry.cost(ArtifactGrade.IMMORTAL) == 5, "A puppet weighs its frame's grade");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSealedPuppetStandsAtItsCoresRealm(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-puppeteer");
		ServerLevel level = helper.getLevel();
		int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID);
		ItemStack frame = drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT, 4, 11L);
		ItemStack core = DemonCoreItem.of(rank, QiElement.FIRE, null);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		PuppetEntity puppet = PuppetEntity.seal(level, frame, core, p, at);
		try {
			helper.assertTrue(puppet.isAlive() && puppet.isOwnedBy(p), "It stands, its puppeteer's");
			helper.assertTrue(puppet.rank() == rank, "At its core's realm and stage");
			helper.assertTrue(puppet.scores().grade() == CircuitBoard.Grade.PERFECT, "Its circuits as drawn: Perfect");
			double health = (CultivationStats.maxHealth(Realm.CORE_FORMATION, Stage.MID) + 20) * Puppetry.MIGHT; // its base 20 and the realm's share
			helper.assertTrue(Math.abs(puppet.getMaxHealth() - health) < 1, "Seven tenths of a cultivator's body: " + health + ", was " + puppet.getMaxHealth());
			helper.assertTrue(!puppet.isInert() && puppet.artReady(), "Awake, its art ready (a Spirit frame, its mouth fed)");
			helper.assertTrue(puppet.element() == QiElement.FIRE, "Its art its core's element");
			Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
			helper.assertTrue(ledger.used(p.getUUID()) == 2, "A Spirit puppet weighs two slots");
			helper.assertTrue(ledger.get(p.getUUID(), puppet.getUUID()) != null, "On its puppeteer's ledger");

			// A fifth of its health lost cuts one line; a blow of Metal qi cuts another, and strikes half again as hard.
			Husk foe = EntityType.HUSK.create(level);
			foe.moveTo(at.x + 2, at.y, at.z, 0, 0);
			level.addFreshEntity(foe);
			float before = puppet.getHealth();
			// (Harm with no one behind it: a Perfect puppet dodges a blow now and then.)
			puppet.hurt(level.damageSources().generic(), puppet.getMaxHealth() / 5 + 0.5f);
			helper.assertTrue(puppet.cuts().length == 1, "A fifth of its health: one line cut, was " + puppet.cuts().length);
			puppet.invulnerableTime = 0;
			float mid = puppet.getHealth();
			puppet.hurt(ModDamageTypes.qi(level, QiElement.METAL, Realm.CORE_FORMATION, null, null), 2);
			helper.assertTrue(puppet.cuts().length >= 2, "Metal qi cuts a line besides");
			helper.assertTrue(mid - puppet.getHealth() > 2.5f || puppet.getHealth() < mid, "Metal strikes it harder");
			helper.assertTrue(before > mid, "It was hurt");
			foe.discard();

			// Its owner's blows pass through it.
			puppet.invulnerableTime = 0;
			float now = puppet.getHealth();
			puppet.hurt(level.damageSources().playerAttack(p), 5);
			helper.assertTrue(puppet.getHealth() == now, "Its puppeteer's blow passes through");

			// Taken apart: its frame (drawing and all) and its core where it stood; off the ledger.
			Vec3 where = puppet.position();
			puppet.dismantle();
			List<ItemEntity> dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(where, where).inflate(2));
			boolean gotFrame = false, gotCore = false;
			for (ItemEntity item : dropped) {
				if (item.getItem().getItem() instanceof PuppetFrameItem) gotFrame = PuppetFrameItem.strokes(item.getItem()) != null;
				if (item.getItem().is(ModItems.DEMON_CORE)) gotCore = DemonCoreItem.rank(item.getItem()) == rank;
				item.discard();
			}
			helper.assertTrue(gotFrame && gotCore, "Its frame, drawn, and its core given back");
			helper.assertTrue(ledger.used(p.getUUID()) == 0, "Off the ledger");
		} finally {
			if (puppet.isAlive()) puppet.discard();
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aHeartCutOffGoesInert(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-inert");
		ServerLevel level = helper.getLevel();
		// The Apprentice's trunk path: one way to each node.
		ItemStack frame = drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, 0, 5L);
		PuppetEntity puppet = PuppetEntity.seal(level, frame, DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), null, null), p,
				Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())));
		try {
			CircuitBoard board = puppet.board(), whole = puppet.board();
			PuppetBlueprint.Node heart = null;
			for (PuppetBlueprint.Node node : board.nodes()) if (node.kind() == PuppetBlueprint.NodeKind.SEAL) heart = node;
			helper.assertTrue(heart != null && !puppet.isInert(), "Fed, it hears its puppeteer");
			// Every line touching the heart cut.
			int[][] sides = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
			for (int[] d : sides) if (board.isFree(heart.x() + d[0], heart.y() + d[1])) board.set(heart.x() + d[0], heart.y() + d[1], CircuitBoard.Stroke.NONE);
			puppet.boardChanged(board);
			helper.assertTrue(puppet.isInert(), "Its heart cut off: inert");
			helper.assertTrue(!puppet.artReady(), "A Mortal frame has no art");
			puppet.boardChanged(whole);
			helper.assertTrue(!puppet.isInert(), "Mended: it hears again");
		} finally {
			puppet.discard();
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void paintingAtTheBench(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-painter");
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		PuppetWorkbenchBlockEntity bench = (PuppetWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
		ItemStack frame = PuppetFrameItem.of(PuppetBlueprint.BEE, ArtifactGrade.MORTAL, Artifacts.Quality.PERFECT, 3L, null);
		bench.setFrame(frame);
		ItemStack brush = new ItemStack(ModItems.INSCRIPTION_BRUSH);
		p.getInventory().add(brush);
		try {
			PuppetBoards.openBench(p, bench);
			int c = CircuitBoard.sizeFor(0) / 2;
			// A plain line on the outlet above the core: one stroke of the brush.
			PuppetBoards.paint(p, 1, c, c - 1, CircuitBoard.Stroke.LINE.ordinal());
			CircuitBoard board = PuppetFrameItem.board(bench.frame());
			helper.assertTrue(board.stroke(c, c - 1) == CircuitBoard.Stroke.LINE, "Painted");
			helper.assertTrue(brushWorn(p) == 1, "The brush a stroke more worn, was " + brushWorn(p));
			// A thick line wants a Master; an Apprentice is refused, and the brush untouched.
			PuppetBoards.paint(p, 2, c, c + 1, CircuitBoard.Stroke.THICK.ordinal());
			helper.assertTrue(PuppetFrameItem.board(bench.frame()).stroke(c, c + 1) == CircuitBoard.Stroke.NONE && brushWorn(p) == 1, "Refused below its rank");
			// A Grandmaster's cooling vent wants an iron ingot besides.
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.GRANDMASTER.from);
			PuppetBoards.paint(p, 3, 0, c + 3, CircuitBoard.Stroke.VENT.ordinal());
			helper.assertTrue(PuppetFrameItem.board(bench.frame()).stroke(0, c + 3) == CircuitBoard.Stroke.NONE, "No iron, no vent");
			p.getInventory().add(new ItemStack(Items.IRON_INGOT));
			PuppetBoards.paint(p, 4, 0, c + 3, CircuitBoard.Stroke.VENT.ordinal());
			helper.assertTrue(PuppetFrameItem.board(bench.frame()).stroke(0, c + 3) == CircuitBoard.Stroke.VENT, "A vent, its iron taken");
			helper.assertTrue(p.getInventory().countItem(Items.IRON_INGOT) == 0, "Its iron taken");
			// Wiping costs nothing.
			int worn = brushWorn(p);
			PuppetBoards.paint(p, 5, c, c - 1, CircuitBoard.Stroke.NONE.ordinal());
			helper.assertTrue(PuppetFrameItem.board(bench.frame()).stroke(c, c - 1) == CircuitBoard.Stroke.NONE && brushWorn(p) == worn, "Wiped for nothing");
			// Sealing with nothing feeding the heart is refused: the frame stays.
			PuppetBoards.act(p, PuppetBoards.SEAL);
			helper.assertTrue(!bench.frame().isEmpty(), "No core, no seal");
		} finally {
			PuppetBoards.forget(p.getUUID());
			gone(p);
		}
		helper.succeed();
	}

	private static int brushWorn(ServerPlayer p) {
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
			ItemStack stack = p.getInventory().getItem(i);
			if (stack.is(ModItems.INSCRIPTION_BRUSH)) return stack.getDamageValue();
		}
		return -1;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void seizingWantsAHigherRank(GameTestHelper helper) {
		ServerPlayer owner = player(helper, "test-owner"), thief = player(helper, "test-thief");
		ServerLevel level = helper.getLevel();
		PuppetEntity normal = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, 1, 9L),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), null, null), owner, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())));
		PuppetEntity perfect = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, 4, 9L),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), null, null), owner, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())));
		try {
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, normal) != null, "An equal rank can't seize");
			// The rank a seizer wants: one above its maker's (an Apprentice's here), two for Fine circuits, three for Perfect, and one more for
			// each vein to its heart past the first (the deeper circuits, phase A).
			helper.assertTrue(normal.scores().grade().ordinal() <= CircuitBoard.Grade.NORMAL.ordinal(), "Its circuits Normal at most");
			int forNormal = 1 + normal.heartVeins() - 1, forPerfect = 3 + perfect.heartVeins() - 1;
			helper.assertTrue(perfect.scores().grade() == CircuitBoard.Grade.PERFECT && forPerfect > forNormal && forPerfect <= 4,
					"A Saint's layout Perfect, its heart fed " + perfect.heartVeins() + " ways");
			CultivationManager.own(thief).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.values()[forNormal].from);
			// Its seal (phase 2c): bound at Mortal (its puppeteer carried none), so a seal above Mortal in hand, and only that, takes it.
			helper.assertTrue(normal.sealGrade() == ArtifactGrade.MORTAL, "Sealed with no Command Seal carried: bound at Mortal");
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, normal) != null, "No seal in hand: nothing to seize with");
			thief.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, CommandSealItem.of(ArtifactGrade.MORTAL));
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, normal) != null, "A Mortal seal doesn't take a puppet bound at Mortal");
			thief.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, CommandSealItem.of(ArtifactGrade.SPIRIT));
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, normal) == null, "Its rank above seizes the Normal puppet");
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, perfect) != null, "A Perfect puppet's seal holds against it");
			CultivationManager.own(thief).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.values()[forPerfect - 1].from);
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, perfect) != null, "And against a rank short of what it wants");
			CultivationManager.own(thief).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.values()[forPerfect].from);
			helper.assertTrue(PuppetCommands.seizeRefusal(thief, perfect) == null, "Its full rank takes it");
			helper.assertTrue(PuppetCommands.seizeTicks(perfect) > PuppetCommands.seizeTicks(normal) || perfect.scores().clarity() == normal.scores().clarity(),
					"The clearer its circuits the longer it takes");
			PuppetCommands.seize(thief, perfect);
			helper.assertTrue(perfect.isOwnedBy(thief), "Seized");
			helper.assertTrue(perfect.sealGrade() == ArtifactGrade.SPIRIT, "Bound to its seizer's seal");
			Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
			helper.assertTrue(ledger.get(thief.getUUID(), perfect.getUUID()) != null && ledger.get(owner.getUUID(), perfect.getUUID()) == null, "The ledgers changed hands");
		} finally {
			Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
			for (PuppetEntity puppet : new PuppetEntity[] {normal, perfect}) {
				ledger.remove(owner.getUUID(), puppet.getUUID());
				ledger.remove(thief.getUUID(), puppet.getUUID());
				puppet.discard();
			}
			gone(owner, thief);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void aBeeCarriesBetweenTwoChests(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-beekeeper");
		ServerLevel level = helper.getLevel();
		BlockPos from = new BlockPos(1, 1, 1), to = new BlockPos(5, 1, 1);
		helper.setBlock(from, Blocks.CHEST);
		helper.setBlock(to, Blocks.CHEST);
		Container source = HopperBlockEntity.getContainerAt(level, helper.absolutePos(from));
		source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
		PuppetEntity bee = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.BEE, ArtifactGrade.MORTAL, 4, 21L),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), null, null), p, Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 2))));
		bee.command(PuppetEntity.Order.WORK, helper.absolutePos(from), helper.absolutePos(to));
		helper.succeedWhen(() -> {
			Container target = HopperBlockEntity.getContainerAt(level, helper.absolutePos(to));
			helper.assertTrue(target != null && target.countItem(Items.COBBLESTONE) == 32, "The bee carried the stones across");
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), bee.getUUID());
			bee.discard();
			gone(p);
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aFrozenPuppetStandsStill(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-frozen");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, 2, 4L),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), null, null), p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())));
		helper.assertTrue(TalismanPattern.CIRCUIT_FREEZE.item() == ModItems.CIRCUIT_FREEZE_TALISMAN && TalismanPattern.CIRCUIT_FREEZE.use == TalismanPattern.Use.THROWN,
				"The Circuit-Freeze Talisman is thrown");
		puppet.freeze(20);
		helper.assertTrue(puppet.isFrozen() && puppet.isStill(), "Frozen, it stands still");
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(!puppet.isFrozen(), "Free again once its time is up");
			puppet.discard();
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
			gone(p);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void diagramsAreCopiedAndFragmentsJoined(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-copyist");
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		PuppetWorkbenchBlockEntity bench = (PuppetWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
		bench.setFrame(drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.EARTH, 3, 8L));
		try {
			p.getInventory().add(new ItemStack(ModItems.INSCRIPTION_BRUSH));
			PuppetBoards.openBench(p, bench);
			PuppetBoards.act(p, PuppetBoards.COPY);
			helper.assertTrue(p.getInventory().countItem(ModItems.CIRCUIT_DIAGRAM) == 0, "No paper, no copy");
			p.getInventory().add(new ItemStack(Items.PAPER));
			PuppetBoards.act(p, PuppetBoards.COPY);
			ItemStack diagram = ItemStack.EMPTY;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(ModItems.CIRCUIT_DIAGRAM)) diagram = p.getInventory().getItem(i);
			helper.assertTrue(!diagram.isEmpty() && CircuitDiagramItem.fits(diagram, PuppetBlueprint.HUMANOID, ArtifactGrade.EARTH), "A diagram of the board");
			byte[] copied = CircuitDiagramItem.strokes(diagram), drawn = PuppetFrameItem.strokes(bench.frame());
			helper.assertTrue(java.util.Arrays.equals(copied, drawn), "Its drawing exactly");
			helper.assertTrue(brushWorn(p) == PuppetBoards.COPY_WEAR, "Four strokes of the brush");
			// Four quarters of an ancient diagram join into it; three don't.
			net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
			p.setItemInHand(hand, CircuitFragmentItem.of(PuppetBlueprint.BEE, ArtifactGrade.HEAVEN, 0));
			for (int q = 1; q < 3; q++) p.getInventory().add(CircuitFragmentItem.of(PuppetBlueprint.BEE, ArtifactGrade.HEAVEN, q));
			p.getMainHandItem().use(helper.getLevel(), p, hand);
			helper.assertTrue(p.getInventory().countItem(ModItems.CIRCUIT_FRAGMENT) == 3, "Three quarters stay apart");
			p.getInventory().add(CircuitFragmentItem.of(PuppetBlueprint.BEE, ArtifactGrade.HEAVEN, 3));
			p.getMainHandItem().use(helper.getLevel(), p, hand);
			helper.assertTrue(p.getInventory().countItem(ModItems.CIRCUIT_FRAGMENT) == 0, "Four join");
			ItemStack ancient = ItemStack.EMPTY;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
				ItemStack stack = p.getInventory().getItem(i);
				if (stack.is(ModItems.CIRCUIT_DIAGRAM) && CircuitDiagramItem.isAncient(stack)) ancient = stack;
			}
			helper.assertTrue(CircuitDiagramItem.fits(ancient, PuppetBlueprint.BEE, ArtifactGrade.HEAVEN), "The ancient diagram of their frame");
			CircuitBoard board = new CircuitBoard(PuppetBlueprint.BEE, ArtifactGrade.HEAVEN.ordinal(), 0, 0);
			Puppetry.lay(board, CircuitDiagramItem.strokes(ancient));
			helper.assertTrue(board.score().grade() == CircuitBoard.Grade.PERFECT, "A Saint's layout: Perfect");
		} finally {
			PuppetBoards.forget(p.getUUID());
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void wanderingPuppeteersFieldPuppets(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(NpcPuppets.gradeFor(Realm.QI_REFINING) == ArtifactGrade.MORTAL && NpcPuppets.gradeFor(Realm.CORE_FORMATION) == ArtifactGrade.SPIRIT
				&& NpcPuppets.gradeFor(Realm.HEAVENLY_BEING) == ArtifactGrade.EARTH && NpcPuppets.gradeFor(Realm.DAO_INTEGRATION) == ArtifactGrade.HEAVEN
				&& NpcPuppets.gradeFor(Realm.TRUE_IMMORTAL) == ArtifactGrade.IMMORTAL, "The frame a cultivator's realm makes");
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
		npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID);
		npc.becomeRogue(rank, Alignment.randomRogue(level.random), level.random);
		npc.setNoAi(true);
		level.addFreshEntity(npc);
		NpcPuppets.field(level, npc, level.random);
		List<PuppetEntity> puppets = level.getEntitiesOfClass(PuppetEntity.class, new AABB(at).inflate(6), e -> npc.getUUID().equals(e.getOwnerUUID()));
		try {
			helper.assertTrue(!puppets.isEmpty() && puppets.size() <= 2, "One or two puppets at its side");
			for (PuppetEntity puppet : puppets) {
				helper.assertTrue(puppet.frameGrade() == ArtifactGrade.SPIRIT && !puppet.ownedByPlayer(), "A Spirit frame, a cultivator's");
				helper.assertTrue(puppet.rank() <= npc.getRank() && puppet.rank() >= PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), "Its core at or below its maker");
				helper.assertTrue(puppet.getOwner() == npc, "It knows its maker");
			}
		} finally {
			for (PuppetEntity puppet : puppets) puppet.discard();
			npc.discard();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aNodePassesTheQiOn(GameTestHelper helper) {
		// A Spirit humanoid's board (11 x 11): the mouth over the up outlet, the head two above the mouth.
		CircuitBoard board = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT.ordinal(), 0, 1L);
		int c = board.size / 2, head = -1, mouth = -1;
		for (int n = 0; n < board.nodes().size(); n++) {
			if (board.nodes().get(n).id().equals("head")) head = n;
			if (board.nodes().get(n).id().equals("mouth")) mouth = n;
		}
		board.paint(c, c - 1, CircuitBoard.Stroke.LINE);
		CircuitBoard.Scores s = board.score();
		helper.assertTrue(!s.dead()[mouth] && s.dead()[head], "The outlet feeds the mouth; nothing reaches the head yet");
		board.paint(c, 2, CircuitBoard.Stroke.LINE);
		s = board.score();
		helper.assertTrue(!s.dead()[mouth] && !s.dead()[head], "A line from the mouth to the head: fed straight through the mouth");
		// A bee's (9 x 9): its head right against its heart, over the up outlet: one stroke feeds both.
		CircuitBoard bee = new CircuitBoard(PuppetBlueprint.BEE, ArtifactGrade.MORTAL.ordinal(), 0, 1L);
		int bc = bee.size / 2;
		bee.paint(bc, bc - 1, CircuitBoard.Stroke.LINE);
		CircuitBoard.Scores b = bee.score();
		for (int n = 0; n < bee.nodes().size(); n++) {
			String id = bee.nodes().get(n).id();
			if (id.equals("heart") || id.equals("head")) helper.assertTrue(!b.dead()[n], "The bee's " + id + " fed through its heart");
		}
		helper.assertTrue(b.sealFed(), "Its heart of command fed");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anEmptiedBenchReachesItsWatchers(GameTestHelper helper) {
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		PuppetWorkbenchBlockEntity bench = (PuppetWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
		bench.setFrame(drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, 1, 2L));
		helper.assertTrue(bench.getUpdateTag().contains("Frame"), "Its frame sent to the clients");
		bench.clear();
		// An empty update tag is never sent (the packet drops it): the clients kept drawing the frame and core after sealing.
		helper.assertTrue(!bench.getUpdateTag().isEmpty(), "An emptied bench still sends word of it");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aLeftClickTakesTheFrameAndCoreBack(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-taker");
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		PuppetWorkbenchBlockEntity bench = (PuppetWorkbenchBlockEntity) helper.getBlockEntity(BENCH);
		try {
			p.getInventory().clearContent();
			bench.setFrame(drawnFrame(PuppetBlueprint.BEE, ArtifactGrade.MORTAL, 1, 6L));
			bench.setCore(DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), null, null));
			// The hook a left-click on the bench runs (Fabric's AttackBlockCallback, as Puppetry registers it): taken back, the bench unbroken.
			net.minecraft.world.InteractionResult result = net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.invoker()
					.interact(p, helper.getLevel(), net.minecraft.world.InteractionHand.MAIN_HAND, helper.absolutePos(BENCH), net.minecraft.core.Direction.UP);
			helper.assertTrue(result == net.minecraft.world.InteractionResult.SUCCESS, "The click taken by the bench (no breaking begun)");
			helper.assertTrue(bench.core().isEmpty() && bench.frame().isEmpty() && p.getInventory().countItem(ModItems.DEMON_CORE) == 1,
					"Both taken back at once");
			helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(BENCH)).is(ModBlocks.PUPPET_WORKBENCH), "The bench still stands");
			helper.assertTrue(net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.invoker().interact(p, helper.getLevel(),
					net.minecraft.world.InteractionHand.MAIN_HAND, helper.absolutePos(BENCH), net.minecraft.core.Direction.UP) == net.minecraft.world.InteractionResult.PASS,
					"An empty bench mines as any other");
			ItemStack frame = ItemStack.EMPTY;
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof PuppetFrameItem) frame = p.getInventory().getItem(i);
			helper.assertTrue(PuppetFrameItem.strokes(frame) != null, "Its drawing kept on it");
		} finally {
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCraftCommandSetsMastery(GameTestHelper helper) {
		ServerPlayer p = player(helper, "test-crafter");
		try {
			var source = p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
			p.server.getCommands().performPrefixedCommand(source, "cultivation craft puppetry master");
			helper.assertTrue(CultivationManager.own(p).getMastery(CraftMastery.Craft.PUPPETRY) == CraftMastery.Rank.MASTER.from, "Puppetry set to Master");
			p.server.getCommands().performPrefixedCommand(source, "cultivation craft alchemy add 25");
			helper.assertTrue(CraftMastery.rank(CultivationManager.own(p), CraftMastery.Craft.ALCHEMY) == CraftMastery.Rank.ADEPT, "Alchemy raised to Adept");
		} finally {
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBoardIsKeptOnTheFrame(GameTestHelper helper) {
		ItemStack frame = drawnFrame(PuppetBlueprint.BEE, ArtifactGrade.HEAVEN, 3, 77L);
		CircuitBoard board = PuppetFrameItem.board(frame);
		CircuitBoard again = PuppetFrameItem.board(frame.copy());
		Set<Integer> differ = new HashSet<>();
		for (int y = 0; y < board.size; y++) for (int x = 0; x < board.size; x++) if (board.stroke(x, y) != again.stroke(x, y)) differ.add(board.at(x, y));
		helper.assertTrue(differ.isEmpty() && board.size == CircuitBoard.sizeFor(ArtifactGrade.HEAVEN.ordinal()), "The drawing travels with the frame");
		helper.assertTrue(board.score().grade().ordinal() >= CircuitBoard.Grade.FINE.ordinal(), "A Grandmaster's drawing kept whole");
		helper.succeed();
	}

	// --- Phase 2c: ranks of command ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSealIsRaisedAtTheBench(GameTestHelper helper) {
		// Raised a grade at a Puppet Workbench (sneaking) for 4 stones of the new grade, as high as the holder's Puppetry: a puppet bound to
		// the best seal its puppeteer carries; a cultivator's puppet bound at Spirit; a better seal's order binds it higher.
		ServerPlayer p = player(helper, "test-sealsmith");
		ServerLevel level = helper.getLevel();
		helper.setBlock(BENCH, ModBlocks.PUPPET_WORKBENCH);
		BlockPos bench = helper.absolutePos(BENCH);
		ItemStack seal = CommandSealItem.of(ArtifactGrade.MORTAL);
		p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, seal);
		PuppetEntity puppet = null, npcs = null;
		try {
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.ADEPT.from);
			CommandSealItem.raise(p, seal);
			helper.assertTrue(CommandSealItem.grade(seal) == ArtifactGrade.MORTAL, "No stones: not raised");
			p.getInventory().add(new ItemStack(ModItems.MID_SPIRIT_STONE, Puppetry.SEAL_RAISE_STONES));
			CommandSealItem.raise(p, seal);
			helper.assertTrue(CommandSealItem.grade(seal) == ArtifactGrade.SPIRIT && p.getInventory().countItem(ModItems.MID_SPIRIT_STONE) == 0,
					"Raised to Spirit for 4 Mid Spirit Stones");
			p.getInventory().add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, Puppetry.SEAL_RAISE_STONES));
			CommandSealItem.raise(p, seal);
			helper.assertTrue(CommandSealItem.grade(seal) == ArtifactGrade.SPIRIT, "An Adept's seal goes no higher than Spirit");
			// As a Master, sneaking on the bench with it (the item's own use on the bench).
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.MASTER.from);
			seal.useOn(new net.minecraft.world.item.context.UseOnContext(p, net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(bench), net.minecraft.core.Direction.UP, bench, false)));
			helper.assertTrue(CommandSealItem.grade(seal) == ArtifactGrade.EARTH && p.getInventory().countItem(ModItems.HIGH_SPIRIT_STONE) == 0,
					"Raised at the bench to Earth");
			helper.assertTrue(CommandSealItem.best(p) == ArtifactGrade.EARTH, "The best seal carried");
			// Sealed with it carried: bound at Earth.
			puppet = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())),
					DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 61L);
			helper.assertTrue(puppet.sealGrade() == ArtifactGrade.EARTH, "Bound to the best seal its puppeteer carried (" + puppet.sealGrade() + ")");
			helper.assertTrue(!puppet.bindSeal(ArtifactGrade.SPIRIT) && puppet.bindSeal(ArtifactGrade.HEAVEN) && puppet.sealGrade() == ArtifactGrade.HEAVEN,
					"Never bound lower; a better seal binds it higher");
			// Kept through saving and loading.
			net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
			puppet.addAdditionalSaveData(saved);
			PuppetEntity copy = new PuppetEntity(ModEntities.PUPPET_HUMANOID, level);
			copy.readAdditionalSaveData(saved);
			helper.assertTrue(copy.sealGrade() == ArtifactGrade.HEAVEN, "Its seal's grade kept");
			// A cultivator's puppet: Spirit.
			npcs = new PuppetEntity(ModEntities.PUPPET_HUMANOID, level);
			npcs.setOwner(UUID.randomUUID(), "Lin Test", false);
			helper.assertTrue(npcs.sealGrade() == ArtifactGrade.SPIRIT, "A cultivator's puppet is bound at Spirit");
		} finally {
			if (puppet != null) {
				Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
				puppet.discard();
			}
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSubSealLendsAPuppet(GameTestHelper helper) {
		// Written for one puppet: its holder takes it (following them, sparing them), cycles it to guard and hold; its puppeteer's order
		// takes it back; revoked, the seal is void.
		ServerPlayer p = player(helper, "test-lender"), q = player(helper, "test-borrower");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 62L);
		try {
			ItemStack sub = SubSealItem.of(puppet, p);
			helper.assertTrue(puppet.getUUID().equals(SubSealItem.puppet(sub)) && SubSealItem.holds(sub, puppet), "Bound to its puppet");
			net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
			q.setItemInHand(hand, sub);
			sub.interactLivingEntity(q, puppet, hand);
			helper.assertTrue(q.getUUID().equals(puppet.commander()) && puppet.order() == PuppetEntity.Order.FOLLOW && puppet.leader() == q,
					"Its holder takes it: it follows them");
			float health = puppet.getHealth();
			helper.assertTrue(!puppet.hurt(level.damageSources().playerAttack(q), 4) && puppet.getHealth() == health, "Its holder's blows pass through it");
			sub.interactLivingEntity(q, puppet, hand);
			helper.assertTrue(puppet.order() == PuppetEntity.Order.GUARD && q.blockPosition().equals(puppet.post()), "Again: it guards where they stand");
			sub.interactLivingEntity(q, puppet, hand);
			helper.assertTrue(puppet.order() == PuppetEntity.Order.HOLD, "Again: it holds");
			sub.interactLivingEntity(q, puppet, hand);
			helper.assertTrue(puppet.order() == PuppetEntity.Order.FOLLOW, "Again: it follows");
			// Its puppeteer's own order takes it back.
			PuppetCommands.act(p, puppet.getUUID(), PuppetCommands.HOLD, "", "");
			helper.assertTrue(puppet.commander() == null && puppet.order() == PuppetEntity.Order.HOLD && puppet.leader() == p, "Its puppeteer's order takes it back");
			sub.interactLivingEntity(q, puppet, hand);
			helper.assertTrue(q.getUUID().equals(puppet.commander()), "The Sub-Seal still good for another time");
			// Revoked: void, and let go.
			PuppetCommands.act(p, puppet.getUUID(), PuppetCommands.REVOKE, "", "");
			helper.assertTrue(puppet.commander() == null && !SubSealItem.holds(sub, puppet), "Revoked: let go, the Sub-Seal void");
			sub.interactLivingEntity(q, puppet, hand);
			helper.assertTrue(puppet.commander() == null, "A void Sub-Seal holds it no more");
			// Written on paper from the page.
			p.getInventory().add(new ItemStack(Items.PAPER));
			PuppetCommands.act(p, puppet.getUUID(), PuppetCommands.SUB_SEAL, "", "");
			helper.assertTrue(p.getInventory().countItem(Items.PAPER) == 0 && p.getInventory().countItem(ModItems.SUB_SEAL) == 1, "Written on a sheet of paper");
		} finally {
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), puppet.getUUID());
			puppet.discard();
			gone(p, q);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aGeneralMustersAndDeploys(GameTestHelper helper) {
		// A Grandmaster's Earth humanoid made a General for 8 High Spirit Stones: its puppeteer's puppets near it taken into its ranks (out of
		// the world, kept on the list), set down again round it, and let out where it falls.
		ServerPlayer p = player(helper, "test-marshal");
		ServerLevel level = helper.getLevel();
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		PuppetEntity general = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.EARTH, 4, 63L),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), null, null), p, at);
		PuppetEntity one = sealedWith(level, p, at.add(3, 0, 0), core, 64L), two = sealedWith(level, p, at.add(0, 0, 3), core, 65L);
		Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
		List<PuppetEntity> all = new java.util.ArrayList<>(List.of(general, one, two));
		try {
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.MASTER.from);
			helper.assertTrue(Puppetry.generalRefusal(general, CraftMastery.Rank.MASTER) != null, "A Master can't make a General");
			helper.assertTrue(Puppetry.generalRefusal(one, CraftMastery.Rank.GRANDMASTER) != null, "Nor a Spirit frame be one");
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.GRANDMASTER.from);
			PuppetCommands.makeGeneral(p, general);
			helper.assertTrue(!general.isGeneral(), "Not without its stones");
			p.getInventory().add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, Puppetry.GENERAL_STONES));
			PuppetCommands.makeGeneral(p, general);
			helper.assertTrue(general.isGeneral() && p.getInventory().countItem(ModItems.HIGH_SPIRIT_STONE) == 0, "Made a General for 8 High Spirit Stones");
			PuppetCommands.muster(p, general);
			helper.assertTrue(general.ranksHeld() == 2 && one.isRemoved() && two.isRemoved(), "Both taken into its ranks");
			Puppetry.Entry entry = ledger.get(p.getUUID(), one.getUUID());
			helper.assertTrue(entry != null && entry.away() && general.getDisplayName().getString().equals(ledger.generalOf(one.getUUID())),
					"Still on its puppeteer's list, in the General's ranks");
			helper.assertTrue(ledger.used(p.getUUID()) == Puppetry.cost(ArtifactGrade.EARTH) + 2 * Puppetry.cost(ArtifactGrade.SPIRIT), "Still in its slots");
			// Kept with the General through saving and loading.
			net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
			general.addAdditionalSaveData(saved);
			PuppetEntity copy = new PuppetEntity(ModEntities.PUPPET_HUMANOID, level);
			copy.readAdditionalSaveData(saved);
			helper.assertTrue(copy.isGeneral() && copy.ranksHeld() == 2, "Its ranks kept");
			helper.assertTrue(general.deploy() == 2 && general.ranksHeld() == 0, "Set down round it");
			List<PuppetEntity> out = level.getEntitiesOfClass(PuppetEntity.class, general.getBoundingBox().inflate(6), e -> e != general && e.isOwnedBy(p));
			all.addAll(out);
			helper.assertTrue(out.size() == 2 && ledger.generalOf(one.getUUID()) == null && !ledger.get(p.getUUID(), one.getUUID()).away(), "Back in the world, back on the list");
			// Mustered again, and the General falls: its ranks let out where it stood.
			PuppetCommands.muster(p, general);
			helper.assertTrue(general.ranksHeld() == 2, "Mustered again");
			general.kill();
			List<PuppetEntity> spilled = level.getEntitiesOfClass(PuppetEntity.class, general.getBoundingBox().inflate(6), e -> e != general && e.isOwnedBy(p) && e.isAlive());
			all.addAll(spilled);
			helper.assertTrue(spilled.size() == 2, "Its ranks let out where it fell (" + spilled.size() + ")");
		} finally {
			for (PuppetEntity e : all) {
				ledger.remove(p.getUUID(), e.getUUID());
				e.discard();
			}
			gone(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSaintsPuppetGrowsAMindOfItsOwn(GameTestHelper helper) {
		// Its soul copied whole, a Saint's puppet under control grows a mind of its own: a name of its own and the bent it did most toward
		// (here its blows: Fierce, harder blows). A lesser puppeteer's doesn't grow one.
		ServerPlayer p = player(helper, "test-mindsmith");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 66L);
		puppet.setNoAi(true);
		PuppetSoul wolf = new PuppetSoul(PuppetSoul.Kind.BEAST, "Wolf", "minecraft:wolf", null, 0, List.of());
		puppet.bindSoul(wolf);
		wolf.copy(PuppetSouls.COPY_TICKS);
		wolf.mark(PuppetSoul.Trait.FIERCE, 10);
		wolf.mark(PuppetSoul.Trait.WATCHFUL, 2);
		wolf.grow(PuppetSouls.GROW_TICKS - 20);
		double blows = puppet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
		CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.GRANDMASTER.from);
		helper.runAfterDelay(25, () -> {
			try {
				helper.assertTrue(wolf.trait() == null && wolf.grown() == PuppetSouls.GROW_TICKS - 20, "A Grandmaster's puppet grows no mind");
				CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.SAINT.from);
			} catch (RuntimeException e) {
				discardAll(level, p, puppet);
				throw e;
			}
			helper.runAfterDelay(25, () -> {
				try {
					helper.assertTrue(wolf.trait() == PuppetSoul.Trait.FIERCE && puppet.mind() == PuppetSoul.Trait.FIERCE, "A Saint's grows one, Fierce from its blows");
					helper.assertTrue(wolf.ownName().equals(NpcNames.of(puppet.getUUID())) && puppet.getDisplayName().getString().contains(wolf.ownName()),
							"It goes by a name of its own (" + puppet.getDisplayName().getString() + ")");
					double now = puppet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
					helper.assertTrue(Math.abs(now / blows - (1 + PuppetSouls.FIERCE_BLOWS)) < 0.01, "Its blows 15 in 100 harder (" + blows + " -> " + now + ")");
					net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
					puppet.addAdditionalSaveData(saved);
					PuppetEntity copy = new PuppetEntity(ModEntities.PUPPET_HUMANOID, level);
					copy.readAdditionalSaveData(saved);
					helper.assertTrue(copy.mind() == PuppetSoul.Trait.FIERCE && copy.soul().ownName().equals(wolf.ownName()), "Its mind kept");
				} finally {
					discardAll(level, p, puppet);
				}
				helper.succeed();
			});
		});
	}

	// --- Phase 2d: players' souls in puppets ---

	/** Every binding of a test's players let go of, and their puppets. */
	private static void unbind(ServerLevel level, PuppetEntity puppet, ServerPlayer... players) {
		Puppetry.Ledger ledger = Puppetry.Ledger.get(level.getServer());
		for (ServerPlayer p : players) {
			ledger.boundSouls.remove(p.getUUID());
			ledger.remove(p.getUUID(), puppet.getUUID());
			if (p.getVehicle() != null) p.stopRiding();
		}
		ledger.remove(puppet.getOwnerUUID() == null ? UUID.randomUUID() : puppet.getOwnerUUID(), puppet.getUUID());
		puppet.discard();
		gone(players);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSlainPlayersSoulIsHeldInAPuppet(GameTestHelper helper) {
		// Slain by a puppeteer beside their hollow puppet: the player's soul in its ring (a devilish deed), their death kept from the
		// Netherworld; at the respawn they watch through it. With the gamerule off, no one's soul is taken.
		ServerPlayer p = player(helper, "test-binder"), q = player(helper, "test-victim");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 71L);
		puppet.setNoAi(true);
		int alignment = CultivationManager.own(p).getAlignment();
		ServerPlayer[] respawned = {null};
		try {
			// Struck down by the puppet (its death hook called straight: the test server's peace and PvP turn a blow aside), then dying.
			PuppetSouls.onDeath(q, level.damageSources().mobAttack(puppet));
			q.kill();
			helper.assertTrue(q.isDeadOrDying(), "The victim is slain");
			PuppetSoul soul = puppet.soul();
			helper.assertTrue(soul != null && soul.kind == PuppetSoul.Kind.PLAYER && q.getUUID().equals(soul.player()) && soul.real(),
					"The slain player's soul held in the ring");
			helper.assertTrue(PuppetPlayers.isBound(q) && CultivationManager.own(p).getAlignment() == alignment - PuppetSouls.BIND_SHIFT,
					"Bound, and a devilish deed");
			helper.assertTrue(puppet.getDisplayName().getString().contains("test-victim"), "The puppet goes by their name");
			respawned[0] = level.getServer().getPlayerList().respawn(q, false);
		} catch (RuntimeException e) {
			unbind(level, puppet, p, q);
			throw e;
		}
		helper.runAfterDelay(5, () -> {
			ServerPlayer back = respawned[0];
			try {
				helper.assertTrue(back.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR && back.getCamera() == puppet,
						"At the respawn they watch through the puppet");
				helper.assertTrue(PuppetPlayers.isHeld(back) && CultivationManager.get(back).isMortal(), "Held, their own cultivation dormant");
				// The gamerule off: another slain beside an empty ring keeps their soul.
				ServerPlayer r = player(helper, "test-spared");
				PuppetEntity other = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())), DemonCoreItem.of(
						PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 72L);
				level.getGameRules().getRule(ModGameRules.PUPPET_SOUL_BINDING).set(false, level.getServer());
				try {
					PuppetSouls.onDeath(r, level.damageSources().mobAttack(other));
					helper.assertTrue(other.soul() == null && !Puppetry.Ledger.get(level.getServer()).boundSouls.containsKey(r.getUUID()),
							"With puppetSoulBinding off no soul is taken");
				} finally {
					level.getGameRules().getRule(ModGameRules.PUPPET_SOUL_BINDING).set(true, level.getServer());
					unbind(level, other, r);
				}
			} finally {
				unbind(level, puppet, p, back);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aHeldSoulIsReleasedToPlayItsPuppet(GameTestHelper helper) {
		// Held: watching through it, its copy growing while its puppeteer controls it. Released: the player rides inside it and steers it
		// as a soul steers a possessed body, untouchable, unseen; its copy fades. Its puppeteer away: released till they come back.
		ServerPlayer p = player(helper, "test-master"), q = player(helper, "test-held");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 73L);
		PuppetPlayers.bind(q, puppet);
		helper.runAfterDelay(25, () -> {
			try {
				helper.assertTrue(q.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SPECTATOR && q.getCamera() == puppet
						&& PuppetPlayers.isHeld(q), "Held: a spectator looking out through it");
				helper.assertTrue(puppet.underControl() && puppet.soul().copied() >= 20, "Under its puppeteer's control its copy of them grows");
				PuppetCommands.act(p, puppet.getUUID(), PuppetCommands.RELEASE_SOUL, "", "");
			} catch (RuntimeException e) {
				unbind(level, puppet, p, q);
				throw e;
			}
			helper.runAfterDelay(25, () -> {
				try {
					helper.assertTrue(puppet.released() && PuppetPlayers.isPilot(q) && q.getVehicle() == puppet, "Released: they ride inside it");
					helper.assertTrue(SoulProjection.possessorOf(puppet) == q && puppet.getControllingPassenger() == q && !puppet.underControl(),
							"It answers to them as a possessed body to its soul, and its copy grows no more");
					helper.assertTrue(Incorporeal.untouchable(q) && !q.hurt(level.damageSources().playerAttack(p), 4) && !SoulProjection.broadcastsTo(q, p),
							"Untouchable and unseen");
					// Its hands its own (the user, 2026-11-18: "live like normal"): a chest opens to it, a bed doesn't, and it rides nothing else.
					helper.assertTrue(!Incorporeal.touchesNothing(q), "It uses and takes up things as the living do");
					BlockPos chestAt = helper.absolutePos(BENCH.east(2)), bedAt = helper.absolutePos(BENCH.west(2));
					level.setBlockAndUpdate(chestAt, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
					level.setBlockAndUpdate(bedAt, net.minecraft.world.level.block.Blocks.RED_BED.defaultBlockState());
					net.minecraft.world.InteractionResult chest = net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(q, level,
							net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(chestAt), net.minecraft.core.Direction.UP, chestAt, false));
					net.minecraft.world.InteractionResult bed = net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(q, level,
							net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(bedAt), net.minecraft.core.Direction.UP, bedAt, false));
					helper.assertTrue(chest != net.minecraft.world.InteractionResult.FAIL && bed == net.minecraft.world.InteractionResult.FAIL,
							"A chest opens to it (" + chest + "), a bed doesn't (" + bed + ")");
					net.minecraft.world.entity.vehicle.Boat boat = new net.minecraft.world.entity.vehicle.Boat(level, puppet.getX() + 2, puppet.getY(), puppet.getZ());
					level.addFreshEntity(boat);
					boolean rode = q.startRiding(boat, true);
					boat.discard();
					helper.assertTrue(!rode && q.getVehicle() == puppet, "It rides nothing but its puppet");
					int copied = puppet.soul().copied();
					helper.assertTrue(copied < PuppetSouls.COPY_TICKS, "Not whole yet (" + copied + ")");
					// Controlled again: held once more.
					PuppetCommands.act(p, puppet.getUUID(), PuppetCommands.CONTROL_SOUL, "", "");
				} catch (RuntimeException e) {
					unbind(level, puppet, p, q);
					throw e;
				}
				helper.runAfterDelay(5, () -> {
					try {
						helper.assertTrue(!puppet.released() && !PuppetPlayers.isPilot(q) && q.getCamera() == puppet, "Controlled again: watching");
						// Its puppeteer gone from the world: it plays the puppet till they come back.
						gone(p);
					} catch (RuntimeException e) {
						unbind(level, puppet, p, q);
						throw e;
					}
					helper.runAfterDelay(5, () -> {
						try {
							helper.assertTrue(PuppetPlayers.isPilot(q), "Its puppeteer away: it plays the puppet");
						} finally {
							unbind(level, puppet, p, q);
						}
						helper.succeed();
					});
				});
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aStruggleCutsTheHeartsLineFromWithin(GameTestHelper helper) {
		// A soul of Puppetry at least its puppeteer's struggles: the line to the heart cut cell by cell from within, its puppeteer told;
		// the heart cut off, the puppet is theirs. One of less Puppetry and a lower realm can't.
		ServerPlayer p = player(helper, "test-holder"), q = player(helper, "test-struggler");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 74L);
		puppet.setNoAi(true);
		try {
			PuppetPlayers.bind(q, puppet);
			CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.SAINT.from);
			helper.assertTrue(!PuppetPlayers.mayStruggle(q, puppet), "Below its puppeteer's Puppetry and the puppet's realm: no struggle");
			CultivationManager.own(q).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.SAINT.from);
			helper.assertTrue(PuppetPlayers.mayStruggle(q, puppet) && PuppetPlayers.struggle(q) == 1 && puppet.struggling(), "Its equal struggles");
			int cuts = 0;
			boolean free = false;
			for (int k = 0; k < 200 && !free; k++) {
				int before = puppet.cuts().length;
				free = puppet.struggleTick(PuppetPlayers.STRUGGLE_TICKS * PuppetPlayers.WARDED_TIMES, 0);
				if (puppet.cuts().length > before) cuts++;
			}
			helper.assertTrue(free && cuts > 0 && puppet.heartLineCell() < 0 && puppet.isInert(), "Its heart cut off from within (" + cuts + " cuts)");
			PuppetPlayers.sever(puppet, q, ModLang.PUPPET_SOUL_STRUGGLED_FREE);
			helper.assertTrue(puppet.isOwnedBy(q) && Puppetry.Ledger.get(level.getServer()).get(q.getUUID(), puppet.getUUID()) != null
					&& Puppetry.Ledger.get(level.getServer()).get(p.getUUID(), puppet.getUUID()) == null, "Free: the puppet theirs");
			helper.assertTrue(PuppetCommands.seizeTicks(puppet) == PuppetPlayers.FREE_SEIZE_TICKS, "Seizing a player's own puppet body takes half a minute");
		} finally {
			unbind(level, puppet, p, q);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSoulGoesOnLetGoOrPushedOut(GameTestHelper helper) {
		// Let go: the ring hollow, the player dead as the dead go (no body). Copied whole: the real soul pushed out, the copy the puppet's
		// with their name.
		ServerPlayer p = player(helper, "test-keeper"), q = player(helper, "test-goer"), r = player(helper, "test-copied");
		ServerLevel level = helper.getLevel();
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		PuppetEntity one = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())), core, 75L),
				two = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())).add(2, 0, 0), core.copy(), 76L);
		try {
			PuppetPlayers.bind(q, one);
			PuppetPlayers.goOn(q, ModLang.PUPPET_SOUL_LET_GO);
			helper.assertTrue(one.soul() == null && q.isDeadOrDying(), "Let go: the ring hollow, the soul gone on");
			net.minecraft.nbt.CompoundTag passing = Puppetry.Ledger.get(level.getServer()).boundSouls.get(q.getUUID());
			helper.assertTrue(passing != null && passing.getBoolean("Passing") && !PuppetPlayers.isBound(q), "On its way: its game back at the respawn");
			PuppetPlayers.bind(r, two);
			PuppetSoul soul = two.soul();
			helper.assertTrue(soul.copy(PuppetSouls.COPY_TICKS), "Copied whole");
			PuppetPlayers.copiedWhole(two, soul);
			helper.assertTrue(r.isDeadOrDying() && two.soul() == soul && soul.whole() && two.getDisplayName().getString().contains("test-copied"),
					"Pushed out: the copy the puppet's own, with their name");
			helper.assertTrue(r.getUUID().equals(two.soulLook()), "And their look");
		} finally {
			unbind(level, one, p, q);
			unbind(level, two, r);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aWillingSoulEntersItsOwnPuppet(GameTestHelper helper) {
		// Asked, then sure: the body dies, the soul in one's own puppet, free; another's offer accepted likewise, held.
		ServerPlayer p = player(helper, "test-willing"), m = player(helper, "test-offerer"), q = player(helper, "test-acceptor");
		ServerLevel level = helper.getLevel();
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		PuppetEntity own = sealedWith(level, p, at, core, 77L), theirs = sealedWith(level, m, at.add(2, 0, 0), core.copy(), 78L);
		own.setNoAi(true);
		theirs.setNoAi(true);
		try {
			// Entered from its page (Enter it, pressed twice: the act).
			helper.assertTrue(PuppetPlayers.enter(p, own) == null, "Entered");
			helper.assertTrue(p.isDeadOrDying() && own.soul() != null && p.getUUID().equals(own.soul().player()) && own.isOwnedBy(p),
					"The body dies, the soul in its own puppet, free");
			// Offered from another's page to the player beside them: declined, then offered again and accepted on the soul screen.
			m.teleportTo(level, at.x + 2, at.y, at.z, 0, 0);
			q.teleportTo(level, at.x + 3, at.y, at.z, 0, 0);
			PuppetPlayers.offer(m, theirs, q);
			PuppetPlayers.decline(q);
			helper.assertTrue(PuppetPlayers.accept(q) == 0 && q.isAlive() && theirs.soul() == null, "Declined: nothing comes of accepting after");
			PuppetPlayers.offer(m, theirs, q);
			helper.assertTrue(PuppetPlayers.accept(q) == 1, "Offered and accepted");
			helper.assertTrue(q.isDeadOrDying() && theirs.soul() != null && q.getUUID().equals(theirs.soul().player()) && theirs.isOwnedBy(m),
					"Held in its puppeteer's puppet");
		} finally {
			unbind(level, own, p);
			unbind(level, theirs, m, q);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aHeldSoulSeesItsCaptorsBoard(GameTestHelper helper) {
		// Its captor's board, its Circuits (the user, 2026-11-18: "a non-free puppet should see circuits too so they can takeover/struggle
		// free"): it opens to the soul held in the puppet, only to look at (a stroke of its refused, where its captor's is painted); the
		// command line its struggle cuts runs from the core, the next cell first, and how far that cell is cut is told.
		ServerPlayer p = player(helper, "test-captor"), q = player(helper, "test-captive");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = sealedWith(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above())),
				DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null), 79L);
		puppet.setNoAi(true);
		try {
			PuppetPlayers.bind(q, puppet);
			helper.assertTrue(PuppetPlayers.heldWithin(q, puppet) && !PuppetPlayers.heldWithin(p, puppet), "Held within it (its captor isn't)");
			int[] line = puppet.commandLine();
			helper.assertTrue(line.length > 0 && line[0] == puppet.heartLineCell(), "Its command line from the core, the next cell first (" + line.length + ")");
			helper.assertTrue(PuppetPlayers.board(q) == 1, "Its Circuits open to the soul held in it");
			CircuitBoard board = puppet.board();
			int free = -1;
			for (int i = 0; i < board.size * board.size && free < 0; i++) {
				int x = i % board.size, y = i / board.size;
				if (board.isFree(x, y) && board.stroke(x, y) == CircuitBoard.Stroke.NONE && board.refusal(x, y, CircuitBoard.Stroke.LINE) == null) free = i;
			}
			helper.assertTrue(free >= 0, "A cell to paint");
			int fx = free % board.size, fy = free / board.size;
			byte[] before = Puppetry.strokes(puppet.board());
			PuppetBoards.paint(q, 1, fx, fy, CircuitBoard.Stroke.LINE.ordinal());
			PuppetBoards.act(q, PuppetBoards.WIPE_ALL);
			helper.assertTrue(java.util.Arrays.equals(before, Puppetry.strokes(puppet.board())), "Not its to paint nor to wipe");
			// Its captor paints there as ever: the refusal the held soul's alone.
			p.getInventory().add(new ItemStack(ModItems.INSCRIPTION_BRUSH));
			PuppetBoards.openPuppet(p, puppet);
			PuppetBoards.paint(p, 1, fx, fy, CircuitBoard.Stroke.LINE.ordinal());
			helper.assertTrue(puppet.board().stroke(fx, fy) == CircuitBoard.Stroke.LINE, "Its captor paints it");
			// Its struggle: how far the next cell is cut.
			helper.assertTrue(PuppetPlayers.struggle(q) == 1 && puppet.struggling() && puppet.struggleProgress(0) == 0, "It struggles, nothing cut yet");
			int next = puppet.heartLineCell(), due = puppet.struggleDue(next, 0);
			puppet.struggleTick(due / 2, 0);
			int progress = puppet.struggleProgress(0);
			helper.assertTrue(progress >= 40 && progress <= 60 && puppet.heartLineCell() == next, "Half the next cell cut (" + progress + ")");
		} finally {
			unbind(level, puppet, p, q);
		}
		helper.succeed();
	}

	/** A Nascent Soul Early cultivator at the bench, its soul already out, full of qi. */
	private static ServerPlayer projectedSoul(GameTestHelper helper, String name) {
		ServerPlayer p = player(helper, name);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		p.setOnGround(true); // (it goes out only from steady ground)
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
		return p;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aProjectedSoulGoesIntoAPuppet(GameTestHelper helper) {
		// The user, 2026-11-18: "can someone posses a puppet with nascent soul projection? ...": a soul as strong or stronger in it holds it,
		// an NPC puppeteer's is shut; one's own hollow puppet entered for good, free; another's, a weaker soul in it driven out and the soul
		// held in its stead, its puppeteer's; a player's soul driven out only where players may fight.
		ServerLevel level = helper.getLevel();
		ServerPlayer m = player(helper, "test-ringkeeper"), r = player(helper, "test-inside");
		ServerPlayer a = projectedSoul(helper, "test-projector"), b = projectedSoul(helper, "test-intruder"), c = projectedSoul(helper, "test-usurper");
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(BENCH.above()));
		PuppetEntity own = sealedWith(level, a, at, core.copy(), 80L), strong = sealedWith(level, m, at.add(1, 0, 0), core.copy(), 81L),
				shut = sealedWith(level, m, at.add(2, 0, 0), core.copy(), 82L), beast = sealedWith(level, m, at.add(0, 0, 1), core.copy(), 83L),
				held = sealedWith(level, m, at.add(1, 0, 1), core.copy(), 84L);
		for (PuppetEntity e : new PuppetEntity[] {own, strong, shut, beast, held}) e.setNoAi(true);
		strong.bindSoul(new PuppetSoul(PuppetSoul.Kind.CULTIVATOR, "Elder Wu", "defying-the-heavens:cultivator", null,
				PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY), List.of()));
		Puppetry.Ledger.get(level.getServer()).remove(m.getUUID(), shut.getUUID());
		shut.setOwner(UUID.randomUUID(), "Elder Lin", false);
		beast.bindSoul(new PuppetSoul(PuppetSoul.Kind.BEAST, "Wolf", "minecraft:wolf", null, 0, List.of()));
		PuppetPlayers.bind(r, held);
		try {
			helper.assertTrue(SoulProjection.tryPossess(a, strong) == net.minecraft.world.InteractionResult.FAIL && strong.soul() != null
					&& SoulProjection.isSoul(a), "A soul stronger than it holds its place");
			helper.assertTrue(SoulProjection.tryPossess(a, shut) == net.minecraft.world.InteractionResult.FAIL && shut.soul() == null
					&& SoulProjection.isSoul(a), "An NPC puppeteer's seal holds its own shut");
			helper.assertTrue(SoulProjection.tryPossess(a, own) == net.minecraft.world.InteractionResult.SUCCESS, "Into its own hollow puppet");
			helper.assertTrue(a.isDeadOrDying() && !SoulProjection.isActive(a.getUUID()) && own.soul() != null && a.getUUID().equals(own.soul().player())
					&& own.isOwnedBy(a) && PuppetPlayers.isBound(a), "Its body dead where it sat, its soul in its own puppet, free");
			helper.assertTrue(SoulProjection.tryPossess(b, beast) == net.minecraft.world.InteractionResult.SUCCESS, "Into another's, a beast's soul in it");
			helper.assertTrue(b.isDeadOrDying() && beast.soul() != null && b.getUUID().equals(beast.soul().player()) && beast.isOwnedBy(m)
					&& PuppetPlayers.heldWithin(b, beast), "The beast's soul driven out, its own held in its stead, its puppeteer's");
			net.minecraft.world.InteractionResult intoHeld = SoulProjection.tryPossess(c, held);
			if (level.getServer().isPvpAllowed()) {
				helper.assertTrue(intoHeld == net.minecraft.world.InteractionResult.SUCCESS && r.isDeadOrDying() && held.soul() != null
						&& c.getUUID().equals(held.soul().player()), "A weaker player's soul driven out (it goes on), its own held in its stead");
			} else {
				helper.assertTrue(intoHeld == net.minecraft.world.InteractionResult.FAIL && held.soul() != null && r.getUUID().equals(held.soul().player()),
						"Where players may not fight, no player's soul is driven out");
			}
		} finally {
			for (ServerPlayer x : new ServerPlayer[] {a, b, c}) SoulProjection.bringHome(x);
			unbind(level, own, a);
			unbind(level, strong, m);
			unbind(level, shut);
			unbind(level, beast, b);
			unbind(level, held, m, r, c);
		}
		helper.succeed();
	}

	private static void discardAll(ServerLevel level, ServerPlayer p, PuppetEntity... puppets) {
		for (PuppetEntity e : puppets) {
			Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), e.getUUID());
			e.discard();
		}
		gone(p);
	}

	// --- Puppet phase 3: the linked army ---

	/** A Spirit humanoid sealed for {@code p} at {@code at}, drawn as a Grandmaster's Balanced path (a link knot by its heart), holding there. */
	private static PuppetEntity linkedAt(ServerLevel level, ServerPlayer p, Vec3 at, long seed) {
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		ItemStack frame = PuppetFrameItem.of(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT, Artifacts.Quality.PERFECT, seed, null);
		CircuitBoard board = PuppetFrameItem.board(frame);
		board.lay(board.circuitPath(CraftMastery.Rank.GRANDMASTER.ordinal(), CircuitBoard.Build.BALANCED, Puppetry.budget(ArtifactGrade.SPIRIT, core), null));
		PuppetFrameItem.keep(frame, board);
		PuppetEntity puppet = PuppetEntity.seal(level, frame, core, p, at);
		puppet.moveTo(at.x, at.y, at.z, 0, 0);
		puppet.command(PuppetEntity.Order.HOLD, null, null);
		return puppet;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void aLinkKnotLinksAPuppet(GameTestHelper helper) {
		// Painted against the heart, a fed vein of the heart coming in through it: linked. Nowhere else. A Grandmaster's path draws one, a
		// Master's none; it carries as the line did (the path's scores the same but for the link).
		for (PuppetBlueprint blueprint : PuppetBlueprint.values()) {
			for (ArtifactGrade grade : new ArtifactGrade[] {ArtifactGrade.SPIRIT, ArtifactGrade.HEAVEN}) {
				CircuitBoard gm = new CircuitBoard(blueprint, grade.ordinal(), 0, 0);
				gm.lay(gm.circuitPath(CraftMastery.Rank.GRANDMASTER.ordinal()));
				CircuitBoard.Scores s = gm.score();
				helper.assertTrue(gm.count(CircuitBoard.Stroke.LINK) == 1 && s.linked(), "A Grandmaster's path links its " + blueprint.getId() + " of " + grade);
				CircuitBoard master = new CircuitBoard(blueprint, grade.ordinal(), 0, 0);
				master.lay(master.circuitPath(CraftMastery.Rank.MASTER.ordinal()));
				helper.assertTrue(master.count(CircuitBoard.Stroke.LINK) == 0 && !master.score().linked(), "A Master's path draws no link knot");
				// The link knot a plain line again: the same scores, unlinked.
				CircuitBoard plain = gm.copy();
				for (int y = 0; y < plain.size; y++) for (int x = 0; x < plain.size; x++) if (plain.stroke(x, y) == CircuitBoard.Stroke.LINK) plain.set(x, y, CircuitBoard.Stroke.LINE);
				CircuitBoard.Scores p = plain.score();
				helper.assertTrue(!p.linked() && p.flow() == s.flow() && p.stability() == s.stability() && p.harmony() == s.harmony()
						&& p.resilience() == s.resilience(), "The knot carries as the line did (" + blueprint.getId() + ", " + grade + ")");
			}
		}
		CircuitBoard board = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.SPIRIT.ordinal(), 0, 0);
		helper.assertTrue("heart".equals(board.refusal(1, 1, CircuitBoard.Stroke.LINK)), "A link knot goes against the heart only");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void linkedPuppetsStrengthenAndPassOrdersOn(GameTestHelper helper) {
		// Five linked puppets in a line, 15 blocks apart, the last beyond its Apprentice puppeteer's reach: each strengthened by its fellows
		// within 16, and the last hearing them through the chain; the chain broken, it hears nothing.
		ServerPlayer p = player(helper, "test-linker");
		ServerLevel level = helper.getLevel();
		Vec3 base = Vec3.atBottomCenterOf(helper.absolutePos(BENCH));
		PuppetEntity[] line = new PuppetEntity[5];
		for (int k = 0; k < line.length; k++) {
			line[k] = linkedAt(level, p, base.add(2 + 15 * k, 0, 0), 300L + k);
			line[k].setNoGravity(true); // (past the test's floor they would fall, out of each other's reach)
		}
		helper.runAfterDelay(30, () -> {
			try {
				helper.assertTrue(line[0].linked() && line[0].linkFellows() == 1 && line[1].linkFellows() == 2, "Fellows within 16 counted ("
						+ line[0].linkFellows() + ", " + line[1].linkFellows() + ")");
				PuppetEntity far = line[4];
				double reach = Puppetry.reach(CultivationManager.own(p)) * Puppetry.clarityReach(far.scores().clarity()) * far.reachFactor();
				helper.assertTrue(far.distanceTo(p) > reach, "The last stands beyond its puppeteer's own reach (" + (int) far.distanceTo(p) + " > " + (int) reach + ")");
				helper.assertTrue(PuppetCommands.inReach(p, far), "It hears its puppeteer through the chain of its linked fellows");
				// The chain broken next to it (the rest may hear their puppeteer themselves): it hears nothing.
				line[3].discard();
				Puppetry.Ledger.get(level.getServer()).remove(p.getUUID(), line[3].getUUID());
				helper.assertTrue(!PuppetCommands.inReach(p, far), "The chain broken: it hears nothing");
			} finally {
				discardAll(level, p, line);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void fourLinkedPuppetsLockAFoe(GameTestHelper helper) {
		// Four linked puppets round a husk, all on it: their ring closes, the husk is Bound and crushed.
		ServerPlayer p = player(helper, "test-ring");
		ServerLevel level = helper.getLevel();
		BlockPos rel = BENCH.east(4);
		net.minecraft.world.entity.monster.Husk husk = helper.spawn(EntityType.HUSK, rel);
		husk.setNoAi(true);
		Vec3 c = husk.position();
		PuppetEntity[] ring = new PuppetEntity[4];
		for (int k = 0; k < 4; k++) {
			double a = Math.PI / 2 * k;
			ring[k] = linkedAt(level, p, c.add(Math.cos(a) * 1.8, 0, Math.sin(a) * 1.8), 310L + k);
			ring[k].setTarget(husk);
		}
		helper.runAfterDelay(45, () -> {
			try {
				helper.assertTrue(husk.hasEffect(ModEffects.BOUND) || !husk.isAlive(), "The ring closed round it: Bound");
				helper.assertTrue(!husk.isAlive() || husk.getHealth() < husk.getMaxHealth(), "Crushed");
				helper.assertTrue(PuppetFormations.closed(husk, java.util.List.of(ring)) || !husk.isAlive(), "Its ring stands");
			} finally {
				husk.discard();
				discardAll(level, p, ring);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void knowingTheEnemysCircuits(GameTestHelper helper) {
		// One who wrecks another's puppet knows its design: its diagram in the wreck, harder blows at puppets built to it, seizing a rank less.
		ServerPlayer owner = player(helper, "test-maker"), foe = player(helper, "test-wrecker"), other = player(helper, "test-stranger");
		ServerLevel level = helper.getLevel();
		Vec3 base = Vec3.atBottomCenterOf(helper.absolutePos(BENCH));
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
		PuppetEntity first = sealedWith(level, owner, base.add(3, 0, 0), core.copy(), 320L), twin = sealedWith(level, owner, base.add(-3, 0, 0), core.copy(), 320L),
				third = sealedWith(level, owner, base.add(0, 0, 3), core.copy(), 320L);
		// Held still (a puppet of Harmony above even slips a blow now and then: frozen, it slips none).
		for (PuppetEntity e : new PuppetEntity[] {first, twin, third}) {
			e.command(PuppetEntity.Order.HOLD, null, null);
			e.freeze(200);
		}
		long design = first.design();
		try {
			helper.assertTrue(design == twin.design() && design == third.design(), "One drawing, one design");
			// Seizing: a rank short of what it wants, and a better seal in hand.
			int needed = twin.heartVeins() - 1 + switch (twin.scores().grade()) {
				case PERFECT -> 3;
				case FINE -> 2;
				default -> 1;
			};
			boolean seizable = needed >= 1 && needed - 1 < CraftMastery.Rank.values().length;
			if (seizable) {
				CultivationManager.own(foe).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.values()[needed - 1].from);
				foe.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, CommandSealItem.of(ArtifactGrade.SPIRIT));
				helper.assertTrue(PuppetCommands.seizeRefusal(foe, twin) != null, "Unknown, it wants a rank more");
			}
			first.hurt(level.damageSources().playerAttack(foe), 1.0e6f);
			helper.assertTrue(!first.isAlive() && Puppetry.knows(level.getServer(), foe.getUUID(), design), "Wrecked: its design known");
			boolean diagram = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, first.getBoundingBox().inflate(3),
					e -> e.getItem().getItem() instanceof CircuitDiagramItem && CircuitDiagramItem.design(e.getItem()) == design).isEmpty();
			helper.assertTrue(diagram, "Its Circuit Diagram lies in the wreck");
			if (seizable) helper.assertTrue(PuppetCommands.seizeRefusal(foe, twin) == null, "Known, seizing wants a rank less");
			float before = twin.getHealth(), beforeThird = third.getHealth();
			twin.hurt(level.damageSources().playerAttack(foe), 4);
			third.hurt(level.damageSources().playerAttack(other), 4);
			float known = before - twin.getHealth(), unknown = beforeThird - third.getHealth();
			helper.assertTrue(known > unknown * 1.15f, "Struck harder by one who knows it (" + known + " against " + unknown + ")");
		} finally {
			for (PuppetEntity e : new PuppetEntity[] {first, twin, third}) {
				Puppetry.Ledger.get(level.getServer()).remove(owner.getUUID(), e.getUUID());
				e.discard();
			}
			gone(owner, foe, other);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void aSpringMendsItsOwnLines(GameTestHelper helper) {
		// A Spring of the Ancients poured into it: it mends its cut lines by itself, as they were drawn; its frame keeps the spring.
		ServerPlayer p = player(helper, "test-spring");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = linkedAt(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.east(3))), 330L);
		p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS));
		puppet.interact(p, net.minecraft.world.InteractionHand.MAIN_HAND);
		helper.assertTrue(puppet.springFed(), "Poured into it");
		helper.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE) && p.getInventory().countItem(ModItems.SPRING_OF_THE_ANCIENTS) == 0,
				"The bottle given back in the hand");
		byte[] drawn = Puppetry.strokes(puppet.board());
		puppet.cut(false);
		helper.assertTrue(puppet.cuts().length == 1, "A line cut");
		int cell = puppet.cuts()[0];
		helper.runAfterDelay(Puppetry.SPRING_MEND_TICKS + 25, () -> {
			try {
				helper.assertTrue(puppet.cuts().length == 0 && Puppetry.strokes(puppet.board())[cell] == drawn[cell], "Mended by itself, as it was drawn");
				puppet.dismantle();
				boolean kept = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, puppet.getBoundingBox().inflate(3),
						e -> e.getItem().getItem() instanceof PuppetFrameItem && e.getItem().getTag() != null && e.getItem().getTag().getBoolean(PuppetEntity.SPRING_FED)).isEmpty();
				helper.assertTrue(kept, "Taken apart, its frame keeps the spring");
			} finally {
				discardAll(level, p, puppet);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aShieldedCoreKeepsItsStrength(GameTestHelper helper) {
		// Jade and obsidian round a core: shielded. A Four Axis puppet in a lower realm stands at half its strength, a shielded one whole.
		ServerPlayer p = player(helper, "test-shield");
		ServerLevel level = helper.getLevel();
		ItemStack core = DemonCoreItem.of(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), null, null);
		net.minecraft.world.inventory.AbstractContainerMenu dummy = new net.minecraft.world.inventory.AbstractContainerMenu(null, 0) {
			@Override
			public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) { return ItemStack.EMPTY; }

			@Override
			public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
		};
		net.minecraft.world.inventory.TransientCraftingContainer grid = new net.minecraft.world.inventory.TransientCraftingContainer(dummy, 3, 3);
		grid.setItem(4, core.copy());
		for (int k : new int[] {0, 2, 6, 8}) grid.setItem(k, new ItemStack(ModItems.JADE));
		for (int k : new int[] {1, 3, 5, 7}) grid.setItem(k, new ItemStack(Items.OBSIDIAN));
		CoreShieldRecipe recipe = new CoreShieldRecipe(DefyingTheHeavens.id("shield_core"), net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
		helper.assertTrue(recipe.matches(grid, level), "The core, four jade and four obsidian");
		ItemStack shielded = recipe.assemble(grid, level.registryAccess());
		helper.assertTrue(DemonCoreItem.shielded(shielded) && DemonCoreItem.rank(shielded) == DemonCoreItem.rank(core), "The same core, shielded");
		grid.setItem(0, ItemStack.EMPTY);
		helper.assertTrue(!recipe.matches(grid, level), "Short of a jade, nothing");
		Vec3 base = Vec3.atBottomCenterOf(helper.absolutePos(BENCH));
		PuppetEntity plain = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.HEAVEN, CraftMastery.Rank.SAINT.ordinal(), 340L), core.copy(), p,
				base.add(3, 0, 0));
		PuppetEntity kept = PuppetEntity.seal(level, drawnFrame(PuppetBlueprint.HUMANOID, ArtifactGrade.HEAVEN, CraftMastery.Rank.SAINT.ordinal(), 340L), shielded, p,
				base.add(-3, 0, 0));
		for (PuppetEntity e : new PuppetEntity[] {plain, kept}) e.command(PuppetEntity.Order.HOLD, null, null);
		helper.runAfterDelay(30, () -> {
			try {
				helper.assertTrue(plain.suppressed() && !kept.suppressed(), "Held down in a lower realm, unless shielded");
				double ratio = plain.getMaxHealth() / kept.getMaxHealth();
				helper.assertTrue(Math.abs(ratio - Puppetry.SUPPRESSED) < 0.02, "Half its health (" + ratio + ")");
			} finally {
				discardAll(level, p, plain, kept);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSelfDestructBursts(GameTestHelper helper) {
		// Its fuse lit: a moment later it bursts, the husk beside it struck, its puppeteer and their other puppet spared, nothing left of it.
		ServerPlayer p = player(helper, "test-burst");
		ServerLevel level = helper.getLevel();
		BlockPos rel = BENCH.east(4);
		net.minecraft.world.entity.monster.Husk husk = helper.spawn(EntityType.HUSK, rel);
		husk.setNoAi(true);
		PuppetEntity bomb = linkedAt(level, p, husk.position().add(1.5, 0, 0), 350L), friend = linkedAt(level, p, husk.position().add(3, 0, 0), 351L);
		p.teleportTo(level, husk.getX() + 2, husk.getY(), husk.getZ() + 1.5, 0, 0);
		float huskBefore = husk.getHealth(), friendBefore = friend.getHealth(), pBefore = p.getHealth();
		// Its own core marked: the tests beside this one wreck and take apart puppets whose cores fall, and those are not its.
		String mark = "burst-" + bomb.getUUID();
		helper.assertTrue(!bomb.core().isEmpty(), "A core in it");
		bomb.core().getOrCreateTag().putString("TestMark", mark);
		bomb.lightFuse();
		helper.runAfterDelay(Puppetry.FUSE_TICKS + 5, () -> {
			try {
				helper.assertTrue(bomb.isRemoved() && Puppetry.Ledger.get(level.getServer()).get(p.getUUID(), bomb.getUUID()) == null, "Burst: gone, off the ledger");
				helper.assertTrue(!husk.isAlive() || husk.getHealth() < huskBefore, "The husk caught in the blast");
				helper.assertTrue(friend.getHealth() >= friendBefore && p.getHealth() >= pBefore, "Its puppeteer and theirs spared");
				boolean coreLeft = !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bomb.getBoundingBox().inflate(24),
						e -> e.getItem().is(ModItems.DEMON_CORE) && e.getItem().hasTag() && mark.equals(e.getItem().getTag().getString("TestMark"))).isEmpty();
				helper.assertTrue(!coreLeft, "Its core gone with it");
			} finally {
				husk.discard();
				discardAll(level, p, friend);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aFactoryBuildsCopiesAndMends(GameTestHelper helper) {
		// Its diagram, matter and a core: a copy built to the diagram's design, flaws clear of its lines, for its maker; then it mends their
		// puppet's cut line nearby. Repair only, it builds nothing.
		ServerPlayer p = player(helper, "test-factory");
		ServerLevel level = helper.getLevel();
		CultivationManager.own(p).setMastery(CraftMastery.Craft.PUPPETRY, CraftMastery.Rank.MASTER.from);
		BlockPos rel = BENCH.east(3);
		helper.setBlock(rel, ModBlocks.PUPPET_FACTORY);
		PuppetFactoryBlockEntity factory = (PuppetFactoryBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
		factory.setOwner(p);
		CircuitBoard board = new CircuitBoard(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL.ordinal(), 0, 0);
		board.lay(board.circuitPath(CraftMastery.Rank.MASTER.ordinal()));
		ItemStack diagram = CircuitDiagramItem.of(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL, Puppetry.strokes(board), "test", false);
		factory.setItem(PuppetFactoryBlockEntity.DIAGRAM, diagram);
		int slot = PuppetFactoryBlockEntity.MATTER;
		for (java.util.Map.Entry<net.minecraft.world.item.Item, Integer> need : PuppetFactoryBlockEntity.needs(PuppetBlueprint.HUMANOID, ArtifactGrade.MORTAL).entrySet()) {
			factory.setItem(slot++, new ItemStack(need.getKey(), need.getValue()));
		}
		factory.setItem(PuppetFactoryBlockEntity.CORES, DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), null, null));
		int before = Puppetry.Ledger.get(level.getServer()).of(p.getUUID()).size();
		for (int t = 0; t < Puppetry.factoryTicks(ArtifactGrade.MORTAL) + 20; t++) factory.tick(level);
		java.util.List<PuppetEntity> built = level.getEntitiesOfClass(PuppetEntity.class, new AABB(helper.absolutePos(rel)).inflate(3), e -> e.isOwnedBy(p));
		try {
			helper.assertTrue(built.size() == 1 && Puppetry.Ledger.get(level.getServer()).of(p.getUUID()).size() == before + 1, "One copy built for its maker");
			PuppetEntity copy = built.get(0);
			helper.assertTrue(copy.design() == CircuitDiagramItem.design(diagram) && copy.soul() == null, "Built to the diagram's design, hollow");
			helper.assertTrue(factory.getItem(PuppetFactoryBlockEntity.CORES).isEmpty() && factory.getItem(PuppetFactoryBlockEntity.MATTER).isEmpty(),
					"Its core and matter taken");
			copy.cut(false);
			helper.assertTrue(copy.cuts().length == 1, "A line cut");
			for (int t = 0; t < Puppetry.FACTORY_MEND_TICKS; t++) factory.tick(level);
			helper.assertTrue(copy.cuts().length == 0, "Mended by the factory beside it");
			factory.toggleMode();
			factory.setItem(PuppetFactoryBlockEntity.CORES, DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), null, null));
			for (int t = 0; t < 60; t++) factory.tick(level);
			helper.assertTrue(factory.getItem(PuppetFactoryBlockEntity.CORES).getCount() == 1, "Repair only: nothing built");
		} finally {
			discardAll(level, p, built.toArray(new PuppetEntity[0]));
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void anOverseerWritesTheWear(GameTestHelper helper) {
		// Its puppets' cut lines recorded by design; a diagram of that design laid in an Overseer Core carries its wear.
		ServerPlayer p = player(helper, "test-overseer");
		ServerLevel level = helper.getLevel();
		PuppetEntity puppet = linkedAt(level, p, Vec3.atBottomCenterOf(helper.absolutePos(BENCH.east(3))), 360L);
		long design = puppet.design();
		ItemStack diagram = CircuitDiagramItem.of(puppet.blueprint(), puppet.frameGrade(), puppet.designStrokes(), "test", false);
		puppet.cut(false);
		puppet.cut(false);
		try {
			Puppetry.Wear wear = Puppetry.Ledger.get(level.getServer()).wearOf(p.getUUID(), design);
			helper.assertTrue(wear != null && wear.cutsTotal == 2 && puppet.design() == design, "Two cuts recorded by its design (still its design, cut)");
			BlockPos rel = BENCH.west(3);
			helper.setBlock(rel, ModBlocks.OVERSEER_CORE);
			OverseerCoreBlockEntity core = (OverseerCoreBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
			core.setOwner(p);
			core.setItem(0, diagram);
			Puppetry.Wear written = CircuitDiagramItem.wear(core.getItem(0));
			helper.assertTrue(written != null && written.cutsTotal == 2 && written.mostCuts() >= 1, "Laid in its Overseer Core, the diagram carries its wear");
		} finally {
			discardAll(level, p, puppet);
		}
		helper.succeed();
	}
}
