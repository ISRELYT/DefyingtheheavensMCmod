package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * The bodies players are born with (Physique, Physiques): the roll and the roots, what each does to the qi and the pace of both
 * paths, the roots holding other elements' methods back, shattered meridians' body refining, and the rest's gifts. First in the test
 * list: loading it turns the roll on joining off, so every test's mock players are born with a plain Mortal Body.
 */
public class PhysiqueGameTests implements FabricGameTest {
	static {
		Physiques.rollOnJoin = false;
	}

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		return player;
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1e-6 * Math.max(1, Math.abs(b));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void rollsByWeightAndRootsKeepKnownMethods(GameTestHelper helper) {
		RandomSource random = RandomSource.create(42);
		Map<Physique, Integer> counts = new EnumMap<>(Physique.class);
		for (int i = 0; i < 40_000; i++) counts.merge(Physique.roll(random), 1, Integer::sum);
		for (Physique physique : Physique.values()) helper.assertTrue(counts.getOrDefault(physique, 0) > 0, physique.getId() + " is born sometimes");
		helper.assertTrue(counts.get(Physique.FIVE_ELEMENT_ROOTS) > counts.get(Physique.CURSED_MERIDIANS)
				&& counts.get(Physique.CURSED_MERIDIANS) > counts.get(Physique.TRUE_ELEMENT_ROOT), "Common bodies are common, rarer grades rarer");
		helper.assertTrue(counts.get(Physique.HEAVENLY_THUNDER_BODY) < counts.get(Physique.FALLEN_TRUE_IMMORTAL), "The Heavenly Thunder Body rarer than a Fallen True Immortal");
		helper.assertTrue(counts.get(Physique.ETERNAL_REINCARNATION_BODY) < counts.get(Physique.HEAVENLY_THUNDER_BODY)
				&& counts.get(Physique.MYRIAD_FORM_BODY) < counts.get(Physique.HEAVENLY_THUNDER_BODY), "The Eternal Reincarnation and Myriad Form Bodies the rarest");

		EnumSet<QiElement> three = Physique.rollRoots(Physique.THREE_ELEMENT_ROOTS, List.of(QiElement.FIRE), random);
		helper.assertTrue(three.size() == 3 && three.contains(QiElement.FIRE), "Three roots, the element of a method known among them");
		EnumSet<QiElement> one = Physique.rollRoots(Physique.TRUE_ELEMENT_ROOT, List.of(QiElement.WATER, QiElement.FIRE), random);
		helper.assertTrue(one.equals(EnumSet.of(QiElement.WATER)), "One true root: the first method known's element");
		helper.assertTrue(Physique.rollRoots(Physique.FIVE_ELEMENT_ROOTS, List.of(), random).isEmpty(), "Five roots: no limit to keep");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void physiquesShapeTheQiAndThePace(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		double max = c.maxQi(), gather = c.qiGatherPerSecond(), pace = c.meditationCultivationPerSecond(0);
		double ghost = GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY);
		helper.assertTrue(c.getPhysique() == null && c.physique() == Physique.FIVE_ELEMENT_ROOTS, "Unrolled: the plain body, Five Element Spiritual Roots");
		// The Mortal Body is gone (2026-10-19): a save that names it is read as the same plain body.
		net.minecraft.nbt.CompoundTag old = c.save();
		old.putString("Physique", "mortal_body");
		helper.assertTrue(PlayerCultivation.load(old).getPhysique() == Physique.FIVE_ELEMENT_ROOTS && Physique.byId("mortal_body") == Physique.FIVE_ELEMENT_ROOTS,
				"An old Mortal Body is Five Element Spiritual Roots now");
		double total = 0;
		for (Physique physique : Physique.values()) total += physique.weight();
		helper.assertTrue(Math.abs(total - 100) < 1.0e-9 && Physique.FIVE_ELEMENT_ROOTS.weight() == 40, "Still out of a hundred, the plain body 40 of them");
		c.setPhysique(Physique.BLESSED_MERIDIANS);
		helper.assertTrue(near(c.maxQi(), max * 1.5) && near(c.qiGatherPerSecond(), gather * 1.5), "Blessed meridians: 1.5 times the pool and the gathering");
		c.setPhysique(Physique.CURSED_MERIDIANS);
		helper.assertTrue(near(c.maxQi(), max * 0.8) && near(c.qiGatherPerSecond(), gather * 0.8), "Cursed meridians: 0.8 times");
		double[][] paces = {{Physique.THREE_ELEMENT_ROOTS.ordinal(), 1.5}, {Physique.TRUE_ELEMENT_ROOT.ordinal(), 2.0},
				{Physique.FALLEN_TRUE_IMMORTAL.ordinal(), 5.0}, {Physique.HEAVEN_SACRED_BODY.ordinal(), 1.2}, {Physique.FIVE_ELEMENT_ROOTS.ordinal(), 1.0}};
		// Each with its grade's small edge on top (Physique.Grade#speed: the user's pick, 2026-10-22).
		for (double[] row : paces) {
			Physique physique = Physique.byIndex((int) row[0]);
			c.setPhysique(physique);
			helper.assertTrue(near(c.meditationCultivationPerSecond(0), pace * row[1] * physique.grade().speed()), physique.getId() + ": the heaven path x" + row[1]);
		}
		c.setPhysique(Physique.GHOST_DEVIL_BODY);
		double immortal = Physique.Grade.IMMORTAL.speed();
		helper.assertTrue(near(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY), ghost * 2 * immortal), "A Ghost Devil Body: the ghost path twice as fast");
		helper.assertTrue(near(c.meditationCultivationPerSecond(0), pace * immortal), "...and the heaven path as ever (but for its grade's edge)");
		c.setPhysique(Physique.FALLEN_TRUE_IMMORTAL);
		helper.assertTrue(near(GhostMethods.ratePerSecond(c, GhostQi.SOUL_SAND_VALLEY), ghost * 5 * immortal), "A Fallen True Immortal: both paths five times");

