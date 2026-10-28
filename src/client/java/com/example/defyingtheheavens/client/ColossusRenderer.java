package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Colossus;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * A legend made vast by its art (Colossus), drawn as its own item's model scaled up, full bright (qi made solid): the Xuanyuan Sword falling
 * point first out of the sky onto where its art lands, ever faster, then sinking slowly into the land ({@link Colossus#FALL}); the Sea-Fixing
 * Needle grown to a mountain coming down upright the same way ({@link Colossus#SLAM}); or the Needle stretched out from its wielder's hand
 * along a line to where it strikes, and drawn back ({@link Colossus#STRETCH}).
 */
public class ColossusRenderer extends EntityRenderer<Colossus> {
	/** A held item's model lies on the diagonal of its square: its length some 1.41 of a block. */
	static final float DIAGONAL = 1.41f;
	/** How deep a fallen one's point is in the land at its moment (a share of its length), and how much more of it has sunk by its end. */
	static final float SUNK = 0.1f, SINKS = 0.45f;
	/** A stretched Needle's breadth beside a held one's. */
	static final float STRETCH_BREADTH = 1.8f;
	/**
	 * The turn about z that stands an item's model on its point (the upper right of its square), straight down: its "fixed" view turns it
	 * half round first, its point then at the upper left (as LegendStoneRenderer's; at -135 the first tour's lay flat).
	 */
	static final float POINT_DOWN = 135;

	private final ItemRenderer items;

	public ColossusRenderer(EntityRendererProvider.Context context) {
		super(context);
		items = context.getItemRenderer();
	}

	@Override
	public void render(Colossus colossus, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		ItemStack item = colossus.item();
		if (item.isEmpty()) return;
		float age = colossus.tickCount + partialTick;
		// Drawn about the entity (where its art lands): where it comes from, beside it.
		Vec3 from = colossus.from().subtract(colossus.getPosition(partialTick));
		poseStack.pushPose();
		if (colossus.mode() == Colossus.STRETCH) stretched(colossus, from, age, poseStack);
		else fallen(colossus, from, age, poseStack);
		items.renderStatic(item, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffers, colossus.level(),
				colossus.getId());
		poseStack.popPose();
		super.render(colossus, yaw, partialTick, poseStack, buffers, light);
	}

	/** Falling point first from {@code from} onto its place, ever faster, then sinking slowly. */
	private static void fallen(Colossus colossus, Vec3 from, float age, PoseStack poseStack) {
		float size = colossus.size(), length = DIAGONAL * size;
		float t = Math.min(1, age / colossus.impact()), fall = t * t;
		float after = Math.max(0, age - colossus.impact()) / Math.max(1, colossus.lifetime() - colossus.impact());
		double y = from.y * (1 - fall) + length / 2 - SUNK * length - SINKS * length * after;
		poseStack.translate(from.x * (1 - fall), y, from.z * (1 - fall));
		poseStack.mulPose(Axis.YP.rotationDegrees(colossus.getId() * 37 % 360)); // each turned its own way
		poseStack.mulPose(Axis.ZP.rotationDegrees(POINT_DOWN));
		poseStack.scale(size, size, size);
	}

	/** Stretched out from the hand ({@code from}) toward its place, as far as it has reached by now, then drawn back. */
	private static void stretched(Colossus colossus, Vec3 from, float age, PoseStack poseStack) {
		Vec3 line = from.scale(-1);
		double full = line.length();
		if (full < 1.0e-3) return;
		Vec3 way = line.scale(1 / full);
		float impact = colossus.impact(), out = age <= impact ? age / impact : Math.max(0, 1 - (age - impact) / Math.max(1, colossus.lifetime() - impact));
		double length = Math.max(0.6, full * out);
		Vec3 middle = from.add(way.scale(length / 2));
		poseStack.translate(middle.x, middle.y, middle.z);
		poseStack.mulPose(new Quaternionf().rotationTo(0, 1, 0, (float) way.x, (float) way.y, (float) way.z));
		poseStack.scale(STRETCH_BREADTH, (float) (length / DIAGONAL), STRETCH_BREADTH);
		poseStack.mulPose(Axis.ZP.rotationDegrees(POINT_DOWN)); // its length, on the diagonal of its square, along the line
	}

	@Override
	public ResourceLocation getTextureLocation(Colossus colossus) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
