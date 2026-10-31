package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Verdant Herb Valley's works (SecretRealm#VERDANT_HERB_VALLEY; its land is HerbValley's, its cycle SecretRealms', the machinery every
 * realm shares RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>, the first time it stirs, a step at a time ({@link #layOut}): the arrival terraces at its paths' heads; the bridge
 *   over the lake; on the island the Pill Sage's hall (its doorway shut by a trial's barrier, its master's cauldron within on his own fire,
 *   his bones at its back); the ring where the heart blooms; the alchemists' sect's ruins about the lake ({@link Ruin}: the Pill Hall, the
 *   Library, the Furnace Court, the Disciples' Quarters, the Stele Court, the Medicine Garden); trees.</li>
 *   <li><b>Renewed each time it stirs</b>: its land mended (RealmLedger); its herbs (an unpicked one older by a century for every year the
 *   realm slept, {@link #YEARS_A_YEAR_SHUT}; one picked grows again, of its bed's age); its vaults sealed anew for the ancients and filled
 *   ({@link #vaultTreasures}); its killing formations' eyes set again; the master's cauldron's water and a jade on his altar; the bones of a
 *   few earlier visitors, with what they carried.</li>
 *   <li><b>Its guardians</b> ({@link Slot}: a Vine Keeper by each bed, Bronze Puppets in the ruins, the Gatekeeper before the hall, the
 *   Elder Keeper by the blooming heart), raised as someone first comes near each in an opening, of the realm's own ranks; one slain stays
 *   slain till the next.</li>
 *   <li><b>Its event</b>: some time into each opening the heart blooms ({@link #bloom}): a ten-thousand-year Spirit Ginseng flowers in the
 *   ring on the island for a minute, its Elder Keeper waking; untaken, it withers.</li>
 *   <li><b>Its trials and inheritance</b>: the Gatekeeper ({@link RealmGuardian#challenge}) passed, its barrier lets one in; in the master's
 *   cauldron, a Foundation Pill of Mid-Grade or better ({@link #brewed}): the Great Inheritance, the Pill Sage's Cauldron and his Notes, once
 *   in a world; after it is taken, a lesser legacy to the first in each opening ({@link #inherit}).</li>
 *   <li><b>Its close</b>: the last two minutes it quakes ({@link #quake}): rumbling, dust, the ground splitting here and there.</li>
 * </ul>
 */
public final class HerbValleyWorks extends RealmWorks {
	/** The valley's works (SecretRealm#works). */
	static final HerbValleyWorks WORKS = new HerbValleyWorks();
	/** An unpicked herb ages this many years more for each year (in-game day) its realm slept. */
	public static final int YEARS_A_YEAR_SHUT = 100;
	/** The heart blooms this long (ticks: a minute). */
	public static final int BLOOM_TICKS = 1200;

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		HerbValley.Shape s = HerbValley.shape(seed);
		RandomSource random = RandomSource.create(seed ^ 0x1A1DL);
		steps.add(() -> arrivalTerraces(level, s, c));
		steps.add(() -> bridge(level, s, c));
		steps.add(() -> hall(level, s, c));
		steps.add(() -> bloomRing(level, s, c));
		for (Ruin ruin : Ruin.values()) steps.add(() -> ruin(level, s, c, ruin, RandomSource.create(seed ^ ruin.ordinal() * 0x2B1EL)));
		for (int part = 0; part < 4; part++) {
			int p = part;
			steps.add(() -> trees(level, s, c, p, random));
		}
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		HerbValley.Shape s = HerbValley.shape(seed);
		steps.add(() -> renewHerbs(level, s, c, r));
		steps.add(() -> renewVaults(level, s, c, r));
		steps.add(() -> renewFormations(level, s, c));
		steps.add(() -> renewHall(level, s, c));
	}

	// --- Building ---

	/** The ground's top at world column (x, z) of the valley (from its land: no chunk needed). */
	static int groundAt(HerbValley.Shape s, BlockPos c, int x, int z) {
		return HerbValley.column(s, x - c.getX(), z - c.getZ()).ground();
	}

	/** A level floor at {@code y} over the rectangle (world), the land under it filled up to it and what stands over it cleared. */
	static void flatten(ServerLevel level, HerbValley.Shape s, BlockPos c, BlockPos a, BlockPos b, int y, BlockState top, int clear) {
		for (int x = Math.min(a.getX(), b.getX()); x <= Math.max(a.getX(), b.getX()); x++) {
			for (int z = Math.min(a.getZ(), b.getZ()); z <= Math.max(a.getZ(), b.getZ()); z++) {
				int g = groundAt(s, c, x, z);
				for (int dy = Math.min(g, y) - 2; dy < y; dy++) put(level, new BlockPos(x, dy, z), Blocks.DIRT.defaultBlockState());
				put(level, new BlockPos(x, y, z), top);
				for (int dy = y + 1; dy <= Math.max(g, y) + clear; dy++) put(level, new BlockPos(x, dy, z), Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** The main way's bearing as a direction (outward from the heart, along x or z). */
	static Direction mainWay(HerbValley.Shape s) {
		double b = s.pathTurn;
		int dx = (int) Math.round(Math.cos(b)), dz = (int) Math.round(Math.sin(b));
		return Direction.fromDelta(dx, 0, dz);
	}

	// --- The arrival terraces ---

	/** At each path's head: a round floor of old stone, a lantern on a post either side, a worn stele at its back. */
	static void arrivalTerraces(ServerLevel level, HerbValley.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(level.getSeed() ^ 0xA77L);
		for (BlockPos local : HerbValley.arrivals(s)) {
			BlockPos at = c.offset(local.getX(), 0, local.getZ()).atY(local.getY());
			int y = at.getY();
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz > 11) continue;
					BlockPos floor = at.offset(dx, 0, dz);
					for (int dy = -3; dy < 0; dy++) put(level, floor.above(dy), Blocks.STONE.defaultBlockState());
					put(level, floor, dx == 0 && dz == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : oldStone(random));
					for (int dy = 1; dy <= 5; dy++) put(level, floor.above(dy), Blocks.AIR.defaultBlockState());
				}
			}
			// Lantern posts across the way, a stele behind (outward).
			Vec3 out = new Vec3(local.getX(), 0, local.getZ()).normalize();
			Vec3 side = new Vec3(-out.z, 0, out.x);
			for (int k = -1; k <= 1; k += 2) {
				BlockPos post = at.offset((int) Math.round(side.x * 3 * k), 0, (int) Math.round(side.z * 3 * k));
				put(level, post.above(), Blocks.STONE_BRICK_WALL.defaultBlockState());
				put(level, post.above(2), Blocks.STONE_BRICK_WALL.defaultBlockState());
				put(level, post.above(3), lantern(false));
			}
			BlockPos stele = at.offset((int) Math.round(out.x * 3), 0, (int) Math.round(out.z * 3));
			put(level, stele.above(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			put(level, stele.above(2), Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
			join(level, at.offset(-4, 0, -4), at.offset(4, 4, 4));
		}
	}

	// --- The bridge ---

	/** The bridge: three wide and level with the island's top, from the island's edge over the lake to the far shore, stepping down to it. */
	static void bridge(ServerLevel level, HerbValley.Shape s, BlockPos c) {
		Direction way = mainWay(s);
		Frame frame = new Frame(c.atY(HerbValley.ISLAND_Y), way);
		RandomSource random = RandomSource.create(level.getSeed() ^ 0xB71DL);
		int deck = HerbValley.ISLAND_Y;
		int end = HerbValley.LAKE_R + 2;
		for (int f = HerbValley.ISLAND_FLAT; f <= end; f++) {
			for (int sd = -2; sd <= 2; sd++) {
				BlockPos at = frame.at(f, sd, 0).atY(deck);
				if (Math.abs(sd) <= 1) {
					put(level, at, oldStone(random));
					for (int dy = 1; dy <= 4; dy++) put(level, at.above(dy), Blocks.AIR.defaultBlockState());
				} else {
					// Its railings, a lantern on a post every few blocks.
					put(level, at, Blocks.STONE_BRICKS.defaultBlockState());
					boolean post = (f - HerbValley.ISLAND_FLAT) % 6 == 0;
					put(level, at.above(), post ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICK_WALL.defaultBlockState());
					if (post) put(level, at.above(2), lantern(false));
				}
			}
			// Its piers, down to the lake's bed, every five.
			if ((f - HerbValley.ISLAND_FLAT) % 5 == 2) {
				for (int sd = -2; sd <= 2; sd += 4) {
					BlockPos top = frame.at(f, sd, 0).atY(deck - 1);
					for (int y = top.getY(); y >= HerbValley.WATER_Y - 8; y--) {
						BlockPos p = top.atY(y);
						if (level.getBlockState(p).isSolid() && y < HerbValley.WATER_Y - 1) break;
						put(level, p, Blocks.STONE_BRICKS.defaultBlockState());
					}
				}
			}
		}
		// Down to the far shore: a stair a block, as far as the ground there lies below the deck.
		int f = end + 1, y = deck;
		while (f < end + 10) {
			BlockPos mid = frame.at(f, 0, 0);
			int g = groundAt(s, c, mid.getX(), mid.getZ());
			if (y - 1 <= g) break;
			y--;
			for (int sd = -1; sd <= 1; sd++) {
				BlockPos at = frame.at(f, sd, 0).atY(y);
				for (int dy = y - 1; dy > g - 2; dy--) put(level, at.atY(dy), Blocks.STONE_BRICKS.defaultBlockState());
				put(level, at, stairs(Blocks.STONE_BRICK_STAIRS, way.getOpposite(), false));
			}
			f++;
		}
		join(level, frame.at(HerbValley.ISLAND_FLAT, -3, -2), frame.at(end + 10, 3, 4));
	}

	// --- The Pill Sage's hall ---

	/** The hall's frame: on the island's middle, facing the bridge (its front toward the main way). */
	static Frame hallFrame(HerbValley.Shape s, BlockPos c) {
		return new Frame(c.atY(HerbValley.ISLAND_Y), mainWay(s));
	}

	/** Where the master's cauldron stands; where the jade lies on his altar; where the Gatekeeper keeps its post. */
	static BlockPos trialCauldron(HerbValley.Shape s, BlockPos c) { return hallFrame(s, c).at(-2, 0, 3); }

	static BlockPos altar(HerbValley.Shape s, BlockPos c) { return hallFrame(s, c).at(-4, 2, 2); }

	static BlockPos gatePost(HerbValley.Shape s, BlockPos c) { return hallFrame(s, c).at(5, -3, 2); }

	/** The hall's doorway (its barrier): three wide, three high, in its front wall. */
	static List<BlockPos> doorway(HerbValley.Shape s, BlockPos c) {
		Frame h = hallFrame(s, c);
		List<BlockPos> door = new ArrayList<>();
		for (int sd = -1; sd <= 1; sd++) for (int y = 2; y <= 4; y++) door.add(h.at(2, sd, y));
		return door;
	}

	/**
	 * The Pill Sage's hall: a platform of white stone a step over the island, pillars of red wood, walls of white plaster over a stone base,
	 * lattice windows, a hipped roof of dark tiles with a gilded ridge; a terrace before it with steps down to the bridge. Within: the master's
	 * cauldron on its fire, his altar, shelves of his books, lanterns hung, and his bones in the corner, facing the cauldron. Its doorway the
	 * trial's barrier.
	 */
	static void hall(ServerLevel level, HerbValley.Shape s, BlockPos c) {
		Frame h = hallFrame(s, c);
		Direction way = h.forward();
		RandomSource random = RandomSource.create(level.getSeed() ^ 0x5A6EL);
		// The platform: a step over the island (y 1), its edges chiseled; the terrace before the hall and its steps down.
		for (int f = -7; f <= 6; f++) {
			for (int sd = -7; sd <= 7; sd++) {
				BlockPos at = h.at(f, sd, 1);
				put(level, at.below(), Blocks.STONE.defaultBlockState());
				boolean edge = f == -7 || f == 6 || Math.abs(sd) == 7;
				put(level, at, edge ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
				for (int y = 2; y <= 12; y++) put(level, h.at(f, sd, y), Blocks.AIR.defaultBlockState());
			}
		}
		for (int sd = -2; sd <= 2; sd++) put(level, h.at(7, sd, 1), stairs(Blocks.STONE_BRICK_STAIRS, way.getOpposite(), false));
		// The walls: f from -6 to 2, s from -5 to 5; pillars of red wood at the corners and every few blocks; a stone base, plaster over it.
		BlockState pillar = log(Blocks.STRIPPED_MANGROVE_LOG), plaster = Blocks.WHITE_TERRACOTTA.defaultBlockState(), base = Blocks.STONE_BRICKS.defaultBlockState();
		for (int f = -6; f <= 2; f++) {
			for (int sd = -5; sd <= 5; sd++) {
				boolean wall = f == -6 || f == 2 || Math.abs(sd) == 5;
				if (!wall) continue;
				boolean post = (f == -6 || f == 2) && (Math.abs(sd) == 5 || Math.abs(sd) == 2) || Math.abs(sd) == 5 && (f == -2);
				for (int y = 2; y <= 6; y++) {
					BlockState block = post ? pillar : y == 2 ? base : plaster;
					// Lattice windows in the side walls and the back: a mangrove trapdoor stood open in the opening (its lattice), flush with
					// the wall's outer face (an open one's panel lies on the side away from its facing).
					if (!post && y == 4 && (Math.abs(sd) == 5 && (f == -4 || f == 0) || f == -6 && Math.abs(sd) == 3)) {
						Direction inward = f == -6 ? way : sd > 0 ? h.right().getOpposite() : h.right();
						block = Blocks.MANGROVE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.FACING, inward);
					}
					put(level, h.at(f, sd, y), block);
				}
			}
		}
		// The beam over the walls, and the floor inside.
		for (int f = -6; f <= 2; f++) for (int sd = -5; sd <= 5; sd++) {
			if (f == -6 || f == 2 || Math.abs(sd) == 5) put(level, h.at(f, sd, 7), log(Blocks.STRIPPED_DARK_OAK_LOG));
			else put(level, h.at(f, sd, 1), (f + sd) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
		}
		hipRoof(level, h, -7, 3, -6, 6, 8);
		// The doorway, shut by the trial's barrier.
		for (BlockPos p : doorway(s, c)) put(level, p, ModBlocks.TRIAL_BARRIER.defaultBlockState());
		// Lanterns either side of the door, on the terrace's posts.
		for (int sd = -4; sd <= 4; sd += 8) {
			put(level, h.at(4, sd, 2), Blocks.STONE_BRICK_WALL.defaultBlockState());
			put(level, h.at(4, sd, 3), lantern(false));
		}
		// Within: the master's cauldron on its fire, his altar at the back, his shelves, his bones, lanterns hung from the beams.
		BlockPos cauldron = trialCauldron(s, c);
		put(level, cauldron.below(), Blocks.CAMPFIRE.defaultBlockState());
		put(level, cauldron, ModBlocks.ALCHEMY_CAULDRON.defaultBlockState().setValue(AlchemyCauldronBlock.FILLED, true));
		BlockPos altar = altar(s, c);
		put(level, altar.below(), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
		for (int sd = -4; sd <= 4; sd++) {
			if (Math.abs(sd) <= 1) continue;
			put(level, h.at(-5, sd, 2), Blocks.BOOKSHELF.defaultBlockState());
			if (random.nextBoolean()) put(level, h.at(-5, sd, 3), Blocks.BOOKSHELF.defaultBlockState());
		}
		for (int[] l : new int[][] {{-3, -3}, {-3, 3}, {0, -3}, {0, 3}}) put(level, h.at(l[0], l[1], 7), lantern(true)); // hung from the roof's tiles
		BlockPos bones = h.at(-4, -3, 2);
		put(level, bones, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.STAGE, CorpseBlock.Rot.BONES));
		if (level.getBlockEntity(bones) instanceof CorpseBlockEntity body) {
			body.ancient(sageName(level), 0, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION), way.getOpposite().toYRot(), level.getGameTime(), List.of());
		}
		join(level, h.at(-8, -8, 0), h.at(8, 8, 12));
	}

	/** The Pill Sage's own name (his bones', in his hall): the world's own, the same every time. */
	public static String sageName(ServerLevel level) {
		return NpcNames.of(new UUID(level.getSeed(), 0x5A6E5A6EL));
	}

	/**
	 * A hipped roof over the rectangle f in [{@code f0}, {@code f1}], s in [{@code s0}, {@code s1}] (one past the walls: its eaves), from
	 * {@code y}: each course a step in, its rim stairs facing out, until it is a ridge; the ridge gilded at its ends.
	 */
	static void hipRoof(ServerLevel level, Frame h, int f0, int f1, int s0, int s1, int y) {
		hipRoof(level, h, f0, f1, s0, s1, y, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES);
	}

	/** The same in other tiles: {@code tile}'s stairs at the rims, {@code full} within, {@code ridge}'s slabs along the top. */
	static void hipRoof(ServerLevel level, Frame h, int f0, int f1, int s0, int s1, int y, Block tile, Block ridge, Block full) {
		for (int k = 0; ; k++) {
			int a0 = f0 + k, a1 = f1 - k, b0 = s0 + k, b1 = s1 - k;
			if (a0 > a1 || b0 > b1) break;
			if (a1 - a0 <= 1 || b1 - b0 <= 1) {
				// The ridge: slabs along it, gold at its ends.
				for (int f = a0; f <= a1; f++) for (int sd = b0; sd <= b1; sd++) {
					boolean end = a1 - a0 > 1 ? f == a0 || f == a1 : sd == b0 || sd == b1;
					put(level, h.at(f, sd, y + k), end ? Blocks.GOLD_BLOCK.defaultBlockState() : slab(ridge, false));
				}
				break;
			}
			for (int f = a0; f <= a1; f++) {
				for (int sd = b0; sd <= b1; sd++) {
					boolean rim = f == a0 || f == a1 || sd == b0 || sd == b1;
					if (!rim) {
						put(level, h.at(f, sd, y + k), full.defaultBlockState());
						continue;
					}
					// Facing out: the stair's low side away from the middle.
					Direction out = f == a0 ? h.forward().getOpposite() : f == a1 ? h.forward() : sd == b0 ? h.right().getOpposite() : h.right();
					put(level, h.at(f, sd, y + k), stairs(tile, out.getOpposite(), false));
				}
			}
		}
	}

	// --- The bloom ---

	/** Where the heart blooms: on the island beside the hall. */
	static BlockPos bloomSpot(HerbValley.Shape s, BlockPos c) {
		return hallFrame(s, c).at(-1, 9, 2);
	}

	/** Its ring: soil in the middle, a ring of mossy stone and flowers round it. */
	static void bloomRing(ServerLevel level, HerbValley.Shape s, BlockPos c) {
		BlockPos spot = bloomSpot(s, c);
		RandomSource random = RandomSource.create(level.getSeed() ^ 0xB100L);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > 6) continue;
				BlockPos ground = spot.offset(dx, -1, dz);
				put(level, ground, d2 == 0 ? Blocks.PODZOL.defaultBlockState() : d2 <= 2 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState());
				BlockState flower = d2 >= 4 && random.nextFloat() < 0.6f ? (random.nextBoolean() ? Blocks.LILY_OF_THE_VALLEY : Blocks.AZURE_BLUET).defaultBlockState() : Blocks.AIR.defaultBlockState();
				if (d2 > 0) put(level, ground.above(), flower);
			}
		}
	}

	// --- The ruins ---

	/** The alchemists' sect's ruins about the lake. */
	enum Ruin {
		PILL_HALL(StorageFormations.Tier.MID, 2), LIBRARY(StorageFormations.Tier.LOW, 1), FURNACE_COURT(StorageFormations.Tier.LOW, 0),
		QUARTERS(StorageFormations.Tier.LOW, 1), STELE_COURT(StorageFormations.Tier.LOW, 1), GARDEN(null, 0);

		final StorageFormations.Tier vault;
		final int puppets;

		Ruin(StorageFormations.Tier vault, int puppets) {
			this.vault = vault;
			this.puppets = puppets;
		}
	}

	/** Out from the heart, this far, its ruins stand; their half width. */
	static final int RUIN_R = 52, RUIN_HALF = 7;

	/**
	 * A ruin's frame: its bearing, in an eighth of the valley between two paths: the five eighths without a brook by turns, the garden beside
	 * one; facing the heart.
	 */
	static Frame ruinFrame(HerbValley.Shape s, BlockPos c, Ruin ruin) {
		List<Integer> dry = new ArrayList<>(), wet = new ArrayList<>();
		for (int e = 0; e < HerbValley.PATHS; e++) {
			double mid = s.pathTurn + (e + 0.5) * Math.PI * 2 / HerbValley.PATHS;
			(s.nearStream(mid, RUIN_R, RUIN_HALF + 4) ? wet : dry).add(e);
		}
		double bearing;
		if (ruin == Ruin.GARDEN) {
			int e = wet.isEmpty() ? 0 : wet.get(0);
			bearing = s.pathTurn + (e + 0.5) * Math.PI * 2 / HerbValley.PATHS + 0.24; // beside the brook
		} else {
			int e = dry.get(Math.min(ruin.ordinal(), dry.size() - 1) % Math.max(1, dry.size()));
			bearing = s.pathTurn + (e + 0.5) * Math.PI * 2 / HerbValley.PATHS;
		}
		int x = (int) Math.round(Math.cos(bearing) * RUIN_R), z = (int) Math.round(Math.sin(bearing) * RUIN_R);
		// Facing the heart: along whichever of x and z points that way more.
		Direction facing = Math.abs(x) > Math.abs(z) ? (x > 0 ? Direction.WEST : Direction.EAST) : (z > 0 ? Direction.NORTH : Direction.SOUTH);
		BlockPos origin = c.offset(x, 0, z);
		return new Frame(origin.atY(ruinFloor(s, c, origin)), facing);
	}

	/** A ruin's floor: the middle of the land's heights under it. */
	static int ruinFloor(HerbValley.Shape s, BlockPos c, BlockPos origin) {
		List<Integer> heights = new ArrayList<>();
		for (int dx = -RUIN_HALF; dx <= RUIN_HALF; dx += 3) for (int dz = -RUIN_HALF; dz <= RUIN_HALF; dz += 3) heights.add(groundAt(s, c, origin.getX() + dx, origin.getZ() + dz));
		java.util.Collections.sort(heights);
		return heights.get(heights.size() / 2);
	}

	/** Where a ruin's vault stands (its back, the middle). */
	static BlockPos vaultAt(HerbValley.Shape s, BlockPos c, Ruin ruin) {
		Frame r = ruinFrame(s, c, ruin);
		return r.at(-RUIN_HALF + 2, ruin == Ruin.QUARTERS ? -4 : 0, 1);
	}

	/** Where a ruin's puppets keep their posts. */
	static List<BlockPos> puppetPosts(HerbValley.Shape s, BlockPos c, Ruin ruin) {
		Frame r = ruinFrame(s, c, ruin);
		List<BlockPos> posts = new ArrayList<>();
		if (ruin.puppets >= 1) posts.add(r.at(-RUIN_HALF + 4, -3, 1));
		if (ruin.puppets >= 2) posts.add(r.at(-RUIN_HALF + 4, 3, 1));
		return posts;
	}

	/** Where the garden's herbs grow (on its beds' soil, the herb over it). */
	static List<BlockPos> gardenSpots(HerbValley.Shape s, BlockPos c) {
		Frame r = ruinFrame(s, c, Ruin.GARDEN);
		List<BlockPos> spots = new ArrayList<>();
		for (int f = -3; f <= 3; f += 3) for (int sd = -3; sd <= 3; sd += 6) spots.add(r.at(f, sd, 1));
		return spots;
	}

	/**
	 * A ruin: its floor levelled and paved in old stone (or the garden's earth), walls broken off at every height round it, a gap toward the
	 * heart; within, what it was: the Pill Hall's pillars and its great vault; the Library's shelves; the Furnace Court's cauldrons and burnt
	 * floor; the Quarters' three huts; the Stele Court's fallen stele and lanterns; the Garden's raised beds, its pavilion and low wall. Rubble
	 * and moss over all.
	 */
	static void ruin(ServerLevel level, HerbValley.Shape s, BlockPos c, Ruin ruin, RandomSource random) {
		Frame r = ruinFrame(s, c, ruin);
		int floor = r.origin().getY();
		boolean garden = ruin == Ruin.GARDEN;
		flatten(level, s, c, r.at(-RUIN_HALF, -RUIN_HALF, 0).atY(floor), r.at(RUIN_HALF, RUIN_HALF, 0).atY(floor),
				floor, Blocks.GRASS_BLOCK.defaultBlockState(), 10);
		for (int f = -RUIN_HALF; f <= RUIN_HALF; f++) {
			for (int sd = -RUIN_HALF; sd <= RUIN_HALF; sd++) {
				BlockPos at = r.at(f, sd, 0);
				if (!garden && Math.abs(f) < RUIN_HALF && Math.abs(sd) < RUIN_HALF) {
					BlockState pave = ruin == Ruin.FURNACE_COURT && random.nextFloat() < 0.35f ? Blocks.BLACKSTONE.defaultBlockState()
							: random.nextFloat() < 0.12f ? Blocks.MOSS_BLOCK.defaultBlockState() : random.nextFloat() < 0.1f ? Blocks.COBBLESTONE.defaultBlockState() : oldStone(random);
					put(level, at, pave);
				}
				// Its walls round it, broken off at all heights; a gap toward the heart.
				boolean wall = Math.abs(f) == RUIN_HALF || Math.abs(sd) == RUIN_HALF;
				if (wall && !(f == RUIN_HALF && Math.abs(sd) <= 2)) {
					int height = garden ? 1 : random.nextInt(5);
					for (int y = 1; y <= height; y++) put(level, at.above(y), garden ? Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState() : oldStone(random));
					if (height > 0 && random.nextFloat() < 0.3f && !garden) put(level, at.above(height + 1), slab(Blocks.STONE_BRICK_SLAB, false));
				}
			}
		}
		switch (ruin) {
			case PILL_HALL -> {
				// Pillars of red wood, some standing to their height, some broken; one fallen across the floor.
				for (int f = -4; f <= 4; f += 4) {
					for (int sd = -4; sd <= 4; sd += 8) {
						int height = 2 + random.nextInt(4);
						for (int y = 1; y <= height; y++) put(level, r.at(f, sd, y), log(Blocks.STRIPPED_MANGROVE_LOG));
					}
				}
				for (int k = 0; k < 4; k++) put(level, r.at(1, -1 + k, 1), Blocks.STRIPPED_MANGROVE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, r.right().getAxis()));
				put(level, r.at(-RUIN_HALF + 1, 0, 1), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			}
			case LIBRARY -> {
				for (int f = -4; f <= 3; f += 2) {
					for (int sd = -5; sd <= 5; sd++) {
						if (Math.abs(sd) <= 1) continue;
						int height = random.nextFloat() < 0.4f ? 2 : 1;
						for (int y = 1; y <= height; y++) if (random.nextFloat() < 0.8f) put(level, r.at(f, sd, y), Blocks.BOOKSHELF.defaultBlockState());
					}
				}
			}
			case FURNACE_COURT -> {
				for (int[] at : new int[][] {{-3, -4}, {-3, 4}, {3, -4}, {3, 4}}) {
					put(level, r.at(at[0], at[1], 1), Blocks.CAULDRON.defaultBlockState());
					put(level, r.at(at[0], at[1], 0), Blocks.MAGMA_BLOCK.defaultBlockState());
				}
			}
			case QUARTERS -> {
				for (int k = -1; k <= 1; k++) {
					int cs = k * 5;
					for (int f = -3; f <= 1; f++) {
						for (int sd = cs - 2; sd <= cs + 2; sd++) {
							boolean edge = f == -3 || f == 1 || Math.abs(sd - cs) == 2;
							if (!edge || f == 1 && sd == cs) continue;
							int height = 1 + random.nextInt(3);
							for (int y = 1; y <= height; y++) put(level, r.at(f, sd, y), random.nextFloat() < 0.5f ? Blocks.SPRUCE_PLANKS.defaultBlockState() : oldStone(random));
						}
					}
				}
			}
			case STELE_COURT -> {
				// The stele, fallen along the court; lanterns of stone at its corners.
				for (int k = -2; k <= 2; k++) put(level, r.at(-1, k, 1), k == 0 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
				for (int[] at : new int[][] {{-5, -5}, {-5, 5}, {5, -5}, {5, 5}}) {
					put(level, r.at(at[0], at[1], 1), Blocks.STONE_BRICK_WALL.defaultBlockState());
					put(level, r.at(at[0], at[1], 2), lantern(false));
				}
			}
			case GARDEN -> {
				// Raised beds of dark soil, a pavilion of four posts under a roof of tiles.
				for (BlockPos spot : gardenSpots(s, c)) {
					BlockPos soil = spot.below();
					for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
						put(level, soil.offset(dx, 0, dz), Blocks.PODZOL.defaultBlockState());
						if (dx != 0 || dz != 0) put(level, soil.offset(dx, 1, dz), random.nextFloat() < 0.3f ? Blocks.FERN.defaultBlockState() : Blocks.AIR.defaultBlockState());
					}
				}
				for (int f = 4; f <= 6; f += 2) for (int sd = -1; sd <= 1; sd += 2) for (int y = 1; y <= 3; y++) put(level, r.at(f, sd, y), log(Blocks.STRIPPED_MANGROVE_LOG));
				for (int f = 3; f <= 7; f++) for (int sd = -2; sd <= 2; sd++) put(level, r.at(f, sd, 4), Math.abs(sd) == 2 || f == 3 || f == 7
						? slab(Blocks.DEEPSLATE_TILE_SLAB, false) : Blocks.DEEPSLATE_TILES.defaultBlockState());
			}
		}
		// The vault, sealed: the realm fills it as it stirs.
		if (ruin.vault != null) put(level, vaultAt(s, c, ruin), ModBlocks.storageFormation(ruin.vault).defaultBlockState());
		// Rubble and moss.
		for (int k = 0; k < 14; k++) {
			BlockPos at = r.at(random.nextInt(RUIN_HALF * 2 - 1) - RUIN_HALF + 1, random.nextInt(RUIN_HALF * 2 - 1) - RUIN_HALF + 1, 1);
			if (!level.getBlockState(at).isAir()) continue;
			put(level, at, random.nextFloat() < 0.5f ? Blocks.MOSS_CARPET.defaultBlockState() : random.nextBoolean() ? slab(Blocks.COBBLESTONE_SLAB, false)
					: Blocks.MOSSY_COBBLESTONE.defaultBlockState());
		}
		join(level, r.at(-RUIN_HALF - 1, -RUIN_HALF - 1, 0), r.at(RUIN_HALF + 1, RUIN_HALF + 1, 6));
	}

	// --- Trees ---

	/** Trees: {@code part} 0 the cherries of the middle ring, 1 the birches of the shore, 2 the azaleas of the terraces, 3 the pines of the rim. */
	static void trees(ServerLevel level, HerbValley.Shape s, BlockPos c, int part, RandomSource random) {
		var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
		ResourceKey<ConfiguredFeature<?, ?>> kind = switch (part) {
			case 0 -> TreeFeatures.CHERRY;
			case 1 -> TreeFeatures.BIRCH;
			case 2 -> TreeFeatures.AZALEA_TREE;
			default -> TreeFeatures.SPRUCE;
		};
		var tree = features.getHolder(kind);
		if (tree.isEmpty()) return;
		int count = switch (part) {
			case 0 -> 16;
			case 1 -> 8;
			case 2 -> 10;
			default -> 36;
		};
		double from = part == 0 ? HerbValley.LAKE_R + 8 : part == 1 ? HerbValley.LAKE_R + 1 : part == 2 ? HerbValley.MIDDLE_R + 4 : HerbValley.TERRACE_R + 6;
		double to = part == 0 ? HerbValley.MIDDLE_R - 3 : part == 1 ? HerbValley.LAKE_R + 5 : part == 2 ? HerbValley.TERRACE_R - 4 : HerbValley.EDGE_R - 14;
		for (int i = 0, tries = 0; i < count && tries < count * 8; tries++) {
			double a = random.nextDouble() * Math.PI * 2, d = from + random.nextDouble() * (to - from);
			int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
			HerbValley.Column col = HerbValley.column(s, x, z);
			boolean fits = switch (part) {
				case 0, 1 -> col.kind() == HerbValley.Kind.MEADOW || col.kind() == HerbValley.Kind.SHORE && !col.wet();
				case 2 -> col.kind() == HerbValley.Kind.TERRACE;
				default -> col.kind() == HerbValley.Kind.RIM || col.kind() == HerbValley.Kind.PILLAR;
			};
			if (!fits || nearRuin(s, c, c.offset(x, 0, z))) continue;
			BlockPos at = c.offset(x, 0, z).atY(col.ground() + 1);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!level.getBlockState(at).isAir() && !level.getBlockState(at).canBeReplaced()) continue;
			if (tree.get().value().place(level, level.getChunkSource().getGenerator(), random, at)) i++;
		}
	}

	/** Whether {@code pos} lies on or by a ruin's grounds. */
	static boolean nearRuin(HerbValley.Shape s, BlockPos c, BlockPos pos) {
		for (Ruin ruin : Ruin.values()) {
			BlockPos o = ruinFrame(s, c, ruin).origin();
			if (Math.abs(o.getX() - pos.getX()) <= RUIN_HALF + 3 && Math.abs(o.getZ() - pos.getZ()) <= RUIN_HALF + 3) return true;
		}
		return false;
	}

	// --- Renewing ---

	/** Its herbs: each bed's, the garden's, the lake's lotuses; an unpicked one aged by the years shut, one picked grown again. */
	static void renewHerbs(ServerLevel level, HerbValley.Shape s, BlockPos c, SecretRealms.Record r) {
		RandomSource random = level.getRandom();
		int bonus = r.realm.years() * YEARS_A_YEAR_SHUT;
		for (HerbValley.Bed bed : s.beds) {
			for (BlockPos local : HerbValley.herbSpots(s, bed)) {
				BlockPos at = c.offset(local.getX(), 0, local.getZ()).atY(local.getY());
				Block kind = bedHerb(bed.level(), random);
				renewHerb(level, at, kind, 100 + bed.level() * 60 + random.nextInt(400), bonus, Blocks.PODZOL);
			}
		}
		Block[] rare = {ModBlocks.SPIRIT_GINSENG, ModBlocks.PURPLE_LINGZHI, ModBlocks.OCHRE_HUANGJING};
		int i = 0;
		for (BlockPos at : gardenSpots(s, c)) renewHerb(level, at, rare[i++ % rare.length], 400 + random.nextInt(1600), bonus, Blocks.PODZOL);
		for (BlockPos at : lotusSpots(s, c)) renewHerb(level, at, ModBlocks.SPIRIT_LOTUS, 300 + random.nextInt(2700), bonus, null);
	}

	/** A bed's herb by its terrace: the common kinds below, the rare ones creeping in higher up. */
	static Block bedHerb(int terrace, RandomSource random) {
		float rare = 0.04f * terrace;
		float roll = random.nextFloat();
		if (roll < rare * 0.4f) return ModBlocks.SPIRIT_GINSENG;
		if (roll < rare * 0.7f) return ModBlocks.PURPLE_LINGZHI;
		if (roll < rare) return ModBlocks.OCHRE_HUANGJING;
		Block[] common = {ModBlocks.GINSENG, ModBlocks.LINGZHI, ModBlocks.HUANGJING};
		return common[random.nextInt(common.length)];
	}

	/** One herb's spot: aged by the years shut if it's there, else grown again at {@code years} (its soil laid under it, if it needs one). */
	static void renewHerb(ServerLevel level, BlockPos at, Block kind, int years, int bonus, Block soil) {
		level.getChunk(at.getX() >> 4, at.getZ() >> 4);
		if (level.getBlockState(at).getBlock() instanceof GinsengBlock && level.getBlockEntity(at) instanceof GinsengBlockEntity herb) {
			herb.setAge(Math.min(FruitAge.MAX_YEARS, herb.age() + bonus));
			return;
		}
		if (soil != null && !level.getBlockState(at.below()).is(net.minecraft.tags.BlockTags.DIRT)) put(level, at.below(), soil.defaultBlockState());
		if (!level.getBlockState(at).canBeReplaced() && !level.getBlockState(at).isAir()) return;
		level.setBlock(at, kind.defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(at) instanceof GinsengBlockEntity herb) herb.setAge(Math.min(FruitAge.MAX_YEARS, years));
	}

	/** The lake's lotus spots (on its still water, about the island, off the bridge's line). */
	static List<BlockPos> lotusSpots(HerbValley.Shape s, BlockPos c) {
		List<BlockPos> spots = new ArrayList<>();
		RandomSource random = RandomSource.create(s.seed ^ 0x707A5L);
		Direction way = mainWay(s);
		for (int tries = 0; spots.size() < 10 && tries < 80; tries++) {
			double a = random.nextDouble() * Math.PI * 2, d = HerbValley.HEART_R + 3 + random.nextDouble() * (HerbValley.LAKE_R - HerbValley.HEART_R - 7);
			int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
			int along = x * way.getStepX() + z * way.getStepZ(), across = x * way.getStepZ() - z * way.getStepX();
			if (along > 0 && Math.abs(across) < 5) continue; // the bridge's way
			HerbValley.Column col = HerbValley.column(s, x, z);
			if (col.kind() != HerbValley.Kind.LAKE || col.water() != HerbValley.WATER_Y) continue;
			spots.add(c.offset(x, 0, z).atY(HerbValley.WATER_Y + 1));
		}
		return spots;
	}

	/** Its vaults: sealed anew for the ancients, filled with what each holds (vaultTreasures). */
	static void renewVaults(ServerLevel level, HerbValley.Shape s, BlockPos c, SecretRealms.Record r) {
		for (Ruin ruin : Ruin.values()) {
			if (ruin.vault == null) continue;
			BlockPos at = vaultAt(s, c, ruin);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(ruin.vault).defaultBlockState());
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, vaultTreasures(ruin, level.getRandom()));
		}
	}

	/**
	 * What a ruin's vault holds: the Pill Hall's pills, spirit stones, an artifact likely, a Departure Jade, now and then Ancient Essence or the
	 * Spring; the Library's manuals; the Furnace Court's pills and jade; the Quarters' stones and herbs; the Stele Court's artifact.
	 */
	static List<ItemStack> vaultTreasures(Ruin ruin, RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 4 + random.nextInt(8)));
		switch (ruin) {
			case PILL_HALL -> {
				out.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 1 + random.nextInt(3)));
				for (int i = 0; i < 2 + random.nextInt(3); i++) out.add(pill(random, 300, 1500));
				if (random.nextFloat() < 0.6f) out.add(Artifacts.any(random, random.nextFloat() < 0.2f ? ArtifactGrade.EARTH : ArtifactGrade.SPIRIT, null));
				if (random.nextFloat() < 0.4f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
				if (random.nextFloat() < 0.12f) out.add(new ItemStack(ModItems.ANCIENT_ESSENCE));
				if (random.nextFloat() < 0.25f) out.add(new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS));
			}
			case LIBRARY -> {
				for (int i = 0; i < 1 + random.nextInt(2); i++) {
					ItemStack manual = manual(random);
					if (!manual.isEmpty()) out.add(manual);
				}
				if (random.nextFloat() < 0.25f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
			}
			case FURNACE_COURT -> {
				for (int i = 0; i < 2; i++) out.add(pill(random, 100, 800));
				out.add(new ItemStack(ModItems.JADE, 2 + random.nextInt(3)));
				if (random.nextFloat() < 0.3f) out.add(Artifacts.any(random, ArtifactGrade.SPIRIT, null));
			}
			case QUARTERS -> {
				out.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 4 + random.nextInt(6)));
				for (int i = 0; i < 2; i++) out.add(GinsengItem.create(random.nextBoolean() ? ModItems.GINSENG : ModItems.LINGZHI, 100 + random.nextInt(250)));
				if (random.nextFloat() < 0.3f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
			}
			case STELE_COURT -> {
				if (random.nextFloat() < 0.5f) out.add(Artifacts.any(random, ArtifactGrade.SPIRIT, null));
				if (random.nextFloat() < 0.2f) out.add(new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS));
				if (random.nextFloat() < 0.06f) out.add(new ItemStack(ModItems.ANCIENT_ESSENCE));
			}
			default -> {
			}
		}
		return out;
	}

	/** An old pill of the alchemists': a breakthrough or cultivation pill, of herbs {@code from} to {@code to} years old. */
	static ItemStack pill(RandomSource random, int from, int to) {
		Item[] kinds = {ModItems.FOUNDATION_PILL, ModItems.CULTIVATION_PILL, ModItems.QI_GATHERING_PILL, ModItems.CORE_PILL};
		Item kind = kinds[random.nextInt(kinds.length)];
		int age = from + random.nextInt(Math.max(1, to - from));
		return PillItem.create(kind, PillGrade.roll(age, random), age);
	}

	/** A manual of a spell up to Core Formation's, of the heaven path. */
	static ItemStack manual(RandomSource random) {
		List<Spell> spells = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven() && spell.heavenRealm().ordinal() <= Realm.CORE_FORMATION.ordinal()) spells.add(spell);
		}
		return spells.isEmpty() ? ItemStack.EMPTY : TechniqueManualItem.of(spells.get(random.nextInt(spells.size())));
	}

	/** Where its killing formations' eyes lie: the Furnace Court's fire, the Stele Court's blades, a fog on the terraces, the weight before the bridge. */
	static List<Map.Entry<BlockPos, KillingFormationBlock.Array>> formations(HerbValley.Shape s, BlockPos c) {
		List<Map.Entry<BlockPos, KillingFormationBlock.Array>> out = new ArrayList<>();
		out.add(Map.entry(ruinFrame(s, c, Ruin.FURNACE_COURT).at(0, 0, 0), KillingFormationBlock.Array.FIRE));
		out.add(Map.entry(ruinFrame(s, c, Ruin.STELE_COURT).at(1, 0, 0), KillingFormationBlock.Array.BLADES));
		HerbValley.Bed bed = s.beds.get(5 % s.beds.size());
		BlockPos fog = c.offset(bed.middle().getX(), 0, bed.middle().getZ());
		out.add(Map.entry(fog.atY(groundAt(s, c, fog.getX(), fog.getZ())), KillingFormationBlock.Array.FOG));
		Frame way = new Frame(c, mainWay(s));
		BlockPos weight = way.at(HerbValley.LAKE_R + 7, 2, 0);
		out.add(Map.entry(weight.atY(groundAt(s, c, weight.getX(), weight.getZ())), KillingFormationBlock.Array.GRAVITY));
		return out;
	}

	/** Its killing formations' eyes set again (flush in the ground) where they were broken. */
	static void renewFormations(ServerLevel level, HerbValley.Shape s, BlockPos c) {
		for (Map.Entry<BlockPos, KillingFormationBlock.Array> eye : formations(s, c)) {
			level.getChunk(eye.getKey().getX() >> 4, eye.getKey().getZ() >> 4);
			put(level, eye.getKey(), ModBlocks.KILLING_FORMATION.defaultBlockState().setValue(KillingFormationBlock.ARRAY, eye.getValue()));
		}
	}

	/** The hall: its barrier whole, the master's cauldron's water and fire, a jade laid on his altar. */
	static void renewHall(ServerLevel level, HerbValley.Shape s, BlockPos c) {
		for (BlockPos p : doorway(s, c)) if (!level.getBlockState(p).is(ModBlocks.TRIAL_BARRIER)) put(level, p, ModBlocks.TRIAL_BARRIER.defaultBlockState());
		BlockPos cauldron = trialCauldron(s, c);
		if (!level.getBlockState(cauldron.below()).is(Blocks.CAMPFIRE)) put(level, cauldron.below(), Blocks.CAMPFIRE.defaultBlockState());
		BlockState pot = level.getBlockState(cauldron);
		if (!pot.is(ModBlocks.ALCHEMY_CAULDRON)) put(level, cauldron, ModBlocks.ALCHEMY_CAULDRON.defaultBlockState().setValue(AlchemyCauldronBlock.FILLED, true));
		else if (!pot.getValue(AlchemyCauldronBlock.FILLED)) put(level, cauldron, pot.setValue(AlchemyCauldronBlock.FILLED, true));
		BlockPos altar = altar(s, c);
		if (level.getEntitiesOfClass(ItemEntity.class, new AABB(altar).inflate(1), e -> e.getItem().is(ModItems.JADE)).isEmpty()) {
			ItemEntity jade = new ItemEntity(level, altar.getX() + 0.5, altar.getY() + 0.1, altar.getZ() + 0.5, new ItemStack(ModItems.JADE));
			jade.setDeltaMovement(Vec3.ZERO);
			jade.setUnlimitedLifetime();
			level.addFreshEntity(jade);
		}
	}

	/** An earlier visitor's bones: in one of its ruins, or by one of its beds. */
	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		HerbValley.Shape s = HerbValley.shape(seed);
		if (random.nextBoolean()) {
			Ruin ruin = Ruin.values()[random.nextInt(Ruin.values().length)];
			return ruinFrame(s, c, ruin).at(random.nextInt(9) - 4, random.nextInt(9) - 4, 1);
		}
		HerbValley.Bed bed = s.beds.get(random.nextInt(s.beds.size()));
		BlockPos m = c.offset(bed.middle().getX() + random.nextInt(5) - 2, 0, bed.middle().getZ() + random.nextInt(5) - 2);
		return m.atY(groundAt(s, c, m.getX(), m.getZ()) + 1);
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 2 + random.nextInt(5)));
		if (random.nextFloat() < 0.5f) carried.add(GinsengItem.create(random.nextBoolean() ? ModItems.GINSENG : ModItems.HUANGJING, 100 + random.nextInt(500)));
		if (random.nextFloat() < 0.3f) carried.add(pill(random, 50, 400));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (random.nextFloat() < 0.15f) {
			ItemStack manual = manual(random);
			if (!manual.isEmpty()) carried.add(manual);
		}
		return carried;
	}

	@Override
	protected int remainsRank(SecretRealm realm, RandomSource random) {
		return PlayerCultivation.rank(random.nextBoolean() ? Realm.QI_REFINING : Realm.FOUNDATION_BUILDING, Stage.byIndex(random.nextInt(4)));
	}

	/** The arrival terraces at its paths' heads. */
	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : HerbValley.arrivals(HerbValley.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	// --- During an opening ---

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		return slots(HerbValley.shape(seed), c, realm);
	}

	/** The Elder Keeper comes with the bloom. */
	@Override
	protected boolean raisable(Slot slot, SecretRealms.Record r) {
		return !slot.id().equals("elder") || r.works.getInt("Bloom") == 1;
	}

	/** The heart holds: the lake's ring and all within it. */
	@Override
	protected int calm() { return HerbValley.LAKE_R; }

	/** Every guardian's post in the valley. */
	static List<Slot> slots(HerbValley.Shape s, BlockPos c, SecretRealm realm) {
		List<Slot> slots = new ArrayList<>();
		for (HerbValley.Bed bed : s.beds) {
			BlockPos m = c.offset(bed.middle().getX(), 0, bed.middle().getZ());
			Vec3 out = new Vec3(bed.middle().getX(), 0, bed.middle().getZ()).normalize();
			// Beside its bed, toward the wall behind it.
			BlockPos home = m.offset((int) Math.round(out.x * 2), 0, (int) Math.round(out.z * 2));
			home = home.atY(groundAt(s, c, home.getX(), home.getZ()) + 1);
			slots.add(new Slot("bed" + bed.index(), ModEntities.VINE_KEEPER, home, realm.guardianRank(bed.level() >= 5 ? Stage.MID : Stage.EARLY), false));
		}
		for (Ruin ruin : Ruin.values()) {
			int k = 0;
			for (BlockPos post : puppetPosts(s, c, ruin)) {
				slots.add(new Slot(ruin.name().toLowerCase() + k++, ModEntities.BRONZE_PUPPET, post, realm.guardianRank(ruin == Ruin.PILL_HALL ? Stage.LATE : Stage.MID), false));
			}
		}
		slots.add(new Slot("garden", ModEntities.VINE_KEEPER, ruinFrame(s, c, Ruin.GARDEN).at(0, 0, 1), realm.guardianRank(Stage.LATE), false));
		slots.add(new Slot("gatekeeper", ModEntities.BRONZE_PUPPET, gatePost(s, c), realm.guardianRank(Stage.EARLY), true));
		slots.add(new Slot("elder", ModEntities.VINE_KEEPER, bloomSpot(s, c).offset(2, 0, 2), realm.guardianRank(Stage.GRAND_PERFECTION), false));
		return slots;
	}

	/** The opening begins: its bloom's hour rolled. */
	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("BloomAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Bloom", 0);
	}

	/** The opening ends: an untaken bloom withers. */
	@Override
	protected void end(ServerLevel level, SecretRealms.Record r) {
		if (r.works.getInt("Bloom") == 1) wither(level, r, false);
	}

	/** Each tick while it stands open: the heart's bloom; the master's cauldron kept; the trials told to whoever comes before the hall. */
	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		HerbValley.Shape s = HerbValley.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		bloom(level, s, c, r, now, data);
		// The master's cauldron keeps its water (the realm fills it while it stands idle); the trial told to whoever comes before the hall.
		if (now % 40 == 11) {
			BlockPos cauldron = trialCauldron(s, c);
			if (level.isLoaded(cauldron) && level.getBlockEntity(cauldron) instanceof AlchemyCauldronBlockEntity pot && !pot.getBlockState().getValue(AlchemyCauldronBlock.FILLED)
					&& !pot.getBlockState().getValue(AlchemyCauldronBlock.BREWING)) {
				RealmLedger.quietly(() -> level.setBlock(cauldron, pot.getBlockState().setValue(AlchemyCauldronBlock.FILLED, true), Block.UPDATE_ALL));
			}
			BlockPos door = gatePost(s, c);
			ListTag told = r.works.getList("Told", Tag.TAG_STRING);
			for (ServerPlayer p : inside) {
				if (p.blockPosition().distSqr(door) > 10 * 10 || told.contains(StringTag.valueOf(p.getStringUUID()))) continue;
				told.add(StringTag.valueOf(p.getStringUUID()));
				p.sendSystemMessage(Component.translatable(ModLang.MSG_SAGE_TRIALS, r.inheritor.isEmpty() ? Component.translatable(ModLang.MSG_SAGE_GREAT)
						: Component.translatable(ModLang.MSG_SAGE_LESSER)).withStyle(ChatFormatting.GOLD));
			}
			r.works.put("Told", told);
		}
	}

	/** A herb taken in the valley (GinsengBlock#onRemove): the keeper of its bed wakes, after whoever is nearest; the bloom, if it was the bloom. */
	static void herbTaken(ServerLevel level, BlockPos pos) {
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm == null) return;
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		SecretRealms.Record r = data.of(realm);
		if (r.openedAt < 0) return;
		LivingEntity taker = nearestVisitor(level, pos, 8);
		for (RealmGuardian keeper : level.getEntitiesOfClass(RealmGuardian.class, new AABB(pos).inflate(10), g -> g.kind() == RealmGuardian.Kind.VINE_KEEPER)) keeper.wake(taker);
		HerbValley.Shape s = HerbValley.shape(level.getSeed());
		if (r.works.getInt("Bloom") == 1 && pos.equals(bloomSpot(s, realm.centre()))) {
			r.works.putInt("Bloom", 2);
			data.setDirty();
			Component who = taker == null ? Component.translatable(ModLang.SECRET_SOMEONE) : taker.getDisplayName();
			tellInside(level, realm, Component.translatable(ModLang.MSG_BLOOM_TAKEN, who).withStyle(ChatFormatting.GOLD));
		}
	}

	// --- The heart blooms ---

	/** Its hour come: a ten-thousand-year Spirit Ginseng flowers in the ring for {@link #BLOOM_TICKS}, told to all inside; then, untaken, it withers. */
	static void bloom(ServerLevel level, HerbValley.Shape s, BlockPos c, SecretRealms.Record r, long now, SecretRealms.Data data) {
		int stage = r.works.getInt("Bloom");
		BlockPos spot = bloomSpot(s, c);
		if (stage == 0 && now >= r.works.getLong("BloomAt")) {
			level.getChunk(spot.getX() >> 4, spot.getZ() >> 4);
			RealmLedger.quietly(() -> {
				if (!level.getBlockState(spot.below()).is(net.minecraft.tags.BlockTags.DIRT)) put(level, spot.below(), Blocks.PODZOL.defaultBlockState());
				level.setBlock(spot, ModBlocks.SPIRIT_GINSENG.defaultBlockState(), Block.UPDATE_ALL);
				if (level.getBlockEntity(spot) instanceof GinsengBlockEntity herb) herb.setAge(FruitAge.MAX_YEARS);
			});
			r.works.putInt("Bloom", 1);
			r.works.putLong("BloomEnds", now + BLOOM_TICKS);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_BLOOM_BEGINS, BLOOM_TICKS / 1200).withStyle(ChatFormatting.LIGHT_PURPLE));
			for (ServerPlayer p : level.players()) {
				if (SecretRealm.at(p.getX(), p.getZ()) == r.realm) p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1.0f, 0.5f);
			}
		} else if (stage == 1) {
			if (level.isLoaded(spot) && now % 5 == 0) {
				level.sendParticles(ParticleTypes.END_ROD, spot.getX() + 0.5, spot.getY() + 1.5, spot.getZ() + 0.5, 4, 0.2, 2.0, 0.2, 0.02);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.getX() + 0.5, spot.getY() + 0.6, spot.getZ() + 0.5, 2, 0.4, 0.3, 0.4, 0);
			}
			if (now >= r.works.getLong("BloomEnds")) wither(level, r, true);
		}
	}

	/** The bloom untaken at its minute's end: it withers. */
	private static void wither(ServerLevel level, SecretRealms.Record r, boolean tell) {
		HerbValley.Shape s = HerbValley.shape(level.getSeed());
		BlockPos spot = bloomSpot(s, r.realm.centre());
		if (level.isLoaded(spot) && level.getBlockState(spot).getBlock() instanceof GinsengBlock) {
			RealmLedger.quietly(() -> level.setBlock(spot, Blocks.DEAD_BUSH.defaultBlockState(), Block.UPDATE_ALL));
		}
		r.works.putInt("Bloom", 2);
		if (tell) tellInside(level, r.realm, Component.translatable(ModLang.MSG_BLOOM_WITHERS).withStyle(ChatFormatting.GRAY));
	}

	// --- Its trials and inheritance ---

	/**
	 * A pill brewed (AlchemyCauldronBlockEntity#finish): in the master's own cauldron, by one who passed the Gatekeeper, a Foundation Pill of
	 * Mid-Grade or better: the second trial passed, the inheritance theirs ({@link #inherit}). A lesser brew there is told it falls short.
	 */
	static void brewed(net.minecraft.world.level.Level world, BlockPos pos, ServerPlayer brewer, Item pill, PillGrade grade) {
		if (!(world instanceof ServerLevel level)) return;
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm == null) return;
		HerbValley.Shape s = HerbValley.shape(level.getSeed());
		if (!pos.equals(trialCauldron(s, realm.centre()))) return;
		SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
		if (r.openedAt < 0 || !passed(r, brewer.getUUID())) return;
		if (pill != ModItems.FOUNDATION_PILL || grade.ordinal() < PillGrade.MID.ordinal()) {
			brewer.sendSystemMessage(Component.translatable(ModLang.MSG_SAGE_FALLS_SHORT).withStyle(ChatFormatting.GRAY));
			return;
		}
		inherit(level, r, brewer);
	}

	/** The Great Inheritance: the Pill Sage's Cauldron and his Notes. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(new ItemStack(ModBlocks.PILL_SAGE_CAULDRON.asItem()), new ItemStack(ModItems.PILL_SAGE_NOTES));
	}

	/** A lesser legacy: a page of his Notes and a Spring of the Ancients. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		return List.of(new ItemStack(ModItems.PILL_SAGE_PAGE), new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_SAGE_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_SAGE_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_SAGE_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** Brewing at the master's cauldron takes one come for it this long (ticks). */
	static final int RIVAL_BREW_TICKS = 900;

	/** Down a path from its head toward the lake: the further, the later. */
	@Override
	protected BlockPos rivalStart(ServerLevel level, long seed, BlockPos c, double along, RandomSource random) {
		HerbValley.Shape s = HerbValley.shape(seed);
		double b = HerbValley.pathBearing(s, random.nextInt(HerbValley.PATHS));
		double out = HerbValley.ARRIVAL_R - along * (HerbValley.ARRIVAL_R - HerbValley.LAKE_R - 6) + random.nextInt(5) - 2;
		return new BlockPos(c.getX() + (int) Math.round(Math.cos(b) * out), 0, c.getZ() + (int) Math.round(Math.sin(b) * out));
	}

	/** The nearest herb grown in its beds. */
	@Override
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) {
		HerbValley.Shape s = HerbValley.shape(seed);
		BlockPos best = null;
		double d = 64 * 64;
		for (HerbValley.Bed bed : s.beds) {
			for (BlockPos local : HerbValley.herbSpots(s, bed)) {
				BlockPos at = c.offset(local.getX(), 0, local.getZ()).atY(local.getY());
				double here = at.distSqr(near);
				if (here < d && level.isLoaded(at) && level.getBlockState(at).getBlock() instanceof GinsengBlock) {
					d = here;
					best = at;
				}
			}
		}
		return best;
	}

	/** A herb dug up (its keeper wakes, as for anyone: GinsengBlock#onRemove). */
	@Override
	protected List<ItemStack> rivalTake(ServerLevel level, BlockPos at, CultivatorNpc npc) {
		if (!(level.getBlockState(at).getBlock() instanceof GinsengBlock herb) || !(level.getBlockEntity(at) instanceof GinsengBlockEntity aging)) return List.of();
		ItemStack root = herb.root(aging.age());
		level.destroyBlock(at, false, npc);
		return List.of(root);
	}

	@Override
	protected List<BlockPos> vaults(long seed, BlockPos c) {
		HerbValley.Shape s = HerbValley.shape(seed);
		List<BlockPos> out = new ArrayList<>();
		for (Ruin ruin : Ruin.values()) if (ruin.vault != null) out.add(vaultAt(s, c, ruin));
		return out;
	}

	/** The Gatekeeper, then the master's cauldron. */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		HerbValley.Shape s = HerbValley.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		if (!passed(r, npc.getUUID())) return new Trial("gate", gatePost(s, c), 4, false);
		return passed(r, "Brewed", npc.getUUID()) ? null : new Trial("brew", trialCauldron(s, c), 2.6, false);
	}

	/** The Gatekeeper challenged; the brew made ({@link #RIVAL_BREW_TICKS}), its success by its realm, a third at the realm's foot and more above. */
	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		if (trial.step().equals("gate")) return challengeAt(level, r, npc, trial.mark(), working);
		BlockPos mark = trial.mark();
		if (working % 40 == 0) level.sendParticles(ParticleTypes.CLOUD, mark.getX() + 0.5, mark.getY() + 1.0, mark.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
		if (working < RIVAL_BREW_TICKS) return false;
		pass(r, "Brewed", npc.getUUID());
		int above = npc.getRank() - r.realm.guardianRank(Stage.EARLY);
		if (npc.getRandom().nextDouble() < Mth.clamp(0.33 + 0.08 * above, 0.15, 0.9)) rivalInherits(level, r, npc);
		else level.sendParticles(ParticleTypes.LARGE_SMOKE, npc.getX(), npc.getY() + 1.5, npc.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
		return true;
	}

	private HerbValleyWorks() {}
}
