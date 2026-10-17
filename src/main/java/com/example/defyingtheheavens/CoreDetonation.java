package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
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
 * armour (Blast Protection best) may live through it. The ground at its heart is blown away.
 */
public final class CoreDetonation {
	/** The second asking must come within this of the first, and no sooner than {@link #CONFIRM_MIN_TICKS} after it. */
	public static final int CONFIRM_TICKS = 100, CONFIRM_MIN_TICKS = 10;
	/** From the second asking to the blast. */
	public static final int CHARGE_TICKS = 60;
	/** The blast's reach by realm (Core Formation first), and a tenth more for each stage within it. */
	private static final double[] RADIUS = {10, 16, 22, 28};
	/** The harm at the heart: this many times the whole health of a body of the cultivator's own stage. */
	public static final double HEART_HARM = 1.1;
	/** Each realm a victim stands below the cultivator, the harm grows by the whole of it again; each realm above, it is this much. */
	public static final double ABOVE_FACTOR = 0.3;
	/** The crater: this share of the blast's reach. */
	private static final double CRATER_SHARE = 0.32;

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
		if (SoulProjection.isActive(id) || InnerRealm.isInside(player) || !player.isAlive() || player.isSpectator()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		long now = player.level().getGameTime();
		Long asked = ASKED.get(id);
		// A second asking hard on the first is a double click on the wheel, not a decision: it is let pass.
		if (asked != null && now - asked < CONFIRM_MIN_TICKS) return;
		ASKED.remove(id);
		if (asked == null || now - asked > CONFIRM_TICKS) {
			ASKED.put(id, now);
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
		player.serverLevel().getServer().getPlayerList().broadcastSystemMessage(
				Component.translatable(ModLang.MSG_CORE_BREAKING, player.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
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

	/** The blast's reach for a cultivator of {@code realm} and {@code stage}. */
	public static double radius(Realm realm, Stage stage) {
		int r = Mth.clamp(realm.ordinal() - Realm.CORE_FORMATION.ordinal(), 0, RADIUS.length - 1);
		return RADIUS[r] * (1 + 0.1 * stage.ordinal());
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
		Realm realm = c.getRealm();
		Stage stage = c.getStage();
		double radius = radius(realm, stage);
		Vec3 heart = p.position().add(0, 1.0, 0);

		SpellFx.send(level, SpellFx.Kind.CORE_DETONATION, heart, Vec3.ZERO, (float) radius, 0, radius * 8);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, heart.x, heart.y, heart.z, 3, radius * 0.2, radius * 0.1, radius * 0.2, 0);
		level.sendParticles(ParticleTypes.FLASH, heart.x, heart.y, heart.z, 2, 0, 0, 0, 0);
		level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 8.0f, 0.5f);
		level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 6.0f, 0.6f);

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
		crater(level, p, BlockPos.containing(heart), radius * CRATER_SHARE);

		// The core lost: they wake at Foundation Building Grand Perfection, the methods understood no deeper than that.
		// Not a creative player's, told by the abilities (isCreative is true for the game tests' mock players whatever their mode).
		if (!p.getAbilities().instabuild && !p.isSpectator()) {
			c.setState(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION, 0);
			HeavenMethods.clampTo(c.heaven(), PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION));
			c.setAbilityEnabled(Ability.CORE_DETONATION, false);
			CultivationManager.markDirty(p.server);
			p.invulnerableTime = 0;
			p.hurt(ModDamageTypes.coreDetonation(level), Float.MAX_VALUE);
		} else {
			c.setAbilityEnabled(Ability.CORE_DETONATION, false);
		}
		CultivationManager.sync(p);
	}

	/** Blows away the ground round {@code centre} out to {@code r}, a ragged bowl, nothing dropped: what a spell may carve, no more. */
	private static void crater(ServerLevel level, ServerPlayer p, BlockPos centre, double r) {
		int reach = (int) Math.ceil(r);
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dy = -reach; dy <= reach; dy++) {
				for (int dz = -reach; dz <= reach; dz++) {
					double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz);
					double edge = r * (0.8 + 0.2 * Mth.frac(Math.sin((centre.getX() + dx) * 12.9898 + (centre.getZ() + dz) * 78.233) * 43758.5453));
					if (d > edge) continue;
					BlockPos pos = centre.offset(dx, dy, dz);
					if (!HeavenSpells.carvable(level, pos, p)) continue;
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
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
