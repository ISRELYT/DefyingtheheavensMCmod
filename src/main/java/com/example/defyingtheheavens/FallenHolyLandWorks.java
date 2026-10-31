package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Ruins of a Fallen Holy Land's works (SecretRealm#FALLEN_HOLY_LAND; its land is FallenHolyLand's, the machinery every realm shares
 * RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: on the heart shard the ancestral hall of white stone and gold on its terrace, its door a barrier, the pillars of its
 *   formation about it; on each eye shard a dais with its formation eye (FormationEyeBlock); the holy land's broken halls on the ruin
 *   shards; a terrace at each arrival.</li>
 *   <li><b>Renewed each time it stirs</b>: the eyes whole, the hall's door barred again; two vaults sealed anew for the ancients; the
 *   bones of a few earlier visitors.</li>
 *   <li><b>The currents</b>: void currents between the shards, two to each pair joined (there and back): step into one and it bears you
 *   along to its far end ({@link #CURRENT_SPEED}).</li>
 *   <li><b>Its guardians</b>: the holy land's remnant guards on the shards, about the hall, and one by each eye.</li>
 *   <li><b>Its event</b>: some time into each opening the void tide ({@link #TIDE_TICKS}): the currents surge, and whoever stands near a
 *   shard's edge is flung off it.</li>
 *   <li><b>Its trial</b>: break the ancestral formation: its four eyes, each broken once its guard is down; all four broken, the formation
 *   falls for this opening and the hall stands open to all; at its ancestral altar the Great Inheritance (the holy land's ancestral sword,
 *   a Perfect Heaven-grade artifact) once in a world, after it a lesser legacy each opening.</li>
 * </ul>
 */
public final class FallenHolyLandWorks extends RealmWorks {
	static final FallenHolyLandWorks WORKS = new FallenHolyLandWorks();
	/** The void tide lasts this long (a minute); every this often in it, those at the shards' edges are flung. */
	static final int TIDE_TICKS = 1200, TIDE_FLING_EVERY = 40;
	/** A current bears one along this fast (blocks a tick), in the tide this much faster; it takes hold this near its line. */
	static final double CURRENT_SPEED = 0.8, TIDE_SURGE = 1.8, CURRENT_REACH = 1.8;
	/** A guard this near its eye keeps every hand off it. */
	static final double EYE_GUARD_REACH = 12;
	/** The formation's eyes. */
	static final int EYES = 4;

	// --- Where things are ---

	static Direction mainWay(FallenHolyLand.Shape s) {
		return Direction.fromDelta((int) Math.round(Math.cos(s.mainTurn)), 0, (int) Math.round(Math.sin(s.mainTurn)));
	}

	/** The hall's frame: its middle the heart shard's at its top, forward along the main way (its door that way), s to its right. */
	static Frame hall(FallenHolyLand.Shape s, BlockPos c) {
		return new Frame(c.atY(FallenHolyLand.HEART_TOP), mainWay(s));
	}

	/** The hall's door (its barrier while the formation stands): three wide, four high, in its front wall. */
	static List<BlockPos> doorway(Frame h) {
		List<BlockPos> door = new ArrayList<>();
		for (int sd = -1; sd <= 1; sd++) for (int y = 1; y <= 4; y++) door.add(h.at(8, sd, y));
		return door;
	}

	/** Before the ancestral altar, where its legacy waits. */
	static BlockPos altar(Frame h) { return h.at(-5, 0, 1); }

	/** Whether local (f, s, y) of the hall's frame is of the hall (its walls, roof and floor); within it, inside it. */
	static boolean ofHall(int[] at) {
		return at[0] >= -11 && at[0] <= 9 && Math.abs(at[1]) <= 10 && at[2] >= -4 && at[2] <= 16;
	}

	static boolean inHall(int[] at) {
		return at[0] >= -9 && at[0] <= 7 && Math.abs(at[1]) <= 8 && at[2] >= 0 && at[2] <= 7;
	}

	/** The {@code i}th formation eye: on a pedestal on its shard's dais. */
	static BlockPos eye(FallenHolyLand.Shape s, BlockPos c, int i) {
		FallenHolyLand.Shard shard = s.eyes().get(i);
		return c.offset(shard.x(), 0, shard.z()).atY(daisY(s, shard) + 2);
	}

	/** An eye's dais: level at its shard's middle. */
	static int daisY(FallenHolyLand.Shape s, FallenHolyLand.Shard shard) {
		return FallenHolyLand.column(s, shard.x(), shard.z()).ground();
	}

	/** Which eye is at {@code pos}, or -1. */
	static int eyeAt(FallenHolyLand.Shape s, BlockPos c, BlockPos pos) {
		for (int i = 0; i < EYES; i++) if (eye(s, c, i).equals(pos)) return i;
		return -1;
	}

	/** A shard's middle, at its top. */
	static BlockPos shardMiddle(FallenHolyLand.Shape s, BlockPos c, FallenHolyLand.Shard shard) {
		return c.offset(shard.x(), 0, shard.z()).atY(FallenHolyLand.column(s, shard.x(), shard.z()).ground());
	}

	/** The ruin shards, the largest first: the first two keep the vaults. */
	static List<FallenHolyLand.Shard> ruins(FallenHolyLand.Shape s) {
		List<FallenHolyLand.Shard> out = new ArrayList<>();
		for (FallenHolyLand.Shard shard : s.shards) if (shard.role().equals("ruin")) out.add(shard);
		out.sort((a, b) -> b.r() - a.r());
		return out;
	}

	static BlockPos vaultAt(FallenHolyLand.Shape s, BlockPos c, FallenHolyLand.Shard shard) {
		return shardMiddle(s, c, shard).above();
	}

	/** One void current: from its start to its end (both a little above the shards' tops), the way it flows. */
	record Current(Vec3 from, Vec3 to) {
		Vec3 way() { return to.subtract(from).normalize(); }
	}

	private static final Map<Long, List<Current>> CURRENTS = Collections.synchronizedMap(new HashMap<>());

	/**
	 * The currents: each shard joined to the nearest already joined (the shortest tree of them, from the heart out), two currents to each
	 * pair, there and back, a little to either side; each from just within one shard's edge to just within the other's.
	 */
	static List<Current> currents(FallenHolyLand.Shape s, BlockPos c) {
		return CURRENTS.computeIfAbsent(s.seed ^ c.asLong(), k -> {
			List<FallenHolyLand.Shard> shards = s.shards;
			boolean[] joined = new boolean[shards.size()];
			joined[0] = true;
			List<Current> out = new ArrayList<>();
			for (int n = 1; n < shards.size(); n++) {
				int from = -1, to = -1;
				double best = Double.MAX_VALUE;
				for (int a = 0; a < shards.size(); a++) {
					if (!joined[a]) continue;
					for (int b = 0; b < shards.size(); b++) {
						if (joined[b]) continue;
						FallenHolyLand.Shard sa = shards.get(a), sb = shards.get(b);
						double d = Math.hypot(sa.x() - sb.x(), sa.z() - sb.z()) - sa.r() - sb.r();
						if (d < best) {
							best = d;
							from = a;
							to = b;
						}
					}
				}
				if (to < 0) break;
				joined[to] = true;
				FallenHolyLand.Shard a = shards.get(from), b = shards.get(to);
				double dx = b.x() - a.x(), dz = b.z() - a.z(), len = Math.max(1, Math.hypot(dx, dz)), ux = dx / len, uz = dz / len;
				double y = Math.max(a.top(), b.top()) + 2.5;
				for (int side = -1; side <= 1; side += 2) {
					double ox = -uz * side * 2, oz = ux * side * 2;
					Vec3 pa = new Vec3(c.getX() + 0.5 + a.x() + ux * (a.r() - 3) + ox, y, c.getZ() + 0.5 + a.z() + uz * (a.r() - 3) + oz);
					Vec3 pb = new Vec3(c.getX() + 0.5 + b.x() - ux * (b.r() - 3) + ox, y, c.getZ() + 0.5 + b.z() - uz * (b.r() - 3) + oz);
					out.add(side < 0 ? new Current(pa, pb) : new Current(pb, pa));
				}
			}
			return List.copyOf(out);
		});
	}

	static void clear() {
		CURRENTS.clear();
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		steps.add(() -> terrace(level, s, c));
		steps.add(() -> hallLaid(level, s, c));
		for (int i = 0; i < EYES; i++) {
			int k = i;
			steps.add(() -> dais(level, s, c, k));
		}
		for (FallenHolyLand.Shard shard : s.shards) {
			if (shard.role().equals("ruin") && shard.r() >= 9) steps.add(() -> brokenHall(level, s, c, shard, RandomSource.create(seed ^ shard.x() * 31L ^ shard.z() * 17L)));
		}
		steps.add(() -> arrivalTerraces(level, s, c));
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		steps.add(() -> renewFormation(level, s, c));
		steps.add(() -> renewVaults(level, s, c));
	}

	// --- Building ---

	static BlockState whiteStone(RandomSource random) {
		float roll = random.nextFloat();
		return (roll < 0.15f ? Blocks.CRACKED_STONE_BRICKS : roll < 0.3f ? Blocks.CALCITE : Blocks.QUARTZ_BRICKS).defaultBlockState();
	}

	/** The hall's terrace on the heart shard: white paving with a ring of gold-veined stone, the shard's grass cleared off it, its pillars of the formation. */
	static void terrace(ServerLevel level, FallenHolyLand.Shape s, BlockPos c) {
		Frame h = hall(s, c);
		for (int cx = (c.getX() - 24) >> 4; cx <= (c.getX() + 24) >> 4; cx++) for (int cz = (c.getZ() - 24) >> 4; cz <= (c.getZ() + 24) >> 4; cz++) level.getChunk(cx, cz);
		for (int f = -17; f <= 17; f++) {
			for (int sd = -17; sd <= 17; sd++) {
				if (Math.abs(f) + Math.abs(sd) > 28) continue;
				BlockPos at = h.at(f, sd, 0);
				boolean rim = Math.abs(f) == 17 || Math.abs(sd) == 17 || Math.abs(f) + Math.abs(sd) >= 27;
				put(level, at, rim ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : (f + sd) % 5 == 0 ? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
						: Blocks.POLISHED_DIORITE.defaultBlockState());
				for (int y = -1; y >= -4; y--) if (level.getBlockState(at.above(y)).isAir()) put(level, at.above(y), Blocks.CALCITE.defaultBlockState());
				for (int y = 1; y <= 18; y++) put(level, at.above(y), Blocks.AIR.defaultBlockState());
			}
		}
		// The formation's eight pillars about the hall, lit at their tops.
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4 + Math.PI / 8;
			BlockPos at = h.origin().offset((int) Math.round(Math.cos(a) * 15), 0, (int) Math.round(Math.sin(a) * 15));
			for (int y = 1; y <= 4; y++) put(level, at.above(y), Blocks.QUARTZ_PILLAR.defaultBlockState());
			put(level, at.above(5), Blocks.SEA_LANTERN.defaultBlockState());
		}
		join(level, h.at(-18, -18, -5), h.at(18, 18, 19));
	}

	/**
	 * The ancestral hall: walls of white stone on columns of carved quartz, a band of gold under its eaves, its door barred by the formation;
	 * within, the ancestral altar of gold (candles on it) and the tablets of the holy land's ancestors along its walls; a roof of white tile.
	 */
	static void hallLaid(ServerLevel level, FallenHolyLand.Shape s, BlockPos c) {
		Frame h = hall(s, c);
		RandomSource random = RandomSource.create(s.seed ^ 0x401E4A11L);
		for (int f = -10; f <= 8; f++) {
			for (int sd = -9; sd <= 9; sd++) {
				boolean wall = f == -10 || f == 8 || Math.abs(sd) == 9;
				for (int y = 1; y <= 7; y++) {
					BlockState state = !wall ? Blocks.AIR.defaultBlockState() : y == 7 ? Blocks.CUT_SANDSTONE.defaultBlockState()
							: (Math.abs(sd) == 9 && (f + 10) % 3 == 0 || f == -10 && Math.abs(sd) % 3 == 0 || f == 8 && Math.abs(sd) % 3 == 0)
							? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : whiteStone(random);
					put(level, h.at(f, sd, y), state);
				}
			}
		}
		for (BlockPos at : doorway(h)) put(level, at, ModBlocks.TRIAL_BARRIER.defaultBlockState());
		for (int sd = -2; sd <= 2; sd++) put(level, h.at(8, sd, 5), Blocks.GOLD_BLOCK.defaultBlockState());
		// The ancestral altar and the ancestors' tablets.
		put(level, h.at(-7, 0, 1), Blocks.QUARTZ_PILLAR.defaultBlockState());
		put(level, h.at(-7, 0, 2), Blocks.GOLD_BLOCK.defaultBlockState());
		put(level, h.at(-7, 0, 3), Blocks.YELLOW_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		for (int f = -7; f <= 4; f += 3) {
			for (int sd = -8; sd <= 8; sd += 16) {
				put(level, h.at(f, sd, 1), Blocks.POLISHED_DIORITE.defaultBlockState());
				put(level, h.at(f, sd, 2), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
			}
		}
		HerbValleyWorks.hipRoof(level, h, -11, 9, -10, 10, 8, Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_SLAB, Blocks.QUARTZ_BLOCK);
		for (int f : new int[] {-6, 0, 5}) put(level, h.at(f, 0, 7), lantern(true));
		join(level, h.at(-12, -11, 0), h.at(10, 11, 18));
	}

	/** An eye's dais: white stone ringed with carved quartz, a pedestal in its middle, the eye on it. */
	static void dais(ServerLevel level, FallenHolyLand.Shape s, BlockPos c, int i) {
		FallenHolyLand.Shard shard = s.eyes().get(i);
		BlockPos mid = c.offset(shard.x(), 0, shard.z()).atY(daisY(s, shard));
		level.getChunk(mid.getX() >> 4, mid.getZ() >> 4);
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				BlockPos at = mid.offset(dx, 0, dz);
				put(level, at, Math.max(Math.abs(dx), Math.abs(dz)) == 3 ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : Blocks.POLISHED_DIORITE.defaultBlockState());
				for (int y = 1; y <= 4; y++) put(level, at.above(y), Blocks.AIR.defaultBlockState());
			}
		}
		put(level, mid.above(), Blocks.QUARTZ_PILLAR.defaultBlockState());
		put(level, eye(s, c, i), ModBlocks.FORMATION_EYE.defaultBlockState());
		join(level, mid.offset(-4, 0, -4), mid.offset(4, 5, 4));
	}

	/** A broken hall of the holy land on a ruin shard: walls fallen to stumps, a column or two still standing, rubble about it. */
	static void brokenHall(ServerLevel level, FallenHolyLand.Shape s, BlockPos c, FallenHolyLand.Shard shard, RandomSource random) {
		BlockPos mid = shardMiddle(s, c, shard);
		level.getChunk(mid.getX() >> 4, mid.getZ() >> 4);
		int hw = 3 + random.nextInt(2), hl = 4 + random.nextInt(2);
		for (int dx = -hl; dx <= hl; dx++) {
			for (int dz = -hw; dz <= hw; dz++) {
				BlockPos at = mid.offset(dx, 0, dz);
				if (FallenHolyLand.column(s, at.getX() - c.getX(), at.getZ() - c.getZ()).kind() == FallenHolyLand.Kind.NONE) continue;
				boolean wall = Math.abs(dx) == hl || Math.abs(dz) == hw;
				put(level, at, random.nextFloat() < 0.7f ? Blocks.POLISHED_DIORITE.defaultBlockState() : Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
				if (!wall) continue;
				int height = random.nextFloat() < 0.25f ? 0 : 1 + random.nextInt(4);
				boolean column = Math.abs(dx) == hl && Math.abs(dz) == hw;
				if (column) height = 3 + random.nextInt(3);
				for (int y = 1; y <= height; y++) put(level, at.above(y), column ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState() : whiteStone(random));
			}
		}
		for (int k = 0; k < 5; k++) {
			BlockPos at = mid.offset(random.nextInt(2 * hl + 5) - hl - 2, 1, random.nextInt(2 * hw + 5) - hw - 2);
			if (level.getBlockState(at).isAir() && !level.getBlockState(at.below()).isAir()) put(level, at, slab(Blocks.QUARTZ_SLAB, false));
		}
		join(level, mid.offset(-hl - 3, -1, -hw - 3), mid.offset(hl + 3, 7, hw + 3));
	}

	static void arrivalTerraces(ServerLevel level, FallenHolyLand.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0x401EA7L);
		for (BlockPos at : WORKS.arrivalSpots(s.seed, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz > 12) continue;
					put(level, at.offset(dx, 0, dz), (random.nextFloat() < 0.3f ? Blocks.CRACKED_STONE_BRICKS : Blocks.POLISHED_DIORITE).defaultBlockState());
					for (int y = 1; y <= 4; y++) put(level, at.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
				}
			}
			for (int side = -3; side <= 3; side += 6) {
				put(level, at.offset(side, 1, 0), Blocks.QUARTZ_PILLAR.defaultBlockState());
				put(level, at.offset(side, 2, 0), lantern(false));
			}
			join(level, at.offset(-4, 0, -4), at.offset(4, 4, 4));
		}
	}

	// --- Renewing ---

	/** The formation whole: its eyes, the hall's door barred. */
	static void renewFormation(ServerLevel level, FallenHolyLand.Shape s, BlockPos c) {
		for (int i = 0; i < EYES; i++) {
			BlockPos at = eye(s, c, i);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!level.getBlockState(at).is(ModBlocks.FORMATION_EYE)) put(level, at, ModBlocks.FORMATION_EYE.defaultBlockState());
		}
		Frame h = hall(s, c);
		level.getChunk(h.origin().getX() >> 4, h.origin().getZ() >> 4);
		for (BlockPos p : doorway(h)) if (!level.getBlockState(p).is(ModBlocks.TRIAL_BARRIER)) put(level, p, ModBlocks.TRIAL_BARRIER.defaultBlockState());
	}

	static void renewVaults(ServerLevel level, FallenHolyLand.Shape s, BlockPos c) {
		List<FallenHolyLand.Shard> ruins = ruins(s);
		for (int k = 0; k < Math.min(2, ruins.size()); k++) {
			BlockPos at = vaultAt(s, c, ruins.get(k));
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(StorageFormations.Tier.HIGH).defaultBlockState());
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, vaultTreasures(level.getRandom()));
		}
	}

	/** A hoard of the holy land: supreme spirit stones, now and then an immortal one, a Heaven-grade artifact, a Departure Jade. */
	static List<ItemStack> vaultTreasures(RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 1 + random.nextInt(3)));
		out.add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 4 + random.nextInt(6)));
		if (random.nextFloat() < 0.15f) out.add(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE));
		if (random.nextFloat() < 0.3f) out.add(Artifacts.any(random, ArtifactGrade.HEAVEN, null));
		if (random.nextFloat() < 0.3f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
		return out;
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : FallenHolyLand.arrivals(FallenHolyLand.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	/** An earlier visitor's bones: on one of the ruin shards. */
	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		List<FallenHolyLand.Shard> ruins = ruins(s);
		if (ruins.isEmpty()) return null;
		FallenHolyLand.Shard shard = ruins.get(random.nextInt(ruins.size()));
		double a = random.nextDouble() * Math.PI * 2, d = shard.r() * random.nextDouble() * 0.7;
		int x = shard.x() + (int) Math.round(Math.cos(a) * d), z = shard.z() + (int) Math.round(Math.sin(a) * d);
		FallenHolyLand.Column col = FallenHolyLand.column(s, x, z);
		return col.kind() != FallenHolyLand.Kind.NONE ? c.offset(x, 0, z).atY(col.ground() + 1) : null;
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 2 + random.nextInt(4)));
		if (random.nextFloat() < 0.3f) carried.add(new ItemStack(ModItems.SUPREME_SPIRIT_STONE));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (random.nextFloat() < 0.1f) carried.add(Artifacts.any(random, ArtifactGrade.HEAVEN, null));
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		List<Slot> slots = new ArrayList<>();
		for (int i = 0; i < EYES; i++) {
			FallenHolyLand.Shard shard = s.eyes().get(i);
			slots.add(new Slot("eye" + i, ModEntities.HOLY_REMNANT, c.offset(shard.x() + 2, 0, shard.z() + 2).atY(daisY(s, shard) + 1), realm.guardianRank(Stage.LATE), false));
		}
		Frame h = hall(s, c);
		for (int side = -1; side <= 1; side += 2) {
			slots.add(new Slot("door" + (side + 1), ModEntities.HOLY_REMNANT, h.at(11, side * 4, 1), realm.guardianRank(Stage.LATE), false));
			slots.add(new Slot("wing" + (side + 1), ModEntities.HOLY_REMNANT, h.at(-4, side * 13, 1), realm.guardianRank(Stage.MID), false));
		}
		int k = 0;
		for (FallenHolyLand.Shard shard : ruins(s)) {
			BlockPos at = shardMiddle(s, c, shard).offset(2, 1, -2);
			slots.add(new Slot("ruin" + k++, ModEntities.HOLY_REMNANT, at, realm.guardianRank(shard.r() >= 11 ? Stage.MID : Stage.EARLY), false));
		}
		return slots;
	}

	/** The heart shard holds: the closing quake splits only the outer shards. */
	@Override
	protected int calm() { return FallenHolyLand.HEART_R + 6; }

	/** While the formation stands: the ancestral hall holds, and an eye with its guard near it. */
	@Override
	protected String holds(ServerLevel level, SecretRealms.Record r, BlockPos pos) {
		if (fallen(r)) return null;
		FallenHolyLand.Shape s = FallenHolyLand.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		if (ofHall(hall(s, c).local(pos))) return ModLang.MSG_HOLY_HALL_HOLDS;
		if (eyeAt(s, c, pos) >= 0 && eyeGuarded(level, pos)) return ModLang.MSG_HOLY_EYE_GUARDED;
		return null;
	}

	/** Whether the formation has fallen in this opening (all its eyes broken). */
	static boolean fallen(SecretRealms.Record r) {
		return r.works.getInt("Broken") >= EYES;
	}

	/** The hall's door lets everyone by once the formation has fallen. */
	@Override
	protected boolean barrierPasses(SecretRealms.Record r, UUID id) {
		return fallen(r) || passed(r, id);
	}

	/** Whether a guard of the holy land stands near the eye at {@code pos} (both sides: its guard is seen on the client too). */
	static boolean eyeGuarded(Level level, BlockPos pos) {
		return !level.getEntitiesOfClass(RealmGuardian.class, new AABB(pos).inflate(EYE_GUARD_REACH),
				g -> g.isAlive() && g.kind() == RealmGuardian.Kind.HOLY_REMNANT).isEmpty();
	}

	/** {@code by} (or a rival, null) broke the eye at {@code pos}: counted; the fourth, and the formation falls. */
	static void eyeBroken(ServerLevel level, BlockPos pos, Player by) {
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm != SecretRealm.FALLEN_HOLY_LAND) return;
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		SecretRealms.Record r = data.of(realm);
		int i = eyeAt(FallenHolyLand.shape(level.getSeed()), realm.centre(), pos);
		ListTag broken = r.works.getList("BrokenEyes", Tag.TAG_INT);
		if (i < 0 || r.openedAt < 0 || broken.contains(IntTag.valueOf(i))) return;
		broken.add(IntTag.valueOf(i));
		r.works.put("BrokenEyes", broken);
		r.works.putInt("Broken", broken.size());
		data.setDirty();
		level.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 2.0f, 0.5f);
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 50, 0.6, 0.6, 0.6, 0.12);
		tellInside(level, realm, Component.translatable(ModLang.MSG_HOLY_EYE_BROKEN, broken.size(), EYES).withStyle(ChatFormatting.GOLD));
		if (broken.size() >= EYES) formationFalls(level, r);
	}

	/** All four eyes broken: the formation over the hall shatters, its door open to all (the realm bars it again before it next opens). */
	static void formationFalls(ServerLevel level, SecretRealms.Record r) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(level.getSeed());
		Frame h = hall(s, r.realm.centre());
		for (BlockPos p : doorway(h)) if (level.isLoaded(p) && level.getBlockState(p).is(ModBlocks.TRIAL_BARRIER)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		Vec3 mid = Vec3.atCenterOf(h.origin().above(6));
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, mid.x, mid.y, mid.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD, mid.x, mid.y, mid.z, 160, 10, 6, 10, 0.2);
		level.playSound(null, h.origin(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 4.0f, 0.4f);
		level.playSound(null, h.origin(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 4.0f, 0.6f);
		tellInside(level, r.realm, Component.translatable(ModLang.MSG_HOLY_FORMATION_FALLS).withStyle(ChatFormatting.GOLD));
		for (ServerPlayer p : inside(level, r.realm)) SecretRealms.HUD_SHOWN.remove(p.getUUID()); // their row (and the barrier's word) sent again
	}

	// --- During an opening ---

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("TideAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Tide", 0);
		r.works.putInt("Broken", 0);
		r.works.remove("BrokenEyes");
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		boolean tide = tide(level, s, c, r, now, inside, data);
		currents(level, s, c, now, inside, tide);
		if (!fallen(r) && now % 5 == 0) shell(level, s, c, inside);
		if (now % 20 == 11) {
			// The trial told to whoever comes to the heart shard; the legacy to one at the altar once the formation has fallen.
			BlockPos altar = altar(hall(s, c));
			ListTag told = r.works.getList("Told", Tag.TAG_STRING);
			for (ServerPlayer p : inside) {
				if (p.blockPosition().distSqr(c) < 60 * 60 && !told.contains(StringTag.valueOf(p.getStringUUID()))) {
					told.add(StringTag.valueOf(p.getStringUUID()));
					p.sendSystemMessage(Component.translatable(ModLang.MSG_HOLY_TRIALS, Component.translatable(r.inheritor.isEmpty() ? ModLang.MSG_HOLY_GREAT
							: ModLang.MSG_HOLY_LESSER)).withStyle(ChatFormatting.GOLD));
				}
				if (p.blockPosition().distSqr(altar) < 3 * 3 && fallen(r) && !passed(r, "Claimed", p.getUUID())) {
					pass(r, "Passed", p.getUUID());
					pass(r, "Claimed", p.getUUID());
					inherit(level, r, p);
				}
			}
			r.works.put("Told", told);
		}
	}

	/** The formation over the hall, seen: its gold light drawn over a dome about it, while it stands. */
	private static void shell(ServerLevel level, FallenHolyLand.Shape s, BlockPos c, List<ServerPlayer> inside) {
		Vec3 mid = Vec3.atBottomCenterOf(hall(s, c).origin());
		boolean near = false;
		for (ServerPlayer p : inside) near |= p.position().distanceToSqr(mid) < 96 * 96;
		if (!near) return;
		DustParticleOptions gold = new DustParticleOptions(new Vector3f(1.0f, 0.82f, 0.36f), 1.6f);
		RandomSource random = level.getRandom();
		for (int k = 0; k < 18; k++) {
			double a = random.nextDouble() * Math.PI * 2, up = Math.acos(random.nextDouble());
			double r = 18;
			level.sendParticles(gold, mid.x + Math.cos(a) * Math.sin(up) * r, mid.y + Math.cos(up) * r, mid.z + Math.sin(a) * Math.sin(up) * r, 1, 0, 0, 0, 0);
		}
	}

	/**
	 * The currents: drawn flowing (where anyone is near), and those who step into one borne along it to its far end, faster in the tide.
	 * Players only (the rivals fly over the void, held at the shards' level).
	 */
	private static void currents(ServerLevel level, FallenHolyLand.Shape s, BlockPos c, long now, List<ServerPlayer> inside, boolean tide) {
		List<Current> all = currents(s, c);
		RandomSource random = level.getRandom();
		double speed = CURRENT_SPEED * (tide ? TIDE_SURGE : 1);
		for (Current current : all) {
			Vec3 way = current.way();
			double len = current.to().distanceTo(current.from());
			if (now % 4 == 0) {
				boolean near = false;
				Vec3 mid = current.from().add(current.to()).scale(0.5);
				for (ServerPlayer p : inside) near |= p.position().distanceToSqr(mid) < (len / 2 + 48) * (len / 2 + 48);
				if (near) {
					for (int k = 0; k < (tide ? 6 : 2); k++) {
						Vec3 at = current.from().add(way.scale(random.nextDouble() * len));
						level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 0, way.x, way.y, way.z, speed * 0.6);
					}
				}
			}
			for (ServerPlayer p : inside) {
				if (p.isSpectator() || p.getAbilities().flying && p.isCreative()) continue;
				Vec3 body = p.position().add(0, 0.9, 0);
				Vec3 rel = body.subtract(current.from());
				double t = rel.dot(way);
				if (t < 0 || t > len - 1.5) continue;
				if (rel.subtract(way.scale(t)).lengthSqr() > CURRENT_REACH * CURRENT_REACH) continue;
				Vec3 toLine = current.from().add(way.scale(t)).subtract(body).scale(0.2);
				p.setDeltaMovement(way.scale(speed).add(toLine).add(0, 0.04, 0));
				p.hurtMarked = true;
				p.fallDistance = 0;
			}
		}
	}

	/** The void tide: the currents surge for a minute, and whoever stands near a shard's edge is flung off it. @return whether it runs now */
	private static boolean tide(ServerLevel level, FallenHolyLand.Shape s, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int tide = r.works.getInt("Tide");
		if (tide == 0 && now >= r.works.getLong("TideAt")) {
			r.works.putInt("Tide", 1);
			r.works.putLong("TideEnds", now + TIDE_TICKS);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_HOLY_TIDE).withStyle(ChatFormatting.DARK_PURPLE));
			return true;
		}
		if (tide != 1) return false;
		if (now >= r.works.getLong("TideEnds")) {
			r.works.putInt("Tide", 2);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_HOLY_TIDE_ENDS).withStyle(ChatFormatting.GRAY));
			return false;
		}
		if (now % TIDE_FLING_EVERY == 0) {
			for (ServerPlayer p : inside) {
				if (p.isCreative() || p.isSpectator() || !p.onGround()) continue;
				FallenHolyLand.Column here = FallenHolyLand.column(s, p.getBlockX() - c.getX(), p.getBlockZ() - c.getZ());
				if (here.shard() < 0) continue;
				FallenHolyLand.Shard shard = s.shards.get(here.shard());
				double ox = p.getX() - (c.getX() + 0.5 + shard.x()), oz = p.getZ() - (c.getZ() + 0.5 + shard.z()), d = Math.max(0.5, Math.hypot(ox, oz));
				int bx = Mth.floor(p.getX() + ox / d * 3), bz = Mth.floor(p.getZ() + oz / d * 3);
				if (FallenHolyLand.column(s, bx - c.getX(), bz - c.getZ()).kind() != FallenHolyLand.Kind.NONE) continue;
				p.setDeltaMovement(p.getDeltaMovement().add(ox / d * 1.3, 0.55, oz / d * 1.3));
				p.hurtMarked = true;
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
				level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.AMBIENT, 0.4f, 1.6f);
			}
		}
		return true;
	}

	// --- Its inheritance ---

	/** The holy land's ancestral sword: a flying sword of Heaven grade, Perfect, named for it. */
	static ItemStack ancestralSword() {
		ItemStack sword = Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.HEAVEN, Artifacts.Quality.PERFECT, null);
		sword.setHoverName(Component.translatable(ModLang.ITEM_ANCESTRAL_SWORD).withStyle(ChatFormatting.GOLD));
		return sword;
	}

	/** The Great Inheritance: the ancestral sword; supreme spirit stones from the altar. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(ancestralSword(), new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 4));
	}

	/** A lesser legacy: a Heaven-grade artifact of the holy land, spirit stones. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		return List.of(Artifacts.any(random, ArtifactGrade.HEAVEN, null), new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 1 + random.nextInt(2)));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_HOLY_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_HOLY_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_HOLY_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** On one of the ruin shards, the nearer the heart the later. */
	@Override
	protected BlockPos rivalStart(ServerLevel level, long seed, BlockPos c, double along, RandomSource random) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		FallenHolyLand.Shard best = null;
		double want = FallenHolyLand.ARRIVAL_R - along * (FallenHolyLand.ARRIVAL_R - FallenHolyLand.HEART_R - 30), d = Double.MAX_VALUE;
		for (FallenHolyLand.Shard shard : s.shards) {
			if (shard.role().equals("heart")) continue;
			double here = Math.abs(Math.hypot(shard.x(), shard.z()) - want) + random.nextDouble() * 12;
			if (here < d) {
				d = here;
				best = shard;
			}
		}
		return c.offset(best.x() + random.nextInt(3) - 1, 0, best.z() + random.nextInt(3) - 1);
	}

	@Override
	protected List<BlockPos> vaults(long seed, BlockPos c) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(seed);
		List<BlockPos> out = new ArrayList<>();
		List<FallenHolyLand.Shard> ruins = ruins(s);
		for (int k = 0; k < Math.min(2, ruins.size()); k++) out.add(vaultAt(s, c, ruins.get(k)));
		return out;
	}

	/** The nearest whole eye while the formation stands; then the hall's door; the altar. */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		FallenHolyLand.Shape s = FallenHolyLand.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		if (passed(r, "Claimed", npc.getUUID())) return null;
		if (!fallen(r)) {
			BlockPos best = null;
			double d = Double.MAX_VALUE;
			ListTag broken = r.works.getList("BrokenEyes", Tag.TAG_INT);
			for (int i = 0; i < EYES; i++) {
				if (broken.contains(IntTag.valueOf(i))) continue;
				BlockPos at = eye(s, c, i);
				double here = at.distSqr(npc.blockPosition());
				if (here < d) {
					d = here;
					best = at;
				}
			}
			return best == null ? null : new Trial("eye", best.above(), 3.0, false);
		}
		Frame h = hall(s, c);
		if (!inHall(h.local(npc.blockPosition()))) return new Trial("door", h.at(11, 0, 1), 3.0, false);
		return new Trial("altar", altar(h), 2.0, true);
	}

	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		switch (trial.step()) {
			case "eye" -> {
				BlockPos eye = trial.mark().below();
				if (!level.getBlockState(eye).is(ModBlocks.FORMATION_EYE)) return true;
				// Its guard first: it fights whoever keeps it.
				if (eyeGuarded(level, eye)) {
					List<RealmGuardian> guards = level.getEntitiesOfClass(RealmGuardian.class, new AABB(eye).inflate(EYE_GUARD_REACH), RealmGuardian::isAlive);
					if (!guards.isEmpty() && npc.getTarget() == null) npc.setTarget(guards.get(0));
					return working > 2400; // gives up on it after a long while (it will look for the nearest again)
				}
				if (working % 10 == 0) level.sendParticles(ParticleTypes.CRIT, eye.getX() + 0.5, eye.getY() + 0.5, eye.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.1);
				if (working < 100) return false;
				level.setBlock(eye, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				eyeBroken(level, eye, null);
			}
			case "door" -> {
				BlockPos in = hall(FallenHolyLand.shape(level.getSeed()), r.realm.centre()).at(5, 0, 1);
				npc.getNavigation().stop();
				npc.setQiFlying(false);
				npc.teleportTo(in.getX() + 0.5, in.getY(), in.getZ() + 0.5);
			}
			case "altar" -> {
				pass(r, "Passed", npc.getUUID());
				pass(r, "Claimed", npc.getUUID());
				rivalInherits(level, r, npc);
			}
			default -> {
			}
		}
		return true;
	}

	private FallenHolyLandWorks() {}
}
