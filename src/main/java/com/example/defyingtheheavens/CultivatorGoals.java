package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * What NPC cultivators do ({@link CultivatorNpc}), highest priority first: flee, fight, travel, keep seclusion, go back to
 * the grounds, do their sect duty, wander.
 * <ul>
 *   <li><b>Sect Master</b>: meditates on the raised dais of the main hall.</li>
 *   <li><b>Grand Elders and Elders</b>: sit in meditation at their posts by the treasury and the Formation Core.</li>
 *   <li><b>Inner Disciples</b>: meditate on the mats of the meditation hall, now and then walking the inner courtyards.</li>
 *   <li><b>Outer Disciples</b>: patrol a ring outside the barrier (up to its edge plus 26), the sect's front line, resting at
 *   home between rounds.</li>
 *   <li><b>Rogues</b>: wander, and sit to meditate when idle.</li>
 * </ul>
 * Below Heavenly Being, sect members never leave their territory except in a fight or running from one, and go straight
 * back to their duty when it ends. Before a cultivator a realm above them, disciples and rogues run, while the Sect
 * Master and its elders stand and fight for their sect. Disciples and rogues attack enemies of their path on sight (players included, by alignment) and Outer and
 * Inner Disciples clear monsters from the grounds; Elders and the Sect Master only answer their sect's call (SectManager).
 */
public final class CultivatorGoals {
	/** Ticks between two strikes: 20 for a mortal, quicker with each realm, 10 at the quickest. */
	static int attackCooldown(CultivatorNpc npc) {
		return npc.isMortal() ? 20 : Math.max(10, 20 - 2 * CultivatorNpc.realmOf(npc.getRank()).ordinal());
	}

	/** Steps a walker toward a far target, a little at a time (paths are only planned so far). */
	static void walkToward(CultivatorNpc npc, Vec3 target, double speed) {
		Vec3 to = target.subtract(npc.position());
		double distance = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 step = distance > 24 ? npc.position().add(to.x / distance * 24, 0, to.z / distance * 24) : target;
		npc.getNavigation().moveTo(step.x, step.y, step.z, speed);
	}

	/** Seats it on the mat (or patch of ground) at {@code pos}: in the middle, facing as it was, meditating. */
	static void sit(CultivatorNpc npc, BlockPos pos) {
		npc.getNavigation().stop();
		double y = pos.getY() + (npc.level().getBlockState(pos).getBlock() instanceof MeditationMatBlock ? 2.0 / 16 : 0);
		if (npc.position().distanceToSqr(pos.getX() + 0.5, y, pos.getZ() + 0.5) > 0.0025) {
			npc.moveTo(pos.getX() + 0.5, y, pos.getZ() + 0.5, npc.getYRot(), 0);
		}
		npc.setMeditating(true);
	}

	/** Someone (a player, or another cultivator) already sits on the mat at {@code pos}. */
	static boolean occupied(CultivatorNpc npc, BlockPos pos) {
		Vec3 centre = Vec3.atBottomCenterOf(pos);
		for (Entity entity : npc.level().getEntities(npc, new net.minecraft.world.phys.AABB(pos).inflate(0.2), e -> e instanceof LivingEntity)) {
			if (entity.position().distanceToSqr(centre) < 0.5) return true;
		}
		return false;
	}

	/** Can it get to the target at all? A flier or someone in reach can; a walker below a flier can't. */
	static boolean unreachableOnFoot(CultivatorNpc npc, LivingEntity target) {
		boolean airborne = !target.onGround() && !target.isInWater()
				&& (target instanceof Player player && player.getAbilities().flying || target.getY() - npc.getY() > 3.0);
		return airborne || target.getY() - npc.getY() > 4.0;
	}

	// --- Movement goals ---

	/**
	 * Badly hurt, or a hostile domain far above its own nearby: it gets away from whatever it fears, on foot or through the
	 * air. From a stronger domain it runs all the way to a refuge some {@link CultivatorNpc#FLEE_DISTANCE} blocks off (sect
	 * members out of their grounds too: the core is no shelter from someone already inside), then comes back
	 * (ReturnToTerritoryGoal); badly hurt, it only backs away. Caught (struck by its pursuer at close quarters), it strikes back.
	 */
	public static class FleeGoal extends Goal {
		/** Struck this recently by the one it runs from, with that one in reach: it's cornered. */
		private static final int CORNERED_TICKS = 40;
		/** Within this many blocks (sideways) of its refuge, it is there. */
		private static final double REFUGE_REACHED = 8;
		private final CultivatorNpc npc;
		private int recheck;
		private int cooldown;

