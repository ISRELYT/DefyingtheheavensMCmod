package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A wild demon beast (BeastKind): an animal of its kind, with no mind yet but a rank on the cultivation scale, Qi Refining Early to
 * Core Formation Grand Perfection, by which it is as strong as a cultivator of that rank (CultivationStats#applyNpc, by its kind's
 * health and armour) and the larger the higher it stands ({@link #scale}). A consciousness domain senses its realm and Realm Suppress
 * weighs it as a cultivator's (CultivatorEntity).
 * <p>
 * A territorial kind goes for whoever comes near (players, cultivators), unless they stand a realm or more above it; the others
 * only fight back. Each strikes with its kind's signature move (DemonBeastGoals). Slain, it leaves its hide and meat and, the more
 * surely the higher its rank, its demon core (DemonCoreItem: a third of the time at Qi Refining, two in three at Foundation Building,
 * always at Core Formation). A monster: no beast spawns in peace, and a sect's barrier keeps them off its grounds (Formations).
 * <p>
 * An elder (BeastKind#isElder, Void Shattering to Entering Nirvana) is never born wild: it rises over its race's blood pool in the Great
 * Void (BloodPools) and keeps to it ({@link #guard}: it ranges no further than {@link #GUARD_ROAM} from it but to give chase, and goes
 * back to it), never wandering off; slain, it leaves its core and, one time in {@link #ELDER_BLOOD_CHANCE}, its race's blood of its realm.
 */
public class DemonBeast extends Monster implements CultivatorEntity, GeoEntity {
	private static final EntityDataAccessor<Integer> DATA_RANK = SynchedEntityData.defineId(DemonBeast.class, EntityDataSerializers.INT);
	/** Flags of what it is doing, for its look: in its shell, charging, coiled round its prey, breathing fire. */
	private static final EntityDataAccessor<Byte> DATA_ACT = SynchedEntityData.defineId(DemonBeast.class, EntityDataSerializers.BYTE);
	public static final int ACT_SHELL = 1, ACT_CHARGE = 2, ACT_COIL = 4, ACT_BREATH = 8;

	private static final Map<EntityType<?>, BeastKind> KINDS = new HashMap<>();
	private static final UUID KIND_HEALTH = UUID.fromString("5b2c7e1a-3d4f-4a8b-9c6e-1f2a3b4c5d6e");
	private static final UUID KIND_ARMOUR = UUID.fromString("6c3d8f2b-4e5a-4b9c-8d7f-2a3b4c5d6e7f");
	private static final UUID NIGHT_HUNT = UUID.fromString("7d4e9a3c-5f6b-4cad-9e8a-3b4c5d6e7f8a");
	private static final UUID KIND_FOOTING = UUID.fromString("8e5fab4d-6a7c-4dbe-8f9b-4c5d6e7f8a9b");

	/** It leaves them alone who stand this many stages (a realm) or more above it. */
	public static final int FAR_ABOVE = 4;
	/** How far it notices whoever comes near; a Chaos Fiend, hunting the open sky, the further (#notice). */
	public static final double NOTICE = 14, CHAOS_NOTICE = 40;
	/** Its core's chance of being left whole: by realm (Qi Refining, Foundation Building, Core Formation). */
	public static final float[] CORE_CHANCE = {1 / 3f, 2 / 3f, 1f};
	/** An elder keeps within this of the pool it guards ({@link #guard}); and leaves its race's blood this often (its core always). */
	public static final int GUARD_ROAM = 18;
	public static final float ELDER_BLOOD_CHANCE = 0.5f;

	private final BeastKind kind;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	/** Game time until which its signature move is spent (DemonBeastGoals). */
	long signatureReady;
	/** The pool an elder guards (null: none, a wild beast's). */
	private BlockPos guards;
	/** How fully it is in each act (by its flag's bit), last tick and now: eased in and out over {@link #ACT_EASE} ticks, the client's, for its look. */
	private final float[] acted = new float[4], actedBefore = new float[4];
	private static final int ACT_EASE = 4;

	public DemonBeast(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		this.kind = of(type);
		switch (kind().moves) {
			case FLYING -> moveControl = new FlyingMoveControl(this, 20, true);
			case SWIMMING -> {
				moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1f, 0.5f, true);
				lookControl = new SmoothSwimmingLookControl(this, 10);
			}
			default -> {
			}
		}
		if (kind().moves == BeastKind.Moves.AMPHIBIOUS || kind().moves == BeastKind.Moves.SWIMMING) setPathfindingMalus(BlockPathTypes.WATER, 0.0f);
		// An elder, huge, strides over the land's rises as a man over a step.
		if (kind().isElder()) setMaxUpStep(1.5f * kind().modelScale());
		xpReward = 5;
	}

	/** The kind of its entity type. */
	public static BeastKind of(EntityType<?> type) {
		BeastKind kind = KINDS.get(type);
		if (kind == null) throw new IllegalStateException("No beast kind for " + type);
		return kind;
	}

	/** ModEntities: each kind its own type. */
	static void bind(EntityType<?> type, BeastKind kind) {
		KINDS.put(type, kind);
	}

	/** Its kind (by its type: the constructor registers its goals before the field is set). */
	public BeastKind kind() { return kind != null ? kind : of(getType()); }

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.FOLLOW_RANGE, 28.0)
				.add(Attributes.ARMOR, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return switch (of(getType()).moves) {
			case FLYING -> {
				FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
				flying.setCanOpenDoors(false);
				flying.setCanFloat(true);
				flying.setCanPassDoors(true);
				yield flying;
			}
			case SWIMMING -> new WaterBoundPathNavigation(this, level);
			case AMPHIBIOUS -> new AmphibiousPathNavigation(this, level);
			default -> super.createNavigation(level);
		};
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_RANK, -1);
		entityData.define(DATA_ACT, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		DemonBeastGoals.register(this, goalSelector, targetSelector);
	}

	// --- Its rank ---

	/** Realm and stage as one number (PlayerCultivation#rank), within its kind's tier; -1 before it is first set. */
	public int getRank() { return entityData.get(DATA_RANK); }

	public void setRank(int rank) {
		holdRank(rank);
		if (!level().isClientSide) setHealth(getMaxHealth());
	}

	/** Its rank set (and its strength and size by it), its health left as it is (a beast loaded again). */
	private void holdRank(int rank) {
		rank = Mth.clamp(rank, kind().minRank(), kind().maxRank());
		entityData.set(DATA_RANK, rank);
		refreshDimensions();
		if (!level().isClientSide) {
			applyStats();
			xpReward = 5 + rank * 3;
		}
	}

	/** A rank for one of its kind born now: the low end of its tier's ranks the likelier; in the Upper Realm, richer in qi, the higher. */
	public static int rollRank(BeastKind kind, RandomSource random, boolean upper) {
		int span = kind.maxRank() - kind.minRank() + 1;
		double roll = Math.pow(random.nextDouble(), upper ? 0.8 : 1.6);
		return kind.minRank() + Math.min(span - 1, (int) (roll * span));
	}

	@Override public Realm getCultivationRealm() { return CultivatorNpc.realmOf(Math.max(0, getRank())); }
	@Override public Stage getCultivationStage() { return CultivatorNpc.stageOf(Math.max(0, getRank())); }

	/** How much larger than its kind's least it is: a twelfth more for each stage above its tier's lowest. */
	public float scale() {
		return 1 + (Math.max(kind().minRank(), getRank()) - kind().minRank()) / 12f;
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return EntityDimensions.scalable(kind().width, kind().height).scale(scale());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_RANK.equals(key)) refreshDimensions();
	}

	/**
	 * Its strength: a cultivator's of its rank (health, blows, armour, toughness, footing), its body and blows by its kind's might (a
	 * primordial beast's the more: BeastKind#might), by its kind's health and armour; its speed its kind's. A primordial beast, vast, is
	 * moved by nothing.
	 */
	private void applyStats() {
		CultivationStats.applyNpc(this, getCultivationRealm(), getCultivationStage(), false, kind().might());
		modifier(Attributes.MAX_HEALTH, KIND_HEALTH, "Beast kind health", kind().healthScale - 1);
		modifier(Attributes.ARMOR, KIND_ARMOUR, "Beast kind armour", kind().armourScale - 1);
		modifier(Attributes.KNOCKBACK_RESISTANCE, KIND_FOOTING, "Beast kind footing", kind().isPrimordial() ? 0.25 : 0);
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.setBaseValue(kind().speed);
		AttributeInstance flying = getAttribute(Attributes.FLYING_SPEED);
		if (flying != null) flying.setBaseValue(kind().moves == BeastKind.Moves.FLYING ? kind().speed * 2.2 : 0.6);
	}

	private void modifier(net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		if (amount != 0) instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data, CompoundTag tag) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data, tag);
		if (getRank() < 0) setRank(rollRank(kind(), level.getRandom(), ModDimensions.isUpperRealm(level.getLevel().dimension())));
		return result;
	}

	/** Only a beast that goes for those near keeps them from their sleep. */
	@Override
	public boolean isPreventingPlayerRest(Player player) {
		return kind().territorial && !farAbove(player);
	}

	// --- An elder's pool ---

	/** An elder set to guard the pool at {@code pool}: kept to it ({@link #GUARD_ROAM}), kept in the world while none is near. */
	public void guard(BlockPos pool) {
		this.guards = pool.immutable();
		restrictTo(guards, GUARD_ROAM);
		setPersistenceRequired();
	}

	/** The pool it guards, or null. */
	public BlockPos guards() { return guards; }

	/** An elder never leaves the world for want of anyone near (it would leave its pool unguarded), nor a primordial beast its lair. */
	@Override
	public boolean requiresCustomPersistence() {
		return kind().isElder() || kind().isPrimordial() || super.requiresCustomPersistence();
	}

	// --- What it is doing ---

	public boolean acting(int act) { return (entityData.get(DATA_ACT) & act) != 0; }

	void setActing(int act, boolean on) {
		byte flags = entityData.get(DATA_ACT);
		entityData.set(DATA_ACT, (byte) (on ? flags | act : flags & ~act));
	}

	/**
	 * How fully it is in {@code act} (0 to 1), eased in and out over {@link #ACT_EASE} ticks: its look (BeastModels) goes into an act
	 * and out of it, a jaw not thrown open in a frame (the elders' bones, 2026-10-25). The client's.
	 */
	public float acted(int act, float partial) {
		int i = Integer.numberOfTrailingZeros(act);
		return Mth.lerp(partial, actedBefore[i], acted[i]);
	}

	/** Whatever it was doing ends (taken by a soul: SoulProjection#possess): its goals under way stopped, and with them its acts. */
	void calm() {
		goalSelector.getRunningGoals().toList().forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
		targetSelector.getRunningGoals().toList().forEach(net.minecraft.world.entity.ai.goal.WrappedGoal::stop);
		entityData.set(DATA_ACT, (byte) 0);
	}

	// --- Who it goes for ---

	/** How far it notices whoever comes near: {@link #NOTICE}, a Chaos Fiend's {@link #CHAOS_NOTICE} (it came out of the air 20 to 48 off). */
	public double notice() {
		return kind().isChaos() ? CHAOS_NOTICE : NOTICE;
	}

	/** Whether it goes for {@code target} unprovoked: a territorial kind, a player or a cultivator in reach, not far above it, not one of its own. */
	public boolean hunts(LivingEntity target) {
		if (!kind().territorial || target instanceof DemonBeast || !target.isAlive()) return false;
		if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
		if (!(target instanceof Player) && !(target instanceof CultivatorNpc)) return false;
		if (target instanceof CultivatorNpc npc && (npc.isConcealed() || npc.isVacant())) return false;
		return !farAbove(target);
	}

	/** {@code target} stands {@link #FAR_ABOVE} stages or more above it: it knows better. */
	public boolean farAbove(LivingEntity target) {
		int theirs;
		if (target instanceof ServerPlayer player) theirs = TwoPaths.weighedRank(CultivationManager.get(player));
		else if (target instanceof CultivatorNpc npc) theirs = npc.isMortal() ? -1 : npc.getRank();
		else theirs = -1;
		return theirs >= getRank() + FAR_ABOVE;
	}

	/** At night a shadow wolf hunts the harder (a quarter more speed and harm). */
	private boolean nightHunter() {
		return kind().signature == BeastKind.Signature.PACK && level().isNight();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide) {
			for (int i = 0; i < acted.length; i++) {
				actedBefore[i] = acted[i];
				acted[i] = Mth.approach(acted[i], acting(1 << i) ? 1 : 0, 1f / ACT_EASE);
			}
		}
		// One that came into the world with no rank (made by a command or another mod without its spawning, finalizeSpawn) is given one
		// now, and its strength with it: it stood at base strength, as no rank at all, before the sweep of 2026-10-23.
		if (!level().isClientSide && getRank() < 0 && level() instanceof ServerLevel server) {
			setRank(rollRank(kind(), random, ModDimensions.isUpperRealm(server.dimension())));
		}
		// An elder, too great for most paths, goes straight at what it hunts when it can find no way there (it stood still, a target in
		// plain sight, a block or two over the land's lie).
		if (!level().isClientSide && kind().isElder() && kind().moves != BeastKind.Moves.FLYING && tickCount % 10 == 0) {
			LivingEntity target = getTarget();
			if (target != null && target.isAlive() && getNavigation().isDone() && distanceToSqr(target) > getBbWidth() * getBbWidth()) {
				getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.2);
			}
		}
		if (level().isClientSide || tickCount % 40 != 0) return;
		AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;
		boolean night = nightHunter();
		if (night != (speed.getModifier(NIGHT_HUNT) != null)) {
			speed.removeModifier(NIGHT_HUNT);
			if (night) speed.addTransientModifier(new AttributeModifier(NIGHT_HUNT, "Night hunt", 0.25, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		float bonus = nightHunter() ? 1.25f : 1.0f;
		AttributeInstance attack = getAttribute(Attributes.ATTACK_DAMAGE);
		double base = attack == null ? 3 : attack.getValue();
		boolean hit = target.hurt(damageSources().mobAttack(this), (float) base * bonus);
		if (!hit) return false;
		doEnchantDamageEffects(this, target);
		setLastHurtMob(target);
		if (target instanceof LivingEntity living) DemonBeastGoals.onBite(this, living);
		return true;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// In its shell (a turtle's, drawn in: DemonBeastGoals), little gets through.
		if (acting(ACT_SHELL)) amount *= 0.2f;
		boolean hurt = super.hurt(source, amount);
		if (hurt && !level().isClientSide) DemonBeastGoals.onHurt(this, source);
		return hurt;
	}

	// --- Where it moves ---

	@Override
	public void travel(Vec3 travel) {
		// A water beast swims as a dolphin does, freely in three dimensions.
		if (kind().moves == BeastKind.Moves.SWIMMING && isEffectiveAi() && isInWater()) {
			moveRelative(getSpeed(), travel);
			move(MoverType.SELF, getDeltaMovement());
			setDeltaMovement(getDeltaMovement().scale(0.9));
			if (getTarget() == null) setDeltaMovement(getDeltaMovement().add(0, -0.005, 0));
			return;
		}
		super.travel(travel);
	}

	@Override public boolean canBreatheUnderwater() { return kind().moves == BeastKind.Moves.AMPHIBIOUS || kind().moves == BeastKind.Moves.SWIMMING; }
	@Override public boolean isPushedByFluid() { return kind().moves != BeastKind.Moves.SWIMMING; }
	@Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		return kind().moves != BeastKind.Moves.FLYING && super.causeFallDamage(distance, multiplier, source);
	}
	@Override protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
		if (kind().moves != BeastKind.Moves.FLYING) super.checkFallDamage(y, onGround, state, pos);
	}
	@Override public boolean fireImmune() { return kind().element == QiElement.FIRE || super.fireImmune(); }

	/** It wanders where it likes, by day or night (a monster would keep to the dark). */
	@Override public float getWalkTargetValue(BlockPos pos, LevelReader level) { return 0.0f; }

	// --- Its voice ---

	@Override protected SoundEvent getAmbientSound() { return voice(0); }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return voice(1); }
	@Override protected SoundEvent getDeathSound() { return voice(2); }
	@Override public int getAmbientSoundInterval() { return 160; }

	/** Its voice (its cry, its hurt, its death): an animal's of its build, borrowed from the world's own beasts. */
	private SoundEvent voice(int which) {
		SoundEvent[] three = switch (kind()) {
			case IRON_HIDE_BOAR -> new SoundEvent[] {SoundEvents.HOGLIN_AMBIENT, SoundEvents.HOGLIN_HURT, SoundEvents.HOGLIN_DEATH};
			case SHADOW_WOLF -> new SoundEvent[] {SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HURT, SoundEvents.WOLF_DEATH};
			case JADE_VIPER, JADE_SCALED_PYTHON, IRON_JAW_CROCODILE -> new SoundEvent[] {SoundEvents.CAT_HISS, SoundEvents.SILVERFISH_HURT, SoundEvents.SILVERFISH_DEATH};
			case ROCK_SHELL_TURTLE -> new SoundEvent[] {SoundEvents.TURTLE_AMBIENT_LAND, SoundEvents.TURTLE_HURT, SoundEvents.TURTLE_DEATH};
			case WIND_HAWK, GOLDEN_WINGED_ROC -> new SoundEvent[] {SoundEvents.PHANTOM_AMBIENT, SoundEvents.PHANTOM_HURT, SoundEvents.PHANTOM_DEATH};
			case FIRE_MONKEY -> new SoundEvent[] {SoundEvents.PIGLIN_AMBIENT, SoundEvents.PIGLIN_HURT, SoundEvents.PIGLIN_DEATH};
			case BLOODFANG_TIGER, HORNED_THUNDER_RHINO -> new SoundEvent[] {SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH};
			case FROST_APE, EARTHQUAKE_BEAR -> new SoundEvent[] {SoundEvents.POLAR_BEAR_AMBIENT, SoundEvents.POLAR_BEAR_HURT, SoundEvents.POLAR_BEAR_DEATH};
			case LIGHTNING_ANTELOPE -> new SoundEvent[] {SoundEvents.GOAT_AMBIENT, SoundEvents.GOAT_HURT, SoundEvents.GOAT_DEATH};
			case EMBER_FOX, NINE_TAILED_FOX -> new SoundEvent[] {SoundEvents.FOX_AMBIENT, SoundEvents.FOX_HURT, SoundEvents.FOX_DEATH};
			case FLOOD_DRAGON -> new SoundEvent[] {SoundEvents.GUARDIAN_AMBIENT, SoundEvents.GUARDIAN_HURT, SoundEvents.GUARDIAN_DEATH};
			case ELDER_WHITE_TIGER -> new SoundEvent[] {SoundEvents.RAVAGER_AMBIENT, SoundEvents.RAVAGER_HURT, SoundEvents.RAVAGER_DEATH};
			case MOUNTAIN_LIZARD_KING -> new SoundEvent[] {SoundEvents.SNIFFER_IDLE, SoundEvents.SNIFFER_HURT, SoundEvents.SNIFFER_DEATH};
			case AZURE_DRAGON -> new SoundEvent[] {SoundEvents.ENDER_DRAGON_AMBIENT, SoundEvents.ENDER_DRAGON_HURT, SoundEvents.ENDER_DRAGON_DEATH};
			// The Kun's deep song (the Peng's cry its own: PrimordialBeast), the Phoenix's scream, the True Dragon's roar.
			case KUN_PENG -> new SoundEvent[] {SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundEvents.ELDER_GUARDIAN_HURT, SoundEvents.ELDER_GUARDIAN_DEATH};
			case TRUE_PHOENIX -> new SoundEvent[] {SoundEvents.ENDER_DRAGON_AMBIENT, SoundEvents.BLAZE_HURT, SoundEvents.BLAZE_DEATH};
			case TRUE_DRAGON -> new SoundEvent[] {SoundEvents.ENDER_DRAGON_GROWL, SoundEvents.ENDER_DRAGON_HURT, SoundEvents.ENDER_DRAGON_DEATH};
			// A Chaos Fiend's wail; the Primordial Chaos's deep, faceless throb.
			case CHAOS_FIEND -> new SoundEvent[] {SoundEvents.VEX_AMBIENT, SoundEvents.VEX_HURT, SoundEvents.VEX_DEATH};
			case PRIMORDIAL_CHAOS -> new SoundEvent[] {SoundEvents.WARDEN_AMBIENT, SoundEvents.WARDEN_HURT, SoundEvents.WARDEN_DEATH};
		};
		return three[which];
	}

	/** Its voice's pitch: its kind's (a tiger's growl the higher, a python's hiss the deeper), deeper the larger it has grown. */
	@Override
	public float getVoicePitch() {
		float kindPitch = switch (kind()) {
			case BLOODFANG_TIGER, WIND_HAWK -> 1.35f;
			case FIRE_MONKEY -> 1.5f;
			case JADE_VIPER -> 1.2f;
			case JADE_SCALED_PYTHON, IRON_JAW_CROCODILE, GOLDEN_WINGED_ROC -> 0.6f;
			case FROST_APE -> 0.8f;
			case ELDER_WHITE_TIGER, AZURE_DRAGON -> 0.85f;
			case NINE_TAILED_FOX -> 0.9f;
			case MOUNTAIN_LIZARD_KING -> 0.7f;
			case KUN_PENG -> 0.5f;
			case TRUE_PHOENIX -> 1.45f;
			case TRUE_DRAGON -> 0.6f;
			case CHAOS_FIEND -> 0.55f;
			case PRIMORDIAL_CHAOS -> 0.5f;
			default -> 1.0f;
		};
		return super.getVoicePitch() * kindPitch / (float) Math.sqrt(scale());
	}

	// --- What it leaves ---

	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		int realm = Math.min(CORE_CHANCE.length - 1, getCultivationRealm().ordinal());
		if (random.nextFloat() < CORE_CHANCE[realm]) spawnAtLocation(DemonCoreItem.of(Math.max(0, getRank()), kind().element, kind().getDisplayName()));
		// A beast of a demon race's kin leaves that race's blood, of its realm, as often as its core (DemonBlood#kin); an elder, half the
		// time (the blood it guarded is the prize).
		Bloodline kin = DemonBlood.kin(kind());
		float bloodChance = kind().isElder() ? ELDER_BLOOD_CHANCE : CORE_CHANCE[realm];
		if (kin != null && random.nextFloat() < bloodChance) spawnAtLocation(DemonBloodItem.of(kin, getCultivationRealm()));
		for (net.minecraft.world.item.Item remains : kind().remains()) {
			int count = random.nextInt(2 + looting) + (getRank() >= kind().maxRank() - 1 ? 1 : 0);
			if (count > 0) spawnAtLocation(new ItemStack(remains, count));
		}
	}

	// --- Saving ---

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Rank", getRank());
		if (guards != null) tag.putLong("Guards", guards.asLong());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Guards")) guard(BlockPos.of(tag.getLong("Guards")));
		if (tag.contains("Rank") && tag.getInt("Rank") >= 0) {
			float health = getHealth();
			holdRank(tag.getInt("Rank"));
			// Its health as saved; given a rank with none (a summon's {Rank:..}), whole at it, not the base's 20 of a far greater whole.
			setHealth(tag.contains("Health", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? health : getMaxHealth());
		}
	}

	// --- GeckoLib (its look: BeastModels on the client) ---

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "idle", 5, state -> state.setAndContinue(IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

	/** An effect a beast's move lays on whoever it strikes, briefly (for DemonBeastGoals). */
	static void afflict(LivingEntity target, net.minecraft.world.effect.MobEffect effect, int ticks, int amplifier, DemonBeast source) {
		target.addEffect(new MobEffectInstance(effect, ticks, amplifier), source);
	}
}
