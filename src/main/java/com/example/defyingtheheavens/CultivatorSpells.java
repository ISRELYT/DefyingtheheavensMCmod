package com.example.defyingtheheavens;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Cultivators casting spells (the user's ask, 2026-10-24: "I want NPC cultivators to be able to use spells"). A living cultivator
 * knows the spells of its heaven method's element (CultivatorNpc#heavenMethod: its sect's, its blood's, its own) up to its method's
 * realm and as far as its rank has come, as a player who knows that method; one of the dead, its ghost foundation's
 * (GhostCultivatorNpc#method). In a fight ({@link SpellGoal}) it casts one every few seconds, out of its own qi and on its own
 * cooldowns (Spells#castBy), facing what it casts at: a ward or a mending when it is hurt, a spell about itself when its foe is
 * close, else one at its foe, the deepest it has the more often. Its spells reach only whom it fights (HeavenSpells#fightsWith) and
 * leave the land as it was (HeavenSpells#carvesLand: no crater, cut, lake or fire stays). In a spar it casts no spell deeper than
 * the realm it holds back to.
 */
public final class CultivatorSpells {
	/** What a spell is to a cultivator choosing one: aimed at its foe, laid on its foe's ground, about itself, a ward, a mending, a helper. */
	enum Use { AT_FOE, ON_FOE, ABOUT, WARD, MEND, HELPER, NONE }

	/** Ticks between its casts in a fight: at least this many, and up to {@link #CAST_SPREAD} more. */
	static final int CAST_EVERY = 70, CAST_SPREAD = 50;
	/** It first casts this long into a fight (a breath to size its foe up). */
	static final int FIRST_CAST = 25;
	/** Below this share of its health it looks to a ward or a mending first. */
	static final float HURT_SHARE = 0.5f;
	/** Flying, it keeps this many seconds of its flight's qi back from any spell. */
	static final double FLIGHT_RESERVE_SECONDS = 10;

	/** How {@code spell} serves a cultivator, and (for one at or about its foe) how far it reaches. */
	static Use use(Spell spell) {
		return switch (spell) {
			case GHOST_FIRE_BOLT, SOUL_REND, YAMA_VERDICT, FLAME_BOLT, WOOD_BULLET, EARTH_SPEAR, WATER_ARROW, FLYING_SWORD, STEEL_HURRICANE,
					FIRE_SERPENT, LIFE_FORCE_DRAIN, MYSTERIOUS_BRISTLE_PRISON, WATER_PRISON, BOULDER_CLEAVE, ADVANCED_FLYING_SWORD_TECHNIQUE,
					THOUSAND_LEAF_STORM, EARTH_DRAGON_SURGE, TIDAL_LANCE, KARMIC_FIRE, GLACIAL_SPEARS, GREAT_TIDE, PIERCING_SWORD_LIGHT,
					PHOENIX_DESCENT, HEAVENLY_RIVER_TORRENT, MOUNTAIN_CLEAVE, ABSOLUTE_ZERO, HEAVEN_SEVERING_SWORD, VERMILION_BIRD_DESCENT,
					QILIN_TRAMPLES_THE_LAND, PROCESSION_OF_TEN_THOUSAND_GHOSTS, VOID_SEVERING_SWORD_LIGHT, BRIDGE_OF_NO_RETURN -> Use.AT_FOE;
			case TORRENTIAL_SPRING, CRIMSON_SWIRL, SWORD_RAIN, BLAZING_METEOR, THORN_SPEAR_FOREST, SEA_OF_FLAMES, HEAVEN_PIERCING_BAMBOO,
					FALLING_MOUNTAIN, NINE_SUNS_DESCEND, MYRIAD_VINE_BINDING, WORLD_TREE, MOUNTAIN_SEAL, BOUNDLESS_SEA, FOUR_SWORDS_ARRAY, AZURE_DRAGON,
					ONE_FLAME_BURNS_THE_HEAVENS, SEAL_OF_TEN_THOUSAND_MOUNTAINS, HUNDRED_RIVERS_CONVERGE, WHITE_TIGER_POUNCE, MYRIAD_SWORDS_RETURN,
					GATE_OF_TEN_THOUSAND_GHOSTS, SPRING_THUNDER_OF_THE_AZURE_EMPEROR, VOID_WITHERING, GOLDEN_CROW_DESCENDS, SUN_THROUGH_THE_VOID,
					BUZHOU_FALLS, WEAK_WATER_FROM_THE_VOID, KUN_RISES_FROM_THE_ABYSS, TEN_DIRECTION_SWORD_PRISON, RIFT_TO_THE_NINE_NETHERS -> Use.ON_FOE;
			case GHOSTLY_WAIL, YELLOW_SPRINGS_TIDE, GHOST_CORE_BURST, KINGS_DECREE, HELL_CHAINS, SIX_PATHS_ANNIHILATION, RAIN_CALL, SOLAR_FIELD,
					EARTHQUAKE, HEAVY_EARTH_SEAL, FROZEN_FIELD, SWORD_DOMAIN, MYRIAD_WOODS_RETURN, EARTH_VEINS_LOCK_THE_VOID -> Use.ABOUT;
			case MARROW_WARD, FLAME_FIST, CRIMSON_TEMPERING, HARDENED_BODY, AZURE_COATING_TECHNIQUE, BLADE_POLISH, FLAME_ARMOUR, ANCIENT_BARK_SKIN,
					MOUNTAIN_STANCE, MIST_VEIL, GOLDEN_BELL_SHIELD, VAJRA_BODY, PHOENIX_NIRVANA, BLACK_TORTOISE_BASTION -> Use.WARD;
			case SAMSARA_WHEEL, EMERALD_HEALING_ART, LOTUS_OF_REBIRTH, SPRING_OF_MYRIAD_LIFE -> Use.MEND;
			case SUMMON_GOLEM, EARTHEN_TITAN -> Use.HELPER;
			// A player's alone: what moves or carries the caster (dashes, steps, waves, a tree, water walking), a bomb set off by a second
			// cast, what takes the land or its ore (a boulder torn out, a cave, Mineral Purification), a life given, a wall, the hands' fire.
			default -> Use.NONE;
		};
	}

	/** How far {@code spell} reaches its foe (blocks), for one aimed at or laid on it; about itself, how far round it. */
	static double reach(Spell spell) {
		return switch (spell) {
			case STEEL_HURRICANE -> 10;
			case LIFE_FORCE_DRAIN -> 12;
			case BOULDER_CLEAVE, WATER_PRISON -> 15;
			case MYSTERIOUS_BRISTLE_PRISON -> 20;
			case GHOST_FIRE_BOLT, EARTH_DRAGON_SURGE, TIDAL_LANCE, HEAVEN_PIERCING_BAMBOO -> 24;
			case GREAT_TIDE -> 28;
			case GHOST_CORE_BURST -> 5;
			case YELLOW_SPRINGS_TIDE -> 6;
			case GHOSTLY_WAIL, RAIN_CALL -> 8;
			case SWORD_DOMAIN -> 10;
			case HELL_CHAINS, SIX_PATHS_ANNIHILATION -> 12;
			case KINGS_DECREE -> 16;
			case EARTHQUAKE -> 20;
			case HEAVY_EARTH_SEAL -> 24;
			case SOLAR_FIELD, FROZEN_FIELD -> 30;
			case FLAME_BOLT, WOOD_BULLET, EARTH_SPEAR, WATER_ARROW, CRIMSON_SWIRL -> 40;
			case FLYING_SWORD, FIRE_SERPENT, ADVANCED_FLYING_SWORD_TECHNIQUE, BLAZING_METEOR, SEA_OF_FLAMES, MOUNTAIN_SEAL, BOUNDLESS_SEA,
					FOUR_SWORDS_ARRAY, ABSOLUTE_ZERO -> 48;
			case HEAVEN_SEVERING_SWORD -> 60;
			case PIERCING_SWORD_LIGHT, PHOENIX_DESCENT, MOUNTAIN_CLEAVE, FALLING_MOUNTAIN, NINE_SUNS_DESCEND, MYRIAD_VINE_BINDING -> 64;
			// Dao Integration's, as far as they reach (their reach by their realm: HeavenSpells#span).
			case AZURE_DRAGON -> WoodSpells.DRAGON_RANGE;
			case ONE_FLAME_BURNS_THE_HEAVENS -> FireSpells.FLAME_RANGE;
			case SEAL_OF_TEN_THOUSAND_MOUNTAINS -> EarthSpells.MOUNTAINS_RANGE;
			case HUNDRED_RIVERS_CONVERGE -> WaterSpells.RIVERS_RANGE;
			case WHITE_TIGER_POUNCE -> MetalSpells.TIGER_RANGE;
			case MYRIAD_SWORDS_RETURN -> MetalSpells.SWORDS_RANGE;
			case MYRIAD_WOODS_RETURN -> WoodSpells.WOODS_RADIUS;
			case QILIN_TRAMPLES_THE_LAND -> EarthSpells.QILIN_RUN * 0.9;
			case VERMILION_BIRD_DESCENT -> FireSpells.BIRD_SWEEP * 0.85;
			case PROCESSION_OF_TEN_THOUSAND_GHOSTS -> GhostSpells.PROCESSION_RANGE * 0.9;
			case GATE_OF_TEN_THOUSAND_GHOSTS -> GhostSpells.GATE_RANGE;
			// Void Shattering's.
			case SPRING_THUNDER_OF_THE_AZURE_EMPEROR -> WoodSpells.THUNDER_RANGE;
			case VOID_WITHERING -> WoodSpells.WITHER_RANGE;
			case GOLDEN_CROW_DESCENDS -> FireSpells.CROW_RANGE;
			case SUN_THROUGH_THE_VOID -> FireSpells.SUN_RANGE;
			case BUZHOU_FALLS -> EarthSpells.BUZHOU_RANGE;
			case EARTH_VEINS_LOCK_THE_VOID -> EarthSpells.LOCK_RADIUS;
			case WEAK_WATER_FROM_THE_VOID -> WaterSpells.WEAK_RANGE;
			case KUN_RISES_FROM_THE_ABYSS -> WaterSpells.KUN_RANGE;
			case VOID_SEVERING_SWORD_LIGHT -> MetalSpells.SEVERING_LENGTH * 0.9;
			case TEN_DIRECTION_SWORD_PRISON -> MetalSpells.PRISON_RANGE;
			case RIFT_TO_THE_NINE_NETHERS -> GhostSpells.NETHERS_RANGE;
			case BRIDGE_OF_NO_RETURN -> GhostSpells.BRIDGE_LENGTH * 0.9;
			default -> 32;
		};
	}

	/** Whether {@code npc} knows {@code spell} and may cast it now (its rank: in a spar, the one it holds back to). */
	static boolean knows(CultivatorNpc npc, Spell spell) {
		if (use(spell) == Use.NONE || spell.isDual()) return false;
		int rank = npc.fightingRank();
		if (rank < 0 || rank < spell.realmNeeded()) return false;
		if (spell.isHeaven()) {
			HeavenMethod method = npc.heavenMethod();
			return method != null && method.element() == spell.element() && spell.heavenRealm().ordinal() <= method.realm().ordinal();
		}
		if (!(npc instanceof GhostCultivatorNpc ghost) || spell.method() == null) return false;
		GhostMethod known = ghost.method();
		if (known == null) return false;
		if (known.realm() == null) return spell.method() == known; // an art alone (the fire, the steps): its own spells
		// A foundation: the dead's arts (no realm of their own) come with it, and every foundation's spells up to its own.
		return spell.method().realm() == null || spell.method().realm().ordinal() <= known.realm().ordinal();
	}

	/** Every spell {@code npc} knows now. */
	public static List<Spell> repertoire(CultivatorNpc npc) {
		List<Spell> known = new ArrayList<>();
		if (npc.isMortal()) return known;
		for (Spell spell : Spell.values()) if (knows(npc, spell)) known.add(spell);
		return known;
	}

	/**
	 * The spell {@code npc} would cast at {@code foe} now, or null: what it knows, off cooldown, its qi enough (a flier keeps its
	 * flight's reserve back), and fit for the moment (hurt: a ward or a mending; its foe in a spell's reach: that spell), the deepest
	 * more often than the rest.
	 */
	static Spell choose(CultivatorNpc npc, LivingEntity foe) {
		PlayerCultivation c = npc.cultivation();
		double reserve = npc.isQiFlying() ? c.qiFlightCostPerSecond() * FLIGHT_RESERVE_SECONDS : 0;
		double distance = npc.distanceTo(foe);
		boolean seen = npc.getSensing().hasLineOfSight(foe);
		boolean hurt = npc.getHealth() < npc.getMaxHealth() * HURT_SHARE;
		List<Spell> mending = new ArrayList<>(), striking = new ArrayList<>();
		for (Spell spell : repertoire(npc)) {
			if (Spells.cooldownLeft(npc, spell) > 0 || c.getQi() - spell.qiCost() - spell.cost() < reserve) continue;
			Use use = use(spell);
			switch (use) {
				case MEND, WARD -> { if (hurt) mending.add(spell); }
				case HELPER -> striking.add(spell);
				case ABOUT -> { if (distance <= reach(spell) * 0.85) striking.add(spell); }
				default -> { if (seen && distance <= reach(spell)) striking.add(spell); }
			}
		}
		if (!mending.isEmpty()) return deepest(npc, mending);
		return striking.isEmpty() ? null : deepest(npc, striking);
	}

	/** One of {@code spells}, the deepest realm's twice as likely as the next, and so on down. */
	private static Spell deepest(CultivatorNpc npc, List<Spell> spells) {
		spells.sort((a, b) -> Integer.compare(b.layer(), a.layer()));
		for (Spell spell : spells) if (npc.getRandom().nextInt(2) == 0) return spell;
		return spells.get(0);
	}

	/** Turns {@code npc} to face {@code foe}'s middle at once, so the spell's aim (its look: getViewVector) goes where it means. */
	static void face(CultivatorNpc npc, LivingEntity foe) {
		Vec3 to = HeavenSpells.middle(foe).subtract(npc.getEyePosition());
		float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0f;
		float pitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);
		npc.setYRot(yaw);
		npc.setYHeadRot(yaw);
		npc.yBodyRot = yaw;
		npc.setXRot(pitch);
	}

	/** In a fight, a spell every few seconds (see the class notes). Alongside the fight's own goal: it takes no move or look of its own. */
	public static final class SpellGoal extends Goal {
		private final CultivatorNpc npc;
		private long nextCast;

		public SpellGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.noneOf(Flag.class));
		}

		private boolean fit() {
			LivingEntity foe = npc.getTarget();
			return foe != null && !npc.isMortal() && !npc.isVacant() && !npc.isBegging() && !npc.isKneeling() && !npc.shouldFlee()
					&& CultivatorGoals.CombatGoal.fights(npc, foe);
		}

		@Override public boolean canUse() { return fit(); }
		@Override public boolean canContinueToUse() { return fit(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void start() {
			nextCast = npc.level().getGameTime() + FIRST_CAST + npc.getRandom().nextInt(CAST_SPREAD / 2);
		}

		@Override
		public void tick() {
			LivingEntity foe = npc.getTarget();
			long now = npc.level().getGameTime();
			if (foe == null || now < nextCast) return;
			Spell spell = choose(npc, foe);
			if (spell == null) {
				nextCast = now + 20; // nothing fit for now: it looks again in a second
				return;
			}
			face(npc, foe);
			nextCast = now + (Spells.castBy(npc, spell) ? CAST_EVERY + npc.getRandom().nextInt(CAST_SPREAD) : 20);
		}
	}

	private CultivatorSpells() {}
}
