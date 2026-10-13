package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Soul Projection, awakened at Nascent Soul: the cultivator sends their nascent soul out of the body.
 * <ul>
 *   <li><b>Going out and coming back</b> each take {@link #CHANNEL_TICKS} (5 s). Going out, the cultivator sits still: moving or
 *   being hurt breaks it off. Coming back, the soul is drawn home along its tether.</li>
 *   <li><b>The body</b> stays behind, sitting in meditation ({@link SoulBodyEntity}) with the player's health and armor. Harm to it
 *   is reported to the soul; if it is slain, the soul dies with it.</li>
 *   <li><b>The soul</b> is the player itself: it flies free of gravity through solid blocks (the client's free flight) within its
 *   tether ({@link PlayerCultivation#soulTetherRadius}), on {@link PlayerCultivation#soulHearts} hearts that never heal. It
 *   touches nothing: no blocks, items, chests or meditation; no plates or tripwires; no portals or rifts. Its only act is to
 *   possess. Qi Sense is forced open (it is how the soul sees), and its upkeep ({@link PlayerCultivation#soulCostPerSecond})
 *   grows with the distance from the body; out of qi, the soul snaps back at once.</li>
 *   <li><b>Who sees it</b> ({@link #perceives}): Nascent Soul cultivators with Qi Sense on (hazy at Early, clearer each stage,
 *   whole at Grand Perfection) and Heavenly Beings and above, whose clients are always sent it (to strike at) but draw it only
 *   under Qi Sense. Nobody else's client is even told it is there. A Core Formation Grand Perfection domain senses one without
 *   seeing it. Only those who see it can harm it, Nascent Soul cultivators for less ({@link #harmFactor}); a Heavenly Being with
 *   Qi Sense on can also strike its tether, and cut it.</li>
 *   <li><b>Losing the soul</b>: a cut tether, a slain body, a destroyed soul, or logging out within {@link #COMBAT_MEMORY_TICKS}
 *   of harm to either kills the cultivator and costs the nascent soul itself ({@link PlayerCultivation#loseNascentSoul}).</li>
 *   <li><b>Possession</b> ({@link #tryPossess}): a being at least {@link #POSSESS_REALM_GAP} realms below the soul (beasts and
 *   monsters count as mortals) is taken over: the soul controls it with its own body's strength and pace (a flier flies where the
 *   soul looks, {@link #flightWish}) until it lets go (sneak), its qi
 *   runs out, it is recalled, or the body dies (the soul is thrown out unharmed, and can then be harmed). A player is not
 *   controlled: their body is seized ({@link SoulVesselEntity}), and they die.</li>
 * </ul>
 * Nothing about it may be used to dodge a tribulation: a soul can't start one, a cultivator mid-tribulation can't send one out.
 */
public final class SoulProjection {
	public enum State {
		NONE, PROJECTING, PROJECTED, RECALLING;

		public static State byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : NONE;
		}
	}

	/** Going out, and coming back: 5 s each. */
	public static final int CHANNEL_TICKS = 100;
	/** The soul can't set out with less than this share of the qi pool. */
	public static final double MIN_QI_FRACTION = 0.1;
	/** A logout within this long of harm to the soul or the body is fleeing, and severs the soul (ticks). */
	public static final int COMBAT_MEMORY_TICKS = 200;
	/** How near a being must be for the soul to enter it, and what entering costs (share of the pool). */
	public static final double POSSESS_REACH = 4.0;
	public static final double POSSESS_QI_FRACTION = 0.1;
	/** A possessed being must stand at least this many realms below the soul (a mortal or a beast counts as below Qi Refining). */
	public static final int POSSESS_REALM_GAP = 3;
	/** The tether's reach by stage from Nascent Soul Early to Four Axis Grand Perfection. */
	public static final int[] TETHER_RADII = {32, 48, 64, 96, 128, 144, 160, 176, 192, 208, 224, 256};
	/**
	 * Share of a blow a Nascent Soul cultivator lands on a soul, by stage: to them it is a haze, clearer each stage. Heavenly Beings
	 * and above see it whole and strike it fully.
	 */
	private static final float[] NASCENT_SOUL_HARM = {0.25f, 0.4f, 0.55f, 0.7f};
	/** The tether holds this much against blows, plus the soul's own health; it mends if left alone for {@link #TETHER_MEND_TICKS}. */
	private static final double TETHER_BASE_STRENGTH = 20;
	private static final int TETHER_MEND_TICKS = 200;
	/** A Heavenly Being's strike reaches a tether this far from their eyes, and hits it within this of the line. */
	public static final double TETHER_STRIKE_REACH = 5.0;
	public static final double TETHER_STRIKE_WIDTH = 0.6;
	/** Going out is broken off by moving this far (squared, horizontally). */
	private static final double CHANNEL_DRIFT_SQR = 0.3 * 0.3;
	/** A soul found this far past the end of its tether is pulled back (the client keeps it inside; this catches the rest). */
	private static final double TETHER_SLACK = 2;
	/** Hostile mobs this close to the body notice it. */
	private static final double BODY_NOTICE_RANGE = 16;
	/** A body hurt is reported at most this often (ticks). */
	private static final int BODY_ALERT_INTERVAL = 40;
	/** How often the soul's own client, and those who see it, are told where things stand (ticks). */
	private static final int SELF_SYNC_INTERVAL = 5;
	private static final int SEEN_SYNC_INTERVAL = 10;
	/** How far those who see a soul are told about it. */
	private static final double SEEN_RANGE = 160;
	/** A body missing this long (a command removed it) counts as slain. */
	private static final int BODY_GRACE_TICKS = 40;
	private static final UUID SOUL_HEALTH_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4c01");
	private static final int BODY_TICKET_RADIUS = 2;
	private static final TicketType<ChunkPos> BODY_TICKET = TicketType.create("defying-the-heavens:soul_body",
			Comparator.comparingLong(ChunkPos::toLong));
	/** The soul's violet, for the particles of going out and coming back. */
	private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.72f, 0.36f, 1.0f), 1.3f);

	/**
	 * Client hooks (set by the client): whether a player is a projected soul on this client, and whether it is gathering to go
	 * out. Common code that runs on both sides (movement, collisions, riding) asks {@link #isSoul} and {@link #isChanneling}.
	 */
	public static Predicate<Player> clientSoul = player -> false;
	public static Predicate<Player> clientChanneling = player -> false;

	private static final class Session {
		State state = State.PROJECTING;
		int ticks = CHANNEL_TICKS;
		/** Where going out began: the body is left here. */
		Vec3 anchor;
		UUID body;
		ResourceKey<Level> dimension;
		Vec3 bodyPos = Vec3.ZERO;
		int bodyMissing;
		/** The being the soul is in, or null. */
		UUID possessed;
		/** Possession made its flier weightless (it wasn't): it gets its weight back when the soul leaves it. */
		boolean weightless;
		/** Game time of the last harm to the soul or the body. */
		long lastHurt = Long.MIN_VALUE / 2;
		double tether;
		long tetherStruck = Long.MIN_VALUE / 2;
		long bodyAlerted = Long.MIN_VALUE / 2;
		/** Domains (players) that have sensed this soul and were told, so each is told once while it stays. */
		final Set<UUID> sensedBy = new HashSet<>();
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	// --- Queries (both sides where noted) ---

	/** The player is a projected soul (out of the body, possessing or coming back). Both sides. */
	public static boolean isSoul(Player player) {
		if (player == null) return false;
		if (player.level().isClientSide) return clientSoul.test(player);
		Session s = SESSIONS.get(player.getUUID());
		return s != null && (s.state == State.PROJECTED || s.state == State.RECALLING);
	}

	/** The player is sitting still while their soul gathers to go out. Both sides. */
	public static boolean isChanneling(Player player) {
		if (player == null) return false;
		if (player.level().isClientSide) return clientChanneling.test(player);
		Session s = SESSIONS.get(player.getUUID());
		return s != null && s.state == State.PROJECTING;
	}

	/** Any part of Soul Projection is under way for this player (server). */
	public static boolean isActive(UUID player) {
		return SESSIONS.containsKey(player);
	}

	public static State state(ServerPlayer player) {
		Session s = SESSIONS.get(player.getUUID());
		return s == null ? State.NONE : s.state;
	}

	/** The soul possessing {@code entity}, or null: a soul riding it (both sides). */
	public static Player possessorOf(Entity entity) {
		return entity != null && entity.getFirstPassenger() instanceof Player player && isSoul(player) ? player : null;
	}

	public static boolean isPossessed(Entity entity) {
		return possessorOf(entity) != null;
	}

	/** Where the soul's body sits (server); null if the player has no soul out. */
	public static Vec3 bodyPos(ServerPlayer player) {
		Session s = SESSIONS.get(player.getUUID());
		return s == null || s.state == State.PROJECTING ? null : s.bodyPos;
	}

	/**
	 * Whether {@code viewer}'s client is sent the soul at all: Heavenly Beings and above always (they may strike at it, and see it
	 * under Qi Sense), Nascent Soul cultivators only with Qi Sense open, spectators always, nobody else.
	 */
	public static boolean perceives(ServerPlayer viewer) {
		if (viewer.isSpectator()) return true;
		PlayerCultivation c = CultivationManager.get(viewer);
		if (c.isMortal()) return false;
		Realm realm = c.getEffectiveRealm();
		if (realm.ordinal() > Realm.NASCENT_SOUL.ordinal()) return true;
		return realm == Realm.NASCENT_SOUL && c.isQiSenseOn();
	}

	/**
	 * Share of a blow from {@code attacker} that a soul takes: 0 from anyone who can't see it, a part from a Nascent Soul cultivator
	 * (it is a haze to them), all of it from a Heavenly Being or above. Cultivators only, players and NPCs alike.
	 */
	public static float harmFactor(Entity attacker) {
		int rank;
		if (attacker instanceof ServerPlayer player) {
			if (isSoul(player)) return 0;
			PlayerCultivation c = CultivationManager.get(player);
			if (c.isMortal()) return 0;
			rank = c.effectiveRank();
			if (rank < PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY) && !c.isQiSenseOn()) return 0;
		} else if (attacker instanceof CultivatorNpc npc && !npc.isMortal()) {
			rank = npc.sustainedRank();
		} else {
			return 0;
		}
		int nascent = PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
		if (rank < nascent) return 0;
		return rank - nascent < NASCENT_SOUL_HARM.length ? NASCENT_SOUL_HARM[rank - nascent] : 1;
	}

	// --- Going out, and back ---

	/** The ability wheel or the Abilities tab: go out, call the soul back, or think better of either. */
	public static void toggle(ServerPlayer player) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null) {
			begin(player);
			return;
		}
		switch (s.state) {
			case PROJECTING -> cancelChannel(player, ModLang.MSG_SOUL_CANCELLED);
			case PROJECTED -> beginRecall(player, s);
			case RECALLING -> {
				s.state = State.PROJECTED; // stays out after all
				s.ticks = 0;
				setState(player, State.PROJECTED);
			}
			default -> { }
		}
	}

	/** Starts gathering the soul to go out: sitting still on steady, dry ground, outside any tribulation, with qi to spare. */
	public static void begin(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		if (!Ability.SOUL_PROJECTION.isUnlocked(c) || !player.isAlive() || player.isSpectator()) return;
		if (TribulationManager.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return;
		}
		ResourceKey<Level> dimension = player.level().dimension();
		if (ModDimensions.isSpatialGap(dimension) || ModDimensions.isInnerRealm(dimension) || SpatialTrialHandler.isInTrial(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_NOT_HERE), true);
			return;
		}
		if (!player.onGround() || player.isInWaterOrBubble() || player.isPassenger() || player.isSleeping() || player.isFallFlying()
				|| player.getAbilities().flying) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_GROUND), true);
			return;
		}
		if (c.getQi() < c.maxQi() * MIN_QI_FRACTION) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_LOW_QI, (int) (MIN_QI_FRACTION * 100)), true);
			return;
		}
		MeditationManager.stop(player, false); // no-op unless meditating
		Session s = new Session();
		s.anchor = player.position();
		SESSIONS.put(player.getUUID(), s);
		setState(player, State.PROJECTING);
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 1.6f);
		player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_GATHERING, CHANNEL_TICKS / 20), true);
	}

	/** Going out is broken off: the soul settles back, nothing lost. */
	private static void cancelChannel(ServerPlayer player, String why) {
		if (SESSIONS.remove(player.getUUID()) == null) return;
		setState(player, State.NONE);
		player.displayClientMessage(Component.translatable(why), true);
	}

	/** The soul leaves the body: the body sits on, and the player flies on as the soul. */
	private static void emerge(ServerPlayer player, Session s) {
		ServerLevel level = player.serverLevel();
		PlayerCultivation c = CultivationManager.get(player);
		SoulBodyEntity body = SoulBodyEntity.of(player);
		level.addFreshEntity(body);
		ChunkPos chunk = new ChunkPos(body.blockPosition());
		level.getChunkSource().addRegionTicket(BODY_TICKET, chunk, BODY_TICKET_RADIUS, chunk);
		s.body = body.getUUID();
		s.dimension = level.dimension();
		s.bodyPos = body.position();
		s.state = State.PROJECTED;
		s.tether = tetherStrength(c);

		CompoundTag back = new CompoundTag();
		back.putString("Dimension", level.dimension().location().toString());
		back.putDouble("X", player.getX());
		back.putDouble("Y", player.getY());
		back.putDouble("Z", player.getZ());
		back.putFloat("YRot", player.getYRot());
		back.putFloat("XRot", player.getXRot());
		back.putFloat("Health", player.getHealth());
		back.putUUID("Body", body.getUUID());
		c.setSoulReturn(back);

		// The soul's own body of qi: its hearts by stage, whole as it sets out.
		player.stopUsingItem();
		player.clearFire();
		updateSoulHealth(player, c);
		player.setHealth(player.getMaxHealth());
		Abilities abilities = player.getAbilities();
		abilities.mayfly = true; // only for the server's flight check: the soul's own flight is the client's (see ClientSoul)
		abilities.flying = true;
		player.onUpdateAbilities();
		player.setDeltaMovement(new Vec3(0, 0.15, 0));
		// Whatever was after the cultivator turns on the body it left.
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m.getTarget() == player)) {
			mob.setTarget(body);
		}
		setState(player, State.PROJECTED);
		RealmSuppressSystem.refreshNow(); // a soul neither presses nor is pressed
		refreshTracking(player); // only those who perceive it are still sent it
		level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.0f, 0.5f);
		level.sendParticles(VIOLET, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
		player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_OUT), true);
	}

	/** Called home: the soul lets go of anything it holds and is drawn back along the tether (the client flies it there). */
	private static void beginRecall(ServerPlayer player, Session s) {
		if (s.possessed != null) release(player, s, false);
		s.state = State.RECALLING;
		s.ticks = CHANNEL_TICKS;
		setState(player, State.RECALLING);
		player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_RECALLING, CHANNEL_TICKS / 20), true);
	}

	/**
	 * The soul goes back into its body: the player stands where the body sat, with the body's health, and the body is gone. Also
	 * the way out of every other ending (qi spent, a logout, the server stopping), and before a death (so its drops fall there).
	 */
	private static void merge(ServerPlayer player, Session s) {
		if (s.possessed != null) release(player, s, false);
		SESSIONS.remove(player.getUUID());
		PlayerCultivation c = CultivationManager.get(player);
		MinecraftServer server = player.server;
		CompoundTag back = c.getSoulReturn();
		ServerLevel level = s.dimension == null ? null : server.getLevel(s.dimension);
		if (level == null && back != null) level = server.getLevel(dimension(back));
		if (level == null) level = player.serverLevel();
		Vec3 pos = s.bodyPos;
		float yRot = back == null ? player.getYRot() : back.getFloat("YRot");
		float health = back == null ? player.getHealth() : back.getFloat("Health");
		if (s.body != null && level.getEntity(s.body) instanceof SoulBodyEntity body) {
			pos = body.position();
			yRot = body.getYRot();
			health = body.getHealth();
			body.discard();
		} else if (back != null && pos.equals(Vec3.ZERO)) {
			pos = new Vec3(back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z"));
		}
		ChunkPos chunk = new ChunkPos(BlockPos.containing(pos));
		level.getChunkSource().removeRegionTicket(BODY_TICKET, chunk, BODY_TICKET_RADIUS, chunk);

		if (player.isPassenger()) player.stopRiding();
		removeSoulHealth(player);
		if (player.level() != level) {
			player.teleportTo(level, pos.x, pos.y, pos.z, yRot, player.getXRot());
		} else {
			player.connection.teleport(pos.x, pos.y, pos.z, yRot, player.getXRot());
		}
		player.connection.resetPosition();
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0;
		player.noPhysics = false;
		player.setHealth(Math.max(1, Math.min(health, player.getMaxHealth())));
		Abilities abilities = player.getAbilities();
		abilities.flying = false;
		player.onUpdateAbilities(); // Qi Flight sets mayfly again next tick, as the realm allows
		c.setSoulReturn(null);
		setState(player, State.NONE);
		RealmSuppressSystem.refreshNow();
		refreshTracking(player); // seen by all again
		level.playSound(null, BlockPos.containing(pos), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.7f);
		level.sendParticles(VIOLET, pos.x, pos.y + 1.0, pos.z, 30, 0.3, 0.6, 0.3, 0.02);
	}

	/**
	 * The soul is lost: the cultivator dies (where the body sat, so that is where their things fall) and loses the nascent soul.
	 * {@code why} is told to them before they fall.
	 */
	public static void sever(ServerPlayer player, String why) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null || s.state == State.PROJECTING) return;
		merge(player, s);
		loseSoul(player, why);
		player.hurt(ModDamageTypes.soulSevered(player.level()), Float.MAX_VALUE);
	}

	/** The nascent soul is gone: the fall to Core Formation Grand Perfection, and the word of it. */
	private static void loseSoul(ServerPlayer player, String why) {
		PlayerCultivation c = CultivationManager.get(player);
		c.loseNascentSoul();
		CultivationManager.markDirty(player.server);
		player.sendSystemMessage(Component.translatable(why));
		player.sendSystemMessage(Component.translatable(ModLang.MSG_SOUL_LOST, PlayerCultivation.rankName(c.getRealm(), c.getStage())));
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.8f, 0.6f);
	}

	// --- Every tick ---

	public static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) return;
		long now = server.overworld().getGameTime();
		for (UUID id : new ArrayList<>(SESSIONS.keySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				SESSIONS.remove(id); // the disconnect already brought it home
				continue;
			}
			tick(player, now);
		}
	}

	/** One player's tick (package-private for the game tests, which drive fake players that aren't in the player list). */
	static void tick(ServerPlayer player, long now) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null || !player.isAlive()) return;
		try {
			if (s.state == State.PROJECTING) tickChannel(player, s);
			else tickSoul(player.server, player, s, now);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Soul projection tick failed for {}", player.getScoreboardName(), e);
		}
	}

	/**
	 * Has the entity tracker ask again who is sent this player (see {@link #broadcastsTo}). Only for players in the player list:
	 * the tracker's bookkeeping knows no others.
	 */
	public static void refreshTracking(ServerPlayer player) {
		if (player.server.getPlayerList().getPlayer(player.getUUID()) == player && !player.isRemoved()) {
			player.serverLevel().getChunkSource().move(player);
		}
	}

	private static void tickChannel(ServerPlayer player, Session s) {
		double dx = player.getX() - s.anchor.x, dz = player.getZ() - s.anchor.z;
		if (dx * dx + dz * dz > CHANNEL_DRIFT_SQR || !player.onGround() || TribulationManager.isActive(player.getUUID())) {
			cancelChannel(player, ModLang.MSG_SOUL_MOVED);
			return;
		}
		if (s.ticks % 5 == 0) {
			player.serverLevel().sendParticles(VIOLET, player.getX(), player.getY() + 0.4 + (CHANNEL_TICKS - s.ticks) / (double) CHANNEL_TICKS * 1.4,
					player.getZ(), 4, 0.35, 0.2, 0.35, 0.01);
		}
		if (s.ticks % 20 == 0) ModPackets.broadcastMeditation(player, true); // the sitting pose's heartbeat (see ClientMeditationTracker)
		if (--s.ticks <= 0) emerge(player, s);
	}

	private static void tickSoul(MinecraftServer server, ServerPlayer player, Session s, long now) {
		ServerLevel level = player.serverLevel();
		PlayerCultivation c = CultivationManager.get(player);
		// Commanded elsewhere (another dimension): the soul goes home at once.
		if (s.dimension != null && level.dimension() != s.dimension) {
			merge(player, s);
			return;
		}
		Entity found = level.getEntity(s.body);
		if (found instanceof SoulBodyEntity body && body.isAlive()) {
			s.bodyPos = body.position();
			s.bodyMissing = 0;
		} else if (++s.bodyMissing > BODY_GRACE_TICKS) {
			sever(player, ModLang.MSG_SOUL_BODY_GONE);
			return;
		}

		player.clearFire();
		player.setAirSupply(player.getMaxAirSupply());
		player.fallDistance = 0;
		if (player.tickCount % 20 == 0) updateSoulHealth(player, c);

		// The tether: the client keeps the soul within it; one well past the end is drawn back.
		int radius = c.soulTetherRadius();
		Vec3 center = player.position().add(0, 1, 0);
		Vec3 home = s.bodyPos.add(0, 1, 0);
		double distance = center.distanceTo(home);
		if (s.possessed != null) {
			tickPossession(player, s, radius);
			if (SESSIONS.get(player.getUUID()) != s) return;
		} else if (distance > radius + TETHER_SLACK && s.state == State.PROJECTED) {
			Vec3 to = home.add(center.subtract(home).normalize().scale(radius - 1)).subtract(0, 1, 0);
			player.connection.teleport(to.x, to.y, to.z, player.getYRot(), player.getXRot());
		}
		// Never into the void below the world, nor far above it.
		if (player.getY() < level.getMinBuildHeight() - 4) {
			player.connection.teleport(player.getX(), level.getMinBuildHeight() + 1, player.getZ(), player.getYRot(), player.getXRot());
		} else if (player.getY() > level.getMaxBuildHeight() + 64) {
			player.connection.teleport(player.getX(), level.getMaxBuildHeight() + 60, player.getZ(), player.getYRot(), player.getXRot());
		}
		if (player.tickCount % 2 == 0) SoulBarriers.check(player, s.possessed != null);
		if (SESSIONS.get(player.getUUID()) != s) return;

		// The tether mends when left alone.
		if (now - s.tetherStruck > TETHER_MEND_TICKS) s.tether = tetherStrength(c);
		// Monsters near the unguarded body go for it.
		if (player.tickCount % 20 == 0 && found instanceof SoulBodyEntity body) {
			AABB around = body.getBoundingBox().inflate(BODY_NOTICE_RANGE);
			for (Monster monster : level.getEntitiesOfClass(Monster.class, around, m -> m.getTarget() == null && m.isAlive())) {
				monster.setTarget(body);
			}
		}
		if (player.tickCount % SELF_SYNC_INTERVAL == 0) sendSelf(player, s, c, distance, radius);
		if (player.tickCount % SEEN_SYNC_INTERVAL == 0) {
			broadcastSeen(player, s, c);
			senseDomains(player, s);
		}
		if (player.tickCount % 20 == 0) refreshTracking(player); // whoever has come to see it (or stopped) since

		if (s.state == State.RECALLING && --s.ticks <= 0) {
			merge(player, s);
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BACK), true);
		}
	}

	/**
	 * Qi a second the soul costs this tick, called by {@link QiManager} for each living player before the pool gathers. Out of
	 * qi, the soul snaps back at once (no 5 s, no penalty) and nothing more is charged.
	 */
	static double upkeep(ServerPlayer player, PlayerCultivation c) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null || s.state == State.PROJECTING) return 0;
		Vec3 at = s.possessed != null && player.getVehicle() != null ? player.getVehicle().position() : player.position();
		double reach = at.distanceTo(s.bodyPos) / c.soulTetherRadius();
		double cost = c.soulCostPerSecond(reach, s.possessed != null);
		if (c.getQi() + (c.qiGatherPerSecond() - cost) / 20.0 >= 0) return cost;
		c.setQi(0);
		merge(player, s);
		player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_NO_QI), true);
		return 0;
	}

	// --- Harm ---

	/**
	 * Whether a soul takes this harm at all: only from cultivators who see it (Nascent Soul with Qi Sense, Heavenly Being and
	 * above), struck by their own hand (no arrows, fire or falls), and never while it is inside a body it possesses. What kills
	 * outright (a severed tether, a command) always goes through.
	 */
	public static boolean allowSoulDamage(ServerPlayer soul, DamageSource source) {
		if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
		Session s = SESSIONS.get(soul.getUUID());
		if (s == null || s.state == State.PROJECTING) return true;
		if (s.possessed != null) return false;
		Entity attacker = source.getEntity();
		if (attacker == null || source.getDirectEntity() != attacker) return false;
		if (attacker instanceof Player player && !soul.canHarmPlayer(player)) return false;
		if (harmFactor(attacker) <= 0) return false;
		s.lastHurt = soul.server.overworld().getGameTime();
		return true;
	}

	/** What a blow on a soul comes to: the attacker's share (see {@link #harmFactor}); anything else is untouched. */
	public static float scaleSoulDamage(LivingEntity target, DamageSource source, float amount) {
		if (!(target instanceof ServerPlayer soul) || !isSoul(soul) || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
		Entity attacker = source.getEntity();
		return attacker == null ? amount : amount * harmFactor(attacker);
	}

	/**
	 * A soul is about to die from its wounds (struck down, or killed outright): it dies where its body sat, and the nascent soul is
	 * lost. Called before the death goes ahead, so its drops fall at the body.
	 */
	public static void onSoulDying(ServerPlayer player) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null) return;
		if (s.state == State.PROJECTING) {
			cancelChannel(player, ModLang.MSG_SOUL_CANCELLED);
			return;
		}
		merge(player, s);
		player.setHealth(0); // the death goes ahead (merge gave it the body's health; a living "dead" player can't respawn)
		loseSoul(player, ModLang.MSG_SOUL_DESTROYED);
	}

	/** The body was hurt: its owner hears of it (now and then), and it counts as being in a fight. */
	public static void onBodyHurt(SoulBodyEntity body, UUID owner) {
		Session s = SESSIONS.get(owner);
		if (s == null || !(body.level() instanceof ServerLevel level)) return;
		long now = level.getServer().overworld().getGameTime();
		s.lastHurt = now;
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
		if (player == null || now - s.bodyAlerted < BODY_ALERT_INTERVAL) return;
		s.bodyAlerted = now;
		player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BODY_HURT, Math.round(body.getHealth()), Math.round(body.getMaxHealth())), true);
		player.playNotifySound(SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
	}

	/** The body was slain: the soul dies with it. */
	public static void onBodySlain(UUID owner, MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayer(owner);
		if (player != null) sever(player, ModLang.MSG_SOUL_BODY_SLAIN);
	}

	/** Whether {@code body} is still the body of a soul out of it (so it stays in the world). */
	public static boolean ownsBody(UUID owner, UUID body) {
		Session s = SESSIONS.get(owner);
		return s != null && body.equals(s.body);
	}

	/** The tether's full strength: a base, plus the soul's own health. */
	private static double tetherStrength(PlayerCultivation c) {
		return TETHER_BASE_STRENGTH + c.soulHearts() * 2.0;
	}

	/**
	 * A Heavenly Being (or above) with Qi Sense on strikes at the tether of the soul with entity id {@code soulId} (C2S): checked
	 * here against the striker's reach and aim. Their blow (as their attack charge allows) wears it down; at nothing it is cut.
	 */
	public static void strikeTether(ServerPlayer striker, int soulId) {
		if (!striker.isAlive() || striker.isSpectator() || isSoul(striker) || isActive(striker.getUUID())) return;
		PlayerCultivation c = CultivationManager.get(striker);
		if (c.isMortal() || c.effectiveRank() < PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY) || !c.isQiSenseOn()) return;
		if (!(striker.serverLevel().getEntity(soulId) instanceof ServerPlayer soul) || soul == striker || !isSoul(soul)) return;
		if (!soul.canHarmPlayer(striker)) return;
		Session s = SESSIONS.get(soul.getUUID());
		if (s == null) return;
		Vec3 a = s.bodyPos.add(0, 1.0, 0);
		Entity holder = s.possessed != null && soul.getVehicle() != null ? soul.getVehicle() : soul;
		Vec3 b = holder.position().add(0, holder.getBbHeight() * 0.55, 0);
		Vec3 eye = striker.getEyePosition();
		Vec3 look = striker.getViewVector(1.0f);
		if (rayToSegment(eye, look, TETHER_STRIKE_REACH, a, b) > TETHER_STRIKE_WIDTH) return;
		float charge = striker.getAttackStrengthScale(0.5f);
		striker.resetAttackStrengthTicker();
		if (charge < 0.3f) return;
		double blow = striker.getAttributeValue(Attributes.ATTACK_DAMAGE) * charge;
		long now = striker.server.overworld().getGameTime();
		s.tether -= blow;
		s.tetherStruck = now;
		s.lastHurt = now;
		ServerLevel level = striker.serverLevel();
		Vec3 hit = closestOnSegment(eye, look, a, b);
		level.sendParticles(new DustParticleOptions(new Vector3f(0.4f, 0.75f, 1.0f), 1.0f), hit.x, hit.y, hit.z, 12, 0.15, 0.15, 0.15, 0.02);
		level.playSound(null, BlockPos.containing(hit), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0f, 0.6f);
		soul.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_TETHER_STRUCK, striker.getDisplayName()), true);
		if (s.tether <= 0) {
			level.playSound(null, BlockPos.containing(hit), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.5f);
			soul.setLastHurtByPlayer(striker); // the death message names who cut it
			sever(soul, ModLang.MSG_SOUL_SEVERED);
		}
	}

	/**
	 * Shortest distance between a ray ({@code origin} along unit {@code dir}, up to {@code reach}) and the segment a-b. Also used by
	 * the client to decide whether a swing could have struck a tether.
	 */
	public static double rayToSegment(Vec3 origin, Vec3 dir, double reach, Vec3 a, Vec3 b) {
		double best = Double.MAX_VALUE;
		int steps = 24;
		for (int i = 0; i <= steps; i++) {
			Vec3 p = a.add(b.subtract(a).scale(i / (double) steps));
			double t = Math.max(0, Math.min(reach, p.subtract(origin).dot(dir)));
			best = Math.min(best, origin.add(dir.scale(t)).distanceTo(p));
		}
		return best;
	}

	private static Vec3 closestOnSegment(Vec3 origin, Vec3 dir, Vec3 a, Vec3 b) {
		Vec3 best = a;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i <= 24; i++) {
			Vec3 p = a.add(b.subtract(a).scale(i / 24.0));
			double t = Math.max(0, Math.min(TETHER_STRIKE_REACH, p.subtract(origin).dot(dir)));
			double d = origin.add(dir.scale(t)).distanceTo(p);
			if (d < bestDistance) {
				bestDistance = d;
				best = p;
			}
		}
		return best;
	}

	// --- Possession ---

	/**
	 * The soul (right-clicking) enters {@code target}: within reach, at least {@link #POSSESS_REALM_GAP} realms below it, with
	 * {@link #POSSESS_QI_FRACTION} of its pool to pay. Not a boss, a Warden, anyone's pet but its own, a named creature, a mount, or
	 * another soul. A player's body is seized instead (they die; see {@link #seize}).
	 */
	public static InteractionResult tryPossess(ServerPlayer player, Entity target) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null || s.state != State.PROJECTED || s.possessed != null) return InteractionResult.FAIL;
		if (!(target instanceof LivingEntity living) || !living.isAlive() || player.distanceTo(target) > POSSESS_REACH + target.getBbWidth()) {
			return InteractionResult.FAIL;
		}
		PlayerCultivation c = CultivationManager.get(player);
		int own = c.getEffectiveRealm().ordinal();
		String refusal = refusal(player, living, own);
		if (refusal != null) {
			player.displayClientMessage(Component.translatable(refusal), true);
			return InteractionResult.FAIL;
		}
		if (!c.consumeQi(c.maxQi() * POSSESS_QI_FRACTION)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_POSSESS_QI, (int) (POSSESS_QI_FRACTION * 100)), true);
			return InteractionResult.FAIL;
		}
		CultivationManager.markDirty(player.server);
		Mob host = target instanceof ServerPlayer victim ? seize(player, victim) : (Mob) target;
		if (host == null) return InteractionResult.FAIL;
		possess(player, s, host);
		return InteractionResult.SUCCESS;
	}

	/** Why {@code player}'s soul can't enter {@code target} (a lang key), or null if it can. */
	private static String refusal(ServerPlayer player, LivingEntity target, int ownRealm) {
		if (target instanceof ServerPlayer victim) {
			if (victim == player || victim.isCreative() || victim.isSpectator() || isActive(victim.getUUID()) || InnerRealm.isInside(victim)) {
				return ModLang.MSG_POSSESS_CANNOT;
			}
			if (!victim.canHarmPlayer(player) || !player.server.isPvpAllowed()) return ModLang.MSG_POSSESS_PVP;
			if (TribulationManager.isActive(victim.getUUID())) return ModLang.MSG_POSSESS_CANNOT;
		} else if (!(target instanceof Mob mob) || !canPossess(mob, player)) {
			return ModLang.MSG_POSSESS_CANNOT;
		}
		return realmOf(target) > ownRealm - POSSESS_REALM_GAP ? ModLang.MSG_POSSESS_TOO_STRONG : null;
	}

	/** What may be possessed at all: no bosses or Wardens, no mounts, no one else's pet, nothing named, nothing ridden or riding. */
	private static boolean canPossess(Mob mob, ServerPlayer player) {
		if (mob instanceof EnderDragon || mob instanceof WitherBoss || mob instanceof ElderGuardian || mob instanceof Warden) return false;
		if (mob instanceof HeartDemonEntity || mob instanceof PlayerRideable || mob.isVehicle() || mob.isPassenger()) return false;
		if (mob instanceof SoulVesselEntity) return false; // a seized body has its one soul already, or is collapsing
		if (mob.hasCustomName()) return false;
		return !(mob instanceof OwnableEntity pet) || pet.getOwnerUUID() == null || pet.getOwnerUUID().equals(player.getUUID());
	}

	/** A being's realm as possession weighs it: a cultivator's realm, -1 for mortals and everything that doesn't cultivate. */
	public static int realmOf(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			PlayerCultivation c = CultivationManager.get(player);
			return c.isMortal() ? -1 : c.getEffectiveRealm().ordinal();
		}
		if (entity instanceof CultivatorNpc npc) return npc.isMortal() ? -1 : CultivatorNpc.realmOf(npc.sustainedRank()).ordinal();
		return -1;
	}

	/**
	 * A player's body is seized: a vessel in their likeness stands where they did, carrying everything they had (unless items are
	 * kept on death), and they die, their soul driven out.
	 */
	private static Mob seize(ServerPlayer soul, ServerPlayer victim) {
		ServerLevel level = victim.serverLevel();
		boolean keep = level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
		SoulVesselEntity vessel = SoulVesselEntity.of(victim, !keep);
		level.addFreshEntity(vessel);
		victim.hurt(ModDamageTypes.soulSeized(level, soul), Float.MAX_VALUE);
		if (victim.isAlive()) { // something kept them alive (creative's invulnerability is already ruled out)
			vessel.discard();
			return null;
		}
		return vessel;
	}

	private static void possess(ServerPlayer player, Session s, Mob host) {
		host.getNavigation().stop();
		host.setTarget(null);
		if (host instanceof CultivatorNpc npc) {
			npc.setMeditating(false);
			if (npc.isQiFlying()) npc.land();
		}
		if (!player.startRiding(host, true)) return;
		s.possessed = host.getUUID();
		if (flies(host)) {
			// It hangs in the air on its soul's keys: weightless while held, or the server would take the soul for one floating its
			// mount and turn it out. A roosting bat is roused, since its own mind, which would wake it, is silent now.
			s.weightless = !host.isNoGravity();
			host.setNoGravity(true);
			if (host instanceof Bat bat) bat.setResting(false);
		}
		// Whoever it was talking with gets no more answers (its own AI, which would break the talk off, no longer runs).
		if (host instanceof CultivatorNpc npc) npc.breakOffTalk(ModLang.TALK_POSSESSED);
		ServerLevel level = player.serverLevel();
		level.playSound(null, host.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.7f);
		level.sendParticles(VIOLET, host.getX(), host.getY() + host.getBbHeight() * 0.6, host.getZ(), 30, 0.3, 0.5, 0.3, 0.02);
		player.displayClientMessage(Component.translatable(ModLang.MSG_POSSESS_START, host.getDisplayName()), true);
		sendSelf(player, s, CultivationManager.get(player), 0, CultivationManager.get(player).soulTetherRadius());
	}

	/** Each tick while possessing: the soul keeps to its host, and its host to the tether. */
	private static void tickPossession(ServerPlayer player, Session s, int radius) {
		Entity vehicle = player.getVehicle();
		Entity host = player.serverLevel().getEntity(s.possessed);
		if (host == null || !host.isAlive()) {
			// The host died (or was taken from the world): the soul is thrown out unharmed.
			s.possessed = null;
			s.weightless = false;
			if (player.isPassenger()) player.stopRiding();
			player.displayClientMessage(Component.translatable(ModLang.MSG_POSSESS_EJECTED), true);
			sendSelf(player, s, CultivationManager.get(player), 0, radius);
			return;
		}
		if (vehicle != host) { // let go (sneaking)
			release(player, s, false);
			return;
		}
		// A flier's way down is flight, not a fall (the server moves it as its soul's client says, and would count the drop).
		if (flies(host)) host.resetFallDistance();
		if (host.position().distanceTo(s.bodyPos) > radius) {
			release(player, s, true);
			player.displayClientMessage(Component.translatable(ModLang.MSG_POSSESS_TETHER), true);
		}
	}

	/** The soul leaves its host (a seized body collapses: nothing holds it up without a soul). */
	private static void release(ServerPlayer player, Session s, boolean pulled) {
		Entity host = s.possessed == null ? null : player.serverLevel().getEntity(s.possessed);
		s.possessed = null;
		if (host != null && s.weightless) host.setNoGravity(false);
		s.weightless = false;
		if (player.isPassenger()) player.stopRiding();
		if (host instanceof SoulVesselEntity vessel && vessel.isAlive()) vessel.collapse();
		else if (host instanceof Mob mob) mob.setTarget(null);
		if (pulled && host != null) { // out where the host stood; the tether draws it back in next tick
			player.connection.teleport(host.getX(), host.getY() + 0.5, host.getZ(), player.getYRot(), player.getXRot());
		}
		sendSelf(player, s, CultivationManager.get(player), 0, CultivationManager.get(player).soulTetherRadius());
	}

	/**
	 * How a possessed body steps for its soul's keys ({@code strafe}, {@code forward}, a player's walking input): at the body's
	 * own pace, as fast as its own legs carry it when it runs at something, and no faster on the diagonal. A mob walking of its
	 * own accord steps by its speed itself (Mob#setSpeed sets its forward input to it), so its speed tells twice in how fast it
	 * goes; the soul's keys are scaled by it the same way. Unscaled, as a horse's rider gets the whole speed from a full key, a
	 * spider ran three times as fast as any spider can.
	 */
	public static Vec3 hostStep(LivingEntity host, float strafe, float forward) {
		Vec3 keys = new Vec3(strafe, 0, forward);
		if (keys.lengthSqr() > 1) keys = keys.normalize();
		return keys.scale(host.getAttributeValue(Attributes.MOVEMENT_SPEED));
	}

	// --- A possessed flier ---

	/** The air's drag on a flier each tick (vanilla's own): how much of its speed it keeps, and so how quickly it gathers more. */
	public static final double AIR_DRAG = 0.91;
	/** Of its pace in the air, a flier keeps this much in water, and in lava (as vanilla's fliers swim: about half, and a fifth). */
	private static final double WATER_PACE = 0.45, LAVA_PACE = 0.2;
	/**
	 * Blocks a tick at full flight, as each flies by itself against the air's drag: a vex's charge (0.05 a tick, its own mover),
	 * a ghast's drift (0.1 every four ticks or so), a bat's flutter (steering for half a block a tick, Bat#customServerAiStep).
	 */
	private static final double VEX_PACE = 0.556, GHAST_PACE = 0.278, BAT_PACE = 0.33;
	/** A phantom's glide, blocks a tick (its own mover): from the least, gathering speed while it holds its line, up to the most. */
	public static final float GLIDE_MIN = 0.2f, GLIDE_MAX = 1.8f;

	/**
	 * A body that flies, so that possessed it flies where its soul looks rather than walking: bees, parrots, allays, bats, ghasts,
	 * phantoms, vexes and blazes, and anything else that moves on wings (a flying mover, a flier's travel).
	 */
	public static boolean flies(Entity host) {
		return host instanceof FlyingMob || host instanceof FlyingAnimal || host instanceof Bat || host instanceof Vex || host instanceof Blaze
				|| host instanceof Mob mob && mob.getMoveControl() instanceof FlyingMoveControl;
	}

	/**
	 * Where a soul's keys send the flier it possesses, as it flies itself (ClientSoul#fly) but with sneaking kept for letting go:
	 * forward along its line of sight, so looking down and going forward goes down; strafing level; jump straight up. At most a
	 * full step.
	 */
	public static Vec3 flightWish(float strafe, float forward, boolean up, Vec3 look, float yRot) {
		float yaw = yRot * ((float) Math.PI / 180f);
		Vec3 wish = look.scale(forward).add(new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(strafe)).add(0, up ? 1 : 0, 0);
		return wish.lengthSqr() > 1 ? wish.normalize() : wish;
	}

	/**
	 * A flier's own pace in the air, blocks a tick at full flight: what its own wings give it (a vex's charge, a ghast's drift, a
	 * bat's flutter, a phantom's {@code glide}); for one with a flying speed, that worked against the air as its flying mover
	 * works it (a bee 0.13, a parrot 0.09, an allay 0.11); for any other (a blaze), its walking pace. Less in water or lava.
	 */
	public static double flightPace(LivingEntity host, float glide) {
		double pace;
		if (host instanceof Phantom) pace = glide;
		else if (host instanceof Vex) pace = VEX_PACE;
		else if (host instanceof Bat) pace = BAT_PACE;
		else if (host instanceof Ghast) pace = GHAST_PACE;
		else if (host.getAttribute(Attributes.FLYING_SPEED) != null) {
			// Its flying mover sets its speed to this and its step to the same: an allay goes by its speed (Allay#travel), the rest by 0.02 of it.
			double wings = host.getAttributeValue(Attributes.FLYING_SPEED);
			pace = (host instanceof Allay ? wings : 0.02) * wings * 0.98 / (1 - AIR_DRAG);
		} else if (host instanceof FlyingMob) {
			pace = GHAST_PACE;
		} else {
			// Its speed tells twice on the ground (see hostStep), against the drag of ordinary ground.
			double legs = host.getAttributeValue(Attributes.MOVEMENT_SPEED);
			pace = 0.98 * legs * legs / (1 - 0.6 * AIR_DRAG);
		}
		if (host.isInLava()) return pace * LAVA_PACE;
		return host.isInWater() ? pace * WATER_PACE : pace;
	}

	/**
	 * A phantom's glide a tick on, as its own mover keeps it: gathering speed toward {@link #GLIDE_MAX} while it holds its line
	 * (turning less than 3 degrees a tick), losing it toward {@link #GLIDE_MIN} while it turns or hangs still.
	 */
	public static float glide(float glide, float turned) {
		return Math.abs(turned) < 3 ? Mth.approach(glide, GLIDE_MAX, 0.005f * (GLIDE_MAX / glide)) : Mth.approach(glide, GLIDE_MIN, 0.025f);
	}

	/**
	 * A tick of a possessed flier's flight: from {@code velocity} toward {@code wish} at {@code pace}, gathering speed and losing
	 * it as through the air, so that it holds exactly its pace, and drifts to a stop in the air when the keys are let go. Its
	 * wings hold it up: no gravity.
	 */
	public static Vec3 flightVelocity(Vec3 velocity, Vec3 wish, double pace) {
		return velocity.scale(AIR_DRAG).add(wish.scale(pace * (1 - AIR_DRAG)));
	}

	/**
	 * The possessed body is about to die (its own death, not the soul's): the kill is the possessor's, so nothing dies "of its own"
	 * in a soul's hands (a sect member drowned by its possessor is murdered, not replaced).
	 */
	public static void onHostDying(LivingEntity host, DamageSource source) {
		Player possessor = possessorOf(host);
		if (possessor == null || source.getEntity() instanceof Player) return;
		host.setLastHurtByPlayer(possessor);
	}

	/**
	 * The soul, while possessing, attacks: its host strikes instead (with the host's own strength). Its own blows land nowhere else.
	 */
	public static void hostAttack(ServerPlayer player, Entity target) {
		if (!(player.getVehicle() instanceof Mob host) || target == host || !(target instanceof LivingEntity living)) return;
		if (host.distanceTo(target) > 4.5 + target.getBbWidth()) return;
		// The blow is the soul's: whatever it kills through its host, it killed (a sect member slain so is murdered, not replaced).
		living.setLastHurtByPlayer(player);
		host.swing(InteractionHand.MAIN_HAND);
		if (host.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			host.doHurtTarget(target);
		} else {
			target.hurt(host.damageSources().mobAttack(host), 1.0f); // a beast with no attack of its own butts and shoves
		}
	}

	// --- Ending, logging out, coming back ---

	/**
	 * Logging out with the soul out: it goes home, unless the soul or the body was harmed within {@link #COMBAT_MEMORY_TICKS}, in
	 * which case quitting is fleeing a fight: the tether is cut. Gathering to go out simply stops.
	 */
	public static void onDisconnect(ServerPlayer player) {
		Session s = SESSIONS.get(player.getUUID());
		if (s == null) return;
		if (s.state == State.PROJECTING) {
			SESSIONS.remove(player.getUUID());
			setState(player, State.NONE);
			return;
		}
		long now = player.server.overworld().getGameTime();
		if (now - s.lastHurt < COMBAT_MEMORY_TICKS) {
			sever(player, ModLang.MSG_SOUL_FLED);
		} else {
			merge(player, s);
		}
	}

	/** The server is stopping: every soul goes home, no penalty, before the players are saved. */
	public static void returnAll(MinecraftServer server) {
		for (UUID id : new ArrayList<>(SESSIONS.keySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			Session s = SESSIONS.get(id);
			if (player == null || s == null) {
				SESSIONS.remove(id);
				continue;
			}
			if (s.state == State.PROJECTING) {
				SESSIONS.remove(id);
				setState(player, State.NONE);
			} else {
				merge(player, s);
			}
		}
	}

	/**
	 * A player joins still marked as out of the body (a crash, or a server killed mid-way): back to where the body sat, with its
	 * health, no penalty. The body itself removes itself (no soul claims it).
	 */
	public static void onJoin(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		CompoundTag back = c.getSoulReturn();
		if (c.isAbilityEnabled(Ability.SOUL_PROJECTION)) c.setAbilityEnabled(Ability.SOUL_PROJECTION, false);
		c.setSoulState(State.NONE);
		if (back == null) return;
		c.setSoulReturn(null);
		CultivationManager.markDirty(player.server);
		ServerLevel level = player.server.getLevel(dimension(back));
		if (level == null) level = player.server.overworld();
		removeSoulHealth(player);
		player.teleportTo(level, back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z"), back.getFloat("YRot"), back.getFloat("XRot"));
		player.setHealth(Math.max(1, Math.min(back.getFloat("Health"), player.getMaxHealth())));
		player.getAbilities().flying = false;
		player.onUpdateAbilities();
		if (back.hasUUID("Body") && level.getEntity(back.getUUID("Body")) instanceof SoulBodyEntity body) body.discard();
	}

	/** After any death: nothing of a soul session survives it. */
	public static void onDeath(ServerPlayer player) {
		Session s = SESSIONS.remove(player.getUUID());
		ServerLevel level = s == null || s.dimension == null ? null : player.server.getLevel(s.dimension);
		if (level != null && s.body != null && level.getEntity(s.body) instanceof SoulBodyEntity body) body.discard();
		PlayerCultivation c = CultivationManager.get(player);
		c.setSoulReturn(null);
		setState(player, State.NONE);
	}

	public static void clear() {
		SESSIONS.clear();
	}

	// --- Helpers ---

	/** Records the soul's state (synced: the menu's switch, forced Qi Sense, the client's flight) and tells the player. */
	private static void setState(ServerPlayer player, State state) {
		PlayerCultivation c = CultivationManager.get(player);
		c.setSoulState(state);
		c.setAbilityEnabled(Ability.SOUL_PROJECTION, state != State.NONE);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		Session s = SESSIONS.get(player.getUUID());
		if (s == null) {
			ModPackets.sendSoulSelf(player, State.NONE, 0, Vec3.ZERO, 0, 0, 0, -1, 0);
			ModPackets.broadcastSoulGone(player);
		} else {
			sendSelf(player, s, c, 0, c.soulTetherRadius());
		}
		// Gathering to go out shows as sitting in meditation to those around.
		ModPackets.broadcastMeditation(player, state == State.PROJECTING || MeditationManager.isMeditating(player.getUUID()));
	}

	private static void sendSelf(ServerPlayer player, Session s, PlayerCultivation c, double distance, int radius) {
		Entity body = s.body == null ? null : player.serverLevel().getEntity(s.body);
		float health = body instanceof LivingEntity living ? living.getHealth() : 0;
		float max = body instanceof LivingEntity living ? living.getMaxHealth() : 0;
		Entity host = s.possessed == null ? null : player.serverLevel().getEntity(s.possessed);
		ModPackets.sendSoulSelf(player, s.state, s.ticks, s.bodyPos, radius, health, max, host == null ? -1 : host.getId(),
				(float) (s.tether / tetherStrength(c)));
	}

	/** Tells everyone who perceives the soul where its body is (for its tether), and which body it is in. */
	private static void broadcastSeen(ServerPlayer player, Session s, PlayerCultivation c) {
		Entity host = s.possessed == null ? null : player.serverLevel().getEntity(s.possessed);
		for (ServerPlayer viewer : player.serverLevel().players()) {
			if (viewer == player || viewer.distanceToSqr(player) > SEEN_RANGE * SEEN_RANGE || !perceives(viewer)) continue;
			ModPackets.sendSoulSeen(viewer, player, s.bodyPos, host == null ? -1 : host.getId(), (float) (s.tether / tetherStrength(c)));
		}
	}

	/**
	 * Core Formation Grand Perfection and above, with the Consciousness Domain open and unable to see the soul, sense it when it
	 * comes inside their domain: no outline, no realm, only the knowledge that one is near. Told once while it stays.
	 */
	private static void senseDomains(ServerPlayer soul, Session s) {
		int threshold = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
		Set<UUID> inside = new HashSet<>();
		for (ServerPlayer other : soul.serverLevel().players()) {
			if (other == soul || isSoul(other) || !ConsciousnessDomainHandler.isActive(other)) continue;
			PlayerCultivation c = CultivationManager.get(other);
			if (c.isMortal() || c.effectiveRank() < threshold) continue;
			double radius = ConsciousnessDomainHandler.radius(c);
			if (ConsciousnessDomainHandler.center(other).distanceToSqr(soul.position().add(0, 1, 0)) > radius * radius) continue;
			inside.add(other.getUUID());
			// Those who see it (Qi Sense open, Nascent Soul and above) need no telling.
			if (s.sensedBy.add(other.getUUID()) && !(c.isQiSenseOn() && c.getEffectiveRealm().canProjectSoul())) {
				other.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_SENSED), true);
				other.playNotifySound(SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.PLAYERS, 1.0f, 0.5f);
			}
		}
		s.sensedBy.retainAll(inside);
	}

	/** Keeps the soul's maximum health at its hearts, whatever else is added to or taken from the attribute. */
	private static void updateSoulHealth(ServerPlayer player, PlayerCultivation c) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health == null) return;
		AttributeModifier current = health.getModifier(SOUL_HEALTH_ID);
		double without = health.getValue() - (current == null ? 0 : current.getAmount());
		double wanted = c.soulHearts() * 2.0 - without;
		if (current != null && Math.abs(current.getAmount() - wanted) < 1.0e-6) return;
		health.removeModifier(SOUL_HEALTH_ID);
		health.addTransientModifier(new AttributeModifier(SOUL_HEALTH_ID, "Nascent soul's health", wanted, AttributeModifier.Operation.ADDITION));
		if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
	}

	private static void removeSoulHealth(ServerPlayer player) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) health.removeModifier(SOUL_HEALTH_ID);
	}

	private static ResourceKey<Level> dimension(CompoundTag back) {
		ResourceLocation id = ResourceLocation.tryParse(back.getString("Dimension"));
		return id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
	}

	/** Every soul out right now (for the game tests). */
	static List<UUID> souls() {
		return new ArrayList<>(SESSIONS.keySet());
	}

	/** Whether the tracker sends {@code soul} to {@code viewer} (see the ServerPlayer mixin): always, unless it is a soul unperceived. */
	public static boolean broadcastsTo(ServerPlayer soul, ServerPlayer viewer) {
		return !isSoul(soul) || viewer == soul || perceives(viewer);
	}

	/** Whether each online player's realm had awakened Soul Projection last tick, to announce it when first gained. */
	private static final Map<UUID, Boolean> AWAKENED = new HashMap<>();

	/**
	 * Called every tick by {@link QiManager}: the first time the nascent soul forms, the cultivator is told, and the ability is put
	 * in the first empty slot of their wheel.
	 */
	static void announce(ServerPlayer player, PlayerCultivation c) {
		boolean awakened = !c.isMortal() && c.getRealm().canProjectSoul();
		Boolean before = AWAKENED.put(player.getUUID(), awakened);
		if (!awakened || !Boolean.FALSE.equals(before)) return;
		player.sendSystemMessage(Component.translatable(ModLang.MSG_SOUL_GAINED));
		boolean placed = false;
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) placed |= c.getWheelSlot(i) == Ability.SOUL_PROJECTION;
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS && !placed; i++) {
			if (c.getWheelSlot(i) == null) {
				c.setWheelSlot(i, Ability.SOUL_PROJECTION);
				placed = true;
			}
		}
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
	}

	public static void forget(UUID id) {
		AWAKENED.remove(id);
	}

	private SoulProjection() {}
}
