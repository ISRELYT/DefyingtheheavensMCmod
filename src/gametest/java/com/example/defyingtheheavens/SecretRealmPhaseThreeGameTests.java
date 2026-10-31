package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The secret realms' third phase (2026-10-31): the Primeval Beast Graveyard, the Cave Abode of a Heavenly Being and the Yellow Springs Hollow:
 * who gets into each (the Hollow the dead too) and who flies freely; their lands from the world's seed (the echo's ground dry, the abode's
 * halls under rock, the Hollow's springs ringing its tomb and its islands joined); the Graveyard's beasts past their kind; the Primeval Blood
 * Essence; the Marrow-Washing Fruit; the maze's way and its walls' hold; the Ferryman's judgement; the Hollow's guardians of the dead; the
 * devilish slipping out as a realm quakes; and a whole opening of each, laid, its pillars lit in their order, and shut.
 */
public class SecretRealmPhaseThreeGameTests implements FabricGameTest {
	private static final SecretRealm GRAVE = SecretRealm.PRIMEVAL_BEAST_GRAVEYARD, ABODE = SecretRealm.CAVE_ABODE, HOLLOW = SecretRealm.YELLOW_SPRINGS_HOLLOW;

	/** A player in survival (a mock player answers that it is creative), out of the player list. */
	private static ServerPlayer survivor(GameTestHelper helper, String name) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
		p.setGameMode(GameType.SURVIVAL);
		return p;
	}

	private static String key(Component c) {
		return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachAdmitsItsLowestAndTheHollowTheDead(GameTestHelper helper) {
		helper.assertTrue(!GRAVE.mayEnter(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID)) && GRAVE.mayEnter(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE)),
				"The Graveyard from Core Formation Late");
		for (SecretRealm realm : List.of(ABODE, HOLLOW)) {
			helper.assertTrue(!realm.mayEnter(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID)) && realm.mayEnter(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.LATE)),
					realm.getId() + " from Nascent Soul Late");
			helper.assertTrue(!realm.fliesFreely(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)) && realm.fliesFreely(PlayerCultivation.rank(Realm.DAO_INTEGRATION,
					Stage.EARLY)), realm.getId() + ": Dao Integration flies freely over it");
		}
		helper.assertTrue(GRAVE.fliesFreely(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY)), "Over the Graveyard, Four Axis");
		helper.assertTrue(HOLLOW.admitsDead() && !ABODE.admitsDead() && !GRAVE.admitsDead(), "Only the Hollow admits the dead");
		helper.assertTrue(HOLLOW.home() == ModDimensions.NETHERWORLD && ABODE.home() == net.minecraft.world.level.Level.OVERWORLD, "The Hollow's Seal in the Netherworld");
		Set<Integer> slots = new HashSet<>();
		for (SecretRealm realm : SecretRealm.values()) {
			slots.add(realm.centre().getX());
			helper.assertTrue(SecretRealm.at(realm.centre().getX(), realm.centre().getZ()) == realm, realm.getId() + " in its own slot");
		}
		helper.assertTrue(slots.size() == SecretRealm.values().length, "No two in one slot");
		// The dead are turned back from every realm but the Hollow.
		ServerPlayer ghost = survivor(helper, "ghost");
		try {
			CultivationManager.get(ghost).setState(Realm.HEAVENLY_BEING, Stage.EARLY, 0);
			CultivationManager.get(ghost).netherworld().setGhost(true);
			helper.assertTrue(key(SecretRealms.whyNotEnter(ghost, ABODE)).equals(ModLang.MSG_SECRET_LIVING_ONLY), "The Cave Abode admits only the living");
			helper.assertTrue(SecretRealms.whyNotEnter(ghost, HOLLOW) == null, "The Hollow admits one of the dead: " + key(SecretRealms.whyNotEnter(ghost, HOLLOW)));
			helper.succeed();
		} finally {
			CultivationManager.get(ghost).netherworld().setGhost(false);
			GhostSystem.forget(ghost.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGraveyardIsLaidFromItsSeed(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();
		BeastGraveyard.Shape s = BeastGraveyard.shape(seed);
		BlockPos skull = BeastGraveyard.skull(s);
		helper.assertTrue(BeastGraveyard.column(s, skull.getX(), skull.getZ()).kind() == BeastGraveyard.Kind.FLOOR, "The skull lies on the floor");
		BlockPos c = GRAVE.centre(), post = BeastGraveyardWorks.echoPost(s, c);
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				BeastGraveyard.Column col = BeastGraveyard.column(s, post.getX() - c.getX() + dx, post.getZ() - c.getZ() + dz);
				helper.assertTrue(col.kind() == BeastGraveyard.Kind.FLOOR, "The echo's ground before the skull is dry floor: " + col);
			}
		}
		helper.assertTrue(BeastGraveyard.column(s, 400, 0).kind() == BeastGraveyard.Kind.NONE, "Nothing past its edge");
		List<BlockPos> arrivals = BeastGraveyard.arrivals(s);
		helper.assertTrue(arrivals.size() == 4, "Four arrivals");
		for (BlockPos at : arrivals) helper.assertTrue(BeastGraveyard.column(s, at.getX(), at.getZ()).kind() == BeastGraveyard.Kind.FLOOR, "Each on the floor: " + at);
		List<RealmWorks.Slot> slots = GRAVE.works().guardianSlots(seed, c, GRAVE);
		long beasts = slots.stream().filter(slot -> slot.id().startsWith("beast")).count(), great = slots.stream().filter(slot -> slot.id().startsWith("great")).count();
		long keepers = slots.stream().filter(RealmWorks.Slot::gatekeeper).count();
		helper.assertTrue(beasts == 24 && great == 3 && keepers == 1, "24 beasts, three great ones, one trial guardian: " + beasts + ", " + great + ", " + keepers);
		helper.assertTrue(slots.stream().filter(RealmWorks.Slot::gatekeeper).allMatch(slot -> slot.type() == ModEntities.DRAGON_ECHO), "...the Dragon's Echo");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theAbodeIsLaidFromItsSeedItsHallsUnderRock(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();
		CaveAbode.Shape s = CaveAbode.shape(seed);
		CaveAbode.Column heart = CaveAbode.column(s, 0, 0);
		helper.assertTrue(heart.kind() == CaveAbode.Kind.MOUNTAIN && heart.ground() >= CaveAbode.PEAK_Y - 15, "The peak at its heart: " + heart);
		int lx = (int) Math.round(Math.cos(s.mainTurn) * CaveAbode.LAKE_R), lz = (int) Math.round(Math.sin(s.mainTurn) * CaveAbode.LAKE_R);
		helper.assertTrue(CaveAbode.column(s, lx, lz).kind() == CaveAbode.Kind.LAKE, "The lake before the gate, on the main way");
		for (BlockPos at : CaveAbode.arrivals(s)) helper.assertTrue(CaveAbode.column(s, at.getX(), at.getZ()).kind() == CaveAbode.Kind.FLOOR, "An arrival on the floor: " + at);
		BlockPos c = ABODE.centre();
		RealmWorks.Frame fr = CaveAbodeWorks.frame(s, c);
		for (CaveAbodeWorks.Room room : List.of(CaveAbodeWorks.HALL, CaveAbodeWorks.LIBRARY, CaveAbodeWorks.SPRINGS, CaveAbodeWorks.LECTURE, CaveAbodeWorks.ELEMENTS,
				CaveAbodeWorks.CHAMBER)) {
			for (int f : new int[] {room.f0() - 1, room.f1() + 1}) {
				for (int sd : new int[] {room.s0() - 1, room.s1() + 1}) {
					BlockPos at = fr.at(f, sd, 0);
					int ground = CaveAbode.column(s, at.getX() - c.getX(), at.getZ() - c.getZ()).ground();
					helper.assertTrue(ground >= CaveAbodeWorks.FLOOR + room.h() + 3, "A hall under the mountain's rock at " + f + ", " + sd + ": " + ground);
				}
			}
		}
		// The pillars: five, in the elements hall, none in the way in from its door.
		Map<ElementPillarBlock.Element, BlockPos> pillars = CaveAbodeWorks.pillars(s, c);
		helper.assertTrue(pillars.size() == 5 && new HashSet<>(pillars.values()).size() == 5, "Five pillars, each its own place");
		for (BlockPos at : pillars.values()) {
			int[] l = fr.local(at);
			helper.assertTrue(CaveAbodeWorks.ELEMENTS.inside(l[0], l[1]), "In the elements hall: " + l[0] + ", " + l[1]);
			helper.assertTrue(!(l[0] == 0 && l[1] > CaveAbodeWorks.ELEMENTS_MIDDLE_S), "Not in the way in: " + l[0] + ", " + l[1]);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theMazesWayIsLongAndItsWallsHold(GameTestHelper helper) {
		ServerLevel realms = helper.getLevel().getServer().getLevel(ModDimensions.SECRET_REALM);
		long seed = realms.getSeed();
		CaveAbodeWorks.Maze maze = CaveAbodeWorks.maze(seed);
		helper.assertTrue(maze.way.size() >= 10, "A long way from the entrance to the heart: " + maze.way.size() + " cells");
		for (int i = 1; i < maze.way.size(); i++) {
			int[] a = maze.way.get(i - 1), b = maze.way.get(i);
			helper.assertTrue(Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) == 1 && maze.passes(a[0], a[1], b[0] - a[0], b[1] - a[1]), "Each step through an open side");
		}
		helper.assertTrue(maze.way.get(0)[0] == 0 && maze.way.get(maze.way.size() - 1)[0] == CaveAbodeWorks.MAZE_MID, "From its entrance to its heart");
		// Every cell reached (a walk that never crosses its own track reaches them all).
		for (int i = 0; i < CaveAbodeWorks.CELLS; i++) {
			for (int j = 0; j < CaveAbodeWorks.CELLS; j++) helper.assertTrue(!maze.path(new int[] {0, CaveAbodeWorks.MAZE_MID}, new int[] {i, j}).isEmpty(), "Cell " + i + ", " + j);
		}
		helper.assertTrue(!maze.wall(0, CaveAbodeWorks.MAZE_MID * CaveAbodeWorks.CELL + 2) && maze.wall(0, 2), "Its near wall open only at its entrance");
		CaveAbode.Shape s = CaveAbode.shape(seed);
		RealmWorks.Frame fr = CaveAbodeWorks.frame(s, ABODE.centre());
		helper.assertTrue(CaveAbodeWorks.holds(realms, fr.at(CaveAbodeWorks.MAZE_F + 4, 0, 2)) && CaveAbodeWorks.holds(realms, fr.at(CaveAbodeWorks.MAZE_F + 8, 14, 5)),
				"Its walls and roof hold");
		helper.assertTrue(!CaveAbodeWorks.holds(realms, fr.at(0, 0, 1)) && !CaveAbodeWorks.holds(helper.getLevel(), fr.at(CaveAbodeWorks.MAZE_F + 4, 0, 2)),
				"The garden does not, nor the same place in another world");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theHollowIsLaidFromItsSeedItsIslandsJoined(GameTestHelper helper) {
		long seed = helper.getLevel().getSeed();
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		SpringsHollow.Column heart = SpringsHollow.column(s, 0, 0);
		helper.assertTrue(heart.kind() == SpringsHollow.Kind.TOMB && heart.ground() > SpringsHollow.WATER_Y, "The tomb's island at its heart: " + heart);
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4;
			SpringsHollow.Column ring = SpringsHollow.column(s, (int) Math.round(Math.cos(a) * 36), (int) Math.round(Math.sin(a) * 36));
			helper.assertTrue(ring.kind() == SpringsHollow.Kind.RIVER && ring.ground() <= SpringsHollow.RIVER_BED_Y + 3, "The springs ring it, deep: " + ring);
		}
		SpringsHollow.Isle landing = s.landing();
		helper.assertTrue(SpringsHollow.column(s, landing.x(), landing.z()).kind() == SpringsHollow.Kind.ISLE && landing.role().equals("landing"), "The landing's island");
		List<BlockPos> arrivals = SpringsHollow.arrivals(s);
		helper.assertTrue(arrivals.size() == 4, "Four arrivals");
		for (BlockPos at : arrivals) helper.assertTrue(SpringsHollow.column(s, at.getX(), at.getZ()).kind() == SpringsHollow.Kind.ISLE, "Each on an island: " + at);
		for (int i = 0; i < s.isles.size(); i++) {
			for (int j = i + 1; j < s.isles.size(); j++) {
				SpringsHollow.Isle a = s.isles.get(i), b = s.isles.get(j);
				helper.assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) > a.r() + b.r(), "Islands apart: " + a + ", " + b);
			}
		}
		// The bridges: none over the springs; the landing and every arrival joined.
		Set<Integer> joined = new HashSet<>();
		joined.add(0);
		for (int[] bridge : SpringsHollowWorks.bridges(s)) {
			helper.assertTrue(SpringsHollowWorks.clearOfSprings(s.isles.get(bridge[0]), s.isles.get(bridge[1])), "A bridge clear of the springs");
			joined.add(bridge[1]);
		}
		for (int i = 0; i < s.isles.size(); i++) {
			if (s.isles.get(i).role().equals("arrival")) helper.assertTrue(joined.contains(i), "Arrival island " + i + " joined to the landing's");
		}
		RealmWorks.Frame w = SpringsHollowWorks.way(s, HOLLOW.centre());
		BlockPos post = SpringsHollowWorks.ferryPost(w);
		helper.assertTrue(SpringsHollow.column(s, post.getX() - HOLLOW.centre().getX(), post.getZ() - HOLLOW.centre().getZ()).kind() == SpringsHollow.Kind.RIVER,
				"The Ferryman's post out over the springs");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGraveyardsBeastsGrowPastTheirKind(GameTestHelper helper) {
		int high = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.LATE);
		DemonBeast plain = helper.spawnWithNoFreeWill(ModEntities.beast(BeastKind.SHADOW_WOLF), new BlockPos(1, 2, 1));
		DemonBeast grown = helper.spawnWithNoFreeWill(ModEntities.beast(BeastKind.SHADOW_WOLF), new BlockPos(3, 2, 1));
		try {
			plain.setRank(high);
			grown.setBoundlessRank(high);
			helper.assertTrue(plain.getRank() == BeastKind.SHADOW_WOLF.maxRank() && plain.getRank() < high, "Of its kind, held to its kind's top: " + plain.getRank());
			helper.assertTrue(grown.getRank() == high && grown.boundless(), "On the dragon's bones, past it: " + grown.getRank());
			helper.assertTrue(grown.getMaxHealth() > plain.getMaxHealth(), "...and the stronger for it");
			CompoundTag saved = new CompoundTag();
			grown.saveWithoutId(saved);
			DemonBeast again = (DemonBeast) ModEntities.beast(BeastKind.SHADOW_WOLF).create(helper.getLevel());
			again.load(saved);
			helper.assertTrue(again.boundless() && again.getRank() == high, "Kept so when loaded again");
			helper.succeed();
		} finally {
			plain.discard();
			grown.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thePrimevalEssenceWakesADragonsBlood(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer human = survivor(helper, "essence");
		Bloodlines.set(server, human.getUUID(), Bloodline.HUMAN, 0);
		DemonPath.born(human);
		try {
			PlayerCultivation c = CultivationManager.get(human);
			c.awaken(0);
			helper.assertTrue(key(PrimevalBloodEssenceItem.why(c, false, false)).equals(ModLang.MSG_ESSENCE_WARN), "A human is warned first");
			helper.assertTrue(PrimevalBloodEssenceItem.why(c, true, false) == null, "...and may drink it sneaking");
			human.setShiftKeyDown(true);
			helper.assertTrue(PrimevalBloodEssenceItem.drink(human), "Drunk sneaking");
			helper.assertTrue(DemonPath.isHalfBlood(c) && c.demon().halfBlood() == Bloodline.DRAGON, "...a dragon's blood wakes in them");
			double purity = c.demon().purity();
			human.setShiftKeyDown(false);
			helper.assertTrue(PrimevalBloodEssenceItem.why(c, false, false) == null && PrimevalBloodEssenceItem.drink(human) && c.demon().purity() > purity,
					"A half-blood drinks again unwarned, its blood the purer: " + purity + " to " + c.demon().purity());
			helper.succeed();
		} finally {
			Bloodlines.forget(server, human.getUUID());
			RaceGifts.readyAgain(human.getUUID());
			RaceGifts.forget(human.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theMarrowWashingFruitRaisesAPhysiqueOnce(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, "marrow");
		PlayerCultivation c = CultivationManager.get(p);
		c.setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		c.awaken(0);
		double talent = c.physiqueTalent(), speed = c.physiqueSpeed();
		helper.assertTrue(MarrowWashingFruitItem.why(c, false) == null && c.physiqueGrade() == Physique.Grade.LOW, "A Low physique may eat it");
		ItemStack fruit = new ItemStack(ModItems.MARROW_WASHING_FRUIT, 2);
		fruit.finishUsingItem(p.level(), p);
		helper.assertTrue(c.isMarrowWashed() && c.physiqueGrade() == Physique.Grade.MID, "Its marrow washed: Mid grade now");
		helper.assertTrue(c.physiqueTalent() > talent && c.physiqueSpeed() >= speed && c.physique() == Physique.FIVE_ELEMENT_ROOTS, "Its talent a grade's higher, its body the same");
		helper.assertTrue(fruit.getCount() == 1, "The fruit eaten");
		helper.assertTrue(key(MarrowWashingFruitItem.why(c, false)).equals(ModLang.MSG_MARROW_ALREADY), "Once in a life");
		helper.assertTrue(PlayerCultivation.load(c.save()).isMarrowWashed(), "Kept when saved");
		PlayerCultivation highest = new PlayerCultivation();
		highest.setPhysique(Physique.FALLEN_TRUE_IMMORTAL);
		highest.awaken(0);
		helper.assertTrue(key(MarrowWashingFruitItem.why(highest, false)).equals(ModLang.MSG_MARROW_HIGHEST), "Nothing above Immortal");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFerrymanJudgesByDeeds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		SecretRealms.Record r = new SecretRealms.Record(HOLLOW);
		r.openedAt = level.getServer().overworld().getGameTime();
		ServerPlayer righteous = survivor(helper, "righteous"), neutral = survivor(helper, "neutral"), poor = survivor(helper, "poor"), devilish = survivor(helper, "devilish");
		CultivationManager.get(righteous).setAlignment(150);
		CultivationManager.get(devilish).setAlignment(-150);
		neutral.getInventory().add(new ItemStack(ModItems.YIN_JADE, SpringsHollowWorks.TOLL_JADE + 1));
		RealmWorks works = HOLLOW.works();
		helper.assertTrue(works.mayChallenge(r, righteous) == null && works.passesUnfought(level, r, righteous), "The righteous he takes for nothing");
		helper.assertTrue(works.mayChallenge(r, neutral) == null && works.passesUnfought(level, r, neutral), "The neutral for his toll");
		helper.assertTrue(neutral.getInventory().countItem(ModItems.YIN_JADE) == 1, "...taken: " + neutral.getInventory().countItem(ModItems.YIN_JADE));
		helper.assertTrue(key(works.mayChallenge(r, poor)).equals(ModLang.MSG_FERRY_TOLL_SHORT), "One who can't pay is told his toll");
		helper.assertTrue(works.mayChallenge(r, devilish) == null && !works.passesUnfought(level, r, devilish), "The devilish must best him");
		RealmWorks.pass(r, "Passed", righteous.getUUID());
		helper.assertTrue(key(works.mayChallenge(r, righteous)).equals(ModLang.MSG_FERRY_JUDGED), "One judged is not judged again");
		helper.assertTrue(r.realm.works() == SpringsHollowWorks.WORKS && RealmWorks.barrierLets(r, righteous.getUUID()) && !RealmWorks.barrierLets(r, devilish.getUUID()),
				"The tomb's door lets in those he judged");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theHollowsGuardiansAreOfTheDead(GameTestHelper helper) {
		ServerLevel realms = helper.getLevel().getServer().getLevel(ModDimensions.SECRET_REALM);
		RealmGuardian shade = ModEntities.DROWNED_SHADE.create(realms), puppet = ModEntities.BRONZE_PUPPET.create(realms), elsewhere = ModEntities.DROWNED_SHADE.create(helper.getLevel());
		BlockPos hollow = HOLLOW.centre(), abode = ABODE.centre();
		shade.moveTo(hollow.getX() + 0.5, hollow.getY(), hollow.getZ() + 0.5);
		puppet.moveTo(abode.getX() + 0.5, abode.getY(), abode.getZ() + 0.5);
		elsewhere.moveTo(hollow.getX() + 0.5, hollow.getY(), hollow.getZ() + 0.5);
		helper.assertTrue(shade.ofTheDead() && GhostSystem.isDead(shade), "A guardian of the Hollow is of the dead");
		helper.assertTrue(!puppet.ofTheDead() && !GhostSystem.isDead(puppet), "The Cave Abode's are not");
		helper.assertTrue(!elsewhere.ofTheDead(), "Nor one outside the realms' world");
		helper.assertTrue(shade.kind() == RealmGuardian.Kind.DROWNED_SHADE && ModEntities.FERRYMAN.create(realms).kind() == RealmGuardian.Kind.FERRYMAN
				&& ModEntities.MASTER_PUPPET.create(realms).kind() == RealmGuardian.Kind.MASTER_PUPPET, "Each its own kind");
		helper.succeed();
	}

	/**
	 * As a realm quakes, the strongest two of the devilish inside slip out (their place on the roll marked so, waiting till two minutes after it
	 * shuts), wherever they stand; not a third, nor the righteous, nor an heir; and none before anyone went in.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theDevilishSlipOutToLieInWait(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		SecretRealms.Record r = new SecretRealms.Record(GRAVE);
		r.seal = helper.absolutePos(new BlockPos(1, 1, 1));
		r.openedAt = server.overworld().getGameTime();
		ListTag roll = new ListTag();
		int[][] who = {{-150, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.LATE)}, {-120, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID)},
				{-180, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY)}, {150, PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY)},
				{-200, PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.LATE)}};
		for (int[] w : who) {
			CompoundTag m = new CompoundTag();
			m.putUUID("Id", UUID.randomUUID());
			m.putInt("Rank", w[1]);
			m.putInt("Align", w[0]);
			m.putInt("State", SecretRealmRivals.INSIDE);
			roll.add(m);
		}
		roll.getCompound(4).putBoolean("Inheritor", true);
		r.rivals.put("Roll", roll);
		SecretRealmRivals.quaking(server, SecretRealms.Data.get(server), r, server.overworld().getGameTime());
		helper.assertTrue(roll.getCompound(0).getInt("State") == SecretRealmRivals.INSIDE, "None slip out of an opening nobody went into");
		r.rivals.putBoolean("Raised", true);
		SecretRealmRivals.quaking(server, SecretRealms.Data.get(server), r, server.overworld().getGameTime());
		int[] state = new int[roll.size()];
		for (int i = 0; i < roll.size(); i++) state[i] = roll.getCompound(i).getInt("State");
		helper.assertTrue(state[0] == SecretRealmRivals.AMBUSH && state[1] == SecretRealmRivals.AMBUSH, "The strongest two of the devilish slip out");
		helper.assertTrue(state[2] == SecretRealmRivals.INSIDE && state[3] == SecretRealmRivals.INSIDE && state[4] == SecretRealmRivals.INSIDE,
				"...not the third, nor the righteous however strong, nor the heir");
		helper.assertTrue(roll.getCompound(0).getLong("AmbushUntil") == r.openedAt + GRAVE.insideTicks() + SecretRealmRivals.AMBUSH_TICKS,
				"Waiting till two minutes after it shuts");
		helper.succeed();
	}

	/** A whole opening of each, out of turn: laid, its heart's things in place, the Cave Abode's pillars lit in their order; then shut. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "secret_realm_phase_three_cycle", timeoutTicks = 1200)
	public void theThirdPhasesRealmsOpenAndClose(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		SecretRealms.inTests = true;
		try {
			SecretRealms.Data data = SecretRealms.Data.get(server);
			ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
			for (SecretRealm realm : List.of(GRAVE, ABODE, HOLLOW)) {
				SecretRealms.Record r = data.of(realm);
				if (r.openedAt >= 0) SecretRealms.closeNow(server, realm);
				SecretRealms.openNow(server, realm);
				helper.assertTrue(r.openedAt >= 0 && r.laid && r.seal != null && r.sealBuilt, realm.getId() + ": open, its land laid and its Seal built");
			}
			ServerLevel netherworld = server.getLevel(ModDimensions.NETHERWORLD);
			BlockPos hollowSeal = data.of(HOLLOW).seal;
			helper.assertTrue(netherworld.getBlockState(hollowSeal).is(ModBlocks.SECRET_REALM_GATE) || netherworld.getBlockState(hollowSeal.above()).is(ModBlocks.SECRET_REALM_GATE),
					"The Hollow's Seal stands in the Netherworld: " + hollowSeal);
			// The Graveyard: the dragon's skull of bone.
			BeastGraveyard.Shape gs = BeastGraveyard.shape(realms.getSeed());
			BlockPos skull = BlockPos.containing(BeastGraveyardWorks.skull(gs, GRAVE.centre()).origin());
			int bones = 0;
			for (BlockPos p : BlockPos.betweenClosed(skull.offset(-8, -1, -8), skull.offset(8, 12, 8))) if (realms.getBlockState(p).is(Blocks.BONE_BLOCK)) bones++;
			helper.assertTrue(bones >= 40, "The dragon's skull, of bone: " + bones);
			// The Cave Abode: its gate, the sky well, the maze's heart, the barrier, the master's bones; the pillars, lit in their order.
			CaveAbode.Shape as = CaveAbode.shape(realms.getSeed());
			RealmWorks.Frame fr = CaveAbodeWorks.frame(as, ABODE.centre());
			helper.assertTrue(realms.getBlockState(fr.at(-58, 8, 5)).is(Blocks.CHISELED_STONE_BRICKS), "The gate's front, carved at its edge");
			helper.assertTrue(realms.getBlockState(fr.at(0, 0, CaveAbodeWorks.GARDEN.h() + 1)).isAir(), "The shaft of sky over the garden");
			helper.assertTrue(realms.getBlockState(CaveAbodeWorks.cellMiddle(fr, CaveAbodeWorks.MAZE_MID, CaveAbodeWorks.MAZE_MID).below()).is(Blocks.SEA_LANTERN), "The maze's heart");
			for (BlockPos p : CaveAbodeWorks.barrier(fr)) helper.assertTrue(realms.getBlockState(p).is(ModBlocks.TRIAL_BARRIER), "The chamber's barrier");
			helper.assertTrue(realms.getBlockState(CaveAbodeWorks.masterBones(fr)).is(ModBlocks.CORPSE), "The master's bones in his seat");
			Map<ElementPillarBlock.Element, BlockPos> pillars = CaveAbodeWorks.pillars(as, ABODE.centre());
			for (BlockPos p : pillars.values()) {
				helper.assertTrue(realms.getBlockState(p).is(ModBlocks.ELEMENT_PILLAR) && realms.getBlockState(p.above()).is(ModBlocks.ELEMENT_PILLAR), "A pillar two stones high");
			}
			SecretRealms.Record ar = data.of(ABODE);
			ServerPlayer p = survivor(helper, "pillars");
			helper.assertTrue(CaveAbodeWorks.light(realms, ar, data, ElementPillarBlock.Element.FIRE, p) && CaveAbodeWorks.lit(realms, pillars.get(ElementPillarBlock.Element.FIRE)),
					"Begun where one will: fire");
			helper.assertTrue(!CaveAbodeWorks.light(realms, ar, data, ElementPillarBlock.Element.METAL, p), "Metal after fire, out of turn: the backlash");
			for (BlockPos at : pillars.values()) helper.assertTrue(!CaveAbodeWorks.lit(realms, at), "...and all five dark");
			for (ElementPillarBlock.Element e : List.of(ElementPillarBlock.Element.WATER, ElementPillarBlock.Element.WOOD, ElementPillarBlock.Element.FIRE,
					ElementPillarBlock.Element.EARTH, ElementPillarBlock.Element.METAL)) {
				helper.assertTrue(CaveAbodeWorks.light(realms, ar, data, e, p), "In their order: " + e);
			}
			helper.assertTrue(RealmWorks.passed(ar, "Elements", p.getUUID()), "All five lit: passed");
			// The Hollow: its tomb's door, its king's bones, the bell on the far jetty.
			SpringsHollow.Shape hs = SpringsHollow.shape(realms.getSeed());
			RealmWorks.Frame t = SpringsHollowWorks.tomb(hs, HOLLOW.centre()), w = SpringsHollowWorks.way(hs, HOLLOW.centre());
			for (BlockPos door : SpringsHollowWorks.doorway(t)) helper.assertTrue(realms.getBlockState(door).is(ModBlocks.TRIAL_BARRIER), "The tomb's barrier");
			helper.assertTrue(realms.getBlockState(t.at(-3, 0, 2)).is(ModBlocks.CORPSE), "The Ghost King's bones on his coffin");
			helper.assertTrue(realms.getBlockState(SpringsHollowWorks.bell(w)).is(Blocks.BELL), "The bell on the far jetty");
			for (SecretRealm realm : List.of(GRAVE, ABODE, HOLLOW)) {
				SecretRealms.closeNow(server, realm);
				helper.assertTrue(data.of(realm).openedAt < 0, realm.getId() + " shut again");
			}
			helper.succeed();
		} finally {
			SecretRealms.inTests = false;
		}
	}
}
