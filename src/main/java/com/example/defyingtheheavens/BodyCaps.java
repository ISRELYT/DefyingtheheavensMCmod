package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.RangedAttributeAccessor;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Vanilla holds max health to 1,024 and attack damage to 2,048; a cultivator's body goes far past both once past Four Axis (a True
 * Immortal's health is some 57,600 at Early: the user's ask, 2026-10-22, to lift the cap). Both are raised to {@link #MAX} when the
 * mod starts, on the server and the client alike (each works an attribute's value out for itself). Armour, toughness and knockback
 * resistance keep vanilla's caps: they turn no more of a blow past them.
 */
public final class BodyCaps {
	/** The new cap on both: far past what any body reaches. */
	public static final double MAX = 10_000_000;

	public static void lift() {
		lift(Attributes.MAX_HEALTH);
		lift(Attributes.ATTACK_DAMAGE);
	}

	private static void lift(Attribute attribute) {
		if (attribute instanceof RangedAttribute ranged && ranged.getMaxValue() < MAX) ((RangedAttributeAccessor) ranged).dth$setMaxValue(MAX);
	}

	private BodyCaps() {}
}
