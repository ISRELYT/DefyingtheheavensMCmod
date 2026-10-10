package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * The craftsmanship of cultivator clothing ({@link ClothingItem}), Mortal to Immortal grade, in the pill grades' colours.
 * <p>
 * Protection deliberately matches vanilla armor tier for tier (Mortal leather, Spirit iron, Earth diamond, Heaven and
 * Immortal netherite), because the Heavenly Tribulations are tuned against vanilla gear: clothing is preferred for what
 * it does for cultivation, not for being a stronger shell. Each piece adds to qi gathering and to cultivation while
 * meditating. A full Immortal set gathers +60% qi, which keeps Nascent Soul flight finite in the lower realms
 * (Grand Perfection 32.5 x 1.6 = 52 qi/s against a cost of 55; see {@link Realm}); keep that in mind when retuning.
 */
public enum ClothingTier implements ArmorMaterial {
	//        id        colour                   trim      dura  helm/chest/legs/boots  tough  kb   ench  qi/piece  med/piece  boots speed
	MORTAL(  "mortal",   ChatFormatting.WHITE,       0xC9C2B0,  7, new int[] {1, 3, 2, 1}, 0.0f, 0.0f, 15, 0.03, 0.02, 0.03, () -> Ingredient.of(ItemTags.WOOL)),
	SPIRIT(  "spirit",   ChatFormatting.GREEN,       0x7FD67F, 15, new int[] {2, 6, 5, 2}, 0.0f, 0.0f, 18, 0.05, 0.04, 0.05, () -> Ingredient.of(ItemTags.WOOL)),
	EARTH(   "earth",    ChatFormatting.AQUA,        0x6FE3E3, 33, new int[] {3, 8, 6, 3}, 2.0f, 0.0f, 20, 0.08, 0.06, 0.07, () -> Ingredient.of(ModItems.SPIRIT_DEW)),
	HEAVEN(  "heaven",   ChatFormatting.LIGHT_PURPLE, 0xD08CF0, 37, new int[] {3, 8, 6, 3}, 3.0f, 0.1f, 22, 0.11, 0.09, 0.09, () -> Ingredient.of(ModItems.SPIRIT_DEW)),
	IMMORTAL("immortal", ChatFormatting.GOLD,        0xF2C14E, 45, new int[] {3, 8, 6, 3}, 3.0f, 0.1f, 25, 0.15, 0.12, 0.12, () -> Ingredient.of(ModItems.SPIRIT_DEW));

	/** Vanilla's durability per piece, before the material's multiplier. */
	private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

	private final String id;
	private final ChatFormatting color;
	private final int trimColor;
	private final int durability;
	private final int[] defense;
	private final float toughness;
	private final float knockbackResistance;
	private final int enchantability;
	private final double qiGatherPerPiece;
	private final double meditationPerPiece;
	private final double bootsSpeed;
	private final Supplier<Ingredient> repair;

	ClothingTier(String id, ChatFormatting color, int trimColor, int durability, int[] defense, float toughness, float knockbackResistance,
			int enchantability, double qiGatherPerPiece, double meditationPerPiece, double bootsSpeed, Supplier<Ingredient> repair) {
		this.id = id;
		this.color = color;
		this.trimColor = trimColor;
		this.durability = durability;
		this.defense = defense;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
		this.enchantability = enchantability;
		this.qiGatherPerPiece = qiGatherPerPiece;
		this.meditationPerPiece = meditationPerPiece;
		this.bootsSpeed = bootsSpeed;
		this.repair = repair;
	}

	public String getId() { return id; }
	/** Name colour, as for pills of the same grade. */
	public ChatFormatting getColor() { return color; }
	/** The embroidery's colour on the worn clothing and its icon. */
	public int getTrimColor() { return trimColor; }
	/** Share added to qi gathering by each piece worn (0.05 = +5%). */
	public double getQiGatherPerPiece() { return qiGatherPerPiece; }
	/** Share added to cultivation while meditating by each piece worn. */
	public double getMeditationPerPiece() { return meditationPerPiece; }
	/** Extra walking speed from boots of this grade (0.05 = +5%). */
	public double getBootsSpeed() { return bootsSpeed; }

	public boolean isLast() { return ordinal() == values().length - 1; }
	public ClothingTier next() { return isLast() ? this : values()[ordinal() + 1]; }

	public static ClothingTier byIndex(int index) {
		return values()[Math.max(0, Math.min(values().length - 1, index))];
	}

	// --- ArmorMaterial ---

	private static int slot(ArmorItem.Type type) {
		return switch (type) {
			case HELMET -> 0;
			case CHESTPLATE -> 1;
			case LEGGINGS -> 2;
			case BOOTS -> 3;
		};
	}

	@Override public int getDurabilityForType(ArmorItem.Type type) { return BASE_DURABILITY[slot(type)] * durability; }
	@Override public int getDefenseForType(ArmorItem.Type type) { return defense[slot(type)]; }
	@Override public int getEnchantmentValue() { return enchantability; }
	@Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
	@Override public Ingredient getRepairIngredient() { return repair.get(); }
	/** Never used for a texture: the clothing has its own armor renderer (client ClothingArmorRenderer). */
	@Override public String getName() { return "cultivator_" + id; }
	@Override public float getToughness() { return toughness; }
	@Override public float getKnockbackResistance() { return knockbackResistance; }
}
