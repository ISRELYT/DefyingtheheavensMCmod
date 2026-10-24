package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The ghost foundations' great spells, from Dao Integration on (the user's ask, 2026-10-24): living shapes of GeckoLib's (SpellConstruct),
 * as the realm's heaven spells are, of ghost fire and the dead's mist, not creatures. Cast on ghostly qi (Spells#cast pays), by a ghost
 * cultivator too (CultivatorSpells); their harm the dead's (ModDamageTypes#GHOSTLY: it reaches the living and the dead alike); their
 * reach and size by their realm (HeavenSpells#span, SpellConstruct#sizedFor), their harm on their own scale, below the heaven spells';
 * and none of them changes the land.
 * <ul>
 *   <li>Procession of Ten Thousand Ghosts (the Ten Thousand Ghosts Union's, layer 57): a column of the dead pours out along the look; all
 *       it passes through are struck, slowed and weakened, the dead the harder.</li>
 *   <li>Gate of Ten Thousand Ghosts (layer 61): a gate of the dead opens where the caster looks, drags every foe near toward it, draining
 *       them, and slams shut on all near it; a share of all it does mends the caster.</li>
 *   <li>Rift to the Nine Nethers (the Void-Rending Nether Canon's, layer 66): the ground where the caster looks tears open onto the Nine
 *       Nethers, nine rings of ghost fire turning down into it; every foe near is dragged in and down, drained, and struck as it seals.</li>
 *   <li>Bridge of No Return (layer 70): the Naihe Bridge, over the River of Forgetting, rises span by span along the look; all it rises
 *       by are struck and forget every blessing on them, and whoever stays on or under it pays its toll.</li>
 * </ul>
 */
public final class GhostSpells {
	/**
	 * Procession of Ten Thousand Ghosts: how far it pours out (61, by its realm: short of a wall), how wide (8), its harm (half as much again
	 * on the dead), how long the struck are slowed and weakened, and how long its head takes to reach the end (then it fades as it goes on).
	 */
	public static final double PROCESSION_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 48), PROCESSION_WIDTH = HeavenSpells.span(Realm.DAO_INTEGRATION, 6.25);
	public static final float PROCESSION_DAMAGE = 90, PROCESSION_DEAD_FACTOR = 1.5f;
	public static final int PROCESSION_WEAKENS = 5 * 20, PROCESSION_TICKS = 40;
	/**
	 * Gate of Ten Thousand Ghosts: how far off it opens (82, by its realm), how high its doorway's middle stands, whom it drags (within 18)
	 * and how hard, its drain a second, its slam's harm, the share of all its harm that mends the caster; how long it takes to open, how long
	 * it stands open, how long its doors take to meet as it slams, and how long it has then to sink away.
	 */
	public static final double GATE_RANGE = HeavenSpells.span(Realm.DAO_INTEGRATION, 64), GATE_MOUTH = HeavenSpells.span(Realm.DAO_INTEGRATION, 4),
			GATE_RADIUS = HeavenSpells.span(Realm.DAO_INTEGRATION, 14);
	public static final float GATE_DRAIN = 30, GATE_SLAM = 120, GATE_MENDS = 0.25f;
	public static final int GATE_OPENS = 30, GATE_TICKS = 8 * 20, GATE_SHUT = 8, GATE_CLOSING = 18;
	/** The Nine Nethers' teal, ghost fire's: the rifts onto them glow so. */
	public static final int NETHER_TEAL = 0x46EBC8;
	/**
	 * Rift to the Nine Nethers: where (up to {@link #NETHERS_RANGE}, 118 by its realm), how wide the rift is (24), whom it drags (within
	 * {@link #NETHERS_RADIUS}, 22) and whom its sealing strikes (within {@link #NETHERS_MOUTH}, 8); its drain a second, its sealing's harm
	 * (half as much again on the dead); how long it takes to open, how long it drags, how long it takes to seal, and how long the struck are
	 * weakened (ticks).
	 */
	public static final double NETHERS_RANGE = HeavenSpells.span(Realm.VOID_SHATTERING, 64), NETHERS_RIFT = HeavenSpells.span(Realm.VOID_SHATTERING, 13),
			NETHERS_RADIUS = HeavenSpells.span(Realm.VOID_SHATTERING, 12), NETHERS_MOUTH = HeavenSpells.span(Realm.VOID_SHATTERING, 4.4);
	public static final float NETHERS_DRAIN = 100, NETHERS_SEAL = 300, NETHERS_DEAD_FACTOR = 1.5f;
	public static final int NETHERS_OPENS = 16, NETHERS_TICKS = 6 * 20, NETHERS_SEALS = 12, NETHERS_WEAKENS = 8 * 20;
	/**
	 * Bridge of No Return: how far along the look it reaches (118 by its realm), each span's length (24: its model's, SpellConstruct#sizedFor),
	 * how near it it strikes (6); its harm as each span rises, and its toll a second after (half as much again on the dead); how far apart its
	 * spans rise, and how long it stands once all have (ticks).
	 */
	public static final double BRIDGE_LENGTH = HeavenSpells.span(Realm.VOID_SHATTERING, 64), BRIDGE_SPAN = HeavenSpells.span(Realm.VOID_SHATTERING, 13),
			BRIDGE_WIDTH = HeavenSpells.span(Realm.VOID_SHATTERING, 3.5);
	public static final float BRIDGE_DAMAGE = 300, BRIDGE_TOLL = 50, BRIDGE_DEAD_FACTOR = 1.5f;
	public static final int BRIDGE_EVERY = 4, BRIDGE_TICKS = 5 * 20;

	/** Casts {@code spell} (Spells#cast has paid for it: a column pours out, a gate opens, wherever one looks). */
	public static void cast(LivingEntity p, Spell spell) {
		switch (spell) {
			case PROCESSION_OF_TEN_THOUSAND_GHOSTS -> HeavenSpells.linger(new Procession(p, spell));
			case GATE_OF_TEN_THOUSAND_GHOSTS -> {
				HeavenSpells.endLingering(p.getUUID(), Gate.class);
				LivingEntity aimed = HeavenSpells.aimed(p, GATE_RANGE);
				HeavenSpells.linger(new Gate(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, GATE_RANGE)));
			}
			case RIFT_TO_THE_NINE_NETHERS -> {
				LivingEntity aimed = HeavenSpells.aimed(p, NETHERS_RANGE);
				HeavenSpells.linger(new Nethers(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, NETHERS_RANGE)));
			}
			case BRIDGE_OF_NO_RETURN -> {
				HeavenSpells.endLingering(p.getUUID(), Bridge.class);
				HeavenSpells.linger(new Bridge(p, spell));
			}
			default -> {}
		}
	}

	/** Strikes {@code target} for {@code amount} of the dead's harm by {@code p} (not past a barrier it couldn't break), and returns how much it took. */
	static float strike(LivingEntity p, LivingEntity target, float amount) {
		if (HeavenSpells.walled(p, target)) return 0;
		float before = target.getHealth() + target.getAbsorptionAmount();
		target.hurt(ModDamageTypes.ghostly(HeavenSpells.level(p), p), amount);
		return Math.max(0, before - target.getHealth() - target.getAbsorptionAmount());
	}

	// --- Procession of Ten Thousand Ghosts ---

	/**
	 * Procession of Ten Thousand Ghosts: a column of the dead, of ghost fire and mist (SpellConstruct PROCESSION, its own GeckoLib model),
	 * pours out of its caster along the look, its head reaching {@link #PROCESSION_RANGE} (short of a wall) in {@link #PROCESSION_TICKS}.
	 * Every being it passes through, within {@link #PROCESSION_WIDTH} across, is struck once for {@link #PROCESSION_DAMAGE} (the dead
	 * {@link #PROCESSION_DEAD_FACTOR} times as hard), slowed and weakened; then it fades as it goes on.
	 */
	public static final class Procession extends HeavenSpells.Timed {
		private final Vec3 from, way;
		private final double length;
		private final SpellConstruct column;
		private final Set<LivingEntity> struck = new HashSet<>();

		Procession(LivingEntity caster, Spell spell) {
			super(caster, PROCESSION_TICKS + SpellConstruct.FADE);
			this.from = caster.getEyePosition().subtract(0, 0.5, 0);
			this.way = caster.getViewVector(1.0f).normalize();
			this.length = Math.max(4, HeavenSpells.beamEnd(caster, PROCESSION_RANGE).distanceTo(caster.getEyePosition()));
			float[] facing = HeavenSpells.facing(way);
			this.column = SpellConstruct.create(level, SpellConstruct.Kind.PROCESSION, from, facing[0], facing[1], PROCESSION_TICKS + SpellConstruct.FADE, caster)
					.sizedFor(spell).spawn();
			HeavenSpells.sound(level, from, SoundEvents.GHAST_SCREAM, 2.5f, 0.35f);
			HeavenSpells.sound(level, from, SoundEvents.SOUL_ESCAPE, 3.0f, 0.5f);
			HeavenSpells.sound(level, from, SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.7f);
		}

		/** How far along its head has come {@code age} ticks in: out to its length, then on a little as it fades. */
		private double head(double age) {
			double speed = length / PROCESSION_TICKS;
			return age <= PROCESSION_TICKS ? length * (age / PROCESSION_TICKS) : length + (age - PROCESSION_TICKS) * speed * 0.5;
		}

		@Override
		protected boolean run(long age, long now) {
			double head = head(age);
			column.place(from.add(way.scale(head)), column.getYRot(), column.getXRot());
			if (age > PROCESSION_TICKS) return true; // spent: it only fades
			// All the column's head has passed through (a line from where it set out to its head, as wide as it), each once.
			double reach = Math.min(head, length), half = PROCESSION_WIDTH / 2;
			Vec3 end = from.add(way.scale(reach));
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(half + 1), e -> !struck.contains(e) && !HeavenSpells.spared(caster, e))) {
				Vec3 mid = HeavenSpells.middle(e).subtract(from);
				double along = mid.dot(way);
				if (along < 0 || along > reach || mid.subtract(way.scale(along)).length() > half + e.getBbWidth() / 2) continue;
				struck.add(e);
				strike(caster, e, GhostSystem.isDead(e) ? PROCESSION_DAMAGE * PROCESSION_DEAD_FACTOR : PROCESSION_DAMAGE);
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PROCESSION_WEAKENS, 1));
				e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PROCESSION_WEAKENS, 1));
				HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, HeavenSpells.middle(e), 10, 0.4, 0.05);
				HeavenSpells.sound(level, e.position(), SoundEvents.SOUL_ESCAPE, 1.2f, 0.6f);
			}
			// Ghost fire and souls trailing off its head.
			Vec3 at = from.add(way.scale(reach));
			HeavenSpells.particles(level, ParticleTypes.SOUL_FIRE_FLAME, at, 6, half * 0.6, 0.02);
			HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, at, 3, half * 0.5, 0.02);
			if (age % 8 == 0) HeavenSpells.sound(level, at, SoundEvents.SOUL_ESCAPE, 2.0f, 0.4f + level.random.nextFloat() * 0.3f);
			return true;
		}

		@Override
		public void end() {
			column.dissolve();
		}
	}

	// --- Gate of Ten Thousand Ghosts ---

	/**
	 * Gate of Ten Thousand Ghosts: a gate of the dead, bone-white mist and ghost fire (SpellConstruct GHOST_GATE, its own GeckoLib model),
	 * rises where its caster looks (on the being looked at, up to {@link #GATE_RANGE}), facing its caster, and opens. For {@link #GATE_TICKS}
	 * every foe within {@link #GATE_RADIUS} (HeavenSpells#mark) is dragged toward its doorway (a great beast only slowed), and once a second
	 * takes {@link #GATE_DRAIN}; then its doors slam shut, {@link #GATE_SLAM} to everything near it, and it sinks away. {@link #GATE_MENDS} of
	 * all its harm mends its caster (there with it).
	 */
	public static final class Gate extends HeavenSpells.Timed {
		private final Vec3 at, mouth;
		private final SpellConstruct gate;

		Gate(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, GATE_OPENS + GATE_TICKS + GATE_CLOSING);
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.at = below != null ? below : point;
			this.mouth = at.add(0, GATE_MOUTH, 0);
			float yaw = HeavenSpells.facing(caster.position().subtract(at).multiply(1, 0, 1))[0];
			this.gate = SpellConstruct.create(level, SpellConstruct.Kind.GHOST_GATE, at, yaw, 0, GATE_OPENS + GATE_TICKS + GATE_CLOSING, caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, at, SoundEvents.SOUL_ESCAPE, 4.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.IRON_DOOR_OPEN, 4.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.WITHER_AMBIENT, 2.0f, 0.5f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < GATE_OPENS) return true; // still rising and opening
			long open = age - GATE_OPENS;
			if (open <= GATE_TICKS) draw(open); // its last second's drain as its doors begin to swing
			if (open == GATE_TICKS) {
				gate.act("slam");
				HeavenSpells.sound(level, at, SoundEvents.IRON_DOOR_CLOSE, 4.0f, 0.4f);
			}
			if (open == GATE_TICKS + GATE_SHUT) slam();
			return true;
		}

		/** A tick of it standing open: every foe near dragged toward its doorway; once a second, drained. */
		private void draw(long open) {
			boolean drain = open > 0 && open % 20 == 0;
			float dealt = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, GATE_RADIUS, e -> HeavenSpells.mark(caster, e))) {
				Vec3 in = mouth.subtract(HeavenSpells.middle(e));
				double distance = in.length();
				if (HeavenSpells.greatBeast(e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
				else if (distance > 2.5) {
					HeavenSpells.move(e, e.getDeltaMovement().scale(0.4).add(in.scale(Math.min(0.55, 0.12 + distance * 0.03) / distance)));
					e.resetFallDistance();
				}
				if (drain) {
					e.invulnerableTime = 0; // its draining isn't turned by the last tick's
					dealt += strike(caster, e, GATE_DRAIN);
					HeavenSpells.line(level, HeavenSpells.middle(e), mouth, ParticleTypes.SCULK_SOUL, Math.max(1.0, distance / 10));
				}
			}
			if (dealt > 0 && casterHere()) HeavenSpells.mend(caster, dealt * GATE_MENDS);
			if (open % 4 == 0) HeavenSpells.particles(level, ParticleTypes.SOUL_FIRE_FLAME, mouth, 8, GATE_MOUTH * 0.5, 0.03);
			if (open % 30 == 0) HeavenSpells.sound(level, mouth, SoundEvents.SOUL_ESCAPE, 3.0f, 0.4f);
		}

		/** Its doors meet: everything near it struck and thrown off. */
		private void slam() {
			float dealt = 0;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, GATE_RADIUS, e -> !HeavenSpells.spared(caster, e))) {
				e.invulnerableTime = 0;
				dealt += strike(caster, e, GATE_SLAM);
				HeavenSpells.push(at, e, 1.6);
			}
			if (dealt > 0 && casterHere()) HeavenSpells.mend(caster, dealt * GATE_MENDS);
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, at, Vec3.ZERO, (float) GATE_RADIUS, 0, 192);
			HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, mouth, 40, GATE_MOUTH, 0.1);
			HeavenSpells.sound(level, at, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.5f);
			HeavenSpells.sound(level, at, SoundEvents.ANVIL_LAND, 3.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.GHAST_SCREAM, 2.0f, 0.3f);
		}

		@Override
		public void end() {
			gate.dissolve();
		}
	}

	/** {@code e}'s harm from one of the dead's spells: {@code amount}, or {@code deadFactor} times it on one of the dead. */
	private static float onTheDead(LivingEntity e, float amount, float deadFactor) {
		return GhostSystem.isDead(e) ? amount * deadFactor : amount;
	}

	// --- Rift to the Nine Nethers ---

	/**
	 * Rift to the Nine Nethers: the ground where its caster looks (on the being looked at, up to {@link #NETHERS_RANGE}) tears open onto the
	 * Nine Nethers (SpellConstruct ABYSS_RIFT, ghost fire's teal), nine rings of ghost fire turning down into it, each narrower and deeper (NINE_NETHERS,
	 * its own GeckoLib model). For {@link #NETHERS_TICKS} every foe within {@link #NETHERS_RADIUS} (HeavenSpells#mark) is dragged toward it and
	 * down (a great beast only slowed), and once a second takes {@link #NETHERS_DRAIN}; then it seals ({@link #NETHERS_SEALS}): {@link #NETHERS_SEAL}
	 * to everything within {@link #NETHERS_MOUTH} of it, weakened. The dead take {@link #NETHERS_DEAD_FACTOR} times as much. The land is left as it was.
	 */
	public static final class Nethers extends HeavenSpells.Timed {
		private final Vec3 ground, heart;
		private final SpellConstruct rift, rings;

		Nethers(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, NETHERS_OPENS + NETHERS_TICKS + NETHERS_SEALS + SpellConstruct.FADE);
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.heart = ground.add(0, 0.5, 0);
			int life = (int) (end - start);
			this.rift = SpellConstruct.create(level, SpellConstruct.Kind.ABYSS_RIFT, ground.add(0, 0.06, 0), caster.getYRot(), 0, life, caster).size((float) NETHERS_RIFT)
					.tint(NETHER_TEAL).spawn();
			this.rings = SpellConstruct.create(level, SpellConstruct.Kind.NINE_NETHERS, ground, caster.getYRot(), 0, life, caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, ground, SoundEvents.END_PORTAL_SPAWN, 3.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 4.0f, 0.3f);
			HeavenSpells.sound(level, ground, SoundEvents.WITHER_AMBIENT, 2.0f, 0.4f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < NETHERS_OPENS) return true;
			long open = age - NETHERS_OPENS;
			if (open < NETHERS_TICKS) drag(open);
			if (open == NETHERS_TICKS) {
				rings.act("seal");
				HeavenSpells.sound(level, ground, SoundEvents.IRON_DOOR_CLOSE, 4.0f, 0.3f);
			}
			if (open == NETHERS_TICKS + NETHERS_SEALS) seal();
			return true;
		}

		/** A tick of it standing open: every foe near dragged toward it and down; once a second, drained. */
		private void drag(long open) {
			boolean drain = open % 20 == 10;
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, NETHERS_RADIUS + 16, e -> HeavenSpells.mark(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > NETHERS_RADIUS || e.getY() > ground.y + 20 || e.getY() < ground.y - 12) continue;
				Vec3 in = heart.subtract(HeavenSpells.middle(e));
				double distance = in.length();
				if (HeavenSpells.greatBeast(e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
				else if (distance > 1.5) {
					HeavenSpells.move(e, e.getDeltaMovement().scale(0.4).add(in.scale(Math.min(0.6, 0.12 + distance * 0.035) / distance)).add(0, -0.08, 0));
					e.resetFallDistance();
				}
				if (drain) {
					e.invulnerableTime = 0;
					strike(caster, e, onTheDead(e, NETHERS_DRAIN, NETHERS_DEAD_FACTOR));
					HeavenSpells.line(level, HeavenSpells.middle(e), heart, ParticleTypes.SCULK_SOUL, Math.max(1.0, distance / 8));
				}
			}
			if (open % 3 == 0) {
				double angle = open * 0.5;
				for (int k = 0; k < 3; k++) {
					double a = angle + k * Math.PI * 2 / 3, r = NETHERS_RIFT * 0.35;
					HeavenSpells.particles(level, ParticleTypes.SOUL_FIRE_FLAME, ground.add(Math.cos(a) * r, 0.4, Math.sin(a) * r), 2, 0.3, 0.02);
				}
			}
			if (open % 30 == 0) HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 3.0f, 0.3f);
		}

		/** It seals: everything at its mouth struck and weakened; the rift closes. */
		private void seal() {
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, NETHERS_MOUTH + 6, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > NETHERS_MOUTH || e.getY() > ground.y + 10 || e.getY() < ground.y - 8) continue;
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, NETHERS_SEAL, NETHERS_DEAD_FACTOR));
				e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, NETHERS_WEAKENS, 1));
				HeavenSpells.push(ground, e, 1.2);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) NETHERS_RADIUS, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 1, 0), Vec3.ZERO, 6, NETHER_TEAL, 192);
			HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, ground.add(0, 1, 0), 50, NETHERS_MOUTH * 0.6, 0.12);
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 4.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.GHAST_SCREAM, 2.5f, 0.3f);
			rift.dissolve();
			rings.dissolve();
		}

		@Override
		public void end() {
			rift.dissolve();
			rings.dissolve();
		}
	}

	// --- Bridge of No Return ---

	/** How many spans the Bridge of No Return has (its length over a span's). */
	public static final int BRIDGE_SPANS = (int) Math.ceil(BRIDGE_LENGTH / BRIDGE_SPAN);

	/**
	 * Bridge of No Return: the Naihe Bridge (奈何桥), that the dead cross over the River of Forgetting, rises out of the mist along its caster's
	 * level look, span after span ({@link #BRIDGE_SPANS} of them, each {@link #BRIDGE_SPAN} long, {@link #BRIDGE_EVERY} ticks apart; SpellConstruct
	 * NAIHE_BRIDGE, its own GeckoLib model), each over the land's highest beneath it. Every being within {@link #BRIDGE_WIDTH} of a span as it
	 * rises takes {@link #BRIDGE_DAMAGE} and forgets every blessing on it (its good effects gone, as the river's water makes the dead forget);
	 * then for {@link #BRIDGE_TICKS} whoever is on or under it is slowed and pays its toll, {@link #BRIDGE_TOLL} a second. The dead take
	 * {@link #BRIDGE_DEAD_FACTOR} times as much. The land is left as it was.
	 */
	public static final class Bridge extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 from, way;
		private final float yaw;
		/** The land's highest within half a span of each block along it. */
		private final double[] high;
		private final List<SpellConstruct> spans = new ArrayList<>();
		private final Set<LivingEntity> struck = new HashSet<>();

		Bridge(LivingEntity caster, Spell spell) {
			super(caster, BRIDGE_SPANS * BRIDGE_EVERY + BRIDGE_TICKS + SpellConstruct.FADE);
			this.way = Vec3.directionFromRotation(0, caster.getYRot());
			this.from = caster.position();
			this.yaw = caster.getYRot();
			this.high = HeavenSpells.highestNear(HeavenSpells.floorAlong(level, from, way, (int) BRIDGE_LENGTH + 2), (int) Math.ceil(BRIDGE_SPAN / 2));
			this.spell = spell;
			HeavenSpells.sound(level, from, SoundEvents.SOUL_ESCAPE, 3.0f, 0.4f);
			HeavenSpells.sound(level, from, SoundEvents.BELL_RESONATE, 2.0f, 0.5f);
		}

		/** Span {@code i}'s middle, at its feet: over the land's highest beneath it. */
		private Vec3 middle(int i) {
			double d = (i + 0.5) * BRIDGE_SPAN;
			return new Vec3(from.x + way.x * d, HeavenSpells.along(high, d), from.z + way.z * d);
		}

		/** Whether {@code e} is on or under the span whose middle is {@code at} (within its width, a little above it, down to the land). */
		private boolean by(LivingEntity e, Vec3 at) {
			Vec3 rel = e.position().subtract(at);
			double along = rel.x * way.x + rel.z * way.z, off = Math.abs(rel.x * -way.z + rel.z * way.x);
			return Math.abs(along) <= BRIDGE_SPAN / 2 + 0.5 && off <= BRIDGE_WIDTH && rel.y > -14 && rel.y < 14;
		}

		@Override
		protected boolean run(long age, long now) {
			// Each span once its time has come: a player's cast is heard between ticks, so its first tick is already a tick in (before
			// 2026-10-24 the nearest span, due at its very first, never rose for a player).
			while (spans.size() < BRIDGE_SPANS && age >= (long) spans.size() * BRIDGE_EVERY) rise(spans.size());
			long standing = age - (long) BRIDGE_SPANS * BRIDGE_EVERY;
			if (standing >= 0 && standing < BRIDGE_TICKS && standing % 20 == 10) toll();
			return true;
		}

		/** Span {@code i} rises: every being by it struck, its blessings forgotten. */
		private void rise(int i) {
			Vec3 at = middle(i);
			spans.add(SpellConstruct.create(level, SpellConstruct.Kind.NAIHE_BRIDGE, at, yaw, 0, (int) (end - level.getGameTime()), caster).sizedFor(spell).spawn());
			for (LivingEntity e : HeavenSpells.aroundPoint(level, at, BRIDGE_SPAN / 2 + BRIDGE_WIDTH + 14, e -> !struck.contains(e) && !HeavenSpells.spared(caster, e))) {
				if (!by(e, at)) continue;
				struck.add(e);
				strike(caster, e, onTheDead(e, BRIDGE_DAMAGE, BRIDGE_DEAD_FACTOR));
				forget(e);
				HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, HeavenSpells.middle(e), 12, 0.4, 0.04);
			}
			HeavenSpells.particles(level, ParticleTypes.SOUL_FIRE_FLAME, at.add(0, 2, 0), 16, BRIDGE_SPAN * 0.3, 0.02);
			HeavenSpells.sound(level, at, SoundEvents.STONE_PLACE, 3.0f, 0.4f);
			HeavenSpells.sound(level, at, SoundEvents.SOUL_ESCAPE, 2.5f, 0.5f);
		}

		/** Whatever blessed {@code e} is forgotten: every good effect on it gone (as the river's water makes the dead forget their lives). */
		private void forget(LivingEntity e) {
			for (MobEffectInstance effect : new ArrayList<>(e.getActiveEffects())) {
				if (effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL) e.removeEffect(effect.getEffect());
			}
		}

		/** A second's toll: whoever is on or under it (once, though it stand where two spans meet) slowed and struck. */
		private void toll() {
			Set<LivingEntity> paying = new HashSet<>();
			for (int i = 0; i < spans.size(); i++) {
				Vec3 at = middle(i);
				for (LivingEntity e : HeavenSpells.aroundPoint(level, at, BRIDGE_SPAN / 2 + BRIDGE_WIDTH + 14, e -> !HeavenSpells.spared(caster, e))) {
					if (by(e, at)) paying.add(e);
				}
			}
			for (LivingEntity e : paying) {
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, BRIDGE_TOLL, BRIDGE_DEAD_FACTOR));
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
			}
			HeavenSpells.sound(level, middle(spans.size() / 2), SoundEvents.SOUL_ESCAPE, 2.0f, 0.4f);
		}

		@Override
		public void end() {
			for (SpellConstruct span : spans) span.dissolve();
		}
	}

	private GhostSpells() {}
}
