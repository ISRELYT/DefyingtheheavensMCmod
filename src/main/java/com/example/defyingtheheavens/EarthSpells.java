package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The earth methods' twelve spells (see HeavenSpells for what they share): a millstone that rolls over what it meets, a rampart
 * of rammed earth, scales of stone that break blow by blow, sands that swallow, a fissure that erupts, the weight of Mount Tai, an
 * earth dragon that hunts from below, the guardian of the earth vein, the bedrock that bears the blows, the burdens of allies
 * shouldered, a river of sand to ride, and the fall of Mount Buzhou.
 */
public final class EarthSpells {
	static final int OCHRE = 0xD99A2B, BROWN = 0x8C5A1E, SAND = 0xE3C77A, DARK_EARTH = 0x5A3A1A;

	/** Rolling Millstone: speed (blocks a tick), how far, how long at most, harm, the shove. */
	static final double MILL_SPEED = 0.55, MILL_REACH = 16, MILL_SHOVE = 0.9;
	static final int MILL_TICKS = 40;
	static final float MILL_HARM = 4;
	/** Rammed Earth Rampart: how far ahead, how wide (each side of the middle), how high, how long it stands. */
	static final int RAMPART_AHEAD = 3, RAMPART_HALF = 2, RAMPART_ROWS = 3, RAMPART_TICKS = 160;
	/** Stone Scale Armour: the scales, how long, the harm each stops. */
	static final int SCALES = 3, SCALE_TICKS = 400;
	static final float SCALE_STOPS = 10;
	/** Swallowing Sands: reach, size, how long, the deepest, the harm per depth each second. */
	static final double SANDS_RANGE = 24, SANDS_RADIUS = 4.5;
	static final int SANDS_TICKS = 120, SANDS_DEEPEST = 5;
	static final float SANDS_HARM = 0.5f;
	/** Earth-Splitting Fissure: how long, the wait before it erupts, harm, the throw. */
	static final double FISSURE_REACH = 20, FISSURE_THROW = 1.0;
	static final int FISSURE_WAIT = 20;
	static final float FISSURE_HARM = 22;
	/** Mount Tai Presses Down: reach, how long, the crush, the most it adds for the fall. */
	static final double TAI_RANGE = 32;
	static final int TAI_TICKS = 120;
	static final float TAI_CRUSH = 18, TAI_FALL_MOST = 12;
	/** Earth Dragon Turns Over: reach looked, reach sought, how long, speed, harm, the reeling. */
	static final double DRAGON_RANGE = 32, DRAGON_SEEK = 24, DRAGON_SPEED = 0.4;
	static final int DRAGON_TICKS = 160, DRAGON_REEL_TICKS = 60;
	static final float DRAGON_HARM = 40;
	/** Earth Vein Guardian: reach, how long, how often it bites, its reach, harm, the drag. */
	static final double GUARDIAN_RANGE = 24, GUARDIAN_BITE_REACH = 10, GUARDIAN_DRAG = 0.75;
	static final int GUARDIAN_TICKS = 240, GUARDIAN_EVERY = 40;
	static final float GUARDIAN_HARM = 24;
	/** Bedrock Bears the Blows: how long, the share that sinks away, the slices the rest comes in (each how far apart). */
	static final int BEDROCK_TICKS = 160, BEDROCK_SLICES = 4, BEDROCK_SLICE_EVERY = 20;
	static final float BEDROCK_KEEPS = 0.6f;
	/** Shoulder the Myriad Burdens: how long, reach, the share an ally keeps, the share that reaches the caster, the break. */
	static final int BURDEN_TICKS = 300;
	static final double BURDEN_RADIUS = 16;
	static final float BURDEN_KEEPS = 0.5f, BURDEN_PASSES = 0.25f, BURDEN_BREAK = 0.25f;
	/** Riding Ten Thousand Dunes: who rides along, speed, how long, the trample (reach, harm, the half-burying). */
	static final double DUNES_RIDERS = 5, DUNES_SPEED = 1.25, DUNES_TRAMPLE = 1.6;
	static final int DUNES_TICKS = 100, DUNES_BURY_TICKS = 40, DUNES_SAFE_TICKS = 40;
	static final float DUNES_HARM = 50;
	/** Fall of Buzhou: reach, the fall, the shadow, the quake, the strike (share of the greatest health, least, most), the burying. */
	static final double BUZHOU_RANGE = 64, BUZHOU_SHADOW = 14, BUZHOU_QUAKE = 22;
	static final int BUZHOU_FALL = 60, BUZHOU_SETTLE = 40, BUZHOU_BURY_TICKS = 100;
	static final float BUZHOU_SHARE = 0.2f, BUZHOU_LEAST = 50, BUZHOU_MOST = 130;

