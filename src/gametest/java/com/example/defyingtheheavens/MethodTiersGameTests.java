package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The foundations one after another (see GhostMethod, GhostMethods): each carrying the ghost path from Qi Refining up to its own
 * realm, any taken up by any but a mortal, a later one at the depth the ghost path stands, longer and rarer the deeper they go;
 * who among the dead knows which, what their slips are worth, a king's canon left behind when it falls, and the deeper
 * foundations' spells.
 */
public class MethodTiersGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFoundationsFollowOneAnotherToFourAxis(GameTestHelper helper) {
		GhostMethod method = GhostMethod.NETHERWORLD_BREATHING;
		for (Realm realm : Realm.values()) {
			helper.assertTrue(method != null && method.realm() == realm && method.tier() == realm.ordinal() + 1 && GhostMethod.foundationOf(realm) == method,
					"A foundation for " + realm.getId());
			method = method.next();
		}
		helper.assertTrue(method == null, "The Six Paths Reincarnation Canon is the last");
		helper.assertTrue(!GhostMethod.GHOST_FIRE_SCRIPTURE.isFoundation() && GhostMethod.WANDERING_SOUL_STEPS.tier() == 0, "The fire and the steps carry no realm");
		double before = 0;
		for (GhostMethod m : GhostMethod.values()) {
			if (!m.isFoundation()) continue;
			double total = 0;
			for (int layer = 1; layer <= m.layers(); layer++) total += GhostMethods.layerCost(m, layer);
			helper.assertTrue(total > before, m.getId() + " takes longer than the one before (" + total / 3600 + " h)");
			before = total;
		}
		helper.assertTrue(GhostMethods.layerCost(GhostMethod.NETHERWORLD_BREATHING, 1) == 180 && GhostMethods.layerCost(GhostMethod.YELLOW_SPRINGS_MARROW, 1) == 180
				&& GhostMethods.layerCost(GhostMethod.YELLOW_SPRINGS_MARROW, 10) == 360 && GhostMethods.layerCost(GhostMethod.SIX_PATHS_CANON, 46) == 180 * 16,
				"Every foundation's first layer 3 minutes; Foundation Building's first 6; Four Axis's 48");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anyFoundationIsTakenUpALaterOneWhereTheGhostPathStands(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0); // its meridians open: a mortal takes up nothing (TwoPathsGameTests)
		for (GhostMethod m : GhostMethod.values()) helper.assertTrue(GhostMethods.canLearn(c, m), "Anyone takes up " + m.getId() + ", from Qi Refining");
		PlayerCultivation high = new PlayerCultivation();
		high.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(GhostMethods.startLayer(high.netherworld(), GhostMethod.SIX_PATHS_CANON)[0] == 1,
				"However high one's ordinary cultivation, the ghost path starts at the first layer");

		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.GRAND_PERFECTION);
		NetherworldState s = CultivationManager.get(player).netherworld();
		try {
			GhostMethods.learn(player, GhostMethod.NETHERWORLD_BREATHING);
			GhostMethods.learn(player, GhostMethod.GHOST_FIRE_SCRIPTURE);
			helper.assertTrue(s.activeMethod() == GhostMethod.NETHERWORLD_BREATHING, "The first learned is practised");
			s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 9, GhostMethods.layerCost(GhostMethod.NETHERWORLD_BREATHING, 9));
			helper.assertTrue(GhostMethods.learn(player, GhostMethod.YELLOW_SPRINGS_MARROW) && s.activeMethod() == GhostMethod.YELLOW_SPRINGS_MARROW
					&& s.layer(GhostMethod.YELLOW_SPRINGS_MARROW) == 9, "The Marrow Art, learned, starts at the ninth and takes the practice over: it is the way on");
			helper.assertTrue(GhostMethods.cultivate(s, GhostMethod.YELLOW_SPRINGS_MARROW, 1) == 1
					&& GhostMethods.ghostRealm(s) == Realm.FOUNDATION_BUILDING, "...the Breathing Art's progress with it: a breath more, Foundation Building");
			Spells.practise(player, GhostMethod.GHOST_FIRE_SCRIPTURE);
			GhostMethods.give(player, GhostMethod.GHOST_CORE_CANON);
			helper.assertTrue(s.layer(GhostMethod.GHOST_CORE_CANON) == 10 && s.activeMethod() == GhostMethod.GHOST_FIRE_SCRIPTURE,
					"The Canon at the depth reached (10); a method chosen over the foundations stays chosen");
			helper.assertTrue(GhostMethods.deepestFoundation(s) == GhostMethod.GHOST_CORE_CANON, "Of two as deep, the one that goes further carries the ghost path");
		} finally {
			Spells.forget(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeepFoundationsAreCarvedSeldomAndMostlyOnThePeaks(GameTestHelper helper) {
		RandomSource random = RandomSource.create(42);
		Map<GhostMethod, Integer> lowlands = new EnumMap<>(GhostMethod.class), peaks = new EnumMap<>(GhostMethod.class);
		for (int i = 0; i < 50_000; i++) lowlands.merge(MethodSteleFeature.pick(random, false), 1, Integer::sum);
		for (int i = 0; i < 200_000; i++) peaks.merge(MethodSteleFeature.pick(random, true), 1, Integer::sum);
		for (GhostMethod m : GhostMethod.values()) {
			if (m.tier() >= 4) helper.assertTrue(!lowlands.containsKey(m), m.getId() + " is never carved off the peaks");
		}
		int lowCanon = lowlands.getOrDefault(GhostMethod.GHOST_CORE_CANON, 0);
		helper.assertTrue(lowCanon > 0 && lowCanon < lowlands.get(GhostMethod.YELLOW_SPRINGS_MARROW) && lowlands.get(GhostMethod.YELLOW_SPRINGS_MARROW) < lowlands.get(GhostMethod.NETHERWORLD_BREATHING),
				"Off the peaks the Ghost Core Canon now and then, the Marrow Art more, the Breathing Art most of all (" + lowCanon + ")");
		int canon = peaks.getOrDefault(GhostMethod.GHOST_CORE_CANON, 0), sutra = peaks.getOrDefault(GhostMethod.NINE_NETHER_SOUL_SUTRA, 0),
				yama = peaks.getOrDefault(GhostMethod.YAMA_JUDGEMENT, 0), six = peaks.getOrDefault(GhostMethod.SIX_PATHS_CANON, 0);
		helper.assertTrue(canon > sutra && sutra > yama && yama > six && six > 0,
				"On the peaks, each deeper rarer: " + canon + ", " + sutra + ", " + yama + ", " + six + " in 200,000");
		helper.assertTrue(six > 200 && six < 800, "The Six Paths Reincarnation Canon about three times in 1,371 steles there (" + six + ")");
		helper.assertTrue(GhostPalacePiece.kingRealm(0.01f) == Realm.FOUR_AXIS && GhostPalacePiece.kingRealm(0.1f) == Realm.HEAVENLY_BEING
				&& GhostPalacePiece.kingRealm(0.3f) == Realm.NASCENT_SOUL && GhostPalacePiece.kingRealm(0.9f) == Realm.CORE_FORMATION,
				"Palaces' kings: now and then Four Axis, more often Heavenly Being or Nascent Soul, most often Core Formation");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadKnowTheirRealmsFoundationOrALesserMethod(GameTestHelper helper) {
		var level = helper.getLevel();
		int own = 0, lesser = 0;
		for (int i = 0; i < 40; i++) {
			GhostCultivatorNpc ghost = ModEntities.GHOST_CULTIVATOR.create(level);
			ghost.becomeGhost(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID), 0, level.getRandom());
			GhostMethod method = ghost.method();
			helper.assertTrue(method != null && method.tier() <= 2, "A Foundation Building ghost knows nothing deeper than its own realm's (" + method + ")");
			if (method == GhostMethod.YELLOW_SPRINGS_MARROW) own++;
			else lesser++;
		}
		helper.assertTrue(own > 0 && lesser > 0, "Some its realm's foundation, some a lesser method (" + own + ", " + lesser + ")");
		GhostCultivatorNpc young = ModEntities.GHOST_CULTIVATOR.create(level);
		young.becomeGhost(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), 0, level.getRandom());
		helper.assertTrue(young.method() == null, "Below Foundation Building, none");
		GhostCultivatorNpc king = ModEntities.GHOST_CULTIVATOR.create(level);
		king.becomeKing(GhostSystem.KING_RANK, 0, helper.absolutePos(new BlockPos(1, 2, 1)), level.getRandom());
		GhostCultivatorNpc axis = ModEntities.GHOST_CULTIVATOR.create(level);
		axis.becomeKing(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), 0, helper.absolutePos(new BlockPos(2, 2, 1)), level.getRandom());
		helper.assertTrue(king.method() == GhostMethod.GHOST_CORE_CANON && axis.method() == GhostMethod.SIX_PATHS_CANON,
				"A king keeps its realm's foundation: the Canon, or a Four Axis king the Six Paths Reincarnation Canon");
		helper.assertTrue(CultivatorTrades.slipPrice(GhostMethod.NETHERWORLD_BREATHING) == 60 && CultivatorTrades.slipPrice(GhostMethod.GHOST_FIRE_SCRIPTURE) == 60
				&& CultivatorTrades.slipPrice(GhostMethod.YELLOW_SPRINGS_MARROW) == 180 && CultivatorTrades.slipPrice(GhostMethod.GHOST_CORE_CANON) == 540
				&& CultivatorTrades.slipPrice(GhostMethod.SIX_PATHS_CANON) == 14_580, "Slips are worth three times more each foundation deeper");
		boolean sold = CultivatorTrades.goodsFor(axis, RandomSource.create(1)).stream()
				.anyMatch(g -> GhostJadeSlipItem.method(g.item()) == GhostMethod.SIX_PATHS_CANON && g.regard() == Regard.WARM && g.price() == 14_580);
		helper.assertTrue(sold, "A Four Axis king sells its canon, to one it is Warm to");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aFallenKingLeavesItsCanon(GameTestHelper helper) {
		GhostCultivatorNpc king = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(2, 2, 2));
		king.becomeKing(GhostSystem.KING_RANK, 0, helper.absolutePos(new BlockPos(2, 2, 2)), helper.getLevel().getRandom());
		// Counted just before and just after: the test world keeps what earlier runs left lying here.
		AABB where = new AABB(king.blockPosition()).inflate(2);
		java.util.function.IntSupplier canons = () -> helper.getLevel().getEntitiesOfClass(ItemEntity.class, where,
				item -> GhostJadeSlipItem.method(item.getItem()) == GhostMethod.GHOST_CORE_CANON).size();
		int before = canons.getAsInt();
		king.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
		helper.assertTrue(!king.isAlive(), "The king is slain");
		helper.assertTrue(canons.getAsInt() == before + 1, "The Ghost Core Canon's slip lies where it fell");
		helper.succeed();
	}

	/**
	 * Far out of the test grid: the spells reach a dozen blocks and more (Soul Rend 32, the step 24), and in the grid would strike
	 * and drag the next tests' creatures and carry the caster among them.
	 */
	private static final BlockPos SPELL_GROUND = new BlockPos(1_000_000, 150, 1_000_000);

	private static Zombie zombie(ServerLevel level, int dx, int dz) {
		Zombie zombie = EntityType.ZOMBIE.create(level);
		zombie.moveTo(SPELL_GROUND.getX() + dx + 0.5, SPELL_GROUND.getY(), SPELL_GROUND.getZ() + dz + 0.5, 180, 0);
		zombie.setNoAi(true);
		level.addFreshEntity(zombie);
		return zombie;
	}

	private static void standAt(ServerPlayer player, int dx, int dz, float yaw) {
		player.teleportTo(player.serverLevel(), SPELL_GROUND.getX() + dx + 0.5, SPELL_GROUND.getY(), SPELL_GROUND.getZ() + dz + 0.5, yaw, 0);
		player.setYRot(yaw);
		player.setXRot(0);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void theDeeperFoundationsSpellsStrike(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -2; dz <= 12; dz++) level.setBlockAndUpdate(SPELL_GROUND.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
		}
		ServerPlayer player = cultivator(helper, Realm.FOUR_AXIS, Stage.EARLY);
		NetherworldState s = CultivationManager.get(player).netherworld();
		List<Zombie> zombies = new ArrayList<>();
		try {
			for (GhostMethod m : GhostMethod.values()) if (m.isFoundation()) s.setMethod(m, m.layers(), 0);
			s.setGhostQi(10_000);
			for (Spell spell : Spell.values()) {
				if (spell.method() != null && spell.method().isFoundation()) helper.assertTrue(spell.isKnown(CultivationManager.get(player)), spell.getId() + " is known with its foundation perfected");
			}
			standAt(player, 0, 0, 0); // facing south, +z
			// Soul Rend: through both zombies in its line.
			Zombie near = zombie(level, 0, 3), far = zombie(level, 0, 6);
			zombies.add(near);
			zombies.add(far);
			float full = near.getHealth();
			Spells.cast(player, Spell.SOUL_REND);
			helper.assertTrue(near.getHealth() < full && far.getHealth() < full, "Soul Rend strikes every being in its line");
			// Marrow Ward; the Wheel of Samsara.
			Spells.cast(player, Spell.MARROW_WARD);
			helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE) && player.hasEffect(MobEffects.ABSORPTION), "Marrow Ward: bones like iron");
			player.setHealth(player.getMaxHealth() / 3);
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
			Spells.cast(player, Spell.SAMSARA_WHEEL);
			helper.assertTrue(player.getHealth() >= player.getMaxHealth() - 0.01f && !player.hasEffect(MobEffects.POISON), "The Wheel of Samsara makes one whole");
			// Chains of Hell hold a monster; King's Decree cows it.
			Spells.cast(player, Spell.HELL_CHAINS);
			helper.assertTrue(near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && near.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() >= 6, "Chained fast");
			Spells.cast(player, Spell.KINGS_DECREE);
			helper.assertTrue(far.hasEffect(MobEffects.WEAKNESS) && far.hasEffect(MobEffects.GLOWING), "Cowed and shown by the King's Decree");
			// Yama's Verdict: twice over on one below half its health.
			float before = near.getHealth();
			Spells.cast(player, Spell.YAMA_VERDICT);
			helper.assertTrue(!near.isAlive() || near.getHealth() < before, "Judged");
			// Nine Nether Step: a step through the dark, south down the ground.
			Spells.cast(player, Spell.NINE_NETHER_STEP);
			helper.assertTrue(player.getZ() > SPELL_GROUND.getZ() + 3, "Nine Nether Step carries it where it looks (" + player.position() + ")");
			// Six Paths Annihilation: all within its ring unmade.
			standAt(player, 0, 0, 0);
			Zombie ring = zombie(level, 2, 1);
			zombies.add(ring);
			Spells.cast(player, Spell.SIX_PATHS_ANNIHILATION);
			helper.assertTrue(!ring.isAlive(), "Unmade by the Six Paths");
			// Ghost Core Burst and Yellow Springs Tide on a fresh one.
			Zombie burnt = zombie(level, 0, 2);
			zombies.add(burnt);
			Spells.cast(player, Spell.GHOST_CORE_BURST);
			helper.assertTrue(burnt.getHealth() < burnt.getMaxHealth() && burnt.hasEffect(ModEffects.GHOST_FIRE), "Ghost Core Burst burns what is near");
			Spells.cast(player, Spell.YELLOW_SPRINGS_TIDE);
			helper.assertTrue(!burnt.isAlive() || burnt.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The tide slows what it reaches");
		} finally {
			Spells.forget(player.getUUID());
			for (Zombie zombie : zombies) zombie.discard();
		}
		helper.succeed();
	}
}
