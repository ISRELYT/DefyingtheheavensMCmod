package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntToDoubleFunction;

/**
 * Lifespan (the user's ask, 2026-10-19). One ages a year for every game day one is online ({@link #YEAR_TICKS}), from {@link
 * #START_AGE}; a body has {@link #BASE_YEARS}, and every realm reached adds its years ({@link #realmYears}: Foundation Building
 * 200, Core Formation 400, Nascent Soul 600, Heavenly Being 1,200, Four Axis 10,000), counted by the higher of the two paths, and
 * the Axis of Longevity's years on top (PlayerCultivation#extraYears). It is one clock for the whole existence, alive and dead
 * alike (the user's ask, 2026-10-20: the dead had a soul's clock of their own, so a death near the end gave a whole span again):
 * dying gives no years back, and neither does coming back to life. It stops at the span's end, so years won after are won in
 * full (the user's ask, 2026-10-20); a body another soul holds (Possession) ages on, its owner only watching.
 * <p>
 * Run out, and the heavens come to collect the life: the lifespan tribulation (LifespanTribulation; the user's ask, 2026-10-20),
 * which grows until nothing can bear it, and ends only with years won again (a new realm's, the grateful's). Struck down by it, or
 * killed by anything while it runs, alive one is born again at one's own spawn point; dead, the soul scatters into the Soul River
 * and is born again. Either way the life is ended whole ({@link #wipe}): both paths, every method and spell, the stat points, the
 * physique (rolled anew), back to a mortal of {@link #START_AGE}. Only what the journal remembers, and the dead's passage (where
 * they died, their own respawn) carry over; and an Eternal Reincarnation Body keeps itself and all its cultivation, its years begun
 * anew ({@link #reincarnate}). Falling a realm takes its years away again, and a life past its new span faces the heavens at once. A
 * Rebirth Pill ends a life so without its death ({@link #rebirth}).
 * <p>
 * Body refining (Shattered Meridians, BodyRefining) counts for the realm each of its stages matches (the user's ask, 2026-10-20):
 * Foundation Building's years from Jade Bone, Core Formation's at the Indestructible Body.
 * <p>
 * What years are spent on besides (the user's asks, 2026-10-21, so a long life still has to be watched): meditation itself, by the
 * cultivation it wins, divided by talent and what saves years (boost, method, stone, dense qi), priced up at a faster pace
 * ({@link #seclude}: the talent race);
 * flying through space (AstralRealm#YEARS_PER_BLOCK, the same for everyone, less for a Starborn Body), Burn Lifespan (BurnLifespan),
 * and a herb or fruit given one's own years by Life Force Gain (WoodSpells); and years can be drawn out of another by Life Force
 * Drain ({@link #draw}). A mortal's body weakens in old age ({@link #oldAge}); a cultivator's never does.
 */
public final class Lifespan {
	/** A year a game day online. */
	public static final int YEAR_TICKS = 24000;
	/** The age a life begins at (and a save from before lifespans). */
	public static final double START_AGE = 16;
	/** A body's years before any realm adds to them: a mortal's, a Qi Refining cultivator's. */
	public static final double BASE_YEARS = 100;
	/** They are warned with this many years left, and again with one. */
	public static final double WARN_YEARS = 10;

	/**
	 * Meditation costs years (the user's vision, 2026-10-21: talent decides how far one gets, a race for most, not for the gifted).
	 * A realm's whole cultivation, on either path or by body refining, costs {@link #SECLUSION_SHARE} times the years the realm gives
	 * (Qi Refining's, of a body's own 84), shared out over its stages by how long each takes (Stage#timeWeight) and paid as it is
	 * won ({@link #seclude}); divided by one's talent (Physique.Grade#talent) and by what saves years ({@link #saving}): the
	 * meditation's boost (CultivationBoost: mats, pedestals, height, tranquillity, garments: the user's call, 2026-10-21), and, the
	 * user's call of 2026-10-22, a method (better than none: its element's share of the qi, by its stele the more), a spirit stone
	 * burning in hand, and dense qi where it is denser than the realm's own place (the Upper Realm's to a Heavenly Being, a rich
	 * planet's; the Netherworld's Nine Nether Peaks to the ghost path); multiplied by its pace's price ({@link #pacePrice}). Only what
	 * is won costs anything: a bottleneck waited at costs nothing, nor a reward of so many seconds, a kill, a tribulation's strike.
	 * <p>
	 * Set by the whole road to True Immortal, where a lifespan has no end ({@link #endless}: its own cultivation costs no years), by
	 * the user's years for the realms after Four Axis (Dao Integration 100,000, Void Shattering 10 million, Sacred Vessel 100 million,
	 * Entering Nirvana 10 billion), whose shares are 8, 12, 16 and 20.5: an Immortal grade becomes a True Immortal at ease (with a
	 * usual saving of x1.5), a Supreme one with good planning (about x5), a High one only with everything done right (about x10, the
	 * most there is), and nobody lower; a Low grade with nothing saving years doesn't get past Core Formation (the user's asks,
	 * 2026-10-21/22; scratchpad talent-ladder.ps1 of session 59448793).
	 */
	private static final double[] SECLUSION_SHARE = {0.6, 0.9, 1.6, 2.3, 3.3, 4.8, 8, 12, 16, 20.5, 0};

