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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The spells' fixes of 2026-10-24 (the user's asks and reports): a player's spells wait out no cooldowns while the gamerule spellCooldowns
 * is off (its default), a cultivator's still do; the spells that pick out foes take a rogue cultivator minding its own business (a Sword
 * Prison struck none: HeavenSpells#mark), their healing not mending it (HeavenSpells#blessed); and the land's new wounds: a flood's basin
 * filled as it is cut (Devastation#flood), a claw's gouge (Devastation#gouge), a shaft burnt up to the sky with its walls on fire
 * (Devastation#shaft). The land's tests on a block of stone of their own, high over the test's spot, taken up again after.
 */
public class SpellFixesGameTests implements FabricGameTest {
	/** A player who may wound the land (a survival player: HeavenSpells#carvesLand), out of the player list. */
	private static ServerPlayer carver(GameTestHelper helper) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "carver"));
		p.setGameMode(GameType.SURVIVAL);
		return p;
	}

	/** A block of stone {@code r} round and {@code deep} deep, its top at {@code top}. */
	private static void stone(ServerLevel level, BlockPos top, int r, int deep) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = 0; dy < deep; dy++) level.setBlock(top.offset(dx, -dy, dz), Blocks.STONE.defaultBlockState(), 2);
			}
		}
	}

	/** Everything {@code r} round from {@code deep} below {@code top} to {@code up} over it, made air (water, fire and all). */
	private static void clear(ServerLevel level, BlockPos top, int r, int deep, int up) {
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = -deep; dy <= up; dy++) level.setBlock(top.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_cooldown_rule")
	public void playersSpellsHaveNoCooldownsWhileTheRuleIsOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = carver(helper);
		boolean was = level.getGameRules().getBoolean(ModGameRules.SPELL_COOLDOWNS);
		try {
			level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(false, level.getServer());
			helper.assertTrue(!Spells.cooldownsHeld(p), "The rule off (its default): a player's spells wait out no cooldowns");
			level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(true, level.getServer());
			helper.assertTrue(Spells.cooldownsHeld(p), "...on, they do");
			CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
			level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(false, level.getServer());
			helper.assertTrue(Spells.cooldownsHeld(npc), "A cultivator keeps its cooldowns whatever the rule");
			npc.discard();
			helper.succeed();
		} finally {
			level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(was, level.getServer());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRogueMindingItsOwnBusinessIsStillAFoesMark(GameTestHelper helper) {
		ServerPlayer p = carver(helper);
		CultivatorNpc rogue = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		rogue.setNoAi(true);
		try {
			helper.assertTrue(HeavenSpells.friendly(p, rogue), "A rogue not fighting the caster is no enemy of its (HeavenSpells#friendly)...");
			helper.assertTrue(HeavenSpells.mark(p, rogue), "...but the spells that pick out foes take it (a Sword Prison struck none before)");
			helper.assertTrue(!HeavenSpells.blessed(p, rogue), "...and their healing doesn't mend it");
			helper.succeed();
		} finally {
			rogue.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_land_flood", timeoutTicks = 200)
	public void aFloodFillsItsBasinAsItIsCut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = carver(helper);
		BlockPos top = helper.absolutePos(new BlockPos(1, HeavenSpellTesting.FLOOR_HEIGHT, 1));
		stone(level, top, 12, 8);
		clear(level, top.above(), 12, 0, 6);
		Vec3 at = Vec3.atBottomCenterOf(top.above());
		Devastation.flood(p, level, at, 8, 4, 0, 4); // the heart first
		Devastation.flood(p, level, at, 8, 4, 4, 9); // then out to its rim
		helper.succeedWhen(() -> {
			helper.assertTrue(Devastation.pending() == 0, "...its basin still being cut");
			try {
				BlockState heart = level.getBlockState(top.below()), out = level.getBlockState(top.offset(5, 0, 0)), beyond = level.getBlockState(top.offset(11, 0, 0));
				helper.assertTrue(heart.is(Blocks.WATER) && out.is(Blocks.WATER), "Water fills its basin, at its heart and out toward its rim (" + heart + ", " + out + ")");
				helper.assertTrue(beyond.is(Blocks.STONE), "...not past it");
			} finally {
				clear(level, top, 13, 10, 8);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_land_gouge", timeoutTicks = 200)
	public void aClawGougesTheLand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = carver(helper);
		BlockPos top = helper.absolutePos(new BlockPos(1, HeavenSpellTesting.FLOOR_HEIGHT, 1));
		stone(level, top, 10, 8);
		clear(level, top.above(), 10, 0, 6);
		Vec3 middle = Vec3.atBottomCenterOf(top.above());
		Devastation.gouge(p, level, middle.add(-8, 0, 0), middle.add(8, 0, 0), 2.5, 5);
		helper.succeedWhen(() -> {
			helper.assertTrue(Devastation.pending() == 0, "...its gouge still being cut");
			try {
				helper.assertTrue(level.getBlockState(top).isAir() && level.getBlockState(top.below(3)).isAir(), "A gouge down the middle of its stroke");
				helper.assertTrue(level.getBlockState(top.offset(0, 0, 4)).is(Blocks.STONE), "...as wide as it is, no more");
				helper.assertTrue(level.getBlockState(top.offset(9, 0, 0)).is(Blocks.STONE), "...shallowing to nothing at its ends");
			} finally {
				clear(level, top, 11, 10, 8);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_land_shaft", timeoutTicks = 200)
	public void aShaftIsBurntUpToTheSky(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = carver(helper);
		// A hill of stone over the spot, 12 high: the shaft burnt from its foot up through it.
		BlockPos foot = helper.absolutePos(new BlockPos(1, HeavenSpellTesting.FLOOR_HEIGHT, 1));
		stone(level, foot.above(12), 7, 14);
		Devastation.shaft(p, level, Vec3.atBottomCenterOf(foot), 3, 64);
		helper.succeedWhen(() -> {
			helper.assertTrue(Devastation.pending() == 0, "...its shaft still being burnt");
			try {
				helper.assertTrue(level.getBlockState(foot.above(1)).isAir() && level.getBlockState(foot.above(11)).isAir(), "A shaft up through the hill to the sky");
				int burnt = 0;
				for (BlockPos pos : BlockPos.betweenClosed(foot.offset(-6, 0, -6), foot.offset(6, 12, 6))) {
					BlockState state = level.getBlockState(pos);
					if (state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.NETHERRACK) || state.is(Blocks.BLACKSTONE) || state.is(net.minecraft.tags.BlockTags.FIRE)) burnt++;
				}
				helper.assertTrue(burnt > 20, "...its walls burnt and on fire (" + burnt + ")");
			} finally {
				clear(level, foot.above(12), 8, 16, 16);
			}
		});
	}
}
