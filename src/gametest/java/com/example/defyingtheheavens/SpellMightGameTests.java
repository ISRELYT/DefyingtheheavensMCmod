package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Spells as their casters' realms (the user's ask, 2026-11-15: "make it so that damage scales with realm so that heavenly lords can use lower tier
 * spells and still do damage to true immortals with them ... balance it so low tier spells arent always good and there is still a need for high
 * tier spells", and "make it so even true immortals have ZERO cooldown for the basic ones, with heavenly lords have ZERO cooldown for the later
 * spells"): a spell strikes the square root of how much mightier its caster stands than it as hard (Spell#harmFor, Spells#harm), and six realms
 * above it waits not at all (Spell#cooldownGone), ready again the next tick, but for the two that keep their wait (Spell#keepsItsWait).
 */
public class SpellMightGameTests implements FabricGameTest {
	private static final int STAGES = Stage.COUNT;

	private static int rank(Realm realm, Stage stage) {
		return PlayerCultivation.rank(realm, stage);
	}

	private static PlayerCultivation at(Realm realm, Stage stage) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(realm, stage, 0);
		return c;
	}

	private static void between(GameTestHelper helper, String what, double value, double low, double high) {
		helper.assertTrue(value >= low && value <= high, what + ": " + value + " (looked for " + low + " to " + high + ")");
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_might_numbers")
	public void aSpellStrikesHarderTheMightierItsCaster(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();
		int top = rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION);
		// Might grows with every stage, so a spell grows with every stage its caster climbs past it.
		for (int r = 1; r <= top; r++) if (Spell.might(r) <= Spell.might(r - 1)) wrong.add("might at rank " + r);
		for (Spell spell : Spell.values()) {
			int own = spell.rank();
			if (spell.harmFor(own) != 1 || spell.harmFor(own - 3) != 1 || spell.harmFor(-1) != 1) wrong.add(spell.getId() + " not as it is at or below its rank");
			double last = 1;
			for (int r = own + 1; r <= top; r++) {
				double now = spell.harmFor(r);
				if (now <= last) wrong.add(spell.getId() + " no harder at rank " + r);
				last = now;
			}
		}
		helper.assertTrue(wrong.isEmpty(), "Each stage its caster climbs above a spell, it strikes harder; never weaker than its own: " + wrong);
		// The numbers the user was told.
		int lordEarly = rank(Realm.HEAVENLY_LORD, Stage.EARLY), immortalEarly = rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
		between(helper, "A Heavenly Lord's True Immortal spell", Spell.FIRST_FLAME_BEFORE_HEAVEN.harmFor(lordEarly), 7.0, 7.6);
		between(helper, "...at its peak", Spell.FIRST_FLAME_BEFORE_HEAVEN.harmFor(top), 8.0, 8.6);
		between(helper, "A Heavenly Lord's Entering Nirvana spell", Spell.PYRE_OF_THE_HEAVENS.harmFor(lordEarly), 21, 25);
		between(helper, "A Heavenly Lord's Four Axis spell", Spell.NINE_SUNS_DESCEND.harmFor(lordEarly), 130, 140);
		between(helper, "A Heavenly Lord's Flame Bolt", Spell.FLAME_BOLT.harmFor(lordEarly), 800, 850);
		between(helper, "A True Immortal's Flame Bolt", Spell.FLAME_BOLT.harmFor(immortalEarly), 100, 107);
		// A spell's harm is made to its own realm's might; cast by one above it, a spell of a deeper realm still strikes the harder: a Heavenly
		// Lord's True Immortal spell some three times its Entering Nirvana one, and that some five times its Four Axis one.
		double tiSpell = Spell.might(Spell.FIRST_FLAME_BEFORE_HEAVEN.rank()) * Spell.FIRST_FLAME_BEFORE_HEAVEN.harmFor(lordEarly);
		double enSpell = Spell.might(Spell.PYRE_OF_THE_HEAVENS.rank()) * Spell.PYRE_OF_THE_HEAVENS.harmFor(lordEarly);
		double faSpell = Spell.might(Spell.NINE_SUNS_DESCEND.rank()) * Spell.NINE_SUNS_DESCEND.harmFor(lordEarly);
		helper.assertTrue(tiSpell > 2.5 * enSpell && enSpell > 4 * faSpell, "The deeper spell the harder, as its realm's might: " + tiSpell + ", " + enSpell + ", " + faSpell);
		// The Arts tab tells it only of spells that strike: not a ward, a mending, a way of moving (Spell#harms).
		int harmless = 0;
		for (Spell spell : Spell.values()) if (!spell.harms()) harmless++;
		helper.assertTrue(harmless == 30 && !Spell.EMERALD_HEALING_ART.harms() && !Spell.GOLDEN_BELL_SHIELD.harms() && !Spell.NINE_NETHER_STEP.harms()
				&& Spell.FLAME_BOLT.harms() && Spell.VAJRA_BODY.harms() && Spell.PHOENIX_NIRVANA.harms() && Spell.GHOSTLY_WAIL.harms(),
				"The spells that strike nothing of their own: " + harmless);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_might_no_cooldown")
	public void noCooldownSixRealmsAboveASpell(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();
		List<PlayerCultivation> casters = List.of(at(Realm.TRUE_IMMORTAL, Stage.EARLY), at(Realm.TRUE_IMMORTAL, Stage.MID), at(Realm.TRUE_IMMORTAL, Stage.LATE),
				at(Realm.EARTH_IMMORTAL, Stage.EARLY), at(Realm.HEAVENLY_LORD, Stage.EARLY), at(Realm.HEAVENLY_LORD, Stage.LATE));
		for (Spell spell : Spell.values()) {
			for (PlayerCultivation c : casters) {
				int r = TwoPaths.higherRank(c, false), wait = Laws.cooldown(c, spell);
				boolean gone = r - spell.rank() >= Spell.COOLDOWN_GONE_REALMS * STAGES && !spell.keepsItsWait();
				if (gone != (wait == 0)) wrong.add(spell.getId() + " at rank " + r + ": " + wait);
			}
		}
		helper.assertTrue(wrong.isEmpty(), "No wait from six realms above a spell, a wait short of it: " + wrong);
		// What the user was told: a True Immortal casts every spell up to Nascent Soul's at will, Heavenly Being's as its stages climb past
		// theirs; a Heavenly Lord at its Late stage every spell there is, but Phoenix Nirvana and the Wheel of Samsara.
		PlayerCultivation immortal = at(Realm.TRUE_IMMORTAL, Stage.EARLY), immortalLate = at(Realm.TRUE_IMMORTAL, Stage.LATE),
				lord = at(Realm.HEAVENLY_LORD, Stage.LATE);
		for (Spell spell : Spell.values()) {
			int realm = spell.rank() / STAGES;
			if (realm <= Realm.NASCENT_SOUL.ordinal() && !spell.keepsItsWait() && Laws.cooldown(immortal, spell) != 0) wrong.add("TI Early " + spell.getId());
			if (realm <= Realm.HEAVENLY_BEING.ordinal() && spell.rank() % STAGES <= Stage.LATE.ordinal() && !spell.keepsItsWait()
					&& Laws.cooldown(immortalLate, spell) != 0) wrong.add("TI Late " + spell.getId());
			if (realm >= Realm.FOUR_AXIS.ordinal() && Laws.cooldown(immortalLate, spell) == 0) wrong.add("TI Late waits not for " + spell.getId());
			if (spell.keepsItsWait() != (Laws.cooldown(lord, spell) > 0)) wrong.add("HL Late " + spell.getId());
		}
		helper.assertTrue(wrong.isEmpty(), "A True Immortal: the lower realms' spells at will; a Heavenly Lord: all of them but two: " + wrong);
		helper.assertTrue(Laws.cooldown(lord, Spell.PHOENIX_NIRVANA) == Spell.COOLDOWN_FLOOR && Laws.cooldown(lord, Spell.SAMSARA_WHEEL) == Spell.COOLDOWN_FLOOR,
				"Phoenix Nirvana and the Wheel of Samsara keep their wait, at its floor");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_might_strike")
	public void anImmortalsFlameBoltLandsAsItsMight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<LivingEntity> spawned = new ArrayList<>();
		CultivatorNpc immortal = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		CultivatorNpc novice = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		spawned.add(immortal);
		spawned.add(novice);
		try {
			immortal.setRank(rank(Realm.TRUE_IMMORTAL, Stage.EARLY), true);
			novice.setRank(rank(Realm.QI_REFINING, Stage.EARLY), true);
			LivingEntity struck = HeavenSpellTesting.idle(level, EntityType.PIG, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 4))), 1.0e6, spawned);
			LivingEntity other = HeavenSpellTesting.idle(level, EntityType.PIG, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 2, 4))), 1.0e6, spawned);
			float byImmortal = HeavenSpells.strike(immortal, struck, FireSpells.BOLT_DAMAGE, Spell.FLAME_BOLT);
			float byNovice = HeavenSpells.strike(novice, other, FireSpells.BOLT_DAMAGE, Spell.FLAME_BOLT);
			double expected = FireSpells.BOLT_DAMAGE * Spell.FLAME_BOLT.harmFor(immortal.sustainedRank());
			helper.assertTrue(Math.abs(byNovice - FireSpells.BOLT_DAMAGE) < 0.01, "A Qi Refining cultivator's Flame Bolt as it is: " + byNovice);
			helper.assertTrue(expected > 500 && Math.abs(byImmortal - expected) < expected * 0.01, "A True Immortal's some hundred times as hard: " + byImmortal + " of " + expected);
			// A player by its highest path as it wields it: a Four Axis caster, held to Heavenly Being's peak in the realms below (PlayerCultivation
			// #wielded, as its body is), some four and a half times.
			ServerPlayer player = HeavenSpellTesting.caster(helper, HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING), helper.absolutePos(new BlockPos(5, 2, 1)));
			float harm = Spells.harm(player, Spell.FLAME_BOLT);
			int wielded = TwoPaths.higherRank(CultivationManager.get(player), true);
			helper.assertTrue(wielded == rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION) && harm == (float) Spell.FLAME_BOLT.harmFor(wielded) && harm > 4 && harm < 5,
					"A Four Axis player's, held to Heavenly Being's peak here: " + harm + " (rank " + wielded + ")");
			helper.assertTrue(Spells.harm(null, Spell.FLAME_BOLT) == 1 && Spells.harm(struck, Spell.FLAME_BOLT) == 1, "No caster, or no cultivator: as it is");
			helper.succeed();
		} finally {
			for (LivingEntity e : spawned) e.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_might_next_tick", timeoutTicks = 60)
	public void aSpellWithNoCooldownComesRoundTheNextTick(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		boolean rule = level.getGameRules().getBoolean(ModGameRules.SPELL_COOLDOWNS);
		level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(true, level.getServer());
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING), helper.absolutePos(new BlockPos(1, 2, 1)));
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		c.setQi(c.maxQi());
		Spell spell = Spell.CRIMSON_TEMPERING;
		try {
			helper.assertTrue(spell.isKnown(c) && Laws.cooldown(p, spell) == 0, "A True Immortal's Crimson Tempering waits not at all: " + Laws.cooldown(p, spell));
			double full = c.getQi();
			Spells.cast(p, spell);
			double once = c.getQi();
			Spells.cast(p, spell); // the same tick: let fall, nothing paid
			helper.assertTrue(once < full && c.getQi() == once && Spells.cooldownLeft(p, spell) == 1, "Once a tick: " + full + " -> " + once + " -> " + c.getQi());
		} catch (RuntimeException | AssertionError e) {
			level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(rule, level.getServer());
			throw e;
		}
		helper.runAfterDelay(1, () -> {
			try {
				double before = c.getQi();
				Spells.cast(p, spell);
				helper.assertTrue(c.getQi() < before, "...and again the next tick");
				helper.succeed();
			} finally {
				level.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS).set(rule, level.getServer());
			}
		});
	}
}
