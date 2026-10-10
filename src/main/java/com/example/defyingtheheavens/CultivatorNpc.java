package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A cultivator who isn't a player: a member of a sect (with a title, see {@link Sect}) or a rogue wandering the world.
 * <p>
 * <b>Cultivation.</b> Its realm, stage, cultivation and qi live in a {@link PlayerCultivation}, so pools, gathering and
 * suppression follow exactly the players' formulas. It cultivates only while meditating, at a tenth of a player's rate
 * ({@link #NPC_RATE}; mats and clothing help it as they help players), and catches up for time its chunk spent unloaded
 * at {@link #CATCH_UP_SHARE} of that rate. It never faces a Heavenly Tribulation: bottlenecks simply give way, which the
 * slower rate pays for. The lower realms still cap it at Heavenly Being Grand Perfection.
 * <p>
 * <b>Life.</b> A sect member in the lower realms who reaches Heavenly Being leaves the sect, flying 200-1000 blocks away
 * ({@link Lifecycle#DEPARTING}). A Heavenly Being then seeks a cave or a mountain, sets down a mat and raises a small
 * Concealment Barrier to cultivate in seclusion ({@link Lifecycle#SECLUDED}): passive, unseen by consciousness domains,
 * pushing away anyone who comes close, and fighting only when attacked or when its barrier is struck. At Grand Perfection it
 * flies to the rift at Overworld 0, 0 and attempts the ascension ({@link Lifecycle#ASCENDING}). Out of every player's sight,
 * long journeys finish without it (see {@link NpcTravel}).
 * <p>
 * <b>Fighting.</b> Rogues always hold their consciousness domain open, sect members only in combat. In combat it presses down
 * on weaker foes with Realm Suppress and flies after flying ones, but only while its qi can carry the cost; it flees when badly
 * hurt or when it senses a hostile domain a realm or more above its own. The righteous and the demonic attack each other (and
 * players of the other path) on sight; see {@link CultivatorGoals} for who fights when.
 */
public class CultivatorNpc extends PathfinderMob implements CultivatorEntity {
	/** NPCs cultivate at this share of a player's rate. */
	public static final double NPC_RATE = 0.1;
	/** Time spent unloaded counts as meditating for this share of it. */
	public static final double CATCH_UP_SHARE = 0.5;
	/** Never catch up for more than this (3 Minecraft days) at once. */
	private static final long MAX_CATCH_UP = 72_000;
	/** A sect member territory reaches this far past the barrier. */
	public static final int TERRITORY_MARGIN = 30;
	/** Flee below this share of health. */
	public static final float FLEE_HEALTH = 0.25f;
	/** A hostile domain this many stages (a realm) above one's own sends a cultivator running. */
	public static final int SUPERIOR_DOMAIN_GAP = 4;
	/** How far it runs from such a domain (blocks, give or take {@link #FLEE_SPREAD}), and for how long at most (ticks). */
	public static final int FLEE_DISTANCE = 200;
	public static final int FLEE_SPREAD = 40;
	public static final int FLEE_TIMEOUT = 3600;
	/** Radius of a secluded cultivator's Concealment Barrier, and how far out its repulsion reaches. */
	public static final int CONCEALMENT_RADIUS = 4;
	private static final double REPULSE_REACH = CONCEALMENT_RADIUS + 3.5;
	/** Share of an ascension attempt that ends in the Upper Realm (the rest are crushed in the Spatial Gap). */
	public static final float ASCENSION_SUCCESS = 0.7f;

	public enum Lifecycle { SECT, ROGUE, DEPARTING, SEEKING_SECLUSION, SECLUDED, ASCENDING }

	private static final EntityDataAccessor<Integer> DATA_RANK = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_SHOWN_RANK = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_TITLE = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> DATA_ALIGNMENT = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BYTE);

	private static final int FLAG_MEDITATING = 1;
	private static final int FLAG_FLYING = 2;
	private static final int FLAG_CONCEALED = 4;
	private static final int FLAG_DOMAIN = 8;
	private static final int FLAG_SUPPRESSING = 16;
	public static final int SKINS = 6;

	/** Rank -1: a mortal who never opened their meridians. */
	public static final int MORTAL = -1;

	private final PlayerCultivation cultivation = new PlayerCultivation();
	private UUID sectId;
	private int homeIndex = -1;
	private BlockPos home;
	private Lifecycle lifecycle = Lifecycle.ROGUE;
	private BlockPos journeyTarget;
	private BlockPos seclusionMat;
	private UUID concealment;
	private long lastSeen = -1;
	/** Players this cultivator went for on its own (not in answer to them): killing it is then no murder. */
	private final Set<UUID> aggressed = new HashSet<>();
	/** Set while it is being taken out of the world to finish a journey elsewhere (NpcTravel), so that isn't a departure. */
	boolean travelling;
	private boolean caughtUp;
	private int seclusionAttempts;
	private int combatTicks;
	private Entity fleeFrom;
	private int fleeTicks;
	private BlockPos refuge;
	private int repulseCooldown;
	private double suppressUpkeep;
	private boolean landing;
	private int lastSteered;

	public CultivatorNpc(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		cultivation.setMortal(true);
		setCanPickUpLoot(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.ARMOR, 0.0);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_RANK, MORTAL);
		entityData.define(DATA_SHOWN_RANK, MORTAL);
		entityData.define(DATA_TITLE, (byte) NpcTitle.WANDERER.ordinal());
		entityData.define(DATA_ALIGNMENT, 0);
		entityData.define(DATA_FLAGS, (byte) 0);
		entityData.define(DATA_SKIN, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new CultivatorGoals.FleeGoal(this));
		goalSelector.addGoal(2, new CultivatorGoals.CombatGoal(this));
		goalSelector.addGoal(3, new CultivatorGoals.JourneyGoal(this));
		goalSelector.addGoal(3, new CultivatorGoals.SeclusionGoal(this));
		goalSelector.addGoal(4, new CultivatorGoals.ReturnToTerritoryGoal(this));
		goalSelector.addGoal(5, new CultivatorGoals.SectDutyGoal(this));
		goalSelector.addGoal(6, new CultivatorGoals.WanderGoal(this));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new CultivatorGoals.RetaliateGoal(this));
		targetSelector.addGoal(2, new CultivatorGoals.HostilityGoal(this));
		targetSelector.addGoal(3, new CultivatorGoals.GuardGoal(this));
	}

	// --- Identity ---

	public static Realm realmOf(int rank) { return Realm.byIndex(Math.max(0, rank) / Stage.values().length); }
	public static Stage stageOf(int rank) { return Stage.byIndex(Math.max(0, rank) % Stage.values().length); }

	/** True rank (realm and stage as one number), or {@link #MORTAL}. */
	public int getRank() { return entityData.get(DATA_RANK); }
	@Override public boolean isMortal() { return getRank() < 0; }
	public NpcTitle getTitle() { return NpcTitle.byIndex(entityData.get(DATA_TITLE)); }
	public int getAlignment() { return entityData.get(DATA_ALIGNMENT); }
	public Alignment.Faction getFaction() { return Alignment.factionOf(getAlignment()); }
	public int getSkin() { return entityData.get(DATA_SKIN); }
	public UUID getSectId() { return sectId; }
	public boolean isSectMember() { return sectId != null; }
	public Lifecycle getLifecycle() { return lifecycle; }
	public int getHomeIndex() { return homeIndex; }
	public BlockPos getHome() { return home; }
	public BlockPos getJourneyTarget() { return journeyTarget; }
	public BlockPos getSeclusionMat() { return seclusionMat; }
	public PlayerCultivation cultivation() { return cultivation; }

	private boolean flag(int flag) { return (entityData.get(DATA_FLAGS) & flag) != 0; }

	private void setFlag(int flag, boolean on) {
		byte flags = entityData.get(DATA_FLAGS);
		byte next = (byte) (on ? flags | flag : flags & ~flag);
		if (next != flags) entityData.set(DATA_FLAGS, next);
	}

	public boolean isMeditating() { return flag(FLAG_MEDITATING); }
	public void setMeditating(boolean meditating) { setFlag(FLAG_MEDITATING, meditating); }
	public boolean isQiFlying() { return flag(FLAG_FLYING); }
	@Override public boolean isConcealed() { return flag(FLAG_CONCEALED); }
	public boolean isDomainOpen() { return flag(FLAG_DOMAIN) && !isConcealed() && isAlive(); }
	public boolean isSuppressing() { return flag(FLAG_SUPPRESSING); }

	/** The realm it can wield where it stands (the lower realms cap it); what suppression and domains weigh. */
	@Override public Realm getCultivationRealm() { return realmOf(sustainedRank()); }
	@Override public Stage getCultivationStage() { return stageOf(sustainedRank()); }
	/** What others sense: lowered further by any pressure on it. */
	@Override public Realm getDisplayedRealm() { return realmOf(entityData.get(DATA_SHOWN_RANK)); }
	@Override public Stage getDisplayedStage() { return stageOf(entityData.get(DATA_SHOWN_RANK)); }

	public int sustainedRank() {
		if (isMortal()) return MORTAL;
		boolean lower = ModDimensions.isLowerRealm(level().dimension());
		int cap = PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE);
		return lower ? Math.min(getRank(), cap) : getRank();
	}

	/** Consciousness domain radius, from what it shows. */
	public double domainRadius() {
		return isMortal() ? 0 : ConsciousnessDomainHandler.radius(getDisplayedRealm(), getDisplayedStage());
	}

	public Component describe() {
		return Component.translatable(ModLang.NPC_TITLE_BRACKETS, getTitle().getDisplayName());
	}

	// --- Setting up ---

	/** Makes this a cultivator of {@code rank} ({@link #MORTAL} for a mortal), re-applying its realm's strength. */
	public void setRank(int rank) {
		rank = Math.max(MORTAL, Math.min(rank, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)));
		if (rank < 0) {
			cultivation.setMortal(true);
		} else {
			double kept = getRank() == rank ? cultivation.getCultivation() : 0;
			cultivation.setState(realmOf(rank), stageOf(rank), kept);
		}
		entityData.set(DATA_RANK, rank);
		refreshStats();
	}

	public void setAlignment(int alignment) { entityData.set(DATA_ALIGNMENT, Alignment.clamp(alignment)); }

	public void setTitle(NpcTitle title) {
		NpcTitle before = getTitle();
		entityData.set(DATA_TITLE, (byte) title.ordinal());
		// Promoted past the clothing it wears: it dresses for its new station.
		if (before != title && !level().isClientSide && title.isSectTitle()) upgradeClothing(ClothingStyle.of(getFaction()), clothingTier(title, getRank(), random));
	}

	public void joinSect(Sect sect, int homeIndex) {
		this.sectId = sect.id;
		this.homeIndex = homeIndex;
		this.home = sect.dwelling(homeIndex);
		this.lifecycle = Lifecycle.SECT;
		setAlignment(sect.alignment);
		setPersistenceRequired();
	}

	/** Strength from realm and stage, as players get it; speed by the square root, since a mob's speed counts twice. */
	private void refreshStats() {
		float oldMax = getMaxHealth();
		float oldHealth = getHealth();
		Realm realm = isMortal() ? Realm.QI_REFINING : realmOf(sustainedRank());
		Stage stage = isMortal() ? Stage.EARLY : stageOf(sustainedRank());
		CultivationStats.applyNpc(this, realm, stage, isMortal());
		if (oldMax > 0 && getMaxHealth() != oldMax) setHealth(Math.min(getMaxHealth(), oldHealth * getMaxHealth() / oldMax));
		updateShownRank();
	}

	private void updateShownRank() {
		int shown = isMortal() ? MORTAL : Math.max(0, sustainedRank() - cultivation.getPressureStages());
		if (entityData.get(DATA_SHOWN_RANK) != shown) entityData.set(DATA_SHOWN_RANK, shown);
	}

	/** Dresses it in its path's colours, in the grade its station (or, for a rogue, its realm) affords, and arms it. */
	public void equip(RandomSource random) {
		ClothingStyle style = ClothingStyle.of(getFaction());
		ClothingTier tier = clothingTier(getTitle(), getRank(), random);
		boolean sect = getTitle().isSectTitle();
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			if (!sect && random.nextFloat() < 0.25f) continue; // rogues are seldom fully dressed
			setItemSlot(type.getSlot(), ClothingItem.create(ModItems.clothing(tier, type), style));
			setDropChance(type.getSlot(), 0.12f);
		}
		Item weapon = weaponFor(getRank(), random);
		if (weapon != null) {
			setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
			setDropChance(EquipmentSlot.MAINHAND, 0.06f);
		}
	}

	private void upgradeClothing(ClothingStyle style, ClothingTier tier) {
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			ItemStack worn = getItemBySlot(type.getSlot());
			if (worn.getItem() instanceof ClothingItem clothing && clothing.getTier().ordinal() >= tier.ordinal()) continue;
			setItemSlot(type.getSlot(), ClothingItem.create(ModItems.clothing(tier, type), style));
		}
	}

	/** Sect members dress by title, rogues by realm; the Upper Realm's are a grade finer. */
	public ClothingTier clothingTier(NpcTitle title, int rank, RandomSource random) {
		int grade = switch (title) {
			case SECT_MASTER -> 3;
			case GRAND_ELDER -> 2 + random.nextInt(2);
			case ELDER -> 1 + random.nextInt(2);
			case INNER_DISCIPLE -> random.nextInt(2);
			case OUTER_DISCIPLE -> 0;
			case ROGUE, WANDERER -> rank < 0 ? 0 : Math.min(4, rank / Stage.values().length - 1 + random.nextInt(2));
		};
		if (ModDimensions.isUpperRealm(level().dimension())) grade++;
		return ClothingTier.byIndex(Math.max(0, grade));
	}

	private static Item weaponFor(int rank, RandomSource random) {
		if (rank < 0) return random.nextFloat() < 0.5f ? Items.WOODEN_SWORD : null;
		Realm realm = realmOf(rank);
		return switch (realm) {
			case QI_REFINING -> random.nextBoolean() ? Items.STONE_SWORD : Items.IRON_SWORD;
			case FOUNDATION_BUILDING, CORE_FORMATION -> Items.IRON_SWORD;
			case NASCENT_SOUL, HEAVENLY_BEING -> Items.DIAMOND_SWORD;
			case FOUR_AXIS -> Items.NETHERITE_SWORD;
		};
	}

	/** Spawned by an egg, a command or a spawner without anything set: a rogue of the land's usual strength. */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data, CompoundTag tag) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data, tag);
		if (sectId == null && (tag == null || !tag.contains("Npc"))) {
			RandomSource random = level.getRandom();
			int rank = NpcSpawner.rollRank(random, ModDimensions.isUpperRealm(level.getLevel().dimension()), true);
			becomeRogue(rank, Alignment.randomRogue(random), random);
		}
		return result;
	}

	/** A cultivator of no sect: rank, path, look, clothing, and where its life goes from here. */
	public void becomeRogue(int rank, int alignment, RandomSource random) {
		setRank(rank);
		setAlignment(alignment);
		entityData.set(DATA_SKIN, (byte) random.nextInt(SKINS));
		setTitle(rank < 0 ? NpcTitle.WANDERER : NpcTitle.ROGUE);
		equip(random);
		lifecycle = Lifecycle.ROGUE;
		decideRogueCourse();
	}

	public void randomizeSkin(RandomSource random) {
		entityData.set(DATA_SKIN, (byte) random.nextInt(SKINS));
	}

	// --- Ticking ---

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		ServerLevel level = (ServerLevel) level();
		boolean lower = ModDimensions.isLowerRealm(level.dimension());
		cultivation.setLowerRealmBound(lower);
		cultivation.setInUpperRealm(ModDimensions.isUpperRealm(level.dimension()));
		if (!caughtUp) {
			caughtUp = true;
			catchUp(level);
			refreshStats();
		}
		if (tickCount % 20 == 0) {
			lastSeen = level.getGameTime();
			secondTick(level);
		}
		if (getTarget() != null || getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 100) combatTicks = 200;
		else if (combatTicks > 0) combatTicks--;
		tickQi();
		if (fleeTicks > 0 && --fleeTicks == 0) reachRefuge(); // given up on getting there: it stops running all the same
		if (lifecycle == Lifecycle.SECLUDED && isConcealed()) repulse(level);
		tickFlight(level);
		NpcTravel.maybeDispatch(this, level);
	}

	/** Whether it is fighting, or was a moment ago. */
	public boolean isInCombat() { return combatTicks > 0; }

	private void secondTick(ServerLevel level) {
		if (isMeditating() && !isMortal()) cultivate(1.0, meditationMultiplier(level));
		// Natural recovery out of combat: quicker once qi sustains the body.
		if (!isInCombat() && getHealth() < getMaxHealth()) heal(isMortal() || getRank() < 4 ? 0.5f : 2.0f);
		setFlag(FLAG_DOMAIN, !isMortal() && (!isSectMember() || isInCombat()) && !isConcealed());
		if (isDomainOpen()) senseDomains(level);
		if (sectId != null) {
			Sect sect = SectManager.get(level.getServer(), sectId);
			if (sect == null || sect.extinct) {
				leaveSect(level, false);
			} else {
				Sect.Member member = sect.members.get(getUUID());
				if (member == null) {
					leaveSect(level, false); // the sect no longer counts it (e.g. a copy of a member that departed)
				} else if (member.title != getTitle()) {
					setTitle(member.title);
				}
			}
		}
		// A lower-realm sect member who becomes a Heavenly Being has outgrown the sect.
		if (lifecycle == Lifecycle.SECT && ModDimensions.isLowerRealm(level.dimension()) && getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY)) {
			beginDeparture(level);
		}
		if (lifecycle == Lifecycle.ROGUE) decideRogueCourse();
		if (lifecycle == Lifecycle.SECLUDED && getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION)
				&& ModDimensions.isLowerRealm(level.dimension())) {
			endSeclusion(level);
			beginAscension();
		}
		cultivation.setGearQiBonus(ClothingItem.qiGatherBonus(this));
		updateShownRank();
	}

	/** Cultivation boost while meditating: the mat it sits on and the clothing it wears, as for a player. */
	private double meditationMultiplier(ServerLevel level) {
		double boost = ClothingItem.meditationBonus(this);
		Block at = level.getBlockState(blockPosition()).getBlock();
		if (at == ModBlocks.RED_MEDITATION_MAT) boost += CultivationBoost.RED_MAT;
		else if (at == ModBlocks.MEDITATION_MAT) boost += CultivationBoost.MAT;
		return 1 + boost;
	}

	/** {@code seconds} of meditation at a tenth of a player's rate (times {@code multiplier}). */
	private void cultivate(double seconds, double multiplier) {
		if (isMortal() || cultivation.isMaxed()) return;
		double perSecond = cultivation.cultivationPerSecond() * NPC_RATE * multiplier
				* (cultivation.isInUpperRealm() ? PlayerCultivation.UPPER_REALM_QI_MULTIPLIER : 1.0);
		advanceUnchecked(perSecond * seconds);
	}

	/** Cultivation a second this NPC gathers meditating with nothing to help it: a tenth of a player's (used by the tests). */
	double meditationRate() {
		return cultivation.cultivationPerSecond() * NPC_RATE;
	}

	/**
	 * Adds cultivation, passing every bottleneck (no tribulation for NPCs) but never past what the realm it stands in can
	 * sustain. Package-private for the game tests.
	 */
	void advance(double amount) {
		boolean lower = ModDimensions.isLowerRealm(level().dimension());
		cultivation.setLowerRealmBound(lower);
		cultivation.setInUpperRealm(ModDimensions.isUpperRealm(level().dimension()));
		advanceUnchecked(amount);
	}

	private void advanceUnchecked(double amount) {
		int before = getRank();
		Realm realm = cultivation.getRealm();
		Stage stage = cultivation.getStage();
		double value = cultivation.getCultivation() + amount;
		int cap = cultivation.isLowerRealmBound()
				? PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)
				: PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		for (int guard = 0; guard < 32; guard++) {
			cultivation.setState(realm, stage, 0);
			double required = cultivation.cultivationRequired();
			if (value < required) break;
			int rank = PlayerCultivation.rank(realm, stage);
			if (rank >= cap) {
				value = required;
				break;
			}
			value -= required;
			stage = stage.isLast() ? Stage.EARLY : stage.next();
			if (stage == Stage.EARLY) realm = realm.next();
		}
		cultivation.setState(realm, stage, value);
		int after = PlayerCultivation.rank(realm, stage);
		if (after != before) {
			entityData.set(DATA_RANK, after);
			refreshStats();
			setHealth(getMaxHealth());
			level().playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.2f);
			if (sectId != null && level() instanceof ServerLevel server) SectManager.onMemberRankChanged(server, this);
		}
	}

	/** The time its chunk spent unloaded, part of it spent meditating. */
	private void catchUp(ServerLevel level) {
		if (lastSeen < 0 || isMortal()) return;
		long elapsed = Math.min(MAX_CATCH_UP, level.getGameTime() - lastSeen);
		if (elapsed < 1200) return;
		cultivate(elapsed / 20.0 * CATCH_UP_SHARE, 1.0);
	}

	// --- Qi, flight, suppression ---

	private void tickQi() {
		if (isMortal()) return;
		double spend = 0;
		if (isQiFlying()) {
			double cost = cultivation.qiFlightCostPerSecond();
			if (cultivation.getQi() + (cultivation.qiGatherPerSecond() - cost) / 20.0 < 0) {
				setQiFlying(false); // out of qi in the air: it falls, as a player would
			} else {
				spend += cost;
			}
		}
		if (isSuppressing()) {
			suppressUpkeep = RealmSuppressSystem.upkeepOf(getUUID());
			if (cultivation.getQi() < cultivation.maxQi() * 0.15 || cultivation.getQi() + (cultivation.qiGatherPerSecond() - spend - suppressUpkeep) / 20.0 < 0) {
				setSuppressing(false);
			} else {
				spend += suppressUpkeep;
			}
		}
		cultivation.gatherQi(1 / 20.0, spend);
	}

	/** Flight it can keep up: endless where gathering covers the cost, otherwise only with ten seconds' reserve in hand. */
	public boolean canFlySustainably() {
		if (isMortal() || !cultivation.getEffectiveRealm().canFlyOnQi()) return false;
		if (ModDimensions.isSpatialGap(level().dimension())) return false;
		double cost = cultivation.qiFlightCostPerSecond();
		if (cultivation.qiGatherPerSecond() >= cost) return true;
		return cultivation.getQi() >= cost * 10 && cultivation.getQi() >= cultivation.maxQi() * 0.4;
	}

	public void setQiFlying(boolean flying) {
		if (flying == isQiFlying()) return;
		if (flying && !canFlySustainably()) return;
		setFlag(FLAG_FLYING, flying);
		setNoGravity(flying);
		getNavigation().stop();
		if (flying) {
			lastSteered = tickCount;
			landing = false;
			setDeltaMovement(getDeltaMovement().add(0, 0.25, 0));
		}
	}

	/**
	 * Steers itself through the air toward {@code target} at about {@code speed} blocks a tick, easing off as it arrives
	 * (direct flight; no pathfinding needed above the terrain).
	 */
	public void flyToward(Vec3 target, double speed) {
		lastSteered = tickCount;
		landing = false;
		steer(target, speed);
	}

	private void steer(Vec3 target, double speed) {
		Vec3 to = target.subtract(position());
		double distance = to.length();
		if (distance < 0.5) {
			setDeltaMovement(getDeltaMovement().scale(0.6));
			return;
		}
		// With the air's drag this settles at about the wanted speed.
		double wanted = Math.min(speed, distance * 0.2);
		setDeltaMovement(getDeltaMovement().scale(0.8).add(to.scale(wanted * 0.3 / distance)));
		getLookControl().setLookAt(target.x, target.y, target.z);
		setYRot((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0f);
		yBodyRot = getYRot();
	}

	/** While flying on purpose (a journey, a chase), it doesn't drop out of the air on touching ground. */
	private boolean shouldStayAloft() {
		return lifecycle == Lifecycle.DEPARTING || lifecycle == Lifecycle.ASCENDING || getTarget() != null && isInCombat();
	}

	@Override
	protected float getFlyingSpeed() {
		return isQiFlying() ? getSpeed() * 0.1f : super.getFlyingSpeed();
	}

	public void setSuppressing(boolean suppressing) {
		if (suppressing == isSuppressing()) return;
		setFlag(FLAG_SUPPRESSING, suppressing);
		RealmSuppressSystem.refreshNow();
	}

	/** Holding down a weaker foe is worth it only while the qi lasts: a third of the pool in hand to start. */
	public boolean canSuppressSustainably() {
		return !isMortal() && cultivation.getQi() >= cultivation.maxQi() * 0.3;
	}

	@Override
	public void onRealmPressure(int stages, double penalty) {
		cultivation.setPressure(stages, penalty);
		updateShownRank();
	}

	/**
	 * Its open domain feels every other within reach: a hostile one a realm above its own sends it running, unless it
	 * {@link #holdsGround holds its ground}.
	 */
	private void senseDomains(ServerLevel level) {
		if (holdsGround()) return;
		int mine = sustainedRank();
		double radius = domainRadius();
		for (LivingEntity other : ConsciousnessDomainHandler.entitiesIn(this, Math.min(radius, 96), LivingEntity.class,
				e -> e.isAlive() && CultivatorEntity.isCultivator(e))) {
			int theirs = rankOf(other);
			if (theirs >= mine + SUPERIOR_DOMAIN_GAP && isThreat(other)) {
				startFleeing(other);
				return;
			}
		}
	}

	/**
	 * Runs from {@code other}: to a refuge {@link #FLEE_DISTANCE} blocks or so the far side of it, staying until it gets there
	 * (or {@link #FLEE_TIMEOUT} runs out). Sensed again on the way, it keeps going; sensed again after, it runs again.
	 */
	void startFleeing(Entity other) {
		if (fleeFrom != other || refuge == null) {
			fleeFrom = other;
			refuge = level() instanceof ServerLevel server ? pickRefuge(server, other) : null;
			fleeTicks = FLEE_TIMEOUT;
		}
		fleeTicks = Math.max(fleeTicks, 600);
		if (getTarget() == other) setTarget(null);
	}

	/**
	 * A spot about {@link #FLEE_DISTANCE} (give or take {@link #FLEE_SPREAD}) blocks away, roughly straight away from
	 * {@code threat}. Kept on ground the world still simulates, drawing it in as far as needed: a fugitive that walked into
	 * chunks nobody is near would stand frozen there, and never come home while its pursuer stays.
	 */
	private BlockPos pickRefuge(ServerLevel level, Entity threat) {
		Vec3 away = position().subtract(threat.position()).multiply(1, 0, 1);
		double angle = (away.lengthSqr() < 1.0e-4 ? random.nextDouble() * Math.PI * 2 : Math.atan2(away.z, away.x)) + (random.nextDouble() - 0.5);
		double distance = FLEE_DISTANCE - FLEE_SPREAD + random.nextInt(2 * FLEE_SPREAD + 1);
		for (; distance >= 24; distance -= 16) {
			BlockPos at = BlockPos.containing(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance);
			if (level.isPositionEntityTicking(at)) return at;
		}
		return null; // nowhere far to go: it just backs away (FleeGoal)
	}

	/** Where it is running to, or null (no refuge, or not running from anyone). */
	public BlockPos getRefuge() { return refuge; }

	/** Safe at its refuge: it stops running, and heads home (ReturnToTerritoryGoal) or back to its own wandering. */
	public void reachRefuge() {
		refuge = null;
		fleeFrom = null;
		fleeTicks = 0;
	}

	/** The refuge it ran for has fallen out of the simulated world (its pursuer moved off): one nearer, the same way. */
	public void redrawRefuge(ServerLevel level) {
		if (refuge == null) return;
		Vec3 to = Vec3.atCenterOf(refuge).subtract(position()).multiply(1, 0, 1);
		double distance = to.length();
		for (distance -= 16; distance >= 8; distance -= 16) {
			BlockPos at = BlockPos.containing(position().add(to.normalize().scale(distance)));
			if (level.isPositionEntityTicking(at)) {
				refuge = at;
				return;
			}
		}
		reachRefuge();
	}

	/**
	 * The Sect Master and its elders defend their sect against anyone, however far above them: no stronger domain sends them
	 * running, only a beating (see {@link #shouldFlee}). Disciples and rogues have no such duty.
	 */
	public boolean holdsGround() {
		return lifecycle == Lifecycle.SECT && getTitle().isElderOrAbove();
	}

	/** A player's or NPC's rank as cultivators weigh each other ({@link #MORTAL} for mortals and creative players). */
	public static int rankOf(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			PlayerCultivation c = CultivationManager.get(player);
			return c.isMortal() ? MORTAL : c.sustainedRank();
		}
		if (entity instanceof CultivatorNpc npc) return npc.isConcealed() ? MORTAL : npc.sustainedRank();
		return MORTAL;
	}

	// --- Allies and enemies ---

	public boolean isAlly(Entity other) {
		return other instanceof CultivatorNpc npc && sectId != null && sectId.equals(npc.sectId);
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return isAlly(other) || super.isAlliedTo(other);
	}

	/** The righteous and the demonic are enemies wherever they meet; the neutral are nobody's. */
	public boolean isEnemy(LivingEntity other) {
		if (other == this || !other.isAlive() || isAlly(other)) return false;
		if (other instanceof Player player) {
			if (player.isCreative() || player.isSpectator()) return false;
			return getFaction().isEnemyOf(Alignment.factionOf(alignmentOf(player)));
		}
		if (other instanceof CultivatorNpc npc) return !npc.isConcealed() && getFaction().isEnemyOf(npc.getFaction());
		return false;
	}

	/** Someone it is (or should be) fighting, or who is fighting it. */
	public boolean isThreat(LivingEntity other) {
		if (isAlly(other)) return false;
		if (getTarget() == other || getLastHurtByMob() == other) return true;
		if (other instanceof Mob mob && mob.getTarget() == this) return true;
		return isEnemy(other);
	}

	/** What its Realm Suppress weighs on: foes in its domain, never its own sect or bystanders. */
	public boolean isHostileTo(LivingEntity other) {
		if (other == this || isAlly(other)) return false;
		if (getTarget() == other || getLastHurtByMob() == other) return true;
		return other instanceof Mob mob && mob.getTarget() == this;
	}

	public static int alignmentOf(Player player) {
		return CultivationManager.forPlayer(player).getAlignment();
	}

	/** It turned on {@code player} unprovoked (their path offends its own): killing it is then self-defence, not murder. */
	public void markAggressed(LivingEntity target) {
		if (target instanceof Player player) aggressed.add(player.getUUID());
	}

	/** Running: from a domain a realm above its own (unless it {@link #holdsGround}), or from a fight it is losing badly. */
	public boolean shouldFlee() {
		return fleeFrom != null && fleeFrom.isAlive() && !fleeFrom.isRemoved() && !holdsGround()
				|| getHealth() < getMaxHealth() * FLEE_HEALTH && isInCombat();
	}

	public Entity getFleeFrom() { return fleeFrom; }

	// --- Sect ---

	/** How far it may go from the sect's core (barrier + {@link #TERRITORY_MARGIN}); 0 if it has no sect. */
	public int territoryRadius(Sect sect) {
		return sect == null ? 0 : sect.barrierRadius + TERRITORY_MARGIN;
	}

	/** Only Heavenly Beings and above may leave the grounds outside a fight. */
	public boolean isLeashed() {
		return sectId != null && getRank() < PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY);
	}

	public Sect sect() {
		return sectId == null || !(level() instanceof ServerLevel server) ? null : SectManager.get(server.getServer(), sectId);
	}

	/** Leaves the sect: to wander ({@code departed}), or because the sect itself is gone. */
	public void leaveSect(ServerLevel level, boolean departed) {
		if (sectId == null) return;
		UUID old = sectId;
		sectId = null;
		home = null;
		homeIndex = -1;
		setTitle(isMortal() ? NpcTitle.WANDERER : NpcTitle.ROGUE);
		lifecycle = Lifecycle.ROGUE;
		if (departed) SectManager.onMemberDeparted(level, old, getUUID());
	}

	/** Answers a call from its sect (see SectManager#alert). */
	public void answerAlert(LivingEntity aggressor) {
		if (aggressor == null || !aggressor.isAlive() || isAlly(aggressor) || shouldFlee()) return;
		if (aggressor instanceof Player player && (player.isCreative() || player.isSpectator())) return;
		if (lifecycle != Lifecycle.SECT) return;
		setMeditating(false);
		setTarget(aggressor);
	}

	// --- Departures, seclusion and ascension ---

	/** Leaves its sect to make its own way: flies off 200-1000 blocks before settling into a rogue's life. */
	private void beginDeparture(ServerLevel level) {
		Sect sect = sect();
		BlockPos from = sect == null ? blockPosition() : sect.core;
		leaveSect(level, true);
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = 200 + random.nextDouble() * 800;
		journeyTarget = BlockPos.containing(from.getX() + Math.cos(angle) * distance, from.getY(), from.getZ() + Math.sin(angle) * distance);
		lifecycle = Lifecycle.DEPARTING;
		setMeditating(false);
		setTarget(null);
		setPersistenceRequired();
		level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 1.0f, 1.4f);
	}

	/** A rogue's next step: Heavenly Beings in the lower realms seek seclusion, and at Grand Perfection go to ascend. */
	public void decideRogueCourse() {
		if (lifecycle != Lifecycle.ROGUE || isMortal() || !ModDimensions.isLowerRealm(level().dimension())) return;
		if (getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION)) {
			beginAscension();
		} else if (getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY)) {
			lifecycle = Lifecycle.SEEKING_SECLUSION;
			journeyTarget = null;
			setPersistenceRequired();
		}
	}

	/** A journey (departure or ascension) has reached its end. */
	public void arrive(ServerLevel level) {
		if (lifecycle == Lifecycle.DEPARTING) {
			lifecycle = Lifecycle.ROGUE;
			journeyTarget = null;
			decideRogueCourse();
		} else if (lifecycle == Lifecycle.ASCENDING) {
			attemptAscension(level);
		}
	}

	private void beginAscension() {
		lifecycle = Lifecycle.ASCENDING;
		journeyTarget = new BlockPos(SpatialRiftBlock.RIFT_X, 0, SpatialRiftBlock.RIFT_Z); // y found on approach
		setPersistenceRequired();
	}

	/** At the rift: into the Spatial Gap, and out into the Upper Realm, or crushed on the way. */
	public void attemptAscension(ServerLevel level) {
		ServerLevel upper = level.getServer().getLevel(ModDimensions.UPPER_REALM);
		level.playSound(null, blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.NEUTRAL, 0.6f, 1.6f);
		if (upper == null || random.nextFloat() >= ASCENSION_SUCCESS) {
			hurt(ModDamageTypes.spatialPressure(level), Float.MAX_VALUE);
			return;
		}
		completeAscension();
		NpcTravel.sendToUpperRealm(this, level, upper);
	}

	/** Through the rift and out into the Upper Realm: a rogue again, no longer held by the lower realms' cap. */
	public void completeAscension() {
		lifecycle = Lifecycle.ROGUE;
		journeyTarget = null;
		landing = false;
		setQiFlying(false);
	}

	/** Comes down out of the air (gently, so it takes no fall). */
	public void land() {
		if (isQiFlying()) landing = true;
	}

	/** While flying: lands when asked to or when nothing is steering it any more; drops out of the sky with no qi left. */
	private void tickFlight(ServerLevel level) {
		if (!isQiFlying()) {
			landing = false;
			return;
		}
		// Nothing has steered it for half a second (the goal that took it up has ended): it comes down.
		if (!landing && tickCount - lastSteered > 10) landing = true;
		if (landing) {
			int ground = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
			steer(new Vec3(getX(), Math.min(getY(), ground), getZ()), 0.2);
			if (onGround() || getY() - ground < 0.2) {
				setQiFlying(false);
				landing = false;
			}
		} else if (onGround() && getDeltaMovement().y <= 0 && !shouldStayAloft()) {
			setQiFlying(false);
		}
	}

	/** Picks a sheltered spot nearby (a cave mouth, an overhang, or a mountainside high up) to seclude itself in. */
	public BlockPos findSeclusionSite(ServerLevel level) {
		BlockPos origin = blockPosition();
		BlockPos best = null;
		int bestScore = Integer.MIN_VALUE;
		for (int attempt = 0; attempt < 48; attempt++) {
			int x = origin.getX() + random.nextInt(65) - 32;
			int z = origin.getZ() + random.nextInt(65) - 32;
			if (!level.hasChunkAt(new BlockPos(x, origin.getY(), z))) continue;
			int surface = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			for (int y = Math.min(surface, origin.getY() + 24); y > Math.max(level.getMinBuildHeight() + 1, origin.getY() - 24); y--) {
				BlockPos feet = new BlockPos(x, y, z);
				if (!isSeatable(level, feet)) continue;
				int score = 0;
				boolean roofed = false;
				for (int up = 3; up <= 6; up++) {
					if (!level.getBlockState(feet.above(up)).isAir()) {
						roofed = true;
						break;
					}
				}
				if (roofed) score += 20; // a cave or an overhang
				if (feet.getY() > 110) score += 10 + (feet.getY() - 110) / 5; // high on a mountain
				if (level.getBrightness(net.minecraft.world.level.LightLayer.SKY, feet) < 4) score += 5;
				if (!roofed && feet.getY() <= 110) continue;
				if (score > bestScore) {
					bestScore = score;
					best = feet;
				}
				break;
			}
		}
		return best;
	}

	private static boolean isSeatable(ServerLevel level, BlockPos feet) {
		return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir() && level.getBlockState(feet.above(2)).isAir()
				&& level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
				&& level.getFluidState(feet).isEmpty();
	}

	public int nextSeclusionAttempt() { return ++seclusionAttempts; }

	/** Sits down at {@code feet}: lays a meditation mat, raises its Concealment Barrier, and begins its seclusion. */
	public void enterSeclusion(ServerLevel level, BlockPos feet) {
		if (level.getBlockState(feet).isAir() && ModBlocks.MEDITATION_MAT.defaultBlockState().canSurvive(level, feet)) {
			level.setBlock(feet, ModBlocks.MEDITATION_MAT.defaultBlockState(), Block.UPDATE_ALL);
		}
		seclusionMat = feet.immutable();
		lifecycle = Lifecycle.SECLUDED;
		setPersistenceRequired();
		raiseConcealment(level);
	}

	public void raiseConcealment(ServerLevel level) {
		if (seclusionMat == null) return;
		if (concealment == null) concealment = UUID.randomUUID();
		Formation formation = Formations.create(level, concealment, Formation.Kind.CONCEALMENT, seclusionMat.above(), CONCEALMENT_RADIUS);
		Formations.update(level, formation, sustainedRank(), getUUID(), null);
		Formations.raise(level, formation);
		setFlag(FLAG_CONCEALED, true);
		level.playSound(null, seclusionMat, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.0f, 0.7f);
	}

	/** The Concealment Barrier comes down (to fight, to ascend, or because it is gone); the mat stays. */
	public void lowerConcealment(ServerLevel level) {
		setFlag(FLAG_CONCEALED, false);
		if (concealment != null) {
			Formations.dissolve(level, concealment);
			concealment = null;
		}
	}

	private void endSeclusion(ServerLevel level) {
		lowerConcealment(level);
		setMeditating(false);
		seclusionMat = null;
	}

	/** Struck or broken barrier: seclusion is broken, and the one who broke it answers for it. */
	public void onConcealmentAssailed(ServerPlayer player) {
		if (lifecycle != Lifecycle.SECLUDED || player.isCreative() || player.isSpectator()) return;
		lowerConcealment((ServerLevel) level());
		setMeditating(false);
		setTarget(player);
	}

	/** Anyone who wanders too close to a secluded cultivator's barrier is pushed back, slowed and warned, never harmed. */
	private void repulse(ServerLevel level) {
		if (repulseCooldown > 0) {
			repulseCooldown--;
			return;
		}
		if (seclusionMat == null) return;
		Vec3 center = Vec3.atBottomCenterOf(seclusionMat).add(0, 1, 0);
		boolean pulsed = false;
		for (ServerPlayer player : level.players()) {
			if (player.isCreative() || player.isSpectator() || player.position().distanceToSqr(center) > REPULSE_REACH * REPULSE_REACH) continue;
			Vec3 away = player.position().subtract(center);
			away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
			player.push(away.x * 1.4, 0.45, away.z * 1.4);
			player.hurtMarked = true;
			player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
			player.displayClientMessage(Component.translatable(ModLang.MSG_SECLUSION_REPULSE), true);
			pulsed = true;
		}
		if (pulsed) {
			level.playSound(null, seclusionMat, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.2f, 0.5f);
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, center.x, center.y, center.z, 24, 2.5, 1.5, 2.5, 0.05);
			repulseCooldown = 30;
		} else {
			repulseCooldown = 5;
		}
	}

	// --- Damage, death and removal ---

	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		if (hurt && !level().isClientSide && level() instanceof ServerLevel server) {
			setMeditating(false);
			LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
			if (attacker != null && lifecycle == Lifecycle.SECLUDED && isConcealed() && attacker instanceof ServerPlayer player) {
				onConcealmentAssailed(player);
			}
			if (attacker != null && sectId != null && !isAlly(attacker)) SectManager.onMemberAttacked(server, this, attacker);
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(level() instanceof ServerLevel server)) return;
		ServerPlayer killer = killerOf(source);
		if (sectId != null) SectManager.onMemberDeath(server, this, killer != null);
		if (killer != null) {
			int shift = Alignment.killShift(getAlignment(), aggressed.contains(killer.getUUID()), realmOf(getRank()), isMortal());
			if (shift != 0) {
				PlayerCultivation c = CultivationManager.get(killer);
				c.setAlignment(c.getAlignment() + shift);
				CultivationManager.markDirty(killer.server);
				CultivationManager.sync(killer);
				killer.displayClientMessage(Component.translatable(shift > 0 ? ModLang.MSG_ALIGNMENT_UP : ModLang.MSG_ALIGNMENT_DOWN,
						Math.abs(shift), Alignment.describe(c.getAlignment())), true);
			}
		}
		lowerConcealment(server);
		setSuppressing(false);
		RealmSuppressSystem.refreshNow();
	}

	/** Who answers for its death: the player who struck it, whose arrow or pet did, or who last hurt it before it fell. */
	private ServerPlayer killerOf(DamageSource source) {
		Entity entity = source.getEntity();
		if (entity instanceof ServerPlayer player) return player;
		if (entity instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner) return owner;
		if (lastHurtByPlayer instanceof ServerPlayer player && lastHurtByPlayerTime > 0) return player;
		return getKillCredit() instanceof ServerPlayer player ? player : null;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!level().isClientSide && level() instanceof ServerLevel server && reason == RemovalReason.DISCARDED && !travelling && isAlive()) {
			// Taken out of the world without dying (despawned, discarded by a command): for its sect, it simply left.
			if (sectId != null) leaveSect(server, true);
			lowerConcealment(server);
			setSuppressing(false);
		}
		super.remove(reason);
	}

	@Override
	public boolean removeWhenFarAway(double distanceSqr) {
		// Only the weaker rogues come and go; sect members, Heavenly Beings and those on a journey stay.
		return lifecycle == Lifecycle.ROGUE && sectId == null && getRank() < PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY)
				&& !hasCustomName();
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		if (isQiFlying()) return false;
		return super.causeFallDamage(distance, multiplier, source);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
			Sect sect = sect();
			String key;
			if (isConcealed()) key = ModLang.NPC_GREET_SECLUDED;
			else if (isEnemy(player)) key = ModLang.NPC_GREET_HOSTILE;
			else if (getFaction() == Alignment.Faction.RIGHTEOUS) key = ModLang.NPC_GREET_RIGHTEOUS;
			else if (getFaction() == Alignment.Faction.DEMONIC) key = ModLang.NPC_GREET_DEMONIC;
			else key = ModLang.NPC_GREET_NEUTRAL;
			Component who = sect == null ? describe() : Component.translatable(ModLang.NPC_OF_SECT, describe(), sect.name);
			player.displayClientMessage(Component.translatable(key, who), true);
		}
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	// --- Saving ---

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		CompoundTag npc = new CompoundTag();
		npc.putInt("Rank", getRank());
		npc.put("Cultivation", cultivation.save());
		npc.putInt("Alignment", getAlignment());
		npc.putInt("Title", getTitle().ordinal());
		npc.putInt("Skin", getSkin());
		npc.putString("Lifecycle", lifecycle.name());
		if (sectId != null) npc.putUUID("Sect", sectId);
		npc.putInt("Home", homeIndex);
		if (home != null) npc.put("HomePos", NbtUtils.writeBlockPos(home));
		if (journeyTarget != null) npc.put("Journey", NbtUtils.writeBlockPos(journeyTarget));
		if (seclusionMat != null) npc.put("SeclusionMat", NbtUtils.writeBlockPos(seclusionMat));
		if (concealment != null) npc.putUUID("Concealment", concealment);
		npc.putBoolean("Concealed", isConcealed());
		npc.putLong("LastSeen", lastSeen);
		npc.putInt("SeclusionAttempts", seclusionAttempts);
		ListTag list = new ListTag();
		for (UUID id : aggressed) list.add(NbtUtils.createUUID(id));
		npc.put("Aggressed", list);
		tag.put("Npc", npc);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (!tag.contains("Npc")) return;
		CompoundTag npc = tag.getCompound("Npc");
		int rank = npc.getInt("Rank");
		PlayerCultivation loaded = PlayerCultivation.load(npc.getCompound("Cultivation"));
		entityData.set(DATA_RANK, rank);
		if (rank < 0) {
			cultivation.setMortal(true);
		} else {
			cultivation.setState(realmOf(rank), stageOf(rank), loaded.getCultivation());
			cultivation.setQi(loaded.getQi());
		}
		setAlignment(npc.getInt("Alignment"));
		entityData.set(DATA_TITLE, (byte) NpcTitle.byIndex(npc.getInt("Title")).ordinal());
		entityData.set(DATA_SKIN, (byte) Mth.clamp(npc.getInt("Skin"), 0, SKINS - 1));
		try {
			lifecycle = Lifecycle.valueOf(npc.getString("Lifecycle"));
		} catch (IllegalArgumentException e) {
			lifecycle = Lifecycle.ROGUE;
		}
		sectId = npc.hasUUID("Sect") ? npc.getUUID("Sect") : null;
		homeIndex = npc.getInt("Home");
		home = npc.contains("HomePos") ? NbtUtils.readBlockPos(npc.getCompound("HomePos")) : null;
		journeyTarget = npc.contains("Journey") ? NbtUtils.readBlockPos(npc.getCompound("Journey")) : null;
		seclusionMat = npc.contains("SeclusionMat") ? NbtUtils.readBlockPos(npc.getCompound("SeclusionMat")) : null;
		concealment = npc.hasUUID("Concealment") ? npc.getUUID("Concealment") : null;
		setFlag(FLAG_CONCEALED, npc.getBoolean("Concealed"));
		lastSeen = npc.getLong("LastSeen");
		seclusionAttempts = npc.getInt("SeclusionAttempts");
		aggressed.clear();
		ListTag list = npc.getList("Aggressed", Tag.TAG_INT_ARRAY);
		for (Tag entry : list) aggressed.add(NbtUtils.loadUUID(entry));
		setNoGravity(false); // flight isn't saved: it comes back to earth
		refreshStats();
	}

	// --- Loaded NPCs, for the systems that scan them ---

	/** Every NPC cultivator loaded on the server (kept by ServerEntityEvents in CultivationEvents). */
	private static final Set<CultivatorNpc> LOADED = new HashSet<>();

	public static void onLoad(Entity entity) {
		if (entity instanceof CultivatorNpc npc) LOADED.add(npc);
	}

	public static void onUnload(Entity entity) {
		if (entity instanceof CultivatorNpc npc) LOADED.remove(npc);
	}

	public static void clearLoaded() {
		LOADED.clear();
	}

	/** Loaded NPCs in {@code level} with their domain open. */
	public static List<CultivatorNpc> openDomains(ServerLevel level) {
		List<CultivatorNpc> found = new ArrayList<>();
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() == level && npc.isDomainOpen()) found.add(npc);
		}
		return found;
	}

	/** Loaded NPCs in {@code level} pressing down on their foes with Realm Suppress. */
	public static List<CultivatorNpc> suppressors(ServerLevel level) {
		List<CultivatorNpc> found = new ArrayList<>();
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() == level && npc.isAlive() && !npc.isRemoved() && npc.isSuppressing()) found.add(npc);
		}
		return found;
	}

	/** Loaded NPCs of {@code level} whose sect is {@code sect}. */
	public static List<CultivatorNpc> loadedMembers(ServerLevel level, UUID sect) {
		List<CultivatorNpc> found = new ArrayList<>();
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() == level && npc.isAlive() && !npc.isRemoved() && sect.equals(npc.sectId)) found.add(npc);
		}
		return found;
	}

	/** Loaded NPCs in {@code level} within {@code radius} of {@code pos}. */
	public static int countNear(ServerLevel level, Vec3 pos, double radius) {
		int count = 0;
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() == level && npc.isAlive() && npc.position().distanceToSqr(pos) <= radius * radius) count++;
		}
		return count;
	}

	public static int countIn(ServerLevel level) {
		int count = 0;
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() == level && npc.isAlive()) count++;
		}
		return count;
	}

	/** True for monsters: what sect disciples clear from their grounds. */
	public static boolean isMonster(LivingEntity entity) {
		return entity instanceof Enemy && !(entity instanceof CultivatorNpc);
	}
}
