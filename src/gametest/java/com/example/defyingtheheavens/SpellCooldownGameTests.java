package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Arts' cooldowns back (the user's ask, 2026-11-10: "add spell cooldowns back, make them change based on realm though so for example a true
 * immortals wouldnt have to wait long for a 4 axis spell"): half as long for each realm its caster stands above a spell's own, a stage at a
 * time, never longer than its own and never under a second (Spell#cooldownFor, #floored); a player's by its highest path, then the Law of
 * Time (Laws#cooldown); the gamerule on by default again, and a world that saved it off turned on once (ModGameRules#cooldownsBack).
 */
public class SpellCooldownGameTests implements FabricGameTest {
	private static final int STAGES = Stage.values().length;

	private static PlayerCultivation at(Realm realm, Stage stage) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(realm, stage, 0);
		return c;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_cooldown_realms")
	public void eachRealmAboveHalvesTheWait(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			int own = spell.rank(), base = spell.cooldownTicks();
			if (spell.isDual() && own != PlayerCultivation.rank(spell.dualRealm(), Stage.EARLY)) wrong.add(spell.getId() + " stands at " + own);
			if (spell.cooldownFor(own) != base || spell.cooldownFor(own - 3) != base) wrong.add(spell.getId() + " not its own at or below its rank");
			if (Math.abs(spell.cooldownFor(own + STAGES) - base / 2.0) > 1.0e-9) wrong.add(spell.getId() + " not halved a realm up");
			double stageUp = spell.cooldownFor(own + 1);
			if (base > 0 && !(stageUp < base && stageUp > base / 2.0)) wrong.add(spell.getId() + " a stage up: " + stageUp);
			double last = base;
			for (int rank = own; rank <= PlayerCultivation.rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION); rank++) {
				double now = spell.cooldownFor(rank);
				if (now > last + 1.0e-9) wrong.add(spell.getId() + " longer at rank " + rank);
				last = now;
			}
			// Never under a second (or its own, if shorter); and every art up to Four Axis's is down to that for a Heavenly Lord.
			int floor = Math.min(base, Spell.COOLDOWN_FLOOR), topped = spell.floored(spell.cooldownFor(PlayerCultivation.rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION)));
			if (topped < floor || own <= PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION) && topped != floor) wrong.add(spell.getId() + " at the top " + topped);
		}
		helper.assertTrue(wrong.isEmpty(), "Each realm above an art's own halves its wait, a stage at a time, floored at a second: " + wrong);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_cooldown_immortal")
	public void aTrueImmortalHardlyWaitsOnAFourAxisArt(GameTestHelper helper) {
		PlayerCultivation immortal = at(Realm.TRUE_IMMORTAL, Stage.EARLY), axis = at(Realm.FOUR_AXIS, Stage.EARLY), dao = at(Realm.DAO_INTEGRATION, Stage.EARLY);
		Spell greatest = null;
		List<String> wrong = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			if (!spell.isHeaven() || spell.heavenRealm() != Realm.FOUR_AXIS) continue;
			if (greatest == null || spell.cooldownTicks() > greatest.cooldownTicks()) greatest = spell;
			int base = spell.cooldownTicks(), forImmortal = Laws.cooldown(immortal, spell);
			if (forImmortal * 16 > Math.max(base, Spell.COOLDOWN_FLOOR * 16)) wrong.add(spell.getId() + " " + forImmortal + " of " + base);
			// One standing at the art's own stage waits all of it; a realm higher, half.
			PlayerCultivation own = at(Realm.FOUR_AXIS, Stage.byIndex(spell.rank() % STAGES));
			if (Laws.cooldown(own, spell) != base) wrong.add(spell.getId() + " at its own stage " + Laws.cooldown(own, spell));
			PlayerCultivation realmUp = at(Realm.DAO_INTEGRATION, Stage.byIndex(spell.rank() % STAGES));
			if (Laws.cooldown(realmUp, spell) != spell.floored(base / 2.0)) wrong.add(spell.getId() + " a realm up " + Laws.cooldown(realmUp, spell));
		}
		helper.assertTrue(greatest != null && wrong.isEmpty(), "A True Immortal waits a sixteenth of a Four Axis art's at most; its own stage the whole, a realm up half: " + wrong);
		int greatestForImmortal = Laws.cooldown(immortal, greatest);
		helper.assertTrue(greatestForImmortal <= 20 * 20 && Laws.cooldown(axis, greatest) == greatest.cooldownTicks() && Laws.cooldown(dao, greatest) < greatest.cooldownTicks(),
				"The greatest (" + greatest.getId() + ", " + greatest.cooldownTicks() / 20 + " s): " + greatestForImmortal / 20.0 + " s for a True Immortal");
		// Its own realm's arts the whole, Early to Early.
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven() && spell.heavenRealm() == Realm.TRUE_IMMORTAL && spell.rank() == immortal.getRealm().ordinal() * STAGES
					&& Laws.cooldown(immortal, spell) != spell.cooldownTicks()) wrong.add(spell.getId());
		}
		helper.assertTrue(wrong.isEmpty(), "A True Immortal Early waits all of its own Early arts': " + wrong);
		// One standing below the art (as a manual's or a bloodline's may) waits its own, never longer.
		PlayerCultivation low = at(Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(TwoPaths.higherRank(low, false) == 0 && Laws.cooldown(low, greatest) == greatest.cooldownTicks(), "Below the art, its own and never longer");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "spell_cooldown_rule_back")
	public void theRuleComesBackOnceInEveryWorld(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		GameRules.BooleanValue rule = server.getGameRules().getRule(ModGameRules.SPELL_COOLDOWNS);
		CultivationSavedData data = CultivationSavedData.forServer(server);
		boolean was = rule.get();
		try {
			helper.assertTrue(new GameRules().getBoolean(ModGameRules.SPELL_COOLDOWNS), "On by default in a new world");
			helper.assertTrue(data.cooldownsBack(), "This world had them turned back on as it started");
			// A world saved while the rule was off by default, never turned back on: on at its next start, and marked.
			rule.set(false, server);
			data.forgetCooldownsBack();
			ModGameRules.cooldownsBack(server);
			helper.assertTrue(rule.get() && data.cooldownsBack(), "A world saved with the rule off: on at its next start");
			// Turned off again after, it stays off.
			rule.set(false, server);
			ModGameRules.cooldownsBack(server);
			helper.assertTrue(!rule.get(), "Turned off again after, it stays off");
			helper.succeed();
		} finally {
			rule.set(was, server);
		}
	}
}
