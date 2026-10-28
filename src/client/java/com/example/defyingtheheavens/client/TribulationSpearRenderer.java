package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Heaven's Tribulation Spear (LegendaryWeaponItem) drawn as what it is (the user's word, 2026-10-28: "it shouldnt be lightning crawling
 * over a spear, it should be a bolt of heavenly tribulation itself"): a spear's length of the heavens' tribulation lightning, jagged and
 * never still (its zig-zag struck anew every {@link #RESTRIKE} ticks), widening to a blazing head and a sharp point, a fork crackling off it
 * now and then. In the tribulation's own blue, layered and glowing as TribulationLightningRenderer draws its bolts, white-hot down its
 * middle. In the hand, on Lei Zhen, on the ground, in a frame and in the inventory alike (builtin/entity: models/item/tribulation_spear.json
 * places it, held as a sword is: it lies from the lower left, its butt, to the upper right, its point, about the item's middle).
 */
public class TribulationSpearRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	/** Its length (blocks, before its display's scale), its spine's segments, how far its zig-zag strays from straight. */
	static final float LENGTH = 2.1f, JAG = 0.045f;
	static final int SEGMENTS = 14;
	/** It is struck anew this often (ticks). */
	static final int RESTRIKE = 2;
	/** Its layers, innermost first: half its shaft's breadth, and the colour (red, green, blue, alpha; added on as light is). */
	static final float[] WIDTH = {0.012f, 0.026f, 0.044f, 0.068f};
	/** How much broader it is drawn in a slot (the first tour's icon was a hair-thin line), and on the ground or in a frame. */
	static final float GUI_BROAD = 2.4f, SMALL_BROAD = 1.6f;
	static final int[][] COLOUR = {{235, 244, 255, 210}, {140, 190, 255, 120}, {38, 115, 255, 80}, {38, 115, 255, 46}};
	/** Along its length (lower left to upper right) and the two ways across it. */
	private static final float S = (float) Math.sqrt(0.5);
	private static final float[] ALONG = {S, S, 0}, ACROSS = {-S, S, 0}, OUT = {0, 0, 1};

	@Override
	public void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
		Minecraft minecraft = Minecraft.getInstance();
		long strike = minecraft.level == null ? 0 : minecraft.level.getGameTime() / RESTRIKE;
		RandomSource random = RandomSource.create(strike * 341873128712L + 132897987541L);
		float[][] spine = new float[SEGMENTS + 1][];
		float[] breadth = new float[SEGMENTS + 1];
		for (int i = 0; i <= SEGMENTS; i++) {
			float t = i / (float) SEGMENTS;
			// Straight at its ends, jagged along its shaft, steadier through its head.
			float jag = i == 0 || i == SEGMENTS ? 0 : JAG * (t > 0.78f ? 0.35f : 1.0f);
			spine[i] = at((t - 0.5f) * LENGTH, (random.nextFloat() - 0.5f) * 2 * jag, (random.nextFloat() - 0.5f) * 2 * jag);
			breadth[i] = profile(t);
		}
		// Broader where it is drawn small (in a slot, on the ground, in a frame), so it reads as a spear there too.
		float broad = mode == ItemDisplayContext.GUI ? GUI_BROAD : mode == ItemDisplayContext.GROUND || mode == ItemDisplayContext.FIXED ? SMALL_BROAD : 1.0f;
		for (int i = 0; i <= SEGMENTS; i++) breadth[i] *= broad;
		VertexConsumer glow = buffers.getBuffer(Types.SPEAR);
		Matrix4f pose = poseStack.last().pose();
		for (int layer = 0; layer < WIDTH.length; layer++) {
			for (int i = 0; i < SEGMENTS; i++) {
				segment(glow, pose, spine[i], spine[i + 1], WIDTH[layer] * breadth[i], WIDTH[layer] * breadth[i + 1], COLOUR[layer]);
			}
		}
		// A fork crackling off its shaft now and then, thinner, dying away.
		if (random.nextFloat() < 0.6f) {
			int from = 3 + random.nextInt(SEGMENTS / 2);
			float side = random.nextBoolean() ? 1 : -1, outward = (random.nextFloat() - 0.5f) * 0.8f, reach = 0.22f + random.nextFloat() * 0.18f;
			float[][] fork = new float[4][];
			fork[0] = spine[from];
			for (int k = 1; k < 4; k++) {
				float f = k / 3.0f;
				fork[k] = add(fork[0], ALONG, reach * f * 0.7f, ACROSS, side * reach * f + (random.nextFloat() - 0.5f) * 0.05f, OUT,
						outward * reach * f + (random.nextFloat() - 0.5f) * 0.05f);
			}
			for (int layer = 0; layer < WIDTH.length - 1; layer++) {
				for (int k = 0; k < 3; k++) {
					segment(glow, pose, fork[k], fork[k + 1], WIDTH[layer] * 0.55f * (1 - k / 3.0f), WIDTH[layer] * 0.55f * (1 - (k + 1) / 3.0f), COLOUR[layer]);
				}
			}
		}
	}

	/** How broad it is at {@code t} along it (0 its butt, 1 its point): a shaft, tapered at its butt, its head swelling then sharp. */
	static float profile(float t) {
		if (t < 0.05f) return 0.45f + 0.55f * t / 0.05f;
		if (t < 0.78f) return 1.0f;
		if (t < 0.86f) return 1.0f + 1.9f * (t - 0.78f) / 0.08f;
		return 2.9f * (1.0f - (t - 0.86f) / 0.14f);
	}

	/** The point {@code along} its length from its middle, strayed {@code across} and {@code out}. */
	private static float[] at(float along, float across, float out) {
		return add(new float[] {0.5f, 0.5f, 0.5f}, ALONG, along, ACROSS, across, OUT, out);
	}

	private static float[] add(float[] p, float[] a, float da, float[] b, float db, float[] c, float dc) {
		return new float[] {p[0] + a[0] * da + b[0] * db + c[0] * dc, p[1] + a[1] * da + b[1] * db + c[1] * dc, p[2] + a[2] * da + b[2] * db + c[2] * dc};
	}

	/** A length of it from {@code a} to {@code b}: four sides about it, {@code wa} and {@code wb} from its middle at either end. */
	private static void segment(VertexConsumer out, Matrix4f pose, float[] a, float[] b, float wa, float wb, int[] colour) {
		float[][] signs = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
		for (int k = 0; k < 4; k++) {
			float[] s0 = signs[k], s1 = signs[(k + 1) % 4];
			vertex(out, pose, a, wa, s0, colour);
			vertex(out, pose, a, wa, s1, colour);
			vertex(out, pose, b, wb, s1, colour);
			vertex(out, pose, b, wb, s0, colour);
		}
	}

	private static void vertex(VertexConsumer out, Matrix4f pose, float[] p, float w, float[] sign, int[] colour) {
		float x = p[0] + (ACROSS[0] * sign[0] + OUT[0] * sign[1]) * w, y = p[1] + (ACROSS[1] * sign[0] + OUT[1] * sign[1]) * w,
				z = p[2] + (ACROSS[2] * sign[0] + OUT[2] * sign[1]) * w;
		out.vertex(pose, x, y, z).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
	}

	/**
	 * Its light: vanilla's lightning (added on, both faces), but drawn straight to the screen, not into the weather's own target as the
	 * world's bolts are under Fabulous graphics (a hand's or the inventory's would never show).
	 */
	private static final class Types extends RenderType {
		static final RenderType SPEAR = create(com.example.defyingtheheavens.DefyingTheHeavens.MOD_ID + "_tribulation_spear", DefaultVertexFormat.POSITION_COLOR,
				VertexFormat.Mode.QUADS, 1536, false, true, CompositeState.builder()
						.setShaderState(RENDERTYPE_LIGHTNING_SHADER)
						.setTransparencyState(LIGHTNING_TRANSPARENCY)
						.setWriteMaskState(COLOR_WRITE)
						.setCullState(NO_CULL)
						.createCompositeState(false));

		private Types(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
			super(name, format, mode, size, crumbling, sort, setup, clear);
		}
	}
}
