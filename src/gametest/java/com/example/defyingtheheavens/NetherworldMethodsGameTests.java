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

	/** No layers but the Laws' (the user's ask, 2026-11-16): a ghost foundation or art known is understood as far as the ghost path, at once. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostArtIsUnderstoodAsFarAsThePathAtOnce(GameTestHelper helper) {
		NetherworldState s = new NetherworldState();
		helper.assertTrue(s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 0, "Not known: nothing");
		s.setMethod(GhostMethod.GHOST_FIRE_SCRIPTURE, 1, 0);
		helper.assertTrue(s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 3 && s.progress(GhostMethod.GHOST_FIRE_SCRIPTURE) == 0,
				"No ghost path begun: as at its Qi Refining, the fire's three");
		s.setPath(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0);
		helper.assertTrue(s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 9, "The path at Core Formation: all nine at once");
		s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 9, 500);
		s.setPath(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 0);
		helper.assertTrue(s.layer(GhostMethod.NETHERWORLD_BREATHING) == 5 && s.progress(GhostMethod.NETHERWORLD_BREATHING) == 0
				&& s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 3 && !GhostMethods.settling(s, GhostMethod.NETHERWORLD_BREATHING),
				"Whatever is set, the path decides: the Breathing Art as far as Mid, the fire's three, nothing settling");
		helper.assertTrue(Spell.GHOSTLY_WAIL.isKnown(withState(s)), "Mid's art open at once (the Ghostly Wail)");
		s.setPath(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 0);
		helper.assertTrue(!Spell.GHOSTLY_WAIL.isKnown(withState(s)) && Spell.GHOST_FIRE.isKnown(withState(s)), "...not at Early; the fire's first, any time");
		helper.succeed();
	}

	/** A cultivator of the living (Qi Refining Early, meridians open) whose ghost path is {@code s}'s. */
	private static PlayerCultivation withState(NetherworldState s) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		c.netherworld().load(s.save());
		return c;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachCultivatorGoesAtTheirOwnPace(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		NetherworldState s = c.netherworld();
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY) - 0.7) < 1.0e-9 && !GhostMethods.canGain(c),
				"No ghost path yet: its flow as its Qi Refining's, -30%, and with no foundation the bar gains none of it");
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
		helper.assertTrue(loaded.knows(GhostMethod.NETHERWORLD_BREATHING) && loaded.layer(GhostMethod.NETHERWORLD_BREATHING) == 5
				&& loaded.layer(GhostMethod.WANDERING_SOUL_STEPS) == 3 && !loaded.knows(GhostMethod.GHOST_FIRE_SCRIPTURE),
				"Each method known, understood as far as the path (Qi Refining Mid: the Breathing Art's fifth, the steps' three)");
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
		helper.assertTrue(migrated.savedLayer(GhostMethod.NETHERWORLD_BREATHING) == 9 && migrated.savedLayer(GhostMethod.YELLOW_SPRINGS_MARROW) == 18
				&& migrated.savedLayer(GhostMethod.GHOST_CORE_CANON) == 21 && migrated.savedLayer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 4
				&& GhostMethods.ghostRank(migrated) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID)
				&& migrated.layer(GhostMethod.GHOST_CORE_CANON) == 23 && migrated.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 9,
				"An old save's Ghost Core Canon at its third layer was its 21st: Core Formation Mid as before (understood, since 2026-11-16, as far as it)");
		// ...and saved before the ghost path had a bar of its own (2026-10-19): the bar starts where its deepest foundation stood, its
		// progress through the layer the bar's; the foundations' settling starts afresh, the fire's kept.
		// (As far through the bar's Core Formation Mid as 42 seconds were through that stage's old layers, 21 to 23.)
		double oldMid = MethodLayers.cost(21) + MethodLayers.cost(22) + MethodLayers.cost(23);
		double expected = 42 / oldMid * GhostMethods.required(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID));
		helper.assertTrue(Math.abs(migrated.pathCultivation() - expected) < 1.0e-6,
				"The Canon's 42 seconds into its 21st layer: as far into the bar's Core Formation Mid (" + migrated.pathCultivation() + ")");
		NetherworldState again = new NetherworldState();
		again.load(migrated.save());
		helper.assertTrue(again.savedLayer(GhostMethod.GHOST_CORE_CANON) == 21 && again.savedLayer(GhostMethod.YELLOW_SPRINGS_MARROW) == 18, "...and moved only once");
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
			// Since 2026-11-16 (no layers but the Laws') the ghost path opens them, nothing settling.
			GhostMethods.learn(player, GhostMethod.NETHERWORLD_BREATHING);
			helper.assertTrue(s.activeMethod() == GhostMethod.GHOST_FIRE_SCRIPTURE && GhostMethods.ghostRank(s) == 0,
					"A second method learned doesn't take over the practice; a foundation begins the ghost path");
			helper.assertTrue(!Ability.GHOST_EYE.isUnlocked(c), "No Ghost Eye at the ghost path's Qi Refining Early...");
			GhostMethods.setPath(s, PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE));
			helper.assertTrue(Ability.GHOST_EYE.isUnlocked(c) && Ability.NETHERWORLD_VEIL.isUnlocked(c), "...from its Mid; the Veil at its Late");
			GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
			GhostMethods.afterLayer(player, s);
			helper.assertTrue(Spell.GHOST_FIRE_BOLT.isKnown(c) && s.spellSlot(1) == Spell.GHOST_FIRE_BOLT, "The ghost path at Foundation Building: the bolt, next on the wheel");
			s.setMethod(GhostMethod.WANDERING_SOUL_STEPS, 1, 0);
			helper.assertTrue(Ability.YIN_WIND_STEPS.isUnlocked(c) && !Ability.GHOST_FLIGHT.isUnlocked(c), "Yin Wind Steps alive");
			s.setGhost(true);
			// Ghost Flight is retired (2026-10-18): the dead fly freely without it (TwoPaths#fliesFreely; BothPathsGameTests).
			helper.assertTrue(!Ability.GHOST_FLIGHT.isUnlocked(c) && TwoPaths.fliesFreely(c), "...and the dead fly freely, no Ghost Flight needed");
			s.setGhost(false);
			helper.assertTrue(GhostMethods.layersKnown(c) == 9 + 6 + 6 && GhostMethods.foundationLayers(c) == 9
					&& Math.abs(GhostQi.poolMax(c) - (GhostQi.poolMax(true, 21, 9) + GhostQi.realmPool(c)) * StatPoints.poolFactor(c) * (1 + TwoPaths.harmony(c))) < 1.0e-9,
					"The pool grows with every layer understood (the Breathing Art's nine, the fire's and the steps' six), the Breathing Art's twice over");
			c.setAbilityEnabled(Ability.GHOST_EYE, true);
			c.setAbilityEnabled(Ability.YIN_WIND_STEPS, true);
			helper.assertTrue(Math.abs(GhostMethods.upkeep(c) - (GhostMethods.GHOST_EYE_COST + GhostMethods.YIN_WIND_COST)) < 1.0e-9,
					"What's switched on costs ghostly qi a second");
			helper.assertTrue(Spells.fallIsSafe(player), "On the yin wind a fall does no harm");
			PlayerCultivation mortal = new PlayerCultivation();
			mortal.netherworld().setMethod(GhostMethod.NETHERWORLD_BREATHING, 5, 0);
			mortal.netherworld().setPath(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 0);
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
			s.setMethod(GhostMethod.GHOST_FIRE_SCRIPTURE, 1, 0);
			GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY)); // the bolt's (understood as far as the path)
			s.setGhostQi(20);
			Spells.cast(player, Spell.GHOST_FIRE_BOLT);
			helper.assertTrue(Math.abs(s.ghostQi() - (20 - Spell.GHOST_FIRE_BOLT.cost())) < 1.0e-9, "The bolt costs its ghostly qi");
			// As long as its caster's realm leaves it (a Nascent Soul stands far above the bolt: Laws#cooldown).
			helper.assertTrue(Spells.cooldownLeft(player, Spell.GHOST_FIRE_BOLT) == Laws.cooldown(player, Spell.GHOST_FIRE_BOLT)
					&& Laws.cooldown(player, Spell.GHOST_FIRE_BOLT) < Spell.GHOST_FIRE_BOLT.cooldownTicks(), "...and cools down, the sooner for its realm");
			// Heeded only with the gamerule on (its default again since 2026-11-10): on for this one cast, whatever the world says.
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
			GhostMethods.setPath(s, PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE)); // the wail's (understood as far as the path)
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
