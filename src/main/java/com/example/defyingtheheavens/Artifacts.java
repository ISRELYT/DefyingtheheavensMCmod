package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What the Artifact Forge makes (ArtifactRecipes, ArtifactForgeBlockEntity; the user's asks, 2026-10-28), the side all artifacts share:
 * each is forged at a grade (its item: ArtifactGrade) and comes out of the fire Normal, Fine or Perfect (its {@link Quality}, kept on the
 * stack: a practised forger's work, CraftMastery), which raises all its powers by a tenth or a quarter.
 */
public final class Artifacts {
	/** How well a piece was forged: its powers times {@link #power}. */
	public enum Quality {
		NORMAL("normal", 1.0, ChatFormatting.GRAY),
		FINE("fine", 1.1, ChatFormatting.AQUA),
		PERFECT("perfect", 1.25, ChatFormatting.GOLD);

		private final String id;
		private final double power;
		private final ChatFormatting colour;

		Quality(String id, double power, ChatFormatting colour) {
			this.id = id;
			this.power = power;
			this.colour = colour;
		}

		public String getId() { return id; }
		/** What its powers are worth: 1, 1.1, 1.25. */
		public double power() { return power; }
		public Component getDisplayName() { return Component.translatable(ModLang.artifactQuality(id)).withStyle(colour); }

		public static Quality byIndex(int index) {
			return values()[Math.max(0, Math.min(values().length - 1, index))];
		}
	}

	private static final String QUALITY = "ArtifactQuality";

	public static Quality quality(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? Quality.NORMAL : Quality.byIndex(tag.getByte(QUALITY));
	}

	/** {@code stack}'s powers' worth: its quality's. */
	public static double power(ItemStack stack) {
		return quality(stack).power();
	}

	/** A fresh artifact of {@code kind} at {@code grade}, forged {@code quality} (a banner of {@code element}). */
	public static ItemStack create(ArtifactKind kind, ArtifactGrade grade, Quality quality, @Nullable QiElement element) {
		ItemStack stack = new ItemStack(ModItems.artifact(kind, grade));
		if (quality != Quality.NORMAL) stack.getOrCreateTag().putByte(QUALITY, (byte) quality.ordinal());
		if (element != null && stack.getItem() instanceof ElementBannerItem) ElementBannerItem.setElement(stack, element);
		return stack;
	}

	/** {@code stack} (an artifact) of {@code quality}'s work (the Sleeping Sword: Perfect, a master's own). */
	public static ItemStack withQuality(ItemStack stack, Quality quality) {
		if (quality == Quality.NORMAL) {
			if (stack.getTag() != null) stack.getTag().remove(QUALITY);
		} else {
			stack.getOrCreateTag().putByte(QUALITY, (byte) quality.ordinal());
		}
		return stack;
	}

	/** The name shown: its own ("Spirit Flying Sword"), a Fine or Perfect piece's work before it, in its grade's colour. */
	public static Component name(Component base, ItemStack stack, ArtifactGrade grade) {
		Quality quality = quality(stack);
		MutableComponent name = quality == Quality.NORMAL ? base.copy() : Component.translatable(ModLang.ARTIFACT_QUALITY_NAME,
				Component.translatable(ModLang.artifactQuality(quality.getId())), base);
		return name.withStyle(grade.colour());
	}

	/** The lines every artifact's tooltip opens with: its grade, and a Fine or Perfect piece's work. */
	public static void describe(ItemStack stack, ArtifactGrade grade, List<Component> lines) {
		Tooltips.add(lines, Component.translatable(ModLang.ARTIFACT_GRADE_LINE, grade.getDisplayName()).withStyle(ChatFormatting.GRAY));
		Quality quality = quality(stack);
		if (quality != Quality.NORMAL) {
			Tooltips.add(lines, Component.translatable(ModLang.ARTIFACT_QUALITY_LINE, quality.getDisplayName(),
					Math.round((quality.power() - 1) * 100)).withStyle(ChatFormatting.GRAY));
		}
	}

	/**
	 * What an artifact of {@code grade} sells for, in Low stones (CultivatorTrades): a little over what goes into it, a Mortal one 8, an
	 * Immortal one three Immortal Jade.
	 */
	public static int price(ArtifactGrade grade) {
		return switch (grade) {
			case MORTAL -> 8;
			case SPIRIT -> 40;
			case EARTH -> 400;
			case HEAVEN -> 3500;
			case IMMORTAL -> 3 * SpiritStoneItem.Grade.IMMORTAL_JADE.value();
		};
	}

	/** What an artifact of {@code kind} at {@code grade} sells for: a weapon made of its element, half again its grade's price. */
	public static int price(ArtifactKind kind, ArtifactGrade grade) {
		return kind.isElemental() ? price(grade) * 3 / 2 : price(grade);
	}

	/** The price of {@code stack} if it is an artifact (as forged: its work aside), else 0. */
	public static int priceOf(ItemStack stack) {
		ArtifactGrade grade = gradeOf(stack);
		if (grade == null) return 0;
		return stack.getItem() instanceof ArtifactWeaponItem weapon ? price(weapon.kind(), grade) : price(grade);
	}

	/** The grade of {@code stack} if it is an artifact, else null. */
	public static @Nullable ArtifactGrade gradeOf(ItemStack stack) {
		if (stack.getItem() instanceof ArtifactWeaponItem weapon) return weapon.grade();
		if (stack.getItem() instanceof ElementBannerItem banner) return banner.grade();
		if (stack.getItem() instanceof QiGourdItem gourd) return gourd.grade();
		if (stack.getItem() instanceof ProtectiveJadeItem jade) return jade.grade();
		return null;
	}

	/** What a cultivator gives for {@code stack} (TalkSystem#worth), as for a slip: two thirds of its price, its work's worth with it. */
	public static double worth(ItemStack stack) {
		return priceOf(stack) * power(stack) / 1.5;
	}

	/**
	 * A Normal artifact of {@code grade}, picked by {@code random}: any kind of the first forging, a banner only of {@code element}'s, and
	 * from Spirit grade up the weapon made of {@code element} too (none of either where it is null).
	 */
	public static ItemStack any(RandomSource random, ArtifactGrade grade, @Nullable QiElement element) {
		List<ArtifactKind> kinds = new java.util.ArrayList<>();
		for (ArtifactKind kind : ArtifactKind.values()) {
			if (kind.isElemental() ? kind.element() == element && kind.has(grade) : kind != ArtifactKind.BANNER || element != null) kinds.add(kind);
		}
		ArtifactKind kind = kinds.get(random.nextInt(kinds.size()));
		return create(kind, grade, Quality.NORMAL, kind == ArtifactKind.BANNER ? element : null);
	}

	/** "35%" of {@code share} (0.35), its quality's worth in it. */
	public static long percent(double share) {
		return Math.round(share * 100);
	}

	/**
	 * Whether {@code attacker}'s blade may strike {@code other} too (a spear's pierce, a glaive's sweep): a foe, never itself, a friend
	 * (its team), its own pet, an armour stand, a player it may not harm (pvp off), the spectating or the dead.
	 */
	public static boolean mayStrike(LivingEntity attacker, LivingEntity other) {
		if (other == attacker || !other.isAlive() || other.isSpectator() || other instanceof ArmorStand || attacker.isAlliedTo(other)) return false;
		if (other instanceof TamableAnimal pet && pet.isOwnedBy(attacker)) return false;
		return !(attacker instanceof Player player && other instanceof Player target && !player.canHarmPlayer(target));
	}

	private Artifacts() {}
}
