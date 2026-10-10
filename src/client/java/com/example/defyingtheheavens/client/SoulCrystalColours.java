package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.SoulCrystalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Soul Crystal's colour: the viewer's own cultivation, smoky slate at Qi Refining warming through blue to a pale,
 * celestial white-gold at Four Axis, blending a little further with each stage. A Leyline sequence cue is gold.
 */
final class SoulCrystalColours {
	//                                  QR        FB        CF        NS        HB        FA
	private static final int[] REALM = {0x5C6280, 0x7088B8, 0x90B0DA, 0xA8C4F0, 0xD0DCFF, 0xFFF4DE};
	private static final int CUE = 0xFFD27A;

	static int of(BlockState state) {
		if (state.hasProperty(SoulCrystalBlock.GLOW) && state.getValue(SoulCrystalBlock.GLOW) == SoulCrystalBlock.GLOW_CUE) return CUE;
		PlayerCultivation c = ClientCultivationData.get();
		if (c == null || c.isMortal()) return REALM[0];
		int tier = c.getRealm().ordinal();
		if (tier >= REALM.length - 1) return REALM[REALM.length - 1];
		return mix(REALM[tier], REALM[tier + 1], c.getStage().ordinal() / 4.0f);
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return r << 16 | g << 8 | bl;
	}

	private SoulCrystalColours() {}
}
