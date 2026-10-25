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
	/**
	 * Mirror of Retribution: how far its light reaches (64 by its realm) and how wide it spreads (degrees off the look); its harm a second in
	 * its light and its last flash (half as much again on the dead); how long it shines, and how long the shown are weakened and lit (ticks).
	 */
	public static final double MIRROR_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 30), MIRROR_CONE = 22, MIRROR_OVER = HeavenSpells.span(Realm.SACRED_VESSEL, 4),
			MIRROR_BEHIND = HeavenSpells.span(Realm.SACRED_VESSEL, 1.5);
	public static final float MIRROR_DAMAGE = 140, MIRROR_FLASH = 500, MIRROR_DEAD_FACTOR = 1.5f;
	public static final int MIRROR_TICKS = 3 * 20, MIRROR_SHOWN = 8 * 20;
	/**
	 * Soul-Locking Pagoda: where (up to {@link #PAGODA_RANGE}, 136), how high it falls from (64), whom it locks in (within 15) and how high
	 * they may go in it (its height, 30); its harm as it lands, its drain a second, and its sealing (half as much again on the dead); how long it
	 * falls, how long it holds, how long its sealing takes, and how long the sealed are weakened (ticks).
	 */
	public static final double PAGODA_RANGE = HeavenSpells.span(Realm.SACRED_VESSEL, 64), PAGODA_HIGH = HeavenSpells.span(Realm.SACRED_VESSEL, 30),
			PAGODA_RADIUS = HeavenSpells.span(Realm.SACRED_VESSEL, 7), PAGODA_HEIGHT = HeavenSpells.span(Realm.SACRED_VESSEL, 14);
	public static final float PAGODA_SLAM = 300, PAGODA_DRAIN = 120, PAGODA_SEAL = 500, PAGODA_DEAD_FACTOR = 1.5f;
	public static final int PAGODA_FALL = 20, PAGODA_TICKS = 6 * 20, PAGODA_SEALS = 14, PAGODA_WEAKENS = 10 * 20;
	/** Hell fire's dull red, the forge's: the pagoda and the mirror burn so where ghost fire's teal does not. */
	public static final int HELL_RED = 0xC8361E;
	/**
	 * Black and White Impermanence: how far its caster may look for the soul to be taken (72 by its realm), how far to either side of it the
	 * two come (7); the White's blow, the Black's chain a second while it holds, and the last of it (a share of the soul's whole health, no more
	 * than its most); half as much again on the dead; how long they take to come, how long after the White's blow the Black's chain lashes, how
	 * long it holds, and how long they have after (ticks).
	 */
	public static final double IMPERMANENCE_RANGE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 30), IMPERMANENCE_BESIDE = HeavenSpells.span(Realm.ENTERING_NIRVANA, 3);
	public static final float IMPERMANENCE_STRIKE = 500, IMPERMANENCE_CHAIN = 300, IMPERMANENCE_SHARE = 0.1f, IMPERMANENCE_MOST = 1200, IMPERMANENCE_DEAD_FACTOR = 1.5f;
	public static final int IMPERMANENCE_COMES = 20, IMPERMANENCE_HOOK = 10, IMPERMANENCE_HOLDS = 3 * 20, IMPERMANENCE_AFTER = 20;
	/**
	 * Book of Life and Death: how far round its caster it finds the names to strike out (72 by its realm), how far over their head and behind it
	 * the book opens (8, 2); its harm a second on the marked, its last stroke, and the share of its whole health below which a marked soul is
	 * struck out of life (a great one of the world takes its most instead); half as much again on the dead; how many it marks; how long it opens,
	 * how long it writes, how long its stroke takes, and how long it has after (ticks).
	 */
	public static final double BOOK_REACH = HeavenSpells.span(Realm.ENTERING_NIRVANA, 30), BOOK_OVER = HeavenSpells.span(Realm.ENTERING_NIRVANA, 3.5),
			BOOK_BEHIND = HeavenSpells.span(Realm.ENTERING_NIRVANA, 1);
	public static final float BOOK_MARK = 250, BOOK_STRIKE = 900, BOOK_EXECUTE = 0.25f, BOOK_GREAT = 2000, BOOK_DEAD_FACTOR = 1.5f;
	public static final int BOOK_MOST = 8, BOOK_OPENS = 20, BOOK_WRITES = 3 * 20, BOOK_STROKE = 10, BOOK_AFTER = 20;
	/** Cinnabar, the judge's red ink: its strokes burn so. */
	public static final int CINNABAR = 0xC21E1E;
	/**
	 * Fengdu's Walls Rise: where (136 by its realm), how far round its ring of walls stands (37), and how many lengths of wall; its harm to every
	 * foe within each time the city's dead fall on them (more to the dead), and as its walls topple in; how long they take to rise, how long
	 * they stand, how often the dead strike, how long the walls take to fall, and how long it has after (ticks).
	 */
	public static final double FENGDU_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 40), FENGDU_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 11);
	public static final int FENGDU_WALLS = 8;
	public static final float FENGDU_HARM = 800, FENGDU_FALL = 4000, FENGDU_DEAD_FACTOR = 1.5f;
	public static final int FENGDU_RISES = 20, FENGDU_TICKS = 5 * 20, FENGDU_EVERY = 10, FENGDU_FALLS = 16, FENGDU_AFTER = 16;
	/**
	 * Ten Thousand Souls Ascend: where (136 by its realm), how far round it draws foes in (34), how far round its helix of souls holds them
	 * (14), how high it carries them (20); its harm each time its souls tear at them (more to the dead), and as it drags them down; how long it
	 * takes to rise, how long it spirals, how often it strikes, how long it takes to go down, and how long it has after (ticks).
	 */
	public static final double SOULS_RANGE = HeavenSpells.span(Realm.TRUE_IMMORTAL, 40), SOULS_PULL = HeavenSpells.span(Realm.TRUE_IMMORTAL, 10),
			SOULS_RADIUS = HeavenSpells.span(Realm.TRUE_IMMORTAL, 4), SOULS_HIGH = HeavenSpells.span(Realm.TRUE_IMMORTAL, 6);
	public static final float SOULS_HARM = 1000, SOULS_SLAM = 5000, SOULS_DEAD_FACTOR = 1.5f;
	public static final int SOULS_RISES = 24, SOULS_TICKS = 5 * 20, SOULS_EVERY = 10, SOULS_DESCENDS = 10, SOULS_AFTER = 14;

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
			case MIRROR_OF_RETRIBUTION -> {
				HeavenSpells.endLingering(p.getUUID(), Mirror.class);
				HeavenSpells.linger(new Mirror(p, spell));
			}
			case SOUL_LOCKING_PAGODA -> {
				LivingEntity aimed = HeavenSpells.aimed(p, PAGODA_RANGE);
				HeavenSpells.linger(new Pagoda(p, spell, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, PAGODA_RANGE)));
			}
			case BLACK_AND_WHITE_IMPERMANENCE -> {
				LivingEntity aimed = HeavenSpells.aimed(p, IMPERMANENCE_RANGE);
				if (aimed == null) {
					// None looked at: the soul nearest where its caster looks, if any is near it.
					Vec3 at = HeavenSpells.lookPoint(p, IMPERMANENCE_RANGE);
					for (LivingEntity e : HeavenSpells.aroundPoint(HeavenSpells.level(p), at, IMPERMANENCE_BESIDE * 2, e -> HeavenSpells.mark(p, e) && !HeavenSpells.spared(p, e))) {
						if (aimed == null || e.distanceToSqr(at) < aimed.distanceToSqr(at)) aimed = e;
					}
				}
				HeavenSpells.linger(new Impermanence(p, spell, aimed, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, IMPERMANENCE_RANGE)));
			}
			case BOOK_OF_LIFE_AND_DEATH -> {
				HeavenSpells.endLingering(p.getUUID(), Book.class);
				HeavenSpells.linger(new Book(p, spell));
			}
			case FENGDU_WALLS_RISE -> {
				HeavenSpells.endLingering(p.getUUID(), Fengdu.class);
				LivingEntity aimed = HeavenSpells.aimed(p, FENGDU_RANGE);
				HeavenSpells.linger(new Fengdu(p, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, FENGDU_RANGE)));
			}
			case TEN_THOUSAND_SOULS_ASCEND -> {
				LivingEntity aimed = HeavenSpells.aimed(p, SOULS_RANGE);
				HeavenSpells.linger(new Souls(p, aimed != null ? aimed.position() : HeavenSpells.lookPoint(p, SOULS_RANGE)));
			}
			default -> {}
		}
	}

	/** Strikes {@code target} for {@code amount} of the dead's harm by {@code p} (not past a barrier it couldn't break), and returns how much it took. */
	static float strike(LivingEntity p, LivingEntity target, float amount) {
		if (HeavenSpells.walled(p, target)) return 0;
		HeavenSpells.landsAcross(p, target);
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
				if (HeavenSpells.greatBeast(caster, e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
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
				if (HeavenSpells.greatBeast(caster, e)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
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

	// --- Sacred Vessel: the Hell-Forged Vessel Sutra's ---

	/**
	 * Mirror of Retribution: the karma mirror of Yama's court, that shows every soul its sins, forged in hell fire, bronze on a stand of bone
	 * (SpellConstruct KARMA_MIRROR, its own GeckoLib model), rises over its caster, behind their head (not before their eyes: as great as its
	 * realm makes it, it hid all they looked at), and turns with their look for {@link #MIRROR_TICKS}: its light pours out over them along
	 * their look, {@link #MIRROR_RANGE} and {@link #MIRROR_CONE} degrees wide from their eyes, and every being in it is shown its sins: it takes
	 * {@link #MIRROR_DAMAGE} of the dead's harm a second, is weakened, slowed and shown glowing (wherever it hides) for {@link #MIRROR_SHOWN}.
	 * Then the mirror flashes: {@link #MIRROR_FLASH} to everything in its light. The dead take half as much again.
	 */
	public static final class Mirror extends HeavenSpells.Timed {
		private final Spell spell;
		private final SpellConstruct mirror;

		Mirror(LivingEntity caster, Spell spell) {
			super(caster, MIRROR_TICKS + 16);
			this.spell = spell;
			this.mirror = SpellConstruct.create(level, SpellConstruct.Kind.KARMA_MIRROR, before(), caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.SOUL_ESCAPE, 3.0f, 0.4f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.BEACON_ACTIVATE, 3.0f, 0.4f);
		}

		/** Where it stands: over its caster's head and a little behind it, so its stand of bone clears them. */
		private Vec3 before() {
			Vec3 look = Vec3.directionFromRotation(0, caster.getYRot());
			return caster.position().add(look.scale(-MIRROR_BEHIND)).add(0, caster.getBbHeight() + MIRROR_OVER, 0);
		}

		/** Every being in its light now: in the cone of its caster's look, from their eyes. */
		private List<LivingEntity> shown() {
			Vec3 from = caster.getEyePosition(), look = caster.getViewVector(1.0f);
			double cos = Math.cos(Math.toRadians(MIRROR_CONE));
			return HeavenSpells.aroundPoint(level, from, MIRROR_RANGE, e -> {
				if (HeavenSpells.spared(caster, e)) return false;
				Vec3 to = HeavenSpells.middle(e).subtract(from);
				return to.length() > 1 && to.normalize().dot(look) >= cos;
			});
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			mirror.place(before(), caster.getYRot(), Math.max(-30, Math.min(30, caster.getXRot())));
			if (age > MIRROR_TICKS) return true;
			if (age % 20 == 10) {
				for (LivingEntity e : shown()) {
					e.invulnerableTime = 0;
					strike(caster, e, onTheDead(e, MIRROR_DAMAGE, MIRROR_DEAD_FACTOR));
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, MIRROR_SHOWN, 1));
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MIRROR_SHOWN, 1));
					e.addEffect(new MobEffectInstance(MobEffects.GLOWING, MIRROR_SHOWN, 0));
				}
				HeavenSpells.sound(level, mirror.position(), SoundEvents.SOUL_ESCAPE, 2.0f, 0.6f);
			}
			if (age % 3 == 0) {
				// Its light along the look: a few rays out to its reach.
				Vec3 from = mirror.position(), look = caster.getViewVector(1.0f);
				for (int k = 0; k < 3; k++) {
					Vec3 ray = look.xRot((float) Math.toRadians((level.random.nextDouble() - 0.5) * MIRROR_CONE * 1.6))
							.yRot((float) Math.toRadians((level.random.nextDouble() - 0.5) * MIRROR_CONE * 1.6));
					SpellFx.send(level, SpellFx.Kind.STREAK, from, ray.scale(MIRROR_RANGE * (0.6 + 0.4 * level.random.nextDouble())), 0, NETHER_TEAL, 160);
				}
			}
			if (age == MIRROR_TICKS) {
				mirror.act("flash");
				for (LivingEntity e : shown()) {
					e.invulnerableTime = 0;
					strike(caster, e, onTheDead(e, MIRROR_FLASH, MIRROR_DEAD_FACTOR));
				}
				SpellFx.send(level, SpellFx.Kind.FLASH, mirror.position(), Vec3.ZERO, 12, NETHER_TEAL, 192);
				HeavenSpells.sound(level, mirror.position(), SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.6f);
			}
			return true;
		}

		@Override
		public void end() {
			mirror.dissolve();
		}
	}

	/**
	 * Soul-Locking Pagoda: a pagoda of seven tiers, black iron and bone forged in the lowest hell, its eaves hung with soul lanterns and bells
	 * (SpellConstruct SOUL_PAGODA, its own GeckoLib model), falls out of the sky onto where its caster looks ({@link #PAGODA_FALL}):
	 * {@link #PAGODA_SLAM} to everything under it. Every foe within {@link #PAGODA_RADIUS} is locked in it for {@link #PAGODA_TICKS}: it can't
	 * leave (it is brought back to its walls), and it is drained of {@link #PAGODA_DRAIN} a second, ghost fire pouring from the eaves. Then it
	 * seals ({@link #PAGODA_SEALS}): {@link #PAGODA_SEAL} to all locked in it, and Weakness II for {@link #PAGODA_WEAKENS}. The dead take half as
	 * much again; the land is left as it was.
	 */
	public static final class Pagoda extends HeavenSpells.Timed {
		private final Spell spell;
		private final Vec3 ground, sky;
		private final SpellConstruct pagoda;
		private final java.util.Set<LivingEntity> locked = new java.util.LinkedHashSet<>();

		Pagoda(LivingEntity caster, Spell spell, Vec3 point) {
			super(caster, PAGODA_FALL + PAGODA_TICKS + PAGODA_SEALS + 12);
			this.spell = spell;
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.sky = ground.add(0, PAGODA_HIGH, 0);
			this.pagoda = SpellConstruct.create(level, SpellConstruct.Kind.SOUL_PAGODA, sky, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, sky, SoundEvents.BELL_RESONATE, 4.0f, 0.3f);
			HeavenSpells.sound(level, sky, SoundEvents.SOUL_ESCAPE, 3.0f, 0.3f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age <= PAGODA_FALL) {
				double f = age / (double) PAGODA_FALL;
				pagoda.place(sky.lerp(ground, f * f), pagoda.getYRot(), 0);
				if (age == PAGODA_FALL) land();
				return true;
			}
			long holding = age - PAGODA_FALL;
			if (holding < PAGODA_TICKS) {
				hold();
				if (holding % 20 == 10) {
					for (LivingEntity e : locked) {
						if (!HeavenSpells.present(e, level)) continue;
						e.invulnerableTime = 0;
						strike(caster, e, onTheDead(e, PAGODA_DRAIN, PAGODA_DEAD_FACTOR));
						SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(e), ground.add(0, PAGODA_HEIGHT * 0.5, 0).subtract(HeavenSpells.middle(e)), 1, 0);
					}
					HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 3.0f, 0.4f);
				}
				return true;
			}
			if (holding == PAGODA_TICKS) {
				pagoda.act("seal");
				HeavenSpells.sound(level, ground, SoundEvents.BELL_BLOCK, 4.0f, 0.3f);
			}
			if (holding < PAGODA_TICKS + PAGODA_SEALS) hold();
			if (holding == PAGODA_TICKS + PAGODA_SEALS) seal();
			return true;
		}

		/** It lands: everything under it struck, every foe within it locked in. */
		private void land() {
			pagoda.act("land");
			for (LivingEntity e : HeavenSpells.aroundPoint(level, ground, PAGODA_RADIUS + PAGODA_HEIGHT, e -> !HeavenSpells.spared(caster, e))) {
				if (Math.hypot(e.getX() - ground.x, e.getZ() - ground.z) > PAGODA_RADIUS || e.getY() > ground.y + PAGODA_HEIGHT || e.getY() < ground.y - 8) continue;
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, PAGODA_SLAM, PAGODA_DEAD_FACTOR));
				locked.add(e);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) PAGODA_RADIUS * 1.6f, 0, 192);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 2, 0), Vec3.ZERO, 8, NETHER_TEAL, 192);
			HeavenSpells.sound(level, ground, SoundEvents.ANVIL_LAND, 4.0f, 0.3f);
			HeavenSpells.sound(level, ground, SoundEvents.CHAIN_PLACE, 4.0f, 0.4f);
		}

		/** The locked kept within its walls (brought back to them if they stray, held under its spire). */
		private void hold() {
			for (java.util.Iterator<LivingEntity> it = locked.iterator(); it.hasNext(); ) {
				LivingEntity e = it.next();
				if (!HeavenSpells.present(e, level)) {
					it.remove();
					continue;
				}
				Vec3 off = e.position().subtract(ground).multiply(1, 0, 1);
				double d = off.length(), y = Math.min(e.getY(), ground.y + PAGODA_HEIGHT - e.getBbHeight());
				if (d > PAGODA_RADIUS * 0.9 || e.getY() > y + 0.01) {
					Vec3 in = d > PAGODA_RADIUS * 0.9 ? ground.add(off.normalize().scale(PAGODA_RADIUS * 0.85)) : e.position();
					HeavenSpells.teleport(e, new Vec3(in.x, y, in.z));
				}
				HeavenSpells.safeFall(e, 40);
			}
		}

		/** It seals: every soul locked in it struck and weakened. */
		private void seal() {
			for (LivingEntity e : locked) {
				if (!HeavenSpells.present(e, level)) continue;
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, PAGODA_SEAL, PAGODA_DEAD_FACTOR));
				e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PAGODA_WEAKENS, 1));
			}
			locked.clear();
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, PAGODA_HEIGHT * 0.5, 0), Vec3.ZERO, 14, NETHER_TEAL, 192);
			HeavenSpells.sound(level, ground, SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.5f);
		}

		@Override
		public void end() {
			locked.clear();
			pagoda.dissolve();
		}
	}

	// --- Entering Nirvana: the Underworld Nirvana Scripture's ---

	/**
	 * Black and White Impermanence: the two soul-catchers of the underworld, the White who smiles and the Black who scowls, of the dead's mist and
	 * ghost fire (SpellConstruct WHITE_IMPERMANENCE, BLACK_IMPERMANENCE, their own GeckoLib models), rise out of the mist to either side of the
	 * soul its caster looks at ({@link #IMPERMANENCE_COMES}), {@link #IMPERMANENCE_BESIDE} from it, and come for it: the White sweeps its banner
	 * down on it ({@link #IMPERMANENCE_STRIKE}, slowed); the Black's chain lashes out and binds it ({@link #IMPERMANENCE_HOOK} after), holding it
	 * for {@link #IMPERMANENCE_HOLDS} at {@link #IMPERMANENCE_CHAIN} a second; and then they take their share of it, {@link #IMPERMANENCE_SHARE}
	 * of its whole health (no more than {@link #IMPERMANENCE_MOST}). The dead take half as much again.
	 */
	public static final class Impermanence extends HeavenSpells.Timed {
		private final LivingEntity target;
		private Vec3 at;
		private final SpellConstruct white, black;

		Impermanence(LivingEntity caster, Spell spell, LivingEntity target, Vec3 point) {
			super(caster, IMPERMANENCE_COMES + IMPERMANENCE_HOOK + IMPERMANENCE_HOLDS + IMPERMANENCE_AFTER);
			this.target = target;
			this.at = point;
			Vec3[] places = places();
			this.white = SpellConstruct.create(level, SpellConstruct.Kind.WHITE_IMPERMANENCE, places[0], facing(places[0]), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			this.black = SpellConstruct.create(level, SpellConstruct.Kind.BLACK_IMPERMANENCE, places[1], facing(places[1]), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, at, SoundEvents.SOUL_ESCAPE, 4.0f, 0.3f);
			HeavenSpells.sound(level, at, SoundEvents.BELL_RESONATE, 3.0f, 0.4f);
		}

		private boolean holds() {
			return target != null && HeavenSpells.present(target, level) && target.isAlive();
		}

		/** Where the White and the Black stand: to either side of their quarry, across its caster's look at it. */
		private Vec3[] places() {
			Vec3 way = at.subtract(caster.position()).multiply(1, 0, 1);
			way = way.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, caster.getYRot()) : way.normalize();
			Vec3 side = new Vec3(-way.z, 0, way.x).scale(IMPERMANENCE_BESIDE);
			return new Vec3[] {at.add(side), at.subtract(side)};
		}

		private float facing(Vec3 from) {
			return HeavenSpells.facing(at.subtract(from).multiply(1, 0, 1))[0];
		}

		@Override
		protected boolean run(long age, long now) {
			if (holds()) at = target.position();
			Vec3[] places = places();
			white.place(places[0], facing(places[0]), 0);
			black.place(places[1], facing(places[1]), 0);
			if (age == IMPERMANENCE_COMES) {
				white.act("strike");
				if (holds()) {
					target.invulnerableTime = 0;
					strike(caster, target, onTheDead(target, IMPERMANENCE_STRIKE, IMPERMANENCE_DEAD_FACTOR));
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
				}
				SpellFx.send(level, SpellFx.Kind.FLASH, at.add(0, 2, 0), Vec3.ZERO, 6, 0xF2FFFD, 192);
				HeavenSpells.sound(level, at, SoundEvents.PHANTOM_BITE, 3.0f, 0.5f);
			}
			long hooked = age - IMPERMANENCE_COMES - IMPERMANENCE_HOOK;
			if (hooked == 0) {
				black.act("strike");
				HeavenSpells.sound(level, at, SoundEvents.CHAIN_PLACE, 4.0f, 0.5f);
			}
			if (hooked >= 0 && hooked < IMPERMANENCE_HOLDS && holds()) {
				HeavenSpells.still(target);
				if (hooked % 20 == 10) {
					target.invulnerableTime = 0;
					strike(caster, target, onTheDead(target, IMPERMANENCE_CHAIN, IMPERMANENCE_DEAD_FACTOR));
					SpellFx.send(level, SpellFx.Kind.LIFE_MOTES, HeavenSpells.middle(target), places[1].add(0, 6, 0).subtract(HeavenSpells.middle(target)), 1.5f, 0);
					HeavenSpells.sound(level, at, SoundEvents.CHAIN_STEP, 3.0f, 0.5f);
				}
			}
			if (hooked == IMPERMANENCE_HOLDS && holds()) {
				float share = Math.min(IMPERMANENCE_MOST, target.getMaxHealth() * IMPERMANENCE_SHARE);
				target.invulnerableTime = 0;
				strike(caster, target, onTheDead(target, share, IMPERMANENCE_DEAD_FACTOR));
				SpellFx.send(level, SpellFx.Kind.FLASH, HeavenSpells.middle(target), Vec3.ZERO, 8, NETHER_TEAL, 192);
				HeavenSpells.sound(level, at, SoundEvents.SOUL_ESCAPE, 4.0f, 0.6f);
				HeavenSpells.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 1.2f);
			}
			return true;
		}

		@Override
		public void end() {
			white.dissolve();
			black.dissolve();
		}
	}

	/**
	 * Book of Life and Death: the book in which every soul's span is written, black lacquer and gold, its judge's brush over it red with cinnabar
	 * (SpellConstruct LIFE_DEATH_BOOK, its own GeckoLib model), opens over its caster's head ({@link #BOOK_OPENS}) and finds the names of the foes
	 * round them, {@link #BOOK_MOST} at most within {@link #BOOK_REACH}, nearest first: each is marked, shown glowing wherever it is, and for
	 * {@link #BOOK_WRITES} takes {@link #BOOK_MARK} a second. Then the brush strikes their names out ({@link #BOOK_STROKE}): {@link #BOOK_STRIKE}
	 * to each, and any left with less than {@link #BOOK_EXECUTE} of its whole health is struck out of life (a great one of the world takes
	 * {@link #BOOK_GREAT} more instead). The dead take half as much again.
	 */
	public static final class Book extends HeavenSpells.Timed {
		private final SpellConstruct book;
		private final List<LivingEntity> marked = new ArrayList<>();

		Book(LivingEntity caster, Spell spell) {
			super(caster, BOOK_OPENS + BOOK_WRITES + BOOK_STROKE + BOOK_AFTER);
			this.book = SpellConstruct.create(level, SpellConstruct.Kind.LIFE_DEATH_BOOK, over(), caster.getYRot(), 0, (int) (end - start), caster).sizedFor(spell).spawn();
			HeavenSpells.sound(level, caster.position(), SoundEvents.BOOK_PAGE_TURN, 4.0f, 0.4f);
			HeavenSpells.sound(level, caster.position(), SoundEvents.SOUL_ESCAPE, 3.0f, 0.4f);
		}

		/** Where it opens: over its caster's head, a little behind it. */
		private Vec3 over() {
			Vec3 look = Vec3.directionFromRotation(0, caster.getYRot());
			return caster.position().add(look.scale(-BOOK_BEHIND)).add(0, caster.getBbHeight() + BOOK_OVER, 0);
		}

		@Override
		protected boolean run(long age, long now) {
			if (!casterHere()) return false;
			book.place(over(), caster.getYRot(), 0);
			if (age == BOOK_OPENS) mark();
			long writing = age - BOOK_OPENS;
			if (writing >= 0 && writing < BOOK_WRITES && writing % 20 == 10) {
				for (LivingEntity e : marked) {
					if (!HeavenSpells.present(e, level) || !e.isAlive()) continue;
					e.invulnerableTime = 0;
					strike(caster, e, onTheDead(e, BOOK_MARK, BOOK_DEAD_FACTOR));
				}
				HeavenSpells.sound(level, caster.position(), SoundEvents.BOOK_PAGE_TURN, 3.0f, 0.6f);
			}
			if (writing == BOOK_WRITES) {
				book.act("strike");
				HeavenSpells.sound(level, caster.position(), SoundEvents.BOOK_PUT, 4.0f, 0.4f);
			}
			if (writing == BOOK_WRITES + BOOK_STROKE) strikeOut();
			return true;
		}

		/** It finds their names: the foes round its caster, nearest first, marked and shown. */
		private void mark() {
			List<LivingEntity> foes = HeavenSpells.around(caster, BOOK_REACH, e -> HeavenSpells.mark(caster, e) && !HeavenSpells.spared(caster, e));
			foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(caster)));
			for (LivingEntity e : foes.subList(0, Math.min(BOOK_MOST, foes.size()))) {
				marked.add(e);
				e.addEffect(new MobEffectInstance(MobEffects.GLOWING, BOOK_WRITES + BOOK_STROKE + 20, 0));
				SpellFx.send(level, SpellFx.Kind.FLASH, HeavenSpells.middle(e).add(0, e.getBbHeight() * 0.6, 0), Vec3.ZERO, 2.5f, CINNABAR, 160);
			}
		}

		/** The brush strikes their names out: each struck, and any left near the end of its span struck out of life. */
		private void strikeOut() {
			Vec3 from = book.position();
			for (LivingEntity e : marked) {
				if (!HeavenSpells.present(e, level) || !e.isAlive()) continue;
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, BOOK_STRIKE, BOOK_DEAD_FACTOR));
				if (e.isAlive() && e.getHealth() < e.getMaxHealth() * BOOK_EXECUTE) {
					e.invulnerableTime = 0;
					boolean great = e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
							|| e instanceof net.minecraft.world.entity.monster.warden.Warden;
					strike(caster, e, great ? onTheDead(e, BOOK_GREAT, BOOK_DEAD_FACTOR) : e.getMaxHealth() * 4 + e.getAbsorptionAmount());
				}
				SpellFx.send(level, SpellFx.Kind.STREAK, from, HeavenSpells.middle(e).subtract(from), 0, CINNABAR, 192);
			}
			SpellFx.send(level, SpellFx.Kind.FLASH, from, Vec3.ZERO, 10, CINNABAR, 192);
			HeavenSpells.sound(level, caster.position(), SoundEvents.WARDEN_SONIC_BOOM, 3.0f, 0.8f);
		}

		@Override
		public void end() {
			marked.clear();
			book.dissolve();
		}
	}

	// --- True Immortal: the Netherworld Immortal Canon ---

	/**
	 * Fengdu's Walls Rise: {@link #FENGDU_WALLS} lengths of the walls of Fengdu, the city of the dead, black stone, crenellated, a gate-tower at
	 * the middle of each and soul lanterns at its ends (SpellConstruct FENGDU_WALL, its own GeckoLib model), rise out of the land in a ring
	 * {@link #FENGDU_RADIUS} round where its caster looks, facing in ({@link #FENGDU_RISES}): for {@link #FENGDU_TICKS} no foe within gets out
	 * past them, and every {@link #FENGDU_EVERY} ticks the city's dead fall on every foe within, {@link #FENGDU_HARM} of the dead's harm (more to
	 * the dead), slowing them. Then the walls topple in on its heart ({@link #FENGDU_FALLS}): {@link #FENGDU_FALL} to every foe within.
	 */
	public static final class Fengdu extends HeavenSpells.Timed {
		private final Vec3 ground;
		private final double inner;
		private final SpellConstruct[] walls = new SpellConstruct[FENGDU_WALLS];

		Fengdu(LivingEntity caster, Vec3 point) {
			super(caster, FENGDU_RISES + FENGDU_TICKS + FENGDU_FALLS + FENGDU_AFTER);
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			// Each length along a side of the ring's octagon, its middle that far in from the ring's corners.
			this.inner = FENGDU_RADIUS * Math.cos(Math.PI / FENGDU_WALLS);
			for (int k = 0; k < FENGDU_WALLS; k++) {
				Vec3 out = Vec3.directionFromRotation(0, caster.getYRot() + 180f / FENGDU_WALLS + 360f * k / FENGDU_WALLS).scale(inner);
				Vec3 land = HeavenSpells.groundBelow(level, ground.add(out).add(0, 16, 0), 0, 48), spot = land != null ? land : ground.add(out);
				// Its face toward the heart (its model's -z: the way its yaw looks), so it falls in on it.
				float[] face = HeavenSpells.facing(ground.subtract(spot).multiply(1, 0, 1));
				walls[k] = SpellConstruct.create(level, SpellConstruct.Kind.FENGDU_WALL, spot, face[0], 0, (int) (end - start), caster).sizedFor(Spell.FENGDU_WALLS_RISE).spawn();
			}
			HeavenSpells.sound(level, ground, SoundEvents.WARDEN_EMERGE, 5.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 5.0f, 0.4f);
		}

		private List<LivingEntity> within() {
			return WoodSpells.foesWithin(caster, level, ground, inner - 1, 10, 30);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < FENGDU_RISES) return true;
			long standing = age - FENGDU_RISES;
			if (standing < FENGDU_TICKS) {
				hold();
				if (standing % FENGDU_EVERY == 0) haunt();
				return true;
			}
			if (standing == FENGDU_TICKS) {
				for (SpellConstruct wall : walls) wall.act("fall");
				HeavenSpells.sound(level, ground, SoundEvents.WITHER_BREAK_BLOCK, 5.0f, 0.4f);
			}
			if (standing == FENGDU_TICKS + FENGDU_FALLS) fall();
			return true;
		}

		/** A tick of its standing: whoever would pass out past its walls turned back in. */
		private void hold() {
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, inner + 2, 10, 30)) {
				double dx = e.getX() - ground.x, dz = e.getZ() - ground.z, d = Math.max(1.0e-3, Math.hypot(dx, dz));
				if (d < inner - 4 || HeavenSpells.greatBeast(caster, e)) continue;
				HeavenSpells.move(e, new Vec3(-dx / d * 0.7, 0.15, -dz / d * 0.7));
			}
		}

		/** The city's dead fall on every foe within. */
		private void haunt() {
			for (LivingEntity e : within()) {
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, FENGDU_HARM, FENGDU_DEAD_FACTOR));
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
				HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, HeavenSpells.middle(e), 10, 0.5, 0.05);
			}
			for (int i = 0; i < 6; i++) {
				double a = level.random.nextDouble() * Math.PI * 2, r = inner * level.random.nextDouble();
				HeavenSpells.particles(level, ParticleTypes.SOUL_FIRE_FLAME, ground.add(Math.cos(a) * r, 1 + level.random.nextDouble() * 4, Math.sin(a) * r), 8, 1.0, 0.02);
			}
			HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 4.0f, 0.5f);
		}

		/** Its walls topple in on its heart: every foe within struck. */
		private void fall() {
			for (LivingEntity e : within()) {
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, FENGDU_FALL, FENGDU_DEAD_FACTOR));
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) FENGDU_RADIUS * 1.2f, 0, 256);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 4, 0), Vec3.ZERO, 14, NETHER_TEAL, 256);
			HeavenSpells.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 5.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.WARDEN_SONIC_BOOM, 4.0f, 0.5f);
		}

		@Override
		public void end() {
			for (SpellConstruct wall : walls) if (wall != null) wall.dissolve();
		}
	}

	/**
	 * Ten Thousand Souls Ascend: a helix of ten thousand souls, two strands of the dead winding up about one another, wisps of mist and ghost
	 * fire and pale faces among them (SpellConstruct SOUL_HELIX, its own GeckoLib model), rises out of the dark where its caster looks
	 * ({@link #SOULS_RISES}) and spirals for {@link #SOULS_TICKS}: every foe within {@link #SOULS_PULL} is drawn in to it, and those within
	 * {@link #SOULS_RADIUS} carried up round it as high as {@link #SOULS_HIGH} (a great beast only drawn, slowly), its souls tearing at them,
	 * {@link #SOULS_HARM} of the dead's harm every {@link #SOULS_EVERY} ticks (more to the dead). Then it goes back down into the dark, dragging
	 * them down with it ({@link #SOULS_DESCENDS}): {@link #SOULS_SLAM} to each.
	 */
	public static final class Souls extends HeavenSpells.Timed {
		private final Vec3 ground;
		private final SpellConstruct helix;

		Souls(LivingEntity caster, Vec3 point) {
			super(caster, SOULS_RISES + SOULS_TICKS + SOULS_DESCENDS + SOULS_AFTER);
			Vec3 below = HeavenSpells.groundBelow(level, point.add(0, 2, 0), 4, 48);
			this.ground = below != null ? below : point;
			this.helix = SpellConstruct.create(level, SpellConstruct.Kind.SOUL_HELIX, ground, caster.getYRot(), 0, (int) (end - start), caster).sizedFor(Spell.TEN_THOUSAND_SOULS_ASCEND).spawn();
			HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 5.0f, 0.3f);
			HeavenSpells.sound(level, ground, SoundEvents.WARDEN_EMERGE, 4.0f, 0.7f);
		}

		@Override
		protected boolean run(long age, long now) {
			if (age < SOULS_RISES) return true;
			long spiralling = age - SOULS_RISES;
			if (spiralling < SOULS_TICKS) {
				spiral(spiralling % SOULS_EVERY == 0);
				if (spiralling % 3 == 0) {
					double a = level.random.nextDouble() * Math.PI * 2;
					HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, ground.add(Math.cos(a) * SOULS_RADIUS * 0.7, level.random.nextDouble() * SOULS_HIGH * 3, Math.sin(a) * SOULS_RADIUS * 0.7), 6, 1.0, 0.04);
				}
				if (spiralling % 20 == 0) HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 4.0f, 0.5f);
				return true;
			}
			if (spiralling == SOULS_TICKS) descend();
			return true;
		}

		/** A tick of its spiralling: foes near drawn in, those in its helix carried up round it; torn at if {@code tear}. */
		private void spiral(boolean tear) {
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, SOULS_PULL, 8, SOULS_HIGH + 16)) {
				double dx = e.getX() - ground.x, dz = e.getZ() - ground.z, d = Math.max(1.0e-3, Math.hypot(dx, dz));
				Vec3 in = new Vec3(-dx / d, 0, -dz / d), round = new Vec3(-in.z, 0, in.x), v = e.getDeltaMovement();
				boolean held = d <= SOULS_RADIUS;
				if (!held || HeavenSpells.greatBeast(caster, e)) {
					double pull = HeavenSpells.greatBeast(caster, e) ? 0.1 : 0.45;
					HeavenSpells.move(e, new Vec3(in.x * pull, v.y, in.z * pull));
				} else {
					double lift = net.minecraft.util.Mth.clamp((ground.y + SOULS_HIGH - e.getY()) * 0.1, -0.3, 0.5);
					HeavenSpells.move(e, round.scale(0.6).add(in.scale(0.15)).add(0, lift, 0));
					HeavenSpells.safeFall(e, 60);
				}
				if (held && tear) {
					e.invulnerableTime = 0;
					strike(caster, e, onTheDead(e, SOULS_HARM, SOULS_DEAD_FACTOR));
					HeavenSpells.particles(level, ParticleTypes.SCULK_SOUL, HeavenSpells.middle(e), 8, 0.4, 0.05);
				}
			}
		}

		/** It goes back down into the dark: everything it held dragged down and struck. */
		private void descend() {
			helix.act("descend");
			for (LivingEntity e : WoodSpells.foesWithin(caster, level, ground, SOULS_RADIUS + 2, 8, SOULS_HIGH + 16)) {
				e.invulnerableTime = 0;
				strike(caster, e, onTheDead(e, SOULS_SLAM, SOULS_DEAD_FACTOR));
				if (!HeavenSpells.greatBeast(caster, e)) HeavenSpells.move(e, new Vec3(0, -2.5, 0));
				HeavenSpells.safeFall(e, 100);
			}
			SpellFx.send(level, SpellFx.Kind.SHOCKWAVE, ground, Vec3.ZERO, (float) SOULS_PULL, 0, 256);
			SpellFx.send(level, SpellFx.Kind.FLASH, ground.add(0, 2, 0), Vec3.ZERO, 12, NETHER_TEAL, 256);
			HeavenSpells.sound(level, ground, SoundEvents.WARDEN_SONIC_BOOM, 4.0f, 0.4f);
			HeavenSpells.sound(level, ground, SoundEvents.SOUL_ESCAPE, 5.0f, 0.3f);
		}

		@Override
		public void end() {
			helix.dissolve();
		}
	}

	private GhostSpells() {}
}
