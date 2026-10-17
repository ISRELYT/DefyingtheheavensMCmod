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
	public void fallingBackGivesEveryPointBack(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.values()[2], Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		StatPoints.spend(player, StatPoints.Stat.CONSTITUTION, StatPoints.earned(c));
		helper.assertTrue(c.statPointsSpent() == StatPoints.earned(c) && player.getMaxHealth() > 0, "Every point spent");

		c.setState(Realm.values()[1], Stage.EARLY, 0);
		CultivationManager.refresh(player);
		helper.assertTrue(c.statPointsSpent() == 0 && StatPoints.unspent(c) == StatPoints.earned(c),
				"Cultivation fallen below what was spent: every point is given back to spend again");

		c.setStatPoints(StatPoints.Stat.INSIGHT, 1);
		c.setState(Realm.values()[2], Stage.EARLY, 0);
		CultivationManager.refresh(player);
		helper.assertTrue(c.getStatPoints(StatPoints.Stat.INSIGHT) == 1, "Points within what was earned are kept");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aColumnIsCutAndAWoundQueued(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = cultivator(helper, Realm.values()[3], Stage.EARLY);
		BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
		for (int i = 0; i < 3; i++) level.setBlockAndUpdate(base.above(i), Blocks.STONE.defaultBlockState());
		int cut = Devastation.cutColumn(player, level, base.getX(), base.getZ(), base.getY(), base.getY() + 2, false);
		helper.assertTrue(cut == 3 && level.getBlockState(base).isAir() && level.getBlockState(base.above(2)).isAir(), "A slash cuts the column through");

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

	@GameTest(template = EMPTY_STRUCTURE, batch = "sever_slash", timeoutTicks = 100)
	public void theSeveringSlashLeavesItsCasterStanding(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 8, 6);
		ServerLevel level = helper.getLevel();
		BlockPos at = floor.centre();
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.of(QiElement.METAL, Realm.FOUR_AXIS), at);
		p.setYRot(0); // facing south, +z
		p.setXRot(0);
		Spells.setCooldown(p, Spell.HEAVEN_SEVERING_SWORD, 0);
		Spells.cast(p, Spell.HEAVEN_SEVERING_SWORD);
		helper.runAfterDelay(3, () -> {
			try {
				helper.assertTrue(!level.getBlockState(at.below()).isAir() && !level.getBlockState(at.offset(0, -1, -3)).isAir()
						&& !level.getBlockState(at.offset(2, -1, 0)).isAir(), "The ground under its caster, and behind, still stands");
				helper.assertTrue(level.getBlockState(at.offset(0, -1, 6)).isAir(), "...and the canyon opens ahead of them");
				helper.succeed();
			} finally {
				// The slash goes no further than this (it would run on through the other tests' ground).
				level.getEntitiesOfClass(SpellProjectile.class, new net.minecraft.world.phys.AABB(at).inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
				HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
			}
		});
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