		PlayerCultivation saved = PlayerCultivation.load(c.save());
		helper.assertTrue(saved.getPhysique() == Physique.FALLEN_TRUE_IMMORTAL, "The physique is saved");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void rootsHoldOtherElementsBack(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		PlayerCultivation c = CultivationManager.get(player);
		c.awaken(0);
		c.setPhysique(Physique.TRUE_ELEMENT_ROOT);
		c.setRootElements(List.of(QiElement.FIRE));
		HeavenMethod water = HeavenMethod.of(QiElement.WATER, Realm.QI_REFINING);
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING);
		helper.assertTrue(HeavenMethods.refusal(c, water) != null && !HeavenMethods.learn(player, water), "A fire root takes no water method");
		helper.assertTrue(HeavenMethods.refusal(c, fire) == null && HeavenMethods.learn(player, fire), "...but takes fire's");
		PlayerCultivation saved = PlayerCultivation.load(c.save());
		helper.assertTrue(saved.getRootElements().equals(EnumSet.of(QiElement.FIRE)), "The roots are saved");
		c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		c.setRootElements(List.of());
		helper.assertTrue(HeavenMethods.learn(player, water), "Five elements' roots take any element");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void shatteredMeridiansRefineTheBody(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		PlayerCultivation c = CultivationManager.get(player);
		c.setPhysique(Physique.SHATTERED_MERIDIANS);
		c.setMortalState(MortalStage.PEAK, 0);
		helper.assertTrue(c.canTemper() && HeavenMethods.refusal(c) != null, "Shattered: no methods, but Peak is no end to tempering");
		helper.assertTrue(BodyRefining.meditationPerSecond(c) > 0, "Meditation tempers it");

		ItemStack elixir = PillItem.create(ModItems.MARROW_CLEANSING_ELIXIR, PillGrade.LOW, 1);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, elixir);
		elixir.finishUsingItem(player.level(), player);
		helper.assertTrue(c.isMortal() && !elixir.isEmpty(), "No elixir opens shattered meridians");

		Tempering.gain(player, BodyRefining.toNext(0));
		TwoPaths.Body body = TwoPaths.body(c, false);
		helper.assertTrue(c.getBodyRefining() == 1 && body.realm() == Realm.QI_REFINING && body.stage() == Stage.MID,
				"Copper Skin: a body as strong as Qi Refining Mid's");
		CultivationManager.refresh(player);
		helper.assertTrue(near(player.getMaxHealth(), 20 + CultivationStats.maxHealth(Realm.QI_REFINING, Stage.MID)), "...in its health too");