		public FleeGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override public boolean canUse() { return npc.shouldFlee(); }
		@Override public boolean canContinueToUse() { return npc.shouldFlee(); }

		@Override
		public void start() {
			npc.setMeditating(false);
			npc.setSuppressing(false);
			recheck = 0;
		}

		@Override public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void tick() {
			Entity threat = npc.getFleeFrom() != null ? npc.getFleeFrom() : npc.getTarget() != null ? npc.getTarget() : npc.getLastHurtByMob();
			if (threat == null) return;
			if (cooldown > 0) cooldown--;
			if (threat instanceof LivingEntity pursuer && cornered(pursuer)) {
				npc.getLookControl().setLookAt(pursuer, 30.0f, 30.0f);
				if (cooldown <= 0 && npc.getSensing().hasLineOfSight(pursuer)) {
					npc.swing(InteractionHand.MAIN_HAND);
					npc.doHurtTarget(pursuer);
					cooldown = attackCooldown(npc);
				}
			}
			BlockPos refuge = npc.getRefuge();
			if (refuge != null) {
				run(refuge);
				return;
			}
			Vec3 away = npc.position().subtract(threat.position());
			away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
			if (npc.canFlySustainably() || npc.isQiFlying()) {
				npc.setQiFlying(true);
				// Up off the ground and away, no higher than a dozen blocks over whatever is below.
				int ground = Formations.surface(npc.level(), Heightmap.Types.MOTION_BLOCKING, npc.getBlockX(), npc.getBlockZ());
				npc.flyToward(npc.position().add(away.scale(12)).add(0, npc.getY() < ground + 12 ? 4 : 0, 0), 0.5);
			} else if (--recheck <= 0 || npc.getNavigation().isDone()) {
				recheck = 20;
				Vec3 spot = DefaultRandomPos.getPosAway(npc, 16, 7, threat.position());
				if (spot != null) npc.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.4);
			}
		}

		/** All the way to its refuge (see CultivatorNpc#startFleeing), through the air if it can sustain flight. */
		private void run(BlockPos refuge) {
			double dx = refuge.getX() + 0.5 - npc.getX(), dz = refuge.getZ() + 0.5 - npc.getZ();
			if (dx * dx + dz * dz < REFUGE_REACHED * REFUGE_REACHED) {
				if (npc.isQiFlying()) npc.land();
				npc.getNavigation().stop();
				npc.reachRefuge();
				return;
			}
			if (npc.tickCount % 40 == 0 && npc.level() instanceof ServerLevel level && !level.isPositionEntityTicking(refuge)) npc.redrawRefuge(level);
			if (!npc.isQiFlying() && npc.canFlySustainably()) npc.setQiFlying(true);
			if (npc.isQiFlying()) {
				int ground = Formations.surface(npc.level(), Heightmap.Types.MOTION_BLOCKING, npc.getBlockX(), npc.getBlockZ());
				npc.flyToward(new Vec3(refuge.getX() + 0.5, Math.max(ground + 10, npc.getY()), refuge.getZ() + 0.5), 0.5);
			} else if (--recheck <= 0 || npc.getNavigation().isDone()) {
				recheck = 20;
				walkToward(npc, Vec3.atBottomCenterOf(refuge), 1.4);
			}
		}

