package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The secret realms (2026-10-31, the design the user approved with two changes: "no upper limit for who can enter, just a lower limit so
 * people too weak can't enter"; "flight should be allowed for anyone vastly powerful compared to the secret realm's recommended level").
 * <ul>
 *   <li><b>The Seal.</b> Each realm (SecretRealm) has one in its world: a ring of old pillars round a paved floor on the land's own level,
 *   and in its middle a stone gate whose keystone is carved with the realm's seal (SecretRealmSealBlock). The Overworld's lie
 *   {@link #SITE_NEAR} to {@link #SITE_FAR} blocks from the world's spawn, built the first time the server starts ({@link #ensureSeals}).
 *   Its keystone, or its gate, tells when the realm next opens, whom it admits and who took its inheritance.</li>
 *   <li><b>The cycle</b>, by the world's own clock: every SecretRealm#years years (in-game days) it opens. Half a day before
 *   (SecretRealm#STIR_TICKS) it stirs: a pillar of light rises from its Seal, everyone who may enter is told which way it lies and roughly how
 *   far (never where), the sects near it send their disciples, and its land is made ready (laid the first time, mended, renewed:
 *   RealmWorks). Then its gate fills with light (SecretRealmGateBlock) for the first SecretRealm#GATE_SHARE-th of its time; inside, one
 *   has SecretRealm#insideTicks in all, a row on the HUD counting down. Two minutes before the end it quakes; at the end all still inside are
 *   cast out beside its Seal, with what they carry.</li>
 *   <li><b>Who may enter</b>: its lowest realm and above, by one's higher path's true rank (no upper limit); only the living, in their own
 *   bodies, bearing no one in a sleeve or a world.</li>
 *   <li><b>The law inside.</b> The law of its world holds (Realm Suppression in a lower realm's: {@link #lawOf}), its qi
 *   {@link #QI} times that world's. The sky presses down: one flies no higher than {@link #CEILING} blocks over the ground, unless they stand
 *   SecretRealm#FREE_FLIGHT_REALMS whole realms above the realm it is meant for (SecretRealm#fliesFreely). Space is sealed: no Spatial Tear,
 *   small world's door or sleeve; one leaves early only by a Departure Jade ({@link #depart}). The sects' law doesn't reach inside.</li>
 *   <li><b>The dead</b> lie inside (Corpses) until someone finds them: nothing the realm mends takes them up (RealmLedger).</li>
 *   <li>Logging out inside, one comes back beside its Seal; a server stopped while a realm stood open ends that opening.</li>
 * </ul>
 * The gamerule {@code secretRealms} (ModGameRules#SECRET_REALMS) stops them all. Kept with the world ({@link Data}).
 */
public final class SecretRealms {
	/** How dense the qi is inside, against the world its Seal stands in. */
	public static final double QI = 3.0;
	/** The sky's hold: one flies at most this high over the ground there (unless one flies freely). */
	public static final int CEILING = 6;
	/** Holding a Departure Jade so long (ticks) carries one out. */
	public static final int DEPARTURE_TICKS = 100;
	/** The Overworld's Seals lie this far from its spawn, at least and at most; and at least {@link #SITE_APART} from one another. */
	public static final int SITE_NEAR = 600, SITE_FAR = 2500, SITE_APART = 400;
	/** The Netherworld's (the Yellow Springs Hollow's) this far from the Ghost Gate, where the dead wake: within a ghost's walk. */
	public static final int NETHER_SITE_NEAR = 160, NETHER_SITE_FAR = 640;
	/** A Seal's paved floor reaches so far from its gate; its pillars stand so far out. */
	public static final int SEAL_RADIUS = 8, PILLAR_RING = 7;
	/** Within this of a Seal, its HUD row shows how it stands (stirring, its gate open). */
	public static final int NEAR_SEAL = 96;
	/** The first opening of a new world comes this long after it begins, give or take another as much (its own roll). */
	static final long FIRST_OPENING = 24_000L;
	/** The game tests run the cycle and build Seals only when they ask for it (their world is kept between runs). */
	public static boolean inTests;

	// --- Where ---

	/** The realm (and its slot) the secret realms' level {@code level} holds at {@code pos}, or null anywhere else. */
	public static SecretRealm realmAt(Level level, double x, double z) {
		return ModDimensions.isSecretRealm(level.dimension()) ? SecretRealm.at(x, z) : null;
	}

	/** The realm {@code p} stands in, or null. */
	public static SecretRealm realmOf(ServerPlayer p) {
		return realmAt(p.level(), p.getX(), p.getZ());
	}

	/**
	 * The world whose law holds where {@code level}'s {@code pos} is: in a secret realm, the world its Seal stands in (its suppression, its
	 * qi); anywhere else the level's own.
	 */
	public static ResourceKey<Level> lawOf(Level level, BlockPos pos) {
		SecretRealm realm = realmAt(level, pos.getX(), pos.getZ());
		return realm != null ? realm.home() : level.dimension();
	}

	/** How dense the qi is about {@code pos} in {@code level}, against the lower realms' (RealmSuppressionHandler#realmQi; a realm's {@link #QI} times its world's). */
	public static double qiAt(Level level, BlockPos pos) {
		SecretRealm realm = realmAt(level, pos.getX(), pos.getZ());
		ResourceKey<Level> world = realm != null ? realm.home() : level.dimension();
		double base = ModDimensions.isUpperRealm(world) ? PlayerCultivation.UPPER_REALM_QI_MULTIPLIER : RealmSuppressionHandler.realmQi(world);
		return realm != null ? base * QI : base;
	}

	public static boolean enabled(MinecraftServer server) {
		return server.getGameRules().getBoolean(ModGameRules.SECRET_REALMS) && (!(server instanceof GameTestServer) || inTests)
				&& server.getLevel(ModDimensions.SECRET_REALM) != null;
	}

	// --- The cycle ---

	/** Where a realm stands in its cycle. */
	public enum Phase { SLEEPING, STIRRING, OPEN, CLOSING }

	public static Phase phase(Record r, long now) {
		if (r.openedAt >= 0) return now >= r.openedAt + r.realm.insideTicks() - SecretRealm.QUAKE_TICKS ? Phase.CLOSING : Phase.OPEN;
		return r.stirred ? Phase.STIRRING : Phase.SLEEPING;
	}

	/** Whether {@code r}'s gate stands open now: the first SecretRealm#gateTicks of an opening. */
	public static boolean gateOpen(Record r, long now) {
		return r.openedAt >= 0 && now < r.openedAt + r.realm.gateTicks();
	}

	/** Ticks until {@code r} next opens (0 while open). */
	public static long untilOpen(Record r, long now) {
		return r.openedAt >= 0 ? 0 : Math.max(0, r.nextOpen - now);
	}

	/** Ticks until the opening under way ends (0 while shut). */
	public static long untilClose(Record r, long now) {
		return r.openedAt < 0 ? 0 : Math.max(0, r.openedAt + r.realm.insideTicks() - now);
	}

	public static void tick(MinecraftServer server) {
		if (server.getLevel(ModDimensions.SECRET_REALM) == null) return;
		Data data = Data.get(server);
		long now = server.overworld().getGameTime();
		boolean on = enabled(server);
		for (SecretRealm realm : SecretRealm.values()) {
			Record r = data.of(realm);
			try {
				if (on) cycle(server, data, r, now);
				else if (r.openedAt >= 0 || r.stirred) stop(server, data, r, now);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Secret realm {} failed its tick", realm.getId(), e);
			}
		}
		RealmWorks.work(server, data);
		ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
		for (ServerPlayer p : new ArrayList<>(realms.players())) {
			try {
				holdInside(p, data, now);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Secret realm rules failed for {}", p.getScoreboardName(), e);
			}
		}
		if (server.getTickCount() % 20 == 3) {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) hud(p, data, now);
		}
		for (Record r : data.records.values()) {
			if (r.openedAt >= 0) {
				RealmWorks.tickOpen(server, data, r, now);
				SecretRealmRivals.tickOpen(server, data, r, now);
			} else if (r.stirred) {
				SecretRealmRivals.tickStirring(server, data, r, now);
			}
			SecretRealmRivals.tickAmbush(server, data, r, now);
		}
	}

	private static void cycle(MinecraftServer server, Data data, Record r, long now) {
		if (r.nextOpen < 0) {
			RandomSource random = RandomSource.create(server.overworld().getSeed() ^ r.realm.ordinal() * 0x51A7L);
			r.nextOpen = now + FIRST_OPENING + random.nextInt((int) FIRST_OPENING);
			data.setDirty();
		}
		if (r.openedAt < 0) {
			if (!r.stirred && now >= r.nextOpen - SecretRealm.STIR_TICKS) stir(server, data, r, now);
			if (r.stirred && now >= r.nextOpen && RealmWorks.ready(r)) open(server, data, r, now);
			return;
		}
		long end = r.openedAt + r.realm.insideTicks();
		if (r.gateLit && !gateOpen(r, now)) {
			r.gateLit = false;
			setGate(server, r, SecretRealmGateBlock.Phase.STIRRING);
			tellInside(server, r, Component.translatable(ModLang.MSG_SECRET_GATE_SEALED, r.realm.displayName()).withStyle(ChatFormatting.GRAY));
			data.setDirty();
		}
		if (!r.quaking && now >= end - SecretRealm.QUAKE_TICKS) {
			r.quaking = true;
			tellInside(server, r, Component.translatable(ModLang.MSG_SECRET_QUAKES, r.realm.displayName(), SecretRealm.QUAKE_TICKS / 1200).withStyle(ChatFormatting.RED));
			SecretRealmRivals.quaking(server, data, r, now); // the devilish slip out early, to lie in wait at its Seal
			data.setDirty();
		}
		if (now >= end) close(server, data, r, now);
	}

	/** Half a day before it opens: the light rises, those who may enter are told, its land made ready, the sects' disciples set out. */
	static void stir(MinecraftServer server, Data data, Record r, long now) {
		r.stirred = true;
		data.setDirty();
		ensureSeal(server, data, r);
		setGate(server, r, SecretRealmGateBlock.Phase.STIRRING);
		RealmWorks.prepare(server, data, r);
		SecretRealmRivals.stir(server, data, r, now);
		Rumours.noteRealmStirring(r.realm, r.realm.home(), r.seal, now);
		long minutes = Math.max(1, (r.nextOpen - now) / 1200);
		ServerLevel home = server.getLevel(r.realm.home());
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (r.seal == null || home == null || p.level() != home || !mayEnterRank(p, r.realm)) continue;
			Component direction = Component.translatable(ModLang.talkDirection(TalkSystem.direction(p.blockPosition(), r.seal)));
			int rough = Math.max(100, (int) Math.round(Math.sqrt(p.blockPosition().distSqr(r.seal)) / 100) * 100);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_SECRET_STIRS, r.realm.displayName(), direction, rough, minutes).withStyle(ChatFormatting.GOLD));
		}
		DefyingTheHeavens.LOGGER.info("{} stirs; it opens at {}", r.realm.getId(), r.nextOpen);
	}

	/** It opens: the gate fills with light, those near and those who may enter told; the disciples waiting go in. */
	static void open(MinecraftServer server, Data data, Record r, long now) {
		if (!r.stirred) stir(server, data, r, now);
		RealmWorks.finishNow(server, data, r); // a command's opening out of turn: its land made ready at once
		r.openedAt = now;
		r.openings++;
		r.gateLit = true;
		r.quaking = false;
		r.inside.clear();
		r.carriedIn.clear();
		data.setDirty();
		setGate(server, r, SecretRealmGateBlock.Phase.OPEN);
		RealmWorks.opened(server, data, r, now);
		SecretRealmRivals.opened(server, data, r, now);
		int gateMinutes = Math.max(1, r.realm.gateTicks() / 1200);
		ServerLevel home = server.getLevel(r.realm.home());
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			boolean near = home != null && p.level() == home && r.seal != null && p.blockPosition().distSqr(r.seal) < 400 * 400;
			if (near || p.level() == home && mayEnterRank(p, r.realm)) {
				p.sendSystemMessage(Component.translatable(ModLang.MSG_SECRET_OPENS, r.realm.displayName(), gateMinutes).withStyle(ChatFormatting.GOLD));
			}
		}
		if (home != null && r.seal != null && home.isLoaded(r.seal)) {
			home.playSound(null, r.seal, SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 3.0f, 0.8f);
			home.sendParticles(ParticleTypes.END_ROD, r.seal.getX() + 0.5, r.seal.getY() + 2.5, r.seal.getZ() + 0.5, 80, 1.2, 1.6, 0.4, 0.1);
		}
		DefyingTheHeavens.LOGGER.info("{} opens", r.realm.getId());
	}

	/** Its time is up: everyone still in it cast out beside its Seal, its guardians and visitors gone, its gate shut till next time. */
	static void close(MinecraftServer server, Data data, Record r, long now) {
		ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
		if (realms != null) {
			for (ServerPlayer p : new ArrayList<>(realms.players())) {
				if (SecretRealm.at(p.getX(), p.getZ()) == r.realm) castOut(p, r, ModLang.MSG_SECRET_CAST_OUT);
			}
		}
		RealmWorks.closed(server, data, r);
		SecretRealmRivals.closed(server, data, r, now);
		long next = r.openedAt + r.realm.period();
		r.nextOpen = Math.max(next, now + SecretRealm.STIR_TICKS + 1200);
		r.openedAt = -1;
		r.stirred = false;
		r.gateLit = false;
		r.quaking = false;
		r.inside.clear();
		r.carriedIn.clear();
		data.setDirty();
		setGate(server, r, SecretRealmGateBlock.Phase.SEALED);
		DefyingTheHeavens.LOGGER.info("{} closes; it opens again at {}", r.realm.getId(), r.nextOpen);
	}

	/** The gamerule turned off (or the realms' dimension gone): an opening under way ends, a stirring is let go, the gate shut. */
	private static void stop(MinecraftServer server, Data data, Record r, long now) {
		if (r.openedAt >= 0) {
			close(server, data, r, now);
			return;
		}
		r.stirred = false;
		data.setDirty();
		setGate(server, r, SecretRealmGateBlock.Phase.SEALED);
		SecretRealmRivals.closed(server, data, r, now);
	}

	/** A command's: {@code realm} open now (stirred at once if it wasn't, its land readied then and there). */
	public static void openNow(MinecraftServer server, SecretRealm realm) {
		Data data = Data.get(server);
		Record r = data.of(realm);
		long now = server.overworld().getGameTime();
		if (r.openedAt >= 0) return;
		r.nextOpen = now;
		open(server, data, r, now);
	}

	/** A command's: the opening under way ends now. */
	public static void closeNow(MinecraftServer server, SecretRealm realm) {
		Data data = Data.get(server);
		Record r = data.of(realm);
		long now = server.overworld().getGameTime();
		if (r.openedAt >= 0) close(server, data, r, now);
		else if (r.stirred) stop(server, data, r, now);
	}

	/** A command's: {@code realm} stirs now, to open half a day on. */
	public static void stirNow(MinecraftServer server, SecretRealm realm) {
		Data data = Data.get(server);
		Record r = data.of(realm);
		long now = server.overworld().getGameTime();
		if (r.openedAt >= 0 || r.stirred) return;
		r.nextOpen = now + SecretRealm.STIR_TICKS;
		stir(server, data, r, now);
	}

	private static void tellInside(MinecraftServer server, Record r, Component message) {
		ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
		if (realms == null) return;
		for (ServerPlayer p : realms.players()) if (SecretRealm.at(p.getX(), p.getZ()) == r.realm) p.sendSystemMessage(message);
	}

	// --- In and out ---

	/** Players a gate has hold of, carried at the server's next task (as SmallWorlds#carry: never in the middle of their own step). */
	private static final Set<UUID> CARRYING = new HashSet<>();
	/** When each was last told why the gate turned them back. */
	private static final Map<UUID, Long> TOLD = new HashMap<>();

	/** Whether {@code p}'s higher path's true rank lets them into {@code realm} (or they are in creative). */
	public static boolean mayEnterRank(ServerPlayer p, SecretRealm realm) {
		return p.isCreative() || realm.mayEnter(TwoPaths.higherRank(CultivationManager.get(p), false));
	}

	/** Why {@code p} can't go into {@code realm} now, or null if they can. */
	public static Component whyNotEnter(ServerPlayer p, SecretRealm realm) {
		if (Incorporeal.isGhost(p) && !realm.admitsDead()) return Component.translatable(ModLang.MSG_SECRET_LIVING_ONLY, realm.displayName());
		if (SoulProjection.isActive(p.getUUID()) || Possession.inhabits(p.getUUID()) || Clones.inClone(p)) {
			return Component.translatable(ModLang.MSG_SECRET_OWN_BODY);
		}
		if (Sleeves.carrying(p.getUUID()) > 0 || SmallWorlds.inside(p) > 0) return Component.translatable(ModLang.MSG_SECRET_BEARING);
		if (TribulationManager.isActive(p.getUUID())) return Component.translatable(ModLang.MSG_SECRET_BUSY);
		if (!mayEnterRank(p, realm)) return Component.translatable(ModLang.MSG_SECRET_TOO_WEAK, realm.displayName(), realm.lowestName());
		return null;
	}

	/** {@code p} walked into an open gate (SecretRealmGateBlock) at {@code gate}: carried in if they may go, else turned back from it. */
	public static void atGate(ServerPlayer p, BlockPos gate) {
		if (p.isSpectator() || !p.isAlive() || CARRYING.contains(p.getUUID())) return;
		Data data = Data.get(p.server);
		Record r = data.atSeal(p.level().dimension(), gate);
		long now = p.server.overworld().getGameTime();
		if (r == null || !gateOpen(r, now)) return;
		Component why = whyNotEnter(p, r.realm);
		if (why != null) {
			Long told = TOLD.get(p.getUUID());
			if (told == null || now - told > 60) {
				TOLD.put(p.getUUID(), now);
				p.displayClientMessage(why.copy().withStyle(ChatFormatting.RED), true);
			}
			Vec3 away = p.position().subtract(Vec3.atCenterOf(gate)).multiply(1, 0, 1);
			if (away.lengthSqr() < 1.0e-4) away = new Vec3(0, 0, 1);
			p.setDeltaMovement(away.normalize().scale(0.7).add(0, 0.2, 0));
			p.hurtMarked = true;
			return;
		}
		Level from = p.level();
		CARRYING.add(p.getUUID());
		p.server.tell(new TickTask(p.server.getTickCount(), () -> {
			CARRYING.remove(p.getUUID());
			if (p.isRemoved() || !p.isAlive() || p.level() != from) return;
			try {
				enter(p, r);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("{} could not be carried into {}", p.getScoreboardName(), r.realm.getId(), e);
			}
		}));
	}

	/** {@code p} into {@code r}'s land, set down on one of its arrival terraces (the one with the fewest about it), told what holds there. */
	public static void enter(ServerPlayer p, Record r) {
		ServerLevel realms = p.server.getLevel(ModDimensions.SECRET_REALM);
		if (realms == null) return;
		Data data = Data.get(p.server);
		TribulationManager.abandon(p);
		MeditationManager.stop(p, false);
		p.stopRiding();
		BlockPos at = RealmWorks.arrival(realms, r, p.getRandom());
		Vec3 inward = Vec3.atBottomCenterOf(r.realm.centre()).subtract(Vec3.atBottomCenterOf(at)).multiply(1, 0, 1);
		float yaw = (float) Math.toDegrees(Math.atan2(-inward.x, inward.z));
		ServerLevel was = p.serverLevel();
		was.playSound(null, p.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.7f);
		realms.getChunk(at.getX() >> 4, at.getZ() >> 4);
		p.teleportTo(realms, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, yaw, 0);
		p.connection.resetPosition();
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
		r.inside.add(p.getUUID());
		r.carriedIn.put(p.getUUID(), carried(p));
		data.setDirty();
		long left = untilClose(r, p.server.overworld().getGameTime());
		p.sendSystemMessage(Component.translatable(ModLang.MSG_SECRET_ENTERED, r.realm.displayName(), (int) QI, Math.max(1, left / 1200)).withStyle(ChatFormatting.GOLD));
		realms.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.7f);
		realms.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
		Milestones.award(p, Milestones.SECRET_ENTERED);
		SecretRealmRivals.playerEntered(p.server, data, r);
		hud(p, data, p.server.overworld().getGameTime());
	}

	/**
	 * {@code p} out of {@code r} beside its Seal (or, that unknown, at their spawn), told {@code why}; got out alive with more than they
	 * brought in, their milestone.
	 */
	public static void castOut(ServerPlayer p, Record r, String why) {
		Data data = Data.get(p.server);
		boolean wasIn = r.inside.remove(p.getUUID());
		Integer brought = r.carriedIn.remove(p.getUUID());
		data.setDirty();
		p.stopRiding();
		if (p.isUsingItem()) p.stopUsingItem();
		ServerLevel home = p.server.getLevel(r.realm.home());
		if (home == null || r.seal == null) {
			ServerLevel spawn = p.server.overworld();
			BlockPos at = spawn.getSharedSpawnPos();
			p.teleportTo(spawn, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, p.getYRot(), p.getXRot());
		} else {
			home.getChunk(r.seal.getX() >> 4, r.seal.getZ() >> 4);
			Vec3 at = besideSeal(home, r.seal, p.getRandom());
			p.teleportTo(home, at.x, at.y, at.z, 180, 0);
		}
		p.connection.resetPosition();
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
		p.sendSystemMessage(Component.translatable(why, r.realm.displayName()).withStyle(ChatFormatting.GOLD));
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.9f);
		boolean laden = wasIn && brought != null && p.isAlive() && carried(p) > brought;
		if (laden) Milestones.award(p, Milestones.SECRET_OUT_ALIVE);
		if (home != null && p.level() == home) SecretRealmRivals.cameOut(home, r, p, laden); // those lying in wait at the Seal, set on the laden
		HUD_SHOWN.remove(p.getUUID());
		ModPackets.sendSecretRealm(p, -1, HUD_NONE, 0);
	}

	/** A spot on a Seal's floor before its gate (its own south side), on the ground there. */
	static Vec3 besideSeal(ServerLevel home, BlockPos seal, RandomSource random) {
		double a = (random.nextDouble() - 0.5) * Math.PI * 0.9;
		double d = 3 + random.nextDouble() * 3;
		double x = seal.getX() + 0.5 + Math.sin(a) * d, z = seal.getZ() + 0.5 + Math.cos(a) * d;
		int y = home.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
		return new Vec3(x, Math.max(y, seal.getY() + 1), z);
	}

	/** Everything {@code p} carries, counted (to tell whether they came out with more than they took in). */
	static int carried(ServerPlayer p) {
		int n = 0;
		Inventory inventory = p.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty()) n += stack.getCount();
		}
		return n;
	}

	/** A Departure Jade held its time inside (DepartureJadeItem): out beside the realm's Seal. @return whether they were carried out */
	public static boolean depart(ServerPlayer p) {
		SecretRealm realm = realmOf(p);
		if (realm == null) return false;
		Record r = Data.get(p.server).of(realm);
		castOut(p, r, ModLang.MSG_SECRET_DEPARTED);
		return true;
	}

	// --- Inside ---

	/** When each was last told the sky pressed them down, or the realm's edge turned them back. */
	private static final Map<UUID, Long> PRESSED = new HashMap<>();

	/**
	 * Each tick for one in the secret realms: out if no realm holds them there (or theirs is shut); set back on its land if they fall off it or
	 * stray past its edge; and, flying, held under the sky's hold unless they fly freely.
	 */
	private static void holdInside(ServerPlayer p, Data data, long now) {
		if (!p.isAlive() || CARRYING.contains(p.getUUID()) || Possession.isHeld(p.getUUID()) || SoulProjection.isAwayFromBody(p.getUUID())) return;
		boolean looking = p.isSpectator() && p.hasPermissions(2); // an operator's look about
		if (looking) return;
		SecretRealm realm = realmOf(p);
		Record r = realm == null ? null : data.of(realm);
		if (r == null || r.openedAt < 0 && !p.isCreative()) {
			castOut(p, r != null ? r : data.of(SecretRealm.VERDANT_HERB_VALLEY), ModLang.MSG_SECRET_CAST_OUT);
			return;
		}
		ServerLevel level = p.serverLevel();
		if (realm.distance(p.getX(), p.getZ()) > SecretRealm.BOUND || p.getY() < level.getMinBuildHeight() + 24) {
			BlockPos back = RealmWorks.nearestArrival(level, r, p.blockPosition());
			p.teleportTo(level, back.getX() + 0.5, back.getY() + 1, back.getZ() + 0.5, p.getYRot(), p.getXRot());
			p.setDeltaMovement(Vec3.ZERO);
			p.fallDistance = 0;
			tellPressed(p, now, Component.translatable(ModLang.MSG_SECRET_CAUGHT, realm.displayName()));
			return;
		}
		if (p.isCreative()) return;
		boolean flying = p.getAbilities().flying || p.isFallFlying();
		if (!flying || realm.fliesFreely(TwoPaths.higherRank(CultivationManager.get(p), false))) return;
		int ground = groundUnder(level, realm, p.getX(), p.getZ());
		double over = p.getY() - ground;
		if (over <= CEILING) return;
		// Over the sky's hold: pressed down (a long way over, set at it at once).
		if (over > CEILING + 3) {
			p.connection.teleport(p.getX(), ground + CEILING, p.getZ(), p.getYRot(), p.getXRot());
		} else {
			Vec3 v = p.getDeltaMovement();
			p.setDeltaMovement(v.x, Math.min(v.y, -0.5), v.z);
			p.hurtMarked = true;
		}
		p.fallDistance = 0;
		tellPressed(p, now, Component.translatable(ModLang.MSG_SECRET_PRESSED, realm.displayName()));
	}

	/**
	 * The ground the sky's hold is measured from at ({@code x}, {@code z}) in {@code realm}: its land's top there; over a floating realm's
	 * void (between the Ruins' shards, off the Tomb's base) its land's level (SecretRealm#holdFloor), not the void's floor.
	 */
	static int groundUnder(ServerLevel level, SecretRealm realm, double x, double z) {
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
		return realm == null ? ground : Math.max(ground, realm.holdFloor());
	}

	private static void tellPressed(ServerPlayer p, long now, Component message) {
		Long told = PRESSED.get(p.getUUID());
		if (told != null && now - told < 100) return;
		PRESSED.put(p.getUUID(), now);
		p.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
	}

	/** What a realm's HUD row shows (ModPackets#sendSecretRealm). */
	public static final int HUD_NONE = 0, HUD_STIRRING = 1, HUD_GATE = 2, HUD_INSIDE = 3, HUD_QUAKING = 4;

	/** Each second: {@code p}'s HUD row: inside, the time left (quaking at the end); near a Seal stirring, how long till it opens; its gate open, how long it stays. */
	private static void hud(ServerPlayer p, Data data, long now) {
		SecretRealm in = realmOf(p);
		if (in != null) {
			Record r = data.of(in);
			if (r.openedAt >= 0) {
				show(p, in, phase(r, now) == Phase.CLOSING ? HUD_QUAKING : HUD_INSIDE, untilClose(r, now));
				return;
			}
		}
		for (Record r : data.records.values()) {
			if (r.seal == null || p.level().dimension() != r.realm.home() || p.blockPosition().distSqr(r.seal) > NEAR_SEAL * NEAR_SEAL) continue;
			if (gateOpen(r, now)) {
				show(p, r.realm, HUD_GATE, r.openedAt + r.realm.gateTicks() - now);
				return;
			}
			if (r.stirred && r.openedAt < 0) {
				show(p, r.realm, HUD_STIRRING, untilOpen(r, now));
				return;
			}
		}
		if (HUD_SHOWN.remove(p.getUUID())) ModPackets.sendSecretRealm(p, -1, HUD_NONE, 0);
	}

	private static void show(ServerPlayer p, SecretRealm realm, int kind, long ticks) {
		HUD_SHOWN.add(p.getUUID());
		ModPackets.sendSecretRealm(p, realm.ordinal(), kind, (int) Math.min(Integer.MAX_VALUE, ticks));
	}

	/** Players whose row is showing (to take it away once, when there's nothing to show). */
	static final Set<UUID> HUD_SHOWN = new HashSet<>();

	// --- Comings and goings ---

	/** Back in the game in a secret realm (logged out there, or the server stopped): beside its Seal. */
	public static void onJoin(ServerPlayer p) {
		if (!ModDimensions.isSecretRealm(p.level().dimension()) || p.isSpectator() && p.hasPermissions(2)) return;
		Data data = Data.get(p.server);
		SecretRealm realm = realmOf(p);
		Record r = data.of(realm != null ? realm : SecretRealm.VERDANT_HERB_VALLEY);
		castOut(p, r, ModLang.MSG_SECRET_BACK_AT_SEAL);
	}

	public static void onDisconnect(ServerPlayer p) {
		CARRYING.remove(p.getUUID());
		TOLD.remove(p.getUUID());
		PRESSED.remove(p.getUUID());
		HUD_SHOWN.remove(p.getUUID());
	}

	/** {@code p} died in a secret realm: no longer in it; their body lies there (Corpses), and the realm's tales tell of it. */
	public static void onDeath(ServerPlayer p) {
		SecretRealm realm = realmOf(p);
		if (realm == null) return;
		Data data = Data.get(p.server);
		Record r = data.of(realm);
		r.inside.remove(p.getUUID());
		r.carriedIn.remove(p.getUUID());
		data.setDirty();
		Rumours.noteRealmFallen(realm, p.getScoreboardName(), p.serverLevel().getGameTime());
		ModPackets.sendSecretRealm(p, -1, HUD_NONE, 0);
	}

	/**
	 * The server started: the Seals built that aren't yet; an opening the last server stopped in the middle of ends (those left in it come
	 * out beside its Seal as they log in), a stirring goes on.
	 */
	public static void onServerStarted(MinecraftServer server) {
		if (server.getLevel(ModDimensions.SECRET_REALM) == null) return;
		RealmLedger.get(server);
		Data data = Data.get(server);
		long now = server.overworld().getGameTime();
		for (SecretRealm realm : SecretRealm.values()) {
			Record r = data.of(realm);
			try {
				if (r.openedAt >= 0) {
					r.nextOpen = Math.max(r.openedAt + realm.period(), now + SecretRealm.STIR_TICKS + 1200);
					r.openedAt = -1;
					r.stirred = false;
					r.gateLit = false;
					r.quaking = false;
					r.inside.clear();
					r.carriedIn.clear();
					data.setDirty();
				}
				if (enabled(server)) {
					ensureSeal(server, data, r);
					setGate(server, r, r.stirred ? SecretRealmGateBlock.Phase.STIRRING : SecretRealmGateBlock.Phase.SEALED);
					if (r.stirred) RealmWorks.prepare(server, data, r); // its land's readying goes on
				}
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Secret realm {} could not be set up", realm.getId(), e);
			}
		}
	}

	public static void clear() {
		CARRYING.clear();
		TOLD.clear();
		PRESSED.clear();
		HUD_SHOWN.clear();
		RealmLedger.clear();
		RealmWorks.clear();
		SecretRealmRivals.clear();
		SpringsHollowWorks.clear();
		FallenHolyLandWorks.clear();
	}

	// --- The Seal ---

	/** Its Seal's place chosen (the first time) and built (if it isn't yet). */
	static void ensureSeal(MinecraftServer server, Data data, Record r) {
		ServerLevel home = server.getLevel(r.realm.home());
		if (home == null) return;
		if (r.seal == null) {
			r.seal = site(home, r.realm, data);
			data.setDirty();
		}
		if (!r.sealBuilt) {
			long start = System.currentTimeMillis();
			int y = buildSeal(home, r.seal, r.realm);
			r.seal = new BlockPos(r.seal.getX(), y, r.seal.getZ());
			r.sealBuilt = true;
			data.setDirty();
			DefyingTheHeavens.LOGGER.info("The Seal of {} built at {} in {} ms", r.realm.getId(), r.seal, System.currentTimeMillis() - start);
		}
	}

	/**
	 * Where {@code realm}'s Seal stands in {@code home} (the world's own roll, the same every time): {@link #SITE_NEAR} to {@link #SITE_FAR}
	 * from its spawn on dry, level land, asked of the generator without a chunk made (as a /locate asks); clear of the sects founded there,
	 * and {@link #SITE_APART} from the other realms' Seals there (so the light of one stirring is never taken for another's).
	 */
	static BlockPos site(ServerLevel home, SecretRealm realm, Data data) {
		RandomSource random = RandomSource.create(home.getSeed() ^ (realm.ordinal() + 1) * 0x5EA15EA1L);
		// The Netherworld's: about the Ghost Gate, where the dead wake, within a ghost's walk.
		boolean nether = ModDimensions.isNetherworld(home.dimension());
		BlockPos spawn = nether ? new BlockPos(GhostGate.X, 0, GhostGate.Z) : home.getSharedSpawnPos();
		int near = nether ? NETHER_SITE_NEAR : SITE_NEAR, far = nether ? NETHER_SITE_FAR : SITE_FAR;
		var generator = home.getChunkSource().getGenerator();
		var state = home.getChunkSource().randomState();
		// A world with no seas, its "sea level" at its floor (the Jade Firmament's islands, the Great Void's continents, the Immortal
		// Realm's peaks): its land floats, the void between; it is looked over the longer, and never stood in the void.
		boolean floating = home.getSeaLevel() <= home.getMinBuildHeight();
		BlockPos best = null;
		int bestRough = Integer.MAX_VALUE;
		for (int tries = 0; tries < (floating ? 64 : 24); tries++) {
			double a = random.nextDouble() * Math.PI * 2, d = near + random.nextDouble() * (far - near);
			int x = spawn.getX() + (int) Math.round(Math.cos(a) * d), z = spawn.getZ() + (int) Math.round(Math.sin(a) * d);
			int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, home, state);
			int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, home, state);
			if (surface != floor || surface <= home.getSeaLevel()) continue; // under water
			if (nearSect(home, x, z) || nearSeal(home, realm, data, x, z)) continue;
			int rough = 0;
			for (int i = 0; i < 8; i++) {
				double b = i * Math.PI / 4;
				int h = generator.getBaseHeight(x + (int) Math.round(Math.cos(b) * SEAL_RADIUS), z + (int) Math.round(Math.sin(b) * SEAL_RADIUS),
						Heightmap.Types.OCEAN_FLOOR_WG, home, state);
				rough += Math.abs(h - surface);
			}
			if (rough < bestRough) {
				bestRough = rough;
				best = new BlockPos(x, surface, z);
			}
			if (rough <= 6) break;
		}
		if (best == null) {
			int x = spawn.getX() + near + (nether ? 0 : realm.ordinal() * SITE_APART), z = spawn.getZ();
			if (floating) {
				BlockPos land = SpatialArrivals.target(home, x, z); // the nearest island's or continent's land to it
				int lx = land.getX(), lz = land.getZ(), y = generator.getBaseHeight(lx, lz, Heightmap.Types.WORLD_SURFACE_WG, home, state);
				// Still the void (the Immortal Realm's, which that doesn't look over): out in rings till land is found.
				for (int out = 16; y <= home.getMinBuildHeight() + 1 && out <= 480; out += 16) {
					for (int k = 0; k < 16 && y <= home.getMinBuildHeight() + 1; k++) {
						lx = x + (int) Math.round(Math.cos(k * Math.PI / 8) * out);
						lz = z + (int) Math.round(Math.sin(k * Math.PI / 8) * out);
						y = generator.getBaseHeight(lx, lz, Heightmap.Types.WORLD_SURFACE_WG, home, state);
					}
				}
				best = new BlockPos(lx, y, lz);
			} else {
				best = new BlockPos(x, home.getSeaLevel() + 1, z);
			}
		}
		return best;
	}

	private static boolean nearSeal(ServerLevel home, SecretRealm realm, Data data, int x, int z) {
		for (Record other : data.records.values()) {
			if (other.realm == realm || other.seal == null || other.realm.home() != home.dimension()) continue;
			if (Math.hypot(other.seal.getX() - x, other.seal.getZ() - z) < SITE_APART) return true;
		}
		return false;
	}

	private static boolean nearSect(ServerLevel home, int x, int z) {
		for (Sect sect : SectData.get(home.getServer()).all()) {
			if (sect.dimension == home.dimension() && sect.core.distSqr(new BlockPos(x, sect.core.getY(), z)) < 320 * 320) return true;
		}
		return false;
	}

	/**
	 * Builds a Seal whose gate's middle is at {@code at} (its column; the floor laid at the land's own level there): a floor of old paving
	 * {@link #SEAL_RADIUS} out, ringed with polished stone; eight pillars round it, some broken, two lanterned; in its middle a gate of dark
	 * stone across x, its opening three wide and four high (SecretRealmGateBlock), its keystone the realm's seal (SecretRealmSealBlock), a little
	 * roof over it. The land about it cleared of trees. @return the floor's level
	 */
	static int buildSeal(ServerLevel level, BlockPos at, SecretRealm realm) {
		int x = at.getX(), z = at.getZ();
		for (int cx = (x - 20) >> 4; cx <= (x + 20) >> 4; cx++) {
			for (int cz = (z - 20) >> 4; cz <= (z + 20) >> 4; cz++) level.getChunk(cx, cz);
		}
		List<Integer> heights = new ArrayList<>();
		for (int dx = -SEAL_RADIUS; dx <= SEAL_RADIUS; dx += 2) {
			for (int dz = -SEAL_RADIUS; dz <= SEAL_RADIUS; dz += 2) {
				if (dx * dx + dz * dz <= SEAL_RADIUS * SEAL_RADIUS) heights.add(level.getHeight(Heightmap.Types.OCEAN_FLOOR, x + dx, z + dz) - 1);
			}
		}
		Collections.sort(heights);
		int y = heights.get(heights.size() / 2);
		RandomSource random = RandomSource.create(level.getSeed() ^ realm.ordinal() * 0x7ECL);
		BlockState air = Blocks.AIR.defaultBlockState();
		// Cleared round it (trees, plants, snow), and filled under the floor down to the ground.
		for (int dx = -SEAL_RADIUS - 4; dx <= SEAL_RADIUS + 4; dx++) {
			for (int dz = -SEAL_RADIUS - 4; dz <= SEAL_RADIUS + 4; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > SEAL_RADIUS + 4.5) continue;
				for (int dy = 1; dy <= 16; dy++) {
					BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
					BlockState here = level.getBlockState(pos);
					if (d <= SEAL_RADIUS + 0.5 || !here.isAir() && (here.canBeReplaced() || here.is(net.minecraft.tags.BlockTags.LEAVES) || here.is(net.minecraft.tags.BlockTags.LOGS))) {
						if (!here.isAir()) level.setBlock(pos, air, Block.UPDATE_CLIENTS);
					}
				}
				if (d > SEAL_RADIUS + 0.5) continue;
				for (int dy = 0; dy >= -12; dy--) {
					BlockPos pos = new BlockPos(x + dx, y + dy, z + dz);
					BlockState here = level.getBlockState(pos);
					if (dy < 0 && !here.canBeReplaced()) break;
					level.setBlock(pos, dy == 0 ? paving(d, random) : Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
		// The pillars: eight round the floor, some whole, some broken off.
		for (int i = 0; i < 8; i++) {
			double b = i * Math.PI / 4 + Math.PI / 8;
			int px = x + (int) Math.round(Math.cos(b) * PILLAR_RING), pz = z + (int) Math.round(Math.sin(b) * PILLAR_RING);
			int height = 3 + random.nextInt(4);
			for (int dy = 1; dy <= height; dy++) {
				BlockState stone = dy == height ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
						: (random.nextFloat() < 0.35 ? Blocks.MOSSY_STONE_BRICKS : random.nextFloat() < 0.2 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
				level.setBlock(new BlockPos(px, y + dy, pz), stone, Block.UPDATE_CLIENTS);
			}
			if (i % 4 == 1) level.setBlock(new BlockPos(px, y + height + 1, pz), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), Block.UPDATE_CLIENTS);
		}
		// The gate: piers of dark stone either side of its opening, its lintel and keystone, a roof of tiles over it.
		BlockState pier = Blocks.POLISHED_DEEPSLATE.defaultBlockState(), cap = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
		BlockState lintel = Blocks.DEEPSLATE_TILES.defaultBlockState();
		for (int dy = 1; dy <= 5; dy++) {
			level.setBlock(new BlockPos(x - 2, y + dy, z), dy == 5 ? cap : pier, Block.UPDATE_CLIENTS);
			level.setBlock(new BlockPos(x + 2, y + dy, z), dy == 5 ? cap : pier, Block.UPDATE_CLIENTS);
		}
		for (int dx = -1; dx <= 1; dx++) level.setBlock(new BlockPos(x + dx, y + 5, z), dx == 0 ? ModBlocks.SECRET_REALM_SEAL.defaultBlockState() : lintel, Block.UPDATE_CLIENTS);
		for (int dx = -3; dx <= 3; dx++) {
			level.setBlock(new BlockPos(x + dx, y + 6, z), Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), Block.UPDATE_CLIENTS);
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = 1; dy <= 4; dy++) level.setBlock(new BlockPos(x + dx, y + dy, z), ModBlocks.SECRET_REALM_GATE.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		if (level.getBlockEntity(new BlockPos(x, y + 5, z)) instanceof SecretRealmSealBlockEntity seal) seal.setRealm(realm);
		// Its threshold, a step of dark stone either side.
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -1; dz <= 1; dz += 2) level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
		return y;
	}

	private static BlockState paving(double d, RandomSource random) {
		if (d > SEAL_RADIUS - 0.5) return Blocks.POLISHED_ANDESITE.defaultBlockState();
		if (Math.abs(d - 4.5) < 0.5) return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
		float roll = random.nextFloat();
		return (roll < 0.3 ? Blocks.MOSSY_STONE_BRICKS : roll < 0.45 ? Blocks.CRACKED_STONE_BRICKS : roll < 0.52 ? Blocks.MOSS_BLOCK : Blocks.STONE_BRICKS).defaultBlockState();
	}

	/** {@code r}'s gate's opening set to {@code phase} (its chunk loaded for it): shut, stirring, or open. */
	static void setGate(MinecraftServer server, Record r, SecretRealmGateBlock.Phase phase) {
		ServerLevel home = server.getLevel(r.realm.home());
		if (home == null || r.seal == null || !r.sealBuilt) return;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = 1; dy <= 4; dy++) {
				BlockPos pos = r.seal.offset(dx, dy, 0);
				BlockState here = home.getBlockState(pos);
				// One broken out of its gate (a command, say) is set back.
				BlockState gate = here.is(ModBlocks.SECRET_REALM_GATE) ? here : ModBlocks.SECRET_REALM_GATE.defaultBlockState();
				if (!here.is(ModBlocks.SECRET_REALM_GATE) || here.getValue(SecretRealmGateBlock.PHASE) != phase) {
					home.setBlock(pos, gate.setValue(SecretRealmGateBlock.PHASE, phase), Block.UPDATE_ALL);
				}
			}
		}
	}

	/**
	 * What {@code p} reads on a Seal ({@code keystone} or its gate, right-clicked): its realm, whom it admits and is meant for, when it opens
	 * (or how long its gate stands open), and who took its Great Inheritance.
	 */
	public static void tellAt(ServerPlayer p, BlockPos where) {
		Data data = Data.get(p.server);
		Record r = data.atSeal(p.level().dimension(), where);
		if (r == null) return;
		long now = p.server.overworld().getGameTime();
		Component meant = r.realm.meantFor().getDisplayName();
		p.sendSystemMessage(Component.translatable(ModLang.MSG_SECRET_INFO, r.realm.displayName(), r.realm.lowestName(), meant).withStyle(ChatFormatting.GOLD));
		Component when;
		if (gateOpen(r, now)) when = Component.translatable(ModLang.MSG_SECRET_INFO_GATE, clock(r.openedAt + r.realm.gateTicks() - now));
		else if (r.openedAt >= 0) when = Component.translatable(ModLang.MSG_SECRET_INFO_SEALED, clock(untilClose(r, now)));
		else if (r.stirred) when = Component.translatable(ModLang.MSG_SECRET_INFO_STIRRING, clock(untilOpen(r, now)));
		else when = Component.translatable(ModLang.MSG_SECRET_INFO_SLEEPING, years(untilOpen(r, now)));
		p.sendSystemMessage(when.copy().withStyle(ChatFormatting.YELLOW));
		p.sendSystemMessage((r.inheritor.isEmpty() ? Component.translatable(ModLang.MSG_SECRET_INFO_HEIR_WAITS)
				: Component.translatable(ModLang.MSG_SECRET_INFO_HEIR, r.inheritor)).withStyle(ChatFormatting.GRAY));
		if (!mayEnterRank(p, r.realm)) p.sendSystemMessage(Component.translatable(ModLang.MSG_SECRET_TOO_WEAK, r.realm.displayName(), r.realm.lowestName()).withStyle(ChatFormatting.RED));
	}

	/** "4:32": real minutes and seconds of {@code ticks}. */
	public static String clock(long ticks) {
		long s = Math.max(0, ticks) / 20;
		return s / 60 + ":" + String.format("%02d", s % 60);
	}

	/** In-game years and days of {@code ticks}, said roughly ("2 years", "half a year"): the world's clock, a year a day. */
	static Component years(long ticks) {
		double years = ticks / 24_000.0;
		if (years >= 1.5) return Component.translatable(ModLang.MSG_SECRET_YEARS, (int) Math.round(years));
		if (years >= 0.75) return Component.translatable(ModLang.MSG_SECRET_YEAR);
		return Component.translatable(ModLang.MSG_SECRET_HOURS, Math.max(1, Math.round(ticks / 1000.0)));
	}

	// --- Kept with the world ---

	/** One realm's record. */
	public static final class Record {
		public final SecretRealm realm;
		/** Its Seal's gate's middle at its floor, in its world (null until chosen); whether it is built. */
		public BlockPos seal;
		public boolean sealBuilt;
		/** When it next opens (game time; -1 not yet set), when the opening under way began (-1 shut), whether it stirs for the next. */
		public long nextOpen = -1, openedAt = -1;
		public boolean stirred;
		/** Its gate's light in, its quake begun (this opening's). */
		boolean gateLit, quaking;
		/** How many times it has opened; whether its land's buildings are laid (the first stirring's). */
		public int openings;
		public boolean laid;
		/** Who took its Great Inheritance ("" none yet), and whether it was one of the cultivators (else a player); their id. */
		public String inheritor = "";
		public boolean inheritorNpc;
		public UUID inheritorId;
		/** The latest lesser legacies' takers (newest first, a few). */
		public final Deque<String> legacies = new ArrayDeque<>();
		/** The players in it now, and how much each carried coming in. */
		public final Set<UUID> inside = new HashSet<>();
		final Map<UUID, Integer> carriedIn = new HashMap<>();
		/** The realm's own (HerbValleyWorks: this opening's event and trials; SecretRealmRivals: its visitors): kept as a tag each owns. */
		public CompoundTag works = new CompoundTag(), rivals = new CompoundTag();

		Record(SecretRealm realm) {
			this.realm = realm;
		}

		public void legacy(String name) {
			legacies.addFirst(name);
			while (legacies.size() > 5) legacies.removeLast();
		}

		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			if (seal != null) tag.putLong("Seal", seal.asLong());
			tag.putBoolean("SealBuilt", sealBuilt);
			tag.putLong("NextOpen", nextOpen);
			tag.putLong("OpenedAt", openedAt);
			tag.putBoolean("Stirred", stirred);
			tag.putBoolean("GateLit", gateLit);
			tag.putBoolean("Quaking", quaking);
			tag.putInt("Openings", openings);
			tag.putBoolean("Laid", laid);
			tag.putString("Inheritor", inheritor);
			tag.putBoolean("InheritorNpc", inheritorNpc);
			if (inheritorId != null) tag.putUUID("InheritorId", inheritorId);
			ListTag names = new ListTag();
			for (String name : legacies) names.add(net.minecraft.nbt.StringTag.valueOf(name));
			tag.put("Legacies", names);
			ListTag in = new ListTag();
			for (UUID id : inside) {
				CompoundTag one = new CompoundTag();
				one.putUUID("Id", id);
				one.putInt("Carried", carriedIn.getOrDefault(id, 0));
				in.add(one);
			}
			tag.put("Inside", in);
			tag.put("Works", works);
			tag.put("Rivals", rivals);
			return tag;
		}

		void load(CompoundTag tag) {
			seal = tag.contains("Seal") ? BlockPos.of(tag.getLong("Seal")) : null;
			sealBuilt = tag.getBoolean("SealBuilt");
			nextOpen = tag.contains("NextOpen") ? tag.getLong("NextOpen") : -1;
			openedAt = tag.contains("OpenedAt") ? tag.getLong("OpenedAt") : -1;
			stirred = tag.getBoolean("Stirred");
			gateLit = tag.getBoolean("GateLit");
			quaking = tag.getBoolean("Quaking");
			openings = tag.getInt("Openings");
			laid = tag.getBoolean("Laid");
			inheritor = tag.getString("Inheritor");
			inheritorNpc = tag.getBoolean("InheritorNpc");
			inheritorId = tag.hasUUID("InheritorId") ? tag.getUUID("InheritorId") : null;
			legacies.clear();
			for (Tag name : tag.getList("Legacies", Tag.TAG_STRING)) legacies.add(name.getAsString());
			inside.clear();
			carriedIn.clear();
			for (Tag entry : tag.getList("Inside", Tag.TAG_COMPOUND)) {
				CompoundTag one = (CompoundTag) entry;
				if (!one.hasUUID("Id")) continue;
				inside.add(one.getUUID("Id"));
				carriedIn.put(one.getUUID("Id"), one.getInt("Carried"));
			}
			works = tag.getCompound("Works");
			rivals = tag.getCompound("Rivals");
		}
	}

	/** Every realm's record, kept with the Overworld's data. */
	public static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_secret_realms";
		final Map<SecretRealm, Record> records = new EnumMap<>(SecretRealm.class);

		public static Data get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		Data() {
			for (SecretRealm realm : SecretRealm.values()) records.put(realm, new Record(realm));
		}

		public Record of(SecretRealm realm) {
			return records.get(realm);
		}

		/** The realm whose Seal {@code pos} (its gate, or the keystone over it) is part of in {@code dimension}, or null. */
		public Record atSeal(ResourceKey<Level> dimension, BlockPos pos) {
			for (Record r : records.values()) {
				if (r.seal == null || r.realm.home() != dimension) continue;
				if (Math.abs(pos.getX() - r.seal.getX()) <= 3 && Math.abs(pos.getZ() - r.seal.getZ()) <= 2 && pos.getY() >= r.seal.getY() && pos.getY() <= r.seal.getY() + 7) return r;
			}
			return null;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			for (Record r : records.values()) tag.put(r.realm.getId(), r.save());
			return tag;
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			for (Record r : data.records.values()) if (tag.contains(r.realm.getId())) r.load(tag.getCompound(r.realm.getId()));
			return data;
		}
	}

	/**
	 * The secret realms' land made by their own generator (SecretRealmGenerator), each realm's biome by its slot (SecretRealmBiomeSource); a
	 * realm's creatures gone as they load once their opening is over (RealmWorks#creatureLoaded); the Yellow Springs Hollow's bell heard
	 * (SpringsHollowWorks#register).
	 */
	public static void register() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, DefyingTheHeavens.id("secret_realm"), SecretRealmGenerator.CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, DefyingTheHeavens.id("secret_realms"), SecretRealmBiomeSource.CODEC);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(RealmWorks::creatureLoaded);
		SpringsHollowWorks.register();
		JadeLakeMirrorItem.register();
	}

	private SecretRealms() {}
}
