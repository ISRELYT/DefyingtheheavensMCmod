package com.example.defyingtheheavens;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Body cultivators (CultivatorNpc#isBodyCultivator; the user's "Go, with body sects", 2026-11-01): men who temper the body on the Body Path and
 * have no qi. One wandering cultivator in five of the lower realms and the Upper Realm (NpcSpawner#BODY_IN), and every member of a body sect,
 * one new sect of men in {@link #SECT_IN} there (Sect#body: its library and its master's hall keep the body's manuals, MethodManualItem#keptBody).
 * Their rank is the Body Path's, their body mightier than their realm's, and in a fight they wield the body's techniques of their realm
 * ({@link TechniqueGoal}, BodyTechniques#npcAct) where others cast spells; they can't fly after a foe in the air, nor press one down.
 */
public final class BodyCultivators {
	/** One new sect of men in this many is a body sect (decided with its plan, SectStructure, so its library keeps the body's manuals). */
	public static final int SECT_IN = 6;
	/**
	 * The highest a body cultivator climbs: the Indestructible Body's Grand Perfection. A player's Body Path goes on up the ladder (BodyPath#LAST_RANK);
	 * no body cultivator of the world's does (the user's "Go, all three parts", 2026-11-01).
	 */
	public static final int LAST_RANK = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
	/** Ticks between its techniques in a fight: at least this many, and up to {@link #SPREAD} more. */
	static final int EVERY = 30, SPREAD = 30;
	/**
	 * It first uses one this long into a fight: at once, near enough, to close on its foe with a leap or a charge before it has run the distance
	 * (seen in game: at twenty ticks a swift body had already closed it on foot, and never leapt).
	 */
	static final int FIRST = 6;
	/** Struck within this many ticks: it is under blows (Iron Skin, Golden Body, Rebounding Bones). */
	static final int STRUCK = 40;
	/** A fight this long (ticks) is a long one: its blood boils (Blood Frenzy). */
	static final int LONG_FIGHT = 160;
	/** Its Blood Frenzy, burnt out, isn't lit again for this long after (ticks). */
	static final int FRENZY_REST = 600;
	/** Below this share of its health it mends its flesh (Flesh Reborn); below this, under blows, it turns to gold (Golden Body). */
	static final float LOW = 0.35f, HURT = 0.5f;
	/** Its foe within this (blocks) is at hand: it primes its fist; its stomp reaches one foe alone this near. */
	static final double AT_HAND = 4, STOMP_ONE = 3;
	/** It leaps at a foe this far off or more, and charges at one this far off or more. */
	static final double LEAP_FROM = 5, CHARGE_FROM = 6;
	/** A foe this far above or below it is out of its reach on foot (one in the air is safe from it). */
	static final double REACH_UP = 4;

	/**
	 * In a fight, a technique of its realm every second or two (see the class notes), each on its own cooldown (a player's: BodyTechniques#cooldown):
	 * low, its flesh mended; under blows, gold or iron skin, its bones turned on its foe at hand; harried by harm or pressed down, its will made
	 * diamond; its foe off at a distance on the ground, a leap or a charge at it; crowded, the stomp; its foe at hand, its fist primed; a long
	 * fight, its blood boiling; at the Indestructible Body, the shattering fist at a foe before it. Running for its life, only its guard and its
	 * mending. In a spar, nothing of a realm past the one it holds back to (BodyTechniques#rankOf). Alongside the fight's own goal: it takes no
	 * move or look of its own.
	 */
	public static final class TechniqueGoal extends Goal {
		private final CultivatorNpc npc;
		/** When each technique may be used again (game time): its own, gone with it. */
		private final Map<Ability, Long> ready = new EnumMap<>(Ability.class);
		private long next, fightFrom;

		public TechniqueGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.noneOf(Flag.class));
		}

		private boolean fit() {
			if (!npc.isBodyCultivator() || npc.isMortal() || !npc.isAlive() || npc.isVacant() || npc.isBegging() || npc.isKneeling()) return false;
			LivingEntity foe = npc.getTarget();
			return foe != null && CultivatorGoals.CombatGoal.fights(npc, foe) || npc.shouldFlee() && struck();
		}

		@Override public boolean canUse() { return fit(); }
		@Override public boolean canContinueToUse() { return fit(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void start() {
			long now = npc.level().getGameTime();
			next = now + FIRST + npc.getRandom().nextInt(FIRST);
			fightFrom = now;
		}

		@Override
		public void tick() {
			long now = npc.level().getGameTime();
			if (now < next) return;
			LivingEntity foe = npc.getTarget();
			Ability technique = choose(foe, now);
			if (technique == null || !BodyTechniques.npcAct(npc, technique, foe)) {
				next = now + 10; // nothing fit for now: it looks again in half a second
				return;
			}
			int rest = technique == Ability.BLOOD_FRENZY ? BodyTechniques.NPC_FRENZY_TICKS + FRENZY_REST : BodyTechniques.cooldown(technique);
			ready.put(technique, now + rest);
			next = now + EVERY + npc.getRandom().nextInt(SPREAD);
		}

		/** Struck a moment ago. */
		private boolean struck() {
			return npc.getLastHurtByMob() != null && npc.tickCount - npc.getLastHurtByMobTimestamp() < STRUCK;
		}

		/** Whether it has {@code technique} at the realm it fights at, off its cooldown and not holding already. */
		private boolean may(Ability technique, int rank, long now) {
			return BodyTechniques.knows(rank, technique) && ready.getOrDefault(technique, 0L) <= now && !BodyTechniques.holding(npc, technique);
		}

		/** The technique it would use now against {@code foe} (null: running, with no foe to strike), or null for none (see the goal's notes). */
		Ability choose(LivingEntity foe, long now) {
			int rank = BodyTechniques.rankOf(npc);
			if (rank < 0) return null;
			float health = npc.getHealth() / Math.max(1, npc.getMaxHealth());
			boolean struck = struck();
			if (health < LOW && may(Ability.FLESH_REBORN, rank, now)) return Ability.FLESH_REBORN;
			if (struck && health < HURT && may(Ability.GOLDEN_BODY, rank, now)) return Ability.GOLDEN_BODY;
			if ((harried() || npc.cultivation().getPressureStages() > 0) && may(Ability.DIAMOND_WILL, rank, now)) return Ability.DIAMOND_WILL;
			if (struck && may(Ability.IRON_SKIN, rank, now)) return Ability.IRON_SKIN;
			if (foe == null || npc.shouldFlee() || !CultivatorGoals.CombatGoal.fights(npc, foe)) return null;
			double distance = npc.distanceTo(foe);
			boolean seen = npc.getSensing().hasLineOfSight(foe);
			// A foe in the air above it, or far below, is out of the body's reach.
			boolean onFoot = Math.abs(foe.getY() - npc.getY()) <= REACH_UP;
			if (struck && distance <= AT_HAND && may(Ability.REBOUNDING_BONES, rank, now)) return Ability.REBOUNDING_BONES;
			if (seen && onFoot && distance <= BodyTechniques.SHATTER_REACH * 0.8 && may(Ability.HEAVEN_SHATTERING_FIST, rank, now)) {
				return Ability.HEAVEN_SHATTERING_FIST;
			}
			if (npc.onGround() && seen && onFoot) {
				if (distance >= CHARGE_FROM && distance <= BodyTechniques.cometBlocks(rank) + 2 && may(Ability.COMET_CHARGE, rank, now)) return Ability.COMET_CHARGE;
				double leap = BodyTechniques.leapForward(rank) * BodyTechniques.LEAP_BLOCKS_PER_FORWARD;
				if (distance >= LEAP_FROM && distance <= leap && may(Ability.GROUND_SPLITTING_LEAP, rank, now)) return Ability.GROUND_SPLITTING_LEAP;
			}
			if (npc.onGround() && may(Ability.MOUNTAIN_STOMP, rank, now)
					&& (foesWithin(BodyTechniques.stompRadius(rank)) >= 2 || onFoot && distance <= STOMP_ONE)) {
				return Ability.MOUNTAIN_STOMP;
			}
			if (onFoot && distance <= AT_HAND && may(Ability.THOUSAND_POUND_FIST, rank, now)) return Ability.THOUSAND_POUND_FIST;
			if (now - fightFrom >= LONG_FIGHT && may(Ability.BLOOD_FRENZY, rank, now)) return Ability.BLOOD_FRENZY;
			return null;
		}

		/** Harried: a harmful effect on it (poison, slowness, weakness...). */
		private boolean harried() {
			for (MobEffectInstance effect : npc.getActiveEffects()) {
				if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) return true;
			}
			return false;
		}

		/** How many it fights stand within {@code radius} of it. */
		private int foesWithin(double radius) {
			return npc.level().getEntitiesOfClass(LivingEntity.class, npc.getBoundingBox().inflate(radius, 2, radius),
					e -> e != npc && e.isAlive() && e.distanceToSqr(npc) <= radius * radius && HeavenSpells.fightsWith(npc, e)).size();
		}
	}

	/**
	 * A body cultivator's record as a nascent soul possessing it lives it (Possession#enter): a mortal at Peak on the Body Path at its rank, drawn
	 * in, its stage's progress carried over (the two paths' bars are the same length), no qi; given back, its rank is the Body Path's it came to
	 * (Possession, the soul leaving). Its qi realm's fields stay as they were beneath, for its own life after.
	 */
	static void asBody(CultivatorNpc npc, PlayerCultivation record) {
		double progress = record.getCultivation();
		record.setMortal(true);
		record.setMortalState(MortalStage.PEAK, 0);
		record.body().set(Math.max(0, Math.min(BodyPath.LAST_RANK, npc.getRank())), progress);
		record.body().setCultivating(true);
	}

	private BodyCultivators() {}
}
