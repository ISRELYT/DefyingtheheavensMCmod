package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.UUID;

/**
 * The strong against a barrier and a way in for them (the user's asks, 2026-10-24): one strong enough to break a barrier wears its stored
 * qi down with each blow, the more the stronger; one a realm above it shatters it whole at once; the weaker do nothing to it. A spell of
 * one strong enough carves what it shelters and its cells. A guest pass of its sect, carried, lets one through while it is good; turning
 * on the sect takes it back. And Heaven-Severing's cut in the land half its blade's height.
 */
public class BarrierPassGameTests implements FabricGameTest {
	/** A survival player at {@code realm}'s {@code stage}, out of the player list. */
	private static ServerPlayer at(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "barrier"));
		p.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(p).setState(realm, stage, 0);
		CultivationManager.refresh(p);
		return p;
	}

	/** A core at the test's spot bound to a sect as strong as Core Formation Early, its barrier {@code radius} round. */
	private static FormationCoreBlockEntity core(GameTestHelper helper, int radius) {
		BlockPos rel = new BlockPos(6, 2, 6);
		helper.setBlock(rel.below(), Blocks.STONE);
		helper.setBlock(rel, ModBlocks.FORMATION_CORE);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
		core.bindToSect(UUID.randomUUID(), "Test", radius, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		return core;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void theStrongWearABarrierAndARealmAboveShattersIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormationCoreBlockEntity core = core(helper, Formations.MIN_RADIUS);
		helper.runAfterDelay(45, () -> {
			Formation formation = Formations.get(level, core.getFormationId());
			helper.assertTrue(formation != null && formation.isRaised(), "The barrier is up");
			BlockPos shell = formation.center.offset(Formations.MIN_RADIUS, 0, 0);
			double full = core.getBattery();
			Formations.batter(level, formation, at(helper, Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION), 1, shell);
			helper.assertTrue(core.getBattery() == full, "One too weak to break it does nothing to it");
			Formations.batter(level, formation, at(helper, Realm.CORE_FORMATION, Stage.EARLY), 1, shell);
			double worn = core.getBattery();
			helper.assertTrue(Math.abs(full - worn - Formations.BATTER_COST) < 1.0e-6, "One just strong enough wears it by a blow's worth (" + (full - worn) + ")");
			Formations.batter(level, formation, at(helper, Realm.CORE_FORMATION, Stage.LATE), 1, shell);
			helper.assertTrue(Math.abs(worn - core.getBattery() - Formations.BATTER_COST * 2) < 1.0e-6, "...two stages above, twice that (" + (worn - core.getBattery()) + ")");
			helper.assertTrue(formation.isRaised(), "...and it stands");
			ServerPlayer immortal = at(helper, Realm.NASCENT_SOUL, Stage.EARLY);
			Formations.batter(level, formation, immortal, 1, shell);
			helper.assertTrue(!formation.isRaised() && core.cooldownSeconds() > 0, "A realm above it, a single blow shatters the whole of it (" + core.cooldownSeconds() + " s)");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void theStrongsSpellsCarveWhatABarrierShelters(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		FormationCoreBlockEntity core = core(helper, 6);
		helper.runAfterDelay(45, () -> {
			Formation formation = Formations.get(level, core.getFormationId());
			BlockPos inside = formation.center.offset(2, -1, 0);
			ServerPlayer weak = at(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY), strong = at(helper, Realm.CORE_FORMATION, Stage.MID);
			helper.assertTrue(Formations.sheltersFrom(level, inside, weak) && !Formations.sheltersFrom(level, inside, strong),
					"What it shelters is kept from a weaker caster's spells, not a stronger's");
			BlockPos cell = null;
			for (BlockPos pos : BlockPos.betweenClosed(formation.center.offset(-7, -1, -7), formation.center.offset(7, 7, 7))) {
				if (level.getBlockState(pos).getBlock() instanceof SectBarrierBlock) {
					cell = pos.immutable();
					break;
				}
			}
			helper.assertTrue(cell != null, "Its shell is built");
			double full = core.getBattery();
			helper.assertTrue(!HeavenSpells.carvable(level, cell, weak), "A weaker caster's spell can't take a cell of its shell");
			helper.assertTrue(HeavenSpells.carvable(level, cell, strong) && core.getBattery() < full, "A stronger's takes it, as the land, the barrier the worse for it");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGuestPassLetsOneThroughWhileItIsGood(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		UUID sect = UUID.randomUUID(), other = UUID.randomUUID();
		Formation formation = Formations.create(level, UUID.randomUUID(), Formation.Kind.CORE, helper.absolutePos(new BlockPos(6, 2, 6)), Formations.MIN_RADIUS);
		Formations.update(level, formation, PlayerCultivation.LAST_RANK, null, sect);
		ServerPlayer visitor = at(helper, Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		long now = level.getGameTime();
		try {
			helper.assertTrue(!formation.allows(visitor), "One not of the sect isn't let through as its own");
			visitor.getInventory().add(SectPassItem.of(other, "Other", now + 100));
			helper.assertTrue(!formation.allows(visitor), "...nor with another sect's pass");
			visitor.getInventory().add(SectPassItem.of(sect, "Test", now - 1));
			helper.assertTrue(!formation.allows(visitor), "...nor with one lapsed");
			visitor.getInventory().add(SectPassItem.of(sect, "Test", now + SectPassItem.LASTS));
			helper.assertTrue(formation.allows(visitor), "Its own pass, good, carried: let through");
			helper.assertTrue(SectPassItem.revoke(visitor, sect) == 2 && !formation.allows(visitor), "Turning on the sect, its passes crumble");
			helper.assertTrue(MetalSpells.SEVER_CUT_SHARE == 0.5, "Heaven-Severing cuts the land half as tall as its blade");
			helper.succeed();
		} finally {
			Formations.dissolve(level, formation.id);
		}
	}
}
