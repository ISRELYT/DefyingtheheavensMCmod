package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Sunken Sword Tomb's works (SecretRealm#SUNKEN_SWORD_TOMB; its land is SwordTomb's, the machinery every realm shares RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: an arrival ledge at each branch canyon's head (old paving, lanterns, a sword set in a stone); the Sword Path's gate at
 *   its foot; on the spire's summit the Sleeping Sword's shrine, a pavilion over the stone it was driven into; on the gorge's floor the ruins of
 *   the sword sect that fell here ({@link Ruin}: its Sword Hall, its Forge, its Hall of Graves, its Quarters), each with a vault.</li>
 *   <li><b>Renewed each time it stirs</b>: its old swords ({@link #RELICS} of the swords driven into its floor glint again, each keeping a
 *   blade, a manual or spirit stones: {@link #drawn}); its vaults sealed anew for the ancients and filled; its killing formations of blades;
 *   the bones of a few earlier visitors.</li>
 *   <li><b>Its guardians</b>: Sword Wraiths about the gorge and in the ruins, one larger at the Sword Path's foot, and on the summit the
 *   Sleeping Sword's own spirit, its duelist.</li>
 *   <li><b>Its danger</b>: lingering sword intent in patches on the floor ({@link #ZONES}, glinting faintly, plain under Qi Sense) cuts
 *   whoever runs, flies or fights in it; walking or sneaking through is safe, and one whose Sword Intent is lit is spared.</li>
 *   <li><b>Its event</b>: some time into each opening the swords wake ({@link #WAKE_TICKS}): blades rise from the gorge and fall like rain
 *   about everyone inside, a glint marking each spot a moment before.</li>
 *   <li><b>Its trials</b>: walk the Sword Path from its foot to the summit on foot, its pressure heavier with every step up (fliers dragged
 *   down onto it; leaving it, one begins again); then best the sword's spirit, matched to oneself; then draw the Sleeping Sword from its
 *   stone: the Great Inheritance (the Sleeping Sword and the Sword Intent manual) once in a world, after it a lesser legacy each opening.</li>
 * </ul>
 */
public final class SwordTombWorks extends RealmWorks {
	static final SwordTombWorks WORKS = new SwordTombWorks();
	/** How many of its fallen swords are old swords each opening; its patches of lingering intent, and how wide each. */
	static final int RELICS = 18, ZONES = 16, ZONE_R = 4;
	/** The swords' wake lasts this long (a minute); one falls about each visitor this often (ticks). */
	static final int WAKE_TICKS = 1200, FALL_EVERY = 10;
	/** Shares of one's whole health: a cut of lingering intent, a fallen blade, the Sword Path's pressure at its top. */
	static final double INTENT_CUT = 0.05, BLADE_CUT = 0.12, PATH_CUT = 0.04;

	/** The sword sect's ruins on the gorge's floor, between the branch canyons: their vaults' tiers, and how many wraiths each. */
	enum Ruin {
		SWORD_HALL(StorageFormations.Tier.MID, 2), SWORD_FORGE(StorageFormations.Tier.LOW, 2), HALL_OF_GRAVES(StorageFormations.Tier.LOW, 2),
		QUARTERS(StorageFormations.Tier.LOW, 1);

		final StorageFormations.Tier vault;
		final int wraiths;

		Ruin(StorageFormations.Tier vault, int wraiths) {
			this.vault = vault;
			this.wraiths = wraiths;
		}
	}

	// --- Where things are ---

	/** The main way out from the middle (the first branch's), as a direction. */
	static Direction mainWay(SwordTomb.Shape s) {
		return Direction.fromDelta((int) Math.round(Math.cos(s.mainTurn)), 0, (int) Math.round(Math.sin(s.mainTurn)));
	}

	/** A ruin's frame on the gorge's floor: between branch {@code ruin} and the next, half way across the ring, facing the spire. */
	static Frame ruinFrame(SwordTomb.Shape s, BlockPos c, Ruin ruin) {
		double b = s.branch(ruin.ordinal()) + Math.PI / SwordTomb.BRANCHES;
		double r = (SwordTomb.RING_IN + SwordTomb.RING_OUT) / 2.0 + 2;
		double cos = Math.cos(b), sin = Math.sin(b);
		int x = c.getX() + (int) Math.round(cos * r), z = c.getZ() + (int) Math.round(sin * r);
		Direction in = Math.abs(cos) > Math.abs(sin) ? (cos > 0 ? Direction.WEST : Direction.EAST) : (sin > 0 ? Direction.NORTH : Direction.SOUTH);
		return new Frame(new BlockPos(x, SwordTomb.FLOOR_Y + 1, z), in);
	}

	/** The summit's middle (the shrine's stone and the Sleeping Sword in it). */
	static BlockPos shrineSword(SwordTomb.Shape s, BlockPos c) {
		return c.atY(SwordTomb.SUMMIT_Y + 2);
	}

	/** Where the sword's spirit, its duelist, stands: before the shrine, on the main way's side. */
	static BlockPos duelPost(SwordTomb.Shape s, BlockPos c) {
		return c.relative(mainWay(s), 7).atY(SwordTomb.SUMMIT_Y + 1);
	}

	/** Each world's patches of lingering intent, worked out once. */
	private static final Map<Long, List<BlockPos>> ZONE_CACHE = java.util.Collections.synchronizedMap(new HashMap<>());

	/** Its patches of lingering intent: their middles on the gorge's floor (its own roll). */
	static List<BlockPos> zones(SwordTomb.Shape s, BlockPos c) {
		return ZONE_CACHE.computeIfAbsent(s.seed, seed -> layZones(s, c));
	}

	private static List<BlockPos> layZones(SwordTomb.Shape s, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		RandomSource random = RandomSource.create(s.seed ^ 0x2017E57L);
		for (int tries = 0; out.size() < ZONES && tries < 200; tries++) {
			double a = random.nextDouble() * Math.PI * 2, r = SwordTomb.RING_IN + 5 + random.nextDouble() * (SwordTomb.RING_OUT - SwordTomb.RING_IN - 10);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			if (SwordTomb.column(s, x, z).kind() != SwordTomb.Kind.FLOOR) continue;
			out.add(c.offset(x, 0, z).atY(SwordTomb.column(s, x, z).ground() + 1));
		}
		return out;
	}

	/** Spots on the floor where old swords may be found: the floor's own swords (SwordTombLand lays them), near the ring's middle. */
	static List<BlockPos> relicSpots(SwordTomb.Shape s, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		RandomSource random = RandomSource.create(s.seed ^ 0x2E11C5L);
		for (int tries = 0; out.size() < 60 && tries < 4000; tries++) {
			double a = random.nextDouble() * Math.PI * 2, r = SwordTomb.RING_IN + 2 + random.nextDouble() * (SwordTomb.RING_OUT - SwordTomb.RING_IN - 4);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			SwordTomb.Column col = SwordTomb.column(s, x, z);
			if (col.kind() != SwordTomb.Kind.FLOOR || !SwordTombLand.swordAt(c.getX() + x, c.getZ() + z, 0.07)) continue;
			BlockPos at = c.offset(x, 0, z).atY(col.ground() + 1);
			if (!out.contains(at)) out.add(at);
		}
		return out;
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		SwordTomb.Shape s = SwordTomb.shape(seed);
		steps.add(() -> arrivalLedges(level, s, c));
		steps.add(() -> pathGate(level, s, c));
		steps.add(() -> shrine(level, s, c));
		for (Ruin ruin : Ruin.values()) steps.add(() -> ruin(level, s, c, ruin, RandomSource.create(seed ^ ruin.ordinal() * 0x5A17L)));
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		SwordTomb.Shape s = SwordTomb.shape(seed);
		steps.add(() -> renewRelics(level, s, c, r));
		steps.add(() -> renewVaults(level, s, c));
		steps.add(() -> renewFormations(level, s, c));
		steps.add(() -> renewShrine(level, s, c));
	}

	/** At each branch's head: a floor of old paving, a lantern either side, a sword set upright in a block of stone at its back. */
	static void arrivalLedges(ServerLevel level, SwordTomb.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0xA771L);
		for (int k = 0; k < SwordTomb.BRANCHES; k++) {
			BlockPos local = SwordTomb.arrivals(s).get(k);
			BlockPos at = c.offset(local.getX(), 0, local.getZ()).atY(local.getY());
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			double b = s.branch(k);
			Direction out = Math.abs(Math.cos(b)) > Math.abs(Math.sin(b)) ? (Math.cos(b) > 0 ? Direction.EAST : Direction.WEST) : (Math.sin(b) > 0 ? Direction.SOUTH : Direction.NORTH);
			Frame f = new Frame(at, out);
			for (int a = -3; a <= 3; a++) {
				for (int d = -3; d <= 3; d++) {
					if (a * a + d * d > 12) continue;
					put(level, f.at(a, d, 0), oldStone(random));
					for (int y = 1; y <= 4; y++) put(level, f.at(a, d, y), Blocks.AIR.defaultBlockState());
				}
			}
			for (int d = -3; d <= 3; d += 6) {
				put(level, f.at(0, d, 1), Blocks.STONE_BRICK_WALL.defaultBlockState());
				put(level, f.at(0, d, 2), lantern(false));
			}
			put(level, f.at(3, 0, 1), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			put(level, f.at(3, 0, 2), ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.FACING, out));
			join(level, f.at(-4, -4, 0).offset(-1, 0, -1), f.at(4, 4, 4).offset(1, 0, 1));
		}
	}

	/** The Sword Path's gate at its foot: two pillars of old stone and a lintel over the first step, a sword hung beneath it. */
	static void pathGate(ServerLevel level, SwordTomb.Shape s, BlockPos c) {
		BlockPos foot = c.offset(SwordTomb.pathFoot(s).getX(), 0, SwordTomb.pathFoot(s).getZ()).atY(SwordTomb.pathFoot(s).getY());
		level.getChunk(foot.getX() >> 4, foot.getZ() >> 4);
		Direction out = Direction.getNearest(foot.getX() - c.getX(), 0, foot.getZ() - c.getZ());
		Frame f = new Frame(foot, out);
		RandomSource random = RandomSource.create(s.seed ^ 0x6A7EL);
		for (int side = -2; side <= 2; side += 4) {
			for (int y = 0; y <= 4; y++) put(level, f.at(0, side, y), y == 4 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : oldStone(random));
		}
		for (int side = -3; side <= 3; side++) put(level, f.at(0, side, 5), stairs(Blocks.STONE_BRICK_STAIRS, out.getClockWise(), true));
		for (int side = -1; side <= 1; side++) put(level, f.at(0, side, 5), Blocks.STONE_BRICKS.defaultBlockState());
		put(level, f.at(0, 0, 4), Blocks.CHAIN.defaultBlockState());
	}

	/**
	 * The Sleeping Sword's shrine on the summit: a platform of polished stone, four pillars of dark wood, a hipped roof of dark tiles, and at
	 * its middle the stone the Sleeping Sword was driven into, the sword in it ({@link #renewShrine}).
	 */
	static void shrine(ServerLevel level, SwordTomb.Shape s, BlockPos c) {
		BlockPos top = c.atY(SwordTomb.SUMMIT_Y);
		level.getChunk(top.getX() >> 4, top.getZ() >> 4);
		Frame f = new Frame(top, mainWay(s));
		for (int a = -6; a <= 6; a++) {
			for (int d = -6; d <= 6; d++) {
				put(level, f.at(a, d, 0), Math.abs(a) == 6 || Math.abs(d) == 6 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
				for (int y = 1; y <= 9; y++) put(level, f.at(a, d, y), Blocks.AIR.defaultBlockState());
			}
		}
		for (int a = -3; a <= 3; a += 6) {
			for (int d = -3; d <= 3; d += 6) for (int y = 1; y <= 4; y++) put(level, f.at(a, d, y), log(Blocks.STRIPPED_DARK_OAK_LOG));
		}
		HerbValleyWorks.hipRoof(level, f, -4, 4, -4, 4, 5);
		put(level, f.at(0, 0, 1), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		for (int a = -4; a <= 4; a += 8) put(level, f.at(a, 0, 1), lantern(false));
		join(level, f.at(-7, -7, 0), f.at(7, 7, 10));
	}

	/** One of the sect's ruins on the gorge's floor: a floor of old paving, broken walls, what each was for, a vault at its back. */
	static void ruin(ServerLevel level, SwordTomb.Shape s, BlockPos c, Ruin ruin, RandomSource random) {
		Frame f = ruinFrame(s, c, ruin);
		level.getChunk(f.origin().getX() >> 4, f.origin().getZ() >> 4);
		int floor = f.origin().getY() - 1;
		for (int a = -6; a <= 6; a++) {
			for (int d = -6; d <= 6; d++) {
				BlockPos p = f.at(a, d, 0).atY(floor);
				put(level, p, random.nextFloat() < 0.15 ? Blocks.GRAVEL.defaultBlockState() : oldStone(random));
				for (int y = 1; y <= 7; y++) put(level, p.above(y), Blocks.AIR.defaultBlockState());
				// Its walls, broken off at heights of their own.
				boolean wall = Math.abs(a) == 6 || Math.abs(d) == 6;
				if (wall && !(a == 6 && Math.abs(d) <= 1)) {
					int height = 1 + random.nextInt(4);
					for (int y = 1; y <= height; y++) put(level, p.above(y), oldStone(random));
				}
			}
		}
		switch (ruin) {
			case SWORD_HALL -> {
				// Pillars standing and fallen; racks of swords along its walls.
				for (int a = -3; a <= 3; a += 3) {
					for (int d = -3; d <= 3; d += 6) {
						int height = 2 + random.nextInt(4);
						for (int y = 1; y <= height; y++) put(level, f.at(a, d, y).atY(floor + y), log(Blocks.STRIPPED_DARK_OAK_LOG));
					}
				}
				for (int a = -4; a <= 4; a += 2) {
					put(level, f.at(a, -5, 1).atY(floor + 1), ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.FACING, f.right()));
					put(level, f.at(a, 5, 1).atY(floor + 1), ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.FACING, f.right().getOpposite())
							.setValue(BuriedSwordBlock.BROKEN, random.nextBoolean()));
				}
			}
			case SWORD_FORGE -> {
				// Its forge: anvils, grindstones, a cold furnace, quenching water.
				put(level, f.at(-2, -2, 1).atY(floor + 1), Blocks.DAMAGED_ANVIL.defaultBlockState());
				put(level, f.at(-2, 2, 1).atY(floor + 1), Blocks.CHIPPED_ANVIL.defaultBlockState());
				put(level, f.at(1, -3, 1).atY(floor + 1), Blocks.GRINDSTONE.defaultBlockState());
				put(level, f.at(-4, 0, 1).atY(floor + 1), Blocks.BLAST_FURNACE.defaultBlockState());
				put(level, f.at(2, 3, 1).atY(floor + 1), Blocks.WATER_CAULDRON.defaultBlockState());
				for (int k = 0; k < 4; k++) put(level, f.at(random.nextInt(9) - 4, random.nextInt(9) - 4, 1).atY(floor + 1), Blocks.CHAIN.defaultBlockState());
			}
			case HALL_OF_GRAVES -> {
				// Rows of graves: a sword driven into each.
				for (int a = -4; a <= 4; a += 2) {
					for (int d = -4; d <= 4; d += 4) {
						put(level, f.at(a, d, 0).atY(floor), Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
						put(level, f.at(a, d, 1).atY(floor + 1), ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.FACING, f.forward())
								.setValue(BuriedSwordBlock.TILTED, random.nextFloat() < 0.3));
					}
				}
			}
			case QUARTERS -> {
				// Bunks and chests long rotted: their frames, a cold hearth.
				for (int a = -3; a <= 3; a += 3) put(level, f.at(a, -4, 1).atY(floor + 1), slab(Blocks.SPRUCE_SLAB, false));
				put(level, f.at(0, 3, 1).atY(floor + 1), Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
			}
		}
		join(level, f.at(-7, -7, 0).atY(floor - 1), f.at(7, 7, 0).atY(floor + 8));
	}

	/** A ruin's vault: at its back, in from its broken wall. */
	static BlockPos vaultAt(SwordTomb.Shape s, BlockPos c, Ruin ruin) {
		return ruinFrame(s, c, ruin).at(-5, 0, 0).atY(SwordTomb.FLOOR_Y + 1);
	}

	// --- Renewing ---

	/** Its old swords: last opening's, drawn or not, laid back as ordinary swords; this opening's chosen afresh, whole and glinting. */
	static void renewRelics(ServerLevel level, SwordTomb.Shape s, BlockPos c, SecretRealms.Record r) {
		List<BlockPos> spots = relicSpots(s, c);
		RandomSource random = RandomSource.create(s.seed ^ r.openings * 0x31L);
		for (BlockPos at : spots) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			BlockState here = level.getBlockState(at);
			if (here.getBlock() instanceof BuriedSwordBlock && here.getValue(BuriedSwordBlock.RELIC)) put(level, at, here.setValue(BuriedSwordBlock.RELIC, false));
		}
		java.util.Collections.shuffle(spots, new java.util.Random(random.nextLong()));
		for (BlockPos at : spots.subList(0, Math.min(RELICS, spots.size()))) {
			if (!level.getBlockState(at.below()).isSolid()) continue;
			put(level, at, ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.FACING, Direction.from2DDataValue(random.nextInt(4)))
					.setValue(BuriedSwordBlock.RELIC, true).setValue(BuriedSwordBlock.TILTED, random.nextBoolean()));
		}
	}

	static void renewVaults(ServerLevel level, SwordTomb.Shape s, BlockPos c) {
		for (Ruin ruin : Ruin.values()) {
			BlockPos at = vaultAt(s, c, ruin);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(ruin.vault).defaultBlockState());
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, vaultTreasures(ruin, level.getRandom()));
		}
	}

	/** What a ruin's vault holds: the Sword Hall's blades; the Forge's metal and a blade half made; the Graves' manuals; the Quarters' stones. */
	static List<ItemStack> vaultTreasures(Ruin ruin, RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 4 + random.nextInt(8)));
		switch (ruin) {
			case SWORD_HALL -> {
				out.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 1 + random.nextInt(3)));
				out.add(Artifacts.create(ArtifactKind.FLYING_SWORD, random.nextFloat() < 0.3f ? ArtifactGrade.EARTH : ArtifactGrade.SPIRIT,
						random.nextFloat() < 0.25f ? Artifacts.Quality.FINE : Artifacts.Quality.NORMAL, null));
				if (random.nextFloat() < 0.4f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
				if (random.nextFloat() < 0.15f) out.add(new ItemStack(ModItems.ANCIENT_ESSENCE));
			}
			case SWORD_FORGE -> {
				out.add(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 3 + random.nextInt(5)));
				if (random.nextFloat() < 0.5f) out.add(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 1 + random.nextInt(3)));
				if (random.nextFloat() < 0.5f) out.add(Artifacts.create(random.nextBoolean() ? ArtifactKind.SABER : ArtifactKind.FLYING_SWORD, ArtifactGrade.SPIRIT,
						Artifacts.Quality.NORMAL, null));
			}
			case HALL_OF_GRAVES -> {
				for (int i = 0; i < 1 + random.nextInt(2); i++) {
					ItemStack manual = metalManual(random, Realm.CORE_FORMATION);
					if (!manual.isEmpty()) out.add(manual);
				}
				if (random.nextFloat() < 0.25f) out.add(new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS));
			}
			case QUARTERS -> {
				out.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 4 + random.nextInt(6)));
				if (random.nextFloat() < 0.3f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
			}
		}
		return out;
	}

	/** A manual of a sword spell (Metal's) up to {@code realm}'s. */
	static ItemStack metalManual(RandomSource random, Realm realm) {
		List<Spell> spells = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven() && spell.element() == QiElement.METAL && spell.heavenRealm().ordinal() <= realm.ordinal()) spells.add(spell);
		}
		return spells.isEmpty() ? ItemStack.EMPTY : TechniqueManualItem.of(spells.get(random.nextInt(spells.size())));
	}

	/** Its killing formations of blades: about the Sword Path's foot and in the Hall of Graves. */
	static List<BlockPos> formations(SwordTomb.Shape s, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		BlockPos foot = SwordTomb.pathFoot(s);
		out.add(c.offset(foot.getX(), 0, foot.getZ()).relative(mainWay(s), 6).atY(SwordTomb.column(s, foot.getX(), foot.getZ()).ground()));
		out.add(ruinFrame(s, c, Ruin.HALL_OF_GRAVES).at(0, 0, 0).atY(SwordTomb.FLOOR_Y));
		return out;
	}

	static void renewFormations(ServerLevel level, SwordTomb.Shape s, BlockPos c) {
		for (BlockPos at : formations(s, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			put(level, at, ModBlocks.KILLING_FORMATION.defaultBlockState().setValue(KillingFormationBlock.ARRAY, KillingFormationBlock.Array.BLADES));
		}
	}

	/** The shrine's stone: the Sleeping Sword in it, whole and glinting, whatever was done to it last time. */
	static void renewShrine(ServerLevel level, SwordTomb.Shape s, BlockPos c) {
		BlockPos at = shrineSword(s, c);
		level.getChunk(at.getX() >> 4, at.getZ() >> 4);
		put(level, at.below(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		put(level, at, ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.FACING, mainWay(s)).setValue(BuriedSwordBlock.RELIC, true));
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : SwordTomb.arrivals(SwordTomb.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		SwordTomb.Shape s = SwordTomb.shape(seed);
		double a = random.nextDouble() * Math.PI * 2, r = SwordTomb.RING_IN + 3 + random.nextDouble() * (SwordTomb.RING_OUT - SwordTomb.RING_IN - 6);
		int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
		SwordTomb.Column col = SwordTomb.column(s, x, z);
		return col.kind() == SwordTomb.Kind.FLOOR ? c.offset(x, 0, z).atY(col.ground() + 1) : null;
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 3 + random.nextInt(6)));
		if (random.nextFloat() < 0.35f) carried.add(Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.SPIRIT, Artifacts.Quality.NORMAL, null));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (random.nextFloat() < 0.15f) {
			ItemStack manual = metalManual(random, Realm.CORE_FORMATION);
			if (!manual.isEmpty()) carried.add(manual);
		}
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		SwordTomb.Shape s = SwordTomb.shape(seed);
		List<Slot> slots = new ArrayList<>();
		// About the gorge: a wraith every thirty degrees, half way across the ring (off the ruins' middles).
		for (int k = 0; k < 12; k++) {
			double a = s.mainTurn + Math.toRadians(k * 30 + 15), r = SwordTomb.RING_IN + 8 + (k % 3) * 12;
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			slots.add(new Slot("ring" + k, ModEntities.SWORD_WRAITH, c.offset(x, 0, z).atY(SwordTomb.column(s, x, z).ground() + 1),
					realm.guardianRank(k % 4 == 0 ? Stage.MID : Stage.EARLY), false));
		}
		for (Ruin ruin : Ruin.values()) {
			Frame f = ruinFrame(s, c, ruin);
			for (int k = 0; k < ruin.wraiths; k++) {
				slots.add(new Slot(ruin.name().toLowerCase() + k, ModEntities.SWORD_WRAITH, f.at(-2, k == 0 ? -3 : 3, 0),
						realm.guardianRank(ruin == Ruin.SWORD_HALL ? Stage.LATE : Stage.MID), false));
			}
		}
		BlockPos foot = SwordTomb.pathFoot(s);
		slots.add(new Slot("warden", ModEntities.SWORD_WRAITH, c.offset(foot.getX(), 0, foot.getZ()).atY(foot.getY()), realm.guardianRank(Stage.LATE), false));
		slots.add(new Slot("duelist", ModEntities.SWORD_WRAITH, duelPost(s, c), realm.guardianRank(Stage.EARLY), true));
		return slots;
	}

	@Override
	protected int calm() { return SwordTomb.RING_IN; }

	// --- During an opening ---

	/** The falling blades of the swords' wake, each with where it lands and when (the constructs are only for the eye). */
	private record Falling(SpellConstruct blade, Vec3 from, Vec3 to, long born, int fall) {}

	private static final List<Falling> FALLING = new ArrayList<>();

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("WakeAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Wake", 0);
	}

	@Override
	protected void end(ServerLevel level, SecretRealms.Record r) {
		for (Falling f : FALLING) f.blade.dissolve();
		FALLING.clear();
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		SwordTomb.Shape s = SwordTomb.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		swordsWake(level, s, c, r, now, inside, data);
		fall(level, now);
		if (now % 5 == 0) for (ServerPlayer p : inside) path(level, s, c, r, p, now, data);
		if (now % 10 == 3) intent(level, s, c, r, now, inside);
		// The trials told to whoever comes to the Sword Path's foot.
		if (now % 40 == 17) {
			BlockPos foot = c.offset(SwordTomb.pathFoot(s).getX(), 0, SwordTomb.pathFoot(s).getZ()).atY(SwordTomb.pathFoot(s).getY());
			ListTag told = r.works.getList("Told", Tag.TAG_STRING);
			for (ServerPlayer p : inside) {
				if (p.blockPosition().distSqr(foot) > 12 * 12 || told.contains(StringTag.valueOf(p.getStringUUID()))) continue;
				told.add(StringTag.valueOf(p.getStringUUID()));
				p.sendSystemMessage(Component.translatable(ModLang.MSG_TOMB_TRIALS, Component.translatable(r.inheritor.isEmpty() ? ModLang.MSG_TOMB_GREAT
						: ModLang.MSG_TOMB_LESSER)).withStyle(ChatFormatting.GOLD));
			}
			r.works.put("Told", told);
		}
	}

	/** How much harder the tomb's cuts fall on {@code p}: twice as hard on one well below the realm it is meant for, a quarter on one far above. */
	static double weakness(SecretRealm realm, LivingEntity e) {
		int rank = e instanceof ServerPlayer p ? TwoPaths.higherRank(CultivationManager.get(p), true) : e instanceof CultivatorNpc npc ? npc.sustainedRank() : 0;
		return Mth.clamp(1 + (realm.guardianRank(Stage.MID) - rank) / 8.0, 0.25, 2.0);
	}

	static void cut(ServerLevel level, SecretRealm realm, LivingEntity e, double share) {
		e.hurt(ModDamageTypes.swordIntent(level), (float) (e.getMaxHealth() * share * weakness(realm, e)));
	}

	/** Whether {@code p}'s Sword Intent is lit (the technique: one so is spared the tomb's own). */
	static boolean intentLit(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return c.isAbilityEnabled(Ability.SWORD_INTENT) && Ability.SWORD_INTENT.isUnlocked(c);
	}

	/** Lingering intent: its patches glint (more for those with Qi Sense open), and cut whoever runs, flies or fights in them. */
	static void intent(ServerLevel level, SwordTomb.Shape s, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		List<BlockPos> zones = zones(s, c);
		for (ServerPlayer p : inside) {
			for (BlockPos z : zones) {
				double d = p.position().distanceTo(Vec3.atBottomCenterOf(z));
				if (d < 32 && now % 20 == 3) {
					boolean sees = CultivationManager.get(p).isAbilityEnabled(Ability.QI_SENSE);
					level.sendParticles(p, ParticleTypes.ENCHANTED_HIT, true, z.getX() + 0.5, z.getY() + 0.8, z.getZ() + 0.5, sees ? 14 : 2, ZONE_R * 0.6, 0.6,
							ZONE_R * 0.6, 0.02);
				}
				if (d > ZONE_R || intentLit(p) || p.isCreative()) continue;
				boolean careless = p.isSprinting() || p.getAbilities().flying || p.isFallFlying() || p.tickCount - p.getLastHurtMobTimestamp() < 10;
				if (!careless) continue;
				cut(level, r.realm, p, INTENT_CUT);
				level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1, p.getZ(), 2, 0.4, 0.4, 0.4, 0);
				level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.AMBIENT, 0.8f, 1.4f);
				p.displayClientMessage(Component.translatable(ModLang.MSG_TOMB_INTENT_CUT).withStyle(ChatFormatting.GRAY), true);
				break;
			}
		}
	}

	/**
	 * The Sword Path, for {@code p}: begun at its foot; on it, its pressure (heavier the higher: slowed, now and then cut, a flier dragged down
	 * onto the steps); left, begun again; its head reached by one who walked it all the way, the trial passed.
	 */
	static void path(ServerLevel level, SwordTomb.Shape s, BlockPos c, SecretRealms.Record r, ServerPlayer p, long now, SecretRealms.Data data) {
		CompoundTag walks = r.works.getCompound("Walks");
		String id = p.getStringUUID();
		int lx = p.getBlockX() - c.getX(), lz = p.getBlockZ() - c.getZ();
		double turn = SwordTomb.pathAt(s, lx + 0.5, lz + 0.5);
		BlockPos foot = c.offset(SwordTomb.pathFoot(s).getX(), 0, SwordTomb.pathFoot(s).getZ()).atY(SwordTomb.pathFoot(s).getY());
		BlockPos head = c.offset(SwordTomb.pathHead(s).getX(), 0, SwordTomb.pathHead(s).getZ()).atY(SwordTomb.pathHead(s).getY());
		boolean atFoot = p.blockPosition().distSqr(foot) < 4 * 4;
		CompoundTag walk = walks.getCompound(id);
		if (atFoot && !walk.getBoolean("On") && !passed(r, "Path", p.getUUID())) {
			walk.putBoolean("On", true);
			walk.putDouble("Best", 0);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_TOMB_PATH_BEGUN).withStyle(ChatFormatting.GOLD));
		}
		if (turn >= 0) {
			double height = turn / SwordTomb.PATH_TURN;
			// Its pressure: slowed by the height climbed; a flier dragged down onto the steps; cut now and then.
			p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, (int) Math.floor(height * 3), true, false));
			if (p.getAbilities().flying && !p.isCreative()) {
				p.getAbilities().flying = false;
				p.onUpdateAbilities();
				p.displayClientMessage(Component.translatable(ModLang.MSG_TOMB_PATH_DRAGGED).withStyle(ChatFormatting.GRAY), true);
			}
			if (now % 40 == 0 && height > 0.2 && !p.isCreative()) cut(level, r.realm, p, PATH_CUT * height);
			if (walk.getBoolean("On")) {
				double best = walk.getDouble("Best");
				if (turn > best + 45) {
					walk.putBoolean("On", false); // come onto it far up, not by its steps
				} else {
					walk.putDouble("Best", Math.max(best, turn));
				}
			}
		} else if (walk.getBoolean("On") && !atFoot) {
			boolean summit = SwordTomb.column(s, lx, lz).kind() == SwordTomb.Kind.SUMMIT && p.blockPosition().distSqr(head) < 8 * 8;
			if (summit && walk.getDouble("Best") >= SwordTomb.PATH_TURN - 10) {
				walk.putBoolean("On", false);
				pass(r, "Path", p.getUUID());
				p.sendSystemMessage(Component.translatable(ModLang.MSG_TOMB_PATH_WALKED).withStyle(ChatFormatting.GOLD));
				p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);
			} else if (!summit) {
				walk.putBoolean("On", false);
				p.sendSystemMessage(Component.translatable(ModLang.MSG_TOMB_PATH_LEFT).withStyle(ChatFormatting.GRAY));
			}
		}
		walks.put(id, walk);
		r.works.put("Walks", walks);
		data.setDirty();
	}

	/** Its hour come, for {@link #WAKE_TICKS}: swords rise from the gorge and fall about everyone inside; then they lie still again. */
	static void swordsWake(ServerLevel level, SwordTomb.Shape s, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int stage = r.works.getInt("Wake");
		if (stage == 0 && now >= r.works.getLong("WakeAt")) {
			r.works.putInt("Wake", 1);
			r.works.putLong("WakeEnds", now + WAKE_TICKS);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_TOMB_SWORDS_WAKE, WAKE_TICKS / 1200).withStyle(ChatFormatting.RED));
			for (ServerPlayer p : inside) p.playNotifySound(SoundEvents.TRIDENT_RETURN, SoundSource.AMBIENT, 1.5f, 0.5f);
		} else if (stage == 1) {
			if (now >= r.works.getLong("WakeEnds")) {
				r.works.putInt("Wake", 2);
				data.setDirty();
				tellInside(level, r.realm, Component.translatable(ModLang.MSG_TOMB_SWORDS_SLEEP).withStyle(ChatFormatting.GRAY));
				return;
			}
			if (now % FALL_EVERY != 0) return;
			RandomSource random = level.getRandom();
			for (ServerPlayer p : inside) {
				// About them, never right on them: a glint first where it will land.
				double a = random.nextDouble() * Math.PI * 2, d = 1.5 + random.nextDouble() * 5;
				int x = Mth.floor(p.getX() + Math.cos(a) * d), z = Mth.floor(p.getZ() + Math.sin(a) * d);
				if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				Vec3 to = new Vec3(x + 0.5, y, z + 0.5), from = to.add((random.nextDouble() - 0.5) * 3, 16, (random.nextDouble() - 0.5) * 3);
				level.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 0.2, to.z, 6, 0.15, 0.05, 0.15, 0.01);
				Vec3 way = to.subtract(from);
				float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z)), pitch = (float) Math.toDegrees(Math.atan2(-way.y, Math.hypot(way.x, way.z)));
				SpellConstruct blade = SpellConstruct.create(level, SpellConstruct.Kind.RAIN_SWORD, from, yaw, pitch, 40, null).spawn();
				FALLING.add(new Falling(blade, from, to, now, 14));
			}
		}
	}

	/** The wake's blades falling, each landing in its time: whoever stands where it lands is cut. */
	static void fall(ServerLevel level, long now) {
		for (Iterator<Falling> it = FALLING.iterator(); it.hasNext(); ) {
			Falling f = it.next();
			if (f.blade.isRemoved() || f.blade.level() != level) {
				it.remove();
				continue;
			}
			double t = Math.min(1, (now - f.born) / (double) f.fall);
			f.blade.place(f.from.lerp(f.to, t * t), f.blade.getYRot(), f.blade.getXRot());
			if (t < 1) continue;
			SecretRealm realm = SecretRealms.realmAt(level, f.to.x, f.to.z);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(f.to, f.to).inflate(1.3, 2, 1.3), RealmGuardian::validFoe)) {
				if (realm != null) cut(level, realm, e, BLADE_CUT);
			}
			level.sendParticles(ParticleTypes.CRIT, f.to.x, f.to.y + 0.2, f.to.z, 10, 0.3, 0.1, 0.3, 0.2);
			level.playSound(null, BlockPos.containing(f.to), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.AMBIENT, 1.0f, 1.2f);
			f.blade.dissolve();
			it.remove();
		}
	}

	// --- An old sword drawn, the shrine's sword ---

	/**
	 * {@code p} draws an old sword out of the tomb's floor (BuriedSwordBlock): what it kept is theirs (a forged blade, a manual of a sword
	 * spell, spirit stones), the sword left broken, the wraiths about it woken. The shrine's own: the inheritance, for one who has passed both
	 * trials ({@link RealmWorks#inherit}).
	 */
	static void drawn(ServerLevel level, BlockPos pos, ServerPlayer p) {
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm != SecretRealm.SUNKEN_SWORD_TOMB) return;
		SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
		if (r.openedAt < 0) return;
		SwordTomb.Shape s = SwordTomb.shape(level.getSeed());
		if (pos.equals(shrineSword(s, realm.centre()))) {
			if (!passed(r, p.getUUID())) {
				p.displayClientMessage(Component.translatable(ModLang.MSG_TOMB_STONE_HOLDS).withStyle(ChatFormatting.GRAY), true);
				return;
			}
			inherit(level, r, p);
			return;
		}
		BlockState state = level.getBlockState(pos);
		RealmLedger.quietly(() -> level.setBlock(pos, state.setValue(BuriedSwordBlock.RELIC, false).setValue(BuriedSwordBlock.BROKEN, true), Block.UPDATE_ALL));
		for (ItemStack stack : relicTreasure(level.getRandom())) give(p, stack);
		p.displayClientMessage(Component.translatable(ModLang.MSG_RELIC_DRAWN).withStyle(ChatFormatting.GOLD), true);
		level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.2, 0.4, 0.2, 0.05);
		wakeAround(level, pos, p);
	}

	/** What an old sword keeps: a blade (likely Spirit grade, now and then Earth), a sword spell's manual, or spirit stones. */
	static List<ItemStack> relicTreasure(RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.35f) {
			return List.of(Artifacts.create(ArtifactKind.FLYING_SWORD, random.nextFloat() < 0.3f ? ArtifactGrade.EARTH : ArtifactGrade.SPIRIT,
					random.nextFloat() < 0.2f ? Artifacts.Quality.FINE : Artifacts.Quality.NORMAL, null));
		}
		if (roll < 0.6f) {
			ItemStack manual = metalManual(random, Realm.CORE_FORMATION);
			if (!manual.isEmpty()) return List.of(manual);
		}
		if (roll < 0.9f) return List.of(new ItemStack(ModItems.LOW_SPIRIT_STONE, 3 + random.nextInt(6)));
		return List.of(new ItemStack(ModItems.MID_SPIRIT_STONE, 1 + random.nextInt(2)));
	}

	// --- Its trials and inheritance ---

	/** The sword's spirit stirs only for one who has walked the Sword Path. */
	@Override
	protected Component mayChallenge(SecretRealms.Record r, LivingEntity p) {
		return passed(r, "Path", p.getUUID()) ? null : Component.translatable(ModLang.MSG_TOMB_DUEL_UNWALKED);
	}

	@Override
	protected MutableComponent challengeMessage(Component rank) {
		return Component.translatable(ModLang.MSG_TOMB_SPIRIT_WAKES, rank);
	}

	@Override
	protected MutableComponent gatePassedMessage() {
		return Component.translatable(ModLang.MSG_TOMB_SPIRIT_BOWS);
	}

	/** The Great Inheritance: the Sleeping Sword, and the manual of Sword Intent. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(SleepingSwordItem.create(), TechniqueManualItem.of(Technique.SWORD_INTENT));
	}

	/** A lesser legacy: a Fine Earth-grade jian, or a sword spell's manual; spirit stones with it. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		ItemStack manual = metalManual(random, Realm.NASCENT_SOUL);
		ItemStack first = random.nextBoolean() || manual.isEmpty() ? Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.EARTH, Artifacts.Quality.FINE, null) : manual;
		return List.of(first, new ItemStack(ModItems.MID_SPIRIT_STONE, 2 + random.nextInt(3)));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_TOMB_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_TOMB_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_TOMB_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** How far up the Sword Path one come for it makes for at a time (degrees round). */
	static final double RIVAL_PATH_STEP = 24;

	/** The nearest old sword still keeping something. */
	@Override
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) {
		BlockPos best = null;
		double d = 56 * 56;
		for (BlockPos at : relicSpots(SwordTomb.shape(seed), c)) {
			double here = at.distSqr(near);
			if (here < d && level.isLoaded(at) && level.getBlockState(at).is(ModBlocks.BURIED_SWORD) && level.getBlockState(at).getValue(BuriedSwordBlock.RELIC)) {
				d = here;
				best = at;
			}
		}
		return best;
	}

	/** An old sword drawn, as a player draws it ({@link #drawn}): what it kept, the wraiths about it woken. */
	@Override
	protected List<ItemStack> rivalTake(ServerLevel level, BlockPos at, CultivatorNpc npc) {
		BlockState state = level.getBlockState(at);
		if (!state.is(ModBlocks.BURIED_SWORD) || !state.getValue(BuriedSwordBlock.RELIC)) return List.of();
		RealmLedger.quietly(() -> level.setBlock(at, state.setValue(BuriedSwordBlock.RELIC, false).setValue(BuriedSwordBlock.BROKEN, true), Block.UPDATE_ALL));
		level.playSound(null, at, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
		wakeAround(level, at, npc);
		return relicTreasure(level.getRandom());
	}

	@Override
	protected List<BlockPos> vaults(long seed, BlockPos c) {
		SwordTomb.Shape s = SwordTomb.shape(seed);
		List<BlockPos> out = new ArrayList<>();
		for (Ruin ruin : Ruin.values()) out.add(vaultAt(s, c, ruin));
		return out;
	}

	/**
	 * The Sword Path walked (on foot, a stretch at a time from wherever on it one stands: to its foot first, and its head last), the sword's
	 * spirit bested, then the Sleeping Sword drawn.
	 */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		SwordTomb.Shape s = SwordTomb.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		UUID id = npc.getUUID();
		if (passed(r, "Drawn", id)) return null;
		if (passed(r, id)) return new Trial("sword", shrineSword(s, c), 3, false);
		if (passed(r, "Path", id)) return new Trial("duel", duelPost(s, c), 4, false);
		double turn = SwordTomb.pathAt(s, npc.getX() - c.getX(), npc.getZ() - c.getZ());
		BlockPos foot = SwordTomb.pathFoot(s);
		BlockPos footAt = c.offset(foot.getX(), 0, foot.getZ()).atY(foot.getY());
		if (turn < 0) {
			// Not on it: to its foot first, and up it from there.
			if (npc.blockPosition().distSqr(footAt) > 4 * 4) return new Trial("path", footAt, 2.5, true);
			turn = 0;
		}
		if (turn + RIVAL_PATH_STEP >= SwordTomb.PATH_TURN - 4) {
			BlockPos head = SwordTomb.pathHead(s);
			return new Trial("head", c.offset(head.getX(), 0, head.getZ()).atY(head.getY()), 2.5, true);
		}
		BlockPos next = SwordTomb.pathPoint(s, turn + RIVAL_PATH_STEP);
		return new Trial("path", c.offset(next.getX(), 0, next.getZ()).atY(next.getY()), 2.0, true);
	}

	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		switch (trial.step()) {
			case "head" -> pass(r, "Path", npc.getUUID());
			case "duel" -> {
				return challengeAt(level, r, npc, trial.mark(), working);
			}
			case "sword" -> {
				pass(r, "Drawn", npc.getUUID());
				rivalInherits(level, r, npc);
			}
			default -> {}
		}
		return true;
	}

	private SwordTombWorks() {}
}