		Tempering.gain(player, 1e7);
		body = TwoPaths.body(c, false);
		helper.assertTrue(c.getBodyRefining() == BodyRefining.LAST && body.realm() == Realm.CORE_FORMATION && body.stage() == Stage.EARLY,
				"The Indestructible Body: Core Formation Early's strength");
		helper.assertTrue(!c.canTemper() && !Tempering.gain(player, 10), "Nothing past the last refining");
		PlayerCultivation saved = PlayerCultivation.load(c.save());
		helper.assertTrue(saved.getBodyRefining() == BodyRefining.LAST && saved.hasShatteredMeridians(), "Body refining is saved");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void bodiesWithGifts(GameTestHelper helper) {
		// Yin Soul Body: the Netherworld's yin never takes hold.
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		YinCorruption.step(c, true, false, 0, false, 0);
		helper.assertTrue(c.netherworld().yinCorruption() > 0, "The yin seeps into an ordinary body");
		c.netherworld().setYinCorruption(0);
		c.setPhysique(Physique.YIN_SOUL_BODY);
		YinCorruption.step(c, true, false, 0, false, 0);
		helper.assertTrue(c.netherworld().yinCorruption() == 0, "...never into a Yin Soul Body");

		// Heaven Sacred Body: no Ritual to the Heavens.
		PlayerCultivation mid = new PlayerCultivation();
		mid.setState(Realm.QI_REFINING, Stage.MID, PlayerCultivation.required(Realm.QI_REFINING, Stage.MID));
		helper.assertTrue(mid.needsHeavensLeave(), "Qi Refining Mid wants the heavens' leave");
		mid.setPhysique(Physique.HEAVEN_SACRED_BODY);
		helper.assertTrue(!mid.needsHeavensLeave() && !mid.isAtRitualGate(), "...a Heaven Sacred Body has it from birth");

		// Jade Poison Body: pill qi refined at once, no toxicity.
		PlayerCultivation jade = new PlayerCultivation();
		jade.awaken(0);
		jade.setPhysique(Physique.JADE_POISON_BODY);
		jade.takeMedicine(10);
		helper.assertTrue(jade.getMedicinalQi() == 0 && near(jade.getCultivation(), 10) && jade.getPillResistance() == 0,
				"A Jade Poison Body refines a pill's qi the moment it is taken, and no toxicity builds");

		// Innate Alchemy Body: its pills twice as strong.
		ItemStack pill = PillItem.create(ModItems.QI_GATHERING_PILL, PillGrade.MID, 100);
		double potency = PillItem.potency(pill);
		pill.getOrCreateTag().putBoolean(PillItem.ALCHEMY_BODY_TAG, true);
		helper.assertTrue(near(PillItem.potency(pill), potency * 2), "A pill an Innate Alchemy Body brewed works twice as strongly");

		// Innate Consciousness Body: a Four Axis domain at once.
		PlayerCultivation sense = new PlayerCultivation();
		sense.awaken(0);
		helper.assertTrue(ConsciousnessDomainHandler.radius(sense) < 20, "A Qi Refining domain is small");
		sense.setPhysique(Physique.INNATE_CONSCIOUSNESS_BODY);
		helper.assertTrue(ConsciousnessDomainHandler.radius(sense) == ConsciousnessDomainHandler.MAX_RADIUS, "...an Innate Consciousness Body's a Four Axis one");

		// Blessed meridians: Qi Sense from a mortal's first day.
		PlayerCultivation mortal = new PlayerCultivation();
		helper.assertTrue(mortal.isMortal() && !Ability.QI_SENSE.isUnlocked(mortal), "A mortal has no Qi Sense");
		mortal.setPhysique(Physique.BLESSED_MERIDIANS);
		helper.assertTrue(Ability.QI_SENSE.isUnlocked(mortal), "...one with blessed meridians does");

		// Ghost Devil Body: a Ghost King's form from birth, a Ghost King in death.
		PlayerCultivation devil = new PlayerCultivation();
		devil.awaken(0);
		helper.assertTrue(!TwoPaths.hasKingForm(devil), "A Qi Refining cultivator has no Ghost King's form");
		devil.setPhysique(Physique.GHOST_DEVIL_BODY);
		helper.assertTrue(TwoPaths.hasKingForm(devil) && TwoPaths.formsOpen(devil), "...a Ghost Devil Body has it from birth");
		devil.netherworld().setGhost(true);
		helper.assertTrue(GhostSystem.isKing(devil), "...and dead, is a Ghost King");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void ghostDevilLeapsToADeepMethod(GameTestHelper helper) {
		ServerPlayer plain = player(helper);
		CultivationManager.get(plain).awaken(0);
		GhostMethods.give(plain, GhostMethod.GHOST_CORE_CANON);
		helper.assertTrue(CultivationManager.get(plain).netherworld().layer(GhostMethod.GHOST_CORE_CANON) == 2
				&& TwoPaths.ghostRank(CultivationManager.get(plain), false) == 0, "An ordinary body begins the ghost path at Qi Refining Early, the foundation as far");

		ServerPlayer devil = player(helper);
		PlayerCultivation c = CultivationManager.get(devil);
		c.awaken(0);
		c.setPhysique(Physique.GHOST_DEVIL_BODY);
		GhostMethods.give(devil, GhostMethod.GHOST_CORE_CANON);
		helper.assertTrue(TwoPaths.ghostRank(c, false) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY),
				"A Ghost Devil Body finding a Core Formation foundation: its ghost path leaps to Core Formation Early");
		helper.succeed();
	}
}
