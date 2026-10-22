package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The 2026-10-17 batch: stat points (earned by breakthroughs, spent, given back when cultivation falls), the spells' wounds in the
 * land (Devastation), and /cultivator secluded finding a cultivator in seclusion.
 */
public class StatPointsGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		c.setMortal(false);
		c.setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void breakthroughsEarnStageAndRealmPoints(GameTestHelper helper) {
		int stages = Stage.values().length;
		helper.assertTrue(StatPoints.earned(0) == 0, "Nothing at the very first stage");
		helper.assertTrue(StatPoints.earned(1) == StatPoints.STAGE_POINTS, "A stage's breakthrough gives a stage's points");
		helper.assertTrue(StatPoints.earned(stages) == (stages - 1) * StatPoints.STAGE_POINTS + StatPoints.REALM_POINTS,
				"A realm's breakthrough gives a realm's points, more than a stage's");
		helper.assertTrue(StatPoints.REALM_POINTS > StatPoints.STAGE_POINTS, "A realm gives more than a stage");

		ServerPlayer player = cultivator(helper, Realm.values()[1], Stage.MID);
		PlayerCultivation c = CultivationManager.get(player);
		helper.assertTrue(StatPoints.earned(c) == StatPoints.earned(stages + 1), "A cultivator already far along has all its points at once");
		c.setMortal(true);
		helper.assertTrue(StatPoints.earned(c) == 0, "A mortal has none");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void pointsAreSpentAndStrengthen(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.values()[1], Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		int earned = StatPoints.earned(c);
		double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		double maxQi = c.maxQi();

		StatPoints.spend(player, StatPoints.Stat.STRENGTH, 3);
		helper.assertTrue(c.getStatPoints(StatPoints.Stat.STRENGTH) == 3 && StatPoints.unspent(c) == earned - 3, "Three points go into strength");
		helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) > damage, "Strength strikes harder");

		StatPoints.spend(player, StatPoints.Stat.SPIRIT, 1000);
		helper.assertTrue(c.getStatPoints(StatPoints.Stat.SPIRIT) == earned - 3 && StatPoints.unspent(c) == 0, "No more can be spent than were earned");
		helper.assertTrue(c.maxQi() > maxQi, "Spirit deepens the qi pool");
		StatPoints.spend(player, StatPoints.Stat.AGILITY, 1);
		helper.assertTrue(c.getStatPoints(StatPoints.Stat.AGILITY) == 0, "None left, none spent");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void fallingBackTakesOnlyTheLastPointsBack(GameTestHelper helper) {
		// Points spent are locked in (the user's rule, 2026-10-19): a fall takes back only what the lost stages gave, the last put in.
		ServerPlayer player = cultivator(helper, Realm.values()[1], Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		c.clearStatPoints();
		int atFoundation = StatPoints.earned(c);
		StatPoints.spend(player, StatPoints.Stat.CONSTITUTION, atFoundation);
		c.setState(Realm.values()[2], Stage.EARLY, 0);
		CultivationManager.refresh(player);
		int more = StatPoints.earned(c) - atFoundation;
		StatPoints.spend(player, StatPoints.Stat.STRENGTH, 3);
		StatPoints.spend(player, StatPoints.Stat.INSIGHT, more - 3);
		helper.assertTrue(c.statPointsSpent() == StatPoints.earned(c) && StatPoints.unspent(c) == 0, "Every point spent");

		c.setState(Realm.values()[1], Stage.EARLY, 0);
		CultivationManager.refresh(player);
		helper.assertTrue(c.getStatPoints(StatPoints.Stat.CONSTITUTION) == atFoundation && c.getStatPoints(StatPoints.Stat.STRENGTH) == 0
						&& c.getStatPoints(StatPoints.Stat.INSIGHT) == 0 && StatPoints.unspent(c) == 0,
				"Fallen back to Foundation Building: the points Core Formation gave are gone, those spent before it stay ("
						+ c.getStatPoints(StatPoints.Stat.CONSTITUTION) + "/" + c.getStatPoints(StatPoints.Stat.STRENGTH) + "/"
						+ c.getStatPoints(StatPoints.Stat.INSIGHT) + ")");

		// A smaller fall within the last spending: only its last few go, last in first out.
		c.setState(Realm.values()[2], Stage.EARLY, 0);
		CultivationManager.refresh(player);
		StatPoints.spend(player, StatPoints.Stat.STRENGTH, 3);
		StatPoints.spend(player, StatPoints.Stat.INSIGHT, StatPoints.unspent(c));
		c.setState(Realm.values()[1], Stage.GRAND_PERFECTION, 0);
		CultivationManager.refresh(player);
		int kept = StatPoints.earned(c) - atFoundation;
		helper.assertTrue(c.getStatPoints(StatPoints.Stat.CONSTITUTION) == atFoundation
						&& c.getStatPoints(StatPoints.Stat.STRENGTH) == Math.min(3, kept) && c.getStatPoints(StatPoints.Stat.INSIGHT) == Math.max(0, kept - 3),
				"A stage down: Insight, put in last, goes first");

		// Saved and loaded, the order holds.
		PlayerCultivation loaded = PlayerCultivation.load(c.save());
		helper.assertTrue(java.util.Arrays.equals(loaded.statPointOrder(), c.statPointOrder()), "The order is saved");
		helper.succeed();
	}

	/**
	 * A batch of its own (2026-10-19): in the default batch a neighbouring sect test's barrier now and then enclosed it (the batch's
	 * tests aren't laid out the same each run), and ground under a raised formation is never carved (HeavenSpells#carvable).
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "stat_points_devastation")
	public void aColumnIsCutAndAWoundQueued(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = cultivator(helper, Realm.values()[3], Stage.EARLY);
		BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
		for (int i = 0; i < 3; i++) level.setBlockAndUpdate(base.above(i), Blocks.STONE.defaultBlockState());
		String why = "temporary " + HeavenSpells.isTemporary(level, base) + ", sheltered " + Formations.shelters(level, base) + ", mayInteract "
				+ level.mayInteract(player, base) + ", sect ground " + Formations.protectsGround(player, base) + ", mayBuild " + player.mayBuild()
				+ ", at " + base + ", spawn " + level.getSharedSpawnPos();
		int cut = Devastation.cutColumn(player, level, base.getX(), base.getZ(), base.getY(), base.getY() + 2, false);
		helper.assertTrue(cut == 3 && level.getBlockState(base).isAir() && level.getBlockState(base.above(2)).isAir(), "A slash cuts the column through (cut " + cut + "; " + why + ")");

		BlockPos lake = helper.absolutePos(new BlockPos(3, 1, 3));
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(lake.offset(dx, 0, dz), Blocks.DIRT.defaultBlockState());
		}
		Devastation.crater(player, level, Vec3.atCenterOf(lake), 1.5, 1.5, Devastation.Floor.NONE);
		helper.assertTrue(Devastation.pending() > 0, "A crater is queued, to be cut over the next ticks");
		helper.runAfterDelay(5, () -> {
			try {
				helper.assertTrue(level.getBlockState(lake).isAir(), "...and is cut");
				helper.succeed();
			} finally {
				HeavenSpells.clear();
				// The pieces it flung, gone with it (the spell tests count what is left flying about).
				level.getEntitiesOfClass(SpellBlock.class, new net.minecraft.world.phys.AABB(lake).inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
			}
		});
	}

	/** Where the slash test's caster faces: 30 degrees off the grid (head and body: the way a spell flies is the head's). */
	private static final float SLASH_YAW = 30;

	/** The block {@code ahead} along the slash test's facing and {@code across} to its side of the caster at {@code at}, {@code dy} up. */
	private static BlockPos slashOffset(BlockPos at, double ahead, double across, int dy) {
		Vec3 forward = Vec3.directionFromRotation(0, SLASH_YAW), side = new Vec3(-forward.z, 0, forward.x);
		return BlockPos.containing(at.getX() + 0.5 + forward.x * ahead + side.x * across, at.getY() + dy, at.getZ() + 0.5 + forward.z * ahead + side.z * across);
	}

	/** The floor's blocks just ahead of the caster, all across its middle: every one of them in the cut, none stepped over. */
	private static List<BlockPos> slashFloor(BlockPos at) {
		List<BlockPos> floor = new ArrayList<>();
		for (int f = 4; f <= 7; f++) for (int s = -3; s <= 3; s++) floor.add(slashOffset(at, f, s, -1));
		return floor;
	}

	/**
	 * Faced off the grid (a slash on a slant sampled its way a block at a time, along and across, and stepped over whole columns,
	 * left standing in its cut), the slash cuts everything its blade passes through, and only that.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sever_slash", timeoutTicks = 100)
	public void theSeveringSlashLeavesItsCasterStanding(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		ServerLevel level = helper.getLevel();
		BlockPos at = floor.centre();
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.of(QiElement.METAL, Realm.FOUR_AXIS), at);
		p.setYRot(SLASH_YAW);
		p.setYHeadRot(SLASH_YAW);
		p.setYBodyRot(SLASH_YAW);
		p.setXRot(0);
		// Stone in its blade's way (ahead, and far out to one side) and stone above its blade.
		BlockPos inBlade = slashOffset(at, 8, 0, 3), wide = slashOffset(at, 10, 20, 0), above = slashOffset(at, 8, 0, 9);
		for (BlockPos pos : new BlockPos[]{inBlade, wide, above}) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
		Spells.setCooldown(p, Spell.HEAVEN_SEVERING_SWORD, 0);
		Spells.cast(p, Spell.HEAVEN_SEVERING_SWORD);
		awaitSlash(helper, level, at, inBlade, wide, above, p, floor, 0);
	}

	/**
	 * Checks the slash once its cut has reached the blocks ahead and to the side, or after four seconds whatever it did. The slash
	 * waits at land not yet made (it asks for the land ahead and goes on once it is there), and the test world's land along its
	 * way is often new. Once checked, it is stopped (it would run on through the other tests' ground).
	 */
	private static void awaitSlash(GameTestHelper helper, ServerLevel level, BlockPos at, BlockPos inBlade, BlockPos wide, BlockPos above,
			ServerPlayer p, HeavenSpellTesting.Floor floor, int waited) {
		List<BlockPos> standing = slashFloor(at).stream().filter(pos -> !level.getBlockState(pos).isAir()).toList();
		boolean passed = standing.isEmpty() && level.getBlockState(inBlade).isAir() && level.getBlockState(wide).isAir();
		if (!passed && waited < 80) {
			helper.runAfterDelay(1, () -> awaitSlash(helper, level, at, inBlade, wide, above, p, floor, waited + 1));
			return;
		}
		try {
			helper.assertTrue(!level.getBlockState(at.below()).isAir() && !level.getBlockState(slashOffset(at, -3, 0, -1)).isAir()
					&& !level.getBlockState(slashOffset(at, 0, 2, -1)).isAir(), "The ground under its caster, and behind, still stands");
			helper.assertTrue(standing.isEmpty(), "...its blade cuts all the floor it passes over ahead of them, no block stepped over (left: " + standing + ")");
			helper.assertTrue(level.getBlockState(inBlade).isAir(), "...and what it passes through above the floor");
			helper.assertTrue(level.getBlockState(wide).isAir(), "...20 blocks out to the side too (it is about 110 across)");
			helper.assertTrue(level.getBlockState(above).is(Blocks.STONE), "...and what stands above its blade is left standing");
			helper.succeed();
		} finally {
			// The slash goes no further than this (it would run on through the other tests' ground).
			level.getEntitiesOfClass(SpellProjectile.class, new net.minecraft.world.phys.AABB(at).inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
			for (BlockPos pos : new BlockPos[]{inBlade, wide, above}) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSecludedAreFound(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CultivatorNpc hermit = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		hermit.setRank(12);
		hermit.setNoAi(true);
		hermit.enterSeclusion(level, hermit.blockPosition());
		List<String> said = new ArrayList<>();
		CommandSource listener = new CommandSource() {
			@Override
			public void sendSystemMessage(Component message) { said.add(message.getString()); }

			@Override
			public boolean acceptsSuccess() { return true; }

			@Override
			public boolean acceptsFailure() { return true; }

			@Override
			public boolean shouldInformAdmins() { return false; }
		};
		CommandSourceStack source = new CommandSourceStack(listener, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)), Vec2.ZERO, level, 4,
				"test", Component.literal("test"), level.getServer(), null);
		try {
			int found = level.getServer().getCommands().performPrefixedCommand(source, "cultivator secluded 100");
			helper.assertTrue(found >= 1, "/cultivator secluded counts the secluded (said: " + said + ")");
			helper.assertTrue(said.stream().anyMatch(line -> line.contains(hermit.getPersonalName()) && line.contains("in seclusion")),
					"...and names the hermit, in seclusion");
			helper.succeed();
		} finally {
			hermit.lowerConcealment(level);
			Rumours.forgetCave(hermit);
			hermit.discard();
		}
	}
}
