package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws a great spell's construct (SpellConstruct): its kind's GeckoLib model (geo/spell), texture and animations, made of qi: see
 * through where its texture is (translucent), lit by its own light whatever the hour (full bright), its brightest parts glowing
 * (`_glowmask`: AutoGlowingGeoLayer), its kind's size times its own, turned the way its spell faces it, and fading in as it forms
 * and out as it goes. A tear in space (SpellConstruct.Form, from Void Shattering on) has no model: RiftRenderer draws it by hand; and a
 * tendril (the void tree's grasping roots) TendrilRenderer.
 */
public final class SpellConstructRenderer extends GeoEntityRenderer<SpellConstruct> {
	public SpellConstructRenderer(EntityRendererProvider.Context context) {
		super(context, new Model());
		this.shadowRadius = 0;
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
	}

	/** Whether a world was just joined and the shapes' textures are still to be loaded ({@link #preload}). */
	private static boolean preloading;

	/**
	 * Has {@link #preload} done at the next client tick, a world joined. Not straight away: joining runs as one of the client's queued
	 * tasks, and from inside one GeckoLib's glow waits for its base texture on a task queued behind itself, forever (the game froze as
	 * it joined; found in the test harness, 2026-10-24).
	 */
	public static void preloadSoon() {
		preloading = true;
	}

	/** At each client tick's end: the preload, if one is due. */
	public static void tick(Minecraft client) {
		if (!preloading || client.level == null) return;
		preloading = false;
		preload();
	}

	/**
	 * Loads every kind's texture and glow ahead, as a world is joined: the first time each shape was drawn, the game stood still some 70 ms
	 * loading them (seen checking the shapes in game, 2026-10-24), just as its great spell came; now that comes with the world's loading.
	 */
	private static void preload() {
		for (SpellConstruct.Kind kind : SpellConstruct.Kind.values()) {
			if (!kind.modelled()) continue; // a tear in space: drawn by hand, no texture
			ResourceLocation texture = texture(kind);
			Minecraft.getInstance().getTextureManager().getTexture(texture);
			AutoGlowingTexture.getRenderType(texture); // makes (and so loads) its glow from its _glowmask
		}
	}

	static ResourceLocation texture(SpellConstruct.Kind kind) {
		return DefyingTheHeavens.id("textures/entity/spell/" + kind.id + ".png");
	}

	@Override
	public void render(SpellConstruct construct, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		if (construct.kind().form == SpellConstruct.Form.TENDRIL) {
			TendrilRenderer.render(construct, partialTick, pose, buffers); // a grasping root, drawn by hand
			return;
		}
		if (!construct.kind().modelled()) {
			RiftRenderer.render(construct, partialTick, pose, buffers); // a tear in space, drawn by hand
			return;
		}
		super.render(construct, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT); // qi's own light
	}

	/** Turned as its spell faces it (its yaw, then its pitch: nose down for a downward look), not as a living body's. */
	@Override
	protected void applyRotations(SpellConstruct construct, PoseStack pose, float ageInTicks, float rotationYaw, float partialTick) {
		float yaw = Mth.rotLerp(partialTick, construct.yRotO, construct.getYRot());
		float pitch = Mth.lerp(partialTick, construct.xRotO, construct.getXRot());
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.mulPose(Axis.XP.rotationDegrees(-pitch));
	}

	@Override
	public RenderType getRenderType(SpellConstruct construct, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
		return RenderType.entityTranslucent(texture);
	}

	@Override
	public void scaleModelForRender(float widthScale, float heightScale, PoseStack pose, SpellConstruct construct, BakedGeoModel model, boolean isReRender,
			float partialTick, int packedLight, int packedOverlay) {
		float size = construct.kind().scale * construct.size();
		super.scaleModelForRender(widthScale * size, heightScale * size, pose, construct, model, isReRender, partialTick, packedLight, packedOverlay);
	}

	@Override
	public Color getRenderColor(SpellConstruct construct, float partialTick, int packedLight) {
		float age = construct.tickCount + partialTick;
		float alpha = Mth.clamp(Math.min(age, construct.life() - age) / SpellConstruct.FADE, 0.0f, 1.0f);
		return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha);
	}

	/** A kind's model, texture and animations (geo/spell, textures/entity/spell, animations/spell). */
	static final class Model extends GeoModel<SpellConstruct> {
		@Override
		public ResourceLocation getModelResource(SpellConstruct construct) {
			return DefyingTheHeavens.id("geo/spell/" + construct.kind().id + ".geo.json");
		}

		@Override
		public ResourceLocation getTextureResource(SpellConstruct construct) {
			return texture(construct.kind());
		}

		@Override
		public ResourceLocation getAnimationResource(SpellConstruct construct) {
			return DefyingTheHeavens.id("animations/spell/" + construct.kind().id + ".animation.json");
		}
	}
}
