package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * What the element spells' pictures share (the spell renderers, ClientSpellFx, the aura layers): their textures
 * ({@code textures/spell/}, drawn by the mod: flames, water, bark, mud, steel, ice...), the ways they are blended ({@link #glow}: added
 * light; {@link #clear}: see-through; {@link #solid}), and the shapes they are made of: quads and billboards, boxes, tubes along a
 * path, spheres, cylinders and cones, rings, crescents. Every shape is drawn in the pose's space (an entity's, about its feet), with
 * the entity format's vertices (colour, uv, overlay, light, normal). Nothing here is a vanilla particle.
 */
public final class SpellRender {
	public static final ResourceLocation GLOW = tex("glow"), SPARK = tex("spark"), RING = tex("ring"), SMOKE = tex("smoke"), FLAME = tex("flame"),
			KARMA = tex("karma"), YANG = tex("yang"), WATER = tex("water"), RIPPLE = tex("ripple"), BUBBLE = tex("bubble"), BARK = tex("bark"),
			LEAF = tex("leaf"), VINE = tex("vine"), BAMBOO = tex("bamboo"), PETAL = tex("petal"), LOTUS_LEAF = tex("lotus_leaf"), MUD = tex("mud"),
			DUST = tex("dust"), STONE_SHELL = tex("stone_shell"), STEEL = tex("steel"),
			GOLD = tex("gold"), SLASH = tex("slash"), WIND = tex("wind"), ICE = tex("ice"), CLOUD = tex("cloud"), RAIN = tex("rain"), MIST = tex("mist"),
			SUN = tex("sun"), RUNE = tex("rune"), VEINS = tex("veins"), FEATHER = tex("feather"), STAR = tex("star"), LIGHTNING = tex("lightning");
	/** The Heaven-Severing Sword's blade: u along its arc, v from its white-hot edge in, its whole body showing (SeverTex). */
	public static final ResourceLocation SEVER = tex("sever");
	/** Vanilla's beacon beam: streaks along a tube, for a beam's see-through core (added light alone is lost against a day sky). */
	public static final ResourceLocation BEAM = new ResourceLocation("minecraft", "textures/entity/beacon_beam.png");
	/** The five elements' seals: its character (火 水 木 土 金) within rings of trigrams and the star of the five (TexGen). */
	public static final ResourceLocation SEAL_FIRE = tex("seal_fire"), SEAL_WATER = tex("seal_water"), SEAL_WOOD = tex("seal_wood"),
			SEAL_EARTH = tex("seal_earth"), SEAL_METAL = tex("seal_metal");

	/** The seal of {@code element} (the plain rune for any other). */
	public static ResourceLocation seal(com.example.defyingtheheavens.QiElement element) {
		if (element == null) return RUNE;
		return switch (element) {
			case FIRE -> SEAL_FIRE;
			case WATER -> SEAL_WATER;
			case WOOD -> SEAL_WOOD;
			case EARTH -> SEAL_EARTH;
			case METAL -> SEAL_METAL;
			default -> RUNE;
		};
	}

	/** {@code element}'s light: its core colour drawn toward its glow, as {r, g, b}. */
	public static float[] light(com.example.defyingtheheavens.QiElement element) {
		if (element == null) return new float[] {0.9f, 0.85f, 1.0f};
		float[] core = rgb(element.getCoreColor()), glow = rgb(element.getGlowColor());
		return new float[] {(core[0] * 2 + glow[0]) / 3, (core[1] * 2 + glow[1]) / 3, (core[2] * 2 + glow[2]) / 3};
	}
	/** Frames in the animated strips (each a square, top to bottom). */
	public static final int FLAME_FRAMES = 8, WATER_FRAMES = 8, SUN_FRAMES = 4;
	public static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

	private static ResourceLocation tex(String name) {
		return DefyingTheHeavens.id("textures/spell/" + name + ".png");
	}

	// --- Blending ---

	/**
	 * Added light: {@code texture} times the vertex colour (its alpha fading it) laid over what is behind, never hiding it; no depth
	 * written, both faces drawn, full bright. Flames, glows, sparks, sword light.
	 */
	public static RenderType glow(ResourceLocation texture) {
		return Types.glow(texture);
	}

	/**
	 * Solid light, lit by nothing: a sun's body. It hides what is behind it and writes the depth, and is drawn first among a spell's (with the solid
	 * kinds: SpellBuffers), so the flames, glows and rays laid about it show only where they stand clear of it. A sun drawn see-through showed the
	 * fire behind it and inside it through its face, as if it were too small for its own fire (the user's report, 2026-10-26: "the vfx inside
	 * this 'sun' is like clipping through it").
	 */
	public static RenderType core(ResourceLocation texture) {
		return Types.core(texture);
	}

	/** See-through and lit by nothing (always bright): water, ice, mist, clouds, a ward's skin. */
	public static RenderType clear(ResourceLocation texture) {
		return RenderType.entityTranslucentEmissive(texture);
	}

	/** See-through, lit by the world: earthen and wooden things that are partly see-through (a ward of stone). */
	public static RenderType clearLit(ResourceLocation texture) {
		return RenderType.entityTranslucent(texture);
	}

	/** Solid, lit by the world, both faces: stakes, swords, podao, bamboo, vines. */
	public static RenderType solid(ResourceLocation texture) {
		return RenderType.entityCutoutNoCull(texture);
	}

	/** Solid things made of the world's blocks (a boulder, a spike of the ground's own rock), on the block atlas. */
	public static RenderType blocks() {
		return RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
	}

	/** Light with no texture at all (a beam's core, a flash): FruitAuraRenderType's additive glow, in the position-colour format. */
	public static RenderType flatGlow() {
		return FruitAuraRenderType.GLOW;
	}

	/** The render types of our own, made the way vanilla's are (extends RenderType only to reach its parts). */
	private static final class Types extends RenderType {
		private static final java.util.Map<ResourceLocation, RenderType> GLOWS = new java.util.HashMap<>(), CORES = new java.util.HashMap<>();

		/** {@link SpellRender#core}: SpellBuffers knows it by its name's start, as solid. */
		static RenderType core(ResourceLocation texture) {
			return CORES.computeIfAbsent(texture, t -> create("defying_the_heavens_spell_core", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536,
					false, false, CompositeState.builder()
							.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
							.setTextureState(new TextureStateShard(t, false, false))
							.setTransparencyState(NO_TRANSPARENCY)
							.setCullState(NO_CULL)
							.setOverlayState(OVERLAY)
							.createCompositeState(false)));
		}

		static RenderType glow(ResourceLocation texture) {
			return GLOWS.computeIfAbsent(texture, t -> create("defying_the_heavens_spell_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536,
					false, true, CompositeState.builder()
							.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
							.setTextureState(new TextureStateShard(t, false, false))
							.setTransparencyState(LIGHTNING_TRANSPARENCY)
							.setCullState(NO_CULL)
							.setWriteMaskState(COLOR_WRITE)
							.setOverlayState(OVERLAY)
							.setOutputState(WEATHER_TARGET)
							.createCompositeState(false)));
		}

		private Types(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
			super(name, format, mode, size, crumbling, sort, setup, clear);
		}
	}

	// --- Colour ---

	/** A colour of 0xRRGGBB as {r, g, b} from 0 to 1. */
	public static float[] rgb(int colour) {
		return new float[]{(colour >> 16 & 0xFF) / 255f, (colour >> 8 & 0xFF) / 255f, (colour & 0xFF) / 255f};
	}

	/** A sprite of a block's own texture on the block atlas (its side's), for things made of the world's blocks. */
	public static TextureAtlasSprite sprite(BlockState state) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
	}

	/** {@link #sprite} from a block state's id (a field's or missile's data). */
	public static TextureAtlasSprite sprite(int stateId) {
		return sprite(Block.stateById(stateId));
	}

	// --- Vertices ---

	/** One vertex in the entity format. */
	public static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float r, float g, float b, float a, float u, float v,
							  int light, float nx, float ny, float nz) {
		vc.vertex(pose.pose(), x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
	}

	/** One vertex in the position-colour format ({@link #flatGlow}). */
	public static void flat(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float r, float g, float b, float a) {
		vc.vertex(pose.pose(), x, y, z).color(r, g, b, a).endVertex();
	}

	/** A quad through four points (a, b, c, d in order round it), the texture's {@code u0..u1}, {@code v0..v1} across it. */
	public static void quad(VertexConsumer vc, PoseStack.Pose pose, Vector3f a, Vector3f b, Vector3f c, Vector3f d, float[] rgb, float alpha,
							float u0, float v0, float u1, float v1, int light) {
		Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a));
		if (n.lengthSquared() > 1.0e-12f) n.normalize();
		else n.set(0, 1, 0);
		vertex(vc, pose, a.x, a.y, a.z, rgb[0], rgb[1], rgb[2], alpha, u0, v0, light, n.x, n.y, n.z);
		vertex(vc, pose, b.x, b.y, b.z, rgb[0], rgb[1], rgb[2], alpha, u0, v1, light, n.x, n.y, n.z);
		vertex(vc, pose, c.x, c.y, c.z, rgb[0], rgb[1], rgb[2], alpha, u1, v1, light, n.x, n.y, n.z);
		vertex(vc, pose, d.x, d.y, d.z, rgb[0], rgb[1], rgb[2], alpha, u1, v0, light, n.x, n.y, n.z);
	}

	/**
	 * A square of {@code size} facing the camera ({@code camera}: its rotation) at {@code at}, turned {@code roll} radians in its
	 * own plane; the texture's {@code u0..u1, v0..v1} across it.
	 */
	public static void billboard(VertexConsumer vc, PoseStack pose, Quaternionf camera, float x, float y, float z, float size, float roll, float[] rgb,
								 float alpha, float u0, float v0, float u1, float v1, int light) {
		billboard(vc, pose, camera, x, y, z, size, size, roll, rgb, alpha, u0, v0, u1, v1, light);
	}

	/** {@link #billboard} {@code width} wide and {@code height} tall, standing on {@code at} if {@code height} is its full height. */
	public static void billboard(VertexConsumer vc, PoseStack pose, Quaternionf camera, float x, float y, float z, float width, float height, float roll,
								 float[] rgb, float alpha, float u0, float v0, float u1, float v1, int light) {
		pose.pushPose();
		pose.translate(x, y, z);
		pose.mulPose(camera);
		if (roll != 0) pose.mulPose(com.mojang.math.Axis.ZP.rotation(roll));
		PoseStack.Pose p = pose.last();
		float w = width / 2, h = height / 2;
		vertex(vc, p, -w, -h, 0, rgb[0], rgb[1], rgb[2], alpha, u1, v1, light, 0, 0, 1);
		vertex(vc, p, w, -h, 0, rgb[0], rgb[1], rgb[2], alpha, u0, v1, light, 0, 0, 1);
		vertex(vc, p, w, h, 0, rgb[0], rgb[1], rgb[2], alpha, u0, v0, light, 0, 0, 1);
		vertex(vc, p, -w, h, 0, rgb[0], rgb[1], rgb[2], alpha, u1, v0, light, 0, 0, 1);
		pose.popPose();
	}

	/**
	 * A flame tongue standing on {@code at}, {@code width} wide and {@code height} tall, turned to face the camera about the up axis
	 * only (so it rises straight even when looked down on), its frame chosen by {@code time} (ticks).
	 */
	public static void flameTongue(VertexConsumer vc, PoseStack pose, Vec3 camera, float x, float y, float z, float width, float height, float time,
								   float[] rgb, float alpha, int frames) {
		double dx = camera.x - x, dz = camera.z - z;
		float yaw = (float) Math.atan2(dx, dz);
		int frame = Math.floorMod((int) Math.floor(time * 0.5f), frames);
		float v0 = frame / (float) frames, v1 = (frame + 1) / (float) frames;
		pose.pushPose();
		pose.translate(x, y, z);
		pose.mulPose(com.mojang.math.Axis.YP.rotation(yaw));
		PoseStack.Pose p = pose.last();
		float w = width / 2;
		vertex(vc, p, -w, 0, 0, rgb[0], rgb[1], rgb[2], alpha, 0, v1, FULL_BRIGHT, 0, 0, 1);
		vertex(vc, p, w, 0, 0, rgb[0], rgb[1], rgb[2], alpha, 1, v1, FULL_BRIGHT, 0, 0, 1);
		vertex(vc, p, w, height, 0, rgb[0], rgb[1], rgb[2], alpha, 1, v0, FULL_BRIGHT, 0, 0, 1);
		vertex(vc, p, -w, height, 0, rgb[0], rgb[1], rgb[2], alpha, 0, v0, FULL_BRIGHT, 0, 0, 1);
		pose.popPose();
	}

	/** A box from (x0, y0, z0) to (x1, y1, z1), the whole texture (or {@code u0..u1, v0..v1}) on each face, shaded a little by its side. */
	public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float[] rgb, float alpha,
						   float u0, float v0, float u1, float v1, int light) {
		float[] top = rgb, side = {rgb[0] * 0.85f, rgb[1] * 0.85f, rgb[2] * 0.85f}, bottom = {rgb[0] * 0.6f, rgb[1] * 0.6f, rgb[2] * 0.6f};
		quad(vc, pose, v(x0, y1, z0), v(x0, y1, z1), v(x1, y1, z1), v(x1, y1, z0), top, alpha, u0, v0, u1, v1, light);
		quad(vc, pose, v(x0, y0, z1), v(x0, y0, z0), v(x1, y0, z0), v(x1, y0, z1), bottom, alpha, u0, v0, u1, v1, light);
		quad(vc, pose, v(x0, y1, z0), v(x1, y1, z0), v(x1, y0, z0), v(x0, y0, z0), side, alpha, u0, v0, u1, v1, light);
		quad(vc, pose, v(x1, y1, z1), v(x0, y1, z1), v(x0, y0, z1), v(x1, y0, z1), side, alpha, u0, v0, u1, v1, light);
		quad(vc, pose, v(x0, y1, z1), v(x0, y1, z0), v(x0, y0, z0), v(x0, y0, z1), side, alpha, u0, v0, u1, v1, light);
		quad(vc, pose, v(x1, y1, z0), v(x1, y1, z1), v(x1, y0, z1), v(x1, y0, z0), side, alpha, u0, v0, u1, v1, light);
	}

	public static Vector3f v(float x, float y, float z) {
		return new Vector3f(x, y, z);
	}

	public static Vector3f v(Vec3 at) {
		return new Vector3f((float) at.x, (float) at.y, (float) at.z);
	}

	/**
	 * A tube along {@code points} ({@code radii} at each), {@code sides} round: the texture's u runs round it once, its v along it
	 * {@code vPerBlock} a block (from {@code vOffset}); its colour and alpha at each point from {@code shade} (null: plain).
	 */
	public static void tube(VertexConsumer vc, PoseStack.Pose pose, List<Vec3> points, float[] radii, int sides, float[] rgb, float alpha,
							float vPerBlock, float vOffset, int light, PointShade shade) {
		int n = points.size();
		if (n < 2) return;
		Vec3[] dirs = new Vec3[n];
		for (int i = 0; i < n; i++) {
			Vec3 a = points.get(Math.max(0, i - 1)), b = points.get(Math.min(n - 1, i + 1));
			Vec3 d = b.subtract(a);
			dirs[i] = d.lengthSqr() < 1.0e-10 ? new Vec3(0, 1, 0) : d.normalize();
		}
		// A frame carried along the path (parallel transport), so the tube doesn't twist.
		Vec3 up = Math.abs(dirs[0].y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3[] normals = new Vec3[n], binormals = new Vec3[n];
		Vec3 nrm = dirs[0].cross(up).normalize();
		for (int i = 0; i < n; i++) {
			if (i > 0) {
				Vec3 b = nrm.subtract(dirs[i].scale(nrm.dot(dirs[i])));
				if (b.lengthSqr() > 1.0e-10) nrm = b.normalize();
			}
			normals[i] = nrm;
			binormals[i] = dirs[i].cross(nrm).normalize();
		}
		float length = 0;
		float[] along = new float[n];
		for (int i = 1; i < n; i++) {
			length += (float) points.get(i).distanceTo(points.get(i - 1));
			along[i] = length;
		}
		for (int i = 0; i < n - 1; i++) {
			float[] c0 = shade == null ? rgb : shade.colour(i, n), c1 = shade == null ? rgb : shade.colour(i + 1, n);
			float a0 = shade == null ? alpha : shade.alpha(i, n) * alpha, a1 = shade == null ? alpha : shade.alpha(i + 1, n) * alpha;
			for (int s = 0; s < sides; s++) {
				double t0 = Math.PI * 2 * s / sides, t1 = Math.PI * 2 * (s + 1) / sides;
				Vec3 o00 = ring(points.get(i), normals[i], binormals[i], radii[i], t0), o01 = ring(points.get(i), normals[i], binormals[i], radii[i], t1);
				Vec3 o10 = ring(points.get(i + 1), normals[i + 1], binormals[i + 1], radii[i + 1], t0);
				Vec3 o11 = ring(points.get(i + 1), normals[i + 1], binormals[i + 1], radii[i + 1], t1);
				float u0 = s / (float) sides, u1 = (s + 1) / (float) sides;
				float v0 = vOffset + along[i] * vPerBlock, v1 = vOffset + along[i + 1] * vPerBlock;
				Vec3 nm = normals[i].scale(Math.cos(t0 + Math.PI / sides)).add(binormals[i].scale(Math.sin(t0 + Math.PI / sides)));
				vertex(vc, pose, (float) o00.x, (float) o00.y, (float) o00.z, c0[0], c0[1], c0[2], a0, u0, v0, light, (float) nm.x, (float) nm.y, (float) nm.z);
				vertex(vc, pose, (float) o10.x, (float) o10.y, (float) o10.z, c1[0], c1[1], c1[2], a1, u0, v1, light, (float) nm.x, (float) nm.y, (float) nm.z);
				vertex(vc, pose, (float) o11.x, (float) o11.y, (float) o11.z, c1[0], c1[1], c1[2], a1, u1, v1, light, (float) nm.x, (float) nm.y, (float) nm.z);
				vertex(vc, pose, (float) o01.x, (float) o01.y, (float) o01.z, c0[0], c0[1], c0[2], a0, u1, v0, light, (float) nm.x, (float) nm.y, (float) nm.z);
			}
		}
	}

	private static Vec3 ring(Vec3 centre, Vec3 n, Vec3 b, float r, double t) {
		return centre.add(n.scale(Math.cos(t) * r)).add(b.scale(Math.sin(t) * r));
	}

	/** A tube's colour and alpha at each of its points (fading toward a tip, warmer toward a head). */
	public interface PointShade {
		float[] colour(int i, int n);

		float alpha(int i, int n);
	}

	/**
	 * A ribbon through {@code points} turned to face the camera ({@code camera}: its position in the pose's space), {@code widths}
	 * across at each point; the texture's u across it, v along it ({@code vPerBlock} from {@code vOffset}).
	 */
	public static void ribbon(VertexConsumer vc, PoseStack.Pose pose, List<Vec3> points, float[] widths, Vec3 camera, float[] rgb, float[] alphas,
							  float vPerBlock, float vOffset, int light) {
		int n = points.size();
		if (n < 2) return;
		Vec3[] left = new Vec3[n], right = new Vec3[n];
		float[] along = new float[n];
		for (int i = 0; i < n; i++) {
			Vec3 p = points.get(i);
			Vec3 d = points.get(Math.min(n - 1, i + 1)).subtract(points.get(Math.max(0, i - 1)));
			Vec3 side = d.cross(p.subtract(camera));
			if (side.lengthSqr() < 1.0e-10) side = new Vec3(0, 1, 0);
			side = side.normalize().scale(widths[i] / 2);
			left[i] = p.add(side);
			right[i] = p.subtract(side);
			if (i > 0) along[i] = along[i - 1] + (float) p.distanceTo(points.get(i - 1));
		}
		for (int i = 0; i < n - 1; i++) {
			float v0 = vOffset + along[i] * vPerBlock, v1 = vOffset + along[i + 1] * vPerBlock;
			vertex(vc, pose, (float) left[i].x, (float) left[i].y, (float) left[i].z, rgb[0], rgb[1], rgb[2], alphas[i], 0, v0, light, 0, 1, 0);
			vertex(vc, pose, (float) right[i].x, (float) right[i].y, (float) right[i].z, rgb[0], rgb[1], rgb[2], alphas[i], 1, v0, light, 0, 1, 0);
			vertex(vc, pose, (float) right[i + 1].x, (float) right[i + 1].y, (float) right[i + 1].z, rgb[0], rgb[1], rgb[2], alphas[i + 1], 1, v1, light, 0, 1, 0);
			vertex(vc, pose, (float) left[i + 1].x, (float) left[i + 1].y, (float) left[i + 1].z, rgb[0], rgb[1], rgb[2], alphas[i + 1], 0, v1, light, 0, 1, 0);
		}
	}

	/**
	 * A sphere of {@code radius} about the pose's origin, {@code rings} by {@code segments}; the texture wrapped round it ({@code uScale}
	 * times round, {@code vScale} pole to pole, offset by {@code uOffset, vOffset}); each vertex pushed out by {@code wobble} (null:
	 * a true sphere).
	 */
	public static void sphere(VertexConsumer vc, PoseStack.Pose pose, float radius, int rings, int segments, float[] rgb, float alpha, float uScale,
							  float vScale, float uOffset, float vOffset, int light, Wobble wobble) {
		for (int i = 0; i < rings; i++) {
			double p0 = Math.PI * i / rings, p1 = Math.PI * (i + 1) / rings;
			for (int j = 0; j < segments; j++) {
				double t0 = Math.PI * 2 * j / segments, t1 = Math.PI * 2 * (j + 1) / segments;
				Vector3f a = point(radius, p0, t0, wobble), b = point(radius, p1, t0, wobble), c = point(radius, p1, t1, wobble), d = point(radius, p0, t1, wobble);
				float u0 = uOffset + uScale * j / segments, u1 = uOffset + uScale * (j + 1) / segments;
				float v0 = vOffset + vScale * i / rings, v1 = vOffset + vScale * (i + 1) / rings;
				Vector3f na = new Vector3f(a).normalize(), nb = new Vector3f(b).normalize(), nc = new Vector3f(c).normalize(), nd = new Vector3f(d).normalize();
				vertex(vc, pose, a.x, a.y, a.z, rgb[0], rgb[1], rgb[2], alpha, u0, v0, light, na.x, na.y, na.z);
				vertex(vc, pose, b.x, b.y, b.z, rgb[0], rgb[1], rgb[2], alpha, u0, v1, light, nb.x, nb.y, nb.z);
				vertex(vc, pose, c.x, c.y, c.z, rgb[0], rgb[1], rgb[2], alpha, u1, v1, light, nc.x, nc.y, nc.z);
				vertex(vc, pose, d.x, d.y, d.z, rgb[0], rgb[1], rgb[2], alpha, u1, v0, light, nd.x, nd.y, nd.z);
			}
		}
	}

	/** How far a sphere's surface stands out at a point (polar angle, azimuth), as a share of its radius (1: unmoved). */
	public interface Wobble {
		float at(double polar, double azimuth);
	}

	private static Vector3f point(float radius, double polar, double azimuth, Wobble wobble) {
		float r = wobble == null ? radius : radius * wobble.at(polar, azimuth);
		return new Vector3f((float) (Math.sin(polar) * Math.cos(azimuth) * r), (float) (Math.cos(polar) * r), (float) (Math.sin(polar) * Math.sin(azimuth) * r));
	}

	/**
	 * An open cylinder (or cone, or funnel) standing on the pose's origin, {@code height} tall, its radius at height h from
	 * {@code radius}, {@code rings} high and {@code segments} round, the texture once round ({@code uScale}) and up it ({@code vScale}),
	 * turned {@code twist} radians as it rises; alpha by height from {@code fade} (null: plain).
	 */
	public static void cylinder(VertexConsumer vc, PoseStack.Pose pose, float height, Radius radius, int rings, int segments, float[] rgb, float alpha,
								float uScale, float vScale, float uOffset, float vOffset, float twist, int light, Radius fade) {
		for (int i = 0; i < rings; i++) {
			float h0 = height * i / rings, h1 = height * (i + 1) / rings;
			float r0 = radius.at(h0 / height), r1 = radius.at(h1 / height);
			float a0 = fade == null ? alpha : alpha * fade.at(h0 / height), a1 = fade == null ? alpha : alpha * fade.at(h1 / height);
			for (int j = 0; j < segments; j++) {
				double t0 = Math.PI * 2 * j / segments, t1 = Math.PI * 2 * (j + 1) / segments;
				double w0 = twist * i / rings, w1 = twist * (i + 1) / rings;
				float x00 = (float) (Math.cos(t0 + w0) * r0), z00 = (float) (Math.sin(t0 + w0) * r0);
				float x01 = (float) (Math.cos(t1 + w0) * r0), z01 = (float) (Math.sin(t1 + w0) * r0);
				float x10 = (float) (Math.cos(t0 + w1) * r1), z10 = (float) (Math.sin(t0 + w1) * r1);
				float x11 = (float) (Math.cos(t1 + w1) * r1), z11 = (float) (Math.sin(t1 + w1) * r1);
				float u0 = uOffset + uScale * j / segments, u1 = uOffset + uScale * (j + 1) / segments;
				float v0 = vOffset + vScale * (1 - (float) i / rings), v1 = vOffset + vScale * (1 - (float) (i + 1) / rings);
				float nx = (float) Math.cos(t0 + Math.PI / segments), nz = (float) Math.sin(t0 + Math.PI / segments);
				vertex(vc, pose, x00, h0, z00, rgb[0], rgb[1], rgb[2], a0, u0, v0, light, nx, 0, nz);
				vertex(vc, pose, x10, h1, z10, rgb[0], rgb[1], rgb[2], a1, u0, v1, light, nx, 0, nz);
				vertex(vc, pose, x11, h1, z11, rgb[0], rgb[1], rgb[2], a1, u1, v1, light, nx, 0, nz);
				vertex(vc, pose, x01, h0, z01, rgb[0], rgb[1], rgb[2], a0, u1, v0, light, nx, 0, nz);
			}
		}
	}

	/** A radius (or an alpha) by height, 0 at the bottom to 1 at the top. */
	public interface Radius {
		float at(float t);
	}

	/** A flat disc of {@code radius} on the pose's ground (y up), the whole texture across it, turned {@code spin} radians. */
	public static void disc(VertexConsumer vc, PoseStack.Pose pose, float radius, float y, float spin, float[] rgb, float alpha, int light) {
		float c = Mth.cos(spin) * radius, s = Mth.sin(spin) * radius;
		vertex(vc, pose, -c + s, y, -s - c, rgb[0], rgb[1], rgb[2], alpha, 0, 0, light, 0, 1, 0);
		vertex(vc, pose, -c - s, y, -s + c, rgb[0], rgb[1], rgb[2], alpha, 0, 1, light, 0, 1, 0);
		vertex(vc, pose, c - s, y, s + c, rgb[0], rgb[1], rgb[2], alpha, 1, 1, light, 0, 1, 0);
		vertex(vc, pose, c + s, y, s - c, rgb[0], rgb[1], rgb[2], alpha, 1, 0, light, 0, 1, 0);
	}

	/**
	 * A crescent lying in the pose's xz-plane, its bow toward +z: from {@code -span/2} to {@code span/2} radians round a centre
	 * {@code radius} behind it, {@code width} deep (the texture's v from its outer edge in), {@code segments} along it.
	 */
	public static void crescent(VertexConsumer vc, PoseStack.Pose pose, float radius, float width, float span, int segments, float[] rgb, float alpha, int light) {
		for (int i = 0; i < segments; i++) {
			float a0 = -span / 2 + span * i / segments, a1 = -span / 2 + span * (i + 1) / segments;
			float taper0 = Mth.sin((float) Math.PI * i / segments), taper1 = Mth.sin((float) Math.PI * (i + 1) / segments);
			float ro0 = radius, ri0 = radius - width * (0.3f + 0.7f * taper0), ro1 = radius, ri1 = radius - width * (0.3f + 0.7f * taper1);
			float u0 = i / (float) segments, u1 = (i + 1) / (float) segments;
			vertex(vc, pose, Mth.sin(a0) * ro0, 0, Mth.cos(a0) * ro0 - radius, rgb[0], rgb[1], rgb[2], alpha, u0, 0, light, 0, 1, 0);
			vertex(vc, pose, Mth.sin(a0) * ri0, 0, Mth.cos(a0) * ri0 - radius, rgb[0], rgb[1], rgb[2], alpha, u0, 1, light, 0, 1, 0);
			vertex(vc, pose, Mth.sin(a1) * ri1, 0, Mth.cos(a1) * ri1 - radius, rgb[0], rgb[1], rgb[2], alpha, u1, 1, light, 0, 1, 0);
			vertex(vc, pose, Mth.sin(a1) * ro1, 0, Mth.cos(a1) * ro1 - radius, rgb[0], rgb[1], rgb[2], alpha, u1, 0, light, 0, 1, 0);
		}
	}

	/**
	 * A spike of rock on the pose's origin, {@code height} tall and {@code radius} round at its foot, leaning by {@code leanX, leanZ}: a spire of
	 * {@code sides} faces, closed all round, narrowing in three bands to its point, each ring a little jagged and turned, faced in {@code sprite}
	 * (a block's own texture) at about the block's own scale, so the rock reads as rock and never stretches. (It was a cone of three-sided
	 * pieces with a sliver missing between every two, a face's whole texture smeared up it: Earth Dragon Surge's "fucked up polygons", the
	 * user's report, 2026-10-26.)
	 */
	public static void spike(VertexConsumer vc, PoseStack.Pose pose, float radius, float height, float leanX, float leanZ, int sides, TextureAtlasSprite sprite,
							 float[] rgb, float alpha, int light, long seed) {
		if (height <= 0.01f || radius <= 0.01f) return;
		// The rings up it: their heights and how wide each is, as shares of the foot's; the last the point.
		float[] at = {0, 0.32f, 0.66f, 1}, wide = {1, 0.68f, 0.34f, 0};
		Vector3f[][] ring = new Vector3f[at.length][sides];
		for (int k = 0; k < at.length; k++) {
			float y = height * at[k];
			// Leaning more the higher it is (bowed, not tipped over whole).
			float cx = leanX * y * at[k], cz = leanZ * y * at[k];
			float twist = (hash(seed * 7 + k) - 0.5f) * 0.5f + (float) (seed % 7) * 0.1f;
			for (int s = 0; s < sides; s++) {
				double t = Math.PI * 2 * s / sides + twist;
				float r = radius * wide[k] * (0.82f + 0.36f * hash(seed * 31 + k * 13 + s));
				float jy = k == 0 || k == at.length - 1 ? 0 : (hash(seed * 17 + k * 5 + s) - 0.5f) * height * 0.08f;
				ring[k][s] = v(cx + (float) Math.cos(t) * r, y + jy, cz + (float) Math.sin(t) * r);
			}
		}
		for (int k = 0; k < at.length - 1; k++) {
			for (int s = 0; s < sides; s++) {
				int n = (s + 1) % sides;
				Vector3f a = ring[k][s], b = ring[k + 1][s], c = ring[k + 1][n], d = ring[k][n];
				// The block's own texture at about its own scale: a face's width and height in blocks, a sixteenth of the sprite a pixel.
				float across = Math.min(16, Math.max(2, a.distance(d) * 16)), up = Math.min(16, Math.max(2, (b.y - a.y) * 16));
				float u0 = 16 * hash(seed + k * 7 + s) * (1 - across / 16), v0 = 16 * hash(seed + k * 11 + s + 3) * (1 - up / 16);
				// Faces toward the light lighter, those away darker, the higher bands a little lighter (weathered).
				double mid = Math.PI * 2 * (s + 0.5) / sides;
				float shade = (0.72f + 0.2f * (float) Math.cos(mid - 0.8)) * (0.92f + 0.08f * k);
				float[] col = {rgb[0] * shade, rgb[1] * shade, rgb[2] * shade};
				if (k == at.length - 2) {
					// The point: three-sided (the quad's last two corners the same).
					quad(vc, pose, a, b, b, d, col, alpha, sprite.getU(u0), sprite.getV(v0 + up), sprite.getU(u0 + across), sprite.getV(v0), light);
				} else {
					quad(vc, pose, a, b, c, d, col, alpha, sprite.getU(u0), sprite.getV(v0 + up), sprite.getU(u0 + across), sprite.getV(v0), light);
				}
			}
		}
	}

	/** A number from 0 to 1 that a seed always gives the same (for shapes that must look the same every frame). */
	public static float hash(long seed) {
		long h = seed * 0x9E3779B97F4A7C15L;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return (h & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	/** The v range of frame {@code time} (ticks, at {@code fps} frames a tick) of an animated strip of {@code frames}. */
	public static float[] frame(float time, float rate, int frames) {
		int f = Math.floorMod((int) Math.floor(time * rate), frames);
		return new float[]{f / (float) frames, (f + 1) / (float) frames};
	}

	/** Eases 0..1 in and out. */
	public static float ease(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	/** Eases 0..1 out (fast, then settling). */
	public static float easeOut(float t) {
		t = Mth.clamp(t, 0, 1);
		return 1 - (1 - t) * (1 - t);
	}

	/** How much of something is shown at {@code age} of {@code life} ticks: growing in over {@code in}, fading out over {@code out}. */
	public static float fade(float age, float life, float in, float out) {
		float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
		float b = out <= 0 ? 1 : Mth.clamp((life - age) / out, 0, 1);
		return Math.min(a, b);
	}

	private SpellRender() {}
}
