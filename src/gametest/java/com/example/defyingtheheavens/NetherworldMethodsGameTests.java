package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;

/**
 * The ghost methods and their spells (phase 4, see GhostMethods, Spells): what a layer costs (all of them to be had wherever
 * ghostly qi flows), how fast each cultivator goes and where, what's saved, what each layer unlocks, and the spells' costs,
 * cooldowns and effects.
 */
public class NetherworldMethodsGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL); // spells are free in creative
		CultivationManager.get(player).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(player);
		return player;
	}

	private static void cleanUp(ServerPlayer player) {
		Spells.forget(player.getUUID());
		GhostSystem.forget(player.getUUID());
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void layersCostTheirMinutesAndSettleInOnHalf(GameTestHelper helper) {
		int total = 0;
		for (int minutes : GhostMethods.MINUTES) total += minutes;
		helper.assertTrue(GhostMethods.layerCost(GhostMethod.GHOST_FIRE_SCRIPTURE, 1) == 300 && GhostMethods.layerCost(GhostMethod.GHOST_FIRE_SCRIPTURE, 9) == 7200
						&& total == 390, "The fire: 5 minutes for the first layer, two hours for the ninth, six and a half hours in all");
		NetherworldState s = new NetherworldState();
		s.setMethod(GhostMethod.GHOST_FIRE_SCRIPTURE, 1, 0);
		s.setPath(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0); // the path far enough for all nine
		int upTo = GhostMethods.understoodTo(s, GhostMethod.GHOST_FIRE_SCRIPTURE);
		helper.assertTrue(GhostMethods.settle(s, GhostMethod.GHOST_FIRE_SCRIPTURE, 240, upTo) == 1
				&& s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 2 && s.progress(GhostMethod.GHOST_FIRE_SCRIPTURE) == 0, "Half the second layer's eight minutes: it settles in");
		helper.assertTrue(GhostMethods.settle(s, GhostMethod.GHOST_FIRE_SCRIPTURE, 1_000_000, upTo) == 7
				&& s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 9 && s.progress(GhostMethod.GHOST_FIRE_SCRIPTURE) == 0,
				"On to the ninth, all of it understood, nothing left settling");
		helper.assertTrue(GhostMethods.settle(s, GhostMethod.NETHERWORLD_BREATHING, 1000, 9) == 0
				&& !s.knows(GhostMethod.NETHERWORLD_BREATHING), "A method never learned gains nothing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachCultivatorGoesAtTheirOwnPace(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		NetherworldState s = c.netherworld();
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY) - 0.7 * GhostMethods.NO_METHOD_FACTOR) < 1.0e-9,
				"No ghost path yet: as its Qi Refining, -30%, and no foundation known: three quarters of that");
		s.setMethod(GhostMethod.NINE_NETHER_SOUL_SUTRA, 1, 0);
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY)); // the ghost path at Nascent Soul
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY) - 1.0) < 1.0e-9,
				"A living ghost path of Nascent Soul in a Soul Sand Valley: the base rate");
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.NETHERWORLD) - 10.0) < 1.0e-9
				&& Math.abs(GhostMethods.ratePerSecond(c, GhostQi.NETHERWORLD * GhostQi.PEAKS_FACTOR) - 12.0) < 1.0e-9
				&& Math.abs(GhostMethods.ratePerSecond(c, GhostQi.NETHER) - 0.5) < 1.0e-9
				&& Math.abs(GhostMethods.ratePerSecond(c, GhostQi.OVERWORLD) - 0.25) < 1.0e-9,
				"Ten times it in the Netherworld, twelve on the Nine Nether Peaks, half in the rest of the Nether, a quarter in the Overworld");
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY) - 1.0) < 1.0e-9, "The ordinary realm counts for nothing");
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION));
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY) - 0.9) < 1.0e-9, "The ghost path a realm below (Core Formation): -10%");
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY));
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY) - 1.1) < 1.0e-9, "A realm above: +10%");
		s.setGhost(true);
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY) - 1.65) < 1.0e-9, "The dead, made of yin, half as fast again");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void methodsAndTheSpellWheelAreSavedById(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		NetherworldState s = c.netherworld();
		s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 5, 123.5);
		s.setMethod(GhostMethod.WANDERING_SOUL_STEPS, 2, 7);
		s.setPath(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 50);
		s.setCultivatingGhost(true);
		s.setActiveMethod(GhostMethod.WANDERING_SOUL_STEPS);
		s.setReadiedSpell(Spell.SOUL_DASH);
		s.setSpellSlot(3, Spell.SOUL_DASH);
		s.setSpellSlot(0, Spell.GHOSTLY_WAIL);
		NetherworldState loaded = PlayerCultivation.load(c.save()).netherworld();
		helper.assertTrue(loaded.layer(GhostMethod.NETHERWORLD_BREATHING) == 5 && loaded.progress(GhostMethod.NETHERWORLD_BREATHING) == 123.5
				&& loaded.layer(GhostMethod.WANDERING_SOUL_STEPS) == 2 && !loaded.knows(GhostMethod.GHOST_FIRE_SCRIPTURE), "Each method's layer and settling");
		helper.assertTrue(loaded.pathRank() == PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID) && loaded.pathCultivation() == 50 && loaded.cultivatingGhost(),
				"The ghost path's bar, and the qi drawn in");
		helper.assertTrue(loaded.activeMethod() == GhostMethod.WANDERING_SOUL_STEPS && loaded.readiedSpell() == Spell.SOUL_DASH, "The practised method, the readied spell");
		helper.assertTrue(loaded.spellSlot(3) == Spell.SOUL_DASH && loaded.spellSlot(0) == Spell.GHOSTLY_WAIL && loaded.spellSlot(1) == null, "The spell wheel");
		// Saved before the foundations spanned the realms below their own: a deeper foundation's layer moves up to the same stage.
		net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag(), learned = new net.minecraft.nbt.CompoundTag();
		for (Object[] entry : new Object[][] {{GhostMethod.NETHERWORLD_BREATHING, 9}, {GhostMethod.YELLOW_SPRINGS_MARROW, 9}, {GhostMethod.GHOST_CORE_CANON, 3},
				{GhostMethod.GHOST_FIRE_SCRIPTURE, 4}}) {
			net.minecraft.nbt.CompoundTag one = new net.minecraft.nbt.CompoundTag();
			one.putInt("Layer", (int) entry[1]);
			one.putDouble("Progress", 42);
			learned.put(((GhostMethod) entry[0]).getId(), one);
		}
		old.put("Methods", learned);
		NetherworldState migrated = new NetherworldState();
		migrated.load(old);
		helper.assertTrue(migrated.layer(GhostMethod.NETHERWORLD_BREATHING) == 9 && migrated.layer(GhostMethod.YELLOW_SPRINGS_MARROW) == 18
				&& migrated.layer(GhostMethod.GHOST_CORE_CANON) == 21 && migrated.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 4
				&& GhostMethods.ghostRank(migrated) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID),
				"An old save's Ghost Core Canon at its third layer is its 21st now: Core Formation Mid as before");
		// ...and saved before the ghost path had a bar of its own (2026-10-19): the bar starts where its deepest foundation stood, its
		// progress through the layer the bar's; the foundations' settling starts afresh, the fire's kept.
		// (As far through the bar's Core Formation Mid as 42 seconds were through that stage's old layers, 21 to 23.)
		double oldMid = MethodLayers.cost(21) + MethodLayers.cost(22) + MethodLayers.cost(23);
		double expected = 42 / oldMid * GhostMethods.required(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID));
		helper.assertTrue(Math.abs(migrated.pathCultivation() - expected) < 1.0e-6 && migrated.progress(GhostMethod.GHOST_CORE_CANON) == 0
				&& migrated.progress(GhostMethod.GHOST_FIRE_SCRIPTURE) == 42,
				"The Canon's 42 seconds into its 21st layer: as far into the bar's Core Formation Mid (" + migrated.pathCultivation() + ")");
		NetherworldState again = new NetherworldState();
		again.load(migrated.save());
		helper.assertTrue(again.layer(GhostMethod.GHOST_CORE_CANON) == 21 && again.layer(GhostMethod.YELLOW_SPRINGS_MARROW) == 18, "...and moved only once");
		// Placing a spell already on the wheel trades the two slots.
		loaded.setSpellSlot(0, Spell.SOUL_DASH);
		helper.assertTrue(loaded.spellSlot(0) == Spell.SOUL_DASH && loaded.spellSlot(3) == Spell.GHOSTLY_WAIL, "Moved onto a full slot, they trade places");
		NetherworldState empty = PlayerCultivation.load(new PlayerCultivation().save()).netherworld();
		helper.assertTrue(!empty.knowsAnyMethod() && empty.activeMethod() == null && empty.readiedSpell() == null, "Nothing, for one who never learned");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void layersUnlockSpellsAndAbilities(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.NASCENT_SOUL);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		try {
			GhostMethods.learn(player, GhostMethod.GHOST_FIRE_SCRIPTURE);
			helper.assertTrue(s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 3 && s.activeMethod() == GhostMethod.GHOST_FIRE_SCRIPTURE
					&& GhostMethods.ghostRank(s) < 0, "Learned as far as a ghost path not yet begun carries it (three layers), and practised, being the first");
			helper.assertTrue(Spell.GHOST_FIRE.isKnown(c) && !Spell.GHOST_FIRE_BOLT.isKnown(c) && s.spellSlot(0) == Spell.GHOST_FIRE,
					"Ghost Fire at once, on the wheel; the bolt not yet");
			helper.assertTrue(!GhostMethods.learn(player, GhostMethod.GHOST_FIRE_SCRIPTURE), "Nothing more to learn of it");
			s.setMethod(GhostMethod.GHOST_FIRE_SCRIPTURE, 4, 0);
			GhostMethods.afterLayer(player, s);
			helper.assertTrue(Spell.GHOST_FIRE_BOLT.isKnown(c) && s.spellSlot(1) == Spell.GHOST_FIRE_BOLT, "The fourth layer: the bolt, next on the wheel");
			GhostMethods.learn(player, GhostMethod.NETHERWORLD_BREATHING);
			helper.assertTrue(s.activeMethod() == GhostMethod.GHOST_FIRE_SCRIPTURE, "A second method learned doesn't take over the practice");
			helper.assertTrue(!Ability.GHOST_EYE.isUnlocked(c), "No Ghost Eye at the first layer of the Breathing Art...");
			s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 6, 0);
			helper.assertTrue(Ability.GHOST_EYE.isUnlocked(c) && Ability.NETHERWORLD_VEIL.isUnlocked(c), "...at the third; the Veil at the sixth");
			s.setMethod(GhostMethod.WANDERING_SOUL_STEPS, 4, 0);
			helper.assertTrue(Ability.YIN_WIND_STEPS.isUnlocked(c) && !Ability.GHOST_FLIGHT.isUnlocked(c), "Yin Wind Steps alive");
			s.setGhost(true);
			// Ghost Flight is retired (2026-10-18): the dead fly freely without it (TwoPaths#fliesFreely; BothPathsGameTests).
			helper.assertTrue(!Ability.GHOST_FLIGHT.isUnlocked(c) && TwoPaths.fliesFreely(c), "...and the dead fly freely, no Ghost Flight needed");
			s.setGhost(false);
			helper.assertTrue(Math.abs(GhostQi.poolMax(c) - (GhostQi.BASE_POOL + 14 * GhostQi.POOL_PER_LAYER + 6 * GhostQi.BREATHING_POOL_PER_LAYER)) < 1.0e-9,
					"The pool grows with every layer, and the Breathing Art's twice over");
			c.setAbilityEnabled(Ability.GHOST_EYE, true);
			c.setAbilityEnabled(Ability.YIN_WIND_STEPS, true);
			helper.assertTrue(Math.abs(GhostMethods.upkeep(c) - (GhostMethods.GHOST_EYE_COST + GhostMethods.YIN_WIND_COST)) < 1.0e-9,
					"What's switched on costs ghostly qi a second");
			helper.assertTrue(Spells.fallIsSafe(player), "On the yin wind a fall does no harm");
			PlayerCultivation mortal = new PlayerCultivation();
			mortal.netherworld().setMethod(GhostMethod.NETHERWORLD_BREATHING, 5, 0);
			helper.assertTrue(!Spell.GHOSTLY_WAIL.isKnown(mortal) && !Ability.GHOST_EYE.isUnlocked(mortal), "A living mortal has no qi to shape them...");
			mortal.netherworld().setGhost(true);
			helper.assertTrue(!Spell.GHOSTLY_WAIL.isKnown(mortal) && !Ability.GHOST_EYE.isUnlocked(mortal), "...nor a mortal's ghost: its meridians were never opened");
			mortal.awaken(0);
			helper.assertTrue(Spell.GHOSTLY_WAIL.isKnown(mortal) && Ability.GHOST_EYE.isUnlocked(mortal), "Opened (the Marrow Cleansing Elixir): its methods wake");
		} finally {
			cleanUp(player);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spellsCostGhostlyQiAndCoolDown(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.NASCENT_SOUL);
		NetherworldState s = CultivationManager.get(player).netherworld();
		try {
			s.setMethod(GhostMethod.GHOST_FIRE_SCRIPTURE, 4, 0);
			s.setGhostQi(20);
			Spells.cast(player, Spell.GHOST_FIRE_BOLT);
			helper.assertTrue(Math.abs(s.ghostQi() - (20 - Spell.GHOST_FIRE_BOLT.cost())) < 1.0e-9, "The bolt costs its ghostly qi");
			helper.assertTrue(Spells.cooldownLeft(player, Spell.GHOST_FIRE_BOLT) == Spell.GHOST_FIRE_BOLT.cooldownTicks(), "...and cools down");
			// Heeded only with the gamerule on (off by default since 2026-10-24): on for this one cast.
			boolean rule = helper.getLevel().getGameRules().getBoolean(ModGameRules.SPELL_COOLDOWNS);
			helper.getLevel().getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(true, helper.getLevel().getServer());
			Spells.cast(player, Spell.GHOST_FIRE_BOLT);
			helper.getLevel().getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(rule, helper.getLevel().getServer());
			helper.assertTrue(Math.abs(s.ghostQi() - (20 - Spell.GHOST_FIRE_BOLT.cost())) < 1.0e-9, "Not again while it cools");
			s.setGhostQi(1);
			Spells.cast(player, Spell.GHOST_FIRE);
			helper.assertTrue(s.ghostQi() == 1 && Spells.cooldownLeft(player, Spell.GHOST_FIRE) == 0, "Not without the ghostly qi for it");
			Spells.cast(player, Spell.SOUL_DASH);
			helper.assertTrue(s.ghostQi() == 1, "Nor a spell not yet reached");
			Spells.ready(player, Spell.GHOST_FIRE);
			s.setGhostQi(30);
			Spells.cast(player, null);
			helper.assertTrue(s.readiedSpell() == Spell.GHOST_FIRE && Math.abs(s.ghostQi() - (30 - Spell.GHOST_FIRE.cost())) < 1.0e-9,
					"The cast key casts the readied spell");
			// Ghost Fire on the hands: three blows set what they strike burning, each costing ghostly qi.
			for (int i = 0; i < Spells.GHOST_FIRE_BLOWS + 1; i++) {
				Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
				Spells.onMeleeHit(player, zombie);
				boolean burning = zombie.hasEffect(ModEffects.GHOST_FIRE);
				helper.assertTrue(burning == i < Spells.GHOST_FIRE_BLOWS, "Blow " + (i + 1) + (burning ? " burns" : " doesn't"));
				zombie.discard();
			}
			helper.assertTrue(Math.abs(s.ghostQi() - (30 - Spell.GHOST_FIRE.cost() - Spells.GHOST_FIRE_BLOWS * Spells.GHOST_FIRE_PER_HIT)) < 1.0e-9,
					"Each burning blow cost its ghostly qi");
		} finally {
			cleanUp(player);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDashCarriesAndTheWailDrivesBack(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.NASCENT_SOUL);
		NetherworldState s = CultivationManager.get(player).netherworld();
		Zombie zombie;
		try {
			s.setMethod(GhostMethod.WANDERING_SOUL_STEPS, 2, 0);
			s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 5, 0);
			s.setGhostQi(100);
			player.setYRot(0);
			player.setXRot(0);
			Spells.cast(player, Spell.SOUL_DASH);
			helper.assertTrue(player.getDeltaMovement().horizontalDistance() > 1.0 && Spells.fallIsSafe(player), "The dash throws them forward, safe from the fall");
			BlockPos near = new BlockPos(3, 2, 3);
			zombie = helper.spawn(EntityType.ZOMBIE, near);
			player.teleportTo(zombie.getX() + 2, zombie.getY(), zombie.getZ());
			Spells.cast(player, Spell.GHOSTLY_WAIL);
		} catch (RuntimeException e) {
			cleanUp(player);
			throw e;
		}
		// Its cry breaks out once its ghost has risen over its caster.
		helper.runAfterDelay(Spells.WAIL_RISES + 2, () -> {
			try {
				helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && zombie.hasEffect(MobEffects.WEAKNESS), "The wail slows and weakens what's near");
			} finally {
				cleanUp(player);
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSteleStandsThreeHighAndTheDeadMayUseIt(GameTestHelper helper) {
		BlockPos foot = helper.absolutePos(new BlockPos(1, 1, 1));
		MethodSteleBlock.place(helper.getLevel(), foot, GhostMethod.WANDERING_SOUL_STEPS, Direction.WEST);
		var lower = helper.getLevel().getBlockState(foot);
		var middle = helper.getLevel().getBlockState(foot.above());
		var upper = helper.getLevel().getBlockState(foot.above(2));
		helper.assertTrue(lower.getValue(MethodSteleBlock.PART) == MethodSteleBlock.Part.LOWER && middle.getValue(MethodSteleBlock.PART) == MethodSteleBlock.Part.MIDDLE
				&& upper.getValue(MethodSteleBlock.PART) == MethodSteleBlock.Part.UPPER, "Three stones, foot to crown");
		helper.assertTrue(middle.getValue(MethodSteleBlock.METHOD) == GhostMethod.WANDERING_SOUL_STEPS && middle.getValue(MethodSteleBlock.FACING) == Direction.WEST,
				"Carved with its method, facing its way");
		helper.assertTrue(middle.is(ModTags.GHOST_USABLE) && middle.getDestroySpeed(helper.getLevel(), foot) < 0,
				"The dead may read it; nobody breaks it");
		helper.succeed();
	}
}
