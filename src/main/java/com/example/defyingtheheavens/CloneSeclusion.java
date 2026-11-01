package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Clone Seclusion (CultivationMethod#CLONE_SECLUSION; the user's pick of 2026-11-01): the heaven path's way of a clone (Clones) left in
 * seclusion. While its maker is online and practising it, the clone standing unplayed meditates, drawn in the lotus pose
 * (CloneBodyEntity#isSecluding), its land held loaded; each second it banks {@link #PACE} of what the realm's own meditation would gather
 * where its qi is at home (no mats, no Inner Realm, no aid), at most {@link #MOST} of a stage, in its tag ({@link #BANKED}, carried as it
 * is played and left again). Drawn back in (Clones#absorb), what it banked is its maker's ({@link #drawnIn}: CultivationMethods#grant, its
 * years paid then). Every {@link #CHALLENGE} ticks a wild beast of the maker's realm comes for it; slain, it dissolves, and what it banked
 * is lost with it ({@link #slain}). Its maker's own meditation gathers nothing.
 */
public final class CloneSeclusion {
	public static final double PACE = 0.5, MOST = 1.0;
	public static final int CHALLENGE = 5 * 60 * 20;
	/** The clone's tag's share of its maker's stage banked. */
	public static final String BANKED = "Banked";
	private static final int TICKET_RADIUS = 2;
	private static final TicketType<ChunkPos> TICKET = TicketType.create("defying-the-heavens:clone_seclusion", Comparator.comparingLong(ChunkPos::toLong));

	/** The land held about each maker's secluded clone (to let go of), and when its next challenger comes (game time); not saved. */
	private record Held(ServerLevel level, ChunkPos chunk) {}
	private static final Map<UUID, Held> HELD = new HashMap<>();
	private static final Map<UUID, Long> NEXT = new HashMap<>();

	/** Whether {@code c} cultivates by its clone's seclusion: one of the living, its meridians open, its heaven path practising it. */
	public static boolean practises(PlayerCultivation c) {
		return HeavenMethods.canLearn(c) && CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) == CultivationMethod.CLONE_SECLUSION;
	}

	/**
	 * The share of {@code c}'s stage a second of its clone's seclusion banks: {@link #PACE} of the stage's own gain where the realm's qi is
	 * at home (PlayerCultivation#qiNeed: the Upper Realm's density for its realms, the Great Void's, the Immortal Realm's), no aid counted.
	 */
	public static double perSecond(PlayerCultivation c) {
		double required = c.cultivationRequired();
		return required <= 0 ? 0 : PACE * c.cultivationPerSecond() * PlayerCultivation.qiNeed(c.getRealm()) / required;
	}

	/** What {@code p}'s clone has banked (0 with none out). */
	public static double banked(ServerPlayer p) {
		CompoundTag tag = CultivationManager.own(p).getClone();
		return tag == null ? 0 : tag.getDouble(BANKED);
	}

	/** The server's tick: each second, every maker's clone secluded (or let be). */
	static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) second(p);
	}

	/** A second of {@code p}'s: its clone standing unplayed and the method practised, a second of seclusion banked; else let be. */
	private static void second(ServerPlayer p) {
		PlayerCultivation own = CultivationManager.own(p);
		CompoundTag tag = own.getClone();
		if (tag == null) NEXT.remove(p.getUUID());
		CloneBodyEntity clone = tag == null ? null : Clones.bodyNotPlayed(p.server, own);
		boolean secludes = tag != null && !Clones.inClone(p) && p.isAlive() && practises(own) && CultivationMethods.holdsVolume(own, TwoPaths.Path.HEAVEN);
		if (!secludes) {
			if (clone != null && !clone.isSitting()) clone.setSecluding(false);
			letGo(p.getUUID());
			return;
		}
		ServerLevel level = Clones.levelOf(p.server, tag);
		if (level == null) return;
		hold(p.getUUID(), level, clone != null ? clone.position() : Clones.posOf(tag));
		if (clone == null || clone.isSitting()) return; // its land still loading
		clone.setSecluding(true);
		double was = tag.getDouble(BANKED), now = Math.min(MOST, was + perSecond(own));
		tag.putDouble(BANKED, now);
		CultivationManager.markDirty(p.server);
		if (now >= MOST && was < MOST) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_CLONE_SECLUSION_FULL).withStyle(ChatFormatting.GOLD));
		} else if (Math.floor(now * 10) > Math.floor(was * 10)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_SECLUSION_BANKED, Math.round(now * 100)).withStyle(ChatFormatting.AQUA), true);
		}
		level.sendParticles(ParticleTypes.ENCHANT, clone.getX(), clone.getY() + 1.0, clone.getZ(), 8, 0.5, 0.5, 0.5, 0.8);
		long time = p.server.overworld().getGameTime();
		long next = NEXT.computeIfAbsent(p.getUUID(), id -> time + CHALLENGE);
		if (time >= next) {
			NEXT.put(p.getUUID(), time + CHALLENGE);
			challenge(p, own, level, clone);
		}
	}

	/**
	 * A challenger comes for {@code p}'s secluded clone: a wild beast of its heaven path's rank (one of a kind past its tier's highest, grown
	 * with it, where no kind's tier holds the rank: DemonBeast#setBoundlessRank), a dozen blocks off, hunting it, and kept from despawning
	 * (its maker may be far away). Told.
	 */
	static void challenge(ServerPlayer p, PlayerCultivation own, ServerLevel level, CloneBodyEntity clone) {
		int rank = Math.max(0, PlayerCultivation.rank(own.getRealm(), own.getStage()));
		// The kinds whose tier holds the rank; else the mightiest ordinary ones (the rare tier), grown to it.
		List<BeastKind> fit = new ArrayList<>(), grown = new ArrayList<>();
		int mightiest = -1;
		for (BeastKind kind : BeastKind.values()) {
			if (kind.isPrimordial() || kind.isGuardian() || kind == BeastKind.CHAOS_FIEND) continue;
			if (kind.minRank() <= rank && kind.maxRank() >= rank) fit.add(kind);
			if (!kind.isElder() && kind.maxRank() < rank) mightiest = Math.max(mightiest, kind.maxRank());
		}
		for (BeastKind kind : BeastKind.values()) {
			if (!kind.isElder() && !kind.isPrimordial() && !kind.isGuardian() && kind != BeastKind.CHAOS_FIEND && kind.maxRank() == mightiest) grown.add(kind);
		}
		boolean boundless = fit.isEmpty();
		List<BeastKind> pool = boundless ? grown : fit;
		if (pool.isEmpty()) return;
		BeastKind kind = pool.get(level.random.nextInt(pool.size()));
		double a = level.random.nextDouble() * Math.PI * 2;
		BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				BlockPos.containing(clone.getX() + Math.cos(a) * 12, clone.getY(), clone.getZ() + Math.sin(a) * 12));
		DemonBeast beast = ModEntities.beast(kind).create(level);
		if (beast == null) return;
		beast.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
		beast.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
		if (boundless) beast.setBoundlessRank(rank);
		else beast.setRank(rank);
		beast.setPersistenceRequired();
		level.addFreshEntity(beast);
		beast.setTarget(clone);
		level.playSound(null, at, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.0f, 0.8f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_CLONE_SECLUSION_CHALLENGED, beast.getDisplayName()).withStyle(ChatFormatting.RED));
	}

	/** {@code p} drew its clone back in (Clones#absorb), {@code banked} of its stage gathered by its seclusion: that is its own now. */
	static void drawnIn(ServerPlayer p, double banked) {
		letGo(p.getUUID());
		NEXT.remove(p.getUUID());
		if (banked > 0) CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.CLONE_SECLUSION, banked);
	}

	/** {@code p}'s clone was slain where it stood (Clones#onStandInDied): what its seclusion gathered, {@code banked}, lost with it (told). */
	static void slain(ServerPlayer p, double banked) {
		letGo(p.getUUID());
		NEXT.remove(p.getUUID());
		if (banked > 0) p.sendSystemMessage(Component.translatable(ModLang.MSG_CLONE_SECLUSION_LOST, Math.round(banked * 100)).withStyle(ChatFormatting.RED));
	}

	/** The land about {@code owner}'s clone at {@code at} held loaded (moved with it, if it was knocked into the next chunk). */
	private static void hold(UUID owner, ServerLevel level, Vec3 at) {
		ChunkPos chunk = new ChunkPos(BlockPos.containing(at));
		Held held = HELD.get(owner);
		if (held != null && held.level() == level && held.chunk().equals(chunk)) return;
		letGo(owner);
		level.getChunkSource().addRegionTicket(TICKET, chunk, TICKET_RADIUS, chunk);
		HELD.put(owner, new Held(level, chunk));
	}

	private static void letGo(UUID owner) {
		Held held = HELD.remove(owner);
		if (held != null) held.level().getChunkSource().removeRegionTicket(TICKET, held.chunk(), TICKET_RADIUS, held.chunk());
	}

	/** A player left: its clone's land let go (the clone stands on, its seclusion paused till it is back). */
	public static void forget(UUID id) {
		letGo(id);
		NEXT.remove(id);
	}

	/** The server stopped. */
	public static void clear() {
		HELD.clear();
		NEXT.clear();
	}

	private CloneSeclusion() {}
}
