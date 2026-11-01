package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The methods overhaul, phase 2 (CultivationMethod, CultivationMethods; the user's design of 2026-10-31): one method a path at a time,
 * switching costing the path's stages in its realm and never the realm; Method Manuals and who may read them; talent weighing a method's
 * years by its weight and share; the methods that gather their own way (the rites, lightning, a body's yin, kin blood, cores) and those
 * that gather by meditation only somewhere (deep seclusion, a territory); the gathering formations, which only speed.
 */
public class MethodsPhaseTwoGameTests implements FabricGameTest {
	private static final TwoPaths.Path HEAVEN = TwoPaths.Path.HEAVEN, GHOST = TwoPaths.Path.GHOST, DEMON = TwoPaths.Path.DEMON;

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	/** {@code message}'s translation key, or "" if it is none. */
	private static String key(Component message) {
		return message != null && message.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMethodsWeightAndShareSetItsYears(GameTestHelper helper) {
		PlayerCultivation low = new PlayerCultivation(), immortal = new PlayerCultivation();
		low.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		immortal.setPhysique(Physique.FALLEN_TRUE_IMMORTAL);
		Realm cf = Realm.CORE_FORMATION;
		double early = Lifespan.stageSeclusion(cf, Stage.EARLY);
		helper.assertTrue(near(Lifespan.seclusionYears(immortal, cf, Stage.EARLY, 1, 1, CultivationMethod.QI_CIRCULATION), early / 64)
						&& near(Lifespan.seclusionYears(immortal, cf, Stage.EARLY, 1, 1, CultivationMethod.TURTLE_BREATH), early / 64 / 3),
				"Full weight: the whole talent divides the years (x64); Turtle Breath a third of them besides");
		helper.assertTrue(near(Lifespan.seclusionYears(immortal, cf, Stage.EARLY, 1, 1, CultivationMethod.CORPSE_YIN), early / 8 / 12)
						&& near(Lifespan.seclusionYears(low, cf, Stage.EARLY, 1, 1, CultivationMethod.CORPSE_YIN), early / 12),
				"Half weight: the talent's square root (x8 for x64), and a twelfth");
		helper.assertTrue(near(Lifespan.seclusionYears(immortal, cf, Stage.EARLY, 1, 1, CultivationMethod.HEAVEN_RITES), early / 16)
						&& near(Lifespan.seclusionYears(low, cf, Stage.EARLY, 1, 1, CultivationMethod.HEAVEN_RITES), early / 16),
				"No weight: the rites cost a genius what they cost anyone, a sixteenth");
		low.setMeditationSpeed(3);
		helper.assertTrue(near(Lifespan.methodYears(low, cf, Stage.EARLY, 0.5, CultivationMethod.KIN_BLOOD), early / 2 / 12)
						&& near(Lifespan.seclusionYears(low, cf, Stage.EARLY, 0.5, 2, CultivationMethod.KIN_BLOOD), early / 12),
				"A gain of its own way: no pace to price it, no aid to save it; meditation's priced (x4 at x3) and saved (x2)");
		for (CultivationMethod method : CultivationMethod.values()) {
			if (method.isOrthodox()) helper.assertTrue(method.weight() == CultivationMethod.Weight.FULL && method.yearsFactor() == 1.0,
					method.getId() + ": the orthodox ways' years as before (the user's lines of 2026-10-27 kept)");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void switchingCostsThePathsStagesNeverItsRealm(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player);
		c.awaken(0);
		try {
			TestMethods.setAt(c, Realm.FOUNDATION_BUILDING, Stage.LATE);
			c.setState(Realm.FOUNDATION_BUILDING, Stage.LATE, 40);
			helper.assertTrue(CultivationMethods.practised(c, HEAVEN) == CultivationMethod.QI_CIRCULATION, "Qi Circulation, the heaven path's own, till another is chosen");
			helper.assertTrue(!CultivationMethods.practise(player, CultivationMethod.TURTLE_BREATH) && c.getStage() == Stage.LATE, "A method not known isn't practised");
			helper.assertTrue(CultivationMethods.learn(player, CultivationMethod.TURTLE_BREATH, Realm.CORE_FORMATION), "Its manual read");
			helper.assertTrue(c.methodReach(CultivationMethod.TURTLE_BREATH) == Realm.CORE_FORMATION.ordinal()
							&& CultivationMethods.hasVolume(c, CultivationMethod.TURTLE_BREATH, Realm.FOUNDATION_BUILDING)
							&& CultivationMethods.hasVolume(c, CultivationMethod.TURTLE_BREATH, Realm.CORE_FORMATION)
							&& !CultivationMethods.hasVolume(c, CultivationMethod.TURTLE_BREATH, Realm.NASCENT_SOUL),
					"Its volumes from Foundation Building to the manual's Core Formation, no further");
			helper.assertTrue(!CultivationMethods.learn(player, CultivationMethod.TURTLE_BREATH, Realm.FOUNDATION_BUILDING), "A lesser manual of it teaches nothing new");
			helper.assertTrue(CultivationMethods.practise(player, CultivationMethod.TURTLE_BREATH) && c.getRealm() == Realm.FOUNDATION_BUILDING
							&& c.getStage() == Stage.EARLY && c.getCultivation() == 0 && CultivationMethods.practised(c, HEAVEN) == CultivationMethod.TURTLE_BREATH,
					"Switched: the path re-laid at Foundation Building Early, its cultivation there lost, its realm kept");
			helper.assertTrue(!CultivationMethods.practise(player, CultivationMethod.TURTLE_BREATH), "Practised already: nothing changes");
			c.setState(Realm.FOUNDATION_BUILDING, Stage.MID, 25);
			helper.assertTrue(CultivationMethods.practise(player, CultivationMethod.QI_CIRCULATION) && c.getStage() == Stage.EARLY && c.getCultivation() == 0
					&& c.practising(HEAVEN) == null, "Back to Qi Circulation: the same price");
			// At Four Axis the axes are the method: choosing one for the realm beyond costs nothing.
			c.setState(Realm.FOUR_AXIS, Stage.MID, 7);
			helper.assertTrue(CultivationMethods.practised(c, HEAVEN) == CultivationMethod.FOUR_AXES, "At Four Axis, the axes");
			helper.assertTrue(CultivationMethods.practise(player, CultivationMethod.TURTLE_BREATH) && c.getStage() == Stage.MID && c.getCultivation() == 7
					&& CultivationMethods.chosen(c, HEAVEN) == CultivationMethod.TURTLE_BREATH, "...and a switch there costs nothing");
			// A path not begun: nothing to lose.
			helper.assertTrue(CultivationMethods.learn(player, CultivationMethod.CORPSE_YIN, Realm.HEAVENLY_BEING) && GhostMethods.ghostRank(c.netherworld()) < 0
					&& CultivationMethods.practise(player, CultivationMethod.CORPSE_YIN) && c.practising(GHOST) == CultivationMethod.CORPSE_YIN,
					"A ghost path not begun: its method chosen, nothing lost");
			helper.assertTrue(!CultivationMethods.practise(player, CultivationMethod.KIN_BLOOD), "A demon method unknown (no demon core to learn it with)");
			helper.succeed();
		} finally {
			leave(player);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void manualsTeachTheirVolumesToWhomTheyMay(GameTestHelper helper) {
		ItemStack turtle = MethodManualItem.of(CultivationMethod.TURTLE_BREATH, Realm.QI_REFINING);
		ItemStack rites = MethodManualItem.of(CultivationMethod.HEAVEN_RITES, Realm.DAO_INTEGRATION);
		helper.assertTrue(MethodManualItem.method(turtle) == CultivationMethod.TURTLE_BREATH && MethodManualItem.realm(turtle) == Realm.FOUNDATION_BUILDING
				&& MethodManualItem.realm(rites) == Realm.HEAVENLY_BEING, "Written within its method's own realms");
		helper.assertTrue(MethodManualItem.method(MethodManualItem.of(CultivationMethod.QI_CIRCULATION, Realm.CORE_FORMATION)) == null
						&& MethodManualItem.method(MethodManualItem.of(CultivationMethod.THUNDER_TEMPERING, Realm.CORE_FORMATION)) == null,
				"No manual of the orthodox ways, nor of the Thunder Body's own");
		PlayerCultivation mortal = new PlayerCultivation();
		helper.assertTrue(key(CultivationMethods.refusal(mortal, CultivationMethod.TURTLE_BREATH)).equals(ModLang.MSG_HEAVEN_METHOD_MERIDIANS), "A mortal's meridians are sealed");
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		helper.assertTrue(key(CultivationMethods.refusal(c, CultivationMethod.THUNDER_TEMPERING)).equals(ModLang.MSG_METHOD_NOT_TAUGHT)
						&& key(CultivationMethods.refusal(c, CultivationMethod.KIN_BLOOD)).equals(ModLang.MSG_METHOD_NEEDS_CORE)
						&& CultivationMethods.refusal(c, CultivationMethod.CORPSE_YIN) == null && CultivationMethods.refusal(c, CultivationMethod.HEAVEN_RITES) == null,
				"The Thunder Body's way is born, not read; a demon method wants a demon core; the living read the rest");
		c.netherworld().setGhost(true);
		helper.assertTrue(key(CultivationMethods.refusal(c, CultivationMethod.TURTLE_BREATH)).equals(ModLang.MSG_HEAVEN_METHOD_DEAD)
				&& CultivationMethods.refusal(c, CultivationMethod.CORPSE_YIN) == null, "The dead read no heaven path manual, but a ghost path's");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sectsKeepTheManualsOfTheirKind(GameTestHelper helper) {
		RandomSource random = RandomSource.create(20261031L);
		// (Phase 3 gave each kind more: MethodManualItem#kinds. The ghost path's darker ways stay the devilish sects', the blood's the clans'.)
		Set<CultivationMethod> heaven = Set.copyOf(MethodManualItem.kinds(Alignment.Faction.RIGHTEOUS, null));
		Set<CultivationMethod> devilish = Set.copyOf(MethodManualItem.kinds(Alignment.Faction.DEVILISH, null));
		Set<CultivationMethod> den = Set.copyOf(MethodManualItem.kinds(Alignment.Faction.NEUTRAL, Bloodline.TIGER));
		helper.assertTrue(heaven.contains(CultivationMethod.HEAVEN_RITES) && heaven.contains(CultivationMethod.TURTLE_BREATH)
				&& devilish.contains(CultivationMethod.CORPSE_YIN) && !devilish.contains(CultivationMethod.HEAVEN_RITES)
				&& den.contains(CultivationMethod.KIN_BLOOD) && den.stream().allMatch(m -> m.path() == TwoPaths.Path.DEMON), "Each kind keeps its own");
		int kept = 0;
		for (int i = 0; i < 600; i++) {
			boolean upper = i % 2 == 0, hall = i % 3 == 0;
			ItemStack[] stacks = {MethodManualItem.kept(Alignment.Faction.RIGHTEOUS, null, upper, hall, random),
					MethodManualItem.kept(Alignment.Faction.DEVILISH, null, upper, hall, random),
					MethodManualItem.kept(Alignment.Faction.NEUTRAL, Bloodline.TIGER, upper, hall, random)};
			List<Set<CultivationMethod>> kinds = List.of(heaven, devilish, den);
			for (int k = 0; k < stacks.length; k++) {
				CultivationMethod method = MethodManualItem.method(stacks[k]);
				if (method == null) continue;
				kept++;
				Realm realm = MethodManualItem.realm(stacks[k]);
				helper.assertTrue(kinds.get(k).contains(method), "A sect keeps the methods of its kind: " + method.getId());
				helper.assertTrue(realm != Realm.FOUR_AXIS && method.spans(realm), "Written to one of its own realms: " + method.getId() + " " + realm.getId());
				helper.assertTrue(upper ? realm.ordinal() >= Math.max(method.first().ordinal(), Realm.CORE_FORMATION.ordinal()) && realm.ordinal() <= Realm.SACRED_VESSEL.ordinal()
						: realm.ordinal() <= Realm.CORE_FORMATION.ordinal(), "The lower realms' to Core Formation, the Upper Realm's from it: " + realm.getId());
			}
		}
		helper.assertTrue(kept > 200 && kept < 1200, "Now and then a chest keeps one (" + kept + " of 1,800)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aMethodOfItsOwnWayGathersNothingByMeditation(GameTestHelper helper) {
		ServerPlayer player = sitter(helper);
		PlayerCultivation c = CultivationManager.get(player);
		TestMethods.setAt(c, Realm.QI_REFINING, Stage.EARLY);
		CultivationMethods.learn(player, CultivationMethod.HEAVEN_RITES, Realm.QI_REFINING);
		CultivationMethods.practise(player, CultivationMethod.HEAVEN_RITES);
		helper.assertTrue(key(CultivationMethods.whyNoGain(player, c, HEAVEN)).equals(ModLang.MSG_METHOD_NOT_BY_MEDITATION),
				"The rites gather at an altar, not by meditation");
		MeditationManager.start(player);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(c.getCultivation() == 0, "...so meditation gathers nothing (" + c.getCultivation() + ")");
			CultivationMethods.practise(player, CultivationMethod.QI_CIRCULATION);
		});
		helper.runAfterDelay(80, () -> {
			try {
				helper.assertTrue(c.getCultivation() > 0, "Back on Qi Circulation, it gathers");
				helper.succeed();
			} finally {
				leave(player);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void turtleBreathGathersOnlyShutAway(GameTestHelper helper) {
		ServerPlayer player = sitter(helper);
		PlayerCultivation c = CultivationManager.get(player);
		TestMethods.setAt(c, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		CultivationMethods.learn(player, CultivationMethod.TURTLE_BREATH, Realm.CORE_FORMATION);
		CultivationMethods.practise(player, CultivationMethod.TURTLE_BREATH);
		helper.assertTrue(key(CultivationMethods.whyNoGain(player, c, HEAVEN)).equals(ModLang.MSG_TURTLE_NOT_SECLUDED), "Under the open sky: nothing");
		// A cell of stone about it, roofed three blocks over its eyes (it stands at 1 2 1).
		List<BlockPos> shell = new ArrayList<>();
		for (int x = 0; x <= 2; x++) {
			for (int y = 1; y <= 5; y++) {
				for (int z = 0; z <= 2; z++) {
					if (x == 1 && z == 1 && y >= 2 && y <= 4) continue;
					BlockPos pos = new BlockPos(x, y, z);
					helper.setBlock(pos, Blocks.STONE);
					shell.add(pos);
				}
			}
		}
		helper.runAfterDelay(30, () -> {
			try {
				helper.assertTrue(TurtleBreath.secluded(player) && CultivationMethods.whyNoGain(player, c, HEAVEN) == null,
						"Walled and roofed in stone, no light of the sky at its eyes: it gathers");
				helper.succeed();
			} finally {
				for (BlockPos pos : shell) helper.setBlock(pos, Blocks.AIR);
				leave(player);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void aFormationBurnsStonesAndOnlySpeeds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(3, 2, 3), abs = helper.absolutePos(at);
		helper.setBlock(at, ModBlocks.gatheringFormation(GatheringFormations.Kind.QI, GatheringFormations.Tier.HIGH));
		GatheringFormationBlockEntity formation = (GatheringFormationBlockEntity) level.getBlockEntity(abs);
		helper.assertTrue(formation != null && GatheringFormations.factor(level, abs, GatheringFormations.Kind.QI) == 1.0, "Unfed, it is dark: nothing");
		formation.addFuel(30);
		helper.assertTrue(level.getBlockState(abs).getValue(GatheringFormationBlock.LIT)
						&& GatheringFormations.factor(level, abs.offset(10, 0, 0), GatheringFormations.Kind.QI) == 2.0
						&& GatheringFormations.factor(level, abs.offset(20, 0, 0), GatheringFormations.Kind.QI) == 1.0
						&& GatheringFormations.factor(level, abs, GatheringFormations.Kind.YIN) == 1.0,
				"Fed, it lights: the heavens' qi twice as thick within 16 blocks, no further, and no yin");
		List<ServerPlayer> made = new ArrayList<>();
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		ServerPlayer p = survivor(helper, made, "formation-sitter");
		p.getAbilities().invulnerable = true;
		PlayerCultivation c = CultivationManager.own(p);
		c.awaken(0);
		c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		TestMethods.setAt(c, Realm.FOUNDATION_BUILDING, Stage.EARLY); // (Core Formation floats as it meditates)
		c.setAge(20);
		double[] seen = new double[4];
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(!level.getBlockState(abs).getValue(GatheringFormationBlock.LIT) && formation.fuel() == 0
					&& GatheringFormations.factor(level, abs, GatheringFormations.Kind.QI) == 1.0, "Its stones burnt out: dark again");
			MeditationManager.start(p);
		});
		helper.runAfterDelay(45, () -> {
			seen[0] = c.getCultivation();
			seen[1] = c.age();
		});
		helper.runAfterDelay(85, () -> {
			seen[2] = c.getCultivation();
			seen[3] = c.age();
			formation.addFuel(4_000);
		});
		helper.runAfterDelay(125, () -> {
			try {
				double dark = seen[2] - seen[0], lit = c.getCultivation() - seen[2];
				double darkYears = (seen[3] - seen[1]) / dark, litYears = (c.age() - seen[3]) / lit;
				helper.assertTrue(dark > 0 && lit > 1.6 * dark, "Lit, Qi Circulation gathers about twice as fast (" + dark + " then " + lit + ")");
				helper.assertTrue(Math.abs(litYears / darkYears - 1) < 0.1, "...and each gain costs the years it did (" + darkYears + ", " + litYears + " a unit)");
				helper.succeed();
			} finally {
				helper.setBlock(at, Blocks.AIR);
				MeditationManager.forget(p.getUUID());
				leave(made);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodysYinIsDrawnOnceAtAPrice(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		BlockPos at = null;
		CultivatorNpc npc = null;
		Corpses.inTests = true;
		try {
			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID));
			BlockPos fell = npc.blockPosition();
			npc.kill();
			at = bodyNear(helper.getLevel(), fell);
			helper.assertTrue(at != null, "A Nascent Soul's body");
			CorpseBlockEntity body = (CorpseBlockEntity) helper.getLevel().getBlockEntity(at);
			ServerPlayer p = survivor(helper, made, "yin-drawer");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
			GhostMethods.give(p, GhostMethod.NETHERWORLD_BREATHING);
			NetherworldState s = c.netherworld();
			helper.assertTrue(GhostMethods.ghostRank(s) == 0, "The living's ghost path begun at Qi Refining Early");
			Corpses.use(p, InteractionHand.MAIN_HAND, body, helper.getLevel().getBlockState(at));
			helper.assertTrue(!body.yinDrawn() && s.pathCultivation() == 0, "Not practising Corpse Yin Refining: an empty hand only asks whose it was");
			CultivationMethods.learn(p, CultivationMethod.CORPSE_YIN, Realm.HEAVENLY_BEING);
			CultivationMethods.practise(p, CultivationMethod.CORPSE_YIN);
			int alignment = c.getAlignment();
			double age = c.age(), corruption = s.yinCorruption();
			double share = CorpseYin.share(body.rank(), 0, CorpseBlock.Rot.FRESH);
			helper.assertTrue(near(share, CorpseYin.MOST), "A fresh body three realms above: as much as a body gives, half a stage");
			Corpses.use(p, InteractionHand.MAIN_HAND, body, helper.getLevel().getBlockState(at));
			helper.assertTrue(body.yinDrawn() && near(s.pathCultivation(), share * GhostMethods.required(0)), "Its yin drawn into the ghost path (" + s.pathCultivation() + ")");
			helper.assertTrue(c.getAlignment() == alignment - CorpseYin.SHIFT && near(s.yinCorruption(), corruption + CorpseYin.CORRUPTION),
					"...the path darkened, the living body filled with yin");
			helper.assertTrue(near(c.age() - age, Lifespan.methodYears(c, Realm.QI_REFINING, Stage.EARLY, share, CultivationMethod.CORPSE_YIN)),
					"...its years a twelfth of meditation's (" + (c.age() - age) + ")");
			double after = s.pathCultivation();
			Corpses.use(p, InteractionHand.MAIN_HAND, body, helper.getLevel().getBlockState(at));
			helper.assertTrue(s.pathCultivation() == after, "Once a body");
			helper.succeed();
		} finally {
			Corpses.inTests = false;
			takeUp(helper.getLevel(), at);
			if (npc != null) npc.discard();
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void kinBloodAndCoresFeedTheDemonPath(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "blood-hoarder");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
			c.demon().setBloodline(Bloodline.FOX);
			c.demon().set(0, 0);
			c.demon().setBloodRealm(Realm.QI_REFINING.ordinal());
			helper.assertTrue(!DemonBlood.drink(p, Bloodline.FOX, Realm.QI_REFINING) && c.demon().cultivation() == 0, "Blood it holds already: refused, unless it hoards");
			helper.assertTrue(CultivationMethods.learn(p, CultivationMethod.KIN_BLOOD, Realm.QI_REFINING)
					&& c.methodReach(CultivationMethod.KIN_BLOOD) == Realm.SACRED_VESSEL.ordinal(), "A demon method learned whole: its blood is its volume");
			CultivationMethods.practise(p, CultivationMethod.KIN_BLOOD);
			double age = c.age();
			helper.assertTrue(DemonBlood.drink(p, Bloodline.FOX, Realm.QI_REFINING) && near(c.demon().cultivation(), DemonBlood.KIN_SHARE * DemonPath.required(0)),
					"Hoarding: a phial of its own realm a fifth of the stage (" + c.demon().cultivation() + ")");
			helper.assertTrue(near(c.age() - age, Lifespan.methodYears(c, Realm.QI_REFINING, Stage.EARLY, DemonBlood.KIN_SHARE, CultivationMethod.KIN_BLOOD)),
					"...its years a twelfth of meditation's");
			helper.assertTrue(near(DemonBlood.kinShare(c, Realm.FOUNDATION_BUILDING), 0.4) && near(DemonBlood.kinShare(c, Realm.HEAVENLY_BEING), DemonBlood.KIN_MOST),
					"Twice as much a realm above, three quarters at most");
			// Core Devouring, from Foundation Building.
			int fb = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY);
			c.demon().set(fb, 0);
			c.demon().setBloodRealm(Realm.FOUNDATION_BUILDING.ordinal());
			CultivationMethods.learn(p, CultivationMethod.CORE_DEVOURING, Realm.FOUNDATION_BUILDING);
			QiElement own = DemonPath.element(Bloodline.FOX), foreign = own == QiElement.METAL ? QiElement.WATER : QiElement.METAL;
			helper.assertTrue(!DemonCoreItem.devour(p, DemonCoreItem.of(fb, own, null)) && c.demon().cultivation() == 0, "Not practising it: a core is no food");
			CultivationMethods.practise(p, CultivationMethod.CORE_DEVOURING);
			double required = DemonPath.required(fb);
			helper.assertTrue(DemonCoreItem.devour(p, DemonCoreItem.of(fb, own, null)) && near(c.demon().cultivation(), DemonCoreItem.SHARE * required)
					&& c.demon().taint() == 0, "A core of its realm and its own element: its share, the blood untainted");
			helper.assertTrue(DemonCoreItem.devour(p, DemonCoreItem.of(fb, foreign, null)) && near(c.demon().taint(), DemonPath.TAINT_PER_CORE),
					"A core of another element taints the blood");
			double before = c.demon().cultivation();
			DemonCoreItem.devour(p, DemonCoreItem.of(fb, own, null));
			helper.assertTrue(near(c.demon().cultivation() - before, DemonCoreItem.SHARE * required * (1 - DemonPath.TAINT_PER_CORE)), "...every gain that much smaller");
			DemonPath.cleanse(c, fb, required * DemonPath.TAINT_PER_CORE);
			helper.assertTrue(near(c.demon().taint(), DemonPath.TAINT_PER_CORE * (1 - DemonPath.CLEANSE_PER_STAGE)), "Bloodline Purification's meditation cleanses it");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTerritoryIsMarkedHeldAndLost(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<DemonBeast> challengers = new ArrayList<>();
		ServerPlayer p = null;
		try {
			p = survivor(helper, made, "territory-holder");
			p.getAbilities().invulnerable = true;
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			c.demon().setBloodline(Bloodline.TIGER);
			c.demon().set(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 0);
			c.demon().setBloodRealm(Realm.QI_REFINING.ordinal());
			CultivationMethods.learn(p, CultivationMethod.TERRITORIAL_DOMINION, Realm.QI_REFINING);
			CultivationMethods.practise(p, CultivationMethod.TERRITORIAL_DOMINION);
			Territories.mark(p);
			helper.assertTrue(!c.demon().hasTerritory(), "No phial of its own blood: nothing marked");
			p.getInventory().setItem(0, DemonBloodItem.of(Bloodline.TIGER, Realm.QI_REFINING));
			Territories.mark(p);
			helper.assertTrue(c.demon().hasTerritory() && p.getInventory().getItem(0).isEmpty() && Territories.inside(p), "Marked in its blood, the phial poured out");
			helper.assertTrue(CultivationMethods.whyNoGain(p, c, DEMON) == null, "Within it, meditation gathers");
			Vec3 home = p.position();
			p.moveTo(home.x + 40, home.y, home.z);
			helper.assertTrue(key(CultivationMethods.whyNoGain(p, c, DEMON)).equals(ModLang.MSG_TERRITORY_OUTSIDE), "Outside it, nothing");
			p.moveTo(home.x, home.y, home.z);
			Territories.challenge(p);
			challengers.addAll(helper.getLevel().getEntitiesOfClass(DemonBeast.class, new AABB(BlockPos.containing(home)).inflate(Territories.RADIUS + 8)));
			ServerPlayer holder = p;
			helper.assertTrue(challengers.stream().anyMatch(b -> b.getTarget() == holder), "A challenger comes for it");
			Territories.second(p);
			helper.assertTrue(c.demon().hasTerritory(), "Standing its ground: held");
			p.moveTo(home.x + 40, home.y, home.z);
			Territories.second(p);
			helper.assertTrue(!c.demon().hasTerritory(), "Fled while the challenger lives: lost");
			p.moveTo(home.x, home.y, home.z);
			p.getInventory().setItem(0, DemonBloodItem.of(Bloodline.TIGER, Realm.QI_REFINING));
			Territories.mark(p);
			Territories.died(p);
			helper.assertTrue(!c.demon().hasTerritory(), "Dead within it: lost");
			helper.succeed();
		} finally {
			challengers.forEach(Entity::discard);
			if (p != null) Territories.forget(p.getUUID());
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theThunderBodyTempersItselfInLightning(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "thunder-body");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			PlayerCultivation other = new PlayerCultivation();
			other.awaken(0);
			other.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
			helper.assertTrue(!CultivationMethods.knows(other, CultivationMethod.THUNDER_TEMPERING), "Another body doesn't know it");
			c.setPhysique(Physique.HEAVENLY_THUNDER_BODY);
			TestMethods.setAt(c, Realm.CORE_FORMATION, Stage.EARLY);
			helper.assertTrue(CultivationMethods.knows(c, CultivationMethod.THUNDER_TEMPERING)
					&& CultivationMethods.practised(c, HEAVEN) == CultivationMethod.QI_CIRCULATION, "The Heavenly Thunder Body is born knowing it, and practises it only if it will");
			CultivationMethods.practise(p, CultivationMethod.THUNDER_TEMPERING);
			helper.assertTrue(key(CultivationMethods.whyNoGain(p, c, HEAVEN)).equals(ModLang.MSG_METHOD_NOT_BY_MEDITATION), "Meditation itself gathers nothing on it");
			float max = p.getMaxHealth();
			p.setHealth(max);
			double age = c.age(), fraction = c.cultivationPerSecond() * ThunderTempering.STRIKE_SECONDS / c.cultivationRequired();
			ThunderTempering.strike(p, c, false);
			helper.assertTrue(Math.abs(p.getHealth() - max * (1 - ThunderTempering.HARM)) < 0.01, "A strike takes its share of the health (" + p.getHealth() + " of " + max + ")");
			helper.assertTrue(near(c.getCultivation(), fraction * c.cultivationRequired()), "...and wins what a minute of meditation would");
			helper.assertTrue(near(c.age() - age, Lifespan.methodYears(c, Realm.CORE_FORMATION, Stage.EARLY, fraction, CultivationMethod.THUNDER_TEMPERING)),
					"...its years a twelfth, by its talent's square root");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRitesWeighTheirOfferingAndTheVirtuous(GameTestHelper helper) {
		helper.assertTrue(HeavenRites.scale(Realm.QI_REFINING) == 1 && HeavenRites.scale(Realm.FOUNDATION_BUILDING) == 9
						&& HeavenRites.scale(Realm.HEAVENLY_BEING) == 6561 && HeavenRites.scale(Realm.DAO_INTEGRATION) == 6561,
				"The offering weighed down nine times a realm, to Heavenly Being");
		PlayerCultivation c = new PlayerCultivation();
		c.setAlignment(100);
		double righteous = HeavenRites.virtue(c);
		c.setAlignment(-100);
		double devilish = HeavenRites.virtue(c);
		c.setAlignment(0);
		helper.assertTrue(near(righteous, 1.5) && near(devilish, 0.5) && near(HeavenRites.virtue(c), 1), "The heavens hear the righteous more often, the devilish less");
		helper.succeed();
	}

	// --- Helpers ---

	/** A mock player (creative: it pays no years), out of every fight, standing still on a block of its own: ready to meditate. */
	private static ServerPlayer sitter(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.getAbilities().invulnerable = true; // a blow ends a meditation
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.moveTo(at.x, at.y, at.z);
		player.setOnGround(true);
		CultivationManager.get(player).awaken(0);
		return player;
	}

	/** A survival player in the player list (the helper's mock player is creative, and pays no years), remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	private static void leave(ServerPlayer player) {
		MeditationManager.forget(player.getUUID());
		Spells.forget(player.getUUID());
		HeavenMethods.forget(player.getUUID());
		GhostSystem.forget(player.getUUID());
		TwoPaths.forget(player.getUUID());
		ThunderTempering.forget(player.getUUID());
		Territories.forget(player.getUUID());
		player.server.getPlayerList().remove(player);
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) leave(p);
	}

	/** The body nearest {@code pos} within a few blocks, or null. */
	private static BlockPos bodyNear(ServerLevel level, BlockPos pos) {
		for (BlockPos p : BlockPos.withinManhattan(pos, 3, 4, 3)) if (level.getBlockState(p).is(ModBlocks.CORPSE)) return p.immutable();
		return null;
	}

	/** A body left by a test taken up whole, so nothing spills into the kept world. */
	private static void takeUp(ServerLevel level, BlockPos at) {
		if (at == null || !level.getBlockState(at).is(ModBlocks.CORPSE)) return;
		if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) body.takeAll();
		level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
		level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4)).forEach(ItemEntity::discard);
	}
}
