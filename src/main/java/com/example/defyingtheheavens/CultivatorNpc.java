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
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * flies to the rift at Overworld 0, 0 and attempts the ascension ({@link Lifecycle#ASCENDING}). Come up into the Upper Realm,
 * it joins the nearest sect of its path that has room (taking the slot a new disciple would have filled, and the Sect Master's
 * seat if it is the strongest), or else goes into seclusion there to cultivate toward Four Axis. Out of every player's sight,
 * a long journey goes on unseen along a real way, and anyone coming upon it on the way finds it there (see {@link NpcTravel}).
 * <p>
 * <b>Fighting.</b> Rogues always hold their consciousness domain open, sect members only in combat. In combat it presses down
 * on weaker foes with Realm Suppress and flies after flying ones, but only while its qi can carry the cost; it flees when badly
 * hurt or when it senses a hostile domain a realm or more above its own. The righteous and the devilish attack each other (and
 * players of the other path) on sight; see {@link CultivatorGoals} for who fights when.
 * <p>
 * <b>Everyday life.</b> Around its duties it lives a day (see {@link NpcLife}): sleeps at night, sits the morning lecture,
 * talks with its fellows, practises, bows to its seniors, mourns the fallen, and says things aloud of its own accord
 * ({@link NpcSpeech}).
 */
public class CultivatorNpc extends PathfinderMob implements CultivatorEntity, Merchant {
	/** NPCs cultivate at this share of a player's rate. */
	public static final double NPC_RATE = 0.1;
	/** Time spent unloaded counts as meditating for this share of it, however long it was (see NpcLedger for what it does). */
	public static final double CATCH_UP_SHARE = 0.5;
	/** At most this many tries at the Ritual to the Heavens in one catch-up: one for each day away. */
	private static final int MAX_CATCH_UP_RITUALS = 64;
	/** Away less than this (ticks, a minute), it hasn't missed enough to catch up on. */
	public static final long MIN_CATCH_UP = 1200;
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
	/** A pursuer this far off (blocks) has been left behind: a badly hurt cultivator stops running from it. */
	public static final double PURSUER_LOST = 32;

	public enum Lifecycle { SECT, ROGUE, DEPARTING, SEEKING_SECLUSION, SECLUDED, ASCENDING }

	private static final EntityDataAccessor<Integer> DATA_RANK = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_SHOWN_RANK = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_TITLE = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> DATA_ALIGNMENT = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> DATA_FLAGS = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> DATA_SKIN = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BYTE);
	/** Its race (Bloodline, by ordinal) and the race's colouring: ordinal times 4 plus colouring. 0: a man's. */
	private static final EntityDataAccessor<Byte> DATA_RACE = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BYTE);
	/** Set aside while a nascent soul uses its body ({@link #isVacant}). */
	private static final EntityDataAccessor<Boolean> DATA_VACANT = SynchedEntityData.defineId(CultivatorNpc.class, EntityDataSerializers.BOOLEAN);

	private static final int FLAG_MEDITATING = 1;
	private static final int FLAG_FLYING = 2;
	private static final int FLAG_CONCEALED = 4;
	private static final int FLAG_DOMAIN = 8;
	private static final int FLAG_SUPPRESSING = 16;
	/** Keeping one of its sect's gates (Sect#guardPost): its nameplate says so. */
	private static final int FLAG_GATE_GUARD = 32;
	/** While flying: riding its sword ({@link SwordFlight}) rather than flying on qi alone. */
	private static final int FLAG_SWORD = 64;
	/** Down on one knee: begging for its life, or under the pressure of someone two realms above it (synced for the pose). */
	private static final int FLAG_KNEELING = 128;
	public static final int SKINS = 6;

	/** Rank -1: a mortal who never opened their meridians. */
	public static final int MORTAL = -1;

	private final PlayerCultivation cultivation = new PlayerCultivation();
	private UUID sectId;
	private int homeIndex = -1;
	private BlockPos home;
	private Lifecycle lifecycle = Lifecycle.ROGUE;
	private BlockPos journeyTarget;
	/** Newly through the rift, with its course in the Upper Realm still to choose (see {@link #decideRogueCourse}). */
	private boolean ascended;
	/** The sect it is on its way to join, a slot held for it (SectManager#reserve); null if none. */
	private UUID joiningSect;
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
	/**
	 * Who last struck it (not an ally, not in a spar), kept until they are {@link #PURSUER_LOST} off, gone, or the quarrel is let go:
	 * a badly hurt cultivator runs from them while they are near. Vanilla forgets the last attacker five seconds after its last blow,
	 * so one that kept ahead of its pursuer had nothing left to run from and stopped, the pursuer right behind (the user's report,
	 * 2026-10-19: wandering ghosts and Hungry Ghosts).
	 */
	private LivingEntity pursuer;
	private int fleeTicks;
	private BlockPos refuge;
	private int repulseCooldown;
	private double suppressUpkeep;
	private boolean landing;
	private int lastSteered;
	/** Its own regard for each player it has dealt with (see {@link Regard}); its sect's standing is added on top. */
	private final Map<UUID, Regard.Entry> regard = new HashMap<>();
	/** When each player last struck it (not saved): a fight costs regard once, not per blow. */
	private final Map<UUID, Long> struckAt = new HashMap<>();
	/**
	 * The player it is talking with (kept after the talk lapses, see {@link #tickTalk}), and for how much longer it stands still
	 * for them (see {@link TalkSystem}).
	 */
	private UUID talkingTo;
	private int talkTicks;
	/** How long a talk holds it still without a word from the player. */
	public static final int TALK_TICKS = 20 * 20;
	/** Ticks a fight is remembered: a player who strikes again within this loses no more regard for it. */
	private static final long STRUCK_MEMORY = 30 * 20;
	/** In a spar (SparSystem), the rank whose strength it holds itself to, or {@link #NO_HOLD_BACK}. */
	private int sparHoldBack = NO_HOLD_BACK;
	public static final int NO_HOLD_BACK = Integer.MIN_VALUE;
	/** What it has won in spars: it carries it, and drops it when it dies. */
	private final List<ItemStack> winnings = new ArrayList<>();
	/** Players who spared its life, and until when (game time) it won't raise a hand against them. */
	private final Map<UUID, Long> sparedBy = new HashMap<>();
	/** The player it is begging for its life (see TalkSystem), and for how much longer it keeps it up. */
	private UUID beggingTo;
	private int begTicks;
	/** How long one who spared it is left alone (a day), and how long it begs before it gives up and runs again. */
	public static final long SPARED_TICKS = 24_000;
	public static final int BEG_TICKS = 15 * 20;
	/** Trading (CultivatorTrades, on the vanilla trading screen): who with, the offers, and how much of each sold today. */
	private Player tradingPlayer;
	private MerchantOffers offers;
	private final Map<MerchantOffer, String> offerKeys = new java.util.IdentityHashMap<>();
	private final Map<String, Integer> tradeUses = new HashMap<>();
	private long tradeDay = -1;
	/** What it is doing of its everyday life (NpcLife: sleep, the lecture, a talk, practice...), or null. Not saved. */
	private NpcLife.Activity activity;
	/** When it last said something aloud of its own accord, and until when that stays up (NpcSpeech). */
	private long lastSpoke = Long.MIN_VALUE / 2;
	private long speakingUntil;
	/** When it last remarked on each player passing, or bowed to each senior (NpcLife). Not saved. */
	private final Map<UUID, Long> remarked = new HashMap<>();
	/** Set while it makes up for time its chunk spent unloaded: breakthroughs then pass without a word. */
	private boolean catchingUp;

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

	/** Its way on foot, with a far search for walks out of a building and round it (CultivatorGoals#walkToward). */
	@Override
	protected PathNavigation createNavigation(Level level) {
		return new CultivatorNavigation(this, level);
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
		entityData.define(DATA_RACE, (byte) 0);
		entityData.define(DATA_VACANT, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(0, new CultivatorGoals.BegGoal(this));
		goalSelector.addGoal(1, new CultivatorGoals.FleeGoal(this));
		goalSelector.addGoal(2, new CultivatorGoals.CombatGoal(this));
		goalSelector.addGoal(2, new CultivatorSpells.SpellGoal(this)); // its spells, alongside the fight (the user's ask, 2026-10-24)
		goalSelector.addGoal(2, new CultivatorGoals.TalkGoal(this));
		goalSelector.addGoal(3, new CultivatorGoals.JourneyGoal(this));
		goalSelector.addGoal(3, new CultivatorGoals.SeclusionGoal(this));
		goalSelector.addGoal(3, new CultivatorGoals.ShelterGoal(this)); // a settler, home while a threat is out (Settlements)
		goalSelector.addGoal(4, new CultivatorGoals.ReturnToTerritoryGoal(this));
		goalSelector.addGoal(5, new CultivatorGoals.LifeGoal(this));
		goalSelector.addGoal(6, new CultivatorGoals.SectDutyGoal(this));
		goalSelector.addGoal(6, new CultivatorGoals.WanderGoal(this));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new CultivatorGoals.RetaliateGoal(this));
		targetSelector.addGoal(2, new CultivatorGoals.HostilityGoal(this));
		targetSelector.addGoal(3, new CultivatorGoals.GuardGoal(this));
		targetSelector.addGoal(3, new CultivatorGoals.SoulWardGoal(this));
	}

	// --- Identity ---

	public static Realm realmOf(int rank) { return Realm.byIndex(Math.max(0, rank) / Stage.values().length); }
	public static Stage stageOf(int rank) { return Stage.byIndex(Math.max(0, rank) % Stage.values().length); }

	/** True rank (realm and stage as one number), or {@link #MORTAL}. */
	public int getRank() { return entityData.get(DATA_RANK); }
	@Override public boolean isMortal() { return getRank() < 0; }
	/** One of the Netherworld's dead (GhostCultivatorNpc). */
	public boolean isGhost() { return false; }

	/** Keeps a post of its own (the Ghost Gate's keepers): it doesn't stroll off, nor fall into talk with passers-by. */
	public boolean keepsPost() { return false; }

	/**
	 * A new cultivator for {@code level}: in the Netherworld one of its dead (a ghost cultivator, GhostCultivatorNpc), anywhere
	 * else a living one. Everything that brings cultivators into the world (NpcSpawner, HermitSites, SectManager, SectCommand)
	 * makes them here; null if the entity can't be made.
	 */
	public static CultivatorNpc createFor(ServerLevel level) {
		return ModDimensions.isNetherworld(level.dimension()) ? ModEntities.GHOST_CULTIVATOR.create(level) : ModEntities.CULTIVATOR.create(level);
	}
	public NpcTitle getTitle() { return NpcTitle.byIndex(entityData.get(DATA_TITLE)); }
	public int getAlignment() { return entityData.get(DATA_ALIGNMENT); }
	public Alignment.Faction getFaction() { return Alignment.factionOf(getAlignment()); }
	public int getSkin() { return entityData.get(DATA_SKIN); }

	/** Its race: a man's (HUMAN), or a demon's (its race's whole form, as a demon player's: ClientBloodlines#look). */
	public Bloodline getBloodline() {
		Bloodline race = Bloodline.byIndex((entityData.get(DATA_RACE) & 0xFF) >> 2);
		return race == null ? Bloodline.HUMAN : race;
	}

	/** Its race's colouring (Bloodline#variantId), 0 to 2. */
	public int getRaceVariant() { return entityData.get(DATA_RACE) & 3; }

	/** One of the demon races (a clan's, or a demon rogue). */
	public boolean isDemon() { return getBloodline().isBeast(); }

	/**
	 * Makes it of {@code race} in {@code variant} (0 to 2). A demon cultivates the Demon Path (its rank is that path's; the same realms
	 * and stages): born with its demon core, so never a mortal, and with no Ritual to the Heavens on its way (that is the heaven path's
	 * gate); its record carries its blood (DemonPath#bloodline: the race's gifts in a body a player's soul possesses, its years).
	 */
	public void setRace(Bloodline race, int variant) {
		if (race == null) race = Bloodline.HUMAN;
		entityData.set(DATA_RACE, (byte) (race.ordinal() << 2 | Mth.clamp(variant, 0, 2)));
		cultivation.demon().setBloodline(race);
		if (!race.isBeast()) return;
		cultivation.setHeavensLeave(true);
		if (isMortal()) setRank(0);
	}

	public UUID getSectId() { return sectId; }
	public boolean isSectMember() { return sectId != null; }
	public Lifecycle getLifecycle() { return lifecycle; }
	public int getHomeIndex() { return homeIndex; }
	public BlockPos getHome() { return home; }
	public BlockPos getJourneyTarget() { return journeyTarget; }
	/** Bound somewhere else on the same journey (JourneyGoal: open sky where it meant to set down, so the nearest island). */
	void setJourneyTarget(BlockPos target) { journeyTarget = target; }
	public BlockPos getSeclusionMat() { return seclusionMat; }
	public PlayerCultivation cultivation() { return cultivation; }

	private boolean flag(int flag) { return (entityData.get(DATA_FLAGS) & flag) != 0; }

	private void setFlag(int flag, boolean on) {
		byte flags = entityData.get(DATA_FLAGS);
		byte next = (byte) (on ? flags | flag : flags & ~flag);
		if (next != flags) entityData.set(DATA_FLAGS, next);
	}

	/**
	 * Its body taken by a nascent soul (Possession): it is set aside, unseen, untouched, silent and still (no mind of its own, no
	 * weight, nothing to strike or bump), and goes where its body goes until it is given back. Synced (no client draws it), and saved
	 * so a crash with it set aside still ends with it back: with nothing holding it, it wakes ({@link #tick}).
	 */
	public boolean isVacant() { return entityData.get(DATA_VACANT); }

	/** What it was before it was set aside, to be again after: bits of {@link #VACANT_NO_AI} and the rest. */
	private byte vacantWas;
	private static final int VACANT_NO_AI = 1, VACANT_INVULNERABLE = 2, VACANT_SILENT = 4, VACANT_NO_GRAVITY = 8, VACANT_INVISIBLE = 16;

	public void setVacant(boolean vacant) {
		if (vacant == isVacant()) return;
		if (vacant && !level().isClientSide) {
			// Whatever it was about ends: a talk, a trade, a bout, a flight, a meditation, its pressure.
			getNavigation().stop();
			setTarget(null);
			setMeditating(false);
			if (isQiFlying()) land();
			setSuppressing(false);
			setKneeling(false);
			if (isBegging()) stopBegging();
			breakOffTalk(ModLang.TALK_POSSESSED);
			if (tradingPlayer != null) setTradingPlayer(null);
			NpcLife.end(this);
			setPose(Pose.STANDING);
			vacantWas = (byte) ((isNoAi() ? VACANT_NO_AI : 0) | (isInvulnerable() ? VACANT_INVULNERABLE : 0) | (isSilent() ? VACANT_SILENT : 0)
					| (isNoGravity() ? VACANT_NO_GRAVITY : 0) | (isInvisible() ? VACANT_INVISIBLE : 0));
		}
		entityData.set(DATA_VACANT, vacant);
		setNoAi(vacant || (vacantWas & VACANT_NO_AI) != 0);
		setInvulnerable(vacant || (vacantWas & VACANT_INVULNERABLE) != 0);
		setSilent(vacant || (vacantWas & VACANT_SILENT) != 0);
		setNoGravity(vacant || (vacantWas & VACANT_NO_GRAVITY) != 0);
		setInvisible(vacant || (vacantWas & VACANT_INVISIBLE) != 0);
		noPhysics = vacant;
		setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		if (!vacant) vacantWas = 0;
	}

	/** Takes what it carries of its winnings (Possession: its body's to carry now), leaving it none. */
	public List<ItemStack> takeWinnings() {
		List<ItemStack> taken = new ArrayList<>(winnings);
		winnings.clear();
		return taken;
	}

	/** Something more for it to carry with its winnings (dropped when it falls). */
	public void addWinnings(ItemStack stack) {
		if (!stack.isEmpty()) winnings.add(stack);
	}

	/** Set aside, it is nothing to pick out, push or bump into. */
	@Override
	public boolean isPickable() {
		return !isVacant() && super.isPickable();
	}

	@Override
	public boolean isPushable() {
		return !isVacant() && super.isPushable();
	}

	/**
	 * Set aside, it shoves nothing either: it is put where its body stands a tick behind it, and its shove (the client's on its own
	 * player too) pushed the soul in its body on, the way it last went, again and again: a steady drift.
	 */
	@Override
	protected void pushEntities() {
		if (!isVacant()) super.pushEntities();
	}

	@Override
	public void tick() {
		super.tick();
		// Set aside with no soul holding its body (a crash, a reload): it wakes where it is.
		if (!level().isClientSide && isVacant() && tickCount % 20 == 0 && !Possession.holds(getUUID())) setVacant(false);
	}

	public boolean isMeditating() { return flag(FLAG_MEDITATING); }
	public void setMeditating(boolean meditating) { setFlag(FLAG_MEDITATING, meditating); }
	/** In the air by its own power: on qi alone, or riding its sword ({@link #isSwordFlying}). */
	public boolean isQiFlying() { return flag(FLAG_FLYING); }
	/** In the air on its sword (synced: the client draws the sword under its feet and the trail). */
	public boolean isSwordFlying() { return flag(FLAG_FLYING) && flag(FLAG_SWORD); }
	@Override public boolean isConcealed() { return flag(FLAG_CONCEALED); }
	public boolean isDomainOpen() { return flag(FLAG_DOMAIN) && !isConcealed() && isAlive() && !isVacant(); } // set aside: the soul's own domain shows
	public boolean isSuppressing() { return flag(FLAG_SUPPRESSING); }
	/** One of its sect's gate guards (set by its duty, SectDutyGoal; for whoever deals with visitors at the gate). */
	public boolean isGateGuard() { return flag(FLAG_GATE_GUARD); }
	public void setGateGuard(boolean guard) { setFlag(FLAG_GATE_GUARD, guard); }
	public boolean isKneeling() { return flag(FLAG_KNEELING); }
	public void setKneeling(boolean kneeling) { setFlag(FLAG_KNEELING, kneeling); }

	/** The realm it can wield where it stands (the lower realms cap it); what suppression and domains weigh. */
	@Override public Realm getCultivationRealm() { return realmOf(sustainedRank()); }
	@Override public Stage getCultivationStage() { return stageOf(sustainedRank()); }
	/** What others sense: lowered further by any pressure on it. */
	@Override public Realm getDisplayedRealm() { return realmOf(entityData.get(DATA_SHOWN_RANK)); }
	@Override public Stage getDisplayedStage() { return stageOf(entityData.get(DATA_SHOWN_RANK)); }

	public int sustainedRank() {
		if (isMortal()) return MORTAL;
		// Held by a lower realm as a player is, but never from Void Shattering on (PlayerCultivation#FREE_OF_LOWER_REALMS).
		boolean held = ModDimensions.isLowerRealm(level().dimension()) && !PlayerCultivation.beyondLowerRealms(getRank());
		int cap = PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE);
		return held ? Math.min(getRank(), cap) : getRank();
	}

	/** Consciousness domain radius, from what it shows. */
	public double domainRadius() {
		return isMortal() ? 0 : ConsciousnessDomainHandler.radius(getDisplayedRealm(), getDisplayedStage());
	}

	public Component describe() {
		return Component.translatable(ModLang.NPC_TITLE_BRACKETS, getTitle().getDisplayName(isDemon()));
	}

	/** Its personal name ("Mo Lian"), the same everywhere it is named (see {@link NpcNames}); a name tag's name if it has one. */
	public String getPersonalName() {
		return hasCustomName() ? getCustomName().getString() : NpcNames.of(getUUID());
	}

	/**
	 * Years it has cultivated (what it says when asked): from its realm and stage, the same every time, and 0 for a mortal. The realms
	 * past Four Axis by their spans (Lifespan#realmYears; before the sweep of 2026-10-24 they all said Four Axis's 1,000 to 3,000).
	 */
	public int yearsCultivated() {
		if (isMortal()) return 0;
		int[][] spans = {{3, 20}, {20, 60}, {60, 150}, {150, 400}, {400, 1000}, {1000, 3000}, {3000, 30_000}, {30_000, 500_000},
				{500_000, 5_000_000}, {5_000_000, 50_000_000}, {50_000_000, 500_000_000}};
		int[] span = spans[Math.min(spans.length - 1, realmOf(getRank()).ordinal())];
		double within = (stageOf(getRank()).ordinal() + new java.util.Random(getUUID().getLeastSignificantBits()).nextDouble()) / Stage.values().length;
		return (int) Math.round(span[0] + (span[1] - span[0]) * within);
	}

	/** How far through its stage it is, in percent. */
	public int stageProgressPercent() {
		double required = cultivation.cultivationRequired();
		return required <= 0 ? 100 : (int) Math.min(100, Math.round(100 * cultivation.getCultivation() / required));
	}

	// --- Regard and talk ---

	/**
	 * What it thinks of {@code player} now: their paths' baseline, its own regard and its sect's standing (see {@link Regard}). One in
	 * another's body is who that body is to it (Possession#identity).
	 */
	public int regard(Player player) {
		int value = Regard.baseline(getFaction(), alignmentOf(player));
		// One obsessed with Merit (Obsession) is thought the better of by everyone.
		if (player instanceof ServerPlayer sp) value += Obsession.regardBonus(CultivationManager.get(sp));
		// The righteous sense something wrong in a Heaven-Defying or a Soul-Devouring Body (Physique).
		if (player instanceof ServerPlayer sp) value += Physiques.regardShift(CultivationManager.get(sp), getFaction()) + RaceGifts.regardShift(CultivationManager.get(sp));
		// A demon minds one's blood: wary of men, warm to its own (Regard#bloodShift).
		if (player instanceof ServerPlayer sp) value += Regard.bloodShift(getBloodline(), DemonPath.bloodline(CultivationManager.get(sp)));
		UUID who = Possession.identity(player);
		Regard.Entry entry = regard.get(who);
		if (entry != null) value += entry.current(Regard.day(level().getGameTime()));
		Sect sect = sect();
		if (sect != null) value += sect.standing(who);
		return Regard.clamp(value);
	}

	/** Its own record of {@code player} (of the body they are in), made if there is none yet. */
	public Regard.Entry regardEntry(Player player) {
		long today = Regard.day(level().getGameTime());
		Regard.Entry entry = regard.computeIfAbsent(Possession.identity(player), id -> {
			Regard.Entry fresh = new Regard.Entry();
			fresh.day = today;
			return fresh;
		});
		entry.current(today);
		return entry;
	}

	/** Changes its own regard for {@code player}. */
	public void changeRegard(Player player, int delta) {
		if (delta == 0) return;
		Regard.Entry entry = regardEntry(player);
		entry.value = Regard.clamp(entry.value + delta);
	}

	/** Starts (or keeps up) a talk with {@code player}: it stops where it is and turns to them. */
	public void beginTalk(Player player) {
		// Someone else whose talk had lapsed (still reading, perhaps) is told it has turned to another.
		if (talkingTo != null && !talkingTo.equals(player.getUUID())) breakOffTalk(ModLang.TALK_OCCUPIED);
		talkingTo = player.getUUID();
		talkTicks = TALK_TICKS;
		getNavigation().stop();
	}

	/** Breaks off its talk, if it has one: the player's talk window closes, with the line of {@code key} saying why. */
	public void breakOffTalk(String key) {
		UUID partner = talkingTo;
		talkingTo = null;
		talkTicks = 0;
		if (partner != null && level().getPlayerByUUID(partner) instanceof ServerPlayer player) TalkSystem.breakOff(player, this, key);
	}

	/**
	 * A talk going on: it breaks off as soon as it won't talk with them any longer (a fight, a reason to run, turning on them;
	 * see TalkSystem#refusal). Left without a word, it lapses quietly, keeping who it was with: a player still reading picks it
	 * up again, and is told if someone else takes it up first.
	 */
	private void tickTalk() {
		if (level().getPlayerByUUID(talkingTo) instanceof ServerPlayer player) {
			String refusal = TalkSystem.refusal(player, this);
			if (refusal != null) {
				breakOffTalk(refusal);
				return;
			}
			// Two realms beneath them, with their realm bearing down on it: it talks on one knee.
			if (tickCount % 10 == 0 && TalkSystem.kneelsTo(player, this)) setKneeling(true);
		}
		talkTicks--;
	}

	public void endTalk(Player player) {
		if (player.getUUID().equals(talkingTo)) {
			talkingTo = null;
			talkTicks = 0;
		}
	}

	public boolean isTalkingTo(Player player) {
		return talkTicks > 0 && player.getUUID().equals(talkingTo);
	}

	/** Who it is talking with, if they are still here; null when not talking. */
	public Player talkPartner() {
		if (talkTicks <= 0 || talkingTo == null) return null;
		return level().getPlayerByUUID(talkingTo);
	}

	/** Keeps a talk from lapsing while the player is busy with it elsewhere (its trading screen is open). */
	public void holdTalk(Player player) {
		if (player.getUUID().equals(talkingTo)) talkTicks = Math.max(talkTicks, 40);
	}

	/** Every loaded cultivator in a talk with {@code player} now. */
	public static List<CultivatorNpc> talkingWith(Player player) {
		List<CultivatorNpc> found = new ArrayList<>();
		for (CultivatorNpc npc : LOADED) {
			if (npc.isTalkingTo(player)) found.add(npc);
		}
		return found;
	}

	// --- Trading (Merchant) ---

	/** Opens its trading screen for {@code player} with today's offers (CultivatorTrades); the talk holds while it is open. */
	public void openTrade(ServerPlayer player) {
		long today = Regard.day(level().getGameTime());
		if (today != tradeDay) {
			tradeDay = today;
			tradeUses.clear(); // a new day, a fresh stock
		}
		offers = CultivatorTrades.offersFor(this, player, tradeUses, offerKeys);
		setTradingPlayer(player);
		openTradingScreen(player, TalkSystem.nameOf(this), 0);
	}

	@Override
	public void setTradingPlayer(Player player) {
		if (player == null && tradingPlayer != null) endTalk(tradingPlayer);
		tradingPlayer = player;
	}

	@Override public Player getTradingPlayer() { return tradingPlayer; }
	@Override public MerchantOffers getOffers() { return offers == null ? new MerchantOffers() : offers; }
	@Override public void overrideOffers(MerchantOffers offers) { this.offers = offers; }

	@Override
	public void notifyTrade(MerchantOffer offer) {
		offer.increaseUses();
		String key = offerKeys.get(offer);
		if (key != null) tradeUses.merge(key, 1, Integer::sum);
		playSound(getNotifyTradeSound(), 0.6f, 1.2f);
	}

	@Override public void notifyTradeUpdated(ItemStack stack) {}
	@Override public int getVillagerXp() { return 0; }
	@Override public void overrideXp(int xp) {}
	@Override public boolean showProgressBar() { return false; }
	@Override public SoundEvent getNotifyTradeSound() { return SoundEvents.AMETHYST_BLOCK_CHIME; }
	@Override public boolean isClientSide() { return level().isClientSide; }

	/** While its trading screen is open it stands and faces the trader; walking off (or anything else) closes it. */
	private void tickTrading() {
		if (!(tradingPlayer instanceof ServerPlayer trader)) return;
		if (!trader.isAlive() || trader.distanceToSqr(this) > TalkSystem.KEEP * TalkSystem.KEEP || !(trader.containerMenu instanceof MerchantMenu)) {
			if (trader.containerMenu instanceof MerchantMenu) trader.closeContainer();
			setTradingPlayer(null);
			return;
		}
		holdTalk(trader);
		getNavigation().stop();
		getLookControl().setLookAt(trader, 30.0f, 30.0f);
	}

	// --- Sparring, mercy ---

	/**
	 * For a spar with {@code opponent} (SparSystem): it fights with no more than the strength of {@code rank} (or its own,
	 * {@link #NO_HOLD_BACK}). A bout it agreed to is nothing to run from, however far above it the opponent is (see
	 * {@link #senseDomains}).
	 */
	public void beginSpar(LivingEntity opponent, int holdBackTo) {
		if (fleeFrom == opponent) reachRefuge();
		sparHoldBack = holdBackTo;
		setMeditating(false);
		getNavigation().stop();
		refreshStats();
	}

	/** The spar is over: its own strength again, and no quarrel with whoever it fought. */
	public void endSpar(LivingEntity opponent) {
		sparHoldBack = NO_HOLD_BACK;
		refreshStats();
		letGoOf(opponent);
	}

	/** No quarrel with {@code opponent} any more (a spar ended; a ghost it fought scattered: GhostSystem#disperse), nor running from them. */
	public void letGoOf(LivingEntity opponent) {
		if (opponent != null && fleeFrom == opponent) reachRefuge();
		if (pursuer == opponent) pursuer = null;
		if (getTarget() == opponent) setTarget(null);
		if (getLastHurtByMob() == opponent) setLastHurtByMob(null);
		combatTicks = 0;
		setSuppressing(false);
		if (isQiFlying()) land();
	}

	public boolean isHoldingBack() { return sparHoldBack != NO_HOLD_BACK; }

	/** Takes {@code stakes} it won in a spar into its keeping (dropped when it dies). */
	public void keepWinnings(List<ItemStack> stakes) {
		for (ItemStack stake : stakes) if (!stake.isEmpty()) winnings.add(stake.copy());
	}

	/** Down on its knees before {@code player}, begging for its life, for {@code ticks}: it neither runs nor fights meanwhile. */
	public void beg(Player player, int ticks) {
		beggingTo = player.getUUID();
		begTicks = ticks;
		setKneeling(true);
		setMeditating(false);
		setTarget(null);
		getNavigation().stop();
		if (isQiFlying()) land();
	}

	public boolean isBeggingTo(Player player) {
		return begTicks > 0 && player.getUUID().equals(beggingTo);
	}

	public boolean isBegging() { return begTicks > 0; }

	/** The player it is begging, if they are here. */
	public Player beggedPlayer() {
		return begTicks > 0 && beggingTo != null ? level().getPlayerByUUID(beggingTo) : null;
	}

	public void stopBegging() {
		begTicks = 0;
		beggingTo = null;
		setKneeling(false);
	}

	/** {@code player} spared its life: it stops running from them and leaves them be for {@link #SPARED_TICKS}. */
	public void sparedBy(Player player) {
		stopBegging();
		sparedBy.put(player.getUUID(), level().getGameTime() + SPARED_TICKS);
		if (fleeFrom == player) reachRefuge();
		if (pursuer == player) pursuer = null;
		if (getTarget() == player) setTarget(null);
		if (getLastHurtByMob() == player) setLastHurtByMob(null);
		combatTicks = 0;
	}

	/** Spared by {@code player} lately: it won't go for them. */
	public boolean owesLife(Player player) {
		Long until = sparedBy.get(player.getUUID());
		return until != null && until > level().getGameTime();
	}

	// --- Everyday life (NpcLife, NpcSpeech) ---

	public NpcLife.Activity activity() { return activity; }
	public void setActivity(NpcLife.Activity activity) { this.activity = activity; }
	public long lastSpoke() { return lastSpoke; }
	public long speakingUntil() { return speakingUntil; }

	/** It just said something that stays up for {@code ticks}. */
	public void spoke(long now, int ticks) {
		lastSpoke = now;
		speakingUntil = now + ticks;
	}

	/** When it last remarked on (or bowed to) {@code who}; a long time ago if never. */
	public long remarkedAt(UUID who) {
		return remarked.getOrDefault(who, Long.MIN_VALUE / 2);
	}

	public void remark(UUID who, long now) {
		remarked.put(who, now);
	}

	// --- Setting up ---

	/**
	 * Makes this a cultivator of {@code rank} ({@link #MORTAL} for a mortal), re-applying its realm's strength: no higher than its
	 * world has room for in the realms after Four Axis (HighRealms).
	 */
	public void setRank(int rank) {
		setRank(rank, false);
	}

	/**
	 * {@link #setRank(int)}, or with {@code pastQuota} as it is, whatever room its world has (a body a player's soul cultivated:
	 * Possession); its place is held all the same.
	 */
	public void setRank(int rank, boolean pastQuota) {
		rank = Math.max(MORTAL, Math.min(rank, PlayerCultivation.LAST_RANK));
		// The census is the server thread's alone (a palace's king is made on worldgen's, of Four Axis at most: it never asks).
		boolean census = level() instanceof ServerLevel server && server.getServer().isSameThread()
				&& Math.max(rank, getRank()) >= PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY);
		if (census && !pastQuota) rank = HighRealms.allowed((ServerLevel) level(), getUUID(), rank);
		if (rank < 0) {
			cultivation.setMortal(true);
		} else {
			double kept = getRank() == rank ? cultivation.getCultivation() : 0;
			cultivation.setState(realmOf(rank), stageOf(rank), kept);
		}
		entityData.set(DATA_RANK, rank);
		refreshStats();
		if (census) HighRealms.hold(this, false);
	}

	public void setAlignment(int alignment) { entityData.set(DATA_ALIGNMENT, Alignment.clamp(alignment)); }

	public void setTitle(NpcTitle title) {
		NpcTitle before = getTitle();
		entityData.set(DATA_TITLE, (byte) title.ordinal());
		// Only Outer Disciples keep the gates (Sect#guardPost): one raised above them (its seniors fallen, say, while it was in
		// the thick of a fight and choosing no duty) is a gate guard no longer, and its name shows its new title.
		if (title != NpcTitle.OUTER_DISCIPLE) setGateGuard(false);
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

	/** The rank it fights at: its own as it can wield it here, or, holding back in a spar (SparSystem), its opponent's realm's. */
	public int fightingRank() {
		return sparHoldBack == NO_HOLD_BACK ? sustainedRank() : Math.min(sustainedRank(), Math.max(0, sparHoldBack));
	}

	/** Strength from realm and stage, as players get it; speed by the square root, since a mob's speed counts twice. */
	private void refreshStats() {
		float oldMax = getMaxHealth();
		float oldHealth = getHealth();
		// Holding back in a spar (SparSystem), it fights with the strength of its opponent's realm.
		int rank = fightingRank();
		Realm realm = isMortal() ? Realm.QI_REFINING : realmOf(rank);
		Stage stage = isMortal() ? Stage.EARLY : stageOf(rank);
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
			case ROGUE, WANDERER, WANDERING_GHOST, GHOST_ELDER, GATE_WARDEN, BRIDGE_KEEPER, GHOST_KING, GHOST_RETAINER ->
					rank < 0 ? 0 : Math.min(4, rank / Stage.values().length - 1 + random.nextInt(2));
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
			case FOUR_AXIS, DAO_INTEGRATION, VOID_SHATTERING, SACRED_VESSEL, ENTERING_NIRVANA, TRUE_IMMORTAL -> Items.NETHERITE_SWORD;
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

	/** Its face (one of the {@link #SKINS}): kept by one that sheds its body (GhostCultivatorNpc#shedBody). */
	public void setSkin(int skin) {
		entityData.set(DATA_SKIN, (byte) Mth.clamp(skin, 0, SKINS - 1));
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
		// One of the realms after Four Axis: seen in the world, holding its place there (HighRealms).
		if (tickCount % 100 == 1 && getRank() >= PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY)) HighRealms.hold(this, true);
		if (getTarget() != null || getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 100) combatTicks = 200;
		else if (combatTicks > 0) combatTicks--;
		tickQi();
		if (fleeTicks > 0 && --fleeTicks == 0) reachRefuge(); // given up on getting there: it stops running all the same
		if (pursuer != null && !pursuerNear()) pursuer = null; // left behind, or gone
		if (talkTicks > 0 && talkingTo != null) tickTalk();
		if (tradingPlayer != null) tickTrading();
		if (begTicks > 0) {
			getNavigation().stop();
			if (--begTicks == 0) stopBegging(); // no answer: it gets up and runs (or fights) again
		} else if (isKneeling() && tickCount % 10 == 0) {
			// Kneeling under someone's pressure (TalkSystem): only while they still press it and are still talking with it.
			if (!(talkPartner() instanceof ServerPlayer partner) || !TalkSystem.kneelsTo(partner, this)) setKneeling(false);
		}
		if (lifecycle == Lifecycle.SECLUDED && isConcealed()) repulse(level);
		tickFlight(level);
		NpcTravel.maybeDispatch(this, level);
	}

	/** Whether it is fighting, or was a moment ago. */
	public boolean isInCombat() { return combatTicks > 0; }

	private void secondTick(ServerLevel level) {
		// Asleep only while it means to be (NpcLife): anything that took it away from its bed has woken it.
		if (hasPose(Pose.SLEEPING) && (activity == null || activity.kind != NpcLife.Kind.SLEEP || !NpcLife.canLive(this))) setPose(Pose.STANDING);
		if (isMeditating() && !isMortal()) cultivate(1.0, meditationMultiplier(level));
		// Natural recovery out of combat: quicker once qi sustains the body, the whole of a great one mended in half a minute
		// (CultivationStats#mendPerSecond: the high realms' bodies took hours at two a second).
		if (!isInCombat() && getHealth() < getMaxHealth()) heal(isMortal() || getRank() < 4 ? 0.5f : CultivationStats.mendPerSecond(getMaxHealth()));
		setFlag(FLAG_DOMAIN, !isMortal() && (!isSectMember() || isInCombat()) && !isConcealed());
		if (isDomainOpen()) senseDomains(level);
		if (sectId != null) {
			Sect sect = SectManager.get(level.getServer(), sectId);
			if (sect == null || sect.extinct) {
				leaveSect(level, false);
			} else {
				Sect.Member member = sect.members.get(getUUID());
				if (member == null) {
					// The sect no longer counts it (let go for a newcomer while its land was unloaded, or a copy of one that
					// departed): it sets off, as it would have.
					dismiss(level);
				} else {
					member.lastAt = blockPosition(); // where to wake it, should it be let go once its land unloads
					if (member.title != getTitle()) setTitle(member.title);
				}
			}
		}
		// A lower-realm sect member who becomes a Heavenly Being has outgrown the sect.
		if (lifecycle == Lifecycle.SECT && ModDimensions.isLowerRealm(level.dimension()) && getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY)) {
			beginDeparture(level);
		}
		if (lifecycle == Lifecycle.ROGUE) decideRogueCourse();
		// A rogue at Qi Refining Mid's gate asks the heavens on its own, at some roadside altar, now and then (NpcLife).
		if (lifecycle == Lifecycle.ROGUE && awaitsHeavensLeave() && random.nextInt(600) == 0) NpcLife.ritualOffscreen(level, this);
		if (lifecycle == Lifecycle.SECLUDED && getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION)
				&& ModDimensions.isLowerRealm(level.dimension())) {
			endSeclusion(level);
			beginAscension();
			HermitSites.onLeft(level, this, false); // a region's hermit: another comes in time
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

	/**
	 * Meditates for {@code seconds} at a tenth of a player's rate (times {@code multiplier}): stage by stage, each at its own rate (a long catch-up passes several), until the time
	 * runs out or it is held (at Qi Refining Mid's gate, or the realm's cap). NpcLedger reckons ahead the same way.
	 */
	private void cultivate(double seconds, double multiplier) {
		for (int guard = 0; guard < 32 && seconds > 1.0e-9 && !isMortal() && !cultivation.isMaxed(); guard++) {
			double perSecond = cultivation.cultivationPerSecond() * NPC_RATE * multiplier
					* (cultivation.isInUpperRealm() ? PlayerCultivation.UPPER_REALM_QI_MULTIPLIER : 1.0);
			if (perSecond <= 0) return;
			double need = Math.max(0, cultivation.cultivationRequired() - cultivation.getCultivation());
			if (need > perSecond * seconds) {
				advanceUnchecked(perSecond * seconds);
				return;
			}
			int before = getRank();
			advanceUnchecked(need + 1.0e-6); // a hair over, so rounding can't leave it a hair short
			if (getRank() == before) return;
			seconds -= need / perSecond;
		}
	}

	/** Cultivation a second this NPC gathers meditating with nothing to help it: a tenth of a player's (used by the tests). */
	double meditationRate() {
		return cultivation.cultivationPerSecond() * NPC_RATE;
	}

	// --- The Ritual to the Heavens (see NpcLife) ---

	/** At Qi Refining Mid with its cultivation full and no leave from the heavens yet: it needs the Ritual to the Heavens. */
	public boolean awaitsHeavensLeave() { return !isMortal() && !isDemon() && cultivation.isAtRitualGate(); }

	/** The heavens grant their leave: it steps on into Qi Refining Late at once if its cultivation is full. */
	public void grantHeavensLeave() {
		cultivation.setHeavensLeave(true);
		advanceUnchecked(0);
	}

	public long ritualFailedDay() { return cultivation.getRitualFailedDay(); }

	public void setRitualFailedDay(long day) { cultivation.setRitualFailedDay(day); }

	/**
	 * Adds cultivation, passing every bottleneck but one (no tribulation for NPCs) and never past what the realm it stands in
	 * can sustain, nor into a realm after Four Axis its world has no more room in (HighRealms: it waits at the Grand Perfection
	 * below). The one is Qi Refining Mid: there it waits on the heavens' leave, as a player does. Package-private for the game tests.
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
				: level() instanceof ServerLevel server ? HighRealms.allowed(server, getUUID(), PlayerCultivation.LAST_RANK)
				: PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION);
		for (int guard = 0; guard < 32; guard++) {
			cultivation.setState(realm, stage, 0);
			double required = cultivation.cultivationRequired();
			if (value < required) break;
			int rank = PlayerCultivation.rank(realm, stage);
			if (rank >= cap || rank == PlayerCultivation.RITUAL_RANK && !cultivation.hasHeavensLeave()) {
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
			if (Math.max(before, after) >= PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY)) HighRealms.hold(this, true);
			level().playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.2f);
			if (sectId != null && level() instanceof ServerLevel server) SectManager.onMemberRankChanged(server, this);
			if (after > before && !catchingUp) NpcLife.onBreakthrough(this);
		}
	}

	/**
	 * The time its chunk spent unloaded, part of it spent meditating, however long it was. Held at Qi Refining Mid's gate,
	 * it will have asked the heavens once a day while away (each try on its own day); granted, it cultivated on for the
	 * days that were left.
	 */
	private void catchUp(ServerLevel level) {
		if (lastSeen < 0) return;
		catchUp(level, level.getGameTime() - lastSeen, HeavenRitual.today(level.getServer()));
	}

	private void catchUp(ServerLevel level, long elapsed, long today) {
		if (isMortal() || elapsed < MIN_CATCH_UP) return;
		catchingUp = true;
		try {
			cultivate(elapsed / 20.0 * CATCH_UP_SHARE, 1.0);
			if (!awaitsHeavensLeave()) return;
			int days = (int) Math.max(1, Math.min(MAX_CATCH_UP_RITUALS, elapsed / HeavenRitual.DAY));
			for (int i = 0; i < days; i++) {
				if (!NpcLife.ritualOffscreen(level, this, today - days + 1 + i)) continue;
				cultivate(elapsed / 20.0 * CATCH_UP_SHARE * (days - 1 - i) / days, 1.0);
				break;
			}
		} finally {
			catchingUp = false;
		}
	}

	/** The game time it was last seen ticking (-1: never), for working out what it did while unloaded (NpcLedger). */
	public long lastSeen() { return lastSeen; }

	/** As if it had been away {@code ticks} and come back on game day {@code today} (game tests). */
	void catchUpForTest(ServerLevel level, long ticks, long today) {
		catchUp(level, ticks, today);
	}

	// --- Qi, flight, suppression ---

	private void tickQi() {
		if (isMortal()) return;
		double spend = 0;
		if (isQiFlying()) {
			// The sword or qi alone, decided again each tick: in a hurry it flies on qi if it can, otherwise it rides.
			boolean sword = flag(FLAG_SWORD);
			boolean canRide = SwordFlight.canRide(this, cultivation);
			if (sword && (!canRide || hurried() && canQiFlySustainably())) sword = false;
			else if (!sword && canRide && !hurried()) sword = true;
			if (!sword && !cultivation.getEffectiveRealm().canFlyOnQi()) {
				setQiFlying(false); // its sword gone and too weak to fly without one: it falls
			} else {
				setFlag(FLAG_SWORD, sword);
				double cost = sword ? cultivation.swordFlightCostPerSecond() : cultivation.qiFlightCostPerSecond();
				if (cultivation.getQi() + (cultivation.qiGatherPerSecond() - cost) / 20.0 < 0) {
					setQiFlying(false); // out of qi in the air: it falls, as a player would
				} else {
					spend += cost;
				}
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

	/** Flight it can keep up, on qi or on its sword (from Foundation Building, with a sword in hand). */
	public boolean canFlySustainably() {
		return canQiFlySustainably() || canSwordFlySustainably();
	}

	private boolean canQiFlySustainably() {
		return !isMortal() && cultivation.getEffectiveRealm().canFlyOnQi() && affords(cultivation.qiFlightCostPerSecond());
	}

	private boolean canSwordFlySustainably() {
		return !isMortal() && SwordFlight.canRide(this, cultivation) && affords(cultivation.swordFlightCostPerSecond());
	}

	/** Endless where gathering covers the cost, otherwise only with ten seconds' reserve in hand. Never in a passage (the Gap, the Soul River). */
	private boolean affords(double cost) {
		if (ModDimensions.isPassage(level().dimension())) return false;
		if (cultivation.qiGatherPerSecond() >= cost) return true;
		return cultivation.getQi() >= cost * 10 && cultivation.getQi() >= cultivation.maxQi() * 0.4;
	}

	/** In a fight or running from danger: speed matters more than qi, so it flies on qi alone if it can. */
	private boolean hurried() {
		return getTarget() != null && isInCombat() || shouldFlee();
	}

	/**
	 * Takes to the air (or comes down). It rides its sword, cheaper and a little slower, unless it is in a hurry and can fly
	 * on qi alone; {@link #tickQi} switches between the two as that changes.
	 */
	public void setQiFlying(boolean flying) {
		if (flying == isQiFlying()) return;
		if (flying && !canFlySustainably()) return;
		setFlag(FLAG_SWORD, flying && canSwordFlySustainably() && (!canQiFlySustainably() || !hurried()));
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
		steer(target, isSwordFlying() ? speed * SwordFlight.SPEED_FACTOR : speed);
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
		// Flying straight at something (a tree, a roof, a hillside) it would hang against it: it climbs over instead.
		if (horizontalCollision && !landing) setDeltaMovement(getDeltaMovement().add(0, 0.12, 0));
		getLookControl().setLookAt(target.x, target.y, target.z);
		setYRot((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0f);
		yBodyRot = getYRot();
	}

	/** While flying on purpose (a journey, a chase), it doesn't drop out of the air on touching ground. */
	private boolean shouldStayAloft() {
		return lifecycle == Lifecycle.DEPARTING || lifecycle == Lifecycle.ASCENDING || getTarget() != null && isInCombat();
	}

	/** About how fast it comes down landing ({@link #tickFlight}: it settles near 5 blocks a second), with a margin. */
	private static final double DESCENT_PER_SECOND = 4;

	/**
	 * In a spar, flying with qi for no more than coming down gently from where it is and two seconds over: it comes down then
	 * (CombatGoal) rather than drop out of the sky as in earnest ({@link #tickQi}). A fall in a friendly bout stops at no line
	 * (SparSystem#allowDeath stops only the partner's blows there).
	 */
	public boolean runningLowAloft() {
		if (!isQiFlying() || !SparSystem.isSparring(this) || !(level() instanceof ServerLevel level)) return false;
		double cost = isSwordFlying() ? cultivation.swordFlightCostPerSecond() : cultivation.qiFlightCostPerSecond();
		double shortfall = cost - cultivation.qiGatherPerSecond();
		if (shortfall <= 0) return false;
		int ground = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
		return cultivation.getQi() < shortfall * (2 + Math.max(0, getY() - ground) / DESCENT_PER_SECOND);
	}

	/**
	 * In the air on qi it goes as fast as its realm lets it walk; on its sword, at the sword's share of its base walking
	 * speed, the same at every realm (a sword is no faster for a stronger rider, see {@link SwordFlight}).
	 */
	@Override
	protected float getFlyingSpeed() {
		if (isSwordFlying()) return (float) getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * 0.1f * SwordFlight.SPEED_FACTOR;
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
	 * {@link #holdsGround holds its ground}. Never the one it is sparring with: it agreed to that bout (it used to run, the spar
	 * counted as the player walking off, and it kept on running after).
	 */
	void senseDomains(ServerLevel level) {
		if (holdsGround()) return;
		int mine = sustainedRank();
		double radius = domainRadius();
		// A soul out of its body bears down on no domain (see SoulProjection): nothing to run from.
		for (LivingEntity other : ConsciousnessDomainHandler.entitiesIn(this, Math.min(radius, 96), LivingEntity.class,
				e -> e.isAlive() && CultivatorEntity.isCultivator(e) && !(e instanceof Player player && SoulProjection.isSoul(player))
						&& !SparSystem.areSparring(this, e))) {
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
			// Nowhere far to run to: it only backs away, for as long as it keeps sensing what it fears and 30 seconds after.
			fleeTicks = refuge != null ? FLEE_TIMEOUT : 0;
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
	 * running, only a beating, and not even that for the master (see {@link #shouldFlee}). Disciples and rogues have no such duty.
	 * A Merit wish's threat (Wishes) came for the one who took it on, and doesn't run from them either.
	 */
	public boolean holdsGround() {
		return lifecycle == Lifecycle.SECT && getTitle().isElderOrAbove() || Wishes.isThreat(this);
	}

	/**
	 * A player's or NPC's rank as cultivators weigh each other ({@link #MORTAL} for mortals, and for a cultivator hidden behind its
	 * Concealment Barrier): a player's by either path (TwoPaths#weighedRank, its body's); a demon beast's (or any other CultivatorEntity's)
	 * its own, where before the sweep of 2026-10-23 it was weighed a mortal (a core blast's share, a cultivator holding back from it).
	 * Creative players keep their rank; who may take them for an enemy is decided elsewhere (isEnemy).
	 */
	public static int rankOf(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			PlayerCultivation c = CultivationManager.get(player);
			return c.isMortal() ? MORTAL : TwoPaths.weighedRank(c);
		}
		if (entity instanceof CultivatorNpc npc) return npc.isConcealed() ? MORTAL : npc.sustainedRank();
		if (entity instanceof CultivatorEntity other) {
			return other.isMortal() || other.isConcealed() ? MORTAL : PlayerCultivation.rank(other.getCultivationRealm(), other.getCultivationStage());
		}
		return MORTAL;
	}

	// --- Allies and enemies ---

	/**
	 * One of its own sect: a cultivator of it, or a player who is its disciple (SectMembership), or one in the body of either
	 * (Possession: it is the body it knows).
	 */
	public boolean isAlly(Entity other) {
		if (sectId == null) return false;
		if (other instanceof CultivatorNpc npc) return sectId.equals(npc.sectId);
		if (!(other instanceof ServerPlayer player)) return false;
		if (Possession.inhabits(player.getUUID())) return sectId.equals(Possession.sectOfBody(player.getUUID()));
		return SectMembership.isDisciple(player.server, sectId, player.getUUID());
	}

	@Override
	public boolean isAlliedTo(Entity other) {
		return isAlly(other) || super.isAlliedTo(other);
	}

	/**
	 * Whether it can sense {@code other} at all: a projected nascent soul (see SoulProjection) is there only for cultivators of
	 * Nascent Soul and above, who see it as players of those realms do; to the rest it doesn't exist. A player's ghost, likewise,
	 * to the living (below Nascent Soul they couldn't touch it either: SoulProjection#harmFactor), but in the Netherworld, where
	 * everyone sees the dead, to all; and one of the living in a Ghost King's form to none of the living at all
	 * (TwoPaths#inGhostForm). The dead's own kind see the dead, and that form. A Ghost King in its human form is seen as the living
	 * are, by everyone (TwoPaths#inHumanForm, as players see it: SoulProjection#seenOnlyBySeers), so the dead of Four Axis may walk
	 * among the living and grant their wishes (Wishes).
	 */
	public boolean senses(LivingEntity other) {
		if (!(other instanceof Player player)) return true;
		if (SoulProjection.isSoul(player)) return seesSouls();
		if (!GhostSystem.seenAsDead(player)) return true;
		if (this instanceof Ghostly) return true;
		if (player instanceof ServerPlayer sp && Incorporeal.isGhost(sp) && TwoPaths.inHumanForm(sp)) return true;
		return Incorporeal.isGhost(player) && (seesSouls() || ModDimensions.isNetherworld(level().dimension()));
	}

	/** Nascent Soul or above: it sees souls, and the dead, as players of those realms do. */
	private boolean seesSouls() {
		return !isMortal() && sustainedRank() >= PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
	}

	/** The righteous and the devilish are enemies wherever they meet; the neutral are nobody's. */
	public boolean isEnemy(LivingEntity other) {
		if (other == this || !other.isAlive() || isAlly(other) || !senses(other)) return false;
		if (other instanceof Player player) {
			if (player.isCreative() || player.isSpectator()) return false;
			// A Merit wish's threat (Wishes) is out for the one who took it on, whatever their path.
			if (Wishes.isThreat(this) && player.getUUID().equals(Wishes.takerOf(this))) return true;
			if (owesLife(player)) return false; // they spared its life: not today
			if (SparSystem.areSparring(this, player)) return false; // a spar is a contest, not a feud
			if (getFaction().isEnemyOf(Alignment.factionOf(alignmentOf(player)))) return true;
			// A man on a demon clan's grounds (the user's pick, 2026-10-23: wary, so attacked there, let be outside unless provoked).
			if (!level().isClientSide && isDemon() && trespassingMan(player)) return true;
			// Loathing turns it on a player too, if it thinks it can win.
			return !level().isClientSide && regard(player) <= Regard.HOSTILE && rankOf(this) >= rankOf(player);
		}
		if (other instanceof CultivatorNpc npc) return !npc.isConcealed() && !npc.isVacant() && getFaction().isEnemyOf(npc.getFaction());
		return false;
	}

	/** Someone it is (or should be) fighting, or who is fighting it. */
	public boolean isThreat(LivingEntity other) {
		if (isAlly(other) || !senses(other)) return false;
		if (getTarget() == other || getLastHurtByMob() == other) return true;
		if (other instanceof Mob mob && mob.getTarget() == this) return true;
		return isEnemy(other);
	}

	/** What its Realm Suppress weighs on: foes in its domain, never its own sect or bystanders. */
	public boolean isHostileTo(LivingEntity other) {
		if (other == this || isAlly(other) || !senses(other)) return false;
		if (getTarget() == other || getLastHurtByMob() == other) return true;
		return other instanceof Mob mob && mob.getTarget() == this;
	}

	/**
	 * {@code player} is a man (no beast's blood: DemonPath#isDemon) on the grounds of its clan; never one of the clan's own players, nor
	 * one sparring with one of the clan (or who did a moment ago: SparSystem#sparsWithSect), whom the rest of the clan set on mid-bout
	 * before the sweep of 2026-10-23.
	 */
	private boolean trespassingMan(Player player) {
		Sect sect = sect();
		if (sect == null || !sect.isClan() || !(player instanceof ServerPlayer sp) || sect.disciples.containsKey(sp.getUUID())) return false;
		if (SparSystem.sparsWithSect(sp, sect.id)) return false;
		return !DemonPath.isDemon(CultivationManager.get(sp)) && sect.dimension == level().dimension() && sect.inTerritory(player.blockPosition());
	}

	public static int alignmentOf(Player player) {
		return CultivationManager.forPlayer(player).getAlignment();
	}

	/** It turned on {@code player} unprovoked (their path offends its own): killing it is then self-defence, not murder. */
	public void markAggressed(LivingEntity target) {
		if (target instanceof Player player) aggressed.add(player.getUUID());
	}

	/** {@code player} provoked it into a fight: whatever it did before, killing it now is murder again. */
	public void provokedBy(Player player) {
		aggressed.remove(player.getUUID());
	}

	/**
	 * Running: from a domain a realm above its own (unless it {@link #holdsGround}), or from a fight it is losing badly (unless
	 * it {@link #makesLastStand}).
	 */
	public boolean shouldFlee() {
		return fleeFrom != null && fleeFrom.isAlive() && !fleeFrom.isRemoved() && !holdsGround()
				|| isBadlyHurt() && (isInCombat() || pursuerNear()) && !makesLastStand();
	}

	/** Who last struck it, while they are still after it ({@link #pursuer}), or null. */
	public LivingEntity getPursuer() { return pursuerNear() ? pursuer : null; }

	/** Its last attacker still about: alive, here, within {@link #PURSUER_LOST}. */
	private boolean pursuerNear() {
		return pursuer != null && pursuer.isAlive() && !pursuer.isRemoved() && pursuer.level() == level()
				&& pursuer.distanceToSqr(this) <= PURSUER_LOST * PURSUER_LOST
				&& !(pursuer instanceof Player player && (player.isCreative() || player.isSpectator()));
	}

	/**
	 * A Sect Master never runs, even from a beating: it makes its last stand in its own sect. Its elders may fall back. Nor does a
	 * Merit wish's threat (Wishes): it fights to the end, never begging for its life (TalkSystem#wouldBeg).
	 */
	public boolean makesLastStand() {
		return lifecycle == Lifecycle.SECT && getTitle() == NpcTitle.SECT_MASTER || Wishes.isThreat(this);
	}

	/** Below {@link #FLEE_HEALTH} of its health. */
	public boolean isBadlyHurt() {
		return getHealth() < getMaxHealth() * FLEE_HEALTH;
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
		setGateGuard(false);
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
		setOff(level, true);
	}

	/**
	 * Let go by its sect, to make room for a newcomer from below (SectManager#admit), or found no longer on its roll: it
	 * leaves as one who departs does, but no replacement is sent for, since its place is taken.
	 */
	public void dismiss(ServerLevel level) {
		setOff(level, false);
	}

	/**
	 * Leaves its sect of its own accord, to make its own way as one who departs does (a replacement is sent for): as a soul in its body
	 * would have it (SectMembership#leaveAsBody); it sets off once the body is its own again.
	 */
	public void leaveToWander(ServerLevel level) {
		setOff(level, true);
	}

	private void setOff(ServerLevel level, boolean replaced) {
		Sect sect = sect();
		BlockPos from = sect == null ? blockPosition() : sect.core;
		// Let go while unloaded and woken for it (SectManager#rouse): it has set off, so it needn't be looked for any more.
		if (sect != null && sect.letGo.remove(getUUID()) != null) SectManager.dirty(level.getServer());
		leaveSect(level, replaced);
		double angle = random.nextDouble() * Math.PI * 2;
		double distance = 200 + random.nextDouble() * 800;
		journeyTarget = BlockPos.containing(from.getX() + Math.cos(angle) * distance, from.getY(), from.getZ() + Math.sin(angle) * distance);
		lifecycle = Lifecycle.DEPARTING;
		setMeditating(false);
		setTarget(null);
		setPersistenceRequired();
		level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 1.0f, 1.4f);
	}

	/**
	 * A rogue's next step: Heavenly Beings in the lower realms seek seclusion, and at Grand Perfection go to ascend. One just
	 * come up into the Upper Realm looks once for a sect of its path with room (SectManager#findOpening): it holds the slot
	 * and goes to join; with none to be had, it seeks seclusion there, to cultivate toward Four Axis.
	 */
	public void decideRogueCourse() {
		// What preys on a settlement (a Merit wish's threat, Wishes) has one course: the one who took it on. No seclusion, no ascent.
		if (lifecycle != Lifecycle.ROGUE || isMortal() || Wishes.isThreat(this)) return;
		if (ModDimensions.isUpperRealm(level().dimension())) {
			if (ascended && level() instanceof ServerLevel level) seekSectOrSeclusion(level);
			return;
		}
		if (!ModDimensions.isLowerRealm(level().dimension())) return;
		if (getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION)) {
			beginAscension();
		} else if (getRank() >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY)) {
			lifecycle = Lifecycle.SEEKING_SECLUSION;
			journeyTarget = null;
			setPersistenceRequired();
		}
	}

	/** Just come up into the Upper Realm, its course there not chosen yet. */
	public boolean isNewlyAscended() { return ascended; }

	private void seekSectOrSeclusion(ServerLevel level) {
		ascended = false; // decided once
		Sect sect = SectManager.findOpening(level, this);
		if (sect != null) {
			SectManager.reserve(level, sect, getUUID());
			joiningSect = sect.id;
			journeyTarget = sect.post(Sect.Post.GATE, 0);
			lifecycle = Lifecycle.DEPARTING;
		} else {
			lifecycle = Lifecycle.SEEKING_SECLUSION;
			journeyTarget = null;
		}
		setPersistenceRequired();
	}

	/** A journey (departure, going to join a sect, or ascension) has reached its end. */
	public void arrive(ServerLevel level) {
		if (lifecycle == Lifecycle.DEPARTING && joiningSect != null) {
			Sect sect = SectManager.get(level.getServer(), joiningSect);
			joiningSect = null;
			journeyTarget = null;
			lifecycle = Lifecycle.ROGUE;
			// No room after all (the sect fell, or lost slots to a player's kills): into seclusion instead.
			if (sect == null || !SectManager.admit(level, sect, this)) {
				lifecycle = Lifecycle.SEEKING_SECLUSION;
				setPersistenceRequired();
			}
		} else if (lifecycle == Lifecycle.DEPARTING) {
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

	/**
	 * At the rift: into the Spatial Gap, and out into the Upper Realm, or crushed on the way. From the Nether its rift leads up
	 * the Soul River instead: it comes out at the Ghost Gate as one of the dead (GhostCultivatorNpc#shedBody), or its soul
	 * scatters on the way.
	 */
	public void attemptAscension(ServerLevel level) {
		if (level.dimension() == net.minecraft.world.level.Level.NETHER) {
			ServerLevel netherworld = level.getServer().getLevel(ModDimensions.NETHERWORLD);
			level.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 1.0f, 0.7f);
			if (netherworld == null || random.nextFloat() >= ASCENSION_SUCCESS) {
				hurt(ModDamageTypes.soulScatter(level), Float.MAX_VALUE);
				return;
			}
			GhostCultivatorNpc ghost = GhostCultivatorNpc.shedBody(this, netherworld);
			travelling = true; // not a departure: it goes on, as one of the dead
			discard();
			if (ghost != null) GhostCultivatorNpc.arriveAtGate(ghost, netherworld);
			return;
		}
		ServerLevel upper = level.getServer().getLevel(ModDimensions.UPPER_REALM);
		level.playSound(null, blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.NEUTRAL, 0.6f, 1.6f);
		if (upper == null || random.nextFloat() >= ASCENSION_SUCCESS) {
			hurt(ModDamageTypes.spatialPressure(level), Float.MAX_VALUE);
			return;
		}
		completeAscension();
		NpcTravel.sendToUpperRealm(this, level, upper);
	}

	/**
	 * Through the rift and out into the Upper Realm: a rogue again, no longer held by the lower realms' cap, with its course
	 * there to choose once it has arrived (a sect to join, or seclusion: see {@link #decideRogueCourse}).
	 */
	public void completeAscension() {
		lifecycle = Lifecycle.ROGUE;
		ascended = true;
		journeyTarget = null;
		landing = false;
		setQiFlying(false);
	}

	/** Comes down out of the air (gently, so it takes no fall). */
	public void land() {
		if (isQiFlying()) landing = true;
	}

	/**
	 * While flying: lands when asked to or when nothing is steering it any more; drops out of the sky with no qi left. Package-private
	 * for the game tests.
	 */
	void tickFlight(ServerLevel level) {
		if (!isQiFlying()) {
			landing = false;
			return;
		}
		// Borne up, it falls only from where it leaves the air (as vanilla's flying players do). Before 2026-10-24 a gentle landing
		// struck it with the whole way down: its flight ends a little above the ground.
		resetFallDistance();
		// Nothing has steered it for half a second (the goal that took it up has ended): it comes down.
		if (!landing && tickCount - lastSteered > 10) landing = true;
		if (landing) {
			int ground = Formations.surface(level, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, getBlockX(), getBlockZ());
			if (ground <= level.getMinBuildHeight() + 1) {
				// Nothing under it but open sky (the Upper Realm's abyss): it holds where it is rather than sink into it, and
				// comes down once something steers it over land.
				setDeltaMovement(getDeltaMovement().scale(0.6));
				return;
			}
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

	static boolean isSeatable(ServerLevel level, BlockPos feet) {
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
		Rumours.noteCave(this); // word gets about: a Heavenly Being has shut itself away here
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
		Rumours.forgetCave(this);
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
		if (isVacant() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false; // set aside: its body is elsewhere
		boolean hurt = super.hurt(source, amount);
		if (hurt && !level().isClientSide && level() instanceof ServerLevel server) {
			setMeditating(false);
			LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
			// Struck while begging by the one it begged: no mercy, then; it runs or fights again.
			if (attacker instanceof Player player && isBeggingTo(player)) stopBegging();
			// Blows in a spar (SparSystem) are a contest: no alarm, no grudge, no broken seclusion.
			if (attacker != null && SparSystem.areSparring(this, attacker)) return hurt;
			if (attacker != null && attacker != this && !isAlly(attacker)) pursuer = attacker; // to run from, should it come to that
			if (attacker != null && lifecycle == Lifecycle.SECLUDED && isConcealed() && attacker instanceof ServerPlayer player) {
				onConcealmentAssailed(player);
			}
			// One of its sect's players striking it (outside a spar: that returned above) is cast out first (SectMembership), and so
			// is answered by the sect as anyone would be.
			if (attacker instanceof ServerPlayer player && sectId != null) SectMembership.onFellowStruck(this, player);
			if (attacker != null && sectId != null && !isAlly(attacker)) SectManager.onMemberAttacked(server, this, attacker);
			if (attacker instanceof ServerPlayer player && !player.isCreative()) {
				// Striking it costs its regard, once a fight (blows in quick succession count as one).
				long now = server.getGameTime();
				Long last = struckAt.put(player.getUUID(), now);
				if (last == null || now - last > STRUCK_MEMORY) changeRegard(player, Regard.STRUCK);
				endTalk(player);
			}
		}
		return hurt;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(level() instanceof ServerLevel server)) return;
		ServerPlayer killer = killerOf(source);
		HighRealms.gone(server.getServer(), getUUID()); // a place among the few after Four Axis free again
		NpcLife.end(this);
		NpcLife.onDeath(server, this); // its fellows cry out, and mourn it where it fell
		if (sectId != null) SectManager.onMemberDeath(server, this, killer != null);
		if (lifecycle == Lifecycle.SECLUDED) Rumours.forgetCave(this);
		HermitSites.onLeft(server, this, killer != null); // a region's hermit: replaced in time, unless a player killed it
		if (joiningSect != null) SectManager.release(server, joiningSect, getUUID()); // the slot it held goes to a new disciple
		if (killer != null) {
			Rumours.noteKilling(this, killer); // people will talk
			// Killing a sect member turns the whole sect against the killer; killing one who was attacking a cultivator earns that
			// cultivator's thanks (and a little of its sect's).
			if (sectId != null) SectManager.changeStanding(server.getServer(), sectId, killer.getUUID(), Regard.MEMBER_KILLED);
			if (getTarget() instanceof CultivatorNpc victim && victim != this && victim.isAlive()) {
				victim.changeRegard(killer, Regard.DEFENDED);
				if (victim.getSectId() != null && !victim.getSectId().equals(sectId)) {
					SectManager.changeStanding(server.getServer(), victim.getSectId(), killer.getUUID(), Regard.DEFENDED_SECT);
				}
			}
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

	/**
	 * What it carries falls when it dies: spirit stones by its realm (SpiritStoneItem; a mortal now and then has a Low one or
	 * two), whatever it won in spars, and now and then the slip of its heaven method. A demon leaves its demon core besides, always
	 * (DemonCoreItem: its rank's, its race's element), unless it is one of the dead, whose body is long gone.
	 */
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		if (isDemon() && !isMortal() && !isGhost()) {
			spawnAtLocation(DemonCoreItem.of(getRank(), DemonPath.element(getBloodline()), getName()));
			// Now and then its race's blood, of its realm (DemonBlood), none beyond what the worlds hold (Realm#atMostTaught).
			if (random.nextFloat() < DemonBlood.NPC_CHANCE) spawnAtLocation(DemonBloodItem.of(getBloodline(), DemonPath.realm(getRank()).atMostTaught()));
		}
		for (ItemStack stack : carriedStones(getRank(), random)) spawnAtLocation(stack);
		for (ItemStack stack : winnings) spawnAtLocation(stack);
		winnings.clear();
		// Its heaven method's slip: never a True Immortal's, the Immortal Realm's (Realm#taughtInTheWorld).
		HeavenMethod method = heavenMethod();
		if (method != null && method.realm().taughtInTheWorld() && random.nextFloat() < (isSectMember() ? HEAVEN_SLIP_CHANCE_SECT : HEAVEN_SLIP_CHANCE)) {
			spawnAtLocation(JadeSlipItem.of(method));
		}
	}

	/** A cultivator struck down leaves its heaven method's slip this often; a sect member (who keeps its sect's own closer), this often. */
	public static final float HEAVEN_SLIP_CHANCE = 0.25f, HEAVEN_SLIP_CHANCE_SECT = 0.1f;
	/** In ten cultivators (not a sect's elders and master, who always do), how many cultivate their own realm's method of their element. */
	public static final int OWN_HEAVEN_METHOD_IN_TEN = 4;

	/**
	 * The heaven method it cultivates, the one it teaches and sells (picked from who it is, so it never changes): none for a
	 * commoner or one of the dead. Its element is its sect's (Sect#element), or for one of no sect its own; its realm its own (a
	 * sect's elders and master always, others four times in ten), else one below it at random.
	 */
	public HeavenMethod heavenMethod() {
		if (isMortal() || isGhost()) return null;
		Sect sect = isSectMember() ? sect() : null;
		// A demon's blood is its element's (DemonPath#element), as its clan's is.
		QiElement element = sect != null ? sect.element() : isDemon() ? DemonPath.element(getBloodline())
				: QiElement.five()[(int) Math.floorMod(getUUID().getLeastSignificantBits() >>> 3, 5L)];
		int own = Math.min(Realm.values().length - 1, getRank() / Stage.values().length);
		int hash = getUUID().hashCode() * 17 + 3;
		int realm = getTitle().isElderOrAbove() || Math.floorMod(hash, 10) < OWN_HEAVEN_METHOD_IN_TEN ? own : Math.floorMod(hash / 10, own + 1);
		return HeavenMethod.of(element, Realm.byIndex(realm));
	}

	/** The spirit stones a cultivator of {@code rank} carries ({@link #MORTAL}: a commoner). */
	public static List<ItemStack> carriedStones(int rank, RandomSource random) {
		List<ItemStack> stones = new ArrayList<>();
		if (rank < 0) {
			if (random.nextBoolean()) stones.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 1 + random.nextInt(2)));
			return stones;
		}
		switch (realmOf(rank)) {
			case QI_REFINING -> stones.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 1 + random.nextInt(4)));
			case FOUNDATION_BUILDING -> {
				stones.add(new ItemStack(ModItems.LOW_SPIRIT_STONE, 3 + random.nextInt(7)));
				if (random.nextFloat() < 0.2f) stones.add(new ItemStack(ModItems.MID_SPIRIT_STONE));
			}
			case CORE_FORMATION -> stones.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 1 + random.nextInt(3)));
			case NASCENT_SOUL -> {
				stones.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 2 + random.nextInt(5)));
				if (random.nextFloat() < 0.2f) stones.add(new ItemStack(ModItems.HIGH_SPIRIT_STONE));
			}
			case HEAVENLY_BEING -> stones.add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 1 + random.nextInt(3)));
			case FOUR_AXIS -> {
				stones.add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 2 + random.nextInt(5)));
				if (random.nextFloat() < 0.15f) stones.add(new ItemStack(ModItems.SUPREME_SPIRIT_STONE));
				if (random.nextFloat() < 0.01f) stones.add(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE));
			}
			case DAO_INTEGRATION -> {
				stones.add(new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 1 + random.nextInt(3)));
				if (random.nextFloat() < 0.05f) stones.add(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE));
			}
			case VOID_SHATTERING, SACRED_VESSEL -> {
				stones.add(new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 3 + random.nextInt(6)));
				stones.add(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE, 1 + random.nextInt(realmOf(rank) == Realm.SACRED_VESSEL ? 3 : 1)));
			}
			case ENTERING_NIRVANA, TRUE_IMMORTAL -> stones.add(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE, 2 + random.nextInt(4)));
		}
		return stones;
	}

	/**
	 * Who answers for its death: the player who struck it, whose arrow or pet did, or who last hurt it before it fell; else the player
	 * still hunting it ({@link #pursuer}), or the one who last struck it within {@link #STRUCK_MEMORY} (its fall from a knock, its fire,
	 * its poison come later than vanilla's five seconds: such a death was nobody's before the sweep of 2026-10-23, its sect none the wiser).
	 */
	private ServerPlayer killerOf(DamageSource source) {
		Entity entity = source.getEntity();
		if (entity instanceof ServerPlayer player) return player;
		if (entity instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer owner) return owner;
		if (lastHurtByPlayer instanceof ServerPlayer player && lastHurtByPlayerTime > 0) return player;
		if (getKillCredit() instanceof ServerPlayer player) return player;
		if (pursuer instanceof ServerPlayer player && pursuerNear()) return player;
		long now = level().getGameTime(), latest = Long.MIN_VALUE;
		ServerPlayer struck = null;
		for (Map.Entry<UUID, Long> blow : struckAt.entrySet()) {
			if (now - blow.getValue() > STRUCK_MEMORY || blow.getValue() <= latest || level().getServer() == null) continue;
			ServerPlayer player = level().getServer().getPlayerList().getPlayer(blow.getKey());
			if (player == null) continue;
			struck = player;
			latest = blow.getValue();
		}
		return struck;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!level().isClientSide && level() instanceof ServerLevel server && reason == RemovalReason.DISCARDED && !travelling && isAlive()) {
			// Taken out of the world without dying (despawned, discarded by a command): for its sect, it simply left.
			if (sectId != null) leaveSect(server, true);
			if (joiningSect != null) SectManager.release(server, joiningSect, getUUID());
			lowerConcealment(server);
			setSuppressing(false);
			HighRealms.gone(server.getServer(), getUUID());
		}
		if (!level().isClientSide) {
			NpcLife.end(this); // a talk or a bout it was in is off
			Spells.forget(getUUID()); // what its spells left lingering ends with it (CultivatorSpells)
		}
		super.remove(reason);
	}

	/** A blow it lands carries what its spells lend its hands (Flame Fist's fire: SpellAuras#onMelee), as a player's does. */
	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
		boolean hit = super.doHurtTarget(target);
		if (hit && target instanceof LivingEntity living && !level().isClientSide) SpellAuras.onMelee(this, living);
		return hit;
	}

	/**
	 * Cultivators never despawn: sect members, rogues and wanderers alike stay in the world until they die or move on of their
	 * own accord (a journey, an ascension). The spawner keeps rogues rare instead (NpcSpawner).
	 */
	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	/**
	 * A rogue cultivator out about the land: the kind NpcSpawner keeps to one near each player. Those in seclusion or on a
	 * journey (departing, seeking seclusion, going to ascend) don't count; nor do mortal wanderers, who are kept to one near
	 * each player of their own ({@link #isWanderingMortal}). One a player has named (a name tag) still counts: naming a rogue
	 * mustn't make room for another.
	 */
	public boolean isWanderingRogue() {
		return lifecycle == Lifecycle.ROGUE && sectId == null && !isMortal();
	}

	/** A mortal wanderer out about the land (of no sect; named by a player or not). */
	public boolean isWanderingMortal() {
		return lifecycle == Lifecycle.ROGUE && sectId == null && isMortal();
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

	/** Right-click: talk to it (see {@link TalkSystem}). */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!level().isClientSide && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
			TalkSystem.open(serverPlayer, this);
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
		if (isDemon()) {
			npc.putString("Race", getBloodline().getId());
			npc.putInt("RaceVariant", getRaceVariant());
		}
		npc.putString("Lifecycle", lifecycle.name());
		if (sectId != null) npc.putUUID("Sect", sectId);
		npc.putInt("Home", homeIndex);
		if (home != null) npc.put("HomePos", NbtUtils.writeBlockPos(home));
		if (journeyTarget != null) npc.put("Journey", NbtUtils.writeBlockPos(journeyTarget));
		if (ascended) npc.putBoolean("Ascended", true);
		if (joiningSect != null) npc.putUUID("JoiningSect", joiningSect);
		if (seclusionMat != null) npc.put("SeclusionMat", NbtUtils.writeBlockPos(seclusionMat));
		if (concealment != null) npc.putUUID("Concealment", concealment);
		npc.putBoolean("Concealed", isConcealed());
		npc.putLong("LastSeen", lastSeen);
		npc.putInt("SeclusionAttempts", seclusionAttempts);
		ListTag list = new ListTag();
		for (UUID id : aggressed) list.add(NbtUtils.createUUID(id));
		npc.put("Aggressed", list);
		ListTag regardList = new ListTag();
		for (Map.Entry<UUID, Regard.Entry> entry : regard.entrySet()) {
			CompoundTag r = entry.getValue().save();
			r.putUUID("Player", entry.getKey());
			regardList.add(r);
		}
		npc.put("Regard", regardList);
		ListTag winningList = new ListTag();
		for (ItemStack stack : winnings) winningList.add(stack.save(new CompoundTag()));
		npc.put("Winnings", winningList);
		ListTag sparedList = new ListTag();
		// Its debts lapsed are forgotten (before the sweep of 2026-10-24 they were kept, and saved, for good).
		sparedBy.values().removeIf(until -> until <= level().getGameTime());
		for (Map.Entry<UUID, Long> entry : sparedBy.entrySet()) {
			CompoundTag s = new CompoundTag();
			s.putUUID("Player", entry.getKey());
			s.putLong("Until", entry.getValue());
			sparedList.add(s);
		}
		npc.put("SparedBy", sparedList);
		if (isVacant()) {
			npc.putBoolean("Vacant", true);
			npc.putByte("VacantWas", vacantWas);
		}
		if (settledAt != null) npc.put("SettledAt", NbtUtils.writeBlockPos(settledAt));
		if (wish != null) npc.put("Wish", wish.copy());
		tag.put("Npc", npc);
	}

	// --- A settler's (Settlements, Wishes) ---

	/** A settler's home ground (Settlements): it lives and wanders near it. Null for anyone else. */
	private BlockPos settledAt;
	/** The wish it would have granted (Wishes), as saved; null for none. */
	private CompoundTag wish;

	public BlockPos settledAt() { return settledAt; }
	public void setSettledAt(BlockPos at) { settledAt = at == null ? null : at.immutable(); }
	public CompoundTag wish() { return wish; }
	public void setWish(CompoundTag tag) { wish = tag; }

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
			if (loaded.hasHeavensLeave()) cultivation.setHeavensLeave(true);
			cultivation.setRitualFailedDay(loaded.getRitualFailedDay());
		}
		setAlignment(npc.getInt("Alignment"));
		entityData.set(DATA_TITLE, (byte) NpcTitle.byIndex(npc.getInt("Title")).ordinal());
		entityData.set(DATA_SKIN, (byte) Mth.clamp(npc.getInt("Skin"), 0, SKINS - 1));
		if (npc.contains("Race")) setRace(Bloodline.byId(npc.getString("Race")), npc.getInt("RaceVariant"));
		try {
			lifecycle = Lifecycle.valueOf(npc.getString("Lifecycle"));
		} catch (IllegalArgumentException e) {
			lifecycle = Lifecycle.ROGUE;
		}
		sectId = npc.hasUUID("Sect") ? npc.getUUID("Sect") : null;
		homeIndex = npc.getInt("Home");
		home = npc.contains("HomePos") ? NbtUtils.readBlockPos(npc.getCompound("HomePos")) : null;
		journeyTarget = npc.contains("Journey") ? NbtUtils.readBlockPos(npc.getCompound("Journey")) : null;
		ascended = npc.getBoolean("Ascended");
		joiningSect = npc.hasUUID("JoiningSect") ? npc.getUUID("JoiningSect") : null;
		seclusionMat = npc.contains("SeclusionMat") ? NbtUtils.readBlockPos(npc.getCompound("SeclusionMat")) : null;
		concealment = npc.hasUUID("Concealment") ? npc.getUUID("Concealment") : null;
		setFlag(FLAG_CONCEALED, npc.getBoolean("Concealed"));
		// Missing (saved before it was kept): never seen, not seen at the world's first tick (that catch-up would be the world's age).
		lastSeen = npc.contains("LastSeen") ? npc.getLong("LastSeen") : -1;
		seclusionAttempts = npc.getInt("SeclusionAttempts");
		settledAt = npc.contains("SettledAt") ? NbtUtils.readBlockPos(npc.getCompound("SettledAt")) : null;
		wish = npc.contains("Wish") ? npc.getCompound("Wish").copy() : null;
		aggressed.clear();
		ListTag list = npc.getList("Aggressed", Tag.TAG_INT_ARRAY);
		for (Tag entry : list) aggressed.add(NbtUtils.loadUUID(entry));
		regard.clear();
		ListTag regardList = npc.getList("Regard", Tag.TAG_COMPOUND);
		for (int i = 0; i < regardList.size(); i++) {
			CompoundTag r = regardList.getCompound(i);
			if (r.hasUUID("Player")) regard.put(r.getUUID("Player"), Regard.Entry.load(r));
		}
		winnings.clear();
		ListTag winningList = npc.getList("Winnings", Tag.TAG_COMPOUND);
		for (int i = 0; i < winningList.size(); i++) {
			ItemStack stack = ItemStack.of(winningList.getCompound(i));
			if (!stack.isEmpty()) winnings.add(stack);
		}
		sparedBy.clear();
		ListTag sparedList = npc.getList("SparedBy", Tag.TAG_COMPOUND);
		for (int i = 0; i < sparedList.size(); i++) {
			CompoundTag s = sparedList.getCompound(i);
			if (s.hasUUID("Player")) sparedBy.put(s.getUUID("Player"), s.getLong("Until"));
		}
		setNoGravity(false); // flight isn't saved: it comes back to earth
		if (npc.getBoolean("Vacant")) {
			// Still set aside (its body taken when it was saved): so it stays while a soul holds it, and wakes once none does.
			vacantWas = npc.getByte("VacantWas");
			entityData.set(DATA_VACANT, true);
			setNoGravity(true);
			setInvisible(true);
			noPhysics = true;
		}
		refreshStats();
	}

	// --- Loaded NPCs, for the systems that scan them ---

	/** Every NPC cultivator loaded on the server (kept by ServerEntityEvents in CultivationEvents). */
	private static final Set<CultivatorNpc> LOADED = new HashSet<>();

	public static void onLoad(Entity entity) {
		if (!(entity instanceof CultivatorNpc npc)) return;
		LOADED.add(npc);
		// Seen from the moment it is first in the world, ticking yet or not: one put down in land that unloads before it ever
		// ticks (a hermit at the edge of what's loaded) still catches up on the time away, as NpcLedger reckons it will.
		if (npc.lastSeen < 0) npc.lastSeen = npc.level().getGameTime();
		RogueIndex.note(npc);
		NpcLedger.onLoad(npc);
		// Let go by its sect while its land was unloaded (for a newcomer from below): it sets off the moment it is back, ticking
		// or not, so it's gone (NpcTravel, with nobody near) before anyone could come upon it still in the grounds.
		if (npc.sectId != null && npc.level() instanceof ServerLevel level) {
			Sect sect = SectManager.get(level.getServer(), npc.sectId);
			if (sect != null && !sect.extinct && !sect.members.containsKey(npc.getUUID())) npc.dismiss(level);
		}
	}

	public static void onUnload(Entity entity) {
		if (!(entity instanceof CultivatorNpc npc)) return;
		LOADED.remove(npc);
		RogueIndex.onUnload(npc);
		NpcLedger.onUnload(npc);
		// Its land unloading (not its death or discarding), a member's place is kept: it stays there till it is next loaded.
		if (npc.getRemovalReason() == null || npc.getRemovalReason().shouldSave()) noteWhereItStands(npc);
	}

	/** A sect member's place, kept on its sect's roll (Sect.Member#lastAt). */
	private static void noteWhereItStands(CultivatorNpc npc) {
		if (npc.sectId == null || !(npc.level() instanceof ServerLevel level)) return;
		Sect sect = SectManager.get(level.getServer(), npc.sectId);
		Sect.Member member = sect == null ? null : sect.members.get(npc.getUUID());
		if (member == null) return;
		member.lastAt = npc.blockPosition();
		SectManager.dirty(level.getServer());
	}

	/** On shutting down, before the save (the land unloads only after it): every loaded member's place is kept. */
	public static void noteWhereMembersStand(net.minecraft.server.MinecraftServer server) {
		for (CultivatorNpc npc : LOADED) noteWhereItStands(npc);
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

	/** Loaded NPCs in {@code level}, alive. */
	public static List<CultivatorNpc> loaded(ServerLevel level) {
		List<CultivatorNpc> found = new ArrayList<>();
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() == level && npc.isAlive() && !npc.isRemoved()) found.add(npc);
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

	/** Loaded wandering rogues ({@link #isWanderingRogue}) within {@code radius} of {@code pos}, sideways. */
	public static int wanderingRoguesNear(ServerLevel level, Vec3 pos, double radius) {
		int count = 0;
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() != level || !npc.isAlive() || !npc.isWanderingRogue()) continue;
			double dx = npc.getX() - pos.x, dz = npc.getZ() - pos.z;
			if (dx * dx + dz * dz <= radius * radius) count++;
		}
		return count;
	}

	/** Loaded NPCs on a journey (departing their sect, or going to ascend), ticking or not, for NpcTravel. */
	public static List<CultivatorNpc> travellers() {
		List<CultivatorNpc> found = new ArrayList<>();
		for (CultivatorNpc npc : LOADED) {
			if (npc.isAlive() && !npc.isRemoved() && (npc.lifecycle == Lifecycle.DEPARTING || npc.lifecycle == Lifecycle.ASCENDING)) found.add(npc);
		}
		return found;
	}

	/** Loaded wandering rogues and mortal wanderers in {@code level} (sect members, hermits and travellers not counted). */
	public static int countWanderers(ServerLevel level) {
		int count = 0;
		for (CultivatorNpc npc : LOADED) {
			if (npc.level() == level && npc.isAlive() && (npc.isWanderingRogue() || npc.isWanderingMortal())) count++;
		}
		return count;
	}

	/** True for monsters: what sect disciples clear from their grounds. */
	public static boolean isMonster(LivingEntity entity) {
		return entity instanceof Enemy && !(entity instanceof CultivatorNpc);
	}
}
