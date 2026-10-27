package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Talking to cultivators. Right-click one (from up to {@link #REACH} blocks) and, if they will talk, the talk wheel opens
 * (client: TalkWheelScreen) with what you can mean by it ({@link TalkIntent}). Greet, Gift, Provoke and the rest are answered
 * in one line; Trade opens the trading screen; Ask and Spar open the dialogue box (DialogueScreen): Ask with topics answered
 * from the world as it is (who they are, their sect and its people, the sects nearby, rumours of real things, guidance), Spar
 * with its terms (SparSystem).
 * <ul>
 *   <li><b>Standing</b> ({@link Standing}): their realm against yours, as each side shows it (so Realm Suppression and
 *   concealment change it), sets how they speak to you and what they will entertain ({@link #intents}). Their path sets the
 *   tone.</li>
 *   <li><b>Regard</b> ({@link Regard}): what they think of you, changed by what you do here and elsewhere. Courteous opens
 *   rumours (and Spar with a senior), Warm a senior's guidance; at Sworn they give you something now and then, and come to
 *   your aid when something attacks you near them.</li>
 * </ul>
 * They won't talk in a fight, in seclusion (trying costs a little regard, once a day), on their way to the sky, while turned
 * on you, or with another's soul inside them; if one of those comes about mid-talk they break it off, and the player's talk
 * window closes with a line saying why (S2C talk_close). Being struck yourself breaks a talk off too. An enemy of their path
 * doesn't talk: its wheel offers only Provoke and Leave, unless you are two realms above it, when it begs to be spared.
 * Talking to one who sits in meditation wakes them; elders and the Sect Master on duty don't mind, anyone else thinks a
 * little less of you.
 */
public final class TalkSystem {
	/** How close a player must be to start talking, and to keep talking. */
	public static final double REACH = 6.0;
	public static final double KEEP = 8.0;
	/** Sects further than this aren't known of. */
	private static final double KNOWN_SECTS = 2000;
	/** A Sworn friend gives you something now and then: at most this often (days), and this likely when you greet them. */
	private static final long GIFT_BACK_DAYS = 3;
	private static final float GIFT_BACK_CHANCE = 0.5f;
	/** A Sworn friend this close comes to your aid. */
	private static final double AID_REACH = 24;
	/** Topics are 0-99 in the dialogue box; spar terms are this plus the terms' index. */
	public static final int SPAR_TERMS = 100;

	/** Their realm against yours. */
	public enum Standing {
		FAR_ABOVE("far_above"), ABOVE("above"), PEER("peer"), BELOW("below"), FAR_BELOW("far_below"),
		/** The player is a mortal: spoken to as one, whatever the cultivator. */
		MORTAL("mortal");

		private final String id;

		Standing(String id) {
			this.id = id;
		}

		public String getId() { return id; }
	}

	/** What one can ask about. */
	public enum Topic {
		SELF("self"), SECT("sect"), PEOPLE("people"), NEARBY("nearby"), RUMOURS("rumours"), GUIDANCE("guidance"),
		/** Meng Po's soup (GhostGate#drinkSoup), asked of her alone. */
		SOUP("soup"),
		/** A living cultivator's heaven method (CultivatorNpc#heavenMethod), taught by a sect's elders and by rogues. */
		METHOD("method"),
		/** Asking to join the sect (SectMembership): an elder judges, and sets the entrance trial. */
		JOIN("join"),
		/** The entrance trial, once an elder has set it: a spar with them (SparSystem.Terms#TRIAL). */
		TRIAL("trial"),
		/** Leaving the sect one belongs to (an elder releases you). */
		LEAVE_SECT("leave_sect"),
		/** A settler's wish (Wishes), only a Four Axis cultivator can grant; its own choices follow under Wishes#ACTIONS. */
		WISH("wish"),
		/** The Celestial Court (CelestialCourt), asked of a Heavenly Venerable: taken into it, or told where you stand in it. Appended. */
		COURT("court"),
		/** The primordial beasts (PrimordialBeasts), asked of a Heavenly Venerable: the way to each, or when the next comes. Appended. */
		BEASTS("beasts");

		private final String id;

		Topic(String id) {
			this.id = id;
		}

		public String getId() { return id; }

		public static Topic byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : null;
		}
	}

	/** One line on the right of the dialogue box: a topic to ask about, or terms to spar on. */
	public record Reply(int id, Component label, boolean enabled, Component note) {}

	public static Standing standing(ServerPlayer player, CultivatorNpc npc) {
		PlayerCultivation c = CultivationManager.get(player);
		if (c.isMortal()) return npc.isMortal() ? Standing.PEER : Standing.MORTAL;
		int theirs = npc.isMortal() ? -1 : npc.getDisplayedRealm().ordinal();
		int gap = theirs - TwoPaths.wieldedRealm(c).ordinal(); // the player's body, either path (TwoPaths#wieldedRank)
		if (gap >= 2) return Standing.FAR_ABOVE;
		if (gap == 1) return Standing.ABOVE;
		if (gap == 0) return Standing.PEER;
		return gap == -1 ? Standing.BELOW : Standing.FAR_BELOW;
	}

	/** The righteous and the devilish: they don't talk, only fight (or, far beneath you, beg). */
	public static boolean isPathEnemy(ServerPlayer player, CultivatorNpc npc) {
		return npc.getFaction().isEnemyOf(Alignment.factionOf(CultivatorNpc.alignmentOf(player)));
	}

	/**
	 * The intents a cultivator at {@code standing} entertains, before regard and pressure: far above you, or to a mortal, no insults
	 * and no bouts. Everyone trades, however far above you (a Ghost King too: the user's ask, 2026-10-19, to see what they have
	 * early), what their regard hasn't opened shown but not sold (CultivatorTrades#offersFor).
	 */
	public static int intents(Standing standing) {
		int mask = TalkIntent.GREET.bit() | TalkIntent.ASK.bit() | TalkIntent.GIFT.bit() | TalkIntent.TRADE.bit();
		if (standing == Standing.FAR_ABOVE) return mask;
		if (standing != Standing.MORTAL) mask |= TalkIntent.PROVOKE.bit() | TalkIntent.SPAR.bit();
		return mask;
	}

	/**
	 * What {@code npc} will entertain from {@code player} now (as {@link TalkIntent#bit()}s): begging, only Spare or Leave; an
	 * enemy of their path, Provoke or Leave (or Spare, far beneath you); otherwise by standing ({@link #intents(Standing)}),
	 * with Spar for cultivators only (a senior wants Courteous regard first), and Demand and Release while you press your realm
	 * on one two realms beneath you.
	 */
	public static int intents(ServerPlayer player, CultivatorNpc npc, Standing standing) {
		int leaveOrSpare = TalkIntent.SPARE.bit() | TalkIntent.LEAVE.bit();
		if (npc.isBeggingTo(player)) return leaveOrSpare;
		if (isPathEnemy(player, npc)) return standing == Standing.FAR_BELOW ? leaveOrSpare : TalkIntent.PROVOKE.bit() | TalkIntent.LEAVE.bit();
		int mask = intents(standing);
		boolean spar = !npc.isMortal() && (standing != Standing.ABOVE || npc.regard(player) >= Regard.COURTEOUS);
		if (!spar) mask &= ~TalkIntent.SPAR.bit();
		if (kneelsTo(player, npc)) mask |= TalkIntent.DEMAND.bit() | TalkIntent.RELEASE.bit();
		// The dead press nobody: they greet, ask, trade and give (with the spirit stones, Yin Jade and slips they carry:
		// Incorporeal#mayCarry), spar, provoke or leave, as their standing allows, as the living do.
		if (GhostSystem.isGhost(player)) mask &= GHOST_INTENTS;
		return mask;
	}

	/** What a ghost may do in a talk, with the dead or the living (those who sense it: CultivatorNpc#senses). */
	public static final int GHOST_INTENTS = TalkIntent.GREET.bit() | TalkIntent.ASK.bit() | TalkIntent.TRADE.bit() | TalkIntent.GIFT.bit()
			| TalkIntent.SPAR.bit() | TalkIntent.PROVOKE.bit() | TalkIntent.LEAVE.bit();

	/**
	 * It kneels to {@code player}: four realms or more beneath them (NpcAddress#KNEEL_GAP: the user's pick, 2026-10-27, "Kneel from 4
	 * realms"), or two beneath them with their realm pressed down on it; never while they have let it up (Release, for its five minutes:
	 * RealmSuppressSystem#spares).
	 */
	public static boolean kneelsTo(ServerPlayer player, CultivatorNpc npc) {
		if (RealmSuppressSystem.spares(player, npc)) return false;
		return NpcAddress.gap(player, npc) >= NpcAddress.KNEEL_GAP || standing(player, npc) == Standing.FAR_BELOW && RealmSuppressSystem.presses(player, npc);
	}

	/** How close one it kneels to must come for it to kneel as they pass, and how far they may go again before it rises. */
	public static final double KNEEL_REACH = 5, RISE_REACH = 8;

	/**
	 * The player {@code npc} kneels before as they pass (the nearest one it {@link #kneelsTo} within {@link #KNEEL_REACH}, or still within
	 * {@link #RISE_REACH} of the one it knelt to, in its sight), or null: never while it fights, runs, sits in meditation, flies, begs, or is
	 * turned on them.
	 */
	public static ServerPlayer kneelsBefore(CultivatorNpc npc, ServerPlayer kneltTo) {
		if (npc.getTarget() != null || npc.shouldFlee() || npc.isMeditating() || npc.isQiFlying() || npc.isBegging() || npc.isVacant()) return null;
		if (kneltTo != null && kneltTo.isAlive() && kneltTo.level() == npc.level() && kneltTo.distanceToSqr(npc) <= RISE_REACH * RISE_REACH
				&& !npc.isHostileTo(kneltTo) && kneelsTo(kneltTo, npc)) return kneltTo;
		ServerPlayer best = null;
		double nearest = KNEEL_REACH * KNEEL_REACH;
		for (ServerPlayer p : ((net.minecraft.server.level.ServerLevel) npc.level()).players()) {
			if (p.isSpectator() || !p.isAlive() || !npc.senses(p)) continue;
			double d = p.distanceToSqr(npc);
			if (d > nearest || npc.isHostileTo(p) || !kneelsTo(p, npc) || !npc.hasLineOfSight(p)) continue;
			nearest = d;
			best = p;
		}
		return best;
	}

	// --- Opening a talk ---

	/**
	 * Why they won't talk to {@code player} (the line to say so), or null if they will. Asked again every tick of a talk, which
	 * breaks off when it isn't null (see CultivatorNpc#customServerAiStep).
	 */
	static String refusal(ServerPlayer player, CultivatorNpc npc) {
		// Someone else's soul sits in it (SoulProjection): nobody answers, and nothing gives away why.
		if (SoulProjection.isPossessed(npc)) return ModLang.TALK_POSSESSED;
		// It doesn't see you at all (in a Ghost King's form, to the living: CultivatorNpc#senses): there is nobody there to answer. A
		// Ghost King of the dead is told how it would be seen: in its human form.
		if (!npc.senses(player)) {
			return Incorporeal.isGhost(player) && GhostSystem.isKing(player) && !ModDimensions.isNetherworld(npc.level().dimension())
					? ModLang.TALK_UNSEEN_KING : ModLang.TALK_UNSEEN;
		}
		// Begging for its life, it hears nothing but the one it begs.
		if (npc.isBeggingTo(player)) return null;
		if (npc.isBegging()) return ModLang.TALK_BUSY;
		if (SparSystem.isSparring(npc)) return ModLang.TALK_BUSY;
		if (npc.getLifecycle() == CultivatorNpc.Lifecycle.SECLUDED && npc.isConcealed()) return ModLang.NPC_GREET_SECLUDED;
		// Turned on you (or loathing you enough to): no talk. An enemy of its path that isn't fighting you yet gets the short wheel.
		if (npc.getTarget() == player || npc.isEnemy(player) && !isPathEnemy(player, npc)) return ModLang.NPC_GREET_HOSTILE;
		if (npc.isInCombat() || npc.shouldFlee() || npc.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING
				|| npc.getLifecycle() == CultivatorNpc.Lifecycle.ASCENDING) return ModLang.TALK_BUSY;
		return null;
	}

	/** A right-click from up to {@link #REACH} blocks (C2S talk_request, the client finding a cultivator past arm's length). */
	public static void request(ServerPlayer player, int entityId) {
		if (!(player.level().getEntity(entityId) instanceof CultivatorNpc npc) || player.isSpectator() || !player.isAlive()) return;
		if (SoulProjection.isSoul(player) || player.isPassenger()) return;
		if (npc.distanceToSqr(player.getEyePosition()) > (REACH + 1.5) * (REACH + 1.5) || !player.hasLineOfSight(npc)) return;
		open(player, npc);
	}

	/** A right-click: opens the talk wheel, or says in a line why they won't talk. */
	public static void open(ServerPlayer player, CultivatorNpc npc) {
		if (player.isSpectator() || !npc.isAlive()) return;
		String refusal = refusal(player, npc);
		if (refusal != null) {
			int delta = 0;
			// Pressing up against a secluded cultivator's barrier to talk: they think a little less of you, once a day.
			if (refusal.equals(ModLang.NPC_GREET_SECLUDED)) {
				Regard.Entry entry = npc.regardEntry(player);
				long today = Regard.day(npc.level().getGameTime());
				if (entry.disturbDay != today) {
					entry.disturbDay = today;
					delta = Regard.SECLUSION_DISTURBED;
					npc.changeRegard(player, delta);
				}
			}
			player.displayClientMessage(withDelta(Component.translatable(refusal, nameOf(npc)), delta), true);
			return;
		}
		Player partner = npc.talkPartner();
		if (partner != null && partner != player) {
			player.displayClientMessage(Component.translatable(ModLang.TALK_OCCUPIED, nameOf(npc)), true);
			return;
		}
		Standing standing = standing(player, npc);
		// An enemy of its path, two realms beneath you: it throws itself down and begs.
		if (isPathEnemy(player, npc) && standing == Standing.FAR_BELOW && !npc.isBeggingTo(player)) beginBegging(player, npc);
		if (npc.isMeditating()) {
			npc.setMeditating(false);
			boolean onDuty = npc.getLifecycle() == CultivatorNpc.Lifecycle.SECT && npc.getTitle().isElderOrAbove();
			if (!onDuty) npc.changeRegard(player, Regard.DISTURBED);
		}
		npc.beginTalk(player);
		if (kneelsTo(player, npc)) {
			npc.setKneeling(true);
			Milestones.award(player, Milestones.KNEEL);
		}
		ModPackets.sendTalkOpen(player, npc, npc.regard(player), intents(player, npc, standing));
		Milestones.award(player, Milestones.TALK);
	}

	/**
	 * The cultivator {@code player} is talking with, if it is the entity they name, still in reach and still willing. A talk
	 * that lapsed while the player read (nobody else having taken it up) picks up again. When they won't go on, the player is
	 * told why and their talk window closes ({@link #breakOff}); one that is gone or out of reach the client closes itself.
	 */
	private static CultivatorNpc partner(ServerPlayer player, int entityId) {
		Entity entity = player.level().getEntity(entityId);
		if (!(entity instanceof CultivatorNpc npc) || !npc.isAlive() || player.isSpectator()) return null;
		if (npc.distanceToSqr(player) > KEEP * KEEP) {
			npc.endTalk(player);
			return null;
		}
		String refusal = refusal(player, npc);
		if (refusal != null) {
			npc.endTalk(player);
			breakOff(player, npc, refusal);
			return null;
		}
		Player other = npc.talkPartner();
		if (other != null && other != player) {
			breakOff(player, npc, ModLang.TALK_OCCUPIED);
			return null;
		}
		return npc;
	}

	/**
	 * {@code npc} won't talk with {@code player} any longer: their talk window closes, and the line of {@code key} says why over
	 * the hotbar (shown by the client if this talk was still its own).
	 */
	public static void breakOff(ServerPlayer player, CultivatorNpc npc, String key) {
		ModPackets.sendTalkClose(player, npc.getId(), Component.translatable(key, nameOf(npc)));
	}

	/** The player closed the talk. */
	public static void end(ServerPlayer player, int entityId) {
		if (player.level().getEntity(entityId) instanceof CultivatorNpc npc) npc.endTalk(player);
	}

	/**
	 * {@code player} has been hurt: any talk they were in breaks off (being struck is no time for words), and Sworn friends of
	 * theirs close by come to their aid against whoever did it.
	 */
	public static void onPlayerHurt(ServerPlayer player, DamageSource source) {
		for (CultivatorNpc npc : CultivatorNpc.talkingWith(player)) {
			npc.endTalk(player);
			breakOff(player, npc, ModLang.TALK_INTERRUPTED);
		}
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player || SparSystem.areSparring(player, attacker)) return;
		if (attacker instanceof Player other && (other.isCreative() || other.isSpectator())) return;
		AABB around = player.getBoundingBox().inflate(AID_REACH);
		for (CultivatorNpc friend : player.level().getEntitiesOfClass(CultivatorNpc.class, around, npc -> npc.isAlive() && npc != attacker)) {
			if (friend.getTarget() != null || friend.isInCombat() || friend.isAlly(attacker) || friend.isBegging() || friend.isConcealed()) continue;
			// Sworn friends, and one's own sect's members (SectMembership): they stand by their own.
			if (SoulProjection.isPossessed(friend) || SparSystem.isSparring(friend)) continue;
			// What preys on a settlement (a Merit wish's threat) is its taker's to slay: its settlers keep home (CultivatorGoals.ShelterGoal).
			if (friend.settledAt() != null && Wishes.isThreat(attacker)) continue;
			if (friend.regard(player) < Regard.SWORN && !SectMembership.areFellows(friend, player)) continue;
			friend.setMeditating(false);
			friend.setTarget(attacker);
			player.displayClientMessage(Component.translatable(ModLang.TALK_AID, nameOf(friend)), true);
		}
	}

	// --- Intents ---

	/** The player chose an intent on the wheel (or a tab of the dialogue box). */
	public static void intent(ServerPlayer player, int entityId, int index) {
		CultivatorNpc npc = partner(player, entityId);
		TalkIntent intent = TalkIntent.byIndex(index);
		if (npc == null || intent == null) return;
		Standing standing = standing(player, npc);
		if ((intents(player, npc, standing) & intent.bit()) == 0) {
			// Their standing changed since the wheel opened (suppression, a breakthrough): the wheel again, with what is on offer now.
			npc.beginTalk(player);
			ModPackets.sendTalkOpen(player, npc, npc.regard(player), intents(player, npc, standing));
			return;
		}
		npc.beginTalk(player);
		switch (intent) {
			case GREET -> greet(player, npc, standing);
			case ASK -> ModPackets.sendTalkDialogue(player, npc, npc.regard(player), intent, speech(lineFor("ask", npc, player, standing)),
					replies(player, npc, standing));
			case SPAR -> ModPackets.sendTalkDialogue(player, npc, npc.regard(player), intent, speech(line("spar.terms", npc)), sparTerms(player, npc));
			case TRADE -> {
				npc.openTrade(player);
				return; // the talk lasts as long as the trading screen
			}
			case GIFT -> gift(player, npc);
			case PROVOKE -> provoke(player, npc);
			case DEMAND -> demand(player, npc);
			case RELEASE -> release(player, npc);
			case SPARE -> spare(player, npc);
			case LEAVE -> { }
		}
		if (intent.isQuick()) npc.endTalk(player);
	}

	private static void greet(ServerPlayer player, CultivatorNpc npc, Standing standing) {
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		int delta = 0;
		if (entry.greetDay != today) {
			entry.greetDay = today;
			delta = Regard.GREET;
			npc.changeRegard(player, delta);
		}
		// You bow to a senior and cup your fist to an equal or a junior; they answer in kind.
		boolean senior = standing == Standing.FAR_ABOVE || standing == Standing.ABOVE || standing == Standing.MORTAL;
		boolean junior = standing == Standing.BELOW || standing == Standing.FAR_BELOW;
		ModPackets.sendGesture(player, senior ? Gesture.BOW : Gesture.CUP_FIST);
		ModPackets.sendGesture(npc, junior ? Gesture.BOW : Gesture.CUP_FIST);
		// A friend's greeting is a friend's, as warm as it is reverent; else by where they stand.
		Component words = npc.regard(player) >= Regard.WARM && NpcLines.has("talk.greet.liked." + NpcAddress.rung(player, npc).id(), npc)
				? NpcLines.line("talk.greet.liked." + NpcAddress.rung(player, npc).id(), npc, NpcAddress.address(player, npc), NpcAddress.name(player))
				: lineFor("greet", npc, player, standing);
		say(player, npc, words, delta);
		// A sworn friend presses something on you now and then.
		if (npc.regard(player) >= Regard.SWORN && today - entry.giftBackDay >= GIFT_BACK_DAYS && npc.getRandom().nextFloat() < GIFT_BACK_CHANCE) {
			entry.giftBackDay = today;
			giftBack(player, npc);
		}
	}

	/** What a friend gives: something of its own worth having (CultivatorLoot), or a few spirit stones. */
	private static void giftBack(ServerPlayer player, CultivatorNpc npc) {
		ItemStack gift = ItemStack.EMPTY;
		for (int i = 0; i < 4 && gift.isEmpty(); i++) {
			for (ItemStack stack : CultivatorLoot.roll(npc)) {
				if (worth(stack.copyWithCount(1)) > 0) {
					gift = stack.copyWithCount(1);
					break;
				}
			}
		}
		if (gift.isEmpty()) gift = CultivatorNpc.carriedStones(npc.getRank(), npc.getRandom()).stream().findFirst().orElse(ItemStack.EMPTY);
		if (gift.isEmpty()) return;
		player.sendSystemMessage(Component.translatable(ModLang.TALK_GIFT_BACK, nameOf(npc), gift.getHoverName()));
		player.getInventory().placeItemBackInInventory(gift.copy()); // what doesn't fit at their feet
	}

	/** What a gift is worth to a cultivator of the first realm (and what spirit stones it would fetch); a stronger one makes less of it. Zero: no use to them. */
	public static double worth(ItemStack stack) {
		if (stack.getItem() instanceof SpiritStoneItem stone) return stone.grade().value();
		if (stack.getItem() instanceof GinsengItem) {
			double w = Math.max(1, GinsengItem.age(stack) / 40.0);
			if (stack.is(ModItems.SPIRIT_GINSENG)) w *= 2;
			else if (stack.is(ModItems.PURPLE_LINGZHI) || stack.is(ModItems.OCHRE_HUANGJING)) w *= 1.5;
			else if (stack.is(ModItems.OTHER_SHORE_FLOWER)) w *= 1.5; // grown where only the dead walk
			else if (stack.is(ModItems.NETHERWORLD_LOTUS)) w *= 2.5;
			return w;
		}
		if (stack.getItem() instanceof PillItem) {
			double w = PillItem.grade(stack).getMultiplier() * 4;
			if (stack.is(ModItems.FOUNDATION_PILL)) w += 6;
			else if (stack.is(ModItems.CORE_PILL)) w += 10;
			else if (stack.is(ModItems.YANG_PRESERVATION_PILL) || stack.is(ModItems.YIN_GATHERING_PILL)) w += 4;
			return w;
		}
		if (stack.getItem() instanceof CultivationFruitItem) return Math.max(2, CultivationFruitItem.age(stack) / 30.0);
		// The Netherworld's: Yin Jade, a Ghost-Quelling Talisman, and a Ghost Jade Slip (a whole method); a heaven method's Jade Slip.
		if (stack.is(ModItems.YIN_JADE)) return 4;
		if (stack.is(ModItems.GHOST_QUELLING_TALISMAN)) return 5;
		if (stack.is(ModItems.GHOST_JADE_SLIP)) return CultivatorTrades.slipPrice(GhostJadeSlipItem.method(stack)) / 1.5;
		if (stack.is(ModItems.JADE_SLIP)) return CultivatorTrades.heavenSlipPrice(JadeSlipItem.method(stack)) / 1.5;
		return 0;
	}

	/**
	 * How much regard {@code worth} buys from a cultivator of {@code realm} (mortals: as the first realm), given as the
	 * {@code repeats}+1th of its kind today. The first of a kind each day always counts for something; the same again can come
	 * to nothing (the user's call, 2026-10-18: at +1 apiece, sixty Low Spirit Stones handed over one by one made anyone Warm).
	 */
	public static int giftRegard(double worth, int realm, int repeats) {
		double scaled = worth * 6.0 / (6.0 + 3.0 * Math.max(0, realm)) / (1 + repeats);
		return Mth.clamp((int) Math.round(scaled), repeats == 0 ? 1 : 0, Regard.GIFT_MAX);
	}

	private static void gift(ServerPlayer player, CultivatorNpc npc) {
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			player.displayClientMessage(Component.translatable(ModLang.TALK_GIFT_EMPTY), true);
			return;
		}
		double worth = worth(stack.copyWithCount(1));
		if (worth <= 0) {
			say(player, npc, line("gift.refuse", npc), 0);
			return;
		}
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		// Counted per item, so turning between two gifts doesn't make each fresh again.
		int repeats = entry.giftsToday(item, today);
		int realm = npc.isMortal() ? 0 : CultivatorNpc.realmOf(npc.getRank()).ordinal();
		int delta = giftRegard(worth, realm, repeats);
		if (delta <= 0) {
			// Worth nothing more to them today: they hand it back rather than take it for nothing.
			say(player, npc, line("gift.enough", npc), 0);
			return;
		}
		entry.recordGift(item, today);
		if (!player.getAbilities().instabuild) stack.shrink(1);
		npc.changeRegard(player, delta);
		Milestones.award(player, Milestones.GIFT);
		say(player, npc, line(delta <= 3 ? "gift.small" : delta <= 10 ? "gift.good" : "gift.great", npc), delta);
	}

	/** The chance an insult starts a fight: likelier on the devilish path, and from someone stronger than you; never from the weaker. */
	public static float provokeChance(Alignment.Faction faction, int theirRank, int yourRank) {
		if (theirRank <= yourRank - Stage.values().length) return 0; // a realm or more below you: they swallow it
		float chance = switch (faction) {
			case RIGHTEOUS -> 0.15f;
			case NEUTRAL -> 0.35f;
			case DEVILISH -> 0.6f;
		};
		if (theirRank > yourRank) chance += 0.3f;
		return Mth.clamp(chance, 0, 1);
	}

	private static void provoke(ServerPlayer player, CultivatorNpc npc) {
		npc.changeRegard(player, Regard.PROVOKE);
		int theirs = CultivatorNpc.rankOf(npc), yours = CultivatorNpc.rankOf(player);
		// An enemy of its path needs no second asking, unless it is a realm or more beneath you.
		float chance = isPathEnemy(player, npc) && theirs > yours - Stage.values().length ? 1 : provokeChance(npc.getFaction(), theirs, yours);
		boolean attack = npc.getRandom().nextFloat() < chance;
		say(player, npc, line(attack ? "provoke.attack" : "provoke.scorn", npc), Regard.PROVOKE);
		if (attack) {
			npc.endTalk(player);
			npc.provokedBy(player); // the player started it: killing them is still murder
			npc.setTarget(player);
		}
	}

	// --- Pressure and mercy ---

	/**
	 * Demand tribute from one kneeling under your pressure: something from what it (or its sect) keeps for one of its station,
	 * for {@link Regard#TRIBUTE} and 5 alignment toward the devilish. Once a day from each.
	 */
	private static void demand(ServerPlayer player, CultivatorNpc npc) {
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		if (entry.tributeDay == today) {
			say(player, npc, line("tribute.again", npc), 0);
			return;
		}
		entry.tributeDay = today;
		List<ItemStack> tribute = new ArrayList<>();
		for (ItemStack stack : CultivatorLoot.roll(npc)) {
			if (tribute.size() < 2 && !stack.isEmpty()) tribute.add(stack);
		}
		tribute.addAll(CultivatorNpc.carriedStones(npc.getRank(), npc.getRandom()));
		for (ItemStack stack : tribute) player.getInventory().placeItemBackInInventory(stack.copy()); // what doesn't fit at their feet
		npc.changeRegard(player, Regard.TRIBUTE);
		shiftAlignment(player, -5);
		say(player, npc, line("tribute", npc), Regard.TRIBUTE);
	}

	/** Lift your pressure off one kneeling under it: it gets up (RealmSuppressSystem spares it for a while). */
	private static void release(ServerPlayer player, CultivatorNpc npc) {
		RealmSuppressSystem.spare(player, npc);
		npc.setKneeling(false);
		say(player, npc, line("release", npc), 0);
	}

	/** Run down by someone two realms above it (a player), a cultivator that can't get away begs rather than fight. */
	public static boolean wouldBeg(CultivatorNpc npc, ServerPlayer player) {
		if (player.isCreative() || player.isSpectator() || npc.makesLastStand() || npc.isBegging() || npc.owesLife(player)) return false;
		return CultivatorNpc.rankOf(player) >= Math.max(0, npc.sustainedRank()) + 2 * Stage.values().length;
	}

	/** It throws itself down before {@code player} and begs for its life; right-clicking it answers (Spare or Leave). */
	public static void beginBegging(ServerPlayer player, CultivatorNpc npc) {
		npc.beg(player, CultivatorNpc.BEG_TICKS);
		player.displayClientMessage(Component.translatable(ModLang.TALK_BEGS, nameOf(npc)), true);
		npc.level().playSound(null, npc.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.NEUTRAL, 0.6f, 0.6f);
	}

	/** Spare one who begged: it owes you its life (CultivatorNpc#sparedBy), {@link Regard#SPARED}, and 2 alignment toward the righteous. */
	private static void spare(ServerPlayer player, CultivatorNpc npc) {
		npc.sparedBy(player);
		npc.changeRegard(player, Regard.SPARED);
		shiftAlignment(player, 2);
		say(player, npc, line("spared", npc), Regard.SPARED);
	}

	private static void shiftAlignment(ServerPlayer player, int shift) {
		PlayerCultivation c = CultivationManager.get(player);
		c.setAlignment(c.getAlignment() + shift);
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
	}

	// --- Asking ---

	/**
	 * The topics on offer: who they are, their sect and its people (members only; its people not to someone far beneath), the
	 * sects nearby, rumours (Courteous; not from those far above you) and guidance (Warm, from one a realm or more above you).
	 * A topic not open yet still shows, greyed, with what it needs.
	 */
	private static List<Reply> replies(ServerPlayer player, CultivatorNpc npc, Standing standing) {
		List<Reply> replies = new ArrayList<>();
		replies.add(topic(Topic.SELF));
		if (npc.isSectMember() && npc.sect() != null) {
			replies.add(topic(Topic.SECT));
			if (standing != Standing.FAR_ABOVE && standing != Standing.MORTAL) replies.add(topic(Topic.PEOPLE));
		}
		replies.add(topic(Topic.NEARBY));
		int regard = npc.regard(player);
		if (standing != Standing.FAR_ABOVE) replies.add(gated(Topic.RUMOURS, regard >= Regard.COURTEOUS, "courteous"));
		// A ghost who knows a method teaches it to those it is Warm to, senior or not.
		if (isSenior(player, npc) || teaches(npc)) replies.add(gated(Topic.GUIDANCE, regard >= Regard.WARM, "warm"));
		if (npc instanceof GhostCultivatorNpc ghost && ghost.getRole() == GhostCultivatorNpc.Role.MENG_PO) replies.add(topic(Topic.SOUP));
		// A sect's elders teach its element's heaven method, and a rogue its own, to the living they are Warm to (to their own
		// sect's disciples once Courteous); a Heavenly Venerable its element's True Immortal method to the Celestial Court's Captains and above.
		if (teachesHeaven(npc) && !GhostSystem.isGhost(player)) {
			if (npc.getTitle() == NpcTitle.HEAVENLY_VENERABLE) {
				CelestialCourt.Rank rank = CelestialCourt.rankOf(player.server, player.getUUID());
				boolean open = rank != null && rank.ordinal() >= CelestialCourt.Rank.CAPTAIN.ordinal();
				replies.add(new Reply(Topic.METHOD.ordinal(), Component.translatable(ModLang.talkTopic(Topic.METHOD.getId())), open,
						open ? null : Component.translatable(ModLang.TALK_NEEDS_COURT, CelestialCourt.Rank.CAPTAIN.displayName())));
			} else {
				boolean fellow = SectMembership.areFellows(npc, player);
				replies.add(gated(Topic.METHOD, regard >= (fellow ? Regard.COURTEOUS : Regard.WARM), fellow ? "courteous" : "warm"));
			}
		}
		// The Celestial Court, of one of its Heavenly Venerables: taken into it, or told where you stand in it.
		if (npc.getTitle() == NpcTitle.HEAVENLY_VENERABLE && CelestialCourt.isCourtier(npc) && !GhostSystem.isGhost(player)) {
			replies.add(topic(Topic.COURT));
			replies.add(topic(Topic.BEASTS));
		}
		// Joining the sect (or leaving it), and the trial an elder has set (SectMembership).
		if (npc.isSectMember() && npc.sect() != null && !GhostSystem.isGhost(player) && !npc.isGhost()) {
			if (SectMembership.areFellows(npc, player)) replies.add(topic(Topic.LEAVE_SECT));
			else if (SectMembership.sectOf(player.server, player.getUUID()) == null) {
				replies.add(topic(Topic.JOIN));
				if (trialSet(player, npc)) replies.add(topic(Topic.TRIAL));
			}
		}
		// A settler's wish (Wishes): said to anyone, granted by a Four Axis cultivator at that wish's axis (the dead by their ghost path).
		TalkSystem.Reply wish = Wishes.reply(player, npc, Topic.WISH.ordinal());
		if (wish != null) replies.add(wish);
		return replies;
	}

	/** Elders who set {@code player} the entrance trial, and until when (game time) the offer stands. */
	private static final java.util.Map<UUID, java.util.Map<UUID, Long>> TRIAL_SET = new java.util.HashMap<>();
	/** How long an elder's offer of the trial stands. */
	private static final long TRIAL_OFFER_TICKS = 5 * 60 * 20;

	/** Server stopped: the offers of the trial still standing go with it. */
	public static void clearTrials() {
		TRIAL_SET.clear();
	}

	private static boolean trialSet(ServerPlayer player, CultivatorNpc npc) {
		Long until = TRIAL_SET.getOrDefault(player.getUUID(), java.util.Map.of()).get(npc.getUUID());
		return until != null && npc.level().getGameTime() < until;
	}

	/**
	 * Asking to join: a disciple sends you to an elder, its barrier opening to you as the sect's guest (SectMembership#welcomeGuest),
	 * or tells you why the sect won't have you; an elder turns you away (SectMembership#refusal), takes you in at once (a gifted
	 * body, or one they are Warm to), or sets the entrance trial (TRIAL is offered next).
	 */
	private static Component askToJoin(ServerPlayer player, CultivatorNpc npc) {
		Sect sect = npc.sect();
		if (sect == null) return Component.translatable(ModLang.talkLine("join.failed"));
		Object[] arg = new Object[1];
		String refusal = SectMembership.refusal(player, npc, sect, arg);
		if (refusal != null) return arg[0] == null ? Component.translatable(ModLang.talkLine(refusal)) : Component.translatable(ModLang.talkLine(refusal), arg[0]);
		if (!npc.getTitle().isElderOrAbove()) {
			SectMembership.welcomeGuest(player, sect);
			return Component.translatable(ModLang.talkLine("join.disciple"));
		}
		if (SectMembership.excused(player, npc)) {
			boolean gifted = CultivationManager.get(player).getPhysique() != null
					&& CultivationManager.get(player).getPhysique().grade().ordinal() >= Physique.Grade.HIGH.ordinal();
			boolean kin = SectMembership.kin(player, sect);
			SectMembership.join(player, sect);
			return Component.translatable(ModLang.talkLine(kin ? "join.kin" : gifted ? "join.gifted" : "join.friend"), sect.name,
					SectMembership.titleOf(sect, player).getDisplayName());
		}
		if (SectMembership.triedToday(player, sect, npc.level().getGameTime())) return Component.translatable(ModLang.talkLine("join.tried"));
		java.util.Map<UUID, Long> offers = TRIAL_SET.computeIfAbsent(player.getUUID(), id -> new java.util.HashMap<>());
		long now = npc.level().getGameTime();
		offers.values().removeIf(until -> until <= now); // offers lapsed are forgotten (kept all the server's life before the sweep of 2026-10-24)
		offers.put(npc.getUUID(), now + TRIAL_OFFER_TICKS);
		return Component.translatable(ModLang.talkLine("join.trial"));
	}

	/** The entrance trial begins (a spar with the elder: SparSystem.Terms#TRIAL), or the line why it can't. */
	private static Component beginTrial(ServerPlayer player, CultivatorNpc npc) {
		String refusal = SparSystem.refusal(player, npc, SparSystem.Terms.TRIAL);
		if (refusal != null) return Component.translatable(refusal, nameOf(npc));
		TRIAL_SET.getOrDefault(player.getUUID(), new java.util.HashMap<>()).remove(npc.getUUID());
		SparSystem.start(player, npc, SparSystem.Terms.TRIAL);
		return null;
	}

	/** Leaving the sect: an elder releases you; a disciple can't. */
	private static Component leaveSect(ServerPlayer player, CultivatorNpc npc) {
		Sect sect = npc.sect();
		if (sect == null || !npc.getTitle().isElderOrAbove()) return Component.translatable(ModLang.talkLine("leave.disciple"));
		// In one of its people's bodies (Possession), it is the body that leaves (the user's ask, 2026-10-23): its master's too.
		if (Possession.inhabits(player.getUUID())) {
			SectMembership.Seat seat = SectMembership.seat(player);
			if (seat == null || seat.sect() != sect || !SectMembership.leaveAsBody(player)) return Component.translatable(ModLang.talkLine("leave.disciple"));
			return Component.translatable(ModLang.talkLine("leave.done"));
		}
		SectMembership.leave(player, sect);
		return Component.translatable(ModLang.talkLine("leave.done"));
	}

	/**
	 * A living cultivator who teaches its heaven method: a sect's elders and master, rogues, and the Celestial Court's Heavenly Venerables
	 * (CultivatorNpc#heavenMethod).
	 */
	private static boolean teachesHeaven(CultivatorNpc npc) {
		return !npc.isGhost() && npc.heavenMethod() != null
				&& (npc.getTitle().isElderOrAbove() || npc.getTitle() == NpcTitle.ROGUE || npc.getTitle() == NpcTitle.HEAVENLY_VENERABLE);
	}

	/** One of the dead who knows a ghost method it can teach (Foundation Building and above, GhostCultivatorNpc#method). */
	private static boolean teaches(CultivatorNpc npc) {
		return npc instanceof GhostCultivatorNpc ghost && ghost.method() != null;
	}

	/** A realm or more above the player (as it shows itself). */
	private static boolean isSenior(ServerPlayer player, CultivatorNpc npc) {
		PlayerCultivation c = CultivationManager.get(player);
		return !npc.isMortal() && (c.isMortal() || npc.getDisplayedRealm().ordinal() > TwoPaths.wieldedRealm(c).ordinal());
	}

	private static Reply topic(Topic topic) {
		return new Reply(topic.ordinal(), Component.translatable(ModLang.talkTopic(topic.getId())), true, null);
	}

	private static Reply gated(Topic topic, boolean open, String needs) {
		return new Reply(topic.ordinal(), Component.translatable(ModLang.talkTopic(topic.getId())), open,
				open ? null : Component.translatable(ModLang.TALK_NEEDS, Component.translatable(ModLang.talkRegardLevel(needs))));
	}

	/** The player picked a reply in the dialogue box: a topic under Ask, terms under Spar. */
	public static void topic(ServerPlayer player, int entityId, int replyId) {
		CultivatorNpc npc = partner(player, entityId);
		if (npc == null) return;
		// A wish's own choices (Wishes): grant it, the years taken.
		if (replyId >= Wishes.ACTIONS) {
			npc.beginTalk(player);
			Component said = Wishes.act(player, npc, replyId - Wishes.ACTIONS);
			List<Reply> replies = new ArrayList<>(replies(player, npc, standing(player, npc)));
			replies.addAll(Wishes.actions(player, npc));
			ModPackets.sendTalkDialogue(player, npc, npc.regard(player), TalkIntent.ASK, speech(said), replies);
			return;
		}
		if (replyId >= SPAR_TERMS) {
			sparOn(player, npc, SparSystem.Terms.byIndex(replyId - SPAR_TERMS));
			return;
		}
		Topic topic = Topic.byIndex(replyId);
		if (topic == null) return;
		Standing standing = standing(player, npc);
		List<Reply> replies = replies(player, npc, standing);
		npc.beginTalk(player);
		if (replies.stream().noneMatch(r -> r.id() == topic.ordinal() && r.enabled())) {
			// No longer on offer (their standing or regard changed): the box again, with the topics there are now.
			ModPackets.sendTalkDialogue(player, npc, npc.regard(player), TalkIntent.ASK, speech(line("ask", npc, standing)), replies);
			return;
		}
		boolean freely = standing == Standing.BELOW || standing == Standing.FAR_BELOW; // a junior answers freely, and says more
		Component answer = switch (topic) {
			case SELF -> aboutSelf(npc, freely);
			case SECT -> aboutSect(npc);
			case PEOPLE -> aboutPeople(npc, freely);
			case NEARBY -> aboutNearby(npc);
			case RUMOURS -> rumours(player, npc);
			case GUIDANCE -> npc instanceof GhostCultivatorNpc ghost && ghost.method() != null && ghost.method().taughtInTheWorld() ? teach(player, ghost) : guidance(player, npc);
			case SOUP -> {
				GhostGate.drinkSoup(player);
				yield Component.translatable(GhostSystem.isGhost(player) ? ModLang.TALK_SOUP : ModLang.TALK_SOUP_LIVING);
			}
			case METHOD -> teachHeaven(player, npc);
			case JOIN -> askToJoin(player, npc);
			case TRIAL -> beginTrial(player, npc);
			case LEAVE_SECT -> leaveSect(player, npc);
			case WISH -> Wishes.tell(player, npc);
			case COURT -> CelestialCourt.ask(player, npc);
			case BEASTS -> primordialBeasts(npc);
		};
		if (answer == null) return; // the trial began: the talk is over (SparSystem#start closed it)
		List<Reply> after = new ArrayList<>(replies(player, npc, standing));
		if (topic == Topic.WISH) after.addAll(Wishes.actions(player, npc));
		ModPackets.sendTalkDialogue(player, npc, npc.regard(player), TalkIntent.ASK, speech(answer), after);
	}

	/** A Heavenly Venerable on the primordial beasts: where each keeps, from the Southern Heavenly Gate, or when the next comes (PrimordialBeasts). */
	private static Component primordialBeasts(CultivatorNpc npc) {
		net.minecraft.network.chat.MutableComponent said = Component.translatable(ModLang.talkLine("primordial"));
		if (npc.level() instanceof ServerLevel level && ModDimensions.isImmortalRealm(level.dimension())) {
			for (Component line : PrimordialBeasts.directions(level)) said.append(" ").append(line);
		}
		return said;
	}

	private static Component aboutSelf(CultivatorNpc npc, boolean freely) {
		Component name = Component.literal(npc.getPersonalName());
		if (npc.isMortal()) return Component.translatable(ModLang.talkLine("self.commoner"), name);
		// One of the Celestial Court's: its station there, a Venerable's element, its realm as it is named (CultivatorNpc#rankLabel).
		if (CelestialCourt.isCourtier(npc)) {
			Component title = npc.getTitle().getDisplayName();
			return npc.getTitle() == NpcTitle.HEAVENLY_VENERABLE && npc.courtElement() != null
					? Component.translatable(ModLang.talkLine("self.court.venerable"), name, npc.courtElement().getDisplayName(), npc.rankLabel())
					: Component.translatable(ModLang.talkLine("self.court"), name, title, npc.rankLabel());
		}
		Component realm = PlayerCultivation.rankName(npc.getDisplayedRealm(), npc.getDisplayedStage());
		Sect sect = npc.sect();
		String path = npc.getFaction().getId();
		Component who = sect != null
				? Component.translatable(ModLang.talkLine("self.sect", path), name, npc.getTitle().getDisplayName(sect.isClan(), sect.isHoly(), sect.isImmortal()), sect.name, realm)
				: Component.translatable(ModLang.talkLine("self.rogue", path), name, realm);
		MutableComponent answer = Component.empty().append(who).append(" ")
				.append(Component.translatable(ModLang.talkLine("self.years"), npc.yearsCultivated()));
		// To a senior, a junior says how far along its stage it is.
		if (freely) answer.append(" ").append(Component.translatable(ModLang.talkLine("self.progress"), npc.stageProgressPercent()));
		// One of the dead says how it died (always the same: picked from who it is).
		if (npc instanceof GhostCultivatorNpc ghost) answer.append(" ").append(Component.translatable(ModLang.npcSay("death." + ghost.deathKey() + ".1")));
		return answer;
	}

	/** The ghost path's cultivation a teacher's guidance gives one who knows its method already: ten minutes' worth (GhostMethods' seconds). */
	public static final double TEACHING_PROGRESS = 10 * 60;

	/**
	 * A ghost teaches its method (once a day, to one it is Warm to): the method to one who doesn't know it; to one who does, its
	 * layers still settling in settled at once, and ten minutes' worth ({@link #TEACHING_PROGRESS}) on the ghost path's bar.
	 */
	static Component teach(ServerPlayer player, GhostCultivatorNpc ghost) {
		GhostMethod method = ghost.method();
		Regard.Entry entry = ghost.regardEntry(player);
		long today = Regard.day(ghost.level().getGameTime());
		if (entry.guidanceDay == today) return line("guidance.again", ghost);
		NetherworldState s = CultivationManager.get(player).netherworld();
		// A mortal's closed meridians: it says so, and the day's guidance isn't spent.
		Component refusal = GhostMethods.refusal(CultivationManager.get(player), method);
		if (refusal != null) return refusal;
		entry.guidanceDay = today;
		if (!s.knows(method)) {
			GhostMethods.learn(player, method);
			return Component.translatable(ModLang.TALK_TEACH_LEARN, method.getDisplayName());
		}
		int ghostPathBefore = GhostMethods.ghostRank(s);
		int upTo = GhostMethods.understoodTo(s, method);
		if (s.layer(method) < upTo) s.setMethod(method, upTo, 0);
		GhostMethods.addCultivation(s, TEACHING_PROGRESS);
		GhostMethods.afterLayer(player, s);
		GhostMethods.afterRank(player, ghostPathBefore);
		CultivationManager.markDirty(player.server);
		Spells.sync(player);
		NetherworldManager.refresh(player);
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 0.7f);
		return Component.translatable(ModLang.TALK_TEACH_PROGRESS, method.getDisplayName(), (int) (TEACHING_PROGRESS / 60));
	}

	/**
	 * A living cultivator teaches its heaven method (once a day, sharing the day's guidance): the method to one who doesn't know
	 * it; to one who does, its layers still settling in, settled at once, and ten minutes of their own meditation's worth of
	 * cultivation ({@link HeavenMethods#teachingProgress}). A mortal or one of the dead is told why not, and the day isn't spent.
	 */
	static Component teachHeaven(ServerPlayer player, CultivatorNpc npc) {
		HeavenMethod method = npc.heavenMethod();
		// A True Immortal's method the worlds below don't teach (Realm#taughtInTheWorld): only guidance there. The Immortal Realm's immortals
		// teach theirs (phase 2: "immortals who teach or sell them").
		if (method == null || !method.realm().taughtInTheWorld() && !ModDimensions.isImmortalRealm(npc.level().dimension())) return guidance(player, npc);
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		if (entry.guidanceDay == today) return line("guidance.again", npc);
		PlayerCultivation c = CultivationManager.get(player);
		Component refusal = HeavenMethods.refusal(c, method);
		if (refusal != null) return refusal;
		entry.guidanceDay = today;
		HeavenState s = c.heaven();
		if (!s.knows(method)) {
			HeavenMethods.learn(player, method);
			return Component.translatable(ModLang.TALK_TEACH_HEAVEN, method.getDisplayName(), method.element().getDisplayName());
		}
		int upTo = HeavenMethods.understoodTo(c, method);
		if (s.layer(method) < upTo) s.setMethod(method, upTo, 0);
		boolean advanced = c.addCultivation(HeavenMethods.teachingProgress(c));
		CultivationManager.refresh(player);
		if (advanced) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_STAGE_UP, c.getRealm().getDisplayName(), c.getStage().getDisplayName()), true);
			player.setHealth(player.getMaxHealth());
		}
		HeavenMethods.afterLayer(player);
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.0f);
		return Component.translatable(ModLang.TALK_TEACH_HEAVEN_PROGRESS, method.getDisplayName(), (int) (HeavenMethods.TEACHING_SECONDS / 60));
	}

	private static Component aboutSect(CultivatorNpc npc) {
		Sect sect = npc.sect();
		if (sect == null) return aboutSelf(npc, false);
		Component path = Component.translatable(ModLang.talkPath(Alignment.factionOf(sect.alignment).getId()));
		Sect.Member master = sect.master();
		int count = sect.members.size();
		Component about;
		if (master == null) about = Component.translatable(ModLang.talkLine("sect.no_master"), sect.name, count, path);
		else if (master.id.equals(npc.getUUID())) about = Component.translatable(ModLang.talkLine("sect.self_master"), sect.name, count, path);
		else about = Component.translatable(ModLang.talkLine("sect"), sect.name, count, path, NpcNames.of(master.id), rankName(master.rank));
		// And the enemies of its path nearest to it.
		Sect enemy = nearestSect(npc, other -> Alignment.factionOf(sect.alignment).isEnemyOf(Alignment.factionOf(other.alignment)));
		if (enemy == null) return about;
		Component direction = Component.translatable(ModLang.talkDirection(direction(npc.blockPosition(), enemy.core)));
		return Component.empty().append(about).append(" ").append(Component.translatable(ModLang.talkLine("sect.enemy"), enemy.name, direction));
	}

	private static Component aboutPeople(CultivatorNpc npc, boolean freely) {
		Sect sect = npc.sect();
		if (sect == null) return aboutSelf(npc, false);
		List<Component> elders = new ArrayList<>();
		Sect.Member master = null;
		for (Sect.Member member : sect.members.values()) {
			if (member.title == NpcTitle.SECT_MASTER) master = member;
			if ((member.title == NpcTitle.GRAND_ELDER || member.title == NpcTitle.ELDER) && !member.id.equals(npc.getUUID())) {
				Component named = Component.translatable(ModLang.TALK_TITLED, member.title.getDisplayName(), NpcNames.of(member.id));
				// A junior also says how strong each is.
				elders.add(freely ? Component.translatable(ModLang.TALK_AT_REALM, named, rankName(member.rank)) : named);
			}
		}
		Component answer;
		boolean iAmMaster = master != null && master.id.equals(npc.getUUID());
		if (iAmMaster) {
			answer = elders.isEmpty() ? Component.translatable(ModLang.talkLine("people.alone"))
					: Component.translatable(ModLang.talkLine("people.master_self"), list(elders));
		} else if (master == null) {
			answer = elders.isEmpty() ? Component.translatable(ModLang.talkLine("people.alone")) : Component.translatable(ModLang.talkLine("people.no_master"), list(elders));
		} else {
			Component masterName = Component.literal(NpcNames.of(master.id));
			answer = elders.isEmpty() ? Component.translatable(ModLang.talkLine("people.no_elders"), masterName)
					: Component.translatable(ModLang.talkLine("people"), list(elders), masterName);
		}
		// Who keeps the gates (Sect#guardPost: the gate guards on duty, those loaded).
		List<Component> guards = new ArrayList<>();
		if (npc.level() instanceof ServerLevel level) {
			for (CultivatorNpc member : level.getEntitiesOfClass(CultivatorNpc.class, new AABB(sect.core).inflate(sect.territoryRadius()),
					other -> other.isGateGuard() && sect.id.equals(other.getSectId()))) {
				if (guards.size() < 4) guards.add(Component.literal(member.getPersonalName()));
			}
		}
		if (guards.isEmpty()) return answer;
		return Component.empty().append(answer).append(" ").append(Component.translatable(ModLang.talkLine("people.gates"), list(guards)));
	}

	/** The nearest other living sect they would know of: its name, path, which way and how far. */
	private static Component aboutNearby(CultivatorNpc npc) {
		Sect best = nearestSect(npc, sect -> true);
		if (best == null) return Component.translatable(ModLang.talkLine("nearby.none"));
		Alignment.Faction faction = Alignment.factionOf(best.alignment);
		double bestDistance = npc.blockPosition().distSqr(best.core);
		int distance = Math.max(100, (int) Math.round(Math.sqrt(bestDistance) / 100.0) * 100);
		Component direction = Component.translatable(ModLang.talkDirection(direction(npc.blockPosition(), best.core)));
		Component path = Component.translatable(ModLang.talkPath(faction.getId()));
		String kind = npc.getFaction().isEnemyOf(faction) ? "nearby.enemy" : "nearby";
		return Component.translatable(ModLang.talkLine(kind), best.name, path, direction, distance);
	}

	/** The nearest living sect other than its own within {@link #KNOWN_SECTS}, among those {@code which} accepts. */
	private static Sect nearestSect(CultivatorNpc npc, java.util.function.Predicate<Sect> which) {
		if (!(npc.level() instanceof ServerLevel level)) return null;
		Sect best = null;
		double bestDistance = KNOWN_SECTS * KNOWN_SECTS;
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (sect.extinct || sect.dimension != level.dimension() || sect.id.equals(npc.getSectId()) || !which.test(sect)) continue;
			double d = npc.blockPosition().distSqr(sect.core);
			if (d < bestDistance) {
				best = sect;
				bestDistance = d;
			}
		}
		return best;
	}

	/** One or two things it has heard of and not yet told this player (Rumours), each noted in the player's journal. */
	private static Component rumours(ServerPlayer player, CultivatorNpc npc) {
		Regard.Entry entry = npc.regardEntry(player);
		List<Rumours.Rumour> untold = Rumours.untold(npc, entry.toldRumours, 2);
		if (untold.isEmpty()) return line("rumour.none", npc);
		MutableComponent said = Component.empty();
		for (int i = 0; i < untold.size(); i++) {
			Rumours.Rumour rumour = untold.get(i);
			Component told = Rumours.tell(rumour, npc);
			entry.toldRumours.add(rumour.id);
			Journal.add(player, rumour.id, npc, told);
			if (i > 0) said.append(" ");
			said.append(told);
		}
		player.displayClientMessage(Component.translatable(ModLang.TALK_JOURNAL_NOTED), true);
		return said;
	}

	/** A senior's guidance: their Insight (ModEffects#INSIGHT), once a day from each teacher. */
	private static Component guidance(ServerPlayer player, CultivatorNpc npc) {
		return grantInsight(player, npc) ? line("guidance", npc) : line("guidance.again", npc);
	}

	/** Grants {@code npc}'s Insight to {@code player} if it hasn't today; true if it did. */
	public static boolean grantInsight(ServerPlayer player, CultivatorNpc npc) {
		Regard.Entry entry = npc.regardEntry(player);
		long today = Regard.day(npc.level().getGameTime());
		if (entry.guidanceDay == today) return false;
		entry.guidanceDay = today;
		player.addEffect(new MobEffectInstance(ModEffects.INSIGHT, ModEffects.INSIGHT_TICKS, 0, false, true, true));
		player.sendSystemMessage(Component.translatable(ModLang.TALK_INSIGHT, nameOf(npc), (int) Math.round(ModEffects.INSIGHT_BONUS * 100),
				ModEffects.INSIGHT_TICKS / 1200));
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.2f);
		return true;
	}

	// --- Sparring ---

	/**
	 * The terms of a spar on offer: friendly, a wager (holding a herb or pill; never with one a realm or more beneath you, see
	 * SparSystem#beneath), and, with a stronger cultivator, at full strength.
	 */
	private static List<Reply> sparTerms(ServerPlayer player, CultivatorNpc npc) {
		List<Reply> terms = new ArrayList<>();
		boolean stake = !SparSystem.stakeInHand(player).isEmpty();
		boolean stronger = npc.sustainedRank() > CultivatorNpc.rankOf(player);
		terms.add(term(SparSystem.Terms.FRIENDLY, true, null));
		if (SparSystem.beneath(player, npc)) terms.add(term(SparSystem.Terms.WAGER, false, Component.translatable(ModLang.SPAR_FRIENDLY_ONLY)));
		else terms.add(term(SparSystem.Terms.WAGER, stake, stake ? null : Component.translatable(ModLang.SPAR_HOLD_STAKE)));
		if (stronger) {
			terms.add(term(SparSystem.Terms.FULL, true, null));
			terms.add(term(SparSystem.Terms.WAGER_FULL, stake, stake ? null : Component.translatable(ModLang.SPAR_HOLD_STAKE)));
		}
		return terms;
	}

	private static Reply term(SparSystem.Terms terms, boolean enabled, Component note) {
		return new Reply(SPAR_TERMS + terms.ordinal(), Component.translatable(ModLang.sparTerms(terms.name().toLowerCase(java.util.Locale.ROOT))), enabled, note);
	}

	private static void sparOn(ServerPlayer player, CultivatorNpc npc, SparSystem.Terms terms) {
		// The entrance trial is set by an elder (askToJoin), never picked from the spar's terms.
		if (terms == null || terms == SparSystem.Terms.TRIAL || (intents(player, npc, standing(player, npc)) & TalkIntent.SPAR.bit()) == 0) return;
		String refusal = SparSystem.refusal(player, npc, terms);
		if (refusal != null) {
			npc.beginTalk(player);
			ModPackets.sendTalkDialogue(player, npc, npc.regard(player), TalkIntent.SPAR,
					speech(Component.translatable(refusal, nameOf(npc))), sparTerms(player, npc));
			return;
		}
		SparSystem.start(player, npc, terms);
	}

	// --- Words ---

	/** The eight-point compass direction from {@code from} to {@code to}. */
	public static String direction(BlockPos from, BlockPos to) {
		double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
		// Minecraft's north is -z; angle 0 = north, turning clockwise through east.
		double angle = Math.toDegrees(Math.atan2(dx, -dz));
		int sector = Math.floorMod((int) Math.round(angle / 45.0), 8);
		return new String[] {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"}[sector];
	}

	private static Component rankName(int rank) {
		return rank < 0 ? Component.translatable(ModLang.TALK_MORTAL) : PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
	}

	/** "A, B and C". */
	private static Component list(List<Component> parts) {
		if (parts.size() == 1) return parts.get(0);
		MutableComponent head = Component.empty();
		for (int i = 0; i < parts.size() - 1; i++) {
			if (i > 0) head.append(Component.translatable(ModLang.TALK_LIST));
			head.append(parts.get(i));
		}
		return Component.translatable(ModLang.TALK_AND, head, parts.get(parts.size() - 1));
	}

	/** The key of a line of {@code kind} for this cultivator at this standing: a mortal's own, else by path and standing. */
	static Component line(String kind, CultivatorNpc npc, Standing standing) {
		if (npc.isMortal()) return Component.translatable(ModLang.talkLine(kind + ".commoner" + (standing == Standing.PEER ? ".mortal" : "")));
		return Component.translatable(ModLang.talkLine(kind, npc.getFaction().getId(), standing.getId()));
	}

	/**
	 * {@code kind}'s line for {@code player} (a greeting, an answer to their asking): one of its variants (NpcLines) by the rung they stand on
	 * before {@code npc} (NpcAddress, from a junior's to an Ancestor's) in its voice, calling them as it does ({@code %1$s}) and naming them
	 * ({@code %2$s}: their title, from Void Shattering on, or their name); the old one by standing where no such line is written.
	 */
	static Component lineFor(String kind, CultivatorNpc npc, ServerPlayer player, Standing standing) {
		String situation = "talk." + kind + "." + NpcAddress.rung(player, npc).id();
		if (NpcLines.has(situation, npc)) return NpcLines.line(situation, npc, NpcAddress.address(player, npc), NpcAddress.name(player));
		return line(kind, npc, standing);
	}

	/** A line by path alone (gifts, insults, spars, mercy); a mortal speaks for itself. */
	static Component line(String kind, CultivatorNpc npc) {
		if (npc.isMortal()) return Component.translatable(ModLang.talkLine(kind + ".commoner"));
		return Component.translatable(ModLang.talkLine(kind, npc.getFaction().getId()));
	}

	/** Their words, in quotes. */
	private static Component speech(Component words) {
		return Component.literal("“").append(words).append("”");
	}

	/** Says a line above the hotbar: "Mo Lian: "..."", with what it did to their regard. */
	static void say(ServerPlayer player, CultivatorNpc npc, Component words, int regardDelta) {
		player.displayClientMessage(withDelta(Component.translatable(ModLang.TALK_SAYS, nameOf(npc), speech(words)), regardDelta), true);
	}

	private static MutableComponent withDelta(Component message, int regardDelta) {
		MutableComponent out = message.copy();
		if (regardDelta != 0) {
			String signed = (regardDelta > 0 ? "+" : "") + regardDelta;
			out.append(Component.translatable(ModLang.TALK_REGARD_DELTA, signed)
					.withStyle(regardDelta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		return out;
	}

	/** Their name, in their title's colour. */
	public static MutableComponent nameOf(CultivatorNpc npc) {
		return Component.literal(npc.getPersonalName()).withStyle(style -> style.withColor(npc.getTitle().getColor()));
	}

	private TalkSystem() {}
}
