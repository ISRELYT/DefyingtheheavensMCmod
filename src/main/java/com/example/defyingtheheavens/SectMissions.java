package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The sect mission board (the user's "A sect mission board is a great idea", 2026-10-28; their rule: the missions are the players' alone,
 * the sect's people only talk of them in passing; the rest my plan, approved as it stood).
 * <ul>
 *   <li><b>The board.</b> Every sect sets one up on its plaza by its Formation Core ({@link #layBoards}: old sects too, the first time a
 *   player comes near), its face to the gate; one found gone is set up again the next day. Its disciples open it (MissionBoardBlock) on a
 *   page of two halves (MissionBoardScreen): the missions, and the exchange (SectExchange).</li>
 *   <li><b>Postings.</b> Each disciple's own: {@link #POSTINGS} each game day, fitted to their realm (Easy a stage below them, Fitting at
 *   it, Hard a stage or two above: {@link Grade}), {@link #HELD} held at once. Six kinds ({@link Kind}): a beast to hunt and a cultivator to
 *   purge out on the land, things to gather, pills to brew or an artifact to forge, a sealed letter to carry to another sect, and the gate
 *   to watch through three waves.</li>
 *   <li><b>In the field.</b> A mission taken gives a Mission Slip (a Sealed Letter for a delivery: MissionSlipItem), which held shows the
 *   way. A hunt's or a purge's quarry comes out of the land, glowing, when its taker draws within {@link #COMES_WITHIN}, and fights them
 *   alone; it is theirs when it falls by their hand (its spoils too: {@link #spoilsDrop}). A taken mission stands its kind's days, and
 *   lapses after (the sect thinks a little less of them).</li>
 *   <li><b>Contribution.</b> A mission seen through pays Contribution (a number on the disciple's record, never an item: nothing to drop
 *   or steal) and raises the sect's regard; leaving the sect leaves it behind.</li>
 *   <li><b>The sect's talk.</b> Its people speak of its missions in passing (NpcLife): of a deed done, of the gate watch a disciple has
 *   taken, of the day's new postings.</li>
 * </ul>
 */
public final class SectMissions {
	/** The postings a disciple finds on the board each game day, and how many missions they may hold at once. */
	public static final int POSTINGS = 5, HELD = 3;
	/** A hunt's or a purge's quarry keeps this far out from the sect's Formation Core (blocks), and comes out when its taker draws this near. */
	public static final int OUT_FROM = 100, OUT_TO = 400, COMES_WITHIN = 48;
	/** A letter goes to a sect this far off (blocks), and no farther. */
	public static final int LETTER_FROM = 300, LETTER_TO = 3000;
	/** The gate watch: this long (ticks), stood within this of the gate (blocks); its waves come at these marks; away this long, it is lost. */
	public static final int WATCH_TICKS = 3 * 60 * 20, WATCH_REACH = 12, WATCH_AWAY = 20 * 20;
	static final int[] WAVES_AT = {10 * 20, 70 * 20, 130 * 20};
	/** A quarry slain by another's hand comes out again only this long after (ticks): no spoils to be had of it meanwhile. */
	static final int QUARRY_AGAIN = 60 * 20;
	/** Earned all told in one sect, a pillar of it (Milestones#MISSION_PILLAR). */
	public static final int PILLAR = 1000;
	/** A sect sets up its board while a player is this near its core, this far round the core on its plaza (blocks). */
	static final int LAY_NEAR = 128, BOARD_FROM = 4, BOARD_TO = 9;
	/** A disciple stands this near the board to use it (blocks). */
	static final double BOARD_REACH = 8;
	/** A deed done stays on the sect's lips this long (ticks): a day. */
	static final long TALKED_OF = HeavenRitual.DAY;
	/** The board's acts (C2S mission_act). */
	public static final int TAKE = 0, ABANDON = 1, HAND_IN = 2, SLIP = 3, BUY = 4;
	/** What a mission on the board may be done with now (S2C mission_board): bits. */
	public static final int CAN_TAKE = 1, CAN_ABANDON = 2, CAN_HAND_IN = 4, CAN_SLIP = 8;
	/** An artifact asked for: its kind's id after this. */
	static final String ARTIFACT = "artifact/";
	private static final String FOE_TAG = "dth_mission_foe", FOE_TAKER = "dth_mission_taker:", FOE_MISSION = "dth_mission:";

	/** The kinds of mission, each with its pay before realm and grade (Contribution) and the game days it stands once taken. */
	public enum Kind {
		HUNT("hunt", 20, 2), PURGE("purge", 24, 2), GATHER("gather", 12, 3), CRAFT("craft", 16, 3), DELIVER("deliver", 10, 2), GUARD("guard", 20, 1);

		private final String id;
		final int base, days;

		Kind(String id, int base, int days) {
			this.id = id;
			this.base = base;
			this.days = days;
		}

		public String getId() { return id; }

		public Component displayName() { return Component.translatable(ModLang.missionKind(id)); }

		public static Kind byId(String id) {
			for (Kind kind : values()) if (kind.id.equals(id)) return kind;
			return null;
		}

		public static Kind byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : null;
		}
	}

	/** How hard a mission is for its taker: stages from their own rank, and its pay for that. */
	public enum Grade {
		EASY("easy", -1, 0.75), FITTING("fitting", 0, 1.0), HARD("hard", 1, 1.5);

		private final String id;
		final int stages;
		final double pay;

		Grade(String id, int stages, double pay) {
			this.id = id;
			this.stages = stages;
			this.pay = pay;
		}

		public String getId() { return id; }

		public Component displayName() { return Component.translatable(ModLang.missionGrade(id)); }

		public static Grade byId(String id) {
			for (Grade grade : values()) if (grade.id.equals(id)) return grade;
			return FITTING;
		}

		public static Grade byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : FITTING;
		}
	}

	/**
	 * One mission, posted or taken. {@code what}: a gather's or a brew's item id, a forge's {@link #ARTIFACT} and kind, a hunt's beast kind, a
	 * purge's quarry's path, a letter's sect's name. {@code quality}: a herb's or a fruit's least years, a demon core's least realm, a pill's
	 * or an artifact's least grade. {@code at}: the quarry's ground, the letter's sect's core, the gate. {@code target}: the purge's quarry
	 * (its name is NpcNames#of it), the letter's sect.
	 */
	public static final class Mission {
		public final int id;
		public final Kind kind;
		public final Grade grade;
		/** The strength it asks for (PlayerCultivation#rank): its quarry's, its things' realm. */
		public final int rank;
		public int reward, standing;
		public String what = "";
		public int count = 1, quality;
		public BlockPos at;
		public ResourceKey<Level> dimension;
		public UUID target;
		/** When it lapses (game time); -1 while it is only posted. */
		public long deadline = -1;
		/** Quarry slain, or the watch's ticks; the watch's ticks away (a quarry's wait to come again); the watch's waves sent. */
		public int progress, away, waves;
		/** Its quarry, or the watch's foes, out in the world now. */
		public final List<UUID> foes = new ArrayList<>();

		public Mission(int id, Kind kind, Grade grade, int rank) {
			this.id = id;
			this.kind = kind;
			this.grade = grade;
			this.rank = rank;
		}

		public boolean isTaken() { return deadline >= 0; }

		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putInt("Id", id);
			tag.putString("Kind", kind.getId());
			tag.putString("Grade", grade.getId());
			tag.putInt("Rank", rank);
			tag.putInt("Reward", reward);
			tag.putInt("Standing", standing);
			tag.putString("What", what);
			tag.putInt("Count", count);
			tag.putInt("Quality", quality);
			if (at != null) tag.putLong("At", at.asLong());
			if (dimension != null) tag.putString("Dimension", dimension.location().toString());
			if (target != null) tag.putUUID("Target", target);
			tag.putLong("Deadline", deadline);
			tag.putInt("Progress", progress);
			tag.putInt("Away", away);
			tag.putInt("Waves", waves);
			ListTag ids = new ListTag();
			for (UUID foe : foes) ids.add(NbtUtils.createUUID(foe));
			tag.put("Foes", ids);
			return tag;
		}

		static @Nullable Mission load(CompoundTag tag) {
			Kind kind = Kind.byId(tag.getString("Kind"));
			if (kind == null) return null;
			Mission m = new Mission(tag.getInt("Id"), kind, Grade.byId(tag.getString("Grade")), tag.getInt("Rank"));
			m.reward = tag.getInt("Reward");
			m.standing = tag.getInt("Standing");
			m.what = tag.getString("What");
			m.count = Math.max(1, tag.getInt("Count"));
			m.quality = tag.getInt("Quality");
			if (tag.contains("At")) m.at = BlockPos.of(tag.getLong("At"));
			ResourceLocation dimension = tag.contains("Dimension") ? ResourceLocation.tryParse(tag.getString("Dimension")) : null;
			if (dimension != null) m.dimension = ResourceKey.create(Registries.DIMENSION, dimension);
			if (tag.hasUUID("Target")) m.target = tag.getUUID("Target");
			m.deadline = tag.getLong("Deadline");
			m.progress = tag.getInt("Progress");
			m.away = tag.getInt("Away");
			m.waves = tag.getInt("Waves");
			ListTag ids = tag.getList("Foes", Tag.TAG_INT_ARRAY);
			for (Tag id : ids) m.foes.add(NbtUtils.loadUUID(id));
			return m;
		}
	}

	/** A deed seen through, as the sect talks of it for a day: the sect, what was done, when. */
	private record Deed(UUID sect, Component what, long at) {}

	/** Each disciple's last deed (runtime: the talk of a day). */
	private static final Map<UUID, Deed> DEEDS = new HashMap<>();
	/** The board each player opened last: its world, its place and its sect (null: none), so its acts are answered only there. */
	private record Opened(ResourceKey<Level> dimension, BlockPos pos, @Nullable UUID sect) {}

	private static final Map<UUID, Opened> OPEN_AT = new HashMap<>();

	/** A mission found by its sect, its disciple and itself. */
	public record Found(Sect sect, Sect.Disciple disciple, Mission mission) {}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(SectMissions::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(SectMissions::onDeath);
		// A quarry come back with its land after its mission is over (seen through, abandoned, lapsed) slips away, a tick on.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (isFoe(entity) && !wanted(level.getServer(), entity)) level.getServer().tell(new TickTask(level.getServer().getTickCount(), entity::discard));
		});
	}

	// --- Saving, on the disciple's record (Sect) ---

	static void save(Sect.Disciple d, CompoundTag tag) {
		tag.putInt("Contribution", d.contribution);
		tag.putInt("Earned", d.earned);
		tag.putLong("PostedDay", d.postedDay);
		tag.put("Postings", list(d.postings));
		tag.put("Taken", list(d.taken));
		tag.putInt("NextMission", d.nextMission);
		tag.putInt("KindsDone", d.kindsDone);
		tag.putLong("BoughtDay", d.boughtDay);
		CompoundTag bought = new CompoundTag();
		d.bought.forEach(bought::putInt);
		tag.put("Bought", bought);
	}

	static void load(Sect.Disciple d, CompoundTag tag) {
		d.contribution = tag.getInt("Contribution");
		d.earned = tag.getInt("Earned");
		d.postedDay = tag.contains("PostedDay") ? tag.getLong("PostedDay") : -1;
		read(tag.getList("Postings", Tag.TAG_COMPOUND), d.postings);
		read(tag.getList("Taken", Tag.TAG_COMPOUND), d.taken);
		d.nextMission = Math.max(1, tag.getInt("NextMission"));
		d.kindsDone = tag.getInt("KindsDone");
		d.boughtDay = tag.contains("BoughtDay") ? tag.getLong("BoughtDay") : -1;
		CompoundTag bought = tag.getCompound("Bought");
		for (String key : bought.getAllKeys()) d.bought.put(key, bought.getInt(key));
	}

	private static ListTag list(List<Mission> missions) {
		ListTag list = new ListTag();
		for (Mission m : missions) list.add(m.save());
		return list;
	}

	private static void read(ListTag list, List<Mission> into) {
		for (int i = 0; i < list.size(); i++) {
			Mission m = Mission.load(list.getCompound(i));
			if (m != null) into.add(m);
		}
	}

	// --- The postings ---

	/** The rank a disciple's missions are measured by: theirs as cultivators weigh it (either path's). */
	static int rankOf(ServerPlayer player) {
		return Math.max(0, CultivatorNpc.rankOf(player));
	}

	/** {@code d}'s postings on {@code sect}'s board: put up afresh each game day (the missions they have taken stay theirs). */
	static void refresh(ServerPlayer player, Sect sect, Sect.Disciple d) {
		long today = Regard.day(player.level().getGameTime());
		if (d.postedDay == today) return;
		d.postedDay = today;
		d.postings.clear();
		ServerLevel level = player.server.getLevel(sect.dimension);
		if (level != null) d.postings.addAll(post(level, sect, d, rankOf(player), player.getRandom()));
		SectManager.dirty(player.server);
	}

	/**
	 * A day's postings for one of {@code rank}: {@link #POSTINGS} of them (one Easy, two Fitting, a Hard, and one more of either), each of the
	 * next kind round that can be had there (no beast that strong lives, no sect to write to, no gate: the next kind instead), at most one
	 * letter and one watch.
	 */
	static List<Mission> post(ServerLevel level, Sect sect, Sect.Disciple d, int rank, RandomSource random) {
		List<Grade> grades = List.of(Grade.EASY, Grade.FITTING, Grade.FITTING, Grade.HARD, random.nextBoolean() ? Grade.FITTING : Grade.HARD);
		List<Kind> kinds = new ArrayList<>(List.of(Kind.values()));
		for (int i = kinds.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			Kind swap = kinds.get(i);
			kinds.set(i, kinds.get(j));
			kinds.set(j, swap);
		}
		List<Mission> posted = new ArrayList<>();
		int next = 0;
		for (Grade grade : grades) {
			Mission made = null;
			for (int tries = 0; tries < kinds.size() && made == null; tries++) {
				Kind kind = kinds.get(next++ % kinds.size());
				if ((kind == Kind.GUARD || kind == Kind.DELIVER) && posted.stream().anyMatch(m -> m.kind == kind)) continue;
				made = make(level, sect, d, kind, grade, rank, random);
			}
			if (made != null) posted.add(made);
		}
		return posted;
	}

	/** A mission of {@code kind} and {@code grade} for one of {@code rank}, or null if none of the kind can be had here. */
	static @Nullable Mission make(ServerLevel level, Sect sect, Sect.Disciple d, Kind kind, Grade grade, int rank, RandomSource random) {
		int stages = grade.stages + (grade == Grade.HARD && random.nextInt(3) == 0 ? 1 : 0);
		int at = Mth.clamp(rank + stages, 0, PlayerCultivation.LAST_RANK);
		Mission m = new Mission(d.nextMission, kind, grade, at);
		switch (kind) {
			case HUNT -> {
				BeastKind beast = quarryFor(level, at, random);
				if (beast == null) return null;
				m.what = beast.getId();
				m.count = beast.signature == BeastKind.Signature.PACK ? 3 : 1;
				m.dimension = sect.dimension;
				m.at = beast.isChaos() ? frontier(sect, at) : groundOut(level, sect, random);
				if (m.at == null) return null;
			}
			case PURGE -> {
				m.at = groundOut(level, sect, random);
				if (m.at == null) return null;
				m.dimension = sect.dimension;
				m.target = UUID.randomUUID();
				m.what = foePath(sect).getId();
			}
			case GATHER -> {
				List<Need> needs = needs(CultivatorNpc.realmOf(at));
				Need need = needs.get(random.nextInt(needs.size()));
				m.what = BuiltInRegistries.ITEM.getKey(need.item()).toString();
				m.count = Math.max(1, (int) Math.round(need.count() * (grade == Grade.EASY ? 0.75 : grade == Grade.HARD ? 1.5 : 1)));
				m.quality = need.quality();
			}
			case CRAFT -> {
				Realm realm = CultivatorNpc.realmOf(at);
				if (random.nextBoolean()) {
					Item[] pills = pillsFor(realm);
					Item pill = pills[random.nextInt(pills.length)];
					m.what = BuiltInRegistries.ITEM.getKey(pill).toString();
					m.count = pill == ModItems.CORE_PILL || pill == ModItems.LONGEVITY_PILL ? 1
							: pill == ModItems.FOUNDATION_PILL || pill == ModItems.MARROW_CLEANSING_ELIXIR || pill == ModItems.YANG_PRESERVATION_PILL ? 2 : 3;
					m.quality = pillGrade(realm, grade).ordinal();
				} else {
					ArtifactGrade forged = artifactGrade(realm);
					// A Hard one of Spirit grade or better: the weapon made of the sect's own element.
					ArtifactKind artifact = grade == Grade.HARD && forged != ArtifactGrade.MORTAL ? ArtifactKind.madeOf(sect.element())
							: PLAIN[random.nextInt(PLAIN.length)];
					m.what = ARTIFACT + artifact.getId();
					m.quality = forged.ordinal();
				}
			}
			case DELIVER -> {
				Sect to = letterSect(level.getServer(), sect, random);
				if (to == null) return null;
				m.target = to.id;
				m.what = to.name;
				m.at = to.core;
				m.dimension = to.dimension;
			}
			case GUARD -> {
				if (sect.posts(Sect.Post.GATE).isEmpty()) return null;
				m.at = sect.post(Sect.Post.GATE, 0);
				m.dimension = sect.dimension;
			}
		}
		d.nextMission++;
		m.reward = reward(kind, grade, at, kind == Kind.DELIVER ? (int) Math.sqrt(m.at.distSqr(sect.core)) : 0);
		m.standing = grade == Grade.HARD ? 2 : 1;
		return m;
	}

	/** The Contribution a mission pays: its kind's, more for each realm, by its grade; a letter, more for every hundred blocks it goes. */
	static int reward(Kind kind, Grade grade, int rank, int distance) {
		int realm = CultivatorNpc.realmOf(rank).ordinal();
		double pay = kind.base * (1 + realm * 0.5) * grade.pay;
		if (kind == Kind.DELIVER) pay += distance / 100.0 * (1 + realm * 0.25);
		return Math.max(1, (int) Math.round(pay));
	}

	/**
	 * A beast for a hunt of {@code rank}: a wild kind whose ranks reach it (Qi Refining to Core Formation), or, for a sect of the Immortal
	 * Realm, a Chaos fiend (True Immortal on). Null if no beast lives that strong, or the world is at peace (Peaceful takes the monsters).
	 * Not a flood dragon: it keeps to the deep water, with no ground of the land's to meet it on.
	 */
	static @Nullable BeastKind quarryFor(ServerLevel level, int rank, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) return null;
		List<BeastKind> fits = new ArrayList<>();
		for (BeastKind kind : BeastKind.values()) {
			boolean wild = kind.tier == BeastKind.Tier.COMMON || kind.tier == BeastKind.Tier.UNCOMMON || kind.tier == BeastKind.Tier.RARE;
			boolean fiend = kind == BeastKind.CHAOS_FIEND && ModDimensions.isImmortalRealm(level.dimension());
			if (!wild && !fiend || kind.moves == BeastKind.Moves.SWIMMING) continue;
			if (rank >= kind.minRank() && rank <= kind.maxRank()) fits.add(kind);
		}
		return fits.isEmpty() ? null : fits.get(random.nextInt(fits.size()));
	}

	static @Nullable BeastKind beast(String id) {
		for (BeastKind kind : BeastKind.values()) if (kind.getId().equals(id)) return kind;
		return null;
	}

	/** The path of those who prey on {@code sect}'s people: the righteous on a devilish sect's, the devilish on anyone else's. */
	static Alignment.Faction foePath(Sect sect) {
		return Alignment.factionOf(sect.alignment) == Alignment.Faction.DEVILISH ? Alignment.Faction.RIGHTEOUS : Alignment.Faction.DEVILISH;
	}

	/**
	 * Open land {@link #OUT_FROM} to {@link #OUT_TO} blocks out from {@code sect}'s core, out of any sect's grounds, read from the land's own
	 * shape (nothing loaded): not the void, nor water. Null if none is found.
	 */
	static @Nullable BlockPos groundOut(ServerLevel level, Sect sect, RandomSource random) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState state = level.getChunkSource().randomState();
		for (int tries = 0; tries < 24; tries++) {
			double angle = random.nextDouble() * Math.PI * 2;
			int distance = OUT_FROM + random.nextInt(OUT_TO - OUT_FROM + 1);
			int x = sect.core.getX() + (int) Math.round(Math.cos(angle) * distance), z = sect.core.getZ() + (int) Math.round(Math.sin(angle) * distance);
			int top = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, state);
			int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, state);
			if (top <= level.getMinBuildHeight() + 4 || top - floor > 1) continue;
			BlockPos at = new BlockPos(x, top, z);
			if (inAnySect(level, at)) continue;
			return at;
		}
		return null;
	}

	private static boolean inAnySect(ServerLevel level, BlockPos pos) {
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (!sect.extinct && sect.dimension == level.dimension() && sect.inTerritory(pos)) return true;
		}
		return false;
	}

	/**
	 * Where in the Chaos Frontier fiends of {@code rank} keep, on the line from the realm's heart out through {@code sect}: as deep past its
	 * edge as their strength (ChaosFrontier: True Immortals near the edge, Earth Immortals deeper, Heaven Immortals beyond).
	 */
	static BlockPos frontier(Sect sect, int rank) {
		double dx = sect.core.getX() - ModNoiseSettings.MAIN_X, dz = sect.core.getZ() - ModNoiseSettings.MAIN_Z;
		double length = Math.hypot(dx, dz);
		if (length < 1) {
			dx = 1;
			dz = 0;
			length = 1;
		}
		Realm realm = CultivatorNpc.realmOf(rank);
		double depth = realm == Realm.TRUE_IMMORTAL ? ChaosFrontier.SHALLOW / 2
				: realm == Realm.EARTH_IMMORTAL ? (ChaosFrontier.SHALLOW + ChaosFrontier.DEEP) / 2 : ChaosFrontier.DEEP + 400;
		double out = ChaosFrontier.EDGE + depth;
		return BlockPos.containing(ModNoiseSettings.MAIN_X + dx / length * out, sect.core.getY(), ModNoiseSettings.MAIN_Z + dz / length * out);
	}

	/**
	 * A sect a letter of {@code sect}'s may go to: in its world, {@link #LETTER_FROM} to {@link #LETTER_TO} blocks off, not of the other path,
	 * a demon clan's to a clan and a sect of men's to men; a real one, its grounds laid out (a gate). Null if none is.
	 */
	static @Nullable Sect letterSect(MinecraftServer server, Sect sect, RandomSource random) {
		Alignment.Faction ours = Alignment.factionOf(sect.alignment);
		List<Sect> fits = new ArrayList<>();
		for (Sect other : SectData.get(server).all()) {
			if (other == sect || other.extinct || other.dimension != sect.dimension || other.isClan() != sect.isClan()) continue;
			if (other.posts(Sect.Post.GATE).isEmpty() || ours.isEnemyOf(Alignment.factionOf(other.alignment))) continue;
			double distance = Math.sqrt(other.core.distSqr(sect.core));
			if (distance >= LETTER_FROM && distance <= LETTER_TO) fits.add(other);
		}
		return fits.isEmpty() ? null : fits.get(random.nextInt(fits.size()));
	}

	/** What the sect's stores may want: an item, how many, and its least years (a herb, a fruit) or realm (a demon core). */
	record Need(Item item, int count, int quality) {}

	/** The stores' wants of one of {@code realm}: herbs older the higher, demon cores, jade and metal; the Immortal Realm's own past True Immortal. */
	static List<Need> needs(Realm realm) {
		int r = realm.ordinal();
		if (r == 0) {
			return List.of(new Need(ModItems.GINSENG, 4, 10), new Need(ModItems.HUANGJING, 4, 10), new Need(ModItems.LINGZHI, 3, 10),
					new Need(ModItems.SPIRIT_DEW, 6, 0), new Need(ModItems.DEMON_CORE, 2, 0), new Need(Items.IRON_INGOT, 12, 0));
		}
		if (r == 1) {
			return List.of(new Need(ModItems.SPIRIT_GINSENG, 3, 30), new Need(ModItems.PURPLE_LINGZHI, 3, 30), new Need(ModItems.OCHRE_HUANGJING, 3, 30),
					new Need(ModItems.DEMON_CORE, 2, 1), new Need(ModItems.JADE, 6, 0), new Need(Items.GOLD_INGOT, 8, 0));
		}
		if (r == 2) {
			return List.of(new Need(ModItems.SPIRIT_LOTUS, 2, 60), new Need(ModItems.SPIRIT_GINSENG, 4, 60), new Need(ModItems.DEMON_CORE, 2, 2),
					new Need(ModItems.JADE, 12, 0), new Need(Items.DIAMOND, 3, 0), new Need(ModItems.CULTIVATION_FRUIT, 1, 50));
		}
		if (r <= 5) {
			int years = 100 * (r - 2);
			return List.of(new Need(ModItems.SPIRIT_LOTUS, 2, years), new Need(ModItems.PURPLE_LINGZHI, 3, years), new Need(ModItems.OCHRE_HUANGJING, 3, years),
					new Need(ModItems.DEMON_CORE, 3, 2), new Need(Items.DIAMOND, 6, 0), new Need(ModItems.JADE, 24, 0), new Need(ModItems.CULTIVATION_FRUIT, 1, years));
		}
		if (r <= 9) {
			return List.of(new Need(ModItems.SPIRIT_LOTUS, 3, 500), new Need(ModItems.SPIRIT_GINSENG, 3, 500), new Need(ModItems.CULTIVATION_FRUIT, 1, 500),
					new Need(Items.NETHERITE_INGOT, 1, 0), new Need(Items.DIAMOND, 12, 0), new Need(ModItems.JADE, 48, 0));
		}
		return List.of(new Need(ModItems.IMMORTAL_JADE, 8, 0), new Need(ModItems.CHAOS_ORIGIN_STONE, 4, 0), new Need(ModItems.SPIRIT_LOTUS, 2, 1000),
				new Need(ModItems.CULTIVATION_FRUIT, 1, 1000), new Need(Items.NETHERITE_INGOT, 2, 0));
	}

	/** The pills a brew of {@code realm} asks for. */
	static Item[] pillsFor(Realm realm) {
		return switch (realm.ordinal()) {
			case 0 -> new Item[] {ModItems.QI_GATHERING_PILL, ModItems.MARROW_CLEANSING_ELIXIR};
			case 1 -> new Item[] {ModItems.QI_GATHERING_PILL, ModItems.CULTIVATION_PILL};
			case 2 -> new Item[] {ModItems.FOUNDATION_PILL, ModItems.CULTIVATION_PILL};
			default -> new Item[] {ModItems.CORE_PILL, ModItems.CULTIVATION_PILL, ModItems.LONGEVITY_PILL, ModItems.YANG_PRESERVATION_PILL};
		};
	}

	/** The least grade of pill a brew asks of one of {@code realm}: Low to Supreme by realm, a grade less if Easy, more if Hard. */
	static PillGrade pillGrade(Realm realm, Grade grade) {
		int r = realm.ordinal();
		int base = r <= 1 ? 0 : r <= 3 ? 1 : r <= 9 ? 2 : 3;
		return PillGrade.values()[Mth.clamp(base + grade.stages, 0, PillGrade.values().length - 1)];
	}

	/** The grade of artifact a forging asks of one of {@code realm}: Mortal at Qi Refining and Foundation Building, up to Immortal past Entering Nirvana. */
	static ArtifactGrade artifactGrade(Realm realm) {
		int r = realm.ordinal();
		return r <= 1 ? ArtifactGrade.MORTAL : r <= 3 ? ArtifactGrade.SPIRIT : r <= 5 ? ArtifactGrade.EARTH : r <= 9 ? ArtifactGrade.HEAVEN : ArtifactGrade.IMMORTAL;
	}

	/** The artifacts of no element a forging may ask for. */
	static final ArtifactKind[] PLAIN = {ArtifactKind.FLYING_SWORD, ArtifactKind.SABER, ArtifactKind.SPEAR, ArtifactKind.GLAIVE, ArtifactKind.GOURD,
			ArtifactKind.JADE, ArtifactKind.BANNER};

	static @Nullable ArtifactKind artifactKind(String what) {
		if (!what.startsWith(ARTIFACT)) return null;
		String id = what.substring(ARTIFACT.length());
		for (ArtifactKind kind : ArtifactKind.values()) if (kind.getId().equals(id)) return kind;
		return null;
	}

	static @Nullable Item item(String id) {
		ResourceLocation key = ResourceLocation.tryParse(id);
		return key == null || !BuiltInRegistries.ITEM.containsKey(key) ? null : BuiltInRegistries.ITEM.get(key);
	}

	/** What a gathering or a brew or a forging asks for, as one of it would show (its least years, grade). */
	static ItemStack thing(Mission m) {
		ArtifactKind kind = artifactKind(m.what);
		if (kind != null) {
			ArtifactGrade grade = ArtifactGrade.values()[Mth.clamp(m.quality, 0, ArtifactGrade.values().length - 1)];
			if (!kind.has(grade)) grade = ArtifactGrade.SPIRIT;
			return Artifacts.create(kind, grade, Artifacts.Quality.NORMAL, kind == ArtifactKind.BANNER ? QiElement.METAL : null);
		}
		Item item = item(m.what);
		if (item == null) return ItemStack.EMPTY;
		if (m.kind == Kind.CRAFT) return PillItem.create(item, PillGrade.values()[Mth.clamp(m.quality, 0, PillGrade.values().length - 1)], 1);
		if (item instanceof GinsengItem) return GinsengItem.create(item, Math.max(1, m.quality));
		if (item == ModItems.CULTIVATION_FRUIT) return CultivationFruitItem.create(Math.max(1, m.quality));
		if (item == ModItems.DEMON_CORE) return DemonCoreItem.of(PlayerCultivation.rank(Realm.byIndex(m.quality), Stage.EARLY), null, null);
		return new ItemStack(item);
	}

	/** What a gathering or a brew or a forging asks for, by name alone ("Ginseng", "Qi Gathering Pill", "Spirit Saber"). */
	static Component name(Mission m) {
		if (artifactKind(m.what) != null) return thing(m).getHoverName();
		Item item = item(m.what);
		return item == null ? Component.literal(m.what) : item.getDescription();
	}

	/** Whether {@code stack} is one of what {@code m} asks for: the item, of its least years, realm or grade or better. */
	static boolean matches(ItemStack stack, Mission m) {
		if (stack.isEmpty()) return false;
		ArtifactKind kind = artifactKind(m.what);
		if (kind != null) {
			for (ArtifactGrade grade : ArtifactGrade.values()) {
				if (grade.ordinal() >= m.quality && kind.has(grade) && stack.is(ModItems.artifact(kind, grade))) return true;
			}
			return false;
		}
		Item item = item(m.what);
		if (item == null || !stack.is(item)) return false;
		if (m.kind == Kind.CRAFT) return PillItem.grade(stack).ordinal() >= m.quality;
		if (item instanceof GinsengItem) return GinsengItem.age(stack) >= m.quality;
		if (item == ModItems.CULTIVATION_FRUIT) return CultivationFruitItem.age(stack) >= m.quality;
		if (item == ModItems.DEMON_CORE) return DemonCoreItem.realm(stack).ordinal() >= m.quality;
		return true;
	}

	/** How many of what {@code m} asks for {@code player} carries. */
	static int carried(ServerPlayer player, Mission m) {
		int count = 0;
		for (ItemStack stack : player.getInventory().items) if (matches(stack, m)) count += stack.getCount();
		for (ItemStack stack : player.getInventory().offhand) if (matches(stack, m)) count += stack.getCount();
		return count;
	}

	// --- Its words: what a mission is, as the board and the slip and the sect's people say it ---

	/** Its posting's heading: "Hunt: Bloodfang Tiger". */
	public static Component title(Mission m) {
		return switch (m.kind) {
			case HUNT -> Component.translatable(ModLang.missionTitle("hunt"), beastName(m));
			case PURGE -> Component.translatable(ModLang.missionTitle("purge"), quarryName(m));
			case GATHER -> Component.translatable(ModLang.missionTitle("gather"), m.count, name(m));
			case CRAFT -> Component.translatable(ModLang.missionTitle(artifactKind(m.what) != null ? "forge" : "brew"), m.count, name(m));
			case DELIVER -> Component.translatable(ModLang.missionTitle("deliver"), m.what);
			case GUARD -> Component.translatable(ModLang.missionTitle("guard"));
		};
	}

	private static Component beastName(Mission m) {
		BeastKind kind = beast(m.what);
		return kind == null ? Component.literal(m.what) : kind.getDisplayName();
	}

	private static Component quarryName(Mission m) {
		return Component.literal(m.target == null ? "?" : NpcNames.of(m.target));
	}

	private static Component rankName(int rank) {
		return PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
	}

	/** What it is, in full, as the board tells it (its lines under the heading). */
	static List<Component> lines(ServerPlayer player, Sect sect, Mission m) {
		List<Component> lines = new ArrayList<>();
		int distance = m.at == null ? 0 : (int) Math.sqrt(horizontal(sect.core, m.at));
		Component direction = m.at == null ? Component.empty() : Component.translatable(ModLang.talkDirection(TalkSystem.direction(sect.core, m.at)));
		switch (m.kind) {
			case HUNT -> {
				BeastKind kind = beast(m.what);
				String text = kind != null && kind.isChaos() ? "hunt_chaos" : m.count > 1 ? "hunt_pack" : "hunt";
				lines.add(Component.translatable(ModLang.missionText(text), beastName(m), rankName(m.rank), distance, direction, m.count));
			}
			case PURGE -> lines.add(Component.translatable(ModLang.missionText("purge." + m.what), quarryName(m), rankName(m.rank), distance, direction));
			case GATHER -> {
				lines.add(Component.translatable(ModLang.missionText("gather"), m.count, name(m)));
				Item item = item(m.what);
				if (item instanceof GinsengItem || item == ModItems.CULTIVATION_FRUIT) lines.add(Component.translatable(ModLang.MISSION_YEARS, m.quality));
				if (item == ModItems.DEMON_CORE) lines.add(Component.translatable(ModLang.MISSION_CORE_REALM, Realm.byIndex(m.quality).getDisplayName()));
			}
			case CRAFT -> {
				ArtifactKind kind = artifactKind(m.what);
				Component grade = kind != null ? ArtifactGrade.values()[m.quality].getDisplayName()
						: Component.translatable(ModLang.missionPillGrade(PillGrade.values()[m.quality].getId()));
				lines.add(Component.translatable(ModLang.missionText(kind != null ? "forge" : "brew"), m.count, name(m), grade));
			}
			case DELIVER -> lines.add(Component.translatable(ModLang.missionText("deliver"), m.what, distance, direction));
			case GUARD -> lines.add(Component.translatable(ModLang.missionText("guard"), WATCH_TICKS / 1200, waves(m.grade)));
		}
		lines.add(Component.translatable(ModLang.MISSION_GRADE_FOR, m.grade.displayName(), rankName(m.rank)));
		lines.add(Component.translatable(ModLang.MISSION_PAYS, m.reward, m.standing));
		long now = player.level().getGameTime();
		if (!m.isTaken()) {
			lines.add(Component.translatable(ModLang.MISSION_LASTS, days(sect, m)));
			return lines;
		}
		lines.add(Component.translatable(ModLang.MISSION_LEFT, clock(m.deadline - now)));
		if ((m.kind == Kind.HUNT || m.kind == Kind.PURGE) && (m.count > 1 || m.progress > 0)) lines.add(Component.translatable(ModLang.MISSION_SLAIN, m.progress, m.count));
		if (m.kind == Kind.GUARD && m.progress > 0) lines.add(Component.translatable(ModLang.MISSION_WATCHED, watch(m.progress), watch(WATCH_TICKS)));
		if (m.kind == Kind.GATHER || m.kind == Kind.CRAFT) lines.add(Component.translatable(ModLang.MISSION_HAVE, Math.min(carried(player, m), m.count), m.count));
		return lines;
	}

	/** The game days {@code m} stands once taken: its kind's, and a day more for every thousand blocks a letter goes. */
	static long days(Sect sect, Mission m) {
		return m.kind.days + (m.kind == Kind.DELIVER && m.at != null ? (long) Math.sqrt(m.at.distSqr(sect.core)) / 1000 : 0);
	}

	private static double horizontal(BlockPos a, BlockPos b) {
		double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	/** {@code ticks} of game time as days and hours: "1 day 4 hours", "6 hours", "less than an hour". */
	static Component clock(long ticks) {
		long days = Math.max(0, ticks) / HeavenRitual.DAY, hours = Math.max(0, ticks) % HeavenRitual.DAY / 1000;
		if (days > 0) return Component.translatable(ModLang.MISSION_TIME_DAYS, days, hours);
		if (hours > 0) return Component.translatable(ModLang.MISSION_TIME_HOURS, hours);
		return Component.translatable(ModLang.MISSION_TIME_SOON);
	}

	/** {@code ticks} of the watch as minutes and seconds: "2:05". */
	static String watch(int ticks) {
		int seconds = Math.max(0, ticks) / 20;
		return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
	}

	/** The waves a watch of {@code grade} sees: two for an Easy one, three else. */
	static int waves(Grade grade) {
		return grade == Grade.EASY ? 2 : 3;
	}

	/** What its taker did, as the sect talks of it: "drove off the Bloodfang Tiger". */
	static Component deed(Mission m) {
		return switch (m.kind) {
			case HUNT -> Component.translatable(ModLang.missionDeed("hunt"), beastName(m));
			case PURGE -> Component.translatable(ModLang.missionDeed("purge"), quarryName(m));
			case GATHER -> Component.translatable(ModLang.missionDeed("gather"), name(m));
			case CRAFT -> Component.translatable(ModLang.missionDeed(artifactKind(m.what) != null ? "forge" : "brew"), name(m));
			case DELIVER -> Component.translatable(ModLang.missionDeed("deliver"), m.what);
			case GUARD -> Component.translatable(ModLang.missionDeed("guard"));
		};
	}

	/** What its taker has taken on, as the sect talks of it: "the hunt for the Bloodfang Tiger". */
	static Component undertaking(Mission m) {
		return switch (m.kind) {
			case HUNT -> Component.translatable(ModLang.missionUndertaking("hunt"), beastName(m));
			case PURGE -> Component.translatable(ModLang.missionUndertaking("purge"), quarryName(m));
			case GATHER -> Component.translatable(ModLang.missionUndertaking("gather"), name(m));
			case CRAFT -> Component.translatable(ModLang.missionUndertaking(artifactKind(m.what) != null ? "forge" : "brew"), name(m));
			case DELIVER -> Component.translatable(ModLang.missionUndertaking("deliver"), m.what);
			case GUARD -> Component.translatable(ModLang.missionUndertaking("guard"));
		};
	}

	/** The thing to show it by on the board: the beast's egg, a sword for a purge, what is asked for, the letter, a bell for the watch. */
	static ItemStack icon(Mission m) {
		return switch (m.kind) {
			case HUNT -> {
				BeastKind kind = beast(m.what);
				yield kind == null ? new ItemStack(Items.BONE) : new ItemStack(ModItems.beastEgg(kind));
			}
			case PURGE -> new ItemStack(Items.IRON_SWORD);
			case GATHER, CRAFT -> thing(m);
			case DELIVER -> new ItemStack(ModItems.SEALED_LETTER);
			case GUARD -> new ItemStack(Items.BELL);
		};
	}

	// --- The board ---

	/** The sect {@code pos} is the board of: the one that set it up there, else the one whose grounds it stands on; null if none. */
	public static @Nullable Sect sectAt(ServerLevel level, BlockPos pos) {
		Sect grounds = null;
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.extinct || sect.dimension != level.dimension()) continue;
			if (pos.equals(sect.board)) return sect;
			if (grounds == null && sect.inTerritory(pos)) grounds = sect;
		}
		return grounds;
	}

	/** {@code player} opened the board at {@code pos} (MissionBoardBlock): its page (S2C mission_board). */
	public static void openBoard(ServerPlayer player, BlockPos pos) {
		Sect sect = sectAt(player.serverLevel(), pos);
		OPEN_AT.put(player.getUUID(), new Opened(player.level().dimension(), pos.immutable(), sect == null ? null : sect.id));
		send(player, sect, null);
	}

	/**
	 * S2C mission_board, the board's page: its heading; for one of no standing at it, why (no sect's, not of it, in another's body); for a
	 * disciple, their Contribution, all they have earned, their title, the missions they hold and the day's postings (each with its heading,
	 * lines, thing to show and what may be done with it now), and the exchange's wares (SectExchange); and {@code said}, the board's answer
	 * to what they last did there (or null).
	 */
	static void send(ServerPlayer player, @Nullable Sect sect, @Nullable Component said) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeComponent(sect == null ? Component.translatable(ModLang.MISSION_BOARD) : Component.translatable(ModLang.MISSION_BOARD_OF, sect.name));
		Sect.Disciple d = sect == null || Possession.inhabits(player.getUUID()) ? null : sect.disciples.get(player.getUUID());
		buf.writeBoolean(d != null);
		if (d == null) {
			buf.writeComponent(sect == null ? Component.translatable(ModLang.MISSION_NO_SECT)
					: Possession.inhabits(player.getUUID()) ? Component.translatable(ModLang.MISSION_IN_BODY)
					: Component.translatable(ModLang.MISSION_NOT_DISCIPLE, sect.name));
			ServerPlayNetworking.send(player, ModPackets.MISSION_BOARD, buf);
			return;
		}
		refresh(player, sect, d);
		buf.writeVarInt(d.contribution);
		buf.writeVarInt(d.earned);
		buf.writeComponent(SectMembership.titleOf(sect, player).getDisplayName(sect.isClan(), sect.isHoly(), sect.isImmortal()));
		buf.writeVarInt(d.taken.size());
		buf.writeVarInt(HELD);
		List<Mission> shown = new ArrayList<>(d.taken);
		shown.addAll(d.postings);
		buf.writeVarInt(shown.size());
		for (Mission m : shown) {
			buf.writeVarInt(m.id);
			buf.writeBoolean(m.isTaken());
			buf.writeVarInt(m.kind.ordinal());
			buf.writeVarInt(m.grade.ordinal());
			buf.writeVarInt(m.reward);
			buf.writeComponent(title(m));
			List<Component> lines = lines(player, sect, m);
			buf.writeVarInt(lines.size());
			for (Component line : lines) buf.writeComponent(line);
			buf.writeItem(icon(m));
			buf.writeVarInt(actions(player, d, m));
		}
		List<SectExchange.Ware> wares = SectExchange.wares(sect, player);
		buf.writeVarInt(wares.size());
		for (int i = 0; i < wares.size(); i++) SectExchange.write(buf, player, sect, d, wares.get(i), i);
		buf.writeBoolean(said != null);
		if (said != null) buf.writeComponent(said);
		ServerPlayNetworking.send(player, ModPackets.MISSION_BOARD, buf);
	}

	/** What may be done with {@code m} at the board now: take a posting (room for it); abandon one taken, hand one in, ask a new slip for it. */
	static int actions(ServerPlayer player, Sect.Disciple d, Mission m) {
		if (!m.isTaken()) return d.taken.size() < HELD ? CAN_TAKE : 0;
		int can = CAN_ABANDON;
		if ((m.kind == Kind.GATHER || m.kind == Kind.CRAFT) && carried(player, m) >= m.count) can |= CAN_HAND_IN;
		if (!hasSlip(player, d, m)) can |= CAN_SLIP;
		return can;
	}

	/**
	 * C2S mission_act: {@code act} on mission (or ware) {@code id}, at the board {@code player} opened last, standing by it, as its disciple;
	 * then the page again, with what came of it.
	 */
	public static void act(ServerPlayer player, int act, int id) {
		Opened opened = OPEN_AT.get(player.getUUID());
		if (opened == null || player.level().dimension() != opened.dimension() || opened.sect() == null
				|| player.distanceToSqr(Vec3.atCenterOf(opened.pos())) > BOARD_REACH * BOARD_REACH
				|| !player.level().getBlockState(opened.pos()).is(ModBlocks.MISSION_BOARD)) {
			return;
		}
		Sect sect = SectManager.get(player.server, opened.sect());
		if (sect == null || sect.extinct || Possession.inhabits(player.getUUID())) return;
		Sect.Disciple d = sect.disciples.get(player.getUUID());
		if (d == null) return;
		Component said = switch (act) {
			case TAKE -> take(player, sect, d, id);
			case ABANDON -> abandon(player, sect, d, id);
			case HAND_IN -> handIn(player, sect, d, id);
			case SLIP -> newSlip(player, sect, d, id);
			case BUY -> SectExchange.buy(player, sect, d, id);
			default -> null;
		};
		send(player, sect, said);
	}

	static @Nullable Mission find(List<Mission> missions, int id) {
		for (Mission m : missions) if (m.id == id) return m;
		return null;
	}

	/** {@code d} takes posting {@code id}: theirs until it lapses (its kind's days), and its slip (a letter, for a delivery) in their pack. */
	static @Nullable Component take(ServerPlayer player, Sect sect, Sect.Disciple d, int id) {
		Mission m = find(d.postings, id);
		if (m == null) return null;
		if (d.taken.size() >= HELD) return Component.translatable(ModLang.MSG_MISSION_HELD_FULL, HELD).withStyle(ChatFormatting.RED);
		d.postings.remove(m);
		m.deadline = player.level().getGameTime() + days(sect, m) * HeavenRitual.DAY;
		d.taken.add(m);
		giveSlip(player, sect, m);
		SectManager.dirty(player.server);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
		return Component.translatable(ModLang.MSG_MISSION_TAKEN, title(m)).withStyle(ChatFormatting.GOLD);
	}

	/** {@code d} gives up mission {@code id}: gone, its quarry with it; the sect holds nothing against them. */
	static @Nullable Component abandon(ServerPlayer player, Sect sect, Sect.Disciple d, int id) {
		Mission m = find(d.taken, id);
		if (m == null) return null;
		d.taken.remove(m);
		discardFoes(player.server, m);
		SectManager.dirty(player.server);
		return Component.translatable(ModLang.MSG_MISSION_ABANDONED, title(m)).withStyle(ChatFormatting.GRAY);
	}

	/** {@code d} hands in what gathering or brew or forging {@code id} asked for, from their pack: seen through. */
	static @Nullable Component handIn(ServerPlayer player, Sect sect, Sect.Disciple d, int id) {
		Mission m = find(d.taken, id);
		if (m == null || m.kind != Kind.GATHER && m.kind != Kind.CRAFT) return null;
		int have = carried(player, m);
		if (have < m.count) return Component.translatable(ModLang.MSG_MISSION_BRING, m.count - have, name(m)).withStyle(ChatFormatting.RED);
		int left = m.count;
		for (List<ItemStack> stacks : List.of(player.getInventory().items, player.getInventory().offhand)) {
			for (ItemStack stack : stacks) {
				if (left <= 0) break;
				if (!matches(stack, m)) continue;
				int used = Math.min(left, stack.getCount());
				stack.shrink(used);
				left -= used;
			}
		}
		return complete(player.server, sect, d, m);
	}

	/** {@code d} lost the slip (or the letter) of mission {@code id}: another. */
	static @Nullable Component newSlip(ServerPlayer player, Sect sect, Sect.Disciple d, int id) {
		Mission m = find(d.taken, id);
		if (m == null) return null;
		if (hasSlip(player, d, m)) return Component.translatable(ModLang.MSG_MISSION_HAS_SLIP).withStyle(ChatFormatting.GRAY);
		giveSlip(player, sect, m);
		return Component.translatable(ModLang.MSG_MISSION_NEW_SLIP, title(m)).withStyle(ChatFormatting.GOLD);
	}

	private static void giveSlip(ServerPlayer player, Sect sect, Mission m) {
		ItemStack slip = MissionSlipItem.make(m.kind == Kind.DELIVER ? ModItems.SEALED_LETTER : ModItems.MISSION_SLIP, sect, player, m);
		if (!player.getInventory().add(slip)) player.drop(slip, false);
	}

	private static boolean hasSlip(ServerPlayer player, Sect.Disciple d, Mission m) {
		for (List<ItemStack> stacks : List.of(player.getInventory().items, player.getInventory().offhand)) {
			for (ItemStack stack : stacks) {
				if (stack.getItem() instanceof MissionSlipItem && MissionSlipItem.isOf(stack, d.id, m.id)) return true;
			}
		}
		return false;
	}

	/**
	 * Mission {@code m} of {@code d}'s seen through: its Contribution theirs, the sect's regard raised, its quarry gone; the sect talks of it
	 * for a day; told to them (if they are here), with the milestones it makes. @return its word for the board
	 */
	static Component complete(MinecraftServer server, Sect sect, Sect.Disciple d, Mission m) {
		d.taken.remove(m);
		discardFoes(server, m);
		d.contribution += m.reward;
		d.earned += m.reward;
		d.kindsDone |= 1 << m.kind.ordinal();
		sect.changeStanding(d.id, m.standing);
		SectManager.dirty(server);
		DEEDS.put(d.id, new Deed(sect.id, deed(m), server.overworld().getGameTime()));
		Component said = Component.translatable(ModLang.MSG_MISSION_DONE, title(m), m.reward, d.contribution).withStyle(ChatFormatting.GOLD);
		ServerPlayer player = server.getPlayerList().getPlayer(d.id);
		if (player == null) return said;
		player.sendSystemMessage(said);
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.3f);
		Milestones.award(player, Milestones.MISSION_DONE);
		if (d.earned >= PILLAR) Milestones.award(player, Milestones.MISSION_PILLAR);
		if (d.kindsDone == (1 << Kind.values().length) - 1) Milestones.award(player, Milestones.MISSION_EVERY_KIND);
		return said;
	}

	// --- In the field ---

	/** Every second, those online with missions out in the field; every five, the missions lapsed, and the boards set up. */
	static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		try {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) field(player);
			if (server.getTickCount() % 100 == 0) {
				lapse(server);
				layBoards(server);
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("The sect mission board's tick failed", e);
		}
	}

	/** A second of {@code player}'s missions out in the field: their quarry, their gate watch. Package-private for the game tests. */
	static void field(ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) return;
		Sect sect = SectMembership.sectOf(player.server, player.getUUID());
		Sect.Disciple d = sect == null ? null : sect.disciples.get(player.getUUID());
		if (d == null || d.taken.isEmpty()) return;
		for (Mission m : List.copyOf(d.taken)) {
			if (m.kind == Kind.HUNT || m.kind == Kind.PURGE) quarry(player, sect, d, m);
			else if (m.kind == Kind.GUARD) watch(player, sect, d, m);
		}
	}

	/**
	 * A hunt's or a purge's quarry: out of the land, glowing, when its taker draws within {@link #COMES_WITHIN} of its ground (the land there
	 * running); out, kept after them.
	 */
	private static void quarry(ServerPlayer player, Sect sect, Sect.Disciple d, Mission m) {
		if (m.at == null || m.dimension == null || player.level().dimension() != m.dimension) return;
		ServerLevel level = player.serverLevel();
		for (UUID id : m.foes) {
			if (level.getEntity(id) instanceof Mob mob && mob.getTarget() == null && mob.distanceToSqr(player) <= 64 * 64) mob.setTarget(player);
		}
		if (m.away > 0) {
			m.away = Math.max(0, m.away - 20);
			return;
		}
		if (!m.foes.isEmpty() || m.progress >= m.count) return;
		BlockPos at = m.at;
		if (horizontal(player.blockPosition(), at) > COMES_WITHIN * COMES_WITHIN || !level.isPositionEntityTicking(at)) return;
		RandomSource random = level.getRandom();
		BeastKind kind = m.kind == Kind.HUNT ? beast(m.what) : null;
		LivingEntity first = null;
		for (int i = m.progress; i < m.count; i++) {
			LivingEntity foe;
			if (m.kind == Kind.PURGE) {
				BlockPos ground = ground(level, at, random, 3);
				foe = ground == null ? null : cultivator(level, sect, m.rank, m.target, ground, random);
			} else if (kind != null && kind.isChaos()) {
				// A fiend comes out of the storm in the air, about its taker's height (the Frontier's land is broken: no ground to it).
				BlockPos air = BlockPos.containing(at.getX() + random.nextInt(17) - 8, player.getY() + 4 + random.nextInt(6), at.getZ() + random.nextInt(17) - 8);
				foe = beast(level, kind, m.rank, air);
			} else {
				BlockPos ground = ground(level, at, random, 3);
				foe = ground == null || kind == null ? null : beast(level, kind, m.rank, ground);
			}
			if (foe == null) continue;
			out(level, foe, sect, d, m, player);
			if (first == null) first = foe;
		}
		// Named as its posting names it (a purge's by its own name: a cultivator's display name is only "Cultivator").
		if (first != null) {
			Component name = m.kind == Kind.PURGE ? quarryName(m) : first.getDisplayName();
			player.sendSystemMessage(Component.translatable(ModLang.MSG_MISSION_QUARRY, name).withStyle(ChatFormatting.RED));
		}
		SectManager.dirty(player.server);
	}

	/**
	 * The gate watch: kept while its taker stands within {@link #WATCH_REACH} of the gate, a second at a time, its waves sent at their marks;
	 * seen through at {@link #WATCH_TICKS}. Begun, and left for {@link #WATCH_AWAY}, it is lost.
	 */
	private static void watch(ServerPlayer player, Sect sect, Sect.Disciple d, Mission m) {
		if (m.at == null) return;
		boolean there = player.level().dimension() == sect.dimension && horizontal(player.blockPosition(), m.at) <= WATCH_REACH * WATCH_REACH
				&& Math.abs(player.getY() - m.at.getY()) <= WATCH_REACH;
		if (there) {
			if (m.progress == 0) player.sendSystemMessage(Component.translatable(ModLang.MSG_WATCH_BEGINS, WATCH_TICKS / 1200).withStyle(ChatFormatting.GOLD));
			m.progress += 20;
			m.away = 0;
			boolean waved = false;
			while (m.waves < waves(m.grade) && m.progress >= WAVES_AT[m.waves]) {
				wave(player, sect, d, m);
				m.waves++;
				waved = true;
			}
			SectManager.dirty(player.server);
			if (m.progress >= WATCH_TICKS) {
				complete(player.server, sect, d, m);
				return;
			}
			// Over the hotbar: a wave coming, or every ten seconds the time left (never the one over the other).
			if (waved) player.displayClientMessage(Component.translatable(ModLang.MSG_WATCH_WAVE).withStyle(ChatFormatting.RED), true);
			else if (m.progress % 200 == 0) player.displayClientMessage(Component.translatable(ModLang.MSG_WATCH_LEFT, watch(WATCH_TICKS - m.progress)), true);
		} else if (m.progress > 0) {
			m.away += 20;
			SectManager.dirty(player.server);
			if (m.away >= WATCH_AWAY) {
				d.taken.remove(m);
				discardFoes(player.server, m);
				sect.changeStanding(d.id, -1);
				player.sendSystemMessage(Component.translatable(ModLang.MSG_WATCH_FAILED).withStyle(ChatFormatting.RED));
				return;
			}
			if (m.away == 20 || m.away % 100 == 0) {
				player.displayClientMessage(Component.translatable(ModLang.MSG_WATCH_AWAY, (WATCH_AWAY - m.away) / 20).withStyle(ChatFormatting.RED), true);
			}
		}
	}

	/**
	 * A wave at the gate: one foe for an Easy watch, two for a Fitting, three for a Hard, a stage below its rank, coming in from twenty
	 * blocks out past the gate (away from the sect): wild beasts while any are that strong, else cultivators of the path that preys on it.
	 */
	private static void wave(ServerPlayer player, Sect sect, Sect.Disciple d, Mission m) {
		ServerLevel level = player.serverLevel();
		RandomSource random = level.getRandom();
		int rank = Math.max(0, m.rank - 1);
		BeastKind kind = quarryFor(level, rank, random);
		if (kind != null && kind.isChaos()) kind = null; // no fiend so far from the Frontier: cultivators instead
		double dx = m.at.getX() - sect.core.getX(), dz = m.at.getZ() - sect.core.getZ();
		double length = Math.max(1, Math.hypot(dx, dz));
		dx /= length;
		dz /= length;
		for (int i = 0; i <= m.grade.ordinal(); i++) {
			double side = (random.nextDouble() - 0.5) * 16, out = 20 + random.nextDouble() * 8;
			BlockPos column = BlockPos.containing(m.at.getX() + dx * out - dz * side, m.at.getY(), m.at.getZ() + dz * out + dx * side);
			BlockPos ground = ground(level, column, random, 2);
			if (ground == null) continue;
			LivingEntity foe = kind != null ? beast(level, kind, rank, ground) : cultivator(level, sect, rank, null, ground, random);
			if (foe != null) out(level, foe, sect, d, m, player);
		}
	}

	/** A beast of {@code kind} of {@code rank}, made ready at {@code at}; not yet in the world. */
	private static @Nullable DemonBeast beast(ServerLevel level, BeastKind kind, int rank, BlockPos at) {
		DemonBeast beast = ModEntities.beast(kind).create(level);
		if (beast == null) return null;
		beast.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		beast.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		beast.setRank(rank);
		return beast;
	}

	/**
	 * A cultivator of the path that preys on {@code sect} ({@link #foePath}), of {@code rank} (whatever room its world has for those past Four
	 * Axis: HighRealms), {@code id} its own if given (a purge's quarry, named on its posting); made ready at {@code at}, not yet in the world.
	 */
	private static @Nullable CultivatorNpc cultivator(ServerLevel level, Sect sect, int rank, @Nullable UUID id, BlockPos at, RandomSource random) {
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return null;
		if (id != null) npc.setUUID(id);
		// A quarry before it is anything else: becoming a rogue decides its course at once, and a quarry's is to hunt (a Heavenly Being of
		// the lower realms would otherwise go off into seclusion: CultivatorNpc#decideRogueCourse).
		npc.addTag(FOE_TAG);
		boolean righteous = foePath(sect) == Alignment.Faction.RIGHTEOUS;
		int alignment = righteous ? Alignment.RIGHTEOUS_ABOVE + 10 + random.nextInt(Alignment.MAX - Alignment.RIGHTEOUS_ABOVE - 10 + 1)
				: Alignment.DEVILISH_BELOW - 10 - random.nextInt(Alignment.DEVILISH_BELOW - 10 - Alignment.MIN + 1);
		npc.becomeRogue(rank, alignment, random);
		npc.setRank(rank, true);
		npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
		return npc;
	}

	/** {@code foe} put in the world as {@code m}'s, {@code d}'s quarry: tagged as theirs, glowing, kept in the world, after them. */
	private static void out(ServerLevel level, LivingEntity foe, Sect sect, Sect.Disciple d, Mission m, ServerPlayer taker) {
		foe.addTag(FOE_TAG);
		foe.addTag(FOE_TAKER + d.id);
		foe.addTag(FOE_MISSION + sect.id + "/" + m.id);
		foe.setGlowingTag(true);
		if (foe instanceof Mob mob) {
			mob.setPersistenceRequired();
			mob.setTarget(taker);
		}
		if (foe instanceof CultivatorNpc npc) npc.markAggressed(taker); // it came for them: slaying it is no murder
		// On its mission's list before it is in the world: coming in, it is asked whether it is still wanted (#register: one not is sent away).
		m.foes.add(foe.getUUID());
		if (!level.addFreshEntity(foe)) m.foes.remove(foe.getUUID());
	}

	/** Open ground {@code spread} or so round {@code at} the world runs (not over the void), out of the water where there is any; or null. */
	private static @Nullable BlockPos ground(ServerLevel level, BlockPos at, RandomSource random, int spread) {
		BlockPos first = null;
		for (int tries = 0; tries < 8; tries++) {
			BlockPos column = at.offset(random.nextInt(spread * 2 + 1) - spread, 0, random.nextInt(spread * 2 + 1) - spread);
			if (!level.isPositionEntityTicking(column)) continue;
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (pos.getY() <= level.getMinBuildHeight() + 1) continue;
			if (first == null) first = pos;
			if (level.getBlockState(pos.below()).getFluidState().isEmpty()) return pos;
		}
		return first;
	}

	/** {@code m}'s quarry (or the watch's foes) still in the world, gone from it. */
	static void discardFoes(MinecraftServer server, Mission m) {
		ServerLevel level = m.dimension == null ? null : server.getLevel(m.dimension);
		if (level != null) {
			for (UUID id : m.foes) {
				Entity foe = level.getEntity(id);
				if (foe != null) foe.discard();
			}
		}
		m.foes.clear();
	}

	/** Every five seconds: missions past their time lapse (their takers online or not), and the sect thinks a little less of them. */
	static void lapse(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (Sect sect : SectData.get(server).all()) {
			for (Sect.Disciple d : sect.disciples.values()) {
				for (Iterator<Mission> it = d.taken.iterator(); it.hasNext(); ) {
					Mission m = it.next();
					if (now <= m.deadline) continue;
					it.remove();
					discardFoes(server, m);
					sect.changeStanding(d.id, -1);
					SectManager.dirty(server);
					ServerPlayer player = server.getPlayerList().getPlayer(d.id);
					if (player != null) player.sendSystemMessage(Component.translatable(ModLang.MSG_MISSION_LAPSED, title(m)).withStyle(ChatFormatting.GRAY));
				}
			}
		}
	}

	// --- The quarry ---

	/** Whether {@code entity} is a mission's quarry (or a watch's foe): it goes for the one who took the mission on, and only them. */
	public static boolean isFoe(Entity entity) {
		return entity.getTags().contains(FOE_TAG);
	}

	/** Who took on the mission {@code foe} came for, or null. */
	public static @Nullable UUID takerOf(Entity foe) {
		for (String tag : foe.getTags()) if (tag.startsWith(FOE_TAKER)) return parse(tag.substring(FOE_TAKER.length()));
		return null;
	}

	private static @Nullable UUID parse(String text) {
		try {
			return UUID.fromString(text);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** The mission {@code foe} came for, as it stands now (taken, not yet seen through), or null. */
	static @Nullable Found found(MinecraftServer server, Entity foe) {
		UUID taker = takerOf(foe);
		String mission = null;
		for (String tag : foe.getTags()) if (tag.startsWith(FOE_MISSION)) mission = tag.substring(FOE_MISSION.length());
		if (taker == null || mission == null || mission.indexOf('/') < 0) return null;
		UUID sectId = parse(mission.substring(0, mission.indexOf('/')));
		int id;
		try {
			id = Integer.parseInt(mission.substring(mission.indexOf('/') + 1));
		} catch (NumberFormatException e) {
			return null;
		}
		Sect sect = sectId == null ? null : SectManager.get(server, sectId);
		Sect.Disciple d = sect == null ? null : sect.disciples.get(taker);
		Mission m = d == null ? null : find(d.taken, id);
		return m == null ? null : new Found(sect, d, m);
	}

	/** Whether {@code foe} is still wanted: its mission taken still, and it one of its quarry out. */
	private static boolean wanted(MinecraftServer server, Entity foe) {
		Found f = found(server, foe);
		return f != null && f.mission().foes.contains(foe.getUUID());
	}

	/**
	 * A quarry has died: struck from its mission's list; by its taker's hand, one more slain (all of it, and the hunt or purge is seen through).
	 * Slain by any other, it comes out again a while later ({@link #QUARRY_AGAIN}).
	 */
	static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level) || !isFoe(entity)) return;
		Found f = found(level.getServer(), entity);
		if (f == null) return;
		Mission m = f.mission();
		m.foes.remove(entity.getUUID());
		SectManager.dirty(level.getServer());
		if (m.kind != Kind.HUNT && m.kind != Kind.PURGE) return;
		if (byTaker(entity, source, f.disciple().id)) m.progress++;
		else if (m.foes.isEmpty()) m.away = QUARRY_AGAIN;
		if (m.progress >= m.count) complete(level.getServer(), f.sect(), f.disciple(), m);
	}

	/** Whether {@code taker} slew it: their blow (or arrow, or spell), or the last of theirs it took. */
	private static boolean byTaker(LivingEntity dead, DamageSource source, UUID taker) {
		Entity killer = source == null ? null : source.getEntity();
		if (!(killer instanceof Player)) killer = dead.getKillCredit();
		return killer instanceof Player player && player.getUUID().equals(taker);
	}

	/**
	 * Whether {@code dead} leaves its spoils: anything but a mission's quarry does; a quarry only when its taker slew it. Slain by any other, it
	 * comes out again (as a wish's threat does: Wishes#spoilsDrop), and its spoils with it would come without end. ThreatLootMixin asks it.
	 */
	public static boolean spoilsDrop(LivingEntity dead, DamageSource source) {
		if (!isFoe(dead)) return true;
		UUID taker = takerOf(dead);
		return taker != null && byTaker(dead, source, taker);
	}

	// --- The slip, and the letter ---

	/** The mission {@code stack}'s slip (or letter) is of, as it stands now (taken, not yet over), or null. */
	public static @Nullable Found ofSlip(MinecraftServer server, ItemStack stack) {
		UUID sectId = MissionSlipItem.sect(stack), owner = MissionSlipItem.owner(stack);
		if (sectId == null || owner == null) return null;
		Sect sect = SectManager.get(server, sectId);
		Sect.Disciple d = sect == null || sect.extinct ? null : sect.disciples.get(owner);
		Mission m = d == null ? null : find(d.taken, MissionSlipItem.mission(stack));
		return m == null ? null : new Found(sect, d, m);
	}

	/** What a slip held shows over the hotbar: the way to its quarry (to it, once out), its letter's sect, the gate; the watch's time left. */
	public static Component pointer(ServerPlayer player, Found f) {
		Mission m = f.mission();
		Component title = title(m);
		if (m.kind == Kind.GATHER || m.kind == Kind.CRAFT) return Component.translatable(ModLang.MSG_SLIP_HAND_IN, title);
		if (m.kind == Kind.GUARD && m.progress > 0) return Component.translatable(ModLang.MSG_WATCH_LEFT, watch(WATCH_TICKS - m.progress));
		if (m.at == null) return title;
		if (m.dimension != null && player.level().dimension() != m.dimension) return Component.translatable(ModLang.MSG_SLIP_ELSEWHERE, title);
		BlockPos to = m.at;
		for (UUID id : m.foes) {
			Entity foe = player.serverLevel().getEntity(id);
			if (foe != null) {
				to = foe.blockPosition();
				break;
			}
		}
		int distance = (int) Math.sqrt(horizontal(player.blockPosition(), to));
		if (distance < 4) return Component.translatable(ModLang.MSG_SLIP_HERE, title);
		return Component.translatable(ModLang.MSG_SLIP_POINT, title, distance, Component.translatable(ModLang.talkDirection(TalkSystem.direction(player.blockPosition(), to))));
	}

	/** The sealed letter {@code player} carries for {@code npc}'s sect (theirs, its mission still out), or null (TalkSystem: the Deliver topic). */
	public static @Nullable ItemStack letterFor(ServerPlayer player, CultivatorNpc npc) {
		if (!npc.isSectMember() || npc.isGhost() || npc.getSectId() == null || Possession.inhabits(player.getUUID())) return null;
		for (List<ItemStack> stacks : List.of(player.getInventory().items, player.getInventory().offhand)) {
			for (ItemStack stack : stacks) {
				if (!stack.is(ModItems.SEALED_LETTER)) continue;
				Found f = ofSlip(player.server, stack);
				if (f != null && f.disciple().id.equals(player.getUUID()) && f.mission().kind == Kind.DELIVER && npc.getSectId().equals(f.mission().target)) {
					return stack;
				}
			}
		}
		return null;
	}

	/** {@code player} hands {@code npc} the letter for its sect: delivered (its sect thinks the better of the courier). @return what it says */
	public static Component deliver(ServerPlayer player, CultivatorNpc npc) {
		ItemStack letter = letterFor(player, npc);
		Found f = letter == null ? null : ofSlip(player.server, letter);
		if (f == null) return Component.translatable(ModLang.talkLine("deliver.none"));
		letter.shrink(1);
		Sect to = npc.sect();
		if (to != null) to.changeStanding(player.getUUID(), 2);
		complete(player.server, f.sect(), f.disciple(), f.mission());
		return Component.translatable(ModLang.talkLine("deliver", npc.getFaction().getId()), f.sect().name);
	}

	// --- The sect's talk of its missions (NpcLife) ---

	/**
	 * What {@code npc} might say of the board to {@code player}, one of its own sect's disciples, passing ({@code address} and {@code name}
	 * as NpcLife calls them): their deed of the day, the gate watch they have taken and not begun, or the day's postings they haven't seen;
	 * else null.
	 */
	public static @Nullable Component remark(CultivatorNpc npc, ServerPlayer player, Component address, String name) {
		Sect sect = npc.sect();
		Sect.Disciple d = sect == null ? null : sect.disciples.get(player.getUUID());
		if (d == null) return null;
		long now = npc.level().getGameTime();
		Deed deed = DEEDS.get(player.getUUID());
		if (deed != null && deed.sect().equals(sect.id) && now - deed.at() < TALKED_OF) return NpcLines.line("pass.mission.done", npc, address, name, deed.what());
		for (Mission m : d.taken) if (m.kind == Kind.GUARD && m.progress == 0) return NpcLines.line("pass.mission.watch", npc, address, name);
		if (d.postedDay != Regard.day(now) && sect.board != null) return NpcLines.line("pass.mission.new", npc, address, name);
		return null;
	}

	/**
	 * Something of {@code sect}'s missions for two of its people to talk over (NpcLife: chat.mission): a deed one of its disciples did today,
	 * or a mission one has taken on; null if there is none.
	 */
	public static @Nullable Component chatSubject(Sect sect, long now, RandomSource random) {
		List<Component> said = new ArrayList<>();
		for (Sect.Disciple d : sect.disciples.values()) {
			Deed deed = DEEDS.get(d.id);
			if (deed != null && deed.sect().equals(sect.id) && now - deed.at() < TALKED_OF) said.add(Component.translatable(ModLang.MISSION_SAID_DONE, d.name, deed.what()));
			for (Mission m : d.taken) said.add(Component.translatable(ModLang.MISSION_SAID_TAKEN, d.name, undertaking(m)));
		}
		return said.isEmpty() ? null : said.get(random.nextInt(said.size()));
	}

	// --- Setting up the boards ---

	/**
	 * Every five seconds: each sect a player has come near (within {@link #LAY_NEAR} of its core, the land there running) has its board on
	 * its plaza; one found gone is set up again the day after. Only a sect's grounds as laid out (its core a Formation Core, a gate): never a
	 * test's.
	 */
	static void layBoards(MinecraftServer server) {
		long today = Regard.day(server.overworld().getGameTime());
		for (Sect sect : SectData.get(server).all()) {
			if (sect.extinct || sect.posts(Sect.Post.GATE).isEmpty()) continue;
			ServerLevel level = server.getLevel(sect.dimension);
			if (level == null || !level.isPositionEntityTicking(sect.core) || !level.getBlockState(sect.core).is(ModBlocks.FORMATION_CORE)) continue;
			if (level.getNearestPlayer(sect.core.getX() + 0.5, sect.core.getY(), sect.core.getZ() + 0.5, LAY_NEAR, false) == null) continue;
			if (sect.board != null) {
				if (!level.isLoaded(sect.board)) continue;
				if (level.getBlockState(sect.board).is(ModBlocks.MISSION_BOARD)) {
					if (sect.boardGoneDay >= 0) {
						sect.boardGoneDay = -1;
						SectManager.dirty(server);
					}
					continue;
				}
				if (sect.boardGoneDay < 0) {
					sect.boardGoneDay = today;
					SectManager.dirty(server);
					continue;
				}
				if (today <= sect.boardGoneDay) continue;
			}
			lay(level, sect);
		}
	}

	/**
	 * {@code sect}'s board set up on its plaza: {@link #BOARD_FROM} to {@link #BOARD_TO} blocks out from its core, toward its gate as near as
	 * may be, on firm ground with room above it, clear of everyone's posts, its face to the gate. @return where, or null if no room was found
	 */
	static @Nullable BlockPos lay(ServerLevel level, Sect sect) {
		BlockPos core = sect.core;
		BlockPos gate = sect.posts(Sect.Post.GATE).isEmpty() ? null : sect.post(Sect.Post.GATE, 0);
		double toGate = gate == null ? 0 : Math.atan2(gate.getZ() - core.getZ(), gate.getX() - core.getX());
		List<BlockPos> posts = new ArrayList<>();
		for (List<BlockPos> list : sect.posts.values()) posts.addAll(list);
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		for (int r = BOARD_FROM; r <= BOARD_TO; r++) {
			for (int a = 0; a < 16; a++) {
				double turn = Math.PI * 2 * a / 16;
				double angle = toGate + (a % 2 == 0 ? turn / 2 : -turn / 2);
				int x = core.getX() + (int) Math.round(Math.cos(angle) * r), z = core.getZ() + (int) Math.round(Math.sin(angle) * r);
				for (int dy = 2; dy >= -3; dy--) {
					BlockPos pos = new BlockPos(x, core.getY() + dy, z);
					if (!roomFor(level, pos)) continue;
					if (!clearOfPosts(pos, posts)) break;
					double score = Math.abs(Mth.wrapDegrees(Math.toDegrees(angle - toGate))) / 20 + r;
					if (score < bestScore) {
						bestScore = score;
						best = pos;
					}
					break;
				}
			}
		}
		if (best == null) return null;
		Direction facing = gate == null ? Direction.SOUTH : Direction.getNearest(gate.getX() - core.getX(), 0, gate.getZ() - core.getZ());
		if (facing.getAxis().isVertical()) facing = Direction.SOUTH;
		level.setBlock(best, ModBlocks.MISSION_BOARD.defaultBlockState().setValue(MissionBoardBlock.FACING, facing), Block.UPDATE_ALL);
		sect.board = best;
		sect.boardGoneDay = -1;
		SectManager.dirty(level.getServer());
		DefyingTheHeavens.LOGGER.info("{} set up its mission board at {}", sect.name, best.toShortString());
		return best;
	}

	/**
	 * Whether the board would stand at {@code pos}: empty and dry, with room above it, on firm ground. Only land already loaded is looked at
	 * (reading a block of land not loaded loads it, and keeps it so).
	 */
	private static boolean roomFor(ServerLevel level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && level.getFluidState(pos).isEmpty()
				&& level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	/** Whether {@code pos} keeps clear of every post of its sect (two blocks), so none of its people is ever kept from theirs by it. */
	private static boolean clearOfPosts(BlockPos pos, List<BlockPos> posts) {
		for (BlockPos post : posts) if (post.distSqr(pos) <= 4) return false;
		return true;
	}

	private SectMissions() {}
}