	/** Whether {@code spell} can take hold for {@code p} now; refused, nothing is paid. */
	static boolean canCast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case ROLLING_MILLSTONE, EARTH_SPLITTING_FISSURE -> {
				return p.onGround() || HeavenSpells.groundBelow(p.serverLevel(), ahead(p, 1.2), 1, 3) != null || HeavenSpells.refuse(p);
			}
			case SWALLOWING_SANDS -> {
				return HeavenSpells.lookedBlock(p, SANDS_RANGE) != null || HeavenSpells.refuse(p);
			}
			case MOUNT_TAI_PRESSES_DOWN -> {
				return HeavenSpells.aimed(p, TAI_RANGE) != null || HeavenSpells.refuse(p);
			}
			case EARTH_DRAGON_TURNS_OVER -> {
				return dragonQuarry(p) != null || HeavenSpells.refuse(p);
			}
			case EARTH_VEIN_GUARDIAN -> {
				return HeavenSpells.lookedBlock(p, GUARDIAN_RANGE) != null || HeavenSpells.refuse(p);
			}
			case FALL_OF_BUZHOU -> {
				return buzhouPoint(p) != null || HeavenSpells.refuse(p);
			}
			default -> {
				return true;
			}
		}
	}

	/** Casts one of the earth spells (paid for). */
	static void cast(ServerPlayer p, Spell spell) {
		switch (spell) {
			case ROLLING_MILLSTONE -> millstone(p);
			case RAMMED_EARTH_RAMPART -> rampart(p);
			case STONE_SCALE_ARMOUR -> scales(p);
			case SWALLOWING_SANDS -> sands(p);
			case EARTH_SPLITTING_FISSURE -> fissure(p);
			case MOUNT_TAI_PRESSES_DOWN -> tai(p);
			case EARTH_DRAGON_TURNS_OVER -> dragon(p);
			case EARTH_VEIN_GUARDIAN -> guardian(p);
			case BEDROCK_BEARS_THE_BLOWS -> bedrock(p);
			case SHOULDER_THE_MYRIAD_BURDENS -> burdens(p);
			case RIDING_TEN_THOUSAND_DUNES -> dunes(p);
			case FALL_OF_BUZHOU -> buzhou(p);
			default -> { }
		}
	}

	/** Every server tick (what lingers is ticked by HeavenSpells). */
	static void tick(MinecraftServer server) {
	}

	/** A player left. */
	static void forget(UUID id) {
	}

	/** The server stopping. */
	static void clear() {
		PASSING.clear();
	}

	// --- Shared ---

	/** The caster's facing along the ground (its look, pitch set aside), one long. */
	private static Vec3 facing(ServerPlayer p) {
		Vec3 look = p.getViewVector(1.0f).multiply(1, 0, 1);
		return look.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, p.getYRot()) : look.normalize();
	}

	/** {@code d} blocks ahead of the caster's feet, along the ground. */
	private static Vec3 ahead(ServerPlayer p, double d) {
		return p.position().add(facing(p).scale(d));
	}

	private static ParticleOptions dust(Block block) {
		return new BlockParticleOption(ParticleTypes.FALLING_DUST, block.defaultBlockState());
	}

	/** The block under a point (for its crumbs), or dirt. */
	private static Block groundBlock(ServerLevel level, Vec3 at) {
		BlockState state = level.getBlockState(BlockPos.containing(at.x, at.y - 0.1, at.z));
		return state.isAir() ? Blocks.DIRT : state.getBlock();
	}

	/** Whether a block with a collision shape stands at {@code pos}. */
	private static boolean solid(ServerLevel level, BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/**
	 * The next point of something rolling or splitting along the ground from {@code at} by {@code step} along {@code d}: up one
	 * step, or down as far as {@code drop}; null where a wall two blocks high stops it, or there is no ground within the drop.
	 */
	private static Vec3 alongGround(ServerLevel level, Vec3 at, Vec3 d, double step, int drop) {
		Vec3 next = at.add(d.scale(step));
		if (solid(level, BlockPos.containing(next.x, at.y + 1.05, next.z))) return null; // a wall two high
		Vec3 ground = HeavenSpells.groundBelow(level, new Vec3(next.x, at.y + 0.95, next.z), 0, drop + 1);
		if (ground == null || ground.y < at.y - drop - 0.01) return null;
		return ground;
	}

	/** Whether {@code e} is an enemy of the caster's (a foe, or a player at odds with it). */
	private static boolean enemy(ServerPlayer p, LivingEntity e) {
		return HeavenSpells.enemy(p, e);
	}

	// --- Rolling Millstone ---

	/** Rolling Millstone: a millstone of packed earth set rolling along the ground, running over and shoving whatever it meets. */
	private static void millstone(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Vec3 start = HeavenSpells.groundBelow(level, ahead(p, 1.2), 1, 3);
		if (start == null) start = p.position();
		HeavenSpells.linger(new Millstone(p, start, facing(p)));
		HeavenSpells.sound(level, start, SoundEvents.GRINDSTONE_USE, 1.0f, 0.6f);
	}

	/** The millstone rolling: where its foot is, how far it has come, whom it has run over. */
	static final class Millstone extends HeavenSpells.Timed {
		final Vec3 d;
		final Set<LivingEntity> struck = new HashSet<>();
		Vec3 at;
		double rolled;

		Millstone(ServerPlayer caster, Vec3 start, Vec3 d) {
			super(caster, MILL_TICKS);
			this.at = start;
			this.d = d;
		}

		@Override
		protected boolean run(long age, long now) {
			if (caster.level() != level || rolled >= MILL_REACH) return false;
			Vec3 next = alongGround(level, at, d, MILL_SPEED, 2);
			if (next == null || !level.isLoaded(BlockPos.containing(next))) {
				crumble();
				return false;
			}
			at = next;
			rolled += MILL_SPEED;
			Vec3 hub = at.add(0, 0.6, 0);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(hub, hub).inflate(0.6), e -> !HeavenSpells.spared(caster, e))) {
				if (!struck.add(e)) continue;
				HeavenSpells.strike(caster, e, MILL_HARM, QiElement.EARTH);
				HeavenSpells.move(e, e.getDeltaMovement().add(d.scale(MILL_SHOVE)).add(0, 0.2, 0));
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.PACKED_MUD), HeavenSpells.middle(e), 12, 0.3, 0.05);
				HeavenSpells.sound(level, e.position(), SoundEvents.STONE_HIT, 1.0f, 0.6f);
				HeavenSpells.sound(level, e.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
			}
			// The wheel: a ring turning in the plane of its travel.
			Vec3 side = new Vec3(-d.z, 0, d.x);
			for (int i = 0; i < 10; i++) {
				double a = Math.PI * 2 * i / 10 + rolled / 0.6;
				Vec3 rim = hub.add(d.scale(Math.cos(a) * 0.6)).add(0, Math.sin(a) * 0.6, 0);
				level.sendParticles(HeavenSpells.block(Blocks.PACKED_MUD), rim.x, rim.y, rim.z, 1, 0, 0, 0, 0);
			}
			level.sendParticles(HeavenSpells.dust(OCHRE, 1.2f), hub.x + side.x * 0.1, hub.y, hub.z + side.z * 0.1, 2, 0.05, 0.05, 0.05, 0);
			Vec3 behind = at.subtract(d.scale(0.6));
			level.sendParticles(dust(Blocks.SAND), behind.x, behind.y + 0.1, behind.z, 3, 0.2, 0.05, 0.2, 0);
			if (age % 6 == 0) HeavenSpells.sound(level, at, SoundEvents.GRAVEL_STEP, 0.8f, 0.7f);
			return true;
		}

		/** Where it rolls now (tests). */
		Vec3 at() {
			return at;
		}

		private void crumble() {
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.PACKED_MUD), at.add(0, 0.6, 0), 20, 0.5, 0.05);
			HeavenSpells.particles(level, ParticleTypes.POOF, at.add(0, 0.6, 0), 6, 0.4, 0.02);
			HeavenSpells.sound(level, at, SoundEvents.PACKED_MUD_BREAK, 1.0f, 0.6f);
		}

		@Override
		public void end() {
			if (rolled >= MILL_REACH || level.getGameTime() > end) crumble();
		}
	}

	// --- Rammed Earth Rampart ---

	/** Rammed Earth Rampart: a rampart of rammed earth raised across the caster's way, row by row, crumbling after a while. */
	private static void rampart(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Direction facing = p.getDirection();
		Direction across = facing.getClockWise();
		BlockPos middle = p.blockPosition().relative(facing, RAMPART_AHEAD);
		List<BlockPos> bases = new ArrayList<>();
		for (int i = -RAMPART_HALF; i <= RAMPART_HALF; i++) {
			BlockPos column = middle.relative(across, i);
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = column.atY(p.blockPosition().getY() + dy);
				if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
					bases.add(pos);
					break;
				}
			}
		}
		Vec3 push = Vec3.atLowerCornerOf(facing.getNormal());
		for (BlockPos base : bases) {
			AABB cells = new AABB(base).expandTowards(0, RAMPART_ROWS - 1, 0).inflate(0.3);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, cells, e -> !HeavenSpells.spared(p, e))) HeavenSpells.move(e, push.add(0, 0.25, 0));
			level.sendParticles(HeavenSpells.dust(OCHRE, 1.0f), base.getX() + 0.5, base.getY() + 0.05, base.getZ() + 0.5, 3, 0.3, 0, 0.3, 0);
		}
		HeavenSpells.linger(new Rampart(p, bases));
		HeavenSpells.sound(level, p.position(), SoundEvents.GRAVEL_BREAK, 1.0f, 0.5f);
	}

	/** The rampart: its rows rising one a tick, standing, then crumbling (its blocks the spells' own, taken up all together). */
	static final class Rampart extends HeavenSpells.Timed {
		final List<BlockPos> bases;
		final List<BlockPos> raised = new ArrayList<>();

		Rampart(ServerPlayer caster, List<BlockPos> bases) {
			super(caster, RAMPART_TICKS + 2);
			this.bases = bases;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age >= 2 && age < 2 + RAMPART_ROWS) {
				int row = (int) age - 2;
				for (BlockPos base : bases) {
					BlockPos cell = base.above(row);
					// Never over a being: no one is buried in it.
					if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(cell)).isEmpty()) continue;
					if (HeavenSpells.placeTemporary(level, cell, Blocks.PACKED_MUD.defaultBlockState(), RAMPART_TICKS - row)) {
						raised.add(cell);
						level.sendParticles(HeavenSpells.block(Blocks.PACKED_MUD), cell.getX() + 0.5, cell.getY() + 0.9, cell.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.1);
					}
				}
				HeavenSpells.sound(level, Vec3.atCenterOf(bases.isEmpty() ? caster.blockPosition() : bases.get(bases.size() / 2)), SoundEvents.PACKED_MUD_PLACE, 1.0f, 0.6f);
			}
			if (age % 10 == 0) {
				for (BlockPos cell : raised) {
					if (cell.getY() == bases.get(0).getY() + RAMPART_ROWS - 1 || !raised.contains(cell.above())) {
						level.sendParticles(dust(Blocks.COARSE_DIRT), cell.getX() + 0.5, cell.getY() + 1.05, cell.getZ() + 0.5, 2, 0.3, 0, 0.3, 0);
					}
				}
			}
			return true;
		}

		/** The blocks it raised (tests). */
		List<BlockPos> raised() {
			return raised;
		}

		@Override
		public void end() {
			for (BlockPos cell : raised) {
				HeavenSpells.removeTemporary(level, cell);
				level.sendParticles(HeavenSpells.block(Blocks.PACKED_MUD), cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0);
			}
			if (!raised.isEmpty()) HeavenSpells.sound(level, Vec3.atCenterOf(raised.get(0)), SoundEvents.PACKED_MUD_BREAK, 1.0f, 0.7f);
		}
	}

	// --- Stone Scale Armour ---

	/** Stone Scale Armour: three scales of stone, each stopping (or blunting) one blow. */
	private static void scales(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		List<Scales> worn = HeavenSpells.lingering(p.getUUID(), Scales.class);
		if (!worn.isEmpty()) {
			worn.get(0).left = SCALES;
			worn.get(0).end = level.getGameTime() + SCALE_TICKS;
		} else {
			HeavenSpells.linger(new Scales(p));
		}
		for (int i = 0; i < 30; i++) {
			double a = i * 0.6, h = i * 0.06;
			level.sendParticles(HeavenSpells.block(Blocks.TUFF), p.getX() + Math.cos(a) * 0.7, p.getY() + h, p.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0);
		}
		HeavenSpells.ring(level, p.position().add(0, 1.2, 0), 0.7, HeavenSpells.dust(BROWN, 1.0f), 16);
		HeavenSpells.sound(level, p.position(), SoundEvents.ARMOR_EQUIP_TURTLE, 1.0f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.DEEPSLATE_PLACE, 1.0f, 0.8f);
	}

	/** The scales left, each spent on a blow (falls, burning and the like are not blows). */
	static final class Scales extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		int left = SCALES;

		Scales(ServerPlayer caster) {
			super(caster, SCALE_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || left <= 0) return false;
			if (age % 10 == 0) {
				for (int s = 0; s < left; s++) HeavenSpells.ring(level, caster.position().add(0, 0.5 + s * 0.5, 0), 0.7, HeavenSpells.dust(OCHRE, 0.8f), 8);
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (target != caster || left <= 0 || amount <= 0 || HeavenPassives.untouchedByWards(source)) return amount;
			boolean blow = source.getEntity() != null || source.getDirectEntity() != null || source.is(DamageTypeTags.IS_EXPLOSION);
			if (!blow || HeavenSpells.inHurtTime(caster)) return amount;
			left--;
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.TUFF), HeavenSpells.middle(caster), 16, 0.4, 0.05);
			HeavenSpells.sound(level, caster.position(), SoundEvents.DECORATED_POT_SHATTER, 1.0f, 0.7f);
			if (left == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.6f);
			return amount <= SCALE_STOPS ? 0 : amount - SCALE_STOPS;
		}
	}

	// --- Swallowing Sands ---

	/** Where the sands open: the top of the block looked at (of its column, if its side was struck). */
	private static Vec3 sandsCentre(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, SANDS_RANGE);
		if (hit == null) return null;
		BlockPos pos = hit.getBlockPos();
		for (int up = 0; up <= 3 && solid(p.serverLevel(), pos.above()); up++) pos = pos.above();
		return Vec3.atBottomCenterOf(pos.above());
	}

	/** Swallowing Sands: the ground round the spot looked at turned to quicksand that holds and sinks whatever stands in it. */
	private static void sands(ServerPlayer p) {
		Vec3 centre = sandsCentre(p);
		if (centre == null) return;
		ServerLevel level = p.serverLevel();
		HeavenSpells.linger(new Sands(p, centre));
		HeavenSpells.ring(level, centre.add(0, 0.1, 0), SANDS_RADIUS, dust(Blocks.SAND), 48);
		HeavenSpells.particles(level, HeavenSpells.block(Blocks.SAND), centre.add(0, 0.2, 0), 30, 0.5, 0.05);
		HeavenSpells.sound(level, centre, SoundEvents.SAND_BREAK, 1.2f, 0.6f);
		HeavenSpells.sound(level, centre, SoundEvents.BRUSH_SAND, 1.0f, 0.7f);
	}

	/** The quicksand: each being in it and how deep it has sunk. */
	static final class Sands extends HeavenSpells.Timed {
		final Vec3 centre;
		final Map<LivingEntity, Integer> depth = new HashMap<>();

		Sands(ServerPlayer caster, Vec3 centre) {
			super(caster, SANDS_TICKS);
			this.centre = centre;
		}

		boolean inside(LivingEntity e) {
			double dx = e.getX() - centre.x, dz = e.getZ() - centre.z;
			return dx * dx + dz * dz <= SANDS_RADIUS * SANDS_RADIUS && e.getY() >= centre.y - 0.5 && e.getY() <= centre.y + 1.0;
		}

		/** How deep {@code e} has sunk (tests). */
		int depth(LivingEntity e) {
			return depth.getOrDefault(e, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			List<LivingEntity> in = HeavenSpells.aroundPoint(level, centre, SANDS_RADIUS + 1.5, e -> !HeavenSpells.spared(caster, e) && inside(e));
			for (LivingEntity e : in) {
				Vec3 v = e.getDeltaMovement();
				if (v.y > 0) HeavenSpells.move(e, new Vec3(v.x, 0, v.z));
				if (age % 2 == 0) level.sendParticles(dust(Blocks.SAND), e.getX(), e.getY() + 0.1, e.getZ(), 2 * Math.max(1, depth(e)), 0.3, 0, 0.3, 0);
			}
			if (age > 0 && age % 20 == 0) {
				for (LivingEntity e : new ArrayList<>(depth.keySet())) {
					if (!in.contains(e)) {
						depth.remove(e);
						e.removeEffect(MobEffects.JUMP);
					}
				}
				for (LivingEntity e : in) {
					int was = depth(e), now2 = Math.min(SANDS_DEEPEST, was + 1);
					depth.put(e, now2);
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, now2 - 1));
					e.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, -6, false, false, false));
					HeavenSpells.strikeAgain(caster, e, SANDS_HARM * now2, QiElement.EARTH);
					HeavenSpells.sound(level, e.position(), SoundEvents.SAND_STEP, 1.0f, 0.5f);
					if (now2 > was) HeavenSpells.sound(level, e.position(), SoundEvents.HONEY_BLOCK_SLIDE, 0.8f, 0.6f);
				}
			}
			if (age % 4 == 0) {
				for (int i = 0; i < 20; i++) {
					double r = SANDS_RADIUS * (1 - i / 20.0), a = i * 0.7 + age * 0.08;
					level.sendParticles(HeavenSpells.block(Blocks.SAND), centre.x + Math.cos(a) * r, centre.y + 0.05, centre.z + Math.sin(a) * r, 1, 0, -0.05, 0, 0);
				}
			}
			if (age % 10 == 0) HeavenSpells.ring(level, centre.add(0, 0.1, 0), SANDS_RADIUS, HeavenSpells.dust(SAND, 1.4f), 40);
			return true;
		}

		@Override
		public void end() {
			for (LivingEntity e : depth.keySet()) if (e.isAlive()) e.removeEffect(MobEffects.JUMP);
			depth.clear();
			HeavenSpells.sound(level, centre, SoundEvents.SAND_FALL, 1.0f, 0.7f);
		}
	}

	// --- Earth-Splitting Fissure ---

	/** Earth-Splitting Fissure: the earth split in a line ahead of the caster, erupting a moment later under all who stand on it. */
	private static void fissure(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Vec3 d = facing(p);
		Vec3 at = HeavenSpells.groundBelow(level, ahead(p, 1), 1, 3);
		if (at == null) at = p.position();
		List<Vec3> line = new ArrayList<>();
		line.add(at);
		for (double gone = 0.5; gone <= FISSURE_REACH; gone += 0.5) {
			Vec3 next = alongGround(level, at, d, 0.5, 3);
			if (next == null) break; // a wall two blocks high stops it
			at = next;
			line.add(at);
		}
		HeavenSpells.linger(new Fissure(p, line));
		HeavenSpells.sound(level, p.position(), SoundEvents.GRAVEL_BREAK, 1.2f, 0.5f);
		HeavenSpells.sound(level, p.position(), SoundEvents.WARDEN_DIG, 0.8f, 1.2f);
	}

	/** The fissure: its line along the ground, cracking, then erupting. */
	static final class Fissure extends HeavenSpells.Timed {
		final List<Vec3> line;

		Fissure(ServerPlayer caster, List<Vec3> line) {
			super(caster, FISSURE_WAIT);
			this.line = line;
		}

		/** How far the fissure runs (tests). */
		int length() {
			return line.size();
		}

		@Override
		protected boolean run(long age, long now) {
			if (caster.level() != level) return false;
			int shown = (int) Math.min(line.size(), (age + 1) * line.size() / 8.0 + 1);
			if (age % 2 == 0) {
				for (int i = 0; i < shown; i++) {
					Vec3 at = line.get(i);
					level.sendParticles(HeavenSpells.block(groundBlock(level, at)), at.x, at.y + 0.1, at.z, 1, 0.1, 0.05, 0.1, 0.05);
					if (i % 2 == 0) level.sendParticles(HeavenSpells.dust(DARK_EARTH, 1.2f), at.x, at.y + 0.05, at.z, 1, 0.05, 0, 0.05, 0);
				}
			}
			if (age < FISSURE_WAIT) return true;
			Set<LivingEntity> struck = new HashSet<>();
			for (Vec3 at : line) {
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1, 2, 1), e -> !HeavenSpells.spared(caster, e))) {
					double dx = e.getX() - at.x, dz = e.getZ() - at.z;
					if (dx * dx + dz * dz > 1.0 || e.getY() < at.y - 0.5 || e.getY() > at.y + 1.5 || !struck.add(e)) continue;
					HeavenSpells.strikeAgain(caster, e, FISSURE_HARM, QiElement.EARTH);
					if (!HeavenSpells.greatBeast(e)) HeavenSpells.move(e, new Vec3(e.getDeltaMovement().x, FISSURE_THROW, e.getDeltaMovement().z));
				}
				level.sendParticles(HeavenSpells.block(groundBlock(level, at)), at.x, at.y + 0.5, at.z, 6, 0.2, 0.4, 0.2, 0.2);
				level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.2, at.z, 0, 0, 1, 0, 0.05);
			}
			Vec3 mid = line.get(line.size() / 2);
			HeavenSpells.sound(level, mid, SoundEvents.WARDEN_ATTACK_IMPACT, 1.2f, 0.6f);
			HeavenSpells.sound(level, mid, SoundEvents.DEEPSLATE_BREAK, 1.2f, 0.5f);
			return false;
		}
	}

	// --- Mount Tai Presses Down ---

	/** Mount Tai Presses Down: the mountain's weight laid on the first being looked at, holding it down and crushing it on the ground. */
	private static void tai(ServerPlayer p) {
		LivingEntity target = HeavenSpells.aimed(p, TAI_RANGE);
		if (target == null) return;
		ServerLevel level = p.serverLevel();
		for (double h = 12; h > 0; h -= 0.5) {
			level.sendParticles(HeavenSpells.block(Blocks.STONE), target.getX(), target.getY() + target.getBbHeight() + h, target.getZ(), 3, 0.3, 0.1, 0.3, 0);
		}
		HeavenSpells.sound(level, target.position(), SoundEvents.BASALT_PLACE, 1.0f, 0.5f);
		HeavenSpells.sound(level, target.position(), SoundEvents.DEEPSLATE_PLACE, 1.0f, 0.4f);
		for (Tai old : HeavenSpells.lingering(null, Tai.class)) if (old.target == target) old.end = 0;
		HeavenSpells.linger(new Tai(p, target));
	}

	/** The weight on its bearer: no rising, dragged down, crushed once on the ground. */
	static final class Tai extends HeavenSpells.Timed {
		final LivingEntity target;
		final boolean wasFlying;
		double highest;
		boolean crushed;

		Tai(ServerPlayer caster, LivingEntity target) {
			super(caster, TAI_TICKS);
			this.target = target;
			this.highest = target.getY();
			target.removeEffect(MobEffects.LEVITATION);
			target.removeEffect(MobEffects.SLOW_FALLING);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TAI_TICKS, 2));
			target.addEffect(new MobEffectInstance(MobEffects.JUMP, TAI_TICKS, -6, false, false, false));
			boolean flying = false;
			if (target instanceof ServerPlayer sp) {
				sp.stopFallFlying();
				flying = sp.getAbilities().flying;
				ground(sp);
			}
			this.wasFlying = flying;
		}

		private static void ground(ServerPlayer sp) {
			if (!sp.getAbilities().flying) return;
			sp.getAbilities().flying = false;
			sp.onUpdateAbilities();
		}

		/** Whether it has been crushed yet (tests). */
		boolean crushed() {
			return crushed;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(target, level) || caster.isRemoved()) return false;
			target.removeEffect(MobEffects.LEVITATION);
			target.removeEffect(MobEffects.SLOW_FALLING);
			if (target instanceof ServerPlayer sp) ground(sp);
			highest = Math.max(highest, target.getY());
			Vec3 v = target.getDeltaMovement();
			double vy = Math.min(v.y, 0);
			boolean down = target.onGround() || (target.isInWater() || target.isInLava());
			if (!down) vy = Math.max(-1.6, vy - 0.15);
			if (vy != v.y) HeavenSpells.move(target, new Vec3(v.x, vy, v.z));
			if (down && !crushed) {
				crushed = true;
				float harm = TAI_CRUSH + ((target.isInWater() || target.isInLava()) ? 0 : (float) Math.min(TAI_FALL_MOST, Math.floor(highest - target.getY())));
				HeavenSpells.strikeAgain(caster, target, harm, QiElement.EARTH);
				HeavenSpells.particles(level, ParticleTypes.EXPLOSION, target.position(), 1, 0, 0);
				HeavenSpells.ring(level, target.position().add(0, 0.1, 0), 1.5, HeavenSpells.block(groundBlock(level, target.position())), 24);
				HeavenSpells.particles(level, ParticleTypes.POOF, target.position(), 10, 0.5, 0.02);
				HeavenSpells.sound(level, target.position(), SoundEvents.WARDEN_ATTACK_IMPACT, 1.0f, 0.5f);
				HeavenSpells.sound(level, target.position(), SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.5f);
			}
			if (age % 2 == 0) {
				Vec3 top = target.position().add(0, target.getBbHeight() + 0.6, 0);
				for (int row = 0; row < 3; row++) {
					for (int k = -row; k <= row; k++) {
						level.sendParticles(HeavenSpells.dust(BROWN, 1.5f), top.x + k * 0.25, top.y + (2 - row) * 0.25, top.z, 1, 0, 0, 0, 0);
					}
				}
				level.sendParticles(dust(Blocks.GRAVEL), top.x, top.y, top.z, 2, 0.4, 0.2, 0.4, 0);
			}
			return true;
		}

		@Override
		public void end() {
			if (!target.isAlive()) return;
			target.removeEffect(MobEffects.JUMP);
			if (wasFlying && target instanceof ServerPlayer sp && sp.getAbilities().mayfly) {
				sp.getAbilities().flying = true;
				sp.onUpdateAbilities();
			}
		}
	}

	// --- Earth Dragon Turns Over ---

	/** The earth dragon's quarry: the being looked at, else the nearest enemy near. */
	private static LivingEntity dragonQuarry(ServerPlayer p) {
		LivingEntity looked = HeavenSpells.aimed(p, DRAGON_RANGE);
		if (looked != null) return looked;
		LivingEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity e : HeavenSpells.around(p, DRAGON_SEEK, e -> enemy(p, e))) {
			double d = e.distanceToSqr(p);
			if (d < best) {
				best = d;
				nearest = e;
			}
		}
		return nearest;
	}

	/** Earth Dragon Turns Over: an earth dragon dives into the ground and hunts its quarry from below, bursting up beneath it. */
	private static void dragon(ServerPlayer p) {
		LivingEntity quarry = dragonQuarry(p);
		if (quarry == null) return;
		HeavenSpells.linger(new Burrower(p, quarry));
		HeavenSpells.sound(p.serverLevel(), p.position(), SoundEvents.WARDEN_DIG, 0.8f, 1.2f);
	}

	/** The dragon under the ground: a point following the surface toward its quarry. */
	static final class Burrower extends HeavenSpells.Timed {
		final LivingEntity quarry;
		final List<Vec3> tail = new ArrayList<>();
		Vec3 at;

		Burrower(ServerPlayer caster, LivingEntity quarry) {
			super(caster, DRAGON_TICKS);
			this.quarry = quarry;
			this.at = caster.position();
		}

		@Override
		protected boolean run(long age, long now) {
			if (!HeavenSpells.present(quarry, level) || caster.isRemoved()) return false;
			double dx = quarry.getX() - at.x, dz = quarry.getZ() - at.z, d = Math.sqrt(dx * dx + dz * dz);
			Vec3 next = d < 1.0e-3 ? at : at.add(dx / d * Math.min(DRAGON_SPEED, d), 0, dz / d * Math.min(DRAGON_SPEED, d));
			if (!level.isLoaded(BlockPos.containing(next))) return false;
			Vec3 surface = HeavenSpells.groundBelow(level, next, 4, 12);
			if (surface != null) at = surface; // else it waits under the gap
			double hx = quarry.getX() - at.x, hz = quarry.getZ() - at.z;
			if (hx * hx + hz * hz <= 0.8 * 0.8 && quarry.getY() - at.y <= 2.5 && quarry.getY() >= at.y - 1) {
				strike();
				return false;
			}
			Vec3 side = d < 1.0e-3 ? Vec3.ZERO : new Vec3(-dz / d, 0, dx / d).scale(Math.sin(age * 0.6) * 0.5);
			Vec3 body = at.add(side);
			level.sendParticles(HeavenSpells.block(groundBlock(level, at)), body.x, body.y + 0.1, body.z, 6, 0.3, 0.1, 0.3, 0.15);
			if (age % 2 == 0) level.sendParticles(HeavenSpells.dust(OCHRE, 1.6f), body.x, body.y + 0.3, body.z, 2, 0.1, 0.1, 0.1, 0);
			tail.add(body);
			if (tail.size() > 8) tail.remove(0);
			for (Vec3 t : tail) level.sendParticles(dust(groundBlock(level, t)), t.x, t.y + 0.1, t.z, 1, 0.1, 0, 0.1, 0);
			if (age % 5 == 0) HeavenSpells.sound(level, at, SoundEvents.SNIFFER_DIGGING, 1.0f, 0.8f);
			return true;
		}

		private void strike() {
			HeavenSpells.strike(caster, quarry, DRAGON_HARM, QiElement.EARTH);
			quarry.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DRAGON_REEL_TICKS, 1));
			quarry.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DRAGON_REEL_TICKS, 0));
			Vec3 v = quarry.getDeltaMovement();
			HeavenSpells.move(quarry, new Vec3(0, v.y, 0));
			for (double h = 0; h < 2; h += 0.25) {
				level.sendParticles(HeavenSpells.block(groundBlock(level, at)), quarry.getX(), at.y + h, quarry.getZ(), 5, 0.4, 0.1, 0.4, 0.1);
			}
			HeavenSpells.particles(level, ParticleTypes.EXPLOSION, HeavenSpells.middle(quarry), 1, 0, 0);
			HeavenSpells.ring(level, at.add(0, 0.2, 0), 1.2, HeavenSpells.dust(BROWN, 2.0f), 16);
			HeavenSpells.sound(level, quarry.position(), SoundEvents.WARDEN_EMERGE, 0.8f, 1.5f);
			HeavenSpells.sound(level, quarry.position(), SoundEvents.ENDER_DRAGON_GROWL, 0.35f, 1.6f);
		}
	}

	// --- Earth Vein Guardian ---

	/** Earth Vein Guardian: the great stone head of the earth vein raised where the caster looks, biting the nearest foe again and again. */
	private static void guardian(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, GUARDIAN_RANGE);
		if (hit == null) return;
		ServerLevel level = p.serverLevel();
		Vec3 at = Vec3.atBottomCenterOf(hit.getBlockPos().above());
		HeavenSpells.endLingering(p.getUUID(), Guardian.class);
		HeavenSpells.linger(new Guardian(p, at));
		HeavenSpells.particles(level, HeavenSpells.block(groundBlock(level, at)), at.add(0, 0.5, 0), 40, 0.8, 0.2);
		HeavenSpells.sound(level, at, SoundEvents.WARDEN_EMERGE, 1.0f, 0.7f);
		HeavenSpells.sound(level, at, SoundEvents.DEEPSLATE_PLACE, 1.0f, 0.5f);
	}

	/** The guardian's head: where it stands, and every two seconds a bite at the nearest foe. */
	static final class Guardian extends HeavenSpells.Timed {
		final Vec3 at;

		Guardian(ServerPlayer caster, Vec3 at) {
			super(caster, GUARDIAN_TICKS);
			this.at = at;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.level() != level) return false;
			Vec3 head = at.add(0, 1.2, 0);
			if (age % 4 == 0) {
				for (int i = 0; i < 24; i++) {
					double y = 1 - 2 * (i + 0.5) / 24, r = Math.sqrt(1 - y * y), a = i * 2.4;
					level.sendParticles(HeavenSpells.block(Blocks.STONE), head.x + Math.cos(a) * r * 1.2, head.y + y * 1.1, head.z + Math.sin(a) * r * 1.2, 1, 0, 0, 0, 0);
				}
				for (int s = -1; s <= 1; s += 2) {
					level.sendParticles(HeavenSpells.dust(0xFFD24A, 1.2f), head.x + s * 0.45, head.y + 0.3, head.z, 1, 0, 0, 0, 0);
					level.sendParticles(HeavenSpells.dust(BROWN, 1.4f), head.x + s * 0.9, head.y + 1.2, head.z, 2, 0.05, 0.15, 0.05, 0);
				}
			}
			if (age == 0 || age % GUARDIAN_EVERY != 0) return true;
			LivingEntity prey = null;
			double best = GUARDIAN_BITE_REACH * GUARDIAN_BITE_REACH;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, GUARDIAN_BITE_REACH, e -> enemy(caster, e))) {
				double d = e.distanceToSqr(at);
				if (d < best) {
					best = d;
					prey = e;
				}
			}
			if (prey == null) return true;
			HeavenSpells.line(level, head, HeavenSpells.middle(prey), HeavenSpells.block(Blocks.STONE), 0.4);
			HeavenSpells.strikeAgain(caster, prey, GUARDIAN_HARM, QiElement.EARTH);
			Vec3 toward = at.subtract(prey.position()).multiply(1, 0, 1);
			if (!HeavenSpells.greatBeast(prey) && toward.lengthSqr() > 1.0e-4) HeavenSpells.move(prey, toward.normalize().scale(GUARDIAN_DRAG).add(0, 0.25, 0));
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.STONE), HeavenSpells.middle(prey), 16, 0.3, 0.05);
			HeavenSpells.sound(level, prey.position(), SoundEvents.EVOKER_FANGS_ATTACK, 1.0f, 0.5f);
			HeavenSpells.sound(level, prey.position(), SoundEvents.STONE_HIT, 1.0f, 0.5f);
			return true;
		}

		@Override
		public void end() {
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.STONE), at.add(0, 1.2, 0), 40, 0.8, 0.1);
			HeavenSpells.sound(level, at, SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.6f);
		}
	}

	// --- Bedrock Bears the Blows ---

	/** Bedrock Bears the Blows: the bedrock takes every hurt first; part sinks away, the rest reaches the caster slowly. */
	private static void bedrock(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		List<Bedrock> worn = HeavenSpells.lingering(p.getUUID(), Bedrock.class);
		if (!worn.isEmpty()) worn.get(0).wardUntil = level.getGameTime() + BEDROCK_TICKS; // its debts kept
		else HeavenSpells.linger(new Bedrock(p));
		for (int i = 0; i < 24; i++) {
			double a = Math.PI * 2 * i / 24;
			level.sendParticles(HeavenSpells.block(Blocks.DIRT), p.getX() + Math.cos(a), p.getY() + 1.4, p.getZ() + Math.sin(a), 0, -Math.cos(a), -1, -Math.sin(a), 0.1);
		}
		HeavenSpells.ring(level, p.position().add(0, 0.1, 0), 1, HeavenSpells.dust(OCHRE, 1.2f), 16);
		HeavenSpells.sound(level, p.position(), SoundEvents.MUD_PLACE, 1.0f, 0.6f);
		HeavenSpells.sound(level, p.position(), SoundEvents.DEEPSLATE_STEP, 1.0f, 0.5f);
	}

	/** The ward and the debts it holds (each slice: when due, how much); the debts outlive the ward. */
	static final class Bedrock extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		final List<double[]> debts = new ArrayList<>();
		long wardUntil;
		boolean repaying;

		Bedrock(ServerPlayer caster) {
			super(caster, BEDROCK_TICKS + BEDROCK_SLICES * BEDROCK_SLICE_EVERY + 2);
			this.wardUntil = start + BEDROCK_TICKS;
		}

		@Override
		public boolean tick(long now) {
			long at = level.getGameTime();
			end = Math.max(end, wardUntil + BEDROCK_SLICES * BEDROCK_SLICE_EVERY + 2);
			return run(at - start, at) && (at <= wardUntil || !debts.isEmpty());
		}

		/** What is still owed (tests). */
		double owed() {
			double sum = 0;
			for (double[] d : debts) sum += d[1];
			return sum;
		}

		@Override
		protected boolean run(long age, long now) {
			if (!caster.isAlive() || caster.isRemoved()) {
				debts.clear();
				return false;
			}
			double due = 0;
			for (java.util.Iterator<double[]> it = debts.iterator(); it.hasNext(); ) {
				double[] d = it.next();
				if (now >= d[0]) {
					due += d[1];
					it.remove();
				}
			}
			if (due > 0) {
				repaying = true;
				caster.invulnerableTime = 0;
				caster.hurt(ModDamageTypes.qi(level, QiElement.EARTH, null), (float) due);
				repaying = false;
				level.sendParticles(HeavenSpells.dust(BROWN, 0.8f), caster.getX(), caster.getY() + 1.2, caster.getZ(), 3, 0.2, 0.2, 0.2, 0);
			}
			if (now <= wardUntil && age % 5 == 0) {
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8 + age * 0.1;
					level.sendParticles(HeavenSpells.dust(BROWN, 1.0f), caster.getX() + Math.cos(a) * 0.9, caster.getY() + 0.1, caster.getZ() + Math.sin(a) * 0.9, 1, 0, 0, 0, 0);
				}
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (target != caster || repaying || amount <= 0 || level.getGameTime() > wardUntil || HeavenPassives.untouchedByWards(source)
					|| TribulationManager.isActive(caster.getUUID()) || HeavenSpells.inHurtTime(caster)) {
				return amount;
			}
			float hurt = source.is(DamageTypeTags.BYPASSES_ARMOR) ? amount
					: CombatRules.getDamageAfterAbsorb(amount, caster.getArmorValue(), (float) caster.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			double slice = hurt * BEDROCK_KEEPS / BEDROCK_SLICES;
			long now = level.getGameTime();
			for (int i = 1; i <= BEDROCK_SLICES; i++) debts.add(new double[]{now + (long) i * BEDROCK_SLICE_EVERY, slice});
			for (int i = 0; i < 8; i++) {
				double h = 1.2 - i * 0.15;
				level.sendParticles(HeavenSpells.block(Blocks.DIRT), caster.getX(), caster.getY() + h, caster.getZ(), 1, 0, 0, 0, 0);
			}
			HeavenSpells.ring(level, caster.position().add(0, 0.05, 0), 1.2, HeavenSpells.dust(OCHRE, 1.0f), 12);
			HeavenSpells.sound(level, caster.position(), SoundEvents.MUD_STEP, 1.2f, 0.6f);
			return 0;
		}
	}

	// --- Shoulder the Myriad Burdens ---

	/** Beings taking a burden passed on to them now: no bond takes it again. */
	private static final Set<LivingEntity> PASSING = new HashSet<>();

	/** Shoulder the Myriad Burdens: the caster bears half of every hurt that befalls its allies near, its stone body halving it again. */
	private static void burdens(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		HeavenSpells.endLingering(p.getUUID(), Burdens.class);
		HeavenSpells.linger(new Burdens(p));
		HeavenSpells.sound(level, p.position(), SoundEvents.DEEPSLATE_BRICKS_PLACE, 1.0f, 0.5f);
		HeavenSpells.sound(level, p.position(), SoundEvents.WARDEN_HEARTBEAT, 0.8f, 0.8f);
	}

	/** The bond: the burdens passed on to the caster, dealt the tick after. */
	static final class Burdens extends HeavenSpells.Timed implements HeavenSpells.HarmListener {
		float queued;

		Burdens(ServerPlayer caster) {
			super(caster, BURDEN_TICKS);
		}

		private boolean broken() {
			return !casterHere() || caster.getHealth() <= caster.getMaxHealth() * BURDEN_BREAK;
		}

		/** Whether {@code e} is covered by this bond: an ally of the caster's within reach. */
		boolean covers(LivingEntity e) {
			return e != caster && e.level() == level && HeavenSpells.ally(caster, e) && e.distanceTo(caster) <= BURDEN_RADIUS;
		}

		@Override
		protected boolean run(long age, long now) {
			if (broken()) {
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.STONE), HeavenSpells.middle(caster), 20, 0.4, 0.05);
				HeavenSpells.sound(level, caster.position(), SoundEvents.DEEPSLATE_BREAK, 1.0f, 0.8f);
				return false;
			}
			if (queued > 0) {
				float due = queued;
				queued = 0;
				PASSING.add(caster);
				try {
					caster.invulnerableTime = 0;
					caster.hurt(ModDamageTypes.qi(level, QiElement.EARTH, null), due);
				} finally {
					PASSING.remove(caster);
				}
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.STONE), caster.position().add(0, 1.4, 0), 6, 0.3, 0.02);
				HeavenSpells.sound(level, caster.position(), SoundEvents.STONE_HIT, 1.0f, 0.5f);
			}
			if (age < 10) HeavenSpells.ring(level, caster.position().add(0, 0.2, 0), 1 + 15 * (age + 1) / 10.0, HeavenSpells.dust(OCHRE, 1.5f), 96);
			if (age % 10 == 0) {
				for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(BURDEN_RADIUS), this::covers)) {
					HeavenSpells.line(level, ally.position().add(0, 0.1, 0), caster.position().add(0, 0.1, 0), HeavenSpells.dust(BROWN, 0.9f), 0.8);
				}
			}
			return true;
		}

		@Override
		public float onIncoming(LivingEntity target, DamageSource source, float amount) {
			if (amount <= 0 || PASSING.contains(target) || !covers(target) || HeavenPassives.untouchedByWards(source) || HeavenSpells.inHurtTime(target)
					|| target instanceof ServerPlayer sp && TribulationManager.isActive(sp.getUUID()) || broken()) {
				return amount;
			}
			// Several bonds over one ally: the nearest caster's alone.
			for (Burdens other : HeavenSpells.lingering(null, Burdens.class)) {
				if (other != this && other.covers(target) && other.caster.distanceToSqr(target) < caster.distanceToSqr(target)) return amount;
			}
			queued += amount * BURDEN_PASSES;
			HeavenSpells.line(level, HeavenSpells.middle(target), caster.position().add(0, 1.4, 0), HeavenSpells.dust(OCHRE, 1.3f), 0.5);
			return amount * BURDEN_KEEPS;
		}
	}

	// --- Riding Ten Thousand Dunes ---

	/** Riding Ten Thousand Dunes: a river of sand carries the caster and its allies near along the ground, trampling foes in its way. */
	private static void dunes(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		HeavenSpells.endLingering(p.getUUID(), Dunes.class);
		Dunes dunes = new Dunes(p);
		dunes.riders.add(p);
		dunes.riders.addAll(HeavenSpells.around(p, DUNES_RIDERS, e -> HeavenSpells.ally(p, e)));
		HeavenSpells.linger(dunes);
		HeavenSpells.sound(level, p.position(), SoundEvents.SAND_BREAK, 1.5f, 0.5f);
		HeavenSpells.sound(level, p.position(), SoundEvents.CAMEL_DASH, 1.0f, 0.8f);
	}

	/** The river of sand: those it carries, and those it has trampled. */
	static final class Dunes extends HeavenSpells.Timed {
		final Set<LivingEntity> riders = new LinkedHashSet<>();
		final Set<LivingEntity> trampled = new HashSet<>();

		Dunes(ServerPlayer caster) {
			super(caster, DUNES_TICKS);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere() || age > 2 && caster.isShiftKeyDown()) return false;
			Vec3 d = facing(caster);
			riders.removeIf(r -> !HeavenSpells.present(r, level) || r != caster && r.isShiftKeyDown());
			for (LivingEntity r : riders) {
				Vec3 foot = r.position();
				BlockPos front = BlockPos.containing(foot.x + d.x * 0.8, foot.y + 0.01, foot.z + d.z * 0.8);
				boolean step = solid(level, front), high = step && solid(level, front.above()) && solid(level, front.above(2));
				double vy = r.getDeltaMovement().y;
				if (step && !high && !solid(level, front.above())) vy = 0.55;
				if (!level.getFluidState(BlockPos.containing(foot.x, foot.y - 0.2, foot.z)).isEmpty() || r.isInWater() || r.isInLava()) vy = Math.max(vy, 0.05);
				else if (!r.onGround()) vy = Math.max(vy, -0.8);
				Vec3 flat = high ? Vec3.ZERO : d.scale(DUNES_SPEED);
				HeavenSpells.move(r, new Vec3(flat.x, vy, flat.z));
				r.resetFallDistance();
				HeavenSpells.safeFall(r, DUNES_SAFE_TICKS);
				level.sendParticles(HeavenSpells.block(Blocks.SAND), foot.x - d.x * 0.6, foot.y + 0.1, foot.z - d.z * 0.6, 8, 0.3, 0.1, 0.3, 0.1);
				level.sendParticles(dust(Blocks.SAND), foot.x, foot.y + 0.2, foot.z, 3, 0.4, 0.1, 0.4, 0);
				level.sendParticles(HeavenSpells.dust(SAND, 1.6f), foot.x - d.x, foot.y + 0.1, foot.z - d.z, 4, 1.0, 0.05, 1.0, 0);
				for (LivingEntity foe : HeavenSpells.aroundPoint(level, foot, DUNES_TRAMPLE, e -> !riders.contains(e) && enemy(caster, e) && !trampled.contains(e))) {
					trampled.add(foe);
					HeavenSpells.strike(caster, foe, DUNES_HARM, QiElement.EARTH);
					foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DUNES_BURY_TICKS, 3));
					HeavenSpells.particles(level, HeavenSpells.block(Blocks.SAND), HeavenSpells.middle(foe), 20, 0.4, 0.1);
					HeavenSpells.particles(level, HeavenSpells.dust(BROWN, 1.5f), HeavenSpells.middle(foe), 8, 0.4, 0);
					HeavenSpells.sound(level, foe.position(), SoundEvents.SAND_FALL, 1.0f, 0.6f);
					HeavenSpells.sound(level, foe.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.7f);
				}
			}
			if (age % 2 == 0) {
				Vec3 crest = caster.position().add(d.scale(1.5));
				Vec3 side = new Vec3(-d.z, 0, d.x);
				for (int i = -5; i <= 5; i++) {
					Vec3 at = crest.add(side.scale(i * 0.3)).subtract(d.scale(Math.abs(i) * 0.1));
					level.sendParticles(HeavenSpells.block(Blocks.SAND), at.x, at.y + 0.3, at.z, 1, 0, 0.1, 0, 0.05);
				}
			}
			if (age % 4 == 0) HeavenSpells.sound(level, caster.position(), SoundEvents.CAMEL_STEP_SAND, 1.0f, 0.7f);
			return true;
		}

		@Override
		public void end() {
			for (LivingEntity r : riders) {
				if (!r.isAlive()) continue;
				HeavenSpells.safeFall(r, DUNES_SAFE_TICKS);
				HeavenSpells.particles(level, HeavenSpells.block(Blocks.SAND), r.position().add(0, 0.3, 0), 30, 0.6, 0.05);
			}
			if (caster.isAlive()) HeavenSpells.sound(level, caster.position(), SoundEvents.SAND_PLACE, 1.0f, 0.8f);
		}
	}

	// --- Fall of Buzhou ---

	/** Where Mount Buzhou falls: the point looked at, or the ground straight below the end of the look; null over the void. */
	private static Vec3 buzhouPoint(ServerPlayer p) {
		BlockHitResult hit = HeavenSpells.lookedBlock(p, BUZHOU_RANGE);
		if (hit != null) return hit.getLocation();
		Vec3 end = p.getEyePosition().add(p.getViewVector(1.0f).scale(BUZHOU_RANGE));
		return HeavenSpells.groundBelow(p.serverLevel(), end, 0, 32);
	}

	/** Fall of Buzhou: the broken pillar of heaven called down on the point looked at, crushing everything in its shadow. */
	private static void buzhou(ServerPlayer p) {
		Vec3 at = buzhouPoint(p);
		if (at == null) return;
		HeavenSpells.linger(new Buzhou(p, at));
		HeavenSpells.sound(p.serverLevel(), at, SoundEvents.AMBIENT_CAVE.value(), 2.0f, 1.0f);
	}

	/** The falling mountain: its shadow spreading, its landing, then its dust settling. */
	static final class Buzhou extends HeavenSpells.Timed {
		final Vec3 at;

		Buzhou(ServerPlayer caster, Vec3 at) {
			super(caster, BUZHOU_FALL + BUZHOU_SETTLE);
			this.at = at;
		}

		private boolean band(LivingEntity e) {
			return e.getY() >= at.y - 4 && e.getY() <= at.y + 24;
		}

		private double flat(LivingEntity e) {
			double dx = e.getX() - at.x, dz = e.getZ() - at.z;
			return Math.sqrt(dx * dx + dz * dz);
		}

		@Override
		protected boolean run(long age, long now) {
			if (caster.isRemoved() || caster.level() != level && age < BUZHOU_FALL) return false;
			java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
			if (age < BUZHOU_FALL) {
				if (age % 2 == 0) {
					HeavenSpells.ring(level, at.add(0, 0.2, 0), BUZHOU_SHADOW, HeavenSpells.dust(DARK_EARTH, 2.0f), 120);
					for (int i = 0; i < 30; i++) {
						double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * BUZHOU_SHADOW;
						level.sendParticles(dust(Blocks.GRAVEL), at.x + Math.cos(a) * r, at.y + 6 + random.nextDouble() * 4, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
					}
					double t = age / (double) BUZHOU_FALL, base = 40 * (1 - t) * (1 - t);
					for (double h = 0; h <= 16; h += 2) {
						double r = 10 * (1 - h / 16);
						int n = Math.max(4, (int) (r * 4));
						for (int i = 0; i < n; i++) {
							double a = Math.PI * 2 * i / n + h;
							Vec3 point = at.add(Math.cos(a) * r, base + h, Math.sin(a) * r);
							level.sendParticles(i % 3 == 0 ? HeavenSpells.block(Blocks.STONE) : HeavenSpells.dust(BROWN, 3.0f), point.x, point.y, point.z, 1, 0, 0, 0, 0);
						}
					}
					level.sendParticles(HeavenSpells.dust(OCHRE, 3.0f), at.x, at.y + base + 16, at.z, 6, 1.0, 0.3, 1.0, 0);
				}
				if (age % 10 == 0) {
					HeavenSpells.sound(level, at.add(random.nextDouble() * 20 - 10, 0, random.nextDouble() * 20 - 10), SoundEvents.POINTED_DRIPSTONE_FALL, 1.5f, 0.5f);
					HeavenSpells.sound(level, at.add(random.nextDouble() * 20 - 10, 0, random.nextDouble() * 20 - 10), SoundEvents.GRAVEL_FALL, 1.0f, 1.0f);
				}
				if (age == 40) HeavenSpells.sound(level, at, SoundEvents.BASALT_BREAK, 2.0f, 0.3f);
				return true;
			}
			if (age == BUZHOU_FALL) land();
			if (age > BUZHOU_FALL && age < BUZHOU_FALL + 10) {
				double r = BUZHOU_SHADOW + (BUZHOU_QUAKE - BUZHOU_SHADOW) * (age - BUZHOU_FALL) / 10.0;
				HeavenSpells.ring(level, at.add(0, 0.3, 0), r, HeavenSpells.dust(OCHRE, 3.0f), 120);
				HeavenSpells.ring(level, at.add(0, 1.0, 0), r - 1, HeavenSpells.dust(OCHRE, 3.0f), 100);
			}
			if (age % 4 == 0) {
				for (int i = 0; i < 20; i++) {
					double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * BUZHOU_SHADOW;
					level.sendParticles(dust(Blocks.SAND), at.x + Math.cos(a) * r, at.y + 4 + random.nextDouble() * 4, at.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
				}
			}
			return true;
		}

		private void land() {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(BUZHOU_QUAKE + 1, 25, BUZHOU_QUAKE + 1),
					e -> !HeavenSpells.spared(caster, e) && band(e))) {
				double d = flat(e);
				if (d <= BUZHOU_SHADOW) {
					float harm = Math.max(BUZHOU_LEAST, Math.min(BUZHOU_MOST, e.getMaxHealth() * BUZHOU_SHARE));
					HeavenSpells.strikeAgain(caster, e, harm, QiElement.EARTH);
					if (!HeavenSpells.greatBeast(e)) HeavenSpells.move(e, new Vec3(0, -1.0, 0));
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BUZHOU_BURY_TICKS, 3));
					e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, BUZHOU_BURY_TICKS, 2));
				} else if (d <= BUZHOU_QUAKE && !HeavenSpells.greatBeast(e)) {
					Vec3 away = e.position().subtract(at).multiply(1, 0, 1).normalize();
					HeavenSpells.move(e, away.scale(1.6).add(0, 0.3, 0));
				}
			}
			HeavenSpells.particles(level, ParticleTypes.EXPLOSION_EMITTER, at.add(0, 1, 0), 1, 0, 0);
			for (int q = 0; q < 4; q++) {
				double a = Math.PI / 2 * q;
				HeavenSpells.particles(level, ParticleTypes.EXPLOSION_EMITTER, at.add(Math.cos(a) * 8, 1, Math.sin(a) * 8), 1, 0, 0);
			}
			HeavenSpells.particles(level, HeavenSpells.block(Blocks.STONE), at.add(0, 1, 0), 300, BUZHOU_SHADOW / 2, 0.5);
			HeavenSpells.particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.add(0, 1, 0), 40, BUZHOU_SHADOW / 2, 0.05);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.DEEPSLATE_BREAK, 3.0f, 0.4f);
		}
	}

	private EarthSpells() {}
}
