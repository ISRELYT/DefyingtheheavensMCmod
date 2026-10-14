package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * The heaven path's methods (see HeavenMethods): one for each of the five elements of qi in each realm, thirty in all, cultivated
 * by the living from Qi Refining up to the method's own realm, nine layers to each realm (MethodLayers), each layer opening as
 * their heaven realm reaches the stage it stands for. Each brings two spells, at the third and seventh layers of its own realm,
 * and every layer strengthens its element's passive. Saved by id: add new ones at the end.
 */
public enum HeavenMethod implements StringRepresentable {
	WOOD_QI_REFINING("green_sprout_breathing_manual", QiElement.WOOD, Realm.QI_REFINING),
	WOOD_FOUNDATION_BUILDING("coiling_vine_scripture", QiElement.WOOD, Realm.FOUNDATION_BUILDING),
	WOOD_CORE_FORMATION("ancient_cypress_art", QiElement.WOOD, Realm.CORE_FORMATION),
	WOOD_NASCENT_SOUL("primeval_forest_canon", QiElement.WOOD, Realm.NASCENT_SOUL),
	WOOD_HEAVENLY_BEING("eternal_spring_sutra", QiElement.WOOD, Realm.HEAVENLY_BEING),
	WOOD_FOUR_AXIS("heaven_bearing_world_tree_classic", QiElement.WOOD, Realm.FOUR_AXIS),
	WATER_QI_REFINING("morning_dew_gathering_art", QiElement.WATER, Realm.QI_REFINING),
	WATER_FOUNDATION_BUILDING("clear_fount_cleansing_manual", QiElement.WATER, Realm.FOUNDATION_BUILDING),
	WATER_CORE_FORMATION("nine_bend_river_record", QiElement.WATER, Realm.CORE_FORMATION),
	WATER_NASCENT_SOUL("moon_mirror_lake_sutra", QiElement.WATER, Realm.NASCENT_SOUL),
	WATER_HEAVENLY_BEING("heavenly_river_scripture", QiElement.WATER, Realm.HEAVENLY_BEING),
	WATER_FOUR_AXIS("boundless_four_seas_canon", QiElement.WATER, Realm.FOUR_AXIS),
	FIRE_QI_REFINING("ember_kindling_art", QiElement.FIRE, Realm.QI_REFINING),
	FIRE_FOUNDATION_BUILDING("furnace_heart_manual", QiElement.FIRE, Realm.FOUNDATION_BUILDING),
	FIRE_CORE_FORMATION("crimson_crucible_classic", QiElement.FIRE, Realm.CORE_FORMATION),
	FIRE_NASCENT_SOUL("vermilion_bird_sutra", QiElement.FIRE, Realm.NASCENT_SOUL),
	FIRE_HEAVENLY_BEING("golden_crow_great_sun_scripture", QiElement.FIRE, Realm.HEAVENLY_BEING),
	FIRE_FOUR_AXIS("heaven_burning_phoenix_nirvana_canon", QiElement.FIRE, Realm.FOUR_AXIS),
	EARTH_QI_REFINING("rammed_earth_manual", QiElement.EARTH, Realm.QI_REFINING),
	EARTH_FOUNDATION_BUILDING("thousand_layer_stone_record", QiElement.EARTH, Realm.FOUNDATION_BUILDING),
	EARTH_CORE_FORMATION("five_sacred_peaks_classic", QiElement.EARTH, Realm.CORE_FORMATION),
	EARTH_NASCENT_SOUL("yellow_dragon_earth_vein_canon", QiElement.EARTH, Realm.NASCENT_SOUL),
	EARTH_HEAVENLY_BEING("sovereign_earth_myriad_bearing_sutra", QiElement.EARTH, Realm.HEAVENLY_BEING),
	EARTH_FOUR_AXIS("four_pillars_upholding_heaven_scripture", QiElement.EARTH, Realm.FOUR_AXIS),
	METAL_QI_REFINING("whetstone_edge_manual", QiElement.METAL, Realm.QI_REFINING),
	METAL_FOUNDATION_BUILDING("hundred_forged_lodestone_art", QiElement.METAL, Realm.FOUNDATION_BUILDING),
	METAL_CORE_FORMATION("golden_thunder_core_sutra", QiElement.METAL, Realm.CORE_FORMATION),
	METAL_NASCENT_SOUL("nine_bells_sword_soul_classic", QiElement.METAL, Realm.NASCENT_SOUL),
	METAL_HEAVENLY_BEING("heaven_severing_sword_scripture", QiElement.METAL, Realm.HEAVENLY_BEING),
	METAL_FOUR_AXIS("four_swords_immortal_slaying_canon", QiElement.METAL, Realm.FOUR_AXIS);

	private final String id;
	private final QiElement element;
	private final Realm realm;

	HeavenMethod(String id, QiElement element, Realm realm) {
		this.id = id;
		this.element = element;
		this.realm = realm;
	}

	public String getId() { return id; }
	public QiElement element() { return element; }
	/** The realm whose method it is, the last it carries one through (from Qi Refining). */
	public Realm realm() { return realm; }
	/** 1 for a Qi Refining method, up to 6 for a Four Axis one. */
	public int tier() { return realm.ordinal() + 1; }
	/** Its layers: nine to each realm it spans (MethodLayers), 9 for a Qi Refining method up to 54 for a Four Axis one. */
	public int layers() { return MethodLayers.layers(realm); }
	/** Its layer standing for {@code within} (1 to 9) of its own realm, where its spells come. */
	public int ownLayer(int within) { return layers() - MethodLayers.PER_REALM + within; }

	@Override
	public String getSerializedName() { return id; }

	public Component getDisplayName() { return Component.translatable(ModLang.heavenMethodKey(id)); }
	public Component getDescription() { return Component.translatable(ModLang.heavenMethodKey(id) + ".description"); }
	public Component getGains() { return Component.translatable(ModLang.heavenMethodKey(id) + ".gains"); }

	/** The method of {@code element} for {@code realm}. */
	public static HeavenMethod of(QiElement element, Realm realm) {
		for (HeavenMethod method : values()) {
			if (method.element == element && method.realm == realm) return method;
		}
		return null;
	}

	public static HeavenMethod byId(String id) {
		for (HeavenMethod method : values()) {
			if (method.id.equals(id)) return method;
		}
		return null;
	}
}
