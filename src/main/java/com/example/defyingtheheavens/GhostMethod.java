package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The ghost methods (鬼修功法), the first cultivation methods: each cultivated on ghostly qi through its layers (see
 * GhostMethods for how). Saved and synced by id, never by ordinal; new methods go at the end all the same.
 * <p>
 * The <b>foundations</b> carry the ghost path, a cultivation of its own beside the ordinary one (GhostMethods#ghostRank), from
 * Qi Refining up to their own realm ({@link #realm}): nine layers to each realm (MethodLayers, {@link #layers}), so the
 * Breathing Art carries it through Qi Refining alone and the Six Paths Reincarnation Canon all the way to Four Axis. The
 * deeper the rarer. Each one's spells come in its own (last) realm:
 * <ul>
 *   <li>Netherworld Breathing Art (冥息诀), Qi Refining: a bigger ghostly qi pool, a ghost's hearts, the living's stay; Ghost
 *       Eye at the third layer, Ghostly Wail at the fifth, Netherworld Veil at the sixth.</li>
 *   <li>Yellow Springs Marrow Art (黄泉炼髓功), Foundation Building: Yellow Springs Tide at the third layer of its Foundation
 *       Building, Marrow Ward at the seventh.</li>
 *   <li>Ghost Core Canon (鬼丹经), Core Formation, the Ghost Kings' own: Ghost Core Burst, King's Decree.</li>
 *   <li>Nine Nether Soul Sutra (九幽魂经), Nascent Soul: Soul Rend, Nine Nether Step.</li>
 *   <li>Yama's Judgement Scripture (阎罗判经), Heavenly Being: Yama's Judgement, Chains of Hell.</li>
 *   <li>Six Paths Reincarnation Canon (六道轮回经), Four Axis: Wheel of Samsara, Six Paths Annihilation.</li>
 * </ul>
 * And two methods of their own, beside the foundations:
 * <ul>
 *   <li>Ghost Fire Scripture (鬼火经), offence: a ghost's blows harder; Ghost Fire at the first layer, Ghost Fire Bolt at the
 *       fourth, the fire leaping to a second foe at the seventh.</li>
 *   <li>Wandering Soul Steps (游魂步), movement: a ghost swifter; Yin Wind Steps at the first layer, Soul Dash at the second,
 *       a ghost's own flight at the fourth.</li>
 * </ul>
 * How often a method's stele turns up, against the others': {@link #steleWeight} across the Netherworld, {@link #peakWeight} on
 * the Nine Nether Peaks, where alone the deepest foundations are carved (MethodSteleFeature).
 */
public enum GhostMethod implements StringRepresentable {
	NETHERWORLD_BREATHING("netherworld_breathing", 600, 600, Realm.QI_REFINING),
	GHOST_FIRE_SCRIPTURE("ghost_fire_scripture", 300, 300, null),
	WANDERING_SOUL_STEPS("wandering_soul_steps", 200, 200, null),
	YELLOW_SPRINGS_MARROW("yellow_springs_marrow", 150, 200, Realm.FOUNDATION_BUILDING),
	GHOST_CORE_CANON("ghost_core_canon", 10, 40, Realm.CORE_FORMATION),
	NINE_NETHER_SOUL_SUTRA("nine_nether_soul_sutra", 0, 20, Realm.NASCENT_SOUL),
	YAMA_JUDGEMENT("yama_judgement", 0, 8, Realm.HEAVENLY_BEING),
	SIX_PATHS_CANON("six_paths_canon", 0, 3, Realm.FOUR_AXIS);

	private final String id;
	private final int steleWeight, peakWeight;
	private final Realm realm;

	GhostMethod(String id, int steleWeight, int peakWeight, Realm realm) {
		this.id = id;
		this.steleWeight = steleWeight;
		this.peakWeight = peakWeight;
		this.realm = realm;
	}

	public String getId() { return id; }

	/** How often its stele turns up across the Netherworld, against the others' (0: never there). */
	public int steleWeight() { return steleWeight; }

	/** ...and on the Nine Nether Peaks. */
	public int peakWeight() { return peakWeight; }

	/** The realm a foundation carries cultivation up to (from Qi Refining), or null for a method of its own (fire, steps). */
	public Realm realm() { return realm; }

	public boolean isFoundation() { return realm != null; }

	/** A foundation's place in the line: 1 for the Breathing Art up to 6 for Four Axis's; 0 for the others. */
	public int tier() { return realm == null ? 0 : realm.ordinal() + 1; }

	/** Its layers: a foundation nine to each realm it spans (MethodLayers), the fire and the steps nine. */
	public int layers() { return realm == null ? GhostMethods.LAYERS : MethodLayers.layers(realm); }

	/** A foundation's layer standing for {@code within} (1 to 9) of its own realm, where its spells come. */
	public int ownLayer(int within) { return realm == null ? within : (layers() - MethodLayers.PER_REALM) + within; }

	/** The foundation that carries {@code realm}, or null. */
	public static GhostMethod foundationOf(Realm realm) {
		for (GhostMethod method : values()) {
			if (method.realm == realm) return method;
		}
		return null;
	}

	/** The foundation that takes over where this one ends, or null (the last, or not a foundation). */
	public GhostMethod next() {
		return realm == null || realm.ordinal() + 1 >= Realm.values().length ? null : foundationOf(Realm.byIndex(realm.ordinal() + 1));
	}

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
