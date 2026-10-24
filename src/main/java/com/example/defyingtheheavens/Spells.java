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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Casting spells (see Spell). The spell wheel (held open by its key) readies a spell with a click, or casts one at once with a
 * right click; the cast key casts the readied one. A cast needs the spell's layer, its ghostly qi, and its cooldown past (a
 * player's only while the gamerule spellCooldowns is on: off by default since 2026-10-24, {@link #cooldownsHeld}); not
 * on the Soul River, and not while dispersed. A spell of both paths (TwoPaths, HeavenDemonSpells) needs the heaven path and the
 * Demon Path at its realm, and costs qi alone; the ghost foundations' great spells from Dao Integration on are GhostSpells'.
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
	/** Yellow Springs Tide: how far ahead and how wide (the cosine of half its angle), the harm to the dead, how long it slows. */
	public static final double TIDE_RANGE = 7, TIDE_CONE = 0.5;
	public static final float TIDE_DEAD_DAMAGE = 6.0f;
	public static final int TIDE_TICKS = 4 * 20;
	/** Marrow Ward: how long the bones hold (Resistance II and four hearts of absorption). */
	public static final int WARD_TICKS = 10 * 20;
	/** Ghost Core Burst: its reach and harm (the dead's), and it sets the struck burning. */
	public static final double BURST_RADIUS = 5;
	public static final float BURST_DAMAGE = 8.0f;
	/** King's Decree: its reach, and how long the hostile are weakened, slowed and shown. */
	public static final double DECREE_RADIUS = 16;
	public static final int DECREE_TICKS = 8 * 20;
	/** Soul Rend: its reach and harm, to every being along the beam. */
	public static final double REND_RANGE = 32;
	public static final float REND_DAMAGE = 16.0f;
	/** Nine Nether Step: how far, and how long falls do no harm after it (a step off a ledge may fall a long way). */
	public static final double STEP_RANGE = 24;
	public static final int STEP_SAFE_TICKS = 5 * 20;
	/** Yama's Verdict: reach and harm, twice over on one below half its health. */
	public static final double VERDICT_RANGE = 32;
	public static final float VERDICT_DAMAGE = 20.0f;
	/** Chains of Hell: reach, and how long the chained are held. */
	public static final double CHAINS_RADIUS = 12;
	public static final int CHAINS_TICKS = 3 * 20;
	/** Six Paths Annihilation: reach and harm. */
	public static final double ANNIHILATION_RADIUS = 12;
	public static final float ANNIHILATION_DAMAGE = 40.0f;

	/** Ghost Fire on the hands: blows left, and until when. */
	private static final Map<UUID, long[]> FIRE = new HashMap<>();
	private static final Map<UUID, Long> SAFE_FALL = new HashMap<>();

	/** Casts {@code spell} (null: the readied one). */
	public static void cast(ServerPlayer p, Spell spell) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		if (spell == null) spell = s.readiedSpell();
		if (spell == null) {
			HeavenSpells.tell(p, Component.translatable(ModLang.MSG_SPELL_NONE_READY));
			return;
		}
		PlayerCultivation c = CultivationManager.get(p);
		if (!p.isAlive() || p.isSpectator() || s.reforming() || !spell.isKnown(c)) return;
		// A projected soul touches nothing (SoulProjection): its one act is to enter a being. In a person's body it casts as that body.
		if (SoulProjection.isSoul(p)) {
			HeavenSpells.tell(p, Component.translatable(ModLang.MSG_SOUL_BUSY));
			return;
		}
		if (ModDimensions.isPassage(p.level().dimension())) {
			HeavenSpells.tell(p, Component.translatable(ModLang.MSG_SPELL_NOT_HERE));
			return;
		}
		if ((spell == Spell.SOUL_DASH || spell == Spell.NINE_NETHER_STEP) && p.hasEffect(ModEffects.QUELLED)) { // bound where it stands (QuelledEffect)
			HeavenSpells.tell(p, Component.translatable(ModLang.MSG_QUELLED_HELD));
			return;
		}
		if (spell == Spell.NINE_NETHER_STEP && EarthSpells.spaceLocked(p)) { // space locked round it (Earth Veins Lock the Void): no step through it
			HeavenSpells.tell(p, Component.translatable(ModLang.MSG_SPACE_LOCKED));
			return;
		}
		long now = p.level().getGameTime();
		long readyAt = cooldowns(p).getOrDefault(spell, 0L);
		if (now < readyAt && cooldownsHeld(p)) {
			HeavenSpells.tell(p, Component.translatable(ModLang.MSG_SPELL_COOLING, spell.getDisplayName(), (readyAt - now + 19) / 20));
			return;
		}
		// A yang bomb set is set off for nothing (FireSpells#canCast), however little qi is left.
		boolean free = HeavenSpells.creative(p) || spell == Spell.REMOTE_YANG_BOMB && FireSpells.hasBomb(p);
		if (!free && (s.ghostQi() < spell.cost() || c.getQi() < spell.qiCost())) {
			p.displayClientMessage(Component.translatable(spell.isHeaven() || spell.isDual() ? ModLang.MSG_SPELL_NO_HEAVEN_QI : ModLang.MSG_SPELL_NO_QI,
					spell.getDisplayName()), true);
			return;
		}
		// A heaven method's spell: its element's own kit (HeavenSpells), refused (and nothing paid) where it can't take hold; one of both paths
		// as well (a seal with nothing to seal).
		if (spell.isHeaven() && !HeavenSpells.canCast(p, spell) || spell.isDual() && !HeavenDemonSpells.canCast(p, spell)) return;
		if (!free) {
			s.setGhostQi(s.ghostQi() - spell.cost());
			if (spell.qiCost() > 0) c.setQi(c.getQi() - spell.qiCost());
		}
		cooldowns(p).put(spell, now + spell.cooldownTicks());
		RaceGifts.breakIllusion(p); // a fox unseen is seen as it casts
		perform(p, spell, s);
		CultivationManager.markDirty(p.server);
		NetherworldManager.refresh(p);
		if (spell.qiCost() > 0 && !free) CultivationManager.sync(p); // the qi pool's share
		sync(p);
	}

	/**
	 * {@code npc} casts {@code spell} (CultivatorSpells chose it, facing what it casts at): as a player casts, but from its own qi (a
	 * ghost path's spell's ghostly qi out of it too), on its own cooldowns, and never in a passage (the Spatial Gap, the Soul River).
	 * Refused where the spell can't take hold, nothing paid. @return whether it was cast
	 */
	public static boolean castBy(CultivatorNpc npc, Spell spell) {
		if (spell == null || !npc.isAlive() || npc.isRemoved() || ModDimensions.isPassage(npc.level().dimension())) return false;
		PlayerCultivation c = npc.cultivation();
		long now = npc.level().getGameTime();
		if (now < cooldowns(npc).getOrDefault(spell, 0L)) return false;
		double cost = spell.qiCost() + spell.cost();
		if (c.getQi() < cost || spell.isHeaven() && !HeavenSpells.canCast(npc, spell)) return false;
		c.setQi(c.getQi() - cost);
		cooldowns(npc).put(spell, now + spell.cooldownTicks());
		perform(npc, spell, c.netherworld());
		return true;
	}

	/** What a cast does once paid for: the hooks that hear of it and its seal, then the spell itself (a heaven one: its element's kit). */
	private static void perform(LivingEntity p, Spell spell, NetherworldState s) {
		HeavenSpells.onCast(p, spell); // what lingers that hears of a cast (a mist's hiding broken)
		HeavenSpells.castSeal(p, spell); // an element spell's seal, turning under its caster
		switch (spell) {
			case GHOST_FIRE -> ghostFire(p);
			case GHOST_FIRE_BOLT -> bolt(p);
			case SOUL_DASH -> dash(p);
			case GHOSTLY_WAIL -> wail(p, s);
			case YELLOW_SPRINGS_TIDE -> tide(p);
			case MARROW_WARD -> marrowWard(p);
			case GHOST_CORE_BURST -> coreBurst(p);
			case KINGS_DECREE -> decree(p);
			case SOUL_REND -> soulRend(p);
			case NINE_NETHER_STEP -> netherStep(p);
			case YAMA_VERDICT -> verdict(p);
			case HELL_CHAINS -> chains(p);
			case SAMSARA_WHEEL -> samsara(p, s);
			case SIX_PATHS_ANNIHILATION -> annihilation(p);
			case PROCESSION_OF_TEN_THOUSAND_GHOSTS, GATE_OF_TEN_THOUSAND_GHOSTS, RIFT_TO_THE_NINE_NETHERS, BRIDGE_OF_NO_RETURN -> GhostSpells.cast(p, spell);
			default -> {
				if (spell.isDual()) HeavenDemonSpells.cast(p, spell);
				else HeavenSpells.cast(p, spell);
			}
		}
	}

	/**
	 * {@code p}'s cooldowns: kept on its own record (NetherworldState#spellReady: saved, a soul's in another's body its own); a
	 * cultivator's on its own (saved with it).
	 */
	private static Map<Spell, Long> cooldowns(LivingEntity p) {
		if (p instanceof ServerPlayer player) return CultivationManager.own(player).netherworld().spellReady();
		if (p instanceof CultivatorNpc npc) return npc.cultivation().netherworld().spellReady();
		return new java.util.EnumMap<>(Spell.class);
	}

	/** Whether {@code p}'s spells wait out their cooldowns: a cultivator's always, a player's while the gamerule spellCooldowns is on. */
	public static boolean cooldownsHeld(LivingEntity p) {
		return !(p instanceof ServerPlayer player) || player.serverLevel().getGameRules().getBoolean(ModGameRules.SPELL_COOLDOWNS);
	}

	/** Ticks until {@code spell} can be cast again by {@code p}. */
	public static int cooldownLeft(LivingEntity p, Spell spell) {
		return (int) Math.max(0, cooldowns(p).getOrDefault(spell, 0L) - p.level().getGameTime());
	}

	/** Takes {@code ticks} off every cooldown {@code p} has running (a heaven spell's rain: HeavenSpells). */
	public static void hasten(LivingEntity p, int ticks) {
		cooldowns(p).replaceAll((spell, readyAt) -> readyAt - ticks);
		if (p instanceof ServerPlayer player) CultivationManager.markDirty(player.server);
	}

	/** Sets {@code spell}'s cooldown to end {@code ticks} from now (a spell that is ready again sooner when it goes unused). */
	public static void setCooldown(LivingEntity p, Spell spell, int ticks) {
		cooldowns(p).put(spell, p.level().getGameTime() + Math.max(0, ticks));
		if (p instanceof ServerPlayer player) CultivationManager.markDirty(player.server);
	}

	/** No fall harm for {@code p} for the next {@code ticks} (a heaven spell's leap, haul or swap); a longer one already set stays. */
	public static void safeFall(LivingEntity p, int ticks) {
		SAFE_FALL.merge(p.getUUID(), p.level().getGameTime() + ticks, Math::max);
	}

	// --- The spells ---

	private static void ghostFire(LivingEntity p) {
		FIRE.put(p.getUUID(), new long[]{GHOST_FIRE_BLOWS, p.level().getGameTime() + GHOST_FIRE_ARMED_TICKS});
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.6f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 1.2f);
		// About the hands, a little ahead and below the eyes.
		Vec3 hands = p.position().add(p.getViewVector(1.0f).multiply(0.7, 0, 0.7)).add(0, 0.9, 0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hands.x, hands.y, hands.z, 18, 0.3, 0.2, 0.3, 0.02);
		HeavenSpells.tell(p, Component.translatable(ModLang.MSG_GHOST_FIRE_ARMED, GHOST_FIRE_BLOWS));
	}

	/**
	 * A blow struck by {@code attacker} landed on {@code target}: with Ghost Fire on the hands (and the ghostly qi for it), it
	 * sets the target burning, and from the seventh layer a second foe nearby.
	 */
	public static void onMeleeHit(ServerPlayer attacker, LivingEntity target) {
		// What the heaven spells do with a blow (HeavenSpells#onMelee: Flame Fist's fire, Steel Hurricane's cut), once the blow itself
		// has landed: dealt inside it (before the sweep of 2026-10-23) their harm opened the target's guard first, and the blow lost as
		// much and its knockback, so they added nothing.
		net.minecraft.server.MinecraftServer server = attacker.getServer();
		if (server != null) {
			server.tell(new net.minecraft.server.TickTask(server.getTickCount(), () -> {
				if (attacker.isAlive() && target.isAlive()) HeavenSpells.onMelee(attacker, target);
			}));
		}
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
			// To a second foe (HeavenSpells#foe: never a pet, a sect fellow, a player one may not harm), not to whoever stands near.
			for (LivingEntity other : attacker.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(GHOST_FIRE_LEAP_RADIUS),
					e -> e != target && e != attacker && e.isAlive() && HeavenSpells.foe(attacker, e))) {
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

	private static void bolt(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 from = p.getEyePosition();
		Vec3 look = p.getViewVector(1.0f);
		Vec3 to = from.add(look.scale(BOLT_RANGE));
		HitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		if (block.getType() != HitResult.Type.MISS) to = block.getLocation();
		// Past whom the caster's spells spare (HeavenSpells#spared: its pets, its sect fellows, a player it may not harm).
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, from, to, new AABB(from, to).inflate(1.0),
				e -> e instanceof LivingEntity living && living.isAlive() && e != p && !e.isSpectator() && !HeavenSpells.spared(p, living));
		Vec3 end = hit != null ? hit.getLocation() : to;
		// From an arm's length out, so the caster's own view isn't filled with its flames.
		trail(level, from.add(look.scale(1.5)), end);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 0.6f);
		if (hit != null && hit.getEntity() instanceof LivingEntity target) {
			// Burning only where it struck home (not a blow turned aside: PvP off, a ward).
			if (target.hurt(ModDamageTypes.ghostly(level, p), BOLT_DAMAGE)) ignite(target);
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

	private static void dash(LivingEntity p) {
		Vec3 look = p.getViewVector(1.0f);
		Vec3 along = new Vec3(look.x, Math.max(-0.2, Math.min(0.35, look.y)), look.z).normalize();
		p.setDeltaMovement(along.scale(DASH_SPEED).add(0, 0.12, 0));
		p.hurtMarked = true; // sends the new motion to the client, which moves the player
		p.fallDistance = 0;
		SAFE_FALL.put(p.getUUID(), p.level().getGameTime() + DASH_SAFE_TICKS);
		ServerLevel level = HeavenSpells.level(p);
		level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.3, 0.6, 0.3, 0.02);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.4f);
	}

	/** Whether a fall should do {@code p} no harm: just after a Soul Dash, or on Yin Wind Steps. */
	public static boolean fallIsSafe(LivingEntity p) {
		Long until = SAFE_FALL.get(p.getUUID());
		return until != null && p.level().getGameTime() <= until || p instanceof ServerPlayer player && CultivationManager.get(player).isAbilityActive(Ability.YIN_WIND_STEPS);
	}

	private static void wail(LivingEntity p, NetherworldState s) {
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 2.0f, 0.45f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0f, 1.3f);
		level.sendParticles(ParticleTypes.SONIC_BOOM, p.getX(), p.getY() + 1.2, p.getZ(), 1, 0, 0, 0, 0);
		for (int i = 0; i < 48; i++) {
			double a = Math.PI * 2 * i / 48;
			level.sendParticles(ParticleTypes.SCULK_SOUL, p.getX() + Math.cos(a) * 1.5, p.getY() + 1.0, p.getZ() + Math.sin(a) * 1.5, 0,
					Math.cos(a), 0.05, Math.sin(a), 0.25);
		}
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(WAIL_RADIUS),
				e -> !spared(p, e) && e.distanceToSqr(p) <= WAIL_RADIUS * WAIL_RADIUS)) {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, WAIL_TICKS, 1));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WAIL_TICKS, 0));
			Vec3 away = target.position().subtract(p.position()).multiply(1, 0, 1);
			if (away.lengthSqr() > 1.0e-4) target.knockback(1.0, -away.x, -away.z);
			if (GhostSystem.isDead(target)) target.hurt(ModDamageTypes.ghostly(level, p), WAIL_DEAD_DAMAGE); // ghosts and the dead's kind
		}
	}

	// --- The deeper foundations' spells ---

	/**
	 * Spared by a caster's spells, as by the heaven spells (HeavenSpells#spared): the caster, spectators, its own tamed creatures, a
	 * player it may not harm (PvP off, a team) and its sect's fellows. No hold, push or slowing reaches them either (only the harm
	 * minded PvP before the 2026-10-20 sweep).
	 */
	private static boolean spared(LivingEntity p, LivingEntity e) {
		return HeavenSpells.spared(p, e);
	}

	/** A foe of the caster's: a monster, or anything set on it (a mob or a cultivator with it as its target). */
	private static boolean foe(LivingEntity p, LivingEntity e) {
		if (spared(p, e)) return false;
		return e instanceof net.minecraft.world.entity.monster.Enemy || e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() == p;
	}

	private static java.util.List<LivingEntity> around(LivingEntity p, double radius, java.util.function.Predicate<LivingEntity> which) {
		return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(radius),
				e -> e.distanceToSqr(p) <= radius * radius && which.test(e));
	}

	private static void push(LivingEntity p, LivingEntity target, double strength) {
		Vec3 away = target.position().subtract(p.position()).multiply(1, 0, 1);
		if (away.lengthSqr() > 1.0e-4) target.knockback(strength, -away.x, -away.z);
	}

	private static final org.joml.Vector3f SPRINGS_YELLOW = new org.joml.Vector3f(0.86f, 0.72f, 0.28f);
	private static final org.joml.Vector3f BONE_WHITE = new org.joml.Vector3f(0.93f, 0.9f, 0.8f);
	private static final org.joml.Vector3f BLOOD_RED = new org.joml.Vector3f(0.55f, 0.04f, 0.06f);
	private static final org.joml.Vector3f CHAIN_GREY = new org.joml.Vector3f(0.3f, 0.3f, 0.34f);

	/** Yellow Springs Tide: a surge of the springs' yellow water rushes out ahead, driving back and slowing all in it. */
	private static void tide(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 look = p.getViewVector(1.0f).multiply(1, 0, 1);
		if (look.lengthSqr() < 1.0e-4) look = Vec3.directionFromRotation(0, p.getYRot());
		look = look.normalize();
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.4f, 0.6f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.8f);
		net.minecraft.core.particles.DustParticleOptions water = new net.minecraft.core.particles.DustParticleOptions(SPRINGS_YELLOW, 1.6f);
		for (double d = 1; d <= TIDE_RANGE; d += 0.5) {
			for (double a = -0.9; a <= 0.9; a += 0.3) {
				Vec3 dir = look.yRot((float) a);
				Vec3 at = p.position().add(dir.scale(d)).add(0, 0.3 + 0.15 * d, 0);
				level.sendParticles(water, at.x, at.y, at.z, 1, 0.15, 0.25, 0.15, 0);
				if (((int) (d * 2)) % 3 == 0) level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 2, 0.2, 0.1, 0.2, 0.1);
			}
		}
		Vec3 ahead = look;
		for (LivingEntity target : around(p, TIDE_RANGE, e -> !spared(p, e))) {
			Vec3 to = target.position().subtract(p.position()).multiply(1, 0, 1);
			if (to.lengthSqr() > 1.0e-4 && to.normalize().dot(ahead) < TIDE_CONE) continue;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TIDE_TICKS, 1));
			push(p, target, 1.5);
			if (GhostSystem.isDead(target)) target.hurt(ModDamageTypes.ghostly(level, p), TIDE_DEAD_DAMAGE);
		}
	}

	/** Marrow Ward: the bones steeped in the Yellow Springs hold like iron for a while. */
	private static void marrowWard(LivingEntity p) {
		p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, WARD_TICKS, 1));
		p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, WARD_TICKS, 1));
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BONE_BLOCK_PLACE, SoundSource.PLAYERS, 1.2f, 0.6f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ARMOR_EQUIP_NETHERITE, SoundSource.PLAYERS, 1.0f, 0.7f);
		net.minecraft.core.particles.DustParticleOptions bone = new net.minecraft.core.particles.DustParticleOptions(BONE_WHITE, 1.2f);
		for (int i = 0; i < 24; i++) {
			double a = Math.PI * 2 * i / 24;
			level.sendParticles(bone, p.getX() + Math.cos(a) * 0.8, p.getY() + 0.2 + (i % 6) * 0.3, p.getZ() + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
		}
	}

	/** Ghost Core Burst: the core flares, and everything near takes its cold fire. */
	private static void coreBurst(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.4f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 0.5f);
		level.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
		for (int i = 0; i < 80; i++) {
			double a = Math.PI * 2 * i / 80, b = (i % 8 - 3.5) * 0.35;
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 0, Math.cos(a) * Math.cos(b), Math.sin(b), Math.sin(a) * Math.cos(b), 0.45);
		}
		for (LivingEntity target : around(p, BURST_RADIUS, e -> !spared(p, e))) {
			target.hurt(ModDamageTypes.ghostly(level, p), BURST_DAMAGE);
			ignite(target);
			push(p, target, 1.0);
		}
	}

	/** King's Decree: a Ghost King's word, and every foe near is cowed: weakened, slowed, and shown for all to see. */
	private static void decree(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 0.5f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0f, 0.6f);
		level.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 2.2, p.getZ(), 60, 0.6, 0.3, 0.6, 0.5);
		for (LivingEntity target : around(p, DECREE_RADIUS, e -> foe(p, e))) {
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DECREE_TICKS, 1));
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DECREE_TICKS, 2));
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, DECREE_TICKS, 0));
			level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(), 6, 0.2, 0.1, 0.2, 0.02);
		}
	}

	/** The end of a beam from the eyes {@code range} along the look, stopped by the first block. */
	private static Vec3 beamEnd(LivingEntity p, double range) {
		Vec3 from = p.getEyePosition();
		Vec3 to = from.add(p.getViewVector(1.0f).scale(range));
		HitResult block = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
		return block.getType() == HitResult.Type.MISS ? to : block.getLocation();
	}

	/** Soul Rend: a beam of the Nine Nethers' dark through every being in its line. */
	private static void soulRend(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 from = p.getEyePosition();
		Vec3 to = beamEnd(p, REND_RANGE);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.5f);
		Vec3 look = p.getViewVector(1.0f);
		double length = from.distanceTo(to);
		for (double d = 1.5; d < length; d += 0.5) {
			Vec3 at = from.add(look.scale(d));
			level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
			if (((int) (d * 2)) % 4 == 0) level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.01);
		}
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0), e -> !spared(p, e))) {
			if (target.getBoundingBox().inflate(0.4).clip(from, to).isEmpty()) continue;
			target.hurt(ModDamageTypes.ghostly(level, p), REND_DAMAGE);
			level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
		}
	}

	/** Nine Nether Step: a step through the dark, to where the caster looks (as far as {@link #STEP_RANGE}, short of a wall). */
	private static void netherStep(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 look = p.getViewVector(1.0f);
		Vec3 eye = beamEnd(p, STEP_RANGE).subtract(look.scale(0.6));
		Vec3 feet = eye.subtract(0, p.getEyeHeight(), 0);
		// Back along the way until the body fits, never past where it started.
		Vec3 start = p.position();
		for (int i = 0; i < 64 && !level.noCollision(p, p.getBoundingBox().move(feet.subtract(start))); i++) {
			feet = feet.subtract(look.scale(0.5));
			if (feet.distanceToSqr(start) < 1) {
				feet = start;
				break;
			}
		}
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1, start.z, 30, 0.3, 0.6, 0.3, 0.05);
		level.sendParticles(ParticleTypes.SCULK_SOUL, start.x, start.y + 1, start.z, 10, 0.3, 0.6, 0.3, 0.02);
		p.teleportTo(feet.x, feet.y, feet.z);
		p.fallDistance = 0;
		SAFE_FALL.put(p.getUUID(), level.getGameTime() + STEP_SAFE_TICKS);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, feet.x, feet.y + 1, feet.z, 30, 0.3, 0.6, 0.3, 0.05);
		level.playSound(null, start.x, start.y, start.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.5f);
		level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.6f);
	}

	/** Yama's Verdict: one foe judged and struck, and one already failing struck twice as hard. */
	private static void verdict(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		Vec3 from = p.getEyePosition();
		Vec3 to = beamEnd(p, VERDICT_RANGE);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, p, from, to, new AABB(from, to).inflate(1.0),
				e -> e instanceof LivingEntity living && !spared(p, living));
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.2f, 0.5f);
		if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) return;
		boolean failing = target.getHealth() < target.getMaxHealth() / 2;
		target.hurt(ModDamageTypes.ghostly(level, p), failing ? VERDICT_DAMAGE * 2 : VERDICT_DAMAGE);
		net.minecraft.core.particles.DustParticleOptions blood = new net.minecraft.core.particles.DustParticleOptions(BLOOD_RED, 2.0f);
		for (double y = 6; y > 0; y -= 0.4) {
			level.sendParticles(blood, target.getX(), target.getY() + y, target.getZ(), 2, 0.15, 0.05, 0.15, 0);
		}
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 0.5, target.getZ(), 20, 0.4, 0.2, 0.4, 0.04);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.5f);
	}

	/** Chains of Hell: every foe near is dragged in on chains and held fast. */
	private static void chains(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
		net.minecraft.core.particles.DustParticleOptions chain = new net.minecraft.core.particles.DustParticleOptions(CHAIN_GREY, 1.6f);
		for (LivingEntity target : around(p, CHAINS_RADIUS, e -> foe(p, e))) {
			Vec3 in = p.position().subtract(target.position());
			double distance = in.length();
			if (distance > 2) target.setDeltaMovement(in.normalize().scale(Math.min(1.6, distance * 0.2)).add(0, 0.25, 0));
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHAINS_TICKS, 6));
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CHAINS_TICKS, 1));
			Vec3 a = p.position().add(0, 1, 0), b = target.position().add(0, target.getBbHeight() / 2, 0);
			for (double d = 0; d < 1; d += 0.25 / Math.max(1, distance)) {
				Vec3 at = a.add(b.subtract(a).scale(d));
				level.sendParticles(chain, at.x, at.y, at.z, 1, 0, 0, 0, 0);
				if (((int) (d * distance * 4)) % 3 == 0) level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
			}
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 0.6f);
		}
	}

	/** Wheel of Samsara: the six paths turn, and what was taken comes back: whole health, harmful effects gone, half the yin. */
	private static void samsara(LivingEntity p, NetherworldState s) {
		p.setHealth(p.getMaxHealth());
		p.clearFire();
		for (MobEffectInstance effect : new java.util.ArrayList<>(p.getActiveEffects())) {
			if (effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) p.removeEffect(effect.getEffect());
		}
		if (!s.ghost()) s.setYinCorruption(s.yinCorruption() / 2);
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.7f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.5f);
		for (int spoke = 0; spoke < 6; spoke++) {
			double a = Math.PI * 2 * spoke / 6;
			for (double r = 0.3; r <= 1.8; r += 0.3) {
				level.sendParticles(ParticleTypes.END_ROD, p.getX() + Math.cos(a) * r, p.getY() + 1.1, p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}
		}
		for (int i = 0; i < 36; i++) {
			double a = Math.PI * 2 * i / 36;
			// GLOW, not SOUL: a soul particle comes out a dull brown face in the light.
			level.sendParticles(ParticleTypes.GLOW, p.getX() + Math.cos(a) * 1.9, p.getY() + 1.1, p.getZ() + Math.sin(a) * 1.9, 1, 0, 0.02, 0, 0.01);
		}
	}

	/** Six Paths Annihilation: six pillars of cold fire rise round the caster, and all within their ring is unmade. */
	private static void annihilation(LivingEntity p) {
		ServerLevel level = HeavenSpells.level(p);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.5f);
		level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.6f);
		for (int path = 0; path < 6; path++) {
			double a = Math.PI * 2 * path / 6;
			double x = p.getX() + Math.cos(a) * ANNIHILATION_RADIUS * 0.6, z = p.getZ() + Math.sin(a) * ANNIHILATION_RADIUS * 0.6;
			for (double y = 0; y < 10; y += 0.5) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, p.getY() + y, z, 2, 0.2, 0.1, 0.2, 0.01);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, p.getY() + 1, z, 1, 0, 0, 0, 0);
		}
		for (LivingEntity target : around(p, ANNIHILATION_RADIUS, e -> !spared(p, e))) {
			target.hurt(ModDamageTypes.ghostly(level, p), ANNIHILATION_DAMAGE);
			push(p, target, 2.0);
		}
	}

	/** Every tick: the spells' lingering effects (HeavenSpells#tick: the elements', both paths' and the ghost foundations' great spells). */
	public static void tick(net.minecraft.server.MinecraftServer server) {
		HeavenSpells.tick(server);
	}

	// --- Readying, arranging, practising ---

	/** Readies {@code spell} for the cast key (null clears it). */
	public static void ready(ServerPlayer p, Spell spell) {
		PlayerCultivation c = CultivationManager.get(p);
		if (spell != null && !spell.isKnown(c)) return;
		c.netherworld().setReadiedSpell(spell);
		CultivationManager.markDirty(p.server);
		if (spell != null) HeavenSpells.tell(p, Component.translatable(ModLang.MSG_SPELL_READIED, spell.getDisplayName()));
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
		HeavenSpells.tell(p, Component.translatable(ModLang.MSG_METHOD_PRACTISED, method.getDisplayName()).withStyle(ChatFormatting.AQUA));
		sync(p);
	}

	/** Tells the client its methods, active method, readied spell, spell wheel and cooldowns (none, while they aren't heeded). */
	public static void sync(ServerPlayer p) {
		boolean held = cooldownsHeld(p);
		ModPackets.sendMethods(p, CultivationManager.get(p).netherworld(), spell -> held ? cooldownLeft(p, spell) : 0);
	}

	public static void forget(UUID id) {
		FIRE.remove(id);
		SAFE_FALL.remove(id);
		HeavenSpells.forget(id);
	}

	public static void clear() {
		FIRE.clear();
		SAFE_FALL.clear();
		HeavenSpells.clear();
	}

	private Spells() {}
}
