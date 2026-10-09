package com.example.defyingtheheavens.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The weight of Realm Suppress made visible: a heavy dark-red mote that drops hard around a pressed entity, shrinking as it
 * falls, and scatters along the ground where it lands.
 */
public class SuppressionParticle extends TextureSheetParticle {
	private final SpriteSet sprites;

	protected SuppressionParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
		super(level, x, y, z);
		this.sprites = sprites;
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		this.gravity = 0.9f;
		this.friction = 0.94f;
		this.lifetime = 14 + random.nextInt(8);
		this.quadSize = 0.07f + random.nextFloat() * 0.06f;
		float shade = 0.7f + random.nextFloat() * 0.3f;
		setColor(0.5f * shade, 0.04f * shade, 0.06f * shade);
		setAlpha(0.9f);
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick() {
		super.tick();
		if (removed) return;
		setSpriteFromAge(sprites);
		float left = 1 - (float) age / lifetime;
		if (left < 0.3f) setAlpha(0.9f * left / 0.3f);
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
			return new SuppressionParticle(level, x, y, z, vx, vy, vz, sprites);
		}
	}
}
