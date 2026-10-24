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
 *   <li>Dao Integration: Qilin Tramples the Land (a qilin's charge), Seal of Ten Thousand Mountains (a seal as great as a hill).</li>
 *   <li>Void Shattering: Buzhou Falls (the broken pillar of heaven falls out of a rift in the sky), Earth Veins Lock the Void (the earth's
 *       veins rise in a dome round you and lock space within: foes held, crushed, and no way out through space).</li>
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
	/**
	 * The quake shakes {@link #QUAKE_RADIUS} round; its fissures run out to {@link #QUAKE_FISSURE_REACH} (40, by its realm:
	 * Devastation#sized).
	 */
	public static final double QUAKE_RADIUS = 20, QUAKE_FISSURE_REACH = Devastation.sized(Realm.NASCENT_SOUL, 72);
	public static final int QUAKE_TICKS = 4 * 20, QUAKE_FISSURES = 14, QUAKE_FISSURE_DEPTH = (int) Math.round(Devastation.sized(Realm.NASCENT_SOUL, 12));
	public static final float QUAKE_DAMAGE = 14;
	/** The mountain's crater (22 round, 13 deep, its fissures out to 48: by its realm, Devastation#sized). */
	public static final double MOUNTAIN_RANGE = 64, MOUNTAIN_RADIUS = 10, MOUNTAIN_CRATER = Devastation.sized(Realm.HEAVENLY_BEING, 28),
			MOUNTAIN_CRATER_DEPTH = MOUNTAIN_CRATER * 0.6;
	public static final float MOUNTAIN_DAMAGE = 140;
	/** The seal presses foes {@link #SEAL_RADIUS} round; the land sinks under it out to {@link #SEAL_SINK_RADIUS} (37, by its realm). */
	public static final double SEAL_RADIUS = 24, SEAL_SINK_RADIUS = Devastation.sized(Realm.HEAVENLY_BEING, 48);
	public static final int SEAL_TICKS = 6 * 20, SEAL_SINK = (int) Math.round(Devastation.sized(Realm.HEAVENLY_BEING, 4));
	public static final float SEAL_DAMAGE = 20;
	public static final int TITAN_TICKS = 60 * 20;
	public static final double TOMB_RANGE = 48;
	public static final int TOMB_RADIUS = 7, TOMB_HEIGHT = 9, TOMB_TICKS = 5 * 20, TOMB_LASTS = 20 * 20;
	/** How far the land tears open round the mountain's foot (40, by its realm: Devastation#sized). */
	public static final double TOMB_FISSURES = Devastation.sized(Realm.FOUR_AXIS, 40);
	public static final float TOMB_DAMAGE = 60;
	/**
	 * Qilin Tramples the Land: how far it runs, how wide its path ({@link #QILIN_PATH} each side), a crater every {@link #QILIN_STRIDE}
	 * blocks (5 round, 3 deep: by its realm); its rearing (and the stamp in it), its gallop and its fading (ticks).
	 */
	public static final double QILIN_RUN = HeavenSpells.span(Realm.DAO_INTEGRATION, 100), QILIN_PATH = HeavenSpells.span(Realm.DAO_INTEGRATION, 5), QILIN_STRIDE = HeavenSpells.span(Realm.DAO_INTEGRATION, 8),
			QILIN_AHEAD = HeavenSpells.span(Realm.DAO_INTEGRATION, 8), QILIN_FOREFEET = HeavenSpells.span(Realm.DAO_INTEGRATION, 3.5), QILIN_CRATER = Devastation.sized(Realm.DAO_INTEGRATION, 3.5),
			QILIN_CRATER_DEPTH = Devastation.sized(Realm.DAO_INTEGRATION, 2.5);
	public static final float QILIN_DAMAGE = 480;
	public static final int QILIN_REAR = 26, QILIN_STAMP = 18, QILIN_GALLOP = 60, QILIN_FADE = 14;
	/**
	 * Seal of Ten Thousand Mountains: where it falls (up to {@link #MOUNTAINS_RANGE}, from {@link #MOUNTAINS_HIGH} above), whom it crushes
	 * (within {@link #MOUNTAINS_RADIUS}); the land it presses down (20 round, {@link #MOUNTAINS_DROP} down) and the fissures it splits (out
	 * to 40: by its realm); its fall, its hold on the crushed, and how long it stands (ticks).
	 */
	public static final double MOUNTAINS_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), MOUNTAINS_HIGH = HeavenSpells.span(Realm.DAO_INTEGRATION, 60), MOUNTAINS_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 14), MOUNTAINS_SINK = Devastation.sized(Realm.DAO_INTEGRATION, 16),
			MOUNTAINS_FISSURE_REACH = Devastation.sized(Realm.DAO_INTEGRATION, 31);
	public static final int MOUNTAINS_FALL = 24, MOUNTAINS_HOLD = 3 * 20, MOUNTAINS_STAND = 6 * 20, MOUNTAINS_DROP = 4, MOUNTAINS_FISSURES = 12,
			MOUNTAINS_FISSURE_DEPTH = (int) Math.round(Devastation.sized(Realm.DAO_INTEGRATION, 5));
	public static final float MOUNTAINS_DAMAGE = 550;
	/** The earth's amber, beyond the void: Buzhou's rift glows so. */
	public static final int EARTH_AMBER = 0xFFA548;
	/**
	 * Buzhou Falls: where (up to {@link #BUZHOU_RANGE}, 118 by its realm), how high its rift opens (70) and how long it is; whom the pillar
	 * crushes (within {@link #BUZHOU_RADIUS}, 22) and how hard; its crater (17 round, 13 deep) and fissures (out to 70, 11 deep), by its realm;
	 * how long the rift takes to open, the pillar's fall, how long the crushed are pinned, and how long it stands after (ticks).
	 */
	public static final double BUZHOU_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), BUZHOU_HIGH = HeavenSpells.span(Realm.VOID_SHATTERING, 38),
			BUZHOU_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 22), BUZHOU_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 12),
			BUZHOU_CRATER = Devastation.sized(Realm.VOID_SHATTERING, 9), BUZHOU_CRATER_DEPTH = Devastation.sized(Realm.VOID_SHATTERING, 7),
			BUZHOU_FISSURE_REACH = Devastation.sized(Realm.VOID_SHATTERING, 38);
	public static final float BUZHOU_DAMAGE = 1600;
	public static final int BUZHOU_OPENS = 14, BUZHOU_FALL = 22, BUZHOU_HOLD = 2 * 20, BUZHOU_STAND = 5 * 20, BUZHOU_FISSURES = 14,
			BUZHOU_FISSURE_DEPTH = (int) Math.round(Devastation.sized(Realm.VOID_SHATTERING, 6));
	/**
	 * Earth Veins Lock the Void: how far round its caster the veins' dome reaches ({@link #LOCK_RADIUS}, 26 by its realm), what each foe in it
	 * is crushed for a second; how long the veins take to rise, how long they hold, and how long before the end they begin to sink (ticks).
	 */
	public static final double LOCK_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 14);
	public static final float LOCK_DAMAGE = 200;
	public static final int LOCK_RISES = 16, LOCK_TICKS = 6 * 20, LOCK_RELEASE = 16;

	/** Each caster's golems, oldest first. */
	private static final Map<UUID, List<EarthGolem>> GOLEMS_OF = new HashMap<>();
	private static final Map<UUID, EarthGolem> TITANS = new HashMap<>();

	static boolean canCast(LivingEntity p, Spell spell) {
		return switch (spell) {
			case DIRT_TIDE -> HeavenSpells.groundBelow(HeavenSpells.level(p), p.position(), 0, 3) != null || HeavenSpells.refuse(p);
			case BOULDER_THROW -> crater(p).size() >= 4 || noEarth(p);
			case CAVE_FORMATION -> HeavenSpells.lookedBlock(p, CAVE_REACH) != null || HeavenSpells.refuse(p);
			default -> true;
		};
	}

	private static boolean noEarth(LivingEntity p) {
		HeavenSpells.tell(p, net.minecraft.network.chat.Component.translatable(ModLang.MSG_NO_EARTH));
		return false;
	}

	static void cast(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		switch (spell) {
			case EARTH_SPEAR -> spear(p, spell);
			case HARDENED_BODY -> {
				SpellAuras.put(p, SpellAura.HARDENED_BODY, HARDENED_TICKS);
				HeavenSpells.sound(level, p.position(), SoundEvents.STONE_PLACE, 1.2f, 0.6f);
				HeavenSpells.sound(level, p.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.8f);
				SpellFx.send(level, SpellFx.Kind.DUST, p.position(), Vec3.ZERO, 1.0f, Block.getId(groundUnder(p)));
			}
			case DIRT_TIDE -> {
				if (!(p instanceof ServerPlayer rider)) return; // a wave a player rides
				if (rider.getVehicle() instanceof DirtTide old) old.sink();
				DirtTide.raise(rider, groundUnder(p), TIDE_TICKS);
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
			case QILIN_TRAMPLES_THE_LAND -> HeavenSpells.linger(new Qilin(p, spell));
			case SEAL_OF_TEN_THOUSAND_MOUNTAINS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, MOUNTAINS_RANGE);
				HeavenSpells.linger(new Mountains(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, MOUNTAINS_RANGE)));
			}
			case BUZHOU_FALLS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, BUZHOU_RANGE);
				HeavenSpells.linger(new Buzhou(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, BUZHOU_RANGE)));
			}
			case EARTH_VEINS_LOCK_THE_VOID -> {
				HeavenSpells.endLingering(p.getUUID(), Lock.class);
				HeavenSpells.linger(new Lock(p, spell));
			}
			default -> {}
		}
	}

	/**
	 * The block {@code p} stands on, as earth (grass as dirt), or dirt where there is none of it under them: only the land's own ground
	 * (earth, sand, gravel, clay, snow, stone, the Nether's and the End's rock), never a block worth copying (an ore, a diamond block,
	 * TNT: a wall of them pushed aside by a piston, or purified, was theirs to keep, found in the sweep of 2026-10-23).
	 */
	static BlockState groundUnder(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		for (int dy = 1; dy <= 16; dy++) {
			BlockPos pos = BlockPos.containing(p.getX(), p.getY() - dy + 0.5, p.getZ());
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty()) continue;
			if (state.isCollisionShapeFullBlock(level, pos) && !state.hasBlockEntity() && natural(state)) return HeavenSpells.asEarth(state);
			break;
		}
		return Blocks.DIRT.defaultBlockState();
	}

	/** The land's own ground, as an earth spell raises it: nothing mined for its worth. */
	private static boolean natural(BlockState state) {
		return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.tags.BlockTags.SAND) || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
				|| state.is(net.minecraft.tags.BlockTags.BASE_STONE_NETHER) || state.is(net.minecraft.tags.BlockTags.TERRACOTTA)
				|| state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
				|| state.is(Blocks.MUD) || state.is(Blocks.PACKED_MUD) || state.is(Blocks.CALCITE) || state.is(Blocks.DRIPSTONE_BLOCK)
				|| state.is(Blocks.END_STONE) || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.COBBLESTONE)
				|| state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE);
	}

	// --- Qi Refining ---

	private static void spear(LivingEntity p, Spell spell) {
		SpellProjectile.cast(p, spell, SpellProjectile.Shape.EARTH_PODAO, 1.0f, 10, new Vec3(0.2, 0.35, 1.5), SPEAR_SPEED, SPEAR_RANGE,
				new SpellProjectile.Behaviour() {
					@Override
					public boolean hitEntity(SpellProjectile missile, LivingEntity target) {
						HeavenSpells.strike(p, target, SPEAR_DAMAGE, spell, missile);
						HeavenSpells.push(missile.position().subtract(missile.getDeltaMovement()), target, 1.0);
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.EARTH_BURST, missile.position(), Vec3.ZERO, 0.8f, missile.data());
						HeavenSpells.sound(HeavenSpells.level(p), target.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.8f);
						return false;
					}

					@Override
					public void hitBlock(SpellProjectile missile, BlockHitResult hit) {
						SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.EARTH_BURST, hit.getLocation(), Vec3.ZERO, 0.8f, missile.data());
						HeavenSpells.sound(HeavenSpells.level(p), hit.getLocation(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.9f);
					}

					@Override
					public void launched(SpellProjectile missile) {
						HeavenSpells.sound(HeavenSpells.level(p), missile.position(), SoundEvents.TRIDENT_THROW, 1.0f, 0.6f);
					}
				}).data(Block.getId(groundUnder(p)));
		HeavenSpells.sound(HeavenSpells.level(p), p.position(), SoundEvents.ROOTED_DIRT_PLACE, 1.0f, 0.7f);
		SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.DUST, p.position(), Vec3.ZERO, 0.6f, Block.getId(groundUnder(p)));
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

		Wall(LivingEntity caster) {
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
	static List<BlockPos> crater(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
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

	private static void boulder(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
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

	private static void cave(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
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

		Carving(LivingEntity caster, List<BlockPos> blocks) {
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

		Surge(LivingEntity caster, Spell spell) {
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

	private static void golem(LivingEntity p) {
		List<EarthGolem> mine = GOLEMS_OF.computeIfAbsent(p.getUUID(), k -> new ArrayList<>());
		mine.removeIf(g -> !g.isAlive() || g.isRemoved());
		while (mine.size() >= GOLEMS) mine.remove(0).crumble();
		Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot() + (mine.isEmpty() ? 35 : -35)).scale(2.5));
		Vec3 ground = HeavenSpells.groundBelow(HeavenSpells.level(p), at.add(0, 2, 0), 3, 8);
		EarthGolem golem = EarthGolem.raise(p, ground != null ? ground : at, false, GOLEM_TICKS);
		mine.add(golem);
		SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.EARTH_BURST, golem.position().add(0, 0.5, 0), Vec3.ZERO, 2.0f, Block.getId(groundUnder(p)));
		HeavenSpells.sound(HeavenSpells.level(p), golem.position(), SoundEvents.IRON_GOLEM_REPAIR, 1.5f, 0.5f);
		HeavenSpells.sound(HeavenSpells.level(p), golem.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.5f);
	}

	/** {@code p}'s golems now (tests). */
	public static List<EarthGolem> golems(LivingEntity p) {
		List<EarthGolem> mine = GOLEMS_OF.getOrDefault(p.getUUID(), List.of());
		return mine.stream().filter(g -> g.isAlive() && !g.isRemoved()).toList();
	}

	/** Earthquake: the ground within {@link #QUAKE_RADIUS} of its caster shakes; those on it are hurt, slowed and tossed. */
	public static final class Quake extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellField field;

		Quake(LivingEntity caster, Spell spell) {
			super(caster, QUAKE_TICKS);
			this.spell = spell;
			this.field = SpellField.create(level, SpellField.Kind.EARTHQUAKE, caster.position(), (float) QUAKE_RADIUS, 2, QUAKE_TICKS, caster)
					.data(Block.getId(groundUnder(caster))).spawn();
			// The ground splits: fissures running out from a little way off the caster to the quake's edge.
			Devastation.fissures(caster, level, caster.position(), QUAKE_FISSURE_REACH, QUAKE_FISSURES, QUAKE_FISSURE_DEPTH, 3);
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

	private static void mountain(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
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
						Devastation.fissures(p, level, where, MOUNTAIN_CRATER * 2.2, 10, 5, MOUNTAIN_CRATER);
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
	static void slam(LivingEntity p, Spell spell, ServerLevel level, Vec3 at, double radius, float damage, int stateId) {
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

		Seal(LivingEntity caster, Spell spell) {
			super(caster, SEAL_TICKS);
			this.spell = spell;
			this.field = SpellField.create(level, SpellField.Kind.HEAVY_SEAL, caster.position(), (float) SEAL_RADIUS, 2, SEAL_TICKS, caster).spawn();
			// Under its weight the land sinks (all but where the caster stands), cracked, split here and there.
			Devastation.subside(caster, level, caster.position(), SEAL_SINK_RADIUS, SEAL_SINK, 3);
			Devastation.fissures(caster, level, caster.position(), SEAL_SINK_RADIUS, 14, 4, 4);
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

	private static void titan(LivingEntity p) {
		EarthGolem old = TITANS.remove(p.getUUID());
		if (old != null && old.isAlive()) old.crumble();
		Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(6));
		Vec3 ground = HeavenSpells.groundBelow(HeavenSpells.level(p), at.add(0, 3, 0), 4, 12);
		EarthGolem titan = EarthGolem.raise(p, ground != null ? ground : at, true, TITAN_TICKS);
		TITANS.put(p.getUUID(), titan);
		SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.EARTH_BURST, titan.position().add(0, 1, 0), Vec3.ZERO, 5.0f, Block.getId(groundUnder(p)), 192);
		SpellFx.send(HeavenSpells.level(p), SpellFx.Kind.SHOCKWAVE, titan.position(), Vec3.ZERO, 12, 0, 192);
		HeavenSpells.sound(HeavenSpells.level(p), titan.position(), SoundEvents.WARDEN_EMERGE, 3.0f, 0.4f);
		HeavenSpells.sound(HeavenSpells.level(p), titan.position(), SoundEvents.IRON_GOLEM_REPAIR, 3.0f, 0.3f);
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

		Tomb(LivingEntity caster, Spell spell) {
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
			Devastation.fissures(caster, level, at, TOMB_FISSURES, 14, 6, TOMB_RADIUS + 1);
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

	// --- Dao Integration ---

	/**
	 * Qilin Tramples the Land: a qilin of rock, molten gold in its cracks (SpellConstruct QILIN, its own GeckoLib model), rears beside its
	 * caster and stamps (a crater, and harm, round it), then gallops along their look, over the land, {@link #QILIN_RUN} blocks in
	 * {@link #QILIN_GALLOP}: every {@link #QILIN_STRIDE} blocks a stride cracks a crater in the land, and everything it tramples (within
	 * {@link #QILIN_PATH} of it) takes {@link #QILIN_DAMAGE} (once) and is thrown high. Then it fades.
	 */
	public static final class Qilin extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, way;
		/** The land along its run, evened (its hooves keep to it). */
		private final double[] floor;
		private final SpellConstruct qilin;
		private final java.util.Set<LivingEntity> trampled = new java.util.HashSet<>();
		/** How far along its run the next stride's crater falls. */
		private double stride = QILIN_STRIDE;

		Qilin(LivingEntity caster, Spell spell) {
			super(caster, QILIN_REAR + QILIN_GALLOP + QILIN_FADE);
			this.spell = spell;
			this.way = Vec3.directionFromRotation(0, caster.getYRot());
			Vec3 front = caster.position().add(way.scale(QILIN_AHEAD));
			this.floor = HeavenSpells.evened(HeavenSpells.floorAlong(level, front, way, (int) QILIN_RUN + 4), 2);
			this.from = new Vec3(front.x, floor[0], front.z);
			this.qilin = SpellConstruct.create(level, SpellConstruct.Kind.QILIN, from, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			qilin.act("rear");
			HeavenSpells.sound(level, from, SoundEvents.HORSE_ANGRY, 3.0f, 0.5f);
			HeavenSpells.sound(level, from, SoundEvents.RAVAGER_ROAR, 2.0f, 1.3f);
		}

		/** Where it stands {@code d} blocks along its run. */
		private Vec3 at(double d) {
			return new Vec3(from.x + way.x * d, HeavenSpells.along(floor, d), from.z + way.z * d);
		}

		/** A stride's crater at {@code where}, and the earth's burst and the ground's shock round it. */
		private void crack(Vec3 where, double radius) {
			Devastation.crater(caster, level, where, radius, QILIN_CRATER_DEPTH, Devastation.Floor.RUBBLE);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, where, Vec3.ZERO, (float) radius, Block.getId(groundBelowState(level, where)), 160);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, where, Vec3.ZERO, (float) radius * 1.6f, 0, 160);
			HeavenSpells.sound(level, where, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.6f);
		}

		/** Whatever is underfoot within {@code reach} of {@code where} is trampled: struck, and thrown high and on. */
		private void trample(Vec3 where, double reach) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, where, reach + 10, e -> !trampled.contains(e) && !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - where.x, e.getZ() - where.z) > reach || e.getY() > where.y + 12 || e.getY() < where.y - 4) continue;
				trampled.add(e);
				HeavenSpells.strike(caster, e, QILIN_DAMAGE, spell);
				if (!HeavenSpells.greatBeast(e)) {
					HeavenSpells.move(e, new Vec3(way.x * 0.8, 1.7, way.z * 0.8));
					HeavenSpells.safeFall(e, 80);
				}
				SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, HeavenSpells.middle(e), Vec3.ZERO, 1.4f, 0);
			}
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == QILIN_STAMP) {
				// The stamp as it comes down out of its rearing.
				crack(from.add(way.scale(QILIN_FOREFEET)), QILIN_CRATER * 1.3);
				trample(from.add(way.scale(QILIN_FOREFEET)), QILIN_PATH * 1.5);
				HeavenSpells.sound(level, from, SoundEvents.HORSE_STEP, 3.0f, 0.4f);
			}
			if (age < QILIN_REAR) return true;
			double d = Math.min(QILIN_RUN, (age - QILIN_REAR) / (double) QILIN_GALLOP * QILIN_RUN);
			Vec3 here = at(d);
			float[] face = HeavenSpells.facing(at(d + 2).subtract(here));
			qilin.place(here, face[0], face[1] * 0.5f);
			if (d >= QILIN_RUN) return true;
			trample(here, QILIN_PATH);
			while (stride <= d) {
				crack(at(stride), QILIN_CRATER);
				stride += QILIN_STRIDE;
			}
			if (age % 4 == 0) SpellFx.send(level, SpellFx.Kind.DUST, here, Vec3.ZERO, 2.0f, Block.getId(groundBelowState(level, here)));
			if (age % 6 == 0) HeavenSpells.sound(level, here, SoundEvents.HORSE_GALLOP, 3.0f, 0.5f);
			return true;
		}

		@Override
		public void end() {
			qilin.dissolve();
		}
	}

	/** The block underfoot at {@code at} (for the colour of what bursts from it), or dirt. */
	private static BlockState groundBelowState(ServerLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at.x, at.y - 0.5, at.z);
		BlockState state = level.isLoaded(pos) ? level.getBlockState(pos) : Blocks.DIRT.defaultBlockState();
		return state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
	}

	/**
	 * Seal of Ten Thousand Mountains: an imperial seal of carved jade crowned with mountains (SpellConstruct MOUNTAIN_SEAL) falls out of
	 * the sky onto where its caster looks ({@link #MOUNTAINS_FALL} ticks, from {@link #MOUNTAINS_HIGH} up) and stamps: everything within
	 * {@link #MOUNTAINS_RADIUS} takes {@link #MOUNTAINS_DAMAGE} and is held pressed to the ground {@link #MOUNTAINS_HOLD}; the land round
	 * it sinks under its weight and splits in fissures. It stands there a while, then fades.
	 */
	public static final class Mountains extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final float yaw;
		private final SpellConstruct seal;
		private final java.util.Set<LivingEntity> pressed = new java.util.HashSet<>();

		Mountains(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, MOUNTAINS_FALL + MOUNTAINS_STAND);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.seal = SpellConstruct.create(level, SpellConstruct.Kind.MOUNTAIN_SEAL, ground.add(0, MOUNTAINS_HIGH, 0), yaw, 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, ground.add(0, MOUNTAINS_HIGH, 0), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.3f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ANVIL_PLACE, 1.5f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < MOUNTAINS_FALL) {
				double f = age / (double) MOUNTAINS_FALL;
				seal.place(ground.add(0, MOUNTAINS_HIGH * (1 - f * f), 0), yaw, 0);
				if (age % 6 == 0) SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) MOUNTAINS_RADIUS * (float) f, Block.getId(groundBelowState(level, ground)));
				return true;
			}
			if (age == MOUNTAINS_FALL) {
				seal.place(ground, yaw, 0);
				seal.act("stamp");
				for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, MOUNTAINS_RADIUS + 14, e -> !HeavenSpells.spared(caster, e))) {
					if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > MOUNTAINS_RADIUS || e.getY() > ground.y + 14 || e.getY() < ground.y - 4) continue;
					HeavenSpells.strike(caster, e, MOUNTAINS_DAMAGE, spell);
					pressed.add(e);
				}
				Devastation.subside(caster, level, ground, MOUNTAINS_SINK, MOUNTAINS_DROP, 0);
				Devastation.fissures(caster, level, ground, MOUNTAINS_FISSURE_REACH, MOUNTAINS_FISSURES, MOUNTAINS_FISSURE_DEPTH, MOUNTAINS_RADIUS * 0.9);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) MOUNTAINS_FISSURE_REACH, 0, 192);
				SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, (float) MOUNTAINS_RADIUS, Block.getId(groundBelowState(level, ground)), 192);
				HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.4f);
				HeavenSpells.sound(level, ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			}
			// The crushed held pressed to the ground under it.
			if (age > MOUNTAINS_FALL && age <= MOUNTAINS_FALL + MOUNTAINS_HOLD) {
				for (java.util.Iterator<LivingEntity> it = pressed.iterator(); it.hasNext(); ) {
					LivingEntity e = it.next();
					if (!HeavenSpells.present(e, level)) {
						it.remove();
						continue;
					}
					// A great beast it only slows (HeavenSpells#greatBeast); the rest it pins.
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
					if (!HeavenSpells.greatBeast(e)) HeavenSpells.move(e, new Vec3(0, Math.min(e.getDeltaMovement().y, -0.5), 0));
				}
				if (age == MOUNTAINS_FALL + MOUNTAINS_HOLD) for (LivingEntity e : pressed) HeavenSpells.safeFall(e, 20);
			}
			return true;
		}

		@Override
		public void end() {
			seal.dissolve();
		}
	}

	// --- Void Shattering ---

	/** The broken pillar's length (59 by its realm: its model is 128 pixels long, SpellConstruct#sizedFor). */
	public static final double BUZHOU_LENGTH = HeavenSpells.span(Realm.VOID_SHATTERING, 32);

	/**
	 * Buzhou Falls: the sky tears open {@link #BUZHOU_HIGH} over where its caster looks (SpellConstruct VOID_RIFT, the earth's amber), and out
	 * of it falls the broken pillar of Mount Buzhou, that once held up heaven (BUZHOU_PILLAR, its own GeckoLib model), {@link #BUZHOU_FALL}
	 * ticks: everything within {@link #BUZHOU_RADIUS} takes {@link #BUZHOU_DAMAGE} and is pinned to the ground {@link #BUZHOU_HOLD}; the land
	 * is smashed into a crater, fissures running out from it. The pillar stands in it a while, then fades.
	 */
	public static final class Buzhou extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, mouth;
		private final float yaw;
		private final SpellConstruct rift;
		private SpellConstruct pillar;
		private final java.util.Set<LivingEntity> pinned = new java.util.HashSet<>();
		private final int landsAt = BUZHOU_OPENS + BUZHOU_FALL;

		Buzhou(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, BUZHOU_OPENS + BUZHOU_FALL + BUZHOU_STAND);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.mouth = ground.add(0, BUZHOU_HIGH, 0);
			this.yaw = caster.getYRot();
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.VOID_RIFT, mouth, 0, 0, BUZHOU_OPENS + 12 + SpellConstruct.FADE, caster).size((float) BUZHOU_RIFT)
					.tint(EARTH_AMBER).spawn();
			HeavenSpells.sound(level, mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.5f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.ANVIL_PLACE, 1.5f, 0.3f);
		}

		/** Where the pillar's foot is {@code falling} ticks into its fall: its middle at the rift as it comes, its foot sunk in the land at the end. */
		private Vec3 foot(double falling) {
			double f = Math.min(1, falling / BUZHOU_FALL), top = mouth.y - BUZHOU_LENGTH / 2, rest = ground.y - BUZHOU_CRATER_DEPTH * 0.5;
			return new Vec3(ground.x, top + (rest - top) * f * f, ground.z);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < BUZHOU_OPENS) return true;
			long falling = age - BUZHOU_OPENS;
			if (falling == 0) {
				pillar = SpellConstruct.create(level, SpellConstruct.Kind.BUZHOU_PILLAR, foot(0), yaw, 0, BUZHOU_FALL + BUZHOU_STAND, caster).sizedFor(spell).spawn();
				SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 8, EARTH_AMBER, 192);
				HeavenSpells.sound(level, mouth, SoundEvents.WITHER_BREAK_BLOCK, 4.0f, 0.4f);
			}
			if (falling == 12) rift.dissolve(); // through, the sky closes behind it
			if (falling <= BUZHOU_FALL) {
				pillar.place(foot(falling), yaw, 0);
				if (falling % 5 == 0) SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) (BUZHOU_RADIUS * falling / BUZHOU_FALL), Block.getId(groundBelowState(level, ground)));
			}
			if (age == landsAt) land();
			// The crushed held pinned to the ground under it.
			if (age > landsAt && age <= landsAt + BUZHOU_HOLD) {
				for (java.util.Iterator<LivingEntity> it = pinned.iterator(); it.hasNext(); ) {
					LivingEntity e = it.next();
					if (!HeavenSpells.present(e, level)) {
						it.remove();
						continue;
					}
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
					if (!HeavenSpells.greatBeast(e)) HeavenSpells.move(e, new Vec3(0, Math.min(e.getDeltaMovement().y, -0.5), 0));
				}
				if (age == landsAt + BUZHOU_HOLD) for (LivingEntity e : pinned) HeavenSpells.safeFall(e, 20);
			}
			return true;
		}

		/** It lands: everything near crushed and pinned, the land smashed into a crater and split by fissures. */
		private void land() {
			pillar.act("impact");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, BUZHOU_RADIUS + 14, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > BUZHOU_RADIUS || e.getY() > ground.y + 16 || e.getY() < ground.y - 6) continue;
				HeavenSpells.strike(caster, e, BUZHOU_DAMAGE, spell);
				pinned.add(e);
			}
			Devastation.crater(caster, level, ground, BUZHOU_CRATER, BUZHOU_CRATER_DEPTH, Devastation.Floor.RUBBLE);
			Devastation.fissures(caster, level, ground, BUZHOU_FISSURE_REACH, BUZHOU_FISSURES, BUZHOU_FISSURE_DEPTH, BUZHOU_CRATER * 0.9);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) BUZHOU_FISSURE_REACH * 0.8f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, (float) BUZHOU_RADIUS, Block.getId(groundBelowState(level, ground)), 192);
			SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) BUZHOU_RADIUS * 1.5f, Block.getId(groundBelowState(level, ground)), 192);
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.3f);
			HeavenSpells.sound(level, ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			HeavenSpells.sound(level, ground, SoundEvents.WITHER_BREAK_BLOCK, 4.0f, 0.5f);
		}

		@Override
		public void end() {
			rift.dissolve();
			if (pillar != null) pillar.dissolve();
		}
	}

	/**
	 * Earth Veins Lock the Void: the earth itself heaves up round where its caster stands, ribs of rock bursting out of the land and bending in
	 * to meet overhead in a dome {@link #LOCK_RADIUS} round (SpellConstruct EARTH_VEIN_LOCK, its own GeckoLib model: the earth's molten gold in
	 * its cracks, boulders torn up drifting inside it, the earth trigram ☷ burning under its keystone), {@link #LOCK_RISES} ticks; for
	 * {@link #LOCK_TICKS} space within it is locked: every foe in it, or that comes in, is held where it stands (WoodSpells#bind: it can't walk,
	 * fly or be thrown out), crushed for {@link #LOCK_DAMAGE} a second, and can't step through space ({@link #spaceLocked}: Nine Nether Step, a
	 * Spatial Tear). Then the veins sink back into the land, which is left as it was.
	 */
	public static final class Lock extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final SpellConstruct dome;
		private final java.util.Set<LivingEntity> held = new java.util.HashSet<>();

		Lock(LivingEntity caster, Spell spell) {
			super(caster, LOCK_RISES + LOCK_TICKS + 4);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, caster.position().add(0, 1, 0), 2, 16);
			this.at = below != null ? below : caster.position();
			this.dome = SpellConstruct.create(level, SpellConstruct.Kind.EARTH_VEIN_LOCK, at, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, (float) LOCK_RADIUS * 0.5f, Block.getId(groundBelowState(level, at)), 160);
			HeavenSpells.sound(level, at, SoundEvents.WARDEN_EMERGE, 3.0f, 0.6f);
			HeavenSpells.sound(level, at, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		}

		/** Whether it is locking now (risen, not yet sinking away). */
		private boolean locking() {
			long age = level.getGameTime() - start;
			return age >= LOCK_RISES && age < LOCK_RISES + LOCK_TICKS;
		}

		/** Whether {@code e} is in its grip now: within it, not its caster's own. */
		boolean grips(LivingEntity e) {
			return locking() && e.level() == level && e.position().distanceTo(at) <= LOCK_RADIUS && !HeavenSpells.friendly(caster, e) && !HeavenSpells.spared(caster, e);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == LOCK_RISES + LOCK_TICKS - LOCK_RELEASE) {
				dome.act("release");
				HeavenSpells.sound(level, at, SoundEvents.BEACON_DEACTIVATE, 3.0f, 0.5f);
			}
			if (age < LOCK_RISES || age >= LOCK_RISES + LOCK_TICKS) return true;
			long locking = age - LOCK_RISES;
			// Whoever is in it, or comes in, is held where it stands till the lock lifts.
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, LOCK_RADIUS, e -> !held.contains(e) && grips(e))) {
				held.add(e);
				WoodSpells.bind(caster, e, (int) (LOCK_TICKS - locking));
				SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
				HeavenSpells.sound(level, e.position(), SoundEvents.CHAIN_PLACE, 2.0f, 0.5f);
			}
			if (locking % 20 == 10) {
				for (LivingEntity e : held) {
					if (!HeavenSpells.present(e, level)) continue;
					HeavenSpells.strikeAgain(caster, e, LOCK_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, HeavenSpells.middle(e), Vec3.ZERO, 1.0f, 0);
				}
				HeavenSpells.sound(level, at, SoundEvents.ANVIL_LAND, 2.0f, 0.4f);
			}
			return true;
		}

		@Override
		public void end() {
			dome.dissolve();
		}
	}

	/**
	 * Whether space is locked where {@code e} stands (Earth Veins Lock the Void, of a caster whose foe it is): it can't step through space
	 * (Nine Nether Step, a Spatial Tear: Spells#cast, SpatialTrialHandler#tear).
	 */
	public static boolean spaceLocked(LivingEntity e) {
		for (Lock lock : HeavenSpells.lingering(null, Lock.class)) if (lock.grips(e)) return true;
		return false;
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
