package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
 * each costing a point of durability; used while sneaking, it wipes away the seal on that face instead. Either is a soft
 * brush stroke to the ear (vanilla's brushing sound, quiet; lower when wiping). The ink can only be seen with Qi Sense on.
 * With a Blank Manual in the other hand, it writes out a technique or a spell its holder knows instead ({@link #use}); with Talisman Paper in
 * the other hand, it draws talismans (Talismans: a pattern picked, then the use held to draw it).
 */
public class InscriptionBrushItem extends Item {
	public InscriptionBrushItem(Properties properties) {
		super(properties);
	}

	/**
	 * With a Blank Manual in the other hand, the brush writes out a technique or spell its holder knows (TechniqueManualItem#offerInscription:
	 * what to write is picked from a list) instead of painting. With Talisman Paper there, it draws the pattern it is set to (held for
	 * Talismans#DRAW_TICKS, finished in {@link #finishUsingItem}); with none set yet, or sneaking, the patterns are shown to pick from.
	 */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack brush = player.getItemInHand(hand);
		if (Talismans.paperIn(player, hand)) {
			if (Talismans.chosen(brush) == null || player.isSecondaryUseActive()) {
				if (player instanceof ServerPlayer sp) Talismans.offer(sp);
				return InteractionResultHolder.sidedSuccess(brush, level.isClientSide);
			}
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(brush);
		}
		if (!blankInOtherHand(player, hand)) return InteractionResultHolder.pass(brush);
		if (player instanceof ServerPlayer sp) TechniqueManualItem.offerInscription(sp);
		return InteractionResultHolder.sidedSuccess(brush, level.isClientSide);
	}

	private static boolean blankInOtherHand(Player player, InteractionHand hand) {
		return player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND).is(ModItems.BLANK_MANUAL);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		// Drawing a talisman (use), not painting a seal, while Talisman Paper is in the other hand.
		if (player != null && Talismans.paperIn(player, context.getHand())) return InteractionResult.PASS;
		// Writing out a manual, not painting a seal, while a Blank Manual is in the other hand.
		if (player != null && blankInOtherHand(player, context.getHand())) {
			if (player instanceof ServerPlayer sp) TechniqueManualItem.offerInscription(sp);
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		Direction face = context.getClickedFace();
		BlockPos clicked = context.getClickedPos();
		BlockPos target = clicked.relative(face);

		if (player != null && player.isShiftKeyDown()) {
			BlockPos seal = level.getBlockState(clicked).getBlock() instanceof SealBlock ? clicked
					: level.getBlockState(target).getBlock() instanceof SealBlock ? target : null;
			if (seal == null) return InteractionResult.PASS;
			if (!level.isClientSide) {
				level.removeBlock(seal, false);
				level.playSound(null, seal, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.6f, 0.8f); // a stroke wiping it away
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
			level.playSound(null, target, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.6f, 1.1f); // a soft brush stroke
			if (player != null) {
				context.getItemInHand().hurtAndBreak(1, player, p -> p.broadcastBreakEvent(context.getHand()));
				if (!CultivationManager.forPlayer(player).isQiSenseOn()) {
					player.displayClientMessage(Component.translatable(ModLang.MSG_SEAL_UNSEEN), true);
				}
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** A talisman's drawing: the use held this long, the brush's stroke. */
	@Override
	public int getUseDuration(ItemStack stack) {
		return Talismans.DRAW_TICKS;
	}

	@Override
	public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
		return net.minecraft.world.item.UseAnim.BRUSH;
	}

	@Override
	public void onUseTick(Level level, net.minecraft.world.entity.LivingEntity user, ItemStack stack, int left) {
		if (!level.isClientSide && left % 10 == 0) level.playSound(null, user.blockPosition(), SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.5f, 1.2f);
	}

	/** The drawing held to its end: the talisman drawn (Talismans#draw), the brush a stroke more worn there. */
	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity user) {
		if (user instanceof ServerPlayer sp) Talismans.draw(sp, sp.getUsedItemHand());
		return stack;
	}

	/**
	 * Painting a talisman at a crafting table (the Yang Fire Talisman) gives the brush back one use more worn, or nothing if that
	 * was its last; a plain crafting remainder would hand back a fresh one.
	 */
	@Override
	public ItemStack getRecipeRemainder(ItemStack stack) {
		ItemStack worn = stack.copyWithCount(1);
		worn.setDamageValue(worn.getDamageValue() + 1);
		return worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.BRUSH_TOOLTIP).withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable(ModLang.BRUSH_TOOLTIP_ERASE).withStyle(ChatFormatting.DARK_GRAY));
	}
}
