package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Void Breach (the user's pick, 2026-10-25): the way up from the Jade Firmament (the Upper Realm) into the Great Void Realm, and
 * the ways back down.
 * <ul>
 *   <li><b>Torn open.</b> From Void Shattering on (either path's, TwoPaths#higherRank), the Spatial Tear torn high in the Jade
 *   Firmament's sky ({@link #TEAR_Y} and up) opens a breach instead of a way onto the gap ({@link #open}; {@link #BREACH_QI} of the
 *   pool). Its opener holds it open while they stay by it ({@link #HOLD_REACH}); let go (or gone through), it lingers
 *   {@link #LINGER} for those who follow, open {@link #MAX_OPEN} at most.</li>
 *   <li><b>Who passes.</b> Void Shattering and above; Dao Integration only through a breach someone stronger holds open (the user's
 *   rule: every breach's opener is), below Dao Integration no one: the void's pressure turns them back ({@link #mayPass}).</li>
 *   <li><b>The climb.</b> Shattered space (ModDimensions#VOID_BREACH): no flight holds there (QiFlight); the breach's current carries
 *   one from {@link #ENTER_Y} to {@link #EXIT_Y} in {@link #CLIMB_SECONDS} seconds, steering as one may. The void presses all the
 *   while ({@link #PRESSURE}, armour no help); shards of broken space fall where light first marks them ({@link #SHARD_DAMAGE}); and
 *   Vestiges of the Void come out of the dark for the climbers, of each one's own might (VoidVestige#setMight).</li>
 *   <li><b>Out.</b> At the top, onto the Great Void's land nearest where the breach was torn (SpatialArrivals#target, its continents'
 *   mask; loaded ahead while one climbs).</li>
 *   <li><b>Down.</b> Off a continent's edge, falling below {@link #FALL_Y}; or by a Spatial Tear in the Great Void, onto the Jade
 *   Firmament ({@link #openDown}): either way into its sky over its nearest island, the landing harmless ({@link #fallOut}).</li>
 * </ul>
 */
public final class VoidBreach {
	/** A breach is torn only this high in the Jade Firmament's sky, above its islands (lower down, a tear opens onto the gap as ever). */
	public static final int TEAR_Y = 448;
	/** Into the breach's shattered space at this height; out into the Great Void at this one. */
	public static final int ENTER_Y = 40, EXIT_Y = 340;
	/** The climb's length, seconds: the current (Levitation of {@link #LIFT_AMPLIFIER}) carries one up at that pace. */
	public static final int CLIMB_SECONDS = 30;
	/** Levitation's amplifier for the current: a twentieth of a block a tick for each level, ten levels: half a block a tick. */
	static final int LIFT_AMPLIFIER = 9;
	/** Tearing a breach costs this share of the pool (a tear onto the gap a quarter, SpatialTrialHandler#TEAR_IN_QI). */
	public static final double BREACH_QI = 0.5;
	/**
	 * The void's pressure a second in the breach, armour no help (the gap's, ModDamageTypes#spatialPressure): some 300 over the
	 * climb before mending; a Void Shattering Early (1,460 health) hardly feels it, a Dao Integration riding along (380 to 490) needs
	 * its wits and its pills.
	 */
	public static final float PRESSURE = 10f;
	/** A shard of broken space striking where it was marked: this much to anyone within {@link #SHARD_REACH} of it. */
	public static final float SHARD_DAMAGE = 60f;
	static final double SHARD_REACH = 2.0;
	/** A shard is marked this long (ticks) before it strikes; one falls near each climber every {@link #SHARD_EVERY} ticks or so. */
	static final int SHARD_WARNING = 30, SHARD_EVERY = 50, SHARD_SPREAD = 50;
	/** A breach is held open while its opener stays this near it; let go, it lingers this long (ticks); open at most this long. */
	static final double HOLD_REACH = 24;
	static final int LINGER = 10 * 20, MAX_OPEN = 60 * 20;
	/** A breach's mouth (it is drawn larger than a tear, SpatialTearRenderer), and a way down's (a tear's). */
	static final double BREACH_REACH = 1.8, BREACH_HEIGHT = 5.5, TEAR_REACH = 0.8, TEAR_HEIGHT = 2.6;
	/** Falling below this in the Great Void, one falls out of it into the Jade Firmament's sky. */
	public static final int FALL_Y = ModDimensions.GREAT_VOID_MIN_Y + 24;
	/** Coming down into the Jade Firmament this far above the top of the island one falls toward, no higher than its sky's top. */
	static final int DROP_ABOVE = 40, DROP_MAX_Y = 505;
	/** Vestiges of the Void out of the dark for the climbers: a try this often (ticks), this likely, no more than this many about one. */
	static final int VESTIGE_EVERY = 100, VESTIGES_NEAR = 2;
	static final float VESTIGE_CHANCE = 0.35f;
	/** A breach's Vestiges are of a Heavenly Being Early's size (some 40 blocks long), with their prey's might. */
	static final int VESTIGE_SIZE = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY);
	/** No one may ride a breach below this; tearing one wants {@link PlayerCultivation#GREAT_VOID_REALMS_FROM}. */
	static final int RIDE_RANK = PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY);
	static final int TEAR_RANK = PlayerCultivation.rank(PlayerCultivation.GREAT_VOID_REALMS_FROM, Stage.EARLY);
	/** No ground in the Great Void near a landing: a platform at this height. */
	private static final int PLATFORM_Y = 210;

	/** A breach (or a way down) standing open: its tear, where, whose, when opened. */
	private record Breach(UUID entity, ServerLevel level, UUID opener, Vec3 at, long opened, boolean down) {}

	/** A shard marked to strike at {@code at} when the breach's clock reaches {@code strikes}. */
	private record Shard(Vec3 at, long strikes) {}

	/** One climbing the breach: ticks in it, the next shard's time, those marked, and where it will come out. */
	private static final class Climb {
		int ticks;
		long nextShard;
		final List<Shard> shards = new ArrayList<>();
		BlockPos arrival;
	}

	private static final List<Breach> BREACHES = new ArrayList<>();
	private static final Map<UUID, Climb> CLIMBS = new HashMap<>();
	/** When each was last told the breach turned them back (game time), so the telling isn't every tick. */
	private static final Map<UUID, Long> TURNED_BACK = new HashMap<>();

	/** Climbing a breach now. */
	public static boolean isClimbing(UUID id) {
		return CLIMBS.containsKey(id);
	}

	// --- Tearing ---

	/** Whether {@code p}'s Spatial Tear opens a breach: of Void Shattering or above, high in the Jade Firmament's sky. */
	public static boolean tearsBreach(ServerPlayer p, PlayerCultivation c) {
		return ModDimensions.isUpperRealm(p.level().dimension()) && p.getY() >= TEAR_Y && TwoPaths.higherRank(c, false) >= TEAR_RANK;
	}

	/** Whether {@code walker} may ride a breach: Dao Integration and above (every breach is held by someone stronger), or one in creative. */
	public static boolean mayPass(ServerPlayer walker) {
		return walker.isCreative() || TwoPaths.higherRank(CultivationManager.get(walker), false) >= RIDE_RANK;
	}

	/** {@code p} tears the void open before them: a breach up into the Great Void (paid for by SpatialTrialHandler#tear). */
	static void open(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		close(p.getUUID());
		Vec3 at = p.position().add(ahead(p).scale(3.5));
		SpatialTearEntity tear = SpatialTearEntity.create(level, at, p.getYRot() + 180, SpatialTearEntity.Leads.VOID_BREACH);
		level.addFreshEntity(tear);
		BREACHES.add(new Breach(tear.getUUID(), level, p.getUUID(), at, level.getGameTime(), false));
		level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.2f, 0.5f);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0f, 0.3f);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 2.5, at.z, 120, 1.2, 2.2, 1.2, 0.4);
		p.displayClientMessage(Component.translatable(ModLang.MSG_BREACH_OPEN).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	/** {@code p} tears space open in the Great Void: a way down onto the Jade Firmament below (paid for by SpatialTrialHandler#tear). */
	static void openDown(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		close(p.getUUID());
		Vec3 at = p.position().add(ahead(p).scale(2.2));
		SpatialTearEntity tear = SpatialTearEntity.create(level, at, p.getYRot() + 180, SpatialTearEntity.Leads.JADE_FIRMAMENT);
		level.addFreshEntity(tear);
		BREACHES.add(new Breach(tear.getUUID(), level, p.getUUID(), at, level.getGameTime(), true));
		level.playSound(null, at.x, at.y, at.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5f, 1.6f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_DOWN).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	/** Which way {@code p} faces across the ground (straight ahead if they look straight up or down). */
	private static Vec3 ahead(ServerPlayer p) {
		Vec3 ahead = p.getLookAngle().multiply(1, 0, 1);
		return ahead.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : ahead.normalize();
	}

	/** Shuts every breach {@code opener} has standing open. */
	private static void close(UUID opener) {
		for (Iterator<Breach> it = BREACHES.iterator(); it.hasNext(); ) {
			Breach breach = it.next();
			if (!breach.opener().equals(opener)) continue;
			if (breach.level().getEntity(breach.entity()) instanceof SpatialTearEntity tear) tear.discard();
			it.remove();
		}
	}

	// --- Each tick ---

	public static void tick(MinecraftServer server) {
		if (server == null) return;
		tickBreaches(server);
		ServerLevel climb = server.getLevel(ModDimensions.VOID_BREACH);
		// Gone from the breach (out, dead, logged out): their climb is let go (back in, it begins again where they are).
		CLIMBS.keySet().removeIf(id -> {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			return p == null || climb == null || p.level() != climb;
		});
		if (climb == null) return;
		for (ServerPlayer p : new ArrayList<>(climb.players())) {
			try {
				tickClimber(server, climb, p);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Void Breach tick failed for {}", p.getScoreboardName(), e);
			}
		}
	}

	/** Each breach held open (or let go), and whoever has stepped into one carried through. */
	private static void tickBreaches(MinecraftServer server) {
		if (BREACHES.isEmpty()) return;
		for (Breach breach : new ArrayList<>(BREACHES)) {
			Entity entity = breach.level().getEntity(breach.entity());
			if (!(entity instanceof SpatialTearEntity tear) || tear.isRemoved()) {
				BREACHES.remove(breach);
				continue;
			}
			if (!breach.down()) {
				ServerPlayer opener = server.getPlayerList().getPlayer(breach.opener());
				boolean held = opener != null && opener.isAlive() && opener.level() == breach.level() && opener.position().distanceTo(breach.at()) <= HOLD_REACH;
				int until = Math.min(MAX_OPEN, tear.tickCount + LINGER + SpatialTearEntity.CLOSE_TICKS);
				if (held) tear.holdOpen(until);
			}
			if (tear.openness(0) < 0.5f) continue;
			double reach = breach.down() ? TEAR_REACH : BREACH_REACH, height = breach.down() ? TEAR_HEIGHT : BREACH_HEIGHT;
			AABB mouth = new AABB(breach.at().x - reach, breach.at().y - 0.5, breach.at().z - reach, breach.at().x + reach, breach.at().y + height,
					breach.at().z + reach);
			for (ServerPlayer walker : breach.level().getEntitiesOfClass(ServerPlayer.class, mouth)) {
				if (!walker.isAlive() || walker.isSpectator() || SoulProjection.isAwayFromBody(walker.getUUID()) || Incorporeal.isRuledGhost(walker)) continue;
				if (breach.down()) fallOut(walker);
				else walkInto(server, walker, breach);
			}
		}
	}

	/** {@code walker} stepped into {@code breach}: up into its shattered space, if the void lets them. */
	private static void walkInto(MinecraftServer server, ServerPlayer walker, Breach breach) {
		if (CLIMBS.containsKey(walker.getUUID())) return;
		if (!mayPass(walker)) {
			long now = walker.level().getGameTime();
			Long told = TURNED_BACK.get(walker.getUUID());
			if (told == null || now - told > 60) {
				TURNED_BACK.put(walker.getUUID(), now);
				walker.displayClientMessage(Component.translatable(ModLang.MSG_BREACH_TURNED_BACK).withStyle(ChatFormatting.RED), true);
			}
			Vec3 back = walker.position().subtract(breach.at()).multiply(1, 0, 1);
			back = back.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : back.normalize();
			walker.push(back.x * 0.8, 0.2, back.z * 0.8);
			walker.hurtMarked = true;
			return;
		}
		enterClimb(server, walker, breach.at().x, breach.at().z);
	}

	/** Into the breach's shattered space at {@link #ENTER_Y}, over (x, z): the climb begins, and the land it ends on is asked for. */
	static void enterClimb(MinecraftServer server, ServerPlayer p, double x, double z) {
		ServerLevel climb = server.getLevel(ModDimensions.VOID_BREACH);
		if (climb == null) {
			DefyingTheHeavens.LOGGER.error("Void Breach dimension is missing; cannot carry {} into it", p.getScoreboardName());
			return;
		}
		TribulationManager.abandon(p);
		MeditationManager.stop(p, false);
		Climb c = new Climb();
		c.nextShard = climb.getGameTime() + 60;
		CLIMBS.put(p.getUUID(), c);
		p.teleportTo(climb, x, ENTER_Y, z, p.getYRot(), 0);
		p.connection.resetPosition();
		p.fallDistance = 0;
		RealmSuppressionHandler.update(p);
		prepareArrival(server, p, c);
		p.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6f, 0.4f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_BREACH_ENTER, CLIMB_SECONDS).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	/** The Great Void's land the climb ends on (found by numbers, nothing generated to look), asked to load meanwhile (30 s a time). */
	private static void prepareArrival(MinecraftServer server, ServerPlayer p, Climb c) {
		ServerLevel greatVoid = server.getLevel(ModDimensions.GREAT_VOID);
		if (greatVoid == null) return;
		if (c.arrival == null) c.arrival = SpatialArrivals.target(greatVoid, Mth.floor(p.getX()), Mth.floor(p.getZ()));
		SpatialArrivals.prepare(greatVoid, c.arrival);
	}

	/** A tick of {@code p}'s climb: the current, the pressure, the shards, the Vestiges; at the top, out into the Great Void. */
	private static void tickClimber(MinecraftServer server, ServerLevel climb, ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator()) return;
		Climb c = CLIMBS.get(p.getUUID());
		if (c == null) {
			// Relogged inside, or sent here by a command: climbing again from where they are.
			c = new Climb();
			c.nextShard = climb.getGameTime() + 60;
			CLIMBS.put(p.getUUID(), c);
			prepareArrival(server, p, c);
		}
		// Vanilla shields a player who just joined for 3 s: relogging over and over would outlast the void. Never here.
		((ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);
		if (p.isFallFlying()) p.stopFallFlying();
		if (p.getY() >= EXIT_Y) {
			arrive(server, p, c);
			return;
		}
		// The current: up at the climb's pace, whatever one does (no flight holds here: QiFlight lands everyone).
		MobEffectInstance lift = p.getEffect(MobEffects.LEVITATION);
		if (lift == null || lift.getAmplifier() != LIFT_AMPLIFIER || lift.getDuration() < 20) {
			p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, LIFT_AMPLIFIER, false, false, false));
		}
		long now = climb.getGameTime();
		if (++c.ticks % 20 == 0) {
			p.hurt(ModDamageTypes.spatialPressure(climb), PRESSURE);
			if (!p.isAlive()) return;
			p.displayClientMessage(Component.translatable(ModLang.MSG_BREACH_CLIMB, Math.max(0, EXIT_Y - Mth.floor(p.getY()))), true);
		}
		if (c.ticks % 400 == 0) prepareArrival(server, p, c); // the land's hold renewed for a long climb
		shards(climb, p, c, now);
		vestiges(climb, p, c);
	}

	/** Shards of broken space: one marked near {@code p} every so often (light where it will strike), striking when its time comes. */
	private static void shards(ServerLevel climb, ServerPlayer p, Climb c, long now) {
		RandomSource r = p.getRandom();
		if (now >= c.nextShard) {
			// Where the climber will be when it strikes, give or take: the current's way, and a little off it.
			Vec3 at = p.position().add(0, 0.5 * SHARD_WARNING, 0).add((r.nextDouble() - 0.5) * 3, 0, (r.nextDouble() - 0.5) * 3);
			c.shards.add(new Shard(at, now + SHARD_WARNING));
			c.nextShard = now + SHARD_EVERY + r.nextInt(SHARD_SPREAD);
		}
		for (Iterator<Shard> it = c.shards.iterator(); it.hasNext(); ) {
			Shard shard = it.next();
			Vec3 at = shard.at();
			if (now < shard.strikes()) {
				if ((shard.strikes() - now) % 3 == 0) climb.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 6, SHARD_REACH * 0.5, 1.0, SHARD_REACH * 0.5, 0.0);
				continue;
			}
			it.remove();
			climb.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 1, at.z, 2, 0.6, 0.6, 0.6, 0.0);
			climb.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 40, 1.4, 1.4, 1.4, 0.3);
			climb.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0f, 0.5f);
			AABB area = new AABB(at, at).inflate(SHARD_REACH, SHARD_REACH + 1, SHARD_REACH);
			for (ServerPlayer hit : climb.getEntitiesOfClass(ServerPlayer.class, area)) {
				if (hit.isAlive() && !hit.isSpectator()) hit.hurt(ModDamageTypes.spatialPressure(climb), SHARD_DAMAGE);
			}
		}
	}

	/** Now and then a Vestige of the Void out of the dark above, for {@code p}: of a Heavenly Being's size, {@code p}'s own might. */
	private static void vestiges(ServerLevel climb, ServerPlayer p, Climb c) {
		if (climb.getDifficulty() == Difficulty.PEACEFUL || p.isCreative() || c.ticks % VESTIGE_EVERY != 0) return;
		RandomSource r = p.getRandom();
		if (r.nextFloat() >= VESTIGE_CHANCE) return;
		if (climb.getEntitiesOfClass(VoidVestige.class, p.getBoundingBox().inflate(120)).size() >= VESTIGES_NEAR) return;
		VoidVestige vestige = ModEntities.VOID_VESTIGE.create(climb);
		if (vestige == null) return;
		vestige.setRank(VESTIGE_SIZE);
		vestige.setMight(Math.max(RIDE_RANK, TwoPaths.higherRank(CultivationManager.get(p), false)));
		double a = r.nextDouble() * Math.PI * 2;
		double y = Math.min(p.getY() + 50 + r.nextDouble() * 20, ModDimensions.VOID_BREACH_MIN_Y + ModDimensions.VOID_BREACH_HEIGHT - VoidVestige.BREACH_MARGIN - 5);
		vestige.moveTo(p.getX() + Math.cos(a) * 40, y, p.getZ() + Math.sin(a) * 40, r.nextFloat() * 360, 0);
		climb.addFreshEntity(vestige);
		p.displayClientMessage(Component.translatable(ModLang.MSG_VESTIGE_NEAR), true);
		climb.playSound(null, vestige.getX(), vestige.getY(), vestige.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3.0f, 0.4f);
	}

	/** Through: onto the Great Void's land nearest where the breach was torn. */
	private static void arrive(MinecraftServer server, ServerPlayer p, Climb c) {
		ServerLevel greatVoid = server.getLevel(ModDimensions.GREAT_VOID);
		if (greatVoid == null) {
			DefyingTheHeavens.LOGGER.error("Great Void dimension is missing; {} stays in the breach", p.getScoreboardName());
			return;
		}
		CLIMBS.remove(p.getUUID());
		p.removeEffect(MobEffects.LEVITATION);
		BlockPos column = c.arrival != null ? c.arrival : SpatialArrivals.target(greatVoid, Mth.floor(p.getX()), Mth.floor(p.getZ()));
		BlockPos ground = landing(greatVoid, column);
		p.teleportTo(greatVoid, ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, p.getYRot(), p.getXRot());
		p.connection.resetPosition();
		p.fallDistance = 0;
		p.setDeltaMovement(Vec3.ZERO);
		RealmSuppressionHandler.update(p);
		CultivationManager.markDirty(server);
		CultivationManager.sync(p);
		greatVoid.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.8, 1.2, 0.8, 0.3);
		p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_GREAT_VOID, (int) PlayerCultivation.GREAT_VOID_QI_MULTIPLIER).withStyle(ChatFormatting.GOLD));
	}

	/** Dry ground at or near {@code column} in the Great Void (its chunk loaded here if it must be), or a platform there (VoidCurrents too). */
	static BlockPos landing(ServerLevel level, BlockPos column) {
		try {
			level.getChunk(column.getX() >> 4, column.getZ() >> 4);
			BlockPos ground = SpatialArrivals.groundNear(level, column);
			if (ground != null) return ground;
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Arrival search in the Great Void failed around {}, {}", column.getX(), column.getZ(), e);
		}
		BlockPos centre = new BlockPos(column.getX(), PLATFORM_Y, column.getZ());
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlock(centre.offset(dx, -1, dz), Blocks.CALCITE.defaultBlockState(), 3);
				for (int dy = 0; dy < 3; dy++) level.setBlock(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
			}
		}
		return centre;
	}

	// --- Down ---

	/**
	 * Out of the Great Void (off a continent's edge, or through a way down) into the Jade Firmament's sky, {@link #DROP_ABOVE} over the
	 * island nearest below (found by numbers), the first landing harmless.
	 */
	public static void fallOut(ServerPlayer p) {
		ServerLevel firmament = p.server.getLevel(ModDimensions.UPPER_REALM);
		if (firmament == null) return;
		TribulationManager.abandon(p);
		BlockPos column = SpatialArrivals.target(firmament, Mth.floor(p.getX()), Mth.floor(p.getZ()));
		int top = firmament.getChunkSource().getGenerator().getBaseHeight(column.getX(), column.getZ(), Heightmap.Types.WORLD_SURFACE_WG, firmament,
				firmament.getChunkSource().randomState());
		int y = Math.min(DROP_MAX_Y, Math.max(top, ModPlacedFeatures.ISLAND_MIN_Y) + DROP_ABOVE);
		SpatialArrivals.prepare(firmament, column);
		PlayerCultivation c = CultivationManager.get(p);
		p.teleportTo(firmament, column.getX() + 0.5, y, column.getZ() + 0.5, p.getYRot(), p.getXRot());
		p.connection.resetPosition();
		p.fallDistance = 0;
		p.setDeltaMovement(Vec3.ZERO);
		c.setFallProtected(true);
		RealmSuppressionHandler.update(p);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_FALL_TO_FIRMAMENT).withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	// --- Bookkeeping ---

	/** Dying in the breach ends the climb. */
	public static void onDeath(ServerPlayer p) {
		if (p != null) CLIMBS.remove(p.getUUID());
	}

	/** Disconnect: the climb is let go (back in, it begins again where they are); their breaches stand till they close. */
	public static void forget(UUID id) {
		CLIMBS.remove(id);
		TURNED_BACK.remove(id);
	}

	/** Server stopped: no breach outlasts it. */
	public static void clear() {
		BREACHES.clear();
		CLIMBS.clear();
		TURNED_BACK.clear();
	}

	private VoidBreach() {}
}
