package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * An element banner, forged at the Artifact Forge (the user's pick, 2026-10-28): a small war banner of one element, the demon core's it
 * was forged with (a Mortal one's, its element's token: ArtifactRecipes). Held in either hand, its holder's spells of that element strike
 * harder, by its grade (5% to 20%, its work's worth with it: a Perfect Immortal banner a quarter, as a Sacred Vessel). A Sacred Vessel's
 * power and a banner's don't add up: the greater holds (HeavenSpells#law).
 */
public class ElementBannerItem extends Item {
	/** What a banner adds to its element's spells, by grade. */
	private static final double[] BOOST = {0.05, 0.08, 0.12, 0.16, 0.20};
	private static final String ELEMENT = "ArtifactElement";

	private final ArtifactGrade grade;

	public ElementBannerItem(ArtifactGrade grade, Properties properties) {
		super(properties);
		this.grade = grade;
	}

	public ArtifactGrade grade() { return grade; }

	public static void setElement(ItemStack stack, QiElement element) {
		stack.getOrCreateTag().putString(ELEMENT, element.name());
	}

	public static @Nullable QiElement element(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		if (tag == null || !tag.contains(ELEMENT)) return null;
		try {
			return QiElement.valueOf(tag.getString(ELEMENT));
		} catch (IllegalArgumentException unknown) {
			return null;
		}
	}

	/** What {@code stack} adds to its element's spells (its grade's, its work's worth with it). */
	public static double boost(ItemStack stack) {
		return stack.getItem() instanceof ElementBannerItem banner ? BOOST[banner.grade.ordinal()] * Artifacts.power(stack) : 0;
	}

	/** What {@code holder}'s spells of {@code element} are worth for the banners in its hands: the best of them, 1 with none. */
	public static float strength(LivingEntity holder, @Nullable QiElement element) {
		if (element == null) return 1f;
		double best = 1;
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = holder.getItemInHand(hand);
			if (stack.getItem() instanceof ElementBannerItem && element(stack) == element) best = Math.max(best, 1 + boost(stack));
		}
		return (float) best;
	}

	/** Its cloth's colour (the client's tint of its second layer): its element's; undyed linen where it has none. */
	public static int colour(ItemStack stack) {
		QiElement element = element(stack);
		if (element == null) return 0xE8E4DA;
		return switch (element) {
			case WOOD -> 0x3CB45A;
			case WATER -> 0x3C78E6;
			case FIRE -> 0xE64A20;
			case EARTH -> 0xC88A2B;
			case METAL -> 0xE8D070;
			case YIN -> 0x5AC8C8;
		};
	}

	/** "Spirit Banner of Fire", in its grade's colour; one with no element, its plain name. */
	@Override
	public Component getName(ItemStack stack) {
		QiElement element = element(stack);
		Component base = element == null ? super.getName(stack)
				: Component.translatable(ModLang.BANNER_NAMED, Component.translatable(ModLang.artifactGrade(grade.getId())), element.getDisplayName());
		return Artifacts.name(base, stack, grade);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
		Artifacts.describe(stack, grade, lines);
		QiElement element = element(stack);
		if (element == null) {
			Tooltips.add(lines, Component.translatable(ModLang.BANNER_NO_ELEMENT).withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		Tooltips.add(lines, Component.translatable(ModLang.BANNER_PERK, element.getDisplayName(), Artifacts.percent(boost(stack)))
				.withStyle(ChatFormatting.BLUE));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return grade == ArtifactGrade.IMMORTAL || Artifacts.quality(stack) == Artifacts.Quality.PERFECT;
	}
}
