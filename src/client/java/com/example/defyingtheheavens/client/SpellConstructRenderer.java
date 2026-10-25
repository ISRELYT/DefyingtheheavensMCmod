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

	/** At each client tick's end: the preload, if one is due, and the shapes still to be drawn once unseen ({@link #warm}). */
	public static void tick(Minecraft client) {
		if (client.level == null || client.player == null) {
			WARMING.clear();
			return;
		}
		if (preloading) {
			preloading = false;
			preload();
			for (SpellConstruct.Kind kind : SpellConstruct.Kind.values()) if (kind.modelled()) WARMING.add(kind);
			return;
		}
		for (int i = 0; i < WARM_A_TICK && !WARMING.isEmpty(); i++) warm(client, WARMING.poll());
	}

	/** The shapes not yet drawn once since the world was joined ({@link #warm}), and how many are drawn a tick. */
	private static final java.util.ArrayDeque<SpellConstruct.Kind> WARMING = new java.util.ArrayDeque<>();
	private static final int WARM_A_TICK = 2;

	/**
	 * Draws {@code kind}'s shape once, unseen (into {@link #NOWHERE}), a few a tick after a world is joined: its model, its animations and
	 * its glow taken through GeckoLib once before its great spell first calls it, so nothing is first made then (the user's ask,
	 * 2026-10-25: "preload models before the move is used so they don't lag out the game").
	 */
	private static void warm(Minecraft client, SpellConstruct.Kind kind) {
		try {
			SpellConstruct shape = SpellConstruct.create(client.level, kind, client.player.position(), 0, 0, 200, null);
			shape.tickCount = 40; // fully formed, as most of its life is drawn
			net.minecraft.client.renderer.entity.EntityRenderer<? super SpellConstruct> renderer = client.getEntityRenderDispatcher().getRenderer(shape);
			renderer.render(shape, 0, 0, new PoseStack(), NOWHERE, LightTexture.FULL_BRIGHT);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.debug("Warming the spell shape {} failed", kind, e);
		}
	}

	/** Where a shape drawn unseen goes: nowhere (every vertex let fall). */
	private static final MultiBufferSource NOWHERE = type -> Nowhere.INSTANCE;

	private enum Nowhere implements com.mojang.blaze3d.vertex.VertexConsumer {
		INSTANCE;

		@Override public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) { return this; }
		@Override public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) { return this; }
		@Override public com.mojang.blaze3d.vertex.VertexConsumer uv(float u, float v) { return this; }
		@Override public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int u, int v) { return this; }
		@Override public com.mojang.blaze3d.vertex.VertexConsumer uv2(int u, int v) { return this; }
		@Override public com.mojang.blaze3d.vertex.VertexConsumer normal(float x, float y, float z) { return this; }
		@Override public void endVertex() {}
		@Override public void defaultColor(int r, int g, int b, int a) {}
		@Override public void unsetDefaultColor() {}
	}

	/**
	 * Loads every kind's texture and glow ahead, as a world is joined: the first time each shape was drawn, the game stood still some 70 ms
	 * loading them (seen checking the shapes in game, 2026-10-24), just as its great spell came; now that comes with the world's loading.
	 */
	private static void preload() {
		for (SpellConstruct.Kind kind : SpellConstruct.Kind.values()) {
			if (!kind.modelled()) continue; // a tear in space: drawn by hand, no texture
			load(texture(kind));
		}
		load(EarthGolemRenderer.TEXTURE); // the golem's is made as theirs are (tools/spell-art)
	}

	private static void load(ResourceLocation texture) {
		Minecraft.getInstance().getTextureManager().getTexture(texture);
		AutoGlowingTexture.getRenderType(texture); // makes (and so loads) its glow from its _glowmask
	}

	static ResourceLocation texture(SpellConstruct.Kind kind) {
		return DefyingTheHeavens.id("textures/entity/spell/" + kind.id + ".png");
	}

	@Override
	public void render(SpellConstruct construct, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		if (construct.kind().form == SpellConstruct.Form.GHOSTS) return; // the dead, drawn as the mod's ghosts are, on the sense layer (SpellGhosts)
		if (construct.kind().form == SpellConstruct.Form.BLOCKS) {
			// Made of the world's own blocks, drawn as the land is (BlockMasses).
			net.minecraft.world.phys.Vec3 shift = construct.anchorShift(partialTick);
			pose.pushPose();
			pose.translate(shift.x, shift.y, shift.z);
			BlockMasses.render(construct, partialTick, pose, buffers, light);
			pose.popPose();
			return;
		}
		if (construct.kind().form == SpellConstruct.Form.DRAWN) {
			// Drawn by hand whole (SpellDrawn), where it is drawn this very frame.
			net.minecraft.world.phys.Vec3 shift = construct.anchorShift(partialTick);
			pose.pushPose();
			pose.translate(shift.x, shift.y, shift.z);
			SpellDrawn.render(construct, partialTick, pose, buffers, light, entityRenderDispatcher.cameraOrientation(),
					entityRenderDispatcher.camera.getPosition().subtract(construct.getPosition(partialTick).add(shift)));
			pose.popPose();
			return;
		}
		if (construct.kind().form == SpellConstruct.Form.TENDRIL) {
			TendrilRenderer.render(construct, partialTick, pose, buffers); // a grasping root, drawn by hand
			return;
		}
		if (!construct.kind().modelled()) {
			RiftRenderer.render(construct, partialTick, pose, buffers); // a tear in space, drawn by hand
			return;
		}
		// Held on a being (SpellConstruct#anchor): drawn where it is drawn this very frame, never a step behind it.
		net.minecraft.world.phys.Vec3 shift = construct.anchorShift(partialTick);
		pose.pushPose();
		pose.translate(shift.x, shift.y, shift.z);
		super.render(construct, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT); // qi's own light
		if (construct.kind() == SpellConstruct.Kind.KARMA_MIRROR) karmaLight(construct, partialTick, pose, buffers);
		pose.popPose();
	}

	/**
	 * The karma mirror's light (Mirror of Retribution): poured out of its face along its caster's look, as wide at the mirror as its face and
	 * widening to the spell's cone ({@code GhostSpells#MIRROR_CONE}) out to its reach, its rays streaming out along it, brightest as it flashes.
	 * (Its light was rays sent from the mirror's middle, thin and lagging behind it: the user's report, 2026-10-25, of beams that "don't line
	 * up" with what casts them.)
	 */
	private void karmaLight(SpellConstruct mirror, float partial, PoseStack pose, MultiBufferSource source) {
		float age = mirror.tickCount + partial;
		float shown = Mth.clamp((age - 8) / 10, 0, 1) * Mth.clamp((mirror.life() - age) / SpellConstruct.FADE, 0, 1);
		if (shown <= 0) return;
		boolean flash = "flash".equals(mirror.phase());
		float flare = flash ? Math.max(0, 1 - mirror.phaseAge(partial) / 12) : 0;
		float scale = mirror.kind().scale * mirror.size() / 16f;
		float face = 10.5f * scale * 0.92f, reach = (float) com.example.defyingtheheavens.GhostSpells.MIRROR_RANGE;
		float spread = (float) Math.tan(Math.toRadians(com.example.defyingtheheavens.GhostSpells.MIRROR_CONE));
		float[] teal = {0.33f, 0.95f, 0.9f}, pale = {0.8f, 1.0f, 0.98f};
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			pose.pushPose();
			pose.mulPose(Axis.YP.rotationDegrees(180.0f - mirror.drawnYaw(partial)));
			pose.mulPose(Axis.XP.rotationDegrees(-mirror.drawnPitch(partial)));
			pose.translate(0, 0, -1.4f * scale);
			pose.mulPose(Axis.XP.rotationDegrees(-90));
			PoseStack.Pose p = pose.last();
			SpellRender.Radius cone = h -> face + h * reach * spread;
			SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.BEAM)), p, reach, cone, 10, 32, teal, (0.3f + 0.5f * flare) * shown, 4, 3, 0, -age * 0.05f,
					0, SpellRender.FULL_BRIGHT, h -> (1 - h) * (1 - h));
			SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), p, reach * 0.8f, h -> face * 0.8f + h * reach * 0.8f * spread * 0.7f, 8, 24, pale,
					(0.12f + 0.4f * flare) * shown, 1, 1, 0, 0, 0, SpellRender.FULL_BRIGHT, h -> (1 - h) * (1 - h));
			// Its face alight.
			SpellRender.disc(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), p, face * 1.1f, 0.05f, 0, pale, (0.45f + 0.5f * flare) * shown, SpellRender.FULL_BRIGHT);
			pose.popPose();
		} finally {
			SpellBuffers.end();
		}
	}

	/**
	 * Turned as its spell faces it (its yaw, then its pitch: nose down for a downward look), not as a living body's; held on a being, as that
	 * being turns this frame (SpellConstruct#drawnYaw).
	 */
	@Override
	protected void applyRotations(SpellConstruct construct, PoseStack pose, float ageInTicks, float rotationYaw, float partialTick) {
		float yaw = construct.drawnYaw(partialTick);
		float pitch = construct.drawnPitch(partialTick);
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
		float alpha = Mth.clamp(Math.min(age / construct.kind().fadeIn, (construct.life() - age) / SpellConstruct.FADE), 0.0f, 1.0f);
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

		/** After its moves: the Azure Dragon's body laid along its flight (DragonPose). */
		@Override
		public void setCustomAnimations(SpellConstruct construct, long instanceId, software.bernie.geckolib.core.animation.AnimationState<SpellConstruct> state) {
			super.setCustomAnimations(construct, instanceId, state);
			if (construct.kind() == SpellConstruct.Kind.AZURE_DRAGON) DragonPose.pose(getAnimationProcessor(), construct, state.getPartialTick());
		}
	}
}
