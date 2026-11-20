package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Puppet formations (puppet phase 3, the linked army; the design: "Four or more puppets can surround a target and lock it in a crushing
 * formation"). {@link Puppetry#FORMATION_SIZE} or more of one puppeteer's linked puppets (a fed Link Knot: CircuitBoard.Scores#linked),
 * awake, fighting the same foe, spread round it at even angles, each at its blow's reach ({@link #spot}: PuppetEntity's fight goes there
 * instead of straight at the foe). Once each stands within {@link Puppetry#FORMATION_REACH} blocks of it and no gap between neighbours round
 * it is wider than {@link Puppetry#FORMATION_GAP} degrees, it is locked: Bound (it neither moves, jumps nor flies) while the ring holds,
 * and crushed every second for {@link Puppetry#FORMATION_CRUSH} of their blows together. Each fighter works its own place out every half
 * second ({@link #update}); the first of them (by its id) locks and crushes, so it is done once.
 */
public final class PuppetFormations {
	/** How far round its foe a fighter looks for its fellows. */
	private static final double GATHER = 12;
	/** The lock's hold, renewed while the ring stands (ticks). */
	private static final int HOLD_TICKS = 30;
	/** When each foe locked in a ring was last crushed (the game's time): once a second. */
	private static final java.util.Map<LivingEntity, Long> CRUSHED = new java.util.WeakHashMap<>();

	/** A fighter's half-second look at its fight: its place in a ring round its foe (null: none), and, the first of them, the lock. */
	static @Nullable Vec3 update(PuppetEntity puppet) {
		LivingEntity foe = puppet.getTarget();
		UUID owner = puppet.getOwnerUUID();
		if (foe == null || !foe.isAlive() || owner == null || !puppet.linked() || puppet.isStill() || !(puppet.level() instanceof ServerLevel level)) return null;
		List<PuppetEntity> ring = new ArrayList<>(level.getEntitiesOfClass(PuppetEntity.class, foe.getBoundingBox().inflate(GATHER),
				p -> p.isAlive() && !p.isStill() && p.linked() && owner.equals(p.getOwnerUUID()) && p.getTarget() == foe));
		if (ring.size() < Puppetry.FORMATION_SIZE || !ring.contains(puppet)) return null;
		ring.sort(Comparator.comparing(PuppetEntity::getUUID));
		// Even angles round the foe from where the first of them stands: each fighter's the same reckoning, so none crosses another's place.
		PuppetEntity first = ring.get(0);
		double base = Math.atan2(first.getZ() - foe.getZ(), first.getX() - foe.getX());
		int i = ring.indexOf(puppet), n = ring.size();
		double angle = base + 2 * Math.PI * i / n;
		double out = Math.max(1.0, puppet.getBbWidth() * 0.9 + foe.getBbWidth() + 1.0 - 0.5);
		Vec3 spot = new Vec3(foe.getX() + Math.cos(angle) * out, foe.getY(), foe.getZ() + Math.sin(angle) * out);
		if (puppet == first && closed(foe, ring)) lock(level, foe, ring, out);
		return spot;
	}

	/** Whether {@code ring} has closed round {@code foe}: each of them near it, and no gap round it wider than the most allowed. */
	static boolean closed(LivingEntity foe, List<PuppetEntity> ring) {
		List<Double> angles = new ArrayList<>();
		for (PuppetEntity p : ring) {
			double dx = p.getX() - foe.getX(), dz = p.getZ() - foe.getZ();
			if (Math.sqrt(dx * dx + dz * dz) > Puppetry.FORMATION_REACH) return false;
			angles.add(Math.toDegrees(Math.atan2(dz, dx)));
		}
		angles.sort(Double::compare);
		double widest = 360 - (angles.get(angles.size() - 1) - angles.get(0));
		for (int k = 1; k < angles.size(); k++) widest = Math.max(widest, angles.get(k) - angles.get(k - 1));
		return widest <= Puppetry.FORMATION_GAP;
	}

	/** The ring closed round {@code foe}: held still a while longer, and once a second crushed by their blows together. */
	private static void lock(ServerLevel level, LivingEntity foe, List<PuppetEntity> ring, double out) {
		boolean fresh = !foe.hasEffect(ModEffects.BOUND);
		foe.addEffect(new MobEffectInstance(ModEffects.BOUND, HOLD_TICKS, 0, false, true, true), ring.get(0));
		if (fresh) level.playSound(null, foe.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.0f, 0.6f);
		// The ring of qi between them, round the foe.
		for (int k = 0; k < 24; k++) {
			double a = 2 * Math.PI * k / 24;
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, foe.getX() + Math.cos(a) * out, foe.getY() + 0.3, foe.getZ() + Math.sin(a) * out, 1, 0, 0.05, 0, 0);
		}
		long now = level.getGameTime();
		Long last = CRUSHED.get(foe);
		if (last != null && now - last < 20) return;
		CRUSHED.put(foe, now);
		// Once a second, a chain of glowing qi from each of them to the foe's chest (it lingers a while), and the crush.
		Vec3 chest = foe.position().add(0, foe.getBbHeight() * 0.55, 0);
		for (PuppetEntity p : ring) {
			Vec3 from = p.position().add(0, p.getBbHeight() * 0.6, 0), step = chest.subtract(from).scale(1 / 5.0);
			for (int k = 1; k < 5; k++) {
				Vec3 at = from.add(step.scale(k));
				level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
		double blows = 0;
		for (PuppetEntity p : ring) blows += p.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float crush = (float) (blows * Puppetry.FORMATION_CRUSH);
		if (crush > 0 && foe.hurt(level.damageSources().mobAttack(ring.get(0)), crush)) {
			level.sendParticles(ParticleTypes.CRIT, foe.getX(), foe.getY(0.5), foe.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
			level.playSound(null, foe.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5f, 0.5f);
		}
	}

	private PuppetFormations() {}
}
