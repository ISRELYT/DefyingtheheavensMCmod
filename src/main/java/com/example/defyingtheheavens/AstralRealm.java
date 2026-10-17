package com.example.defyingtheheavens;

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
 *   <li><b>Going up.</b> A Nascent Soul or above, flying up past {@link #EXIT_Y} in the Overworld, rises out of its sky into space
 *   ({@link ModDimensions#ASTRAL_VOID}), over the Overworld's lit side. Below Nascent Soul the sky thins to nothing and they are
 *   told so. Where they rose from is remembered: flying back into the Overworld brings them down there.</li>
 *   <li><b>Space.</b> Stars and their planets (AstralPlanets), drawn by the client; nothing there is a block. One always flies there,
 *   at a speed of one's realm ({@link #spaceSpeed}), on no qi; qi gathers at a quarter of the lower realms' ({@link #SPACE_QI}). A
 *   star burns whoever comes too near. Vestiges of the Void hunt there (VoidVestige).</li>
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
	/** Coming down onto a planet: this high over its ground (never above {@link #ARRIVAL_MAX_Y}); onto the Overworld, at that. */
	public static final int ARRIVAL_ABOVE = 50, ARRIVAL_MAX_Y = 325;
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

	/** How fast one flies in space (vanilla flying speed): by the realm one wields, faster with every stage. */
	public static float spaceSpeed(Realm realm, Stage stage) {
		int s = stage.ordinal();
		if (realm.ordinal() < MIN_REALM.ordinal()) return 0.08f;
		return switch (realm) {
			case NASCENT_SOUL -> 0.12f + s * 0.012f;
			case HEAVENLY_BEING -> 0.20f + s * 0.016f;
			default -> 0.32f + s * 0.02f;
		};
	}

	public static float spaceSpeed(PlayerCultivation c) {
		return c.isMortal() ? 0.08f : spaceSpeed(c.getEffectiveRealm(), c.getEffectiveStage());
	}

	/** Strong enough to bear the void: a Nascent Soul or above (as the realm lets them wield it); creative and spectators always. */
	public static boolean canBearTheVoid(ServerPlayer p) {
		if (p.isCreative() || p.isSpectator()) return true;
		PlayerCultivation c = CultivationManager.get(p);
		return !c.isMortal() && c.getEffectiveRealm().ordinal() >= MIN_REALM.ordinal() && !SoulProjection.isSoul(p) && !Incorporeal.isGhost(p);
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
				if (dimension == Level.OVERWORLD) {
					if (p.getY() > EXIT_Y && rising && settled) riseFromOverworld(p, space, planets);
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
	}

	// --- The Overworld's sky ---

	private static void riseFromOverworld(ServerPlayer p, ServerLevel space, AstralPlanets planets) {
		if (!canBearTheVoid(p)) {
			tell(p, ModLang.MSG_ASTRAL_TOO_WEAK, 100);
			return;
		}
		AstralData.get(p.server).setDeparture(p.getUUID(), p.getX(), p.getZ());
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
		PlayerCultivation c = CultivationManager.get(p);
		if (c.setPlaceQi(SPACE_QI)) CultivationManager.sync(p);

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

		Vec3 at = p.position();
		AstralPlanets.Planet into = planets.atmosphereAt(at);
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

		// Nearing a planet: the land under one's way in is made ready ahead, on the world-generation threads.
		if (now % 10 == 0) {
			AstralPlanets.Planet near = planets.atmosphereAt(at, LOAD_AHEAD);
			if (near != null && !near.isOverworld()) {
				BlockPos column = landingColumn(near, at);
				Long last = PREPARED.get(id);
				long key = new net.minecraft.world.level.ChunkPos(column).toLong();
				if (last == null || last != key) {
					PREPARED.put(id, key);
					SpatialArrivals.prepare(surfaces, column);
				}
			}
		}

		// A star's heat.
		if (now % 20 == 0 && !p.isCreative() && !p.isSpectator()) {
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
		move(p, overworld, new Vec3(x, ARRIVAL_MAX_Y, z), null);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ASTRAL_HOME).withStyle(ChatFormatting.AQUA));
	}

	// --- A planet's land ---

	private static void tickSurface(ServerPlayer p, ServerLevel space, AstralPlanets planets, boolean risingOut) {
		AstralPlanets.Planet planet = planets.planetAt(p.getX(), p.getZ());
		PlayerCultivation c = CultivationManager.get(p);
		if (c.setPlaceQi(planet == null ? SPACE_QI : planet.qi())) CultivationManager.sync(p);
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
		VestigeSpawner.forget(id);
	}

	public static void clear() {
		LAST_Y.clear();
		ARRIVED.clear();
		TOLD.clear();
		HOLDS.clear();
		PREPARED.clear();
		VestigeSpawner.clear();
	}

	/** Where each player last rose out of the Overworld (its x and z), so flying back into it brings them down there. Saved. */
	public static final class AstralData extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_astral";
		private final Map<UUID, double[]> departures = new HashMap<>();

		public static AstralData get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(AstralData::load, AstralData::new, NAME);
		}

		public double[] departure(UUID player) { return departures.get(player); }

		public void setDeparture(UUID player, double x, double z) {
			departures.put(player, new double[] {x, z});
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
				list.add(entry);
			});
			tag.put("Departures", list);
			return tag;
		}

		public static AstralData load(CompoundTag tag) {
			AstralData data = new AstralData();
			for (Tag t : tag.getList("Departures", Tag.TAG_COMPOUND)) {
				CompoundTag entry = (CompoundTag) t;
				if (entry.hasUUID("Player")) data.departures.put(entry.getUUID("Player"), new double[] {entry.getDouble("X"), entry.getDouble("Z")});
			}
			return data;
		}
	}

	private AstralRealm() {}
}
