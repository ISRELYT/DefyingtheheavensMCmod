package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The earth element's spells (Spell, HeavenSpells): earth is weak to wood and strong against water (SpellAuras). The ground they
 * move is the world's own: what rises is the block underfoot, what is torn up leaves its hole. Every number is the design's; every
 * look its own (the client's spell renderers).
 * <ul>
 *   <li>Qi Refining: Earth Spear (a podao of earth formed in the air and flung), Hardened Body (-7% harm, a thin skin of stone),
 *       Dirt Tide (a wave of dirt to ride).</li>
 *   <li>Foundation Building: Earth Wall Technique (a 6 by 4 wall rises ahead), Boulder Throw (a boulder torn from the ground ahead,
 *       leaving a half-round hole, and thrown), Cave Formation (a cave for one carved before your eyes).</li>
 *   <li>Core Formation: Earth Dragon Surge, Mountain Stance.</li>
 *   <li>Nascent Soul: Summon Golem (two at most), Earthquake.</li>
 *   <li>Heavenly Being: Falling Mountain, Heavy Earth Seal.</li>
 *   <li>Four Axis: Earthen Titan, Mountain Seal.</li>
 * </ul>
 */
public final class EarthSpells {
	public static final double SPEAR_SPEED = 1.8, SPEAR_RANGE = 40;
	public static final float SPEAR_DAMAGE = 6;
	public static final int HARDENED_TICKS = 30 * 20, TIDE_TICKS = 10 * 20;
	public static final int WALL_AHEAD = 3, WALL_LONG = 6, WALL_TALL = 4, WALL_RISE = 20, WALL_LASTS = 30 * 20;
	public static final int CRATER = 2;
	public static final double BOULDER_SPEED = 1.4, BOULDER_RANGE = 48;
	public static final float BOULDER_DAMAGE = 16;
	public static final double CAVE_REACH = 6;
	public static final int CAVE_TICKS = 60;
	public static final double SURGE_RANGE = 24, SURGE_PACE = 1.5;
	public static final float SURGE_DAMAGE = 20;
	public static final int STANCE_TICKS = 15 * 20;
	public static final int GOLEM_TICKS = 90 * 20, GOLEMS = 2;
	public static final double QUAKE_RADIUS = 20;
	public static final int QUAKE_TICKS = 4 * 20, QUAKE_FISSURES = 9, QUAKE_FISSURE_DEPTH = 5;
	public static final float QUAKE_DAMAGE = 14;
	public static final double MOUNTAIN_RANGE = 64, MOUNTAIN_RADIUS = 10, MOUNTAIN_CRATER = 12, MOUNTAIN_CRATER_DEPTH = 7;
	public static final float MOUNTAIN_DAMAGE = 140;
	public static final double SEAL_RADIUS = 24;
	public static final int SEAL_TICKS = 6 * 20, SEAL_SINK = 2;
	public static final float SEAL_DAMAGE = 20;
	public static final int TITAN_TICKS = 60 * 20;
	public static final double TOMB_RANGE = 48;
	public static final int TOMB_RADIUS = 7, TOMB_HEIGHT = 9, TOMB_TICKS = 5 * 20, TOMB_LASTS = 20 * 20;
	public static final float TOMB_DAMAGE = 60;

	/** Each caster's golems, oldest first. */
	private static final Map<UUID, List<EarthGolem>> GOLEMS_OF = new HashMap<>();
	private static final Map<UUID, EarthGolem> TITANS = new HashMap<>();

	static boolean canCast(ServerPlayer p, Spell spell) {
		return switch (spell) {
			case DIRT_TIDE -> HeavenSpells.groundBelow(p.serverLevel(), p.position(), 0, 3) != null || HeavenSpells.refuse(p);
			case BOULDER_THROW -> crater(p).size() >= 4 || noEarth(p);
			case CAVE_FORMATION -> HeavenSpells.lookedBlock(p, CAVE_REACH) != null || HeavenSpells.refuse(p);
			default -> true;
		};
	}

	private static boolean noEarth(ServerPlayer p) {
		p.displayClientMessage(net.minecraft.network.chat.Component.translatable(ModLang.MSG_NO_EARTH), true);
		return false;
	}

	static void cast(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		switch (spell) {
			case EARTH_SPEAR -> spear(p, spell);
			case HARDENED_BODY -> {
				SpellAuras.put(p, SpellAura.HARDENED_BODY, HARDENED_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.STONE_PLACE, 1.2f, 0.6f);
				HeavenSpells.sound(level, p.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.8f);
				SpellFx.send(level, SpellFx.Kind.DUST, p.position(), Vec3.ZERO, 1.0f, Block.getId(groundUnder(p)));
			}
			case DIRT_TIDE -> {
				if (p.getVehicle() instanceof DirtTide old) old.sink();
				DirtTide.raise(p, groundUnder(p), TIDE_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
			}
			case EARTH_WALL_TECHNIQUE -> HeavenSpells.linger(new Wall(p));
			case BOULDER_THROW -> boulder(p, spell);
			case CAVE_FORMATION -> cave(p);
			case EARTH_DRAGON_SURGE -> HeavenSpells.linger(new Surge(p, spell));
			case MOUNTAIN_STANCE -> {
				SpellAuras.put(p, SpellAura.MOUNTAIN_STANCE, STANCE_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.ANVIL_LAND, 1.0f, 0.5f);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, p.position(), Vec3.ZERO, 3, 0);
			}
			case SUMMON_GOLEM -> golem(p);
			case EARTHQUAKE -> HeavenSpells.linger(new Quake(p, spell));
			case FALLING_MOUNTAIN -> mountain(p, spell);
			case HEAVY_EARTH_SEAL -> HeavenSpells.linger(new Seal(p, spell));
			case EARTHEN_TITAN -> titan(p);
			case MOUNTAIN_SEAL -> HeavenSpells.linger(new Tomb(p, spell));
			default -> {}
		}
	}

	/** The block {@code p} stands on, as earth (grass as dirt), or dirt where there is none of it under them. */
	static BlockState groundUnder(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		for (int dy = 1; dy <= 16; dy++) {
			BlockPos pos = BlockPos.containing(p.getX(), p.getY() - dy + 0.5, p.getZ());
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty()) continue;
			if (state.isCollisionShapeFullBlock(level, pos) && !state.hasBlockEntity()) return HeavenSpells.asEarth(state);
			break;
		}
		return Blocks.DIRT.defaultBlockState();
	}

	// --- Qi Refining ---

	private static void spear(ServerPlayer p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.EARTH_PODAO, 1.0f, 10, new Vec3(0.2, 0.35, 1.5), SPEAR_SPEED, SPEAR_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						HeavenSpells.strike(p, target, SPEAR_DAMAGE, spell, missile);
						HeavenSpells.push(missile.position().subtract(missile.getDeltaMovement()), target, 1.0);
						SpellFx.send(p.serverLevel(), SpellFx.Kind.EARTH_BURST, missile.position(), Vec3.ZERO, 0.8f, missile.data());
						HeavenSpells.sound(p.serverLevel(), target.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.8f);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(p.serverLevel(), SpellFx.Kind.EARTH_BURST, hit.getLocation(), Vec3.ZERO, 0.8f, missile.data());
						HeavenSpells.sound(p.serverLevel(), hit.getLocation(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.9f);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(p.serverLevel(), missile.position(), SoundEvents.TRIDENT_THROW, 1.0f, 0.6f);
					}
				}).data(Block.getId(groundUnder(p)));
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.7f);
		SpellFx.send(p.serverLevel(), SpellFx.Kind.DUST, p.position(), Vec3.ZERO, 0.6f, Block.getId(groundUnder(p)));
	}

	// --- Foundation Building ---

	/**
	 * Earth Wall Technique: a wall {@link #WALL_LONG} long and {@link #WALL_TALL} tall of the ground's own block rises
	 * {@link #WALL_AHEAD} blocks ahead, across the way its caster faces, each column out of the ground beneath it (from the middle
	 * outward). Off the ground, it forms in mid air from its centre block outward. It stands {@link #WALL_LASTS}, then sinks back.
	 */
	public static final class Wall extends HeavenSpells.Timed {
		private final BlockState earth;
		/** Each of its blocks, with the tick it is set down. */
		private final Map<BlockPos, Integer> blocks = new LinkedHashMap<>();
		private final List<BlockPos> standing = new ArrayList<>();
		private final boolean grounded;

		Wall(ServerPlayer caster) {
			super(caster, WALL_LASTS + WALL_RISE + 10);
			this.earth = groundUnder(caster);
			Direction facing = caster.getDirection();
			Direction along = facing.getClockWise();
			BlockPos front = caster.blockPosition().relative(facing, WALL_AHEAD);
			this.grounded = caster.onGround() || HeavenSpells.groundBelow(level, caster.position(), 0, 2) != null;
			for (int i = -WALL_LONG / 2; i < WALL_LONG - WALL_LONG / 2; i++) {
				BlockPos column = front.relative(along, i);
				int delay = Math.abs(i) * 2;
				if (grounded) {
					// Each column out of the ground beneath it: the first open block above the ground there.
					BlockPos base = column;
					for (int dy = 2; dy >= -3; dy--) {
						BlockPos at = column.above(dy);
						if (!level.getBlockState(at).canBeReplaced() && level.getBlockState(at.above()).canBeReplaced()) {
							base = at.above();
							break;
						}
					}
					for (int h = 0; h < WALL_TALL; h++) {
						BlockPos pos = base.above(h);
						if (!level.getBlockState(pos).canBeReplaced()) continue;
						blocks.put(pos, delay + WALL_RISE);
						SpellBlock.show(level, earth, Vec3.atBottomCenterOf(pos).subtract(0, WALL_TALL, 0), new Vec3(0, WALL_TALL, 0), delay, WALL_RISE,
								SpellBlock.Motion.SLIDE, 2);
					}
				} else {
					// In mid air: from the centre block outward, each growing from nothing.
					BlockPos centre = BlockPos.containing(caster.getEyePosition()).relative(facing, WALL_AHEAD).below(1);
					for (int h = 0; h < WALL_TALL; h++) {
						BlockPos pos = centre.relative(along, i).above(h - 1);
						if (!level.getBlockState(pos).canBeReplaced()) continue;
						int d = (int) Math.round(Math.sqrt(i * i + (h - 1.5) * (h - 1.5)) * 3);
						blocks.put(pos, d + 6);
						SpellBlock.show(level, earth, Vec3.atBottomCenterOf(pos), Vec3.ZERO, d, 6, SpellBlock.Motion.GROW, 2);
					}
				}
			}
			HeavenSpells.sound(level, Vec3.atCenterOf(front), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.5f);
			HeavenSpells.sound(level, Vec3.atCenterOf(front), SoundEvents.STONE_BREAK, 1.2f, 0.4f);
			SpellFx.send(level, SpellFx.Kind.DUST, Vec3.atBottomCenterOf(front), Vec3.ZERO, 2.0f, Block.getId(earth));
		}

		@Override
		protected boolean run(long age, long now) {
			for (Map.Entry<BlockPos, Integer> entry : blocks.entrySet()) {
				if (entry.getValue() != age) continue;
				BlockPos pos = entry.getKey();
				if (HeavenSpells.placeTemporary(level, pos, earth, WALL_LASTS + 40)) standing.add(pos);
			}
			if (age == WALL_RISE / 2) HeavenSpells.sound(level, caster.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.7f);
			return age < WALL_LASTS + WALL_RISE;
		}

		/** It sinks back into the ground (or crumbles, in the air) and is gone. */
		@Override
		public void end() {
			for (BlockPos pos : standing) {
				if (!HeavenSpells.isTemporary(level, pos)) continue;
				HeavenSpells.removeTemporary(level, pos);
				if (grounded) SpellBlock.show(level, earth, Vec3.atBottomCenterOf(pos), new Vec3(0, -WALL_TALL, 0), 0, 16, SpellBlock.Motion.SLIDE, 0);
				else SpellBlock.show(level, earth, Vec3.atBottomCenterOf(pos), Vec3.ZERO, level.random.nextInt(6), 12, SpellBlock.Motion.CRUMBLE, 0);
			}
			if (!standing.isEmpty()) HeavenSpells.sound(level, Vec3.atCenterOf(standing.get(0)), SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.5f);
		}
	}

	/**
	 * Where Boulder Throw tears its boulder from: a half-round hole {@link #CRATER} round in the ground two blocks ahead, natural or
	 * built (whatever a spell may carve: HeavenSpells#carvable).
	 */
	static List<BlockPos> crater(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Vec3 ahead = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(CRATER + 1.5));
		Vec3 ground = HeavenSpells.groundBelow(level, ahead.add(0, 2, 0), 3, 4);
		if (ground == null) return List.of();
		BlockPos centre = BlockPos.containing(ground.x, ground.y - 0.5, ground.z);
		List<BlockPos> hole = new ArrayList<>();
		for (int dx = -CRATER; dx <= CRATER; dx++) {
			for (int dz = -CRATER; dz <= CRATER; dz++) {
				for (int dy = -CRATER; dy <= 0; dy++) {
					if (dx * dx + dy * dy + dz * dz > CRATER * CRATER + 1) continue;
					BlockPos pos = centre.offset(dx, dy, dz);
					if (HeavenSpells.carvable(level, pos, p)) hole.add(pos);
				}
			}
		}
		return hole;
	}

	private static void boulder(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		List<BlockPos> hole = crater(p);
		if (hole.size() < 4) return;
		// The boulder is of what the ground mostly was.
		Map<Block, Integer> counts = new HashMap<>();
		BlockState most = Blocks.DIRT.defaultBlockState();
		int best = 0;
		Vec3 centre = Vec3.ZERO;
		for (BlockPos pos : hole) centre = centre.add(Vec3.atCenterOf(pos));
		centre = centre.scale(1.0 / hole.size());
		Vec3 gather = new Vec3(centre.x, centre.y + CRATER + 2.5, centre.z);
		int i = 0;
		for (BlockPos pos : hole) {
			BlockState state = HeavenSpells.asEarth(level.getBlockState(pos));
			int n = counts.merge(state.getBlock(), 1, Integer::sum);
			if (n > best) {
				best = n;
				most = state;
			}
			level.removeBlock(pos, false);
			Vec3 from = Vec3.atBottomCenterOf(pos);
			SpellBlock.show(level, state, from, gather.subtract(from).subtract(0, 0.5, 0), (i++ % 6), 12, SpellBlock.Motion.FLY, 0).scale(0.7f);
		}
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, centre, Vec3.ZERO, 1.5f, Block.getId(most));
		HeavenSpells.sound(level, centre, SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.5f);
		HeavenSpells.sound(level, centre, SoundEvents.STONE_BREAK, 1.5f, 0.5f);
		int stateId = Block.getId(most);
		HeavenSpells.linger(new FireSpells.Delayed(p, 18, () -> {
			Vec3 to = HeavenSpells.aimed(p, BOULDER_RANGE) != null ? HeavenSpells.middle(HeavenSpells.aimed(p, BOULDER_RANGE)) : HeavenSpells.lookPoint(p, BOULDER_RANGE);
			Vec3 v = to.subtract(gather).normalize().scale(BOULDER_SPEED).add(0, 0.12, 0);
			SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.BOULDER, 1.0f, gather, v, BOULDER_RANGE, new SpellProjectile.Behaviour() {
				@Override
				public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
					HeavenSpells.strike(p, target, BOULDER_DAMAGE, spell, missile);
					HeavenSpells.push(missile.position().subtract(missile.getDeltaMovement()), target, 1.6);
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, missile.position(), Vec3.ZERO, 1.6f, stateId);
					HeavenSpells.sound(level, missile.position(), SoundEvents.STONE_BREAK, 1.5f, 0.5f);
					return false;
				}

				@Override
				public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, hit.getLocation(), Vec3.ZERO, 1.6f, stateId);
					HeavenSpells.sound(level, hit.getLocation(), SoundEvents.STONE_BREAK, 1.5f, 0.5f);
				}
			}).gravity(0.03).hitSize(1.2f).data(stateId);
			HeavenSpells.sound(level, gather, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.4f);
		}));
		// The boulder itself hangs at the gathering point while it forms (drawn as a field until it is thrown).
		SpellField.create(level, SpellField.Kind.BOULDER_FORMING, gather, 1.0f, 1.0f, 19, p).data(stateId).spawn();
	}

	private static void cave(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		BlockHitResult hit = HeavenSpells.lookedBlock(p, CAVE_REACH);
		if (hit == null) return;
		List<BlockPos> carve = new ArrayList<>();
		BlockPos face = hit.getBlockPos();
		Direction in = hit.getDirection().getOpposite();
		if (in.getAxis().isHorizontal()) {
			// Into a hillside: an entrance one wide and two tall, then a chamber three by three by three behind it.
			Direction side = in.getClockWise();
			BlockPos floor = face;
			carve.add(floor);
			carve.add(floor.above());
			BlockPos room = floor.relative(in, 2);
			for (int d = -1; d <= 1; d++) for (int s = -1; s <= 1; s++) for (int h = 0; h <= 2; h++) carve.add(room.relative(in, d).relative(side, s).above(h));
		} else if (in == Direction.DOWN) {
			// Into the ground: three steps down toward where the caster faces, then the chamber at their foot.
			Direction ahead = p.getDirection();
			Direction side = ahead.getClockWise();
			BlockPos step = face;
			for (int k = 0; k < 3; k++) {
				for (int h = 0; h <= 2; h++) carve.add(step.above(h));
				step = step.relative(ahead).below();
			}
			BlockPos room = step.relative(ahead);
			for (int d = -1; d <= 1; d++) for (int s = -1; s <= 1; s++) for (int h = 0; h <= 2; h++) carve.add(room.relative(ahead, d).relative(side, s).above(h));
		} else {
			// Up into a ceiling: a chamber above the face.
			BlockPos room = face.above(1);
			for (int d = -1; d <= 1; d++) for (int s = -1; s <= 1; s++) for (int h = 0; h <= 2; h++) carve.add(room.offset(d, h - 1, s));
		}
		List<BlockPos> real = new ArrayList<>();
		for (BlockPos pos : carve) if (!real.contains(pos) && HeavenSpells.carvable(level, pos, p)) real.add(pos);
		if (real.isEmpty()) {
			HeavenSpells.refuse(p);
			return;
		}
		HeavenSpells.linger(new Carving(p, real));
		HeavenSpells.sound(level, Vec3.atCenterOf(face), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
	}

	/** Cave Formation's carving: its blocks crumble away one after another, front to back, over {@link #CAVE_TICKS}. */
	public static final class Carving extends HeavenSpells.Timed {
		private final List<BlockPos> blocks;

		Carving(ServerPlayer caster, List<BlockPos> blocks) {
			super(caster, CAVE_TICKS + 2);
			this.blocks = blocks;
		}

		@Override
		protected boolean run(long age, long now) {
			int from = (int) ((age - 1) * blocks.size() / (double) CAVE_TICKS), to = (int) (age * blocks.size() / (double) CAVE_TICKS);
			for (int i = Math.max(0, from); i < Math.min(blocks.size(), to); i++) {
				BlockPos pos = blocks.get(i);
				BlockState state = level.getBlockState(pos);
				if (state.isAir() || !HeavenSpells.carvable(level, pos, caster)) continue;
				level.removeBlock(pos, false);
				SpellBlock.show(level, state, Vec3.atBottomCenterOf(pos), Vec3.ZERO, 0, 14, SpellBlock.Motion.CRUMBLE, 0);
				if (i % 3 == 0) HeavenSpells.sound(level, Vec3.atCenterOf(pos), state.getSoundType().getBreakSound(), 0.8f, 0.7f);
			}
			return age < CAVE_TICKS;
		}
	}

	// --- Core Formation ---

	/**
	 * Earth Dragon Surge: a ridge of jagged spikes runs along the ground from its caster toward where they look, {@link #SURGE_PACE}
	 * a tick for up to {@link #SURGE_RANGE}; whatever its front passes over takes {@link #SURGE_DAMAGE} and is thrown up. The spikes
	 * sink back a few seconds later (SpellField EARTH_SPIKES: drawn along the ground the client sees).
	 */
	public static final class Surge extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, way;
		private final double length;
		private final SpellField field;
		private final java.util.Set<LivingEntity> struck = new java.util.HashSet<>();

		Surge(ServerPlayer caster, Spell spell) {
			super(caster, (int) (SURGE_RANGE / SURGE_PACE) + 70);
			this.spell = spell;
			this.from = caster.position();
			Vec3 look = caster.getViewVector(1.0f).multiply(1, 0, 1);
			this.way = look.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, caster.getYRot()) : look.normalize();
			this.length = SURGE_RANGE;
			this.field = SpellField.create(level, SpellField.Kind.EARTH_SPIKES, from, (float) SURGE_RANGE, 3, (int) (end - start), caster)
					.facing(caster.getYRot(), 0).data(Block.getId(groundUnder(caster))).value(0).spawn();
			HeavenSpells.sound(level, from, SoundEvents.RAVAGER_ROAR, 1.2f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			double reach = Math.min(length, age * SURGE_PACE);
			field.value((float) (reach / length));
			if (reach < length) {
				Vec3 front = from.add(way.scale(reach));
				for (LivingEntity e : HeavenSpells.aroundPoint(level, front, 2.5, e -> !HeavenSpells.spared(caster, e) && !struck.contains(e))) {
					if (Math.abs(e.getY() - front.y) > 4) continue;
					struck.add(e);
					HeavenSpells.strike(caster, e, SURGE_DAMAGE, spell);
					HeavenSpells.move(e, new Vec3(way.x * 0.4, 1.0, way.z * 0.4));
				}
				if (age % 3 == 0) HeavenSpells.sound(level, front, SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.6f + level.random.nextFloat() * 0.3f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Nascent Soul ---

	private static void golem(ServerPlayer p) {
		List<EarthGolem> mine = GOLEMS_OF.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
		mine.removeIf(g -> !g.isAlive() || g.isRemoved());
		while (mine.size() >= GOLEMS) mine.remove(0).crumble();
		Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot() + (mine.isEmpty() ? 35 : -35)).scale(2.5));
		Vec3 ground = HeavenSpells.groundBelow(p.serverLevel(), at.add(0, 2, 0), 3, 8);
		EarthGolem golem = EarthGolem.raise(p, ground != null ? ground : at, false, GOLEM_TICKS);
		mine.add(golem);
		SpellFx.send(p.serverLevel(), SpellFx.Kind.EARTH_BURST, golem.position().add(0, 0.5, 0), Vec3.ZERO, 2.0f, Block.getId(groundUnder(p)));
		HeavenSpells.sound(p.serverLevel(), golem.position(), SoundEvents.IRON_GOLEM_REPAIR, 1.5f, 0.5f);
		HeavenSpells.sound(p.serverLevel(), golem.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.5f);
	}

	/** {@code p}'s golems now (tests). */
	public static List<EarthGolem> golems(ServerPlayer p) {
		List<EarthGolem> mine = GOLEMS_OF.getOrDefault(p.getUUID(), List.of());
		return mine.stream().filter(g -> g.isAlive() && !g.isRemoved()).toList();
	}

	/** Earthquake: the ground within {@link #QUAKE_RADIUS} of its caster shakes; those on it are hurt, slowed and tossed. */
	public static final class Quake extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellField field;

		Quake(ServerPlayer caster, Spell spell) {
			super(caster, QUAKE_TICKS);
			this.spell = spell;
			this.field = SpellField.create(level, SpellField.Kind.EARTHQUAKE, caster.position(), (float) QUAKE_RADIUS, 2, QUAKE_TICKS, caster)
					.data(Block.getId(groundUnder(caster))).spawn();
			// The ground splits: fissures running out from a little way off the caster to the quake's edge.
			Devastation.fissures(caster, level, caster.position(), QUAKE_RADIUS, QUAKE_FISSURES, QUAKE_FISSURE_DEPTH, 3);
			HeavenSpells.sound(level, caster.position(), SoundEvents.WARDEN_EMERGE, 2.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 10 == 5) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, field.position(), QUAKE_RADIUS, e -> !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e))) {
					if (!e.onGround()) continue;
					HeavenSpells.strikeAgain(caster, e, QUAKE_DAMAGE / 2, spell);
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
					if (age % 20 == 5) HeavenSpells.move(e, e.getDeltaMovement().add((level.random.nextDouble() - 0.5) * 0.4, 0.45, (level.random.nextDouble() - 0.5) * 0.4));
				}
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, field.position(), Vec3.ZERO, (float) QUAKE_RADIUS, 0);
				HeavenSpells.sound(level, field.position(), SoundEvents.GENERIC_EXPLODE, 1.0f, 0.4f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Heavenly Being ---

	private static void mountain(ServerPlayer p, Spell spell) {
		ServerLevel level = p.serverLevel();
		LivingEntity aimed = HeavenSpells.aimed(p, MOUNTAIN_RANGE);
		Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, MOUNTAIN_RANGE);
		Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
		Vec3 target = ground != null ? ground : point;
		SpellField mark = SpellField.create(level, SpellField.Kind.METEOR_MARK, target, (float) MOUNTAIN_RADIUS, 1, 50, p).data(1).spawn();
		Vec3 from = target.add(0, 42, 0);
		int stateId = Block.getId(groundUnder(p));
		HeavenSpells.sound(level, from, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.3f);
		HeavenSpells.linger(new FireSpells.Delayed(p, 10, () -> SpellProjectile.thrown(level, p, spell, SpellProjectile.Shape.MOUNTAIN, 1.0f, from,
				new Vec3(0, -1.05, 0), 48, new SpellProjectile.Behaviour() {
					private void land(Vec3 where) {
						mark.discard();
						slam(p, spell, level, where, MOUNTAIN_RADIUS, MOUNTAIN_DAMAGE, stateId);
						Devastation.crater(p, level, where, MOUNTAIN_CRATER, MOUNTAIN_CRATER_DEPTH, Devastation.Floor.RUBBLE);
						Devastation.fissures(p, level, where, MOUNTAIN_CRATER * 2.2, 7, 4, MOUNTAIN_CRATER);
					}

					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity hit) {
						land(missile.position());
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						land(hit.getLocation());
					}

					@Override
					public void expire(SpellProjectile missile) {
						land(missile.position());
					}
				}).hitSize(6).data(stateId)));
	}

	/** A great weight of earth lands at {@code at}: all not spared within {@code radius} struck and thrown, a shockwave, a roar. */
	static void slam(ServerPlayer p, Spell spell, ServerLevel level, Vec3 at, double radius, float damage, int stateId) {
		for (LivingEntity e : HeavenSpells.aroundPoint(level, at, radius + 1, e -> !HeavenSpells.spared(p, e))) {
			double share = 1 - 0.4 * Math.min(1, e.position().distanceTo(at) / (radius + 1));
			HeavenSpells.strike(p, e, (float) (damage * share), spell);
			HeavenSpells.push(at, e, 2.0);
			HeavenSpells.move(e, e.getDeltaMovement().add(0, 0.6, 0));
		}
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, (float) radius, stateId, 192);
		SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) radius * 2, 0, 192);
		HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.4f);
		HeavenSpells.sound(level, at, SoundEvents.ANVIL_LAND, 3.0f, 0.3f);
	}

	/**
	 * Heavy Earth Seal: every foe within {@link #SEAL_RADIUS} of its caster is pressed to the ground for {@link #SEAL_TICKS}: no
	 * jump, no flight (a flier is brought down), slowed hard, {@link #SEAL_DAMAGE} of earth a second.
	 */
	public static final class Seal extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellField field;
		private final java.util.Set<LivingEntity> pressed = new java.util.HashSet<>();

		Seal(ServerPlayer caster, Spell spell) {
			super(caster, SEAL_TICKS);
			this.spell = spell;
			this.field = SpellField.create(level, SpellField.Kind.HEAVY_SEAL, caster.position(), (float) SEAL_RADIUS, 2, SEAL_TICKS, caster).spawn();
			// Under its weight the land sinks (all but where the caster stands), cracked, split here and there.
			Devastation.subside(caster, level, caster.position(), SEAL_RADIUS, SEAL_SINK, 3);
			Devastation.fissures(caster, level, caster.position(), SEAL_RADIUS, 10, 3, 4);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ANVIL_LAND, 2.0f, 0.3f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_DEACTIVATE, 2.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 5 == 0) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, field.position(), SEAL_RADIUS, e -> !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e))) {
					if (pressed.add(e)) SpellAuras.put(e, SpellAura.PRESSED, (int) (end - level.getGameTime()));
				}
			}
			for (LivingEntity e : pressed) {
				if (!HeavenSpells.present(e, level)) continue;
				e.removeEffect(MobEffects.LEVITATION);
				if (e instanceof ServerPlayer player && player.getAbilities().flying && !player.isCreative()) {
					player.getAbilities().flying = false;
					player.onUpdateAbilities();
				}
				if (!e.onGround()) HeavenSpells.move(e, new Vec3(e.getDeltaMovement().x * 0.5, Math.min(e.getDeltaMovement().y, -0.6), e.getDeltaMovement().z * 0.5));
				if (age % 10 == 9) {
					HeavenSpells.strikeAgain(caster, e, SEAL_DAMAGE / 2, spell);
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
				}
			}
			return true;
		}

		@Override
		public void end() {
			for (LivingEntity e : pressed) SpellAuras.remove(e, SpellAura.PRESSED);
			field.discard();
		}
	}

	// --- Four Axis ---

	private static void titan(ServerPlayer p) {
		EarthGolem old = TITANS.remove(p.getUUID());
		if (old != null && old.isAlive()) old.crumble();
		Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(6));
		Vec3 ground = HeavenSpells.groundBelow(p.serverLevel(), at.add(0, 3, 0), 4, 12);
		EarthGolem titan = EarthGolem.raise(p, ground != null ? ground : at, true, TITAN_TICKS);
		TITANS.put(p.getUUID(), titan);
		SpellFx.send(p.serverLevel(), SpellFx.Kind.EARTH_BURST, titan.position().add(0, 1, 0), Vec3.ZERO, 5.0f, Block.getId(groundUnder(p)), 192);
		SpellFx.send(p.serverLevel(), SpellFx.Kind.SHOCKWAVE, titan.position(), Vec3.ZERO, 12, 0, 192);
		HeavenSpells.sound(p.serverLevel(), titan.position(), SoundEvents.WARDEN_EMERGE, 3.0f, 0.4f);
		HeavenSpells.sound(p.serverLevel(), titan.position(), SoundEvents.IRON_GOLEM_REPAIR, 3.0f, 0.3f);
	}

	/**
	 * Mountain Seal: a mountain of stone erupts over the point looked at ({@link #TOMB_RADIUS} round, {@link #TOMB_HEIGHT} tall):
	 * everything not spared inside is entombed for {@link #TOMB_TICKS}, held, crushed ({@link #TOMB_DAMAGE} of earth a second). The
	 * mountain is the spell's (it gives nothing back when dug) and crumbles after {@link #TOMB_LASTS}.
	 */
	public static final class Tomb extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final List<LivingEntity> entombed = new ArrayList<>();
		private final List<BlockPos> blocks = new ArrayList<>();
		private final SpellField field;
		private final BlockState stone;
		private boolean risen;

		Tomb(ServerPlayer caster, Spell spell) {
			super(caster, TOMB_LASTS);
			this.spell = spell;
			LivingEntity aimed = HeavenSpells.aimed(caster, TOMB_RANGE);
			Vec3 point = aimed != null ? aimed.position() : HeavenSpells.lookPoint(caster, TOMB_RANGE);
			Vec3 ground = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.at = ground != null ? ground : point;
			BlockState under = level.getBlockState(BlockPos.containing(at).below());
			this.stone = under.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || under.is(net.minecraft.tags.BlockTags.BASE_STONE_NETHER) ? under : Blocks.STONE.defaultBlockState();
			this.field = SpellField.create(level, SpellField.Kind.MOUNTAIN_SEAL, at, TOMB_RADIUS, TOMB_HEIGHT, 14, caster).data(Block.getId(stone)).spawn();
			HeavenSpells.sound(level, at, SoundEvents.WARDEN_EMERGE, 3.0f, 0.3f);
			HeavenSpells.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == 12 && !risen) rise();
			if (risen && age < 12 + TOMB_TICKS) {
				for (LivingEntity e : entombed) {
					if (!HeavenSpells.present(e, level)) continue;
					HeavenSpells.still(e);
					if (age % 10 == 1) HeavenSpells.strikeAgain(caster, e, TOMB_DAMAGE / 2, spell);
				}
			}
			return true;
		}

		/** The mountain stands: its stone set round whoever is inside, who is held there. */
		private void rise() {
			risen = true;
			field.discard();
			BlockPos centre = BlockPos.containing(at);
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, TOMB_RADIUS, e -> !HeavenSpells.spared(caster, e) && e.getY() < at.y + TOMB_HEIGHT)) {
				entombed.add(e);
				HeavenSpells.linger(new WoodSpells.Bound(caster, e, e.position(), TOMB_TICKS));
			}
			for (int dx = -TOMB_RADIUS; dx <= TOMB_RADIUS; dx++) {
				for (int dz = -TOMB_RADIUS; dz <= TOMB_RADIUS; dz++) {
					double r = Math.sqrt(dx * dx + dz * dz) / TOMB_RADIUS;
					if (r > 1) continue;
					int top = (int) Math.round(TOMB_HEIGHT * (1 - r * r) + level.random.nextDouble() * 1.2);
					for (int dy = 0; dy < top; dy++) {
						BlockPos pos = centre.offset(dx, dy, dz);
						if (HeavenSpells.placeTemporary(level, pos, stone, TOMB_LASTS + 40)) blocks.add(pos);
					}
				}
			}
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at.add(0, TOMB_HEIGHT / 2.0, 0), Vec3.ZERO, TOMB_RADIUS, Block.getId(stone), 192);
			// The land torn open round the mountain's foot where it burst up.
			Devastation.fissures(caster, level, at, TOMB_RADIUS * 2.6, 9, 5, TOMB_RADIUS + 1);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, TOMB_RADIUS * 3, 0, 192);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.4f);
		}

		/** The mountain crumbles away: its stone falls apart from the top down. */
		@Override
		public void end() {
			field.discard();
			for (BlockPos pos : blocks) {
				if (!HeavenSpells.isTemporary(level, pos)) continue;
				HeavenSpells.removeTemporary(level, pos);
				if (level.random.nextInt(4) == 0) {
					SpellBlock.show(level, stone, Vec3.atBottomCenterOf(pos), Vec3.ZERO, (TOMB_HEIGHT - (pos.getY() - BlockPos.containing(at).getY())) * 2, 12,
							SpellBlock.Motion.CRUMBLE, 0);
				}
			}
			if (!blocks.isEmpty()) {
				SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at.add(0, TOMB_HEIGHT / 2.0, 0), Vec3.ZERO, TOMB_RADIUS, Block.getId(stone), 192);
				HeavenSpells.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
			}
		}
	}

	static void tick(MinecraftServer server) {}

	static void forget(UUID id) {
		List<EarthGolem> mine = GOLEMS_OF.remove(id);
		if (mine != null) for (EarthGolem golem : mine) if (golem.isAlive()) golem.crumble();
		EarthGolem titan = TITANS.remove(id);
		if (titan != null && titan.isAlive()) titan.crumble();
	}

	static void clear() {
		GOLEMS_OF.clear();
		TITANS.clear();
	}

	private EarthSpells() {}
}
