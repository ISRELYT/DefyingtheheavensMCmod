package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * How much a meditator's surroundings speed up cultivation. Each source adds a share on top of the normal rate
 * (+0.20 means 20% faster), and the shares add up:
 * <ul>
 * <li>sitting on a Meditation Mat (+10%) or a Red Silk Meditation Mat (+20%);</li>
 * <li>Spirit Pedestals bearing Cultivation Fruit nearby: older fruit gives more, 2% to 50% (see {@link #pedestalBonus}); only the
 * {@link #MAX_PEDESTALS} strongest count;</li>
 * <li>height, under open sky: from y {@link #HEIGHT_START}, rising to +15% at y {@link #HEIGHT_FULL};</li>
 * <li>tranquillity: a cherry grove, the Peach Blossom Sanctuary or snow-capped peaks (+10%).</li>
 * </ul>
 * Everything here reads only blocks, block entities, biome and sky, which the client knows too, so the client works out
 * the same boost for the menu and the meditation visuals without any extra packets.
 */
public final class CultivationBoost {
	public static final double MAT = 0.10;
	public static final double RED_MAT = 0.20;

	/** Pedestals count within this many blocks of the meditator sideways, and {@link #PEDESTAL_REACH_Y} up or down. */
	public static final int PEDESTAL_REACH = 4;
	public static final int PEDESTAL_REACH_Y = 2;
	public static final int MAX_PEDESTALS = 4;
	/**
	 * A pedestal's share: a 1-year fruit gives 2% and a 10,000-year fruit 50%, rising evenly with each tenfold in age
	 * (10 years 14%, 100 years 26%, 1,000 years 38%).
	 */
	public static final double PEDESTAL_BASE = 0.02;
	public static final double PEDESTAL_PER_TENFOLD = 0.12;

	public static final int HEIGHT_START = 120;
	public static final int HEIGHT_FULL = 250;
	public static final double HEIGHT_MAX = 0.15;

	public static final double TRANQUIL = 0.10;
	/** Calm places: blossom groves and the tops of snowy mountains. */
	private static final Set<ResourceKey<Biome>> TRANQUIL_BIOMES = Set.of(Biomes.CHERRY_GROVE, ModBiomes.PEACH_BLOSSOM_SANCTUARY,
			Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.SNOWY_SLOPES);

	/** What a meditator at one spot gets, source by source. */
	public record Breakdown(Block mat, double matBonus, List<SpiritPedestalBlockEntity> pedestals, double pedestalBonus,
			double heightBonus, double tranquilBonus) {
		public static final Breakdown NONE = new Breakdown(null, 0, List.of(), 0, 0, 0);

		public double total() { return matBonus + pedestalBonus + heightBonus + tranquilBonus; }

		/** Multiplies the normal meditation rate. */
		public double multiplier() { return 1.0 + total(); }

		/** The boost as a whole percentage, for messages and the menu. */
		public int percent() { return (int) Math.round(total() * 100); }
	}

	/** One fruit's share on a pedestal (0 for an empty pedestal). */
	public static double pedestalBonus(int years) {
		return years <= 0 ? 0 : PEDESTAL_BASE + PEDESTAL_PER_TENFOLD * Math.log10(FruitAge.clamp(years));
	}

	public static Breakdown of(Player player) {
		Level level = player.level();
		BlockPos feet = player.blockPosition();
		double y = player.getY();
		// A soul in the Inner Realm is still fed by what surrounds its body.
		if (player instanceof net.minecraft.server.level.ServerPlayer sp && InnerRealm.isInside(sp)) {
			net.minecraft.nbt.CompoundTag back = CultivationManager.get(sp).getInnerReturn();
			Level bodyLevel = sp.server.getLevel(InnerRealm.bodyDimension(sp));
			if (bodyLevel != null) {
				level = bodyLevel;
				y = back.getDouble("Y");
				feet = BlockPos.containing(back.getDouble("X"), y, back.getDouble("Z"));
			}
		}

		// The mat's block is where a seated player's feet are (they sit a pixel or two above its floor).
		Block mat = null;
		double matBonus = 0;
		Block at = level.getBlockState(feet).getBlock();
		if (at == ModBlocks.RED_MEDITATION_MAT) {
			mat = at;
			matBonus = RED_MAT;
		} else if (at == ModBlocks.MEDITATION_MAT) {
			mat = at;
			matBonus = MAT;
		}

		List<SpiritPedestalBlockEntity> pedestals = pedestalsAround(level, feet);
		double pedestalBonus = 0;
		for (SpiritPedestalBlockEntity pedestal : pedestals) pedestalBonus += pedestalBonus(pedestal.fruitAge());

		double heightBonus = 0;
		if (level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() && y > HEIGHT_START
				&& level.canSeeSky(feet.above())) {
			heightBonus = HEIGHT_MAX * Math.min(1.0, (y - HEIGHT_START) / (HEIGHT_FULL - HEIGHT_START));
		}

		Holder<Biome> biome = level.getBiome(feet);
		double tranquilBonus = TRANQUIL_BIOMES.stream().anyMatch(key -> biome.is(key)) ? TRANQUIL : 0;

		return new Breakdown(mat, matBonus, pedestals, pedestalBonus, heightBonus, tranquilBonus);
	}

	/**
	 * The fruit-bearing pedestals that feed someone meditating at {@code feet}: oldest fruit first, at most
	 * {@link #MAX_PEDESTALS}. Ties go by position so server and client always pick the same ones.
	 */
	public static List<SpiritPedestalBlockEntity> pedestalsAround(Level level, BlockPos feet) {
		List<SpiritPedestalBlockEntity> found = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-PEDESTAL_REACH, -PEDESTAL_REACH_Y, -PEDESTAL_REACH),
				feet.offset(PEDESTAL_REACH, PEDESTAL_REACH_Y, PEDESTAL_REACH))) {
			if (level.getBlockEntity(pos) instanceof SpiritPedestalBlockEntity pedestal && pedestal.hasFruit()) {
				found.add(pedestal);
			}
		}
		found.sort(Comparator.comparingInt((SpiritPedestalBlockEntity p) -> p.fruitAge()).reversed()
				.thenComparingLong(p -> p.getBlockPos().asLong()));
		return found.size() > MAX_PEDESTALS ? List.copyOf(found.subList(0, MAX_PEDESTALS)) : found;
	}

	private CultivationBoost() {}
}
