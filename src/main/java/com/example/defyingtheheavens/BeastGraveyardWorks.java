package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The Primeval Beast Graveyard's works (SecretRealm#PRIMEVAL_BEAST_GRAVEYARD; its land is BeastGraveyard's, the machinery every realm shares
 * RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: the primeval dragon's bones across the basin's heart ({@link #bones}: its skull, hollow, its jaws open on the
 *   ground and its eyes empty; its spine curving round, ribs arching over the jungle floor, its legs' claws gripping the earth, its tail
 *   tapering away); the jungle (trees great and small, kept off the bones); a hunters' camp at each arrival; two beast tamers' lodges,
 *   fallen in, each with a vault.</li>
 *   <li><b>Renewed each time it stirs</b>: herbs grown in the dragon's shadow ({@link #HERBS}, older for every year it slept); its vaults
 *   sealed anew for the ancients; the bones of a few earlier visitors.</li>
 *   <li><b>Its beasts</b>: wild demon beasts gather here and grow past their kind on the dragon's bones (DemonBeast#setBoundlessRank), of the
 *   ranks of the realm it is meant for, each kept to its own ground; three great ones about the ribs.</li>
 *   <li><b>Its event</b>: some time into each opening the bones roar ({@link #ROAR_TICKS}): every beast in it goes wild, swifter and stronger,
 *   and rushes the nearest visitor.</li>
 *   <li><b>Its trial</b>: before the skull the dragon's echo rises for whoever comes to face it, matched to them; bested, it sinks back into the
 *   bones, and its blood essence gathers in the skull: the Great Inheritance (the Primeval Blood Essence) once in a world, after it a lesser
 *   legacy each opening.</li>
 * </ul>
 */
public final class BeastGraveyardWorks extends RealmWorks {
	static final BeastGraveyardWorks WORKS = new BeastGraveyardWorks();
	/** Herbs grown in the dragon's shadow each opening; the bones' roar lasts this long (a minute). */
	static final int HERBS = 12, ROAR_TICKS = 1200;
	/** The beasts that gather here: the wild kinds that walk the ground, large enough to be of the dragon's company. */
	static final BeastKind[] KINDS = {BeastKind.BLOODFANG_TIGER, BeastKind.FROST_APE, BeastKind.EARTHQUAKE_BEAR, BeastKind.LIGHTNING_ANTELOPE,
			BeastKind.EMBER_FOX, BeastKind.JADE_SCALED_PYTHON, BeastKind.HORNED_THUNDER_RHINO, BeastKind.SHADOW_WOLF};

	// --- Where things are ---

	/** The skull's frame: its middle on the ground, the way its snout points (away from its body), level. */
	record Skull(Vec3 origin, Vec3 forward, Vec3 right) {
		/** The world point (f forward, s to the right, y up) from it. */
		Vec3 at(double f, double s, double y) {
			return origin.add(forward.scale(f)).add(right.scale(s)).add(0, y, 0);
		}
	}

	static Skull skull(BeastGraveyard.Shape s, BlockPos c) {
		BlockPos local = BeastGraveyard.skull(s);
		double a = s.skullTurn - Math.toRadians(12);
		// Its snout points back along the curve, away from its body (the way the spine's angle falls).
		Vec3 forward = new Vec3(Math.sin(a), 0, -Math.cos(a));
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		int g = BeastGraveyard.column(s, local.getX(), local.getZ()).ground();
		return new Skull(new Vec3(c.getX() + local.getX() + 0.5, g + 1, c.getZ() + local.getZ() + 0.5), forward, right);
	}

	/** Where the dragon's echo rises: before its snout. */
	static BlockPos echoPost(BeastGraveyard.Shape s, BlockPos c) {
		Vec3 p = skull(s, c).at(26, 0, 0);
		int x = Mth.floor(p.x), z = Mth.floor(p.z);
		return new BlockPos(x, groundAt(SecretRealm.PRIMEVAL_BEAST_GRAVEYARD, s.seed, x, z) + 1, z);
	}

	/** Where its blood essence gathers once its echo is bested: in the hollow of the skull. */
	static BlockPos essenceSpot(BeastGraveyard.Shape s, BlockPos c) {
		return BlockPos.containing(skull(s, c).at(1, 0, 0.5));
	}

	/** The spine's height over the ground {@code turn} degrees round from the skull: low at the neck and the tail's tip, highest at the back. */
	static double spineHeight(double turn) {
		return 1.5 + 8 * Math.sin(Math.PI * Mth.clamp(turn / BeastGraveyard.SPINE_TURN, 0, 1));
	}

	/** Whether (lx, lz) lies within {@code margin} of the dragon's bones (the trees, herbs and beasts' posts kept off them). */
	static boolean nearBones(BeastGraveyard.Shape s, int lx, int lz, double margin) {
		double[] along = BeastGraveyard.alongDragon(s, lx, lz);
		boolean body = along[0] <= BeastGraveyard.SPINE_TURN + 4 && Math.abs(along[1]) < 15 + margin;
		BlockPos sk = BeastGraveyard.skull(s);
		return body || Math.hypot(lx - sk.getX(), lz - sk.getZ()) < 30 + margin;
	}

	/** The beast tamers' lodges: across the basin from the skull, either side. */
	static Frame lodge(BeastGraveyard.Shape s, BlockPos c, int k) {
		double b = s.skullTurn + Math.PI + (k == 0 ? -0.5 : 0.5);
		double r = 92;
		int x = (int) Math.round(Math.cos(b) * r), z = (int) Math.round(Math.sin(b) * r);
		double cos = Math.cos(b), sin = Math.sin(b);
		Direction in = Math.abs(cos) > Math.abs(sin) ? (cos > 0 ? Direction.WEST : Direction.EAST) : (sin > 0 ? Direction.NORTH : Direction.SOUTH);
		return new Frame(c.offset(x, 0, z).atY(groundAt(SecretRealm.PRIMEVAL_BEAST_GRAVEYARD, s.seed, c.getX() + x, c.getZ() + z) + 1), in);
	}

	static BlockPos vaultAt(BeastGraveyard.Shape s, BlockPos c, int k) {
		return lodge(s, c, k).at(-2, 0, 0);
	}

	/** The herbs' places (the world's own roll): in the dragon's shadow, between its ribs and by its skull, on the jungle floor. */
	static List<BlockPos> herbSpots(BeastGraveyard.Shape s, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		RandomSource random = RandomSource.create(s.seed ^ 0x4E7B5L);
		for (int tries = 0; out.size() < HERBS && tries < 600; tries++) {
			double turn = random.nextDouble() * 140, off = (random.nextBoolean() ? 1 : -1) * (5 + random.nextDouble() * 9);
			double a = s.skullTurn + Math.toRadians(turn), r = BeastGraveyard.SPINE_R + off;
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			BeastGraveyard.Column col = BeastGraveyard.column(s, x, z);
			if (col.kind() != BeastGraveyard.Kind.FLOOR) continue;
			BlockPos at = c.offset(x, 0, z).atY(col.ground() + 1);
			if (!out.contains(at)) out.add(at);
		}
		return out;
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		BeastGraveyard.Shape s = BeastGraveyard.shape(seed);
		steps.add(() -> skullBones(level, s, c));
		for (int part = 0; part < 5; part++) {
			int p = part;
			steps.add(() -> spineBones(level, s, c, p));
		}
		for (int part = 0; part < 6; part++) {
			int p = part;
			steps.add(() -> trees(level, s, c, p, RandomSource.create(seed ^ p * 0x7E3EL)));
		}
		steps.add(() -> camps(level, s, c));
		for (int k = 0; k < 2; k++) {
			int t = k;
			steps.add(() -> lodgeAt(level, s, c, t, RandomSource.create(seed ^ t * 0x10D6EL)));
		}
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		BeastGraveyard.Shape s = BeastGraveyard.shape(seed);
		steps.add(() -> renewHerbs(level, s, c, r));
		steps.add(() -> renewVaults(level, s, c));
	}

	// --- The dragon's bones ---

	private static BlockState bone(Direction.Axis axis) {
		return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	/** A bone block at world point {@code p} along {@code way} (its grain), where there is room (never into the ground). */
	private static void boneAt(ServerLevel level, Vec3 p, Vec3 way) {
		BlockPos pos = BlockPos.containing(p);
		BlockState here = level.getBlockState(pos);
		if (!here.isAir() && !here.canBeReplaced() && !here.is(Blocks.BONE_BLOCK) && !here.is(Blocks.WATER) && !(here.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)) return;
		double ax = Math.abs(way.x), ay = Math.abs(way.y), az = Math.abs(way.z);
		put(level, pos, bone(ay >= ax && ay >= az ? Direction.Axis.Y : ax >= az ? Direction.Axis.X : Direction.Axis.Z));
	}

	/**
	 * The skull: its cranium a hollow shell of bone over the ground (walk in by its open jaws), its eye sockets empty, two horns sweeping back
	 * from its crown; its upper jaw a long snout open beneath, fanged; its lower jaw lying open on the ground before it, fanged too.
	 */
	static void skullBones(ServerLevel level, BeastGraveyard.Shape s, BlockPos c) {
		Skull k = skull(s, c);
		BlockPos o = BlockPos.containing(k.origin());
		for (int cx = (o.getX() - 24) >> 4; cx <= (o.getX() + 24) >> 4; cx++) for (int cz = (o.getZ() - 24) >> 4; cz <= (o.getZ() + 24) >> 4; cz++) level.getChunk(cx, cz);
		for (BlockPos pos : BlockPos.betweenClosed(o.offset(-24, -1, -24), o.offset(24, 14, 24))) {
			Vec3 d = Vec3.atCenterOf(pos).subtract(k.origin());
			double f = d.dot(k.forward()), sd = d.dot(k.right()), y = d.y;
			// The cranium: an egg of bone, hollow, open toward the snout.
			double e = Math.pow(f / 6.2, 2) + Math.pow(sd / 4.8, 2) + Math.pow((y - 3.6) / 4.2, 2);
			boolean shell = e <= 1 && e >= 0.62 && y >= 0;
			boolean socket = Math.pow(f - 2.6, 2) + Math.pow(Math.abs(sd) - 4.1, 2) + Math.pow(y - 4.6, 2) < 2.4;
			boolean throat = f > 3.2 && Math.abs(sd) < 2.6 && y < 4.2;
			// The snout: a roof and two walls over the open mouth, narrowing to its tip.
			double narrow = 3.2 - Math.max(0, f - 6) * 0.08;
			boolean snout = f >= 4.5 && f <= 18 && y >= 1.5 && y <= 5 - Math.max(0, f - 6) * 0.12
					&& (Math.abs(Math.abs(sd) - narrow) < 0.6 || y >= 4.2 - Math.max(0, f - 6) * 0.12 && Math.abs(sd) < narrow + 0.4);
			// The lower jaw, fallen open: two bars along the ground meeting at the chin.
			boolean jaw = y >= 0 && y < 1 && f >= 4 && f <= 18.5 && (Math.abs(Math.abs(sd) - (3.2 - (f - 4) * 0.08)) < 0.6 || f > 17.5 && Math.abs(sd) < 2.2);
			if ((shell && !socket && !throat) || snout || jaw) boneAt(level, Vec3.atCenterOf(pos), f > 4 ? k.forward() : new Vec3(0, 1, 0));
		}
		// Its fangs: hanging from the snout's walls, rising from the jaw's bars.
		for (double f = 6; f <= 17; f += 2) {
			double narrow = 3.2 - Math.max(0, f - 6) * 0.08;
			for (int side = -1; side <= 1; side += 2) {
				fang(level, BlockPos.containing(k.at(f, side * narrow, 1.0)), Direction.DOWN);
				fang(level, BlockPos.containing(k.at(f + 1, side * (3.2 - (f - 3) * 0.08), 1.0)), Direction.UP);
			}
		}
		// Its horns: from its crown, sweeping back and up, thinning.
		for (int side = -1; side <= 1; side += 2) {
			for (double u = 0; u <= 1; u += 0.02) {
				Vec3 p = k.at(-2.5 - 10 * u, side * (3.4 + 2.2 * u), 7.2 + 4 * u - 2.6 * u * u);
				Vec3 way = k.forward().scale(-1).add(0, 0.4, 0);
				boneAt(level, p, way);
				if (u < 0.45) boneAt(level, p.add(0, -1, 0), way);
			}
		}
	}

	private static void fang(ServerLevel level, BlockPos pos, Direction tip) {
		if (!level.getBlockState(pos).isAir()) return;
		put(level, pos, Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION, tip)
				.setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP));
	}

	/**
	 * The spine, part {@code part} of five along the curve: a cord of bone, a vertebra every few blocks with its spike, ribs arching out and in
	 * over the ground along its fore half, the legs' bones and claws at the shoulders and the hips; the tail thinning to its tip.
	 */
	static void spineBones(ServerLevel level, BeastGraveyard.Shape s, BlockPos c, int part) {
		double from = part * BeastGraveyard.SPINE_TURN / 5, to = (part + 1) * BeastGraveyard.SPINE_TURN / 5;
		for (double t = from; t <= to; t += 0.4) {
			Vec3 p = spineAt(s, c, t);
			Vec3 tangent = spineAt(s, c, t + 0.5).subtract(p);
			boneAt(level, p, tangent);
			double thin = t / BeastGraveyard.SPINE_TURN;
			// A vertebra every five degrees: a knot of bone round the cord, a spike rising from it (smaller toward the tail).
			if (Math.abs(t / 5 - Math.round(t / 5)) < 0.04) {
				int half = thin < 0.75 ? 1 : 0;
				for (int dx = -half; dx <= half; dx++) for (int dy = -half; dy <= half; dy++) for (int dz = -half; dz <= half; dz++) boneAt(level, p.add(dx, dy, dz), tangent);
				int spike = thin < 0.6 ? 3 : thin < 0.85 ? 2 : 1;
				for (int y = 1; y <= spike; y++) boneAt(level, p.add(0, half + y, 0), new Vec3(0, 1, 0));
			}
			// Ribs along its fore half: a pair every seven degrees, arching out from the spine and down to the ground, either side.
			if (t >= 18 && t <= 128 && Math.abs(t / 7 - Math.round(t / 7)) < 0.03) {
				for (int side = -1; side <= 1; side += 2) rib(level, s, c, t, side);
			}
		}
		for (double leg : new double[] {34, 122}) if (leg >= from && leg < to) for (int side = -1; side <= 1; side += 2) leg(level, s, c, leg, side);
	}

	/** The spine's middle {@code turn} degrees round from the skull, at its height over the land there. */
	static Vec3 spineAt(BeastGraveyard.Shape s, BlockPos c, double turn) {
		double a = s.skullTurn + Math.toRadians(turn);
		double x = Math.cos(a) * BeastGraveyard.SPINE_R, z = Math.sin(a) * BeastGraveyard.SPINE_R;
		int g = BeastGraveyard.column(s, (int) Math.round(x), (int) Math.round(z)).ground();
		return new Vec3(c.getX() + x + 0.5, g + 1 + spineHeight(turn), c.getZ() + z + 0.5);
	}

	/** A rib from the spine at {@code turn}, on {@code side} (+1 out from the heart, -1 in): up and over, then down to the ground. */
	private static void rib(ServerLevel level, BeastGraveyard.Shape s, BlockPos c, double turn, int side) {
		double a = s.skullTurn + Math.toRadians(turn);
		Vec3 radial = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(side), back = new Vec3(-Math.sin(a), 0, Math.cos(a));
		Vec3 root = spineAt(s, c, turn);
		double h = spineHeight(turn);
		Vec3 last = root;
		for (double u = 0; u <= 1; u += 0.015) {
			double out = 12 * Math.sin(u * Math.PI / 2), up = 5 * Math.sin(u * Math.PI) - (h + 0.5) * u * u;
			Vec3 p = root.add(radial.scale(out)).add(back.scale(1.5 * u)).add(0, up, 0);
			BlockPos column = BlockPos.containing(p);
			int ground = groundAt(SecretRealm.PRIMEVAL_BEAST_GRAVEYARD, s.seed, column.getX(), column.getZ());
			if (p.y < ground + 1) break; // into the earth: it ends there
			boneAt(level, p, p.subtract(last));
			last = p;
		}
	}

	/** A leg's bones at {@code turn} on {@code side}: down and out from the spine to the ground, three claws gripping the earth forward. */
	private static void leg(ServerLevel level, BeastGraveyard.Shape s, BlockPos c, double turn, int side) {
		double a = s.skullTurn + Math.toRadians(turn);
		Vec3 radial = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(side), fore = new Vec3(Math.sin(a), 0, -Math.cos(a));
		Vec3 root = spineAt(s, c, turn), last = root;
		Vec3 foot = root;
		for (double u = 0; u <= 1; u += 0.02) {
			Vec3 p = root.add(radial.scale(9 * u)).add(0, -(spineHeight(turn) + 0.5) * u + 2.5 * Math.sin(u * Math.PI), 0);
			boneAt(level, p, p.subtract(last));
			boneAt(level, p.add(radial.scale(0.6)), p.subtract(last));
			last = p;
			foot = p;
		}
		for (int toe = -1; toe <= 1; toe++) {
			Vec3 way = fore.add(radial.scale(toe * 0.6)).normalize();
			for (double d = 0; d <= 4; d += 0.4) boneAt(level, foot.add(way.scale(d)).add(0, d > 3 ? -0.5 : 0, 0), way);
		}
	}

	// --- The jungle, the camps, the lodges ---

	/** Trees, part {@code part}: a sixth of the basin round, great jungle trees, lesser ones and bushes, off the bones, the ponds and the camps; the rim's. */
	static void trees(ServerLevel level, BeastGraveyard.Shape s, BlockPos c, int part, RandomSource random) {
		var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
		List<BlockPos> arrivals = WORKS.arrivalSpots(s.seed, c);
		for (int i = 0, tries = 0; i < 26 && tries < 200; tries++) {
			double a = s.skullTurn + (part + random.nextDouble()) * Math.PI / 3, r = 12 + random.nextDouble() * (BeastGraveyard.EDGE_R - 18);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			BeastGraveyard.Column col = BeastGraveyard.column(s, x, z);
			if (col.kind() != BeastGraveyard.Kind.FLOOR && col.kind() != BeastGraveyard.Kind.RIM) continue;
			if (col.kind() == BeastGraveyard.Kind.FLOOR && nearBones(s, x, z, 3)) continue;
			BlockPos at = c.offset(x, 0, z).atY(col.ground() + 1);
			boolean campNear = false;
			for (BlockPos camp : arrivals) if (camp.distSqr(at.atY(camp.getY())) < 12 * 12) campNear = true;
			if (campNear) continue;
			double roll = random.nextDouble();
			ResourceKey<ConfiguredFeature<?, ?>> kind = roll < 0.18 && r < BeastGraveyard.BASIN_R - 12 ? TreeFeatures.MEGA_JUNGLE_TREE : roll < 0.62 ? TreeFeatures.JUNGLE_TREE
					: TreeFeatures.JUNGLE_BUSH;
			var tree = features.getHolder(kind);
			if (tree.isEmpty()) continue;
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!level.getBlockState(at).isAir() && !level.getBlockState(at).canBeReplaced()) continue;
			if (!level.getBlockState(at).isAir()) put(level, at, Blocks.AIR.defaultBlockState());
			if (tree.get().value().place(level, level.getChunkSource().getGenerator(), random, at)) i++;
		}
	}

	/** A hunters' camp at each arrival: a cold fire, logs to sit on, a lantern on a post. */
	static void camps(ServerLevel level, BeastGraveyard.Shape s, BlockPos c) {
		for (BlockPos at : WORKS.arrivalSpots(s.seed, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			BlockPos fire = at.above();
			for (BlockPos p : BlockPos.betweenClosed(fire.offset(-3, 0, -3), fire.offset(3, 4, 3))) {
				BlockState here = level.getBlockState(p);
				if (!here.isAir() && (here.canBeReplaced() || here.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock)) put(level, p, Blocks.AIR.defaultBlockState());
			}
			put(level, fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
			for (int side = -1; side <= 1; side += 2) {
				put(level, fire.offset(side * 2, 0, 0), Blocks.STRIPPED_JUNGLE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
			}
			put(level, fire.offset(0, 0, 2), Blocks.JUNGLE_FENCE.defaultBlockState());
			put(level, fire.offset(0, 1, 2), lantern(false));
		}
	}

	/** A beast tamers' lodge, fallen in: a stone footing, broken walls of old stone and jungle wood, a vault at its back. */
	static void lodgeAt(ServerLevel level, BeastGraveyard.Shape s, BlockPos c, int k, RandomSource random) {
		Frame f = lodge(s, c, k);
		level.getChunk(f.origin().getX() >> 4, f.origin().getZ() >> 4);
		int floor = f.origin().getY() - 1;
		for (int a = -4; a <= 4; a++) {
			for (int d = -4; d <= 4; d++) {
				BlockPos p = f.at(a, d, 0).atY(floor);
				put(level, p, random.nextFloat() < 0.7 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
				for (int y = 1; y <= 6; y++) put(level, p.above(y), Blocks.AIR.defaultBlockState());
				boolean wall = Math.abs(a) == 4 || Math.abs(d) == 4;
				boolean door = a == 4 && Math.abs(d) <= 1;
				if (!wall || door) continue;
				int height = 1 + random.nextInt(4);
				for (int y = 1; y <= height; y++) put(level, p.above(y), y == 1 ? oldStone(random) : random.nextFloat() < 0.5 ? Blocks.JUNGLE_PLANKS.defaultBlockState()
						: Blocks.MOSSY_COBBLESTONE.defaultBlockState());
			}
		}
		put(level, f.at(0, 2, 0).atY(floor + 1), Blocks.BARREL.defaultBlockState());
		put(level, f.at(1, -3, 0).atY(floor + 1), Blocks.BONE_BLOCK.defaultBlockState());
		join(level, f.at(-5, -5, 0).atY(floor), f.at(5, 5, 0).atY(floor + 6));
	}

	// --- Renewing ---

	/** Its herbs grown again where they were taken, older by a century for every year it slept where they were not. */
	static void renewHerbs(ServerLevel level, BeastGraveyard.Shape s, BlockPos c, SecretRealms.Record r) {
		RandomSource random = RandomSource.create(s.seed ^ r.openings * 0x4E7B6L);
		int bonus = r.realm.years() * HerbValleyWorks.YEARS_A_YEAR_SHUT;
		Block[] kinds = {ModBlocks.SPIRIT_GINSENG, ModBlocks.PURPLE_LINGZHI, ModBlocks.OCHRE_HUANGJING, ModBlocks.GINSENG, ModBlocks.LINGZHI};
		int i = 0;
		for (BlockPos at : herbSpots(s, c)) {
			Block kind = kinds[i++ % kinds.length];
			HerbValleyWorks.renewHerb(level, at, kind, 300 + random.nextInt(900), bonus, Blocks.PODZOL);
		}
	}

	static void renewVaults(ServerLevel level, BeastGraveyard.Shape s, BlockPos c) {
		for (int k = 0; k < 2; k++) {
			BlockPos at = vaultAt(s, c, k);
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			StorageFormations.Tier tier = k == 0 ? StorageFormations.Tier.MID : StorageFormations.Tier.LOW;
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(tier).defaultBlockState());
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, tamersTreasures(k == 0, level.getRandom()));
		}
	}

	/** A beast tamer's hoard: demon cores, a phial of dragon blood, spirit stones, now and then a Departure Jade or Ancient Essence. */
	static List<ItemStack> tamersTreasures(boolean first, RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		for (int i = 0; i < 2 + random.nextInt(2); i++) {
			BeastKind kind = KINDS[random.nextInt(KINDS.length)];
			out.add(DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.values()[random.nextInt(Stage.values().length)]), kind.element, null));
		}
		out.add(new ItemStack(ModItems.MID_SPIRIT_STONE, first ? 3 + random.nextInt(4) : 1 + random.nextInt(3)));
		if (random.nextFloat() < (first ? 0.5f : 0.25f)) out.add(DemonBloodItem.of(Bloodline.DRAGON, Realm.CORE_FORMATION));
		if (random.nextFloat() < 0.35f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (first && random.nextFloat() < 0.2f) out.add(new ItemStack(ModItems.ANCIENT_ESSENCE));
		return out;
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : BeastGraveyard.arrivals(BeastGraveyard.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		BeastGraveyard.Shape s = BeastGraveyard.shape(seed);
		double a = random.nextDouble() * Math.PI * 2, r = 20 + random.nextDouble() * 90;
		int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
		BeastGraveyard.Column col = BeastGraveyard.column(s, x, z);
		return col.kind() == BeastGraveyard.Kind.FLOOR && !nearBones(s, x, z, 0) ? c.offset(x, 0, z).atY(col.ground() + 1) : null;
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 4 + random.nextInt(8)));
		if (random.nextFloat() < 0.6f) carried.add(DemonCoreItem.of(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE), KINDS[random.nextInt(KINDS.length)].element, null));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		BeastGraveyard.Shape s = BeastGraveyard.shape(seed);
		List<Slot> slots = new ArrayList<>();
		RandomSource random = RandomSource.create(seed ^ 0xB5E7L);
		for (int i = 0, tries = 0; i < 24 && tries < 400; tries++) {
			double a = random.nextDouble() * Math.PI * 2, r = 16 + random.nextDouble() * (BeastGraveyard.BASIN_R - 30);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			BeastGraveyard.Column col = BeastGraveyard.column(s, x, z);
			if (col.kind() != BeastGraveyard.Kind.FLOOR || nearBones(s, x, z, -6)) continue;
			BeastKind kind = KINDS[random.nextInt(KINDS.length)];
			slots.add(new Slot("beast" + i, ModEntities.beast(kind), c.offset(x, 0, z).atY(col.ground() + 1), realm.guardianRank(i % 4 == 0 ? Stage.MID : Stage.EARLY), false));
			i++;
		}
		// Three great beasts about the ribs, at the realm's peak.
		BeastKind[] great = {BeastKind.HORNED_THUNDER_RHINO, BeastKind.JADE_SCALED_PYTHON, BeastKind.EARTHQUAKE_BEAR};
		for (int k = 0; k < great.length; k++) {
			Vec3 p = spineAt(s, c, 40 + k * 35);
			double a = s.skullTurn + Math.toRadians(40 + k * 35);
			int x = Mth.floor(p.x + Math.cos(a) * 18), z = Mth.floor(p.z + Math.sin(a) * 18);
			slots.add(new Slot("great" + k, ModEntities.beast(great[k]), new BlockPos(x, groundAt(realm, seed, x, z) + 1, z), realm.guardianRank(Stage.LATE), false));
		}
		slots.add(new Slot("echo", ModEntities.DRAGON_ECHO, echoPost(s, c), realm.guardianRank(Stage.EARLY), true));
		return slots;
	}

	/** The graveyard's beasts: grown past their kind on the dragon's bones, each kept to its own ground. */
	@Override
	protected void raiseCreature(ServerLevel level, SecretRealms.Record r, Slot slot, Mob mob) {
		if (mob instanceof DemonBeast beast) {
			beast.setBoundlessRank(slot.rank());
			beast.guard(slot.home());
		}
	}

	@Override
	protected int calm() { return 30; }

	// --- During an opening ---

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("RoarAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Roar", 0);
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		BeastGraveyard.Shape s = BeastGraveyard.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		roar(level, c, r, now, inside, data);
		if (now % 20 == 7) {
			// The trial told to whoever comes to the skull; the essence to one who bested the echo, at the skull's hollow.
			BlockPos post = echoPost(s, c), essence = essenceSpot(s, c);
			ListTag told = r.works.getList("Told", Tag.TAG_STRING);
			for (ServerPlayer p : inside) {
				if (p.blockPosition().distSqr(post) < 30 * 30 && !told.contains(StringTag.valueOf(p.getStringUUID()))) {
					told.add(StringTag.valueOf(p.getStringUUID()));
					p.sendSystemMessage(Component.translatable(ModLang.MSG_GRAVE_TRIALS, Component.translatable(r.inheritor.isEmpty() ? ModLang.MSG_GRAVE_GREAT
							: ModLang.MSG_GRAVE_LESSER)).withStyle(ChatFormatting.GOLD));
				}
				if (p.blockPosition().distSqr(essence) < 3 * 3 && passed(r, p.getUUID()) && !passed(r, "Claimed", p.getUUID())) {
					pass(r, "Claimed", p.getUUID());
					inherit(level, r, p);
				}
			}
			r.works.put("Told", told);
			if (passedAny(r) && level.isLoaded(essence)) {
				level.sendParticles(ParticleTypes.CRIMSON_SPORE, essence.getX() + 0.5, essence.getY() + 0.8, essence.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
			}
		}
	}

	private static boolean passedAny(SecretRealms.Record r) {
		return !r.works.getList("Passed", Tag.TAG_STRING).isEmpty();
	}

	/** Its hour come, for {@link #ROAR_TICKS}: the bones roar, and every beast in it goes wild after the nearest visitor; then they quiet. */
	static void roar(ServerLevel level, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int stage = r.works.getInt("Roar");
		if (stage == 0 && now >= r.works.getLong("RoarAt")) {
			r.works.putInt("Roar", 1);
			r.works.putLong("RoarEnds", now + ROAR_TICKS);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_GRAVE_ROAR).withStyle(ChatFormatting.RED));
			for (ServerPlayer p : inside) p.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.AMBIENT, 1.4f, 0.5f);
		} else if (stage == 1) {
			if (now >= r.works.getLong("RoarEnds")) {
				r.works.putInt("Roar", 2);
				data.setDirty();
				tellInside(level, r.realm, Component.translatable(ModLang.MSG_GRAVE_ROAR_ENDS).withStyle(ChatFormatting.GRAY));
				return;
			}
			if (now % 20 != 3) return;
			for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(c).inflate(SecretRealm.BOUND, 128, SecretRealm.BOUND), m -> m.getTags().contains(CREATURE_TAG))) {
				m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1, true, false));
				m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 1, true, false));
				if (m.getTarget() == null || !m.getTarget().isAlive()) {
					LivingEntity foe = nearestVisitor(level, m.blockPosition(), 40);
					if (foe != null) m.setTarget(foe);
				}
				if (now % 60 == 3) level.sendParticles(ParticleTypes.ANGRY_VILLAGER, m.getX(), m.getY() + m.getBbHeight(), m.getZ(), 1, 0.2, 0.2, 0.2, 0);
			}
		}
	}

	// --- Its trial and inheritance ---

	@Override
	protected MutableComponent challengeMessage(Component rank) {
		return Component.translatable(ModLang.MSG_GRAVE_ECHO_WAKES, rank);
	}

	@Override
	protected MutableComponent gatePassedMessage() {
		return Component.translatable(ModLang.MSG_GRAVE_ECHO_BOWS);
	}

	/** The Great Inheritance: the Primeval Blood Essence. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(new ItemStack(ModItems.PRIMEVAL_BLOOD_ESSENCE));
	}

	/** A lesser legacy: a phial of dragon blood of the realm it is meant for, a great beast's core, spirit stones. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		return List.of(DemonBloodItem.of(Bloodline.DRAGON, Realm.NASCENT_SOUL), DemonCoreItem.of(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID), QiElement.WOOD, null),
				new ItemStack(ModItems.MID_SPIRIT_STONE, 2 + random.nextInt(3)));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_GRAVE_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_GRAVE_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_GRAVE_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** The nearest herb grown in the dragon's shadow. */
	@Override
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) {
		BlockPos best = null;
		double d = 64 * 64;
		for (BlockPos at : herbSpots(BeastGraveyard.shape(seed), c)) {
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
		BeastGraveyard.Shape s = BeastGraveyard.shape(seed);
		return List.of(vaultAt(s, c, 0), vaultAt(s, c, 1));
	}

	/** The echo bested, then the skull's hollow. */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		BeastGraveyard.Shape s = BeastGraveyard.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		if (passed(r, "Claimed", npc.getUUID())) return null;
		if (passed(r, npc.getUUID())) return new Trial("essence", essenceSpot(s, c), 2.6, false);
		return new Trial("echo", echoPost(s, c), 4, false);
	}

	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		if (trial.step().equals("echo")) return challengeAt(level, r, npc, trial.mark(), working);
		pass(r, "Claimed", npc.getUUID());
		rivalInherits(level, r, npc);
		return true;
	}

	private BeastGraveyardWorks() {}
}
