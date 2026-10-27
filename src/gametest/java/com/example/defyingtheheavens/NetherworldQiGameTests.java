package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;

/**
 * The Netherworld for the living (phase 2): ghostly qi and its density, yin among the elements, yin corruption (filling in the
 * Netherworld, held on the river, ebbing elsewhere, burnt away by the Yang Fire Talisman), the Netherworld's saved state, and
 * the Soul River's course. The test server has only vanilla's dimensions, so these call the pure rules the realm's code calls.
 */
public class NetherworldQiGameTests implements FabricGameTest {
	private static Holder<Biome> biome(GameTestHelper helper, ResourceKey<Biome> key) {
		Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		return biomes.getHolderOrThrow(key);
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-9;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ghostlyQiIsThickestInTheNetherworld(GameTestHelper helper) {
		helper.assertTrue(near(GhostQi.density(ModDimensions.NETHERWORLD, biome(helper, ModBiomes.FIELDS_OF_THE_OTHER_SHORE)), 10),
				"The Netherworld: x10");
		helper.assertTrue(near(GhostQi.density(ModDimensions.NETHERWORLD, biome(helper, ModBiomes.NINE_NETHER_PEAKS)), 12),
				"The Nine Nether Peaks: x12");
		helper.assertTrue(near(GhostQi.density(Level.NETHER, biome(helper, Biomes.SOUL_SAND_VALLEY)), 1), "A Soul Sand Valley: x1");
		helper.assertTrue(near(GhostQi.density(Level.NETHER, biome(helper, Biomes.NETHER_WASTES)), 0.5), "The rest of the Nether: x0.5");
		helper.assertTrue(near(GhostQi.density(Level.OVERWORLD, biome(helper, Biomes.PLAINS)), 0.25)
				&& near(GhostQi.densityAt(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1))), 0.25), "The Overworld: x0.25 (the thinnest), anywhere in it");
		helper.assertTrue(GhostQi.density(ModDimensions.UPPER_REALM, biome(helper, ModBiomes.SPIRIT_FOREST)) == 0
				&& GhostQi.density(Level.END, biome(helper, Biomes.THE_END)) == 0, "None anywhere else");
		helper.assertTrue(near(GhostQi.fillPerSecond(10), 2) && near(GhostQi.fillPerSecond(12), 2.4) && near(GhostQi.fillPerSecond(0.5), 0.1)
				&& near(GhostQi.fillPerSecond(0.25), 0.05), "The pool fills at 2/s in the Netherworld, 2.4 on its peaks, 0.1 in the Nether, 0.05 in the Overworld");
		helper.assertTrue(GhostQi.poolMax(false, 5, 5) == 0 && near(GhostQi.poolMax(true, 0, 0), 30)
				&& near(GhostQi.poolMax(true, 27, 9), 480), "No pool without one; 30, up to 480 with every layer");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thePoolAndItsFillingGrowWithTheGhostRealm(GameTestHelper helper) {
		// The user's ask, 2026-10-27: "ghost qi pool should increase with realm in ghost path similarly to how it does with the others".
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(false);
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		helper.assertTrue(GhostQi.realmPool(c) == 0 && near(GhostQi.realmFill(c), 1), "No ghost path yet: nothing from a realm");
		c.netherworld().setPath(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 0);
		helper.assertTrue(near(GhostQi.realmPool(c), CultivationStats.maxQi(Realm.QI_REFINING, Stage.EARLY)) && near(GhostQi.realmFill(c), 1)
				&& near(GhostQi.fillPerSecond(c, GhostQi.NETHERWORLD), 2), "Qi Refining Early: its realm's 100, and the Netherworld fills it at 2/s as ever");
		c.netherworld().setPath(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), 0);
		double pool = CultivationStats.maxQi(Realm.TRUE_IMMORTAL, Stage.EARLY);
		double fill = CultivationStats.qiGather(Realm.TRUE_IMMORTAL, Stage.EARLY) / CultivationStats.qiGather(Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(near(GhostQi.realmPool(c), pool) && GhostQi.poolMax(c) >= pool, "A True Immortal's ghost path: the heaven path's pool of its rank (" + pool + ")");
		helper.assertTrue(near(GhostQi.realmFill(c), fill) && near(GhostQi.fillPerSecond(c, GhostQi.NETHERWORLD), 2 * fill)
				&& GhostQi.fillPerSecond(c, GhostQi.OVERWORLD) < GhostQi.fillPerSecond(c, GhostQi.NETHERWORLD),
				"...filled as much faster as its realm gathers, the place still the measure (" + fill + "x)");
		c.setMortal(true);
		helper.assertTrue(GhostQi.realmPool(c) == 0, "A mortal's dormant path gives nothing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void yinShowsOnlyWhereGhostlyQiFlows(GameTestHelper helper) {
		RandomSource random = RandomSource.create(77L);
		int rolls = 4000;
		int[] yin = new int[3];
		Holder<Biome> plains = biome(helper, Biomes.PLAINS), fields = biome(helper, ModBiomes.FIELDS_OF_THE_OTHER_SHORE),
				valley = biome(helper, Biomes.SOUL_SAND_VALLEY);
		for (int i = 0; i < rolls; i++) {
			if (QiElement.pick(plains, false, random) == QiElement.YIN) yin[0]++;
			if (QiElement.pick(fields, true, random) == QiElement.YIN) yin[1]++;
			if (QiElement.pick(valley, false, random) == QiElement.YIN) yin[2]++;
		}
		helper.assertTrue(yin[0] == 0, "Not a mote of yin in the living world (" + yin[0] + ")");
		helper.assertTrue(yin[1] > rolls * 0.88, "The Netherworld is almost pure yin, underground too (" + yin[1] + " of " + rolls + ")");
		helper.assertTrue(yin[2] > rolls / 2 && yin[2] < rolls * 0.65, "A Soul Sand Valley is mostly yin (" + yin[2] + " of " + rolls + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theStrongerTheBodyTheLongerItHoldsOutTheYin(GameTestHelper helper) {
		PlayerCultivation mortal = new PlayerCultivation();
		helper.assertTrue(near(YinCorruption.fullStaySeconds(mortal, 0), 60), "A mortal: a minute");
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(near(YinCorruption.fullStaySeconds(c, 0), 60 * 1.3), "Below Nascent Soul: a minute, by stage");
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		helper.assertTrue(near(YinCorruption.fullStaySeconds(c, 0), 300), "Nascent Soul Early: 5 minutes");
		c.setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
		helper.assertTrue(near(YinCorruption.fullStaySeconds(c, 0), 600), "Heavenly Being Early: 10 minutes");
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(near(YinCorruption.fullStaySeconds(c, 0), 1560), "Four Axis Grand Perfection: 26 minutes");
		helper.assertTrue(near(YinCorruption.fullStaySeconds(c, 9), 1560 * 1.9), "...and with all nine layers of Breathing Art, x1.9");
		helper.assertTrue(near(YinCorruption.perSecond(c, 0, true), 0.5 / 1560), "The Veil halves the rate");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void yinRisesThereHoldsOnTheRiverAndEbbsElsewhere(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		NetherworldState s = c.netherworld();
		for (int second = 0; second < 150; second++) YinCorruption.step(c, true, false, 0, false, 0);
		helper.assertTrue(near(s.yinCorruption(), 0.5), "Half full after half a Nascent Soul Early's 5 minutes (" + s.yinCorruption() + ")");
		helper.assertTrue(YinCorruption.step(c, false, true, 0, false, 0) == 0 && near(s.yinCorruption(), 0.5), "Held still on the river");
		helper.assertTrue(YinCorruption.step(c, false, false, 0, false, 0) == -1 && near(s.yinCorruption(), 0.5 - 1.0 / 300),
				"Ebbing outside at 1/300 a second");
		s.setYinPausedUntil(100);
		helper.assertTrue(YinCorruption.step(c, true, false, 0, false, 50) == 0, "A Yang Preservation Pill holds it still...");
		helper.assertTrue(YinCorruption.step(c, true, false, 0, false, 100) == 1, "...until it wears off");
		s.setYinCorruption(0.5);
		helper.assertTrue(near(YinCorruption.cleanse(c, YinCorruption.TALISMAN_CLEANSE), 1.0 / 3) && near(s.yinCorruption(), 0.5 - 1.0 / 3),
				"A Yang Fire Talisman burns away a third");
		helper.assertTrue(near(YinCorruption.cleanse(c, YinCorruption.TALISMAN_CLEANSE), 0.5 - 1.0 / 3) && s.yinCorruption() == 0,
				"...never below nothing");
		helper.assertTrue(YinCorruption.cleanse(c, YinCorruption.TALISMAN_CLEANSE) == 0, "...and nothing to burn, nothing burnt (the talisman isn't spent)");
		s.setYinCorruption(0.999);
		YinCorruption.step(c, true, false, 0, false, 200); // the pill has worn off by then
		helper.assertTrue(s.yinCorruption() == 1.0, "It fills to full and no further");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theNetherworldsHoldIsSavedOnlyWhenThere(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		helper.assertTrue(!c.save().contains("Netherworld"), "Nothing saved for someone the Netherworld never touched (or an NPC)");
		NetherworldState s = c.netherworld();
		s.setGhostQi(12.5);
		s.setYinCorruption(0.4);
		s.setYinPausedUntil(1234);
		s.setRiver(SoulRiver.Route.DESCEND, 17, -9);
		s.setRiverCooldownUntil(999);
		s.setDensity(10);
		CompoundTag saved = c.save();
		NetherworldState loaded = PlayerCultivation.load(saved).netherworld();
		helper.assertTrue(near(loaded.ghostQi(), 12.5) && near(loaded.yinCorruption(), 0.4) && loaded.yinPausedUntil() == 1234,
				"The pool, the corruption and the pill's hold come back");
		helper.assertTrue(loaded.river() == SoulRiver.Route.DESCEND && loaded.riverFromX() == 17 && loaded.riverFromZ() == -9
				&& loaded.riverCooldownUntil() == 999, "...and the river journey, where it set out from, and the rifts' cooldown");
		helper.assertTrue(loaded.density() == 0, "The density isn't saved: it is measured again");
		CompoundTag older = c.save();
		older.remove("Netherworld");
		NetherworldState fresh = PlayerCultivation.load(older).netherworld();
		helper.assertTrue(fresh.ghostQi() == 0 && fresh.yinCorruption() == 0 && fresh.river() == SoulRiver.Route.NONE,
				"A save from before the Netherworld loads with every default");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSoulRiverRunsTrueAtItsEndsAndCanBeFollowed(GameTestHelper helper) {
		Vec3 start = SoulRiverCourse.centre(0), end = SoulRiverCourse.centre(SoulRiverCourse.LENGTH);
		helper.assertTrue(near(start.z, 0) && near(start.y, SoulRiverCourse.BASE_Y) && near(end.z, 0) && near(end.y, SoulRiverCourse.BASE_Y),
				"Both ends lie on the river's line");
		helper.assertTrue(near(SoulRiverCourse.LENGTH, 480) && SoulRiverCourse.TICKS == 1200, "60 seconds at 0.4 a tick: 480 blocks");
		double steepest = 0;
		for (double x = 0; x < SoulRiverCourse.LENGTH; x += 0.5) {
			Vec3 a = SoulRiverCourse.centre(x), b = SoulRiverCourse.centre(x + 0.5);
			steepest = Math.max(steepest, Math.hypot(b.z - a.z, b.y - a.y) / 0.5);
		}
		double needed = steepest * SoulRiverCourse.FLOW + SoulRiverCourse.TURBULENCE_END;
		helper.assertTrue(needed < SoulRiverCourse.STEER, "The sharpest bend and strongest crosscurrent need " + needed + " a tick, under the "
				+ SoulRiverCourse.STEER + " a traveller can steer");
		helper.assertTrue(steepest > 0.2, "...but the river does bend: it has to be steered (" + steepest + ")");
		helper.assertTrue(SoulRiverCourse.radius(0) == SoulRiverCourse.RADIUS_START && SoulRiverCourse.radius(1) == SoulRiverCourse.RADIUS_END
				&& SoulRiverCourse.progress(0, true) == 0 && SoulRiverCourse.progress(0, false) == 1, "It narrows the way you go, either way");
		helper.assertTrue(near(SoulRiverCourse.distanceFromCentre(SoulRiverCourse.centre(200).add(0, 0, 2)), 2), "Distance is measured across the river");
		for (int tick = 0; tick <= SoulRiverCourse.TICKS; tick += 7) {
			Vec3 push = SoulRiverCourse.crosscurrent(tick);
			helper.assertTrue(push.x == 0 && push.length() <= SoulRiverCourse.TURBULENCE_END + 1.0e-9, "Crosscurrents only push across, never past their strength");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theChiefJudgesEyeMustBeTurnedToAndKillsThoseWhoHoldItsGaze(GameTestHelper helper) {
		helper.assertTrue(!JudgeEye.meets(new Vec3(1, 0, 0)) && !JudgeEye.meets(new Vec3(-1, 0, 0)),
				"Looking down the river either way, the Eye is out of the gaze: it hangs square to the flow");
		helper.assertTrue(JudgeEye.meets(JudgeEye.DIRECTION) && JudgeEye.meets(new Vec3(0, 0, 1)), "Turned 90 degrees toward it, it is met");
		helper.assertTrue(!JudgeEye.meets(new Vec3(0, 0, -1)), "...and not from the other side");
		double gaze = 0;
		int ticks = 0;
		while (gaze < 1 && ticks < 1000) {
			gaze = JudgeEye.step(gaze, true);
			ticks++;
		}
		helper.assertTrue(Math.abs(ticks - 200) <= 1, "Ten seconds of meeting it kill (" + ticks + " ticks)");
		ticks = 0;
		while (gaze > 0 && ticks < 1000) {
			gaze = JudgeEye.step(gaze, false);
			ticks++;
		}
		helper.assertTrue(Math.abs(ticks - 80) <= 1, "Looking away, it ebbs from the brink in four seconds (" + ticks + " ticks)");
		boolean blinks = false, open = false;
		for (int t = 0; t < JudgeEye.BLINK_EVERY * 3; t++) {
			float o = JudgeEye.openness(t);
			blinks |= o < 0.3f;
			open |= o == 1;
		}
		helper.assertTrue(blinks && open, "It blinks now and then, and is open the rest of the time");
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(!JudgeEye.isImmune(c), "Nothing below a True Immortal is spared, a Four Axis Grand Perfection included");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void paintingATalismanWearsTheBrush(GameTestHelper helper) {
		ItemStack brush = new ItemStack(ModItems.INSCRIPTION_BRUSH);
		ItemStack back = brush.getItem().getRecipeRemainder(brush);
		helper.assertTrue(back.is(ModItems.INSCRIPTION_BRUSH) && back.getDamageValue() == 1 && back.getCount() == 1, "The brush comes back a use more worn");
		brush.setDamageValue(brush.getMaxDamage() - 1);
		helper.assertTrue(brush.getItem().getRecipeRemainder(brush).isEmpty(), "...or not at all, if that was its last");
		helper.assertTrue(helper.getLevel().getRecipeManager().byKey(DefyingTheHeavens.id("yang_fire_talisman")).isPresent(),
				"The Yang Fire Talisman's recipe is there");
		helper.succeed();
	}
}
