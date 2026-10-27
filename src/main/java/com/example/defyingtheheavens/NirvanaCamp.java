package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The camp on the Shore of Nirvana (the user's picks, 2026-10-25): some twenty of Entering Nirvana come to the Great Void's edge to
 * wait for the Immortal Gate over the void sea, camped some 40 blocks short of the shore's coast north of 0, 0 (ModNoiseSettings: the
 * shore's land, its sea beyond). Built in code the first time the server starts with the Great Void (as the Ghost Gate is), and kept
 * peopled: the first time its land runs, all {@link #CAMPERS} at once; after that, while there are fewer, one more comes in from the
 * south every {@link #ARRIVES_EVERY}. They keep to the camp (CultivatorNpc#settledAt, within {@link #ROAM}: CultivatorGoals.WanderGoal),
 * meditate and trade as any rogue, never set off anywhere, and start no fights (CultivatorGoals.HostilityGoal), but for the
 * short-tempered of the devilish path, who go for a player of the other path come within {@link #TEMPER_REACH} of them: rarely hostile.
 * One in {@link #DEMON_IN} is of a demon race, and on the camp's ground nobody goes for one of the demon races at all ({@link #shelters}:
 * the user's rule). The Great Void's census holds them to its room for Entering Nirvana (HighRealms: 24, a few of them its rogues').
 */
public final class NirvanaCamp {
	/** The camp's middle (its y the floor it was built at). */
	public static final int X = 0, Z = -150;
	/** How many wait here. */
	public static final int CAMPERS = 20;
	/**
	 * Once peopled, a newcomer arrives this often (ticks: a quarter of an in-game day) while there is room: those the Immortal Gate takes
	 * are made up over the following days (ImmortalGate, the user's picks).
	 */
	static final long ARRIVES_EVERY = 6000;
	/** A camper strays this far from the middle before it heads back. */
	public static final int ROAM = 22;
	/** A short-tempered (devilish) camper goes for an enemy of its path this close to it. */
	public static final double TEMPER_REACH = 6;
	/** Its land must have run this long (ticks) before newcomers are sent for: its people load with it, a little after. */
	private static final int SETTLE_TICKS = 200;
	/** The camp's ground, levelled; its paved middle. */
	private static final int RADIUS = 18, PAVED = 11;
	private static final int FILL_DEPTH = 14, CLEAR_HEIGHT = 12;
	/** The floor if the land can't be read (it always can: the shore is sure land, ModNoiseSettings#SHORE_INNER). */
	private static final int FALLBACK_Y = 214;

	/** How long the camp's land has run (ticks; not saved). */
	private static int tickingFor;

	/** Builds the camp if it isn't yet (called when the server starts). */
	public static void ensure(MinecraftServer server) {
		if (server == null) return;
		ServerLevel level = server.getLevel(ModDimensions.GREAT_VOID);
		if (level == null) return;
		try {
			Data data = Data.get(level);
			if (data.built) return;
			int y = findFloor(level);
			long start = System.currentTimeMillis();
			build(level, y);
			data.built = true;
			data.y = y;
			data.setDirty();
			DefyingTheHeavens.LOGGER.info("Shore of Nirvana camp built at {}, {}, {} in {} ms", X, y, Z, System.currentTimeMillis() - start);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Could not build the camp on the Shore of Nirvana", e);
		}
	}

	/** Its middle, where it stands (its floor's level once built). */
	public static BlockPos middle(ServerLevel greatVoid) {
		Data data = Data.get(greatVoid);
		return new BlockPos(X, data.built ? data.y : FALLBACK_Y, Z);
	}

	/** Whether {@code npc} waits in the camp: a cultivator of the Great Void settled at its middle. */
	public static boolean isCamper(CultivatorNpc npc) {
		BlockPos home = npc.settledAt();
		return home != null && home.getX() == X && home.getZ() == Z && ModDimensions.isGreatVoid(npc.level().dimension());
	}

	/**
	 * Whether camper {@code npc} goes for {@code target} (an enemy of its path, HostilityGoal asks after that): of the devilish path, at a
	 * player come this close. Never at another who waits there, nor at a cultivator passing by (tour of 2026-10-25: the devilish went
	 * for the righteous beside them, and the camp fought itself).
	 */
	public static boolean loses(CultivatorNpc npc, net.minecraft.world.entity.LivingEntity target) {
		return target instanceof net.minecraft.world.entity.player.Player && npc.getFaction() == Alignment.Faction.DEVILISH
				&& npc.distanceToSqr(target) <= TEMPER_REACH * TEMPER_REACH && !shelters(target);
	}

	/** One waiting here in this many is of a demon race (as the upper realms' rogues are, of any: DenStructure#upperRace). */
	static final int DEMON_IN = 5;
	/** The camp's ground for {@link #shelters}: its own, and a little round it. */
	private static final int SANCTUARY = ROAM + 16;

	/**
	 * Whether {@code target} is a demon (of a demon race: DemonPath#isDemon for a player, CultivatorNpc#isDemon) on the camp's ground,
	 * where nobody goes for it unprovoked (the user's rule, 2026-10-25: "demon race entering nirvana cultivators shouldn't get attacked
	 * at the gate"; CultivatorGoals.HostilityGoal asks). Struck first, one still strikes back (RetaliateGoal).
	 */
	public static boolean shelters(net.minecraft.world.entity.LivingEntity target) {
		if (!ModDimensions.isGreatVoid(target.level().dimension())) return false;
		boolean demon = target instanceof CultivatorNpc npc ? npc.isDemon()
				: target instanceof net.minecraft.server.level.ServerPlayer player && DemonPath.isDemon(CultivationManager.get(player));
		if (!demon || !(target.level() instanceof ServerLevel level)) return false;
		BlockPos middle = middle(level);
		double dx = target.getX() - middle.getX(), dz = target.getZ() - middle.getZ();
		return dx * dx + dz * dz <= SANCTUARY * SANCTUARY;
	}

	/**
	 * Strike one waiting here and the camp turns on you (the user, 2026-10-27: "if you attack one of the EN campers they all should
	 * retaliate"): every camper about the camp takes the striker for its foe, as it would had it been struck itself (RetaliateGoal), and goes
	 * for it now if it fights no one else. Not those flying for the Gate (it lets none turn them aside: ImmortalGate#isRushing), nor at one
	 * of the camp (a quarrel of the two's), nor at one in creative or watching.
	 */
	public static void rally(ServerLevel level, CultivatorNpc struck, net.minecraft.world.entity.LivingEntity attacker) {
		if (attacker == struck || attacker instanceof CultivatorNpc npc && isCamper(npc)) return;
		if (attacker instanceof net.minecraft.world.entity.player.Player player && (player.isCreative() || player.isSpectator())) return;
		for (CultivatorNpc camper : campers(level, middle(level))) {
			if (camper == struck || ImmortalGate.isRushing(camper) || camper.isAlly(attacker)) continue;
			camper.setLastHurtByMob(attacker);
			if (camper.getTarget() == null) camper.setTarget(attacker);
		}
	}

	/** Every two seconds: the camp peopled the first time its land runs, then a newcomer now and then while there is room. */
	public static void keep(MinecraftServer server) {
		if (server.getTickCount() % 40 != 13) return;
		ServerLevel level = server.getLevel(ModDimensions.GREAT_VOID);
		if (level == null) return;
		Data data = Data.get(level);
		if (!data.built) return;
		BlockPos middle = new BlockPos(X, data.y, Z);
		if (!level.isPositionEntityTicking(middle)) {
			tickingFor = 0;
			return;
		}
		tickingFor += 40;
		long now = level.getGameTime();
		RandomSource random = level.getRandom();
		if (!data.peopled) {
			for (int i = 0; i < CAMPERS; i++) arrive(level, data.y, random, false);
			data.peopled = true;
			data.lastArrival = now;
			data.setDirty();
			return;
		}
		if (tickingFor < SETTLE_TICKS || now - data.lastArrival < ARRIVES_EVERY || campers(level, middle).size() >= CAMPERS) return;
		arrive(level, data.y, random, true);
		data.lastArrival = now;
		data.setDirty();
	}

	/** The campers about {@code middle} now (loaded). */
	static List<CultivatorNpc> campers(ServerLevel level, BlockPos middle) {
		return level.getEntitiesOfClass(CultivatorNpc.class, new AABB(middle).inflate(ROAM + 32, 64, ROAM + 32), npc -> npc.isAlive() && isCamper(npc));
	}

	/**
	 * One of Entering Nirvana (any stage; the census may hold it to Sacred Vessel's peak) of any path, settled at the camp: on its
	 * ground anywhere, or ({@code newcomer}) walking in at its south edge.
	 */
	private static CultivatorNpc arrive(ServerLevel level, int y, RandomSource random, boolean newcomer) {
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return null;
		double x = X + 0.5, z = Z + 0.5;
		for (int tries = 0; tries < 8; tries++) {
			double angle = newcomer ? Math.PI * (0.8 + random.nextDouble() * 0.4) : random.nextDouble() * Math.PI * 2;
			double out = newcomer ? RADIUS - 2 : 3 + random.nextDouble() * (PAVED - 3);
			// Angles from north (-z), clockwise: the south edge is half a turn round. Not on a campfire, a post or a mat's edge.
			x = X + 0.5 + Math.sin(angle) * out;
			z = Z + 0.5 - Math.cos(angle) * out;
			BlockPos feet = BlockPos.containing(x, y, z);
			if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) break;
		}
		npc.moveTo(x, y, z, random.nextFloat() * 360, 0);
		npc.becomeRogue(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.byIndex(random.nextInt(Stage.values().length))),
				Alignment.randomRogue(random), random);
		if (random.nextInt(DEMON_IN) == 0) npc.setRace(DenStructure.upperRace(random), random.nextInt(3)); // the demon races wait here too
		npc.setSettledAt(new BlockPos(X, y, Z));
		level.addFreshEntity(npc);
		if (newcomer) DefyingTheHeavens.LOGGER.info("One of Entering Nirvana comes to wait on the Shore of Nirvana");
		return npc;
	}

	private static int findFloor(ServerLevel level) {
		List<Integer> heights = new ArrayList<>();
		for (int dx = -8; dx <= 8; dx += 8) {
			for (int dz = -8; dz <= 8; dz += 8) {
				int x = X + dx, z = Z + dz;
				level.getChunk(x >> 4, z >> 4);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				if (y > level.getMinBuildHeight() + 1) heights.add(y);
			}
		}
		if (heights.isEmpty()) return FALLBACK_Y;
		Collections.sort(heights);
		return Mth.clamp(heights.get(heights.size() / 2), level.getMinBuildHeight() + 30, level.getMaxBuildHeight() - 40);
	}

	// --- Building ---

	private static final BlockState FILL = Blocks.CALCITE.defaultBlockState();
	private static final BlockState PAVING = Blocks.SMOOTH_QUARTZ.defaultBlockState();
	private static final BlockState RING = Blocks.POLISHED_DIORITE.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState POST = Blocks.SPRUCE_FENCE.defaultBlockState();
	private static final BlockState CANOPY = Blocks.WHITE_WOOL.defaultBlockState();

	/**
	 * The camp at floor {@code y}: its ground levelled (filled up under it, cleared over it), paved in white with a ring of diorite;
	 * a raised dais at its north edge facing the sea, where one may sit and watch for the Gate; five awnings round it, their bedrolls
	 * under them; four campfires; meditation mats in two arcs facing north; and lanterns on posts round its edge.
	 */
	private static void build(ServerLevel level, int y) {
		for (int cx = (X - RADIUS - 2) >> 4; cx <= (X + RADIUS + 2) >> 4; cx++) {
			for (int cz = (Z - RADIUS - 2) >> 4; cz <= (Z + RADIUS + 2) >> 4; cz++) level.getChunk(cx, cz);
		}
		// The ground.
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > RADIUS + 0.5) continue;
				for (int yy = y - FILL_DEPTH; yy < y - 1; yy++) {
					BlockPos at = new BlockPos(X + dx, yy, Z + dz);
					if (level.getBlockState(at).canBeReplaced()) level.setBlock(at, FILL, Block.UPDATE_CLIENTS);
				}
				put(level, dx, y - 1, dz, d <= PAVED - 1 ? PAVING : d <= PAVED ? RING : FILL);
				for (int yy = y; yy <= y + CLEAR_HEIGHT; yy++) put(level, dx, yy, dz, AIR);
			}
		}
		// The dais at the north edge: quartz bricks a step up, stairs up to it from the camp, a lantern at either end.
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -15; dz <= -12; dz++) put(level, dx, y, dz, Blocks.QUARTZ_BRICKS.defaultBlockState());
			put(level, dx, y, -11, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
		}
		for (int dx : new int[] {-4, 4}) lanternPost(level, dx, y + 1, -15);
		for (int dx = -3; dx <= 3; dx += 2) put(level, dx, y + 1, -13, Blocks.WHITE_CARPET.defaultBlockState());
		// Five awnings round the camp (none in the way north): posts, a white canopy, a bedroll under it.
		for (int k = 1; k <= 5; k++) {
			double angle = Math.toRadians(60 * k);
			int ax = (int) Math.round(Math.sin(angle) * 14), az = (int) Math.round(-Math.cos(angle) * 14);
			for (int px : new int[] {-1, 1}) {
				for (int pz : new int[] {-1, 1}) {
					put(level, ax + px, y, az + pz, POST);
					put(level, ax + px, y + 1, az + pz, POST);
				}
			}
			for (int cx = -1; cx <= 1; cx++) {
				for (int cz = -1; cz <= 1; cz++) put(level, ax + cx, y + 2, az + cz, CANOPY);
			}
			put(level, ax, y + 3, az, Blocks.WHITE_CARPET.defaultBlockState());
			put(level, ax, y, az, Blocks.LIGHT_GRAY_CARPET.defaultBlockState());
		}
		// Four campfires.
		for (int k = 0; k < 4; k++) {
			double angle = Math.toRadians(45 + 90 * k);
			put(level, (int) Math.round(Math.sin(angle) * 7), y, (int) Math.round(-Math.cos(angle) * 7),
					Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		}
		// Meditation mats in two arcs, facing the sea.
		for (int i = -2; i <= 2; i++) {
			put(level, i * 3, y, -6, Blocks.RED_CARPET.defaultBlockState());
			if (i != 0) put(level, i * 3 + (i > 0 ? -1 : 1), y, -3, Blocks.RED_CARPET.defaultBlockState());
		}
		// Lanterns round the edge.
		for (int k = 0; k < 8; k++) {
			double angle = Math.toRadians(22.5 + 45 * k);
			lanternPost(level, (int) Math.round(Math.sin(angle) * (RADIUS - 1)), y, (int) Math.round(-Math.cos(angle) * (RADIUS - 1)));
		}
	}

	/** A post two high at {@code (dx, y, dz)} with a lantern on it. */
	private static void lanternPost(ServerLevel level, int dx, int y, int dz) {
		put(level, dx, y, dz, POST);
		put(level, dx, y + 1, dz, POST);
		put(level, dx, y + 2, dz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
	}

	private static void put(ServerLevel level, int dx, int y, int dz, BlockState state) {
		level.setBlock(new BlockPos(X + dx, y, Z + dz), state, Block.UPDATE_CLIENTS);
	}

	/** Server stopped (not saved): the camp's land counted afresh. */
	public static void clear() {
		tickingFor = 0;
	}

	/** That the camp is built, at which floor, whether it has been peopled, and when its last newcomer came; kept with the Great Void's data. */
	public static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_nirvana_camp";
		boolean built, peopled;
		int y = FALLBACK_Y;
		long lastArrival;

		static Data get(ServerLevel greatVoid) {
			return greatVoid.getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			data.built = tag.getBoolean("Built");
			data.peopled = tag.getBoolean("Peopled");
			data.y = tag.contains("FloorY") ? tag.getInt("FloorY") : FALLBACK_Y;
			data.lastArrival = tag.getLong("LastArrival");
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			tag.putBoolean("Built", built);
			tag.putBoolean("Peopled", peopled);
			tag.putInt("FloorY", y);
			tag.putLong("LastArrival", lastArrival);
			return tag;
		}
	}

	private NirvanaCamp() {}
}