	/** The years a whole realm's cultivation costs one of no talent and no boost: its share of the years it gives (none for an endless one's). */
	public static double realmSeclusion(Realm realm) {
		double years = realm == Realm.QI_REFINING ? BASE_YEARS - START_AGE : realmYears(realm);
		if (Double.isInfinite(years)) return 0;
		return SECLUSION_SHARE[Math.min(realm.ordinal(), SECLUSION_SHARE.length - 1)] * years;
	}

	/** {@code stage}'s part of its realm's ({@link #realmSeclusion}), by how long it takes against the others (Stage#timeWeight). */
	public static double stageSeclusion(Realm realm, Stage stage) {
		double all = 0;
		for (Stage s : Stage.values()) all += s.timeWeight();
		return realmSeclusion(realm) * stage.timeWeight() / all;
	}

	/** {@code c}'s talent: its physique's (Physique#talent: its grade's, but an Eternal Reincarnation Body's a Low one's), a Low grade's with none yet. */
	public static double talent(PlayerCultivation c) {
		return c.getPhysique() == null ? Physique.Grade.LOW.talent() : c.getPhysique().talent();
	}

	/**
	 * The years winning {@code fraction} of a stage of {@code realm} by meditation costs {@code c} (its own record), at its pace, with
	 * {@code saving} what saves years where it sits ({@link #saving}).
	 */
	public static double seclusionYears(PlayerCultivation c, Realm realm, Stage stage, double fraction, double saving) {
		if (fraction <= 0) return 0;
		return fraction * stageSeclusion(realm, stage) * pacePrice(c.meditationSpeed()) / (talent(c) * Math.max(1.0, saving));
	}

	/**
	 * What saves a meditation's years besides talent ({@link #seclusionYears}): its boost (CultivationBoost#multiplier: mats,
	 * pedestals, height, tranquillity, garments), its method ({@code method}: what the method practised makes of the gain, by its
	 * element's share of the qi and its stele: HeavenMethods#elementFactor, GhostMethods#STELE_BONUS), dense qi ({@code place}: the
	 * qi there against the realm's own place, {@link #placeSaving}, {@link #ghostPlaceSaving}) and a spirit stone burning in hand
	 * ({@code stone}: SpiritStoneItem#meditationMultiplier). Each only ever helps: none of them costs years for being poor.
	 */
	public static double saving(double boost, double method, double place, double stone) {
		return Math.max(1.0, boost) * Math.max(1.0, method) * Math.max(1.0, place) * Math.max(1.0, stone);
	}

	/**
	 * The heaven path's dense qi against the realm's own place ({@link #saving}): the qi where {@code c} sits (PlayerCultivation#placeFactor),
	 * over the Upper Realm's for a realm cultivated nowhere else (Four Axis: its needs are made that much greater already,
	 * PlayerCultivation#isUpperRealmOnly), else over the mortal world's.
	 */
	public static double placeSaving(PlayerCultivation c) {
		return c.placeFactor() / (PlayerCultivation.isUpperRealmOnly(c.getRealm()) ? PlayerCultivation.UPPER_REALM_QI_MULTIPLIER : 1.0);
	}

	/** The ghost path's ghostly qi at {@code density} against the Netherworld's, its own place ({@link #saving}): more only on the Peaks. */
	public static double ghostPlaceSaving(double density) {
		return density / GhostQi.NETHERWORLD;
	}

