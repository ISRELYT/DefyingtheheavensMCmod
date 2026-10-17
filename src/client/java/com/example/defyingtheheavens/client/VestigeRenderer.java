package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.VoidVestige;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A Vestige of the Void, drawn after the user's dragonfish: a long black eel's body that follows the very path its head has swum
 * (VoidVestige#trail), swaying as it goes and tapering to a finned tail; a blunt skull over a great lower jaw that juts out past
 * it, both lined with long glassy fangs, the jaw gaping and snapping shut when it bites; a huge pale glowing eye on each side; a
 * barbel hanging from its chin with a glowing lure; rows of violet lights along its belly; its black skin flecked with faint
 * stars, and a violet sheen round its edges where the light of the void catches it. Everything is built here, at its size.
 * <p>
 * Each kind of surface is drawn in its own pass (the render-buffer rule: a buffer is used through, then let go): the skin, then
 * the fangs and eyes' whites, then the skin's glints, then everything that glows.
 */
public class VestigeRenderer extends EntityRenderer<VoidVestige> {
	/** Body rings, sides to a ring. */
	private static final int RINGS = 40, SIDES = 16;
	/** The body's girth behind the head, and the head's size, in the creature's scale (VoidVestige#scale): a slim eel, a great head. */
	private static final double GIRTH = 0.6, HEAD = 0.9;
	private static ResourceLocation skin, glints, white;

	public VestigeRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0;
	}

	@Override
	public ResourceLocation getTextureLocation(VoidVestige entity) {
		return textures();
	}

	/** The skin (near-black, faint scales), its glints (black but for scattered specks of starlight), and plain white. */
	private static ResourceLocation textures() {
		if (skin == null) {
			RandomSource r = RandomSource.create(0x7E571CEL);
			NativeImage base = new NativeImage(NativeImage.Format.RGBA, 128, 128, false);
			NativeImage specks = new NativeImage(NativeImage.Format.RGBA, 128, 128, false);
			for (int y = 0; y < 128; y++) {
				for (int x = 0; x < 128; x++) {
					double scale = 0.5 + 0.5 * Math.sin(x * 0.7 + (y % 8 < 4 ? 0 : 2.2)) * Math.sin(y * 0.78);
					int k = (int) (6 + 6 * scale);
					base.setPixelRGBA(x, y, 0xFF000000 | (k + 6) << 16 | k << 8 | k);
					float roll = r.nextFloat();
					int speck = roll < 0.012f ? 0xFFFFE8E0 : roll < 0.03f ? 0xFFFF90B0 : roll < 0.04f ? 0xFFFFB070 : 0xFF000000;
					specks.setPixelRGBA(x, y, speck);
				}
			}
			NativeImage plain = new NativeImage(NativeImage.Format.RGBA, 4, 4, false);
			plain.fillRect(0, 0, 4, 4, 0xFFFFFFFF);
			skin = register("vestige_skin", base);
			glints = register("vestige_glints", specks);
			white = register("vestige_white", plain);
		}
		return skin;
	}

	private static ResourceLocation register(String name, NativeImage image) {
		ResourceLocation id = DefyingTheHeavens.id("astral/" + name);
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
		return id;
	}

	@Override
	public boolean shouldRender(VoidVestige entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
		return entity.shouldRenderAtSqrDistance(entity.distanceToSqr(x, y, z)) && frustum.isVisible(entity.getBoundingBoxForCulling());
	}

	@Override
	public void render(VoidVestige entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
		textures();
		double s = entity.scale();
		double length = entity.length();
		double r0 = GIRTH * s; // the body's girth behind the head
		double hr = HEAD * s; // the head's size: skull, jaw, fangs, eyes, lure
		Vec3 origin = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()),
				Mth.lerp(partialTick, entity.zo, entity.getZ()));
		Vec3 head = origin.add(0, entity.getBbHeight() / 2, 0);
		float time = entity.tickCount + partialTick;
		float fade = entity.deathTime > 0 ? Math.max(0, 1 - (entity.deathTime + partialTick) / 20f) : 1;
		if (fade <= 0) return;
		int overlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
		Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		// The spine: from the head back along the path it swam, evenly spaced, swaying side to side as an eel swims.
		Vec3 facing = Vec3.directionFromRotation(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()), Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()));
		Vec3[] spine = spine(entity, head, facing, length);
		Vec3[] tangent = new Vec3[RINGS + 1], side = new Vec3[RINGS + 1], up = new Vec3[RINGS + 1];
		frames(spine, facing, tangent, side, up);
		for (int i = 1; i <= RINGS; i++) {
			double t = i / (double) RINGS;
			double sway = Math.sin(time * 0.22 - t * 9) * r0 * 0.9 * t;
			spine[i] = spine[i].add(side[i].scale(sway));
		}
		frames(spine, facing, tangent, side, up);
		double[] radius = new double[RINGS + 1];
		for (int i = 0; i <= RINGS; i++) radius[i] = girth(i / (double) RINGS) * r0;
		lastSpine = spine;
		lastSide = side;
		lastUp = up;
		lastRadius = radius;
		lastR0 = r0;

		Vec3 f = tangent[0], u = up[0], rt = side[0];
		double jawOpen = jaw(entity.biteAnimation + partialTick, time);

		poseStack.pushPose();
		Matrix4f pose = poseStack.last().pose();
		Matrix3f normals = poseStack.last().normal();
		List<Vec3[]> skull = skull(head, f, u, rt, hr);
		List<Vec3[]> jawLoft = jawLoft(head, f, u, rt, hr, jawOpen);
		List<double[]> fins = fins(spine, side, up, radius);

		// 1. The skin: body, skull, jaw, fins.
		VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(skin));
		tube(vc, pose, normals, origin, spine, side, up, radius, 1.0, 0.05f, 0.05f, 0.07f, fade, packedLight, overlay, eye, false);
		loft(vc, pose, normals, origin, skull, 0.06f, 0.06f, 0.08f, fade, packedLight, overlay, eye, false);
		loft(vc, pose, normals, origin, jawLoft, 0.05f, 0.05f, 0.07f, fade, packedLight, overlay, eye, false);
		finSheets(vc, pose, normals, origin, fins, 0.07f, 0.05f, 0.12f, 0.75f * fade, packedLight, overlay);

		// 2. Its fangs and the whites of its eyes.
		vc = buffers.getBuffer(RenderType.entityCutoutNoCull(white));
		teeth(vc, pose, normals, origin, head, f, u, rt, hr, jawOpen, packedLight, overlay, fade);
		eyes(vc, pose, normals, origin, head, f, u, rt, hr, 0.8f, 0.85f, 0.9f, 1.0, LightTexture.FULL_BRIGHT, overlay, fade);

		// 3. Starlight caught in its skin.
		vc = buffers.getBuffer(SpellRender.glow(glints));
		tube(vc, pose, normals, origin, spine, side, up, radius, 1.002, 0.8f, 0.75f, 1f, 0.55f * fade, LightTexture.FULL_BRIGHT, overlay, eye, false);
		loft(vc, pose, normals, origin, skull, 0.8f, 0.75f, 1f, 0.55f * fade, LightTexture.FULL_BRIGHT, overlay, eye, false);

		// 4. All that glows: the violet sheen at its edges, its eyes, the lights along its belly, its lure, its fins' edges.
		vc = buffers.getBuffer(SpellRender.glow(white));
		tube(vc, pose, normals, origin, spine, side, up, radius, 1.05, 0.55f, 0.2f, 1f, 0.9f * fade, LightTexture.FULL_BRIGHT, overlay, eye, true);
		loft(vc, pose, normals, origin, skullAt(skull, 1.05, head), 0.55f, 0.2f, 1f, 0.9f * fade, LightTexture.FULL_BRIGHT, overlay, eye, true);
		eyes(vc, pose, normals, origin, head, f, u, rt, hr, 0.75f, 0.95f, 1f, 0.72, LightTexture.FULL_BRIGHT, overlay, fade);
		photophores(vc, pose, normals, origin, spine, side, up, radius, time, eye, fade, overlay);
		lure(vc, pose, normals, origin, head, f, u, rt, hr, jawOpen, time, eye, fade, overlay);
		finEdges(vc, pose, normals, origin, fins, fade, overlay, eye);
		poseStack.popPose();
		super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
	}

	// --- Its shape ---

	/** Its girth along it (0 the head, 1 the tail), as a share of the girth behind the head. */
	private static double girth(double t) {
		if (t < 0.08) return 0.9 + t * 1.25;
		return Math.max(0.06, 1.0 - 0.94 * Math.pow((t - 0.08) / 0.92, 1.35));
	}

	/** How wide its jaw gapes (radians): wide open, snapping shut as it bites and opening again; a slow working of the jaw otherwise. */
	private static double jaw(double sinceBite, double time) {
		double rest = 0.55 + 0.08 * Math.sin(time * 0.05);
		if (sinceBite < 3) return rest * (1 - sinceBite / 3);
		if (sinceBite < 14) return rest * ((sinceBite - 3) / 11.0);
		return rest;
	}

	/** {@link #RINGS} + 1 points evenly along {@code length}, from {@code head} back along its trail (straight back past its end). */
	private static Vec3[] spine(VoidVestige entity, Vec3 head, Vec3 facing, double length) {
		List<Vec3> path = new ArrayList<>();
		path.add(head);
		for (Vec3 p : entity.trail) if (p.distanceToSqr(path.get(path.size() - 1)) > 1e-4) path.add(p);
		Vec3[] out = new Vec3[RINGS + 1];
		double step = length / RINGS;
		int segment = 0;
		double into = 0;
		Vec3 back = facing.scale(-1);
		for (int i = 0; i <= RINGS; i++) {
			double want = i * step;
			// Walk the path to the point {@code want} along it.
			while (segment < path.size() - 1) {
				double seg = path.get(segment).distanceTo(path.get(segment + 1));
				if (into + seg >= want) break;
				into += seg;
				segment++;
			}
			if (segment < path.size() - 1) {
				Vec3 a = path.get(segment), b = path.get(segment + 1);
				double seg = a.distanceTo(b);
				out[i] = seg < 1e-6 ? a : a.add(b.subtract(a).scale((want - into) / seg));
			} else {
				Vec3 end = path.get(path.size() - 1);
				Vec3 way = path.size() > 1 ? end.subtract(path.get(path.size() - 2)).normalize() : back;
				if (way.lengthSqr() < 1e-6) way = back;
				out[i] = end.add(way.scale(want - into));
			}
		}
		return out;
	}

	/** Each ring's frame: its tangent (toward the head), side and up, carried down the body without twisting. */
	private static void frames(Vec3[] spine, Vec3 facing, Vec3[] tangent, Vec3[] side, Vec3[] up) {
		for (int i = 0; i <= RINGS; i++) {
			Vec3 t = i == 0 ? facing : spine[i - 1].subtract(spine[i]);
			if (t.lengthSqr() < 1e-8) t = i == 0 ? facing : tangent[i - 1];
			tangent[i] = t.normalize();
		}
		Vec3 ref = Math.abs(tangent[0].y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		Vec3 s = tangent[0].cross(ref).normalize();
		for (int i = 0; i <= RINGS; i++) {
			// Parallel transport: the last side, made square to this tangent.
			s = s.subtract(tangent[i].scale(s.dot(tangent[i])));
			if (s.lengthSqr() < 1e-8) s = tangent[i].cross(ref);
			s = s.normalize();
			side[i] = s;
			up[i] = s.cross(tangent[i]).normalize();
		}
	}

	/** The skull: rings from behind the eyes to the blunt snout, each [centre, side axis, up axis] (half-width and half-height built in). */
	private static List<Vec3[]> skull(Vec3 head, Vec3 f, Vec3 u, Vec3 r, double r0) {
		double[][] rings = {
				// along, up, half-width, half-height
				{-0.2, 0.05, 0.95, 0.98}, {0.4, 0.12, 0.92, 1.0}, {1.0, 0.16, 0.8, 0.9}, {1.6, 0.14, 0.62, 0.72}, {2.1, 0.08, 0.42, 0.5},
				{2.45, 0.0, 0.22, 0.28}, {2.6, -0.04, 0.05, 0.06}};
		List<Vec3[]> out = new ArrayList<>();
		for (double[] ring : rings) {
			Vec3 c = head.add(f.scale(ring[0] * r0)).add(u.scale(ring[1] * r0));
			out.add(new Vec3[] {c, r.scale(ring[2] * r0), u.scale(ring[3] * r0)});
		}
		return out;
	}

	/** The same rings grown by {@code by} about the head's axis (the sheen's shell). */
	private static List<Vec3[]> skullAt(List<Vec3[]> skull, double by, Vec3 head) {
		List<Vec3[]> out = new ArrayList<>();
		for (Vec3[] ring : skull) out.add(new Vec3[] {ring[0], ring[1].scale(by), ring[2].scale(by)});
		return out;
	}

	/** The lower jaw: hinged under the back of the skull, opened by {@code open}, jutting well past the snout; a thin, deep wedge. */
	private static List<Vec3[]> jawLoft(Vec3 head, Vec3 f, Vec3 u, Vec3 r, double r0, double open) {
		Vec3 hinge = head.add(f.scale(0.1 * r0)).add(u.scale(-0.55 * r0));
		Vec3 jf = f.scale(Math.cos(open)).add(u.scale(-Math.sin(open)));
		Vec3 ju = u.scale(Math.cos(open)).add(f.scale(Math.sin(open)));
		double[][] rings = {{0, 0.82, 0.42}, {0.8, 0.78, 0.38}, {1.6, 0.66, 0.32}, {2.4, 0.5, 0.26}, {3.0, 0.32, 0.2}, {3.3, 0.12, 0.12}, {3.4, 0.03, 0.03}};
		List<Vec3[]> out = new ArrayList<>();
		for (double[] ring : rings) {
			Vec3 c = hinge.add(jf.scale(ring[0] * r0)).add(ju.scale(-0.1 * r0 * ring[0] / 3.4));
			out.add(new Vec3[] {c, r.scale(ring[1] * r0), ju.scale(ring[2] * r0)});
		}
		return out;
	}

	/** Fins: [ring index, out along side (+1/-1) or up (0), length] for pectorals behind the head, and the dorsal and belly fins near the tail. */
	private static List<double[]> fins(Vec3[] spine, Vec3[] side, Vec3[] up, double[] radius) {
		List<double[]> out = new ArrayList<>();
		out.add(new double[] {3, 1, 2.2});
		out.add(new double[] {3, -1, 2.2});
		out.add(new double[] {31, 2, 1.8});
		out.add(new double[] {32, -2, 1.6});
		out.add(new double[] {RINGS - 1, 3, 2.4});
		return out;
	}

	// --- Drawing ---

	/** The body's tube: rings of {@code radius} (times {@code inflate}) round the spine; with {@code rim}, coloured only where seen edge-on. */
	private static void tube(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3[] spine, Vec3[] side, Vec3[] up, double[] radius,
			double inflate, float r, float g, float b, float a, int light, int overlay, Vec3 eye, boolean rim) {
		for (int i = 0; i < RINGS; i++) {
			for (int k = 0; k < SIDES; k++) {
				double a0 = k / (double) SIDES * Math.PI * 2, a1 = (k + 1) / (double) SIDES * Math.PI * 2;
				Vec3 n00 = side[i].scale(Math.cos(a0)).add(up[i].scale(Math.sin(a0)));
				Vec3 n01 = side[i].scale(Math.cos(a1)).add(up[i].scale(Math.sin(a1)));
				Vec3 n10 = side[i + 1].scale(Math.cos(a0)).add(up[i + 1].scale(Math.sin(a0)));
				Vec3 n11 = side[i + 1].scale(Math.cos(a1)).add(up[i + 1].scale(Math.sin(a1)));
				// The skin's pattern several times round and along: fine scales, small glints.
				float u0 = k * 3f / SIDES, u1 = (k + 1) * 3f / SIDES, v0 = i / 1.5f, v1 = (i + 1) / 1.5f;
				// Round first, then down the body: counter-clockwise seen from outside, so it faces out.
				vertex(vc, pose, normals, origin, spine[i].add(n00.scale(radius[i] * inflate)), n00, u0, v0, r, g, b, a, light, overlay, eye, rim);
				vertex(vc, pose, normals, origin, spine[i].add(n01.scale(radius[i] * inflate)), n01, u1, v0, r, g, b, a, light, overlay, eye, rim);
				vertex(vc, pose, normals, origin, spine[i + 1].add(n11.scale(radius[i + 1] * inflate)), n11, u1, v1, r, g, b, a, light, overlay, eye, rim);
				vertex(vc, pose, normals, origin, spine[i + 1].add(n10.scale(radius[i + 1] * inflate)), n10, u0, v1, r, g, b, a, light, overlay, eye, rim);
			}
		}
	}

	/** A shape lofted through elliptical rings ([centre, side half-axis, up half-axis]), its ends closed by the rings narrowing. */
	private static void loft(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, List<Vec3[]> rings, float r, float g, float b, float a,
			int light, int overlay, Vec3 eye, boolean rim) {
		for (int i = 0; i < rings.size() - 1; i++) {
			Vec3[] p = rings.get(i), q = rings.get(i + 1);
			for (int k = 0; k < SIDES; k++) {
				double a0 = k / (double) SIDES * Math.PI * 2, a1 = (k + 1) / (double) SIDES * Math.PI * 2;
				Vec3 p0 = p[0].add(p[1].scale(Math.cos(a0))).add(p[2].scale(Math.sin(a0)));
				Vec3 p1 = p[0].add(p[1].scale(Math.cos(a1))).add(p[2].scale(Math.sin(a1)));
				Vec3 q0 = q[0].add(q[1].scale(Math.cos(a0))).add(q[2].scale(Math.sin(a0)));
				Vec3 q1 = q[0].add(q[1].scale(Math.cos(a1))).add(q[2].scale(Math.sin(a1)));
				Vec3 n0 = p[1].normalize().scale(Math.cos(a0)).add(p[2].normalize().scale(Math.sin(a0)));
				Vec3 n1 = p[1].normalize().scale(Math.cos(a1)).add(p[2].normalize().scale(Math.sin(a1)));
				float u0 = k / (float) SIDES, u1 = (k + 1) / (float) SIDES, v0 = i / 4f, v1 = (i + 1) / 4f;
				// Forward first, then round: counter-clockwise seen from outside, so it faces out.
				vertex(vc, pose, normals, origin, p0, n0, u0, v0, r, g, b, a, light, overlay, eye, rim);
				vertex(vc, pose, normals, origin, q0, n0, u0, v1, r, g, b, a, light, overlay, eye, rim);
				vertex(vc, pose, normals, origin, q1, n1, u1, v1, r, g, b, a, light, overlay, eye, rim);
				vertex(vc, pose, normals, origin, p1, n1, u1, v0, r, g, b, a, light, overlay, eye, rim);
			}
		}
	}

	/**
	 * One vertex, its position given in the world and drawn relative to the entity. {@code rim}: only as bright as it is seen
	 * edge-on (its normal across the line of sight), the void's light catching its outline.
	 */
	private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 at, Vec3 normal, float u, float v,
			float r, float g, float b, float a, int light, int overlay, Vec3 eye, boolean rim) {
		float k = 1;
		if (rim) {
			Vec3 view = at.subtract(eye).normalize();
			double edge = 1 - Math.abs(view.dot(normal));
			k = (float) (edge * edge * edge);
		}
		Vec3 p = at.subtract(origin);
		vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z).color(r * k, g * k, b * k, a * (rim ? k : 1)).uv(u, v).overlayCoords(overlay).uv2(light)
				.normal(normals, (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
	}

	/** Long glassy fangs: up from the lower jaw's edges (longest at the front), down from the skull's. */
	private static void teeth(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 head, Vec3 f, Vec3 u, Vec3 r, double r0, double open,
			int light, int overlay, float fade) {
		Vec3 hinge = head.add(f.scale(0.1 * r0)).add(u.scale(-0.55 * r0));
		Vec3 jf = f.scale(Math.cos(open)).add(u.scale(-Math.sin(open)));
		Vec3 ju = u.scale(Math.cos(open)).add(f.scale(Math.sin(open)));
		for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
			// The lower jaw's fangs: few, long and needle-thin, the front ones longest, leaning forward.
			for (int i = 0; i < 7; i++) {
				double along = 0.6 + i * 0.4;
				double width = 0.78 - along * 0.17;
				Vec3 base = hinge.add(jf.scale(along * r0)).add(r.scale(sideSign * width * 0.85 * r0)).add(ju.scale(0.3 * r0));
				double length = (0.55 + 0.95 * (i / 6.0)) * r0;
				Vec3 tip = base.add(ju.scale(length)).add(jf.scale(0.35 * length)).add(r.scale(-sideSign * 0.1 * length));
				fang(vc, pose, normals, origin, base, tip, jf, r, 0.04 * r0, light, overlay, fade);
			}
			for (int i = 0; i < 4; i++) {
				double along = 0.9 + i * 0.4;
				Vec3 base = head.add(f.scale(along * r0)).add(r.scale(sideSign * (0.7 - along * 0.18) * r0)).add(u.scale(-0.45 * r0));
				double length = (0.55 + 0.4 * (i / 3.0)) * r0;
				Vec3 tip = base.add(u.scale(-length)).add(f.scale(0.22 * length));
				fang(vc, pose, normals, origin, base, tip, f, r, 0.035 * r0, light, overlay, fade);
			}
		}
	}

	/** A fang: a thin four-sided spike from {@code base} to {@code tip}. */
	private static void fang(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 base, Vec3 tip, Vec3 a, Vec3 b, double width,
			int light, int overlay, float fade) {
		Vec3[] corners = {base.add(a.scale(width)), base.add(b.scale(width)), base.add(a.scale(-width)), base.add(b.scale(-width))};
		for (int i = 0; i < 4; i++) {
			Vec3 c0 = corners[i], c1 = corners[(i + 1) % 4];
			Vec3 n = c1.subtract(c0).cross(tip.subtract(c0)).normalize();
			plain(vc, pose, normals, origin, c0, n, 0.86f, 0.88f, 0.92f, fade, light, overlay);
			plain(vc, pose, normals, origin, c1, n, 0.86f, 0.88f, 0.92f, fade, light, overlay);
			plain(vc, pose, normals, origin, tip, n, 0.95f, 0.95f, 1f, fade, light, overlay);
			plain(vc, pose, normals, origin, tip, n, 0.95f, 0.95f, 1f, fade, light, overlay);
		}
	}

	/** Its eyes: a disc on each side of the skull ({@code size}: of the full eye; the glow draws a smaller, brighter heart). */
	private static void eyes(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 head, Vec3 f, Vec3 u, Vec3 r, double r0,
			float cr, float cg, float cb, double size, int light, int overlay, float fade) {
		for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
			Vec3 out = r.scale(sideSign);
			Vec3 centre = head.add(f.scale(1.25 * r0)).add(u.scale(0.25 * r0)).add(out.scale(0.8 * r0));
			double radius = 0.42 * r0 * size;
			int n = 16;
			for (int i = 0; i < n; i++) {
				double a0 = i / (double) n * Math.PI * 2, a1 = (i + 1) / (double) n * Math.PI * 2;
				Vec3 p0 = centre.add(f.scale(Math.cos(a0) * radius)).add(u.scale(Math.sin(a0) * radius));
				Vec3 p1 = centre.add(f.scale(Math.cos(a1) * radius)).add(u.scale(Math.sin(a1) * radius));
				plain(vc, pose, normals, origin, centre.add(out.scale(0.08 * r0)), out, cr, cg, cb, fade, light, overlay);
				plain(vc, pose, normals, origin, p0, out, cr * 0.85f, cg * 0.85f, cb * 0.9f, fade, light, overlay);
				plain(vc, pose, normals, origin, p1, out, cr * 0.85f, cg * 0.85f, cb * 0.9f, fade, light, overlay);
				plain(vc, pose, normals, origin, centre.add(out.scale(0.08 * r0)), out, cr, cg, cb, fade, light, overlay);
			}
		}
	}

	/** Lights along its belly, two rows, pulsing slowly down its length. */
	private static void photophores(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3[] spine, Vec3[] side, Vec3[] up, double[] radius,
			float time, Vec3 eye, float fade, int overlay) {
		for (int i = 2; i < RINGS - 6; i++) {
			float pulse = 0.55f + 0.45f * Mth.sin(time * 0.15f - i * 0.5f);
			for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
				Vec3 normal = up[i].scale(-0.85).add(side[i].scale(sideSign * 0.5)).normalize();
				Vec3 at = spine[i].add(normal.scale(radius[i] * 1.02));
				double size = Math.max(0.12, radius[i] * 0.16);
				billboard(vc, pose, normals, origin, at, eye, size, 0.55f * pulse, 0.5f * pulse, 1f * pulse, fade, overlay);
			}
		}
	}

	/** The barbel under its chin, swaying, and the glowing lure at its end. */
	private static void lure(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 head, Vec3 f, Vec3 u, Vec3 r, double r0, double open,
			float time, Vec3 eye, float fade, int overlay) {
		Vec3 hinge = head.add(f.scale(0.1 * r0)).add(u.scale(-0.55 * r0));
		Vec3 jf = f.scale(Math.cos(open)).add(u.scale(-Math.sin(open)));
		Vec3 start = hinge.add(jf.scale(2.6 * r0)).add(u.scale(-0.35 * r0));
		Vec3 last = start;
		int n = 10;
		for (int i = 1; i <= n; i++) {
			double t = i / (double) n;
			Vec3 at = start.add(u.scale(-3.2 * r0 * t)).add(f.scale(-1.2 * r0 * t * t)).add(r.scale(Math.sin(time * 0.1 + t * 3) * 0.4 * r0 * t));
			segment(vc, pose, normals, origin, last, at, eye, 0.035 * r0, 0.45f, 0.3f, 0.65f, fade, overlay);
			last = at;
		}
		float pulse = 0.75f + 0.25f * Mth.sin(time * 0.2f);
		billboard(vc, pose, normals, origin, last, eye, 0.45 * r0, 0.85f * pulse, 0.75f * pulse, 1f * pulse, fade, overlay);
		billboard(vc, pose, normals, origin, last, eye, 1.4 * r0, 0.3f * pulse, 0.15f * pulse, 0.5f * pulse, fade, overlay);
	}

	/** Its fins as sheets: each a fan from its root along the body out to its tip. */
	private static void finSheets(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, List<double[]> fins, float r, float g, float b, float a,
			int light, int overlay) {
		for (double[] fin : fins) {
			Vec3[] q = finShape(fin);
			if (q == null) continue;
			Vec3 n = q[1].subtract(q[0]).cross(q[2].subtract(q[0])).normalize();
			for (int pass = 0; pass < 2; pass++) {
				Vec3 nn = pass == 0 ? n : n.scale(-1);
				plain(vc, pose, normals, origin, q[0], nn, r, g, b, a, light, overlay);
				plain(vc, pose, normals, origin, q[1], nn, r, g, b, a, light, overlay);
				plain(vc, pose, normals, origin, q[2], nn, r * 1.5f, g * 1.5f, b * 1.5f, a, light, overlay);
				plain(vc, pose, normals, origin, q[3], nn, r, g, b, a, light, overlay);
			}
		}
	}

	/** Their glowing edges. */
	private static void finEdges(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, List<double[]> fins, float fade, int overlay, Vec3 eye) {
		for (double[] fin : fins) {
			Vec3[] q = finShape(fin);
			if (q == null) continue;
			double w = Math.max(0.05, q[0].distanceTo(q[2]) * 0.025);
			segment(vc, pose, normals, origin, q[1], q[2], eye, w, 0.6f, 0.25f, 1f, fade, overlay);
			segment(vc, pose, normals, origin, q[2], q[3], eye, w, 0.6f, 0.25f, 1f, fade, overlay);
		}
	}

	/** A fin's four corners (root front, root back, tip, trailing tip), worked out from the last drawn spine (null if out of range). */
	private static Vec3[] finShape(double[] fin) {
		Vec3[] spine = lastSpine, side = lastSide, up = lastUp;
		double[] radius = lastRadius;
		if (spine == null) return null;
		int i = (int) fin[0];
		if (i < 1 || i >= RINGS) return null;
		Vec3 out = switch ((int) fin[1]) {
			case 1 -> side[i].add(up[i].scale(-0.3)).normalize();
			case -1 -> side[i].scale(-1).add(up[i].scale(-0.3)).normalize();
			case 2 -> up[i];
			case -2 -> up[i].scale(-1);
			default -> up[i].scale(0.6);
		};
		double size = fin[2] * lastR0 * (fin[1] == 3 ? 0.9 : 0.7);
		Vec3 front = spine[Math.max(0, i - 1)].add(out.scale(radius[i] * 0.8));
		Vec3 back = spine[Math.min(RINGS, i + 2)].add(out.scale(radius[Math.min(RINGS, i + 2)] * 0.8));
		Vec3 along = back.subtract(front).normalize();
		Vec3 tip = front.add(out.scale(size)).add(along.scale(size * 0.6));
		Vec3 trailing = back.add(out.scale(size * 0.55)).add(along.scale(size * 0.4));
		return new Vec3[] {front, back, trailing.add(tip).scale(0.5).add(out.scale(size * 0.2)), trailing};
	}

	/** The spine last drawn (render is on the one render thread), for the fins worked out from it. */
	private static Vec3[] lastSpine, lastSide, lastUp;
	private static double[] lastRadius;
	private static double lastR0;

	/** A thin glowing strip from {@code a} to {@code b}, turned to face the eye. */
	private static void segment(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 a, Vec3 b, Vec3 eye, double width,
			float r, float g, float bl, float fade, int overlay) {
		Vec3 along = b.subtract(a);
		Vec3 across = along.cross(a.subtract(eye)).normalize().scale(width);
		if (!Double.isFinite(across.x)) return;
		Vec3 n = a.subtract(eye).normalize().scale(-1);
		plain(vc, pose, normals, origin, a.add(across), n, r, g, bl, fade, LightTexture.FULL_BRIGHT, overlay);
		plain(vc, pose, normals, origin, b.add(across), n, r, g, bl, fade, LightTexture.FULL_BRIGHT, overlay);
		plain(vc, pose, normals, origin, b.subtract(across), n, r, g, bl, fade, LightTexture.FULL_BRIGHT, overlay);
		plain(vc, pose, normals, origin, a.subtract(across), n, r, g, bl, fade, LightTexture.FULL_BRIGHT, overlay);
	}

	/** A small square facing the eye (a light). */
	private static void billboard(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 at, Vec3 eye, double size,
			float r, float g, float b, float fade, int overlay) {
		Vec3 view = at.subtract(eye).normalize();
		Vec3 right = view.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1e-6) right = view.cross(new Vec3(1, 0, 0));
		right = right.normalize().scale(size / 2);
		Vec3 upv = right.cross(view).normalize().scale(size / 2);
		Vec3 n = view.scale(-1);
		plain(vc, pose, normals, origin, at.subtract(right).subtract(upv), n, r, g, b, fade, LightTexture.FULL_BRIGHT, overlay);
		plain(vc, pose, normals, origin, at.add(right).subtract(upv), n, r, g, b, fade, LightTexture.FULL_BRIGHT, overlay);
		plain(vc, pose, normals, origin, at.add(right).add(upv), n, r, g, b, fade, LightTexture.FULL_BRIGHT, overlay);
		plain(vc, pose, normals, origin, at.subtract(right).add(upv), n, r, g, b, fade, LightTexture.FULL_BRIGHT, overlay);
	}

	private static void plain(VertexConsumer vc, Matrix4f pose, Matrix3f normals, Vec3 origin, Vec3 at, Vec3 n, float r, float g, float b, float a,
			int light, int overlay) {
		Vec3 p = at.subtract(origin);
		vc.vertex(pose, (float) p.x, (float) p.y, (float) p.z).color(r, g, b, a).uv(0.5f, 0.5f).overlayCoords(overlay).uv2(light)
				.normal(normals, (float) n.x, (float) n.y, (float) n.z).endVertex();
	}
}
