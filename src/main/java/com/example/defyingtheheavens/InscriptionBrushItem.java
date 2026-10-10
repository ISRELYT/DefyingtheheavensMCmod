package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * The Inscription Brush paints seals of qi ink ({@link SealBlock}) on the top of a block or the side of a wall, one per use,
 * each costing a point of durability; used while sneaking, it wipes away the seal on that face instead. The ink can only be
 * seen with Qi Sense on.
 */
public class InscriptionBrushItem extends Item {
	public InscriptionBrushItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		Direction face = context.getClickedFace();
		BlockPos clicked = context.getClickedPos();
		BlockPos target = clicked.relative(face);

		if (player != null && player.isShiftKeyDown()) {
			BlockPos seal = level.getBlockState(clicked).getBlock() instanceof SealBlock ? clicked
					: level.getBlockState(target).getBlock() instanceof SealBlock ? target : null;
			if (seal == null) return InteractionResult.PASS;
			if (!level.isClientSide) {
				level.removeBlock(seal, false);
				level.playSound(null, seal, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8f, 0.7f);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}

		if (face == Direction.DOWN || level.getBlockState(clicked).getBlock() instanceof SealBlock) return InteractionResult.PASS;
		BlockState existing = level.getBlockState(target);
		if (!existing.isAir() && !(existing.canBeReplaced() && existing.getFluidState().isEmpty())) return InteractionResult.FAIL;
		BlockState seal = ModBlocks.SEAL.defaultBlockState().setValue(SealBlock.FACE, face.getOpposite());
		if (!seal.canSurvive(level, target)) return InteractionResult.FAIL;
		if (!level.isClientSide) {
			level.setBlock(target, seal, Block.UPDATE_ALL);
			level.gameEvent(player, GameEvent.BLOCK_PLACE, target);
			level.playSound(null, target, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.9f, 1.3f);
			if (player != null) {
				context.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
				if (!CultivationManager.forPlayer(player).isAbilityActive(Ability.QI_SENSE)) {
					player.displayClientMessage(Component.translatable(ModLang.MSG_SEAL_UNSEEN), true);
				}
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.BRUSH_TOOLTIP).withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable(ModLang.BRUSH_TOOLTIP_ERASE).withStyle(ChatFormatting.DARK_GRAY));
	}
}
