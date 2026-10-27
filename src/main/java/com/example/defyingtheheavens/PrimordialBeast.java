package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One of the Immortal Realm's primordial beasts (phase 2 of the realm plan, 2026-10-26; the user's change: "the 3 primordial beasts
 * should be slightly stronger than true immortal grand perfection"; the fourth, the Primordial Chaos, phase 3c's): a world boss, one of each
 * in the realm at a time (PrimordialBeasts: their lairs, their coming and going), a True Immortal of Grand Perfection (the Primordial Chaos
 * a Golden Immortal's) {@link #MIGHT} times as strong as its realm. Its own ways in
 * the air, steered here rather than by paths (no path is made for a body this vast):
 * <ul>
 *   <li><b>The Kun-Peng</b> (water, then metal): the Kun, a fish some fifty blocks long (Void Shattering's Kun, its model the spell's:
 *   BeastModels), glides through the sea of clouds, its back breaking them, rising at what comes over it: a spout of cloud that throws it
 *   up, a ram from below. Brought to half its strength it breaks from the clouds and is the Peng ({@link #breach}), a bird whose wings span
 *   some sixty blocks: a blast of wind that hurls back, a volley of feather blades (BeastShot), dives. Left alone {@link #CALM_TICKS} it goes
 *   back into the clouds the Kun, whole again.</li>
 *   <li><b>The True Phoenix</b> (fire): circles its peak; fire breathed in a cone, a rain of burning feathers, the shock of its wings. Slain,
 *   it burns down to an ember ({@link #EMBER_TICKS}, untouchable) and rises again whole, once in a fight; the second time it dies.</li>
 *   <li><b>The True Dragon</b> (wood, thunder's element), some eighty blocks long: rides its storm (dark clouds about it on the client,
 *   lightning striking round it, no fire kindled); lightning called down where its foe stood (it roars), the sweep of its tail, its coils,
 *   its dive.</li>
 *   <li><b>The Primordial Chaos</b> (phase 3c), Hundun's remnant out in the Chaos Frontier: a faceless sack of churning chaos on six legs
 *   and four wings, banded in the five colours fading to grey, some forty blocks across; a Golden Immortal of Grand Perfection
 *   (BeastKind#CHAOS_RANK). The Unmaking ({@link #UNMAKING_REACH}: it draws in on itself, told and seen, then bursts, hurling all about it
 *   and folding away every Law domain whose owner it catches, LawDomains#unmake), Devour (whatever is within {@link #DEVOUR_REACH} pulled
 *   in and crushed against it), a fan of chaos bolts (BeastShot), and Heaven Immortal fiends called up about it (ChaosFiend). Slain it
 *   leaves {@link #CHAOS_FRAGMENTS} Dao Fragments of as many Laws, {@link #CHAOS_STONES} Chaos Origin Stones and {@link #CHAOS_JADE}
 *   Immortal Jade, and the Court pays the more for it (CelestialCourt#SLAY_CHAOS).</li>
 * </ul>
 * Each keeps to its lair ({@link #LEASH}, giving chase {@link #CHASE} further), goes for whoever comes within {@link #NOTICE}, shows its
 * strength to those within {@link #BAR_REACH} on a boss bar, and left alone {@link #CALM_TICKS} is whole again (the Phoenix its rebirth
 * with it). Slain it leaves its core, Immortal Jade, the Phoenix's and the Dragon's their races' True Immortal blood (DemonBlood#kin), the
 * Kun-Peng a True Immortal slip of its form's element, all hanging in the air where it fell (over the sea of clouds they would be lost);
 * the Celestial Court pays each of its officers who fought it (CelestialCourt#onPrimordialSlain).
 */
public class PrimordialBeast extends DemonBeast {
	/** How many times their realm's strength (True Immortal Grand Perfection's) they have, body and blows: a little more (the user's word). */
	public static final double MIGHT = 1.25;
	/**
	 * It goes for whoever comes this near; keeps within this of its lair, giving chase this much further; shows its bar this far (half
	 * again what they were when the three were drawn smaller: its reaches below grew with them, 2026-10-26).
	 */
	public static final double NOTICE = 64, LEASH = 128, CHASE = 96, BAR_REACH = 112;
	/** Left alone this long (no foe, nothing striking it), it is whole again: the Peng the Kun, the Phoenix's rebirth restored. */
	public static final int CALM_TICKS = 2 * 60 * 20;
	/** The Phoenix lies an ember this long before it is reborn. */
	public static final int EMBER_TICKS = 10 * 20;
	/** Immortal Jade it leaves: this many, and up to this many more. */
	public static final int JADE = 6, JADE_MORE = 5;
	/** Those within this hear of what it does (its rising, its rebirth, its fall). */
	static final double HEARD = 160;
	/**
	 * Those within this see what it does: its particles are sent this far (sendParticles alone reaches only those within 32 blocks of each,
	 * and a body this vast is fought from further: its breaking from the clouds was lost at seventy blocks, tour 15). PrimordialLooks likewise.
	 */
	public static final double FX_REACH = 160;
	/** The Kun keeps in the sea of clouds: its feet (its body's foot) this far under their top, at the most and the least. */
	static final double KUN_DEEP = 11, KUN_SHALLOW = 8;
	/** The Peng's size (the Kun's is its kind's). */
	static final float PENG_WIDTH = 12.8f, PENG_HEIGHT = 8.0f;
	/**
	 * The Primordial Chaos: its Unmaking bursts this far about it after drawing in so long (ticks); its Devour pulls in whatever is this
	 * near; it calls up fiends while fewer than this many are about it; what it leaves.
	 */
	public static final double UNMAKING_REACH = 30, DEVOUR_REACH = 40;
	static final int UNMAKING_GATHER = 40, SUMMONED_MAX = 4;
	public static final int CHAOS_FRAGMENTS = 6, CHAOS_STONES = 12, CHAOS_JADE = 12;
	/** Chaos streaming into the Primordial Chaos, and out of it: grey-violet motes. */
	private static final DustParticleOptions CHAOS_MOTE = new DustParticleOptions(new Vector3f(0.56f, 0.46f, 0.74f), 3.0f);

	private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(PrimordialBeast.class, EntityDataSerializers.BYTE);
	/** The Kun's spout: sea-blue mist, seen against the sea of clouds' white. */
	private static final DustParticleOptions MIST = new DustParticleOptions(new Vector3f(0.45f, 0.72f, 0.95f), 2.5f);
	/** Its phase: the Kun-Peng's form (the Kun, the Peng); the Phoenix whole or an ember. */
	public static final byte FIRST = 0, PENG = 1, EMBER = 1;

	// Its moves, one at a time, each its own rest after.
	private static final int NONE = 0, SPOUT = 1, RAM = 2, GUST = 3, VOLLEY = 4, DIVE = 5, BREATH = 6, RAIN = 7, SHOCK = 8, LIGHTNING = 9,
			SWEEP = 10, COIL = 11, UNMAKING = 12, DEVOUR = 13, BOLTS = 14, SUMMON = 15;
	private static final int MOVES = 16;

	/** Its lair (its own spot, when it came into the world some other way than to its lair: an egg, a command). */
	private BlockPos lair;
	/** Whether it is the realm's own (PrimordialBeasts set it at its lair), the one of its kind the realm has. */
	private boolean ofTheRealm;
	/** The Phoenix: reborn already in this fight. */
	private boolean reborn;
	/** The Phoenix: ticks left as an ember. */
	private int ember;
	/** When it last had a foe or was struck, and when it last settled, whole again. */
	private long lastFight, calmedAt;
	/** Players who struck it in this fight (the Court pays those of it who fought it). */
	private final Set<UUID> fought = new HashSet<>();
	private final long[] ready = new long[MOVES];
	private int move = NONE, moveTicks, divePhase;
	private Vec3 mark;
	private LivingEntity held;
	private double circling;
	private long nextBolt;
	private ServerBossEvent bar;

	public PrimordialBeast(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		setNoGravity(true);
		AttributeInstance range = getAttribute(Attributes.FOLLOW_RANGE);
		if (range != null) range.setBaseValue(NOTICE + 16);
		circling = random.nextDouble() * Math.PI * 2;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_PHASE, FIRST);
	}

	/** Only its foes: whoever strikes it, whoever comes near it whom it hunts (DemonBeast#hunts). Where it goes is its own (below). */
	@Override
	protected void registerGoals() {
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, t -> near(t) && hunts(t)));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CultivatorNpc.class, 10, false, false, t -> near(t) && hunts(t)));
	}

	private boolean near(LivingEntity target) {
		double reach = NOTICE + getBbWidth();
		return distanceToSqr(target) <= reach * reach;
	}

	// --- What it is ---

	public byte phase() { return entityData.get(DATA_PHASE); }

	private void setPhase(byte phase) { entityData.set(DATA_PHASE, phase); }

	/** The Kun-Peng flying as the Peng. */
	public boolean isPeng() { return kind() == BeastKind.KUN_PENG && phase() == PENG; }

	/** The Kun-Peng swimming the clouds as the Kun. */
	public boolean isKun() { return kind() == BeastKind.KUN_PENG && phase() == FIRST; }

	/** The Phoenix burning as an ember, to be reborn. */
	public boolean isEmber() { return kind() == BeastKind.TRUE_PHOENIX && phase() == EMBER; }

	/** Whether the Phoenix has been reborn in this fight already. */
	public boolean reborn() { return reborn; }

	/** Its build as it is now (the client draws it so: BeastModels): the Peng a bird, else its kind's. */
	public BeastKind.Body body() { return isPeng() ? BeastKind.Body.BIRD : kind().body; }

	/** Its element as it is now: the Peng's metal (the wind), else its kind's. */
	public QiElement element() { return isPeng() ? QiElement.METAL : kind().element; }

	public BlockPos lair() { return lair; }

	public boolean isOfTheRealm() { return ofTheRealm; }

	/** Set at its lair as the realm's own (PrimordialBeasts). */
	void settle(BlockPos lair) {
		this.lair = lair.immutable();
		this.ofTheRealm = true;
		setPersistenceRequired();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return isPeng() ? EntityDimensions.scalable(PENG_WIDTH, PENG_HEIGHT) : super.getDimensions(pose);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_PHASE.equals(key)) refreshDimensions();
	}

	/**
	 * Its model runs far past its body (the Kun's tail and fins, the Peng's and the Phoenix's wings, the Dragon's coils, some seventy blocks
	 * of them trailing its head): drawn while any of it shows.
	 */
	@Override
	public AABB getBoundingBoxForCulling() {
		double reach = switch (kind()) {
			case TRUE_DRAGON -> 76;
			case TRUE_PHOENIX -> 28;
			case PRIMORDIAL_CHAOS -> 30;
			default -> isPeng() ? 34 : 40;
		};
		return getBoundingBox().inflate(reach);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.isInvulnerableTo(source);
		// An ember can't be touched; nor does a body this vast suffocate where it brushes the land, fall, or burn in its own storm (the
		// Primordial Chaos's, the Frontier's).
		if (isEmber() || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL) || source.is(DamageTypes.LIGHTNING_BOLT)
				|| source.is(ModDamageTypes.CHAOS_STORM)) return true;
		return super.isInvulnerableTo(source);
	}

	@Override
	public void thunderHit(ServerLevel level, LightningBolt bolt) {
		// The storm is its own.
	}

	// --- Each tick ---

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!(level() instanceof ServerLevel level)) return;
		long now = level.getGameTime();
		if (lair == null) lair = blockPosition();
		// One of each at a time: a copy left behind (a beast lost and come again) goes.
		if (ofTheRealm && tickCount % 20 == 0 && !PrimordialBeasts.claims(this)) {
			discard();
			return;
		}
		if (tickCount % 10 == 0) updateBar(level);
		if (isEmber()) {
			burn(level);
			return;
		}
		LivingEntity target = keepTarget();
		if (target != null) lastFight = now;
		else if (now - lastFight > CALM_TICKS && calmedAt <= lastFight) calm(level, now);
		if (isKun() && getHealth() <= getMaxHealth() * 0.5f) breach(level);
		if (target != null) getLookControl().setLookAt(target, 20, 20);
		if (move != NONE) act(level, target, now);
		else if (target != null) choose(level, target, now);
		if (move == NONE || !steersItself()) cruise(level, target);
		bite(target, now);
		storm(level, target, now);
	}

	/** Its foe, while it is one: alive, in this world, one it may strike, not run off past its chase. */
	private LivingEntity keepTarget() {
		LivingEntity target = getTarget();
		if (target == null) return null;
		double give = LEASH + CHASE;
		if (!target.isAlive() || target.level() != level() || !canAttack(target)
				|| target.distanceToSqr(Vec3.atCenterOf(lair)) > give * give) {
			setTarget(null);
			return null;
		}
		return target;
	}

	/** Left alone: whole again, its fight forgotten; the Peng goes back into the clouds the Kun; the Phoenix may be reborn again. */
	void calm(ServerLevel level, long now) {
		calmedAt = now;
		setHealth(getMaxHealth());
		reborn = false;
		fought.clear();
		endMove();
		if (isPeng()) {
			setPhase(FIRST);
			fx(level, ParticleTypes.CLOUD, getX(), getY(), getZ(), 90, 6, 1.5, 6, 0.1);
		}
	}

	// --- Where it goes ---

	@Override
	public void travel(Vec3 travel) {
		// Steered (below), it goes as it is sent, without a walker's friction or a faller's weight.
		if (isEffectiveAi()) {
			move(MoverType.SELF, getDeltaMovement());
			calculateEntityAnimation(false);
			return;
		}
		super.travel(travel);
	}

	/**
	 * Whether its move under way steers it (a ram, a dive, the Phoenix hovering as it breathes fire, the Dragon holding its prey, the
	 * Primordial Chaos drawing in on itself or devouring).
	 */
	private boolean steersItself() {
		return move == RAM || move == DIVE || move == BREATH || move == COIL || move == UNMAKING || move == DEVOUR;
	}

	/**
	 * How much swifter than first made they go, their rushes with them (the user, 2026-10-27: "all the primordial beasts are a little
	 * slow"); and the most they turn a tick (degrees).
	 */
	static final double SWIFT = 1.5;
	static final float TURN = 8.0f;

	/** Its way while it is not moved by its move: round its foe if it has one, else round its lair, each at its own height and pace. */
	private void cruise(ServerLevel level, LivingEntity target) {
		Vec3 centre;
		double radius, height, speed;
		boolean kun = isKun(), chaos = kind() == BeastKind.PRIMORDIAL_CHAOS;
		if (target != null) {
			centre = target.position();
			radius = kun ? 10 : isPeng() ? 30 : kind() == BeastKind.TRUE_PHOENIX ? 24 : chaos ? 26 : 33;
			height = kun ? 0 : isPeng() ? 15 : kind() == BeastKind.TRUE_PHOENIX ? 12 : chaos ? 6 : 15;
			speed = (kun ? 0.44 : isPeng() ? 0.78 : chaos ? 0.5 : 0.65) * SWIFT;
		} else {
			centre = Vec3.atCenterOf(lair);
			radius = kun ? 54 : isPeng() ? 78 : kind() == BeastKind.TRUE_PHOENIX ? 33 : chaos ? 48 : 84;
			height = isPeng() ? ModDimensions.IMMORTAL_CLOUD_Y + 40 - centre.y : 0;
			speed = (kun ? 0.24 : chaos ? 0.3 : 0.42) * SWIFT;
		}
		circling += speed / radius;
		Vec3 to = new Vec3(centre.x + Math.cos(circling) * radius, centre.y + height, centre.z + Math.sin(circling) * radius);
		flyToward(level, to, speed);
	}

	/**
	 * Flies toward {@code to} at {@code speed} blocks a tick, easing round (a body this great turns slowly), facing the way it goes: the Kun
	 * kept in the clouds, the rest over the land (climbing where it rises ahead, or where it meets it).
	 */
	private void flyToward(ServerLevel level, Vec3 to, double speed) {
		double y = to.y;
		if (isKun()) {
			double top = ModDimensions.IMMORTAL_CLOUD_Y;
			y = Mth.clamp(y, top - KUN_DEEP, top - KUN_SHALLOW);
		} else {
			Vec3 way = new Vec3(to.x - getX(), 0, to.z - getZ());
			Vec3 ahead = way.lengthSqr() < 1.0e-4 ? position() : position().add(way.normalize().scale(getBbWidth() + 6));
			int ground = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(ahead.x), Mth.floor(ahead.z)),
					level.getHeight(Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ()));
			y = Math.max(y, ground + 4);
		}
		Vec3 want = new Vec3(to.x, y, to.z).subtract(position());
		double distance = want.length();
		want = distance < 1.0e-3 ? Vec3.ZERO : want.scale(Math.min(speed, distance * 0.25) / distance);
		if (horizontalCollision && !isKun()) want = want.add(0, speed, 0);
		setDeltaMovement(getDeltaMovement().lerp(want, 0.16));
		face(getDeltaMovement());
	}

	/** Its body turned toward {@code way} (up to {@link #TURN} degrees a tick). */
	private void face(Vec3 way) {
		if (way.horizontalDistanceSqr() < 1.0e-4) return;
		float yaw = (float) (Mth.atan2(way.z, way.x) * Mth.RAD_TO_DEG) - 90.0f;
		setYRot(Mth.approachDegrees(getYRot(), yaw, TURN));
		yBodyRot = getYRot();
	}

	/** Toward {@code target} at {@code speed} straight (a ram, a dive; {@link #SWIFT} the swifter), facing it. */
	private void rush(Vec3 to, double speed) {
		Vec3 want = to.subtract(position());
		double distance = want.length();
		want = distance < 1.0e-3 ? Vec3.ZERO : want.scale(speed * SWIFT / distance);
		setDeltaMovement(getDeltaMovement().lerp(want, 0.3));
		face(getDeltaMovement());
	}

	// --- Its moves ---

	private boolean rested(int which, long now) {
		return now >= ready[which];
	}

	private void begin(int which) {
		move = which;
		moveTicks = 0;
		divePhase = 0;
	}

	private void endMove() {
		if (move != NONE) ready[move] = level().getGameTime() + rest(move);
		if (move == COIL || move == SWEEP || move == UNMAKING) setActing(ACT_COIL, false);
		if (move == BREATH || move == DEVOUR) setActing(ACT_BREATH, false);
		move = NONE;
		held = null;
		mark = null;
	}

	/** How long each move rests after (ticks). */
	private static int rest(int which) {
		return switch (which) {
			case SPOUT -> 110;
			case RAM -> 140;
			case GUST -> 90;
			case VOLLEY -> 70;
			case DIVE -> 170;
			case BREATH -> 150;
			case RAIN -> 110;
			case SHOCK -> 130;
			case LIGHTNING -> 80;
			case SWEEP -> 100;
			case COIL -> 220;
			case UNMAKING -> 260;
			case DEVOUR -> 200;
			case BOLTS -> 70;
			case SUMMON -> 500;
			default -> 40;
		};
	}

	/** A move for its foe now, if one is rested and in its reach: the ones it has, tried in a random order. */
	private void choose(ServerLevel level, LivingEntity target, long now) {
		if (now % 5 != 0) return;
		double d = Math.sqrt(distanceToSqr(target)) - getBbWidth() / 2;
		boolean seen = hasLineOfSight(target);
		List<Integer> can = new ArrayList<>();
		if (isKun()) {
			double flat = Math.sqrt(Math.pow(target.getX() - getX(), 2) + Math.pow(target.getZ() - getZ(), 2));
			if (rested(SPOUT, now) && flat < 33 && target.getY() > ModDimensions.IMMORTAL_CLOUD_Y) can.add(SPOUT);
			if (rested(RAM, now) && d < 27) can.add(RAM);
		} else if (isPeng()) {
			if (rested(GUST, now) && d < 39 && seen) can.add(GUST);
			if (rested(VOLLEY, now) && d > 9 && d < 51 && seen) can.add(VOLLEY);
			if (rested(DIVE, now) && d < 60) can.add(DIVE);
		} else if (kind() == BeastKind.TRUE_PHOENIX) {
			if (rested(BREATH, now) && d < 33 && seen) can.add(BREATH);
			if (rested(RAIN, now) && d < 60) can.add(RAIN);
			if (rested(SHOCK, now) && d < 18) can.add(SHOCK);
		} else if (kind() == BeastKind.TRUE_DRAGON) {
			if (rested(LIGHTNING, now) && d < 72) can.add(LIGHTNING);
			if (rested(SWEEP, now) && d < 21) can.add(SWEEP);
			if (rested(COIL, now) && d < 10) can.add(COIL);
			if (rested(DIVE, now) && d < 66) can.add(DIVE);
		} else if (kind() == BeastKind.PRIMORDIAL_CHAOS) {
			if (rested(UNMAKING, now) && d < UNMAKING_REACH - 6) can.add(UNMAKING);
			if (rested(DEVOUR, now) && d < DEVOUR_REACH - 8) can.add(DEVOUR);
			if (rested(BOLTS, now) && d > 6 && d < 60 && seen) can.add(BOLTS);
			if (rested(SUMMON, now)) can.add(SUMMON);
		}
		if (can.isEmpty()) return;
		begin(can.get(random.nextInt(can.size())));
		act(level, target, now);
	}

	/** The move under way, a tick of it. */
	private void act(ServerLevel level, LivingEntity target, long now) {
		moveTicks++;
		if (target == null && move != SPOUT && move != LIGHTNING && move != UNMAKING) {
			endMove();
			return;
		}
		switch (move) {
			case SPOUT -> spout(level, target);
			case RAM -> ram(level, target);
			case GUST -> gust(level, target);
			case VOLLEY -> volley(level, target);
			case DIVE -> dive(level, target);
			case BREATH -> breathe(level, target);
			case RAIN -> rain(level, target);
			case SHOCK -> shock(level, target);
			case LIGHTNING -> lightning(level, target);
			case SWEEP -> sweep(level, target);
			case COIL -> coil(level, target);
			case UNMAKING -> unmaking(level);
			case DEVOUR -> devour(level, target);
			case BOLTS -> bolts(level, target);
			case SUMMON -> summon(level, target);
			default -> endMove();
		}
	}

	/** The Kun's spout: the clouds swirl under its foe a moment, then burst up in a column, throwing up whatever is in it. */
	private void spout(ServerLevel level, LivingEntity target) {
		double top = ModDimensions.IMMORTAL_CLOUD_Y;
		if (mark == null) {
			mark = new Vec3(target.getX(), top, target.getZ());
			sound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0f, 0.5f);
		}
		if (moveTicks < 25) {
			if (moveTicks % 2 == 0) {
				double angle = moveTicks * 0.6;
				for (int i = 0; i < 6; i++) {
					double a = angle + i * Math.PI / 3, r = 5 - moveTicks * 0.15;
					fx(level, MIST, mark.x + Math.cos(a) * r, top + 0.5, mark.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0.02);
				}
			}
			return;
		}
		// The spout: sea-blue mist (white cloud is lost against the white sea of clouds: tour 14), spray about it.
		for (int i = 0; i < 34; i++) {
			fx(level, MIST, mark.x, top + i, mark.z, 8, 1.5, 0.4, 1.5, 0.02);
			if (i % 3 == 0) fx(level, ParticleTypes.CLOUD, mark.x, top + i, mark.z, 5, 2.0, 0.4, 2.0, 0.06);
		}
		fx(level, ParticleTypes.SPLASH, mark.x, top + 2, mark.z, 90, 3.5, 1, 3.5, 0.3);
		sound(SoundEvents.TRIDENT_RIPTIDE_3, 3.0f, 0.5f);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(mark.x - 5, top - 3, mark.z - 5, mark.x + 5, top + 36, mark.z + 5),
				e -> reaches(e, false))) {
			if (other.hurt(damageSources().mobAttack(this), attack())) {
				double share = pushShare(other);
				other.setDeltaMovement(other.getDeltaMovement().add(0, 1.3 * share, 0));
				other.hurtMarked = true;
			}
		}
		endMove();
	}

	/** The Kun's ram: it sinks a moment, then rises at its foe and strikes it, and sinks back into the clouds. */
	private void ram(ServerLevel level, LivingEntity target) {
		if (moveTicks <= 8) {
			setDeltaMovement(getDeltaMovement().lerp(new Vec3(0, -0.2, 0), 0.3));
			if (moveTicks == 1) sound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.0f, 0.6f);
			return;
		}
		rush(target.position().add(0, target.getBbHeight() / 2, 0), 1.4);
		if (getBoundingBox().inflate(2.0).intersects(target.getBoundingBox())) {
			if (target.hurt(damageSources().mobAttack(this), attack() * 1.6f)) {
				Vec3 away = target.position().subtract(position()).normalize();
				double share = pushShare(target);
				target.setDeltaMovement(away.x * 2.0 * share, 0.6 * share, away.z * 2.0 * share);
				target.hurtMarked = true;
			}
			fx(level, ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 40, 2, 1, 2, 0.1);
			sound(SoundEvents.GENERIC_SPLASH, 3.0f, 0.5f);
			endMove();
		} else if (moveTicks > 30) {
			endMove();
		}
	}

	/** The Peng's gust: its wings drawn back, then a blast of wind before it, hurling back whatever it meets. */
	private void gust(ServerLevel level, LivingEntity target) {
		if (moveTicks < 10) return;
		Vec3 from = getEyePosition(), look = target.getEyePosition().subtract(from).normalize();
		for (int i = 3; i < 39; i += 3) {
			Vec3 at = from.add(look.scale(i));
			fx(level, ParticleTypes.CLOUD, at.x, at.y, at.z, 4, i * 0.08, i * 0.08, i * 0.08, 0.05);
		}
		sound(SoundEvents.ENDER_DRAGON_FLAP, 4.0f, 0.5f);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(39), e -> reaches(e, true))) {
			Vec3 to = other.getEyePosition().subtract(from);
			if (to.length() > 39 || to.normalize().dot(look) < 0.72) continue;
			if (other.hurt(damageSources().mobAttack(this), attack() * 0.8f)) {
				double share = pushShare(other);
				Vec3 push = to.normalize();
				other.setDeltaMovement(push.x * 2.2 * share, 0.5 * share + push.y * share, push.z * 2.2 * share);
				other.hurtMarked = true;
			}
		}
		endMove();
	}

	/** The Peng's volley: feather blades loosed at its foe in a fan. */
	private void volley(ServerLevel level, LivingEntity target) {
		Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
		Vec3 to = target.getEyePosition().subtract(from);
		for (int i = -3; i <= 3; i++) {
			BeastShot blade = new BeastShot(level, this, BeastShot.Kind.FEATHER, attack() * 0.45f);
			blade.setPos(from.x, from.y, from.z);
			Vec3 way = to.yRot((float) Math.toRadians(i * 6));
			blade.shoot(way.x, way.y, way.z, 2.0f, 2.0f);
			level.addFreshEntity(blade);
		}
		sound(SoundEvents.ENDER_DRAGON_FLAP, 3.0f, 1.2f);
		endMove();
	}

	/** A dive (the Peng's, the Dragon's): up over its foe, down on it, and up away; the Dragon's with a thunderclap where it strikes. */
	private void dive(ServerLevel level, LivingEntity target) {
		switch (divePhase) {
			case 0 -> {
				rush(target.position().add(0, 26, 0), 0.9);
				if (position().distanceToSqr(target.position().add(0, 26, 0)) < 64 || moveTicks > 40) {
					divePhase = 1;
					moveTicks = 0;
					sound(kind() == BeastKind.TRUE_DRAGON ? SoundEvents.ENDER_DRAGON_GROWL : SoundEvents.PHANTOM_SWOOP, 4.0f, 0.5f);
				}
			}
			case 1 -> {
				rush(target.position().add(0, target.getBbHeight() * 0.5, 0), 1.8);
				if (getBoundingBox().inflate(1.5).intersects(target.getBoundingBox())) {
					target.hurt(damageSources().mobAttack(this), attack() * 1.8f);
					for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10), e -> reaches(e, false))) {
						Vec3 away = other.position().subtract(position());
						away = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize();
						double share = pushShare(other);
						other.setDeltaMovement(away.x * 1.5 * share, 0.6 * share, away.z * 1.5 * share);
						other.hurtMarked = true;
					}
					if (kind() == BeastKind.TRUE_DRAGON) bolt(level, target.position());
					fx(level, ParticleTypes.CLOUD, getX(), getY(), getZ(), 70, 4.5, 1.5, 4.5, 0.15);
					divePhase = 2;
					moveTicks = 0;
				} else if (moveTicks > 50) {
					divePhase = 2;
					moveTicks = 0;
				}
			}
			default -> {
				rush(position().add(getLookAngle().x * 12, 15, getLookAngle().z * 12), 1.0);
				if (moveTicks > 20) endMove();
			}
		}
	}

	/** The Phoenix's fire, breathed in a cone at its foe as it hangs in the air. */
	private void breathe(ServerLevel level, LivingEntity target) {
		if (moveTicks == 1) {
			setActing(ACT_BREATH, true);
			sound(SoundEvents.BLAZE_SHOOT, 4.0f, 0.5f);
		}
		setDeltaMovement(getDeltaMovement().scale(0.7));
		face(target.position().subtract(position()));
		Vec3 mouth = getEyePosition(), look = target.getEyePosition().subtract(mouth).normalize();
		if (moveTicks % 2 == 0) {
			for (int i = 3; i <= 33; i += 3) {
				Vec3 at = mouth.add(look.scale(i));
				fx(level, ParticleTypes.FLAME, at.x, at.y, at.z, 4, i * 0.07, i * 0.07, i * 0.07, 0.01);
			}
		}
		if (moveTicks % 6 == 0) {
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(33), e -> reaches(e, true))) {
				Vec3 to = other.getEyePosition().subtract(mouth);
				if (to.length() > 33 || to.normalize().dot(look) < 0.82) continue;
				if (other.hurt(damageSources().mobAttack(this), attack() * 0.6f)) other.setSecondsOnFire(4);
			}
			sound(SoundEvents.FIRECHARGE_USE, 2.0f, 0.6f);
		}
		if (moveTicks >= 50) endMove();
	}

	/** The Phoenix's rain: burning feathers falling about its foe. */
	private void rain(ServerLevel level, LivingEntity target) {
		for (int i = 0; i < 14; i++) {
			double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * 7.5;
			BeastShot feather = new BeastShot(level, this, BeastShot.Kind.FIRE_FEATHER, attack() * 0.5f);
			feather.setPos(target.getX() + Math.cos(a) * r, target.getY() + 20 + random.nextDouble() * 8, target.getZ() + Math.sin(a) * r);
			feather.shoot((random.nextDouble() - 0.5) * 0.1, -1, (random.nextDouble() - 0.5) * 0.1, 1.0f, 1.0f);
			level.addFreshEntity(feather);
		}
		sound(SoundEvents.BLAZE_SHOOT, 3.0f, 0.8f);
		endMove();
	}

	/** The shock of the Phoenix's wings: a ring of fire thrown out from it. */
	private void shock(ServerLevel level, LivingEntity target) {
		if (moveTicks < 8) {
			setDeltaMovement(getDeltaMovement().add(0, 0.05, 0));
			return;
		}
		for (int k = 0; k < 64; k++) {
			double a = Math.PI * 2 * k / 64;
			for (double r : new double[] {6, 12, 18}) fx(level, ParticleTypes.FLAME, getX() + Math.cos(a) * r, getY() + 1.5, getZ() + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
		}
		sound(SoundEvents.ENDER_DRAGON_FLAP, 4.0f, 0.7f);
		sound(SoundEvents.BLAZE_SHOOT, 3.0f, 0.6f);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(18), e -> reaches(e, false) && e.distanceToSqr(this) <= 324 + getBbWidth() * getBbWidth())) {
			if (!other.hurt(damageSources().mobAttack(this), attack())) continue;
			other.setSecondsOnFire(3);
			Vec3 away = other.position().subtract(position());
			away = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize();
			double share = pushShare(other);
			other.setDeltaMovement(away.x * 1.8 * share, 0.6 * share, away.z * 1.8 * share);
			other.hurtMarked = true;
		}
		endMove();
	}

	/** The Dragon's lightning: sparks gather where its foe stands, and a moment after the sky strikes there (and about it). */
	private void lightning(ServerLevel level, LivingEntity target) {
		if (mark == null) {
			if (target == null) {
				endMove();
				return;
			}
			mark = target.position();
			sound(SoundEvents.TRIDENT_THUNDER, 2.0f, 1.4f);
			triggerAnim(CONTROLLER, "roar"); // it roars, calling the storm down
		}
		if (moveTicks < 20) {
			if (moveTicks % 2 == 0) fx(level, ParticleTypes.ELECTRIC_SPARK, mark.x, mark.y + 0.5, mark.z, 12, 1.2, 0.3, 1.2, 0.05);
			return;
		}
		bolt(level, mark);
		for (int i = 0; i < 2; i++) {
			double a = random.nextDouble() * Math.PI * 2, r = 4 + random.nextDouble() * 6;
			bolt(level, mark.add(Math.cos(a) * r, 0, Math.sin(a) * r));
		}
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(mark, mark).inflate(4, 5, 4), e -> reaches(e, false))) {
			other.hurt(damageSources().mobAttack(this), attack() * 1.2f);
		}
		endMove();
	}

	/** The sweep of the Dragon's tail: its body curled, then whipped round through whatever is near. */
	private void sweep(ServerLevel level, LivingEntity target) {
		if (moveTicks == 1) setActing(ACT_COIL, true);
		if (moveTicks < 6) return;
		if (moveTicks == 6) {
			sound(SoundEvents.PLAYER_ATTACK_SWEEP, 4.0f, 0.5f);
			sound(SoundEvents.ENDER_DRAGON_FLAP, 4.0f, 0.6f);
			for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(21), e -> reaches(e, false))) {
				if (!other.hurt(damageSources().mobAttack(this), attack() * 1.3f)) continue;
				Vec3 out = other.position().subtract(position());
				Vec3 round = new Vec3(-out.z, 0, out.x);
				round = round.lengthSqr() < 1.0e-6 ? Vec3.ZERO : round.normalize();
				double share = pushShare(other);
				other.setDeltaMovement(round.x * 1.8 * share, 0.4 * share, round.z * 1.8 * share);
				other.hurtMarked = true;
			}
		}
		if (moveTicks >= 12) endMove();
	}

	/** The Dragon's coils: its prey held fast before its jaws, crushed now and then, a few seconds. */
	private void coil(ServerLevel level, LivingEntity target) {
		if (moveTicks == 1) {
			held = target;
			setActing(ACT_COIL, true);
			sound(SoundEvents.PHANTOM_BITE, 3.0f, 0.4f);
		}
		setDeltaMovement(getDeltaMovement().scale(0.5));
		if (held == null || !held.isAlive() || held.distanceToSqr(this) > 324 || moveTicks > 60) {
			endMove();
			return;
		}
		Vec3 hold = position().add(Vec3.directionFromRotation(0, getYRot()).scale(getBbWidth() * 0.7 + 0.5)).add(0, getBbHeight() * 0.3, 0);
		DemonBeast.afflict(held, MobEffects.MOVEMENT_SLOWDOWN, 5, 6, this);
		if (held.position().distanceToSqr(hold) > 2.25 && level.noCollision(held, held.getBoundingBox().move(hold.subtract(held.position())))) {
			if (held instanceof ServerPlayer player) player.teleportTo(hold.x, hold.y, hold.z);
			else held.teleportTo(hold.x, hold.y, hold.z);
		}
		if (moveTicks % 12 == 0) held.hurt(damageSources().mobAttack(this), attack() * 0.7f);
	}

	/**
	 * The Primordial Chaos's Unmaking: it draws in on itself {@link #UNMAKING_GATHER} ticks (told to those near; chaos streaming into it, a
	 * ring marking its reach), then bursts: all within {@link #UNMAKING_REACH} harmed and hurled out, every Law domain whose owner it catches
	 * folded away (LawDomains#unmake).
	 */
	private void unmaking(ServerLevel level) {
		Vec3 middle = position().add(0, getBbHeight() * 0.5, 0);
		if (moveTicks == 1) {
			setActing(ACT_COIL, true);
			sound(SoundEvents.WARDEN_SONIC_CHARGE, 5.0f, 0.4f);
			tell(level, Component.translatable(ModLang.MSG_CHAOS_UNMAKING).withStyle(ChatFormatting.DARK_PURPLE));
		}
		setDeltaMovement(getDeltaMovement().scale(0.6));
		if (moveTicks < UNMAKING_GATHER) {
			if (moveTicks % 2 == 0) {
				double reach = UNMAKING_REACH * (1 - 0.6 * moveTicks / UNMAKING_GATHER);
				for (int i = 0; i < 10; i++) {
					Vec3 from = middle.add(unit().scale(reach));
					fx(level, CHAOS_MOTE, from.x, from.y, from.z, 1, 0, 0, 0, 0);
				}
			}
			if (moveTicks % 8 == 0) {
				for (int k = 0; k < 64; k++) {
					double a = Math.PI * 2 * k / 64;
					fx(level, ParticleTypes.REVERSE_PORTAL, middle.x + Math.cos(a) * UNMAKING_REACH, middle.y, middle.z + Math.sin(a) * UNMAKING_REACH, 1, 0, 0.2, 0, 0.01);
				}
			}
			return;
		}
		setActing(ACT_COIL, false);
		sound(SoundEvents.WARDEN_SONIC_BOOM, 6.0f, 0.5f);
		sound(SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
		for (double r = 4; r <= UNMAKING_REACH; r += 4) {
			for (int k = 0; k < 24; k++) {
				Vec3 at = middle.add(unit().scale(r));
				fx(level, CHAOS_MOTE, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
		fx(level, ParticleTypes.SONIC_BOOM, middle.x, middle.y, middle.z, 1, 0, 0, 0, 0);
		fx(level, ParticleTypes.EXPLOSION_EMITTER, middle.x, middle.y, middle.z, 2, 2, 2, 2, 0);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(UNMAKING_REACH),
				e -> reaches(e, false) && HeavenSpells.middle(e).distanceToSqr(middle) <= UNMAKING_REACH * UNMAKING_REACH)) {
			if (!other.hurt(damageSources().mobAttack(this), attack() * 1.5f)) continue;
			Vec3 away = other.position().subtract(position());
			away = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize();
			// Half of it at the least, past any footing: the immortals it is fought by stand all but unmoved by a blow (their realms' footing).
			double share = Math.max(0.5, pushShare(other));
			other.setDeltaMovement(away.x * 2.0 * share, 0.6 * share + away.y * share, away.z * 2.0 * share);
			other.hurtMarked = true;
		}
		LawDomains.unmake(level, middle, UNMAKING_REACH);
		endMove();
	}

	/**
	 * The Primordial Chaos's Devour: hanging still, it pulls in whatever is within {@link #DEVOUR_REACH} (chaos streaming in with it), no footing
	 * holding against it (an immortal's would: tour 3c-1 drew a True Immortal four blocks), and crushes what it has drawn against it; three
	 * seconds.
	 */
	private void devour(ServerLevel level, LivingEntity target) {
		if (moveTicks == 1) {
			setActing(ACT_BREATH, true);
			sound(SoundEvents.WARDEN_ROAR, 5.0f, 0.5f);
		}
		setDeltaMovement(getDeltaMovement().scale(0.7));
		face(target.position().subtract(position()));
		Vec3 maw = position().add(0, getBbHeight() * 0.5, 0);
		if (moveTicks % 2 == 0) {
			for (int i = 0; i < 8; i++) {
				Vec3 from = maw.add(unit().scale(DEVOUR_REACH * (0.5 + random.nextDouble() * 0.5)));
				Vec3 in = maw.subtract(from).scale(0.06);
				fx(level, ParticleTypes.REVERSE_PORTAL, from.x, from.y, from.z, 0, in.x, in.y, in.z, 1.0);
			}
		}
		double crush = getBbWidth() * 0.5 + 3;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(DEVOUR_REACH), e -> reaches(e, false))) {
			Vec3 in = maw.subtract(HeavenSpells.middle(other));
			double d = in.length();
			if (d > DEVOUR_REACH || d < 1.0e-3) continue;
			// Drawn in to its hide and held there, crushed against it (pulled on into its middle, a player's view was inside it: tour 3c-3).
			other.setDeltaMovement(d > crush ? other.getDeltaMovement().scale(0.6).add(in.scale(0.3 / d)) : other.getDeltaMovement().scale(0.2));
			other.hurtMarked = true;
			if (d < crush + 1 && moveTicks % 10 == 0) other.hurt(damageSources().mobAttack(this), attack() * 0.6f);
		}
		if (moveTicks % 20 == 0) sound(SoundEvents.WARDEN_HEARTBEAT, 4.0f, 0.5f);
		if (moveTicks >= 60) endMove();
	}

	/** The Primordial Chaos's bolts: a fan of nine chaos bolts loosed at its foe. */
	private void bolts(ServerLevel level, LivingEntity target) {
		Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
		Vec3 to = target.getEyePosition().subtract(from);
		for (int i = -4; i <= 4; i++) {
			BeastShot bolt = new BeastShot(level, this, BeastShot.Kind.CHAOS, attack() * 0.5f);
			bolt.setPos(from.x, from.y, from.z);
			Vec3 way = to.yRot((float) Math.toRadians(i * 8));
			bolt.shoot(way.x, way.y, way.z, 1.8f, 1.5f);
			level.addFreshEntity(bolt);
		}
		sound(SoundEvents.ILLUSIONER_CAST_SPELL, 4.0f, 0.5f);
		endMove();
	}

	/** The Primordial Chaos calls up fiends: up to three Heaven Immortals out of the chaos about it, set on its foe, while fewer than {@link #SUMMONED_MAX} are near. */
	private void summon(ServerLevel level, LivingEntity target) {
		int near = level.getEntitiesOfClass(ChaosFiend.class, getBoundingBox().inflate(48), ChaosFiend::isAlive).size();
		int[] ranks = ChaosFrontier.fiendRanks(ChaosFrontier.DEEP);
		for (int i = 0; i < 3 && near + i < SUMMONED_MAX; i++) {
			double a = random.nextDouble() * Math.PI * 2, r = getBbWidth() * 0.6 + 4;
			Vec3 at = position().add(Math.cos(a) * r, getBbHeight() * 0.5, Math.sin(a) * r);
			ChaosFiend fiend = ChaosFrontier.fiend(level, BlockPos.containing(at), ranks[0] + random.nextInt(ranks[1] - ranks[0] + 1));
			if (fiend == null) continue;
			fiend.setTarget(target);
			fx(level, CHAOS_MOTE, at.x, at.y, at.z, 30, 1.0, 1.4, 1.0, 0.05);
			fx(level, ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 20, 0.8, 1.2, 0.8, 0.03);
		}
		sound(SoundEvents.EVOKER_PREPARE_SUMMON, 4.0f, 0.5f);
		endMove();
	}

	/** A way at random, every way alike likely. */
	private Vec3 unit() {
		Vec3 way = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
		return way.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : way.normalize();
	}

	/** Its jaws (its beak) on whatever of its foe touches it, now and then. */
	private void bite(LivingEntity target, long now) {
		if (target == null || now % 20 != 0 || move == RAM || move == DIVE) return;
		if (getBoundingBox().inflate(1.5).intersects(target.getBoundingBox())) doHurtTarget(target);
	}

	/** The True Dragon's storm: lightning about it now and then (the oftener the more it fights), none of it kindling anything. */
	private void storm(ServerLevel level, LivingEntity target, long now) {
		if (kind() != BeastKind.TRUE_DRAGON || now < nextBolt) return;
		nextBolt = now + (target != null ? 60 + random.nextInt(80) : 200 + random.nextInt(200));
		double a = random.nextDouble() * Math.PI * 2, r = 24 + random.nextDouble() * 36;
		int x = Mth.floor(getX() + Math.cos(a) * r), z = Mth.floor(getZ() + Math.sin(a) * r);
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
		bolt(level, new Vec3(x + 0.5, Math.max(ground, getY() - 60), z + 0.5));
	}

	/** A bolt of the Dragon's storm at {@code at}: seen and heard, harming nothing and kindling nothing (its strikes harm by themselves). */
	private void bolt(ServerLevel level, Vec3 at) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt == null) return;
		bolt.moveTo(at.x, at.y, at.z);
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
	}

	/** Whether an area move of its reaches {@code other}: not itself nor another beast, nor what nothing living touches; seen, if {@code sight}. */
	private boolean reaches(LivingEntity other, boolean sight) {
		if (other == this || other instanceof DemonBeast || !other.isAlive()) return false;
		if (other instanceof Player player && (player.isSpectator() || player.getAbilities().invulnerable || Incorporeal.untouchable(player))) return false;
		return !sight || hasLineOfSight(other);
	}

	/** What of a push is left on {@code other} past its knockback resistance. */
	private static double pushShare(LivingEntity other) {
		return Math.max(0, 1 - other.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
	}

	private float attack() {
		AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
		return attack == null ? 3 : (float) attack.getValue();
	}

	private void sound(SoundEvent sound, float volume, float pitch) {
		level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
	}

	/** Particles as sendParticles makes them, seen by whoever is within {@link #FX_REACH} of them (drawn however far off they are). */
	private static void fx(ServerLevel world, ParticleOptions options, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
		for (ServerPlayer player : world.players()) {
			if (player.distanceToSqr(x, y, z) <= FX_REACH * FX_REACH) world.sendParticles(player, options, true, x, y, z, count, dx, dy, dz, speed);
		}
	}

	// --- The Kun becomes the Peng ---

	/** Brought to half its strength, the Kun breaks from the sea of clouds and is the Peng. */
	void breach(ServerLevel level) {
		if (!isKun()) return;
		endMove();
		setPhase(PENG);
		setDeltaMovement(getDeltaMovement().x, 1.6, getDeltaMovement().z);
		fx(level, ParticleTypes.CLOUD, getX(), ModDimensions.IMMORTAL_CLOUD_Y, getZ(), 300, 12, 3, 12, 0.3);
		fx(level, MIST, getX(), ModDimensions.IMMORTAL_CLOUD_Y + 1, getZ(), 200, 13, 4, 13, 0.05);
		fx(level, ParticleTypes.POOF, getX(), ModDimensions.IMMORTAL_CLOUD_Y + 2, getZ(), 120, 9, 4, 9, 0.2);
		sound(SoundEvents.ENDER_DRAGON_FLAP, 5.0f, 0.4f);
		sound(SoundEvents.PHANTOM_AMBIENT, 5.0f, 0.3f);
		tell(level, Component.translatable(ModLang.MSG_PRIMORDIAL_BREACH).withStyle(ChatFormatting.AQUA));
	}

	// --- The Phoenix's rebirth ---

	@Override
	public void die(DamageSource source) {
		if (!level().isClientSide && kind() == BeastKind.TRUE_PHOENIX && !reborn && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			burnDown((ServerLevel) level());
			return;
		}
		super.die(source);
		if (level() instanceof ServerLevel level && isDeadOrDying()) {
			PrimordialBeasts.slain(this);
			CelestialCourt.onPrimordialSlain(this, fought);
			tell(level, Component.translatable(ModLang.MSG_PRIMORDIAL_SLAIN, getDisplayName()).withStyle(ChatFormatting.GOLD));
		}
	}

	/** Slain the first time, the Phoenix burns down to an ember, to be reborn. */
	private void burnDown(ServerLevel level) {
		endMove();
		setActing(ACT_BREATH, false);
		setHealth(1.0f);
		setPhase(EMBER);
		ember = EMBER_TICKS;
		setDeltaMovement(Vec3.ZERO);
		fx(level, ParticleTypes.FLAME, getX(), getY() + 3, getZ(), 220, 4.5, 4.5, 4.5, 0.15);
		fx(level, ParticleTypes.LARGE_SMOKE, getX(), getY() + 3, getZ(), 90, 4.5, 4.5, 4.5, 0.05);
		sound(SoundEvents.FIRE_EXTINGUISH, 4.0f, 0.5f);
		tell(level, Component.translatable(ModLang.MSG_PRIMORDIAL_EMBER).withStyle(ChatFormatting.RED));
	}

	/** An ember, burning where it fell: in the end it rises again, whole. */
	void burn(ServerLevel level) {
		setDeltaMovement(Vec3.ZERO);
		if (tickCount % 4 == 0) fx(level, ParticleTypes.LAVA, getX(), getY() + 2, getZ(), 2, 1.5, 1.5, 1.5, 0.1);
		if (--ember > 0) return;
		setPhase(FIRST);
		reborn = true;
		setHealth(getMaxHealth());
		for (int k = 0; k < 96; k++) {
			double a = Math.PI * 2 * k / 96;
			fx(level, ParticleTypes.FLAME, getX() + Math.cos(a) * 9, getY() + 3, getZ() + Math.sin(a) * 9, 2, 0, 0.3, 0, 0.05);
		}
		fx(level, ParticleTypes.TOTEM_OF_UNDYING, getX(), getY() + 3, getZ(), 180, 4.5, 4.5, 4.5, 0.5);
		sound(SoundEvents.TOTEM_USE, 4.0f, 0.8f);
		sound(SoundEvents.ENDER_DRAGON_GROWL, 5.0f, 1.5f);
		tell(level, Component.translatable(ModLang.MSG_PRIMORDIAL_REBORN).withStyle(ChatFormatting.GOLD));
	}

	/** Who struck it: fought it, its fight under way. */
	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		if (hurt && !level().isClientSide) {
			lastFight = level().getGameTime();
			if (source.getEntity() instanceof ServerPlayer player) fought.add(player.getUUID());
		}
		return hurt;
	}

	/** The players who fought it in this fight. */
	public Set<UUID> fought() { return fought; }

	/** Told to whoever is near. */
	private void tell(ServerLevel level, Component line) {
		for (ServerPlayer player : level.players()) if (player.distanceToSqr(this) <= HEARD * HEARD) player.displayClientMessage(line, false);
	}

	// --- Its bar ---

	private ServerBossEvent bar() {
		if (bar == null) {
			BossEvent.BossBarColor colour = switch (kind()) {
				case TRUE_PHOENIX -> BossEvent.BossBarColor.RED;
				case TRUE_DRAGON -> BossEvent.BossBarColor.YELLOW;
				case PRIMORDIAL_CHAOS -> BossEvent.BossBarColor.PURPLE;
				default -> BossEvent.BossBarColor.BLUE;
			};
			bar = new ServerBossEvent(getDisplayName(), colour, BossEvent.BossBarOverlay.NOTCHED_10);
		}
		return bar;
	}

	/** Its strength on a bar for whoever is near it (an ember's its rebirth's coming). */
	private void updateBar(ServerLevel level) {
		ServerBossEvent b = bar();
		b.setProgress(isEmber() ? 1 - ember / (float) EMBER_TICKS : Mth.clamp(getHealth() / getMaxHealth(), 0, 1));
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= BAR_REACH * BAR_REACH) b.addPlayer(player);
			else b.removePlayer(player);
		}
		for (ServerPlayer player : List.copyOf(b.getPlayers())) if (player.level() != level || player.isRemoved()) b.removePlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		if (bar != null) bar.removePlayer(player);
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (bar != null) bar.removeAllPlayers();
	}

	// --- Its voice ---

	@Override
	protected SoundEvent getAmbientSound() {
		if (isEmber()) return SoundEvents.FIRE_AMBIENT;
		return isPeng() ? SoundEvents.PHANTOM_AMBIENT : super.getAmbientSound();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isPeng() ? SoundEvents.PHANTOM_HURT : super.getHurtSound(source);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return isPeng() ? SoundEvents.PHANTOM_DEATH : super.getDeathSound();
	}

	@Override
	protected float getSoundVolume() {
		return 4.0f; // heard from afar
	}

	// --- What it leaves ---

	/** A world boss's worth of experience. */
	@Override
	public int getExperienceReward() {
		return 500;
	}

	/**
	 * Its core (of the element it was when it fell), Immortal Jade, its race's True Immortal blood (the Phoenix's the fox's, the Dragon's
	 * the dragon's), the Kun-Peng a True Immortal slip of its form's element, and its remains. The Primordial Chaos, no beast: Dao
	 * Fragments of {@link #CHAOS_FRAGMENTS} Laws, Chaos Origin Stones and Immortal Jade.
	 */
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		if (kind() == BeastKind.PRIMORDIAL_CHAOS) {
			List<Law> laws = new ArrayList<>(List.of(Law.values()));
			java.util.Collections.shuffle(laws, new java.util.Random(random.nextLong()));
			for (int i = 0; i < Math.min(CHAOS_FRAGMENTS, laws.size()); i++) spawnAtLocation(DaoFragmentItem.of(laws.get(i)));
			spawnAtLocation(new ItemStack(ModItems.CHAOS_ORIGIN_STONE, CHAOS_STONES));
			spawnAtLocation(new ItemStack(ModItems.IMMORTAL_JADE, CHAOS_JADE));
			return;
		}
		QiElement element = element();
		spawnAtLocation(DemonCoreItem.of(Math.max(0, getRank()), element, kind().getDisplayName()));
		Bloodline kin = DemonBlood.kin(kind());
		if (kin != null) spawnAtLocation(DemonBloodItem.of(kin, Realm.TRUE_IMMORTAL));
		spawnAtLocation(new ItemStack(ModItems.IMMORTAL_JADE, JADE + random.nextInt(JADE_MORE + 1)));
		if (kind() == BeastKind.KUN_PENG) spawnAtLocation(JadeSlipItem.of(HeavenMethod.of(element, Realm.TRUE_IMMORTAL)));
		for (Item remains : kind().remains()) spawnAtLocation(new ItemStack(remains, 2 + random.nextInt(3 + looting)));
	}

	/** What it leaves hangs where it fell, glowing, a long while: in the air, and over the sea of clouds, it would fall and be lost. */
	@Override
	public ItemEntity spawnAtLocation(ItemStack stack, float offsetY) {
		ItemEntity item = super.spawnAtLocation(stack, offsetY);
		if (item != null) {
			item.setNoGravity(true);
			item.setDeltaMovement(Vec3.ZERO);
			item.setExtendedLifetime();
			item.setGlowingTag(true);
		}
		return item;
	}

	// --- Its look (the client's) ---

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			PrimordialLooks.tick(this);
			lead();
		}
	}

	/**
	 * How far it is turning, and climbing or diving, eased (-1..1: turning its fastest right, climbing at 45 degrees or steeper; the
	 * client's): the True Dragon's head leads into its turns and rises and dips with its flight (BeastModels; the user, 2026-10-27: "its
	 * head never moves"). This tick's and the last, for the frames between.
	 */
	private float turning, turningO, climbing, climbingO;

	private void lead() {
		turningO = turning;
		climbingO = climbing;
		float turn = Mth.clamp(Mth.wrapDegrees(getYRot() - yRotO) / TURN, -1, 1);
		double flat = Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
		float climb = (float) Mth.clamp(Mth.atan2(getY() - yo, Math.max(flat, 0.05)) / (Math.PI / 4), -1, 1);
		turning += (turn - turning) * 0.15f;
		climbing += (climb - climbing) * 0.1f;
	}

	public float turning(float partial) { return Mth.lerp(partial, turningO, turning); }

	public float climbing(float partial) { return Mth.lerp(partial, climbingO, climbing); }

	/** Its one animation controller's name (the Dragon's roar is triggered on it). */
	static final String CONTROLLER = "body";
	private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("swim"), IDLE = RawAnimation.begin().thenLoop("idle"),
			ROAR = RawAnimation.begin().thenPlay("roar");

	/** Whether its model swims by a loop of its own (tools/spell-art's: the Kun's, the spell's; the True Dragon's), not moved in code (BeastModels). */
	public boolean swims() {
		return isKun() || kind() == BeastKind.TRUE_DRAGON;
	}

	/**
	 * Its animation: the Kun's and the Dragon's own "swim", slow for bodies this vast though quickened as they were (SWIFT; the Dragon's
	 * "roar" on top as it calls the lightning down); the Peng's and the Phoenix's "idle" (tools/beast-art's: breathing, blinking; the rest
	 * moved in code).
	 */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<PrimordialBeast> body = new AnimationController<>(this, CONTROLLER, 5, state -> state.setAndContinue(swims() ? SWIM : IDLE));
		body.setAnimationSpeedHandler(beast -> !beast.swims() ? 1.0 : beast.kind() == BeastKind.TRUE_DRAGON ? 1.0 : 0.8);
		if (kind() == BeastKind.TRUE_DRAGON) body.triggerableAnim("roar", ROAR);
		controllers.add(body);
	}

	// --- Saving ---

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putByte("Phase", phase());
		if (lair != null) tag.put("Lair", NbtUtils.writeBlockPos(lair));
		tag.putBoolean("OfTheRealm", ofTheRealm);
		tag.putBoolean("Reborn", reborn);
		tag.putInt("Ember", ember);
		tag.putLong("LastFight", lastFight);
		tag.putLong("CalmedAt", calmedAt);
		ListTag list = new ListTag();
		for (UUID id : fought) list.add(NbtUtils.createUUID(id));
		tag.put("Fought", list);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setPhase(tag.getByte("Phase"));
		if (tag.contains("Lair")) lair = NbtUtils.readBlockPos(tag.getCompound("Lair"));
		ofTheRealm = tag.getBoolean("OfTheRealm");
		reborn = tag.getBoolean("Reborn");
		ember = tag.getInt("Ember");
		lastFight = tag.getLong("LastFight");
		calmedAt = tag.getLong("CalmedAt");
		fought.clear();
		for (Tag id : tag.getList("Fought", Tag.TAG_INT_ARRAY)) fought.add(NbtUtils.loadUUID(id));
	}
}
