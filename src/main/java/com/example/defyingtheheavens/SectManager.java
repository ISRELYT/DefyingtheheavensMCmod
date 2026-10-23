package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Runs the sects (see {@link Sect}): founds them when their Formation Core first ticks, keeps titles in step with
 * cultivation, replaces those lost to anything but a player, and calls members to arms.
 * <p>
 * <b>Who answers a call.</b> When a member is attacked anywhere in the sect's territory by a player or a cultivator, or
 * someone breaks through the barrier, the whole sect answers: every member in the territory, the elders and the Sect Master too, goes for the
 * aggressor. Otherwise (a strike on the barrier that doesn't break it, trespass, an attack on a member far outside the
 * territory) Inner and Outer Disciples answer anything near them (or anywhere on the grounds when the treasury or the core is
 * breached); Elders and Grand Elders keep to their posts unless a lower-ranked member is attacked near them, the treasury or
 * core is breached, or the barrier is down; the Sect Master stays on the dais until the barrier is shattered, its core
 * destroyed, or an Elder (or higher) has just fallen. Anyone attacked fights back.
 */
public final class SectManager {
	/** For this long after an Elder falls, the Sect Master takes the field. */
	private static final long MASTER_WRATH = 6000;
	private static final double DISCIPLE_REACH = 40;
	private static final double ELDER_REACH = 24;

	public enum Alarm { MEMBER_ATTACKED, BARRIER_ASSAILED, BARRIER_BROKEN, TRESPASS, CRITICAL_BREACH, CORE_DESTROYED }

	public static Sect get(MinecraftServer server, UUID id) {
		return id == null ? null : SectData.get(server).get(id);
	}

	static void dirty(MinecraftServer server) {
		SectData.get(server).setDirty();
	}

	/** Changes what a whole sect thinks of a player (see {@link Regard}). */
	public static void changeStanding(MinecraftServer server, UUID sectId, UUID player, int delta) {
		Sect sect = get(server, sectId);
		if (sect == null || delta == 0) return;
		sect.changeStanding(player, delta);
		dirty(server);
	}

	// --- Founding ---

	/**
	 * A core placed by worldgen ticks for the first time: the sect in its {@code blueprint} (see SectStructure) is founded,
	 * its members are born at their dwellings, and the barrier goes up.
	 */
	public static void found(ServerLevel level, FormationCoreBlockEntity core, CompoundTag blueprint) {
		MinecraftServer server = level.getServer();
		Sect sect = new Sect(UUID.randomUUID());
		RandomSource random = RandomSource.create(blueprint.getLong("Seed") ^ core.getBlockPos().asLong());
		sect.alignment = Alignment.clamp(blueprint.getInt("Alignment"));
		sect.name = SectNames.generate(random, Alignment.factionOf(sect.alignment));
		sect.dimension = level.dimension();
		sect.core = core.getBlockPos();
		sect.barrierRadius = Math.max(Formations.MIN_RADIUS, Math.min(Formations.MAX_RADIUS, blueprint.getInt("Radius")));
		sect.capacity = Math.max(1, blueprint.getInt("Capacity"));
		sect.bounds = box(blueprint.getIntArray("Bounds"), sect.core);
		sect.treasury = box(blueprint.getIntArray("Treasury"), sect.core);
		CompoundTag posts = blueprint.getCompound("Posts");
		for (Sect.Post post : Sect.Post.values()) {
			for (long pos : posts.getLongArray(post.name())) sect.posts(post).add(BlockPos.of(pos));
		}

		boolean upper = ModDimensions.isUpperRealm(level.dimension());
		int[] ranks = foundingRanks(random, sect.capacity, upper);
		// Strongest first after the master, so the first homes (the elders' houses, see SectStructure) go to the elders.
		java.util.Arrays.sort(ranks, 1, ranks.length);
		for (int i = 1, j = ranks.length - 1; i < j; i++, j--) {
			int t = ranks[i];
			ranks[i] = ranks[j];
			ranks[j] = t;
		}
		List<CultivatorNpc> born = new ArrayList<>();
		for (int i = 0; i < ranks.length; i++) {
			CultivatorNpc npc = spawnMember(level, sect, ranks[i], i == 0 ? -1 : i - 1, random);
			if (npc != null) born.add(npc);
		}
		SectData.get(server).put(sect);
		sect.recalculateTitles();
		for (CultivatorNpc npc : born) {
			Sect.Member member = sect.members.get(npc.getUUID());
			npc.setTitle(member.title);
			npc.equip(random);
			level.addFreshEntity(npc);
		}
		Sect.Member master = sect.master();
		core.bindToSect(sect.id, sect.name, sect.barrierRadius, master == null ? 0 : master.rank);
		DefyingTheHeavens.LOGGER.info("Founded the {} ({}, {} members) at {}", sect.name, Alignment.factionOf(sect.alignment).getId(),
				sect.members.size(), sect.core.toShortString());
	}

