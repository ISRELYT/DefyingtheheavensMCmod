package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Every sect in the world (all dimensions), saved with the Overworld. */
public class SectData extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_sects";

	private final Map<UUID, Sect> sects = new LinkedHashMap<>();

	public static SectData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(SectData::load, SectData::new, NAME);
	}

	public Sect get(UUID id) { return id == null ? null : sects.get(id); }

	public Collection<Sect> all() { return sects.values(); }

	public void put(Sect sect) {
		sects.put(sect.id, sect);
		setDirty();
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Sect sect : sects.values()) list.add(sect.save());
		tag.put("Sects", list);
		return tag;
	}

	public static SectData load(CompoundTag tag) {
		SectData data = new SectData();
		ListTag list = tag.getList("Sects", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			Sect sect = Sect.load(list.getCompound(i));
			data.sects.put(sect.id, sect);
		}
		return data;
	}
}
