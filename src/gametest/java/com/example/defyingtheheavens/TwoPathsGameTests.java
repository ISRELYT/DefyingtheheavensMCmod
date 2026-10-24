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

	/**
	 * Sets the ghost path at {@code realm}'s {@code stage} (its bar, GhostMethods#setPath): the foundations up to that realm's own
	 * known, understood as far as the path carries them.
	 */
	private static void ghostPath(ServerPlayer player, Realm realm, Stage stage) {
		ghostPath(CultivationManager.get(player), realm, stage);
		CultivationManager.refresh(player);
	}

	private static void heaven(ServerPlayer player, Realm realm, Stage stage) {
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
	}

	/** A tiger's demon core, its Demon Path at {@code realm}'s {@code stage} (DemonPath: a human has none, TwoPaths#demonRank). */
	private static void demonPath(ServerPlayer player, Realm realm, Stage stage) {
		PlayerCultivation c = CultivationManager.get(player);
		c.demon().setBloodline(Bloodline.TIGER);
		c.demon().set(PlayerCultivation.rank(realm, stage), 0);
		CultivationManager.refresh(player);
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-3;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void othersWeighEitherPath(GameTestHelper helper) {
		// The heaven path at Qi Refining, the ghost path at Heavenly Being: weighed as its body, the ghost path's (the 2026-10-20 sweep:
		// the heaven path's alone was weighed before, so Realm Suppress, cultivators, barriers and the domain took it for Qi Refining).
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		ghostPath(player, Realm.HEAVENLY_BEING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		int hb = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY), cf = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
		helper.assertTrue(TwoPaths.body(c, false).rank() == hb, "Its body is the ghost path's Heavenly Being");
		helper.assertTrue(TwoPaths.weighedRank(c) == hb && CultivatorNpc.rankOf(player) == hb && Formations.rankOf(player) == hb,
				"Cultivators and barriers weigh it a Heavenly Being (" + CultivatorNpc.rankOf(player) + ")");
		helper.assertTrue(TwoPaths.wieldedRealm(c) == Realm.HEAVENLY_BEING
				&& near(ConsciousnessDomainHandler.radius(c), ConsciousnessDomainHandler.radius(Realm.HEAVENLY_BEING, Stage.EARLY)),
				"Sensed a Heavenly Being, its domain one's (" + ConsciousnessDomainHandler.radius(c) + ")");
		helper.assertTrue(RealmSuppressSystem.pressureOn(cf, TwoPaths.weighedRank(c)) == null && RealmSuppressSystem.pressureOn(hb, cf) != null,
				"A Core Formation cultivator's Realm Suppress can't press it; its own presses one");
		// Pressure on it doesn't lower how it is weighed (no chains of pressed pressers), but what it wields, and is sensed at.
		c.setPressure(Stage.values().length, 0.25);
		helper.assertTrue(TwoPaths.weighedRank(c) == hb && TwoPaths.wieldedRank(c) == hb - Stage.values().length, "Pressed: weighed as before, sensed lower");
		c.setPressure(0, 0);
		helper.assertTrue(TwoPaths.weighedRank(new PlayerCultivation()) == -1, "A mortal weighs as one");
		helper.succeed();
	}

	/** {@link #ghostPath} for a bare cultivation, no player: the foundations to the realm's own known, deeper ones not. */
	private static void ghostPath(PlayerCultivation c, Realm realm, Stage stage) {
		NetherworldState s = c.netherworld();
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, method.realm().ordinal() <= realm.ordinal() ? 1 : 0, 0);
		}
		GhostMethods.setPath(s, PlayerCultivation.rank(realm, stage));
	}

	/** A name's translation key ("" for one that isn't translated as a whole, like a plain rank's), and its first argument's. */
	private static String key(net.minecraft.network.chat.Component name, int arg) {
		if (!(name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t)) return "";
		if (arg < 0) return t.getKey();
		return t.getArgs()[arg] instanceof net.minecraft.network.chat.Component a ? key(a, -1) : "";
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void aMeditationDrawsInOneQiOrTheOther(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		// Out of the other tests' fights (a Hungry Ghost goes for a survival player, and a blow ends a meditation: CultivationEvents).
		player.getAbilities().invulnerable = true;
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		helper.assertTrue(!TwoPaths.cultivatesGhost(c), "No ghost path: the heavens' qi");
		TwoPaths.switchPath(player);
		helper.assertTrue(!s.cultivatingGhost(), "...and no turning to a ghost path not begun");
		ghostPath(player, Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(TwoPaths.walksBoth(c) && !TwoPaths.cultivatesGhost(c), "On both paths, the heaven path's qi until switched");
		player.setOnGround(true);
		MeditationManager.start(player);
		helper.assertTrue(MeditationManager.isMeditating(player.getUUID()), "Sat down to meditate");
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(c.getCultivation() > 0 && s.pathCultivation() == 0,
					"Drawing in the heavens' qi, only the heaven path's bar fills (" + c.getCultivation() + ", " + s.pathCultivation() + ")");
			double heaven = c.getCultivation();
			TwoPaths.switchPath(player);
			helper.assertTrue(TwoPaths.cultivatesGhost(c) && MeditationManager.isMeditating(player.getUUID()), "Switched, the meditation goes on");
			helper.runAfterDelay(60, () -> {
				helper.assertTrue(c.getCultivation() == heaven && s.pathCultivation() > 0,
						"Drawing in ghostly qi, only the ghost path's (" + c.getCultivation() + ", " + s.pathCultivation() + ")");
				MeditationManager.forget(player.getUUID());
				TwoPaths.forget(player.getUUID());
				player.server.getPlayerList().remove(player);
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void effectiveCultivationNamesWhatTheBodyComesTo(GameTestHelper helper) {
		int stages = Stage.values().length;
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.FOUNDATION_BUILDING, Stage.MID, 0);
		TwoPaths.Effective e = TwoPaths.effective(c, false);
		helper.assertTrue(e.rank() == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID) && !e.quasi() && !TwoPaths.strongerThanRank(c),
				"One path, no Favour: its own rank");
		// Mid on both paths in one realm: x1.3 on Mid's 1.1 is 1.43 of Early's might, past Grand Perfection's 1.3, a tenth of the
		// way to Core Formation Early (might 5.72 between 5.2 and 10).
		ghostPath(c, Realm.FOUNDATION_BUILDING, Stage.MID);
		e = TwoPaths.effective(c, false);
		helper.assertTrue(e.rank() == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION) && e.quasi() && near(e.toward(), 0.52 / 4.8)
				&& TwoPaths.strongerThanRank(c), "Mid on both paths: past Grand Perfection (" + e + ")");
		helper.assertTrue(key(TwoPaths.effectiveName(e), -1).equals(ModLang.EFFECTIVE_QUASI_REALM)
				&& key(TwoPaths.effectiveName(e), 0).equals(Realm.CORE_FORMATION.getTranslationKey()), "...named Quasi-Core Formation");
		net.minecraft.network.chat.Component label = TwoPaths.effectiveLabel(e);
		helper.assertTrue(key(label, -1).equals(ModLang.EFFECTIVE_TOWARD) && label.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
				&& Long.valueOf(11).equals(t.getArgs()[1]), "...11% of the way there");
		// Early on both: x1.3 is Grand Perfection's might exactly.
		c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
		ghostPath(c, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		e = TwoPaths.effective(c, false);
		helper.assertTrue(e.rank() == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION) && !e.quasi() && TwoPaths.strongerThanRank(c)
				&& key(TwoPaths.effectiveName(e), -1).isEmpty() && key(TwoPaths.effectiveLabel(e), -1).isEmpty(),
				"Early on both: Grand Perfection itself, no Quasi-, no per cent (" + e + ")");
		// A stage apart (x1.2), counted across the realm's border: Core Formation Early beside Foundation Building Grand Perfection is
		// Core Formation Late. (The heaven path the higher: alive, the ghost path's would give half its strength, TwoPaths#pathShare.)
		ghostPath(c, Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		e = TwoPaths.effective(c, false);
		helper.assertTrue(e.rank() == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE) && !e.quasi(), "A stage apart, Early: Late (" + e + ")");
		// Two stages apart (x1.1): Nascent Soul Mid's 1.21 is just past Late's 1.2.
		ghostPath(c, Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
		c.setState(Realm.NASCENT_SOUL, Stage.MID, 0);
		e = TwoPaths.effective(c, false);
		helper.assertTrue(e.rank() == PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.LATE) && e.quasi()
				&& key(TwoPaths.effectiveName(e), -1).equals(ModLang.EFFECTIVE_QUASI_STAGE)
				&& key(TwoPaths.effectiveName(e), 0).equals(Realm.NASCENT_SOUL.getTranslationKey()), "Two stages apart, Mid: Nascent Soul - Quasi-Grand Perfection (" + e + ")");
		// Three apart: no harmony, its own rank (a realm apart is four).
		c.setState(Realm.NASCENT_SOUL, Stage.LATE, 0);
		helper.assertTrue(!TwoPaths.strongerThanRank(c), "Three stages apart: no harmony, its own rank");
		// The ghost path alone far above, alive: half its strength, worth less than its rank.
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		ghostPath(c, Realm.NASCENT_SOUL, Stage.EARLY);
		e = TwoPaths.effective(c, false);
		helper.assertTrue(e.rank() < PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY) && TwoPaths.strongerThanRank(c),
				"The ghost path alone, alive: below its own rank (" + e + ")");
		// Heaven's Favour on one path counts too: Early x1.05, past Early, short of Mid.
		PlayerCultivation favoured = new PlayerCultivation();
		favoured.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		favoured.setFavour(1);
		e = TwoPaths.effective(favoured, false);
		helper.assertTrue(e.rank() == PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY) && e.quasi() && near(e.toward(), 0.5), "Favour: Quasi-Mid, half way (" + e + ")");
		// The very last, with harmony: beyond True Immortal Grand Perfection (Four Axis's before the realms after it, 2026-10-22).
		PlayerCultivation top = new PlayerCultivation();
		top.setState(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, 0);
		ghostPath(top, Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		e = TwoPaths.effective(top, false);
		helper.assertTrue(e.quasi() && key(TwoPaths.effectiveName(e), -1).equals(ModLang.EFFECTIVE_BEYOND), "Beyond the very last (" + e + ")");
		// Harmony and Favour at their most never make a body a realm's: each realm's Grand Perfection x1.3 x1.15 stops short of the next.
		for (Realm realm : Realm.values()) {
			PlayerCultivation most = new PlayerCultivation();
			most.setState(realm, Stage.GRAND_PERFECTION, 0);
			ghostPath(most, realm, Stage.GRAND_PERFECTION);
			most.setFavour(3);
			e = TwoPaths.effective(most, false);
			helper.assertTrue(e.rank() / stages == realm.ordinal() && e.quasi(), realm.getId() + " at its most: still its own realm, Quasi- the next (" + e + ")");
		}
		// A mortal: no effective cultivation.
		helper.assertTrue(TwoPaths.effective(new PlayerCultivation(), false) == null, "A mortal: none");
		helper.succeed();
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
		// The ghost path above it: the ghost path's body (five stages apart, no harmony: TwoPaths#HARMONY). Alive, the ghost path gives
		// half its strength (TwoPaths#pathShare), half the whole of its health; still more than Core Formation's whole.
		ghostPath(player, Realm.NASCENT_SOUL, Stage.MID);
		body = TwoPaths.body(c, true);
		helper.assertTrue(body.ghostPath() && body.realm() == Realm.NASCENT_SOUL && body.stage() == Stage.MID && body.share() == TwoPaths.HALF_PATH,
				"The ghost path above: its body, at half (" + body + ")");
		helper.assertTrue(TwoPaths.harmony(c) == 0 && near(body.scale(), c.favourFactor()), "Five stages apart: no harmony");
		helper.assertTrue(near(player.getMaxHealth(), (base + CultivationStats.maxHealth(Realm.NASCENT_SOUL, Stage.MID) * body.scale()) * TwoPaths.HALF_PATH),
				"The body's health from the ghost path, half the whole (" + player.getMaxHealth() + ")");
		// A stage apart: the harmony on the body (the heaven path's now, whole alive, the mightier).
		heaven(player, Realm.NASCENT_SOUL, Stage.EARLY);
		body = TwoPaths.body(c, true);
		helper.assertTrue(near(TwoPaths.harmony(c), TwoPaths.HARMONY[1]) && near(body.scale(), c.favourFactor() * (1 + TwoPaths.HARMONY[1])), "A stage apart: +20%");
		helper.assertTrue(near(player.getMaxHealth(), (base + CultivationStats.maxHealth(body.realm(), body.stage()) * body.scale()) * body.share()),
				"...on the body's health (" + player.getMaxHealth() + ", " + body + ")");
		// Dead, the ghost path's whole.
		c.netherworld().setGhost(true);
		body = TwoPaths.body(c, true);
		helper.assertTrue(body.ghostPath() && body.share() == 1.0, "Dead, the ghost path's body whole (" + body + ")");
		c.netherworld().setGhost(false);
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
		// By stage (the user's call, 2026-10-24; by realm, within three, was too generous): the heaven path at Four Axis Late.
		Realm[] realm = {Realm.FOUR_AXIS, Realm.FOUR_AXIS, Realm.FOUR_AXIS, Realm.HEAVENLY_BEING, Realm.NASCENT_SOUL};
		Stage[] stage = {Stage.LATE, Stage.MID, Stage.EARLY, Stage.GRAND_PERFECTION, Stage.LATE};
		double[] wanted = {0.30, 0.20, 0.10, 0, 0};
		for (int i = 0; i < realm.length; i++) {
			ghostPath(player, realm[i], stage[i]);
			helper.assertTrue(near(TwoPaths.harmony(c), wanted[i]),
					"Ghost path at " + realm[i].getId() + " " + stage[i] + ": harmony " + wanted[i] + " (" + TwoPaths.harmony(c) + ")");
		}
		// The stages are counted across a realm's border, whichever path is the higher.
		heaven(player, Realm.QI_REFINING, Stage.GRAND_PERFECTION);
		ghostPath(player, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		helper.assertTrue(TwoPaths.stageGap(c) == 1 && near(TwoPaths.harmony(c), 0.20), "Qi Refining Grand Perfection and Foundation Building Early: a stage apart, +20%");
		ghostPath(player, Realm.QI_REFINING, Stage.GRAND_PERFECTION);
		helper.assertTrue(near(TwoPaths.harmony(c), 0.30), "The same stage: the fullest harmony");
		ghostPath(player, Realm.FOUNDATION_BUILDING, Stage.LATE);
		helper.assertTrue(TwoPaths.stageGap(c) == 3 && TwoPaths.harmony(c) == 0, "Three stages apart: none (the ghost path above)");
		helper.succeed();
	}

	/**
	 * The spells of both paths are the heaven path's and the Demon Path's (the user's ask, 2026-10-24: the heaven path's and the ghost
	 * path's before): neither path alone, nor the heaven path with the ghost path, reaches them; the dead cast none.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theSpellsOfBothPathsNeedHeavenAndDemon(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.NASCENT_SOUL, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		try {
			for (Spell spell : Spell.values()) if (spell.isDual()) helper.assertTrue(!spell.isKnown(c), spell.getId() + ": not on the heaven path alone");
			ghostPath(player, Realm.FOUR_AXIS, Stage.EARLY);
			for (Spell spell : Spell.values()) if (spell.isDual()) helper.assertTrue(!spell.isKnown(c), spell.getId() + ": nor with the ghost path beside it");
			TwoPaths.onJoin(player);
			demonPath(player, Realm.CORE_FORMATION, Stage.EARLY);
			helper.assertTrue(Spell.HEAVEN_DEMON_PALM.isKnown(c) && Spell.HEAVEN_DEMON_MANTLE.isKnown(c) && !Spell.DEVOURING_SEAL.isKnown(c),
					"The Demon Path at Core Formation beside the heaven path: the palm and the mantle, not the seal");
			TwoPaths.watch(player);
			boolean onWheel = false;
			for (int i = 0; i < Spells.WHEEL_SLOTS; i++) onWheel |= s.spellSlot(i) == Spell.HEAVEN_DEMON_MANTLE;
			helper.assertTrue(onWheel, "Newly in reach, the mantle goes on the wheel");
			demonPath(player, Realm.DAO_INTEGRATION, Stage.EARLY);
			helper.assertTrue(Spell.DEVOURING_SEAL.isKnown(c) && !Spell.HEAVEN_DEMON_CLAW.isKnown(c), "The Demon Path far above: only as far as the heaven path, Nascent Soul");
			heaven(player, Realm.DAO_INTEGRATION, Stage.EARLY);
			for (Spell spell : Spell.values()) {
				if (!spell.isDual()) continue;
				boolean reached = spell.dualRealm().ordinal() <= Realm.DAO_INTEGRATION.ordinal();
				helper.assertTrue(spell.isKnown(c) == reached, spell.getId() + (reached ? ": both paths at Dao Integration" : ": not till both paths reach its realm"));
			}
			s.setGhost(true);
			for (Spell spell : Spell.values()) if (spell.isDual()) helper.assertTrue(!spell.isKnown(c), spell.getId() + ": the dead cast none of them");
			s.setGhost(false);
		} finally {
			TwoPaths.forget(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSpellOfBothPathsCostsQiAlone(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		try {
			demonPath(player, Realm.FOUNDATION_BUILDING, Stage.EARLY);
			s.setGhostQi(100);
			c.setQi(0);
			Spells.cast(player, Spell.HEAVEN_DEMON_PALM);
			helper.assertTrue(Spells.cooldownLeft(player, Spell.HEAVEN_DEMON_PALM) == 0, "No qi: not cast");
			c.setQi(100);
			Spells.cast(player, Spell.HEAVEN_DEMON_PALM);
			helper.assertTrue(near(c.getQi(), 100 - Spell.HEAVEN_DEMON_PALM.qiCost()) && near(s.ghostQi(), 100),
					"Cast: qi alone drawn on, the ghost qi untouched (" + c.getQi() + ", " + s.ghostQi() + ")");
			helper.assertTrue(Spells.cooldownLeft(player, Spell.HEAVEN_DEMON_PALM) > 0, "And it cools");
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
		demonPath(player, Realm.FOUR_AXIS, Stage.EARLY);
		List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
		standAt(player, 0, 0, 0); // facing south, +z
		// Heaven-Demon Palm: the first ahead struck, and the harm mends the caster.
		Zombie palmed = zombie(level, 0, 4);
		made.add(palmed);
		player.setHealth(player.getMaxHealth() / 2);
		float hurt = player.getHealth();
		c.setQi(100_000);
		Spells.cast(player, Spell.HEAVEN_DEMON_PALM);
		helper.assertTrue(palmed.getHealth() < palmed.getMaxHealth() && player.getHealth() > hurt, "The palm strikes, and mends (" + player.getHealth() + ")");
		helper.assertTrue(palmed.getLastDamageSource() != null && palmed.getLastDamageSource().is(ModDamageTypes.HEAVEN_DEMON), "...with heaven's and a demon's qi");
		// Heaven-Demon Mantle: an arrow flying at the caster turned back on its way.
		Spells.cast(player, Spell.HEAVEN_DEMON_MANTLE);
		helper.assertTrue(HeavenDemonSpells.mantled(player) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "The mantle wraps it");
		Arrow arrow = new Arrow(level, SPELL_GROUND.getX() + 0.5, SPELL_GROUND.getY() + 1, SPELL_GROUND.getZ() + 3.0);
		arrow.setDeltaMovement(new Vec3(0, 0, -1.0));
		arrow.setOwner(palmed);
		level.addFreshEntity(arrow);
		made.add(arrow);
		// Devouring Seal on a fresh one; looking at nothing, it isn't cast (nothing paid).
		Zombie sealed = zombie(level, 2, 6);
		made.add(sealed);
		standAt(player, 0, 0, 180);
		c.setQi(100_000);
		double full = c.getQi();
		Spells.cast(player, Spell.DEVOURING_SEAL);
		helper.assertTrue(near(c.getQi(), full) && Spells.cooldownLeft(player, Spell.DEVOURING_SEAL) == 0, "Nothing to seal: refused, nothing paid (" + full + " -> " + c.getQi() + ")");
		standAt(player, 0, 0, (float) Math.toDegrees(Math.atan2(-2, 6))); // a little east of south, at it
		Spells.cast(player, Spell.DEVOURING_SEAL);
		helper.assertTrue(HeavenDemonSpells.sealed(sealed), "Sealed (look " + player.getViewVector(1.0f) + " from " + player.getEyePosition() + " at " + sealed.position()
				+ ", qi " + c.getQi() + ", cooling " + Spells.cooldownLeft(player, Spell.DEVOURING_SEAL) + ")");
		helper.runAfterDelay(30, () -> {
			try {
				helper.assertTrue(arrow.getOwner() == player && arrow.getDeltaMovement().z > 0, "The arrow is turned back, the caster's own now (" + arrow.getDeltaMovement() + ")");
				helper.assertTrue(!sealed.isAlive() || sealed.getHealth() < sealed.getMaxHealth(), "The seal devours its life (" + sealed.getHealth() + ")");
				// Heaven-Demon Claw where it looks: a fresh one struck, thrown up and weakened (the palmed one out of the way).
				palmed.discard();
				standAt(player, 0, 0, 0);
				Zombie clawed = zombie(level, 0, 7);
				clawed.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
				clawed.setHealth(100);
				made.add(clawed);
				c.setQi(100_000);
				Spells.cast(player, Spell.HEAVEN_DEMON_CLAW);
				helper.assertTrue(clawed.getHealth() < clawed.getMaxHealth() && clawed.hasEffect(MobEffects.WEAKNESS), "The claw strikes and weakens (" + clawed.getHealth() + ")");
				// Heaven and Demon as One: all round dragged in and unmade; the caster made strong.
				Zombie a = zombie(level, 6, -4), b = zombie(level, -7, 2);
				made.add(a);
				made.add(b);
				c.setQi(100_000);
				Spells.cast(player, Spell.HEAVEN_AND_DEMON_AS_ONE);
				helper.assertTrue(!a.isAlive() && !b.isAlive(), "Unmade by Heaven and Demon as One");
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
		c.setAbilityEnabled(Ability.FORM_SHIFT, false);
		s.setGhost(true);
		// Dead, Four Axis on the heaven path and Nascent Soul on the ghost: a Ghost King, its true form the King's, its human form
		// there for Form Shift to take.
		helper.assertTrue(TwoPaths.formsOpen(c) && Ability.FORM_SHIFT.isUnlocked(c) && TwoPaths.trueForm(c) == TwoPaths.Form.GHOST_KING,
				"A Ghost King has its two forms");
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING && GhostSystem.isKing(c) && !TwoPaths.inGhostForm(c) && !TwoPaths.inHumanForm(c),
				"Dead, Form Shift off: a Ghost King, drawn as one");
		c.setAbilityEnabled(Ability.FORM_SHIFT, true);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.HUMAN && TwoPaths.inHumanForm(c) && !TwoPaths.inGhostForm(c), "Form Shift on: its human form");
		c.setAbilityEnabled(Ability.FORM_SHIFT, false);
		// No ghost path at all, dead at Four Axis on the heaven path: one of the common dead, one form (the user's rule, 2026-10-19).
		for (GhostMethod method : GhostMethod.values()) if (method.isFoundation()) s.setMethod(method, 0, 0);
		s.setPath(-1, 0);
		helper.assertTrue(TwoPaths.ghostRank(c, false) < 0 && !GhostSystem.isKing(c) && !TwoPaths.formsOpen(c) && !Ability.FORM_SHIFT.isUnlocked(c)
				&& TwoPaths.look(c) == TwoPaths.Look.DEAD, "The heaven path's Four Axis alone crowns no ghost");
		ghostPath(player, Realm.NASCENT_SOUL, Stage.EARLY);
		// Only the ghost path at Core Formation makes a king of the dead, whatever the heaven path; below it, a common ghost.
		heaven(player, Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING, "Dead at Qi Refining with a Nascent Soul ghost path: a Ghost King");
		ghostPath(player, Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.DEAD, "Below Core Formation on both paths: a common ghost");
		heaven(player, Realm.CORE_FORMATION, Stage.EARLY);
		helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.DEAD && !GhostSystem.isKing(c),
				"Dead at Core Formation on the heaven path, the ghost path below it: a common ghost still");
		// Alive again: the forms are its own again.
		ghostPath(player, Realm.NASCENT_SOUL, Stage.EARLY);
		s.setGhost(false);
		helper.assertTrue(TwoPaths.formsOpen(c) && TwoPaths.trueForm(c) == TwoPaths.Form.GHOST_KING, "Born again: its true form the ghost path's");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostKingBornAgainKeepsItsForm(GameTestHelper helper) {
		// Four Axis on the heaven path, the ghost path at Core Formation: dead, a Ghost King (by its ghost path).
		ServerPlayer king = cultivator(helper, Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		ghostPath(king, Realm.CORE_FORMATION, Stage.EARLY);
		// Four Axis on the heaven path as the set command makes it, no ghost path: dead, one of the common dead (the user's rule).
		ServerPlayer heavenOnly = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		CultivationCommand.set(CultivationManager.get(heavenOnly), Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		CultivationManager.refresh(heavenOnly);
		helper.assertTrue(TwoPaths.ghostRank(CultivationManager.get(heavenOnly), false) < 0, "The set command sets the heaven path alone");
		GhostSystem.makeGhost(king);
		GhostSystem.makeGhost(heavenOnly);
		helper.assertTrue(GhostSystem.isKing(king) && CultivationManager.get(king).netherworld().reignedKing(), "Dead, a Ghost King");
		helper.assertTrue(!GhostSystem.isKing(heavenOnly) && !CultivationManager.get(heavenOnly).netherworld().reignedKing()
				&& TwoPaths.look(CultivationManager.get(heavenOnly)) == TwoPaths.Look.DEAD, "Four Axis on the heaven path alone: a common ghost");
		net.minecraft.server.players.PlayerList players = helper.getLevel().getServer().getPlayerList();
		ServerPlayer kingReborn = null, heavenReborn = null;
		try {
			GhostSystem.revive(king);
			GhostSystem.revive(heavenOnly);
			kingReborn = players.getPlayer(king.getUUID());
			heavenReborn = players.getPlayer(heavenOnly.getUUID());
			helper.assertTrue(kingReborn != null && !GhostSystem.isGhost(kingReborn) && heavenReborn != null, "Both born again");
			PlayerCultivation c = CultivationManager.get(kingReborn);
			// Born again, it keeps the King's form: its true form the human (the heaven path is the higher), Form Shift there for the other.
			helper.assertTrue(TwoPaths.formsOpen(c) && Ability.FORM_SHIFT.isUnlocked(c) && TwoPaths.trueForm(c) == TwoPaths.Form.HUMAN
					&& TwoPaths.look(c) == TwoPaths.Look.HUMAN, "A Ghost King born again keeps its two forms");
			CultivationManager.toggleAbility(kingReborn, Ability.FORM_SHIFT.ordinal());
			helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.KING && TwoPaths.inGhostForm(c), "Form Shift: the Ghost King's form, alive");
			CultivationManager.toggleAbility(kingReborn, Ability.FORM_SHIFT.ordinal());
			helper.assertTrue(TwoPaths.look(c) == TwoPaths.Look.HUMAN, "...and back to the human");
			// It reigned: the form stays its own even with its ghost path fallen below Core Formation (a ghost core detonated).
			GhostMethods.fallTo(c.netherworld(), PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION));
			helper.assertTrue(TwoPaths.ghostRank(c, false) < GhostSystem.KING_RANK && TwoPaths.formsOpen(c), "...kept though its ghost path fell");
			// The heaven path's Four Axis had no King's form to keep.
			PlayerCultivation h = CultivationManager.get(heavenReborn);
			helper.assertTrue(!TwoPaths.formsOpen(h) && !Ability.FORM_SHIFT.isUnlocked(h) && TwoPaths.look(h) == TwoPaths.Look.HUMAN,
					"The heaven path's Four Axis born again: one form, the human");
		} finally {
			GhostSystem.forget(king.getUUID());
			GhostSystem.forget(heavenOnly.getUUID());
			TwoPaths.forget(king.getUUID());
			TwoPaths.forget(heavenOnly.getUUID());
			if (kingReborn != null) players.remove(kingReborn);
			if (heavenReborn != null) players.remove(heavenReborn);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aReignTheGhostPathNeverReachedIsUndoneInOldWorlds(GameTestHelper helper) {
		// Saved before 2026-10-19: reigned (crowned by the heaven path's Core Formation then), no ghost path. Undone as it loads.
		net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
		old.putBoolean("ReignedKing", true);
		NetherworldState heavenKing = new NetherworldState();
		heavenKing.load(old);
		helper.assertTrue(!heavenKing.reignedKing(), "A reign the ghost path never reached is undone");
		// The same, its ghost path at Core Formation: a true reign, kept.
		PlayerCultivation scratch = new PlayerCultivation();
		scratch.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		ghostPath(scratch, Realm.CORE_FORMATION, Stage.EARLY);
		scratch.netherworld().setReignedKing(true);
		net.minecraft.nbt.CompoundTag ghostKing = scratch.netherworld().save();
		ghostKing.remove("KingByGhostPath"); // as an old world wrote it
		NetherworldState kept = new NetherworldState();
		kept.load(ghostKing);
		helper.assertTrue(kept.reignedKing(), "A reign the ghost path reached is kept");
		// Saved since, a reign stays whatever became of the ghost path after (a ghost core detonated).
		NetherworldState fallen = new NetherworldState();
		net.minecraft.nbt.CompoundTag since = new net.minecraft.nbt.CompoundTag();
		since.putBoolean("ReignedKing", true);
		since.putBoolean("KingByGhostPath", true);
		fallen.load(since);
		helper.assertTrue(fallen.reignedKing(), "A reign saved under the ghost path's rule is kept");
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
			s.setPath(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION), 0);
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