	/**
	 * The ranks a new sect is born with, strongest (the first Sect Master) first: in the lower realms a master between Core
	 * Formation and Nascent Soul Grand Perfection (never a Heavenly Being: they leave), most members far below; in the Upper
	 * Realm, rich in qi, a Heavenly Being or Four Axis master and its members from Core Formation up, leaning toward the top.
	 */
	public static int[] foundingRanks(RandomSource random, int count, boolean upperRealm) {
		int[] ranks = new int[count];
		int floor = floorRank(upperRealm);
		// Below most members sit near the floor; in the Upper Realm they spread toward the master.
		double lean = upperRealm ? 0.8 : 1.7;
		int master = upperRealm
				? PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY) + (int) (8 * Math.pow(random.nextFloat(), 1.3))
				: PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) + (int) (8 * Math.pow(random.nextFloat(), 1.5));
		master = Math.min(master, upperRealm ? PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)
				: PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION));
		ranks[0] = master;
		for (int i = 1; i < count; i++) {
			ranks[i] = Math.min(master - 1, floor + (int) ((master - floor) * Math.pow(random.nextFloat(), lean)));
		}
		return ranks;
	}

	/** A sect's lowest rank, at which its new disciples come: Qi Refining Early below, Core Formation Early in the Upper Realm. */
	public static int floorRank(boolean upperRealm) {
		return upperRealm ? PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) : 0;
	}

	/** A new member of {@code rank} living at dwelling {@code home} (-1: the master's seat); not yet added to the world. */
	private static CultivatorNpc spawnMember(ServerLevel level, Sect sect, int rank, int home, RandomSource random) {
		CultivatorNpc npc = CultivatorNpc.createFor(level);
		if (npc == null) return null;
		npc.randomizeSkin(random);
		npc.setRank(rank);
		npc.joinSect(sect, home);
		Sect.Member member = new Sect.Member(npc.getUUID(), rank, home);
		sect.members.put(member.id, member);
		BlockPos at = sect.dwelling(home);
		if (!level.isLoaded(at)) at = sect.core.above();
		npc.moveTo(at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, random.nextFloat() * 360, 0);
		npc.setHealth(npc.getMaxHealth());
		return npc;
	}

	private static BoundingBox box(int[] values, BlockPos fallback) {
		return values.length == 6 ? new BoundingBox(values[0], values[1], values[2], values[3], values[4], values[5]) : new BoundingBox(fallback);
	}

	// --- Titles ---

	/** Recalculates titles and hands them to the members that are loaded; the barrier takes the new master's strength. */
	public static void retitle(ServerLevel level, Sect sect) {
		sect.recalculateTitles();
		for (CultivatorNpc npc : CultivatorNpc.loadedMembers(level, sect.id)) {
			Sect.Member member = sect.members.get(npc.getUUID());
			if (member != null && npc.getTitle() != member.title) npc.setTitle(member.title);
		}
		Sect.Member master = sect.master();
		if (master != null && level.dimension() == sect.dimension && level.isLoaded(sect.core)
				&& level.getBlockEntity(sect.core) instanceof FormationCoreBlockEntity core) {
			core.setSectRank(master.rank);
		}
		dirty(level.getServer());
	}

	public static void onMemberRankChanged(ServerLevel level, CultivatorNpc npc) {
		Sect sect = get(level.getServer(), npc.getSectId());
		if (sect == null) return;
		Sect.Member member = sect.members.get(npc.getUUID());
		if (member == null) return;
		member.rank = npc.getRank();
		member.cultivation = npc.cultivation().getCultivation();
		retitle(level, sect);
	}

	// --- Losing members ---

	/**
	 * A member died. Killed by a player (directly, or by their arrows, pets or a push into the lava): never replaced, and the
	 * sect's capacity drops for good. Otherwise a replacement comes in a few days. The last member's death ends the sect.
	 */
	public static void onMemberDeath(ServerLevel level, CultivatorNpc npc, boolean byPlayer) {
		Sect sect = get(level.getServer(), npc.getSectId());
		if (sect == null) return;
		Sect.Member member = sect.members.remove(npc.getUUID());
		if (member == null) return;
		long now = level.getGameTime();
		if (member.title.isElderOrAbove()) sect.elderFellAt = now;
		if (sect.members.isEmpty()) {
			sect.extinct = true;
			sect.replacements.clear();
			DefyingTheHeavens.LOGGER.info("The {} has fallen: its last member is dead", sect.name);
			Rumours.noteFallenSect(sect, now); // its core stands unclaimed, and people will talk of it
		} else if (byPlayer) {
			sect.capacity = Math.max(0, sect.capacity - 1);
		} else {
			scheduleReplacement(sect, now, level.getRandom());
		}
		retitle(level, sect);
		LivingEntity killer = npc.getKillCredit();
		if (killer != null && !sect.extinct) alert(level, sect, killer, npc.blockPosition(), Alarm.MEMBER_ATTACKED, member.title, null);
	}

	/** A member left the sect alive (outgrew it, or was taken out of the world): a replacement comes in a few days. */
	public static void onMemberDeparted(ServerLevel level, UUID sectId, UUID memberId) {
		Sect sect = get(level.getServer(), sectId);
		if (sect == null || sect.members.remove(memberId) == null) return;
		if (!sect.extinct) scheduleReplacement(sect, level.getGameTime(), level.getRandom());
		retitle(level, sect);
	}

	/**
	 * A member cast out by one of the sect's seniors (SectMembership#banish: a soul in its master's or an elder's body): it sets off to
	 * make its own way as a rogue (woken for it, if its land isn't loaded, as one let go to make room is), and a new disciple is sent
	 * for in a few days.
	 */
	public static void banish(MinecraftServer server, Sect sect, UUID memberId) {
		Sect.Member out = sect.members.remove(memberId);
		if (out == null) return;
		ServerLevel home = server.getLevel(sect.dimension);
		if (home != null && home.getEntity(memberId) instanceof CultivatorNpc banished) banished.dismiss(home);
		else sect.letGo.put(memberId, new Sect.LetGo(out.lastAt, out.home));
		long now = home != null ? home.getGameTime() : server.overworld().getGameTime();
		if (!sect.extinct) scheduleReplacement(sect, now, server.overworld().getRandom());
		if (home != null) retitle(home, sect);
		else dirty(server);
	}

	private static void scheduleReplacement(Sect sect, long now, RandomSource random) {
		sect.replacements.add(now + Sect.REPLACEMENT_MIN + random.nextInt((int) Sect.REPLACEMENT_SPREAD));
	}

	// --- Newcomers from below (cultivators who ascended to the Upper Realm) ---

	/**
	 * How far (blocks) a newly ascended cultivator looks for a sect to join, nearest first, so it goes further only when
	 * there's none nearer (8,192 blocks takes it about 17 minutes unseen, NpcTravel); and how long a slot is held for it (3
	 * days).
	 */
	public static final double JOIN_RANGE = 8192;
	public static final long RESERVE_TIME = 3 * HeavenRitual.DAY;

	/**
	 * The nearest sect {@code npc} could join: in its realm, of its path (righteous, neutral or devilish), within
	 * {@link #JOIN_RANGE}, still standing, and either with a slot nobody fills or has reserved, or full with a member weaker
	 * than {@code npc} to let go (Sect#canTakeIn). Only sects already founded are known: those within SectSites#RIFT_REACH of
	 * the rift, where newcomers arrive, and those near where players have been. Null if none.
	 */
	public static Sect findOpening(ServerLevel level, CultivatorNpc npc) {
		Alignment.Faction faction = Alignment.factionOf(npc.getAlignment());
		Sect best = null;
		double bestDistance = JOIN_RANGE * JOIN_RANGE;
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.extinct || sect.dimension != level.dimension() || !sect.canTakeIn(npc.getRank()) || Alignment.factionOf(sect.alignment) != faction) continue;
			double dx = sect.core.getX() - npc.getX(), dz = sect.core.getZ() - npc.getZ();
			double distance = dx * dx + dz * dz;
			if (distance <= bestDistance) {
				bestDistance = distance;
				best = sect;
			}
		}
		return best;
	}

	/**
	 * Holds a slot in {@code sect} for {@code npc} on its way to join, for {@link #RESERVE_TIME}: the next new disciple due to
	 * fill a slot (if any is on the way) is called off, and nobody else can take the slot meanwhile.
	 */
	public static void reserve(ServerLevel level, Sect sect, UUID npc) {
		sect.reserved.put(npc, level.getGameTime() + RESERVE_TIME);
		if (!sect.replacements.isEmpty()) sect.replacements.remove(java.util.Collections.min(sect.replacements));
		dirty(level.getServer());
	}

	/** A held slot given up (the newcomer died on the way, or never came): the sect sends for a new disciple as usual. */
	public static void release(ServerLevel level, UUID sectId, UUID npc) {
		Sect sect = get(level.getServer(), sectId);
		if (sect == null || sect.reserved.remove(npc) == null) return;
		if (!sect.extinct) scheduleReplacement(sect, level.getGameTime(), level.getRandom());
		dirty(level.getServer());
	}

	/**
	 * {@code npc} arrives and joins {@code sect} in the place it holds: a member like any other, titled by its strength, so
	 * the Sect Master if it is the strongest. A full sect lets its weakest member go to make room (it sets off to make its own
	 * way, and no replacement is sent for). @return false if there is no room after all (the sect fell, or everyone left to
	 * let go has grown stronger than the newcomer)
	 */
	public static boolean admit(ServerLevel level, Sect sect, CultivatorNpc npc) {
		sect.reserved.remove(npc.getUUID());
		if (!sect.extinct && sect.members.size() >= sect.capacity) {
			Sect.Member out = sect.weakestBelow(npc.getRank());
			if (out != null) {
				sect.members.remove(out.id);
				// Loaded, it sets off now; unloaded, its land is woken so that it does all the same (rouse).
				ServerLevel home = level.getServer().getLevel(sect.dimension);
				if (home != null && home.getEntity(out.id) instanceof CultivatorNpc dismissed) dismissed.dismiss(home);
				else sect.letGo.put(out.id, new Sect.LetGo(out.lastAt, out.home));
				DefyingTheHeavens.LOGGER.info("The {} lets its weakest member go to make room for a cultivator from below", sect.name);
			}
		}
		if (sect.extinct || sect.members.size() >= sect.capacity) {
			dirty(level.getServer());
			return false;
		}
		int home = sect.freeHome();
		npc.joinSect(sect, home);
		Sect.Member member = new Sect.Member(npc.getUUID(), npc.getRank(), home);
		member.cultivation = npc.cultivation().getCultivation();
		sect.members.put(member.id, member);
		retitle(level, sect);
		npc.setTitle(sect.members.get(npc.getUUID()).title);
		DefyingTheHeavens.LOGGER.info("A cultivator from below joins the {} as {}", sect.name, member.title.getId());
		return true;
	}

	// --- Ticking ---

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 0) return;
		SectData data = SectData.get(server);
		for (Sect sect : data.all()) {
			if (!sect.letGo.isEmpty()) rouse(server, sect);
			if (!sect.reserved.isEmpty()) {
				// Held slots whose newcomer never came: the sect sends for disciples instead.
				ServerLevel home = server.getLevel(sect.dimension);
				long now = server.overworld().getGameTime();
				for (UUID late : sect.reserved.entrySet().stream().filter(e -> e.getValue() <= now).map(Map.Entry::getKey).toList()) {
					if (home != null) release(home, sect.id, late);
					else sect.reserved.remove(late);
				}
			}
			if (sect.extinct || sect.replacements.isEmpty()) continue;
			ServerLevel level = server.getLevel(sect.dimension);
			if (level == null || !level.isLoaded(sect.core)) continue;
			long now = level.getGameTime();
			boolean changed = false;
			for (Iterator<Long> it = sect.replacements.iterator(); it.hasNext(); ) {
				long due = it.next();
				if (due > now) continue;
				it.remove();
				changed = true;
				// No room left: the slot was a player's kill, or is held for a newcomer from below.
				if (sect.members.size() + sect.reserved.size() >= sect.capacity) continue;
				CultivatorNpc npc = spawnMember(level, sect, floorRank(ModDimensions.isUpperRealm(sect.dimension)), sect.freeHome(), level.getRandom());
				if (npc == null) continue;
				BlockPos gate = sect.post(Sect.Post.GATE, 0);
				if (level.isLoaded(gate)) npc.moveTo(gate.getX() + 0.5, gate.getY() + 0.1, gate.getZ() + 0.5, npc.getYRot(), 0);
				sect.recalculateTitles();
				npc.setTitle(sect.members.get(npc.getUUID()).title);
				npc.equip(level.getRandom());
				level.addFreshEntity(npc);
				DefyingTheHeavens.LOGGER.info("A new disciple joins the {}", sect.name);
			}
			if (changed) retitle(level, sect);
		}
	}

	/** How many looks (5 seconds apart) each place a let-go member may be is woken for. */
	static final int LOOKS_PER_PLACE = 3;

	/**
	 * Members let go for a newcomer while their land was unloaded (Sect#letGo): each one's land is woken until it has loaded,
	 * and then it sets off at once (CultivatorNpc#onLoad), out of the world straight away with nobody near (NpcTravel), long
	 * before anyone comes. Looked for where it last stood (exact: it can't move while unloaded), then, if that isn't known (a
	 * member unloaded before positions were kept), at its home, then across the whole territory. Should it be in none of
	 * them, it still sets off the moment its land next loads.
	 */
	static void rouse(MinecraftServer server, Sect sect) {
		ServerLevel level = server.getLevel(sect.dimension);
		for (Iterator<Map.Entry<UUID, Sect.LetGo>> it = sect.letGo.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Sect.LetGo> entry = it.next();
			Sect.LetGo gone = entry.getValue();
			if (level != null && level.getEntity(entry.getKey()) instanceof CultivatorNpc npc) {
				if (sect.id.equals(npc.getSectId())) npc.dismiss(level); // (it already has, as it loaded)
				it.remove();
				continue;
			}
			List<BlockPos> places = new ArrayList<>();
			if (gone.lastAt != null) places.add(gone.lastAt);
			places.add(sect.dwelling(gone.home));
			places.add(sect.core);
			int place = gone.looks++ / LOOKS_PER_PLACE;
			if (level == null || place >= places.size()) {
				it.remove();
				DefyingTheHeavens.LOGGER.info("A member let go by the {} wasn't found; it sets off when its land next loads", sect.name);
				continue;
			}
			// The last look covers the whole territory: the barrier, the 30 blocks members keep to past it, and a chase beyond.
			int distance = place < places.size() - 1 ? 2 : (sect.barrierRadius + 30 + 48) / 16 + 1;
			NpcLedger.wake(level, places.get(place), distance);
		}
		dirty(server);
	}

	// --- Calls to arms ---

	/** Calls the members loaded nearby to deal with {@code aggressor}; each decides by its title (see the class notes). */
	public static void alert(ServerLevel level, Sect sect, LivingEntity aggressor, BlockPos where, Alarm alarm, NpcTitle victimTitle, CultivatorNpc victim) {
		if (sect == null || sect.extinct || aggressor == null || !aggressor.isAlive()) return;
		if (aggressor instanceof CultivatorNpc npc && sect.id.equals(npc.getSectId())) return;
		if (aggressor instanceof ServerPlayer player) SectMembership.revokeGuest(player, sect); // its barrier shuts on them again
		long now = level.getGameTime();
		boolean masterRoused = sect.isBarrierDown() || sect.elderFellAt >= 0 && now - sect.elderFellAt < MASTER_WRATH || alarm == Alarm.CORE_DESTROYED;
		// A member attacked in the territory by a player or a cultivator, or the barrier broken through: the whole sect turns
		// out. (A monster at a gate guard is only the nearby disciples' business.)
		boolean person = aggressor instanceof Player || aggressor instanceof CultivatorNpc;
		boolean everyone = alarm == Alarm.BARRIER_BROKEN || alarm == Alarm.MEMBER_ATTACKED && person && sect.inTerritory(where);
		for (CultivatorNpc npc : CultivatorNpc.loadedMembers(level, sect.id)) {
			if (npc == victim || npc.getTarget() == aggressor) continue;
			double distance = Math.sqrt(npc.blockPosition().distSqr(where));
			boolean critical = alarm == Alarm.CRITICAL_BREACH || alarm == Alarm.CORE_DESTROYED;
			boolean respond = everyone ? sect.inTerritory(npc.blockPosition()) : switch (npc.getTitle()) {
				case OUTER_DISCIPLE, INNER_DISCIPLE -> distance <= DISCIPLE_REACH || critical && sect.inTerritory(npc.blockPosition());
				case ELDER, GRAND_ELDER -> critical || sect.isBarrierDown()
						|| alarm == Alarm.MEMBER_ATTACKED && victimTitle != null && victimTitle.ordinal() > npc.getTitle().ordinal() && distance <= ELDER_REACH;
				case SECT_MASTER -> masterRoused;
				default -> false;
			};
			if (respond) npc.answerAlert(aggressor);
		}
		if (person) NpcLife.onAlarm(level, sect, alarm, where); // the nearest raise the cry
	}

	public static void onMemberAttacked(ServerLevel level, CultivatorNpc victim, LivingEntity attacker) {
		alert(level, get(level.getServer(), victim.getSectId()), attacker, victim.blockPosition(), Alarm.MEMBER_ATTACKED, victim.getTitle(), victim);
	}

	/** A player struck the sect's barrier, or ({@code broken}) broke a block of it, forcing a way in. */
	public static void onBarrierAssailed(ServerLevel level, UUID sectId, ServerPlayer player, BlockPos where, boolean broken) {
		alert(level, get(level.getServer(), sectId), player, where, broken ? Alarm.BARRIER_BROKEN : Alarm.BARRIER_ASSAILED, null, null);
	}

	/** Someone laid hands on the treasury or the core. */
	public static void onCriticalAreaBreached(ServerLevel level, UUID sectId, ServerPlayer player, BlockPos where) {
		if (player.isCreative() || player.isSpectator()) return;
		alert(level, get(level.getServer(), sectId), player, where, Alarm.CRITICAL_BREACH, null, null);
	}

	/** The sect a position lies in (its grounds' bounds), or null. */
	public static Sect sectAt(ServerLevel level, BlockPos pos) {
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (!sect.extinct && sect.dimension == level.dimension() && sect.bounds.isInside(pos)) return sect;
		}
		return null;
	}

	/** How long a broken treasury seal stays broken before the elders keeping it cast it anew (5 minutes). */
	public static final long SEAL_RECAST = 6000;

	/**
	 * The treasury's seal, if it holds: while one of its keepers, an Elder or a Grand Elder, stands in the territory (alive,
	 * its land loaded), the treasury's chests and pedestals won't open and nothing in it can be broken. They must be beaten,
	 * or drawn off, first; a treasury nobody is left to keep is open to whoever comes. A cultivator a realm above the
	 * strongest of them can break it ({@link #breakTreasurySeal}). @return the seal's realm (its strongest keeper's), or null
	 * if it doesn't hold (no keeper there, or broken and not yet cast anew)
	 */
	public static Realm treasurySeal(ServerLevel level, Sect sect) {
		if (level.getGameTime() < sect.sealBrokenUntil) return null;
		Realm seal = null;
		for (CultivatorNpc npc : CultivatorNpc.loadedMembers(level, sect.id)) {
			NpcTitle title = npc.getTitle();
			if ((title != NpcTitle.ELDER && title != NpcTitle.GRAND_ELDER) || !sect.inTerritory(npc.blockPosition())) continue;
			Realm realm = CultivatorNpc.realmOf(npc.getRank());
			if (seal == null || realm.ordinal() > seal.ordinal()) seal = realm;
		}
		return seal;
	}

	public static boolean treasurySealed(ServerLevel level, Sect sect) {
		return treasurySeal(level, sect) != null;
	}

	/**
	 * {@code player} lays hands on the sealed treasury of {@code sect}: one a full realm above the seal (its strongest keeper's
	 * realm; a mortal never is) breaks it, and the treasury lies open until its keepers cast it anew ({@link #SEAL_RECAST}).
	 * The sect knows its treasury is being robbed either way (onTrespass). @return whether the seal broke
	 */
	public static boolean breakTreasurySeal(ServerLevel level, Sect sect, ServerPlayer player, BlockPos at) {
		Realm seal = treasurySeal(level, sect);
		if (seal == null) return false;
		PlayerCultivation c = CultivationManager.get(player);
		// By the stronger of their two paths, the body's (as a ghost palace's seal is: PalaceTreasury#touch).
		if (c.isMortal() || TwoPaths.wieldedRealm(c).ordinal() <= seal.ordinal()) return false;
		sect.sealBrokenUntil = level.getGameTime() + SEAL_RECAST;
		dirty(level.getServer());
		level.sendParticles(ParticleTypes.ENCHANT, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 30, 0.5, 0.4, 0.5, 0.6);
		level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.06);
		level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 0.6f);
		level.playSound(null, at, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.7f, 0.8f);
		return true;
	}

	/**
	 * Whether {@code pos} lies in a treasury no hopper may draw from (HopperBlockEntityMixin; the user's pick after the bug sweep of
	 * 2026-10-23: a hopper under a treasury chest emptied it with no hand laid on it): a sect's, whose rules are for hands (the
	 * warning, the casting out, the seal: SectMembership#treasury), or a ghost palace's (PalaceTreasury).
	 */
	public static boolean treasuryAt(ServerLevel level, BlockPos pos) {
		// Every sect's, not the first whose grounds hold the place (sectAt): grounds that overlap would hide a treasury.
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (!sect.extinct && sect.dimension == level.dimension() && sect.bounds.isInside(pos) && sect.treasury.isInside(pos)) return true;
		}
		return PalaceTreasury.isTreasury(level, pos);
	}

	/** The sect whose sealed treasury {@code pos} lies in (see {@link #treasurySealed}), or null. */
	public static Sect sealedTreasuryAt(ServerLevel level, BlockPos pos) {
		Sect sect = sectAt(level, pos);
		return sect != null && sect.treasury.isInside(pos) && treasurySealed(level, sect) ? sect : null;
	}

	/** An explosion: the blocks it would break in sealed treasuries are spared (see ExplosionMixin). */
	public static void spareSealedTreasuries(ServerLevel level, List<BlockPos> toBlow) {
		if (toBlow.isEmpty()) return;
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.extinct || sect.dimension != level.dimension() || toBlow.stream().noneMatch(sect.treasury::isInside)) continue;
			if (treasurySealed(level, sect)) toBlow.removeIf(sect.treasury::isInside);
		}
	}

	/** A player broke a block or opened something on a sect's grounds: theft from the treasury, or plain trespass. */
	public static void onTrespass(ServerLevel level, ServerPlayer player, BlockPos pos) {
		if (player.isCreative() || player.isSpectator()) return;
		Sect sect = sectAt(level, pos);
		if (sect == null || sect.members.isEmpty()) return;
		boolean theft = sect.treasury.isInside(pos);
		if (theft) {
			// Robbing the treasury turns the whole sect against the thief, once a day however many blocks it takes.
			long today = Regard.day(level.getGameTime());
			sect.robbedDay.values().removeIf(day -> day != today); // only today's are needed (and saved)
			Long last = sect.robbedDay.put(player.getUUID(), today);
			if (last == null || last != today) changeStanding(level.getServer(), sect.id, player.getUUID(), Regard.TREASURY_ROBBED);
		}
		alert(level, sect, player, pos, theft ? Alarm.CRITICAL_BREACH : Alarm.TRESPASS, null, null);
	}

	public static void onCoreDestroyed(ServerLevel level, UUID sectId) {
		Sect sect = get(level.getServer(), sectId);
		if (sect == null) return;
		sect.barrierDownSince = level.getGameTime();
		dirty(level.getServer());
		Rumours.forgetCore(sect); // no unclaimed core there now
		ServerPlayer culprit = null;
		double best = 64;
		for (ServerPlayer player : level.players()) {
			double d = player.distanceToSqr(sect.core.getX() + 0.5, sect.core.getY() + 0.5, sect.core.getZ() + 0.5);
			if (d < best && !player.isCreative() && !player.isSpectator()) {
				best = d;
				culprit = player;
			}
		}
		if (culprit != null) alert(level, sect, culprit, sect.core, Alarm.CORE_DESTROYED, null, null);
	}

	/** The core's barrier rose, or came down against the sect's will ({@code involuntary}: out of qi or shattered). */
	public static void onBarrierChanged(ServerLevel level, UUID sectId, boolean raised, boolean involuntary) {
		Sect sect = get(level.getServer(), sectId);
		if (sect == null) return;
		if (raised) sect.barrierDownSince = -1;
		else if (involuntary && sect.barrierDownSince < 0) sect.barrierDownSince = level.getGameTime();
		dirty(level.getServer());
	}

	private SectManager() {}
}
