package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Additive glow for the fruit aura. The same blending as {@link RenderType#lightning()}, but it does not write depth
 * and does not cull back faces. Lightning writes depth, so each faint, mostly transparent glow quad hid everything
 * drawn behind it afterwards (other glow layers, beams, particles); up close, where the quads fill the screen, that
 * showed as see-through holes. Additive light does not need sorting or depth, so turning both off is safe.
 * <p>
 * Extends RenderType only to reach its protected render-state constants; it is never instantiated.
 */
public final class FruitAuraRenderType extends RenderType {
	public static final RenderType GLOW = create("defying_the_heavens_fruit_aura", DefaultVertexFormat.POSITION_COLOR,
			VertexFormat.Mode.QUADS, 1536, false, false,
			CompositeState.builder()
					.setShaderState(RENDERTYPE_LIGHTNING_SHADER)
					.setTransparencyState(LIGHTNING_TRANSPARENCY)
					.setWriteMaskState(COLOR_WRITE)
					.setCullState(NO_CULL)
					.setOutputState(WEATHER_TARGET)
					.createCompositeState(false));

	private FruitAuraRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling,
			boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
