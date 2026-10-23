package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Detonating the golden core (Ability#CORE_DETONATION), from Core Formation: the last resort. Asked once, the core only trembles
 * and the cultivator is warned; asked again within {@link #CONFIRM_TICKS}, it breaks: {@link #CHARGE_TICKS} of the core's light
 * swelling (nothing stops it now), then the blast. The cultivator dies, and the core is lost with them: they wake at Foundation
 * Building Grand Perfection, their heaven methods understood no deeper. Everything living within the blast's reach
 * ({@link #radius}) takes its harm ({@link #harm}): a full body's worth of the cultivator's own stage at the heart, less with
 * distance, more for every realm a victim stands below, far less for every realm above. A cultivator of the same realm in good
 * armour (Blast Protection best) may live through it. The ground at its heart is blown away ({@link #craterRadius}), a share at a
 * time (Devastation), so even a True Immortal's crater never stalls the world. The deeper the core, the further the blast reaches, the deeper its crater
 * and the further off it is heard.
 */
public final class CoreDetonation {
	/** The second asking must come within this of the first, and no sooner than {@link #CONFIRM_MIN_TICKS} after it. */
	public static final int CONFIRM_TICKS = 100, CONFIRM_MIN_TICKS = 10;
	/** From the second asking to the blast. */
	public static final int CHARGE_TICKS = 60;
	/**
	 * The blast's reach by realm, Core Formation to True Immortal, and a tenth more for each stage within it (the user's asks,
	 * 2026-10-23: the higher the realm, the larger the blast, a Heavenly Being's some 125 blocks and the rest scaled from it). To Four
	 * Axis, 35 blocks a realm; after it, some 45 for every doubling of the realm's might (Realm): Dao Integration twice Four Axis's
	 * (+45), Void Shattering four times (+90), Sacred Vessel and Entering Nirvana twice again (+45 each), True Immortal ten times
	 * (+160).
	 */
	private static final double[] RADIUS = {55, 90, 125, 160, 205, 295, 340, 385, 545};
	/** The harm at the heart: this many times the whole health of a body of the cultivator's own stage. */
	public static final double HEART_HARM = 1.1;
	/** Each realm a victim stands below the cultivator, the harm grows by the whole of it again; each realm above, it is this much. */
	public static final double ABOVE_FACTOR = 0.3;
	/**
	 * The crater, to Heavenly Being Grand Perfection: this share of the blast's reach round (48 blocks there, {@link #CRATER_PIVOT}),
	 * and this share of that deep.
	 */
	private static final double CRATER_SHARE = 0.32, CRATER_PIVOT = 48, CRATER_DEPTH = 0.79;
	/**
	 * Past Heavenly Being Grand Perfection the crater goes on growing (the user's ask, 2026-10-23: a True Immortal Grand Perfection's
	 * was no bigger than a Heavenly Being Grand Perfection's, which is where a Heavenly Being's should stand; the True Immortal's to
	 * be much bigger): round as the blast's reach (against Heavenly Being Grand Perfection's) to the power {@link #CRATER_GROWTH}, so a
	 * True Immortal Grand Perfection's is 128 blocks round, 256 across; deep as its roundness to the power {@link #DEPTH_GROWTH},
	 * shallower for its size the greater it is, as great craters are (62 deep at True Immortal Grand Perfection), and so carved in
	 * well under a minute (Devastation's budget).
	 */
	private static final double CRATER_GROWTH = 2.0 / 3.0, DEPTH_GROWTH = 0.5;

	private static final Map<UUID, Long> ASKED = new HashMap<>();
	private static final Map<UUID, Integer> CHARGING = new HashMap<>();

	public static boolean isCharging(UUID id) { return CHARGING.containsKey(id); }

	/** The ability's switch: the first asking warns; the second within {@link #CONFIRM_TICKS} begins the end. */
	public static void toggle(ServerPlayer player) {
		UUID id = player.getUUID();
		if (CHARGING.containsKey(id)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CORE_TOO_LATE).withStyle(ChatFormatting.RED), true);
			return;
		}
		// A soul in a person's body detonates that body's core, if it has one (Possession; the user's ask, 2026-10-18): the body dies
		// as its owner, its core lost, and the soul is thrown out.
		if (SoulProjection.isAwayFromBody(id) || InnerRealm.isInside(player) || !player.isAlive() || player.isSpectator()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		// A clone holds no core of its own (Clones): its maker's is in the main body.
		if (Clones.inClone(player)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CLONE_CANNOT), true);
			return;
		}
		long now = player.level().getGameTime();
		Long asked = ASKED.get(id);
		// A second asking hard on the first is a double click on the wheel, not a decision: it is let pass.
		if (asked != null && now - asked < CONFIRM_MIN_TICKS) return;
		ASKED.remove(id);
		if (asked == null || now - asked > CONFIRM_TICKS) {
			ASKED.put(id, now);
			// Over the hotbar, never in chat (user's ask).
			player.displayClientMessage(Component.translatable(ModLang.MSG_CORE_CONFIRM, CONFIRM_TICKS / 20).withStyle(ChatFormatting.RED), true);
			player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f);
			return;
		}
		begin(player);
	}

	/** The core breaks: {@link #CHARGE_TICKS} to the blast. */
	static void begin(ServerPlayer player) {
		CHARGING.put(player.getUUID(), CHARGE_TICKS);
		PlayerCultivation c = CultivationManager.get(player);
		c.setAbilityEnabled(Ability.CORE_DETONATION, true);
		CultivationManager.sync(player);
		MeditationManager.stop(player, true);
		// No word in chat (the user's ask): only those the blast could reach are warned, over their hotbar.
		double warn = 2 * radius(c);
		for (ServerPlayer near : player.serverLevel().players()) {
			if (near != player && near.distanceToSqr(player) <= warn * warn) {
				near.displayClientMessage(Component.translatable(ModLang.MSG_CORE_BREAKING, player.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
			}
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.5f);
	}

	public static void tick(MinecraftServer server) {
		if (CHARGING.isEmpty()) return;
		for (Iterator<Map.Entry<UUID, Integer>> it = CHARGING.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null || !p.isAlive()) {
				it.remove();
				continue;
			}
			int left = entry.getValue() - 1;
			entry.setValue(left);
			swell(p, CHARGE_TICKS - left);
			if (left <= 0) {
				it.remove();
				detonate(p);
			}
		}
	}

	/** The core's light swelling in the cultivator's chest, qi rushing inward; they can barely move. */
	private static void swell(ServerPlayer p, int age) {
		ServerLevel level = p.serverLevel();
		double share = age / (double) CHARGE_TICKS;
		Vec3 heart = p.position().add(0, 1.1, 0);
		level.sendParticles(ParticleTypes.END_ROD, heart.x, heart.y, heart.z, 2 + (int) (share * 10), 0.15, 0.15, 0.15, 0.01);
		for (int i = 0; i < 4; i++) { // qi drawn in from all round
			double a = p.getRandom().nextDouble() * Mth.TWO_PI, r = 3 + p.getRandom().nextDouble() * 4;
			Vec3 from = heart.add(Math.cos(a) * r, (p.getRandom().nextDouble() - 0.5) * 3, Math.sin(a) * r);
			Vec3 to = heart.subtract(from).scale(0.12);
			level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 0, to.x, to.y, to.z, 1.0);
		}
		if (age % 10 == 0) {
			level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 0.5f + (float) share * 1.2f);
		}
		p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false, false));
		p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false, false));
	}

	/**
	 * The path whose core it is (TwoPaths#awakens: the heaven path's golden core once it stands at Core Formation, else the ghost
	 * path's), and so whose rank the blast goes by and whose fall it is.
	 */
	static TwoPaths.Path corePath(PlayerCultivation c) {
		TwoPaths.Path path = TwoPaths.awakens(c, Realm.CORE_FORMATION, false);
		return path == null ? TwoPaths.Path.HEAVEN : path;
	}

	/** The rank of the core that breaks ({@link #corePath}): its realm and stage set the blast. */
	static int coreRank(PlayerCultivation c) {
		return Math.max(0, TwoPaths.rank(c, corePath(c), false));
	}

	/** The blast's reach for {@code c}'s core ({@link #coreRank}). */
	public static double radius(PlayerCultivation c) {
		int rank = coreRank(c), stages = Stage.values().length;
		return radius(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages));
	}

	/** The blast's reach for a cultivator of {@code realm} and {@code stage}. */
	public static double radius(Realm realm, Stage stage) {
		int r = Mth.clamp(realm.ordinal() - Realm.CORE_FORMATION.ordinal(), 0, RADIUS.length - 1);
		return RADIUS[r] * (1 + 0.1 * stage.ordinal());
	}

	/** How far round the crater of a cultivator of {@code realm} and {@code stage} is (blocks): {@link #CRATER_SHARE}, then {@link #CRATER_GROWTH}. */
	public static double craterRadius(Realm realm, Stage stage) {
		double blast = radius(realm, stage), pivot = radius(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
		if (blast <= pivot) return Math.min(blast * CRATER_SHARE, CRATER_PIVOT);
		return CRATER_PIVOT * Math.pow(blast / pivot, CRATER_GROWTH);
	}

	/** How deep that crater is (blocks): {@link #CRATER_DEPTH} of its roundness, then {@link #DEPTH_GROWTH}. */
	public static double craterDepth(Realm realm, Stage stage) {
		double round = craterRadius(realm, stage);
		if (round <= CRATER_PIVOT) return round * CRATER_DEPTH;
		return CRATER_PIVOT * CRATER_DEPTH * Math.pow(round / CRATER_PIVOT, DEPTH_GROWTH);
	}

	/**
	 * What the blast does to one {@code distance} from its heart, from a cultivator of {@code realm}-{@code stage} ({@code radius}
	 * its reach), to a victim whose realm index is {@code victimRealm} (-1 for a mortal or a beast): before armour.
	 */
	public static double harm(Realm realm, Stage stage, double radius, double distance, int victimRealm) {
		if (distance >= radius) return 0;
		double heart = HEART_HARM * (20 + CultivationStats.maxHealth(realm, stage));
		double falloff = 1 - distance / radius;
		int below = realm.ordinal() - victimRealm;
		double gap = below >= 0 ? 1 + below : Math.pow(ABOVE_FACTOR, -below);
		return heart * falloff * gap;
	}

	/** The core bursts: the blast, the crater, the cultivator's death and the core lost. */
	static void detonate(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		PlayerCultivation c = CultivationManager.get(p);
		TwoPaths.Path path = corePath(c);
		int core = coreRank(c), stages = Stage.values().length;
		Realm realm = Realm.byIndex(core / stages);
		Stage stage = Stage.byIndex(core % stages);
		double radius = radius(realm, stage);
		Vec3 heart = p.position().add(0, 1.0, 0);

		SpellFx.send(level, SpellFx.Kind.CORE_DETONATION, heart, Vec3.ZERO, (float) radius, 0, radius * 8);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, heart.x, heart.y, heart.z, 3, radius * 0.2, radius * 0.1, radius * 0.2, 0);
		level.sendParticles(ParticleTypes.FLASH, heart.x, heart.y, heart.z, 2, 0, 0, 0, 0);
		// Heard sixteen blocks off for each of its volume: a deeper core's from further away (a low realm's as loud as ever).
		level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, (float) Math.max(8, radius * 0.25), 0.5f);
		level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, (float) Math.max(6, radius * 0.2), 0.6f);

		// Everything living within reach: harmed by its distance and its realm, and thrown outward.
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(heart, heart).inflate(radius), e -> e != p && e.isAlive())) {
			double distance = victim.position().add(0, victim.getBbHeight() * 0.5, 0).distanceTo(heart);
			int rank = CultivatorNpc.rankOf(victim);
			int victimRealm = rank < 0 ? -1 : rank / Stage.values().length;
			double harm = harm(realm, stage, radius, distance, victimRealm);
			if (harm <= 0) continue;
			victim.invulnerableTime = 0;
			victim.hurt(level.damageSources().explosion(p, p), (float) harm);
			Vec3 away = victim.position().subtract(heart).normalize().scale(2.5 * (1 - distance / radius));
			victim.push(away.x, 0.4 + away.y * 0.5, away.z);
			victim.hurtMarked = true;
		}
		// The crater, a ragged bowl (craterRadius, craterDepth), carved by Devastation a share a tick, nearest the heart first, its
		// pieces flung, since a high realm's would stall the world carved at once.
		Devastation.crater(p, level, heart, craterRadius(realm, stage), craterDepth(realm, stage), Devastation.Floor.NONE);

		// The core lost: they wake at Foundation Building Grand Perfection, the methods understood no deeper than that: on the heaven
		// path, or on the ghost path if the core was its (its methods held there: GhostMethods#fallTo).
		// Not a creative player's, told by the abilities (isCreative is true for the game tests' mock players whatever their mode).
		if (!p.getAbilities().instabuild && !p.isSpectator()) {
			int fall = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
			if (path == TwoPaths.Path.GHOST) {
				GhostMethods.fallTo(c.netherworld(), fall);
			} else if (path == TwoPaths.Path.DEMON) { // a demon core, of the Demon Path's (DemonPath)
				DemonPath.fallTo(c, fall);
			} else {
				c.setState(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION, 0);
				HeavenMethods.clampTo(c.heaven(), fall);
			}
			c.setAbilityEnabled(Ability.CORE_DETONATION, false);
			CultivationManager.markDirty(p.server);
			p.invulnerableTime = 0;
			p.hurt(ModDamageTypes.coreDetonation(level), Float.MAX_VALUE);
		} else {
			c.setAbilityEnabled(Ability.CORE_DETONATION, false);
		}
		CultivationManager.sync(p);
	}

	/**
	 * On joining: a switch left on by a server that stopped mid-way (the core was breaking, or a tear held open) is put back to
	 * what is so now: no core is breaking yet, and one is in the gap by their own tear only while there.
	 */
	public static void onJoin(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		boolean changed = false;
		if (c.isAbilityEnabled(Ability.CORE_DETONATION) && !CHARGING.containsKey(player.getUUID())) {
			c.setAbilityEnabled(Ability.CORE_DETONATION, false);
			changed = true;
		}
		if (c.isAbilityEnabled(Ability.SPATIAL_TEAR) && !(ModDimensions.isSpatialGap(player.level().dimension())
				&& c.getPendingTrial() == SpatialTrialHandler.Route.TRAVEL)) {
			c.setAbilityEnabled(Ability.SPATIAL_TEAR, false);
			changed = true;
		}
		if (changed) CultivationManager.markDirty(player.server);
	}

	/** A player leaving while their core breaks doesn't escape it: it bursts as they go. */
	public static void onDisconnect(ServerPlayer player) {
		ASKED.remove(player.getUUID());
		if (CHARGING.remove(player.getUUID()) != null && player.isAlive()) detonate(player);
	}

	/** Server stop: nothing held. */
	public static void clear() {
		ASKED.clear();
		CHARGING.clear();
	}

	private CoreDetonation() {}
}
