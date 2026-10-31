package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The Yellow Springs Hollow's works (SecretRealm#YELLOW_SPRINGS_HOLLOW, a realm of the dead in the Netherworld; its land is SpringsHollow's,
 * the machinery every realm shares RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: bridges of black stone between the drowned city's islands, lit by soul lanterns; the ruins of its houses on its
 *   islands and of its streets under the shallows; the Ferryman's landing, a jetty out into the springs with his boat moored by it, and a
 *   jetty on the tomb's island with a bell to call him back; on the island's mound the Ghost King's tomb, its door the trial's barrier; a
 *   terrace at each arrival.</li>
 *   <li><b>Renewed each time it stirs</b>: the Other Shore Flowers on the islands (older by every year it slept); the city's two vaults
 *   sealed anew for the ancients; the tomb's barrier whole and its king's bones in place; the bones of a few earlier visitors.</li>
 *   <li><b>Its guardians</b>: Drowned Shades about the islands and bridges and before the tomb, more risen from the springs on the lantern
 *   night; the Ferryman at his landing.</li>
 *   <li><b>The springs</b>: their deep ring about the tomb's island drags down and withers whoever swims it ({@link #pull}).</li>
 *   <li><b>Its event</b>: some time into each opening the lantern night ({@link #NIGHT_TICKS}): lanterns of the dead rise from the water, the
 *   Other Shore Flowers bloom a century and more ({@link #NIGHT_YEARS}), the drowned rise, and the Ferryman carries all who ask.</li>
 *   <li><b>Its trial</b>: the Ferryman's judgement of who would cross: the righteous he takes for nothing, the neutral for his toll (Yin Jade
 *   or spirit stones), the devilish must best him, matched to them; then his boat across, and the tomb's door lets the judged by. At the
 *   Ghost King's coffin the Great Inheritance (his foundation method, the Six Paths Reincarnation Canon) once in a world, after it a lesser
 *   legacy each opening.</li>
 * </ul>
 */
public final class SpringsHollowWorks extends RealmWorks {
	static final SpringsHollowWorks WORKS = new SpringsHollowWorks();
	/** The lantern night lasts this long (two minutes); the Other Shore Flowers bloom this much older with it. */
	static final int NIGHT_TICKS = 2400, NIGHT_YEARS = 300;
	/** The crossing takes this long (ticks); the drowned city's ruins under the shallows. */
	static final int FERRY_TICKS = 80, RUINS = 26;
	/** The Ferryman's toll: so much Yin Jade, or so many Mid spirit stones. */
	static final int TOLL_JADE = 3, TOLL_STONES = 2;
	/** The tomb's floor (its hall's), on the island's mound. */
	static final int TOMB_FLOOR = SpringsHollow.ISLAND_Y + 3;

	// --- Where things are ---

	static Direction mainWay(SpringsHollow.Shape s) {
		return Direction.fromDelta((int) Math.round(Math.cos(s.mainTurn)), 0, (int) Math.round(Math.sin(s.mainTurn)));
	}

	/** The way's frame: the hollow's middle at the islands' height, forward along the main way outward to the landing (f), s to its right. */
	static Frame way(SpringsHollow.Shape s, BlockPos c) {
		return new Frame(c.atY(SpringsHollow.ISLAND_Y), mainWay(s));
	}

	/** The tomb's frame: its middle the island's heart at the tomb's floor, facing the landing. */
	static Frame tomb(SpringsHollow.Shape s, BlockPos c) {
		return new Frame(c.atY(TOMB_FLOOR), mainWay(s));
	}

	/** The Ferryman's post: at the near jetty's end over the springs. */
	static BlockPos ferryPost(Frame w) { return w.at(SpringsHollow.RIVER_R - 4, 0, 1); }

	/** Where the ferried are set down: at the near jetty's root; on the far jetty. */
	static BlockPos nearLanding(Frame w) { return w.at(SpringsHollow.RIVER_R + 1, 0, 1); }

	static BlockPos farLanding(Frame w) { return w.at(SpringsHollow.TOMB_R + 3, 0, 1); }

	/** The bell on the far jetty that calls the Ferryman back. */
	static BlockPos bell(Frame w) { return w.at(SpringsHollow.TOMB_R + 1, 2, 1); }

	/** Before the Ghost King's coffin, where his legacy waits. */
	static BlockPos coffin(Frame t) { return t.at(-1, 0, 1); }

	/** The tomb's doorway (its barrier): three wide, three high, in its front wall. */
	static List<BlockPos> doorway(Frame t) {
		List<BlockPos> door = new ArrayList<>();
		for (int sd = -1; sd <= 1; sd++) for (int y = 1; y <= 3; y++) door.add(t.at(5, sd, y));
		return door;
	}

	/** Whether local (f, s, y) of the tomb's frame lies in its hall. */
	static boolean inTomb(int[] at) {
		return at[0] >= -6 && at[0] <= 4 && Math.abs(at[1]) <= 5 && at[2] >= 0 && at[2] <= 7;
	}

	static BlockPos isleMiddle(SpringsHollow.Shape s, BlockPos c, SpringsHollow.Isle isle) {
		return c.offset(isle.x(), 0, isle.z()).atY(SpringsHollow.ISLAND_Y);
	}

	/** An island's frame: at its middle, facing the hollow's middle. */
	static Frame isleFrame(SpringsHollow.Shape s, BlockPos c, SpringsHollow.Isle isle) {
		double cos = -isle.x(), sin = -isle.z();
		Direction in = Math.abs(cos) > Math.abs(sin) ? (cos > 0 ? Direction.EAST : Direction.WEST) : (sin > 0 ? Direction.SOUTH : Direction.NORTH);
		return new Frame(isleMiddle(s, c, isle), in);
	}

	/** The city's islands that keep vaults: its two largest. */
	static List<SpringsHollow.Isle> vaultIsles(SpringsHollow.Shape s) {
		List<SpringsHollow.Isle> city = new ArrayList<>();
		for (SpringsHollow.Isle isle : s.isles) if (isle.role().equals("city")) city.add(isle);
		city.sort((a, b) -> b.r() - a.r());
		return city.subList(0, Math.min(2, city.size()));
	}

	static BlockPos vaultAt(SpringsHollow.Shape s, BlockPos c, SpringsHollow.Isle isle) {
		return isleFrame(s, c, isle).at(-2, 0, 1);
	}

	/** The Other Shore Flowers' places: two on each island of the city and the landing's, three on the tomb's island beside the tomb. */
	static List<BlockPos> flowerSpots(SpringsHollow.Shape s, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		RandomSource random = RandomSource.create(s.seed ^ 0x0F10E5L);
		for (SpringsHollow.Isle isle : s.isles) {
			if (isle.role().equals("arrival")) continue;
			for (int k = 0, tries = 0; k < 2 && tries < 20; tries++) {
				double a = random.nextDouble() * Math.PI * 2, d = isle.r() * (0.5 + 0.25 * random.nextDouble());
				int x = isle.x() + (int) Math.round(Math.cos(a) * d), z = isle.z() + (int) Math.round(Math.sin(a) * d);
				SpringsHollow.Column col = SpringsHollow.column(s, x, z);
				if (col.kind() != SpringsHollow.Kind.ISLE) continue;
				out.add(c.offset(x, 0, z).atY(col.ground() + 1));
				k++;
			}
		}
		Frame t = tomb(s, c);
		for (int sd = -14; sd <= 14; sd += 28) {
			BlockPos at = t.at(-4, sd, 0);
			out.add(at.atY(groundAt(SecretRealm.YELLOW_SPRINGS_HOLLOW, s.seed, at.getX(), at.getZ()) + 1));
		}
		BlockPos back = t.at(-15, 0, 0);
		out.add(back.atY(groundAt(SecretRealm.YELLOW_SPRINGS_HOLLOW, s.seed, back.getX(), back.getZ()) + 1));
		return out;
	}

	/** The bridges: each outer island joined to the nearest not yet joined (the shortest tree of them), none over the springs' ring. */
	static List<int[]> bridges(SpringsHollow.Shape s) {
		List<SpringsHollow.Isle> isles = s.isles;
		boolean[] joined = new boolean[isles.size()];
		joined[0] = true;
		List<int[]> out = new ArrayList<>();
		for (int k = 1; k < isles.size(); k++) {
			int from = -1, to = -1;
			double best = Double.MAX_VALUE;
			for (int a = 0; a < isles.size(); a++) {
				if (!joined[a]) continue;
				for (int b = 0; b < isles.size(); b++) {
					if (joined[b]) continue;
					SpringsHollow.Isle ia = isles.get(a), ib = isles.get(b);
					double d = Math.hypot(ia.x() - ib.x(), ia.z() - ib.z()) - ia.r() - ib.r();
					if (d < best && clearOfSprings(ia, ib)) {
						best = d;
						from = a;
						to = b;
					}
				}
			}
			if (to < 0) break;
			joined[to] = true;
			out.add(new int[] {from, to});
		}
		return out;
	}

	/** Whether the line between two islands' middles keeps off the springs' ring (the nearest it comes to the hollow's middle). */
	static boolean clearOfSprings(SpringsHollow.Isle a, SpringsHollow.Isle b) {
		double dx = b.x() - a.x(), dz = b.z() - a.z(), len2 = dx * dx + dz * dz;
		double t = len2 == 0 ? 0 : Mth.clamp(-(a.x() * dx + a.z() * dz) / len2, 0, 1);
		return Math.hypot(a.x() + dx * t, a.z() + dz * t) > SpringsHollow.RIVER_R + 6;
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		List<int[]> bridges = bridges(s);
		for (int k = 0; k < bridges.size(); k++) {
			int[] b = bridges.get(k);
			steps.add(() -> bridge(level, s, c, s.isles.get(b[0]), s.isles.get(b[1])));
		}
		for (int part = 0; part < 2; part++) {
			int p = part;
			steps.add(() -> drownedRuins(level, s, c, p, RandomSource.create(seed ^ p * 0x0D2E1L)));
		}
		for (SpringsHollow.Isle isle : s.isles) {
			if (isle.role().equals("city")) steps.add(() -> houseRuin(level, s, c, isle, RandomSource.create(seed ^ isle.x() * 31L ^ isle.z() * 17L)));
		}
		steps.add(() -> landings(level, s, c));
		steps.add(() -> tombLaid(level, s, c));
		steps.add(() -> arrivalTerraces(level, s, c));
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		steps.add(() -> renewFlowers(level, s, c, r));
		steps.add(() -> renewVaults(level, s, c));
		steps.add(() -> renewTomb(level, s, c));
	}

	// --- Building ---

	static BlockState soulLantern(boolean hanging) {
		return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging);
	}

	/** Old black stone: bricks of polished blackstone, cracked here and there. */
	static BlockState blackBricks(RandomSource random) {
		return (random.nextFloat() < 0.25f ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
	}

	/** A pier from the land's ground (under the water) up to just under {@code top}. */
	static void pier(ServerLevel level, BlockPos top, BlockState state) {
		int g = groundAt(SecretRealm.YELLOW_SPRINGS_HOLLOW, level.getSeed(), top.getX(), top.getZ());
		for (int y = g + 1; y < top.getY(); y++) put(level, top.atY(y), state);
	}

	/**
	 * A bridge of black stone from island {@code a} to {@code b}, three wide at the islands' height over the water (on the islands their own
	 * ground), low walls along it, a pier every eight blocks, soul lanterns on posts between.
	 */
	static void bridge(ServerLevel level, SpringsHollow.Shape s, BlockPos c, SpringsHollow.Isle a, SpringsHollow.Isle b) {
		RandomSource random = RandomSource.create(s.seed ^ a.x() * 7L ^ b.z() * 13L);
		double dx = b.x() - a.x(), dz = b.z() - a.z(), len = Math.hypot(dx, dz);
		double ux = dx / len, uz = dz / len;
		int y = SpringsHollow.ISLAND_Y;
		for (double t = 0; t <= len; t += 0.5) {
			double cx = a.x() + ux * t, cz = a.z() + uz * t;
			level.getChunk(c.getX() + (int) Math.round(cx) >> 4, c.getZ() + (int) Math.round(cz) >> 4);
			int step = (int) Math.round(t);
			for (int w = -2; w <= 2; w++) {
				int x = (int) Math.round(cx - uz * w), z = (int) Math.round(cz + ux * w);
				if (SpringsHollow.column(s, x, z).kind() == SpringsHollow.Kind.ISLE) continue;
				BlockPos at = c.offset(x, 0, z).atY(y);
				if (Math.abs(w) <= 1) {
					put(level, at, blackBricks(random));
					if (step % 8 == 0) pier(level, at, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
				} else if (!level.getBlockState(at).is(Blocks.POLISHED_BLACKSTONE_BRICKS) && !level.getBlockState(at).is(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS)) {
					put(level, at, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.defaultBlockState());
					put(level, at.above(), Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState());
					if (step % 8 == 4) put(level, at.above(2), soulLantern(false));
				}
			}
		}
		BlockPos from = c.offset(a.x(), 0, a.z()), to = c.offset(b.x(), 0, b.z());
		join(level, new BlockPos(Math.min(from.getX(), to.getX()) - 3, y, Math.min(from.getZ(), to.getZ()) - 3),
				new BlockPos(Math.max(from.getX(), to.getX()) + 3, y + 2, Math.max(from.getZ(), to.getZ()) + 3));
	}

	/** The drowned city's streets, half {@code part} of them: walls and pillars of black stone rising from the shallows' floor, a few breaking the water. */
	static void drownedRuins(ServerLevel level, SpringsHollow.Shape s, BlockPos c, int part, RandomSource random) {
		List<int[]> bridges = bridges(s);
		for (int i = 0, tries = 0; i < RUINS / 2 && tries < 300; tries++) {
			double a = random.nextDouble() * Math.PI * 2, r = SpringsHollow.RIVER_R + 8 + random.nextDouble() * (SpringsHollow.WALL_R - SpringsHollow.RIVER_R - 16);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			SpringsHollow.Column col = SpringsHollow.column(s, x, z);
			if (col.kind() != SpringsHollow.Kind.SHALLOWS || nearBridge(s, bridges, x, z, 5)) continue;
			level.getChunk(c.getX() + x >> 4, c.getZ() + z >> 4);
			boolean pillar = random.nextFloat() < 0.35f;
			int length = pillar ? 1 : 3 + random.nextInt(5);
			boolean alongX = random.nextBoolean();
			for (int k = 0; k < length; k++) {
				int wx = x + (alongX ? k : 0), wz = z + (alongX ? 0 : k);
				SpringsHollow.Column here = SpringsHollow.column(s, wx, wz);
				if (here.kind() != SpringsHollow.Kind.SHALLOWS) continue;
				int height = (pillar ? 8 : 4) + random.nextInt(pillar ? 6 : 7);
				for (int y = here.ground() + 1; y <= here.ground() + height; y++) {
					put(level, c.offset(wx, 0, wz).atY(y), pillar ? Blocks.POLISHED_BASALT.defaultBlockState() : blackBricks(random));
				}
			}
			i++;
		}
	}

	/** Whether (lx, lz) lies within {@code margin} of a bridge's line. */
	static boolean nearBridge(SpringsHollow.Shape s, List<int[]> bridges, int lx, int lz, double margin) {
		for (int[] b : bridges) {
			SpringsHollow.Isle a = s.isles.get(b[0]), e = s.isles.get(b[1]);
			double dx = e.x() - a.x(), dz = e.z() - a.z(), len2 = dx * dx + dz * dz;
			double t = len2 == 0 ? 0 : Mth.clamp(((lx - a.x()) * dx + (lz - a.z()) * dz) / len2, 0, 1);
			if (Math.hypot(lx - (a.x() + dx * t), lz - (a.z() + dz * t)) < margin) return true;
		}
		return false;
	}

	/** One of the city's houses on its island, fallen in: a floor of black stone, broken walls, a soul lantern on a post, old urns. */
	static void houseRuin(ServerLevel level, SpringsHollow.Shape s, BlockPos c, SpringsHollow.Isle isle, RandomSource random) {
		Frame f = isleFrame(s, c, isle);
		level.getChunk(f.origin().getX() >> 4, f.origin().getZ() >> 4);
		int floor = SpringsHollow.ISLAND_Y;
		for (int a = -3; a <= 3; a++) {
			for (int d = -3; d <= 3; d++) {
				BlockPos p = f.at(a, d, 0);
				put(level, p, random.nextFloat() < 0.2f ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState());
				for (int y = 1; y <= 5; y++) put(level, p.above(y), Blocks.AIR.defaultBlockState());
				boolean wall = Math.abs(a) == 3 || Math.abs(d) == 3, door = a == 3 && Math.abs(d) <= 1;
				if (!wall || door) continue;
				int height = 1 + random.nextInt(4);
				for (int y = 1; y <= height; y++) put(level, p.above(y), blackBricks(random));
			}
		}
		put(level, f.at(1, 2, 1), Blocks.DECORATED_POT.defaultBlockState());
		if (random.nextBoolean()) put(level, f.at(-1, -2, 1), Blocks.DECORATED_POT.defaultBlockState());
		put(level, f.at(4, 2, 1), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
		put(level, f.at(4, 2, 2), soulLantern(false));
		join(level, f.at(-4, -4, 0).atY(floor), f.at(5, 4, 0).atY(floor + 5));
	}

	/**
	 * The Ferryman's landing: a jetty of dark wood from the landing's island out over the springs, posts down to their bed, his boat moored
	 * beside it, soul lanterns at its root; on the tomb's island a jetty of its own, its bell on a pier of black stone; a path of black stone
	 * from it up to the tomb.
	 */
	static void landings(ServerLevel level, SpringsHollow.Shape s, BlockPos c) {
		Frame w = way(s, c);
		CaveAbodeWorks.loadChunks(level, w.at(SpringsHollow.TOMB_R - 8, -6, 0), w.at(SpringsHollow.RIVER_R + 8, 6, 0));
		jetty(level, w, SpringsHollow.RIVER_R - 6, SpringsHollow.RIVER_R + 5);
		jetty(level, w, SpringsHollow.TOMB_R - 4, SpringsHollow.TOMB_R + 6);
		// His boat, moored beside the near jetty: a skiff of dark wood at the water, a lantern on its stern post.
		int f0 = SpringsHollow.RIVER_R - 6, f1 = SpringsHollow.RIVER_R;
		for (int f = f0; f <= f1; f++) {
			for (int sd = 2; sd <= 4; sd++) {
				boolean bow = f == f0 && sd != 3;
				if (bow) continue;
				put(level, w.at(f, sd, -3), Blocks.DARK_OAK_PLANKS.defaultBlockState());
				boolean rim = f == f0 || f == f1 || sd != 3;
				put(level, w.at(f, sd, -2), rim ? slab(Blocks.DARK_OAK_SLAB, false) : Blocks.AIR.defaultBlockState());
			}
		}
		put(level, w.at(f1, 3, -2), Blocks.DARK_OAK_FENCE.defaultBlockState());
		put(level, w.at(f1, 3, -1), soulLantern(false));
		for (int sd = -2; sd <= 2; sd += 4) {
			put(level, w.at(SpringsHollow.RIVER_R + 5, sd, 1), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
			put(level, w.at(SpringsHollow.RIVER_R + 5, sd, 2), soulLantern(false));
		}
		// The far jetty's bell, on its pier.
		BlockPos bell = bell(w);
		pier(level, bell, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		put(level, bell.below(), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		put(level, bell, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, w.right()).setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
		// The path from the far jetty up the mound to the tomb.
		for (int f = 12; f <= SpringsHollow.TOMB_R - 4; f++) {
			for (int sd = -1; sd <= 1; sd++) {
				BlockPos at = w.at(f, sd, 0);
				int g = groundAt(SecretRealm.YELLOW_SPRINGS_HOLLOW, s.seed, at.getX(), at.getZ());
				if (g >= SpringsHollow.WATER_Y) put(level, at.atY(g), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
			}
		}
		join(level, w.at(SpringsHollow.TOMB_R - 8, -6, 0).below(4), w.at(SpringsHollow.RIVER_R + 8, 6, 3));
	}

	/** A jetty of dark wood along the main way from {@code f0} to {@code f1}, three wide at the islands' height, posts at its corners down to the bed. */
	static void jetty(ServerLevel level, Frame w, int f0, int f1) {
		for (int f = f0; f <= f1; f++) {
			for (int sd = -1; sd <= 1; sd++) {
				BlockPos at = w.at(f, sd, 0);
				if (groundAt(SecretRealm.YELLOW_SPRINGS_HOLLOW, level.getSeed(), at.getX(), at.getZ()) > SpringsHollow.ISLAND_Y) continue;
				put(level, at, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				if (Math.abs(sd) == 1 && (f - f0) % 4 == 0) pier(level, at, log(Blocks.DARK_OAK_LOG));
			}
		}
	}

	/**
	 * The Ghost King's tomb on the mound: two broad steps of black stone, his hall on them (walls of black bricks, pillars of basalt at its
	 * porch, a hipped roof of dark tiles), its door the trial's barrier; within, his coffin and his bones on it, soul fire in its corners, soul
	 * lanterns hung.
	 */
	static void tombLaid(ServerLevel level, SpringsHollow.Shape s, BlockPos c) {
		Frame t = tomb(s, c);
		RandomSource random = RandomSource.create(s.seed ^ 0x70B5L);
		SecretRealm realm = SecretRealm.YELLOW_SPRINGS_HOLLOW;
		CaveAbodeWorks.loadChunks(level, t.at(-12, -12, 0), t.at(12, 12, 0));
		levelOff(level, realm, t.at(-11, -11, -2), t.at(11, 11, -2), TOMB_FLOOR - 2, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(),
				Blocks.BLACKSTONE.defaultBlockState(), 14);
		for (int f = -9; f <= 9; f++) for (int sd = -9; sd <= 9; sd++) put(level, t.at(f, sd, -1), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
		for (int f = -7; f <= 8; f++) for (int sd = -7; sd <= 7; sd++) put(level, t.at(f, sd, 0), Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		for (int sd = -2; sd <= 2; sd++) {
			put(level, t.at(10, sd, -1), stairs(Blocks.POLISHED_BLACKSTONE_STAIRS, t.forward().getOpposite(), false));
			put(level, t.at(9, sd, 0), stairs(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, t.forward().getOpposite(), false));
		}
		// The hall: f -7 to 5, s -6 to 6 (its walls), six high; the porch before it.
		for (int f = -7; f <= 5; f++) {
			for (int sd = -6; sd <= 6; sd++) {
				boolean wall = f == -7 || f == 5 || Math.abs(sd) == 6;
				for (int y = 1; y <= 6; y++) put(level, t.at(f, sd, y), wall ? blackBricks(random) : Blocks.AIR.defaultBlockState());
				put(level, t.at(f, sd, 7), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
			}
		}
		for (int sd = -4; sd <= 4; sd += 8) for (int y = 1; y <= 6; y++) put(level, t.at(7, sd, y), log(Blocks.POLISHED_BASALT));
		HerbValleyWorks.hipRoof(level, t, -8, 8, -7, 7, 8);
		for (BlockPos p : doorway(t)) put(level, p, ModBlocks.TRIAL_BARRIER.defaultBlockState());
		for (int sd = -2; sd <= 2; sd += 4) put(level, t.at(7, sd, 6), soulLantern(true));
		// Within: his coffin, his bones on it; soul fire in the back corners; lanterns hung.
		for (int f = -4; f <= -2; f++) for (int sd = -1; sd <= 1; sd++) put(level, t.at(f, sd, 1), Blocks.POLISHED_BLACKSTONE.defaultBlockState());
		for (int sd = -1; sd <= 1; sd++) put(level, t.at(-4, sd, 2), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
		layKing(level, t);
		for (int sd = -5; sd <= 5; sd += 10) put(level, t.at(-6, sd, 1), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		for (int f = -3; f <= 2; f += 5) for (int sd = -3; sd <= 3; sd += 6) put(level, t.at(f, sd, 6), soulLantern(true));
		join(level, t.at(-12, -12, -2), t.at(12, 12, 14));
	}

	/** The Ghost King's own name (his bones', in his tomb): the world's own. */
	public static String kingName(ServerLevel level) {
		return NpcNames.of(new UUID(level.getSeed(), 0x60157C1L));
	}

	/** His bones on his coffin, facing his door. */
	static void layKing(ServerLevel level, Frame t) {
		BlockPos at = t.at(-3, 0, 2);
		put(level, at, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.STAGE, CorpseBlock.Rot.BONES));
		if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) {
			body.ancient(kingName(level), 0, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), t.forward().toYRot(), level.getGameTime(), List.of());
		}
	}

	/** At each arrival island: a terrace of black stone, a soul lantern either side. */
	static void arrivalTerraces(ServerLevel level, SpringsHollow.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0xA771BL);
		for (BlockPos at : WORKS.arrivalSpots(s.seed, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz > 12) continue;
					put(level, at.offset(dx, 0, dz), random.nextFloat() < 0.3f ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : blackBricks(random));
					for (int y = 1; y <= 4; y++) put(level, at.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
				}
			}
			for (int side = -3; side <= 3; side += 6) {
				put(level, at.offset(side, 1, 0), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
				put(level, at.offset(side, 2, 0), soulLantern(false));
			}
			join(level, at.offset(-4, 0, -4), at.offset(4, 4, 4));
		}
	}

	// --- Renewing ---

	/** The Other Shore Flowers: an unpicked one aged by the years shut, one picked grown again. */
	static void renewFlowers(ServerLevel level, SpringsHollow.Shape s, BlockPos c, SecretRealms.Record r) {
		RandomSource random = RandomSource.create(s.seed ^ r.openings * 0x0F10E6L);
		int bonus = r.realm.years() * HerbValleyWorks.YEARS_A_YEAR_SHUT;
		for (BlockPos at : flowerSpots(s, c)) HerbValleyWorks.renewHerb(level, at, ModBlocks.OTHER_SHORE_FLOWER, 300 + random.nextInt(700), bonus, Blocks.SOUL_SOIL);
	}

	static void renewVaults(ServerLevel level, SpringsHollow.Shape s, BlockPos c) {
		for (SpringsHollow.Isle isle : vaultIsles(s)) {
			BlockPos at = vaultAt(s, c, isle);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(StorageFormations.Tier.MID).defaultBlockState());
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, vaultTreasures(level.getRandom()));
		}
	}

	/** A drowned house's hoard: Yin Jade, spirit stones, a pill of the dead, now and then a ghost method's slip or a Departure Jade. */
	static List<ItemStack> vaultTreasures(RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.YIN_JADE, 4 + random.nextInt(5)));
		out.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 1 + random.nextInt(3)));
		if (random.nextFloat() < 0.5f) out.add(new ItemStack(ModItems.YIN_GATHERING_PILL));
		if (random.nextFloat() < 0.3f) out.add(new ItemStack(ModItems.YANG_PRESERVATION_PILL));
		if (random.nextFloat() < 0.3f) out.add(GhostJadeSlipItem.of(GhostMethod.NINE_NETHER_SOUL_SUTRA));
		if (random.nextFloat() < 0.3f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
		return out;
	}

	/** The tomb's barrier whole; the king's bones in place. */
	static void renewTomb(ServerLevel level, SpringsHollow.Shape s, BlockPos c) {
		Frame t = tomb(s, c);
		level.getChunk(t.origin().getX() >> 4, t.origin().getZ() >> 4);
		for (BlockPos p : doorway(t)) if (!level.getBlockState(p).is(ModBlocks.TRIAL_BARRIER)) put(level, p, ModBlocks.TRIAL_BARRIER.defaultBlockState());
		if (!(level.getBlockState(t.at(-3, 0, 2)).getBlock() instanceof CorpseBlock)) layKing(level, t);
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : SpringsHollow.arrivals(SpringsHollow.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	/** An earlier visitor's bones: on one of the city's islands. */
	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		SpringsHollow.Isle isle = s.isles.get(random.nextInt(s.isles.size()));
		double a = random.nextDouble() * Math.PI * 2, d = isle.r() * random.nextDouble() * 0.8;
		int x = isle.x() + (int) Math.round(Math.cos(a) * d), z = isle.z() + (int) Math.round(Math.sin(a) * d);
		SpringsHollow.Column col = SpringsHollow.column(s, x, z);
		return col.kind() == SpringsHollow.Kind.ISLE ? c.offset(x, 0, z).atY(col.ground() + 1) : null;
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.YIN_JADE, 2 + random.nextInt(4)));
		carried.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 3 + random.nextInt(6)));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (random.nextFloat() < 0.1f) carried.add(GhostJadeSlipItem.of(GhostMethod.GHOST_CORE_CANON));
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		List<Slot> slots = new ArrayList<>();
		RandomSource random = RandomSource.create(seed ^ 0xD2011EDL);
		int k = 0;
		for (SpringsHollow.Isle isle : s.isles) {
			if (isle.role().equals("arrival")) continue;
			// Before each house's door (the landing's: either side of the jetty's root).
			Frame f = isleFrame(s, c, isle);
			slots.add(new Slot("isle" + k++, ModEntities.DROWNED_SHADE, f.at(5, 0, 1), realm.guardianRank(isle.r() >= 12 ? Stage.MID : Stage.EARLY), false));
		}
		List<int[]> bridges = bridges(s);
		for (int b = 0; b < bridges.size(); b += 2) {
			SpringsHollow.Isle from = s.isles.get(bridges.get(b)[0]), to = s.isles.get(bridges.get(b)[1]);
			int x = (from.x() + to.x()) / 2, z = (from.z() + to.z()) / 2;
			slots.add(new Slot("bridge" + b, ModEntities.DROWNED_SHADE, c.offset(x, 0, z).atY(SpringsHollow.ISLAND_Y + 1), realm.guardianRank(Stage.EARLY), false));
		}
		Frame t = tomb(s, c);
		for (int side = -1; side <= 1; side += 2) {
			slots.add(new Slot("tomb" + (side + 1), ModEntities.DROWNED_SHADE, t.at(9, side * 5, 0), realm.guardianRank(Stage.LATE), false));
			slots.add(new Slot("mound" + (side + 1), ModEntities.DROWNED_SHADE, t.at(0, side * 10, -1), realm.guardianRank(Stage.LATE), false));
		}
		// Risen on the lantern night: about the islands' shores.
		for (int i = 0, tries = 0; i < 6 && tries < 120; tries++) {
			SpringsHollow.Isle isle = s.isles.get(random.nextInt(s.isles.size()));
			double a = random.nextDouble() * Math.PI * 2, d = isle.r() * 0.7;
			int x = isle.x() + (int) Math.round(Math.cos(a) * d), z = isle.z() + (int) Math.round(Math.sin(a) * d);
			SpringsHollow.Column col = SpringsHollow.column(s, x, z);
			if (col.kind() != SpringsHollow.Kind.ISLE) continue;
			slots.add(new Slot("risen" + i++, ModEntities.DROWNED_SHADE, c.offset(x, 0, z).atY(col.ground() + 1), realm.guardianRank(Stage.MID), false));
		}
		slots.add(new Slot("ferryman", ModEntities.FERRYMAN, ferryPost(way(s, c)), realm.guardianRank(Stage.EARLY), true));
		return slots;
	}

	/** The drowned rise only on the lantern night. */
	@Override
	protected boolean raisable(Slot slot, SecretRealms.Record r) {
		return !slot.id().startsWith("risen") || r.works.getInt("Night") == 1;
	}

	/** The tomb's island and the springs about it hold: the closing quake splits only the city. */
	@Override
	protected int calm() { return SpringsHollow.RIVER_R; }

	// --- During an opening ---

	/** Those being ferried now: who, which way (to the tomb's island or back), when they land. */
	private record Crossing(UUID who, boolean out, long lands) {}

	private static final List<Crossing> CROSSINGS = new ArrayList<>();

	static void clear() {
		CROSSINGS.clear();
	}

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("NightAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Night", 0);
	}

	@Override
	protected void end(ServerLevel level, SecretRealms.Record r) {
		CROSSINGS.clear();
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		SpringsHollow.Shape s = SpringsHollow.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		night(level, s, c, r, now, inside, data);
		crossings(level, s, c, now);
		if (now % 10 != 5) return;
		Frame w = way(s, c), t = tomb(s, c);
		BlockPos post = ferryPost(w);
		ListTag told = r.works.getList("Told", Tag.TAG_STRING);
		for (ServerPlayer p : inside) {
			pull(level, s, c, p, now);
			UUID id = p.getUUID();
			if (p.blockPosition().distSqr(post) < 14 * 14 && !told.contains(StringTag.valueOf(id.toString()))) {
				told.add(StringTag.valueOf(id.toString()));
				r.works.put("Told", told);
				data.setDirty();
				p.sendSystemMessage(Component.translatable(ModLang.MSG_HOLLOW_TRIALS, TOLL_JADE, TOLL_STONES, Component.translatable(r.inheritor.isEmpty()
						? ModLang.MSG_HOLLOW_GREAT : ModLang.MSG_HOLLOW_LESSER)).withStyle(ChatFormatting.GOLD));
			}
			if (p.blockPosition().distSqr(coffin(t)) < 3 * 3 && passed(r, id) && !passed(r, "Claimed", id)) {
				pass(r, "Claimed", id);
				data.setDirty();
				inherit(level, r, p);
			}
		}
		if (now % 40 == 5) {
			BlockPos at = coffin(t);
			if (level.isLoaded(at)) level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 4, 0.4, 0.3, 0.4, 0.01);
		}
	}

	/** The springs' pull on one swimming their deep ring: dragged down, withered, slowed (unless they fly over it). */
	static void pull(ServerLevel level, SpringsHollow.Shape s, BlockPos c, ServerPlayer p, long now) {
		if (p.isCreative() || !p.isInWater()) return;
		if (SpringsHollow.column(s, p.getBlockX() - c.getX(), p.getBlockZ() - c.getZ()).kind() != SpringsHollow.Kind.RIVER) return;
		p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, true, true));
		p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, true, false));
		p.setDeltaMovement(p.getDeltaMovement().add(0, -0.25, 0));
		p.hurtMarked = true;
		if (now % 40 == 5) p.displayClientMessage(Component.translatable(ModLang.MSG_HOLLOW_PULL).withStyle(ChatFormatting.RED), true);
	}

	/**
	 * Its hour come, for {@link #NIGHT_TICKS}: lanterns of the dead rise from the water about everyone inside; every Other Shore Flower blooms
	 * {@link #NIGHT_YEARS} the older; the drowned rise (their posts raisable now); the Ferryman carries all who ask. Then the lanterns sink.
	 */
	static void night(ServerLevel level, SpringsHollow.Shape s, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int stage = r.works.getInt("Night");
		if (stage == 0 && now >= r.works.getLong("NightAt")) {
			r.works.putInt("Night", 1);
			r.works.putLong("NightEnds", now + NIGHT_TICKS);
			data.setDirty();
			for (BlockPos at : flowerSpots(s, c)) {
				if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof GinsengBlockEntity flower)) continue;
				flower.setAge(Math.min(FruitAge.MAX_YEARS, flower.age() + NIGHT_YEARS));
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 8, 0.2, 0.3, 0.2, 0.01);
			}
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_HOLLOW_NIGHT, NIGHT_TICKS / 1200).withStyle(ChatFormatting.LIGHT_PURPLE));
			for (ServerPlayer p : inside) p.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.AMBIENT, 1.5f, 0.5f);
		} else if (stage == 1) {
			if (now >= r.works.getLong("NightEnds")) {
				r.works.putInt("Night", 2);
				data.setDirty();
				tellInside(level, r.realm, Component.translatable(ModLang.MSG_HOLLOW_NIGHT_ENDS).withStyle(ChatFormatting.GRAY));
				return;
			}
			if (now % 10 != 0) return;
			RandomSource random = level.getRandom();
			for (ServerPlayer p : inside) {
				for (int k = 0; k < 6; k++) {
					double a = random.nextDouble() * Math.PI * 2, d = 4 + random.nextDouble() * 28;
					double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
					SpringsHollow.Column col = SpringsHollow.column(s, Mth.floor(x) - c.getX(), Mth.floor(z) - c.getZ());
					if (col.kind() != SpringsHollow.Kind.RIVER && col.kind() != SpringsHollow.Kind.SHALLOWS) continue;
					level.sendParticles(p, ParticleTypes.SOUL_FIRE_FLAME, true, x, SpringsHollow.WATER_Y + 1.2, z, 2, 0.1, 0.4, 0.1, 0.01);
					level.sendParticles(p, ParticleTypes.END_ROD, true, x, SpringsHollow.WATER_Y + 1.6, z, 1, 0.1, 0.6, 0.1, 0.004);
				}
			}
		}
	}

	// --- The Ferryman ---

	/** {@code who} put on the Ferryman's boat: across to the tomb's island ({@code out}) or back, landing {@link #FERRY_TICKS} on. */
	static void ferry(ServerLevel level, LivingEntity who, boolean out, long now) {
		for (Crossing crossing : CROSSINGS) if (crossing.who().equals(who.getUUID())) return; // already on the water
		CROSSINGS.add(new Crossing(who.getUUID(), out, now + FERRY_TICKS));
		who.addEffect(new MobEffectInstance(MobEffects.DARKNESS, FERRY_TICKS + 30, 0, true, false));
		who.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FERRY_TICKS, 6, true, false));
		level.playSound(null, who.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 1.2f, 0.7f);
		if (who instanceof ServerPlayer p) p.sendSystemMessage(Component.translatable(ModLang.MSG_FERRY_CROSSING).withStyle(ChatFormatting.GRAY));
	}

	/** The crossings under way: the mist about each on the water; each landed in its time on the far shore. */
	static void crossings(ServerLevel level, SpringsHollow.Shape s, BlockPos c, long now) {
		if (CROSSINGS.isEmpty()) return;
		Frame w = way(s, c);
		for (Iterator<Crossing> it = CROSSINGS.iterator(); it.hasNext(); ) {
			Crossing crossing = it.next();
			Entity who = level.getEntity(crossing.who());
			if (!(who instanceof LivingEntity living) || !living.isAlive() || SecretRealm.at(who.getX(), who.getZ()) != SecretRealm.YELLOW_SPRINGS_HOLLOW) {
				it.remove();
				continue;
			}
			if (now % 5 == 0) level.sendParticles(ParticleTypes.SOUL, who.getX(), who.getY() + 1, who.getZ(), 3, 0.5, 0.6, 0.5, 0.01);
			if (now < crossing.lands()) continue;
			it.remove();
			BlockPos to = crossing.out() ? farLanding(w) : nearLanding(w);
			float yaw = (crossing.out() ? w.forward().getOpposite() : w.forward()).toYRot();
			if (who instanceof ServerPlayer p) p.teleportTo(level, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, yaw, 0);
			else who.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
			level.playSound(null, to, SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 1.0f, 0.9f);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
			if (who instanceof ServerPlayer p) p.sendSystemMessage(Component.translatable(ModLang.MSG_FERRY_LANDED).withStyle(ChatFormatting.GRAY));
		}
	}

	/** The far jetty's bell rung (SpringsHollowWorks#register): the Ferryman comes for whoever rang it, and takes them back. */
	static void bellRung(ServerLevel level, BlockPos pos, ServerPlayer p) {
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm != SecretRealm.YELLOW_SPRINGS_HOLLOW) return;
		SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
		if (r.openedAt < 0 || !pos.equals(bell(way(SpringsHollow.shape(level.getSeed()), realm.centre())))) return;
		ferry(level, p, false, level.getServer().overworld().getGameTime());
	}

	/** The far jetty's bell, heard (a block's use: its own ring goes on). */
	static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (hand == InteractionHand.MAIN_HAND && world instanceof ServerLevel level && player instanceof ServerPlayer p && ModDimensions.isSecretRealm(level.dimension())
					&& level.getBlockState(hit.getBlockPos()).is(Blocks.BELL)) {
				bellRung(level, hit.getBlockPos(), p);
			}
			return InteractionResult.PASS;
		});
	}

	/** The Ferryman touched by one he has judged (or by anyone on the lantern night): he takes them across. */
	@Override
	protected boolean guardianTouched(ServerLevel level, SecretRealms.Record r, RealmGuardian guardian, Player p) {
		if (guardian.kind() != RealmGuardian.Kind.FERRYMAN || !(p instanceof ServerPlayer sp)) return false;
		long now = level.getServer().overworld().getGameTime();
		if (r.works.getInt("Night") == 1 && !passed(r, p.getUUID())) {
			pass(r, "Passed", p.getUUID());
			SecretRealms.Data.get(level.getServer()).setDirty();
			sp.sendSystemMessage(Component.translatable(ModLang.MSG_FERRY_NIGHT).withStyle(ChatFormatting.LIGHT_PURPLE));
			SecretRealms.HUD_SHOWN.remove(sp.getUUID()); // the barrier's word sent again
		}
		if (!passed(r, p.getUUID())) return false;
		ferry(level, sp, true, now);
		return true;
	}

	/** One the Ferryman has judged is told so; a neutral one without his toll is told what it is. */
	@Override
	protected Component mayChallenge(SecretRealms.Record r, LivingEntity p) {
		if (passed(r, p.getUUID())) return Component.translatable(ModLang.MSG_FERRY_JUDGED);
		if (faction(p) == Alignment.Faction.NEUTRAL && p instanceof ServerPlayer sp && !sp.isCreative() && toll(sp) == null) {
			return Component.translatable(ModLang.MSG_FERRY_TOLL_SHORT, TOLL_JADE, TOLL_STONES);
		}
		return null;
	}

	/** The righteous he takes for nothing; the neutral for his toll (taken now); the devilish must best him. */
	@Override
	protected boolean passesUnfought(ServerLevel level, SecretRealms.Record r, LivingEntity p) {
		Alignment.Faction faction = faction(p);
		if (faction == Alignment.Faction.DEVILISH) return false;
		if (p instanceof ServerPlayer sp) {
			if (faction == Alignment.Faction.RIGHTEOUS || sp.isCreative()) {
				sp.sendSystemMessage(Component.translatable(ModLang.MSG_FERRY_LIGHT).withStyle(ChatFormatting.GRAY));
			} else {
				Item toll = toll(sp);
				if (toll == null) return false;
				int count = toll == ModItems.YIN_JADE ? TOLL_JADE : TOLL_STONES;
				sp.getInventory().clearOrCountMatchingItems(stack -> stack.is(toll), count, sp.inventoryMenu.getCraftSlots());
				sp.sendSystemMessage(Component.translatable(ModLang.MSG_FERRY_TOLL_PAID, count, new ItemStack(toll).getHoverName()).withStyle(ChatFormatting.GRAY));
			}
		}
		return true;
	}

	/** What {@code p} would pay the Ferryman with: Yin Jade first, else Mid spirit stones, else nothing (null). */
	static Item toll(ServerPlayer p) {
		if (p.getInventory().countItem(ModItems.YIN_JADE) >= TOLL_JADE) return ModItems.YIN_JADE;
		if (p.getInventory().countItem(ModItems.MID_SPIRIT_STONE) >= TOLL_STONES) return ModItems.MID_SPIRIT_STONE;
		return null;
	}

	static Alignment.Faction faction(LivingEntity e) {
		if (e instanceof ServerPlayer p) return Alignment.factionOf(CultivationManager.get(p).getAlignment());
		return e instanceof CultivatorNpc npc ? npc.getFaction() : Alignment.Faction.NEUTRAL;
	}

	@Override
	protected MutableComponent challengeMessage(Component rank) {
		return Component.translatable(ModLang.MSG_FERRY_WAKES, rank);
	}

	@Override
	protected MutableComponent gatePassedMessage() {
		return Component.translatable(ModLang.MSG_FERRY_BOWS);
	}

	// --- Its inheritance ---

	/** The Great Inheritance: the Ghost King's own foundation, the Six Paths Reincarnation Canon; Yin Jade from his tomb. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(GhostJadeSlipItem.of(GhostMethod.SIX_PATHS_CANON), new ItemStack(ModItems.YIN_JADE, 16));
	}

	/** A lesser legacy: the method he cultivated before it (Yama's Judgement), spirit stones. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		return List.of(GhostJadeSlipItem.of(GhostMethod.YAMA_JUDGEMENT), new ItemStack(ModItems.MID_SPIRIT_STONE, 2 + random.nextInt(3)));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_HOLLOW_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_HOLLOW_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_HOLLOW_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** On one of the city's islands, the nearer the springs the later. */
	@Override
	protected BlockPos rivalStart(ServerLevel level, long seed, BlockPos c, double along, RandomSource random) {
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		SpringsHollow.Isle best = null;
		double want = SpringsHollow.ARRIVAL_R - along * (SpringsHollow.ARRIVAL_R - SpringsHollow.RIVER_R - 14), d = Double.MAX_VALUE;
		for (SpringsHollow.Isle isle : s.isles) {
			double here = Math.abs(Math.hypot(isle.x(), isle.z()) - want) + random.nextDouble() * 12;
			if (here < d) {
				d = here;
				best = isle;
			}
		}
		return c.offset(best.x() + random.nextInt(5) - 2, 0, best.z() + random.nextInt(5) - 2);
	}

	/** The nearest Other Shore Flower on the islands. */
	@Override
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) {
		BlockPos best = null;
		double d = 40 * 40;
		for (BlockPos at : flowerSpots(SpringsHollow.shape(seed), c)) {
			double here = at.distSqr(near);
			if (here < d && level.isLoaded(at) && level.getBlockState(at).getBlock() instanceof GinsengBlock) {
				d = here;
				best = at;
			}
		}
		return best;
	}

	@Override
	protected List<ItemStack> rivalTake(ServerLevel level, BlockPos at, CultivatorNpc npc) {
		return HerbValleyWorks.WORKS.rivalTake(level, at, npc);
	}

	@Override
	protected List<BlockPos> vaults(long seed, BlockPos c) {
		SpringsHollow.Shape s = SpringsHollow.shape(seed);
		List<BlockPos> out = new ArrayList<>();
		for (SpringsHollow.Isle isle : vaultIsles(s)) out.add(vaultAt(s, c, isle));
		return out;
	}

	/** The Ferryman's judgement; his boat across; through the tomb's door; the coffin. */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		SpringsHollow.Shape s = SpringsHollow.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		UUID id = npc.getUUID();
		if (passed(r, "Claimed", id)) return null;
		Frame w = way(s, c), t = tomb(s, c);
		if (!passed(r, id)) return new Trial("judge", ferryPost(w), 3.5, false);
		boolean onIsland = Math.hypot(npc.getX() - c.getX() - 0.5, npc.getZ() - c.getZ() - 0.5) < SpringsHollow.TOMB_R + 4;
		if (!onIsland) return new Trial("ferry", ferryPost(w), 3.5, false);
		if (!inTomb(t.local(npc.blockPosition()))) return new Trial("door", t.at(7, 0, 1), 2.5, true);
		return new Trial("coffin", coffin(t), 2.5, true);
	}

	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		switch (trial.step()) {
			case "judge" -> {
				return challengeAt(level, r, npc, trial.mark(), working);
			}
			case "ferry" -> {
				// Poled across in the mist, and set down on the far jetty as the step ends (else it would make back for this mark).
				if (working == 0) level.playSound(null, npc.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 1.2f, 0.7f);
				if (working % 5 == 0) level.sendParticles(ParticleTypes.SOUL, npc.getX(), npc.getY() + 1, npc.getZ(), 3, 0.5, 0.6, 0.5, 0.01);
				if (working < FERRY_TICKS) return false;
				BlockPos to = farLanding(way(SpringsHollow.shape(level.getSeed()), r.realm.centre()));
				npc.getNavigation().stop();
				npc.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
			}
			case "door" -> {
				// The barrier lets the judged by: set down within.
				BlockPos in = tomb(SpringsHollow.shape(level.getSeed()), r.realm.centre()).at(2, 0, 1);
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

	private SpringsHollowWorks() {}
}
