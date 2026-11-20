package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The methods from manuals (the user's asks of 2026-11-16): no method jade slip left anywhere, every method from its Method Manual (Qi
 * Circulation's and Netherworld Breathing's too, the scriptures and foundations carrying none of their volumes); the scriptures and ghost
 * foundations Art Manuals, learned whole at once (no layers but the Laws'), each art wielded as the path reaches its stage; the techniques
 * on jade slips; a stele told by a touch and teaching a method of its own, drawn by its place, in seven seconds whatever qi is drawn in;
 * bodies born against some methods; the old worlds' volumes carried over; the vanilla world's chests holding the mod's goods; and the deep
 * ghost foundations the steles no longer teach kept in the Yellow Springs Hollow's vaults.
 */
public class ManualsGameTests implements FabricGameTest {
	/** No body born against anything: five roots, no forms. */
	private static void plain(PlayerCultivation c) {
		c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		c.setForms(List.of());
		c.setRootElements(List.of());
	}

	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		c.awaken(0);
		plain(c);
		c.setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	private static void leave(ServerPlayer player) {
		MeditationManager.forget(player.getUUID());
		Spells.forget(player.getUUID());
		HeavenMethods.forget(player.getUUID());
		GhostSystem.forget(player.getUUID());
		TwoPaths.forget(player.getUUID());
		player.server.getPlayerList().remove(player);
	}

	private static String key(Component line) {
		return line != null && line.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	private static void read(ServerPlayer player, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		stack.use(player.level(), player, InteractionHand.MAIN_HAND);
	}

	// --- Steles ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSteleHoldsOneMethodItsPlaceChose(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos somewhere = helper.absolutePos(new BlockPos(1, 2, 1));
		helper.assertTrue(SteleTeachings.of(level, somewhere, Realm.TRUE_IMMORTAL) == null, "A True Immortal carving teaches nothing below the Immortal Realm");
		SteleTeachings.Teaching axis = SteleTeachings.of(level, somewhere, Realm.FOUR_AXIS);
		helper.assertTrue(axis != null && axis.realm() == Realm.HEAVENLY_BEING && axis.method().spans(Realm.HEAVENLY_BEING),
				"A Four Axis carving: Heavenly Being's volumes (no method has one of Four Axis)");
		// Every one a manual's method spanning the carved realm, of any path; always the same at one place, different between places.
		Set<CultivationMethod> seen = EnumSet.noneOf(CultivationMethod.class);
		Set<TwoPaths.Path> paths = EnumSet.noneOf(TwoPaths.Path.class);
		for (int i = 0; i < 400; i++) {
			BlockPos at = new BlockPos(i * 37, 64, i * 101 - 5000);
			SteleTeachings.Teaching t = SteleTeachings.of(level, at, Realm.CORE_FORMATION);
			helper.assertTrue(t != null && t.method().fromManual() && t.method().spans(Realm.CORE_FORMATION) && t.realm() == Realm.CORE_FORMATION
					&& t.equals(SteleTeachings.of(level, at, Realm.CORE_FORMATION)), "A manual's method of Core Formation, fixed by its place (" + t + ")");
			seen.add(t.method());
			paths.add(t.method().path());
		}
		helper.assertTrue(seen.size() >= 4 && paths.size() >= 2, "...and drawn at random between places (" + seen.size() + " methods, " + paths + ")");
		// A placed stele's own: its carving's realm, by its middle stone.
		BlockPos foot = helper.absolutePos(new BlockPos(3, 1, 3));
		HeavenMethod earth = HeavenMethod.of(QiElement.EARTH, Realm.FOUNDATION_BUILDING);
		DaoSteleBlock.place(level, foot, earth, Direction.NORTH);
		var middle = level.getBlockState(foot.above());
		helper.assertTrue(SteleTeachings.carved(middle) == Realm.FOUNDATION_BUILDING && SteleTeachings.middle(level.getBlockState(foot), foot).equals(foot.above())
				&& SteleTeachings.middle(level.getBlockState(foot.above(2)), foot.above(2)).equals(foot.above())
				&& SteleTeachings.of(level, foot.above(), middle).equals(SteleTeachings.of(level, foot.above(), Realm.FOUNDATION_BUILDING)),
				"A Dao Stele of a Foundation Building scripture teaches a method of Foundation Building, read by its middle stone");
		helper.assertTrue(SteleTeachings.carved(GhostMethod.GHOST_FIRE_SCRIPTURE) == Realm.QI_REFINING && SteleTeachings.carved(GhostMethod.GHOST_CORE_CANON) == Realm.CORE_FORMATION,
				"A Method Stele of the fire: Qi Refining's; of a foundation, its own realm's");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void aSteleTeachesItsMethodInSevenSecondsWhateverQiIsDrawn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		player.getAbilities().invulnerable = true; // a blow ends a meditation
		Vec3 seat = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
		player.moveTo(seat.x, seat.y, seat.z);
		player.setOnGround(true);
		PlayerCultivation c = CultivationManager.get(player);
		// A Method Stele of the fire (Qi Refining's), where its place gives it a ghost path's method: read with the heavens' qi drawn in.
		BlockPos[] foot = new BlockPos[1];
		SteleTeachings.Teaching[] teaching = new SteleTeachings.Teaching[1];
		for (int dx = -3; dx <= 3 && foot[0] == null; dx++) {
			for (int dz = -3; dz <= 3 && foot[0] == null; dz++) {
				if (dx == 0 && dz == 0 || dx * dx + dz * dz > 12) continue;
				BlockPos at = helper.absolutePos(new BlockPos(2 + dx, 2, 2 + dz));
				SteleTeachings.Teaching t = SteleTeachings.of(level, at.above(), Realm.QI_REFINING);
				if (t != null && t.method().path() == TwoPaths.Path.GHOST && CultivationMethods.refusal(c, t.method()) == null) {
					foot[0] = at;
					teaching[0] = t;
				}
			}
		}
		helper.assertTrue(foot[0] != null, "A place near enough whose stele holds a ghost path's method");
		MethodSteleBlock.place(level, foot[0], GhostMethod.GHOST_FIRE_SCRIPTURE, Direction.NORTH);
		BlockPos middle = foot[0].above();
		helper.assertTrue(teaching[0].equals(SteleTeachings.of(level, middle, level.getBlockState(middle))), "Placed, it holds that method");
		SteleTeachings.tell(player, level.getBlockState(middle), middle); // a touch: told, nothing learned
		helper.assertTrue(!CultivationMethods.knows(c, teaching[0].method()), "Told by a touch, not taught");
		MeditationManager.start(player);
		helper.assertTrue(MeditationManager.isMeditating(player.getUUID()) && !c.netherworld().cultivatingGhost(), "Meditating on the heavens' qi");
		helper.runAfterDelay(100, () -> helper.assertTrue(!SteleTeachings.holds(c, teaching[0]), "Five seconds: not yet"));
		helper.runAfterDelay(220, () -> {
			try {
				CultivationMethod method = teaching[0].method();
				helper.assertTrue(CultivationMethods.knows(c, method) && c.methodReach(method) >= teaching[0].reach().ordinal() && SteleTeachings.holds(c, teaching[0]),
						"Seven seconds by it: " + method.getId() + " learned, its volumes to Qi Refining (the qi drawn in the heavens')");
				helper.assertTrue(method != CultivationMethod.NETHERWORLD_BREATHING || GhostMethods.ghostRank(c.netherworld()) >= 0,
						"Netherworld Breathing begins the ghost path");
				helper.succeed();
			} finally {
				leave(player);
			}
		});
	}

	// --- Art Manuals ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void anArtManualTeachesAWholeScriptureItsArtsByTheRealm(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		try {
			HeavenMethod woodFb = HeavenMethod.of(QiElement.WOOD, Realm.FOUNDATION_BUILDING);
			read(player, TechniqueManualItem.of(woodFb));
			helper.assertTrue(c.heaven().knows(woodFb) && HeavenMethods.artsOf(woodFb) == 6, "Read: the scripture known, its six arts with it");
			helper.assertTrue(Spell.WOOD_BULLET.isKnown(c) && !Spell.EMERALD_HEALING_ART.isKnown(c) && !Spell.LIFE_FORCE_DRAIN.isKnown(c),
					"At Qi Refining Early, Early's art wielded; Mid's and Foundation Building's wait on the realm");
			c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
			helper.assertTrue(Spell.EMERALD_HEALING_ART.isKnown(c) && Spell.VERDANT_GROWTH.isKnown(c) && Spell.LIFE_FORCE_DRAIN.isKnown(c) && !Spell.LIFE_FORCE_GAIN.isKnown(c),
					"At Foundation Building Early: every art below, and its Early's; not yet its Mid's");
			// A ghost foundation's, and a ghost art's.
			read(player, TechniqueManualItem.of(GhostMethod.GHOST_FIRE_SCRIPTURE));
			helper.assertTrue(c.netherworld().knows(GhostMethod.GHOST_FIRE_SCRIPTURE) && Spell.GHOST_FIRE.isKnown(c) && !Spell.GHOST_FIRE_BOLT.isKnown(c),
					"The fire's manual: the fire at once; its bolt waits on the ghost path's Foundation Building");
			read(player, TechniqueManualItem.of(GhostMethod.YELLOW_SPRINGS_MARROW));
			helper.assertTrue(c.netherworld().knows(GhostMethod.YELLOW_SPRINGS_MARROW) && GhostMethods.ghostRank(c.netherworld()) == 0
					&& c.methodReach(CultivationMethod.NETHERWORLD_BREATHING) < 0, "A foundation's manual begins the ghost path, and carries none of Breathing's volumes");
			// The slips of before turn into these manuals.
			ItemStack ghostSlip = new ItemStack(ModItems.GHOST_JADE_SLIP);
			ghostSlip.getOrCreateTag().putString("Method", GhostMethod.WANDERING_SOUL_STEPS.getId());
			ItemStack now = GhostJadeSlipItem.converted(ghostSlip);
			helper.assertTrue(now.is(ModItems.TECHNIQUE_MANUAL) && TechniqueManualItem.ghostArt(now) == GhostMethod.WANDERING_SOUL_STEPS
					&& GhostJadeSlipItem.of(GhostMethod.WANDERING_SOUL_STEPS).is(ModItems.TECHNIQUE_MANUAL), "A ghost jade slip of before: the Art Manual of its art");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	// --- Method Manuals ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void qiCirculationAndBreathingComeFromTheirOwnManuals(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		try {
			helper.assertTrue(!CultivationMethods.knows(c, CultivationMethod.QI_CIRCULATION) && !HeavenMethods.canGain(c), "No Qi Circulation: nothing gathered");
			read(player, TechniqueManualItem.of(HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS)));
			helper.assertTrue(c.heaven().knows(HeavenMethod.of(QiElement.FIRE, Realm.FOUR_AXIS)) && !HeavenMethods.canGain(c), "A Four Axis scripture known: still nothing");
			read(player, MethodManualItem.of(CultivationMethod.QI_CIRCULATION, Realm.QI_REFINING));
			helper.assertTrue(c.methodReach(CultivationMethod.QI_CIRCULATION) == Realm.QI_REFINING.ordinal() && !HeavenMethods.canGain(c),
					"Its first volume: Qi Refining's alone, nothing in Foundation Building");
			read(player, MethodManualItem.of(CultivationMethod.QI_CIRCULATION, Realm.FOUNDATION_BUILDING));
			helper.assertTrue(c.methodReach(CultivationMethod.QI_CIRCULATION) == Realm.FOUNDATION_BUILDING.ordinal() && HeavenMethods.canGain(c),
					"Its volumes to Foundation Building: it gathers");
			read(player, MethodManualItem.of(CultivationMethod.NETHERWORLD_BREATHING, Realm.QI_REFINING));
			helper.assertTrue(GhostMethods.ghostRank(c.netherworld()) == 0 && GhostMethods.canGain(c), "Netherworld Breathing's first volume begins the ghost path, and it gathers");
			// The sects' libraries keep Qi Circulation's volumes: the lower realms' to their last, Heavenly Being (2026-11-16: Core Formation
			// before), the lower volumes the likelier; the Upper Realm's Core Formation to Sacred Vessel; the Great Void's Heavenly Being to
			// Entering Nirvana (the same evening); never Four Axis.
			net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(5);
			int lowerDeep = 0, greatVoidDeep = 0, immortal = 0;
			MethodManualItem.World[] worlds = MethodManualItem.World.values();
			for (int i = 0; i < 100 * worlds.length; i++) {
				MethodManualItem.World world = worlds[i % worlds.length];
				ItemStack kept = MethodManualItem.circulation(world, random);
				Realm realm = MethodManualItem.realm(kept);
				// (The Immortal Realm's sects Entering Nirvana to True Immortal: the user's word, 2026-11-17.)
				Realm low = switch (world) {
					case LOWER -> Realm.QI_REFINING;
					case UPPER -> Realm.CORE_FORMATION;
					case GREAT_VOID -> Realm.HEAVENLY_BEING;
					case IMMORTAL -> Realm.ENTERING_NIRVANA;
				};
				Realm high = switch (world) {
					case LOWER -> Realm.HEAVENLY_BEING;
					case UPPER -> Realm.SACRED_VESSEL;
					case GREAT_VOID -> Realm.ENTERING_NIRVANA;
					case IMMORTAL -> Realm.TRUE_IMMORTAL;
				};
				if (world == MethodManualItem.World.IMMORTAL && realm == Realm.TRUE_IMMORTAL) immortal++;
				helper.assertTrue(MethodManualItem.method(kept) == CultivationMethod.QI_CIRCULATION && realm != Realm.FOUR_AXIS
						&& realm.ordinal() >= low.ordinal() && realm.ordinal() <= high.ordinal(),
						"A library's Qi Circulation volume of its world's realms (" + world + ": " + realm + ")");
				if (world == MethodManualItem.World.LOWER && realm.ordinal() >= Realm.NASCENT_SOUL.ordinal()) lowerDeep++;
				if (world == MethodManualItem.World.GREAT_VOID && realm.ordinal() >= Realm.VOID_SHATTERING.ordinal()) greatVoidDeep++;
			}
			// (Of a hundred lower libraries some sixteen keep Nascent Soul's or Heavenly Being's: the lower of two draws of five; of a hundred
			// of the Great Void's some twenty-five Void Shattering's to Entering Nirvana's: the lower of two of six.)
			helper.assertTrue(lowerDeep > 0 && lowerDeep < 40, "A lower library keeps Nascent Soul's and Heavenly Being's now and then (" + lowerDeep + " of 100)");
			helper.assertTrue(greatVoidDeep > 0 && greatVoidDeep < 50, "A Great Void library keeps Void Shattering's to Entering Nirvana's now and then (" + greatVoidDeep + " of 100)");
			// (Of a hundred of the Immortal Realm's some twenty-five True Immortal's: the lower of two of two.)
			helper.assertTrue(immortal > 5 && immortal < 50, "An Immortal Realm library keeps True Immortal's a quarter of the time (" + immortal + " of 100)");
			boolean nirvana = false;
			for (int i = 0; i < 2000 && !nirvana; i++) nirvana = MethodManualItem.realm(MethodManualItem.circulation(MethodManualItem.World.GREAT_VOID, random)) == Realm.ENTERING_NIRVANA;
			helper.assertTrue(nirvana, "...and now and then Entering Nirvana's");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	// --- Bodies born against a method ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBodyBornAgainstAMethodRefusesIt(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		plain(c);
		helper.assertTrue(CultivationMethods.refusal(c, CultivationMethod.CORPSE_YIN) == null && CultivationMethods.refusal(c, CultivationMethod.FIVE_ELEMENTS) == null
				&& CultivationMethods.refusal(c, CultivationMethod.HEAVEN_RITES) == null, "Five roots: nothing barred");
		Map<Physique, List<CultivationMethod>> bars = Map.of(
				Physique.HEAVEN_SACRED_BODY, List.of(CultivationMethod.CORPSE_YIN, CultivationMethod.HAUNTING, CultivationMethod.SOUL_RIVER_FEAST,
						CultivationMethod.RIFT_HAUNTING, CultivationMethod.SOUL_FURNACE, CultivationMethod.TEN_THOUSAND_GHOSTS, CultivationMethod.BORROWED_LIFE),
				Physique.BLOOD_DEMON_BODY, List.of(CultivationMethod.HEAVEN_RITES, CultivationMethod.INCENSE_FAITH, CultivationMethod.JUDGEMENT_HALL_SERVICE),
				Physique.SOUL_DEVOURING_BODY, List.of(CultivationMethod.HEAVEN_RITES, CultivationMethod.INCENSE_FAITH, CultivationMethod.JUDGEMENT_HALL_SERVICE),
				Physique.HEAVEN_DEFYING_BODY, List.of(CultivationMethod.HEAVEN_RITES, CultivationMethod.INCENSE_FAITH),
				Physique.NINE_YANG_BODY, List.of(CultivationMethod.CORPSE_YIN, CultivationMethod.YELLOW_SPRINGS_IMMERSION, CultivationMethod.SOUL_FURNACE),
				Physique.GHOST_DEVIL_BODY, List.of(CultivationMethod.HEAVEN_RITES));
		for (Map.Entry<Physique, List<CultivationMethod>> body : bars.entrySet()) {
			c.setPhysique(body.getKey());
			for (CultivationMethod method : CultivationMethod.values()) {
				boolean barred = body.getValue().contains(method);
				helper.assertTrue(body.getKey().bars(method) == barred && (CultivationMethods.barredBy(c, method) == body.getKey()) == barred
						&& (!barred || key(CultivationMethods.refusal(c, method)).equals(ModLang.MSG_PHYSIQUE_BARS_METHOD)),
						body.getKey().getId() + (barred ? " bars " : " lets be ") + method.getId());
			}
		}
		// Roots of fewer than five can't cycle all five.
		c.setPhysique(Physique.TRUE_ELEMENT_ROOT);
		c.setRootElements(List.of(QiElement.FIRE));
		helper.assertTrue(CultivationMethods.barredBy(c, CultivationMethod.FIVE_ELEMENTS) == Physique.TRUE_ELEMENT_ROOT
				&& CultivationMethods.barredBy(c, CultivationMethod.QI_CIRCULATION) == null, "A single root: no Five Elements Cycling, Qi Circulation as ever");
		c.setPhysique(Physique.THREE_ELEMENT_ROOTS);
		c.setRootElements(List.of(QiElement.FIRE, QiElement.WOOD, QiElement.EARTH));
		helper.assertTrue(CultivationMethods.barredBy(c, CultivationMethod.FIVE_ELEMENTS) == Physique.THREE_ELEMENT_ROOTS, "Three roots: none either");
		// A Myriad Form Body's forms bar as their own bodies do.
		c.setPhysique(Physique.MYRIAD_FORM_BODY);
		c.setRootElements(List.of());
		c.setForms(List.of(Physique.NINE_YANG_BODY, Physique.ANCIENT_HOLY_BODY));
		helper.assertTrue(CultivationMethods.barredBy(c, CultivationMethod.CORPSE_YIN) == Physique.NINE_YANG_BODY, "A form of the Nine Yang Body bars the corpses' yin");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aManualOfABarredMethodTeachesNothing(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		try {
			c.setPhysique(Physique.HEAVEN_SACRED_BODY);
			read(player, MethodManualItem.of(CultivationMethod.CORPSE_YIN, CultivationMethod.CORPSE_YIN.first()));
			helper.assertTrue(!CultivationMethods.knows(c, CultivationMethod.CORPSE_YIN), "A Heaven Sacred Body reads Corpse Yin Refining and learns nothing");
			read(player, MethodManualItem.of(CultivationMethod.QI_CIRCULATION, Realm.QI_REFINING));
			helper.assertTrue(CultivationMethods.knows(c, CultivationMethod.QI_CIRCULATION), "...Qi Circulation as anyone");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	// --- The old worlds ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void anOldWorldsScripturesLeaveTheirVolumes(GameTestHelper helper) {
		PlayerCultivation old = new PlayerCultivation();
		old.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		old.heaven().setMethod(HeavenMethod.of(QiElement.EARTH, Realm.FOUNDATION_BUILDING), 1, 0);
		old.netherworld().setMethod(GhostMethod.YELLOW_SPRINGS_MARROW, 1, 0);
		CompoundTag tag = old.save();
		helper.assertTrue(tag.getInt("ManualsModel") > 0, "Saved now, the record says so");
		tag.remove("ManualsModel"); // as saved before 2026-11-16
		PlayerCultivation loaded = PlayerCultivation.load(tag);
		helper.assertTrue(loaded.methodReach(CultivationMethod.QI_CIRCULATION) == Realm.FOUNDATION_BUILDING.ordinal()
				&& loaded.methodReach(CultivationMethod.NETHERWORLD_BREATHING) == Realm.FOUNDATION_BUILDING.ordinal(),
				"A world of before: the volumes its scriptures and foundations carried kept as Qi Circulation's and Breathing's own");
		PlayerCultivation since = PlayerCultivation.load(old.save());
		helper.assertTrue(since.methodReach(CultivationMethod.QI_CIRCULATION) < 0, "Saved since: a scripture carries nothing");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSoulRiverTakesAShareOfTheStageNotALayer(GameTestHelper helper) {
		ServerPlayer player = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(player);
		try {
			NetherworldState s = c.netherworld();
			s.setMethod(GhostMethod.YELLOW_SPRINGS_MARROW, 1, 0);
			int rank = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID);
			double required = GhostMethods.required(rank);
			s.setPath(rank, required * 0.6);
			SoulRiverFeast.forgetMemory(player);
			helper.assertTrue(Math.abs(s.pathCultivation() - required * (0.6 - SoulRiverFeast.MEMORY_SHARE)) < 1.0e-6 && GhostMethods.ghostRank(s) == rank
					&& s.layer(GhostMethod.YELLOW_SPRINGS_MARROW) == 14, "A memory: a quarter of the stage gone, never a stage, the foundation as far as the path");
		} finally {
			leave(player);
		}
		helper.succeed();
	}

	// --- Loot ---

	private static LootParams chest(GameTestHelper helper) {
		return new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(1, 2, 1))).create(LootContextParamSets.CHEST);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theVanillaWorldsChestsKeepTheModsGoods(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		LootTable village = server.getLootData().getLootTable(BuiltInLootTables.VILLAGE_PLAINS_HOUSE);
		int stones = 0, villageWritings = 0;
		for (int i = 0; i < 300; i++) {
			for (ItemStack stack : village.getRandomItems(chest(helper))) {
				if (stack.getItem() instanceof SpiritStoneItem) stones++;
				if (stack.is(ModItems.METHOD_MANUAL) || stack.is(ModItems.TECHNIQUE_MANUAL) || stack.is(ModItems.TECHNIQUE_JADE_SLIP)) villageWritings++;
			}
		}
		helper.assertTrue(stones > 0 && villageWritings == 0, "A village's chests: spirit stones now and then, no writings (" + stones + ")");
		record Want(ResourceLocation table, Realm low, Realm high) {}
		for (Want want : List.of(new Want(BuiltInLootTables.SIMPLE_DUNGEON, Realm.QI_REFINING, Realm.FOUNDATION_BUILDING),
				new Want(BuiltInLootTables.ANCIENT_CITY, Realm.CORE_FORMATION, Realm.NASCENT_SOUL),
				new Want(BuiltInLootTables.END_CITY_TREASURE, Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING))) {
			LootTable table = server.getLootData().getLootTable(want.table());
			int methods = 0, arts = 0, slips = 0, goods = 0;
			for (int i = 0; i < 300; i++) {
				for (ItemStack stack : table.getRandomItems(chest(helper))) {
					helper.assertTrue(!stack.is(ModItems.JADE_SLIP) && !stack.is(ModItems.GHOST_JADE_SLIP), "No method jade slip");
					if (stack.getItem() instanceof SpiritStoneItem || stack.getItem() instanceof PillItem || stack.getItem() instanceof GinsengItem) goods++;
					if (stack.is(ModItems.METHOD_MANUAL)) {
						Realm realm = MethodManualItem.realm(stack);
						helper.assertTrue(realm.ordinal() >= want.low().ordinal() && realm.ordinal() <= want.high().ordinal()
								&& MethodManualItem.method(stack).spans(realm), want.table() + ": a volume of its realms (" + realm + ")");
						methods++;
					}
					if (stack.is(ModItems.TECHNIQUE_MANUAL)) arts++;
					if (stack.is(ModItems.TECHNIQUE_JADE_SLIP)) slips++;
				}
			}
			boolean end = want.table().equals(BuiltInLootTables.END_CITY_TREASURE);
			helper.assertTrue(goods > 0 && methods > 0 && arts > 0 && (end ? slips > 0 : slips == 0),
					want.table() + ": the mod's goods, Method Manuals, Art Manuals" + (end ? ", technique slips" : "") + " (" + goods + ", " + methods + ", " + arts + ", " + slips + ")");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noLootTableKeepsAMethodJadeSlip(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		var tables = server.getResourceManager().listResources("loot_tables", id -> id.getNamespace().equals(DefyingTheHeavens.MOD_ID) && id.getPath().endsWith(".json"));
		for (var table : tables.entrySet()) {
			StringBuilder text = new StringBuilder();
			try (BufferedReader reader = table.getValue().openAsReader()) {
				reader.lines().forEach(text::append);
			} catch (java.io.IOException unreadable) {
				throw new IllegalStateException(table.getKey() + " can't be read", unreadable);
			}
			String json = text.toString();
			helper.assertTrue(!json.contains(DefyingTheHeavens.MOD_ID + ":jade_slip\"") && !json.contains(DefyingTheHeavens.MOD_ID + ":ghost_jade_slip\""),
					table.getKey() + " keeps no method jade slip");
		}
		helper.assertTrue(tables.size() > 10, "Every table read (" + tables.size() + ")");
		helper.succeed();
	}

	/**
	 * The deep ghost foundations, which the Nine Nether Peaks' steles taught before a stele held a method, keep to the Yellow Springs
	 * Hollow's vaults (the user's pick, 2026-11-16): each now and then, the deeper the rarer, one a hoard at most, with Breathing's manual
	 * written to its realm beside it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeepGhostFoundationsKeepToTheHollowsVaults(GameTestHelper helper) {
		RandomSource random = RandomSource.create(1116);
		Map<GhostMethod, Integer> found = new java.util.EnumMap<>(GhostMethod.class);
		int hoards = 20000;
		for (int i = 0; i < hoards; i++) {
			List<ItemStack> hoard = SpringsHollowWorks.vaultTreasures(random);
			GhostMethod deep = null;
			for (ItemStack stack : hoard) {
				GhostMethod art = stack.is(ModItems.TECHNIQUE_MANUAL) ? TechniqueManualItem.ghostArt(stack) : null;
				if (art == null || art == GhostMethod.NINE_NETHER_SOUL_SUTRA) continue;
				helper.assertTrue(deep == null, "One deep foundation a hoard at most");
				deep = art;
			}
			if (deep == null) continue;
			helper.assertTrue(List.of(SpringsHollowWorks.DEEP_FOUNDATIONS).contains(deep), deep.getId() + " is one of the deep ones");
			GhostMethod kept = deep;
			helper.assertTrue(hoard.stream().anyMatch(s -> s.is(ModItems.METHOD_MANUAL) && MethodManualItem.method(s) == CultivationMethod.NETHERWORLD_BREATHING
					&& MethodManualItem.realm(s) == kept.realm()), deep.getId() + " comes with Breathing's manual of its realm");
			found.merge(deep, 1, Integer::sum);
		}
		int total = 0;
		for (int i = 0; i < SpringsHollowWorks.DEEP_FOUNDATIONS.length; i++) {
			GhostMethod deep = SpringsHollowWorks.DEEP_FOUNDATIONS[i];
			int count = found.getOrDefault(deep, 0);
			total += count;
			double expected = hoards * SpringsHollowWorks.DEEP_CHANCES[i] / 100.0;
			helper.assertTrue(Math.abs(count - expected) < expected * 0.25, deep.getId() + ": " + count + " in " + hoards + " hoards, about " + expected);
			if (i > 0) helper.assertTrue(count < found.getOrDefault(SpringsHollowWorks.DEEP_FOUNDATIONS[i - 1], 0), deep.getId() + " rarer than the one before");
		}
		helper.assertTrue(Math.abs(total - hoards * 0.15) < hoards * 0.015, "About fifteen hoards in a hundred keep one (" + total + ")");
		helper.succeed();
	}
}
