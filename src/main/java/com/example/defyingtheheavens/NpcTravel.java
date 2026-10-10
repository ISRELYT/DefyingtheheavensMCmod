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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Long journeys off-screen. Flying a few hundred blocks would leave an NPC frozen at the edge of the loaded world, so once
 * nobody is within {@link #SIGHT} of a travelling NPC (departing its sect, going to ascend, rising out of the Nether or End)
 * it is taken out of the world and its journey finishes on its own: it reappears at its destination, at about its flying
 * speed, as soon as that place is loaded. An ascension out of sight is decided there and then: into the Upper Realm, or lost
 * in the Spatial Gap. Saved with the Overworld, so journeys survive restarts.
 */
public class NpcTravel extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_npc_travel";
	/** Nobody this close: the journey may finish unseen. */
	private static final double SIGHT = 96;
	/** Blocks a tick an unseen traveller covers (about its flying speed). */
	private static final double SPEED = 0.4;

	private record Journey(ResourceKey<Level> dimension, BlockPos target, long readyAt, CompoundTag entity) {}

	private final List<Journey> journeys = new ArrayList<>();

	public static NpcTravel get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(NpcTravel::load, NpcTravel::new, NAME);
	}

	/** Called every tick by a travelling NPC: once it is out of everyone's sight, the rest of its journey happens unseen. */
	public static void maybeDispatch(CultivatorNpc npc, ServerLevel level) {
		if (npc.tickCount % 40 != 0 || npc.isRemoved()) return;
		CultivatorNpc.Lifecycle life = npc.getLifecycle();
		boolean ascending = life == CultivatorNpc.Lifecycle.ASCENDING;
		if (life != CultivatorNpc.Lifecycle.DEPARTING && !ascending) return;
		if (level.getNearestPlayer(npc.getX(), npc.getY(), npc.getZ(), SIGHT, false) != null) return;
		MinecraftServer server = level.getServer();
		if (ascending) {
			ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
			if (upper == null || npc.getRandom().nextFloat() >= CultivatorNpc.ASCENSION_SUCCESS) {
				DefyingTheHeavens.LOGGER.info("A {} perished in the Spatial Gap on the way to ascend", npc.getTitle().getId());
				npc.travelling = true;
				npc.discard();
				return;
			}
			BlockPos rift = new BlockPos(SpatialRiftBlock.RIFT_X, npc.getBlockY(), SpatialRiftBlock.RIFT_Z);
			double distance = Math.sqrt(npc.blockPosition().distSqr(rift)) + (level.dimension() == Level.OVERWORLD ? 0 : 400);
			npc.completeAscension(); // decided: it makes it through
			schedule(server, npc, ModDimensions.UPPER_REALM, scatter(npc), distance);
			return;
		}
		BlockPos target = npc.getJourneyTarget();
		if (target == null) return;
		double distance = Math.sqrt(npc.blockPosition().distSqr(target));
		npc.arrive(level); // it reaches its destination: a rogue now (or straight on to seclusion or ascension)
		schedule(server, npc, level.dimension(), target, distance);
	}

	/** Somewhere near the Upper Realm's side of the rift. */
	private static BlockPos scatter(CultivatorNpc npc) {
		return new BlockPos(SpatialRiftBlock.RIFT_X + npc.getRandom().nextInt(161) - 80, 0, SpatialRiftBlock.RIFT_Z + npc.getRandom().nextInt(161) - 80);
	}

	private static void schedule(MinecraftServer server, CultivatorNpc npc, ResourceKey<Level> dimension, BlockPos target, double distance) {
		CompoundTag tag = new CompoundTag();
		if (!npc.saveAsPassenger(tag)) return;
		NpcTravel data = get(server);
		long readyAt = server.overworld().getGameTime() + (long) (distance / SPEED);
		data.journeys.add(new Journey(dimension, target, readyAt, tag));
		data.setDirty();
		npc.travelling = true;
		npc.discard();
	}

	/** Through the rift right now (an ascension the player is watching): the NPC reappears on the Upper Realm side. */
	public static void sendToUpperRealm(CultivatorNpc npc, ServerLevel from, ServerLevel upper) {
		schedule(from.getServer(), npc, upper.dimension(), scatter(npc), 0);
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

	/** Every few seconds: travellers whose journey is done reappear, if the place they're bound for is loaded. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 50) return;
		NpcTravel data = get(server);
		if (data.journeys.isEmpty()) return;
		long now = server.overworld().getGameTime();
		boolean changed = false;
		for (Iterator<Journey> it = data.journeys.iterator(); it.hasNext(); ) {
			Journey journey = it.next();
			if (journey.readyAt() > now) continue;
			ServerLevel level = server.getLevel(journey.dimension());
			if (level == null) {
				it.remove();
				changed = true;
				continue;
			}
			BlockPos column = journey.target();
			if (!level.isLoaded(column) || !level.isPositionEntityTicking(column)) continue;
			if (arrive(level, journey)) {
				it.remove();
				changed = true;
			}
		}
		if (changed) data.setDirty();
	}

	private static boolean arrive(ServerLevel level, Journey journey) {
		Entity entity = EntityType.loadEntityRecursive(journey.entity(), level, e -> e);
		if (!(entity instanceof CultivatorNpc npc)) return true; // nothing to bring back: drop the journey
		int x = journey.target().getX(), z = journey.target().getZ();
		int y = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (y <= level.getMinBuildHeight() + 1) y = journey.target().getY() > level.getMinBuildHeight() ? journey.target().getY() : level.getSeaLevel();
		npc.moveTo(x + 0.5, y, z + 0.5, npc.getYRot(), 0);
		npc.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		npc.setQiFlying(false);
		npc.travelling = false;
		level.addFreshEntity(npc);
		return true;
	}

	/** How many NPCs are on their way somewhere unseen (for /sect info and tests). */
	public static int pending(MinecraftServer server) {
		return get(server).journeys.size();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Journey journey : journeys) {
			CompoundTag j = new CompoundTag();
			j.putString("Dimension", journey.dimension().location().toString());
			j.putLong("Target", journey.target().asLong());
			j.putLong("ReadyAt", journey.readyAt());
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
			data.journeys.add(new Journey(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(j.getLong("Target")),
					j.getLong("ReadyAt"), j.getCompound("Entity")));
		}
		return data;
	}

	/** For SectionPos-free callers: the chunk a journey is bound for. */
	static long chunkOf(BlockPos pos) {
		return ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
	}
}
