package com.example.defyingtheheavens.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the cultivation lift vanilla's caps on an attribute's value (BodyCaps: max health past 1,024 and attack damage past 2,048,
 * which the realms after Four Axis go beyond).
 */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {
	@Mutable
	@Accessor("maxValue")
	void dth$setMaxValue(double value);
}
