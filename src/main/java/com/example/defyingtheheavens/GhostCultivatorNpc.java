package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A ghost cultivator (鬼修): one of the Netherworld's dead who cultivates on, the Netherworld's rogues. A {@link CultivatorNpc} in
 * every way that matters (rank, path, regard, talk, trade, spars, daily life), drawn as cyan light in its cultivator skin
 * (SoulRenderer) and struck by the rules for the dead (GhostSystem#reaches: arrows, lava and the like pass through it; the
 * living land blows by how clearly they see it; ghost fire, a Peach Wood Sword and the dead land in full).
 * <p>
 * It turns up in the Netherworld instead of living rogues (NpcSpawner), at the high realms' strengths, Qi Refining Early to
 * Four Axis Grand Perfection, never a mortal; it joins no sect, keeps no ledger, makes no journeys. It speaks in the ghost
 * voice (NpcLines), tells how it died when asked about itself ({@link #deathKey}), and from Foundation Building on knows a
 * ghost method it can teach ({@link #method}); Ghost Elders (Foundation Building and above) sell Ghost Jade Slips
 * (CultivatorTrades). Struck down, it scatters in cyan sparks.
 * <p>
 * Three keep the Ghost Gate ({@link Role}): Ox-Head and Horse-Face, Four Axis at Grand Perfection, who greet the dead, warn the
 * living, and fight whoever attacks them (slain, they come back after three days: GhostGate#keepOfficials); and Meng Po by the
 * Naihe Bridge, who doesn't fight, and whose soup washes medicinal toxicity away on the return to life. All three stand three
 * times a man's height ({@link #KEEPER_SCALE}).
 * <p>
 * From Core Formation one of the dead is a <b>Ghost King</b>, half again a man's height ({@link #KING_SCALE}), crowned: the
 * Netherworld's lords, each on the throne of its palace (GhostPalacePiece) among the Ghost Retainers of its court, greeting the
 * dead and warning off the living. The wanderers the land spawns are kept below it; one that cultivates up to it, or comes up
 * the Soul River from the Nether a Core Formation cultivator, crowns itself and goes to rule the nearest palace whose throne
 * stands empty ({@link #rise}, GhostPalaces). Strike the king or any of its court and the whole court fights you.
 */
public class GhostCultivatorNpc extends CultivatorNpc implements Ghostly {
	/** Ox-Head and Horse-Face greet or warn a player this close, once in this long (ticks). */
	public static final double OFFICIAL_SPEAKS_WITHIN = 12;
	private static final long OFFICIAL_SPEAKS_EVERY = 5 * 60 * 20;
	/** An official this far from its post walks back to it. */
	private static final double POST_LEASH = 3;
	/** A keeper of the Gate on its way back to its post that gets no nearer for this long (ticks) is set down there again. */
	private static final int KEEPER_STRAY_TICKS = 200;
	/** The ways the dead tell how they died (Ask, about themselves): one picked from each ghost's UUID, so it never changes. */
	public static final String[] DEATHS = {"tribulation", "deviation", "sect_war", "devilish_palm", "old_age", "poison", "betrayal",
			"beast", "duel", "breakthrough", "starvation", "plague", "drowning", "assassin", "ascension"};

	/** Which of the Gate's keepers it is, if any; or a Ghost King, or a retainer of a king's court (appended: synced by ordinal). */
	public enum Role {
		NONE("none"), OX_HEAD("ox_head"), HORSE_FACE("horse_face"), MENG_PO("meng_po"), GHOST_KING("ghost_king"), COURTIER("courtier");

		private final String id;

		Role(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		/** The two who keep the Gate and fight for it. */
		public boolean isWarden() { return this == OX_HEAD || this == HORSE_FACE; }

		/** The three who keep the Ghost Gate (GhostGate#keepOfficials). */
		public boolean isGateKeeper() { return isWarden() || this == MENG_PO; }

		/** A ghost palace's king and its retainers, who fight as one. */
		public boolean isCourt() { return this == GHOST_KING || this == COURTIER; }

		/** The beast's head the keeper wears in place of a man's (see BeastHead), or null. */
		public BeastHead beastHead() {
			return switch (this) {
				case OX_HEAD -> BeastHead.OX;
				case HORSE_FACE -> BeastHead.HORSE;
				default -> null;
			};
		}
	}

	private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> DATA_ROLE = net.minecraft.network.syncher.SynchedEntityData.defineId(
			GhostCultivatorNpc.class, net.minecraft.network.syncher.EntityDataSerializers.BYTE);
	private BlockPos post;
	/** A keeper on its way back to its post: the nearest it has come, and for how long it has come no nearer. Not saved. */
	private double strayNearest = Double.MAX_VALUE;
	private int strayTicks;
	/** When it last greeted or warned each player (an official). Not saved. */
	private final Map<UUID, Long> spokenTo = new HashMap<>();

	public GhostCultivatorNpc(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		cultivation().setHeavensLeave(true); // asked of the heavens in life: the dead aren't held at Qi Refining Mid's gate
	}

	@Override
	public boolean isGhost() { return true; }

	/** The Gate's keepers stand at their posts, as a king at its throne and its retainers at theirs (a king with no palace roams). */
	@Override
	public boolean keepsPost() { return getRole().isGateKeeper() || post != null; }

	/** Nor are they the land's rogues, whom the spawner keeps apart (NpcSpawner, RogueIndex). */
	@Override
	public boolean isWanderingRogue() { return getRole() == Role.NONE && super.isWanderingRogue(); }

	/** How far apart two of a court's posts can be and still be one palace's (it is 41 blocks across). */
	private static final double COURT_REACH = 48;

	/**
	 * A court is one: its king and its retainers (their posts in the same palace) are allies, as a sect's members are, and never
	 * turn on one another whatever paths they walk.
	 */
	@Override
	public boolean isAlly(Entity other) {
		if (super.isAlly(other)) return true;
		return getRole().isCourt() && post != null && other instanceof GhostCultivatorNpc ghost && ghost.getRole().isCourt()
				&& ghost.post != null && ghost.post.closerThan(post, COURT_REACH);
	}

	/** Which of the Gate's keepers it is, if any (synced, for its look: GhostlyRenderers). */
	public Role getRole() {
		int index = entityData.get(DATA_ROLE);
		return index >= 0 && index < Role.values().length ? Role.values()[index] : Role.NONE;
	}

	private void setRole(Role role) { entityData.set(DATA_ROLE, (byte) role.ordinal()); }

	/** The Gate's keepers (Ox-Head, Horse-Face, Meng Po) stand this many times a man's height, and as broad. */
	public static final float KEEPER_SCALE = 3.0f;
	/** A Ghost King stands half again a man's height. */
	public static final float KING_SCALE = 1.5f;

	/**
	 * A keeper's size: its body, hitbox and eyes scaled by {@link #KEEPER_SCALE} (LivingEntity#getDimensions goes by this, and
	 * the dead's light is drawn by it, SoulRenderer); a Ghost King's by {@link #KING_SCALE}; any other ghost a man's.
	 */
	@Override
	public float getScale() {
		Role role = getRole();
		return role.isGateKeeper() ? KEEPER_SCALE : role == Role.GHOST_KING ? KING_SCALE : super.getScale();
	}

	/** A keeper's stride takes a block's step (or a little more) as a man takes a slab's; a king's, a block's. */
	@Override
	public float maxUpStep() {
		Role role = getRole();
		return role.isGateKeeper() ? 1.5f : role == Role.GHOST_KING ? 1.0f : super.maxUpStep();
	}

	/** Its size follows its role, on both sides (the role is synced). */
	@Override
	public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_ROLE.equals(key)) refreshDimensions();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_ROLE, (byte) 0);
	}

	/**
	 * A ghost of {@code rank} (never a mortal), on {@code alignment}'s path, in one of the skins; titled by its strength, and
	 * from Core Formation a Ghost King (one with no palace yet: {@link #rise} finds it a throne).
	 */
	public void becomeGhost(int rank, int alignment, RandomSource random) {
		becomeRogue(Math.max(0, rank), alignment, random);
		if (getRank() >= GhostSystem.KING_RANK) {
			setRole(Role.GHOST_KING);
			setPersistenceRequired();
		}
		setTitle(titleFor(getRank()));
	}

	/** A Wandering Ghost; from Foundation Building a Ghost Elder; from Core Formation a Ghost King. */
	public static NpcTitle titleFor(int rank) {
		if (rank >= GhostSystem.KING_RANK) return NpcTitle.GHOST_KING;
		return rank >= PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY) ? NpcTitle.GHOST_ELDER : NpcTitle.WANDERING_GHOST;
	}

	/** The strongest the land's wandering ghosts come (NpcSpawner, spawn eggs): just short of a Ghost King, whom palaces keep. */
	public static final int WANDERER_CAP = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);

	/** A Ghost King of {@code rank} on {@code alignment}'s path, on the throne at {@code throne} (a ghost palace's, GhostPalacePiece). */
	public void becomeKing(int rank, int alignment, BlockPos throne, RandomSource random) {
		becomeRogue(Math.max(GhostSystem.KING_RANK, rank), alignment, random);
		setRole(Role.GHOST_KING);
		this.post = throne;
		setTitle(NpcTitle.GHOST_KING);
		setPersistenceRequired();
	}

	/** A Ghost Retainer of {@code rank} in a king's court, on its king's path ({@code alignment}), standing at {@code post}. */
	public void becomeCourtier(int rank, int alignment, BlockPos post, RandomSource random) {
		becomeRogue(Math.max(0, Math.min(WANDERER_CAP, rank)), alignment, random);
		setRole(Role.COURTIER);
		this.post = post;
		setTitle(NpcTitle.GHOST_RETAINER);
		setPersistenceRequired();
	}

	/**
	 * {@code living}, come up the Soul River from the Nether at its ascension (CultivatorNpc#attemptAscension, NpcTravel): one of
	 * the dead now, the same soul (the same id, so its name and face carry over, NpcNames), on its path, at its strength.
	 */
	public static GhostCultivatorNpc shedBody(CultivatorNpc living, ServerLevel netherworld) {
		GhostCultivatorNpc ghost = ModEntities.GHOST_CULTIVATOR.create(netherworld);
		if (ghost == null) return null;
		ghost.setUUID(living.getUUID());
		ghost.becomeGhost(Math.max(0, living.getRank()), living.getAlignment(), netherworld.getRandom());
		ghost.setSkin(living.getSkin());
		ghost.setRace(living.getBloodline(), living.getRaceVariant()); // a demon's soul keeps its race's form
		if (living.hasCustomName()) ghost.setCustomName(living.getCustomName());
		return ghost;
	}

	/** Sets {@code ghost} down on the Ghost Gate's plaza, where the dead wake (or there when the Gate's land is next loaded). */
	public static void arriveAtGate(GhostCultivatorNpc ghost, ServerLevel netherworld) {
		BlockPos at = GhostGate.at(netherworld, GhostGate.ARRIVAL);
		RandomSource random = netherworld.getRandom();
		BlockPos spot = at.offset(random.nextInt(7) - 3, 0, random.nextInt(5) - 2);
		if (!netherworld.isPositionEntityTicking(spot)) {
			NpcTravel.sendTo(ghost, netherworld, spot);
			return;
		}
		ghost.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
		netherworld.addFreshEntity(ghost);
		netherworld.sendParticles(ParticleTypes.SOUL, ghost.getX(), ghost.getY() + 1, ghost.getZ(), 20, 0.4, 0.8, 0.4, 0.04);
	}

	/** Makes it one of the Gate's keepers, standing at {@code post}. */
	public void becomeOfficial(Role role, BlockPos post, RandomSource random) {
		setRole(role);
		this.post = post;
		switch (role) {
			case OX_HEAD, HORSE_FACE -> {
				becomeRogue(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION), 0, random);
				setTitle(NpcTitle.GATE_WARDEN);
			}
			case MENG_PO -> {
				becomeRogue(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.LATE), 300, random);
				setTitle(NpcTitle.BRIDGE_KEEPER);
				setInvulnerable(true); // she doesn't fight, and nobody harms her
			}
			default -> {}
		}
		if (role.isGateKeeper()) setCustomName(Component.translatable(ModLang.officialName(role.getId())));
		setPersistenceRequired();
	}

	/** Where it keeps its post (one of the Gate's keepers), or null. */
	public BlockPos keeperPost() { return post; }

	/** Its post moved (GhostGate#keepOfficials, as Meng Po's pavilion became her hall): it keeps the new one. */
	public void moveKeeperPost(BlockPos post) { this.post = post; }

	/** In ten ghosts of Foundation Building or above, how many know their own realm's foundation (a Ghost King always does). */
	public static final int OWN_FOUNDATION_IN_TEN = 5;

	/**
	 * Its ghost method, the one it teaches and sells (picked from its UUID, so it never changes): null below Foundation Building;
	 * a Ghost King its realm's foundation (the Ghost Core Canon, a Nascent Soul king the Nine Nether Soul Sutra); any other ghost
	 * its realm's foundation five times in ten, else one of the first three (the Breathing Art, the fire, the steps) by how often
	 * the dead know them (GhostMethod#knownWeight: as their steles once stood, 600 / 300 / 200).
	 */
	public GhostMethod method() {
		if (getRank() < PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY)) return null;
		GhostMethod own = GhostMethod.foundationOf(Realm.byIndex(Math.min(Realm.LAST_TAUGHT.ordinal(), getRank() / Stage.values().length)));
		int hash = getUUID().hashCode() * 31 + 7;
		if (own != null && (getRole() == Role.GHOST_KING || Math.floorMod(hash, 10) < OWN_FOUNDATION_IN_TEN)) return own;
		int total = 0;
		for (GhostMethod m : GhostMethod.values()) if (m.tier() <= 1) total += m.knownWeight();
		int roll = Math.floorMod(hash / 10, total);
		for (GhostMethod m : GhostMethod.values()) {
			if (m.tier() > 1) continue;
			roll -= m.knownWeight();
			if (roll < 0) return m;
		}
		return GhostMethod.NETHERWORLD_BREATHING;
	}

	/** One of the dead's own foundations past the Breathing Art falls when it is struck down this often (a king's, always). */
	public static final float FOUNDATION_SLIP_CHANCE = 0.15f;

	/** Struck down, it may leave the slip of the foundation it knew (its loot table holds only the first three methods'). */
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		super.dropCustomDeathLoot(source, looting, recentlyHit);
		GhostMethod method = method();
		// Never True Immortal's foundation, the Immortal Realm's (GhostMethod#taughtInTheWorld).
		if (method == null || method.tier() <= 1 || !method.taughtInTheWorld()) return;
		if (getRole() == Role.GHOST_KING || random.nextFloat() < FOUNDATION_SLIP_CHANCE) spawnAtLocation(GhostJadeSlipItem.of(method));
	}

	/** How it died, as a key under {@code npc.death.} (see {@link #DEATHS}); Meng Po and the wardens have their own. */
	public String deathKey() {
		Role role = getRole();
		if (role.isGateKeeper()) return role.getId();
		return DEATHS[(int) Math.floorMod(getUUID().getLeastSignificantBits(), (long) DEATHS.length)];
	}

	// --- No clothes, no weapon: the dead are light, not cloth ---

	@Override
	public void equip(RandomSource random) {}

	/** Spawned by an egg or a command: a ghost of the Netherworld's usual strength. */
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data, CompoundTag tag) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data, tag);
		if (tag == null || !tag.contains("Npc")) {
			RandomSource random = level.getRandom();
			becomeGhost(Math.min(WANDERER_CAP, NpcSpawner.rollRank(random, true, true)), Alignment.randomRogue(random), random);
		}
		return result;
	}

	// --- Its keepers' duties ---

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!(level() instanceof ServerLevel level)) return;
		if (tickCount % 100 == 0) rise(level);
		if (getRole() == Role.NONE) return;
		if (post != null && getTarget() == null && !isInCombat() && talkPartner() == null && tickCount % 20 == 0
				&& distanceToSqr(Vec3.atBottomCenterOf(post)) > POST_LEASH * POST_LEASH) {
			// Far off (a risen king going to its new throne), a step at a time; near, straight there.
			double away = Math.sqrt(distanceToSqr(Vec3.atBottomCenterOf(post)));
			if (away > 24) CultivatorGoals.walkToward(this, Vec3.atBottomCenterOf(post), 0.9);
			else getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0.8);
			// A keeper of the Gate that gets no nearer for a while (no way back it can find) is set down at its post again.
			if (getRole().isGateKeeper()) {
				if (away < strayNearest - 0.5) {
					strayNearest = away;
					strayTicks = 0;
				} else if ((strayTicks += 20) >= KEEPER_STRAY_TICKS) {
					GhostGate.returnToPost(this);
					strayNearest = Double.MAX_VALUE;
					strayTicks = 0;
				}
			}
		} else if (tickCount % 20 == 0) {
			strayNearest = Double.MAX_VALUE;
			strayTicks = 0;
		}
		if (tickCount % 20 == 0) speakToComers(level);
		// A king on its throne is noted as its palace's (worldgen, which built the palace and set it there, keeps no records).
		if (getRole() == Role.GHOST_KING && post != null && tickCount % 200 == 0 && distanceToSqr(Vec3.atBottomCenterOf(post)) < 9) {
			GhostPalaces.note(level, post, getUUID());
		}
	}

	/**
	 * A ghost grown to Core Formation is a Ghost King: crowned (half again a man's height, the Ghost King's title), it goes to rule
	 * the nearest palace whose throne stands empty (GhostPalaces), or roams the Netherworld a king without a court.
	 */
	private void rise(ServerLevel level) {
		if (getRole() == Role.NONE && getRank() >= GhostSystem.KING_RANK) {
			setRole(Role.GHOST_KING);
			setTitle(NpcTitle.GHOST_KING);
			setPersistenceRequired();
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.5, getZ(), 50, 0.5, 1.0, 0.5, 0.06);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1.0f, 0.5f);
		}
		if (getRole() == Role.GHOST_KING && post == null) post = GhostPalaces.claimVacant(level, blockPosition(), getUUID());
	}

	/**
	 * Ox-Head and Horse-Face greet the dead as they come, and warn the living off; Meng Po calls to the dead to drink; a Ghost King
	 * on its throne bids the dead kneel and warns the living out of its palace.
	 */
	private void speakToComers(ServerLevel level) {
		long now = level.getGameTime();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || distanceToSqr(player) > OFFICIAL_SPEAKS_WITHIN * OFFICIAL_SPEAKS_WITHIN) continue;
			Long last = spokenTo.get(player.getUUID());
			if (last != null && now - last < OFFICIAL_SPEAKS_EVERY) continue;
			boolean dead = GhostSystem.isGhost(player);
			Role role = getRole();
			String key = role.isWarden() ? (dead ? "warden.greet" : "warden.warn") : role == Role.MENG_PO ? (dead ? "meng_po.call" : null)
					: role == Role.GHOST_KING && post != null ? (dead ? "king.greet" : "king.warn") : null;
			if (key == null) continue;
			spokenTo.put(player.getUUID(), now);
			getLookControl().setLookAt(player, 30, 30);
			NpcSpeech.say(this, NpcLines.line(key, getRandom(), player.getGameProfile().getName()));
			return;
		}
	}

	// --- Struck as the dead are ---

	/**
	 * It sees the dead (and the living in a Ghost King's form) as anyone else, to fight them or strike back at them, whom the
	 * living world's mobs and NPCs don't (GhostSystem#deadMayAttack).
	 */
	@Override
	public boolean canAttack(LivingEntity target) {
		Boolean dead = GhostSystem.deadMayAttack(this, target);
		return dead != null ? dead : super.canAttack(target);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide) return super.hurt(source, amount);
		if (!GhostSystem.reaches(source)) return false;
		if (source.getEntity() != null && source.getEntity() != this && isAlly(source.getEntity())) return false; // no blow of its court's lands on it
		boolean hurt = super.hurt(source, GhostSystem.scaled(source, amount));
		if (hurt && getRole().isCourt() && source.getEntity() instanceof LivingEntity attacker && attacker != this
				&& level() instanceof ServerLevel level) {
			rallyCourt(level, attacker);
		}
		return hurt;
	}

	/** A court's reach: the king and its retainers this near one struck turn on the one who struck it. */
	public static final double COURT_RALLY = 40;

	/** Strike a Ghost King or any of its court, and the whole court (all within {@link #COURT_RALLY}) fights you. */
	private void rallyCourt(ServerLevel level, LivingEntity attacker) {
		for (GhostCultivatorNpc ally : level.getEntitiesOfClass(GhostCultivatorNpc.class, getBoundingBox().inflate(COURT_RALLY),
				ghost -> ghost.isAlive() && (ghost == this || isAlly(ghost)))) {
			ally.setLastHurtByMob(attacker);
			if (ally.getTarget() == null) ally.setTarget(attacker);
		}
	}

	@Override
	public boolean isSensitiveToWater() { return false; }

	/** It doesn't burn: fire and lava pass through it (GhostSystem#reaches), and nothing is shown burning. */
	@Override
	public boolean fireImmune() { return true; }

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

	/** The dead tread without a sound: no footsteps, no splashing (a player's ghost likewise, SoulPlayerMixin). */
	@Override
	protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1, getZ(), 60, 0.4, 0.8, 0.4, 0.08);
			level.sendParticles(ParticleTypes.GLOW, getX(), getY() + 1, getZ(), 30, 0.5, 0.9, 0.5, 0.15);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.ALLAY_DEATH, SoundSource.HOSTILE, 1.0f, 0.5f);
			if (getRole().isWarden()) GhostGate.onOfficialSlain(level, getRole());
			if (getRole() == Role.GHOST_KING && post != null) GhostPalaces.vacate(level, post); // the throne stands empty
		}
	}

	/** Dispersed, it leaves no body to fall: what it carried falls (its loot table and spirit stones), no dying pose. */
	@Override
	protected void tickDeath() {
		if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
	}

	/** The officials, and a king's court, don't stray into fights that aren't theirs; Meng Po fights nobody. */
	@Override
	public boolean isHostileTo(LivingEntity other) {
		if (getRole() == Role.MENG_PO) return false;
		if (getRole().isWarden() || getRole().isCourt()) return other instanceof Player player && player.isAlive() && getLastHurtByMob() == player;
		return super.isHostileTo(other);
	}

	/**
	 * Nor do the Gate's keepers seek out foes by their path (CultivatorGoals.HostilityGoal), which drew them off their posts after
	 * any cultivator of an opposed path who came by, and Meng Po to strike the living: Meng Po is no one's foe, and the wardens
	 * fight whoever strikes them.
	 */
	@Override
	public boolean isEnemy(LivingEntity other) {
		if (getRole() == Role.MENG_PO) return false;
		if (getRole().isWarden()) return other.isAlive() && getLastHurtByMob() == other;
		return super.isEnemy(other);
	}

	/** The Gate's keepers keep it: no stronger domain sends them running, nor a beating (a slain warden comes back: GhostGate). */
	@Override
	public boolean holdsGround() {
		return getRole().isGateKeeper() || super.holdsGround();
	}

	@Override
	public boolean makesLastStand() {
		return getRole().isGateKeeper() || super.makesLastStand();
	}

	// --- Saving ---

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		CompoundTag ghost = new CompoundTag();
		ghost.putString("Role", getRole().getId());
		if (post != null) ghost.put("Post", NbtUtils.writeBlockPos(post));
		tag.put("Ghost", ghost);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		cultivation().setHeavensLeave(true);
		CompoundTag ghost = tag.getCompound("Ghost");
		Role role = Role.NONE;
		for (Role r : Role.values()) if (r.getId().equals(ghost.getString("Role"))) role = r;
		setRole(role);
		post = ghost.contains("Post") ? NbtUtils.readBlockPos(ghost.getCompound("Post")) : null;
		if (role == Role.MENG_PO) setInvulnerable(true);
	}
}
