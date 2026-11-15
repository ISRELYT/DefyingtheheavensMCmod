package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Fighting from the saddle (the user's "Build as designed", 2026-11-13): the Saddle Strike key (B by default: ModKeybinds#SADDLE_STRIKE, C2S
 * SADDLE_STRIKE) has the bound beast its owner rides strike what they look at with its kind's own move ({@link #moveOf}): a tiger's pounce, a
 * boar's, an antelope's or a rhino's charge, a roc's dive or a hawk's swoop, an ape's or a bear's slam, a fox's breath, a python's coil, a
 * monkey's burning stone or a flood dragon's jet; the rest (wolves, vipers, crocodiles, turtles) a lunging bite, its venom or its hold in it as
 * ever. As strong as its wild kin strike (DemonBeastGoals: its attack by the move's measure), and spent for as long as theirs
 * (DemonBeastGoals#restAfter, DemonBeast#signatureReady). Ridden it still takes up no fight of its own (DemonBeast#tendRidden).
 * <p>
 * A ridden beast is moved by its rider's client, so the moves that carry it (a charge, a pounce, a dive, a bite's lunge) and those that hold
 * it still (a coil, a slam's wind-up, the breath) are its rider's client's to carry out (S2C SADDLE_LUNGE: DemonBeast#lunge); the server judges
 * what it reaches each tick ({@link #tick}) and tells the client when it is over. Whom it strikes: alive, in the move's reach, not its rider,
 * nor anyone its rider's own blows would spare (HeavenSpells#spared: their sect's fellows, their own beasts, a player with no fighting
 * allowed), nor a soul or one of the dead.
 */
public final class SaddleStrikes {
	/** The moves a ridden beast strikes with: its signature's ({@link #moveOf}), or a bite. */
	public enum Move { CHARGE, POUNCE, SLAM, BREATH, COIL, DIVE, SHOT, BITE }

	/** How far off (blocks, edge to edge) what its rider looks at may be for each move; an elder's the further as it is grown (DemonBeastGoals#grown). */
	public static final double CHARGE_REACH = 14, POUNCE_REACH = 9, BREATH_REACH = 6, COIL_REACH = 2, DIVE_REACH = 24, BITE_REACH = 2.5,
			STONE_REACH = 16, JET_REACH = 20;
	/** How long each move goes on at most (ticks); a slam comes down {@link #SLAM_AT} in. */
	public static final int CHARGE_TICKS = 40, POUNCE_TICKS = 30, SLAM_TICKS = 14, SLAM_AT = 12, BREATH_TICKS = 30, COIL_TICKS = 60, DIVE_TICKS = 40,
			BITE_TICKS = 8;
	/** How wide of what its rider looks at the aim still finds it (blocks). */
	public static final double AIM_SLACK = 0.6;
	/** A charge's pace (blocks a tick) as its wild kin's, by its rank, and at most this; a bite's lunge; a dive's at least, by its flight's pace. */
	public static final double CHARGE_PACE_MOST = 1.2, BITE_PACE = 0.45, DIVE_PACE_LEAST = 0.8, DIVE_PACE_MOST = 2.0;

	/** A strike under way, the server's (DemonBeast#saddleStrike): its move, its kind's way of striking, its rider, what it strikes, how long it has gone on. */
	static final class Strike {
		final Move move;
		final BeastKind.Signature how;
		final ServerPlayer rider;
		final LivingEntity target;
		/** A charge's way across the land; a pounce's leap. */
		Vec3 way = Vec3.ZERO;
		int ticks;

		Strike(Move move, BeastKind.Signature how, ServerPlayer rider, LivingEntity target) {
			this.move = move;
			this.how = how;
			this.rider = rider;
			this.target = target;
		}
	}

	/** The move a beast striking in its kind's way ({@code how}) strikes with from the saddle; a bite for those whose move is in their bite or no strike. */
	public static Move moveOf(BeastKind.Signature how) {
		return switch (how) {
			case CHARGE, SPARK_CHARGE, STUN_CHARGE -> Move.CHARGE;
			case POUNCE -> Move.POUNCE;
			case ICE_SLAM, POUND -> Move.SLAM;
			case BREATH -> Move.BREATH;
			case COIL -> Move.COIL;
			case SWOOP, DIVE -> Move.DIVE;
			case THROW, WATER_JET -> Move.SHOT;
			default -> Move.BITE;
		};
	}

	/** How far {@code beast}'s move reaches: from its edge, a slam's from its middle (as its wild kin's: DemonBeastGoals.SlamGoal). */
	public static double reach(DemonBeast beast) {
		BeastKind.Signature how = beast.kind().signature;
		double grown = DemonBeastGoals.grown(beast);
		return switch (moveOf(how)) {
			case CHARGE -> CHARGE_REACH;
			case POUNCE -> POUNCE_REACH * grown;
			case SLAM -> (how == BeastKind.Signature.ICE_SLAM ? 3.5 : 4.5) * (beast.kind().isElder() ? grown : beast.scale());
			case BREATH -> BREATH_REACH * grown;
			case COIL -> COIL_REACH;
			case DIVE -> DIVE_REACH * grown;
			case SHOT -> how == BeastKind.Signature.WATER_JET ? JET_REACH : STONE_REACH;
			case BITE -> BITE_REACH;
		};
	}

	/** Whether {@code e} is within {@code beast}'s move's {@code reach}. */
	static boolean within(DemonBeast beast, LivingEntity e, double reach) {
		if (moveOf(beast.kind().signature) == Move.SLAM) return beast.distanceToSqr(e) <= reach * reach;
		double d = Math.sqrt(beast.distanceToSqr(e)) - beast.getBbWidth() / 2 - e.getBbWidth() / 2;
		return d <= reach;
	}

	// --- The key ---

	/**
	 * {@code rider} pressed the Saddle Strike key (C2S SADDLE_STRIKE): the bound beast of theirs they ride strikes what they look at, its move
	 * ready and something it may strike in its reach; else a word why (its wait, or nothing there). Nothing while a strike is under way.
	 */
	public static void strike(ServerPlayer rider) {
		if (!(rider.getVehicle() instanceof DemonBeast beast) || beast.rider() != rider || !rider.getUUID().equals(beast.getOwnerUUID())) return;
		if (!beast.isAlive() || beast.saddleStrike != null) return;
		long now = beast.level().getGameTime();
		if (now < beast.signatureReady) {
			tell(rider, Component.translatable(ModLang.MSG_SADDLE_RESTING, beast.getDisplayName(), (beast.signatureReady - now + 19) / 20));
			return;
		}
		LivingEntity target = lookedAt(rider, beast, reach(beast));
		if (target == null) {
			tell(rider, Component.translatable(ModLang.MSG_SADDLE_NOTHING, beast.getDisplayName()));
			return;
		}
		begin(beast, rider, target);
	}

	private static void tell(ServerPlayer rider, Component message) {
		rider.displayClientMessage(message.copy().withStyle(ChatFormatting.GRAY), true);
	}

	/**
	 * What {@code rider} looks at that their {@code beast} may strike within {@code reach} (#within): the nearest along their sight, {@link #AIM_SLACK}
	 * wide, short of the first block in the way; or null.
	 */
	static LivingEntity lookedAt(ServerPlayer rider, DemonBeast beast, double reach) {
		Vec3 eye = rider.getEyePosition();
		Vec3 look = Vec3.directionFromRotation(rider.getXRot(), rider.getYRot());
		double far = reach + beast.getBbWidth() + 3;
		Vec3 end = eye.add(look.scale(far));
		BlockHitResult block = rider.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
		if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
		LivingEntity best = null;
		double nearest = Double.MAX_VALUE;
		for (LivingEntity e : rider.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(AIM_SLACK + 1), e -> strikable(rider, beast, e))) {
			AABB box = e.getBoundingBox().inflate(AIM_SLACK);
			Optional<Vec3> hit = box.clip(eye, end);
			double d;
			if (box.contains(eye)) d = 0;
			else if (hit.isPresent()) d = eye.distanceToSqr(hit.get());
			else continue;
			if (d < nearest && within(beast, e, reach)) {
				nearest = d;
				best = e;
			}
		}
		return best;
	}

	/** Whether {@code beast}, ridden by {@code rider}, may strike {@code e} (or catch it in a slam, its breath, a dive's gust). */
	static boolean strikable(ServerPlayer rider, DemonBeast beast, LivingEntity e) {
		if (e == beast || e == rider || !e.isAlive() || e.isPassengerOfSameVehicle(rider)) return false;
		if (e instanceof Player player && (player.isSpectator() || player.getAbilities().invulnerable || Incorporeal.untouchable(player))) return false;
		return !HeavenSpells.spared(rider, e);
	}

	// --- A strike ---

	/** {@code beast} begins its move on {@code target}: its wait set now (a strike that misses spends it too), its rider's client set carrying it. */
	static void begin(DemonBeast beast, ServerPlayer rider, LivingEntity target) {
		BeastKind.Signature how = beast.kind().signature;
		Move move = moveOf(how);
		Strike strike = new Strike(move, how, rider, target);
		beast.signatureReady = beast.level().getGameTime() + DemonBeastGoals.restAfter(how);
		Vec3 to = target.position().subtract(beast.position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 across = flat < 1.0e-3 ? Vec3.directionFromRotation(0, beast.getYRot()) : new Vec3(to.x / flat, 0, to.z / flat);
		switch (move) {
			case CHARGE -> {
				strike.way = across;
				beast.setActing(DemonBeast.ACT_CHARGE, true);
				DemonBeastGoals.sound(beast, how == BeastKind.Signature.STUN_CHARGE ? SoundEvents.RAVAGER_ROAR : how == BeastKind.Signature.CHARGE
						? SoundEvents.HOGLIN_ANGRY : SoundEvents.GOAT_SCREAMING_PREPARE_RAM, 1.2f, 0.9f);
				ModPackets.sendSaddleLunge(rider, beast, move, target, across, Math.min(CHARGE_PACE_MOST, 0.55 + beast.getRank() * 0.03), CHARGE_TICKS);
			}
			case POUNCE -> {
				// The wild pounce's leap (DemonBeastGoals.PounceGoal): further the further off its prey, as far as it is grown.
				double reach = Math.min(1.4 * DemonBeastGoals.grown(beast), 0.35 + flat * 0.13);
				strike.way = new Vec3(across.x * reach, 0.45 + flat * 0.02, across.z * reach);
				DemonBeastGoals.sound(beast, SoundEvents.RAVAGER_ROAR, 0.9f, 1.4f);
				ModPackets.sendSaddleLunge(rider, beast, move, target, strike.way, 0, POUNCE_TICKS);
			}
			case DIVE -> {
				boolean dive = how == BeastKind.Signature.DIVE;
				DemonBeastGoals.sound(beast, dive ? SoundEvents.ENDER_DRAGON_FLAP : SoundEvents.PHANTOM_SWOOP, 1.2f, dive ? 0.6f : 1.2f);
				double pace = Math.max(DIVE_PACE_LEAST, Math.min(DIVE_PACE_MOST, SpiritBonds.flightPace(beast) * 1.6));
				ModPackets.sendSaddleLunge(rider, beast, move, target, Vec3.ZERO, pace, DIVE_TICKS);
			}
			case BITE -> ModPackets.sendSaddleLunge(rider, beast, move, target, across, BITE_PACE, BITE_TICKS);
			case COIL -> {
				beast.setActing(DemonBeast.ACT_COIL, true);
				DemonBeastGoals.sound(beast, SoundEvents.PHANTOM_BITE, 1.0f, 0.5f);
				ModPackets.sendSaddleLunge(rider, beast, move, target, Vec3.ZERO, 0, COIL_TICKS);
			}
			case SLAM -> {
				beast.swing(InteractionHand.MAIN_HAND);
				ModPackets.sendSaddleLunge(rider, beast, move, target, Vec3.ZERO, 0, SLAM_TICKS);
			}
			case BREATH -> {
				beast.setActing(DemonBeast.ACT_BREATH, true);
				DemonBeastGoals.sound(beast, SoundEvents.BLAZE_SHOOT, 1.0f, 0.7f);
				ModPackets.sendSaddleLunge(rider, beast, move, target, Vec3.ZERO, 0, BREATH_TICKS);
			}
			case SHOT -> {
				shoot(beast, how, target);
				return; // loosed: nothing more to it
			}
		}
		beast.saddleStrike = strike;
	}

	/** How long a move goes on at most. */
	static int lasts(Move move) {
		return switch (move) {
			case CHARGE -> CHARGE_TICKS;
			case POUNCE -> POUNCE_TICKS;
			case SLAM -> SLAM_TICKS;
			case BREATH -> BREATH_TICKS;
			case COIL -> COIL_TICKS;
			case DIVE -> DIVE_TICKS;
			case BITE -> BITE_TICKS;
			case SHOT -> 0;
		};
	}

	/**
	 * Each tick of {@code beast}'s on the server (DemonBeast#aiStep): its strike under way goes on, what it reaches judged; over when it lands,
	 * its time is up, its rider is off it, or what it strikes is gone.
	 */
	static void tick(DemonBeast beast) {
		Strike s = beast.saddleStrike;
		if (s == null || !(beast.level() instanceof ServerLevel level)) return;
		s.ticks++;
		LivingEntity target = s.target;
		if (beast.rider() != s.rider || !beast.isAlive() || !target.isAlive() || target.isRemoved() || target.level() != beast.level() || s.ticks > lasts(s.move)) {
			end(beast, s);
			return;
		}
		float attack = DemonBeastGoals.attack(beast);
		switch (s.move) {
			case CHARGE -> {
				if (s.how == BeastKind.Signature.SPARK_CHARGE && s.ticks % 2 == 0) {
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, beast.getX(), beast.getY() + beast.getBbHeight() * 0.6, beast.getZ(), 3, 0.3, 0.3, 0.3, 0.05);
				}
				if (!touches(beast, target, 0.4)) return;
				if (target.hurt(beast.damageSources().mobAttack(beast), attack * 1.6f)) {
					target.knockback(1.6, -s.way.x, -s.way.z);
					target.setDeltaMovement(target.getDeltaMovement().add(0, 0.3, 0));
					target.hurtMarked = true;
					if (s.how == BeastKind.Signature.SPARK_CHARGE) DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 40, 1, beast);
					if (s.how == BeastKind.Signature.STUN_CHARGE) {
						DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 40, 5, beast);
						DemonBeast.afflict(target, MobEffects.WEAKNESS, 40, 2, beast);
						level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getEyeY(), target.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
					}
					DemonBeastGoals.sound(beast, SoundEvents.GOAT_RAM_IMPACT, 1.2f, 0.7f);
				}
				end(beast, s);
			}
			case POUNCE -> {
				if (!touches(beast, target, 0.6)) return;
				if (target.hurt(beast.damageSources().mobAttack(beast), attack * 1.7f)) DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 30, 1, beast);
				end(beast, s);
			}
			case DIVE -> {
				if (!touches(beast, target, 0.8)) return;
				boolean dive = s.how == BeastKind.Signature.DIVE;
				beast.swing(InteractionHand.MAIN_HAND);
				if (target.hurt(beast.damageSources().mobAttack(beast), attack * (dive ? 1.8f : 1.4f)) && dive) gust(level, beast, s.rider);
				end(beast, s);
			}
			case BITE -> {
				if (!touches(beast, target, 0.8)) return;
				beast.swing(InteractionHand.MAIN_HAND);
				beast.doHurtTarget(target); // a viper's venom, a crocodile's hold in it (DemonBeastGoals#onBite)
				end(beast, s);
			}
			case SLAM -> {
				if (s.ticks == SLAM_AT) slam(level, beast, s);
			}
			case BREATH -> breathe(level, beast, s);
			case COIL -> coil(beast, s);
			default -> end(beast, s);
		}
	}

	/** Whether {@code beast} has reached {@code target} ({@code slack} blocks round it). */
	static boolean touches(DemonBeast beast, LivingEntity target, double slack) {
		return beast.getBoundingBox().inflate(slack).intersects(target.getBoundingBox());
	}

	/** {@code s} over: its look's act ended, its rider's client told to stop carrying it. */
	static void end(DemonBeast beast, Strike s) {
		if (beast.saddleStrike == s) beast.saddleStrike = null;
		switch (s.move) {
			case CHARGE -> beast.setActing(DemonBeast.ACT_CHARGE, false);
			case COIL -> beast.setActing(DemonBeast.ACT_COIL, false);
			case BREATH -> beast.setActing(DemonBeast.ACT_BREATH, false);
			default -> {
			}
		}
		if (s.rider.isAlive() && !s.rider.hasDisconnected() && s.rider.getVehicle() == beast) {
			ModPackets.sendSaddleLunge(s.rider, beast, s.move, null, Vec3.ZERO, 0, 0);
		}
	}

	// --- The moves that are the server's ---

	/** The slam coming down (as its wild kin's: DemonBeastGoals.SlamGoal): all round it its reach, but its rider and whom they would spare. */
	private static void slam(ServerLevel level, DemonBeast beast, Strike s) {
		boolean frost = s.how == BeastKind.Signature.ICE_SLAM;
		double radius = reach(beast);
		float harm = DemonBeastGoals.attack(beast) * 1.3f;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(radius),
				e -> e.distanceToSqr(beast) <= radius * radius && strikable(s.rider, beast, e) && beast.hasLineOfSight(e))) {
			if (!other.hurt(beast.damageSources().mobAttack(beast), harm)) continue;
			if (frost) {
				DemonBeast.afflict(other, MobEffects.MOVEMENT_SLOWDOWN, 80, 2, beast);
				other.setTicksFrozen(Math.max(other.getTicksFrozen(), 160));
			} else {
				Vec3 away = other.position().subtract(beast.position());
				away = new Vec3(away.x, 0, away.z);
				away = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize();
				double share = DemonBeastGoals.pushShare(other);
				other.setDeltaMovement(away.x * 0.8 * share, 0.7 * share, away.z * 0.8 * share);
				other.hurtMarked = true;
			}
		}
		if (frost) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, beast.getX(), beast.getY() + 0.3, beast.getZ(), 60, radius * 0.4, 0.2, radius * 0.4, 0.05);
			DemonBeastGoals.sound(beast, SoundEvents.GLASS_BREAK, 1.0f, 0.6f);
		} else {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), beast.getX(), beast.getY() + 0.2, beast.getZ(), 80,
					radius * 0.4, 0.2, radius * 0.4, 0.2);
			DemonBeastGoals.sound(beast, SoundEvents.GENERIC_EXPLODE, 0.8f, 0.6f);
		}
	}

	/** A tick of its breath (as its wild kin's: DemonBeastGoals.BreathGoal), a cone of fire at what its rider looked at, their own side spared. */
	private static void breathe(ServerLevel level, DemonBeast beast, Strike s) {
		Vec3 mouth = beast.getEyePosition();
		Vec3 look = s.target.getEyePosition().subtract(mouth).normalize();
		double grown = DemonBeastGoals.grown(beast), length = 7 * grown;
		if (s.ticks % 2 == 0) {
			for (int i = 1; i <= (int) (6 * grown); i++) {
				Vec3 at = mouth.add(look.scale(i));
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, i * 0.08, i * 0.08, i * 0.08, 0.01);
			}
		}
		if (s.ticks % 6 != 0) return;
		float harm = DemonBeastGoals.attack(beast) * 0.6f;
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(length),
				e -> strikable(s.rider, beast, e) && beast.hasLineOfSight(e))) {
			Vec3 to = other.getEyePosition().subtract(mouth);
			if (to.length() > length || to.normalize().dot(look) < 0.8) continue;
			if (other.hurt(beast.damageSources().mobAttack(beast), harm)) other.setSecondsOnFire(3);
		}
	}

	/** A tick of its coil (as its wild kin's: DemonBeastGoals.CoilGoal): held before its jaws, crushed now and then; over if it gets away. */
	private static void coil(DemonBeast beast, Strike s) {
		LivingEntity target = s.target;
		double dx = target.getX() - beast.getX(), dz = target.getZ() - beast.getZ();
		if (dx * dx + dz * dz >= 36 || Math.abs(target.getY() - beast.getY()) > 3) {
			end(beast, s);
			return;
		}
		Vec3 hold = beast.position().add(Vec3.directionFromRotation(0, beast.getYRot()).scale(beast.getBbWidth() * 0.7 + 0.5));
		DemonBeast.afflict(target, MobEffects.MOVEMENT_SLOWDOWN, 5, 6, beast);
		if (target.position().distanceToSqr(hold) > 1.0 && beast.hasLineOfSight(target)
				&& beast.level().noCollision(target, target.getBoundingBox().move(hold.x - target.getX(), 0, hold.z - target.getZ()))) {
			if (target instanceof ServerPlayer player) player.teleportTo(hold.x, target.getY(), hold.z);
			else target.teleportTo(hold.x, target.getY(), hold.z);
		}
		if (s.ticks % 12 == 0) target.hurt(beast.damageSources().mobAttack(beast), DemonBeastGoals.attack(beast) * 0.7f);
	}

	/** The roc's wings' gust as its dive strikes (as its wild kin's), its rider and their own side not thrown. */
	private static void gust(ServerLevel level, DemonBeast beast, ServerPlayer rider) {
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(4 * DemonBeastGoals.grown(beast)),
				e -> strikable(rider, beast, e) && beast.hasLineOfSight(e))) {
			Vec3 away = other.position().subtract(beast.position());
			away = away.lengthSqr() < 1.0e-6 ? Vec3.ZERO : away.normalize();
			double share = DemonBeastGoals.pushShare(other);
			other.setDeltaMovement(away.x * 1.2 * share, 0.5 * share, away.z * 1.2 * share);
			other.hurtMarked = true;
		}
		level.sendParticles(ParticleTypes.CLOUD, beast.getX(), beast.getY(), beast.getZ(), 40, 2, 0.5, 2, 0.1);
	}

	/** Its stone or its jet loosed at {@code target} (as its wild kin's: DemonBeastGoals.ShootGoal), which may strike another beast from the saddle. */
	private static void shoot(DemonBeast beast, BeastKind.Signature how, LivingEntity target) {
		if (!(beast.level() instanceof ServerLevel level)) return;
		boolean water = how == BeastKind.Signature.WATER_JET;
		BeastShot.Kind kind = water ? BeastShot.Kind.WATER : BeastShot.Kind.FIRE_STONE;
		beast.swing(InteractionHand.MAIN_HAND);
		BeastShot projectile = new BeastShot(level, beast, kind, DemonBeastGoals.attack(beast) * (water ? 1.0f : 0.8f));
		projectile.fromSaddle();
		Vec3 from = beast.getEyePosition();
		projectile.setPos(from.x, from.y - 0.1, from.z);
		double dx = target.getX() - from.x, dz = target.getZ() - from.z;
		double dy = target.getY(0.5) - projectile.getY();
		double flat = Math.sqrt(dx * dx + dz * dz);
		projectile.shoot(dx, dy + (water ? 0 : flat * 0.2), dz, water ? 1.6f : 1.1f, 2.0f);
		level.addFreshEntity(projectile);
		DemonBeastGoals.sound(beast, water ? SoundEvents.DROWNED_SHOOT : SoundEvents.SNOWBALL_THROW, 1.0f, 0.8f);
	}

	private SaddleStrikes() {}
}
