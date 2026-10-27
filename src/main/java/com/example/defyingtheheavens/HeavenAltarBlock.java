package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The Heaven Altar: a carved stone offering table with a bronze incense burner, where the Ritual to the Heavens is performed
 * (see {@link HeavenRitual}). Right-click to open its offering tray (and hear how the heavens would weigh the ritual here);
 * sneak and right-click with an empty hand to begin. How grand the altar round it is, and how high it stands, are judged by
 * {@link HeavenAltar} and shown on its screen (HeavenAltarMenu). Breaking it spills the tray.
 */
public class HeavenAltarBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(
			box(1, 0, 1, 15, 2, 15),     // plinth
			box(2.5, 2, 2.5, 13.5, 9, 13.5), // body
			box(0.5, 9, 0.5, 15.5, 11, 15.5), // table top
			box(5, 11, 5, 11, 15, 11));  // the incense burner

	public HeavenAltarBlock(Properties properties) { super(properties); }

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HeavenAltarBlockEntity(pos, state); }

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

	@Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

	/** No way through (not a whole block, vanilla took it for open ground: a walker would push against it, as at a Formation Core). */
	@Override
	@SuppressWarnings("deprecation")
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) { return false; }

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof HeavenAltarBlockEntity altar)) return InteractionResult.PASS;
		// Vanilla only lets a sneaking player use a block with both hands empty, so this is "sneak and right-click, empty-handed".
		if (player.isSecondaryUseActive()) {
			HeavenRitual.begin(sp, pos);
			return InteractionResult.CONSUME;
		}
		if (HeavenRitual.isAltarInUse(level, pos)) {
			sp.displayClientMessage(Component.translatable(ModLang.MSG_RITUAL_ALTAR_BUSY), true);
			return InteractionResult.CONSUME;
		}
		sp.openMenu(altar); // the offering ring, and how the heavens would weigh a ritual here (HeavenAltarMenu)
		return InteractionResult.CONSUME;
	}

	/** Incense smoke curling up from the burner. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) return;
		level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.15, pos.getY() + 0.95,
				pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.15, 0, 0.02, 0);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof HeavenAltarBlockEntity altar) Containers.dropContents(level, pos, altar);
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
		Tooltips.add(lines, Component.translatable(ModLang.ALTAR_TOOLTIP).withStyle(ChatFormatting.GRAY));
		Tooltips.add(lines, Component.translatable(ModLang.ALTAR_TOOLTIP_BEGIN).withStyle(ChatFormatting.DARK_GRAY));
	}
}
