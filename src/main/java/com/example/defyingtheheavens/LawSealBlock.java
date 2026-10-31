package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.function.BiPredicate;

/**
 * A Law's seal on the door of one of the Tomb of a Fallen Immortal's halls (ImmortalTombWorks): a sheet of the Law's own light ({@link #LAW},
 * Law#byIndex; tinted on the client by its colour), solid to everyone but one who holds that Law (LawState: a level of it at least); to
 * them it is nothing. The client is told nothing: it asks its own player's Laws ({@link #CLIENT_HOLDS}: SecretRealmClient). Nothing breaks it.
 */
public class LawSealBlock extends Block {
	public static final IntegerProperty LAW = IntegerProperty.create("law", 0, Law.values().length - 1);

	/** The client's: whether {@code entity} (its own player) holds {@code law}. Set by SecretRealmClient. */
	public static volatile BiPredicate<Entity, Law> CLIENT_HOLDS = (entity, law) -> false;

	public LawSealBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LAW, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LAW);
	}

	/** {@code law}'s seal. */
	public static BlockState of(Law law) {
		return ModBlocks.LAW_SEAL.defaultBlockState().setValue(LAW, law.ordinal());
	}

	public static Law law(BlockState state) {
		return Law.byIndex(state.getValue(LAW));
	}

	/** Whether {@code entity} holds {@code law}: a player with a level of it. */
	public static boolean holds(Entity entity, Law law) {
		if (entity == null) return false;
		if (entity.level().isClientSide) return CLIENT_HOLDS.test(entity, law);
		return entity instanceof ServerPlayer p && CultivationManager.get(p).laws().level(law) >= 1;
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entity && holds(entity.getEntity(), law(state))) return Shapes.empty();
		return Shapes.block();
	}

	/** One sheet of light across its door: no faces drawn between two of them. */
	@Override
	@SuppressWarnings("deprecation")
	public boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
		return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
	}

	/** The Law's light drifting up from it. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) return;
		int c = law(state).color();
		DustParticleOptions dust = new DustParticleOptions(new Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 1.0f);
		level.addParticle(dust, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.02, 0);
	}
}
