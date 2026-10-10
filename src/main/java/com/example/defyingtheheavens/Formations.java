package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Raises, repairs and lowers the barriers of every {@link Formation}, and makes sure none is ever left behind.
 * <ul>
 *   <li>Raising: a raised formation's shell is filled in a few thousand cells a tick (air and other replaceable blocks only;
 *   water, leaves and buildings are left alone), only in loaded chunks. After the first full pass it keeps going round
 *   slowly, putting back blocks that were broken (a core pays qi for each, see FormationCoreBlockEntity#payRepair) and filling
 *   chunks that loaded since.</li>
 *   <li>Lowering (switched off, out of qi, shattered) and destroying a formation clears every barrier block of it at once in
 *   loaded chunks; unloaded chunks of the shell are remembered ({@link FormationData#pendingClear}) and swept the moment they
 *   load. As a last resort, a barrier block that a random tick finds on no raised formation's shell removes itself.</li>
 * </ul>
 * Breaking a barrier block takes a cultivator at least as strong as the formation (see {@link #canBreak}); the ground the
 * shell runs through can't be dug by anyone weaker either ({@link #protectsGround}), so it can't be tunnelled under, and
 * anyone weaker found inside it anyway is put out ({@link #guard}). Monsters can't spawn inside, and any that get in are
 * swept out ({@link #purge}).
 */
public final class Formations {
	public static final int MIN_RADIUS = 4;
	public static final int MAX_RADIUS = 64;
	/** Ground within this of the shell is as hard to dig through as the barrier itself. */
	public static final double GROUND_BAND = 1.5;
	/** Barrier cells handled per tick: a formation being raised, one being kept up, and all of them together. */
	private static final int RAISE_BUDGET = 2048;
	private static final int REPAIR_BUDGET = 256;
	private static final int TOTAL_BUDGET = 6144;
	/** Ticks a barrier takes to break for a cultivator exactly as strong as its formation; halved for each stage beyond. */
	public static final int BREAK_TICKS_AT_EQUAL = 100;
	private static final int SYNC_INTERVAL = 40;
	/** Ticks between two sweeps of each raised formation for monsters inside it. */
	private static final int PURGE_INTERVAL = 10;
	private static final double SYNC_RANGE = 256;
	/** A concealment barrier whose cultivator hasn't been seen for this long (its chunk loaded) is dissolved. */
	private static final long CONCEALMENT_GRACE = 600;

	/** Client hook: the formation whose shell holds {@code pos}, from the formations the server last sent (see ModPackets). */
	public static ClientLookup client = pos -> null;

	@FunctionalInterface
	public interface ClientLookup {
		Formation find(BlockPos pos);
	}

	private record Sweep(ServerLevel level, long chunk) {}

	private static final Deque<Sweep> SWEEPS = new ArrayDeque<>();
	private static final Map<UUID, Long> ATTACK_COOLDOWN = new HashMap<>();
	private static boolean syncSoon;

	// --- Lookup ---

	/** The raised formation whose shell holds {@code pos}, on whichever side {@code level} is; null if none. */
	public static Formation at(BlockGetter level, BlockPos pos) {
		if (level instanceof ServerLevel server) return onShell(server, pos, null);
		if (level instanceof Level world && world.isClientSide) return client.find(pos);
		return null;
	}

	/** A raised formation other than {@code except} whose shell holds {@code pos}. */
	private static Formation onShell(ServerLevel level, BlockPos pos, Formation except) {
		for (Formation formation : FormationData.get(level).all()) {
			if (formation != except && formation.isRaised() && formation.isOnShell(pos)) return formation;
		}
		return null;
	}

	public static Formation get(ServerLevel level, UUID id) {
		return FormationData.get(level).get(id);
	}

	/** The raised formation, if any, whose shell runs through the ground at {@code pos}. */
	public static Formation groundAt(ServerLevel level, BlockPos pos) {
		for (Formation formation : FormationData.get(level).all()) {
			if (formation.isRaised() && formation.isNearShell(pos, GROUND_BAND)) return formation;
		}
		return null;
	}

	/** Every raised formation in {@code level} enclosing {@code pos}. */
	public static List<Formation> enclosing(ServerLevel level, BlockPos pos) {
		List<Formation> found = new ArrayList<>();
		for (Formation formation : FormationData.get(level).all()) {
			if (formation.isRaised() && formation.encloses(pos)) found.add(formation);
		}
		return found;
	}

	// --- Keeping monsters out ---

	/** Whether a raised formation in {@code level} encloses {@code pos}: no monster may be born there. */
	public static boolean shelters(ServerLevel level, BlockPos pos) {
		for (Formation formation : FormationData.get(level).all()) {
			if (formation.isRaised() && formation.encloses(pos)) return true;
		}
		return false;
	}

	/**
	 * The monsters a formation keeps out: every hostile mob but the bosses, and the Inner Realm's heart demons (part of its
	 * trial, not strays).
	 */
	public static boolean isMonster(Entity entity) {
		return entity instanceof Mob && entity instanceof Enemy && !(entity instanceof WitherBoss || entity instanceof EnderDragon
				|| entity instanceof ElderGuardian || entity instanceof HeartDemonEntity);
	}

	/**
	 * Monsters that got inside anyway (a phantom, an enderman's jump, a pursuer swimming in where water breaks the shell, one
	 * spawned by a spawner) are driven out: a wild one is dispersed, one that must stay in the world (named, or carrying
	 * loot) is set down outside the barrier.
	 */
	private static void purge(ServerLevel level, Formation formation) {
		double reach = formation.getRadius() + 1;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(formation.center).inflate(reach),
				m -> m.isAlive() && isMonster(m) && formation.encloses(m.blockPosition()))) {
			level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
			if (!mob.isPersistenceRequired() && !mob.requiresCustomPersistence()) {
				mob.discard();
			} else {
				expel(level, formation, mob);
			}
		}
	}

	/**
	 * The shell itself has no openings (see {@link #fillFor}), so this is only for what jumps over it: chorus fruit, a Nether
	 * portal that opened inside, a respawn point or a body left inside. Nobody the barrier would stop stays inside it: a player
	 * not strong enough to break it (see {@link #canBreak}) is set down just outside it.
	 */
	private static void guard(ServerLevel level, Formation formation) {
		for (ServerPlayer player : level.players()) {
			if (player.isCreative() || player.isSpectator() || !player.isAlive() || !formation.encloses(player.blockPosition())) continue;
			if (canBreak(player, formation)) continue;
			if (expel(level, formation, player)) player.displayClientMessage(Component.translatable(ModLang.MSG_BARRIER_EXPELLED), true);
		}
	}

	/**
	 * Puts {@code entity} down on the ground just outside {@code formation}, on its own side of it. @return false if that
	 * ground isn't loaded (it is tried again on the next sweep). Package-private for the game tests.
	 */
	static boolean expel(ServerLevel level, Formation formation, LivingEntity entity) {
		Vec3 centre = Vec3.atCenterOf(formation.center);
		Vec3 out = entity.position().subtract(centre).multiply(1, 0, 1);
		out = out.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : out.normalize();
		double distance = formation.getRadius() + 3;
		int x = Mth.floor(centre.x + out.x * distance), z = Mth.floor(centre.z + out.z * distance);
		if (!level.isLoaded(new BlockPos(x, entity.getBlockY(), z))) return false;
		int y = surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (entity instanceof Mob mob) {
			mob.getNavigation().stop();
			mob.setTarget(null);
		}
		entity.stopRiding();
		if (entity instanceof ServerPlayer player) {
			player.teleportTo(level, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
		} else {
			entity.teleportTo(x + 0.5, y, z + 0.5);
		}
		entity.fallDistance = 0;
		return true;
	}

	/**
	 * The height of the ground at {@code x, z} by the {@code type} heightmap, looking through any barrier there: barrier blocks
	 * block motion, so vanilla's heightmaps take a dome for the ground, but nothing should land or be set down on top of one.
	 */
	public static int surface(Level level, Heightmap.Types type, int x, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, level.getHeight(type, x, z) - 1, z);
		while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos).getBlock() instanceof SectBarrierBlock) {
			do {
				pos.move(0, -1, 0);
			} while (pos.getY() > level.getMinBuildHeight() && !type.isOpaque().test(level.getBlockState(pos)));
		}
		return pos.getY() + 1;
	}

	// --- Breaking through ---

	/** A player's cultivation rank as a formation weighs it: their sustained rank, or -1 for a mortal. */
	public static int rankOf(Player player) {
		PlayerCultivation c = CultivationManager.forPlayer(player);
		return c.isMortal() ? -1 : c.sustainedRank();
	}

	/** Strong enough to force a way through (or simply let through). */
	public static boolean canBreak(Player player, Formation formation) {
		return formation.allows(player) || rankOf(player) >= formation.getRank();
	}

	/** Ticks breaking one barrier block takes {@code player}, or -1 if they can't. */
	public static int breakTicks(Player player, Formation formation) {
		if (!canBreak(player, formation)) return -1;
		int gap = Math.max(0, rankOf(player) - formation.getRank());
		return Math.max(4, BREAK_TICKS_AT_EQUAL >> Math.min(gap, 5));
	}

	/** Whether {@code player} may dig the block at {@code pos}: false where a formation's shell runs through the ground. */
	public static boolean protectsGround(ServerPlayer player, BlockPos pos) {
		if (player.isCreative()) return false;
		Formation formation = groundAt(player.serverLevel(), pos);
		if (formation == null || canBreak(player, formation)) return false;
		player.displayClientMessage(Component.translatable(ModLang.MSG_BARRIER_GROUND), true);
		return true;
	}

	/** A player struck a barrier block (whether or not they can break it). Throttled to once a second per player. */
	public static void onAttacked(ServerLevel level, BlockPos pos, ServerPlayer player) {
		Formation formation = onShell(level, pos, null);
		if (formation == null) return;
		long now = level.getGameTime();
		Long last = ATTACK_COOLDOWN.get(player.getUUID());
		if (last != null && now - last < 20) return;
		ATTACK_COOLDOWN.put(player.getUUID(), now);
		if (!canBreak(player, formation)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_BARRIER_TOO_STRONG), true);
		}
		assailed(level, formation, player, pos, false);
	}

	/** A player broke through a barrier block. */
	public static void onBroken(ServerLevel level, BlockPos pos, ServerPlayer player) {
		Formation formation = onShell(level, pos, null);
		if (formation != null) assailed(level, formation, player, pos, true);
	}

	private static void assailed(ServerLevel level, Formation formation, ServerPlayer player, BlockPos pos, boolean broken) {
		if (formation.allows(player) || player.isCreative() || player.isSpectator()) return;
		if (formation.kind == Formation.Kind.CORE) {
			if (broken && level.getBlockEntity(formation.center) instanceof FormationCoreBlockEntity core) core.onBreach();
			if (formation.getSect() != null) SectManager.onBarrierAssailed(level, formation.getSect(), player, pos);
		} else if (formation.getOwner() != null && level.getEntity(formation.getOwner()) instanceof CultivatorNpc npc) {
			npc.onConcealmentAssailed(player);
		}
	}

	// --- Raising and lowering ---

	/** Creates (or returns) the formation with this id. */
	public static Formation create(ServerLevel level, UUID id, Formation.Kind kind, BlockPos center, int radius) {
		FormationData data = FormationData.get(level);
		Formation formation = data.get(id);
		if (formation == null) {
			formation = new Formation(id, kind, center, radius);
			data.put(formation);
		}
		return formation;
	}

	public static void raise(ServerLevel level, Formation formation) {
		if (formation.isRaised()) return;
		formation.setRaised(true);
		formation.heartbeat = level.getGameTime();
		changed(level);
	}

	/** Takes the barrier down: every block of it goes at once (unloaded parts as soon as they load). */
	public static void lower(ServerLevel level, Formation formation) {
		if (!formation.isRaised()) return;
		formation.setRaised(false);
		clearShell(level, formation, formation.getRadius());
		changed(level);
	}

	/** A new radius: the old shell comes down and the new one goes up (if raised). */
	public static void resize(ServerLevel level, Formation formation, int radius) {
		radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
		if (radius == formation.getRadius()) return;
		int old = formation.getRadius();
		formation.setRadius(radius); // first, so the old shell's cells are no longer on it
		if (formation.isRaised()) clearShell(level, formation, old);
		changed(level);
	}

	public static void update(ServerLevel level, Formation formation, int rank, UUID owner, UUID sect) {
		if (formation.getRank() == rank && java.util.Objects.equals(formation.getOwner(), owner) && java.util.Objects.equals(formation.getSect(), sect)) return;
		formation.setRank(rank);
		formation.setOwner(owner);
		formation.setSect(sect);
		changed(level);
	}

	/** The players the formation's owner trusts to pass as they do. */
	public static void setTrusted(ServerLevel level, Formation formation, java.util.Collection<UUID> players) {
		if (formation.setTrusted(players)) changed(level);
	}

	/** The formation is gone for good (its core broken, its cultivator gone): its barrier comes down and its record goes. */
	public static void dissolve(ServerLevel level, UUID id) {
		FormationData data = FormationData.get(level);
		Formation formation = data.get(id);
		if (formation == null) return;
		lower(level, formation);
		data.remove(id);
		changed(level);
	}

	private static void changed(ServerLevel level) {
		FormationData.get(level).setDirty();
		syncSoon = true;
	}

	/**
	 * What a shell cell holding {@code state} becomes, so the shell has no opening anywhere: the barrier, holding the cell's
	 * water or lava if it was a still source; or null to leave the cell as it is, because it is already a wall (a block whose
	 * collision fills its whole footprint at least half a block high: stone, logs, leaves, stairs, slabs), it can't be moved
	 * (bedrock, portals), it is a seal (kept, so arrays can cross the shell; SealBlock closes itself instead), or it belongs to
	 * the sect's own buildings. Anything else in the way (a torch, a fence, a door, a flower, sugar cane, flowing water) is
	 * displaced (see {@link #displace}).
	 */
	private static BlockState fillFor(ServerLevel level, BlockPos pos, BlockState state, Formation formation) {
		if (state.isAir()) return SectBarrierBlock.holding(SectBarrierBlock.Held.NONE);
		if (state.getBlock() instanceof SectBarrierBlock || state.getBlock() instanceof SealBlock) return null;
		if (state.getBlock() instanceof LiquidBlock) return SectBarrierBlock.holding(held(state.getFluidState()));
		if (state.canBeReplaced()) return SectBarrierBlock.holding(held(state.getFluidState()));
		if (state.getDestroySpeed(level, pos) < 0 || state.getBlock() instanceof NetherPortalBlock || state.getBlock() instanceof EndPortalBlock
				|| state.getBlock() instanceof EndGatewayBlock) return null;
		if (isWall(level, pos, state)) return null;
		if (formation.getSect() != null) {
			Sect sect = SectManager.get(level.getServer(), formation.getSect());
			if (sect != null && sect.bounds.isInside(pos)) return null; // a pagoda's spire through the dome: the sect's own, kept
		}
		return SectBarrierBlock.holding(held(state.getFluidState()));
	}

	/** The barrier keeps a still source of water or lava it closes over; flowing water or lava is simply shut out. */
	private static SectBarrierBlock.Held held(FluidState fluid) {
		if (fluid.isEmpty() || !fluid.isSource()) return SectBarrierBlock.Held.NONE;
		if (fluid.is(FluidTags.WATER)) return SectBarrierBlock.Held.WATER;
		if (fluid.is(FluidTags.LAVA)) return SectBarrierBlock.Held.LAVA;
		return SectBarrierBlock.Held.NONE;
	}

	/** Already closes its cell: its collision covers the whole footprint and stands at least half a block high. */
	private static boolean isWall(ServerLevel level, BlockPos pos, BlockState state) {
		VoxelShape shape = state.getCollisionShape(level, pos);
		if (shape.isEmpty()) return false;
		AABB bounds = shape.bounds();
		return bounds.minX <= 0.01 && bounds.maxX >= 0.99 && bounds.minZ <= 0.01 && bounds.maxZ >= 0.99 && bounds.getYsize() >= 0.5;
	}

	/**
	 * Clears the way for the barrier: what someone built or placed drops as an item (whole: a door, a bed, a chest and what
	 * was in it), while wild plants (flowers, saplings, sugar cane, kelp, snow and the like) simply give way, as grass does.
	 */
	private static void displace(ServerLevel level, BlockPos pos, BlockState state) {
		if (isWild(state)) return;
		// Two-block things drop from the half that holds their loot, so they come out whole, and only once.
		BlockPos at = pos;
		BlockState main = state;
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
			at = pos.below();
			main = level.getBlockState(at);
			if (!main.is(state.getBlock())) return;
		} else if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.FOOT) {
			at = pos.relative(BedBlock.getConnectedDirection(state));
			main = level.getBlockState(at);
			if (!main.is(state.getBlock())) return;
		}
		BlockEntity entity = level.getBlockEntity(at);
		if (entity == null) {
			ItemStack item = main.getBlock().getCloneItemStack(level, at, main);
			if (!item.isEmpty()) {
				Block.popResource(level, at, item);
				return;
			}
		}
		Block.dropResources(main, level, at, entity); // a container's contents spill out as it is replaced
	}

	/** Plants and snow that grow by themselves: the barrier takes their place without leaving them lying about. */
	private static boolean isWild(BlockState state) {
		return state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)
				|| state.is(BlockTags.CAVE_VINES) || state.is(BlockTags.CORALS) || state.is(BlockTags.SNOW) || state.is(BlockTags.CORAL_PLANTS)
				|| state.is(Blocks.SUGAR_CANE) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.BAMBOO)
				|| state.is(Blocks.BAMBOO_SAPLING) || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.BROWN_MUSHROOM)
				|| state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.CRIMSON_FUNGUS) || state.is(Blocks.WARPED_FUNGUS) || state.is(Blocks.POINTED_DRIPSTONE)
				|| state.is(Blocks.SMALL_DRIPLEAF) || state.is(Blocks.BIG_DRIPLEAF) || state.is(Blocks.BIG_DRIPLEAF_STEM) || state.is(Blocks.MOSS_CARPET)
				|| state.is(Blocks.SEA_PICKLE) || state.is(Blocks.LILY_PAD) || state.is(Blocks.COBWEB) || state.is(Blocks.SPORE_BLOSSOM)
				|| state.is(Blocks.AZALEA) || state.is(Blocks.FLOWERING_AZALEA) || state.is(Blocks.SMALL_AMETHYST_BUD) || state.is(Blocks.MEDIUM_AMETHYST_BUD)
				|| state.is(Blocks.LARGE_AMETHYST_BUD) || state.is(Blocks.AMETHYST_CLUSTER) || state.is(Blocks.TWISTING_VINES)
				|| state.is(Blocks.TWISTING_VINES_PLANT) || state.is(Blocks.WEEPING_VINES) || state.is(Blocks.WEEPING_VINES_PLANT)
				|| state.is(Blocks.PINK_PETALS) || state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER) || state.is(Blocks.FROGSPAWN);
	}

	/** A barrier cell comes down: back to air, or to the water or lava it stood in (which then flows on as it did). */
	private static void clearCell(ServerLevel level, BlockPos pos, BlockState barrier) {
		BlockState residue = SectBarrierBlock.residue(barrier);
		level.setBlock(pos, residue, residue.isAir() ? Block.UPDATE_CLIENTS : Block.UPDATE_ALL);
	}

	/** Clears this formation's barrier blocks on the shell of {@code radius}, remembering the chunks that aren't loaded. */
	private static void clearShell(ServerLevel level, Formation formation, int radius) {
		FormationData data = FormationData.get(level);
		BlockPos c = formation.center;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		LongSet unloaded = new LongOpenHashSet();
		for (int packed : Formation.shell(radius)) {
			pos.set(c.getX() + Formation.dx(packed), c.getY() + Formation.dy(packed), c.getZ() + Formation.dz(packed));
			if (level.isOutsideBuildHeight(pos)) continue;
			if (!level.isLoaded(pos)) {
				unloaded.add(ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())));
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlocks.SECT_BARRIER) && onShell(level, pos, formation) == null) clearCell(level, pos, state);
		}
		if (!unloaded.isEmpty()) {
			data.pendingClear.addAll(unloaded);
			data.setDirty();
		}
	}

	/** Fills (or keeps up) {@code budget} cells of a raised formation's shell. @return the cells handled */
	private static int build(ServerLevel level, Formation formation, int budget) {
		int[] shell = Formation.shell(formation.getRadius());
		if (shell.length == 0) return 0;
		int steps = Math.min(budget, formation.complete ? REPAIR_BUDGET : RAISE_BUDGET);
		BlockPos c = formation.center;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		FormationCoreBlockEntity core = null;
		boolean coreLooked = false;
		for (int i = 0; i < steps; i++) {
			if (formation.cursor >= shell.length) {
				formation.cursor = 0;
				formation.complete = true;
			}
			int packed = shell[formation.cursor++];
			pos.set(c.getX() + Formation.dx(packed), c.getY() + Formation.dy(packed), c.getZ() + Formation.dz(packed));
			if (level.isOutsideBuildHeight(pos) || !level.isLoaded(pos)) continue;
			BlockState state = level.getBlockState(pos);
			BlockState fill = fillFor(level, pos, state, formation);
			if (fill == null) continue;
			if (formation.complete && formation.kind == Formation.Kind.CORE) {
				// A gap after the first raising: a breach (or newly loaded ground). Mending it costs the core qi.
				if (!coreLooked) {
					core = level.getBlockEntity(c) instanceof FormationCoreBlockEntity found ? found : null;
					coreLooked = true;
				}
				if (core == null || !core.payRepair()) continue;
			}
			if (state.isAir()) {
				level.setBlock(pos, fill, Block.UPDATE_CLIENTS); // most of the shell: open air, nothing around to tell
			} else {
				if (!(state.getBlock() instanceof LiquidBlock) && !state.canBeReplaced()) displace(level, pos, state);
				// Neighbours hear of it: the other half of a door or a tall flower goes, and water around settles.
				level.setBlock(pos, fill, Block.UPDATE_ALL);
			}
		}
		return steps;
	}

	// --- Ticking ---

	public static void tick(MinecraftServer server) {
		for (int i = 0; i < 4 && !SWEEPS.isEmpty(); i++) {
			Sweep sweep = SWEEPS.poll();
			sweep(sweep.level(), sweep.chunk());
		}
		boolean heartbeat = server.getTickCount() % 100 == 0;
		boolean purging = server.getTickCount() % PURGE_INTERVAL == 0;
		for (ServerLevel level : server.getAllLevels()) {
			FormationData data = FormationData.get(level);
			if (data.all().isEmpty()) continue;
			int budget = TOTAL_BUDGET;
			List<UUID> lapsed = new ArrayList<>();
			for (Formation formation : data.all()) {
				if (!formation.isRaised() || !level.isLoaded(formation.center)) continue;
				if (budget > 0) budget -= build(level, formation, budget);
				if (purging) {
					purge(level, formation);
					guard(level, formation);
				}
				if (heartbeat && formation.kind == Formation.Kind.CONCEALMENT) {
					Entity owner = formation.getOwner() == null ? null : level.getEntity(formation.getOwner());
					if (owner instanceof CultivatorNpc npc && npc.isAlive()) {
						formation.heartbeat = level.getGameTime();
					} else if (level.getGameTime() - formation.heartbeat > CONCEALMENT_GRACE) {
						lapsed.add(formation.id);
					}
				}
			}
			for (UUID id : lapsed) dissolve(level, id);
		}
		if (syncSoon || server.getTickCount() % SYNC_INTERVAL == 0) {
			syncSoon = false;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
		}
		if (server.getTickCount() % 1200 == 0) {
			long now = server.overworld().getGameTime();
			ATTACK_COOLDOWN.values().removeIf(at -> now - at > 200);
		}
	}

	/** ServerChunkEvents.CHUNK_LOAD: a chunk that may hold a lowered formation's blocks is swept on the next ticks. */
	public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
		FormationData data = FormationData.get(level);
		long key = chunk.getPos().toLong();
		if (data.pendingClear.contains(key)) SWEEPS.add(new Sweep(level, key));
	}

	/** Removes every barrier block in the chunk that no raised formation's shell holds. */
	private static void sweep(ServerLevel level, long key) {
		FormationData data = FormationData.get(level);
		LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
		if (chunk == null) return; // unloaded again before its turn: still pending, swept on the next load
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		LevelChunkSection[] sections = chunk.getSections();
		for (int index = 0; index < sections.length; index++) {
			LevelChunkSection section = sections[index];
			if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(ModBlocks.SECT_BARRIER))) continue;
			int bottom = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState state = section.getBlockState(x, y, z);
						if (!state.is(ModBlocks.SECT_BARRIER)) continue;
						pos.set(chunk.getPos().getMinBlockX() + x, bottom + y, chunk.getPos().getMinBlockZ() + z);
						if (onShell(level, pos, null) == null) clearCell(level, pos, state);
					}
				}
			}
		}
		data.pendingClear.remove(key);
		data.setDirty();
	}

	/** SectBarrierBlock#randomTick: a barrier block on no raised formation's shell is an orphan. */
	public static boolean isOrphan(ServerLevel level, BlockPos pos) {
		return onShell(level, pos, null) == null;
	}

	/** Tells the player about the raised formations near them (for drawing them, and for walking through their own). */
	private static void sync(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		List<Formation> near = new ArrayList<>();
		for (Formation formation : FormationData.get(level).all()) {
			if (!formation.isRaised()) continue;
			double reach = SYNC_RANGE + formation.getRadius();
			if (formation.center.distToCenterSqr(player.position()) <= reach * reach) near.add(formation);
		}
		ModPackets.sendFormations(player, near);
	}

	public static void syncSoon() {
		syncSoon = true;
	}

	/** Server stopped: nothing runtime-only may leak into the next world. */
	public static void clear() {
		SWEEPS.clear();
		ATTACK_COOLDOWN.clear();
		syncSoon = false;
	}

	private Formations() {}
}
