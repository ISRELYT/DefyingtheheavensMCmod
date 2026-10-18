package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The 60-second Spatial Pressure Trial in the Spatial Gap, for both directions:
 * ascension (Overworld rift -> Upper Realm) and descension (Upper Realm void -> Overworld sky).
 * <p>
 * The route alone decides which way the gap carries the player: ascending, they are lifted upward the whole way;
 * descending, they fall. Flying (creative or any other kind) and elytra gliding are cancelled inside the gap, and a
 * descending player loses any Levitation, so nobody can hover or go against the current.
 * <p>
 * The running timer is runtime-only and restarts from 60 s if the player relogs inside the gap; which direction they
 * were travelling is persisted in their cultivation data. Dying in the gap ends the trial.
 */
public final class SpatialTrialHandler {
	/**
	 * How one came into the gap: carried up from the Overworld's rift (ASCEND) or down out of the Upper Realm (DESCEND), each a
	 * timed trial; or through one's own tear in space (TRAVEL, Heavenly Being and above: {@link #tear}), free to fly, with no
	 * timer: a block crossed here is {@link #GAP_SCALE} in the world, rising through the stormy upper layers past
	 * {@link #CROSSING_Y} leads into the Upper Realm, sinking through the stormy lower layers below {@link #FLOOR_Y} drops one
	 * into the Overworld's sky. Tearing out again only ever leads back to the realm one came from ({@link #homeRealm}): to
	 * pass from one realm to the other, one goes up or down.
	 */
	public enum Route {
		NONE, ASCEND, DESCEND, TRAVEL;

		public static Route byName(String name) {
			for (Route route : values()) {
				if (route.name().equals(name)) return route;
			}
			return NONE;
		}
	}

	public static final int TRIAL_TICKS = 60 * 20;
	/**
	 * Per second; ignores armor, enchantments and effects (see ModDamageTypes). Health, regen, absorption and healing count.
	 * Against Qi Sustenance's 2 health/s regen (the ability switched on; off, the gap is harder), 60 hits net 131 damage: Heavenly Being - Grand Perfection (137 max
	 * health), the peak of the lower realms, is the first stage that endures it unaided; Late (128) falls at ~59 s.
	 * Four Axis (200+) passes with room to spare. A golden apple or two carries a Late cultivator through.
	 */
	public static final float PRESSURE_DAMAGE = 4.15f;

	private static final int ASCENT_ENTRY_Y = 96;
	private static final int DESCENT_ENTRY_Y = 300;
	/** Out of the gap into the Overworld's sky: above its build limit, below where rising leads out into space (AstralRealm#EXIT_Y). */
	private static final int OVERWORLD_DROP_Y = 330;
	/** The gap is floorless: below this Y the player is lifted back up mid-fall, keeping their speed, so they never hit the void. */
	private static final int LOOP_BELOW_Y = 64;
	/** ...and has no ceiling: above this Y a rising player is dropped back down mid-climb, keeping their speed. */
	private static final int LOOP_ABOVE_Y = 320;
	/** How far the loop moves a player; the gap repeats every this many blocks (see {@link SpatialStorms}). */
	public static final int LOOP_LIFT = 192;
	/**
	 * Ascending, the gap carries the player up with a hidden Levitation effect of this amplifier (a steady ~1.8 blocks per
	 * tick). Levitation is how the client is made to rise, and servers don't kick a levitating player for "flying".
	 */
	private static final int ASCENT_LIFT_AMPLIFIER = 39;
	private static final int PLATFORM_Y = 256;

	// --- Travel through one's own tear (Route TRAVEL) ---

	/** A block crossed in the gap is this many in the world, both ways (Overworld and Upper Realm alike). */
	public static final int GAP_SCALE = 20;
	/** Tearing in, one stands this high in the gap: its shallows. */
	public static final int TEAR_IN_Y = 128;
	/** Above this, the gap's upper layers: the heaviest pressure ({@link #LAYER_PRESSURE}) and the storms thick (SpatialStorms#targetAround). */
	public static final int UPPER_LAYERS_Y = 200;
	/** Rising past this, one is through: the Upper Realm, where the gap's place there lies. */
	public static final int CROSSING_Y = 360;
	/**
	 * Below this, the gap's lower layers, as the upper ones are above: the heaviest pressure and the storms thick. The gap is thick
	 * there too: nothing sinks through it faster than {@link #LOWER_SINK} (client GapSinkMixin), about as fast as Qi Flight climbs,
	 * so falling out costs about as much a block as rising through.
	 */
	public static final int LOWER_LAYERS_Y = 60;
	/** The fastest one sinks through the lower layers, in blocks a tick (4 a second). */
	public static final double LOWER_SINK = 0.2;
	/** Sinking below this, one falls out of the gap into the Overworld's sky. */
	public static final int FLOOR_Y = 12;
	/**
	 * The pressure in the shallows, a second: against Qi Sustenance's 2 a second a Heavenly Being Early (59 health) holds out a couple
	 * of minutes, crossing hundreds of thousands of blocks; the stormy layers press with {@link #LAYER_PRESSURE}.
	 */
	public static final float SHALLOW_PRESSURE = 2.5f;
	/**
	 * The pressure a second in the gap's stormy upper and lower layers, flown by one's own tear: twice the trial's (the user's ask,
	 * 2026-10-17). Climbing the upper layers on Qi Flight (160 blocks at ~4.5 a second, some 35 s) costs about 220 health against Qi
	 * Sustenance; sinking through the lower ones (12 s) about 75. The trial itself keeps {@link #PRESSURE_DAMAGE}.
	 */
	public static final float LAYER_PRESSURE = PRESSURE_DAMAGE * 2;
	/** Tearing space open costs this share of the qi pool going in, and this coming out. */
	public static final double TEAR_IN_QI = 0.25, TEAR_OUT_QI = 0.10;
	/** Between one tear and the next (ticks). */
	public static final int TEAR_COOLDOWN = 100;
	private static final Map<UUID, Long> LAST_TEAR = new HashMap<>();

