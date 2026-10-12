package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Where every wandering rogue ({@link CultivatorNpc#isWanderingRogue}) and mortal wanderer is, loaded or not (the two kept
 * apart: each is spaced only from its own kind). Cultivators never despawn, so
 * NpcSpawner keeps rogues rare by where it puts them, and needs to know about the ones in land nobody has loaded: without
 * this, a route flown twice would gather a new rogue each time just short of an old one that wasn't loaded yet.
 * <p>
 * A rogue's place is refreshed every few seconds while it is loaded and kept when its land unloads. One that dies, stops
 * wandering (seeks seclusion, sets off on a journey) or leaves the dimension is dropped. So is an entry whose
 * land is loaded and ticking while its rogue is nowhere to be found for {@link #LOST_AFTER} checks in a row (lost with a
 * deleted region, say). Saved with the Overworld.
 */
public class RogueIndex extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_rogues";
	/** Checks (5 seconds apart) a rogue may be missing from its loaded, ticking land before its entry is given up. */
	private static final int LOST_AFTER = 3;

	private record Entry(ResourceKey<Level> dimension, int x, int z, boolean mortal) {}

	private final Map<UUID, Entry> entries = new HashMap<>();
	/** Entries found missing at the last checks, and how many times in a row (not saved). */
	private final Map<UUID, Integer> missing = new HashMap<>();

	public static RogueIndex get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(RogueIndex::load, RogueIndex::new, NAME);
	}

	/** A rogue's place, kept up to date; or its entry dropped, if it no longer wanders. */
	public static void note(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		RogueIndex index = get(level.getServer());
		index.missing.remove(npc.getUUID());
		boolean mortal = npc.isWanderingMortal();
		if (!npc.isWanderingRogue() && !mortal) {
			if (index.entries.remove(npc.getUUID()) != null) index.setDirty();
			return;
		}
		Entry entry = new Entry(level.dimension(), npc.getBlockX(), npc.getBlockZ(), mortal);
		if (!entry.equals(index.entries.put(npc.getUUID(), entry))) index.setDirty();
	}

	/**
	 * A rogue leaving the loaded world: its land unloading keeps its entry (where it now stands); anything else (death, being
	 * discarded or carried off on a journey, another dimension) drops it. Land unloading shows no reason yet: the entity
	 * stops being tracked when its chunk is hidden, before it is marked removed.
	 */
	public static void onUnload(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		Entity.RemovalReason reason = npc.getRemovalReason();
		if (reason == null || reason.shouldSave()) {
			note(npc);
		} else {
			RogueIndex index = get(level.getServer());
			index.missing.remove(npc.getUUID());
			if (index.entries.remove(npc.getUUID()) != null) index.setDirty();
		}
	}

	/** Whether any indexed rogue ({@code mortal}: mortal wanderer) in {@code level} stands within {@code radius} of {@code x, z}, sideways. */
	public static boolean anyWithin(ServerLevel level, double x, double z, double radius, boolean mortal) {
		for (Entry entry : get(level.getServer()).entries.values()) {
			if (entry.dimension() != level.dimension() || entry.mortal() != mortal) continue;
			double dx = entry.x() + 0.5 - x, dz = entry.z() + 0.5 - z;
			if (dx * dx + dz * dz <= radius * radius) return true;
		}
		return false;
	}

	/** How many rogues the index holds (for tests). */
	public static int size(MinecraftServer server) {
		return get(server).entries.size();
	}

	/** Every 5 seconds: loaded rogues' places refreshed, and entries whose rogue is lost dropped. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 25) return;
		for (ServerLevel level : server.getAllLevels()) {
			for (CultivatorNpc npc : CultivatorNpc.loaded(level)) note(npc);
		}
		RogueIndex index = get(server);
		for (Iterator<Map.Entry<UUID, Entry>> it = index.entries.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Entry> e = it.next();
			ServerLevel level = server.getLevel(e.getValue().dimension());
			boolean lost;
			if (level == null) {
				lost = true;
			} else if (!level.isPositionEntityTicking(new BlockPos(e.getValue().x(), 0, e.getValue().z())) || level.getEntity(e.getKey()) != null) {
				index.missing.remove(e.getKey());
				continue;
			} else {
				lost = index.missing.merge(e.getKey(), 1, Integer::sum) >= LOST_AFTER;
			}
			if (lost) {
				it.remove();
				index.missing.remove(e.getKey());
				index.setDirty();
			}
		}
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
			CompoundTag r = new CompoundTag();
			r.putUUID("Id", e.getKey());
			r.putString("Dimension", e.getValue().dimension().location().toString());
			r.putInt("X", e.getValue().x());
			r.putInt("Z", e.getValue().z());
			if (e.getValue().mortal()) r.putBoolean("Mortal", true);
			list.add(r);
		}
		tag.put("Rogues", list);
		return tag;
	}

	public static RogueIndex load(CompoundTag tag) {
		RogueIndex index = new RogueIndex();
		ListTag list = tag.getList("Rogues", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag r = list.getCompound(i);
			ResourceLocation dimension = ResourceLocation.tryParse(r.getString("Dimension"));
			if (dimension == null || !r.hasUUID("Id")) continue;
			index.entries.put(r.getUUID("Id"), new Entry(ResourceKey.create(Registries.DIMENSION, dimension), r.getInt("X"), r.getInt("Z"),
					r.getBoolean("Mortal")));
		}
		return index;
	}
}
