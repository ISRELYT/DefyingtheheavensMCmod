package com.example.defyingtheheavens.client.fx;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Drawing the effects (the VFX plan's toolkit), into the effects' layer ({@link FxLayer}): batches of textured quads through the
 * {@code fx} core shader, one texture and one {@link Mode} a batch, every place in world coordinates (made camera-relative here), and
 * nothing allocated per quad. The shapes: sprites facing the camera, streaks along a line, ribbons along a path (facing the camera, or
 * lying on a plane), flat squares, rings and squares laid over the ground's heights, standing walls, and tubes of light whose faces fade
 * as they turn side-on.
 */
public final class Fx {
	/** How a batch is drawn: light in the texture's colours, light through the heat ramp (white-hot cores), or covering (smoke, cloud). */
	public enum Mode {
		LIGHT(0), HEAT(1), COVER(2);

		final float id;

		Mode(float id) {
			this.id = id;
		}
	}

	private static ShaderInstance shader;

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
				context.register(DefyingTheHeavens.id("fx"), DefaultVertexFormat.POSITION_COLOR_TEX, s -> shader = s));
	}

	/** Whether the shader loaded (else nothing of the effects is drawn). */
	public static boolean ready() {
		return shader != null;
	}

	// --- The frame's camera ---

	private static Matrix4f pose;
	public static double camX, camY, camZ;
	/** The camera's axes: to its left, up the screen, and where it looks. */
	public static float leftX, leftY, leftZ, upX, upY, upZ, lookX, lookY, lookZ;

	static void frame(WorldRenderContext context) {
		pose = context.matrixStack().last().pose();
		Camera camera = context.camera();
		Vec3 at = camera.getPosition();
		camX = at.x;
		camY = at.y;
		camZ = at.z;
		Vector3f l = camera.getLeftVector(), u = camera.getUpVector(), f = camera.getLookVector();
		leftX = l.x();
		leftY = l.y();
		leftZ = l.z();
		upX = u.x();
		upY = u.y();
		upZ = u.z();
		lookX = f.x();
		lookY = f.y();
		lookZ = f.z();
	}

	/** How far (along the view) a point is in front of the camera; below 0 behind it. */
	public static double depthOf(double x, double y, double z) {
		return (x - camX) * lookX + (y - camY) * lookY + (z - camZ) * lookZ;
	}

	public static double distanceTo(double x, double y, double z) {
		double dx = x - camX, dy = y - camY, dz = z - camZ;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	// --- Batches ---

	private static BufferBuilder buf;
	private static int count;

	/** Starts a batch: {@code texture} drawn in {@code mode}, its light times {@code gain} (light past white for the brightest parts). */
	public static void begin(ResourceLocation texture, Mode mode, float gain) {
		end();
		FxTextures.bind(texture);
		RenderSystem.setShader(() -> shader);
		shader.safeGetUniform("FxMode").set(mode.id);
		shader.safeGetUniform("FxGain").set(gain);
		if (mode == Mode.COVER) {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		} else {
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		}
		buf = Tesselator.getInstance().getBuilder();
		buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		count = 0;
	}

	/** Draws the batch begun (nothing if none is open). */
	public static void end() {
		if (buf == null) return;
		BufferBuilder b = buf;
		buf = null;
		if (count == 0) b.end().release();
		else BufferUploader.drawWithShader(b.end());
	}

	/**
	 * After the layer's painters, whatever happened: the shared builder freed for its next user if anyone left it building (a batch of
	 * ours, or a body drawn into the layer by SoulRenderer, let go part-way by a failure), so nothing else that draws through it trips.
	 */
	static void abandon() {
		buf = null;
		BufferBuilder shared = Tesselator.getInstance().getBuilder();
		if (shared.building()) shared.end().release();
	}

	// --- When drawing the effects fails ---

	private static final java.util.List<java.util.function.Consumer<RuntimeException>> ON_FAILURE = new java.util.ArrayList<>();
	private static int failures;

	/** Told when the layer or the post chain fails part-way (the cinematics: every one ends and everything is put back). */
	public static void onFailure(java.util.function.Consumer<RuntimeException> listener) {
		ON_FAILURE.add(listener);
	}

	/** The layer's set-up or the post chain threw: logged (in full the first time, a line after), and its owners told. */
	static void failed(String where, RuntimeException e) {
		if (failures++ == 0) DefyingTheHeavens.LOGGER.error("[DTH-FX] {} failed: put back, and the cinematics ended", where, e);
		else DefyingTheHeavens.LOGGER.error("[DTH-FX] {} failed again ({} times): {}", where, failures, e.toString());
		for (var listener : ON_FAILURE) {
			try {
				listener.accept(e);
			} catch (RuntimeException again) {
				DefyingTheHeavens.LOGGER.error("[DTH-FX] a failure's listener failed too", again);
			}
		}
	}

	// --- Development only (-Ddth.dev=true): a failure forced once, where the capture harness asks (the failure-safety test) ---

	private static String devThrow;

	/** The next frame's drawing throws once at {@code where}: "painter" (inside the cinematics' painter) or "post" (inside the chain). */
	public static void devThrowOnce(String where) {
		if (com.example.defyingtheheavens.DevTools.ENABLED) devThrow = where;
	}

	/** Whether to throw at {@code where} now (once). */
	public static boolean devThrowNow(String where) {
		if (devThrow == null || !devThrow.equals(where)) return false;
		devThrow = null;
		return true;
	}

	/**
	 * The blend functions kept: every core shader carries a blend mode, and applying one with another mode than the last one applied
	 * resets the blend functions. The cache's last mode is set to vanilla's (by way of an opaque one, which always takes), so the fx
	 * shader, which carries vanilla's, leaves the functions {@link #begin} sets alone.
	 */
	public static void primeBlendMode() {
		OPAQUE.apply();
		ShaderInstance vanilla = net.minecraft.client.renderer.GameRenderer.getPositionColorShader();
		if (vanilla == null) return;
		vanilla.apply();
		vanilla.clear();
	}

	private static final com.mojang.blaze3d.shaders.BlendMode OPAQUE = new com.mojang.blaze3d.shaders.BlendMode();

	public static void v(double x, double y, double z, float u, float v, float r, float g, float b, float a) {
		buf.vertex(pose, (float) (x - camX), (float) (y - camY), (float) (z - camZ))
				.color(clamp(r), clamp(g), clamp(b), clamp(a)).uv(u, v).endVertex();
		count++;
	}

	private static float clamp(float f) {
		return f < 0 ? 0 : f > 1 ? 1 : f;
	}

	// --- Shapes ---

	// --- How many sprites and streaks (the effects' particles) the last frame's layer drew: the capture tool counts them (VfxRecorder) ---

	private static int sprites, spritesLast;

	/** A frame's layer begins (FxLayer, every frame, drawn or not). */
	static void newFrame() {
		spritesLast = sprites;
		sprites = 0;
	}

	public static int spritesLastFrame() {
		return spritesLast;
	}

	/** A square facing the camera, {@code half} its half-size, turned {@code spin} radians on the screen. */
	public static void sprite(double x, double y, double z, float half, float spin, float r, float g, float b, float a) {
		if (a <= 0 || half <= 0) return;
		sprites++;
		float c = Mth.cos(spin) * half, s = Mth.sin(spin) * half;
		double ax = -leftX * c + upX * s, ay = -leftY * c + upY * s, az = -leftZ * c + upZ * s; // the square's right
		double bx = leftX * s + upX * c, by = leftY * s + upY * c, bz = leftZ * s + upZ * c; // and its up
		v(x - ax - bx, y - ay - by, z - az - bz, 0, 1, r, g, b, a);
		v(x + ax - bx, y + ay - by, z + az - bz, 1, 1, r, g, b, a);
		v(x + ax + bx, y + ay + by, z + az + bz, 1, 0, r, g, b, a);
		v(x - ax + bx, y - ay + by, z - az + bz, 0, 0, r, g, b, a);
	}

	/** A strip from (x0, y0, z0) to (x1, y1, z1), {@code w} wide, facing the camera: u along it (1 at the end), v across. */
	public static void streak(double x0, double y0, double z0, double x1, double y1, double z1, float w, float r, float g, float b, float a) {
		if (a <= 0) return;
		sprites++;
		double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		double tx = camX - x0, ty = camY - y0, tz = camZ - z0;
		double sx = dy * tz - dz * ty, sy = dz * tx - dx * tz, sz = dx * ty - dy * tx;
		double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (len < 1.0E-9) return;
		double k = w * 0.5 / len;
		sx *= k;
		sy *= k;
		sz *= k;
		v(x0 - sx, y0 - sy, z0 - sz, 0, 0, r, g, b, a);
		v(x0 + sx, y0 + sy, z0 + sz, 0, 1, r, g, b, a);
		v(x1 + sx, y1 + sy, z1 + sz, 1, 1, r, g, b, a);
		v(x1 - sx, y1 - sy, z1 - sz, 1, 0, r, g, b, a);
	}

	/** A flat square lying level at height {@code y}, {@code half} its half-size, turned {@code turn} radians about the vertical. */
	public static void flat(double x, double y, double z, float half, float turn, float r, float g, float b, float a) {
		if (a <= 0 || half <= 0) return;
		float c = Mth.cos(turn) * half, s = Mth.sin(turn) * half;
		v(x - c + s, y, z - s - c, 0, 0, r, g, b, a);
		v(x + c + s, y, z + s - c, 1, 0, r, g, b, a);
		v(x + c - s, y, z + s + c, 1, 1, r, g, b, a);
		v(x - c - s, y, z - s + c, 0, 1, r, g, b, a);
	}

	// --- Ribbons: a path of points, each with its width, its v (along the texture) and its colour ---

	private static final int MAX_POINTS = 1024;
	private static final double[] PX = new double[MAX_POINTS], PY = new double[MAX_POINTS], PZ = new double[MAX_POINTS];
	private static final float[] PW = new float[MAX_POINTS], PV = new float[MAX_POINTS], PR = new float[MAX_POINTS], PG = new float[MAX_POINTS],
			PB = new float[MAX_POINTS], PA = new float[MAX_POINTS];
	private static int points;

	public static void path() {
		points = 0;
	}

	public static void point(double x, double y, double z, float width, float v, float r, float g, float b, float a) {
		if (points >= MAX_POINTS) return;
		PX[points] = x;
		PY[points] = y;
		PZ[points] = z;
		PW[points] = width;
		PV[points] = v;
		PR[points] = r;
		PG[points] = g;
		PB[points] = b;
		PA[points] = a;
		points++;
	}

	/** The path as a strip turned to face the camera (u across it, 0 to 1). */
	public static void ribbon() {
		strip(Double.NaN, 0, 0);
	}

	/** The path as a strip lying in the plane whose normal is (nx, ny, nz) (cracks on the ground, veins across a cloud base). */
	public static void ribbonOnPlane(double nx, double ny, double nz) {
		strip(nx, ny, nz);
	}

	private static void strip(double nx, double ny, double nz) {
		if (points < 2) return;
		double ps = 0, qs = 0, rs = 0; // the last point's side, carried on where a side can't be found
		for (int i = 0; i < points; i++) {
			int a = Math.max(0, i - 1), b = Math.min(points - 1, i + 1);
			double tx = PX[b] - PX[a], ty = PY[b] - PY[a], tz = PZ[b] - PZ[a];
			double ox, oy, oz;
			if (Double.isNaN(nx)) {
				ox = camX - PX[i];
				oy = camY - PY[i];
				oz = camZ - PZ[i];
			} else {
				ox = nx;
				oy = ny;
				oz = nz;
			}
			double sx = ty * oz - tz * oy, sy = tz * ox - tx * oz, sz = tx * oy - ty * ox;
			double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
			if (len > 1.0E-9) {
				ps = sx / len;
				qs = sy / len;
				rs = sz / len;
			}
			double h = PW[i] * 0.5;
			double x0 = PX[i] - ps * h, y0 = PY[i] - qs * h, z0 = PZ[i] - rs * h;
			double x1 = PX[i] + ps * h, y1 = PY[i] + qs * h, z1 = PZ[i] + rs * h;
			if (i > 0 && (PA[i] > 0 || PA[i - 1] > 0)) {
				v(LX0, LY0, LZ0, 0, PV[i - 1], PR[i - 1], PG[i - 1], PB[i - 1], PA[i - 1]);
				v(LX1, LY1, LZ1, 1, PV[i - 1], PR[i - 1], PG[i - 1], PB[i - 1], PA[i - 1]);
				v(x1, y1, z1, 1, PV[i], PR[i], PG[i], PB[i], PA[i]);
				v(x0, y0, z0, 0, PV[i], PR[i], PG[i], PB[i], PA[i]);
			}
			LX0 = x0;
			LY0 = y0;
			LZ0 = z0;
			LX1 = x1;
			LY1 = y1;
			LZ1 = z1;
		}
		points = 0;
	}

	/** The last point's two edges, carried to the next quad. */
	private static double LX0, LY0, LZ0, LX1, LY1, LZ1;

	// --- On the ground ---

	/**
	 * A ring on the ground about (x, z) from radius {@code r0} to {@code r1}, every point set {@code lift} over the ground's heights; u
	 * round it ({@code uTimes} repeats, shifted by {@code uShift}), v across it (0 at r0, 1 at r1); alpha {@code a0} at r0 and {@code a1} at r1.
	 */
	public static void groundRing(HeightField ground, double x, double z, float r0, float r1, int sides, float uTimes, float uShift, float lift,
								  float r, float g, float b, float a0, float a1) {
		if (r1 <= 0 || a0 <= 0 && a1 <= 0) return;
		for (int i = 0; i < sides; i++) {
			double t0 = i * Math.PI * 2 / sides, t1 = (i + 1) * Math.PI * 2 / sides;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			float u0 = uShift + uTimes * i / sides, u1 = uShift + uTimes * (i + 1) / sides;
			double ax = x + c0 * r0, az = z + s0 * r0, bx = x + c0 * r1, bz = z + s0 * r1;
			double cx = x + c1 * r1, cz = z + s1 * r1, dx = x + c1 * r0, dz = z + s1 * r0;
			v(ax, ground.at(ax, az) + lift, az, u0, 0, r, g, b, a0);
			v(bx, ground.at(bx, bz) + lift, bz, u0, 1, r, g, b, a1);
			v(cx, ground.at(cx, cz) + lift, cz, u1, 1, r, g, b, a1);
			v(dx, ground.at(dx, dz) + lift, dz, u1, 0, r, g, b, a0);
		}
	}

	/** A ring lying level at height {@code y} (a cloud base, a ceiling): as {@link #groundRing}, without the ground. */
	public static void levelRing(double x, double y, double z, float r0, float r1, int sides, float uTimes, float uShift,
								 float r, float g, float b, float a0, float a1) {
		if (r1 <= 0 || a0 <= 0 && a1 <= 0) return;
		for (int i = 0; i < sides; i++) {
			double t0 = i * Math.PI * 2 / sides, t1 = (i + 1) * Math.PI * 2 / sides;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			float u0 = uShift + uTimes * i / sides, u1 = uShift + uTimes * (i + 1) / sides;
			v(x + c0 * r0, y, z + s0 * r0, u0, 0, r, g, b, a0);
			v(x + c0 * r1, y, z + s0 * r1, u0, 1, r, g, b, a1);
			v(x + c1 * r1, y, z + s1 * r1, u1, 1, r, g, b, a1);
			v(x + c1 * r0, y, z + s1 * r0, u1, 0, r, g, b, a0);
		}
	}

	/**
	 * A square of {@code cells} x {@code cells} laid over the ground about (x, z), {@code half} its half-size, the whole texture across it
	 * turned {@code turn} radians (an array, a pool of light on the ground).
	 */
	public static void groundSquare(HeightField ground, double x, double z, float half, int cells, float turn, float lift,
									float r, float g, float b, float a) {
		if (a <= 0 || half <= 0) return;
		float c = Mth.cos(turn), s = Mth.sin(turn);
		for (int i = 0; i < cells; i++) {
			for (int j = 0; j < cells; j++) {
				cell(ground, x, z, half, cells, i, j, c, s, lift, r, g, b, a);
			}
		}
	}

	private static void cell(HeightField ground, double x, double z, float half, int cells, int i, int j, float c, float s, float lift,
							 float r, float g, float b, float a) {
		corner(ground, x, z, half, cells, i, j, c, s, lift, r, g, b, a);
		corner(ground, x, z, half, cells, i + 1, j, c, s, lift, r, g, b, a);
		corner(ground, x, z, half, cells, i + 1, j + 1, c, s, lift, r, g, b, a);
		corner(ground, x, z, half, cells, i, j + 1, c, s, lift, r, g, b, a);
	}

	private static void corner(HeightField ground, double x, double z, float half, int cells, int i, int j, float c, float s, float lift,
							   float r, float g, float b, float a) {
		float u = (float) i / cells, w = (float) j / cells;
		double px = (u * 2 - 1) * half, pz = (w * 2 - 1) * half;
		double wx = x + px * c - pz * s, wz = z + px * s + pz * c;
		v(wx, ground.at(wx, wz) + lift, wz, u, w, r, g, b, a);
	}

	/** A standing wall round (x, z) at {@code radius}, from the ground up {@code height}: u round it, v from 0 at its foot to 1 at its top. */
	public static void groundWall(HeightField ground, double x, double z, float radius, float height, int sides, float uTimes, float uShift,
								  float r, float g, float b, float aFoot, float aTop) {
		if (radius <= 0 || height <= 0 || aFoot <= 0 && aTop <= 0) return;
		for (int i = 0; i < sides; i++) {
			double t0 = i * Math.PI * 2 / sides, t1 = (i + 1) * Math.PI * 2 / sides;
			double ax = x + Math.cos(t0) * radius, az = z + Math.sin(t0) * radius, bx = x + Math.cos(t1) * radius, bz = z + Math.sin(t1) * radius;
			float u0 = uShift + uTimes * i / sides, u1 = uShift + uTimes * (i + 1) / sides;
			double ya = ground.at(ax, az), yb = ground.at(bx, bz);
			v(ax, ya, az, u0, 0, r, g, b, aFoot);
			v(bx, yb, bz, u1, 0, r, g, b, aFoot);
			v(bx, yb + height, bz, u1, 1, r, g, b, aTop);
			v(ax, ya + height, az, u0, 1, r, g, b, aTop);
		}
	}

	/**
	 * An upright tube of light about (x, z) from {@code y0} to {@code y1}: u round it ({@code uTimes} repeats), v from {@code v0} at its
	 * foot to {@code v1} at its top. Each face is as bright as it is turned toward the camera (to the power {@code sharp}), so the tube's
	 * edges fade softly, as a beam's do.
	 */
	public static void tube(double x, double z, double y0, double y1, float radius, int sides, float uTimes, float v0, float v1,
							float r, float g, float b, float a, float sharp) {
		tube(x, z, y0, y1, radius, radius, sides, uTimes, v0, v1, r, g, b, a, sharp);
	}

	/** As {@link #tube}, {@code radius} round at its foot and {@code topRadius} at its top (a column of light flaring as it rises). */
	public static void tube(double x, double z, double y0, double y1, float radius, float topRadius, int sides, float uTimes, float v0, float v1,
							float r, float g, float b, float a, float sharp) {
		tube(x, z, y0, y1, radius, topRadius, sides, uTimes, v0, v1, r, g, b, a, sharp, 1);
	}

	/**
	 * As {@link #tube}, its light times {@code topAlpha} at its top (eased between): a tall column thins as it rises, so seen from its
	 * foot, its length folded into the vanishing point doesn't burn white there.
	 */
	public static void tube(double x, double z, double y0, double y1, float radius, float topRadius, int sides, float uTimes, float v0, float v1,
							float r, float g, float b, float a, float sharp, float topAlpha) {
		if (radius <= 0 || a <= 0 || y1 <= y0) return;
		double tx = camX - x, tz = camZ - z, tl = Math.sqrt(tx * tx + tz * tz);
		if (tl < 1.0E-6) {
			tx = 1;
			tl = 1;
		}
		tx /= tl;
		tz /= tl;
		for (int i = 0; i < sides; i++) {
			double t0 = i * Math.PI * 2 / sides, t1 = (i + 1) * Math.PI * 2 / sides;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			float f0 = (float) Math.pow(Math.abs(c0 * tx + s0 * tz), sharp) * a, f1 = (float) Math.pow(Math.abs(c1 * tx + s1 * tz), sharp) * a;
			float u0 = uTimes * i / sides, u1 = uTimes * (i + 1) / sides;
			v(x + c0 * radius, y0, z + s0 * radius, u0, v0, r, g, b, f0);
			v(x + c1 * radius, y0, z + s1 * radius, u1, v0, r, g, b, f1);
			v(x + c1 * topRadius, y1, z + s1 * topRadius, u1, v1, r, g, b, f1 * topAlpha);
			v(x + c0 * topRadius, y1, z + s0 * topRadius, u0, v1, r, g, b, f0 * topAlpha);
		}
	}

	private Fx() {}
}
