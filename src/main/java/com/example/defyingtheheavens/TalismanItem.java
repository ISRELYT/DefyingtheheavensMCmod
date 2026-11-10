package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A talisman drawn with the Inscription Brush (Talismans, TalismanPattern): used, thrown (Fire, Five Thunder, Frost, Binding: TalismanEntity) or
 * burnt on oneself (Golden Light, Divine Speed, Thousand-Li Escape, Concealment), and spent; then its pattern's own short wait (the hotbar's).
 * Its strength is its drawer's realm (written on it), at most a realm above whoever uses it; a Fine or Perfect drawing's a tenth or a quarter more.
 */
public class TalismanItem extends Item {
	private final TalismanPattern pattern;

	public TalismanItem(TalismanPattern pattern, Properties properties) {
		super(properties);
		this.pattern = pattern;
	}

	public TalismanPattern pattern() { return pattern; }

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer p)) return InteractionResultHolder.sidedSuccess(stack, true);
		if (pattern.use == TalismanPattern.Use.THROWN) Talismans.toss(p, stack);
		else if (!Talismans.burn(p, stack, pattern)) return InteractionResultHolder.fail(stack);
		p.awardStat(Stats.ITEM_USED.get(this));
		if (pattern.cooldown > 0) p.getCooldowns().addCooldown(this, pattern.cooldown);
		if (!p.getAbilities().instabuild) stack.shrink(1);
		return InteractionResultHolder.sidedSuccess(stack, false);
	}

	/** Its name, a Fine or Perfect drawing's work before it (as an artifact's). */
	@Override
	public Component getName(ItemStack stack) {
		Artifacts.Quality quality = Artifacts.quality(stack);
		Component base = super.getName(stack);
		return quality == Artifacts.Quality.NORMAL ? base
				: Component.translatable(ModLang.ARTIFACT_QUALITY_NAME, Component.translatable(ModLang.artifactQuality(quality.getId())), base);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.talismanTooltip(pattern.getId())).withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable(pattern.use == TalismanPattern.Use.THROWN ? ModLang.TOOLTIP_TALISMAN_THROW : ModLang.TOOLTIP_TALISMAN_BURN)
				.withStyle(ChatFormatting.DARK_GRAY));
		CompoundTag tag = stack.getTag();
		int rank = Talismans.drawnAt(stack);
		if (rank >= 0) {
			Object at = PlayerCultivation.rankName(CultivatorNpc.realmOf(rank), CultivatorNpc.stageOf(rank));
			lines.add(Component.translatable(ModLang.TOOLTIP_TALISMAN_DRAWN, tag.getString(Talismans.DRAWER), at).withStyle(ChatFormatting.GOLD));
			lines.add(Component.translatable(ModLang.TOOLTIP_TALISMAN_CAP).withStyle(ChatFormatting.DARK_GRAY));
		} else {
			lines.add(Component.translatable(ModLang.TOOLTIP_TALISMAN_UNSIGNED).withStyle(ChatFormatting.DARK_GRAY));
		}
		if (pattern == TalismanPattern.THOUSAND_LI_ESCAPE) {
			int reach = (int) Math.round((tag != null && tag.contains(Talismans.REACH) ? tag.getInt(Talismans.REACH) : Talismans.ESCAPE_REACH[0]) * Artifacts.power(stack));
			lines.add(Component.translatable(ModLang.TOOLTIP_TALISMAN_REACH, reach).withStyle(ChatFormatting.GRAY));
		}
		Artifacts.Quality quality = Artifacts.quality(stack);
		if (quality != Artifacts.Quality.NORMAL) {
			lines.add(Component.translatable(ModLang.ARTIFACT_QUALITY_LINE, quality.getDisplayName(), Math.round((quality.power() - 1) * 100)).withStyle(ChatFormatting.GRAY));
		}
	}
}
