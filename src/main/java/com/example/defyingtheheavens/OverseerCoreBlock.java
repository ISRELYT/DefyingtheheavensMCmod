package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Overseer Core (puppet phase 3, the linked army; the design: "It stores your diagrams and records where your puppets' lines were cut or
 * overheated in real fights. That shows up as a wear map on the diagram, so you can see what to fix. Puppets get better because you redraw
 * them, not on their own"): a block of green jade with a watching eye, crafted. Whoever sets it down is its keeper; it holds their Circuit
 * Diagrams and writes on each its design's wear (OverseerCoreBlockEntity).
 */
public class OverseerCoreBlock extends BaseEntityBlock {
	public OverseerCoreBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof OverseerCoreBlockEntity core) core.setOwner(player);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof OverseerCoreBlockEntity core) || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
		if (!core.mayUse(sp)) {
			sp.displayClientMessage(Component.translatable(ModLang.OVERSEER_NOT_YOURS, core.ownerName()).withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.CONSUME;
		}
		if (core.owner() == null) core.setOwner(sp);
		core.writeWear();
		sp.openMenu(core);
		return InteractionResult.CONSUME;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof OverseerCoreBlockEntity core) Containers.dropContents(level, pos, core);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.OVERSEER_TOOLTIP).withStyle(ChatFormatting.GRAY));
	}

	@Override
	@SuppressWarnings("deprecation")
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new OverseerCoreBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.OVERSEER_CORE, (l, pos, s, core) -> core.tick());
	}
}
