package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Laws (phase 3 of the realm plan, 2026-10-26; the user's picks: the five elements' Laws and three greater ones, a weathered Decay's
 * level chosen by the player): what the realms past True Immortal are built on, comprehended level by level ({@link #MAX_LEVEL} each,
 * LawState) by True Immortals and those past them.
 * <ul>
 *   <li><b>The way up.</b> Each of the ladder's breakthroughs, Earth Immortal to Heavenly Lord, wants so many levels of the Laws together
 *       ({@link #needed}: 3, 7, 12, 18, 25, 35), on whichever path it is cultivated (HeavenMethods#opensWay, GhostMethods#opensWay,
 *       DemonPath#heldByLaws): no method or blood carries one past True Immortal.</li>
 *   <li><b>Comprehended</b> by meditating in the Immortal Realm with a Law in focus ({@link #meditate}: the realm keeps no steles, the
 *       user's rule), level L wanting L x {@link #INSIGHT_SECONDS} of it at the meditation's pace (in the First Flame's light, FirstFlame,
 *       {@link FirstFlame#INSIGHT_PACE} times as fast); in a rare glimpse, a foe of Entering Nirvana or above slain by an element's spell
 *       ({@link #onDeath}); from a Dao Fragment taken in ({@link #absorb}); a level outright for each Five Decays weathered (FiveDecays),
 *       chosen on the Methods tab's Laws page ({@link #choose}). The greater Laws open to it from Golden Immortal on ({@link #GREATER_FROM}).</li>
 *   <li><b>What they give</b>: an element's Law its spells {@link #HARM_PER_LEVEL} harder and {@link #REACH_PER_LEVEL} farther-reaching a
 *       level ({@link #outgoing}, {@link #reach}); Space the Void Shattering realm's spells as much harder, their reach and the steps
 *       through the dark (Nine Nether Step, Soul Dash) as much farther, and the Spatial Tear {@link #COOLDOWN_PER_LEVEL} sooner ready; Time
 *       every spell's cooldown as much shorter a level ({@link #cooldown}); Life and Death the ghost path's spells {@link #LIFE_PER_LEVEL}
 *       harder and all the healing one takes as much greater ({@link #healed}). Players' alone: the realm's own immortals have their
 *       realms' might.</li>
 * </ul>
 */
public final class Laws {
	public static final int MAX_LEVEL = 9;
	/** The levels of the Laws together that breaking into each of the ladder's realms wants, Earth Immortal to Heavenly Lord. */
	private static final int[] NEEDED = {3, 7, 12, 18, 25, 35};
	/** The greater Laws (Space, Time, Life and Death) open to comprehension from this realm on. */
	public static final Realm GREATER_FROM = Realm.GOLDEN_IMMORTAL;
	public static final float HARM_PER_LEVEL = 0.10f, LIFE_PER_LEVEL = 0.10f;
	public static final double REACH_PER_LEVEL = 0.05, COOLDOWN_PER_LEVEL = 0.03;
	/** Seconds of meditation in the Immortal Realm a level wants, times the level: the first a quarter of an hour, the ninth two and a quarter. */
	public static final double INSIGHT_SECONDS = 900;
	/** A foe of this rank or above, slain by an element's spell, may show its Law: this often, worth this share of its next level. */
	public static final int GLIMPSE_FROM = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY);
	public static final float GLIMPSE_CHANCE = 0.25f;
	public static final double GLIMPSE_SHARE = 0.25;
	/** A Dao Fragment taken in (DaoFragmentItem, the Chaos Frontier's) is worth this share of its Law's next level. */
	public static final double FRAGMENT_SHARE = 0.5;

	/** The spell being cast this moment and by whom (Spells#perform), for its reach ({@link #reach}) and its Space ({@link #outgoing}). */
	private static Spell casting;
	private static LivingEntity castBy;

	// --- The way up ---

	/** The levels of the Laws together that breaking into {@code realm} wants: none short of the ladder. */
	public static int needed(Realm realm) {
		return realm.isPastTrueImmortal() ? NEEDED[Math.min(NEEDED.length, realm.pastTrueImmortal()) - 1] : 0;
	}

	/** Whether {@code c}'s Laws open the way into {@code realm}: any realm short of the ladder, or the levels it wants held. */
	public static boolean opensWay(PlayerCultivation c, Realm realm) {
		return !realm.isPastTrueImmortal() || c.laws().total() >= needed(realm);
	}

	/** Why a breakthrough into {@code realm} waits on the Laws: so many levels wanted, so many held. */
	public static Component gateLine(PlayerCultivation c, Realm realm) {
		return Component.translatable(ModLang.LAW_GATE, realm.getDisplayName(), needed(realm), c.laws().total());
	}

	// --- Who comprehends them ---

	/** Whether {@code c} stands where the Laws are comprehended: a True Immortal or past it, on its higher path. */
	public static boolean isImmortal(PlayerCultivation c) {
		return !c.isMortal() && TwoPaths.higherRank(c, false) >= PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
	}

	/** Whether the greater Laws are open to {@code c}: from {@link #GREATER_FROM} on. */
	public static boolean greaterOpen(PlayerCultivation c) {
		return !c.isMortal() && TwoPaths.higherRank(c, false) >= PlayerCultivation.rank(GREATER_FROM, Stage.EARLY);
	}

	/** Whether {@code c} can comprehend {@code law} further: an immortal, the Law not complete, a greater one only from Golden Immortal. */
	public static boolean mayComprehend(PlayerCultivation c, Law law) {
		return law != null && isImmortal(c) && c.laws().level(law) < MAX_LEVEL && (!law.isGreater() || greaterOpen(c));
	}

	/** Seconds of insight level {@code level} wants (from the one before). */
	public static double insightFor(int level) {
		return Math.max(1, level) * INSIGHT_SECONDS;
	}

	// --- Comprehending ---

	/**
	 * Half a second (or so: {@code seconds}, at the meditation's pace) of {@code p}'s meditation (MeditationManager, whichever qi it draws
	 * in): in the Immortal Realm, or a small world refined there (its qi the realm's: SmallWorlds), the Law in focus comprehended by it, in
	 * the First Flame's light {@link FirstFlame#INSIGHT_PACE} times as fast; by where the body sits, so a soul turned inward (InnerRealm)
	 * goes on.
	 */
	static void meditate(ServerPlayer p, PlayerCultivation c, double seconds) {
		Law law = c.laws().focus();
		if (seconds <= 0 || !ModDimensions.hasImmortalQi(InnerRealm.bodyDimension(p)) || !mayComprehend(c, law)) return;
		boolean flame = FirstFlame.inLight(InnerRealm.bodyDimension(p), net.minecraft.world.phys.Vec3.atBottomCenterOf(InnerRealm.bodyPos(p)));
		gain(p, c, law, seconds * (flame ? FirstFlame.INSIGHT_PACE : 1));
		CultivationManager.sync(p); // the Laws page's bar
	}

	/**
	 * A Dao Fragment of {@code law} taken in by {@code p} (DaoFragmentItem): {@link #FRAGMENT_SHARE} of its next level's insight, told; or,
	 * if {@code p} may not comprehend it, told why not. @return whether it was taken in
	 */
	public static boolean absorb(ServerPlayer p, Law law) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!mayComprehend(c, law)) {
			p.displayClientMessage(whyNot(c, law), true);
			return false;
		}
		p.sendSystemMessage(Component.translatable(ModLang.MSG_DAO_FRAGMENT, law.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.6f);
		p.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
		gain(p, c, law, FRAGMENT_SHARE * insightFor(c.laws().level(law) + 1));
		CultivationManager.sync(p);
		return true;
	}

	/** Why {@code c} can't comprehend {@code law} further: a greater Law shut to them yet, the Law complete, or not an immortal. */
	static Component whyNot(PlayerCultivation c, Law law) {
		return Component.translatable(law.isGreater() && isImmortal(c) && !greaterOpen(c) ? ModLang.MSG_LAW_GREATER_SHUT
				: c.laws().level(law) >= MAX_LEVEL ? ModLang.MSG_LAW_COMPLETE : ModLang.MSG_LAW_NOT_IMMORTAL, law.getDisplayName());
	}

	/** {@code seconds} of insight into {@code law} for {@code p}: its levels rise as it fills, each told. @return the levels gained */
	static int gain(ServerPlayer p, PlayerCultivation c, Law law, double seconds) {
		LawState s = c.laws();
		double insight = s.insight(law) + seconds;
		int gained = 0;
		while (s.level(law) < MAX_LEVEL && insight >= insightFor(s.level(law) + 1)) {
			insight -= insightFor(s.level(law) + 1);
			s.setLevel(law, s.level(law) + 1);
			gained++;
		}
		s.setInsight(law, s.level(law) >= MAX_LEVEL ? 0 : insight);
		CultivationManager.markDirty(p.server);
		if (gained > 0) comprehended(p, c, law);
		return gained;
	}

	/** {@code law} a level deeper for {@code p} outright (a Decay weathered and chosen): told. @return whether it rose */
	static boolean raise(ServerPlayer p, PlayerCultivation c, Law law) {
		LawState s = c.laws();
		if (law == null || s.level(law) >= MAX_LEVEL) return false;
		s.setLevel(law, s.level(law) + 1);
		s.setInsight(law, 0);
		CultivationManager.markDirty(p.server);
		comprehended(p, c, law);
		return true;
	}

	/** {@code law} has risen a level for {@code p}: told, heard, seen; and told too if it has opened the way into the realm ahead. */
	private static void comprehended(ServerPlayer p, PlayerCultivation c, Law law) {
		p.sendSystemMessage(Component.translatable(ModLang.MSG_LAW_LEVEL, law.getDisplayName(), c.laws().level(law), MAX_LEVEL).withStyle(ChatFormatting.GOLD));
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 0.7f);
		p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.6, 0.9, 0.6, 0.08);
		int rank = TwoPaths.higherRank(c, false);
		Realm ahead = Realm.byIndex(rank / Stage.values().length + 1);
		if (ahead.isPastTrueImmortal() && c.laws().total() == needed(ahead)) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_LAW_WAY_OPEN, ahead.getDisplayName()).withStyle(ChatFormatting.GOLD));
		}
		CultivationManager.sync(p);
	}

	/** The Methods tab's Laws page (C2S law_focus): {@code law} the one meditation comprehends now (null: none), if it may be. */
	public static void focus(ServerPlayer p, Law law) {
		PlayerCultivation c = CultivationManager.get(p);
		if (law != null && !mayComprehend(c, law)) {
			p.displayClientMessage(whyNot(c, law), true);
			return;
		}
		c.laws().setFocus(law);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		p.displayClientMessage(law == null ? Component.translatable(ModLang.MSG_LAW_UNFOCUSED)
				: Component.translatable(ModLang.MSG_LAW_FOCUSED, law.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
	}

	/** The Methods tab's Laws page (C2S law_choose): one of the levels owed for a weathered Decay spent on {@code law}. */
	public static void choose(ServerPlayer p, Law law) {
		PlayerCultivation c = CultivationManager.get(p);
		LawState s = c.laws();
		if (law == null || s.choices() <= 0) return;
		if (!mayComprehend(c, law)) {
			focus(p, law); // told why not, as the focus would be
			return;
		}
		s.setChoices(s.choices() - 1);
		raise(p, c, law);
	}

	/**
	 * AFTER_DEATH: {@code victim} slain by a player's element spell ({@code source}: its qi), of Entering Nirvana or above: {@link
	 * #GLIMPSE_CHANCE}, a glimpse of that element's Law for one who may comprehend it, {@link #GLIMPSE_SHARE} of its next level.
	 */
	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer killer) || killer == victim || !killer.isAlive()) return;
		Law law = Law.of(elementOf(source));
		if (law == null || rankOf(victim) < GLIMPSE_FROM) return;
		PlayerCultivation c = CultivationManager.get(killer);
		if (!mayComprehend(c, law) || killer.getRandom().nextFloat() >= GLIMPSE_CHANCE) return;
		killer.sendSystemMessage(Component.translatable(ModLang.MSG_LAW_GLIMPSE, law.getDisplayName()).withStyle(ChatFormatting.GOLD));
		gain(killer, c, law, GLIMPSE_SHARE * insightFor(c.laws().level(law) + 1));
		CultivationManager.sync(killer);
	}

	/** Where {@code e} stands, for a glimpse: a cultivator's rank, a player's higher path's, a beast's; -1 for a mortal or anything else. */
	private static int rankOf(LivingEntity e) {
		if (e instanceof CultivatorNpc npc) return npc.isMortal() ? -1 : npc.getRank();
		if (e instanceof DemonBeast beast) return beast.getRank();
		if (e instanceof ServerPlayer p) return TwoPaths.higherRank(CultivationManager.get(p), false);
		return -1;
	}

	/** The element of a heaven spell's qi ({@code source}: ModDamageTypes#qiType), or null for any other harm. */
	public static QiElement elementOf(DamageSource source) {
		for (QiElement element : QiElement.five()) if (source.is(ModDamageTypes.qiType(element))) return element;
		return null;
	}

	// --- What they give ---

	/**
	 * A blow or spell of {@code caster}'s (Physiques#outgoing): an element's spell (its qi) by that element's Law; a spell of Void
	 * Shattering's realm (its qi says so, or it is that spell's own cast this moment) by Space; the ghost path's spells (the dead's harm,
	 * not one of the dead's own blows) by Life and Death.
	 */
	public static float outgoing(ServerPlayer caster, DamageSource source, float amount) {
		LawState s = CultivationManager.get(caster).laws();
		float factor = 1;
		Law law = Law.of(elementOf(source));
		if (law != null) factor *= 1 + HARM_PER_LEVEL * s.level(law);
		if (source instanceof QiDamageSource qi && qi.realm() == Realm.VOID_SHATTERING || castingSpatial(caster)) {
			factor *= 1 + HARM_PER_LEVEL * s.level(Law.SPACE);
		}
		if (source.is(ModDamageTypes.GHOSTLY) && !GhostSystem.isBlow()) factor *= 1 + LIFE_PER_LEVEL * s.level(Law.LIFE_AND_DEATH);
		return amount * factor;
	}

	/** A spell Space strengthens: one of Void Shattering's realm (space torn open, and what comes through it), the steps through the dark. */
	public static boolean isSpatial(Spell spell) {
		return spell == Spell.NINE_NETHER_STEP || spell == Spell.SOUL_DASH || spell.realm() == Realm.VOID_SHATTERING;
	}

	/** Whether {@code caster} is casting a spell of space this moment ({@link #isSpatial}), whatever harm it does as it is cast. */
	static boolean castingSpatial(LivingEntity caster) {
		return casting != null && castBy == caster && isSpatial(casting);
	}

	/**
	 * How much farther what {@code caster} aims at reaches (HeavenSpells#beamEnd), while it casts ({@link #casting}): its element's Law's
	 * reach, and Space's for a spell of space. 1 for anything else.
	 */
	public static double reach(LivingEntity caster) {
		if (casting == null || castBy != caster || !(caster instanceof ServerPlayer p)) return 1;
		LawState s = CultivationManager.get(p).laws();
		double factor = 1;
		Law law = Law.of(casting.element());
		if (law != null) factor *= 1 + REACH_PER_LEVEL * s.level(law);
		if (isSpatial(casting)) factor *= 1 + REACH_PER_LEVEL * s.level(Law.SPACE);
		return factor;
	}

	/** {@code spell}'s cooldown for {@code caster} (Spells#cast): shortened by the Law of Time. A cultivator's as it is. */
	public static int cooldown(LivingEntity caster, Spell spell) {
		if (!(caster instanceof ServerPlayer p)) return spell.cooldownTicks();
		int time = CultivationManager.get(p).laws().level(Law.TIME);
		return (int) Math.round(spell.cooldownTicks() * (1 - COOLDOWN_PER_LEVEL * time));
	}

	/** What the Spatial Tear's wait (SpatialTrialHandler#tear) is for {@code c}: shortened by the Law of Space. */
	public static double tearWait(PlayerCultivation c) {
		return 1 - COOLDOWN_PER_LEVEL * c.laws().level(Law.SPACE);
	}

	/**
	 * What {@code amount} of healing comes to for {@code healed} (SoulLivingEntityMixin: every heal): none for a foe within a domain of Life
	 * and Death (LawDomains#withholdsHealing), any being; for a player none while the Five Decays wither it (FiveDecays#withered), else
	 * greater by its Law of Life and Death.
	 */
	public static float healed(LivingEntity healed, float amount) {
		if (amount > 0 && LawDomains.withholdsHealing(healed)) return 0;
		if (!(healed instanceof ServerPlayer p) || amount <= 0) return amount;
		if (FiveDecays.withered(p)) return 0;
		int life = CultivationManager.get(p).laws().level(Law.LIFE_AND_DEATH);
		return life <= 0 ? amount : amount * (1 + LIFE_PER_LEVEL * life);
	}

	/** {@code caster} casts {@code spell} now (Spells#perform), until {@link #castDone}: what it aims at reaches by its Laws. */
	static void casting(LivingEntity caster, Spell spell) {
		casting = spell;
		castBy = caster;
	}

	static void castDone() {
		casting = null;
		castBy = null;
	}

	private Laws() {}
}
