package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A demon clan's den (the user's design, 2026-10-23): a sect of one demon race (Sect#race) in a wild den of its race's own rather
 * than a sect's halls. It stands on the same kind of level ground a sect does (SectStructure's site checks, its sizes, the
 * grounds levelled and the Formation Core at the plaza in the middle), and keeps the same posts, so its people live as a sect's do;
 * but its buildings are hollow mounds and caves along trails, among trees and undergrowth (DenPieces):
 * <ul>
 *   <li>Tiger, in the forests: boulder mounds, the Clan Head's cave hall at the back, a rock overhang for the disciples, a hoard
 *       cave, a ring of standing stones to spar in, two standing stones for a gate, boulders about the grounds.</li>
 *   <li>Lizard, in the swamps and the dry lands: domes of mud under arching mangrove roots, a basking pool in the head's, a low
 *       burrow and a sunk hoard, a root arch for a gate, pools with reeds about the grounds.</li>
 *   <li>Fox, in the birch and cherry woods and the snow: grassy hillocks with round doors, the head's little shrine on the
 *       greatest of them, a red torii for its gate (its tunnel of torii up the way taken out: the user's call, 2026-10-25, "there
 *       shouldnt be a tunnel, just one gate at the start").</li>
 *   <li>Dragon, in the highlands: one great hollow crag at the back holding all of it (the head's floor, the hoard, the elders'
 *       ledges, the disciples' mats), rock spires and a pool before it.</li>
 * </ul>
 * Which race a den is for, the land decides ({@link #raceOf}): each race's own biomes (tags {@code clan/<race>}); in the Upper Realm
 * any race, a dragon's most often. In the Overworld some three in five as many as sects ({@code structure_set/demon_clans}, 28 chunks
 * apart), kept clear of villages and of wherever a sect may stand ({@link #nearSectSite}). The Upper Realm's sects stand one to a cell
 * that tiles the whole realm (SectStructure#ISLAND_SEARCH), so there a den takes a cell of their own instead ({@link #clanCell}: 45 in
 * a hundred of them), where no sect is looked for.
 * <p>
 * In the Great Void a clan is a Demon Palace (DemonPalaces, structure demon_palace), laid out by DemonPalacePlan on grounds as wide as a
 * Holy Land's, at the sites the Holy Lands' structure set offers both.
 */
public class DenStructure extends SectStructure {
	/**
	 * How near (chunks, either way) a sect's possible start keeps a den off: a little more than two grounds' half-widths. (9 before
	 * 2026-10-24, with the dens' structure set 40 chunks apart: the clans came to a fifth as many as the sects, not half: the user's
	 * report, "demon clans are super rare". Now 7, and 28 apart: near three in five.)
	 */
	private static final int SECT_CLEARANCE = 7;
	/** The Upper Realm's cells (16 chunks, its sects' structure set's spacing) a den takes, of each (a quarter, a third as many as sects, before 2026-10-24). */
	private static final double CLAN_CELLS = 0.45;
	private static final long CELL_SALT = 0x5A7E_C1A2_D0E5L;
	/** A den's members, by size (SMALL, MEDIUM: SectStructure's): its share of the sizes rolled. */
	private static final float SMALL_SHARE = 0.6f;

	public DenStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.DEN_TYPE;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		Size rolled = random.nextFloat() < SMALL_SHARE ? Size.SMALL : Size.MEDIUM;
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		// A Demon Palace of the Great Void (DemonPalaces, DemonPalacePlan): of the race of its land, its grounds a Holy Land's, so they stand
		// at their cell's middle or a few blocks off it where a continent holds them well inside its rim.
		if (isGreatVoid(context, new BlockPos(x, 64, z))) {
			Bloodline race = DemonPalaces.raceOf(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z),
					context.randomState().sampler()), random);
			for (int[] off : HOLY_SPOTS) {
				Frame frame = new Frame(facing, x + off[0], z + off[1], DemonPalacePlan.W, DemonPalacePlan.D);
				int platformY = DemonPalacePlan.groundLevel(context, frame);
				if (platformY != NO_SITE) {
					CityPlanner.Ground ground = ground(context);
					int floor = context.heightAccessor().getMinBuildHeight();
					return Optional.of(new GenerationStub(new BlockPos(frame.cx(), platformY, frame.cz()),
							builder -> DemonPalacePlan.plan(builder, random, frame, platformY, race, ground, floor)));
				}
			}
			return Optional.empty();
		}
		boolean upper = isUpperRealm(context, new BlockPos(x, 64, z));
		if (upper ? !clanCell(context.seed(), chunk) : nearSectSite(context, chunk)) return Optional.empty();
		Bloodline race = raceOf(context, x, z, upper, random);
		if (race == null) return Optional.empty();
		if (!upper) {
			Frame frame = new Frame(facing, x, z, rolled.w, rolled.d);
			int platformY = groundLevel(context, rolled, frame, false);
			return platformY == NO_SITE ? Optional.empty() : Optional.of(stub(context, random, rolled, frame, platformY, false, race));
		}
		for (Size size = rolled; size != null; size = size.smaller()) {
			for (Frame frame : islandSpots(context, size, facing, x, z)) {
				int platformY = groundLevel(context, size, frame, true);
				if (platformY != NO_SITE) return Optional.of(stub(context, random, size, frame, platformY, true, race));
			}
		}
		return Optional.empty();
	}

	private static GenerationStub stub(GenerationContext context, WorldgenRandom random, Size size, Frame frame, int platformY, boolean upper, Bloodline race) {
		CityPlanner.Ground ground = ground(context);
		int floor = context.heightAccessor().getMinBuildHeight();
		return new GenerationStub(new BlockPos(frame.cx(), platformY, frame.cz()),
				builder -> plan(builder, random, size, frame, platformY, upper, race, ground, floor));
	}

	/** The races' lands: {@code #defying-the-heavens:clan/<race>}. */
	private static final Map<Bloodline, TagKey<Biome>> LANDS = new EnumMap<>(Bloodline.class);

	static {
		for (Bloodline race : Bloodline.values()) {
			if (race.isBeast()) LANDS.put(race, TagKey.create(Registries.BIOME, DefyingTheHeavens.id("clan/" + race.getId())));
		}
	}

	/**
	 * The race whose den this land holds: in the Overworld the race whose biomes these are (null: none here); in the Upper Realm, rich
	 * in qi, any race, a dragon's two times in five.
	 */
	static Bloodline raceOf(GenerationContext context, int x, int z, boolean upper, WorldgenRandom random) {
		if (upper) return upperRace(random);
		return raceOfLand(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z),
				context.randomState().sampler()));
	}

	/** The demon race whose land {@code biome} is ({@code clan/<race>}), or null for none's. */
	public static Bloodline raceOfLand(Holder<Biome> biome) {
		for (Map.Entry<Bloodline, TagKey<Biome>> land : LANDS.entrySet()) {
			if (biome.is(land.getValue())) return land.getKey();
		}
		return null;
	}

	/** A demon of the Upper Realm, where every race comes: a dragon two times in five, the others one in five each. */
	public static Bloodline upperRace(net.minecraft.util.RandomSource random) {
		float roll = random.nextFloat();
		return roll < 0.4f ? Bloodline.DRAGON : roll < 0.6f ? Bloodline.TIGER : roll < 0.8f ? Bloodline.FOX : Bloodline.LIZARD;
	}

	/**
	 * Whether the Upper Realm's cell holding {@code chunk} is a den's (its sect is not looked for: SectStructure): a quarter of them,
	 * by the world's seed.
	 */
	public static boolean clanCell(long seed, ChunkPos chunk) {
		return DenPieces.hash(seed ^ CELL_SALT, Math.floorDiv(chunk.x, 16), Math.floorDiv(chunk.z, 16)) < CLAN_CELLS;
	}

	/**
	 * Whether a sect might start within {@link #SECT_CLEARANCE} chunks of {@code chunk} in the Overworld (its structure set's own
	 * spread, asked of every placement cell round it): a den keeps away from where a sect may stand, whether or not the land there
	 * takes one.
	 */
	private static boolean nearSectSite(GenerationContext context, ChunkPos chunk) {
		StructureSet set = context.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(DefyingTheHeavens.id("sects"));
		if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) return false;
		int step = spread.spacing();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				ChunkPos start = spread.getPotentialStructureChunk(context.seed(), chunk.x + dx * step, chunk.z + dz * step);
				if (Math.abs(start.x - chunk.x) <= SECT_CLEARANCE && Math.abs(start.z - chunk.z) <= SECT_CLEARANCE) return true;
			}
		}
		return false;
	}

	/** Lays out a den of {@code race} of size {@code size} (0 small, 1 medium) at the origin as worldgen would, for the game tests. */
	public static List<StructurePiece> layOut(long seed, int size, Direction facing, boolean upper, Bloodline race) {
		return layOut(seed, size, facing, upper, race, 0, 64, 0);
	}

	/** As {@link #layOut(long, int, Direction, boolean, Bloodline)}, its plaza at (x, z) on level ground at {@code y} (its posts too). */
	public static List<StructurePiece> layOut(long seed, int size, Direction facing, boolean upper, Bloodline race, int x, int y, int z) {
		Size s = Size.values()[size];
		StructurePiecesBuilder builder = new StructurePiecesBuilder();
		plan(builder, new WorldgenRandom(new LegacyRandomSource(seed)), s, new Frame(facing, x, z, s.w, s.d), y, upper, race, CityPlanner.flat(y), y - 64);
		return builder.build().pieces();
	}

	private static SectPiece piece(SectPiece.Kind kind, SectLayout.Lot lot, Frame frame, int y, int height, long seed, boolean upper, Bloodline race,
			CompoundTag extra) {
		return new SectPiece(kind, frame.box(lot.u0(), lot.v0(), lot.u1(), lot.v1(), y, height), frame.toWorld(lot.side()), seed, Alignment.Faction.NEUTRAL,
				upper, extra, race);
	}

	private static CompoundTag feature(String what) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Feature", what);
		return tag;
	}

	/**
	 * The den: the trails (up the middle from the gate to the plaza and on to the head's den, and out to either side), the head's den
	 * at the back, the hoard and the disciples' burrow before the plaza on either side of the way, the elders' dens scattered about
	 * the plaza, the sparring ring out on the grounds, the gates, and the race's own things (DenPieces); then the grounds, then the
	 * plaza with the blueprint (posts, alignment, race, capacity) the core founds the clan from.
	 */
	private static void plan(StructurePiecesBuilder builder, WorldgenRandom random, Size size, Frame frame, int y, boolean upper, Bloodline race,
			CityPlanner.Ground ground, int floor) {
		long seed = random.nextLong();
		int alignment = Alignment.randomSect(random); // a clan's path, as a sect's (the user's pick: no feud of the righteous with demons)
		int w = size.w, d = size.d, cu = size.a, cv = size.b, r = SectPiece.PLAZA_RADIUS;
		Direction back = frame.facing();
		int capacity = size.minMembers + random.nextInt(size.maxMembers - size.minMembers + 1);
		SectLayout layout = new SectLayout(size.a, size.b, size.reach, seed);
		// Each den on its own ground, the land shaped round it (the structure is beard_thin), the grounds laid on the land as it lies.
		CityPlanner fit = new CityPlanner(frame, layout, ground, seed, Alignment.Faction.NEUTRAL, null);
		int gateDistance = size.reach + GATE_BEYOND;
		List<SectPiece> pieces = new ArrayList<>();
		boolean dragon = race == Bloodline.DRAGON;

		// The trails: up the middle from the gate to the plaza and on to the head's den, and out to either side.
		int backV0 = cv + r + 2;
		layout.road(cu, 0, cu, cv, 1);
		layout.road(cu, cv, cu, backV0 + 1, 1);
		layout.road(0, cv, w - 1, cv, 1);
		layout.plaza(r);

		// The head's den at the back: the crag for a dragon clan (all of it under one), else its own den.
		int[] head = DenPieces.headSize(race);
		SectLayout.Lot headLot = new SectLayout.Lot(cu - head[0] / 2, backV0, cu - head[0] / 2 + head[0] - 1, backV0 + head[1] - 1, SectLayout.Side.BACK);
		layout.claim(headLot, 2);
		CompoundTag headData = new CompoundTag();
		EnumMap<NpcTitle, Integer> titles = Sect.titleCounts(size.maxMembers);
		int elders = titles.get(NpcTitle.GRAND_ELDER) + titles.get(NpcTitle.ELDER);
		headData.putInt("Elders", elders);
		headData.putInt("Members", size.maxMembers);
		SectPiece headPiece = piece(dragon ? SectPiece.Kind.DEN_CRAG : SectPiece.Kind.DEN_HALL, headLot, frame, fit.level(headLot), head[2], seed + 30, upper, race,
				headData);
		pieces.add(headPiece);

		// The hoard and the disciples' burrow before the plaza, either side of the way (a dragon's are in its crag).
		SectPiece hoard = null, burrow = null;
		int sides = random.nextBoolean() ? 1 : -1;
		if (!dragon) {
			int[] hoardSize = DenPieces.hoardSize(race), burrowSize = DenPieces.burrowSize(race, size.maxMembers);
			SectLayout.Lot hoardLot = beforePlaza(cu, cv, sides, hoardSize[0], hoardSize[1]);
			SectLayout.Lot burrowLot = beforePlaza(cu, cv, -sides, burrowSize[0], burrowSize[1]);
			for (SectLayout.Lot lot : new SectLayout.Lot[] {hoardLot, burrowLot}) {
				layout.claim(lot, 2);
				layout.apron(lot, 1, 1);
				layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
			}
			hoard = piece(SectPiece.Kind.DEN_HOARD, hoardLot, frame, fit.level(hoardLot), hoardSize[2], seed + 20, upper, race, null);
			burrow = piece(SectPiece.Kind.DEN_BURROW, burrowLot, frame, fit.level(burrowLot), burrowSize[2], seed + 21, upper, race, null);
			pieces.add(hoard);
			pieces.add(burrow);
		}

		// The gates: across the trail up the middle and either side way, outside the barrier, where the gate guards stand.
		SectLayout.Lot gateLot = new SectLayout.Lot(cu - 9, cv - gateDistance - 1, cu + 9, cv - gateDistance + 1, SectLayout.Side.BACK);
		layout.keepClear(gateLot.u0(), gateLot.v0() - 1, gateLot.u1(), gateLot.v1() + 1);
		SectPiece gate = piece(SectPiece.Kind.DEN_GATE, gateLot, frame, fit.level(gateLot), 14, seed + 1, upper, race, null);
		pieces.add(gate);
		List<SectPiece> gates = new ArrayList<>(List.of(gate));
		for (int k = -1; k <= 1; k += 2) {
			int u = cu + k * gateDistance;
			SectLayout.Lot lot = new SectLayout.Lot(u - 1, cv - 5, u + 1, cv + 5, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			layout.keepClear(lot.u0() - 1, lot.v0(), lot.u1() + 1, lot.v1());
			SectPiece side = piece(SectPiece.Kind.DEN_GATE, lot, frame, fit.level(lot), 12, seed + 2 + k, upper, race, null);
			pieces.add(side);
			gates.add(side);
		}

		// The sparring ring, out on the grounds.
		SectLayout.Lot ringLot = layout.place(random, DenPieces.RING, DenPieces.RING, size.reach / 2 + 2, 3);
		SectPiece ring = null;
		if (ringLot != null) {
			ring = piece(SectPiece.Kind.DEN_RING, ringLot, frame, fit.level(ringLot), 8, seed + 70, upper, race, null);
			pieces.add(ring);
		}

		// The elders' dens round the plaza, nearest first (a dragon clan's elders keep ledges in its crag).
		List<SectPiece> dens = new ArrayList<>();
		if (!dragon) {
			int[] elderSize = DenPieces.elderSize(race);
			for (int i = 0; i < elders; i++) {
				SectLayout.Lot lot = layout.place(random, elderSize[0], elderSize[1], size.reach / 2 - 4 + random.nextInt(14), 5 + random.nextInt(3));
				if (lot == null) break;
				dens.add(piece(SectPiece.Kind.DEN_ELDER, lot, frame, fit.level(lot), elderSize[2], seed + 100 + i, upper, race, null));
			}
			pieces.addAll(dens);
		}

		// The race's own about the grounds: a tiger's boulders, a lizard's pools, a dragon's spires and the pool before its crag.
		String what = switch (race) {
			case TIGER -> "boulder";
			case LIZARD -> "pool";
			case DRAGON -> "spire";
			default -> null;
		};
		if (what != null) {
			int count = race == Bloodline.LIZARD ? 4 : 5;
			for (int i = 0; i < count; i++) {
				int s = race == Bloodline.LIZARD ? 7 + random.nextInt(3) : 5 + random.nextInt(3);
				SectLayout.Lot lot = layout.place(random, s, s, size.reach / 2 + random.nextInt(size.reach / 2), 2);
				if (lot != null) pieces.add(piece(SectPiece.Kind.DEN_FEATURE, lot, frame, fit.level(lot), race == Bloodline.DRAGON ? 22 : 8, seed + 200 + i, upper, race,
						feature(what)));
			}
		}
		if (dragon) {
			SectLayout.Lot pool = layout.place(random, 9, 7, r + 8, 1);
			if (pool != null) pieces.add(piece(SectPiece.Kind.DEN_FEATURE, pool, frame, fit.level(pool), 6, seed + 250, upper, race, feature("pool")));
		}

		// Lights along the trails, and the land's trees and undergrowth thick between.
		int lights = 8;
		for (int v = cv - gateDistance + 4; v < cv - r; v += lights) {
			layout.lantern(cu - 3, v);
			layout.lantern(cu + 3, v);
		}
		for (int u = cu - gateDistance + 4; u < cu - r; u += lights) {
			layout.lantern(u, cv - 3);
			layout.lantern(w - 1 - u, cv + 3);
		}
		layout.plant(random, size.trees * 3 / 2, size.bamboo * 2);

		// The grounds go first, under everything: laid on the land as it lies, their box at the world's floor (SectPiece#followGround).
		CompoundTag groundsData = new CompoundTag();
		groundsData.putInt("A", size.a);
		groundsData.putInt("B", size.b);
		groundsData.putByteArray("Map", layout.map);
		groundsData.putBoolean("Follow", true);
		SectPiece terrace = new SectPiece(SectPiece.Kind.TERRACE, frame.box(0, 0, w - 1, d - 1, floor, 2), back, seed, Alignment.Faction.NEUTRAL, upper,
				groundsData, race);
		pieces.add(0, terrace);

		// The plaza last, so its blueprint can name every post of the grounds: on its own ground, evened under it.
		BoundingBox plazaBox = frame.box(cu - r, cv - r, cu + r, cv + r, fit.level(new SectLayout.Lot(cu - r, cv - r, cu + r, cv + r, SectLayout.Side.BACK)), 8);
		CompoundTag plazaData = new CompoundTag();
		SectPiece plaza = new SectPiece(SectPiece.Kind.PLAZA, plazaBox, back, seed + 50, Alignment.Faction.NEUTRAL, upper, plazaData, race);
		BlockPos core = plaza.toWorld(r, 2, r);

		Map<Sect.Post, List<BlockPos>> posts = new EnumMap<>(Sect.Post.class);
		for (Sect.Post post : Sect.Post.values()) posts.put(post, new ArrayList<>());
		int[] seat = DenPieces.seat(headPiece);
		posts.get(Sect.Post.MASTER_SEAT).add(headPiece.toWorld(seat[0], seat[1], seat[2]));
		SectPiece mats = dragon ? headPiece : burrow, vault = dragon ? headPiece : hoard;
		for (int[] mat : DenPieces.mats(mats)) posts.get(Sect.Post.MEDITATION).add(mats.toWorld(mat[0], mat[1], mat[2]));
		for (int[] guard : DenPieces.hoardGuards(vault)) posts.get(Sect.Post.TREASURY).add(vault.toWorld(guard[0], guard[1], guard[2]));
		for (int[] guard : SectPiece.corePosts()) posts.get(Sect.Post.CORE).add(plaza.toWorld(guard[0], 1, guard[1]));
		// Homes: the elders' dens nearest the plaza first (a dragon clan's ledges), then the disciples' mats.
		if (dragon) {
			for (int[] ledge : DenPieces.ledges(headPiece)) posts.get(Sect.Post.DWELLING).add(headPiece.toWorld(ledge[0], ledge[1], ledge[2]));
		} else {
			dens.sort((p, q) -> Double.compare(p.getBoundingBox().getCenter().distSqr(core), q.getBoundingBox().getCenter().distSqr(core)));
			for (SectPiece den : dens) {
				int[] mat = DenPieces.home(den);
				posts.get(Sect.Post.DWELLING).add(den.toWorld(mat[0], mat[1], mat[2]));
			}
		}
		for (int[] mat : DenPieces.mats(mats)) posts.get(Sect.Post.DWELLING).add(mats.toWorld(mat[0], mat[1], mat[2]));
		// Where the inner disciples stroll: along the trails, round the plaza.
		List<BlockPos> courtyard = posts.get(Sect.Post.COURTYARD);
		int along = (r + size.reach) / 2;
		courtyard.add(fit.onGround(cu, cv - along));
		courtyard.add(fit.onGround(cu, cv - size.reach + 4));
		courtyard.add(fit.onGround(cu - along, cv));
		courtyard.add(fit.onGround(cu + along, cv));
		for (int k = 0; k < 4; k++) {
			double angle = Math.PI / 2 * k;
			courtyard.add(plaza.toWorld(r + (int) Math.round(Math.cos(angle) * 10.5), 1, r + (int) Math.round(Math.sin(angle) * 10.5)));
		}
		int[] in = DenPieces.gateInside(gate);
		posts.get(Sect.Post.GATE).add(gate.toWorld(in[0], in[1], in[2]));
		if (ring != null) {
			for (int[] post : DenPieces.ringPosts(ring)) posts.get(Sect.Post.TRAINING).add(ring.toWorld(post[0], post[1], post[2]));
		}
		List<BlockPos> ways = posts.get(Sect.Post.WAY);
		for (int v = cv - gateDistance + 2; v <= backV0; v += 10) if (Math.abs(v - cv) > r) ways.add(fit.onGround(cu, v));
		for (int u = cu - gateDistance + 2; u <= cu + gateDistance - 2; u += 10) if (Math.abs(u - cu) > r) ways.add(fit.onGround(u, cv));
		List<BlockPos> guards = posts.get(Sect.Post.GATE_GUARD);
		int out = gateDistance + 2;
		guards.add(fit.onGround(cu - 2, cv - out));
		guards.add(fit.onGround(cu + 2, cv - out));
		guards.add(fit.onGround(cu - out, cv - 2));
		guards.add(fit.onGround(cu + out, cv - 2));
		guards.add(fit.onGround(cu - out, cv + 2));
		guards.add(fit.onGround(cu + out, cv + 2));

		// The barrier: from the core out round every den (not the grounds' own things: the gates stand outside it).
		int radius = 0;
		for (SectPiece piece : pieces) {
			if (piece == terrace || gates.contains(piece) || piece.kind() == SectPiece.Kind.DEN_FEATURE) continue;
			BoundingBox box = piece.getBoundingBox();
			for (int[] corner : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
				double dx = corner[0] - core.getX(), dz = corner[1] - core.getZ();
				radius = Math.max(radius, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)) + 2);
			}
		}
		radius = Math.min(Formations.MAX_RADIUS, radius);

		CompoundTag blueprint = new CompoundTag();
		blueprint.putLong("Seed", seed);
		blueprint.putInt("Alignment", alignment);
		blueprint.putString("Race", race.getId());
		blueprint.putInt("Capacity", Math.min(capacity, posts.get(Sect.Post.DWELLING).size() + 1));
		blueprint.putInt("Radius", radius);
		blueprint.putIntArray("Bounds", SectStructure.bounds(terrace, pieces, plaza, 48));
		BoundingBox treasury = DenPieces.hoardBox(vault);
		blueprint.putIntArray("Treasury", new int[] {treasury.minX(), treasury.minY(), treasury.minZ(), treasury.maxX(), treasury.maxY(), treasury.maxZ()});
		CompoundTag postTag = new CompoundTag();
		for (Map.Entry<Sect.Post, List<BlockPos>> entry : posts.entrySet()) {
			postTag.put(entry.getKey().name(), new LongArrayTag(entry.getValue().stream().mapToLong(BlockPos::asLong).toArray()));
		}
		blueprint.put("Posts", postTag);
		plazaData.put("Blueprint", blueprint);
		pieces.add(plaza);

		for (SectPiece piece : pieces) builder.addPiece(piece);
	}

	/** A den {@code width} across and {@code depth} deep before the plaza on {@code side} of the way, its door facing the way. */
	private static SectLayout.Lot beforePlaza(int cu, int cv, int side, int width, int depth) {
		int v1 = cv - SectPiece.PLAZA_RADIUS - 3 - SET_BACK, v0 = v1 - width + 1;
		int near = 4 + SET_BACK;
		return side > 0 ? new SectLayout.Lot(cu + near, v0, cu + near - 1 + depth, v1, SectLayout.Side.RIGHT)
				: new SectLayout.Lot(cu - near + 1 - depth, v0, cu - near, v1, SectLayout.Side.LEFT);
	}
}
