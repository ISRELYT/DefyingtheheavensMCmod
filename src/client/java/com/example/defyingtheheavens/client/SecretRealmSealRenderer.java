package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.SecretRealmGateBlock;
import com.example.defyingtheheavens.SecretRealmSealBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A Seal's light (SecretRealmSealBlock, SecretRealms): while its realm stirs, a pale jade pillar of light rises from its keystone into the sky;
 * while its gate stands open, a golden one. Drawn as a beacon's beam is, wide and soft, from afar (the way there is told, never shown on a
 * map: the light is what one follows once near).
 */
public class SecretRealmSealRenderer implements BlockEntityRenderer<SecretRealmSealBlockEntity> {
	private static final float[] STIRRING = {0.62f, 0.95f, 0.78f}, OPEN = {1.0f, 0.86f, 0.42f};

	@Override
	public void render(SecretRealmSealBlockEntity seal, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
		if (seal.getLevel() == null) return;
		BlockState gate = seal.getLevel().getBlockState(seal.getBlockPos().below());
		if (!gate.is(ModBlocks.SECRET_REALM_GATE)) return;
		SecretRealmGateBlock.Phase phase = gate.getValue(SecretRealmGateBlock.PHASE);
		if (phase == SecretRealmGateBlock.Phase.SEALED) return;
		boolean open = phase == SecretRealmGateBlock.Phase.OPEN;
		BeaconRenderer.renderBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, partial, 1.0f, seal.getLevel().getGameTime(), 1, 1024,
				open ? OPEN : STIRRING, open ? 0.35f : 0.22f, open ? 0.5f : 0.32f);
	}

	@Override
	public boolean shouldRenderOffScreen(SecretRealmSealBlockEntity seal) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 512;
	}

	@Override
	public boolean shouldRender(SecretRealmSealBlockEntity seal, Vec3 camera) {
		return Vec3.atCenterOf(seal.getBlockPos()).multiply(1, 0, 1).closerThan(camera.multiply(1, 0, 1), getViewDistance());
	}
}
