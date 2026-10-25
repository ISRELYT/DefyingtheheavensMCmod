package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

/**
 * What the third bug sweep of 2026-10-24 put right, held to it: only harm that lands across a barrier batters it (asking whom a spell
 * spares, as heals, golems and a dragon's might do, wore sects' barriers down and set them on the asker); a spell's mending shared with
 * its caster's own alone (not the walled, the dead or spectators); the cultivators past Four Axis telling their own years.
 */
public class SweepThreeGameTests implements FabricGameTest {
	/** A survival player at {@code realm}'s {@code stage} at {@code rel}, out of the player list. */
	private static ServerPlayer at(GameTestHelper helper, Realm realm, Stage stage, BlockPos rel) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sweep-three"));
		p.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(p).setState(realm, stage, 0);
		CultivationManager.refresh(p);
		BlockPos abs = helper.absolutePos(rel);
		p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
		return p;
	}

	/** Where the core stands (its barrier {@link Formations#MIN_RADIUS} round it). */
	private static final BlockPos CORE = new BlockPos(6, 2, 6);

	/** A core at {@link #CORE} bound to a sect as strong as Core Formation Early. */
	private static FormationCoreBlockEntity core(GameTestHelper helper) {
		helper.setBlock(CORE.below(), Blocks.STONE);
		helper.setBlock(CORE, ModBlocks.FORMATION_CORE);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CORE));
		core.bindToSect(UUID.randomUUID(), "Sweep", Formations.MIN_RADIUS, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		return core;
	}

	/** The core and its barrier gone again (the test world is kept: a barrier left standing in it walls off the tests after it). */
	private static void takeDown(GameTestHelper helper, Formation formation) {
		if (formation != null) Formations.dissolve(helper.getLevel(), formation.id);
		helper.setBlock(CORE, Blocks.AIR);
		helper.setBlock(CORE.below(), Blocks.AIR);
	}

	/** A zombie that stays put (named: a barrier drives out no monster someone named, Formations#purge). */
	private static Zombie still(GameTestHelper helper, BlockPos rel) {
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, rel);
		zombie.setNoAi(true);
		zombie.setCustomName(net.minecraft.network.chat.Component.literal("kept"));
		return zombie;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void onlyHarmLandingAcrossABarrierWearsItDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormationCoreBlockEntity core = core(helper);
		helper.runAfterDelay(45, () -> {
			Formation formation = Formations.get(level, core.getFormationId());
			try {
				helper.assertTrue(formation != null && formation.isRaised(), "The barrier is up");
				// One just strong enough to break it, outside it; a zombie inside.
				ServerPlayer strong = at(helper, Realm.CORE_FORMATION, Stage.EARLY, CORE.offset(9, 0, 0));
				Zombie inside = still(helper, CORE.offset(1, 0, 0));
				double full = core.getBattery();
				boolean spared = HeavenSpells.spared(strong, inside), ally = HeavenSpells.ally(strong, inside), own = HeavenSpells.own(strong, inside);
				HeavenSpells.mark(strong, inside);
				HeavenSpells.friendly(strong, inside);
				helper.assertTrue(!spared && !ally && !own, "Across a barrier it is strong enough for, the zombie is no one it spares");
				helper.assertTrue(core.getBattery() == full, "Asking whom its spells spare wears the barrier not at all (" + (full - core.getBattery()) + ")");
				HeavenSpells.strike(strong, inside, 1.0f, QiElement.FIRE);
				helper.assertTrue(Math.abs(full - core.getBattery() - Formations.BATTER_COST * 2) < 1.0e-6,
						"Harm landing across it batters it as a spell's blow (" + (full - core.getBattery()) + ")");
				inside.discard();
				helper.succeed();
			} finally {
				takeDown(helper, formation);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSpellsMendingIsSharedWithItsCastersOwnAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormationCoreBlockEntity core = core(helper);
		helper.runAfterDelay(45, () -> {
			Formation formation = Formations.get(level, core.getFormationId());
			Wolf near = helper.spawn(EntityType.WOLF, CORE.offset(10, 0, 2)), walled = helper.spawn(EntityType.WOLF, CORE.offset(1, 0, 0));
			Zombie zombie = still(helper, CORE.offset(10, 0, -2));
			try {
				// One too weak to break the barrier, outside it: its pet beside it, and its pet within the barrier.
				ServerPlayer weak = at(helper, Realm.QI_REFINING, Stage.EARLY, CORE.offset(9, 0, 0));
				near.tame(weak);
				walled.tame(weak);
				helper.assertTrue(HeavenSpells.own(weak, near), "Its pet beside it shares its mending");
				helper.assertTrue(HeavenSpells.spared(weak, walled) && !HeavenSpells.own(weak, walled), "...not one behind a barrier from it, though spared");
				helper.assertTrue(!HeavenSpells.own(weak, zombie), "...nor a monster");
				near.kill();
				helper.assertTrue(!HeavenSpells.own(weak, near), "...nor the dead");
				helper.succeed();
			} finally {
				near.discard();
				walled.discard();
				zombie.discard();
				takeDown(helper, formation);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theCultivatorsPastFourAxisTellTheirOwnYears(GameTestHelper helper) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		try {
			npc.setNoAi(true);
			npc.setRank(PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY), true);
			int dao = npc.yearsCultivated();
			helper.assertTrue(dao >= 3000 && dao < 30_000, "A Dao Integration cultivator has cultivated thousands of years (" + dao + ")");
			npc.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), true);
			int immortal = npc.yearsCultivated();
			helper.assertTrue(immortal >= 50_000_000, "...a True Immortal, ages (" + immortal + ")");
			helper.succeed();
		} finally {
			npc.discard();
		}
	}
}