	/**
	 * A meditation's gain (MeditationManager): {@code p} went from {@code fromRank} with {@code from} of its stage to {@code toRank}
	 * with {@code to} ({@code required}: each rank's stage's need; a body refining stage is the rank it matches), with {@code saving}
	 * ({@link #saving}); {@code share} of what it won (all but a reward's seconds) costs its years ({@link #seclusionYears}), stage by
	 * stage, to the span's end and no further. Creative and spectating players pay nothing.
	 */
	public static void seclude(ServerPlayer p, int fromRank, double from, int toRank, double to, IntToDoubleFunction required, double saving,
			double share) {
		if (fromRank < 0 || toRank < fromRank || share <= 0 || p.isCreative() || p.isSpectator()) return;
		PlayerCultivation own = CultivationManager.own(p);
		int stages = Stage.values().length;
		double years = 0;
		for (int rank = fromRank; rank <= toRank; rank++) {
			double need = required.applyAsDouble(rank);
			if (need <= 0) continue;
			double start = rank == fromRank ? from : 0, end = rank == toRank ? to : need;
			years += seclusionYears(own, Realm.byIndex(Math.min(rank / stages, Realm.values().length - 1)), Stage.byIndex(rank % stages),
					Math.max(0, end - start) / need, saving);
		}
		if (years > 0) age(p, years * share);
	}

	/**
	 * The paces a meditation may go at (the Cultivation tab; PlayerCultivation#meditationSpeed): every gain of it as many times as
	 * fast (MeditationManager), and every bit of what it wins costs {@link #pacePrice} times the years (the user's ask, priced
	 * steeply; set against the years meditation costs, 2026-10-21): x1 its own, x2 twice the years for each bit, x3 four times. Near
	 * the span's end a fast meditation slows to x1 by itself ({@link #check}).
	 */
	public static final int MEDITATION_SPEEDS = 3;
	private static final double[] PACE_PRICE = {1, 2, 4};

	/** What the years a meditation wins cost are multiplied by at {@code speed} (1 to {@link #MEDITATION_SPEEDS}). */
	public static double pacePrice(int speed) {
		return PACE_PRICE[Math.max(1, Math.min(MEDITATION_SPEEDS, speed)) - 1];
	}

