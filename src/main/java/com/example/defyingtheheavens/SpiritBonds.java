package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Spirit Beast Bond (CultivationMethod#SPIRIT_BEAST_BOND; the methods overhaul's phase 3): one whose heaven path practises it subdues a wild
 * demon beast (brought to {@link #SUBDUED} of its health or less, of no higher rank than itself, no elder nor primordial) and binds it with
 * an empty hand ({@link #interact}): one at a time (PlayerCultivation#spiritBeast). Bound, the beast follows it ({@link FollowGoal}), fights
 * whoever hurts it or whom it strikes ({@link DefendGoal}), never turns on it and never fades away (DemonBeast#bindTo). They grow together:
 * <ul>
 *   <li>a demon core fed to it is {@link #SHARE} of its owner's stage, twice as much for each realm the core stands above its owner's and
 *   half for each below, at most {@link #MOST} (CultivationMethods#grant); a core of its own rank or above lifts the beast a stage, as far
 *   as its owner's rank (past its kind's highest: DemonBeast#setBoundlessRank);</li>
 *   <li>each cultivator or beast it slays is {@link #KILL_SHARE} of the stage by the slain's realm likewise, at most {@link #KILL_MOST}.</li>
 * </ul>
 * If it dies, the realm's progress it carried goes with it: its owner's heaven path is re-laid at the realm's Early, as a switch is
 * (CultivationMethods#restart), its owner online or not.
 * <p>
 * <b>Ridden</b> (the user's pick of 2026-11-10, "Build as designed"): its owner's empty hand on it puts them on its back ({@link #mount}), if it
 * is wide enough to bear them at its size now ({@link #MOUNT_WIDTH}: it grows a twelfth larger a stage, so the small kinds bear a rider once
 * grown); sneaking gets them off. Ridden, it goes where its rider steers it (DemonBeast#travel: its kind's pace on the ground, Space a hop; a
 * swimmer along the water's top; a flier where its rider looks, Space a climb, but only where its rider could fly: {@link #groundedFor}), and
 * fights nothing: its mind is quiet until its rider gets off (SoulMobMixin).
 */
public final class SpiritBonds {
	public static final float SUBDUED = 0.25f;
	public static final double SHARE = 0.15, MOST = 0.6, KILL_SHARE = 0.05, KILL_MOST = 0.3;
	/** It keeps within this of its owner, and comes to it (in a stride) from further than {@link #CATCH_UP}. */
	public static final double FOLLOW = 6, CATCH_UP = 24;
	/** A beast this wide (blocks, at its size now: DemonBeast#scale) or wider bears its owner on its back. */
	public static final double MOUNT_WIDTH = 0.9;
	/**
	 * A flier's pace with its rider on its back, blocks a tick at full flight, for each of its kind's speed (a roc's 0.3: 0.6 a tick, twelve
	 * blocks a second, about Qi Flight's own), and this much swifter for each stage it has grown above its kind's least.
	 */
	public static final double FLIGHT_PACE = 2.0, PACE_PER_STAGE = 0.02;
	/** A rider taken this far off its back by a teleport (a rift, a step through space, a talisman's flash) is off it, not dragged back. */
	public static final double TORN_AWAY = 16;

	/** Whether {@code c}'s heaven path gathers by its bond now: practising it, holding its volume. */
	static boolean bound(PlayerCultivation c) {
		return CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) == CultivationMethod.SPIRIT_BEAST_BOND && CultivationMethods.holdsVolume(c, TwoPaths.Path.HEAVEN);
	}

	/** {@code player}'s hand on {@code beast} (DemonBeast#mobInteract): a core fed to its own beast, its own beast mounted, or a subdued beast bound. */
	static InteractionResult interact(DemonBeast beast, ServerPlayer player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.getUUID().equals(beast.getOwnerUUID())) {
			if (held.isEmpty() && hand == InteractionHand.MAIN_HAND) return mount(beast, player);
			if (!held.is(ModItems.DEMON_CORE)) return InteractionResult.PASS;
			feed(beast, player, held);
			return InteractionResult.SUCCESS;
		}
		PlayerCultivation c = CultivationManager.get(player);
		if (beast.getOwnerUUID() != null || !held.isEmpty() || CultivationMethods.practised(c, TwoPaths.Path.HEAVEN) != CultivationMethod.SPIRIT_BEAST_BOND) {
			return InteractionResult.PASS;
		}
		Component why = refusal(beast, player, c);
		if (why != null) {
			player.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		bind(beast, player);
		return InteractionResult.SUCCESS;
	}

	/** Why {@code player} can't bind {@code beast} now, or null. */
	static Component refusal(DemonBeast beast, ServerPlayer player, PlayerCultivation c) {
		BeastKind kind = beast.kind();
		if (kind.isElder() || kind.isPrimordial() || kind.isChaos()) return Component.translatable(ModLang.MSG_BOND_TOO_GREAT, beast.getDisplayName());
		if (beast.getRank() > PlayerCultivation.rank(c.getRealm(), c.getStage())) return Component.translatable(ModLang.MSG_BOND_TOO_STRONG, beast.getDisplayName());
		if (beast.getHealth() > beast.getMaxHealth() * SUBDUED) return Component.translatable(ModLang.MSG_BOND_NOT_SUBDUED, beast.getDisplayName());
		DemonBeast mine = beastOf(player.serverLevel(), CultivationManager.own(player).spiritBeast());
		if (mine != null && mine.isAlive()) return Component.translatable(ModLang.MSG_BOND_ALREADY, mine.getDisplayName());
		return null;
	}

	/**
	 * {@code player} on its own beast's back (an empty hand), if it is big enough to bear them: its fight left off, its mind quiet while they
	 * ride (SoulMobMixin). Sneaking (vanilla's) gets them off. Sneaking as they reach for it, nothing.
	 */
	static InteractionResult mount(DemonBeast beast, ServerPlayer player) {
		if (player.isSecondaryUseActive()) return InteractionResult.PASS;
		Component why = mountRefusal(beast, player);
		if (why != null) {
			player.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		beast.calm();
		beast.setTarget(null);
		beast.getNavigation().stop();
		return player.startRiding(beast) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
	}

	/** Why {@code player} can't get on its own {@code beast} now, or null: too small yet; or the rider a soul or one of the dead, or the beast another soul's. */
	static Component mountRefusal(DemonBeast beast, ServerPlayer player) {
		if (!beast.bearsARider()) return Component.translatable(ModLang.MSG_MOUNT_TOO_SMALL, beast.getDisplayName());
		if (Incorporeal.is(player) || SoulProjection.isSoul(player) || SoulProjection.isPossessed(beast) || beast.isVehicle()) {
			return Component.translatable(ModLang.MSG_MOUNT_CANT, beast.getDisplayName());
		}
		return null;
	}

	/**
	 * Whether a flier can't take to the air with {@code rider} on its back where they are: wherever the rider couldn't fly either. Under the Weak
	 * Water or a Killing Formation's weight (QiFlight#weighedDown); on the ways between the worlds that carry their travellers (QiFlight#carried);
	 * and in a secret realm, unless its rider flies freely there (its sky's hold: a beast keeps to the ground rather than to the hold's height).
	 */
	static boolean groundedFor(ServerPlayer rider) {
		if (QiFlight.weighedDown(rider)) return true;
		PlayerCultivation c = CultivationManager.get(rider);
		if (QiFlight.carried(rider.level().dimension(), c)) return true;
		if (ModDimensions.isSecretRealm(rider.level().dimension())) {
			SecretRealm realm = SecretRealm.at(rider.getX(), rider.getZ());
			return realm == null || !realm.fliesFreely(TwoPaths.higherRank(c, false));
		}
		return false;
	}

	/** A flier's pace with its rider (blocks a tick at full flight): {@link #FLIGHT_PACE} its kind's speed, swifter the more it has grown. */
	public static double flightPace(DemonBeast beast) {
		int above = Math.max(0, beast.getRank() - beast.kind().minRank());
		return beast.kind().speed * FLIGHT_PACE * (1 + PACE_PER_STAGE * above);
	}

	/** {@code player}'s bound beast in {@code level} by its id, or null if it isn't there. */
	static DemonBeast beastOf(ServerLevel level, UUID id) {
		return id != null && level.getEntity(id) instanceof DemonBeast beast ? beast : null;
	}

	/** {@code beast} bound to {@code player}: its fight left off, whole again. */
	static void bind(DemonBeast beast, ServerPlayer player) {
		beast.bindTo(player.getUUID());
		beast.setTarget(null);
		beast.setLastHurtByMob(null);
		beast.setHealth(beast.getMaxHealth());
		CultivationManager.own(player).setSpiritBeast(beast.getUUID());
		CultivationManager.markDirty(player.server);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.HEART, beast.getX(), beast.getY() + beast.getBbHeight(), beast.getZ(), 8, 0.4, 0.3, 0.4, 0.02);
		level.playSound(null, beast.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_BOND_BOUND, beast.getDisplayName()).withStyle(ChatFormatting.GOLD));
	}

	/** A core fed to {@code beast} by its owner: it grows by it, and its owner with it (practising the bond). */
	static void feed(DemonBeast beast, ServerPlayer player, ItemStack core) {
		PlayerCultivation c = CultivationManager.get(player);
		int coreRank = DemonCoreItem.rank(core);
		if (!player.getAbilities().instabuild) core.shrink(1);
		int ownerRank = PlayerCultivation.rank(c.getRealm(), c.getStage());
		if (coreRank >= beast.getRank() && beast.getRank() < ownerRank) grow(beast, beast.getRank() + 1);
		ServerLevel level = player.serverLevel();
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, beast.getX(), beast.getY() + beast.getBbHeight(), beast.getZ(), 10, 0.4, 0.3, 0.4, 0.02);
		level.playSound(null, beast.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0f, 0.7f);
		if (bound(c)) CultivationMethods.grant(player, TwoPaths.Path.HEAVEN, CultivationMethod.SPIRIT_BEAST_BOND, share(DemonPath.realm(coreRank).ordinal(), c.getRealm().ordinal()));
		else player.displayClientMessage(Component.translatable(ModLang.MSG_BOND_FED, beast.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
	}

	/** {@code beast} grown to {@code rank} (past its kind's highest it grows boundless). */
	static void grow(DemonBeast beast, int rank) {
		if (rank > beast.kind().maxRank()) beast.setBoundlessRank(rank);
		else beast.setRank(rank);
	}

	/** The share of its owner's stage a core of {@code core}'s realm is to an owner standing in {@code owner} (ordinals). */
	public static double share(int core, int owner) {
		return Math.min(MOST, SHARE * Math.pow(2, core - owner));
	}

	/** A slain foe's share likewise (by the slain's realm). */
	public static double killShare(int slain, int owner) {
		return Math.min(KILL_MOST, KILL_SHARE * Math.pow(2, slain - owner));
	}

	/** {@code beast}, bound, slew {@code killed} (DemonBeast#killedEntity): its owner, practising the bond, shares in it. */
	static void killed(DemonBeast beast, LivingEntity killed) {
		if (!(killed instanceof CultivatorEntity slain) || !(beast.level() instanceof ServerLevel level) || beast.getOwnerUUID() == null) return;
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(beast.getOwnerUUID());
		if (owner == null) return;
		PlayerCultivation c = CultivationManager.get(owner);
		if (!bound(c)) return;
		CultivationMethods.grant(owner, TwoPaths.Path.HEAVEN, CultivationMethod.SPIRIT_BEAST_BOND,
				killShare(slain.getCultivationRealm().ordinal(), c.getRealm().ordinal()));
	}

	/** {@code beast}, bound, died (DemonBeast#die): the realm's progress it carried goes with it. */
	static void died(ServerLevel level, DemonBeast beast) {
		UUID ownerId = beast.getOwnerUUID();
		PlayerCultivation c = CultivationManager.own(level.getServer(), ownerId);
		if (!beast.getUUID().equals(c.spiritBeast())) return;
		c.setSpiritBeast(null);
		boolean lost = CultivationMethods.restart(c, TwoPaths.Path.HEAVEN);
		CultivationManager.markDirty(level.getServer());
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null) return;
		CultivationManager.refresh(owner);
		owner.sendSystemMessage(Component.translatable(lost ? ModLang.MSG_BOND_DIED_LOST : ModLang.MSG_BOND_DIED, beast.getDisplayName(),
				PlayerCultivation.rankName(c.getRealm(), Stage.EARLY)).withStyle(ChatFormatting.RED));
	}

	/** A bound beast keeps by its owner: within {@link #FOLLOW}, coming at once from past {@link #CATCH_UP}. Its fights come first. */
	static final class FollowGoal extends Goal {
		private final DemonBeast beast;

		FollowGoal(DemonBeast beast) {
			this.beast = beast;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		private LivingEntity owner() {
			return beast.getOwnerUUID() == null ? null : beast.getOwner();
		}

		@Override
		public boolean canUse() {
			LivingEntity owner = owner();
			return owner != null && beast.getTarget() == null && owner.level() == beast.level() && beast.distanceToSqr(owner) > FOLLOW * FOLLOW;
		}

		@Override
		public boolean canContinueToUse() {
			LivingEntity owner = owner();
			return owner != null && beast.getTarget() == null && owner.level() == beast.level() && beast.distanceToSqr(owner) > 9;
		}

		@Override
		public void tick() {
			LivingEntity owner = owner();
			if (owner == null) return;
			beast.getLookControl().setLookAt(owner, 10, beast.getMaxHeadXRot());
			if (beast.distanceToSqr(owner) > CATCH_UP * CATCH_UP) {
				beast.moveTo(owner.getX() + beast.getRandom().nextInt(5) - 2, owner.getY(), owner.getZ() + beast.getRandom().nextInt(5) - 2, beast.getYRot(), 0);
				beast.getNavigation().stop();
			} else {
				beast.getNavigation().moveTo(owner, 1.15);
			}
		}

		@Override
		public void stop() {
			beast.getNavigation().stop();
		}
	}

	/** A bound beast takes on whoever hurt its owner, or whom its owner struck (lately: vanilla keeps both a few seconds). */
	static final class DefendGoal extends TargetGoal {
		private LivingEntity foe;

		DefendGoal(DemonBeast beast) {
			super(beast, false);
			setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (!(mob instanceof DemonBeast beast) || beast.getOwnerUUID() == null) return false;
			LivingEntity owner = beast.getOwner();
			if (owner == null) return false;
			LivingEntity attacker = owner.getLastHurtByMob(), struck = owner.getLastHurtMob();
			foe = fit(beast, attacker) ? attacker : fit(beast, struck) ? struck : null;
			return foe != null;
		}

		private static boolean fit(DemonBeast beast, LivingEntity other) {
			return other != null && other != beast && other.isAlive() && beast.canAttack(other)
					&& !(other instanceof OwnableEntity pet && beast.getOwnerUUID().equals(pet.getOwnerUUID()));
		}

		@Override
		public void start() {
			mob.setTarget(foe);
			super.start();
		}
	}

	private SpiritBonds() {}
}
