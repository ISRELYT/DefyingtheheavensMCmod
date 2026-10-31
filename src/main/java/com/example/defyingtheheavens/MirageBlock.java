package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BooleanSupplier;

/**
 * A mirage of the Jade Lake Mirage (MirageLakeWorks lays and renews them): the look of a thing and nothing of it. A span of a jade bridge
 * one walks onto and falls through into the lake; a spirit lotus or a vault that dissolves into mist at a touch. Nothing in it stops a
 * foot; under Qi Sense it shimmers (the client's: {@link #SENSING}), plain for what it is. Gone, the realm sets it again before it next
 * opens (RealmLedger).
 */
public class MirageBlock extends Block {
	/** What it seems to be. */
	public enum Kind { BRIDGE, LOTUS, CASKET }

	/** The client's: whether its own player's Qi Sense is open. Set by SecretRealmClient. */
	public static volatile BooleanSupplier SENSING = () -> false;

	private static final VoxelShape LOTUS = Block.box(1, 0, 1, 15, 4, 15);

	private final Kind kind;

	public MirageBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() { return kind; }

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return kind == Kind.LOTUS ? LOTUS : Shapes.block();
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	/** A lotus or a vault walked into: gone into mist. (A bridge's span is walked onto, and through.) */
	@Override
	@SuppressWarnings("deprecation")
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (kind != Kind.BRIDGE && entity instanceof Player p && !p.isSpectator()) dissolve(level, pos, p);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (kind == Kind.BRIDGE) return InteractionResult.PASS;
		dissolve(level, pos, player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (kind != Kind.BRIDGE) dissolve(level, pos, player);
	}

	/** It goes into mist before {@code who}: a sigh of wind, its place empty, they told what it was. */
	static void dissolve(Level level, BlockPos pos, Player who) {
		if (!(level instanceof ServerLevel server) || !(level.getBlockState(pos).getBlock() instanceof MirageBlock)) return;
		server.removeBlock(pos, false);
		server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
		server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.04);
		server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, 1.6f);
		if (who instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable(ModLang.MSG_MIRAGE_DISSOLVES).withStyle(ChatFormatting.GRAY), true);
	}

	/** Under Qi Sense, a shimmer of false light over it: plain for what it is. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!SENSING.getAsBoolean() || random.nextInt(2) != 0) return;
		double y = kind == Kind.LOTUS ? 0.3 : random.nextDouble();
		level.addParticle(ParticleTypes.WAX_OFF, pos.getX() + random.nextDouble(), pos.getY() + y, pos.getZ() + random.nextDouble(), 0, 0.01, 0);
	}
}
