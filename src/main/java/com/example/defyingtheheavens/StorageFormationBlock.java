package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.List;

/**
 * A storage formation of one tier (StorageFormations): right-click opens it to its owner and those they trust, and sets a stranger of its
 * seal's realm to forcing it; sneaking with an empty hand its owner packs it up, its space kept in the item (StorageFormationItem).
 * Unbreakable (StorageFormations: no pickaxe, blast or spell).
 */
public class StorageFormationBlock extends BaseEntityBlock {
	private final StorageFormations.Tier tier;

	public StorageFormationBlock(StorageFormations.Tier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public StorageFormations.Tier tier() { return tier; }

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageFormationBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.STORAGE_FORMATION, StorageFormationBlockEntity::serverTick);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof StorageFormationBlockEntity formation) {
			formation.placedBy(player, stack);
		}
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof StorageFormationBlockEntity formation)) return InteractionResult.PASS;
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (sp.isShiftKeyDown() && sp.getMainHandItem().isEmpty()) {
			packUp(sp, formation);
			return InteractionResult.CONSUME;
		}
		StorageFormations.use(sp, formation);
		return InteractionResult.CONSUME;
	}

	/** Its owner packs it up: the block gone, the item (its space within) in their hand. A treasury's, no one's to take. */
	private static void packUp(ServerPlayer player, StorageFormationBlockEntity formation) {
		boolean own = player.getUUID().equals(formation.getOwner()) || player.isCreative();
		if (!own || formation.getOwner() == null && !player.isCreative()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_STORAGE_NOT_YOURS).withStyle(ChatFormatting.RED), true);
			return;
		}
		Level level = player.level();
		BlockPos pos = formation.getBlockPos();
		ItemStack item = StorageFormationItem.of(formation.tier());
		if (formation.spaceId() != null) StorageFormationItem.setSpace(item, formation.spaceId());
		level.removeBlock(pos, false);
		player.setItemInHand(InteractionHand.MAIN_HAND, item);
		level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0f, 1.2f);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
		StorageFormationItem.describe(tier, stack, lines);
	}
}
