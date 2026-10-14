package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The two paths together (see TwoPaths): the body from the higher, the harmony of the two, the spells of both paths (what they
 * need, what they cost, what they do), and the true form with Form Shift.
 */
public class TwoPathsGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	/** Sets the ghost path at {@code realm}'s {@code stage}: that realm's foundation to the layer that carries it there. */
	private static void ghostPath(ServerPlayer player, Realm realm, Stage stage) {
		NetherworldState s = CultivationManager.get(player).netherworld();
		int layer = switch (stage) {
			case EARLY -> 1;
			case MID -> 3;
			case LATE -> 6;
			case GRAND_PERFECTION -> 9;
		};
		// The foundations before the realm's own perfected, its own at that stage of its own realm (MethodLayers).
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, method.realm().ordinal() < realm.ordinal() ? method.layers()
					: method.realm() == realm ? MethodLayers.PER_REALM * realm.ordinal() + layer : 0, 0);
		}
		CultivationManager.refresh(player);
	}

	private static void heaven(ServerPlayer player, Realm realm, Stage stage) {
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-3;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBodyFollowsTheHigherPath(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		double base = player.getAttributeBaseValue(Attributes.MAX_HEALTH);
		// The heaven path alone: its own realm's body.
		TwoPaths.Body body = TwoPaths.body(c, true);
		helper.assertTrue(!body.ghostPath() && body.realm() == Realm.CORE_FORMATION && near(body.scale(), c.favourFactor()), "One path: the heaven path's body");
		helper.assertTrue(near(player.getMaxHealth(), base + CultivationStats.maxHealth(Realm.CORE_FORMATION, Stage.EARLY) * body.scale()), "Its health");
		// The ghost path above it: the ghost path's body, and the harmony of two paths a realm apart.
		ghostPath(player, Realm.NASCENT_SOUL, Stage.MID);
		body = TwoPaths.body(c, true);
		helper.assertTrue(body.ghostPath() && body.realm() == Realm.NASCENT_SOUL && body.stage() == Stage.MID, "The ghost path above: its body (" + body + ")");
		helper.assertTrue(near(TwoPaths.harmony(c), TwoPaths.HARMONY[1]) && near(body.scale(), c.favourFactor() * (1 + TwoPaths.HARMONY[1])), "A realm apart: +20%");
		helper.assertTrue(near(player.getMaxHealth(), base + CultivationStats.maxHealth(Realm.NASCENT_SOUL, Stage.MID) * body.scale()),
				"The body's health from the ghost path, harmony on it (" + player.getMaxHealth() + ")");
		// The heaven path back above it.
		heaven(player, Realm.HEAVENLY_BEING, Stage.EARLY);
		body = TwoPaths.body(c, true);
		helper.assertTrue(!body.ghostPath() && body.realm() == Realm.HEAVENLY_BEING, "The heaven path above again: its body");
		// A mortal's ghost path counts for nothing, alive or dead: its meridians are closed.
		c.setMortal(true);
		helper.assertTrue(TwoPaths.ghostRank(c, false) == -1 && !TwoPaths.body(c, false).ghostPath(), "A living mortal: no ghost path");
		c.netherworld().setGhost(true);
		helper.assertTrue(TwoPaths.ghostRank(c, false) == -1 && !TwoPaths.body(c, false).ghostPath(), "Nor a mortal's ghost: its methods lie dormant");
		helper.assertTrue(TwoPaths.harmony(c) == 0, "No path, no harmony");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theHarmonyGrowsTheCloserThePathsStand(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.FOUR_AXIS, Stage.LATE);
		PlayerCultivation c = CultivationManager.get(player);
		helper.assertTrue(TwoPaths.harmony(c) == 0 && !TwoPaths.walksBoth(c), "No ghost path: no harmony");
		Realm[] ghost = {Realm.FOUR_AXIS, Realm.HEAVENLY_BEING, Realm.NASCENT_SOUL, Realm.CORE_FORMATION, Realm.QI_REFINING};
		double[] wanted = {0.30, 0.20, 0.10, 0, 0};
		for (int i = 0; i < ghost.length; i++) {
			ghostPath(player, ghost[i], Stage.EARLY);
			helper.assertTrue(near(TwoPaths.harmony(c), wanted[i]), "Ghost path at " + ghost[i].getId() + ": harmony " + wanted[i] + " (" + TwoPaths.harmony(c) + ")");
		}
		// The stage within the realm doesn't matter, nor which path is the higher.
		heaven(player, Realm.QI_REFINING, Stage.GRAND_PERFECTION);
		ghostPath(player, Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(near(TwoPaths.harmony(c), 0.30), "Both in Qi Refining: the fullest harmony");
		ghostPath(player, Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		helper.assertTrue(near(TwoPaths.harmony(c), 0.20), "The ghost path a realm above: +20% too");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSpellsOfBothPathsNeedBoth(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.NASCENT_SOUL, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		for (Spell spell : Spell.values()) if (spell.isDual()) helper.assertTrue(!spell.isKnown(c), spell.getId() + ": not on the heaven path alone");
		TwoPaths.onJoin(player);
		ghostPath(player, Realm.CORE_FORMATION, Stage.EARLY);
		helper.assertTrue(Spell.YIN_YANG_PALM.isKnown(c) && Spell.TAIJI_SHIELD.isKnown(c) && !Spell.LIFE_DEATH_SEAL.isKnown(c),
				"Both paths at Core Formation: the palm and the shield, not the seal");
		TwoPaths.watch(player);
		boolean onWheel = false;
		for (int i = 0; i < Spells.WHEEL_SLOTS; i++) onWheel |= s.spellSlot(i) == Spell.TAIJI_SHIELD;
		helper.assertTrue(onWheel, "Newly in reach, the shield goes on the wheel");
		ghostPath(player, Realm.FOUR_AXIS, Stage.EARLY);
		helper.assertTrue(Spell.LIFE_DEATH_SEAL.isKnown(c) && !Spell.HEAVEN_HELL_PILLAR.isKnown(c), "The ghost path far above: only as far as the heaven path, Nascent Soul");
		heaven(player, Realm.FOUR_AXIS, Stage.EARLY);
		for (Spell spell : Spell.values()) if (spell.isDual()) helper.assertTrue(spell.isKnown(c), spell.getId() + ": both paths at Four Axis");
		c.setMortal(true);
		s.setGhost(true);
		for (Spell spell : Spell.values()) if (spell.isDual()) helper.assertTrue(!spell.isKnown(c), spell.getId() + ": a mortal's ghost walks one path");
		TwoPaths.forget(player.getUUID());
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSpellOfBothPathsDrawsOnBothPools(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		try {
			ghostPath(player, Realm.FOUNDATION_BUILDING, Stage.EARLY);
			s.setGhostQi(100);
			c.setQi(0);
			Spells.cast(player, Spell.YIN_YANG_PALM);
			helper.assertTrue(near(s.ghostQi(), 100) && Spells.cooldownLeft(player, Spell.YIN_YANG_PALM) == 0, "No qi: not cast, the ghost qi kept");
			c.setQi(100);
			Spells.cast(player, Spell.YIN_YANG_PALM);
			helper.assertTrue(near(s.ghostQi(), 100 - Spell.YIN_YANG_PALM.cost()) && near(c.getQi(), 100 - Spell.YIN_YANG_PALM.qiCost()),
					"Cast: both pools drawn on (" + s.ghostQi() + ", " + c.getQi() + ")");
			helper.assertTrue(Spells.cooldownLeft(player, Spell.YIN_YANG_PALM) > 0, "And it cools");
		} finally {
			Spells.forget(player.getUUID());
		}
		helper.succeed();
	}

	/**
	 * Far from the test grid and from the deeper foundations' spells' own ground (MethodTiersGameTests): these reach 16 blocks and
	 * more. The middle of a chunk, and everything kept within it (-8 to 7 either way): a creature put in a chunk no one is near
	 * isn't found there.
	 */
	private static final BlockPos SPELL_GROUND = new BlockPos(-999_992, 150, 1_000_008);

	private static Zombie zombie(ServerLevel level, int dx, int dz) {
		Zombie zombie = EntityType.ZOMBIE.create(level);
		zombie.moveTo(SPELL_GROUND.getX() + dx + 0.5, SPELL_GROUND.getY(), SPELL_GROUND.getZ() + dz + 0.5, 180, 0);
		zombie.setNoAi(true);
		level.addFreshEntity(zombie);
		return zombie;
	}

	/** Stands {@code player} on the spells' ground facing {@code yaw}, head and all (a spell aims where the head looks). */
	private static void standAt(ServerPlayer player, int dx, int dz, float yaw) {
		player.teleportTo(player.serverLevel(), SPELL_GROUND.getX() + dx + 0.5, SPELL_GROUND.getY(), SPELL_GROUND.getZ() + dz + 0.5, yaw, 0);
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(0);
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void theSpellsOfBothPathsStrike(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int dx = -8; dx <= 7; dx++) {
			for (int dz = -8; dz <= 7; dz++) level.setBlockAndUpdate(SPELL_GROUND.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
		}
		ServerPlayer player = cultivator(helper, Realm.FOUR_AXIS, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		ghostPath(player, Realm.FOUR_AXIS, Stage.EARLY);
		c.netherworld().setGhostQi(10_000);
		List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
		standAt(player, 0, 0, 0); // facing south, +z
		// Yin-Yang Palm: the first ahead struck, and the harm mends the caster.
		Zombie palmed = zombie(level, 0, 4);
		made.add(palmed);
		player.setHealth(player.getMaxHealth() / 2);
		float hurt = player.getHealth();
		c.setQi(100_000);
		Spells.cast(player, Spell.YIN_YANG_PALM);
		helper.assertTrue(palmed.getHealth() < palmed.getMaxHealth() && player.getHealth() > hurt, "The palm strikes, and mends (" + player.getHealth() + ")");
		// Taiji Shield: an arrow flying at the caster turned back on its way.
		Spells.cast(player, Spell.TAIJI_SHIELD);
		helper.assertTrue(Spells.shielded(player) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "The shield turns round it");
		Arrow arrow = new Arrow(level, SPELL_GROUND.getX() + 0.5, SPELL_GROUND.getY() + 1, SPELL_GROUND.getZ() + 3.0);
		arrow.setDeltaMovement(new Vec3(0, 0, -1.0));
		arrow.setOwner(palmed);
		level.addFreshEntity(arrow);
		made.add(arrow);
		// Life and Death Seal on a fresh one.
		Zombie sealed = zombie(level, 2, 6);
		made.add(sealed);
		standAt(player, 0, 0, (float) Math.toDegrees(Math.atan2(-2, 6))); // a little east of south, at it
		c.setQi(100_000);
		Spells.cast(player, Spell.LIFE_DEATH_SEAL);
		helper.assertTrue(Spells.sealed(sealed), "Sealed (look " + player.getViewVector(1.0f) + " from " + player.getEyePosition() + " at " + sealed.position()
				+ ", qi " + c.getQi() + ", ghost qi " + c.netherworld().ghostQi() + ", cooling " + Spells.cooldownLeft(player, Spell.LIFE_DEATH_SEAL) + ")");
		helper.runAfterDelay(30, () -> {
			try {
				helper.assertTrue(arrow.getOwner() == player && arrow.getDeltaMovement().z > 0, "The arrow is turned back, the caster's own now (" + arrow.getDeltaMovement() + ")");
				helper.assertTrue(!sealed.isAlive() || sealed.getHealth() < sealed.getMaxHealth(), "The seal draws its life off (" + sealed.getHealth() + ")");
				// Heaven and Hell Pillar where it looks: a fresh one struck, thrown up and burning (the palmed one out of the way).
				palmed.discard();
				standAt(player, 0, 0, 0);
				Zombie pillared = zombie(level, 0, 7);
				made.add(pillared);
				c.setQi(100_000);
				Spells.cast(player, Spell.HEAVEN_HELL_PILLAR);
				helper.assertTrue(!pillared.isAlive() || pillared.getHealth() < pillared.getMaxHealth() && pillared.hasEffect(ModEffects.GHOST_FIRE),
						"The pillar strikes and burns");
				// Primordial Chaos: all round dragged in and unmade; the caster made strong.
				Zombie a = zombie(level, 6, -4), b = zombie(level, -7, 2);
				made.add(a);
				made.add(b);
				c.setQi(100_000);
				Spells.cast(player, Spell.PRIMORDIAL_CHAOS);
				helper.assertTrue(!a.isAlive() && !b.isAlive(), "Unmade by Primordial Chaos");
				helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST), "Made strong by it");
				helper.succeed();
			} finally {
				Spells.forget(player.getUUID());
				for (net.minecraft.world.entity.Entity entity : made) entity.discard();
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theTrueFormIsTheHigherPaths(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.FOUR_AXIS, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		ghostPath(player, Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		helper.assertTrue(!TwoPaths.formsOpen(c) && !Ability.FORM_SHIFT.isUnlocked(c) && TwoPaths.look(c) == TwoPaths.Look.HUMAN,
				"Below a ghost path of Core Formation, one form: the living look human");
		// Four Axis on the heaven path, Nascent Soul on the ghost path: human, by default.
		ghostPath(player, Realm.NASCENT_SOUL, Stage.EARLY);
		helper.assertTrue(TwoPaths.formsOpen(c) && Ability.FORM_SHIFT.isUnlocked(c), "A ghost path of Core Formation and more: two forms");
		helper.assertTrue(TwoPaths.trueForm(c) == TwoPaths.Form.HUMAN && TwoPaths.look(c) == TwoPaths.Look.HUMAN, "The heaven path higher: human");
		c.setAbilityEnabled(Ability.FORM_SHIFT, true);
		helper.assertTrue(TwoPaths.shownForm(c) == TwoPaths.Form.GHOST_KING && TwoPaths.look(c) == TwoPaths.Look.KING, "Form Shift: a Ghost King's form, alive");
		c.setAbilityEnabled(Ability.FORM_SHIFT, false);
		// The ghost path higher: a Ghost King's.
		heaven(player, Realm.CORE_FORMATION, Stage.LATE);
		helper.assertTrue(TwoPaths.trueForm(c) == TwoPaths.Form.GHOST_KING && TwoPaths.look(c) == TwoPaths.Look.KING, "The ghost path higher: the Ghost King's");
		// The paths level: the human (it lives).
		heaven(player, Realm.NASCENT_SOUL, Stage.EARLY);
		helper.assertTrue(TwoPaths.trueForm(c) == TwoPaths.Form.HUMAN && TwoPaths.look(c) == TwoPaths.Look.HUMAN, "Level, alive: human");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadAreTheDeadWhateverTheirPaths(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.FOUR_AXIS, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		ghostPath(player, Realm.NASCENT_SOUL, Stage.EARLY);
		c.setAbilityEnabled(Ability.FORM_SHIFT, true);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING, "Alive, Form Shift on: the Ghost King's form");
		s.setGhost(true);
		// Dead: no forms, no true form, no Form Shift; Four Axis on the heaven path and Nascent Soul on the ghost: a Ghost King.
		helper.assertTrue(!TwoPaths.formsOpen(c) && !Ability.FORM_SHIFT.isUnlocked(c) && TwoPaths.hasKingForm(c), "The dead have no forms to choose");
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING && GhostSystem.isKing(c) && !TwoPaths.inGhostForm(c),
				"Dead, the heaven path higher: a Ghost King, drawn as one, never in a human form");
		c.setAbilityEnabled(Ability.FORM_SHIFT, false);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING, "Form Shift's switch is nothing to the dead");
		// Either path at Core Formation makes a king of the dead; below it on both, a common ghost.
		heaven(player, Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING, "Dead at Qi Refining with a Nascent Soul ghost path: a Ghost King");
		ghostPath(player, Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.DEAD, "Below Core Formation on both paths: a common ghost");
		heaven(player, Realm.CORE_FORMATION, Stage.EARLY);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING, "Dead at Core Formation, the ghost path below it: a Ghost King");
		// Alive again: the forms are its own again.
		ghostPath(player, Realm.NASCENT_SOUL, Stage.EARLY);
		s.setGhost(false);
		helper.assertTrue(TwoPaths.formsOpen(c) && TwoPaths.trueForm(c) == TwoPaths.Form.GHOST_KING, "Born again: its true form the ghost path's");
		helper.succeed();
	}

	/** Who the entity tracker sends {@code subject} to right now: the connections in its tracked entity's seenBy (ChunkMap, by reflection). */
	@SuppressWarnings("unchecked")
	private static java.util.Set<Object> seenBy(ServerLevel level, ServerPlayer subject) throws ReflectiveOperationException {
		net.minecraft.server.level.ChunkMap map = level.getChunkSource().chunkMap;
		java.lang.reflect.Field entityMap = net.minecraft.server.level.ChunkMap.class.getDeclaredField("entityMap");
		entityMap.setAccessible(true);
		Object tracked = ((it.unimi.dsi.fastutil.ints.Int2ObjectMap<Object>) entityMap.get(map)).get(subject.getId());
		java.lang.reflect.Field seenBy = tracked.getClass().getDeclaredField("seenBy");
		seenBy.setAccessible(true);
		return (java.util.Set<Object>) seenBy.get(tracked);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostKingsFormIsSeenOnlyBySeers(GameTestHelper helper) throws ReflectiveOperationException {
		ServerLevel level = helper.getLevel();
		ServerPlayer subject = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer low = cultivator(helper, Realm.QI_REFINING, Stage.MID);
		ServerPlayer nascent = cultivator(helper, Realm.NASCENT_SOUL, Stage.EARLY);
		ServerPlayer seer = cultivator(helper, Realm.HEAVENLY_BEING, Stage.EARLY);
		ServerPlayer dead = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		ServerPlayer[] everyone = {subject, low, nascent, seer, dead};
		try {
			BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
			for (int i = 0; i < everyone.length; i++) everyone[i].teleportTo(level, at.getX() + 0.5 + i, at.getY(), at.getZ() + 0.5, 0, 0);
			GhostSystem.makeGhost(dead);
			CultivationManager.get(nascent).setAbilityEnabled(Ability.QI_SENSE, false);
			// The ghost path above the heaven path: the true form the Ghost King's, and so seen as the dead are.
			ghostPath(subject, Realm.NASCENT_SOUL, Stage.EARLY);
			helper.assertTrue(TwoPaths.inGhostForm(subject) && SoulProjection.seenOnlyBySeers(subject), "In a Ghost King's form, seen only by seers");
			helper.assertTrue(!SoulProjection.broadcastsTo(subject, low), "Hidden from a Qi Refining cultivator");
			helper.assertTrue(!SoulProjection.broadcastsTo(subject, nascent), "...and from a Nascent Soul with Qi Sense shut");
			CultivationManager.get(nascent).setAbilityEnabled(Ability.QI_SENSE, true);
			helper.assertTrue(SoulProjection.broadcastsTo(subject, nascent), "A Nascent Soul with Qi Sense open sees it");
			helper.assertTrue(SoulProjection.broadcastsTo(subject, seer) && SoulProjection.broadcastsTo(subject, dead) && SoulProjection.broadcastsTo(subject, subject),
					"A Heavenly Being sees it, the dead see it, it sees itself");
			// The tracker itself: after the form is taken, the low cultivator's client isn't sent it; put off, it is.
			for (ServerPlayer p : everyone) SoulProjection.refreshTracking(p);
			helper.assertTrue(!seenBy(level, subject).contains(low.connection) && seenBy(level, subject).contains(seer.connection),
					"The tracker sends the Ghost King's form to the seer and not the Qi Refining cultivator");
			CultivationManager.toggleAbility(subject, Ability.FORM_SHIFT.ordinal()); // the human form
			helper.assertTrue(!TwoPaths.inGhostForm(subject) && !SoulProjection.seenOnlyBySeers(subject) && SoulProjection.broadcastsTo(subject, low),
					"In its human form, seen by all");
			helper.assertTrue(seenBy(level, subject).contains(low.connection), "...the tracker sends it to the Qi Refining cultivator at once");
			CultivationManager.toggleAbility(subject, Ability.FORM_SHIFT.ordinal()); // the Ghost King's again
			helper.assertTrue(!seenBy(level, subject).contains(low.connection), "...and takes it away again with the Ghost King's form");
		} finally {
			GhostSystem.forget(dead.getUUID());
			for (ServerPlayer p : everyone) TwoPaths.forget(p.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMortalHasNoGhostPathUntilItsMeridiansOpen(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel(); // mortal, as every new cultivator is
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		try {
			helper.assertTrue(c.isMortal(), "A mortal");
			helper.assertTrue(!GhostMethods.canLearn(c, GhostMethod.NETHERWORLD_BREATHING) && !GhostMethods.learn(player, GhostMethod.NETHERWORLD_BREATHING)
					&& !s.knows(GhostMethod.NETHERWORLD_BREATHING), "Alive, it can't take up even the Breathing Art");
			helper.assertTrue(GhostMethods.refusal(c, GhostMethod.NETHERWORLD_BREATHING) != null, "...and is told why");
			GhostSystem.makeGhost(player);
			helper.assertTrue(!GhostMethods.learn(player, GhostMethod.NETHERWORLD_BREATHING) && !s.knows(GhostMethod.NETHERWORLD_BREATHING),
					"Dead, no more: a mortal's ghost has no ghost path");
			MeditationManager.start(player);
			helper.assertTrue(!MeditationManager.isMeditating(player.getUUID()), "It can't sit down to cultivate one either");
			// A method it knew (an older world, an op's gift) lies dormant: no ghost path, no spells, no abilities, no king.
			s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 9, 0);
			s.setMethod(GhostMethod.YELLOW_SPRINGS_MARROW, 18, 0);
			s.setMethod(GhostMethod.GHOST_CORE_CANON, 27, 0);
			helper.assertTrue(TwoPaths.ghostRank(c, false) == -1 && !Spell.GHOSTLY_WAIL.isKnown(c) && !Ability.GHOST_EYE.isUnlocked(c) && !GhostSystem.isKing(c),
					"Its methods dormant");
			helper.assertTrue(GhostQi.poolMax(c) == GhostQi.BASE_POOL && GhostMethods.foundationLayers(c) == 0,
					"Its layers add nothing: the bare pool of the dead, no hearts from them");
			helper.assertTrue(GhostMethods.refusal(c, GhostMethod.NETHERWORLD_BREATHING) != null, "Not cultivated further either");
			// The meridians opened (a Marrow Cleansing Elixir, drunk in life): the ghost path wakes as the heaven path begins.
			c.awaken(0);
			helper.assertTrue(TwoPaths.ghostRank(c, false) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION)
					&& Spell.GHOSTLY_WAIL.isKnown(c) && GhostSystem.isKing(c) && GhostMethods.refusal(c, GhostMethod.NETHERWORLD_BREATHING) == null,
					"Opened: its ghost path, spells and might are its own");
			helper.assertTrue(GhostQi.poolMax(c) > GhostQi.BASE_POOL && GhostMethods.foundationLayers(c) == 27, "...and its pool and hearts");
			helper.assertTrue(GhostMethods.learn(player, GhostMethod.GHOST_FIRE_SCRIPTURE), "And it takes up methods again");
		} finally {
			MeditationManager.forget(player.getUUID());
			GhostSystem.forget(player.getUUID());
			Spells.forget(player.getUUID());
		}
		helper.succeed();
	}
}
