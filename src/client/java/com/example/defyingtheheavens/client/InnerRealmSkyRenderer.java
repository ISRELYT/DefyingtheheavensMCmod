package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.Realm;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The Inner Realm's sky, which is the cultivator's own progress made visible (see InnerRealm):
 * <ul>
 *   <li>far below the island, a web of qi leylines that grows with every stage, qi pulsing along them toward the centre;</li>
 *   <li>at the centre, from Core Formation, a solid golden core that forms over the realm (shards of gold orbiting and
 *   fusing in, motes drawn to it); from Nascent Soul, the cultivator themselves, see-through and sitting in meditation,
 *   wrapped in black mist that clears stage by stage;</li>
 *   <li>a sky that starts almost black and brightens through starlight, nebulae and aurora to something celestial, with
 *   pillars of light from Heavenly Being and every line rising into the heavens at Four Axis.</li>
 * </ul>
 * Kept soft: everything is additive and dim enough that it never blinds.
 */
public class InnerRealmSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
	/** The leyline plane, this far below the eye. */
	private static final float PLANE = -70.0f;
	private static final int MAX_ROOTS = 14;
	private static final int MAX_DEPTH = 4;

	//                                      QR                     FB                    CF                    NS                    HB                    FA
	private static final float[][] ZENITH = {{0.010f, 0.010f, 0.020f}, {0.020f, 0.030f, 0.075f}, {0.030f, 0.030f, 0.11f}, {0.05f, 0.03f, 0.16f}, {0.08f, 0.09f, 0.27f}, {0.16f, 0.20f, 0.43f}};
	private static final float[][] HORIZON = {{0.030f, 0.030f, 0.050f}, {0.050f, 0.060f, 0.12f}, {0.09f, 0.07f, 0.17f}, {0.18f, 0.10f, 0.28f}, {0.30f, 0.24f, 0.48f}, {0.62f, 0.58f, 0.80f}};
	private static final float[][] NADIR = {{0.000f, 0.000f, 0.010f}, {0.010f, 0.010f, 0.030f}, {0.010f, 0.010f, 0.040f}, {0.02f, 0.02f, 0.06f}, {0.03f, 0.03f, 0.10f}, {0.08f, 0.08f, 0.20f}};
	private static final float[] STARS = {0.15f, 0.40f, 0.60f, 0.80f, 0.90f, 1.00f};
	private static final float[][] LINE = {{0.35f, 0.45f, 0.60f}, {0.40f, 0.65f, 0.95f}, {0.50f, 0.75f, 1.00f}, {0.60f, 0.82f, 1.00f}, {0.75f, 0.90f, 1.00f}, {0.90f, 0.95f, 1.00f}};
	private static final float[] LINE_ALPHA = {0.35f, 0.45f, 0.55f, 0.60f, 0.65f, 0.70f};
	private static final float[] AURORA = {0, 0, 0, 0.18f, 0.40f, 0.45f};
	/** Black mist around the Nascent Soul, Early to Grand Perfection. */
	private static final float[] MIST = {0.75f, 0.50f, 0.28f, 0.08f};

	/** One leyline segment: from (x1, z1) to (x2, z2), its width, its distance along the line from the centre, and when it appears. */
	private record Segment(float x1, float z1, float x2, float z2, float width, float dist, int root, int depth, int step) {}

	private static List<Segment> web;
	private static UUID webOwner;
	private PlayerModel<?> soulModel;
	private PlayerModel<?> slimSoulModel;

	@Override
	public void render(WorldRenderContext context) {
		ClientLevel level = context.world();
		Minecraft mc = Minecraft.getInstance();
		if (level == null || mc.player == null) return;
		try {
			PlayerCultivation c = ClientCultivationData.get();
			int tier = c.isMortal() ? 0 : c.getRealm().ordinal();
			int stage = c.isMortal() ? 0 : c.getStage().ordinal();
			int progress = tier * 4 + stage; // 0 .. 23
			float time = level.getGameTime() + context.tickDelta();
			PoseStack poseStack = context.matrixStack();
			Matrix4f pose = poseStack.last().pose();

			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(pose, ZENITH[tier], HORIZON[tier], NADIR[tier], 45.0f);

			poseStack.pushPose();
			poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.01f));
			SkyDome.drawStars(poseStack.last().pose(), STARS[tier], 0.9f, 0.92f, 1.0f, time);
			poseStack.popPose();

			additive();
			if (AURORA[tier] > 0) drawAurora(pose, AURORA[tier], time);
			drawLeylines(pose, mc.player.getUUID(), tier, progress, time);
			if (tier >= Realm.HEAVENLY_BEING.ordinal()) drawPillars(pose, tier == Realm.FOUR_AXIS.ordinal() ? 8 : 6, time);
			if (tier >= Realm.FOUR_AXIS.ordinal()) drawRisingLines(pose, time);

			if (tier < Realm.CORE_FORMATION.ordinal()) {
				// Before the core: only a faint node where the lines meet, brighter each stage.
				glow(pose, new Vector3f(0, PLANE + 0.5f, 0), 2.0f + progress * 0.4f, LINE[tier], 0.15f + progress * 0.03f);
			} else if (tier == Realm.CORE_FORMATION.ordinal()) {
				drawGoldenCore(poseStack, stage, time);
			} else {
				drawSoul(poseStack, mc, tier, stage, time);
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Inner Realm sky rendering failed", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	private static void additive() {
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
	}

	// --- Leylines ---

	/** The full web for this player (the same every time, so it grows rather than changes); stages reveal more of it. */
	private static List<Segment> web(UUID owner) {
		if (web != null && owner.equals(webOwner)) return web;
		List<Segment> segments = new ArrayList<>();
		Random random = new Random(owner.getMostSignificantBits() ^ owner.getLeastSignificantBits());
		for (int root = 0; root < MAX_ROOTS; root++) {
			// Interleaved so each new root lands between the existing ones.
			float angle = (float) (Math.PI * 2 * Integer.reverse(root) / 4294967296.0 + Math.PI) + random.nextFloat() * 0.2f;
			walk(segments, random, 0, 0, angle, 0, 0, root, 1.0f, 0);
		}
		web = segments;
		webOwner = owner;
		return segments;
	}

	private static void walk(List<Segment> out, Random random, float x, float z, float angle, float dist, int depth, int root,
			float width, int stepOffset) {
		int steps = 10 + random.nextInt(8) - depth * 2;
		for (int i = 0; i < steps; i++) {
			angle += (random.nextFloat() - 0.5f) * 0.7f;
			float length = 9.0f + random.nextFloat() * 6.0f;
			float nx = x + Mth.cos(angle) * length;
			float nz = z + Mth.sin(angle) * length;
			out.add(new Segment(x, z, nx, nz, width, dist, root, depth, stepOffset + i));
			x = nx;
			z = nz;
			dist += length;
			if (depth < MAX_DEPTH && random.nextFloat() < 0.32f) {
				walk(out, random, x, z, angle + (random.nextBoolean() ? 1 : -1) * (0.5f + random.nextFloat() * 0.6f), dist, depth + 1, root,
						width * 0.7f, stepOffset + i + 1);
			}
		}
	}

	private static void drawLeylines(Matrix4f pose, UUID owner, int tier, int progress, float time) {
		int roots = Math.min(MAX_ROOTS, 3 + progress / 2);
		int depth = Math.min(MAX_DEPTH, progress / 5);
		int reach = 6 + progress; // how far out each line has grown, in segments
		float[] colour = LINE[tier];
		float baseAlpha = LINE_ALPHA[tier];
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (Segment s : web(owner)) {
			if (s.root >= roots || s.depth > depth || s.step >= reach) continue;
			// Qi pulses inward along the lines, toward the centre.
			float pulse = 0.55f + 0.45f * Mth.sin(time * 0.06f + s.dist * 0.07f);
			float fade = Mth.clamp(1.0f - s.dist / 420.0f, 0.15f, 1.0f);
			float alpha = baseAlpha * pulse * fade * (0.6f + 0.4f * s.width);
			ribbonFlat(buffer, pose, s, s.width * 2.6f, colour, alpha * 0.18f); // soft glow
			ribbonFlat(buffer, pose, s, s.width * 0.45f, colour, alpha);        // the line itself
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}

	private static void ribbonFlat(BufferBuilder buffer, Matrix4f pose, Segment s, float halfWidth, float[] c, float alpha) {
		float dx = s.x2 - s.x1, dz = s.z2 - s.z1;
		float len = Mth.sqrt(dx * dx + dz * dz);
		if (len < 1e-3f) return;
		float px = -dz / len * halfWidth, pz = dx / len * halfWidth;
		buffer.vertex(pose, s.x1 - px, PLANE, s.z1 - pz).color(c[0], c[1], c[2], alpha).endVertex();
		buffer.vertex(pose, s.x1 + px, PLANE, s.z1 + pz).color(c[0], c[1], c[2], alpha).endVertex();
		buffer.vertex(pose, s.x2 + px, PLANE, s.z2 + pz).color(c[0], c[1], c[2], alpha).endVertex();
		buffer.vertex(pose, s.x2 - px, PLANE, s.z2 - pz).color(c[0], c[1], c[2], alpha).endVertex();
	}

	// --- The centre ---

	/**
	 * Core Formation: a solid golden core, larger each stage. Early on, shards of gold orbit and fuse into it and qi motes
	 * spiral in; by Grand Perfection it is whole and gleaming.
	 */
	private static void drawGoldenCore(PoseStack poseStack, int stage, float time) {
		float radius = 3.0f + stage * 1.2f;
		Vector3f centre = new Vector3f(0, PLANE + radius + 1.0f, 0);
		Matrix4f pose = poseStack.last().pose();
		glow(pose, centre, radius * 3.2f, new float[] {1.0f, 0.75f, 0.35f}, 0.22f + stage * 0.05f);

		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		poseStack.pushPose();
		poseStack.translate(centre.x, centre.y, centre.z);
		poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.6f));
		solidOrb(poseStack.last().pose(), radius, 0.12f * (3 - stage)); // rough at first, smooth when complete
		// Shards still drifting in, closing on the core.
		int shards = new int[] {8, 5, 3, 0}[stage];
		for (int i = 0; i < shards; i++) {
			float a = time * 0.02f * (1 + i % 3) + i * Mth.TWO_PI / Math.max(1, shards);
			float r = radius * (1.6f + 0.25f * Mth.sin(time * 0.03f + i));
			poseStack.pushPose();
			poseStack.translate(Mth.cos(a) * r, Mth.sin(time * 0.05f + i * 1.7f) * radius * 0.5f, Mth.sin(a) * r);
			poseStack.mulPose(Axis.XP.rotationDegrees(time * 2.0f + i * 40));
			poseStack.mulPose(Axis.ZP.rotationDegrees(time * 1.3f + i * 70));
			shard(poseStack.last().pose(), radius * 0.28f);
			poseStack.popPose();
		}
		poseStack.popPose();
		RenderSystem.depthMask(false);

		additive();
		// Qi motes spiralling in.
		int motes = new int[] {30, 20, 12, 6}[stage];
		for (int i = 0; i < motes; i++) {
			float phase = (time * 0.004f + i * 0.618f) % 1.0f;
			float r = radius * (1.3f + 5.0f * (1 - phase));
			float a = i * 2.39996f + phase * 6.0f;
			Vector3f at = new Vector3f(centre.x + Mth.cos(a) * r, centre.y + (i % 5 - 2) * radius * 0.25f * (1 - phase), centre.z + Mth.sin(a) * r);
			glow(pose, at, 0.5f, new float[] {1.0f, 0.85f, 0.45f}, 0.6f * phase);
		}
	}

	/** A faceted golden sphere, lit from above; {@code roughness} jitters its facets. */
	private static void solidOrb(Matrix4f pose, float radius, float roughness) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		int lats = 8, lons = 14;
		Vector3f light = new Vector3f(0.35f, 1.0f, 0.25f).normalize();
		for (int i = 0; i < lats; i++) {
			for (int j = 0; j < lons; j++) {
				Vector3f a = orbPoint(i, j, lats, lons, radius, roughness), b = orbPoint(i + 1, j, lats, lons, radius, roughness);
				Vector3f d = orbPoint(i, j + 1, lats, lons, radius, roughness), e = orbPoint(i + 1, j + 1, lats, lons, radius, roughness);
				facet(buffer, pose, a, b, e, light);
				facet(buffer, pose, a, e, d, light);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}

	private static Vector3f orbPoint(int i, int j, int lats, int lons, float radius, float roughness) {
		float lat = Mth.PI * i / lats - Mth.HALF_PI;
		float lon = Mth.TWO_PI * (j % lons) / lons;
		int hash = (i * 73856093) ^ ((j % lons) * 19349663);
		float r = radius * (1 + roughness * (((hash >>> 8) & 0xFF) / 255.0f - 0.5f) * (i == 0 || i == lats ? 0 : 1));
		return new Vector3f(Mth.cos(lat) * Mth.cos(lon) * r, Mth.sin(lat) * r, Mth.cos(lat) * Mth.sin(lon) * r);
	}

	private static void facet(BufferBuilder buffer, Matrix4f pose, Vector3f a, Vector3f b, Vector3f c, Vector3f light) {
		Vector3f mid = new Vector3f(a).add(b).add(c).div(3);
		Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
		if (normal.dot(mid) < 0) normal.negate();
		float lit = 0.45f + 0.55f * Math.max(0, normal.dot(light));
		float rim = (float) Math.pow(1 - Math.abs(normal.y), 3) * 0.25f;
		float r = Math.min(1, 1.0f * lit + rim), g = Math.min(1, 0.76f * lit + rim * 0.8f), bl = Math.min(1, 0.28f * lit + rim * 0.4f);
		buffer.vertex(pose, a.x, a.y, a.z).color(r, g, bl, 1.0f).endVertex();
		buffer.vertex(pose, b.x, b.y, b.z).color(r, g, bl, 1.0f).endVertex();
		buffer.vertex(pose, c.x, c.y, c.z).color(r, g, bl, 1.0f).endVertex();
	}

	/** A small golden octahedron. */
	private static void shard(Matrix4f pose, float size) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		Vector3f light = new Vector3f(0.35f, 1.0f, 0.25f).normalize();
		Vector3f[] p = {new Vector3f(size, 0, 0), new Vector3f(-size, 0, 0), new Vector3f(0, size * 1.6f, 0),
				new Vector3f(0, -size * 1.6f, 0), new Vector3f(0, 0, size), new Vector3f(0, 0, -size)};
		int[][] faces = {{0, 2, 4}, {4, 2, 1}, {1, 2, 5}, {5, 2, 0}, {0, 4, 3}, {4, 1, 3}, {1, 5, 3}, {5, 0, 3}};
		for (int[] f : faces) facet(buffer, pose, p[f[0]], p[f[1]], p[f[2]], light);
		BufferUploader.drawWithShader(buffer.end());
	}

	/**
	 * From Nascent Soul: the cultivator themselves, in their own skin, see-through and meditating where the core was, in
	 * black mist that thins each stage of Nascent Soul and is gone after it.
	 */
	private void drawSoul(PoseStack poseStack, Minecraft mc, int tier, int stage, float time) {
		float scale = 7.0f;
		Vector3f centre = new Vector3f(0, PLANE + 0.6f * scale, 0);
		Matrix4f pose = poseStack.last().pose();
		float radiance = tier == Realm.NASCENT_SOUL.ordinal() ? 0.25f + 0.05f * stage : 0.35f + 0.08f * (tier - Realm.NASCENT_SOUL.ordinal());
		glow(pose, centre, scale * 2.2f, new float[] {0.65f, 0.80f, 1.0f}, radiance);

		PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(mc.player.getUUID());
		ResourceLocation skin = InnerBodyRenderer.skinOf(mc.player.getUUID());
		boolean slim = info != null && "slim".equals(info.getModelName());
		PlayerModel<?> model = slim ? slimModel(mc) : wideModel(mc);
		LotusPose.applyWithLayers(model);

		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		poseStack.pushPose();
		poseStack.translate(0, PLANE, 0);
		poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(time * 0.002f) * 20.0f + 180.0f));
		poseStack.scale(scale, scale, scale);
		poseStack.scale(-1.0f, -1.0f, 1.0f); // entity models are built upside down, like LivingEntityRenderer draws them
		poseStack.translate(0.0f, -1.501f, 0.0f);
		Lighting.setupLevel(poseStack.last().pose());
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		float alpha = 0.55f + 0.1f * Math.min(stage, 3);
		model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(skin)), LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, 0.85f, 0.92f, 1.0f, alpha);
		buffers.endBatch();
		poseStack.popPose();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();

		if (tier == Realm.NASCENT_SOUL.ordinal() && MIST[stage] > 0.01f) {
			// Black mist clinging around the soul, slowly turning.
			RenderSystem.defaultBlendFunc();
			for (int i = 0; i < 16; i++) {
				float a = i * 2.39996f + time * 0.004f * (1 + i % 3);
				float r = scale * (0.35f + 0.5f * ((i * 37) % 10) / 10.0f);
				float y = centre.y + scale * (((i * 53) % 10) / 10.0f - 0.45f) * 0.9f;
				Vector3f at = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
				glow(pose, at, scale * (0.45f + 0.25f * ((i * 17) % 5) / 5.0f), new float[] {0.02f, 0.015f, 0.03f}, MIST[stage]);
			}
		}
		additive();
	}

	private PlayerModel<?> wideModel(Minecraft mc) {
		if (soulModel == null) soulModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		return soulModel;
	}

	private PlayerModel<?> slimModel(Minecraft mc) {
		if (slimSoulModel == null) slimSoulModel = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
		return slimSoulModel;
	}

	// --- Sky dressing ---

	/** A soft aurora band around the horizon. */
	private static void drawAurora(Matrix4f pose, float strength, float time) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float low = 14 * Mth.DEG_TO_RAD, high = 34 * Mth.DEG_TO_RAD;
		for (int lon = 0; lon < 360; lon += 6) {
			float a0 = lon * Mth.DEG_TO_RAD, a1 = (lon + 6) * Mth.DEG_TO_RAD;
			float w0 = strength * (0.5f + 0.5f * Mth.sin(lon * 0.05f + time * 0.01f)) * (0.6f + 0.4f * Mth.sin(lon * 0.13f - time * 0.017f));
			float w1 = strength * (0.5f + 0.5f * Mth.sin((lon + 6) * 0.05f + time * 0.01f)) * (0.6f + 0.4f * Mth.sin((lon + 6) * 0.13f - time * 0.017f));
			sky(buffer, pose, a0, low, 0.30f, 0.90f, 0.70f, w0);
			sky(buffer, pose, a1, low, 0.30f, 0.90f, 0.70f, w1);
			sky(buffer, pose, a1, high, 0.45f, 0.55f, 0.95f, 0);
			sky(buffer, pose, a0, high, 0.45f, 0.55f, 0.95f, 0);
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}

	private static void sky(BufferBuilder buffer, Matrix4f pose, float lon, float lat, float r, float g, float b, float alpha) {
		float d = 95.0f;
		buffer.vertex(pose, Mth.cos(lat) * Mth.cos(lon) * d, Mth.sin(lat) * d, Mth.cos(lat) * Mth.sin(lon) * d).color(r, g, b, alpha).endVertex();
	}

	/** Pillars of light rising out of the leylines. */
	private static void drawPillars(Matrix4f pose, int count, float time) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < count; i++) {
			float a = Mth.TWO_PI * i / count + 0.3f;
			float r = 90.0f + (i % 2) * 40.0f;
			Vector3f bottom = new Vector3f(Mth.cos(a) * r, PLANE, Mth.sin(a) * r);
			Vector3f top = new Vector3f(bottom.x, PLANE + 220.0f, bottom.z);
			float alpha = 0.16f * (0.7f + 0.3f * Mth.sin(time * 0.02f + i));
			ribbon(buffer, pose, bottom, top, 2.5f, new float[] {0.85f, 0.92f, 1.0f}, alpha, 0.0f);
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}

	/** Four Axis: every line rises from the web and curves up to meet far overhead. */
	private static void drawRisingLines(Matrix4f pose, float time) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Vector3f apex = new Vector3f(0, 160.0f, 0);
		for (int i = 0; i < 16; i++) {
			float a = Mth.TWO_PI * i / 16 + time * 0.0005f;
			Vector3f start = new Vector3f(Mth.cos(a) * 170.0f, PLANE, Mth.sin(a) * 170.0f);
			Vector3f control = new Vector3f(start.x * 0.75f, 110.0f, start.z * 0.75f);
			Vector3f previous = start;
			for (int k = 1; k <= 12; k++) {
				float t = k / 12.0f;
				Vector3f point = bezier(start, control, apex, t);
				float alpha = 0.22f * (1 - t * 0.5f) * (0.6f + 0.4f * Mth.sin(time * 0.05f - t * 6 + i));
				ribbon(buffer, pose, previous, point, 0.9f, new float[] {1.0f, 0.97f, 0.88f}, alpha, alpha);
				previous = point;
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}

	private static Vector3f bezier(Vector3f a, Vector3f b, Vector3f c, float t) {
		float u = 1 - t;
		return new Vector3f(a).mul(u * u).add(new Vector3f(b).mul(2 * u * t)).add(new Vector3f(c).mul(t * t));
	}

	/** A camera-facing ribbon from {@code a} to {@code b} (the camera is at the origin of the sky pose). */
	private static void ribbon(BufferBuilder buffer, Matrix4f pose, Vector3f a, Vector3f b, float halfWidth, float[] c, float alphaA, float alphaB) {
		Vector3f along = new Vector3f(b).sub(a);
		Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
		Vector3f side = along.cross(mid, new Vector3f());
		if (side.lengthSquared() < 1e-6f) return;
		side.normalize(halfWidth);
		buffer.vertex(pose, a.x - side.x, a.y - side.y, a.z - side.z).color(c[0], c[1], c[2], alphaA).endVertex();
		buffer.vertex(pose, a.x + side.x, a.y + side.y, a.z + side.z).color(c[0], c[1], c[2], alphaA).endVertex();
		buffer.vertex(pose, b.x + side.x, b.y + side.y, b.z + side.z).color(c[0], c[1], c[2], alphaB).endVertex();
		buffer.vertex(pose, b.x - side.x, b.y - side.y, b.z - side.z).color(c[0], c[1], c[2], alphaB).endVertex();
	}

	/**
	 * A soft disc facing the camera, fading to nothing at its edge, in whatever blend mode is set: additive for glows,
	 * normal for the dark mist.
	 */
	private static void glow(Matrix4f pose, Vector3f centre, float radius, float[] c, float alpha) {
		if (alpha <= 0.005f) return;
		Vector3f view = new Vector3f(centre).normalize();
		Vector3f right = view.cross(Math.abs(view.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0), new Vector3f()).normalize();
		Vector3f up = right.cross(view, new Vector3f()).normalize();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		int segments = 20;
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
			buffer.vertex(pose, centre.x, centre.y, centre.z).color(c[0], c[1], c[2], alpha).endVertex();
			Vector3f p0 = new Vector3f(right).mul(Mth.cos(a0) * radius).add(new Vector3f(up).mul(Mth.sin(a0) * radius)).add(centre);
			Vector3f p1 = new Vector3f(right).mul(Mth.cos(a1) * radius).add(new Vector3f(up).mul(Mth.sin(a1) * radius)).add(centre);
			buffer.vertex(pose, p0.x, p0.y, p0.z).color(c[0], c[1], c[2], 0.0f).endVertex();
			buffer.vertex(pose, p1.x, p1.y, p1.z).color(c[0], c[1], c[2], 0.0f).endVertex();
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
	}
}