	// --- Coming out without the server waiting on new land (SpatialArrivals) ---

	/** Within this of the crossing, or of the floor, the land one is heading for is asked for ahead (SpatialArrivals#prepare). */
	public static final int AHEAD_ABOVE = 40, AHEAD_BELOW = 20;
	/** ...asked for again once one has moved this far in the gap (blocks), or this long after (ticks: the land is held 30 s). */
	private static final double AHEAD_MOVE = 2;
	private static final int AHEAD_RENEW = 20 * 20;
	/** The ascent trial's landing is asked for this many ticks before the trial ends. */
	private static final int AHEAD_TRIAL_TICKS = 10 * 20;
	/** Coming out where the land isn't loaded yet, one is held in the gap while space settles: at most this long (ticks), then out regardless. */
	public static final int HOLD_TICKS = 100;

	/** The way out of the gap: by one's own tear, rising through the crossing, or sinking through the floor. */
	private enum Way { TEAR, CROSSING, FLOOR }

	/** One held at {@code at} until the land at {@code column} in {@code to} is loaded (or {@link #HOLD_TICKS} have passed). */
	private record Hold(ServerLevel to, BlockPos column, Way way, Vec3 at, long since) {}

	/** Where the land was last asked for ahead, for each traveller: in which realm, from where in the gap, when. */
	private record Ahead(ResourceKey<Level> to, double x, double z, long when) {}

	private static final Map<UUID, Hold> HOLDS = new HashMap<>();
	private static final Map<UUID, Ahead> AHEAD = new HashMap<>();

	/** Held in the gap while the land of their way out loads (no pressure, no storms' strikes meanwhile). */
	public static boolean isHeld(UUID id) {
		return HOLDS.containsKey(id);
	}

	private static final class Trial {
		final Route route;
		int ticksLeft = TRIAL_TICKS;
		/** Ticks spent in the gap (travel has no timer: the pressure lands once every 20). */
		int ticks;
		/** Times the loop has moved the player down (rising) minus times it moved them up (falling). */
		int loops;

		Trial(Route route) {
			this.route = route;
		}
	}

	private static final Map<UUID, Trial> ACTIVE = new HashMap<>();

	public static boolean isInTrial(UUID id) {
		return ACTIVE.containsKey(id);
	}

	/** In the gap through their own tear (Route TRAVEL): free to fly there (QiFlight), no timer. */
	public static boolean isTravelling(UUID id) {
		Trial t = ACTIVE.get(id);
		return t != null && t.route == Route.TRAVEL;
	}

	/**
	 * How many {@link #LOOP_LIFT}s the loop has taken off the player's height this trial, so that
	 * {@code getY() + loops * LOOP_LIFT} is how far they have really climbed (or fallen): the gap behaves as an endless
	 * sky. 0 outside a trial, and it starts over with the trial (a relog, say).
	 */
	public static int loops(ServerPlayer player) {
		Trial t = ACTIVE.get(player.getUUID());
		return t == null ? 0 : t.loops;
	}

	/** +1 while the gap carries the player up (ascension, and any trial not yet begun), -1 while it drops them. */
	public static int direction(ServerPlayer player) {
		Trial t = ACTIVE.get(player.getUUID());
		return t != null && t.route == Route.DESCEND ? -1 : 1;
	}

	/**
	 * Called by the Spatial Rift. One strong enough to tear space (a Heavenly Being) isn't carried: they fly the gap freely as by
	 * their own tear ({@link #travelIn}); anyone else is carried up on the trial.
	 */
	public static void beginAscension(ServerPlayer player) {
		if (!canEnter(player)) return;
		// Someone just cast down from the Upper Realm is still falling from the sky drop; descending near X=0, Z=0
		// would otherwise drop them straight back through the rift into a new ascension.
		if (CultivationManager.get(player).isFallProtected()) return;
		if (flysTheGap(player)) {
			travelIn(player, false);
			return;
		}
		enterGap(player, Route.ASCEND, player.getX(), ASCENT_ENTRY_Y, player.getZ());
	}

	/** Called by the VoidFallHandler when a player drops out of the bottom of the Upper Realm: a Heavenly Being flies the gap freely. */
	public static void beginDescension(ServerPlayer player) {
		if (!canEnter(player)) return;
		if (flysTheGap(player)) {
			travelIn(player, true);
			return;
		}
		enterGap(player, Route.DESCEND, player.getX(), DESCENT_ENTRY_Y, player.getZ());
	}

	/** Strong enough to tear space (Ability#SPATIAL_TEAR: the living, from Heavenly Being): the gap carries them nowhere, they fly it. */
	public static boolean flysTheGap(ServerPlayer player) {
		return Ability.SPATIAL_TEAR.isUnlocked(CultivationManager.get(player));
	}

	/**
	 * Into the gap's shallows as one who flies it (Route TRAVEL), at a {@link #GAP_SCALE}th of where they were, from the Upper
	 * Realm ({@code fromUpper}) or the Overworld: as a tear in, but free (the rift or the fall opened the way), and out again the
	 * same ways: up through the crossing, down through the floor, or by a tear back home.
	 */
	private static void travelIn(ServerPlayer player, boolean fromUpper) {
		PlayerCultivation c = CultivationManager.get(player);
		c.setGapFromUpper(fromUpper);
		c.setAbilityEnabled(Ability.SPATIAL_TEAR, true);
		enterGap(player, Route.TRAVEL, player.getX() / GAP_SCALE, TEAR_IN_Y, player.getZ() / GAP_SCALE);
		player.fallDistance = 0;
		CultivationManager.sync(player);
	}

