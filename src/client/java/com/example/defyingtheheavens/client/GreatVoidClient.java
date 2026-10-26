package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModDimensions;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Great Void Realm's look (2026-10-25, the user's pick: "a starlit void"): its continents float under a black-violet void full
 * of stars (space's own field and nebulae, AstralSky#backdrop, its seed the void's) and of drifting shards of broken space, glinting
 * at their broken edges ({@link #shards}); a violet haze about the far continents, no clouds. The Void Breach between it and the Jade
 * Firmament: the same broken space close about the climber, brighter, its shards near and quick.
 */
public final class GreatVoidClient {
	/** The void's own stars: the same sky for everyone. */
	private static final long SEED = 0x6A1C5EED7A11L;
	/** Fog start and end, as fractions of the render distance: the far continents fade into the void's violet. */
	private static final float[] GREAT_VOID_FOG = {0.35f, 1.0f};
	private static final float[] VOID_BREACH_FOG = {0.05f, 0.75f};
	private static final Vec3 VOID_HAZE = new Vec3(0.10, 0.06, 0.19), BREACH_HAZE = new Vec3(0.09, 0.04, 0.17);

	public static void register() {
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.GREAT_VOID_EFFECTS, new Effects(VOID_HAZE, 0.6));
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.GREAT_VOID, GreatVoidClient::greatVoidSky);
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.GREAT_VOID, context -> {});
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.VOID_BREACH_EFFECTS, new Effects(BREACH_HAZE, 1.0));
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.VOID_BREACH, GreatVoidClient::breachSky);
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.VOID_BREACH, context -> {});
	}

	/** Fog start and end fractions in the Great Void or the breach, else null. */
	static float[] fogFor(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
		if (ModDimensions.isGreatVoid(dimension)) return GREAT_VOID_FOG;
		if (ModDimensions.isVoidBreach(dimension)) return VOID_BREACH_FOG;
		return null;
	}

	/** The Great Void's sky: black above, violet at the horizon, the stars and nebulae, and the shards of broken space adrift. */
	private static void greatVoidSky(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null) return;
		try {
			PlanetTextures.frame();
			Matrix4f pose = context.matrixStack().last().pose();
			float time = level.getGameTime() + context.tickDelta();
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(pose, new float[] {0.012f, 0.004f, 0.03f}, new float[] {0.11f, 0.05f, 0.21f}, new float[] {0.04f, 0.015f, 0.08f}, 55f);
			AstralSky.backdrop(pose, SEED, time, 0.85f);
			shards(pose, time, 56, 1f, 0.75f, 0x5A1D);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Great Void sky rendering failed", e);
		} finally {
			restore();
		}
	}

	/** The breach's: broken space close about the climber, turning, its shards near, bright and quick. */
	private static void breachSky(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null) return;
		try {
			PoseStack poseStack = context.matrixStack();
			float time = level.getGameTime() + context.tickDelta();
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(poseStack.last().pose(), new float[] {0.16f, 0.08f, 0.30f}, new float[] {0.06f, 0.02f, 0.13f}, new float[] {0.02f, 0.0f, 0.06f}, 70f);
			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.05f));
			SkyDome.drawStars(poseStack.last().pose(), 0.9f, 0.85f, 0.75f, 1.0f, time);
			shards(poseStack.last().pose(), time, 90, 6f, 1f, 0xB2EAC);
			poseStack.popPose();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Void Breach sky rendering failed", e);
		} finally {
			restore();
		}
	}

	private static void restore() {
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
	}

	/**
	 * {@code count} shards of broken space on the sky, each a jagged four-sided splinter some degrees across, drifting round the sky
	 * ({@code drift} times the Great Void's slow pace) and turning slowly: a faint violet within, its broken edges glinting white-violet
	 * ({@code bright} of their full brightness). Their shapes and places come from {@code salt}.
	 */
	static void shards(Matrix4f pose, float time, int count, float drift, float bright, long salt) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < count; i++) {
			long id = salt * 977L + i * 131L;
			float lon = SpellRender.hash(id + 1) * Mth.TWO_PI + time * drift * (0.0002f + 0.0004f * SpellRender.hash(id + 2));
			float lat = (SpellRender.hash(id + 3) * 1.25f - 0.3f) * Mth.HALF_PI;
			float spin = SpellRender.hash(id + 4) * Mth.TWO_PI + time * drift * (SpellRender.hash(id + 5) - 0.5f) * 0.002f;
			float size = 2.5f + 6.5f * SpellRender.hash(id + 6) * SpellRender.hash(id + 7);
			float twinkle = 0.75f + 0.25f * Mth.sin(time * 0.04f + i * 2.3f);
			Vec3 centre = new Vec3(Mth.cos(lat) * Mth.cos(lon), Mth.sin(lat), Mth.cos(lat) * Mth.sin(lon));
			Vec3 t1 = centre.cross(Math.abs(centre.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
			Vec3 t2 = centre.cross(t1).normalize();
			Vec3 at = centre.scale(95);
			Vec3[] corners = new Vec3[4];
			for (int k = 0; k < 4; k++) {
				float a = spin + k * Mth.HALF_PI + (SpellRender.hash(id + 10 + k) - 0.5f) * 0.9f;
				float r = size * (0.45f + 0.75f * SpellRender.hash(id + 20 + k)) * (k % 2 == 0 ? 1.0f : 0.45f); // long and thin, a splinter
				corners[k] = at.add(t1.scale(Mth.cos(a) * r)).add(t2.scale(Mth.sin(a) * r));
			}
			// The faint violet within.
			float fill = 0.07f * bright * twinkle;
			for (Vec3 c : corners) buffer.vertex(pose, (float) c.x, (float) c.y, (float) c.z).color(0.55f, 0.38f, 0.95f, fill).endVertex();
			// Its broken edges glinting: a narrow band inside each edge, bright at the edge and gone a little way in.
			float edge = 0.6f * bright * twinkle;
			for (int k = 0; k < 4; k++) {
				Vec3 a = corners[k], b = corners[(k + 1) % 4];
				Vec3 ai = a.add(at.subtract(a).scale(0.16)), bi = b.add(at.subtract(b).scale(0.16));
				buffer.vertex(pose, (float) a.x, (float) a.y, (float) a.z).color(0.9f, 0.82f, 1.0f, edge).endVertex();
				buffer.vertex(pose, (float) b.x, (float) b.y, (float) b.z).color(0.9f, 0.82f, 1.0f, edge).endVertex();
				buffer.vertex(pose, (float) bi.x, (float) bi.y, (float) bi.z).color(0.6f, 0.45f, 1.0f, 0f).endVertex();
				buffer.vertex(pose, (float) ai.x, (float) ai.y, (float) ai.z).color(0.6f, 0.45f, 1.0f, 0f).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.defaultBlendFunc();
	}

	/** No clouds and no sun or moon: the sky is the renderer's. The fog leans to the void's violet haze ({@code lean} of the way). */
	private static final class Effects extends DimensionSpecialEffects {
		private final Vec3 haze;
		private final double lean;

		Effects(Vec3 haze, double lean) {
			super(Float.NaN, false, SkyType.NONE, false, false);
			this.haze = haze;
			this.lean = lean;
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			return fogColor.lerp(haze, lean);
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	private GreatVoidClient() {}
}
