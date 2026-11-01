package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * A gathering formation of one kind and tier (GatheringFormations): right-clicked with spirit stones, it takes one to burn (sneaking, the
 * whole stack), lit while they last; with anything else, it says what it does and how long it will burn. Lit, its qi (or yin) is drawn
 * in to it in motes.
 */
public class GatheringFormationBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 7, 16);
	private final GatheringFormations.Kind kind;
	private final GatheringFormations.Tier tier;

	public GatheringFormationBlock(GatheringFormations.Kind kind, GatheringFormations.Tier tier, Properties properties) {
		super(properties);
		this.kind = kind;
		this.tier = tier;
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	public GatheringFormations.Kind kind() { return kind; }
	public GatheringFormations.Tier tier() { return tier; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GatheringFormationBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.GATHERING_FORMATION, GatheringFormationBlockEntity::serverTick);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof GatheringFormationBlockEntity formation)) return InteractionResult.PASS;
		ItemStack held = sp.getItemInHand(hand);
		int each = SpiritStoneItem.valueOf(held.copyWithCount(1));
		if (each > 0) {
			int stones = sp.isShiftKeyDown() ? held.getCount() : 1;
			formation.addFuel(Math.round(each * stones * tier.ticksPerWorth()));
			if (!sp.getAbilities().instabuild) held.shrink(stones);
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0f, kind == GatheringFormations.Kind.QI ? 1.2f : 0.7f);
		}
		sp.displayClientMessage(Component.translatable(ModLang.MSG_GATHERING_STATUS, Component.translatable(getDescriptionId()), Math.round(tier.factor() * 100),
				tier.radius(), duration(formation.fuel())).withStyle(formation.fuel() > 0 ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
		return InteractionResult.CONSUME;
	}

	/** "2 h 30 min", "45 min", "out" for ticks of burning. */
	static Component duration(long ticks) {
		if (ticks <= 0) return Component.translatable(ModLang.GATHERING_OUT);
		long minutes = Math.max(1, ticks / 1200);
		return minutes >= 60 ? Component.translatable(ModLang.GATHERING_HOURS, minutes / 60, minutes % 60) : Component.translatable(ModLang.GATHERING_MINUTES, minutes);
	}

	/** Lit, its qi drawn in to it: motes from about it to its heart. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		ParticleOptions mote = kind == GatheringFormations.Kind.QI ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME;
		for (int i = 0; i < 2; i++) {
			double a = random.nextDouble() * Math.PI * 2, r = 1.5 + random.nextDouble() * tier.radius() / 3.0;
			double x = pos.getX() + 0.5 + Math.cos(a) * r, z = pos.getZ() + 0.5 + Math.sin(a) * r, y = pos.getY() + 0.5 + random.nextDouble() * 2;
			level.addParticle(mote, x, y, z, (pos.getX() + 0.5 - x) * 0.04, (pos.getY() + 0.6 - y) * 0.04, (pos.getZ() + 0.5 - z) * 0.04);
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable(kind == GatheringFormations.Kind.QI ? ModLang.TOOLTIP_QI_GATHERING : ModLang.TOOLTIP_YIN_GATHERING,
				Math.round(tier.factor() * 100), tier.radius()).withStyle(ChatFormatting.GRAY));
		// The stone that suits its tier (a Low one the Low, a Mid the Mid, a High the High) and how long it burns on one.
		net.minecraft.world.item.Item stone = tier == GatheringFormations.Tier.LOW ? ModItems.LOW_SPIRIT_STONE
				: tier == GatheringFormations.Tier.MID ? ModItems.MID_SPIRIT_STONE : ModItems.HIGH_SPIRIT_STONE;
		lines.add(Component.translatable(ModLang.TOOLTIP_GATHERING_FUEL, Component.translatable(stone.getDescriptionId()),
				Math.round(SpiritStoneItem.valueOf(new ItemStack(stone)) * tier.ticksPerWorth() / 1200.0)).withStyle(ChatFormatting.DARK_GRAY));
	}
}
