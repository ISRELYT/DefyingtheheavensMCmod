package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.FruitAura;
import com.example.defyingtheheavens.SpiritStoneItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Spirit stones glow, and the higher the grade the more: a soft halo in the grade's colour around the stone (in a slot, in
 * hand, on the ground, in an item frame), and the stone lit from within so it still shows in the dark. A Low stone barely
 * gleams; Supreme and Immortal stones shine, and slowly breathe. Hooked into item rendering by ItemRendererMixin; the halo
 * is the fruit aura's additive glow ({@link FruitAuraRenderType}).
 */
public final class SpiritStoneGlow {
	/** Halo radius per grade (Low to Immortal, then Immortal Jade), in model pixels. In a slot the largest just fills its 16 pixels. */
	private static final float[] RADIUS = {4.2f, 4.5f, 4.8f, 5.1f, 5.33f, 7.0f};
	/** Halo brightness at its centre, per grade. */
	private static final float[] BRIGHTNESS = {0.16f, 0.26f, 0.36f, 0.46f, 0.58f, 0.5f};
	/** The light a stone gives itself, per grade (block light, 0-15): it is never drawn darker than this. */
	private static final int[] SELF_LIGHT = {4, 7, 10, 13, 15, 15};
	/** One slow breath of the glow, per grade, in ticks (0: steady). */
	private static final int[] BREATH_TICKS = {0, 0, 0, 90, 60, 80};
	/** The breaths' clock wraps at this many milliseconds, a whole number of every breath, so it stays small enough for a float. */
	private static final long CLOCK_WRAP_MS = 360_000L;
	/** The stone's centre in its model, in blocks: it lies flat, about 4 pixels tall. */
	private static final float CENTRE_X = 0.5f, CENTRE_Y = 2.0f / 16, CENTRE_Z = 0.5f;
	/** Immortal Jade's: a disc standing in the middle of its model, as a flat item's sprite does. */
	private static final float JADE_CENTRE_Y = 0.5f;
	private static final int SEGMENTS = 20;

	/** {@code light} to draw {@code stack} with: a spirit stone's block light raised to its grade's own. */
	public static int light(ItemStack stack, int light) {
		if (!(stack.getItem() instanceof SpiritStoneItem stone)) return light;
		int self = SELF_LIGHT[stone.grade().ordinal()];
		return LightTexture.block(light) >= self ? light : LightTexture.pack(self, LightTexture.sky(light));
	}

	/**
	 * Draws a spirit stone's halo, with {@code poseStack} set up the way its model is about to be drawn (display transform
	 * applied, origin at the model's corner). The halo faces the viewer, however the stone is turned.
	 */
	public static void draw(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers) {
		if (!(stack.getItem() instanceof SpiritStoneItem stone)) return;
		// How far the halo reaches past the stone: a slot has no room to spare; in the world it spreads well beyond.
		float spread, strength = 1.0f;
		RenderType type = FruitAuraRenderType.GLOW;
		switch (context) {
			case GUI -> {
				spread = 1.0f;
				strength = 1.6f; // the stone fills most of the slot, so only the halo's faint outer rim shows around it
				type = FruitAuraRenderType.GLOW_ON_MAIN;
			}
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
				spread = 1.2f;
				strength = 0.75f; // right in front of your eyes
				type = FruitAuraRenderType.GLOW_ON_MAIN;
			}
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> spread = 2.2f;
			case FIXED -> spread = 1.5f;
			case GROUND -> {
				spread = 2.0f;
				// A dropped stack is drawn as up to five stones, each with its own halo: a bigger pile glows a little more, not five times.
				strength = (float) Math.pow(copies(stack.getCount()), -0.7);
			}
			default -> {
				return;
			}
		}
		int grade = stone.grade().ordinal();
		float alpha = BRIGHTNESS[grade] * strength;
		if (BREATH_TICKS[grade] > 0) {
			float clock = (Util.getMillis() % CLOCK_WRAP_MS) / 50.0f;
			alpha *= 0.82f + 0.18f * FruitAura.swell(clock, BREATH_TICKS[grade]);
		}
		// Facing the viewer: keep where the stone's centre lands and how much its model is scaled, and drop the turn.
		Matrix4f pose = poseStack.last().pose();
		boolean jade = stone.grade() == SpiritStoneItem.Grade.IMMORTAL_JADE;
		Vector3f centre = pose.transformPosition(CENTRE_X, jade ? JADE_CENTRE_Y : CENTRE_Y, CENTRE_Z, new Vector3f());
		float radius = RADIUS[grade] / 16 * spread * pose.getScale(new Vector3f()).x();
		Matrix4f facing = new Matrix4f().translation(centre);
		int colour = stone.grade().color();
		float r = (colour >> 16 & 0xFF) / 255.0f, g = (colour >> 8 & 0xFF) / 255.0f, b = (colour & 0xFF) / 255.0f;
		VertexConsumer consumer = buffers.getBuffer(type);
		for (int i = 0; i < SEGMENTS; i++) {
			// A wedge from the centre (bright) to the rim (clear).
			vertex(consumer, facing, 0, 0, r, g, b, alpha);
			vertex(consumer, facing, 0, 0, r, g, b, alpha);
			vertex(consumer, facing, cos(i + 1) * radius, sin(i + 1) * radius, r, g, b, 0);
			vertex(consumer, facing, cos(i) * radius, sin(i) * radius, r, g, b, 0);
		}
	}

	/** How many stones ItemEntityRenderer draws for a dropped stack of {@code count}. */
	private static int copies(int count) {
		return count > 48 ? 5 : count > 32 ? 4 : count > 16 ? 3 : count > 1 ? 2 : 1;
	}

	private static float cos(int segment) {
		return Mth.cos((float) (segment * Math.PI * 2 / SEGMENTS));
	}

	private static float sin(int segment) {
		return Mth.sin((float) (segment * Math.PI * 2 / SEGMENTS));
	}

	private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float y, float r, float g, float b, float a) {
		consumer.vertex(pose, x, y, 0).color(r, g, b, a).endVertex();
	}

	private SpiritStoneGlow() {}
}
