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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Spatial Gap, between the Overworld and the Upper Realm. However one comes in (the Overworld's rift at 0, 0, a fall off the
 * Upper Realm, a tear in space: one's own or another's), one is in it the same way ({@link Route#TRAVEL}): no timer and no current,
 * free to fly it as anywhere (Qi Flight, Sword Flight, a ghostly form's flight; one who can't fly falls). A block crossed here is
 * {@link #GAP_SCALE} in the world; rising through the stormy upper layers past {@link #CROSSING_Y} leads into the Upper Realm,
 * sinking through the stormy lower layers below {@link #FLOOR_Y} drops one into the Overworld's sky, and a tear out (a Heavenly
 * Being's) leads back to the realm one came from. Spatial pressure presses all the while ({@link #pressureAt}).
 * <p>
 * The gap once carried those below Heavenly Being on a timed trial instead (60 seconds, lifted up or dropped down, no flying):
 * the user removed it on 2026-10-18 ("anyone who can fly can move around in there"). Which realm one came from is saved with
 * them; dying in the gap ends the journey.
 */
public final class SpatialTrialHandler {
	/**
	 * How one is in the gap: {@link #TRAVEL}, the only way now (see above); NONE outside it. ASCEND and DESCEND were the old timed
	 * trials, kept so a save from then still reads: one back in the gap on either travels (DESCEND as one come down from the Upper
	 * Realm).
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

	/**
	 * The old trial's pressure a second (the stormy layers were twice it until 2026-10-19: see {@link #LAYER_PRESSURE}). The gap's
	 * pressure ignores armor, enchantments and effects (see ModDamageTypes); health, regen, absorption and healing count.
	 */
	public static final float PRESSURE_DAMAGE = 4.15f;

	/** Out of the gap into the Overworld's sky: above its build limit, below where rising leads out into space (AstralRealm#EXIT_Y). */
	private static final int OVERWORLD_DROP_Y = 330;
	/** The storms' band height (SpatialStorms#PERIOD; the old trial's endless sky repeated every this many blocks). */
	public static final int LOOP_LIFT = 192;
	/** The old trial's hidden lift (Levitation of this amplifier), still taken off anyone a save from then left it on. */
	private static final int ASCENT_LIFT_AMPLIFIER = 39;
	private static final int PLATFORM_Y = 256;

	// --- Travel through the gap (Route TRAVEL) ---

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
	 * The pressure a second in the gap's stormy upper and lower layers: 8.2, the user's pick of 2026-10-19 (it was twice the old
	 * trial's, 8.3, from 2026-10-17). Climbing the upper layers on Qi Flight (160 blocks at ~7.5 a second, measured 2026-10-19: some
	 * 21 s) costs about 175 health, the whole climb from where one comes in about 200; sinking through the lower ones (12 s) about
	 * 100 (both before any healing). With Qi Sustenance and base stats, a Heavenly Being Grand Perfection is the first to come through
	 * the climb (1.3 health left), and a Nascent Soul Grand Perfection the first to live through falling out. (That at vanilla's
	 * mending: since 2026-10-22 a body mends at its realm's pace, CultivationStats#mendFactor, so from Nascent Soul up the gap presses
	 * the lighter, the user's word: "its okay if they become easier".)
	 */
	public static final float LAYER_PRESSURE = 8.2f;
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
	/** Coming out where the land isn't loaded yet, one is held in the gap while space settles: at most this long (ticks), then out regardless. */
	public static final int HOLD_TICKS = 100;

	/** The way out of the gap: by a tear, rising through the crossing, or sinking through the floor. */
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

	/** One in the gap (Route TRAVEL). */
	private static final class Trial {
		final Route route;
		/** Ticks spent in the gap (the pressure lands once every 20). */
		int ticks;

		Trial(Route route) {
			this.route = route;
		}
	}

	private static final Map<UUID, Trial> ACTIVE = new HashMap<>();

	/** In the gap (on its way through it, by any way in). */
	public static boolean isInTrial(UUID id) {
		return ACTIVE.containsKey(id);
	}

	/** In the gap, free to fly it (QiFlight): everyone in it now (Route TRAVEL). */
	public static boolean isTravelling(UUID id) {
		Trial t = ACTIVE.get(id);
		return t != null && t.route == Route.TRAVEL;
	}

	/** Called by the Spatial Rift at the Overworld's 0, 0: into the gap's shallows, to fly up through it (or fall back down). */
	public static void beginAscension(ServerPlayer player) {
		if (!canEnter(player)) return;
		// Someone just cast down from the Upper Realm is still falling from the sky drop; descending near X=0, Z=0
		// would otherwise drop them straight back through the rift into the gap again.
		if (CultivationManager.get(player).isFallProtected()) return;
		travelIn(player, false);
	}

	/** Called by the VoidFallHandler when a player drops out of the bottom of the Upper Realm: into the gap's shallows below it. */
	public static void beginDescension(ServerPlayer player) {
		if (!canEnter(player)) return;
		travelIn(player, true);
	}

	/**
	 * Into the gap's shallows (Route TRAVEL), at a {@link #GAP_SCALE}th of where they were, from the Upper Realm ({@code fromUpper})
	 * or the Overworld: as a tear in (the rift or the fall opened the way), and out again the same ways: up through the crossing,
	 * down through the floor, or by a tear back home for one who can tear space.
	 */
	private static void travelIn(ServerPlayer player, boolean fromUpper) {
		PlayerCultivation c = CultivationManager.get(player);
		c.setGapFromUpper(fromUpper);
		c.setAbilityEnabled(Ability.SPATIAL_TEAR, true);
		enterGap(player, Route.TRAVEL, player.getX() / GAP_SCALE, TEAR_IN_Y, player.getZ() / GAP_SCALE);
		player.fallDistance = 0;
		CultivationManager.sync(player);
	}

	/**
	 * {@code player} cast out into the gap at {@code at} (out of a sleeve whose bearer fell, or let them out, there: Sleeves), in it as any
	 * who came in: from the Upper Realm ({@code fromUpper}) or the Overworld, its pressure on them now.
	 */
	static void castIn(ServerPlayer player, Vec3 at, boolean fromUpper) {
		if (!canEnter(player)) return;
		PlayerCultivation c = CultivationManager.get(player);
		c.setGapFromUpper(fromUpper);
		enterGap(player, Route.TRAVEL, at.x, at.y, at.z);
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
			DefyingTheHeavens.LOGGER.error("Spatial Gap dimension is missing; cannot carry {} into it", player.getScoreboardName());
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
		// Its ways out: a tear back for one who can tear space; up through the crossing or down through the floor for everyone.
		boolean tears = Ability.SPATIAL_TEAR.isUnlocked(c);
		player.sendSystemMessage(Component.translatable(tears ? ModLang.MSG_GAP_TRAVEL : ModLang.MSG_GAP_TRAVEL_NO_TEAR, GAP_SCALE)
				.withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
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
		long wait = Math.round(TEAR_COOLDOWN * Laws.tearWait(c)); // the sooner by the Law of Space (Laws)
		if (last != null && now - last < wait) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_COOLING, (wait - (now - last) + 19) / 20), true);
			return;
		}
		ResourceKey<Level> dimension = p.level().dimension();
		// Immortal space holds: no tear opens in the Immortal Realm, nor in the Gate's passage; the Gate is the only way down (the user's
		// rule, 2026-10-25).
		if (ModDimensions.isImmortalRealm(dimension) || ModDimensions.isImmortalPassage(dimension)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_IMMORTAL), true);
			return;
		}
		// A secret realm's space is sealed: one leaves it by a Departure Jade, or as it closes (SecretRealms).
		if (ModDimensions.isSecretRealm(dimension)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SECRET_SEALED_SPACE), true);
			return;
		}
		boolean out = ModDimensions.isSpatialGap(dimension);
		// From Void Shattering on, torn high in the Jade Firmament's sky: a Void Breach up into the Great Void; torn in the Great Void, a
		// way down onto the Jade Firmament (VoidBreach, 2026-10-25).
		boolean breach = VoidBreach.tearsBreach(p, c), down = ModDimensions.isGreatVoid(dimension);
		if (!out && !breach && !down
				&& (dimension != Level.OVERWORLD && dimension != ModDimensions.UPPER_REALM || p.server.getLevel(ModDimensions.SPATIAL_GAP) == null)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_NOT_HERE), true);
			return;
		}
		// A soul in a person's body tears space with that body's own tear, if it has one (Possession; the user's ask, 2026-10-18).
		if (SoulProjection.isAwayFromBody(p.getUUID()) || InnerRealm.isInside(p) || TribulationManager.isActive(p.getUUID())
				|| CoreDetonation.isCharging(p.getUUID())) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		// Space locked round it (Earth Veins Lock the Void): it won't tear.
		if (EarthSpells.spaceLocked(p)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SPACE_LOCKED), true);
			return;
		}
		// Nor within a foe's domain of Space (LawDomains).
		if (LawDomains.locksSpace(p)) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_DOMAIN_SPACE_LOCKED), true);
			return;
		}
		// Paid from the pool of the path that awakened the tear (TwoPaths#awakens): the qi, or the ghost qi. A soul in a body that can't
		// tear space tears it with its own, from its own pool (Possession#soulTears).
		PlayerCultivation payer = Ability.SPATIAL_TEAR.awakenedBy(c) == null && Possession.inhabits(p.getUUID()) ? CultivationManager.own(p) : c;
		TwoPaths.Path path = Ability.SPATIAL_TEAR.awakenedBy(payer);
		if (path == null) path = TwoPaths.Path.HEAVEN;
		double cost = TwoPaths.poolMax(payer, path) * (breach ? VoidBreach.BREACH_QI : out || down ? TEAR_OUT_QI : TEAR_IN_QI);
		if (!p.getAbilities().instabuild && !TwoPaths.spend(payer, path, cost)) {
			p.displayClientMessage(Component.translatable(path == TwoPaths.Path.GHOST ? ModLang.MSG_TEAR_NO_GHOST_QI : ModLang.MSG_TEAR_NO_QI,
					Math.round(cost)), true);
			return;
		}
		LAST_TEAR.put(p.getUUID(), now);
		riftFx(p);
		if (breach) {
			VoidBreach.open(p);
		} else if (down) {
			VoidBreach.openDown(p);
		} else {
			openTear(p, out);
			p.displayClientMessage(Component.translatable(ModLang.MSG_TEAR_OPEN), true);
		}
		CultivationManager.sync(p);
	}

	// --- Tears standing open (SpatialTearEntity) ---

	/** How far before its opener a tear opens (the nearest of these with room), and how near one must come to step through it. */
	private static final double[] TEAR_AHEAD = {2.2, 1.6, 1.0};
	private static final double TEAR_REACH = 0.8, TEAR_HEIGHT = 2.6;

	/**
	 * A tear standing open, and where it leads: into the gap (from the Upper Realm or the Overworld, {@code fromUpper}), or out of it
	 * to {@code to}, where its place leads there at {@code scale} (its opener's way home: GAP_SCALE).
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
			// Back where its opener came from, never on into the other realm, where the gap's place leads there (GAP_SCALE).
			Trial t = ACTIVE.get(p.getUUID());
			Route route = t != null ? t.route : c.getPendingTrial();
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
	 * A second in the gap: the pressure by its layer, and where the world lies beneath them on the action bar.
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
			// Twice as heavy for each one carried in one's sleeve (Sleeves).
			p.hurt(ModDamageTypes.spatialPressure(gap), pressureAt(p.getY()) * Sleeves.pressureFactor(p));
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
	 * The realm a tear out of the gap leads back to: the one come in from (an old save's trial, the one it set out from: the Upper
	 * Realm for a descent, the Overworld for an ascent). Never the other: the way between them is up or down through the gap.
	 */
	static ResourceKey<Level> homeRealm(Route route, boolean gapFromUpper) {
		boolean upper = route == Route.TRAVEL ? gapFromUpper : route == Route.DESCEND;
		return upper ? ModDimensions.UPPER_REALM : Level.OVERWORLD;
	}

	/** In the gap's lower layers ({@link #LOWER_LAYERS_Y}), still above its floor. */
	public static boolean inLowerLayers(double y) {
		return y < LOWER_LAYERS_Y && y >= FLOOR_Y;
	}

	/** The pressure a second on one in the gap at height {@code y}: {@link #LAYER_PRESSURE} in the stormy layers, less in the shallows. */
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
		// A spectator only looks on (as on the Soul River): no journey. One whose body is held (Possession) is carried in so, watching it.
		if (!p.isAlive() || p.isSpectator()) return;
		Trial t = ACTIVE.get(p.getUUID());
		if (t == null) {
			// Relogged inside the gap, or sent here by a command: on the way through it again. A save from the old trials' days too:
			// one coming down out of the Upper Realm came from there; one going up, or with nothing saved, from the Overworld.
			PlayerCultivation c = CultivationManager.get(p);
			Route was = c.getPendingTrial();
			if (was == Route.DESCEND) c.setGapFromUpper(true);
			else if (was != Route.TRAVEL) c.setGapFromUpper(false);
			c.setPendingTrial(Route.TRAVEL);
			c.setAbilityEnabled(Ability.SPATIAL_TEAR, true); // flying the gap (and the client shows its layers)
			CultivationManager.markDirty(server);
			CultivationManager.sync(p);
			t = new Trial(Route.TRAVEL);
			ACTIVE.put(p.getUUID(), t);
			RealmSuppressionHandler.update(p);
		}

		// Vanilla shields a player from damage for 3 s after joining while health keeps regenerating, so relogging over
		// and over would let anyone outlast the gap. The pressure must never pause.
		((ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);

		Hold hold = HOLDS.get(p.getUUID());
		if (hold != null) { // on the way out, space settling: the land beyond loading
			tickHold(server, p, hold);
			return;
		}
		tickTravel(server, gap, p, t);
	}

	/** The old trial's ascent lift: our amplifier, no particles, no icon. */
	private static boolean isLift(MobEffectInstance effect) {
		return effect.getAmplifier() == ASCENT_LIFT_AMPLIFIER && !effect.isVisible() && !effect.showIcon();
	}

	/** Takes the old trial's lift off anyone a save from then left it on (in the gap, or as they leave it). */
	private static void removeLift(ServerPlayer p) {
		MobEffectInstance levitation = p.getEffect(MobEffects.LEVITATION);
		if (levitation != null && isLift(levitation)) p.removeEffect(MobEffects.LEVITATION);
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
			// (Coming down onto a planet out of space is protected the same way, and into the Upper Realm's sky out of it: AstralRealm,
			// whose protection there was cleared the moment it was given before the sweep of 2026-10-23.)
			boolean landed = (p.onGround() && p.fallDistance <= 0) || p.isInWater() || p.getAbilities().flying || !p.isAlive()
					|| p.level().dimension() != Level.OVERWORLD && p.level().dimension() != ModDimensions.UPPER_REALM
					&& !ModDimensions.isPlanetSurface(p.level().dimension());
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

	/** Dying in the gap ends the journey through it; the player respawns at their spawn point as usual. */
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
	 * Disconnect: forget what runs; the saved route and realm come back with them (back in, the journey simply goes on). A hold goes
	 * too: back in, one stands where it held them, and the way out opens again at the crossing or floor, or with a new tear.
	 */
	public static void forget(UUID id) {
		ACTIVE.remove(id);
		LAST_TEAR.remove(id);
		HOLDS.remove(id);
		AHEAD.remove(id);
	}

	private SpatialTrialHandler() {}
}
