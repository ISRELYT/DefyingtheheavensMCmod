package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.BeastKind;
import com.example.defyingtheheavens.ChaosFrontier;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.ModNoiseSettings;
import com.example.defyingtheheavens.PrimordialBeast;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The Immortal Realm's look (phase 2, 2026-10-25, the plan the user took: "an endless sea of clouds with jade peaks rising out of it,
 * waterfalls pouring into the clouds, and a golden sky"):
 * <ul>
 *   <li>a golden sky ({@link #sky}): pale gold overhead, rich gold at the horizon, a great white-gold sun low in the east with its
 *   halo;</li>
 *   <li>the sea of clouds ({@link #cloudSea}), drawn in the world after its translucent blocks, so the peaks rise out of it and their
 *   roots are lost in it: {@link #SHELLS} layers of one soft cloud texture laid one over another {@link #SHELL_GAP} apart below
 *   ModDimensions#IMMORTAL_CLOUD_Y, the lowest whole (nothing of the void below shows through), each higher one only where the clouds
 *   swell higher, so the top billows; drifting slowly east, the higher layers the faster; fading into the horizon's gold with the
 *   fog;</li>
 *   <li>a close white-gold fog within the clouds and under them ({@link #cloudFog});</li>
 *   <li>the True Dragon's storm (PrimordialBeast): near it the sky and the fog darken to the storm's grey ({@link #storm}), easing in as
 *   one nears it and out as one leaves it;</li>
 *   <li>the Chaos Frontier (phase 3c, ChaosFrontier): out past the realm's edge ({@link #chaos}) the sky bruises violet and the sun is
 *   lost, the sea of clouds turns grey-violet and churns (each layer sloshing against the next), the fog closes in violet, and motes of the
 *   storm's chaos stream through the air (none in the First Flame's light).</li>
 * </ul>
 * The Gate's passage: all gold light, motes of it streaming down past the climber.
 */
public final class ImmortalRealmClient {
	/** The sea of clouds: this many layers, this far apart (blocks), the top one at ModDimensions#IMMORTAL_CLOUD_Y. */
	static final int SHELLS = 10;
	static final float SHELL_GAP = 1.3f;
	/** The cloud texture covers this many blocks, and is this many texels across. */
	private static final float TILE = 192f;
	private static final int TEXTURE_SIZE = 256;
	/** How fast the clouds drift east (blocks a tick), the top layer this much the faster again. */
	private static final float DRIFT = 0.02f, TOP_DRIFT = 0.6f;
	/** The sea is drawn in cells this many blocks across, out to this far past the render distance. */
	private static final int CELL = 32;
	private static final float BEYOND = 1.25f;
	/** Fog start and end, as fractions of the render distance: the far peaks and the far clouds fade into the horizon's gold. */
	private static final float[] IMMORTAL_FOG = {0.45f, 1.0f};
	private static final float[] PASSAGE_FOG = {0.05f, 0.7f};
	/** Within the clouds, and under them, the fog closes in to these (blocks). */
	private static final float[] IN_CLOUD_FOG = {0f, 10f}, UNDER_CLOUD_FOG = {0f, 56f};
	private static final Vec3 GOLD_HAZE = new Vec3(0.98, 0.86, 0.62), CLOUD_WHITE = new Vec3(0.97, 0.94, 0.86), CLOUD_SHADE = new Vec3(0.78, 0.74, 0.66),
			PASSAGE_GOLD = new Vec3(1.0, 0.86, 0.5);
	private static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
	private static final ResourceLocation[] LAYERS = new ResourceLocation[SHELLS];
	private static boolean made;
	/** The True Dragon's storm: whole within {@link #STORM_FULL} of it, none past {@link #STORM_REACH}; its sky and fog its grey. */
	private static final double STORM_REACH = 112, STORM_FULL = 56;
	private static final float[] STORM_ZENITH = {0.20f, 0.22f, 0.28f}, STORM_HORIZON = {0.30f, 0.31f, 0.36f};
	private static final Vec3 STORM_FOG = new Vec3(0.26, 0.27, 0.32);
	private static float storm;
	/**
	 * The Chaos Frontier's look: none short of ModNoiseSettings#FRONTIER_FROM, whole by its edge (ChaosFrontier#EDGE, where its storm
	 * begins); its sky's bruised violet, its fog's, closer in; its clouds' grey-violet, each layer sloshing this far (blocks) against the
	 * next.
	 */
	private static final float[] CHAOS_ZENITH = {0.16f, 0.11f, 0.21f}, CHAOS_HORIZON = {0.38f, 0.28f, 0.44f}, CHAOS_LOW = {0.27f, 0.20f, 0.33f};
	private static final Vec3 CHAOS_FOG = new Vec3(0.30, 0.23, 0.38), CHAOS_CLOUD = new Vec3(0.52, 0.45, 0.62);
	private static final float[] CHAOS_FOG_RANGE = {0.2f, 0.85f}; // (closer, the shards faded into it before they could be seen: tour 3c-2)
	private static final double CHURN = 14;
	private static final net.minecraft.core.particles.DustParticleOptions CHAOS_MOTE =
			new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.55f, 0.45f, 0.70f), 1.2f);
	private static float chaos;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ImmortalRealmClient::stormTick);
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.IMMORTAL_REALM_EFFECTS, new Effects(GOLD_HAZE, 0.55, true));
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.IMMORTAL_REALM, ImmortalRealmClient::sky);
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.IMMORTAL_REALM, context -> {}); // the sea of clouds is ours
		DimensionRenderingRegistry.registerDimensionEffects(ModDimensions.IMMORTAL_PASSAGE_EFFECTS, new Effects(PASSAGE_GOLD, 0.9, false));
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.IMMORTAL_PASSAGE, ImmortalRealmClient::passageSky);
		DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.IMMORTAL_PASSAGE, context -> {});
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ImmortalRealmClient::cloudSea);
	}

	/** How deep in the True Dragon's storm the camera is, 0 to 1 (eased a little each tick toward how near the nearest True Dragon is). */
	public static float storm() { return storm; }

	/** How far into the Chaos Frontier's look the camera is, 0 to 1 (eased each tick toward where it stands). */
	public static float chaos() { return chaos; }

	private static void stormTick(Minecraft mc) {
		float target = 0, chaosTarget = 0;
		if (mc.level != null && mc.player != null && ModDimensions.isImmortalRealm(mc.level.dimension())) {
			double nearest = Double.MAX_VALUE;
			for (PrimordialBeast beast : mc.level.getEntitiesOfClass(PrimordialBeast.class, mc.player.getBoundingBox().inflate(STORM_REACH),
					b -> b.kind() == BeastKind.TRUE_DRAGON && b.isAlive())) {
				nearest = Math.min(nearest, beast.distanceTo(mc.player));
			}
			target = (float) Mth.clamp((STORM_REACH - nearest) / (STORM_REACH - STORM_FULL), 0, 1);
			Vec3 at = mc.gameRenderer.getMainCamera().getPosition();
			chaosTarget = (float) Mth.clamp((ChaosFrontier.out(at.x, at.z) - ModNoiseSettings.FRONTIER_FROM) / (ChaosFrontier.EDGE - ModNoiseSettings.FRONTIER_FROM), 0, 1);
		}
		storm = Mth.approach(storm, target, 0.01f);
		chaos = Mth.approach(chaos, chaosTarget, 0.02f);
		if (chaos > 0.3f && mc.level != null && mc.player != null && !mc.isPaused()) chaosMotes(mc);
	}

	/** The Frontier's storm in the air about the camera: motes of chaos streaming past on its wind, none in the First Flame's light. */
	private static void chaosMotes(Minecraft mc) {
		Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
		if (com.example.defyingtheheavens.FirstFlame.inLight(mc.level.dimension(), cam)) return;
		net.minecraft.util.RandomSource random = mc.level.getRandom();
		double windX = Math.cos(mc.level.getGameTime() * 0.002) * 0.35, windZ = Math.sin(mc.level.getGameTime() * 0.002) * 0.35;
		int count = Math.round(4 * chaos);
		for (int i = 0; i < count; i++) {
			double x = cam.x + (random.nextDouble() - 0.5) * 24, y = cam.y + (random.nextDouble() - 0.5) * 14, z = cam.z + (random.nextDouble() - 0.5) * 24;
			mc.level.addParticle(CHAOS_MOTE, x, y, z, windX, (random.nextDouble() - 0.5) * 0.05, windZ);
		}
		if (random.nextInt(3) == 0) {
			mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, cam.x + (random.nextDouble() - 0.5) * 16, cam.y + (random.nextDouble() - 0.5) * 10,
					cam.z + (random.nextDouble() - 0.5) * 16, windX, 0, windZ);
		}
	}

	private static float[] stormy(float[] clear, float[] dark) {
		float t = storm * 0.85f;
		return new float[] {Mth.lerp(t, clear[0], dark[0]), Mth.lerp(t, clear[1], dark[1]), Mth.lerp(t, clear[2], dark[2])};
	}

	/** {@code colour} gone toward the Chaos Frontier's {@code bruised}, by how far into it the camera is. */
	private static float[] chaotic(float[] colour, float[] bruised) {
		return new float[] {Mth.lerp(chaos, colour[0], bruised[0]), Mth.lerp(chaos, colour[1], bruised[1]), Mth.lerp(chaos, colour[2], bruised[2])};
	}

	/** Fog start and end fractions in the Immortal Realm (closing in out in the Chaos Frontier) or the Gate's passage, else null. */
	static float[] fogFor(ResourceKey<Level> dimension) {
		if (ModDimensions.isImmortalRealm(dimension)) {
			if (chaos <= 0) return IMMORTAL_FOG;
			return new float[] {Mth.lerp(chaos, IMMORTAL_FOG[0], CHAOS_FOG_RANGE[0]), Mth.lerp(chaos, IMMORTAL_FOG[1], CHAOS_FOG_RANGE[1])};
		}
		if (ModDimensions.isImmortalPassage(dimension)) return PASSAGE_FOG;
		return null;
	}

	/** Within the clouds or under them in the Immortal Realm: the fog's start and end in blocks; else null. */
	public static float[] cloudFog(Camera camera) {
		if (camera == null || camera.getEntity() == null || !ModDimensions.isImmortalRealm(camera.getEntity().level().dimension())) return null;
		double y = camera.getPosition().y, top = ModDimensions.IMMORTAL_CLOUD_Y, bottom = top - (SHELLS - 1) * SHELL_GAP;
		if (y > top) return null;
		return y >= bottom - 1 ? IN_CLOUD_FOG : UNDER_CLOUD_FOG;
	}

	/**
	 * The golden sky: its gradient, the sun's halo and the sun low over the clouds in the east. Within the clouds, or under them, the sky is
	 * the clouds' own white (or their shadowed underside's), sun and all hidden (tour 2: the gold showed through between their layers).
	 */
	private static void sky(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null) return;
		try {
			PoseStack poseStack = context.matrixStack();
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			float[] clouds = cloudFog(context.camera());
			if (clouds != null) {
				float[] white = clouds == IN_CLOUD_FOG ? new float[] {0.97f, 0.94f, 0.86f} : new float[] {0.78f, 0.74f, 0.66f};
				white = chaotic(white, clouds == IN_CLOUD_FOG ? new float[] {0.52f, 0.45f, 0.62f} : new float[] {0.34f, 0.28f, 0.42f});
				SkyDome.drawGradient(poseStack.last().pose(), white, white, white, 48f);
				return;
			}
			// The golden sky, or near the True Dragon its storm's grey (its sun's halo dimmed with it); out in the Chaos Frontier bruised
			// violet, the sun lost.
			SkyDome.drawGradient(poseStack.last().pose(), chaotic(stormy(new float[] {0.80f, 0.78f, 0.66f}, STORM_ZENITH), CHAOS_ZENITH),
					chaotic(stormy(new float[] {1.0f, 0.82f, 0.48f}, STORM_HORIZON), CHAOS_HORIZON),
					chaotic(stormy(new float[] {0.98f, 0.92f, 0.78f}, STORM_HORIZON), CHAOS_LOW), 48f);
			poseStack.pushPose();
			// The sun low in the east, some twenty-five degrees over the clouds.
			poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
			poseStack.mulPose(Axis.XP.rotationDegrees(65.0f));
			Matrix4f sun = poseStack.last().pose();
			float clear = (1 - storm * 0.85f) * (1 - chaos);
			SkyDome.drawHalo(sun, 70.0f, 1.0f, 0.86f, 0.55f, 0.55f * clear);
			SkyDome.drawHalo(sun, 30.0f, 1.0f, 0.95f, 0.8f, 0.6f * clear);
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			if (chaos < 1) drawSun(sun, 22.0f, 1 - chaos);
			poseStack.popPose();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Immortal Realm sky rendering failed", e);
		} finally {
			restore();
		}
	}

	/** The Gate's passage: all gold, its light streaming down past the climber. */
	private static void passageSky(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null) return;
		try {
			PoseStack poseStack = context.matrixStack();
			float time = level.getGameTime() + context.tickDelta();
			RenderSystem.depthMask(false);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(poseStack.last().pose(), new float[] {1.0f, 0.97f, 0.85f}, new float[] {1.0f, 0.82f, 0.45f},
					new float[] {0.75f, 0.52f, 0.22f}, 70f);
			poseStack.pushPose();
			poseStack.mulPose(Axis.XP.rotationDegrees(time * 0.35f % 360));
			SkyDome.drawStars(poseStack.last().pose(), 1.0f, 1.0f, 0.95f, 0.75f, time);
			poseStack.popPose();
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Immortal passage sky rendering failed", e);
		} finally {
			restore();
		}
	}

	private static void drawSun(Matrix4f pose, float size, float alpha) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, SUN);
		RenderSystem.setShaderColor(1.0f, 0.96f, 0.82f, alpha);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(pose, -size, 100.0f, -size).uv(0.0f, 0.0f).endVertex();
		buffer.vertex(pose, size, 100.0f, -size).uv(1.0f, 0.0f).endVertex();
		buffer.vertex(pose, size, 100.0f, size).uv(1.0f, 1.0f).endVertex();
		buffer.vertex(pose, -size, 100.0f, size).uv(0.0f, 1.0f).endVertex();
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	private static void restore() {
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
	}

	// --- The sea of clouds ---

	/**
	 * WorldRenderEvents.AFTER_TRANSLUCENT, in the Immortal Realm: the sea of clouds about the camera, its layers drawn back to front (from
	 * below the lowest first, from above the highest first; from within, the farthest first), depth-tested against the land, so the peaks
	 * rise out of it, fogged as the land is.
	 */
	private static void cloudSea(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null || !ModDimensions.isImmortalRealm(level.dimension()) || context.camera() == null) return;
		try {
			make();
			Vec3 cam = context.camera().getPosition();
			Matrix4f pose = context.matrixStack().last().pose();
			float time = level.getGameTime() + context.tickDelta();
			float reach = Math.max(256f, context.gameRenderer().getRenderDistance() * BEYOND);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
			float bottom = ModDimensions.IMMORTAL_CLOUD_Y - (SHELLS - 1) * SHELL_GAP;
			boolean fromAbove = cam.y >= ModDimensions.IMMORTAL_CLOUD_Y, fromBelow = cam.y <= bottom;
			int[] order = new int[SHELLS];
			for (int i = 0; i < SHELLS; i++) order[i] = fromBelow ? SHELLS - 1 - i : i;
			if (!fromAbove && !fromBelow) {
				// Within: the layers farthest from the camera first.
				Integer[] by = new Integer[SHELLS];
				for (int i = 0; i < SHELLS; i++) by[i] = i;
				java.util.Arrays.sort(by, (a, b) -> Double.compare(Math.abs(cam.y - (bottom + b * SHELL_GAP)), Math.abs(cam.y - (bottom + a * SHELL_GAP))));
				for (int i = 0; i < SHELLS; i++) order[i] = by[i];
			}
			for (int shell : order) drawShell(pose, cam, shell, bottom + shell * SHELL_GAP, reach, time);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Immortal Realm cloud sea rendering failed", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	/**
	 * One layer at {@code y}: a grid of cells about the camera, its texture laid by the world's coordinates and drifting east; out in the
	 * Chaos Frontier grey-violet, and churning: each layer sloshing round its own way against the next ({@link #CHURN}).
	 */
	private static void drawShell(Matrix4f pose, Vec3 cam, int shell, float y, float reach, float time) {
		RenderSystem.setShaderTexture(0, LAYERS[shell]);
		float up = shell / (float) (SHELLS - 1);
		// The lower layers shadowed, the top in the sun: white-gold over warm grey.
		float shade = 0.74f + 0.26f * up;
		float r = Mth.lerp(chaos, shade, (float) CHAOS_CLOUD.x * shade), g = Mth.lerp(chaos, shade * 0.965f, (float) CHAOS_CLOUD.y * shade),
				b = Mth.lerp(chaos, shade * 0.88f, (float) CHAOS_CLOUD.z * shade);
		double drift = time * DRIFT * (1 + TOP_DRIFT * up);
		double churnX = chaos * CHURN * Math.sin(time * 0.006 + shell * 1.7), churnZ = chaos * CHURN * Math.cos(time * 0.0047 + shell * 2.3);
		// The texture's origin a whole tile from the camera's, so the coordinates stay small however far out one is.
		double originX = Math.floor((cam.x + drift) / TILE) * TILE - drift + churnX, originZ = Math.floor(cam.z / TILE) * TILE + churnZ;
		float dy = (float) (y - cam.y);
		int cells = Mth.ceil(reach / CELL);
		double startX = Math.floor(cam.x / CELL) * CELL - cells * CELL, startZ = Math.floor(cam.z / CELL) * CELL - cells * CELL;
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		float ny = shell == SHELLS - 1 || cam.y > y ? 1 : -1;
		for (int i = 0; i < cells * 2; i++) {
			double x0 = startX + i * CELL, x1 = x0 + CELL;
			for (int k = 0; k < cells * 2; k++) {
				double z0 = startZ + k * CELL, z1 = z0 + CELL;
				vertex(buffer, pose, x0, z0, dy, cam, originX, originZ, r, g, b, ny);
				vertex(buffer, pose, x0, z1, dy, cam, originX, originZ, r, g, b, ny);
				vertex(buffer, pose, x1, z1, dy, cam, originX, originZ, r, g, b, ny);
				vertex(buffer, pose, x1, z0, dy, cam, originX, originZ, r, g, b, ny);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void vertex(BufferBuilder buffer, Matrix4f pose, double x, double z, float dy, Vec3 cam, double originX, double originZ,
			float r, float g, float b, float ny) {
		buffer.vertex(pose, (float) (x - cam.x), dy, (float) (z - cam.z))
				.uv((float) ((x - originX) / TILE), (float) ((z - originZ) / TILE))
				.color(r, g, b, 1.0f)
				.normal(0, ny, 0)
				.endVertex();
	}

	/**
	 * The layers' textures, made once: one soft cloud noise (tileable, its octaves wrapping), each layer white where the clouds reach
	 * up to it and clear where they don't, its edge soft; the lowest whole.
	 */
	private static void make() {
		if (made) return;
		made = true;
		float[] noise = cloudNoise(TEXTURE_SIZE, 0x1A5CE11DL);
		for (int shell = 0; shell < SHELLS; shell++) {
			NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, false);
			float threshold = shell / (float) SHELLS * 0.78f;
			for (int x = 0; x < TEXTURE_SIZE; x++) {
				for (int z = 0; z < TEXTURE_SIZE; z++) {
					float n = noise[x * TEXTURE_SIZE + z];
					float alpha = shell == 0 ? 1.0f : smooth((n - threshold + 0.07f) / 0.14f);
					// A little shading within each layer: the swells brighter, the hollows a shade greyer.
					int light = (int) (255 * (0.88f + 0.12f * n));
					int a = (int) (255 * alpha);
					image.setPixelRGBA(x, z, a << 24 | light << 16 | light << 8 | light); // ABGR
				}
			}
			DynamicTexture texture = new DynamicTexture(image);
			texture.setFilter(true, false);
			LAYERS[shell] = DefyingTheHeavens.id("immortal_clouds_" + shell);
			Minecraft.getInstance().getTextureManager().register(LAYERS[shell], texture);
		}
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0f, 1.0f);
		return t * t * (3 - 2 * t);
	}

	/** Tileable value noise of four octaves on a {@code size}-square, 0..1, broad billows with finer puffs on them. */
	static float[] cloudNoise(int size, long seed) {
		float[] out = new float[size * size];
		float total = 0;
		int[] cellsPer = {4, 8, 16, 32};
		float[] weights = {0.5f, 0.27f, 0.15f, 0.08f};
		for (int o = 0; o < cellsPer.length; o++) {
			int cells = cellsPer[o];
			float[] lattice = new float[cells * cells];
			for (int i = 0; i < lattice.length; i++) lattice[i] = hash(seed + o * 7919L, i);
			for (int x = 0; x < size; x++) {
				float fx = x * cells / (float) size;
				int x0 = (int) fx, x1 = (x0 + 1) % cells;
				float tx = smooth(fx - x0);
				for (int z = 0; z < size; z++) {
					float fz = z * cells / (float) size;
					int z0 = (int) fz, z1 = (z0 + 1) % cells;
					float tz = smooth(fz - z0);
					float a = Mth.lerp(tx, lattice[x0 * cells + z0], lattice[x1 * cells + z0]);
					float c = Mth.lerp(tx, lattice[x0 * cells + z1], lattice[x1 * cells + z1]);
					out[x * size + z] += weights[o] * Mth.lerp(tz, a, c);
				}
			}
			total += weights[o];
		}
		// Stretched to fill 0..1, so every layer's threshold finds clouds to show.
		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
		for (float v : out) {
			min = Math.min(min, v);
			max = Math.max(max, v);
		}
		for (int i = 0; i < out.length; i++) out[i] = (out[i] - min) / Math.max(1e-6f, max - min);
		return out;
	}

	private static float hash(long seed, int i) {
		long h = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L;
		h ^= h >>> 31;
		h *= 0x94D049BB133111EBL;
		h ^= h >>> 29;
		return (h >>> 40) / (float) (1L << 24);
	}

	/**
	 * No vanilla clouds, sun or moon: the sky and the sea of clouds are ours. The fog leans to the realm's gold ({@code lean} of the way),
	 * and within or under the clouds to their white ({@code clouds}).
	 */
	private static final class Effects extends DimensionSpecialEffects {
		private final Vec3 haze;
		private final double lean;
		private final boolean clouds;

		Effects(Vec3 haze, double lean, boolean clouds) {
			super(Float.NaN, false, SkyType.NONE, false, false);
			this.haze = haze;
			this.lean = lean;
			this.clouds = clouds;
		}

		@Override
		public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
			float[] fog = clouds ? cloudFog(Minecraft.getInstance().gameRenderer.getMainCamera()) : null;
			if (fog != null) return (fog == IN_CLOUD_FOG ? CLOUD_WHITE : CLOUD_SHADE).lerp(fog == IN_CLOUD_FOG ? CHAOS_CLOUD : CHAOS_FOG, chaos);
			Vec3 golden = fogColor.lerp(haze, lean);
			if (clouds && storm > 0) golden = golden.lerp(STORM_FOG, storm * 0.75);
			return clouds && chaos > 0 ? golden.lerp(CHAOS_FOG, chaos * 0.9) : golden;
		}

		@Override
		public boolean isFoggyAt(int x, int y) {
			return false;
		}
	}

	private ImmortalRealmClient() {}
}