	private static boolean canEnter(ServerPlayer player) {
		if (player == null || !player.isAlive() || player.isSpectator() || player.getServer() == null) return false;
		// A soul is bound to its body's realm, unless a person's body it is in carries it (Possession).
		if (SoulProjection.isAwayFromBody(player.getUUID())) return false;
		if (Incorporeal.isRuledGhost(player)) return false; // nor do the dead take a rift
		return !ACTIVE.containsKey(player.getUUID());
	}

	/**
	 * Into the gap, whichever way. A running tribulation can't follow: the gap is a way out of it in both directions (into the
	 * rift going up, off the edge of an island going down), without reward or penalty (see TribulationManager#abandon).
	 */
	private static void enterGap(ServerPlayer player, Route route, double x, double y, double z) {
		ServerLevel gap = player.getServer().getLevel(ModDimensions.SPATIAL_GAP);
		TribulationManager.abandon(player); // no-op without one; falling out of the Upper Realm, even with no gap to fall into
		if (gap == null) {
			DefyingTheHeavens.LOGGER.error("Spatial Gap dimension is missing; cannot start the {} trial for {}", route, player.getScoreboardName());
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		c.setPendingTrial(route);
		CultivationManager.markDirty(player.server);
		ACTIVE.put(player.getUUID(), new Trial(route));
		MeditationManager.stop(player, false);

		player.teleportTo(gap, x, y, z, player.getYRot(), player.getXRot());
		resyncMovement(player);
		player.fallDistance = 0;
		RealmSuppressionHandler.update(player); // the gap lifts suppression immediately

		player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6f, 0.6f);
		if (route == Route.TRAVEL) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_GAP_TRAVEL, GAP_SCALE).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
		} else {
			player.sendSystemMessage(Component.translatable(route == Route.ASCEND ? ModLang.MSG_GAP_ASCEND : ModLang.MSG_GAP_DESCEND,
					TRIAL_TICKS / 20));
		}
	}

	// --- One's own tear (Ability#SPATIAL_TEAR) ---

	/**
	 * The Spatial Tear: space torn open before the cultivator, a tear ({@link SpatialTearEntity}) that stands for a while and that
	 * they, and anyone else, walk through ({@link #walkInto}). From the Overworld or the Upper Realm it leads into the gap's shallows
	 * where the gap's place for it lies (a {@link #GAP_SCALE}th of the way); from inside the gap (by one's own tear or not), out
	 * again into the realm its opener came from ({@link #homeRealm}, never the other one), where the gap's place leads there.
	 * Heavenly Being and above on either path, for that path's pool ({@link #TEAR_IN_QI}, {@link #TEAR_OUT_QI} of it: qi, or the
	 * ghost path's ghost qi), {@link #TEAR_COOLDOWN} between tears; a new one closes the last.
	 */
	public static void tear(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!Ability.SPATIAL_TEAR.isUnlocked(c) || !p.isAlive() || p.isSpectator()) return;
		if (isHeld(p.getUUID())) { // already on the way out
			p.displayClientMessage(Component.translatable(ModLang.MSG_GAP_SETTLING), true);
			return;
		}
		long now = p.level().getGameTime();
		Long last = LAST_TEAR.get(p.getUUID());
		if (last != null && now - last < TEAR_COOLDOWN) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_COOLING, (TEAR_COOLDOWN - (now - last) + 19) / 20), true);
			return;
		}
		ResourceKey<Level> dimension = p.level().dimension();
		boolean out = ModDimensions.isSpatialGap(dimension);
		if (!out && (dimension != Level.OVERWORLD && dimension != ModDimensions.UPPER_REALM || p.server.getLevel(ModDimensions.SPATIAL_GAP) == null)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_NOT_HERE), true);
			return;
		}
		// A soul in a person's body tears space with that body's own tear, if it has one (Possession; the user's ask, 2026-10-18).
		if (SoulProjection.isAwayFromBody(p.getUUID()) || InnerRealm.isInside(p) || TribulationManager.isActive(p.getUUID())
				|| CoreDetonation.isCharging(p.getUUID())) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		// Paid from the pool of the path that awakened the tear (TwoPaths#awakens): the qi, or the ghost qi. A soul in a body that can't
		// tear space tears it with its own, from its own pool (Possession#soulTears).
		PlayerCultivation payer = Ability.SPATIAL_TEAR.awakenedBy(c) == null && Possession.inhabits(p.getUUID()) ? CultivationManager.own(p) : c;
		TwoPaths.Path path = Ability.SPATIAL_TEAR.awakenedBy(payer);
		if (path == null) path = TwoPaths.Path.HEAVEN;
		double cost = TwoPaths.poolMax(payer, path) * (out ? TEAR_OUT_QI : TEAR_IN_QI);
		if (!p.getAbilities().instabuild && !TwoPaths.spend(payer, path, cost)) {
			p.displayClientMessage(Component.translatable(path == TwoPaths.Path.GHOST ? ModLang.MSG_TEAR_NO_GHOST_QI : ModLang.MSG_TEAR_NO_QI,
					Math.round(cost)), true);
			return;
		}
		LAST_TEAR.put(p.getUUID(), now);
		riftFx(p);
		openTear(p, out);
		p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_OPEN), true);
		CultivationManager.sync(p);
	}

	// --- Tears standing open (SpatialTearEntity) ---

	/** How far before its opener a tear opens (the nearest of these with room), and how near one must come to step through it. */
	private static final double[] TEAR_AHEAD = {2.2, 1.6, 1.0};
	private static final double TEAR_REACH = 0.8, TEAR_HEIGHT = 2.6;

	/**
	 * A tear standing open, and where it leads: into the gap (from the Upper Realm or the Overworld, {@code fromUpper}), or out of it
	 * to {@code to}, where its place leads there at {@code scale} (its opener's way home: GAP_SCALE, or a trial's own 1).
	 */
	private record Tear(UUID entity, ServerLevel level, UUID opener, boolean out, boolean fromUpper, ServerLevel to, int scale, Vec3 at, long until) {}

	private static final List<Tear> TEARS = new ArrayList<>();

	/** Opens {@code p}'s tear before them (closing any of theirs still open), its far side's land asked for at once. */
	private static void openTear(ServerPlayer p, boolean out) {
		ServerLevel level = p.serverLevel();
		closeTears(p.getUUID());
		Vec3 ahead = p.getLookAngle().multiply(1, 0, 1);
		ahead = ahead.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : ahead.normalize();
		Vec3 at = p.position();
		for (double distance : TEAR_AHEAD) {
			Vec3 spot = p.position().add(ahead.scale(distance));
			BlockPos feet = BlockPos.containing(spot);
			if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
				at = spot;
				break;
			}
		}
		PlayerCultivation c = CultivationManager.get(p);
		ServerLevel to;
		int scale = GAP_SCALE;
		SpatialTearEntity.Leads leads;
		if (out) {
			// Back where its opener came from, never on into the other realm. One's own tear's gap is the world at GAP_SCALE; a trial's
			// lies over the place it began, at the world's own scale (at GAP_SCALE, falling off an island and tearing out at once would
			// be a free leap twenty times as far).
			Trial t = ACTIVE.get(p.getUUID());
			Route route = t != null ? t.route : c.getPendingTrial();
			scale = route == Route.TRAVEL ? GAP_SCALE : 1;
			ServerLevel home = p.server.getLevel(homeRealm(route, c.isGapFromUpper()));
			to = home == null ? p.server.overworld() : home;
			leads = to.dimension() == ModDimensions.UPPER_REALM ? SpatialTearEntity.Leads.UPPER_REALM : SpatialTearEntity.Leads.OVERWORLD;
			SpatialArrivals.prepare(to, columnFor(to, at.x * scale, at.z * scale)); // loading while one steps toward it
		} else {
			to = p.server.getLevel(ModDimensions.SPATIAL_GAP);
			leads = SpatialTearEntity.Leads.GAP;
		}
		// Its face turned to its opener.
		SpatialTearEntity entity = SpatialTearEntity.create(level, at, p.getYRot() + 180, leads);
		level.addFreshEntity(entity);
		TEARS.add(new Tear(entity.getUUID(), level, p.getUUID(), out, level.dimension() == ModDimensions.UPPER_REALM, to, scale, at,
				level.getGameTime() + SpatialTearEntity.LIFE - SpatialTearEntity.CLOSE_TICKS / 2));
	}

	/** Closes every tear {@code opener} has standing open. */
	private static void closeTears(UUID opener) {
		for (Iterator<Tear> it = TEARS.iterator(); it.hasNext(); ) {
			Tear tear = it.next();
			if (!tear.opener().equals(opener)) continue;
			if (tear.level().getEntity(tear.entity()) instanceof SpatialTearEntity entity) entity.discard();
			it.remove();
		}
	}

	/** Each tick: whoever has stepped into a tear goes through it; a tear closing (or gone) is let go. */
	private static void tickTears(MinecraftServer server) {
		if (TEARS.isEmpty()) return;
		for (Tear tear : new ArrayList<>(TEARS)) {
			Entity entity = tear.level().getEntity(tear.entity());
			if (!(entity instanceof SpatialTearEntity) || entity.isRemoved() || tear.level().getGameTime() >= tear.until() || tear.to() == null) {
				TEARS.remove(tear);
				continue;
			}
			net.minecraft.world.phys.AABB mouth = new net.minecraft.world.phys.AABB(tear.at().x - TEAR_REACH, tear.at().y - 0.2, tear.at().z - TEAR_REACH,
					tear.at().x + TEAR_REACH, tear.at().y + TEAR_HEIGHT, tear.at().z + TEAR_REACH);
			for (ServerPlayer walker : tear.level().getEntitiesOfClass(ServerPlayer.class, mouth)) walkInto(server, walker, tear);
		}
	}

	/**
	 * {@code walker} stepped into {@code tear}: through it into the gap's shallows, as one who flies the gap freely (a Heavenly
	 * Being's tear carries anyone through, those who can't tear space themselves too: they come out by the crossing, the floor, or
	 * a tear out opened for them); or out of the gap to where the tear leads. The dead, souls out of their bodies and those held
	 * on their way out already don't pass.
	 */
	private static void walkInto(MinecraftServer server, ServerPlayer walker, Tear tear) {
		if (!walker.isAlive() || walker.isSpectator() || SoulProjection.isAwayFromBody(walker.getUUID()) || Incorporeal.isRuledGhost(walker)
				|| isHeld(walker.getUUID())) return;
		if (tear.out()) {
			if (!ModDimensions.isSpatialGap(walker.level().dimension())) return;
			leaveWhenReady(server, walker, tear.to(), tear.at().x * tear.scale(), tear.at().z * tear.scale(), Way.TEAR);
			return;
		}
		if (ACTIVE.containsKey(walker.getUUID())) return;
		PlayerCultivation c = CultivationManager.get(walker);
		c.setGapFromUpper(tear.fromUpper());
		c.setAbilityEnabled(Ability.SPATIAL_TEAR, true); // flying the gap freely (and the client shows its layers)
		enterGap(walker, Route.TRAVEL, tear.at().x / GAP_SCALE, TEAR_IN_Y, tear.at().z / GAP_SCALE);
		walker.fallDistance = 0;
		CultivationManager.sync(walker);
	}

	/** Server stopped: no tear outlasts it. */
	public static void clear() {
		TEARS.clear();
		HOLDS.clear();
		AHEAD.clear();
	}

	/** The tear opening before the cultivator, for all to see, and its sound. */
	private static void riftFx(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2));
		SpellFx.send(level, SpellFx.Kind.RIFT_TEAR, at, new Vec3(p.getYRot(), 0, 0), 1.6f, 0);
		level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5f, 1.6f);
		level.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
	}

	/**
	 * The column one comes out at in {@code to} from ({@code x}, {@code z}) in its own blocks, kept inside its world border: in the
	 * Upper Realm, an island's, found by numbers (SpatialArrivals#target).
	 */
	private static BlockPos columnFor(ServerLevel to, double x, double z) {
		double border = to.getWorldBorder().getSize() / 2 - 16;
		x = Mth.clamp(x, to.getWorldBorder().getCenterX() - border, to.getWorldBorder().getCenterX() + border);
		z = Mth.clamp(z, to.getWorldBorder().getCenterZ() - border, to.getWorldBorder().getCenterZ() + border);
		return SpatialArrivals.target(to, Mth.floor(x), Mth.floor(z));
	}

	/**
	 * Out of the gap by {@code way} into {@code to} at ({@code x}, {@code z}) in its own blocks, without the server waiting on new
	 * land: the land there is asked for (generated off the server thread), and one goes at once if it is loaded (or falls from the
	 * sky, which waits on nothing: it loads as one falls); else one is held in the gap while space settles ({@link #tickHold}).
	 */
	private static void leaveWhenReady(MinecraftServer server, ServerPlayer p, ServerLevel to, double x, double z, Way way) {
		BlockPos column = columnFor(to, x, z);
		SpatialArrivals.prepare(to, column);
		if (way == Way.FLOOR || SpatialArrivals.ready(to, column)) {
			leaveTo(server, p, to, column, way);
			return;
		}
		HOLDS.put(p.getUUID(), new Hold(to, column, way, p.position(), p.level().getGameTime()));
		hover(p);
		p.setDeltaMovement(Vec3.ZERO);
		p.displayClientMessage(Component.translatable(ModLang.MSG_GAP_SETTLING), true);
	}

	/**
	 * Held while space settles: kept where the way out opened, hovering, with no pressure (nor storms' strikes), until the land
	 * beyond is loaded or {@link #HOLD_TICKS} have passed (then out regardless: SpatialArrivals reads only loaded land, and the
	 * Upper Realm's arrival loads its one chunk if it must).
	 */
	private static void tickHold(MinecraftServer server, ServerPlayer p, Hold hold) {
		long held = p.level().getGameTime() - hold.since();
		if (SpatialArrivals.ready(hold.to(), hold.column()) || held >= HOLD_TICKS) {
			leaveTo(server, p, hold.to(), hold.column(), hold.way());
			return;
		}
		hover(p);
		if (p.position().distanceToSqr(hold.at()) > 0.25) {
			p.connection.teleport(hold.at().x, hold.at().y, hold.at().z, p.getYRot(), p.getXRot());
		}
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
		if (held % 20 == 0) p.displayClientMessage(Component.translatable(ModLang.MSG_GAP_SETTLING), true);
	}

	/** Flying where one is (the gap is floorless: a hold must not fall). Out of the gap, QiFlight decides whether the flight goes on. */
	private static void hover(ServerPlayer p) {
		if (p.isSpectator() || p.getAbilities().flying) return;
		p.getAbilities().mayfly = true;
		p.getAbilities().flying = true;
		p.onUpdateAbilities();
	}

	/**
	 * Nearing the crossing or the floor, the land one is heading for is asked for ahead (SpatialArrivals#prepare), loading while
	 * one climbs or sinks the rest of the way; asked again only once one has moved on in the gap, or its hold on the land runs short.
	 */
	private static void loadAhead(MinecraftServer server, ServerPlayer p) {
		ResourceKey<Level> toward = p.getY() >= CROSSING_Y - AHEAD_ABOVE ? ModDimensions.UPPER_REALM
				: p.getY() < FLOOR_Y + AHEAD_BELOW ? Level.OVERWORLD : null;
		ServerLevel to = toward == null ? null : server.getLevel(toward);
		if (to == null) return;
		long now = p.level().getGameTime();
		Ahead last = AHEAD.get(p.getUUID());
		if (last != null && last.to() == toward && Math.abs(p.getX() - last.x()) < AHEAD_MOVE && Math.abs(p.getZ() - last.z()) < AHEAD_MOVE
				&& now - last.when() < AHEAD_RENEW) return;
		AHEAD.put(p.getUUID(), new Ahead(toward, p.getX(), p.getZ(), now));
		SpatialArrivals.prepare(to, columnFor(to, p.getX() * GAP_SCALE, p.getZ() * GAP_SCALE));
	}

	/**
	 * Out of the gap into {@code to} at {@code column}: the Upper Realm onto the island ground there (or a platform), the Overworld
	 * onto its ground (or, through the floor, dropped from its sky as a descent is, the first landing harmless). Only loaded land
	 * is read for the Overworld's ground: where none is, one comes down from the sky instead, the landing protected.
	 */
	private static void leaveTo(MinecraftServer server, ServerPlayer p, ServerLevel to, BlockPos column, Way way) {
		HOLDS.remove(p.getUUID());
		AHEAD.remove(p.getUUID());
		ACTIVE.remove(p.getUUID());
		removeLift(p);
		PlayerCultivation c = CultivationManager.get(p);
		c.setPendingTrial(Route.NONE);
		c.setAbilityEnabled(Ability.SPATIAL_TEAR, false);
		c.setGapFromUpper(false);
		int bx = column.getX(), bz = column.getZ();
		if (to.dimension() == ModDimensions.UPPER_REALM) {
			BlockPos arrival = arrivalAt(to, column);
			bx = arrival.getX();
			bz = arrival.getZ();
			p.teleportTo(to, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, p.getYRot(), p.getXRot());
			p.fallDistance = 0;
		} else if (way == Way.FLOOR) {
			p.teleportTo(to, bx + 0.5, OVERWORLD_DROP_Y, bz + 0.5, p.getYRot(), p.getXRot());
			p.fallDistance = 0;
			p.setOnGround(false);
			c.setFallProtected(true);
		} else {
			// Onto the ground there, or the nearest ground about it that isn't lava, in the land loaded round it.
			int y = to.getMinBuildHeight();
			for (int[] off : new int[][] {{0, 0}, {6, 0}, {-6, 0}, {0, 6}, {0, -6}, {12, 12}, {-12, -12}, {12, -12}, {-12, 12}}) {
				int cx = bx + off[0], cz = bz + off[1];
				if (to.getChunkSource().getChunkNow(cx >> 4, cz >> 4) == null) continue;
				int top = to.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
				if (top <= to.getMinBuildHeight() + 1 || to.getFluidState(new BlockPos(cx, top - 1, cz)).is(net.minecraft.tags.FluidTags.LAVA)) continue;
				bx = cx;
				bz = cz;
				y = top;
				break;
			}
			if (y <= to.getMinBuildHeight() + 1) {
				y = OVERWORLD_DROP_Y;
				c.setFallProtected(true);
			}
			p.teleportTo(to, bx + 0.5, y, bz + 0.5, p.getYRot(), p.getXRot());
			p.fallDistance = 0;
		}
		resyncMovement(p);
		p.setDeltaMovement(Vec3.ZERO);
		RealmSuppressionHandler.update(p);
		CultivationManager.markDirty(server);
		CultivationManager.sync(p);
		riftFx(p);
		p.displayClientMessage(Component.translatable(to.dimension() == ModDimensions.UPPER_REALM ? ModLang.MSG_TEAR_OUT_UPPER : ModLang.MSG_TEAR_OUT,
				bx, bz).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
		if (way == Way.CROSSING && to.dimension() == ModDimensions.UPPER_REALM) {
			p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_UPPER, (int) PlayerCultivation.UPPER_REALM_QI_MULTIPLIER));
		}
	}

	/**
	 * A second in the gap by one's own tear: the pressure by its layer, and where the world lies beneath them on the action bar.
	 * Past {@link #CROSSING_Y}, through into the Upper Realm; below {@link #FLOOR_Y}, out into the Overworld's sky.
	 */
	private static void tickTravel(MinecraftServer server, ServerLevel gap, ServerPlayer p, Trial t) {
		removeLift(p);
		// Wings and rockets don't carry anyone through the gap (it would make the climb through the upper layers a few seconds' dash):
		// one flies it on qi, or on a sword.
		if (p.isFallFlying()) p.stopFallFlying();
		if (p.getY() >= CROSSING_Y) {
			ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
			leaveWhenReady(server, p, upper == null ? server.overworld() : upper, p.getX() * GAP_SCALE, p.getZ() * GAP_SCALE, Way.CROSSING);
			return;
		}
		if (p.getY() < FLOOR_Y) {
			leaveWhenReady(server, p, server.overworld(), p.getX() * GAP_SCALE, p.getZ() * GAP_SCALE, Way.FLOOR);
			return;
		}
		if (++t.ticks % 20 == 0) {
			loadAhead(server, p);
			p.hurt(ModDamageTypes.spatialPressure(gap), pressureAt(p.getY()));
			if (!p.isAlive()) return;
			long x = Math.round(p.getX() * GAP_SCALE), z = Math.round(p.getZ() * GAP_SCALE);
			if (inLowerLayers(p.getY())) {
				p.displayClientMessage(Component.translatable(ModLang.MSG_GAP_TRAVEL_LOWER, x, z, Math.max(0, Mth.floor(p.getY()) - FLOOR_Y)), true);
			} else {
				p.displayClientMessage(Component.translatable(p.getY() >= UPPER_LAYERS_Y ? ModLang.MSG_GAP_TRAVEL_UPPER : ModLang.MSG_GAP_TRAVEL_SHALLOWS,
						x, z, Math.max(0, CROSSING_Y - Mth.floor(p.getY()))), true);
			}
		}
	}

	/**
	 * The realm a tear out of the gap leads back to: the one torn in from (a trial's, the one it set out from: the Upper Realm for a
	 * descent, the Overworld for an ascent). Never the other: the way between them is up or down through the gap.
	 */
	static ResourceKey<Level> homeRealm(Route route, boolean gapFromUpper) {
		boolean upper = route == Route.TRAVEL ? gapFromUpper : route == Route.DESCEND;
		return upper ? ModDimensions.UPPER_REALM : Level.OVERWORLD;
	}

	/** In the gap's lower layers ({@link #LOWER_LAYERS_Y}), still above its floor. */
	public static boolean inLowerLayers(double y) {
		return y < LOWER_LAYERS_Y && y >= FLOOR_Y;
	}

	/** The pressure a second on one flying the gap by their own tear at height {@code y}: twice the trial's in the stormy layers, less in the shallows. */
	static float pressureAt(double y) {
		return y >= UPPER_LAYERS_Y || y < LOWER_LAYERS_Y ? LAYER_PRESSURE : SHALLOW_PRESSURE;
	}

	public static void tick(MinecraftServer server) {
		if (server == null) return;
		tickTears(server); // whoever stepped into a tear goes through (out of the levels' own ticking: safe to carry them off)
		ServerLevel gap = server.getLevel(ModDimensions.SPATIAL_GAP);

		// Drop trials for players who logged out (the route stays saved) or left the gap by other means.
		for (Iterator<Map.Entry<UUID, Trial>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Trial> entry = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null || gap == null || p.level() != gap) {
				HOLDS.remove(entry.getKey());
				AHEAD.remove(entry.getKey());
			}
			if (p == null) {
				it.remove();
			} else if (gap == null || p.level() != gap) {
				it.remove();
				CultivationManager.get(p).setPendingTrial(Route.NONE);
				if (CultivationManager.get(p).isAbilityEnabled(Ability.SPATIAL_TEAR)) {
					CultivationManager.get(p).setAbilityEnabled(Ability.SPATIAL_TEAR, false);
					CultivationManager.sync(p);
				}
				CultivationManager.markDirty(server);
				removeLift(p);
			}
		}

		if (gap != null) {
			for (ServerPlayer p : new ArrayList<>(gap.players())) {
				try {
					tickPlayer(server, gap, p);
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("Spatial trial tick failed for {}", p.getScoreboardName(), e);
				}
			}
		}

		tickFallProtection(server);
	}

	private static void tickPlayer(MinecraftServer server, ServerLevel gap, ServerPlayer p) {
		if (!p.isAlive()) return;
		Trial t = ACTIVE.get(p.getUUID());
		if (t == null) {
			// Relogged inside the gap, or sent here by a command: the timer starts over.
			PlayerCultivation c = CultivationManager.get(p);
			Route route = c.getPendingTrial() == Route.NONE ? Route.ASCEND : c.getPendingTrial();
			c.setPendingTrial(route);
			CultivationManager.markDirty(server);
			t = new Trial(route);
			ACTIVE.put(p.getUUID(), t);
			RealmSuppressionHandler.update(p);
			if (route != Route.TRAVEL) p.sendSystemMessage(Component.translatable(ModLang.MSG_GAP_RESTART, TRIAL_TICKS / 20));
		}

		// Vanilla shields a player from damage for 3 s after joining while health keeps regenerating, so relogging over
		// and over would let anyone outlast the gap. The pressure must never pause.
		((ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);

		Hold hold = HOLDS.get(p.getUUID());
		if (hold != null) { // on the way out, space settling: the land beyond loading
			tickHold(server, p, hold);
			return;
		}
		if (t.route == Route.TRAVEL) { // one's own tear: free to fly, no timer
			tickTravel(server, gap, p, t);
			return;
		}

		t.ticksLeft--;
		forceDirection(p, t.route);
		// The climb's landing in the Upper Realm asked for ten seconds before it ends (complete: findArrival finds it loaded).
		if (t.route == Route.ASCEND && t.ticksLeft == AHEAD_TRIAL_TICKS) {
			ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
			if (upper != null) SpatialArrivals.prepare(upper, SpatialArrivals.target(upper, p.getBlockX(), p.getBlockZ()));
		}

		// All axes relative: the client keeps its speed through the loop.
		if (p.getY() < LOOP_BELOW_Y) {
			p.connection.teleport(p.getX(), p.getY() + LOOP_LIFT, p.getZ(), p.getYRot(), p.getXRot(), EnumSet.allOf(RelativeMovement.class));
			resyncMovement(p);
			t.loops--;
		} else if (p.getY() > LOOP_ABOVE_Y) {
			p.connection.teleport(p.getX(), p.getY() - LOOP_LIFT, p.getZ(), p.getYRot(), p.getXRot(), EnumSet.allOf(RelativeMovement.class));
			resyncMovement(p);
			t.loops++;
		}

		if (t.ticksLeft % 20 == 0) {
			p.hurt(ModDamageTypes.spatialPressure(gap), PRESSURE_DAMAGE);
			if (!p.isAlive()) return; // AFTER_DEATH -> onDeath() cleans up
			p.displayClientMessage(Component.translatable(ModLang.MSG_GAP_TIMER, Math.max(0, t.ticksLeft / 20)), true);
		}

		if (t.ticksLeft <= 0) {
			complete(server, p, t.route);
		}
	}

	/**
	 * Makes the route decide the direction: no flying (creative or any other kind) and no elytra gliding in the gap;
	 * ascending, the hidden Levitation lift is kept topped up; descending, any Levitation (a potion, say) is removed.
	 */
	private static void forceDirection(ServerPlayer p, Route route) {
		if (p.isSpectator()) return;
		if (p.getAbilities().flying) {
			p.getAbilities().flying = false;
			p.onUpdateAbilities();
		}
		if (p.isFallFlying()) p.stopFallFlying();

		MobEffectInstance levitation = p.getEffect(MobEffects.LEVITATION);
		if (route == Route.ASCEND) {
			if (levitation != null && isLift(levitation) && levitation.getDuration() >= 20) return;
			if (levitation != null && !isLift(levitation)) p.removeEffect(MobEffects.LEVITATION);
			p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, ASCENT_LIFT_AMPLIFIER, false, false, false));
			// Being carried up isn't levitating: keep vanilla from counting it toward "Great View From Up Here".
			((ServerPlayerAccessor) p).dth$setLevitationStartPos(null);
		} else if (levitation != null) {
			p.removeEffect(MobEffects.LEVITATION);
		}
	}

	/** The ascent lift: our amplifier, no particles, no icon. */
	private static boolean isLift(MobEffectInstance effect) {
		return effect.getAmplifier() == ASCENT_LIFT_AMPLIFIER && !effect.isVisible() && !effect.showIcon();
	}

	/** Takes the ascent lift off on arrival, or when the player leaves the gap some other way. */
	private static void removeLift(ServerPlayer p) {
		MobEffectInstance levitation = p.getEffect(MobEffects.LEVITATION);
		if (levitation != null && isLift(levitation)) p.removeEffect(MobEffects.LEVITATION);
	}

	private static void complete(MinecraftServer server, ServerPlayer p, Route route) {
		ACTIVE.remove(p.getUUID());
		removeLift(p);
		PlayerCultivation c = CultivationManager.get(p);
		c.setPendingTrial(Route.NONE);

		if (route == Route.ASCEND) {
			ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
			if (upper == null) {
				DefyingTheHeavens.LOGGER.error("Upper Realm dimension is missing; returning {} to the Overworld instead", p.getScoreboardName());
				dropIntoOverworld(server, p, c);
				return;
			}
			BlockPos arrival = findArrival(upper, p.getBlockX(), p.getBlockZ());
			p.teleportTo(upper, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, p.getYRot(), p.getXRot());
			resyncMovement(p);
			p.setDeltaMovement(Vec3.ZERO);
			p.fallDistance = 0;
			RealmSuppressionHandler.update(p);
			p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_UPPER, (int) PlayerCultivation.UPPER_REALM_QI_MULTIPLIER));
		} else {
			dropIntoOverworld(server, p, c);
		}
		CultivationManager.markDirty(server);
	}

	private static void dropIntoOverworld(MinecraftServer server, ServerPlayer p, PlayerCultivation c) {
		ServerLevel overworld = server.overworld();
		if (overworld == null) return;
		p.teleportTo(overworld, p.getX(), OVERWORLD_DROP_Y, p.getZ(), p.getYRot(), p.getXRot());
		resyncMovement(p);
		p.fallDistance = 0;
		p.setOnGround(false); // nothing stale from the gap may count as a landing (see tickFallProtection)
		c.setFallProtected(true); // the first landing from the sky drop is harmless
		RealmSuppressionHandler.update(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_ARRIVE_LOWER), true);
	}

	/**
	 * Solid, dry island ground nearest (x, z) in the Upper Realm, the island found by numbers (SpatialArrivals#target: nothing
	 * generated to look); a small platform there if the abyss is empty round it (NpcTravel too).
	 */
	static BlockPos findArrival(ServerLevel level, int x, int z) {
		return arrivalAt(level, SpatialArrivals.target(level, x, z));
	}

	/**
	 * Ground at or near {@code column} in the Upper Realm, its land loaded here if it isn't yet (one chunk; as a rule it was
	 * loaded ahead, SpatialArrivals#prepare), or a platform there.
	 */
	private static BlockPos arrivalAt(ServerLevel level, BlockPos column) {
		try {
			level.getChunk(column.getX() >> 4, column.getZ() >> 4);
			BlockPos ground = SpatialArrivals.groundNear(level, column);
			if (ground != null) return ground;
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Arrival search in the Upper Realm failed around {}, {}", column.getX(), column.getZ(), e);
		}
		return buildPlatform(level, column.getX(), column.getZ());
	}

	private static BlockPos buildPlatform(ServerLevel level, int x, int z) {
		BlockPos center = new BlockPos(x, PLATFORM_Y, z);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlock(center.offset(dx, -1, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
				for (int dy = 0; dy < 3; dy++) {
					level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		DefyingTheHeavens.LOGGER.info("No island near the arrival point; built a landing platform at {}", center.toShortString());
		return center;
	}

	/**
	 * Clears the sky-drop fall protection once the player has landed (or is no longer falling into the Overworld).
	 * On ground alone is not enough: the server's own collision marks the player as grounded a tick or more before the
	 * client's landing packet applies the fall damage, so on a dedicated server the protection would be gone by then.
	 * A landing only counts once the fall itself is resolved (fall distance back to 0); a damaging one consumes the
	 * protection in {@link #consumeFallProtection} first.
	 */
	private static void tickFallProtection(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(p);
			if (!c.isFallProtected()) continue;
			// (Coming down onto a planet out of space is protected the same way: AstralRealm.)
			boolean landed = (p.onGround() && p.fallDistance <= 0) || p.isInWater() || p.getAbilities().flying || !p.isAlive()
					|| p.level().dimension() != Level.OVERWORLD && !ModDimensions.isPlanetSurface(p.level().dimension());
			if (landed) {
				c.setFallProtected(false);
				CultivationManager.markDirty(server);
			}
		}
	}

	/**
	 * Re-baselines vanilla's movement check after the server moves a player. Otherwise, on a dedicated server, the client's
	 * first packets from the new spot are measured against the old one ("moved too quickly!") and rubber-banded back.
	 * Singleplayer hosts are exempt from that check, which is why this only shows on servers.
	 */
	private static void resyncMovement(ServerPlayer p) {
		p.connection.resetPosition();
	}

	/** Returns true if this fall damage should be cancelled (consumes the protection). */
	public static boolean consumeFallProtection(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		if (!c.isFallProtected()) return false;
		c.setFallProtected(false);
		CultivationManager.markDirty(player.server);
		return true;
	}

	/** Dying in the gap ends the trial; the player respawns at their spawn point as usual. */
	public static void onDeath(ServerPlayer player) {
		if (player == null) return;
		HOLDS.remove(player.getUUID());
		AHEAD.remove(player.getUUID());
		if (ACTIVE.remove(player.getUUID()) != null || CultivationManager.get(player).getPendingTrial() != Route.NONE) {
			CultivationManager.get(player).setPendingTrial(Route.NONE);
			CultivationManager.get(player).setAbilityEnabled(Ability.SPATIAL_TEAR, false);
			CultivationManager.get(player).setGapFromUpper(false);
			CultivationManager.markDirty(player.server);
		}
	}

	/**
	 * Disconnect: forget the running timer; the saved route restarts a full trial on the next login (travel simply goes on). A hold
	 * goes too: back in, one stands where it held them, and the way out opens again at the crossing or floor, or with a new tear.
	 */
	public static void forget(UUID id) {
		ACTIVE.remove(id);
		LAST_TEAR.remove(id);
		HOLDS.remove(id);
		AHEAD.remove(id);
	}

	private SpatialTrialHandler() {}
}
