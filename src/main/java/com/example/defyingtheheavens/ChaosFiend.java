package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Chaos Fiend (phase 3c of the realm plan, 2026-10-26; BeastKind#CHAOS_FIEND): a drifting wraith of the Chaos Frontier's chaos, grey-violet
 * smoke and broken stone, long clawed arms, white-fire eyes, shards of stone circling it (its look: BeastModels). It comes only in the
 * Frontier (ChaosFrontier#spawnTick), the stronger the deeper (ChaosFrontier#fiendRanks); it flies, circles its prey loosing chaos bolts and
 * dives to claw it (DemonBeastGoals' CHAOS). It leaves alone whoever stands a realm or more above it, as a beast does (DemonBeast#farAbove),
 * and whoever stands in the First Flame's light, where it burns away itself (FirstFlame). The storm is its own: nothing of it harms it.
 * Slain it leaves no core, blood or hide, only what chaos sets hard: {@link #ORIGIN_CHANCE} a Chaos Origin Stone, {@link #FRAGMENT_CHANCE}
 * a Dao Fragment of any Law; hanging where it fell (out over the sea of clouds they would be lost).
 */
public class ChaosFiend extends DemonBeast {
	public static final float ORIGIN_CHANCE = 0.4f, FRAGMENT_CHANCE = 0.05f;

	public ChaosFiend(EntityType<? extends Monster> type, Level level) {
		super(type, level);
		// Chaos weighs nothing: it hangs where it is, even before it first flies (a flier falls till its first flight's order; out over the
		// sea of clouds the first came fell through them into the void, 2026-10-26 tour).
		setNoGravity(true);
		// It seeks its prey across the open sky (DemonBeast#notice): the search reaches past where it notices them.
		net.minecraft.world.entity.ai.attributes.AttributeInstance range = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
		if (range != null) range.setBaseValue(CHAOS_NOTICE + 8);
	}

	/** Never what stands in the First Flame's light. */
	@Override
	public boolean hunts(LivingEntity target) {
		return super.hunts(target) && !FirstFlame.inLight(target.level().dimension(), target.position());
	}

	/** In the First Flame's light it burns away. */
	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (tickCount % 10 == 0 && level() instanceof ServerLevel level && FirstFlame.inLight(level.dimension(), position())) FirstFlame.burnAway(level, this);
		// Its prey gone into the light, it lets it be.
		LivingEntity target = getTarget();
		if (target != null && tickCount % 10 == 5 && FirstFlame.inLight(target.level().dimension(), target.position())) setTarget(null);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return source.is(ModDamageTypes.CHAOS_STORM) || super.isInvulnerableTo(source);
	}

	/** No core, blood or hide: a Chaos Origin Stone, now and then; a Dao Fragment, seldom. */
	@Override
	protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
		if (random.nextFloat() < ORIGIN_CHANCE) spawnAtLocation(new ItemStack(ModItems.CHAOS_ORIGIN_STONE));
		if (random.nextFloat() < FRAGMENT_CHANCE) spawnAtLocation(DaoFragmentItem.of(Law.values()[random.nextInt(Law.values().length)]));
	}

	/** What it leaves hangs where it fell, glowing, a long while. */
	@Override
	public ItemEntity spawnAtLocation(ItemStack stack, float offsetY) {
		ItemEntity item = super.spawnAtLocation(stack, offsetY);
		if (item != null) {
			item.setNoGravity(true);
			item.setDeltaMovement(Vec3.ZERO);
			item.setExtendedLifetime();
			item.setGlowingTag(true);
		}
		return item;
	}

	/** Smoke trails off it as it drifts (the client's). */
	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide && random.nextInt(3) == 0) {
			level().addParticle(ParticleTypes.LARGE_SMOKE, getRandomX(0.5), getY() + random.nextDouble() * getBbHeight() * 0.6, getRandomZ(0.5), 0, -0.02, 0);
		}
	}
}
