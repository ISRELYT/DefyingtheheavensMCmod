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
 * The heaven methods' framework (see HeavenMethod, HeavenMethods): thirty of them, five elements by six realms, each spanning Qi
 * Refining up to its own realm; gating the heaven realm, never gated by it; learned by the living alone, a later one of an element at the element's
 * depth; faster where its element's qi runs thick and in realms long passed; saved; known by cultivators, carved on Dao Steles
 * whose realms the lands decide; and the blocks the spells set down always taken up again.
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
		helper.assertTrue(HeavenMethod.values().length == 30, "Thirty heaven methods");
		java.util.Set<String> ids = new java.util.HashSet<>();
		for (QiElement element : QiElement.five()) {
			for (Realm realm : Realm.values()) {
				HeavenMethod method = HeavenMethod.of(element, realm);
				helper.assertTrue(method != null && method.element() == element && method.realm() == realm, "One for " + element + " " + realm);
				helper.assertTrue(method.layers() == 9 * (realm.ordinal() + 1) && method.ownLayer(3) == method.layers() - 6,
						method.getId() + ": nine layers a realm from Qi Refining, its spells in its own");
				helper.assertTrue(HeavenMethod.byId(method.getId()) == method && ids.add(method.getId()), "Its id is its own");
			}
		}
		// Its spells: two each, the third and seventh layers of its own realm, costing qi alone.
		int spells = 0;
		for (Spell spell : Spell.values()) {
			if (!spell.isHeaven()) continue;
			spells++;
			HeavenMethod method = spell.heavenMethod();
			int within = MethodLayers.within(spell.layer());
			helper.assertTrue(MethodLayers.realmOf(spell.layer()) == method.realm() && (within == 3 || within == 7) && spell.cost() == 0 && spell.qiCost() > 0,
					spell.getId() + " comes in its method's own realm, on qi");
		}
		helper.assertTrue(spells == 0 || spells == 60, "Sixty spells, or none yet (" + spells + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theMethodGrowsWhateverTheRealm(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		HeavenMethod fourAxis = HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS);
		helper.assertTrue(HeavenMethods.refusal(c) != null && !HeavenMethods.canLearn(c), "A mortal: its meridians sealed");
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		helper.assertTrue(HeavenMethods.canLearn(c), "Opened, a cultivator takes them up");
		HeavenState s = c.heaven();
		s.setMethod(fourAxis, 1, 0);
		helper.assertTrue(HeavenMethods.cultivate(s, fourAxis, 1_000_000) > 20 && s.layer(fourAxis) > 20 && c.getRealm() == Realm.QI_REFINING,
				"At Qi Refining Early it goes on far past its realm's layers (to " + s.layer(fourAxis) + "): no realm holds it back");
		helper.assertTrue(HeavenMethods.cultivate(s, fourAxis, 1.0e12) > 0 && s.layer(fourAxis) == 54 && s.progress(fourAxis) == HeavenMethods.layerCost(54)
				&& HeavenMethods.perfected(s, fourAxis) && HeavenMethods.cultivate(s, fourAxis, 1.0e12) == 0, "...on to the 54th, perfected, and no further");
		helper.assertTrue(HeavenMethods.reach(c) == PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION),
				"Perfected, it carries cultivation all the way: the realm still has to be climbed, the method waits on nothing");
		PlayerCultivation ghost = new PlayerCultivation();
		ghost.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		ghost.netherworld().setGhost(true);
		helper.assertTrue(!HeavenMethods.canLearn(ghost) && HeavenMethods.refusal(ghost) != null, "The dead take up no heaven method");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMethodIsLearnedAtItsElementsDepth(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		HeavenState s = c.heaven();
		try {
			HeavenMethod woodFb = HeavenMethod.of(QiElement.WOOD, Realm.FOUNDATION_BUILDING);
			helper.assertTrue(HeavenMethods.learn(player, woodFb) && s.layer(woodFb) == 1 && s.activeMethod() == woodFb, "The first learned: its first layer, practised");
			s.setMethod(woodFb, 14, 77);
			HeavenMethod woodNs = HeavenMethod.of(QiElement.WOOD, Realm.NASCENT_SOUL);
			helper.assertTrue(HeavenMethods.learn(player, woodNs) && s.layer(woodNs) == 14 && s.progress(woodNs) == 77 && s.activeMethod() == woodNs,
					"A deeper wood method starts where wood stands (14, progress and all), and takes over the practice");
			HeavenMethod woodQr = HeavenMethod.of(QiElement.WOOD, Realm.QI_REFINING);
			helper.assertTrue(HeavenMethods.learn(player, woodQr) && s.layer(woodQr) == 9 && s.progress(woodQr) == HeavenMethods.layerCost(9),
					"A lesser wood method, outgrown, is known whole");
			HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.CORE_FORMATION);
			helper.assertTrue(HeavenMethods.learn(player, fire) && s.layer(fire) == 1 && s.activeMethod() == woodNs,
					"Another element's starts at its first layer; the practice stays with wood");
			helper.assertTrue(!HeavenMethods.learn(player, fire), "Known: not learned again");
			helper.assertTrue(HeavenMethods.depth(s, QiElement.WOOD) == 14 && HeavenMethods.deepestOf(s, QiElement.WOOD) == woodNs,
					"Wood's depth is its deepest method's (of two as deep, the one going further)");
			// Saved and loaded by id.
			HeavenState loaded = PlayerCultivation.load(c.save()).heaven();
			helper.assertTrue(loaded.layer(woodNs) == 14 && loaded.progress(woodNs) == 77 && loaded.layer(fire) == 1 && loaded.activeMethod() == woodNs
					&& loaded.known().size() == 4, "Saved and loaded");
			// A slip, read, teaches its method, and isn't used up.
			HeavenMethod metal = HeavenMethod.of(QiElement.METAL, Realm.QI_REFINING);
			ItemStack slip = JadeSlipItem.of(metal);
			helper.assertTrue(JadeSlipItem.method(slip) == metal, "A slip holds its method");
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, slip);
			slip.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			helper.assertTrue(s.knows(metal) && s.layer(metal) == 1 && !player.getMainHandItem().isEmpty(), "Read: its method learned, the slip kept");
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
		helper.assertTrue(HeavenMethods.elementFactor(0.2) == 1.0 && HeavenMethods.elementFactor(0) == 0.5 && HeavenMethods.elementFactor(1) == 3.0,
				"An even fifth: the base pace; none: half; all of it: three times");
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		helper.assertTrue(HeavenMethods.methodFactor(c, woodInForest, false) > HeavenMethods.methodFactor(c, woodInDesert, false) * 2,
				"Wood cultivates more than twice as fast in a forest as in a desert");
		c.setQiBoost(0.5);
		c.setGearQiBonus(0.6);
		helper.assertTrue(near(HeavenMethods.methodFactor(c, 0.2, true), 1.25 * 1.5 * 1.6),
				"By its stele x1.25, a Qi Gathering Pill's +50% and clothing's +60% on top (" + HeavenMethods.methodFactor(c, 0.2, true) + ")");
		// A stage's layers cost what the stage does: a realm long passed goes by as fast as its stages would now.
		helper.assertTrue(near(HeavenMethods.layerCost(1) + HeavenMethods.layerCost(2), 100) && near(HeavenMethods.layerCost(3) + HeavenMethods.layerCost(4)
				+ HeavenMethods.layerCost(5), 150) && near(HeavenMethods.layerCost(6) + HeavenMethods.layerCost(7) + HeavenMethods.layerCost(8), 200)
				&& near(HeavenMethods.layerCost(9), 300), "Qi Refining's layers: Early's two 100 between them, Mid's three 150, Late's 200, Grand Perfection's 300");
		helper.assertTrue(near(HeavenMethods.layerCost(54), PlayerCultivation.required(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION))
				&& HeavenMethods.layerCost(2) > HeavenMethods.layerCost(1), "Four Axis's last as its Grand Perfection; a stage's later layers dearer");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRealmKeepsStepWithTheMethod(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		helper.assertTrue(!c.addCultivation(1000) && c.getStage() == Stage.EARLY && c.getCultivation() == c.cultivationRequired() && c.isHeldByMethod()
				&& !c.isStalled() && HeavenMethods.reach(c) == 0, "No method: Qi Refining Early's bar fills, and there it waits");
		HeavenState s = c.heaven();
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING);
		s.setMethod(fire, 2, 0);
		helper.assertTrue(!c.addCultivation(0) && c.isHeldByMethod(), "Its second layer begun, not finished: still waiting");
		s.setMethod(fire, 2, HeavenMethods.layerCost(2));
		helper.assertTrue(c.addCultivation(0) && c.getStage() == Stage.MID, "Early's two layers cultivated: on into Mid");
		c.setHeavensLeave(true);
		helper.assertTrue(!c.addCultivation(10_000) && c.getStage() == Stage.MID && c.isHeldByMethod(), "Mid's three layers not yet: Late waits, the ritual done");
		s.setMethod(fire, 5, HeavenMethods.layerCost(5));
		helper.assertTrue(c.addCultivation(0) && c.getStage() == Stage.LATE, "...cultivated: into Late");
		// A method goes no further than its own realm.
		s.setMethod(fire, 9, HeavenMethods.layerCost(9));
		c.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
		c.prepareBreakthrough(Realm.FOUNDATION_BUILDING, 0);
		c.addCultivation(10_000);
		helper.assertTrue(c.isAtBottleneck() && c.isHeldByMethod() && !c.canBreakthrough(),
				"A Qi Refining method, perfected, carries no one into Foundation Building");
		s.setMethod(HeavenMethod.of(QiElement.WOOD, Realm.FOUNDATION_BUILDING), 9, HeavenMethods.layerCost(9));
		helper.assertTrue(c.canBreakthrough() && !c.isHeldByMethod() && HeavenMethods.reach(c) == PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY),
				"A Foundation Building method as deep (any element's): the tribulation may come");
		// Unrefined qi waiting at the method's gate goes into the method, not the full bar.
		PlayerCultivation held = new PlayerCultivation();
		held.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		held.addCultivation(1000);
		held.setMedicinalQi(50);
		helper.assertTrue(held.refine(10) == 0 && held.refineIntoMethod(10) == 10 && near(held.getMedicinalQi(), 40), "Refined into the method at its gate");
		// An op setting one back to or below Qi Refining Mid: the ritual is asked again.
		PlayerCultivation op = new PlayerCultivation();
		CultivationCommand.set(op, Realm.CORE_FORMATION, Stage.EARLY);
		op.setFavour(2);
		helper.assertTrue(op.hasHeavensLeave(), "Set past the ritual: as if it had been asked");
		CultivationCommand.set(op, Realm.QI_REFINING, Stage.EARLY);
		helper.assertTrue(!op.hasHeavensLeave() && op.getFavour() == 0 && op.needsHeavensLeave() == false, "Set back to Early: no leave, no Favour");
		CultivationCommand.set(op, Realm.QI_REFINING, Stage.MID);
		helper.assertTrue(op.needsHeavensLeave(), "...and at Mid the ritual is wanted again");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 260)
	public void meditationFillsTheBarAndTheMethodAlike(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.moveTo(at.x, at.y, at.z);
		player.setOnGround(true);
		PlayerCultivation c = CultivationManager.get(player);
		c.awaken(0);
		HeavenMethod fire = HeavenMethod.of(QiElement.FIRE, Realm.QI_REFINING);
		HeavenState s = c.heaven();
		s.setMethod(fire, 1, 0);
		s.setActiveMethod(fire);
		MeditationManager.start(player);
		helper.assertTrue(MeditationManager.isMeditating(player.getUUID()), "Sat down to meditate");
		double factor = HeavenMethods.methodFactor(c, HeavenMethods.shareAt(player, QiElement.FIRE), false);
		double[] before = new double[1];
		helper.runAfterDelay(100, () -> {
			helper.assertTrue(c.getCultivation() > 0 && Math.abs(s.progress(fire) / c.getCultivation() - factor) < 1.0e-6,
					"The method takes in what the bar gathers, by fire's share here (" + s.progress(fire) + " / " + c.getCultivation() + ", x" + factor + ")");
			// The bar full and its method's second layer not yet: it waits, and a pill's qi goes into the method.
			c.addCultivation(c.cultivationRequired());
			c.setMedicinalQi(50);
			before[0] = s.progress(fire);
		});
		helper.runAfterDelay(160, () -> {
			helper.assertTrue(c.getStage() == Stage.EARLY && c.isHeldByMethod() && c.getMedicinalQi() < 50 && s.progress(fire) > before[0],
					"Held at the method's gate, the unrefined qi refined into the method (" + c.getMedicinalQi() + ")");
			s.setMethod(fire, 2, HeavenMethods.layerCost(2));
		});
		helper.runAfterDelay(200, () -> {
			try {
				helper.assertTrue(c.getStage() == Stage.MID, "Its layers cultivated, the bar steps on into Mid (" + c.getStage() + ")");
				helper.succeed();
			} finally {
				MeditationManager.forget(player.getUUID());
				Spells.forget(player.getUUID());
				player.server.getPlayerList().remove(player);
			}
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spellsAndPassivesWaitForTheRealm(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		HeavenMethod wood = HeavenMethod.of(QiElement.WOOD, Realm.QI_REFINING);
		Spell third = null, seventh = null;
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven() && spell.heavenMethod() == wood) {
				if (spell.layer() == 3) third = spell;
				if (spell.layer() == 7) seventh = spell;
			}
		}
		helper.assertTrue(third != null && seventh != null, "The wood method's two spells");
		try {
			c.heaven().setMethod(wood, 9, HeavenMethods.layerCost(9)); // perfected, far ahead of the realm
			HeavenMethods.watchSpells(player); // the first look: noted, nothing new
			helper.assertTrue(!third.isKnown(c) && !seventh.isKnown(c) && third.realmNeeded() == PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID)
					&& seventh.realmNeeded() == PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), "At Qi Refining Early neither spell, its layers cultivated or not");
			helper.assertTrue(near(HeavenPassives.amount(c, QiElement.WOOD), 0.04), "Its passive only for Early's two layers (" + HeavenPassives.amount(c, QiElement.WOOD) + ")");
			c.setState(Realm.QI_REFINING, Stage.MID, 0);
			helper.assertTrue(third.isKnown(c) && !seventh.isKnown(c) && near(HeavenPassives.amount(c, QiElement.WOOD), 0.10), "Mid: the third layer's spell, five layers' passive");
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
	public void oldSavesKeepTheirShareOfALayer(GameTestHelper helper) {
		HeavenMethod wood = HeavenMethod.of(QiElement.WOOD, Realm.QI_REFINING);
		net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
		net.minecraft.nbt.CompoundTag methods = new net.minecraft.nbt.CompoundTag();
		net.minecraft.nbt.CompoundTag one = new net.minecraft.nbt.CompoundTag();
		one.putInt("Layer", 1);
		one.putDouble("Progress", MethodLayers.cost(1) / 2); // half its first layer, in the seconds it was once counted in
		methods.put(wood.getId(), one);
		old.put("Methods", methods);
		HeavenState migrated = new HeavenState();
		migrated.load(old);
		helper.assertTrue(near(migrated.progress(wood), HeavenMethods.layerCost(1) / 2), "Half its first layer still (" + migrated.progress(wood) + ")");
		HeavenState again = new HeavenState();
		again.load(migrated.save());
		helper.assertTrue(again.progress(wood) == migrated.progress(wood), "Saved again, read as it is");
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
		helper.assertTrue(CultivatorTrades.goodsFor(rogue, random).stream().anyMatch(g -> JadeSlipItem.method(g.item()) == rogue.heavenMethod()),
				"...whose slip it sells");
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
		HeavenState s = CultivationManager.get(player).heaven();
		try {
			rogue.regardEntry(player).value = Regard.WARM;
			TalkSystem.teachHeaven(player, rogue);
			helper.assertTrue(s.knows(method) && s.layer(method) == 1, "Warm: it teaches its method (" + method.getId() + ")");
			TalkSystem.teachHeaven(player, rogue);
			helper.assertTrue(s.layer(method) == 1 && s.progress(method) == 0, "...once a day");
			rogue.regardEntry(player).guidanceDay = -1;
			TalkSystem.teachHeaven(player, rogue);
			helper.assertTrue(s.layer(method) > 1, "Another day: ten minutes' progress (" + s.layer(method) + ")");
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

	@GameTest(template = EMPTY_STRUCTURE)
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
