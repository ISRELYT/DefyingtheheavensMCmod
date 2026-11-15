package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * A personal disciple (the user's "Build as designed", 2026-11-13). One at a time (PlayerCultivation#personalDisciple), taken through talk
 * ({@link #take}: TalkSystem.Topic#TAKE_DISCIPLE): a cultivator of one's own sect, or a wanderer who admires one (regard {@link #ADMIRE} or
 * more), weaker than one, of none's yet and not an elder. Taken (CultivatorNpc#master), it:
 * <ul>
 *   <li>follows its master as a bound beast does ({@link FollowGoal}: within {@link #FOLLOW}, at once from past {@link #CATCH_UP}) and fights
 *   for them with its own spells ({@link DefendGoal}: whoever hurts them or whom they strike); told in talk to Follow, Stay where it is, or
 *   Guard this spot (TalkSystem.Topic#DISCIPLE: {@link #actions}, {@link #act}); its master away, it waits where it is in meditation;</li>
 *   <li>cultivates the method its master picks of those they know (CultivatorNpc#heavenMethod) and the war arts they teach it (Sword Intent,
 *   Blood Battle: WarArts, as its own blows'); within {@link #NEAR} of them {@link #FASTER} as fast, walking or not
 *   ({@link #cultivationFactor}); it makes the Lists and may outgrow its master;</li>
 *   <li>at a realm's gate asks its master for a pill ({@link #holdsAtGate}: the breakthrough's own, else a Cultivation Pill) and waits for it
 *   a day at most, given it (Gift: {@link #givePill}) it breaks through at once;</li>
 *   <li>a righteous one who sees its master do the devilish ({@link #witnessed}: within {@link #WITNESS_REACH}, an act that darkens their
 *   path) thinks less of them, and below {@link #LEAVES_BELOW} leaves them;</li>
 *   <li>dead ({@link #died}), it is mourned, and its master takes no other for {@link #MOURN_TICKS} (a week).</li>
 * </ul>
 */
public final class PersonalDisciples {
	/** What its master tells it. */
	public enum Order { FOLLOW, STAY, GUARD }

	/** Its instructions' reply ids in the dialogue box start here (TalkSystem). */
	public static final int ACTIONS = 300;
	static final int A_FOLLOW = 0, A_STAY = 1, A_GUARD = 2, A_RELEASE = 3, A_METHOD = 10, A_TEACH = 60;
	/** It keeps within this of its master, and comes to them at once from past {@link #CATCH_UP}. */
	public static final double FOLLOW = 5, CATCH_UP = 32;
	/** Within this of its master it cultivates {@link #FASTER} as fast, walking or not. */
	public static final double NEAR = 32, FASTER = 1.5;
	/** Guarding a spot, it keeps within this of it and takes on what comes there. */
	public static final double GUARD_REACH = 10;
	/** A wanderer follows one it thinks this well of (Regard#WARM). */
	public static final int ADMIRE = Regard.WARM;
	/** A righteous disciple who sees its master's path darken thinks this much less of them; below {@link #LEAVES_BELOW} it leaves. */
	public static final int WITNESSED = -20, LEAVES_BELOW = 20;
	public static final double WITNESS_REACH = 32;
	/** Its death mourned this long (a week) before its master takes another; a pill waited for this long (a day) at most. */
	public static final long MOURN_TICKS = 7 * 24_000L, PILL_WAIT = 24_000L;

	// --- Whose ---

	public static boolean isDisciple(CultivatorNpc npc) {
		return npc.master() != null;
	}

	/** Whether {@code npc} is {@code player}'s disciple. */
	public static boolean isDiscipleOf(CultivatorNpc npc, ServerPlayer player) {
		return player.getUUID().equals(npc.master());
	}

	/** Its master, online in its world, or null. */
	public static @Nullable ServerPlayer masterOf(CultivatorNpc npc) {
		if (npc.master() == null || !(npc.level() instanceof ServerLevel level)) return null;
		ServerPlayer master = level.getServer().getPlayerList().getPlayer(npc.master());
		return master != null && master.level() == npc.level() && master.isAlive() ? master : null;
	}

	/** {@code player}'s disciple where it is loaded, or null. */
	public static @Nullable CultivatorNpc discipleOf(ServerPlayer player) {
		UUID id = CultivationManager.own(player).personalDisciple();
		if (id == null) return null;
		for (ServerLevel level : player.server.getAllLevels()) {
			if (level.getEntity(id) instanceof CultivatorNpc npc && npc.isAlive()) return npc;
		}
		return null;
	}

	// --- Taking one ---

	/** Whether {@code player} might ask {@code npc} to be their disciple (TalkSystem: the topic shown, greyed with why if it won't). */
	static boolean offered(ServerPlayer player, CultivatorNpc npc) {
		return !npc.isGhost() && !npc.isMortal() && npc.livesItsLife() && !isDiscipleOf(npc, player) && !GhostSystem.isGhost(player)
				&& CultivationManager.own(player).personalDisciple() == null;
	}

	/** Why {@code npc} won't be {@code player}'s disciple now (a translation key), or null. */
	static @Nullable Component refusal(ServerPlayer player, CultivatorNpc npc) {
		PlayerCultivation own = CultivationManager.own(player);
		long now = player.level().getGameTime();
		if (own.personalDisciple() != null) return Component.translatable(ModLang.DISCIPLE_HAVE_ONE);
		if (now < own.discipleMournUntil()) return Component.translatable(ModLang.DISCIPLE_MOURNING, (own.discipleMournUntil() - now + 23_999) / 24_000);
		if (npc.master() != null) return Component.translatable(ModLang.DISCIPLE_TAKEN, npc.getDisplayName());
		if (npc.getRank() >= TwoPaths.wieldedRank(CultivationManager.get(player))) return Component.translatable(ModLang.DISCIPLE_TOO_STRONG, npc.getDisplayName());
		if (npc.getTitle().isElderOrAbove()) return Component.translatable(ModLang.DISCIPLE_ELDER, npc.getDisplayName());
		if (npc.isSectMember()) {
			if (!SectMembership.areFellows(npc, player) && !isFoundersSect(player, npc)) return Component.translatable(ModLang.DISCIPLE_OTHER_SECT, npc.getDisplayName());
			return null;
		}
		if (npc.getLifecycle() != CultivatorNpc.Lifecycle.ROGUE || npc.keepsPost() || npc.settledAt() != null) {
			return Component.translatable(ModLang.DISCIPLE_OWN_ROAD, npc.getDisplayName());
		}
		if (npc.regard(player) < ADMIRE) return Component.translatable(ModLang.DISCIPLE_NOT_ADMIRED, npc.getDisplayName());
		return null;
	}

	/** Whether {@code npc}'s sect is the one {@code player} founded (PlayerSects). */
	private static boolean isFoundersSect(ServerPlayer player, CultivatorNpc npc) {
		Sect sect = npc.sect();
		return sect != null && !sect.extinct && player.getUUID().equals(sect.founder);
	}

	/** {@code player} asks {@code npc} to be their disciple (TalkSystem.Topic#TAKE_DISCIPLE): taken, or why not. @return what it says */
	static Component take(ServerPlayer player, CultivatorNpc npc) {
		Component why = refusal(player, npc);
		if (why != null) return why;
		bind(player, npc);
		return Component.translatable(ModLang.DISCIPLE_TAKEN_SAY, player.getDisplayName());
	}

	/** {@code npc} {@code player}'s disciple, following them. */
	static void bind(ServerPlayer player, CultivatorNpc npc) {
		npc.setMaster(player.getUUID());
		npc.setDiscipleOrder(Order.FOLLOW, null);
		npc.setMeditating(false);
		npc.setTarget(null);
		npc.getNavigation().stop();
		npc.setPersistenceRequired();
		NpcLife.end(npc);
		PlayerCultivation own = CultivationManager.own(player);
		own.setPersonalDisciple(npc.getUUID());
		CultivationManager.markDirty(player.server);
		if (npc.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + npc.getBbHeight(), npc.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
			level.playSound(null, npc.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.6f);
		}
		ModPackets.sendGesture(npc, Gesture.BOW);
		player.sendSystemMessage(Component.translatable(ModLang.DISCIPLE_TAKEN_MSG, npc.getDisplayName()).withStyle(ChatFormatting.GOLD));
	}

	/** The bond between {@code npc} and its master undone (let go, gone its own way, dead): both sides forgotten. */
	static void unbind(ServerLevel level, CultivatorNpc npc) {
		UUID master = npc.master();
		npc.setMaster(null);
		npc.setDiscipleOrder(Order.FOLLOW, null);
		npc.setMeditating(false);
		npc.resetPill();
		if (master == null) return;
		PlayerCultivation own = CultivationManager.own(level.getServer(), master);
		if (npc.getUUID().equals(own.personalDisciple())) own.setPersonalDisciple(null);
		CultivationManager.markDirty(level.getServer());
	}

	// --- Its master's instructions (TalkSystem.Topic#DISCIPLE) ---

	/** What its master may tell it: follow, stay, guard here, go its own way; which of their methods to cultivate; a war art to learn. */
	static List<TalkSystem.Reply> actions(ServerPlayer player, CultivatorNpc npc) {
		List<TalkSystem.Reply> replies = new ArrayList<>();
		if (!isDiscipleOf(npc, player)) return replies;
		Order order = npc.discipleOrder();
		replies.add(new TalkSystem.Reply(ACTIONS + A_FOLLOW, Component.translatable(ModLang.DISCIPLE_ORDER_FOLLOW), order != Order.FOLLOW, null));
		replies.add(new TalkSystem.Reply(ACTIONS + A_STAY, Component.translatable(ModLang.DISCIPLE_ORDER_STAY), order != Order.STAY, null));
		replies.add(new TalkSystem.Reply(ACTIONS + A_GUARD, Component.translatable(ModLang.DISCIPLE_ORDER_GUARD), true, null));
		List<HeavenMethod> methods = teachable(player, npc);
		for (int i = 0; i < methods.size() && i < A_TEACH - A_METHOD; i++) {
			HeavenMethod method = methods.get(i);
			boolean current = method == npc.heavenMethod();
			replies.add(new TalkSystem.Reply(ACTIONS + A_METHOD + i, Component.translatable(ModLang.DISCIPLE_METHOD, method.getDisplayName()), !current,
					current ? Component.translatable(ModLang.DISCIPLE_METHOD_CURRENT) : null));
		}
		for (Technique art : new Technique[] {Technique.SWORD_INTENT, Technique.BLOOD_BATTLE}) {
			if (!CultivationManager.own(player).techniques().contains(art)) continue;
			boolean known = npc.cultivation().techniques().contains(art);
			boolean ready = CultivatorNpc.realmOf(npc.getRank()).ordinal() >= art.realm().ordinal();
			replies.add(new TalkSystem.Reply(ACTIONS + A_TEACH + art.ordinal(), Component.translatable(ModLang.DISCIPLE_TEACH, art.getDisplayName()), !known && ready,
					known ? Component.translatable(ModLang.DISCIPLE_TEACH_KNOWN) : ready ? null : Component.translatable(ModLang.DISCIPLE_TEACH_LOW, art.realm().getDisplayName())));
		}
		replies.add(new TalkSystem.Reply(ACTIONS + A_RELEASE, Component.translatable(ModLang.DISCIPLE_ORDER_RELEASE), true, null));
		return replies;
	}

	/** The heaven methods {@code player} knows that {@code npc} may cultivate: its master's, in a steady order. */
	static List<HeavenMethod> teachable(ServerPlayer player, CultivatorNpc npc) {
		List<HeavenMethod> methods = new ArrayList<>(CultivationManager.own(player).heaven().known());
		methods.sort(Comparator.comparing((HeavenMethod m) -> m.element().ordinal()).thenComparing(m -> m.realm().ordinal()));
		return npc.isBodyCultivator() ? List.of() : methods;
	}

	/** {@code player} tells their disciple {@code action} (one of {@link #actions}). @return what it says */
	static Component act(ServerPlayer player, CultivatorNpc npc, int action) {
		if (!isDiscipleOf(npc, player)) return Component.translatable(ModLang.DISCIPLE_NOT_YOURS);
		switch (action) {
			case A_FOLLOW -> {
				npc.setDiscipleOrder(Order.FOLLOW, null);
				npc.setMeditating(false);
				return Component.translatable(ModLang.DISCIPLE_SAY_FOLLOW);
			}
			case A_STAY -> {
				npc.setDiscipleOrder(Order.STAY, npc.blockPosition());
				return Component.translatable(ModLang.DISCIPLE_SAY_STAY);
			}
			case A_GUARD -> {
				npc.setDiscipleOrder(Order.GUARD, player.blockPosition());
				npc.setMeditating(false);
				return Component.translatable(ModLang.DISCIPLE_SAY_GUARD);
			}
			case A_RELEASE -> {
				if (npc.level() instanceof ServerLevel level) unbind(level, npc);
				player.sendSystemMessage(Component.translatable(ModLang.DISCIPLE_RELEASED_MSG, npc.getDisplayName()).withStyle(ChatFormatting.GRAY));
				return Component.translatable(ModLang.DISCIPLE_SAY_RELEASED);
			}
			default -> {
			}
		}
		if (action >= A_METHOD && action < A_TEACH) {
			List<HeavenMethod> methods = teachable(player, npc);
			int i = action - A_METHOD;
			if (i >= methods.size()) return Component.translatable(ModLang.DISCIPLE_SAY_PUZZLED);
			npc.setTaughtMethod(methods.get(i));
			return Component.translatable(ModLang.DISCIPLE_SAY_METHOD, methods.get(i).getDisplayName());
		}
		Technique[] arts = Technique.values();
		int t = action - A_TEACH;
		if (t >= 0 && t < arts.length && (arts[t] == Technique.SWORD_INTENT || arts[t] == Technique.BLOOD_BATTLE)
				&& CultivationManager.own(player).techniques().contains(arts[t])
				&& CultivatorNpc.realmOf(npc.getRank()).ordinal() >= arts[t].realm().ordinal()) {
			npc.cultivation().learnTechnique(arts[t]);
			return Component.translatable(ModLang.DISCIPLE_SAY_TAUGHT, arts[t].getDisplayName());
		}
		return Component.translatable(ModLang.DISCIPLE_SAY_PUZZLED);
	}

	// --- Its life beside its master ---

	/** How much faster {@code npc} cultivates now: {@link #FASTER} within {@link #NEAR} of its master (walking or not), else as ever (1). */
	public static double cultivationFactor(CultivatorNpc npc) {
		ServerPlayer master = masterOf(npc);
		return master != null && master.distanceToSqr(npc) <= NEAR * NEAR ? FASTER : 1;
	}

	/** The pill {@code npc} asks its master for at {@code next}'s gate: that breakthrough's own (PillItem), else a Cultivation Pill. */
	static Item pillFor(Realm next) {
		Item pill = PillItem.breakthroughPillFor(next);
		return pill != null ? pill : ModItems.CULTIVATION_PILL;
	}

	/**
	 * {@code npc}, a disciple, has reached {@code next}'s gate (CultivatorNpc#advance): it asks its master for its pill (once, told to them if
	 * near), and waits for it a day at most. Returns whether it holds at the gate now.
	 */
	public static boolean holdsAtGate(CultivatorNpc npc, Realm next) {
		if (!isDisciple(npc) || next == null || !(npc.level() instanceof ServerLevel level)) return false;
		long now = level.getGameTime();
		if (npc.pillGate() != next.ordinal()) {
			npc.askPill(next.ordinal(), now);
			ServerPlayer master = masterOf(npc);
			if (master != null) {
				master.sendSystemMessage(Component.translatable(ModLang.DISCIPLE_ASKS_PILL, npc.getDisplayName(), next.getDisplayName(),
						pillFor(next).getDescription()).withStyle(ChatFormatting.AQUA));
			}
			return true;
		}
		if (npc.pillGiven()) return false;
		return now - npc.pillAskedAt() < PILL_WAIT;
	}

	/**
	 * {@code player} gives {@code npc} {@code stack} (TalkSystem's Gift): their disciple waiting at a gate for this pill takes it, and breaks
	 * through at once. Returns whether it was that.
	 */
	static boolean givePill(ServerPlayer player, CultivatorNpc npc, ItemStack stack) {
		if (!isDiscipleOf(npc, player) || npc.pillGate() < 0 || npc.pillGiven()) return false;
		Realm next = Realm.byIndex(npc.pillGate());
		if (next == null || !stack.is(pillFor(next))) return false;
		if (!player.getAbilities().instabuild) stack.shrink(1);
		npc.givePill();
		npc.advance(0); // through the gate now
		player.sendSystemMessage(Component.translatable(ModLang.DISCIPLE_PILL_TAKEN, npc.getDisplayName(), next.getDisplayName()).withStyle(ChatFormatting.GOLD));
		return true;
	}

	/**
	 * {@code player} did something that darkened their path by {@code shift} (a killing, a body bled, a soul devoured): their disciple near, of
	 * the righteous, thinks the less of them for it, and too little now, leaves them.
	 */
	public static void witnessed(ServerPlayer player, int shift) {
		if (shift >= 0) return;
		CultivatorNpc npc = discipleOf(player);
		if (npc == null || npc.level() != player.level() || npc.distanceToSqr(player) > WITNESS_REACH * WITNESS_REACH) return;
		if (npc.getFaction() != Alignment.Faction.RIGHTEOUS) return;
		npc.changeRegard(player, WITNESSED);
		if (npc.regard(player) >= LEAVES_BELOW) {
			player.sendSystemMessage(Component.translatable(ModLang.DISCIPLE_DISMAYED, npc.getDisplayName()).withStyle(ChatFormatting.RED));
			return;
		}
		if (npc.level() instanceof ServerLevel level) unbind(level, npc);
		player.sendSystemMessage(Component.translatable(ModLang.DISCIPLE_LEAVES, npc.getDisplayName()).withStyle(ChatFormatting.RED));
	}

	/** {@code npc}, a disciple, died (CultivatorNpc#die): mourned, and its master takes no other for a week. */
	public static void died(ServerLevel level, CultivatorNpc npc) {
		UUID masterId = npc.master();
		if (masterId == null) return;
		PlayerCultivation own = CultivationManager.own(level.getServer(), masterId);
		if (!npc.getUUID().equals(own.personalDisciple())) return;
		own.setPersonalDisciple(null);
		own.setDiscipleMournUntil(level.getGameTime() + MOURN_TICKS);
		CultivationManager.markDirty(level.getServer());
		ServerPlayer master = level.getServer().getPlayerList().getPlayer(masterId);
		if (master != null) master.sendSystemMessage(Component.translatable(ModLang.DISCIPLE_DIED, npc.getDisplayName()).withStyle(ChatFormatting.RED));
	}

	/** Whether {@code other} is on {@code npc}'s side as its master's (its master, their beast, their other bound ones). */
	public static boolean sidesWith(CultivatorNpc npc, LivingEntity other) {
		UUID master = npc.master();
		if (master == null || other == null) return false;
		return master.equals(other.getUUID()) || other instanceof OwnableEntity owned && master.equals(owned.getOwnerUUID());
	}

	// --- Following ---

	/**
	 * A disciple keeps by its master (above its own life's goals, below its fights): following within {@link #FOLLOW}, at once from past
	 * {@link #CATCH_UP}; staying where it was told, meditating there; guarding its spot within {@link #GUARD_REACH}. Its master away (offline,
	 * another world), it waits where it is in meditation.
	 */
	static final class FollowGoal extends Goal {
		private final CultivatorNpc npc;

		FollowGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			return isDisciple(npc) && npc.isAlive() && npc.getTarget() == null && npc.talkPartner() == null && !npc.isVacant();
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void stop() {
			npc.getNavigation().stop();
		}

		@Override
		public void tick() {
			ServerPlayer master = masterOf(npc);
			switch (npc.discipleOrder()) {
				case STAY -> keepTo(npc.discipleSpot(), 1.5, true);
				case GUARD -> keepTo(npc.discipleSpot(), GUARD_REACH, false);
				default -> {
					if (master == null) {
						npc.getNavigation().stop();
						if (!npc.isMeditating() && npc.onGround()) npc.setMeditating(true); // waits for them
						return;
					}
					if (npc.isMeditating()) npc.setMeditating(false);
					npc.getLookControl().setLookAt(master, 10, npc.getMaxHeadXRot());
					double d = npc.distanceToSqr(master);
					if (d > CATCH_UP * CATCH_UP) {
						npc.moveTo(master.getX() + npc.getRandom().nextInt(5) - 2, master.getY(), master.getZ() + npc.getRandom().nextInt(5) - 2, npc.getYRot(), 0);
						npc.getNavigation().stop();
					} else if (d > FOLLOW * FOLLOW) {
						if (npc.getNavigation().isDone() || npc.tickCount % 10 == 0) npc.getNavigation().moveTo(master, 1.15);
					} else {
						npc.getNavigation().stop();
					}
				}
			}
		}

		/** Back within {@code reach} of {@code spot}; there, still (meditating if {@code sit}). */
		private void keepTo(BlockPos spot, double reach, boolean sit) {
			if (spot == null) spot = npc.blockPosition();
			Vec3 at = Vec3.atBottomCenterOf(spot);
			if (npc.position().distanceToSqr(at) > reach * reach) {
				if (npc.isMeditating()) npc.setMeditating(false);
				if (npc.getNavigation().isDone() || npc.tickCount % 20 == 0) npc.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
				return;
			}
			npc.getNavigation().stop();
			if (sit && !npc.isMeditating() && npc.onGround()) npc.setMeditating(true);
		}
	}

	/** A disciple following its master takes on whoever hurts them or whom they strike; guarding a spot, the monsters that come there. */
	static final class DefendGoal extends TargetGoal {
		private LivingEntity foe;

		DefendGoal(CultivatorNpc npc) {
			super(npc, false);
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (!(mob instanceof CultivatorNpc npc) || !isDisciple(npc)) return false;
			foe = null;
			if (npc.discipleOrder() == Order.GUARD) {
				BlockPos spot = npc.discipleSpot();
				if (spot == null) return false;
				for (LivingEntity e : npc.level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(spot).inflate(GUARD_REACH),
						e -> e instanceof Enemy && e.isAlive() && npc.canAttack(e) && !sidesWith(npc, e))) {
					foe = e;
					break;
				}
				return foe != null;
			}
			ServerPlayer master = masterOf(npc);
			if (master == null || npc.discipleOrder() != Order.FOLLOW) return false;
			LivingEntity attacker = master.getLastHurtByMob(), struck = master.getLastHurtMob();
			foe = fit(npc, attacker) ? attacker : fit(npc, struck) ? struck : null;
			return foe != null;
		}

		private static boolean fit(CultivatorNpc npc, LivingEntity other) {
			return other != null && other != npc && other.isAlive() && npc.canAttack(other) && !sidesWith(npc, other) && other.distanceToSqr(npc) <= CATCH_UP * CATCH_UP;
		}

		@Override
		public void start() {
			mob.setTarget(foe);
			super.start();
		}
	}

	private PersonalDisciples() {}
}
