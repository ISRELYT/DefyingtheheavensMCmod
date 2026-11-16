package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

/**
 * The heaven scriptures' framework (see HeavenMethod, HeavenMethods): five elements by every realm to True Immortal, each spanning Qi
 * Refining up to its own realm; understood as far as the realm at once, nothing settling (since 2026-11-16, the user's ask: no layers but
 * the Laws'); never holding back a stage, carrying no Qi Circulation volume (its own manuals do); learned by the living alone, from Art
 * Manuals; their element's lands speeding Qi Circulation; saved; known by cultivators, carved on Dao Steles whose realms the lands decide;
 * and the blocks the spells set down always taken up again.
 */
public class HeavenMethodsGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	private static Holder<Biome> biome(GameTestHelper helper, ResourceKey<Biome> key) {
		return helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thirtyMethodsFiveElementsBySixRealmsEachFromQiRefining(GameTestHelper helper) {
		helper.assertTrue(HeavenMethod.values().length == 55, "Fifty-five heaven methods (the realms after Four Axis' since 2026-10-22)");
		java.util.Set<String> ids = new java.util.HashSet<>();
		for (QiElement element : QiElement.five()) {
			for (Realm realm : Realm.values()) {
				HeavenMethod method = HeavenMethod.of(element, realm);
				if (realm.isPastTrueImmortal()) { // the ladder past it (phase 3): no method carries it, the Laws do
					helper.assertTrue(method == null, "None for " + element + " " + realm);
					continue;
				}
				helper.assertTrue(method != null && method.element() == element && method.realm() == realm, "One for " + element + " " + realm);
				helper.assertTrue(method.layers() == 9 * (realm.ordinal() + 1) && method.ownLayer(3) == method.layers() - 6,
						method.getId() + ": nine layers a realm from Qi Refining, its spells in its own");
				helper.assertTrue(HeavenMethod.byId(method.getId()) == method && ids.add(method.getId()), "Its id is its own");
			}
		}
		// The elements' spells: three of each element in Qi Refining and in Foundation Building (layers 2, 5, 8 of the realm), two in each
		// realm deeper (3 and 7), costing qi alone, each taught by every method of its element reaching its realm; and between Four Axis
		// wood's two, at its 5th, Return to the Bough (the user's ask, 2026-10-25: "a mid to late realm wood spell that lets u put
		// cultivation fruits back onto trees").
		int spells = 0;
		java.util.Map<String, Integer> perSlot = new java.util.HashMap<>();
		for (Spell spell : Spell.values()) {
			if (!spell.isHeaven()) continue;
			spells++;
			int within = MethodLayers.within(spell.layer());
			boolean early = spell.heavenRealm().ordinal() <= Realm.FOUNDATION_BUILDING.ordinal(), between = spell == Spell.RETURN_TO_THE_BOUGH;
			helper.assertTrue(MethodLayers.realmOf(spell.layer()) == spell.heavenRealm()
					&& (early ? within == 2 || within == 5 || within == 8 : within == 3 || within == 7 || between && within == 5)
					&& spell.cost() == 0 && spell.qiCost() > 0, spell.getId() + " comes at its place in its own realm, on qi");
			helper.assertTrue(spell.taughtBy(HeavenMethod.of(spell.element(), spell.heavenRealm())) && spell.taughtBy(HeavenMethod.of(spell.element(), Realm.TRUE_IMMORTAL))
					&& (spell.heavenRealm() == Realm.QI_REFINING || !spell.taughtBy(HeavenMethod.of(spell.element(), spell.heavenRealm().previous()))),
					spell.getId() + ": taught by its realm's method and deeper, never shallower");
			perSlot.merge(spell.element() + " " + spell.heavenRealm(), 1, Integer::sum);
		}
		helper.assertTrue(spells == 121, "A hundred and twenty element spells and Return to the Bough (" + spells + ")");
		for (QiElement element : QiElement.five()) {
			for (Realm realm : Realm.values()) {
				// Every realm has its spells (the realms after Four Axis had none of their own at first, 2026-10-22; all of them came
				// 2026-10-24); the ladder past True Immortal none (phase 3: the Laws strengthen the spells there are).
				int want = realm.isPastTrueImmortal() ? 0
						: (realm.ordinal() <= Realm.FOUNDATION_BUILDING.ordinal() ? 3 : 2) + (element == QiElement.WOOD && realm == Realm.FOUR_AXIS ? 1 : 0);
				helper.assertTrue(perSlot.getOrDefault(element + " " + realm, 0) == want, element + " " + realm + ": " + want + " spells");
			}
		}
		helper.succeed();
	}

