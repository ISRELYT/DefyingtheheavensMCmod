package com.example.defyingtheheavens;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * The contract for cultivators that aren't players (NPCs, see {@link CultivatorNpc}). Players are always cultivators,
 * through their {@link PlayerCultivation}; every other entity is a mortal unless its class implements this.
 * <p>
 * A cultivator NPC shows as a cultivator in the Consciousness Domain (red outline, realm label) and is compared by realm and
 * stage under Realm Suppress, like a player.
 */
public interface CultivatorEntity {
	/** The realm it can wield where it stands (what Realm Suppress weighs). */
	Realm getCultivationRealm();

	Stage getCultivationStage();

	/** What a consciousness domain senses: lowered further by a stronger cultivator's pressure. */
	default Realm getDisplayedRealm() { return getCultivationRealm(); }

	default Stage getDisplayedStage() { return getCultivationStage(); }

	/** Never opened its meridians: weighed and sensed as a mortal, whatever its class. */
	default boolean isMortal() { return false; }

	/** Hidden by a Concealment Barrier: no domain senses it. */
	default boolean isConcealed() { return false; }

	/**
	 * A stronger cultivator's Realm Suppress started, changed or lifted ({@code stages} and {@code penalty} both 0). The
	 * attribute penalty is applied for you; override to also lower the NPC's own displayed realm, qi or techniques.
	 */
	default void onRealmPressure(int stages, double penalty) {
	}

	/** Players and {@link CultivatorEntity} NPCs that aren't mortal; everything else (vanilla mobs, villagers) is a mortal. */
	static boolean isCultivator(Entity entity) {
		return entity instanceof Player || entity instanceof CultivatorEntity cultivator && !cultivator.isMortal();
	}
}
