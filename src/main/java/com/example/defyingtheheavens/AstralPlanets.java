package com.example.defyingtheheavens;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Astral Realm's planets and stars, the same for everyone who knows its seed (the server sends it on joining: ModPackets
 * ASTRAL_SEED; it is the world's seed hashed, so it gives away nothing else of the world).
 * <p>
 * Space ({@link ModDimensions#ASTRAL_VOID}) holds star systems: a star and its planets, some with moons and rings. The Overworld
 * is a planet of the home system at space's origin, with a few others round the same star; the other systems lie out from it on
 * a scattered grid some {@link #SYSTEM_SPACING} apart, until there are {@link #PLANETS} planets in all. Planets in space are
 * spheres drawn by the client (they aren't blocks); flying into one sets the flier down on its land.
 * <p>
 * Every planet but the Overworld has its land in {@link ModDimensions#ASTRAL_SURFACES}: a square {@link #CELL} blocks across of a
 * {@link #GRID} by {@link #GRID} grid, the land itself out to {@link #LAND_RADIUS} from the square's middle (PlanetTerrain makes
 * it, by the planet's {@link Kind} and its own numbers). Each has its own qi density, never near the Upper Realm's.
 */
public final class AstralPlanets {
	/** Planets with land of their own: one to each square of the surfaces' grid. */
	public static final int GRID = 60, PLANETS = GRID * GRID;
	/**
	 * Each planet's square of the surfaces, and how far out from its middle its land goes before it ends at the edge of the void:
	 * {@link #LAND_RADIUS} east and west (once round its equator), {@link #LAND_POLE} north and south (pole to pole), so the land
	 * is the planet's map, as true to its shape near the equator as a map can be (PlanetTerrain#lonLat).
	 */
	public static final int CELL = 1_000_000, LAND_RADIUS = 480_000, LAND_POLE = LAND_RADIUS / 2;
	/** About how far apart star systems lie in space. */
	public static final double SYSTEM_SPACING = 16_000;
	/**
	 * The height space is centred on. The void dimension itself is a sliver (ModDimensions#ASTRAL_VOID, 16 blocks: nothing is ever
	 * built in it, and a tall one would cost every client a column of empty sections to every chunk); everything in space lies far
	 * above it, from {@link #SPACE_FLOOR} to {@link #SPACE_CEILING}, where fliers and creatures go as they would anywhere.
	 */
	public static final double SPACE_Y = 12000, SPACE_FLOOR = 300, SPACE_CEILING = 24000;
	/**
	 * How far above and below {@link #SPACE_Y} star systems lie (most of their spacing: space is as deep as it is wide, so the stars
	 * fill the whole sky rather than a band along the horizon), and how far a system's planets stray from its star's plane.
	 */
	public static final double SYSTEM_HEIGHT = 9600, PLANET_TILT = 260;
	/**
	 * Qi density never passes three times the lower realms' (the user's cap, 2026-10-18: at six, a rich planet outdid everything
	 * short of the Upper Realm's ten, {@link PlayerCultivation#UPPER_REALM_QI_MULTIPLIER}).
	 */
	public static final double MAX_QI = 3.0;
	/** Where the Overworld hangs in space, and how large. */
	public static final Vec3 OVERWORLD_POS = new Vec3(0, SPACE_Y, 0);
	public static final float OVERWORLD_RADIUS = 360;

	/**
	 * What a planet is like: its land's blocks and biomes (PlanetTerrain), its look from space (the client's PlanetPainter), its usual
	 * qi. Several kinds, and every planet's own numbers within its kind, keep most planets unlike each other; a few verdant ones come
	 * out much like the Overworld.
	 */
	public enum Kind {
		VERDANT("verdant", 0.8, 1.6),
		TEMPERATE("temperate", 0.8, 1.8),
		OCEAN("ocean", 0.9, 2.0),
		DESERT("desert", 0.4, 1.1),
		MESA("mesa", 0.5, 1.2),
		FROZEN("frozen", 0.5, 1.4),
		JUNGLE("jungle", 1.2, 2.6),
		MUSHROOM("mushroom", 1.0, 2.4),
		VOLCANIC("volcanic", 0.7, 1.8),
		ASHEN("ashen", 0.3, 0.9),
		CRYSTAL("crystal", 1.8, 3.0),
		SWAMP("swamp", 0.9, 1.9),
		BARREN("barren", 0.2, 0.6),
		BLOSSOM("blossom", 1.6, 2.8),
		SAVANNA("savanna", 0.7, 1.5),
		TAIGA("taiga", 0.7, 1.6);

		private final String id;
		private final double qiLow, qiHigh;

		Kind(String id, double qiLow, double qiHigh) {
			this.id = id;
			this.qiLow = qiLow;
			this.qiHigh = qiHigh;
		}

		public String getId() { return id; }

		public static Kind byIndex(int i) {
			Kind[] all = values();
			return i >= 0 && i < all.length ? all[i] : VERDANT;
		}
	}

	/** A star: where it burns, how large, its colour (0xRRGGBB), and its name. */
	public record Star(int index, Vec3 pos, float radius, int colour, String name) {}

	/**
	 * A planet: its place among them ({@code index}; -1 is the Overworld), its star system, the planet it circles if it is a moon
	 * (-1 if not), its kind, where it hangs in space and how large, its qi density, its own seed (the land and the look come from
	 * it), whether it wears rings, and its name.
	 */
	public record Planet(int index, int system, int parent, Kind kind, Vec3 pos, float radius, double qi, long seed, boolean rings,
						 String name) {
		public boolean isOverworld() { return index < 0; }

		/** How far out from its middle a flier is taken down to its land (its atmosphere's edge). */
		public double atmosphere() { return radius * 1.12; }

		/** The middle of its square of land in the surfaces (meaningless for the Overworld). */
		public int landX() { return cellCentre(index % GRID); }

		public int landZ() { return cellCentre(index / GRID); }
	}

	private final long seed;
	private final List<Star> stars;
	private final List<Planet> planets;
	private final Planet overworld;

	private AstralPlanets(long seed) {
		this.seed = seed;
		List<Star> stars = new ArrayList<>();
		List<Planet> planets = new ArrayList<>();
		RandomSource random = RandomSource.create(seed);

		// The home system: the Overworld, its star, and a few others round it.
		Star home = new Star(0, new Vec3(-2600, SPACE_Y + 260, 1500), 900, 0xFFF1C8, "Taiyang");
		stars.add(home);
		overworld = new Planet(-1, 0, -1, Kind.VERDANT, OVERWORLD_POS, OVERWORLD_RADIUS, 1.0, seed ^ 0x6F766572L, false, "The Overworld");
		int siblings = 3 + random.nextInt(3);
		for (int i = 0; i < siblings && planets.size() < PLANETS; i++) {
			// Out past the star's heat as every system's planets are (its radius 1.8 times, and more), and clear of the Overworld.
			double orbit = home.radius() * 1.8 + 900 + i * 1700 + random.nextDouble() * 900;
			addPlanet(planets, home, orbit, random);
		}

		// The other systems, out from home on a scattered grid, nearest first.
		List<int[]> cells = new ArrayList<>();
		int reach = 40;
		for (int gx = -reach; gx <= reach; gx++) for (int gz = -reach; gz <= reach; gz++) if (gx != 0 || gz != 0) cells.add(new int[]{gx, gz});
		cells.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
		for (int[] cell : cells) {
			if (planets.size() >= PLANETS) break;
			RandomSource here = RandomSource.create(seed ^ (cell[0] * 341873128712L + cell[1] * 132897987541L));
			if (here.nextFloat() < 0.18f) continue; // some reaches of space are empty
			Vec3 centre = new Vec3((cell[0] + (here.nextDouble() - 0.5) * 0.6) * SYSTEM_SPACING, SPACE_Y + (here.nextDouble() * 2 - 1) * SYSTEM_HEIGHT,
					(cell[1] + (here.nextDouble() - 0.5) * 0.6) * SYSTEM_SPACING);
			Star star = new Star(stars.size(), centre, 600 + here.nextFloat() * 1500, starColour(here), name(here) );
			stars.add(star);
			int count = 2 + here.nextInt(6);
			for (int i = 0; i < count && planets.size() < PLANETS; i++) {
				double orbit = star.radius() * 1.8 + 900 + i * (1200 + here.nextDouble() * 1400);
				addPlanet(planets, star, orbit, here);
			}
		}
		this.stars = Collections.unmodifiableList(stars);
		this.planets = Collections.unmodifiableList(planets);
	}

	/**
	 * A planet (and maybe a moon or two of it) on an orbit of {@code orbit} round {@code star}, at a place clear of every other
	 * planet's air in its system ({@link #clear}: turned further round its orbit, or out to a wider one, until it is); the moons
	 * count toward {@link #PLANETS} too.
	 */
	private void addPlanet(List<Planet> planets, Star sun, double orbit, RandomSource random) {
		int system = sun.index();
		Vec3 star = sun.pos();
		int index = planets.size();
		Kind kind = Kind.byIndex(random.nextInt(Kind.values().length));
		float radius = kind == Kind.BARREN ? 140 + random.nextFloat() * 180 : 220 + random.nextFloat() * 460;
		Vec3 at = aroundStar(star, orbit, random);
		for (int tries = 1; tries < 24 && !clear(planets, sun, at, radius); tries++) at = aroundStar(star, orbit + (tries / 6) * 900, random);
		long planetSeed = seed * 6364136223846793005L + index * 1442695040888963407L;
		boolean rings = kind != Kind.BARREN && random.nextFloat() < 0.14f;
		planets.add(new Planet(index, system, -1, kind, at, radius, qiFor(kind, random), planetSeed, rings, name(random)));
		int moons = random.nextFloat() < 0.32f ? 1 + (random.nextFloat() < 0.3f ? 1 : 0) : 0;
		for (int m = 0; m < moons && planets.size() < PLANETS; m++) {
			float moonRadius = 90 + random.nextFloat() * 110;
			Vec3 moonAt = at;
			for (int tries = 0; tries < 24; tries++) {
				Vec3 offset = randomDirection(random).multiply(1, 0.35, 1).normalize().scale(radius * 2.2 + 300 + random.nextDouble() * 700 + tries * 150);
				// Later tries swing it round to the planet's far side from its star.
				if (tries >= 12) offset = offset.add(at.subtract(star).normalize().scale(offset.length())).normalize().scale(offset.length());
				moonAt = at.add(offset);
				if (clear(planets, sun, moonAt, moonRadius)) break;
			}
			Kind moonKind = random.nextFloat() < 0.7f ? Kind.BARREN : random.nextBoolean() ? Kind.FROZEN : Kind.ASHEN;
			int moonIndex = planets.size();
			planets.add(new Planet(moonIndex, system, index, moonKind, moonAt, moonRadius, qiFor(moonKind, random),
					seed * 6364136223846793005L + moonIndex * 1442695040888963407L, false, name(random)));
		}
	}

	/**
	 * Room for a planet of {@code radius} at {@code at}: its air and every other's in its system (and the Overworld's) well apart,
	 * and out of its star's heat (AstralRealm#STAR_HEAT).
	 */
	private boolean clear(List<Planet> planets, Star sun, Vec3 at, float radius) {
		int system = sun.index();
		double air = radius * 1.12 + 150;
		if (at.distanceTo(sun.pos()) - air < sun.radius() * AstralRealm.STAR_HEAT) return false;
		if (system == 0 && at.distanceTo(OVERWORLD_POS) < air + OVERWORLD_RADIUS * 1.12) return false;
		for (int i = planets.size() - 1; i >= 0 && planets.get(i).system() == system; i--) {
			Planet other = planets.get(i);
			if (at.distanceTo(other.pos()) < air + other.atmosphere()) return false;
		}
		return true;
	}

	private static double qiFor(Kind kind, RandomSource random) {
		double qi = Mth.lerp(random.nextDouble(), kind.qiLow, kind.qiHigh);
		// Now and then a planet rich in qi beyond its kind: a spirit world (but never past MAX_QI).
		if (random.nextFloat() < 0.04f) qi *= 1.6;
		return Math.min(MAX_QI, Math.round(qi * 100) / 100.0);
	}

	private static Vec3 aroundStar(Vec3 star, double orbit, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2;
		return star.add(Math.cos(angle) * orbit, (random.nextDouble() * 2 - 1) * PLANET_TILT, Math.sin(angle) * orbit);
	}

	private static Vec3 randomDirection(RandomSource random) {
		double z = random.nextDouble() * 2 - 1, a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(1 - z * z);
		return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
	}

	/** Stars by kind: most yellow-white, some orange or red, a few blue-white. */
	private static int starColour(RandomSource random) {
		float f = random.nextFloat();
		if (f < 0.12f) return 0xA9C4FF;
		if (f < 0.45f) return 0xFFF4DE;
		if (f < 0.72f) return 0xFFE2A0;
		if (f < 0.90f) return 0xFFB36B;
		return 0xFF7A52;
	}

	private static final String[] SYLLABLES = {"qing", "lan", "xuan", "ming", "han", "yue", "tai", "yi", "zhao", "ling", "hua", "chen",
			"shan", "he", "feng", "yun", "jin", "mu", "shui", "huo", "tu", "xing", "ye", "wu", "kong", "zi", "bai", "hong", "lu", "jiang",
			"tian", "di", "an", "yao", "su", "rong", "qian", "kun", "li", "zhen", "dui", "gen", "xun", "luo", "mei", "song", "zhu", "lian"};

	/** A name of two or three syllables, capitalised ("Hanyue", "Qinglanyao"). */
	private static String name(RandomSource random) {
		StringBuilder b = new StringBuilder();
		int count = 2 + (random.nextFloat() < 0.25f ? 1 : 0);
		for (int i = 0; i < count; i++) b.append(SYLLABLES[random.nextInt(SYLLABLES.length)]);
		return Character.toUpperCase(b.charAt(0)) + b.substring(1);
	}

	/** The middle of grid square {@code i} along one axis: the squares run from -30 million to +30 million. */
	static int cellCentre(int i) {
		return (i - GRID / 2) * CELL + CELL / 2;
	}

	// --- Lookups ---

	public long seed() { return seed; }

	public List<Star> stars() { return stars; }

	/** The planets with land of their own, by index (the Overworld isn't among them: {@link #overworld()}). */
	public List<Planet> planets() { return planets; }

	public Planet overworld() { return overworld; }

	/** Planet {@code index}, the Overworld for -1, or null. */
	public Planet planet(int index) {
		if (index < 0) return overworld;
		return index < planets.size() ? planets.get(index) : null;
	}

	public Star star(int system) {
		return system >= 0 && system < stars.size() ? stars.get(system) : null;
	}

	/**
	 * The planet whose land holds (x, z) of the surfaces, or null past every planet's edge (in the void between them). The land's
	 * edges fall on chunk borders, so a chunk is all one planet's land or all void.
	 */
	public Planet planetAt(double x, double z) {
		int gx = Mth.floor(x / CELL) + GRID / 2, gz = Mth.floor(z / CELL) + GRID / 2;
		if (gx < 0 || gz < 0 || gx >= GRID || gz >= GRID) return null;
		Planet planet = planet(gz * GRID + gx);
		if (planet == null) return null;
		double dx = x - planet.landX(), dz = z - planet.landZ();
		return dx >= -LAND_RADIUS && dx < LAND_RADIUS && dz >= -LAND_POLE && dz < LAND_POLE ? planet : null;
	}

	/** The planet (or the Overworld) whose atmosphere {@code at} in space is inside, or null. */
	public Planet atmosphereAt(Vec3 at) {
		return atmosphereAt(at, 0);
	}

	/** The planet (or the Overworld) whose atmosphere {@code at} is within {@code margin} of (or inside), or null. */
	public Planet atmosphereAt(Vec3 at, double margin) {
		if (at.distanceToSqr(overworld.pos()) < sq(overworld.atmosphere() + margin)) return overworld;
		for (Planet planet : planets) {
			double reach = planet.atmosphere() + margin;
			if (Math.abs(at.x - planet.pos().x) < reach && Math.abs(at.z - planet.pos().z) < reach && at.distanceToSqr(planet.pos()) < reach * reach) return planet;
		}
		return null;
	}

	/** The star whose heat {@code at} is inside (within {@code reach} times its radius), or null. */
	public Star starNear(Vec3 at, double reach) {
		for (Star star : stars) {
			double r = star.radius() * reach;
			if (Math.abs(at.x - star.pos().x) < r && Math.abs(at.z - star.pos().z) < r && at.distanceToSqr(star.pos()) < r * r) return star;
		}
		return null;
	}

	/** The nearest star system to {@code at} in space. */
	public Star nearestStar(Vec3 at) {
		Star best = stars.get(0);
		double bestDistance = Double.MAX_VALUE;
		for (Star star : stars) {
			double d = star.pos().distanceToSqr(at);
			if (d < bestDistance) {
				bestDistance = d;
				best = star;
			}
		}
		return best;
	}

	private static double sq(double v) { return v * v; }

	// --- The one in use ---

	private static volatile AstralPlanets current;

	/** The catalog for the astral seed {@code seed} (made once per seed; the server's at start, the client's when told it). */
	public static AstralPlanets forSeed(long seed) {
		AstralPlanets c = current;
		if (c != null && c.seed == seed) return c;
		c = new AstralPlanets(seed);
		current = c;
		return c;
	}

	/** The catalog in use on this side, or null before the seed is known. */
	public static AstralPlanets get() { return current; }

	/** The Astral Realm's seed from a world's seed: its hash, so knowing it tells nothing else of the world. */
	public static long astralSeed(long worldSeed) {
		long h = worldSeed * 0x9E3779B97F4A7C15L + 0x2545F4914F6CDD1DL;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return h;
	}
}
