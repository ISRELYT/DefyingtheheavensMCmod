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
	/** How far behind the surge's front its ridge of spikes bursts up: behind the earth dragon's last coil, in its wake. */
	public static final float SURGE_WAKE = 15;
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
	/**
	 * Falling Mountain's peak: how high it falls from, how wide its foot is (blocks), how long it falls and how long it is driven down after (ticks).
	 * From high in the sky, a speck growing into a mountain (the user's ask, 2026-10-26: "make it fall from higher up ... doesnt mean it has to fall
	 * faster"): from 46 blocks over 34 ticks it was 100 over 64, coming down no faster than it did.
	 */
	public static final double MOUNTAIN_HIGH = 100, MOUNTAIN_PEAK = 11;
	public static final int MOUNTAIN_FALLS = 64, MOUNTAIN_AFTER = 34;
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
	/**
	 * Xirang, the Self-Growing Soil: where (up to {@link #XIRANG_RANGE}, 136 by its realm), how far round its mound heaves up and buries
	 * (17) and how high it stands (13, by its realm); its harm a second while it holds them, and as it settles; how long its clod flies, how
	 * long the mound swells, and how long it holds what it has buried (ticks).
	 */
	public static final double XIRANG_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), XIRANG_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 8),
			XIRANG_HEIGHT = Devastation.sized(Realm.SACRED_VESSEL, 6);
	public static final float XIRANG_DAMAGE = 500, XIRANG_SETTLE = 600;
	public static final int XIRANG_FLIES = 14, XIRANG_SWELLS = 30, XIRANG_HOLDS = 4 * 20;
	/**
	 * Nine Cauldrons Suppress the Land: where (up to {@link #CAULDRONS_RANGE}, 136), how far round its ring of eight stands (30) and how high
	 * they fall from (68); whom each strikes as it lands (within 11) and how hard; its press a second on every foe within the ring; each
	 * cauldron's crater (9 round, the heart's 13) and how far the land within the ring sinks (6), by its realm; how long each falls, how far
	 * apart they come, how long they suppress the land, and how long they take to rise away (ticks).
	 */
	public static final double CAULDRONS_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), CAULDRONS_RING = HeavenSpells.span(Realm.SACRED_VESSEL, 14),
			CAULDRONS_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 32), CAULDRON_STRIKE = HeavenSpells.span(Realm.SACRED_VESSEL, 5),
			CAULDRON_CRATER = Devastation.sized(Realm.SACRED_VESSEL, 4), CAULDRON_HEART_CRATER = Devastation.sized(Realm.SACRED_VESSEL, 6),
			CAULDRON_CRATER_DEPTH = Devastation.sized(Realm.SACRED_VESSEL, 2.5);
	public static final int CAULDRONS_SINK = (int) Math.round(Devastation.sized(Realm.SACRED_VESSEL, 3));
	public static final float CAULDRON_DAMAGE = 1500, CAULDRONS_PRESS = 250;
	public static final int CAULDRON_FALL = 16, CAULDRON_EVERY = 6, CAULDRONS_TICKS = 6 * 20, CAULDRONS_RISE = 16;
	/**
	 * Gravestone Falls: where (154 by its realm), how high it falls from (58); whom it strikes as it lands (within 10) and how hard, and how long
	 * they are pinned; how far round its sinking drags foes to it (24) and whom it buries as it goes under (within 14), and how hard; its crater
	 * (12 round, 6 deep), how far its cracks run (34) and the pit it leaves (10 round, 14 deep), by its realm; how long it falls, how long it
	 * stands, how long it sinks, and how long it has after (ticks).
	 */
	public static final double GRAVE_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), GRAVE_HIGH = HeavenSpells.span(Realm.ENTERING_NIRVANA, 24),
			GRAVE_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 4), GRAVE_PULL = HeavenSpells.span(Realm.ENTERING_NIRVANA, 10),
			GRAVE_BURY = HeavenSpells.span(Realm.ENTERING_NIRVANA, 6), GRAVE_CRATER = Devastation.sized(Realm.ENTERING_NIRVANA, 5),
			GRAVE_CRATER_DEPTH = Devastation.sized(Realm.ENTERING_NIRVANA, 2.5), GRAVE_CRACKS = Devastation.sized(Realm.ENTERING_NIRVANA, 14),
			GRAVE_PIT = Devastation.sized(Realm.ENTERING_NIRVANA, 4), GRAVE_PIT_DEPTH = Devastation.sized(Realm.ENTERING_NIRVANA, 6);
	public static final float GRAVE_DAMAGE = 3000, GRAVE_BURIES = 1500;
	public static final int GRAVE_FALL = 16, GRAVE_PINS = 3 * 20, GRAVE_STANDS = 40, GRAVE_SINKS = 40, GRAVE_AFTER = 10;
	/**
	 * Buried World: where (154 by its realm), how far round its ring of standing stones stands (34); how many stones; how far the land within
	 * sinks each second it sinks (4, by its realm), and what it crushes every foe within a second; the stones' fall, its harm; how long they
	 * rise, how long the land sinks, how long the stones take to fall, and how long it has after (ticks).
	 */
	public static final double BURIED_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 64), BURIED_RADIUS = HeavenSpells.span(Realm.ENTERING_NIRVANA, 14);
	public static final int BURIED_STONES = 8, BURIED_SINK = (int) Math.round(Devastation.sized(Realm.ENTERING_NIRVANA, 1.6));
	public static final float BURIED_CRUSH = 500, BURIED_TOPPLE = 3500;
	public static final int BURIED_RISES = 24, BURIED_TICKS = 6 * 20, BURIED_FALLS = 20, BURIED_AFTER = 20;
	/**
	 * Hand of Houtu: where it rises (218 by its realm), how far round its hand grasps (17), how high its fist holds what it grasped (15: its
	 * model's), how far round its fist strikes where it slams down (20); the crater it leaves there (20 round, 9 deep, by its realm) and how far
	 * its cracks run (61); its harm as it grasps, and as it slams; how long it rises, how long it takes to close, how long it holds, how long its
	 * fist takes to come down, and how long it has after (ticks).
	 */
	public static final double HOUTU_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), HOUTU_GRASP = HeavenSpells.span(Realm.TRUE_IMMORTAL, 5),
			HOUTU_FIST = HeavenSpells.span(Realm.TRUE_IMMORTAL, 4.3), HOUTU_SLAM_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 6),
			HOUTU_CRATER = Devastation.sized(Realm.TRUE_IMMORTAL, 6), HOUTU_CRATER_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 2.5),
			HOUTU_CRACKS = Devastation.sized(Realm.TRUE_IMMORTAL, 18);
	public static final float HOUTU_CRUSH = 15000, HOUTU_SLAM = 45000;
	public static final int HOUTU_RISES = 20, HOUTU_GRASPS = 10, HOUTU_HOLDS = 30, HOUTU_SLAMS = 7, HOUTU_AFTER = 30;
	/**
	 * Falling Continent: where (218 by its realm), how high it falls from (136), how high over the land its heart is as it strikes (10), how far
	 * round it strikes (41); the crater it smashes (31 round, 12 deep, by its realm) and how far its cracks run (82); its harm; how long it falls,
	 * and how long it lies there after (ticks).
	 */
	public static final double CONTINENT_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 64), CONTINENT_HIGH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 40),
			CONTINENT_LANDS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 3), CONTINENT_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 12),
			CONTINENT_CRATER = Devastation.sized(Realm.TRUE_IMMORTAL, 9), CONTINENT_CRATER_DEPTH = Devastation.sized(Realm.TRUE_IMMORTAL, 3.5),
			CONTINENT_CRACKS = Devastation.sized(Realm.TRUE_IMMORTAL, 24);
	public static final float CONTINENT_DAMAGE = 75000;
	public static final int CONTINENT_FALLS = 72, CONTINENT_AFTER = 50;
	/** How wide the land itself is beside the reach of its blow (its round, as a share of {@link #CONTINENT_RADIUS}). */
	public static final double CONTINENT_LAND = 0.9;

	/** Each caster's golems, oldest first. */
	private static final Map<UUID, List<EarthGolem>> GOLEMS_OF = new HashMap<>();
	private static final Map<UUID, EarthGolem> TITANS = new HashMap<>();

	static boolean canCast(LivingEntity p, Spell spell) {
		return switch (spell) {
			case DIRT_TIDE -> HeavenSpells.groundBelow(HeavenSpells.level(p), p.position(), 0, 3) != null || HeavenSpells.refuse(p);
			case BOULDER_THROW -> crater(p, false).size() >= 4 || noEarth(p);
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
			case FALLING_MOUNTAIN -> {
				LivingEntity aimed = HeavenSpells.aimed(p, MOUNTAIN_RANGE);
				HeavenSpells.linger(new Peak(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, MOUNTAIN_RANGE)));
			}
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
			case XIRANG_SELF_GROWING_SOIL -> {
				LivingEntity aimed = HeavenSpells.aimed(p, XIRANG_RANGE);
				HeavenSpells.linger(new Xirang(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, XIRANG_RANGE)));
			}
			case NINE_CAULDRONS_SUPPRESS_THE_LAND -> {
				LivingEntity aimed = HeavenSpells.aimed(p, CAULDRONS_RANGE);
				HeavenSpells.linger(new Cauldrons(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, CAULDRONS_RANGE)));
			}
			case GRAVESTONE_FALLS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, GRAVE_RANGE);
				HeavenSpells.linger(new Gravestone(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, GRAVE_RANGE)));
			}
			case BURIED_WORLD -> {
				HeavenSpells.endLingering(p.getUUID(), Buried.class);
				LivingEntity aimed = HeavenSpells.aimed(p, BURIED_RANGE);
				HeavenSpells.linger(new Buried(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, BURIED_RANGE)));
			}
			case HAND_OF_HOUTU -> {
				LivingEntity aimed = HeavenSpells.aimed(p, HOUTU_RANGE);
				HeavenSpells.linger(new Houtu(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, HOUTU_RANGE)));
			}
			case FALLING_CONTINENT -> {
				HeavenSpells.endLingering(p.getUUID(), Continent.class);
				LivingEntity aimed = HeavenSpells.aimed(p, CONTINENT_RANGE);
				HeavenSpells.linger(new Continent(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, CONTINENT_RANGE)));
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
			sound(Vec3.atCenterOf(front), SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.5f);
			sound(Vec3.atCenterOf(front), SoundEvents.STONE_BREAK, 1.2f, 0.4f);
			SpellFx.send(level, SpellFx.Kind.DUST, Vec3.atBottomCenterOf(front), Vec3.ZERO, 2.0f, Block.getId(earth));
		}

		@Override
		protected boolean run(long age, long now) {
			for (Map.Entry<BlockPos, Integer> entry : blocks.entrySet()) {
				if (entry.getValue() != age) continue;
				BlockPos pos = entry.getKey();
				if (HeavenSpells.placeTemporary(level, pos, earth, WALL_LASTS + 40)) standing.add(pos);
			}
			if (age == WALL_RISE / 2) sound(caster.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.7f);
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
			if (!standing.isEmpty()) sound(Vec3.atCenterOf(standing.get(0)), SoundEvents.ROOTED_DIRT_BREAK, 1.2f, 0.5f);
		}
	}

	/**
	 * Where Boulder Throw tears its boulder from: a half-round hole {@link #CRATER} round in the ground two blocks ahead, natural or
	 * built (whatever a spell may carve: HeavenSpells#carvable); {@code taking} it (else only weighed: HeavenSpells#mayCarve). With the spells'
	 * griefing off, the ground it would take, griefing aside (HeavenSpells#couldCarve): the boulder is torn out of it all the same and it is
	 * left whole.
	 */
	static List<BlockPos> crater(LivingEntity p, boolean taking) {
		boolean lifts = HeavenSpells.carvesLand(p);
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
					if (!lifts ? HeavenSpells.couldCarve(level, pos, p) : taking ? HeavenSpells.carvable(level, pos, p) : HeavenSpells.mayCarve(level, pos, p)) hole.add(pos);
				}
			}
		}
		return hole;
	}

	private static void boulder(LivingEntity p, Spell spell) {
		ServerLevel level = HeavenSpells.level(p);
		boolean lifts = HeavenSpells.carvesLand(p);
		List<BlockPos> hole = crater(p, true);
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
			// Griefing off, the ground stays and its clods rise up out of it.
			if (lifts) level.removeBlock(pos, false);
			Vec3 from = Vec3.atBottomCenterOf(pos).add(0, lifts ? 0 : 0.7, 0);
			SpellBlock.show(level, state, from, gather.subtract(from).subtract(0, 0.5, 0), (i++ % 6), 12, SpellBlock.Motion.FLY, 0).scale(0.7f);
		}
		SpellFx.send(level, SpellFx.Kind.EARTH_BURST, centre, Vec3.ZERO, 1.5f, Block.getId(most));
		HeavenSpells.sound(level, centre, SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.5f);
		HeavenSpells.sound(level, centre, SoundEvents.STONE_BREAK, 1.5f, 0.5f);
		int stateId = Block.getId(most);
		// The boulder itself hangs at the gathering point while it forms (drawn as a field until it is thrown, and gone as it is).
		SpellField forming = SpellField.create(level, SpellField.Kind.BOULDER_FORMING, gather, 1.0f, 1.0f, 19, p).data(stateId).fleeting().spawn();
		HeavenSpells.linger(new FireSpells.Delayed(p, 18, () -> {
			forming.discard();
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
		// Weighed only: each is answered for as the carving takes it (Carving).
		for (BlockPos pos : carve) if (!real.contains(pos) && HeavenSpells.mayCarve(level, pos, p)) real.add(pos);
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
				if (i % 3 == 0) sound(Vec3.atCenterOf(pos), state.getSoundType().getBreakSound(), 0.8f, 0.7f);
			}
			return age < CAVE_TICKS;
		}
	}

	// --- Core Formation ---

	/**
	 * Earth Dragon Surge: an earth dragon swims through the land from its caster toward where they look, {@link #SURGE_PACE} a tick for
	 * up to {@link #SURGE_RANGE}; whatever its head passes over takes {@link #SURGE_DAMAGE} and is thrown up. A ridge of jagged spikes
	 * bursts up in its wake ({@link #SURGE_WAKE} behind), sinking back a few seconds later (SpellField EARTH_SPIKES: drawn along the
	 * ground the client sees).
	 */
	public static final class Surge extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, way;
		private final double length;
		private final SpellField field;
		/** The earth dragon swimming through the land at its front (SpellConstruct EARTH_DRAGON), until it dives at the ridge's end. */
		private final SpellConstruct dragon;
		private final float yaw;
		private long dived = -1;
		private final java.util.Set<LivingEntity> struck = new java.util.HashSet<>();

		Surge(LivingEntity caster, Spell spell) {
			super(caster, (int) (SURGE_RANGE / SURGE_PACE) + 70);
			this.spell = spell;
			this.from = HeavenSpells.footing(caster); // along the land, however high its caster is
			Vec3 look = caster.getViewVector(1.0f).multiply(1, 0, 1);
			this.way = look.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, caster.getYRot()) : look.normalize();
			this.length = SURGE_RANGE;
			this.field = SpellField.create(level, SpellField.Kind.EARTH_SPIKES, from, (float) SURGE_RANGE, SURGE_WAKE, (int) (end - start), caster)
					.facing(caster.getYRot(), 0).data(Block.getId(groundUnder(caster))).value(0).spawn();
			this.yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
			this.dragon = SpellConstruct.create(level, SpellConstruct.Kind.EARTH_DRAGON, from, yaw, 0, (int) (SURGE_RANGE / SURGE_PACE) + 30, caster).sizedFor(spell).spawn();
			sound(from, SoundEvents.RAVAGER_ROAR, 1.2f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			double reach = Math.min(length, age * SURGE_PACE);
			field.value((float) (reach / length));
			if (dived < 0) {
				// The dragon at the ridge's front, on the land there; at its end it dives back into the land.
				Vec3 front = from.add(way.scale(reach)), ground = HeavenSpells.groundBelow(level, front.add(0, 6, 0), 6, 24);
				dragon.place(ground != null ? ground : front, yaw, 0);
				if (reach >= length) {
					dragon.act("dive");
					dived = age;
				}
			} else if (age == dived + 12) {
				dragon.dissolve();
			}
			if (reach < length) {
				Vec3 front = from.add(way.scale(reach));
				for (LivingEntity e : HeavenSpells.aroundPoint(level, front, 2.5, e -> !HeavenSpells.spared(caster, e) && !struck.contains(e))) {
					if (Math.abs(e.getY() - front.y) > 4) continue;
					struck.add(e);
					HeavenSpells.strike(caster, e, SURGE_DAMAGE, spell);
					HeavenSpells.move(e, new Vec3(way.x * 0.4, 1.0, way.z * 0.4));
				}
				if (age % 3 == 0) sound(front, SoundEvents.ROOTED_DIRT_BREAK, 1.0f, 0.6f + level.random.nextFloat() * 0.3f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
			dragon.dissolve();
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
			sound(caster.position(), SoundEvents.WARDEN_EMERGE, 2.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 10 == 5) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, field.position(), QUAKE_RADIUS, e -> HeavenSpells.mark(caster, e))) {
					if (!e.onGround()) continue;
					HeavenSpells.strikeAgain(caster, e, QUAKE_DAMAGE / 2, spell);
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
					if (age % 20 == 5) HeavenSpells.move(e, e.getDeltaMovement().add((level.random.nextDouble() - 0.5) * 0.4, 0.45, (level.random.nextDouble() - 0.5) * 0.4));
				}
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, field.position(), Vec3.ZERO, (float) QUAKE_RADIUS, 0);
				sound(field.position(), SoundEvents.GENERIC_EXPLODE, 1.0f, 0.4f);
			}
			return true;
		}

		@Override
		public void end() {
			field.discard();
		}
	}

	// --- Heavenly Being ---

	/**
	 * Falling Mountain: a mountain torn up out of a range, made of the world's own blocks as Falling Continent's land is (SpellConstruct PEAK_MASS:
	 * EarthMasses; the user's ask, 2026-10-25: "rework falling mountain, to look more like falling continent"), snow on its top and spruce on its
	 * slopes, the rock it was torn from hanging jagged under it, comes down out of the sky from {@link #MOUNTAIN_HIGH} onto where its caster looks
	 * ({@link #MOUNTAIN_FALLS}), slowly and then ever faster, tumbling a little, rock and earth breaking off its rim and raining down ahead of it,
	 * its shadow on the land and the dust rising to meet it. It strikes: {@link #MOUNTAIN_DAMAGE} to everything within {@link #MOUNTAIN_RADIUS},
	 * thrown out, the land smashed into a crater with fissures running out of it; it is driven down into what it smashed and is gone.
	 */
	public static final class Peak extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final float yaw;
		private final SpellConstruct peak;
		private final SpellField mark;
		private final int stateId;
		private boolean struck;
		/** The wind roaring past it as it falls (a voice of its spell's, hushed as it strikes). */
		private int wind = -1;

		Peak(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, MOUNTAIN_FALLS + MOUNTAIN_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 32);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.stateId = Block.getId(groundUnder(caster));
			this.mark = SpellField.create(level, SpellField.Kind.METEOR_MARK, ground, (float) MOUNTAIN_RADIUS, 1, MOUNTAIN_FALLS + 2, caster).data(1).spawn();
			this.peak = SpellConstruct.create(level, SpellConstruct.Kind.PEAK_MASS, at(0), yaw, 0, (int) (end - start) + SpellConstruct.FADE, caster)
					.size((float) MOUNTAIN_PEAK).tint(level.random.nextInt()).spawn();
			sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 4.0f, 0.3f);
			sound(ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0f, 0.6f);
			this.wind = loop(peak, SoundEvents.ELYTRA_FLYING, 3.5f, 0.5f);
		}

		/** Where its foot is {@code age} ticks into its fall: high over the spot, coming down ever faster. */
		private Vec3 at(double age) {
			double f = Math.min(1, age / MOUNTAIN_FALLS);
			return ground.add(0, MOUNTAIN_HIGH * (1 - Math.pow(f, 2.2)), 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= MOUNTAIN_FALLS) {
				double f = age / (double) MOUNTAIN_FALLS;
				Vec3 here = at(age);
				peak.place(here, yaw + 25 * (float) f, 0);
				// Rock and earth breaking off its rim, raining down ahead of it.
				if (age % 3 == 1 && f < 0.9) {
					double a = level.random.nextDouble() * Math.PI * 2, out = MOUNTAIN_PEAK * (0.5 + 0.45 * level.random.nextDouble());
					Vec3 from = here.add(Math.cos(a) * out, -MOUNTAIN_PEAK * 0.3 * level.random.nextDouble(), Math.sin(a) * out);
					double drop = Math.max(4, from.y - ground.y);
					BlockState rock = (level.random.nextInt(3) == 0 ? Blocks.DIRT : level.random.nextInt(2) == 0 ? Blocks.STONE : Blocks.COBBLESTONE).defaultBlockState();
					SpellBlock.show(level, rock, from, new Vec3(Math.cos(a) * 2.5, -drop, Math.sin(a) * 2.5), 0, (int) Math.min(30, 8 + drop / 3), SpellBlock.Motion.FLY, 0)
							.scale(1.0f + level.random.nextFloat());
				}
				if (age % 5 == 0) SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) (MOUNTAIN_RADIUS * (0.4 + f)), stateId, 192);
				if (age % 10 == 0) sound(here, SoundEvents.GRAVEL_BREAK, 3.0f, 0.4f);
				// Its weight on everything under it, heavier as it comes: unseen (its swirls, given anew every tick to all under it, rose round them as
				// a purple mist: the user's report, 2026-10-26).
				if (f > 0.4) {
					for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, MOUNTAIN_RADIUS, 6, MOUNTAIN_HIGH)) {
						e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2, false, false));
					}
				}
			}
			if (!struck && age >= MOUNTAIN_FALLS) {
				struck = true;
				peak.place(ground.add(0, -1, 0), peak.getYRot(), 0);
				peak.act("impact");
				mark.discard();
				if (wind >= 0) hush(wind, 4);
				slam(caster, spell, level, ground, MOUNTAIN_RADIUS, MOUNTAIN_DAMAGE, stateId);
				Devastation.crater(caster, level, ground, MOUNTAIN_CRATER, MOUNTAIN_CRATER_DEPTH, Devastation.Floor.RUBBLE);
				Devastation.fissures(caster, level, ground, MOUNTAIN_CRATER * 2.2, 10, 5, MOUNTAIN_CRATER);
				SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) MOUNTAIN_RADIUS * 2, stateId, 256);
				sound(ground, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.3f);
				sound(ground, SoundEvents.STONE_BREAK, 5.0f, 0.3f);
			}
			if (struck && age % 6 == 0 && age < MOUNTAIN_FALLS + 24) {
				SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) MOUNTAIN_RADIUS * 1.6f, stateId, 256);
				sound(ground, SoundEvents.GRAVEL_BREAK, 4.0f, 0.35f);
			}
			return true;
		}

		@Override
		public void end() {
			if (!mark.isRemoved()) mark.discard();
			peak.dissolve();
		}
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
			sound(caster.position(), SoundEvents.ANVIL_LAND, 2.0f, 0.3f);
			sound(caster.position(), SoundEvents.BEACON_DEACTIVATE, 2.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age % 5 == 0) {
				for (LivingEntity e : HeavenSpells.aroundPoint(level, field.position(), SEAL_RADIUS, e -> HeavenSpells.mark(caster, e))) {
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
		HeavenSpells.sound(HeavenSpells.level(p), titan, SoundEvents.WARDEN_EMERGE, 3.0f, 0.4f);
		HeavenSpells.sound(HeavenSpells.level(p), titan.position(), SoundEvents.IRON_GOLEM_REPAIR, 3.0f, 0.3f);
	}

	/**
	 * Mountain Seal: a mountain of stone erupts over the point looked at ({@link #TOMB_RADIUS} round, {@link #TOMB_HEIGHT} tall at its
	 * shoulders, a summit and four lesser peaks standing over them): everything not spared inside is entombed for {@link #TOMB_TICKS}, held,
	 * crushed ({@link #TOMB_DAMAGE} of earth a second). The mountain is the spell's (it gives nothing back when dug) and crumbles after
	 * {@link #TOMB_LASTS}. A talisman of yellow jade (SpellConstruct MOUNTAIN_TALISMAN) comes down over it as it rises, and seals it: its chains
	 * stake the mountain to the land round its foot until it crumbles.
	 */
	public static final class Tomb extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 at;
		private final List<LivingEntity> entombed = new ArrayList<>();
		private final List<BlockPos> blocks = new ArrayList<>();
		private final SpellField field;
		private final SpellConstruct talisman;
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
			// Unturned: its chains run down between the lesser peaks (height), at the corners.
			this.talisman = SpellConstruct.create(level, SpellConstruct.Kind.MOUNTAIN_TALISMAN, at, 0, 0, TOMB_LASTS + 12, caster).sizedFor(spell).spawn();
			sound(at, SoundEvents.WARDEN_EMERGE, 3.0f, 0.3f);
			sound(at, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.3f);
		}

		/**
		 * How many blocks tall the mountain stands {@code dx}, {@code dz} from its middle (none past its foot): its shoulders, a summit over them,
		 * four lesser peaks round it (along the axes: the talisman's chains run down between them), crags.
		 */
		private int height(int dx, int dz) {
			double r = Math.sqrt(dx * dx + dz * dz) / TOMB_RADIUS;
			if (r > 1) return 0;
			double h = TOMB_HEIGHT * 0.82 * (1 - r * r) + 3 * Math.exp(-(dx * dx + dz * dz) / 4.5);
			for (int k = 0; k < 4; k++) {
				double px = (k == 0 ? 1 : k == 2 ? -1 : 0) * TOMB_RADIUS * 0.55, pz = (k == 1 ? 1 : k == 3 ? -1 : 0) * TOMB_RADIUS * 0.55;
				h += 4 * Math.exp(-((dx - px) * (dx - px) + (dz - pz) * (dz - pz)) / 2.4) * (1 - r * r * 0.5);
			}
			return (int) Math.round(h + level.random.nextDouble() * 1.2);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == 12 && !risen) rise();
			if (age == TOMB_LASTS - 14) talisman.act("break");
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
					int top = height(dx, dz);
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
			sound(at, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.4f);
		}

		/** The mountain crumbles away: its stone falls apart from the top down. */
		@Override
		public void end() {
			field.discard();
			talisman.dissolve();
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
				sound(at, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.4f);
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
			sound(from, SoundEvents.HORSE_ANGRY, 3.0f, 0.5f);
			sound(from, SoundEvents.RAVAGER_ROAR, 2.0f, 1.3f);
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
			sound(where, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.6f);
		}

		/** Whatever is underfoot within {@code reach} of {@code where} is trampled: struck, and thrown high and on. */
		private void trample(Vec3 where, double reach) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, where, reach + 10, e -> !trampled.contains(e) && !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - where.x, e.getZ() - where.z) > reach || e.getY() > where.y + 12 || e.getY() < where.y - 4) continue;
				trampled.add(e);
				HeavenSpells.strike(caster, e, QILIN_DAMAGE, spell);
				if (!HeavenSpells.greatBeast(caster, e)) {
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
				sound(from, SoundEvents.HORSE_STEP, 3.0f, 0.4f);
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
			if (age % 6 == 0) sound(here, SoundEvents.HORSE_GALLOP, 3.0f, 0.5f);
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
			// Of the world's own blocks (SpellConstruct SEAL_MASS: EarthMasses; the user's ask, 2026-10-25: its model "looks a bit weird").
			this.seal = SpellConstruct.create(level, SpellConstruct.Kind.SEAL_MASS, ground.add(0, MOUNTAINS_HIGH, 0), yaw, 0, (int) (end - start), caster)
					.size((float) (MOUNTAINS_RADIUS * 0.55)).tint(level.random.nextInt()).spawn();
			sound(ground.add(0, MOUNTAINS_HIGH, 0), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.3f);
			sound(caster.position(), SoundEvents.ANVIL_PLACE, 1.5f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < MOUNTAINS_FALL) {
				double f = age / (double) MOUNTAINS_FALL;
				seal.place(ground.add(0, MOUNTAINS_HIGH * (1 - f * f), 0), yaw, 0);
				if (age % 6 == 0) SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) MOUNTAINS_RADIUS * (float) f, Block.getId(groundBelowState(level, ground)));
				if (age % 8 == 4) sound(ground.add(0, MOUNTAINS_HIGH * (1 - f * f), 0), SoundEvents.ANVIL_PLACE, 2.0f, 0.3f + 0.2f * (float) f);
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
				sound(ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.4f);
				sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
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
					if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(0, Math.min(e.getDeltaMovement().y, -0.5), 0));
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
			sound(mouth, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.5f);
			sound(caster.position(), SoundEvents.ANVIL_PLACE, 1.5f, 0.3f);
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
				// Of the world's own blocks (SpellConstruct PILLAR_MASS: EarthMasses; the user's ask, 2026-10-25: its model "looks a bit weird").
				pillar = SpellConstruct.create(level, SpellConstruct.Kind.PILLAR_MASS, foot(0), yaw, 0, BUZHOU_FALL + BUZHOU_STAND, caster).size((float) BUZHOU_LENGTH)
						.tint(level.random.nextInt()).spawn();
				SpellFx.send(level, SpellFx.Kind.FLASH, mouth, Vec3.ZERO, 8, EARTH_AMBER, 192);
				sound(mouth, SoundEvents.WITHER_BREAK_BLOCK, 4.0f, 0.4f);
			}
			if (falling == 12) rift.dissolve(); // through, the sky closes behind it
			if (falling <= BUZHOU_FALL) {
				pillar.place(foot(falling), yaw, 0);
				// Stone breaking off it as it plunges, raining down round where it will land.
				if (falling % 3 == 1 && falling < BUZHOU_FALL - 2) {
					double a = level.random.nextDouble() * Math.PI * 2;
					Vec3 from = foot(falling).add(Math.cos(a) * BUZHOU_LENGTH * 0.1, BUZHOU_LENGTH * (0.3 + 0.6 * level.random.nextDouble()), Math.sin(a) * BUZHOU_LENGTH * 0.1);
					SpellBlock.show(level, (level.random.nextInt(3) == 0 ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE).defaultBlockState(), from,
							new Vec3(Math.cos(a) * 5, ground.y - from.y, Math.sin(a) * 5), 0, 20, SpellBlock.Motion.FLY, 0).scale(1.2f + level.random.nextFloat());
				}
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
					if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(0, Math.min(e.getDeltaMovement().y, -0.5), 0));
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
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.3f);
			sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			sound(ground, SoundEvents.WITHER_BREAK_BLOCK, 4.0f, 0.5f);
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
			this.at = HeavenSpells.footing(caster);
			// Of the world's own rock, run through with gold and fire (SpellConstruct VEIN_DOME_MASS: EarthMasses; the user's ask, 2026-10-25).
			this.dome = SpellConstruct.create(level, SpellConstruct.Kind.VEIN_DOME_MASS, at, caster.getYRot(), 0, (int) (end - start), caster)
					.size((float) LOCK_RADIUS).tint(level.random.nextInt()).spawn();
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, (float) LOCK_RADIUS * 0.5f, Block.getId(groundBelowState(level, at)), 160);
			sound(at, SoundEvents.WARDEN_EMERGE, 3.0f, 0.6f);
			sound(at, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		}

		/** Whether it is locking now (risen, not yet sinking away). */
		private boolean locking() {
			long age = level.getGameTime() - start;
			return age >= LOCK_RISES && age < LOCK_RISES + LOCK_TICKS;
		}

		/** Whether {@code e} is in its grip now: within it, not its caster's own. */
		boolean grips(LivingEntity e) {
			return locking() && e.level() == level && e.position().distanceTo(at) <= LOCK_RADIUS && HeavenSpells.mark(caster, e);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age == LOCK_RISES + LOCK_TICKS - LOCK_RELEASE) {
				dome.act("release");
				sound(at, SoundEvents.BEACON_DEACTIVATE, 3.0f, 0.5f);
			}
			if (age < LOCK_RISES || age >= LOCK_RISES + LOCK_TICKS) return true;
			long locking = age - LOCK_RISES;
			// Whoever is in it, or comes in, is held where it stands till the lock lifts.
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, LOCK_RADIUS, e -> !held.contains(e) && grips(e))) {
				held.add(e);
				WoodSpells.bind(caster, e, (int) (LOCK_TICKS - locking));
				SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, HeavenSpells.middle(e), Vec3.ZERO, 1.2f, 0);
				sound(e.position(), SoundEvents.CHAIN_PLACE, 2.0f, 0.5f);
			}
			if (locking % 20 == 10) {
				for (LivingEntity e : held) {
					if (!HeavenSpells.present(e, level)) continue;
					HeavenSpells.strikeAgain(caster, e, LOCK_DAMAGE, spell);
					SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, HeavenSpells.middle(e), Vec3.ZERO, 1.0f, 0);
				}
				sound(at, SoundEvents.ANVIL_LAND, 2.0f, 0.4f);
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

	// --- Sacred Vessel ---

	/**
	 * Xirang, the Self-Growing Soil: a clod of the soil that grows of itself, that Gun stole from heaven to dam the flood (SpellConstruct XIRANG,
	 * its own GeckoLib model: dark living earth, gold root-veins, shoots), flies from its caster's hands to where they look
	 * ({@link #XIRANG_FLIES}) and swells ({@link #XIRANG_SWELLS}): the land there heaves up into a mound {@link #XIRANG_RADIUS} round and
	 * {@link #XIRANG_HEIGHT} high (Devastation#raise: stone at its heart, earth and grass over it; nothing laid round its caster), burying
	 * whatever stands there. Every foe within it is held fast, buried, for {@link #XIRANG_HOLDS} and crushed for {@link #XIRANG_DAMAGE} a second;
	 * then the mound settles on them, {@link #XIRANG_SETTLE} more. The mound stays.
	 */
	public static final class Xirang extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, ground;
		private final SpellConstruct clod;
		private final java.util.Set<LivingEntity> buried = new java.util.HashSet<>();
		/**
		 * What is seen of its soil growing (the user's report, 2026-10-26: "I'm just confused by what its doing"): its roots of gold and its shoots
		 * running out over the land from where its clod sank in (SpellConstruct XIRANG_BLOOM), and the earth heaved up round each being it buries
		 * (BURIAL_MASS, held on that being), whatever the spells' griefing.
		 */
		private SpellConstruct bloom;
		private final java.util.Map<LivingEntity, SpellConstruct> mounds = new java.util.HashMap<>();

		Xirang(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, XIRANG_FLIES + XIRANG_SWELLS + XIRANG_HOLDS + 12);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.from = HeavenSpells.fromHands(caster);
			// Of the world's own earth (SpellConstruct CLOD_MASS: EarthMasses; the user's ask, 2026-10-25).
			this.clod = SpellConstruct.create(level, SpellConstruct.Kind.CLOD_MASS, from, caster.getYRot(), 0, (int) (end - start), caster)
					.size((float) Math.max(2, XIRANG_RADIUS * 0.16)).tint(level.random.nextInt()).spawn();
			sound(from, SoundEvents.ROOTED_DIRT_PLACE, 2.0f, 0.5f);
			sound(from, SoundEvents.SNIFFER_EGG_CRACK, 2.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= XIRANG_FLIES) {
				// An arc to it, high over the land between.
				double f = age / (double) XIRANG_FLIES;
				Vec3 at = from.lerp(ground, f).add(0, Math.sin(Math.PI * f) * Math.min(24, from.distanceTo(ground) * 0.25), 0);
				clod.place(at, clod.getYRot(), 0);
				if (age == XIRANG_FLIES) swell();
				return true;
			}
			long holding = age - XIRANG_FLIES;
			// Whoever stands within it as it swells, or comes in, is buried in it.
			if (holding < XIRANG_SWELLS + XIRANG_HOLDS) {
				// Its soil spreads out from the heart as it swells: a being is buried once it reaches them.
				double spread = XIRANG_RADIUS * Math.min(1, (holding + 4) / (double) (XIRANG_SWELLS * 0.6));
				for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, XIRANG_RADIUS + XIRANG_HEIGHT, e -> !buried.contains(e) && !HeavenSpells.spared(caster, e))) {
					if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > spread || e.getY() > ground.y + XIRANG_HEIGHT + 4 || e.getY() < ground.y - 8) continue;
					buried.add(e);
					int holds = (int) (XIRANG_SWELLS + XIRANG_HOLDS - holding);
					WoodSpells.bind(caster, e, holds);
					mounds.put(e, SpellConstruct.create(level, SpellConstruct.Kind.BURIAL_MASS, e.position(), 0, 0, holds + 16, caster)
							.size((float) Math.max(1.3, e.getBbWidth() * 0.8 + 0.8)).value(e.getBbHeight() * 0.72f).tint(level.random.nextInt())
							.anchor(e, SpellAnchor.POSITION, Vec3.ZERO).spawn());
					SpellFx.send(level, SpellFx.Kind.EARTH_BURST, HeavenSpells.middle(e), Vec3.ZERO, 1.5f, Block.getId(Blocks.DIRT.defaultBlockState()));
					sound(e.position(), SoundEvents.ROOTED_DIRT_PLACE, 2.0f, 0.5f);
				}
				// The land heaving as its soil grows out under it.
				if (holding < XIRANG_SWELLS && holding % 3 == 0) {
					double a = level.random.nextDouble() * Math.PI * 2, d = spread * Math.sqrt(level.random.nextDouble());
					Vec3 at = HeavenSpells.groundBelow(level, ground.add(Math.cos(a) * d, 6, Math.sin(a) * d), 0, 16);
					if (at != null) SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 1.8f, Block.getId(Blocks.ROOTED_DIRT.defaultBlockState()), 192);
				}
				if (holding % 20 == 10) {
					for (LivingEntity e : buried) if (HeavenSpells.present(e, level)) HeavenSpells.strikeAgain(caster, e, XIRANG_DAMAGE, spell);
					sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 3.0f, 0.5f);
				}
			}
			if (holding == XIRANG_SWELLS + XIRANG_HOLDS) {
				clod.act("settle");
				if (bloom != null) bloom.act("settle");
				for (SpellConstruct mound : mounds.values()) mound.act("crumble");
				for (LivingEntity e : buried) {
					if (!HeavenSpells.present(e, level)) continue;
					e.invulnerableTime = 0;
					HeavenSpells.strike(caster, e, XIRANG_SETTLE, spell);
				}
				SpellFx.send(level, SpellFx.Kind.DUST, ground.add(0, XIRANG_HEIGHT * 0.6, 0), Vec3.ZERO, (float) XIRANG_RADIUS, 0, 192);
				sound(ground, SoundEvents.GENERIC_EXPLODE, 3.0f, 0.4f);
			}
			return true;
		}

		/** It lands and swells, sinking into the land: its soil grows out over it from there and the mound heaves up round it. */
		private void swell() {
			clod.act("swell");
			clod.place(ground.add(0, clod.size() * 0.6, 0), clod.getYRot(), 0);
			bloom = SpellConstruct.create(level, SpellConstruct.Kind.XIRANG_BLOOM, ground.add(0, 0.05, 0), 0, 0, XIRANG_SWELLS + XIRANG_HOLDS + 24, caster)
					.size((float) XIRANG_RADIUS).value((float) XIRANG_HEIGHT).spawn();
			Devastation.raise(caster, level, ground, XIRANG_RADIUS, XIRANG_HEIGHT, 3);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, (float) XIRANG_RADIUS, Block.getId(Blocks.DIRT.defaultBlockState()), 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) XIRANG_RADIUS * 1.5f, 0, 192);
			sound(ground, SoundEvents.WARDEN_EMERGE, 3.0f, 0.7f);
			sound(ground, SoundEvents.ROOTED_DIRT_PLACE, 4.0f, 0.4f);
		}

		@Override
		public void end() {
			clod.dissolve();
			if (bloom != null) bloom.dissolve();
			for (SpellConstruct mound : mounds.values()) mound.dissolve();
		}
	}

	/**
	 * Nine Cauldrons Suppress the Land: the nine bronze cauldrons Yu the Great cast for the nine provinces (SpellConstruct NINE_CAULDRON, its own
	 * GeckoLib model) fall one after another out of the sky, {@link #CAULDRON_EVERY} ticks apart: the first on where its caster looks, the rest
	 * in a ring {@link #CAULDRONS_RING} round it. Each strikes everything within {@link #CAULDRON_STRIKE} of where it lands for
	 * {@link #CAULDRON_DAMAGE}, smashing a crater. Once all nine stand, for {@link #CAULDRONS_TICKS} they suppress the land within the ring: every
	 * foe inside is pressed down (it can't fly or leap, slowed to a crawl, weakened) and takes {@link #CAULDRONS_PRESS} a second, and the land
	 * within sinks {@link #CAULDRONS_SINK}. Then they rise away.
	 */
	public static final class Cauldrons extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final Vec3[] spots = new Vec3[9];
		private final SpellConstruct[] cauldrons = new SpellConstruct[9];
		private final float yaw;
		private final int landed;

		Cauldrons(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, 8 * CAULDRON_EVERY + CAULDRON_FALL + CAULDRONS_TICKS + CAULDRONS_RISE + 8);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.landed = 8 * CAULDRON_EVERY + CAULDRON_FALL;
			spots[0] = ground;
			for (int k = 1; k < 9; k++) {
				Vec3 out = Vec3.directionFromRotation(0, yaw + 45 * (k - 1)).scale(CAULDRONS_RING);
				Vec3 at = ground.add(out);
				Vec3 land = HeavenSpells.groundBelow(level, at.add(0, 16, 0), 0, 48);
				spots[k] = land != null ? land : at;
			}
			sound(ground.add(0, CAULDRONS_HIGH, 0), SoundEvents.BELL_RESONATE, 4.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			for (int k = 0; k < 9; k++) {
				long falling = age - k * CAULDRON_EVERY;
				if (falling < 0 || falling > CAULDRON_FALL) continue;
				Vec3 sky = spots[k].add(0, CAULDRONS_HIGH, 0);
				// The first tick it is due (not its tick 0 alone: a cast after this tick's spells have run is first seen a tick in).
				if (cauldrons[k] == null) {
					// Each faces out from the heart (the heart's, the way its caster looks), its province's glyph outward.
					float facing = k == 0 ? yaw + 180 : yaw + 45 * (k - 1);
					// Of the world's own blocks, bronze green with age, each its own (SpellConstruct CAULDRON_MASS: EarthMasses; the user's ask, 2026-10-25).
					cauldrons[k] = SpellConstruct.create(level, SpellConstruct.Kind.CAULDRON_MASS, sky, facing, 0, (int) (end - start - k * CAULDRON_EVERY), caster)
							.size((float) (CAULDRON_STRIKE * 0.85)).tint(level.random.nextInt()).spawn();
					sound(sky, SoundEvents.ANVIL_PLACE, 3.0f, 0.5f);
				}
				double f = falling / (double) CAULDRON_FALL;
				cauldrons[k].place(sky.lerp(spots[k], f * f), cauldrons[k].getYRot(), 0);
				if (falling == CAULDRON_FALL) land(k);
			}
			long pressing = age - landed;
			if (pressing >= 0 && pressing < CAULDRONS_TICKS) press(pressing);
			if (pressing == CAULDRONS_TICKS) {
				for (SpellConstruct cauldron : cauldrons) if (cauldron != null) cauldron.act("rise");
				sound(ground, SoundEvents.BELL_RESONATE, 4.0f, 0.6f);
			}
			if (pressing > CAULDRONS_TICKS) {
				double f = (pressing - CAULDRONS_TICKS) / (double) CAULDRONS_RISE;
				for (int k = 0; k < 9; k++) if (cauldrons[k] != null) cauldrons[k].place(spots[k].add(0, CAULDRONS_HIGH * 0.4 * f * f, 0), cauldrons[k].getYRot(), 0);
			}
			return true;
		}

		/** Cauldron {@code k} lands: everything near it struck and thrown down, a crater; the last one down, the land within the ring sinks. */
		private void land(int k) {
			cauldrons[k].act("land");
			Vec3 at = spots[k];
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, CAULDRON_STRIKE + 8, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - at.x, e.getZ() - at.z) > CAULDRON_STRIKE || e.getY() > at.y + 16 || e.getY() < at.y - 8) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, CAULDRON_DAMAGE, spell);
				HeavenSpells.push(at, e, 1.4);
			}
			Devastation.crater(caster, level, at, k == 0 ? CAULDRON_HEART_CRATER : CAULDRON_CRATER, CAULDRON_CRATER_DEPTH, Devastation.Floor.RUBBLE);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, (float) CAULDRON_STRIKE, Block.getId(groundBelowState(level, at)), 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) CAULDRON_STRIKE * 1.6f, 0, 192);
			sound(at, SoundEvents.ANVIL_LAND, 4.0f, 0.4f);
			sound(at, SoundEvents.BELL_BLOCK, 4.0f, 0.5f);
			if (k == 8) {
				Devastation.subside(caster, level, ground, CAULDRONS_RING, CAULDRONS_SINK, 0);
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) CAULDRONS_RING * 1.4f, 0, 192);
			}
		}

		/** A tick of their suppression: every foe within the ring pressed down; every second, crushed. */
		private void press(long pressing) {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, CAULDRONS_RING + 16, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > CAULDRONS_RING || e.getY() > ground.y + 40 || e.getY() < ground.y - 16) continue;
				if (e instanceof net.minecraft.server.level.ServerPlayer player) QiFlight.weighDown(player, 10);
				Vec3 v = e.getDeltaMovement();
				if (v.y > -0.6) HeavenSpells.move(e, new Vec3(v.x * 0.5, Math.min(v.y, 0) - 0.25, v.z * 0.5));
				HeavenSpells.safeFall(e, 40);
				if (pressing % 20 == 0) {
					HeavenSpells.strikeAgain(caster, e, CAULDRONS_PRESS, spell);
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
				}
			}
			if (pressing % 40 == 0) {
				SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) CAULDRONS_RING, 0, 192);
				sound(ground, SoundEvents.BELL_RESONATE, 3.0f, 0.4f);
			}
		}

		@Override
		public void end() {
			for (SpellConstruct cauldron : cauldrons) if (cauldron != null) cauldron.dissolve();
		}
	}

	// --- Entering Nirvana ---

	/**
	 * Gravestone Falls: a colossal stele written with the names of the dead, borne on a stone tortoise (SpellConstruct GRAVESTONE, its own GeckoLib
	 * model), falls out of the sky onto where its caster looks ({@link #GRAVE_FALL}): {@link #GRAVE_DAMAGE} to everything within
	 * {@link #GRAVE_RADIUS}, pinned for {@link #GRAVE_PINS}, the land smashed into a crater and cracked out to {@link #GRAVE_CRACKS}. It stands a
	 * while ({@link #GRAVE_STANDS}); then it sinks into the land ({@link #GRAVE_SINKS}), dragging every foe within {@link #GRAVE_PULL} to it and
	 * down after it, and buries whatever is within {@link #GRAVE_BURY} as it goes under: {@link #GRAVE_BURIES} more, and a pit where it went.
	 */
	public static final class Gravestone extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, sky;
		private final SpellConstruct stone;

		Gravestone(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, GRAVE_FALL + GRAVE_STANDS + GRAVE_SINKS + GRAVE_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.sky = ground.add(0, GRAVE_HIGH, 0);
			// Of the world's own blocks (SpellConstruct GRAVESTONE_MASS: EarthMasses; the user's ask, 2026-10-25: its model "looks a bit weird").
			this.stone = SpellConstruct.create(level, SpellConstruct.Kind.GRAVESTONE_MASS, sky, caster.getYRot() + 180, 0, (int) (end - start), caster)
					.size((float) (GRAVE_RADIUS * 2.4)).tint(level.random.nextInt()).spawn();
			sound(sky, SoundEvents.BELL_RESONATE, 4.0f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= GRAVE_FALL) {
				double f = age / (double) GRAVE_FALL;
				stone.place(sky.lerp(ground, f * f), stone.getYRot(), 0);
				if (age == GRAVE_FALL) land();
				return true;
			}
			long since = age - GRAVE_FALL;
			if (since == GRAVE_STANDS) {
				stone.act("sink");
				sound(ground, SoundEvents.GRAVEL_BREAK, 4.0f, 0.4f);
			}
			if (since > GRAVE_STANDS && since < GRAVE_STANDS + GRAVE_SINKS) drag();
			if (since == GRAVE_STANDS + GRAVE_SINKS) bury();
			return true;
		}

		/** It lands: everything under it struck and pinned, the land smashed and cracked. */
		private void land() {
			stone.act("land");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, GRAVE_RADIUS + 8, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > GRAVE_RADIUS || e.getY() > ground.y + 24 || e.getY() < ground.y - 8) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, GRAVE_DAMAGE, spell);
				WoodSpells.bind(caster, e, GRAVE_PINS);
			}
			Devastation.crater(caster, level, ground, GRAVE_CRATER, GRAVE_CRATER_DEPTH, Devastation.Floor.RUBBLE);
			Devastation.fissures(caster, level, ground, GRAVE_CRACKS, 8, 4, GRAVE_CRATER * 0.8);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, (float) GRAVE_RADIUS * 1.5f, Block.getId(groundBelowState(level, ground)), 192);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) GRAVE_CRACKS, 0, 192);
			sound(ground, SoundEvents.ANVIL_LAND, 5.0f, 0.3f);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.5f);
		}

		/** A tick of its sinking: every foe near dragged to it and down after it. */
		private void drag() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, GRAVE_PULL + 8, e -> !HeavenSpells.spared(caster, e))) {
				Vec3 to = ground.subtract(e.position()).multiply(1, 0, 1);
				double d = to.length();
				if (d > GRAVE_PULL || e.getY() > ground.y + 16 || e.getY() < ground.y - 16) continue;
				double pull = HeavenSpells.greatBeast(caster, e) ? 0.12 : 0.45;
				HeavenSpells.move(e, (d > 1 ? to.normalize().scale(pull) : Vec3.ZERO).add(0, -0.35, 0));
				HeavenSpells.safeFall(e, 60);
			}
			if (level.getGameTime() % 4 == 0) SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) GRAVE_PULL * 0.6f, 0, 160);
		}

		/** It goes under: whatever is near buried with it, a pit where it went. */
		private void bury() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, GRAVE_BURY + 8, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > GRAVE_BURY || e.getY() > ground.y + 16 || e.getY() < ground.y - 20) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, GRAVE_BURIES, spell);
			}
			Devastation.crater(caster, level, ground, GRAVE_PIT, GRAVE_PIT_DEPTH, Devastation.Floor.RUBBLE);
			SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) GRAVE_BURY, 0, 192);
			sound(ground, SoundEvents.GRAVEL_BREAK, 5.0f, 0.3f);
			sound(ground, SoundEvents.WARDEN_DIG, 4.0f, 0.6f);
		}

		@Override
		public void end() {
			stone.dissolve();
		}
	}

	/**
	 * Buried World: a ring of {@link #BURIED_STONES} standing stones, carved with spirals and runes of the old world (SpellConstruct
	 * STANDING_STONE, its own GeckoLib model), heaves up out of the land {@link #BURIED_RADIUS} round where its caster looks ({@link #BURIED_RISES});
	 * for {@link #BURIED_TICKS} the land within sinks, {@link #BURIED_SINK} deeper every second, every foe within dragged down with it and crushed
	 * for {@link #BURIED_CRUSH} a second. Then the stones topple in on it ({@link #BURIED_FALLS}): {@link #BURIED_TOPPLE} to every foe within, and
	 * the land left a vast pit.
	 */
	public static final class Buried extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final SpellConstruct[] stones = new SpellConstruct[BURIED_STONES];
		private final Vec3[] spots = new Vec3[BURIED_STONES];

		Buried(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, BURIED_RISES + BURIED_TICKS + BURIED_FALLS + BURIED_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			for (int k = 0; k < BURIED_STONES; k++) {
				Vec3 out = Vec3.directionFromRotation(0, caster.getYRot() + 360f * k / BURIED_STONES).scale(BURIED_RADIUS);
				Vec3 land = HeavenSpells.groundBelow(level, ground.add(out).add(0, 16, 0), 0, 48);
				spots[k] = land != null ? land : ground.add(out);
				// Its carved face toward the heart (its model's -z: the way its yaw looks), so it topples in.
				float[] face = HeavenSpells.facing(ground.subtract(spots[k]).multiply(1, 0, 1));
				// Of the world's own stone (SpellConstruct MENHIR_MASS: EarthMasses; the user's ask, 2026-10-25), each its own.
				stones[k] = SpellConstruct.create(level, SpellConstruct.Kind.MENHIR_MASS, spots[k], face[0], 0, (int) (end - start), caster)
						.size((float) (BURIED_RADIUS * 0.75)).tint(level.random.nextInt()).spawn();
			}
			sound(ground, SoundEvents.WARDEN_EMERGE, 4.0f, 0.5f);
			sound(ground, SoundEvents.ANCIENT_DEBRIS_BREAK, 4.0f, 0.4f);
		}

		private boolean within(LivingEntity e) {
			return Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) <= BURIED_RADIUS && e.getY() < ground.y + 20 && e.getY() > ground.y - 40;
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < BURIED_RISES) return true;
			long sinking = age - BURIED_RISES;
			if (sinking < BURIED_TICKS) {
				if (sinking % 20 == 0) {
					Devastation.subside(caster, level, ground, BURIED_RADIUS * 0.92, BURIED_SINK, 0);
					SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) BURIED_RADIUS, 0, 192);
					sound(ground, SoundEvents.GRAVEL_BREAK, 4.0f, 0.4f);
				}
				for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, BURIED_RADIUS + 20, e -> !HeavenSpells.spared(caster, e))) {
					if (!within(e)) continue;
					Vec3 v = e.getDeltaMovement();
					if (!HeavenSpells.greatBeast(caster, e) && v.y > -0.6) HeavenSpells.move(e, new Vec3(v.x * 0.6, Math.min(v.y, 0) - 0.3, v.z * 0.6));
					HeavenSpells.safeFall(e, 60);
					if (sinking % 20 == 10) {
						HeavenSpells.strikeAgain(caster, e, BURIED_CRUSH, spell);
						e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
					}
				}
				return true;
			}
			if (sinking == BURIED_TICKS) {
				for (SpellConstruct stone : stones) stone.act("topple");
				sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			}
			if (sinking == BURIED_TICKS + BURIED_FALLS) topple();
			return true;
		}

		/** The stones fall in on it: every foe within struck. */
		private void topple() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, BURIED_RADIUS + 20, e -> !HeavenSpells.spared(caster, e))) {
				if (!within(e)) continue;
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, BURIED_TOPPLE, spell);
			}
			for (Vec3 spot : spots) {
				SpellFx.send(level, SpellFx.Kind.EARTH_BURST, spot.add(ground.subtract(spot).scale(0.25)), Vec3.ZERO, 6, Block.getId(Blocks.STONE.defaultBlockState()), 192);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) BURIED_RADIUS * 1.3f, 0, 192);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
		}

		@Override
		public void end() {
			for (SpellConstruct stone : stones) if (stone != null) stone.dissolve();
		}
	}

	// --- True Immortal ---

	/**
	 * Hand of Houtu: the hand of the mother of the earth, of the land's yellow jade veined with gold (SpellConstruct HOUTU_HAND, its own GeckoLib
	 * model), rises open out of the land where its caster looks ({@link #HOUTU_RISES}) and closes ({@link #HOUTU_GRASPS}): every foe within
	 * {@link #HOUTU_GRASP} takes {@link #HOUTU_CRUSH} and is caught up in its fist (a great beast only crushed, and held slowed where it stands).
	 * It holds them {@link #HOUTU_HOLDS}, then brings its fist down forward onto the land, carrying them with it ({@link #HOUTU_SLAMS}): everything
	 * within {@link #HOUTU_SLAM_RADIUS} of where it lands, and all it holds, take {@link #HOUTU_SLAM}, the land smashed into a crater, cracks
	 * running out of it.
	 */
	public static final class Houtu extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, forward;
		private final float yaw;
		private final SpellConstruct hand;
		private final List<LivingEntity> held = new ArrayList<>();
		private boolean grasped;

		Houtu(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, HOUTU_RISES + HOUTU_GRASPS + HOUTU_HOLDS + HOUTU_SLAMS + HOUTU_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.forward = Vec3.directionFromRotation(0, yaw);
			// Of the world's own stone, yellow jade veined with gold, its fingers curling joint by joint (SpellConstruct HAND_MASS: EarthMasses; the
			// user's ask, 2026-10-25).
			this.hand = SpellConstruct.create(level, SpellConstruct.Kind.HAND_MASS, ground, yaw, 0, (int) (end - start), caster).size((float) HOUTU_FIST)
					.tint(level.random.nextInt()).spawn();
			sound(ground, SoundEvents.WARDEN_EMERGE, 5.0f, 0.4f);
			sound(ground, SoundEvents.ROOTED_DIRT_BREAK, 5.0f, 0.3f);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, 8, Block.getId(Blocks.DIRT.defaultBlockState()), 192);
		}

		/** Where its fist is, turned {@code degrees} forward from upright about its wrist's foot. */
		private Vec3 fist(double degrees) {
			double a = Math.toRadians(degrees);
			return ground.add(forward.scale(Math.sin(a) * HOUTU_FIST)).add(0, Math.cos(a) * HOUTU_FIST, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < HOUTU_RISES) return true;
			if (!grasped) {
				grasped = true;
				grasp();
			}
			long slamming = age - HOUTU_RISES - HOUTU_GRASPS - HOUTU_HOLDS;
			if (slamming == 0) {
				hand.act("slam");
				sound(ground, SoundEvents.WARDEN_ATTACK_IMPACT, 5.0f, 0.4f);
			}
			if (slamming <= HOUTU_SLAMS) {
				// What it holds, in its fist: still as it waits, swept along with it as it comes down.
				Vec3 at = fist(slamming < 0 ? 0 : 95.0 * slamming / HOUTU_SLAMS);
				held.removeIf(e -> !HeavenSpells.present(e, level));
				for (LivingEntity e : held) {
					// Never into the land: its fist comes down onto it, a little past upright, and what it holds lies on it.
					HeavenSpells.teleport(e, new Vec3(at.x, Math.max(at.y - e.getBbHeight() / 2, ground.y + 0.2), at.z));
					HeavenSpells.safeFall(e, 60);
				}
			}
			if (slamming == HOUTU_SLAMS) slam();
			return true;
		}

		/** It closes: every foe within crushed, all but a great beast caught up in its fist. */
		private void grasp() {
			hand.act("grasp");
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, HOUTU_GRASP, 8, 30)) {
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, HOUTU_CRUSH, spell);
				if (HeavenSpells.greatBeast(caster, e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HOUTU_GRASPS + HOUTU_HOLDS + HOUTU_SLAMS, 4));
				else held.add(e);
			}
			SpellFx.send(level, SpellFx.Kind.ROCK_CHIPS, fist(0), Vec3.ZERO, 6, 0, 192);
			sound(ground, SoundEvents.STONE_BREAK, 5.0f, 0.3f);
			sound(ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
		}

		/** Its fist comes down on the land: all within reach of it and all it holds struck, the land smashed into a crater, cracked round it. */
		private void slam() {
			Vec3 land = fist(90), below = HeavenSpells.groundBelow(level, land.add(0, 8, 0), 0, 24);
			Vec3 at = below != null ? below : land;
			java.util.Set<LivingEntity> struck = new java.util.HashSet<>(held);
			struck.addAll(WoodSpells.foesWithin(caster, level, at, HOUTU_SLAM_RADIUS, 10, 24));
			for (LivingEntity e : struck) {
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, HOUTU_SLAM, spell);
				HeavenSpells.safeFall(e, 100);
			}
			held.clear();
			Devastation.crater(caster, level, at, HOUTU_CRATER, HOUTU_CRATER_DEPTH, Devastation.Floor.RUBBLE);
			Devastation.fissures(caster, level, at, HOUTU_CRACKS, 10, 5, HOUTU_CRATER * 0.8);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, at, Vec3.ZERO, 12, Block.getId(Blocks.STONE.defaultBlockState()), 256);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) HOUTU_SLAM_RADIUS * 1.8f, 0, 256);
			SpellFx.send(level, SpellFx.Kind.DUST, at, Vec3.ZERO, (float) HOUTU_SLAM_RADIUS * 1.5f, 0, 256);
			sound(at, SoundEvents.GENERIC_EXPLODE, 6.0f, 0.4f);
			sound(at, SoundEvents.WARDEN_SONIC_BOOM, 4.0f, 0.4f);
		}

		@Override
		public void end() {
			hand.dissolve();
		}
	}

	/**
	 * Falling Continent: a land torn up whole, meadows, woods, snow-capped peaks and a lake on it, the bedrock it was torn from hanging jagged
	 * under it, made of the world's own blocks (SpellConstruct CONTINENT_MASS: BlockMasses; the user's ask, 2026-10-25, "the earth move falling
	 * continent looks dumb": it was a see-through model of its own), comes down out of the sky from {@link #CONTINENT_HIGH} onto where its caster
	 * looks, slowly at first and then faster and faster ({@link #CONTINENT_FALLS}), tumbling, the wind roaring past it, rocks and earth breaking
	 * off its rim and raining down ahead of it, its weight pressing down every foe under it, the dust rising to meet it. It strikes:
	 * {@link #CONTINENT_DAMAGE} to everything within {@link #CONTINENT_RADIUS}, thrown out, the land smashed into a vast crater, cracks running
	 * out of it, the ground shaking far round; it is driven down into what it smashed ({@link #CONTINENT_AFTER}) and is gone.
	 */
	public static final class Continent extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground;
		private final float yaw;
		private final SpellConstruct land;
		private final double radius;
		private boolean struck;

		Continent(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, CONTINENT_FALLS + CONTINENT_AFTER);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.yaw = caster.getYRot();
			this.radius = CONTINENT_RADIUS * CONTINENT_LAND;
			this.land = SpellConstruct.create(level, SpellConstruct.Kind.CONTINENT_MASS, at(0), yaw, 0, (int) (end - start) + SpellConstruct.FADE, caster)
					.size((float) radius).tint(level.random.nextInt()).spawn();
			sound(ground, SoundEvents.WARDEN_ROAR, 5.0f, 0.3f);
			sound(ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 5.0f, 0.4f);
			loop(land, SoundEvents.ELYTRA_FLYING, 5.0f, 0.4f);
		}

		/** Where its heart is {@code age} ticks into its fall: high over the spot, coming down ever faster, to its rest driven into the land. */
		private Vec3 at(double age) {
			double f = Math.min(1, age / CONTINENT_FALLS);
			return ground.add(0, CONTINENT_HIGH - (CONTINENT_HIGH - radius * 0.3) * Math.pow(f, 2.3), 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= CONTINENT_FALLS) {
				double f = Math.min(1, age / (double) CONTINENT_FALLS);
				Vec3 here = at(age);
				land.place(here, yaw + 40 * (float) f, 0);
				// Its weight on everything under it, heavier as it comes.
				for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, CONTINENT_RADIUS, 10, CONTINENT_HIGH)) {
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, f > 0.5 ? 4 : 2));
					Vec3 v = e.getDeltaMovement();
					if (!HeavenSpells.greatBeast(caster, e) && v.y > -0.4) HeavenSpells.move(e, new Vec3(v.x, Math.min(v.y, 0) - 0.1 - 0.25 * f, v.z));
				}
				// Rock and earth breaking off its rim, raining down ahead of it.
				if (age % 3 == 0 && f < 0.92) {
					for (int i = 0; i < 2; i++) {
						double a = level.random.nextDouble() * Math.PI * 2, out = radius * (0.55 + 0.4 * level.random.nextDouble());
						Vec3 from = here.add(Math.cos(a) * out, -radius * 0.25 * level.random.nextDouble(), Math.sin(a) * out);
						double drop = Math.max(6, from.y - ground.y);
						BlockState rock = (level.random.nextInt(3) == 0 ? Blocks.DIRT : level.random.nextInt(2) == 0 ? Blocks.STONE : Blocks.COBBLESTONE).defaultBlockState();
						SpellBlock.show(level, rock, from, new Vec3(Math.cos(a) * 4, -drop, Math.sin(a) * 4), 0, (int) Math.min(40, 10 + drop / 3), SpellBlock.Motion.FLY, 0)
								.scale(1.4f + level.random.nextFloat() * 1.6f);
					}
				}
				if (age % 12 == 0) sound(here, SoundEvents.GRAVEL_BREAK, 4.0f, 0.4f);
				// The dust rising to meet it, wider and thicker as it comes.
				if (age % 6 == 0) SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) (CONTINENT_RADIUS * (0.4 + f)), 0, 256);
				if (age == CONTINENT_FALLS - 14) sound(ground, SoundEvents.WARDEN_SONIC_CHARGE, 5.0f, 0.4f);
			}
			if (!struck && age >= CONTINENT_FALLS) {
				struck = true;
				strike();
			}
			if (struck && age % 8 == 0 && age < CONTINENT_FALLS + 32) {
				// The land still giving way under it as it is driven down.
				SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) CONTINENT_RADIUS * 1.4f, 0, 256);
				sound(ground, SoundEvents.STONE_BREAK, 5.0f, 0.3f + level.random.nextFloat() * 0.1f);
			}
			return true;
		}

		/** It strikes the land: everything under it struck and thrown out, the land smashed into a vast crater, cracked round it, the ground shaken. */
		private void strike() {
			land.act("impact");
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, CONTINENT_RADIUS, 12, 40)) {
				e.invulnerableTime = 0;
				HeavenSpells.strike(caster, e, CONTINENT_DAMAGE, spell);
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.push(ground, e, 3.0);
				HeavenSpells.safeFall(e, 120);
			}
			Devastation.crater(caster, level, ground, CONTINENT_CRATER, CONTINENT_CRATER_DEPTH, Devastation.Floor.RUBBLE);
			Devastation.fissures(caster, level, ground, CONTINENT_CRACKS, 14, 6, CONTINENT_CRATER * 0.85);
			SpellFx.send(level, SpellFx.Kind.EARTH_BURST, ground, Vec3.ZERO, 22, Block.getId(Blocks.STONE.defaultBlockState()), 256);
			for (int i = 0; i < 6; i++) {
				double a = Math.PI * 2 * i / 6;
				Vec3 rim = ground.add(Math.cos(a) * radius, 1, Math.sin(a) * radius);
				SpellFx.send(level, SpellFx.Kind.EARTH_BURST, rim, Vec3.ZERO, 10, Block.getId((i % 2 == 0 ? Blocks.DIRT : Blocks.GRASS_BLOCK).defaultBlockState()), 256);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) CONTINENT_RADIUS * 2, 0, 320);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) CONTINENT_RADIUS * 3, 0, 320);
			SpellFx.send(level, SpellFx.Kind.DUST, ground, Vec3.ZERO, (float) CONTINENT_RADIUS * 1.8f, 0, 256);
			sound(ground, SoundEvents.GENERIC_EXPLODE, 8.0f, 0.3f);
			sound(ground, SoundEvents.WARDEN_SONIC_BOOM, 6.0f, 0.3f);
			sound(ground, SoundEvents.ANVIL_LAND, 6.0f, 0.3f);
			sound(ground, SoundEvents.DRAGON_FIREBALL_EXPLODE, 6.0f, 0.4f);
			sound(ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 6.0f, 0.3f);
		}

		@Override
		public void end() {
			land.dissolve();
		}
	}

	/**
	 * Every minute, the golems and titans gone forgotten: a cultivator's are never forget()ted (it dies, it doesn't leave), so before the
	 * sweep of 2026-10-24 each that ever raised one was kept here, its crumbled golems with it, all the server's life.
	 */
	static void tick(MinecraftServer server) {
		if (server.getTickCount() % 1200 != 0) return;
		GOLEMS_OF.values().forEach(mine -> mine.removeIf(g -> !g.isAlive() || g.isRemoved()));
		GOLEMS_OF.values().removeIf(List::isEmpty);
		TITANS.values().removeIf(titan -> !titan.isAlive() || titan.isRemoved());
	}

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
