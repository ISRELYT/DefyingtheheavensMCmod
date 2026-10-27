package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ImmortalGate;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModDimensions;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Immortal Gate's light, and the Southern Heavenly Gate's, drawn whole (the user's ask, 2026-10-27: "overhaul the gates portal effect,
 * the actual portal part looks not great, please make it better (using the golden colour of the immortal realm)"). The light's blocks draw
 * nothing of their own (ImmortalGateLightBlock); here, over the cells they fill in the opening (its own shape, as the client sees them):
 * <ul>
 *   <li>a deep sea of gold: a sheet of drifting light, and two more of it seen far back behind it, their pattern shifting with where one
 *   looks from ({@link #DEEP}, {@link #DEEPER}), so the opening seems to open a long way in; glints rising through it;</li>
 *   <li>a white-gold heart that breathes at its middle, rings of light running out from it;</li>
 *   <li>a bright rim where it meets its frame, its glow spilling over the floor before and behind it and up the pillars' faces;</li>
 *   <li>gold motes drifting out of it and up.</li>
 * </ul>
 * Opening, it blooms out from its middle ({@link #BLOOM_TICKS}); shutting, it folds back into it ({@link #FOLD_TICKS}).
 */
public final class GatePortalRenderer {
	/** The cells of an opening: across, and up from its threshold. */
	private static final int W = ImmortalGate.HALF * 2 + 1, H = ImmortalGate.HEIGHT;
	/** How long it takes to bloom open, and to fold shut (ticks). */
	static final float BLOOM_TICKS = 30, FOLD_TICKS = 18;
	/** How far back its two deeper layers lie (blocks), as the eye sees them through it. */
	private static final float DEEP = 4, DEEPER = 10;
	private static final int TEXTURE_SIZE = 128, SMALL_TEXTURE = 64;
	private static final ResourceLocation CLOUDS = DefyingTheHeavens.id("gate_light_clouds"), GLOW = DefyingTheHeavens.id("gate_light_glow"),
			GLINTS = DefyingTheHeavens.id("gate_light_glints"), HEART = DefyingTheHeavens.id("gate_light_heart"), RING = DefyingTheHeavens.id("gate_light_ring");
	private static final DustParticleOptions MOTE = new DustParticleOptions(new Vector3f(1.0f, 0.84f, 0.42f), 1.1f);
	private static boolean made;

	/** The gate of the realm the client is in: where it is, the cells its light fills, when it opened or shut. */
	private static final class Gate {
		final ResourceKey<Level> dimension;
		final int x, z;
		int threshold = Integer.MIN_VALUE;
		final boolean[][] lit = new boolean[W][H];
		boolean open;
		long openedAt = Long.MIN_VALUE / 2, shutAt = Long.MIN_VALUE / 2;

		Gate(ResourceKey<Level> dimension, int x, int z) {
			this.dimension = dimension;
			this.x = x;
			this.z = z;
		}

		boolean isLit(int i, int j) {
			return i >= 0 && i < W && j >= 0 && j < H && lit[i][j];
		}

		/** How far it is open, 0 to 1, at {@code time}: blooming, open, folding, or gone. */
		float open(float time) {
			if (open) return smooth((time - openedAt) / BLOOM_TICKS);
			return 1 - smooth((time - shutAt) / FOLD_TICKS);
		}

		/** Its light looked at again: where its threshold is (found the first time), which cells are lit, opened or shut since. */
		void look(ClientLevel level, long now) {
			if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) return;
			if (threshold == Integer.MIN_VALUE) {
				for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
					if (light(level, x, y, z)) {
						threshold = y - 1;
						break;
					}
				}
				if (threshold == Integer.MIN_VALUE) return; // shut, and never seen open: nothing to draw
			}
			boolean[][] seen = new boolean[W][H];
			int count = 0;
			for (int i = 0; i < W; i++) {
				for (int j = 0; j < H; j++) {
					seen[i][j] = light(level, x - ImmortalGate.HALF + i, threshold + 1 + j, z);
					if (seen[i][j]) count++;
				}
			}
			if (count > 0) {
				for (int i = 0; i < W; i++) System.arraycopy(seen[i], 0, lit[i], 0, H);
				if (!open) {
					open = true;
					openedAt = now;
				}
			} else if (open) {
				open = false; // the last light it had kept, to fold away
				shutAt = now;
			}
		}

		private static boolean light(ClientLevel level, int x, int y, int z) {
			return level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.IMMORTAL_GATE_LIGHT) || level.getBlockState(new BlockPos(x, y, z - 1)).is(ModBlocks.IMMORTAL_GATE_LIGHT);
		}
	}

	private static Gate gate;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(GatePortalRenderer::tick);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(GatePortalRenderer::render);
	}

	/** Disconnect. */
	public static void clear() {
		gate = null;
	}

	private static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			gate = null;
			return;
		}
		ResourceKey<Level> dimension = level.dimension();
		int x, z;
		if (ModDimensions.isGreatVoid(dimension)) {
			x = ImmortalGate.SKY_X;
			z = ImmortalGate.SKY_Z;
		} else if (ModDimensions.isImmortalRealm(dimension)) {
			x = ImmortalGate.HEAVEN_X;
			z = ImmortalGate.HEAVEN_Z;
		} else {
			gate = null;
			return;
		}
		if (gate == null || gate.dimension != dimension) gate = new Gate(dimension, x, z);
		long now = level.getGameTime();
		if (now % (gate.open ? 5 : 10) == 0) gate.look(level, now);
		if (!mc.isPaused() && gate.open && gate.threshold != Integer.MIN_VALUE) motes(level, gate, now);
	}

	/** Gold motes drifting out of its light either way and rising, now and then a spark. */
	private static void motes(ClientLevel level, Gate g, long now) {
		if (g.open(now) < 0.6f) return;
		RandomSource random = level.random;
		for (int k = 0; k < 5; k++) {
			int i = random.nextInt(W), j = random.nextInt(H);
			if (!g.lit[i][j]) continue;
			double x = g.x - ImmortalGate.HALF + i + random.nextDouble(), y = g.threshold + 1 + j + random.nextDouble();
			double out = random.nextBoolean() ? 1 : -1;
			level.addParticle(MOTE, true, x, y, g.z + out * 0.2, (random.nextDouble() - 0.5) * 0.02, 0.012 + random.nextDouble() * 0.02,
					out * (0.02 + random.nextDouble() * 0.03));
		}
		if (random.nextInt(3) == 0) {
			int i = random.nextInt(W), j = random.nextInt(H);
			if (g.lit[i][j]) level.addParticle(ParticleTypes.END_ROD, true, g.x - ImmortalGate.HALF + i + random.nextDouble(), g.threshold + 1 + j + random.nextDouble(),
					g.z + (random.nextBoolean() ? 0.3 : -0.3), 0, 0.02, 0);
		}
	}

	// --- Drawing ---

	private static void render(WorldRenderContext context) {
		Gate g = gate;
		ClientLevel level = context.world();
		if (g == null || level == null || g.threshold == Integer.MIN_VALUE || context.camera() == null || level.dimension() != g.dimension) return;
		float time = level.getGameTime() + context.tickDelta();
		float open = g.open(time);
		if (open <= 0) return;
		try {
			make();
			Vec3 cam = context.camera().getPosition();
			Matrix4f pose = context.matrixStack().last().pose();
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			float cx = g.x + 0.5f, cy = g.threshold + 1 + H / 2f;
			// The reach of its bloom: every cell lit within it, softly at its front.
			float reach = open * (float) (Math.hypot(W / 2.0, H / 2.0) + 3);

			// The sheet of gold itself, drifting up, its veins darker.
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderTexture(0, CLOUDS);
			sheet(pose, cam, g, cx, cy, reach, 0, 12, 0.0f, -time * 0.0045f, 1.0f, 0.80f, 0.36f, 0.9f);
			// Seen far back through it: two more layers of light (added), and glints rising through all of it.
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			RenderSystem.setShaderTexture(0, GLOW);
			sheet(pose, cam, g, cx, cy, reach, DEEP, 9, time * 0.0021f, -time * 0.0032f, 1.0f, 0.74f, 0.30f, 0.45f);
			sheet(pose, cam, g, cx, cy, reach, DEEPER, 16, -time * 0.0016f, -time * 0.0019f, 1.0f, 0.88f, 0.55f, 0.35f);
			RenderSystem.setShaderTexture(0, GLINTS);
			sheet(pose, cam, g, cx, cy, reach, 1.5f, 6, 0.0f, -time * 0.011f, 1.0f, 0.95f, 0.78f, 0.65f);
			// Its heart, breathing; rings of light running out of it.
			RenderSystem.setShaderTexture(0, HEART);
			float breath = 0.5f + 0.5f * Mth.sin(time * 0.07f);
			disc(pose, cam, cx, cy, g.z, (7.5f + 1.2f * breath) * open, 1.0f, 0.95f, 0.78f, (0.45f + 0.2f * breath) * open);
			RenderSystem.setShaderTexture(0, RING);
			for (int k = 0; k < 3; k++) {
				float phase = (time * 0.006f + k / 3f) % 1f;
				disc(pose, cam, cx, cy, g.z, (1.5f + 16 * phase) * open, 1.0f, 0.86f, 0.5f, 0.5f * (1 - phase) * (1 - phase) * open);
			}
			// The rim where it meets its frame, and its glow spilling out over the floor and up the pillars.
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			rim(pose, cam, g, cx, cy, reach, open);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("The Immortal Gate's light failed to draw", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}

	/**
	 * One layer of its light over every lit cell, its texture laid {@code scale} blocks to a tile and drifting by ({@code du}, {@code dv}),
	 * seen {@code depth} blocks back through the opening from the camera (0: on it), in (r, g, b) at {@code alpha}, softened at the bloom's
	 * front ({@code reach} from the middle).
	 */
	private static void sheet(Matrix4f pose, Vec3 cam, Gate g, float cx, float cy, float reach, float depth, float scale, float du, float dv,
			float r, float gr, float b, float alpha) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		float z = g.z;
		for (int i = 0; i < W; i++) {
			for (int j = 0; j < H; j++) {
				if (!g.lit[i][j]) continue;
				float x0 = g.x - ImmortalGate.HALF + i, y0 = g.threshold + 1 + j;
				vertex(buffer, pose, cam, x0, y0, z, cx, cy, reach, depth, scale, du, dv, r, gr, b, alpha);
				vertex(buffer, pose, cam, x0 + 1, y0, z, cx, cy, reach, depth, scale, du, dv, r, gr, b, alpha);
				vertex(buffer, pose, cam, x0 + 1, y0 + 1, z, cx, cy, reach, depth, scale, du, dv, r, gr, b, alpha);
				vertex(buffer, pose, cam, x0, y0 + 1, z, cx, cy, reach, depth, scale, du, dv, r, gr, b, alpha);
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void vertex(BufferBuilder buffer, Matrix4f pose, Vec3 cam, float x, float y, float z, float cx, float cy, float reach, float depth,
			float scale, float du, float dv, float r, float g, float b, float alpha) {
		// Where the eye's line through this point meets the layer lying {@code depth} back behind the opening.
		double vx = x - cam.x, vy = y - cam.y, vz = Math.max(0.6, Math.abs(z - cam.z));
		double u = x + vx * depth / vz, v = y + vy * depth / vz;
		float front = Mth.clamp((reach - (float) Math.hypot(x - cx, y - cy)) / 2.5f, 0, 1);
		buffer.vertex(pose, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z))
				.uv((float) (u / scale) + du, (float) (-v / scale) + dv)
				.color(r, g, b, alpha * front)
				.endVertex();
	}

	/** A disc of the bound texture, {@code radius} about ({@code cx}, {@code cy}) on the opening's plane (the frame's own blocks hide what of it lies past the opening). */
	private static void disc(Matrix4f pose, Vec3 cam, float cx, float cy, float z, float radius, float r, float g, float b, float alpha) {
		if (radius <= 0 || alpha <= 0) return;
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		float px = (float) (cx - cam.x), py = (float) (cy - cam.y), pz = (float) (z - cam.z);
		buffer.vertex(pose, px - radius, py - radius, pz).uv(0, 1).color(r, g, b, alpha).endVertex();
		buffer.vertex(pose, px + radius, py - radius, pz).uv(1, 1).color(r, g, b, alpha).endVertex();
		buffer.vertex(pose, px + radius, py + radius, pz).uv(1, 0).color(r, g, b, alpha).endVertex();
		buffer.vertex(pose, px - radius, py + radius, pz).uv(0, 0).color(r, g, b, alpha).endVertex();
		BufferUploader.drawWithShader(buffer.end());
	}

	/** Its rim: a bright edge on every lit cell beside its frame; and its glow spilling over the floor before and behind it and up the pillars. */
	private static void rim(Matrix4f pose, Vec3 cam, Gate g, float cx, float cy, float reach, float open) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float z = (float) (g.z - cam.z), w = 0.45f;
		float rr = 1.0f, rg = 0.93f, rb = 0.66f;
		for (int i = 0; i < W; i++) {
			for (int j = 0; j < H; j++) {
				if (!g.lit[i][j]) continue;
				float x0 = (float) (g.x - ImmortalGate.HALF + i - cam.x), y0 = (float) (g.threshold + 1 + j - cam.y);
				float a = 0.75f * Mth.clamp((reach - (float) Math.hypot(g.x - ImmortalGate.HALF + i + 0.5 - cx, g.threshold + 1.5 + j - cy)) / 2.5f, 0, 1);
				if (a <= 0) continue;
				if (!g.isLit(i - 1, j)) strip(buffer, pose, x0, y0, z, x0 + w, y0 + 1, a, 0, rr, rg, rb, true);
				if (!g.isLit(i + 1, j)) strip(buffer, pose, x0 + 1, y0, z, x0 + 1 - w, y0 + 1, a, 0, rr, rg, rb, true);
				if (!g.isLit(i, j - 1)) strip(buffer, pose, x0, y0, z, x0 + 1, y0 + w, a, 0, rr, rg, rb, false);
				if (!g.isLit(i, j + 1)) strip(buffer, pose, x0, y0 + 1, z, x0 + 1, y0 + 1 - w, a, 0, rr, rg, rb, false);
			}
		}
		// The floor before and behind it, lit from it: brightest at the opening, gone five blocks out.
		float left = (float) (g.x - ImmortalGate.HALF - cam.x), right = (float) (g.x + ImmortalGate.HALF + 1 - cam.x);
		float floor = (float) (g.threshold + 1.02 - cam.y), spill = 0.34f * open;
		for (int side : new int[] {-1, 1}) {
			float far = z + side * 5;
			buffer.vertex(pose, left, floor, z).color(1.0f, 0.82f, 0.4f, spill).endVertex();
			buffer.vertex(pose, right, floor, z).color(1.0f, 0.82f, 0.4f, spill).endVertex();
			buffer.vertex(pose, right, floor, far).color(1.0f, 0.82f, 0.4f, 0).endVertex();
			buffer.vertex(pose, left, floor, far).color(1.0f, 0.82f, 0.4f, 0).endVertex();
		}
		// Up the pillars' inner faces either side of it, a little way out from the light.
		float bottom = (float) (g.threshold + 1 - cam.y), top = bottom + H, glow = 0.28f * open;
		for (float face : new float[] {left + 0.01f, right - 0.01f}) {
			for (int side : new int[] {-1, 1}) {
				float far = z + side * 1.6f;
				buffer.vertex(pose, face, bottom, z).color(1.0f, 0.84f, 0.45f, glow).endVertex();
				buffer.vertex(pose, face, top, z).color(1.0f, 0.84f, 0.45f, glow).endVertex();
				buffer.vertex(pose, face, top, far).color(1.0f, 0.84f, 0.45f, 0).endVertex();
				buffer.vertex(pose, face, bottom, far).color(1.0f, 0.84f, 0.45f, 0).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	/** A strip from the edge at ({@code x0}, {@code y0}) (bright, {@code a}) to ({@code x1}, {@code y1}) (faded to {@code a1}): along y if {@code upright}. */
	private static void strip(BufferBuilder buffer, Matrix4f pose, float x0, float y0, float z, float x1, float y1, float a, float a1, float r, float g, float b,
			boolean upright) {
		if (upright) {
			buffer.vertex(pose, x0, y0, z).color(r, g, b, a).endVertex();
			buffer.vertex(pose, x0, y1, z).color(r, g, b, a).endVertex();
			buffer.vertex(pose, x1, y1, z).color(r, g, b, a1).endVertex();
			buffer.vertex(pose, x1, y0, z).color(r, g, b, a1).endVertex();
		} else {
			buffer.vertex(pose, x0, y0, z).color(r, g, b, a).endVertex();
			buffer.vertex(pose, x1, y0, z).color(r, g, b, a).endVertex();
			buffer.vertex(pose, x1, y1, z).color(r, g, b, a1).endVertex();
			buffer.vertex(pose, x0, y1, z).color(r, g, b, a1).endVertex();
		}
	}

	// --- Its textures, made once ---

	private static void make() {
		if (made) return;
		made = true;
		float[] noise = ImmortalRealmClient.cloudNoise(TEXTURE_SIZE, 0x60D1A6E7L);
		NativeImage clouds = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, false), glow = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, false);
		for (int x = 0; x < TEXTURE_SIZE; x++) {
			for (int y = 0; y < TEXTURE_SIZE; y++) {
				float n = noise[x * TEXTURE_SIZE + y];
				int light = (int) (255 * (0.62f + 0.38f * n));
				clouds.setPixelRGBA(x, y, 0xFF000000 | light << 16 | light << 8 | light); // ABGR: whole, its veins darker
				int a = (int) (255 * (float) Math.pow(n, 2.4));
				glow.setPixelRGBA(x, y, a << 24 | 0xFFFFFF); // white, as bright as it is dense
			}
		}
		register(CLOUDS, clouds);
		register(GLOW, glow);
		// Glints: a scatter of soft points, some drawn out up and down, wrapping at the edges.
		NativeImage glints = new NativeImage(SMALL_TEXTURE, SMALL_TEXTURE, false);
		float[] bright = new float[SMALL_TEXTURE * SMALL_TEXTURE];
		RandomSource random = RandomSource.create(0x6A7E1L);
		for (int k = 0; k < 46; k++) {
			int px = random.nextInt(SMALL_TEXTURE), py = random.nextInt(SMALL_TEXTURE), tail = random.nextInt(3) == 0 ? 3 + random.nextInt(5) : 0;
			float size = 0.8f + random.nextFloat() * 1.3f;
			for (int dx = -3; dx <= 3; dx++) {
				for (int dy = -3 - tail; dy <= 3; dy++) {
					float d = (float) Math.sqrt(dx * dx + (dy < 0 ? Math.max(0, -dy - tail) : dy) * (double) (dy < 0 ? Math.max(0, -dy - tail) : dy));
					float v = Math.max(0, 1 - d / (size + 1)) * (dy < 0 && tail > 0 ? 0.6f : 1);
					int i = Math.floorMod(px + dx, SMALL_TEXTURE) * SMALL_TEXTURE + Math.floorMod(py + dy, SMALL_TEXTURE);
					bright[i] = Math.max(bright[i], v * v);
				}
			}
		}
		for (int x = 0; x < SMALL_TEXTURE; x++) for (int y = 0; y < SMALL_TEXTURE; y++) glints.setPixelRGBA(x, y, (int) (255 * bright[x * SMALL_TEXTURE + y]) << 24 | 0xFFFFFF);
		register(GLINTS, glints);
		// The heart: a soft round glow; the ring: a soft band near its edge.
		NativeImage heart = new NativeImage(SMALL_TEXTURE, SMALL_TEXTURE, false), ring = new NativeImage(SMALL_TEXTURE, SMALL_TEXTURE, false);
		for (int x = 0; x < SMALL_TEXTURE; x++) {
			for (int y = 0; y < SMALL_TEXTURE; y++) {
				float dx = (x + 0.5f) / SMALL_TEXTURE * 2 - 1, dy = (y + 0.5f) / SMALL_TEXTURE * 2 - 1;
				float d = (float) Math.sqrt(dx * dx + dy * dy);
				int h = (int) (255 * (float) Math.pow(Math.max(0, 1 - d), 2.2));
				heart.setPixelRGBA(x, y, h << 24 | 0xFFFFFF);
				float band = (d - 0.84f) / 0.07f;
				int r = d > 1 ? 0 : (int) (255 * (float) Math.exp(-band * band));
				ring.setPixelRGBA(x, y, r << 24 | 0xFFFFFF);
			}
		}
		register(HEART, heart);
		register(RING, ring);
	}

	private static void register(ResourceLocation id, NativeImage image) {
		DynamicTexture texture = new DynamicTexture(image);
		texture.setFilter(true, false);
		Minecraft.getInstance().getTextureManager().register(id, texture);
	}

	private static float smooth(float t) {
		t = Mth.clamp(t, 0.0f, 1.0f);
		return t * t * (3 - 2 * t);
	}

	private GatePortalRenderer() {}
}
