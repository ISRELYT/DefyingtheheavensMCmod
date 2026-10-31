package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * The Ancient Battlefield's works (SecretRealm#ANCIENT_BATTLEFIELD; its land is Battlefield's, the machinery every realm shares RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: an arrival camp on each way in (trampled earth, a banner, a cold brazier); war banners standing all over the plain;
 *   broken palisades along the earthworks; a general's tent between the trenches on each of the four diagonals, a vault in each; on the
 *   hill's top the ring of banners where the General made his last stand, the war drum at its middle; his tomb within the hill, its door in
 *   its far side shut by a trial's barrier.</li>
 *   <li><b>Renewed each time it stirs</b>: the fallen ({@link #BODIES} soldiers' bones across the plain, each with what it died with: an
 *   old blade, armour, spirit stones, now and then a manual); the tents' vaults; its killing formations; the General's bones on his bier.</li>
 *   <li><b>Its guardians</b>: Bone Soldiers kneeling where they fell about the trenches and the tents and at the hill's foot, rising for
 *   whoever comes close.</li>
 *   <li><b>Its event</b>: some time into each opening the dead rise ({@link #DEAD_TICKS}): soldiers claw up out of the earth about everyone
 *   inside.</li>
 *   <li><b>Its trial</b>: hold the General's hill. One who steps into the ring of banners sounds the drum; {@link #WAVES} waves of Battle
 *   Spirits come up the hill at them, the last with the General's Spirit; whoever leaves the ring or falls has lost it. Held, the tomb's
 *   barrier lets them through, and at his bier the inheritance: the Great (the War General's Banner and the Blood Battle manual) once in a
 *   world, after it a lesser legacy each opening.</li>
 * </ul>
 * Its yin is thick: the ghostly qi there is three times the Netherworld's (GhostQi#SECRET_BATTLEFIELD).
 */
public final class BattlefieldWorks extends RealmWorks {
	static final BattlefieldWorks WORKS = new BattlefieldWorks();
	/** The fallen laid about the plain each opening. */
	static final int BODIES = 24;
	/** The dead rise this long (a minute and a half), a few about each visitor every {@link #RISE_EVERY} ticks, at most {@link #RISEN_MOST} at once. */
	static final int DEAD_TICKS = 1800, RISE_EVERY = 40, RISEN_MOST = 16;
	/** The ring of banners on the hill's top: its reach (blocks); the waves come up at its holder; how long one may stand out of it. */
	static final int RING_R = 7, WAVES = 5, OUT_TICKS = 60;

	// --- Where things are ---

	static Direction mainWay(Battlefield.Shape s) {
		return Direction.fromDelta((int) Math.round(Math.cos(s.mainTurn)), 0, (int) Math.round(Math.sin(s.mainTurn)));
	}

	/** The middle of the ring of banners, on the hill's top. */
	static BlockPos ringMiddle(BlockPos c) {
		return c.atY(Battlefield.HILL_TOP_Y + 1);
	}

	/** The tomb's door in the hill's far side (its middle, at its floor), and its chamber's middle under the hill's top. */
	static BlockPos tombDoor(Battlefield.Shape s, BlockPos c) {
		return c.relative(mainWay(s).getOpposite(), Battlefield.HILL_R - 8).atY(Battlefield.PLAIN_Y + 3);
	}

	static BlockPos tombMiddle(BlockPos c) {
		return c.atY(Battlefield.PLAIN_Y + 3);
	}

	/** The General's bier in his chamber, at its back. */
	static BlockPos bier(Battlefield.Shape s, BlockPos c) {
		return tombMiddle(c).relative(mainWay(s), 3);
	}

	/** A general's tent on diagonal {@code k}, between the first trench and the second. */
	static Frame tent(Battlefield.Shape s, BlockPos c, int k) {
		double b = s.way(k) + Math.PI / 4, r = (Battlefield.TRENCHES[0] + Battlefield.TRENCHES[1]) / 2.0;
		double cos = Math.cos(b), sin = Math.sin(b);
		int x = (int) Math.round(cos * r), z = (int) Math.round(sin * r);
		Direction in = Math.abs(cos) > Math.abs(sin) ? (cos > 0 ? Direction.WEST : Direction.EAST) : (sin > 0 ? Direction.NORTH : Direction.SOUTH);
		return new Frame(c.offset(x, 0, z).atY(groundAt(SecretRealm.ANCIENT_BATTLEFIELD, s.seed, c.getX() + x, c.getZ() + z) + 1), in);
	}

	/** The fallen's places on the plain (its own roll): between the trenches, off the ways. */
	static List<BlockPos> bodySpots(Battlefield.Shape s, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		RandomSource random = RandomSource.create(s.seed ^ 0xB0D1E5L);
		for (int tries = 0; out.size() < BODIES && tries < 600; tries++) {
			double a = random.nextDouble() * Math.PI * 2, r = Battlefield.HILL_R + 6 + random.nextDouble() * (Battlefield.TRENCHES[2] - Battlefield.HILL_R - 8);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			Battlefield.Column col = Battlefield.column(s, x, z);
			if (col.kind() != Battlefield.Kind.PLAIN && col.kind() != Battlefield.Kind.CRATER) continue;
			out.add(c.offset(x, 0, z).atY(col.ground() + 1));
		}
		return out;
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		Battlefield.Shape s = Battlefield.shape(seed);
		steps.add(() -> camps(level, s, c));
		for (int part = 0; part < 4; part++) {
			int p = part;
			steps.add(() -> banners(level, s, c, p));
		}
		steps.add(() -> palisades(level, s, c));
		for (int k = 0; k < 4; k++) {
			int t = k;
			steps.add(() -> tentAt(level, s, c, t));
		}
		steps.add(() -> hillRing(level, s, c));
		steps.add(() -> tomb(level, s, c));
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		Battlefield.Shape s = Battlefield.shape(seed);
		steps.add(() -> renewFallen(level, s, c, r));
		steps.add(() -> renewVaults(level, s, c));
		steps.add(() -> renewFormations(level, s, c));
		steps.add(() -> renewBier(level, s, c));
	}

	static void put(ServerLevel level, BlockPos pos, Block block) {
		put(level, pos, block.defaultBlockState());
	}

	/** A war banner of the old armies, red or black, raised on a pole of dark wood ({@code height} posts under it) at {@code at}. */
	static void banner(ServerLevel level, BlockPos at, int height, RandomSource random) {
		for (int y = 0; y < height; y++) put(level, at.above(y), Blocks.DARK_OAK_FENCE);
		Block banner = random.nextBoolean() ? Blocks.RED_BANNER : Blocks.BLACK_BANNER;
		put(level, at.above(height), banner.defaultBlockState().setValue(BannerBlock.ROTATION, random.nextInt(16)));
	}

	/** On each way in: a camp of trampled earth, a banner either side, a cold brazier. */
	static void camps(ServerLevel level, Battlefield.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0xCA3BL);
		for (BlockPos local : Battlefield.arrivals(s)) {
			BlockPos at = c.offset(local.getX(), 0, local.getZ()).atY(local.getY());
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz > 12) continue;
					put(level, at.offset(dx, 0, dz), random.nextBoolean() ? Blocks.COARSE_DIRT : Blocks.PACKED_MUD);
					for (int y = 1; y <= 3; y++) put(level, at.offset(dx, y, dz), Blocks.AIR);
				}
			}
			banner(level, at.offset(3, 1, 0), 1, random);
			banner(level, at.offset(-3, 1, 0), 1, random);
			put(level, at.offset(0, 1, 0), Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
		}
	}

	/** War banners standing all over the plain (a quarter of it at a time), on poles of their own heights. */
	static void banners(ServerLevel level, Battlefield.Shape s, BlockPos c, int part) {
		RandomSource random = RandomSource.create(s.seed ^ 0xBA22E2L ^ part);
		for (int i = 0; i < 10; i++) {
			double a = (part + random.nextDouble()) * Math.PI / 2, r = Battlefield.HILL_R + 4 + random.nextDouble() * (Battlefield.TRENCHES[2] - Battlefield.HILL_R);
			int x = c.getX() + (int) Math.round(Math.cos(a) * r), z = c.getZ() + (int) Math.round(Math.sin(a) * r);
			Battlefield.Column col = Battlefield.column(s, x - c.getX(), z - c.getZ());
			if (col.kind() != Battlefield.Kind.PLAIN && col.kind() != Battlefield.Kind.BERM) continue;
			level.getChunk(x >> 4, z >> 4);
			banner(level, new BlockPos(x, col.ground() + 1, z), 1 + random.nextInt(2), random);
		}
	}

	/** Palisades of old logs along the earthworks, broken: a stretch at every sixth of each ring, gaps in it. */
	static void palisades(ServerLevel level, Battlefield.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0x9A115L);
		for (int ring : Battlefield.TRENCHES) {
			for (int k = 0; k < 6; k++) {
				double from = s.mainTurn + Math.PI / 4 + k * Math.PI / 3;
				for (double a = from; a < from + 10.0 / ring; a += 1.0 / ring) {
					if (random.nextFloat() < 0.3f) continue;
					double r = ring - Battlefield.TRENCH_HALF - 1.5;
					int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
					Battlefield.Column col = Battlefield.column(s, x, z);
					if (col.kind() != Battlefield.Kind.BERM) continue;
					BlockPos at = c.offset(x, 0, z).atY(col.ground() + 1);
					level.getChunk(at.getX() >> 4, at.getZ() >> 4);
					int height = 1 + random.nextInt(3);
					for (int y = 0; y < height; y++) put(level, at.above(y), log(Blocks.SPRUCE_LOG));
				}
			}
		}
	}

	/**
	 * A general's tent: a floor of planks under a ridge of red cloth running from its back to its front, its slopes stepping down to low
	 * walls at its sides, its back closed and its front open in the middle toward the hill, a banner on a pole either side of its door; a
	 * table, a barrel, a vault at its back.
	 */
	static void tentAt(ServerLevel level, Battlefield.Shape s, BlockPos c, int k) {
		Frame f = tent(s, c, k);
		level.getChunk(f.origin().getX() >> 4, f.origin().getZ() >> 4);
		int floor = f.origin().getY() - 1;
		for (int a = -3; a <= 3; a++) {
			for (int d = -3; d <= 3; d++) {
				BlockPos p = f.at(a, d, 0).atY(floor);
				put(level, p, Blocks.SPRUCE_PLANKS);
				for (int y = 1; y <= 7; y++) put(level, p.above(y), Blocks.AIR);
				int roof = 6 - Math.abs(d); // its slope: three over its sides, six at the ridge
				put(level, p.above(roof), Blocks.RED_WOOL);
				for (int y = 1; y < roof; y++) {
					boolean side = Math.abs(d) == 3, back = a == -3, front = a == 3 && !(Math.abs(d) <= 1 && y <= 3);
					if (side || back || front) put(level, p.above(y), Blocks.RED_WOOL);
				}
			}
		}
		BlockState banner = Blocks.RED_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, Mth.positiveModulo(Math.round(f.forward().toYRot() / 22.5f), 16));
		for (int d = -2; d <= 2; d += 4) {
			BlockPos pole = f.at(4, d, 0).atY(floor + 1);
			for (int y = 0; y < 2; y++) put(level, pole.above(y), Blocks.DARK_OAK_FENCE);
			put(level, pole.above(2), banner);
		}
		put(level, f.at(0, 0, 0).atY(floor + 1), Blocks.CRAFTING_TABLE);
		put(level, f.at(0, 2, 0).atY(floor + 1), Blocks.BARREL);
		join(level, f.at(-4, -4, 0).atY(floor), f.at(4, 4, 0).atY(floor + 7));
	}

	static BlockPos vaultAt(Battlefield.Shape s, BlockPos c, int k) {
		Frame f = tent(s, c, k);
		return f.at(-2, 0, 0).atY(f.origin().getY());
	}

	/** The hill's top: a ring of old stone, eight banners standing round it, the war drum at its middle. */
	static void hillRing(ServerLevel level, Battlefield.Shape s, BlockPos c) {
		BlockPos m = ringMiddle(c);
		level.getChunk(m.getX() >> 4, m.getZ() >> 4);
		RandomSource random = RandomSource.create(s.seed ^ 0x81CEL);
		for (int dx = -RING_R - 1; dx <= RING_R + 1; dx++) {
			for (int dz = -RING_R - 1; dz <= RING_R + 1; dz++) {
				double d = Math.hypot(dx, dz);
				if (d > RING_R + 1.2) continue;
				put(level, m.offset(dx, -1, dz), Math.abs(d - RING_R) < 0.8 ? Blocks.POLISHED_ANDESITE : random.nextFloat() < 0.6 ? Blocks.COBBLESTONE : Blocks.COARSE_DIRT);
				for (int y = 0; y <= 4; y++) put(level, m.offset(dx, y, dz), Blocks.AIR);
			}
		}
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4;
			BlockPos at = m.offset((int) Math.round(Math.cos(a) * (RING_R + 0.5)), 0, (int) Math.round(Math.sin(a) * (RING_R + 0.5)));
			for (int y = 0; y < 2; y++) put(level, at.above(y), Blocks.DARK_OAK_FENCE);
			put(level, at.above(2), (k % 2 == 0 ? Blocks.RED_BANNER : Blocks.BLACK_BANNER).defaultBlockState()
					.setValue(BannerBlock.ROTATION, Mth.positiveModulo(Math.round((float) Math.toDegrees(Math.atan2(-Math.cos(a), Math.sin(a))) / 22.5f), 16)));
		}
		// The war drum: a great drum (a barrel turned up) on a stone, spears crossed by it.
		put(level, m, Blocks.CHISELED_STONE_BRICKS);
		put(level, m.above(), Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, Direction.UP));
		put(level, m.offset(1, 0, 0), ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.TILTED, true));
		put(level, m.offset(-1, 0, 0), ModBlocks.BURIED_SWORD.defaultBlockState().setValue(BuriedSwordBlock.TILTED, true)
				.setValue(BuriedSwordBlock.FACING, Direction.SOUTH));
	}

	/** The General's tomb: a passage into the hill from its far side, its door shut by a trial's barrier, his chamber under the hill's top. */
	static void tomb(ServerLevel level, Battlefield.Shape s, BlockPos c) {
		Direction in = mainWay(s);
		BlockPos door = tombDoor(s, c), middle = tombMiddle(c);
		level.getChunk(door.getX() >> 4, door.getZ() >> 4);
		RandomSource random = RandomSource.create(s.seed ^ 0x7033L);
		// The passage: from the door in toward the middle, three wide and three high, walled in old stone.
		int length = (int) Math.round(Math.sqrt(door.distSqr(middle.atY(door.getY())))) - 4;
		Frame p = new Frame(door, in);
		for (int f = -2; f <= length; f++) {
			for (int side = -2; side <= 2; side++) {
				for (int y = -1; y <= 4; y++) {
					boolean shell = Math.abs(side) == 2 || y == -1 || y == 4;
					put(level, p.at(f, side, y), shell ? oldStone(random).getBlock() : Blocks.AIR);
				}
			}
		}
		// Its mouth opened to the hillside, the barrier across it.
		for (int f = -6; f <= -3; f++) for (int side = -1; side <= 1; side++) for (int y = 0; y <= 3; y++) put(level, p.at(f, side, y), Blocks.AIR);
		for (int side = -1; side <= 1; side++) for (int y = 0; y <= 2; y++) put(level, p.at(-2, side, y), ModBlocks.TRIAL_BARRIER);
		// The chamber: nine across, five high, soul lanterns on its walls, the bier at its back.
		Frame ch = new Frame(middle, in);
		for (int f = -4; f <= 4; f++) {
			for (int side = -4; side <= 4; side++) {
				for (int y = -1; y <= 5; y++) {
					boolean shell = Math.abs(f) == 4 || Math.abs(side) == 4 || y == -1 || y == 5;
					if (shell && f == -4 && Math.abs(side) <= 1 && y >= 0 && y <= 2) continue; // where the passage comes in
					put(level, ch.at(f, side, y), shell ? (y == -1 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_BRICKS) : Blocks.AIR);
				}
			}
		}
		for (int f = -2; f <= 2; f += 4) for (int side = -3; side <= 3; side += 6) put(level, ch.at(f, side, 2), Blocks.SOUL_LANTERN);
		for (int side = -1; side <= 1; side++) put(level, ch.at(3, side, 0), Blocks.POLISHED_BLACKSTONE);
		for (int side = -2; side <= 2; side += 4) {
			put(level, ch.at(3, side, 0), Blocks.DARK_OAK_FENCE);
			put(level, ch.at(3, side, 1), Blocks.BLACK_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, Mth.positiveModulo(Math.round(in.getOpposite().toYRot() / 22.5f), 16)));
		}
		join(level, ch.at(-5, -5, -1), ch.at(5, 5, 6));
	}

	// --- Renewing ---

	/** The fallen laid about the plain again: old soldiers' bones, each with what it died with (a fresh death's body is left as it lies). */
	static void renewFallen(ServerLevel level, Battlefield.Shape s, BlockPos c, SecretRealms.Record r) {
		RandomSource random = level.getRandom();
		for (BlockPos at : bodySpots(s, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			BlockState here = level.getBlockState(at);
			if (here.getBlock() instanceof CorpseBlock && level.getBlockEntity(at) instanceof CorpseBlockEntity body && !body.isAncient()) continue;
			if (!here.isAir() && !here.canBeReplaced() && !(here.getBlock() instanceof CorpseBlock) || !level.getBlockState(at.below()).isSolid()) continue;
			put(level, at, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.STAGE, CorpseBlock.Rot.BONES));
			if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) {
				body.ancient(NpcNames.of(UUID.randomUUID()), random.nextInt(CultivatorNpc.SKINS), WORKS.remainsRank(r.realm, random), random.nextFloat() * 360,
						level.getGameTime(), soldierCarried(random));
			}
		}
	}

	/** What an old soldier of the war died with: a blade or a spear, a piece of armour, spirit stones; now and then a manual of war. */
	static List<ItemStack> soldierCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		if (random.nextFloat() < 0.4f) {
			ArtifactKind kind = new ArtifactKind[] {ArtifactKind.SPEAR, ArtifactKind.SABER, ArtifactKind.GLAIVE}[random.nextInt(3)];
			carried.add(Artifacts.create(kind, random.nextFloat() < 0.25f ? ArtifactGrade.SPIRIT : ArtifactGrade.MORTAL, Artifacts.Quality.NORMAL, null));
		}
		if (random.nextFloat() < 0.4f) {
			ItemStack armour = new ItemStack(new net.minecraft.world.item.Item[] {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.CHAINMAIL_LEGGINGS,
					Items.CHAINMAIL_HELMET}[random.nextInt(4)]);
			armour.setDamageValue(armour.getMaxDamage() / 3 + random.nextInt(Math.max(1, armour.getMaxDamage() / 3)));
			carried.add(armour);
		}
		carried.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 2 + random.nextInt(6)));
		if (random.nextFloat() < 0.08f) {
			ItemStack manual = warManual(random, Realm.NASCENT_SOUL);
			if (!manual.isEmpty()) carried.add(manual);
		}
		return carried;
	}

	/** A manual of war: a spell of Metal or Earth up to {@code realm}'s. */
	static ItemStack warManual(RandomSource random, Realm realm) {
		List<Spell> spells = new ArrayList<>();
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven() && (spell.element() == QiElement.METAL || spell.element() == QiElement.EARTH) && spell.heavenRealm().ordinal() <= realm.ordinal()) {
				spells.add(spell);
			}
		}
		return spells.isEmpty() ? ItemStack.EMPTY : TechniqueManualItem.of(spells.get(random.nextInt(spells.size())));
	}

	static void renewVaults(ServerLevel level, Battlefield.Shape s, BlockPos c) {
		for (int k = 0; k < 4; k++) {
			BlockPos at = vaultAt(s, c, k);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			StorageFormations.Tier tier = k == 0 ? StorageFormations.Tier.MID : StorageFormations.Tier.LOW;
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(tier).defaultBlockState());
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, tentTreasures(k == 0, level.getRandom()));
		}
	}

	/** A general's war chest: spirit stones, a blade of his, a manual of war; the first general's richer. */
	static List<ItemStack> tentTreasures(boolean first, RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 6 + random.nextInt(10)));
		out.add(new ItemStack(ModItems.MID_SPIRIT_STONE, first ? 2 + random.nextInt(3) : random.nextInt(2)));
		ArtifactKind kind = new ArtifactKind[] {ArtifactKind.SPEAR, ArtifactKind.SABER, ArtifactKind.GLAIVE}[random.nextInt(3)];
		out.add(Artifacts.create(kind, first || random.nextFloat() < 0.3f ? ArtifactGrade.EARTH : ArtifactGrade.SPIRIT,
				random.nextFloat() < 0.3f ? Artifacts.Quality.FINE : Artifacts.Quality.NORMAL, null));
		ItemStack manual = warManual(random, Realm.NASCENT_SOUL);
		if (!manual.isEmpty() && random.nextFloat() < 0.6f) out.add(manual);
		if (random.nextFloat() < 0.35f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (first && random.nextFloat() < 0.2f) out.add(new ItemStack(ModItems.ANCIENT_ESSENCE));
		return out;
	}

	/** Its killing formations: a fog before the hill, a weight at the far camp's way in. */
	static void renewFormations(ServerLevel level, Battlefield.Shape s, BlockPos c) {
		BlockPos fog = c.relative(mainWay(s), Battlefield.HILL_R + 10), weight = c.relative(mainWay(s).getOpposite(), Battlefield.TRENCHES[1]);
		for (BlockPos eye : List.of(fog, weight)) {
			BlockPos at = eye.atY(groundAt(SecretRealm.ANCIENT_BATTLEFIELD, s.seed, eye.getX(), eye.getZ()));
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			put(level, at, ModBlocks.KILLING_FORMATION.defaultBlockState().setValue(KillingFormationBlock.ARRAY,
					eye == fog ? KillingFormationBlock.Array.FOG : KillingFormationBlock.Array.GRAVITY));
		}
	}

	/** The General's bones on his bier: whole, as he was laid, whatever was done to them. */
	static void renewBier(ServerLevel level, Battlefield.Shape s, BlockPos c) {
		BlockPos at = bier(s, c).above();
		level.getChunk(at.getX() >> 4, at.getZ() >> 4);
		put(level, at, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.STAGE, CorpseBlock.Rot.BONES));
		if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) {
			body.ancient(generalName(level), 0, PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), mainWay(s).getOpposite().toYRot(),
					level.getGameTime(), List.of());
		}
	}

	/** The General's name, the same in a world every time. */
	public static String generalName(ServerLevel level) {
		return NpcNames.of(new UUID(level.getSeed(), 0x6E6E5A1L));
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : Battlefield.arrivals(Battlefield.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		Battlefield.Shape s = Battlefield.shape(seed);
		double a = random.nextDouble() * Math.PI * 2, r = Battlefield.HILL_R + 10 + random.nextDouble() * 70;
		int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
		Battlefield.Column col = Battlefield.column(s, x, z);
		return col.kind() == Battlefield.Kind.PLAIN ? c.offset(x, 0, z).atY(col.ground() + 1) : null;
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>(soldierCarried(random));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		Battlefield.Shape s = Battlefield.shape(seed);
		List<Slot> slots = new ArrayList<>();
		RandomSource random = RandomSource.create(seed ^ 0x501D1E5L);
		for (int i = 0; i < 20; i++) {
			double a = random.nextDouble() * Math.PI * 2, r = Battlefield.HILL_R + 8 + random.nextDouble() * (Battlefield.TRENCHES[2] - Battlefield.HILL_R - 10);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			Battlefield.Column col = Battlefield.column(s, x, z);
			if (col.kind() != Battlefield.Kind.PLAIN && col.kind() != Battlefield.Kind.BERM) continue;
			slots.add(new Slot("field" + i, ModEntities.BONE_SOLDIER, c.offset(x, 0, z).atY(col.ground() + 1), realm.guardianRank(i % 5 == 0 ? Stage.MID : Stage.EARLY), false));
		}
		for (int k = 0; k < 4; k++) {
			Frame f = tent(s, c, k);
			for (int side = -1; side <= 1; side += 2) {
				slots.add(new Slot("tent" + k + (side > 0 ? "a" : "b"), ModEntities.BONE_SOLDIER, f.at(5, side * 3, 0), realm.guardianRank(Stage.LATE), false));
			}
		}
		for (int k = 0; k < 4; k++) {
			double a = s.way(k) + Math.PI / 4;
			int x = (int) Math.round(Math.cos(a) * (Battlefield.HILL_R + 3)), z = (int) Math.round(Math.sin(a) * (Battlefield.HILL_R + 3));
			slots.add(new Slot("hill" + k, ModEntities.BONE_SOLDIER, c.offset(x, 0, z).atY(Battlefield.column(s, x, z).ground() + 1), realm.guardianRank(Stage.MID), false));
		}
		return slots;
	}

	@Override
	protected int calm() { return Battlefield.HILL_R; }

	// --- During an opening ---

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("DeadAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Dead", 0);
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		Battlefield.Shape s = Battlefield.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		deadRise(level, s, c, r, now, inside, data);
		if (now % 10 == 4) hill(level, s, c, r, now, inside, data);
		if (now % 20 == 9) {
			// The trial told to whoever comes to the hill; the inheritance at the bier to one who held it.
			ListTag told = r.works.getList("Told", Tag.TAG_STRING);
			BlockPos bier = bier(s, c);
			for (ServerPlayer p : inside) {
				double d = Math.hypot(p.getX() - c.getX(), p.getZ() - c.getZ());
				if (d < Battlefield.HILL_R + 6 && !told.contains(StringTag.valueOf(p.getStringUUID()))) {
					told.add(StringTag.valueOf(p.getStringUUID()));
					p.sendSystemMessage(Component.translatable(ModLang.MSG_FIELD_TRIALS, WAVES, Component.translatable(r.inheritor.isEmpty() ? ModLang.MSG_FIELD_GREAT
							: ModLang.MSG_FIELD_LESSER)).withStyle(ChatFormatting.GOLD));
				}
				if (p.blockPosition().distSqr(bier) < 3 * 3 && passed(r, p.getUUID()) && !passed(r, "Claimed", p.getUUID())) {
					pass(r, "Claimed", p.getUUID());
					inherit(level, r, p);
				}
			}
			r.works.put("Told", told);
		}
	}

	/** Its hour come, for {@link #DEAD_TICKS}: soldiers claw up out of the earth about everyone inside; then the dead lie still again. */
	static void deadRise(ServerLevel level, Battlefield.Shape s, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int stage = r.works.getInt("Dead");
		if (stage == 0 && now >= r.works.getLong("DeadAt")) {
			r.works.putInt("Dead", 1);
			r.works.putLong("DeadEnds", now + DEAD_TICKS);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_DEAD_RISE).withStyle(ChatFormatting.RED));
			for (ServerPlayer p : inside) p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.4f, 0.6f);
		} else if (stage == 1) {
			if (now >= r.works.getLong("DeadEnds")) {
				r.works.putInt("Dead", 2);
				data.setDirty();
				tellInside(level, r.realm, Component.translatable(ModLang.MSG_DEAD_REST).withStyle(ChatFormatting.GRAY));
				return;
			}
			if (now % RISE_EVERY != 0) return;
			int risen = level.getEntitiesOfClass(RealmGuardian.class, new net.minecraft.world.phys.AABB(c).inflate(SecretRealm.BOUND, 128, SecretRealm.BOUND),
					g -> g.slot().startsWith("risen")).size();
			RandomSource random = level.getRandom();
			for (ServerPlayer p : inside) {
				if (risen >= RISEN_MOST) break;
				double a = random.nextDouble() * Math.PI * 2, d = 8 + random.nextDouble() * 6;
				int x = Mth.floor(p.getX() + Math.cos(a) * d), z = Mth.floor(p.getZ() + Math.sin(a) * d);
				if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
				BlockPos at = new BlockPos(x, y, z);
				RealmGuardian soldier = ModEntities.BONE_SOLDIER.create(level);
				if (soldier == null) continue;
				soldier.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
				soldier.setUp(r.realm, r.realm.guardianRank(random.nextBoolean() ? Stage.EARLY : Stage.MID), at, r.openings, "risen" + now + "_" + risen, false);
				level.addFreshEntity(soldier);
				soldier.charge(p);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(at.below())), x + 0.5, y + 0.2, z + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
				level.sendParticles(ParticleTypes.SOUL, x + 0.5, y + 0.5, z + 0.5, 8, 0.3, 0.5, 0.3, 0.02);
				level.playSound(null, at, SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.6f);
				risen++;
			}
		}
	}

	/** Its slot's prefix in the works: the dead risen (no post of their own: never raised again, never counted slain). */
	static boolean isRisen(RealmGuardian g) { return g.slot().startsWith("risen"); }

	/**
	 * The General's hill, held: one stepping into the ring of banners sounds the drum (a player, or one come for it: SecretRealmRivals); each
	 * wave of Battle Spirits brought down, the next comes up the hill at them, the last with the General's Spirit; out of the ring
	 * {@link #OUT_TICKS} (one come for it, four times as long: it chases its foes), fallen or gone, they have lost it; the last brought down,
	 * they have passed (the tomb's barrier lets them by).
	 */
	static void hill(ServerLevel level, Battlefield.Shape s, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		CompoundTag hill = r.works.getCompound("Hill");
		BlockPos m = ringMiddle(c);
		if (!hill.hasUUID("Holder")) {
			LivingEntity first = null;
			for (ServerPlayer p : inside) {
				if (!p.isCreative() && p.isAlive() && !passed(r, p.getUUID()) && inRing(p, m)) {
					first = p;
					break;
				}
			}
			if (first == null) {
				for (CultivatorNpc npc : level.getEntitiesOfClass(CultivatorNpc.class, new AABB(m).inflate(RING_R + 1, 4, RING_R + 1), n -> n.isAlive()
						&& r.realm.getId().equals(n.visitRealm()) && !passed(r, n.getUUID()) && !gaveUp(r, n.getUUID()) && inRing(n, m))) {
					first = npc;
					break;
				}
			}
			if (first == null) return;
			hill = new CompoundTag();
			hill.putUUID("Holder", first.getUUID());
			hill.putInt("Wave", 0);
			hill.putLong("Out", -1);
			r.works.put("Hill", hill);
			data.setDirty();
			level.playSound(null, m, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 4.0f, 0.8f);
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_HILL_DRUM, first.getDisplayName()).withStyle(ChatFormatting.RED));
			nextWave(level, s, c, r, first, hill);
			return;
		}
		UUID holderId = hill.getUUID("Holder");
		Entity holder = level.getEntity(holderId);
		boolean lost = !(holder instanceof LivingEntity e) || !e.isAlive() || SecretRealm.at(e.getX(), e.getZ()) != r.realm;
		if (!lost) {
			LivingEntity e = (LivingEntity) holder;
			// Anyone else stepping into the ring is told whose it is.
			for (ServerPlayer other : inside) if (other != e && inRing(other, m) && now % 60 == 4) other.displayClientMessage(Component.translatable(ModLang.MSG_HILL_BUSY,
					e.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			int allowed = e instanceof ServerPlayer ? OUT_TICKS : OUT_TICKS * 4;
			if (inRing(e, m)) hill.putLong("Out", -1);
			else if (hill.getLong("Out") < 0) hill.putLong("Out", now);
			else if (now - hill.getLong("Out") > allowed) lost = true;
		}
		if (lost) {
			for (RealmGuardian spirit : waveOf(level, hill)) spirit.discard();
			if (holder instanceof ServerPlayer p) p.sendSystemMessage(Component.translatable(ModLang.MSG_HILL_LOST).withStyle(ChatFormatting.GRAY));
			else giveUp(r, holderId); // one come for it doesn't try it twice
			r.works.remove("Hill");
			data.setDirty();
			return;
		}
		LivingEntity e = (LivingEntity) holder;
		if (!waveOf(level, hill).isEmpty()) return;
		if (hill.getInt("Wave") >= WAVES) {
			pass(r, "Passed", e.getUUID());
			r.works.remove("Hill");
			data.setDirty();
			if (e instanceof ServerPlayer p) {
				p.sendSystemMessage(Component.translatable(ModLang.MSG_HILL_HELD).withStyle(ChatFormatting.GOLD));
				p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
				SecretRealms.HUD_SHOWN.remove(p.getUUID()); // their row, and the barrier's word with it, sent again
			}
			return;
		}
		nextWave(level, s, c, r, e, hill);
		r.works.put("Hill", hill);
		data.setDirty();
	}

	static boolean inRing(LivingEntity e, BlockPos m) {
		return Math.hypot(e.getX() - m.getX() - 0.5, e.getZ() - m.getZ() - 0.5) <= RING_R + 0.5 && e.getY() > m.getY() - 3;
	}

	/** The spirits of the wave on the hill now (those still standing). */
	static List<RealmGuardian> waveOf(ServerLevel level, CompoundTag hill) {
		List<RealmGuardian> out = new ArrayList<>();
		for (Tag t : hill.getList("Spirits", Tag.TAG_INT_ARRAY)) {
			UUID id = net.minecraft.nbt.NbtUtils.loadUUID(t);
			if (level.getEntity(id) instanceof RealmGuardian g && g.isAlive()) out.add(g);
		}
		return out;
	}

	/** The next wave up the hill at {@code p}: two more spirits each time and stronger, the last with the General's Spirit at their head. */
	static void nextWave(ServerLevel level, Battlefield.Shape s, BlockPos c, SecretRealms.Record r, LivingEntity p, CompoundTag hill) {
		int wave = hill.getInt("Wave") + 1;
		hill.putInt("Wave", wave);
		ListTag spirits = new ListTag();
		RandomSource random = level.getRandom();
		Stage stage = Stage.values()[Math.min(Stage.values().length - 1, wave - 1)];
		int count = 1 + wave;
		for (int i = 0; i <= count; i++) {
			boolean general = wave == WAVES && i == count;
			if (i == count && !general) break;
			double a = random.nextDouble() * Math.PI * 2, d = Battlefield.HILL_R - 6 + random.nextDouble() * 4;
			int x = c.getX() + (int) Math.round(Math.cos(a) * d), z = c.getZ() + (int) Math.round(Math.sin(a) * d);
			int y = groundAt(r.realm, level.getSeed(), x, z) + 1;
			EntityType<RealmGuardian> type = general ? ModEntities.GENERAL_SPIRIT : ModEntities.BATTLE_SPIRIT;
			RealmGuardian spirit = type.create(level);
			if (spirit == null) continue;
			level.getChunk(x >> 4, z >> 4);
			spirit.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360, 0);
			int rank = general ? PlayerCultivation.rank(Realm.values()[r.realm.meantFor().ordinal() + 1], Stage.EARLY) : r.realm.guardianRank(stage);
			spirit.setUp(r.realm, rank, new BlockPos(x, y, z), r.openings, "", false);
			level.addFreshEntity(spirit);
			spirit.charge(p);
			spirits.add(net.minecraft.nbt.NbtUtils.createUUID(spirit.getUUID()));
			level.sendParticles(ParticleTypes.SOUL, x + 0.5, y + 1, z + 0.5, 16, 0.4, 0.8, 0.4, 0.04);
		}
		hill.put("Spirits", spirits);
		level.playSound(null, ringMiddle(c), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 4.0f, 0.5f);
		if (p instanceof ServerPlayer player) {
			player.sendSystemMessage(Component.translatable(wave == WAVES ? ModLang.MSG_HILL_GENERAL : ModLang.MSG_HILL_WAVE, wave, WAVES).withStyle(ChatFormatting.RED));
		}
	}

	// --- Its inheritance ---

	/** The Great Inheritance: the War General's Banner, and the manual of Blood Battle. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(new ItemStack(ModItems.WAR_GENERALS_BANNER), TechniqueManualItem.of(Technique.BLOOD_BATTLE));
	}

	/** A lesser legacy: a manual of war, or a fine weapon of the old armies; spirit stones with it. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		ItemStack manual = warManual(random, Realm.HEAVENLY_BEING);
		ItemStack first = random.nextBoolean() || manual.isEmpty() ? Artifacts.create(ArtifactKind.SPEAR, ArtifactGrade.EARTH, Artifacts.Quality.FINE, null) : manual;
		return List.of(first, new ItemStack(ModItems.MID_SPIRIT_STONE, 2 + random.nextInt(4)));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_FIELD_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_FIELD_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_FIELD_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** The nearest of the old fallen still holding what it died with. */
	@Override
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) {
		BlockPos best = null;
		double d = 56 * 56;
		for (BlockPos at : bodySpots(Battlefield.shape(seed), c)) {
			double here = at.distSqr(near);
			if (here < d && level.isLoaded(at) && level.getBlockEntity(at) instanceof CorpseBlockEntity body && body.isAncient() && !body.held().isEmpty()) {
				d = here;
				best = at;
			}
		}
		return best;
	}

	/** What an old soldier died with, taken off its bones (they stay where they lie). */
	@Override
	protected List<ItemStack> rivalTake(ServerLevel level, BlockPos at, CultivatorNpc npc) {
		if (!(level.getBlockEntity(at) instanceof CorpseBlockEntity body) || !body.isAncient()) return List.of();
		List<ItemStack> taken = new ArrayList<>(body.held());
		body.held().clear();
		body.setChanged();
		level.playSound(null, at, SoundEvents.SKELETON_STEP, SoundSource.BLOCKS, 0.8f, 0.7f);
		return taken;
	}

	@Override
	protected List<BlockPos> vaults(long seed, BlockPos c) {
		Battlefield.Shape s = Battlefield.shape(seed);
		List<BlockPos> out = new ArrayList<>();
		for (int k = 0; k < 4; k++) out.add(vaultAt(s, c, k));
		return out;
	}

	/** Where one come for it waits to go in at the tomb's door: before its barrier, outside. */
	static BlockPos tombOutside(Battlefield.Shape s, BlockPos c) {
		return tombDoor(s, c).relative(mainWay(s).getOpposite(), 4);
	}

	/** The hill held (stepping into its ring sounds the drum: {@link #hill}), then the General's tomb. */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		Battlefield.Shape s = Battlefield.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		if (passed(r, "Claimed", npc.getUUID())) return null;
		if (passed(r, npc.getUUID())) return new Trial("tomb", tombOutside(s, c), 4, false);
		return new Trial("hill", ringMiddle(c), RING_R - 2, false);
	}

	/**
	 * On the hill: it stands in the ring (the waves come at it, and it fights) till it has held it or lost it; another holding it, it waits a
	 * while, then gives up. At the tomb's door: in, and what waits at the bier is its.
	 */
	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		UUID id = npc.getUUID();
		if (trial.step().equals("tomb")) {
			pass(r, "Claimed", id);
			level.sendParticles(ParticleTypes.SOUL, npc.getX(), npc.getY() + 1, npc.getZ(), 20, 0.4, 0.8, 0.4, 0.04);
			rivalInherits(level, r, npc);
			return true;
		}
		if (passed(r, id) || gaveUp(r, id)) return true;
		CompoundTag hill = r.works.getCompound("Hill");
		if (hill.hasUUID("Holder") && !hill.getUUID("Holder").equals(id) && working > 900) {
			giveUp(r, id);
			return true;
		}
		return false;
	}

	private BattlefieldWorks() {}
}
