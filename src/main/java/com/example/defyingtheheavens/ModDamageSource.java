package com.example.defyingtheheavens;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The mod's harm (ModDamageTypes), telling of a death as its words mean (the bug sweep of 2026-10-27). Vanilla tells a death by one who
 * dealt it with the plain message ("death.attack.X", the dealer never named in ours) and keeps ".player" for a death with no dealer but one
 * who last hurt them; and a dealer holding a renamed item turns it to ".item", which none of ours has (its key shown raw). So with a dealer,
 * here, it is ".player", naming them ("burnt away by Steve's fire qi"); without one, vanilla's own way.
 */
public class ModDamageSource extends DamageSource {
	public ModDamageSource(Holder<DamageType> type) {
		super(type);
	}

	public ModDamageSource(Holder<DamageType> type, Entity attacker) {
		super(type, attacker);
	}

	public ModDamageSource(Holder<DamageType> type, Entity direct, Entity attacker) {
		super(type, direct, attacker);
	}

	@Override
	public Component getLocalizedDeathMessage(LivingEntity victim) {
		Entity by = getEntity();
		if (by == null) return super.getLocalizedDeathMessage(victim);
		String key = "death.attack." + type().msgId();
		// One's own harm come back on oneself: the plain message (a dealer named as oneself would read oddly).
		if (by == victim) return Component.translatable(key, victim.getDisplayName(), victim.getDisplayName());
		return Component.translatable(key + ".player", victim.getDisplayName(), by.getDisplayName());
	}
}
