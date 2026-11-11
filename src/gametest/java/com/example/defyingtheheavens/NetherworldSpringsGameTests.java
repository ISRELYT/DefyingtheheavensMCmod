package com.example.defyingtheheavens;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Netherworld's springs (2026-11-11, NetherworldSpringFeature): one forms only where its way out hangs over the abyss, so its water
 * falls into the dark. Before, one that opened onto the land ran down its slope when its chunk first came into reach and washed the
 * plants in its way into items (316 in 245 chunks of the Fields of the Other Shore, where the spider lilies grow thickest).
 */
public class NetherworldSpringsGameTests implements FabricGameTest {
	/** Fresh Fields land looked over: this many places, each this many chunks out from its middle kept ticking. */
	private static final int AREAS = 4, RADIUS = 2, SETTLE_TICKS = 400;
	/** Holds the land in reach while it is looked over; never saved, so a test cut short leaves nothing held. */
	private static final TicketType<ChunkPos> HELD = TicketType.create("dth_netherworld_springs", Comparator.comparingLong(ChunkPos::toLong));
	private static boolean listening;
	private static volatile Set<Long> watched = Set.of();
	private static final AtomicInteger WASHED = new AtomicInteger();
	private static final List<String> SEEN = Collections.synchronizedList(new ArrayList<>());

