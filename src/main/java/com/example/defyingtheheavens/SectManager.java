package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Runs the sects (see {@link Sect}): founds them when their Formation Core first ticks, keeps titles in step with
 * cultivation, replaces those lost to anything but a player, and calls members to arms.
 * <p>
 * <b>Who answers a call.</b> Inner and Outer Disciples answer anything near them (or anywhere on the grounds when the
 * treasury or the core is breached). Elders and Grand Elders keep to their posts unless a lower-ranked member is attacked near
 * them, the treasury or core is breached, or the barrier is down. The Sect Master stays on the dais until the barrier is
 * shattered, its core destroyed, or an Elder (or higher) has just fallen. Anyone attacked fights back.
 */
public final class SectManager {
	/** For this long after an Elder falls, the Sect Master takes the field. */
	private static final long MASTER_WRATH = 6000;
	private static final double DISCIPLE_REACH = 40;
	private static final double ELDER_REACH = 24;

	public enum Alarm { MEMBER_ATTACKED, BARRIER_ASSAILED, TRESPASS, CRITICAL_BREACH, CORE_DESTROYED }

	public static Sect get(MinecraftServer server, UUID id) {
		return id == null ? null : SectData.get(server).get(id);
	}

	private static void dirty(MinecraftServer server) {
		SectData.get(server).setDirty();
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
	 * Realm a Heavenly Being or Four Axis master, its disciples from Foundation Building up.
	 */
	public static int[] foundingRanks(RandomSource random, int count, boolean upperRealm) {
		int[] ranks = new int[count];
		int floor = upperRealm ? PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY) : 0;
		int master = upperRealm
				? PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY) + (int) (8 * Math.pow(random.nextFloat(), 1.3))
				: PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) + (int) (8 * Math.pow(random.nextFloat(), 1.5));
		master = Math.min(master, upperRealm ? PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)
				: PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION));
		ranks[0] = master;
		for (int i = 1; i < count; i++) {
			ranks[i] = Math.min(master - 1, floor + (int) ((master - floor) * Math.pow(random.nextFloat(), 1.7)));
		}
		return ranks;
	}

	/** A new member of {@code rank} living at dwelling {@code home} (-1: the master's seat); not yet added to the world. */
	private static CultivatorNpc spawnMember(ServerLevel level, Sect sect, int rank, int home, RandomSource random) {
		CultivatorNpc npc = ModEntities.CULTIVATOR.create(level);
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

	private static void scheduleReplacement(Sect sect, long now, RandomSource random) {
		sect.replacements.add(now + Sect.REPLACEMENT_MIN + random.nextInt((int) Sect.REPLACEMENT_SPREAD));
	}

	// --- Ticking ---

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 0) return;
		SectData data = SectData.get(server);
		for (Sect sect : data.all()) {
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
				if (sect.members.size() >= sect.capacity) continue; // no room left: the slot was a player's kill
				CultivatorNpc npc = spawnMember(level, sect, 0, sect.freeHome(), level.getRandom());
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

	// --- Calls to arms ---

	/** Calls the members loaded nearby to deal with {@code aggressor}; each decides by its title (see the class notes). */
	public static void alert(ServerLevel level, Sect sect, LivingEntity aggressor, BlockPos where, Alarm alarm, NpcTitle victimTitle, CultivatorNpc victim) {
		if (sect == null || sect.extinct || aggressor == null || !aggressor.isAlive()) return;
		if (aggressor instanceof CultivatorNpc npc && sect.id.equals(npc.getSectId())) return;
		long now = level.getGameTime();
		boolean masterRoused = sect.isBarrierDown() || sect.elderFellAt >= 0 && now - sect.elderFellAt < MASTER_WRATH || alarm == Alarm.CORE_DESTROYED;
		for (CultivatorNpc npc : CultivatorNpc.loadedMembers(level, sect.id)) {
			if (npc == victim || npc.getTarget() == aggressor) continue;
			double distance = Math.sqrt(npc.blockPosition().distSqr(where));
			boolean critical = alarm == Alarm.CRITICAL_BREACH || alarm == Alarm.CORE_DESTROYED;
			boolean respond = switch (npc.getTitle()) {
				case OUTER_DISCIPLE, INNER_DISCIPLE -> distance <= DISCIPLE_REACH || critical && sect.inTerritory(npc.blockPosition());
				case ELDER, GRAND_ELDER -> critical || sect.isBarrierDown()
						|| alarm == Alarm.MEMBER_ATTACKED && victimTitle != null && victimTitle.ordinal() > npc.getTitle().ordinal() && distance <= ELDER_REACH;
				case SECT_MASTER -> masterRoused;
				default -> false;
			};
			if (respond) npc.answerAlert(aggressor);
		}
	}

	public static void onMemberAttacked(ServerLevel level, CultivatorNpc victim, LivingEntity attacker) {
		alert(level, get(level.getServer(), victim.getSectId()), attacker, victim.blockPosition(), Alarm.MEMBER_ATTACKED, victim.getTitle(), victim);
	}

	public static void onBarrierAssailed(ServerLevel level, UUID sectId, ServerPlayer player, BlockPos where) {
		alert(level, get(level.getServer(), sectId), player, where, Alarm.BARRIER_ASSAILED, null, null);
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

	/** A player broke a block or opened something on a sect's grounds: theft from the treasury, or plain trespass. */
	public static void onTrespass(ServerLevel level, ServerPlayer player, BlockPos pos) {
		if (player.isCreative() || player.isSpectator()) return;
		Sect sect = sectAt(level, pos);
		if (sect == null || sect.members.isEmpty()) return;
		alert(level, sect, player, pos, sect.treasury.isInside(pos) ? Alarm.CRITICAL_BREACH : Alarm.TRESPASS, null, null);
	}

	public static void onCoreDestroyed(ServerLevel level, UUID sectId) {
		Sect sect = get(level.getServer(), sectId);
		if (sect == null) return;
		sect.barrierDownSince = level.getGameTime();
		dirty(level.getServer());
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
