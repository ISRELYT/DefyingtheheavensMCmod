package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Those who come for a secret realm besides the players (SecretRealms' design: "each nearby sect sends 2 to 4 disciples who are under the
 * limit [now: at or above its lowest realm, and not far past what it is meant for]; a few rogues and devilish cultivators come too; at most
 * about 16 NPCs per realm").
 * <ul>
 *   <li><b>Who</b> ({@link #stir}): as a realm stirs, its roll is made, kept with its record: the sects of its world nearest its Seal
 *   ({@link #SECTS} at most, within {@link #SECT_REACH}) send two to four each, of the realm's ranks; rogues of either path fill out the rest,
 *   {@link #MOST} at most.</li>
 *   <li><b>Waiting</b>: while it stirs, those on the roll wait at its Seal, camped round its floor (raised there whenever someone is near enough
 *   to see them); its gate open, they go in.</li>
 *   <li><b>Inside</b> ({@link VisitGoal}): raised in it as the first player comes in (where they would have got to by then). They take what
 *   lies about (herbs from the Valley's beds, old swords from the Tomb's floor, the Battlefield's fallen's things: its guardians wake), force
 *   the vaults their realm can, and later go at its trials, each realm's own (RealmWorks#rivalTrial); one who passes them all may take the
 *   inheritance, and carries it. The righteous and devilish fight as anywhere; one of the devilish goes for anyone near it badly hurt
 *   ({@link #greedy}); a sect's disciples stand with its own disciples, players among them (CultivatorNpc#isAlly). The realm's sky holds them
 *   low as it holds everyone.</li>
 *   <li><b>Unseen</b>: an opening no player went into goes on in its tales ({@link #closed}): a few of them die there (their bodies lie
 *   inside, where its earlier visitors' bones lie: RealmWorks#remainsSpot), and now and then one takes a legacy.</li>
 *   <li><b>Out</b>: cast out beside its Seal with the rest as it closes; seen, they linger, and go their way out of sight (CultivatorNpc's
 *   check, {@link #stale}). An inheritor stays in the world: a rogue now, carrying what it won, to be heard of (Rumours).</li>
 * </ul>
 */
public final class SecretRealmRivals {
	/** At most this many come for one opening. */
	public static final int MOST = 16;
	/** The sects within this of a Seal send their disciples, at most this many sects. */
	static final int SECT_REACH = 3000, SECTS = 3;
	/** Within this of a Seal, someone sees its camp (and its people are raised). */
	static final int CAMP_SEEN = 160;
	/** Roll states (AMBUSH: of the devilish, gone out early to lie in wait at its Seal: {@link #quaking}). */
	static final int WAITING = 0, INSIDE = 1, OUT = 2, DEAD = 3, AMBUSH = 4;
	/** At most this many lie in wait at the Seal; how long after it shuts they wait (ticks). */
	static final int AMBUSHERS = 2, AMBUSH_TICKS = 2400;

	// --- The roll ---

	private static ListTag roll(SecretRealms.Record r) {
		return r.rivals.getList("Roll", Tag.TAG_COMPOUND);
	}

	/** The roll of those coming for its next opening, made as it stirs. */
	static void stir(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		RandomSource random = server.overworld().getRandom();
		ListTag roll = new ListTag();
		SecretRealm realm = r.realm;
		int low = realm.lowest(), high = PlayerCultivation.rank(Realm.values()[Math.min(Realm.values().length - 1, realm.meantFor().ordinal() + 1)], Stage.MID);
		if (r.seal != null) {
			List<Sect> near = new ArrayList<>();
			for (Sect sect : SectData.get(server).all()) {
				if (!sect.extinct && sect.dimension == realm.home() && sect.core.distSqr(r.seal) < (double) SECT_REACH * SECT_REACH) near.add(sect);
			}
			near.sort(Comparator.comparingDouble(sect -> sect.core.distSqr(r.seal)));
			for (Sect sect : near.subList(0, Math.min(SECTS, near.size()))) {
				int sent = 2 + random.nextInt(3);
				for (int i = 0; i < sent && roll.size() < MOST - 2; i++) roll.add(member(random, low, high, sect.alignment, sect.id, sect.race));
			}
		}
		// Rogues of either path to fill it out.
		int rogues = Math.max(2, Math.min(MOST - roll.size(), 2 + random.nextInt(4)));
		for (int i = 0; i < rogues && roll.size() < MOST; i++) roll.add(member(random, low, high, Alignment.randomRogue(random), null, Bloodline.HUMAN));
		r.rivals = new CompoundTag();
		r.rivals.put("Roll", roll);
		data.setDirty();
	}

	private static CompoundTag member(RandomSource random, int low, int high, int alignment, UUID sect, Bloodline race) {
		CompoundTag m = new CompoundTag();
		m.putUUID("Id", UUID.randomUUID());
		if (sect != null) m.putUUID("Sect", sect);
		m.putInt("Rank", low + random.nextInt(Math.max(1, high - low + 1)));
		m.putInt("Align", Alignment.clamp(alignment + random.nextInt(21) - 10));
		m.putInt("Skin", random.nextInt(CultivatorNpc.SKINS));
		if (race != null && race.isBeast()) {
			m.putString("Race", race.getId());
			m.putInt("Variant", random.nextInt(3));
		}
		m.putInt("State", WAITING);
		return m;
	}

	/** One of the roll raised at {@code at} in {@code level} (as one who waits at the Seal, or one inside), unless it already stands there. */
	static CultivatorNpc raise(ServerLevel level, SecretRealms.Record r, CompoundTag m, Vec3 at, boolean inside) {
		UUID id = m.getUUID("Id");
		if (level.getEntity(id) != null) return null;
		CultivatorNpc npc = ModEntities.CULTIVATOR.create(level);
		if (npc == null) return null;
		npc.setUUID(id);
		RandomSource random = RandomSource.create(id.getLeastSignificantBits());
		npc.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
		npc.becomeRogue(m.getInt("Rank"), m.getInt("Align"), random);
		npc.setSkin(m.getInt("Skin"));
		if (m.contains("Race")) npc.setRace(Bloodline.byId(m.getString("Race")), m.getInt("Variant"));
		UUID sect = m.hasUUID("Sect") ? m.getUUID("Sect") : null;
		if (sect != null) npc.setTitle(m.getInt("Rank") >= PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) ? NpcTitle.INNER_DISCIPLE : NpcTitle.OUTER_DISCIPLE);
		npc.setVisit(r.realm.getId(), inside ? r.openings : r.openings + 1, sect);
		npc.setPersistenceRequired();
		level.addFreshEntity(npc);
		return npc;
	}

	/** While it stirs: those on its roll wait at its Seal, raised as someone comes near enough to see them, each at their place round its floor. */
	static void tickStirring(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		if (now % 40 != 13 || r.seal == null) return;
		ServerLevel home = server.getLevel(r.realm.home());
		if (home == null || !home.isLoaded(r.seal) || home.getNearestPlayer(r.seal.getX(), r.seal.getY(), r.seal.getZ(), CAMP_SEEN, false) == null) return;
		ListTag roll = roll(r);
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.getInt("State") != WAITING) continue;
			double a = i * Math.PI * 2 / Math.max(1, roll.size()) + 0.3, d = SecretRealms.SEAL_RADIUS + 4 + (i % 3) * 2;
			int x = Mth.floor(r.seal.getX() + 0.5 + Math.cos(a) * d), z = Mth.floor(r.seal.getZ() + 0.5 + Math.sin(a) * d);
			if (!home.isLoaded(new BlockPos(x, 0, z))) continue;
			int y = home.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			CultivatorNpc npc = raise(home, r, m, new Vec3(x + 0.5, y, z + 0.5), false);
			if (npc != null) {
				npc.setSettledAt(new BlockPos(x, y, z));
				if (i % 2 == 0) npc.setMeditating(true);
			}
		}
	}

	/** It opens: those waiting go in (those standing at the Seal pass into its light and are gone). */
	static void opened(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		ListTag roll = roll(r);
		ServerLevel home = server.getLevel(r.realm.home());
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.getInt("State") != WAITING) continue;
			m.putInt("State", INSIDE);
			if (home != null && home.getEntity(m.getUUID("Id")) instanceof CultivatorNpc npc) {
				home.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1, npc.getZ(), 20, 0.4, 0.8, 0.4, 0.08);
				npc.discard();
			}
		}
		r.rivals.putBoolean("Raised", false);
		data.setDirty();
	}

	/**
	 * The first player in this opening: those of the roll inside are raised in it, where they would have got to by now: at its arrivals early
	 * on, further in the later it is (each realm's own way in: RealmWorks#rivalStart).
	 */
	static void playerEntered(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r) {
		if (r.rivals.getBoolean("Raised")) return;
		ServerLevel level = server.getLevel(ModDimensions.SECRET_REALM);
		if (level == null) return;
		r.rivals.putBoolean("Raised", true);
		data.setDirty();
		long now = server.overworld().getGameTime();
		double along = Mth.clamp((now - r.openedAt) / (double) Math.max(1, r.realm.insideTicks()), 0, 1);
		BlockPos c = r.realm.centre();
		ListTag roll = roll(r);
		RandomSource random = level.getRandom();
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.getInt("State") != INSIDE) continue;
			BlockPos at = r.realm.works().rivalStart(level, level.getSeed(), c, along, random);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
			raise(level, r, m, new Vec3(at.getX() + 0.5, y, at.getZ() + 0.5), true);
		}
	}

	/** While it stands open: those come for it held under the sky's hold as everyone is (unless they fly freely). */
	static void tickOpen(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		if (now % 5 != 0) return;
		ServerLevel level = server.getLevel(ModDimensions.SECRET_REALM);
		if (level == null || level.players().isEmpty()) return;
		BlockPos c = r.realm.centre();
		for (CultivatorNpc npc : level.getEntitiesOfClass(CultivatorNpc.class, new AABB(c).inflate(SecretRealm.BOUND, 160, SecretRealm.BOUND))) {
			if (!npc.isQiFlying() || r.realm.fliesFreely(npc.getRank())) continue;
			int ground = SecretRealms.groundUnder(level, r.realm, npc.getX(), npc.getZ());
			if (npc.getY() > ground + SecretRealms.CEILING) {
				Vec3 v = npc.getDeltaMovement();
				npc.setDeltaMovement(v.x, Math.min(v.y, -0.35), v.z);
			}
		}
	}

	/** One of the roll died: so it stands on the roll (its body lies where it fell, with what it carried: Corpses). */
	static void visitorDied(ServerLevel level, CultivatorNpc npc) {
		SecretRealm realm = SecretRealm.byId(npc.visitRealm());
		if (realm == null) return;
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		SecretRealms.Record r = data.of(realm);
		ListTag roll = roll(r);
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.hasUUID("Id") && m.getUUID("Id").equals(npc.getUUID())) {
				m.putInt("State", DEAD);
				data.setDirty();
			}
		}
		if (ModDimensions.isSecretRealm(level.dimension())) Rumours.noteRealmFallen(realm, npc.getPersonalName(), level.getGameTime());
	}

	/**
	 * It closes: what went on unseen told as it went (an opening no player went into: a few of those in it die there, now and then one takes
	 * a legacy); those inside gone from it; seen, they come out beside its Seal; an inheritor stays in the world as a rogue carrying its
	 * winnings (raised at the Seal whether or not anyone sees it).
	 */
	static void closed(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
		ServerLevel home = server.getLevel(r.realm.home());
		ListTag roll = roll(r);
		RandomSource random = server.overworld().getRandom();
		boolean seen = r.rivals.getBoolean("Raised");
		List<CompoundTag> inside = new ArrayList<>();
		for (int i = 0; i < roll.size(); i++) if (roll.getCompound(i).getInt("State") == INSIDE) inside.add(roll.getCompound(i));
		if (!seen && r.openedAt >= 0 && !inside.isEmpty() && realms != null) unseen(realms, r, inside, random);
		for (CompoundTag m : inside) {
			UUID id = m.getUUID("Id");
			List<ItemStack> carried = new ArrayList<>();
			if (realms != null && realms.getEntity(id) instanceof CultivatorNpc npc) {
				carried.addAll(npc.winnings());
				npc.discard();
			}
			if (m.getInt("State") == DEAD) continue;
			m.putInt("State", OUT);
			if (home == null || r.seal == null) continue;
			boolean heir = m.getBoolean("Inheritor");
			boolean watched = home.isLoaded(r.seal) && home.getNearestPlayer(r.seal.getX(), r.seal.getY(), r.seal.getZ(), CAMP_SEEN, false) != null;
			if (!heir && !watched) continue;
			if (heir) home.getChunk(r.seal.getX() >> 4, r.seal.getZ() >> 4);
			Vec3 at = SecretRealms.besideSeal(home, r.seal, random);
			CultivatorNpc out = raise(home, r, m, at, true);
			if (out == null) continue;
			for (ItemStack stack : carried) out.addWinnings(stack);
			if (heir) {
				// The inheritor goes its own way in the world, what it won with it (won unseen: the Great Inheritance itself).
				out.setVisit("", 0, null);
				if (carried.isEmpty()) for (ItemStack stack : r.realm.works().great(home)) out.addWinnings(stack);
			}
		}
		r.rivals.put("Roll", roll);
		data.setDirty();
	}

	/**
	 * An opening nobody went into, told as its tales would tell it: one in eight of those in it dies there (their bodies left inside, with
	 * a little of what they found); the Great Inheritance untaken, now and then the strongest of them takes it; else now and then one takes
	 * the opening's lesser legacy.
	 */
	private static void unseen(ServerLevel realms, SecretRealms.Record r, List<CompoundTag> inside, RandomSource random) {
		RealmWorks works = r.realm.works();
		BlockPos c = r.realm.centre();
		int bodies = 0;
		for (CompoundTag m : inside) {
			if (random.nextInt(8) != 0 || bodies >= 2) continue;
			m.putInt("State", DEAD);
			bodies++;
			UUID id = m.getUUID("Id");
			Rumours.noteRealmFallen(r.realm, NpcNames.of(id), realms.getGameTime());
			// Where an earlier visitor's bones might lie (each realm's own), with a little of what it found.
			BlockPos at = null;
			for (int tries = 0; at == null && tries < 12; tries++) at = works.remainsSpot(realms, realms.getSeed(), c, random);
			if (at == null) continue;
			realms.getChunk(at.getX() >> 4, at.getZ() >> 4);
			BlockPos where = at.atY(realms.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()));
			if (!realms.getBlockState(where).isAir()) continue;
			realms.setBlock(where, ModBlocks.CORPSE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
			if (realms.getBlockEntity(where) instanceof CorpseBlockEntity body) {
				body.ancient(NpcNames.of(id), m.getInt("Skin"), m.getInt("Rank"), random.nextFloat() * 360, realms.getGameTime(), works.remainsCarried(random));
			}
		}
		CompoundTag strongest = null;
		for (CompoundTag m : inside) if (m.getInt("State") == INSIDE && (strongest == null || m.getInt("Rank") > strongest.getInt("Rank"))) strongest = m;
		if (strongest == null) return;
		String name = NpcNames.of(strongest.getUUID("Id"));
		if (r.inheritor.isEmpty()) {
			if (random.nextInt(16) != 0) return;
			r.inheritor = name;
			r.inheritorNpc = true;
			r.inheritorId = strongest.getUUID("Id");
			strongest.putBoolean("Inheritor", true);
			realms.getServer().getPlayerList().broadcastSystemMessage(works.inheritedMessage(net.minecraft.network.chat.Component.literal(name), r.realm)
					.withStyle(net.minecraft.ChatFormatting.GOLD), false);
			Rumours.noteRealmInheritor(r.realm, name, realms.getGameTime());
		} else if (random.nextInt(3) == 0) {
			r.legacy(name);
		}
	}

	// --- Lying in wait at the Seal ---

	/**
	 * Whether {@code npc}, come for a realm, has been passed by: waiting at its Seal for an opening that has come and gone, inside a realm since
	 * shut, or come out of it and seen by no one now (gone its way); one lying in wait at its Seal stays till its time is up.
	 */
	static boolean stale(ServerLevel level, CultivatorNpc npc) {
		SecretRealm realm = SecretRealm.byId(npc.visitRealm());
		if (realm == null) return true;
		SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
		CompoundTag waiting = entry(r, npc.getUUID());
		if (waiting != null && waiting.getInt("State") == AMBUSH) {
			// Gone out early: inside, its old self is gone; at the Seal it waits its time out.
			if (ModDimensions.isSecretRealm(level.dimension())) return true;
			return level.getServer().overworld().getGameTime() > waiting.getLong("AmbushUntil") && level.getNearestPlayer(npc, 48) == null;
		}
		if (ModDimensions.isSecretRealm(level.dimension())) return r.openedAt < 0 || r.openings != npc.visitOpening();
		if (r.stirred && r.openedAt < 0 && npc.visitOpening() == r.openings + 1) return false; // waiting for it
		if (npc.visitOpening() == r.openings && r.openedAt < 0) return level.getNearestPlayer(npc, 48) == null; // come out, going its way
		return true;
	}

	/**
	 * Whether {@code npc}, of the devilish path and come for the realm it stands in, goes for {@code target} out of greed: anyone close, not its
	 * own, come in as it did (a player, or another come for it), hurt to half their body or worse.
	 */
	public static boolean greedy(CultivatorNpc npc, LivingEntity target) {
		if (npc.visitRealm().isEmpty() || target == npc || !ModDimensions.isSecretRealm(npc.level().dimension())) return false;
		if (npc.getFaction() != Alignment.Faction.DEVILISH || npc.isAlly(target)) return false;
		boolean visitor = target instanceof Player p ? !p.isCreative() && !p.isSpectator() : target instanceof CultivatorNpc other && !other.visitRealm().isEmpty();
		return visitor && target.getHealth() < target.getMaxHealth() * 0.5f && npc.distanceToSqr(target) < 16 * 16;
	}

	static void clear() {
	}

	/** {@code id}'s place on {@code r}'s roll, or null. */
	static CompoundTag entry(SecretRealms.Record r, UUID id) {
		ListTag roll = roll(r);
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.hasUUID("Id") && m.getUUID("Id").equals(id)) return m;
		}
		return null;
	}

	/**
	 * Its quake begins (SecretRealms#cycle): the strongest of the devilish still inside ({@link #AMBUSHERS} at most; not an inheritor) leave it
	 * early, what they took with them (if they stand where anyone is to see them go: else their old selves are let go as they load,
	 * {@link #stale}), to lie in wait at its Seal till {@link #AMBUSH_TICKS} after it shuts.
	 */
	static void quaking(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
		if (realms == null || r.seal == null || !r.rivals.getBoolean("Raised")) return;
		ListTag roll = roll(r);
		List<CompoundTag> devilish = new ArrayList<>();
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.getInt("State") == INSIDE && !m.getBoolean("Inheritor") && Alignment.factionOf(m.getInt("Align")) == Alignment.Faction.DEVILISH) devilish.add(m);
		}
		devilish.sort(Comparator.comparingInt((CompoundTag m) -> -m.getInt("Rank")));
		long until = r.openedAt + r.realm.insideTicks() + AMBUSH_TICKS;
		for (CompoundTag m : devilish.subList(0, Math.min(AMBUSHERS, devilish.size()))) {
			m.putInt("State", AMBUSH);
			m.putLong("AmbushUntil", until);
			if (!(realms.getEntity(m.getUUID("Id")) instanceof CultivatorNpc npc)) continue;
			ListTag won = new ListTag();
			for (ItemStack stack : npc.winnings()) won.add(stack.save(new CompoundTag()));
			m.put("Won", won);
			realms.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1, npc.getZ(), 20, 0.4, 0.8, 0.4, 0.08);
			npc.discard();
		}
		data.setDirty();
	}

	/** Those lying in wait at its Seal, raised there as someone comes near enough to see them; their time up, they are let go. */
	static void tickAmbush(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		if (now % 40 != 21 || r.seal == null) return;
		ServerLevel home = server.getLevel(r.realm.home());
		if (home == null || !home.isLoaded(r.seal) || home.getNearestPlayer(r.seal.getX(), r.seal.getY(), r.seal.getZ(), CAMP_SEEN, false) == null) return;
		lieInWait(home, r, now);
	}

	/** Each of those lying in wait at {@code r}'s Seal raised at its place in the shadows about it (unless it stands there), set on the nearest one marked as prey. */
	private static void lieInWait(ServerLevel home, SecretRealms.Record r, long now) {
		ListTag roll = roll(r);
		ListTag prey = r.rivals.getList("Prey", Tag.TAG_INT_ARRAY);
		int k = 0;
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.getInt("State") != AMBUSH || now > m.getLong("AmbushUntil")) continue;
			CultivatorNpc npc = home.getEntity(m.getUUID("Id")) instanceof CultivatorNpc standing ? standing : null;
			if (npc == null) {
				double a = k * Math.PI + 0.7, d = SecretRealms.SEAL_RADIUS + 9;
				int x = Mth.floor(r.seal.getX() + 0.5 + Math.cos(a) * d), z = Mth.floor(r.seal.getZ() + 0.5 + Math.sin(a) * d);
				if (!home.isLoaded(new BlockPos(x, 0, z))) continue;
				int y = home.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				npc = raise(home, r, m, new Vec3(x + 0.5, y, z + 0.5), true);
				if (npc == null) continue;
				npc.setSettledAt(new BlockPos(x, y, z));
				for (Tag t : m.getList("Won", Tag.TAG_COMPOUND)) npc.addWinnings(ItemStack.of((CompoundTag) t));
			}
			k++;
			if (npc.getTarget() != null && npc.getTarget().isAlive()) continue;
			ServerPlayer nearest = null;
			double best = 40 * 40;
			for (Tag t : prey) {
				if (!(home.getPlayerByUUID(net.minecraft.nbt.NbtUtils.loadUUID(t)) instanceof ServerPlayer p) || !p.isAlive() || p.isCreative() || p.isSpectator()) continue;
				double d = p.distanceToSqr(npc);
				if (d < best) {
					best = d;
					nearest = p;
				}
			}
			if (nearest == null) continue;
			npc.setTarget(nearest);
			nearest.sendSystemMessage(Component.translatable(ModLang.MSG_AMBUSH, npc.getPersonalName(), r.realm.displayName()).withStyle(ChatFormatting.RED));
		}
	}

	/**
	 * {@code p} cast out beside {@code r}'s Seal (SecretRealms#castOut): one who came out with more than they took in ({@code laden}) is marked
	 * prey for those lying in wait there, who are raised and set on them now.
	 */
	static void cameOut(ServerLevel home, SecretRealms.Record r, ServerPlayer p, boolean laden) {
		if (!laden || r.seal == null) return;
		ListTag prey = r.rivals.getList("Prey", Tag.TAG_INT_ARRAY);
		net.minecraft.nbt.IntArrayTag id = net.minecraft.nbt.NbtUtils.createUUID(p.getUUID());
		if (!prey.contains(id)) prey.add(id);
		r.rivals.put("Prey", prey);
		SecretRealms.Data.get(home.getServer()).setDirty();
		lieInWait(home, r, home.getServer().overworld().getGameTime());
	}

	// --- Inside: what one come for it does ---

	/**
	 * One come for a secret realm, in it while it stands open and no fight is on: what lies about for the taking first (two or three: its
	 * realm's pickings, RealmWorks#rivalPickings: herbs in the Valley, old swords in the Tomb, the fallen's things on the Battlefield), then a
	 * vault its realm can force, then (some way into the opening, or with nothing else to do) its trials a step at a time, each realm's own
	 * (RealmWorks#rivalTrial: the Gatekeeper and the master's cauldron; the Sword Path, the sword's spirit and the Sleeping Sword; the General's
	 * hill and his tomb), and so may take the inheritance. Flies low where it can fly (never where a step must be walked), walks where it can't;
	 * gives up on a mark it makes no way toward, and on its trials after three such.
	 */
	public static class VisitGoal extends Goal {
		/** A mark it gets no nearer for this long (ticks) is given up; so many given up on its trials, it gives them up. */
		static final int STUCK_TICKS = 300, STUCK_TRIALS = 3;
		private enum Mode { PICK, VAULT, TRIAL }

		private final CultivatorNpc npc;
		private Mode mode;
		private BlockPos mark;
		private RealmWorks.Trial trial;
		private int working, stuck, picked, strikes;
		/** Asked whether it may start every other tick (a goal's choosing is), so counted, not told by tickCount's (that may never match). */
		private int askAgain;
		private double nearest;
		/** Whether it has set out on its trials (it keeps on at them after). */
		private boolean begun;

		public VisitGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		private SecretRealms.Record record() {
			if (npc.visitRealm().isEmpty() || !(npc.level() instanceof ServerLevel level) || !ModDimensions.isSecretRealm(level.dimension())) return null;
			SecretRealm realm = SecretRealm.byId(npc.visitRealm());
			if (realm == null) return null;
			SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
			return r.openedAt >= 0 ? r : null;
		}

		@Override
		public boolean canUse() {
			if (--askAgain > 0) return false;
			askAgain = 10; // some twenty ticks
			return npc.getTarget() == null && !npc.shouldFlee() && record() != null;
		}

		@Override
		public boolean canContinueToUse() {
			return npc.getTarget() == null && !npc.shouldFlee() && record() != null;
		}

		@Override
		public void stop() {
			npc.getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void tick() {
			SecretRealms.Record r = record();
			if (r == null || !(npc.level() instanceof ServerLevel level)) return;
			if (mark == null) choose(level, r);
			if (mark == null) return;
			double d = npc.position().distanceTo(Vec3.atBottomCenterOf(mark));
			double reach = mode == Mode.TRIAL ? trial.reach() : 2.6;
			if (d > reach) {
				if (d < nearest - 0.5) {
					nearest = d;
					stuck = 0;
				} else if (++stuck > STUCK_TICKS) {
					// No way there: something else (its trials, given up on after a few such).
					if (mode == Mode.TRIAL && ++strikes >= STUCK_TRIALS) RealmWorks.giveUp(r, npc.getUUID());
					mark = null;
					return;
				}
				go(level, Vec3.atBottomCenterOf(mark), d, mode == Mode.TRIAL && trial.onFoot());
				return;
			}
			npc.getNavigation().stop();
			if (npc.isQiFlying()) npc.setQiFlying(false);
			act(level, r);
		}

		/** Its next mark, by how far the opening has gone and what it has done. */
		private void choose(ServerLevel level, SecretRealms.Record r) {
			RealmWorks works = r.realm.works();
			BlockPos c = r.realm.centre();
			double along = (level.getServer().overworld().getGameTime() - r.openedAt) / (double) r.realm.insideTicks();
			mark = null;
			trial = null;
			working = 0;
			stuck = 0;
			RealmWorks.Trial next = RealmWorks.gaveUp(r, npc.getUUID()) ? null : works.rivalTrial(level, r, npc);
			if (next != null && (begun || along > 0.45)) {
				mode = Mode.TRIAL;
				trial = next;
				mark = next.mark();
				begun = true;
			} else if (picked < 3) {
				mode = Mode.PICK;
				mark = works.rivalPickings(level, level.getSeed(), c, npc.blockPosition());
			}
			if (mark == null) {
				mode = Mode.VAULT;
				mark = nearestVault(level, works, c);
			}
			if (mark == null && next != null) {
				mode = Mode.TRIAL;
				trial = next;
				mark = next.mark();
				begun = true;
			}
			nearest = mark == null ? 0 : npc.position().distanceTo(Vec3.atBottomCenterOf(mark));
		}

		/** The nearest of its realm's vaults still sealed that its realm can force. */
		private BlockPos nearestVault(ServerLevel level, RealmWorks works, BlockPos c) {
			BlockPos best = null;
			double d = Double.MAX_VALUE;
			for (BlockPos at : works.vaults(level.getSeed(), c)) {
				if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) || vault.isBroken()) continue;
				if (npc.getRank() < PlayerCultivation.rank(vault.tier().seal(), Stage.EARLY)) continue;
				double here = at.distSqr(npc.blockPosition());
				if (here < d) {
					d = here;
					best = at;
				}
			}
			return best;
		}

		/** Toward {@code to}: low through the air if it can fly (never over the realm's sky-hold, nor where it must walk), else on foot. */
		private void go(ServerLevel level, Vec3 to, double d, boolean onFoot) {
			if (d > 8 && npc.canFlySustainably() && !onFoot) {
				if (!npc.isQiFlying()) npc.setQiFlying(true);
				// Over a floating realm's void, held by its land's level, not the void's floor (else it would sink into the void between shards).
				int ground = SecretRealms.groundUnder(level, SecretRealm.at(npc.getX(), npc.getZ()), npc.getX(), npc.getZ());
				double y = Math.min(Math.max(to.y + 2, ground + 2), ground + SecretRealms.CEILING - 1);
				npc.flyToward(new Vec3(to.x, y, to.z), 0.45);
				return;
			}
			if (npc.isQiFlying() && (d < 6 || onFoot)) npc.setQiFlying(false); // down for a step that must be walked (else it hovers over it)
			if (npc.tickCount % 20 == 0 || npc.getNavigation().isDone()) {
				Vec3 step = d > 24 ? npc.position().add(to.subtract(npc.position()).normalize().scale(24)) : to;
				npc.getNavigation().moveTo(step.x, d > 24 ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(step.x), Mth.floor(step.z)) : step.y,
						step.z, 1.0);
			}
		}

		/** At its mark: what lay there taken and carried; a vault forced and some of it taken; a step of its trials. */
		private void act(ServerLevel level, SecretRealms.Record r) {
			switch (mode) {
				case PICK -> {
					List<ItemStack> got = r.realm.works().rivalTake(level, mark, npc);
					for (ItemStack stack : got) npc.addWinnings(stack);
					if (!got.isEmpty()) npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
					picked++;
					mark = null;
				}
				case VAULT -> {
					if (!(level.getBlockEntity(mark) instanceof StorageFormationBlockEntity vault) || vault.isBroken()) {
						mark = null;
						return;
					}
					if (working++ == 0) RealmWorks.wakeAround(level, mark, npc);
					if (working % 20 == 0) level.sendParticles(ParticleTypes.ENCHANT, mark.getX() + 0.5, mark.getY() + 1.2, mark.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.2);
					if (working >= vault.tier().forceSeconds() * 20) {
						vault.breakSeal();
						StorageSpaces.Space held = StorageSpaces.forServer(level.getServer()).existing(vault.spaceId());
						if (held != null) {
							for (int i = 0; i < 2 && !held.entries().isEmpty(); i++) {
								ItemStack kind = held.entries().get(npc.getRandom().nextInt(held.entries().size())).kind();
								npc.addWinnings(held.extract(kind, kind.getMaxStackSize()));
							}
							StorageSpaces.forServer(level.getServer()).setDirty();
						}
						level.playSound(null, mark, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 0.8f);
						mark = null;
					}
				}
				case TRIAL -> {
					if (r.realm.works().rivalAtTrial(level, r, npc, trial, working++)) mark = null;
				}
			}
		}
	}

	/** {@code id} took the Great Inheritance: so marked on the roll (it stays in the world when the realm closes). */
	static void markHeir(ServerLevel level, SecretRealms.Record r, UUID id) {
		ListTag roll = roll(r);
		for (int i = 0; i < roll.size(); i++) {
			CompoundTag m = roll.getCompound(i);
			if (m.hasUUID("Id") && m.getUUID("Id").equals(id)) m.putBoolean("Inheritor", true);
		}
		SecretRealms.Data.get(level.getServer()).setDirty();
	}

	private SecretRealmRivals() {}
}