		/** Its pursuer has just struck it and is close enough to strike back at. */
		private boolean cornered(LivingEntity pursuer) {
			return npc.getLastHurtByMob() == pursuer && npc.tickCount - npc.getLastHurtByMobTimestamp() < CORNERED_TICKS
					&& pursuer.isAlive() && !npc.isAlly(pursuer) && npc.isWithinMeleeAttackRange(pursuer)
					&& !(pursuer instanceof Player player && (player.isCreative() || player.isSpectator()));
		}
	}

	/** Closes with its target and strikes; flies after fliers and presses weaker foes down, while its qi can bear the cost. */
	public static class CombatGoal extends Goal {
		private final CultivatorNpc npc;
		private int cooldown;
		private int repath;

		public CombatGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		private boolean valid(LivingEntity target) {
			if (target == null || !target.isAlive() || target.isRemoved() || target.level() != npc.level()) return false;
			if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
			if (npc.isAlly(target) || npc.shouldFlee()) return false;
			// A leashed member chases only so far past its territory.
			if (npc.isLeashed()) {
				Sect sect = npc.sect();
				if (sect != null) {
					double dx = target.getX() - sect.core.getX(), dz = target.getZ() - sect.core.getZ();
					double limit = sect.territoryRadius() + 48;
					if (dx * dx + dz * dz > limit * limit) return false;
				}
			}
			return npc.position().distanceToSqr(target.position()) < 96 * 96;
		}

		@Override
		public boolean canUse() {
			return valid(npc.getTarget());
		}

		@Override
		public boolean canContinueToUse() {
			boolean keep = valid(npc.getTarget());
			if (!keep && npc.getTarget() != null && !npc.shouldFlee()) npc.setTarget(null);
			return keep;
		}

		@Override
		public void start() {
			npc.setMeditating(false);
			repath = 0;
		}

		@Override
		public void stop() {
			npc.setSuppressing(false);
			npc.getNavigation().stop();
			if (npc.isQiFlying()) npc.land();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = npc.getTarget();
			if (target == null) return;
			npc.getLookControl().setLookAt(target, 30.0f, 30.0f);
			double distanceSqr = npc.distanceToSqr(target);

			// Into the air after a flier, while the qi lasts; back down once the fight is on the ground again.
			boolean airborne = unreachableOnFoot(npc, target);
			if (airborne && !npc.isQiFlying() && npc.canFlySustainably()) npc.setQiFlying(true);
			if (npc.isQiFlying()) {
				if (!airborne && target.onGround() && distanceSqr < 25) {
					npc.land();
				} else {
					npc.flyToward(target.position().add(0, target.getBbHeight() * 0.3, 0), 0.45);
				}
			} else if (--repath <= 0) {
				repath = distanceSqr > 256 ? 10 : 4;
				npc.getNavigation().moveTo(target, 1.25);
			}

			// Realm Suppress on a weaker foe, only with qi enough to carry it.
			if (npc.tickCount % 10 == 0) {
				boolean weaker = CultivatorNpc.rankOf(target) < npc.sustainedRank();
				if (weaker && !npc.isSuppressing() && npc.canSuppressSustainably()) npc.setSuppressing(true);
				else if (!weaker && npc.isSuppressing()) npc.setSuppressing(false);
			}

			if (cooldown > 0) cooldown--;
			if (cooldown <= 0 && npc.isWithinMeleeAttackRange(target) && npc.getSensing().hasLineOfSight(target)) {
				npc.swing(InteractionHand.MAIN_HAND);
				npc.doHurtTarget(target);
				cooldown = attackCooldown(npc);
			}
		}
	}

	/** Departing its sect, going to ascend, or looking for a place of seclusion. */
	public static class JourneyGoal extends Goal {
		private final CultivatorNpc npc;
		private BlockPos site;
		private int searchIn;
		private int wanderTicks;
		private Vec3 wanderTo;

		public JourneyGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			CultivatorNpc.Lifecycle life = npc.getLifecycle();
			return npc.getTarget() == null && !npc.shouldFlee() && (life == CultivatorNpc.Lifecycle.DEPARTING
					|| life == CultivatorNpc.Lifecycle.ASCENDING || life == CultivatorNpc.Lifecycle.SEEKING_SECLUSION);
		}

		@Override
		public void start() {
			npc.setMeditating(false);
			site = null;
			searchIn = 0;
		}

		@Override public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void tick() {
			if (!(npc.level() instanceof ServerLevel level)) return;
			switch (npc.getLifecycle()) {
				case DEPARTING -> travel(level, npc.getJourneyTarget(), 16);
				case ASCENDING -> ascend(level);
				case SEEKING_SECLUSION -> seek(level);
				default -> {}
			}
		}

		/** Toward {@code target} through the air (or on foot), arriving within {@code arrival} blocks sideways. */
		private void travel(ServerLevel level, BlockPos target, double arrival) {
			if (target == null) {
				npc.arrive(level);
				return;
			}
			double dx = target.getX() + 0.5 - npc.getX(), dz = target.getZ() + 0.5 - npc.getZ();
			double sideways = Math.sqrt(dx * dx + dz * dz);
			if (sideways <= arrival) {
				if (npc.isQiFlying()) npc.land();
				if (npc.onGround() || !npc.isQiFlying()) npc.arrive(level);
				return;
			}
			if (!npc.isQiFlying() && npc.canFlySustainably()) npc.setQiFlying(true);
			if (npc.isQiFlying()) {
				// Cruise well above whatever is below, so hills and trees don't get in the way.
				int ground = level.hasChunkAt(npc.blockPosition()) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, npc.getBlockX(), npc.getBlockZ()) : (int) npc.getY();
				double cruise = Math.max(ground + 14, Math.min(npc.getY(), ground + 40));
				npc.flyToward(new Vec3(npc.getX() + dx / sideways * 16, cruise, npc.getZ() + dz / sideways * 16), 0.45);
			} else if (npc.getNavigation().isDone()) {
				walkToward(npc, new Vec3(target.getX() + 0.5, npc.getY(), target.getZ() + 0.5), 1.0);
			}
		}

		/** To the rift at Overworld 0, 0 (from another lower realm it rises out of sight; NpcTravel carries it on). */
		private void ascend(ServerLevel level) {
			if (level.dimension() != Level.OVERWORLD) {
				if (!npc.isQiFlying() && npc.canFlySustainably()) npc.setQiFlying(true);
				if (npc.isQiFlying()) npc.flyToward(npc.position().add(0, 6, 0), 0.3);
				return;
			}
			BlockPos rift = NpcTravel.riftPosition(level);
			if (rift == null) {
				travel(level, new BlockPos(SpatialRiftBlock.RIFT_X, (int) npc.getY(), SpatialRiftBlock.RIFT_Z), 6);
				return;
			}
			Vec3 into = Vec3.atCenterOf(rift);
			if (npc.position().distanceToSqr(into) < 4.0) {
				npc.arrive(level);
				return;
			}
			double sideways = Math.sqrt(Math.pow(into.x - npc.getX(), 2) + Math.pow(into.z - npc.getZ(), 2));
			if (sideways > 24) {
				travel(level, rift, 0);
			} else {
				if (!npc.isQiFlying()) npc.setQiFlying(true);
				if (npc.isQiFlying()) npc.flyToward(into, 0.3);
				else npc.getNavigation().moveTo(into.x, into.y, into.z, 1.0);
			}
		}

		/** Looks around for a cave or a mountainside; after a few tries, wanders on and looks again; at last, sits where it is. */
		private void seek(ServerLevel level) {
			if (site != null) {
				if (npc.position().distanceToSqr(Vec3.atBottomCenterOf(site)) < 2.5) {
					if (npc.isQiFlying()) npc.land();
					npc.enterSeclusion(level, site);
					site = null;
					return;
				}
				if (npc.isQiFlying()) npc.flyToward(Vec3.atBottomCenterOf(site).add(0, 0.2, 0), 0.35);
				else if (npc.getNavigation().isDone()) npc.getNavigation().moveTo(site.getX() + 0.5, site.getY(), site.getZ() + 0.5, 1.0);
				return;
			}
			if (wanderTicks > 0) {
				wanderTicks--;
				if (wanderTo != null) {
					if (npc.isQiFlying()) npc.flyToward(wanderTo, 0.4);
					else if (npc.getNavigation().isDone()) walkToward(npc, wanderTo, 1.0);
				}
				return;
			}
			if (--searchIn > 0) return;
			searchIn = 40;
			site = npc.findSeclusionSite(level);
			if (site != null) {
				if (npc.position().distanceToSqr(Vec3.atCenterOf(site)) > 400 && npc.canFlySustainably()) npc.setQiFlying(true);
				return;
			}
			int attempt = npc.nextSeclusionAttempt();
			if (attempt > 5 && npc.onGround()) {
				npc.enterSeclusion(level, npc.blockPosition()); // no better place to be found: here will do
				return;
			}
			double angle = npc.getRandom().nextDouble() * Math.PI * 2;
			wanderTo = npc.position().add(Math.cos(angle) * 64, 0, Math.sin(angle) * 64);
			wanderTicks = 200;
			if (npc.canFlySustainably()) {
				npc.setQiFlying(true);
				wanderTo = wanderTo.add(0, 20, 0);
			}
		}
	}

	/** Back on its mat in seclusion, meditating; its Concealment Barrier rises again once a fight is well over. */
	public static class SeclusionGoal extends Goal {
		private final CultivatorNpc npc;
		private int calm;

		public SeclusionGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return npc.getLifecycle() == CultivatorNpc.Lifecycle.SECLUDED && npc.getTarget() == null && !npc.shouldFlee() && npc.getSeclusionMat() != null;
		}

		@Override
		public void start() {
			calm = 0;
		}

		@Override
		public void tick() {
			BlockPos mat = npc.getSeclusionMat();
			if (mat == null) return;
			if (npc.position().distanceToSqr(Vec3.atBottomCenterOf(mat)) > 2.25) {
				npc.setMeditating(false);
				if (npc.getNavigation().isDone()) npc.getNavigation().moveTo(mat.getX() + 0.5, mat.getY(), mat.getZ() + 0.5, 1.0);
				return;
			}
			sit(npc, mat);
			if (!npc.isConcealed() && ++calm > 200 && npc.level() instanceof ServerLevel level) npc.raiseConcealment(level);
		}

		@Override
		public void stop() {
			npc.setMeditating(false);
		}
	}

	/** A leashed sect member outside its territory, with no fight to keep it there, goes straight back. */
	public static class ReturnToTerritoryGoal extends Goal {
		private final CultivatorNpc npc;
		private int repath;

		public ReturnToTerritoryGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (npc.getLifecycle() != CultivatorNpc.Lifecycle.SECT || npc.getTarget() != null) return false;
			Sect sect = npc.sect();
			return sect != null && npc.level().dimension() == sect.dimension && !sect.inTerritory(npc.blockPosition());
		}

		@Override
		public void start() {
			npc.setMeditating(false);
			repath = 0;
		}

		@Override
		public void tick() {
			Sect sect = npc.sect();
			if (sect == null) return;
			Vec3 core = Vec3.atBottomCenterOf(sect.core);
			if (npc.position().distanceToSqr(core) > 64 * 64 && npc.canFlySustainably()) npc.setQiFlying(true);
			if (npc.isQiFlying()) {
				npc.flyToward(core.add(0, 12, 0), 0.45);
				if (sect.inTerritory(npc.blockPosition())) npc.land();
			} else if (--repath <= 0) {
				repath = 40;
				walkToward(npc, core, 1.15);
			}
		}

		@Override
		public void stop() {
			if (npc.isQiFlying()) npc.land();
		}
	}

	/** A member's duty on the grounds, by title (see the class notes). */
	public static class SectDutyGoal extends Goal {
		private final CultivatorNpc npc;
		private BlockPos goal;
		private boolean seat;
		private int busy;
		private int patrolLeft;
		private int repath;

		public SectDutyGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (npc.getLifecycle() != CultivatorNpc.Lifecycle.SECT || npc.getTarget() != null || npc.shouldFlee()) return false;
			Sect sect = npc.sect();
			return sect != null && npc.level().dimension() == sect.dimension && sect.inTerritory(npc.blockPosition());
		}

		@Override
		public void start() {
			goal = null;
			busy = 0;
		}

		@Override
		public void stop() {
			npc.setMeditating(false);
			goal = null;
		}

		@Override
		public void tick() {
			Sect sect = npc.sect();
			if (sect == null) return;
			if (goal == null) choose(sect);
			if (goal == null) return;
			double distanceSqr = npc.position().distanceToSqr(Vec3.atBottomCenterOf(goal));
			if (distanceSqr > (seat ? 1.0 : 4.0)) {
				npc.setMeditating(false);
				if (--repath <= 0 || npc.getNavigation().isDone()) {
					repath = 40;
					if (distanceSqr > 24 * 24) walkToward(npc, Vec3.atBottomCenterOf(goal), 1.0);
					else npc.getNavigation().moveTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 0.9);
				}
				if (++busy > 900) goal = null; // stuck: pick another
				return;
			}
			if (seat) {
				// Someone else took the mat (a player sat down on it): find another.
				if (!npc.isMeditating() && occupied(npc, goal)) {
					goal = null;
					return;
				}
				sit(npc, goal);
			}
			if (--busy <= 0) goal = null;
		}

		/** What to do next, by title. */
		private void choose(Sect sect) {
			Sect.Member member = sect.members.get(npc.getUUID());
			if (member == null) return;
			busy = 0;
			repath = 0;
			switch (member.title) {
				case SECT_MASTER, GRAND_ELDER, ELDER -> {
					goal = sect.dutyPost(member);
					seat = true;
					busy = 1200;
				}
				case INNER_DISCIPLE -> {
					BlockPos stroll = npc.getRandom().nextFloat() < 0.25f || sect.posts(Sect.Post.MEDITATION).isEmpty() ? courtyardPost(sect) : null;
					if (stroll != null) {
						goal = stroll;
						seat = false;
						busy = 60 + npc.getRandom().nextInt(80);
					} else if (sect.posts(Sect.Post.MEDITATION).isEmpty()) {
						goal = sect.dwelling(member.home);
						seat = true;
						busy = 1200 + npc.getRandom().nextInt(1200);
					} else {
						goal = freeMat(sect);
						seat = true;
						busy = 2400 + npc.getRandom().nextInt(3600);
					}
				}
				default -> {
					if (patrolLeft <= 0) {
						// A rest at home between rounds of the perimeter.
						patrolLeft = 4 + npc.getRandom().nextInt(4);
						goal = sect.dwelling(member.home);
						seat = true;
						busy = 400 + npc.getRandom().nextInt(800);
					} else {
						patrolLeft--;
						goal = perimeterPoint(sect);
						seat = false;
						busy = 40 + npc.getRandom().nextInt(60);
					}
				}
			}
			if (goal != null && seat && occupied(npc, goal) && npc.position().distanceToSqr(Vec3.atBottomCenterOf(goal)) > 1.0) goal = null;
		}

		/**
		 * A courtyard to walk to, or null. Not the pagoda's top chamber or anywhere else well above it: walkers don't plan paths
		 * up ladders, so they'd only stand at the foot of it until they gave up.
		 */
		private BlockPos courtyardPost(Sect sect) {
			List<BlockPos> courtyards = sect.posts(Sect.Post.COURTYARD);
			for (int attempt = 0; attempt < 4 && !courtyards.isEmpty(); attempt++) {
				BlockPos post = courtyards.get(npc.getRandom().nextInt(courtyards.size()));
				if (post.getY() - npc.getBlockY() <= 3) return post;
			}
			return null;
		}

		/** A meditation mat nobody is sitting on (or the home mat if they're all taken). */
		private BlockPos freeMat(Sect sect) {
			List<BlockPos> mats = sect.posts(Sect.Post.MEDITATION);
			int start = npc.getRandom().nextInt(mats.size());
			for (int i = 0; i < mats.size(); i++) {
				BlockPos mat = mats.get((start + i) % mats.size());
				if (!occupied(npc, mat)) return mat;
			}
			Sect.Member member = sect.members.get(npc.getUUID());
			return sect.dwelling(member == null ? 0 : member.home);
		}

		/** A point on the outer ring: between 6 and 26 blocks outside the barrier, on the ground. */
		private BlockPos perimeterPoint(Sect sect) {
			double angle = Math.atan2(npc.getZ() - sect.core.getZ(), npc.getX() - sect.core.getX()) + (npc.getRandom().nextDouble() * 0.6 + 0.2);
			double radius = sect.barrierRadius + 6 + npc.getRandom().nextInt(21);
			int x = sect.core.getX() + (int) Math.round(Math.cos(angle) * radius);
			int z = sect.core.getZ() + (int) Math.round(Math.sin(angle) * radius);
			if (!npc.level().hasChunkAt(new BlockPos(x, 0, z))) return null;
			int y = Formations.surface(npc.level(), Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			return new BlockPos(x, y, z);
		}
	}

	/** A rogue's idle life: strolling about, and sitting down to meditate now and then. */
	public static class WanderGoal extends Goal {
		private final CultivatorNpc npc;
		private int meditate;
		private int strolls;

		public WanderGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return npc.getLifecycle() == CultivatorNpc.Lifecycle.ROGUE && npc.getTarget() == null && !npc.shouldFlee();
		}

		@Override
		public void tick() {
			if (meditate > 0) {
				if (--meditate == 0 || !npc.onGround()) {
					npc.setMeditating(false);
					meditate = 0;
				} else {
					npc.setMeditating(!npc.isMortal());
				}
				return;
			}
			if (!npc.getNavigation().isDone()) return;
			if (strolls > 2 && npc.onGround() && npc.getRandom().nextInt(3) == 0 && !npc.isMortal()) {
				strolls = 0;
				npc.getNavigation().stop();
				meditate = 600 + npc.getRandom().nextInt(1800);
				npc.setMeditating(true);
				return;
			}
			if (npc.getRandom().nextInt(60) != 0) return;
			Vec3 spot = LandRandomPos.getPos(npc, 14, 6);
			if (spot != null) {
				strolls++;
				npc.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.8);
			}
		}

		@Override
		public void stop() {
			npc.setMeditating(false);
			meditate = 0;
		}
	}

	// --- Target goals ---

	/** Fights back against whoever hurt it (never its own sect). */
	public static class RetaliateGoal extends HurtByTargetGoal {
		private final CultivatorNpc npc;

		public RetaliateGoal(CultivatorNpc npc) {
			super(npc);
			this.npc = npc;
		}

		@Override
		public boolean canUse() {
			LivingEntity attacker = npc.getLastHurtByMob();
			return attacker != null && !npc.isAlly(attacker) && !npc.shouldFlee() && super.canUse();
		}
	}

	/**
	 * Goes for enemies of its path on sight: players and NPCs of the opposing path within sight (16 blocks), or within its open
	 * domain even through walls. Sect members only within their territory; never Elders or the Master (they answer calls),
	 * never in seclusion or on a journey.
	 */
	public static class HostilityGoal extends Goal {
		private final CultivatorNpc npc;
		private int scanIn;

		public HostilityGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.TARGET));
		}

		private boolean wants(LivingEntity target) {
			if (npc.isConcealed() || npc.shouldFlee()) return false;
			CultivatorNpc.Lifecycle life = npc.getLifecycle();
			if (life != CultivatorNpc.Lifecycle.SECT && life != CultivatorNpc.Lifecycle.ROGUE) return false;
			if (npc.getTitle().isElderOrAbove()) return false;
			if (!npc.isEnemy(target)) return false;
			if (npc.isSectMember()) {
				Sect sect = npc.sect();
				return sect != null && sect.inTerritory(target.blockPosition());
			}
			return true;
		}

		@Override
		public boolean canUse() {
			if (npc.getTarget() != null || --scanIn > 0) return false;
			scanIn = 10 + npc.getRandom().nextInt(10);
			return find() != null;
		}

		private LivingEntity find() {
			double sight = 16;
			double domain = npc.isDomainOpen() ? Math.min(npc.domainRadius(), 48) : 0;
			double range = Math.max(sight, domain);
			LivingEntity best = null;
			double bestDistance = Double.MAX_VALUE;
			for (LivingEntity candidate : ConsciousnessDomainHandler.entitiesIn(npc, range, LivingEntity.class, this::wants)) {
				double d = npc.distanceToSqr(candidate);
				boolean sensed = d <= domain * domain || d <= sight * sight && npc.getSensing().hasLineOfSight(candidate);
				if (sensed && d < bestDistance) {
					best = candidate;
					bestDistance = d;
				}
			}
			return best;
		}

		@Override
		public void start() {
			LivingEntity target = find();
			if (target == null) return;
			npc.setTarget(target);
			npc.markAggressed(target);
		}

		@Override
		public boolean canContinueToUse() {
			return false; // the target stays with the combat goal until it's dealt with
		}
	}

	/** Outer and Inner Disciples clear monsters off the grounds. */
	public static class GuardGoal extends Goal {
		private final CultivatorNpc npc;
		private int scanIn;

		public GuardGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (npc.getTarget() != null || npc.getLifecycle() != CultivatorNpc.Lifecycle.SECT || --scanIn > 0) return false;
			NpcTitle title = npc.getTitle();
			if (title != NpcTitle.OUTER_DISCIPLE && title != NpcTitle.INNER_DISCIPLE) return false;
			scanIn = 20;
			return find() != null;
		}

		private LivingEntity find() {
			Sect sect = npc.sect();
			if (sect == null) return null;
			LivingEntity best = null;
			double bestDistance = Double.MAX_VALUE;
			for (LivingEntity monster : ConsciousnessDomainHandler.entitiesIn(npc, 12, LivingEntity.class,
					e -> e.isAlive() && CultivatorNpc.isMonster(e) && sect.inTerritory(e.blockPosition()))) {
				double d = npc.distanceToSqr(monster);
				if (d < bestDistance && npc.getSensing().hasLineOfSight(monster)) {
					best = monster;
					bestDistance = d;
				}
			}
			return best;
		}

		@Override
		public void start() {
			npc.setTarget(find());
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}
	}

	private CultivatorGoals() {}
}
