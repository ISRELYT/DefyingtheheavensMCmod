package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Which method each path cultivates by, and what it holds of it (CultivationMethod; the methods overhaul's phase 2, 2026-10-31): the method
 * practised ({@link #practised}), its volumes ({@link #hasVolume}), why meditation gathers nothing ({@link #whyNoGain}), switching
 * ({@link #practise}: the path's stages in its realm lost, never the realm), learning a manual ({@link #learn}), and a gain won in a
 * method's own way ({@link #grant}: a rite, a strike, a corpse's yin, a phial, a core).
 */
public final class CultivationMethods {
	/** Turtle Breath gathers at this share of what Qi Circulation would (its years a third: CultivationMethod#yearsFactor). */
	public static final double TURTLE_PACE = 0.5;
	/** Territorial Dominion's territory gathers this many times as fast as Bloodline Purification would (its years a third). */
	public static final double TERRITORY_PACE = 1.5;

	// --- The method practised ---

	/** The method {@code path} of {@code c} cultivates by now: at Four Axis the axes; else the one chosen ({@link #chosen}). */
	public static CultivationMethod practised(PlayerCultivation c, TwoPaths.Path path) {
		if (realmOf(c, path) == Realm.FOUR_AXIS) return CultivationMethod.FOUR_AXES;
		return chosen(c, path);
	}

	/** The method {@code path} of {@code c} is set to cultivate by (out of Four Axis): the one chosen, or its orthodox one. */
	public static CultivationMethod chosen(PlayerCultivation c, TwoPaths.Path path) {
		CultivationMethod chosen = c.practising(path);
		return chosen != null && knows(c, chosen) ? chosen : CultivationMethod.orthodox(path);
	}

	/** The realm {@code path} of {@code c} stands in, or null if it isn't begun (a mortal's heaven path; no ghost path; no demon core). */
	public static Realm realmOf(PlayerCultivation c, TwoPaths.Path path) {
		int rank = rankOf(c, path);
		return rank < 0 ? null : Realm.byIndex(Math.min(Realm.values().length - 1, rank / Stage.values().length));
	}

	/** Where {@code path} of {@code c} stands (PlayerCultivation#rank), or -1 if it isn't begun. */
	public static int rankOf(PlayerCultivation c, TwoPaths.Path path) {
		return switch (path) {
			case HEAVEN -> c.isMortal() ? -1 : PlayerCultivation.rank(c.getRealm(), c.getStage());
			case GHOST -> GhostMethods.ghostRank(c.netherworld());
			case DEMON -> DemonPath.rank(c);
			case BODY -> BodyPath.rank(c);
		};
	}

	// --- What is held of it ---

	/**
	 * Whether {@code c} knows {@code method} at all: the orthodox ways by what their paths teach (a heaven scripture, a ghost foundation,
	 * a demon core), the Heavenly Thunder Body's by its body, the rest by a Method Manual read (PlayerCultivation#methodReach).
	 */
	public static boolean knows(PlayerCultivation c, CultivationMethod method) {
		return switch (method) {
			case QI_CIRCULATION -> c.heaven().knowsAny();
			case NETHERWORLD_BREATHING -> GhostMethods.foundationReach(c.netherworld()) != null;
			case BLOODLINE_PURIFICATION -> DemonPath.rank(c) >= 0;
			case BODY_TEMPERING -> BodyPath.rank(c) >= 0;
			case FOUR_AXES -> true;
			case THUNDER_TEMPERING -> c.hasPhysique(Physique.HEAVENLY_THUNDER_BODY);
			default -> c.methodReach(method) >= 0;
		};
	}

	/**
	 * Whether {@code c} holds {@code method}'s volume for {@code realm}: the orthodox ways' by what their paths teach (HeavenMethods#hasVolume,
	 * GhostMethods#hasVolume, DemonPath#hasVolume); the demon methods' by its blood, as Purification's (the user's rule: a demon's blood is
	 * its volume, every demon method it knows working in every realm it has reached); the Heavenly Thunder Body's whole; the rest as far
	 * as their manuals reach. None outside the method's own realms, nor of Four Axis but the axes.
	 */
	public static boolean hasVolume(PlayerCultivation c, CultivationMethod method, Realm realm) {
		if (realm == null || !method.spans(realm)) return false;
		return switch (method) {
			case QI_CIRCULATION -> HeavenMethods.hasVolume(c, realm);
			case NETHERWORLD_BREATHING -> GhostMethods.hasVolume(c, realm);
			case BLOODLINE_PURIFICATION -> DemonPath.hasVolume(c, realm);
			case BODY_TEMPERING -> BodyPath.rank(c) >= 0; // the body is its own volume, every realm of it
			case FOUR_AXES -> false; // the axes are no volume: they fill Four Axis by themselves (FourAxis#fillBars)
			case THUNDER_TEMPERING -> knows(c, method);
			case KIN_BLOOD, CORE_DEVOURING, TERRITORIAL_DOMINION, BEAST_DEITY, ANCESTRAL_COMMUNION, BLOODLINE_NIRVANA, PRIMORDIAL_RETURN ->
					knows(c, method) && DemonPath.hasVolume(c, realm);
			default -> c.methodReach(method) >= realm.ordinal();
		};
	}

	/** Whether {@code path}'s practised method holds its volume for the path's realm: whether it can gather at all, its own way. */
	public static boolean holdsVolume(PlayerCultivation c, TwoPaths.Path path) {
		return hasVolume(c, practised(c, path), realmOf(c, path));
	}

	/**
	 * The deepest realm {@code method}'s volumes known reach for {@code c} (FourAxis#volumeReach: Four Axis has none), or null with none:
	 * the Methods tab's line.
	 */
	public static Realm reach(PlayerCultivation c, CultivationMethod method) {
		if (!knows(c, method)) return null;
		Realm deepest = switch (method) {
			case QI_CIRCULATION -> HeavenMethods.deepestRealm(c.heaven());
			case NETHERWORLD_BREATHING -> GhostMethods.foundationReach(c.netherworld());
			case BLOODLINE_PURIFICATION, KIN_BLOOD, CORE_DEVOURING, TERRITORIAL_DOMINION, BEAST_DEITY, ANCESTRAL_COMMUNION, BLOODLINE_NIRVANA, PRIMORDIAL_RETURN ->
					Realm.byIndex(Math.min(method.last().ordinal(), Math.max(DemonPath.realm(DemonPath.rank(c)).ordinal(), c.demon().bloodRealm())));
			case THUNDER_TEMPERING, FOUR_AXES, BODY_TEMPERING -> method.last();
			default -> Realm.byIndex(Math.min(method.last().ordinal(), c.methodReach(method)));
		};
		return deepest == null ? null : FourAxis.volumeReach(deepest);
	}

	/**
	 * Why meditation gathers nothing on {@code path} of {@code p} now, or null if it gathers: Four Axis (the axes are the method there);
	 * no volume of the realm in the method practised (no scripture at all, on the heaven path); a method that doesn't gather by meditation
	 * (its own way told); Turtle Breath out of deep seclusion; Territorial Dominion out of the territory; a place's way away from its place
	 * (MethodGrounds: the Dao field of the stage, the primeval dragon's bones, a primordial beast; the Immortal Realm, with essence to burn).
	 */
	public static Component whyNoGain(ServerPlayer p, PlayerCultivation c, TwoPaths.Path path) {
		Realm realm = realmOf(c, path);
		if (realm == null) return null;
		CultivationMethod method = practised(c, path);
		if (realm == Realm.FOUR_AXIS) return Component.translatable(ModLang.MSG_AXES_ARE_THE_METHOD);
		if (!hasVolume(c, method, realm)) {
			if (method == CultivationMethod.QI_CIRCULATION) return c.heaven().knowsAny()
					? Component.translatable(ModLang.MSG_HEAVEN_NO_VOLUME, realm.getDisplayName()) : Component.translatable(ModLang.MSG_HEAVEN_NONE_KNOWN);
			if (method == CultivationMethod.NETHERWORLD_BREATHING) return Component.translatable(ModLang.MSG_GHOST_NO_VOLUME, realm.getDisplayName());
			return Component.translatable(ModLang.MSG_METHOD_NO_VOLUME, method.getDisplayName(), realm.getDisplayName());
		}
		if (!method.meditates()) {
			return Component.translatable(ModLang.MSG_METHOD_NOT_BY_MEDITATION, method.getDisplayName(),
					Component.translatable(ModLang.cultivationMethodKey(method.getId()) + ".how"));
		}
		if (method == CultivationMethod.TURTLE_BREATH && !TurtleBreath.secluded(p)) return Component.translatable(ModLang.MSG_TURTLE_NOT_SECLUDED);
		if (method == CultivationMethod.TERRITORIAL_DOMINION && !Territories.inside(p)) return Component.translatable(ModLang.MSG_TERRITORY_OUTSIDE);
		return MethodGrounds.whyNot(p, c, method);
	}

	// --- Switching ---

	/**
	 * {@code player}'s path cultivates by {@code method} from now on (the user's rule, 2026-10-31: "switching methods should ONLY make you
	 * lose your cultivation stages not realms, as volumes should last for the realm"): another than the one chosen re-lays the path's
	 * foundation, the path falling to its realm's Early, its cultivation there lost, never a realm ({@link #restart}); at Four Axis it costs
	 * nothing (the axes are the method there, whatever is chosen for after), nor does a path not begun. Told. @return whether it changed
	 */
	public static boolean practise(ServerPlayer player, CultivationMethod method) {
		if (method == null || method.path() == null) return false;
		// Not a body's way switched by the soul in it (Possession): the switch costs its owner their stage, theirs to choose, as its stat points
		// are theirs to place (StatPoints#spend; a soul switched a held player's for them before the sweep of 2026-11-01).
		if (Possession.inhabits(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return false;
		}
		PlayerCultivation c = CultivationManager.get(player);
		TwoPaths.Path path = method.path();
		if (!knows(c, method)) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_UNKNOWN, method.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		if (chosen(c, path) == method) return false;
		Realm realm = realmOf(c, path);
		c.setPractising(path, method == CultivationMethod.orthodox(path) ? null : method);
		boolean free = realm == null || realm == Realm.FOUR_AXIS;
		if (!free && restart(c, path)) {
			// The Body Path's own realm names (BodyPath#rankName): "Iron Flesh - Early", not the qi realm beside it.
			Component early = path == TwoPaths.Path.BODY ? BodyPath.rankName(PlayerCultivation.rank(realm, Stage.EARLY)) : PlayerCultivation.rankName(realm, Stage.EARLY);
			player.sendSystemMessage(Component.translatable(ModLang.MSG_METHOD_SWITCH_COST, method.getDisplayName(), early).withStyle(ChatFormatting.GOLD));
		}
		player.sendSystemMessage(Component.translatable(ModLang.MSG_CULTIVATION_METHOD_PRACTISED, method.getDisplayName()).withStyle(ChatFormatting.GOLD));
		player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 0.9f);
		CultivationManager.markDirty(player.server);
		CultivationManager.refresh(player);
		Spells.sync(player);
		return true;
	}

	/**
	 * The path's foundation re-laid: {@code path} falls to its realm's Early, its cultivation there lost (never a realm), the scriptures' and
	 * foundations' layers past it with it, to settle in again (a switch; a bound beast's death: SpiritBonds#died, its owner online or not).
	 * @return whether anything was lost
	 */
	static boolean restart(PlayerCultivation c, TwoPaths.Path path) {
		int rank = rankOf(c, path);
		if (rank < 0) return false;
		int early = rank - rank % Stage.values().length;
		switch (path) {
			case HEAVEN -> {
				if (rank == early && c.getCultivation() <= 0) return false;
				c.setBackTo(c.getRealm(), Stage.EARLY);
			}
			case GHOST -> {
				NetherworldState s = c.netherworld();
				if (rank == early && s.pathCultivation() <= 0) return false;
				s.setPath(early, 0);
				GhostMethods.clampTo(s, early);
			}
			case DEMON -> {
				if (rank == early && c.demon().cultivation() <= 0) return false;
				c.demon().set(early, 0);
			}
			case BODY -> {
				if (rank == early && c.body().cultivation() <= 0) return false;
				c.body().set(early, 0);
			}
		}
		return true;
	}

	// --- Learning ---

	/**
	 * {@code player} reads a manual of {@code method} reaching {@code realm} (MethodManualItem): its volumes from its first realm up to it
	 * learned (a demon method's whole: its blood is its volume). Refused, told why: a mortal's sealed meridians, a heaven method to the dead,
	 * a demon method to one with no demon core, the Heavenly Thunder Body's to another body, or nothing new in it. @return whether learned
	 */
	public static boolean learn(ServerPlayer player, CultivationMethod method, Realm realm) {
		PlayerCultivation c = CultivationManager.own(player);
		Component refusal = refusal(c, method);
		if (refusal != null) {
			player.displayClientMessage(refusal.copy().withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		int to = method.path() == TwoPaths.Path.DEMON ? method.last().ordinal() : Math.min(method.last().ordinal(), realm.ordinal());
		if (c.methodReach(method) >= to) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_KNOWN, method.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		c.setMethodReach(method, to);
		player.sendSystemMessage((method.path() == TwoPaths.Path.DEMON ? Component.translatable(ModLang.MSG_METHOD_LEARNED_BLOOD, method.getDisplayName())
				: Component.translatable(ModLang.MSG_CULTIVATION_METHOD_LEARNED, method.getDisplayName(), FourAxis.volumeReach(Realm.byIndex(to)).getDisplayName()))
				.withStyle(ChatFormatting.GOLD));
		player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
		Journal.note(player, "cultivation_method:" + method.getId(), ModLang.JOURNAL_CULTIVATION_METHOD,
				Component.translatable(ModLang.JOURNAL_CULTIVATION_METHOD_TEXT, method.getDisplayName(), method.getDescription()));
		CultivationManager.markDirty(player.server);
		Spells.sync(player);
		return true;
	}

	/**
	 * Why {@code c} can't take up {@code method} from a manual, or null if it can: a mortal's meridians are sealed to every path's but the
	 * body's (its ways are the flesh's: a mortal on the Body Path reads them), which wants the Body Path begun.
	 */
	public static Component refusal(PlayerCultivation c, CultivationMethod method) {
		if (c.isMortal() && method.path() != TwoPaths.Path.BODY) return Component.translatable(ModLang.MSG_HEAVEN_METHOD_MERIDIANS);
		if (!method.fromManual()) return Component.translatable(ModLang.MSG_METHOD_NOT_TAUGHT, method.getDisplayName());
		return switch (method.path()) {
			case HEAVEN -> HeavenMethods.refusal(c);
			case GHOST -> null;
			case DEMON -> DemonPath.rank(c) < 0 ? Component.translatable(ModLang.MSG_METHOD_NEEDS_CORE, method.getDisplayName()) : null;
			case BODY -> BodyPath.rank(c) < 0 ? Component.translatable(ModLang.MSG_METHOD_NEEDS_BODY_PATH, method.getDisplayName()) : null;
		};
	}

	// --- A gain won its own way ---

	/**
	 * {@code player} wins {@code fraction} of the stage its {@code path} stands at by {@code method} in its own way (a rite, a strike, a
	 * corpse's yin, a phial, a core): onto the path's bar, no further than its bottleneck (on the Demon Path, its taint takes its share:
	 * DemonState#taint), its years paid as the method's weight and share have them (Lifespan#methodYears). Told how much, and of any stage
	 * won. Only with the method's volume of the realm: none, nothing (told). @return the cultivation won
	 */
	public static double grant(ServerPlayer player, TwoPaths.Path path, CultivationMethod method, double fraction) {
		return grant(player, path, method, fraction, true);
	}

	/** {@link #grant}, the gain told ({@code tell}) or not: a way that gains a little every second (a soak, a furnace) shows it otherwise. */
	public static double grant(ServerPlayer player, TwoPaths.Path path, CultivationMethod method, double fraction, boolean tell) {
		PlayerCultivation c = CultivationManager.get(player);
		Realm realm = realmOf(c, path);
		if (realm == null || fraction <= 0) return 0;
		if (!hasVolume(c, method, realm)) {
			if (tell) player.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_NO_VOLUME, method.getDisplayName(), realm.getDisplayName())
					.withStyle(ChatFormatting.GOLD), true);
			return 0;
		}
		int before = rankOf(c, path);
		double won;
		switch (path) {
			case HEAVEN -> {
				double was = c.getCultivation();
				boolean advanced = c.addCultivation(fraction * c.cultivationRequired());
				int after = PlayerCultivation.rank(c.getRealm(), c.getStage());
				won = after > before ? fraction * PlayerCultivation.required(realm, Stage.byIndex(before % Stage.values().length)) : c.getCultivation() - was;
				Lifespan.secludeBy(player, before, was, after, c.getCultivation(),
						rank -> PlayerCultivation.required(Realm.byIndex(rank / Stage.values().length), Stage.byIndex(rank % Stage.values().length)), method);
				CultivationManager.refresh(player);
				if (advanced) {
					player.displayClientMessage(Component.translatable(ModLang.MSG_STAGE_UP, c.getRealm().getDisplayName(), c.getStage().getDisplayName()), true);
					player.setHealth(player.getMaxHealth());
				}
			}
			case GHOST -> {
				NetherworldState s = c.netherworld();
				double was = s.pathCultivation();
				int stages = GhostMethods.addCultivation(s, fraction * GhostMethods.required(before));
				won = stages > 0 ? fraction * GhostMethods.required(before) : s.pathCultivation() - was;
				Lifespan.secludeBy(player, before, was, GhostMethods.ghostRank(s), s.pathCultivation(), GhostMethods::required, method);
				if (stages > 0) GhostMethods.afterRank(player, before);
				NetherworldManager.refresh(player);
				Spells.sync(player);
			}
			case BODY -> {
				double was = c.body().cultivation();
				double amount = fraction * BodyPath.required(before);
				int stages = BodyPath.addCultivation(c, amount);
				won = stages > 0 ? amount : c.body().cultivation() - was;
				Lifespan.secludeBy(player, before, was, BodyPath.rank(c), c.body().cultivation(), BodyPath::required, method);
				if (stages > 0) BodyPath.afterRank(player, before);
				else CultivationManager.sync(player);
			}
			default -> {
				double was = c.demon().cultivation();
				double amount = fraction * DemonPath.required(before) * (1 - c.demon().taint());
				int stages = DemonPath.addCultivation(c, amount);
				won = stages > 0 ? amount : c.demon().cultivation() - was;
				Lifespan.secludeBy(player, before, was, DemonPath.rank(c), c.demon().cultivation(), DemonPath::required, method);
				if (stages > 0) DemonPath.afterRank(player, before);
				else CultivationManager.sync(player);
			}
		}
		CultivationManager.markDirty(player.server);
		if (won > 0 && tell) {
			// A share under a hundredth told to a tenth of it (a Hungry Ghost's 0.4%, not "+0%").
			double percent = fraction * 100;
			Object shown = percent < 0.95 ? String.format(java.util.Locale.ROOT, "%.1f", percent) : (Object) Math.round(percent);
			player.displayClientMessage(Component.translatable(ModLang.MSG_METHOD_GAIN, method.getDisplayName(), shown).withStyle(ChatFormatting.GREEN), true);
		}
		return won;
	}

	/**
	 * {@code player}'s {@code path} loses {@code fraction} of its stage's cultivation (a way's price: a heart demon's victory, a vessel's
	 * failure, the furnace breaking), never below the stage's start: never a stage, nor a realm. @return the cultivation lost
	 */
	public static double lose(ServerPlayer player, TwoPaths.Path path, double fraction) {
		PlayerCultivation c = CultivationManager.get(player);
		int rank = rankOf(c, path);
		if (rank < 0 || fraction <= 0) return 0;
		double lost;
		switch (path) {
			case HEAVEN -> {
				lost = Math.min(c.getCultivation(), fraction * c.cultivationRequired());
				c.setState(c.getRealm(), c.getStage(), c.getCultivation() - lost);
				CultivationManager.refresh(player);
			}
			case GHOST -> {
				NetherworldState s = c.netherworld();
				lost = Math.min(s.pathCultivation(), fraction * GhostMethods.required(rank));
				s.setPath(rank, s.pathCultivation() - lost);
				Spells.sync(player);
			}
			case BODY -> {
				lost = Math.min(c.body().cultivation(), fraction * BodyPath.required(rank));
				c.body().set(rank, c.body().cultivation() - lost);
				CultivationManager.sync(player);
			}
			default -> {
				lost = Math.min(c.demon().cultivation(), fraction * DemonPath.required(rank));
				c.demon().set(rank, c.demon().cultivation() - lost);
				CultivationManager.sync(player);
			}
		}
		CultivationManager.markDirty(player.server);
		return lost;
	}

	private CultivationMethods() {}
}
