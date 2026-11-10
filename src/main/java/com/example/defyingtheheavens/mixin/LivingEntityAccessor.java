package com.example.defyingtheheavens.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether one is holding jump: a ridden beast's rider climbs on its back with it (DemonBeast's flight, SpiritBonds), or hops a ground beast. */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("jumping")
	boolean dth$isJumping();
}
