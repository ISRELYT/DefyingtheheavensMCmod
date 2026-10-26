package com.example.defyingtheheavens;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * What hangs in the air about a primordial beast (PrimordialBeast#tick, the client's: particles only, nothing the world keeps): the True
 * Phoenix's fire streaming from its wings and its tail, and the ember it burns down to, an orb of flame; the True Dragon's storm, dark
 * clouds rolling over it and sparks along its coils; the Kun's wake where its back breaks the sea of clouds, and the mist it spouts; the
 * Peng's wingtips trailing cloud; the Primordial Chaos's chaos churning off it.
 */
final class PrimordialLooks {
	private PrimordialLooks() {}

	static void tick(PrimordialBeast beast) {
		Level level = beast.level();
		if (level.getNearestPlayer(beast, PrimordialBeast.FX_REACH) == null) return;
		RandomSource random = beast.getRandom();
		double x = beast.getX(), y = beast.getY(), z = beast.getZ();
		float yaw = beast.yBodyRot * Mth.DEG_TO_RAD;
		// Facing +z at yaw 0: ahead of it (-sin, cos), across it to one side (cos, sin).
		Vec3 ahead = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw)), across = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
		switch (beast.kind()) {
			case TRUE_PHOENIX -> {
				if (beast.isEmber()) {
					// An orb of fire where it fell about the bird curled in it (BeastModels draws it small), great motes of fire about small
					// flames (seen however far: mote), sparks rising out of it.
					for (int i = 0; i < 5; i++) {
						Vec3 at = sphere(random, 4.2);
						float heat = random.nextFloat();
						mote(level, new DustParticleOptions(new Vector3f(1.0f, 0.3f + heat * 0.45f, 0.05f), 2.5f + heat * 1.5f),
								x + at.x, y + 2.2 + at.y, z + at.z, 0, 0.02, 0);
					}
					for (int i = 0; i < 10; i++) {
						Vec3 at = sphere(random, 3.2);
						mote(level, ParticleTypes.FLAME, x + at.x, y + 2.2 + at.y, z + at.z, at.x * -0.02, at.y * -0.02, at.z * -0.02);
					}
					if (random.nextInt(3) == 0) mote(level, ParticleTypes.LAVA, x, y + 2.2, z, 0, 0, 0);
					mote(level, ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 3, y + 6, z + (random.nextDouble() - 0.5) * 3, 0, 0.08, 0);
					return;
				}
				// Fire along its wings' edges (some forty blocks across) and streaming from its tail.
				for (int i = 0; i < 4; i++) {
					double out = (random.nextDouble() * 2 - 1) * 20;
					Vec3 at = across.scale(out).add(ahead.scale(-1.5 - random.nextDouble() * 3));
					mote(level, ParticleTypes.FLAME, x + at.x, y + 4.2 + random.nextDouble() * 1.5, z + at.z, 0, 0.02, 0);
				}
				Vec3 tail = ahead.scale(-(12 + random.nextDouble() * 14));
				mote(level, ParticleTypes.FLAME, x + tail.x, y + 2.8 + random.nextDouble() * 2, z + tail.z, 0, 0, 0);
				if (random.nextInt(4) == 0) mote(level, ParticleTypes.LAVA, x + tail.x, y + 3, z + tail.z, 0, 0, 0);
			}
			case TRUE_DRAGON -> {
				// Its storm: dark cloud rolling over the length of it (its body trailing some seventy blocks behind its head), and now and then
				// a spark along its coils.
				Vec3 middle = ahead.scale(-30);
				for (int i = 0; i < 6; i++) {
					double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * 40;
					mote(level, ParticleTypes.LARGE_SMOKE, x + middle.x + Math.cos(a) * r, y + 18 + random.nextDouble() * 12, z + middle.z + Math.sin(a) * r,
							(random.nextDouble() - 0.5) * 0.05, 0, (random.nextDouble() - 0.5) * 0.05);
				}
				if (random.nextInt(2) == 0) {
					Vec3 at = ahead.scale(-random.nextDouble() * 66);
					mote(level, ParticleTypes.ELECTRIC_SPARK, x + at.x, y + 3 + random.nextDouble() * 5, z + at.z, 0, 0, 0);
				}
			}
			case KUN_PENG -> {
				if (beast.isPeng()) {
					if (random.nextInt(2) == 0) {
						for (int side : new int[] {1, -1}) {
							Vec3 tip = across.scale(side * 28);
							mote(level, ParticleTypes.CLOUD, x + tip.x, y + 6.5, z + tip.z, 0, 0, 0);
						}
					}
					return;
				}
				// Its back breaking the clouds: a wake along it at their top (its head some fifteen blocks ahead of where it is, its tail
				// some thirty behind: BeastModels#KUN_SET_BACK), and now and then a spout of mist from its crown.
				double top = ModDimensions.IMMORTAL_CLOUD_Y;
				if (beast.getBoundingBox().maxY < top - 1.5) return;
				for (int i = 0; i < 3; i++) {
					Vec3 at = ahead.scale(14 - random.nextDouble() * 42).add(across.scale((random.nextDouble() - 0.5) * 6));
					mote(level, ParticleTypes.CLOUD, x + at.x, top + 0.3, z + at.z, 0, 0.02, 0);
				}
				if (beast.tickCount % 60 < 6) {
					Vec3 crown = ahead.scale(9);
					for (int i = 0; i < 4; i++) {
						mote(level, ParticleTypes.CLOUD, x + crown.x, top + 0.5, z + crown.z, (random.nextDouble() - 0.5) * 0.15, 0.5 + random.nextDouble() * 0.3,
								(random.nextDouble() - 0.5) * 0.15);
					}
				}
			}
			case PRIMORDIAL_CHAOS -> {
				// The chaos it is made of, churning off it: great grey-violet motes and smoke about its sack (some twenty-five blocks across),
				// drifting round it, and now and then a spark of the five colours it was banded in.
				double middle = y + beast.getBbHeight() * 0.55;
				for (int i = 0; i < 4; i++) {
					Vec3 at = sphere(random, 16);
					mote(level, CHAOS, x + at.x, middle + at.y, z + at.z, -at.z * 0.004, 0.01, at.x * 0.004);
				}
				if (random.nextInt(2) == 0) {
					Vec3 at = sphere(random, 14);
					mote(level, ParticleTypes.LARGE_SMOKE, x + at.x, middle + at.y, z + at.z, 0, 0.03, 0);
				}
				if (random.nextInt(4) == 0) {
					Vec3 at = sphere(random, 13);
					mote(level, FIVE_COLOURS[random.nextInt(FIVE_COLOURS.length)], x + at.x, middle + at.y, z + at.z, 0, 0.02, 0);
				}
			}
			default -> {
			}
		}
	}

	/** The Primordial Chaos's churn: grey-violet; and the five colours it was banded in, azure, crimson, yellow, white and black. */
	private static final DustParticleOptions CHAOS = new DustParticleOptions(new Vector3f(0.52f, 0.43f, 0.66f), 4.0f);
	private static final DustParticleOptions[] FIVE_COLOURS = {new DustParticleOptions(new Vector3f(0.2f, 0.5f, 0.85f), 2.5f),
			new DustParticleOptions(new Vector3f(0.85f, 0.2f, 0.15f), 2.5f), new DustParticleOptions(new Vector3f(0.9f, 0.72f, 0.2f), 2.5f),
			new DustParticleOptions(new Vector3f(0.92f, 0.9f, 0.86f), 2.5f), new DustParticleOptions(new Vector3f(0.1f, 0.08f, 0.12f), 2.5f)};

	/**
	 * A particle drawn however far it is from whoever sees it (one added the plain way is dropped past 32 blocks from the camera, and these
	 * bodies are fought from further: the Phoenix's ember showed none of its fire at eighty, tour 15).
	 */
	private static void mote(Level world, ParticleOptions options, double x, double y, double z, double dx, double dy, double dz) {
		world.addParticle(options, true, x, y, z, dx, dy, dz);
	}

	/** A point in a ball of {@code r} about nothing. */
	private static Vec3 sphere(RandomSource random, double r) {
		Vec3 v;
		do {
			v = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
		} while (v.lengthSqr() > 1);
		return v.scale(r);
	}
}
