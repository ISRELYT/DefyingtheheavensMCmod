package com.example.defyingtheheavens;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sparring with a cultivator (chosen from the talk wheel, see TalkSystem): a duel to first blood. It ends when either side
 * has lost {@link #LINE_SHARE} of their maximum health since it began, when the player walks {@link #WALK_AWAY} blocks off
 * (a forfeit), or after {@link #TIME_LIMIT_TICKS} (a draw). Nobody dies: a blow from the other duellist that would go past
 * the line stops at it. A stronger cultivator holds back to the player's realm, unless asked not to.
 * <ul>
 *   <li><b>Wager</b>: the player stakes one of what they hold (a herb, a pill, a fruit, spirit stones); the cultivator stakes
 *   something of like worth from its own things (CultivatorLoot). The winner takes both.</li>
 *   <li><b>Don't hold back</b>: it fights with its full realm. Winning is worth {@link Regard#SPAR_WIN_FULL}; losing costs no
 *   more than any loss.</li>
 *   <li>Regard: a win {@link Regard#SPAR_WIN} ({@link Regard#SPAR_WIN_SENIOR} over a higher realm); a loss
 *   {@link Regard#SPAR_LOSS}, but {@link Regard#SPAR_LOSS_BADLY} if they were a realm or more below you or you walked off.
 *   Losing to a senior who is Warm to you can grant their Insight (once a day from each teacher).</li>
 * </ul>
 * Blows between the two are no attack on a sect (no alarm, no grudge; see CultivatorNpc#hurt), and neither of them dies of
 * the other's hand while it lasts.
 */
public final class SparSystem {
	public static final int COUNTDOWN_TICKS = 60;
	public static final int TIME_LIMIT_TICKS = 3 * 60 * 20;
	public static final double WALK_AWAY = 16;
	public static final float LINE_SHARE = 0.25f;
	/** Neither side starts a spar below this share of their health. */
	public static final float READY_SHARE = 0.5f;

	/** What the player asked for: a wager, and whether the cultivator holds back. */
	public enum Terms {
		FRIENDLY(false, false), WAGER(true, false), FULL(false, true), WAGER_FULL(true, true),
		/** A sect's entrance trial (SectMembership): a friendly bout with the elder, held back to the player's realm; set by the elder only. */
		TRIAL(false, false);

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

	public enum Outcome { WON, LOST, FORFEIT, DRAW, VOID }

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
		float playerLine;
		float npcLine;
		int countdown = COUNTDOWN_TICKS;
		int ticks;

		Spar(ServerPlayer player, CultivatorNpc npc, Terms terms, List<ItemStack> playerStake, List<ItemStack> npcStake) {
			this.player = player.getUUID();
			this.npc = npc.getUUID();
			this.dimension = npc.level().dimension();
			this.terms = terms;
			this.playerStake = playerStake;
			this.npcStake = npcStake;
			this.regardBefore = npc.regard(player);
			int theirs = npc.isMortal() ? -1 : CultivatorNpc.realmOf(npc.getRank()).ordinal();
			int yours = CultivationManager.get(player).getRealm().ordinal();
			this.senior = theirs > yours;
			this.junior = theirs < yours;
			this.stronger = npc.sustainedRank() > CultivatorNpc.rankOf(player);
		}
	}

	private static final Map<UUID, Spar> BY_PLAYER = new HashMap<>();
	private static final Map<UUID, Spar> BY_NPC = new HashMap<>();

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
		if (terms.wager && stakeInHand(player).isEmpty()) return ModLang.SPAR_NO_STAKE;
		// One wager a day with each: no farming its things one bout after another.
		if (terms.wager && npc.regardEntry(player).wagerDay == Regard.day(npc.level().getGameTime())) return ModLang.SPAR_WAGERED;
		return null;
	}

	/** Begins a spar between {@code player} and {@code npc} on {@code terms}: stakes put up, three breaths, then the fight. */
	public static void start(ServerPlayer player, CultivatorNpc npc, Terms terms) {
		List<ItemStack> playerStake = new ArrayList<>();
		List<ItemStack> npcStake = new ArrayList<>();
		if (terms.wager) {
			ItemStack stake = stakeInHand(player);
			player.getMainHandItem().shrink(1);
			playerStake.add(stake);
			npcStake.addAll(CultivatorLoot.stakeFor(npc, TalkSystem.worth(stake)));
			npc.regardEntry(player).wagerDay = Regard.day(npc.level().getGameTime());
		}
		Spar spar = new Spar(player, npc, terms, playerStake, npcStake);
		// A stronger cultivator holds back to the player's realm, unless told not to.
		int yours = CultivatorNpc.rankOf(player);
		boolean holdBack = !terms.full && npc.sustainedRank() > yours;
		npc.endTalk(player);
		npc.beginSpar(holdBack ? Math.max(0, yours) : CultivatorNpc.NO_HOLD_BACK);
		spar.playerLine = Math.max(1, player.getHealth() - player.getMaxHealth() * LINE_SHARE);
		spar.npcLine = Math.max(1, npc.getHealth() - npc.getMaxHealth() * LINE_SHARE);
		BY_PLAYER.put(spar.player, spar);
		BY_NPC.put(spar.npc, spar);
		Component stakes = npcStake.isEmpty() ? Component.empty() : Component.translatable(ModLang.SPAR_STAKES, describe(playerStake), describe(npcStake));
		TalkSystem.breakOff(player, npc, ModLang.SPAR_BEGIN);
		if (!npcStake.isEmpty()) player.sendSystemMessage(stakes);
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
			if (npc == null || player.level() != npc.level()) {
				end(spar, Outcome.VOID, player, npc);
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
				end(spar, Outcome.FORFEIT, player, npc);
			} else if (++spar.ticks >= TIME_LIMIT_TICKS) {
				end(spar, Outcome.DRAW, player, npc);
			}
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
		if (player == null) {
			// Gone mid-spar (logged out): the cultivator keeps the stakes, the player's included.
			if (npc != null) {
				npc.keepWinnings(spar.playerStake);
				npc.keepWinnings(spar.npcStake);
			}
			return;
		}
		if (player.getLastHurtByMob() == npc) player.setLastHurtByMob(null);
		if (npc == null) {
			// Gone (died of something else, or out of the world): the player's stake comes back.
			give(player, spar.playerStake);
			return;
		}
		int delta = switch (outcome) {
			case WON -> spar.terms.full && spar.stronger ? Regard.SPAR_WIN_FULL : spar.senior ? Regard.SPAR_WIN_SENIOR : Regard.SPAR_WIN;
			case LOST -> spar.junior ? Regard.SPAR_LOSS_BADLY : Regard.SPAR_LOSS;
			case FORFEIT -> Regard.SPAR_LOSS_BADLY;
			case DRAW, VOID -> 0;
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
			}
			case LOST, FORFEIT -> {
				npc.keepWinnings(spar.playerStake);
				npc.keepWinnings(spar.npcStake);
			}
			case DRAW, VOID -> {
				give(player, spar.playerStake);
				npc.keepWinnings(spar.npcStake);
			}
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
			case DRAW, VOID -> "spar.draw";
		};
		TalkSystem.say(player, npc, TalkSystem.line(line, npc), delta);
		npc.level().playSound(null, npc.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.3f, 1.8f);
		// Beaten by a senior who likes them, the player is shown where they went wrong: the senior's Insight, once a day.
		if (outcome == Outcome.LOST && spar.senior && spar.regardBefore >= Regard.WARM) TalkSystem.grantInsight(player, npc);
	}

	/** Into the player's inventory, or at their feet. */
	private static void give(ServerPlayer player, List<ItemStack> stacks) {
		for (ItemStack stack : stacks) {
			ItemStack copy = stack.copy();
			if (!player.getInventory().add(copy)) player.drop(copy, false);
		}
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
	}

	private SparSystem() {}
}
