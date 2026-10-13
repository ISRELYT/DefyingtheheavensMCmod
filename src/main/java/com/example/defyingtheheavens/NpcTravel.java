package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Long journeys off-screen, along a real way. Flying a few hundred blocks would leave an NPC frozen at the edge of the
 * loaded world, so once nobody is within {@link #reach} (the simulation distance) of a travelling NPC (departing its sect,
 * going to ascend, coming up into the Upper Realm), or its land has stopped ticking, it is taken out of the world and goes
 * on unseen: in a straight line from where it left to where it's bound, at about its flying speed ({@link #SPEED}), so at
 * any moment it is somewhere on that line ({@link Journey#at}). It isn't lost to the world meanwhile: as soon as a player is
 * within reach of where it has got to, and the land there ticks, it is back in the world right there (as far off as it can
 * be and still move), in the air on its way, to be met,
 * followed, talked to, traded with or fought; out of everyone's sight again, it goes on unseen from there. And it never
 * appears out of thin air where it's bound: someone waiting there sees it fly in from back along its way.
 * <p>
 * What a journey comes to is settled at its end: one leaving its sect becomes a rogue (or goes into seclusion), one coming
 * to join a sect joins (SectManager#admit), and one going to ascend tries the rift at Overworld 0, 0 (from the End it rises
 * out of sight first, and goes on from the matching place in the Overworld): lost in the Spatial Gap, or out on the Upper Realm
 * side, close to the rift. One ascending from the Nether goes to the Nether's own rift instead, up the Soul River: its soul
 * scattered on the way, or out at the Ghost Gate as one of the dead. Unseen, the land at a journey's end is loaded for a moment if nobody is there
 * (NpcLedger#wake). Saved with the Overworld, so journeys survive restarts.
 * <p>
 * Travellers are checked twice a second whether they are ticking or not ({@link #sweep}): one in loaded land that has stopped
 * ticking (its player flew off fast, teleported or left) would otherwise wait there, frozen, until someone came back.
 */
public class NpcTravel extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_npc_travel";
	/**
	 * Beyond {@link #reach} by this much more before a traveller in the world goes on unseen, so one just met (at the edge of
	 * the reach, and higher up than the player) isn't sent off again at once.
	 */
	static final double SLACK = 32;
	/** Blocks a tick an unseen traveller covers (about its flying speed). */
	static final double SPEED = 0.4;
	/** At most this many destinations woken every few seconds for travellers to arrive in. */
	private static final int ARRIVALS_WOKEN = 4;
	/** How far from its destination a traveller looks for dry ground to set down on (outside the Upper Realm). */
	private static final int LANDING_REACH = 64;
	/** How far from its destination in the Upper Realm a traveller looks for an island to set down on. */
	private static final int ISLAND_REACH = 320;
	/** The height cultivators come out at on the Upper Realm side of the rift, over its lowest islands. */
	private static final int UPPER_ENTRY_Y = 160;
	/** How far round the rift's Upper Realm side they come out. */
	private static final int UPPER_SCATTER = 80;
	/** How far out of the End's middle one rising out of the End is taken to be, in the Overworld, on its way to the rift. */
	private static final int END_RISE = 400;

	/**
	 * A traveller on its way unseen: from {@code origin} to {@code target} in {@code dimension}, having left at
	 * {@code departedAt} and getting there at {@code readyAt} (game time); {@code ascent}: going to try the rift.
	 */
	record Journey(ResourceKey<Level> dimension, BlockPos origin, BlockPos target, long departedAt, long readyAt, boolean ascent, CompoundTag entity) {
		/** How far it goes, in blocks. */
		double length() {
			return Math.sqrt(origin.distSqr(target));
		}

		/** Where along its way it is at {@code now}: straight from origin to target, at its pace. */
		Vec3 at(long now) {
			double done = readyAt <= departedAt ? 1 : Mth.clamp((now - departedAt) / (double) (readyAt - departedAt), 0, 1);
			Vec3 from = Vec3.atBottomCenterOf(origin), to = Vec3.atBottomCenterOf(target);
			return from.add(to.subtract(from).scale(done));
		}

		/** The same journey, put back to {@code back} blocks short of its end at {@code now} (all of it, if shorter). */
		Journey shortOfTheEnd(long now, double back) {
			double length = length(), remaining = Math.min(back, length);
			return new Journey(dimension, origin, target, now - (long) ((length - remaining) / SPEED), now + (long) (remaining / SPEED), ascent, entity);
		}
	}

	private final List<Journey> journeys = new ArrayList<>();

	/**
	 * How far round a player (sideways) travellers are in the world: as far as the land runs round them, the simulation
	 * distance (plus a chunk, as it runs to the edge of the chunks it reaches). An unseen traveller comes back as soon as it
	 * is that near and its land runs; so it is back as far off as it can be, moving, and with the usual settings first seen
	 * as a speck in the distance coming nearer, rather than appearing close by. Nearer, it would only be frozen in the air.
	 */
	static double reach(MinecraftServer server) {
		return server.getPlayerList().getSimulationDistance() * 16 + 16;
	}

	public static NpcTravel get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(NpcTravel::load, NpcTravel::new, NAME);
	}

	/** Called every tick by a travelling NPC: once it is out of everyone's sight, the rest of its journey happens unseen. */
	public static void maybeDispatch(CultivatorNpc npc, ServerLevel level) {
		if (npc.tickCount % 40 != 0) return;
		dispatch(npc, level);
	}

	/** Every loaded traveller, ticking or not, that nobody is near goes on its way unseen. */
	private static void sweep() {
		for (CultivatorNpc npc : CultivatorNpc.travellers()) {
			if (npc.level() instanceof ServerLevel level) dispatch(npc, level);
		}
	}

	private static void dispatch(CultivatorNpc npc, ServerLevel level) {
		if (npc.isRemoved()) return;
		CultivatorNpc.Lifecycle life = npc.getLifecycle();
		boolean ascending = life == CultivatorNpc.Lifecycle.ASCENDING;
		if (life != CultivatorNpc.Lifecycle.DEPARTING && !ascending) return;
		MinecraftServer server = level.getServer();
		// Someone within reach and its land ticking: it travels in the world, where it can be met, followed and fought.
		if (level.isPositionEntityTicking(npc.blockPosition())
				&& level.getNearestPlayer(npc.getX(), npc.getY(), npc.getZ(), reach(server) + SLACK, false) != null) return;
		BlockPos from = npc.blockPosition();
		if (ascending) {
			// From the Nether, to its own rift at Nether 0, 0 (the Journey knows its dimension: the Soul River, see tryTheRift).
			if (ascentRift(level.dimension()) == Level.NETHER) {
				schedule(server, npc, Level.NETHER, from, new BlockPos(SpatialRiftBlock.RIFT_X, from.getY(), SpatialRiftBlock.RIFT_Z), true);
				return;
			}
			// To the Overworld's rift; from the End, it rises out of sight and goes on from the matching place in the Overworld.
			if (level.dimension() != Level.OVERWORLD) from = overworldSide(level, from);
			schedule(server, npc, Level.OVERWORLD, from, new BlockPos(SpatialRiftBlock.RIFT_X, from.getY(), SpatialRiftBlock.RIFT_Z), true);
			return;
		}
		BlockPos target = npc.getJourneyTarget();
		if (target == null) {
			npc.arrive(level);
			return;
		}
		schedule(server, npc, level.dimension(), from, target, false);
	}

	/**
	 * Which rift an NPC ascending from {@code from} goes to (the dimension it stands in): the Nether's own, up the Soul River to
	 * the Netherworld, from the Nether; the Overworld's, to the Upper Realm, from anywhere else.
	 */
	public static ResourceKey<Level> ascentRift(ResourceKey<Level> from) {
		return from == Level.NETHER ? Level.NETHER : Level.OVERWORLD;
	}

	/** Where an NPC through {@code rift}'s rift comes out: the Netherworld from the Nether's, the Upper Realm from the Overworld's. */
	public static ResourceKey<Level> ascentDestination(ResourceKey<Level> rift) {
		return rift == Level.NETHER ? ModDimensions.NETHERWORLD : ModDimensions.UPPER_REALM;
	}

	/** The Overworld place matching {@code pos} in the Nether (eight times as far out) or the End (out from the rift). */
	private static BlockPos overworldSide(ServerLevel level, BlockPos pos) {
		int y = Mth.clamp(pos.getY(), 80, 200);
		if (level.dimension() == Level.NETHER) {
			int limit = 29_000_000;
			return new BlockPos(Mth.clamp(pos.getX() * 8, -limit, limit), y, Mth.clamp(pos.getZ() * 8, -limit, limit));
		}
		double angle = pos.getX() == 0 && pos.getZ() == 0 ? 0 : Math.atan2(pos.getZ(), pos.getX());
		return new BlockPos(SpatialRiftBlock.RIFT_X + (int) (Math.cos(angle) * END_RISE), y, SpatialRiftBlock.RIFT_Z + (int) (Math.sin(angle) * END_RISE));
	}

	/** Through the rift right now (an ascension the player is watching): it comes out on the Upper Realm side. */
	public static void sendToUpperRealm(CultivatorNpc npc, ServerLevel from, ServerLevel upper) {
		schedule(from.getServer(), npc, upper.dimension(), upperEntry(), upperLanding(from.getRandom()), false);
	}

	private static BlockPos upperEntry() {
		return new BlockPos(SpatialRiftBlock.RIFT_X, UPPER_ENTRY_Y, SpatialRiftBlock.RIFT_Z);
	}

	/** Somewhere round the Upper Realm's side of the rift. */
	private static BlockPos upperLanding(net.minecraft.util.RandomSource random) {
		return new BlockPos(SpatialRiftBlock.RIFT_X + random.nextInt(2 * UPPER_SCATTER + 1) - UPPER_SCATTER, UPPER_ENTRY_Y,
				SpatialRiftBlock.RIFT_Z + random.nextInt(2 * UPPER_SCATTER + 1) - UPPER_SCATTER);
	}

	/** Out of the world, on its way from {@code origin} to {@code target} in {@code dimension}, unseen. */
	private static void schedule(MinecraftServer server, CultivatorNpc npc, ResourceKey<Level> dimension, BlockPos origin, BlockPos target, boolean ascent) {
		CompoundTag tag = new CompoundTag();
		if (!npc.saveAsPassenger(tag)) return;
		NpcTravel data = get(server);
		data.journeys.add(journey(server, dimension, origin, target, ascent, tag));
		data.setDirty();
		npc.travelling = true;
		npc.discard();
	}

	private static Journey journey(MinecraftServer server, ResourceKey<Level> dimension, BlockPos origin, BlockPos target, boolean ascent, CompoundTag tag) {
		long now = server.overworld().getGameTime();
		return new Journey(dimension, origin, target, now, now + (long) (Math.sqrt(origin.distSqr(target)) / SPEED), ascent, tag);
	}

	/** The rift block in the Overworld's 0, 0 column, or null if that column isn't loaded (or holds none). */
	public static BlockPos riftPosition(ServerLevel overworld) {
		BlockPos column = new BlockPos(SpatialRiftBlock.RIFT_X, 0, SpatialRiftBlock.RIFT_Z);
		if (!overworld.hasChunkAt(column)) return null;
		int top = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, SpatialRiftBlock.RIFT_X, SpatialRiftBlock.RIFT_Z) + 4;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(SpatialRiftBlock.RIFT_X, 0, SpatialRiftBlock.RIFT_Z);
		for (int y = Math.min(top, overworld.getMaxBuildHeight() - 1); y > overworld.getMinBuildHeight(); y--) {
			if (overworld.getBlockState(cursor.setY(y)).is(ModBlocks.SPATIAL_RIFT)) return cursor.immutable();
		}
		return null;
	}

	/**
	 * Twice a second, loaded travellers out of everyone's sight are sent on their way ({@link #sweep}), and unseen ones a
	 * player has come near are back in the world ({@link #meet}); every few seconds, journeys that are done end.
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 == 0) {
			sweep();
			meet(server);
		}
		if (server.getTickCount() % 100 != 50) return;
		NpcTravel data = get(server);
		if (data.journeys.isEmpty()) return;
		long now = server.overworld().getGameTime();
		double reach = reach(server);
		boolean changed = false;
		int woken = 0;
		List<Journey> onward = new ArrayList<>();
		for (ListIterator<Journey> it = data.journeys.listIterator(); it.hasNext(); ) {
			Journey journey = it.next();
			if (journey.readyAt() > now) continue;
			ServerLevel level = server.getLevel(journey.dimension());
			if (level == null) {
				it.remove();
				changed = true;
				continue;
			}
			// Someone waiting where it's bound: it comes in from back along its way, beyond their reach, flying in (meet).
			if (journey.length() > reach + 16 && someoneNear(level, Vec3.atBottomCenterOf(journey.target()), reach + 16)) {
				it.set(journey.shortOfTheEnd(now, reach + SLACK));
				changed = true;
				continue;
			}
			if (journey.ascent()) {
				it.remove();
				changed = true;
				Journey through = tryTheRift(server, level, journey);
				if (through != null) onward.add(through);
				continue;
			}
			BlockPos column = journey.target();
			if (!level.isLoaded(column) || !level.isPositionEntityTicking(column)) {
				// Nobody there: the land is woken for a moment so it can arrive and go on with its life (NpcLedger).
				if (woken++ < ARRIVALS_WOKEN) NpcLedger.wake(level, column);
				continue;
			}
			arrive(level, journey);
			it.remove();
			changed = true;
		}
		data.journeys.addAll(onward);
		if (changed || !onward.isEmpty()) data.setDirty();
	}

	/**
	 * At the rift, unseen: lost in the Spatial Gap ({@link CultivatorNpc#ASCENSION_SUCCESS}), or through, and out on the
	 * Upper Realm side. @return its way on from the rift, or null if it was lost
	 */
	private static Journey tryTheRift(MinecraftServer server, ServerLevel level, Journey journey) {
		if (journey.dimension() == Level.NETHER) return tryTheSoulRiver(server, level, journey);
		ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
		Entity entity = EntityType.loadEntityRecursive(journey.entity(), level, e -> e);
		if (!(entity instanceof CultivatorNpc npc)) return null;
		if (upper == null || level.getRandom().nextFloat() >= CultivatorNpc.ASCENSION_SUCCESS) {
			DefyingTheHeavens.LOGGER.info("A {} perished in the Spatial Gap on the way to ascend", npc.getTitle().getId());
			return null;
		}
		npc.completeAscension();
		CompoundTag tag = new CompoundTag();
		if (!npc.saveAsPassenger(tag)) return null;
		return journey(server, upper.dimension(), upperEntry(), upperLanding(level.getRandom()), false, tag);
	}

	/**
	 * At the Nether's rift, unseen: up the Soul River, its soul scattered on the way ({@link CultivatorNpc#ASCENSION_SUCCESS}), or
	 * come out at the Ghost Gate as one of the dead, its body shed (GhostCultivatorNpc#shedBody). @return its way on to the
	 * Gate's plaza, or null if it was lost
	 */
	private static Journey tryTheSoulRiver(MinecraftServer server, ServerLevel nether, Journey journey) {
		ServerLevel netherworld = server.getLevel(ModDimensions.NETHERWORLD);
		Entity entity = EntityType.loadEntityRecursive(journey.entity(), nether, e -> e);
		if (!(entity instanceof CultivatorNpc npc)) return null;
		if (netherworld == null || nether.getRandom().nextFloat() >= CultivatorNpc.ASCENSION_SUCCESS) {
			DefyingTheHeavens.LOGGER.info("A {}'s soul scattered on the Soul River on the way up to the Netherworld", npc.getTitle().getId());
			return null;
		}
		GhostCultivatorNpc ghost = GhostCultivatorNpc.shedBody(npc, netherworld);
		CompoundTag tag = new CompoundTag();
		if (ghost == null || !ghost.saveAsPassenger(tag)) return null;
		BlockPos arrival = GhostGate.at(netherworld, GhostGate.ARRIVAL);
		return journey(server, netherworld.dimension(), arrival, arrival, false, tag);
	}

	/**
	 * Sends {@code npc} (not yet in any world) to {@code target} in {@code level}, there when that land is next loaded: one just
	 * come up into the Netherworld while nobody is at the Gate.
	 */
	public static void sendTo(CultivatorNpc npc, ServerLevel level, BlockPos target) {
		CompoundTag tag = new CompoundTag();
		if (!npc.saveAsPassenger(tag)) return;
		NpcTravel data = get(level.getServer());
		data.journeys.add(journey(level.getServer(), level.dimension(), target, target, false, tag));
		data.setDirty();
	}

	/** Its journey done, unseen: it sets down where it was bound, and what the journey was for comes about. */
	private static void arrive(ServerLevel level, Journey journey) {
		Entity entity = EntityType.loadEntityRecursive(journey.entity(), level, e -> e);
		if (!(entity instanceof CultivatorNpc npc)) return; // nothing to bring back: drop the journey
		BlockPos at = landing(level, journey.target());
		npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, npc.getYRot(), 0);
		npc.setDeltaMovement(Vec3.ZERO);
		npc.setQiFlying(false);
		npc.travelling = false;
		level.addFreshEntity(npc);
		npc.arrive(level); // a rogue now, into seclusion, or into the sect it came to join
	}

	/**
	 * Unseen travellers a player has come within {@link #reach} of (sideways), where they have got to on their way: back in
	 * the world there, if that land ticks (otherwise not yet: it comes back as the player comes nearer, or goes on unseen).
	 */
	private static void meet(MinecraftServer server) {
		NpcTravel data = get(server);
		if (data.journeys.isEmpty()) return;
		long now = server.overworld().getGameTime();
		double reach = reach(server);
		boolean changed = false;
		for (Iterator<Journey> it = data.journeys.iterator(); it.hasNext(); ) {
			Journey journey = it.next();
			ServerLevel level = server.getLevel(journey.dimension());
			if (level == null || level.players().isEmpty()) continue;
			Vec3 at = journey.at(now);
			if (!level.isPositionEntityTicking(BlockPos.containing(at)) || !someoneNear(level, at, reach)) continue;
			if (materialize(level, journey, at)) {
				it.remove();
				changed = true;
			}
		}
		if (changed) data.setDirty();
	}

	/**
	 * Back in the world at {@code at} on its way: in the air cruising above the land (or over open sky), still bound where it
	 * was. One that can't keep itself up there comes down onto the land below and goes on afoot; over open sky it can't, so
	 * it stays unseen for now. @return whether it is back (or there was nothing to bring back)
	 */
	private static boolean materialize(ServerLevel level, Journey journey, Vec3 at) {
		Entity entity = EntityType.loadEntityRecursive(journey.entity(), level, e -> e);
		if (!(entity instanceof CultivatorNpc npc)) return true;
		Vec3 way = Vec3.atBottomCenterOf(journey.target()).subtract(Vec3.atBottomCenterOf(journey.origin()));
		float yaw = way.horizontalDistanceSqr() < 1e-6 ? npc.getYRot() : (float) (Mth.atan2(way.z, way.x) * Mth.RAD_TO_DEG) - 90.0f;
		npc.moveTo(at.x, at.y, at.z, yaw, 0);
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z));
		boolean land = ground > level.getMinBuildHeight() + 1;
		boolean flies = npc.canFlySustainably();
		double y;
		if (flies) y = land ? Mth.clamp(at.y, ground + 14, ground + 40) : at.y;
		else if (land) y = ground;
		else return false;
		npc.moveTo(at.x, y, at.z, yaw, 0);
		npc.setDeltaMovement(Vec3.ZERO);
		npc.travelling = false;
		level.addFreshEntity(npc);
		if (flies) npc.setQiFlying(true);
		return true;
	}

	/** Whether a player (not a spectator) is within {@code reach} of {@code at}, sideways. */
	private static boolean someoneNear(ServerLevel level, Vec3 at, double reach) {
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - at.x, dz = player.getZ() - at.z;
			if (dx * dx + dz * dz <= reach * reach) return true;
		}
		return false;
	}

	/**
	 * Where a traveller sets down at {@code target}: in the Upper Realm, on the nearest island within {@link #ISLAND_REACH}
	 * (failing that, a small platform as for a player arriving: SpatialTrialHandler#findArrival), never over the void;
	 * elsewhere, on dry ground near the spot if there is some within {@link #LANDING_REACH} (not out at sea), else on the
	 * surface there.
	 */
	static BlockPos landing(ServerLevel level, BlockPos target) {
		int x = target.getX(), z = target.getZ();
		if (ModDimensions.isUpperRealm(level.dimension())) {
			BlockPos island = nearestIsland(level, x, z);
			return island != null ? island : SpatialTrialHandler.findArrival(level, x, z);
		}
		for (int radius = 0; radius <= LANDING_REACH; radius += 16) {
			int points = radius == 0 ? 1 : 8;
			for (int i = 0; i < points; i++) {
				double angle = Math.PI * 2 * i / points;
				int cx = x + (int) Math.round(Math.cos(angle) * radius), cz = z + (int) Math.round(Math.sin(angle) * radius);
				level.getChunk(cx >> 4, cz >> 4);
				int top = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
				if (top <= level.getMinBuildHeight() + 1) continue;
				BlockPos ground = new BlockPos(cx, top - 1, cz);
				if (standsOn(level, ground)) {
					return ground.above();
				}
			}
		}
		int y = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (y <= level.getMinBuildHeight() + 1) y = target.getY() > level.getMinBuildHeight() ? target.getY() : level.getSeaLevel();
		return new BlockPos(x, y, z);
	}

	/**
	 * Solid, dry island ground nearest (x, z), out to {@link #ISLAND_REACH}, or null. The world generator's own heights are
	 * asked first (no land is generated for that), and only a column with an island under it is loaded and looked at: a
	 * cultivator set down on a lone platform in the sky would sooner or later stroll off it into the void.
	 */
	private static BlockPos nearestIsland(ServerLevel level, int x, int z) {
		net.minecraft.world.level.chunk.ChunkGenerator generator = level.getChunkSource().getGenerator();
		net.minecraft.world.level.levelgen.RandomState noise = level.getChunkSource().randomState();
		int floor = level.getMinBuildHeight() + 2;
		for (int radius = 0; radius <= ISLAND_REACH; radius += 16) {
			int points = radius == 0 ? 1 : Math.max(8, radius / 8);
			for (int i = 0; i < points; i++) {
				double angle = Math.PI * 2 * i / points;
				int cx = x + (int) Math.round(Math.cos(angle) * radius), cz = z + (int) Math.round(Math.sin(angle) * radius);
				if (generator.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, level, noise) <= floor) continue;
				level.getChunk(cx >> 4, cz >> 4);
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
				if (top <= floor || top >= level.getMaxBuildHeight() - 2) continue;
				BlockPos ground = new BlockPos(cx, top - 1, cz);
				if (standsOn(level, ground)) {
					return ground.above();
				}
			}
		}
		return null;
	}

	/** Dry, solid ground to stand on, and not the top of a tree trunk (it would stand wedged in the leaves). */
	private static boolean standsOn(ServerLevel level, BlockPos ground) {
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(ground);
		return level.getFluidState(ground).isEmpty() && !state.is(net.minecraft.tags.BlockTags.LOGS)
				&& state.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP);
	}

	/** How many NPCs are on their way somewhere unseen (for /sect info and tests). */
	public static int pending(MinecraftServer server) {
		return get(server).journeys.size();
	}

	/** Unseen journeys under way (for tests). */
	static List<Journey> journeys(MinecraftServer server) {
		return get(server).journeys;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Journey journey : journeys) {
			CompoundTag j = new CompoundTag();
			j.putString("Dimension", journey.dimension().location().toString());
			j.putLong("Origin", journey.origin().asLong());
			j.putLong("Target", journey.target().asLong());
			j.putLong("DepartedAt", journey.departedAt());
			j.putLong("ReadyAt", journey.readyAt());
			j.putBoolean("Ascent", journey.ascent());
			j.put("Entity", journey.entity());
			list.add(j);
		}
		tag.put("Journeys", list);
		return tag;
	}

	public static NpcTravel load(CompoundTag tag) {
		NpcTravel data = new NpcTravel();
		ListTag list = tag.getList("Journeys", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag j = list.getCompound(i);
			ResourceLocation dimension = ResourceLocation.tryParse(j.getString("Dimension"));
			if (dimension == null) continue;
			BlockPos target = BlockPos.of(j.getLong("Target"));
			long readyAt = j.getLong("ReadyAt");
			// Journeys saved before they had a way (their end already settled when they set off) are simply at their end.
			BlockPos origin = j.contains("Origin") ? BlockPos.of(j.getLong("Origin")) : target;
			long departedAt = j.contains("DepartedAt") ? j.getLong("DepartedAt") : readyAt;
			data.journeys.add(new Journey(ResourceKey.create(Registries.DIMENSION, dimension), origin, target, departedAt, readyAt,
					j.getBoolean("Ascent"), j.getCompound("Entity")));
		}
		return data;
	}

	/** For SectionPos-free callers: the chunk a journey is bound for. */
	static long chunkOf(BlockPos pos) {
		return ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
	}
}
