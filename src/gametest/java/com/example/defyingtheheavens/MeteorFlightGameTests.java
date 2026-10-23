package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Meteor Flight (the user's ask, 2026-10-23): Qi Flight at creative flight's speed at every realm, and its fast mode (MeteorFlight):
 * a top speed climbing with the realm, three times the qi flying fast (from Heavenly Being never more than one gathers), the sound
 * barrier broken once, the heat from Nascent Soul at top speed, and the ablaze tearing through what the great spells' wounds may take,
 * unless the gamerule says no. The steering, the pose and the sights are the client's, seen in game.
 */
public class MeteorFlightGameTests implements FabricGameTest {
	/** A living cultivator in survival, out of the player list (no tick of the server's moves it), of {@code realm} {@code stage}. */
	private static ServerPlayer cultivator(ServerLevel level, Realm realm, Stage stage) {
		ServerPlayer player = FakePlayer.get(level, new GameProfile(UUID.randomUUID(), "meteor"));
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(realm, stage, 0);
		c.setLowerRealmBound(false);
		CultivationManager.refresh(player);
		c.setQi(c.maxQi());
		return player;
	}

	/** {@code p} in the air on Qi Flight with Meteor Flight switched {@code meteor}. */
	private static PlayerCultivation flying(ServerPlayer p, boolean meteor) {
		PlayerCultivation c = CultivationManager.get(p);
		c.setAbilityEnabled(Ability.QI_FLIGHT, true);
		c.setAbilityEnabled(Ability.METEOR_FLIGHT, meteor);
		QiFlight.tick(p, c);
		p.getAbilities().flying = true;
		return c;
	}

	private static void forget(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			QiFlight.forget(p.getUUID());
			GhostSystem.forget(p.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void qiFlightIsCreativeSpeedAtEveryRealm(GameTestHelper helper) {
		ServerPlayer core = cultivator(helper.getLevel(), Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer immortal = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		ServerPlayer dead = cultivator(helper.getLevel(), Realm.SACRED_VESSEL, Stage.MID);
		try {
			for (ServerPlayer p : new ServerPlayer[] {core, immortal}) {
				PlayerCultivation c = flying(p, false);
				QiFlight.tick(p, c);
				helper.assertTrue(p.getAbilities().mayfly && p.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED,
						"Qi Flight at creative flight's speed: " + c.getRealm().getId() + " (" + p.getAbilities().getFlyingSpeed() + ")");
				helper.assertTrue(!c.isAbilityEnabled(Ability.METEOR_FLIGHT) && Ability.METEOR_FLIGHT.isUnlocked(c), "Meteor Flight awakened, off until switched on");
			}
			GhostSystem.makeGhost(dead);
			PlayerCultivation d = CultivationManager.get(dead);
			QiFlight.tick(dead, d);
			helper.assertTrue(dead.getAbilities().mayfly && dead.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED,
					"The dead fly freely at the same speed (" + dead.getAbilities().getFlyingSpeed() + ")");
			helper.assertTrue(Ability.METEOR_FLIGHT.isUnlocked(d), "...and have Meteor Flight as the living do");
			ServerPlayer foundation = cultivator(helper.getLevel(), Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
			helper.assertTrue(!Ability.METEOR_FLIGHT.isUnlocked(CultivationManager.get(foundation)), "Not before Core Formation");
			forget(foundation);
		} finally {
			forget(core, immortal, dead);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itsTopSpeedClimbsWithTheRealm(GameTestHelper helper) {
		helper.assertTrue(MeteorFlight.topSpeed(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION) == 0, "None below Core Formation");
		helper.assertTrue(Math.abs(MeteorFlight.topSpeed(Realm.CORE_FORMATION, Stage.EARLY) - 45) < 1e-9
				&& Math.abs(MeteorFlight.topSpeed(Realm.TRUE_IMMORTAL, Stage.EARLY) - 200) < 1e-9, "Core Formation 45 blocks a second, True Immortal 200");
		double last = 0;
		for (Realm realm : Realm.values()) {
			if (realm.ordinal() < Realm.CORE_FORMATION.ordinal()) continue;
			for (Stage stage : Stage.values()) {
				double top = MeteorFlight.topSpeed(realm, stage);
				helper.assertTrue(top > last, "Swifter with every stage: " + realm.getId() + " " + stage.getId() + " (" + top + ")");
				last = top;
			}
		}
		// Swifter than the old Qi Flight of the high realms (True Immortal Grand Perfection's 0.36 sprinted: 160 blocks a second).
		helper.assertTrue(MeteorFlight.topSpeed(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION) > 22.2 * 0.36 * 20, "Past the flight it replaces");
		ServerPlayer core = cultivator(helper.getLevel(), Realm.CORE_FORMATION, Stage.EARLY);
		try {
			PlayerCultivation c = CultivationManager.get(core);
			helper.assertTrue(MeteorFlight.cruisePerTick(c) >= MeteorFlight.NORMAL_SPRINT && MeteorFlight.cruisePerTick(c) < MeteorFlight.topPerTick(c),
					"Cruising: no slower than Qi Flight's sprint, slower than the top");
		} finally {
			forget(core);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void flyingFastCostsThreeTimesTheQiAndHeavenlyBeingsNeverRunDry(GameTestHelper helper) {
		ServerPlayer core = cultivator(helper.getLevel(), Realm.CORE_FORMATION, Stage.EARLY);
		ServerPlayer being = cultivator(helper.getLevel(), Realm.HEAVENLY_BEING, Stage.EARLY);
		try {
			PlayerCultivation c = flying(core, true);
			double base = QiFlight.tick(core, c);
			helper.assertTrue(Math.abs(base - c.qiFlightCostPerSecond()) < 1e-9, "Hovering in Meteor Flight: Qi Flight's own cost (" + base + ")");
			MeteorFlight.setStreaking(core, true, false);
			double fast = QiFlight.tick(core, c);
			helper.assertTrue(MeteorFlight.isStreaking(core) && Math.abs(fast - base * MeteorFlight.STREAK_COST) < 1e-9,
					"Flying fast: three times it (" + fast + " for " + base + ")");
			// Switched off, flying fast is no more, whatever the client says.
			c.setAbilityEnabled(Ability.METEOR_FLIGHT, false);
			helper.assertTrue(QiFlight.tick(core, c) == base && !MeteorFlight.isStreaking(core), "Switched off: Qi Flight's cost");
			PlayerCultivation b = flying(being, true);
			MeteorFlight.setStreaking(being, true, false);
			double cost = QiFlight.tick(being, b), gather = b.qiGatherPerSecond();
			helper.assertTrue(MeteorFlight.isStreaking(being) && cost > b.qiFlightCostPerSecond() && cost <= Math.max(b.qiFlightCostPerSecond(), gather) + 1e-9,
					"A Heavenly Being flying fast never spends more than it gathers (" + cost + " for " + gather + " gathered)");
		} finally {
			forget(core, being);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBarrierBreaksOnceAndTheHeatBuildsFromNascentSoul(GameTestHelper helper) {
		ServerPlayer core = cultivator(helper.getLevel(), Realm.CORE_FORMATION, Stage.LATE);
		ServerPlayer soul = cultivator(helper.getLevel(), Realm.NASCENT_SOUL, Stage.EARLY);
		try {
			// High in the empty sky, so nothing stands in the way of one ablaze.
			Vec3 sky = new Vec3(helper.absolutePos(BlockPos.ZERO).getX() + 0.5, helper.getLevel().getMaxBuildHeight() - 8, helper.absolutePos(BlockPos.ZERO).getZ() + 0.5);
			for (ServerPlayer p : new ServerPlayer[] {core, soul}) {
				PlayerCultivation c = flying(p, true);
				MeteorFlight.setStreaking(p, true, false);
				double top = MeteorFlight.topPerTick(c);
				Vec3 at = sky;
				for (int tick = 0; tick < MeteorFlight.HEAT_AFTER + 30; tick++) {
					Vec3 next = at.add(top, 0, 0);
					MeteorFlight.observe(p, c, true, at, next);
					at = next;
				}
				helper.assertTrue(MeteorFlight.booms(p) == 1, "Past the sound barrier: one boom, not one a tick (" + MeteorFlight.booms(p) + ")");
				helper.assertTrue(MeteorFlight.speed(p) > top * MeteorFlight.FLAME_SHARE, "Going at top speed (" + MeteorFlight.speed(p) + " of " + top + ")");
			}
			helper.assertTrue(MeteorFlight.heat(core) == 0, "Core Formation never comes ablaze");
			float hot = MeteorFlight.heat(soul);
			helper.assertTrue(hot >= MeteorFlight.ABLAZE, "Nascent Soul ablaze after holding top speed (" + hot + ")");
			// Packets bunched (two moves in one tick, none the next, as a stuttering client sends them): the heat holds.
			PlayerCultivation s = CultivationManager.get(soul);
			double top = MeteorFlight.topPerTick(s);
			Vec3 at = sky;
			for (int tick = 0; tick < 20; tick++) {
				Vec3 next = at.add(tick % 2 == 0 ? top * 2 : 0, 0, 0);
				MeteorFlight.observe(soul, s, true, at, next);
				at = next;
			}
			helper.assertTrue(MeteorFlight.heat(soul) >= hot, "Bunched packets don't cool it (" + MeteorFlight.heat(soul) + ")");
			hot = MeteorFlight.heat(soul);
			// Slowing: it cools, a while yet hot.
			for (int tick = 0; tick < 20; tick++) MeteorFlight.observe(soul, s, true, sky, sky.add(0.2, 0, 0));
			float cooling = MeteorFlight.heat(soul);
			helper.assertTrue(cooling < hot && cooling > 0, "Slowed, it cools (" + hot + " -> " + cooling + ")");
		} finally {
			forget(core, soul);
		}
		helper.succeed();
	}

	/** Its own batch: it builds and breaks blocks, and switches the gamerule. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "meteor_flight_smash")
	public void ablazeTheyTearThroughStoneButNotObsidianNorWhatIsKept(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer soul = cultivator(level, Realm.NASCENT_SOUL, Stage.MID);
		BlockPos wall = helper.absolutePos(new BlockPos(4, 2, 1));
		boolean rule = level.getGameRules().getBoolean(ModGameRules.METEOR_FLIGHT_SMASHES);
		try {
			build(level, wall);
			Vec3 from = Vec3.atBottomCenterOf(wall.offset(-4, 0, 0)).add(0, -0.4, 0), to = from.add(1, 0, 0);
			int cut = MeteorFlight.carve(soul, level, from, to, 6);
			helper.assertTrue(cut > 0 && level.getBlockState(wall).isAir() && level.getBlockState(wall.above()).isAir(), "Stone in its way torn away (" + cut + ")");
			helper.assertTrue(level.getBlockState(wall.offset(0, 0, 1)).is(Blocks.OBSIDIAN), "Obsidian stands");
			helper.assertTrue(level.getBlockState(wall.offset(0, 0, -1)).is(Blocks.CHEST), "A chest stands (what's kept in it is safe)");
			// Flown at, ablaze: the gamerule off, nothing; on, through.
			build(level, wall);
			PlayerCultivation c = flying(soul, true);
			MeteorFlight.setStreaking(soul, true, false);
			level.getGameRules().getRule(ModGameRules.METEOR_FLIGHT_SMASHES).set(false, level.getServer());
			heat(soul, c, from, to);
			helper.assertTrue(MeteorFlight.heat(soul) >= MeteorFlight.ABLAZE && level.getBlockState(wall).is(Blocks.STONE), "The gamerule off: ablaze, but the wall stands");
			level.getGameRules().getRule(ModGameRules.METEOR_FLIGHT_SMASHES).set(true, level.getServer());
			MeteorFlight.observe(soul, c, true, from, to);
			helper.assertTrue(level.getBlockState(wall).isAir(), "On: through it");
		} finally {
			level.getGameRules().getRule(ModGameRules.METEOR_FLIGHT_SMASHES).set(rule, level.getServer());
			for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-1, -1, -2), wall.offset(1, 3, 2))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			forget(soul);
		}
		helper.succeed();
	}

	/**
	 * The bulldozer (the user's ask, 2026-10-23): the body's box swept ahead takes the wall in its way but not the floor under the feet,
	 * nor the wall's sides beyond the body, nor obsidian or a chest; the server takes what the client names only with the gamerule on,
	 * and never what the spells' wounds couldn't. Its own batch: it builds and breaks blocks, and switches the gamerule.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "meteor_flight_smash")
	public void theBulldozerClearsTheWayAheadButNotTheGroundBelow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer core = cultivator(level, Realm.CORE_FORMATION, Stage.EARLY);
		BlockPos wall = helper.absolutePos(new BlockPos(4, 2, 1));
		boolean rule = level.getGameRules().getBoolean(ModGameRules.METEOR_FLIGHT_SMASHES);
		try {
			build(level, wall);
			for (int dx = -4; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) level.setBlock(wall.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 3);
			}
			// Feet on the floor, three blocks short of the wall, flying at it.
			PlayerCultivation c = flying(core, true);
			Vec3 feet = Vec3.atBottomCenterOf(wall.offset(-3, 0, 0));
			core.moveTo(feet.x, feet.y, feet.z);
			List<BlockPos> ahead = Bulldozer.sweep(level, core, core.getBoundingBox(), new Vec3(1, 0, 0), 6, core.getY(), Bulldozer.MOST_A_TICK);
			helper.assertTrue(ahead.contains(wall) && ahead.contains(wall.above()), "The stone in the body's way is swept: " + ahead);
			helper.assertTrue(!ahead.contains(wall.offset(0, 0, 1)) && !ahead.contains(wall.offset(0, 0, -1)), "Not the obsidian, nor the chest");
			helper.assertTrue(!ahead.contains(wall.offset(0, 1, 1)) && !ahead.contains(wall.offset(0, 1, -1)), "Nor the wall beyond the body's width");
			helper.assertTrue(ahead.stream().allMatch(pos -> pos.getY() >= wall.getY()), "Nor the ground under the feet");
			helper.assertTrue(!ahead.contains(wall.above(2)), "Nor above the head");
			// The client's word: the gamerule off, nothing goes; on, the way goes; never what isn't to be taken.
			level.getGameRules().getRule(ModGameRules.METEOR_FLIGHT_SMASHES).set(false, level.getServer());
			helper.assertTrue(MeteorFlight.bulldoze(core, 1, ahead) == 0 && level.getBlockState(wall).is(Blocks.STONE), "The gamerule off: the wall stands");
			level.getGameRules().getRule(ModGameRules.METEOR_FLIGHT_SMASHES).set(true, level.getServer());
			int cut = MeteorFlight.bulldoze(core, 2, ahead);
			helper.assertTrue(cut == ahead.size() && level.getBlockState(wall).isAir() && level.getBlockState(wall.above()).isAir(), "On: through it (" + cut + ")");
			int more = MeteorFlight.bulldoze(core, 3, List.of(wall.offset(0, 0, 1), wall.offset(0, 0, -1), wall.offset(0, 0, -1).above(200)));
			helper.assertTrue(more == 0 && level.getBlockState(wall.offset(0, 0, 1)).is(Blocks.OBSIDIAN) && level.getBlockState(wall.offset(0, 0, -1)).is(Blocks.CHEST),
					"Obsidian, a chest and what's far off are not taken however the client asks");
			// Not flying in Meteor Flight: nothing.
			level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
			c.setAbilityEnabled(Ability.METEOR_FLIGHT, false);
			helper.assertTrue(MeteorFlight.bulldoze(core, 4, List.of(wall)) == 0 && level.getBlockState(wall).is(Blocks.STONE), "Meteor Flight off: the wall stands");
		} finally {
			level.getGameRules().getRule(ModGameRules.METEOR_FLIGHT_SMASHES).set(rule, level.getServer());
			for (BlockPos pos : BlockPos.betweenClosed(wall.offset(-4, -1, -2), wall.offset(1, 3, 2))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			forget(core);
		}
		helper.succeed();
	}

	/** A stone wall three high and three across at {@code wall} (its bottom middle), an obsidian block and a chest in it. */
	private static void build(ServerLevel level, BlockPos wall) {
		for (int dy = 0; dy < 3; dy++) {
			for (int dz = -1; dz <= 1; dz++) level.setBlock(wall.offset(0, dy, dz), Blocks.STONE.defaultBlockState(), 3);
		}
		level.setBlock(wall.offset(0, 0, 1), Blocks.OBSIDIAN.defaultBlockState(), 3);
		level.setBlock(wall.offset(0, 0, -1), Blocks.CHEST.defaultBlockState(), 3);
	}

	/** {@code p} flown at top speed until ablaze, from {@code from} to {@code to} each tick (as at a wall). */
	private static void heat(ServerPlayer p, PlayerCultivation c, Vec3 from, Vec3 to) {
		double top = MeteorFlight.topPerTick(c);
		Vec3 step = to.subtract(from).normalize().scale(top);
		for (int tick = 0; tick < MeteorFlight.HEAT_AFTER + 30; tick++) MeteorFlight.observe(p, c, true, from, from.add(step));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTrueImmortalHeldBackByTheLandStillComesAblaze(GameTestHelper helper) {
		// The user's report (2026-10-23): a True Immortal caught alight and went out for good, held back by land never made before
		// below the 180 blocks a second its own 90% asks. Flat out as fast as the land lets it (the client's word), it burns.
		ServerPlayer immortal = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.EARLY);
		try {
			PlayerCultivation c = flying(immortal, true);
			Vec3 sky = new Vec3(helper.absolutePos(BlockPos.ZERO).getX() + 0.5, helper.getLevel().getMaxBuildHeight() - 8, helper.absolutePos(BlockPos.ZERO).getZ() + 0.5);
			double held = 4.5; // blocks a tick: 90 a second, under half its top
			run(immortal, c, sky, held, MeteorFlight.HEAT_AFTER + 30, false);
			helper.assertTrue(MeteorFlight.heat(immortal) == 0, "Held back and not flat out: no heat (" + MeteorFlight.heat(immortal) + ")");
			helper.assertTrue(MeteorFlight.booms(immortal) == 1, "...but past the sound barrier, which breaks by 60 a second (" + MeteorFlight.booms(immortal) + " booms)");
			run(immortal, c, sky, held, MeteorFlight.HEAT_AFTER + 30, true);
			helper.assertTrue(MeteorFlight.heat(immortal) >= MeteorFlight.ABLAZE, "Flat out as fast as the land lets it: ablaze (" + MeteorFlight.heat(immortal) + ")");
			float hot = MeteorFlight.heat(immortal);
			run(immortal, c, sky, held, 20, true);
			helper.assertTrue(MeteorFlight.heat(immortal) >= hot, "...and it stays alight while it keeps on so");
			MeteorFlight.forget(immortal.getUUID());
			c = flying(immortal, true);
			run(immortal, c, sky, 1.5, MeteorFlight.HEAT_AFTER + 30, true);
			helper.assertTrue(MeteorFlight.heat(immortal) == 0, "The boost held but crawling (30 a second, under the floor of 40): no heat");
		} finally {
			forget(immortal);
		}
		helper.succeed();
	}

	/** {@code p} flown {@code ticks} ticks at {@code perTick} blocks a tick, the client saying it is {@code flatOut}. */
	private static void run(ServerPlayer p, PlayerCultivation c, Vec3 from, double perTick, int ticks, boolean flatOut) {
		MeteorFlight.setStreaking(p, true, flatOut);
		Vec3 at = from;
		for (int tick = 0; tick < ticks; tick++) {
			Vec3 next = at.add(perTick, 0, 0);
			MeteorFlight.observe(p, c, true, at, next);
			at = next;
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theOldSwiftnessStaysWhereItIsntFlown(GameTestHelper helper) {
		// The user's ask (2026-10-23): "make sure you can still reach previous flight speeds in space and the spatial gap". In the gap,
		// Meteor Flight on gives Qi Flight the speed it had there before; off, creative flight's.
		float vanilla = SwordFlight.QI_FLIGHT_SPEED;
		ServerPlayer immortal = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		ServerPlayer shatterer = cultivator(helper.getLevel(), Realm.VOID_SHATTERING, Stage.EARLY);
		ServerPlayer axis = cultivator(helper.getLevel(), Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		try {
			PlayerCultivation i = CultivationManager.get(immortal), v = CultivationManager.get(shatterer), a = CultivationManager.get(axis);
			for (PlayerCultivation c : new PlayerCultivation[] {i, v, a}) c.setAbilityEnabled(Ability.METEOR_FLIGHT, true);
			helper.assertTrue(QiFlight.speedIn(ModDimensions.SPATIAL_GAP, i) == 0.30f + 3 * 0.02f && QiFlight.speedIn(ModDimensions.SPATIAL_GAP, v) == 0.12f,
					"In the gap with it on: True Immortal Grand Perfection's old 0.36, Void Shattering Early's 0.12 ("
							+ QiFlight.speedIn(ModDimensions.SPATIAL_GAP, i) + ", " + QiFlight.speedIn(ModDimensions.SPATIAL_GAP, v) + ")");
			helper.assertTrue(QiFlight.speedIn(ModDimensions.SPATIAL_GAP, a) == vanilla, "Four Axis flew the gap at creative speed before, and does now");
			helper.assertTrue(QiFlight.speedIn(Level.OVERWORLD, i) == vanilla && QiFlight.speedIn(ModDimensions.UPPER_REALM, i) == vanilla,
					"Where Meteor Flight is flown, Qi Flight itself stays at creative speed (the fast flight is Meteor Flight's)");
			i.setAbilityEnabled(Ability.METEOR_FLIGHT, false);
			helper.assertTrue(QiFlight.speedIn(ModDimensions.SPATIAL_GAP, i) == vanilla, "In the gap with it off: creative speed, for fine work");
			// Space keeps its own speeds, as before (Void Shattering 1.2, True Immortal 6.0 at Early), Meteor Flight or not.
			helper.assertTrue(AstralRealm.spaceSpeed(Realm.VOID_SHATTERING, Stage.EARLY) == 1.2f && AstralRealm.spaceSpeed(Realm.TRUE_IMMORTAL, Stage.EARLY) == 6.0f
					&& AstralRealm.spaceSpeed(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION) == 6.0f + 3 * 0.5f, "Space's speeds as they were");
		} finally {
			forget(immortal, shatterer, axis);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void flownWhereItMayAndRoomForItOnTheServer(GameTestHelper helper) {
		helper.assertTrue(MeteorFlight.flownIn(Level.OVERWORLD) && MeteorFlight.flownIn(Level.NETHER) && MeteorFlight.flownIn(ModDimensions.UPPER_REALM),
				"Flown in the worlds with land");
		helper.assertTrue(!MeteorFlight.flownIn(ModDimensions.SPATIAL_GAP) && !MeteorFlight.flownIn(ModDimensions.ASTRAL_VOID) && !MeteorFlight.flownIn(ModDimensions.SOUL_RIVER),
				"Not in the gap, in space or on the Soul River");
		ServerPlayer immortal = cultivator(helper.getLevel(), Realm.TRUE_IMMORTAL, Stage.EARLY);
		try {
			PlayerCultivation c = flying(immortal, false);
			float before = QiFlight.moveAllowanceSq(immortal, 100);
			c.setAbilityEnabled(Ability.METEOR_FLIGHT, true);
			double reach = MeteorFlight.topPerTick(c) * 20;
			helper.assertTrue(MeteorFlight.mayStreak(immortal, c) && QiFlight.moveAllowanceSq(immortal, 100) >= (float) (reach * reach) && before < reach * reach,
					"Room on the server for a second of top speed in a packet, only with it on (" + before + " -> " + QiFlight.moveAllowanceSq(immortal, 100) + ")");
			immortal.setGameMode(GameType.CREATIVE);
			helper.assertTrue(!MeteorFlight.mayStreak(immortal, c), "Not for creative flight, which is vanilla's");
		} finally {
			forget(immortal);
		}
		helper.succeed();
	}
}
