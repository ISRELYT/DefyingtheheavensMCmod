package com.example.defyingtheheavens;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * An element spell's qi harm (ModDamageTypes#qi), carrying the realm of the spell that dealt it, so a ward of the element that
 * overcomes it can tell whether it is strong enough to stop it outright (SpellAuras#wardFactor).
 */
public class QiDamageSource extends DamageSource {
	private final QiElement element;
	private final Realm realm;

	public QiDamageSource(Holder<DamageType> type, Entity direct, Entity attacker, QiElement element, Realm realm) {
		super(type, direct, attacker);
		this.element = element;
		this.realm = realm;
	}

	public QiElement element() { return element; }

	/** The realm of the spell that dealt it. */
	public Realm realm() { return realm; }
}
