package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Netherworld's ghost palaces (GhostPalacePiece) and who sits on each throne: noted by its king the first time it stands at
 * its throne (the palace is built and peopled on worldgen threads, which keep no saved data), emptied when the king is slain,
 * and claimed by a ghost newly risen to Ghost King (GhostCultivatorNpc#rise), which goes to rule the nearest empty one.
 */
public final class GhostPalaces extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_ghost_palaces";
	/** A risen king looks this far (blocks, sideways) for an empty throne. */
	public static final double CLAIM_RANGE = 768;

	/** Each throne (where its king stands), and its king's id, or null while the throne stands empty. */
	private final Map<BlockPos, UUID> thrones = new LinkedHashMap<>();

	static GhostPalaces get(ServerLevel netherworld) {
		return netherworld.getDataStorage().computeIfAbsent(GhostPalaces::load, GhostPalaces::new, NAME);
	}

	/** {@code king} sits on the throne at {@code throne} (noted again harmlessly). */
	static void note(ServerLevel level, BlockPos throne, UUID king) {
		GhostPalaces palaces = get(level);
		if (king.equals(palaces.thrones.get(throne))) return;
		palaces.thrones.put(throne.immutable(), king);
		palaces.setDirty();
	}

	/** The throne at {@code throne} stands empty: its king is slain. */
	static void vacate(ServerLevel level, BlockPos throne) {
		GhostPalaces palaces = get(level);
		if (!palaces.thrones.containsKey(throne)) return;
		palaces.thrones.put(throne.immutable(), null);
		palaces.setDirty();
	}

	/** The nearest empty throne within {@link #CLAIM_RANGE} of {@code from}, now {@code king}'s; null if there is none. */
	static BlockPos claimVacant(ServerLevel level, BlockPos from, UUID king) {
		GhostPalaces palaces = get(level);
		BlockPos best = null;
		for (Map.Entry<BlockPos, UUID> entry : palaces.thrones.entrySet()) {
			if (entry.getValue() != null) continue;
			BlockPos throne = entry.getKey();
			double dx = throne.getX() - from.getX(), dz = throne.getZ() - from.getZ();
			if (dx * dx + dz * dz > CLAIM_RANGE * CLAIM_RANGE) continue;
			if (best == null || throne.distSqr(from) < best.distSqr(from)) best = throne;
		}
		if (best != null) {
			palaces.thrones.put(best, king);
			palaces.setDirty();
		}
		return best;
	}

	/** Every throne known, and its king or null (the game tests). */
	Map<BlockPos, UUID> thrones() {
		return thrones;
	}

	static GhostPalaces load(CompoundTag tag) {
		GhostPalaces palaces = new GhostPalaces();
		for (Tag entry : tag.getList("Thrones", Tag.TAG_COMPOUND)) {
			CompoundTag one = (CompoundTag) entry;
			palaces.thrones.put(NbtUtils.readBlockPos(one.getCompound("At")), one.hasUUID("King") ? one.getUUID("King") : null);
		}
		return palaces;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Map.Entry<BlockPos, UUID> entry : thrones.entrySet()) {
			CompoundTag one = new CompoundTag();
			one.put("At", NbtUtils.writeBlockPos(entry.getKey()));
			if (entry.getValue() != null) one.putUUID("King", entry.getValue());
			list.add(one);
		}
		tag.put("Thrones", list);
		return tag;
	}
}
