package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A sect: its grounds (the posts its members keep, from the structure's blueprint), its shared alignment, and its members,
 * kept here (in {@link SectData}) whether or not their chunks are loaded, so titles and repopulation always see everyone.
 * <p>
 * <b>Titles</b> follow cultivation within the sect ({@link #recalculateTitles}), recalculated whenever a member advances,
 * dies or leaves: the strongest is the Sect Master, then the next tenth Grand Elders, the next fifth Elders, the next
 * three tenths Inner Disciples, and the rest Outer Disciples.
 * <p>
 * <b>Population.</b> A member killed by a player is never replaced (the sect's {@link #capacity} drops for good, so a sect can't
 * be farmed). One who dies otherwise, or leaves (reaching Heavenly Being), is replaced after a few days by a Qi Refining
 * Outer Disciple. A sect whose last member dies is {@link #extinct} and never repopulates.
 */
public final class Sect {
	/** A replacement disciple arrives this many ticks (2-4 Minecraft days) after a member is lost to anything but a player. */
	public static final long REPLACEMENT_MIN = 48_000;
	public static final long REPLACEMENT_SPREAD = 48_000;

	public static final class Member {
		public final UUID id;
		public int rank;
		public double cultivation;
		public NpcTitle title = NpcTitle.OUTER_DISCIPLE;
		public int home;

		Member(UUID id, int rank, int home) {
			this.id = id;
			this.rank = rank;
			this.home = home;
		}
	}

	public enum Post { MASTER_SEAT, MEDITATION, TREASURY, CORE, DWELLING, COURTYARD, GATE }

	public final UUID id;
	public String name = "";
	public ResourceKey<Level> dimension = Level.OVERWORLD;
	public BlockPos core = BlockPos.ZERO;
	public int barrierRadius = 48;
	public int alignment;
	public int capacity;
	public boolean extinct;
	/** The barrier is down against the sect's will (out of qi, shattered or its core destroyed) since this game time; -1 if up. */
	public long barrierDownSince = -1;
	/** The last time an Elder or Grand Elder fell: the Sect Master takes the field for a while after. */
	public long elderFellAt = -1;
	public BoundingBox bounds = new BoundingBox(BlockPos.ZERO);
	public BoundingBox treasury = new BoundingBox(BlockPos.ZERO);
	public final Map<UUID, Member> members = new LinkedHashMap<>();
	public final List<Long> replacements = new ArrayList<>();
	public final Map<Post, List<BlockPos>> posts = new EnumMap<>(Post.class);

	public Sect(UUID id) {
		this.id = id;
		for (Post post : Post.values()) posts.put(post, new ArrayList<>());
	}

	public List<BlockPos> posts(Post post) { return posts.get(post); }

	public BlockPos post(Post post, int index) {
		List<BlockPos> list = posts.get(post);
		return list.isEmpty() ? core : list.get(Math.floorMod(index, list.size()));
	}

	/** Member {@code index}'s home: a dwelling of its own (the master's is the seat in the main hall). */
	public BlockPos dwelling(int index) {
		if (index < 0) return post(Post.MASTER_SEAT, 0);
		return post(Post.DWELLING, index);
	}

	public int territoryRadius() { return barrierRadius + CultivatorNpc.TERRITORY_MARGIN; }

	/** Within the sect's territory (sideways from the core; height doesn't matter). */
	public boolean inTerritory(BlockPos pos) {
		double dx = pos.getX() - core.getX(), dz = pos.getZ() - core.getZ();
		return dx * dx + dz * dz <= (double) territoryRadius() * territoryRadius();
	}

	public boolean isBarrierDown() { return barrierDownSince >= 0; }

	/** The lowest dwelling index no member holds (or a shared one when they are all taken). */
	public int freeHome() {
		int count = Math.max(1, posts(Post.DWELLING).size());
		for (int i = 0; i < count; i++) {
			final int index = i;
			if (members.values().stream().noneMatch(m -> m.home == index)) return i;
		}
		return members.size() % count;
	}

	// --- Titles ---

	/** How many of {@code n} members hold each title, strongest first. */
	public static EnumMap<NpcTitle, Integer> titleCounts(int n) {
		EnumMap<NpcTitle, Integer> counts = new EnumMap<>(NpcTitle.class);
		if (n <= 0) return counts;
		int left = n - 1;
		counts.put(NpcTitle.SECT_MASTER, 1);
		int grand = n >= 6 ? Math.max(1, (int) Math.round(n * 0.1)) : 0;
		grand = Math.min(grand, left);
		left -= grand;
		int elders = Math.min(left, (int) Math.round(n * 0.2));
		left -= elders;
		int inner = Math.min(left, (int) Math.round(n * 0.3));
		left -= inner;
		counts.put(NpcTitle.GRAND_ELDER, grand);
		counts.put(NpcTitle.ELDER, elders);
		counts.put(NpcTitle.INNER_DISCIPLE, inner);
		counts.put(NpcTitle.OUTER_DISCIPLE, left);
		return counts;
	}

	/** The order of seniority: higher rank first, then more cultivation, then a fixed tiebreak. */
	public static final Comparator<Member> SENIORITY = Comparator.comparingInt((Member m) -> m.rank).reversed()
			.thenComparing(Comparator.comparingDouble((Member m) -> m.cultivation).reversed())
			.thenComparing(m -> m.id);

	/**
	 * Hands out the titles by seniority. @return true if anyone's title changed
	 */
	public boolean recalculateTitles() {
		List<Member> ordered = new ArrayList<>(members.values());
		ordered.sort(SENIORITY);
		EnumMap<NpcTitle, Integer> counts = titleCounts(ordered.size());
		boolean changed = false;
		int i = 0;
		for (NpcTitle title : new NpcTitle[] {NpcTitle.SECT_MASTER, NpcTitle.GRAND_ELDER, NpcTitle.ELDER, NpcTitle.INNER_DISCIPLE, NpcTitle.OUTER_DISCIPLE}) {
			int count = counts.getOrDefault(title, 0);
			for (int k = 0; k < count && i < ordered.size(); k++, i++) {
				Member member = ordered.get(i);
				if (member.title != title) {
					member.title = title;
					changed = true;
				}
			}
		}
		return changed;
	}

	/** The current Sect Master, or null for an empty sect. */
	public Member master() {
		return members.values().stream().filter(m -> m.title == NpcTitle.SECT_MASTER).findFirst().orElse(null);
	}

	/** Where this member stands guard or sits, by title: elders spread over the treasury and the core, in seniority order. */
	public BlockPos dutyPost(Member member) {
		if (member.title == NpcTitle.SECT_MASTER) return post(Post.MASTER_SEAT, 0);
		if (member.title == NpcTitle.GRAND_ELDER || member.title == NpcTitle.ELDER) {
			List<Member> elders = new ArrayList<>(members.values().stream().filter(m -> m.title == NpcTitle.GRAND_ELDER || m.title == NpcTitle.ELDER).toList());
			elders.sort(SENIORITY);
			int index = elders.indexOf(member);
			return index % 2 == 0 ? post(Post.TREASURY, index / 2) : post(Post.CORE, index / 2);
		}
		return dwelling(member.home);
	}

	// --- Saving ---

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", id);
		tag.putString("Name", name);
		tag.putString("Dimension", dimension.location().toString());
		tag.putLong("Core", core.asLong());
		tag.putInt("BarrierRadius", barrierRadius);
		tag.putInt("Alignment", alignment);
		tag.putInt("Capacity", capacity);
		tag.putBoolean("Extinct", extinct);
		tag.putLong("BarrierDownSince", barrierDownSince);
		tag.putLong("ElderFellAt", elderFellAt);
		tag.putIntArray("Bounds", new int[] {bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()});
		tag.putIntArray("Treasury", new int[] {treasury.minX(), treasury.minY(), treasury.minZ(), treasury.maxX(), treasury.maxY(), treasury.maxZ()});
		ListTag list = new ListTag();
		for (Member member : members.values()) {
			CompoundTag m = new CompoundTag();
			m.putUUID("Id", member.id);
			m.putInt("Rank", member.rank);
			m.putDouble("Cultivation", member.cultivation);
			m.putInt("Title", member.title.ordinal());
			m.putInt("Home", member.home);
			list.add(m);
		}
		tag.put("Members", list);
		tag.put("Replacements", new LongArrayTag(replacements.stream().mapToLong(Long::longValue).toArray()));
		CompoundTag postTag = new CompoundTag();
		for (Map.Entry<Post, List<BlockPos>> entry : posts.entrySet()) {
			postTag.put(entry.getKey().name(), new LongArrayTag(entry.getValue().stream().mapToLong(BlockPos::asLong).toArray()));
		}
		tag.put("Posts", postTag);
		return tag;
	}

	public static Sect load(CompoundTag tag) {
		Sect sect = new Sect(tag.getUUID("Id"));
		sect.name = tag.getString("Name");
		ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
		sect.dimension = dimension == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dimension);
		sect.core = BlockPos.of(tag.getLong("Core"));
		sect.barrierRadius = tag.getInt("BarrierRadius");
		sect.alignment = tag.getInt("Alignment");
		sect.capacity = tag.getInt("Capacity");
		sect.extinct = tag.getBoolean("Extinct");
		sect.barrierDownSince = tag.getLong("BarrierDownSince");
		sect.elderFellAt = tag.getLong("ElderFellAt");
		sect.bounds = box(tag.getIntArray("Bounds"), sect.core);
		sect.treasury = box(tag.getIntArray("Treasury"), sect.core);
		ListTag list = tag.getList("Members", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag m = list.getCompound(i);
			Member member = new Member(m.getUUID("Id"), m.getInt("Rank"), m.getInt("Home"));
			member.cultivation = m.getDouble("Cultivation");
			member.title = NpcTitle.byIndex(m.getInt("Title"));
			sect.members.put(member.id, member);
		}
		for (long time : tag.getLongArray("Replacements")) sect.replacements.add(time);
		CompoundTag postTag = tag.getCompound("Posts");
		for (Post post : Post.values()) {
			for (long pos : postTag.getLongArray(post.name())) sect.posts.get(post).add(BlockPos.of(pos));
		}
		return sect;
	}

	private static BoundingBox box(int[] values, BlockPos fallback) {
		return values.length == 6 ? new BoundingBox(values[0], values[1], values[2], values[3], values[4], values[5]) : new BoundingBox(fallback);
	}
}
