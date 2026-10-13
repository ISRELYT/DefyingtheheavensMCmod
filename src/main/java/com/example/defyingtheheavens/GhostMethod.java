package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The ghost methods (鬼修功法), the first cultivation methods: each cultivated on ghostly qi through nine layers (see
 * GhostMethods for how). Saved and synced by id, never by ordinal; new methods go at the end all the same.
 * <ul>
 *   <li>Netherworld Breathing Art (冥息诀), the foundation: a bigger ghostly qi pool, a ghost's hearts, the living's stay; Ghost
 *       Eye at the third layer, Ghostly Wail at the fifth, Netherworld Veil at the sixth.</li>
 *   <li>Ghost Fire Scripture (鬼火经), offence: a ghost's blows harder; Ghost Fire at the first layer, Ghost Fire Bolt at the
 *       fourth, the fire leaping to a second foe at the seventh.</li>
 *   <li>Wandering Soul Steps (游魂步), movement: a ghost swifter; Yin Wind Steps at the first layer, Soul Dash at the second,
 *       a ghost's own flight at the fourth.</li>
 * </ul>
 * The stronger the method, the rarer its stele ({@link #steleWeight}).
 */
public enum GhostMethod implements StringRepresentable {
	NETHERWORLD_BREATHING("netherworld_breathing", 6),
	GHOST_FIRE_SCRIPTURE("ghost_fire_scripture", 3),
	WANDERING_SOUL_STEPS("wandering_soul_steps", 2);

	private final String id;
	private final int steleWeight;

	GhostMethod(String id, int steleWeight) {
		this.id = id;
		this.steleWeight = steleWeight;
	}

	public String getId() { return id; }

	/** How often its stele turns up, against the others'. */
	public int steleWeight() { return steleWeight; }

	@Override
	public String getSerializedName() { return id; }

	public Component getDisplayName() { return Component.translatable(ModLang.methodKey(id)); }

	public Component getDescription() { return Component.translatable(ModLang.methodKey(id) + ".description"); }

	/** What its layers bring, in a line. */
	public Component getGains() { return Component.translatable(ModLang.methodKey(id) + ".gains"); }

	public static GhostMethod byId(String id) {
		for (GhostMethod method : values()) {
			if (method.id.equals(id)) return method;
		}
		return null;
	}
}