	/** No layers but the Laws' (the user's ask, 2026-11-16): a scripture known is understood as far as the realm, at once, whatever is set. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aScriptureIsUnderstoodAsFarAsTheRealmAtOnce(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		HeavenMethod fourAxis = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		helper.assertTrue(HeavenMethods.refusal(c) != null && !HeavenMethods.canLearn(c), "A mortal: its meridians sealed");
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		helper.assertTrue(HeavenMethods.canLearn(c), "Opened, a cultivator takes them up");
		HeavenState s = c.heaven();
		s.setMethod(fourAxis, 1, 0);
		helper.assertTrue(s.layer(fourAxis) == 2 && HeavenMethods.understoodTo(c, fourAxis) == 2 && s.progress(fourAxis) == 0 && !HeavenMethods.settling(c, fourAxis),
				"At Qi Refining Early it is understood as far as Early, nothing to settle");
		c.setState(Realm.QI_REFINING, Stage.MID, 0);
		helper.assertTrue(s.layer(fourAxis) == 5 && !HeavenMethods.settling(c, fourAxis), "The realm at Mid: as far as Mid at once");
		s.setMethod(fourAxis, 40, 1234);
		helper.assertTrue(s.layer(fourAxis) == 5 && s.progress(fourAxis) == 0, "Whatever is set, the realm decides");
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		helper.assertTrue(s.layer(fourAxis) == 2, "A fall to Early: as far as Early");
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(s.layer(fourAxis) == 54 && HeavenMethods.perfected(s, fourAxis), "At the very last: all of it");
		c.setState(Realm.TRUE_IMMORTAL, Stage.LATE, 0);
		helper.assertTrue(s.layer(fourAxis) == 54, "...and never past its own last");
		PlayerCultivation ghost = new PlayerCultivation();
		ghost.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		ghost.netherworld().setGhost(true);
		helper.assertTrue(!HeavenMethods.canLearn(ghost) && HeavenMethods.refusal(ghost) != null, "The dead take up no heaven method");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMethodIsLearnedAsFarAsTheRealm(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		HeavenState s = c.heaven();
		try {
			HeavenMethod woodFb = HeavenMethod.of(QiElement.WOOD, Realm.FOUNDATION_BUILDING);
			helper.assertTrue(HeavenMethods.learn(player, woodFb) && s.layer(woodFb) == 18 && s.activeMethod() == woodFb,
					"The first learned: understood whole, as it ends before Core Formation, and practised");
			HeavenMethod woodNs = HeavenMethod.of(QiElement.WOOD, Realm.NASCENT_SOUL);
			helper.assertTrue(HeavenMethods.learn(player, woodNs) && s.layer(woodNs) == 20 && s.progress(woodNs) == 0 && s.activeMethod() == woodNs,
					"A deeper one: understood as far as Core Formation Early (layer 20), and it takes over the practice of its element");
			HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.CORE_FORMATION);
			helper.assertTrue(HeavenMethods.learn(player, fire) && s.layer(fire) == 20 && s.activeMethod() == woodNs,
					"Another element's, as far too; the practice stays with wood");
			helper.assertTrue(!HeavenMethods.learn(player, fire), "Known: not learned again");
			helper.assertTrue(HeavenMethods.depth(s, QiElement.WOOD) == 20 && HeavenMethods.deepestOf(s, QiElement.WOOD) == woodNs,
					"Wood's depth is its deepest method's");
			// Saved and loaded by id: understood, as ever, as far as the realm (whatever was set: nothing settles since 2026-11-16).
			s.setMethod(woodNs, 19, 7);
			HeavenState loaded = PlayerCultivation.load(c.save()).heaven();
			helper.assertTrue(loaded.layer(woodNs) == 20 && loaded.progress(woodNs) == 0 && loaded.layer(fire) == 20 && loaded.activeMethod() == woodNs
					&& loaded.known().size() == 3, "Saved and loaded");
			// An Art Manual, read, teaches its scripture whole, and isn't used up.
			HeavenMethod metal = HeavenMethod.of(QiElement.METAL, Realm.QI_REFINING);
			ItemStack manual = TechniqueManualItem.of(metal);
			helper.assertTrue(manual.is(ModItems.TECHNIQUE_MANUAL) && TechniqueManualItem.heavenArt(manual) == metal, "An Art Manual holds its scripture");
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, manual);
			manual.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			helper.assertTrue(s.knows(metal) && s.layer(metal) == 9 && Spell.STEEL_HURRICANE.isKnown(c) && !player.getMainHandItem().isEmpty(),
					"Read: its scripture learned (all nine, every art), the manual kept");
			// A jade slip of before turns into the Art Manual of its scripture.
			ItemStack old = new ItemStack(ModItems.JADE_SLIP);
			old.getOrCreateTag().putString("HeavenMethod", metal.getId());
			ItemStack now = JadeSlipItem.converted(old);
			helper.assertTrue(now.is(ModItems.TECHNIQUE_MANUAL) && TechniqueManualItem.heavenArt(now) == metal && JadeSlipItem.of(metal).is(ModItems.TECHNIQUE_MANUAL),
					"A slip of before is the Art Manual of its scripture now");
		} finally {
			Spells.forget(player.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anElementsLandsAndOldRealmsSpeedIt(GameTestHelper helper) {
		Holder<Biome> forest = biome(helper, Biomes.FOREST), desert = biome(helper, Biomes.DESERT), ocean = biome(helper, Biomes.OCEAN);
		double woodInForest = QiElement.share(forest, false, QiElement.WOOD), woodInDesert = QiElement.share(desert, false, QiElement.WOOD);
		double fireInDesert = QiElement.share(desert, false, QiElement.FIRE), waterAtSea = QiElement.share(ocean, false, QiElement.WATER);
		helper.assertTrue(woodInForest > 0.4 && woodInDesert < 0.15 && fireInDesert > 0.3 && waterAtSea > 0.4,
				"Wood thick in forests, thin in deserts; fire in deserts; water at sea (" + woodInForest + ", " + woodInDesert + ")");
		double sum = 0;
		for (QiElement element : QiElement.five()) sum += QiElement.share(forest, false, element);
		helper.assertTrue(Math.abs(sum - 1) < 1.0e-6, "The five shares make the whole");
		helper.assertTrue(near(HeavenMethods.elementFactor(0.2), 1.0) && near(HeavenMethods.elementFactor(0), 0.75) && near(HeavenMethods.elementFactor(0.4), 1.25)
				&& near(HeavenMethods.elementFactor(0.6), 1.5) && near(HeavenMethods.elementFactor(1), 1.5),
				"An even fifth: the base pace; none of it: three quarters; thick: up to half again, no more");
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		helper.assertTrue(HeavenMethods.methodFactor(c, woodInForest, false) > HeavenMethods.methodFactor(c, woodInDesert, false) * 1.3,
				"Wood cultivates a third faster or more in a forest than in a desert");
		// No scripture practised: Qi Circulation's plain pace (whether it gathers at all is its volume's: since 2026-11-16 the scriptures
		// are arts, and its volumes its own manuals').
		helper.assertTrue(near(HeavenMethods.cultivationFactor(c, null, 0.6, false), 1) && near(HeavenMethods.cultivationFactor(c, null, 0.6, true), HeavenMethods.STELE_BONUS),
				"With no scripture, the plain pace wherever one sits; by a stele of the method x1.25");
		c.setQiBoost(0.5);
		c.setGearQiBonus(0.6);
		helper.assertTrue(near(HeavenMethods.methodFactor(c, 0.2, true), 1.25 * 1.5 * 1.6),
				"By its stele x1.25, a Qi Gathering Pill's +50% and clothing's +60% on top (" + HeavenMethods.methodFactor(c, 0.2, true) + ")");
		helper.assertTrue(near(HeavenMethods.cultivationFactor(c, null, 0, false), 1.5 * 1.6), "...and with no scripture, the boosts on the plain pace");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cultivationIsItsOwnAndMethodsOpenOnlyRealms(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		helper.assertTrue(c.addCultivation(PlayerCultivation.required(Realm.QI_REFINING, Stage.EARLY)) && c.getStage() == Stage.MID && c.getCultivation() == 0,
				"No method needed: a full Early steps into Mid");
		helper.assertTrue(!c.addCultivation(10_000) && c.getStage() == Stage.MID && c.isAtRitualGate(), "...and Mid waits full for the heavens' leave");
		c.setHeavensLeave(true);
		helper.assertTrue(c.addCultivation(0) && c.getStage() == Stage.LATE, "The leave granted, a full Mid steps into Late");
		c.addCultivation(10_000);
		// The methods overhaul (2026-10-31): no scripture holds a breakthrough below True Immortal; without one, the realm ahead
		// (and this one) gathers nothing.
		helper.assertTrue(c.getStage() == Stage.GRAND_PERFECTION && c.isAtBottleneck() && !c.isHeldByMethod() && c.isMissingBreakthroughPill()
				&& !c.canBreakthrough() && !HeavenMethods.canGain(c) && !HeavenMethods.hasVolume(c, Realm.FOUNDATION_BUILDING) && HeavenMethods.gateLine(c) != null,
				"At Grand Perfection's bottleneck no method holds the breakthrough (only the pill), though none gathers here or ahead");
		HeavenState s = c.heaven();
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING);
		s.setMethod(fire, 9, 0);
		helper.assertTrue(!HeavenMethods.canGain(c) && !HeavenMethods.hasVolume(c, Realm.QI_REFINING),
				"A scripture carries no Qi Circulation volume (since 2026-11-16: its own manuals do)");
		c.raiseReach(CultivationMethod.QI_CIRCULATION, Realm.QI_REFINING);
		helper.assertTrue(HeavenMethods.canGain(c) && HeavenMethods.hasVolume(c, Realm.QI_REFINING) && !HeavenMethods.hasVolume(c, Realm.FOUNDATION_BUILDING)
				&& HeavenMethods.opensWay(c, Realm.FOUNDATION_BUILDING), "Qi Circulation's Qi Refining volume: Qi Refining gathers, not Foundation Building");
		HeavenMethod woodFb = HeavenMethod.of(QiElement.WOOD, Realm.FOUNDATION_BUILDING);
		s.setMethod(woodFb, 9, 0);
		c.raiseReach(CultivationMethod.QI_CIRCULATION, Realm.FOUNDATION_BUILDING);
		helper.assertTrue(!c.isHeldByMethod() && HeavenMethods.hasVolume(c, Realm.FOUNDATION_BUILDING) && !HeavenMethods.hasVolume(c, Realm.CORE_FORMATION)
				&& HeavenMethods.deepestRealm(s) == Realm.FOUNDATION_BUILDING && !c.canBreakthrough(),
				"Its Foundation Building volume: only the pill is wanting");
		c.prepareBreakthrough(Realm.FOUNDATION_BUILDING, 0.5);
		helper.assertTrue(c.canBreakthrough() && c.breakthrough() && c.getRealm() == Realm.FOUNDATION_BUILDING && c.getStage() == Stage.EARLY
				&& near(c.getCultivation(), PlayerCultivation.required(Realm.FOUNDATION_BUILDING, Stage.EARLY) * 0.5),
				"Through the tribulation, the pill's head start on the bar (" + c.getCultivation() + ")");
		helper.assertTrue(!HeavenMethods.settling(c, woodFb) && s.layer(woodFb) == 11 && s.layer(fire) == 9,
				"The new stage: the scripture understood through it at once, nothing to settle; the Qi Refining one whole");
		helper.assertTrue(c.addCultivation(PlayerCultivation.required(Realm.FOUNDATION_BUILDING, Stage.EARLY)) && c.getStage() == Stage.MID,
				"A minor stage never waits on a scripture");
		// A minor stage's tribulation (from Nascent Soul) waits on no method either.
		PlayerCultivation soul = new PlayerCultivation();
		soul.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		soul.heaven().setMethod(fire, 9, 0);
		soul.addCultivation(soul.cultivationRequired());
		helper.assertTrue(soul.isAtBottleneck() && !soul.isHeldByMethod() && soul.canBreakthrough(), "Nascent Soul Early's tribulation: no method wanted");
		helper.assertTrue(!HeavenMethods.canGain(soul), "...though without Qi Circulation's Nascent Soul volume it gathers nothing there");
		// A failed tribulation's fall takes the stage's cultivation; the scriptures are understood as far as the stage fallen to.
		c.fallOneStage();
		helper.assertTrue(c.getRealm() == Realm.FOUNDATION_BUILDING && c.getStage() == Stage.EARLY && c.getCultivation() == 0
				&& s.layer(woodFb) == 11 && s.layer(fire) == 9, "Fallen to Early: as far as Early (" + s.layer(woodFb) + ")");
		// A pill's qi, refined, goes onto the bar, never past the bottleneck (meditation refines it only with a volume of the realm:
		// MeditationManager, the overhaul of 2026-10-31).
		PlayerCultivation sick = new PlayerCultivation();
		sick.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		sick.setMedicinalQi(50);
		helper.assertTrue(sick.refine(10) == 10 && near(sick.getMedicinalQi(), 40), "The pill's qi refined");
		// A save from before: its cultivation as it was, its methods understood as far as the realm (deeper set back, behind brought up).
		PlayerCultivation old = new PlayerCultivation();
		old.setState(Realm.QI_REFINING, Stage.MID, 60);
		old.heaven().setMethod(fire, 7, 50);
		old.heaven().setMethod(woodFb, 1, 0);
		net.minecraft.nbt.CompoundTag tag = old.save();
		tag.putInt("PathModel", 2);
		tag.getCompound("HeavenMethods").putInt("Units", 2);
		PlayerCultivation loaded = PlayerCultivation.load(tag);
		helper.assertTrue(near(loaded.getCultivation(), 60) && loaded.getStage() == Stage.MID && loaded.heaven().layer(fire) == 5
				&& loaded.heaven().progress(fire) == 0 && loaded.heaven().layer(woodFb) == 5,
				"An old save: 60 cultivation kept, both methods at Mid's last layer (" + loaded.heaven().layer(fire) + ", " + loaded.heaven().layer(woodFb) + ")");
		// An op setting the realm leaves the methods understood as far as it; back to or below Qi Refining Mid, the ritual is asked again.
		PlayerCultivation op = new PlayerCultivation();
		op.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		op.heaven().setMethod(woodFb, 2, 0);
		op.prepareBreakthrough(Realm.FOUNDATION_BUILDING, 0.3);
		CultivationCommand.set(op, Realm.FOUNDATION_BUILDING, Stage.MID);
		helper.assertTrue(op.heaven().layer(woodFb) == 14 && op.getStage() == Stage.MID && op.getCultivation() == 0 && op.hasHeavensLeave(),
				"Set: the methods understood through the stage, past the ritual as if it had been asked");
		helper.assertTrue(op.getPreparedRealm() == null, "...and a Foundation Pill taken for a realm already reached spent");
		op.setFavour(2);
		CultivationCommand.set(op, Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(op.heaven().layer(woodFb) == 2 && op.getStage() == Stage.EARLY && !op.hasHeavensLeave() && op.getFavour() == 0,
				"Set back to Early: the methods with it, no leave, no Favour");
		CultivationCommand.set(op, Realm.QI_REFINING, Stage.MID);
		helper.assertTrue(op.needsHeavensLeave(), "...and at Mid the ritual is wanted again");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theMethodPractisedSetsThePace(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		HeavenState s = c.heaven();
		helper.assertTrue(HeavenMethods.practised(s) == null && HeavenMethods.deepestRealm(s) == null, "No method known: none practised");
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING);
		HeavenMethod woodFb = HeavenMethod.of(QiElement.WOOD, Realm.FOUNDATION_BUILDING);
		s.setMethod(fire, 2, 0);
		s.setMethod(woodFb, 2, 0);
		helper.assertTrue(HeavenMethods.practised(s) == woodFb, "None chosen: the one going furthest");
		s.setActiveMethod(fire);
		helper.assertTrue(HeavenMethods.practised(s) == fire, "Chosen: that one");
		helper.assertTrue(near(HeavenMethods.cultivationFactor(c, fire, 0.6, true), 1.5 * HeavenMethods.STELE_BONUS)
				&& near(HeavenMethods.cultivationFactor(c, fire, 0, false), 0.75), "Its element thick and its stele near: x1.875; none of it: x0.75");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 260)
	public void meditationFillsTheBarAndTheRealmOpensTheArts(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		// Out of the other tests' fights: a Hungry Ghost of a neighbouring test went for this survival player, and a blow ends a
		// meditation (CultivationEvents), so its layer never settled. Invulnerable, no mob takes it for prey and no stray blow lands.
		player.getAbilities().invulnerable = true;
		net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.moveTo(at.x, at.y, at.z);
		player.setOnGround(true);
		PlayerCultivation c = CultivationManager.get(player);
		c.awaken(0);
		c.raiseReach(CultivationMethod.QI_CIRCULATION, Realm.QI_REFINING); // Qi Circulation's first volume: the method that gathers
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING);
		HeavenState s = c.heaven();
		s.setMethod(fire, 1, 0);
		s.setActiveMethod(fire);
		Spell sleeping = Spell.FLAME_BOLT; // Early's art
		helper.assertTrue(sleeping.isKnown(c) && !Spell.FLAME_FIST.isKnown(c), "At Early, Flame Bolt, not yet Flame Fist (Mid's)");
		MeditationManager.start(player);
		helper.assertTrue(MeditationManager.isMeditating(player.getUUID()), "Sat down to meditate");
		double[] before = new double[1];
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(c.getCultivation() > 0 && s.layer(fire) == 2 && s.progress(fire) == 0,
					"Meditation fills the bar (" + c.getCultivation() + "); the scripture understood as far as Early, nothing settling");
			c.setState(Realm.QI_REFINING, Stage.MID, 0);
			c.setMedicinalQi(50);
			before[0] = c.getCultivation();
		});
		helper.runAfterDelay(160, () -> {
			try {
				helper.assertTrue(s.layer(fire) == 5 && Spell.FLAME_FIST.isKnown(c) && sleeping.isKnown(c),
						"At Mid, Mid's art at once, Early's still known (" + s.layer(fire) + ")");
				helper.assertTrue(c.getMedicinalQi() < 50 && c.getCultivation() > before[0], "A pill's qi refined onto the bar (" + c.getMedicinalQi() + " left)");
				helper.succeed();
			} finally {
				MeditationManager.forget(player.getUUID());
				Spells.forget(player.getUUID());
				HeavenMethods.forget(player.getUUID());
				player.server.getPlayerList().remove(player);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spellsAndPassivesWaitForTheRealm(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		HeavenMethod wood = HeavenMethod.of(QiElement.WOOD, Realm.QI_REFINING);
		Spell first = Spell.WOOD_BULLET, third = Spell.EMERALD_HEALING_ART, seventh = Spell.VERDANT_GROWTH;
		helper.assertTrue(first.taughtBy(wood) && third.taughtBy(wood) && seventh.taughtBy(wood), "The wood method's three spells");
		try {
			c.heaven().setMethod(wood, 9, 0); // known (understood, as every scripture since 2026-11-16, as far as the realm: whatever is set)
			HeavenMethods.watchSpells(player); // the first look: noted, nothing new
			helper.assertTrue(first.isKnown(c) && !third.isKnown(c) && !seventh.isKnown(c) && third.realmNeeded() == PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID)
					&& seventh.realmNeeded() == PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), "At Qi Refining Early only the first, its layers cultivated or not");
			helper.assertTrue(near(HeavenPassives.amount(c, QiElement.WOOD), 0.04), "Its passive only for Early's two layers (" + HeavenPassives.amount(c, QiElement.WOOD) + ")");
			c.setState(Realm.QI_REFINING, Stage.MID, 0);
			helper.assertTrue(third.isKnown(c) && !seventh.isKnown(c) && near(HeavenPassives.amount(c, QiElement.WOOD), 0.10), "Mid: the fifth layer's spell, five layers' passive");
			HeavenMethods.watchSpells(player);
			NetherworldState s = c.netherworld();
			int slot = -1;
			for (int i = 0; i < Spells.WHEEL_SLOTS; i++) if (s.spellSlot(i) == third) slot = i;
			helper.assertTrue(slot >= 0, "...on the wheel as the realm brings it");
			s.setSpellSlot(slot, null);
			HeavenMethods.watchSpells(player);
			boolean back = false;
			for (int i = 0; i < Spells.WHEEL_SLOTS; i++) back |= s.spellSlot(i) == third;
			helper.assertTrue(!back, "Taken off the wheel, it stays off");
			c.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
			helper.assertTrue(seventh.isKnown(c) && near(HeavenPassives.amount(c, QiElement.WOOD), 0.18), "Grand Perfection: both, and all nine layers' passive");
		} finally {
			HeavenMethods.forget(player.getUUID());
			Spells.forget(player.getUUID());
			player.server.getPlayerList().remove(player);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void oldSavesDropTheirOldProgress(GameTestHelper helper) {
		HeavenMethod wood = HeavenMethod.of(QiElement.WOOD, Realm.QI_REFINING);
		net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
		net.minecraft.nbt.CompoundTag methods = new net.minecraft.nbt.CompoundTag();
		net.minecraft.nbt.CompoundTag one = new net.minecraft.nbt.CompoundTag();
		one.putInt("Layer", 4);
		one.putDouble("Progress", 33); // progress through the layer it was on, as it was once counted
		methods.put(wood.getId(), one);
		old.put("Methods", methods);
		old.putInt("Units", 2);
		HeavenState migrated = new HeavenState();
		migrated.load(old);
		helper.assertTrue(migrated.layer(wood) == 4 && migrated.progress(wood) == 0, "Its layer kept, the old progress dropped");
		migrated.setMethod(wood, 4, 12);
		HeavenState again = new HeavenState();
		again.load(migrated.save());
		helper.assertTrue(again.layer(wood) == 4 && again.progress(wood) == 12, "Saved again, its settling read as it is");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cultivatorsKnowTheirSectsElementAndSteleRealmsFollowTheLands(GameTestHelper helper) {
		var level = helper.getLevel();
		RandomSource random = RandomSource.create(7);
		CultivatorNpc elder = ModEntities.CULTIVATOR.create(level);
		elder.becomeRogue(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID), 0, random);
		elder.setTitle(NpcTitle.ELDER);
		HeavenMethod known = elder.heavenMethod();
		helper.assertTrue(known != null && known.realm() == Realm.NASCENT_SOUL, "An elder cultivates its own realm's method (" + known + ")");
		helper.assertTrue(elder.heavenMethod() == known, "...always the same one");
		CultivatorNpc rogue = ModEntities.CULTIVATOR.create(level);
		rogue.becomeRogue(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0, random);
		helper.assertTrue(rogue.heavenMethod() != null && rogue.heavenMethod().tier() <= 3, "A rogue a method no deeper than its realm");
		helper.assertTrue(CultivatorTrades.goodsFor(rogue, random).stream().anyMatch(g -> g.item().is(ModItems.TECHNIQUE_MANUAL)
						&& TechniqueManualItem.heavenArt(g.item()) == rogue.heavenMethod()), "...whose Art Manual it sells");
		helper.assertTrue(CultivatorTrades.goodsFor(rogue, random).stream().noneMatch(g -> g.item().is(ModItems.JADE_SLIP) || g.item().is(ModItems.GHOST_JADE_SLIP)),
				"...and no method jade slip, since 2026-11-16");
		GhostCultivatorNpc ghost = ModEntities.GHOST_CULTIVATOR.create(level);
		ghost.becomeGhost(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID), 0, random);
		helper.assertTrue(ghost.heavenMethod() == null, "The dead keep no heaven method");
		Sect sect = new Sect(java.util.UUID.randomUUID());
		helper.assertTrue(sect.element() == sect.element() && sect.element() != QiElement.YIN, "A sect has one of the five");
		helper.assertTrue(CultivatorTrades.heavenSlipPrice(HeavenMethod.of(QiElement.WATER, Realm.QI_REFINING)) == 60
				&& CultivatorTrades.heavenSlipPrice(HeavenMethod.of(QiElement.WATER, Realm.FOUR_AXIS)) == 14_580, "Slips three times dearer each realm deeper");
		// The steles' realms: the Overworld no Four Axis, the Upper Realm no Qi Refining.
		int overworldDeep = 0, upperQr = 0, upperFa = 0;
		for (int i = 0; i < 20_000; i++) {
			Realm o = DaoSteleFeature.pickRealm(random, DaoSteleFeature.Lands.OVERWORLD);
			if (o == Realm.FOUR_AXIS || o == Realm.HEAVENLY_BEING) overworldDeep++;
			Realm u = DaoSteleFeature.pickRealm(random, DaoSteleFeature.Lands.UPPER_REALM_RARE);
			if (u == Realm.QI_REFINING) upperQr++;
			if (u == Realm.FOUR_AXIS) upperFa++;
		}
		helper.assertTrue(overworldDeep == 0 && upperQr == 0 && upperFa > 0, "No Heavenly Being or Four Axis stele in the common Overworld; Four Axis ones in the Upper Realm's wonders");
		helper.assertTrue(DaoSteleFeature.lands(biome(helper, Biomes.CHERRY_GROVE)) == DaoSteleFeature.Lands.OVERWORLD_RARE
				&& DaoSteleFeature.lands(biome(helper, Biomes.PLAINS)) == DaoSteleFeature.Lands.OVERWORLD, "Cherry groves are among the rare lands, plains not");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRogueTeachesItsMethodOnceADay(GameTestHelper helper) {
		RandomSource random = RandomSource.create(11);
		CultivatorNpc rogue = ModEntities.CULTIVATOR.create(helper.getLevel());
		rogue.moveTo(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
		rogue.becomeRogue(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID), 0, random);
		HeavenMethod method = rogue.heavenMethod();
		ServerPlayer player = cultivator(helper, Realm.CORE_FORMATION, Stage.MID);
		PlayerCultivation c = CultivationManager.get(player);
		HeavenState s = c.heaven();
		try {
			rogue.regardEntry(player).value = Regard.WARM;
			TalkSystem.teachHeaven(player, rogue);
			int upTo = HeavenMethods.understoodTo(c, method);
			helper.assertTrue(s.knows(method) && s.layer(method) == upTo && c.methodReach(CultivationMethod.QI_CIRCULATION) == method.realm().ordinal(),
					"Warm: it teaches its scripture (" + method.getId() + "), understood as far as the realm, and the Qi Circulation it breathes by, to its realm");
			TalkSystem.teachHeaven(player, rogue);
			helper.assertTrue(c.getCultivation() == 0, "...once a day");
			rogue.regardEntry(player).guidanceDay = -1;
			TalkSystem.teachHeaven(player, rogue);
			// The ten minutes' cultivation only with Circulation's volume of the realm (the methods overhaul, 2026-10-31).
			boolean volume = HeavenMethods.canGain(c);
			helper.assertTrue(volume == (method.realm() == Realm.CORE_FORMATION) && (volume ? c.getStage() != Stage.MID || c.getCultivation() > 0 : c.getCultivation() == 0),
					"Another day: ten minutes' cultivation only with a volume of the realm (" + c.getStage() + ", " + volume + ")");
			if (!volume) {
				c.raiseReach(CultivationMethod.QI_CIRCULATION, Realm.CORE_FORMATION); // Core Formation's volume, as its manual teaches it
				rogue.regardEntry(player).guidanceDay = -1;
				TalkSystem.teachHeaven(player, rogue);
				helper.assertTrue(c.getStage() != Stage.MID || c.getCultivation() > 0, "With Core Formation's volume, its ten minutes (" + c.getCultivation() + ")");
			}
			ServerPlayer mortal = helper.makeMockServerPlayerInLevel();
			rogue.regardEntry(mortal).value = Regard.WARM;
			TalkSystem.teachHeaven(mortal, rogue);
			helper.assertTrue(!CultivationManager.get(mortal).heaven().knows(method) && rogue.regardEntry(mortal).guidanceDay != Regard.day(helper.getLevel().getGameTime()),
					"A mortal is told its meridians are sealed, and the day isn't spent");
		} finally {
			Spells.forget(player.getUUID());
		}
		helper.succeed();
	}

	/**
	 * A batch of its own (2026-10-19): a spell's block is never set inside a raised formation's barrier (HeavenSpells#placeTemporary),
	 * and now and then a neighbouring test's formation reached over this one's ground ("Into air once" failed).
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_methods_spell_blocks")
	public void aDaoSteleTeachesAndASpellsBlocksAreTakenUpAgain(GameTestHelper helper) {
		BlockPos foot = helper.absolutePos(new BlockPos(2, 1, 2));
		HeavenMethod method = HeavenMethod.of(QiElement.EARTH, Realm.CORE_FORMATION);
		DaoSteleBlock.place(helper.getLevel(), foot, method, net.minecraft.core.Direction.NORTH);
		helper.assertTrue(helper.getLevel().getBlockState(foot.above()).getValue(DaoSteleBlock.METHOD) == method
				&& helper.getLevel().getBlockState(foot.above(2)).getValue(DaoSteleBlock.PART) == MethodSteleBlock.Part.UPPER, "A stele three stones tall");
		// A spell's block: into air only, and gone when its time is up.
		BlockPos air = helper.absolutePos(new BlockPos(5, 2, 5));
		helper.getLevel().setBlock(air, Blocks.AIR.defaultBlockState(), 3);
		BlockPos stone = helper.absolutePos(new BlockPos(5, 1, 5));
		helper.getLevel().setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
		int before = HeavenSpells.temporaryCount();
		helper.assertTrue(HeavenSpells.placeTemporary(helper.getLevel(), air, Blocks.ICE.defaultBlockState(), 10)
				&& !HeavenSpells.placeTemporary(helper.getLevel(), stone, Blocks.ICE.defaultBlockState(), 10)
				&& !HeavenSpells.placeTemporary(helper.getLevel(), air, Blocks.ICE.defaultBlockState(), 10), "Into air once; never over stone, nor twice");
		helper.assertTrue(helper.getLevel().getBlockState(air).is(Blocks.ICE) && HeavenSpells.temporaryCount() == before + 1, "Set down");
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(helper.getLevel().getBlockState(air).isAir() && helper.getLevel().getBlockState(stone).is(Blocks.STONE),
					"Taken up again in its time, the stone beside it untouched");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "heaven_methods_passives")
	public void theFivePassivesComeFromTheDeepestMethods(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		try {
			// A Qi Refining wood method alone: its nine layers.
			c.heaven().setMethod(HeavenMethod.WOOD_QI_REFINING, HeavenMethod.WOOD_QI_REFINING.layers(), 0);
			helper.assertTrue(near(HeavenPassives.amount(c, QiElement.WOOD), 0.18), "A full Qi Refining wood method: 0.18 a second ("
					+ HeavenPassives.amount(c, QiElement.WOOD) + ")");
			// Every element's Four Axis method to its last layer: an element's methods don't stack, the deepest counts.
			for (QiElement element : QiElement.five()) {
				HeavenMethod method = HeavenMethod.of(element, Realm.FOUR_AXIS);
				c.heaven().setMethod(method, method.layers(), 0);
			}
			CultivationManager.refresh(player);
			helper.assertTrue(near(HeavenPassives.amount(c, QiElement.WOOD), 8.01) && near(HeavenPassives.amount(c, QiElement.WATER), 42.57)
					&& near(HeavenPassives.amount(c, QiElement.FIRE), 21.6) && near(HeavenPassives.amount(c, QiElement.EARTH), 18)
					&& near(HeavenPassives.amount(c, QiElement.METAL), 35.1), "The five at their fullest");
			player.setHealth(player.getMaxHealth() - 20);
			float hurt = player.getHealth();
			HeavenPassives.apply(player);
			helper.assertTrue(near(player.getHealth() - hurt, 8.01), "Wood mends (" + (player.getHealth() - hurt) + ")");
			// The body's realm brings its own strength: the passive's is its own modifier.
			double whetted = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getModifiers().stream()
					.filter(m -> m.getName().equals("Whetted Edge")).mapToDouble(m -> m.getAmount()).sum();
			helper.assertTrue(near(whetted, 35.1), "Metal whets the blow (" + whetted + ")");
			float blow = HeavenSpells.incoming(player, player.damageSources().generic(), 100);
			helper.assertTrue(near(blow, 82), "Earth lessens the harm by 18% (" + blow + ")");
			helper.assertTrue(HeavenSpells.incoming(player, player.damageSources().fellOutOfWorld(), 100) == 100, "...but never the void's");
			// The dead have none.
			c.netherworld().setGhost(true);
			helper.assertTrue(HeavenPassives.amount(c, QiElement.WOOD) == 0, "A ghost has no heaven passives");
		} finally {
			c.netherworld().setGhost(false);
			HeavenPassives.forget(player.getUUID());
			Spells.forget(player.getUUID());
			player.server.getPlayerList().remove(player);
		}
		helper.succeed();
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 0.01;
	}
}
