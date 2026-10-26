package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.HashSet;
import java.util.Set;

/**
 * The mod's own creative tab, "Defying The Heavens": everything it adds in one place, the way a cultivator would look for
 * it. First what cultivation is done with (mats, pedestal, altar, cauldron, formation core, brush, rings), then what is
 * taken (fruit, every pill at every grade, spirit dew), herbs and what they're grown from, spirit stones and jade, the
 * blocks of the world (ores, qi veins, spirit wood, leaves), the slips and manuals (every method's, technique's and spell's),
 * a demon core of every realm, the clothing (every style, grade and piece), and last the spawn eggs. An item the list doesn't name (one added later) still turns up, at the end, so none is ever missing. The items
 * stay in vanilla's tabs too (ModItems#register, ModBlocks#register).
 */
public final class ModCreativeTab {
	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DefyingTheHeavens.id("main"),
			FabricItemGroup.builder()
					.title(Component.translatable(ModLang.CREATIVE_TAB))
					.icon(() -> CultivationFruitItem.create(1))
					.displayItems((parameters, output) -> new Filler(output).fill())
					.build());

	/** Lists the tab's items, remembering which have been, so the rest of the mod's can follow. */
	private record Filler(CreativeModeTab.Output output, Set<Item> listed) {
		Filler(CreativeModeTab.Output output) {
			this(output, new HashSet<>());
		}

		void add(ItemStack stack) {
			output.accept(stack);
			listed.add(stack.getItem());
		}

		void add(ItemLike... items) {
			for (ItemLike item : items) add(new ItemStack(item));
		}

		void fill() {
			// Cultivating
			add(ModBlocks.MEDITATION_MAT, ModBlocks.RED_MEDITATION_MAT, ModBlocks.SPIRIT_PEDESTAL, ModBlocks.HEAVEN_ALTAR,
					ModBlocks.ALCHEMY_CAULDRON, ModBlocks.FORMATION_CORE, ModItems.INSCRIPTION_BRUSH, ModItems.RING_OF_POWER,
					ModItems.RING_OF_TRANSCENDENCE);
			for (StorageRings.Grade grade : StorageRings.Grade.values()) add(StorageRingItem.of(grade));
			// Taken for qi and cultivation
			add(CultivationFruitItem.create(1));
			for (Item pill : new Item[] {ModItems.MARROW_CLEANSING_ELIXIR, ModItems.QI_GATHERING_PILL, ModItems.CULTIVATION_PILL,
					ModItems.FOUNDATION_PILL, ModItems.CORE_PILL, ModItems.LONGEVITY_PILL}) {
				for (PillGrade grade : PillGrade.values()) add(PillItem.create(pill, grade, 1));
			}
			add(ModItems.REBIRTH_PILL);
			add(ModItems.SPIRIT_DEW);
			// Herbs (a year old), and what they're grown from
			for (Item herb : new Item[] {ModItems.GINSENG, ModItems.SPIRIT_GINSENG, ModItems.LINGZHI, ModItems.PURPLE_LINGZHI,
					ModItems.HUANGJING, ModItems.OCHRE_HUANGJING, ModItems.SPIRIT_LOTUS}) {
				add(GinsengItem.create(herb, 1));
			}
			add(ModItems.GINSENG_SEEDS, ModItems.SPIRIT_GINSENG_SEEDS, ModItems.LINGZHI_SPORES, ModItems.PURPLE_LINGZHI_SPORES,
					ModItems.HUANGJING_SEEDS, ModItems.OCHRE_HUANGJING_SEEDS, ModItems.SPIRIT_LOTUS_SEEDS, ModItems.PEACH_PIT,
					ModBlocks.SPIRIT_DEW_GRASS);
			// Spirit stones and jade; the Chaos Frontier's stones and every Law's fragment
			for (SpiritStoneItem.Grade grade : SpiritStoneItem.Grade.values()) add(SpiritStoneItem.of(grade));
			add(ModItems.JADE, ModItems.CHAOS_ORIGIN_STONE);
			for (Law law : Law.values()) add(DaoFragmentItem.of(law));
			add(ModItems.WORLD_SEED);
			// The world's blocks
			add(ModBlocks.JADE_STONE, ModBlocks.JADE_ORE, ModBlocks.DEEPSLATE_JADE_ORE, ModBlocks.SPIRIT_STONE_ORE,
					ModBlocks.DEEPSLATE_SPIRIT_STONE_ORE, ModBlocks.RICH_SPIRIT_STONE_ORE, ModBlocks.QI_VEIN, ModBlocks.DEEPSLATE_QI_VEIN,
					ModBlocks.BLUE_SPIRIT_LOG, ModBlocks.BLUE_SPIRIT_WOOD, ModBlocks.BLUE_SPIRIT_PLANKS, ModBlocks.WHITE_BLOSSOM_LEAVES,
					ModBlocks.SPIRIT_PEACH_LEAVES, ModBlocks.CHAOS_ORIGIN_ORE, ModBlocks.FIRST_FLAME);
			// The Netherworld: talismans and the exorcist's sword, its herbs (a year old) and what they're grown from, Yin Jade, its
			// pills at every grade, the slips
			add(ModItems.YANG_FIRE_TALISMAN, ModItems.GHOST_QUELLING_TALISMAN, ModItems.PEACH_WOOD_SWORD, ModBlocks.RED_SPIDER_LILY);
			for (Item herb : new Item[] {ModItems.OTHER_SHORE_FLOWER, ModItems.NETHERWORLD_LOTUS}) add(GinsengItem.create(herb, 1));
			add(ModItems.OTHER_SHORE_FLOWER_SEEDS, ModItems.NETHERWORLD_LOTUS_SEEDS, ModItems.YIN_JADE, ModBlocks.YIN_JADE_ORE, ModBlocks.YIN_JADE_BLOCK);
			for (Item pill : new Item[] {ModItems.YANG_PRESERVATION_PILL, ModItems.YIN_GATHERING_PILL}) {
				for (PillGrade grade : PillGrade.values()) add(PillItem.create(pill, grade, 1));
			}
			for (GhostMethod method : GhostMethod.values()) add(GhostJadeSlipItem.of(method));
			for (HeavenMethod method : HeavenMethod.values()) add(JadeSlipItem.of(method));
			// The techniques' manuals, every spell's that can be written out (TechniqueManualItem#writable: all but both paths'), and
			// the blank one a technique or spell is written out on
			for (Technique technique : Technique.values()) add(TechniqueManualItem.of(technique));
			for (Spell spell : Spell.values()) if (!spell.isDual()) add(TechniqueManualItem.of(spell));
			add(ModItems.BLANK_MANUAL);
			// Demon cores, one of each realm (formed at its Early stage)
			for (Realm realm : Realm.values()) add(DemonCoreItem.of(PlayerCultivation.rank(realm, Stage.EARLY), null, null));
			// Each demon race's blood, of every realm (DemonBloodItem)
			for (Bloodline race : Bloodline.values()) {
				if (race.isBeast()) for (Realm realm : Realm.values()) add(DemonBloodItem.of(race, realm));
			}
			// Clothing: each path's colours, every grade, crown to boots
			for (ClothingStyle style : ClothingStyle.values()) {
				for (ClothingTier tier : ClothingTier.values()) {
					for (ArmorItem.Type type : ArmorItem.Type.values()) add(ClothingItem.create(ModItems.clothing(tier, type), style));
				}
			}
			// Spawn eggs
			add(ModItems.CULTIVATOR_SPAWN_EGG, ModItems.GHOST_CULTIVATOR_SPAWN_EGG, ModItems.HUNGRY_GHOST_SPAWN_EGG);
			// Anything of the mod's not named above.
			for (Item item : BuiltInRegistries.ITEM) {
				if (!listed.contains(item) && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(DefyingTheHeavens.MOD_ID)) add(item);
			}
		}
	}

	/** Touching this class registers the tab (after the items and blocks it lists). */
	public static void register() {}

	private ModCreativeTab() {}
}
