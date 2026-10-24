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
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.UUID;

/**
 * Cultivators casting spells (CultivatorSpells, the user's ask of 2026-10-24): what one knows (its method's element, to its rank; in a
 * spar, to the realm it holds back to), that its spell strikes whom it fights and spares its own and those standing by, and that it
 * leaves the land as it was.
 */
public class NpcSpellsGameTests implements FabricGameTest {
	/** A sect of {@code element}: a sect's element is picked from its id (Sect#element), so ids are drawn until one is. */
	private static Sect sectOf(GameTestHelper helper, QiElement element) {
		ServerLevel level = helper.getLevel();
		Sect sect;
		do {
			sect = new Sect(UUID.randomUUID());
		} while (sect.element() != element);
		sect.name = "Spell Sect";
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		SectData.get(level.getServer()).put(sect);
		return sect;
	}

	/** One of {@code sect}'s people, still (no goals of its own), of {@code realm} and {@code stage}, its qi full. */
	private static CultivatorNpc member(GameTestHelper helper, Sect sect, Realm realm, Stage stage, BlockPos at, NpcTitle title) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, at);
		npc.setNoAi(true);
		npc.setRank(PlayerCultivation.rank(realm, stage));
		npc.joinSect(sect, sect.members.size());
		sect.members.put(npc.getUUID(), new Sect.Member(npc.getUUID(), npc.getRank(), sect.members.size()));
		npc.setTitle(title);
		npc.setHealth(npc.getMaxHealth());
		npc.cultivation().setQi(npc.cultivation().maxQi());
		return npc;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCultivatorKnowsItsElementsSpellsToItsRank(GameTestHelper helper) {
		Sect sect = sectOf(helper, QiElement.METAL);
		CultivatorNpc elder = member(helper, sect, Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, new BlockPos(2, 2, 2), NpcTitle.ELDER);
		Zombie partner = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 2));
		CultivatorNpc commoner = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(0, 2, 2));
		try {
			partner.setNoAi(true);
			commoner.setNoAi(true);
			commoner.setRank(CultivatorNpc.MORTAL);
			List<Spell> known = CultivatorSpells.repertoire(elder);
			helper.assertTrue(known.contains(Spell.FLYING_SWORD) && known.contains(Spell.SWORD_RAIN) && known.contains(Spell.PIERCING_SWORD_LIGHT)
					&& known.contains(Spell.VAJRA_BODY), "A metal sect's Nascent Soul elder knows metal's spells to its realm (" + known + ")");
			helper.assertTrue(!known.contains(Spell.MOUNTAIN_CLEAVE) && !known.contains(Spell.FLAME_BOLT) && !known.contains(Spell.MINERAL_PURIFICATION),
					"...none of a deeper realm's, another element's, or a player's own (ore into the hands)");
			elder.beginSpar(partner, PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
			known = CultivatorSpells.repertoire(elder);
			helper.assertTrue(known.contains(Spell.FLYING_SWORD) && !known.contains(Spell.ADVANCED_FLYING_SWORD_TECHNIQUE) && !known.contains(Spell.PIERCING_SWORD_LIGHT),
					"Holding back to Qi Refining in a spar, it casts no deeper (" + known + ")");
			elder.endSpar(partner);
			helper.assertTrue(CultivatorSpells.repertoire(commoner).isEmpty(), "A commoner casts nothing");
			helper.succeed();
		} finally {
			sect.extinct = true;
			elder.discard();
			partner.discard();
			commoner.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aCultivatorsSpellStrikesWhomItFightsAndLeavesTheLand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 7, 6);
		int y = HeavenSpellTesting.FLOOR_HEIGHT;
		Sect sect = sectOf(helper, QiElement.METAL);
		CultivatorNpc elder = member(helper, sect, Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION, new BlockPos(-4, y, 1), NpcTitle.ELDER);
		CultivatorNpc fellow = member(helper, sect, Realm.QI_REFINING, Stage.EARLY, new BlockPos(-1, y, 1), NpcTitle.OUTER_DISCIPLE);
		Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, y, 1));
		Zombie foe = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, y, 1));
		BlockPos wall = helper.absolutePos(new BlockPos(7, y, 1));
		ServerPlayer player = FakePlayer.get(level, new GameProfile(UUID.randomUUID(), "npc-spells-player"));
		try {
			player.setGameMode(GameType.SURVIVAL);
			villager.setNoAi(true);
			foe.setNoAi(true);
			level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
			level.setBlock(wall.above(), Blocks.STONE.defaultBlockState(), 3);
			elder.setTarget(foe);
			helper.assertTrue(!HeavenSpells.spared(elder, foe) && HeavenSpells.spared(elder, fellow) && HeavenSpells.spared(elder, villager),
					"Its spells reach whom it fights, not its own nor those standing by");
			CultivatorSpells.face(elder, foe);
			double qi = elder.cultivation().getQi();
			helper.assertTrue(Spells.castBy(elder, Spell.PIERCING_SWORD_LIGHT), "It casts Piercing Sword Light");
			helper.assertTrue(Math.abs(elder.cultivation().getQi() - (qi - Spell.PIERCING_SWORD_LIGHT.qiCost())) < 1.0e-6
					&& Spells.cooldownLeft(elder, Spell.PIERCING_SWORD_LIGHT) > 0, "...out of its own qi, on its own cooldown");
			helper.assertTrue(!Spells.castBy(elder, Spell.PIERCING_SWORD_LIGHT), "...not again while it cools");
			helper.assertTrue(foe.getHealth() < foe.getMaxHealth(), "The beam cuts its foe (" + foe.getHealth() + ")");
			helper.assertTrue(fellow.getHealth() == fellow.getMaxHealth() && villager.getHealth() == villager.getMaxHealth(),
					"...and passes its fellow and the villager on its line by");
			helper.assertTrue(level.getBlockState(wall).is(Blocks.STONE) && level.getBlockState(wall.above()).is(Blocks.STONE),
					"It bores no tunnel: a cultivator's spells leave the land as it was");
			helper.assertTrue(!HeavenSpells.carvable(level, wall, elder) && !Devastation.mayChange(elder, level, wall.above(2))
					&& HeavenSpells.carvable(level, wall, player), "Nothing it casts carves or sets anything in the land, as a player's would");
			elder.setHealth(elder.getMaxHealth() * 0.3f);
			Spell mending = CultivatorSpells.choose(elder, foe);
			helper.assertTrue(mending != null && CultivatorSpells.use(mending) == CultivatorSpells.Use.WARD, "Badly hurt, it wards itself first (" + mending + ")");
			helper.succeed();
		} finally {
			sect.extinct = true;
			for (net.minecraft.world.entity.Entity e : new net.minecraft.world.entity.Entity[] {elder, fellow, villager, foe}) e.discard();
			level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(wall.above(), Blocks.AIR.defaultBlockState(), 3);
			floor.remove();
		}
	}

	/**
	 * A cultivator left to itself in a fight (its own goals, no hand on it) casts its spells as it fights. A rogue: a sect's people
	 * keep their day (duties, sleep: NpcLife) by the test world's clock, which kept one from its fight half the time. A batch of its
	 * own: it runs for many ticks, and the tests beside it clear the land round their floors of whatever stands there (its foe too).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400, batch = "npc_spells_fight")
	public void aCultivatorInAFightCastsOfItsOwnAccord(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 7, 6);
		int y = HeavenSpellTesting.FLOOR_HEIGHT;
		CultivatorNpc rogue = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(-4, y, 1));
		Zombie foe = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, y, 1));
		rogue.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION));
		rogue.setHealth(rogue.getMaxHealth());
		rogue.cultivation().setQi(rogue.cultivation().maxQi());
		foe.setNoAi(true);
		foe.setInvulnerable(true); // its blows and spells land on nothing, and the fight goes on
		rogue.setTarget(foe);
		helper.succeedWhen(() -> {
			helper.assertTrue(!rogue.cultivation().netherworld().spellReady().isEmpty(), "Fighting, it casts (" + rogue.getTarget() + ")");
			rogue.discard();
			foe.discard();
			floor.remove();
		});
	}
}
