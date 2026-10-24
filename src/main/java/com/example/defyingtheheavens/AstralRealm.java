package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Astral Realm (the user's, 2026-10-17): the sky beyond the sky, where the Overworld is one planet among thousands.
 * <ul>
 *   <li><b>Going up.</b> A Nascent Soul or above, flying up past {@link #EXIT_Y} in the Overworld (the Upper Realm's taller lands:
 *   {@link #UPPER_EXIT_Y}), rises out of
 *   its sky into space ({@link ModDimensions#ASTRAL_VOID}), over the Overworld's lit side. Below Nascent Soul the sky thins to nothing
 *   and they are told so. Where they rose from is remembered: flying back into the Overworld's sky brings them down there, in the
 *   realm they left (over the nearest island in the Upper Realm). No Realm Suppression holds anyone here (not a lower realm).</li>
 *   <li><b>Space.</b> Stars and their planets (AstralPlanets), drawn by the client; nothing there is a block. One always flies there,
 *   at a speed of one's realm ({@link #spaceSpeed}), on no qi; qi gathers at a quarter of the lower realms' ({@link #SPACE_QI}). A
 *   star burns whoever comes too near. Vestiges of the Void hunt there (VoidVestige). Every block flown there costs years, the same
 *   for everyone ({@link #YEARS_PER_BLOCK}: the user's ask, 2026-10-21; {@link #travel}).</li>
 *   <li><b>The planets.</b> Flying into a planet's atmosphere sets one down in its sky (ModDimensions#ASTRAL_SURFACES), over the very
 *   land one flew down toward (PlanetTerrain#landUnder), the first landing harmless; the land there is made ready ahead, and if it
 *   isn't yet, one is held a moment at the edge. Each planet has its own qi (AstralPlanets.Planet#qi). Flying back up past
 *   {@link #EXIT_Y} takes one out into space above it. Flight costs no qi anywhere in the Astral Realm (QiFlight): no one is ever
 *   stranded on a thin-aired world.</li>
 * </ul>
 * The planets' catalog comes from the world's seed (AstralPlanets#astralSeed), set before the worlds load and sent to each player
 * on joining ({@link ModPackets#ASTRAL_SEED}).
 */
public final class AstralRealm {
	/** Rising past this (in the Overworld or on a planet) leads out into space: above the build limit, below the gap's drop. */
	public static final int EXIT_Y = 340;
	/**
	 * ...and in the Upper Realm past this: above its own build limit (512), as the Overworld's is above its 320 (the user's ask,
	 * 2026-10-22: its islands rose past Y 340 and their heights led out into space). {@link #exitY}.
	 */
	public static final int UPPER_EXIT_Y = 540;
	/** Coming down onto a planet: this high over its ground (never above {@link #ARRIVAL_MAX_Y}); onto the Overworld, at that. */
	public static final int ARRIVAL_ABOVE = 50, ARRIVAL_MAX_Y = 325;
	/** Coming down into the Upper Realm from space: over its highest lands, below {@link #UPPER_EXIT_Y} (the first landing harmless). */
	public static final int UPPER_ARRIVAL_Y = UPPER_EXIT_Y - 15;

	/** How high one rises in {@code dimension} before it leads out into space: {@link #UPPER_EXIT_Y} in the Upper Realm, else {@link #EXIT_Y}. */
	public static int exitY(ResourceKey<Level> dimension) {
		return dimension == ModDimensions.UPPER_REALM ? UPPER_EXIT_Y : EXIT_Y;
	}
	/** Out of a planet into space, one comes out this far beyond its atmosphere (so as not to fall straight back in). */
	public static final double LEAVE_MARGIN = 40;
	/** Space's thin qi: this share of the lower realms'. */
	public static final double SPACE_QI = 0.25;
	/** The weakest who can bear the void. */
	public static final Realm MIN_REALM = Realm.NASCENT_SOUL;
	/** Nearing a planet, the land one is heading for is asked for ahead from this far out of its atmosphere. */
	public static final double LOAD_AHEAD = 160;
	/** Coming down where the land isn't ready yet, one is held at the atmosphere's edge at most this long (ticks). */
	public static final int HOLD_TICKS = 100;
	/** A star's heat burns within its radius times this; inside its radius, it consumes. */
	public static final double STAR_HEAT = 1.6;
	/** After arriving in a world, this long (ticks) before rising out of it again counts. */
	private static final int SETTLE_TICKS = 100;
	/**
	 * Flying through space costs years (the user's ask, 2026-10-21): this many for every block flown there, the same at every realm,
	 * so the way between the stars is a risk to all below Four Axis (to the next star, some 16,000 blocks, about 160 years); a Starborn
	 * Body pays the less by its speed there ({@link #yearsPerBlock}). Only flight counts (an arrival, a hold at a planet's edge, a
	 * teleport are none); on a planet's land one ages as anywhere (Lifespan).
	 */
	public static final double YEARS_PER_BLOCK = 0.01;
	/** A step longer than this in a tick is no flight but a teleport, and costs nothing (the swiftest step farther: {@link #maxStep}). */
	private static final double MAX_STEP = 64;
	/** One flying faster than this a tick (blocks) can cross the look-ahead between two looks: the planet its way meets is looked for instead. */
	private static final double SWIFT_AHEAD = LOAD_AHEAD / 20;
	/** ...that far ahead along its way (ticks). */
	private static final int AHEAD_TICKS = 40;
	/**
	 * One flying space faster than this a tick (blocks: a chunk a tick) loads none of its empty chunks while it does ({@link #zooming}):
	 * the server can't make them as fast, and making, sending and saving the ones it can ate its ticks (measured 2026-10-22: a True
	 * Immortal's sprint, over a hundred chunks a tick, held the server at 60 to 100 ms a tick).
	 */
	private static final double ZOOM_STEP = 16;
	/**
	 * ...and for this long after its last such step (ticks): a flier's packets come in bunched, two one tick and none the next, and
	 * its chunks loaded and let go again at every lull cost as much as loading them all along.
	 */
	private static final int ZOOM_HOLD = 40;
	/** Those flying space faster than {@link #ZOOM_STEP}, and the server tick of their last step so swift. */
	private static final Map<UUID, Integer> ZOOMING = new HashMap<>();

	/** Where each player's height was last tick, to tell rising from falling. */
	private static final Map<UUID, Double> LAST_Y = new HashMap<>();
	/** The server tick each player last arrived in a world (any way). */
	private static final Map<UUID, Integer> ARRIVED = new HashMap<>();
	/** Each player's last-told time for a throttled line (ticks). */
	private static final Map<UUID, Integer> TOLD = new HashMap<>();
	/** Players held at a planet's edge while its land is made ready. */
	private static final Map<UUID, Hold> HOLDS = new HashMap<>();
	/** The land asked for ahead for each player's approach, so it isn't asked for every tick. */
	private static final Map<UUID, Long> PREPARED = new HashMap<>();
	/** Space travel's years (YEARS_PER_BLOCK): each player's place last tick, what this trip out into space has cost, what is still to pay. */
	private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
	private static final Map<UUID, Double> TRIP = new HashMap<>();
	private static final Map<UUID, Double> OWED = new HashMap<>();
	/** The server tick each player was last moved by the server itself (a teleport: {@link #teleported}), to tell a jump from a flight. */
	private static final Map<UUID, Integer> TELEPORTED = new HashMap<>();
	/** Those told they no longer have the years to fly straight home (told again only once they have, and then haven't). */
	private static final java.util.Set<UUID> NO_WAY_HOME = new java.util.HashSet<>();

	private record Hold(int planet, Vec3 at, BlockPos column, int since) {}

	public static void registerWorldgen() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, DefyingTheHeavens.id("planet_surface"), PlanetSurfaceGenerator.CODEC);
		Registry.register(BuiltInRegistries.BIOME_SOURCE, DefyingTheHeavens.id("planets"), PlanetBiomeSource.CODEC);
	}

	public static void register() {
		// The catalog first, before any world (or chunk) is made.
		ServerLifecycleEvents.SERVER_STARTING.register(server -> AstralPlanets.forSeed(AstralPlanets.astralSeed(server.getWorldData().worldGenOptions().seed())));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSeed(handler.getPlayer()));
		ServerTickEvents.END_SERVER_TICK.register(AstralRealm::tick);
		// Things dropped in space (a death's too) and the experience let loose there hang where they are, slowing to a stop as
		// anything dropped does: with no ground under them they fell into the void and were gone. Without the little hop up a
		// thing is let go with (weightless, it carried them ten blocks up), they hang at the height they were let go.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (ModDimensions.isSpace(level.dimension()) && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
				entity.setNoGravity(true);
				entity.setDeltaMovement(entity.getDeltaMovement().multiply(1, 0, 1));
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
		AstralCommand.register();
	}

	/** S2C astral_seed: the Astral Realm's seed, from which the client works out the same stars and planets. */
	public static void sendSeed(ServerPlayer player) {
		AstralPlanets planets = AstralPlanets.get();
		if (planets == null) return;
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeLong(planets.seed());
		ServerPlayNetworking.send(player, ModPackets.ASTRAL_SEED, buf);
	}

	/**
	 * How fast one flies in space (vanilla flying speed): by the realm one wields, faster with every stage; from Void Shattering on, as
	 * no one below it can (the user's ask, 2026-10-22: "void shattering beings and onwards should be able to travel through space ...
	 * super fast"): Void Shattering some four times Four Axis's, Sacred Vessel six, Entering Nirvana nine, True Immortal nineteen and
	 * more. To the next star (some 16,000 blocks) sprinting: Four Axis about two minutes, Void Shattering half a minute, True Immortal
	 * five or six seconds. So swift a flier passes through space unchecked for blocks ({@link #passesThrough}).
	 */
	public static float spaceSpeed(Realm realm, Stage stage) {
		int s = stage.ordinal();
		if (realm.ordinal() < MIN_REALM.ordinal()) return 0.08f;
		return switch (realm) {
			case NASCENT_SOUL -> 0.12f + s * 0.012f;
			case HEAVENLY_BEING -> 0.20f + s * 0.016f;
			case DAO_INTEGRATION -> 0.44f + s * 0.03f;
			case VOID_SHATTERING -> 1.2f + s * 0.1f;
			case SACRED_VESSEL -> 2.0f + s * 0.15f;
			case ENTERING_NIRVANA -> 3.0f + s * 0.25f;
			case TRUE_IMMORTAL -> 6.0f + s * 0.5f;
			default -> 0.32f + s * 0.02f; // Four Axis
		};
	}

	/** Flying space faster than this (vanilla flying speed: past any Four Axis, a Starborn Body's too) one passes through it unchecked ({@link #passesThrough}). */
	public static final float SWIFT = 1.0f;

	/**
	 * Whether {@code p} flies space faster than {@link #SWIFT}: nothing there is a block (the client draws the stars and planets), and a
	 * way of a hundred blocks and more a tick, swept for anything to bump into, would cost the server and the client dearly, so it goes
	 * through as a spectator does (PlayerMixin). Both sides tell it alike, by the flight's speed (vanilla sends it to the client).
	 */
	public static boolean passesThrough(net.minecraft.world.entity.player.Player p) {
		return ModDimensions.isSpace(p.level().dimension()) && p.getAbilities().flying && p.getAbilities().getFlyingSpeed() > SWIFT;
	}

	/**
	 * {@link #spaceSpeed} of the higher path, as the realm lets them wield it (either path: TwoPaths#higherRank); a Starborn Body's
	 * {@link #starborn} times that.
	 */
	public static float spaceSpeed(PlayerCultivation c) {
		int rank = TwoPaths.higherRank(c, true);
		float speed = c.isMortal() || rank < 0 ? 0.08f : spaceSpeed(Realm.byIndex(rank / Stage.values().length), Stage.byIndex(rank % Stage.values().length));
		return c.hasPhysique(Physique.STARBORN_BODY) ? starborn(speed) : speed;
	}

	/** A Starborn Body (Physique) flies through space this many times as fast. */
	public static final float STARBORN_SPEED = 1.5f;

	/** {@code speed} as a Starborn Body flies it. */
	public static float starborn(float speed) {
		return speed * STARBORN_SPEED;
	}

	/** The realm from which the void bears a Starborn Body (Physique), a realm before anyone else. */
	public static final Realm STARBORN_REALM = Realm.CORE_FORMATION;

	/** Space's qi as {@code c} draws it: {@link #SPACE_QI}, whole to a Starborn Body (Physique). */
	public static double spaceQi(PlayerCultivation c) {
		return c.hasPhysique(Physique.STARBORN_BODY) ? 1.0 : SPACE_QI;
	}

	/**
	 * Strong enough to bear the void: a Nascent Soul or above on either path (as the realm lets them wield it), a Starborn Body from
	 * Core Formation; creative and spectators always. Of the dead, only a Ghost King, whose hands are its own (Incorporeal#isRuledGhost):
	 * the dead of Four Axis on the ghost path seek their axes among the settlements too (the user's ask, 2026-10-19; FourAxis,
	 * Settlements).
	 */
	public static boolean canBearTheVoid(ServerPlayer p) {
		if (p.isCreative() || p.isSpectator()) return true;
		PlayerCultivation c = CultivationManager.get(p);
		Realm least = c.hasPhysique(Physique.STARBORN_BODY) ? STARBORN_REALM : MIN_REALM;
		return !c.isMortal() && TwoPaths.higherRank(c, true) >= PlayerCultivation.rank(least, Stage.EARLY) && !SoulProjection.isSoul(p)
				&& !Incorporeal.isRuledGhost(p);
	}

	public static void tick(MinecraftServer server) {
		AstralPlanets planets = AstralPlanets.get();
		ServerLevel space = server.getLevel(ModDimensions.ASTRAL_VOID);
		ServerLevel surfaces = server.getLevel(ModDimensions.ASTRAL_SURFACES);
		if (planets == null || space == null || surfaces == null) return;
		int now = server.getTickCount();
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
			// A projected soul goes nowhere by these ways: its body stays where it is, and the tether holds it near.
			if (!p.isAlive() || SoulProjection.isSoul(p)) continue;
			UUID id = p.getUUID();
			Double before = LAST_Y.put(id, p.getY());
			boolean rising = before != null && p.getY() > before + 0.02;
			boolean settled = now - ARRIVED.getOrDefault(id, -SETTLE_TICKS) >= SETTLE_TICKS;
			ResourceKey<Level> dimension = p.level().dimension();
			try {
				// Out of space (a death, a command, anything but the ways out below): the trip's years paid, its HUD line taken away.
				if (dimension != ModDimensions.ASTRAL_VOID && TRIP.containsKey(id)) leftSpace(p);
				if (dimension != ModDimensions.ASTRAL_VOID) {
					ZOOMING.remove(id);
					TICK_SEEN.remove(id);
				}
				// Out of the Overworld's sky, or the Upper Realm's (its heaven, over the same world: the user's ask, 2026-10-19).
				if (dimension == Level.OVERWORLD || dimension == ModDimensions.UPPER_REALM) {
					if (p.getY() > exitY(dimension) && rising && settled) riseFromOverworld(p, space, planets);
				} else if (dimension == ModDimensions.ASTRAL_VOID) {
					tickSpace(p, space, surfaces, planets, now);
				} else if (dimension == ModDimensions.ASTRAL_SURFACES) {
					tickSurface(p, space, planets, rising && settled);
				} else {
					HOLDS.remove(id);
				}
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Astral Realm routing failed for {}", p.getScoreboardName(), e);
			}
		}
		VestigeSpawner.tick(space, planets);
		Settlements.tick(server, surfaces, planets);
	}

	// --- The Overworld's sky ---

	private static void riseFromOverworld(ServerPlayer p, ServerLevel space, AstralPlanets planets) {
		if (!canBearTheVoid(p)) {
			tell(p, ModLang.MSG_ASTRAL_TOO_WEAK, 100);
			return;
		}
		AstralData.get(p.server).setDeparture(p.getUUID(), p.getX(), p.getZ(), ModDimensions.isUpperRealm(p.level().dimension()));
		AstralPlanets.Planet overworld = planets.overworld();
		Vec3 way = overworldWay(planets, p.getX(), p.getZ());
		Vec3 at = overworld.pos().add(way.scale(overworld.atmosphere() + LEAVE_MARGIN));
		move(p, space, at, way);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ASTRAL_ENTER).withStyle(ChatFormatting.LIGHT_PURPLE));
		space.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.5f);
	}

	/**
	 * Where over the Overworld one rises out of it: its side lit by its star, turned a little for every 20,000 blocks one rose from
	 * the world's middle (so fliers from far apart come out apart).
	 */
	static Vec3 overworldWay(AstralPlanets planets, double x, double z) {
		Vec3 sun = planets.star(0).pos().subtract(planets.overworld().pos()).normalize();
		Vec3 base = new Vec3(0, 0.55, 0).add(sun.scale(0.85)).normalize();
		double turn = Mth.clamp(x / 20000.0, -1.2, 1.2), tilt = Mth.clamp(-z / 20000.0, -0.6, 0.6);
		double c = Math.cos(turn), s = Math.sin(turn);
		Vec3 turned = new Vec3(base.x * c + base.z * s, base.y, -base.x * s + base.z * c);
		return turned.add(0, tilt, 0).normalize();
	}

	// --- Space ---

	private static void tickSpace(ServerPlayer p, ServerLevel space, ServerLevel surfaces, AstralPlanets planets, int now) {
		UUID id = p.getUUID();
		keepTicking(p, space);
		PlayerCultivation c = CultivationManager.get(p);
		if (c.setPlaceQi(spaceQi(c))) CultivationManager.sync(p);

		// Space has a floor and a ceiling, far above and below anything in it: one is kept between them.
		if (p.getY() < AstralPlanets.SPACE_FLOOR || p.getY() > AstralPlanets.SPACE_CEILING) {
			double y = Mth.clamp(p.getY(), AstralPlanets.SPACE_FLOOR + 1, AstralPlanets.SPACE_CEILING - 1);
			p.connection.teleport(p.getX(), y, p.getZ(), p.getYRot(), p.getXRot());
		}

		Hold hold = HOLDS.get(id);
		if (hold != null) {
			AstralPlanets.Planet planet = planets.planet(hold.planet);
			if (planet != null && (SpatialArrivals.ready(surfaces, hold.column) || now - hold.since >= HOLD_TICKS)) {
				HOLDS.remove(id);
				land(p, surfaces, planet, hold.at);
			} else if (p.position().distanceToSqr(hold.at) > 0.25) {
				p.connection.teleport(hold.at.x, hold.at.y, hold.at.z, p.getYRot(), p.getXRot());
			}
			return;
		}

		Vec3 from = LAST_POS.get(id); // where it was last tick (none just come out into space)
		travel(p, planets);
		Vec3 at = p.position();
		AstralPlanets.Planet into = planets.atmosphereAt(at);
		// The swift (spaceSpeed: Void Shattering on) can cross an atmosphere's edge, or a small moon's whole air, between two ticks:
		// they come down where their way met it.
		boolean flew = from != null && from.distanceToSqr(at) > 1 && from.distanceTo(at) <= maxStep(p);
		zoom(p, flew && from.distanceToSqr(at) > ZOOM_STEP * ZOOM_STEP, now);
		if (into == null && flew) {
			AstralPlanets.Crossing crossing = planets.atmosphereAlong(from, at);
			if (crossing != null) {
				into = crossing.planet();
				at = crossing.at();
			}
		}
		if (into != null) {
			if (into.isOverworld()) {
				comeHome(p);
				return;
			}
			BlockPos column = landingColumn(into, at);
			SpatialArrivals.prepare(surfaces, column);
			if (SpatialArrivals.ready(surfaces, column) || p.isCreative() || p.isSpectator()) {
				land(p, surfaces, into, at);
			} else {
				HOLDS.put(id, new Hold(into.index(), at, column, now));
				p.displayClientMessage(Component.translatable(ModLang.MSG_ASTRAL_HOLD, into.name()), true);
			}
			return;
		}

		// Nearing a planet: the land under one's way in is made ready ahead, on the world-generation threads. One too swift for the
		// look-ahead to catch between two looks has the land made ready where its way will meet a planet in the next two seconds.
		if (now % 10 == 0) {
			AstralPlanets.Planet near = null;
			Vec3 meet = at;
			Vec3 step = flew ? at.subtract(from) : Vec3.ZERO;
			if (step.lengthSqr() > SWIFT_AHEAD * SWIFT_AHEAD) {
				AstralPlanets.Crossing ahead = planets.atmosphereAlong(at, at.add(step.scale(AHEAD_TICKS)));
				if (ahead != null) {
					near = ahead.planet();
					meet = ahead.at();
				}
			} else {
				near = planets.atmosphereAt(at, LOAD_AHEAD);
			}
			if (near != null && !near.isOverworld()) {
				BlockPos column = landingColumn(near, meet);
				Long last = PREPARED.get(id);
				long key = new net.minecraft.world.level.ChunkPos(column).toLong();
				if (last == null || last != key) {
					PREPARED.put(id, key);
					SpatialArrivals.prepare(surfaces, column);
				}
			}
		}

		// A star's heat (never a Starborn Body's harm: Physique).
		if (now % 20 == 0 && !p.isCreative() && !p.isSpectator() && !c.hasPhysique(Physique.STARBORN_BODY)) {
			AstralPlanets.Star star = planets.starNear(at, STAR_HEAT);
			if (star != null) {
				double d = at.distanceTo(star.pos()) / star.radius();
				float harm = d < 1 ? 1000 : (float) (4 + 26 * (STAR_HEAT - d) / (STAR_HEAT - 1));
				p.setSecondsOnFire(4);
				p.hurt(d < 1 ? space.damageSources().lava() : space.damageSources().onFire(), harm);
				tell(p, ModLang.MSG_ASTRAL_STAR_HEAT, 60, star.name());
			}
		}
	}

	/**
	 * A tick in space: what {@code p} flew since the last costs its years ({@link #YEARS_PER_BLOCK}), paid every half second (Lifespan#age:
	 * to the span's end and no further, when the heavens come for them out here); the HUD told what the trip has cost, the years left
	 * and what flying straight home would cost, and they are warned once those left can't pay for it. Creative and spectating players
	 * pay nothing, nor a True Immortal (Lifespan#endless: its years have no end).
	 */
	private static void travel(ServerPlayer p, AstralPlanets planets) {
		UUID id = p.getUUID();
		Vec3 at = p.position();
		Vec3 last = LAST_POS.put(id, at);
		if (p.isCreative() || p.isSpectator() || Lifespan.endless(CultivationManager.own(p))) {
			// Nothing to pay, or no end to pay it from (become a True Immortal on the way, say): its HUD line goes, if it was up.
			if (TRIP.containsKey(id)) endTrip(p);
			return;
		}
		double perBlock = yearsPerBlock(CultivationManager.get(p));
		Integer jumped = TELEPORTED.get(id);
		flown(id, last == null ? 0 : at.distanceTo(last), perBlock, maxStep(p), jumped != null && p.server.getTickCount() - jumped <= 2);
		if (p.tickCount % 10 != 0) return;
		pay(p);
		double left = Lifespan.left(CultivationManager.own(p)), home = homeYears(planets, at, perBlock);
		ModPackets.sendSpaceYears(p, true, TRIP.get(id), left, home);
		if (left >= home) {
			NO_WAY_HOME.remove(id);
		} else if (NO_WAY_HOME.add(id)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_SPACE_NO_WAY_HOME, (long) Math.ceil(home), (long) Math.max(0, Math.floor(left)))
					.withStyle(ChatFormatting.RED));
		}
	}

	/**
	 * What a block flown through space costs one of {@code c}: {@link #YEARS_PER_BLOCK}, the less by its speed for a Starborn Body (the
	 * user's ask, 2026-10-21: it flies there {@link #STARBORN_SPEED} times as fast, so it pays as much less).
	 */
	public static double yearsPerBlock(PlayerCultivation c) {
		return c.hasPhysique(Physique.STARBORN_BODY) ? YEARS_PER_BLOCK / STARBORN_SPEED : YEARS_PER_BLOCK;
	}

	/**
	 * {@code blocks} flown through space by {@code id} this tick, owed in years on its trip at {@code perBlock} ({@link #yearsPerBlock}).
	 * Package-private for the game tests, as {@link #pay}.
	 */
	static void flown(UUID id, double blocks, double perBlock) {
		flown(id, blocks, perBlock, MAX_STEP, false);
	}

	/**
	 * {@link #flown}: a step longer than {@code maxStep} (the flier's: {@link #maxStep}) owes nothing only when the server itself moved
	 * them ({@code teleported}: a command, a pearl, a rift: {@link #teleported}); any other is flight, and owes its every block (the user's
	 * pick, 2026-10-24: every long step went free before, so a client sending its moves so crossed space for nothing).
	 */
	static void flown(UUID id, double blocks, double perBlock, double maxStep, boolean teleported) {
		TRIP.putIfAbsent(id, 0.0);
		if (blocks <= 0 || blocks > maxStep && teleported) return;
		OWED.merge(id, blocks * perBlock, Double::sum);
		TRIP.merge(id, blocks * perBlock, Double::sum);
	}

	/** The server is moving {@code player} itself (ServerGamePacketListenerImplMixin, at its teleport): the step it makes is no flight. */
	public static void teleported(ServerPlayer player) {
		TELEPORTED.put(player.getUUID(), player.server.getTickCount());
	}

	/**
	 * The longest step a tick {@code p}'s flight can make as the server takes it (QiFlight#reachAt: several ticks' flight, for a
	 * server behind its client), {@link #MAX_STEP} at least: anything longer is a teleport.
	 */
	static double maxStep(ServerPlayer p) {
		return Math.max(MAX_STEP, QiFlight.reachAt(p.getAbilities().getFlyingSpeed()));
	}

	/** Each space-goer's own tick count as last seen, to tell a tick its level passed it over ({@link #keepTicking}). */
	private static final Map<UUID, Integer> TICK_SEEN = new HashMap<>();

	/**
	 * The level ticks a player only where its chunk is ticking, and one racing space loads none ({@link #zooming}): its own tick (its
	 * count, which so much goes by: the HUD's years, Burn Lifespan, the lifespan tribulation; its menus kept in step, its hurt time
	 * running down) would stop while it raced (the user's report, 2026-10-22: the years line froze). One the level passed over this
	 * tick is ticked here, as the level would have.
	 */
	private static void keepTicking(ServerPlayer p, ServerLevel space) {
		Integer seen = TICK_SEEN.get(p.getUUID());
		if (seen != null && seen == p.tickCount && p.isAlive() && p.level() == space) space.tickNonPassenger(p);
		TICK_SEEN.put(p.getUUID(), p.tickCount);
	}

	/**
	 * {@code p}'s step this tick {@code fast} (past {@link #ZOOM_STEP}) or not: zooming from a fast one until {@link #ZOOM_HOLD} after the
	 * last, its chunks' loading told at once when that changes (ChunkMapMixin).
	 */
	private static void zoom(ServerPlayer p, boolean fast, int now) {
		UUID id = p.getUUID();
		boolean was = ZOOMING.containsKey(id);
		if (fast) ZOOMING.put(id, now);
		else if (was && now - ZOOMING.get(id) > ZOOM_HOLD) ZOOMING.remove(id);
		if (was != ZOOMING.containsKey(id)) p.serverLevel().getChunkSource().move(p);
	}

	/**
	 * Whether {@code p} flies space faster than {@link #ZOOM_STEP}, and so loads none of space's chunks round it (ChunkMapMixin): it
	 * meets nothing there (nothing is a block, and the client flies on without them: client SpaceLoadingMixin), and they load round it
	 * {@link #ZOOM_HOLD} after it slows. No Vestige rises round it meanwhile (VestigeSpawner: it would hang unmoving where nothing is loaded).
	 */
	public static boolean zooming(ServerPlayer p) {
		return ZOOMING.containsKey(p.getUUID()) && ModDimensions.isSpace(p.level().dimension());
	}

	/** What {@code p}'s flight owes, paid (Lifespan#age: to the span's end and no further). */
	static void pay(ServerPlayer p) {
		Double owed = OWED.remove(p.getUUID());
		if (owed != null && owed > 0) Lifespan.age(p, owed);
	}

	/** The years flying straight from {@code at} into the Overworld's air (home: {@link #comeHome}) would cost at {@code perBlock}. */
	static double homeYears(AstralPlanets planets, Vec3 at, double perBlock) {
		AstralPlanets.Planet home = planets.overworld();
		return Math.max(0, at.distanceTo(home.pos()) - home.atmosphere()) * perBlock;
	}

	/** Out of space: what the trip still owes paid, its HUD line taken away, and the next trip begun afresh. */
	private static void leftSpace(ServerPlayer p) {
		LAST_POS.remove(p.getUUID());
		endTrip(p);
	}

	/** A trip's years done with: what it still owes paid (none by creative or spectating players), its HUD line taken away. */
	private static void endTrip(ServerPlayer p) {
		UUID id = p.getUUID();
		if (!p.isCreative() && !p.isSpectator()) pay(p);
		OWED.remove(id);
		TRIP.remove(id);
		NO_WAY_HOME.remove(id);
		ModPackets.sendSpaceYears(p, false, 0, 0, 0);
	}

	/**
	 * The land under where one meets the planet's atmosphere at {@code at}, in the surfaces; or, if that is a lava sea or a lava
	 * river, the nearest ground that isn't (looked for out to {@link #SAFE_REACH}), so nobody comes down into fire.
	 */
	static BlockPos landingColumn(AstralPlanets.Planet planet, Vec3 at) {
		double[] land = PlanetTerrain.landUnder(at.subtract(planet.pos()));
		PlanetTerrain terrain = PlanetTerrain.of(planet);
		double dx = land[0], dz = land[1];
		if (lava(terrain.column(dx, dz, false))) {
			search:
			for (int reach = 48; reach <= SAFE_REACH; reach += 48) {
				int points = Math.max(8, reach / 24);
				for (int i = 0; i < points; i++) {
					double angle = Math.PI * 2 * i / points;
					double x = dx + Math.cos(angle) * reach, z = Mth.clamp(dz + Math.sin(angle) * reach, -AstralPlanets.LAND_POLE + 64, AstralPlanets.LAND_POLE - 64);
					if (!lava(terrain.column(x, z, false))) {
						dx = x;
						dz = z;
						break search;
					}
				}
			}
		}
		dx = Mth.clamp(dx, -AstralPlanets.LAND_RADIUS + 64, AstralPlanets.LAND_RADIUS - 64);
		return new BlockPos(Mth.floor(planet.landX() + dx), 0, Mth.floor(planet.landZ() + dz));
	}

	/** Coming down over lava, the landing is moved at most this far (blocks) to find ground. */
	private static final int SAFE_REACH = 2400;

	private static boolean lava(PlanetTerrain.Column c) {
		return c.underFluid() && c.fluid() == PlanetTerrain.Fluid.LAVA;
	}

	/** Down through {@code planet}'s sky, over the land below where one met it ({@code at}; never over lava). */
	private static void land(ServerPlayer p, ServerLevel surfaces, AstralPlanets.Planet planet, Vec3 at) {
		BlockPos column = landingColumn(planet, at);
		PlanetTerrain.Column c = PlanetTerrain.of(planet).column(column.getX() - planet.landX(), column.getZ() - planet.landZ(), false);
		int y = Math.min(ARRIVAL_MAX_Y, Math.max(c.ground() + Math.max(0, c.spire()), c.fluidTop()) + ARRIVAL_ABOVE);
		Vec3 to = new Vec3(column.getX() + 0.5, y, column.getZ() + 0.5);
		CultivationManager.get(p).setFallProtected(true); // the first landing from the sky is harmless
		move(p, surfaces, to, null);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ASTRAL_LAND, planet.name(), Component.translatable(ModLang.astralKind(planet.kind())),
				String.format("%.2f", planet.qi())).withStyle(ChatFormatting.AQUA));
	}

	/** Back into the Overworld: down where one rose from (or, never having risen from it, over the world's spawn). */
	private static void comeHome(ServerPlayer p) {
		ServerLevel overworld = p.server.overworld();
		double[] from = AstralData.get(p.server).departure(p.getUUID());
		double x, z;
		if (from != null) {
			x = from[0];
			z = from[1];
		} else {
			BlockPos spawn = overworld.getSharedSpawnPos();
			x = spawn.getX() + 0.5;
			z = spawn.getZ() + 0.5;
		}
		CultivationManager.get(p).setFallProtected(true);
		// Risen out of the Upper Realm: back down into it, over the island nearest where they left (SpatialArrivals).
		ServerLevel upper = p.server.getLevel(ModDimensions.UPPER_REALM);
		if (from != null && from.length > 2 && from[2] > 0 && upper != null) {
			BlockPos column = SpatialArrivals.target(upper, (int) Math.floor(x), (int) Math.floor(z));
			SpatialArrivals.prepare(upper, column);
			move(p, upper, new Vec3(column.getX() + 0.5, UPPER_ARRIVAL_Y, column.getZ() + 0.5), null);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_ASTRAL_HOME).withStyle(ChatFormatting.AQUA));
			return;
		}
		move(p, overworld, new Vec3(x, ARRIVAL_MAX_Y, z), null);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ASTRAL_HOME).withStyle(ChatFormatting.AQUA));
	}

	// --- A planet's land ---

	private static void tickSurface(ServerPlayer p, ServerLevel space, AstralPlanets planets, boolean risingOut) {
		AstralPlanets.Planet planet = planets.planetAt(p.getX(), p.getZ());
		PlayerCultivation c = CultivationManager.get(p);
		if (c.setPlaceQi(planet == null ? spaceQi(c) : planet.qi())) CultivationManager.sync(p);
		// Up past the sky, or fallen off the land's edge into the void below it: out into space over the planet.
		boolean fellOff = p.getY() < PlanetTerrain.MIN_Y - 24;
		if (!(p.getY() > EXIT_Y && risingOut) && !fellOff) return;
		if (planet == null) {
			// Between the planets' lands (fallen off an edge): out by the nearest.
			planet = nearestLand(planets, p.getX(), p.getZ());
			if (planet == null) return;
		}
		double dx = Mth.clamp(p.getX() - planet.landX(), -AstralPlanets.LAND_RADIUS, AstralPlanets.LAND_RADIUS);
		double dz = Mth.clamp(p.getZ() - planet.landZ(), -AstralPlanets.LAND_POLE, AstralPlanets.LAND_POLE);
		Vec3 way = PlanetTerrain.directionOver(dx, dz);
		Vec3 at = planet.pos().add(way.scale(planet.atmosphere() + LEAVE_MARGIN));
		move(p, space, at, way);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ASTRAL_LEAVE, planet.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	private static AstralPlanets.Planet nearestLand(AstralPlanets planets, double x, double z) {
		int gx = Mth.clamp(Mth.floor(x / AstralPlanets.CELL) + AstralPlanets.GRID / 2, 0, AstralPlanets.GRID - 1);
		int gz = Mth.clamp(Mth.floor(z / AstralPlanets.CELL) + AstralPlanets.GRID / 2, 0, AstralPlanets.GRID - 1);
		return planets.planet(gz * AstralPlanets.GRID + gx);
	}

	// --- Moving between them ---

	/** To {@code at} in {@code to}, looking along {@code facing} (null: as one looks now), flying on if one was. */
	private static void move(ServerPlayer p, ServerLevel to, Vec3 at, Vec3 facing) {
		float yaw = p.getYRot(), pitch = p.getXRot();
		if (facing != null) {
			yaw = (float) (Mth.atan2(facing.z, facing.x) * Mth.RAD_TO_DEG) - 90.0f;
			pitch = (float) (-Math.asin(Mth.clamp(facing.y, -1, 1)) * Mth.RAD_TO_DEG) + 20.0f;
		}
		boolean flying = p.getAbilities().flying;
		HOLDS.remove(p.getUUID());
		PREPARED.remove(p.getUUID());
		// Space travel's years: out of space, the trip paid and its line taken away; into it, a new trip (the arrival is no flight).
		if (ModDimensions.isSpace(p.level().dimension()) && to.dimension() != p.level().dimension()) leftSpace(p);
		LAST_POS.remove(p.getUUID());
		p.teleportTo(to, at.x, at.y, at.z, yaw, pitch);
		p.fallDistance = 0;
		ARRIVED.put(p.getUUID(), p.server.getTickCount());
		LAST_Y.put(p.getUUID(), at.y);
		if (flying && p.getAbilities().mayfly && !p.getAbilities().flying) {
			p.getAbilities().flying = true;
			p.onUpdateAbilities();
		}
		RealmSuppressionHandler.update(p);
	}

	/** A line over the hotbar, at most every {@code every} ticks. */
	private static void tell(ServerPlayer p, String key, int every, Object... args) {
		int now = p.server.getTickCount();
		Integer last = TOLD.get(p.getUUID());
		if (last != null && now - last < every && now >= last) return;
		TOLD.put(p.getUUID(), now);
		p.displayClientMessage(Component.translatable(key, args), true);
	}

	public static void forget(UUID id) {
		LAST_Y.remove(id);
		ARRIVED.remove(id);
		TOLD.remove(id);
		HOLDS.remove(id);
		PREPARED.remove(id);
		LAST_POS.remove(id);
		TRIP.remove(id);
		OWED.remove(id);
		TELEPORTED.remove(id);
		NO_WAY_HOME.remove(id);
		ZOOMING.remove(id);
		TICK_SEEN.remove(id);
		VestigeSpawner.forget(id);
		Settlements.forget(id);
	}

	public static void clear() {
		LAST_Y.clear();
		ARRIVED.clear();
		TOLD.clear();
		HOLDS.clear();
		PREPARED.clear();
		LAST_POS.clear();
		TRIP.clear();
		OWED.clear();
		TELEPORTED.clear();
		NO_WAY_HOME.clear();
		ZOOMING.clear();
		TICK_SEEN.clear();
		VestigeSpawner.clear();
		Settlements.clear();
	}

	/**
	 * Where each player last rose out of the Overworld or the Upper Realm (its x and z, and 1 if the Upper Realm), so flying back into
	 * the Overworld's sky brings them down there, in the realm they left. Saved.
	 */
	public static final class AstralData extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_astral";
		private final Map<UUID, double[]> departures = new HashMap<>();

		public static AstralData get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(AstralData::load, AstralData::new, NAME);
		}

		/** {x, z, 1 if they rose out of the Upper Realm else 0}, or null. */
		public double[] departure(UUID player) { return departures.get(player); }

		public void setDeparture(UUID player, double x, double z, boolean upper) {
			departures.put(player, new double[] {x, z, upper ? 1 : 0});
			setDirty();
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			ListTag list = new ListTag();
			departures.forEach((player, at) -> {
				CompoundTag entry = new CompoundTag();
				entry.putUUID("Player", player);
				entry.putDouble("X", at[0]);
				entry.putDouble("Z", at[1]);
				if (at.length > 2 && at[2] > 0) entry.putBoolean("Upper", true);
				list.add(entry);
			});
			tag.put("Departures", list);
			return tag;
		}

		public static AstralData load(CompoundTag tag) {
			AstralData data = new AstralData();
			for (Tag t : tag.getList("Departures", Tag.TAG_COMPOUND)) {
				CompoundTag entry = (CompoundTag) t;
				if (entry.hasUUID("Player")) data.departures.put(entry.getUUID("Player"),
						new double[] {entry.getDouble("X"), entry.getDouble("Z"), entry.getBoolean("Upper") ? 1 : 0});
			}
			return data;
		}
	}

	private AstralRealm() {}
}
