package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A secret realm's guardian (SecretRealms, HerbValleyWorks), made of the realm's own matter the way the spells' beasts are made of their
 * elements: in the Verdant Herb Valley a <b>Vine Keeper</b> (ModEntities#VINE_KEEPER), a stooped thing of old wood and living vines lying by
 * a herb bed like a stump overgrown, waking when a herb is taken from its bed or it is struck; and a <b>Bronze Puppet</b>
 * (ModEntities#BRONZE_PUPPET), an alchemists' guard of green-rusted bronze standing still among the ruins, waking when anyone comes close, its
 * vault is forced, or it is struck. Each is of its rank (CultivationStats#applyNpc), kept to its post (it goes back there and sleeps again
 * when nobody is left to fight), and belongs to one opening of its realm: the realm closed, it crumbles.
 * <p>
 * <b>The Gatekeeper</b> is a puppet before the Pill Sage's hall, its first trial: woken by one who challenges it (right-click, or a blow),
 * it fights them alone, matched to their own strength ({@link #challenge}); brought down, it kneels and is whole again, and its challenger
 * has passed (HerbValleyWorks#passedGate). Nobody else's blows touch it while it fights.
 */
public class RealmGuardian extends PathfinderMob implements GeoEntity, Enemy {
	/**
	 * Each kind's make: how tough for its rank (CultivationStats#applyNpc), how quick, how far its blow reaches, how long between blows; whether
	 * it wakes for whoever comes close (the Vine Keeper only when its bed is touched or it is struck).
	 * The second phase's (2026-10-31): the Sunken Sword Tomb's <b>Sword Wraith</b> (a fallen sword's spirit, ModEntities#SWORD_WRAITH; the
	 * sleeping sword's own is its duelist), the Ancient Battlefield's <b>Bone Soldier</b> (an old soldier risen in the armour it died in), its
	 * <b>Battle Spirit</b> (one in light, come up the General's hill against whoever holds it) and the <b>General's Spirit</b> (the last of them).
	 * The third phase's: the Primeval Beast Graveyard's <b>Dragon's Echo</b> (the dead dragon risen as a shade over its bones, its trial's
	 * guardian), a long reach for its long neck; the Cave Abode's <b>Master's Puppet</b> (his own, of white jade and gold, before his chamber),
	 * tougher and quicker than the bronze ones; the Yellow Springs Hollow's <b>Drowned Shade</b> (a soldier of the drowned city's garrison, risen
	 * in the springs' light) and its <b>Ferryman</b> (at his landing: he judges who would cross, and fights the devilish with his pole). The
	 * Hollow's are of the dead ({@link #ofTheDead}).
	 */
	public enum Kind {
		VINE_KEEPER(0.95, 0.25, 3.2, 28, false), BRONZE_PUPPET(1.15, 0.21, 2.6, 38, true), SWORD_WRAITH(0.85, 0.30, 3.0, 22, true),
		BONE_SOLDIER(0.9, 0.24, 3.4, 30, true), BATTLE_SPIRIT(0.8, 0.27, 3.4, 28, true), GENERAL_SPIRIT(1.4, 0.25, 3.8, 26, true),
		DRAGON_ECHO(1.3, 0.26, 4.6, 26, true), MASTER_PUPPET(1.3, 0.25, 3.0, 30, true), DROWNED_SHADE(0.85, 0.26, 3.4, 28, true),
		FERRYMAN(1.25, 0.24, 4.0, 30, true), MIRROR_SHADE(0.9, 0.27, 3.2, 26, true), HOLY_REMNANT(1.0, 0.25, 3.4, 28, true),
		DECAYED_GUARD(1.05, 0.24, 3.4, 30, true), DECAYED_IMMORTAL(1.45, 0.25, 4.2, 26, true);

		final double toughness, speed, reach;
		final int cooldown;
		final boolean wakesNear;

		Kind(double toughness, double speed, double reach, int cooldown, boolean wakesNear) {
			this.toughness = toughness;
			this.speed = speed;
			this.reach = reach;
			this.cooldown = cooldown;
			this.wakesNear = wakesNear;
		}
	}

	private static final EntityDataAccessor<Integer> DATA_RANK = SynchedEntityData.defineId(RealmGuardian.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(RealmGuardian.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_GATEKEEPER = SynchedEntityData.defineId(RealmGuardian.class, EntityDataSerializers.BOOLEAN);

	/** How far from its post it keeps, follows a foe, and watches for one awake; how near a dormant puppet lets anyone come. */
	public static final int LEASH = 24, WATCH = 14, WAKE_NEAR = 5;
	/** Awake with nobody to fight this long (ticks) at its post, it sleeps again. */
	static final int SETTLE_TICKS = 160;

	private BlockPos home;
	/** The opening of its realm it belongs to (SecretRealms.Record#openings), and its place in the realm's works (HerbValleyWorks). */
	private int opening = -1;
	private String slot = "";
	private SecretRealm realm = SecretRealm.VERDANT_HERB_VALLEY;
	/** The Gatekeeper's challenger now (null: none), and the rank it holds itself to while it lasts. */
	private UUID challenger;
	private int idle;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public RealmGuardian(EntityType<? extends RealmGuardian> type, Level level) {
		super(type, level);
		setPersistenceRequired();
		xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.ARMOR, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_RANK, 0);
		entityData.define(DATA_DORMANT, true);
		entityData.define(DATA_GATEKEEPER, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new Strike());
		goalSelector.addGoal(4, new Home());
		targetSelector.addGoal(1, new HurtByTargetGoal(this) {
			@Override
			public boolean canUse() { return !isGatekeeper() && super.canUse(); }
		});
		targetSelector.addGoal(2, new Watch());
	}

	public Kind kind() {
		EntityType<?> type = getType();
		if (type == ModEntities.VINE_KEEPER) return Kind.VINE_KEEPER;
		if (type == ModEntities.SWORD_WRAITH) return Kind.SWORD_WRAITH;
		if (type == ModEntities.BONE_SOLDIER) return Kind.BONE_SOLDIER;
		if (type == ModEntities.BATTLE_SPIRIT) return Kind.BATTLE_SPIRIT;
		if (type == ModEntities.GENERAL_SPIRIT) return Kind.GENERAL_SPIRIT;
		if (type == ModEntities.DRAGON_ECHO) return Kind.DRAGON_ECHO;
		if (type == ModEntities.MASTER_PUPPET) return Kind.MASTER_PUPPET;
		if (type == ModEntities.DROWNED_SHADE) return Kind.DROWNED_SHADE;
		if (type == ModEntities.FERRYMAN) return Kind.FERRYMAN;
		if (type == ModEntities.MIRROR_SHADE) return Kind.MIRROR_SHADE;
		if (type == ModEntities.HOLY_REMNANT) return Kind.HOLY_REMNANT;
		if (type == ModEntities.DECAYED_GUARD) return Kind.DECAYED_GUARD;
		if (type == ModEntities.DECAYED_IMMORTAL) return Kind.DECAYED_IMMORTAL;
		return Kind.BRONZE_PUPPET;
	}

	/** What it sounds like woken, struck, striking and falling, by its kind. */
	private SoundEvent sound(int what) {
		return switch (kind()) {
			case VINE_KEEPER -> new SoundEvent[] {SoundEvents.ROOTED_DIRT_BREAK, SoundEvents.AZALEA_LEAVES_HIT, SoundEvents.BIG_DRIPLEAF_TILT_DOWN, SoundEvents.ROOTED_DIRT_BREAK}[what];
			case SWORD_WRAITH -> new SoundEvent[] {SoundEvents.TRIDENT_RETURN, SoundEvents.CHAIN_HIT, SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.ITEM_BREAK}[what];
			case BONE_SOLDIER -> new SoundEvent[] {SoundEvents.SKELETON_AMBIENT, SoundEvents.SKELETON_HURT, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundEvents.SKELETON_DEATH}[what];
			case BATTLE_SPIRIT -> new SoundEvent[] {SoundEvents.SOUL_ESCAPE, SoundEvents.VEX_HURT, SoundEvents.VEX_CHARGE, SoundEvents.SOUL_ESCAPE}[what];
			case GENERAL_SPIRIT -> new SoundEvent[] {SoundEvents.WITHER_SKELETON_AMBIENT, SoundEvents.WITHER_SKELETON_HURT, SoundEvents.IRON_GOLEM_ATTACK,
					SoundEvents.WITHER_SKELETON_DEATH}[what];
			case DRAGON_ECHO -> new SoundEvent[] {SoundEvents.ENDER_DRAGON_GROWL, SoundEvents.ENDER_DRAGON_HURT, SoundEvents.EVOKER_FANGS_ATTACK,
					SoundEvents.ENDER_DRAGON_DEATH}[what];
			case DROWNED_SHADE -> new SoundEvent[] {SoundEvents.DROWNED_AMBIENT_WATER, SoundEvents.DROWNED_HURT_WATER, SoundEvents.DROWNED_SHOOT,
					SoundEvents.DROWNED_DEATH_WATER}[what];
			case FERRYMAN -> new SoundEvent[] {SoundEvents.SOUL_ESCAPE, SoundEvents.WOOD_HIT, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundEvents.SOUL_ESCAPE}[what];
			case MIRROR_SHADE -> new SoundEvent[] {SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.GLASS_HIT, SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.GLASS_BREAK}[what];
			case HOLY_REMNANT -> new SoundEvent[] {SoundEvents.BEACON_AMBIENT, SoundEvents.CHAIN_HIT, SoundEvents.PLAYER_ATTACK_STRONG, SoundEvents.BEACON_DEACTIVATE}[what];
			case DECAYED_GUARD, DECAYED_IMMORTAL -> new SoundEvent[] {SoundEvents.WITHER_SKELETON_AMBIENT, SoundEvents.WITHER_SKELETON_HURT,
					SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundEvents.WITHER_SKELETON_DEATH}[what];
			default -> new SoundEvent[] {SoundEvents.IRON_GOLEM_REPAIR, SoundEvents.COPPER_HIT, SoundEvents.IRON_GOLEM_ATTACK, SoundEvents.IRON_GOLEM_DEATH}[what];
		};
	}

	/** The motes it gives off as it wakes, by its kind. */
	private net.minecraft.core.particles.ParticleOptions motes() {
		return switch (kind()) {
			case VINE_KEEPER -> ParticleTypes.COMPOSTER;
			case SWORD_WRAITH -> ParticleTypes.END_ROD;
			case BONE_SOLDIER -> ParticleTypes.ASH;
			case BATTLE_SPIRIT, GENERAL_SPIRIT -> ParticleTypes.SOUL;
			case DRAGON_ECHO -> ParticleTypes.HAPPY_VILLAGER;
			case MASTER_PUPPET -> ParticleTypes.END_ROD;
			case DROWNED_SHADE -> ParticleTypes.BUBBLE_POP;
			case FERRYMAN -> ParticleTypes.SOUL_FIRE_FLAME;
			case MIRROR_SHADE -> ParticleTypes.WAX_OFF;
			case HOLY_REMNANT -> ParticleTypes.END_ROD;
			case DECAYED_GUARD, DECAYED_IMMORTAL -> ParticleTypes.ASH;
			default -> ParticleTypes.WAX_ON;
		};
	}

	public int getRank() { return entityData.get(DATA_RANK); }

	public boolean isDormant() { return entityData.get(DATA_DORMANT); }

	public boolean isGatekeeper() { return entityData.get(DATA_GATEKEEPER); }

	public BlockPos home() { return home; }

	public String slot() { return slot; }

	/** Set at its post: of {@code rank}, for {@code realm}'s opening {@code opening}, in its works' {@code slot}; the Gatekeeper if {@code gatekeeper}. */
	public void setUp(SecretRealm realm, int rank, BlockPos home, int opening, String slot, boolean gatekeeper) {
		this.realm = realm;
		this.home = home.immutable();
		this.opening = opening;
		this.slot = slot;
		entityData.set(DATA_GATEKEEPER, gatekeeper);
		setRank(rank);
		setDormant(true);
	}

	public void setRank(int rank) {
		entityData.set(DATA_RANK, Math.max(0, rank));
		applyStats();
		setHealth(getMaxHealth());
	}

	private void applyStats() {
		int rank = getRank();
		CultivationStats.applyNpc(this, CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank), false, kind().toughness);
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.setBaseValue(kind().speed);
	}

	public void setDormant(boolean dormant) {
		entityData.set(DATA_DORMANT, dormant);
		if (dormant) {
			setTarget(null);
			getNavigation().stop();
			idle = 0;
		}
	}

	/** Woken (by its bed touched, its vault forced, a blow): it looks for whom to fight, {@code foe} first if one is given. */
	public void wake(LivingEntity foe) {
		if (isGatekeeper()) return;
		boolean was = isDormant();
		setDormant(false);
		if (foe != null && foe.isAlive() && validFoe(foe)) setTarget(foe);
		if (was && level() instanceof ServerLevel level) {
			level.playSound(null, blockPosition(), sound(0), SoundSource.HOSTILE, 1.2f, 0.6f);
			level.sendParticles(motes(), getX(), getY() + getBbHeight() * 0.6, getZ(), 16, 0.4, 0.6, 0.4, 0.05);
		}
	}

	/**
	 * Whether it is one of the dead's own: a guardian of a realm of the dead (the Yellow Springs Hollow's, SecretRealm#admitsDead). It sees and
	 * strikes the dead, and they it, as the dead do each other (GhostSystem#isDead); the living's blows land on it all the same. By where it
	 * stands: both sides.
	 */
	public boolean ofTheDead() {
		if (!ModDimensions.isSecretRealm(level().dimension())) return false;
		SecretRealm at = SecretRealm.at(getX(), getZ());
		return at != null && at.admitsDead();
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		Boolean dead = ofTheDead() ? GhostSystem.deadMayAttack(this, target) : null;
		return dead != null ? dead : super.canAttack(target);
	}

	/** Whom a guardian fights: those who came into its realm (a player not in creative nor looking on, or one of the cultivators who came in). */
	public static boolean validFoe(LivingEntity e) {
		if (!e.isAlive()) return false;
		if (e instanceof Player p) return !p.isCreative() && !p.isSpectator() && !Incorporeal.is(p);
		return e instanceof CultivatorNpc npc && !npc.isVacant() && npc.livesItsLife(); // never a visitor's reflection (MirrorReflection)
	}

	// --- The Gatekeeper ---

	/**
	 * {@code p} challenges the Gatekeeper: unless it already fights someone, it wakes matched to their strength (their higher path, as the
	 * realm lets them wield it; never weaker than the realm it is meant for) and fights them alone.
	 */
	public void challenge(LivingEntity p) {
		if (!isGatekeeper() || !validFoe(p)) return;
		if (challenger != null && !challenger.equals(p.getUUID())) {
			if (p instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable(ModLang.MSG_GATEKEEPER_BUSY), true);
			return;
		}
		if (challenger != null) return;
		// Some realms' duelists stir only for one who has passed what comes before (the Sword Path: RealmWorks#mayChallenge); some let one
		// by unfought (the Ferryman: the righteous, and those who pay his toll: RealmWorks#passesUnfought).
		if (level() instanceof ServerLevel level) {
			SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
			Component why = realm.works().mayChallenge(r, p);
			if (why != null) {
				if (p instanceof ServerPlayer sp) sp.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
				return;
			}
			if (realm.works().passesUnfought(level, r, p)) {
				RealmWorks.passedGate(level, realm, p.getUUID(), p);
				return;
			}
		}
		challenger = p.getUUID();
		int strength = p instanceof ServerPlayer sp ? TwoPaths.higherRank(CultivationManager.get(sp), true) : p instanceof CultivatorNpc npc ? npc.sustainedRank() : 0;
		setRank(Math.max(realm.guardianRank(Stage.EARLY), strength));
		entityData.set(DATA_DORMANT, false);
		setTarget(p);
		if (level() instanceof ServerLevel level) {
			level.playSound(null, blockPosition(), sound(0), SoundSource.HOSTILE, 1.5f, 0.5f);
			level.sendParticles(motes(), getX(), getY() + 1.5, getZ(), 30, 0.5, 0.8, 0.5, 0.1);
		}
		if (p instanceof ServerPlayer sp) {
			sp.sendSystemMessage(realm.works().challengeMessage(PlayerCultivation.rankName(CultivatorNpc.realmOf(getRank()), CultivatorNpc.stageOf(getRank())))
					.withStyle(ChatFormatting.GOLD));
		}
	}

	/** The challenge over without a winner (its challenger gone, fallen or fled): whole again, still at its post. */
	private void resetGate() {
		challenger = null;
		setHealth(getMaxHealth());
		setDormant(true);
		if (home != null) teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !isGatekeeper()) return super.mobInteract(player, hand);
		// Its realm's own answer first, if it has one (the Ferryman ferries one he has judged: RealmWorks#guardianTouched); else the challenge.
		if (level() instanceof ServerLevel level && !realm.works().guardianTouched(level, SecretRealms.Data.get(level.getServer()).of(realm), this, player)) challenge(player);
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity by = source.getEntity();
		if (isGatekeeper()) {
			if (level().isClientSide) return false;
			if (by instanceof LivingEntity living && challenger == null) challenge(living);
			// Only its challenger's blows touch it; brought down, it kneels: they have passed.
			if (by == null || !by.getUUID().equals(challenger)) return false;
			if (amount >= getHealth()) {
				UUID winner = challenger;
				resetGate();
				if (level() instanceof ServerLevel level) {
					level.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0f, 0.6f);
					level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.4, getZ(), 40, 0.5, 0.9, 0.5, 0.08);
					RealmWorks.passedGate(level, realm, winner, by);
				}
				return false;
			}
			return super.hurt(source, amount);
		}
		if (isDormant() && by instanceof LivingEntity living && !level().isClientSide) wake(living);
		return super.hurt(source, amount);
	}

	// --- Each tick ---

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!(level() instanceof ServerLevel level)) return;
		if (tickCount % 20 == 0 && stale(level)) {
			discard();
			return;
		}
		if (isDormant()) {
			getNavigation().stop();
			// A puppet among the ruins, a wraith, a fallen soldier wakes for whoever comes close (a keeper only for its bed or a blow).
			if (kind().wakesNear && !isGatekeeper() && tickCount % 10 == 0) {
				List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(WAKE_NEAR), RealmGuardian::validFoe);
				if (!near.isEmpty()) wake(near.get(0));
			}
			return;
		}
		if (isGatekeeper() && tickCount % 20 == 0) {
			Entity foe = challenger == null ? null : level.getEntity(challenger);
			if (!(foe instanceof LivingEntity living) || !living.isAlive() || home != null && living.distanceToSqr(home.getX(), home.getY(), home.getZ()) > LEASH * LEASH) resetGate();
		}
	}

	/** Belonging to an opening of its realm that has ended (or none), or with no post: it crumbles. */
	private boolean stale(ServerLevel level) {
		if (home == null) return true;
		SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
		return r.openedAt < 0 || r.openings != opening;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && !slot.isEmpty()) RealmWorks.guardianSlain(level, realm, slot);
	}

	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		switch (kind()) {
			case VINE_KEEPER -> {
				spawnAtLocation(new ItemStack(ModItems.SPIRIT_DEW, 1 + random.nextInt(2)));
				if (random.nextFloat() < 0.5f) spawnAtLocation(new ItemStack(random.nextBoolean() ? ModItems.GINSENG_SEEDS : ModItems.LINGZHI_SPORES));
			}
			case SWORD_WRAITH -> {
				spawnAtLocation(new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(4)));
				if (random.nextFloat() < 0.3f) spawnAtLocation(new ItemStack(ModItems.LOW_SPIRIT_STONE, 1 + random.nextInt(2)));
				if (random.nextFloat() < 0.05f) spawnAtLocation(Artifacts.create(ArtifactKind.FLYING_SWORD, ArtifactGrade.SPIRIT, Artifacts.Quality.NORMAL, null));
			}
			case BONE_SOLDIER -> {
				spawnAtLocation(new ItemStack(Items.BONE, 1 + random.nextInt(3)));
				spawnAtLocation(new ItemStack(Items.IRON_NUGGET, 1 + random.nextInt(3)));
				if (random.nextFloat() < 0.2f) spawnAtLocation(new ItemStack(ModItems.LOW_SPIRIT_STONE, 1 + random.nextInt(2)));
				if (random.nextFloat() < 0.04f) spawnAtLocation(Artifacts.create(ArtifactKind.SPEAR, ArtifactGrade.SPIRIT, Artifacts.Quality.NORMAL, null));
			}
			case BATTLE_SPIRIT, GENERAL_SPIRIT, DRAGON_ECHO, FERRYMAN -> {
				// Light, gone back into the hill (the echo into its bones, the Ferryman into his mist): nothing is left of them.
			}
			case DROWNED_SHADE -> {
				// The garrison's pay, still on it: Yin Jade, now and then a spirit stone.
				spawnAtLocation(new ItemStack(ModItems.YIN_JADE, 1 + random.nextInt(2)));
				if (random.nextFloat() < 0.25f) spawnAtLocation(new ItemStack(ModItems.LOW_SPIRIT_STONE, 1 + random.nextInt(3)));
			}
			case MIRROR_SHADE -> {
				// Glass and light: a shard of the lake's jade, now and then a spirit stone.
				spawnAtLocation(new ItemStack(ModItems.JADE, 1 + random.nextInt(2)));
				if (random.nextFloat() < 0.25f) spawnAtLocation(new ItemStack(ModItems.HIGH_SPIRIT_STONE));
			}
			case HOLY_REMNANT -> {
				// What the holy land paid its disciples: spirit stones, now and then a supreme one.
				spawnAtLocation(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 1 + random.nextInt(2)));
				if (random.nextFloat() < 0.15f) spawnAtLocation(new ItemStack(ModItems.SUPREME_SPIRIT_STONE));
			}
			case DECAYED_GUARD -> {
				// Its rotted gold: a supreme spirit stone now and then, an immortal one rarely.
				if (random.nextFloat() < 0.4f) spawnAtLocation(new ItemStack(ModItems.SUPREME_SPIRIT_STONE));
				if (random.nextFloat() < 0.06f) spawnAtLocation(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE));
			}
			case DECAYED_IMMORTAL -> {
				// His spirit, gone back into his decay: nothing is left of it.
			}
			default -> {
				spawnAtLocation(new ItemStack(Items.COPPER_INGOT, 1 + random.nextInt(3)));
				if (random.nextFloat() < 0.5f) spawnAtLocation(new ItemStack(ModItems.LOW_SPIRIT_STONE, 1 + random.nextInt(2)));
			}
		}
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		triggerAnim("main", "strike");
		boolean hit = super.doHurtTarget(target);
		if (hit && level() instanceof ServerLevel level) level.playSound(null, blockPosition(), sound(2), SoundSource.HOSTILE, 1.0f, 0.7f);
		return hit;
	}

	/** Set on {@code foe} at once, awake, kept to it (a wave come up the General's hill, the dead risen: RealmWorks' own). */
	public void charge(LivingEntity foe) {
		setDormant(false);
		focus = foe.getUUID();
		setTarget(foe);
	}

	/** The one it was set on ({@link #charge}), or null: it watches for no one else. */
	private UUID focus;

	@Override
	public boolean removeWhenFarAway(double distance) { return false; }

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return sound(1);
	}

	@Override
	protected SoundEvent getDeathSound() {
		return sound(3);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("Rank", getRank());
		tag.putBoolean("Dormant", isDormant());
		tag.putBoolean("Gatekeeper", isGatekeeper());
		if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
		tag.putInt("Opening", opening);
		tag.putString("Slot", slot);
		tag.putString("Realm", realm.getId());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(DATA_GATEKEEPER, tag.getBoolean("Gatekeeper"));
		entityData.set(DATA_RANK, tag.getInt("Rank"));
		applyStats();
		entityData.set(DATA_DORMANT, tag.getBoolean("Dormant"));
		home = tag.contains("Home") ? NbtUtils.readBlockPos(tag.getCompound("Home")) : null;
		opening = tag.getInt("Opening");
		slot = tag.getString("Slot");
		SecretRealm r = SecretRealm.byId(tag.getString("Realm"));
		if (r != null) realm = r;
	}

	// --- Its goals ---

	/** Awake with a foe: after it (within its leash of its post), striking when in reach. */
	private final class Strike extends Goal {
		private int cooldown;

		Strike() {
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = getTarget();
			return !isDormant() && target != null && target.isAlive();
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void tick() {
			LivingEntity target = getTarget();
			if (target == null) return;
			if (focus == null && home != null && target.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > LEASH * LEASH) {
				setTarget(null); // gone past its post's reach: let go
				return;
			}
			getLookControl().setLookAt(target, 30, 30);
			double reach = kind().reach;
			double d = distanceToSqr(target);
			if (d > reach * reach) {
				if (tickCount % 10 == 0) getNavigation().moveTo(target, 1.15);
			} else {
				getNavigation().stop();
			}
			if (--cooldown <= 0 && d <= reach * reach) {
				cooldown = kind().cooldown;
				swing(InteractionHand.MAIN_HAND);
				doHurtTarget(target);
			}
		}
	}

	/** Awake with nobody to fight: back to its post, and there, after a while, asleep again. */
	private final class Home extends Goal {
		Home() {
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !isDormant() && getTarget() == null && home != null;
		}

		// Every tick: a goal that isn't is ticked every other one, and tickCount's twentieth might never fall on its tick.
		@Override
		public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void tick() {
			double d = distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
			if (d > 2.5 * 2.5) {
				if (tickCount % 20 == 0) getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
				if (++idle > SETTLE_TICKS * 6) teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5); // stuck on the way: home all the same
			} else if (++idle > SETTLE_TICKS) {
				setDormant(true);
			}
		}
	}

	/** Awake: whoever of those who came in is nearest its post, within {@link #WATCH} (the Gatekeeper: its challenger only). */
	private final class Watch extends Goal {
		/** Asked every other tick (a goal's choosing is), so counted down, not told by tickCount's tenth (that may never fall on it). */
		private int askAgain;

		@Override
		public boolean canUse() {
			if (isDormant() || getTarget() != null || home == null || focus != null || --askAgain > 0) return false;
			askAgain = 5; // some ten ticks
			if (isGatekeeper()) return false;
			List<LivingEntity> near = level().getEntitiesOfClass(LivingEntity.class, new AABB(home).inflate(WATCH), RealmGuardian::validFoe);
			LivingEntity nearest = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity e : near) {
				double d = e.distanceToSqr(RealmGuardian.this);
				if (d < best) {
					best = d;
					nearest = e;
				}
			}
			if (nearest == null) return false;
			setTarget(nearest);
			idle = 0;
			return false;
		}
	}

	// --- Its animations (the client's: VineKeeperRenderer, BronzePuppetRenderer) ---

	private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("dormant"), IDLE = RawAnimation.begin().thenLoop("idle"),
			WALK = RawAnimation.begin().thenLoop("walk");

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<RealmGuardian> main = new AnimationController<>(this, "main", 6, state -> {
			if (isDormant()) return state.setAndContinue(DORMANT);
			return state.setAndContinue(state.isMoving() ? WALK : IDLE);
		});
		main.triggerableAnim("strike", RawAnimation.begin().thenPlay("strike"));
		controllers.add(main);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
