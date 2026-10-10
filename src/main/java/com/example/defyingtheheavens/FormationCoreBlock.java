package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * A Formation Core: an altar of ritual bronze, black lacquer and jade with cinnabar posts, an azure crystal hovering over it
 * (drawn by the client's FormationCoreRenderer, spinning and burning bright while its barrier stands). Placed by a player, it
 * answers to them; right-click
 * it to open its screen (radius, on/off, qi). Power it with Qi Veins joined to it by lines of seals (see
 * {@link FormationCoreBlockEntity}). Breaking it brings its barrier down at once.
 */
public class FormationCoreBlock extends BaseEntityBlock {
	/** The altar (plinth, body, dais), its four corner posts, and the space the crystal hovers in. */
	private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 10, 16), box(1, 10, 1, 3, 14, 3), box(13, 10, 1, 15, 14, 3),
			box(1, 10, 13, 3, 14, 15), box(13, 10, 13, 15, 14, 15), box(5, 10, 5, 11, 16, 11));

	public FormationCoreBlock(Properties properties) {
		super(properties);
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FormationCoreBlockEntity(pos, state); }
	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FORMATION_CORE, FormationCoreBlockEntity::serverTick);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide && placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof FormationCoreBlockEntity core) {
			core.setOwner(player);
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof FormationCoreBlockEntity core)) return InteractionResult.PASS;
		if (!core.canControl(sp)) {
			if (core.getSect() != null) {
				sp.displayClientMessage(Component.translatable(ModLang.MSG_CORE_SECT, core.getSectName()), true);
				SectManager.onCriticalAreaBreached(sp.serverLevel(), core.getSect(), sp, pos);
			} else {
				sp.displayClientMessage(Component.translatable(ModLang.MSG_CORE_NOT_YOURS, core.getOwnerName()), true);
			}
			return InteractionResult.CONSUME;
		}
		ModPackets.sendFormationCore(sp, core);
		return InteractionResult.CONSUME;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof FormationCoreBlockEntity core) {
			core.onRemovedFromWorld(server);
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(ModLang.CORE_TOOLTIP).withStyle(ChatFormatting.GRAY));
	}
}
