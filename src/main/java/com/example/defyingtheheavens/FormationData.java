package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One level's formations (see {@link Formation}), saved with the level, and the chunks that may still hold barrier blocks of
 * a formation that has since been lowered or destroyed while they were unloaded ({@link #pendingClear}): those are swept as
 * soon as they load (see {@link Formations}).
 */
public class FormationData extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_formations";

	private final Map<UUID, Formation> formations = new LinkedHashMap<>();
	final LongSet pendingClear = new LongOpenHashSet();

	public static FormationData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FormationData::load, FormationData::new, NAME);
	}

	public Formation get(UUID id) { return id == null ? null : formations.get(id); }

	public Collection<Formation> all() { return formations.values(); }

	public void put(Formation formation) {
		formations.put(formation.id, formation);
		setDirty();
	}

	public Formation remove(UUID id) {
		Formation removed = formations.remove(id);
		if (removed != null) setDirty();
		return removed;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Formation formation : formations.values()) list.add(formation.save());
		tag.put("Formations", list);
		tag.put("PendingClear", new LongArrayTag(pendingClear.toLongArray()));
		return tag;
	}

	public static FormationData load(CompoundTag tag) {
		FormationData data = new FormationData();
		ListTag list = tag.getList("Formations", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			Formation formation = Formation.load(list.getCompound(i));
			data.formations.put(formation.id, formation);
		}
		for (long chunk : tag.getLongArray("PendingClear")) data.pendingClear.add(chunk);
		return data;
	}
}
