package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One protective barrier: a hollow sphere of {@link SectBarrierBlock}s, one block thick, around {@code center}. Either a
 * Formation Core's ({@link Kind#CORE}, owned by the player who set it or by a sect) or the small Concealment Barrier a
 * secluded cultivator raises around themselves ({@link Kind#CONCEALMENT}, owned by that NPC).
 * <p>
 * The record (kept in {@link FormationData}, so it survives restarts and holds for unloaded chunks) is the only truth about
 * which barrier blocks belong where: a barrier block not on the shell of a raised formation is an orphan and removes itself
 * ({@link Formations}).
 */
public final class Formation {
	public enum Kind { CORE, CONCEALMENT }

	public final UUID id;
	public final Kind kind;
	public final BlockPos center;
	private int radius;
	private boolean raised;
	/**
	 * The cultivation rank (see {@link PlayerCultivation#rank}) that can force its way through: the owner's, or the Sect
	 * Master's. -1: a mortal's formation, which anyone can break.
	 */
	private int rank;
	/** The player (core) or NPC (concealment) it answers to; null for a sect's. */
	private UUID owner;
	/** The sect whose members walk through it; null if none. */
	private UUID sect;
	/** Concealment: the last game time its NPC was seen alive nearby (see Formations#tick). */
	long heartbeat;

	// Runtime only: how far the raising (or the next repair pass) has got through the shell.
	int cursor;
	boolean complete;

	public Formation(UUID id, Kind kind, BlockPos center, int radius) {
		this.id = id;
		this.kind = kind;
		this.center = center.immutable();
		this.radius = radius;
	}

	public int getRadius() { return radius; }
	public boolean isRaised() { return raised; }
	public int getRank() { return rank; }
	public UUID getOwner() { return owner; }
	public UUID getSect() { return sect; }

	void setRadius(int radius) {
		if (this.radius == radius) return;
		this.radius = radius;
		cursor = 0;
		complete = false;
	}

	void setRaised(boolean raised) {
		this.raised = raised;
		cursor = 0;
		complete = false;
	}

	void setRank(int rank) { this.rank = rank; }
	void setOwner(UUID owner) { this.owner = owner; }
	void setSect(UUID sect) { this.sect = sect; }

	/** Whether {@code pos} lies on this formation's shell (whether or not a block stands there now). */
	public boolean isOnShell(BlockPos pos) {
		return isShell(pos.getX() - center.getX(), pos.getY() - center.getY(), pos.getZ() - center.getZ(), radius);
	}

	/** Whether {@code pos} lies within {@code band} blocks of the shell, inside or out: the ground the shell runs through. */
	public boolean isNearShell(BlockPos pos, double band) {
		double distance = Math.sqrt(pos.distSqr(center));
		return Math.abs(distance - radius) <= band;
	}

	/** Whether {@code pos} is inside the sphere (shell included). */
	public boolean encloses(BlockPos pos) {
		return pos.distSqr(center) <= (radius + 0.5) * (radius + 0.5);
	}

	/**
	 * Walks through the barrier as if it weren't there: the player who owns it, the NPC whose concealment it is, and every
	 * member of the sect it protects.
	 */
	public boolean allows(Entity entity) {
		if (entity == null) return false;
		if (owner != null && owner.equals(entity.getUUID())) return true;
		return sect != null && entity instanceof CultivatorNpc npc && sect.equals(npc.getSectId());
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", id);
		tag.putString("Kind", kind.name());
		tag.putLong("Center", center.asLong());
		tag.putInt("Radius", radius);
		tag.putBoolean("Raised", raised);
		tag.putInt("Rank", rank);
		if (owner != null) tag.putUUID("Owner", owner);
		if (sect != null) tag.putUUID("Sect", sect);
		tag.putLong("Heartbeat", heartbeat);
		return tag;
	}

	/** A raised formation as the client hears of it (see ModPackets#sendFormations). */
	public static Formation view(UUID id, Kind kind, BlockPos center, int radius, int rank, UUID owner) {
		Formation f = new Formation(id, kind, center, radius);
		f.raised = true;
		f.rank = rank;
		f.owner = owner;
		return f;
	}

	public static Formation load(CompoundTag tag) {
		Kind kind;
		try {
			kind = Kind.valueOf(tag.getString("Kind"));
		} catch (IllegalArgumentException e) {
			kind = Kind.CORE;
		}
		Formation f = new Formation(tag.getUUID("Id"), kind, BlockPos.of(tag.getLong("Center")), tag.getInt("Radius"));
		f.raised = tag.getBoolean("Raised");
		f.rank = tag.getInt("Rank");
		if (tag.hasUUID("Owner")) f.owner = tag.getUUID("Owner");
		if (tag.hasUUID("Sect")) f.sect = tag.getUUID("Sect");
		f.heartbeat = tag.getLong("Heartbeat");
		return f;
	}

	// --- Shell geometry ---

	/** On the shell of a sphere of {@code radius}: the distance from the centre rounds to the radius. */
	public static boolean isShell(int dx, int dy, int dz, int radius) {
		return Math.round(Math.sqrt((double) dx * dx + (double) dy * dy + (double) dz * dz)) == radius;
	}

	private static final Map<Integer, int[]> SHELLS = new ConcurrentHashMap<>();

	/**
	 * Every offset on the shell of a sphere of {@code radius}, packed by {@link #pack} (about 12.6 r^2 of them: 51,000 at
	 * radius 64). Worked out column by column instead of testing the whole cube, and cached per radius.
	 */
	public static int[] shell(int radius) {
		return SHELLS.computeIfAbsent(radius, r -> {
			double inner = (r - 0.5) * (r - 0.5);
			double outer = (r + 0.5) * (r + 0.5);
			int[] out = new int[16 * r * r + 16];
			int n = 0;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					int h = dx * dx + dz * dz;
					if (h >= outer) continue;
					int minDy = (int) Math.ceil(Math.sqrt(Math.max(0, inner - h)));
					int maxDy = (int) Math.floor(Math.sqrt(outer - h - 1.0e-9));
					for (int dy = minDy; dy <= maxDy; dy++) {
						if (!isShell(dx, dy, dz, r)) continue; // guards the rounding at the edges
						if (n + 2 > out.length) out = java.util.Arrays.copyOf(out, out.length * 2);
						out[n++] = pack(dx, dy, dz);
						if (dy != 0) out[n++] = pack(dx, -dy, dz);
					}
				}
			}
			return java.util.Arrays.copyOf(out, n);
		});
	}

	/** Offsets of up to +-255 in one int. */
	public static int pack(int dx, int dy, int dz) {
		return (dx + 256) | (dy + 256) << 10 | (dz + 256) << 20;
	}

	public static int dx(int packed) { return (packed & 0x3FF) - 256; }
	public static int dy(int packed) { return (packed >> 10 & 0x3FF) - 256; }
	public static int dz(int packed) { return (packed >> 20 & 0x3FF) - 256; }
}
