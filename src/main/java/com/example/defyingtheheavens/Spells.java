package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Casting spells (see Spell). The spell wheel (held open by its key) readies a spell with a click, or casts one at once with a
 * right click; the cast key casts the readied one. A cast needs the spell's layer, its ghostly qi, and its cooldown past; not
 * on the Soul River, and not while dispersed.
 */
public final class Spells {
	public static final int WHEEL_SLOTS = 8;
	/** Ghost Fire: this many blows, within this long, set what they strike burning this long; each such blow costs this much. */
	public static final int GHOST_FIRE_BLOWS = 3, GHOST_FIRE_ARMED_TICKS = 20 * 20, GHOST_FIRE_BURN_TICKS = 4 * 20;
	public static final double GHOST_FIRE_PER_HIT = 3;
	/** The burn: this much a second, this many times over on the dead. */
	public static final float GHOST_FIRE_DAMAGE = 1.0f, GHOST_FIRE_DEAD_FACTOR = 3.0f;
	/** From the seventh layer of the Ghost Fire Scripture the fire leaps to a second foe this close. */
	public static final int GHOST_FIRE_LEAP_LAYER = 7;
	public static final double GHOST_FIRE_LEAP_RADIUS = 4;
	/** Ghost Fire Bolt: reach and harm (the dead's), and it sets the struck burning. */
	public static final double BOLT_RANGE = 24;
	public static final float BOLT_DAMAGE = 6.0f;
	/** Soul Dash: the rush's speed (about eight blocks), and how long falls do no harm after it. */
	public static final double DASH_SPEED = 1.6;
	public static final int DASH_SAFE_TICKS = 40;
	/** Ghostly Wail: its reach, the harm to the dead, and how long it slows and weakens. */
	public static final double WAIL_RADIUS = 8;
	public static final float WAIL_DEAD_DAMAGE = 4.0f;
	public static final int WAIL_TICKS = 5 * 20;

	private static final Map<UUID, Map<Spell, Long>> COOLDOWNS = new HashMap<>();
	/** Ghost Fire on the hands: blows left, and until when. */
	private static final Map<UUID, long[]> FIRE = new HashMap<>();
	private static final Map<UUID, Long> SAFE_FALL = new HashMap<>();

