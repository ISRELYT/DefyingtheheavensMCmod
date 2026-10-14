package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A Dao Stele standing on a small worn plinth of stone bricks, its heart-stone of its element, in the Overworld and the Upper
 * Realm: its element drawn by the land's own (QiElement#pickOfFive: forests carve wood, seas water, deserts fire...), its realm by
 * rarity ({@link #WEIGHTS}): the Overworld carves mostly Qi Refining and Foundation Building methods, Core Formation's now and then
 * and Nascent Soul's seldom; its rare lands (cherry groves, mushroom isles, ice spikes, eroded badlands, the deep jungles) the
 * deeper ones more often. Heavenly Being methods are carved almost only in the Upper Realm, and Four Axis methods there alone, the
 * most in its own wondrous lands (the Dense Qi Peaks, the Thunder Peaks, the Ancient Sword Graveyard, the Spirit Forest, the
 * Peach Blossom Sanctuary).
 */
public class DaoSteleFeature extends Feature<NoneFeatureConfiguration> {
	/** No other stele within this many blocks across and this many up or down. */
	private static final int SPACING = 12, SPACING_HEIGHT = 8;

	/** Where a stele stands, for its realm's rarity. */
	public enum Lands { OVERWORLD, OVERWORLD_RARE, UPPER_REALM, UPPER_REALM_RARE }

	/** Each kind of land's weights for a stele's realm, Qi Refining to Four Axis. */
	static final int[][] WEIGHTS = {
			{600, 250, 60, 8, 0, 0},
			{300, 250, 150, 40, 3, 0},
			{0, 40, 150, 100, 40, 10},
			{0, 0, 100, 100, 60, 25},
	};

	public DaoSteleFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos foot = context.origin();
		RandomSource random = context.random();
		if (!level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)) return false;
		if (!level.getFluidState(foot).isEmpty() || !level.getFluidState(foot.below()).isEmpty()) return false;
		for (int dy = 0; dy < 4; dy++) {
			if (!level.getBlockState(foot.above(dy)).canBeReplaced()) return false;
		}
		for (BlockPos near : BlockPos.betweenClosed(foot.offset(-SPACING, -SPACING_HEIGHT, -SPACING), foot.offset(SPACING, SPACING_HEIGHT, SPACING))) {
			if (level.getBlockState(near).is(ModBlocks.DAO_STELE)) return false;
		}
		Holder<Biome> biome = level.getBiome(foot);
		QiElement element = QiElement.pickOfFive(biome, false, random);
		HeavenMethod chosen = HeavenMethod.of(element, pickRealm(random, lands(biome)));
		// The plinth: three by three, a few bricks missing or mossed with age, the element's stone at its heart.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos at = foot.offset(dx, -1, dz);
				if (random.nextInt(6) == 0) continue;
				level.setBlock(at, (random.nextInt(4) == 0 ? Blocks.MOSSY_STONE_BRICKS : random.nextInt(3) == 0 ? Blocks.CRACKED_STONE_BRICKS
						: Blocks.STONE_BRICKS).defaultBlockState(), 2);
			}
		}
		level.setBlock(foot.below(), heartStone(element).defaultBlockState(), 2);
		DaoSteleBlock.place(level, foot, chosen, Direction.Plane.HORIZONTAL.getRandomDirection(random));
		return true;
	}

	/** The plinth's heart-stone for each element. */
	static Block heartStone(QiElement element) {
		return switch (element) {
			case WOOD -> Blocks.MOSS_BLOCK;
			case WATER -> Blocks.PRISMARINE_BRICKS;
			case FIRE -> Blocks.RED_NETHER_BRICKS;
			case EARTH -> Blocks.POLISHED_GRANITE;
			default -> Blocks.CHISELED_QUARTZ_BLOCK;
		};
	}

	/** What kind of land {@code biome} is, for its steles' realms. */
	static Lands lands(Holder<Biome> biome) {
		if (biome.is(ModTags.IS_UPPER_REALM)) {
			return biome.is(ModBiomes.DENSE_QI_PEAKS) || biome.is(ModBiomes.THUNDER_PEAKS) || biome.is(ModBiomes.ANCIENT_SWORD_GRAVEYARD)
					|| biome.is(ModBiomes.SPIRIT_FOREST) || biome.is(ModBiomes.PEACH_BLOSSOM_SANCTUARY) ? Lands.UPPER_REALM_RARE : Lands.UPPER_REALM;
		}
		return biome.is(Biomes.CHERRY_GROVE) || biome.is(Biomes.MUSHROOM_FIELDS) || biome.is(Biomes.ICE_SPIKES) || biome.is(Biomes.ERODED_BADLANDS)
				|| biome.is(Biomes.BAMBOO_JUNGLE) || biome.is(Biomes.FLOWER_FOREST) ? Lands.OVERWORLD_RARE : Lands.OVERWORLD;
	}

	/** A stele's realm in {@code lands}, by its weights. */
	static Realm pickRealm(RandomSource random, Lands lands) {
		int[] weights = WEIGHTS[lands.ordinal()];
		int total = 0;
		for (int w : weights) total += w;
		int roll = random.nextInt(total);
		for (Realm realm : Realm.values()) {
			roll -= weights[realm.ordinal()];
			if (roll < 0) return realm;
		}
		return Realm.QI_REFINING;
	}
}
