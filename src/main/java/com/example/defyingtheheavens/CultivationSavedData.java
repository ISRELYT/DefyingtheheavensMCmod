package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-level storage keyed by player UUID. Survives death, relogs and restarts without mixins. */
public class CultivationSavedData extends SavedData {
	private static final String FILE_ID = "defying_the_heavens_cultivation";

	private final Map<UUID, PlayerCultivation> players = new HashMap<>();

	public static CultivationSavedData forServer(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(CultivationSavedData::load, CultivationSavedData::new, FILE_ID);
	}

	public PlayerCultivation getOrCreate(UUID id) {
		return players.computeIfAbsent(id, k -> new PlayerCultivation());
	}

	public static CultivationSavedData load(CompoundTag tag) {
		CultivationSavedData data = new CultivationSavedData();
		CompoundTag all = tag.getCompound("Players");
		for (String key : all.getAllKeys()) {
			try {
				data.players.put(UUID.fromString(key), PlayerCultivation.load(all.getCompound(key)));
			} catch (IllegalArgumentException ignored) {
				// skip corrupt entry
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag all = new CompoundTag();
		players.forEach((id, c) -> all.put(id.toString(), c.save()));
		tag.put("Players", all);
		return tag;
	}
}
