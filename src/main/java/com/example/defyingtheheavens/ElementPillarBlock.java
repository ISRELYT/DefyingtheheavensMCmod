package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

/**
 * One of the Cave Abode's five element pillars (CaveAbodeWorks, its first trial, 2026-10-31): carved pale stone banded with its element's
 * glyph. Touched (right-click), it is lit, if it is the next in the order the elements give rise to one another (wood, fire, earth, metal,
 * water: {@link Element} in that order); out of turn, all five go dark again (CaveAbodeWorks#pillarTouched). Lit, it glows and gives off its
 * element's light. It can't be broken.
 */
public class ElementPillarBlock extends Block {
	/** The five elements in the order each gives rise to the next. */
	public enum Element implements StringRepresentable {
		WOOD("wood", 0x4FB848), FIRE("fire", 0xE8502A), EARTH("earth", 0xD8A030), METAL("metal", 0xEDE6D2), WATER("water", 0x3C8CE8);

		private final String id;
		final int colour;

		Element(String id, int colour) {
			this.id = id;
			this.colour = colour;
		}

		@Override
		public String getSerializedName() { return id; }
	}

	public static final EnumProperty<Element> ELEMENT = EnumProperty.create("element", Element.class);
	public static final BooleanProperty LIT = BooleanProperty.create("lit");

	public ElementPillarBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ELEMENT, Element.WOOD).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ELEMENT, LIT);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) CaveAbodeWorks.pillarTouched(server, pos, sp);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		int c = state.getValue(ELEMENT).colour;
		DustParticleOptions dust = new DustParticleOptions(new Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 1.2f);
		for (int i = 0; i < 2; i++) {
			level.addParticle(dust, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.1, pos.getY() + random.nextDouble() * 1.4, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.1,
					0, 0.02, 0);
		}
	}
}
