package com.example.defyingtheheavens.client.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A key binding's count of presses not yet taken, for KeyMappingMixin to add to a mod binding sharing its key. */
@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Accessor("clickCount")
	int dth$getClickCount();

	@Accessor("clickCount")
	void dth$setClickCount(int count);
}
