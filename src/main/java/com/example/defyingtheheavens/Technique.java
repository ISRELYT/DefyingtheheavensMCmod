package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Techniques (the user's ask, 2026-10-23): ways of the cultivator learned from a manual (TechniqueManualItem), not awakened by a realm
 * as the realm's abilities are. Once read, one is known for good (PlayerCultivation#knowsTechnique), but only wielded by one who
 * stands at its realm ({@link #wieldable}). Their manuals are kept in the sects' masters' halls and libraries, and anyone who knows
 * one may write it out again (InscriptionBrushItem, on a Blank Manual), for whoever has lost theirs or never had it.
 * Saved by id: add new ones at the end.
 */
public enum Technique {
	/** A second body split off one's own (Clones): one's realm, half one's strength, played as oneself while the main body sits on (Ability#CLONE). */
	CLONE("clone", Realm.NASCENT_SOUL),
	/**
	 * Universe in a Sleeve (Sleeves; the user's ask of 2026-10-26): others carried in one's sleeve wherever one goes, up through the Spatial
	 * Gap, the Void Breach and the Immortal Gate whatever their realm, for twice the spatial pressure for each (Ability#SLEEVE). From
	 * Heavenly Being, the first realm that tears its own way into the gap (the user's pick).
	 */
	SLEEVE("universe_in_a_sleeve", Realm.HEAVENLY_BEING);

	private final String id;
	private final Realm realm;

	Technique(String id, Realm realm) {
		this.id = id;
		this.realm = realm;
	}

	public String getId() { return id; }

	/** The realm one must stand at to learn it and to wield it (its Early stage). */
	public Realm realm() { return realm; }

	public int requiredRank() { return PlayerCultivation.rank(realm, Stage.EARLY); }

	/** Whether {@code c} stands at its realm: either path's, as wielded (TwoPaths#higherRank); never a mortal. */
	public boolean wieldable(PlayerCultivation c) {
		return !c.isMortal() && TwoPaths.higherRank(c, true) >= requiredRank();
	}

	public Component getDisplayName() { return Component.translatable(ModLang.techniqueKey(id)); }

	public Component getDescription() { return Component.translatable(ModLang.techniqueKey(id) + ".description"); }

	public static Technique byId(String id) {
		for (Technique technique : values()) if (technique.id.equals(id)) return technique;
		return null;
	}

	public static Technique byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
