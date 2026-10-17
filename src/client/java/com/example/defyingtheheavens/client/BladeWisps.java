package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellAura;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

/**
 * Blade Polish on a weapon in hand (third person: ItemInHandLayerMixin; first person: ItemInHandRendererMixin): a faint white sheen
 * along the blade and wisps of white smoke curling off it. Drawn in the item's own space (as the item renderer places it), along the
 * diagonal a sword's blade runs in its texture, from the hilt to the point.
 */
public final class BladeWisps {
	private static final float[] WHITE = {1, 1, 1}, POLISH = {0.85f, 0.92f, 1.0f};

	public static void draw(LivingEntity holder, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose, MultiBufferSource source) {
		if (stack.isEmpty() || !(stack.getItem() instanceof TieredItem || stack.getItem() instanceof TridentItem)) return;
		if (!ClientSpellAuras.has(holder, SpellAura.BLADE_POLISH)) return;
		Minecraft mc = Minecraft.getInstance();
		float partial = mc.getFrameTime();
		float shown = ClientSpellAuras.shown(holder, SpellAura.BLADE_POLISH, partial, 8, 20);
		if (shown <= 0) return;
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			draw(holder, stack, context, leftHand, pose, buffers, partial, shown);
		} finally {
			SpellBuffers.end();
		}
	}

	private static void draw(LivingEntity holder, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack pose, MultiBufferSource buffers,
							 float partial, float shown) {
		Minecraft mc = Minecraft.getInstance();
		float age = holder.tickCount + partial;
		BakedModel model = mc.getItemRenderer().getModel(stack, holder.level(), holder, holder.getId());
		pose.pushPose();
		model.getTransforms().getTransform(context).apply(leftHand, pose);
		pose.translate(-0.5f, -0.5f, -0.5f);
		// The blade: from the hilt (a third of the way up the diagonal) to the point.
		VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
		PoseStack.Pose p = pose.last();
		for (int i = 0; i < 6; i++) {
			float t = 0.35f + i * 0.11f;
			float size = 0.34f;
			SpellRender.quad(glow, p, SpellRender.v(t - size / 2, t - size / 2, 0.5f), SpellRender.v(t - size / 2, t + size / 2, 0.5f),
					SpellRender.v(t + size / 2, t + size / 2, 0.5f), SpellRender.v(t + size / 2, t - size / 2, 0.5f), POLISH, 0.5f * shown, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		// The keen edge: a thread of light along it, both faces.
		float e = 0.025f;
		for (float z : new float[]{0.47f, 0.53f}) {
			SpellRender.quad(glow, p, SpellRender.v(0.32f - e, 0.32f + e, z), SpellRender.v(0.32f + e, 0.32f - e, z), SpellRender.v(0.97f + e, 0.97f - e, z),
					SpellRender.v(0.97f - e, 0.97f + e, z), WHITE, 0.9f * shown, 0.45f, 0, 0.55f, 1, SpellRender.FULL_BRIGHT);
		}
		// A glint running from the hilt to the point now and again, flaring at the point.
		float run = (age % 30) / 12f;
		if (run < 1) {
			float t = 0.35f + run * 0.62f, size = 0.22f + (run > 0.85f ? (run - 0.85f) * 2.5f : 0);
			VertexConsumer star = buffers.getBuffer(SpellRender.glow(SpellRender.STAR));
			SpellRender.quad(star, p, SpellRender.v(t - size, t - size, 0.55f), SpellRender.v(t - size, t + size, 0.55f), SpellRender.v(t + size, t + size, 0.55f),
					SpellRender.v(t + size, t - size, 0.55f), WHITE, shown * Mth.sin((float) Math.PI * Math.min(1, run * 1.1f)), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		// Wisps of white smoke curling up off the edge.
		VertexConsumer smoke = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		for (int i = 0; i < 7; i++) {
			float t = (age * 0.025f + i / 7f) % 1;
			float along = 0.4f + (i % 4) * 0.15f;
			float x = along + Mth.sin(age * 0.2f + i) * 0.05f - t * 0.25f, y = along + t * 0.4f;
			float size = 0.12f + t * 0.25f;
			float alpha = 0.45f * shown * Mth.sin((float) Math.PI * t);
			SpellRender.quad(smoke, p, SpellRender.v(x - size, y - size, 0.5f), SpellRender.v(x - size, y + size, 0.5f), SpellRender.v(x + size, y + size, 0.5f),
					SpellRender.v(x + size, y - size, 0.5f), WHITE, alpha, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		pose.popPose();
	}

	private BladeWisps() {}
}
