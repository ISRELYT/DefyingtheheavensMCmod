package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * The Tomb of a Fallen Immortal's works (SecretRealm#FALLEN_IMMORTAL_TOMB; its land is ImmortalTomb's, the machinery every realm shares
 * RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: in the base's middle the immortal's court, paved and cracked, a withered tree at its heart; round it eight halls,
 *   one for each Law ({@link #lawOf}), each door sealed by its Law (LawSealBlock), its Law's stele within; beyond the court, up its steps,
 *   the sealed sanctum (its door a barrier), his coffin in it; a gate of his palace behind the court; a terrace at each arrival.</li>
 *   <li><b>Renewed each time it stirs</b>: each hall's vault sealed anew for the ancients (that Law's Dao Fragments, immortal spirit
 *   stones), the seals and the sanctum's door whole; the bones of a few earlier visitors.</li>
 *   <li><b>Its guardians</b>: decayed celestial guards before the halls, about the court and on the sanctum's steps; the immortal's own
 *   decayed spirit before its door, its trial's (matched to who challenges it).</li>
 *   <li><b>Its event</b>: some time into each opening the Five Decays that took him spread through it for {@link #DECAY_TICKS}
 *   (ModEffects#DECAYING: blows and armour weakened, as the decays' signs weaken an immortal).</li>
 *   <li><b>Its trial</b>: the sanctum's door lets by one who holds {@link #LAWS_FOR_SANCTUM} Laws, or one who bested his spirit; at his
 *   coffin the Great Inheritance (his glaive, an Immortal-grade artifact, and Dao Fragments) once in a world, after it a lesser legacy each
 *   opening.</li>
 * </ul>
 */
public final class ImmortalTombWorks extends RealmWorks {
	static final ImmortalTombWorks WORKS = new ImmortalTombWorks();
	/** The decays spread this long (a minute), renewed on those inside every this often. */
	static final int DECAY_TICKS = 1200, DECAY_EVERY = 40;
	/** So many Laws held (a level of each) open the sanctum's door without a fight. */
	static final int LAWS_FOR_SANCTUM = 3;
	/** The halls' doors and the court's; the sanctum's terrace height. */
	static final int HALLS = 8, RAISED = 3;

	// --- Where things are ---

	static Direction mainWay(ImmortalTomb.Shape s) {
		return Direction.fromDelta((int) Math.round(Math.cos(s.mainTurn)), 0, (int) Math.round(Math.sin(s.mainTurn)));
	}

	/** The tomb's frame: the court's middle at the base's top, forward toward the sanctum (f), s to its right. */
	static Frame tomb(ImmortalTomb.Shape s, BlockPos c) {
		return new Frame(c.atY(ImmortalTomb.BASE_Y), mainWay(s));
	}

	/** The Law whose hall is the {@code k}th. */
	static Law lawOf(int k) {
		return Law.values()[k % Law.values().length];
	}

	/** The {@code k}th Law hall's frame: its middle at the base's top, forward toward the court (its door that way): two halls to a side of the court. */
	static Frame lawHall(ImmortalTomb.Shape s, BlockPos c, int k) {
		Frame t = tomb(s, c);
		int off = k % 2 == 0 ? -14 : 14;
		return switch (k / 2) {
			case 0 -> new Frame(t.at(off, -38, 0), t.right());
			case 1 -> new Frame(t.at(off, 38, 0), t.right().getOpposite());
			case 2 -> new Frame(t.at(-38, off, 0), t.forward());
			default -> new Frame(t.at(38, off, 0), t.forward().getOpposite());
		};
	}

	/** A Law hall's door (its seal): three wide, three high, in its front wall. */
	static List<BlockPos> hallDoor(Frame h) {
		List<BlockPos> door = new ArrayList<>();
		for (int sd = -1; sd <= 1; sd++) for (int y = 1; y <= 3; y++) door.add(h.at(6, sd, y));
		return door;
	}

	static BlockPos hallVault(Frame h) { return h.at(-4, 3, 1); }

	/** The sanctum's door (its barrier): three wide, four high, in its front wall, up its terrace. */
	static List<BlockPos> sanctumDoor(Frame t) {
		List<BlockPos> door = new ArrayList<>();
		for (int sd = -1; sd <= 1; sd++) for (int y = RAISED + 1; y <= RAISED + 4; y++) door.add(t.at(56, sd, y));
		return door;
	}

	/** The immortal's spirit's post: before the sanctum's door. */
	static BlockPos spiritPost(Frame t) { return t.at(52, 0, RAISED + 1); }

	/** Before his coffin, where his legacy waits. */
	static BlockPos coffin(Frame t) { return t.at(68, 0, RAISED + 1); }

	/** Whether local (f, s, y) of the tomb's frame is of the sanctum (its walls, roof and floor); within it, inside it. */
	static boolean ofSanctum(int[] at) {
		return at[0] >= 55 && at[0] <= 77 && Math.abs(at[1]) <= 11 && at[2] >= 0 && at[2] <= 22;
	}

	static boolean inSanctum(int[] at) {
		return at[0] >= 57 && at[0] <= 75 && Math.abs(at[1]) <= 9 && at[2] >= RAISED && at[2] <= RAISED + 8;
	}

	/** Whether local (f, s, y) of a Law hall's frame is of it. */
	static boolean ofHall(int[] at) {
		return Math.abs(at[0]) <= 7 && Math.abs(at[1]) <= 6 && at[2] >= -1 && at[2] <= 12;
	}

	/** A Law's stone, in its colour, for its stele. */
	static BlockState stele(Law law) {
		return (switch (law) {
			case METAL -> Blocks.WHITE_GLAZED_TERRACOTTA;
			case WOOD -> Blocks.GREEN_GLAZED_TERRACOTTA;
			case WATER -> Blocks.BLUE_GLAZED_TERRACOTTA;
			case FIRE -> Blocks.RED_GLAZED_TERRACOTTA;
			case EARTH -> Blocks.YELLOW_GLAZED_TERRACOTTA;
			case SPACE -> Blocks.PURPLE_GLAZED_TERRACOTTA;
			case TIME -> Blocks.CYAN_GLAZED_TERRACOTTA;
			case LIFE_AND_DEATH -> Blocks.BLACK_GLAZED_TERRACOTTA;
		}).defaultBlockState();
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(seed);
		steps.add(() -> court(level, s, c));
		for (int k = 0; k < HALLS; k++) {
			int i = k;
			steps.add(() -> hallLaid(level, s, c, i));
		}
		steps.add(() -> sanctumTerrace(level, s, c));
		steps.add(() -> sanctumLaid(level, s, c));
		steps.add(() -> gate(level, s, c));
		steps.add(() -> arrivalTerraces(level, s, c));
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(seed);
		steps.add(() -> renewSeals(level, s, c));
		steps.add(() -> renewVaults(level, s, c));
	}

	// --- Building ---

	static final Block ROOF = Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, RIDGE = Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, ROOF_FULL = Blocks.WAXED_OXIDIZED_CUT_COPPER;

	/** The palace's paving: white stone and quartz, lines of gold-yellow stone through it, cracked here and there. */
	static BlockState paving(int f, int sd, RandomSource random) {
		if (random.nextFloat() < 0.1f) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		if (f % 8 == 0 || sd % 8 == 0) return Blocks.SMOOTH_SANDSTONE.defaultBlockState();
		return ((f + sd) % 2 == 0 ? Blocks.SMOOTH_QUARTZ : Blocks.POLISHED_DIORITE).defaultBlockState();
	}

	/** The court: paved, a lantern post at each corner, the withered tree at its heart; the paths from it to each hall, the sanctum and the gate. */
	static void court(ServerLevel level, ImmortalTomb.Shape s, BlockPos c) {
		Frame t = tomb(s, c);
		RandomSource random = RandomSource.create(s.seed ^ 0x7031C0L);
		for (int cx = (c.getX() - 64) >> 4; cx <= (c.getX() + 64) >> 4; cx++) for (int cz = (c.getZ() - 64) >> 4; cz <= (c.getZ() + 64) >> 4; cz++) level.getChunk(cx, cz);
		for (int f = -24; f <= 24; f++) {
			for (int sd = -24; sd <= 24; sd++) {
				put(level, t.at(f, sd, 0), paving(f, sd, random));
				for (int y = 1; y <= 3; y++) put(level, t.at(f, sd, y), Blocks.AIR.defaultBlockState());
				if (random.nextFloat() < 0.02f && Math.abs(f) > 3 && Math.abs(sd) > 3) put(level, t.at(f, sd, 1), Blocks.DEAD_BUSH.defaultBlockState());
			}
		}
		// The paths: to each hall's door, up to the sanctum's steps, back to the gate.
		for (int k = 0; k < HALLS; k++) {
			Frame h = lawHall(s, c, k);
			for (int f = 7; f <= 14; f++) for (int sd = -1; sd <= 1; sd++) put(level, h.at(f, sd, 0), paving(f, sd, random));
		}
		for (int f = 25; f <= 47; f++) for (int sd = -2; sd <= 2; sd++) put(level, t.at(f, sd, 0), paving(f, sd, random));
		for (int f = -60; f <= -25; f++) for (int sd = -2; sd <= 2; sd++) put(level, t.at(f, sd, 0), paving(f, sd, random));
		for (int i = 0; i < 4; i++) {
			BlockPos post = t.at(i < 2 ? -22 : 22, i % 2 == 0 ? -22 : 22, 0);
			for (int y = 1; y <= 3; y++) put(level, post.above(y), Blocks.QUARTZ_PILLAR.defaultBlockState());
			put(level, post.above(4), lantern(false));
		}
		witheredTree(level, t.origin(), random);
		join(level, t.at(-62, -46, -1), t.at(48, 46, 14));
	}

	/** The immortal's tree at the court's heart, withered with him: a great bare trunk, its boughs dead. */
	static void witheredTree(ServerLevel level, BlockPos base, RandomSource random) {
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) put(level, base.offset(dx, 0, dz), Blocks.PODZOL.defaultBlockState());
		int height = 9;
		for (int y = 1; y <= height; y++) put(level, base.above(y), log(Blocks.DARK_OAK_LOG));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockPos at = base.above(height - 3 + random.nextInt(3));
			for (int k = 1; k <= 3 + random.nextInt(2); k++) {
				at = at.relative(d);
				if (k % 2 == 0) at = at.above();
				put(level, at, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, d.getAxis()));
			}
		}
	}

	/** The {@code k}th Law's hall: walls of pale gold stone on carved columns, a roof of old green bronze, its door sealed by its Law; within, its stele. */
	static void hallLaid(ServerLevel level, ImmortalTomb.Shape s, BlockPos c, int k) {
		Frame h = lawHall(s, c, k);
		Law law = lawOf(k);
		level.getChunk(h.origin().getX() >> 4, h.origin().getZ() >> 4);
		for (int f = -6; f <= 6; f++) {
			for (int sd = -5; sd <= 5; sd++) {
				put(level, h.at(f, sd, 0), Blocks.POLISHED_DIORITE.defaultBlockState());
				boolean wall = Math.abs(f) == 6 || Math.abs(sd) == 5;
				boolean corner = Math.abs(f) == 6 && Math.abs(sd) == 5;
				for (int y = 1; y <= 5; y++) {
					put(level, h.at(f, sd, y), !wall ? Blocks.AIR.defaultBlockState() : corner ? Blocks.CHISELED_SANDSTONE.defaultBlockState()
							: y == 5 ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState());
				}
			}
		}
		for (BlockPos at : hallDoor(h)) put(level, at, LawSealBlock.of(law));
		put(level, h.at(-4, 0, 1), Blocks.CHISELED_SANDSTONE.defaultBlockState());
		put(level, h.at(-4, 0, 2), stele(law));
		put(level, h.at(-4, 0, 3), stele(law));
		put(level, h.at(-4, 0, 4), Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
		HerbValleyWorks.hipRoof(level, h, -7, 7, -6, 6, 6, ROOF, RIDGE, ROOF_FULL);
		put(level, h.at(1, 0, 5), lantern(true));
		join(level, h.at(-8, -7, -1), h.at(8, 7, 12));
	}

	/** The sanctum's terrace: raised up {@link #RAISED} above the court, steps up its front. */
	static void sanctumTerrace(ServerLevel level, ImmortalTomb.Shape s, BlockPos c) {
		Frame t = tomb(s, c);
		RandomSource random = RandomSource.create(s.seed ^ 0x7031C5L);
		for (int cx = (t.at(64, 0, 0).getX() - 24) >> 4; cx <= (t.at(64, 0, 0).getX() + 24) >> 4; cx++) {
			for (int cz = (t.at(64, 0, 0).getZ() - 24) >> 4; cz <= (t.at(64, 0, 0).getZ() + 24) >> 4; cz++) level.getChunk(cx, cz);
		}
		for (int f = 50; f <= 78; f++) {
			for (int sd = -14; sd <= 14; sd++) {
				boolean rim = f == 50 || f == 78 || Math.abs(sd) == 14;
				for (int y = 0; y <= RAISED; y++) put(level, t.at(f, sd, y), y == RAISED ? (rim ? Blocks.CUT_SANDSTONE.defaultBlockState() : paving(f, sd, random))
						: Blocks.SMOOTH_SANDSTONE.defaultBlockState());
				for (int y = RAISED + 1; y <= RAISED + 3; y++) put(level, t.at(f, sd, y), Blocks.AIR.defaultBlockState());
			}
		}
		for (int k = 1; k <= RAISED; k++) {
			for (int sd = -3; sd <= 3; sd++) {
				BlockPos step = t.at(50 - RAISED - 1 + k, sd, k);
				put(level, step, stairs(Blocks.SANDSTONE_STAIRS, t.forward(), false));
				for (int y = 1; y < k; y++) put(level, step.atY(step.getY() - y), Blocks.SMOOTH_SANDSTONE.defaultBlockState());
			}
		}
		join(level, t.at(45, -15, -1), t.at(79, 15, RAISED + 4));
	}

	/** The sanctum: walls of pale gold stone, its door the barrier, a roof of old bronze; within, his coffin of black stone, candles about it. */
	static void sanctumLaid(ServerLevel level, ImmortalTomb.Shape s, BlockPos c) {
		Frame t = tomb(s, c);
		int floor = RAISED;
		for (int f = 56; f <= 76; f++) {
			for (int sd = -10; sd <= 10; sd++) {
				boolean wall = f == 56 || f == 76 || Math.abs(sd) == 10;
				boolean column = wall && (Math.abs(sd) % 5 == 0 && (f == 56 || f == 76) || Math.abs(sd) == 10 && (f - 56) % 5 == 0);
				for (int y = floor + 1; y <= floor + 8; y++) {
					put(level, t.at(f, sd, y), !wall ? Blocks.AIR.defaultBlockState() : column ? Blocks.CHISELED_SANDSTONE.defaultBlockState()
							: y == floor + 8 ? Blocks.CUT_SANDSTONE.defaultBlockState() : Blocks.SMOOTH_SANDSTONE.defaultBlockState());
				}
			}
		}
		for (BlockPos at : sanctumDoor(t)) put(level, at, ModBlocks.TRIAL_BARRIER.defaultBlockState());
		for (int sd = -2; sd <= 2; sd++) put(level, t.at(56, sd, floor + 5), Blocks.GOLD_BLOCK.defaultBlockState());
		// His coffin of black stone on its plinth, candles at its corners.
		for (int f = 71; f <= 73; f++) {
			put(level, t.at(f, 0, floor + 1), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
			put(level, t.at(f, 0, floor + 2), slab(Blocks.POLISHED_BLACKSTONE_SLAB, false));
		}
		for (int f = 70; f <= 74; f += 4) {
			for (int sd = -1; sd <= 1; sd += 2) put(level, t.at(f, sd, floor + 1), Blocks.YELLOW_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		}
		HerbValleyWorks.hipRoof(level, t, 55, 77, -11, 11, floor + 9, ROOF, RIDGE, ROOF_FULL);
		for (int f : new int[] {60, 66, 72}) put(level, t.at(f, 0, floor + 8), lantern(true));
		join(level, t.at(54, -12, floor), t.at(78, 12, floor + 22));
	}

	/** The palace's gate behind the court: two columns of carved quartz, a lintel of gold-yellow stone, a little bronze roof. */
	static void gate(ServerLevel level, ImmortalTomb.Shape s, BlockPos c) {
		Frame t = tomb(s, c);
		level.getChunk(t.at(-62, 0, 0).getX() >> 4, t.at(-62, 0, 0).getZ() >> 4);
		for (int sd = -4; sd <= 4; sd += 8) for (int y = 1; y <= 6; y++) put(level, t.at(-62, sd, y), Blocks.QUARTZ_PILLAR.defaultBlockState());
		for (int sd = -5; sd <= 5; sd++) put(level, t.at(-62, sd, 6), Blocks.CUT_SANDSTONE.defaultBlockState());
		HerbValleyWorks.hipRoof(level, t, -63, -61, -6, 6, 7, ROOF, RIDGE, ROOF_FULL);
		join(level, t.at(-64, -7, 0), t.at(-60, 7, 10));
	}

	static void arrivalTerraces(ServerLevel level, ImmortalTomb.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0x7031A7L);
		for (BlockPos at : WORKS.arrivalSpots(s.seed, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz > 12) continue;
					put(level, at.offset(dx, 0, dz), paving(dx, dz, random));
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

	/** Every hall's seal and the sanctum's door whole. */
	static void renewSeals(ServerLevel level, ImmortalTomb.Shape s, BlockPos c) {
		for (int k = 0; k < HALLS; k++) {
			Frame h = lawHall(s, c, k);
			level.getChunk(h.origin().getX() >> 4, h.origin().getZ() >> 4);
			for (BlockPos at : hallDoor(h)) if (!level.getBlockState(at).is(ModBlocks.LAW_SEAL)) put(level, at, LawSealBlock.of(lawOf(k)));
		}
		Frame t = tomb(s, c);
		level.getChunk(t.at(56, 0, 0).getX() >> 4, t.at(56, 0, 0).getZ() >> 4);
		for (BlockPos at : sanctumDoor(t)) if (!level.getBlockState(at).is(ModBlocks.TRIAL_BARRIER)) put(level, at, ModBlocks.TRIAL_BARRIER.defaultBlockState());
	}

	/** Each hall's vault sealed anew for the ancients: its Law's Dao Fragments and immortal spirit stones. */
	static void renewVaults(ServerLevel level, ImmortalTomb.Shape s, BlockPos c) {
		RandomSource random = level.getRandom();
		for (int k = 0; k < HALLS; k++) {
			BlockPos at = hallVault(lawHall(s, c, k));
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(StorageFormations.Tier.HIGH).defaultBlockState());
			List<ItemStack> hoard = new ArrayList<>();
			ItemStack fragments = DaoFragmentItem.of(lawOf(k));
			fragments.setCount(1 + random.nextInt(2));
			hoard.add(fragments);
			hoard.add(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE, 1 + random.nextInt(2)));
			if (random.nextFloat() < 0.25f) hoard.add(new ItemStack(ModItems.IMMORTAL_JADE));
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, hoard);
		}
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : ImmortalTomb.arrivals(ImmortalTomb.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	/** An earlier visitor's bones: somewhere on the base, out of the palace. */
	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(seed);
		double a = random.nextDouble() * Math.PI * 2, d = 50 + random.nextDouble() * 30;
		int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
		ImmortalTomb.Column col = ImmortalTomb.column(s, x, z);
		BlockPos at = c.offset(x, 0, z).atY(col.ground() + 1);
		if (col.kind() != ImmortalTomb.Kind.BASE || ofSanctum(tomb(s, c).local(at))) return null;
		for (int k = 0; k < HALLS; k++) if (ofHall(lawHall(s, c, k).local(at))) return null;
		return at;
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 1 + random.nextInt(3)));
		if (random.nextFloat() < 0.3f) carried.add(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (random.nextFloat() < 0.08f) carried.add(DaoFragmentItem.of(Law.values()[random.nextInt(Law.values().length)]));
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(seed);
		List<Slot> slots = new ArrayList<>();
		for (int k = 0; k < HALLS; k++) {
			slots.add(new Slot("hall" + k, ModEntities.DECAYED_GUARD, lawHall(s, c, k).at(9, 2, 1), realm.guardianRank(Stage.MID), false));
		}
		Frame t = tomb(s, c);
		for (int i = 0; i < 4; i++) {
			slots.add(new Slot("court" + i, ModEntities.DECAYED_GUARD, t.at(i < 2 ? -18 : 18, i % 2 == 0 ? -18 : 18, 1), realm.guardianRank(Stage.EARLY), false));
		}
		for (int side = -1; side <= 1; side += 2) {
			slots.add(new Slot("steps" + (side + 1), ModEntities.DECAYED_GUARD, t.at(46, side * 5, 1), realm.guardianRank(Stage.LATE), false));
		}
		slots.add(new Slot("immortal", ModEntities.DECAYED_IMMORTAL, spiritPost(t), realm.guardianRank(Stage.EARLY), true));
		return slots;
	}

	/** The court and its halls hold: the closing quake splits only the base's rim and its islets. */
	@Override
	protected int calm() { return 84; }

	/** Its halls and sanctum hold: gone into by their doors, never round them. */
	@Override
	protected String holds(ServerLevel level, SecretRealms.Record r, BlockPos pos) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		if (ofSanctum(tomb(s, c).local(pos))) return ModLang.MSG_IMMORTAL_TOMB_HOLDS;
		for (int k = 0; k < HALLS; k++) if (ofHall(lawHall(s, c, k).local(pos))) return ModLang.MSG_IMMORTAL_TOMB_HOLDS;
		return null;
	}

	/** The sanctum's door lets by one who bested his spirit, or one who holds enough Laws (known to it once they came to its door). */
	@Override
	protected boolean barrierPasses(SecretRealms.Record r, UUID id) {
		return passed(r, id) || passed(r, "Laws", id);
	}

	/** How many Laws {@code p} holds (a level of each). */
	static int lawsHeld(ServerPlayer p) {
		LawState laws = CultivationManager.get(p).laws();
		int held = 0;
		for (Law law : Law.values()) if (laws.level(law) >= 1) held++;
		return held;
	}

	@Override
	protected MutableComponent challengeMessage(Component rank) {
		return Component.translatable(ModLang.MSG_IMMORTAL_TOMB_SPIRIT_WAKES, rank);
	}

	@Override
	protected MutableComponent gatePassedMessage() {
		return Component.translatable(ModLang.MSG_IMMORTAL_TOMB_SPIRIT_BOWS);
	}

	// --- During an opening ---

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("DecayAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Decay", 0);
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		decays(level, r, now, inside, data);
		if (now % 20 == 13) {
			Frame t = tomb(s, c);
			BlockPos door = t.at(55, 0, RAISED + 1), coffin = coffin(t);
			ListTag told = r.works.getList("Told", Tag.TAG_STRING);
			for (ServerPlayer p : inside) {
				// The trial told to whoever comes to the court; the sanctum's door knowing the Laws of one who comes to it; the legacy at his coffin.
				if (p.blockPosition().distSqr(t.origin()) < 40 * 40 && !told.contains(StringTag.valueOf(p.getStringUUID()))) {
					told.add(StringTag.valueOf(p.getStringUUID()));
					p.sendSystemMessage(Component.translatable(ModLang.MSG_IMMORTAL_TOMB_TRIALS, LAWS_FOR_SANCTUM, Component.translatable(r.inheritor.isEmpty()
							? ModLang.MSG_IMMORTAL_TOMB_GREAT : ModLang.MSG_IMMORTAL_TOMB_LESSER)).withStyle(ChatFormatting.GOLD));
				}
				if (p.blockPosition().distSqr(door) < 8 * 8 && !passed(r, "Laws", p.getUUID()) && lawsHeld(p) >= LAWS_FOR_SANCTUM) {
					pass(r, "Laws", p.getUUID());
					data.setDirty();
					SecretRealms.HUD_SHOWN.remove(p.getUUID());
					p.sendSystemMessage(Component.translatable(ModLang.MSG_IMMORTAL_TOMB_LAWS_KNOWN).withStyle(ChatFormatting.GOLD));
				}
				if (p.blockPosition().distSqr(coffin) < 3 * 3 && barrierPasses(r, p.getUUID()) && !passed(r, "Claimed", p.getUUID())) {
					pass(r, "Passed", p.getUUID());
					pass(r, "Claimed", p.getUUID());
					inherit(level, r, p);
				}
			}
			r.works.put("Told", told);
		}
	}

	/** The Five Decays that took him, spreading through it for a minute: all inside weakened as the decays' signs weaken an immortal. */
	private static void decays(ServerLevel level, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int decay = r.works.getInt("Decay");
		if (decay == 0 && now >= r.works.getLong("DecayAt")) {
			r.works.putInt("Decay", 1);
			r.works.putLong("DecayEnds", now + DECAY_TICKS);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_IMMORTAL_TOMB_DECAY).withStyle(ChatFormatting.DARK_GREEN));
			decay = 1;
		}
		if (decay != 1) return;
		if (now >= r.works.getLong("DecayEnds")) {
			r.works.putInt("Decay", 2);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_IMMORTAL_TOMB_DECAY_ENDS).withStyle(ChatFormatting.GRAY));
			return;
		}
		if (now % DECAY_EVERY != 0) return;
		for (ServerPlayer p : inside) {
			if (p.isCreative() || p.isSpectator()) continue;
			p.addEffect(new MobEffectInstance(ModEffects.DECAYING, DECAY_EVERY + 20, 0, false, true, true));
			level.sendParticles(ParticleTypes.ASH, p.getX(), p.getY() + 1, p.getZ(), 30, 0.6, 0.9, 0.6, 0.01);
		}
	}

	// --- Its inheritance ---

	/** His glaive: an Immortal-grade artifact, Perfect, named for him. */
	static ItemStack glaive() {
		ItemStack glaive = Artifacts.create(ArtifactKind.GLAIVE, ArtifactGrade.IMMORTAL, Artifacts.Quality.PERFECT, null);
		glaive.setHoverName(Component.translatable(ModLang.ITEM_FALLEN_IMMORTAL_GLAIVE).withStyle(ChatFormatting.GOLD));
		return glaive;
	}

	/** The Great Inheritance: his glaive; Dao Fragments of three Laws. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		List<ItemStack> out = new ArrayList<>();
		out.add(glaive());
		List<Law> laws = new ArrayList<>(List.of(Law.values()));
		java.util.Collections.shuffle(laws, new java.util.Random(level.getRandom().nextLong()));
		for (int k = 0; k < 3; k++) out.add(DaoFragmentItem.of(laws.get(k)));
		return out;
	}

	/** A lesser legacy: a Dao Fragment, immortal spirit stones. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		return List.of(DaoFragmentItem.of(Law.values()[random.nextInt(Law.values().length)]), new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE, 2));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_IMMORTAL_TOMB_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_IMMORTAL_TOMB_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_IMMORTAL_TOMB_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** On the base out of the palace, the nearer the court the later. */
	@Override
	protected BlockPos rivalStart(ServerLevel level, long seed, BlockPos c, double along, RandomSource random) {
		ImmortalTomb.Shape s = ImmortalTomb.shape(seed);
		Frame t = tomb(s, c);
		int f = (int) Math.round(-80 + along * 40) + random.nextInt(5) - 2;
		return t.at(f, random.nextInt(9) - 4, 0);
	}

	/** Its spirit before the sanctum; through the door; his coffin. */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		Frame t = tomb(ImmortalTomb.shape(level.getSeed()), r.realm.centre());
		UUID id = npc.getUUID();
		if (passed(r, "Claimed", id)) return null;
		if (!passed(r, id)) return new Trial("spirit", spiritPost(t), 3.5, false);
		if (!inSanctum(t.local(npc.blockPosition()))) return new Trial("door", t.at(54, 0, RAISED + 1), 2.5, true);
		return new Trial("coffin", coffin(t), 2.0, true);
	}

	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		switch (trial.step()) {
			case "spirit" -> {
				return challengeAt(level, r, npc, trial.mark(), working);
			}
			case "door" -> {
				BlockPos in = tomb(ImmortalTomb.shape(level.getSeed()), r.realm.centre()).at(60, 0, RAISED + 1);
				npc.getNavigation().stop();
				npc.teleportTo(in.getX() + 0.5, in.getY(), in.getZ() + 0.5);
			}
			case "coffin" -> {
				pass(r, "Claimed", npc.getUUID());
				rivalInherits(level, r, npc);
			}
			default -> {
			}
		}
		return true;
	}

	private ImmortalTombWorks() {}
}
