package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Immortal Gate (phase 2 of the realm plan, 2026-10-25, the user's answers): the way between the Great Void and the Immortal Realm,
 * and the only one (the user's rule: space won't tear in the Immortal Realm, and its clouds bear the fallen back up, ImmortalRealm).
 * <ul>
 *   <li><b>Where.</b> The Immortal Gate stands in the Great Void's sky over its void sea, north of the Shore of Nirvana's camp
 *   ({@link #SKY_X}, {@link #SKY_Y}, {@link #SKY_Z}); its other side, the Southern Heavenly Gate, on the Immortal Realm's Gate ground
 *   about its 0, 0 ({@link #HEAVEN_X}, {@link #HEAVEN_Z}). Both built in code the first time the server starts with their realm (as the
 *   camp is, NirvanaCamp): a gateway of white jade banded with gold under a gilded roof, the first on a bank of cloud.</li>
 *   <li><b>When.</b> Every five in-game days ({@link #PERIOD}, the user's change), for three minutes ({@link #OPEN_TICKS}), by the
 *   world's own clock; told to everyone a minute before ({@link #WARNING_TICKS}), when it opens and when it shuts. While it stands open
 *   light fills both gates' openings (ModBlocks#IMMORTAL_GATE_LIGHT).</li>
 *   <li><b>Up.</b> Into the Immortal Gate's light: Entering Nirvana and above pass ({@link #mayPass}; below, its pressure turns them
 *   back). The climb through the Gate's passage (ModDimensions#IMMORTAL_PASSAGE, {@link #CLIMB_SECONDS} from {@link #ENTER_Y} to
 *   {@link #EXIT_Y}, no flight: its current carries everyone) presses all the while, a share of one's own body a second
 *   ({@link #pressureShare}), armour worn turning up to {@link #MAX_ARMOUR_SHARE} of it ({@link #armourShare}): nothing on a True
 *   Immortal; some 2.2 bodies over the climb on Entering Nirvana Grand Perfection, which its own mending (about a body in the climb's
 *   length, CultivationStats#MEND_SECONDS) with armour or pills carries it through: the user's "survives with care"; 4.5 bodies on
 *   Late, 9 on Mid, 18 on Early: crushed. Out at the top, north of the Southern Heavenly Gate.</li>
 *   <li><b>Down.</b> Only while the Gate stands open (the user's pick): into the Southern Heavenly Gate's light, down onto the Shore of
 *   Nirvana's camp.</li>
 *   <li><b>Those waiting.</b> When it opens, some of the camp ({@link #RUSH_SHARE}, at most {@link #RUSH_MOST}) rise and fly into it and
 *   are gone; new ones come in over the following days (NirvanaCamp#ARRIVES_EVERY). Those it shuts on fly back.</li>
 *   <li><b>Its guards.</b> {@link #GUARDS} Immortal Officers of the Celestial Court, True Immortals, keep the Southern Heavenly Gate.
 *   Settled at their posts they start no fight (CultivatorGoals.HostilityGoal), so no demon-race Entering Nirvana come up through the
 *   Gate is set upon there (the user's rule); struck, they strike back.</li>
 * </ul>
 */
public final class ImmortalGate {
	/** The Gate opens every this many ticks (five in-game days), and stands open this long (three minutes). */
	public static final long PERIOD = 5L * 24000L;
	public static final int OPEN_TICKS = 3 * 60 * 20;
	/** Its opening is told this long before (ticks: a minute). */
	static final int WARNING_TICKS = 60 * 20;
	/** The Immortal Gate's middle (its opening's) and the height of its threshold, over the Great Void's void sea. */
	public static final int SKY_X = 0, SKY_Z = -330, SKY_Y = 262;
	/** The Southern Heavenly Gate's middle, on the Immortal Realm's Gate ground (its threshold the ground's own level). */
	public static final int HEAVEN_X = 0, HEAVEN_Z = 0;
	/** A gate's opening: this many blocks either side of its middle, this tall over its threshold; its pillars this thick. */
	public static final int HALF = 10, HEIGHT = 28, PILLAR = 4; // public: its light drawn by the client (GatePortalRenderer)
	/** Into the Gate's passage at this height, out at this one; the climb's length (seconds) at Levitation {@link #LIFT_AMPLIFIER}. */
	public static final int ENTER_Y = 40, EXIT_Y = 340, CLIMB_SECONDS = 30;
	static final int LIFT_AMPLIFIER = 9;
	/** Armour worn turns this share of the pressure a point (its defence, half its toughness), at most this much in all. */
	public static final double ARMOUR_POINT_SHARE = 0.015, MAX_ARMOUR_SHARE = 0.45;
	/** When it opens, this share of the camp rush it, at most this many, flying this fast (blocks a tick). */
	static final double RUSH_SHARE = 1.0 / 3, RUSH_SPEED = 0.9;
	static final int RUSH_MOST = 8;
	/** The Southern Heavenly Gate's guards: how many, how often one more comes while there is room (ticks: an in-game hour), how far each keeps from its post. */
	public static final int GUARDS = 4;
	static final long GUARD_EVERY = 1000;
	public static final int GUARD_ROAM = 6;
	/** Entering Nirvana may pass; from True Immortal on the pressure is nothing. */
	static final int PASS_RANK = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY), FREE_RANK = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
	/** Whoever stands in a shut gate's opening is told when it opens, at most this often (ticks). */
	private static final int TOLD_EVERY = 200;

	/** One climbing the Gate's passage: ticks in it. */
	private static final class Climb {
		int ticks;
	}

	private static final Map<UUID, Climb> CLIMBS = new HashMap<>();
	/** Campers flying into the open Gate; those it shut on, flying back to the camp. */
	private static final Set<UUID> RUSHING = new HashSet<>(), RETURNING = new HashSet<>();
	/** When each was last told the Gate turned them back, or is shut (game time). */
	private static final Map<UUID, Long> TOLD = new HashMap<>();

	public static boolean isClimbing(UUID id) {
		return CLIMBS.containsKey(id);
	}

	/**
	 * Whether {@code npc} is one of the camp flying for the open Gate, or back from the shut one: no camper while it flies (its home let
	 * go), it starts no fight (CultivatorGoals.HostilityGoal) and lets none turn it aside ({@link #rushers}). The tour of 2026-10-27
	 * found the righteous and devilish rushing it fighting the camp's other path as they rose, and most of them flown off out of the land.
	 */
	public static boolean isRushing(CultivatorNpc npc) {
		return RUSHING.contains(npc.getUUID()) || RETURNING.contains(npc.getUUID());
	}

	// --- When ---

	/** Where in its cycle the world's clock {@code time} stands (0 to {@link #PERIOD}). */
	static long phase(long time) {
		return Math.floorMod(time, PERIOD);
	}

	/** Whether the Gate stands open at {@code time} by its course: the last {@link #OPEN_TICKS} of each cycle. */
	public static boolean opensAt(long time) {
		return phase(time) >= PERIOD - OPEN_TICKS;
	}

	/** Whether the Gate stands open now (by its course, or opened out of turn by a command and not shut since). */
	public static boolean isOpen(MinecraftServer server) {
		Data data = Data.get(server);
		long now = server.overworld().getGameTime();
		if (now < data.forcedUntil) return true;
		return opensAt(now) && now >= data.shutUntil;
	}

	/** The Southern Heavenly Gate's threshold's height (its plaza's paving), or {@code Integer.MIN_VALUE} while it isn't built. */
	public static int heavenY(MinecraftServer server) {
		Data data = Data.get(server);
		return data.heavenBuilt ? data.heavenY : Integer.MIN_VALUE;
	}

	/** Ticks until the Gate next opens by its course (0 while it is open). */
	public static long untilOpen(long time) {
		return opensAt(time) ? 0 : PERIOD - OPEN_TICKS - phase(time);
	}

	/** A command's: the Gate open now, for {@link #OPEN_TICKS}. */
	public static void openNow(MinecraftServer server) {
		Data data = Data.get(server);
		data.forcedUntil = server.overworld().getGameTime() + OPEN_TICKS;
		data.shutUntil = 0;
		data.setDirty();
	}

	/** A command's: the Gate shut now, till its course next opens it. */
	public static void shutNow(MinecraftServer server) {
		Data data = Data.get(server);
		long now = server.overworld().getGameTime();
		data.forcedUntil = 0;
		data.shutUntil = opensAt(now) ? now + (PERIOD - phase(now)) : 0;
		data.setDirty();
	}

	// --- Each tick ---

	public static void tick(MinecraftServer server) {
		if (server == null) return;
		ServerLevel greatVoid = server.getLevel(ModDimensions.GREAT_VOID), immortal = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (greatVoid == null || immortal == null) return;
		Data data = Data.get(server);
		boolean open = isOpen(server);
		if (server.getTickCount() % 20 == 7) {
			try {
				if (open != data.lit) {
					if (open) opened(server, greatVoid, immortal, data);
					else shut(server, greatVoid, immortal, data);
				}
				warn(server, data);
				keepGuards(immortal, data);
				if (!open) tellShut(greatVoid, immortal, data);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Immortal Gate tick failed", e);
			}
		}
		if (open && data.lit) walkers(server, greatVoid, immortal, data);
		rushers(greatVoid, data, open);
		ServerLevel passage = server.getLevel(ModDimensions.IMMORTAL_PASSAGE);
		CLIMBS.keySet().removeIf(id -> {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			return p == null || passage == null || p.level() != passage;
		});
		if (passage == null) return;
		for (ServerPlayer p : new ArrayList<>(passage.players())) {
			try {
				tickClimber(server, passage, immortal, data, p);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Immortal Gate climb failed for {}", p.getScoreboardName(), e);
			}
		}
	}

	/** It opens: light in both openings, everyone told, the sound of it, and some of the camp rise to it. */
	private static void opened(MinecraftServer server, ServerLevel greatVoid, ServerLevel immortal, Data data) {
		if (data.skyBuilt) light(greatVoid, SKY_X, data.skyY, SKY_Z, true);
		if (data.heavenBuilt) light(immortal, HEAVEN_X, data.heavenY, HEAVEN_Z, true);
		data.lit = true;
		data.setDirty();
		announce(server, Component.translatable(ModLang.MSG_GATE_OPENS, OPEN_TICKS / 1200).withStyle(ChatFormatting.GOLD));
		greatVoid.playSound(null, SKY_X, data.skyY + HEIGHT / 2.0, SKY_Z, SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 6.0f, 0.6f);
		greatVoid.playSound(null, SKY_X, data.skyY + HEIGHT / 2.0, SKY_Z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 8.0f, 0.5f);
		immortal.playSound(null, HEAVEN_X, data.heavenY + HEIGHT / 2.0, HEAVEN_Z, SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 6.0f, 0.6f);
		rush(greatVoid);
	}

	/** It shuts: the light gone from both openings, everyone told, those still flying to it sent back. */
	private static void shut(MinecraftServer server, ServerLevel greatVoid, ServerLevel immortal, Data data) {
		if (data.skyBuilt) light(greatVoid, SKY_X, data.skyY, SKY_Z, false);
		if (data.heavenBuilt) light(immortal, HEAVEN_X, data.heavenY, HEAVEN_Z, false);
		data.lit = false;
		data.setDirty();
		announce(server, Component.translatable(ModLang.MSG_GATE_CLOSES, PERIOD / 24000).withStyle(ChatFormatting.GRAY));
		greatVoid.playSound(null, SKY_X, data.skyY + HEIGHT / 2.0, SKY_Z, SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 6.0f, 0.5f);
		immortal.playSound(null, HEAVEN_X, data.heavenY + HEIGHT / 2.0, HEAVEN_Z, SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 6.0f, 0.5f);
		RETURNING.addAll(RUSHING);
		RUSHING.clear();
	}

	/** A minute before it opens by its course, everyone told (once a cycle). */
	private static void warn(MinecraftServer server, Data data) {
		long now = server.overworld().getGameTime();
		long until = untilOpen(now), cycle = Math.floorDiv(now, PERIOD);
		if (until <= 0 || until > WARNING_TICKS || data.warnedCycle == cycle || now < data.shutUntil) return;
		data.warnedCycle = cycle;
		data.setDirty();
		announce(server, Component.translatable(ModLang.MSG_GATE_STIRS).withStyle(ChatFormatting.YELLOW));
	}

	private static void announce(MinecraftServer server, Component message) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) p.sendSystemMessage(message);
	}

	/** Light set in (or taken out of) the opening of the gate whose threshold is ({@code x}, {@code y}, {@code z}). */
	static void light(ServerLevel level, int x, int y, int z, boolean on) {
		BlockState light = ModBlocks.IMMORTAL_GATE_LIGHT.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dy = 1; dy <= HEIGHT; dy++) {
				for (int dz = -1; dz <= 0; dz++) {
					BlockPos at = new BlockPos(x + dx, y + dy, z + dz);
					BlockState here = level.getBlockState(at);
					if (on && here.isAir()) level.setBlock(at, light, Block.UPDATE_CLIENTS);
					else if (!on && here.is(ModBlocks.IMMORTAL_GATE_LIGHT)) level.setBlock(at, air, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** The opening of the gate whose threshold is ({@code x}, {@code y}, {@code z}), as a box. */
	static AABB opening(int x, int y, int z) {
		return new AABB(x - HALF, y + 1, z - 1, x + HALF + 1, y + HEIGHT + 1, z + 1);
	}

	/** Who stepped into either gate's light: up from the Great Void (who may pass), down from the Immortal Realm. */
	private static void walkers(MinecraftServer server, ServerLevel greatVoid, ServerLevel immortal, Data data) {
		if (data.skyBuilt) {
			for (ServerPlayer walker : greatVoid.getEntitiesOfClass(ServerPlayer.class, opening(SKY_X, data.skyY, SKY_Z))) {
				if (!present(walker) || CLIMBS.containsKey(walker.getUUID())) continue;
				if (mayPass(walker)) enterClimb(server, walker);
				else turnBack(walker, SKY_Z);
			}
		}
		if (data.heavenBuilt) {
			for (ServerPlayer walker : immortal.getEntitiesOfClass(ServerPlayer.class, opening(HEAVEN_X, data.heavenY, HEAVEN_Z))) {
				if (present(walker)) descend(server, walker);
			}
		}
	}

	/** One who is there in body: alive, not watching, not a soul wandering from its body, not one of the dead. */
	private static boolean present(ServerPlayer p) {
		return p.isAlive() && !p.isSpectator() && !SoulProjection.isAwayFromBody(p.getUUID()) && !Incorporeal.isRuledGhost(p);
	}

	/** Whether {@code walker} may pass the Gate: Entering Nirvana and above (either path's), or one in creative. */
	public static boolean mayPass(ServerPlayer walker) {
		return walker.isCreative() || TwoPaths.higherRank(CultivationManager.get(walker), false) >= PASS_RANK;
	}

	/** The Gate's pressure turns {@code walker} back out of its light, away from it along z. */
	private static void turnBack(ServerPlayer walker, int z) {
		long now = walker.level().getGameTime();
		Long told = TOLD.get(walker.getUUID());
		if (told == null || now - told > 60) {
			TOLD.put(walker.getUUID(), now);
			walker.displayClientMessage(Component.translatable(ModLang.MSG_GATE_TURNED_BACK).withStyle(ChatFormatting.RED), true);
		}
		double away = walker.getZ() >= z ? 1 : -1;
		walker.push(0, 0.2, away * 1.2);
		walker.hurtMarked = true;
	}

	/** Whoever stands in a shut gate's opening is told when it opens. */
	private static void tellShut(ServerLevel greatVoid, ServerLevel immortal, Data data) {
		long now = greatVoid.getGameTime(), days = Math.max(1, Mth.ceil(untilOpen(now) / 24000.0));
		List<ServerPlayer> standing = new ArrayList<>();
		if (data.skyBuilt) standing.addAll(greatVoid.getEntitiesOfClass(ServerPlayer.class, opening(SKY_X, data.skyY, SKY_Z)));
		if (data.heavenBuilt) standing.addAll(immortal.getEntitiesOfClass(ServerPlayer.class, opening(HEAVEN_X, data.heavenY, HEAVEN_Z)));
		for (ServerPlayer p : standing) {
			Long told = TOLD.get(p.getUUID());
			if (told != null && now - told < TOLD_EVERY) continue;
			TOLD.put(p.getUUID(), now);
			long ticks = untilOpen(now);
			p.displayClientMessage(ticks < 24000 ? Component.translatable(ModLang.MSG_GATE_SHUT_HOURS, Math.max(1, Mth.ceil(ticks / 1000.0)))
					: Component.translatable(ModLang.MSG_GATE_SHUT_DAYS, days), true);
		}
	}

	// --- Up: the climb ---

	/** Into the Gate's passage at {@link #ENTER_Y}: the climb begins, and the Southern Heavenly Gate's ground is asked for. */
	static void enterClimb(MinecraftServer server, ServerPlayer p) {
		ServerLevel passage = server.getLevel(ModDimensions.IMMORTAL_PASSAGE);
		ServerLevel immortal = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (passage == null || immortal == null) {
			DefyingTheHeavens.LOGGER.error("The Immortal Gate's passage or the Immortal Realm is missing; cannot carry {} up", p.getScoreboardName());
			return;
		}
		TribulationManager.abandon(p);
		MeditationManager.stop(p, false);
		CLIMBS.put(p.getUUID(), new Climb());
		p.teleportTo(passage, 0.5, ENTER_Y, 0.5, p.getYRot(), 0);
		p.connection.resetPosition();
		p.fallDistance = 0;
		RealmSuppressionHandler.update(p);
		SpatialArrivals.prepare(immortal, new BlockPos(HEAVEN_X, 0, HEAVEN_Z));
		p.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6f, 0.7f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_GATE_ENTER, CLIMB_SECONDS).withStyle(ChatFormatting.GOLD));
	}

	/** A tick of {@code p}'s climb: the current, the pressure; at the top, out at the Southern Heavenly Gate. */
	private static void tickClimber(MinecraftServer server, ServerLevel passage, ServerLevel immortal, Data data, ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator()) return;
		Climb c = CLIMBS.computeIfAbsent(p.getUUID(), id -> new Climb()); // relogged inside, or sent by a command: climbing again
		// Vanilla shields a player who just joined for 3 s: relogging over and over would outlast the pressure. Never here.
		((ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);
		if (p.isFallFlying()) p.stopFallFlying();
		if (p.getY() >= EXIT_Y) {
			arrive(server, immortal, data, p);
			return;
		}
		MobEffectInstance lift = p.getEffect(MobEffects.LEVITATION);
		if (lift == null || lift.getAmplifier() != LIFT_AMPLIFIER || lift.getDuration() < 20) {
			p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, LIFT_AMPLIFIER, false, false, false));
		}
		if (++c.ticks % 20 == 0) {
			float harm = (float) (p.getMaxHealth() * pressureShare(TwoPaths.higherRank(CultivationManager.get(p), false)) * (1 - armourShare(p)));
			if (harm > 0) p.hurt(ModDamageTypes.spatialPressure(passage), harm);
			if (!p.isAlive()) return;
			p.displayClientMessage(Component.translatable(ModLang.MSG_GATE_CLIMB, Math.max(0, EXIT_Y - Mth.floor(p.getY()))), true);
		}
		if (c.ticks % 400 == 0) SpatialArrivals.prepare(immortal, new BlockPos(HEAVEN_X, 0, HEAVEN_Z));
		if (c.ticks % 4 == 0) {
			passage.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 4, p.getZ(), 6, 2.5, 3, 2.5, 0.08);
		}
	}

	/**
	 * The Gate's pressure on one of {@code rank} (the higher path's), a share of its own body a second: nothing from True Immortal on;
	 * on Entering Nirvana's Grand Perfection {@link #GRAND_PERFECTION_BODIES} over the climb, on Late 4.5, Mid 9, Early 18; below it (who
	 * are turned back at the Gate) a body a second. A fed body mends about one over the climb (CultivationStats#MEND_SECONDS), so a Grand
	 * Perfection bare of armour falls some 1.2 bodies short: armour (up to {@link #MAX_ARMOUR_SHARE} off) or pills carry it through, the
	 * user's "survives with care" (tour 1: at 1.4 bodies, netherite and mending kept it above 97%).
	 */
	public static double pressureShare(int rank) {
		if (rank >= FREE_RANK) return 0;
		if (rank < PASS_RANK) return 1;
		double bodies = switch (Stage.byIndex(rank % Stage.values().length)) {
			case GRAND_PERFECTION -> GRAND_PERFECTION_BODIES;
			case LATE -> 4.5;
			case MID -> 9.0;
			case EARLY -> 18.0;
		};
		return bodies / CLIMB_SECONDS;
	}

	public static final double GRAND_PERFECTION_BODIES = 2.2;

	/** How much of the Gate's pressure {@code p}'s worn armour turns: its pieces' defence and half their toughness, by {@link #ARMOUR_POINT_SHARE}. */
	public static double armourShare(ServerPlayer p) {
		double points = 0;
		for (ItemStack stack : p.getArmorSlots()) {
			if (stack.getItem() instanceof ArmorItem armour) points += armour.getDefense() + armour.getToughness() * 0.5;
		}
		return Math.min(MAX_ARMOUR_SHARE, points * ARMOUR_POINT_SHARE);
	}

	/** Through: out onto the Southern Heavenly Gate's ground, on its north side (inside the heavens), facing the Court. */
	private static void arrive(MinecraftServer server, ServerLevel immortal, Data data, ServerPlayer p) {
		CLIMBS.remove(p.getUUID());
		p.removeEffect(MobEffects.LEVITATION);
		int y = data.heavenBuilt ? data.heavenY + 1 : immortal.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HEAVEN_X, HEAVEN_Z - 8);
		p.teleportTo(immortal, HEAVEN_X + 0.5, y, HEAVEN_Z - 8 + 0.5, 180.0f, 0);
		p.connection.resetPosition();
		p.fallDistance = 0;
		p.setDeltaMovement(Vec3.ZERO);
		RealmSuppressionHandler.update(p);
		CultivationManager.markDirty(server);
		CultivationManager.sync(p);
		immortal.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 60, 0.8, 1.2, 0.8, 0.12);
		p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_IMMORTAL, (int) PlayerCultivation.IMMORTAL_QI_MULTIPLIER).withStyle(ChatFormatting.GOLD));
	}

	// --- Down ---

	/** Down through the Southern Heavenly Gate while the Gate stands open: onto the Shore of Nirvana's camp. */
	static void descend(MinecraftServer server, ServerPlayer p) {
		ServerLevel greatVoid = server.getLevel(ModDimensions.GREAT_VOID);
		if (greatVoid == null) return;
		TribulationManager.abandon(p);
		BlockPos camp = NirvanaCamp.middle(greatVoid);
		SpatialArrivals.prepare(greatVoid, camp);
		p.teleportTo(greatVoid, camp.getX() + 0.5, camp.getY() + 1, camp.getZ() + 4.5, 180.0f, 0);
		p.connection.resetPosition();
		p.fallDistance = 0;
		p.setDeltaMovement(Vec3.ZERO);
		RealmSuppressionHandler.update(p);
		CultivationManager.markDirty(server);
		CultivationManager.sync(p);
		greatVoid.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 1.0, 0.6, 0.1);
		p.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.4f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_GATE_DOWN).withStyle(ChatFormatting.GOLD), true);
	}

	// --- Those waiting on the Shore ---

	/** The Gate opens: some of the camp (those loaded) rise and fly for it. */
	private static void rush(ServerLevel greatVoid) {
		BlockPos middle = NirvanaCamp.middle(greatVoid);
		List<CultivatorNpc> campers = new ArrayList<>(NirvanaCamp.campers(greatVoid, middle));
		Collections.shuffle(campers, new java.util.Random(greatVoid.getRandom().nextLong()));
		int going = Math.min(RUSH_MOST, Mth.ceil(campers.size() * RUSH_SHARE));
		for (int i = 0; i < going; i++) {
			CultivatorNpc npc = campers.get(i);
			npc.setMeditating(false);
			npc.setQiFlying(true);
			if (!npc.isQiFlying()) continue; // it can't take to the air now: it waits for the next time
			npc.setSettledAt(null);
			npc.setTarget(null);
			RUSHING.add(npc.getUUID());
		}
		if (going > 0) DefyingTheHeavens.LOGGER.info("{} of those waiting on the Shore of Nirvana rush the Immortal Gate", going);
	}

	/** The rushing fly in at the Gate's opening from this far before it, on the camp's side (past its platform's eight and its cloud's rim). */
	static final int APPROACH = 12;
	/** While any fly between the camp and the Gate, the land between is kept ticking: a region ticket this wide over its middle, renewed. */
	private static final TicketType<ChunkPos> RUSH_TICKET = TicketType.create(DefyingTheHeavens.MOD_ID + ":gate_rush", Comparator.comparingLong(ChunkPos::toLong), 100);
	private static final int RUSH_TICKET_RADIUS = 9;
	/** The rushing past the point before the opening (flying straight in now), and the returning come out over the camp (coming down now). */
	private static final Set<UUID> IN_LINE = new HashSet<>(), OVER_CAMP = new HashSet<>();

	/**
	 * Each tick: the rushing fly on into the Gate's light and are gone; those it shut on fly back to the camp and settle there again. Each
	 * goes by the way up and level across, never straight from the camp at the opening: that line ran them into the underside of the Gate's
	 * cloud and platform, and out past the land the server kept ticking about one waiting at the camp, where they hung in the air (the user,
	 * 2026-10-27: "I don't think the EN cultivators at the camp enter the gate"). Up first to the opening's height, then across to
	 * {@link #APPROACH} before it, then in; back the same way reversed.
	 */
	private static void rushers(ServerLevel greatVoid, Data data, boolean open) {
		if (RUSHING.isEmpty() && RETURNING.isEmpty()) {
			IN_LINE.clear();
			OVER_CAMP.clear();
			return;
		}
		BlockPos camp = NirvanaCamp.middle(greatVoid);
		Vec3 mouth = new Vec3(SKY_X + 0.5, data.skyY + HEIGHT / 2.0, SKY_Z + 0.5);
		int side = camp.getZ() >= SKY_Z ? 1 : -1;
		Vec3 before = new Vec3(mouth.x, mouth.y, mouth.z + side * APPROACH);
		if (greatVoid.getGameTime() % 20 == 0) {
			ChunkPos middle = new ChunkPos(BlockPos.containing((mouth.x + camp.getX()) / 2, mouth.y, (mouth.z + camp.getZ()) / 2));
			greatVoid.getChunkSource().addRegionTicket(RUSH_TICKET, middle, RUSH_TICKET_RADIUS, middle);
		}
		for (UUID id : new ArrayList<>(RUSHING)) {
			if (!(greatVoid.getEntity(id) instanceof CultivatorNpc npc) || !npc.isAlive()) {
				RUSHING.remove(id);
				IN_LINE.remove(id);
				continue;
			}
			if (!npc.isQiFlying()) npc.setQiFlying(true);
			if (npc.getTarget() != null) npc.setTarget(null); // nothing turns it aside
			if (npc.position().distanceToSqr(before) < 3 * 3) IN_LINE.add(id);
			Vec3 to = IN_LINE.contains(id) ? mouth : npc.getY() < mouth.y - 3 ? new Vec3(npc.getX(), mouth.y, npc.getZ()) : before;
			npc.flyToward(to, RUSH_SPEED);
			if (open && npc.position().distanceToSqr(mouth) < 6 * 6) {
				greatVoid.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1, npc.getZ(), 30, 0.5, 1.0, 0.5, 0.15);
				greatVoid.playSound(null, npc.getX(), npc.getY(), npc.getZ(), SoundEvents.PORTAL_TRAVEL, SoundSource.NEUTRAL, 0.5f, 1.6f);
				RUSHING.remove(id);
				IN_LINE.remove(id);
				npc.discard(); // through (or crushed on the way: none comes back to tell)
			}
		}
		Vec3 home = Vec3.atBottomCenterOf(camp).add(0, 2, 0);
		Vec3 over = new Vec3(home.x, Math.max(home.y, mouth.y), home.z);
		for (UUID id : new ArrayList<>(RETURNING)) {
			if (!(greatVoid.getEntity(id) instanceof CultivatorNpc npc) || !npc.isAlive()) {
				RETURNING.remove(id);
				OVER_CAMP.remove(id);
				continue;
			}
			if (!npc.isQiFlying()) npc.setQiFlying(true);
			if (npc.getTarget() != null) npc.setTarget(null);
			if (npc.position().distanceToSqr(over) < 4 * 4) OVER_CAMP.add(id);
			// Out from the opening to before it, across to over the camp, then down.
			boolean nearGate = npc.position().distanceToSqr(mouth) < (APPROACH - 2) * (APPROACH - 2);
			Vec3 to = OVER_CAMP.contains(id) ? home : nearGate ? before : over;
			npc.flyToward(to, RUSH_SPEED);
			if (npc.position().distanceToSqr(home) < 4 * 4) {
				npc.setSettledAt(camp);
				npc.setQiFlying(false);
				RETURNING.remove(id);
				OVER_CAMP.remove(id);
			}
		}
	}

	// --- The guards ---

	/** The Southern Heavenly Gate's guards' posts: either side of its great portal, before it and behind it. */
	static BlockPos post(int i, int y) {
		int side = i % 2 == 0 ? -1 : 1, z = i < 2 ? -6 : 5;
		return new BlockPos(HEAVEN_X + side * (HALF - 2), y + 1, HEAVEN_Z + z);
	}

	/** Whether {@code npc} guards the Southern Heavenly Gate: an Immortal Officer settled at a post there. */
	public static boolean isGuard(CultivatorNpc npc) {
		BlockPos home = npc.settledAt();
		return npc.getTitle() == NpcTitle.IMMORTAL_OFFICER && home != null && ModDimensions.isImmortalRealm(npc.level().dimension())
				&& Math.abs(home.getX() - HEAVEN_X) <= HALF + PILLAR + 4 && Math.abs(home.getZ() - HEAVEN_Z) <= 6;
	}

	/** Every second while the Gate's ground is loaded: its guards kept to {@link #GUARDS}, all at once the first time, then one an hour. */
	private static void keepGuards(ServerLevel immortal, Data data) {
		if (!data.heavenBuilt) return;
		BlockPos middle = new BlockPos(HEAVEN_X, data.heavenY, HEAVEN_Z);
		if (!immortal.isPositionEntityTicking(middle)) return;
		long now = immortal.getGameTime();
		List<CultivatorNpc> guards = immortal.getEntitiesOfClass(CultivatorNpc.class, new AABB(middle).inflate(48, 32, 48),
				npc -> npc.isAlive() && isGuard(npc));
		if (guards.size() >= GUARDS) return;
		if (data.guarded && now - data.lastGuard < GUARD_EVERY) return;
		Set<BlockPos> manned = new HashSet<>();
		for (CultivatorNpc guard : guards) manned.add(guard.settledAt());
		RandomSource random = immortal.getRandom();
		for (int i = 0; i < GUARDS; i++) {
			BlockPos post = post(i, data.heavenY);
			if (manned.contains(post)) continue;
			CultivatorNpc npc = CultivatorNpc.createFor(immortal);
			if (npc == null) break;
			npc.moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, post.getZ() < HEAVEN_Z ? 180 : 0, 0);
			npc.becomeRogue(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.byIndex(random.nextInt(2))), Alignment.RIGHTEOUS_ABOVE + 20 + random.nextInt(60), random);
			npc.setTitle(NpcTitle.IMMORTAL_OFFICER);
			npc.setSettledAt(post);
			immortal.addFreshEntity(npc);
			if (data.guarded) break; // one an hour once they have been set at their posts
		}
		data.guarded = true;
		data.lastGuard = now;
		data.setDirty();
	}

	// --- Building ---

	/** Builds either gate that isn't yet (called when the server starts), and puts the light in or out as the Gate stands now. */
	public static void ensure(MinecraftServer server) {
		if (server == null) return;
		Data data = Data.get(server);
		ServerLevel greatVoid = server.getLevel(ModDimensions.GREAT_VOID), immortal = server.getLevel(ModDimensions.IMMORTAL_REALM);
		try {
			if (greatVoid != null && !data.skyBuilt) {
				long start = System.currentTimeMillis();
				buildGate(greatVoid, SKY_X, SKY_Y, SKY_Z, true);
				data.skyBuilt = true;
				data.skyY = SKY_Y;
				data.setDirty();
				DefyingTheHeavens.LOGGER.info("The Immortal Gate built over the void sea at {}, {}, {} in {} ms", SKY_X, SKY_Y, SKY_Z, System.currentTimeMillis() - start);
			}
			if (immortal != null && !data.heavenBuilt) {
				long start = System.currentTimeMillis();
				int y = floorAt(immortal, HEAVEN_X, HEAVEN_Z);
				plaza(immortal, HEAVEN_X, y, HEAVEN_Z);
				buildGate(immortal, HEAVEN_X, y, HEAVEN_Z, false);
				data.heavenBuilt = true;
				data.heavenY = y;
				data.setDirty();
				DefyingTheHeavens.LOGGER.info("The Southern Heavenly Gate built at {}, {}, {} in {} ms", HEAVEN_X, y, HEAVEN_Z, System.currentTimeMillis() - start);
			}
			// Restarted while it stood open, or since it shut: the light as the Gate stands now.
			if (greatVoid != null && immortal != null && data.lit != isOpen(server)) {
				boolean open = isOpen(server);
				if (data.skyBuilt) light(greatVoid, SKY_X, data.skyY, SKY_Z, open);
				if (data.heavenBuilt) light(immortal, HEAVEN_X, data.heavenY, HEAVEN_Z, open);
				data.lit = open;
				data.setDirty();
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Could not build the Immortal Gate", e);
		}
	}

	/** The ground's level at a column (its chunks loaded here: at the server's start, once), the realm's Gate ground's if it can't be read. */
	private static int floorAt(ServerLevel level, int x, int z) {
		List<Integer> heights = new ArrayList<>();
		for (int dx = -16; dx <= 16; dx += 16) {
			for (int dz = -16; dz <= 16; dz += 16) {
				level.getChunk((x + dx) >> 4, (z + dz) >> 4);
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
				if (y > ModDimensions.IMMORTAL_CLOUD_Y) heights.add(y);
			}
		}
		if (heights.isEmpty()) return ModNoiseSettings.GATE_Y;
		Collections.sort(heights);
		return heights.get(heights.size() / 2) - 1;
	}

	private static final BlockState PILLAR_BLOCK = Blocks.QUARTZ_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	private static final BlockState BAND = Blocks.GOLD_BLOCK.defaultBlockState();
	private static final BlockState PLINTH = Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState();
	private static final BlockState BEAM = Blocks.QUARTZ_BRICKS.defaultBlockState();
	private static final BlockState FLOOR = Blocks.SMOOTH_QUARTZ.defaultBlockState();
	/** Jade-green glazed tiles (tour 1: gilded copper read as rust against the golden sky). */
	private static final BlockState ROOF = Blocks.DARK_PRISMARINE.defaultBlockState();
	private static final BlockState ROOF_SLAB = Blocks.DARK_PRISMARINE_SLAB.defaultBlockState();
	private static final BlockState CLOUD = Blocks.WHITE_CONCRETE.defaultBlockState(), CLOUD_PUFF = Blocks.WHITE_WOOL.defaultBlockState();
	/**
	 * The gateway's three portals and four pillars (east-west, as {@link #HALF} and {@link #PILLAR}): the great middle one between the inner
	 * pillars, a lesser one each side out to the outer pillars ({@link #SIDE_WIDTH} wide, {@link #SIDE_HEIGHT} tall); its platform
	 * {@link #REACH} either way and {@link #DEPTH} before and behind.
	 */
	static final int SIDE_WIDTH = 9, SIDE_HEIGHT = 16, REACH = HALF + 2 * PILLAR + SIDE_WIDTH + 4, DEPTH = 8;
	/** How far round the Southern Heavenly Gate's middle its plaza is paved ({@link #plaza}). */
	public static final int PLAZA_RADIUS = REACH + 10;

	/**
	 * A gate whose threshold is ({@code x}, {@code y}, {@code z}), running east-west: a gateway of three portals and four pillars (tour 1's
	 * two-pillar gate looked bare). Its pillars white jade banded with gold on plinths; the great middle portal (the Gate's light fills
	 * it) under a thick beam with a golden plaque on both faces, a lesser portal each side under its own beam, a lantern hung in each; a
	 * double-eaved roof of jade-green tiles over the middle, its eaves' tips upturned and gold, its ridge gold-capped, and a single eave
	 * over each side; all on a platform of white jade, and that ({@code cloud}) on a bank of cloud in open sky.
	 */
	static void buildGate(ServerLevel level, int x, int y, int z, boolean cloud) {
		for (int cx = (x - REACH - 12) >> 4; cx <= (x + REACH + 12) >> 4; cx++) {
			for (int cz = (z - DEPTH - 10) >> 4; cz <= (z + DEPTH + 10) >> 4; cz++) level.getChunk(cx, cz);
		}
		if (cloud) cloudBank(level, x, y, z, level.getRandom());
		// The platform: white jade, its rim chiseled.
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -DEPTH; dz <= DEPTH - 1; dz++) {
				boolean rim = Math.abs(dx) == REACH || dz == -DEPTH || dz == DEPTH - 1;
				put(level, x + dx, y, z + dz, rim ? PLINTH : FLOOR);
			}
		}
		int innerFrom = HALF + 1, innerTo = HALF + PILLAR, outerFrom = innerTo + SIDE_WIDTH + 1, outerTo = outerFrom + PILLAR - 1;
		int mainBeam = y + HEIGHT + 1, sideBeam = y + SIDE_HEIGHT + 1;
		for (int side = -1; side <= 1; side += 2) {
			// The inner pillars, up to the middle roof; the outer ones up to the side roofs.
			pillar(level, x, y, z, side, innerFrom, innerTo, mainBeam + 2);
			pillar(level, x, y, z, side, outerFrom, outerTo, sideBeam + 1);
			// The side portal's beam, a small plaque in its middle, a lantern hung under it.
			int mid = side * (innerTo + 1 + SIDE_WIDTH / 2);
			for (int dx = innerTo + 1; dx <= outerTo + 1; dx++) {
				for (int dy = 0; dy <= 1; dy++) {
					for (int dz = -2; dz <= 1; dz++) {
						boolean plaque = Math.abs(side * dx - mid) <= 1 && dy == 0 && (dz == -2 || dz == 1);
						put(level, x + side * dx, sideBeam + dy, z + dz, plaque ? BAND : BEAM);
					}
				}
			}
			put(level, x + mid, sideBeam - 1, z - 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
			put(level, x + mid, sideBeam - 1, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
			// The side roof: a single eave, its tips upturned and gold, a short ridge capped gold at its outer end.
			int roof = sideBeam + 2, from = innerTo + 1, to = outerTo + 3;
			for (int dx = from; dx <= to; dx++) {
				for (int dz = -4; dz <= 3; dz++) {
					boolean edge = dz == -4 || dz == 3 || dx == to;
					put(level, x + side * dx, roof, z + dz, !edge ? ROOF : dz == -4 ? stair(Direction.SOUTH) : dz == 3 ? stair(Direction.NORTH)
							: stair(side < 0 ? Direction.EAST : Direction.WEST));
				}
				if (dx <= to - 2) {
					for (int dz = -1; dz <= 0; dz++) put(level, x + side * dx, roof + 1, z + dz, dx == to - 2 ? BAND : ROOF_SLAB.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
				}
			}
			put(level, x + side * to, roof + 1, z - 4, BAND);
			put(level, x + side * to, roof + 1, z + 3, BAND);
		}
		// The middle portal's beam, its great plaque gold on both faces.
		for (int dx = -innerTo - 2; dx <= innerTo + 2; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				for (int dz = -2; dz <= 1; dz++) {
					boolean plaque = Math.abs(dx) <= 5 && dy == 1 && (dz == -2 || dz == 1);
					put(level, x + dx, mainBeam + dy, z + dz, plaque ? BAND : BEAM);
				}
			}
		}
		// The middle roof: a lower eave over every side, a band of white jade trimmed gold, an upper eave, a ridge capped gold.
		int lower = mainBeam + 3, lowerReach = innerTo + 5;
		eave(level, x, lower, z, lowerReach, 5, 4);
		for (int dx = -innerTo + 1; dx <= innerTo - 1; dx++) {
			for (int dz = -2; dz <= 1; dz++) put(level, x + dx, lower + 1, z + dz, (dz == -2 || dz == 1) && Math.abs(dx) % 4 == 0 ? BAND : BEAM);
		}
		int upper = lower + 2, upperReach = innerTo + 2;
		eave(level, x, upper, z, upperReach, 4, 3);
		for (int dx = -upperReach + 2; dx <= upperReach - 2; dx++) {
			for (int dz = -2; dz <= 1; dz++) {
				boolean edge = dz == -2 || dz == 1;
				put(level, x + dx, upper + 1, z + dz, edge ? stair(dz < 0 ? Direction.SOUTH : Direction.NORTH) : ROOF);
			}
			for (int dz = -1; dz <= 0; dz++) {
				boolean end = Math.abs(dx) >= upperReach - 3;
				put(level, x + dx, upper + 2, z + dz, end ? BAND : ROOF_SLAB.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
			}
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			put(level, x + sx * (upperReach - 3), upper + 3, z - 1, BAND); // the ridge's end ornaments
			put(level, x + sx * (upperReach - 3), upper + 3, z, BAND);
		}
		// Clear the portals (nothing generated in them): the middle one the Gate's light fills, the side ones open.
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dy = 1; dy <= HEIGHT; dy++) {
				for (int dz = -2; dz <= 1; dz++) put(level, x + dx, y + dy, z + dz, Blocks.AIR.defaultBlockState());
			}
		}
		for (int side = -1; side <= 1; side += 2) {
			for (int dx = innerTo + 1; dx <= innerTo + SIDE_WIDTH; dx++) {
				for (int dy = 1; dy <= SIDE_HEIGHT; dy++) {
					for (int dz = -2; dz <= 1; dz++) {
						BlockPos at = new BlockPos(x + side * dx, y + dy, z + dz);
						if (!level.getBlockState(at).is(Blocks.LANTERN)) level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	/** A pillar of white jade banded with gold on its plinth, from {@code from} to {@code to} out on {@code side}, up to {@code top}. */
	private static void pillar(ServerLevel level, int x, int y, int z, int side, int from, int to, int top) {
		for (int dx = from; dx <= to; dx++) {
			for (int dz = -3; dz <= 2; dz++) {
				put(level, x + side * dx, y + 1, z + dz, PLINTH);
				if (dz >= -2 && dz <= 1) put(level, x + side * dx, y + 2, z + dz, PLINTH);
			}
			for (int dy = 3; dy <= top - y; dy++) {
				BlockState block = dy % 6 == 0 ? BAND : PILLAR_BLOCK;
				for (int dz = -2; dz <= 1; dz++) put(level, x + side * dx, y + dy, z + dz, block);
			}
		}
	}

	/**
	 * An eave at {@code y} over the middle, {@code reach} either way and from {@code back} behind to {@code front} before: tiles inside,
	 * its rim stepped down outward, its four tips raised a block in gold.
	 */
	private static void eave(ServerLevel level, int x, int y, int z, int reach, int back, int front) {
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -back; dz <= front; dz++) {
				boolean edge = dz == -back || dz == front || Math.abs(dx) == reach;
				put(level, x + dx, y, z + dz, !edge ? ROOF : dz == -back ? stair(Direction.SOUTH) : dz == front ? stair(Direction.NORTH)
						: stair(dx < 0 ? Direction.EAST : Direction.WEST));
			}
		}
		for (int sx = -1; sx <= 1; sx += 2) {
			put(level, x + sx * reach, y + 1, z - back, BAND);
			put(level, x + sx * reach, y + 1, z + front, BAND);
		}
	}

	private static BlockState stair(Direction facing) {
		return Blocks.DARK_PRISMARINE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing);
	}

	/** A bank of cloud under a gate in open sky: billowing white, broadest under its floor, thinning out at its rims. */
	private static void cloudBank(ServerLevel level, int x, int y, int z, RandomSource random) {
		int rx = REACH + 10, rz = DEPTH + 8, ry = 6;
		long salt = random.nextLong();
		for (int dx = -rx; dx <= rx; dx++) {
			for (int dz = -rz; dz <= rz; dz++) {
				for (int dy = -ry; dy <= 0; dy++) {
					double fx = dx / (double) rx, fz = dz / (double) rz, fy = (dy + 1.5) / (double) ry;
					double puff = 0.18 * (hash(salt, x + dx >> 2, z + dz >> 2) - 0.5);
					double d = fx * fx + fz * fz + fy * fy;
					if (d > 1 + puff) continue;
					put(level, x + dx, y + dy, z + dz, d > 0.72 + puff ? CLOUD_PUFF : CLOUD);
				}
			}
		}
	}

	private static double hash(long salt, int a, int b) {
		long h = salt ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL);
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (h >>> 11) / (double) (1L << 53);
	}

	/**
	 * The Southern Heavenly Gate's plaza: its ground made level and paved in white jade (a rim, a border of bricks, a ring of green jade, a
	 * road of bricks along the Gate's axis, a lattice of bricks between), cleared over it, and the trees about it cleared too (tour 1: blossom
	 * crowded the gate). The land two chunks further round is made first (decorated), so nothing grown later spills onto it (tour 2: flowers
	 * on the paving, laid by the ground made after it).
	 */
	private static void plaza(ServerLevel level, int x, int y, int z) {
		int r = PLAZA_RADIUS, clear = r + 8, made = clear + 32;
		for (int cx = (x - made) >> 4; cx <= (x + made) >> 4; cx++) {
			for (int cz = (z - made) >> 4; cz <= (z + made) >> 4; cz++) level.getChunk(cx, cz);
		}
		BlockState bricks = Blocks.QUARTZ_BRICKS.defaultBlockState(), jade = ModBlocks.JADE_STONE.defaultBlockState();
		for (int dx = -clear; dx <= clear; dx++) {
			for (int dz = -clear; dz <= clear; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > clear + 0.5) continue;
				if (d <= r + 0.5) {
					for (int dy = -6; dy < 0; dy++) {
						BlockPos at = new BlockPos(x + dx, y + dy, z + dz);
						if (level.getBlockState(at).canBeReplaced()) level.setBlock(at, Blocks.CALCITE.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
					BlockState pave = d > r - 1.5 ? PLINTH : d > r - 3 ? FLOOR : d > r - 4.5 ? bricks : Math.abs(d - (r - 12)) < 0.8 ? jade
							: Math.abs(dx) <= 3 ? (Math.abs(dx) == 3 ? jade : bricks) : (Math.abs(dx) + Math.abs(dz)) % 6 == 0 ? bricks : FLOOR;
					put(level, x + dx, y, z + dz, pave);
					for (int dy = 1; dy <= 48; dy++) put(level, x + dx, y + dy, z + dz, Blocks.AIR.defaultBlockState());
				} else {
					// Round it, every tree taken down (its trunk and crown), so none overhangs the plaza.
					for (int dy = -4; dy <= 40; dy++) {
						BlockPos at = new BlockPos(x + dx, y + dy, z + dz);
						BlockState here = level.getBlockState(at);
						if (here.is(net.minecraft.tags.BlockTags.LEAVES) || here.is(net.minecraft.tags.BlockTags.LOGS)) level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		// The guards' posts: a lantern on a white jade post beside each.
		for (int i = 0; i < GUARDS; i++) {
			BlockPos post = post(i, y);
			int lx = post.getX() + (post.getX() < x ? -2 : 2);
			put(level, lx, y + 1, post.getZ(), Blocks.QUARTZ_PILLAR.defaultBlockState());
			put(level, lx, y + 2, post.getZ(), Blocks.QUARTZ_PILLAR.defaultBlockState());
			put(level, lx, y + 3, post.getZ(), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
		}
	}

	private static void put(ServerLevel level, int x, int y, int z, BlockState state) {
		level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
	}

	// --- Bookkeeping ---

	/** Dying in the passage ends the climb. */
	public static void onDeath(ServerPlayer p) {
		if (p != null) CLIMBS.remove(p.getUUID());
	}

	/** Disconnect: the climb is let go (back in, it begins again where they are). */
	public static void forget(UUID id) {
		CLIMBS.remove(id);
		TOLD.remove(id);
	}

	/** Server stopping, before the save: those still flying to the Gate, or back from it, are of the camp again where they are. */
	public static void settleAll(MinecraftServer server) {
		ServerLevel greatVoid = server.getLevel(ModDimensions.GREAT_VOID);
		if (greatVoid == null) return;
		BlockPos camp = NirvanaCamp.middle(greatVoid);
		for (UUID id : new ArrayList<>(RUSHING)) if (greatVoid.getEntity(id) instanceof CultivatorNpc npc) npc.setSettledAt(camp);
		for (UUID id : new ArrayList<>(RETURNING)) if (greatVoid.getEntity(id) instanceof CultivatorNpc npc) npc.setSettledAt(camp);
	}

	/** Server stopped (not saved): nobody climbs, nobody rushes. */
	public static void clear() {
		CLIMBS.clear();
		RUSHING.clear();
		RETURNING.clear();
		IN_LINE.clear();
		OVER_CAMP.clear();
		TOLD.clear();
	}

	/**
	 * Whether each gate is built and at which height, whether their light is in, the cycle last warned of, a command's opening or
	 * shutting out of turn, and the guards' keeping; kept with the Overworld's data (the Gate is both realms').
	 */
	public static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_immortal_gate";
		boolean skyBuilt, heavenBuilt, lit, guarded;
		int skyY = SKY_Y, heavenY = ModNoiseSettings.GATE_Y;
		long warnedCycle = -1, forcedUntil, shutUntil, lastGuard;

		static Data get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			data.skyBuilt = tag.getBoolean("SkyBuilt");
			data.heavenBuilt = tag.getBoolean("HeavenBuilt");
			data.lit = tag.getBoolean("Lit");
			data.guarded = tag.getBoolean("Guarded");
			data.skyY = tag.contains("SkyY") ? tag.getInt("SkyY") : SKY_Y;
			data.heavenY = tag.contains("HeavenY") ? tag.getInt("HeavenY") : ModNoiseSettings.GATE_Y;
			data.warnedCycle = tag.contains("WarnedCycle") ? tag.getLong("WarnedCycle") : -1;
			data.forcedUntil = tag.getLong("ForcedUntil");
			data.shutUntil = tag.getLong("ShutUntil");
			data.lastGuard = tag.getLong("LastGuard");
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			tag.putBoolean("SkyBuilt", skyBuilt);
			tag.putBoolean("HeavenBuilt", heavenBuilt);
			tag.putBoolean("Lit", lit);
			tag.putBoolean("Guarded", guarded);
			tag.putInt("SkyY", skyY);
			tag.putInt("HeavenY", heavenY);
			tag.putLong("WarnedCycle", warnedCycle);
			tag.putLong("ForcedUntil", forcedUntil);
			tag.putLong("ShutUntil", shutUntil);
			tag.putLong("LastGuard", lastGuard);
			return tag;
		}
	}

	private ImmortalGate() {}
}
