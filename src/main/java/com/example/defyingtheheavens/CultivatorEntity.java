package com.example.defyingtheheavens;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * The contract for cultivators that aren't players (NPCs; none exist yet). Players are always cultivators, through their
 * {@link PlayerCultivation}; every other entity is a mortal unless its class implements this.
 * <p>
 * A cultivator NPC shows as a cultivator in the Consciousness Domain (red outline, realm label) and is compared by realm and
 * stage under Realm Suppress, like a player.
 */
public interface CultivatorEntity {
	Realm getCultivationRealm();

	Stage getCultivationStage();

	/**
	 * A stronger cultivator's Realm Suppress started, changed or lifted ({@code stages} and {@code penalty} both 0). The
	 * attribute penalty is applied for you; override to also lower the NPC's own displayed realm, qi or techniques.
	 */
	default void onRealmPressure(int stages, double penalty) {
	}

	/** Players and {@link CultivatorEntity} NPCs; everything else (vanilla mobs, villagers) is a mortal. */
	static boolean isCultivator(Entity entity) {
		return entity instanceof Player || entity instanceof CultivatorEntity;
	}
}
