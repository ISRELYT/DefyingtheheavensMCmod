package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.MetalSpells;
import com.example.defyingtheheavens.SpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Every element spell's missile (SpellProjectile), by its shape: a bolt of flame, a wooden stake, a podao of earth, an arrow of
 * water, a flying sword, a serpent of fire, a boulder, a crescent of sword light, a razor leaf, a spear of ice, a meteor, a phoenix, a
 * falling sun, a colossal sword, a falling mountain, a ball of water. A missile still forming beside its caster is drawn gathering
 * itself; one in flight trails fire, light, water or dust behind it along where it has been (ClientSpellTrails).
 */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectile> {
	private static final float[] WHITE = {1, 1, 1};
	private static final float[] FIRE_CORE = {1.0f, 0.95f, 0.75f}, FIRE_MID = {1.0f, 0.55f, 0.15f}, FIRE_OUTER = {0.95f, 0.22f, 0.06f};
	private static final float[] WATER_GLOW = {0.35f, 0.75f, 1.0f}, ICE_GLOW = {0.6f, 0.9f, 1.0f};
	private static final float[] SWORD_LIGHT = {1.0f, 0.95f, 0.8f}, LEAF_GLOW = {0.45f, 1.0f, 0.4f}, EARTH_GLOW = {1.0f, 0.68f, 0.28f};

	public SpellProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
		shadowRadius = 0;
	}

	@Override
	public ResourceLocation getTextureLocation(SpellProjectile missile) {
		return SpellRender.GLOW;
	}

	@Override
	public boolean shouldRender(SpellProjectile missile, Frustum frustum, double x, double y, double z) {
		return true; // big trails and glows reach well past its little hitbox
	}

	@Override
	public void render(SpellProjectile missile, float entityYaw, float partial, PoseStack pose, MultiBufferSource parent, int light) {
		MultiBufferSource buffers = SpellBuffers.begin(parent);
		try {
			draw(missile, partial, pose, buffers, light);
		} finally {
			SpellBuffers.end();
		}
	}

	private void draw(SpellProjectile missile, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
		float age = missile.tickCount + partial;
		float lift = missile.getBbHeight() / 2;
		Vec3 at = missile.getPosition(partial).add(0, lift, 0);
		Vec3 cam = entityRenderDispatcher.camera.getPosition().subtract(at);
		Quaternionf facing = entityRenderDispatcher.cameraOrientation();
		float yaw = Mth.rotLerp(partial, missile.yRotO, missile.getYRot());
		float pitch = Mth.lerp(partial, missile.xRotO, missile.getXRot());
		// How far it has gathered itself (1 once formed).
		float formed = missile.formTicks() <= 0 ? 1 : Mth.clamp(age / missile.formTicks(), 0, 1);
		List<Vec3> trail = trail(missile, at, lift);
		pose.pushPose();
		pose.translate(0, lift, 0);
		switch (missile.shape()) {
			case FLAME_BOLT, YANG_BOLT -> flameBolt(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch);
			case WOOD_STAKE -> stake(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch, light);
			case EARTH_PODAO -> podao(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch, light);
			case WATER_ARROW -> waterArrow(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch);
			case FLYING_SWORD -> sword(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch, light, 1.3f);
			case PLUNGING_SWORD -> plunging(pose, buffers, cam, trail);
			case COLOSSAL_SWORD -> colossal(missile, pose, buffers, facing, cam, trail, age, light);
			case FIRE_SERPENT -> serpent(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch);
			case BOULDER, MOUNTAIN -> boulder(missile, pose, buffers, facing, cam, trail, age, light);
			case CLEAVE_SLASH, MOUNTAIN_SLASH -> slash(missile, pose, buffers, trail, age, yaw, pitch);
			case HEAVEN_SLASH -> heavenSlash(pose, buffers, facing, cam, trail, age, yaw);
			case RAZOR_LEAF -> leaf(missile, pose, buffers, cam, trail, age, yaw, pitch, light);
			case ICE_SPEAR -> iceSpear(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch);
			case METEOR -> meteor(missile, pose, buffers, facing, cam, trail, age, light);
			case PHOENIX -> phoenix(missile, pose, buffers, facing, cam, trail, age, formed, yaw, pitch);
			case FALLING_SUN -> fallingSun(missile, pose, buffers, facing, cam, trail, age);
			case WATER_BALL -> waterBall(missile, pose, buffers, facing, trail, age);
		}
		pose.popPose();
	}

	/** The missile's trail as points relative to where it is drawn now, newest (itself) first. */
	private static List<Vec3> trail(SpellProjectile missile, Vec3 at, float lift) {
		List<Vec3> points = new ArrayList<>();
		points.add(Vec3.ZERO);
		Deque<Vec3> past = ClientSpellTrails.of(missile);
		for (Vec3 p : past) {
			Vec3 rel = p.add(0, lift, 0).subtract(at);
			if (rel.lengthSqr() < 1.0e-4 && points.size() == 1) continue;
			points.add(rel);
		}
		return points;
	}

	/** Turns the pose so +z runs the way the missile flies. */
	private static void face(PoseStack pose, float yaw, float pitch) {
		pose.mulPose(Axis.YP.rotationDegrees(yaw));
		pose.mulPose(Axis.XP.rotationDegrees(-pitch));
	}

	/** {@code count} points back along the trail, at most {@code length} blocks, spaced evenly (for ribbons). */
	private static List<Vec3> back(List<Vec3> trail, double length, int count) {
		List<Vec3> out = new ArrayList<>();
		out.add(trail.get(0));
		double total = 0;
		for (int i = 1; i < trail.size() && total < length; i++) {
			total += trail.get(i).distanceTo(trail.get(i - 1));
			out.add(trail.get(i));
		}
		if (out.size() < 2) return out;
		// Resampled to even steps.
		List<Vec3> even = new ArrayList<>();
		double step = Math.min(length, total) / (count - 1);
		int seg = 0;
		double into = 0;
		for (int k = 0; k < count; k++) {
			double want = k * step;
			while (seg < out.size() - 2 && into + out.get(seg).distanceTo(out.get(seg + 1)) < want) {
				into += out.get(seg).distanceTo(out.get(seg + 1));
				seg++;
			}
			double len = out.get(seg).distanceTo(out.get(seg + 1));
			double t = len < 1.0e-6 ? 0 : Mth.clamp((want - into) / len, 0, 1);
			even.add(out.get(seg).lerp(out.get(seg + 1), t));
		}
		return even;
	}

	/** A glowing ribbon along the trail: widest at the head, thinning and fading toward its tail. */
	private static void streak(MultiBufferSource buffers, PoseStack pose, List<Vec3> trail, Vec3 cam, ResourceLocation texture, float[] rgb, float width,
							   float alpha, double length, float scroll) {
		List<Vec3> points = back(trail, length, 10);
		if (points.size() < 2) return;
		float[] widths = new float[points.size()], alphas = new float[points.size()];
		for (int i = 0; i < points.size(); i++) {
			float t = i / (float) (points.size() - 1);
			widths[i] = width * (1 - t * 0.8f);
			alphas[i] = alpha * (1 - t) * (1 - t);
		}
		SpellRender.ribbon(buffers.getBuffer(SpellRender.glow(texture)), pose.last(), points, widths, cam, rgb, alphas, 0.5f, scroll, SpellRender.FULL_BRIGHT);
	}

	/**
	 * {@code count} ribbons winding round the trail (out to {@code length} blocks behind), {@code radius} out from it at the head and
	 * closing in toward the tail, turning as they go: the spiralling streams round a missile's wake.
	 */
	private static void spirals(MultiBufferSource buffers, PoseStack pose, List<Vec3> trail, Vec3 cam, ResourceLocation texture, float[] rgb, float radius,
								float width, float alpha, double length, float age, int count) {
		List<Vec3> path = back(trail, length, 14);
		if (path.size() < 3) return;
		for (int k = 0; k < count; k++) {
			List<Vec3> ribbon = new ArrayList<>();
			float[] widths = new float[path.size()], alphas = new float[path.size()];
			for (int i = 0; i < path.size(); i++) {
				Vec3 p = path.get(i);
				Vec3 dir = path.get(Math.min(path.size() - 1, i + 1)).subtract(path.get(Math.max(0, i - 1)));
				dir = dir.lengthSqr() < 1.0e-8 ? new Vec3(0, 0, 1) : dir.normalize();
				Vec3 side = dir.cross(new Vec3(0, 1, 0));
				side = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
				Vec3 up = side.cross(dir).normalize();
				float t = i / (float) (path.size() - 1);
				double a = age * 0.45 + i * 0.75 + k * Math.PI * 2 / count;
				float r = radius * (1 - t * 0.6f);
				ribbon.add(p.add(side.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r)));
				widths[i] = width * (1 - t * 0.7f);
				alphas[i] = alpha * (1 - t) * Math.min(1, i * 0.5f);
			}
			SpellRender.ribbon(buffers.getBuffer(SpellRender.glow(texture)), pose.last(), ribbon, widths, cam, rgb, alphas, 0.5f, -age * 0.1f, SpellRender.FULL_BRIGHT);
		}
	}

	/** A four-point star of light, turning, at (x, y, z). */
	private static void star(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, float x, float y, float z, float size, float spin, float[] rgb, float alpha) {
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.STAR)), pose, facing, x, y, z, size, spin, rgb, alpha, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
	}

	private static void glowBall(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, float x, float y, float z, float size, float[] rgb, float alpha) {
		SpellRender.billboard(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose, facing, x, y, z, size, 0, rgb, alpha, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
	}

	// --- Fire ---

	/**
	 * Flame Bolt: a white-hot heart in layered glows of yellow, orange and red, flame tongues streaming back from it, embers shed
	 * along its trail, and a thin flare across it.
	 */
	private void flameBolt(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age,
						   float formed, float yaw, float pitch) {
		boolean yang = missile.shape() == SpellProjectile.Shape.YANG_BOLT;
		float s = missile.scale() * 1.25f * (0.3f + 0.7f * SpellRender.easeOut(formed));
		float pulse = 1 + 0.08f * Mth.sin(age * 1.7f);
		glowBall(buffers, pose, facing, 0, 0, 0, 2.6f * s * pulse, FIRE_OUTER, 0.35f);
		glowBall(buffers, pose, facing, 0, 0, 0, 1.4f * s * pulse, FIRE_MID, 0.7f);
		glowBall(buffers, pose, facing, 0, 0, 0, 0.7f * s, FIRE_CORE, 1.0f);
		// A star of heat at its heart, turning, and two ribbons of flame winding round its wake.
		star(buffers, pose, facing, 0, 0, 0, 2.2f * s, age * 0.12f, FIRE_CORE, 0.8f);
		spirals(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_MID, 0.35f * s, 0.32f * s, 0.85f, 2.8, age, 2);
		// The flare: a thin line of light across it.
		VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		SpellRender.billboard(spark, pose, facing, 0, 0, 0, 3.2f * s, 0.35f * s, 0, FIRE_MID, 0.5f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		// Flame tongues streaming back.
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_MID, 0.9f * s, 0.9f, 2.4, -age * 0.15f);
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_CORE, 0.45f * s, 0.9f, 1.6, -age * 0.2f);
		// Embers: shed along where it passed, drifting and dimming.
		List<Vec3> path = back(trail, 4.0, 14);
		for (int i = 1; i < path.size(); i++) {
			long seed = missile.getId() * 31L + i + (long) Math.floor(age / 3) * 7;
			Vec3 p = path.get(i).add((SpellRender.hash(seed) - 0.5) * 0.5, (SpellRender.hash(seed + 1) - 0.5) * 0.5 + i * 0.03, (SpellRender.hash(seed + 2) - 0.5) * 0.5);
			float fade = 1 - i / (float) path.size();
			SpellRender.billboard(spark, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.22f * s * (0.5f + fade), 0, yang ? FIRE_CORE : FIRE_MID,
					fade * 0.9f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * Fire Serpent: a body of fire following its head along its path, swaying as it swims through the air, thick behind the head and
	 * thinning to a flickering tail, flames licking up off it; the head long-jawed and horned, with eyes of white fire.
	 */
	private void serpent(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age,
						 float formed, float yaw, float pitch) {
		float s = missile.scale();
		long seed = missile.data();
		if (missile.forming()) {
			// Coiling out of the palm: a ring of fire tightening and brightening.
			float g = SpellRender.easeOut(formed);
			for (int i = 0; i < 12; i++) {
				double a = age * 0.4 + i * Math.PI * 2 / 12;
				float r = 0.8f * (1 - g) + 0.15f;
				glowBall(buffers, pose, facing, (float) Math.cos(a) * r, (float) Math.sin(a * 2) * 0.15f, (float) Math.sin(a) * r, 0.5f * g + 0.2f, FIRE_MID, 0.8f);
			}
			glowBall(buffers, pose, facing, 0, 0, 0, 1.2f * g, FIRE_CORE, g);
			return;
		}
		// The body along the path, swaying side to side.
		List<Vec3> path = back(trail, 5.5, 18);
		if (path.size() >= 2) {
			List<Vec3> body = new ArrayList<>();
			float[] radii = new float[path.size()];
			for (int i = 0; i < path.size(); i++) {
				Vec3 p = path.get(i);
				Vec3 dir = path.get(Math.min(path.size() - 1, i + 1)).subtract(path.get(Math.max(0, i - 1)));
				Vec3 side = dir.cross(new Vec3(0, 1, 0));
				side = side.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : side.normalize();
				double sway = Math.sin(age * 0.5 - i * 0.7) * 0.35 * Math.min(1, i / 3.0);
				double rise = Math.cos(age * 0.35 - i * 0.6) * 0.15 * Math.min(1, i / 3.0);
				body.add(p.add(side.scale(sway)).add(0, rise, 0));
				float t = i / (float) (path.size() - 1);
				radii[i] = s * (i == 0 ? 0.32f : 0.38f * (1 - t) + 0.03f);
			}
			float[] fv = SpellRender.frame(age, 0.5f, SpellRender.FLAME_FRAMES);
			SpellRender.tube(buffers.getBuffer(SpellRender.glow(SpellRender.FLAME)), pose.last(), body, radii, 8, FIRE_MID, 0.95f, 0.08f,
					fv[0] - age * 0.05f, SpellRender.FULL_BRIGHT, new SpellRender.PointShade() {
						@Override
						public float[] colour(int i, int n) {
							float t = i / (float) n;
							return new float[]{1.0f, 0.85f - 0.55f * t, 0.45f - 0.38f * t};
						}

						@Override
						public float alpha(int i, int n) {
							return 1 - (i / (float) n) * 0.7f;
						}
					});
			// A hot core along the spine, and flames licking up off its back.
			float[] coreR = new float[radii.length];
			for (int i = 0; i < radii.length; i++) coreR[i] = radii[i] * 0.45f;
			SpellRender.tube(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), body, coreR, 6, FIRE_CORE, 0.8f, 0.1f, 0, SpellRender.FULL_BRIGHT, null);
			VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
			for (int i = 1; i < body.size(); i += 2) {
				Vec3 p = body.get(i);
				float t = i / (float) body.size();
				SpellRender.flameTongue(flames, pose, cam, (float) p.x, (float) p.y + radii[i] * 0.5f, (float) p.z, radii[i] * 2.4f, radii[i] * 3.4f,
						age + i * 3, FIRE_MID, 0.9f - t * 0.5f, SpellRender.FLAME_FRAMES);
			}
		}
		// The head: long jaws, a brow, horns of flame, eyes of white fire.
		pose.pushPose();
		face(pose, yaw, pitch);
		glowBall(buffers, pose, facing, 0, 0, 0.1f * s, 1.8f * s, FIRE_OUTER, 0.5f);
		glowBall(buffers, pose, facing, 0, 0.05f * s, 0.35f * s, 0.9f * s, FIRE_MID, 0.9f);
		glowBall(buffers, pose, facing, 0, 0, 0.7f * s, 0.55f * s, FIRE_CORE, 0.9f);
		float jaw = 0.12f + 0.08f * Mth.sin(age * 0.9f);
		VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
		float[] fv = SpellRender.frame(age + seed % 5, 0.5f, SpellRender.FLAME_FRAMES);
		// Upper and lower jaw as flame blades reaching forward.
		jawBlade(flames, pose, 0.12f * s + jaw * s, 0.95f * s, 0.32f * s, fv);
		jawBlade(flames, pose, -0.1f * s - jaw * s, 0.85f * s, 0.26f * s, fv);
		// Horns: two tongues sweeping back and up from the brow.
		for (int side = -1; side <= 1; side += 2) {
			pose.pushPose();
			pose.translate(side * 0.18f * s, 0.25f * s, 0.25f * s);
			pose.mulPose(Axis.XP.rotationDegrees(-55));
			pose.mulPose(Axis.ZP.rotationDegrees(side * 15));
			hornQuad(flames, pose, 0.22f * s, 0.75f * s, fv);
			pose.popPose();
		}
		VertexConsumer spark = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
		for (int side = -1; side <= 1; side += 2) {
			SpellRender.billboard(spark, pose, facing, side * 0.15f * s, 0.12f * s, 0.55f * s, 0.32f * s, 0, WHITE, 1, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
		pose.popPose();
	}

	/** A jaw of flame: a tapering blade reaching forward from the head at height {@code y}. */
	private static void jawBlade(VertexConsumer vc, PoseStack pose, float y, float length, float width, float[] fv) {
		PoseStack.Pose p = pose.last();
		SpellRender.quad(vc, p, SpellRender.v(-width / 2, y, 0.2f), SpellRender.v(width / 2, y, 0.2f), SpellRender.v(0.02f, y, 0.2f + length),
				SpellRender.v(-0.02f, y, 0.2f + length), FIRE_MID, 0.95f, 0, fv[1], 1, fv[0], SpellRender.FULL_BRIGHT);
		SpellRender.quad(vc, p, SpellRender.v(0, y - width / 3, 0.2f), SpellRender.v(0, y + width / 3, 0.2f), SpellRender.v(0, y + 0.02f, 0.2f + length),
				SpellRender.v(0, y - 0.02f, 0.2f + length), FIRE_MID, 0.95f, 0, fv[1], 1, fv[0], SpellRender.FULL_BRIGHT);
	}

	/** A horn of flame along +y, {@code width} at its root, {@code length} long, crossed so it shows from any side. */
	private static void hornQuad(VertexConsumer vc, PoseStack pose, float width, float length, float[] fv) {
		PoseStack.Pose p = pose.last();
		SpellRender.quad(vc, p, SpellRender.v(-width / 2, 0, 0), SpellRender.v(width / 2, 0, 0), SpellRender.v(0.01f, length, 0), SpellRender.v(-0.01f, length, 0),
				FIRE_MID, 0.9f, 0, fv[1], 1, fv[0], SpellRender.FULL_BRIGHT);
		SpellRender.quad(vc, p, SpellRender.v(0, 0, -width / 2), SpellRender.v(0, 0, width / 2), SpellRender.v(0, length, 0.01f), SpellRender.v(0, length, -0.01f),
				FIRE_MID, 0.9f, 0, fv[1], 1, fv[0], SpellRender.FULL_BRIGHT);
	}

	/**
	 * Blazing Meteor: the burning rock itself is its body (SpellConstruct BLAZING_METEOR: SpellProjectile#body), so this is only the wake of fire
	 * and smoke it leaves across the sky behind it.
	 */
	private void meteor(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age, int light) {
		float s = missile.scale();
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_MID, 4.0f * s, 0.75f, 24, -age * 0.1f);
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_CORE, 1.8f * s, 0.6f, 14, -age * 0.15f);
		smoke(buffers, pose, facing, trail, missile.getId(), age, 3.0f * s, 28, 0.5f);
	}

	/** Smoke puffs hanging along the trail, growing and thinning as they fall behind. */
	private static void smoke(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, List<Vec3> trail, int id, float age, float size, double length, float alpha) {
		VertexConsumer vc = buffers.getBuffer(SpellRender.clear(SpellRender.SMOKE));
		List<Vec3> path = back(trail, length, 12);
		for (int i = 2; i < path.size(); i++) {
			Vec3 p = path.get(i);
			float t = i / (float) path.size();
			long seed = id * 17L + i;
			SpellRender.billboard(vc, pose, facing, (float) p.x, (float) p.y + t * 0.6f, (float) p.z, size * (0.6f + t * 1.2f), (float) (SpellRender.hash(seed) * 6 + age * 0.02f),
					new float[]{0.22f, 0.2f, 0.2f}, alpha * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * Phoenix Descent: the phoenix of fire itself is its body (SpellConstruct DESCENT_PHOENIX: SpellProjectile#body), so this is only the wake of
	 * fire it leaves behind it as it stoops (none while it gathers beside its caster).
	 */
	private void phoenix(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age,
						 float formed, float yaw, float pitch) {
		if (missile.forming()) return;
		float s = missile.scale();
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_MID, 3.4f * s, 0.75f, 16, -age * 0.12f);
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_CORE, 1.4f * s, 0.6f, 9, -age * 0.16f);
	}

	/**
	 * One of the Nine Suns falling: the sun itself is its body (SpellConstruct FALLING_SUN, a great spell's shape: SpellProjectile#body), so this
	 * is only the wake it burns across the sky behind it, and the smoke hanging in it.
	 */
	private void fallingSun(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age) {
		float s = missile.scale();
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_MID, 5.0f * s, 0.75f, 30, -age * 0.1f);
		streak(buffers, pose, trail, cam, SpellRender.FLAME, FIRE_CORE, 2.2f * s, 0.6f, 18, -age * 0.15f);
		smoke(buffers, pose, facing, trail, missile.getId(), age, 3.5f * s, 30, 0.4f);
	}

	// --- Wood ---

	/**
	 * Wood Bullet: a tapered stake of bark, point first, in a glow of the wood's qi; while it grows in the air, leaves spiral in round it;
	 * in flight it leaves a green wake and sheds leaves that flutter down behind it.
	 */
	private void stake(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age, float formed,
					   float yaw, float pitch, int light) {
		if (!missile.forming()) {
			float s = missile.scale();
			streak(buffers, pose, trail, cam, SpellRender.GLOW, LEAF_GLOW, 0.45f * s, 0.55f, 3.0, 0);
			spirals(buffers, pose, trail, cam, SpellRender.GLOW, LEAF_GLOW, 0.22f * s, 0.12f * s, 0.6f, 2.4, age, 2);
			VertexConsumer shed = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
			List<Vec3> path = back(trail, 3.5, 8);
			for (int i = 1; i < path.size(); i++) {
				long seed = missile.getId() * 41L + i;
				Vec3 p = path.get(i).add(Mth.sin(age * 0.4f + i) * 0.25f, -i * 0.06f, Mth.cos(age * 0.35f + i) * 0.25f);
				SpellRender.billboard(shed, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.2f * s, age * 0.3f + SpellRender.hash(seed) * 6, WHITE, 1, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
		}
		float s = missile.scale();
		float g = SpellRender.easeOut(formed);
		pose.pushPose();
		face(pose, yaw, pitch);
		pose.mulPose(Axis.ZP.rotationDegrees(age * (missile.forming() ? 25 : 8)));
		SpellModels.stake(pose, buffers, 1.15f * s, 0.12f * s, light, 0.25f + 0.75f * g);
		pose.popPose();
		if (missile.forming()) {
			VertexConsumer leaves = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
			for (int i = 0; i < 6; i++) {
				double a = age * 0.6 + i * Math.PI * 2 / 6;
				float r = 0.9f * (1 - g) + 0.1f;
				float x = (float) Math.cos(a) * r, y = (float) Math.sin(a * 1.3) * 0.25f, z = (float) Math.sin(a) * r;
				SpellRender.billboard(leaves, pose, facing, x, y, z, 0.25f, (float) a, WHITE, 1, 0, 0, 1, 1, light);
			}
			glowBall(buffers, pose, facing, 0, 0, 0, 1.0f * (1 - g) + 0.2f, LEAF_GLOW, 0.4f);
		}
	}

	/** Thousand Leaf Storm: one razor leaf spinning edge-on along its way, a faint green streak behind it. */
	private void leaf(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Vec3 cam, List<Vec3> trail, float age, float yaw, float pitch, int light) {
		float s = missile.scale();
		streak(buffers, pose, trail, cam, SpellRender.GLOW, LEAF_GLOW, 0.25f * s, 0.5f, 1.8, 0);
		pose.pushPose();
		face(pose, yaw, pitch);
		pose.mulPose(Axis.ZP.rotationDegrees(age * 40 + missile.data() % 360));
		pose.mulPose(Axis.XP.rotationDegrees(90));
		VertexConsumer vc = buffers.getBuffer(SpellRender.solid(SpellRender.LEAF));
		PoseStack.Pose p = pose.last();
		float w = 0.22f * s, l = 0.42f * s;
		SpellRender.quad(vc, p, SpellRender.v(-w, 0, -l), SpellRender.v(-w, 0, l), SpellRender.v(w, 0, l), SpellRender.v(w, 0, -l), WHITE, 1, 0, 1, 1, 0,
				SpellRender.FULL_BRIGHT);
		// Its razor edge lit with the wood's qi: the leaf again, larger, in light.
		VertexConsumer edge = buffers.getBuffer(SpellRender.glow(SpellRender.LEAF));
		SpellRender.quad(edge, p, SpellRender.v(-w * 1.5f, 0.01f, -l * 1.4f), SpellRender.v(-w * 1.5f, 0.01f, l * 1.4f), SpellRender.v(w * 1.5f, 0.01f, l * 1.4f),
				SpellRender.v(w * 1.5f, 0.01f, -l * 1.4f), LEAF_GLOW, 0.55f, 0, 1, 1, 0, SpellRender.FULL_BRIGHT);
		pose.popPose();
	}

	// --- Earth ---

	/** Earth Spear: a podao of dried earth; while it forms, clods rise round it and fuse into it. */
	private void podao(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age, float formed,
					   float yaw, float pitch, int light) {
		float g = SpellRender.ease(formed);
		pose.pushPose();
		face(pose, yaw, pitch);
		if (missile.forming()) pose.mulPose(Axis.ZP.rotationDegrees((1 - g) * 90));
		SpellModels.podao(pose, buffers, light, 0.2f + 0.8f * g, null);
		pose.popPose();
		if (missile.forming()) {
			var sprite = SpellRender.sprite(missile.data() == 0 ? Block.getId(Blocks.DIRT.defaultBlockState()) : missile.data());
			VertexConsumer vc = buffers.getBuffer(SpellRender.blocks());
			for (int i = 0; i < 9; i++) {
				long seed = missile.getId() * 13L + i;
				float delay = SpellRender.hash(seed) * 0.4f;
				float t = Mth.clamp((formed - delay) / 0.6f, 0, 1);
				Vec3 from = new Vec3((SpellRender.hash(seed + 1) - 0.5) * 2.4, -1.6 - SpellRender.hash(seed + 2), (SpellRender.hash(seed + 3) - 0.5) * 2.4);
				Vec3 at = from.scale(1 - SpellRender.ease(t));
				float c = 0.14f * (1 - t * 0.7f);
				pose.pushPose();
				pose.translate(at.x, at.y, at.z);
				pose.mulPose(Axis.YP.rotation(age * 0.3f + i));
				SpellRender.box(vc, pose.last(), -c, -c, -c, c, c, c, WHITE, 1, sprite.getU0(), sprite.getV0(), sprite.getU(6), sprite.getV(6), light);
				pose.popPose();
			}
		} else {
			dustTrail(buffers, pose, facing, trail, missile.getId(), age, 0.9f, 3.0, 0.5f);
			// An amber wake of the earth's qi, and chips of stone wheeling round the blade as it flies.
			streak(buffers, pose, trail, cam, SpellRender.GLOW, EARTH_GLOW, 0.5f, 0.5f, 3.0, 0);
			var sprite = SpellRender.sprite(missile.data() == 0 ? Block.getId(Blocks.DIRT.defaultBlockState()) : missile.data());
			VertexConsumer vc = buffers.getBuffer(SpellRender.blocks());
			for (int i = 0; i < 6; i++) {
				double a = age * 0.5 + i * Math.PI * 2 / 6;
				float r = 0.55f, c = 0.07f + 0.03f * (i % 2);
				pose.pushPose();
				face(pose, yaw, pitch);
				pose.translate(Math.cos(a) * r, Math.sin(a) * r, -0.3f - (i % 3) * 0.25f);
				pose.mulPose(Axis.XP.rotation(age * 0.4f + i));
				SpellRender.box(vc, pose.last(), -c, -c, -c, c, c, c, WHITE, 1, sprite.getU0(), sprite.getV0(), sprite.getU(6), sprite.getV(6), SpellRender.FULL_BRIGHT);
				pose.popPose();
			}
		}
	}

	/** Dust thinning out along a trail. */
	private static void dustTrail(MultiBufferSource buffers, PoseStack pose, Quaternionf facing, List<Vec3> trail, int id, float age, float size, double length, float alpha) {
		VertexConsumer vc = buffers.getBuffer(SpellRender.clear(SpellRender.DUST));
		List<Vec3> path = back(trail, length, 8);
		for (int i = 1; i < path.size(); i++) {
			Vec3 p = path.get(i);
			float t = i / (float) path.size();
			SpellRender.billboard(vc, pose, facing, (float) p.x, (float) p.y, (float) p.z, size * (0.7f + t), (float) (SpellRender.hash(id * 7L + i) * 6),
					WHITE, alpha * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
		}
	}

	/**
	 * Boulder Throw: a lump of the ground's own rock, tumbling, shedding dust. Falling Mountain: the mountain itself is its body (SpellConstruct
	 * FALLING_PEAK: SpellProjectile#body), so this is only the dust it sheds behind it as it falls.
	 */
	private void boulder(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age, int light) {
		if (missile.shape() == SpellProjectile.Shape.MOUNTAIN) {
			dustTrail(buffers, pose, facing, trail, missile.getId(), age, 7, 24, 0.6f);
			return;
		}
		var sprite = SpellRender.sprite(missile.data() == 0 ? Block.getId(Blocks.STONE.defaultBlockState()) : missile.data());
		float radius = 0.95f, spin = age * 0.25f;
		SpellModels.boulder(pose, buffers, sprite, radius * missile.scale(), spin, light, missile.getId(), null);
		// The earth's qi that tore it loose: seams of its fire glowing through the rock, turning with it, and a halo about it.
		pose.pushPose();
		pose.mulPose(Axis.XP.rotation(spin));
		pose.mulPose(Axis.ZP.rotation(spin * 0.6f));
		SpellRender.sphere(buffers.getBuffer(SpellRender.glow(SpellRender.VEINS)), pose.last(), radius * missile.scale() * 1.05f, 8, 12, EARTH_GLOW, 0.75f, 2, 1, 0, 0,
				SpellRender.FULL_BRIGHT, null);
		pose.popPose();
		glowBall(buffers, pose, facing, 0, 0, 0, radius * missile.scale() * 3.2f, EARTH_GLOW, 0.3f);
		dustTrail(buffers, pose, facing, trail, missile.getId(), age, 1.2f, 4, 0.6f);
	}

	// --- Water ---

	/** Water Arrow: a spindle of moving water, point first, two currents spiralling round it and spilling behind, drops shed. */
	private void waterArrow(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age,
							float formed, float yaw, float pitch) {
		float s = missile.scale() * (0.2f + 0.8f * SpellRender.easeOut(formed));
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		pose.pushPose();
		face(pose, yaw, pitch);
		List<Vec3> spindle = new ArrayList<>();
		float[] radii = {0.0f, 0.1f, 0.17f, 0.16f, 0.12f, 0.08f, 0.04f};
		float[] scaled = new float[radii.length];
		for (int i = 0; i < radii.length; i++) {
			spindle.add(new Vec3(0, 0, (0.7 - i * 0.28) * s));
			scaled[i] = radii[i] * s;
		}
		SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), spindle, scaled, 10, WHITE, 0.85f, 0.25f / s,
				fv[0] + age * 0.02f, SpellRender.FULL_BRIGHT, null);
		// Two currents winding round it and running off behind.
		for (int k = 0; k < 2; k++) {
			List<Vec3> spiral = new ArrayList<>();
			float[] r = new float[14];
			for (int i = 0; i < 14; i++) {
				double t = i / 13.0;
				double a = age * 0.6 + k * Math.PI + t * 7;
				double rad = (0.14 + t * 0.3) * s;
				spiral.add(new Vec3(Math.cos(a) * rad, Math.sin(a) * rad, (0.4 - t * 2.4) * s));
				r[i] = (float) (0.05 * (1 - t) + 0.01) * s;
			}
			SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), spiral, r, 6, WHITE, 0.75f, 0.5f, fv[0], SpellRender.FULL_BRIGHT, null);
		}
		glowBall(buffers, pose, facing, 0, 0, 0.2f * s, 0.9f * s, WATER_GLOW, 0.35f);
		pose.popPose();
		// A twinkle at its point, and the water's light about it.
		star(buffers, pose, facing, 0, 0, 0, 1.3f * s, age * 0.15f, new float[]{0.8f, 0.95f, 1.0f}, 0.75f);
		glowBall(buffers, pose, facing, 0, 0, 0, 1.8f * s, WATER_GLOW, 0.25f);
		// Drops left hanging behind.
		VertexConsumer drops = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
		List<Vec3> path = back(trail, 3.5, 10);
		for (int i = 1; i < path.size(); i++) {
			long seed = missile.getId() * 19L + i + (long) Math.floor(age / 4);
			Vec3 p = path.get(i).add((SpellRender.hash(seed) - 0.5) * 0.6, (SpellRender.hash(seed + 1) - 0.5) * 0.6 - i * 0.03, (SpellRender.hash(seed + 2) - 0.5) * 0.6);
			SpellRender.billboard(drops, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.12f * s, 0, WHITE, 0.8f * (1 - i / (float) path.size()), 0, 0, 1, 1,
					SpellRender.FULL_BRIGHT);
		}
	}

	/** Glacial Spears: a long crystal of ice, a cold light in its heart, frost mist trailing. While it forms, it grows from a sliver. */
	private void iceSpear(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age,
						  float formed, float yaw, float pitch) {
		float g = SpellRender.easeOut(formed);
		float s = missile.scale();
		pose.pushPose();
		face(pose, yaw, pitch);
		List<Vec3> spine = new ArrayList<>();
		float len = 1.7f * s * (0.15f + 0.85f * g);
		float[] profile = {0.0f, 0.13f, 0.15f, 0.12f, 0.06f, 0.0f};
		float[] radii = new float[profile.length];
		for (int i = 0; i < profile.length; i++) {
			spine.add(new Vec3(0, 0, len * (0.55 - i / (double) (profile.length - 1))));
			radii[i] = profile[i] * s * (0.4f + 0.6f * g);
		}
		pose.mulPose(Axis.ZP.rotationDegrees(age * 3 + missile.data() * 40));
		SpellRender.tube(buffers.getBuffer(SpellRender.clear(SpellRender.ICE)), pose.last(), spine, radii, 6, WHITE, 0.85f, 0.6f, 0, SpellRender.FULL_BRIGHT, null);
		float[] core = new float[radii.length];
		for (int i = 0; i < radii.length; i++) core[i] = radii[i] * 0.5f;
		SpellRender.tube(buffers.getBuffer(SpellRender.glow(SpellRender.GLOW)), pose.last(), spine, core, 6, ICE_GLOW, 0.8f, 0.3f, 0, SpellRender.FULL_BRIGHT, null);
		pose.popPose();
		glowBall(buffers, pose, facing, 0, 0, 0, 1.2f * s * g, ICE_GLOW, 0.3f);
		// Frost glinting about it as it turns.
		for (int i = 0; i < 4; i++) {
			double a = age * 0.3 + i * Math.PI / 2;
			float tw = 0.5f + 0.5f * Mth.sin(age * 0.7f + i * 2);
			star(buffers, pose, facing, (float) Math.cos(a) * 0.35f * s, (float) Math.sin(a) * 0.35f * s, 0, 0.45f * s * g, (float) a, WHITE, 0.8f * tw);
		}
		if (!missile.forming()) {
			VertexConsumer mist = buffers.getBuffer(SpellRender.clear(SpellRender.MIST));
			List<Vec3> path = back(trail, 4, 8);
			for (int i = 1; i < path.size(); i++) {
				Vec3 p = path.get(i);
				float t = i / (float) path.size();
				SpellRender.billboard(mist, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.6f + t, i, ICE_GLOW, 0.4f * (1 - t), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}
	}

	/** Torrential Spring's falling water: a great wobbling ball of it, a bright heart, water streaming off behind. */
	private void waterBall(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, List<Vec3> trail, float age) {
		float[] fv = SpellRender.frame(age, 0.5f, SpellRender.WATER_FRAMES);
		float r = 2.2f * missile.scale();
		SpellRender.sphere(buffers.getBuffer(SpellRender.clear(SpellRender.WATER)), pose.last(), r, 12, 18, WHITE, 0.85f, 2, (fv[1] - fv[0]) * 2, age * 0.01f, fv[0],
				SpellRender.FULL_BRIGHT, (polar, azimuth) -> 1 + 0.06f * Mth.sin((float) (azimuth * 3 + age * 0.3)) * Mth.sin((float) (polar * 4 + age * 0.2)));
		glowBall(buffers, pose, facing, 0, 0, 0, r * 1.4f, WATER_GLOW, 0.3f);
		VertexConsumer drops = buffers.getBuffer(SpellRender.clear(SpellRender.BUBBLE));
		List<Vec3> path = back(trail, 6, 12);
		for (int i = 1; i < path.size(); i++) {
			for (int k = 0; k < 3; k++) {
				long seed = missile.getId() * 23L + i * 3 + k;
				Vec3 p = path.get(i).add((SpellRender.hash(seed) - 0.5) * r * 1.4, (SpellRender.hash(seed + 1) - 0.5) * r, (SpellRender.hash(seed + 2) - 0.5) * r * 1.4);
				SpellRender.billboard(drops, pose, facing, (float) p.x, (float) p.y, (float) p.z, 0.25f, 0, WHITE, 0.8f, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
		}
	}

	// --- Metal ---

	/**
	 * Flying Sword (and the plunging swords of Sword Rain, the six of the Advanced technique): a jian of steel and gold, point first,
	 * a streak of white-gold light behind it; while it forms, shards of metal fly together into it.
	 */
	/**
	 * One of Sword Rain's swords plunging: the sword itself is its body (SpellConstruct RAIN_SWORD: SpellProjectile#body), so this is only the
	 * sword-light streaming behind it.
	 */
	private void plunging(PoseStack pose, MultiBufferSource buffers, Vec3 cam, List<Vec3> trail) {
		streak(buffers, pose, trail, cam, SpellRender.GLOW, SWORD_LIGHT, 0.9f, 0.75f, 6.0, 0);
		streak(buffers, pose, trail, cam, SpellRender.GLOW, WHITE, 0.3f, 0.9f, 3.5, 0);
	}

	private void sword(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age, float formed,
					   float yaw, float pitch, int light, float length) {
		float s = missile.scale();
		float g = SpellRender.ease(formed * 1.6f);
		pose.pushPose();
		face(pose, yaw, pitch);
		pose.translate(0, 0, -length * s * 0.35f);
		SpellModels.jian(pose, buffers, length * s, SpellRender.FULL_BRIGHT, missile.forming() ? g : 1, null);
		pose.popPose();
		glowBall(buffers, pose, facing, 0, 0, 0, 1.0f * s, SWORD_LIGHT, missile.forming() ? 0.5f * (1 - g) + 0.15f : 0.2f);
		if (missile.forming()) {
			VertexConsumer steel = buffers.getBuffer(SpellRender.solid(SpellRender.STEEL));
			for (int i = 0; i < 8; i++) {
				long seed = missile.getId() * 11L + i;
				float t = Mth.clamp(formed * 1.4f - SpellRender.hash(seed) * 0.4f, 0, 1);
				if (t >= 1) continue;
				Vec3 from = new Vec3((SpellRender.hash(seed + 1) - 0.5) * 3, (SpellRender.hash(seed + 2) - 0.5) * 3, (SpellRender.hash(seed + 3) - 0.5) * 3);
				Vec3 at = from.scale(1 - SpellRender.ease(t));
				pose.pushPose();
				pose.translate(at.x, at.y, at.z);
				pose.mulPose(Axis.YP.rotation(age * 0.5f + i));
				pose.mulPose(Axis.XP.rotation(age * 0.3f + i));
				SpellRender.box(steel, pose.last(), -0.06f, -0.015f, -0.1f, 0.06f, 0.015f, 0.1f, WHITE, 1, 0, 0, 0.4f, 0.4f, SpellRender.FULL_BRIGHT);
				pose.popPose();
				glowBall(buffers, pose, facing, (float) at.x, (float) at.y, (float) at.z, 0.3f, SWORD_LIGHT, 0.5f);
			}
		} else {
			// Sword light streaming behind it, white at its heart, and a star at its point.
			streak(buffers, pose, trail, cam, SpellRender.GLOW, SWORD_LIGHT, 0.5f * s, 0.75f, 4.0, 0);
			streak(buffers, pose, trail, cam, SpellRender.GLOW, WHITE, 0.18f * s, 0.9f, 2.5, 0);
			Vec3 way = trail.size() > 1 ? trail.get(0).subtract(trail.get(1)) : Vec3.ZERO;
			Vec3 tip = way.lengthSqr() < 1.0e-6 ? Vec3.ZERO : way.normalize().scale(length * s * 0.65);
			star(buffers, pose, facing, (float) tip.x, (float) tip.y, (float) tip.z, 1.1f * s, age * 0.2f, WHITE, 0.85f);
		}
	}

	/** Heaven-Severing Sword: a jian as long as a tower, point down, wreathed in golden light, a pillar of light behind it. */
	private void colossal(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age, int light) {
		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(-missile.data()));
		pose.mulPose(Axis.XP.rotationDegrees(90));
		pose.translate(0, 0, -6);
		SpellModels.jian(pose, buffers, 18, SpellRender.FULL_BRIGHT, 1, new float[]{1.0f, 0.95f, 0.8f});
		pose.popPose();
		glowBall(buffers, pose, facing, 0, 0, 0, 14, new float[]{1.0f, 0.85f, 0.45f}, 0.35f);
		glowBall(buffers, pose, facing, 0, -6, 0, 6, SWORD_LIGHT, 0.6f);
		streak(buffers, pose, trail, cam, SpellRender.GLOW, SWORD_LIGHT, 4, 0.5f, 30, 0);
		// Seals of metal turning round the blade as it comes down, and a blazing star where its point leads.
		VertexConsumer seal = buffers.getBuffer(SpellRender.glow(SpellRender.SEAL_METAL));
		for (int k = 0; k < 3; k++) {
			pose.pushPose();
			pose.translate(0, -2 + k * 6, 0);
			SpellRender.disc(seal, pose.last(), 3.2f - k * 0.6f, 0, age * (k % 2 == 0 ? 0.05f : -0.07f), new float[]{1.0f, 0.88f, 0.5f}, 0.75f, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}
		star(buffers, pose, facing, 0, -12, 0, 9, age * 0.1f, WHITE, 0.9f);
	}

	/**
	 * Boulder Cleave and Mountain Cleave: a crescent of sword light flying bow-first (a mountain's stood on its edge, as tall as the
	 * slot it cuts), a hot white edge, fainter echoes of it trailing behind.
	 */
	private void slash(SpellProjectile missile, PoseStack pose, MultiBufferSource buffers, List<Vec3> trail, float age, float yaw, float pitch) {
		boolean mountain = missile.shape() == SpellProjectile.Shape.MOUNTAIN_SLASH;
		float s = missile.scale();
		VertexConsumer vc = buffers.getBuffer(SpellRender.glow(SpellRender.SLASH));
		float flicker = 0.85f + 0.15f * Mth.sin(age * 2.3f);
		for (int echo = 0; echo < 4; echo++) {
			pose.pushPose();
			face(pose, yaw, 0);
			pose.translate(0, mountain ? 3.0f * s / 4 : 0, -echo * (mountain ? 2.5f : 0.6f));
			if (mountain) pose.mulPose(Axis.ZP.rotationDegrees(90));
			float alpha = (echo == 0 ? 1 : 0.45f / echo) * flicker;
			float radius = (mountain ? 7.0f : 1.7f) * (mountain ? s / 4 : s);
			SpellRender.crescent(vc, pose.last(), radius, radius * 0.45f, 2.3f, 16, new float[]{0.85f, 0.95f, 1.0f}, alpha, SpellRender.FULL_BRIGHT);
			SpellRender.crescent(vc, pose.last(), radius * 0.98f, radius * 0.16f, 2.1f, 16, WHITE, alpha, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}
		// Stars flaring at its two tips, and the light of its edge about it.
		Quaternionf facing = entityRenderDispatcher.cameraOrientation();
		float radius = (mountain ? 7.0f : 1.7f) * (mountain ? s / 4 : s);
		// The crescent's tips: 0.91 of its radius out to either side, 0.59 back from its bow (SpellRender#crescent, span 2.3).
		Vec3 ahead = Vec3.directionFromRotation(0, -yaw), across = new Vec3(ahead.z, 0, -ahead.x);
		float lift = mountain ? 3.0f * s / 4 : 0;
		for (int side = -1; side <= 1; side += 2) {
			Vec3 tip = (mountain ? new Vec3(0, side * radius * 0.91 + lift, 0) : across.scale(side * radius * 0.91)).add(ahead.scale(-radius * 0.59));
			star(buffers, pose, facing, (float) tip.x, (float) tip.y, (float) tip.z, (mountain ? 3.5f : 1.1f) * s, age * 0.25f, WHITE, 0.9f * flicker);
		}
		glowBall(buffers, pose, facing, 0, lift, 0, radius * 2.2f, new float[]{0.8f, 0.92f, 1.0f}, 0.2f);
	}

	/**
	 * How far below the Heaven-Severing slash's drawn middle its blade lies: the missile flies a block less MetalSpells#SEVER_LIFT under
	 * its blade's middle (setting out from its caster's chest, the way they look) and is drawn from half its 4-block height up.
	 */
	private static final float SEVER_DROP = 3 - MetalSpells.SEVER_LIFT;

	/**
	 * Heaven-Severing Sword: the edge of sword light the stroke threw (SpellFieldRenderer#severSwing), lying level, about 110 blocks
	 * across (MetalSpells' numbers, the same the server cuts by), sweeping bow-first at a caster's chest. A keen blade, not a cloud:
	 * its bow stands up as a band of white-hot light as tall as the blade is thick (thick at its middle, thinning to its tips), a
	 * narrow sheet of light lies behind its edge, echoes of the edge stream behind it, small stars glint at its tips and wakes of
	 * light trail from its tips and middle.
	 */
	private void heavenSlash(PoseStack pose, MultiBufferSource buffers, Quaternionf facing, Vec3 cam, List<Vec3> trail, float age, float yaw) {
		float flicker = 0.9f + 0.1f * Mth.sin(age * 2.7f);
		float r = MetalSpells.SEVER_RADIUS, span = MetalSpells.SEVER_SPAN, half = MetalSpells.SEVER_THICK / 2;
		float depth = r * 0.14f;
		// Warm sword light (gold-white, as the other metal spells), which shows against a blue sky where pale blue-white is lost.
		float[] pale = {1.0f, 0.86f, 0.52f}, edge = {1.0f, 0.97f, 0.86f};
		pose.pushPose();
		pose.translate(0, -SEVER_DROP, 0);
		Vec3 eye = cam.add(0, SEVER_DROP, 0);
		VertexConsumer light = buffers.getBuffer(SpellRender.glow(SpellRender.SEVER));
		VertexConsumer body = buffers.getBuffer(SpellRender.clear(SpellRender.SEVER));
		// Its edge: the bow stood up as tall as the blade is thick (thick at its middle, thin at its tips), white-hot and solid, with a
		// halo of light about it; seen from behind and level with it, it is a great arc of light bowed up over the land.
		pose.pushPose();
		face(pose, yaw, 0);
		bow(body, pose.last(), r, half, span, 48, edge, flicker, 0.3f);
		bow(light, pose.last(), r, half, span, 48, edge, flicker, 0.3f);
		bow(light, pose.last(), r * 1.004f, half * 1.6f, span, 48, pale, 0.5f * flicker, 1);
		// Its body: the crescent itself, lying level from its edge back, see-through, with light over it.
		SpellRender.crescent(body, pose.last(), r, depth, span, 48, pale, 0.75f * flicker, SpellRender.FULL_BRIGHT);
		SpellRender.crescent(light, pose.last(), r, depth, span, 48, pale, 0.7f * flicker, SpellRender.FULL_BRIGHT);
		// Its inner side stood up too, fainter and lower, so the blade shows its depth from behind.
		bow(body, pose.last(), r - depth, half * 0.5f, span * 0.9f, 48, pale, 0.45f * flicker, 1);
		pose.popPose();
		// Echoes of the blade streaming behind it.
		for (int echo = 1; echo < 5; echo++) {
			pose.pushPose();
			face(pose, yaw, 0);
			pose.translate(0, 0, -echo * 9.0f);
			float radius = r * (1 - echo * 0.03f);
			bow(light, pose.last(), radius, half * (1 - echo * 0.15f), span, 48, pale, 0.5f / echo * flicker, 1);
			SpellRender.crescent(light, pose.last(), radius, depth * 0.7f, span, 48, pale, 0.4f / echo * flicker, SpellRender.FULL_BRIGHT);
			pose.popPose();
		}
		// Stars at its two tips (out to either side, back from its bow by the crescent's curve), a wake of light from each.
		Vec3 ahead = Vec3.directionFromRotation(0, -yaw), across = new Vec3(ahead.z, 0, -ahead.x);
		float tipOut = r * Mth.sin(span / 2), tipBack = r * (1 - Mth.cos(span / 2));
		for (int side = -1; side <= 1; side += 2) {
			Vec3 tip = across.scale(side * tipOut).add(ahead.scale(-tipBack));
			star(buffers, pose, facing, (float) tip.x, 0, (float) tip.z, 6, age * 0.25f, WHITE, flicker);
			List<Vec3> wake = new ArrayList<>();
			for (Vec3 point : trail) wake.add(point.add(tip));
			streak(buffers, pose, wake, eye, SpellRender.GLOW, pale, 3.5f, 0.7f, 60, age * 0.05f);
		}
		// Its middle's wake, burning white over the cut it leaves.
		streak(buffers, pose, trail, eye, SpellRender.GLOW, edge, 4.0f, 0.7f, 90, age * 0.05f);
		streak(buffers, pose, trail, eye, SpellRender.SLASH, WHITE, 3.0f, 0.9f, 60, age * 0.08f);
		pose.popPose();
	}

	/**
	 * The bow of a crescent ({@link SpellRender#crescent}'s outer edge, round a centre {@code radius} behind it) stood up as a band
	 * {@code half} above and below it, thinning toward its tips: bright along its middle, fading up and down (the texture's v from 0
	 * at the middle to {@code vEdge} at the band's top and bottom: less than 1 keeps the whole band nearer full), so a level crescent
	 * shows from level with it too.
	 */
	private static void bow(VertexConsumer vc, PoseStack.Pose pose, float radius, float half, float span, int segments, float[] rgb, float alpha,
							float vEdge) {
		for (int i = 0; i < segments; i++) {
			float a0 = -span / 2 + span * i / segments, a1 = -span / 2 + span * (i + 1) / segments;
			float h0 = half * (0.25f + 0.75f * Mth.sin((float) Math.PI * i / segments));
			float h1 = half * (0.25f + 0.75f * Mth.sin((float) Math.PI * (i + 1) / segments));
			float x0 = Mth.sin(a0) * radius, z0 = Mth.cos(a0) * radius - radius, x1 = Mth.sin(a1) * radius, z1 = Mth.cos(a1) * radius - radius;
			float u0 = i / (float) segments, u1 = (i + 1) / (float) segments;
			float nx = Mth.sin((a0 + a1) / 2), nz = Mth.cos((a0 + a1) / 2);
			for (int up = -1; up <= 1; up += 2) {
				SpellRender.vertex(vc, pose, x0, 0, z0, rgb[0], rgb[1], rgb[2], alpha, u0, 0, SpellRender.FULL_BRIGHT, nx, 0, nz);
				SpellRender.vertex(vc, pose, x0, up * h0, z0, rgb[0], rgb[1], rgb[2], alpha, u0, vEdge, SpellRender.FULL_BRIGHT, nx, 0, nz);
				SpellRender.vertex(vc, pose, x1, up * h1, z1, rgb[0], rgb[1], rgb[2], alpha, u1, vEdge, SpellRender.FULL_BRIGHT, nx, 0, nz);
				SpellRender.vertex(vc, pose, x1, 0, z1, rgb[0], rgb[1], rgb[2], alpha, u1, 0, SpellRender.FULL_BRIGHT, nx, 0, nz);
			}
		}
	}

	@Override
	protected int getBlockLightLevel(SpellProjectile missile, net.minecraft.core.BlockPos pos) {
		return 15;
	}

	/** A missile has no name to show. */
	@Override
	protected boolean shouldShowName(SpellProjectile entity) {
		return false;
	}
}