	@GameTest(template = EMPTY_STRUCTURE, batch = "netherworld_springs_rule", timeoutTicks = 400)
	public void aSpringFormsOnlyOverTheAbyss(GameTestHelper helper) {
		ServerLevel nw = helper.getLevel().getServer().getLevel(ModDimensions.NETHERWORLD);
		helper.assertTrue(nw != null, "The Netherworld is there");
		ChunkGenerator generator = nw.getChunkSource().getGenerator();
		// A chasm far out: a column with no land in it at all.
		BlockPos chasm = null;
		for (int i = 0; i < 400 && chasm == null; i++) {
			int x = 1_200_000 + i * 32, z = 1_200_000;
			if (generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, nw, nw.getChunkSource().randomState()) <= nw.getMinBuildHeight()) {
				chasm = new BlockPos(x, 150, z);
			}
		}
		helper.assertTrue(chasm != null, "A chasm is found");
		ConfiguredFeature<?, ?> spring = nw.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(ModConfiguredFeatures.NW_SPRING_WATER);
		// A spring's place in the rock beside it: rock above, below and on three sides, its one way out over the chasm.
		BlockPos at = chasm.east(), ledge = chasm.below(40);
		List<BlockPos> rock = List.of(at.above(), at.below(), at.east(), at.north(), at.south());
		Map<BlockPos, BlockState> before = new LinkedHashMap<>();
		for (BlockPos pos : List.of(at, chasm, ledge, at.above(), at.below(), at.east(), at.north(), at.south())) before.put(pos, nw.getBlockState(pos));
		BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
		try {
			for (BlockPos pos : rock) nw.setBlock(pos, deepslate, 2);
			nw.setBlock(at, air, 2);
			helper.assertTrue(NetherworldSpringFeature.overTheAbyss(nw, chasm), "Nothing lies beneath its way out, down to the bottom of the world");
			// Ground some way down beneath its way out: no spring, or it would run over the land.
			nw.setBlock(ledge, deepslate, 2);
			helper.assertTrue(!NetherworldSpringFeature.overTheAbyss(nw, chasm), "Ground 40 blocks beneath it is found");
			helper.assertTrue(!spring.place(nw, generator, RandomSource.create(1), at) && nw.getBlockState(at).isAir(),
					"A spring whose water would land on the ground beneath doesn't form");
			// Over the abyss it forms, as vanilla's spring would.
			nw.setBlock(ledge, air, 2);
			helper.assertTrue(spring.place(nw, generator, RandomSource.create(1), at) && nw.getFluidState(at).isSource(),
					"Over the abyss the spring forms");
			// More than one way out: none.
			nw.setBlock(at, air, 2);
			nw.setBlock(at.north(), air, 2);
			helper.assertTrue(!spring.place(nw, generator, RandomSource.create(1), at) && nw.getBlockState(at).isAir(), "Two ways out: no spring");
		} finally {
			before.forEach((pos, state) -> nw.setBlock(pos, state, 2));
		}
		helper.succeed();
	}

	/** Long in ticks: the test server runs its ticks as fast as it can while the land is made in real time (6,000 went by in 4 s). */
	@GameTest(template = EMPTY_STRUCTURE, batch = "netherworld_springs_fields", timeoutTicks = 40000)
	public void freshFieldsWashNoPlantsAway(GameTestHelper helper) {
		listen();
		ServerLevel nw = helper.getLevel().getServer().getLevel(ModDimensions.NETHERWORLD);
		helper.assertTrue(nw != null, "The Netherworld is there");
		// Land nobody has seen, far out, in the Fields of the Other Shore.
		ThreadLocalRandom random = ThreadLocalRandom.current();
		List<ChunkPos> middles = new ArrayList<>();
		Set<Long> chunks = new HashSet<>();
		for (int i = 0; i < AREAS; i++) {
			int x = (random.nextBoolean() ? 1 : -1) * random.nextInt(400_000, 3_000_000), z = (random.nextBoolean() ? 1 : -1) * random.nextInt(400_000, 3_000_000);
			Pair<BlockPos, Holder<Biome>> fields = nw.findClosestBiome3d(b -> b.is(ModBiomes.FIELDS_OF_THE_OTHER_SHORE), new BlockPos(x, 100, z), 3000, 32, 64);
			if (fields == null) continue;
			ChunkPos middle = new ChunkPos(fields.getFirst());
			middles.add(middle);
			for (int cx = -RADIUS; cx <= RADIUS; cx++) {
				for (int cz = -RADIUS; cz <= RADIUS; cz++) chunks.add(ChunkPos.asLong(middle.x + cx, middle.z + cz));
			}
		}
		helper.assertTrue(!middles.isEmpty(), "Some of the Fields are found");
		WASHED.set(0);
		SEEN.clear();
		watched = Set.copyOf(chunks);
		// Made here and now (its water is still: it runs only once its chunk ticks), so the wait below is short.
		for (long chunk : chunks) nw.getChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
		// Each place held so that its middle and RADIUS chunks about it tick, entities and all (ticket level 31 at RADIUS out).
		for (ChunkPos middle : middles) nw.getChunkSource().addRegionTicket(HELD, middle, RADIUS + 2, middle);
		Runnable letGo = () -> {
			watched = Set.of();
			for (ChunkPos middle : middles) nw.getChunkSource().removeRegionTicket(HELD, middle, RADIUS + 2, middle);
		};
		helper.startSequence()
				.thenWaitUntil(() -> {
					for (long chunk : chunks) {
						if (!nw.isPositionEntityTicking(new ChunkPos(chunk).getMiddleBlockPosition(100)) || !nw.areEntitiesLoaded(chunk)) {
							throw new GameTestAssertException("Waiting for the land to come into reach");
						}
					}
				})
				// All in reach and ticking: time for any running water to run its course.
				.thenIdle(SETTLE_TICKS)
				.thenExecute(() -> {
					int overLand;
					try {
						overLand = springsOverLand(nw, chunks);
					} finally {
						letGo.run();
					}
					helper.assertTrue(WASHED.get() == 0 && overLand == 0, "In " + chunks.size() + " fresh chunks of the Fields " + WASHED.get()
							+ " plants were washed into items " + SEEN + " and " + overLand + " springs run over the land");
				})
				.thenSucceed();
	}

	/** Counts the springs in these chunks whose water runs onto the land (not straight down into the abyss). */
	private static int springsOverLand(ServerLevel nw, Set<Long> chunks) {
		int count = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (long chunk : chunks) {
			ChunkPos c = new ChunkPos(chunk);
			for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x++) {
				for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z++) {
					for (int y = 25; y <= 215; y++) {
						FluidState water = nw.getFluidState(pos.set(x, y, z));
						if (!water.isSource() || !water.is(Fluids.WATER)) continue;
						// A spring: set in the rock above and below it, with one way out.
						if (!rock(nw.getBlockState(pos.above())) || !rock(nw.getBlockState(pos.below()))) continue;
						BlockPos way = null;
						int ways = 0;
						for (Direction side : Direction.Plane.HORIZONTAL) {
							BlockPos next = pos.relative(side);
							if (!rock(nw.getBlockState(next))) {
								ways++;
								way = next;
							}
						}
						if (ways == 1 && !fallsClear(nw, way)) count++;
					}
				}
			}
		}
		return count;
	}

	private static boolean rock(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty();
	}

	/** Nothing but air or falling water beneath it, down to the bottom of the world. */
	private static boolean fallsClear(ServerLevel nw, BlockPos way) {
		BlockPos.MutableBlockPos below = way.mutable();
		for (int y = way.getY() - 1; y >= nw.getMinBuildHeight(); y--) {
			if (rock(nw.getBlockState(below.setY(y)))) return false;
		}
		return true;
	}

	/** Notes every plant (the flowers and roots, the herbs) that turns up as an item in the land being looked over. */
	private static void listen() {
		if (listening) return;
		listening = true;
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof ItemEntity item) || level.dimension() != ModDimensions.NETHERWORLD) return;
			if (!watched.contains(ChunkPos.asLong(item.blockPosition()))) return;
			if (!(item.getItem().getItem() instanceof BlockItem block) || !(block.getBlock() instanceof BushBlock || block.getBlock() instanceof GinsengBlock)) return;
			WASHED.incrementAndGet();
			if (SEEN.size() < 6) SEEN.add(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath() + " at " + item.blockPosition().toShortString());
		});
	}
}