	/** Casts {@code spell} (null: the readied one). */
	public static void cast(ServerPlayer p, Spell spell) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (spell == null) spell = s.readiedSpell();
		if (spell == null) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SPELL_NONE_READY), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(p);
		if (!p.isAlive() || p.isSpectator() || s.reforming() || !spell.isKnown(c)) return;
		if (ModDimensions.isPassage(p.level().dimension())) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SPELL_NOT_HERE), true);
			return;
		}
		if (spell == Spell.SOUL_DASH && p.hasEffect(ModEffects.QUELLED)) { // bound where it stands (QuelledEffect)
			p.displayClientMessage(Component.translatable(ModLang.MSG_QUELLED_HELD), true);
			return;
		}
		long now = p.level().getGameTime();
		long readyAt = cooldowns(p).getOrDefault(spell, 0L);
		if (now < readyAt) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SPELL_COOLING, spell.getDisplayName(), (readyAt - now + 19) / 20), true);
			return;
		}
		boolean free = p.getAbilities().instabuild;
		if (!free && s.ghostQi() < spell.cost()) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SPELL_NO_QI, spell.getDisplayName()), true);
			return;
		}
		if (!free) s.setGhostQi(s.ghostQi() - spell.cost());
		cooldowns(p).put(spell, now + spell.cooldownTicks());
		switch (spell) {
			case GHOST_FIRE -> ghostFire(p);
			case GHOST_FIRE_BOLT -> bolt(p);
			case SOUL_DASH -> dash(p);
			case GHOSTLY_WAIL -> wail(p, s);
		}
		CultivationManager.markDirty(p.server);
		NetherworldManager.refresh(p);
		sync(p);
	}

	private static Map<Spell, Long> cooldowns(ServerPlayer p) {
		return COOLDOWNS.computeIfAbsent(p.getUUID(), id -> new EnumMap<>(Spell.class));
	}

	/** Ticks until {@code spell} can be cast again by {@code p}. */
	public static int cooldownLeft(ServerPlayer p, Spell spell) {
		return (int) Math.max(0, cooldowns(p).getOrDefault(spell, 0L) - p.level().getGameTime());
	}

	// --- The spells ---

	private static void ghostFire(ServerPlayer p) {
		FIRE.put(p.getUUID(), new long[]{GHOST_FIRE_BLOWS, p.level().getGameTime() + GHOST_FIRE_ARMED_TICKS});
		ServerLevel level = p.serverLevel();
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.6f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 1.2f);
		// About the hands, a little ahead and below the eyes.
		Vec3 hands = p.position().add(p.getViewVector(1.0f).multiply(0.7, 0, 0.7)).add(0, 0.9, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hands.x, hands.y, hands.z, 18, 0.3, 0.2, 0.3, 0.02);
		p.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_FIRE_ARMED, GHOST_FIRE_BLOWS), true);
	}

	/**
	 * A blow struck by {@code attacker} landed on {@code target}: with Ghost Fire on the hands (and the ghostly qi for it), it
	 * sets the target burning, and from the seventh layer a second foe nearby.
	 */
	public static void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
		long[] fire = FIRE.get(attacker.getUUID());
		if (fire == null) return;
		if (attacker.level().getGameTime() > fire[1] || fire[0] <= 0) {
			FIRE.remove(attacker.getUUID());
			return;
		}
		NetherworldState s = CultivationManager.get(attacker).netherworld();
		boolean free = attacker.getAbilities().instabuild;
		if (!free && s.ghostQi() < GHOST_FIRE_PER_HIT) {
			FIRE.remove(attacker.getUUID());
			attacker.displayClientMessage(Component.translatable(ModLang.MSG_SPELL_NO_QI, Spell.GHOST_FIRE.getDisplayName()), true);
			return;
		}
		if (!free) s.setGhostQi(s.ghostQi() - GHOST_FIRE_PER_HIT);
		ignite(target);
		if (s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) >= GHOST_FIRE_LEAP_LAYER) {
			for (LivingEntity other : attacker.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(GHOST_FIRE_LEAP_RADIUS),
					e -> e != target && e != attacker && e.isAlive() && !(e instanceof Player player && player.isSpectator()))) {
				ignite(other);
				if (attacker.level() instanceof ServerLevel level) trail(level, target.position().add(0, 1, 0), other.position().add(0, 1, 0));
				break;
			}
		}
		if (--fire[0] <= 0) FIRE.remove(attacker.getUUID());
		NetherworldManager.refresh(attacker);
	}

	private static void ignite(LivingEntity target) {
		target.addEffect(new MobEffectInstance(ModEffects.GHOST_FIRE, GHOST_FIRE_BURN_TICKS, 0));
		target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8f, 1.5f);
	}

	private static void bolt(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		Vec3 from = p.getEyePosition();
		Vec3 look = p.getViewVector(1.0f);
		Vec3 to = from.add(look.scale(BOLT_RANGE));
		HitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		if (block.getType() != HitResult.Type.MISS) to = block.getLocation();
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, from, to, new AABB(from, to).inflate(1.0),
				e -> e instanceof LivingEntity living && living.isAlive() && e != p && !e.isSpectator());
		Vec3 end = hit != null ? hit.getLocation() : to;
		// From an arm's length out, so the caster's own view isn't filled with its flames.
		trail(level, from.add(look.scale(1.5)), end);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 0.6f);
		if (hit != null && hit.getEntity() instanceof LivingEntity target) {
			target.hurt(ModDamageTypes.ghostly(level, p), BOLT_DAMAGE);
			ignite(target);
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, end.x, end.y, end.z, 25, 0.3, 0.3, 0.3, 0.08);
		}
	}

	/** A streak of cold fire from {@code a} to {@code b}. */
	private static void trail(ServerLevel level, Vec3 a, Vec3 b) {
		double length = a.distanceTo(b);
		for (double d = 0; d < length; d += 0.4) {
			Vec3 at = a.add(b.subtract(a).scale(d / Math.max(length, 1.0e-6)));
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
		}
	}

	private static void dash(ServerPlayer p) {
		Vec3 look = p.getViewVector(1.0f);
		Vec3 along = new Vec3(look.x, Math.max(-0.2, Math.min(0.35, look.y)), look.z).normalize();
		p.setDeltaMovement(along.scale(DASH_SPEED).add(0, 0.12, 0));
		p.hurtMarked = true; // sends the new motion to the client, which moves the player
		p.fallDistance = 0;
		SAFE_FALL.put(p.getUUID(), p.level().getGameTime() + DASH_SAFE_TICKS);
		ServerLevel level = p.serverLevel();
		level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.3, 0.6, 0.3, 0.02);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.4f);
	}

	/** Whether a fall should do {@code p} no harm: just after a Soul Dash, or on Yin Wind Steps. */
	public static boolean fallIsSafe(ServerPlayer p) {
		Long until = SAFE_FALL.get(p.getUUID());
		return until != null && p.level().getGameTime() <= until || CultivationManager.get(p).isAbilityActive(Ability.YIN_WIND_STEPS);
	}

	private static void wail(ServerPlayer p, NetherworldState s) {
		ServerLevel level = p.serverLevel();
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 2.0f, 0.45f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 1.3f);
		level.sendParticles(ParticleTypes.SONIC_BOOM, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0, 0, 0, 0);
		for (int i = 0; i < 48; i++) {
			double a = Math.PI * 2 * i / 48;
			level.sendParticles(ParticleTypes.SCULK_SOUL, p.getX() + Math.cos(a) * 1.5, p.getY() + 1.0, p.getZ() + Math.sin(a) * 1.5, 0,
					Math.cos(a), 0.05, Math.sin(a), 0.25);
		}
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(WAIL_RADIUS),
				e -> e != p && e.isAlive() && e.distanceToSqr(p) <= WAIL_RADIUS * WAIL_RADIUS && !(e instanceof Player player && player.isSpectator()))) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WAIL_TICKS, 1));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WAIL_TICKS, 0));
			Vec3 away = target.position().subtract(p.position()).multiply(1, 0, 1);
			if (away.lengthSqr() > 1.0e-4) target.knockback(1.0, -away.x, -away.z);
			if (GhostSystem.isDead(target)) target.hurt(ModDamageTypes.ghostly(level, p), WAIL_DEAD_DAMAGE); // ghosts and the dead's kind
		}
	}

	// --- Readying, arranging, practising ---

	/** Readies {@code spell} for the cast key (null clears it). */
	public static void ready(ServerPlayer p, Spell spell) {
		PlayerCultivation c = CultivationManager.get(p);
		if (spell != null && !spell.isKnown(c)) return;
		c.netherworld().setReadiedSpell(spell);
		CultivationManager.markDirty(p.server);
		if (spell != null) p.displayClientMessage(Component.translatable(ModLang.MSG_SPELL_READIED, spell.getDisplayName()), true);
		sync(p);
	}

	/** Puts a spell in a slot of the spell wheel (null empties it). Only a spell known can be placed. */
	public static void setSlot(ServerPlayer p, int slot, Spell spell) {
		PlayerCultivation c = CultivationManager.get(p);
		if (slot < 0 || slot >= WHEEL_SLOTS || spell != null && !spell.isKnown(c)) return;
		c.netherworld().setSpellSlot(slot, spell);
		CultivationManager.markDirty(p.server);
		sync(p);
	}

	/** Makes {@code method} the one meditation cultivates. */
	public static void practise(ServerPlayer p, GhostMethod method) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (method == null || !s.knows(method)) return;
		s.setActiveMethod(method);
		CultivationManager.markDirty(p.server);
		p.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_PRACTISED, method.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
		sync(p);
	}

	/** Tells the client its methods, active method, readied spell, spell wheel and cooldowns. */
	public static void sync(ServerPlayer p) {
		ModPackets.sendMethods(p, CultivationManager.get(p).netherworld(), spell -> cooldownLeft(p, spell));
	}

	public static void forget(UUID id) {
		COOLDOWNS.remove(id);
		FIRE.remove(id);
		SAFE_FALL.remove(id);
	}

	public static void clear() {
		COOLDOWNS.clear();
		FIRE.clear();
		SAFE_FALL.clear();
	}

	private Spells() {}
}