	/** The Cultivation tab's pace button (C2S meditation_speed): the next pace, x1, x2, x3 and round again, its price told. */
	public static void cycleMeditationSpeed(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		c.setMeditationSpeed(c.meditationSpeed() % MEDITATION_SPEEDS + 1);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_PACE[c.meditationSpeed() - 1]), true);
	}

	/** Of the years Life Force Drain draws out of another ({@link #draw}), the share the drawer keeps (the rest is lost on the way). */
	public static final double DRAWN_KEPT = 0.5;

	/**
	 * A mortal's body begins to fail at this share of its span, and by the span's end has lost {@link #OLD_HEALTH} of its max health,
	 * {@link #OLD_SPEED} of its speed and {@link #OLD_STRENGTH} of the strength of its blows, a little more every year (the user's ask,
	 * 2026-10-21: old age weakens only mortals; a cultivator keeps its vigour to the end, when the heavens come for it instead).
	 */
	public static final double OLD_AGE_FROM = 0.6;
	public static final double OLD_HEALTH = 0.4, OLD_SPEED = 0.2, OLD_STRENGTH = 0.3;
	private static final UUID OLD_HEALTH_ID = UUID.fromString("5b0c6d2e-7a51-4f0e-9d3c-1e2f3a4b5c61");
	private static final UUID OLD_SPEED_ID = UUID.fromString("5b0c6d2e-7a51-4f0e-9d3c-1e2f3a4b5c62");
	private static final UUID OLD_STRENGTH_ID = UUID.fromString("5b0c6d2e-7a51-4f0e-9d3c-1e2f3a4b5c63");
	/** Those told their body has begun to fail (once a login, and again whenever it starts anew). */
	private static final Set<UUID> OLD_TOLD = new HashSet<>();

	/**
	 * The years a realm adds to the span, once reached (and all those before it, theirs): the user's, Dao Integration's to Entering
	 * Nirvana's too (2026-10-21); a True Immortal's are without end ({@link #endless}).
	 */
	public static double realmYears(Realm realm) {
		return switch (realm) {
			case FOUNDATION_BUILDING -> 200;
			case CORE_FORMATION -> 400;
			case NASCENT_SOUL -> 600;
			case HEAVENLY_BEING -> 1200;
			case FOUR_AXIS -> 10000;
			case DAO_INTEGRATION -> 100_000;
			case VOID_SHATTERING -> 10_000_000;
			case SACRED_VESSEL -> 100_000_000;
			case ENTERING_NIRVANA -> 10_000_000_000.0;
			case TRUE_IMMORTAL -> Double.POSITIVE_INFINITY;
			default -> 0;
		};
	}

	/**
	 * Whether {@code c}'s years have no end: a True Immortal's (its higher path's realm; the user's, 2026-10-21). It doesn't age, no
	 * lifespan tribulation comes for it, and nothing it does costs it years: meditation, flight through space, Life Force; nor can its
	 * years be drawn out of it. Burn Lifespan burns on for nothing (the user's call, 2026-10-22: left as it is for now).
	 */
	public static boolean endless(PlayerCultivation c) {
		return Double.isInfinite(span(c));
	}

	/**
	 * {@code c}'s span in years: the body's, every realm its higher path has reached, and the years won besides; all of it x1.2 for
	 * an obsession with Longevity (Obsession#lifespanFactor), x1.5 for a demon (DemonPath#LIFESPAN: the beasts are long-lived). Infinite
	 * for a True Immortal ({@link #endless}).
	 */
	public static double span(PlayerCultivation c) {
		double years = BASE_YEARS + c.extraYears();
		int rank = spanRank(c);
		if (rank >= 0) {
			Realm top = Realm.byIndex(rank / Stage.values().length);
			for (Realm realm : Realm.values()) if (realm.ordinal() <= top.ordinal()) years += realmYears(realm);
		}
		return years * Obsession.lifespanFactor(c) * (DemonPath.isDemon(c) ? DemonPath.LIFESPAN : 1.0);
	}

	/** The rank whose realms' years {@code c}'s span counts: the higher path's, or a body refined past a mortal's Peak's; -1 a mortal's. */
	static int spanRank(PlayerCultivation c) {
		int rank = TwoPaths.higherRank(c, false);
		// A body refined past a mortal's Peak lives as long as the cultivator it is as strong as.
		if (rank < 0 && c.hasShatteredMeridians() && c.getBodyRefining() > 0) rank = BodyRefining.rank(c.getBodyRefining());
		return rank;
	}

	/** The years {@code c} has used of it: its age, alive or dead. */
	public static double used(PlayerCultivation c) {
		return c.age();
	}

	public static double left(PlayerCultivation c) {
		return span(c) - used(c);
	}

	/** What each player was last warned of: 0 nothing, 1 the last ten years, 2 the last year. */
	private static final Map<UUID, Integer> WARNED = new HashMap<>();

	/**
	 * Once a second: everyone online (but creative and spectating players; one whose body another soul holds watches, and ages on),
	 * alive or dead, ages a second's worth, no further than the span's end (what meditation wins costs its own years besides:
	 * {@link #seclude}); and a spent life faces the heavens. A mortal's old age shows on its body ({@link #weaken}).
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		double step = 20.0 / YEAR_TICKS;
		for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
			if (!p.isAlive()) continue;
			if (!p.isSpectator()) weaken(p);
			if (p.isCreative() || p.isSpectator() && !Possession.isHeld(p.getUUID())) continue;
			age(p, step);
		}
	}

	/** How far into a mortal's old age {@code c} is: 0 none (or no mortal: a realm on either path, a refined body, the dead) to 1 at the span's end. */
	public static double oldAge(PlayerCultivation c) {
		if (c.netherworld().ghost() || spanRank(c) >= 0) return 0;
		double span = span(c);
		return Mth.clamp((c.age() - span * OLD_AGE_FROM) / (span * (1 - OLD_AGE_FROM)), 0, 1);
	}

	/** Once a second: a mortal's old age on its body, or none, in steps of 2% (not made anew every second); told as it begins. */
	static void weaken(ServerPlayer p) {
		double old = Math.ceil(oldAge(CultivationManager.own(p)) * 50) / 50.0;
		modifier(p, Attributes.MAX_HEALTH, OLD_HEALTH_ID, -OLD_HEALTH * old);
		modifier(p, Attributes.MOVEMENT_SPEED, OLD_SPEED_ID, -OLD_SPEED * old);
		modifier(p, Attributes.ATTACK_DAMAGE, OLD_STRENGTH_ID, -OLD_STRENGTH * old);
		if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
		if (old <= 0) {
			OLD_TOLD.remove(p.getUUID());
		} else if (OLD_TOLD.add(p.getUUID())) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_OLD_AGE).withStyle(ChatFormatting.GRAY));
		}
	}

	/** {@code attribute} of {@code p} times 1 + {@code amount} under {@code id} (none at 0), remade only when that changes. */
	private static void modifier(ServerPlayer p, Attribute attribute, UUID id, double amount) {
		AttributeInstance instance = p.getAttribute(attribute);
		if (instance == null) return;
		AttributeModifier now = instance.getModifier(id);
		if (now != null && Math.abs(now.getAmount() - amount) < 1e-9) return;
		if (now != null) instance.removeModifier(id);
		if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, "Old age", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	/**
	 * Life Force Drain on another (WoodSpells; the user's ask, 2026-10-21): up to {@code years} of {@code victim}'s years drawn out of
	 * them (no more than are left them: they age), and {@link #DRAWN_KEPT} of those won by {@code taker}, its span the longer (which
	 * ends a lifespan tribulation of its, as years won do). Creative and spectating players have none to give. @return the years drawn
	 */
	public static double draw(ServerPlayer taker, ServerPlayer victim, double years) {
		if (victim.isCreative() || victim.isSpectator() || endless(CultivationManager.own(victim))) return 0;
		double drawn = Math.min(years, Math.max(0, left(CultivationManager.own(victim))));
		if (drawn <= 0) return 0;
		age(victim, drawn);
		PlayerCultivation mine = CultivationManager.own(taker);
		mine.setExtraYears(mine.extraYears() + drawn * DRAWN_KEPT);
		CultivationManager.markDirty(taker.server);
		check(taker);
		return drawn;
	}

	/**
	 * {@code p} ages {@code years}, no further than the span's end (what is won after is won in full); then {@link #check}. A True
	 * Immortal ({@link #endless}) never does: only {@link #check}, which lets a lifespan tribulation go.
	 */
	static void age(ServerPlayer p, double years) {
		PlayerCultivation c = CultivationManager.own(p);
		double span = span(c);
		if (Double.isInfinite(span)) {
			check(p);
			return;
		}
		if (c.age() < span) c.setAge(Math.min(span, c.age() + years));
		CultivationManager.markDirty(p.server);
		check(p);
	}

	/**
	 * A spent life faces the lifespan tribulation (LifespanTribulation), and one with years won again is let go by it; nearing its
	 * end, they are warned. @return whether the lifespan tribulation runs for them
	 */
	public static boolean check(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		boolean dead = c.netherworld().ghost();
		double left = left(c);
		if (left <= 0) {
			WARNED.remove(p.getUUID());
			LifespanTribulation.begin(p);
			return true;
		}
		if (LifespanTribulation.isRunning(p.getUUID())) LifespanTribulation.end(p);
		int level = left <= 1 ? 2 : left <= WARN_YEARS ? 1 : 0;
		int before = WARNED.getOrDefault(p.getUUID(), 0);
		if (level > before) {
			p.sendSystemMessage(Component.translatable(dead ? ModLang.MSG_SOUL_TIME_WARN : ModLang.MSG_LIFESPAN_WARN, (long) Math.ceil(left))
					.withStyle(ChatFormatting.RED));
			p.playNotifySound(SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);
			// Near the end a fast meditation slows to its own pace by itself (it may be set fast again): none spends the last years unawares.
			if (c.meditationSpeed() > 1) {
				c.setMeditationSpeed(1);
				p.sendSystemMessage(Component.translatable(ModLang.MSG_MEDITATION_SLOWED).withStyle(ChatFormatting.GOLD));
				CultivationManager.sync(p);
			}
		}
		if (level != before) WARNED.put(p.getUUID(), level);
		return false;
	}

	/**
	 * The lifespan tribulation has struck one of the dead down (LifespanTribulation): the soul scatters into the Soul River and is
	 * born again, a mortal knowing nothing.
	 */
	static void soulScatters(ServerPlayer p) {
		p.sendSystemMessage(Component.translatable(ModLang.MSG_SOUL_TIME_END).withStyle(ChatFormatting.DARK_AQUA));
		p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2f, 0.5f);
		wipe(p, true);
		GhostSystem.revive(p);
	}

	/**
	 * A Rebirth Pill: the life's cultivation ends whole as a spent life's does ({@link #wipe}: a new physique with it), but the living
	 * don't die for it, and the dead are born again, as when their years run out among the dead. (An Eternal Reincarnation Body can't
	 * take one: RebirthPillItem#refuses.)
	 */
	public static void rebirth(ServerPlayer p) {
		boolean dead = CultivationManager.own(p).netherworld().ghost();
		wipe(p, dead);
		if (dead) GhostSystem.revive(p);
		p.setHealth(p.getMaxHealth());
		p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 0.7f);
	}

	/**
	 * The life ends whole: a fresh record in its place (a mortal of {@link #START_AGE}, no path, no method, no point, the physique
	 * rolled anew), keeping only the journal and the dead's passage (where they died and their own respawn, and, for {@code
	 * stillDead}, that they are dead: the Wheel takes them back). A soul out of its body, or in another's, comes home first. An
	 * Eternal Reincarnation Body (Physique) keeps itself and all its cultivation ({@link #reincarnate}).
	 */
	public static void wipe(ServerPlayer p, boolean stillDead) {
		SoulProjection.bringHome(p);
		Clones.lifeEnds(p); // out of a clone, and it dissolves (its things falling where it stood): nothing of the old life goes on
		// The heavens' storm over a spent life goes with it (the new one has its years).
		if (LifespanTribulation.isRunning(p.getUUID())) ModPackets.sendLifespanTribulation(p, false, 0, false, false);
		LifespanTribulation.forget(p.getUUID());
		// A life that ends as it sits, or under a tribulation, ends them too: nothing of the old life goes on working on the new one
		// (a tribulation "failed" by this death would set the new mortal back a stage).
		MeditationManager.stop(p, false);
		if (TribulationManager.isActive(p.getUUID())) {
			TribulationManager.forget(p.getUUID());
			ModPackets.sendTribulation(p, false, 0, Realm.QI_REFINING, Stage.EARLY, false); // its bar off their screen
		}
		if (InnerRealm.isInside(p)) InnerRealm.leave(p);
		PlayerCultivation old = CultivationManager.own(p);
		PlayerCultivation fresh = new PlayerCultivation();
		fresh.journal().addAll(old.journal());
		NetherworldState from = old.netherworld(), to = fresh.netherworld();
		to.setGhost(stillDead && from.ghost());
		to.setBoundBelow(from.boundBelow());
		if (from.hasDeathPlace()) to.setDeathPlace(from.deathDimension(), from.deathPos());
		to.setSavedRespawn(from.savedRespawn());
		to.setSawHome(from.sawHome());
		boolean eternal = old.hasPhysique(Physique.ETERNAL_REINCARNATION_BODY);
		if (eternal) reincarnate(old, fresh);
		CultivationSavedData.forServer(p.server).replace(p.getUUID(), fresh);
		// What the systems noted of the old life, forgotten: the new one is taken as it is found, with no word of anything "lost".
		UUID id = p.getUUID();
		TwoPaths.forget(id);
		QiFlight.forget(id);
		QiSense.forget(id);
		HeavenMethods.forget(id);
		NetherworldManager.forget(id);
		ConsciousnessDomainHandler.forget(id);
		Tempering.forget(id);
		// The body of the next life (refreshes the body, tells them): an Eternal Reincarnation Body's own again.
		Physiques.give(p, eternal ? Physique.ETERNAL_REINCARNATION_BODY : Physique.roll(p.getRandom()));
		if (eternal) {
			int rank = TwoPaths.higherRank(fresh, false), stages = Stage.values().length;
			p.sendSystemMessage(Component.translatable(ModLang.MSG_REINCARNATED, rank < 0 ? Component.translatable(ModLang.MORTAL)
					: PlayerCultivation.rankName(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages))).withStyle(ChatFormatting.GOLD));
		}
		Spells.sync(p);
		GhostSystem.broadcast(p);
		CultivationManager.markDirty(p.server);
	}

	/**
	 * An Eternal Reincarnation Body born again (Physique; the user's ask, 2026-10-22: "you can reach it if you work hard and you dont lose
	 * stuff"): into {@code fresh}, a life begun anew at {@link #START_AGE} with its realm's years ahead of it, goes all {@code old}'s
	 * cultivation (PlayerCultivation#carryOver). Until then it was set back to half its realm on each path, which a Low talent never
	 * outgrew.
	 */
	static void reincarnate(PlayerCultivation old, PlayerCultivation fresh) {
		fresh.carryOver(old);
	}

	public static void forget(UUID id) {
		WARNED.remove(id);
		OLD_TOLD.remove(id);
	}

	private Lifespan() {}
}
