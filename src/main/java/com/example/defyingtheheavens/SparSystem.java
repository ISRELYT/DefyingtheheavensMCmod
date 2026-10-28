package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sparring with a cultivator (chosen from the talk wheel, see TalkSystem): a bout of several exchanges. It ends when either
 * side has lost {@link #LINE_SHARE} of their maximum health since it began, when the player walks {@link #WALK_AWAY} blocks
 * off (a forfeit), or after {@link #TIME_LIMIT_TICKS} (a draw). Blows between the two land at {@link #BLOW_SHARE} of their
 * strength (pulled: at a quarter's line, one blow from an equal ended it, up to Nascent Soul). Nobody dies: a blow from the
 * other duellist that would go past the line stops at it. A stronger cultivator holds back to the player's realm, unless
 * asked not to. The entrance trial (Terms#TRIAL) is fought the same way.
 * <ul>
 *   <li><b>Wager</b>: the player stakes one of what they hold (a herb, a pill, a fruit, spirit stones); the cultivator stakes
 *   something of like worth from its own things (CultivatorLoot). The winner takes both.</li>
 *   <li><b>Don't hold back</b>: it fights with its full realm. Winning is worth {@link Regard#SPAR_WIN_FULL}; losing costs no
 *   more than any loss.</li>
 *   <li><b>Dao Duel</b> ({@link Terms#DUEL}): a General of the Celestial Court may challenge a Heavenly Venerable with its weapon in hand,
 *   however far above them it stands, staking {@link #DUEL_TOKENS} Court Merit Tokens; it holds back to their realm and fights with its
 *   weapon. Won, it hands the weapon over and keeps its post (Legends#handOver); lost, it keeps the tokens.</li>
 *   <li>Regard: a win {@link Regard#SPAR_WIN} ({@link Regard#SPAR_WIN_SENIOR} over a higher realm); a loss
 *   {@link Regard#SPAR_LOSS}, but {@link Regard#SPAR_LOSS_BADLY} if they were a realm or more below you or you walked off.
 *   Losing to a senior who is Warm to you can grant their Insight (once a day from each teacher).</li>
 * </ul>
 * Blows between the two are no attack on a sect (no alarm, no grudge; see CultivatorNpc#hurt), and neither of them dies of
 * the other's hand while it lasts; those that land in the moment after it ends are pulled ({@link #AFTERMATH_TICKS}), and so are
 * the player's stray blows on the partner's fellows in the bout or just after ({@link #allowDamage}). Wagers are only with
 * one not beneath the player's realm ({@link #beneath}).
 */
public final class SparSystem {
	public static final int COUNTDOWN_TICKS = 60;
	public static final int TIME_LIMIT_TICKS = 3 * 60 * 20;
	public static final double WALK_AWAY = 16;
	public static final float LINE_SHARE = 0.5f;
	/** Neither side starts a spar below this share of their health (so the line never lies below a quarter of it). */
	public static final float READY_SHARE = 0.75f;
	/** How hard blows between the two land: pulled, as one spars, not fights. */
	public static final float BLOW_SHARE = 0.5f;
	/**
	 * One of a sect's people on its own grounds leads its partner to where bouts are held, its training ground's sparring ring (the
	 * plaza for a sect without one), and the bout begins there (the user's ask, 2026-10-23: not on the spot): once it stands in its
	 * place and its partner within {@link #ARRIVED} of theirs. Not followed within {@link #LEAD_TICKS}, it is called off. Unable to
	 * reach its place for {@link #LEAD_STUCK_TICKS}, it begins where it stands, its partner by it. A rogue spars where it stands.
	 */
	public static final int LEAD_TICKS = 90 * 20, LEAD_STUCK_TICKS = 60 * 20;
	public static final double ARRIVED = 3;
	/** What a Dao Duel ({@link Terms#DUEL}) is staked in: this many Court Merit Tokens, held in hand. */
	public static final int DUEL_TOKENS = 32;

	/** What the player asked for: a wager, and whether the cultivator holds back. */
	public enum Terms {
		FRIENDLY(false, false), WAGER(true, false), FULL(false, true), WAGER_FULL(true, true),
		/** A sect's entrance trial (SectMembership): a friendly bout with the elder, held back to the player's realm; set by the elder only. */
		TRIAL(false, false),
		/**
		 * A Dao Duel (phase 3c of the Artifact Forge, the user's pick): a General of the Celestial Court challenges a Heavenly Venerable for
		 * its weapon (Legends#duelFor), staking {@link #DUEL_TOKENS} Court Merit Tokens; it holds back to their realm. Won, it hands its
		 * weapon over and keeps its post (Legends#handOver). Appended: synced by ordinal.
		 */
		DUEL(true, false);

		public final boolean wager;
		public final boolean full;

		Terms(boolean wager, boolean full) {
			this.wager = wager;
			this.full = full;
		}

		public static Terms byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : null;
		}
	}

	/** How a bout ended; CALLED_OFF: it never began, its partner never came to the sparring ground ({@link #LEAD_TICKS}). */
	public enum Outcome { WON, LOST, FORFEIT, DRAW, VOID, CALLED_OFF }

	/** One spar under way. */
	static final class Spar {
		final UUID player;
		final UUID npc;
		final ResourceKey<Level> dimension;
		final Terms terms;
		final List<ItemStack> playerStake;
		final List<ItemStack> npcStake;
		/** Regard before it began, and the realms against each other. */
		final int regardBefore;
		final boolean senior;
		final boolean junior;
		/** It is the stronger of the two (by rank), so holding back or not makes a difference. */
		final boolean stronger;
		/** The cultivator's sect (null for none): the player's stray blows on its fellows are pulled ({@link #allowDamage}). */
		final UUID sect;
		/** A Dao Duel's: the Venerable's weapon staked on it (handed over if it loses), or null. */
		Legend duel;
		float playerLine;
		float npcLine;
		int countdown = COUNTDOWN_TICKS;
		int ticks;
		/** Led to the sparring ground ({@link #LEAD_TICKS}): the cultivator's place and its partner's there, and ticks since it set off (-1: there, or none). */
		BlockPos npcSpot, playerSpot;
		int leading = -1;

		Spar(ServerPlayer player, CultivatorNpc npc, Terms terms, List<ItemStack> playerStake, List<ItemStack> npcStake) {
			this.player = player.getUUID();
			this.npc = npc.getUUID();
			this.dimension = npc.level().dimension();
			this.terms = terms;
			this.playerStake = playerStake;
			this.npcStake = npcStake;
			this.regardBefore = npc.regard(player);
			int theirs = npc.isMortal() ? -1 : CultivatorNpc.realmOf(npc.getRank()).ordinal();
			// The player's realm as its body is wielded, on any path (TwoPaths#wieldedRank; a mortal's none): by the heaven path's alone
			// before the sweep of 2026-10-23, so a ghost or demon path elder beat a "senior" it stood far above.
			int wielded = TwoPaths.wieldedRank(CultivationManager.get(player));
			int yours = wielded < 0 ? -1 : wielded / Stage.values().length;
			this.senior = theirs > yours;
			this.junior = theirs < yours;
			this.stronger = npc.sustainedRank() > CultivatorNpc.rankOf(player);
			this.sect = npc.getSectId();
		}
	}

	private static final Map<UUID, Spar> BY_PLAYER = new HashMap<>();
	private static final Map<UUID, Spar> BY_NPC = new HashMap<>();

	/**
	 * Blows between the two in this long after their spar ends are pulled ({@link #allowDamage}). The winning blow often ends it
	 * at once (one far stronger takes half its partner's health in a stroke), and the next swing a moment later landed
	 * as an attack in earnest: the whole sect turned out, and its barrier set its guest down outside.
	 */
	public static final int AFTERMATH_TICKS = 60;

	/** A spar just over: the player's side, until when (game time) blows between them are pulled, and the cultivator's sect. */
	private record Aftermath(UUID player, long until, UUID sect) {}

	/** By the cultivator's id. */
	private static final Map<UUID, Aftermath> AFTER = new HashMap<>();

	private static Spar of(Entity entity) {
		Spar spar = BY_PLAYER.get(entity.getUUID());
		return spar != null ? spar : BY_NPC.get(entity.getUUID());
	}

	public static boolean isSparring(Entity entity) {
		return of(entity) != null;
	}

	/** {@code a} and {@code b} are the two sides of one spar (or {@code b} is a projectile one of them loosed at the other). */
	public static boolean areSparring(Entity a, Entity b) {
		if (b instanceof Projectile projectile && projectile.getOwner() != null) b = projectile.getOwner();
		Spar spar = of(a);
		if (spar == null || a == b) return false;
		UUID other = a.getUUID().equals(spar.player) ? spar.npc : spar.player;
		return b.getUUID().equals(other);
	}

	/** {@code a} and {@code b} (either way round, or a projectile one of them loosed) ended a spar between them a moment ago. */
	public static boolean justSparred(Entity a, Entity b) {
		if (b instanceof Projectile projectile && projectile.getOwner() != null) b = projectile.getOwner();
		Aftermath after = AFTER.get(a.getUUID());
		if (after != null && after.player().equals(b.getUUID())) return a.level().getGameTime() < after.until();
		after = AFTER.get(b.getUUID());
		return after != null && after.player().equals(a.getUUID()) && b.level().getGameTime() < after.until();
	}

	/** How much of a blow on {@code entity} from {@code source} lands: {@link #BLOW_SHARE} of one from its partner in a spar. */
	public static float pulled(LivingEntity entity, DamageSource source, float amount) {
		return !BY_PLAYER.isEmpty() && source.getEntity() != null && areSparring(entity, source.getEntity()) ? amount * BLOW_SHARE : amount;
	}

	/**
	 * Whether a blow may land on {@code entity} (ALLOW_DAMAGE): not one from its partner in a spar just over, nor a sparring
	 * player's on one of the partner's fellows, in the bout or just after (a sweep, a spell's spread, the next swing gone wide:
	 * a stray blow raised the whole sect against a friendly bout).
	 */
	public static boolean allowDamage(LivingEntity entity, DamageSource source) {
		if (BY_PLAYER.isEmpty() && AFTER.isEmpty()) return true;
		Entity from = source.getEntity();
		if (from == null) return true;
		if (!AFTER.isEmpty() && justSparred(entity, from)) return false;
		if (!(entity instanceof CultivatorNpc npc) || npc.getSectId() == null || !(from instanceof ServerPlayer) || areSparring(entity, from)) return true;
		return !npc.getSectId().equals(sparSect(from, entity.level().getGameTime()));
	}

	/** Whether {@code player} is sparring with one of the sect {@code sect}'s people, or did a moment ago (its bout's aftermath). */
	public static boolean sparsWithSect(Entity player, UUID sect) {
		return sect != null && (!BY_PLAYER.isEmpty() || !AFTER.isEmpty()) && sect.equals(sparSect(player, player.level().getGameTime()));
	}

	/** The sect of the cultivator {@code player} is sparring with, or sparred with a moment ago; or null. */
	private static UUID sparSect(Entity player, long now) {
		Spar spar = BY_PLAYER.get(player.getUUID());
		if (spar != null) return spar.sect;
		for (Aftermath after : AFTER.values()) {
			if (after.player().equals(player.getUUID()) && now < after.until()) return after.sect();
		}
		return null;
	}

	/** Still in the three breaths before it begins. */
	public static boolean inCountdown(Entity entity) {
		Spar spar = of(entity);
		return spar != null && spar.countdown > 0;
	}

	/** What the player would stake: one of what they hold, if it is worth anything to a cultivator; else an empty stack. */
	public static ItemStack stakeInHand(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();
		return !held.isEmpty() && TalkSystem.worth(held.copyWithCount(1)) > 0 ? held.copyWithCount(1) : ItemStack.EMPTY;
	}

	/** Why a spar on {@code terms} can't begin (the line to say so), or null. */
	public static String refusal(ServerPlayer player, CultivatorNpc npc, Terms terms) {
		if (isSparring(player) || isSparring(npc)) return ModLang.SPAR_BUSY;
		if (player.getHealth() < player.getMaxHealth() * READY_SHARE || npc.getHealth() < npc.getMaxHealth() * READY_SHARE) {
			return ModLang.SPAR_HURT;
		}
		// A Dao Duel: a General of the Court against a Venerable with its weapon, staked in Court Merit Tokens, once a day with each.
		if (terms == Terms.DUEL) {
			Legend legend = Legends.duelFor(player, npc);
			if (legend == null) return ModLang.SPAR_NO_DUEL;
			if (!LegendaryWeaponItem.mayWield(player, legend)) return ModLang.SPAR_DUEL_TOO_WEAK;
			if (!duelStakeInHand(player)) return ModLang.SPAR_DUEL_NO_STAKE;
			if (npc.regardEntry(player).wagerDay == Regard.day(npc.level().getGameTime())) return ModLang.SPAR_WAGERED;
			return null;
		}
		// One a realm or more beneath you can't win your stake (it never holds back, only the stronger does), so it won't put up
		// its own: friendly bouts only (a strong cultivator wagered against the weak for a thing a day from every one of them).
		if (terms.wager && beneath(player, npc)) return ModLang.SPAR_BENEATH;
		if (terms.wager && stakeInHand(player).isEmpty()) return ModLang.SPAR_NO_STAKE;
		// One wager a day with each: no farming its things one bout after another.
		if (terms.wager && npc.regardEntry(player).wagerDay == Regard.day(npc.level().getGameTime())) return ModLang.SPAR_WAGERED;
		return null;
	}

	/** Whether {@code player} holds a Dao Duel's stake: {@link #DUEL_TOKENS} Court Merit Tokens in hand. */
	public static boolean duelStakeInHand(ServerPlayer player) {
		ItemStack held = player.getMainHandItem();
		return held.is(ModItems.COURT_MERIT_TOKEN) && held.getCount() >= DUEL_TOKENS;
	}

	/** {@code npc} is a realm or more beneath {@code player} (as it shows itself): it spars with them friendly only. */
	public static boolean beneath(ServerPlayer player, CultivatorNpc npc) {
		TalkSystem.Standing standing = TalkSystem.standing(player, npc);
		return standing == TalkSystem.Standing.BELOW || standing == TalkSystem.Standing.FAR_BELOW;
	}

	/** Begins a spar between {@code player} and {@code npc} on {@code terms}: stakes put up, three breaths, then the fight. */
	public static void start(ServerPlayer player, CultivatorNpc npc, Terms terms) {
		List<ItemStack> playerStake = new ArrayList<>();
		List<ItemStack> npcStake = new ArrayList<>();
		// A Dao Duel's stake: the player's tokens; the Venerable's, its weapon, which it fights with and hands over only if it loses.
		Legend duel = terms == Terms.DUEL ? Legends.duelFor(player, npc) : null;
		if (duel != null) {
			playerStake.add(player.getMainHandItem().split(DUEL_TOKENS));
			npc.regardEntry(player).wagerDay = Regard.day(npc.level().getGameTime());
		} else if (terms.wager) {
			ItemStack stake = stakeInHand(player);
			player.getMainHandItem().shrink(1);
			playerStake.add(stake);
			npcStake.addAll(CultivatorLoot.stakeFor(npc, TalkSystem.worth(stake)));
			npc.regardEntry(player).wagerDay = Regard.day(npc.level().getGameTime());
		}
		Spar spar = new Spar(player, npc, terms, playerStake, npcStake);
		spar.duel = duel;
		AFTER.remove(spar.npc);
		// A stronger cultivator holds back to the player's realm, unless told not to.
		int yours = CultivatorNpc.rankOf(player);
		boolean holdBack = !terms.full && npc.sustainedRank() > yours;
		npc.endTalk(player);
		npc.beginSpar(player, holdBack ? Math.max(0, yours) : CultivatorNpc.NO_HOLD_BACK);
		spar.playerLine = Math.max(1, player.getHealth() - player.getMaxHealth() * LINE_SHARE);
		spar.npcLine = Math.max(1, npc.getHealth() - npc.getMaxHealth() * LINE_SHARE);
		// One of a sect's people on its grounds takes them to its sparring ground first (LEAD_TICKS).
		BlockPos[] spots = sparringGround(npc);
		if (spots != null) {
			spar.npcSpot = spots[0];
			spar.playerSpot = spots[1];
			spar.leading = 0;
		}
		BY_PLAYER.put(spar.player, spar);
		BY_NPC.put(spar.npc, spar);
		Component stakes = npcStake.isEmpty() ? Component.empty() : Component.translatable(ModLang.SPAR_STAKES, describe(playerStake), describe(npcStake));
		TalkSystem.breakOff(player, npc, spots != null ? ModLang.SPAR_FOLLOW : ModLang.SPAR_BEGIN);
		if (!npcStake.isEmpty()) player.sendSystemMessage(stakes);
		if (duel != null) player.sendSystemMessage(Component.translatable(ModLang.SPAR_DUEL_BEGINS, DUEL_TOKENS, duel.getDisplayName()));
		npc.level().playSound(null, npc.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, 0.4f, 1.6f);
	}

	private static Component describe(List<ItemStack> stacks) {
		Component joined = Component.empty();
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			Component one = stack.getCount() > 1 ? Component.translatable(ModLang.SPAR_COUNT, stack.getCount(), stack.getHoverName()) : stack.getHoverName();
			joined = i == 0 ? one : Component.translatable(ModLang.TALK_AND, joined, one);
		}
		return joined;
	}

	public static void tick(MinecraftServer server) {
		if (!AFTER.isEmpty()) {
			long now = server.overworld().getGameTime();
			AFTER.values().removeIf(after -> now >= after.until());
		}
		if (BY_PLAYER.isEmpty()) return;
		for (Spar spar : new ArrayList<>(BY_PLAYER.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(spar.player);
			ServerLevel level = server.getLevel(spar.dimension);
			Entity entity = level == null ? null : level.getEntity(spar.npc);
			CultivatorNpc npc = entity instanceof CultivatorNpc cultivator && cultivator.isAlive() ? cultivator : null;
			if (player == null || !player.isAlive()) {
				end(spar, Outcome.FORFEIT, player, npc);
				continue;
			}
			// Gone, elsewhere, or its body taken by a soul (Possession: it is set aside, and the one in it is no partner of this bout).
			if (npc == null || npc.isVacant() || player.level() != npc.level()) {
				end(spar, Outcome.VOID, player, npc);
				continue;
			}
			// On the way to the sparring ground: the bout begins once both are there.
			if (spar.leading >= 0) {
				lead(spar, player, npc);
				continue;
			}
			if (spar.countdown > 0) {
				npc.getNavigation().stop();
				npc.getLookControl().setLookAt(player, 30.0f, 30.0f);
				if (spar.countdown % 20 == 0) {
					player.displayClientMessage(Component.translatable(ModLang.SPAR_COUNTDOWN, spar.countdown / 20), true);
					player.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.8f, 1.0f);
				}
				if (--spar.countdown == 0) {
					player.displayClientMessage(Component.translatable(ModLang.SPAR_FIGHT), true);
					player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
					npc.setTarget(player);
				}
				continue;
			}
			if (npc.getTarget() == null && !npc.isBegging()) npc.setTarget(player);
			// A blow past the line stops at it (one from anyone: the spar is over all the same).
			if (player.getHealth() <= spar.playerLine) {
				player.setHealth(Math.max(player.getHealth(), spar.playerLine));
				end(spar, Outcome.LOST, player, npc);
			} else if (npc.getHealth() <= spar.npcLine) {
				npc.setHealth(Math.max(npc.getHealth(), spar.npcLine));
				end(spar, Outcome.WON, player, npc);
			} else if (player.distanceTo(npc) > WALK_AWAY) {
				// Apart: the player walked off (a forfeit), unless it was the cultivator that ran (from someone else's domain): no result.
				end(spar, npc.shouldFlee() ? Outcome.VOID : Outcome.FORFEIT, player, npc);
			} else if (++spar.ticks >= TIME_LIMIT_TICKS) {
				end(spar, Outcome.DRAW, player, npc);
			}
		}
	}

	/**
	 * Where {@code npc} would hold a bout: on its own sect's grounds, its place and its partner's in the sparring ring (NpcLife#boutSpots);
	 * null for a rogue, one off its grounds, or a ring with no room to stand: it spars where it stands.
	 */
	static BlockPos[] sparringGround(CultivatorNpc npc) {
		Sect sect = npc.sect();
		if (sect == null || sect.extinct || !(npc.level() instanceof ServerLevel level) || sect.dimension != level.dimension()
				|| !sect.inTerritory(npc.blockPosition())) {
			return null;
		}
		return NpcLife.boutSpots(level, sect, level.random);
	}

	/** A tick of {@code npc} leading {@code player} to the sparring ground ({@link #LEAD_TICKS}): the countdown once both are there. */
	private static void lead(Spar spar, ServerPlayer player, CultivatorNpc npc) {
		spar.leading++;
		Vec3 spot = Vec3.atBottomCenterOf(spar.npcSpot);
		boolean there = npc.position().distanceToSqr(spot) <= 1.5 * 1.5;
		if (!there) {
			if (npc.getNavigation().isDone() || spar.leading % 20 == 0) npc.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.9);
		} else {
			npc.getNavigation().stop();
			npc.getLookControl().setLookAt(player, 30.0f, 30.0f);
		}
		boolean come = player.position().distanceToSqr(Vec3.atBottomCenterOf(spar.playerSpot)) <= ARRIVED * ARRIVED;
		if (there && come || spar.leading >= LEAD_STUCK_TICKS && player.distanceTo(npc) <= 6) {
			spar.leading = -1; // both there: its three breaths, from the next tick
			npc.getNavigation().stop();
			return;
		}
		if (spar.leading >= LEAD_TICKS) {
			end(spar, Outcome.CALLED_OFF, player, npc);
			return;
		}
		if (spar.leading % 60 == 0) {
			player.displayClientMessage(Component.translatable(there ? ModLang.SPAR_WAITING : ModLang.SPAR_LEADING, Possession.nameOf(npc)), true);
		}
	}

	/**
	 * A blow that would kill one duellist, from the other: it stops at the line instead and ends the spar (ALLOW_DEATH, see
	 * CultivationEvents). Returns false to cancel the death.
	 */
	public static boolean allowDeath(LivingEntity entity, DamageSource source) {
		Spar spar = of(entity);
		if (spar == null || source.getEntity() == null || !areSparring(entity, source.getEntity())) return true;
		boolean isPlayer = entity.getUUID().equals(spar.player);
		entity.setHealth(isPlayer ? spar.playerLine : spar.npcLine);
		return false;
	}

	private static void end(Spar spar, Outcome outcome, ServerPlayer player, CultivatorNpc npc) {
		BY_PLAYER.remove(spar.player);
		BY_NPC.remove(spar.npc);
		if (npc != null) npc.endSpar(player);
		if (npc != null && player != null) AFTER.put(spar.npc, new Aftermath(spar.player, npc.level().getGameTime() + AFTERMATH_TICKS, spar.sect));
		// The cultivator's own stake is only ever what it puts up to be won (CultivatorLoot#stakeFor makes it, at the wager): only the
		// player takes it, winning. It kept it too before the sweep of 2026-10-23, however a bout ended but won, so each wager made it
		// that much richer out of nothing, for whoever slew it after.
		if (player == null) {
			// Gone mid-spar (logged out): the cultivator keeps the player's stake.
			if (npc != null) npc.keepWinnings(spar.playerStake);
			return;
		}
		if (player.getLastHurtByMob() == npc) player.setLastHurtByMob(null);
		if (npc == null) {
			// Gone (died of something else, or out of the world): the player's stake comes back.
			give(player, spar.playerStake);
			return;
		}
		if (outcome == Outcome.WON) Milestones.award(player, Milestones.SPAR_WON);
		int delta = switch (outcome) {
			case WON -> spar.terms.full && spar.stronger ? Regard.SPAR_WIN_FULL : spar.senior ? Regard.SPAR_WIN_SENIOR : Regard.SPAR_WIN;
			case LOST -> spar.junior ? Regard.SPAR_LOSS_BADLY : Regard.SPAR_LOSS;
			case FORFEIT -> Regard.SPAR_LOSS_BADLY;
			case DRAW, VOID, CALLED_OFF -> 0;
		};
		// A spar moves its regard once a day; later bouts the same day are for the practice.
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		if (delta != 0 && entry.sparDay == today) delta = 0;
		if (delta != 0) entry.sparDay = today;
		npc.changeRegard(player, delta);
		switch (outcome) {
			case WON -> {
				give(player, spar.playerStake);
				give(player, spar.npcStake);
				if (spar.duel != null) Legends.handOver(npc, player, spar.duel); // a Dao Duel won: its weapon theirs, its post kept
			}
			case LOST, FORFEIT -> npc.keepWinnings(spar.playerStake);
			case DRAW, VOID, CALLED_OFF -> give(player, spar.playerStake);
		}
		if (npc.isVacant()) return; // its body taken by a soul (Possession): set aside, it says nothing
		if (outcome == Outcome.CALLED_OFF) {
			// Never came to the sparring ground: no bout, and no trial tried (it may be asked again).
			TalkSystem.say(player, npc, TalkSystem.line("spar.called_off", npc), 0);
			return;
		}
		if (spar.terms == Terms.TRIAL && outcome != Outcome.VOID) {
			// The entrance trial: won or held to the time, the elder takes them in; else not today (SectMembership#onTrial).
			TalkSystem.say(player, npc, Component.translatable(ModLang.talkLine(SectMembership.onTrial(player, npc, outcome))), delta);
			npc.level().playSound(null, npc.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.3f, 1.8f);
			return;
		}
		String line = switch (outcome) {
			case WON -> "spar.won";
			case LOST -> "spar.lost";
			case FORFEIT -> "spar.forfeit";
			case DRAW, VOID, CALLED_OFF -> "spar.draw";
		};
		TalkSystem.say(player, npc, TalkSystem.line(line, npc), delta);
		npc.level().playSound(null, npc.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.3f, 1.8f);
		// Beaten by a senior who likes them, the player is shown where they went wrong: the senior's Insight, once a day.
		if (outcome == Outcome.LOST && spar.senior && spar.regardBefore >= Regard.WARM) TalkSystem.grantInsight(player, npc);
	}

	/** Into the player's inventory, what doesn't fit at their feet (vanilla's add keeps no part it couldn't place, if any fit). */
	private static void give(ServerPlayer player, List<ItemStack> stacks) {
		for (ItemStack stack : stacks) player.getInventory().placeItemBackInInventory(stack.copy());
	}

	/** Any spar the player is in ends at once (they die, leave, or the server stops). */
	public static void abandon(ServerPlayer player) {
		Spar spar = BY_PLAYER.get(player.getUUID());
		if (spar == null) return;
		ServerLevel level = player.server.getLevel(spar.dimension);
		CultivatorNpc npc = level != null && level.getEntity(spar.npc) instanceof CultivatorNpc cultivator ? cultivator : null;
		end(spar, Outcome.FORFEIT, player.isAlive() ? player : null, npc);
	}

	/** Every spar ends without a result (server stopping): stakes go back to the players still here. */
	public static void clear(MinecraftServer server) {
		for (Spar spar : new ArrayList<>(BY_PLAYER.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(spar.player);
			ServerLevel level = server.getLevel(spar.dimension);
			CultivatorNpc npc = level != null && level.getEntity(spar.npc) instanceof CultivatorNpc cultivator ? cultivator : null;
			if (npc != null) npc.endSpar(player);
			if (player != null) give(player, spar.playerStake);
		}
		BY_PLAYER.clear();
		BY_NPC.clear();
		AFTER.clear();
	}

	private SparSystem() {}
}
