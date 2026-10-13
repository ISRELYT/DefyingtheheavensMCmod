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
 * ghost method it can teach ({@link #method}); Ghost Elders (Nascent Soul and above) sell Ghost Jade Slips (CultivatorTrades).
 * Struck down, it scatters in cyan sparks.
 * <p>
 * Three keep the Ghost Gate ({@link Role}): Ox-Head and Horse-Face, Four Axis at Grand Perfection, who greet the dead, warn the
 * living, and fight whoever attacks them (slain, they come back after three days: GhostGate#keepOfficials); and Meng Po by the
 * Naihe Bridge, who doesn't fight, and whose soup washes medicinal toxicity away on the return to life. All three stand three
 * times a man's height ({@link #KEEPER_SCALE}).
 */
public class GhostCultivatorNpc extends CultivatorNpc implements Ghostly {
	/** Ox-Head and Horse-Face greet or warn a player this close, once in this long (ticks). */
	public static final double OFFICIAL_SPEAKS_WITHIN = 12;
	private static final long OFFICIAL_SPEAKS_EVERY = 5 * 60 * 20;
	/** An official this far from its post walks back to it. */
	private static final double POST_LEASH = 3;
	/** The ways the dead tell how they died (Ask, about themselves): one picked from each ghost's UUID, so it never changes. */
	public static final String[] DEATHS = {"tribulation", "deviation", "sect_war", "demonic_palm", "old_age", "poison", "betrayal",
			"beast", "duel", "breakthrough", "starvation", "plague", "drowning", "assassin", "ascension"};

	/** Which of the Gate's keepers it is, if any. */
	public enum Role {
		NONE("none"), OX_HEAD("ox_head"), HORSE_FACE("horse_face"), MENG_PO("meng_po");

		private final String id;

		Role(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		/** The two who keep the Gate and fight for it. */
		public boolean isWarden() { return this == OX_HEAD || this == HORSE_FACE; }

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
	/** When it last greeted or warned each player (an official). Not saved. */
	private final Map<UUID, Long> spokenTo = new HashMap<>();

	public GhostCultivatorNpc(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		cultivation().setHeavensLeave(true); // asked of the heavens in life: the dead aren't held at Qi Refining Mid's gate
	}

	@Override
	public boolean isGhost() { return true; }

	/** The Gate's keepers stand at their posts. */
	@Override
	public boolean keepsPost() { return getRole() != Role.NONE; }

	/** Nor are they the land's rogues, whom the spawner keeps apart (NpcSpawner, RogueIndex). */
	@Override
	public boolean isWanderingRogue() { return getRole() == Role.NONE && super.isWanderingRogue(); }

	/** Which of the Gate's keepers it is, if any (synced, for its look: GhostlyRenderers). */
	public Role getRole() {
		int index = entityData.get(DATA_ROLE);
		return index >= 0 && index < Role.values().length ? Role.values()[index] : Role.NONE;
	}

	private void setRole(Role role) { entityData.set(DATA_ROLE, (byte) role.ordinal()); }

	/** The Gate's keepers (Ox-Head, Horse-Face, Meng Po) stand this many times a man's height, and as broad. */
	public static final float KEEPER_SCALE = 3.0f;

	/**
	 * A keeper's size: its body, hitbox and eyes scaled by {@link #KEEPER_SCALE} (LivingEntity#getDimensions goes by this, and
	 * the dead's light is drawn by it, SoulRenderer); any other ghost a man's.
	 */
	@Override
	public float getScale() {
		return getRole() == Role.NONE ? super.getScale() : KEEPER_SCALE;
	}

	/** A keeper's stride takes a block's step (or a little more) as a man takes a slab's. */
	@Override
	public float maxUpStep() {
		return getRole() == Role.NONE ? super.maxUpStep() : 1.5f;
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

	/** A ghost of {@code rank} (never a mortal), on {@code alignment}'s path, in one of the skins; titled by its strength. */
	public void becomeGhost(int rank, int alignment, RandomSource random) {
		becomeRogue(Math.max(0, rank), alignment, random);
		setTitle(titleFor(getRank()));
	}

	/** A Wandering Ghost, or from Nascent Soul a Ghost Elder. */
	public static NpcTitle titleFor(int rank) {
		return rank >= PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY) ? NpcTitle.GHOST_ELDER : NpcTitle.WANDERING_GHOST;
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
		if (role != Role.NONE) setCustomName(Component.translatable(ModLang.officialName(role.getId())));
		setPersistenceRequired();
	}

	/** Its ghost method: from Foundation Building on, one of the three, by its UUID and the methods' rarity; null below. */
	public GhostMethod method() {
		if (getRank() < PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY)) return null;
		int total = 0;
		for (GhostMethod m : GhostMethod.values()) total += m.steleWeight();
		int roll = Math.floorMod(getUUID().hashCode() * 31 + 7, total);
		for (GhostMethod m : GhostMethod.values()) {
			roll -= m.steleWeight();
			if (roll < 0) return m;
		}
		return GhostMethod.NETHERWORLD_BREATHING;
	}

	/** How it died, as a key under {@code npc.death.} (see {@link #DEATHS}); Meng Po and the wardens have their own. */
	public String deathKey() {
		Role role = getRole();
		if (role != Role.NONE) return role.getId();
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
			becomeGhost(NpcSpawner.rollRank(random, true, true), Alignment.randomRogue(random), random);
		}
		return result;
	}

	// --- Its keepers' duties ---

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (getRole() == Role.NONE || !(level() instanceof ServerLevel level)) return;
		if (post != null && getTarget() == null && !isInCombat() && talkPartner() == null && tickCount % 20 == 0
				&& distanceToSqr(Vec3.atBottomCenterOf(post)) > POST_LEASH * POST_LEASH) {
			getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 0.8);
		}
		if (tickCount % 20 == 0) speakToComers(level);
	}

	/** Ox-Head and Horse-Face greet the dead as they come, and warn the living off; Meng Po calls to the dead to drink. */
	private void speakToComers(ServerLevel level) {
		long now = level.getGameTime();
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || distanceToSqr(player) > OFFICIAL_SPEAKS_WITHIN * OFFICIAL_SPEAKS_WITHIN) continue;
			Long last = spokenTo.get(player.getUUID());
			if (last != null && now - last < OFFICIAL_SPEAKS_EVERY) continue;
			boolean dead = GhostSystem.isGhost(player);
			String key = getRole().isWarden() ? (dead ? "warden.greet" : "warden.warn") : dead ? "meng_po.call" : null;
			if (key == null) continue;
			spokenTo.put(player.getUUID(), now);
			getLookControl().setLookAt(player, 30, 30);
			NpcSpeech.say(this, NpcLines.line(key, getRandom(), player.getGameProfile().getName()));
			return;
		}
	}

	// --- Struck as the dead are ---

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (level().isClientSide) return super.hurt(source, amount);
		if (!GhostSystem.reaches(source)) return false;
		return super.hurt(source, GhostSystem.scaled(source, amount));
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
		}
	}

	/** Dispersed, it leaves no body to fall: what it carried falls (its loot table and spirit stones), no dying pose. */
	@Override
	protected void tickDeath() {
		if (!level().isClientSide && !isRemoved()) remove(RemovalReason.KILLED);
	}

	/** The officials don't stray into fights that aren't theirs; Meng Po fights nobody. */
	@Override
	public boolean isHostileTo(LivingEntity other) {
		if (getRole() == Role.MENG_PO) return false;
		if (getRole().isWarden()) return other instanceof Player player && player.isAlive() && getLastHurtByMob() == player;
		return super.isHostileTo(other);
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
