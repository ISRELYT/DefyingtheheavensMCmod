package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * What there is to hear about (see TalkSystem's Rumours topic): an index of real things in the world, fed as they come and go,
 * never by scanning chunks. Old herbs and Cultivation Fruit and Spirit Peach Trees note themselves as their block entities
 * load or change (their age is kept as a start age and a clock, so it is worked out at the telling); a cultivator going into
 * seclusion notes its cave; a sect's fall notes its unclaimed core; a cultivator slain by a player notes the killing (heard
 * of for {@link #KILLING_REMEMBERED} ticks). Rumours give a direction, a rough distance and a vague age, never coordinates,
 * and a stronger teller guesses closer.
 */
public final class Rumours extends SavedData {
	private static final String NAME = DefyingTheHeavens.MOD_ID + "_rumours";
	/** Herbs, fruit and trees are worth a rumour from this age. */
	public static final int OLD_FROM = 100;
	/** A killing is talked of for three days. */
	public static final long KILLING_REMEMBERED = 3 * 24_000L;
	/** How far a sect member hears of things, and how far a rogue (who travels). */
	public static final double LOCAL_REACH = 400;
	public static final double FAR_REACH = 2500;

	/** STELE: a Method Stele in the Netherworld, told of only by its dead. Saved by name: new kinds go at the end. */
	public enum Kind { HERB, FRUIT, PEACH_TREE, CAVE, CORE, KILLING, STELE }

	/** One thing worth telling of. */
	public static final class Rumour {
		public final String id;
		public final Kind kind;
		public final ResourceKey<Level> dimension;
		public final BlockPos pos;
		/** Herbs: the block's id; caves: the cultivator's name; cores: the sect's name; killings: the victim's name. */
		public final String subject;
		/** Herbs, fruit, trees: their age as a start and a clock (FruitAge). Killings: when (game time) and who did it. */
		public final int initialYears;
		public final long since;
		public final String detail;

		Rumour(String id, Kind kind, ResourceKey<Level> dimension, BlockPos pos, String subject, int initialYears, long since, String detail) {
			this.id = id;
			this.kind = kind;
			this.dimension = dimension;
			this.pos = pos;
			this.subject = subject;
			this.initialYears = initialYears;
			this.since = since;
			this.detail = detail;
		}

		int age(long now) {
			return FruitAge.at(initialYears, since, now);
		}

		/** Still worth telling: an old enough herb, a killing still talked of; anything else while it lasts. */
		boolean current(long now) {
			return switch (kind) {
				case HERB, FRUIT, PEACH_TREE -> age(now) >= OLD_FROM;
				case KILLING -> now - since < KILLING_REMEMBERED;
				case CAVE, CORE, STELE -> true;
			};
		}

		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Id", id);
			tag.putString("Kind", kind.name());
			tag.putString("Dimension", dimension.location().toString());
			tag.putLong("Pos", pos.asLong());
			tag.putString("Subject", subject);
			tag.putInt("InitialYears", initialYears);
			tag.putLong("Since", since);
			tag.putString("Detail", detail);
			return tag;
		}

		static Rumour load(CompoundTag tag) {
			Kind kind;
			try {
				kind = Kind.valueOf(tag.getString("Kind"));
			} catch (IllegalArgumentException e) {
				return null;
			}
			ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
			if (dimension == null) return null;
			return new Rumour(tag.getString("Id"), kind, ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(tag.getLong("Pos")),
					tag.getString("Subject"), tag.getInt("InitialYears"), tag.getLong("Since"), tag.getString("Detail"));
		}
	}

	private final Map<String, Rumour> rumours = new LinkedHashMap<>();
	/**
	 * Notes from block entities as their chunks load (when the world mustn't be touched): taken in on the next server tick.
	 * Removals are queued too, so they keep their order with the notes.
	 */
	private static final Queue<Object> PENDING = new ConcurrentLinkedQueue<>();

	private record Forget(String id) {}

	public static Rumours get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(Rumours::load, Rumours::new, NAME);
	}

	public static Rumours load(CompoundTag tag) {
		Rumours data = new Rumours();
		ListTag list = tag.getList("Rumours", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			Rumour rumour = Rumour.load(list.getCompound(i));
			if (rumour != null) data.rumours.put(rumour.id, rumour);
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Rumour rumour : rumours.values()) list.add(rumour.save());
		tag.put("Rumours", list);
		return tag;
	}

	// --- Feeding the index ---

	private static String placeId(String prefix, Level level, BlockPos pos) {
		return prefix + ":" + level.dimension().location() + ":" + pos.asLong();
	}

	/** A herb, fruit or tree with an age, as its block entity loads or its age is set (any thread; taken in next tick). */
	public static void noteAging(Level level, BlockPos pos, Kind kind, Block block, int initialYears, long plantedAt) {
		if (level.isClientSide || plantedAt < 0) return;
		String id = placeId(kind.name().toLowerCase(java.util.Locale.ROOT), level, pos);
		PENDING.add(new Rumour(id, kind, level.dimension(), pos.immutable(), BuiltInRegistries.BLOCK.getKey(block).toString(), initialYears, plantedAt, ""));
	}

	/** The herb, fruit or tree at {@code pos} is gone (dug up, picked, cut down). */
	public static void forgetAt(Level level, BlockPos pos, Kind kind) {
		if (level.isClientSide) return;
		PENDING.add(new Forget(placeId(kind.name().toLowerCase(java.util.Locale.ROOT), level, pos)));
	}

	/** A Heavenly Being has gone into seclusion at its mat. */
	public static void noteCave(CultivatorNpc npc) {
		if (npc.getSeclusionMat() == null || npc.level().isClientSide) return;
		PENDING.add(new Rumour("cave:" + npc.getUUID(), Kind.CAVE, npc.level().dimension(), npc.getSeclusionMat(), npc.getPersonalName(),
				0, npc.level().getGameTime(), ""));
	}

	public static void forgetCave(CultivatorNpc npc) {
		PENDING.add(new Forget("cave:" + npc.getUUID()));
	}

	/** A sect has fallen, its core left standing for whoever finds it. */
	public static void noteFallenSect(Sect sect, long now) {
		PENDING.add(new Rumour("core:" + sect.id, Kind.CORE, sect.dimension, sect.core, sect.name, 0, now, ""));
	}

	public static void forgetCore(Sect sect) {
		PENDING.add(new Forget("core:" + sect.id));
	}

	/** A Method Stele stands at {@code middle} (placed by the world, or the Gate's): any thread, taken in next tick. */
	public static void noteStele(ResourceKey<Level> dimension, BlockPos middle, GhostMethod method) {
		PENDING.add(new Rumour("stele:" + dimension.location() + ":" + middle.asLong(), Kind.STELE, dimension, middle.immutable(), method.getId(), 0, 0, ""));
	}

	/** A player has killed a cultivator. */
	public static void noteKilling(CultivatorNpc victim, ServerPlayer killer) {
		PENDING.add(new Rumour("kill:" + victim.getUUID(), Kind.KILLING, victim.level().dimension(), victim.blockPosition(),
				victim.getPersonalName(), 0, victim.level().getGameTime(), killer.getGameProfile().getName()));
	}

	/** Takes in what was noted since last tick; now and then lets old killings go. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty() && server.getTickCount() % 6000 != 0) return;
		Rumours data = get(server);
		Object next;
		while ((next = PENDING.poll()) != null) {
			if (next instanceof Rumour rumour) data.rumours.put(rumour.id, rumour);
			else if (next instanceof Forget forget) data.rumours.remove(forget.id());
		}
		long now = server.overworld().getGameTime();
		data.rumours.values().removeIf(r -> r.kind == Kind.KILLING && now - r.since >= KILLING_REMEMBERED);
		data.setDirty();
	}

	public static void clearPending() {
		PENDING.clear();
	}

	// --- Telling ---

	/** How far {@code npc} hears of things: rogues travel and hear of far-off things; sect members only of what is near. */
	public static double reach(CultivatorNpc npc) {
		return npc.isSectMember() ? LOCAL_REACH : FAR_REACH;
	}

	/**
	 * Up to {@code count} things {@code npc} could tell of that it hasn't told already ({@code told}), taking turns: the oldest
	 * herb, fruit or tree it knows of, then the nearest of the rest (a cave, a fallen core, a killing), and so on.
	 */
	public static List<Rumour> untold(CultivatorNpc npc, Set<String> told, int count) {
		if (!(npc.level() instanceof ServerLevel level)) return List.of();
		Rumours data = get(level.getServer());
		long now = level.getGameTime();
		double reach = reach(npc);
		List<Rumour> growing = new ArrayList<>();
		List<Rumour> other = new ArrayList<>();
		for (Rumour rumour : data.rumours.values()) {
			if (rumour.dimension != level.dimension() || told.contains(rumour.id) || !rumour.current(now)) continue;
			if (rumour.kind == Kind.STELE && !npc.isGhost()) continue; // only the dead tell of the steles
			if (rumour.pos.distSqr(npc.blockPosition()) > reach * reach) continue;
			if (gone(level, rumour)) {
				PENDING.add(new Forget(rumour.id));
				continue;
			}
			(rumour.kind == Kind.HERB || rumour.kind == Kind.FRUIT || rumour.kind == Kind.PEACH_TREE ? growing : other).add(rumour);
		}
		growing.sort(Comparator.comparingInt((Rumour r) -> -r.age(now)));
		other.sort(Comparator.comparingDouble(r -> r.pos.distSqr(npc.blockPosition())));
		List<Rumour> picked = new ArrayList<>();
		for (int i = 0; picked.size() < count && (i < growing.size() || i < other.size()); i++) {
			if (i < growing.size()) picked.add(growing.get(i));
			if (picked.size() < count && i < other.size()) picked.add(other.get(i));
		}
		return picked;
	}

	/** A herb, fruit or tree whose chunk is loaded and which isn't there any more (picked by someone with no rumour to go on). */
	private static boolean gone(ServerLevel level, Rumour rumour) {
		if (rumour.kind == Kind.STELE) return level.isLoaded(rumour.pos) && !level.getBlockState(rumour.pos).is(ModBlocks.METHOD_STELE);
		if (rumour.kind != Kind.HERB && rumour.kind != Kind.FRUIT && rumour.kind != Kind.PEACH_TREE) return false;
		if (!level.isLoaded(rumour.pos)) return false;
		ResourceLocation block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(rumour.pos).getBlock());
		return !block.toString().equals(rumour.subject);
	}

	/**
	 * What {@code teller} says of {@code rumour}: which way and roughly how far from where it stands, in what country, and for
	 * herbs how old. The stronger the teller (its realm), the closer its guesses.
	 */
	public static Component tell(Rumour rumour, CultivatorNpc teller) {
		ServerLevel level = (ServerLevel) teller.level();
		int realm = teller.isMortal() ? 0 : CultivatorNpc.realmOf(teller.getRank()).ordinal();
		BlockPos from = teller.blockPosition();
		Component direction = Component.translatable(ModLang.talkDirection(roughDirection(from, rumour.pos, realm)));
		double distance = Math.sqrt(from.distSqr(rumour.pos));
		int unit = realm < 2 ? 500 : realm < 4 ? 100 : 50;
		int rough = Math.max(unit, (int) Math.round(distance / unit) * unit);
		Component country = biomeName(level, rumour.pos);
		long now = level.getGameTime();
		return switch (rumour.kind) {
			case HERB, FRUIT, PEACH_TREE -> {
				Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(rumour.subject));
				Component name = rumour.kind == Kind.HERB ? block.getName() : Component.translatable(ModLang.rumourSubject(rumour.kind));
				yield Component.translatable(ModLang.rumour("old"), name, direction, country, rough, ageGuess(rumour.age(now), realm));
			}
			case CAVE -> Component.translatable(ModLang.rumour("cave"), rumour.subject, direction, country, rough);
			case CORE -> Component.translatable(ModLang.rumour("core"), rumour.subject, direction, country, rough);
			case KILLING -> {
				long days = (now - rumour.since) / 24_000L;
				Component when = days == 0 ? Component.translatable(ModLang.rumour("today"))
						: days == 1 ? Component.translatable(ModLang.rumour("yesterday")) : Component.translatable(ModLang.rumour("days_ago"), days);
				yield Component.translatable(ModLang.rumour("killing"), rumour.subject, direction, country, rough, when, rumour.detail);
			}
			case STELE -> {
				GhostMethod method = GhostMethod.byId(rumour.subject);
				Component name = method == null ? Component.literal(rumour.subject) : method.getDisplayName();
				yield Component.translatable(ModLang.rumour("stele"), name, direction, country, rough);
			}
		};
	}

	/** Eight points of the compass for a teller of Core Formation and above, four for the weaker. */
	static String roughDirection(BlockPos from, BlockPos to, int realm) {
		String exact = TalkSystem.direction(from, to);
		if (realm >= 2 || !exact.contains("_")) return exact;
		// The weak only know the four quarters: the nearer one of the two the diagonal lies between.
		double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
		return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
	}

	/** "five or six hundred": a range around {@code age}, narrower for a stronger teller. */
	static Component ageGuess(int age, int realm) {
		double spread = realm < 2 ? 0.4 : realm < 4 ? 0.2 : 0.08;
		int low = nice(age * (1 - spread));
		int high = nice(age * (1 + spread));
		if (low >= high) return Component.translatable(ModLang.rumour("about"), high);
		return Component.translatable(ModLang.rumour("between"), low, high);
	}

	/** Rounded the way people round ages: tens under a hundred, fifties under a thousand, hundreds above. */
	private static int nice(double years) {
		int step = years < 100 ? 10 : years < 1000 ? 50 : 100;
		return Math.max(step, (int) Math.round(years / step) * step);
	}

	/** The country at {@code pos}, from the world's biome noise (no chunk is loaded to find it). */
	static Component biomeName(ServerLevel level, BlockPos pos) {
		Holder<Biome> biome = level.getChunkSource().getGenerator().getBiomeSource()
				.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2, level.getChunkSource().randomState().sampler());
		return biome.unwrapKey().map(key -> (Component) Component.translatable("biome." + key.location().getNamespace() + "." + key.location().getPath()))
				.orElse(Component.translatable(ModLang.rumour("wilds")));
	}

	/** For the tests: everything known now. */
	public List<Rumour> all() {
		return List.copyOf(rumours.values());
	}

	private Rumours() {}
}
