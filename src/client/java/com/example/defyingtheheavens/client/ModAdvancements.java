package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BeastKind;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.Legend;
import com.example.defyingtheheavens.MilestoneTrigger;
import com.example.defyingtheheavens.Milestones;
import com.example.defyingtheheavens.ModBiomes;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModEntities;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.Realm;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The mod's advancements (the user's ask, 2026-10-27: "advancements like normal Minecraft but for the mod"; five themed tabs, no rewards,
 * what is done already granted on joining: Milestones), as vanilla's tabs are laid out: each tab a root and its tree, every one
 * toasted and told in chat. Read by ModAdvancementProvider (the advancements' data) and ModEnglishLangProvider (their words), so the
 * two never part.
 * <ul>
 *   <li><b>Defying the Heavens</b>: the realms, Qi Refining to Heavenly Lord, the highest of one's paths; tribulations, a physique, the
 *   Ritual, the four axes, a Law, the Five Decays.</li>
 *   <li><b>Arts and Treasures</b>: spells, Qi Sense and the flights, the soul's arts and the Clone, the treasures, the great beasts.</li>
 *   <li><b>Worlds Beyond</b>: the Spatial Gap to the Immortal Realm and the Chaos Frontier, the Great Void's lands, one's own small world.</li>
 *   <li><b>The Yellow Springs</b>: the dead and the ghost path.</li>
 *   <li><b>Sects and Cultivators</b>: talk, gifts and spars, a sect and its titles, a title of one's own, the Court.</li>
 * </ul>
 */
public final class ModAdvancements {
	/** One advancement: its id (under the mod's namespace), its parent's (null: a tab's root), how it shows, and what it asks for. */
	public record Entry(String id, String parent, ItemLike icon, FrameType frame, boolean hidden, String title, String description,
						ResourceLocation background, boolean anyOf, Map<String, CriterionTriggerInstance> criteria) {
		public ResourceLocation location() {
			return DefyingTheHeavens.id(id);
		}

		public String titleKey() {
			return "advancements." + DefyingTheHeavens.MOD_ID + "." + id.replace('/', '.') + ".title";
		}

		public String descriptionKey() {
			return "advancements." + DefyingTheHeavens.MOD_ID + "." + id.replace('/', '.') + ".description";
		}
	}

	private static final List<Entry> ALL = new ArrayList<>();

	/** Every advancement, roots first within each tab, a parent always before its children. */
	public static List<Entry> all() {
		return Collections.unmodifiableList(ALL);
	}

	/** Every advancement's title and description, by key (for ModEnglishLangProvider). */
	public static Map<String, String> english() {
		Map<String, String> english = new LinkedHashMap<>();
		for (Entry entry : ALL) {
			english.put(entry.titleKey(), entry.title());
			english.put(entry.descriptionKey(), entry.description());
		}
		return english;
	}

	// --- How the entries are written ---

	private static ResourceLocation texture(String path) {
		return path.contains(":") ? new ResourceLocation(path) : DefyingTheHeavens.id("textures/block/" + path + ".png");
	}

	private static Map<String, CriterionTriggerInstance> one(String name, CriterionTriggerInstance criterion) {
		Map<String, CriterionTriggerInstance> criteria = new LinkedHashMap<>();
		criteria.put(name, criterion);
		return criteria;
	}

	private static Map<String, CriterionTriggerInstance> milestone(String milestone) {
		return one(milestone.replace('/', '_'), MilestoneTrigger.TriggerInstance.reached(milestone));
	}

	@SafeVarargs
	private static Map<String, CriterionTriggerInstance> entered(ResourceKey<Level>... worlds) {
		Map<String, CriterionTriggerInstance> criteria = new LinkedHashMap<>();
		for (ResourceKey<Level> world : worlds) criteria.put(world.location().getPath(), ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(world));
		return criteria;
	}

	private static Map<String, CriterionTriggerInstance> holding(ItemLike... items) {
		return one("has", InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(items).build()));
	}

	private static Map<String, CriterionTriggerInstance> slain(BeastKind... kinds) {
		Map<String, CriterionTriggerInstance> criteria = new LinkedHashMap<>();
		for (BeastKind kind : kinds) {
			criteria.put(kind.getId(), KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(ModEntities.beast(kind))));
		}
		return criteria;
	}

	private static Map<String, CriterionTriggerInstance> standing(String name, LocationPredicate where) {
		return one(name, PlayerTrigger.TriggerInstance.located(where));
	}

	private static ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> structure(String id) {
		return ResourceKey.create(Registries.STRUCTURE, DefyingTheHeavens.id(id));
	}

	/** An advancement asking for all of its criteria. */
	private static void add(String id, String parent, ItemLike icon, FrameType frame, String title, String description, Map<String, CriterionTriggerInstance> criteria) {
		ALL.add(new Entry(id, parent, icon, frame, false, title, description, null, false, criteria));
	}

	/** An advancement asking for any one of its criteria. */
	private static void addAny(String id, String parent, ItemLike icon, FrameType frame, String title, String description, Map<String, CriterionTriggerInstance> criteria) {
		ALL.add(new Entry(id, parent, icon, frame, false, title, description, null, true, criteria));
	}

	/** A tab's root, on its background, asking for any one of its criteria. */
	private static void root(String id, ItemLike icon, String background, String title, String description, Map<String, CriterionTriggerInstance> criteria) {
		ALL.add(new Entry(id, null, icon, FrameType.TASK, false, title, description, texture(background), true, criteria));
	}

	/** One not shown until it is made. */
	private static void secret(String id, String parent, ItemLike icon, FrameType frame, String title, String description, Map<String, CriterionTriggerInstance> criteria) {
		ALL.add(new Entry(id, parent, icon, frame, true, title, description, null, false, criteria));
	}

	/** A realm on the ladder: the highest of one's paths reaching it. */
	private static void realm(Realm realm, String parent, ItemLike icon, FrameType frame, String title, String description) {
		add("cultivation/" + realm.name().toLowerCase(Locale.ROOT), parent, icon, frame, title, description, milestone(Milestones.realm(realm)));
	}

	static {
		// ================================================= Defying the Heavens =================================================
		root("cultivation/root", ModItems.CULTIVATION_PILL, "jade_stone", "Defying the Heavens",
				"Begin to cultivate: reach Qi Refining", milestone(Milestones.realm(Realm.QI_REFINING)));
		realm(Realm.FOUNDATION_BUILDING, "cultivation/root", ModItems.FOUNDATION_PILL, FrameType.TASK, "Foundation Building", "Lay your foundation");
		realm(Realm.CORE_FORMATION, "cultivation/foundation_building", ModItems.CORE_PILL, FrameType.TASK, "Core Formation", "Condense your golden core");
		realm(Realm.NASCENT_SOUL, "cultivation/core_formation", ModItems.SPIRIT_LOTUS, FrameType.GOAL, "Nascent Soul", "Give birth to your nascent soul");
		realm(Realm.HEAVENLY_BEING, "cultivation/nascent_soul", ModItems.SPIRIT_DEW, FrameType.TASK, "Heavenly Being", "Rise above the mortal and the mundane");
		realm(Realm.FOUR_AXIS, "cultivation/heavenly_being", ModItems.JADE, FrameType.GOAL, "Four Axis", "Stand upon the four axes of heaven and earth");
		realm(Realm.DAO_INTEGRATION, "cultivation/four_axis", ModItems.JADE_SLIP, FrameType.TASK, "Dao Integration", "Become one with the Dao");
		realm(Realm.VOID_SHATTERING, "cultivation/dao_integration", Items.ENDER_EYE, FrameType.GOAL, "Void Shattering",
				"Shatter the void: the lower realms hold you no longer");
		realm(Realm.SACRED_VESSEL, "cultivation/void_shattering", ModItems.NINE_PROVINCES_DING, FrameType.TASK, "Sacred Vessel",
				"Temper yourself into a vessel for the heavens' power");
		realm(Realm.ENTERING_NIRVANA, "cultivation/sacred_vessel", Items.TOTEM_OF_UNDYING, FrameType.GOAL, "Entering Nirvana", "Burn, and be reborn from the ashes");
		realm(Realm.TRUE_IMMORTAL, "cultivation/entering_nirvana", ModItems.IMMORTAL_JADE, FrameType.CHALLENGE, "True Immortal", "Become a True Immortal");
		realm(Realm.EARTH_IMMORTAL, "cultivation/true_immortal", ModItems.OCHRE_HUANGJING, FrameType.TASK, "Earth Immortal", "Root your immortality in the earth");
		realm(Realm.HEAVEN_IMMORTAL, "cultivation/earth_immortal", ModItems.PURPLE_LINGZHI, FrameType.TASK, "Heaven Immortal", "Raise it to the heavens");
		realm(Realm.GOLDEN_IMMORTAL, "cultivation/heaven_immortal", Items.GOLD_BLOCK, FrameType.GOAL, "Golden Immortal", "Your immortality is gold, and never tarnishes");
		realm(Realm.TAIYI_GOLDEN_IMMORTAL, "cultivation/golden_immortal", Items.ENCHANTED_GOLDEN_APPLE, FrameType.TASK, "Taiyi Golden Immortal",
				"Reach the Great Unity's gold");
		realm(Realm.PROFOUND_SOVEREIGN, "cultivation/taiyi_golden_immortal", Items.ENCHANTED_BOOK, FrameType.GOAL, "Profound Sovereign",
				"Rule over the profound mysteries");
		realm(Realm.HEAVENLY_LORD, "cultivation/profound_sovereign", Items.NETHER_STAR, FrameType.CHALLENGE, "Heavenly Lord",
				"Stand at the summit of the Dao: reach Heavenly Lord");
		add("cultivation/tribulation", "cultivation/root", Items.LIGHTNING_ROD, FrameType.TASK, "Heaven's Trial", "Survive a heavenly tribulation",
				milestone(Milestones.TRIBULATION));
		add("cultivation/immortal_tribulation", "cultivation/tribulation", Items.TRIDENT, FrameType.CHALLENGE, "The Heavens Defied",
				"Survive the tribulation into True Immortal, or one past it", milestone(Milestones.IMMORTAL_TRIBULATION));
		add("cultivation/physique", "cultivation/root", ModItems.MARROW_CLEANSING_ELIXIR, FrameType.TASK, "A Body Sent by Heaven",
				"Have a physique of the High grade or above", milestone(Milestones.HIGH_PHYSIQUE));
		add("cultivation/ritual", "cultivation/root", Items.CANDLE, FrameType.TASK, "Heard by the Heavens",
				"Have the heavens grant your Ritual to the Heavens", milestone(Milestones.RITUAL));
		add("cultivation/four_axes", "cultivation/four_axis", Items.COMPASS, FrameType.GOAL, "The Four Axes of Virtue",
				"Complete all four axes of virtue", milestone(Milestones.FOUR_AXES));
		add("cultivation/law", "cultivation/true_immortal", Items.WRITTEN_BOOK, FrameType.GOAL, "A Law Comprehended",
				"Comprehend one of the Laws of heaven and earth", milestone(Milestones.LAW));
		add("cultivation/decays", "cultivation/true_immortal", Items.WITHER_ROSE, FrameType.CHALLENGE, "The Five Decays",
				"Weather the judgement of the Five Decays", milestone(Milestones.DECAYS));

		// ================================================= Arts and Treasures ==================================================
		Map<String, CriterionTriggerInstance> beginning = new LinkedHashMap<>();
		beginning.putAll(holding(ModItems.LOW_SPIRIT_STONE, ModItems.MID_SPIRIT_STONE, ModItems.HIGH_SPIRIT_STONE, ModItems.SUPREME_SPIRIT_STONE,
				ModItems.IMMORTAL_SPIRIT_STONE));
		beginning.putAll(milestone(Milestones.HEAVEN_METHOD));
		beginning.putAll(milestone(Milestones.SPELL));
		root("arts/root", ModItems.LOW_SPIRIT_STONE, "blue_spirit_planks", "Arts and Treasures", "Hold a spirit stone, learn a heaven method or cast a spell",
				beginning);
		add("arts/immortal_stone", "arts/root", ModItems.IMMORTAL_SPIRIT_STONE, FrameType.GOAL, "An Immortal Treasure", "Obtain an Immortal Spirit Stone",
				holding(ModItems.IMMORTAL_SPIRIT_STONE));
		add("arts/first_spell", "arts/root", Items.BLAZE_POWDER, FrameType.TASK, "The First Spell", "Cast a spell", milestone(Milestones.SPELL));
		Map<String, CriterionTriggerInstance> five = new LinkedHashMap<>();
		for (QiElement element : QiElement.five()) five.putAll(milestone(Milestones.spell(element)));
		add("arts/five_phases", "arts/first_spell", ModItems.HEAVEN_FURNACE_CENSER, FrameType.GOAL, "The Five Phases",
				"Cast a heaven spell of each of the five elements", five);
		add("arts/heaven_demon", "arts/first_spell", Items.DRAGON_BREATH, FrameType.GOAL, "Heaven and Demon as One", "Cast a spell of both paths",
				milestone(Milestones.SPELL_HEAVEN_DEMON));
		add("arts/qi_sense", "arts/root", Items.SPYGLASS, FrameType.TASK, "Eyes of Qi", "Open your Qi Sense and see the world's qi",
				milestone(Milestones.QI_SENSE));
		add("arts/qi_flight", "arts/root", Items.FEATHER, FrameType.TASK, "Riding the Wind", "Fly on your qi", milestone(Milestones.QI_FLIGHT));
		add("arts/sword_flight", "arts/qi_flight", Items.IRON_SWORD, FrameType.TASK, "One Sword, Ten Thousand Li", "Ride your flying sword",
				milestone(Milestones.SWORD_FLIGHT));
		add("arts/meteor_flight", "arts/qi_flight", Items.FIRE_CHARGE, FrameType.GOAL, "Falling Star", "Streak across the sky in Meteor Flight",
				milestone(Milestones.METEOR_FLIGHT));
		add("arts/inner_realm", "arts/qi_sense", Items.ENDER_PEARL, FrameType.TASK, "The World Within", "Turn inward, into your Inner Realm",
				entered(ModDimensions.INNER_REALM));
		add("arts/soul_projection", "arts/qi_sense", Items.SOUL_LANTERN, FrameType.TASK, "The Soul Wanders", "Send your nascent soul out of your body",
				milestone(Milestones.SOUL_PROJECTION));
		add("arts/possession", "arts/soul_projection", Items.ARMOR_STAND, FrameType.GOAL, "Another's Body", "Take another being's body as your own",
				milestone(Milestones.POSSESSION));
		add("arts/clone", "arts/root", ModItems.TECHNIQUE_MANUAL, FrameType.GOAL, "Two Bodies, One Dao", "Make a Clone of yourself", milestone(Milestones.CLONE));
		secret("arts/core_detonation", "arts/root", Items.TNT, FrameType.GOAL, "A Core Shattered", "Detonate your own golden core",
				milestone(Milestones.CORE_DETONATION));
		// Its id kept from the Storage Rings' days, so who earned it keeps it (the storage formations took the rings' place, 2026-10-28).
		add("arts/storage_ring", "arts/root", ModItems.storageFormation(com.example.defyingtheheavens.StorageFormations.Tier.MID), FrameType.TASK,
				"A Sealed World", "Refine a storage formation", milestone(Milestones.STORAGE_RING));
		add("arts/thousand_year_fruit", "arts/root", ModItems.CULTIVATION_FRUIT, FrameType.TASK, "A Thousand Years Ripened",
				"Eat a Cultivation Fruit a thousand years old or more", milestone(Milestones.THOUSAND_YEAR_FRUIT));
		add("arts/sacred_vessel", "arts/immortal_stone", ModItems.JIANMU_BOUGH_VASE, FrameType.GOAL, "A Sacred Vessel", "Hold one of the five Sacred Vessels",
				holding(ModItems.JIANMU_BOUGH_VASE, ModItems.HEAVEN_FURNACE_CENSER, ModItems.NINE_PROVINCES_DING, ModItems.IMMORTAL_FORGING_BELL,
						ModItems.SEALED_OCEAN_GOURD));
		add("arts/dao_fragment", "arts/immortal_stone", ModItems.DAO_FRAGMENT, FrameType.GOAL, "A Fragment of the Dao", "Obtain a Dao Fragment",
				holding(ModItems.DAO_FRAGMENT));
		add("arts/origin_stone", "arts/dao_fragment", ModItems.CHAOS_ORIGIN_STONE, FrameType.TASK, "Before Heaven and Earth", "Obtain a Chaos Origin Stone",
				holding(ModItems.CHAOS_ORIGIN_STONE));
		add("arts/demon_core", "arts/root", ModItems.DEMON_CORE, FrameType.TASK, "The Beast's Core", "Take a demon beast's core", holding(ModItems.DEMON_CORE));
		addAny("arts/chaos_fiend", "arts/demon_core", Items.ECHO_SHARD, FrameType.TASK, "Fiends of the Frontier", "Slay a Chaos Fiend",
				slain(BeastKind.CHAOS_FIEND));
		addAny("arts/elder_beast", "arts/demon_core", ModItems.DEMON_BLOOD, FrameType.GOAL, "Elder of the Blood", "Slay one of the Great Void's elder beasts",
				slain(BeastKind.ELDER_WHITE_TIGER, BeastKind.NINE_TAILED_FOX, BeastKind.MOUNTAIN_LIZARD_KING, BeastKind.AZURE_DRAGON));
		addAny("arts/primordial_beast", "arts/elder_beast", Items.DRAGON_HEAD, FrameType.CHALLENGE, "Primordial",
				"Slay one of the Immortal Realm's primordial beasts", slain(BeastKind.KUN_PENG, BeastKind.TRUE_PHOENIX, BeastKind.TRUE_DRAGON));
		addAny("arts/primordial_chaos", "arts/primordial_beast", Items.DRAGON_EGG, FrameType.CHALLENGE, "Hundun's Remnant", "Slay the Primordial Chaos",
				slain(BeastKind.PRIMORDIAL_CHAOS));
		// The legendary weapons (Legends): heard of from the world's cultivators (or found in hand), and bonded with.
		Map<String, CriterionTriggerInstance> legends = new LinkedHashMap<>();
		for (Legend legend : Legend.values()) legends.putAll(milestone(Milestones.legendHeard(legend)));
		addAny("arts/legends", "arts/root", ModItems.legend(Legend.GANJIANG), FrameType.TASK, "Legends of the World",
				"Hear of one of the legendary weapons", legends);
		Map<String, CriterionTriggerInstance> pair = new LinkedHashMap<>();
		pair.putAll(milestone(Milestones.legendHeard(Legend.GANJIANG)));
		pair.putAll(milestone(Milestones.legendHeard(Legend.MOYE)));
		addAny("arts/legend_ganjiang_moye", "arts/legends", ModItems.legend(Legend.MOYE), FrameType.TASK, "Yang and Yin",
				"Hear of the paired swords Ganjiang and Moye", pair);
		add("arts/legend_tribulation_spear", "arts/legends", ModItems.legend(Legend.TRIBULATION_SPEAR), FrameType.GOAL, "The Tribulation Breaker",
				"Hear of mad Lei Zhen and Heaven's Tribulation Spear", milestone(Milestones.legendHeard(Legend.TRIBULATION_SPEAR)));
		add("arts/legend_xuanyuan", "arts/legends", ModItems.legend(Legend.XUANYUAN), FrameType.TASK, "The Yellow Emperor's Sword",
				"Hear of the Xuanyuan Sword, the bane of evil", milestone(Milestones.legendHeard(Legend.XUANYUAN)));
		add("arts/legend_sun_bow", "arts/legends", ModItems.legend(Legend.SUN_BOW), FrameType.TASK, "Nine Suns Fell",
				"Hear of the Sun-Shooting Bow and the dead sun it lies in", milestone(Milestones.legendHeard(Legend.SUN_BOW)));
		add("arts/legend_sea_needle", "arts/legends", ModItems.legend(Legend.SEA_NEEDLE), FrameType.GOAL, "As You Will",
				"Hear of the Sea-Fixing Needle standing over the sea of clouds", milestone(Milestones.legendHeard(Legend.SEA_NEEDLE)));
		add("arts/legend_pangu_axe", "arts/legends", ModItems.legend(Legend.PANGU_AXE), FrameType.GOAL, "Heaven Above, Earth Below",
				"Hear of Pangu's Axe in the heart of the Primordial Chaos", milestone(Milestones.legendHeard(Legend.PANGU_AXE)));
		Map<String, CriterionTriggerInstance> symbols = new LinkedHashMap<>();
		for (Legend legend : Legend.values()) if (legend.venerable() != null) symbols.putAll(milestone(Milestones.legendHeard(legend)));
		addAny("arts/legend_venerables", "arts/legends", ModItems.legend(Legend.AZURE_GLAIVE), FrameType.TASK, "The Four Symbols",
				"Hear of the Heavenly Venerables' five weapons", symbols);
		add("arts/legend_dao_duel", "arts/legend_venerables", ModItems.legend(Legend.YELLOW_DRAGON_SEAL), FrameType.CHALLENGE, "A Dao Duel Won",
				"Beat a Heavenly Venerable in a Dao Duel and take its weapon", milestone(Milestones.DUEL_WON));
		add("arts/legend_bonded", "arts/legends", Items.NETHER_STAR, FrameType.CHALLENGE, "One in All the Worlds", "Bond with a legendary weapon",
				milestone(Milestones.LEGEND_BONDED));

		// ==================================================== Worlds Beyond ====================================================
		root("worlds/root", Items.RECOVERY_COMPASS, "minecraft:textures/block/obsidian.png", "Worlds Beyond", "Leave the mortal world for a world beyond it",
				entered(ModDimensions.SPATIAL_GAP, ModDimensions.UPPER_REALM, ModDimensions.ASTRAL_VOID, ModDimensions.ASTRAL_SURFACES, ModDimensions.GREAT_VOID,
						ModDimensions.VOID_BREACH, ModDimensions.IMMORTAL_PASSAGE, ModDimensions.IMMORTAL_REALM, ModDimensions.NETHERWORLD, ModDimensions.SOUL_RIVER,
						ModDimensions.SMALL_WORLD));
		add("worlds/spatial_gap", "worlds/root", Items.OBSIDIAN, FrameType.TASK, "Tearing Space", "Enter the Spatial Gap", entered(ModDimensions.SPATIAL_GAP));
		add("worlds/jade_firmament", "worlds/spatial_gap", Items.EMERALD, FrameType.GOAL, "The Jade Firmament", "Reach the Upper Realm",
				entered(ModDimensions.UPPER_REALM));
		addAny("worlds/astral", "worlds/jade_firmament", Items.AMETHYST_CLUSTER, FrameType.TASK, "Among the Stars", "Reach the Astral Realm",
				entered(ModDimensions.ASTRAL_VOID, ModDimensions.ASTRAL_SURFACES));
		add("worlds/great_void", "worlds/jade_firmament", Items.CRYING_OBSIDIAN, FrameType.GOAL, "Shattering the Void", "Break through into the Great Void",
				entered(ModDimensions.GREAT_VOID));
		add("worlds/battlefield", "worlds/great_void", Items.SKELETON_SKULL, FrameType.TASK, "The Ancient War",
				"Stand on one of the Great Void's Ancient Battlefields", standing("battlefield", LocationPredicate.inBiome(ModBiomes.ANCIENT_BATTLEFIELD)));
		add("worlds/holy_land", "worlds/great_void", Items.QUARTZ_PILLAR, FrameType.TASK, "Holy Ground", "Set foot in a Holy Land",
				standing("holy_land", LocationPredicate.inStructure(structure("holy_land"))));
		add("worlds/demon_palace", "worlds/great_void", Items.RED_NETHER_BRICKS, FrameType.TASK, "A Palace of Beasts", "Set foot in a Demon Palace",
				standing("demon_palace", LocationPredicate.inStructure(structure("demon_palace"))));
		add("worlds/shore_of_nirvana", "worlds/great_void", Items.CAMPFIRE, FrameType.TASK, "The Shore of Nirvana",
				"Reach the shore where the Immortal Gate is awaited", standing("shore", LocationPredicate.inBiome(ModBiomes.SHORE_OF_NIRVANA)));
		add("worlds/immortal_realm", "worlds/shore_of_nirvana", Items.BEACON, FrameType.CHALLENGE, "Through the Immortal Gate", "Enter the Immortal Realm",
				entered(ModDimensions.IMMORTAL_REALM));
		add("worlds/chaos_frontier", "worlds/immortal_realm", ModItems.CHAOS_ORIGIN_STONE, FrameType.GOAL, "The Edge of Chaos", "Reach the Chaos Frontier",
				standing("chaos_frontier", LocationPredicate.inBiome(ModBiomes.CHAOS_FRONTIER)));
		add("worlds/small_world", "worlds/immortal_realm", ModItems.WORLD_SEED, FrameType.GOAL, "A World of One's Own", "Refine a small world of your own",
				milestone(Milestones.SMALL_WORLD));

		// ================================================== The Yellow Springs =================================================
		Map<String, CriterionTriggerInstance> dead = new LinkedHashMap<>(entered(ModDimensions.NETHERWORLD, ModDimensions.SOUL_RIVER));
		dead.putAll(milestone(Milestones.GHOST));
		dead.putAll(milestone(Milestones.GHOST_PATH));
		root("springs/root", ModItems.OTHER_SHORE_FLOWER, "yin_jade_block", "The Yellow Springs", "Come among the dead, or set out on the ghost path", dead);
		add("springs/ghost", "springs/root", ModItems.YIN_JADE, FrameType.TASK, "Death Is Not the End", "Die, and walk on as one of the dead",
				milestone(Milestones.GHOST));
		add("springs/soul_river", "springs/root", ModItems.NETHERWORLD_LOTUS, FrameType.TASK, "The River of Souls", "Ride the Soul River",
				entered(ModDimensions.SOUL_RIVER));
		add("springs/nine_nether_peaks", "springs/root", Items.BASALT, FrameType.TASK, "The Nine Nether Peaks",
				"Climb where the Netherworld's ghostly qi is thickest", standing("peaks", LocationPredicate.inBiome(ModBiomes.NINE_NETHER_PEAKS)));
		add("springs/hungry_ghost", "springs/root", ModItems.PEACH_WOOD_SWORD, FrameType.TASK, "Hungry No More", "Slay a Hungry Ghost",
				one("hungry_ghost", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(ModEntities.HUNGRY_GHOST))));
		add("springs/ghost_path", "springs/root", ModItems.GHOST_JADE_SLIP, FrameType.TASK, "The Ghost Path", "Begin the ghost path",
				milestone(Milestones.GHOST_PATH));
		add("springs/ghost_spell", "springs/ghost_path", Items.SOUL_TORCH, FrameType.TASK, "A Spell of the Dead", "Cast a ghost spell",
				milestone(Milestones.SPELL_GHOST));
		add("springs/both_paths", "springs/ghost_path", ModItems.YIN_GATHERING_PILL, FrameType.GOAL, "Two Paths, One Body",
				"Walk the heaven path and the ghost path both, among the living", milestone(Milestones.BOTH_PATHS));
		add("springs/ghost_king", "springs/ghost_path", Items.GOLDEN_HELMET, FrameType.CHALLENGE, "King of the Dead",
				"Be among the dead with a ghost path of Core Formation or higher: a Ghost King",
				milestone(Milestones.GHOST_KING));
		add("springs/meng_po", "springs/ghost", Items.SUSPICIOUS_STEW, FrameType.TASK, "Meng Po's Soup", "Drink, and forget", milestone(Milestones.SOUP));
		add("springs/reborn", "springs/ghost", ModItems.REBIRTH_PILL, FrameType.GOAL, "The Wheel Turns", "Be born again", milestone(Milestones.REBORN));

		// ================================================ Sects and Cultivators ================================================
		root("sects/root", Items.WRITABLE_BOOK, "formation_core_lacquer", "Sects and Cultivators", "Talk with a cultivator", milestone(Milestones.TALK));
		add("sects/gift", "sects/root", ModItems.GINSENG, FrameType.TASK, "A Token of Esteem", "Give a cultivator a gift they accept", milestone(Milestones.GIFT));
		add("sects/sworn", "sects/gift", ModItems.PEACH_PIT, FrameType.GOAL, "Sworn Friend", "Become a cultivator's sworn friend", milestone(Milestones.SWORN));
		add("sects/spar", "sects/root", Items.WOODEN_SWORD, FrameType.TASK, "A Friendly Bout", "Win a spar", milestone(Milestones.SPAR_WON));
		add("sects/joined", "sects/root", ModItems.SECT_PASS, FrameType.TASK, "Disciple of the Sect", "Join a sect", milestone(Milestones.SECT_JOINED));
		add("sects/elder", "sects/joined", Items.LECTERN, FrameType.GOAL, "Elder", "Become an Elder of your sect", milestone(Milestones.SECT_ELDER));
		add("sects/master", "sects/elder", Items.BELL, FrameType.CHALLENGE, "Sect Master", "Lead your sect as its master", milestone(Milestones.SECT_MASTER));
		add("sects/mission", "sects/joined", ModItems.MISSION_SLIP, FrameType.TASK, "Duty to the Sect", "See a sect mission through", milestone(Milestones.MISSION_DONE));
		add("sects/every_mission", "sects/mission", ModItems.SEALED_LETTER, FrameType.GOAL, "Of Every Service", "See a sect mission of every kind through",
				milestone(Milestones.MISSION_EVERY_KIND));
		add("sects/pillar", "sects/mission", ModBlocks.MISSION_BOARD, FrameType.CHALLENGE, "Pillar of the Sect", "Earn 1,000 Contribution in one sect",
				milestone(Milestones.MISSION_PILLAR));
		add("sects/bloodline", "sects/root", ModItems.DEMON_BLOOD, FrameType.TASK, "Blood of Beasts", "Carry a beast's bloodline",
				milestone(Milestones.BEAST_BLOOD));
		add("sects/kneel", "sects/root", Items.GOLD_INGOT, FrameType.GOAL, "Revered", "Have a cultivator kneel before you", milestone(Milestones.KNEEL));
		add("sects/title", "sects/kneel", Items.NAME_TAG, FrameType.GOAL, "A Name Under Heaven", "Take a title of your own", milestone(Milestones.TITLE));
		add("sects/court", "sects/title", ModItems.COURT_MERIT_TOKEN, FrameType.CHALLENGE, "The Celestial Court", "Enter the service of the Celestial Court",
				milestone(Milestones.COURT));
	}

	private ModAdvancements() {}
}
