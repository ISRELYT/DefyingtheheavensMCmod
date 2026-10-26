package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * The ghost path, and the Ghost Kings (see GhostMethods, GhostSystem, GhostCultivatorNpc, GhostPalaces): the ghost path's gate
 * on the fire and the steps, the foundations carrying the ghost path (never the ordinary cultivation), who is a Ghost King and
 * what one may do, the kings of the Netherworld and their courts, and their palaces' thrones.
 */
public class GhostKingGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	private static int rank(PlayerCultivation c) {
		return PlayerCultivation.rank(c.getRealm(), c.getStage());
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFireAndTheStepsAreUnderstoodAsFarAsTheGhostPath(GameTestHelper helper) {
		GhostMethod fire = GhostMethod.GHOST_FIRE_SCRIPTURE;
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		NetherworldState s = c.netherworld();
		helper.assertTrue(GhostMethods.ghostRank(s) == -1 && GhostMethods.understoodTo(s, fire) == 3,
				"A Four Axis cultivator with no ghost path: three layers of the fire, its ordinary realm counts for nothing");
		s.setMethod(GhostMethod.NETHERWORLD_BREATHING, 1, 0);
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
		helper.assertTrue(GhostMethods.understoodTo(s, fire) == 3 && GhostMethods.opensAt(fire, 4) == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY),
				"The ghost path at Qi Refining: three, the fourth from its Foundation Building");
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
		helper.assertTrue(GhostMethods.understoodTo(s, fire) == 6 && GhostMethods.opensAt(fire, 7) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY),
				"At its Foundation Building: six, the seventh from its Core Formation");
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		helper.assertTrue(GhostMethods.understoodTo(s, fire) == 9, "At its Core Formation: all nine");
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION));
		helper.assertTrue(GhostMethods.understoodTo(s, GhostMethod.SIX_PATHS_CANON) == 54 && GhostMethods.understoodTo(s, GhostMethod.NETHERWORLD_BREATHING) == 9,
				"A foundation understood as far as the path goes, no further than its own last: the last 54, the Breathing Art 9");
		PlayerCultivation ghost = new PlayerCultivation();
		ghost.netherworld().setGhost(true);
		helper.assertTrue(GhostMethods.understoodTo(ghost.netherworld(), fire) == 3, "The dead are held by the ghost path too");

		// A layer behind the path settles in, as a heaven method's does: half its cost, never past the path.
		NetherworldState t = new NetherworldState();
		t.setMethod(fire, 3, 0);
		t.setPath(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), 0);
		helper.assertTrue(!GhostMethods.settling(t, fire) && GhostMethods.settle(t, fire, 100_000, GhostMethods.understoodTo(t, fire)) == 0
				&& t.layer(fire) == 3 && t.progress(fire) == 0, "At Qi Refining, the third is as far as it goes: nothing settles");
		t.setPath(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0);
		helper.assertTrue(GhostMethods.settling(t, fire) && GhostMethods.settleCost(fire, 4) == GhostMethods.SETTLE_SHARE * GhostMethods.layerCost(fire, 4),
				"The path at Foundation Building: the fourth settles in, on half its cost");
		helper.assertTrue(GhostMethods.settle(t, fire, GhostMethods.settleCost(fire, 4), 6) == 1 && t.layer(fire) == 4, "...and on it goes, to the fourth");
		// The rate goes by the ghost path, not the ordinary realm.
		PlayerCultivation low = new PlayerCultivation(), high = new PlayerCultivation();
		high.setState(Realm.FOUR_AXIS, Stage.EARLY, 0);
		helper.assertTrue(Math.abs(GhostMethods.ratePerSecond(low, GhostQi.NETHERWORLD) - GhostMethods.ratePerSecond(high, GhostQi.NETHERWORLD)) < 1.0e-9,
				"A mortal and a Four Axis cultivator cultivate the ghost path alike");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFoundationsCarryTheGhostPathAndNeverTheOrdinaryCultivation(GameTestHelper helper) {
		helper.assertTrue(MethodLayers.stageAt(1) == Stage.EARLY && MethodLayers.stageAt(2) == Stage.EARLY && MethodLayers.stageAt(3) == Stage.MID
				&& MethodLayers.stageAt(6) == Stage.LATE && MethodLayers.stageAt(9) == Stage.GRAND_PERFECTION,
				"In each realm's nine: Early at the first layer, Mid at the third, Late at the sixth, Grand Perfection at the ninth");
		helper.assertTrue(GhostMethods.floor(GhostMethod.NETHERWORLD_BREATHING, 9) == PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION)
				&& GhostMethods.floor(GhostMethod.SIX_PATHS_CANON, 9) == PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION)
				&& GhostMethods.floor(GhostMethod.SIX_PATHS_CANON, 10) == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY)
				&& GhostMethods.floor(GhostMethod.SIX_PATHS_CANON, 54) == PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)
				&& GhostMethods.floor(GhostMethod.GHOST_FIRE_SCRIPTURE, 9) == -1,
				"The Breathing Art ends at Qi Refining's Grand Perfection; the last foundation goes from Qi Refining to Four Axis's; the fire carries nothing");
		// Measured against the deepest foundation there is (True Immortal's since 2026-10-22, the Six Paths Canon's before; none teaches the
		// ladder past it, phase 3).
		GhostMethod deepest = GhostMethod.foundationOf(Realm.LAST_TAUGHT);
		for (GhostMethod method : GhostMethod.values()) {
			if (!method.isFoundation()) continue;
			helper.assertTrue(method.layers() == 9 * method.tier(), method.getId() + ": nine layers a realm, Qi Refining to its own");
			for (int layer = 1; layer <= method.layers(); layer++) {
				helper.assertTrue(GhostMethods.floor(method, layer) == GhostMethods.floor(deepest, layer)
						&& GhostMethods.layerCost(method, layer) == GhostMethods.layerCost(deepest, layer),
						method.getId() + "'s layer " + layer + " stands and costs as every foundation's does");
			}
			if (method.next() != null) helper.assertTrue(GhostMethods.floor(method.next(), method.layers() + 1) > GhostMethods.floor(method, method.layers()),
					method.getId() + " ends where " + method.next().getId() + " goes on");
		}
		// The ghost path's own bar (2026-10-19): every stage takes longer than the one before, the way to Four Axis Grand Perfection about
		// as long as it was (the realms after it go on in the same steps, 2026-10-22).
		double all = 0, last = 0;
		boolean rising = true;
		int axis = PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		for (int rank = 0; rank <= GhostMethods.LAST_RANK; rank++) {
			double time = GhostMethods.required(rank) / GhostMethods.realmFactor(Realm.byIndex(rank / Stage.values().length));
			rising &= time > last;
			last = time;
			if (rank <= axis) all += time;
		}
		helper.assertTrue(rising && all > 50 * 3600 && all < 56 * 3600,
				"Every stage longer than the one before; the way to Four Axis Grand Perfection about 53 h in a Soul Sand Valley (" + Math.round(all / 3600) + " h)");
		ServerPlayer ghost = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(ghost);
		NetherworldState s = c.netherworld();
		try {
			GhostSystem.makeGhost(ghost);
			GhostMethods.give(ghost, GhostMethod.NETHERWORLD_BREATHING);
			helper.assertTrue(GhostMethods.ghostRank(s) == 0 && s.layer(GhostMethod.NETHERWORLD_BREATHING) == 2,
					"The first foundation begins the ghost path at Qi Refining Early, understood as far (its two layers)");
			int stages = GhostMethods.addCultivation(s, 1.0e9);
			helper.assertTrue(stages == 3 && GhostMethods.ghostRank(s) == PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION)
					&& GhostMethods.atBottleneck(c) && s.pathCultivation() == GhostMethods.required(GhostMethods.ghostRank(s)),
					"Its stages step on by themselves, to Grand Perfection's bottleneck, the bar full (the Netherworld's tribulation's)");
			helper.assertTrue(c.getRealm() == Realm.QI_REFINING && c.getStage() == Stage.EARLY, "...and the ordinary cultivation untouched");
			helper.assertTrue(GhostMethods.heldByMethod(c) && !GhostMethods.canBreakthrough(c), "The Breathing Art opens no way into Foundation Building");
			GhostMethods.give(ghost, GhostMethod.YELLOW_SPRINGS_MARROW);
			helper.assertTrue(!GhostMethods.heldByMethod(c) && GhostMethods.canBreakthrough(c), "The Marrow Art opens it: the tribulation can be faced");
			helper.assertTrue(GhostMethods.breakthrough(s) && GhostMethods.ghostRealm(s) == Realm.FOUNDATION_BUILDING && !GhostSystem.isKing(ghost)
					&& s.pathCultivation() == 0, "Through: Foundation Building, the bar empty, no king");
			GhostMethods.give(ghost, GhostMethod.GHOST_CORE_CANON);
			GhostMethods.setPath(s, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			helper.assertTrue(GhostMethods.ghostRealm(s) == Realm.CORE_FORMATION && GhostSystem.isKing(ghost) && c.getRealm() == Realm.QI_REFINING,
					"Its Core Formation: a Ghost King by its ghost path, its ordinary cultivation still Qi Refining");
			GhostMethods.fallOneStage(s);
			helper.assertTrue(GhostMethods.ghostRank(s) == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION)
					&& s.layer(GhostMethod.GHOST_CORE_CANON) == MethodLayers.deepestAt(GhostMethods.ghostRank(s)),
					"A tribulation failed: a stage down, the layers past it with it");
		} finally {
			GhostSystem.forget(ghost.getUUID());
		}
		PlayerCultivation mortalGhost = new PlayerCultivation();
		mortalGhost.netherworld().setGhost(true);
		mortalGhost.netherworld().setMethod(GhostMethod.GHOST_CORE_CANON, 1, 0);
		GhostMethods.setPath(mortalGhost.netherworld(), PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID));
		helper.assertTrue(mortalGhost.isMortal() && !GhostSystem.isKing(mortalGhost), "A mortal's ghost path lies dormant, its meridians closed: no king by it");
		mortalGhost.awaken(0);
		helper.assertTrue(GhostSystem.isKing(mortalGhost), "Its meridians opened (a Qi Refining cultivator now), a king by its ghost path");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anyMethodLearnedIsUnderstoodAsFarAsTheGhostPath(GameTestHelper helper) {
		ServerPlayer living = cultivator(helper, Realm.NASCENT_SOUL, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(living);
		NetherworldState s = c.netherworld();
		try {
			helper.assertTrue(GhostMethods.canLearn(c, GhostMethod.SIX_PATHS_CANON) && GhostMethods.learn(living, GhostMethod.NINE_NETHER_SOUL_SUTRA)
					&& s.layer(GhostMethod.NINE_NETHER_SOUL_SUTRA) == 2 && GhostMethods.ghostRank(s) == PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY),
					"A Nascent Soul with no ghost path takes up the Nine Nether Soul Sutra: the ghost path begun at Qi Refining Early, the Sutra understood as far");
			helper.assertTrue(c.getRealm() == Realm.NASCENT_SOUL && c.getStage() == Stage.EARLY && s.activeMethod() == GhostMethod.NINE_NETHER_SOUL_SUTRA,
					"Its ordinary cultivation as it was, the Sutra practised");
			GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
			helper.assertTrue(s.layer(GhostMethod.NINE_NETHER_SOUL_SUTRA) == 11, "The path at Foundation Building Early: the Sutra to its eleventh layer");
			helper.assertTrue(GhostMethods.learn(living, GhostMethod.GHOST_CORE_CANON) && s.layer(GhostMethod.GHOST_CORE_CANON) == 11
					&& s.activeMethod() == GhostMethod.NINE_NETHER_SOUL_SUTRA,
					"The Canon taken up later is understood as far as the path (layer 11); the Sutra, going further, still practised");
			helper.assertTrue(GhostMethods.learn(living, GhostMethod.NETHERWORLD_BREATHING) && s.layer(GhostMethod.NETHERWORLD_BREATHING) == 9
					&& Spell.GHOSTLY_WAIL.isKnown(c) && Ability.NETHERWORLD_VEIL.isUnlocked(c),
					"The Breathing Art, outgrown, is learned whole: perfected at once, its spells in reach");
			helper.assertTrue(GhostMethods.learn(living, GhostMethod.GHOST_FIRE_SCRIPTURE) && s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) == 6,
					"The fire too, as far as the path carries it: six layers at its Foundation Building");
			helper.assertTrue(GhostMethods.foundationLayers(c) == 11 && GhostMethods.layersKnown(c) == 17,
					"The foundations count once, at their depth (11), with the fire's layers beside them");
		} finally {
			Spells.forget(living.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostKingStrikesAsTheLivingAndWalksUnseenAbove(GameTestHelper helper) {
		// The heaven path's Core Formation crowns no ghost; the ghost path's does (the user's rule, 2026-10-19).
		ServerPlayer common = cultivator(helper, Realm.CORE_FORMATION, Stage.LATE);
		ServerPlayer king = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		CultivationManager.get(king).netherworld().setMethod(GhostMethod.GHOST_CORE_CANON, 1, 0); // its ghost path at Core Formation
		GhostMethods.setPath(CultivationManager.get(king).netherworld(), PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		ServerPlayer living = cultivator(helper, Realm.QI_REFINING, Stage.MID);
		ServerPlayer seer = cultivator(helper, Realm.HEAVENLY_BEING, Stage.EARLY);
		try {
			helper.assertTrue(!GhostSystem.isKing(king), "Alive, no king");
			GhostSystem.makeGhost(common);
			GhostSystem.makeGhost(king);
			helper.assertTrue(GhostSystem.isKing(king) && !GhostSystem.isKing(common),
					"Dead with its ghost path at Core Formation: a Ghost King; at Core Formation on the heaven path alone, not");
			PlayerCultivation mortalGhost = new PlayerCultivation();
			mortalGhost.netherworld().setGhost(true);
			helper.assertTrue(!GhostSystem.isKing(mortalGhost), "A mortal's ghost is no king");
			Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
			helper.assertTrue(GhostSystem.attack(king, zombie) && !GhostSystem.attack(common, zombie),
					"A king's hand lands on the living world; a common ghost's passes through");
			helper.assertTrue(SoulProjection.seenOnlyBySeers(king) && !SoulProjection.seenOnlyBySeers(living),
					"Out of the Netherworld the dead are seen only by those who perceive souls");
			helper.assertTrue(!SoulProjection.broadcastsTo(king, living) && SoulProjection.broadcastsTo(king, seer) && SoulProjection.broadcastsTo(king, king),
					"Hidden from a Qi Refining cultivator; a Heavenly Being sees it, as it sees itself");
			helper.assertTrue(SoulProjection.broadcastsTo(king, common), "The dead see the dead");
			// A Ghost King's human form (Form Shift, whatever its ghost path) is seen as the living are.
			CultivationManager.get(king).setAbilityEnabled(Ability.FORM_SHIFT, true);
			helper.assertTrue(TwoPaths.inHumanForm(king) && !SoulProjection.seenOnlyBySeers(king) && SoulProjection.broadcastsTo(king, living),
					"In its human form a Ghost King is seen by all");
			CultivationManager.get(king).setAbilityEnabled(Ability.FORM_SHIFT, false);
			// The dead send their souls out of their forms too (the user's call, 2026-10-19: SoulFormsGameTests).
			CultivationManager.get(king).setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
			helper.assertTrue(Ability.SOUL_PROJECTION.isUnlocked(CultivationManager.get(king)), "A Ghost King of Nascent Soul may send its soul out");
			CultivationManager.get(king).setState(Realm.QI_REFINING, Stage.EARLY, 0);
			GhostMethods.fallTo(CultivationManager.get(king).netherworld(), PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION));
			helper.assertTrue(!GhostSystem.isKing(king), "Its ghost path brought low (a ghost core detonated), a king no longer");
		} finally {
			GhostSystem.forget(common.getUUID());
			GhostSystem.forget(king.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theNetherworldsKingsAndTheirCourts(GameTestHelper helper) {
		helper.assertTrue(GhostCultivatorNpc.titleFor(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE)) == NpcTitle.WANDERING_GHOST
				&& GhostCultivatorNpc.titleFor(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY)) == NpcTitle.GHOST_ELDER
				&& GhostCultivatorNpc.titleFor(GhostSystem.KING_RANK) == NpcTitle.GHOST_KING,
				"A Wandering Ghost; from Foundation Building a Ghost Elder; from Core Formation a Ghost King");
		helper.assertTrue(GhostCultivatorNpc.WANDERER_CAP < GhostSystem.KING_RANK, "The land's wanderers come just short of a king");
		var level = helper.getLevel();
		GhostCultivatorNpc risen = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(1, 2, 1));
		risen.becomeGhost(GhostSystem.KING_RANK + 1, Alignment.randomRogue(level.getRandom()), level.getRandom());
		helper.assertTrue(risen.getRole() == GhostCultivatorNpc.Role.GHOST_KING && risen.getTitle() == NpcTitle.GHOST_KING
				&& Math.abs(risen.getScale() - GhostCultivatorNpc.KING_SCALE) < 1.0e-6, "Dead at Core Formation, a crowned king, half again a man's height");
		GhostCultivatorNpc elder = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(2, 2, 1));
		elder.becomeGhost(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID), Alignment.randomRogue(level.getRandom()), level.getRandom());
		helper.assertTrue(elder.getRole() == GhostCultivatorNpc.Role.NONE && elder.getTitle() == NpcTitle.GHOST_ELDER
				&& Math.abs(elder.getScale() - 1) < 1.0e-6, "Below it, an elder, a man's size");

		BlockPos throne = helper.absolutePos(new BlockPos(3, 2, 3));
		GhostCultivatorNpc king = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(3, 2, 3));
		king.becomeKing(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 150, throne, level.getRandom());
		helper.assertTrue(king.getRank() >= GhostSystem.KING_RANK && king.keepsPost() && throne.equals(king.keeperPost()),
				"A palace's king: Core Formation at least, keeping its throne");
		GhostCultivatorNpc retainer = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(4, 2, 3));
		retainer.becomeCourtier(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), -150, helper.absolutePos(new BlockPos(4, 2, 3)), level.getRandom());
		helper.assertTrue(king.isAlly(retainer) && retainer.isAlly(king) && !king.isEnemy(retainer),
				"One court: allies, even a righteous king and a devilish retainer");
		helper.assertTrue(!king.isAlly(risen) && !risen.isAlly(king), "A king of no palace is no ally of another's court");
		helper.assertTrue(retainer.getRole().isCourt() && retainer.getTitle() == NpcTitle.GHOST_RETAINER && retainer.getRank() <= GhostCultivatorNpc.WANDERER_CAP,
				"A retainer, never a king's equal");
		helper.assertTrue(!king.getRole().isGateKeeper() && !retainer.getRole().isGateKeeper(), "Neither one of the Gate's keepers");
		HungryGhost hungry = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(5, 2, 5));
		ServerPlayer passer = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		helper.assertTrue(!king.isHostileTo(passer) && !retainer.isHostileTo(passer), "A court leaves the passer-by be");
		retainer.hurt(ModDamageTypes.ghostly(level, hungry), 1);
		helper.assertTrue(king.getTarget() == hungry && retainer.getLastHurtByMob() == hungry, "Strike one of the court, and the court turns on you");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSlainKingsThroneGoesToTheNearestRisen(GameTestHelper helper) {
		var level = helper.getLevel();
		GhostPalaces palaces = GhostPalaces.get(level);
		BlockPos near = new BlockPos(900_000, 80, 900_000), far = new BlockPos(900_000 + 2000, 80, 900_000);
		UUID first = UUID.randomUUID(), second = UUID.randomUUID();
		try {
			GhostPalaces.note(level, near, first);
			GhostPalaces.note(level, far, UUID.randomUUID());
			helper.assertTrue(GhostPalaces.claimVacant(level, near.offset(10, 0, 0), second) == null, "No empty throne while its king sits on it");
			GhostPalaces.vacate(level, near);
			GhostPalaces.vacate(level, far);
			helper.assertTrue(palaces.thrones().get(near) == null && palaces.thrones().containsKey(near), "Slain, its throne stands empty");
			helper.assertTrue(near.equals(GhostPalaces.claimVacant(level, near.offset(100, 0, 0), second)) && second.equals(palaces.thrones().get(near)),
					"The risen king takes the nearest empty throne");
			helper.assertTrue(GhostPalaces.claimVacant(level, near.offset(-(int) GhostPalaces.CLAIM_RANGE - 500, 0, 0), first) == null,
					"Nothing within its reach: it roams, a king without a court");
			GhostPalaces loaded = GhostPalaces.load(palaces.save(new net.minecraft.nbt.CompoundTag()));
			helper.assertTrue(second.equals(loaded.thrones().get(near)) && loaded.thrones().containsKey(far) && loaded.thrones().get(far) == null,
					"The thrones and their kings are saved");
		} finally {
			palaces.thrones().remove(near);
			palaces.thrones().remove(far);
			palaces.setDirty();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "ghost_kings_hungryghosthunts", timeoutTicks = 200)
	public void hungryGhostsHuntTheNetherworldsCultivatorsToo(GameTestHelper helper) {
		// With no player about: a Hungry Ghost goes for the living first, so this one stands in a batch of its own on a floor high
		// above the grid (HeavenSpellTesting#floor), where no mock player another test left about is within its 24 blocks' reach.
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 4);
		int y = HeavenSpellTesting.FLOOR_HEIGHT;
		GhostCultivatorNpc ghost = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(1, y, 1));
		ghost.becomeGhost(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), Alignment.randomRogue(helper.getLevel().getRandom()), helper.getLevel().getRandom());
		HungryGhost hungry = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(4, y, 1));
		helper.succeedWhen(() -> {
			helper.assertTrue(hungry.getTarget() instanceof CultivatorNpc target && !(target instanceof GhostCultivatorNpc keeper && keeper.getRole().isGateKeeper()),
					"The hungry ghost goes for a cultivator (" + hungry.getTarget() + ")");
			floor.remove(); // the floor taken up, the two ghosts with it
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void aWanderingGhostStrikesBackAtAHungryGhost(GameTestHelper helper) {
		var level = helper.getLevel();
		GhostCultivatorNpc wanderer = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(1, 2, 1));
		wanderer.becomeGhost(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID), 0, level.getRandom());
		HungryGhost hungry = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(2, 2, 1));
		hungry.setTarget(wanderer);
		// Its first blow struck at once (a living player from another test nearby could draw it off before it reached the ghost);
		// not in the first tick, though: a blow at tick 0 looks to vanilla's HurtByTargetGoal like no blow at all (its timestamp 0).
		helper.runAfterDelay(5, () -> hungry.doHurtTarget(wanderer));
		helper.succeedWhen(() -> helper.assertTrue(!hungry.isAlive() || hungry.getHealth() < hungry.getMaxHealth(),
				"The wandering ghost fights back (" + wanderer.getTarget() + ")"));
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void aKingsCourtStrikesBackAtAHungryGhost(GameTestHelper helper) {
		var level = helper.getLevel();
		GhostCultivatorNpc retainer = helper.spawn(ModEntities.GHOST_CULTIVATOR, new BlockPos(1, 2, 1));
		retainer.becomeCourtier(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0, helper.absolutePos(new BlockPos(1, 2, 1)), level.getRandom());
		HungryGhost hungry = helper.spawn(ModEntities.HUNGRY_GHOST, new BlockPos(2, 2, 1));
		hungry.setTarget(retainer);
		helper.runAfterDelay(5, () -> hungry.doHurtTarget(retainer)); // as above: struck early, before a player nearby can draw it off
		helper.succeedWhen(() -> helper.assertTrue(!hungry.isAlive() || hungry.getHealth() < hungry.getMaxHealth(),
				"The retainer fights back (" + retainer.getTarget() + ")"));
	}
}