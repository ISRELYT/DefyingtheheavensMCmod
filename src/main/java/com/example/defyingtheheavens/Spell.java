package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Spells: acts cast at will, unlike abilities, which are switched on and left on (see Spells for casting). Each comes with a
 * layer of a method, and costs ghostly qi; after it, a while must pass before it can be cast again. Add new spells at the end:
 * their ordinals are synced (saves use their ids).
 * <ul>
 *   <li>Ghost Fire (Ghost Fire Scripture, first layer): the hands take cold fire, and the next few blows set what they strike
 *       burning with it.</li>
 *   <li>Ghost Fire Bolt (fourth layer): a bolt of it, thrown.</li>
 *   <li>Soul Dash (Wandering Soul Steps, second layer): a rush forward on the yin wind.</li>
 *   <li>Ghostly Wail (Netherworld Breathing Art, fifth layer): a cry of the dead that drives back and weakens all around.</li>
 * </ul>
 */
public enum Spell {
	/** Cost: lighting the hands; each burning blow then costs {@link Spells#GHOST_FIRE_PER_HIT} more. */
	GHOST_FIRE("ghost_fire", GhostMethod.GHOST_FIRE_SCRIPTURE, 1, 3, 2 * 20),
	GHOST_FIRE_BOLT("ghost_fire_bolt", GhostMethod.GHOST_FIRE_SCRIPTURE, 4, 8, 30),
	SOUL_DASH("soul_dash", GhostMethod.WANDERING_SOUL_STEPS, 2, 6, 3 * 20),
	GHOSTLY_WAIL("ghostly_wail", GhostMethod.NETHERWORLD_BREATHING, 5, 15, 12 * 20);

	private final String id;
	private final GhostMethod method;
	private final int layer;
	private final double cost;
	private final int cooldownTicks;

	Spell(String id, GhostMethod method, int layer, double cost, int cooldownTicks) {
		this.id = id;
		this.method = method;
		this.layer = layer;
		this.cost = cost;
		this.cooldownTicks = cooldownTicks;
	}

	public String getId() { return id; }
	public GhostMethod method() { return method; }
	public int layer() { return layer; }
	public double cost() { return cost; }
	public int cooldownTicks() { return cooldownTicks; }

	/**
	 * Whether this cultivator can cast it: the method cultivated to the spell's layer, by a ghost or a living cultivator (a
	 * living mortal has no qi to shape it, though a mortal's ghost does).
	 */
	public boolean isKnown(PlayerCultivation c) {
		NetherworldState s = c.netherworld();
		return (!c.isMortal() || s.ghost()) && s.layer(method) >= layer;
	}

	public Component getDisplayName() { return Component.translatable(ModLang.spellKey(id)); }

	public Component getDescription() { return Component.translatable(ModLang.spellKey(id) + ".description"); }

	public static Spell byId(String id) {
		for (Spell spell : values()) {
			if (spell.id.equals(id)) return spell;
		}
		return null;
	}

	public static Spell byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
