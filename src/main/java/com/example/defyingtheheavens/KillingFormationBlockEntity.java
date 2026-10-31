package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A killing formation at work (KillingFormationBlock): every few ticks, while its realm stands open, on everyone within its reach but its
 * realm's guardians and those looking on. Its harm is a share of each one's own body ({@link #BLADE_SHARE}, {@link #FIRE_SHARE}), so it is
 * no less a danger to the strong.
 */
public class KillingFormationBlockEntity extends BlockEntity {
	/** The blades cut this share of a body each time ({@link #BLADE_EVERY} ticks); the fire burns this share a second. */
	public static final float BLADE_SHARE = 0.08f, FIRE_SHARE = 0.03f;
	static final int BLADE_EVERY = 30;

	public KillingFormationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.KILLING_FORMATION, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, KillingFormationBlockEntity formation) {
		if (!(level instanceof ServerLevel server) || level.getGameTime() % 10 != 0) return;
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm == null || SecretRealms.Data.get(server.getServer()).of(realm).openedAt < 0) return;
		KillingFormationBlock.Array array = state.getValue(KillingFormationBlock.ARRAY);
		Vec3 middle = Vec3.atBottomCenterOf(pos.above());
		long time = level.getGameTime();
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(array.reach, 4, array.reach), KillingFormationBlockEntity::subject)) {
			if (e.position().distanceToSqr(middle) > array.reach * array.reach + 16) continue;
			switch (array) {
				case BLADES -> {
					if (time % BLADE_EVERY != 0) break;
					e.hurt(level.damageSources().magic(), Math.max(2, e.getMaxHealth() * BLADE_SHARE));
					server.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
					server.playSound(null, e.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.BLOCKS, 0.8f, 1.4f);
				}
				case FIRE -> {
					if (time % 20 != 0) break;
					e.setSecondsOnFire(4);
					e.hurt(level.damageSources().inFire(), Math.max(1, e.getMaxHealth() * FIRE_SHARE));
					server.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + 0.1, e.getZ(), 12, 0.4, 0.1, 0.4, 0.04);
				}
				case GRAVITY -> {
					e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false, true));
					e.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 128, false, false, false)); // a jump of 128 or more holds one down
					if (e instanceof ServerPlayer p && p.getAbilities().flying && !p.isCreative()) {
						p.getAbilities().flying = false;
						p.onUpdateAbilities();
					}
					Vec3 v = e.getDeltaMovement();
					if (v.y > -0.3) {
						e.setDeltaMovement(v.x, Math.min(v.y, 0) - 0.3, v.z);
						e.hurtMarked = true;
					}
					if (time % 20 == 0) server.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 2, e.getZ(), 6, 0.3, 0.3, 0.3, 0);
				}
				case FOG -> {
					e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false, true));
					// Lost in it: now and then turned about.
					if (e instanceof ServerPlayer p && time % 60 == 0 && p.getRandom().nextInt(3) == 0) {
						float turn = 90 + p.getRandom().nextFloat() * 180;
						p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot() + turn, p.getXRot());
					}
					if (time % 20 == 0) server.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + 1, e.getZ(), 6, 1.2, 0.6, 1.2, 0.01);
				}
			}
		}
	}

	/** Whom a formation works on: anyone alive but the realm's guardians, a player in creative and one looking on. */
	private static boolean subject(LivingEntity e) {
		if (!e.isAlive() || e instanceof RealmGuardian) return false;
		return !(e instanceof Player p) || !p.isCreative() && !p.isSpectator();
	}
}
