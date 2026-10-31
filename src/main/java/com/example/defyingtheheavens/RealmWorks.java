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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A secret realm's works (SecretRealm#works): what is built on its land and what goes on in it, the shared machinery here and each realm's
 * own in its subclass (HerbValleyWorks, SwordTombWorks, BattlefieldWorks).
 * <ul>
 *   <li><b>The readying</b>, a step at a time while it stirs ({@link #prepare}, at most {@link #BUDGET_MS} a tick): its buildings laid the
 *   first time ({@link #layOut}); its land mended (RealmLedger); its treasures, traps and the like renewed ({@link #renewal}); a few
 *   earlier visitors' bones laid about it ({@link #remains}). It opens only once all is done ({@link #ready}).</li>
 *   <li><b>Its arrivals</b>: where those who come in are set down ({@link #arrivalSpots}), the one with the fewest about it.</li>
 *   <li><b>Its guardians</b>: every post ({@link #guardianSlots}) raised as someone first comes within {@link #RAISE_NEAR} of it in an
 *   opening, asleep at its post (RealmGuardian); one slain stays slain till the next; all crumble when it closes.</li>
 *   <li><b>Its event and trials</b>, each realm's own ({@link #begin}, {@link #everyTick}, {@link #end}); the last two minutes its
 *   quake ({@link #quake}).</li>
 *   <li><b>Its inheritance</b>: once a world its Great Inheritance ({@link #great}), then a lesser legacy to the first who passes in each
 *   opening ({@link #lesser}); those who passed its trials are kept in its record ({@link #passed}).</li>
 * </ul>
 */
public abstract class RealmWorks {
	/** How near (blocks) one must come for a guardian's post to raise it. */
	static final int RAISE_NEAR = 40;
	/** How many earlier visitors' bones lie about it each opening. */
	static final int REMAINS = 2;
	/** The readying's work a tick: at most this long (ms) of it. */
	static final long BUDGET_MS = 6;

	// --- Each realm's own ---

	/** Its buildings, laid the first time it stirs (as steps, done a little each tick). */
	protected abstract void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r);

	/** What is renewed each time it stirs, after its land is mended: its treasures, its traps, its guardians' things. */
	protected abstract void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r);

	/** Where those who come in are set down (in the world, on the ground). */
	protected abstract List<BlockPos> arrivalSpots(long seed, BlockPos c);

	/** Every guardian's post in it. */
	protected abstract List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm);

	/** Whether a post's guardian may be raised now (some come only with the realm's event). */
	protected boolean raisable(Slot slot, SecretRealms.Record r) { return true; }

	/** An opening begins (its record of the opening new): its event's hour rolled, and the like. */
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {}

	/** Each tick while it stands open, for those {@code inside}: its event, its trials' upkeep. */
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {}

	/** The opening ends (its guardians gone): what was left standing of its event put away. */
	protected void end(ServerLevel level, SecretRealms.Record r) {}

	/** Within this of its middle the closing quake never splits the land (its heart holds). */
	protected int calm() { return 30; }

	/** A spot for an earlier visitor's bones (on the ground, in the world), or null. */
	protected abstract BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random);

	/** What one of those earlier visitors carried. */
	protected abstract List<ItemStack> remainsCarried(RandomSource random);

	/** Their rank when they fell: about the realm's lowest and the realm it is meant for. */
	protected int remainsRank(SecretRealm realm, RandomSource random) {
		int high = realm.guardianRank(Stage.GRAND_PERFECTION);
		return realm.lowest() + random.nextInt(Math.max(1, high - realm.lowest() + 1));
	}

	/** Its Great Inheritance (once a world). */
	protected abstract List<ItemStack> great(ServerLevel level);

	/** A lesser legacy (the first to pass in each opening after the Great is taken). */
	protected abstract List<ItemStack> lesser(ServerLevel level, RandomSource random);

	/** What the world is told when {@code who} takes the Great Inheritance. */
	protected abstract MutableComponent inheritedMessage(Component who, SecretRealm realm);

	/** What one taking a lesser legacy is told (the Great went to {@code inheritor}). */
	protected abstract MutableComponent legacyMessage(String inheritor);

	/** What one who passed is told when this opening's legacy is already taken. */
	protected abstract MutableComponent silentMessage();

	// --- The readying: laid, mended, renewed, a step at a time ---

	/** Each realm's steps still to do before it may open. */
	private static final Map<SecretRealm, Deque<Runnable>> STEPS = new EnumMap<>(SecretRealm.class);

	/** Its land made ready to open: laid if it isn't, mended, renewed, queued to be done a little each tick. */
	static void prepare(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r) {
		ServerLevel level = server.getLevel(ModDimensions.SECRET_REALM);
		if (level == null) return;
		RealmWorks works = r.realm.works();
		Deque<Runnable> steps = new ArrayDeque<>();
		long seed = level.getSeed();
		BlockPos c = r.realm.centre();
		if (!r.laid) {
			works.layOut(steps, level, seed, c, r);
			steps.add(() -> {
				r.laid = true;
				data.setDirty();
			});
		}
		RealmLedger.Mending mending = RealmLedger.get(server).mending(r.realm);
		steps.add(new Runnable() {
			@Override
			public void run() {
				mending.step(level);
				if (!mending.done()) STEPS.get(r.realm).addFirst(this); // the next chunk's, the next time
			}
		});
		works.renewal(steps, level, seed, c, r);
		steps.add(() -> remains(level, seed, c, r));
		STEPS.put(r.realm, steps);
	}

	/** Whether {@code r}'s land is ready to open (nothing left of its readying). */
	static boolean ready(SecretRealms.Record r) {
		Deque<Runnable> steps = STEPS.get(r.realm);
		return steps == null || steps.isEmpty();
	}

	/** All of {@code r}'s readying done now (an opening out of turn). */
	static void finishNow(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r) {
		Deque<Runnable> steps = STEPS.get(r.realm);
		while (steps != null && !steps.isEmpty()) runStep(steps.poll(), r.realm);
	}

	/** Each tick: the readying under way, as much as {@link #BUDGET_MS} allows. */
	static void work(MinecraftServer server, SecretRealms.Data data) {
		long until = System.nanoTime() + BUDGET_MS * 1_000_000L;
		for (Map.Entry<SecretRealm, Deque<Runnable>> entry : STEPS.entrySet()) {
			Deque<Runnable> steps = entry.getValue();
			while (!steps.isEmpty() && System.nanoTime() < until) runStep(steps.poll(), entry.getKey());
		}
	}

	private static void runStep(Runnable step, SecretRealm realm) {
		try {
			RealmLedger.quietly(step);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Readying {} failed a step", realm.getId(), e);
		}
	}

	static void clear() {
		STEPS.clear();
	}

	// --- Building ---

	/** A frame to build in: its origin, the way it faces (forward), and its right; (f, s, y) from it. */
	record Frame(BlockPos origin, Direction forward) {
		Direction right() { return forward.getClockWise(); }

		BlockPos at(int f, int s, int y) {
			return origin.relative(forward, f).relative(right(), s).above(y);
		}

		/** {@code pos} in this frame: (f, s, y), {@link #at}'s other way. */
		int[] local(BlockPos pos) {
			int dx = pos.getX() - origin.getX(), dz = pos.getZ() - origin.getZ();
			Direction r = right();
			return new int[] {dx * forward.getStepX() + dz * forward.getStepZ(), dx * r.getStepX() + dz * r.getStepZ(), pos.getY() - origin.getY()};
		}
	}

	static void put(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_CLIENTS);
	}

	/** Walls, fences and stairs in {@code box} joined to what stands about them (laid without their neighbours' word). */
	static void join(ServerLevel level, BlockPos from, BlockPos to) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof WallBlock || state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof StairBlock) {
				BlockState joined = Block.updateFromNeighbourShapes(state, level, pos);
				if (joined != state) level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
			}
		}
	}

	static BlockState stairs(Block block, Direction facing, boolean top) {
		return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	static BlockState slab(Block block, boolean top) {
		return block.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	static BlockState log(Block block) {
		return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	}

	static BlockState lantern(boolean hanging) {
		return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, hanging);
	}

	/** Old stone, weathered: bricks, mossy and cracked. */
	static BlockState oldStone(RandomSource random) {
		float roll = random.nextFloat();
		return (roll < 0.45 ? Blocks.STONE_BRICKS : roll < 0.75 ? Blocks.MOSSY_STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS).defaultBlockState();
	}

	/** The ground's top at world column ({@code x}, {@code z}) of {@code realm}'s land (from the land itself: no chunk needed). */
	static int groundAt(SecretRealm realm, long seed, int x, int z) {
		BlockPos c = realm.centre();
		return realm.land().ground(seed, x - c.getX(), z - c.getZ());
	}

	/** A level floor at {@code y} over the rectangle (world) of {@code realm}'s land: under it filled up to it, over it cleared {@code clear} high. */
	static void levelOff(ServerLevel level, SecretRealm realm, BlockPos a, BlockPos b, int y, BlockState top, BlockState fill, int clear) {
		long seed = level.getSeed();
		for (int x = Math.min(a.getX(), b.getX()); x <= Math.max(a.getX(), b.getX()); x++) {
			for (int z = Math.min(a.getZ(), b.getZ()); z <= Math.max(a.getZ(), b.getZ()); z++) {
				int g = groundAt(realm, seed, x, z);
				for (int dy = Math.min(g, y) - 2; dy < y; dy++) put(level, new BlockPos(x, dy, z), fill);
				put(level, new BlockPos(x, y, z), top);
				for (int dy = y + 1; dy <= Math.max(g, y) + clear; dy++) put(level, new BlockPos(x, dy, z), Blocks.AIR.defaultBlockState());
			}
		}
	}

	// --- The bones of earlier visitors ---

	/** A few earlier visitors' bones about it, with what they carried. */
	static void remains(ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		RealmWorks works = r.realm.works();
		RandomSource random = level.getRandom();
		for (int i = 0, tries = 0; i < REMAINS && tries < 40; tries++) {
			BlockPos at = works.remainsSpot(level, seed, c, random);
			if (at == null) continue;
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!level.getBlockState(at).isAir() && !level.getBlockState(at).canBeReplaced() || !level.getBlockState(at.below()).isSolid()) continue;
			put(level, at, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.STAGE, CorpseBlock.Rot.BONES));
			if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) {
				body.ancient(NpcNames.of(UUID.randomUUID()), random.nextInt(CultivatorNpc.SKINS), works.remainsRank(r.realm, random), random.nextFloat() * 360,
						level.getGameTime(), works.remainsCarried(random));
			}
			i++;
		}
	}

	// --- The arrivals, in use ---

	/** Where one coming in is set down: an arrival spot, the one with the fewest about it (players and visitors), a little off its middle. */
	static BlockPos arrival(ServerLevel level, SecretRealms.Record r, RandomSource random) {
		List<BlockPos> at = new ArrayList<>(r.realm.works().arrivalSpots(level.getSeed(), r.realm.centre()));
		Collections.shuffle(at, new java.util.Random(random.nextLong()));
		BlockPos best = null;
		int fewest = Integer.MAX_VALUE;
		for (BlockPos w : at) {
			int here = level.getEntitiesOfClass(LivingEntity.class, new AABB(w).inflate(16), e -> e instanceof ServerPlayer || e instanceof CultivatorNpc).size();
			if (here < fewest) {
				fewest = here;
				best = w;
			}
		}
		return best.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
	}

	/** The arrival spot nearest {@code pos} (one caught straying is set back there). */
	static BlockPos nearestArrival(ServerLevel level, SecretRealms.Record r, BlockPos pos) {
		BlockPos best = null;
		double d = Double.MAX_VALUE;
		for (BlockPos w : r.realm.works().arrivalSpots(level.getSeed(), r.realm.centre())) {
			double here = w.distSqr(pos.atY(w.getY()));
			if (here < d) {
				d = here;
				best = w;
			}
		}
		return best;
	}

	// --- During an opening ---

	/**
	 * A guardian's post: its id in the works, its kind, where, its rank, whether it is the trial's (the Gatekeeper, a duelist). Its kind may be
	 * a creature that is no guardian of the realm's own (the Graveyard's wild beasts): set up by {@link #raiseCreature}, tagged to the
	 * opening ({@link #CREATURE_TAG}), gone when it closes.
	 */
	record Slot(String id, EntityType<? extends Mob> type, BlockPos home, int rank, boolean gatekeeper) {}

	/** The tag a realm's creatures (not its own guardians) carry, and the one naming the opening they belong to ({@link #openingTag}). */
	static final String CREATURE_TAG = "dth_realm_creature";

	static String openingTag(SecretRealms.Record r) {
		return "dth_realm_" + r.realm.getId() + "_" + r.openings;
	}

	/** A post's creature that is no guardian of the realm's own (the Graveyard's beasts), set up for its post before it comes into the world. */
	protected void raiseCreature(ServerLevel level, SecretRealms.Record r, Slot slot, Mob mob) {}

	/**
	 * A realm's creature come into the world again (its chunk loaded): gone if its opening is over (the realm shut, or opened again since),
	 * as the guardians crumble (ENTITY_LOAD, SecretRealms#register).
	 */
	static void creatureLoaded(Entity entity, ServerLevel level) {
		if (!entity.getTags().contains(CREATURE_TAG) || !ModDimensions.isSecretRealm(level.dimension())) return;
		SecretRealm realm = SecretRealm.at(entity.getX(), entity.getZ());
		SecretRealms.Record r = realm == null ? null : SecretRealms.Data.get(level.getServer()).of(realm);
		if (r == null || r.openedAt < 0 || !entity.getTags().contains(openingTag(r))) entity.discard();
	}

	/** The opening begins: its works' record of this opening new, the realm's own first entries in it. */
	static void opened(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		r.works = new CompoundTag();
		r.realm.works().begin(r, now, server.overworld().getRandom());
		data.setDirty();
	}

	/** The opening ends: its guardians crumble (those loaded; the rest as they load), what was left of its event put away. */
	static void closed(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r) {
		ServerLevel level = server.getLevel(ModDimensions.SECRET_REALM);
		if (level == null) return;
		BlockPos c = r.realm.centre();
		for (RealmGuardian guardian : level.getEntitiesOfClass(RealmGuardian.class, new AABB(c).inflate(SecretRealm.BOUND, 160, SecretRealm.BOUND))) guardian.discard();
		for (Mob creature : level.getEntitiesOfClass(Mob.class, new AABB(c).inflate(SecretRealm.BOUND, 160, SecretRealm.BOUND), m -> m.getTags().contains(CREATURE_TAG))) {
			creature.discard();
		}
		r.realm.works().end(level, r);
	}

	/** Each tick while it stands open: its guardians raised as visitors come near; its event and trials; its quake at the end. */
	static void tickOpen(MinecraftServer server, SecretRealms.Data data, SecretRealms.Record r, long now) {
		ServerLevel level = server.getLevel(ModDimensions.SECRET_REALM);
		if (level == null) return;
		List<ServerPlayer> inside = new ArrayList<>();
		for (ServerPlayer p : level.players()) if (SecretRealm.at(p.getX(), p.getZ()) == r.realm && !p.isSpectator()) inside.add(p);
		if (now % 20 == 5 && !inside.isEmpty()) raise(level, r, inside, data);
		r.realm.works().everyTick(level, data, r, now, inside);
		if (SecretRealms.phase(r, now) == SecretRealms.Phase.CLOSING) quake(level, r, now, inside);
	}

	/** The posts near any of {@code inside} not raised yet this opening (nor slain), their guardians raised now, asleep at their posts. */
	private static void raise(ServerLevel level, SecretRealms.Record r, List<ServerPlayer> inside, SecretRealms.Data data) {
		RealmWorks works = r.realm.works();
		ListTag raised = r.works.getList("Raised", Tag.TAG_STRING);
		Set<String> done = new HashSet<>();
		for (Tag t : raised) done.add(t.getAsString());
		boolean changed = false;
		for (Slot slot : works.guardianSlots(level.getSeed(), r.realm.centre(), r.realm)) {
			if (done.contains(slot.id()) || !works.raisable(slot, r)) continue;
			boolean near = false;
			for (ServerPlayer p : inside) if (p.blockPosition().distSqr(slot.home()) < RAISE_NEAR * RAISE_NEAR) near = true;
			if (!near || !level.isLoaded(slot.home())) continue;
			Mob mob = slot.type().create(level);
			if (mob == null) continue;
			mob.moveTo(slot.home().getX() + 0.5, slot.home().getY(), slot.home().getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
			if (mob instanceof RealmGuardian guardian) {
				guardian.setUp(r.realm, slot.rank(), slot.home(), r.openings, slot.id(), slot.gatekeeper());
			} else {
				mob.addTag(CREATURE_TAG);
				mob.addTag(openingTag(r));
				works.raiseCreature(level, r, slot, mob);
			}
			level.addFreshEntity(mob);
			raised.add(StringTag.valueOf(slot.id()));
			changed = true;
		}
		if (changed) {
			r.works.put("Raised", raised);
			data.setDirty();
		}
	}

	/** A guardian slain: its post stays empty till the next opening (it is already marked raised). */
	static void guardianSlain(ServerLevel level, SecretRealm realm, String slot) {
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		SecretRealms.Record r = data.of(realm);
		ListTag slain = r.works.getList("Slain", Tag.TAG_STRING);
		slain.add(StringTag.valueOf(slot));
		r.works.put("Slain", slain);
		data.setDirty();
	}

	/** A vault forced in a realm (StorageFormations#force): the guardians about it wake, after its forcer. */
	static void vaultForced(ServerLevel level, BlockPos pos, ServerPlayer forcer) {
		wakeAround(level, pos, forcer);
	}

	/** The guardians about {@code pos} in a realm wake, after {@code foe} (a vault forced, a treasure taken, by a player or one come for it). */
	static void wakeAround(ServerLevel level, BlockPos pos, LivingEntity foe) {
		if (SecretRealms.realmAt(level, pos.getX(), pos.getZ()) == null) return;
		for (RealmGuardian guardian : level.getEntitiesOfClass(RealmGuardian.class, new AABB(pos).inflate(14), g -> !g.isGatekeeper())) guardian.wake(foe);
	}

	static LivingEntity nearestVisitor(ServerLevel level, BlockPos pos, int within) {
		LivingEntity best = null;
		double d = Double.MAX_VALUE;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(within), RealmGuardian::validFoe)) {
			double here = e.distanceToSqr(Vec3.atCenterOf(pos));
			if (here < d) {
				d = here;
				best = e;
			}
		}
		return best;
	}

	static void tellInside(ServerLevel level, SecretRealm realm, Component message) {
		for (ServerPlayer p : level.players()) if (SecretRealm.at(p.getX(), p.getZ()) == realm) p.sendSystemMessage(message);
	}

	/** Everyone in {@code realm} who isn't looking on. */
	static List<ServerPlayer> inside(ServerLevel level, SecretRealm realm) {
		List<ServerPlayer> inside = new ArrayList<>();
		for (ServerPlayer p : level.players()) if (SecretRealm.at(p.getX(), p.getZ()) == realm && !p.isSpectator()) inside.add(p);
		return inside;
	}

	// --- The close: its quake ---

	/** Its last two minutes: the ground rumbling about everyone inside, dust falling, and now and then the land splitting near them (not under them). */
	static void quake(ServerLevel level, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		if (now % 30 == 0) {
			for (ServerPlayer p : inside) {
				p.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 0.5f, 0.4f + p.getRandom().nextFloat() * 0.2f);
				level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.STONE.defaultBlockState()), p.getX(), p.getY() + 8, p.getZ(), 30, 8, 2, 8, 0);
			}
		}
		if (now % 80 != 40 || inside.isEmpty()) return;
		BlockPos c = r.realm.centre();
		ServerPlayer near = inside.get(level.getRandom().nextInt(inside.size()));
		RandomSource random = level.getRandom();
		double a = random.nextDouble() * Math.PI * 2, d = 8 + random.nextDouble() * 12;
		int x = Mth.floor(near.getX() + Math.cos(a) * d), z = Mth.floor(near.getZ() + Math.sin(a) * d);
		if (Math.hypot(x - c.getX(), z - c.getZ()) < r.realm.works().calm()) return; // the heart holds
		double along = random.nextDouble() * Math.PI;
		int length = 5 + random.nextInt(6);
		for (int k = -length / 2; k <= length / 2; k++) {
			int fx = x + (int) Math.round(Math.cos(along) * k), fz = z + (int) Math.round(Math.sin(along) * k);
			boolean someone = false;
			for (ServerPlayer p : inside) if (Math.abs(p.getX() - fx) < 3 && Math.abs(p.getZ() - fz) < 3) someone = true;
			if (someone || !level.isLoaded(new BlockPos(fx, 0, fz))) continue;
			int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fx, fz) - 1;
			for (int y = top; y > top - 3; y--) {
				BlockPos p = new BlockPos(fx, y, fz);
				BlockState here = level.getBlockState(p);
				if (here.getBlock() instanceof CorpseBlock || here.getBlock() instanceof StorageFormationBlock || here.is(ModBlocks.KILLING_FORMATION)) break;
				level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), fx + 0.5, top + 0.5, fz + 0.5, 10, 0.4, 0.4, 0.4, 0.1);
		}
		level.playSound(null, x, near.getY(), z, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 1.0f, 0.5f);
	}

	// --- Its trials and inheritance ---

	/** One of the realm's trial guardians (the Gatekeeper, a duelist) brought down by {@code winner} (RealmGuardian#hurt): they have passed. */
	static void passedGate(ServerLevel level, SecretRealm realm, UUID winner, Entity by) {
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		SecretRealms.Record r = data.of(realm);
		pass(r, "Passed", winner);
		data.setDirty();
		if (by instanceof ServerPlayer p) {
			p.sendSystemMessage(realm.works().gatePassedMessage().withStyle(ChatFormatting.GOLD));
			p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
			SecretRealms.HUD_SHOWN.remove(p.getUUID()); // their row (and the barrier's word with it) sent again
		}
		realm.works().gatePassed(level, r, winner, by);
	}

	/** What one who brought down the realm's trial guardian is told. */
	protected MutableComponent gatePassedMessage() { return Component.translatable(ModLang.MSG_GATEKEEPER_PASSED); }

	/** Why {@code p} may not challenge the realm's trial guardian yet (null: they may), as some realms ask a trial passed first. */
	protected Component mayChallenge(SecretRealms.Record r, LivingEntity p) { return null; }

	/**
	 * Whether the realm's trial guardian lets {@code p} pass unfought as they challenge it (the Ferryman: the righteous, and those who pay his
	 * toll); the passing is then theirs as if they had bested it (RealmGuardian#challenge).
	 */
	protected boolean passesUnfought(ServerLevel level, SecretRealms.Record r, LivingEntity p) { return false; }

	/** {@code p} touches the realm's trial guardian {@code guardian} (right-click): whether its realm answers that itself, before any challenge. */
	protected boolean guardianTouched(ServerLevel level, SecretRealms.Record r, RealmGuardian guardian, net.minecraft.world.entity.player.Player p) { return false; }

	/**
	 * Whether {@code pos} in it holds now against a hand, a blast or a spell (the Cave Abode's maze; the Ruins' ancestral hall while its
	 * formation stands; the Tomb's halls and sanctum): what one who tries is told (a lang key), or null.
	 */
	protected String holds(ServerLevel level, SecretRealms.Record r, BlockPos pos) { return null; }

	/** Whether {@code pos} holds in the secret realm it lies in, if any ({@link #holds}): what one who tries is told, or null. */
	public static String holdsAt(net.minecraft.world.level.Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel server) || !ModDimensions.isSecretRealm(server.dimension())) return null;
		SecretRealm realm = SecretRealm.at(pos.getX(), pos.getZ());
		return realm == null ? null : realm.works().holds(server, SecretRealms.Data.get(server.getServer()).of(realm), pos);
	}

	/** What a challenger is told as the trial guardian wakes to meet them at {@code rank} (named). */
	protected MutableComponent challengeMessage(Component rank) { return Component.translatable(ModLang.MSG_GATEKEEPER_WAKES, rank); }

	/** After {@code winner} brought down the realm's trial guardian (their passing already kept): the realm's own next step. */
	protected void gatePassed(ServerLevel level, SecretRealms.Record r, UUID winner, Entity by) {}

	/** {@code id} marked as having passed the trial kept under {@code key} in this opening's record. */
	static void pass(SecretRealms.Record r, String key, UUID id) {
		ListTag passed = r.works.getList(key, Tag.TAG_STRING);
		StringTag tag = StringTag.valueOf(id.toString());
		if (!passed.contains(tag)) passed.add(tag);
		r.works.put(key, passed);
	}

	/** Whether {@code id} has passed the trial kept under {@code key} in the opening under way. */
	static boolean passed(SecretRealms.Record r, String key, UUID id) {
		return r.openedAt >= 0 && r.works.getList(key, Tag.TAG_STRING).contains(StringTag.valueOf(id.toString()));
	}

	/** Whether {@code entity} has passed its realm's barrier's trial this opening (TrialBarrierBlock, server side). */
	static boolean hasPassed(Entity entity) {
		if (!(entity.level() instanceof ServerLevel level)) return false;
		SecretRealm realm = SecretRealms.realmAt(level, entity.getX(), entity.getZ());
		if (realm == null) return false;
		return realm.works().barrierPasses(SecretRealms.Data.get(level.getServer()).of(realm), entity.getUUID());
	}

	/** Whether the realm's trial barrier lets {@code id} through: by default, those who brought down its trial guardian. */
	protected boolean barrierPasses(SecretRealms.Record r, UUID id) { return passed(r, id); }

	/** Whether {@code r}'s trial barrier lets {@code id} through (told to the client with its HUD row: ModPackets#sendSecretRealm). */
	static boolean barrierLets(SecretRealms.Record r, UUID id) {
		return r.realm.works().barrierPasses(r, id);
	}

	/** Whether {@code id} has passed its realm's first trial (its trial guardian) this opening. */
	public static boolean passed(SecretRealms.Record r, UUID id) {
		return passed(r, "Passed", id);
	}

	/**
	 * {@code p} has passed every trial: the Great Inheritance if nobody in this world has taken it (the world told, the tales tell of it), else
	 * the lesser legacy of this opening if it is still there, else nothing more this opening.
	 */
	static void inherit(ServerLevel level, SecretRealms.Record r, ServerPlayer p) {
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		RealmWorks works = r.realm.works();
		if (r.inheritor.isEmpty()) {
			r.inheritor = p.getScoreboardName();
			r.inheritorId = p.getUUID();
			r.inheritorNpc = false;
			data.setDirty();
			for (ItemStack stack : works.great(level)) give(p, stack);
			level.getServer().getPlayerList().broadcastSystemMessage(works.inheritedMessage(p.getDisplayName(), r.realm).withStyle(ChatFormatting.GOLD), false);
			Rumours.noteRealmInheritor(r.realm, p.getScoreboardName(), level.getGameTime());
			Milestones.award(p, Milestones.SECRET_INHERITOR);
			level.playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1.0, 0.6, 0.3);
			return;
		}
		if (r.works.getBoolean("Legacy")) {
			p.sendSystemMessage(works.silentMessage().withStyle(ChatFormatting.GRAY));
			return;
		}
		r.works.putBoolean("Legacy", true);
		r.legacy(p.getScoreboardName());
		data.setDirty();
		for (ItemStack stack : works.lesser(level, level.getRandom())) give(p, stack);
		p.sendSystemMessage(works.legacyMessage(r.inheritor).withStyle(ChatFormatting.GOLD));
		level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	/** A cultivator who came in has passed every trial (SecretRealmRivals): the Great Inheritance, or this opening's lesser legacy. @return whether the Great */
	static boolean inheritNpc(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		RealmWorks works = r.realm.works();
		String name = npc.getPersonalName();
		if (r.inheritor.isEmpty()) {
			r.inheritor = name;
			r.inheritorId = npc.getUUID();
			r.inheritorNpc = true;
			data.setDirty();
			for (ItemStack stack : works.great(level)) npc.addWinnings(stack);
			level.getServer().getPlayerList().broadcastSystemMessage(works.inheritedMessage(npc.getDisplayName(), r.realm).withStyle(ChatFormatting.GOLD), false);
			Rumours.noteRealmInheritor(r.realm, name, level.getGameTime());
			return true;
		}
		if (r.works.getBoolean("Legacy")) return false;
		r.works.putBoolean("Legacy", true);
		r.legacy(name);
		data.setDirty();
		for (ItemStack stack : works.lesser(level, level.getRandom())) npc.addWinnings(stack);
		return false;
	}

	static void give(ServerPlayer p, ItemStack stack) {
		if (!p.getInventory().add(stack)) p.drop(stack, false);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/**
	 * A step of one come for it on its way to the inheritance (SecretRealmRivals.VisitGoal): its realm's word for it, where to go, how near to
	 * come, and whether it must be walked (never flown).
	 */
	record Trial(String step, BlockPos mark, double reach, boolean onFoot) {}

	/**
	 * The column one of those come for it is raised at, in it {@code along} of the opening already: by default from one of its arrivals, that
	 * share of the way toward its heart (never into its calm middle).
	 */
	protected BlockPos rivalStart(ServerLevel level, long seed, BlockPos c, double along, RandomSource random) {
		List<BlockPos> arrivals = arrivalSpots(seed, c);
		BlockPos from = arrivals.get(random.nextInt(arrivals.size()));
		double dx = c.getX() - from.getX(), dz = c.getZ() - from.getZ(), d = Math.max(1, Math.hypot(dx, dz));
		double go = Mth.clamp(along * d, 0, Math.max(0, d - calm() - 6)) + random.nextInt(5) - 2;
		return new BlockPos(from.getX() + (int) Math.round(dx / d * go), 0, from.getZ() + (int) Math.round(dz / d * go));
	}

	/** The nearest thing about {@code near} one come for it would take (a herb, an old sword, a fallen soldier's things), or null. */
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) { return null; }

	/** One come for it takes what lies at {@code at} (as a player taking it would leave it): what it got. */
	protected List<ItemStack> rivalTake(ServerLevel level, BlockPos at, CultivatorNpc npc) { return List.of(); }

	/** Its vaults (storage formations sealed for the ancients), where each stands. */
	protected List<BlockPos> vaults(long seed, BlockPos c) { return List.of(); }

	/** Where {@code npc} goes next on its way to the inheritance, or null when it has done all it can this opening. */
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) { return null; }

	/** {@code npc} at {@code trial}'s mark, its {@code working}th tick there: whether that step is over (it chooses its next). */
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) { return true; }

	/** One come for it ({@code id}) gives up on its trials this opening: a step it made no way at, or its best not enough. */
	static void giveUp(SecretRealms.Record r, UUID id) {
		pass(r, "GaveUp", id);
	}

	static boolean gaveUp(SecretRealms.Record r, UUID id) {
		return passed(r, "GaveUp", id);
	}

	/**
	 * {@code npc} at its realm's trial guardian's post ({@code post}): it challenges it (and fights it). @return whether that step is over:
	 * passed, or given up (no guardian there for ten seconds, or a minute's fighting without winning)
	 */
	static boolean challengeAt(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, BlockPos post, int working) {
		if (passed(r, npc.getUUID())) return true;
		RealmGuardian keeper = null;
		for (RealmGuardian g : level.getEntitiesOfClass(RealmGuardian.class, new AABB(post).inflate(8), RealmGuardian::isGatekeeper)) keeper = g;
		if (keeper == null || working > 1200) {
			if (keeper != null || working > 200) {
				giveUp(r, npc.getUUID());
				return true;
			}
			return false;
		}
		keeper.challenge(npc);
		if (keeper.getTarget() == npc) npc.setTarget(keeper);
		return false;
	}

	/** {@code npc} has passed every trial and takes what waits (RealmWorks#inheritNpc), marked so on its roll if it was the Great. */
	static void rivalInherits(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		if (inheritNpc(level, r, npc)) SecretRealmRivals.markHeir(level, r, npc.getUUID());
		level.playSound(null, npc.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0f, 1.0f);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, npc.getX(), npc.getY() + 1, npc.getZ(), 30, 0.5, 0.8, 0.5, 0.2);
	}
}
